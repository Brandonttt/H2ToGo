# Guía de demostración — H2ToGo (revisión de avances)

Checklist para enseñar el sistema en vivo desde la **consola web**. Todo usa los datos demo ya
sembrados, así que la mayoría de los pasos son solo un clic. Ten este archivo abierto al lado.

**Consola:** http://localhost:8088/console/  · Panel derecho = registro de cada petición y su respuesta.

**Usuarios** (contraseña para todos: `Demo1234`):

| Rol | Correo |
|---|---|
| Administrador | `admin@h2togo.mx` |
| Dueño / repartidor | `dueno@h2togo.mx` |
| Cliente | `cliente@h2togo.mx` |

> En la pestaña **Sesión**, campo *Correo* + *Password* → **Iniciar sesión**. La píldora de arriba
> a la derecha muestra el rol activo. Para cambiar de actor, vuelve a Sesión e inicia con otro correo.

---

## Demo 1 — Motor de ruteo A\* sobre el mapa  *(≈1 min)*

- [ ] Inicia sesión como **`admin@h2togo.mx`**.
- [ ] Pestaña **Ruteo A\*** → botón **Calcular ruta** (las coordenadas ya vienen puestas).
- [ ] Se dibuja la ruta sobre el grafo real de Benito Juárez y muestra la distancia (~0.65 km).
- 🗣️ *"El motor A\* calcula la ruta óptima sobre ~27 000 nodos reales de OpenStreetMap."*

---

## Demo 2 — Ciclo de vida de un pedido, de extremo a extremo  *(≈3 min)*

**2.1 El repartidor abre su jornada**
- [ ] Inicia sesión como **`dueno@h2togo.mx`** → pestaña **Repartidor**.
- [ ] En *Iniciar jornada* (ya precargado) → **Iniciar jornada**:
```json
{ "idVehiculo": 1, "cargaInicial": [ { "idMarca": 1, "cantidad": 20 } ] }
```

**2.2 El cliente crea un pedido**
- [ ] Inicia sesión como **`cliente@h2togo.mx`** → pestaña **Cliente**.
- [ ] Modalidad *Directa* → **Crear pedido** (ya precargado):
```json
{
  "tipoSolicitud": "directa",
  "idNegocio": 1,
  "idDireccionEntrega": 1,
  "detalles": [ { "idMarca": 1, "cantidad": 2, "tieneEnvase": true } ]
}
```
- [ ] 👉 En la respuesta, apunta el **`"id"`** del pedido (será **1** la primera vez). El estado nace en `pendiente`.

**2.3 El repartidor lo atiende**
- [ ] Inicia sesión como **`dueno@h2togo.mx`** → pestaña **Repartidor** → escribe el **id del pedido** en *Id pedido*.
- [ ] **Aceptar** → el estado pasa a `asignado`.
- [ ] En *En camino* → **Marcar en camino** (precargado): `{ "lat": 19.376692, "lon": -99.165057 }` → estado `en_camino`.
- [ ] En *Reportar ubicación* → **Enviar ubicación** (mismo punto).
- [ ] **Ver ruta en mapa** → dibuja la ruta del pedido hasta la casa del cliente.
- [ ] En *Entregar* → **Registrar resultado** (precargado) → estado `entregado`:
```json
{
  "resultado": "ENTREGADO",
  "lat": 19.377934,
  "lon": -99.171138,
  "lineas": [ { "idDetalle": 1, "cantidadEntregada": 2, "agregadaEnSitio": false } ]
}
```
> `idDetalle` es el `detalles[0].id` del pedido (para el primero es **1**).

- [ ] (Opcional) Vuelve a **Cliente** → *Id pedido* + **Ver (incluye historial)**: muestra el pedido cerrado y todo su historial de estados.
- 🗣️ *"El pedido recorrió `pendiente → asignado → en_camino → entregado`, cada transición registrada y validada."*

---

## Demo 3 — Rastreo en tiempo real (WebSocket)  *(≈2 min)*

Abre la consola en **dos pestañas del navegador**:

- [ ] **Pestaña A (cliente):** login `cliente@h2togo.mx` → **Tracking** → *Id pedido* = el del pedido → **Conectar WS** → **Suscribirse (cliente)**.
- [ ] **Pestaña B (repartidor):** login `dueno@h2togo.mx` → **Tracking** → mismo *Id pedido* → **Conectar WS** → **Publicar**:
```json
{ "lat": 19.3760, "lon": -99.1685 }
```
- [ ] En la **pestaña A** aparece la ubicación al instante. Cambia lat/lon en B y vuelve a **Publicar** para "moverlo".
- 🗣️ *"El cliente ve al repartidor moverse en vivo por WebSocket; así funcionará el mapa de la app."*

---

## Demo 4 — Reglas de negocio (opcional, para mostrar robustez)  *(≈1 min)*

**4.1 Dirección fuera de la zona de cobertura**
- [ ] Login `cliente@h2togo.mx` → **Cliente** → en *Crear dirección* pega esto y **Crear dirección**:
```json
{
  "alias": "Fuera de zona", "calle": "Reforma", "numeroExterior": "1",
  "colonia": "Juárez", "codigoPostal": "06600", "referencias": "prueba",
  "lat": 19.5000, "lon": -99.0000
}
```
- [ ] En la respuesta, `"enZonaCobertura": false` → el sistema detecta que está fuera de Benito Juárez (RN-001).

**4.2 Control de acceso por rol**
- [ ] Con la sesión de **cliente** activa, ve a la pestaña **Admin** → **Usuarios**.
- [ ] Respuesta **`403`**: un cliente no puede entrar al panel de administración.
- 🗣️ *"Cada endpoint valida rol y pertenencia; no basta con estar autenticado."*

---

## Cierre

- 🗣️ Menciona que además del uso manual hay **pruebas automatizadas** que respaldan todo esto
  (integración contra base de datos real), y que el `PLAN_BACKEND.md` documenta las 13 fases y las
  decisiones de diseño. *(Si te lo piden, lo enseñas; si no, con la demo de la consola basta.)*

> **Reinicio limpio** entre ensayos (borra y vuelve a sembrar la base):
> `docker compose down -v && docker compose up -d --build`
