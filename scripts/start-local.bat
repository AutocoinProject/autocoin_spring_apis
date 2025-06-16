@echo off
echo ====================================
echo Starting Autocoin Local Environment
echo ====================================

echo.
echo [1/4] Copying local environment file...
if exist .env.local (
    copy .env.local .env
    echo Local environment file copied successfully
) else (
    echo ERROR: .env.local file not found
    pause
    exit /b 1
)

echo.
echo [2/4] Starting Docker containers...
docker-compose up -d mysql redis
if %errorlevel% neq 0 (
    echo ERROR: Failed to start Docker containers
    echo Make sure Docker Desktop is running
    pause
    exit /b 1
)

echo.
echo [3/4] Waiting for services to be ready...
timeout /t 10 /nobreak > nul

echo.
echo [4/4] Environment setup complete!
echo.
echo =========================================
echo   Local Environment Ready!
echo =========================================
echo   MySQL:    localhost:3306 (autocoin_db)
echo   Redis:    localhost:6379
echo   S3:       AWS S3 (autocoin-local-storage)
echo   API:      http://localhost:8080
echo   Swagger:  http://localhost:8080/swagger-ui.html
echo   Actuator: http://localhost:8080/actuator
echo =========================================
echo.
echo Now run the application from IntelliJ with:
echo   - Active profiles: local
echo   - Or run: ./gradlew bootRun --args='--spring.profiles.active=local'
echo.
pause
