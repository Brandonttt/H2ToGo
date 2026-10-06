# INFRA_AZURE — Despliegue de H2ToGo en Microsoft Azure

**Proyecto:** Trabajo Terminal 2026-B176 · H2ToGo
**Fecha:** 2026-09-29
**Objetivo:** dejar el backend (`h2togo-backend/`) y su base de datos corriendo en Azure con una URL
HTTPS pública, para que la app móvil consuma los endpoints y el OTP llegue por SMS.
**Base:** diagrama "HToGo – Arquitectura en Microsoft Azure" del TT. Único cambio respecto al
diagrama: **MariaDB → PostgreSQL 16 + PostGIS 3.4** (esquema v6).

> **Cómo usar este documento.** Sigue los pasos en orden; cada uno termina con una verificación.
> Todo lo marcado con **⚠️ PENDIENTE D-x** depende de una decisión de la sección "Decisiones pendientes" y **no se ejecuta**
> hasta que esté resuelta. Los comandos son para bash/zsh (macOS o Linux; en Windows usa WSL o Git Bash)
> y se corren **desde la raíz del repositorio** `H2ToGo/`.

---

## Resumen — Qué se despliega y cómo se mapea al diagrama

| Bloque del diagrama | Recurso en Azure | Nombre propuesto | Estado en este documento |
|---|---|---|---|
| Spring Boot (API REST, A*, STOMP, jobs) | Azure Container App (HTTP externo, puerto 8080) | `h2togo-api` | Pasos 1–10 |
| Base de datos (antes MariaDB) | Azure Container App con `postgis/postgis:16-3.4` (TCP **interno**, 5432) | `h2togo-db` | Pasos 5–8 |
| Azure File Share (volumen persistente) | Storage Account Standard LRS + file share SMB 10 GiB (HDD, como en el TT) | `pgdata` | Paso 6 |
| Azure Container Registry | ACR **Basic** | `acrh2togo<sufijo>` | Paso 3 |
| "docker-compose" dentro de Container Apps | Un **Container Apps Environment** con las dos apps; se ven por nombre en la red interna | `cae-h2togo` | Paso 5 |
| Azure Monitor | Log Analytics workspace del environment (logs de consola) con tope diario | `law-h2togo` | Paso 5 |
| Application Insights | Agente Java de App Insights | — | Paso 14 · ⚠️ PENDIENTE D-9 |
| Azure Pipelines (build + deploy) | Pipeline YAML en Azure DevOps conectado al repo de GitHub | `azure-pipelines.yml` | Paso 12 · ⚠️ PENDIENTE D-7 |
| Azure Static Web Apps (Panel Admin) | **No requiere recurso**: el backend sirve el panel en `/admin/` (mismo origen, sin CORS) | `h2togo-api` | Paso 13 |
| Cola de mensajes (notificaciones) | **RabbitMQ** como Container App interna (`rabbitmq:3.13-management`, 0.25 vCPU / 0.5 GiB). Azure Service Bus no sirve: habla AMQP 1.0, no el protocolo de RabbitMQ | `h2togo-rabbitmq` | Paso 15 |
| OpenStreetMap | **No requiere recurso**: el grafo va dentro del jar (`osm_data/sample_map.json`) | — | — |
| Google Maps | **No requiere recurso en Azure** (lo consume la app móvil) | — | — |

**Por qué no hay "docker-compose" literal:** Container Apps no ejecuta archivos compose. El equivalente
es un *environment* (red privada compartida) con una app por servicio. El backend llega a la BD con
`jdbc:postgresql://h2togo-db:5432/h2togo_v6`, igual que en el `docker-compose.yml` local llega a `db:5432`.

**Por qué la BD es una app aparte y no un sidecar del backend:** cada despliegue del backend crea una
revisión nueva que arranca *antes* de apagar la anterior. Si PostgreSQL viviera en la misma app,
durante unos segundos habría dos PostgreSQL escribiendo sobre el mismo directorio de datos (corrupción).
Separada, la BD no se toca cuando se redepliega el backend.

**Qué NO se crea (ahorro):** VNet, Key Vault, Front Door, dominio propio, base de datos administrada,
réplicas extra. Nada de eso aparece en la arquitectura del TT.

---

## Decisiones pendientes (resolver antes de los pasos marcados)

Ordenadas de más a menos importante y agrupadas por tema.

### Código y perfil `prod`

| # | Tema | Situación | Qué se necesita |
|---|---|---|---|
| **D-1** | **El backend no arranca con `SPRING_PROFILES_ACTIVE=prod`** | `TwilioSmsService` solo existe en `prod` (bien), pero `DevPushService` es `@Profile("!prod")` y no hay otra implementación de `PushService`. En `prod` Spring no encuentra el bean y el contexto falla al arrancar (`EntregaService`, `AsignacionService`, `ProximidadService`, `SolicitudService` y dos jobs lo inyectan). | Aprobar el cambio del **Paso 0** (una línea). Sin esto no hay OTP por SMS en Azure. |
| **D-2** | **Proveedor y cuenta del OTP** | El código usa **Twilio** (`TwilioSmsService`, commit `v1.1 integración Twilio OTP`). La tabla de costos del TT pone "OTP $0" y la lámina 58 muestra precios de **Firebase**. Además, las cuentas **trial** de Twilio solo envían a números verificados (máx. 5), solo al país con el que se registró la cuenta, y según la documentación actual de Twilio **no permiten cuerpos de mensaje personalizados** (solo plantillas). El backend manda texto personalizado. | Confirmar: (a) Twilio es el proveedor final (y el TT se alinea a eso); (b) si la cuenta es trial o de pago; (c) qué números de prueba están verificados; (d) el `TWILIO_FROM_NUMBER`. ¿Ya les llegó algún SMS real con esta integración? |
| D-2b | Texto del SMS | El mensaje dice "Válido por 5 minutos" pero `h2togo.auth.otp-vigencia-minutos: 10`. | Decidir cuál vale y alinear (cambio de texto o de propiedad). |

### Infraestructura y costo

| # | Tema | Situación | Qué se necesita |
|---|---|---|---|
| **D-3** | **PostgreSQL sobre Azure Files (SMB)** | El TT planeó MariaDB en contenedor + Azure Files HDD. Con PostgreSQL hay un problema conocido: SMB no permite cambiar permisos después de montar, y `initdb` falla con `could not change permissions of directory`. Este documento aplica el arreglo que la comunidad reporta como funcional en Container Apps (`mountOptions: dir_mode=0750,file_mode=0750,uid=999,gid=999`), pero **no es una configuración que Microsoft soporte oficialmente** para bases de datos. | Aceptar el riesgo y probar (Plan A, el del TT). Si falla, el Plan B sería **Azure Database for PostgreSQL Flexible Server** (sí soporta PostGIS), pero **cambia la arquitectura del TT** — decisión de ustedes, no se documenta aquí hasta que la tomen. |
| **D-4** | **¿El backend escala a cero?** | El TT dice "Backend 1 vCPU, 2 GiB, escala a cero". Pero el backend tiene: `PedidosProgramadosJob` cada minuto (RF-025), jobs de las 03:00/03:30, broker STOMP en memoria, flag de proximidad en memoria y carga del grafo OSM al arrancar. Con cero réplicas **los jobs no corren** y la primera petición tras inactividad espera el arranque completo. | Recomendación: `min=1, max=1`. Costo extra aprox. **+$10 USD/mes** (ver "Costo estimado"). El comando del Paso 9 usa la variable `API_MIN_REPLICAS` para cambiarlo en un solo lugar. |
| D-5 | Tamaño de los contenedores | TT: backend 1 vCPU/2 GiB y BD 0.5 vCPU/1 GiB. En desarrollo el backend corre con `-Xmx512m`. | Recomendación para ahorrar: **backend 0.5 vCPU/1 GiB** y **BD 0.25 vCPU/0.5 GiB**; se sube con un comando si hay `OOMKilled`. Confirmar. |

### Datos

| # | Tema | Situación | Qué se necesita |
|---|---|---|---|
| D-6 | ¿Cargar `data-demo.sql` en Azure? | Deja usuarios listos (incluido `admin@h2togo.mx`) con la contraseña `Demo1234`, que está escrita en el repo y en `GUIA_DEMO.md`. En una URL pública cualquiera podría entrar como ADMIN. | Decidir: (a) cargarlo y cambiar la contraseña del admin después, (b) cargarlo tal cual solo durante pruebas, o (c) no cargarlo y dar de alta usuarios por la API. El Paso 4 tiene la línea comentada. |

### Cuenta de Azure y CI/CD

| # | Tema | Situación | Qué se necesita |
|---|---|---|---|
| D-7 | Cuenta, región y Azure DevOps | Las suscripciones **Azure for Students** tienen una política "Allowed resource deployment regions" (≈5 regiones, distintas por alumno). Crear la *service connection* de Azure Pipelines requiere permiso para registrar aplicaciones en el tenant (con cuentas institucionales suele estar bloqueado). Las organizaciones nuevas de Azure DevOps no traen agentes hospedados gratis: hay que pedirlos por formulario (tarda días) o usar un agente propio. | Confirmar: ¿qué suscripción se usa (Azure for Students con correo del IPN o personal)? ¿cuánto crédito queda? ¿regiones permitidas (Paso 2)? ¿ya existe organización de Azure DevOps? |

### Componentes restantes

| # | Tema | Situación | Qué se necesita |
|---|---|---|---|
| D-8 | Panel admin | ✅ Resuelto: el panel vive en `h2togo-backend/src/main/resources/static/admin/` y el backend lo sirve en `/admin/`. Viaja en la misma imagen, así que se publica con cada despliegue del backend. | Nada. Static Web Apps queda descartado (costaría un recurso más y obligaría a configurar CORS). |
| D-9 | Application Insights | Está en el diagrama, pero requiere agregar el agente Java al `Dockerfile` y consume memoria extra (~100 MB), lo que puede obligar a subir el backend a 0.75 vCPU/1.5 GiB. | Decidir si se agrega ahora o después de que lo básico funcione (recomendado: después). |

> **Fuera de alcance (informativo, no bloquea el despliegue):** `PushService` real con FCM no está
> implementado (las notificaciones push se escriben en el log); la descarga diaria del grafo OSM a las
> 03:00 que describe el TT no está implementada (el grafo es un archivo fijo dentro del jar); y la app
> móvil usa Leaflet con teselas de OpenStreetMap, no Google Maps como indica el diagrama.

---

## Costo estimado (consumo mensual)

Precios de referencia de Container Apps (consumo, East US): vCPU activa $0.000024/s, vCPU inactiva
$0.000003/s, memoria $0.000003/GiB·s. Concesión gratuita mensual por suscripción: 180 000 vCPU·s,
360 000 GiB·s y 2 M de peticiones. Una réplica mínima sin tráfico se cobra a tarifa **inactiva**.
Verifiquen el precio de su región en la [calculadora de Azure](https://azure.microsoft.com/pricing/calculator/).

| Escenario | Container Apps (inactivo, ya con concesión) | ACR Basic | Azure Files 10 GiB HDD | Log Analytics (≤5 GB) | **Total aprox.** |
|---|---|---|---|---|---|
| **Recomendado** (API 0.5/1 GiB min 1 + BD 0.25/0.5 GiB) | ~$16 | ~$5 | ~$1* | $0 | **≈ $22 USD/mes** |
| TT literal (API 1/2 GiB escala a cero + BD 0.5/1 GiB) | ~$10 (solo BD) | ~$5 | ~$1* | $0 | ≈ $17 USD/mes (pero sin jobs, ver D-4) |
| TT tamaños con API min 1 (API 1/2 GiB + BD 0.5/1 GiB) | ~$33 | ~$5 | ~$1* | $0 | ≈ $40 USD/mes |

\* Azure Files estándar cobra también por transacciones; PostgreSQL escribe continuamente (WAL,
checkpoints), así que puede quedar algo arriba de los $0.63 del TT.

Con $100 USD de crédito, el escenario recomendado alcanza ~4 meses: cubre de octubre hasta las
presentaciones de TT-II (17 nov – 1 dic). Las horas de pruebas activas suman centavos
(0.5 vCPU activa ≈ $0.04/hora).

**Protección de crédito:** en Azure for Students, cuando se acaba el crédito la suscripción se
deshabilita y los recursos se detienen (no se cobra a tarjeta). Aun así, creen una alerta de
presupuesto en *Cost Management → Budgets* (por ejemplo $30/mes con aviso al 80 %).

---

## Prerrequisitos

En la máquina de quien despliega:

- **Azure CLI** ≥ 2.60 (`az version`) y la extensión de Container Apps:
  ```bash
  az extension add --name containerapp --upgrade
  ```
- **Docker Desktop** con `buildx` (ya lo usan para `docker compose`).
- Acceso de escritura al repo `Brandonttt/H2ToGo` (para el pipeline).
- `openssl` (para generar la contraseña de la BD; viene en macOS/Linux).

Iniciar sesión y registrar los proveedores que se usan (solo la primera vez por suscripción):

```bash
az login
az account show --query "{suscripcion:name, id:id}" -o table   # confirmar la suscripción correcta
for p in Microsoft.App Microsoft.OperationalInsights Microsoft.ContainerRegistry Microsoft.Storage; do
  az provider register --namespace $p
done
```

---

## Paso 0 — Cambio de código bloqueante (⚠️ D-1)

Sin este cambio el backend no arranca con el perfil `prod`, y sin perfil `prod` el OTP no sale por
Twilio (se queda en el log como `[SMS-DEV]`).

**Cambio mínimo propuesto** — `h2togo-backend/src/main/java/com/h2togo/backend/notificaciones/DevPushService.java`:

```diff
 @Service
-@Profile("!prod")
 public class DevPushService implements PushService {
```

(y quitar el `import org.springframework.context.annotation.Profile;` que queda sin uso).

Efecto: en todos los perfiles las notificaciones push se registran en el log (`[PUSH-DEV]`), que es
exactamente lo que pasa hoy en `dev`. Cuando exista la implementación real con FCM, se le pone
`@Profile("prod")` a esa clase y se regresa el `@Profile("!prod")` a esta.

Verificación local antes de subir nada:

```bash
cd h2togo-backend
docker compose up -d db
SPRING_PROFILES_ACTIVE=prod TWILIO_ACCOUNT_SID=ACxxxx TWILIO_AUTH_TOKEN=xxxx TWILIO_FROM_NUMBER=+1xxxx \
  ./mvnw spring-boot:run     # o: mvn spring-boot:run
# Debe arrancar y mostrar "[TWILIO] Cliente de Twilio inicializado correctamente"
cd ..
```

Hacer commit y push de este cambio antes del Paso 4.

---

## Paso 1 — Variables del despliegue

Guarda esto en `infra/azure.env` (el patrón `*.env` ya está en `.gitignore`, **no se versiona**) y
cárgalo con `source infra/azure.env` en cada terminal nueva.

```bash
# ---- Ubicación y nombres -------------------------------------------------
export LOCATION="<REGION>"          # ⚠️ PENDIENTE D-7: una región permitida (Paso 2)
export RG="rg-h2togo"
export SUFIJO="<3-5 letras/números únicos>"   # ACR y Storage exigen nombres globalmente únicos
export ACR="acrh2togo${SUFIJO}"               # solo minúsculas y números
export STG="sth2togo${SUFIJO}"                # solo minúsculas y números, máx. 24
export LAW="law-h2togo"
export ENV_NAME="cae-h2togo"
export DB_APP="h2togo-db"
export API_APP="h2togo-api"

# ---- Tamaños y escalado (⚠️ D-4 / D-5) -------------------------------------
export API_CPU="0.5";  export API_MEM="1.0Gi"
export DB_CPU="0.25";  export DB_MEM="0.5Gi"
export API_MIN_REPLICAS="1"         # ⚠️ PENDIENTE D-4 (TT dice 0)

# ---- Base de datos ---------------------------------------------------------
export DB_NAME="h2togo_v6"
export DB_USER="h2togo"
export DB_PASSWORD="<generar: openssl rand -base64 24 | tr -d '/+='>"

# ---- OTP / Twilio (⚠️ D-2) -------------------------------------------------
export TWILIO_ACCOUNT_SID="AC..."
export TWILIO_AUTH_TOKEN="..."
export TWILIO_FROM_NUMBER="+1..."   # número de Twilio en formato E.164

# ---- CORS (⚠️ D-8) ---------------------------------------------------------
# La app Android NO usa CORS; solo aplica a un panel web en otro dominio.
export CORS_ORIGINS="http://localhost:3000,http://localhost:5173"
```

---

## Paso 2 — Región permitida y grupo de recursos

1. Ver las regiones que permite la suscripción de estudiante:
   ```bash
   SUB_ID=$(az account show --query id -o tsv)
   az policy assignment list --scope /subscriptions/$SUB_ID \
     --query "[?displayName=='Allowed resource deployment regions'].parameters" -o json
   ```
   Si no aparece ninguna asignación, no hay restricción. En el portal: *Policy → Assignments →
   "Allowed resource deployment regions"*.
2. Ver en cuáles de esas está Container Apps:
   ```bash
   az provider show -n Microsoft.App \
     --query "resourceTypes[?resourceType=='managedEnvironments'].locations | [0]" -o tsv
   ```
3. Elegir la más cercana a CDMX que aparezca en **ambas** listas, escribirla en `LOCATION`
   (formato corto, p. ej. `southcentralus`) y crear el grupo:
   ```bash
   source infra/azure.env
   az group create -n $RG -l $LOCATION
   ```

**Verificación:** `az group show -n $RG -o table` muestra el grupo. Si algún comando posterior falla
con `RequestDisallowedByAzure`, la región no está permitida.

---

## Paso 3 — Azure Container Registry (Basic)

```bash
az acr create -g $RG -n $ACR -l $LOCATION --sku Basic --admin-enabled true
az acr login -n $ACR
export ACR_PASSWORD=$(az acr credential show -n $ACR --query "passwords[0].value" -o tsv)
```

Se usa el **usuario admin** del registro para que Container Apps y el pipeline puedan descargar
imágenes sin crear identidades en Entra ID (evita el problema de permisos de D-7).

**Verificación:** `az acr show -n $ACR --query loginServer -o tsv` → `acrh2togo<sufijo>.azurecr.io`.

---

## Paso 4 — Construir y subir las dos imágenes

> **Importante en Mac con chip Apple (M1/M2/M3):** Container Apps solo ejecuta `linux/amd64`. Siempre
> construir con `--platform linux/amd64`; si no, el contenedor falla con `exec format error`.

### 4.1 Imagen de la BD (PostgreSQL + PostGIS + esquema v6)

Es la flecha "pull" del ACR hacia la BD en el diagrama: la imagen oficial `postgis/postgis:16-3.4`
(la misma del `docker-compose.yml`) con los scripts de inicialización adentro. PostgreSQL los ejecuta
**solo la primera vez**, cuando el directorio de datos está vacío.

Crear `infra/db/Dockerfile` (este sí se versiona):

```dockerfile
# BD de H2ToGo para Azure: misma imagen que docker-compose.yml + scripts de inicialización.
FROM postgis/postgis:16-3.4

# Subdirectorio dentro del volumen de Azure Files (initdb exige un directorio propio).
ENV PGDATA=/var/lib/postgresql/data/pgdata

COPY H2ToGo_v6_postgresql.sql /docker-entrypoint-initdb.d/01_schema.sql
COPY zona_benito_juarez.sql   /docker-entrypoint-initdb.d/02_zona.sql
# ⚠️ PENDIENTE D-6 — descomentar solo si se decide cargar los datos demo:
# COPY data-demo.sql          /docker-entrypoint-initdb.d/03_demo.sql
```

Construir en una carpeta temporal para no mandar todo el repo (docx, mockups, .git) como contexto:

```bash
BUILD_DB=$(mktemp -d)
cp infra/db/Dockerfile H2ToGo_v6_postgresql.sql h2togo-backend/sql/zona_benito_juarez.sql "$BUILD_DB"/
# ⚠️ D-6: cp data-demo.sql "$BUILD_DB"/
docker buildx build --platform linux/amd64 -t $ACR.azurecr.io/h2togo-db:v1 --push "$BUILD_DB"
```

### 4.2 Imagen del backend

Usa el `h2togo-backend/Dockerfile` existente (multi-etapa: Maven + JDK 21 → JRE 21). No requiere cambios.

```bash
docker buildx build --platform linux/amd64 -t $ACR.azurecr.io/h2togo-backend:v1 --push h2togo-backend
```

**Verificación:**
```bash
az acr repository list -n $ACR -o table          # h2togo-db y h2togo-backend
```

---

## Paso 5 — Azure Monitor (Log Analytics) y Container Apps Environment

```bash
az monitor log-analytics workspace create -g $RG -n $LAW -l $LOCATION
# Tope diario de ingesta: 0.15 GB/día ≈ 4.5 GB/mes, dentro de los 5 GB gratuitos.
az monitor log-analytics workspace update -g $RG -n $LAW --quota 0.15

LAW_ID=$(az monitor log-analytics workspace show -g $RG -n $LAW --query customerId -o tsv)
LAW_KEY=$(az monitor log-analytics workspace get-shared-keys -g $RG -n $LAW --query primarySharedKey -o tsv)

az containerapp env create -n $ENV_NAME -g $RG -l $LOCATION \
  --logs-workspace-id $LAW_ID --logs-workspace-key $LAW_KEY
```

**Verificación:** `az containerapp env show -n $ENV_NAME -g $RG --query properties.provisioningState -o tsv` → `Succeeded`.

---

## Paso 6 — Azure Files (volumen persistente de la BD)

Cuenta **StorageV2 Standard LRS** (HDD, como en el TT) con un file share SMB de 10 GiB.
Container Apps solo acepta file shares "clásicos" de una cuenta de almacenamiento, no el recurso
nuevo `Microsoft.FileShares`.

```bash
az storage account create -n $STG -g $RG -l $LOCATION --sku Standard_LRS --kind StorageV2
az storage share-rm create -g $RG --storage-account $STG -n pgdata --quota 10 --access-tier TransactionOptimized

STG_KEY=$(az storage account keys list -g $RG -n $STG --query "[0].value" -o tsv)

# Registrar el share dentro del environment con el nombre lógico "pgdata"
az containerapp env storage set -n $ENV_NAME -g $RG \
  --storage-name pgdata \
  --azure-file-account-name $STG \
  --azure-file-account-key $STG_KEY \
  --azure-file-share-name pgdata \
  --access-mode ReadWrite
```

**Verificación:** `az containerapp env storage list -n $ENV_NAME -g $RG -o table` muestra `pgdata`.

---

## Paso 7 — Container App de la base de datos (`h2togo-db`) · ⚠️ riesgo D-3

Se crea con YAML porque las opciones de montaje (`mountOptions`) no tienen bandera en la CLI.

- **Ingress TCP interno** en 5432: solo las apps del mismo environment llegan a la BD; no queda
  expuesta a internet. (El TCP *externo* sí exigiría VNet; el interno no.)
- **`mountOptions`**: hace que el share aparezca con dueño `postgres` (uid/gid 999 en la imagen
  Debian de postgres) y permisos `0750`, que PostgreSQL ≥ 11 acepta. Es el arreglo al problema de D-3.
- **1 réplica fija** (min = max = 1): una BD no se puede escalar horizontalmente sobre el mismo volumen.

```bash
ENV_ID=$(az containerapp env show -n $ENV_NAME -g $RG --query id -o tsv)

cat > /tmp/h2togo-db.yaml <<EOF
location: ${LOCATION}
name: ${DB_APP}
type: Microsoft.App/containerApps
properties:
  managedEnvironmentId: ${ENV_ID}
  configuration:
    activeRevisionsMode: Single
    ingress:
      external: false
      transport: tcp
      targetPort: 5432
      exposedPort: 5432
    registries:
      - server: ${ACR}.azurecr.io
        username: ${ACR}
        passwordSecretRef: acr-password
    secrets:
      - name: acr-password
        value: "${ACR_PASSWORD}"
      - name: pg-password
        value: "${DB_PASSWORD}"
  template:
    containers:
      - name: postgres
        image: ${ACR}.azurecr.io/h2togo-db:v1
        resources:
          cpu: ${DB_CPU}
          memory: ${DB_MEM}
        env:
          - name: POSTGRES_DB
            value: ${DB_NAME}
          - name: POSTGRES_USER
            value: ${DB_USER}
          - name: POSTGRES_PASSWORD
            secretRef: pg-password
        volumeMounts:
          - volumeName: pgdata
            mountPath: /var/lib/postgresql/data
    scale:
      minReplicas: 1
      maxReplicas: 1
    volumes:
      - name: pgdata
        storageType: AzureFile
        storageName: pgdata
        mountOptions: "dir_mode=0750,file_mode=0750,uid=999,gid=999"
EOF

az containerapp create -n $DB_APP -g $RG --yaml /tmp/h2togo-db.yaml
rm /tmp/h2togo-db.yaml      # contiene secretos
```

> `POSTGRES_PASSWORD` solo se usa en la **primera** inicialización. Cambiarla después en Azure no
> cambia la contraseña real; habría que hacer `ALTER USER` dentro de la BD.

---

## Paso 8 — Verificar la base de datos

1. Logs de arranque (la primera vez tarda 1–3 min porque corre los scripts):
   ```bash
   az containerapp logs show -n $DB_APP -g $RG --tail 200 --follow
   ```
   Debe aparecer `PostgreSQL init process complete; ready for start up.` y después
   `database system is ready to accept connections`.

   Si aparece `could not change permissions of directory` o `data directory ... has invalid
   permissions` → las `mountOptions` no se aplicaron o no funcionan con esta combinación: detenerse
   y resolver **D-3**.

2. Revisar el esquema desde dentro del contenedor:
   ```bash
   az containerapp exec -n $DB_APP -g $RG --command bash
   # ya dentro:
   psql -U h2togo -d h2togo_v6 -c "\dt"                               # 19 tablas del esquema v6
   psql -U h2togo -d h2togo_v6 -c "SELECT nombre, activo FROM zonas_cobertura;"   # Benito Juarez | t
   psql -U h2togo -d h2togo_v6 -c "SELECT postgis_version();"
   exit
   ```

3. Persistencia (hacer una vez): reiniciar la revisión y confirmar que las tablas siguen ahí y que en
   los logs **no** vuelve a aparecer `init process`:
   ```bash
   REV=$(az containerapp revision list -n $DB_APP -g $RG --query "[?properties.active].name" -o tsv)
   az containerapp revision restart -n $DB_APP -g $RG --revision $REV
   ```

---

## Paso 9 — Container App del backend (`h2togo-api`)

Variables que lee `application.yml` (no hay `application-prod.yml`; el perfil `prod` solo cambia qué
implementación de SMS se usa y evita el `show-sql`/`debug` del perfil `dev`):

| Variable | Valor | Motivo |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Activa `TwilioSmsService` (OTP real). Requiere el Paso 0. |
| `DB_URL` | `jdbc:postgresql://h2togo-db:5432/h2togo_v6` | Nombre de la app de BD dentro del environment. |
| `DB_USER` / `DB_PASSWORD` | `h2togo` / secreto | Mismo usuario que en local. |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` | secretos | Credenciales de Twilio (D-2). |
| `TWILIO_FROM_NUMBER` | `+1…` | Número emisor de Twilio. |
| `H2TOGO_CORS_ALLOWED_ORIGINS` | ver Paso 1 | Solo relevante para el panel web (D-8). |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=60 -XX:ReservedCodeCacheSize=96m -Duser.timezone=America/Mexico_City` | Sin esto la JVM toma solo 25 % de la memoria del contenedor (~256 MB) y el grafo OSM no cabe. La zona horaria hace que los jobs de las 03:00/03:30 sean hora de CDMX (como indica el TT) y que "hoy" en RN-030 sea el de CDMX, igual que en las máquinas de desarrollo. |

`PORT` no se define: el backend usa 8080 por defecto, que es el `--target-port`.

```bash
az containerapp create -n $API_APP -g $RG --environment $ENV_NAME \
  --image $ACR.azurecr.io/h2togo-backend:v1 \
  --registry-server $ACR.azurecr.io --registry-username $ACR --registry-password "$ACR_PASSWORD" \
  --ingress external --target-port 8080 \
  --cpu $API_CPU --memory $API_MEM \
  --min-replicas $API_MIN_REPLICAS --max-replicas 1 \
  --secrets db-password="$DB_PASSWORD" twilio-sid="$TWILIO_ACCOUNT_SID" twilio-token="$TWILIO_AUTH_TOKEN" \
  --env-vars \
    SPRING_PROFILES_ACTIVE=prod \
    DB_URL=jdbc:postgresql://${DB_APP}:5432/${DB_NAME} \
    DB_USER=$DB_USER \
    DB_PASSWORD=secretref:db-password \
    TWILIO_ACCOUNT_SID=secretref:twilio-sid \
    TWILIO_AUTH_TOKEN=secretref:twilio-token \
    TWILIO_FROM_NUMBER=$TWILIO_FROM_NUMBER \
    H2TOGO_CORS_ALLOWED_ORIGINS=$CORS_ORIGINS \
    "JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=60 -XX:ReservedCodeCacheSize=96m -Duser.timezone=America/Mexico_City"

export API_FQDN=$(az containerapp show -n $API_APP -g $RG --query properties.configuration.ingress.fqdn -o tsv)
echo "https://$API_FQDN"
```

**Por qué `max-replicas 1`:** el broker STOMP (`enableSimpleBroker`) y el flag anti-duplicado de
proximidad viven en memoria (PLAN_BACKEND §10 #2). Con dos réplicas, un cliente suscrito a una réplica
no recibiría la ubicación publicada en la otra.

Revisar el arranque:

```bash
az containerapp logs show -n $API_APP -g $RG --tail 200 --follow
```

Debe verse `Picked up JAVA_TOOL_OPTIONS`, `[TWILIO] Cliente de Twilio inicializado`, `Tomcat started on
port 8080` y la carga del grafo OSM. Si Hibernate falla con `Schema-validation: missing table …`, la BD
no se inicializó (volver al Paso 8).

---

## Paso 10 — Verificación de extremo a extremo

```bash
# 1. Healthcheck: debe responder {"status":"UP","db":"ok"}
curl -s https://$API_FQDN/api/v1/health

# 2. Swagger UI y consola de pruebas (navegador)
echo "https://$API_FQDN/swagger-ui.html"
echo "https://$API_FQDN/console/"
echo "https://$API_FQDN/admin/"

# 3. WebSocket: el handshake debe responder "HTTP/1.1 101 Switching Protocols"
curl -s -i --http1.1 -N -m 5 \
  -H "Connection: Upgrade" -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: SGVsbG8sIHdvcmxkIQ==" \
  https://$API_FQDN/ws | head -1
```

**4. Flujo OTP completo (CU-001 → CU-002)** con un número real de 10 dígitos que esté **verificado en
Twilio** si la cuenta es trial (D-2). El backend antepone `+52` si el número no trae `+`.

```bash
TEL="55XXXXXXXX"   # número real de prueba
curl -s -X POST https://$API_FQDN/api/v1/auth/registro -H "Content-Type: application/json" -d "{
  \"nombre\":\"Prueba\",\"apellidos\":\"Azure\",\"correo\":\"prueba.azure@test.mx\",
  \"password\":\"password123\",\"telefono\":\"$TEL\",\"rol\":\"cliente\"}"
# → 201. Debe llegar el SMS. En logs del backend: "[TWILIO] SMS enviado a +52... SID: SM..."

curl -s -o /dev/null -w "%{http_code}\n" -X POST https://$API_FQDN/api/v1/auth/verificacion-telefono \
  -H "Content-Type: application/json" -d '{"correo":"prueba.azure@test.mx","codigo":"<CODIGO_DEL_SMS>"}'
# → 204

curl -s -X POST https://$API_FQDN/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{"correo":"prueba.azure@test.mx","password":"password123","tokenFcm":"fcm-demo"}'
# → 200 con el token de sesión
```

Si el registro responde 201 pero el SMS no llega, el backend **no** falla (captura la excepción para
permitir el reenvío): buscar `[TWILIO] Error al enviar` en los logs y ver la sección B.

**5. Colecciones `.http`:** en `h2togo-backend/http/*.http` cambiar `@host` a `https://<API_FQDN>`.

---

## Paso 11 — Apuntar la app móvil a Azure

`h2togo-mobile/app/src/main/java/com/htogo/app/data/api/ApiConstants.kt`:

```kotlin
const val BASE_URL = "https://<API_FQDN>/api/v1/"   // la barra final es obligatoria para Retrofit
```

- Probar con el teléfono en **datos móviles** (no en el Wi-Fi de casa) para confirmar que no depende
  de la red local ni de `adb reverse`.
- Con HTTPS ya no hace falta `android:usesCleartextTraffic="true"`; se puede dejar mientras se sigue
  probando contra `localhost`.
- Cuando la app implemente el rastreo por STOMP, la URL será `wss://<API_FQDN>/ws`.

---

## Paso 12 — CI/CD con Azure Pipelines · ⚠️ PENDIENTE D-7

Primero debe funcionar el despliegue manual (Pasos 1–10). El pipeline solo automatiza el Paso 4.2 y
el redespliegue; la imagen de la BD se cambia a mano y muy rara vez (ver sección A).

### 12.1 Preparar Azure DevOps (una vez)

1. Crear organización y proyecto en <https://dev.azure.com> (pueden ser gratuitos).
2. **Agentes:** las organizaciones nuevas no tienen agente hospedado gratis hasta pedirlo en
   <https://aka.ms/azpipelines-parallelism-request> (aprobación en unos días hábiles). Alternativa
   inmediata: un **agente propio** (*Project settings → Agent pools → Default → New agent*) en una
   laptop con Docker, JDK 21 y Maven; en ese caso cambiar `pool` en el YAML a `name: Default`.
3. *Project settings → Service connections*:
   - **Docker Registry → Others**: servidor `https://<ACR>.azurecr.io`, usuario `<ACR>`, contraseña
     `$ACR_PASSWORD`. Nombre: `sc-acr-h2togo`. (Tipo "Others" para no depender de Entra ID.)
   - **Azure Resource Manager → Workload identity federation (automatic)**, alcance: grupo de recursos
     `rg-h2togo`. Nombre: `sc-azure-h2togo`. **Si falla por permisos** (común con cuentas
     institucionales), se queda el despliegue manual y se reporta en D-7.
4. *Pipelines → New pipeline → GitHub → Brandonttt/H2ToGo* (el dueño del repo autoriza la app de
   Azure Pipelines) → *Existing Azure Pipelines YAML file* → `/azure-pipelines.yml`.

### 12.2 `azure-pipelines.yml` (raíz del repo)

Sustituir `<ACR>` por el nombre real. Los nombres de service connection van literales porque Azure
DevOps los resuelve antes de ejecutar (no aceptan variables de tiempo de ejecución).

```yaml
# CI/CD del backend H2ToGo: pruebas → imagen al ACR → nueva revisión en Container Apps.
trigger:
  branches:
    include: [ main ]
  paths:
    include: [ h2togo-backend/* ]

pool:
  vmImage: ubuntu-latest          # agente propio: reemplazar por  name: Default

variables:
  acrLoginServer: '<ACR>.azurecr.io'
  imageRepository: 'h2togo-backend'
  resourceGroup: 'rg-h2togo'
  containerApp: 'h2togo-api'
  tag: '$(Build.BuildId)'

stages:
  - stage: Test
    jobs:
      - job: mvn_verify
        steps:
          - script: |
              export JAVA_HOME=$JAVA_HOME_21_X64
              cd h2togo-backend
              mvn -B verify
            displayName: 'mvn verify (unitarias + integración con Testcontainers)'

  - stage: Build
    dependsOn: Test
    jobs:
      - job: docker
        steps:
          - task: Docker@2
            displayName: 'Build y push al ACR'
            inputs:
              command: buildAndPush
              containerRegistry: 'sc-acr-h2togo'
              repository: $(imageRepository)
              Dockerfile: h2togo-backend/Dockerfile
              buildContext: h2togo-backend
              tags: |
                $(tag)

  - stage: Deploy
    dependsOn: Build
    jobs:
      - job: containerapp
        steps:
          - task: AzureCLI@2
            displayName: 'Nueva revisión de h2togo-api'
            inputs:
              azureSubscription: 'sc-azure-h2togo'
              scriptType: bash
              scriptLocation: inlineScript
              inlineScript: |
                az extension add --name containerapp --upgrade -y
                az containerapp update -n $(containerApp) -g $(resourceGroup) \
                  --image $(acrLoginServer)/$(imageRepository):$(tag)
```

**Verificación:** un push a `main` que toque `h2togo-backend/` dispara el pipeline; al terminar,
`az containerapp revision list -n $API_APP -g $RG -o table` muestra una revisión nueva con la imagen
`h2togo-backend:<BuildId>`, y `/api/v1/health` sigue respondiendo.

---

## Paso 13 — Panel admin

No hay recurso extra: el panel es una página estática dentro del jar del backend y se publica con
cada `desplegar-backend.ps1` (o el pipeline del Paso 12).

- URL: `https://$API_FQDN/admin/` (hoy: https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/admin/)
- Acceso: cualquier cuenta con rol `admin`. Las llamadas a `/api/v1/admin/**` exigen ese rol.
- Al estar en el mismo origen que la API **no hace falta tocar** `H2TOGO_CORS_ALLOWED_ORIGINS`.
- Sesión única por cuenta (RN-018): si el admin ya tiene sesión abierta en otro lado, el panel pide
  confirmación antes de cerrarla.

---

## Paso 14 — Application Insights · ⚠️ PENDIENTE D-9

Hacer solo cuando todo lo anterior funcione. Resumen de lo que implica (no ejecutar aún):

1. Crear el recurso ligado al mismo Log Analytics (hereda el tope diario del Paso 5):
   ```bash
   az extension add --name application-insights
   az monitor app-insights component create -a appi-h2togo -g $RG -l $LOCATION \
     --workspace $(az monitor log-analytics workspace show -g $RG -n $LAW --query id -o tsv)
   ```
2. Agregar el agente al `h2togo-backend/Dockerfile` (etapa runtime), con la última versión 3.x de
   <https://github.com/microsoft/ApplicationInsights-Java/releases>:
   ```dockerfile
   ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/<VERSION>/applicationinsights-agent-<VERSION>.jar /app/applicationinsights-agent.jar
   ```
3. En la Container App: variable `APPLICATIONINSIGHTS_CONNECTION_STRING` (como secreto) y agregar
   `-javaagent:/app/applicationinsights-agent.jar` al inicio de `JAVA_TOOL_OPTIONS`.
4. Vigilar memoria: probablemente haya que subir el backend a `--cpu 0.75 --memory 1.5Gi`.

---

## Paso 15 — RabbitMQ y notificaciones push

Las notificaciones (pedido asignado, en camino, cerca, entregado, nuevo pedido disponible,
solicitud resuelta, lotes por caducar) ya no se envían dentro de la petición del usuario: el
backend las publica en RabbitMQ **después del commit** y un consumidor las entrega.

```
servicio ──(after commit)──▶ exchange h2togo.eventos (topic, rk notificacion.push)
                                   │
                                   ▼
                        cola h2togo.notificaciones ──▶ NotificacionesListener
                                   │ 4 intentos (1 s, 2 s, 4 s)      ├─▶ WebSocket /user/queue/notificaciones (app abierta)
                                   ▼ si todos fallan                 └─▶ FCM (app cerrada, si hay credenciales)
                        DLQ h2togo.notificaciones.dlq (para revisar)
```

- Si la transacción se revierte, no se publica nada.
- Si RabbitMQ está caído al publicar, el cambio de negocio **no** se revierte; se registra un error
  en el log y esa notificación se pierde.
- Sin RabbitMQ (`H2TOGO_MENSAJERIA_HABILITADA=false`, valor por defecto) la entrega es directa en el
  mismo proceso: así corren el desarrollo local sin Docker y las pruebas.

### 15.1 Crear RabbitMQ y conectar el backend (una sola vez)

Primero despliega el backend con `desplegar-backend.ps1` (la imagen debe incluir el soporte de
mensajería). Luego:

```powershell
./desplegar-rabbitmq.ps1
```

Crea `h2togo-rabbitmq` con ingress TCP **interno** en 5672 (no queda expuesto a internet), genera una
contraseña y configura en `h2togo-api` las variables `H2TOGO_MENSAJERIA_HABILITADA`,
`RABBITMQ_HOST/PORT/USER` y el secreto `RABBITMQ_PASSWORD`. Guarda la contraseña que imprime.

**Verificación:** en `az containerapp logs show -n h2togo-api -g rg-h2togo --tail 80` aparece
`Created new connection` de RabbitMQ y, al mover un pedido, líneas `[PUSH] usuario ...`.

### 15.2 Activar Firebase Cloud Messaging (notificaciones con la app cerrada)

Sin este paso las notificaciones llegan solo mientras la app está abierta (WebSocket).

> Guía paso a paso para entregar a quien tenga la cuenta del equipo: [`docs/CONFIGURAR_FIREBASE.md`](docs/CONFIGURAR_FIREBASE.md).

1. En <https://console.firebase.google.com> crea un proyecto (plan Spark, gratuito).
2. **App Android:** agrega una app con el paquete `com.htogo.app`, descarga `google-services.json` y
   colócalo en `h2togo-mobile/app/`. La app activa Firebase sola al compilar si el archivo existe.
   No lo subas al repositorio (ya está en `.gitignore`).
3. **Backend:** en *Configuración del proyecto → Cuentas de servicio → Generar nueva clave privada*
   descarga el JSON y ejecuta:
   ```powershell
   ./desplegar-rabbitmq.ps1 -Password "<la contraseña de 15.1>" -FcmCredenciales ./cuenta-servicio.json
   ```
   El JSON se guarda como secreto (`FCM_CREDENCIALES_JSON`, en base64). En los logs debe aparecer
   `FCM habilitado para el proyecto ...`. Borra el archivo local después.

---

## A. Operación diaria

| Tarea | Comando |
|---|---|
| Redesplegar backend a mano | `docker buildx build --platform linux/amd64 -t $ACR.azurecr.io/h2togo-backend:v2 --push h2togo-backend` y luego `az containerapp update -n $API_APP -g $RG --image $ACR.azurecr.io/h2togo-backend:v2` |
| Ver logs | `az containerapp logs show -n $API_APP -g $RG --tail 200 --follow` (igual con `$DB_APP`) |
| Reiniciar | `az containerapp revision restart -n <app> -g $RG --revision <revision-activa>` |
| **Pausar para ahorrar** (vacaciones, semanas sin pruebas) | Desactivar la revisión activa de **ambas** apps (las revisiones inactivas no se cobran): `az containerapp revision deactivate -n <app> -g $RG --revision <rev>`. Reanudar con `revision activate` (primero la BD). ACR y Storage siguen cobrando (~$6/mes). |
| Borrar todo | `az group delete -n $RG` (irreversible, incluye los datos) |

**Reglas para la BD:**

- **No ejecutar `az containerapp update` sobre `h2togo-db` con la réplica corriendo.** Una revisión
  nueva arranca antes de apagar la vieja y habría dos PostgreSQL sobre el mismo volumen. Si hay que
  cambiarla: desactivar la revisión actual, esperar a que no haya réplicas
  (`az containerapp replica list -n $DB_APP -g $RG`), y entonces actualizar.
- **Reinicializar la BD desde cero** (borra todos los datos): desactivar la revisión de la BD, vaciar
  el share y reactivar; al encontrar el directorio vacío vuelve a correr los scripts del Paso 4.1.
  ```bash
  az storage file delete-batch --source pgdata --account-name $STG --account-key $STG_KEY
  ```

---

## B. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `exec format error` al arrancar | Imagen construida para `arm64` en Mac | Reconstruir con `--platform linux/amd64` |
| `RequestDisallowedByAzure` | Región no permitida por la política de estudiante | Paso 2 |
| BD: `could not change permissions of directory` / `invalid permissions` | SMB no respeta permisos (D-3) | Revisar `mountOptions` del YAML; si persiste, decidir D-3 |
| Backend: `No qualifying bean of type ... PushService` | Falta el Paso 0 | Aplicar el cambio, reconstruir la imagen |
| Backend: `Connection refused` / `UnknownHost h2togo-db` | BD aún iniciando o ingress TCP mal configurado | Esperar el log `ready to accept connections`; revisar `az containerapp show -n $DB_APP -g $RG --query properties.configuration.ingress` |
| Backend: `Schema-validation: missing table` | Los scripts de init no corrieron (el volumen ya tenía datos) | Reinicializar la BD (sección A) |
| Backend reinicia con `OOMKilled` o `OutOfMemoryError` | Poca memoria para JVM + grafo OSM | Subir a `--cpu 0.75 --memory 1.5Gi` (combinación válida) |
| Registro 201 pero no llega SMS; log `[TWILIO] Error ... 21608` | Cuenta trial y número no verificado | Verificar el número en la consola de Twilio o actualizar la cuenta (D-2) |
| Registro 201 pero no llega SMS; error de contenido/plantilla | Trial que no permite cuerpo personalizado | D-2 |
| Pipeline: `No hosted parallelism has been purchased or granted` | Organización nueva sin agente hospedado | Formulario o agente propio (Paso 12.1) |
| Pipeline: falla al crear la service connection ARM | Sin permiso para registrar apps en el tenant | Despliegue manual; reportar en D-7 |
| App móvil: timeout o `Unable to resolve host` | `BASE_URL` sin actualizar o sin `https://` | Paso 11 |

---

## C. Diferencias TT ↔ implementación que conviene alinear en el documento del TT

Para que lo desplegado coincida con lo que se va a defender (TT-II, 17 nov – 1 dic):

1. **Motor de BD:** MariaDB → PostgreSQL 16 + PostGIS (diagrama, tabla de costos y texto).
2. **Proveedor de OTP:** el código usa Twilio; la factibilidad económica muestra Firebase (D-2).
3. **Escalado del backend:** el TT dice "escala a cero"; con los jobs programados la configuración
   defendible es mínimo 1 réplica (D-4) y el costo cambia (ver "Costo estimado").
4. **Mapa en la app:** el diagrama dice Google Maps; la app usa Leaflet + teselas de OpenStreetMap.
5. **Actualización diaria del grafo OSM:** descrita en el TT, no implementada (grafo fijo en el jar).
6. **Notificaciones push (FCM):** descritas en el TT, hoy solo se registran en el log.
7. **"docker-compose" en Container Apps:** en la implementación son dos Container Apps dentro de un
   mismo environment (ver Resumen).
