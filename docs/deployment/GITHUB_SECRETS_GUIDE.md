# GitHub Secrets 설정 가이드

이 프로젝트의 자동 배포를 위해 다음 GitHub Secrets을 설정해야 합니다.

## 🔐 필수 Secrets

### 1. 서버 접속 정보
```
DEPLOY_HOST=your_server_ip_address
DEPLOY_USER=deploy
DEPLOY_SSH_KEY=-----BEGIN OPENSSH PRIVATE KEY-----
...your_ssh_private_key...
-----END OPENSSH PRIVATE KEY-----
```

### 2. 애플리케이션 환경 변수
```
# 데이터베이스
DB_PASSWORD=your_secure_db_password
MYSQL_ROOT_PASSWORD=your_secure_root_password

# JWT 보안
JWT_SECRET=your_jwt_secret_key_256_bits_or_longer

# 업비트 API
UPBIT_ENCRYPTION_KEY=your_upbit_encryption_key_32_chars

# OAuth2
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

# API 키
SERP_API_KEY=your_serp_api_key
```

## 📋 Secrets 설정 방법

### 1. GitHub 리포지토리 설정
1. GitHub 리포지토리 → **Settings**
2. **Secrets and variables** → **Actions**
3. **New repository secret** 클릭
4. 위의 각 항목들을 하나씩 추가

### 2. SSH 키 생성 (서버 접속용)
```bash
# 로컬에서 SSH 키 생성
ssh-keygen -t ed25519 -C "github-actions@autocoin.com" -f ~/.ssh/github_actions

# 공개키를 서버에 복사
ssh-copy-id -i ~/.ssh/github_actions.pub deploy@your_server_ip

# 개인키 내용을 DEPLOY_SSH_KEY에 설정
cat ~/.ssh/github_actions
```

### 3. 환경 변수 그룹화 (선택사항)
대신 하나의 큰 Secret으로 관리할 수도 있습니다:

```
ENV_PRODUCTION=DB_PASSWORD=xxx
JWT_SECRET=xxx
UPBIT_ENCRYPTION_KEY=xxx
...
```

## 🚀 자동 배포 플로우

### 1. 트리거 조건
- `main` 브랜치에 push
- Pull Request 생성/업데이트 (테스트만 실행)

### 2. 배포 과정
1. **테스트**: 단위 테스트 실행
2. **빌드**: JAR 파일 생성
3. **Docker**: 이미지 빌드 및 GitHub Container Registry에 푸시
4. **배포**: 서버에 SSH 접속하여 배포 스크립트 실행
5. **검증**: 배포 후 헬스체크 실행
6. **알림**: Slack으로 결과 알림

### 3. 배포 환경
- **레지스트리**: GitHub Container Registry (ghcr.io)
- **이미지**: `ghcr.io/username/autocoin_spring_api:latest`
- **서버**: SSH를 통한 원격 배포

## 🔍 배포 모니터링

### 1. GitHub Actions 로그
- **위치**: Repository → Actions 탭
- **실시간**: 배포 진행 상황 실시간 확인
- **히스토리**: 이전 배포 기록 및 로그

### 2. Slack 알림
- **성공**: 배포 완료 시 #deployments 채널에 알림
- **실패**: 에러 발생 시 실패 원인과 로그 링크

### 3. 서버 모니터링
- **Grafana**: https://monitoring.autocoin.com
- **헬스체크**: https://api.autocoin.com/actuator/health

## ⚠️ 주의사항

### 1. 보안
- **SSH 키**: 절대 공개되지 않도록 주의
- **Secrets**: GitHub Secrets에만 저장, 코드에 하드코딩 금지
- **권한**: 최소 권한 원칙 적용

### 2. 안정성
- **테스트**: 모든 테스트 통과 후 배포
- **백업**: 배포 전 자동 백업
- **롤백**: 문제 발생 시 즉시 롤백 가능

### 3. 비용
- **GitHub Actions**: 공개 저장소는 무료, 비공개는 분당 과금
- **Container Registry**: GitHub은 공개 이미지 무료, 비공개는 용량별 과금

## 🛠️ 수동 배포 (백업 방법)

자동 배포에 문제가 있을 경우:
```bash
# 로컬에서 수동 배포
./scripts/deploy.sh deploy

# 또는 서버에서 직접
ssh deploy@your_server_ip
cd /opt/autocoin
git pull origin main
./scripts/deploy.sh deploy
```

---

## 📞 지원

- **GitHub Actions 문제**: GitHub Support
- **서버 연결 문제**: 인프라팀
- **애플리케이션 문제**: 개발팀
