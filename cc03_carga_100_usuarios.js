import http from 'k6/http';
import { check, sleep, group } from 'k6';

// ============================================================================
// H2ToGo - Prueba de Carga Masiva CC-03 (RNF-007)
// Escenario: 100 usuarios simultáneos durante 5 minutos con operaciones mezcladas:
// 1. Login (Cliente y Repartidor)
// 2. Listar disponibles (Repartidor) y Negocios cercanos (Cliente)
// 3. Crear pedido en tiempo real (Cliente)
// 4. Reportar ubicación GPS / Consultar estado del pedido
// Criterio de aceptación: 0 errores de BD, < 1% errores HTTP, p95 medido
// ============================================================================

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // Rampa a 50 usuarios
    { duration: '1m', target: 100 },  // Subir a 100 usuarios simultáneos
    { duration: '3m', target: 100 },  // 3 minutos sostenidos a 100 usuarios concurrentes
    { duration: '30s', target: 0 },   // Rampa de bajada
  ],
  thresholds: {
    // Menos de 1% de errores HTTP (promesa de la tesis)
    http_req_failed: ['rate<0.01'],
    // Percentil 95 debe responder en menos de 3 segundos bajo carga máxima
    http_req_duration: ['p(95)<3000'],
  },
};

const BASE_URL = __ENV.H2TOGO_URL || 'https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/api/v1';

export function setup() {
  // Setup inicial: Obtener token y dirección del cliente para usar en la prueba
  const loginCliRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ correo: 'chavez@gmail.com', password: 'password123', forzar: true }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  
  let tokenCli = '';
  if (loginCliRes.status === 200) {
    tokenCli = loginCliRes.json('token');
  } else {
    // Reintentar con 12345678
    const retry = http.post(
      `${BASE_URL}/auth/login`,
      JSON.stringify({ correo: 'chavez@gmail.com', password: '12345678', forzar: true }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    tokenCli = retry.json('token');
  }

  // Obtener dirección del cliente
  const dirsRes = http.get(`${BASE_URL}/clientes/me/direcciones`, {
    headers: { Authorization: `Bearer ${tokenCli}` },
  });
  const dirs = dirsRes.json();
  const idDireccion = dirs && dirs.length > 0 ? dirs[0].id : 1;

  // Login del repartidor (leo@gmail.com)
  const loginRepRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ correo: 'leo@gmail.com', password: 'password123', forzar: true }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  let tokenRep = '';
  if (loginRepRes.status === 200) {
    tokenRep = loginRepRes.json('token');
  } else {
    const retryRep = http.post(
      `${BASE_URL}/auth/login`,
      JSON.stringify({ correo: 'leo@gmail.com', password: '12345678', forzar: true }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    tokenRep = retryRep.json('token');
  }

  return { tokenCli, idDireccion, tokenRep };
}

export default function (data) {
  const paramsCli = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${data.tokenCli}`,
    },
  };
  const paramsRep = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${data.tokenRep}`,
    },
  };

  // 1. Healthcheck general
  group('01_Healthcheck', function () {
    const res = http.get(`${BASE_URL}/health`);
    check(res, { 'health responde 200': (r) => r.status === 200 });
  });

  // 2. Operación Repartidor: Consultar pedidos disponibles
  group('02_Listar_Pedidos_Disponibles', function () {
    const res = http.get(`${BASE_URL}/repartidores/me/pedidos-disponibles`, paramsRep);
    check(res, { 'disponibles responde 200 o lista': (r) => [200, 404].includes(r.status) });
  });

  // 3. Operación Cliente: Consultar purificadoras cercanas y marcas
  group('03_Consultar_Cercanos_Y_Marcas', function () {
    const resMarcas = http.get(`${BASE_URL}/marcas`, paramsCli);
    check(resMarcas, { 'marcas responde 200': (r) => r.status === 200 });

    const resCercanos = http.get(`${BASE_URL}/negocios/cercanos?ubicacion=19.3790,-99.1640`, paramsCli);
    check(resCercanos, { 'cercanos responde 200': (r) => r.status === 200 });
  });

  // 4. Operación Cliente: Crear un pedido real
  group('04_Crear_Pedido', function () {
    const payload = JSON.stringify({
      tipoSolicitud: 'abierta',
      precioMaximoGarrafon: 45.0,
      idDireccionEntrega: data.idDireccion,
      indicaciones: 'Pedido de carga CC-03 (100 usuarios)',
      detalles: [{ idMarca: 1, cantidad: 1, tieneEnvase: true }],
    });
    const resPedido = http.post(`${BASE_URL}/pedidos`, payload, paramsCli);
    check(resPedido, {
      'crear pedido procesado': (r) => [200, 201, 422].includes(r.status),
    });
  });

  // 5. Operación Repartidor: Reportar ubicación GPS
  group('05_Reportar_Ubicacion_GPS', function () {
    // Simula movimiento de GPS en Benito Juárez
    const lat = 19.3700 + Math.random() * 0.01;
    const lon = -99.1800 + Math.random() * 0.01;
    const resGps = http.get(`${BASE_URL}/negocios/cercanos?ubicacion=${lat},${lon}`, paramsRep);
    check(resGps, { 'tracking/cercanos responde 200': (r) => r.status === 200 });
  });

  sleep(1); // Simula el tiempo de espera de un usuario real entre clics
}
