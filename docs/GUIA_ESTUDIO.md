# Guía de estudio técnica de H2ToGo

Todo lo que hay que dominar para presentar el sistema y responder preguntas técnicas. Cada tema
dice **qué es**, **cómo lo usamos en H2ToGo** (con la clase o el archivo donde vive) y **qué te
pueden preguntar**.

**Orden sugerido de estudio** (de lo que más preguntan a lo que menos):

1. Grafos, Dijkstra y A* (secciones 1 a 4)
2. Kosaraju y componentes fuertemente conexas (5)
3. Problema del viajero y OR-Tools (6 y 7)
4. RabbitMQ y colas de mensajes (8)
5. Spring Boot, JPA, Hibernate y JDBC (10 y 11)
6. Contenedores, Docker y Azure (14 y 15)
7. El resto: WebSocket/STOMP, PostgreSQL/PostGIS, seguridad, Kotlin Multiplatform, Firebase

---

## 1. Grafos: lo básico

Un **grafo** es un conjunto de **nodos** unidos por **aristas**. En H2ToGo:

- **Nodo** = un punto de la red de calles de OpenStreetMap (una esquina o un punto donde la calle cambia de dirección).
- **Arista** = un tramo de calle entre dos nodos.
- **Peso** de la arista = su longitud en km, calculada con la fórmula de Haversine.
- Es un grafo **dirigido**: una calle de doble sentido son dos aristas (A→B y B→A); una de un solo sentido (`oneway=yes` en OSM) es una sola.

| Dato | Valor |
|---|---|
| Nodos | 27,278 |
| Aristas dirigidas | 42,239 |
| Archivo | `h2togo-backend/src/main/resources/osm_data/sample_map.json` (~5 MB, va dentro del jar) |
| Dónde se carga | `OsmGraphLoader` → `Grafo` (lista de adyacencia: `Map<idNodo, List<Arista>>`) |
| Cuándo | Una sola vez al arrancar (`RoutingWarmup`); luego queda en memoria, solo lectura |

**Lista de adyacencia:** para cada nodo se guardan sus aristas de salida. Obtener los vecinos de un
nodo cuesta O(1), que es lo que más hacen Dijkstra y A*.

**Haversine:** distancia en línea recta entre dos coordenadas sobre la superficie de la Tierra
(considera que es una esfera). Sirve como peso de las aristas y como heurística de A*.

---

## 2. Dijkstra

**Problema:** el camino más corto desde un nodo origen hacia los demás, en un grafo con pesos no negativos.

**Idea:** explorar en "círculos crecientes" de distancia desde el origen.

1. La distancia al origen es 0 y a todos los demás es ∞.
2. Se saca de una **cola de prioridad** el nodo con menor distancia conocida. Esa distancia ya es definitiva: ese nodo queda "fijo".
3. Se revisan sus vecinos: si llegar por este nodo es más barato que lo conocido, se actualiza (esto se llama **relajar** la arista).
4. Se repite hasta vaciar la cola (o hasta fijar los nodos que nos interesan).

**Por qué funciona:** como los pesos no son negativos, cuando un nodo sale de la cola con la menor
distancia, ningún otro camino puede mejorarla después.

**Complejidad:** O((V + E) log V) con cola de prioridad (montículo binario). V = nodos, E = aristas.

**En H2ToGo:** `MatrizDistancias` (paquete `routing.service`). Se usa para la **ruta del día**: calcula
la distancia por calles entre todas las paradas (ver sección 4 para el porqué).

---

## 3. A* (A estrella)

**Problema:** el camino más corto de **un origen a un destino**.

**Idea:** Dijkstra "con brújula". En vez de expandir el nodo más cercano al origen, expande el que
minimiza:

```
f(n) = g(n) + h(n)
g(n) = costo real desde el origen hasta n
h(n) = estimación de lo que falta de n al destino (heurística)
```

En H2ToGo, h(n) = **distancia Haversine en línea recta** de n al destino.

**Heurística admisible:** nunca sobreestima el costo real. La línea recta siempre es menor o igual que
cualquier camino por calles, así que Haversine es admisible y **A\* garantiza el camino óptimo**.

**Ventaja sobre Dijkstra:** explora muchos menos nodos, porque prioriza los que van "hacia" el
destino. Dijkstra explora en círculo; A* en un "óvalo" orientado al destino.

**En H2ToGo:** `AEstrella` (paquete `routing.service`). Se usa en:
- la ruta del repartidor a un pedido (`GET /pedidos/{id}/ruta`);
- la ruta que ve el cliente en el seguimiento (`GET /pedidos/{id}/ruta-seguimiento`);
- dibujar cada tramo de la ruta del día en el mapa (de una parada a la siguiente).

**Detalle de implementación:** el origen y el destino se "pegan" al nodo más cercano del grafo
(`Grafo.nodoMasCercano`), porque un GPS o un domicilio casi nunca cae exactamente en un nodo.

---

## 4. ¿Por qué Dijkstra para la matriz y no A*?

La ruta del día necesita una **matriz de distancias**: la distancia por calles de cada punto a cada
otro punto (salida del repartidor + paradas). Con 10 paradas son 11 puntos y 110 distancias.

| | A* | Dijkstra |
|---|---|---|
| Responde | 1 origen → **1** destino | 1 origen → **todos** los destinos |
| Para 11 puntos | 11 × 10 = **110 búsquedas** | **11 búsquedas** (una desde cada punto) |
| Heurística | Necesita saber hacia qué destino apuntar | No necesita |

**A\* no sirve bien aquí** porque su ventaja (la heurística) depende de tener **un solo destino**.
Con 10 destinos a la vez no hay a dónde "apuntar". Habría que correr A* una vez por cada par
(110 veces), y cada búsqueda repetiría el trabajo de explorar las mismas calles cercanas al origen.

**Dijkstra sí sirve:** desde cada punto avanza en círculos y en una sola pasada encuentra la
distancia a **todas** las paradas. Nuestra versión se detiene en cuanto fija la última parada
(no explora toda la ciudad).

**Dato técnico para el asesor:** Dijkstra es exactamente A* con h(n) = 0. Cuando hay muchos destinos,
la mejor heurística posible es "ninguna".

**Resumen para decir en voz alta:**
> "Usamos A* cuando hay un destino: la ruta a un pedido y cada tramo dibujado en el mapa. Para la
> matriz que necesita OR-Tools usamos Dijkstra desde cada parada: n búsquedas en vez de n·(n−1),
> porque Dijkstra encuentra todos los destinos en una sola pasada."

**La matriz no es simétrica:** por los sentidos únicos, ir de A a B puede costar distinto que de B a A.

---

## 5. Kosaraju y componentes fuertemente conexas

### El problema que resuelve

Los datos de OSM traen **fragmentos de calle sueltos**: privadas, accesos a estacionamientos, tramos
cortados en el borde del mapa, calles de un solo sentido sin salida. Ejemplos:

```
Red principal  ●━━━●━━━●━━━●          (calles de doble sentido, todo conectado)

Fragmento        ○━━━○                 (tramo suelto, no toca la red principal)

Trampa           ●━━━●──→○──→○         (un sentido: entras pero no puedes volver)
```

Si un domicilio o el GPS del repartidor se "pegaba" a un nodo de un fragmento, **no había camino**
desde ni hacia ahí. A* devolvía "ruta no encontrada" y OR-Tools descartaba la parada. Esto ya pasaba
antes con algunas direcciones; lo encontré al probar la ruta del día.

**Medición real:** de 27,278 nodos, **24,579 (90%)** forman la red principal. Los otros 2,699 (10%)
son fragmentos.

### Conceptos

- **Alcanzable:** B es alcanzable desde A si hay un camino A → … → B respetando los sentidos.
- **Componente fuertemente conexa (CFC, o SCC en inglés):** grupo máximo de nodos donde **cada uno es alcanzable desde cada otro**. Dentro de una CFC siempre se puede ir y volver.
- **Red principal** = la CFC más grande. Si tanto el origen como el destino están en ella, **siempre existe ruta**.

¿Por qué "fuertemente" y no solo "conectado"? Porque en un grafo dirigido estar conectado no
basta: en la "trampa" de arriba los nodos ○ están conectados a la red, pero si te pegas a uno puedes
llegar y no salir (o al revés).

### El algoritmo de Kosaraju

Encuentra todas las CFC con **dos recorridos en profundidad (DFS)**:

1. **DFS sobre el grafo original.** Se anota el orden en que cada nodo **termina** (cuando ya se exploró todo lo que se alcanza desde él).
2. **Se invierte el grafo** (cada arista A→B pasa a ser B→A); a esto se le llama grafo **transpuesto**.
3. **DFS sobre el grafo invertido**, tomando los nodos en **orden inverso de terminación**. Cada árbol que se forma en este segundo recorrido es una CFC.

**Intuición de por qué funciona:** el nodo que termina al último en el paso 1 pertenece a una
componente "fuente" (de la que se sale pero a la que no se llega desde otras). En el grafo invertido
esa componente queda "atrapada": el DFS desde ahí solo puede recorrer su propia componente. Al
avanzar en orden inverso de terminación se van "pelando" las componentes una por una sin mezclarlas.

**Complejidad:** O(V + E), lineal. Se calcula **una sola vez** al arrancar el backend.

**Alternativa equivalente:** el algoritmo de Tarjan encuentra las CFC con un solo DFS. Elegimos
Kosaraju porque es más fácil de explicar y de implementar sin recursión.

### Cómo lo usamos

En `Grafo.java`:

- `componentePrincipal()` corre Kosaraju y guarda la CFC más grande.
- `nodoMasCercano(lat, lon)` busca solo dentro de esa componente. Así cualquier punto se pega a la calle conectada más cercana, no a un fragmento.
- Está implementado **iterativo**, con una pila propia en lugar de recursión. Con miles de nodos, la recursión podría desbordar la pila de Java (`StackOverflowError`).
- **Salvaguarda:** solo se aplica si la componente principal tiene más de la mitad de los nodos. Así no afecta grafos pequeños de prueba.

**Efecto:** mejora la ruta del día, la ruta del repartidor y el seguimiento del cliente.

**Prueba:** `MatrizDistanciasTest` arma un grafo de juguete con un fragmento suelto y verifica que la
red principal lo excluye.

---

## 6. El problema del viajero (TSP)

**Problema clásico (Traveling Salesman Problem):** dadas varias ciudades, ¿en qué orden visitarlas para
recorrer la menor distancia?

**¿Por qué es difícil?** Es **NP-difícil**: no se conoce un algoritmo que lo resuelva exacto en tiempo
polinomial. El número de órdenes posibles crece como factorial:

| Paradas | Órdenes posibles (n!) |
|---|---|
| 5 | 120 |
| 10 | 3,628,800 |
| 15 | 1,307,674,368,000 (1.3 billones) |

**Formas de resolverlo:**
- **Exactas:** fuerza bruta (n!) o programación dinámica de Held-Karp (O(n²·2ⁿ)), que solo es práctica hasta unas 15–20 paradas.
- **Heurísticas y metaheurísticas:** encuentran soluciones muy buenas, normalmente óptimas en casos pequeños, en un tiempo acotado. Es lo que usan en la práctica las empresas de logística.

**Variantes que nosotros resolvemos** (más realista que el TSP puro):
- **Con ventanas de horario (TSPTW):** cada pedido programado tiene un rango de hora de llegada.
- **Con capacidad:** el vehículo solo carga cierto número de garrafones.
- **Ruta abierta:** termina en la última entrega, sin regreso a la base.
- **Paradas opcionales:** si algo no cabe o no tiene camino, se deja fuera y se reporta.

---

## 7. OR-Tools y la ruta del día

**OR-Tools** es la librería de optimización de Google (código abierto). Su módulo de **ruteo de
vehículos** resuelve TSP y VRP (varios vehículos) con restricciones. La usamos desde Java
(`com.google.ortools:ortools-java`). Está escrita en C++, por eso trae binarios nativos por
plataforma: Linux x86-64 para Azure, Linux ARM64 para Docker en Mac, macOS ARM y Windows.

### Cómo modelamos el problema (`OptimizadorRuta.java`)

| Elemento | Cómo se modela |
|---|---|
| Nodo 0 | Posición actual del repartidor (o la base del negocio si no hay GPS) |
| Nodos 1..n | Paradas |
| Ruta abierta | Un nodo final **ficticio** al que se llega desde cualquier parada con costo 0 |
| Costo a minimizar | Metros por calles (de la matriz de Dijkstra) |
| Dimensión "Tiempo" | Minutos de manejo a 20 km/h promedio + 5 min por entrega; se permite **esperar** si se llega antes |
| Ventanas de horario | Programados: hora ± 30 min. Llegar antes = esperar (límite duro); llegar tarde = penalización por minuto (límite **suave**, para que una hora ya vencida no deje el problema sin solución) |
| Dimensión "Carga" | Suma de garrafones ≤ capacidad del vehículo |
| Paradas opcionales | **Disyunción** con penalización enorme (equivale a 1,000 km): el solver solo omite una parada si no cabe o no tiene camino |
| Pedido "en camino" | Ventana [0, 0]: se fuerza a ser la primera parada |

### Cómo busca la solución

1. **Solución inicial con PATH_CHEAPEST_ARC:** desde el inicio, siempre toma la arista más barata disponible. Es una heurística "golosa" (greedy): rápida pero no óptima.
2. **Mejora con Guided Local Search (GLS):** prueba cambios pequeños (mover una parada, intercambiar dos, invertir un tramo) y se queda con los que mejoran. Para no atorarse en un óptimo local, **penaliza las aristas** que aparecen mucho en soluciones malas y así obliga a explorar otras.
3. **Límite de tiempo:** 300 ms + 150 ms por parada, máximo 3 s. Con 5 paradas la respuesta total tarda ~1.5 s.

### Todo el flujo de la ruta del día

```
App (Mi ruta del día)
  │ GET /repartidores/me/ruta-optima
  ▼
PlaneacionService
  1. Paradas del repartidor: asignadas, en camino y programados de hoy reservados
  2. Puntos = [salida, parada1, ..., paradaN]
  3. MatrizDistancias  → Dijkstra desde cada punto (sobre la red principal, Kosaraju)
  4. OptimizadorRuta   → OR-Tools: orden, hora estimada de llegada y paradas omitidas
  5. A* entre paradas consecutivas → polilínea por calles para el mapa
  ▼
Respuesta: paradas en orden, km, minutos, carga/capacidad, omitidos con motivo, polilínea
```

**Agenda y reservas:** el repartidor "toma" un programado de hoy con `POST /pedidos/{id}/reserva`. Es un
**UPDATE condicional** (`... WHERE id_repartidor IS NULL`): si dos lo intentan a la vez, solo uno lo
consigue (el otro recibe 409). La reserva no aparta garrafones; eso pasa al **aceptar**, cuando el
repartidor va a esa parada.

**RN-009:** si el cliente cancela un pedido reservado, el backend avisa al repartidor (tipo
`ruta_recalculada`) y la app vuelve a pedir la ruta, ya sin ese pedido.

### Preguntas probables

- **¿Por qué no programaron su propio algoritmo?** Held-Karp sería exacto para pocas paradas, pero solo resuelve el TSP puro. Agregar horarios, capacidad, paradas opcionales y ruta abierta lo complica mucho. OR-Tools es estándar en la industria y resuelve todo eso. Lo que sí es propio son las piezas alrededor: A*, Dijkstra, Kosaraju y el modelado.
- **¿Garantiza el óptimo?** No; el TSP es NP-difícil. Con pocas paradas casi siempre encuentra el óptimo, y la búsqueda local garantiza una solución muy buena dentro del límite de tiempo.
- **¿Por qué 20 km/h y 5 min?** Son supuestos configurables en `application.yml` (`h2togo.planeacion`). Con datos reales de entregas se podrían calibrar.
- **¿Qué pasa con el tráfico?** No se considera: el peso de las aristas es la distancia, no el tiempo real. Es trabajo a futuro (por ejemplo, con velocidades por tipo de calle).

---

## 8. RabbitMQ y colas de mensajes

### Conceptos

- **Broker de mensajes:** programa intermediario que recibe mensajes, los guarda y los entrega. El que envía y el que procesa no se hablan directamente.
- **Productor:** el que publica el mensaje (nuestro backend, `NotificacionesEnCola`).
- **Exchange:** "oficina de clasificación". Recibe el mensaje y decide a qué cola mandarlo según su **routing key** (etiqueta). Tipos: direct, fanout, **topic** (por patrón, el que usamos) y headers.
- **Cola (queue):** donde esperan los mensajes. **Durable** = sobrevive a un reinicio del broker.
- **Binding:** la regla que une un exchange con una cola (por ejemplo, `notificacion.#`).
- **Consumidor:** el que procesa los mensajes (`NotificacionesListener`, con `@RabbitListener`).
- **ACK:** el consumidor confirma que procesó el mensaje; hasta entonces el broker no lo borra.
- **Reintentos:** si el procesamiento falla, se vuelve a intentar.
- **DLX / DLQ:** *dead letter exchange* y *dead letter queue*. Adonde van los mensajes que fallaron todos sus intentos, para revisarlos después.
- **Al menos una vez (at-least-once):** un mensaje no se pierde, pero puede llegar dos veces. Por eso el consumidor debe tolerar duplicados (idempotencia).

### Nuestra topología (`MensajeriaConfig.java`)

```
EntregaService (guarda "entregado")
  └─▶ PushService.notificar(...)  ── espera al COMMIT de la transacción (DespuesDelCommit)
        └─▶ exchange h2togo.eventos (topic), routing key "notificacion.push"
              └─▶ cola h2togo.notificaciones (durable, JSON)
                    └─▶ NotificacionesListener (4 intentos: espera 1 s, 2 s, 4 s)
                          └─▶ EntregaNotificaciones
                                ├─▶ WebSocket /user/queue/notificaciones  (app abierta: Android e iOS)
                                └─▶ FcmClient → Firebase                   (Android con la app cerrada)
        si falla 4 veces ─▶ exchange h2togo.eventos.dlx ─▶ cola h2togo.notificaciones.dlq
```

### Por qué lo usamos

- **No hacer esperar al usuario:** la petición responde en cuanto se guarda el cambio; el envío pasa en segundo plano.
- **No perder avisos:** si Firebase falla, se reintenta; lo que no se pudo entregar queda en la DLQ.
- **Desacoplar:** los servicios solo dicen "avisa a este usuario"; no saben de Firebase ni de WebSocket.

### Detalles que demuestran buen diseño

- **Después del commit:** si la transacción se revierte (por ejemplo, el repartidor estaba lejos), el aviso nunca sale. No se avisa algo que no pasó.
- **Dos implementaciones de `PushService`:** con `h2togo.mensajeria.habilitada=true` se usa la cola (producción); con `false`, la entrega es directa (desarrollo y pruebas). Spring elige con `@ConditionalOnProperty` y ningún servicio cambia.
- **Errores que no se reintentan:** si Firebase dice que el token es inválido, se borra el token y se termina; reintentar no serviría.
- **Duplicados:** cada notificación lleva un `id` único y la app descarta repetidos.

### Preguntas probables

- **¿Para qué, si solo hay un servidor?** No es para escalar: es por confiabilidad y para no bloquear al usuario. Si hubiera varios servidores, todos podrían consumir de la misma cola sin cambios.
- **¿Y si se cae RabbitMQ?** El cambio ya quedó guardado y no se revierte; el aviso se pierde y queda en el log. El cliente igual ve el estado correcto porque la app consulta el pedido periódicamente. La mejora sería el **patrón outbox**: guardar el aviso en una tabla dentro de la misma transacción y publicarlo después.
- **¿Por qué no Azure Service Bus?** Habla AMQP 1.0; RabbitMQ usa AMQP 0-9-1, y Spring AMQP está hecho para RabbitMQ. Además RabbitMQ corre igual en local (Docker) y en Azure.

---

## 9. WebSocket y STOMP

- **WebSocket:** conexión persistente y bidireccional sobre HTTP; el servidor puede **empujar** datos sin que el cliente pregunte.
- **STOMP:** protocolo de mensajería simple que corre encima del WebSocket. Agrega comandos (`CONNECT`, `SUBSCRIBE`, `SEND`, `MESSAGE`) y **destinos** tipo tópico.

**En H2ToGo** (`WebSocketConfig`, endpoint `/ws`):

| Destino | Para qué |
|---|---|
| `/topic/pedidos/{id}/ubicacion` | Posición del repartidor en vivo, aviso de llegada y cierre del pedido |
| `/user/queue/notificaciones` | Notificaciones privadas de cada usuario |

- **Seguridad:** `StompAuthChannelInterceptor` valida el token en `CONNECT` y en cada `SUBSCRIBE` revisa con `AccesoRastreo` que solo el cliente y el repartidor del pedido puedan escuchar.
- **Heart-beats cada 10 s:** si la app deja de recibirlos, da la conexión por muerta y reconecta. Es necesario porque iOS suspende la app en segundo plano y el socket puede quedar "zombie".
- **Cliente:** `StompCliente.kt`, un cliente STOMP mínimo propio sobre el WebSocket de Ktor, compartido por Android e iOS.

**¿Por qué no consultar cada pocos segundos (polling)?** Con WebSocket el servidor manda la posición
solo cuando cambia, en vez de que cada cliente pregunte una y otra vez. Hay respaldo: si el
WebSocket no conecta, la app consulta la ubicación por REST cada 10 s.

---

## 10. Spring Boot

**Spring** es un framework de Java para aplicaciones empresariales. **Spring Boot** lo hace fácil de
arrancar: configuración automática y servidor web (Tomcat) incluido; se corre con `java -jar`.

### Conceptos clave

- **Inversión de control (IoC) e inyección de dependencias (DI):** las clases no crean sus dependencias; las declaran en el constructor y Spring se las pasa. Por ejemplo, `EntregaService` recibe `PushService` y no sabe qué implementación es.
- **Bean:** objeto administrado por Spring (`@Service`, `@Component`, `@RestController`, `@Configuration` + `@Bean`).
- **Autoconfiguración y starters:** al agregar `spring-boot-starter-web`, Boot configura Tomcat, Jackson (JSON), etc.
- **Perfiles:** `dev`, `test`, `prod` cambian la configuración (`application-*.yml`).
- **`@ConditionalOnProperty`:** crea un bean solo si una propiedad tiene cierto valor (ejemplo: cola o entrega directa).
- **`@Transactional`:** todo el método es una transacción: o se guarda todo o nada.
- **`@Scheduled`:** tareas periódicas (`PedidosProgramadosJob` cada minuto, `LotesPorCaducarJob` y `SesionesExpiradasJob` diarios).
- **Filtros de seguridad (Spring Security):** `TokenAuthFilter` valida el token antes de llegar al controlador.

### Arquitectura en capas

```
Petición HTTP
  → TokenAuthFilter (seguridad)
  → @RestController   (recibe, valida, responde JSON)
  → @Service          (reglas de negocio, @Transactional)
  → Repository / JdbcTemplate (acceso a datos)
  → PostgreSQL + PostGIS
```

**Organización por dominio:** 21 paquetes (`pedidos`, `tracking`, `routing`, `planeacion`,
`notificaciones`, `inventario`, `auth`, `admin`…). Cada uno tiene su controlador, servicio, entidades y DTOs.

---

## 11. JPA, Hibernate y JDBC

| Término | Qué es |
|---|---|
| **JDBC** | La API básica de Java para mandar SQL a una base de datos |
| **ORM** | *Object-Relational Mapping*: mapear tablas a clases y filas a objetos |
| **JPA** | La **especificación** (el estándar, solo interfaces y anotaciones) para ORM en Java |
| **Hibernate** | La **implementación** de JPA que usa Spring Boot (la que hace el trabajo) |
| **Spring Data JPA** | Genera los repositorios: `PedidoRepository extends JpaRepository<Pedido, Integer>` da `findById`, `save`, etc. sin escribir SQL |

**En H2ToGo usamos los dos enfoques**, cada uno donde conviene:

- **JPA/Hibernate** para entidades y CRUD normal: `@Entity Pedido`, `Usuario`, `Negocio`… Por ejemplo, cancelar un pedido usa la entidad y su historial.
- **SQL directo con `NamedParameterJdbcTemplate`** donde JPA estorba:
  - consultas **PostGIS** (`ST_DWithin`, `ST_Distance`);
  - **concurrencia**: el UPDATE condicional al aceptar o reservar un pedido;
  - bloqueos `SELECT … FOR UPDATE` en inventario;
  - `FOR UPDATE SKIP LOCKED` en el job de programados.

**Configuración importante:**
- `ddl-auto: validate`: Hibernate **no** crea ni cambia tablas. Solo valida al arrancar que las entidades coincidan con el esquema (`H2ToGo_v6_postgresql.sql`). Un desajuste se detecta al desplegar, no en producción.
- `open-in-view: false`: la sesión de Hibernate no se queda abierta durante la vista. Evita consultas ocultas y conexiones retenidas.

**Pregunta probable: ¿por qué mezclar?** Porque JPA es cómodo para objetos, pero las funciones
geográficas y las operaciones atómicas de concurrencia se expresan mejor y más eficientes en SQL.

---

## 12. PostgreSQL y PostGIS

- **PostgreSQL 16:** base de datos relacional. Tenemos 19 tablas en el esquema v6.
- **PostGIS 3.4:** extensión para datos geográficos.
  - Tipo `geography(Point, 4326)`: coordenadas lat/lon sobre el elipsoide WGS84, el mismo sistema que usa el GPS.
  - `ST_DWithin(a, b, metros)`: ¿están a menos de X metros? Lo usamos para el radio de entrega (50 m), la proximidad (500 m) y la zona de cobertura.
  - `ST_Distance`: distancia en metros. Ordena los pedidos disponibles por cercanía.
  - **Índice GIST:** índice espacial que acelera esas consultas.
- **Trigger de cobertura:** al guardar una dirección, un trigger calcula `en_zona_cobertura` comparándola con el polígono de Benito Juárez. El backend no la escribe.

**¿Por qué no MariaDB, como decía la tesis?** PostGIS calcula distancias geodésicas reales en metros,
con índice espacial. Las funciones espaciales de MariaDB son más limitadas.

---

## 13. Seguridad

- **Contraseñas:** BCrypt (hash con sal; no se pueden revertir).
- **Token opaco:** al iniciar sesión se genera una cadena aleatoria guardada en `usuarios.token_sesion`, con vigencia de 30 días que se renueva con el uso. `TokenAuthFilter` la busca en cada petición.
  - **¿Por qué no JWT?** RN-018 exige **una sola sesión activa** y poder cerrarla al instante. Un JWT no se puede revocar sin mantener una lista negra; el token en BD se invalida reemplazándolo.
- **Roles:** cliente, repartidor y admin (`SecurityConfig` + `SecurityUtils.exigirRol`).
- **Autorización por recurso:** solo el repartidor asignado puede entregar; solo el cliente y su repartidor ven el rastreo.
- **Secretos:** credenciales de BD, Twilio, Firebase y RabbitMQ van como variables de entorno o secretos de Azure, nunca en el código.

---

## 14. Contenedores y Docker

### Conceptos

- **Imagen:** "plantilla" inmutable con todo lo necesario para correr un programa: sistema base, Java y nuestro jar.
- **Contenedor:** una imagen **en ejecución**. Proceso aislado con su propio sistema de archivos y red, pero que comparte el kernel del sistema anfitrión. Es más ligero que una máquina virtual.
- **Dockerfile:** receta para construir la imagen.
- **Volumen:** almacenamiento que **sobrevive** cuando el contenedor se borra o reinicia. Indispensable para la base de datos.
- **Registro (registry):** almacén de imágenes; en Azure es Azure Container Registry (ACR).
- **docker-compose:** archivo que levanta varios contenedores juntos, con su red y volúmenes.

### Nuestro Dockerfile (`h2togo-backend/Dockerfile`): construcción en dos etapas

```
Etapa 1 (build):   imagen maven:3.9-eclipse-temurin-21
                   descarga dependencias → compila → genera app.jar
Etapa 2 (runtime): imagen eclipse-temurin:21-jre (solo Java, sin Maven ni código fuente)
                   copia app.jar → java -jar app.jar (puerto 8080)
```

**¿Por qué dos etapas?** La imagen final es más pequeña y segura: no lleva Maven, el código fuente ni
las herramientas de compilación. Usa Ubuntu como base, lo que importa porque OR-Tools necesita glibc
(no funcionaría con Alpine).

### Local: `h2togo-backend/docker-compose.yml`

```
┌──────────────── red de docker-compose ────────────────┐
│  h2togo-db        postgis/postgis:16-3.4   :5432      │◀── volumen h2togo_pgdata
│  h2togo-rabbitmq  rabbitmq:3.13-management :5672/15672│
│  h2togo-app       nuestra imagen           :8088→8080 │
│      se conecta por nombre: db:5432, rabbitmq:5672    │
└───────────────────────────────────────────────────────┘
```

- La primera vez, la BD ejecuta el esquema, la zona de Benito Juárez y los datos de demo.
- La app espera a que la BD y RabbitMQ estén sanos (`healthcheck`) antes de arrancar.
- Dentro de la red, los servicios se encuentran **por nombre** (`db`, `rabbitmq`), no por IP.

### En Azure (ver sección 15): la misma idea, con servicios administrados

```
Azure Container Apps Environment "cae-h2togo" (red privada)
├── h2togo-api       nuestra imagen (desde ACR)  ── ingreso PÚBLICO HTTPS/WSS
├── h2togo-db        postgis/postgis:16-3.4       ── solo red interna, volumen en Azure File Share
└── h2togo-rabbitmq  rabbitmq:3.13-management     ── solo red interna
```

**¿Por qué la BD es una app aparte y no va junto al backend?** Cada despliegue del backend crea una
**revisión nueva** que arranca antes de apagar la anterior. Si PostgreSQL viviera en la misma app,
durante unos segundos habría dos PostgreSQL escribiendo los mismos archivos y se corrompería. Separada,
la BD no se toca al redesplegar el backend.

**Pregunta probable: ¿contenedor vs máquina virtual?** La VM virtualiza el hardware y lleva su propio
sistema operativo completo (pesada, arranca en minutos). El contenedor comparte el kernel y solo
aísla procesos (ligero, arranca en segundos). La misma imagen corre igual en tu Mac, en la Windows de tu
compañero y en Azure.

---

## 15. Microsoft Azure

| Recurso | Para qué | Nombre |
|---|---|---|
| **Container Apps** | Corre los contenedores sin administrar servidores; da HTTPS, escalado y revisiones | `h2togo-api`, `h2togo-db`, `h2togo-rabbitmq` |
| **Container Apps Environment** | La red privada compartida por las tres apps | `cae-h2togo` |
| **Container Registry (ACR)** | Guarda las imágenes Docker | `acrh2togo…` |
| **Storage Account + File Share** | Disco persistente de PostgreSQL | `pgdata` |
| **Log Analytics (Azure Monitor)** | Logs de los contenedores | `law-h2togo` |

**Cómo se despliega** (`desplegar-backend.ps1`):
1. `az acr build`: Azure construye la imagen a partir del código y la guarda en ACR.
2. `az containerapp update --image ...`: la app usa la imagen nueva y se crea una revisión.
3. Se verifica `GET /api/v1/health`.

**URL pública:** `https://h2togo-api.<...>.eastus.azurecontainerapps.io`. Expone `/api/v1/` (REST), `/ws`
(WebSocket), `/admin/` (panel) y `/swagger-ui.html`.

**Pendientes y riesgos (dilos tú antes de que te los pregunten):**
- **CI/CD:** GitHub Actions corre las pruebas; el despliegue es con script. Azure Pipelines quedó pendiente.
- **Application Insights:** pendiente; hoy solo hay logs en Log Analytics.
- **PostgreSQL sobre Azure Files (SMB):** funciona, pero Microsoft no lo recomienda oficialmente para bases de datos. El plan B documentado es Azure Database for PostgreSQL Flexible Server (soporta PostGIS).

**¿Por qué Container Apps y no una VM o App Service?** Corre contenedores directamente (la misma
imagen que en local), da HTTPS y WebSocket listos y cobra por uso. App Service no maneja bien varios
servicios internos (BD y RabbitMQ) en una red privada.

---

## 16. Kotlin Multiplatform (la app)

- **Un solo código** (`h2togo-mobile/shared`) para Android e iOS: pantallas con Compose Multiplatform, ViewModels, red con Ktor, WebSocket, sesión y DTOs.
- **Por plataforma** solo lo que depende del sistema, detrás de interfaces (`UiPlataforma`, `ServiciosPlataforma`): GPS, el WebView del mapa, notificaciones y geocodificación.
- **Android:** el módulo compila a bytecode de la JVM. **iOS:** Kotlin/Native genera `Shared.framework`, que monta una capa mínima en Swift.
- **Patrón MVVM:** las pantallas observan el estado (`StateFlow`) de los ViewModels; los ViewModels usan `ApiClient` (REST) y `StompCliente` (tiempo real).
- **Mapa:** Leaflet + OpenStreetMap en un WebView, con el mismo HTML/JS en las dos apps y en el panel admin.

---

## 17. Firebase Cloud Messaging (FCM)

- Servicio de Google para mandar **notificaciones push** que llegan aunque la app esté cerrada.
- El backend usa la **API HTTP v1** con una cuenta de servicio (`FcmClient`). No usamos el SDK pesado `firebase-admin`.
- La app Android registra su token FCM al iniciar sesión; el backend lo guarda en `usuarios.token_fcm`.
- **iOS:** necesita APNs (Apple Developer, 99 USD al año), así que está pendiente. Con la app abierta, iOS recibe por WebSocket y muestra notificaciones locales.

---

## 18. Preguntas rápidas de repaso

Intenta contestarlas en voz alta en menos de 30 segundos cada una:

1. ¿Qué diferencia hay entre Dijkstra y A*? ¿Cuándo usan cada uno?
2. ¿Qué significa que una heurística sea admisible? ¿Por qué Haversine lo es?
3. ¿Qué es una componente fuertemente conexa? ¿Qué problema resolvía Kosaraju en el proyecto?
4. ¿Por qué el TSP es difícil? ¿Qué hace OR-Tools para resolverlo?
5. ¿Cómo modelan los horarios y la capacidad en OR-Tools?
6. ¿Qué pasa si dos repartidores aceptan o reservan el mismo pedido al mismo tiempo?
7. ¿Para qué sirve RabbitMQ si hay un solo servidor? ¿Qué es una DLQ?
8. ¿Por qué la notificación se manda después del commit?
9. ¿Qué diferencia hay entre JPA, Hibernate y JDBC? ¿Por qué usan los dos enfoques?
10. ¿Qué es un contenedor y en qué se diferencia de una VM? ¿Qué contenedores tiene el sistema?
11. ¿Por qué la base de datos es una Container App separada?
12. ¿Por qué token opaco y no JWT?
13. ¿Cómo detectan una conexión WebSocket muerta?
14. ¿Qué cambiaría para tener dos instancias del backend? (Broker STOMP en memoria → relay de RabbitMQ; avisos de proximidad y llegada guardados en memoria → BD o Redis.)
15. ¿Qué no está terminado y por qué? (Push en iOS, Azure Pipelines, App Insights, OTP real.)

---

## 19. Glosario

| Término | Significado corto |
|---|---|
| Arista dirigida | Tramo que solo se recorre en un sentido |
| Heurística | Estimación que guía una búsqueda |
| Admisible | Heurística que nunca sobreestima |
| CFC / SCC | Grupo de nodos donde todos se alcanzan entre sí |
| DFS | Recorrido en profundidad |
| Grafo transpuesto | El mismo grafo con todas las aristas invertidas |
| NP-difícil | Sin algoritmo exacto conocido en tiempo polinomial |
| Metaheurística | Estrategia general para mejorar soluciones (ej. Guided Local Search) |
| Ventana de horario | Rango de hora en que debe llegar una entrega |
| Disyunción (OR-Tools) | Parada opcional con penalización si se omite |
| Broker | Intermediario de mensajes |
| Exchange / routing key | Clasificador de mensajes / etiqueta para clasificar |
| DLQ | Cola de mensajes que fallaron |
| At-least-once | Garantía de entrega: puede duplicar, no pierde |
| Idempotencia | Repetir una operación da el mismo resultado |
| ORM | Mapeo entre tablas y objetos |
| Bean | Objeto administrado por Spring |
| Imagen / contenedor | Plantilla / plantilla en ejecución |
| Volumen | Almacenamiento persistente de un contenedor |
| Revisión (Container Apps) | Versión desplegada de una app |
| Ingreso (ingress) | Entrada de tráfico externo a una app |
