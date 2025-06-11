# 📚 Documentation Index

이 폴더는 Autocoin Spring API의 모든 문서를 포함합니다.

## 📁 디렉토리 구조

### 🚀 [`deployment/`](deployment/)
배포 관련 모든 가이드와 문서

#### 📋 **주요 문서들**
- **[`EC2_DEPLOYMENT_GUIDE.md`](deployment/EC2_DEPLOYMENT_GUIDE.md)** - EC2 Docker 자동 배포 완전 가이드
- **[`FINAL_DEPLOYMENT_GUIDE.md`](deployment/FINAL_DEPLOYMENT_GUIDE.md)** - 종합 배포 가이드 (전체 스택)
- **[`PRODUCTION_DEPLOYMENT_GUIDE.md`](deployment/PRODUCTION_DEPLOYMENT_GUIDE.md)** - 프로덕션 환경 상세 가이드
- **[`DEPLOYMENT_CHECKLIST.md`](deployment/DEPLOYMENT_CHECKLIST.md)** - 배포 전 체크리스트
- **[`GITHUB_SECRETS_GUIDE.md`](deployment/GITHUB_SECRETS_GUIDE.md)** - GitHub Secrets 설정 가이드

#### 🎯 **용도별 가이드 선택**

| 상황 | 추천 가이드 | 설명 |
|------|------------|------|
| **🚀 EC2 자동 배포** | [`EC2_DEPLOYMENT_GUIDE.md`](deployment/EC2_DEPLOYMENT_GUIDE.md) | GitHub Actions + EC2 + Docker |
| **🏢 전체 스택 배포** | [`FINAL_DEPLOYMENT_GUIDE.md`](deployment/FINAL_DEPLOYMENT_GUIDE.md) | 모니터링 포함 완전한 배포 |
| **📋 배포 전 준비** | [`DEPLOYMENT_CHECKLIST.md`](deployment/DEPLOYMENT_CHECKLIST.md) | 체크리스트 기반 검증 |
| **🔐 환경 변수 설정** | [`GITHUB_SECRETS_GUIDE.md`](deployment/GITHUB_SECRETS_GUIDE.md) | GitHub Secrets 상세 설정 |

---

## 🔗 **빠른 링크**

### 🚀 **시작하기**
1. [📋 배포 체크리스트](deployment/DEPLOYMENT_CHECKLIST.md) - 배포 전 필수 확인 사항
2. [🔐 GitHub Secrets 설정](deployment/GITHUB_SECRETS_GUIDE.md) - 환경 변수 및 보안 설정
3. [🚀 EC2 자동 배포](deployment/EC2_DEPLOYMENT_GUIDE.md) - 실제 배포 실행

### 📊 **고급 설정**
- [🏢 전체 스택 배포](deployment/FINAL_DEPLOYMENT_GUIDE.md) - Elasticsearch, Grafana 포함
- [🔧 프로덕션 환경](deployment/PRODUCTION_DEPLOYMENT_GUIDE.md) - 프로덕션 최적화

---

## 📖 **문서 작성 가이드**

### ✍️ **새 문서 추가 시**
1. 적절한 하위 디렉토리 선택
2. Markdown 형식으로 작성
3. 이 인덱스 파일 업데이트
4. 상호 참조 링크 추가

### 📝 **문서 스타일 가이드**
- **제목**: `# 🚀 제목` 형식으로 이모지 포함
- **섹션**: `## 📋 섹션명` 형식
- **코드 블록**: 언어 명시 (```bash, ```yaml 등)
- **링크**: 상대 경로 사용
- **이모지**: 가독성을 위해 적절히 사용

---

## 🔄 **문서 업데이트 정책**

### 📅 **정기 업데이트**
- **매월**: 모든 배포 가이드 검토
- **분기별**: 스크린샷 및 UI 변경사항 반영
- **버전 릴리즈 시**: 새 기능 관련 문서 추가

### ✅ **문서 품질 체크**
- [ ] 모든 링크가 유효한가?
- [ ] 코드 예제가 최신 버전과 일치하는가?
- [ ] 스크린샷이 현재 UI와 일치하는가?
- [ ] 설명이 명확하고 이해하기 쉬운가?

---

## 🤝 **기여하기**

문서 개선에 기여하고 싶으시다면:

1. **이슈 생성**: 문제점이나 개선 사항 제안
2. **Pull Request**: 직접 문서 수정 및 개선
3. **피드백**: 사용 후기 및 개선 의견 공유

---

**📧 문의사항**: docs@autocoin.com  
**🔗 위키**: https://github.com/your-username/autocoin_spring_api/wiki  
**📊 이슈 트래커**: https://github.com/your-username/autocoin_spring_api/issues
