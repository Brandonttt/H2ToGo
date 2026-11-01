# Evidencia SE-03 — Expiración de sesión por inactividad (30 días)

## Contexto
- **Fecha de ejecución:** 2026-10-31 17:22:03 (UTC-6)
- **Ejecutó:** Equipo H2ToGo (verificación automatizada contra entorno real)
- **Commit:** `2df9a0a`
- **Ambiente:** Azure Container Apps (h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io + h2togo-db en rg-h2togo)
- **Dispositivo / Cliente:** Cliente HTTPS/TLS 1.3 + WebSocket STOMP 1.2 (`wss://`) + `psql` en contenedor `h2togo-db`

- **Requisito validado:** RNF-004
- **Resultado:** **Pasó**

## Procedimiento ejecutado
1. Con una sesión activa válida, se movieron en `h2togo-db` las marcas temporales `sesion_fecha_creacion` (`now() - interval '61 days'`) y `sesion_fecha_expiracion` (`now() - interval '31 days'`), respetando la restricción `chk_usuarios_sesion` del esquema v7 (`sesion_fecha_expiracion > sesion_fecha_creacion`).
2. Se invocó un endpoint protegido REST (`GET /api/v1/clientes/me/direcciones`) con dicho token -> rechazado con `401 Unauthorized` (la app limpia sesión y redirige a Login).
3. Se intentó abrir sesión WebSocket STOMP (`CONNECT` en `wss://.../ws`) con el mismo token vencido -> rechazado por `StompAuthChannelInterceptor`.
4. Se volvió a iniciar sesión (`POST /api/v1/auth/login`, `forzar: false`) -> `200 OK` (no exige `forzar` porque la sesión previa ya estaba vencida).

## Salida real en BD y API Azure

### 1) Actualización de `sesion_fecha_expiracion` en `h2togo-db`
```text
(1 row)



 id_usuario |           correo            |     sesion_fecha_creacion     |    sesion_fecha_expiracion    

------------+-----------------------------+-------------------------------+-------------------------------

        132 | se.cli2.88923@h2togo-sec.mx | 2026-08-31 23:22:40.582813+00 | 2026-09-30 23:22:40.582813+00

(1 row)



UPDATE 1
```

### 2) Petición REST con el token vencido
```http
HTTP 401
{"timestamp":"2026-10-31T23:22:41.489951224Z","status":401,"codigo":"NO_AUTENTICADO","mensaje":"Se requiere autenticación para acceder a este recurso.","detalles":[]}
```

### 3) Conexión WebSocket STOMP con el token vencido
```text
TEXT: ERROR
message:Token de sesión inválido para el WebSocket.
content-length:0
```

### 4) Nuevo inicio de sesión tras expiración
```text
HTTP 200 (nueva sesión emitida tras expirar sin requerir forzar=true)
```
