#!/bin/bash
echo "🔨 Building Autocoin API..."
./gradlew clean build -x test
echo "✅ Build completed!"
