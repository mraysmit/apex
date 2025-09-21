#!/bin/bash
# ============================================================================
# APEX Rules Engine - Comprehensive Test Execution with Permanent Logging
# ============================================================================
#
# This script runs all tests with comprehensive logging and generates
# permanent reports that can be analyzed after test execution.
#
# Generated outputs:
# - target/surefire-reports/        - XML and TXT test reports
# - target/test-reports/            - HTML test reports  
# - target/test-logs/               - Detailed log files
# - test-execution-summary.txt      - Summary report
#
# Usage:
#   ./run-tests-with-reports.sh [module]
#
# Examples:
#   ./run-tests-with-reports.sh           (run all modules)
#   ./run-tests-with-reports.sh apex-core (run only apex-core)
#   ./run-tests-with-reports.sh apex-demo (run only apex-demo)
#
# ============================================================================

set -e  # Exit on any error

# Set timestamp for this test run
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

echo "============================================================================"
echo "APEX Rules Engine - Comprehensive Test Execution"
echo "============================================================================"
echo "Timestamp: $TIMESTAMP"
echo

# Create output directories
mkdir -p target/test-logs
mkdir -p target/test-reports

# Determine which module(s) to test
MODULE=$1
if [ -z "$MODULE" ]; then
    echo "Running tests for ALL modules..."
    MAVEN_MODULES=""
else
    echo "Running tests for module: $MODULE"
    MAVEN_MODULES="-pl $MODULE"
fi

echo
echo "============================================================================"
echo "Phase 1: Clean and Compile"
echo "============================================================================"

mvn clean compile test-compile $MAVEN_MODULES
if [ $? -ne 0 ]; then
    echo "ERROR: Compilation failed!"
    exit 1
fi

echo
echo "============================================================================"
echo "Phase 2: Execute Tests with Comprehensive Logging"
echo "============================================================================"

# Run tests with enhanced reporting profile
mvn test $MAVEN_MODULES \
    -DgenerateTestReports=true \
    -Dmaven.build.timestamp=$TIMESTAMP \
    -Djava.util.logging.config.file=src/test/resources/logging-test.properties \
    -Dtest.execution.timestamp=$TIMESTAMP \
    -Dtest.execution.mode=comprehensive \
    > "target/test-logs/test-execution-$TIMESTAMP.log" 2>&1

TEST_EXIT_CODE=$?

echo
echo "============================================================================"
echo "Phase 3: Generate Test Summary Report"
echo "============================================================================"

# Create comprehensive summary report
cat > "test-execution-summary-$TIMESTAMP.txt" << EOF
APEX Rules Engine - Test Execution Summary
==============================================

Execution Timestamp: $TIMESTAMP
Module(s) Tested: ${MODULE:-"ALL"}
Test Exit Code: $TEST_EXIT_CODE

Test Results Summary:
====================
EOF

# Count test results
if [ -d "apex-core/target/surefire-reports" ]; then
    echo "" >> "test-execution-summary-$TIMESTAMP.txt"
    echo "APEX-CORE Module:" >> "test-execution-summary-$TIMESTAMP.txt"
    TEST_COUNT=$(find apex-core/target/surefire-reports -name "TEST-*.xml" 2>/dev/null | wc -l)
    echo "  Test Classes: $TEST_COUNT" >> "test-execution-summary-$TIMESTAMP.txt"
fi

if [ -d "apex-demo/target/surefire-reports" ]; then
    echo "" >> "test-execution-summary-$TIMESTAMP.txt"
    echo "APEX-DEMO Module:" >> "test-execution-summary-$TIMESTAMP.txt"
    TEST_COUNT=$(find apex-demo/target/surefire-reports -name "TEST-*.xml" 2>/dev/null | wc -l)
    echo "  Test Classes: $TEST_COUNT" >> "test-execution-summary-$TIMESTAMP.txt"
fi

cat >> "test-execution-summary-$TIMESTAMP.txt" << EOF

Generated Reports:
==================
  - XML Reports: target/surefire-reports/*.xml
  - Text Reports: target/surefire-reports/*.txt
  - HTML Reports: target/test-reports/*.html
  - Log Files: target/test-logs/*.log
  - Summary: test-execution-summary-$TIMESTAMP.txt
EOF

echo
echo "============================================================================"
echo "Phase 4: Analysis and Recommendations"
echo "============================================================================"

# Analyze test results for issues
cat >> "test-execution-summary-$TIMESTAMP.txt" << EOF

Issue Analysis:
===============
EOF

# Check for failures
if grep -qi "failure\|error\|fatal\|severe" "target/test-logs/test-execution-$TIMESTAMP.log"; then
    echo "  WARNING: Potential issues detected in test execution" >> "test-execution-summary-$TIMESTAMP.txt"
    echo "  Review the detailed logs for analysis" >> "test-execution-summary-$TIMESTAMP.txt"
else
    echo "  No critical issues detected in test execution" >> "test-execution-summary-$TIMESTAMP.txt"
fi

# Check for intentional errors
if grep -qi "TEST: Triggering intentional error" "target/test-logs/test-execution-$TIMESTAMP.log"; then
    echo "  Intentional test errors detected (this is expected)" >> "test-execution-summary-$TIMESTAMP.txt"
else
    echo "  No intentional test errors found" >> "test-execution-summary-$TIMESTAMP.txt"
fi

echo
echo "============================================================================"
echo "Test Execution Complete"
echo "============================================================================"
echo
echo "Summary Report: test-execution-summary-$TIMESTAMP.txt"
echo "Detailed Logs: target/test-logs/test-execution-$TIMESTAMP.log"
echo

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo " ALL TESTS PASSED SUCCESSFULLY!"
    echo
    echo "Generated permanent reports:"
    echo "  📊 XML Reports: target/surefire-reports/"
    echo "  📋 Text Reports: target/surefire-reports/"
    echo "  🌐 HTML Reports: target/test-reports/"
    echo "  📝 Log Files: target/test-logs/"
    echo "  📄 Summary: test-execution-summary-$TIMESTAMP.txt"
else
    echo "❌ SOME TESTS FAILED - Check reports for details"
    echo
    echo "============================================================================"
    echo "ERROR: Test execution encountered issues"
    echo "============================================================================"
    echo "Check the following for details:"
    echo "  - target/test-logs/test-execution-$TIMESTAMP.log"
    echo "  - target/surefire-reports/"
    echo "  - test-execution-summary-$TIMESTAMP.txt"
    echo
    exit 1
fi

echo
echo "============================================================================"
