#!/bin/bash

echo "Building Rust optimizer library..."

# Build standard release (C FFI only)
echo "Building C FFI version..."
cargo build --release

# Build with JNI support (optional)
echo "Building JNI version..."
cargo build --release --features jni

echo "Build complete!"
echo ""
echo "Outputs:"
echo "  C FFI: target/release/libstrategic_mind_optimizer.so (Linux)"
echo "         target/release/libstrategic_mind_optimizer.dylib (macOS)"
echo "         target/release/strategic_mind_optimizer.dll (Windows)"
echo ""
echo "  JNI: Same as above, but with JNI bindings enabled"