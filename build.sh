#!/bin/bash

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BUILD_RUST=true
BUILD_PYTHON=true
BUILD_JAVA=true
TRAIN_MODEL=true
RUN_TESTS=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-rust)
            BUILD_RUST=false
            shift
            ;;
        --skip-python)
            BUILD_PYTHON=false
            shift
            ;;
        --skip-java)
            BUILD_JAVA=false
            shift
            ;;
        --skip-training)
            TRAIN_MODEL=false
            shift
            ;;
        --with-tests)
            RUN_TESTS=true
            shift
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  --skip-rust       Skip Rust optimizer build"
            echo "  --skip-python     Skip Python AI setup"
            echo "  --skip-java       Skip Java frontend build"
            echo "  --skip-training   Skip AI model training"
            echo "  --with-tests      Run tests after build"
            echo "  --help            Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Logging function
log_step() {
    echo -e "${BLUE}==>${NC} ${1}"
}

log_success() {
    echo -e "${GREEN}✓${NC} ${1}"
}

log_error() {
    echo -e "${RED}✗${NC} ${1}"
}

log_warning() {
    echo -e "${YELLOW}⚠${NC} ${1}"
}

# Check prerequisites
check_prerequisites() {
    log_step "Checking prerequisites..."
    
    local missing=0
    
    if [ "$BUILD_RUST" = true ]; then
        if ! command -v cargo &> /dev/null; then
            log_error "Rust/Cargo not found. Install from https://rustup.rs/"
            missing=$((missing + 1))
        else
            log_success "Rust found: $(rustc --version)"
        fi
    fi
    
    if [ "$BUILD_PYTHON" = true ]; then
        if ! command -v python3 &> /dev/null; then
            log_error "Python3 not found. Install Python 3.9+"
            missing=$((missing + 1))
        else
            log_success "Python found: $(python3 --version)"
        fi
        
        if ! command -v pip3 &> /dev/null; then
            log_error "pip3 not found. Install pip for Python3"
            missing=$((missing + 1))
        fi
    fi
    
    if [ "$BUILD_JAVA" = true ]; then
        if ! command -v mvn &> /dev/null; then
            log_error "Maven not found. Install from https://maven.apache.org/"
            missing=$((missing + 1))
        else
            log_success "Maven found: $(mvn --version | head -n 1)"
        fi
        
        if ! command -v java &> /dev/null; then
            log_error "Java not found. Install JDK 17+"
            missing=$((missing + 1))
        else
            log_success "Java found: $(java -version 2>&1 | head -n 1)"
        fi
    fi
    
    if [ $missing -gt 0 ]; then
        log_error "$missing prerequisite(s) missing"
        exit 1
    fi
    
    log_success "All prerequisites satisfied"
}

# Build Rust optimizer
build_rust() {
    log_step "Building Rust optimizer..."
    
    cd rust-optimizer
    
    # Clean previous builds
    if [ -d "target" ]; then
        log_step "Cleaning previous Rust build..."
        cargo clean
    fi
    
    # Build release version
    log_step "Compiling Rust library (release mode)..."
    cargo build --release
    
    if [ $? -eq 0 ]; then
        log_success "Rust build successful"
        
        # Show library details
        if [ -f "target/release/libstrategic_mind_optimizer.so" ]; then
            ls -lh target/release/libstrategic_mind_optimizer.so
        elif [ -f "target/release/libstrategic_mind_optimizer.dylib" ]; then
            ls -lh target/release/libstrategic_mind_optimizer.dylib
        elif [ -f "target/release/strategic_mind_optimizer.dll" ]; then
            ls -lh target/release/strategic_mind_optimizer.dll
        fi
        
        # Run tests if requested
        if [ "$RUN_TESTS" = true ]; then
            log_step "Running Rust tests..."
            cargo test --release
        fi
    else
        log_error "Rust build failed"
        cd ..
        exit 1
    fi
    
    cd ..
}

# Setup Python environment
setup_python() {
    log_step "Setting up Python AI environment..."
    
    cd python-ai
    
    # Create virtual environment if it doesn't exist
    if [ ! -d "venv" ]; then
        log_step "Creating Python virtual environment..."
        python3 -m venv venv
        log_success "Virtual environment created"
    else
        log_step "Using existing virtual environment"
    fi
    
    # Activate virtual environment
    source venv/bin/activate
    
    # Upgrade pip
    log_step "Upgrading pip..."
    pip install --upgrade pip --quiet
    
    # Install dependencies
    log_step "Installing Python dependencies..."
    pip install -r requirements.txt --quiet
    
    if [ $? -eq 0 ]; then
        log_success "Python dependencies installed"
    else
        log_error "Failed to install Python dependencies"
        deactivate
        cd ..
        exit 1
    fi
    
    # Train model if requested
    if [ "$TRAIN_MODEL" = true ]; then
        log_step "Training AI models (this may take a few minutes)..."
        python -m src.training_pipeline
        
        if [ $? -eq 0 ]; then
            log_success "AI model training complete"
            
            # Show model files
            if [ -d "models" ]; then
                log_step "Model files:"
                ls -lh models/
            fi
        else
            log_error "AI model training failed"
            deactivate
            cd ..
            exit 1
        fi
    else
        log_warning "Skipping AI model training"
    fi
    
    deactivate
    cd ..
}

# Build Java frontend
build_java() {
    log_step "Building Java frontend..."
    
    cd java-frontend
    
    # Clean previous builds
    log_step "Cleaning previous Java build..."
    mvn clean --quiet
    
    # Compile
    log_step "Compiling Java sources..."
    mvn compile --quiet
    
    if [ $? -ne 0 ]; then
        log_error "Java compilation failed"
        cd ..
        exit 1
    fi
    
    log_success "Java compilation successful"
    
    # Run tests if requested
    if [ "$RUN_TESTS" = true ]; then
        log_step "Running Java tests..."
        mvn test
    fi
    
    # Package
    log_step "Packaging JAR files..."
    mvn package --quiet
    
    if [ $? -eq 0 ]; then
        log_success "Java build successful"
        
        # Show JAR details
        log_step "Build artifacts:"
        ls -lh target/*.jar 2>/dev/null || log_warning "No JAR files found"
    else
        log_error "Java packaging failed"
        cd ..
        exit 1
    fi
    
    cd ..
}

# Create distribution package
create_distribution() {
    log_step "Creating distribution package..."
    
    DIST_DIR="dist"
    rm -rf "$DIST_DIR"
    mkdir -p "$DIST_DIR"
    
    # Copy Java JAR
    if [ -f "java-frontend/target/strategic-mind-games-1.0.0-jar-with-dependencies.jar" ]; then
        cp java-frontend/target/strategic-mind-games-1.0.0-jar-with-dependencies.jar "$DIST_DIR/"
        log_success "Copied Java JAR"
    fi
    
    # Copy Rust library
    if [ -f "rust-optimizer/target/release/libstrategic_mind_optimizer.so" ]; then
        mkdir -p "$DIST_DIR/lib"
        cp rust-optimizer/target/release/libstrategic_mind_optimizer.so "$DIST_DIR/lib/"
        log_success "Copied Rust library (Linux)"
    elif [ -f "rust-optimizer/target/release/libstrategic_mind_optimizer.dylib" ]; then
        mkdir -p "$DIST_DIR/lib"
        cp rust-optimizer/target/release/libstrategic_mind_optimizer.dylib "$DIST_DIR/lib/"
        log_success "Copied Rust library (macOS)"
    elif [ -f "rust-optimizer/target/release/strategic_mind_optimizer.dll" ]; then
        mkdir -p "$DIST_DIR/lib"
        cp rust-optimizer/target/release/strategic_mind_optimizer.dll "$DIST_DIR/lib/"
        log_success "Copied Rust library (Windows)"
    fi
    
    # Copy Python AI
    mkdir -p "$DIST_DIR/python-ai"
    cp -r python-ai/src "$DIST_DIR/python-ai/"
    cp -r python-ai/models "$DIST_DIR/python-ai/" 2>/dev/null || log_warning "No trained models found"
    cp python-ai/requirements.txt "$DIST_DIR/python-ai/"
    cp python-ai/config.yaml "$DIST_DIR/python-ai/" 2>/dev/null || true
    log_success "Copied Python AI"
    
    # Create run script
    cat > "$DIST_DIR/run.sh" << 'EOF'
#!/bin/bash
cd "$(dirname "$0")"
export LD_LIBRARY_PATH=./lib:$LD_LIBRARY_PATH
java -Djava.library.path=./lib -jar strategic-mind-games-1.0.0-jar-with-dependencies.jar
EOF
    chmod +x "$DIST_DIR/run.sh"
    log_success "Created run script"
    
    log_success "Distribution package created in: $DIST_DIR/"
}

# Main build process
main() {
    echo "=========================================="
    echo "Strategic Mind Games - Build Script"
    echo "=========================================="
    echo ""
    
    # Check prerequisites
    check_prerequisites
    echo ""
    
    # Build components
    if [ "$BUILD_RUST" = true ]; then
        build_rust
        echo ""
    fi
    
    if [ "$BUILD_PYTHON" = true ]; then
        setup_python
        echo ""
    fi
    
    if [ "$BUILD_JAVA" = true ]; then
        build_java
        echo ""
    fi
    
    # Create distribution
    create_distribution
    echo ""
    
    # Summary
    echo "=========================================="
    echo -e "${GREEN}Build Complete!${NC}"
    echo "=========================================="
    echo ""
    echo "To run the game:"
    echo "  cd dist"
    echo "  ./run.sh"
    echo ""
    echo "Or directly:"
    echo "  cd java-frontend"
    echo "  java -jar target/strategic-mind-games-1.0.0-jar-with-dependencies.jar"
    echo ""
}

# Run main
main