#!/bin/bash

# 빠른 업데이트 배포 (Quick Deploy)

echo "🚀 빠른 배포 시작..."

# Git 최신 코드 가져오기
git pull origin main

# 환경변수 설정 (있다면)
[ -f "scripts/set-env-ec2.sh" ] && source scripts/set-env-ec2.sh

# .env 파일 적용 (있다면)
[ -f ".env.ec2" ] && cp .env.ec2 .env

# 컨테이너 재시작
docker-compose -f docker/docker-compose.ec2-fixed.yml up --build -d

echo "✅ 배포 완료!"
