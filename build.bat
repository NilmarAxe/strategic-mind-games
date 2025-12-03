@echo off
setlocal enabledelayedexpansion

:: Colors (limited in CMD)
set "RED=[91m"
set "GREEN=[92m"
set "YELLOW=[93m"
set "BLUE=[94m"
set "NC=[0m"

:: Configuration
set BUILD_RUST=1
set BUILD_PYTHON=1
set BUILD_JAVA=1
set TRAIN_MODEL=1
set RUN_TESTS=0

:: Parse arguments
:parse_args
if "%~1"=="" goto end_parse
if "%~1"=="--skip-rust" set BUILD_RUST=0
if "%~1"=="--skip-python" set BUILD_PYTHON=0
if "%~1"=="--skip-java" set BUILD_JAVA=0
if "%~1"=="--skip-training" set TRAIN_MODEL=0
if "%~1"=="--with-tests" set RUN_TESTS=1
if "%~1"=="--help" goto show_help
shift
goto parse_args
:end_parse

echo ==========================================
echo Strategic Mind Games - Build Script
echo ==========================================
echo.

:: Check prerequisites
echo Checking prerequisites...

if %BUILD_RUST%==1 (
    where cargo >nul 2>nul
    if errorlevel 1 (
        echo %RED%✗ Rust/Cargo not found%NC%
        exit /b 1
    )
    echo %GREEN%✓ Rust found%NC%
)

if %BUILD_PYTHON%==1 (
    where python >nul 2>nul
    if errorlevel 1 (
        echo %RED%✗ Python not found%NC%
        exit /b 1
    )
    echo %GREEN%✓ Python found%NC%
)

if %BUILD_JAVA%==1 (
    where mvn >nul 2>nul
    if errorlevel 1 (
        echo %RED%✗ Maven not found%NC%
        exit /b 1
    )
    echo %GREEN%✓ Maven found%NC%
    
    where java >nul 2>nul
    if errorlevel 1 (
        echo %RED%✗ Java not found%NC%
        exit /b 1
    )
    echo %GREEN%✓ Java found%NC%
)

echo.

:: Build Rust
if %BUILD_RUST%==1 (
    echo Building Rust optimizer...
    cd rust-optimizer
    cargo build --release
    if errorlevel 1 (
        echo %RED%✗ Rust build failed%NC%
        cd ..
        exit /b 1
    )
    echo %GREEN%✓ Rust build successful%NC%
    cd ..
    echo.
)

:: Setup Python
if %BUILD_PYTHON%==1 (
    echo Setting up Python environment...
    cd python-ai
    
    if not exist venv (
        echo Creating virtual environment...
        python -m venv venv
    )
    
    call venv\Scripts\activate.bat
    
    echo Installing dependencies...
    pip install -r requirements.txt --quiet
    
    if %TRAIN_MODEL%==1 (
        echo Training AI models...
        python -m src.training_pipeline
        if errorlevel 1 (
            echo %RED%✗ Model training failed%NC%
            deactivate
            cd ..
            exit /b 1
        )
        echo %GREEN%✓ Model training complete%NC%
    )
    
    deactivate
    cd ..
    echo.
)

:: Build Java
if %BUILD_JAVA%==1 (
    echo Building Java frontend...
    cd java-frontend
    
    call mvn clean compile --quiet
    if errorlevel 1 (
        echo %RED%✗ Java compilation failed%NC%
        cd ..
        exit /b 1
    )
    
    if %RUN_TESTS%==1 (
        echo Running tests...
        call mvn test
    )
    
    call mvn package --quiet
    if errorlevel 1 (
        echo %RED%✗ Java packaging failed%NC%
        cd ..
        exit /b 1
    )
    
    echo %GREEN%✓ Java build successful%NC%
    cd ..
    echo.
)

:: Create distribution
echo Creating distribution package...
if exist dist rmdir /s /q dist
mkdir dist

if exist java-frontend\target\strategic-mind-games-1.0.0-jar-with-dependencies.jar (
    copy java-frontend\target\strategic-mind-games-1.0.0-jar-with-dependencies.jar dist\
)

if exist rust-optimizer\target\release\strategic_mind_optimizer.dll (
    mkdir dist\lib
    copy rust-optimizer\target\release\strategic_mind_optimizer.dll dist\lib\
)

mkdir dist\python-ai
xcopy /E /I /Y python-ai\src dist\python-ai\src
xcopy /E /I /Y python-ai\models dist\python-ai\models 2>nul
copy python-ai\requirements.txt dist\python-ai\

:: Create run script
(
echo @echo off
echo cd /d "%%~dp0"
echo set PATH=%%PATH%%;.\lib
echo java -Djava.library.path=.\lib -jar strategic-mind-games-1.0.0-jar-with-dependencies.jar
) > dist\run.bat

echo %GREEN%✓ Distribution created%NC%
echo.

echo ==========================================
echo %GREEN%Build Complete!%NC%
echo ==========================================
echo.
echo To run the game:
echo   cd dist
echo   run.bat
echo.

goto :eof

:show_help
echo Usage: build.bat [OPTIONS]
echo Options:
echo   --skip-rust       Skip Rust optimizer build
echo   --skip-python     Skip Python AI setup
echo   --skip-java       Skip Java frontend build
echo   --skip-training   Skip AI model training
echo   --with-tests      Run tests after build
echo   --help            Show this help message
exit /b 0