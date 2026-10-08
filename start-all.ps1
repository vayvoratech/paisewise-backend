# Ultra-fast launcher for PaiseWise backend with auto port-cleanup
$ErrorActionPreference = "Continue"

Write-Host "🧹 Stopping any old Java processes to free all ports..." -ForegroundColor Yellow
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 1

$env:Path += ";C:\Users\hp\maven\apache-maven-3.9.9\bin"
$workDir = $PSScriptRoot
if (-not $workDir) { $workDir = "c:\Users\hp\Desktop\paisewise\paisewise-backend" }

Write-Host "⚡ Building all jars with 4 parallel threads..." -ForegroundColor Cyan
& "$env:USERPROFILE\maven\apache-maven-3.9.9\bin\mvn.cmd" clean package -DskipTests -T 4

Write-Host "🚀 Starting Discovery Server (Port 8761)..." -ForegroundColor Green
Start-Process powershell -WorkingDirectory $workDir -ArgumentList "-NoExit", "-Command", "Write-Host '--- Discovery Server :8761 ---' -ForegroundColor Green; java -jar discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev"
Start-Sleep -Seconds 5

Write-Host "🚀 Starting API Gateway (Port 8080)..." -ForegroundColor Green
Start-Process powershell -WorkingDirectory $workDir -ArgumentList "-NoExit", "-Command", "Write-Host '--- API Gateway :8080 ---' -ForegroundColor Green; java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev"
Start-Sleep -Seconds 3

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
    Write-Host "🚀 Starting $svc..." -ForegroundColor Green
    Start-Process powershell -WorkingDirectory $workDir -ArgumentList "-NoExit", "-Command", "Write-Host '--- $svc ---' -ForegroundColor Green; java -jar $svc/target/$svc-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev"
}

Write-Host "`n✅ All services launched! Gateway entry point: http://localhost:8080" -ForegroundColor Yellow
