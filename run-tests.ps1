Write-Host "🧪 Starting Gradle Test..." -ForegroundColor Cyan

# Set working directory
Set-Location "C:\DEV\autocoin\autocoin_spring_apis"

# Clean and test
Write-Host "🧹 Cleaning project..." -ForegroundColor Yellow
& ".\gradlew.bat" clean

Write-Host "🧪 Running tests..." -ForegroundColor Yellow
& ".\gradlew.bat" test --info --continue

# Check result
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ All tests passed!" -ForegroundColor Green
} else {
    Write-Host "❌ Some tests failed!" -ForegroundColor Red
    Write-Host "📋 Check test results in build/reports/tests/test/index.html" -ForegroundColor Yellow
}

Write-Host "🏁 Test execution completed!" -ForegroundColor Cyan
