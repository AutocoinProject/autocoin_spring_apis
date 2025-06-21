# EC2 인스턴스 사전 설정 가이드

## 1. Docker 설치 확인
```bash
# EC2에 SSH 접속 후 실행
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -a -G docker ec2-user

# 재로그인 후 확인
docker --version
```

## 2. 환경변수 설정 필요
애플리케이션 실행을 위해 다음 환경변수들이 필요합니다:

### 필수 환경변수:
- `DB_PASSWORD`: RDS 데이터베이스 비밀번호
- `JWT_SECRET`: JWT 토큰 암호화 키
- `UPBIT_ENCRYPTION_KEY`: 업비트 API 암호화 키

### 선택적 환경변수:
- `DB_HOST`: RDS 호스트 (기본값: autocoin-rds.cz7dv3rknd8e.ap-northeast-2.rds.amazonaws.com)
- `DB_PORT`: 데이터베이스 포트 (기본값: 3306)
- `REDIS_HOST`: Redis 호스트
- `REDIS_PORT`: Redis 포트 (기본값: 6379)

## 3. 보안 그룹 설정
- SSH (포트 22): GitHub Actions에서 접근
- HTTP (포트 8080): 애플리케이션 서비스
- MySQL (포트 3306): RDS 연결 (필요시)
- Redis (포트 6379): Redis 연결 (필요시)

## 4. RDS 및 Redis 설정
- RDS 인스턴스가 실행 중이어야 함
- Redis는 선택사항 (없어도 실행 가능)

## 5. 현재 워크플로우 한계점
환경변수가 하드코딩되어 있지 않아서, EC2에서 환경변수를 별도로 설정해야 합니다.
