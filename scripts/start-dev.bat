@echo off
echo ====================================
echo Starting Autocoin Dev Environment
echo ====================================

echo.
echo [1/4] Copying dev environment file...
copy .env.dev .env
if %errorlevel% neq 0 (
    echo ERROR: Failed to copy .env.dev file
    pause
    exit /b 1
)

echo.
echo [2/4] Starting Redis container...
docker-compose up -d redis
if %errorlevel% neq 0 (
    echo ERROR: Failed to start Redis container
    pause
    exit /b 1
)

echo.
echo [3/4] Waiting for Redis to be ready...
timeout /t 5 /nobreak > nul

echo.
echo [4/4] Starting Spring Boot application with RDS connection...
echo.
echo =========================================
echo   Dev Environment Ready!
echo =========================================
echo   MySQL:    AWS RDS (autocoin-dev-mysql)
echo   Redis:    localhost:6379 (Docker)
echo   S3:       AWS S3 (autocoin-dev-storage)
echo   API:      http://localhost:8080
echo   Swagger:  http://localhost:8080/swagger-ui.html
echo   Actuator: http://localhost:8080/actuator
echo =========================================
echo.

./gradlew bootRun --args='--spring.profiles.active=dev'
