# ✅ Autocoin API Production Deployment Checklist

## 🎯 **배포 전 최종 체크리스트**

### 📋 **1. 인프라 준비**
- [ ] **서버**: Ubuntu 20.04+ LTS, 8GB RAM, 50GB+ SSD
- [ ] **도메인**: DNS 설정 완료 (api.autocoin.com, monitoring.autocoin.com)
- [ ] **포트**: 80, 443, 22 개방
- [ ] **방화벽**: UFW 설정 완료
- [ ] **SSL**: Let's Encrypt 또는 유효한 인증서

### 🔐 **2. 보안 설정**
- [ ] **SSH**: 키 기반 인증 설정
- [ ] **사용자**: deploy 사용자 생성 및 권한 설정
- [ ] **Fail2ban**: 무차별 대입 공격 방지
- [ ] **방화벽**: 불필요한 포트 차단
- [ ] **비밀번호**: 모든 서비스 강력한 비밀번호 설정

### 🔑 **3. 환경 변수 (중요!)**
```bash
# 필수 확인 항목
DB_PASSWORD=                    # ✅ 강력한 MySQL 비밀번호
JWT_SECRET=                     # ✅ 256비트 이상 랜덤 문자열
UPBIT_ENCRYPTION_KEY=           # ✅ 32자 랜덤 문자열
GRAFANA_ADMIN_PASSWORD=         # ✅ Grafana 관리자 비밀번호
SLACK_WEBHOOK_URL=             # ✅ Slack 알림용 웹훅
SENTRY_DSN=                    # ✅ 에러 트래킹용
AWS_ACCESS_KEY=                # ✅ S3 접근 키
AWS_SECRET_KEY=                # ✅ S3 비밀 키
```

### 🔧 **4. 서비스 구성**
- [ ] **Docker**: 최신 버전 설치
- [ ] **Docker Compose**: v2.x 이상
- [ ] **Nginx**: SSL 터미네이션 및 리버스 프록시
- [ ] **MySQL**: 8.0+ 데이터베이스
- [ ] **Redis**: 캐시 및 세션 저장소
- [ ] **Elasticsearch**: 로그 저장 및 검색
- [ ] **Prometheus**: 메트릭 수집
- [ ] **Grafana**: 대시보드 및 시각화

### 📊 **5. 모니터링 설정**
- [ ] **Grafana**: 대시보드 접근 가능
- [ ] **Prometheus**: 메트릭 수집 확인
- [ ] **Alertmanager**: 알림 규칙 설정
- [ ] **Slack**: 알림 채널 생성 (#alerts, #critical-alerts)
- [ ] **Elasticsearch**: 로그 인덱싱 확인
- [ ] **Kibana**: 로그 분석 대시보드

### 🚀 **6. 배포 프로세스**
- [ ] **빌드**: `./gradlew clean build` 성공
- [ ] **테스트**: 모든 단위 테스트 통과
- [ ] **환경 설정**: `.env.prod` 파일 완성
- [ ] **백업**: 기존 데이터 백업 (해당시)
- [ ] **배포**: `./scripts/deploy.sh deploy` 실행
- [ ] **검증**: 헬스체크 통과

---

## 🔍 **배포 후 검증 체크리스트**

### ✅ **기본 동작 확인**
```bash
# 1. API 헬스체크
curl -sf https://api.autocoin.com/actuator/health
# 응답: {"status":"UP"}

# 2. 컨테이너 상태
docker ps
# 모든 컨테이너가 "Up" 상태

# 3. 로그 확인
docker logs autocoin-api-prod --tail 20
# ERROR 로그 없음
```

### ✅ **핵심 기능 테스트**
```bash
# 1. 사용자 인증 API
curl -X POST https://api.autocoin.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'

# 2. 업비트 API 연결 (인증 후)
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  https://api.autocoin.com/api/v1/upbit/status

# 3. 뉴스 수집 기능
curl https://api.autocoin.com/api/v1/news
```

### ✅ **모니터링 시스템 확인**
- [ ] **Grafana**: https://monitoring.autocoin.com 접속 가능
- [ ] **대시보드**: 메트릭 데이터 표시됨
- [ ] **알림**: 테스트 알림 발송 확인
- [ ] **로그**: Kibana에서 로그 조회 가능
- [ ] **SSL**: 인증서 유효성 확인

### ✅ **성능 기준**
- [ ] **응답시간**: 95th percentile < 500ms
- [ ] **에러율**: < 1%
- [ ] **CPU 사용률**: < 70%
- [ ] **메모리 사용률**: < 80%
- [ ] **동시 접속**: 100+ 사용자 처리 가능

---

## 🚨 **문제 발생 시 대응**

### 🔴 **Critical Issues**
```bash
# 애플리케이션 다운
./scripts/deploy.sh rollback

# 데이터베이스 연결 실패
docker restart autocoin-mysql-prod

# SSL 인증서 문제
./scripts/ssl-setup.sh renew
```

### 🟡 **Warning Issues**
```bash
# 높은 메모리 사용률
docker exec autocoin-api-prod jcmd 1 VM.gc

# 느린 응답시간
docker logs autocoin-api-prod | grep "ERROR\|WARN"

# 디스크 공간 부족
docker system prune -a
```

---

## 📞 **긴급 연락처**

### 🆘 **시스템 관리자**
- **이름**: [담당자명]
- **전화**: [연락처]
- **이메일**: admin@autocoin.com
- **Slack**: @admin

### 🔧 **개발팀**
- **이름**: [개발팀장]
- **전화**: [연락처]
- **이메일**: dev@autocoin.com
- **Slack**: #dev-team

### ☁️ **인프라팀**
- **이름**: [인프라 담당자]
- **전화**: [연락처]
- **이메일**: infra@autocoin.com
- **Slack**: #infra-team

---

## 📝 **배포 기록 템플릿**

```
===============================
Autocoin API 배포 기록
===============================

📅 배포 일시: 2024-XX-XX XX:XX:XX
👤 배포자: [이름]
🏷️ 버전: v1.0.0
🌍 환경: Production

📋 변경사항:
- [주요 기능 추가/수정 내용]
- [버그 수정 내용]
- [성능 개선 사항]

✅ 검증 완료:
- [ ] API 헬스체크 통과
- [ ] 핵심 기능 테스트 완료
- [ ] 모니터링 시스템 정상
- [ ] 성능 기준 만족

📊 배포 후 메트릭:
- 응답시간: XXXms (95th percentile)
- 에러율: X.XX%
- CPU 사용률: XX%
- 메모리 사용률: XX%

🔗 관련 링크:
- API: https://api.autocoin.com
- 모니터링: https://monitoring.autocoin.com
- 문서: https://docs.autocoin.com

💬 특이사항:
[배포 과정에서 발생한 특별한 사항이나 주의사항]

===============================
```

---

## 🎉 **배포 완료!**

모든 체크리스트를 완료하셨다면 **Autocoin API**가 성공적으로 프로덕션 환경에 배포되었습니다!

### 🌟 **다음 단계**
1. **모니터링**: 첫 24시간 동안 시스템 상태 집중 관찰
2. **최적화**: 실제 트래픽 패턴에 따른 성능 튜닝
3. **백업**: 정기 백업 스케줄 확인
4. **문서화**: 운영 매뉴얼 업데이트
5. **교육**: 팀원들에게 운영 지식 공유

**🚀 성공적인 서비스 런칭을 축하합니다! 🚀**
