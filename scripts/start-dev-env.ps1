# scripts/start-dev-env.ps1
# Automates the startup of the APEX development environment
# 1. Checks and clears ports 8080 and 8081
# 2. Ensures log directories exist
# 3. Starts REST API and Playground in separate windows

$ErrorActionPreference = "Stop"
$projectRoot = Resolve-Path "$PSScriptRoot/.."

Write-Host "=== APEX Development Environment Startup ===" -ForegroundColor Cyan

# 1. Create necessary directories
$logDir = Join-Path $projectRoot "apex-rest-api/logs"
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
    Write-Host "[OK] Created log directory: $logDir" -ForegroundColor Green
}

# 2. Function to kill process on port
function Stop-PortProcess ($port) {
    $connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connections) {
        $processId = $connections | Select-Object -ExpandProperty OwningProcess -Unique
        if ($processId) {
            Write-Host "[INFO] Port $port is in use by PID $processId. Stopping..." -ForegroundColor Yellow
            Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
            Write-Host "[OK] Stopped process on port $port" -ForegroundColor Green
        }
    }
}

# 3. Clean up ports
Write-Host "Checking ports..."
Stop-PortProcess 8080
Stop-PortProcess 8081

# 4. Start REST API
Write-Host "Launching APEX REST API (Port 8080)..." -ForegroundColor Cyan
$restApiDir = Join-Path $projectRoot "apex-rest-api"
Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -WorkingDirectory $restApiDir

# 5. Start Playground
Write-Host "Launching APEX Playground (Port 8081)..." -ForegroundColor Cyan
$playgroundDir = Join-Path $projectRoot "apex-playground"
Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -WorkingDirectory $playgroundDir

Write-Host "`n------------------------------------------------" -ForegroundColor Green
Write-Host "Startup initiated!" -ForegroundColor Green
Write-Host "The applications are launching in separate windows."
Write-Host "Monitor those windows for startup logs."
Write-Host "`nAccess URLs:"
Write-Host " - Playground: http://localhost:8081"
Write-Host " - REST API:   http://localhost:8080/swagger-ui.html"
Write-Host "------------------------------------------------" -ForegroundColor Green
