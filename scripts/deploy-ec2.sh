#!/bin/bash

# EC2에서 Autocoin Spring API 실행 스크립트

echo "=== Autocoin Spring API EC2 배포 시작 ==="

# 1. 현재 컨테이너 정리
echo "기존 컨테이너 정리 중..."
docker-compose -f docker/docker-compose.ec2-fixed.yml down --remove-orphans

# 2. 환경변수 설정
echo "환경변수 설정 중..."
source scripts/set-env-ec2.sh

# 3. .env.ec2 파일을 .env로 복사
echo "Production 환경 설정 파일 적용 중..."
cp .env.ec2 .env

# 4. Docker 이미지 빌드 및 실행
echo "Docker 이미지 빌드 및 실행 중..."
docker-compose -f docker/docker-compose.ec2-fixed.yml up --build -d

# 5. 상태 확인
echo "컨테이너 상태 확인 중..."
sleep 10
docker-compose -f docker/docker-compose.ec2-fixed.yml ps

# 6. 로그 확인
echo "=== 실행 로그 확인 ==="
docker-compose -f docker/docker-compose.ec2-fixed.yml logs autocoin-api

echo "=== 배포 완료 ==="
echo "API URL: http://localhost:8080"
echo "Health Check: http://localhost:8080/actuator/health"
