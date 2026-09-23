#!/usr/bin/env bash
# Social Shuffle - Push Backend to GitHub Script
set -e

REPO_NAME="Social-Shuffle-Backend"

echo "=========================================================="
echo "  Social Shuffle Pune — Push Backend to GitHub ($REPO_NAME)"
echo "=========================================================="

if [ -z "$1" ] && [ -z "$GITHUB_TOKEN" ]; then
  echo ""
  echo "Usage:"
  echo "  Option A (Direct Push with GitHub Token):"
  echo "    ./push_to_github.sh <YOUR_GITHUB_USERNAME> <YOUR_GITHUB_TOKEN>"
  echo ""
  echo "  Option B (Using existing remote URL):"
  echo "    git remote add origin https://github.com/<YOUR_USERNAME>/$REPO_NAME.git"
  echo "    git push -u origin main"
  echo ""
  exit 1
fi

GITHUB_USER="$1"
GITHUB_PAT="$2"

if [ -n "$GITHUB_PAT" ]; then
  echo "1. Checking/Creating repository '$REPO_NAME' under user '$GITHUB_USER'..."
  curl -s -H "Authorization: token $GITHUB_PAT" \
       -H "Accept: application/vnd.github.v3+json" \
       https://api.github.com/user/repos \
       -d "{\"name\":\"$REPO_NAME\",\"private\":false,\"description\":\"Social Shuffle Pune - Spring Boot 4 & MongoDB Backend REST APIs\"}" > /dev/null || true

  echo "2. Setting remote origin..."
  git remote remove origin 2>/dev/null || true
  git remote add origin "https://${GITHUB_USER}:${GITHUB_PAT}@github.com/${GITHUB_USER}/${REPO_NAME}.git"

  echo "3. Pushing branch 'main' to GitHub..."
  git push -u origin main

  echo ""
  echo "✅ Successfully pushed to https://github.com/${GITHUB_USER}/${REPO_NAME}"
fi
