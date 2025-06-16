# 🐳 Docker Configuration

Autocoin Spring API의 Docker 관련 설정 파일들입니다.

## 📁 파일 구조

```
docker/
├── Dockerfile                 # API 서버 이미지 빌드
├── docker-compose.yml         # 개발용 (Redis만)
├── docker-compose.prod.yml    # 운영용 (전체 스택)
├── .dockerignore             # Docker 빌드 제외 파일
└── README.md                 # 이 파일
```

## 🚀 사용법

### 1. 개발 환경 (Redis만 실행)

```bash
# docker 폴더에서 실행
cd docker
docker-compose up -d

# 또는 루트에서 실행
docker-compose -f docker/docker-compose.yml up -d
```

### 2. 운영 환경 (전체 스택)

```bash
# 환경변수 설정 필요 (.env 파일)
cd docker
docker-compose -f docker-compose.prod.yml up -d

# 또는 루트에서 실행
docker-compose -f docker/docker-compose.prod.yml up -d
```

### 3. API 서버만 빌드

```bash
# 루트 디렉토리에서 실행
docker build -f docker/Dockerfile -t autocoin-api .
```

## 🔧 환경변수 설정

운영 환경 실행 전 다음 환경변수들을 설정해야 합니다:

### 필수 환경변수
```bash
# 데이터베이스
DB_ROOT_PASSWORD=strongpassword123
DB_USERNAME=autocoin
DB_PASSWORD=autocoinpass

# JWT 보안
JWT_SECRET=your-jwt-secret-key

# OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
KAKAO_CLIENT_ID=your-kakao-client-id
KAKAO_CLIENT_SECRET=your-kakao-client-secret

# AWS S3
AWS_S3_BUCKET=your-s3-bucket
AWS_ACCESS_KEY=your-aws-access-key
AWS_SECRET_KEY=your-aws-secret-key

# API 키
SERP_API_KEY=your-serp-api-key
UPBIT_ENCRYPTION_KEY=your-upbit-key

# 모니터링
SENTRY_DSN=your-sentry-dsn
GRAFANA_ADMIN_PASSWORD=admin-password
```

## 📊 포트 매핑

| 서비스 | 포트 | 설명 |
|--------|------|------|
| API 서버 | 8080 | Spring Boot API |
| MySQL | 3306 | 데이터베이스 |
| Redis | 6379 | 캐시/세션 저장소 |
| Nginx | 80/443 | 리버스 프록시 |
| Prometheus | 9090 | 메트릭 수집 |
| Grafana | 3001 | 모니터링 대시보드 |

## 🗂️ 볼륨 매핑

```yaml
# 데이터 영속성
mysql_data:/var/lib/mysql
redis_data:/data
prometheus_data:/prometheus
grafana_data:/var/lib/grafana

# 설정 파일 (호스트 → 컨테이너)
../nginx/nginx.conf → /etc/nginx/nginx.conf
../monitoring/prometheus/prometheus-prod.yml → /etc/prometheus/prometheus.yml
../monitoring/grafana/provisioning → /etc/grafana/provisioning

# 로그 파일
../logs → /app/logs
../logs/nginx → /var/log/nginx
```

## 🔍 모니터링

### Prometheus 메트릭
- http://localhost:9090

### Grafana 대시보드  
- http://localhost:3001
- 기본 계정: admin / (GRAFANA_ADMIN_PASSWORD)

### 헬스체크
```bash
# API 서버
curl http://localhost:8080/actuator/health

# Redis
docker exec autocoin-redis redis-cli ping

# MySQL
docker exec autocoin-mysql mysqladmin ping -h localhost
```

## 🐛 문제해결

### 컨테이너 로그 확인
```bash
# 전체 서비스 로그
docker-compose -f docker-compose.prod.yml logs

# 특정 서비스 로그
docker-compose -f docker-compose.prod.yml logs autocoin-api
docker-compose -f docker-compose.prod.yml logs mysql
```

### 컨테이너 재시작
```bash
# 특정 서비스 재시작
docker-compose -f docker-compose.prod.yml restart autocoin-api

# 전체 스택 재시작
docker-compose -f docker-compose.prod.yml restart
```

### 데이터 초기화
```bash
# 볼륨 삭제 (데이터 손실 주의!)
docker-compose -f docker-compose.prod.yml down -v

# 이미지 재빌드
docker-compose -f docker-compose.prod.yml build --no-cache
```

## ⚠️ 주의사항

1. **운영 환경**: `SWAGGER_ENABLED=false` 설정 권장
2. **보안**: 강력한 비밀번호 사용 필수
3. **백업**: 정기적인 MySQL 데이터 백업 수행
4. **모니터링**: Grafana 대시보드 정기 확인
5. **로그**: 로그 파일 정기 정리 필요

## 📞 지원

- 이슈 발생시: GitHub Issues
- 문서: 루트 README.md 참조
