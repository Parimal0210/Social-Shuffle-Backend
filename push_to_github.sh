#!/usr/bin/env bash
# Social Shuffle - Push Backend to GitHub Script
set -e

REPO_URL="https://github.com/Parimal0210/Social-Shuffle-Backend.git"
REPO_NAME="Social-Shuffle-Backend"
GITHUB_USER="Parimal0210"

echo "=========================================================="
echo "  Social Shuffle Pune — Push Backend to GitHub ($REPO_NAME)"
echo "=========================================================="

PAT="${1:-$GITHUB_TOKEN}"

if [ -z "$PAT" ]; then
  echo ""
  echo "Usage:"
  echo "  ./push_to_github.sh <YOUR_GITHUB_PERSONAL_ACCESS_TOKEN>"
  echo "  or export GITHUB_TOKEN=<YOUR_TOKEN> and run ./push_to_github.sh"
  echo ""
  exit 1
fi

echo "1. Configuring authenticated remote..."
git remote remove origin 2>/dev/null || true
git remote add origin "https://${GITHUB_USER}:${PAT}@github.com/${GITHUB_USER}/${REPO_NAME}.git"

echo "2. Pushing main branch to $REPO_URL..."
git push -u origin main --force

echo ""
echo "✅ Successfully pushed to $REPO_URL!"
