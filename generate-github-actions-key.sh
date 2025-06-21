#!/bin/bash

# GitHub Actions용 새 SSH 키 생성 및 설정 스크립트

echo "🔑 GitHub Actions용 새 SSH 키 생성..."

# 새 키 페어 생성
ssh-keygen -t ed25519 -f ./github_actions_key -N "" -C "github-actions-autocoin"

echo ""
echo "✅ 새 키 생성 완료!"
echo ""

echo "📋 1단계: 다음 공개키를 EC2에 추가하세요"
echo "================================================"
cat ./github_actions_key.pub
echo ""
echo "================================================"
echo ""

echo "📋 2단계: EC2에서 실행할 명령어"
echo "현재 키로 EC2 접속 후 다음 명령어 실행:"
echo "ssh -i \"C:\DEV\spring_key.pem\" ec2-user@54.175.66.222"
echo ""
echo "EC2에서 실행:"
echo "echo \"$(cat ./github_actions_key.pub)\" >> ~/.ssh/authorized_keys"
echo "chmod 600 ~/.ssh/authorized_keys"
echo ""

echo "📋 3단계: GitHub Secrets에 설정할 Private Key"
echo "================================================"
cat ./github_actions_key
echo ""
echo "================================================"
echo ""

echo "📋 4단계: 테스트"
echo "ssh -i ./github_actions_key ec2-user@54.175.66.222"
echo ""

echo "🎯 완료되면 GitHub Actions에서 새 키로 배포 가능!"
