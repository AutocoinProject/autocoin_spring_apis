#!/bin/bash

# 스크립트 실행 권한 부여
echo "📋 스크립트 실행 권한 설정 중..."

chmod +x scripts/deploy.sh
chmod +x scripts/server-setup.sh  
chmod +x scripts/ssl-setup.sh
chmod +x scripts/verify-deployment.sh
chmod +x scripts/deploy-ec2.sh

echo "✅ 모든 스크립트에 실행 권한이 부여되었습니다."

echo ""
echo "🚀 사용 가능한 스크립트:"
echo "  • ./scripts/deploy.sh          - 일반 배포 스크립트"
echo "  • ./scripts/deploy-ec2.sh      - EC2 Docker 배포 스크립트"
echo "  • ./scripts/server-setup.sh    - 서버 초기 설정"
echo "  • ./scripts/ssl-setup.sh       - SSL 인증서 설정"
echo "  • ./scripts/verify-deployment.sh - 배포 검증"
echo ""
echo "📚 배포 가이드:"
echo "  • docs/deployment/FINAL_DEPLOYMENT_GUIDE.md    - 완전한 배포 가이드"
echo "  • docs/deployment/EC2_DEPLOYMENT_GUIDE.md      - EC2 Docker 배포 가이드"
