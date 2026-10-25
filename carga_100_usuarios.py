import asyncio
import time
import json
import statistics
import urllib.request
import urllib.error

# Prueba de carga de 100 usuarios concurrentes (RNF-007, CC-03)
BASE_URL = "https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/api/v1"
CONCURRENT_USERS = 10000
DURATION_SECONDS = 60  # 1 minuto de estrés constante a 100 usuarios

latencies = []
status_counts = {}
errors_count = 0
total_requests = 0

def make_sync_request(endpoint, method="GET", body=None):
    url = f"{BASE_URL}{endpoint}"
    req = urllib.request.Request(url, method=method)
    if body:
        req.add_header('Content-Type', 'application/json')
        data = json.dumps(body).encode('utf-8')
    else:
        data = None
    
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, data=data, timeout=10) as response:
            t1 = time.time()
            return response.status, t1 - t0
    except urllib.error.HTTPError as e:
        t1 = time.time()
        return e.code, t1 - t0
    except Exception as e:
        t1 = time.time()
        return 0, t1 - t0

async def user_worker(worker_id, end_time):
    global total_requests, errors_count
    loop = asyncio.get_running_loop()
    
    endpoints = [
        ("/health", "GET", None),
        ("/marcas", "GET", None),
        ("/negocios/cercanos?ubicacion=19.3790,-99.1640", "GET", None)
    ]
    
    while time.time() < end_time:
        for ep, method, payload in endpoints:
            if time.time() >= end_time:
                break
            status, duration = await loop.run_in_executor(None, make_sync_request, ep, method, payload)
            total_requests += 1
            latencies.append(duration)
            status_counts[status] = status_counts.get(status, 0) + 1
            if status == 0 or status >= 500:
                errors_count += 1
            await asyncio.sleep(0.5)

async def main():
    print(f"================================================================")
    print(f"  H2ToGo - Prueba de Carga CC-03 (RNF-007)")
    print(f"  Destino: {BASE_URL}")
    print(f"  Usuarios simultaneos: {CONCURRENT_USERS}")
    print(f"  Duracion: {DURATION_SECONDS} segundos")
    print(f"================================================================")
    print("Iniciando hilos virtuales...")
    
    start_time = time.time()
    end_time = start_time + DURATION_SECONDS
    
    tasks = [asyncio.create_task(user_worker(i, end_time)) for i in range(CONCURRENT_USERS)]
    await asyncio.gather(*tasks)
    
    total_time = time.time() - start_time
    rps = total_requests / total_time if total_time > 0 else 0
    error_rate = (errors_count / total_requests * 100) if total_requests > 0 else 0
    
    latencies.sort()
    p50 = statistics.median(latencies) if latencies else 0
    p95 = latencies[int(len(latencies) * 0.95)] if latencies else 0
    p99 = latencies[int(len(latencies) * 0.99)] if latencies else 0
    
    print("\n------------------- RESULTADOS DE LA MEDICION -------------------")
    print(f"Total de peticiones procesadas : {total_requests}")
    print(f"Tiempo total de ejecucion      : {total_time:.2f} s")
    print(f"Peticiones por segundo (RPS)   : {rps:.2f} req/s")
    print(f"Distribucion de codigos HTTP   : {status_counts}")
    print(f"Errores (500s o caidas)        : {errors_count} ({error_rate:.2f}%)")
    print(f"Latencia promedio (p50)        : {p50*1000:.1f} ms")
    print(f"Latencia percentil 95 (p95)    : {p95*1000:.1f} ms")
    print(f"Latencia percentil 99 (p99)    : {p99*1000:.1f} ms")
    print("-----------------------------------------------------------------")
    
    if error_rate < 1.0 and p95 < 5.0:
        print("[RESULTADO: PASO] El backend soporta 10000 usuarios con <1% de error.")
    else:
        print("[RESULTADO: REVISAR] Verifique la tasa de errores o la latencia.")

if __name__ == "__main__":
    asyncio.run(main())
