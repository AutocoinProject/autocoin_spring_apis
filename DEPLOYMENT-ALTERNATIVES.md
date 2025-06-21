# EC2 Auto-Update 설정 가이드

## 🔄 SSH 문제 해결을 위한 3가지 대안

### 1. AWS Systems Manager (SSM) 사용 (추천)

SSH 대신 AWS SSM을 사용하여 EC2에 명령 실행:

**장점:**
- SSH 키 문제 해결
- AWS 네이티브 서비스 
- 로그 및 상태 추적 가능

**설정 방법:**
1. EC2에 SSM Agent 설치 (Amazon Linux 2023은 기본 설치됨)
2. EC2에 SSM 역할 부여: `AmazonSSMManagedInstanceCore`
3. GitHub Secrets 추가:
   ```
   AWS_ACCESS_KEY_ID=<액세스 키>
   AWS_SECRET_ACCESS_KEY=<시크릿 키>
   EC2_INSTANCE_ID=i-09f9b0cd2b6a81741
   ```

**사용 파일:** `.github/workflows/ci-ssm.yml`

### 2. 새로운 SSH 키 생성

현재 SSH 키에 문제가 있으므로 새로운 키 페어 생성:

**설정 방법:**
1. GitHub Actions에서 "SSH Key Troubleshooting" 워크플로우 실행
2. 생성된 Public Key를 EC2의 `~/.ssh/authorized_keys`에 추가
3. Private Key를 GitHub Secrets `EC2_SSH_KEY`에 설정

### 3. EC2 자동 업데이트 (Cron 방식)

EC2에서 주기적으로 최신 이미지를 확인하고 자동 배포:

**EC2 설정:**
```bash
# 1. 스크립트 복사
sudo cp auto-update.sh /home/ec2-user/
chmod +x /home/ec2-user/auto-update.sh

# 2. 환경변수 설정
echo "export GITHUB_TOKEN=<GitHub Token>" >> ~/.bashrc
echo "export SLACK_WEBHOOK_URL=<Slack URL>" >> ~/.bashrc
source ~/.bashrc

# 3. Cron 작업 추가 (5분마다 확인)
crontab -e
# 다음 라인 추가:
*/5 * * * * /home/ec2-user/auto-update.sh >> /home/ec2-user/update.log 2>&1
```

## 🎯 추천 방법

**가장 추천: AWS SSM 방식**
- 가장 안정적이고 AWS 네이티브
- SSH 키 문제 완전히 해결
- 로그 및 모니터링 우수

## 📝 다음 단계

1. **SSM 방식 시도**: `ci-ssm.yml` 워크플로우 사용
2. **필요 시 SSH 키 재생성**: 기존 키에 문제가 있는 경우
3. **백업 방법**: EC2 cron 자동 업데이트

어떤 방식을 선택하시겠습니까?
