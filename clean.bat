@echo off
echo Cleaning build artifacts...

if exist rust-optimizer\target (
    echo Cleaning Rust...
    cd rust-optimizer
    cargo clean
    cd ..
)

if exist python-ai\venv (
    echo Cleaning Python virtual environment...
    rmdir /s /q python-ai\venv
)

if exist java-frontend\target (
    echo Cleaning Java...
    cd java-frontend
    call mvn clean --quiet
    cd ..
)

if exist dist (
    echo Cleaning distribution...
    rmdir /s /q dist
)

if exist logs (
    echo Cleaning logs...
    rmdir /s /q logs
)

echo Clean complete!
```

## Passo 5.5: Criar Diretório de Logs

**File: `logs/.gitkeep`** (NOVO)
```
# This file keeps the logs directory in git
```

E adicionar ao `.gitignore`:

**File: `.gitignore`** (na raiz - NOVO ou ADICIONAR)
```
# Build artifacts
target/
dist/
*.jar
*.class

# Rust
rust-optimizer/target/
rust-optimizer/Cargo.lock

# Python
python-ai/venv/
python-ai/__pycache__/
python-ai/**/__pycache__/
*.pyc
*.pyo
*.egg-info/
python-ai/models/*.pkl

# Java
java-frontend/target/
java-frontend/.classpath
java-frontend/.project
java-frontend/.settings/

# Logs
logs/*.log

# IDE
.idea/
.vscode/
*.swp
*.swo
*~

# OS
.DS_Store
Thumbs.db