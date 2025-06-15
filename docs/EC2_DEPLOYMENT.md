# EC2 배포 가이드

## 환경 설정

### 1. 환경변수 파일 설정
```bash
# 템플릿을 복사해서 실제 환경변수 파일 생성
cp .env.ec2.template .env.ec2

# 실제 값들을 입력
vi .env.ec2
```

### 2. 배포 스크립트 설정
```bash
# 템플릿을 복사해서 실제 스크립트 생성
cp scripts/set-env-ec2.sh.template scripts/set-env-ec2.sh

# 실제 값들을 입력
vi scripts/set-env-ec2.sh

# 실행 권한 부여
chmod +x scripts/set-env-ec2.sh
```

### 3. 배포 실행
```bash
# 환경변수 설정
source scripts/set-env-ec2.sh

# Docker Compose로 실행
docker-compose -f docker/docker-compose.ec2-fixed.yml up --build -d
```

## 보안 주의사항

- `.env.ec2` 파일은 절대 Git에 커밋하지 마세요
- `scripts/set-env-ec2.sh` 파일은 절대 Git에 커밋하지 마세요
- 모든 민감한 정보는 환경변수나 AWS Secrets Manager를 사용하세요

## 필요한 환경변수

- `DB_URL`: RDS 데이터베이스 연결 URL
- `DB_USERNAME`: 데이터베이스 사용자명
- `DB_PASSWORD`: 데이터베이스 비밀번호
- `JWT_SECRET`: JWT 토큰 시크릿 (최소 32자)
- `AWS_ACCESS_KEY`: AWS 액세스 키
- `AWS_SECRET_KEY`: AWS 시크릿 키
- `UPBIT_ENCRYPTION_KEY`: Upbit API 암호화 키
- `SERP_API_KEY`: SERP API 키
- `SLACK_WEBHOOK_URL`: Slack 웹훅 URL
- `SENTRY_DSN`: Sentry DSN
