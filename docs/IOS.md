# H2ToGo en iOS (Kotlin Multiplatform)

La app se comparte entre Android e iOS con **Kotlin Multiplatform + Compose Multiplatform**.
El backend no cambia.

## Estructura

```
h2togo-mobile/
├── app/        App Android (por ahora casi todo sigue aquí)
├── shared/     Código común Android/iOS
│   ├── commonMain/   tema, componentes, login (pantalla, ViewModel, Ktor, sesión)
│   ├── androidMain/  piezas solo de Android
│   ├── iosMain/      piezas solo de iOS (MainViewController)
│   └── commonTest/   pruebas del código común
└── iosApp/     App iOS (SwiftUI que muestra la UI de Compose)
    └── project.yml   proyecto de Xcode (se genera con XcodeGen)
```

## Correr en el simulador de iPhone

1. Instala **Xcode** (App Store) y ábrelo una vez para aceptar la licencia y descargar el simulador de iOS.
2. Selecciona Xcode como herramienta de línea de comandos:
   `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`
3. Instala XcodeGen y genera el proyecto:
   ```bash
   brew install xcodegen
   cd h2togo-mobile/iosApp && xcodegen
   ```
4. Abre `h2togo-mobile/iosApp/iosApp.xcodeproj`, elige un simulador (p. ej. iPhone 16) y dale ▶︎.
   La fase "Compilar Kotlin (Shared)" corre Gradle y genera el framework `Shared`;
   la primera vez tarda varios minutos.

Por defecto usa el backend de Azure. Para el backend local (Docker en el puerto 8088):

- En Xcode: *Product > Scheme > Edit Scheme > Run > Arguments > Environment Variables*,
  agrega `H2TOGO_API` = `http://localhost:8088/api/v1/`.
- O desde terminal, con la app ya instalada en el simulador:
  `SIMCTL_CHILD_H2TOGO_API=http://localhost:8088/api/v1/ xcrun simctl launch booted com.htogo.app`

Por línea de comandos (sin abrir Xcode):

```bash
cd h2togo-mobile/iosApp
xcodebuild -project iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath build/dd build
xcrun simctl install booted build/dd/Build/Products/Debug-iphonesimulator/H2ToGo.app
```

## Comandos útiles

```bash
cd h2togo-mobile
./gradlew :shared:testDebugUnitTest              # pruebas del código común
./gradlew :shared:compileKotlinIosSimulatorArm64 # verifica que compila para iOS (no requiere Xcode)
./gradlew :app:assembleDebug                     # app Android
```

Gradle 8.9 necesita un JDK 17–21 (`JAVA_HOME`); con JDK 25 falla.

## Estado de la migración

| Fase | Estado |
|---|---|
| 1. Prueba inicial: módulo `shared`, login compartido, app iOS | ✅ Probado en simulador (iPhone 17, iOS 27) contra el backend local |
| 2. Capa de datos compartida: APIs con Ktor, DTOs con kotlinx.serialization, sesión con multiplatform-settings, WebSocket STOMP con Ktor | ✅ Android e iOS la usan |
| 3. ViewModels, navegación y las 25 pantallas a `commonMain` | ✅ La app completa corre en iOS (sin mapas, GPS ni notificaciones) |
| 4. Mapa (WKWebView con el mismo HTML de Leaflet), GPS (CoreLocation), notificaciones (APNs), enlaces | Pendiente |
| 5. iPhone real y TestFlight (requiere Apple Developer Program) | Pendiente |

Desde la fase 3 la app (ViewModels, navegación, 25 pantallas, componentes y la raíz `HToGoRaiz`)
vive en `shared/commonMain`, con los mismos paquetes que tenía en Android. Lo que depende del
sistema operativo se pide a través de dos contratos en `com.htogo.app.plataforma`:

- `UiPlataforma` (por `LocalUiPlataforma`): mapas, conectividad y permiso de ubicación.
  Android: `UiPlataformaAndroid` (Leaflet en WebView). iOS: `UiPlataformaIos` (por ahora sin mapas).
- `ServiciosPlataforma` (por `Plataforma.servicios`): geocodificador, GPS de la entrega, ajustes
  y apps de navegación. Android: `ServiciosAndroid`. iOS: `ServiciosIos` (vacío hasta la fase 4).

Utilidades comunes: `mostrarAviso` (Toast / aviso flotante en iOS), `String.format` propio
(`com.htogo.app.util.format`), fechas en hora de la CDMX con kotlinx-datetime (`util/Fechas.kt`) y
selectores de fecha y hora de Material 3 (`SelectorFecha`, `SelectorHora`).

Siguen solo en Android (`app/`): `MainActivity`, `HToGoApp`, notificaciones (FCM), el servicio de
ubicación en primer plano, el geocodificador y los mapas en WebView.

### Prueba de contrato con el backend

```bash
H2TOGO_API=http://localhost:8088/api/v1/ ./gradlew :shared:testDebugUnitTest
```

Inicia sesión con los usuarios de `data-demo.sql` (cierra su sesión abierta) y verifica que cada
endpoint de lectura y el WebSocket se decodifiquen con los DTOs compartidos. Sin `H2TOGO_API` se omite.
