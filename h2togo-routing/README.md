# H2ToGo — Motor de Ruteo A* con OpenStreetMap
**Trabajo Terminal 2026-B176 · IPN**

Motor de ruteo propio construido sobre datos de OpenStreetMap que implementa el algoritmo **A\*** (A-Estrella) con la heurística de **Haversine** como función de estimación. Expuesto como API REST con Spring Boot y visualizable en un mapa Leaflet.js.

---

## Estructura del proyecto

```
h2togo-routing/
├── pom.xml
└── src/main/
    ├── java/com/h2togo/routing/
    │   ├── H2ToGoRoutingApplication.java      ← Punto de entrada
    │   ├── controller/
    │   │   └── RouteController.java           ← POST /api/route
    │   ├── domain/
    │   │   ├── Nodo.java                      ← Intersección OSM
    │   │   ├── Arista.java                    ← Segmento de calle
    │   │   └── Grafo.java                     ← Lista de adyacencia
    │   ├── dto/
    │   │   ├── RouteRequest.java              ← Entrada del endpoint
    │   │   └── RouteResponse.java             ← Salida del endpoint
    │   └── service/
    │       ├── CalculoRutaService.java        ← Interfaz (Strategy)
    │       ├── AEstrella.java                 ← Implementación A*
    │       └── OsmGraphLoader.java            ← Carga OSM → Grafo
    └── resources/
        ├── application.properties
        ├── static/
        │   └── index.html                     ← Cliente web Leaflet
        └── osm_data/
            ├── sample_map.json                ← Datos OSM de ejemplo
            └── overpass_query.txt             ← Query para Overpass Turbo
```

---

## Prerrequisitos

| Herramienta | Versión mínima |
|---|---|
| Java JDK | 17 |
| Apache Maven | 3.8 |
| (Opcional) Docker | para BD del proyecto principal |

---

## Paso a paso para ejecutar

### 1. Compilar y ejecutar

```bash
cd h2togo-routing
mvn spring-boot:run
```

Al arrancar verás en consola:
```
INFO  OsmGraphLoader - Grafo OSM cargado: 13 nodos, 28 aristas.
INFO  Started H2ToGoRoutingApplication in 1.8 seconds
```

### 2. Abrir el cliente web

Abre el navegador en:
```
http://localhost:8080
```

Esto sirve el `index.html` embebido en Spring Boot (carpeta `static`).

### 3. Probar el endpoint directamente (curl / Postman)

```bash
curl -X POST http://localhost:8080/api/route \
  -H "Content-Type: application/json" \
  -d '{
    "origenLat":  19.432608,
    "origenLon": -99.133209,
    "destinoLat": 19.441234,
    "destinoLon": -99.128765
  }'
```

Respuesta esperada:
```json
{
  "encontrada": true,
  "distanciaTotalKm": 1.234,
  "coordenadas": [
    { "lat": 19.432608, "lon": -99.133209 },
    ...
    { "lat": 19.441234, "lon": -99.128765 }
  ]
}
```

---

## Cargar datos reales de OpenStreetMap

El archivo de ejemplo `sample_map.json` contiene solo 13 nodos ficticios.  
Para usar datos reales de tu zona de entrega:

1. Ve a **[Overpass Turbo](https://overpass-turbo.eu/)**
2. Copia el contenido de `src/main/resources/osm_data/overpass_query.txt`
3. Ajusta el bounding box a tu zona (colonia, municipio, etc.)
4. Haz clic en **Ejecutar** y luego en **Exportar → Download directly from Overpass API**
5. Reemplaza `src/main/resources/osm_data/sample_map.json` con el archivo descargado
6. Reinicia la aplicación

---

## Decisiones de diseño

### ¿Por qué Haversine como heurística?

| Candidato | Admisible | Precisa en OSM | Costo O(1) |
|---|---|---|---|
| Euclidiana plana | ✓ | ✗ (curva terrestre) | ✓ |
| Manhattan | ✓ (solo en grillas) | ✗ | ✓ |
| **Haversine** | **✓** | **✓** | **✓** |

Haversine mide la distancia de arco real entre dos puntos sobre la esfera terrestre.  
Nunca sobreestima el costo real de ir de A a B por calles → A\* siempre encuentra la ruta óptima.

### Patrón Strategy

`CalculoRutaService` es la interfaz. `AEstrella` es la implementación actual.  
Para agregar Dijkstra en el futuro:

```java
@Service
@Qualifier("dijkstra")
public class Dijkstra implements CalculoRutaService { ... }
```

Y en `RouteController`, cambiar `@Qualifier("aEstrella")` por `"dijkstra"` — sin tocar ninguna otra clase.

---

## Base de datos del proyecto principal

El esquema SQL completo del proyecto (`htogo_db`) está en [`Query_HToGo.sql`](../Query_HToGo.sql).
