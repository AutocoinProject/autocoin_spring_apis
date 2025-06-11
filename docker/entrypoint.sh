#!/bin/bash

# ====================================
# Autocoin API Production Entrypoint
# ====================================

set -e

echo "🚀 Starting Autocoin API in Production mode..."

# Environment validation
required_vars=(
    "DB_PASSWORD"
    "JWT_SECRET"
    "UPBIT_ENCRYPTION_KEY"
)

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "❌ Error: Required environment variable $var is not set"
        exit 1
    fi
done

echo "✅ Environment validation passed"

# Wait for dependencies
echo "⏳ Waiting for database..."
while ! curl -s mysql:3306 > /dev/null; do
    sleep 2
done

echo "⏳ Waiting for Redis..."
while ! curl -s redis:6379 > /dev/null; do
    sleep 2
done

echo "✅ Dependencies are ready"

# Create log directory
mkdir -p /app/logs

# JVM Memory settings based on container limits
CONTAINER_MEMORY=${CONTAINER_MEMORY:-2048}
if [ "$CONTAINER_MEMORY" -le 1024 ]; then
    export JAVA_OPTS="-Xms512m -Xmx1g $JAVA_OPTS"
elif [ "$CONTAINER_MEMORY" -le 2048 ]; then
    export JAVA_OPTS="-Xms1g -Xmx1536m $JAVA_OPTS"
else
    export JAVA_OPTS="-Xms1g -Xmx2g $JAVA_OPTS"
fi

echo "🔧 JVM Settings: $JAVA_OPTS"

# Start application
echo "🚀 Starting Autocoin API..."
exec java $JAVA_OPTS -jar app.jar \
    --spring.profiles.active=prod \
    --logging.file.name=/app/logs/autocoin.log \
    --management.endpoints.web.exposure.include=health,info,metrics,prometheus \
    --management.endpoint.health.show-details=when-authorized
