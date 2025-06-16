#!/bin/bash

# Git에서 민감한 정보가 포함된 파일들을 제거하는 스크립트

echo "=== Git에서 민감한 파일들 제거 시작 ==="

# 1. 파일들을 Git 추적에서 제거 (파일은 유지하되 Git에서만 제거)
echo "Git 추적에서 민감한 파일들 제거 중..."
git rm --cached .env.ec2 2>/dev/null || echo ".env.ec2 파일이 이미 추적되지 않음"
git rm --cached scripts/set-env-ec2.sh 2>/dev/null || echo "set-env-ec2.sh 파일이 이미 추적되지 않음"
git rm --cached scripts/deploy-ec2.sh 2>/dev/null || echo "deploy-ec2.sh 파일이 이미 추적되지 않음"
git rm --cached docker/docker-compose.ec2-fixed.yml 2>/dev/null || echo "docker-compose.ec2-fixed.yml 파일이 이미 추적되지 않음"

# 2. .gitignore 변경사항 추가
git add .gitignore

# 3. 커밋
git commit -m "chore: Add sensitive files to .gitignore and remove from tracking

- Add .env.ec2 to .gitignore
- Add deployment scripts with secrets to .gitignore
- Remove sensitive files from Git tracking while keeping them locally"

echo "=== 완료 ==="
echo "이제 안전하게 push할 수 있습니다."
