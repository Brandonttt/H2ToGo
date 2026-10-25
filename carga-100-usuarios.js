import http from 'k6/http';
import { check, sleep } from 'k6';

// Configuración de la prueba de carga para cumplir RNF-007 y CC-03
export const options = {
  stages: [
    { duration: '30s', target: 50 },  // Sube a 50 usuarios en 30s
    { duration: '1m', target: 100 },  // Sube y mantiene 100 usuarios simultáneos
    { duration: '2m', target: 100 },  // Se mantiene a 100 usuarios durante 2 minutos
    { duration: '30s', target: 0 },   // Rampa de bajada suave
  ],
  thresholds: {
    // Menos del 1% de errores HTTP (promesa de la tesis)
    http_req_failed: ['rate<0.01'],
    // El 95% de las peticiones debe responder en menos de 2 segundos
    http_req_duration: ['p(95)<2000'],
  },
};

const BASE_URL = __ENV.H2TOGO_URL || 'https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/api/v1';

export default function () {
  // 1. Healthcheck
  const resHealth = http.get(`${BASE_URL}/health`);
  check(resHealth, {
    'health responde 200': (r) => r.status === 200,
  });

  // 2. Consulta de marcas disponibles
  const resMarcas = http.get(`${BASE_URL}/marcas`);
  check(resMarcas, {
    'marcas responde 200': (r) => r.status === 200,
  });

  // 3. Consulta de purificadoras cercanas en Benito Juárez
  const resCercanos = http.get(`${BASE_URL}/negocios/cercanos?ubicacion=19.3790,-99.1640`);
  check(resCercanos, {
    'negocios cercanos responde 200': (r) => r.status === 200,
  });

  // 4. Intento de login cliente
  const payloadLogin = JSON.stringify({
    correo: 'leo@gmail.com',
    password: 'password123',
    forzar: false,
  });
  const params = {
    headers: { 'Content-Type': 'application/json' },
  };
  const resLogin = http.post(`${BASE_URL}/auth/login`, payloadLogin, params);
  check(resLogin, {
    'login responde status esperado (200 o 409 o 401)': (r) => [200, 401, 409, 422].includes(r.status),
  });

  // Pausa entre 1 y 2 segundos para simular comportamiento humano
  sleep(1);
}
