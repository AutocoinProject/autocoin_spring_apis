# 🔐 GitHub Secrets 설정 가이드 (EC2 Docker 배포)

EC2에서 Docker를 사용한 자동 배포를 위해 다음 GitHub Secrets을 설정해야 합니다.

## 📋 필수 Secrets 목록

### 🖥️ **EC2 서버 접속 정보**
```
EC2_HOST=your.ec2.public.ip.address
EC2_USERNAME=ubuntu
EC2_SSH_KEY=-----BEGIN OPENSSH PRIVATE KEY-----
...your_ssh_private_key...
-----END OPENSSH PRIVATE KEY-----
EC2_SSH_PORT=22
```

### 🗄️ **데이터베이스 설정**
```
DB_NAME=autocoin
DB_USERNAME=autocoin
DB_PASSWORD=your_secure_db_password_here
MYSQL_ROOT_PASSWORD=your_secure_root_password_here
```

### 🔐 **JWT 보안 설정**
```
JWT_SECRET=your_jwt_secret_key_must_be_at_least_256_bits_long_here
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=86400000
```

### 🔑 **OAuth2 설정**
```
OAUTH2_ENABLED=true
GOOGLE_CLIENT_ID=your_google_client_id_here
GOOGLE_CLIENT_SECRET=your_google_client_secret_here
KAKAO_CLIENT_ID=your_kakao_client_id_here
KAKAO_CLIENT_SECRET=your_kakao_client_secret_here
```

### ☁️ **AWS S3 설정**
```
AWS_ACCESS_KEY=your_aws_access_key_here
AWS_SECRET_KEY=your_aws_secret_key_here
AWS_S3_BUCKET=autocoin-prod-bucket
AWS_REGION=ap-northeast-2
```

### 💰 **업비트 API 설정**
```
UPBIT_ENCRYPTION_KEY=your_upbit_encryption_key_32_chars_here
```

### 🔌 **외부 API 키**
```
SERP_API_KEY=your_serp_api_key_here
```

### 📊 **모니터링 설정**
```
SENTRY_DSN=your_sentry_dsn_here
SLACK_WEBHOOK_URL=your_slack_webhook_url_here
GRAFANA_ADMIN_PASSWORD=your_secure_grafana_password_here
GRAFANA_SECRET_KEY=your_grafana_secret_key_here
```

### 🌐 **도메인 및 CORS 설정**
```
API_DOMAIN=api.autocoin.com
MONITORING_DOMAIN=monitoring.autocoin.com
CORS_ALLOWED_ORIGINS=https://autocoin.com,https://www.autocoin.com,https://app.autocoin.com
```

---

## 🛠️ EC2 서버 준비

### 1. EC2 인스턴스 생성
```bash
# 추천 스펙
- Instance Type: t3.medium 이상 (2 vCPU, 4GB RAM)
- Storage: 30GB 이상
- Security Group: 22(SSH), 80(HTTP), 443(HTTPS), 8080(API) 포트 오픈
- OS: Ubuntu 20.04 LTS 이상
```

### 2. EC2 서버 초기 설정
```bash
# EC2에 SSH 접속
ssh -i your-key.pem ubuntu@your-ec2-ip

# 시스템 업데이트
sudo apt update && sudo apt upgrade -y

# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker ubuntu

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 필수 패키지 설치
sudo apt install -y git curl jq

# 방화벽 설정
sudo ufw allow 22
sudo ufw allow 80
sudo ufw allow 443
sudo ufw allow 8080
sudo ufw --force enable

# 로그아웃 후 재로그인 (Docker 그룹 적용)
exit
```

### 3. SSH 키 설정
```bash
# 로컬에서 SSH 키 생성
ssh-keygen -t ed25519 -C "github-actions@autocoin.com" -f ~/.ssh/autocoin-deploy

# 공개키를 EC2에 추가
ssh-copy-id -i ~/.ssh/autocoin-deploy.pub ubuntu@your-ec2-ip

# 개인키 내용을 GitHub Secret에 추가
cat ~/.ssh/autocoin-deploy
# 이 내용을 EC2_SSH_KEY에 설정
```

---

## 🔧 GitHub Secrets 설정 방법

### 1. GitHub 리포지토리 설정
1. GitHub 리포지토리 → **Settings**
2. **Secrets and variables** → **Actions**
3. **New repository secret** 클릭
4. 위의 각 항목들을 하나씩 추가

### 2. 빠른 설정 스크립트
```bash
# GitHub CLI를 사용한 일괄 설정 (선택사항)
gh secret set EC2_HOST --body "your.ec2.ip.address"
gh secret set EC2_USERNAME --body "ubuntu"
gh secret set DB_PASSWORD --body "your_secure_password"
# ... 나머지 secrets
```

---

## 🚀 자동 배포 플로우

### 📊 **배포 과정**
1. **코드 푸시** → `main` 브랜치에 푸시
2. **테스트** → 단위 테스트 실행
3. **빌드** → Spring Boot JAR 생성
4. **Docker 이미지** → GitHub Container Registry에 푸시
5. **EC2 배포** → SSH로 서버 접속하여 배포
6. **검증** → 헬스체크 및 상태 확인
7. **알림** → Slack으로 결과 통보

### 🔄 **배포 시 수행되는 작업**
```bash
# EC2 서버에서 자동 실행되는 내용
1. 최신 소스 코드 Pull
2. 환경 변수 파일 생성
3. 기존 컨테이너 중지 및 제거
4. 새 Docker 이미지 Pull
5. 데이터베이스 백업 (기존 MySQL 컨테이너가 있는 경우)
6. 새 컨테이너 시작
7. 헬스체크 대기 (최대 2.5분)
8. 배포 검증
9. 리소스 정리
```

---

## 📊 배포 모니터링

### 1. GitHub Actions 대시보드
- **위치**: Repository → Actions 탭
- **확인 사항**: 
  - 빌드 성공/실패
  - 테스트 결과
  - 배포 진행 상황
  - 에러 로그

### 2. EC2 서버 직접 확인
```bash
# SSH로 서버 접속
ssh ubuntu@your-ec2-ip

# 컨테이너 상태 확인
docker ps

# 애플리케이션 로그 확인
docker logs autocoin-api-prod --tail 50

# 헬스체크
curl http://localhost:8080/actuator/health

# 리소스 사용량
docker stats --no-stream
```

### 3. Slack 알림
- **성공 알림**: #deployments 채널
- **실패 알림**: 에러 상세 정보 및 로그 링크

---

## 🛠️ 트러블슈팅

### ❌ **일반적인 문제들**

#### 1. SSH 연결 실패
```bash
# 해결 방법
- EC2 Security Group에서 22번 포트 확인
- SSH 키 권한 확인: chmod 600 ~/.ssh/your-key
- EC2 인스턴스 상태 확인
```

#### 2. Docker 권한 오류
```bash
# EC2에서 실행
sudo usermod -aG docker ubuntu
# 로그아웃 후 재로그인
```

#### 3. 포트 충돌
```bash
# 기존 프로세스 확인
sudo lsof -i :8080
# 필요시 프로세스 종료
sudo kill -9 <PID>
```

#### 4. 메모리 부족
```bash
# 스왑 메모리 추가
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

### 🚨 **응급 복구**

#### 즉시 롤백
```bash
# EC2에서 수동 롤백
ssh ubuntu@your-ec2-ip
cd /opt/autocoin

# 이전 이미지로 롤백
docker stop autocoin-api-prod
docker rm autocoin-api-prod
docker run -d --name autocoin-api-prod --restart unless-stopped -p 8080:8080 --env-file .env.prod ghcr.io/your-username/autocoin-api:previous-tag
```

#### 수동 배포
```bash
# GitHub Actions가 실패한 경우
ssh ubuntu@your-ec2-ip
cd /opt/autocoin
git pull origin main
docker-compose -f docker/docker-compose.prod.yml --env-file .env.prod up -d
```

---

## 💰 비용 최적화

### 1. EC2 인스턴스
- **개발/테스트**: t3.micro (1GB RAM) - 프리티어
- **프로덕션**: t3.medium (4GB RAM) - $30/월 정도

### 2. GitHub
- **Actions**: 공개 저장소 무료, 비공개 2,000분/월 무료
- **Container Registry**: 공개 이미지 무료, 비공개 500MB 무료

### 3. 모니터링
- **CloudWatch**: 기본 모니터링 무료
- **추가 모니터링**: Grafana/Prometheus (자체 호스팅)

---

## ✅ 배포 준비 체크리스트

### 🔐 **보안**
- [ ] EC2 Security Group 설정
- [ ] SSH 키 기반 인증
- [ ] 강력한 데이터베이스 비밀번호
- [ ] JWT Secret 256비트 이상
- [ ] 모든 API 키 GitHub Secrets에 저장

### 🛠️ **인프라**
- [ ] EC2 인스턴스 실행 중
- [ ] Docker 설치 완료
- [ ] 필요한 포트 오픈 (22, 80, 443, 8080)
- [ ] 충분한 디스크 공간 (30GB 이상)

### 📊 **모니터링**
- [ ] Slack 웹훅 설정
- [ ] 도메인 DNS 설정 (선택사항)
- [ ] SSL 인증서 준비 (선택사항)

---

## 🎯 **배포 실행**

모든 설정이 완료되면:

```bash
git add .
git commit -m "feat: EC2 Docker 자동 배포 설정"
git push origin main
```

🎉 **자동 배포가 시작됩니다!**

GitHub Actions에서 진행 상황을 확인하고, Slack에서 완료 알림을 받으세요.
