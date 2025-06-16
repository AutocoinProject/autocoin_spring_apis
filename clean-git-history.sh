#!/bin/bash

# Git 히스토리에서 민감한 정보 완전 제거

echo "🔥 Git 히스토리에서 민감한 정보 완전 제거 시작..."

# 1. Git filter-branch로 특정 파일들을 히스토리에서 완전 삭제
echo "📂 민감한 파일들을 Git 히스토리에서 완전 제거 중..."

git filter-branch --force --index-filter \
  'git rm --cached --ignore-unmatch scripts/manual-deploy.sh scripts/set-env-ec2.sh' \
  --prune-empty --tag-name-filter cat -- --all

# 2. 참조 정리
echo "🧹 참조 정리 중..."
git for-each-ref --format="%(refname)" refs/original/ | xargs -n 1 git update-ref -d

# 3. 로그와 참조 만료
echo "🗑️ 로그 정리 중..."
git reflog expire --expire=now --all

# 4. 가비지 컬렉션으로 완전 정리
echo "🔄 가비지 컬렉션 실행 중..."
git gc --prune=now --aggressive

echo "✅ Git 히스토리 정리 완료!"
echo ""
echo "⚠️  중요: 이제 강제 푸시가 필요합니다:"
echo "git push origin --force --all"
echo ""
echo "❗ 주의: 이 명령은 Git 히스토리를 변경하므로 팀원들과 조율이 필요합니다."
