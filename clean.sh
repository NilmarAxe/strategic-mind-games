#!/bin/bash

echo "Cleaning build artifacts..."

# Rust
if [ -d "rust-optimizer/target" ]; then
    echo "Cleaning Rust..."
    cd rust-optimizer
    cargo clean
    cd ..
fi

# Python
if [ -d "python-ai/venv" ]; then
    echo "Cleaning Python virtual environment..."
    rm -rf python-ai/venv
fi

if [ -d "python-ai/__pycache__" ]; then
    rm -rf python-ai/__pycache__
fi

find python-ai -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
find python-ai -type f -name "*.pyc" -delete 2>/dev/null || true

# Java
if [ -d "java-frontend/target" ]; then
    echo "Cleaning Java..."
    cd java-frontend
    mvn clean --quiet
    cd ..
fi

# Distribution
if [ -d "dist" ]; then
    echo "Cleaning distribution..."
    rm -rf dist
fi

# Logs
if [ -d "logs" ]; then
    echo "Cleaning logs..."
    rm -rf logs
fi

echo "Clean complete!"