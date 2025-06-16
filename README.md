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

## 🚀 **배포 & CI/CD**

### **🔄 GitHub Actions Workflows**

#### **1️⃣ Main CI/CD Pipeline** (`main-ci-cd.yml`)
```yaml
# 트리거: push to main/develop, PR
# 단계: 코드품질 → 테스트 → 빌드 → Docker → 배포 → 알림
Workflow Jobs:
├── 🔍 code-quality        # 코드 품질 검사 (병렬)
├── 🛡️ security-scan       # 보안 스캔 (병렬)
├── 📦 dependency-check    # 의존성 분석 (병렬)
├── 🧪 unit-tests          # 단위 테스트 (순차)
├── 📊 coverage-report     # 커버리지 리포트 (순차)
├── 🏗️ build              # 애플리케이션 빌드 (순차)
├── 🐳 docker-build        # Docker 이미지 빌드 (순차)
├── 🚀 deploy-staging      # 스테이징 배포 (조건부)
├── 🚀 deploy-production   # 프로덕션 배포 (조건부)
└── ✅ notify-success      # 성공/실패 알림
```

#### **2️⃣ PR Quality Checks** (`pr-checks.yml`)
```yaml
# 트리거: Pull Request
# 기능: PR 품질 검증, 자동 코멘트 생성
PR Checks:
├── 📋 pr-info            # PR 정보 수집
├── ⚡ quick-checks       # 빠른 검증 (제목, 브랜치명)
├── 🔍 code-quality       # 코드 품질 검사
├── 🧪 tests              # 테스트 실행
├── 🏗️ build-check        # 빌드 확인
├── 🐳 docker-check       # Docker 빌드 확인
└── 📝 pr-summary         # PR 요약 코멘트 생성
```

#### **3️⃣ Emergency Rollback** (`rollback.yml`)
```yaml
# 트리거: Manual (workflow_dispatch)
# 기능: 긴급 롤백, 백업, 검증
Rollback Process:
├── 🔍 validate-rollback  # 롤백 요청 검증
├── 💾 pre-rollback-backup # 현재 상태 백업
├── 🔄 rollback-production # 프로덕션 롤백
├── ✅ post-rollback-verification # 롤백 검증
└── 📢 notify-rollback    # 롤백 결과 알림
```

#### **4️⃣ Health Monitoring** (`monitoring.yml`)
```yaml
# 트리거: Schedule (매 30분), Manual
# 기능: 헬스체크, 성능 모니터링, 일일 리포트
Monitoring Jobs:
├── 🏥 basic-health-check    # 기본 헬스체크 (스케줄)
├── 🔬 detailed-system-check # 상세 시스템 체크 (수동)
├── 🚀 performance-check     # 성능 테스트 (수동)
├── 🚨 alert-on-failure      # 장애 알림
└── 📈 daily-report          # 일일 리포트
```

### **🎯 배포 전략**

#### **자동 배포**
- **develop → staging**: 자동 배포
- **main → production**: 자동 배포 (테스트 통과 시)
- **PR → review**: 자동 품질 검사 + 코멘트

#### **수동 배포**
- **Quick Deploy**: `deploy.yml` (긴급 배포용)
- **Environment Select**: `main-ci-cd.yml` (환경 선택 배포)
- **Emergency Rollback**: `rollback.yml` (긴급 롤백)

### **🔧 로컬 개발**
```bash
# 로컬 환경 시작
./scripts/start-local.bat

# 개발 환경 시작 (RDS 연결)
./scripts/start-dev.bat

# 운영 빌드
./scripts/build-prod.bat
```

### **📊 배포 모니터링**
- **GitHub Actions**: 실시간 배포 상태
- **Slack 알림**: #deployments, #alerts 채널
- **Health Check**: 자동 헬스체크 (30분마다)
- **Daily Report**: 매일 오전 9시 상태 리포트

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

### **🔀 Git Workflow**
1. Fork the repository
2. Create a feature branch
   ```bash
   git checkout -b feature/awesome-feature
   ```
3. Commit your changes (Conventional Commits)
   ```bash
   git commit -m "feat(auth): add JWT refresh token functionality"
   ```
4. Push to the branch
   ```bash
   git push origin feature/awesome-feature
   ```
5. Create a Pull Request

### **📝 커밋 컨벤션**
```
type(scope): description

Types:
- feat: 새로운 기능
- fix: 버그 수정
- docs: 문서 변경
- style: 코드 스타일 변경
- refactor: 코드 리팩토링
- test: 테스트 추가/수정
- chore: 빌드 관련 수정

Scopes:
- auth: 인증/인가
- user: 사용자 관리
- trading: 자동매매
- chart: 차트/분석
- news: 뉴스
- notification: 알림
```

### **🌿 브랜치 전략**
```
main          # 운영 환경 (자동 배포)
├── develop   # 개발 환경 (자동 배포)
├── feature/  # 기능 개발
├── bugfix/   # 버그 수정
├── hotfix/   # 긴급 수정
└── release/  # 릴리즈 준비
```

### **✅ PR 체크리스트**
- [ ] 테스트 코드 작성
- [ ] 코드 품질 검사 통과
- [ ] 문서 업데이트
- [ ] 브레이킹 체인지 여부 확인
- [ ] 리뷰어 지정

## 📞 **지원 & 문의**

### **🆘 문제 해결**
- **GitHub Issues**: 버그 리포트, 기능 요청
- **GitHub Discussions**: 질문, 아이디어 토론
- **Wiki**: 상세 개발 가이드

### **📢 알림 채널**
- **#deployments**: 배포 상태 알림
- **#alerts**: 시스템 장애 알림
- **#dev-alerts**: 개발 관련 알림
- **#daily-reports**: 일일 상태 리포트

### **🔧 개발 도구**
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Actuator**: http://localhost:8080/actuator
- **Health Check**: http://localhost:8080/actuator/health
- **GitHub Actions**: 실시간 CI/CD 상태

### **📈 모니터링**
- **Sentry**: 에러 트래킹
- **Prometheus**: 메트릭 수집
- **Slack**: 실시간 알림
- **GitHub**: 코드 품질 및 보안

---

**🚀 Autocoin Team**  
*"혁신적인 암호화폐 자동매매 플랫폼"*