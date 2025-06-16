#!/bin/bash

echo "🔨 Autocoin API 빌드 중..."

./gradlew clean build -x test

echo "✅ 빌드 완료!"
