# HToGo · Android (Jetpack Compose)

Aplicación móvil de delivery de garrafones para Benito Juárez, CDMX. Tres roles: cliente, repartidor (purificadora) y admin (este último vive en el panel web `15-admin-panel.html`).

## Estructura

```
android/
├── settings.gradle.kts
├── build.gradle.kts                 # configuración top-level
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/htogo/app/
        │   ├── HToGoApp.kt           # Application
        │   ├── MainActivity.kt       # entry point
        │   ├── navigation/
        │   │   ├── HToGoRoutes.kt    # catálogo de rutas
        │   │   └── HToGoNavHost.kt   # NavHost que conecta todas las pantallas
        │   └── ui/
        │       ├── theme/HToGoTheme.kt
        │       ├── components/       # botones, textfield, chips
        │       └── screens/          # 1 archivo por pantalla
        └── res/                      # strings, themes, icon
```

## Cómo abrirlo y ejecutarlo en Android Studio

1. **Abrir el proyecto**
   - Android Studio → *File* → *Open…* → selecciona `HtoGo/android/`.
   - Acepta cuando te pregunte por confiar en el proyecto.

2. **Generar el Gradle wrapper**
   - Este repo no incluye el binario `gradle-wrapper.jar`. Tras abrir, Android Studio te ofrecerá generarlo.
   - Si no lo hace automáticamente, abre la terminal integrada y ejecuta:
     ```
     gradle wrapper --gradle-version 8.9
     ```
     Necesitas tener Gradle 8.9+ instalado a mano (`brew install gradle` / `scoop install gradle`).
     Después de eso ya puedes usar `./gradlew` (mac/linux) o `gradlew.bat` (Windows).

3. **Sincronizar dependencias**
   - Android Studio descargará Compose BOM 2024.10.01 + Navigation 2.8.4. Espera al *Sync*.

4. **Ejecutar**
   - Selecciona un emulador o dispositivo Android (API 24+).
   - *Run* → *app*.

## Navegación

`HToGoNavHost` arranca en `HToGoRoutes.SPLASH` y conecta las 17 pantallas. Para saltar entre rol cliente y rol repartidor sin cerrar sesión, hay un acceso rápido en el header de `HomeClienteScreen` y `HomeRepartidorScreen` (chip "Vista Repartidor" / "Vista Cliente"). Es solo para el mockup — en producción ese cambio dependería del rol del usuario autenticado.

Mapa de rutas (id corto = `data-htogo-nav` del HTML original):

| Ruta            | Pantalla                       | HTML correspondiente            |
|-----------------|--------------------------------|---------------------------------|
| `splash`        | SplashOnboardingScreen         | 01-splash-onboarding.html       |
| `registro`      | RegistroScreen                 | 02-registro.html                |
| `otp`           | VerificacionTelefonoScreen     | 03-verificacion-telefono.html   |
| `login`         | LoginScreen                    | 04-login.html                   |
| `home_c`        | HomeClienteScreen              | 05-home-cliente.html            |
| `home_r`        | HomeRepartidorScreen           | 05b-home-repartidor.html        |
| `purif_c`       | PerfilPurificadoraScreen       | 05c-perfil-purificadora.html    |
| `form`          | NuevoPedidoScreen              | 06-formulario-pedido.html       |
| `track`         | SeguimientoPedidoScreen        | 07-seguimiento-pedido.html      |
| `buscando`      | AsignandoRepartidorScreen      | 07a-asignando-repartidor.html   |
| `hist_c`        | HistorialPedidosScreen         | 08-historial-pedidos.html       |
| `perfil_c`      | PerfilClienteScreen            | 09-perfil.html                  |
| `inv`           | InventarioVehiculoScreen       | 10-inventario-vehiculo.html     |
| `ped`           | PedidosDisponiblesScreen       | 11-pedidos-disponibles.html     |
| `ruta`          | RutaEntregaScreen              | 12-ruta-entrega.html            |
| `ingresos`      | IngresosScreen                 | 13-ganancias.html               |
| `perfil_r`      | PerfilRepartidorScreen         | 14-perfil-repartidor.html       |

> El admin panel (`15-admin-panel.html`) es desktop-only y queda fuera del scope mobile.

## Paleta

Definida en `HToGoColors`:

- `Primary` `#0077B6`
- `PrimaryLight` `#00B4D8`
- `PrimaryDark` `#03045E`
- `PrimarySoft` `#CAF0F8`
- Estados: `StatusPendiente`, `StatusAsignado`, `StatusEnCamino`, `StatusEntregado`, `StatusCancelado`.
