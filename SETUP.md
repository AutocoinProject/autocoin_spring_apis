# 🚀 Autocoin API 로컬 개발 환경 설정

## 빠른 시작 가이드

### 1️⃣ 환경 파일 설정
```bash
# 환경 파일 복사 및 수정
cp .env.example .env

# .env 파일을 열어서 필요한 값들 설정
# 기본값으로도 로컬 개발은 가능합니다!
```

### 2️⃣ 데이터베이스 시작
```bash
# Docker로 MySQL과 Redis 시작
docker-compose up -d mysql redis

# 또는 개별 실행
docker-compose up -d mysql
docker-compose up -d redis
```

### 3️⃣ 애플리케이션 실행
```bash
# Gradle로 실행
./gradlew bootRun

# 또는 IDE에서 AutocoinSpringApiApplication.main() 실행
```

### 4️⃣ 확인
- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

## 📋 환경별 설정

### 🖥️ 로컬 개발 (권장)
```properties
SPRING_PROFILES_ACTIVE=local
DB_URL=jdbc:mysql://localhost:3306/autocoin_db...
DDL_AUTO=update
SWAGGER_ENABLED=true
```

### 🧪 개발 서버 연결
```properties
SPRING_PROFILES_ACTIVE=dev
DB_URL=jdbc:mysql://your-rds-endpoint:3306/autocoin_db...
DDL_AUTO=validate
SWAGGER_ENABLED=true
```

### 🚀 프로덕션 (주의!)
```properties
SPRING_PROFILES_ACTIVE=prod
DDL_AUTO=validate
SWAGGER_ENABLED=false
```

## 🔧 필수 설정 항목

### 최소 설정 (로컬 개발)
```bash
# 데이터베이스만 설정하면 기본 동작
DB_URL=jdbc:mysql://localhost:3306/autocoin_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=password
```

### OAuth2 로그인 사용 시
```bash
OAUTH2_ENABLED=true
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
KAKAO_CLIENT_ID=your-kakao-client-id
KAKAO_CLIENT_SECRET=your-kakao-client-secret
```

### 파일 업로드 사용 시
```bash
AWS_S3_BUCKET=your-bucket-name
AWS_ACCESS_KEY=your-access-key
AWS_SECRET_KEY=your-secret-key
```

### 뉴스 기능 사용 시
```bash
SERP_API_KEY=your-serp-api-key
```

### 업비트 연동 사용 시
```bash
UPBIT_ENCRYPTION_KEY=your-encryption-key
```

## 🚨 주의사항

### ⛔ 절대 하지 말 것
- **main 브랜치에 푸시 금지** (자동 배포 위험)
- **실제 API 키를 Git에 커밋 금지**
- **프로덕션 DB에 직접 연결 금지**

### ✅ 안전한 개발 방법
1. **develop 브랜치**에서 개발
2. **로컬 Docker DB** 사용
3. **테스트 API 키** 사용
4. **민감정보는 .env에만** 저장

## 🐳 Docker 컨테이너 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker ps

# 로그 확인
docker logs autocoin-mysql
docker logs autocoin-redis

# 컨테이너 중지
docker-compose down
```

## 🆘 문제 해결

### 데이터베이스 연결 실패
```bash
# MySQL 컨테이너 재시작
docker-compose restart mysql

# 포트 충돌 확인
netstat -an | findstr :3306
```

### Redis 연결 실패
```bash
# Redis 컨테이너 재시작
docker-compose restart redis

# 포트 충돌 확인
netstat -an | findstr :6379
```

### 컴파일 에러
```bash
# 캐시 정리 후 다시 빌드
./gradlew clean build
```

## 📞 도움말

문제가 해결되지 않으면:
1. `config/local-development.md` 파일 확인
2. GitHub Issues에 문제 등록
3. 팀 Slack 채널에 질문

---
**Happy Coding! 🎉**