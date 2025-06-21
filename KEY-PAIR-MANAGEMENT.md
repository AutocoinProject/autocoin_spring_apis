# EC2 키 페어 확인 및 관리 가이드

## 🔍 현재 사용 중인 키 확인 방법

### 1. AWS 콘솔에서 확인
1. **EC2 콘솔** → **인스턴스** → `i-09f9b0cd2b6a81741` 선택
2. **세부 정보** 탭에서 **키 페어 이름** 확인
3. 현재 사용 중인 키가 `ec2_key` 또는 `spring_key` 중 어느 것인지 확인

### 2. 로컬에서 테스트
```bash
# spring_key 테스트 (기존 키)
ssh -i "C:\DEV\autocoin-new-key.pem" ec2-user@54.175.66.222

# 연결되면 spring_key가 현재 키
# 연결 안 되면 ec2_key가 현재 키
```

## 🎯 권장 사항

### 옵션 1: 기존 키로 연결 확인
현재 로컬에 있는 `autocoin-new-key.pem`이 `spring_key`인지 확인:
```bash
ssh -i "C:\DEV\autocoin-new-key.pem" ec2-user@54.175.66.222
```

### 옵션 2: 새 키 다운로드
만약 `ec2_key`가 현재 키라면:
1. AWS 콘솔에서 `ec2_key.pem` 다운로드
2. GitHub Secrets에 새 키 설정

### 옵션 3: 키 통합
```bash
# EC2에 접속 후 (기존 키로)
ssh -i current_working_key.pem ec2-user@54.175.66.222

# 새로운 키 추가
echo "ssh-rsa AAAAB3NzaC1yc2E... new_public_key" >> ~/.ssh/authorized_keys
```

## ⚠️ 보안 주의사항

### 하지 말아야 할 것
- ❌ 여러 키를 동시에 GitHub Secrets에 설정
- ❌ 사용하지 않는 키를 그대로 방치
- ❌ 키 없이 EC2 접근 시도

### 해야 할 것  
- ✅ 현재 사용 중인 키 정확히 파악
- ✅ 작동하는 키 하나만 GitHub Secrets에 설정
- ✅ 사용하지 않는 키는 삭제

## 🚀 즉시 해결 방법

1. **현재 키 확인**:
   ```bash
   ssh -i "C:\DEV\autocoin-new-key.pem" ec2-user@54.175.66.222
   ```

2. **연결 성공 시**: 
   - GitHub Secrets `EC2_SSH_KEY`에 `autocoin-new-key.pem` 내용 설정

3. **연결 실패 시**:
   - AWS 콘솔에서 EC2 인스턴스의 키 페어 이름 확인
   - 해당 키 다운로드하여 GitHub Secrets 설정

## 🔧 최종 목표

- **하나의 작동하는 키**만 유지
- **GitHub Actions SSH 연결 성공**
- **자동 배포 정상화**
