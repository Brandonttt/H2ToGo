# Configurar Firebase (notificaciones con la app cerrada)

**Para:** quien tenga acceso a la cuenta de Google del equipo y a Azure.
**Tiempo:** unos 20 minutos. **Costo:** $0 (plan Spark de Firebase).

## Contexto

La app ya recibe notificaciones (pedido asignado, en camino, cerca, entregado, nuevo pedido
disponible, solicitud resuelta…) **mientras está abierta**, por WebSocket. Para que lleguen
también **con la app cerrada** Android exige Firebase Cloud Messaging (FCM). El código ya está
listo en la app y en el backend; solo faltan dos archivos de credenciales que se generan en la
consola de Firebase con una cuenta de Google:

| Archivo | Para qué | ¿Secreto? |
|---|---|---|
| `google-services.json` | Identifica la app ante Firebase | No es crítico, pero no se sube al repo |
| Clave de cuenta de servicio (`.json`) | Permite al backend enviar notificaciones | **Sí**: quien la tenga puede mandar push a los usuarios |

Ambos ya están en `.gitignore`. **No los subas al repositorio ni los compartas por el chat del equipo.**

---

## 1. Crear el proyecto

1. Entra a <https://console.firebase.google.com> con la cuenta del equipo.
2. **Crear un proyecto** → nombre `H2ToGo` → puedes desactivar Google Analytics (no se usa).
3. Espera a que termine y entra al proyecto.

## 2. Registrar la app Android → `google-services.json`

1. En la página del proyecto, botón **Android** (o *Configuración del proyecto → Tus apps → Agregar app*).
2. **Nombre del paquete:** `com.htogo.app` (exactamente así; si no coincide, la app no recibe nada).
3. Apodo: `H2ToGo`. El SHA-1 **no** hace falta para notificaciones.
4. **Descarga `google-services.json`** y cópialo en:
   ```
   h2togo-mobile/app/google-services.json
   ```
5. Omite los pasos del asistente sobre agregar el SDK: ya está agregado en el proyecto.
6. En Android Studio: **Sync Project with Gradle Files** y genera la APK de nuevo. La app activa
   Firebase sola cuando encuentra ese archivo (sin él compila igual, solo sin notificaciones con la
   app cerrada).

## 3. Clave del backend → cuenta de servicio

1. *Configuración del proyecto* (engrane) → pestaña **Cuentas de servicio**.
2. **Generar nueva clave privada** → se descarga un `.json` (por ejemplo
   `h2togo-firebase-adminsdk-xxxx.json`).
3. Guárdalo fuera del repositorio. Si alguien más va a desplegar, pásaselo por un canal privado
   (no por el grupo).

## 4. Cargar la clave en Azure

Requiere Azure CLI con sesión iniciada (`az login`) y PowerShell (`pwsh` en Mac). Desde la raíz del repo:

```powershell
./desplegar-rabbitmq.ps1 -Password "<contraseña de RabbitMQ>" -FcmCredenciales "C:\ruta\h2togo-firebase-adminsdk-xxxx.json"
```

- La contraseña de RabbitMQ la imprimió ese mismo script la primera vez que se ejecutó (pídesela
  a quien lo corrió). Si la cambias aquí, el backend dejará de conectarse a RabbitMQ.
- El script guarda la clave como secreto de la Container App (`FCM_CREDENCIALES_JSON`, en base64)
  y crea una revisión nueva del backend.
- **Borra el `.json` de tu equipo** después de este paso.

## 5. Verificar

1. Logs del backend:
   ```bash
   az containerapp logs show -n h2togo-api -g rg-h2togo --tail 80
   ```
   Debe aparecer `FCM habilitado para el proyecto h2togo-...`.
   Si dice `FCM deshabilitado`, la variable no llegó: repite el paso 4.
2. Prueba real con dos teléfonos (o emuladores con Google Play):
   - Instala la APK nueva en ambos e inicia sesión (cliente y repartidor). Acepta el permiso de
     notificaciones. Al iniciar sesión la app registra su dispositivo en el backend.
   - **Cierra la app del cliente** (deslízala fuera de recientes).
   - Con el repartidor, acepta el pedido del cliente.
   - Al cliente le debe llegar "Pedido asignado". Al tocarla, abre el seguimiento de ese pedido.

## Problemas comunes

| Síntoma | Causa probable |
|---|---|
| No llega nada con la app cerrada, pero sí abierta | Falta `google-services.json` en la APK instalada, o el paquete no es `com.htogo.app` |
| Logs: `FCM deshabilitado` | No se cargó la clave (paso 4) o se cargó en otra Container App |
| Logs: `Token FCM inválido del usuario N; se elimina` | La app se desinstaló o el token caducó; se corrige solo al volver a iniciar sesión |
| No aparece la notificación en Android 13+ | Se negó el permiso de notificaciones: *Ajustes → Apps → H2ToGo → Notificaciones* |
| Teléfonos Xiaomi/Huawei no reciben con la app cerrada | El ahorro de batería del fabricante mata la app; desactivarlo para H2ToGo |
