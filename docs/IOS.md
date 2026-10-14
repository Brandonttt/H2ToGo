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

Para probar contra el backend local (Docker en el puerto 8088), cambia la URL en
`iosApp/iosApp/ContentView.swift` por `http://localhost:8088/api/v1/`.

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
| 1. Prueba inicial: módulo `shared`, login compartido, app iOS | ✅ (falta correrla en simulador al tener Xcode) |
| 2. Capa de datos a Ktor / kotlinx.serialization / multiplatform-settings / kotlinx-datetime | Pendiente |
| 3. ViewModels, navegación y las 25 pantallas a `commonMain` | Pendiente |
| 4. Mapa (WKWebView con el mismo HTML de Leaflet), GPS (CoreLocation), notificaciones (APNs), enlaces | Pendiente |
| 5. iPhone real y TestFlight (requiere Apple Developer Program) | Pendiente |

En la fase 1, Android usa la **pantalla** de login compartida, pero la petición sigue pasando por su
`AuthViewModel` (Retrofit). iOS ya usa la lógica compartida (`LoginViewModel` + Ktor). En la fase 2
Android pasará también a la lógica compartida.
