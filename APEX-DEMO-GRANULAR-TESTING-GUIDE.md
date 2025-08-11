# 🎯 APEX Demo Module - Granular Testing Guide

The apex-demo module contains extensive tests that can be run at various granular levels for faster development and targeted testing.

## 📊 **Test Categories Overview**

### **🚀 Quick Tests (< 5 seconds each)**
```bash
# Unit Tests - Fast execution
mvn test -Dtest=MockDataSourceTest -pl apex-demo
mvn test -Dtest=DataServiceManagerTest -pl apex-demo
mvn test -Dtest=ValidationTest -pl apex-demo
mvn test -Dtest=CollectionOperationsTest -pl apex-demo
mvn test -Dtest=DynamicMethodExecutionTest -pl apex-demo
```

### **⚡ Medium Tests (5-15 seconds each)**
```bash
# Demo Logic Tests - Moderate execution
mvn test -Dtest=QuickStartDemoTest -pl apex-demo
mvn test -Dtest=LayeredAPIDemoTest -pl apex-demo
mvn test -Dtest=CommoditySwapValidationTest -pl apex-demo
mvn test -Dtest=SpelAdvancedFeaturesTest -pl apex-demo
mvn test -Dtest=JsonXmlProcessingTest -pl apex-demo
```

### **🐌 Slow Tests (15+ seconds each)**
```bash
# Integration Tests - Longer execution
mvn test -Dtest=AllDemosRunnerIntegrationTest -pl apex-demo
mvn test -Dtest=CommoditySwapValidationBootstrapTest -pl apex-demo
mvn test -Dtest=YamlDependencyAnalysisIntegrationTest -pl apex-demo
```

### **🔥 Heavy Tests (30+ seconds each)**
```bash
# Full Demo Execution - Longest execution
mvn test -Dtest=AllDemosRunnerTest -pl apex-demo
mvn test -Dtest=AllDemosRunnerCommandLineTest -pl apex-demo
```

## 🎯 **Granular Testing Strategies**

### **1. By Test Class Pattern**
```bash
# Run all service tests
mvn test -Dtest="*Service*Test" -pl apex-demo

# Run all validation tests
mvn test -Dtest="*Validation*Test" -pl apex-demo

# Run all advanced feature tests
mvn test -Dtest="*Advanced*Test" -pl apex-demo

# Run all demo tests (excluding integration)
mvn test -Dtest="*Demo*Test" -Dtest="!*Integration*Test" -pl apex-demo
```

### **2. By Package Structure**
```bash
# Service layer tests only
mvn test -Dtest="dev.mars.apex.demo.service.*Test" -pl apex-demo

# Examples tests only
mvn test -Dtest="dev.mars.apex.demo.examples.*Test" -pl apex-demo

# Advanced features tests only
mvn test -Dtest="dev.mars.apex.demo.advanced.*Test" -pl apex-demo

# Bootstrap tests only
mvn test -Dtest="dev.mars.apex.demo.bootstrap.*Test" -pl apex-demo
```

### **3. By Execution Speed**
```bash
# Fast tests only (exclude integration and runner tests)
mvn test -pl apex-demo \
    -Dtest="!*Integration*Test,!*Runner*Test,!*Bootstrap*Test"

# Medium speed tests
mvn test -pl apex-demo \
    -Dtest="*Demo*Test" \
    -Dtest="!*Integration*Test,!*Runner*Test"

# Integration tests only
mvn test -pl apex-demo \
    -Dtest="*Integration*Test,*Bootstrap*Test"
```

### **4. By Functionality**
```bash
# Financial services tests
mvn test -Dtest="*Financial*Test,*Commodity*Test,*Swap*Test" -pl apex-demo

# Data processing tests
mvn test -Dtest="*Data*Test,*Json*Test,*Xml*Test,*Yaml*Test" -pl apex-demo

# Core demo functionality
mvn test -Dtest="QuickStart*Test,LayeredAPI*Test" -pl apex-demo

# Advanced SpEL features
mvn test -Dtest="*Spel*Test,*Advanced*Test,*Dynamic*Test" -pl apex-demo
```

## ⚡ **Performance-Optimized Test Execution**

### **Development Workflow (Fastest)**
```bash
# Quick smoke test - essential functionality only
mvn test -pl apex-demo \
    -Dtest="MockDataSourceTest,DataServiceManagerTest,ValidationTest"

# Core demo validation
mvn test -pl apex-demo \
    -Dtest="QuickStartDemoTest,CommoditySwapValidationTest"
```

### **Pre-Commit Validation (Medium)**
```bash
# Comprehensive but excluding heavy integration tests
mvn test -pl apex-demo \
    -Dtest="!*Integration*Test,!*Runner*Test,!*CommandLine*Test"
```

### **Full Validation (Complete)**
```bash
# All tests including integration (for CI/CD)
mvn test -pl apex-demo
```

## 🔧 **Advanced Granular Options**

### **1. Parallel Execution**
```bash
# Run tests in parallel (faster on multi-core systems)
mvn test -pl apex-demo -T 4 -Dtest="*Test"

# Parallel with specific test pattern
mvn test -pl apex-demo -T 2 \
    -Dtest="!*Integration*Test,!*Runner*Test"
```

### **2. Test Method Level**
```bash
# Run specific test methods
mvn test -pl apex-demo \
    -Dtest="QuickStartDemoTest#testBasicFunctionality"

mvn test -pl apex-demo \
    -Dtest="CommoditySwapValidationTest#testValidSwapValidation"

# Multiple specific methods
mvn test -pl apex-demo \
    -Dtest="QuickStartDemoTest#testBasicFunctionality,LayeredAPIDemoTest#testBasicFunctionality"
```

### **3. Timeout-Based Filtering**
```bash
# Skip tests that might timeout (useful for quick validation)
mvn test -pl apex-demo \
    -Dtest="!*Runner*Test,!*Integration*Test,!*Bootstrap*Test" \
    -Dsurefire.timeout=30
```

### **4. Category-Based Execution**
```bash
# Create custom test suites for different scenarios

# Quick Development Suite
mvn test -pl apex-demo \
    -Dtest="MockDataSourceTest,DataServiceManagerTest,ValidationTest,QuickStartDemoTest"

# Financial Validation Suite
mvn test -pl apex-demo \
    -Dtest="*Financial*Test,*Commodity*Test,*Swap*Test,*Validation*Test"

# Integration Suite
mvn test -pl apex-demo \
    -Dtest="*Integration*Test,*Bootstrap*Test"
```

## 📊 **Test Execution Time Estimates**

### **Quick Tests (< 30 seconds total)**
- MockDataSourceTest: ~2 seconds
- DataServiceManagerTest: ~3 seconds
- ValidationTest: ~2 seconds
- CollectionOperationsTest: ~3 seconds
- DynamicMethodExecutionTest: ~4 seconds

### **Medium Tests (30-60 seconds total)**
- QuickStartDemoTest: ~8 seconds
- LayeredAPIDemoTest: ~12 seconds
- CommoditySwapValidationTest: ~15 seconds
- SpelAdvancedFeaturesTest: ~10 seconds

### **Slow Tests (60+ seconds total)**
- AllDemosRunnerIntegrationTest: ~45 seconds
- CommoditySwapValidationBootstrapTest: ~30 seconds
- YamlDependencyAnalysisIntegrationTest: ~20 seconds

### **Heavy Tests (2+ minutes total)**
- AllDemosRunnerTest: ~90 seconds
- AllDemosRunnerCommandLineTest: ~120 seconds

## 🚀 **Recommended Development Workflows**

### **1. Feature Development (Fastest - 30 seconds)**
```bash
# Test only what you're working on
mvn test -pl apex-demo -Dtest="MockDataSourceTest,ValidationTest"
```

### **2. Local Validation (Medium - 2 minutes)**
```bash
# Core functionality without heavy integration
mvn test -pl apex-demo \
    -Dtest="!*Runner*Test,!*Integration*Test,!*CommandLine*Test"
```

### **3. Pre-Push Validation (Complete - 5 minutes)**
```bash
# Full test suite
mvn test -pl apex-demo
```

### **4. Continuous Integration (Optimized)**
```bash
# Parallel execution for CI environments
mvn test -pl apex-demo -T 4 \
    -Dmaven.test.failure.ignore=false \
    -Dsurefire.rerunFailingTestsCount=2
```

## 🎯 **Custom Test Scripts**

### **Create Fast Development Script**
```bash
#!/bin/bash
# fast-demo-tests.sh
echo "Running fast apex-demo tests..."
mvn test -pl apex-demo \
    -Dtest="MockDataSourceTest,DataServiceManagerTest,ValidationTest,QuickStartDemoTest" \
    -q
```

### **Create Medium Validation Script**
```bash
#!/bin/bash
# medium-demo-tests.sh
echo "Running medium apex-demo tests..."
mvn test -pl apex-demo \
    -Dtest="!*Runner*Test,!*Integration*Test,!*Bootstrap*Test" \
    -q
```

### **Create Integration Test Script**
```bash
#!/bin/bash
# integration-demo-tests.sh
echo "Running apex-demo integration tests..."
mvn test -pl apex-demo \
    -Dtest="*Integration*Test,*Bootstrap*Test" \
    -q
```

## 💡 **Pro Tips**

1. **Use Test Profiles**: Create Maven profiles for different test scenarios
2. **Parallel Execution**: Use `-T` flag for faster execution on multi-core systems
3. **Selective Testing**: Focus on changed areas during development
4. **CI Optimization**: Use different test suites for different CI stages
5. **Local Development**: Use fast tests for immediate feedback
6. **Pre-Commit**: Use medium tests for comprehensive validation
7. **Nightly Builds**: Use full test suite including integration tests

## 🔍 **Troubleshooting Slow Tests**

### **Identify Slow Tests**
```bash
# Run with timing information
mvn test -pl apex-demo -Dtest="*Test" | grep "Time elapsed"
```

### **Profile Test Execution**
```bash
# Enable detailed timing
mvn test -pl apex-demo -X -Dtest="AllDemosRunnerTest"
```

### **Skip Known Slow Tests During Development**
```bash
# Development mode - skip heavy tests
mvn test -pl apex-demo \
    -Dtest="!AllDemosRunnerTest,!AllDemosRunnerCommandLineTest,!*Integration*Test"
```

---

**🎯 Result: You can now run apex-demo tests at any granularity level, from 30-second quick validations to comprehensive 5-minute full test suites!**
