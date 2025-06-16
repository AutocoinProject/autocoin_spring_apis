# 🚀 Autocoin API 프로덕션 배포 최종 가이드

## 📋 배포 전 체크리스트

### ✅ 필수 준비사항
- [ ] 서버 준비 완료 (Ubuntu 20.04+, 8GB RAM, 50GB SSD)
- [ ] 도메인 DNS 설정 완료 (`api.autocoin.com`, `monitoring.autocoin.com`)
- [ ] SSL 인증서 준비 (Let's Encrypt 또는 유효한 인증서)
- [ ] 환경 변수 파일 `.env.prod` 작성 완료
- [ ] AWS S3, OAuth2, API 키 등 외부 서비스 계정 준비
- [ ] Slack 웹훅 URL 설정
- [ ] 팀 구성원들에게 배포 일정 공지

---

## 🔧 1단계: 서버 초기 설정

### 서버 접속 및 권한 설정
```bash
# 서버에 SSH 접속
ssh root@your_server_ip

# 서버 초기 설정 스크립트 실행
curl -fsSL https://raw.githubusercontent.com/your-repo/autocoin-api/main/scripts/server-setup.sh -o server-setup.sh
chmod +x server-setup.sh
sudo ./server-setup.sh
```

### 배포 사용자로 전환
```bash
# Deploy 사용자로 전환
sudo su - deploy

# 배포 디렉토리 생성
sudo mkdir -p /opt/autocoin
sudo chown deploy:deploy /opt/autocoin
cd /opt/autocoin
```

---

## 🔐 2단계: SSL 인증서 설정

### Let's Encrypt 인증서 (프로덕션)
```bash
# SSL 설정 스크립트 실행
sudo ./scripts/ssl-setup.sh letsencrypt
```

### 자체 서명 인증서 (개발/테스트)
```bash
# 개발 환경용 SSL 설정
sudo ./scripts/ssl-setup.sh self-signed
```

---

## ⚙️ 3단계: 환경 설정

### .env.prod 파일 생성
```bash
# 환경 파일 복사 및 수정
cp .env.prod.example .env.prod
nano .env.prod
```

### 필수 환경 변수 설정
```bash
# 데이터베이스 (강력한 비밀번호 사용!)
DB_PASSWORD=your_secure_db_password_here
MYSQL_ROOT_PASSWORD=your_secure_root_password_here

# JWT 보안 (256비트 이상)
JWT_SECRET=your_jwt_secret_key_must_be_at_least_256_bits_long_here

# 업비트 암호화 키 (32자)
UPBIT_ENCRYPTION_KEY=your_upbit_encryption_key_32_chars_here

# OAuth2 설정
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret

# AWS S3
AWS_ACCESS_KEY=your_aws_access_key
AWS_SECRET_KEY=your_aws_secret_key
AWS_S3_BUCKET=autocoin-prod-bucket

# 모니터링
SENTRY_DSN=your_sentry_dsn
SLACK_WEBHOOK_URL=your_slack_webhook_url
GRAFANA_ADMIN_PASSWORD=your_secure_grafana_password

# 서버 정보
DEPLOY_HOST=your_server_ip_here
API_DOMAIN=api.autocoin.com
MONITORING_DOMAIN=monitoring.autocoin.com
```

---

## 🚀 4단계: 로컬에서 배포 실행

### 로컬 환경 준비
```bash
# 프로젝트 루트에서 환경 파일 설정
cp .env.prod.example .env.prod
# .env.prod 파일 수정 (위의 설정과 동일)

# 스크립트 실행 권한 부여
chmod +x scripts/*.sh
```

### 전체 배포 실행
```bash
# 📦 빌드 → 🔄 배포 → ✅ 검증 자동 실행
./scripts/deploy.sh deploy
```

### 단계별 배포 (수동)
```bash
# 1. 애플리케이션 빌드
./gradlew clean build

# 2. 데이터베이스 백업
./scripts/deploy.sh backup

# 3. 서버에 파일 업로드 및 배포
./scripts/deploy.sh deploy

# 4. 배포 상태 확인
./scripts/deploy.sh status
```

---

## 🔍 5단계: 배포 검증

### 자동 검증 실행
```bash
# 서버에서 검증 스크립트 실행
cd /opt/autocoin
./scripts/verify-deployment.sh
```

### 수동 검증
```bash
# 1. 기본 헬스체크
curl -sf https://api.autocoin.com/actuator/health

# 2. 컨테이너 상태 확인
docker ps

# 3. 로그 확인
docker logs autocoin-api-prod --tail 20

# 4. 리소스 사용량 확인
docker stats --no-stream

# 5. 모니터링 대시보드 접속
# https://monitoring.autocoin.com
```

---

## 📊 6단계: 모니터링 설정

### Grafana 대시보드 접속
```bash
# URL: https://monitoring.autocoin.com
# 계정: admin / [GRAFANA_ADMIN_PASSWORD]
```

### Slack 알림 채널 설정
1. `#alerts` - 일반 알림
2. `#critical-alerts` - 긴급 알림
3. `#performance-alerts` - 성능 알림
4. `#security-alerts` - 보안 알림

### 모니터링 URL
- **API**: https://api.autocoin.com
- **Grafana**: https://monitoring.autocoin.com
- **Prometheus**: https://monitoring.autocoin.com/prometheus
- **AlertManager**: https://monitoring.autocoin.com/alertmanager

---

## 🛠️ 7단계: 운영 관리

### 일상적인 운영 명령어
```bash
# 현재 상태 확인
./scripts/deploy.sh status

# 실시간 로그 확인
./scripts/deploy.sh logs

# 시스템 상태 요약
system-status

# 시스템 정리
cleanup-system

# 새 버전 배포
./scripts/deploy.sh deploy

# 이전 버전으로 롤백
./scripts/deploy.sh rollback

# 데이터베이스 백업
./scripts/deploy.sh backup
```

### SSL 인증서 관리
```bash
# 인증서 상태 확인
sudo ./scripts/ssl-setup.sh status

# 인증서 수동 갱신
sudo ./scripts/ssl-setup.sh renew
```

---

## 🚨 8단계: 장애 대응

### 일반적인 문제 해결

#### API 서버가 응답하지 않는 경우
```bash
# 1. 컨테이너 상태 확인
docker ps -a

# 2. 로그 확인
docker logs autocoin-api-prod

# 3. 컨테이너 재시작
docker restart autocoin-api-prod

# 4. 전체 스택 재시작
docker-compose -f docker/docker-compose.prod.yml restart
```

#### 데이터베이스 연결 실패
```bash
# 1. MySQL 상태 확인
docker exec autocoin-mysql-prod mysqladmin ping

# 2. MySQL 재시작
docker restart autocoin-mysql-prod

# 3. 연결 테스트
docker exec autocoin-api-prod curl -sf http://localhost:8080/actuator/health
```

#### 높은 리소스 사용량
```bash
# 1. 리소스 사용량 확인
docker stats

# 2. 시스템 리소스 확인
htop
df -h

# 3. 불필요한 데이터 정리
cleanup-system

# 4. 메모리 정리 (Java GC 강제 실행)
docker exec autocoin-api-prod jcmd 1 VM.gc
```

### 긴급 롤백 절차
```bash
# ⚠️ 문제 발생 시 즉시 실행
./scripts/deploy.sh rollback

# 상태 확인
curl -sf https://api.autocoin.com/actuator/health

# 팀에 알림
echo "긴급 롤백 완료 - $(date)" | slack-notify
```

---

## 📈 9단계: 성능 모니터링

### 주요 지표 확인
- **응답시간**: 95th percentile < 500ms
- **에러율**: < 1%
- **CPU 사용률**: < 70%
- **메모리 사용률**: < 80%
- **디스크 사용률**: < 85%

### 성능 최적화
```bash
# JVM 힙 상태 확인
docker exec autocoin-api-prod jcmd 1 VM.gc

# 데이터베이스 최적화
docker exec autocoin-mysql-prod mysqlcheck -u root -p --optimize --all-databases

# 로그 파일 압축
sudo logrotate -f /etc/logrotate.d/autocoin

# Docker 시스템 정리
docker system prune -a
```

---

## 📋 10단계: 정기 유지보수

### 일일 체크리스트
- [ ] 🔍 시스템 상태 확인 (`system-status`)
- [ ] 📊 Grafana 대시보드 모니터링
- [ ] 📈 리소스 사용량 확인
- [ ] 🔔 알림 채널 확인

### 주간 체크리스트
- [ ] 📦 애플리케이션 로그 분석
- [ ] 🔒 보안 업데이트 확인
- [ ] 💾 백업 파일 정리
- [ ] 📊 성능 트렌드 분석

### 월간 체크리스트
- [ ] 🔐 SSL 인증서 갱신 확인
- [ ] 🗄️ 데이터베이스 최적화
- [ ] 📈 용량 계획 검토
- [ ] 🔄 재해 복구 계획 테스트

---

## 🔧 고급 설정

### 다중 인스턴스 배포 (로드 밸런싱)
```yaml
# docker-compose.prod.yml에 추가
autocoin-api-2:
  extends:
    service: autocoin-api
  container_name: autocoin-api-prod-2
  ports:
    - "8081:8080"
```

### CI/CD 파이프라인 연동
```yaml
# .github/workflows/deploy.yml
name: Deploy to Production
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Deploy to server
        run: ./scripts/deploy.sh deploy
```

---

## 📞 지원 및 연락처

### 긴급 상황
- **📞 긴급 연락처**: +82-xxx-xxxx-xxxx
- **📧 긴급 이메일**: emergency@autocoin.com
- **💬 Slack**: #emergency 채널

### 일반 지원
- **📧 기술 지원**: tech@autocoin.com
- **📚 문서**: https://docs.autocoin.com
- **📊 상태 페이지**: https://status.autocoin.com

### 팀 연락처
- **🔧 개발팀**: dev@autocoin.com
- **☁️ 인프라팀**: infra@autocoin.com
- **🔒 보안팀**: security@autocoin.com

---

## 🎉 배포 성공!

축하합니다! **Autocoin API**가 성공적으로 프로덕션 환경에 배포되었습니다.

### 🌟 다음 단계
1. **📊 모니터링**: 첫 24시간 동안 시스템 상태 집중 관찰
2. **🔧 최적화**: 실제 트래픽 패턴에 따른 성능 튜닝
3. **💾 백업**: 정기 백업 스케줄 확인
4. **📚 문서화**: 운영 매뉴얼 업데이트
5. **👥 교육**: 팀원들에게 운영 지식 공유

### 📊 성공 지표
- ✅ 모든 헬스체크 통과
- ✅ 모니터링 시스템 정상 작동
- ✅ 보안 설정 완료
- ✅ 백업 시스템 구축
- ✅ 알림 시스템 연동

**🚀 성공적인 서비스 런칭을 축하합니다! 🚀**

---

*마지막 업데이트: $(date '+%Y-%m-%d')*
