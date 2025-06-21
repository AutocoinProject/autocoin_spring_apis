# SSH 키 문제 해결 가이드

## 🔑 새로운 SSH 키 생성 및 설정

### 1단계: 로컬에서 새 키 생성
```bash
# PowerShell에서 실행
ssh-keygen -t ed25519 -f autocoin-deploy-key -N ""
```

### 2단계: Public Key 확인
```bash
cat autocoin-deploy-key.pub
```

### 3단계: EC2에 Public Key 추가
현재 키로 EC2에 접속:
```bash
ssh -i "C:\DEV\autocoin-new-key.pem" ec2-user@54.175.66.222
```

EC2에서 실행:
```bash
# authorized_keys에 새 public key 추가
echo "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIxxxxx... (위에서 복사한 public key)" >> ~/.ssh/authorized_keys

# 권한 설정
chmod 600 ~/.ssh/authorized_keys
chmod 700 ~/.ssh
```

### 4단계: GitHub Secrets 업데이트
```bash
# Private Key 내용 복사
cat autocoin-deploy-key
```
이 내용을 GitHub Secrets `EC2_SSH_KEY`에 설정

### 5단계: 테스트
```bash
ssh -i autocoin-deploy-key ec2-user@54.175.66.222
```

## 🚀 빠른 해결책: 인스턴스 키 확인

EC2 콘솔에서 인스턴스의 키 페어 이름을 다시 확인하고,
올바른 키 파일을 사용하고 있는지 검증
