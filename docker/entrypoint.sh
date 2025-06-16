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

# Wait for RDS database
echo "⏳ Waiting for RDS database..."
DB_HOST=${DB_HOST:-autocoin-rds.cz7dv3rknd8e.ap-northeast-2.rds.amazonaws.com}
DB_PORT=${DB_PORT:-3306}

# RDS 연결 확인 (최대 60초 대기)
RETRY_COUNT=0
MAX_RETRIES=20

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if timeout 5 bash -c "</dev/tcp/$DB_HOST/$DB_PORT" 2>/dev/null; then
        echo "✅ RDS connection successful ($DB_HOST:$DB_PORT)"
        break
    fi
    
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "Waiting for RDS connection... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 3
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "❌ Failed to connect to RDS after $MAX_RETRIES attempts"
    exit 1
fi

# Redis는 선택사항 (환경변수가 있으면 연결 확인)
if [ -n "$REDIS_HOST" ]; then
    echo "⏳ Waiting for Redis..."
    REDIS_PORT=${REDIS_PORT:-6379}
    
    REDIS_RETRY_COUNT=0
    REDIS_MAX_RETRIES=10
    
    while [ $REDIS_RETRY_COUNT -lt $REDIS_MAX_RETRIES ]; do
        if timeout 3 bash -c "</dev/tcp/$REDIS_HOST/$REDIS_PORT" 2>/dev/null; then
            echo "✅ Redis connection successful ($REDIS_HOST:$REDIS_PORT)"
            break
        fi
        
        REDIS_RETRY_COUNT=$((REDIS_RETRY_COUNT + 1))
        echo "Waiting for Redis connection... ($REDIS_RETRY_COUNT/$REDIS_MAX_RETRIES)"
        sleep 2
    done
    
    if [ $REDIS_RETRY_COUNT -eq $REDIS_MAX_RETRIES ]; then
        echo "⚠️ Redis connection failed, continuing without Redis..."
    fi
else
    echo "⚠️ Redis not configured, skipping..."
fi

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
