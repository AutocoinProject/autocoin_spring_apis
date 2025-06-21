# Docker Compose 사용 가이드

## 파일 구조 정리 완료! ✅

중복된 docker-compose 파일들을 정리하고 용도별로 분리했습니다.

### 📁 파일 구조

```
├── docker-compose.prod.yml          # 🚀 EC2 프로덕션 배포용
├── docker/
│   ├── docker-compose.yml           # 🛠️ 로컬 개발용
│   ├── Dockerfile                   # 🐳 Docker 이미지 빌드
│   └── entrypoint.sh               # 📋 컨테이너 시작 스크립트
└── deployment/
    └── docker-compose.yml.backup    # 📦 백업 파일
```

## 🚀 사용 방법

### 1. 로컬 개발 (docker/docker-compose.yml)
```bash
cd docker
docker-compose up -d
```
- 애플리케이션을 빌드하고 로컬에서 실행
- Redis 포함
- .env 파일 필요

### 2. EC2 프로덕션 배포 (docker-compose.prod.yml)
```bash
# GitHub Actions에서 자동 실행
# 또는 수동으로:
docker-compose -f docker-compose.prod.yml up -d
```
- GitHub Container Registry에서 이미지 가져와서 실행
- 환경변수를 .env 파일 또는 GitHub Secrets에서 주입
- Redis는 선택사항 (profiles 사용)

## 🔧 필수 환경변수

### 필수
- `DB_PASSWORD`: RDS 데이터베이스 비밀번호
- `JWT_SECRET`: JWT 토큰 암호화 키
- `UPBIT_ENCRYPTION_KEY`: 업비트 API 암호화 키

### 선택사항
- `DB_HOST`, `AWS_*`, `REDIS_*`, `OAUTH2_*` 등

## 🎯 개선사항

1. **중복 제거**: 3개 → 2개 파일로 정리
2. **용도 분리**: 개발용 vs 프로덕션용
3. **Docker Compose 사용**: 단순 docker run → docker-compose
4. **환경변수 관리**: .env 파일로 통합
5. **Redis 선택적 실행**: profiles 기능 활용

## 📝 GitHub Secrets 설정 필요

CI/CD가 작동하려면 다음 secrets 설정:
- `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`
- `DB_HOST`, `DB_PASSWORD`, `JWT_SECRET`, `UPBIT_ENCRYPTION_KEY`
- 기타 선택적 환경변수들
