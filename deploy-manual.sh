#!/bin/bash

# ====================================
# Autocoin API Manual Deployment Script
# EC2에서 직접 실행용
# ====================================

set -e

echo "🚀 Starting manual deployment..."

# 설정
DEPLOYMENT_DIR="/home/ec2-user/autocoin-deployment"
GITHUB_TOKEN="ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"  # GitHub Token 설정 필요
IMAGE_NAME="ghcr.io/autocoinproject/autocoin-api:latest"

# 배포 디렉토리 생성
mkdir -p "$DEPLOYMENT_DIR"
mkdir -p /home/ec2-user/autocoin-logs
mkdir -p /home/ec2-user/autocoin-temp
cd "$DEPLOYMENT_DIR"

echo "📁 Working directory: $(pwd)"

# docker-compose 파일 다운로드
echo "📥 Downloading docker-compose.prod.yml..."
curl -o docker-compose.prod.yml https://raw.githubusercontent.com/AutocoinProject/autocoin_spring_apis/main/docker-compose.prod.yml

# .env 파일 생성 (환경변수 설정 필요)
echo "⚙️ Creating .env file..."
cat > .env << EOF
# Database
DB_HOST=your-rds-host
DB_PASSWORD=your-db-password
DB_USERNAME=admin
DB_NAME=autocoin
DB_PORT=3306

# Security
JWT_SECRET=your-jwt-secret
UPBIT_ENCRYPTION_KEY=your-upbit-key

# AWS
AWS_ACCESS_KEY=your-aws-access-key
AWS_SECRET_KEY=your-aws-secret-key
AWS_S3_BUCKET=your-s3-bucket

# APIs (optional)
SERP_API_KEY=your-serp-key
SLACK_WEBHOOK_URL=your-slack-webhook
SENTRY_DSN=your-sentry-dsn
EOF

echo "⚠️  Please edit .env file with actual values:"
echo "   nano .env"
echo ""
echo "Press Enter after editing .env file..."
read -r

# GitHub Container Registry 로그인
echo "🔐 Logging into GitHub Container Registry..."
echo "$GITHUB_TOKEN" | docker login ghcr.io -u autocoinproject --password-stdin

# 최신 이미지 pull
echo "📦 Pulling latest image..."
docker pull "$IMAGE_NAME"

# 기존 컨테이너 중지
echo "🛑 Stopping existing containers..."
docker-compose -f docker-compose.prod.yml down || true

# 새 컨테이너 시작
echo "🚀 Starting new containers..."
docker-compose -f docker-compose.prod.yml up -d autocoin-api

# 컨테이너 시작 대기
echo "⏳ Waiting for application to start..."
sleep 45

# 헬스체크
echo "🔍 Health check..."
for i in {1..3}; do
  echo "Health check attempt $i/3..."
  if curl -f http://localhost:8080/actuator/health; then
    echo "✅ Health check passed!"
    break
  elif [ $i -eq 3 ]; then
    echo "❌ Health check failed after 3 attempts"
    echo "📋 Application logs:"
    docker-compose -f docker-compose.prod.yml logs autocoin-api --tail 50
    exit 1
  else
    echo "Retrying in 15 seconds..."
    sleep 15
  fi
done

# 컨테이너 상태 확인
echo "📊 Container status:"
docker-compose -f docker-compose.prod.yml ps

# 이미지 정리
echo "🧹 Cleaning up unused images..."
docker image prune -f

echo "🎉 Deployment completed successfully!"
echo "🌐 Application URL: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080"
echo "🔍 Health check: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080/actuator/health"
