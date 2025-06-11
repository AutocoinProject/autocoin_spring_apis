#!/bin/bash

# ====================================
# Autocoin API 배포 후 검증 스크립트
# ====================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
DEPLOY_USER=${DEPLOY_USER:-deploy}
DEPLOY_HOST=${DEPLOY_HOST:-localhost}
API_DOMAIN=${API_DOMAIN:-localhost}
MONITORING_DOMAIN=${MONITORING_DOMAIN:-localhost}
TIMEOUT=${TIMEOUT:-30}

echo -e "${BLUE}🔍 Autocoin API 배포 후 검증 시작${NC}"
echo "======================================"

# Verification results
VERIFICATION_RESULTS=()
FAILED_CHECKS=0
TOTAL_CHECKS=0

# Function to run verification
verify() {
    local test_name="$1"
    local command="$2"
    local expected_result="$3"
    local is_critical="${4:-false}"
    
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    echo -e "${CYAN}🧪 Testing: $test_name${NC}"
    
    if eval "$command"; then
        echo -e "${GREEN}✅ PASS: $test_name${NC}"
        VERIFICATION_RESULTS+=("✅ $test_name")
    else
        echo -e "${RED}❌ FAIL: $test_name${NC}"
        VERIFICATION_RESULTS+=("❌ $test_name")
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        
        if [ "$is_critical" = "true" ]; then
            echo -e "${RED}🚨 Critical check failed! Deployment may have issues.${NC}"
        fi
    fi
    echo ""
}

# ====================================
# 1. 기본 인프라 확인
# ====================================
echo -e "${PURPLE}📋 1. 기본 인프라 확인${NC}"
echo "======================================"

verify "Docker 서비스 실행 중" \
    "systemctl is-active --quiet docker" \
    "active" \
    true

verify "Docker Compose 설치됨" \
    "command -v docker-compose >/dev/null 2>&1" \
    "installed" \
    true

verify "방화벽(UFW) 활성화" \
    "sudo ufw status | grep -q 'Status: active'" \
    "active"

verify "Fail2ban 실행 중" \
    "systemctl is-active --quiet fail2ban" \
    "active"

# ====================================
# 2. Docker 컨테이너 상태 확인
# ====================================
echo -e "${PURPLE}📋 2. Docker 컨테이너 상태 확인${NC}"
echo "======================================"

# 필수 컨테이너 목록
REQUIRED_CONTAINERS=(
    "autocoin-api-prod"
    "autocoin-mysql-prod"
    "autocoin-redis-prod"
    "autocoin-nginx-prod"
    "autocoin-prometheus-prod"
    "autocoin-grafana-prod"
    "autocoin-alertmanager-prod"
    "autocoin-elasticsearch-prod"
    "autocoin-logstash-prod"
)

for container in "${REQUIRED_CONTAINERS[@]}"; do
    verify "컨테이너 $container 실행 중" \
        "docker ps --format '{{.Names}}' | grep -q '^$container\$'" \
        "running" \
        true
done

verify "모든 컨테이너 정상 상태" \
    "[ \$(docker ps -q --filter 'status=exited' | wc -l) -eq 0 ]" \
    "no_exited_containers"

# ====================================
# 3. 네트워크 연결 확인
# ====================================
echo -e "${PURPLE}📋 3. 네트워크 연결 확인${NC}"
echo "======================================"

verify "API 포트 (8080) 응답" \
    "curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1" \
    "responding" \
    true

verify "Nginx 포트 (80) 응답" \
    "curl -sf http://localhost:80 >/dev/null 2>&1 || curl -sf http://localhost >/dev/null 2>&1" \
    "responding"

verify "MySQL 포트 (3306) 연결 가능" \
    "nc -z localhost 3306" \
    "connectable" \
    true

verify "Redis 포트 (6379) 연결 가능" \
    "nc -z localhost 6379" \
    "connectable" \
    true

verify "Prometheus 포트 (9090) 연결 가능" \
    "nc -z localhost 9090" \
    "connectable"

verify "Grafana 포트 (3000) 연결 가능" \
    "nc -z localhost 3000" \
    "connectable"

# ====================================
# 4. API 기능 테스트
# ====================================
echo -e "${PURPLE}📋 4. API 기능 테스트${NC}"
echo "======================================"

# Health Check
verify "API 헬스체크 통과" \
    "curl -sf http://localhost:8080/actuator/health | jq -r '.status' | grep -q 'UP'" \
    "UP" \
    true

# Info endpoint
verify "API 정보 엔드포인트 응답" \
    "curl -sf http://localhost:8080/actuator/info >/dev/null 2>&1" \
    "responding"

# Metrics endpoint
verify "메트릭 엔드포인트 응답" \
    "curl -sf http://localhost:8080/actuator/prometheus >/dev/null 2>&1" \
    "responding"

# API endpoints (인증이 필요하지 않은 것들)
verify "뉴스 API 엔드포인트 응답" \
    "curl -sf http://localhost:8080/api/v1/news >/dev/null 2>&1 || curl -sf http://localhost:8080/api/v1/public/news >/dev/null 2>&1" \
    "responding"

# API 응답 시간 테스트
verify "API 응답 시간 < 2초" \
    "timeout 2 curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1" \
    "fast_response"

# ====================================
# 5. 데이터베이스 연결 테스트
# ====================================
echo -e "${PURPLE}📋 5. 데이터베이스 연결 테스트${NC}"
echo "======================================"

verify "MySQL 컨테이너 내부 연결" \
    "docker exec autocoin-mysql-prod mysqladmin ping -h localhost >/dev/null 2>&1" \
    "connected" \
    true

verify "MySQL 데이터베이스 존재" \
    "docker exec autocoin-mysql-prod mysql -e 'SHOW DATABASES;' | grep -q autocoin" \
    "database_exists" \
    true

verify "Redis 연결 테스트" \
    "docker exec autocoin-redis-prod redis-cli ping | grep -q PONG" \
    "connected" \
    true

# ====================================
# 6. 모니터링 시스템 확인
# ====================================
echo -e "${PURPLE}📋 6. 모니터링 시스템 확인${NC}"
echo "======================================"

verify "Prometheus 타겟 수집 중" \
    "curl -sf http://localhost:9090/api/v1/targets | jq '.data.activeTargets | length' | grep -q '[1-9]'" \
    "collecting_targets"

verify "Grafana 대시보드 접근 가능" \
    "curl -sf http://localhost:3000/api/health >/dev/null 2>&1" \
    "accessible"

verify "AlertManager 실행 중" \
    "curl -sf http://localhost:9093/api/v1/status >/dev/null 2>&1" \
    "running"

verify "Elasticsearch 클러스터 정상" \
    "curl -sf http://localhost:9200/_cluster/health | jq -r '.status' | grep -q -E '(green|yellow)'" \
    "healthy"

verify "Logstash 파이프라인 실행 중" \
    "curl -sf http://localhost:9600/_node/stats >/dev/null 2>&1" \
    "running"

# ====================================
# 7. SSL/TLS 인증서 확인
# ====================================
echo -e "${PURPLE}📋 7. SSL/TLS 인증서 확인${NC}"
echo "======================================"

if [ -f "/opt/autocoin/ssl/autocoin.com.crt" ]; then
    verify "SSL 인증서 파일 존재" \
        "[ -f /opt/autocoin/ssl/autocoin.com.crt ]" \
        "exists"
    
    verify "SSL 인증서 유효성" \
        "openssl x509 -in /opt/autocoin/ssl/autocoin.com.crt -noout -checkend 86400 >/dev/null 2>&1" \
        "valid"
    
    # 인증서 만료일 확인
    CERT_EXPIRY=$(openssl x509 -in /opt/autocoin/ssl/autocoin.com.crt -noout -enddate | cut -d= -f2)
    EXPIRY_SECONDS=$(date -d "$CERT_EXPIRY" +%s 2>/dev/null || echo "0")
    CURRENT_SECONDS=$(date +%s)
    DAYS_LEFT=$(( ($EXPIRY_SECONDS - $CURRENT_SECONDS) / 86400 ))
    
    if [ $DAYS_LEFT -gt 30 ]; then
        verify "SSL 인증서 만료일 (30일 이상 남음)" \
            "[ $DAYS_LEFT -gt 30 ]" \
            "sufficient_time"
    else
        verify "SSL 인증서 만료일 체크" \
            "false" \
            "renewal_needed"
        echo -e "${YELLOW}⚠️  SSL 인증서가 ${DAYS_LEFT}일 후 만료됩니다.${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  SSL 인증서 파일을 찾을 수 없습니다.${NC}"
fi

# ====================================
# 8. 로그 시스템 확인
# ====================================
echo -e "${PURPLE}📋 8. 로그 시스템 확인${NC}"
echo "======================================"

verify "애플리케이션 로그 생성됨" \
    "[ \$(docker logs autocoin-api-prod --tail 10 2>/dev/null | wc -l) -gt 0 ]" \
    "logs_generated"

verify "로그에 에러 없음 (최근 10줄)" \
    "! docker logs autocoin-api-prod --tail 10 2>/dev/null | grep -i 'ERROR\\|FATAL'" \
    "no_recent_errors"

verify "Nginx 로그 생성됨" \
    "[ \$(docker logs autocoin-nginx-prod --tail 5 2>/dev/null | wc -l) -gt 0 ]" \
    "nginx_logs_generated"

# ====================================
# 9. 리소스 사용량 확인
# ====================================
echo -e "${PURPLE}📋 9. 리소스 사용량 확인${NC}"
echo "======================================"

# CPU 사용률 확인
CPU_USAGE=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | awk -F'%' '{print $1}')
verify "CPU 사용률 < 80%" \
    "[ \$(echo \"$CPU_USAGE < 80\" | bc 2>/dev/null || echo 1) -eq 1 ]" \
    "acceptable_cpu"

# 메모리 사용률 확인
MEMORY_USAGE=$(free | grep Mem | awk '{printf("%.1f", $3/$2 * 100.0)}')
verify "메모리 사용률 < 80%" \
    "[ \$(echo \"$MEMORY_USAGE < 80\" | bc 2>/dev/null || echo 1) -eq 1 ]" \
    "acceptable_memory"

# 디스크 사용률 확인
DISK_USAGE=$(df / | tail -1 | awk '{print $5}' | sed 's/%//')
verify "디스크 사용률 < 80%" \
    "[ $DISK_USAGE -lt 80 ]" \
    "acceptable_disk"

# ====================================
# 10. 보안 설정 확인
# ====================================
echo -e "${PURPLE}📋 10. 보안 설정 확인${NC}"
echo "======================================"

verify "UFW 방화벽 규칙 설정됨" \
    "sudo ufw status numbered | grep -q '80\\|443\\|22'" \
    "firewall_configured"

verify "Fail2ban 감옥 설정됨" \
    "sudo fail2ban-client status | grep -q 'jail'" \
    "fail2ban_configured"

verify "Deploy 사용자 존재" \
    "id deploy >/dev/null 2>&1" \
    "user_exists"

verify "Deploy 사용자 sudo 권한" \
    "sudo -l -U deploy 2>/dev/null | grep -q 'ALL'" \
    "sudo_configured"

# ====================================
# 11. 환경별 추가 테스트
# ====================================
echo -e "${PURPLE}📋 11. 환경별 추가 테스트${NC}"
echo "======================================"

# 프로덕션 환경인 경우 HTTPS 테스트
if [ "$API_DOMAIN" != "localhost" ]; then
    verify "HTTPS API 접근 가능" \
        "curl -sf https://$API_DOMAIN/actuator/health >/dev/null 2>&1" \
        "https_accessible"
    
    verify "HTTP → HTTPS 리다이렉트" \
        "curl -sI http://$API_DOMAIN | grep -q '301\\|302'" \
        "redirect_configured"
    
    verify "모니터링 대시보드 HTTPS 접근" \
        "curl -sf https://$MONITORING_DOMAIN >/dev/null 2>&1" \
        "monitoring_https"
fi

# 외부 API 연결 테스트 (업비트)
verify "외부 인터넷 연결 (업비트 API)" \
    "curl -sf https://api.upbit.com/v1/market/all >/dev/null 2>&1" \
    "external_api_reachable"

# ====================================
# 결과 요약
# ====================================
echo ""
echo "======================================"
echo -e "${BLUE}📊 검증 결과 요약${NC}"
echo "======================================"

echo -e "${CYAN}총 검사 항목: $TOTAL_CHECKS${NC}"
echo -e "${GREEN}통과: $((TOTAL_CHECKS - FAILED_CHECKS))${NC}"
echo -e "${RED}실패: $FAILED_CHECKS${NC}"

if [ $FAILED_CHECKS -eq 0 ]; then
    echo ""
    echo -e "${GREEN}🎉 모든 검증을 통과했습니다!${NC}"
    echo -e "${GREEN}✅ Autocoin API가 성공적으로 배포되었습니다.${NC}"
    EXIT_CODE=0
else
    echo ""
    echo -e "${YELLOW}⚠️  일부 검증에 실패했습니다.${NC}"
    echo -e "${YELLOW}📋 실패한 항목들을 확인하고 수정해주세요.${NC}"
    EXIT_CODE=1
fi

echo ""
echo -e "${BLUE}📊 상세 결과:${NC}"
for result in "${VERIFICATION_RESULTS[@]}"; do
    echo "$result"
done

# ====================================
# 시스템 정보 출력
# ====================================
echo ""
echo "======================================"
echo -e "${BLUE}💻 시스템 정보${NC}"
echo "======================================"

echo -e "${CYAN}🖥️  시스템:${NC}"
echo "OS: $(lsb_release -d 2>/dev/null | cut -f2 || uname -s)"
echo "Kernel: $(uname -r)"
echo "Uptime: $(uptime -p 2>/dev/null || uptime | awk -F'up ' '{print $2}' | awk -F', load' '{print $1}')"

echo -e "${CYAN}💾 리소스:${NC}"
echo "Memory: $(free -h | grep Mem | awk '{print "Used: " $3 " / " $2 " (" $3/$2*100 "%)"}' 2>/dev/null || echo 'N/A')"
echo "Disk: $(df -h / | tail -1 | awk '{print "Used: " $3 " / " $2 " (" $5 ")"}')"
echo "CPU Load: $(cat /proc/loadavg | awk '{print $1, $2, $3}')"

echo -e "${CYAN}🐳 Docker:${NC}"
echo "Running Containers: $(docker ps --format 'table {{.Names}}\t{{.Status}}' | grep -v NAMES | wc -l)"
echo "Docker Version: $(docker --version 2>/dev/null | awk '{print $3}' | sed 's/,//' || echo 'N/A')"

echo -e "${CYAN}🌐 네트워크:${NC}"
echo "Listening Ports: $(ss -tuln | grep LISTEN | wc -l)"
echo "Active Connections: $(ss -tuln | wc -l)"

# ====================================
# 권장 다음 단계
# ====================================
echo ""
echo "======================================"
echo -e "${BLUE}🚀 권장 다음 단계${NC}"
echo "======================================"

if [ $FAILED_CHECKS -eq 0 ]; then
    echo -e "${GREEN}✅ 배포가 성공적으로 완료되었습니다!${NC}"
    echo ""
    echo "📋 다음 단계:"
    echo "1. 🔍 모니터링 대시보드 확인: https://${MONITORING_DOMAIN}"
    echo "2. 📊 API 메트릭 모니터링: https://${MONITORING_DOMAIN}/prometheus"
    echo "3. 📈 Grafana 대시보드 설정: https://${MONITORING_DOMAIN}"
    echo "4. 🔔 Slack 알림 채널 확인: #alerts, #critical-alerts"
    echo "5. 📝 운영 문서 업데이트"
    echo "6. 👥 팀원들에게 배포 완료 알림"
    echo ""
    echo "📚 유용한 명령어:"
    echo "• ./scripts/deploy.sh status     - 배포 상태 확인"
    echo "• ./scripts/deploy.sh logs      - 실시간 로그 확인"
    echo "• system-status                 - 시스템 상태 요약"
    echo "• cleanup-system               - 시스템 정리"
else
    echo -e "${RED}❌ 일부 검증에 실패했습니다.${NC}"
    echo ""
    echo "🔧 문제 해결 단계:"
    echo "1. 📋 실패한 항목들을 위에서 확인"
    echo "2. 🔍 로그 확인: ./scripts/deploy.sh logs"
    echo "3. 🔄 컨테이너 재시작: docker-compose restart [container_name]"
    echo "4. 📞 필요시 기술 지원팀 연락"
    echo ""
    echo "🚨 긴급한 문제가 있다면:"
    echo "• ./scripts/deploy.sh rollback  - 이전 버전으로 롤백"
    echo "• system-status                 - 전체 시스템 상태 확인"
fi

echo ""
echo "======================================"
echo -e "${BLUE}📞 지원 정보${NC}"
echo "======================================"
echo "📧 기술 지원: tech@autocoin.com"
echo "🆘 긴급 문의: emergency@autocoin.com"
echo "📚 문서: https://docs.autocoin.com"
echo "📊 상태 페이지: https://status.autocoin.com"
echo ""

# ====================================
# 검증 완료 알림
# ====================================
if [ -n "$SLACK_WEBHOOK_URL" ] && [ $FAILED_CHECKS -eq 0 ]; then
    # Slack에 성공 알림 전송
    curl -X POST -H 'Content-type: application/json' \
        --data "{
            \"text\": \"🎉 Autocoin API 배포 검증 완료!\",
            \"attachments\": [{
                \"color\": \"good\",
                \"fields\": [{
                    \"title\": \"검증 결과\",
                    \"value\": \"✅ 총 $TOTAL_CHECKS개 항목 모두 통과\",
                    \"short\": true
                }, {
                    \"title\": \"환경\",
                    \"value\": \"${ENVIRONMENT:-Production}\",
                    \"short\": true
                }, {
                    \"title\": \"시간\",
                    \"value\": \"$(date '+%Y-%m-%d %H:%M:%S')\",
                    \"short\": true
                }]
            }]
        }" \
        "$SLACK_WEBHOOK_URL" >/dev/null 2>&1 || true
fi

echo -e "${GREEN}✅ 검증 스크립트 완료${NC}"
exit $EXIT_CODE
