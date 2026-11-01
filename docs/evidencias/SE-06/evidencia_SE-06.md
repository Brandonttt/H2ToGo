# Evidencia SE-06 — Rechazo de entrega lejos del domicilio (> 50 m)

## Contexto
- **Fecha de ejecución:** 2026-10-31 17:22:03 (UTC-6)
- **Ejecutó:** Equipo H2ToGo (verificación automatizada contra entorno real)
- **Commit:** `2df9a0a`
- **Ambiente:** Azure Container Apps (h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io + h2togo-db en rg-h2togo)
- **Dispositivo / Cliente:** Cliente HTTPS/TLS 1.3 + WebSocket STOMP 1.2 (`wss://`) + `psql` en contenedor `h2togo-db`

- **Requisito validado:** RF-014 (`IT-02 AsignacionEntregaFlowIT`)
- **Resultado:** **Pasó**

## Procedimiento ejecutado
1. Pedido `#985` en estado `en_camino`, con domicilio de entrega en `(19.372, -99.178)` (Col. Del Valle, Benito Juárez).
2. El repartidor asignado intentó avisar `POST /api/v1/pedidos/985/llegada` y registrar `POST /api/v1/pedidos/985/resultado` (`ENTREGADO`) desde `(19.377, -99.178) [~555 m > 50 m]` (~555 m del domicilio).
3. PostGIS (`ST_DWithin(..., 50)`) rechazó ambas operaciones con **`HTTP 422`** y código **`FUERA_DE_RANGO`**.
4. A continuación, el repartidor registró la entrega a `(19.37205, -99.178) [~5.5 m <= 50 m]` (~5.5 m del domicilio, dentro del radio de 50 m) -> **`HTTP 200 OK`** (`estado: "entregado"`).

## Respuestas reales del servidor en Azure

### 1) Intento de aviso "Llegué" a ~555 m (`POST /api/v1/pedidos/985/llegada`)
```http
HTTP 422
{"timestamp":"2026-10-31T23:22:27.578149839Z","status":422,"codigo":"FUERA_DE_RANGO","mensaje":"Debes estar a menos de 50 m del domicilio para entregar (RF-014).","detalles":[]}
```

### 2) Intento de marcar "Entregado" a ~555 m (`POST /api/v1/pedidos/985/resultado`)
```http
HTTP 422
{"timestamp":"2026-10-31T23:22:28.297540019Z","status":422,"codigo":"FUERA_DE_RANGO","mensaje":"Debes estar a menos de 50 m del domicilio para entregar (RF-014).","detalles":[]}
```

### 3) Control positivo: entrega dentro del radio (a ~5.5 m <= 50 m)
```http
HTTP 200
{"id":985,"estado":"entregado","tipoSolicitud":"directa","idNegocioSolicitado":111,"idRepartidor":133,"idDireccionEntrega":113,"precioMaximoGarrafon":null,"totalPagar":50.00,"garrafonesTotales":2,"indicaciones":null,"esProgramado":false,"fechaProgramada":null,"fechaCreacion":"2026-10-31T23:22:20.819309Z","detalles":[{"id":985,"idMarca":1,"nombreMarca":"Agua Demo 20L","cantidadSolicitada":2,"cantidadEntregada":2,"tieneEnvase":true,"precioUnitario":25.00,"precioEnvaseUnitario":null,"agregadaEnSitio":false}],"historial":[{"estado":"pendiente","fechaCambio":"2026-10-31T23:22:20.819309Z","notas":null},{"estado":"asignado","fechaCambio":"2026-10-31T23:22:21.7721Z","notas":null},{"estado":"en_camino","fechaCambio":"2026-10-31T23:22:23.331735Z","notas":null},{"estado":"entregado","fechaCambio":"2026-10-31T23:22:28.732127Z","notas":null}],"nombreCliente":"Prueba Seguridad","telefonoCliente":"5591088923","direccionTexto":"Pilares 100, Del Valle","latEntrega":19.372,"lonEntrega":-99.178,"nombreNegocio":"PurifSeg88923","repartidor":{"nombre":"Prueba Seguridad","telefono":null,"negocio":"PurifSeg88923","vehiculo":null,"placas":null}}
```
