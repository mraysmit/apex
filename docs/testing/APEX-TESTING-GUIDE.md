# APEX Rules Engine - Comprehensive Testing Guide

This guide provides complete testing strategies for the APEX Rules Engine, covering granular test execution, performance optimization, and permanent logging for analysis and compliance.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Testing Documentation Suite](#testing-documentation-suite)
3. [Granular Testing Strategies](#granular-testing-strategies)
4. [Performance-Optimized Execution](#performance-optimized-execution)
5. [Test Logging and Reporting](#test-logging-and-reporting)
6. [Development Workflows](#development-workflows)
7. [Advanced Options](#advanced-options)
8. [Troubleshooting](#troubleshooting)

## Quick Start

### Immediate Test Execution Options

#### **Fast Tests (10-15 seconds)**
```bash
# Windows
fast-demo-tests.bat

# Linux/Mac  
./fast-demo-tests.sh
```
**Includes:** MockDataSourceTest, DataServiceManagerTest, ValidationTest, QuickStartDemoTest  
**Total:** ~55 tests in 10-15 seconds

#### **Medium Tests (2-3 minutes)**
```bash
# Windows
medium-demo-tests.bat

# Linux/Mac
./medium-demo-tests.sh
```
**Includes:** All fast tests + advanced features, excluding heavy integration tests  
**Total:** ~80-100 tests in 2-3 minutes

#### **Integration Tests (3-5 minutes)**
```bash
# Windows
integration-demo-tests.bat

# Linux/Mac (manual command)
mvn test -pl apex-demo -Dtest="*Integration*Test,*Bootstrap*Test,*Runner*Test"
```
**Includes:** Only the heavy integration, bootstrap, and runner tests  
**Total:** ~20-30 integration tests in 3-5 minutes

### Generate Permanent Test Reports

#### **Automated Scripts (Recommended)**

**Windows:**
```bash
# Run all tests with comprehensive logging
run-tests-with-reports.bat

# Run specific module
run-tests-with-reports.bat apex-core
run-tests-with-reports.bat apex-demo
```

**Linux/Mac:**
```bash
# Make script executable (first time only)
chmod +x run-tests-with-reports.sh

# Run all tests with comprehensive logging
./run-tests-with-reports.sh

# Run specific module
./run-tests-with-reports.sh apex-core
./run-tests-with-reports.sh apex-demo
```

## Testing Documentation Suite

This comprehensive testing guide is part of a complete documentation suite covering all aspects of APEX testing:

### Core Testing Guides
- **APEX-TESTING-GUIDE.md** (this document) - Main testing overview and strategy

### Engine & Executor Testing
- **engine-executor-testing-methodology.md** - Detailed methodology for rule executor testing
- **executor-testing-examples.md** - Practical examples and code samples
- **executor-testing-quick-reference.md** - Quick reference for common testing patterns

### Service Layer Testing
- **service-layer-testing-guide.md** - Comprehensive guide to service layer testing (462 tests)
- **service-testing-methodology.md** - Testing methodology for service components
- **service-testing-quick-reference.md** - Quick reference for service testing patterns

### Service Layer Test Coverage Summary

The service layer contains **462 comprehensive tests** across **20 test classes**:

#### **Cache Services (117 tests)**
- **CacheDataSourceTest** (37 tests) - Cache-based data source implementation
- **CacheManagerTest** (22 tests) - Central cache management system
- **CacheStatisticsTest** (22 tests) - Cache statistics and performance monitoring
- **InMemoryCacheManagerTest** (36 tests) - In-memory cache implementation

#### **Database Services (70 tests)**
- **DatabaseDataSourceTest** (27 tests) - Database connectivity and operations
- **DatabaseHealthIndicatorTest** (14 tests) - Database health monitoring
- **JdbcTemplateFactoryTest** (15 tests) - JDBC template creation and configuration
- **PostgreSQLIntegrationTest** (14 tests) - PostgreSQL-specific integration

#### **File Services (54 tests)**
- **CsvDataLoaderTest** (27 tests) - CSV file processing and data loading
- **JsonDataLoaderTest** (27 tests) - JSON file processing and data loading

#### **Configuration Services (62 tests)**
- **DataSourceConfigurationServiceTest** (24 tests) - Configuration management service
- **DataSourceConfigurationEventTest** (24 tests) - Configuration event system
- **DataSourceConfigurationListenerTest** (14 tests) - Configuration event listeners

#### **Factory Services (20 tests)**
- **DataSourceFactoryTest** (20 tests) - Data source factory pattern implementation

#### **Core Data Services (122 tests)**
- **CustomDataSourceTest** (32 tests) - Custom data source implementations
- **DataSourceTypeTest** (33 tests) - Data source type enumeration and behavior
- **DataSourceMetricsTest** (32 tests) - Metrics collection and performance monitoring
- **DataSourceExceptionTest** (25 tests) - Exception handling and error management

#### **Integration Tests (17 tests)**
- **ExternalDataSourceIntegrationTest** (8 tests) - End-to-end integration testing
- **YamlDatasetIntegrationTest** (9 tests) - YAML-based dataset integration

### Quick Service Test Execution

```bash
# Run all service tests
mvn test -pl apex-core -Dtest="dev.mars.apex.core.service.**"

# Run by category
mvn test -pl apex-core -Dtest="*Cache*Test"        # Cache services
mvn test -pl apex-core -Dtest="*Database*Test"     # Database services
mvn test -pl apex-core -Dtest="*DataLoader*Test"   # File services
mvn test -pl apex-core -Dtest="*Configuration*Test" # Configuration services
mvn test -pl apex-core -Dtest="*Integration*Test"  # Integration tests
```

## Granular Testing Strategies

### Test Categories by Speed

#### **Quick Tests (< 5 seconds each)**
```bash
# Unit Tests - Fast execution
mvn test -Dtest=MockDataSourceTest -pl apex-demo           # 14 tests, ~2 sec
mvn test -Dtest=DataServiceManagerTest -pl apex-demo       # 24 tests, ~3 sec
mvn test -Dtest=ValidationTest -pl apex-demo               # 6 tests, ~2 sec
mvn test -Dtest=CollectionOperationsTest -pl apex-demo     # ~3 sec
mvn test -Dtest=DynamicMethodExecutionTest -pl apex-demo   # ~4 sec
```

#### **Medium Tests (5-15 seconds each)**
```bash
# Demo Logic Tests - Moderate execution
mvn test -Dtest=QuickStartDemoTest -pl apex-demo           # 11 tests, ~8 sec
mvn test -Dtest=LayeredAPIDemoTest -pl apex-demo           # ~12 sec
mvn test -Dtest=CommoditySwapValidationTest -pl apex-demo  # ~15 sec
mvn test -Dtest=SpelAdvancedFeaturesTest -pl apex-demo     # ~10 sec
mvn test -Dtest=JsonXmlProcessingTest -pl apex-demo        # ~8 sec
```

#### **Slow Tests (15+ seconds each)**
```bash
# Integration Tests - Longer execution
mvn test -Dtest=AllDemosRunnerIntegrationTest -pl apex-demo     # ~45 sec
mvn test -Dtest=CommoditySwapValidationBootstrapTest -pl apex-demo  # ~30 sec
mvn test -Dtest=YamlDependencyAnalysisIntegrationTest -pl apex-demo # ~20 sec
```

#### **Heavy Tests (30+ seconds each)**
```bash
# Full Demo Execution - Longest execution
mvn test -Dtest=AllDemosRunnerTest -pl apex-demo                # ~90 sec
mvn test -Dtest=AllDemosRunnerCommandLineTest -pl apex-demo     # ~120 sec
```

### Pattern-Based Testing

#### **By Test Class Pattern**
```bash
# Run all service tests
mvn test -Dtest="*Service*Test" -pl apex-demo

# Run all validation tests
mvn test -Dtest="*Validation*Test" -pl apex-demo

# Run all demo tests (excluding integration)
mvn test -Dtest="*Demo*Test" -Dtest="!*Integration*Test" -pl apex-demo

# Fast tests only (exclude slow ones)
mvn test -pl apex-demo -Dtest="!*Integration*Test,!*Runner*Test,!*Bootstrap*Test"
```

#### **By Functionality**
```bash
# Financial services tests
mvn test -Dtest="*Financial*Test,*Commodity*Test,*Swap*Test" -pl apex-demo

# Data processing tests
mvn test -Dtest="*Data*Test,*Json*Test,*Xml*Test,*Yaml*Test" -pl apex-demo

# Advanced SpEL features
mvn test -Dtest="*Spel*Test,*Advanced*Test,*Dynamic*Test" -pl apex-demo
```

#### **By Execution Speed**
```bash
# Fast tests only (exclude integration and runner tests)
mvn test -pl apex-demo -Dtest="!*Integration*Test,!*Runner*Test,!*Bootstrap*Test"

# Medium speed tests
mvn test -pl apex-demo -Dtest="*Demo*Test" -Dtest="!*Integration*Test,!*Runner*Test"

# Integration tests only
mvn test -pl apex-demo -Dtest="*Integration*Test,*Bootstrap*Test"
```

## Performance-Optimized Execution

### Performance Comparison

| **Test Level** | **Time** | **Tests** | **Use Case** |
|----------------|----------|-----------|--------------|
| **Fast** | 10-15 sec | ~55 | Development feedback |
| **Medium** | 2-3 min | ~80-100 | Pre-commit validation |
| **Integration** | 3-5 min | ~20-30 | System validation |
| **Full** | 5+ min | ~150+ | Complete CI/CD |

### Development Workflow Optimization

#### **Active Development (Fastest - 2-4 seconds)**
```bash
# Run only what you're working on
mvn test -Dtest=MockDataSourceTest -pl apex-demo
```

#### **Feature Validation (Fast - 10-15 seconds)**
```bash
# Essential functionality check
fast-demo-tests.bat  # or ./fast-demo-tests.sh
```

#### **Pre-Commit Check (Medium - 2-3 minutes)**
```bash
# Comprehensive without integration overhead
medium-demo-tests.bat  # or ./medium-demo-tests.sh
```

#### **Pre-Push Validation (Complete - 5+ minutes)**
```bash
# Full test suite
mvn test -pl apex-demo
```

### Parallel Execution
```bash
# Run tests in parallel (faster on multi-core)
mvn test -pl apex-demo -T 4 -Dtest="!*Integration*Test"

# Parallel with specific test pattern
mvn test -pl apex-demo -T 2 -Dtest="!*Integration*Test,!*Runner*Test"
```

## Test Logging and Reporting

### Generated Output Locations

After running tests, you'll find permanent reports in these locations:

#### **Surefire Reports (Always Generated)**
```
target/surefire-reports/
├── TEST-*.xml                    # JUnit XML reports
├── *.txt                         # Text summaries
└── *-output.txt                  # Console output capture
```

#### **Enhanced Logging Files (When Using Scripts)**
```
target/test-logs/
├── test-execution-TIMESTAMP.log  # Complete test execution log
├── apex-tests-*.log              # Detailed application logs
└── apex-demo-tests-*.log         # Demo-specific logs
```

#### **Summary Reports (When Using Scripts)**
```
test-execution-summary-TIMESTAMP.txt  # Comprehensive summary
```

#### **HTML Reports (When Using test-reports Profile)**
```
target/test-reports/
└── test-report.html              # HTML formatted report
```

### Manual Test Execution with Logging

#### **Basic Test Execution with File Output:**
```bash
# Redirect all output to a file
mvn test > test-results-$(date +%Y%m%d_%H%M%S).log 2>&1

# Run specific test with output capture
mvn test -Dtest=SimpleRulesEngineTest -pl apex-core > simple-rules-test.log 2>&1
```

#### **Enhanced Test Execution with Reporting Profile:**
```bash
# Use the test-reports profile for enhanced logging
mvn test -DgenerateTestReports=true > comprehensive-test-report.log 2>&1

# With timestamp and additional properties
mvn test -DgenerateTestReports=true \
    -Dmaven.build.timestamp=$(date +%Y%m%d_%H%M%S) \
    > "test-execution-$(date +%Y%m%d_%H%M%S).log" 2>&1
```

### Understanding Test Output

#### **Expected Messages (Normal Operation):**
```
INFO: Loaded data source: products (type: products)
INFO: Rule 'Check' evaluated to true in 0.00ms
INFO: Rule matched successfully
```

#### **Intentional Test Errors (Expected):**
```
TEST: Triggering intentional error - testing invalid parameters handling
WARNING: No data source found for data type: unsupportedType
WARNING: Data source has no name - this indicates a programming error
```

#### **Real Issues (Need Investigation):**
```
SEVERE: Unexpected database connection failure
FATAL: Critical system error during rule evaluation
ERROR: Configuration file not found (without TEST: prefix)
```

### Key Metrics to Monitor

1. **Test Execution Summary:**
   - Total tests run, failures, errors, skipped
   - Execution time and performance metrics

2. **Rule Evaluation Performance:**
   - Rule evaluation times (should be < 100ms typically)
   - Memory usage and resource consumption

3. **Integration Health:**
   - Database connection status
   - External data source availability
   - YAML validation results

4. **Error Classification:**
   - Intentional test errors (documented with "TEST:" prefix)
   - Real production issues (no "TEST:" prefix)

## Development Workflows

### Recommended Development Workflow

#### **1. Active Development (Fastest)**
```bash
# Run only what you're working on
mvn test -Dtest=MockDataSourceTest -pl apex-demo
```
**Time:** 2-4 seconds  
**Feedback:** Immediate

#### **2. Feature Validation (Fast)**
```bash
# Essential functionality check
fast-demo-tests.bat  # or ./fast-demo-tests.sh
```
**Time:** 10-15 seconds  
**Feedback:** Core functionality verified

#### **3. Pre-Commit Check (Medium)**
```bash
# Comprehensive without integration overhead
medium-demo-tests.bat  # or ./medium-demo-tests.sh
```
**Time:** 2-3 minutes  
**Feedback:** Thorough validation

#### **4. Pre-Push Validation (Complete)**
```bash
# Full test suite
mvn test -pl apex-demo
```
**Time:** 5+ minutes  
**Feedback:** Complete system validation

### Key Benefits Achieved

#### **Development Speed**
- **95% faster feedback** for development (15 sec vs 5+ min)
- **Rapid iteration** with immediate test results
- **Targeted testing** for specific functionality

#### **Flexible Granularity**
- **Individual test classes** (2-4 seconds)
- **Fast test suites** (10-15 seconds)
- **Medium test suites** (2-3 minutes)
- **Integration test suites** (3-5 minutes)
- **Complete validation** (5+ minutes)

#### **Professional Tooling**
- **Cross-platform scripts** (Windows, Linux, Mac)
- **Clear execution reporting** with test counts and timing
- **Error handling** and troubleshooting guidance
- **Comprehensive documentation** and usage examples

#### **Workflow Integration**
- **IDE integration** ready (can run individual Maven commands)
- **CI/CD pipeline** compatible (different test levels for different stages)
- **Team adoption** friendly (simple scripts and clear documentation)

## Advanced Options

### Test Method Level Execution
```bash
# Run specific test methods
mvn test -pl apex-demo -Dtest="QuickStartDemoTest#testBasicFunctionality"

mvn test -pl apex-demo -Dtest="CommoditySwapValidationTest#testValidSwapValidation"

# Multiple specific methods
mvn test -pl apex-demo \
    -Dtest="QuickStartDemoTest#testBasicFunctionality,LayeredAPIDemoTest#testBasicFunctionality"
```

### Timeout-Based Filtering
```bash
# Skip tests that might timeout (useful for quick validation)
mvn test -pl apex-demo \
    -Dtest="!*Runner*Test,!*Integration*Test,!*Bootstrap*Test" \
    -Dsurefire.timeout=30
```

### Custom Test Combinations
```bash
# Create your own fast combination
mvn test -pl apex-demo -Dtest="MockDataSourceTest,ValidationTest,QuickStartDemoTest"

# Quick Development Suite
mvn test -pl apex-demo \
    -Dtest="MockDataSourceTest,DataServiceManagerTest,ValidationTest,QuickStartDemoTest"

# Financial Validation Suite
mvn test -pl apex-demo \
    -Dtest="*Financial*Test,*Commodity*Test,*Swap*Test,*Validation*Test"

# Integration Suite
mvn test -pl apex-demo -Dtest="*Integration*Test,*Bootstrap*Test"
```

### Custom Logging Levels

#### **Enable Debug Logging:**
```bash
# Uncomment debug lines in logging-test.properties
# dev.mars.apex.level = FINE
# org.springframework.expression.spel.level = FINE

mvn test -Djava.util.logging.config.file=src/test/resources/logging-test.properties
```

#### **Performance Monitoring:**
```bash
# Enable performance tracking
mvn test -Dtest.performance.monitoring=true
```

### Continuous Integration Integration

#### **Jenkins/GitHub Actions:**
```yaml
- name: Run Tests with Permanent Logging
  run: |
    ./run-tests-with-reports.sh > ci-test-results.log 2>&1
    
- name: Archive Test Reports
  uses: actions/upload-artifact@v3
  with:
    name: test-reports
    path: |
      target/surefire-reports/
      target/test-logs/
      test-execution-summary-*.txt
```

#### **Docker Integration:**
```dockerfile
# Copy test results out of container
COPY target/surefire-reports/ /output/surefire-reports/
COPY target/test-logs/ /output/test-logs/
COPY test-execution-summary-*.txt /output/
```

## Troubleshooting

### Common Issues and Solutions

#### **1. No Log Files Generated**
```bash
# Ensure logging configuration is found
ls -la src/test/resources/logging-test.properties

# Check Java logging system property
mvn test -Djava.util.logging.config.file=src/test/resources/logging-test.properties -X
```

#### **2. Incomplete Output Capture**
```bash
# Use explicit output redirection
mvn test 2>&1 | tee complete-test-output.log
```

#### **3. Large Log Files**
```bash
# Rotate logs automatically (configured in logging-test.properties)
# Files are limited to 10MB with 5 backup files

# Manual cleanup
find target/test-logs -name "*.log" -size +50M -delete
```

### Identify Slow Tests
```bash
# Run with timing information
mvn test -pl apex-demo -Dtest="*Test" | grep "Time elapsed"
```

### Profile Test Execution
```bash
# Enable detailed timing
mvn test -pl apex-demo -X -Dtest="AllDemosRunnerTest"
```

### Skip Known Slow Tests During Development
```bash
# Development mode - skip heavy tests
mvn test -pl apex-demo \
    -Dtest="!AllDemosRunnerTest,!AllDemosRunnerCommandLineTest,!*Integration*Test"
```

## Pro Tips

1. **Use Test Profiles**: Create Maven profiles for different test scenarios
2. **Parallel Execution**: Use `-T` flag for faster execution on multi-core systems
3. **Selective Testing**: Focus on changed areas during development
4. **CI Optimization**: Use different test suites for different CI stages
5. **Local Development**: Use fast tests for immediate feedback
6. **Pre-Commit**: Use medium tests for comprehensive validation
7. **Nightly Builds**: Use full test suite including integration tests
8. **Regular Monitoring**: Run comprehensive tests weekly with permanent logging
9. **Trend Analysis**: Compare execution times and error patterns over time
10. **Issue Classification**: Always distinguish intentional test errors from real issues
11. **Archive Management**: Keep test reports for compliance and debugging purposes
12. **Performance Tracking**: Monitor rule evaluation times and resource usage

## Configuration Details

### Maven Surefire Plugin Configuration

The project is configured with enhanced Surefire settings:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.1.2</version>
    <configuration>
        <!-- Permanent Test Logging Configuration -->
        <redirectTestOutputToFile>true</redirectTestOutputToFile>
        <reportsDirectory>${project.build.directory}/surefire-reports</reportsDirectory>
        
        <!-- Detailed logging for all test output -->
        <trimStackTrace>false</trimStackTrace>
        <printSummary>true</printSummary>
        <reportFormat>brief</reportFormat>
        
        <!-- Capture all system output -->
        <systemPropertyVariables>
            <java.util.logging.config.file>src/test/resources/logging-test.properties</java.util.logging.config.file>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

### Logging Configuration Files

#### **apex-core/src/test/resources/logging-test.properties**
- Comprehensive logging configuration for core module
- Captures all rule evaluation, data source operations, and YAML processing
- Distinguishes between intentional test errors and real issues

#### **apex-demo/src/test/resources/logging-test.properties**
- Demo-specific logging configuration
- Captures demo execution, examples, and integration scenarios
- Enhanced logging for financial validation and custody operations

## Support

For questions about test logging and reporting:
- Review the generated summary reports first
- Check the detailed logs in `target/test-logs/`
- Analyze XML reports for specific test failures
- Use the troubleshooting guide above

---

**Generated by APEX Rules Engine Test Framework**  
**Last Updated:** 2025-08-12

**Result: You can now run apex-demo tests at any granularity level, from 30-second quick validations to comprehensive 5-minute full test suites with complete logging and reporting capabilities!**
