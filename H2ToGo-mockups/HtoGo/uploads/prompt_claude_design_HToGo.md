# PROMPT PARA CLAUDE DESIGN — HToGo (revisión de mockups v2)

Tengo un prototipo HTML de mi app **HToGo** (delivery de garrafones de agua para Benito Juárez, CDMX) con tres roles: **Cliente**, **Repartidor** (dueño de purificadora) y **Admin**. Necesito que actualices los mockups existentes para alinearlos con la base de datos final del proyecto. **No rehagas todo desde cero** — conserva el estilo visual, la paleta `#0077B6`/`#00B4D8`/`#03045E`, los frames de teléfono Android, y la estructura de pantallas. Solo aplica los cambios listados abajo.

---

## CONTEXTO DEL PROYECTO (lee antes de cambiar nada)

HToGo es un marketplace tipo Uber de garrafones, **pero con un giro importante:** el "repartidor" en realidad representa una **purificadora pequeña** (un negocio comercial con nombre propio — "Aguas Del Valle", "HidroExpress BJ", etc.). Una purificadora puede tener **uno o varios repartidores físicos** trabajando bajo su mismo negocio, compartiendo precios, vehículos e inventario base.

El cliente puede pedir agua de dos formas:
1. **Directa** — Eligiendo una purificadora específica desde la lista de cercanas.
2. **Abierta** — Especificando un precio máximo por garrafón. El sistema busca purificadoras que cumplan ese tope; el primer repartidor disponible que acepte se queda el pedido (no aparece más para los demás).

El inventario funciona en **dos niveles**:
- **Base del negocio** (almacén físico) — Stock compartido entre todos los repartidores del mismo negocio.
- **Vehículo** (cada repartidor) — Stock individual cargado en su carro/moto/bici.

Cuando un repartidor acepta un pedido, los garrafones se **apartan** primero del vehículo y, si no alcanzan, también de la base (siempre que la base tenga stock suficiente). Si el repartidor aceptó pedidos por más de lo que carga, la app le avisa "ve a cargar más antes de salir".

---

## ELIMINAR DE TODOS LOS MOCKUPS

1. **Calificaciones / estrellas / ratings.** Quita TODO lo que tenga que ver con esto:
   - Pantalla `home_c`: en las cards de "Repartidores cerca de ti", elimina el `<span class="material-icons star">star</span>4.9` y solo deja distancia + tiempo.
   - Pantalla `track`: quita `4.9` del lado del nombre del repartidor.
   - Pantalla `hist_c`: ningún rating en cards de pedidos.
   - Pantalla `perfil_c`: quita el bloque de "4.9 ★ Promedio" en stats. Deja solo "Pedidos: 14".
   - Pantalla `perfil_r`: elimina toda la sección "Calificaciones recientes" con sus reviews. También quita "★ 4.9 · 247 calificaciones" del header del perfil. Y la stat row de "% concretadas" si está basada en rating.
   - Pantalla `gan_b` y `gan_a`: quita la sección "Calificación / 4.9 / ★★★★★". También el item "Calificación: 4.9 ★" del big-card.
   - Pantalla `ped`: quita "⭐ 4.9" de las cards de cliente.
   - Pantalla `ruta`: quita "⭐ 4.9" del client-card.
   - Pantalla `admin`: quita la columna "⭐" en la tabla "Top repartidores". Quita "4.9 · 247 calificaciones" del modal de detalle de pedido.

2. **Pago en efectivo / billete / cambio.** Eliminar de todas las pantallas:
   - Pantalla `form`: borra TODO el bloque "¿Con qué billete vas a pagar?" con los botones de $200, $500, $1000, "Exacto" y la línea "Cambio a entregar: $75 MXN". El método de pago es siempre efectivo y eso ya se da por entendido — no hace falta declararlo en la UI.
   - Pantalla `track`: en el order-row, borra el renglón "Pagas con $200 · cambio $75". Solo deja "Total a pagar".
   - Pantalla `ruta`: en el pay-box, borra "Cliente paga con $200 · Cambio: $65". Solo deja "Cobrarás en efectivo: $135 MXN".
   - Pantalla `ruta` (modal "Confirmar entrega"): elimina el `change-row` con "Cambio entregado al cliente: $65". Deja solo "Total cobrado en efectivo: $135".

---

## CAMBIOS POR PANTALLA

### `home_c` — Home Cliente

- En "Acciones rápidas" (la grid de 4 tiles), reemplaza la tile "Pedir 1 garrafón / $35" y "Pedir 5 garrafones / $160" por **dos formas claras de hacer pedido**:
  - Tile 1: **"Elegir purificadora"** — icono `storefront`, subtítulo "Pide a un negocio específico"
  - Tile 2: **"Pedir con precio máximo"** — icono `attach_money`, subtítulo "Te asignamos el primero disponible"
  - Mantén las tiles "Repetir último" y "Programar".
- En "Repartidores cerca de ti", **renómbralo a "Purificadoras cerca de ti"**. Cambia el iconito de "delivery_dining" o avatar de persona a `storefront`. El texto bajo el nombre debe ser "0.8 km · 12 min" (sin estrella).
- En el botón FAB inferior "Pedir agua", al hacer click debe ir al `form` pero el flujo cambia (ver siguiente pantalla).

### `purif_c` — Perfil de purificadora (NUEVA pantalla, vista por el cliente)

Crea una pantalla nueva que se abre cuando el cliente hace tap sobre el nombre/avatar de una purificadora desde `home_c` (en la lista "Purificadoras cerca de ti") o desde la card de un pedido en `track`/`hist_c`. Sirve para que el cliente conozca el negocio antes de pedir.

Estructura sugerida:
- **Header**: nombre comercial grande con icono `storefront`, distancia, tiempo estimado de llegada. Botón "atrás" arriba a la izquierda.
- **Mini mapa** mostrando la ubicación de la base de la purificadora (sin trazar ruta, solo el pin del negocio).
- **Datos del negocio**:
  - Dirección de la base (texto).
  - Horario de atención (ej. "Lun–Sáb · 8:00 AM – 7:00 PM"; "Cerrado los domingos"). Indicar visualmente si está abierto ahora ("● Abierto" en verde / "● Cerrado" en gris).
  - Tiempo en la plataforma ("8 meses con HToGo") — opcional.
- **Productos disponibles**: lista de marcas que vende esta purificadora con su precio. Cada item: icono de garrafón, nombre de marca + capacidad ("Ciel 20 L"), precio ("$45 c/u"). Indicador de stock chiquito a la derecha: "Disponible" en verde, "Pocas unidades" en amarillo, "Agotado" en gris (no clickeable).
- **Vehículos del negocio**: cards horizontales con scroll lateral mostrando los vehículos que pueden hacer entregas. Cada card: foto/icono del vehículo, tipo + marca ("Camioneta Nissan"), capacidad ("hasta 25 garrafones"). Esto le da confianza al cliente sobre la capacidad del negocio.
- **Botón CTA fijo abajo**: "Pedir a esta purificadora" — al tocarlo navega a `form` con el modo `directa` preseleccionado y esta purificadora ya elegida.

NO incluir en esta pantalla:
- Calificaciones / estrellas / reviews.
- Lista de repartidores individuales (no le interesa al cliente saber cuántos empleados tiene la purificadora; le interesa saber qué le ofrecen).
- Datos de contacto directo del repartidor (teléfono, etc.) — la comunicación se da por el flujo del pedido, no antes.

Conecta con `data-htogo-nav="purif_c"` desde:
- Cada card de purificadora en `home_c` (al tocar el nombre/avatar, no el botón "Pedir").
- El nombre de la purificadora en `track` (cuando el cliente está rastreando, puede tocar para ver más info).
- El nombre de la purificadora en cada card de `hist_c`.

---

### `form` — Formulario de pedido

- Agrega un **selector de tipo de pedido al inicio** (arriba de "Marca de garrafón"), dos cards radio-button:
  - **"A una purificadora específica"** (seleccionado por default si vino desde el botón "Pedir" de una card específica) — muestra el nombre de la purificadora elegida.
  - **"Con precio máximo"** — al elegirlo aparece un input/stepper "Precio máximo por garrafón" (ej. "$60 MXN") y oculta la sección "Marca de garrafón" porque las marcas dependerán de quién acepte.
- En la sección "Marca de garrafón", el texto pequeño "Marcas disponibles con Aguas Del Valle" cámbialo a **"Marcas que vende [Nombre Purificadora]"**.
- Mantén "Indicaciones para el repartidor" y la sección de mapa con las "Notas a la dirección" (pero asegúrate de que en `perfil_c` la sección de direcciones tenga un campo permanente "Referencias / Nota fija" tipo "Casa azul, frente al parque").
- Quita todo lo relacionado a billete y cambio (ya cubierto arriba).

### `buscando` — Buscando repartidor

- El banner del mapa que dice "Buscando repartidor cerca de ti" cámbialo a:
  - Si es modo directo: **"Esperando que [Aguas Del Valle] acepte..."**
  - Si es modo abierto: **"Buscando purificadoras con tu precio máximo..."**
- En el bottom sheet, el primer item del timeline ("Pedido recibido · Aguas Del Valle") está bien si es directo, pero agrega versión para abierto: "Pedido lanzado · Buscando entre N purificadoras".

### `track` — Seguimiento del pedido

- Donde dice "Carlos viene en camino", agrega arriba del nombre el **nombre comercial de la purificadora** ("Aguas Del Valle · Carlos M.").
- En el driver-card del bottom sheet, debajo del nombre del repartidor agrega línea con `storefront` y nombre de la purificadora. El texto actual "Aguas Del Valle" ya está pero asegúrate de que el icono de storefront acompañe.
- Quita "Pagas con $200 · cambio $75".

### `hist_c` — Historial cliente

- En cada card de pedido, donde dice "Aguas Del Valle" o "Hidro Express BJ", asegúrate de que aparezca con icono `storefront` chiquito antes del nombre. El nombre del repartidor individual (ej. "Carlos M.") puede aparecer en línea aparte chica.
- Las cards "Repetir" deben llevar al `form` con el modo "directa" preseleccionado a esa misma purificadora.

### `perfil_c` — Perfil cliente

- Quita el stats de "4.9 ★ Promedio". Deja solo "14 Pedidos" como una stat única o agrega "$XXX gastado" si quieres.
- En cada `addr-card`, asegúrate de que "Ref:" esté visible — actualmente sí está pero verifica que sea consistente y que se llame "Referencias" o "Notas de la dirección".

### `home_r` — Home Repartidor

- En el bloque del avatar, debajo del nombre "Hola, Carlos" agrega el **nombre comercial del negocio** ("Purificadora Aqua Pura") con icono `storefront`. Queda claro que Carlos pertenece a ese negocio.
- En "Tu jornada de hoy", la stat "Inventario · 22 garrafones" cámbialo para que diga **"En tu vehículo · 22"** (porque ahora es el inventario del vehículo, no el total). Agrega una 5ta stat o reemplaza una para mostrar "En base · 38 garrafones" (lo que tiene la purificadora compartido).
- En "Próxima entrega", quita "Ganarás $120 MXN" — eso es un cobro, no una ganancia (el repartidor cobra el total al cliente). Cámbialo a **"Cobrarás $120 MXN"**.

### `inv` — Inventario y vehículo

Esta pantalla necesita un **rediseño grande** porque ahora hay dos niveles de inventario.

- Agrega **un tercer tab** al segmented control actual ("Inventario / Vehículo"). Que sea: **"En base" / "En mi vehículo" / "Vehículo"**.
- **Tab "En base"**: muestra el stock compartido del negocio (lo que tiene la purificadora en su almacén). Mismo diseño de cards por marca con barra de stock, pero el texto es "12 / 50 capacidad de la base". Aquí el repartidor puede registrar entradas (compras al proveedor).
- **Tab "En mi vehículo"**: muestra solo lo que el repartidor trae cargado HOY. Aquí hay un botón nuevo prominente: **"Cargar desde la base"** que abre un modal donde puede seleccionar marca + cantidad para mover de base→vehículo.
  - En cada card de marca debe aparecer "8 disponibles · 2 apartados para pedidos" (los apartados son los garrafones reservados por pedidos aceptados pero aún no entregados).
- **Tab "Vehículo"**: igual que el actual con datos del carro.
- En el resumen superior (summary card), cambia "Stock total / 22 garrafones / 3 marcas activas / Vendidos hoy / Ingresos hoy / Reservados" para reflejar el contexto del tab activo (base o vehículo).
- Modal "Registrar entrada de inventario": el subtítulo actual dice "Agrega los garrafones nuevos que ingresaron a tu vehículo." → cámbialo a **"Agrega los garrafones que entraron a la base del negocio."**
- Agrega un **modal nuevo "Cargar al vehículo"** con: select de marca (de las que hay en base), stepper de cantidad (max = lo disponible en base), y botón "Confirmar carga". Esto registra un movimiento de traspaso base→vehículo.

### `ped` — Pedidos disponibles

- Cuando un pedido es de tipo "abierto" (con precio máximo), agrega un tag visual en la card del pedido: **chip naranja "Precio máx $60"**. Cuando es directo, no hace falta nada extra.
- En la card de cliente, quita "⭐ 4.9".
- En el modal "¿Aceptar este pedido?", el check-list actual dice "Tienes 12 garrafones de Ciel en inventario". Cámbialo para que sea más útil:
  - "✓ Tienes 5 garrafones de Ciel en tu vehículo (suficiente)" *o*
  - "⚠️ Solo tienes 2 en tu vehículo. Tendrás que cargar 1 más desde la base antes de salir." (este mensaje aparece cuando el pedido excede el vehículo pero hay respaldo en base)
  - "❌ No hay stock suficiente entre tu vehículo y la base" → bloquea el botón aceptar.

### `ruta` — Ruta de entrega

- En la pantalla 1 ("En ruta"): quita "⭐ 4.9" del client-card. Quita "Cliente paga con $200 · Cambio: $65" del pay-box.
- En la pantalla 2 ("Llegaste"): quita "Cambio para $200: $65" del pay-box. Solo deja "3 × Ciel 20 L".
- En el modal "Confirmar entrega": elimina el change-row con cambio. Solo deja el pay-box principal.

### `gan_b` y `gan_a` — Ganancias

- Estas pantallas se llaman "Ganancias" pero realmente muestran **ingresos** (lo que el repartidor cobró en efectivo, no su ganancia neta). Cámbialas a **"Ingresos"** en el header, en el tab del bottom-nav y en todos los textos de label que digan "Ganancias".
- Quita TODA la sección de "Calificación 4.9 ★★★★★ / 247 calificaciones / +0.1 esta semana".
- Quita el item "Calificación: 4.9 ★" del big-card (en variant B). Reemplázalo por algo útil como "Tasa de entrega: 92%" o quítalo y deja solo Entregas + Garrafones.
- En la stat-grid de variant A, quita la stat "Calificación promedio · 4.9". Reemplázala por **"Garrafones vendidos · 28 hoy"** o "Pedidos no entregados".
- En el desglose, asegúrate de que sea claro que "Total recibido en efectivo" es **lo que cobró**, no su ganancia. Considera agregar un texto explicativo: "Este monto incluye lo que pagas a tu proveedor por cada garrafón."

### `perfil_r` — Perfil del repartidor

- Esta es la pantalla que **más cambia**. Quita todo lo relacionado a calificaciones:
  - Header del perfil: quita "★ 4.9 · 247 calificaciones".
  - Stats row: quita "98% Concretadas" o cámbialo a "1,284 Entregas / 8 mes Antigüedad". Pueden ser solo 2 stats.
  - **Sección completa de "Calificaciones recientes"**: ELIMINAR todo el bloque con los 3 reviews.

- **El concepto de "negocio" debe ser explícito**: en "Información del negocio", agrega un **subtítulo al inicio** que indique si este usuario es **Dueño** o **Empleado** del negocio:
  - Si es dueño: badge "Dueño del negocio" en el header del card.
  - Si es empleado: badge "Repartidor de [Aguas Del Valle]" — y en este caso, los campos del negocio (nombre comercial, dirección base, etc.) aparecen en gris no editables, con leyenda "Solo el dueño puede modificar estos datos".

- **Vehículos**: la sección actual está bien pero aclara con un aviso al inicio: "Los vehículos pertenecen al negocio. Cualquier repartidor del negocio puede usarlos." Y cuando el dueño edita, agrega el flujo de "Solicitar cambio" (todos los cambios pasan por aprobación del admin según RN-020).

- **Solicitudes de cambio (NUEVO)**: agrega una sección nueva visible solo para el dueño llamada **"Mis solicitudes de cambio"** con cards que muestren:
  - Tipo de cambio (con el catálogo: Foto perfil, Datos vehículo, Agregar vehículo, Eliminar vehículo, Nombre negocio, Dirección base, Precio producto, Agregar producto)
  - Estado: pendiente / aprobado / rechazado
  - Fecha de solicitud
  - Si rechazado: comentario del admin

- **Nuevo: Productos y precios del negocio**: agrega sección "Mis productos y precios" con tabla de marcas que vende este negocio + precio que cobra cada uno. Botón "Cambiar precio" que dispara una solicitud de cambio (no edita directo).

- **QR**: la sección ya está y es buena. Aclara debajo: "Los clientes que escaneen este QR podrán hacer pedidos directos a tu purificadora."

### `admin` — Panel administrativo

- En la tabla "Top repartidores": quita la columna "⭐". La métrica de ranking ahora es solo por entregas + ingresos.
- En el modal "Detalle de pedido" (`modalDetalle`): quita "★ 4.9 · 247 calificaciones" debajo del nombre del repartidor.
- En la sidebar agrega un nuevo ítem de menú: **"Solicitudes de cambio"** con badge contador (ej. "8" pendientes). Es la pantalla donde el admin aprueba/rechaza las solicitudes que vienen del flujo RN-020.
- Crea **una vista nueva `view-solicitudes`** con tabla similar a Usuarios, columnas: Repartidor solicitante / Negocio / Tipo de cambio / Estado / Fecha. Cada fila con botones "Aprobar" y "Rechazar (con motivo)".
- En la vista de Usuarios, agrega columna **"Negocio"** para repartidores (porque ahora pueden estar agrupados). Para clientes, vacía.
- En el modal "Dar de alta nuevo usuario", cuando se elija rol "Repartidor", aparece un campo extra: **"¿Crea negocio nuevo o se une a uno existente?"** con dos opciones (crear / unirse → select de negocios existentes).

---

## CAMBIOS GLOBALES (todo el flujo)

1. **Reemplazar "Repartidor" por "Purificadora" en cualquier texto cara al cliente.** En el lado del repartidor sí puede seguir diciendo "Repartidor" porque él es el repartidor; pero el cliente ve "Purificadoras", no "Repartidores".
2. **Iconografía consistente**: usa `storefront` (Material Icons) para representar al negocio/purificadora en cualquier lugar donde antes había avatar de persona o `delivery_dining`. Reserva `delivery_dining` y `two_wheeler` para representar al repartidor físico (la persona con su vehículo).
3. **Términos**: en lugar de "tu repartidor" en mensajes al cliente, usa "tu purificadora" o "[Nombre Purificadora]". Cuando hablamos del repartidor físico, sí "Carlos viene en camino" sigue siendo correcto.

---

## QUÉ MANTENER SIN TOCAR

- La paleta de colores, tipografía Roboto, tamaño 412×868 del frame Android.
- El sistema de navegación con `data-htogo-nav`.
- Las animaciones de pulso, dots, transiciones.
- El estilo del panel admin con Tailwind y la sidebar oscura.
- Las pantallas `splash`, `registro`, `otp`, `login` no requieren cambios.

Cuando termines, devuélveme el HTML completo actualizado en un solo archivo (mismo formato que el original con `SCREENS_HTML`). Si tienes dudas sobre alguna decisión, pregúntame antes de inventar.
