@echo off
REM ============================================================================
REM APEX Rules Engine - Comprehensive Test Execution with Permanent Logging
REM ============================================================================
REM
REM This script runs all tests with comprehensive logging and generates
REM permanent reports that can be analyzed after test execution.
REM
REM Generated outputs:
REM - target/surefire-reports/        - XML and TXT test reports
REM - target/test-reports/            - HTML test reports  
REM - target/test-logs/               - Detailed log files
REM - test-execution-summary.txt      - Summary report
REM
REM Usage:
REM   run-tests-with-reports.bat [module]
REM
REM Examples:
REM   run-tests-with-reports.bat           (run all modules)
REM   run-tests-with-reports.bat apex-core (run only apex-core)
REM   run-tests-with-reports.bat apex-demo (run only apex-demo)
REM
REM ============================================================================

setlocal enabledelayedexpansion

REM Set timestamp for this test run
set TIMESTAMP=%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

echo ============================================================================
echo APEX Rules Engine - Comprehensive Test Execution
echo ============================================================================
echo Timestamp: %TIMESTAMP%
echo.

REM Create output directories
if not exist "target\test-logs" mkdir "target\test-logs"
if not exist "target\test-reports" mkdir "target\test-reports"

REM Determine which module(s) to test
set MODULE=%1
if "%MODULE%"=="" (
    echo Running tests for ALL modules...
    set MAVEN_MODULES=
) else (
    echo Running tests for module: %MODULE%
    set MAVEN_MODULES=-pl %MODULE%
)

echo.
echo ============================================================================
echo Phase 1: Clean and Compile
echo ============================================================================

call mvn clean compile test-compile %MAVEN_MODULES%
if errorlevel 1 (
    echo ERROR: Compilation failed!
    goto :error
)

echo.
echo ============================================================================
echo Phase 2: Execute Tests with Comprehensive Logging
echo ============================================================================

REM Run tests with enhanced reporting profile
call mvn test %MAVEN_MODULES% ^
    -DgenerateTestReports=true ^
    -Dmaven.build.timestamp=%TIMESTAMP% ^
    -Djava.util.logging.config.file=src/test/resources/logging-test.properties ^
    -Dtest.execution.timestamp=%TIMESTAMP% ^
    -Dtest.execution.mode=comprehensive ^
    > "target\test-logs\test-execution-%TIMESTAMP%.log" 2>&1

set TEST_EXIT_CODE=%errorlevel%

echo.
echo ============================================================================
echo Phase 3: Generate Test Summary Report
echo ============================================================================

REM Create comprehensive summary report
echo APEX Rules Engine - Test Execution Summary > "test-execution-summary-%TIMESTAMP%.txt"
echo ============================================== >> "test-execution-summary-%TIMESTAMP%.txt"
echo. >> "test-execution-summary-%TIMESTAMP%.txt"
echo Execution Timestamp: %TIMESTAMP% >> "test-execution-summary-%TIMESTAMP%.txt"
echo Module(s) Tested: %MODULE% >> "test-execution-summary-%TIMESTAMP%.txt"
echo Test Exit Code: %TEST_EXIT_CODE% >> "test-execution-summary-%TIMESTAMP%.txt"
echo. >> "test-execution-summary-%TIMESTAMP%.txt"

REM Count test results
echo Test Results Summary: >> "test-execution-summary-%TIMESTAMP%.txt"
echo ==================== >> "test-execution-summary-%TIMESTAMP%.txt"

if exist "apex-core\target\surefire-reports" (
    echo. >> "test-execution-summary-%TIMESTAMP%.txt"
    echo APEX-CORE Module: >> "test-execution-summary-%TIMESTAMP%.txt"
    for /f "tokens=*" %%i in ('dir /b "apex-core\target\surefire-reports\TEST-*.xml" 2^>nul ^| find /c /v ""') do (
        echo   Test Classes: %%i >> "test-execution-summary-%TIMESTAMP%.txt"
    )
)

if exist "apex-demo\target\surefire-reports" (
    echo. >> "test-execution-summary-%TIMESTAMP%.txt"
    echo APEX-DEMO Module: >> "test-execution-summary-%TIMESTAMP%.txt"
    for /f "tokens=*" %%i in ('dir /b "apex-demo\target\surefire-reports\TEST-*.xml" 2^>nul ^| find /c /v ""') do (
        echo   Test Classes: %%i >> "test-execution-summary-%TIMESTAMP%.txt"
    )
)

echo. >> "test-execution-summary-%TIMESTAMP%.txt"
echo Generated Reports: >> "test-execution-summary-%TIMESTAMP%.txt"
echo ================== >> "test-execution-summary-%TIMESTAMP%.txt"
echo   - XML Reports: target\surefire-reports\*.xml >> "test-execution-summary-%TIMESTAMP%.txt"
echo   - Text Reports: target\surefire-reports\*.txt >> "test-execution-summary-%TIMESTAMP%.txt"
echo   - HTML Reports: target\test-reports\*.html >> "test-execution-summary-%TIMESTAMP%.txt"
echo   - Log Files: target\test-logs\*.log >> "test-execution-summary-%TIMESTAMP%.txt"
echo   - Summary: test-execution-summary-%TIMESTAMP%.txt >> "test-execution-summary-%TIMESTAMP%.txt"

echo.
echo ============================================================================
echo Phase 4: Analysis and Recommendations
echo ============================================================================

REM Analyze test results for issues
echo. >> "test-execution-summary-%TIMESTAMP%.txt"
echo Issue Analysis: >> "test-execution-summary-%TIMESTAMP%.txt"
echo =============== >> "test-execution-summary-%TIMESTAMP%.txt"

REM Check for failures
findstr /i "failure\|error\|fatal\|severe" "target\test-logs\test-execution-%TIMESTAMP%.log" > nul
if not errorlevel 1 (
    echo   WARNING: Potential issues detected in test execution >> "test-execution-summary-%TIMESTAMP%.txt"
    echo   Review the detailed logs for analysis >> "test-execution-summary-%TIMESTAMP%.txt"
) else (
    echo   No critical issues detected in test execution >> "test-execution-summary-%TIMESTAMP%.txt"
)

REM Check for intentional errors
findstr /i "TEST: Triggering intentional error" "target\test-logs\test-execution-%TIMESTAMP%.log" > nul
if not errorlevel 1 (
    echo   Intentional test errors detected (this is expected) >> "test-execution-summary-%TIMESTAMP%.txt"
) else (
    echo   No intentional test errors found >> "test-execution-summary-%TIMESTAMP%.txt"
)

echo.
echo ============================================================================
echo Test Execution Complete
echo ============================================================================
echo.
echo Summary Report: test-execution-summary-%TIMESTAMP%.txt
echo Detailed Logs: target\test-logs\test-execution-%TIMESTAMP%.log
echo.

if %TEST_EXIT_CODE% equ 0 (
    echo ✅ ALL TESTS PASSED SUCCESSFULLY!
    echo.
    echo Generated permanent reports:
    echo   📊 XML Reports: target\surefire-reports\
    echo   📋 Text Reports: target\surefire-reports\
    echo   🌐 HTML Reports: target\test-reports\
    echo   📝 Log Files: target\test-logs\
    echo   📄 Summary: test-execution-summary-%TIMESTAMP%.txt
) else (
    echo ❌ SOME TESTS FAILED - Check reports for details
    goto :error
)

echo.
echo ============================================================================
pause
goto :end

:error
echo.
echo ============================================================================
echo ERROR: Test execution encountered issues
echo ============================================================================
echo Check the following for details:
echo   - target\test-logs\test-execution-%TIMESTAMP%.log
echo   - target\surefire-reports\
echo   - test-execution-summary-%TIMESTAMP%.txt
echo.
pause
exit /b 1

:end
endlocal
