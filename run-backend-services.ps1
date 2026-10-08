$ErrorActionPreference = "Continue"
$workDir = "c:\Users\hp\Desktop\paisewise\paisewise-backend"

Write-Host "🧹 Stopping any old Java processes..." -ForegroundColor Yellow
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 2

$javaExe = (Get-Command java.exe).Source

Write-Host "1. Starting Discovery Server on :8761..." -ForegroundColor Green
Start-Process -FilePath $javaExe -ArgumentList "-jar", "$workDir\discovery-server\target\discovery-server-0.0.1-SNAPSHOT.jar", "--spring.profiles.active=dev" -WorkingDirectory $workDir -RedirectStandardOutput "$workDir\discovery.log" -RedirectStandardError "$workDir\discovery_err.log"
Start-Sleep -Seconds 6

Write-Host "2. Starting API Gateway on :8080..." -ForegroundColor Green
Start-Process -FilePath $javaExe -ArgumentList "-jar", "$workDir\api-gateway\target\api-gateway-0.0.1-SNAPSHOT.jar", "--spring.profiles.active=dev" -WorkingDirectory $workDir -RedirectStandardOutput "$workDir\gateway.log" -RedirectStandardError "$workDir\gateway_err.log"
Start-Sleep -Seconds 4

$services = @(
    "auth-service",
    "profile-service",
    "learn-service",
    "practice-service",
    "portfolio-service",
    "community-service",
    "market-data-service"
)

foreach ($svc in $services) {
    Write-Host "Starting $svc..." -ForegroundColor Green
    $logFile = "$workDir\$svc.log"
    $errFile = "$workDir\${svc}_err.log"
    Start-Process -FilePath $javaExe -ArgumentList "-jar", "$workDir\$svc\target\$svc-0.0.1-SNAPSHOT.jar", "--spring.profiles.active=dev" -WorkingDirectory $workDir -RedirectStandardOutput $logFile -RedirectStandardError $errFile
    Start-Sleep -Seconds 2
}

Write-Host "✅ All services started in background with logging!" -ForegroundColor Yellow
