@echo off
set "PATH=%PATH%;C:\Users\hp\maven\apache-maven-3.9.9\bin"
cd /d "c:\Users\hp\Desktop\paisewise-backend"

echo [1/3] Cleaning up any old/stale Java processes to free all ports...
taskkill /F /IM java.exe 2>nul
ping 127.0.0.1 -n 2 >nul

echo [2/3] Fast parallel build (skipping tests)...
call mvn clean package -DskipTests -T 4

echo [3/3] Launching microservices...
start "Discovery-Server:8761" cmd /k "cd /d c:\Users\hp\Desktop\paisewise-backend && java -Dspring.profiles.active=dev -jar discovery-server\target\discovery-server-0.0.1-SNAPSHOT.jar"
ping 127.0.0.1 -n 6 >nul

start "API-Gateway:8080" cmd /k "cd /d c:\Users\hp\Desktop\paisewise-backend && java -Dspring.profiles.active=dev -jar api-gateway\target\api-gateway-0.0.1-SNAPSHOT.jar"
ping 127.0.0.1 -n 3 >nul

start "Auth-Service:8081" cmd /k "cd /d c:\Users\hp\Desktop\paisewise-backend && java -Dspring.profiles.active=dev -jar auth-service\target\auth-service-0.0.1-SNAPSHOT.jar"
start "Profile-Service:8082" cmd /k "cd /d c:\Users\hp\Desktop\paisewise-backend && java -Dspring.profiles.active=dev -jar profile-service\target\profile-service-0.0.1-SNAPSHOT.jar"
start "Learn-Service:8083" cmd /k "cd /d c:\Users\hp\Desktop\paisewise-backend && java -Dspring.profiles.active=dev -jar learn-service\target\learn-service-0.0.1-SNAPSHOT.jar"
start "Practice-Service:8084" cmd /k "cd /d c:\Users\hp\Desktop\paisewise-backend && java -Dspring.profiles.active=dev -jar practice-service\target\practice-service-0.0.1-SNAPSHOT.jar"
start "Portfolio-Service:8085" cmd /k "cd /d c:\Users\hp\Desktop\paisewise-backend && java -Dspring.profiles.active=dev -jar portfolio-service\target\portfolio-service-0.0.1-SNAPSHOT.jar"
start "Community-Service:8086" cmd /k "cd /d c:\Users\hp\Desktop\paisewise-backend && java -Dspring.profiles.active=dev -jar community-service\target\community-service-0.0.1-SNAPSHOT.jar"
start "Market-Data-Service:8087" cmd /k "cd /d c:\Users\hp\Desktop\paisewise-backend && java -Dspring.profiles.active=dev -jar market-data-service\target\market-data-service-0.0.1-SNAPSHOT.jar"

echo.
echo ========================================================
echo   Backend is UP and ready!
echo   API Gateway URL: http://localhost:8080
echo   Eureka Registry: http://localhost:8761
echo ========================================================
