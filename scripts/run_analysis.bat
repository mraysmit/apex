@echo off
REM APEX Demo YAML Analysis Runner for Windows
REM Convenience script for running the YAML analysis with common options

setlocal enabledelayedexpansion

echo 🔍 APEX Demo YAML Analysis Runner
echo =================================

REM Get script directory and APEX root
set "SCRIPT_DIR=%~dp0"
set "APEX_ROOT=%SCRIPT_DIR%\.."

REM Check if we're in the right directory
if not exist "%APEX_ROOT%\apex-demo" (
    echo X Error: apex-demo directory not found
    echo Please run this script from the APEX root directory
    exit /b 1
)

REM Check Python installation
python --version >nul 2>&1
if errorlevel 1 (
    echo X Error: Python is required but not installed
    echo Please install Python 3.7 or higher
    exit /b 1
)

REM Check if virtual environment should be created
if not exist "%SCRIPT_DIR%\venv" (
    echo 📦 Creating virtual environment...
    python -m venv "%SCRIPT_DIR%\venv"
)

REM Activate virtual environment
echo 🔧 Activating virtual environment...
call "%SCRIPT_DIR%\venv\Scripts\activate.bat"

REM Install dependencies
echo 📥 Installing dependencies...
pip install -q -r "%SCRIPT_DIR%\requirements.txt"

REM Set default output files
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "TIMESTAMP=%dt:~0,8%_%dt:~8,6%"
set "OUTPUT_DIR=%APEX_ROOT%\reports"
set "MARKDOWN_REPORT=%OUTPUT_DIR%\demo_yaml_analysis_%TIMESTAMP%.md"
set "JSON_REPORT=%OUTPUT_DIR%\demo_yaml_analysis_%TIMESTAMP%.json"

REM Create reports directory
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM Parse command line arguments
set "VERBOSE="
set "CUSTOM_OUTPUT="
set "CUSTOM_JSON="

:parse_args
if "%~1"=="" goto run_analysis
if "%~1"=="-v" set "VERBOSE=--verbose" & shift & goto parse_args
if "%~1"=="--verbose" set "VERBOSE=--verbose" & shift & goto parse_args
if "%~1"=="-o" set "CUSTOM_OUTPUT=%~2" & shift & shift & goto parse_args
if "%~1"=="--output" set "CUSTOM_OUTPUT=%~2" & shift & shift & goto parse_args
if "%~1"=="-j" set "CUSTOM_JSON=%~2" & shift & shift & goto parse_args
if "%~1"=="--json" set "CUSTOM_JSON=%~2" & shift & shift & goto parse_args
if "%~1"=="-h" goto show_help
if "%~1"=="--help" goto show_help

echo X Unknown option: %~1
echo Use --help for usage information
exit /b 1

:show_help
echo Usage: %0 [OPTIONS]
echo.
echo Options:
echo   -v, --verbose     Enable verbose output
echo   -o, --output FILE Custom markdown output file
echo   -j, --json FILE   Custom JSON output file
echo   -h, --help        Show this help message
echo.
echo Examples:
echo   %0                          # Run with default settings
echo   %0 --verbose                # Run with verbose output
echo   %0 -o my_report.md          # Custom markdown output
echo   %0 -o report.md -j data.json # Custom outputs
exit /b 0

:run_analysis
REM Use custom outputs if provided
if not "%CUSTOM_OUTPUT%"=="" set "MARKDOWN_REPORT=%CUSTOM_OUTPUT%"
if not "%CUSTOM_JSON%"=="" set "JSON_REPORT=%CUSTOM_JSON%"

REM Run the analysis
echo 🚀 Running APEX Demo YAML Analysis...
echo 📁 APEX Root: %APEX_ROOT%
echo 📝 Markdown Report: %MARKDOWN_REPORT%
echo 📊 JSON Report: %JSON_REPORT%
echo.

python "%SCRIPT_DIR%\analyze_demo_yaml_files.py" --apex-root "%APEX_ROOT%" --output "%MARKDOWN_REPORT%" --json "%JSON_REPORT%" %VERBOSE%

if errorlevel 1 (
    echo X Analysis failed!
    echo Check the error messages above for details
    exit /b 1
)

echo.
echo ✅ Analysis completed successfully!
echo.
echo 📋 Generated Reports:
echo   📝 Markdown: %MARKDOWN_REPORT%
echo   📊 JSON:     %JSON_REPORT%
echo.

REM Show file sizes
if exist "%MARKDOWN_REPORT%" (
    for %%A in ("%MARKDOWN_REPORT%") do echo   📏 Markdown size: %%~zA bytes
)

if exist "%JSON_REPORT%" (
    for %%A in ("%JSON_REPORT%") do echo   📏 JSON size: %%~zA bytes
)

echo.
echo 💡 Next steps:
echo   • Review the markdown report for detailed analysis
echo   • Use the JSON report for programmatic processing
echo   • Check recommendations section for improvements

REM Offer to open the report
set /p "OPEN_REPORT=📖 Open markdown report now? (y/N): "
if /i "%OPEN_REPORT%"=="y" (
    start "" "%MARKDOWN_REPORT%"
)

REM Deactivate virtual environment
call deactivate 2>nul

echo 🎉 All done!
pause
