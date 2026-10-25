import asyncio
import time
import json
import random
import statistics
import urllib.request
import urllib.error
import sys

try:
    sys.stdout.reconfigure(encoding='utf-8')
except Exception:
    pass

BASE_URL = "https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/api/v1"
CONCURRENT_USERS = 100
DURATION_SECONDS = 300  # 5 minutos (o puedes probarlo con 60s)

latencies = []
status_counts = {}
db_errors_count = 0
http_errors_count = 0
total_requests = 0

def http_sync(endpoint, method="GET", body=None, token=None):
    url = f"{BASE_URL}{endpoint}"
    req = urllib.request.Request(url, method=method)
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', f"Bearer {token}")
    
    data = json.dumps(body).encode('utf-8') if body else None
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, data=data, timeout=12) as response:
            t1 = time.time()
            return response.status, t1 - t0
    except urllib.error.HTTPError as e:
        t1 = time.time()
        return e.code, t1 - t0
    except Exception:
        t1 = time.time()
        return 0, t1 - t0

async def user_worker(user_id, token_cli, id_direccion, token_rep, end_time):
    global total_requests, db_errors_count, http_errors_count
    loop = asyncio.get_running_loop()
    
    while time.time() < end_time:
        # 1. Healthcheck
        st, d = await loop.run_in_executor(None, http_sync, "/health", "GET", None, None)
        record_metric(st, d)
        
        # 2. Listar disponibles (Repartidor)
        st, d = await loop.run_in_executor(None, http_sync, "/repartidores/me/pedidos-disponibles", "GET", None, token_rep)
        record_metric(st, d)
        
        # 3. Negocios cercanos y marcas (Cliente)
        st, d = await loop.run_in_executor(None, http_sync, "/marcas", "GET", None, token_cli)
        record_metric(st, d)
        
        # 4. Crear pedido (Cliente)
        payload_pedido = {
            "tipoSolicitud": "abierta",
            "precioMaximoGarrafon": 45.0,
            "idDireccionEntrega": id_direccion,
            "indicaciones": f"Pedido carga CC-03 hilo {user_id}",
            "detalles": [{"idMarca": 1, "cantidad": 1, "tieneEnvase": True}]
        }
        st, d = await loop.run_in_executor(None, http_sync, "/pedidos", "POST", payload_pedido, token_cli)
        record_metric(st, d)
        
        # 5. Reportar ubicacion / GPS
        lat = 19.3700 + random.random() * 0.01
        lon = -99.1800 + random.random() * 0.01
        st, d = await loop.run_in_executor(None, http_sync, f"/negocios/cercanos?ubicacion={lat:.4f},{lon:.4f}", "GET", None, token_cli)
        record_metric(st, d)
        
        await asyncio.sleep(random.uniform(1.0, 2.5))

def record_metric(status, duration):
    global total_requests, db_errors_count, http_errors_count
    total_requests += 1
    latencies.append(duration)
    status_counts[status] = status_counts.get(status, 0) + 1
    
    if status == 500 or status == 0:
        db_errors_count += 1
        http_errors_count += 1
    elif status >= 400 and status not in [401, 404, 409, 422]:
        http_errors_count += 1

async def main():
    print("=====================================================================")
    print("  H2ToGo - Prueba CC-03: Carga de 100 Usuarios Simultaneos (RNF-007)")
    print(f"  Destino: {BASE_URL}")
    print(f"  Usuarios Concurrentes: {CONCURRENT_USERS}")
    print(f"  Duracion Programada   : {DURATION_SECONDS} segundos (5 minutos)")
    print("  Operaciones mezcladas: Login, Listar, Crear Pedidos, GPS")
    print("=====================================================================")
    
    # Setup de tokens
    print("\n[Setup] Autenticando usuarios base en Azure...")
    def login(correo, pwd):
        req = urllib.request.Request(f"{BASE_URL}/auth/login", method="POST")
        req.add_header('Content-Type', 'application/json')
        body = json.dumps({"correo": correo, "password": pwd, "forzar": True}).encode('utf-8')
        try:
            with urllib.request.urlopen(req, data=body, timeout=10) as r:
                return json.loads(r.read().decode('utf-8')).get("token")
        except Exception:
            return None

    token_cli = login("chavez@gmail.com", "password123") or login("chavez@gmail.com", "12345678")
    token_rep = login("leo@gmail.com", "password123") or login("leo@gmail.com", "12345678")
    
    if not token_cli or not token_rep:
        print("[ERROR] No se pudo obtener tokens de cliente o repartidor. Revisa credenciales.")
        return
        
    print("[OK] Tokens obtenidos exitosamente para Cliente y Repartidor.")
    
    # Obtener direccion del cliente
    req_dir = urllib.request.Request(f"{BASE_URL}/clientes/me/direcciones")
    req_dir.add_header('Authorization', f"Bearer {token_cli}")
    try:
        with urllib.request.urlopen(req_dir, timeout=10) as r:
            dirs = json.loads(r.read().decode('utf-8'))
            id_direccion = dirs[0]["id"] if (dirs and len(dirs) > 0) else 1
    except Exception:
        id_direccion = 1
    print(f"[OK] Direccion de entrega vinculada: ID {id_direccion}")
    
    print(f"\n[Arranque] Lanzando {CONCURRENT_USERS} usuarios concurrentes...")
    start_time = time.time()
    end_time = start_time + DURATION_SECONDS
    
    tasks = [asyncio.create_task(user_worker(i, token_cli, id_direccion, token_rep, end_time)) for i in range(CONCURRENT_USERS)]
    await asyncio.gather(*tasks)
    
    total_time = time.time() - start_time
    rps = total_requests / total_time if total_time > 0 else 0
    error_rate = (http_errors_count / total_requests * 100) if total_requests > 0 else 0
    
    latencies.sort()
    p50 = statistics.median(latencies) if latencies else 0
    p95 = latencies[int(len(latencies) * 0.95)] if latencies else 0
    p99 = latencies[int(len(latencies) * 0.99)] if latencies else 0
    
    print("\n================== REPORTE FORMAL DE MEDICION CC-03 ==================")
    print(f"Total de peticiones procesadas : {total_requests}")
    print(f"Tiempo total de ejecucion      : {total_time:.2f} s")
    print(f"Peticiones por segundo (RPS)   : {rps:.2f} req/s")
    print(f"Distribucion de codigos HTTP   : {status_counts}")
    print(f"Errores de base de datos (500) : {db_errors_count} (Esperado: 0)")
    print(f"Tasa de fallos HTTP            : {error_rate:.2f}% (Limite: < 1.00%)")
    print(f"Latencia mediana (p50)         : {p50*1000:.1f} ms")
    print(f"Latencia percentil 95 (p95)    : {p95*1000:.1f} ms")
    print(f"Latencia percentil 99 (p99)    : {p99*1000:.1f} ms")
    print("======================================================================")
    
    if db_errors_count == 0 and error_rate < 1.0:
        print("\n[RESULTADO: PASO] Se cumple formalmente RNF-007 y CC-03.")
    else:
        print("\n[RESULTADO: FALLO] Revise los errores registrados.")

if __name__ == "__main__":
    asyncio.run(main())
