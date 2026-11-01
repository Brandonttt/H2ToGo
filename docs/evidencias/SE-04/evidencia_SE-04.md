# Evidencia SE-04 — Control de acceso basado en roles (RBAC)

## Contexto
- **Fecha de ejecución:** 2026-10-31 17:22:03 (UTC-6)
- **Ejecutó:** Equipo H2ToGo (verificación automatizada contra entorno real)
- **Commit:** `2df9a0a`
- **Ambiente:** Azure Container Apps (h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io + h2togo-db en rg-h2togo)
- **Dispositivo / Cliente:** Cliente HTTPS/TLS 1.3 + WebSocket STOMP 1.2 (`wss://`) + `psql` en contenedor `h2togo-db`

- **Requisito validado:** RNF-008 (`IT-05 SeguridadIT`)
- **Resultado:** **Pasó**

## Matriz ejecutada contra Azure (`h2togo-api`)

| Caso probado | Esperado | Obtenido | Cuerpo de respuesta |
|---|---|---|---|
| Sin token -> Ruta Admin (GET /admin/pedidos) | `401` | `401` | `{"timestamp":"2026-10-31T23:22:09.996509828Z","status":401,"codigo":"NO_AUTENTICADO","mensaje":"Se requiere autenticació` |
| Cliente -> Ruta Repartidor (GET /inventario/base) | `403` | `403` | `{"timestamp":"2026-10-31T23:22:10.421791388Z","status":403,"codigo":"ACCESO_DENEGADO","mensaje":"No tiene permiso para e` |
| Cliente -> Ruta Repartidor (GET /inventario/vehiculo) | `403` | `403` | `{"timestamp":"2026-10-31T23:22:10.892704410Z","status":403,"codigo":"ACCESO_DENEGADO","mensaje":"No tiene permiso para e` |
| Cliente -> Ruta Repartidor (PUT /repartidores/me/ubicacion) | `403` | `403` | `{"timestamp":"2026-10-31T23:22:11.325708059Z","status":403,"codigo":"ACCESO_DENEGADO","mensaje":"No tiene permiso para e` |
| Cliente -> Ruta Repartidor (GET /negocios/me/repartidores) | `403` | `403` | `{"timestamp":"2026-10-31T23:22:11.759554175Z","status":403,"codigo":"ACCESO_DENEGADO","mensaje":"No tiene permiso para e` |
| Cliente -> Ruta Admin (GET /admin/pedidos) | `403` | `403` | `{"timestamp":"2026-10-31T23:22:12.175745801Z","status":403,"codigo":"ACCESO_DENEGADO","mensaje":"No tiene permiso para e` |
| Cliente -> Ruta Admin (GET /admin/usuarios) | `403` | `403` | `{"timestamp":"2026-10-31T23:22:12.587470008Z","status":403,"codigo":"ACCESO_DENEGADO","mensaje":"No tiene permiso para e` |
| Cliente -> Ruta Admin (GET /admin/solicitudes) | `403` | `403` | `{"timestamp":"2026-10-31T23:22:13.016949300Z","status":403,"codigo":"ACCESO_DENEGADO","mensaje":"No tiene permiso para e` |
| Repartidor -> Ruta Cliente (GET /clientes/me/direcciones) | `403` | `403` | `{"timestamp":"2026-10-31T23:22:13.429369179Z","status":403,"codigo":"ACCESO_DENEGADO","mensaje":"No tiene permiso para e` |
| Repartidor -> Ruta Admin (GET /admin/usuarios) | `403` | `403` | `{"timestamp":"2026-10-31T23:22:13.849862692Z","status":403,"codigo":"ACCESO_DENEGADO","mensaje":"No tiene permiso para e` |

Todas las peticiones sin credenciales recibieron `401` y todos los cruces de rol (`cliente` hacia rutas de `repartidor` o `admin`, y `repartidor` hacia rutas de `cliente` o `admin`) recibieron `403 Forbidden` sin filtrar trazas internas.
