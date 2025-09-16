# APEX Core Syntax Tests - Comprehensive Documentation

## Overview

This document provides comprehensive documentation for the APEX Core Syntax Test Suite, which validates all fundamental APEX functionality using real APEX services and comprehensive business logic testing.

## Test Suite Structure

### Phase 1.2: Core Syntax Elements Test Suite (COMPLETED)
**Total: 12 Test Classes | 48 Test Methods | 240 Enrichments**

## Individual Test Documentation

### 1. SpELExpressionTest
**Purpose**: Validates SpEL (Spring Expression Language) functionality in APEX
**Enrichments**: 20 total (5 per test method)
**Test Methods**:
- `testBasicSpELExpressions()` - Basic SpEL syntax and evaluation
- `testMathematicalOperations()` - Mathematical calculations and operations
- `testStringManipulation()` - String operations and transformations
- `testConditionalExpressions()` - Conditional logic and ternary operators

### 2. ConditionalLogicTest
**Purpose**: Validates conditional logic patterns and decision trees
**Enrichments**: 20 total (5 per test method)
**Test Methods**:
- `testSimpleConditionalLogic()` - Basic if-then-else logic
- `testComplexConditionalChains()` - Multi-level conditional chains
- `testNestedConditionalLogic()` - Nested conditional structures
- `testConditionalWithNullHandling()` - Null-safe conditional operations

### 3. FieldTransformationTest
**Purpose**: Validates field transformation operations and data mapping
**Enrichments**: 20 total (5 per test method)
**Test Methods**:
- `testBasicFieldTransformation()` - Simple field mappings
- `testComplexFieldTransformation()` - Complex transformation logic
- `testMultiFieldTransformation()` - Multiple field operations
- `testConditionalFieldTransformation()` - Conditional field mappings

### 4. LookupOperationsTest
**Purpose**: Validates lookup operations and dataset queries
**Enrichments**: 20 total (5 per test method)
**Test Methods**:
- `testBasicLookupOperations()` - Simple dataset lookups
- `testParameterizedLookupOperations()` - Parameterized queries
- `testMultiKeyLookupOperations()` - Multi-key lookup operations
- `testConditionalLookupOperations()` - Conditional lookup logic

### 5. CalculationEngineTest
**Purpose**: Validates calculation engine functionality and mathematical operations
**Enrichments**: 20 total (5 per test method)
**Test Methods**:
- `testBasicCalculations()` - Basic mathematical operations
- `testComplexCalculations()` - Complex mathematical formulas
- `testFinancialCalculations()` - Financial calculation patterns
- `testStatisticalCalculations()` - Statistical operations

### 6. ValidationRulesTest
**Purpose**: Validates validation rules and data quality checks
**Enrichments**: 20 total (5 per test method)
**Test Methods**:
- `testBasicValidationRules()` - Simple validation patterns
- `testComplexValidationRules()` - Complex validation logic
- `testCrossFieldValidation()` - Multi-field validation rules
- `testConditionalValidation()` - Conditional validation patterns

### 7. DataSourceConfigTest
**Purpose**: Validates data source configuration and connectivity
**Enrichments**: 20 total (5 per test method)
**Test Methods**:
- `testDatabaseConfiguration()` - Database connection configuration
- `testFileSystemConfiguration()` - File system data source configuration
- `testCacheConfiguration()` - Cache data source configuration
- `testExternalApiConfiguration()` - External API data source configuration

### 8. EnrichmentPatternsTest
**Purpose**: Validates enrichment patterns and orchestration
**Enrichments**: 18 total (4-5 per test method)
**Test Methods**:
- `testSequentialEnrichmentPattern()` - Sequential enrichment chains
- `testParallelEnrichmentPattern()` - Parallel enrichment execution
- `testConditionalEnrichmentPattern()` - Conditional enrichment logic
- `testAccumulativeEnrichmentPattern()` - Accumulative enrichment patterns

### 9. RuleGroupProcessingTest
**Purpose**: Validates rule group processing and workflows
**Enrichments**: 18 total (4-5 per test method)
**Test Methods**:
- `testSequentialRuleGroupProcessing()` - Sequential rule group execution
- `testParallelRuleGroupProcessing()` - Parallel rule group processing
- `testConditionalRuleGroupProcessing()` - Conditional rule group logic
- `testAggregatedRuleGroupProcessing()` - Aggregated rule group results

### 10. MetadataValidationTest
**Purpose**: Validates metadata validation and integrity checks
**Enrichments**: 20 total (5 per test method)
**Test Methods**:
- `testRequiredMetadataValidation()` - Required field validation
- `testDocumentTypeValidation()` - Document type validation
- `testConstraintValidation()` - Metadata constraint validation
- `testIntegrityValidation()` - Metadata integrity validation

### 11. ErrorHandlingTest
**Purpose**: Validates error handling patterns and recovery mechanisms
**Enrichments**: 20 total (5 per test method)
**Test Methods**:
- `testValidationErrorHandling()` - Validation error processing
- `testExpressionErrorHandling()` - Expression error handling
- `testRecoveryMechanisms()` - Error recovery strategies
- `testErrorClassificationAndLogging()` - Error classification and logging

### 12. ConfigurationLoadingTest
**Purpose**: Validates configuration loading mechanisms
**Enrichments**: 20 total (5 per test method)
**Test Methods**:
- `testBasicConfigurationLoading()` - Basic YAML configuration loading
- `testConfigurationValidation()` - Configuration structure validation
- `testPropertyResolution()` - Property placeholder resolution
- `testConfigurationCaching()` - Configuration caching mechanisms

## Debug Logging Features

### Enabling Debug Logging
```bash
# Enable debug logging
mvn test -Dapex.test.debug=true

# Enable verbose logging
mvn test -Dapex.test.verbose=true

# Enable both debug and verbose logging
mvn test -Dapex.test.debug=true -Dapex.test.verbose=true
```

### Debug Logging Levels
1. **Standard Logging**: Basic test progress and results
2. **Debug Logging** (`-Dapex.test.debug=true`): Detailed test execution steps
3. **Verbose Logging** (`-Dapex.test.verbose=true`): Comprehensive enrichment analysis

### Debug Output Examples
```
🔍 DEBUG: Loading YAML configuration from classpath: yaml/config-basic-loading-test.yaml
🔍 DEBUG: Configuration metadata: ID=config-basic-loading-test, Version=1.0.0, Type=rule-config
📝 VERBOSE: Input data breakdown: name=test-config, version=1.0.0, type=rule-config
📊 ENRICHMENT ANALYSIS for Basic Configuration Loading:
   • configName = test-config (String)
   • nameValidation = VALID_NAME (String)
   • versionValidation = VALID_VERSION (String)
```

## Critical Validation Checklist

### Before Running Tests
1. ✅ **Count enrichments in YAML** - Know exactly how many enrichments each test expects
2. ✅ **Verify test data completeness** - Ensure all enrichment conditions are triggered
3. ✅ **Check YAML syntax** - Validate YAML files are properly formatted

### During Test Execution
1. ✅ **Monitor log output** - Watch for "Processed: X out of Y" messages
2. ✅ **Verify 100% execution rate** - All enrichments must execute successfully
3. ✅ **Check for warnings** - Address any APEX service warnings

### After Test Completion
1. ✅ **Validate all assertions** - Every enrichment result must be asserted
2. ✅ **Review debug output** - Use debug logging to troubleshoot failures
3. ✅ **Verify business logic** - Ensure actual business operations are tested

## Troubleshooting Guide

### Common Issues and Solutions

#### Issue: "Processed: X out of Y" shows X < Y
**Cause**: Test data doesn't match YAML enrichment conditions
**Solution**: Review enrichment conditions and ensure test data triggers all enrichments

#### Issue: Assertion failures on expected values
**Cause**: Expected values don't match actual enrichment logic
**Solution**: Review YAML transformation expressions and update expected values

#### Issue: Configuration loading failures
**Cause**: YAML syntax errors or missing classpath resources
**Solution**: Validate YAML syntax and verify file locations

#### Issue: Null pointer exceptions
**Cause**: Missing null safety in SpEL expressions
**Solution**: Add null checks to transformation expressions

## Performance Considerations

### Test Execution Times
- **Individual Test**: ~0.5-1.0 seconds
- **Full Test Class**: ~2-4 seconds
- **Complete Test Suite**: ~30-60 seconds

### Memory Usage
- **Heap Usage**: ~100-200 MB during test execution
- **Configuration Cache**: ~10-50 MB depending on YAML complexity

## Best Practices

### Test Development
1. **Follow existing patterns** - Model after successful test implementations
2. **Use real APEX services** - Never simulate or mock APEX functionality
3. **Test business logic** - Focus on actual business operations, not YAML syntax
4. **Validate comprehensively** - Assert every enrichment result

### Debugging
1. **Enable debug logging** - Use system properties for detailed output
2. **Read logs carefully** - Pay attention to "Processed: X out of Y" messages
3. **Verify test data** - Ensure test data matches YAML conditions
4. **Check incrementally** - Test each change before proceeding

### Maintenance
1. **Keep documentation updated** - Update this document when adding new tests
2. **Review regularly** - Ensure tests remain relevant and comprehensive
3. **Monitor performance** - Watch for test execution time increases
4. **Update dependencies** - Keep APEX core dependencies current
