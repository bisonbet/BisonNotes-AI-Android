#!/bin/bash
# Quick test validation script

echo "🔍 Checking test files..."

TEST_DIR="app/src/test/java/com/bisonnotesai/android"

if [ ! -d "$TEST_DIR" ]; then
    echo "❌ Test directory not found: $TEST_DIR"
    exit 1
fi

echo "✅ Test directory exists"

# Count test files
TEST_FILES=$(find "$TEST_DIR" -name "*Test.kt" | wc -l)
echo "📊 Found $TEST_FILES test files"

# List test files
echo ""
echo "📝 Test files:"
find "$TEST_DIR" -name "*Test.kt" -exec basename {} \;

echo ""
echo "📦 Project structure validated!"
echo ""
echo "To run tests when Gradle is set up:"
echo "  ./gradlew test"
echo ""
echo "Or use the test runner script:"
echo "  ./run-tests.sh"
