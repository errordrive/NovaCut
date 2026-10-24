#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_SRC="$PROJECT_ROOT/app/src/main/java"
TEST_SRC="$PROJECT_ROOT/tests/src"
BUILD_BIN="$PROJECT_ROOT/tests/bin"

echo "=== Compiling NovaCut Video Editor & Test Suite ==="
rm -rf "$BUILD_BIN"
mkdir -p "$BUILD_BIN"

# Find core java files and tests (excluding Android framework UI Activity)
JAVA_FILES=$(find "$APP_SRC/com/novacut/model" "$APP_SRC/com/novacut/engine" "$APP_SRC/com/novacut/history" "$APP_SRC/com/novacut/ui" "$TEST_SRC" -name "*.java")

echo "Source files to compile:"
echo "$JAVA_FILES"

javac -encoding UTF-8 -d "$BUILD_BIN" $JAVA_FILES

echo "=== Compilation Succeeded ==="
echo "=== Running Automated Test Suite ==="

java -cp "$BUILD_BIN" com.novacut.tests.VideoEditorTestSuite
