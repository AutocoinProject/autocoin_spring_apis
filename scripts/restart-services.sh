#!/bin/bash

# ====================================
# Autocoin Spring API 서비스 재시작 스크립트
# ====================================

echo "=== Autocoin Spring API 서비스 재시작 ==="
echo "시간: $(date)"
echo ""

# 0. 환경변수 설정
echo "0. 환경변수 파일 설정"
if [ -f ".env.ec2" ]; then
    echo "✅ .env.ec2를 .env로 복사"
    cp .env.ec2 .env
    echo "환경변수 로드 확인:"
    source .env
    echo "DB_PASSWORD 설정: ${DB_PASSWORD:0:3}***"
    echo "JWT_SECRET 설정: ${JWT_SECRET:0:10}***"
else
    echo "❌ .env.ec2 파일이 없습니다!"
    exit 1
fi
echo ""

# 1. 기존 컨테이너 정리
echo "1. 기존 컨테이너 및 이미지 정리"
echo "컨테이너 중지 및 제거..."
sudo docker-compose -f docker/docker-compose.prod.yml down --remove-orphans

echo "미사용 리소스 정리..."
sudo docker system prune -f

echo "미사용 볼륨 정리 (선택적)..."
# sudo docker volume prune -f  # 주석 처리: 데이터 보존
echo ""

# 2. 네트워크 재생성
echo "2. Docker 네트워크 확인"
sudo docker network ls | grep autocoin || echo "기존 네트워크 없음"
echo ""

# 3. 서비스 시작
echo "3. 서비스 시작"
echo "docker-compose로 서비스 시작..."
sudo docker-compose -f docker/docker-compose.prod.yml up -d

echo ""
echo "서비스 시작 대기 (30초)..."
sleep 30
echo ""

# 4. 상태 확인
echo "4. 서비스 상태 확인"
echo "실행 중인 컨테이너:"
sudo docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""

echo "서비스 상태:"
sudo docker-compose -f docker/docker-compose.prod.yml ps
echo ""

# 5. 로그 확인
echo "5. 주요 서비스 로그 확인"
echo ""
echo "=== MySQL 로그 ==="
sudo docker logs autocoin-mysql-prod --tail 5 2>/dev/null || echo "MySQL 로그 없음"
echo ""

echo "=== Redis 로그 ==="
sudo docker logs autocoin-redis-prod --tail 5 2>/dev/null || echo "Redis 로그 없음"
echo ""

echo "=== API 서버 로그 ==="
sudo docker logs autocoin-api-prod --tail 10 2>/dev/null || echo "API 서버 로그 없음"
echo ""

# 6. 건강 상태 체크
echo "6. 건강 상태 체크"
echo "API 서버 건강 체크 시도 중..."
for i in {1..6}; do
    if curl -f -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo "✅ API 서버 응답 정상 (시도 $i/6)"
        curl -s http://localhost:8080/actuator/health | jq . 2>/dev/null || curl -s http://localhost:8080/actuator/health
        break
    else
        echo "⏳ API 서버 응답 대기 중... (시도 $i/6)"
        if [ $i -eq 6 ]; then
            echo "❌ API 서버 응답 없음 - 로그를 확인하세요"
            echo "로그 확인 명령어: sudo docker logs autocoin-api-prod -f"
        else
            sleep 10
        fi
    fi
done
echo ""

# 7. 포트 확인
echo "7. 포트 확인"
echo "포트 8080 (API): $(sudo netstat -tlnp | grep :8080 | head -1 || echo '사용되지 않음')"
echo "포트 80 (Nginx): $(sudo netstat -tlnp | grep :80 | head -1 || echo '사용되지 않음')"
echo "포트 3306 (MySQL): $(sudo netstat -tlnp | grep :3306 | head -1 || echo '사용되지 않음')"
echo ""

echo "=== 재시작 완료 ==="
echo ""
echo "다음 명령어로 상태를 계속 모니터링할 수 있습니다:"
echo "- 전체 로그: sudo docker-compose -f docker/docker-compose.prod.yml logs -f"
echo "- API 로그만: sudo docker logs autocoin-api-prod -f"
echo "- 상태 체크: curl http://localhost:8080/actuator/health"
echo "- 컨테이너 상태: sudo docker ps"
