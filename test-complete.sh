#!/bin/bash

echo "=========================================="
echo "Complete System Test"
echo "=========================================="
echo ""

# Test 1: Build
echo "Test 1: Building project..."
./build.sh --skip-training --with-tests

if [ $? -eq 0 ]; then
    echo "✓ Build successful"
else
    echo "✗ Build failed"
    exit 1
fi

echo ""

# Test 2: Check artifacts
echo "Test 2: Checking artifacts..."

ERRORS=0

if [ -f "rust-optimizer/target/release/libstrategic_mind_optimizer.so" ] || \
   [ -f "rust-optimizer/target/release/libstrategic_mind_optimizer.dylib" ] || \
   [ -f "rust-optimizer/target/release/strategic_mind_optimizer.dll" ]; then
    echo "✓ Rust library found"
else
    echo "⚠ Rust library not found (will use fallback)"
fi

if [ -d "python-ai/venv" ]; then
    echo "✓ Python environment found"
else
    echo "⚠ Python environment not found (will use fallback)"
fi

if [ -f "java-frontend/target/strategic-mind-games-1.0.0-jar-with-dependencies.jar" ]; then
    echo "✓ Java JAR found"
else
    echo "✗ Java JAR not found"
    ERRORS=$((ERRORS + 1))
fi

if [ $ERRORS -gt 0 ]; then
    echo "✗ Critical artifacts missing"
    exit 1
fi

echo ""

# Test 3: Java can run
echo "Test 3: Testing Java execution..."
cd java-frontend
java -version > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✓ Java runtime working"
else
    echo "✗ Java runtime failed"
    exit 1
fi
cd ..

echo ""
echo "=========================================="
echo "All tests passed!"
echo "=========================================="
echo ""
echo "The game is ready to run:"
echo "  ./run.sh"