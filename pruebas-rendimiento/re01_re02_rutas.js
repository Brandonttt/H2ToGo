// ============================================================================
// RE-01 y RE-02 · Tiempo de cálculo de rutas (RNF-009: 5 s o menos)
//
// RE-01: ruta A* de un pedido asignado (GET /pedidos/{id}/ruta), 20 mediciones
//        desde 20 puntos de origen distintos de Benito Juárez.
// RE-02: ruta del día con OR-Tools (GET /repartidores/me/ruta-optima), 20 mediciones
//        con 5 paradas y 20 con 10 paradas.
//
// Prepara sus datos con la API usando las cuentas de 01_datos_carga.sql:
//   - rep02 inicia jornada y acepta un pedido del cliente 001 (RE-01);
//   - los clientes 002 a 011 programan pedidos para hoy y rep03 los reserva (RE-02).
// Los pedidos programados deben quedar dentro del día: correr antes de las 21:30.
//
//   k6 run -e H2TOGO_URL=https://.../api/v1 re01_re02_rutas.js
// ============================================================================
import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import {
    BASE, login, cabeceras, negocioDeCarga, direccionDe, pedidoDirecto, correoCliente, correoRepartidor,
} from './comun.js';

const MEDICIONES = 20;
const tAstar = new Trend('re01_ruta_astar_ms', true);
const tDia5 = new Trend('re02_ruta_dia_5_paradas_ms', true);
const tDia10 = new Trend('re02_ruta_dia_10_paradas_ms', true);

export const options = {
    vus: 1,
    iterations: 1,
    setupTimeout: '3m',
    thresholds: {
        re01_ruta_astar_ms: ['p(95)<5000'],
        re02_ruta_dia_5_paradas_ms: ['p(95)<5000'],
        re02_ruta_dia_10_paradas_ms: ['p(95)<5000'],
        checks: ['rate==1'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(95)', 'max'],
};

// 20 orígenes dentro de Benito Juárez (Del Valle, Narvarte, Nápoles, Portales, Mixcoac…).
const ORIGENES = [];
for (let i = 0; i < MEDICIONES; i++) {
    ORIGENES.push([19.364 + (i % 5) * 0.008, -99.178 + Math.floor(i / 5) * 0.009]);
}

function exigir(r, esperado, que) {
    if (r.status !== esperado) {
        throw new Error(`${que} respondió ${r.status}: ${r.body}`);
    }
    return r;
}

export function setup() {
    // Hora local de la CDMX (UTC-6, sin horario de verano).
    const local = new Date(Date.now() - 6 * 3600 * 1000);
    if (local.getUTCHours() * 60 + local.getUTCMinutes() > 21 * 60 + 30) {
        throw new Error('Corre esta prueba antes de las 21:30: los pedidos programados deben quedar para hoy.');
    }
    const rep02 = login(correoRepartidor(2));
    const rep03 = login(correoRepartidor(3));
    const negocio = negocioDeCarga(login(correoRepartidor(1)));
    const { idNegocio, idMarca, vehiculos } = negocio;

    // RE-01: pedido asignado a rep02.
    exigir(http.post(`${BASE}/repartidores/me/jornada`,
        JSON.stringify({ idVehiculo: vehiculos[1], cargaInicial: [{ idMarca, cantidad: 20 }] }),
        cabeceras(rep02, 'setup')), 200, 'Iniciar jornada de rep02');
    const cli1 = login(correoCliente(1));
    const pedido = exigir(http.post(`${BASE}/pedidos`, pedidoDirecto(idNegocio, idMarca, direccionDe(cli1).id),
        cabeceras(cli1, 'setup')), 201, 'Crear pedido del cliente 001').json('id');
    exigir(http.post(`${BASE}/pedidos/${pedido}/aceptacion`, null, cabeceras(rep02, 'setup')), 200, 'Aceptar pedido');

    // RE-02: 10 programados para dentro de 90 min (sin pasar de las 23:30).
    const fecha = new Date(Math.min(Date.now() + 90 * 60 * 1000,
        Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), local.getUTCDate(), 23 + 6, 30) - 0));
    const programados = [];
    for (let i = 2; i <= 11; i++) {
        const tk = login(correoCliente(i));
        programados.push(exigir(http.post(`${BASE}/pedidos`,
            pedidoDirecto(idNegocio, idMarca, direccionDe(tk).id, fecha.toISOString()),
            cabeceras(tk, 'setup')), 201, `Programar pedido del cliente ${i}`).json('id'));
    }
    return { rep02, rep03, pedido, programados };
}

function medir(trend, token, url, op) {
    const r = http.get(url, cabeceras(token, op));
    check(r, { [`${op} → 200`]: (x) => x.status === 200 });
    trend.add(r.timings.duration);
}

function reservar(token, ids) {
    for (const id of ids) {
        exigir(http.post(`${BASE}/pedidos/${id}/reserva`, null, cabeceras(token, 'setup')), 204, `Reservar ${id}`);
    }
}

export default function (data) {
    for (const [lat, lon] of ORIGENES) {
        medir(tAstar, data.rep02,
            `${BASE}/pedidos/${data.pedido}/ruta?origenLat=${lat}&origenLon=${lon}`, 'ruta_astar');
    }

    reservar(data.rep03, data.programados.slice(0, 5));
    for (const [lat, lon] of ORIGENES) {
        medir(tDia5, data.rep03,
            `${BASE}/repartidores/me/ruta-optima?origenLat=${lat}&origenLon=${lon}`, 'ruta_dia_5');
    }

    reservar(data.rep03, data.programados.slice(5));
    for (const [lat, lon] of ORIGENES) {
        medir(tDia10, data.rep03,
            `${BASE}/repartidores/me/ruta-optima?origenLat=${lat}&origenLon=${lon}`, 'ruta_dia_10');
    }
}
