# APEX Rules Engine - Permanent Test Logging Guide

This guide explains how to create permanent log output from test reports for analysis, debugging, and compliance purposes.

## **Quick Start - Generate Permanent Test Reports**

### **Option 1: Use the Automated Scripts (Recommended)**

#### **Windows:**
```bash
# Run all tests with comprehensive logging
run-tests-with-reports.bat

# Run specific module
run-tests-with-reports.bat apex-core
run-tests-with-reports.bat apex-demo
```

#### **Linux/Mac:**
```bash
# Make script executable (first time only)
chmod +x run-tests-with-reports.sh

# Run all tests with comprehensive logging
./run-tests-with-reports.sh

# Run specific module
./run-tests-with-reports.sh apex-core
./run-tests-with-reports.sh apex-demo
```

### **Option 2: Manual Maven Commands**

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

## **Generated Output Locations**

After running tests, you'll find permanent reports in these locations:

### **1. Surefire Reports (Always Generated)**
```
target/surefire-reports/
├── TEST-*.xml                    # JUnit XML reports
├── *.txt                         # Text summaries
└── *-output.txt                  # Console output capture
```

### **2. Enhanced Logging Files (When Using Scripts)**
```
target/test-logs/
├── test-execution-TIMESTAMP.log  # Complete test execution log
├── apex-tests-*.log              # Detailed application logs
└── apex-demo-tests-*.log         # Demo-specific logs
```

### **3. Summary Reports (When Using Scripts)**
```
test-execution-summary-TIMESTAMP.txt  # Comprehensive summary
```

### **4. HTML Reports (When Using test-reports Profile)**
```
target/test-reports/
└── test-report.html              # HTML formatted report
```

## **Configuration Details**

### **Maven Surefire Plugin Configuration**

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

### **Logging Configuration Files**

#### **apex-core/src/test/resources/logging-test.properties**
- Comprehensive logging configuration for core module
- Captures all rule evaluation, data source operations, and YAML processing
- Distinguishes between intentional test errors and real issues

#### **apex-demo/src/test/resources/logging-test.properties**
- Demo-specific logging configuration
- Captures demo execution, examples, and integration scenarios
- Enhanced logging for financial validation and custody operations

## **Report Analysis Guide**

### **Understanding Test Output**

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

### **Key Metrics to Monitor**

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

## **Advanced Usage**

### **Custom Logging Levels**

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

### **Continuous Integration Integration**

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

## **Troubleshooting**

### **Common Issues and Solutions**

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

## **Report Examples**

### **Successful Test Execution Summary:**
```
APEX Rules Engine - Test Execution Summary
==============================================

Execution Timestamp: 20250811_160117
Module(s) Tested: apex-core
Test Exit Code: 0

Test Results Summary:
====================
APEX-CORE Module:
  Test Classes: 14

Generated Reports:
==================
  - XML Reports: target/surefire-reports/*.xml
  - Text Reports: target/surefire-reports/*.txt
  - Log Files: target/test-logs/*.log

Issue Analysis:
===============
  No critical issues detected in test execution
  Intentional test errors detected (this is expected)
```

### **Test Failure Analysis:**
```
Issue Analysis:
===============
  WARNING: Potential issues detected in test execution
  Review the detailed logs for analysis
  
Critical Errors Found:
  - Line 1247: SEVERE database connection timeout
  - Line 2156: ERROR rule evaluation failed unexpectedly
  
Recommendations:
  1. Check database connectivity
  2. Review rule configuration syntax
  3. Verify test data integrity
```

## **Best Practices**

1. **Regular Monitoring:** Run comprehensive tests weekly with permanent logging
2. **Trend Analysis:** Compare execution times and error patterns over time
3. **Issue Classification:** Always distinguish intentional test errors from real issues
4. **Archive Management:** Keep test reports for compliance and debugging purposes
5. **Performance Tracking:** Monitor rule evaluation times and resource usage

## **Support**

For questions about test logging and reporting:
- Review the generated summary reports first
- Check the detailed logs in `target/test-logs/`
- Analyze XML reports for specific test failures
- Use the troubleshooting guide above

---

**Generated by APEX Rules Engine Test Framework**  
**Last Updated:** 2025-08-02
