#!/bin/bash

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log_step() {
    echo -e "${BLUE}==>${NC} ${1}"
}

log_success() {
    echo -e "${GREEN}✓${NC} ${1}"
}

log_error() {
    echo -e "${RED}✗${NC} ${1}"
}

echo "=========================================="
echo "Strategic Mind Games - Quick Start"
echo "=========================================="
echo ""

# Check if built
if [ ! -f "java-frontend/target/strategic-mind-games-1.0.0-jar-with-dependencies.jar" ]; then
    log_error "Game not built. Run ./build.sh first"
    exit 1
fi

# Check if Python AI is set up
if [ ! -d "python-ai/venv" ]; then
    log_error "Python environment not set up. Run ./build.sh first"
    exit 1
fi

# Start Python AI server in background
log_step "Starting AI Engine..."
cd python-ai
source venv/bin/activate
python -m src.api_server > ../logs/ai-server.log 2>&1 &
AI_PID=$!
cd ..

# Wait for server to start
sleep 3

if kill -0 $AI_PID 2>/dev/null; then
    log_success "AI Engine started (PID: $AI_PID)"
else
    log_error "Failed to start AI Engine"
    exit 1
fi

# Start Java application
log_step "Starting game..."
cd java-frontend

export LD_LIBRARY_PATH=../rust-optimizer/target/release:$LD_LIBRARY_PATH
export DYLD_LIBRARY_PATH=../rust-optimizer/target/release:$DYLD_LIBRARY_PATH

java -Djava.library.path=../rust-optimizer/target/release \
     -jar target/strategic-mind-games-1.0.0-jar-with-dependencies.jar

# Cleanup
log_step "Shutting down..."
kill $AI_PID 2>/dev/null || true
wait $AI_PID 2>/dev/null || true

log_success "Goodbye!"