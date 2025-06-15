#!/bin/bash

# EC2에서 Autocoin Spring API 업데이트 배포 스크립트

echo "=== Autocoin Spring API 업데이트 배포 시작 ==="

# 1. 현재 실행 중인 컨테이너 확인
echo "현재 실행 중인 컨테이너 상태:"
docker-compose -f docker/docker-compose.ec2-fixed.yml ps

# 2. Git에서 최신 코드 가져오기
echo "Git에서 최신 코드 가져오는 중..."
git stash  # 로컬 변경사항 임시 저장
git pull origin main

# 3. 환경변수 재설정 (기존 설정 유지)
if [ -f "scripts/set-env-ec2.sh" ]; then
    echo "환경변수 재설정 중..."
    source scripts/set-env-ec2.sh
else
    echo "⚠️  경고: scripts/set-env-ec2.sh 파일이 없습니다."
    echo "환경변수를 수동으로 설정해주세요."
fi

# 4. .env.ec2 파일 확인
if [ -f ".env.ec2" ]; then
    echo "Production 환경 설정 파일 적용 중..."
    cp .env.ec2 .env
else
    echo "⚠️  경고: .env.ec2 파일이 없습니다."
    echo "템플릿에서 복사해서 설정해주세요: cp .env.ec2.template .env.ec2"
fi

# 5. 기존 컨테이너 중지
echo "기존 컨테이너 중지 중..."
docker-compose -f docker/docker-compose.ec2-fixed.yml down

# 6. Docker 이미지 새로 빌드 및 실행
echo "새 코드로 Docker 이미지 빌드 및 실행 중..."
docker-compose -f docker/docker-compose.ec2-fixed.yml up --build -d

# 7. 잠시 대기 후 상태 확인
echo "배포 완료 대기 중..."
sleep 15

# 8. 컨테이너 상태 확인
echo "=== 배포 후 상태 확인 ==="
docker-compose -f docker/docker-compose.ec2-fixed.yml ps

# 9. 헬스체크
echo "=== 헬스체크 ==="
sleep 5
curl -f http://localhost:8080/actuator/health || echo "❌ 헬스체크 실패"

# 10. 최근 로그 확인
echo "=== 최근 로그 확인 ==="
docker-compose -f docker/docker-compose.ec2-fixed.yml logs --tail=20 autocoin-api

echo "=== 배포 완료 ==="
echo "✅ API URL: http://localhost:8080"
echo "✅ Health Check: http://localhost:8080/actuator/health"
echo "📋 전체 로그 확인: docker-compose -f docker/docker-compose.ec2-fixed.yml logs autocoin-api"
