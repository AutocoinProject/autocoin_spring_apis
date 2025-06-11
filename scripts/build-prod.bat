@echo off
echo ====================================
echo Building Autocoin for Production
echo ====================================

echo.
echo [1/3] Copying production environment file...
copy .env .env.prod.backup
echo Production environment file backed up

echo.
echo [2/3] Running tests...
./gradlew test
if %errorlevel% neq 0 (
    echo ERROR: Tests failed
    pause
    exit /b 1
)

echo.
echo [3/3] Building production JAR...
./gradlew clean build -x test
if %errorlevel% neq 0 (
    echo ERROR: Build failed
    pause
    exit /b 1
)

echo.
echo =========================================
echo   Production Build Complete!
echo =========================================
echo   JAR Location: build/libs/
echo   Environment:  Production (.env)
echo   Docker:       Redis container needed
echo =========================================
echo.
echo To deploy:
echo 1. Copy build/libs/*.jar to server
echo 2. Start Redis: docker run -d -p 6379:6379 redis:7-alpine
echo 3. Run: java -jar autocoin-spring-api.jar
echo.
pause
