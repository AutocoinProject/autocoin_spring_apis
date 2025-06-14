# 🔐 Autocoin Spring API - 보안 설정 가이드

## 개요
이 문서는 Autocoin Spring API의 민감한 정보 관리와 보안 설정에 대한 가이드입니다.

## 🔑 민감 정보 목록

### 필수 보안 정보
- **DB_PASSWORD**: 데이터베이스 비밀번호
- **JWT_SECRET**: JWT 토큰 서명용 비밀 키 (최소 32자)
- **OAUTH2 클라이언트 정보**: Google, Kakao 클라이언트 ID/Secret
- **AWS 자격증명**: Access Key, Secret Key
- **암호화 키**: Upbit API 암호화 키
- **외부 API 키**: SERP API 키
- **모니터링**: Slack Webhook URL, Sentry DSN

## 📁 파일 구조

```
autocoin_spring_apis/
├── .env                          # 로컬 개발용 (gitignore됨)
├── .env.dev.template            # 개발 환경 템플릿
├── .env.prod.template           # 운영 환경 템플릿
├── .env.example                 # 예시 파일 (민감정보 제외)
├── docker-compose.yml           # 로컬 개발용
├── docker-compose.dev.yml       # 개발 서버용
└── docker-compose.prod.yml      # 운영 서버용
```

## 🚀 설정 방법

### 1. GitHub Secrets 설정
Repository Settings → Secrets and variables → Actions에서 다음 secrets 추가:

#### 개발 환경
- `DB_PASSWORD_DEV`
- `JWT_SECRET_DEV`
- `GOOGLE_CLIENT_ID_DEV`
- `GOOGLE_CLIENT_SECRET_DEV`
- `KAKAO_CLIENT_ID_DEV`
- `KAKAO_CLIENT_SECRET_DEV`
- `AWS_ACCESS_KEY_DEV`
- `AWS_SECRET_KEY_DEV`
- `UPBIT_ENCRYPTION_KEY_DEV`
- `SERP_API_KEY_DEV`
- `SLACK_WEBHOOK_URL_DEV`
- `SENTRY_DSN_DEV`

#### 운영 환경
- `DB_PASSWORD_PROD`
- `JWT_SECRET_PROD`
- `GOOGLE_CLIENT_ID_PROD`
- `GOOGLE_CLIENT_SECRET_PROD`
- `KAKAO_CLIENT_ID_PROD`
- `KAKAO_CLIENT_SECRET_PROD`
- `AWS_ACCESS_KEY_PROD`
- `AWS_SECRET_KEY_PROD`
- `UPBIT_ENCRYPTION_KEY_PROD`
- `SERP_API_KEY_PROD`
- `SLACK_WEBHOOK_URL_PROD`
- `SENTRY_DSN_PROD`

### 2. 로컬 개발 환경 설정

1. `.env.dev.template`를 복사하여 `.env` 파일 생성
2. 실제 개발용 값들로 교체
3. **절대로 .env 파일을 커밋하지 마세요!**

```bash
cp .env.dev.template .env
# .env 파일 편집하여 실제 값 입력
```

### 3. Docker 환경에서 사용

```bash
# 개발 환경
docker-compose -f docker-compose.dev.yml up

# 운영 환경
docker-compose -f docker-compose.prod.yml up
```

## 🛡️ 보안 모범 사례

### JWT Secret 생성
```bash
# 안전한 JWT 시크릿 생성 (32자 이상)
openssl rand -base64 32
```

### 환경변수 검증
- JWT_SECRET: 최소 32자 이상
- DB_PASSWORD: 강력한 비밀번호 사용
- OAuth2 클라이언트: 올바른 redirect URI 설정

### 접근 제한
- AWS IAM: 최소 권한 원칙 적용
- 데이터베이스: 애플리케이션 전용 계정 사용
- Redis: 비밀번호 설정 권장

## 🚨 주의사항

1. **절대 커밋하지 말 것**:
   - `.env` 파일
   - 실제 API 키나 비밀번호가 포함된 파일
   - 백업 파일 (`*.bak`, `*.backup`)

2. **정기적인 키 로테이션**:
   - JWT Secret: 3개월마다 변경
   - API 키: 6개월마다 검토
   - 데이터베이스 비밀번호: 분기별 변경

3. **모니터링**:
   - Sentry로 보안 관련 오류 추적
   - Slack으로 중요 알림 설정
   - 로그에 민감정보 노출 방지

## 🔍 문제 해결

### 환경변수 누락 오류
```
Error: JWT_SECRET must be at least 32 characters
```
→ GitHub Secrets에서 JWT_SECRET_DEV/PROD 확인

### OAuth2 설정 오류
```
Provider ID must be specified for client registration
```
→ OAuth2 클라이언트 ID/Secret 설정 확인

### 데이터베이스 연결 실패
```
Access denied for user
```
→ DB_PASSWORD 설정 확인

## 📞 지원

보안 관련 문제나 의심스러운 활동 발견 시:
- 즉시 관련 API 키 비활성화
- 팀에 알림
- 보안 로그 확인
