# Tests That Threw Exceptions

**Total unique test methods with exceptions**: 49
**Total exception occurrences**: 68

## CRITICAL FINDING: Incomplete Error Verification Pattern

**Date Discovered**: January 14, 2026

### The Problem

Many tests that appear to be "working" are actually **INCOMPLETE** because they only verify:
1. ✅ Test passes (0 failures, 0 errors)
2. ✅ Exception is caught and handled (no crash)
3. ❌ **MISSING**: Error messages are properly reported through `RuleResult` API

### The Pattern

Tests catch exceptions and handle them gracefully, but **fail to verify programmatic error reporting**:

```java
// INCOMPLETE PATTERN (what many tests currently do):
RuleResult result = engine.evaluate(config, inputData);
assertNotNull(result, "Result should not be null");  // ❌ NOT ENOUGH!

// COMPLETE PATTERN (what tests SHOULD do):
RuleResult result = engine.evaluate(config, inputData);
assertNotNull(result, "Result should not be null");
assertTrue(result.hasFailures(), "Result should indicate failure");  // ✅ VERIFY ERROR STATE
List<String> failureMessages = result.getFailureMessages();
assertFalse(failureMessages.isEmpty(), "Should have failure messages");  // ✅ VERIFY ERROR DETAILS
```

### Why This Matters

APEX is designed for **programmatic error handling** where calling applications:
- Must be able to detect failures without parsing logs
- Need access to detailed error messages for recovery strategies
- Require structured error information for reporting and monitoring

Tests that only verify "no exception thrown" are **missing critical verification** of the error handling contract.

### Example: testEnrichmentGroupWithDatabaseLookupMissingCustomer

**Before**: Test only checked `result != null` (INCOMPLETE)
**After**: Test now verifies:
1. `result.hasFailures()` returns `true`
2. `result.getFailureMessages()` contains error details
3. Exception is logged but caught (graceful handling)
4. Error is propagated through `RuleResult` API (programmatic access)

### Action Required

Tests marked as **"🐛 TESTING BUG"** may actually be **"⚠️ INCOMPLETE"** if they:
- Pass without test framework failures
- Catch and handle exceptions  
- But don't verify `RuleResult.hasFailures()` or `RuleResult.getFailureMessages()`

Each test needs individual verification to determine proper classification.

---

## Test Methods and Exceptions


### dev.mars.apex.core.config.yaml.EnrichmentCategoryInheritanceIntegrationTest

- **testCustomerProfileEnrichmentInheritance** - java.nio.file.NoSuchFileException (2 occurrence(s)) - **✅ NOT A BUG: Test PASSES (0 failures, 0 errors). The NoSuchFileException is from java.util.logging.FileHandler trying to create log file at `target/test-logs/apex-tests-0.log.0.lck` but directory doesn't exist. This is a harmless logging configuration warning that doesn't affect test functionality. The test completes successfully.**

### dev.mars.apex.core.engine.config.EnrichmentGroupDatabaseLookupTest

- **testEnrichmentGroupWithDatabaseLookupMissingCustomer** - dev.mars.apex.core.service.enrichment.EnrichmentException (1 occurrence(s)) - **✅ WORKING AS DESIGNED BUT WAS INCOMPLETE: Test PASSES (0 failures, 0 errors) after enhancements. Already has INTENTIONAL ERROR TEST log marker. The EnrichmentException is logged at ERROR level when required field 'CUSTOMER_NAME' is missing from lookup result, but the exception is properly caught and handled by the enrichment processor. Test was enhanced to verify `result.hasFailures()` returns true and `result.getFailureMessages()` contains error details. This is proper error handling for failed lookups with programmatic error reporting through RuleResult API.**

### dev.mars.apex.core.engine.config.RulesEngineErrorPropagationTest

- **testRulesEngineHandlesInvalidTransformationExpression** - org.springframework.expression.spel.SpelEvaluationException, java.lang.RuntimeException (2 occurrence(s)) - **🐛 TESTING BUG: Test verifies error propagation - errors should be caught and handled by RulesEngine with RuleResult showing failure. Stack dumps indicate errors aren't being properly caught.**
- **testRulesEngineHandlesMissingDatasource** - dev.mars.apex.core.service.enrichment.EnrichmentException (1 occurrence(s)) - **🐛 TESTING BUG: Test verifies error propagation - errors should be caught and handled by RulesEngine with RuleResult showing failure. Stack dumps indicate errors aren't being properly caught.**

### dev.mars.apex.core.service.data.external.cache.CacheDataSourceTest

- **testGetDataWithNullCacheManager** - java.lang.NullPointerException (1 occurrence(s)) - **🐛 BUG: Test expects null return for graceful handling but NullPointerException is thrown. Code should handle null cache manager gracefully and propagate error through the system properly.**
- **testGetDataWithUnsupportedType** - java.lang.NullPointerException (1 occurrence(s)) - **🐛 BUG: Test expects null return for graceful handling but NullPointerException is thrown. Code should handle null cache manager gracefully and propagate error through the system properly.**

### dev.mars.apex.core.service.data.external.config.DataSourceConfigurationServiceTest

- **testListenerExceptionHandling** - java.lang.RuntimeException (1 occurrence(s)) - **🐛 BUG: Test expects graceful handling with assertDoesNotThrow() but RuntimeException isn't being caught. Exception should be caught, propagated through APEX error handling system, and ultimately fail the Rules engine process.**

### dev.mars.apex.core.service.data.external.database.H2ConnectionStringTest

- **testH2TcpServerConnectionIntentionalFailure** - org.h2.jdbc.JdbcSQLNonTransientConnectionException (1 occurrence(s)) - **✅ INTENTIONAL: Uses assertThrows(YamlConfigurationException.class), renamed from testH2TcpServerConnection, added INTENTIONAL ERROR TEST log marker**

### dev.mars.apex.core.service.data.external.database.JdbcTemplateFactoryTest

- **testConnectionFailureIntentional** - org.postgresql.util.PSQLException (1 occurrence(s)) - **✅ INTENTIONAL: Uses assertThrows(DataSourceException.class), renamed from testConnectionFailure, added INTENTIONAL ERROR TEST log marker**
- **testH2TcpJdbcUrlIntentionalFailure** - org.h2.jdbc.JdbcSQLNonTransientConnectionException (1 occurrence(s)) - **✅ INTENTIONAL: Uses assertThrows(DataSourceException.class), renamed from testH2TcpJdbcUrl, added INTENTIONAL ERROR TEST log marker**
- **testInvalidDatabaseConfigurationIntentionalFailure** - org.postgresql.util.PSQLException (1 occurrence(s)) - **✅ INTENTIONAL: Uses assertThrows(DataSourceException.class), renamed from testInvalidDatabaseConfiguration, added INTENTIONAL ERROR TEST log marker**

### dev.mars.apex.core.service.data.external.ExternalDataSourceIntegrationTest

- **testErrorHandlingAndResilience** - java.net.ConnectException (2 occurrence(s)) - **✅ INTENTIONAL: Uses assertThrows(DataSourceException.class), already has INTENTIONAL ERROR TEST log marker**

### dev.mars.apex.core.service.data.external.file.CsvDataLoaderTest

- **testMissingFileIntentional** - java.nio.file.NoSuchFileException (1 occurrence(s)) - **✅ INTENTIONAL: Uses assertThrows(IOException.class), renamed from testMissingFile, added INTENTIONAL ERROR TEST log marker**

### dev.mars.apex.core.service.data.external.file.JsonDataLoaderTest

- **testInvalidEncodingIntentional** - java.nio.charset.UnsupportedCharsetException (1 occurrence(s)) - **✅ INTENTIONAL: Uses assertThrows(IOException.class), renamed from testInvalidEncoding, added INTENTIONAL ERROR TEST log marker**
- **testMissingFileIntentional** - java.nio.file.NoSuchFileException (1 occurrence(s)) - **✅ INTENTIONAL: Uses assertThrows(IOException.class), renamed from testMissingFile, added INTENTIONAL ERROR TEST log marker**

### dev.mars.apex.core.service.enrichment.EnrichmentServiceRuleResultTest

- **testEnrichObjectWithResult_RequiredFieldFailure** - dev.mars.apex.core.service.enrichment.EnrichmentException (1 occurrence(s)) - **✅ INTENTIONAL: Already has INTENTIONAL ERROR TEST log marker, verifies RuleResult shows failure for required field**

### dev.mars.apex.core.service.scenario.DataTypeScenarioServiceMalformedRegistryTest

- **testEmptyScenarioId** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **🐛 TESTING BUG: Uses assertDoesNotThrow() expecting graceful handling, but YamlConfigurationException is thrown. Should be caught and handled gracefully**

### dev.mars.apex.core.service.scenario.DataTypeScenarioServiceStageTest

- **testProcessData_WithLegacyScenario** - org.springframework.expression.spel.SpelEvaluationException (3 occurrence(s)) - **✅ INTENTIONAL: Already has INTENTIONAL ERROR TEST log marker, tests legacy scenario with error-handling YAML**
- **testProcessData_WithStageBasedScenario** - org.springframework.expression.spel.SpelEvaluationException (6 occurrence(s)) - **✅ INTENTIONAL: Already has INTENTIONAL ERROR TEST log marker, tests stage-based scenario with error-handling YAML**
- **testProcessDataWithScenario_LegacyProcessing** - org.springframework.expression.spel.SpelEvaluationException (3 occurrence(s)) - **✅ INTENTIONAL: Tests legacy processing with error-handling YAML**
- **testProcessDataWithScenario_StageBasedProcessing** - org.springframework.expression.spel.SpelEvaluationException (3 occurrence(s)) - **✅ INTENTIONAL: Tests stage-based processing with error-handling YAML**
- **testProcessDataWithStages_Success** - org.springframework.expression.spel.SpelEvaluationException (3 occurrence(s)) - **✅ INTENTIONAL: Already has INTENTIONAL ERROR TEST log marker, tests processing with error-handling YAML**

### dev.mars.apex.core.service.scenario.DataTypeScenarioServiceTest

- **testLoadScenariosFromRegistry** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **🐛 TESTING BUG: Uses assertDoesNotThrow() expecting successful load, but YamlConfigurationException is thrown. Should be caught and handled gracefully**
- **testLoadScenariosWithInvalidConfigIntentional** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **✅ INTENTIONAL: Uses assertThrows(RuntimeException.class), renamed from testLoadScenariosWithInvalidConfig, added INTENTIONAL ERROR TEST log marker**
- **testLoadScenariosWithMissingFileIntentional** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **✅ INTENTIONAL: Uses assertThrows(RuntimeException.class), renamed from testLoadScenariosWithMissingFile, added INTENTIONAL ERROR TEST log marker**
- **testScenarioLoadingErrors** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **🐛 TESTING BUG: Uses assertDoesNotThrow() expecting graceful handling, but YamlConfigurationException is thrown. Should be caught and handled gracefully**

### dev.mars.apex.core.service.scenario.RulesEngineScenarioRegistryTest

- **testEvaluateScenarioById** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **✅ WORKING AS DESIGNED: Exception is caught and handled gracefully by ScenarioStageExecutor. Test PASSES. The exception appears in logs but scenario execution completes with TERMINATED status as expected. This is proper error handling - configuration file not found errors are logged and execution terminates gracefully without throwing to caller.**

### dev.mars.apex.core.service.scenario.ScenarioStageExecutorFileHandlingTest

- **testDeeplyNestedMissingFilePath** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **🐛 TESTING BUG: Expects graceful handling returning ScenarioExecutionResult showing failure, but YamlConfigurationException is thrown as stack trace**
- **testFilePathWithSpaces** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **🐛 TESTING BUG: Expects graceful handling, but YamlConfigurationException is thrown as stack trace**
- **testFilePathWithSpecialCharacters** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **🐛 TESTING BUG: Expects graceful handling, but YamlConfigurationException is thrown as stack trace**
- **testInvalidFilePathCharacters** - java.nio.file.InvalidPathException (1 occurrence(s)) - **🐛 TESTING BUG: Expects graceful handling, but InvalidPathException is thrown as stack trace**
- **testMissingStageConfigFile** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **🐛 TESTING BUG: Expects graceful handling returning ScenarioExecutionResult showing failure, but YamlConfigurationException is thrown as stack trace**
- **testMultipleStagesWithMissingFiles** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **🐛 TESTING BUG: Expects graceful handling, but YamlConfigurationException is thrown as stack trace**
- **testRelativePathOutsideProject** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **🐛 TESTING BUG: Expects graceful handling, but YamlConfigurationException is thrown as stack trace**

### dev.mars.apex.core.service.scenario.ScenarioStageExecutorTest

- **testExecuteStages_FailurePolicyContinueWithWarnings** - java.lang.RuntimeException (1 occurrence(s)) - **🐛 TESTING BUG: Tests failure policy where failures should be gracefully handled through ScenarioExecutionResult. RuntimeException should be caught and converted to stage failure, not thrown as stack trace**
- **testExecuteStages_FailurePolicyFlagForReview** - java.lang.RuntimeException (1 occurrence(s)) - **🐛 TESTING BUG: Tests failure policy where failures should be gracefully handled. RuntimeException should be caught and converted to review flag, not thrown as stack trace**
- **testExecuteStages_FailurePolicyTerminate** - java.lang.RuntimeException (1 occurrence(s)) - **🐛 TESTING BUG: Tests failure policy where failures should be gracefully handled. RuntimeException should be caught and converted to termination result, not thrown as stack trace**
- **testExecuteStages_SkippedDueToDependencies** - java.lang.RuntimeException (1 occurrence(s)) - **🐛 TESTING BUG: Tests stage dependency handling. RuntimeException should be caught and result in skipped stages, not thrown as stack trace**

### dev.mars.apex.core.service.scenario.ScenarioStageMissingDependencyTest

- **testCaseSensitiveStageDependency** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **🐛 TESTING BUG: Expects graceful handling with case-sensitive dependency mismatch, but YamlConfigurationException is thrown as stack trace**
- **testDependencyChainWithMissingMiddleStage** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **🐛 TESTING BUG: Expects graceful handling for missing dependency in chain, but exception thrown**
- **testStageDependsOnMissingStage** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **🐛 TESTING BUG: Expects graceful handling for missing stage dependency, but exception thrown**
- **testStageMixedValidAndMissingDependencies** - dev.mars.apex.core.config.yaml.YamlConfigurationException (1 occurrence(s)) - **🐛 TESTING BUG: Expects graceful handling for mixed dependencies, but exception thrown**

### dev.mars.apex.core.service.transformation.YamlConditionalTransformationTest

- **testErrorHandling** - java.lang.RuntimeException (1 occurrence(s)) - **✅ INTENTIONAL: Already has INTENTIONAL ERROR TEST log marker, tests division by zero error handling, verifies RuleResult shows ERROR**

### dev.mars.apex.core.service.transformation.YamlTransformationProcessorDeprecationTest

- **testDeprecatedMethodCannotPropagateErrors** - java.lang.IllegalArgumentException (1 occurrence(s)) - **✅ INTENTIONAL: Already has INTENTIONAL ERROR TEST log marker, tests deprecated method with invalid transformation type**
- **testNewMethodPropagatesErrors** - java.lang.IllegalArgumentException (1 occurrence(s)) - **✅ INTENTIONAL: Already has INTENTIONAL ERROR TEST log marker, tests new method properly propagates errors via RuleResult**

### dev.mars.apex.core.service.transformation.YamlTransformationProcessorErrorHandlingTest

- **testCatchBlockHandlesTransformationException** - org.springframework.expression.spel.SpelEvaluationException, java.lang.RuntimeException (2 occurrence(s)) - **✅ INTENTIONAL: Already has INTENTIONAL ERROR TEST log marker, tests invalid SpEL accessing null object, verifies exception caught and returned as ERROR RuleResult**
- **testErrorResultContainsProperErrorMessage** - java.lang.ArithmeticException, java.lang.RuntimeException (2 occurrence(s)) - **✅ INTENTIONAL: Already has INTENTIONAL ERROR TEST log marker, tests division by zero, verifies error message and metadata in RuleResult**

### dev.mars.apex.core.service.transformation.YamlTransformationProcessorRuleResultTest

- **testErrorsTrackedInFailureMessages** - org.springframework.expression.spel.SpelEvaluationException, java.lang.RuntimeException (2 occurrence(s)) - **✅ INTENTIONAL: Already has INTENTIONAL ERROR TEST log marker, tests SpEL accessing missing property, verifies errors tracked in RuleResult.failureMessages**
- **testResultTypeErrorOnTransformationErrors** - java.lang.IllegalArgumentException (1 occurrence(s)) - **✅ INTENTIONAL: Already has INTENTIONAL ERROR TEST log marker, tests null expression in transformation, verifies RuleResult.resultType = ERROR**

---

## Summary Statistics

### By Category:
- **✅ INTENTIONAL** (26 tests): Properly use `assertThrows()` or verify error handling with RuleResult
- **✅ WORKING AS DESIGNED** (3 verified tests): Exceptions caught, proper error propagation verified
- **⚠️ INCOMPLETE** (1 fixed test): Now verifies RuleResult error reporting
- **🐛 TESTING BUG** (18 tests): Need individual verification - may be incomplete or real bugs
- **🐛 REAL BUG** (3 tests): Actual code defects requiring fixes

### Tests Requiring Individual Verification (18):
These tests need to be run individually to determine if they are:
1. **INCOMPLETE**: Catch exceptions but don't verify `RuleResult.hasFailures()` / `getFailureMessages()`
2. **REAL BUGS**: Exceptions not caught, should be handled gracefully

**Verification Process**:
```bash
# Run individual test
mvn test -Dtest=ClassName#testMethodName

# Check for:
# 1. Test passes (0 failures, 0 errors)?
# 2. Exception appears in logs but is caught?
# 3. Test verifies result.hasFailures() and result.getFailureMessages()?
```

**If test passes but doesn't verify error messages**, enhance with:
```java
// Add these imports if missing
import java.util.List;

// Add after result = engine.evaluate(...):
assertTrue(result.hasFailures(), "Result should indicate failure");
List<String> failureMessages = result.getFailureMessages();
assertNotNull(failureMessages, "Failure messages should not be null");
assertFalse(failureMessages.isEmpty(), "Should have failure messages");
logger.info("  Failure messages: " + failureMessages);
```

### Known Real Bugs (3):
1. **testGetDataWithNullCacheManager** - NullPointerException not handled gracefully
2. **testGetDataWithUnsupportedType** - NullPointerException not handled gracefully  
3. **testListenerExceptionHandling** - RuntimeException not caught by assertDoesNotThrow()

---

## Next Steps

1. **Verify remaining 18 tests individually** - Run each test to determine INCOMPLETE vs REAL BUG
2. **Enhance INCOMPLETE tests** - Add RuleResult error verification assertions
3. **Fix REAL BUGS** - Update code to handle exceptions gracefully with proper error propagation
4. **Rerun full test suite** - Verify all 1786 tests still pass with enhanced assertions
5. **Update this document** - Reclassify tests based on verification results
