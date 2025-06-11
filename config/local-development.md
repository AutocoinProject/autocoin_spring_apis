# 🚨 긴급 로컬 개발 가이드

## 현재 상황
- **자동 배포 비활성화됨** (안전)
- **main 브랜치 푸시 금지** 
- **develop 브랜치에서 개발 진행**

## 로컬 개발 설정

### 1. 안전한 데이터베이스 설정
```bash
# 로컬 MySQL 사용 (Docker)
cd C:\DEV\autocoin\autocoin_spring_api
docker-compose up -d mysql redis
```

### 2. 환경 변수 설정
```properties
# .env 파일에서 로컬 개발용으로 변경
SPRING_PROFILES_ACTIVE=local
DDL_AUTO=update
DB_URL=jdbc:mysql://localhost:3306/autocoin_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=password
```

### 3. 개발 워크플로우
1. `develop` 브랜치에서 개발
2. 로컬 테스트 완료 후 커밋
3. **절대 main 브랜치로 머지 금지**

## 해결해야 할 문제들
- [ ] 프로덕션 DB 연결 전략 수정
- [ ] DDL 전략 통일
- [ ] Staging 환경 구축
- [ ] 승인 기반 배포 프로세스
- [ ] GitHub Secrets 설정
