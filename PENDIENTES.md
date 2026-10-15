# Pendientes H2ToGo

## Desarrollo

### Importante para la entrega
- [ ] **Recuperar contraseña.** Primero, restablecimiento desde el panel admin; después, código por correo (Azure Communication Services o Brevo). Twilio de prueba solo permite 5 números.
- [ ] **Error en el registro:** si falla la carga de purificadoras, la app muestra "Purificadora Demo" y puede asociar al repartidor al negocio 1 (`RegistroScreen.kt`, respaldo `?: 1`).
- [ ] **RN-030:** no permitir cargar ni entregar garrafones con menos de 7 días de vigencia. Hoy solo se valida al registrar el lote; carga, apartado y entrega solo excluyen los vencidos.
- [ ] **RN-013:** historial de estados inmutable. Falta un trigger o REVOKE de UPDATE/DELETE en `historial_estados_pedido` y quitar el `ON DELETE CASCADE`.
- [ ] **CU-002 S2.2:** si el login responde `TELEFONO_NO_VERIFICADO`, llevar a la pantalla del código OTP con la opción de reenviar.
- [ ] **RF-026:** permitir solicitar el cambio de foto de perfil desde la app (el backend ya acepta `FOTO_PERFIL`).

### Antes de la entrega final
- [ ] Quitar el OTP fijo `123456` y conectar el SMS real (Twilio).
- [ ] Cambiar la contraseña `Demo1234` del admin en producción.
- [ ] Desplegar en Azure el backend más reciente (ruta por calles del seguimiento, baja forzada de repartidor, aviso "Llegué" al cliente y heart-beats del WebSocket, etc.).
- [ ] Decidir los mapas: hoy OSM es la capa principal. La capa de Google (`mt1.google.com`) no es oficial; usar la oficial (Map Tiles API / SDK con clave) o quitarla.

### iOS
- [ ] Notificaciones push con la app cerrada (APNs + Firebase). Requiere Apple Developer Program (99 USD/año). Las notificaciones con la app abierta ya funcionan.

### Opcional
- [ ] Evidencia de RNF-007: prueba de carga con 100 usuarios simultáneos (no subir el pool de conexiones a 100).

## Pruebas
- [ ] **Android completo** tras las fases 3 y 4 (pantallas compartidas, selectores de fecha y hora, mapas, barra superior de la ruta, notificaciones con la app abierta).
- [ ] **iOS en iPhone real:** aviso "Tu repartidor llegó", notificaciones tras bloquear o salir de la app, encabezados sin franja blanca, GPS en segundo plano, mapa para elegir dirección (arrastrar el pin), búsqueda de direcciones, abrir Maps o Waze y el aviso de "sin conexión".
- [ ] Capturas para `docs/Validacion_Casos_de_Uso.docx`.

## Tesis (cambios de texto)
- [ ] RF-018, CU-017, introducción, 2.2.5, 3.1.1 y tabla de tecnologías: Google Maps → OpenStreetMap con Leaflet (sí se puede abrir la ruta en Google Maps o Waze).
- [ ] RF-013 y RN-005: cancelar solo en "Pendiente" o "Programado" (no en "Asignado"); quitar "se libera al repartidor" y "se liberan los apartados".
- [ ] RN-009 (recálculo con varios pedidos): bajar el alcance a una entrega activa por ruta o pasarlo a trabajo a futuro.
- [ ] RN-023 y RN-028: quitar que los pedidos "expiran" o pasarlo a trabajo a futuro.
- [ ] RN-027: el pedido programado se activa al llegar la hora programada (el job revisa cada minuto).
- [ ] Introducción de 4.3: "aplicación web" → aplicación móvil.
- [ ] CU-014: quitar el correo de bienvenida con credenciales o marcarlo como trabajo a futuro.
- [ ] CU-012: el repartidor pulsa "Llegué" y el sistema valida que esté a 50 m o menos.
- [ ] Opcional: agregar la versión para iOS (Kotlin Multiplatform) como alcance extra o trabajo a futuro.
