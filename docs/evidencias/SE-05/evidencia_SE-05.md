# Evidencia SE-05 — Bloqueo de rastreo ajeno (WebSocket STOMP y REST)

## Contexto
- **Fecha de ejecución:** 2026-10-31 17:22:03 (UTC-6)
- **Ejecutó:** Equipo H2ToGo (verificación automatizada contra entorno real)
- **Commit:** `2df9a0a`
- **Ambiente:** Azure Container Apps (h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io + h2togo-db en rg-h2togo)
- **Dispositivo / Cliente:** Cliente HTTPS/TLS 1.3 + WebSocket STOMP 1.2 (`wss://`) + `psql` en contenedor `h2togo-db`

- **Requisito validado:** RNF-008, CU-006 (RN-016, `IT-06 TrackingFlowIT`)
- **Resultado:** **Pasó**

## Procedimiento ejecutado
1. Se puso el pedido `#985` en estado `en_camino` (perteneciente al Cliente 1 y asignado al Repartidor de prueba).
2. **Cliente 2 (ajeno)** abrió conexión TLS WebSocket (`wss://.../ws`), se autenticó por STOMP (`CONNECT`) e intentó suscribirse a `/topic/pedidos/985/ubicacion`.
3. El interceptor `StompAuthChannelInterceptor` rechazó la suscripción devolviendo un frame `ERROR` y cerrando el canal.
4. **Cliente 2 (ajeno)** intentó consultar también el fallback REST `GET /api/v1/pedidos/985/ubicacion-repartidor` -> rechazado con `403 Forbidden`.
5. **Cliente 1 (dueño legítimo)** se suscribió al mismo tópico `/topic/pedidos/985/ubicacion`, recibió confirmación `CONNECTED` y recibió en tiempo real el frame `MESSAGE` con las coordenadas publicadas por el repartidor (`200 OK` en REST).

## Evidencia capturada

### 1) Suscripción STOMP de cliente ajeno (`SUBSCRIBE /topic/pedidos/985/ubicacion`)
```text
TEXT:
ERROR
message:No tiene permiso para suscribirse a /topic/pedidos/985/ubicacion.
content-length:0
```

### 2) Consulta REST de ubicación por cliente ajeno (`GET /api/v1/pedidos/985/ubicacion-repartidor`)
```http
HTTP 403
{"timestamp":"2026-10-31T23:22:26.442140109Z","status":403,"codigo":"ACCESO_DENEGADO","mensaje":"No tiene permiso para esta operación.","detalles":[]}
```

### 3) Conexión STOMP y recepción `MESSAGE` en vivo por el cliente dueño legítimo
```text
CONNECTED
version:1.2
heart-beat:10000,10000
user-name:131

MESSAGE
destination:/topic/pedidos/985/ubicacion
content-type:application/json
subscription:sub-dueno
message-id:ed3e5a56-fbd8-4681-becb-1201829c67cb-1
content-length:73

{"lon":-99.178,"timestamp":"2026-10-31T23:22:25.985399376Z","lat":19.375}
```

### 4) Consulta REST de ubicación por el cliente dueño legítimo
```http
HTTP 200
{"lon":-99.178,"lat":19.375,"timestamp":"2026-10-31T23:22:25.823104Z"}
```
