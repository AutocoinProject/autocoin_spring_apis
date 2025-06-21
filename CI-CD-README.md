# 🚀 Autocoin Spring API - CI/CD 설정 완료

## 📁 현재 배포 구조

```
├── .github/workflows/
│   └── ci.yml                    # 🎯 메인 CI/CD 워크플로우
├── docker-compose.prod.yml       # 🐳 프로덕션 배포용
├── docker/
│   ├── docker-compose.yml        # 🛠️ 로컬 개발용
│   ├── Dockerfile               # 📦 Docker 이미지 빌드
│   └── entrypoint.sh           # 🚀 컨테이너 시작 스크립트
└── backup/                      # 📦 백업 파일들
    ├── ci-ssm.yml              # AWS SSM 방식 (권한 문제로 백업)
    ├── debug-ssh.yml           # SSH 디버그용
    └── ssh-troubleshoot.yml    # SSH 문제 해결용
```

## 🎯 현재 배포 방식

### ✅ Docker Compose + SSH 방식

**워크플로우**: `.github/workflows/ci.yml`

1. **테스트 실행** → Gradle 테스트
2. **Docker 이미지 빌드** → GitHub Container Registry
3. **EC2 배포** → SSH + Docker Compose

### 🔧 배포 과정

1. EC2에 SSH 접속
2. `docker-compose.prod.yml` 다운로드
3. 환경변수로 `.env` 파일 생성
4. Docker Compose로 컨테이너 실행
5. 헬스체크 (3번 재시도)
6. 이미지 정리

## 🔑 필요한 GitHub Secrets

### SSH 접속
- `EC2_HOST`: `54.175.66.222`
- `EC2_USER`: `ec2-user`
- `EC2_SSH_KEY`: SSH private key

### 애플리케이션 환경변수
- `DB_HOST`, `DB_PASSWORD`, `DB_USERNAME`, `DB_NAME`, `DB_PORT`
- `JWT_SECRET`, `UPBIT_ENCRYPTION_KEY`
- `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_S3_BUCKET`
- `SERP_API_KEY`, `SLACK_WEBHOOK_URL`, `SENTRY_DSN`

## 🚀 사용 방법

### 자동 배포
- `main` 브랜치에 푸시 시 자동 실행
- Pull Request 시 테스트만 실행

### 수동 배포
- GitHub Actions → "CI/CD" → "Run workflow"

## 🔧 로컬 개발

```bash
# 개발용 Docker Compose 실행
cd docker
docker-compose up -d

# 또는 직접 Spring Boot 실행
./gradlew bootRun
```

## 📊 배포 상태 확인

배포 완료 후 다음으로 확인 가능:
- **헬스체크**: `http://54.175.66.222:8080/actuator/health`
- **애플리케이션**: `http://54.175.66.222:8080`

## 🚨 문제 해결

### SSH 연결 실패 시
1. GitHub Secrets의 `EC2_SSH_KEY` 확인
2. EC2 보안 그룹에서 포트 22 허용 확인
3. `backup/` 폴더의 대안 방법 참고

### 배포 실패 시
1. GitHub Actions 로그 확인
2. EC2에서 Docker 상태 확인: `docker ps`
3. 애플리케이션 로그 확인: `docker logs autocoin-api`

## 📈 향후 개선 계획

- ⏳ AWS SSM 방식 도입 (권한 설정 후)
- ⏳ 블루-그린 배포
- ⏳ 자동 롤백 기능
- ⏳ 모니터링 대시보드

---

**현재 상태**: ✅ SSH + Docker Compose 방식으로 안정적 배포 가능
