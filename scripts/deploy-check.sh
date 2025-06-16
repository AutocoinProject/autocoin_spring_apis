#!/bin/bash

# ====================================
# Autocoin Spring API 배포 상태 체크 스크립트
# ====================================

echo "=== Autocoin Spring API 배포 상태 체크 ==="
echo "시간: $(date)"
echo ""

# 1. 환경변수 파일 확인
echo "1. 환경변수 파일 확인"
if [ -f ".env.ec2" ]; then
    echo "✅ .env.ec2 파일 존재"
    echo "주요 환경변수 확인:"
    grep -E "^(DB_PASSWORD|JWT_SECRET|SLACK_WEBHOOK_URL)=" .env.ec2 | head -3
else
    echo "❌ .env.ec2 파일이 없습니다!"
fi
echo ""

# 2. Docker 컨테이너 상태 확인
echo "2. Docker 컨테이너 상태"
echo "실행 중인 컨테이너:"
sudo docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""

echo "모든 컨테이너 (중지된 것 포함):"
sudo docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""

# 3. Docker Compose 서비스 상태
echo "3. Docker Compose 서비스 상태"
if [ -f "docker/docker-compose.prod.yml" ]; then
    echo "서비스 상태:"
    sudo docker-compose -f docker/docker-compose.prod.yml ps
else
    echo "❌ docker-compose.prod.yml 파일이 없습니다!"
fi
echo ""

# 4. 네트워크 확인
echo "4. Docker 네트워크 확인"
sudo docker network ls | grep autocoin
echo ""

# 5. 볼륨 확인
echo "5. Docker 볼륨 확인"
sudo docker volume ls | grep -E "(mysql|redis|elastic)"
echo ""

# 6. 로그 확인 (메인 애플리케이션)
echo "6. 애플리케이션 로그 확인"
if sudo docker ps | grep -q "autocoin-api-prod"; then
    echo "✅ autocoin-api-prod 컨테이너 실행 중"
    echo "최근 로그 (마지막 10줄):"
    sudo docker logs autocoin-api-prod --tail 10
else
    echo "❌ autocoin-api-prod 컨테이너가 실행되지 않음"
    echo "최근 실행 시도 로그:"
    sudo docker logs autocoin-api-prod --tail 20 2>/dev/null || echo "로그 없음"
fi
echo ""

# 7. 포트 확인
echo "7. 포트 사용 현황"
echo "포트 8080 (API):"
sudo netstat -tlnp | grep :8080 || echo "포트 8080 사용되지 않음"
echo "포트 80 (Nginx):"
sudo netstat -tlnp | grep :80 || echo "포트 80 사용되지 않음"
echo "포트 3306 (MySQL):"
sudo netstat -tlnp | grep :3306 || echo "포트 3306 사용되지 않음"
echo ""

# 8. 디스크 사용량
echo "8. 디스크 사용량"
df -h | grep -E "(Filesystem|/dev/)"
echo ""
echo "Docker 이미지 사용량:"
sudo docker system df
echo ""

# 9. 메모리 사용량
echo "9. 메모리 사용량"
free -h
echo ""

# 10. 건강 상태 체크
echo "10. 서비스 건강 상태 체크"
echo "API 건강 체크 (curl 테스트):"
if curl -f -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo "✅ API 서버 응답 정상"
    curl -s http://localhost:8080/actuator/health | jq . 2>/dev/null || curl -s http://localhost:8080/actuator/health
else
    echo "❌ API 서버 응답 없음"
fi
echo ""

echo "=== 체크 완료 ==="
echo ""
echo "문제 해결 제안:"
echo "1. 컨테이너가 실행되지 않는 경우: sudo docker-compose -f docker/docker-compose.prod.yml up -d"
echo "2. 환경변수 문제인 경우: .env.ec2 파일을 .env로 복사 후 재시작"
echo "3. 로그 상세 확인: sudo docker logs autocoin-api-prod -f"
echo "4. 전체 재시작: ./scripts/restart-services.sh"
