# Guía de pruebas tras el cambio a la base de datos v7

Para Brandon. Qué volver a probar después del cambio a la base de datos v7 (commit `fece487`)
para confirmar que el sistema sigue funcionando igual. El detalle de qué se cambió y por qué
está en [docs/BASE_DE_DATOS_v7.md](docs/BASE_DE_DATOS_v7.md); aquí solo va qué probar y qué
resultado esperar.

---

## Qué cambió, en corto

- **Menos tablas y relaciones:** de 19 a 18 tablas y de 34 a 24 llaves foráneas. Ya no
  existen la tabla `administradores`, `negocios.id_dueno` (ahora el dueño se marca con
  `repartidores.es_dueno`), `pedidos.id_vehiculo_utilizado` ni
  `solicitudes_cambio_perfil.id_producto_negocio`.
- **Inventario:**
  - los apartados y los movimientos guardan el lote y el vehículo;
  - `inventario_vehiculo` tiene como llave (vehículo, lote);
  - un lote solo puede ser de una marca que el negocio vende;
  - `movimientos_inventario` es una bitácora sin llaves foráneas que no se puede modificar
    ni borrar.
- **Seguridad dentro de la base:** el backend entra a PostgreSQL con el rol del usuario de
  cada petición (`h2togo_cliente`, `h2togo_repartidor`, `h2togo_admin`, `h2togo_publico` o
  `h2togo_sistema` para las tareas programadas). Cada rol solo ve y modifica lo suyo. Si una
  parte del backend intenta algo que su rol no tiene permitido, la base lo bloquea. **Ese es
  el tipo de error que más hay que buscar** (ver la sección 5).
- **Sesión y cuenta:**
  - el token de sesión se guarda como hash SHA-256, no en texto;
  - el código SMS se invalida después de 5 intentos fallidos.
- **22 verificaciones nuevas (CHECK):** algunos datos incoherentes que antes entraban ahora
  se rechazan; por ejemplo, un pedido entregado sin fecha de entrega.

**Ya probado:**
- las 111 pruebas automáticas del backend;
- las 21 verificaciones de `pruebas_v7.sql`;
- la prueba de contrato de la app (14 endpoints y el WebSocket) contra un backend v7 local;
- la migración v6 → v7 sobre una base con datos.

**Falta:** recorrer a mano los flujos completos, sobre todo desde la app.

---

## 1. Preparar el entorno

1. Actualiza tu copia:

   ```bash
   git pull
   ```

2. **Recrea la base local.** Tu volumen de Docker tiene el esquema v6 y el backend ya no
   arranca contra él: Hibernate valida el esquema al iniciar. Esto borra los datos locales y
   vuelve a cargar el esquema v7 con los datos demo:

   ```bash
   cd h2togo-backend
   docker compose down -v
   docker compose up -d --build
   ```

   Con Docker, el backend queda en **http://localhost:8088** (`docker-compose.yml` publica
   el 8088). Si lo corres con Maven (opción B de [PRUEBAS.md](PRUEBAS.md)), queda en el 8080.

   | Herramienta | Dirección |
   |---|---|
   | Consola de pruebas | `/console/` |
   | Panel de administración | `/admin/` |
   | Swagger | `/swagger-ui.html` |

3. **Usuarios demo.** Todos usan la contraseña `Demo1234`. El código SMS de demo es `123456`.

   | Id | Correo | Rol |
   |---|---|---|
   | 1 | `admin@h2togo.mx` | Administrador |
   | 2 | `dueno@h2togo.mx` | Repartidor, dueño de "Purificadora Demo" (negocio 1, vehículo 1, marca 1, lote de 100) |
   | 3 | `cliente@h2togo.mx` | Cliente, con dirección 1 |

   Varias pruebas necesitan un **segundo negocio** y un **segundo cliente**. Regístralos desde
   la app o desde la consola.

4. **Para revisar la base** (las consultas de "Revisar en la BD" de cada sección):

   ```bash
   docker exec -it h2togo-db psql -U h2togo -d h2togo_v6
   ```

   Ese usuario es el dueño del esquema y ve todo. La base sigue llamándose `h2togo_v6`; solo
   cambió el esquema.

5. **App móvil.** La app apunta a Azure (`API_AZURE` en
   `h2togo-mobile/shared/src/commonMain/kotlin/com/htogo/shared/data/ClienteHttp.kt`), y
   Azure sigue en v6 hasta que corramos la migración. Tienes dos opciones:
   - **Esperar a que migremos Azure** y probar la app ahí. Es lo más realista.
   - **Apuntarla temporalmente a tu backend local** cambiando esa constante (no la
     commitees):

     | Dispositivo | URL |
     |---|---|
     | Emulador de Android | `http://10.0.2.2:8088/api/v1/` |
     | Simulador de iOS | `http://localhost:8088/api/v1/` |
     | Teléfono físico | `http://<IP de tu computadora>:8088/api/v1/` |

     La app ya permite HTTP local en Android e iOS.

---

## 2. Pruebas automáticas (primero)

Si alguna de estas falla, avísame antes de seguir con las manuales.

```bash
cd h2togo-backend
mvn verify
```

- [ ] Termina con **111 pruebas y 0 fallos**. Necesita Docker encendido para las de
  integración.
- [ ] `SeguridadBdIT` pasa. Es la nueva: comprueba que un cliente no ve inventario y que un
  repartidor no ve datos de otro negocio, entre otras cosas.

Verificaciones del esquema contra una base desechable (no toca tu volumen):

```bash
docker run -d --name h2test -e POSTGRES_PASSWORD=pass -e POSTGRES_DB=h2 -p 55432:5432 postgis/postgis:16-3.4
```
```bash
docker exec -i h2test psql -U postgres -d h2 -v ON_ERROR_STOP=1 < H2ToGo_v7_postgresql.sql
```
```bash
docker exec -i h2test psql -U postgres -d h2 < pruebas_v7.sql
```
```bash
docker rm -f h2test
```

- [ ] El tercer comando imprime 21 líneas `OK ...` y ningún `FALLO`.

Contrato de la app contra tu backend local (con el `docker compose` del paso 1 arriba):

```bash
cd h2togo-mobile
H2TOGO_API=http://localhost:8088/api/v1/ ./gradlew :shared:testDebugUnitTest --tests '*ContratoBackendTest*'
```

- [ ] Pasan las 2 pruebas. En el reporte deben salir los 14 endpoints con `200` y
  `STOMP -> Conectado`.

---

## 3. Pruebas manuales por prioridad

Prueba primero las de prioridad **Alta**: son las que más cambiaron por dentro. Para cada
paso anota el resultado. Si algo falla, guarda lo que pide la sección 5.

| # | Área | Qué cambió por dentro | Prioridad |
|---|---|---|---|
| 3.1 | Sesión y cuenta | Token guardado como hash, límite de intentos del código | Alta |
| 3.2 | Registro de repartidores y dueño | `es_dueno` en lugar de `negocios.id_dueno`; aprobar y rechazar ahora pasan por funciones de la BD | Alta |
| 3.3 | Inventario y jornada | Llaves de inventario, lotes ligados al catálogo, devolución | Alta |
| 3.4 | Pedidos del cliente | Seguridad por fila (RLS), `es_programado` calculado por la BD | Media |
| 3.5 | Pedidos disponibles y aceptación | Qué pedidos ve cada repartidor; apartados con lote y vehículo | Alta |
| 3.6 | Entrega | Vehículo de la entrega sin `id_vehiculo_utilizado`; consumo de apartados | Alta |
| 3.7 | Seguimiento | Vehículo visible solo mientras el pedido está activo | Media |
| 3.8 | Perfil de la purificadora | Stock y repartidores salen de vistas; sin vehículos | Media |
| 3.9 | Solicitudes y panel de administración | Sin tabla `administradores`; revisor y dueño nuevos | Alta |
| 3.10 | Notificaciones y tareas programadas | Corren como `h2togo_sistema` | Media |

### 3.1 Sesión y cuenta

- [ ] **Login** con los tres usuarios demo, en la app y en la consola.
- [ ] **La sesión sigue** al cerrar y volver a abrir la app.
- [ ] **Sesión única.** Entra con el mismo usuario en otro dispositivo o en la consola. Debe
  responder `409 SESION_ACTIVA_EN_OTRO_DISPOSITIVO`, y con `forzar` debe cerrar la sesión
  anterior.
- [ ] **Logout.** Después de cerrar sesión, cualquier petición con ese token da `401`.
- [ ] **Registro de cliente** con el código `123456`.
- [ ] **Límite de 5 intentos.** Registra otro usuario y escribe 5 códigos equivocados. Al
  sexto intento, **incluso el código correcto** debe fallar con `OTP_INVALIDO`. Después pide
  *Reenviar código* y verifica con el nuevo: debe funcionar.
- [ ] **Recuperar contraseña** (código por SMS) y entrar con la nueva.
- [ ] **Cambiar contraseña** desde el perfil. Con la actual equivocada debe dar
  `CONTRASENA_ACTUAL_INCORRECTA`, y si es igual a la actual, `CONTRASENA_IGUAL`.
- [ ] **Editar perfil** (nombre, teléfono) y registrar el token FCM (se hace solo al entrar).

**Revisar en la BD.** `token_sesion_hash` debe tener 64 caracteres hexadecimales y **no**
ser igual al token que devolvió el login:

```sql
SELECT correo, token_sesion_hash, codigo_intentos FROM usuarios;
```

### 3.2 Registro de repartidores y dueño

- [ ] **Repartidor con negocio nuevo** ("Purificadora B"). Debe quedar como **dueño** y la app
  debe mostrarle las opciones de dueño (miembros, solicitudes, precios).
- [ ] **Repartidor que se une a un negocio existente** (al negocio 1). Queda **pendiente**: al
  intentar aceptar pedidos debe recibir `PENDIENTE_APROBACION`.
- [ ] **El dueño lo aprueba** desde la app (miembros del negocio). Después ya puede iniciar
  jornada y aceptar pedidos.
- [ ] **El dueño rechaza a otro pendiente.** Ese usuario ya no puede entrar (cuenta inactiva).
- [ ] **Un repartidor que no es dueño** intenta aprobar o rechazar
  (`POST /api/v1/negocios/me/repartidores/{id}/aprobacion`). Debe ser rechazado.
- [ ] **La lista de miembros** marca correctamente quién es el dueño.

**Revisar en la BD.** Debe haber un solo dueño por negocio:

```sql
SELECT r.id_negocio, u.correo, r.es_dueno, r.aprobado
FROM repartidores r JOIN usuarios u USING (id_usuario) ORDER BY 1;
```

### 3.3 Inventario y jornada

- [ ] **Lote de una marca que no está en el catálogo.** Con el dueño de "Purificadora B"
  (aún sin productos), registra un lote. Debe dar `PRODUCTO_NO_CONFIGURADO`.
- [ ] **Agregar el producto al catálogo.** Pide *Agregar producto* (solicitud
  `AGREGAR_PRODUCTO`) y apruébalo en `/admin/`. Ahora el lote sí se registra.
- [ ] **Iniciar jornada** con un vehículo propio y una carga inicial. El inventario de la base
  baja y el del vehículo sube en la misma cantidad.
- [ ] **Iniciar jornada con un vehículo de otro negocio.** Desde la consola o REST, usa el id
  de un vehículo ajeno. Debe dar `VEHICULO_INVALIDO`.
- [ ] **Cargar más** al vehículo. Si supera la capacidad, `CAPACIDAD_VEHICULO_EXCEDIDA`; si
  no hay stock, `STOCK_INSUFICIENTE`.
- [ ] **Salida manual** (merma) desde la base.
- [ ] **Fin de jornada (devolución) con un pedido asignado pendiente de entregar.** Regresa a
  la base **solo lo libre**. Lo apartado para ese pedido se queda en el vehículo.
- [ ] **Pantallas de inventario en la app** (base y vehículo). Los números cuadran con lo
  anterior.

**Revisar en la BD.** Cada movimiento tiene `id_lote`, y `id_vehiculo` cuando aplica:

```sql
SELECT id_lote, id_vehiculo, cantidad_actual, cantidad_apartada FROM inventario_vehiculo;
SELECT id_lote, cantidad_actual, cantidad_apartada FROM lotes_inventario;
SELECT id_movimiento, tipo, id_lote, id_vehiculo, cantidad, id_pedido
FROM movimientos_inventario ORDER BY id_movimiento DESC;
```

**La bitácora no se puede modificar.** Con al menos un movimiento registrado, esto debe dar
error:

```sql
UPDATE movimientos_inventario SET cantidad = 1;
```

### 3.4 Pedidos del cliente

- [ ] **Crear pedidos de cada modalidad:** directa (a una purificadora), abierta y
  programada. El programado debe aparecer como programado.
- [ ] **Cancelar** un pedido pendiente. Uno ya en camino no se puede cancelar
  (`PEDIDO_NO_CANCELABLE`).
- [ ] **Historial** del pedido y lista "mis pedidos".
- [ ] **Pedido ajeno.** Con el segundo cliente crea un pedido y anota su id. Con
  `cliente@h2togo.mx` pide su detalle (`GET /api/v1/pedidos/{id}`). Ahora debe responder
  **`404`**; antes era `403`. Es el cambio esperado: el cliente ni siquiera puede saber que
  ese pedido existe.
- [ ] **Direcciones:** alta, edición y baja. Una dirección fuera de Benito Juárez se rechaza.

### 3.5 Pedidos disponibles y aceptación

Para esta sección necesitas los dos negocios con jornada iniciada y stock.

- [ ] **Pedido directo al negocio 1.** Lo ve el repartidor del negocio 1 y **no** lo ve el
  repartidor de "Purificadora B".
- [ ] **Pedido abierto.** Lo ven los repartidores de ambos negocios (si están dentro del
  rango).
- [ ] **Aceptar sin jornada.** Debe dar `SIN_JORNADA`.
- [ ] **Aceptar con stock en el vehículo.** El pedido pasa a `asignado` y `cantidad_apartada`
  sube en el vehículo.
- [ ] **Aceptar sin stock en el vehículo, pero con stock en la base.** El apartado queda en
  la base. Al marcar *En camino* debe dar `CARGA_PENDIENTE_EN_BASE`. Después de *Cargar
  apartados* sí deja salir.
- [ ] **Nuevo comportamiento, apartados en la base:**
  1. acepta un pedido cuyo apartado quede en la base;
  2. cierra la jornada;
  3. intenta *Cargar apartados*: debe dar `SIN_JORNADA`;
  4. inicia jornada con **otro** vehículo y vuelve a *Cargar apartados*: los garrafones deben
     cargarse a ese vehículo nuevo.
- [ ] **Dos repartidores aceptan el mismo pedido casi a la vez.** Solo uno lo obtiene.

### 3.6 Entrega

- [ ] **Entregado.**
  - el pedido pasa a `entregado` con fecha de entrega;
  - baja el stock del vehículo y desaparece su apartado;
  - aparece un movimiento `salida_pedido` con el **vehículo** de la entrega.
- [ ] **Entregado con una línea agregada en sitio** (el cliente pide un garrafón más). Solo
  usa stock **libre** del vehículo. Si lo único que queda está apartado para otro pedido,
  debe dar `SIN_STOCK_ENSITIO`.
- [ ] **No entregado con motivo.** El pedido pasa a `no_entregado` y el apartado se libera
  (`cantidad_apartada` vuelve a bajar). Sin motivo debe dar `MOTIVO_REQUERIDO`.
- [ ] **"Llegué".** Avisa al cliente una sola vez.
- [ ] **Agenda y ruta del día** del repartidor, y reservar o cancelar un pedido programado
  (`/pedidos/{id}/reserva`).

**Revisar en la BD:**

```sql
SELECT id_pedido, estado_actual, id_repartidor, fecha_entrega FROM pedidos ORDER BY 1 DESC;
SELECT * FROM apartados_pedido;
```

### 3.7 Seguimiento

- [ ] **Mientras el pedido está `asignado` o `en_camino`**, el cliente ve en la app al
  repartidor, el vehículo y las placas (si tiene), su ubicación en vivo y la ruta.
- [ ] **Al quedar `entregado` o `no_entregado`**, el seguimiento ya **no** muestra el vehículo
  (nuevo comportamiento) y la pantalla no truena.
- [ ] **WebSocket.** El cliente se suscribe y recibe la ubicación que publica el repartidor.
  También se puede probar en la consola con dos pestañas.

### 3.8 Perfil de la purificadora

- [ ] **Como cliente**, abre el perfil de una purificadora. Muestra productos, precios, stock
  disponible, horario y cuántos repartidores tiene. **Ya no muestra vehículos ni placas**; la
  pantalla debe verse bien sin ellos.
- [ ] **Negocios cercanos** (con y sin ubicación) y el estado abierto/cerrado.
- [ ] **Como dueño**, "Mi negocio" **sí** sigue mostrando sus vehículos (pantalla de inventario
  del vehículo).
- [ ] **El dueño cambia el precio** de un producto y edita horarios. El cliente ve el cambio.

### 3.9 Solicitudes y panel de administración

Desde la app del dueño, crea una solicitud de cada tipo. Resuélvelas en `/admin/` y comprueba
que el cambio se aplica:

- [ ] **`AGREGAR_PRODUCTO`:** el producto aparece en el perfil.
- [ ] **`AGREGAR_VEHICULO`**, **`DATOS_VEHICULO`** y **`ELIMINAR_VEHICULO`.**
- [ ] **`NOMBRE_NEGOCIO`**, **`DIRECCION_BASE`** y **`FOTO_PERFIL`.**
- [ ] **Rechazo con comentario:** el dueño ve el comentario. Sin comentario debe dar
  `COMENTARIO_REQUERIDO`.

Después, en el panel:

- [ ] **Alta de un administrador nuevo.** Puede entrar y usar el panel; ahora un administrador
  es solo una fila de `usuarios` con rol `admin`.
- [ ] **Baja de un repartidor con pedidos asignados.** Sin forzar debe avisar. Forzando, sus
  pedidos regresan a `pendiente`, con su historial, y sus apartados se liberan.
- [ ] **Baja del dueño con transferencia** a otro repartidor del negocio. El nuevo queda como
  dueño (aprobado). **Sin transferencia**, el negocio queda inactivo.
- [ ] **Reactivación** de un usuario dado de baja.
- [ ] **Dashboard, lista de usuarios, pedidos, negocios y marcas.** Cargan sin error y en
  negocios aparece el dueño correcto.
- [ ] **Ruteo A\*** desde la consola (requiere admin).

**Revisar en la BD.** Cada solicitud resuelta guarda quién la revisó:

```sql
SELECT id_solicitud, codigo_cambio, estado, id_admin_revisor FROM solicitudes_cambio_perfil;
```

### 3.10 Notificaciones y tareas programadas

- [ ] **Pedido nuevo:** les llega la notificación a los repartidores. Ahora se envía cuando
  termina de guardarse el pedido.
- [ ] **Asignado, en camino, "Llegué", entregado y no entregado:** le llega la notificación al
  cliente.
- [ ] **Pedido programado:** a su hora pasa a disponible (la tarea corre cada minuto).
- [ ] **Sin probar a mano:** la limpieza de sesiones vencidas (3:00 am) y los lotes por caducar
  (3:30 am). Las cubre `SchedulingFlowIT`. Solo revisa que el log del backend no muestre
  errores de esas tareas.

---

## 4. Seguridad en la base (opcional, para entender lo nuevo)

Puedes ver los permisos por rol directamente en `psql` (paso 1.4). Cada bloque se hace pasar
por un usuario demo:

```sql
-- Como el cliente demo (id 3)
SET ROLE h2togo_cliente;
SET h2togo.id_usuario = '3';
SELECT count(*) FROM lotes_inventario;                    -- ERROR: permission denied
SELECT * FROM v_existencias_negocio;                      -- sí: solo lo disponible por marca
UPDATE usuarios SET rol = 'admin' WHERE id_usuario = 3;   -- ERROR: permission denied
RESET ROLE;

-- Como el dueño demo (id 2)
SET ROLE h2togo_repartidor;
SET h2togo.id_usuario = '2';
SELECT count(*) FROM lotes_inventario;                    -- solo los lotes de su negocio
SELECT count(*) FROM direcciones_clientes;                -- 0: no ve direcciones sin pedido suyo
UPDATE solicitudes_cambio_perfil SET estado = 'aprobado'; -- ERROR: solo el admin resuelve
DELETE FROM movimientos_inventario;                       -- ERROR: la bitácora no se borra
RESET ROLE;
```

---

## 5. Si algo falla

**Cómo reconocer el origen del error.** En el log del backend
(`docker compose logs -f app`) busca:

| Mensaje en el log | Qué significa |
|---|---|
| `permission denied for table ...` | El rol del usuario no tiene permiso para esa tabla o columna |
| `new row violates row-level security policy` | Intentó escribir una fila que no es suya |
| `violates check constraint "chk_..."` | Una de las verificaciones nuevas rechazó el dato |
| `violates foreign key constraint "fk_..."` | La fila apunta a algo que no existe |
| `Solo el dueño...` / `insufficient_privilege` | Una función de la BD rechazó la operación |

**Prueba rápida para confirmar si es un permiso de la BD.** Repite el paso con la seguridad
por actor apagada:
- con Docker, agrega temporalmente `H2TOGO_BD_ROL_POR_ACTOR: "false"` en el `environment`
  del servicio `app` de `docker-compose.yml` y vuelve a levantarlo;
- con Maven, antepón `H2TOGO_BD_ROL_POR_ACTOR=false` al `mvn spring-boot:run`.

Si con eso funciona, falta un permiso en la BD. Si sigue fallando, es otro bug. **No
commitees ese cambio**: en Azure debe quedar encendido.

**Qué mandarme por cada falla:**
1. Usuario y rol con el que lo probaste, y si fue en la app, en la consola o por REST.
2. Pasos exactos (ids de pedido, negocio y vehículo).
3. Código HTTP y JSON de la respuesta (`codigo` y `mensaje`).
4. Las líneas del log del backend alrededor del error.

---

## 6. Migración de datos (solo si quieres probarla)

En Azure la migración la corro yo antes de desplegar (ver `PENDIENTES.md`). Si quieres ver que
una base v6 **con datos** pasa bien a v7, puedes hacerlo en un contenedor desechable:

```bash
git show ec73953:H2ToGo_v6_postgresql.sql > /tmp/h2_v6.sql
git show ec73953:data-demo.sql > /tmp/h2_demo_v6.sql
docker run -d --name h2mig -e POSTGRES_USER=h2togo -e POSTGRES_PASSWORD=h2togo -e POSTGRES_DB=h2togo_v6 -p 55433:5432 postgis/postgis:16-3.4
```

Espera a que la base esté lista (unos segundos) y carga v6 con sus datos:

```bash
for f in /tmp/h2_v6.sql h2togo-backend/sql/zona_benito_juarez.sql /tmp/h2_demo_v6.sql; do docker exec -i h2mig psql -q -U h2togo -d h2togo_v6 -v ON_ERROR_STOP=1 < $f; done
```

Aplica las migraciones en el mismo orden que en Azure:

```bash
docker exec -i h2mig psql -U h2togo -d h2togo_v6 -v ON_ERROR_STOP=1 < h2togo-backend/sql/migraciones/2026-10-25_aprobacion_repartidores.sql
```
```bash
docker exec -i h2mig psql -U h2togo -d h2togo_v6 -v ON_ERROR_STOP=1 < h2togo-backend/sql/migraciones/2026-10-26_v7.sql
```

- [ ] **Termina sin error.** Los `NOTICE ... skipping` son normales.
- [ ] **Quedan 18 tablas y 24 llaves foráneas**, y `dueno@h2togo.mx` tiene `es_dueno = true`:

```sql
SELECT count(*) FROM information_schema.tables
WHERE table_schema = 'public' AND table_type = 'BASE TABLE' AND table_name <> 'spatial_ref_sys';
SELECT count(*) FROM pg_constraint c JOIN pg_namespace n ON n.oid = c.connamespace
WHERE n.nspname = 'public' AND c.contype = 'f';
```

Al terminar: `docker rm -f h2mig`.

**Después de migrar Azure, revisa también:**
- [ ] Los usuarios que ya tenían sesión en la app **siguen dentro sin volver a entrar**. La
  migración convierte sus tokens a hash.
- [ ] Repite en la app las pruebas de prioridad Alta de la sección 3.

---

## 7. Lista final

- [ ] Sección 2: pruebas automáticas, `pruebas_v7.sql` y contrato de la app, en verde.
- [ ] Sección 3: todas las de prioridad Alta, en local o en Azure.
- [ ] Sección 3: las de prioridad Media.
- [ ] Sección 6: la app contra Azure después de la migración.
- [ ] Fallas reportadas con lo que pide la sección 5.
