# Evidencia SE-02 — Sesión única por cuenta

## Contexto
- **Fecha de ejecución:** 2026-10-31 17:22:03 (UTC-6)
- **Ejecutó:** Equipo H2ToGo (verificación automatizada contra entorno real)
- **Commit:** `2df9a0a`
- **Ambiente:** Azure Container Apps (h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io + h2togo-db en rg-h2togo)
- **Dispositivo / Cliente:** Cliente HTTPS/TLS 1.3 + WebSocket STOMP 1.2 (`wss://`) + `psql` en contenedor `h2togo-db`

- **Requisito validado:** RNF-006, RF-016 (RN-018)
- **Resultado:** **Pasó**

## Procedimiento ejecutado
1. **Dispositivo A** inicia sesión (`forzar: false`) y consume `GET /api/v1/clientes/me/direcciones` -> `200 OK`.
2. **Dispositivo B** intenta iniciar sesión con la misma cuenta y `forzar: false` -> el servidor rechaza con `409 Conflict` (`SESION_ACTIVA_EN_OTRO_DISPOSITIVO` / sesión activa).
3. **Dispositivo B** confirma cierre de la otra sesión enviando `forzar: true` -> `200 OK` con un nuevo token opaco.
4. **Dispositivo A** intenta reutilizar su token anterior -> rechazado con `401 Unauthorized`.
5. **Dispositivo B** opera normalmente con su nuevo token -> `200 OK`.

## Respuestas reales del servidor en Azure

### 1) Dispositivo A con sesión activa inicial (`GET /api/v1/clientes/me/direcciones`)
```http
HTTP 200
[]
```

### 2) Dispositivo B intenta login sin forzar (`POST /api/v1/auth/login`, `forzar=false`)
```http
HTTP 409
{"timestamp":"2026-10-31T23:22:08.225038821Z","status":409,"codigo":"SESION_ACTIVA_EN_OTRO_DISPOSITIVO","mensaje":"Ya hay una sesión activa en otro dispositivo. Reintenta con forzar=true para cerrarla.","detalles":[]}
```

### 3) Dispositivo B fuerza inicio de sesión (`POST /api/v1/auth/login`, `forzar=true`)
```text
HTTP 200 (nuevo token emitido: AfjLH4Lj...)
```

### 4) Dispositivo A intenta usar su token desplazado (`GET /api/v1/clientes/me/direcciones`)
```http
HTTP 401
{"timestamp":"2026-10-31T23:22:09.113969996Z","status":401,"codigo":"NO_AUTENTICADO","mensaje":"Se requiere autenticación para acceder a este recurso.","detalles":[]}
```

### 5) Dispositivo B consulta con el nuevo token (`GET /api/v1/clientes/me/direcciones`)
```http
HTTP 200
[]
```
