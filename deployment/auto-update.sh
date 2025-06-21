#!/bin/bash

# ====================================
# EC2 Auto Update Script
# GitHub Actions 없이 EC2에서 직접 실행
# ====================================

set -e

echo "🔄 Auto-update started at $(date)"

# 환경변수 설정
GITHUB_REGISTRY="ghcr.io"
IMAGE_NAME="autocoinproject/autocoin-api"
TAG="latest"
CONTAINER_NAME="autocoin-api"
DEPLOYMENT_DIR="/home/ec2-user/autocoin-deployment"

# 배포 디렉토리로 이동
cd "$DEPLOYMENT_DIR" || {
    echo "❌ Deployment directory not found: $DEPLOYMENT_DIR"
    exit 1
}

# GitHub Token이 설정되어 있는지 확인
if [ -z "$GITHUB_TOKEN" ]; then
    echo "❌ GITHUB_TOKEN environment variable is not set"
    echo "Please set it in /home/ec2-user/.bashrc or /etc/environment"
    exit 1
fi

echo "🔍 Checking for new image updates..."

# 현재 실행 중인 이미지 ID 확인
CURRENT_IMAGE_ID=$(docker inspect "$CONTAINER_NAME" --format='{{.Image}}' 2>/dev/null || echo "none")

# GitHub Container Registry 로그인
echo "$GITHUB_TOKEN" | docker login ghcr.io -u autocoinproject --password-stdin

# 최신 이미지 pull
docker pull "$GITHUB_REGISTRY/$IMAGE_NAME:$TAG"

# 새로 pull한 이미지 ID 확인
NEW_IMAGE_ID=$(docker inspect "$GITHUB_REGISTRY/$IMAGE_NAME:$TAG" --format='{{.Id}}')

echo "Current Image ID: $CURRENT_IMAGE_ID"
echo "New Image ID: $NEW_IMAGE_ID"

# 이미지가 업데이트되었는지 확인
if [ "$CURRENT_IMAGE_ID" = "$NEW_IMAGE_ID" ]; then
    echo "✅ No updates available. Current image is already the latest."
    exit 0
fi

echo "🚀 New image detected! Starting deployment..."

# 기존 컨테이너 중지 및 제거
docker stop "$CONTAINER_NAME" || true
docker rm "$CONTAINER_NAME" || true

# docker-compose 파일이 있으면 사용, 없으면 직접 실행
if [ -f "docker-compose.prod.yml" ]; then
    echo "Using docker-compose for deployment..."
    docker-compose -f docker-compose.prod.yml up -d autocoin-api
else
    echo "Using direct docker run for deployment..."
    docker run -d \
        --name "$CONTAINER_NAME" \
        --restart unless-stopped \
        -p 8080:8080 \
        --env-file .env \
        -v /home/ec2-user/autocoin-logs:/app/logs \
        "$GITHUB_REGISTRY/$IMAGE_NAME:$TAG"
fi

# 헬스체크
echo "⏳ Waiting for application to start..."
sleep 45

for i in {1..3}; do
    echo "Health check attempt $i/3..."
    if curl -f http://localhost:8080/actuator/health; then
        echo "✅ Health check passed!"
        break
    elif [ $i -eq 3 ]; then
        echo "❌ Health check failed after 3 attempts"
        docker logs "$CONTAINER_NAME" --tail 50
        exit 1
    else
        echo "Retrying in 15 seconds..."
        sleep 15
    fi
done

# 사용하지 않는 이미지 정리
docker image prune -f

echo "🎉 Auto-update completed successfully at $(date)"

# 선택사항: Slack 알림
if [ -n "$SLACK_WEBHOOK_URL" ]; then
    curl -X POST -H 'Content-type: application/json' \
        --data "{\"text\":\"🚀 Autocoin API successfully updated to latest version on $(date)\"}" \
        "$SLACK_WEBHOOK_URL"
fi
