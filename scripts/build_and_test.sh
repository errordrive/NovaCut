#!/bin/bash
set -e

PROJECT_ROOT="/storage/emulated/0/ApkLab/CapCutREA"
APP_SRC="$PROJECT_ROOT/app/src/main/java"
TEST_SRC="$PROJECT_ROOT/tests/src"
BUILD_BIN="$PROJECT_ROOT/tests/bin"

echo "=== Compiling NovaCut Video Editor & Test Suite ==="
rm -rf "$BUILD_BIN"
mkdir -p "$BUILD_BIN"

# Find all java files
JAVA_FILES=$(find "$APP_SRC" "$TEST_SRC" -name "*.java")

echo "Source files to compile:"
echo "$JAVA_FILES"

javac -encoding UTF-8 -d "$BUILD_BIN" $JAVA_FILES

echo "=== Compilation Succeeded ==="
echo "=== Running Automated Test Suite ==="

java -cp "$BUILD_BIN" com.novacut.tests.VideoEditorTestSuite
