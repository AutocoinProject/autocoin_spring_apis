#!/bin/bash

# ====================================
# EC2 Docker 배포용 간단한 스크립트
# ====================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Starting EC2 Docker deployment...${NC}"

# Configuration
APP_NAME="autocoin-api"
CONTAINER_NAME="autocoin-api-prod"
COMPOSE_FILE="docker/docker-compose.ec2.yml"
ENV_FILE=".env.prod"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check if environment file exists
if [ ! -f "$ENV_FILE" ]; then
    log_error "Environment file $ENV_FILE not found!"
    exit 1
fi

# Source environment variables
source $ENV_FILE

# Create necessary directories
log_info "📁 Creating directories..."
mkdir -p logs backups

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    log_error "Docker is not running or not accessible"
    exit 1
fi

# Login to GitHub Container Registry if credentials are provided
if [ ! -z "$GITHUB_TOKEN" ] && [ ! -z "$GITHUB_USERNAME" ]; then
    log_info "🔑 Logging into GitHub Container Registry..."
    echo "$GITHUB_TOKEN" | docker login ghcr.io -u "$GITHUB_USERNAME" --password-stdin
fi

# Pull latest image
if [ ! -z "$DOCKER_IMAGE" ]; then
    log_info "📦 Pulling Docker image: $DOCKER_IMAGE"
    docker pull "$DOCKER_IMAGE"
fi

# Backup database if MySQL container exists
if docker ps -q -f name=autocoin-mysql-prod | grep -q .; then
    log_info "💾 Creating database backup..."
    BACKUP_FILE="backups/autocoin_backup_$(date +%Y%m%d_%H%M%S).sql"
    docker exec autocoin-mysql-prod mysqladump \
        -u root -p"$MYSQL_ROOT_PASSWORD" \
        --single-transaction \
        --routines \
        --triggers \
        "$DB_NAME" > "$BACKUP_FILE" 2>/dev/null || log_warning "Database backup failed"
    
    # Clean old backups (keep last 7 days)
    find backups -name "*.sql" -mtime +7 -delete 2>/dev/null || true
fi

# Stop and remove existing containers
log_info "🔄 Managing existing containers..."

# Check if using docker-compose
if [ -f "$COMPOSE_FILE" ]; then
    log_info "Using Docker Compose for deployment..."
    
    # Stop existing services
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down || true
    
    # Start services
    log_info "🚀 Starting services with Docker Compose..."
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
else
    log_info "Using single container deployment..."
    
    # Stop existing container
    if docker ps -q -f name="$CONTAINER_NAME" | grep -q .; then
        log_info "Stopping existing container..."
        docker stop "$CONTAINER_NAME"
    fi
    
    # Remove existing container
    if docker ps -aq -f name="$CONTAINER_NAME" | grep -q .; then
        log_info "Removing existing container..."
        docker rm "$CONTAINER_NAME"
    fi
    
    # Start new container
    log_info "🚀 Starting new container..."
    docker run -d \
        --name "$CONTAINER_NAME" \
        --restart unless-stopped \
        -p 8080:8080 \
        --env-file "$ENV_FILE" \
        -v "$(pwd)/logs:/app/logs" \
        "${DOCKER_IMAGE:-autocoin-api:latest}"
fi

# Wait for application to start
log_info "⏳ Waiting for application to start..."
RETRY_COUNT=0
MAX_RETRIES=30

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        log_success "✅ Application started successfully!"
        break
    fi
    
    echo "Waiting for application... ($((RETRY_COUNT + 1))/$MAX_RETRIES)"
    sleep 5
    RETRY_COUNT=$((RETRY_COUNT + 1))
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    log_error "❌ Application failed to start within timeout"
    log_info "📋 Container logs:"
    docker logs "$CONTAINER_NAME" --tail 20
    exit 1
fi

# Verify deployment
log_info "🔍 Verifying deployment..."

# Health check
HEALTH_STATUS=$(curl -sf http://localhost:8080/actuator/health | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
if [ "$HEALTH_STATUS" = "UP" ]; then
    log_success "✅ Health check passed"
else
    log_error "❌ Health check failed: $HEALTH_STATUS"
    exit 1
fi

# Container status
if docker ps -f name="$CONTAINER_NAME" --format "table {{.Names}}\t{{.Status}}" | grep -q "Up"; then
    log_success "✅ Container is running"
else
    log_error "❌ Container is not running"
    exit 1
fi

# Clean up old images
log_info "🧹 Cleaning up old images..."
docker image prune -f || true

# Display deployment info
log_success "🎉 Deployment completed successfully!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Deployment Summary:"
echo "  • Application: $APP_NAME"
echo "  • Container: $CONTAINER_NAME"
echo "  • Image: ${DOCKER_IMAGE:-'local build'}"
echo "  • Health: http://localhost:8080/actuator/health"
echo "  • Time: $(date)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Show running containers
echo ""
log_info "📋 Running containers:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
log_success "✅ EC2 deployment completed!"
