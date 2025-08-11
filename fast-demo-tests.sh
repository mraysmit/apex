#!/bin/bash
# ============================================================================
# APEX Demo Module - Fast Test Execution Script
# ============================================================================
#
# This script runs the fastest apex-demo tests for rapid development feedback.
# Typical execution time: 10-15 seconds
#
# Tests included:
# - MockDataSourceTest (14 tests)
# - DataServiceManagerTest (24 tests) 
# - ValidationTest (6 tests)
# - QuickStartDemoTest (11 tests)
#
# Usage:
#   ./fast-demo-tests.sh
#
# ============================================================================

set -e  # Exit on any error

echo "============================================================================"
echo "APEX Demo Module - Fast Test Execution"
echo "============================================================================"
echo "Running essential tests for rapid development feedback..."
echo

# Set timestamp for this test run
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
echo "Timestamp: $TIMESTAMP"
echo

# Run fast tests
echo "============================================================================"
echo "Phase 1: Service Layer Tests (Fast)"
echo "============================================================================"

mvn test -pl apex-demo \
    -Dtest="MockDataSourceTest,DataServiceManagerTest" \
    -q

echo
echo "============================================================================"
echo "Phase 2: Validation Tests (Fast)"
echo "============================================================================"

mvn test -pl apex-demo \
    -Dtest="ValidationTest" \
    -q

echo
echo "============================================================================"
echo "Phase 3: Core Demo Tests (Medium)"
echo "============================================================================"

mvn test -pl apex-demo \
    -Dtest="QuickStartDemoTest" \
    -q

echo
echo "============================================================================"
echo "Fast Test Execution Complete"
echo "============================================================================"
echo
echo "✅ ALL FAST TESTS PASSED SUCCESSFULLY!"
echo
echo "Tests executed:"
echo "  📊 Service Layer: MockDataSourceTest, DataServiceManagerTest"
echo "  🔍 Validation: ValidationTest"
echo "  🚀 Core Demo: QuickStartDemoTest"
echo
echo "Total estimated tests: ~55 tests"
echo "Typical execution time: 10-15 seconds"
echo
echo "For more comprehensive testing, use:"
echo "  ./medium-demo-tests.sh  (2-3 minutes)"
echo "  mvn test -pl apex-demo  (5+ minutes)"
echo
echo "============================================================================"
