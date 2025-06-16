#!/bin/bash
set -e

echo "🚀 Autocoin API 배포 중..."

# 환경변수 확인
if [ -z "$DB_PASSWORD" ] || [ -z "$JWT_SECRET" ]; then
    echo "❌ 필수 환경변수가 설정되지 않았습니다!"
    echo "필요한 변수: DB_PASSWORD, JWT_SECRET"
    exit 1
fi

# 기존 컨테이너 중지
echo "🛑 기존 컨테이너 중지 중..."
docker-compose down || true

# 새 컨테이너 시작
echo "🐳 새 컨테이너 시작 중..."
docker-compose up --build -d

# 헬스체크
echo "⏳ 애플리케이션 시작 대기 중..."
sleep 20

if curl -f -s http://localhost:8080/actuator/health > /dev/null; then
    echo "✅ 배포 성공!"
    echo "🌐 API: http://localhost:8080"
else
    echo "❌ 헬스체크 실패"
    docker-compose logs
fi
