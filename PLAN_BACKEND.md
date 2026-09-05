# Plan de desarrollo — Backend H2ToGo (Spring Boot)

**Proyecto:** Trabajo Terminal 2026-B176 · H2ToGo
**Fecha del plan:** 2026-07-23
**Base de datos:** esquema v6 (`H2ToGo_v6_postgresql.sql`, PostgreSQL 16 + PostGIS 3)
**Repositorio:** `H2ToGo` (rama `main`). En la raíz ya existe **`h2togo-routing/`: prototipo
FUNCIONAL del motor A*** — compila, corre, expone `POST /api/route`, incluye cliente Leaflet
de pruebas y ya trae datos OSM reales de la zona en `osm_data/sample_map.json` (~5.6 MB).
No se reescribe: se integra tal cual en la fase F9. También están en la raíz los scripts
`H2ToGo_v6_postgresql.sql` y `pruebas_v6.sql`. El backend nuevo se crea como carpeta hermana
`h2togo-backend/` en este mismo repositorio.
**Referencias obligadas:** TT1 2026-B176 v2 (casos de uso CU-001…CU-023 y reglas de negocio RN-xxx), esquema v6, prototipo `h2togo-routing/`.

> **Cómo usar este documento.** Es el plano de seguimiento del backend. Cada fase tiene
> tareas con checkbox, prerrequisitos y criterios de aceptación. El agente (o persona) que
> tome una fase debe: (1) leer los CU del TT que la fase referencia, (2) implementar,
> (3) dejar `mvn verify` en verde, (4) marcar los checkboxes y actualizar la tabla de estado.
> Nada se marca como hecho si los criterios de aceptación no se cumplen.

## Estado global

| Fase | Nombre | Depende de | Tamaño | Estado |
|---|---|---|---|---|
| F0 | Andamiaje del proyecto | — | S | En curso (código listo + `mvn verify` verde; falta smoke test runtime) |
| F1 | Modelo de datos (entidades + repositorios) | F0 | M | **Hecha** (`mvn verify` verde con Testcontainers: validate + ciclo diferido, 2026-08-22) |
| F2 | Seguridad y autenticación | F1 | M | **Hecha** (`mvn verify` verde con Docker: 10 unit + 7 IT, 2026-08-22) |
| F3 | Perfil y direcciones del cliente | F2 | S | **Hecha** (`mvn verify` verde con Docker: 10 unit + 13 IT; polígono real de OSM cargado) |
| F4 | Negocios, catálogo y horarios | F2 | M | **Hecha** (`mvn verify` verde con Docker: 10 unit + 20 IT) |
| F5 | Inventario y jornada del repartidor | F4 | M | **Hecha** (`mvn verify` verde con Docker: 10 unit + 25 IT, incl. concurrencia) |
| F6 | Pedidos (creación, cancelación, historiales) | F3, F4, F5 | L | **Hecha** (`mvn verify` verde con Docker: 10 unit + 33 IT) |
| F7 | Asignación y entrega | F6 | L | **Hecha** (`mvn verify` verde con Docker: 10 unit + 39 IT, incl. concurrencia RF-019 y suspensión RN-006) |
| F8 | Rastreo y notificaciones | F7 | M | **Hecha** (`mvn verify` verde con Docker: 12 unit + 41 IT, incl. WebSocket STOMP real) |
| F9 | Integración del motor de ruteo A* | F0 (código), F7 (endpoint) | S | **Hecha** (`mvn verify` verde con Docker: 14 unit + 42 IT; A* sobre grafo real de BJ) |
| F10 | Administración | F2 (mín.), F6/F7 (completa) | M | **Hecha** (`mvn verify` verde con Docker: 14 unit + 49 IT) |
| F11 | Tareas programadas | F6 | S | Hecha |
| F12 | Pruebas transversales y endurecimiento | todas | M | **Hecha** (`mvn verify` verde: 14 unit + 59 IT; prueba de 100 concurrentes diferida por Rubén) |
| F13 | Consola web de pruebas (admin) | F9, F12 | M | **Hecha** (consola en `/console/`, ruteo validado en vivo; `mvn verify` verde) |

---

## 0. Alcance

**Cubre:** la API REST del sistema para los tres actores (cliente, repartidor, administrador),
autenticación por token de sesión, WebSocket para rastreo de pedidos, notificaciones push
(FCM) y SMS (OTP) detrás de interfaces, tareas programadas del backend, y la integración del
motor de ruteo A* ya desarrollado.

**No cubre:** la aplicación Android (consume esta API), el panel de administración web
(consume esta API), la visualización con Google Maps (responsabilidad del cliente móvil,
CU-017), ni cambios al esquema v6 (si una fase los necesita, se documentan como propuesta
v6.x antes de aplicarlos — ver §10).

---

## 1. Decisiones técnicas

| Tema | Decisión | Justificación |
|---|---|---|
| Build | Maven, un solo `pom.xml` (proyecto único) | Requisito del equipo; un artefacto desplegable simplifica el TT |
| Servidor | **Tomcat embebido** (viene dentro de `spring-boot-starter-web`); empaquetado como jar ejecutable (`mvn spring-boot:run` en desarrollo, `java -jar` en despliegue) | No se instala ningún Tomcat externo: el servidor viaja dentro del artefacto, igual que en el prototipo de ruteo |
| Java | **21 (LTS)** | Requisito del equipo; soportado por Spring Boot 4.x |
| Spring Boot | **4.1.x** (la 3.5 terminó soporte OSS en junio 2026) | Proyecto nuevo en 2026-2027; 4.1 tiene soporte durante toda la vida del TT. El módulo de ruteo (hoy en 3.2.3/Java 17) se alinea al integrarse (F9) |
| Arquitectura | REST + MVC por capas: `Controller → Service → Repository` | Requisito del equipo; coincide con el C4 del TT |
| Persistencia | Spring Data JPA (Hibernate) + **queries nativas** para PostGIS, FIFO y concurrencia | JPA para CRUD; lo espacial y los bloqueos se expresan mejor en SQL nativo |
| Enums | Tipos nativos de PostgreSQL mapeados con `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` | El esquema v6 ya define los tipos; un enum Java por cada tipo SQL |
| Espacial | Columnas `geography` **no** se mapean como atributos JPA; lat/lon viajan en DTOs y las operaciones espaciales son queries nativas (`ST_DWithin`, `ST_Covers`, `ST_MakePoint`) | Evita acoplar Hibernate-Spatial/JTS; menos dependencias que explicar en el TT. Si estorba, se reevalúa en F3 (§10) |
| Autenticación | **Token opaco en BD** (columna `usuarios.token_sesion`) + filtro de Spring Security. BCrypt para contraseñas | El TT exige sesión única por dispositivo y revocable (RN-018, RNF-006, CU-015 invalida token). Un JWT sin estado no se puede revocar sin lista negra; el esquema v6 ya modela el token en BD |
| Autorización | Roles `CLIENTE`, `REPARTIDOR`, `ADMIN` desde `usuarios.rol` + verificación de propiedad en servicios (dueño, RN-021) | RNF-008 |
| Documentación API | springdoc-openapi (Swagger UI en `/swagger-ui.html`) | Evidencia directa para el documento del TT |
| SMS / Push | Interfaces `SmsService` y `PushService` con implementación `dev` (log) y proveedor real intercambiable por perfil | El TT no fija proveedor; no bloquear el desarrollo por credenciales |
| Pruebas | JUnit 5 + Mockito (servicios), MockMvc (controllers), Testcontainers con imagen `postgis/postgis` (repos y flujos) | RNF-007 exige confianza en transacciones reales; H2 no tiene PostGIS ni los enums |

**Dependencias del `pom.xml` (F0):** `spring-boot-starter-web`, `spring-boot-starter-data-jpa`,
`spring-boot-starter-validation`, `spring-boot-starter-security`, `spring-boot-starter-websocket`,
`org.postgresql:postgresql`, `lombok`, `springdoc-openapi-starter-webmvc-ui`,
`spring-boot-starter-test`, `spring-security-test`, `org.testcontainers:postgresql` (test).
Plugin: `spring-boot-maven-plugin` (mismo patrón que el pom del módulo de ruteo).

---

## 2. Estructura del proyecto

Ubicación dentro del repositorio existente (el prototipo de ruteo no se mueve hasta F9):

```
H2ToGo/                                  ← repositorio actual (rama main)
├── h2togo-routing/                      ← prototipo A* FUNCIONAL; no tocar hasta F9
├── H2ToGo_v6_postgresql.sql             ← esquema v6 (ya está en la raíz)
├── pruebas_v6.sql
├── PLAN_BACKEND.md                      ← este documento
└── h2togo-backend/                      ← NUEVO: el backend de este plan
```

```
h2togo-backend/
├── pom.xml
├── docker-compose.yml                  ← PostgreSQL+PostGIS local (postgis/postgis:16)
└── src/
    ├── main/java/com/h2togo/backend/
    │   ├── H2ToGoBackendApplication.java
    │   ├── config/          SecurityConfig, WebSocketConfig, CorsConfig, OpenApiConfig
    │   ├── security/        TokenAuthFilter, UsuarioPrincipal, SecurityUtils
    │   ├── common/          ApiError, GlobalExceptionHandler, excepciones de negocio,
    │   │                    PagedResponse, GeoPunto (lat/lon)
    │   ├── auth/            AuthController, AuthService, dto/
    │   ├── usuarios/        entidades Usuario/Cliente/Repartidor/Administrador,
    │   │                    repositorios, UsuariosController (perfil propio)
    │   ├── direcciones/     DireccionesController, DireccionService, dto/
    │   ├── negocios/        Negocio, HorarioNegocio, NegociosPublicController,
    │   │                    NegocioDuenoController, servicios
    │   ├── vehiculos/       VehiculoNegocio, repositorio (las altas/bajas van por solicitudes)
    │   ├── catalogo/        Marca, ProductoNegocio, servicios de precios
    │   ├── inventario/      LoteInventario, InventarioVehiculo, MovimientoInventario,
    │   │                    InventarioService (FIFO), controllers
    │   ├── repartidores/    JornadaController (CU-008), UbicacionService
    │   ├── pedidos/         Pedido, DetallePedido, ApartadoPedido, HistorialEstadoPedido,
    │   │                    PedidosClienteController, PedidosRepartidorController,
    │   │                    PedidoService, AsignacionService, EntregaService, ApartadoService
    │   ├── solicitudes/     SolicitudCambioPerfil, controllers dueño/admin, servicio
    │   ├── admin/           AdminUsuariosController, AdminPedidosController
    │   ├── tracking/        WS: TrackingController (STOMP), ProximidadService
    │   ├── notificaciones/  SmsService, PushService (+ impl dev y real)
    │   ├── scheduling/      PedidosProgramadosJob, SesionesExpiradasJob, LotesPorCaducarJob
    │   └── routing/         módulo A* integrado (F9): domain, service, controller
    └── main/resources/
        ├── application.yml  (+ application-dev.yml, application-test.yml)
        └── osm_data/        JSON OSM de la Alcaldía Benito Juárez (F9)
```

Convención de rutas: **todo bajo `/api/v1`**. El actor autenticado se resuelve del token
(`/…/me/…`); nunca se recibe el id del propio usuario en el body (RNF-008).

---

## 3. Convenciones transversales (definir en F0, aplicar siempre)

1. **DTOs**: records de Java 21. Sufijos `Request` / `Response`. Nunca se expone una entidad
   JPA en un controller. Validación con `jakarta.validation` en el DTO (`@NotNull`,
   `@Positive`, `@Email`…); las reglas de negocio (RN) van en el servicio, no en el DTO.
2. **Mappers**: clase estática por módulo (`PedidoMapper.toResponse(...)`). Sin MapStruct
   (una dependencia menos que justificar; los DTOs son pequeños).
3. **Errores**: formato único `ApiError { timestamp, status, codigo, mensaje, detalles[] }`.
   `codigo` es legible por la app móvil (ej. `RN-030_CADUCIDAD_INVALIDA`,
   `PEDIDO_YA_ASIGNADO`). `GlobalExceptionHandler` traduce: `NotFoundException→404`,
   `BusinessRuleException→422`, `ConflictException→409`, `AccessDeniedException→403`,
   validación→400. Mensajes de login genéricos (CU-002 E1: nunca revelar si el correo existe).
4. **Transacciones**: `@Transactional` en métodos de servicio que tocan más de una tabla.
   Los flujos de inventario y asignación son transaccionales SIEMPRE (RF-022, RF-019).
5. **Estados de pedido**: cada cambio de `pedidos.estado_actual` inserta también en
   `historial_estados_pedido` (RN-013) dentro de la misma transacción, con la ubicación del
   repartidor si aplica. Transiciones válidas centralizadas en `PedidoService`
   (`pendiente|pendiente_programado → asignado → en_camino → entregado|no_entregado`;
   `pendiente|asignado → cancelado`, RF-013). Cualquier otra → `BusinessRuleException`.
6. **Paginación**: `Pageable` de Spring en listados (`?page=&size=&sort=`), respuesta
   `PagedResponse<T>`; tamaño máximo 100.
7. **Idioma**: dominio en español (como el esquema y el módulo de ruteo): `Pedido`,
   `LoteInventario`, `crearPedido(...)`.
8. **Fechas**: `Instant`/`OffsetDateTime` en UTC; la app convierte a hora local.
9. **Git**: una rama por fase (`fase/F5-inventario`), PR con descripción de CU cubiertos,
   merge solo con `mvn verify` en verde.

---

## 4. Fases

### F0 — Andamiaje del proyecto (S)

**Objetivo:** proyecto que compila, levanta contra la BD v6 y responde un healthcheck.

- [x] `pom.xml` con parent `spring-boot-starter-parent` 4.1.x (fijado **4.1.0**, release
      vigente), Java 21 y las dependencias de §1.
- [x] `docker-compose.yml` con `postgis/postgis:16` + volumen que ejecuta
      `../H2ToGo_v6_postgresql.sql` (raíz del repo) al inicializar
      (carpeta `docker-entrypoint-initdb.d`).
- [x] `.gitignore` del repo: `target/`, `hs_err_pid*.log`, credenciales locales.
      (Hoy `h2togo-routing/` tiene `target/` compilado y dos `hs_err_pid*.log`
      susceptibles de versionarse por accidente.)
- [x] `application.yml` (datasource, JPA `ddl-auto: validate` — el esquema manda, Hibernate solo valida), perfiles `dev`/`test`.
- [x] `common/`: `ApiError`, `GlobalExceptionHandler`, jerarquía de excepciones
      (`NotFoundException`, `ConflictException`, `BusinessRuleException(codigo)`), `GeoPunto` (+ `PagedResponse`).
- [x] `GET /api/v1/health` (público) → `{ status, db: ok }`.
- [x] `SecurityConfig` provisional: todo público excepto lo que se vaya asegurando; CORS para el origen del panel web.
- [x] springdoc operativo (`/swagger-ui.html`).

**Aceptación:** `docker compose up` + `mvn spring-boot:run` levanta el Tomcat embebido en `:8080` sin errores con
`ddl-auto: validate` contra la BD v6 (esto ya valida que las 19 tablas y enums cuadran);
el healthcheck responde 200.

> **Estado de la aceptación (2026-08-09):** `mvn verify` **en verde** sin flags manuales
> (2 tests, `BUILD SUCCESS`; límites de memoria horneados en `.mvn/jvm.config` y en el
> plugin surefire para no tronar la JVM en equipos con poco archivo de paginación —
> mismo problema `hs_err_pid` que el módulo de ruteo). El **smoke test de runtime**
> (`docker compose up` + `mvn spring-boot:run` + `GET /health` → 200) queda **pendiente
> de ejecutar en una máquina con más RAM**; el equipo actual no lo soporta con holgura.
> Ver desviaciones menores en §10 (#9, #10).

### F1 — Modelo de datos (M)

**Objetivo:** las 19 tablas de v6 como entidades JPA + repositorios, verificadas contra la BD real.

- [x] Enums Java: `RolUsuario`, `TipoVehiculo`, `CodigoCambioPerfil`, `EstadoSolicitud`,
      `TipoSolicitudPedido`, `EstadoPedido`, `TipoMovimiento` — con `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`.
      (Constantes en minúsculas donde el esquema define etiquetas en minúsculas; ver §10 #12.)
- [x] Entidades del subtipo (`Cliente`, `Repartidor`, `Administrador`): `@Id` = `id_usuario`,
      relación `@OneToOne @MapsId` hacia `Usuario`. La columna `rol` del subtipo NO se mapea
      como atributo editable (la maneja la BD/insert del servicio).
- [x] Resto de entidades (19 en total). Columnas `geography` NO mapeadas (ver §1); columnas derivadas
      (`en_zona_cobertura`, `fecha_actualizacion`, y timestamps `DEFAULT now()`) mapeadas como `insertable=false, updatable=false`.
      FKs entre agregados como escalares `Integer`; el agregado `Pedido` usa `@OneToMany` (ver §10 #13).
- [x] Repositorios Spring Data por agregado (16, uno por tabla raíz; `DetallePedido`, `ApartadoPedido`
      e `HistorialEstadoPedido` se manejan a través de `Pedido`).
- [x] Test de integración (Testcontainers): arranca el contexto, guarda y lee un grafo mínimo
      de objetos (usuario+cliente, usuario+repartidor+negocio en una transacción — FKs diferidas).
      Implementado en `ModeloDatosIT`; se salta sin Docker y corre bajo failsafe en `verify`.

**Aceptación:** `mvn verify` verde con Testcontainers; `ddl-auto: validate` no reporta
diferencias; el alta dueño+negocio funciona en una sola transacción.

> **Estado de la aceptación (2026-08-22):** código completo (7 enums + 19 entidades + 16
> repositorios + `ModeloDatosIT`). Corrido en máquina con Docker: **`ddl-auto: validate` PASÓ
> contra la BD v6 real (PostgreSQL 16.4 + PostGIS)** — el `EntityManagerFactory` inicializó sin
> reportar diferencias, confirmando que el mapeo de las 19 entidades y los enums cuadra con el
> esquema (NAMED_ENUM, `precision/scale`, booleanos, columnas `insertable=false`). El primer
> intento del IT falló por un detalle del TEST (usaba `webEnvironment=NONE`, sin el bean
> `HttpSecurity` que necesita `SecurityConfig`); corregido a `webEnvironment=MOCK`. El segundo
> intento destapó un **bug de producción en `SecurityConfig` (F0)**: en contexto web hay dos
> beans `CorsConfigurationSource` (el nuestro y `mvcHandlerMappingIntrospector` de WebMvc), así
> que la inyección era ambigua — habría roto también `spring-boot:run`. Corregido con
> `@Qualifier("corsConfigurationSource")`.
> **Confirmado (2026-08-22):** `mvn verify` en verde con Docker — `ModeloDatosIT` 2/2, BUILD SUCCESS.
> Los tres criterios de aceptación de F1 quedan cumplidos. **F1 = Hecha.**

**Nota para el agente:** el ciclo `negocios.id_dueno ↔ repartidores.id_negocio` usa FKs
`DEFERRABLE INITIALLY DEFERRED`: el alta de repartidor-dueño y su negocio debe ocurrir en
UNA transacción (ver `pruebas_v6.sql` §1 como referencia de orden de inserción).

### F2 — Seguridad y autenticación (M) — CU-001, CU-002, CU-003

**Objetivo:** registro, verificación por OTP, login con sesión única, logout, y el filtro
de seguridad que protege todo lo demás.

- [x] `AuthService.registrar(RegistroRequest)`: valida duplicados correo/teléfono (RN-015,
      responde 409 con mensaje claro), BCrypt (RN-017), crea `usuarios` + fila de subtipo en
      una transacción. Si `rol=repartidor`: crea negocio nuevo (queda como dueño, RN-024) o
      lo asocia a negocio existente. Genera OTP (RN-002) y lo envía por `SmsService`.
- [x] `POST /auth/verificacion-telefono` (valida OTP y expiración → `telefono_verificado=true`)
      y `/reenviar`.
- [x] `AuthService.login(LoginRequest)`: BCrypt, exige `telefono_verificado` y `cuenta_activa`,
      informa `clientes.suspendido_hasta` sin bloquear (RN-006, §10 #16), sesión única (RN-018):
      si hay token vigente responde `409 SESION_ACTIVA_EN_OTRO_DISPOSITIVO`; con
      `forzar=true` invalida la anterior (CU-002 S1). Genera token opaco (SecureRandom,
      base64url), expiración deslizante por inactividad (RNF-004, §10 #15), guarda `token_fcm`.
- [x] `TokenAuthFilter`: `Authorization: Bearer <token>` → busca usuario por token, valida
      expiración y `cuenta_activa`, monta `UsuarioPrincipal {id, rol}`; refresco deslizante <7 d (§10 #15).
- [x] `POST /auth/logout` (RN-018: anula `token_sesion` y campos de sesión).
- [x] `SecurityConfig` final: público solo `/auth/**`, `/health`, swagger y WS handshake;
      el resto por rol (`/admin/** → ADMIN`, etc.). BCrypt como `PasswordEncoder`; 401/403 en JSON.
- [x] Tests: registro duplicado (409), login sin verificar (422), sesión única (409/forzar),
      acceso sin token (401) y por rol incorrecto (403). Unitarios (Mockito) + `AuthFlowIT` (Testcontainers).

**Aceptación:** flujo completo registro→OTP→login→endpoint protegido→logout demostrable
con la colección `.http`; tests verdes.

> **Confirmado (2026-08-22):** `mvn verify` **BUILD SUCCESS** con Docker — unitarios 10/10 y
> de integración 7/7 (`AuthFlowIT` 5/5 el flujo completo + 401/403 + sesión única; `ModeloDatosIT` 2/2).
> Colección de evidencia en `h2togo-backend/http/auth.http`. Decisiones del TT en §10 #15–#17. **F2 = Hecha.**

### F3 — Perfil y direcciones del cliente (S) — soporte de CU-004/006

- [x] `GET /usuarios/me` (datos comunes + los del subtipo según rol).
- [x] `PATCH /usuarios/me` (nombre, foto; el cliente edita directo — los cambios del negocio
      del dueño van por solicitudes, F10).
- [x] CRUD de direcciones del cliente: `GET/POST/PUT/DELETE /clientes/me/direcciones[/{id}]`.
      El INSERT/UPDATE usa query nativa con `ST_SetSRID(ST_MakePoint(:lon,:lat),4326)::geography`;
      el trigger de BD calcula `en_zona_cobertura`; la respuesta lo informa y si es `false`
      la app avisa que está fuera de cobertura. DELETE = `activo=false` si la dirección ya
      fue usada en pedidos (conserva histórico, RN-013).
- [x] Cargar el polígono **real** de la Alcaldía Benito Juárez en `zonas_cobertura`
      (`sql/zona_benito_juarez.sql`): contorno de OSM (relación R5605589, boundary=administrative
      admin_level 8), traído por Nominatim y cargado con `ST_GeomFromGeoJSON`+`ST_Multi`. §10 #20.

**Aceptación:** dirección dentro/fuera de la alcaldía se clasifica correctamente; tests de
ownership (un cliente no ve/edita direcciones de otro → 403/404).

> **Confirmado (2026-09-01):** `mvn verify` **BUILD SUCCESS** con Docker — unit 10/10 e IT 13/13
> (`DireccionesFlowIT` 6/6: clasificación de cobertura, ownership 404, CRUD, perfil, repartidor→403).
> Polígono **real de OSM** cargado y validado (§10 #20 cerrado). Bug real corregido en §10 #21. **F3 = Hecha.**

### F4 — Negocios, catálogo y horarios (M) — CU-022, CU-023 (parte inmediata)

- [x] `GET /negocios/{id}/perfil` (público autenticado, CU-022): datos del negocio, horario
      semanal, productos activos con precio, precio de envase y stock disponible agregado
      (suma de lotes activos de la base), ubicación de la base para el mapa. Indicador
      abierto/cerrado calculado en hora local (RN-004, §10 #22).
- [x] `GET /negocios?cerca=lat,lon&limite=n`: negocios activos EN COBERTURA ordenados por
      distancia (KNN `<->`, query nativa) — apoya la elección de purificadora del cliente.
- [x] Dueño (RN-021): `GET /negocios/me` (cualquier repartidor del negocio lo consulta),
      `PUT /negocios/me/horarios` (7 filas, valida consistencia → 422), `PUT /negocios/me/productos/{id}/precio`
      (cambio inmediato, RF-028). Ediciones solo del DUEÑO (403 si no). Alta de producto nuevo va por solicitudes (F10).
- [x] `GET /marcas` (catálogo global).
- [x] Tests: perfil público con stock; horario inconsistente → 422; no-dueño intenta editar → 403;
      cercanía KNN; precio inmediato; cliente no accede a `/negocios/me` (403). `NegociosFlowIT` (7).

> **Confirmado (2026-09-01):** `mvn verify` **BUILD SUCCESS** con Docker — unit 10/10 e IT 20/20
> (`NegociosFlowIT` 7/7). Colección `http/negocios.http`. **F4 = Hecha.** (§10 #23: gotcha JPA+nativa.)

### F5 — Inventario y jornada (M) — CU-018, CU-019, CU-008

- [x] `POST /inventario/lotes` (CU-018, solo dueño RN-021): valida caducidad > hoy+7 días
      (RN-030), capacidad máxima del producto en base, crea lote + movimiento
      `entrada_proveedor` (RN-013) en una transacción.
- [x] `GET /inventario/base` y `GET /inventario/vehiculo` (por negocio del repartidor):
      existencias por marca y lote, próximos a caducar primero.
- [x] `POST /repartidores/me/jornada` (CU-008): selecciona vehículo del negocio propio
      (RN-014; el servicio valida negocio+activo), carga inicial → delega en carga FIFO;
      al confirmar, `estado_operativo=true`.
- [x] `POST /inventario/carga-vehiculo` (CU-019): FIFO por `fecha_caducidad` (lotes vigentes)
      con `SELECT … FOR UPDATE` sobre lotes, valida capacidad del vehículo
      (`capacidad_garrafones`, RN-008), descuenta base / incrementa vehículo, movimiento
      `traspaso_a_vehiculo` (una fila, ambas ubicaciones). Todo transaccional (RF-022/RNF-014).
- [x] `POST /inventario/devolucion` (fin de jornada): vehículo→base, movimiento
      `devolucion_a_base`; `estado_operativo=false`, `id_vehiculo_actual=null`.
- [x] Tests: FIFO respeta caducidades; caducidad a 5 días → 422 `RN-030…`; carga que excede
      capacidad → 422; concurrencia de dos cargas sobre el mismo lote (Testcontainers).

> **Confirmado (2026-09-01):** `mvn verify` **BUILD SUCCESS** con Docker — unit 10/10 e IT 25/25
> (`InventarioFlowIT` 5/5, incluida la prueba real de concurrencia: dos cargas simultáneas sobre
> el mismo lote → exactamente una gana, lote 10→4). Colección `http/inventario.http`. **F5 = Hecha.** (§10 #24.)

### F6 — Pedidos: creación, cancelación e historiales (L) — CU-004, CU-005, CU-007, CU-013

- [x] `POST /pedidos` (CU-004): modalidad `directa` (valida negocio activo + horario RF-011;
      si está cerrado responde 422 `NEGOCIO_CERRADO`) o `abierta` (requiere `precio_maximo_garrafon`;
      valida que exista purificadora que cumpla, RN-028). Valida dirección propia y en zona (RN-001),
      y suspensión (RN-006). Calcula `total_pagar` con snapshot de precios (RN-025/031) en directa;
      estimado con `precio_maximo` en abierta (§10 #27). Programado → estado `pendiente_programado` (activa F11).
- [ ] Apartado de inventario: **movido a F7** — RN-022 aparta al ACEPTAR, no al crear (§10 #25).
      La creación en F6 no aparta ni valida stock. La liberación (RN-023) queda cableada en la cancelación
      (no-op mientras no haya apartados).
- [x] `POST /pedidos/{id}/cancelacion` (CU-005): solo dueño del pedido y estado
      `pendiente|pendiente_programado` (ficha CU-005, §10 #26; `asignado`/`en_camino` → 422); historial.
- [x] `GET /clientes/me/pedidos` (CU-007) y `GET /repartidores/me/entregas` (CU-013):
      paginados, más reciente primero, con detalle al entrar (`GET /pedidos/{id}` con
      ownership: cliente dueño, repartidor asignado o admin, RN-016).
- [x] Tests: horario cerrado (422); abierta sin precio máximo (422) y sin purificadora (422);
      snapshot de precios no cambia si el dueño cambia el precio después; cancelar ya cancelado → 422;
      fuera de cobertura (422); ownership (otro cliente → 403). `PedidosFlowIT` (8).

> **Confirmado (2026-09-05):** `mvn verify` **BUILD SUCCESS** con Docker — unit 10/10 e IT 33/33
> (`PedidosFlowIT` 8/8). Colección `http/pedidos.http`. **F6 = Hecha.** (§10 #25–#27.)

### F7 — Asignación y entrega (L) — CU-010, CU-012

- [x] `GET /repartidores/me/pedidos-disponibles` (CU-010/RF-008): pedidos `pendiente`
      compatibles → modalidad directa de su negocio, o abierta cuyo catálogo cumpla el
      `precio_maximo_garrafon` en TODAS las marcas; con dirección en zona;
      ordenados por distancia a la ubicación actual (§7-Q3, NULLS LAST); incluye distancia.
- [x] `POST /pedidos/{id}/aceptacion`: **concurrencia RF-019** con UPDATE condicional
      (§7-Q4): si `filas==0` → `409 PEDIDO_YA_ASIGNADO`. Valida carga del vehículo (RN-008,
      §10 #28) antes del UPDATE; aparta desde el vehículo en la misma transacción. Estado →
      `asignado`, snapshot `id_vehiculo_utilizado`; en abierta fija precios reales del negocio.
- [x] `POST /pedidos/{id}/en-camino`: estado + historial con ubicación.
- [x] `POST /pedidos/{id}/resultado` (CU-012): `entregado` exige ubicación reportada a
      ≤ 50 m de la dirección (RF-014, `ST_DWithin`); registra `cantidad_entregada` por
      línea (parciales RN-032), líneas `agregada_en_sitio` con snapshot de precio vigente,
      recalcula `total_pagar` definitivo (RN-033), descuenta inventario del vehículo con
      movimiento `salida_pedido`, libera apartados (§10 #29). `no_entregado` exige motivo y
      dispara RN-006: `ausencias_consecutivas+1` y, al llegar al **umbral 2**, fija
      `suspendido_hasta = now()+3 días` (§10 #1). Entrega exitosa reinicia el contador.
- [x] Tests: dos repartidores aceptan a la vez → uno gana (concurrencia real);
      entrega fuera de rango → 422; parcial recalcula total; carga insuficiente → 422;
      no_entregado ×2 suspende y bloquea nuevo pedido. `AsignacionEntregaFlowIT` (6).

> **Confirmado (2026-09-05):** `mvn verify` **BUILD SUCCESS** con Docker — unit 10/10 e IT 39/39
> (`AsignacionEntregaFlowIT` 6/6). Colección `http/asignacion-entrega.http`. **F7 = Hecha.** (§10 #1,#28,#29.)

### F8 — Rastreo y notificaciones (M) — CU-006, RF-005, RF-006

- [x] WebSocket STOMP: endpoint `/ws` (token en el CONNECT, `StompAuthChannelInterceptor`);
      el repartidor publica en `/app/pedidos/{id}/ubicacion`; el backend rebota a
      `/topic/pedidos/{id}/ubicacion` para el cliente suscrito y persiste
      `repartidores.ubicacion_actual` (query nativa §7-Q5) con *throttle* (máx. 1 escritura / 5 s).
- [x] Fallback REST `PUT /repartidores/me/ubicacion` (misma lógica) para redes que bloquean WS.
- [x] `ProximidadService` (RF-005): con cada ubicación de un pedido `en_camino`, si
      `ST_DWithin(direccion, ubicacion, 500)` y aún no se notificó → push al cliente.
      Flag anti-duplicado en memoria (`ConcurrentHashMap<pedidoId>`, §10 #2).
- [x] `PushService` interfaz + `DevPushService` (log); eventos cableados: pedido asignado,
      en camino, proximidad, entregado/no entregado. FCM real pendiente de credenciales (§10 #4);
      la solicitud resuelta se cablea en F10.
- [x] Tests: unitarios de proximidad (dentro/fuera/duplicado); integración WS con cliente STOMP real.

> **Confirmado (2026-09-05):** `mvn verify` **BUILD SUCCESS** con Docker — unit 12/12 e IT 41/41
> (`TrackingFlowIT` 2/2: proximidad por REST + relay WebSocket STOMP real). **F8 = Hecha.**
> Pendiente solo la impl FCM real (§10 #4) — la interfaz y el evento ya están.

### F9 — Integración del motor de ruteo A* (S) — CU-011

- [x] Traer el código del prototipo `../h2togo-routing` al paquete `routing/` del backend
      (mismos `Grafo`, `Nodo`, `Arista`, `AEstrella`, `OsmGraphLoader`, patrón Strategy
      intacto), renombrando paquete a `com.h2togo.backend.routing`. Sin cambios de algoritmo.
- [x] Copiar `osm_data/sample_map.json` (5.4 MB, export real de BJ) a los resources del backend.
      El cliente Leaflet de pruebas NO se trae (decisión: no aporta al backend).
- [x] Memoria: `OsmGraphLoader` carga **lazy** (primer `getGrafo()`) + warm-up al arranque
      (`RoutingWarmup`, desactivable con `h2togo.routing.precargar=false` en pruebas).
      Grafo real: **27278 nodos, 42239 aristas, ~340 ms de carga**. Los tests usan failsafe
      `-Xmx1024m`; la app en dev corre holgada con `-Xmx512m` (el grafo ocupa ~50 MB).
- [x] `GET /pedidos/{id}/ruta` (CU-011, solo el repartidor asignado): origen = ubicación
      actual del repartidor, destino = dirección de entrega; responde la polilínea de
      coordenadas y distancia total (mismo `RouteResponse` del módulo). 422 si no hay ruta o sin GPS.
- [x] `POST /api/v1/admin/routing/route` (endpoint de pruebas del motor, solo ADMIN).
- [x] Tests: `AEstrellaTest` (algoritmo portado, grafo mínimo) + `RutaFlowIT` (endpoint con
      ownership y ruta real sobre el grafo OSM).

> **Confirmado (2026-09-05):** `mvn verify` **BUILD SUCCESS** con Docker — unit 14/14 e IT 42/42
> (`AEstrellaTest` 2/2, `RutaFlowIT` 1/1: A* sobre el grafo real de BJ). Colección `http/ruta.http`. **F9 = Hecha.**

### F10 — Administración (M) — CU-014, CU-015, CU-016, CU-020, CU-021

- [x] `POST /admin/usuarios` (CU-014): alta manual con rol y, si es repartidor, negocio;
      queda `telefono_verificado=true` (pre-verificado, §10 #30) → login inmediato.
- [x] `POST /admin/usuarios/{id}/baja` (CU-015): baja lógica (`cuenta_activa=false`,
      motivo, fecha), invalida token, bloquea si tiene pedidos `en_camino` (422), y si es
      dueño de un negocio: con `idNuevoDueno` transfiere, sin él lo desactiva (RN-024, §10 #30).
      Reactivación: `POST /admin/usuarios/{id}/reactivacion`.
- [x] `GET /admin/pedidos` (CU-016): filtros combinables fecha/estado/cliente/repartidor/
      negocio (query nativa dinámica), paginado (RN-016: solo ADMIN).
- [x] Solicitudes (RN-019, RN-021): dueño crea `POST /solicitudes` (los 7 tipos del enum) y lista
      las suyas; admin `GET /admin/solicitudes?estado=pendiente` y
      `POST /admin/solicitudes/{id}/resolucion` (CU-021): aprobar **aplica el cambio** al
      negocio/vehículo/producto en la misma transacción; rechazar exige comentario. **Push al dueño**
      (cierra el pendiente de F8).
- [x] Tests: baja con/sin transferencia; alta+login; historial con filtro; aprobación aplica el
      cambio real (nombre negocio); rechazo exige comentario; no-dueño → 403. `AdministracionFlowIT` (7).

> **Confirmado (2026-09-05):** `mvn verify` **BUILD SUCCESS** con Docker — unit 14/14 e IT 49/49
> (`AdministracionFlowIT` 7/7). Colecciones `http/admin.http`, `http/solicitudes.http`. **F10 = Hecha.** (§10 #30.)

### F11 — Tareas programadas (S) — RF-025 y mantenimiento

- [x] `PedidosProgramadosJob` (`@Scheduled` cada minuto): activa `pendiente_programado`
      cuya `fecha_programada` llegó → `pendiente` + `notificado_programado=true` (evita
      doble proceso) con `FOR UPDATE SKIP LOCKED`; notifica a repartidores compatibles (push).
- [x] `SesionesExpiradasJob` (diario): limpia tokens vencidos (RNF-004).
- [x] `LotesPorCaducarJob` (diario): marca lotes vencidos `activo=false` (movimiento
      `salida_manual` por merma) y avisa al dueño de lotes a ≤7 días.
- [x] Tests con reloj inyectable (`Clock`): activación exacta, no doble notificación.

### F12 — Pruebas transversales y endurecimiento (M)

- [x] Colección `.http`/Postman por CU completo (evidencia para el TT) — 10 archivos en `http/`.
- [x] Suite de integración E2E del flujo feliz: registro→login→dirección→pedido→aceptar→
      en camino→ruta→entrega, y del flujo abierto con programado (`e2e/E2EFlujoFelizIT`).
- [x] Datos semilla de demo (`data-demo.sql`) coherentes con `pruebas_v6.sql` (validados en PostGIS).
- [ ] ~~Revisión RNF-007: pool Hikari dimensionado, prueba de 100 transacciones concurrentes~~
      **Diferido (Rubén, 2026-09-05):** la prueba de carga de 100 concurrentes se hará después.
- [x] Revisión de seguridad: ningún endpoint sin rol (401 sin token, 403 rol equivocado),
      ningún id de usuario aceptado del cliente (siempre del token, RN-016), errores sin
      filtración (`e2e/SeguridadIT`). Se endureció `NoResourceFoundException → 404`.
- [x] Swagger revisado y exportado (`docs/openapi.json`, generado por `e2e/OpenApiExportIT`).

### F13 — Consola web de pruebas para el admin (M) — apoyo al TT

Página web estática (servida por el propio backend en `/console/`, sin build aparte) que
consume la API REST + WebSocket para **demostrar y probar el sistema end-to-end** sin la app
Android: login, alta de pedidos (directa/abierta/programada), aceptación/entrega por el
repartidor, seguimiento en vivo y **visualización del motor de ruteo A\* sobre un mapa** (Leaflet).

- [x] `static/console/index.html` + JS vanilla (sin framework): login que guarda el token,
      registro rápido, y paneles por flujo. Servida en `/`, `/console` y `/console/`.
- [x] Panel **Ruteo**: formulario origen/destino (o clic en el mapa) → `POST /admin/routing/route`
      → dibuja la polilínea y muestra la distancia. Mapa Leaflet centrado en Benito Juárez.
      **Validado en vivo** (ruta de 0.653 km, 11 vértices, sobre el grafo real).
- [x] Panel **Cliente**: crear dirección, negocios cercanos, crear pedido (directo/abierto/
      programado), listar, ver detalle+historial, cancelar.
- [x] Panel **Repartidor**: precio, horario, lote, jornada, carga; disponibles, aceptar, en
      camino, enviar ubicación, ver ruta del pedido en el mapa, registrar resultado.
- [x] Panel **Tracking**: STOMP a `/topic/pedidos/{id}/ubicacion` (publicar/suscribir en vivo).
- [x] Whitelist de seguridad para `/`, `/console/**` (la página es pública; las llamadas a
      la API siguen exigiendo token/rol). `WebMvcConfig` resuelve el directorio a `index.html`.
- [x] Documentado en `PRUEBAS.md` (cómo levantar todo en Docker, correr las pruebas y el guion de demo).

---

## 5. Catálogo de endpoints

Notación: 🔓 público (sin token) · C=CLIENTE · R=REPARTIDOR · D=dueño (repartidor con verificación de propiedad) · A=ADMIN.

### Auth (`auth/`)
| Método | Ruta | Rol | CU | Request → Response | Tablas |
|---|---|---|---|---|---|
| POST | `/auth/registro` | 🔓 | CU-001 | `RegistroRequest` → `RegistroResponse` | usuarios, clientes/repartidores, negocios |
| POST | `/auth/verificacion-telefono` | 🔓 | CU-001 | `OtpRequest` → 204 | usuarios |
| POST | `/auth/verificacion-telefono/reenviar` | 🔓 | CU-001 | `{correo}` → 204 | usuarios |
| POST | `/auth/login` | 🔓 | CU-002 | `LoginRequest` → `SesionResponse` | usuarios (+clientes por RN-006) |
| POST | `/auth/logout` | C/R/A | CU-003 | — → 204 | usuarios |

### Perfil y direcciones (`usuarios/`, `direcciones/`)
| Método | Ruta | Rol | CU | Request → Response | Tablas |
|---|---|---|---|---|---|
| GET | `/usuarios/me` | C/R/A | — | — → `PerfilResponse` | usuarios + subtipo |
| PATCH | `/usuarios/me` | C/R/A | — | `PerfilUpdateRequest` → `PerfilResponse` | usuarios |
| GET | `/clientes/me/direcciones` | C | CU-004 | — → `[DireccionResponse]` | direcciones_clientes |
| POST | `/clientes/me/direcciones` | C | CU-004 | `DireccionRequest` → `DireccionResponse` | direcciones_clientes (+zonas via trigger) |
| PUT | `/clientes/me/direcciones/{id}` | C | — | `DireccionRequest` → `DireccionResponse` | direcciones_clientes |
| DELETE | `/clientes/me/direcciones/{id}` | C | — | — → 204 (baja lógica si tiene pedidos) | direcciones_clientes |

### Negocios y catálogo (`negocios/`, `catalogo/`)
| Método | Ruta | Rol | CU | Request → Response | Tablas |
|---|---|---|---|---|---|
| GET | `/negocios/{id}/perfil` | C/R | CU-022 | — → `PerfilNegocioResponse` | negocios, horarios, productos, marcas, lotes |
| GET | `/negocios?cerca=lat,lon` | C | CU-004 | — → `[NegocioCercanoResponse]` | negocios (KNN) |
| GET | `/marcas` | C/R/A | — | — → `[MarcaResponse]` | marcas |
| GET | `/negocios/me` | D | CU-023 | — → `NegocioResponse` | negocios, vehiculos, productos |
| PUT | `/negocios/me/horarios` | D | RF-031 | `[HorarioRequest]×7` → `[HorarioResponse]` | horarios_negocio |
| PUT | `/negocios/me/productos/{id}/precio` | D | CU-023 | `PrecioRequest` → `ProductoResponse` | productos_negocio |

### Inventario y jornada (`inventario/`, `repartidores/`)
| Método | Ruta | Rol | CU | Request → Response | Tablas |
|---|---|---|---|---|---|
| POST | `/inventario/lotes` | D | CU-018 | `LoteEntradaRequest` → `LoteResponse` | lotes_inventario, movimientos |
| GET | `/inventario/base` | R | CU-018/019 | — → `InventarioBaseResponse` | lotes_inventario |
| GET | `/inventario/vehiculo` | R | CU-019 | — → `InventarioVehiculoResponse` | inventario_vehiculo, lotes |
| POST | `/repartidores/me/jornada` | R | CU-008 | `IniciarJornadaRequest` → `JornadaResponse` | repartidores, inventario_*, movimientos |
| POST | `/inventario/carga-vehiculo` | R | CU-019 | `CargaVehiculoRequest` → `InventarioVehiculoResponse` | lotes, inventario_vehiculo, movimientos |
| POST | `/inventario/devolucion` | R | — | `DevolucionRequest` → 204 | inventario_*, movimientos, repartidores |

### Pedidos (`pedidos/`)
| Método | Ruta | Rol | CU | Request → Response | Tablas |
|---|---|---|---|---|---|
| POST | `/pedidos` | C | CU-004 | `PedidoCreateRequest` → `PedidoResponse` | pedidos, detalles, apartados, productos, horarios |
| GET | `/pedidos/{id}` | C/R/A* | CU-006/007 | — → `PedidoDetalleResponse` | pedidos + joins |
| POST | `/pedidos/{id}/cancelacion` | C | CU-005 | `{motivo?}` → `PedidoResponse` | pedidos, apartados, historial |
| GET | `/clientes/me/pedidos` | C | CU-007 | `Pageable` → `PagedResponse<PedidoResumen>` | pedidos |
| GET | `/repartidores/me/pedidos-disponibles` | R | CU-010 | — → `[PedidoDisponibleResponse]` | pedidos, direcciones, productos (espacial) |
| POST | `/pedidos/{id}/aceptacion` | R | CU-010 | — → `PedidoResponse` | pedidos (UPDATE condicional), apartados, historial |
| POST | `/pedidos/{id}/en-camino` | R | CU-012 | `{ubicacion}` → `PedidoResponse` | pedidos, historial |
| POST | `/pedidos/{id}/resultado` | R | CU-012 | `ResultadoEntregaRequest` → `PedidoResponse` | pedidos, detalles, inventario_vehiculo, movimientos, clientes, historial |
| GET | `/repartidores/me/entregas` | R | CU-013 | `Pageable` → `PagedResponse<PedidoResumen>` | pedidos |
| GET | `/pedidos/{id}/ruta` | R | CU-011 | — → `RouteResponse` | pedidos, direcciones, repartidores (+grafo en memoria) |

\* con verificación de propiedad (cliente dueño / repartidor asignado / admin).

### Rastreo (WebSocket + fallback)
| Canal | Dirección | Rol | CU | Payload |
|---|---|---|---|---|
| STOMP `/app/pedidos/{id}/ubicacion` | repartidor → servidor | R | CU-006 | `{lat, lon, timestamp}` |
| STOMP `/topic/pedidos/{id}/ubicacion` | servidor → cliente | C | CU-006 | `{lat, lon, estado, timestamp}` |
| PUT `/repartidores/me/ubicacion` | REST fallback | R | CU-006 | `{lat, lon}` → 204 |

### Solicitudes y administración (`solicitudes/`, `admin/`)
| Método | Ruta | Rol | CU | Request → Response | Tablas |
|---|---|---|---|---|---|
| POST | `/solicitudes` | D | CU-009/020/023 | `SolicitudRequest` → `SolicitudResponse` | solicitudes_cambio_perfil |
| GET | `/solicitudes` | D | CU-020 | `Pageable` → `PagedResponse<SolicitudResponse>` | solicitudes |
| GET | `/admin/solicitudes` | A | CU-021 | filtros → `PagedResponse<SolicitudResponse>` | solicitudes |
| POST | `/admin/solicitudes/{id}/resolucion` | A | CU-021 | `ResolucionRequest` → `SolicitudResponse` | solicitudes + tabla afectada |
| POST | `/admin/usuarios` | A | CU-014 | `AltaUsuarioRequest` → `PerfilResponse` | usuarios + subtipo (+negocios) |
| POST | `/admin/usuarios/{id}/baja` | A | CU-015 | `BajaRequest` → 204 | usuarios, negocios (transferencia) |
| POST | `/admin/usuarios/{id}/reactivacion` | A | CU-015 | — → 204 | usuarios |
| GET | `/admin/pedidos` | A | CU-016 | filtros+`Pageable` → `PagedResponse<PedidoResumen>` | pedidos + joins |

**Cobertura de CU:** CU-001–016 y 018–023 quedan mapeados arriba. CU-017 (Desplegar mapa)
es del cliente móvil con Google Maps; el backend solo provee coordenadas (rastreo, ruta,
perfil de negocio), ya cubiertas.

---

## 6. Reglas de negocio → dónde se aplican

| Regla | Qué dice (resumen) | BD (v6) | Backend |
|---|---|---|---|
| RN-002 | Verificar teléfono por OTP | columnas de verificación | `AuthService` (F2) |
| RN-006 | Ausencias consecutivas → suspensión temporal | `clientes.ausencias_consecutivas / suspendido_hasta` | `EntregaService` acumula/reinicia; login y crear-pedido la revisan (F2/F6/F7). **Umbral y duración: leer RN-006 en el TT** |
| RN-013 | Históricos inmutables | tablas de bitácora solo-INSERT | sin endpoints de edición; historial en cada cambio de estado |
| RN-015 | Sin correo/teléfono duplicado | UNIQUE | `AuthService` → 409 amigable (F2) |
| RN-016 | Visibilidad restringida por actor | — | ownership en servicios + roles (todas las fases) |
| RN-017 | Contraseña cifrada | — | BCrypt (F2) |
| RN-018 | Sesión única, revocable | `token_sesion` UNIQUE | login/logout/filtro (F2) |
| RN-019/021 | Cambios del negocio con aprobación; solo dueño | enum de tipos, FK a administradores | `SolicitudesService` (F10) |
| RN-024 | Negocio con ≥1 dueño activo | `id_dueno NOT NULL` | baja con transferencia (F10) |
| RN-025/031 | Snapshot de precio agua/envase | columnas snapshot en detalles | `PedidoService` al crear (F6) |
| RN-030 | Caducidad > 7 días al registrar lote | — | `InventarioService` (F5) |
| RN-032 | Entregas parciales y líneas en sitio | CHECKs en detalles | `EntregaService` (F7) |
| RN-033 | Total definitivo al cerrar | — | `EntregaService` (F7) |
| RF-011 | Validar horario del negocio | horarios_negocio | `PedidoService` (F6) |
| RF-012 | Stock suficiente y capacidad del vehículo | — | `AsignacionService` (F7) |
| RF-019 | Un solo repartidor gana el pedido | — | UPDATE condicional (F7, §7-Q4) |
| RF-005 | Aviso a 500 m | geografía | `ProximidadService` (F8) |
| RF-014 | Entregar a ≤ 50 m | geografía | `EntregaService` (F7) |
| RF-025 | Activación de programados | `notificado_programado` | job F11 |
| RNF-004 | Token caduca a 30 días | `sesion_fecha_expiracion` | filtro F2 + job F11 |
| RNF-007 | 100 transacciones concurrentes | pool | Hikari + prueba F12 |

> El TT contiene más RN de las listadas. **Antes de implementar cada CU, el agente debe
> releer su ficha completa en `TT1 2026-B176 v2.pdf`** y agregar aquí las RN que falten.

---

## 7. Consultas clave (repositorios / queries nativas)

**Q1 — Validar punto en zona** (referencia; el trigger de BD ya lo hace al guardar direcciones):
```sql
SELECT EXISTS (SELECT 1 FROM zonas_cobertura z
               WHERE z.activo AND ST_Covers(z.geom,
                     ST_SetSRID(ST_MakePoint(:lon,:lat),4326)::geography));
```

**Q2 — FIFO de lotes para carga de vehículo (F5):**
```sql
SELECT * FROM lotes_inventario
WHERE id_negocio = :negocio AND id_marca = :marca AND activo
  AND (cantidad_actual - cantidad_apartada) > 0
ORDER BY fecha_caducidad, id_lote
FOR UPDATE;                -- bloquea los lotes mientras se descuenta
```

**Q3 — Pedidos disponibles para el repartidor (F7):** pendientes, compatibles por modalidad
y precio, con distancia:
```sql
SELECT p.id_pedido, ST_Distance(d.ubicacion, r.ubicacion_actual) AS distancia_m, ...
FROM pedidos p
JOIN direcciones_clientes d ON d.id_direccion = p.id_direccion_entrega
JOIN repartidores r ON r.id_usuario = :idRepartidor
WHERE p.estado_actual = 'pendiente' AND d.en_zona_cobertura
  AND ( p.id_negocio_solicitado = r.id_negocio
     OR (p.tipo_solicitud = 'abierta' AND NOT EXISTS (  -- toda marca pedida cabe en el precio tope
           SELECT 1 FROM detalles_pedido dp
           JOIN productos_negocio pn ON pn.id_negocio = r.id_negocio
                                    AND pn.id_marca = dp.id_marca AND pn.activo
           WHERE dp.id_pedido = p.id_pedido
             AND pn.precio > p.precio_maximo_garrafon)) )
ORDER BY distancia_m;
```

**Q4 — Aceptación con concurrencia RF-019 (F7):**
```sql
UPDATE pedidos
SET id_repartidor = :r, estado_actual = 'asignado', id_vehiculo_utilizado = :vehiculo
WHERE id_pedido = :id AND estado_actual = 'pendiente' AND id_repartidor IS NULL;
-- filasAfectadas == 0  →  409 PEDIDO_YA_ASIGNADO
```

**Q5 — Persistir ubicación del repartidor (F8):**
```sql
UPDATE repartidores
SET ubicacion_actual = ST_SetSRID(ST_MakePoint(:lon,:lat),4326)::geography,
    ubicacion_reportada_en = now()
WHERE id_usuario = :id;
```

**Q6 — Proximidad 500 m (F8) / entrega 50 m (F7):**
```sql
SELECT ST_DWithin(d.ubicacion,
       ST_SetSRID(ST_MakePoint(:lon,:lat),4326)::geography, :metros)
FROM pedidos p JOIN direcciones_clientes d ON d.id_direccion = p.id_direccion_entrega
WHERE p.id_pedido = :id;
```

**Q7 — Activación de programados (F11):**
```sql
SELECT * FROM pedidos
WHERE estado_actual = 'pendiente_programado'
  AND NOT notificado_programado AND fecha_programada <= now()
FOR UPDATE SKIP LOCKED;
```

**Q8 — Historial global con filtros (F10):** `Specification<Pedido>` de Spring Data
(fecha desde/hasta, estado, cliente, repartidor, negocio), `Pageable`.

**Q9 — Stock disponible RF-012 (F7):** suma `(cantidad_actual - cantidad_apartada)` del
vehículo del repartidor + lotes de la base de su negocio, por marca pedida; comparar contra
lo solicitado y contra `capacidad_garrafones` del vehículo.

---

## 8. DTOs principales (los demás siguen el mismo patrón)

```
RegistroRequest        { nombre, apellidos, correo, password, telefono, rol,
                         negocio?: { idExistente | nombreComercial } }   // repartidor
LoginRequest           { correo, password, tokenFcm, forzar=false }
SesionResponse         { token, expiraEn, rol, perfil: PerfilResponse }
DireccionRequest       { alias, calle, numeroExterior, numeroInterior?, colonia,
                         codigoPostal, referencias?, lat, lon }
DireccionResponse      { id, …campos…, lat, lon, enZonaCobertura, activo }
PedidoCreateRequest    { tipoSolicitud, idNegocio?,            // directa
                         precioMaximoGarrafon?,                // abierta
                         idDireccionEntrega, indicaciones?,
                         programado?: { fechaProgramada },
                         detalles: [ { idMarca, cantidad, tieneEnvase } ] }
PedidoResponse         { id, estado, tipoSolicitud, totalPagar, garrafonesTotales,
                         negocio?, repartidor?, direccion, detalles[], fechas… }
PedidoDisponibleResponse { idPedido, distanciaM, garrafonesTotales, colonia,
                         tipoSolicitud, totalEstimado, detalles[] }
ResultadoEntregaRequest{ resultado: ENTREGADO|NO_ENTREGADO, lat, lon,
                         motivoNoEntrega?,                     // exigido si NO_ENTREGADO
                         lineas: [ { idDetalle?, idMarca?, cantidadEntregada,
                                     agregadaEnSitio, tieneEnvase? } ] }
LoteEntradaRequest     { idMarca, cantidad, fechaCaducidad, costoUnitario?, proveedor?, notas? }
CargaVehiculoRequest   { cargas: [ { idMarca, cantidad } ] }   // el servicio elige lotes FIFO
IniciarJornadaRequest  { idVehiculo, cargaInicial: [ { idMarca, cantidad } ] }
SolicitudRequest       { codigoCambio, idVehiculo?, idProductoNegocio?, valorNuevo (JSON) }
ResolucionRequest      { decision: APROBADO|RECHAZADO, comentario? }   // comentario exigido al rechazar
BajaRequest            { motivo, idNuevoDueno? }               // transferencia RN-024
```

---

## 9. Flujo de trabajo para los agentes

1. Tomar la primera fase `Pendiente` cuyas dependencias estén `Hecha`; ponerla `En curso`
   en la tabla de estado con su nombre.
2. Leer las fichas de CU del TT que la fase lista **antes** de escribir código; si la ficha
   contradice este plan, gana la ficha y se anota la corrección en §10.
3. Implementar en rama `fase/Fx-nombre`. Los archivos van donde marca §2; las convenciones
   de §3 no son opcionales.
4. Terminar = checkboxes marcados + criterios de aceptación cumplidos + `mvn verify` verde
   + swagger actualizado + fase `Hecha` en la tabla.
5. Si aparece la necesidad de cambiar el esquema v6, NO se cambia sobre la marcha: se anota
   la propuesta en §10 y se decide con Rubén.

---

## 10. Riesgos y decisiones abiertas

| # | Tema | Estado |
|---|---|---|
| 1 | **Umbral y duración de la suspensión RN-006.** El TT fija el umbral (**2 ausencias consecutivas**, RN-006/CU-012) pero NO la duración. **Decidido (Rubén, 2026-09-05): 3 días naturales** (`suspendido_hasta = now() + 3 días`). Una entrega exitosa reinicia el contador; al suspender, el contador se reinicia a 0. Configurable en `h2togo.pedidos.*` | Decidido |
| 28 | **Apartado desde el vehículo al aceptar (F7, reconciliación RN-022/RN-008).** RN-022 dice "apartar primero del vehículo y si falta, de la base", pero un apartado sobre la base es incompatible con el FIFO de carga (CU-019), que solo mueve stock disponible (no apartado). **Decidido:** al aceptar (CU-010) se aparta SOLO del vehículo; si el vehículo no alcanza, el aceptar se rechaza con `CARGA_INSUFICIENTE` pidiendo cargar más desde la base — exactamente lo que indica RN-008 ("debe cargar más antes de salir"). La entrega consume del vehículo | Decidido |
| 30 | **Decisiones de administración (F10).** (a) **Alta por admin** (CU-014): el usuario queda `telefono_verificado=true` (pre-verificado; la ficha permite login inmediato, sin OTP). (b) **Baja** (CU-015): si el repartidor tiene pedidos `en_camino` → 422 (el "forzar/reasignar" de S1 se difiere; se puede añadir después). Si es dueño de un negocio: con `idNuevoDueno` (repartidor activo del mismo negocio) se transfiere `id_dueno`; sin él, el negocio queda `activo=false` (RN-024). La baja anula el token de sesión. (c) **Solicitudes** (CU-020/021): `valor_nuevo` viaja como JSON; al aprobar se aplica el cambio del tipo correspondiente (los 7 del enum) en la misma transacción (RN-019); rechazar exige comentario. Al resolver se notifica al dueño (push "solicitud resuelta", cierra el pendiente de F8) | Decidido |
| 29 | **Entrega consume del vehículo; libera lo no entregado (F7).** Al `entregado`, por cada línea se consume `cantidad_entregada` del vehículo (movimiento `salida_pedido`) y se liberan los apartados de lo no entregado (RN-023, RN-032 parciales); las líneas agregadas en sitio (RN-032) consumen del vehículo con snapshot del precio de envase vigente. Total definitivo RN-033. Al `no_entregado`: libera todos los apartados (garrafones vuelven al vehículo), total 0, ausencia +1 (suspende al llegar al umbral, RN-006) | Decidido |
| 2 | **Flag de proximidad 500 m**: en memoria (decisión actual, válido con una sola instancia). Si se quisiera sobrevivir reinicios: columna `pedidos.notificado_proximidad` en una v6.1 | Decidido (memoria) |
| 3 | **Proveedor SMS real** para el OTP (RN-002): la interfaz lo aísla; en demo puede mostrarse el código en logs/pantalla del admin | Abierto |
| 4 | **Credenciales FCM** (proyecto Firebase del equipo) necesarias desde F8 | Abierto |
| 5 | **Apartado de inventario en modalidad abierta**: este plan aparta al ACEPTAR (no al crear, porque aún no hay negocio). Confirmar contra la ficha CU-004/CU-010 | Decidido (validar) |
| 6 | **Mapeo espacial**: si las queries nativas resultan incómodas en F3, evaluar `hibernate-spatial` + JTS en lugar de lat/lon en DTOs | Decidido (nativas) |
| 7 | **Módulo de ruteo**: integrado como paquete (un despliegue). Alternativa descartada: microservicio aparte (dos despliegues que justificar en el TT) | Decidido |
| 8 | Baja lógica de dirección usada en pedidos (conserva histórico): confirmar que la app filtra `activo=false` | Decidido (validar en app) |
| 9 | **Módulo Testcontainers (F0).** Boot 4.1 gestiona Testcontainers 2.0, que renombró los módulos con prefijo `testcontainers-`. El plan escribía `org.testcontainers:postgresql` (nombre de la línea 1.x); se usó `org.testcontainers:testcontainers-postgresql`. Cambio forzado por el ecosistema, sin impacto de diseño | Aplicado (informativo) |
| 10 | **Test-slice web (resuelto en F2).** Boot 4 modularizó los slices de test: `@WebMvcTest`/`@AutoConfigureMockMvc` ya no los arrastra `spring-boot-starter-test`. El módulo correcto es **`org.springframework.boot:spring-boot-starter-webmvc-test`** (clases en el paquete `org.springframework.boot.webmvc.test.autoconfigure`). Añadido al `pom.xml` (test scope) en F2; `AuthFlowIT` usa `@AutoConfigureMockMvc` desde ahí | Cerrado |
| 11 | **Memoria de build (F0).** En este equipo la JVM caía con `hs_err_pid` (archivo de paginación pequeño) al pedir heap G1 grande. Se acotó memoria en `.mvn/jvm.config` (`-Xmx512m -XX:+UseSerialGC`), surefire (`-Xmx320m`) y failsafe (`-Xmx768m`). El smoke test de runtime (`spring-boot:run` con la app completa) y el IT de F1 quedan pendientes de una máquina con más RAM/Docker | Abierto (probar en otra PC) |
| 12 | **Enums NAMED_ENUM en minúsculas (F1).** Hibernate `@JdbcTypeCode(NAMED_ENUM)` mapea por `name()` sin conversión de caso, y v6 define casi todas las etiquetas en minúsculas (`rol_usuario`, `estado_pedido`, etc.). Por eso las constantes Java van en minúsculas (excepto `CodigoCambioPerfil`, que v6 define en MAYÚSCULAS). Un `AttributeConverter` a MAYÚSCULAS no sirve: PostgreSQL no castea implícitamente `varchar`→`enum`. No se cambió el esquema | Aplicado (informativo) |
| 13 | **Mapeo de FKs (F1).** FKs entre agregados → columnas escalares `Integer` (evita el trap del ciclo diferido `negocios.id_dueno ↔ repartidores.id_negocio` con IDENTITY, y encaja con el uso de queries nativas). Dentro del agregado `Pedido` → `@OneToMany` con cascade (Detalle/Apartado/Historial "se manejan a través de Pedido", como pide F1). Las columnas `geography` no se mapean; las direcciones/negocios con ubicación se dan de alta por query nativa (F3/F4) | Decidido (escalares) |
| 14 | **F1 sin verificar contra BD real (F1).** `ddl-auto: validate` y el test del ciclo diferido solo se ejecutan con Docker (Testcontainers). En este equipo `ModeloDatosIT` se salta. El gate real de F1 es correr `mvn verify` en la máquina con Docker; ahí pueden aparecer ajustes finos de mapeo (`columnDefinition`, `precision/scale`, boolean). Hasta entonces F1 NO se marca "Hecha" | Cerrado (F1 verde 2026-08-22) |
| 15 | **Expiración del token: por INACTIVIDAD, deslizante (F2, RNF-004).** RNF-004 textual: *"Si la aplicación… permanece… sin abrirse por más de 30 días, el token caducará…"* → deslizante. **Decidido (Rubén, 2026-08-22):** el `TokenAuthFilter`, si al validar una request quedan **<7 días** de vigencia, extiende `sesion_fecha_expiracion` a now()+30d (una sola escritura). Un usuario activo al menos cada ~23 días nunca caduca; inactivo >30 días, sí. Minimiza escrituras a BD | Decidido |
| 16 | **Login SÍ revisa suspensión (F2).** El plan lo pedía; el TT acota RN-006 a "nuevos pedidos" y no lo cita en CU-002. **Decidido (Rubén, 2026-08-22): revisar también en login.** El login de un cliente suspendido **NO se bloquea**; se informa la fecha (`suspendidoHasta` en `SesionResponse`), coherente con "informa fecha" del plan. El bloqueo efectivo de pedidos sigue en F6 | Decidido |
| 17 | **OTP 6 dígitos / 10 min (F2).** El TT no fija longitud ni caducidad (solo "SMS en <2 min"). **Decidido (Rubén, 2026-08-22):** OTP numérico de 6 dígitos, caducidad 10 min, reenvío permitido (cabe en `codigo_verificacion` VARCHAR(10)). Password mínimo 8 caracteres SÍ lo fija el TT (CU-001) → se valida | Decidido |
| 18 | **Cobertura: clasificar al guardar, bloquear al pedir (F3, RN-001).** RN-001 restringe direcciones/entregas a la Alcaldía Benito Juárez. Interpretación (coherente con la ficha CU-004 S1 y con la propia aceptación de F3 "clasifica correctamente"): al guardar una dirección NO se rechaza; el trigger `trg_direcciones_zona` calcula `en_zona_cobertura` y la respuesta lo informa. El bloqueo efectivo ocurre al crear el pedido (F6, CU-004 S1). El esquema v6 no cambia | Decidido (clasificar) |
| 19 | **`referencias` obligatorio (F3, RN-003).** RN-003 (y CU-004: "coordenadas y referencias") exige que la dirección tenga descripción para facilitar la entrega. El plan (§8) lo tenía opcional; **gana la ficha:** `referencias` se valida como requerido en `DireccionRequest`. La columna sigue siendo `TEXT` nullable en v6 (no se toca el esquema; la obligatoriedad la impone el backend) | Decidido (requerido) |
| 20 | **Polígono de Benito Juárez (F3, resuelto).** Se cargó el contorno **real** de OSM: relación R5605589 (boundary=administrative, admin_level 8), obtenida por Nominatim `/lookup?polygon_geojson=1` (© OpenStreetMap, ODbL). `sql/zona_benito_juarez.sql` lo inserta con `ST_Multi(ST_SetSRID(ST_GeomFromGeoJSON(...),4326))::geography` (idempotente por `ON CONFLICT`). Validado por `DireccionesFlowIT`. Para actualizarlo: repetir la consulta a Nominatim y regenerar el archivo | Cerrado |
| 27 | **Precio estimado en modalidad abierta (F6).** Al crear un pedido abierto no se conoce el negocio, así que no hay precios reales que capturar. **Decidido:** `detalles.precio_unitario = precio_maximo_garrafon` (estimado), `precio_envase_unitario = 0` como placeholder cuando la línea no trae envase (el CHECK de v6 exige NOT NULL), y `total_pagar` estimado = garrafones × precio_max. El precio/total DEFINITIVO se fija al aceptar/cerrar (F7, RN-025/RN-033). En modalidad directa sí se capturan los precios reales del negocio al crear (RN-025) | Decidido |
| 33 | **Consola web de pruebas servida por el backend (F13).** Página estática (`static/console/index.html`, JS vanilla + Leaflet + STOMP por CDN) que consume la API para demostrar el sistema end-to-end sin la app Android: login, alta de pedidos, atención del repartidor, tracking en vivo y **visualización del motor de ruteo A\* en un mapa**. Se sirve en `/`, `/console` y `/console/` — un `WebMvcConfig` reenvía esas rutas a `index.html` porque el manejador de estáticos no resuelve un directorio a su índice. La página es pública (whitelist), pero cada llamada a la API sigue exigiendo token/rol. Validada en vivo contra el backend real (login admin + ruta de 0.653 km dibujada). No toca el esquema v6 | Decidido |
| 32 | **Endurecimiento F12 y export de OpenAPI.** (a) `NoResourceFoundException → 404` limpio en `GlobalExceptionHandler` (antes lo tomaba el handler genérico como 500). (b) La revisión de seguridad se fija como prueba viva (`SeguridadIT`): 401 sin token, 403 con rol equivocado, y errores con `codigo`/`mensaje` sin trazas ni clases Java (RN-016); los DTOs no aceptan id de usuario del cliente (siempre de `SecurityUtils`). (c) `OpenApiExportIT` escribe `docs/openapi.json` en cada `mvn verify` con Docker (evidencia versionada). (d) **Diferido (Rubén, 2026-09-05):** la prueba de carga de 100 transacciones concurrentes (RNF-007) se hará después | Decidido |
| 31 | **Reloj inyectable y jobs conmutables (F11, RF-025).** Los tres jobs (`PedidosProgramadosJob`, `SesionesExpiradasJob`, `LotesPorCaducarJob`) reciben un `Clock` (`ClockConfig` → `Clock.systemDefaultZone()`) en vez de llamar a `now()` estático, para que los tests sean deterministas. `@EnableScheduling` vive en `SchedulingConfig` con `@ConditionalOnProperty(h2togo.scheduling.enabled, matchIfMissing=true)`; el perfil `test` lo pone en `false`, así los jobs NO se autodisparan y `SchedulingFlowIT` los invoca a mano tras sembrar datos (adelanta `fecha_programada`/`sesion_fecha_expiracion`/`fecha_caducidad` por query nativa, ya que las validaciones de alta impiden crear registros «vencidos» directamente). La activación de programados usa `FOR UPDATE SKIP LOCKED` (Q7) y `notificado_programado` evita doble proceso; la merma de caducados inserta `salida_manual`. No se tocó el esquema v6 | Decidido |
| 26 | **Cancelación solo en Pendiente (F6).** La ficha CU-005 dice explícitamente "solo se puede cancelar en estado Pendiente; una vez Asignado la opción se oculta", contradiciendo la tabla RN-005 ("Pendiente o Asignado") y al plan F6 ("pendiente\|programado\|asignado"). **Gana la ficha CU-005:** cancelable en `pendiente` y `pendiente_programado` (variantes no asignadas); en `asignado`/`en_camino` → 422. Coherente con que el apartado (y por tanto la liberación, RN-023) vive en F7 | Decidido (revisar si prefieren permitir cancelar asignado) |
| 25 | **Apartado al ACEPTAR, no al crear (F6).** RN-022 ("Al aceptar un pedido, el sistema aparta…") y las postcondiciones de CU-004 (no mencionan apartado) mueven TODO el apartado de inventario y la validación de stock (RF-012/RN-008) a **F7** (aceptación). El plan F6 preveía apartar al crear el pedido directo; **gana la ficha.** La creación en F6 solo valida cobertura, horario, precio (abierta) y suspensión, y captura snapshot de precios (directa) | Decidido |
| 24 | **FIFO y movimientos de carga (F5).** (a) **RN-030 por operación:** el umbral "caducidad > hoy+7d" aplica al REGISTRAR lote (CU-018) y al ENTREGAR (F7); la CARGA al vehículo (CU-019) usa lotes **vigentes = no vencidos** (`fecha_caducidad >= hoy`), como dice la precondición de la ficha. (b) **Orden FIFO determinista:** `ORDER BY fecha_caducidad, id_lote` (a igual caducidad gana el lote más antiguo). (c) **Movimiento traspaso/devolución = UNA fila** con `id_lote_base` + `id_inventario_vehiculo` (lo exige el CHECK de v6), reconciliando el "par de movimientos enlazados" que menciona el TT — decisión ya prevista en el plan. (d) Concurrencia por `SELECT … FOR UPDATE` sobre los lotes (RF-022/RNF-014) | Decidido |
| 23 | **JPA write + lectura nativa: flush explícito (F4, patrón).** Una escritura con `save()` (JPA) NO es visible para una query NATIVA (JdbcTemplate) en la misma transacción hasta sincronizar a la BD (Hibernate auto-flushea antes de queries JPQL, no antes de JDBC crudo). Lo destapó `actualizarPrecio` (devolvía el precio viejo). **Regla:** cuando un servicio escribe por JPA y luego lee por query nativa en la misma transacción, usar `saveAndFlush(...)` (o `EntityManager.flush()`) antes de la lectura nativa | Cerrado |
| 22 | **Horario = edición directa, no solicitud (F4).** La tabla de RF pone RF-031 (definir horario) bajo CU-020 (solicitar cambio), pero **RN-019** —la lista autoritativa de cambios que requieren aprobación del admin— **no incluye el horario** (sí foto, vehículos, nombre, dirección base, producto nuevo). El plan F4 lo trata como edición directa del dueño (`PUT /negocios/me/horarios`, inmediato). **Decidido:** horario se edita directo (dueño), sin pasar por solicitudes. El indicador abierto/cerrado (RN-004) se calcula en zona horaria `America/Mexico_City` | Decidido (revisar si el TT insiste en aprobación) |
| 21 | **Bug de deserialización de booleanos primitivos (corregido en F3).** Boot 4 activa `FAIL_ON_NULL_FOR_PRIMITIVES`: un `boolean` primitivo en un DTO (record) truena con **500** si el cliente OMITE el campo. Lo destapó `DireccionesFlowIT` al mandar login sin `forzar`. **Corregido:** `LoginRequest.forzar` pasó a `Boolean` opcional (default false vía `forzarSesion()`), y se añadió handler `HttpMessageNotReadableException → 400`. Guía para el resto de fases: los campos booleanos opcionales de DTOs van como `Boolean`, no `boolean` | Cerrado |
