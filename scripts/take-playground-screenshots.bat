@echo off
REM Script to take screenshots of the APEX Playground using Selenium
REM Usage: take-playground-screenshots.bat [base_url] [screenshot_dir]
REM Example: take-playground-screenshots.bat http://localhost:8080 screenshots

setlocal enabledelayedexpansion

REM Set default values
set BASE_URL=http://localhost:8080
set SCREENSHOT_DIR=screenshots

REM Override with command line arguments if provided
if not "%1"=="" set BASE_URL=%1
if not "%2"=="" set SCREENSHOT_DIR=%2

echo ============================================================================
echo APEX Playground Screenshot Capture
echo ============================================================================
echo Base URL: %BASE_URL%
echo Screenshot Directory: %SCREENSHOT_DIR%
echo.

REM Check if Maven is available
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Maven is not available in PATH
    echo Please install Maven or add it to your PATH
    exit /b 1
)

REM Check if the playground is running
echo Checking if playground is accessible at %BASE_URL%...
curl -s --head %BASE_URL%/playground >nul 2>nul
if %errorlevel% neq 0 (
    echo WARNING: Playground may not be running at %BASE_URL%
    echo Please ensure the APEX Playground application is started
    echo.
    echo To start the playground, run:
    echo   mvn spring-boot:run -pl apex-playground
    echo.
    set /p continue="Continue anyway? (y/N): "
    if /i not "!continue!"=="y" (
        echo Exiting...
        exit /b 1
    )
)

echo.
echo Starting screenshot capture...
echo.

REM Run the screenshot utility
mvn exec:java -pl apex-playground ^
    -Dexec.mainClass="dev.mars.apex.playground.util.PlaygroundScreenshotRunner" ^
    -Dexec.args="%BASE_URL% %SCREENSHOT_DIR%" ^
    -Dexec.cleanupDaemonThreads=false

if %errorlevel% equ 0 (
    echo.
    echo ============================================================================
    echo Screenshot capture completed successfully!
    echo ============================================================================
    echo Screenshots saved to: %SCREENSHOT_DIR%
    echo.
    echo Generated screenshots:
    dir /b %SCREENSHOT_DIR%\playground_*.png 2>nul
    echo.
    echo You can view the screenshots in your file explorer:
    echo   explorer %SCREENSHOT_DIR%
) else (
    echo.
    echo ============================================================================
    echo Screenshot capture failed!
    echo ============================================================================
    echo Please check the error messages above and ensure:
    echo 1. The APEX Playground is running at %BASE_URL%
    echo 2. Chrome browser is installed
    echo 3. All dependencies are available
)

echo.
pause
