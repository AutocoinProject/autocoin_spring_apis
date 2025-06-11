# 🚀 Autocoin API Production Deployment Guide

## 📋 **준비사항 체크리스트**

### ✅ **서버 요구사항**
- **OS**: Ubuntu 20.04 LTS 이상
- **RAM**: 최소 4GB (권장 8GB)
- **Storage**: 최소 50GB SSD
- **Network**: 고정 IP 또는 도메인
- **포트**: 80, 443, 22 개방

### ✅ **도메인 설정**
- `api.autocoin.com` → 서버 IP
- `monitoring.autocoin.com` → 서버 IP
- `autocoin.com` → 서버 IP (선택)

### ✅ **필수 계정/서비스**
- AWS S3 버킷 및 IAM 사용자
- Google/Kakao OAuth 앱
- Sentry 프로젝트
- Slack 웹훅
- Upbit API 키 암호화 키

---

## 🛠️ **1단계: 서버 초기 설정**

### 서버에 접속
```bash
ssh your_user@your_server_ip
```

### 초기 설정 스크립트 실행
```bash
# 스크립트 다운로드
curl -O https://raw.githubusercontent.com/your-repo/autocoin-api/main/scripts/server-setup.sh
chmod +x server-setup.sh

# 서버 설정 실행
./server-setup.sh
```

이 스크립트는 다음을 자동으로 설정합니다:
- ✅ Docker & Docker Compose 설치
- ✅ 방화벽 (UFW) 구성
- ✅ Fail2ban 보안 설정
- ✅ Deploy 사용자 생성
- ✅ 로그 로테이션 설정
- ✅ 시스템 최적화

---

## 🔐 **2단계: SSL 인증서 설정**

### Let's Encrypt 자동 설정
```bash
# Deploy 사용자로 전환
sudo su - deploy
cd /opt/autocoin

# SSL 인증서 자동 설정
./scripts/ssl-setup.sh auto
```

### 개발환경용 자체 서명 인증서 (선택)
```bash
./scripts/ssl-setup.sh self-signed
```

---

## ⚙️ **3단계: 환경 변수 설정**

### 환경 파일 생성
```bash
cd /opt/autocoin
cp .env.prod.example .env.prod
nano .env.prod
```

### 필수 환경 변수 설정
```bash
# Database
DB_PASSWORD=your_secure_db_password_here
MYSQL_ROOT_PASSWORD=your_secure_root_password_here

# JWT Security (256비트 이상)
JWT_SECRET=your_jwt_secret_key_must_be_at_least_256_bits_long_here

# Upbit Encryption (32자)
UPBIT_ENCRYPTION_KEY=your_upbit_encryption_key_32_chars_here

# OAuth2
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret

# AWS S3
AWS_ACCESS_KEY=your_aws_access_key
AWS_SECRET_KEY=your_aws_secret_key
AWS_S3_BUCKET=autocoin-prod-bucket

# Monitoring
SENTRY_DSN=your_sentry_dsn
SLACK_WEBHOOK_URL=your_slack_webhook_url

# Grafana
GRAFANA_ADMIN_PASSWORD=your_secure_grafana_password
GRAFANA_SECRET_KEY=your_grafana_secret_key

# Server
DEPLOY_HOST=your_server_ip_here
```

---

## 🚀 **4단계: 애플리케이션 배포**

### 로컬에서 배포 실행
```bash
# 프로젝트 루트에서
cp .env.prod.example .env.prod
# .env.prod 파일 수정 후

# 배포 스크립트 실행 권한 부여
chmod +x scripts/*.sh

# 전체 배포 실행
./scripts/deploy.sh deploy
```

### 배포 과정
1. **빌드**: Spring Boot JAR 생성
2. **백업**: 기존 데이터베이스 백업
3. **업로드**: 소스코드 및 설정파일 업로드
4. **배포**: Docker 컨테이너 시작
5. **검증**: 헬스체크 및 동작 확인
6. **정리**: 오래된 백업 파일 정리

---

## 📊 **5단계: 모니터링 설정**

### 접속 URL
- **API**: https://api.autocoin.com
- **Grafana**: https://monitoring.autocoin.com (admin/your_password)
- **Prometheus**: https://monitoring.autocoin.com/prometheus
- **AlertManager**: https://monitoring.autocoin.com/alertmanager

### Slack 알림 설정
1. Slack에서 웹훅 URL 생성
2. `.env.prod`에 `SLACK_WEBHOOK_URL` 설정
3. 알림 채널 생성: `#alerts`, `#critical-alerts`, `#performance-alerts`

---

## 🔍 **6단계: 배포 검증**

### 헬스체크
```bash
# API 상태 확인
curl -sf https://api.autocoin.com/actuator/health

# 컨테이너 상태 확인
./scripts/deploy.sh status
```

### 로그 확인
```bash
# 애플리케이션 로그
./scripts/deploy.sh logs

# 특정 컨테이너 로그
docker logs autocoin-api-prod
docker logs autocoin-mysql-prod
```

### 리소스 모니터링
```bash
# 컨테이너 리소스 사용량
docker stats

# 시스템 리소스
htop
df -h
```

---

## 🛠️ **운영 관리**

### 일반적인 운영 명령어

#### 배포 관련
```bash
# 새 버전 배포
./scripts/deploy.sh deploy

# 이전 버전으로 롤백
./scripts/deploy.sh rollback

# 현재 상태 확인
./scripts/deploy.sh status

# 데이터베이스 백업
./scripts/deploy.sh backup
```

#### SSL 인증서 관리
```bash
# 인증서 상태 확인
./scripts/ssl-setup.sh status

# 수동 갱신
./scripts/ssl-setup.sh renew
```

#### 로그 관리
```bash
# 실시간 로그 조회
docker logs -f autocoin-api-prod

# 에러 로그만 조회
docker logs autocoin-api-prod 2>&1 | grep ERROR

# 특정 시간대 로그
docker logs autocoin-api-prod --since="2024-01-01T00:00:00" --until="2024-01-01T23:59:59"
```

### 데이터베이스 관리
```bash
# MySQL 접속
docker exec -it autocoin-mysql-prod mysql -u root -p

# 백업 생성
docker exec autocoin-mysql-prod mysqldump -u root -p autocoin > backup_$(date +%Y%m%d).sql

# 백업 복원
docker exec -i autocoin-mysql-prod mysql -u root -p autocoin < backup_20240101.sql
```

### 성능 최적화
```bash
# 불필요한 Docker 이미지 정리
docker system prune -a

# 로그 파일 압축
sudo logrotate -f /etc/logrotate.d/autocoin

# 데이터베이스 최적화
docker exec autocoin-mysql-prod mysqlcheck -u root -p --optimize --all-databases
```

---

## 🚨 **장애 대응**

### 일반적인 문제 해결

#### 애플리케이션이 시작되지 않는 경우
```bash
# 컨테이너 상태 확인
docker ps -a

# 로그 확인
docker logs autocoin-api-prod

# 환경변수 확인
docker exec autocoin-api-prod env | grep -E "(DB_|JWT_|UPBIT_)"

# 컨테이너 재시작
docker restart autocoin-api-prod
```

#### 데이터베이스 연결 실패
```bash
# MySQL 상태 확인
docker exec autocoin-mysql-prod mysqladmin ping -u root -p

# 네트워크 연결 확인
docker exec autocoin-api-prod ping mysql

# MySQL 재시작
docker restart autocoin-mysql-prod
```

#### 메모리 부족
```bash
# 메모리 사용량 확인
free -h
docker stats

# JVM 힙 덤프 생성
docker exec autocoin-api-prod jcmd 1 GC.run_finalization
docker exec autocoin-api-prod jcmd 1 VM.gc
```

#### SSL 인증서 만료
```bash
# 인증서 확인
openssl x509 -in /opt/autocoin/ssl/autocoin.com.crt -noout -dates

# 수동 갱신
sudo certbot renew
./scripts/ssl-setup.sh renew
```

### 응급 롤백 절차
```bash
# 1. 즉시 이전 버전으로 롤백
./scripts/deploy.sh rollback

# 2. 서비스 상태 확인
curl -sf https://api.autocoin.com/actuator/health

# 3. 로그 확인
docker logs autocoin-api-prod --tail 50

# 4. 데이터베이스 백업에서 복원 (필요시)
docker exec -i autocoin-mysql-prod mysql -u root -p autocoin < /opt/autocoin/backups/latest_backup.sql
```

---

## 📈 **성능 모니터링**

### 주요 지표
- **응답시간**: 95th percentile < 500ms
- **에러율**: < 1%
- **CPU 사용률**: < 70%
- **메모리 사용률**: < 80%
- **디스크 사용률**: < 85%

### 알림 설정
- **Critical**: API 다운, 데이터베이스 연결 실패
- **Warning**: 높은 응답시간, 높은 에러율, 리소스 사용률 증가
- **Info**: 배포 완료, 백업 성공

---

## 🔧 **고급 설정**

### 로드 밸런싱 (다중 인스턴스)
```yaml
# docker-compose.prod.yml에 추가
autocoin-api-2:
  extends:
    service: autocoin-api
  container_name: autocoin-api-prod-2
  ports:
    - "8081:8080"
```

### Redis 클러스터 설정
```yaml
redis-sentinel:
  image: redis:7.0-alpine
  command: redis-sentinel /usr/local/etc/redis/sentinel.conf
  # sentinel 설정...
```

### Elasticsearch 클러스터
```yaml
elasticsearch-2:
  extends:
    service: elasticsearch
  container_name: autocoin-elasticsearch-2
  environment:
    - node.name=elasticsearch-2
    - cluster.initial_master_nodes=elasticsearch,elasticsearch-2
```

---

## 📋 **보안 체크리스트**

### ✅ **네트워크 보안**
- 방화벽 설정 (UFW)
- SSH 키 기반 인증
- 기본 포트 변경 고려
- DDoS 보호 (Cloudflare 등)

### ✅ **애플리케이션 보안**
- JWT 시크릿 키 강화
- API 키 암호화
- HTTPS 강제 사용
- CORS 설정 제한

### ✅ **데이터베이스 보안**
- 강력한 비밀번호
- 네트워크 격리
- 백업 암호화
- 접근 로그 모니터링

### ✅ **모니터링 보안**
- 기본 인증 설정
- VPN 접근 제한
- 로그 수집 암호화
- 알림 채널 보안

---

## 💡 **최적화 팁**

### 성능 최적화
1. **JVM 튜닝**: GC 알고리즘 최적화
2. **데이터베이스**: 인덱스 최적화, 쿼리 튜닝
3. **캐싱**: Redis 활용도 증대
4. **CDN**: 정적 파일 최적화

### 비용 최적화
1. **리소스 모니터링**: 불필요한 리소스 제거
2. **자동 스케일링**: 트래픽 기반 조정
3. **백업 정책**: 보관 기간 최적화
4. **로그 관리**: 압축 및 정리 자동화

### 운영 효율성
1. **자동화**: CI/CD 파이프라인 구축
2. **모니터링**: 사전 경고 시스템
3. **문서화**: 운영 가이드 지속 업데이트
4. **교육**: 팀 운영 지식 공유

---

## 📞 **지원 및 문의**

- **Technical Support**: tech@autocoin.com
- **Emergency**: emergency@autocoin.com
- **Documentation**: https://docs.autocoin.com
- **Status Page**: https://status.autocoin.com

---

**🎉 축하합니다! Autocoin API가 프로덕션 환경에서 실행 중입니다!**
