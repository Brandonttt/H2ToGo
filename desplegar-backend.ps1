<#
.SYNOPSIS
    Script de despliegue rápido para H2ToGo Backend a Azure Container Apps.
.DESCRIPTION
    Compila la imagen Docker en Azure Container Registry (ACR) y actualiza
    la revisión activa en Azure Container Apps sin requerir permisos de administrador de tenant.
.EXAMPLE
    .\desplegar-backend.ps1
    .\desplegar-backend.ps1 -Tag "v3"
#>
param (
    [string]$Tag = ""
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "         H2ToGo Backend - Despliegue a Azure             " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Comprobar sesión de Azure CLI
Write-Host "`n[1/4] Verificando sesión activa en Azure CLI..." -ForegroundColor Yellow
try {
    $account = az account show --query "{sub:name, user:user.name}" -o json | ConvertFrom-Json
    Write-Host "✓ Sesión activa como: $($account.user) ($($account.sub))" -ForegroundColor Green
} catch {
    Write-Host "✗ No se detectó una sesión activa en Azure CLI. Ejecuta 'az login' primero." -ForegroundColor Red
    exit 1
}

# 2. Definir Tag de la versión
if ([string]::IsNullOrWhiteSpace($Tag)) {
    $Tag = "v" + (Get-Date -Format "yyyyMMdd-HHmmss")
}
$acrName = "acrh2togo87"
$appName = "h2togo-api"
$rgName = "rg-h2togo"
$imageFullName = "$acrName.azurecr.io/h2togo-backend:$Tag"

Write-Host "`n[2/4] Compilando imagen en la nube ($acrName) con etiqueta: $Tag ..." -ForegroundColor Yellow
az acr build -r $acrName -t "h2togo-backend:$Tag" h2togo-backend
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Error al compilar la imagen en ACR." -ForegroundColor Red
    exit 1
}
Write-Host "✓ Imagen construida y almacenada en ACR: $imageFullName" -ForegroundColor Green

# 3. Actualizar Azure Container App
Write-Host "`n[3/4] Desplegando nueva revisión en Container App '$appName'..." -ForegroundColor Yellow
az containerapp update -n $appName -g $rgName --image $imageFullName
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Error al actualizar la Container App en Azure." -ForegroundColor Red
    exit 1
}
Write-Host "✓ Nueva revisión desplegada exitosamente." -ForegroundColor Green

# 4. Verificación de salud (Health Check)
Write-Host "`n[4/4] Verificando estado del servicio (Health Check)..." -ForegroundColor Yellow
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
            Write-Host "`n✓ Backend saludable: $($response | ConvertTo-Json -Compress)" -ForegroundColor Green
            break
        }
    } catch {
        # Esperar a que la nueva revisión termine de levantar
    }
    Start-Sleep -Seconds 7
    $attempt++
}

if (-not $healthy) {
    Write-Host "⚠ El backend tardó en responder al healthcheck. Puedes revisar los logs con:" -ForegroundColor Yellow
    Write-Host "  az containerapp logs show -n $appName -g $rgName --tail 50 --follow" -ForegroundColor Gray
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " Despliegue completado con éxito: $Tag" -ForegroundColor Green
Write-Host " URL API: https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/api/v1/" -ForegroundColor White
Write-Host " Swagger: https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/swagger-ui.html" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Cyan
