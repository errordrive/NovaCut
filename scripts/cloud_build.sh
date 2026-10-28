#!/usr/bin/env bash
set -euo pipefail

# Load token from environment or /home/.env
if [ -z "${GH_TOKEN:-}" ]; then
  if [ -f "/home/.env" ]; then
    export GH_TOKEN=$(grep -E '^GITHUB_TOKEN=' /home/.env | cut -d '=' -f2)
  fi
fi

if [ -z "${GH_TOKEN:-}" ]; then
  echo "ERROR: GH_TOKEN or GITHUB_TOKEN is not set!"
  exit 1
fi

REPO="errordrive/NovaCut"
BRANCH="main"
COMMIT_MSG="${1:-build: trigger cloud build on GitHub Actions}"

PROJECT_DIR="/storage/emulated/0/ApkLab/CapCutREA/pro-cut-android"
ARTIFACTS_DIR="/storage/emulated/0/ApkLab/CapCutREA/cloud-build-artifacts"

cd "$PROJECT_DIR"

echo "=== 1. Checking git status ==="
if [ -n "$(git status --porcelain)" ]; then
  echo "Staging and committing local changes..."
  git add -A
  git commit -m "$COMMIT_MSG"
fi

echo "=== 2. Pushing to GitHub ($REPO:$BRANCH) ==="
git push origin "$BRANCH"

echo "=== 3. Waiting for GitHub Actions workflow to trigger ==="
sleep 6

RUN_ID=""
for i in {1..15}; do
  RUN_ID=$(gh run list --repo "$REPO" --branch "$BRANCH" --limit 1 --json databaseId -q '.[0].databaseId' || true)
  if [ -n "$RUN_ID" ]; then
    echo "Found active workflow run ID: $RUN_ID"
    break
  fi
  sleep 2
done

if [ -z "$RUN_ID" ]; then
  echo "ERROR: Failed to detect workflow run!"
  exit 1
fi

echo "=== 4. Watching Cloud Build ($RUN_ID) ==="
gh run watch "$RUN_ID" --repo "$REPO" --exit-status

echo "=== 5. Downloading Cloud Build Artifacts ==="
mkdir -p "$ARTIFACTS_DIR"
rm -f "$ARTIFACTS_DIR/app-debug.apk"
gh run download "$RUN_ID" --repo "$REPO" --name "novacut-debug-apk" --dir "$ARTIFACTS_DIR"

APK_PATH="$ARTIFACTS_DIR/app-debug.apk"
if [ -f "$APK_PATH" ]; then
  echo "=== 6. Cloud Build Succeeded! ==="
  ls -lh "$APK_PATH"
  sha256sum "$APK_PATH"
  # Copy to workspace root for convenience
  cp "$APK_PATH" "/storage/emulated/0/ApkLab/CapCutREA/ProCut-debug.apk"
  echo "Artifact copied to: /storage/emulated/0/ApkLab/CapCutREA/ProCut-debug.apk"
else
  echo "ERROR: Artifact app-debug.apk was not found after download!"
  exit 1
fi
