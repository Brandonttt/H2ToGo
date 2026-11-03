// Funciones compartidas por los scripts de k6 de H2ToGo.
import http from 'k6/http';

export const BASE = __ENV.H2TOGO_URL || 'http://localhost:8088/api/v1';
export const PASSWORD = 'Demo1234';

export const correoCliente = (n) => `carga.cli${String(n).padStart(3, '0')}@carga.h2togo.test`;
export const correoRepartidor = (n) => `carga.rep${String(n).padStart(2, '0')}@carga.h2togo.test`;

const JSON_H = { 'Content-Type': 'application/json' };

/** Inicia sesión (cerrando la que hubiera) y devuelve el token, o lanza si falla. */
export function login(correo) {
    const r = http.post(`${BASE}/auth/login`, JSON.stringify({ correo, password: PASSWORD, forzar: true }),
        { headers: JSON_H, tags: { op: 'setup' } });
    if (r.status !== 200) {
        throw new Error(`Login de ${correo} respondió ${r.status}: ${r.body}. ¿Corriste 01_datos_carga.sql?`);
    }
    return r.json('token');
}

export function cabeceras(token, op) {
    return { headers: { ...JSON_H, Authorization: `Bearer ${token}` }, tags: { op } };
}

/** Datos de la purificadora de carga, leídos con la sesión de su dueño (rep01). */
export function negocioDeCarga(tokenDueno) {
    const r = http.get(`${BASE}/negocios/me`, cabeceras(tokenDueno, 'setup'));
    if (r.status !== 200) {
        throw new Error(`GET /negocios/me respondió ${r.status}: ${r.body}`);
    }
    const n = r.json();
    return {
        idNegocio: n.id,
        idMarca: n.productos[0].idMarca,
        vehiculos: n.vehiculos.map((v) => v.id),
        lat: n.direccion.lat,
        lon: n.direccion.lon,
    };
}

/** Primera dirección del cliente. */
export function direccionDe(token) {
    const r = http.get(`${BASE}/clientes/me/direcciones`, cabeceras(token, 'setup'));
    if (r.status !== 200 || r.json().length === 0) {
        throw new Error(`El cliente no tiene dirección (${r.status}): ${r.body}`);
    }
    return r.json()[0];
}

export function pedidoDirecto(idNegocio, idMarca, idDireccion, fechaProgramada) {
    const body = {
        tipoSolicitud: 'directa',
        idNegocio,
        idDireccionEntrega: idDireccion,
        indicaciones: 'Prueba de carga',
        detalles: [{ idMarca, cantidad: 1, tieneEnvase: true }],
    };
    if (fechaProgramada) {
        body.programado = { fechaProgramada };
    }
    return JSON.stringify(body);
}
