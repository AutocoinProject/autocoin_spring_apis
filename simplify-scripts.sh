#!/bin/bash

# Scripts 폴더 대폭 정리 - 핵심만 남기기

echo "🧹 Scripts 폴더 대폭 정리 시작..."

# 1. 위험한 파일들 제거
echo "❌ 위험한 파일들 제거..."
git rm --cached scripts/manual-deploy.sh 2>/dev/null || true
git rm --cached scripts/set-env-ec2.sh 2>/dev/null || true

# 로컬에서도 삭제
rm -f scripts/manual-deploy.sh
rm -f scripts/set-env-ec2.sh
rm -f scripts/fix-git-secrets.sh

# 2. 중복/불필요한 파일들 제거
echo "🗑️ 중복 파일들 제거..."
rm -f scripts/manual-deploy-safe.sh
rm -f scripts/quick-deploy-safe.sh
rm -f scripts/update-deploy-safe.sh
rm -f scripts/deploy-ec2-safe.sh
rm -f scripts/set-env-ec2-safe.sh
rm -f scripts/deploy-check.sh
rm -f scripts/verify-deployment.sh
rm -f scripts/restart-services.sh
rm -f scripts/server-setup.sh
rm -f scripts/ssl-setup.sh
rm -f scripts/README.md

# 3. 핵심 파일들만 유지하고 개선
echo "✅ 핵심 파일들만 남기기..."

# 간단한 로컬 개발용
cat > scripts/start-local.bat << 'EOF'
@echo off
echo Starting Autocoin API in Local Mode...
docker-compose -f docker/docker-compose.yml up --build
EOF

# 간단한 개발 모드
cat > scripts/start-dev.bat << 'EOF'
@echo off
echo Starting Autocoin API in Development Mode...
set SPRING_PROFILES_ACTIVE=dev
gradlew bootRun
EOF

# 프로덕션 빌드
cat > scripts/build.sh << 'EOF'
#!/bin/bash
echo "Building Autocoin API for Production..."
./gradlew clean build -x test
echo "Build completed!"
EOF

# 간단한 배포 스크립트 (환경변수 기반)
cat > scripts/deploy.sh << 'EOF'
#!/bin/bash
set -e

echo "🚀 Deploying Autocoin API..."

# 환경변수 확인
if [ -z "$DB_PASSWORD" ] || [ -z "$JWT_SECRET" ]; then
    echo "❌ Required environment variables not set!"
    echo "Set: DB_PASSWORD, JWT_SECRET"
    exit 1
fi

# 컨테이너 재시작
docker-compose down
docker-compose up --build -d

# 헬스체크
echo "⏳ Waiting for application to start..."
sleep 15
curl -f http://localhost:8080/actuator/health || echo "❌ Health check failed"

echo "✅ Deployment completed!"
EOF

chmod +x scripts/build.sh
chmod +x scripts/deploy.sh

# 4. Git에 변경사항 추가
git add scripts/
git add -u scripts/

echo "✅ 정리 완료!"
echo ""
echo "📁 남은 파일들:"
echo "  scripts/start-local.bat   - 로컬 Docker 실행"
echo "  scripts/start-dev.bat     - 개발 모드 실행"
echo "  scripts/build.sh          - 프로덕션 빌드"
echo "  scripts/deploy.sh         - 간단한 배포"
echo ""
echo "커밋하려면: git commit -m \"simplify: Clean up scripts folder - keep only essentials\""
