#!/bin/bash

echo "=========================================="
echo "Rust Native Library Verification"
echo "=========================================="

# Check if Rust library exists
RUST_LIB="../rust-optimizer/target/release"

if [ -d "$RUST_LIB" ]; then
    echo "✓ Rust build directory found"
    
    if [ -f "$RUST_LIB/libstrategic_mind_optimizer.so" ] || \
       [ -f "$RUST_LIB/libstrategic_mind_optimizer.dylib" ] || \
       [ -f "$RUST_LIB/strategic_mind_optimizer.dll" ]; then
        echo "✓ Native library found"
        ls -lh $RUST_LIB/libstrategic_mind_optimizer.* 2>/dev/null || \
        ls -lh $RUST_LIB/strategic_mind_optimizer.* 2>/dev/null
    else
        echo "✗ Native library NOT found"
        echo "  Run: cd ../rust-optimizer && cargo build --release"
        exit 1
    fi
else
    echo "✗ Rust build directory NOT found"
    echo "  Run: cd ../rust-optimizer && cargo build --release"
    exit 1
fi

# Check Java can find the library
echo ""
echo "Testing Java JNI loading..."
mvn test -Dtest=RustBridgeTest#testIsAvailable -q

if [ $? -eq 0 ]; then
    echo "✓ JNI loading test passed"
else
    echo "✗ JNI loading test failed"
    exit 1
fi

echo ""
echo "=========================================="
echo "All checks passed!"
echo "=========================================="