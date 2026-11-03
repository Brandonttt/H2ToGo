// ============================================================================
// CC-03 · 100 usuarios simultáneos con operaciones mezcladas (RNF-007)
//
// 100 usuarios virtuales, cada uno con su propia cuenta y sesión:
//   - VU 1 a 10:   repartidores. Reportan su ubicación cada 5 s (como la app en
//                  camino) y consultan pedidos disponibles y sus entregas.
//   - VU 11 a 100: clientes. Buscan purificadoras, ven un perfil, crean un pedido,
//                  lo consultan, lo cancelan y revisan su lista de pedidos.
// Rampa de 30 s, 5 min sostenidos con 100 usuarios y 30 s de bajada (para un ensayo
// corto: -e DURACION=1m).
//
// Al final, una ráfaga: los 100 clientes crean un pedido en el mismo instante, para
// tener 100 transacciones de escritura simultáneas en la base (lo que pide RNF-007).
//
// Criterios del plan de pruebas: menos de 1% de errores HTTP y 0 errores de base de
// datos (aquí: 0 respuestas 5xx; confirmar en el log del backend). Se reporta p95.
//
//   k6 run -e H2TOGO_URL=https://.../api/v1 cc03_100_usuarios.js
// ============================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import {
    BASE, login, cabeceras, negocioDeCarga, direccionDe, pedidoDirecto, correoCliente, correoRepartidor,
} from './comun.js';

const REPARTIDORES = 10;
const USUARIOS = 100;
const errores5xx = new Counter('errores_5xx');

// La ráfaga empieza cuando terminó la mezcla (rampa + sostenido + bajada + margen).
const DURACION = __ENV.DURACION || '5m';
const segundos = (d) => parseInt(d, 10) * ({ s: 1, m: 60, h: 3600 })[d.slice(-1)];
const INICIO_RAFAGA = `${30 + segundos(DURACION) + 30 + 30 + 10}s`;

export const options = {
    setupTimeout: '5m',
    scenarios: {
        mezcla: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: USUARIOS },
                { duration: DURACION, target: USUARIOS },
                { duration: '30s', target: 0 },
            ],
            gracefulRampDown: '30s',
        },
        rafaga: {
            executor: 'per-vu-iterations',
            vus: USUARIOS,
            iterations: 1,
            startTime: INICIO_RAFAGA,
            exec: 'rafaga',
        },
    },
    thresholds: {
        'http_req_failed{scenario:mezcla}': ['rate<0.01'],
        checks: ['rate>0.99'],
        errores_5xx: ['count==0'],
        'http_req_duration{op:crear_pedido}': ['p(95)<3000'],
        'http_req_duration{op:reportar_ubicacion}': ['p(95)<3000'],
        'http_req_duration{scenario:mezcla}': ['p(95)<3000'],
        'http_req_failed{scenario:rafaga}': ['rate<0.01'],
        'http_req_duration{op:rafaga_crear}': ['p(95)<3000'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max'],
};

export function setup() {
    const repartidores = [];
    for (let i = 1; i <= REPARTIDORES; i++) {
        repartidores.push(login(correoRepartidor(i)));
    }
    const negocio = negocioDeCarga(repartidores[0]);
    const clientes = [];
    for (let i = 1; i <= USUARIOS; i++) {
        const token = login(correoCliente(i));
        const dir = direccionDe(token);
        clientes.push({ token, idDireccion: dir.id, lat: dir.lat, lon: dir.lon });
    }
    return { repartidores, clientes, negocio };
}

function registrar(r, op, esperado) {
    if (r.status >= 500) {
        errores5xx.add(1, { op });
    }
    return check(r, { [`${op} → ${esperado}`]: (x) => x.status === esperado });
}

function repartidor(data, token) {
    const lat = data.negocio.lat + (Math.random() - 0.5) * 0.02;
    const lon = data.negocio.lon + (Math.random() - 0.5) * 0.02;
    registrar(http.put(`${BASE}/repartidores/me/ubicacion`, JSON.stringify({ lat, lon }),
        cabeceras(token, 'reportar_ubicacion')), 'reportar_ubicacion', 204);
    registrar(http.get(`${BASE}/repartidores/me/pedidos-disponibles`, cabeceras(token, 'pedidos_disponibles')),
        'pedidos_disponibles', 200);
    registrar(http.get(`${BASE}/repartidores/me/entregas`, cabeceras(token, 'mis_entregas')), 'mis_entregas', 200);
    sleep(5);
}

function cliente(data, c) {
    const { idNegocio, idMarca } = data.negocio;
    registrar(http.get(`${BASE}/negocios?cerca=${c.lat},${c.lon}&limite=10`, cabeceras(c.token, 'negocios_cercanos')),
        'negocios_cercanos', 200);
    registrar(http.get(`${BASE}/negocios/${idNegocio}/perfil`, cabeceras(c.token, 'perfil_negocio')),
        'perfil_negocio', 200);
    sleep(1 + Math.random() * 2);

    const creado = http.post(`${BASE}/pedidos`, pedidoDirecto(idNegocio, idMarca, c.idDireccion),
        cabeceras(c.token, 'crear_pedido'));
    if (registrar(creado, 'crear_pedido', 201)) {
        const id = creado.json('id');
        registrar(http.get(`${BASE}/pedidos/${id}`, cabeceras(c.token, 'detalle_pedido')), 'detalle_pedido', 200);
        sleep(1 + Math.random() * 2);
        registrar(http.post(`${BASE}/pedidos/${id}/cancelacion`, JSON.stringify({ motivo: 'Prueba de carga' }),
            cabeceras(c.token, 'cancelar_pedido')), 'cancelar_pedido', 200);
    }
    registrar(http.get(`${BASE}/clientes/me/pedidos`, cabeceras(c.token, 'mis_pedidos')), 'mis_pedidos', 200);
    sleep(1 + Math.random() * 2);
}

export default function (data) {
    if (__VU <= REPARTIDORES) {
        repartidor(data, data.repartidores[__VU - 1]);
    } else {
        cliente(data, data.clientes[__VU - REPARTIDORES - 1]);
    }
}

/** Cada uno de los 100 clientes crea un pedido al mismo tiempo y luego lo cancela. */
export function rafaga(data) {
    const c = data.clientes[__VU - 1];
    const { idNegocio, idMarca } = data.negocio;
    const creado = http.post(`${BASE}/pedidos`, pedidoDirecto(idNegocio, idMarca, c.idDireccion),
        cabeceras(c.token, 'rafaga_crear'));
    if (registrar(creado, 'rafaga_crear', 201)) {
        registrar(http.post(`${BASE}/pedidos/${creado.json('id')}/cancelacion`, null,
            cabeceras(c.token, 'rafaga_cancelar')), 'rafaga_cancelar', 200);
    }
}
