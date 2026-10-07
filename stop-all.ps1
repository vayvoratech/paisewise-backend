# Stop all PaiseWise microservices and containers
Write-Host "🛑 Stopping all running Java / Spring Boot processes..." -ForegroundColor Cyan
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force

Write-Host "📦 Stopping Docker containers..." -ForegroundColor Cyan
docker compose down

Write-Host "✅ All services stopped." -ForegroundColor Green
