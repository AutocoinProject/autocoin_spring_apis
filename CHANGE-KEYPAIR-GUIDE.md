# EC2 키 페어 변경 가이드

## 🔄 spring_key → ec2_key 변경 방법

### ⚠️ 중요사항
EC2 인스턴스의 키 페어는 **생성 후 변경 불가**합니다.
하지만 **추가 키**를 설정할 수 있습니다.

## 🛠️ 방법 1: 새 공개키 추가 (권장)

### 1단계: 현재 키로 EC2 접속
```bash
# 현재 작동하는 키로 접속
ssh -i "C:\DEV\autocoin-new-key.pem" ec2-user@54.175.66.222
```

### 2단계: ec2_key의 공개키 확인
AWS 콘솔에서 ec2_key의 공개키를 확인하거나, 로컬에 ec2_key.pem이 있다면:
```bash
# 공개키 추출
ssh-keygen -y -f ec2_key.pem
```

### 3단계: EC2에서 새 공개키 추가
```bash
# EC2에서 실행
echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQ... ec2_key의_공개키" >> ~/.ssh/authorized_keys

# 권한 확인
chmod 600 ~/.ssh/authorized_keys
chmod 700 ~/.ssh
```

### 4단계: 새 키로 테스트
```bash
# 로컬에서 새 키로 접속 테스트
ssh -i ec2_key.pem ec2-user@54.175.66.222
```

## 🛠️ 방법 2: 인스턴스 재생성 (완전 변경)

### 장점
- 완전히 새로운 키 페어 사용
- 깔끔한 설정

### 단점
- **데이터 손실 위험**
- 다운타임 발생
- 복잡한 설정 재구성

### 과정
1. **현재 데이터 백업**
2. **AMI(이미지) 생성**
3. **새 인스턴스 시작** (ec2_key 선택)
4. **데이터 복원**
5. **Elastic IP 재할당**

## 🛠️ 방법 3: 키 파일 교체 (실용적)

### AWS 콘솔에서 ec2_key.pem 다운로드
1. **EC2 콘솔** → **키 페어** → `ec2_key` 선택
2. **작업** → **키 페어 다운로드** (처음 생성 시에만 가능)

⚠️ **주의**: 키 페어는 생성 시에만 다운로드 가능합니다.

## 🎯 권장 솔루션

### 즉시 적용 가능한 방법

1. **방법 1 사용**: 기존 인스턴스에 새 공개키 추가
2. **GitHub Secrets 업데이트**: 새 ec2_key 내용으로 설정
3. **기존 키 제거**: 작동 확인 후 spring_key 공개키 제거

### 단계별 실행
```bash
# 1. 현재 키로 접속
ssh -i "C:\DEV\autocoin-new-key.pem" ec2-user@54.175.66.222

# 2. 현재 authorized_keys 백업
cp ~/.ssh/authorized_keys ~/.ssh/authorized_keys.backup

# 3. 새 공개키 추가 (ec2_key의 공개키)
echo "새_공개키_내용" >> ~/.ssh/authorized_keys

# 4. 권한 확인
chmod 600 ~/.ssh/authorized_keys

# 5. 새 키로 테스트
exit
ssh -i ec2_key.pem ec2-user@54.175.66.222

# 6. 성공 시 기존 키 제거 (선택사항)
# nano ~/.ssh/authorized_keys  # spring_key 공개키 라인 삭제
```

## 🚨 백업 계획

문제 발생 시 복구 방법:
```bash
# authorized_keys 복원
cp ~/.ssh/authorized_keys.backup ~/.ssh/authorized_keys
```

## ❓ ec2_key.pem 파일이 없다면?

1. **새 키 페어 생성**: AWS 콘솔에서 새로운 키 페어 생성
2. **방법 1 적용**: 새 공개키를 authorized_keys에 추가
3. **GitHub Secrets 업데이트**: 새 키로 설정
