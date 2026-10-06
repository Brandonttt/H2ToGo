<#
  H2ToGo - RabbitMQ en Azure Container Apps (se ejecuta UNA vez).

  Crea la Container App "h2togo-rabbitmq" en el mismo environment que el backend, con ingress
  TCP interno (5672): solo las apps del environment la ven, no queda expuesta a internet.
  Luego configura el backend para publicar las notificaciones en RabbitMQ y, si se indica,
  activa Firebase Cloud Messaging.

  Uso:
    ./desplegar-rabbitmq.ps1
    ./desplegar-rabbitmq.ps1 -FcmCredenciales ./firebase-cuenta-servicio.json

  Los mensajes de RabbitMQ no se persisten en disco (sin volumen): si el contenedor se reinicia
  se pierden las notificaciones aún no entregadas. Es aceptable porque son avisos efímeros.
#>
param (
    [string]$Password = "",
    [string]$FcmCredenciales = ""
)

$ErrorActionPreference = "Stop"

$rgName = "rg-h2togo"
$envName = "cae-h2togo"
$rabbitApp = "h2togo-rabbitmq"
$apiApp = "h2togo-api"
$rabbitUser = "h2togo"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "        H2ToGo - RabbitMQ (notificaciones) en Azure        " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

Write-Host ""
Write-Host "[1/4] Verificando sesion activa en Azure CLI..." -ForegroundColor Yellow
try {
    $account = az account show --query "{sub:name, user:user.name}" -o json | ConvertFrom-Json
    Write-Host "[OK] Sesion activa como: $($account.user) ($($account.sub))" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] No se detecto sesion activa en Azure CLI. Ejecuta 'az login' primero." -ForegroundColor Red
    exit 1
}

if ([string]::IsNullOrWhiteSpace($Password)) {
    # Solo letras y numeros: evita problemas de escape en la CLI.
    $chars = (48..57) + (65..90) + (97..122)
    $Password = -join ($chars | Get-Random -Count 24 | ForEach-Object { [char]$_ })
}

Write-Host ""
Write-Host "[2/4] Creando la Container App '$rabbitApp'..." -ForegroundColor Yellow
$existe = az containerapp show -n $rabbitApp -g $rgName --query name -o tsv 2>$null
if ($existe) {
    Write-Host "[OK] Ya existe; se conserva su configuracion." -ForegroundColor Green
    Write-Host "     (Si no conoces su contrasena, pasala con -Password para alinear el backend.)" -ForegroundColor Gray
} else {
    az containerapp create -n $rabbitApp -g $rgName --environment $envName `
        --image "rabbitmq:3.13-management" `
        --ingress internal --transport tcp --target-port 5672 --exposed-port 5672 `
        --cpu 0.25 --memory 0.5Gi --min-replicas 1 --max-replicas 1 `
        --secrets "rabbit-pass=$Password" `
        --env-vars "RABBITMQ_DEFAULT_USER=$rabbitUser" "RABBITMQ_DEFAULT_PASS=secretref:rabbit-pass" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] No se pudo crear la Container App de RabbitMQ." -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] RabbitMQ creado (interno, puerto 5672)." -ForegroundColor Green
}

Write-Host ""
Write-Host "[3/4] Configurando el backend '$apiApp' para usar RabbitMQ..." -ForegroundColor Yellow
$secretos = @("rabbit-pass=$Password")
$envVars = @(
    "H2TOGO_MENSAJERIA_HABILITADA=true",
    "RABBITMQ_HOST=$rabbitApp",
    "RABBITMQ_PORT=5672",
    "RABBITMQ_USER=$rabbitUser",
    "RABBITMQ_PASSWORD=secretref:rabbit-pass"
)
if (-not [string]::IsNullOrWhiteSpace($FcmCredenciales)) {
    if (-not (Test-Path $FcmCredenciales)) {
        Write-Host "[ERROR] No existe el archivo $FcmCredenciales" -ForegroundColor Red
        exit 1
    }
    # Base64 en una sola linea: el JSON tiene comillas y saltos que la CLI no pasa bien.
    $fcmB64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path $FcmCredenciales)))
    $secretos += "fcm-credenciales=$fcmB64"
    $envVars += "FCM_CREDENCIALES_JSON=secretref:fcm-credenciales"
    Write-Host "     Firebase Cloud Messaging: se activara con $FcmCredenciales" -ForegroundColor Gray
}
az containerapp secret set -n $apiApp -g $rgName --secrets $secretos | Out-Null
az containerapp update -n $apiApp -g $rgName --set-env-vars $envVars | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] No se pudo actualizar el backend." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Backend configurado (nueva revision)." -ForegroundColor Green

Write-Host ""
Write-Host "[4/4] Verificando en los logs del backend..." -ForegroundColor Yellow
Write-Host "  Busca 'Created new connection' (RabbitMQ) y 'FCM habilitado' en:" -ForegroundColor Gray
Write-Host "  az containerapp logs show -n $apiApp -g $rgName --tail 80" -ForegroundColor Gray

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " RabbitMQ listo. Usuario: $rabbitUser" -ForegroundColor Green
Write-Host " Guarda la contrasena en un lugar seguro: $Password" -ForegroundColor White
Write-Host " Consola de administracion (desde tu equipo):" -ForegroundColor White
Write-Host "   az containerapp exec -n $rabbitApp -g $rgName --command 'rabbitmq-diagnostics status'" -ForegroundColor Gray
Write-Host "==========================================================" -ForegroundColor Cyan
