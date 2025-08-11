@echo off
REM ============================================================================
REM APEX Demo Module - Fast Test Execution Script
REM ============================================================================
REM
REM This script runs the fastest apex-demo tests for rapid development feedback.
REM Typical execution time: 10-15 seconds
REM
REM Tests included:
REM - MockDataSourceTest (14 tests)
REM - DataServiceManagerTest (24 tests) 
REM - ValidationTest (6 tests)
REM - QuickStartDemoTest (11 tests)
REM
REM Usage:
REM   fast-demo-tests.bat
REM
REM ============================================================================

setlocal enabledelayedexpansion

echo ============================================================================
echo APEX Demo Module - Fast Test Execution
echo ============================================================================
echo Running essential tests for rapid development feedback...
echo.

REM Set timestamp for this test run
set TIMESTAMP=%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

echo Timestamp: %TIMESTAMP%
echo.

REM Run fast tests
echo ============================================================================
echo Phase 1: Service Layer Tests (Fast)
echo ============================================================================

call mvn test -pl apex-demo ^
    -Dtest="MockDataSourceTest,DataServiceManagerTest" ^
    -q

if errorlevel 1 (
    echo ERROR: Service layer tests failed!
    goto :error
)

echo.
echo ============================================================================
echo Phase 2: Validation Tests (Fast)
echo ============================================================================

call mvn test -pl apex-demo ^
    -Dtest="ValidationTest" ^
    -q

if errorlevel 1 (
    echo ERROR: Validation tests failed!
    goto :error
)

echo.
echo ============================================================================
echo Phase 3: Core Demo Tests (Medium)
echo ============================================================================

call mvn test -pl apex-demo ^
    -Dtest="QuickStartDemoTest" ^
    -q

if errorlevel 1 (
    echo ERROR: Core demo tests failed!
    goto :error
)

echo.
echo ============================================================================
echo Fast Test Execution Complete
echo ============================================================================
echo.
echo ✅ ALL FAST TESTS PASSED SUCCESSFULLY!
echo.
echo Tests executed:
echo   📊 Service Layer: MockDataSourceTest, DataServiceManagerTest
echo   🔍 Validation: ValidationTest  
echo   🚀 Core Demo: QuickStartDemoTest
echo.
echo Total estimated tests: ~55 tests
echo Typical execution time: 10-15 seconds
echo.
echo For more comprehensive testing, use:
echo   medium-demo-tests.bat  (2-3 minutes)
echo   full-demo-tests.bat    (5+ minutes)
echo.
goto :end

:error
echo.
echo ============================================================================
echo ERROR: Fast test execution encountered issues
echo ============================================================================
echo.
echo Troubleshooting steps:
echo 1. Check if apex-demo module compiled successfully
echo 2. Verify test dependencies are available
echo 3. Run individual test classes to isolate issues:
echo    mvn test -Dtest=MockDataSourceTest -pl apex-demo
echo    mvn test -Dtest=DataServiceManagerTest -pl apex-demo
echo    mvn test -Dtest=ValidationTest -pl apex-demo
echo    mvn test -Dtest=QuickStartDemoTest -pl apex-demo
echo.
pause
exit /b 1

:end
echo ============================================================================
pause
endlocal
