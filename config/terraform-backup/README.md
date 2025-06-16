# 🗂️ Terraform Configuration (Backup)

이 폴더는 AWS ECS 기반 인프라를 위한 Terraform 설정의 백업입니다.

## 📋 **현재 상황**
- **사용 중**: ❌ 현재 사용되지 않음
- **배포 방식**: EC2 + Docker (GitHub Actions)
- **보관 이유**: 향후 ECS 마이그레이션 시 참고용

## 📄 **포함된 파일**

### [`main.tf`](main.tf)
- **용도**: AWS ECS + ALB + Auto Scaling 전체 인프라
- **포함 구성**:
  - VPC, Subnet, Internet Gateway
  - Application Load Balancer
  - ECS Cluster + Fargate
  - Auto Scaling Group
  - Security Groups
  - CloudWatch Logs

## 🔄 **현재 vs Terraform 배포 방식**

| 구분 | 현재 (EC2 + Docker) | Terraform (ECS + ALB) |
|------|---------------------|----------------------|
| **인프라** | 단일 EC2 인스턴스 | ECS Fargate + ALB |
| **확장성** | 수동 스케일링 | 자동 스케일링 |
| **가용성** | Single AZ | Multi AZ |
| **비용** | 낮음 (~$30/월) | 높음 (~$100/월) |
| **관리** | 간단 | 복잡 |
| **배포** | GitHub Actions → EC2 | GitHub Actions → ECS |

## 🚀 **ECS로 마이그레이션하려면**

### 1️⃣ **Terraform 준비**
```bash
# Terraform 설치
brew install terraform  # macOS
# 또는 choco install terraform  # Windows

# AWS CLI 설정
aws configure
```

### 2️⃣ **Terraform 배포**
```bash
cd config/terraform-backup/

# 초기화
terraform init

# 계획 확인
terraform plan

# 인프라 생성
terraform apply
```

### 3️⃣ **ECR 리포지토리 생성**
```bash
# ECR 리포지토리 생성
aws ecr create-repository --repository-name autocoin

# Docker 이미지 빌드 & 푸시
docker build -t autocoin .
docker tag autocoin:latest {account-id}.dkr.ecr.ap-northeast-2.amazonaws.com/autocoin:latest
docker push {account-id}.dkr.ecr.ap-northeast-2.amazonaws.com/autocoin:latest
```

### 4️⃣ **ECS 배포 자동화**
GitHub Actions 워크플로우를 ECS 배포용으로 수정 필요

## ⚠️ **주의사항**

### 💰 **비용 고려**
- **ECS Fargate**: 시간당 과금
- **ALB**: 시간당 + 요청당 과금
- **NAT Gateway**: 시간당 + 데이터 전송 과금
- **예상 비용**: 월 $80-150 (트래픽에 따라)

### 🔧 **복잡성**
- Terraform 학습 필요
- AWS 네트워킹 지식 필요
- 더 많은 설정 관리 포인트

## 🎯 **권장사항**

### 현재 유지 (EC2)가 좋은 경우:
- 💰 비용 절약이 중요한 경우
- 🏃‍♂️ 빠른 개발/배포가 필요한 경우
- 👨‍💻 소규모 팀인 경우
- 📊 트래픽이 예측 가능한 경우

### ECS 마이그레이션이 좋은 경우:
- 📈 트래픽 변동이 큰 경우
- 🔄 자동 스케일링이 필요한 경우
- 🏢 엔터프라이즈급 안정성이 필요한 경우
- 👥 DevOps 전문 인력이 있는 경우

---

**💡 결론**: 현재 프로젝트 규모라면 **EC2 + Docker 방식**이 더 적합합니다. ECS는 나중에 트래픽이 증가했을 때 고려해보세요!
