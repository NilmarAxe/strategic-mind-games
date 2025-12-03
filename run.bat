@echo off
echo ==========================================
echo Strategic Mind Games - Quick Start
echo ==========================================
echo.

if not exist java-frontend\target\strategic-mind-games-1.0.0-jar-with-dependencies.jar (
    echo Game not built. Run build.bat first
    exit /b 1
)

if not exist python-ai\venv (
    echo Python environment not set up. Run build.bat first
    exit /b 1
)

echo Starting AI Engine...
cd python-ai
call venv\Scripts\activate.bat
start /B python -m src.api_server > ..\logs\ai-server.log 2>&1
cd ..

timeout /t 3 /nobreak >nul

echo Starting game...
cd java-frontend
set PATH=%PATH%;..\rust-optimizer\target\release
java -Djava.library.path=..\rust-optimizer\target\release -jar target\strategic-mind-games-1.0.0-jar-with-dependencies.jar

echo.
echo Goodbye!