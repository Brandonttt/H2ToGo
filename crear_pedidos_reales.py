import urllib.request
import urllib.error
import json
import time
import sys

try:
    sys.stdout.reconfigure(encoding='utf-8')
except Exception:
    pass

BASE_URL = "https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/api/v1"

def http_req(endpoint, method="GET", body=None, token=None):
    url = f"{BASE_URL}{endpoint}"
    req = urllib.request.Request(url, method=method)
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', f"Bearer {token}")
    
    data = json.dumps(body).encode('utf-8') if body else None
    try:
        with urllib.request.urlopen(req, data=data, timeout=15) as res:
            res_body = res.read().decode('utf-8')
            return res.status, json.loads(res_body) if res_body else {}
    except urllib.error.HTTPError as e:
        err_body = e.read().decode('utf-8')
        try:
            parsed = json.loads(err_body)
        except Exception:
            parsed = {"raw": err_body}
        return e.code, parsed
    except Exception as e:
        return 0, {"error": str(e)}

def main():
    print("==================================================")
    print("  H2ToGo: Creacion de Pedidos Reales en Azure")
    print("==================================================")
    
    # 1. Login con el cliente: chavez@gmail.com
    print("\n[1/4] Iniciando sesion con el cliente chavez@gmail.com...")
    login_payload = {
        "correo": "chavez@gmail.com",
        "password": "password123",
        "forzar": True
    }
    status, res = http_req("/auth/login", "POST", login_payload)
    if status != 200:
        login_payload["password"] = "12345678"
        status, res = http_req("/auth/login", "POST", login_payload)
        
    if status != 200:
        print(f"[ERROR] Error en login del cliente ({status}): {res}")
        return
        
    token = res.get("token")
    perfil = res.get("perfil", {})
    print(f"[OK] Login exitoso! Cliente: {perfil.get('nombre')} {perfil.get('apellidos', '')}")

    # 2. Obtener direcciones del cliente (/clientes/me/direcciones)
    print("\n[2/4] Consultando direcciones de entrega guardadas...")
    status, dirs = http_req("/clientes/me/direcciones", "GET", token=token)
    id_direccion = None
    if status == 200 and isinstance(dirs, list) and len(dirs) > 0:
        id_direccion = dirs[0]["id"]
        calle = dirs[0].get('calleNumero')
        col = dirs[0].get('colonia')
        print(f"[OK] Direccion encontrada: {calle}, {col} (ID: {id_direccion})")
    else:
        print("     No tenia direcciones guardadas, creando una en Benito Juarez...")
        nueva_dir = {
            "alias": "Domicilio",
            "calleNumero": "Av Cuauhtémoc 123",
            "colonia": "Narvarte Poniente",
            "codigoPostal": "03020",
            "alcaldiaMunicipio": "Benito Juárez",
            "latitud": 19.3980,
            "longitud": -99.1550,
            "esPredeterminada": True
        }
        status, dir_creada = http_req("/clientes/me/direcciones", "POST", nueva_dir, token=token)
        if status in [200, 201]:
            id_direccion = dir_creada["id"]
            print(f"[OK] Direccion creada (ID: {id_direccion})")
        else:
            print(f"[ERROR] Error creando direccion: {dir_creada}")
            return

    # 3. Obtener marcas disponibles
    print("\n[3/4] Buscando marcas disponibles...")
    status, marcas = http_req("/marcas", "GET", token=token)
    id_marca = marcas[0]["id"] if (isinstance(marcas, list) and len(marcas) > 0) else 1
    marca_nombre = marcas[0]["nombre"] if (isinstance(marcas, list) and len(marcas) > 0) else "Generica"
    print(f"[OK] Marca seleccionada: {marca_nombre} (ID: {id_marca})")

    # 4. Crear 3 pedidos reales
    print("\n[4/4] Creando 3 pedidos reales...")
    pedidos_creados = []
    for i in range(1, 4):
        pedido_payload = {
            "tipoSolicitud": "abierta",
            "precioMaximoGarrafon": 45.0,
            "idDireccionEntrega": id_direccion,
            "indicaciones": f"Pedido #{i} creado por cliente chavez para prueba en vivo",
            "detalles": [
                {
                    "idMarca": id_marca,
                    "cantidad": 2,
                    "tieneEnvase": True
                }
            ]
        }
        status, pedido_res = http_req("/pedidos", "POST", pedido_payload, token=token)
        if status in [200, 201]:
            id_ped = pedido_res.get("id")
            total = pedido_res.get("total")
            estado = pedido_res.get("estado")
            pedidos_creados.append(id_ped)
            print(f"[EXITO] Pedido #{i} CREADO EXITOSAMENTE! -> ID Pedido: {id_ped} | Total: ${total} | Estado: {estado}")
        else:
            print(f"[AVISO] Pedido #{i} respondio ({status}): {pedido_res}")
        time.sleep(0.5)

    print("\n==================================================")
    print(f"Resumen: Se crearon {len(pedidos_creados)} pedidos reales en Azure.")
    print("Ahora entra a tu Panel de Administracion web o a la App:")
    print(f"Veras los pedidos con IDs: {pedidos_creados}")
    print("==================================================")

if __name__ == "__main__":
    main()
