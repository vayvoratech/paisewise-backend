@echo off
echo Stopping all Java Spring Boot processes...
taskkill /F /IM java.exe 2>nul

echo Stopping Docker containers...
docker compose down

echo All PaiseWise services stopped.
