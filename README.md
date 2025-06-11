# 🚀 Autocoin Spring API

암호화폐 자동 매매 시스템의 백엔드 API 서버입니다.

## 🏗️ **환경 구성**

### **🏠 Local (개발자 로컬)**
- **DB**: Docker MySQL
- **Cache**: Docker Redis
- **Storage**: AWS S3
- **Monitoring**: Sentry + Actuator + Prometheus

### **🧪 Dev (개발/테스트 서버)**
- **DB**: AWS RDS MySQL
- **Cache**: Docker Redis
- **Storage**: AWS S3
- **Monitoring**: Sentry + Actuator + Prometheus + Slack

### **🚀 Prod (운영 환경)**
- **DB**: AWS RDS MySQL (Multi-AZ)
- **Cache**: Docker Redis
- **Storage**: AWS S3
- **Monitoring**: Sentry + Actuator + Prometheus + Slack
- **Server**: AWS EC2 + Auto Scaling + ALB

## ⚡ **빠른 시작**

### 1️⃣ 로컬 환경 시작
```bash
# Docker 컨테이너 시작 + 애플리케이션 실행
./scripts/start-local.bat

# 또는 수동으로
docker-compose up -d mysql redis
cp .env.local .env
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 2️⃣ 개발 환경 시작 (RDS 연결)
```bash
# Redis만 시작 + RDS 연결하여 실행
./scripts/start-dev.bat

# 또는 수동으로
docker-compose up -d redis
cp .env.dev .env
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 3️⃣ 운영 빌드
```bash
# 테스트 + 빌드
./scripts/build-prod.bat

# JAR 파일 생성됨: build/libs/autocoin-spring-api.jar
```

## 🔧 **기술 스택**

### **Backend**
- **Framework**: Spring Boot 3.2.4
- **Language**: Java 17
- **Database**: MySQL 8.0
- **Cache**: Redis 7
- **Storage**: AWS S3
- **Security**: Spring Security + JWT
- **Documentation**: Swagger/OpenAPI 3

### **Monitoring**
- **Error Tracking**: Sentry
- **Metrics**: Prometheus + Micrometer
- **Health Check**: Spring Actuator
- **Alerts**: Slack Webhook
- **Logging**: Logback + JSON + Trace ID

### **External APIs**
- **Crypto Exchange**: Upbit API
- **News**: SERP API
- **AI/ML**: Flask API 연동

## 📂 **프로젝트 구조**

```
src/main/java/com/autocoin/
├── global/           # 글로벌 설정 (Security, Config, Exception)
├── user/            # 사용자 관리
├── auth/            # 인증/인가
├── trading/         # 자동매매
├── chart/           # 차트/기술분석
├── news/            # 뉴스 수집/분석
└── notification/    # 알림 시스템

src/main/resources/
├── application.yml              # 공통 설정
├── application-local.yml        # 로컬 환경
├── application-dev.yml          # 개발 환경
├── application-prod.yml         # 운영 환경
└── application-monitoring.yml   # 모니터링 설정

scripts/
├── start-local.bat    # 로컬 환경 시작
├── start-dev.bat      # 개발 환경 시작
└── build-prod.bat     # 운영 빌드

docker/
├── mysql/init/        # MySQL 초기화 스크립트
└── redis/            # Redis 설정
```

## 🌐 **API 엔드포인트**

### **인증**
- `POST /api/v1/auth/login` - 로그인
- `POST /api/v1/auth/register` - 회원가입
- `POST /api/v1/auth/refresh` - 토큰 갱신
- `GET /api/v1/auth/oauth2/{provider}` - 소셜 로그인

### **사용자**
- `GET /api/v1/users/profile` - 프로필 조회
- `PUT /api/v1/users/profile` - 프로필 수정
- `GET /api/v1/users/trading-settings` - 매매 설정

### **자동매매**
- `GET /api/v1/trading/strategies` - 전략 목록
- `POST /api/v1/trading/strategies` - 전략 생성
- `PUT /api/v1/trading/strategies/{id}/start` - 매매 시작
- `PUT /api/v1/trading/strategies/{id}/stop` - 매매 중지

### **차트/분석**
- `GET /api/v1/charts/{symbol}/candles` - 캔들 차트
- `GET /api/v1/charts/{symbol}/indicators` - 기술 지표
- `GET /api/v1/analysis/signals` - 매매 신호

### **뉴스**
- `GET /api/v1/news` - 뉴스 목록
- `GET /api/v1/news/{id}` - 뉴스 상세
- `GET /api/v1/news/analysis` - 뉴스 분석 결과

## 📊 **모니터링**

### **대시보드**
- **API**: http://localhost:8080
- **Swagger**: http://localhost:8080/swagger-ui.html
- **Actuator**: http://localhost:8080/actuator
- **Health**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus

### **로그**
```bash
# 로그 파일 위치
Local: logs/autocoin-local.log
Dev:   logs/autocoin-dev.log
Prod:  /app/logs/autocoin.log

# 실시간 로그 확인
tail -f logs/autocoin-local.log
```

## 🔐 **보안**

### **인증/인가**
- JWT 토큰 기반 인증
- Access Token (1시간) + Refresh Token (24시간)
- 소셜 로그인 지원 (Google, Kakao)
- Role 기반 접근 제어 (USER, ADMIN)

### **API 보안**
- CORS 설정
- Rate Limiting
- Request/Response 로깅
- 민감 정보 마스킹

## 🧪 **테스트**

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests UserServiceTest

# 통합 테스트 실행
./gradlew integrationTest
```

## 🚀 **배포**

### **로컬 개발**
```bash
./scripts/start-local.bat
```

### **개발 서버**
```bash
./scripts/start-dev.bat
```

### **운영 서버**
```bash
# 1. 빌드
./scripts/build-prod.bat

# 2. 서버에 업로드
scp build/libs/*.jar user@server:/app/
scp .env user@server:/app/

# 3. 서버에서 실행
ssh user@server
cd /app
docker run -d -p 6379:6379 --name redis redis:7-alpine
java -jar autocoin-spring-api.jar
```

## 🔧 **개발 가이드**

### **환경 변수 설정**
```bash
# 필수 환경 변수
DB_URL=jdbc:mysql://localhost:3306/autocoin_db
JWT_SECRET=your-secret-key
AWS_ACCESS_KEY=your-aws-key
AWS_SECRET_KEY=your-aws-secret
SENTRY_DSN=your-sentry-dsn
```

### **코드 스타일**
- Google Java Style Guide
- Lombok 사용으로 보일러플레이트 코드 최소화
- 계층형 아키텍처 (Controller → Service → Repository)
- Domain-Driven Design 적용

## 📈 **성능**

### **최적화**
- Connection Pool 튜닝 (HikariCP)
- Redis 캐싱
- JPA 배치 처리
- 비동기 처리 (@Async)

### **모니터링**
- 응답 시간 추적
- 메모리 사용량 모니터링
- DB 쿼리 성능 분석
- 에러율 추적

## 🤝 **기여하기**

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📞 **지원**

- **이슈**: GitHub Issues
- **문서**: Wiki
- **알림**: Slack (#dev-alerts)

---

**Autocoin Team** 🚀