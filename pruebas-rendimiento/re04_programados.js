// ============================================================================
// RE-04 · Activación de pedidos programados (RNF-013: la tarea corre al menos cada 5 min)
//
// Los clientes 012 a 031 programan 20 pedidos, el primero a 3 min y luego uno cada
// 20 s. El script consulta cada pedido hasta que pasa a "pendiente" y mide el retraso
// entre su hora programada y la hora en que el historial registra la activación.
// Si la tarea corre cada 5 min o menos, ningún retraso puede pasar de 300 s.
// Tarda unos 12 minutos.
//
//   k6 run -e H2TOGO_URL=https://.../api/v1 re04_programados.js
// ============================================================================
import http from 'k6/http';
import { sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE, login, cabeceras, negocioDeCarga, direccionDe, pedidoDirecto, correoCliente, correoRepartidor }
    from './comun.js';

const PEDIDOS = 20;
const retraso = new Trend('re04_retraso_activacion_s');

export const options = {
    vus: 1,
    iterations: 1,
    setupTimeout: '3m',
    thresholds: { re04_retraso_activacion_s: ['max<300'] },
    summaryTrendStats: ['avg', 'min', 'med', 'p(95)', 'max'],
};

export function setup() {
    const { idNegocio, idMarca } = negocioDeCarga(login(correoRepartidor(1)));
    const inicio = Date.now() + 3 * 60 * 1000;
    const pedidos = [];
    for (let i = 0; i < PEDIDOS; i++) {
        const token = login(correoCliente(12 + i));
        const fecha = new Date(inicio + i * 20 * 1000).toISOString();
        const r = http.post(`${BASE}/pedidos`, pedidoDirecto(idNegocio, idMarca, direccionDe(token).id, fecha),
            cabeceras(token, 'setup'));
        if (r.status !== 201) {
            throw new Error(`Programar pedido ${i + 1} respondió ${r.status}: ${r.body}`);
        }
        pedidos.push({ id: r.json('id'), token, fecha });
    }
    return { pedidos };
}

export default function (data) {
    const pendientes = new Map(data.pedidos.map((p) => [p.id, p]));
    const limite = Date.now() + 25 * 60 * 1000;
    while (pendientes.size > 0 && Date.now() < limite) {
        for (const [id, p] of pendientes) {
            const r = http.get(`${BASE}/pedidos/${id}`, cabeceras(p.token, 'consultar'));
            if (r.status !== 200) {
                continue;
            }
            const activacion = (r.json('historial') || []).find((h) => h.estado === 'pendiente');
            if (activacion) {
                const s = (Date.parse(activacion.fechaCambio) - Date.parse(r.json('fechaProgramada'))) / 1000;
                retraso.add(s);
                console.log(`Pedido ${id}: programado ${p.fecha}, activado ${activacion.fechaCambio} → ${s.toFixed(0)} s`);
                pendientes.delete(id);
            }
        }
        sleep(15);
    }
    if (pendientes.size > 0) {
        console.error(`${pendientes.size} pedidos no se activaron en 25 minutos: ${[...pendientes.keys()]}`);
        retraso.add(9999);
    }
}
