#!/usr/bin/env bash
# ==============================================================================
# Social Shuffle Pune — Automated Backend Sync to GitHub
# Target Repository: https://github.com/Parimal0210/Social-Shuffle-Backend.git
# ==============================================================================
set -e

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$BACKEND_DIR"

REPO_OWNER="Parimal0210"
REPO_NAME="Social-Shuffle-Backend"
BRANCH="main"
REPO_HTTP_URL="https://github.com/${REPO_OWNER}/${REPO_NAME}.git"

echo "=========================================================="
echo "  Social Shuffle Pune — Push Backend to GitHub"
echo "  Target: ${REPO_HTTP_URL}"
echo "=========================================================="

# 1. Retrieve Personal Access Token
# Order of precedence:
#   a) First command line argument: ./push_to_github.sh <TOKEN>
#   b) GITHUB_TOKEN environment variable
#   c) Local .github_token file (git-ignored)
PAT="${1:-$GITHUB_TOKEN}"

if [ -z "$PAT" ] && [ -f "$BACKEND_DIR/.github_token" ]; then
  PAT="$(cat "$BACKEND_DIR/.github_token" | tr -d '\r\n[:space:]')"
fi

if [ -z "$PAT" ] && [ -f "$BACKEND_DIR/../.env" ]; then
  ENV_TOKEN="$(grep -E '^GITHUB_TOKEN=' "$BACKEND_DIR/../.env" | cut -d '=' -f2- | tr -d '\"'"'[:space:]" || true)"
  if [ -n "$ENV_TOKEN" ]; then
    PAT="$ENV_TOKEN"
  fi
fi

if [ -z "$PAT" ]; then
  echo ""
  echo "⚠️  GitHub Authentication Token Required"
  echo "----------------------------------------------------------"
  echo "To push code to GitHub (https://github.com/${REPO_OWNER}/${REPO_NAME}),"
  echo "provide your GitHub Personal Access Token (classic with 'repo' scope,"
  echo "or fine-grained with 'Contents: Read and write'):"
  echo ""
  echo "Option 1: Pass token directly as an argument:"
  echo "  ./push_to_github.sh <YOUR_GITHUB_PAT_TOKEN>"
  echo ""
  echo "Option 2: Set environment variable:"
  echo "  export GITHUB_TOKEN=<YOUR_GITHUB_PAT_TOKEN>"
  echo "  ./push_to_github.sh"
  echo ""
  echo "Option 3: Save to a local token file (auto git-ignored):"
  echo "  echo 'your_github_token' > backend-spring-boot-mongodb/.github_token"
  echo "  ./push_to_github.sh"
  echo "----------------------------------------------------------"
  exit 1
fi

# 2. Ensure Git repository is healthy
if [ ! -d ".git" ]; then
  echo "Initializing local repository..."
  git init -b "$BRANCH"
fi

git config user.name "Parimal Shete"
git config user.email "parimalmshete@gmail.com"

# 3. Stage and commit any unstaged modifications
if [ -n "$(git status --porcelain)" ]; then
  echo "Staging backend changes..."
  git add -A
  COMMIT_MSG="${2:-feat(backend): update Social Shuffle backend codebase $(date -u +"%Y-%m-%d %H:%M:%S UTC")}"
  echo "Committing with message: '$COMMIT_MSG'..."
  git commit -m "$COMMIT_MSG"
else
  echo "Working tree is clean. Current commit:"
  git log -n 1 --oneline
fi

# 4. Configure authenticated remote securely
git remote remove origin 2>/dev/null || true
git remote add origin "https://${REPO_OWNER}:${PAT}@github.com/${REPO_OWNER}/${REPO_NAME}.git"

# 5. Push to GitHub
echo "Pushing latest commits to GitHub ($BRANCH)..."
git push -u origin "$BRANCH"

# Clean up remote URL to not leave token stored in .git/config
git remote set-url origin "$REPO_HTTP_URL"

echo ""
echo "=========================================================="
echo "✅ Successfully synced backend code to GitHub!"
echo "   Repository: $REPO_HTTP_URL"
echo "   Latest Commit: $(git rev-parse --short HEAD)"
echo "=========================================================="
