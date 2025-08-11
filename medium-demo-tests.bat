@echo off
REM ============================================================================
REM APEX Demo Module - Medium Test Execution Script
REM ============================================================================
REM
REM This script runs comprehensive apex-demo tests excluding heavy integration
REM tests for thorough validation without excessive wait times.
REM Typical execution time: 2-3 minutes
REM
REM Tests included:
REM - All fast tests (service, validation, core demo)
REM - Advanced feature tests (SpEL, JSON/XML processing)
REM - Financial validation tests (commodity swaps)
REM - Collection and dynamic method tests
REM
REM Tests excluded:
REM - Integration tests (*Integration*Test)
REM - Heavy runner tests (*Runner*Test)
REM - Bootstrap tests (*Bootstrap*Test)
REM
REM Usage:
REM   medium-demo-tests.bat
REM
REM ============================================================================

setlocal enabledelayedexpansion

echo ============================================================================
echo APEX Demo Module - Medium Test Execution
echo ============================================================================
echo Running comprehensive tests excluding heavy integration tests...
echo.

REM Set timestamp for this test run
set TIMESTAMP=%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

echo Timestamp: %TIMESTAMP%
echo.

REM Run medium tests (exclude heavy integration tests)
echo ============================================================================
echo Executing Comprehensive Test Suite (Excluding Integration Tests)
echo ============================================================================

REM Run all tests except integration, runner, and bootstrap tests
call mvn test -pl apex-demo ^
    -Dtest="MockDataSourceTest,DataServiceManagerTest,ValidationTest,QuickStartDemoTest,LayeredAPIDemoTest,CommoditySwapValidationTest,SpelAdvancedFeaturesTest,DynamicMethodExecutionTest,JsonXmlProcessingTest,CollectionOperationsTest" ^
    -q

set TEST_EXIT_CODE=%errorlevel%

echo.
echo ============================================================================
echo Medium Test Execution Complete
echo ============================================================================
echo.

if %TEST_EXIT_CODE% equ 0 (
    echo ALL MEDIUM TESTS PASSED SUCCESSFULLY!
    echo.
    echo Test categories executed:
    echo   Service Layer: MockDataSourceTest, DataServiceManagerTest
    echo   Validation: ValidationTest, CommoditySwapValidationTest
    echo   Core Demos: QuickStartDemoTest, LayeredAPIDemoTest
    echo   Advanced Features: SpelAdvancedFeaturesTest, DynamicMethodExecutionTest
    echo   Data Processing: JsonXmlProcessingTest, CollectionOperationsTest
    echo.
    echo Test categories excluded (for faster execution):
    echo   Integration Tests: *Integration*Test
    echo   Heavy Runners: *Runner*Test
    echo   Bootstrap Tests: *Bootstrap*Test
    echo.
    echo Estimated tests executed: ~80-100 tests
    echo Typical execution time: 2-3 minutes
    echo.
    echo For full validation including integration tests, use:
    echo   mvn test -pl apex-demo    (5+ minutes)
    echo.
    echo For faster development feedback, use:
    echo   fast-demo-tests.bat    (10-15 seconds)
) else (
    echo SOME MEDIUM TESTS FAILED
    echo.
    echo ============================================================================
    echo ERROR: Medium test execution encountered issues
    echo ============================================================================
    echo.
    echo Troubleshooting steps:
    echo 1. Check the Maven output above for specific test failures
    echo 2. Run fast tests first to isolate basic issues:
    echo    fast-demo-tests.bat
    echo 3. Run individual test categories to isolate problems:
    echo    mvn test -Dtest="*Demo*Test" -pl apex-demo
    echo    mvn test -Dtest="*Validation*Test" -pl apex-demo
    echo    mvn test -Dtest="*Advanced*Test" -pl apex-demo
    echo 4. Check for resource or dependency issues
    echo 5. Verify all required configuration files are present
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
