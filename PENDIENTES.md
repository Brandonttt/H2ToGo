# Pendientes H2ToGo

## Desarrollo

### Importante para la entrega
- [ ] **Error en el registro:** si falla la carga de purificadoras, la app muestra "Purificadora Demo" y puede asociar al repartidor al negocio 1 (`RegistroScreen.kt`, respaldo `?: 1`).
- [ ] **RN-030:** no permitir cargar ni entregar garrafones con menos de 7 días de vigencia. Hoy solo se valida al registrar el lote; carga, apartado y entrega solo excluyen los vencidos.
- [ ] **CU-002 S2.2:** si el login responde `TELEFONO_NO_VERIFICADO`, llevar a la pantalla del código OTP con la opción de reenviar.
- [ ] **RF-026:** permitir solicitar el cambio de foto de perfil desde la app (el backend ya acepta `FOTO_PERFIL`).

### Antes de la entrega final
- [ ] Quitar el OTP fijo `123456` (`H2TOGO_OTP_DEMO` vacío) y conectar el SMS real (Twilio). **Urgente:** con el código fijo, cualquiera que sepa un correo puede cambiar esa contraseña desde "¿Olvidaste tu contraseña?". Twilio de prueba solo permite 5 números.
- [ ] Cambiar la contraseña `Demo1234` del admin en producción.
- [ ] **Antes de desplegar**, correr en la base de Azure, en este orden: `h2togo-backend/sql/migraciones/2026-10-25_aprobacion_repartidores.sql` y `2026-10-26_v7.sql` (esquema v7 y roles de seguridad). Sin ellas el backend no arranca. Ver `docs/BASE_DE_DATOS_v7.md`.
- [ ] Recomendado: crear el usuario `h2togo_app` (al final de la migración v7) y cambiar `DB_USER`/`DB_PASSWORD` del backend, para que el backend no entre con el dueño del esquema.
- [ ] Desplegar en Azure el backend más reciente (aprobación de repartidores por el dueño, cambiar y recuperar contraseña, ruta por calles del seguimiento, baja forzada de repartidor, aviso "Llegué" al cliente, heart-beats del WebSocket y ruta del día con OR-Tools, etc.). La imagen crece unos 60 MB por los binarios nativos de OR-Tools.
- [ ] Decidir los mapas: hoy OSM es la capa principal. La capa de Google (`mt1.google.com`) no es oficial; usar la oficial (Map Tiles API / SDK con clave) o quitarla.

### iOS
- [ ] Notificaciones push con la app cerrada (APNs + Firebase). Requiere Apple Developer Program (99 USD/año). Las notificaciones con la app abierta ya funcionan.

### Opcional
- [ ] Evidencia de RNF-007: prueba de carga con 100 usuarios simultáneos (no subir el pool de conexiones a 100).

## Pruebas
- [ ] **Android completo** tras las fases 3 y 4 (pantallas compartidas, selectores de fecha y hora, mapas, barra superior de la ruta, notificaciones con la app abierta).
- [ ] **iOS en iPhone real:** aviso "Tu repartidor llegó", notificaciones tras bloquear o salir de la app, encabezados sin franja blanca, GPS en segundo plano, mapa para elegir dirección (arrastrar el pin), búsqueda de direcciones, abrir Maps o Waze y el aviso de "sin conexión".
- [ ] **Aprobación de repartidores:** registrar un repartidor que se une a una purificadora existente; ve "Esperando aprobación"; al dueño le llega la notificación y lo aprueba (o rechaza) en su perfil; el panel admin lo marca "Por aprobar".
- [ ] **Contraseña:** cambiarla desde el perfil (cliente y repartidor) y recuperarla desde el login con el código por SMS.
- [ ] **Ruta del día (OR-Tools):** con 3 o más programados de hoy, reservarlos, revisar el orden y el mapa numerado, cancelar uno desde el cliente (la ruta se recalcula) y entregar parada por parada.
- [ ] Capturas para `docs/Validacion_Casos_de_Uso.docx`.

## Tesis (cambios de texto)
- [ ] Capítulo de base de datos: reemplazar el diagrama E/R y el diccionario de datos por los de v7 (`docs/BASE_DE_DATOS_v7.md`): sin `negocios.id_dueno` (ahora `repartidores.es_dueno`), apartados y movimientos con lote + vehículo, `inventario_vehiculo` con llave (vehículo, lote), sin `solicitudes.id_producto_negocio`, sin tabla `administradores`, lotes ligados al producto del negocio, `movimientos_inventario` como bitácora sin FKs, sin `pedidos.id_vehiculo_utilizado`, token de sesión guardado como hash (18 tablas, 24 relaciones). Agregar la sección de seguridad en la BD (roles por actor, permisos por columna y RLS) como respuesta a RNF-008.
- [ ] RN-013: ya se cumple en la BD (el historial y los movimientos rechazan UPDATE y DELETE).
- [ ] RF-018, CU-017, introducción, 2.2.5, 3.1.1 y tabla de tecnologías: Google Maps → OpenStreetMap con Leaflet (sí se puede abrir la ruta en Google Maps o Waze).
- [ ] RF-013 y RN-005: cancelar solo en "Pendiente" o "Programado" (no en "Asignado"); quitar "se libera al repartidor" y "se liberan los apartados".
- [ ] RN-009: ya implementada. Si el cliente cancela un pedido reservado, se avisa al repartidor y su ruta se recalcula sin él.
- [ ] RN-023 y RN-028: quitar que los pedidos "expiran" o pasarlo a trabajo a futuro.
- [ ] RN-027: el pedido programado se activa al llegar la hora programada (el job revisa cada minuto). Cambia "ocultos hasta su activación": los programados del día aparecen en la agenda de los repartidores de purificadoras compatibles, que pueden reservarlos; los demás repartidores no los ven.
- [ ] Nuevo requerimiento (agenda y ruta óptima): el repartidor reserva programados del día y el sistema calcula el orden de visita con OR-Tools (problema del viajero con ventanas de horario de ±30 min, capacidad del vehículo y ruta abierta que termina en la última entrega). Agregar OR-Tools a la tabla de tecnologías y explicar la combinación: A*/Dijkstra para distancias por calles y OR-Tools para el orden.
- [ ] CU-001: si el repartidor se une a una purificadora existente, queda "pendiente de aprobación" hasta que el dueño lo acepta (o lo rechaza y su cuenta se da de baja). Mientras tanto no ve pedidos ni inicia jornada. Agregar una regla de negocio para esto.
- [ ] CU-002: agregar el flujo "¿Olvidaste tu contraseña?" (código por SMS y contraseña nueva; se cierran las sesiones abiertas) y "Cambiar contraseña" desde el perfil.
- [ ] Introducción de 4.3: "aplicación web" → aplicación móvil.
- [ ] CU-014: quitar el correo de bienvenida con credenciales o marcarlo como trabajo a futuro.
- [ ] CU-012: el repartidor pulsa "Llegué" y el sistema valida que esté a 50 m o menos.
- [ ] Opcional: agregar la versión para iOS (Kotlin Multiplatform) como alcance extra o trabajo a futuro.
