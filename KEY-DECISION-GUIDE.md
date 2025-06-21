# spring_key 문제 해결 vs ec2_key 변경

## 🔍 현재 상황
- EC2 인스턴스: `spring_key` 사용
- 로컬 파일: `autocoin-new-key.pem` (spring_key의 private key)
- 문제: GitHub Actions SSH 인증 실패

## 🎯 방법 1: 현재 spring_key 활용 (권장)

### 1단계: 로컬에서 SSH 연결 테스트
```bash
ssh -i "C:\DEV\autocoin-new-key.pem" ec2-user@54.175.66.222
```

### 2단계: 연결 결과에 따른 조치

#### A. 연결 성공하는 경우
```bash
# GitHub Secrets EC2_SSH_KEY에 설정할 내용 확인
cat C:\DEV\autocoin-new-key.pem
```
이 내용을 **정확히** GitHub Secrets `EC2_SSH_KEY`에 복사

#### B. 연결 실패하는 경우
spring_key에 문제가 있으므로 ec2_key로 변경 필요

## 🎯 방법 2: ec2_key로 변경

### 1단계: ec2_key.pem 파일 확인
ec2_key.pem 파일이 로컬에 있는지 확인

### 2단계: ec2_key 공개키를 EC2에 추가
```bash
# 1. 현재 키로 EC2 접속
ssh -i "C:\DEV\autocoin-new-key.pem" ec2-user@54.175.66.222

# 2. ec2_key 공개키 추가
# (ec2_key.pem에서 공개키 추출)
ssh-keygen -y -f ec2_key.pem > ec2_key.pub
cat ec2_key.pub >> ~/.ssh/authorized_keys

# 3. 권한 설정
chmod 600 ~/.ssh/authorized_keys

# 4. 새 키로 테스트
exit
ssh -i ec2_key.pem ec2-user@54.175.66.222
```

### 3단계: GitHub Secrets 업데이트
```bash
# ec2_key 내용을 GitHub Secrets에 설정
cat ec2_key.pem
```

## 🤔 어떤 방법을 선택할까?

### spring_key 계속 사용 (방법 1)
**장점**: 
- 간단함
- 현재 설정 유지
- 리스크 적음

**단점**: 
- SSH 문제의 근본 원인이 키가 아닐 수 있음

### ec2_key로 변경 (방법 2)
**장점**: 
- 새로운 키로 깔끔하게 시작
- 키 관리 일원화

**단점**: 
- 설정 변경 필요
- ec2_key.pem 파일 필요

## 🚀 권장 순서

1. **먼저 방법 1 시도**: 현재 키로 SSH 연결 테스트
2. **연결 성공 시**: GitHub Secrets만 정확히 설정
3. **연결 실패 시**: 방법 2로 ec2_key 추가

## ❓ 다음 단계 결정을 위한 질문

1. `ec2_key.pem` 파일이 로컬에 있나요?
2. 현재 `autocoin-new-key.pem`으로 EC2 SSH 접속이 되나요?
3. ec2_key로 변경하려는 특별한 이유가 있나요?
