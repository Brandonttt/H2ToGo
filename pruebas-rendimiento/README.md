# Pruebas de concurrencia, carga y rendimiento

Cómo ejecutar las pruebas CC y RE de `docs/PLAN_PRUEBAS.docx` contra el backend en Azure y
registrar su evidencia. Las de seguridad (SE-01 a SE-06) ya están en `docs/evidencias/`.

| ID | Qué demuestra | Requisito | Cómo se corre | Dónde |
|---|---|---|---|---|
| CC-01 | 2 repartidores aceptan el mismo pedido: solo uno gana | RF-019, RNF-011 | `AsignacionEntregaFlowIT#aceptacionConcurrenteSoloUnoGana` | `mvn verify` |
| CC-02 | Lo mismo con 10 repartidores | RF-019, RNF-011, R-4 | `AsignacionEntregaFlowIT#aceptacionConcurrente10RepartidoresSoloUnoGana` | `mvn verify` |
| CC-03 | 100 usuarios simultáneos, operaciones mezcladas y una ráfaga de 100 pedidos a la vez | RNF-007 | `cc03_100_usuarios.js` + `PedidosFlowIT#veintePedidosSimultaneosSeCreanTodos` | Azure + `mvn verify` |
| CC-04 | Nada queda a medias si una operación falla | RNF-014 | Pruebas de integración de aceptación y carga (IT-02, IT-09) | `mvn verify` |
| CC-05 | 10 repartidores reservan el mismo programado: solo uno gana | Ruta del día | `PlaneacionFlowIT#reservaConcurrenteSoloUnoGana` | `mvn verify` |
| RE-01 | Ruta A* de un pedido en 5 s o menos | RNF-009 | `re01_re02_rutas.js` | Azure |
| RE-02 | Ruta del día (OR-Tools) con 5 y 10 paradas en 5 s o menos | RNF-009 | `re01_re02_rutas.js` | Azure |
| RE-03 | La ubicación llega al cliente en 10 s o menos | RNF-012 | Dos teléfonos (sección 5) | Teléfonos con 4G |
| RE-04 | Un programado se activa a su hora (tarea cada 5 min o menos) | RNF-013 | `re04_programados.js` | Azure |
| RE-05 | Disponibilidad de 99% de 08:00 a 18:00 | RNF-010 | Monitor externo durante 2 semanas (sección 6) | Azure |

> **Antes de correr CC-03 en Azure hay que desplegar el backend con el arreglo del pool de
> conexiones** (ver "Hallazgo" al final). Sin él, la ráfaga de 100 pedidos falla.

---

## 1. Preparación (una vez)

1. Instala k6: `brew install k6` (macOS) o `winget install k6` (Windows).
2. Elige un horario **sin usuarios reales** y avisa al equipo: CC-03 manda ~70 peticiones por
   segundo durante 6 minutos.
3. **Siembra los usuarios de prueba en la base de Azure.** No hay acceso directo a la base desde
   fuera; se entra al contenedor y se pega el script:

   ```bash
   az containerapp exec -n h2togo-db -g rg-h2togo --command bash
   ```
   Dentro del contenedor:
   ```bash
   psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
   ```
   Pega todo el contenido de `01_datos_carga.sql`. Al final debe imprimir `10 | 100 | 100`
   (repartidores, clientes y direcciones en cobertura). Escribe `\q` y `exit` para salir.

   Crea 10 repartidores y 100 clientes `@carga.h2togo.test` (contraseña `Demo1234`) y la
   purificadora "Prueba de Carga". Todos los pedidos de las pruebas son directos a esa
   purificadora: los repartidores reales no los ven ni reciben avisos.

4. Define la URL de la API para los comandos siguientes:

   ```bash
   export H2TOGO_URL=https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/api/v1
   ```

Todos los comandos de k6 se corren dentro de esta carpeta (`pruebas-rendimiento/`). Cada uno
guarda su resumen en `resultados/` para la evidencia.

## 2. CC-03 · 100 usuarios simultáneos (≈7 min)

```bash
k6 run -e H2TOGO_URL=$H2TOGO_URL --summary-export resultados/cc03.json cc03_100_usuarios.js | tee resultados/cc03.txt
```

Qué hace:
- **Mezcla** (30 s de subida, 5 min con 100 usuarios, 30 s de bajada). Cada usuario virtual
  tiene su propia cuenta y sesión:
  - 10 repartidores: reportan ubicación cada 5 s y consultan disponibles y entregas;
  - 90 clientes: buscan purificadoras, ven un perfil, crean, consultan y cancelan un pedido y
    revisan su lista.
- **Ráfaga:** los 100 clientes crean un pedido en el mismo instante y lo cancelan.

Criterios (k6 marca ✓ o ✗ en `THRESHOLDS`):

| Criterio | Umbral |
|---|---|
| Errores HTTP en la mezcla y en la ráfaga | < 1% |
| Respuestas 5xx (errores del servidor o de la base) | 0 |
| Checks correctos | > 99% |
| p95 de todas las peticiones, de crear pedido y de reportar ubicación | < 3 s |

**Confirmar 0 errores de base de datos** en el log del backend durante la ventana de la prueba:

```bash
az containerapp logs show -n h2togo-api -g rg-h2togo --tail 1000 | grep -iE "ERROR|SQLState|Hikari|deadlock"
```

No debe aparecer nada de la ventana de la prueba.

Para la tesis: transacciones por segundo = `http_reqs` (columna `/s`), y p95 de
`http_req_duration`.

## 3. RE-01 y RE-02 · Rutas (≈2 min)

Correr **antes de las 21:30**: los programados de la prueba deben quedar para el mismo día.

```bash
k6 run -e H2TOGO_URL=$H2TOGO_URL --summary-export resultados/re01_re02.json re01_re02_rutas.js | tee resultados/re01_re02.txt
```

Reporta promedio y p95 de `re01_ruta_astar_ms`, `re02_ruta_dia_5_paradas_ms` y
`re02_ruta_dia_10_paradas_ms` (20 mediciones cada una); el límite es 5 000 ms.

**La primera ruta del día tarda más** (en local, 9.9 s contra 1.2 a 2 s las demás): es la
carga inicial de OR-Tools tras un despliegue o reinicio. Repórtala aparte como "primera
ejecución" y no la mezcles con el p95. Para que la medición sea de régimen normal, corre el
script dos veces y reporta la segunda (antes, corre la limpieza y la siembra de nuevo).

Complemento con la app (plan: "desde que se abre la ruta hasta que aparece en el mapa"): con 4G,
cronometra 20 veces cuánto tarda en dibujarse la ruta al abrir un pedido asignado.

## 4. RE-04 · Activación de programados (≈12 min)

```bash
k6 run -e H2TOGO_URL=$H2TOGO_URL --summary-export resultados/re04.json re04_programados.js | tee resultados/re04.txt
```

Programa 20 pedidos (el primero a 3 min, luego uno cada 20 s) y mide el retraso entre la hora
programada y la activación registrada en el historial. Criterio: el máximo de
`re04_retraso_activacion_s` es menor a 300 s (la tarea corre cada 60 s, así que lo esperado es
menos de 60 s).

## 5. RE-03 · Ubicación en vivo (manual, dos teléfonos)

1. Teléfono A: repartidor con un pedido en camino. Teléfono B: el cliente de ese pedido, en la
   pantalla de seguimiento. Ambos con datos móviles (4G), no Wi-Fi.
2. Graba con un tercer teléfono, o con grabación de pantalla, ambos teléfonos y un reloj con
   segundos visibles.
3. Camina con el teléfono A. Anota 20 veces la hora en que el punto azul del A se mueve y la hora
   en que el marcador del repartidor se mueve en el B.
4. Reporta promedio y p95 de la diferencia. Límite: 10 s. Valor diseñado: la app reporta cada 5 s.

## 6. RE-05 · Disponibilidad (empezar hoy: son 2 semanas)

1. Revisa que el backend no se apague por falta de uso: con 0 réplicas mínimas, la primera
   petición tras un rato tarda en arrancar y el monitor la contaría como caída.

   ```bash
   az containerapp show -n h2togo-api -g rg-h2togo --query properties.template.scale
   ```
   Debe decir `"minReplicas": 1`.
2. Crea un monitor gratuito en uptimerobot.com (tipo HTTP(s), cada 5 min) para
   `.../api/v1/health`. El plan gratuito revisa cada 5 min: son 120 revisiones por día de 08:00 a
   18:00, suficiente para medir 99% (cada caída de 5 min resta 0.8%).
3. Al terminar las 2 semanas, exporta el reporte y calcula: revisiones correctas de 08:00 a 18:00
   ÷ revisiones totales en ese horario.

## 7. Limpieza (obligatoria al terminar)

Entra a la base como en el paso 1.3 y pega `99_limpieza_carga.sql`. Al final debe imprimir
`0 | 0 | 0`. Borra la purificadora de prueba, sus usuarios, pedidos, historial y movimientos. Para
eso desactiva por un momento los triggers de bitácora inmutable, solo dentro de su transacción.

## 8. Registrar la evidencia

Por cada prueba, crea `docs/evidencias/<ID>/evidencia_<ID>.md` con el mismo formato que las de
SE: contexto (fecha, commit, ambiente), procedimiento, resultado y la salida de k6 de
`resultados/`. Después llena "Medición obtenida" y "Resultado" en `docs/PLAN_PRUEBAS.docx`.

---

## Hallazgo: interbloqueo del pool de conexiones (corregido)

El primer ensayo de la ráfaga falló: de 100 pedidos simultáneos, 90 esperaron 30 s y fallaron.

**Causa:** al confirmar una petición, el aviso de "pedido nuevo" y la entrega de notificaciones
pedían una segunda conexión a la base, mientras la petición todavía tenía la suya. Con 10
peticiones a la vez, las 10 conexiones del pool quedaban ocupadas esperando otra.

**Arreglo:** ese trabajo ahora corre en segundo plano
(`TareaSistema.despuesDelCommit`). La petición suelta su conexión primero.

**Resultado:** en el ensayo local, los 100 pedidos simultáneos se crearon con p95 de 155 ms.
Una prueba automática con 20 pedidos simultáneos
(`PedidosFlowIT#veintePedidosSimultaneosSeCreanTodos`) falla con el código anterior y pasa con
el nuevo.

Vale la pena mencionarlo en la tesis: es exactamente el tipo de error que solo aparece con
pruebas de concurrencia.
