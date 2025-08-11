@echo off
REM ============================================================================
REM APEX Demo Module - Integration Test Execution Script
REM ============================================================================
REM
REM This script runs only the integration and bootstrap tests for the apex-demo
REM module. These are the slower, more comprehensive tests that validate
REM end-to-end functionality and system integration.
REM Typical execution time: 3-5 minutes
REM
REM Tests included:
REM - Integration tests (*Integration*Test)
REM - Bootstrap tests (*Bootstrap*Test)
REM - Heavy runner tests (*Runner*Test)
REM
REM Usage:
REM   integration-demo-tests.bat
REM
REM ============================================================================

setlocal enabledelayedexpansion

echo ============================================================================
echo APEX Demo Module - Integration Test Execution
echo ============================================================================
echo Running integration and bootstrap tests for comprehensive validation...
echo.

REM Set timestamp for this test run
set TIMESTAMP=%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

echo Timestamp: %TIMESTAMP%
echo.

echo ============================================================================
echo Phase 1: Integration Tests
echo ============================================================================

call mvn test -pl apex-demo ^
    -Dtest="*Integration*Test" ^
    -q

set INTEGRATION_EXIT_CODE=%errorlevel%

if %INTEGRATION_EXIT_CODE% neq 0 (
    echo WARNING: Integration tests encountered issues
)

echo.
echo ============================================================================
echo Phase 2: Bootstrap Tests
echo ============================================================================

call mvn test -pl apex-demo ^
    -Dtest="*Bootstrap*Test" ^
    -q

set BOOTSTRAP_EXIT_CODE=%errorlevel%

if %BOOTSTRAP_EXIT_CODE% neq 0 (
    echo WARNING: Bootstrap tests encountered issues
)

echo.
echo ============================================================================
echo Phase 3: Heavy Runner Tests
echo ============================================================================

call mvn test -pl apex-demo ^
    -Dtest="*Runner*Test" ^
    -q

set RUNNER_EXIT_CODE=%errorlevel%

if %RUNNER_EXIT_CODE% neq 0 (
    echo WARNING: Runner tests encountered issues
)

echo.
echo ============================================================================
echo Integration Test Execution Complete
echo ============================================================================
echo.

REM Calculate overall result
set OVERALL_SUCCESS=1
if %INTEGRATION_EXIT_CODE% neq 0 set OVERALL_SUCCESS=0
if %BOOTSTRAP_EXIT_CODE% neq 0 set OVERALL_SUCCESS=0
if %RUNNER_EXIT_CODE% neq 0 set OVERALL_SUCCESS=0

if %OVERALL_SUCCESS% equ 1 (
    echo ✅ ALL INTEGRATION TESTS PASSED SUCCESSFULLY!
    echo.
    echo Test categories executed:
    echo   🔗 Integration Tests: End-to-end system validation
    echo   🏗️ Bootstrap Tests: Configuration and startup validation
    echo   🔥 Runner Tests: Comprehensive demo execution
    echo.
    echo These tests validate:
    echo   - Complete system integration
    echo   - Configuration file processing
    echo   - End-to-end demo workflows
    echo   - Performance under load
    echo   - Resource management
    echo.
    echo Estimated tests executed: ~20-30 integration tests
    echo Typical execution time: 3-5 minutes
    echo.
    echo For complete validation, combine with:
    echo   medium-demo-tests.bat  (unit and functional tests)
    echo.
    echo For faster development feedback, use:
    echo   fast-demo-tests.bat    (essential tests only)
) else (
    echo ⚠️ SOME INTEGRATION TESTS HAD ISSUES
    echo.
    echo Results summary:
    if %INTEGRATION_EXIT_CODE% equ 0 (
        echo   ✅ Integration Tests: PASSED
    ) else (
        echo   ❌ Integration Tests: FAILED
    )
    if %BOOTSTRAP_EXIT_CODE% equ 0 (
        echo   ✅ Bootstrap Tests: PASSED
    ) else (
        echo   ❌ Bootstrap Tests: FAILED
    )
    if %RUNNER_EXIT_CODE% equ 0 (
        echo   ✅ Runner Tests: PASSED
    ) else (
        echo   ❌ Runner Tests: FAILED
    )
    echo.
    echo ============================================================================
    echo Troubleshooting Integration Test Issues
    echo ============================================================================
    echo.
    echo Common integration test issues:
    echo 1. Resource availability (databases, external services)
    echo 2. Configuration file dependencies
    echo 3. Memory or timeout issues with heavy tests
    echo 4. Environment-specific dependencies
    echo.
    echo Debugging steps:
    echo 1. Run individual integration test categories:
    echo    mvn test -Dtest="AllDemosRunnerIntegrationTest" -pl apex-demo
    echo    mvn test -Dtest="CommoditySwapValidationBootstrapTest" -pl apex-demo
    echo    mvn test -Dtest="YamlDependencyAnalysisIntegrationTest" -pl apex-demo
    echo.
    echo 2. Check if basic functionality works first:
    echo    fast-demo-tests.bat
    echo.
    echo 3. Verify system resources and dependencies
    echo.
    goto :error
)

echo ============================================================================
pause
goto :end

:error
pause
exit /b 1

:end
endlocal
