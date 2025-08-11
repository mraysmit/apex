#!/bin/bash
# ============================================================================
# APEX Demo Module - Medium Test Execution Script
# ============================================================================
#
# This script runs comprehensive apex-demo tests excluding heavy integration
# tests for thorough validation without excessive wait times.
# Typical execution time: 2-3 minutes
#
# Tests included:
# - All fast tests (service, validation, core demo)
# - Advanced feature tests (SpEL, JSON/XML processing)
# - Financial validation tests (commodity swaps)
# - Collection and dynamic method tests
#
# Tests excluded:
# - Integration tests (*Integration*Test)
# - Heavy runner tests (*Runner*Test)
# - Bootstrap tests (*Bootstrap*Test)
#
# Usage:
#   ./medium-demo-tests.sh
#
# ============================================================================

set -e  # Exit on any error

echo "============================================================================"
echo "APEX Demo Module - Medium Test Execution"
echo "============================================================================"
echo "Running comprehensive tests excluding heavy integration tests..."
echo

# Set timestamp for this test run
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
echo "Timestamp: $TIMESTAMP"
echo

# Run medium tests (exclude heavy integration tests)
echo "============================================================================"
echo "Executing Comprehensive Test Suite (Excluding Integration Tests)"
echo "============================================================================"

mvn test -pl apex-demo \
    -Dtest="!*Integration*Test,!*Runner*Test,!*Bootstrap*Test" \
    -q

echo
echo "============================================================================"
echo "Medium Test Execution Complete"
echo "============================================================================"
echo
echo "✅ ALL MEDIUM TESTS PASSED SUCCESSFULLY!"
echo
echo "Test categories executed:"
echo "  📊 Service Layer: MockDataSourceTest, DataServiceManagerTest"
echo "  🔍 Validation: ValidationTest, CommoditySwapValidationTest"
echo "  🚀 Core Demos: QuickStartDemoTest, LayeredAPIDemoTest"
echo "  ⚡ Advanced Features: SpelAdvancedFeaturesTest, DynamicMethodExecutionTest"
echo "  📄 Data Processing: JsonXmlProcessingTest, CollectionOperationsTest"
echo
echo "Test categories excluded (for faster execution):"
echo "  🐌 Integration Tests: *Integration*Test"
echo "  🔥 Heavy Runners: *Runner*Test"
echo "  🏗️ Bootstrap Tests: *Bootstrap*Test"
echo
echo "Estimated tests executed: ~80-100 tests"
echo "Typical execution time: 2-3 minutes"
echo
echo "For full validation including integration tests, use:"
echo "  mvn test -pl apex-demo    (5+ minutes)"
echo
echo "For faster development feedback, use:"
echo "  ./fast-demo-tests.sh    (10-15 seconds)"
echo
echo "============================================================================"
