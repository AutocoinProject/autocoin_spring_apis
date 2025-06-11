@echo off
echo 🧪 Starting Gradle Test...

cd /d "C:\DEV\autocoin\autocoin_spring_apis"

echo 🧹 Cleaning project...
gradlew.bat clean

echo 🧪 Running tests...
gradlew.bat test --continue

if %ERRORLEVEL% equ 0 (
    echo ✅ All tests passed!
) else (
    echo ❌ Some tests failed!
    echo 📋 Check test results in build/reports/tests/test/index.html
)

echo 🏁 Test execution completed!
pause
