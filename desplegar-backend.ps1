param (
    [string]$Tag = ""
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "         H2ToGo Backend - Despliegue a Azure             " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Comprobar sesion de Azure CLI
Write-Host ""
Write-Host "[1/4] Verificando sesion activa en Azure CLI..." -ForegroundColor Yellow
try {
    $account = az account show --query "{sub:name, user:user.name}" -o json | ConvertFrom-Json
    Write-Host "[OK] Sesion activa como: $($account.user) ($($account.sub))" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] No se detecto sesion activa en Azure CLI. Ejecuta 'az login' primero." -ForegroundColor Red
    exit 1
}

# 2. Definir Tag de la version
if ([string]::IsNullOrWhiteSpace($Tag)) {
    $Tag = "v" + (Get-Date -Format "yyyyMMdd-HHmmss")
}
$acrName = "acrh2togo87"
$appName = "h2togo-api"
$rgName = "rg-h2togo"
$imageFullName = "$acrName.azurecr.io/h2togo-backend:$Tag"

Write-Host ""
Write-Host "[2/4] Compilando imagen en ACR ($acrName) con etiqueta: $Tag ..." -ForegroundColor Yellow
az acr build -r $acrName -t "h2togo-backend:$Tag" h2togo-backend
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Error al compilar la imagen en ACR." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Imagen construida y almacenada en ACR: $imageFullName" -ForegroundColor Green

# 3. Actualizar Azure Container App
Write-Host ""
Write-Host "[3/4] Desplegando nueva revision en Container App '$appName'..." -ForegroundColor Yellow
az containerapp update -n $appName -g $rgName --image $imageFullName
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Error al actualizar la Container App en Azure." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Nueva revision desplegada exitosamente." -ForegroundColor Green

# 4. Verificacion de salud (Health Check)
Write-Host ""
Write-Host "[4/4] Verificando estado del servicio (Health Check)..." -ForegroundColor Yellow
$healthUrl = "https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/api/v1/health"

$maxAttempts = 6
$attempt = 1
$healthy = $false

Start-Sleep -Seconds 5
while ($attempt -le $maxAttempts) {
    Write-Host "  Consultando $healthUrl (Intento $attempt/$maxAttempts)..." -ForegroundColor Gray
    try {
        $response = Invoke-RestMethod -Uri $healthUrl -Method Get -TimeoutSec 10
        if ($response.status -eq "UP") {
            $healthy = $true
            $jsonRes = $response | ConvertTo-Json -Compress
            Write-Host "[OK] Backend saludable: $jsonRes" -ForegroundColor Green
            break
        }
    } catch {
        # Esperando inicio del contenedor
    }
    Start-Sleep -Seconds 7
    $attempt++
}

if (-not $healthy) {
    Write-Host "[AVISO] El backend tardo en responder. Puedes revisar logs con:" -ForegroundColor Yellow
    Write-Host "  az containerapp logs show -n $appName -g $rgName --tail 50 --follow" -ForegroundColor Gray
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Despliegue completado con exito: $Tag" -ForegroundColor Green
Write-Host " URL API: https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/api/v1/" -ForegroundColor White
Write-Host " Swagger: https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/swagger-ui.html" -ForegroundColor White
Write-Host " Panel admin: https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/admin/" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Cyan
