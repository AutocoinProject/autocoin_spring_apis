# ⚙️ Configuration Files

이 폴더는 다양한 환경과 서비스 설정 파일들을 포함합니다.

## 📁 디렉토리 구조

```
config/
├── 📁 aws/                    # AWS 관련 설정
│   └── 📄 aws-s3-policy.json # S3 버킷 정책
└── 📄 buildspec.yml          # AWS CodeBuild 설정
```

---

## 📄 **파일 설명**

### ☁️ **AWS 설정**

#### [`aws/aws-s3-policy.json`](aws/aws-s3-policy.json)
- **용도**: S3 버킷 접근 권한 정책
- **포함 내용**: 
  - 파일 업로드/다운로드 권한
  - 버킷 리스팅 권한
  - CORS 설정 관련 권한

#### [`buildspec.yml`](buildspec.yml)
- **용도**: AWS CodeBuild CI/CD 파이프라인 설정
- **포함 내용**:
  - 빌드 환경 설정
  - 테스트 실행 단계
  - 아티팩트 생성 과정

---

## 🔧 **사용 방법**

### 🪣 **S3 정책 적용**
```bash
# AWS CLI를 사용하여 S3 버킷에 정책 적용
aws s3api put-bucket-policy \
  --bucket your-autocoin-bucket \
  --policy file://config/aws/aws-s3-policy.json
```

### 🏗️ **CodeBuild 프로젝트 설정**
1. AWS Console → CodeBuild
2. 새 프로젝트 생성
3. buildspec.yml 파일 경로: `config/buildspec.yml`

---

## 📋 **설정 파일 관리 가이드**

### ✅ **보안 주의사항**
- **🔐 민감 정보 금지**: API 키, 비밀번호 등 하드코딩 금지
- **📝 환경 변수 사용**: 모든 민감 정보는 환경 변수로 관리
- **🔍 정기 검토**: 권한 설정 정기적 검토 및 최소 권한 원칙 적용

### 📊 **버전 관리**
- 설정 변경 시 반드시 Git 커밋
- 변경 사유를 커밋 메시지에 명시
- 프로덕션 적용 전 테스트 환경에서 검증

### 🔄 **업데이트 프로세스**
1. **로컬 테스트**: 로컬 환경에서 설정 검증
2. **스테이징 적용**: 스테이징 환경에서 테스트
3. **프로덕션 배포**: 문제없음 확인 후 프로덕션 적용
4. **롤백 준비**: 이전 설정 백업 보관

---

## 🚨 **트러블슈팅**

### ❌ **일반적인 문제들**

#### S3 접근 권한 오류
```bash
# 해결 방법
1. IAM 사용자 권한 확인
2. S3 버킷 정책 검토
3. CORS 설정 확인
```

#### CodeBuild 빌드 실패
```bash
# 디버깅 순서
1. buildspec.yml 문법 검사
2. 환경 변수 설정 확인
3. IAM 역할 권한 검토
4. CloudWatch 로그 확인
```

---

## 📚 **관련 문서**
- [AWS S3 정책 가이드](https://docs.aws.amazon.com/s3/latest/userguide/bucket-policies.html)
- [CodeBuild buildspec 참조](https://docs.aws.amazon.com/codebuild/latest/userguide/build-spec-ref.html)
- [IAM 정책 예제](https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_examples.html)

---

**💡 Tip**: 새로운 설정 파일을 추가할 때는 이 README.md도 함께 업데이트해주세요!
