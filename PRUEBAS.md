# H2ToGo — Guía de pruebas

Cómo levantar todo, probar el sistema a mano (consola web + REST + WebSocket) y correr la
batería de pruebas automatizadas. Trabajo Terminal 2026-B176.

- **Backend:** Spring Boot 4.1 / Java 21 (Corretto) / Maven, en `h2togo-backend/`.
- **Base de datos:** PostgreSQL 16 + PostGIS 3 (esquema v6).
- **Motor de ruteo:** A\* sobre el grafo OSM real de la Alcaldía Benito Juárez.

---

## 0. Requisitos

Hay dos formas de levantarlo. Elige una:

- **Opción A — Todo en Docker (recomendada para clonar y correr).** Solo necesitas **Docker
  Desktop**. El backend se construye y ejecuta en contenedor (JDK/Maven van dentro), así que
  **no** hace falta instalar Java ni Maven.
- **Opción B — Backend local (para desarrollar).** BD en Docker + backend con Maven en tu
  máquina; necesitas **JDK 21** y **Maven 3.9+** además de Docker.

| Herramienta | ¿Cuándo? |
|---|---|
| **Docker Desktop** | Siempre (BD, y en la opción A también el backend) |
| **JDK 21** (Corretto) | Solo opción B, y para correr `mvn verify` (pruebas) |
| **Maven 3.9+** | Solo opción B / pruebas |
| Navegador | Consola web de pruebas |

> Los comandos asumen que estás en la raíz del repo salvo que se indique `cd h2togo-backend`.

---

## 1A. Opción A — Todo en Docker (solo Docker)

```bash
cd h2togo-backend
docker compose up -d --build
```

Esto construye la imagen del backend, levanta PostGIS (sembrando esquema + zona + datos demo
la primera vez) y arranca el backend en **http://localhost:8080**. Luego abre la consola en
**http://localhost:8080/console/**.

```bash
docker compose logs -f app     # ver el arranque del backend (y el OTP en dev: [SMS-DEV])
docker compose up -d db        # SOLO la base de datos (si prefieres la opción B)
docker compose down            # detener (conserva el volumen de datos)
docker compose down -v         # borrar también el volumen → reinicializa esquema + datos demo
```

- ¿Puerto ocupado? publica en otro: `APP_PORT=8081 DB_PORT=5433 docker compose up -d --build`
  (y abre la consola en ese `APP_PORT`).
- El backend reintenta si arranca antes de que la BD termine de sembrar (`restart: on-failure`).
- Tras cambiar código, reconstruye con `docker compose up -d --build`.

> Las **pruebas automatizadas** (sección 4) sí requieren JDK 21 + Maven en la máquina; la
> opción A cubre ejecutar y demostrar la aplicación, no correr `mvn verify`.

---

## 1B. Opción B — Base de datos en Docker (backend local)

El `docker-compose.yml` levanta PostGIS y, la **primera** vez que se crea el volumen, ejecuta
en orden: (1) el esquema v6, (2) el polígono de cobertura de Benito Juárez y (3) los **datos
demo** (`data-demo.sql`).

```bash
cd h2togo-backend
docker compose up -d db
```

Esto deja una BD `h2togo_v6` en `localhost:5432` (usuario `h2togo`, contraseña `h2togo`)
ya sembrada. Continúa en la sección 2 para arrancar el backend con Maven.

```bash
docker compose down      # detiene (conserva los datos en el volumen)
docker compose down -v   # borra el volumen → al volver a subir, reinicializa y re-siembra
```

### Credenciales demo

Todos los usuarios demo comparten la contraseña **`Demo1234`** y ya vienen verificados:

| Rol | Correo | Notas |
|---|---|---|
| Administrador | `admin@h2togo.mx` | Panel admin y motor de ruteo |
| Dueño / repartidor | `dueno@h2togo.mx` | Negocio 1 "Purificadora Demo", vehículo 1, producto 1, lote de 100 |
| Cliente | `cliente@h2togo.mx` | Dirección 1 en cobertura (destino del grafo) |

Ids fijos que usa la consola por defecto: negocio `1`, marca `1`, vehículo `1`, dirección `1`,
producto `1`.

---

## 2. Levantar el backend con Maven (opción B)

Con la BD del paso 1B arriba (perfil `dev` apunta a `localhost:5432/h2togo_v6` por defecto):

```bash
cd h2togo-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

El backend queda en **http://localhost:8080**. Al arrancar carga el grafo OSM (~27 000 nodos)
en unos cientos de milisegundos.

- ¿Puerto 8080 ocupado? añade `-Dspring-boot.run.arguments=--server.port=8081`.
- ¿BD en otro host/puerto? exporta `DB_URL`, `DB_USER`, `DB_PASSWORD` antes de `mvn`.
- El **OTP** de verificación no se envía por SMS en desarrollo: aparece en la consola del
  backend con el prefijo `[SMS-DEV]`.

---

## 3. Consola web de pruebas  ⭐

Una página servida por el propio backend para ejercitar todo el sistema sin la app Android.

**Ábrela en:** http://localhost:8080/console/  (también responde `/` y `/console`).

La columna derecha (**Registro de peticiones**) muestra cada llamada con su método, ruta,
código de estado y respuesta. Paneles:

- **Sesión** — base URL, registro rápido, verificación de OTP y login (guarda el token).
- **Ruteo A\*** — mapa de Benito Juárez; fija origen/destino (clic o coordenadas) y calcula
  la ruta: dibuja la polilínea y muestra la distancia. *Requiere token de administrador.*
- **Cliente** — direcciones, negocios cercanos, crear pedido (directo/abierto/programado),
  listar, detalle+historial, cancelar.
- **Repartidor** — precio, horario, lote, jornada y carga; disponibles, aceptar, en camino,
  reportar ubicación, ver la ruta del pedido en el mapa, registrar la entrega.
- **Tracking** — conexión WebSocket/STOMP para publicar y ver la ubicación en vivo.
- **Admin** — usuarios, pedidos, solicitudes y una petición REST libre.

### 3.1 Guion de demo (flujo feliz completo)

1. **Ruteo (admin).** Sesión → login `admin@h2togo.mx` / `Demo1234`. Pestaña **Ruteo A\*** →
   *Calcular ruta*. Debe dibujar la ruta (~0.65 km) entre los dos puntos por defecto.
2. **Abrir jornada (dueño).** Login `dueno@h2togo.mx`. Pestaña **Repartidor** → *Iniciar
   jornada* (vehículo 1, carga 20). Opcional: *Ver carga del vehículo*.
3. **Crear pedido (cliente).** Login `cliente@h2togo.mx`. Pestaña **Cliente** → modalidad
   *Directa* → *Crear pedido*. Anota el `id` del pedido en la respuesta.
4. **Atender (dueño/repartidor).** Login `dueno@h2togo.mx`. Pestaña **Repartidor**, escribe el
   id del pedido:
   - *Aceptar* → estado `asignado`.
   - *Marcar en camino* → `en_camino`.
   - *Enviar ubicación*.
   - *Ver ruta en mapa* → dibuja la ruta del pedido.
   - *Registrar resultado* (ENTREGADO) → `entregado`.
5. **Tracking en vivo (opcional).** Abre la consola en **dos** pestañas del navegador: en una,
   logueado como cliente, *Conectar WS* + *Suscribirse* al id del pedido; en la otra, como
   repartidor, *Conectar WS* + *Publicar* ubicación. La ubicación aparece en la del cliente.

> Para volver a empezar limpio: `docker compose down -v && docker compose up -d`.

---

## 4. Pruebas automatizadas (unitarias + integración)

Las pruebas de integración usan **Testcontainers**, que levanta su propio PostGIS efímero
(con esquema + zona) — **necesitan Docker corriendo**. No usan la BD del paso 1.

```bash
cd h2togo-backend
mvn verify        # compila, corre unitarias (surefire) e integración (failsafe)
```

- Sin Docker, las de integración se **saltan** (`@Testcontainers(disabledWithoutDocker=true)`)
  y solo corren las unitarias: `mvn test`.
- Una sola clase: `mvn -Dit.test=E2EFlujoFelizIT verify`.

Qué cubre la suite (todas en verde con Docker):

| Área | Prueba(s) |
|---|---|
| Flujo feliz E2E (registro→…→entrega) + abierta/programada | `e2e/E2EFlujoFelizIT` |
| Seguridad (401 sin token, 403 rol equivocado, errores sin fugas) | `e2e/SeguridadIT` |
| Export de OpenAPI a `docs/openapi.json` | `e2e/OpenApiExportIT` |
| Auth y sesión (OTP, sesión única, expiración deslizante) | `auth/AuthFlowIT` |
| Direcciones y cobertura (polígono real de BJ) | `direcciones/DireccionesFlowIT` |
| Negocios, catálogo y horarios | `negocios/NegociosFlowIT` |
| Inventario y jornada (concurrencia FIFO) | `inventario/InventarioFlowIT` |
| Pedidos (directa/abierta, cancelación, historial) | `pedidos/PedidosFlowIT` |
| Asignación y entrega (concurrencia RF-019, suspensión RN-006) | `pedidos/AsignacionEntregaFlowIT` |
| Rastreo WebSocket STOMP | `tracking/TrackingFlowIT` |
| Ruta A\* sobre el grafo OSM real | `routing/RutaFlowIT` |
| Administración (altas/bajas, solicitudes) | `admin/AdministracionFlowIT` |
| Tareas programadas (reloj inyectable) | `scheduling/SchedulingFlowIT` |
| Modelo de datos y ciclo diferido | `ModeloDatosIT` |

---

## 5. Colecciones `.http` (REST Client)

En `h2togo-backend/http/` hay una colección por área (auth, direcciones, negocios, inventario,
pedidos, asignación-entrega, ruta, tracking, solicitudes, admin) con peticiones de ejemplo.
Ábrelas con la extensión **REST Client** de VS Code (o impórtalas a Postman) y ejecútalas
contra `http://localhost:8080` con el backend arriba.

---

## 6. Swagger / OpenAPI

Con el backend arriba:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Spec JSON:** http://localhost:8080/v3/api-docs

El spec se exporta además a **`docs/openapi.json`** cada vez que corre `mvn verify` (lo genera
`OpenApiExportIT`), como evidencia versionada.

---

## 7. Pruebas del esquema SQL (independientes del backend)

`pruebas_v6.sql` valida triggers, cálculo de cobertura/proximidad y las restricciones que la
BD debe rechazar (cada bloque negativo imprime `OK ...`). Contra una BD desechable:

```bash
docker run -d --name h2test -e POSTGRES_PASSWORD=pass -e POSTGRES_DB=h2 -p 55432:5432 postgis/postgis:16-3.4
# esperar a que esté lista, luego:
docker exec -i h2test psql -U postgres -d h2 -v ON_ERROR_STOP=1 < H2ToGo_v6_postgresql.sql
docker exec -i h2test psql -U postgres -d h2 < pruebas_v6.sql
docker rm -f h2test
```

---

## 8. Problemas frecuentes

| Síntoma | Causa / solución |
|---|---|
| `Port 8080 already in use` | Otra app en 8080 → usa `--server.port=8081` y abre `/console/` en ese puerto. |
| Login demo da 401/422 | ¿Levantaste la BD con `data-demo.sql`? (`docker compose down -v && up -d`). La contraseña es `Demo1234`. |
| El ruteo da 422 desde la consola | Origen/destino fuera del grafo. Usa los valores por defecto o puntos dentro de Benito Juárez. |
| `mvn verify` se salta la integración | Docker no está corriendo. Arráncalo y reintenta. |
| La JVM se queda sin memoria en el build | Ya está acotada en `.mvn/jvm.config`; cierra otras apps pesadas si persiste. |
| No veo el OTP al registrar por la consola | En dev no hay SMS: míralo en la salida del backend (`[SMS-DEV] Código de verificación …`). Los usuarios demo ya vienen verificados. |

---

## 9. Estructura relevante

```
H2ToGo_v6_postgresql.sql              esquema v6 (PostgreSQL + PostGIS)
pruebas_v6.sql                        pruebas del esquema
data-demo.sql                         datos demo para la consola
docs/openapi.json                     spec OpenAPI exportado (evidencia)
PLAN_BACKEND.md                       plan de desarrollo y decisiones (§10)
h2togo-backend/
  Dockerfile                          imagen del backend (build con JDK 21 + runtime JRE 21)
  docker-compose.yml                  BD PostGIS + init (esquema, zona, demo) y servicio del backend
  sql/zona_benito_juarez.sql          polígono real de cobertura (OSM, ODbL)
  http/*.http                         colecciones REST Client por área
  src/main/resources/static/console/  consola web de pruebas (F13)
  src/test/java/.../e2e/              pruebas transversales (E2E, seguridad, OpenAPI)
```
