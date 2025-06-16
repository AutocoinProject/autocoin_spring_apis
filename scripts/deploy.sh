#!/bin/bash

# ====================================
# Autocoin API Production Deployment Script
# ====================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DEPLOY_USER=${DEPLOY_USER:-deploy}
DEPLOY_HOST=${DEPLOY_HOST:-your_server_ip}
DEPLOY_PATH=${DEPLOY_PATH:-/opt/autocoin}
APP_NAME="autocoin-api"
BACKUP_RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-30}

echo -e "${BLUE}🚀 Starting Autocoin API Production Deployment${NC}"

# Check if .env.prod exists
if [ ! -f ".env.prod" ]; then
    echo -e "${RED}❌ Error: .env.prod file not found${NC}"
    echo -e "${YELLOW}💡 Copy .env.prod.example to .env.prod and configure it${NC}"
    exit 1
fi

# Load environment variables
source .env.prod

# Validate required variables
required_vars=(
    "DB_PASSWORD"
    "JWT_SECRET"
    "UPBIT_ENCRYPTION_KEY"
    "DEPLOY_HOST"
)

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo -e "${RED}❌ Error: Required environment variable $var is not set in .env.prod${NC}"
        exit 1
    fi
done

echo -e "${GREEN}✅ Environment validation passed${NC}"

# Functions
backup_database() {
    echo -e "${BLUE}📦 Creating database backup...${NC}"
    
    BACKUP_FILE="autocoin_backup_$(date +%Y%m%d_%H%M%S).sql"
    
    ssh $DEPLOY_USER@$DEPLOY_HOST "
        docker exec autocoin-mysql-prod mysqldump \
            -u root -p$MYSQL_ROOT_PASSWORD \
            --single-transaction \
            --routines \
            --triggers \
            $DB_NAME > $DEPLOY_PATH/backups/$BACKUP_FILE
    "
    
    echo -e "${GREEN}✅ Database backup created: $BACKUP_FILE${NC}"
}

cleanup_old_backups() {
    echo -e "${BLUE}🧹 Cleaning up old backups...${NC}"
    
    ssh $DEPLOY_USER@$DEPLOY_HOST "
        find $DEPLOY_PATH/backups -name '*.sql' -mtime +$BACKUP_RETENTION_DAYS -delete
        find $DEPLOY_PATH/backups -name '*.tar.gz' -mtime +$BACKUP_RETENTION_DAYS -delete
    "
    
    echo -e "${GREEN}✅ Old backups cleaned up${NC}"
}

build_application() {
    echo -e "${BLUE}🔨 Building application...${NC}"
    
    # Clean and build
    ./gradlew clean build -x test
    
    echo -e "${GREEN}✅ Application built successfully${NC}"
}

upload_files() {
    echo -e "${BLUE}📤 Uploading files to server...${NC}"
    
    # Create remote directories
    ssh $DEPLOY_USER@$DEPLOY_HOST "
        sudo mkdir -p $DEPLOY_PATH/{backups,logs,ssl}
        sudo chown -R $DEPLOY_USER:$DEPLOY_USER $DEPLOY_PATH
    "
    
    # Upload application files
    rsync -avz --delete \
        --exclude '.git' \
        --exclude 'build' \
        --exclude 'logs' \
        --exclude '.gradle' \
        --exclude 'node_modules' \
        . $DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_PATH/
    
    # Upload environment file
    scp .env.prod $DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_PATH/
    
    echo -e "${GREEN}✅ Files uploaded successfully${NC}"
}

deploy_application() {
    echo -e "${BLUE}🚀 Deploying application...${NC}"
    
    ssh $DEPLOY_USER@$DEPLOY_HOST "
        cd $DEPLOY_PATH
        
        # Docker 이미지가 설정된 경우 GitHub Container Registry에서 pull
        if [ -n \"\${DOCKER_IMAGE}\" ]; then
            echo 'Using Docker image from GitHub Container Registry: '\$DOCKER_IMAGE
            
            # GitHub Container Registry 로그인 (필요시)
            if [ -n \"\${GITHUB_TOKEN}\" ]; then
                echo \$GITHUB_TOKEN | docker login ghcr.io -u \$GITHUB_USERNAME --password-stdin
            fi
            
            # 이미지 pull
            docker pull \$DOCKER_IMAGE
            
            # docker-compose.yml에서 이미지 태그 업데이트
            sed -i 's|image: ghcr.io/.*|image: '\$DOCKER_IMAGE'|' docker/docker-compose.prod.yml
        else
            echo 'Building Docker image locally...'
            # 로컬 빌드 (기존 방식)
            docker-compose -f docker/docker-compose.prod.yml --env-file .env.prod build
        fi
        
        # 기존 컨테이너 graceful shutdown
        echo 'Stopping existing containers...'
        docker-compose -f docker/docker-compose.prod.yml --env-file .env.prod down --timeout 30
        
        # 새 컨테이너 시작
        echo 'Starting new containers...'
        docker-compose -f docker/docker-compose.prod.yml --env-file .env.prod up -d
        
        # 애플리케이션 시작 대기
        echo 'Waiting for application to start...'
        timeout 120 bash -c 'until curl -sf http://localhost:8080/actuator/health; do sleep 5; done'
    "
    
    echo -e "${GREEN}✅ Application deployed successfully${NC}"
}

verify_deployment() {
    echo -e "${BLUE}🔍 Verifying deployment...${NC}"
    
    # Check if containers are running
    RUNNING_CONTAINERS=$(ssh $DEPLOY_USER@$DEPLOY_HOST "docker ps --format 'table {{.Names}}\t{{.Status}}' | grep autocoin")
    
    echo "Running containers:"
    echo "$RUNNING_CONTAINERS"
    
    # Health check
    HEALTH_STATUS=$(ssh $DEPLOY_USER@$DEPLOY_HOST "curl -sf http://localhost:8080/actuator/health | jq -r '.status'")
    
    if [ "$HEALTH_STATUS" = "UP" ]; then
        echo -e "${GREEN}✅ Application health check passed${NC}"
    else
        echo -e "${RED}❌ Application health check failed${NC}"
        return 1
    fi
    
    # Check logs for errors
    echo -e "${BLUE}📋 Recent application logs:${NC}"
    ssh $DEPLOY_USER@$DEPLOY_HOST "docker logs autocoin-api-prod --tail 10"
}

rollback() {
    echo -e "${YELLOW}⏪ Rolling back deployment...${NC}"
    
    ssh $DEPLOY_USER@$DEPLOY_HOST "
        cd $DEPLOY_PATH
        
        # Get previous image
        PREVIOUS_IMAGE=\$(docker images autocoin-api --format '{{.Repository}}:{{.Tag}}' | sed -n '2p')
        
        if [ -n \"\$PREVIOUS_IMAGE\" ]; then
            # Stop current containers
            docker-compose -f docker/docker-compose.prod.yml --env-file .env.prod down
            
            # Start with previous image
            docker-compose -f docker/docker-compose.prod.yml --env-file .env.prod up -d
            
            echo 'Rollback completed'
        else
            echo 'No previous image found for rollback'
            exit 1
        fi
    "
}

show_status() {
    echo -e "${BLUE}📊 Deployment Status${NC}"
    echo "================================"
    
    ssh $DEPLOY_USER@$DEPLOY_HOST "
        echo 'Container Status:'
        docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep autocoin
        
        echo ''
        echo 'Resource Usage:'
        docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}'
        
        echo ''
        echo 'Disk Usage:'
        df -h $DEPLOY_PATH
        
        echo ''
        echo 'Application URLs:'
        echo 'API: https://$API_DOMAIN'
        echo 'Monitoring: https://$MONITORING_DOMAIN'
    "
}

# Main deployment flow
case "${1:-deploy}" in
    "deploy")
        echo -e "${BLUE}🔄 Full deployment process${NC}"
        build_application
        backup_database
        upload_files
        deploy_application
        verify_deployment
        cleanup_old_backups
        show_status
        echo -e "${GREEN}🎉 Deployment completed successfully!${NC}"
        ;;
    
    "rollback")
        echo -e "${YELLOW}⏪ Rolling back deployment${NC}"
        rollback
        verify_deployment
        echo -e "${GREEN}✅ Rollback completed${NC}"
        ;;
    
    "status")
        show_status
        ;;
    
    "backup")
        backup_database
        cleanup_old_backups
        ;;
    
    "logs")
        ssh $DEPLOY_USER@$DEPLOY_HOST "docker logs autocoin-api-prod -f"
        ;;
    
    *)
        echo "Usage: $0 {deploy|rollback|status|backup|logs}"
        echo ""
        echo "Commands:"
        echo "  deploy   - Full deployment (build, backup, upload, deploy)"
        echo "  rollback - Rollback to previous version"
        echo "  status   - Show deployment status"
        echo "  backup   - Create database backup only"
        echo "  logs     - Show application logs"
        exit 1
        ;;
esac
