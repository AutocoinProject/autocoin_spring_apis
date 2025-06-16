# 🚨 프로덕션 배포 수정 계획

## 현재 문제점
1. **자동 배포**: main 푸시 시 즉시 프로덕션 배포
2. **DB 이중화**: RDS와 Docker MySQL 혼재
3. **DDL 불일치**: 로컬 update vs 프로덕션 validate
4. **환경변수 노출**: 민감 정보가 .env에 하드코딩
5. **테스트 환경 부재**: staging 없이 바로 production

## 수정 계획

### Phase 1: 긴급 안전화 (완료)
- [x] 자동 배포 비활성화
- [x] develop 브랜치 생성
- [x] 로컬 개발 가이드 작성

### Phase 2: 환경 분리 (진행 중)
- [ ] GitHub Secrets 설정
- [ ] 프로덕션용 docker-compose 수정 (RDS 연결)
- [ ] DDL 전략 통일
- [ ] Staging 환경 구축

### Phase 3: 안전한 배포 프로세스
- [ ] 승인 기반 배포 워크플로우
- [ ] 데이터베이스 마이그레이션 전략
- [ ] 롤백 프로세스
- [ ] 모니터링 강화

## 필요한 GitHub Secrets
```
DB_PASSWORD - RDS 비밀번호
JWT_SECRET - 운영용 JWT 시크릿
AWS_ACCESS_KEY - AWS 접근키
AWS_SECRET_KEY - AWS 비밀키
SENTRY_DSN - Sentry DSN
SLACK_WEBHOOK_URL - Slack 웹훅
```

## 배포 전략 변경
```
현재: main → 즉시 production
변경: develop → staging → (승인) → production
```

## DDL 전략
```
로컬: update (개발 편의성)
Staging: validate + Flyway 마이그레이션
Production: validate + 수동 마이그레이션
```
