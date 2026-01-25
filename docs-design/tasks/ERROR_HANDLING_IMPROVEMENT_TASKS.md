# APEX Error Handling Improvement Tasks

**Document Version:** 1.1  
**Created:** January 24, 2026  
**Updated:** January 24, 2026  
**Status:** Draft  
**Branch:** refactor/rules-engine-decomposition

## Governing Document

⚠️ **IMPORTANT**: All tasks in this document MUST comply with the requirements specified in:
- **[APEX_ERROR_HANDLING_GUIDE.md](../APEX_ERROR_HANDLING_GUIDE.md)** (v2.2) - The authoritative guide for APEX error handling

Key principles from the guide that govern this work:
1. **ResultType vs Severity distinction** - `ResultType` indicates operation outcome (MATCH, NO_MATCH, ERROR, ENRICHMENT_FAILURE); `Severity` indicates business importance (CRITICAL, ERROR, WARNING, INFO)
2. **Fail-fast for enrichments/transformations** - These always use fail-fast error handling by default
3. **Exception hierarchy** - Use APEX-specific exceptions (`RuleEngineException`, `DataSourceException`, `EnrichmentException`, etc.)
4. **Error propagation pattern** - All errors must return `RuleResult` with appropriate `ResultType`
5. **SeverityConstants** - Always use `SeverityConstants.ERROR`, `SeverityConstants.WARNING`, etc. - never hardcoded strings

---

## Executive Summary

Analysis of the `apex-core/test-output-full.txt` log file reveals several areas where error handling can be improved for better operational clarity, debugging, and graceful error propagation. This document outlines the specific tasks required to address these issues while maintaining compliance with the APEX Error Handling Guide.

---

## Issue Categories

### Category 1: Intentional Test Errors Not Clearly Identified in Logs

**Problem:** Tests that intentionally trigger error conditions produce log output that is indistinguishable from actual runtime errors. This makes log analysis confusing and can lead to false positives when monitoring for real issues.

**Evidence from Logs:**
```
18:50:29.678 [main] WARN  d.m.a.c.config.ConfigurationContext - Failed to load file: ...test-config.yaml
dev.mars.apex.core.config.yaml.YamlConfigurationException: Rule name is required for rule: test-rule
```

There is no indication this is an **expected** test scenario.

**Contrast with Good Practice (already exists in some places):**
```
18:50:29.479 [main] ERROR d.m.a.c.s.s.ScenarioStageExecutor - [TEST-EXPECTED-ERROR] Stage 'component-processing' failed
18:50:29.506 [main] WARN  d.m.a.c.s.s.ScenarioStageExecutor - [TEST-EXPECTED-WARNING] Stage 'component-processing' failed
```

---

### Category 2: Incorrect Log Levels for Error Conditions

**Problem:** Several error conditions are logged at WARN level when they should be ERROR level, making it difficult to properly monitor and alert on application issues.

**Evidence from Logs:**
```
18:50:29.701 [main] WARN  d.m.a.c.config.ConfigurationContext - Failed to load file: ...circular-classpath-a.yaml
dev.mars.apex.core.config.yaml.YamlConfigurationException: Failed to load component...
```

A configuration load **failure** should be ERROR, not WARN.

---

### Category 3: Generic Exceptions Instead of APEX-Specific Exceptions

**Problem:** Several code paths throw generic Java exceptions (`RuntimeException`, `IllegalArgumentException`, `NullPointerException`) instead of APEX-specific exceptions, making error handling and recovery less precise.

**APEX Error Handling Guide Reference:** Section "Exception Hierarchy" (lines 1319-1614)

The guide defines a comprehensive exception hierarchy that MUST be used:
- `RuleEngineException` - Base exception with `errorCode` and `context`
- `RuleEvaluationException` - For rule evaluation failures (includes `ruleName`, `expression`, `suggestion`)
- `RuleValidationException` - For configuration validation errors
- `RuleConfigurationException` - For configuration issues
- `DataSourceException` - For data source operations (includes `ErrorType` enum)
- `EnrichmentException` - For enrichment processing failures

**Evidence from Logs:**
```
java.lang.NullPointerException: Cannot invoke "...CacheManager.get(String)" because "this.cacheManager" is null
    at dev.mars.apex.core.service.data.external.cache.CacheDataSource.getData(CacheDataSource.java:160)

java.lang.RuntimeException: Field transformation failed: EL1004E: Method call: Method nonExistentMethod()...
    at dev.mars.apex.core.service.transformation.YamlTransformationProcessor.processFieldTransformation

java.lang.IllegalArgumentException: Transformation invalid-transformation has no type specified
```

---

### Category 4: Errors Not Properly Propagated to RuleResult

**Problem:** Some error conditions produce stack traces in logs but may not properly propagate structured error information through the `RuleResult` back to the caller.

**APEX Error Handling Guide Reference:** Section "Error Propagation Pattern" (lines 227-256) and "How They Work Together" (lines 145-175)

The guide specifies the exact pattern for error propagation:
```java
// 1. Process item (rule, enrichment, transformation)
RuleResult itemResult = processItem(item, yamlConfig, enrichedData);

// 2. Check for ERROR result type
if (itemResult.getResultType() == RuleResult.ResultType.ERROR) {
    overallSuccess = false;
    failureMessages.add("Processing failed: " + itemResult.getMessage());
    // Optionally fail-fast or continue collecting errors
}
```

**Key Requirements from Guide:**
- `ResultType.ERROR` = System failure, always stops processing (unless explicitly handled)
- `ResultType.ENRICHMENT_FAILURE` = Specific type for enrichment failures
- For enrichments/transformations: Always fail-fast (guide section lines 760-810)

**Requirement:** Every error that occurs during rule/enrichment/transformation processing MUST:
1. Be captured in the `RuleResult`
2. Include a meaningful, actionable error message
3. Include the error code (if applicable)
4. Preserve the original exception chain for debugging

---

### Category 5: Full Stack Traces at Inappropriate Log Levels

**Problem:** Full stack traces are logged at WARN level for expected validation errors, cluttering logs and making it harder to identify actual issues.

**Evidence from Logs:**
```
18:50:29.678 [main] WARN  d.m.a.c.config.ConfigurationContext - Failed to load file: ...
dev.mars.apex.core.config.yaml.YamlConfigurationException: Rule name is required...
    at dev.mars.apex.core.config.yaml.YamlConfigurationLoader.validateRule(...)
    at dev.mars.apex.core.config.yaml.YamlConfigurationLoader.validateRules(...)
    ... (85 more lines)
```

---

## Detailed Task List

### Task 1: Add Test Context Markers for Intentional Errors

**Priority:** High  
**Effort:** Medium  
**Files to Modify:**
- Test classes that intentionally trigger error conditions
- Logging framework configuration (optional)

**Requirements:**

1.1. Create a standardized prefix convention for test-expected errors:
   - `[TEST-EXPECTED-ERROR]` - For errors that are intentionally triggered
   - `[TEST-EXPECTED-WARNING]` - For warnings that are intentionally triggered
   - `[TEST-VALIDATION]` - For validation test scenarios

1.2. Update all test methods that intentionally trigger errors to:
   - Log a clear "TEST: Triggering intentional error" message BEFORE the operation
   - Ensure the error message includes the test context
   - Log a "TEST: Error correctly caught" message AFTER successful assertion

1.3. Consider creating a test utility method:
```java
// Proposed utility
TestErrorContext.withExpectedError("testing invalid rule configuration", () -> {
    // code that triggers error
});
```

**Source Classes Producing WARN/ERROR (by count, from test-output-latest.txt):**

| # | Class | WARN | ERROR | Total | Status |
|---|-------|------|-------|-------|--------|
| 1 | ScenarioStageExecutor | 95 | 63 | 158 | ✅ DONE |
| 2 | ScenarioRegistryLoader | 5 | 57 | 62 | ✅ DONE |
| 3 | DatasetSignature | 47 | 0 | 47 | ✅ DONE (fixed YAML configs to add key-field) |
| 4 | PipelineExecutor | 0 | 39 | 39 | ✅ DONE |
| 5 | PipelineExecutionManager | 31 | 7 | 38 | ⚠️ PARTIAL (ErrorHandlingTests done) |
| 6 | DatabaseDataSource | 0 | 27 | 27 | ❌ TODO |
| 7 | RulesEngine | 0 | 21 | 21 | ⚠️ PARTIAL (ErrorHandlingTests done) |
| 8 | YamlTransformationProcessor | 0 | 20 | 20 | ❌ TODO |
| 9 | DataSourceFactory | 0 | 18 | 18 | ❌ TODO |
| 10 | DatasetLookupService | 17 | 0 | 17 | ❌ TODO |
| 11 | UnifiedRuleEvaluator | 15 | 14 | 29 | ❌ TODO |
| 12 | DatabaseHealthIndicator | 13 | 0 | 13 | ❌ TODO |
| 13 | DataTypeScenarioService | 10 | 0 | 10 | ❌ TODO |
| 14 | ScenarioConfiguration | 8 | 0 | 8 | ❌ TODO |
| 15 | YamlDependencyAnalyzer | 7 | 0 | 7 | ❌ TODO |
| 16 | ValidationService | 6 | 0 | 6 | ❌ TODO |
| 17 | GenericTransformerService | 5 | 0 | 5 | ❌ TODO |
| 18 | JdbcParameterUtils | 5 | 0 | 5 | ❌ TODO |
| 19 | ConditionalChainingExecutor | 4 | 0 | 4 | ❌ TODO |

**Affected Test Classes (from log analysis):**
- `ConfigurationContextTest$BulkLoadingTests`
- `CacheDataSourceTest`
- `RulesEngineErrorPropagationTest`
- `YamlTransformationProcessorTest`
- Various validation tests

---

### Task 2: Correct Log Levels for Error Conditions

**Priority:** High  
**Effort:** Low  
**Files to Modify:**

| File | Current Level | Required Level | Condition |
|------|---------------|----------------|-----------|
| `ConfigurationContext.java` | WARN | ERROR | Failed to load file |
| `CacheDataSource.java` | ERROR | ERROR (OK) | Failed to get data from cache |
| `ComponentLoader.java` | (implied) | ERROR | Component load failure |
| `YamlTransformationProcessor.java` | ERROR | ERROR (OK) | Transformation failed |

**Rules for Log Level Selection:**

| Condition | Level | Rationale |
|-----------|-------|-----------|
| Configuration file failed to load | ERROR | Application may not function correctly |
| Configuration file failed validation | ERROR | Invalid configuration is a critical issue |
| Runtime processing error (recoverable) | WARN | Processing continues with degraded functionality |
| Runtime processing error (non-recoverable) | ERROR | Processing cannot continue |
| Expected test error | INFO with [TEST-EXPECTED] prefix | Not a real error |
| Stack traces / full exception details | DEBUG | Keep ERROR/WARN logs clean; details available when needed |

**Stack Trace Logging Pattern:**

All stack traces MUST be logged at DEBUG level, not ERROR or WARN. The error message should be logged at the appropriate level, with full exception details available via DEBUG:

```java
// CORRECT: Error message at ERROR, stack trace at DEBUG
logger.error("Pipeline '{}' failed after {}ms: {}", 
    pipeline.getName(), durationMs, e.getMessage());
logger.debug("Full exception details for pipeline '{}':", pipeline.getName(), e);

// WRONG: Stack trace at ERROR level
logger.error("Pipeline failed", e);  // Don't do this
```

This keeps production logs clean while preserving full debugging information when needed.

---

### Task 3: Create APEX-Specific Exception Types

**Priority:** High  
**Effort:** Medium  
**APEX Error Handling Guide Compliance:** Section "Exception Hierarchy" (lines 1319-1614)

The guide already defines exception patterns that MUST be followed:

**Existing Exception Classes (per guide):**
- `RuleEngineException` - Base with `errorCode`, `context`
- `RuleEvaluationException` - With `ruleName`, `expression`, `suggestion`
- `RuleValidationException` - With `validationErrors` list
- `RuleConfigurationException` - With `configurationElement`, `expectedFormat`
- `DataSourceException` - With `ErrorType` enum (CONNECTION_ERROR, CONFIGURATION_ERROR, EXECUTION_ERROR, etc.)
- `EnrichmentException` - For enrichment processing

**Files to Create/Modify:**

3.1. **Create new exception class:** `ApexTransformationException`
```java
Location: dev.mars.apex.core.exception.ApexTransformationException
Purpose: Wrap all transformation-related errors (following RuleEngineException pattern)
Fields: transformationId, expression, originalValue, errorCode, context
```

Must follow the guide's exception pattern:
```java
public class ApexTransformationException extends RuleEngineException {
    private final String transformationId;
    private final String expression;
    private final Object originalValue;

    public ApexTransformationException(String transformationId, String expression, 
                                        String message, Object originalValue) {
        super("APEX-TRANS-001", message, "Transformation: " + transformationId);
        this.transformationId = transformationId;
        this.expression = expression;
        this.originalValue = originalValue;
    }
}
```

3.2. **Create new exception class:** `ApexCacheException`
```java
Location: dev.mars.apex.core.exception.ApexCacheException
Purpose: Wrap all cache-related errors (following DataSourceException pattern)
Fields: cacheName, operation, key, errorType (from DataSourceException.ErrorType enum)
```

3.3. **Update existing code to use specific exceptions:**

| Location | Current Exception | New Exception | Guide Reference |
|----------|-------------------|---------------|-----------------|
| `CacheDataSource.getData()` | `NullPointerException` | `DataSourceException.configurationError()` | Lines 1420-1430 |
| `YamlTransformationProcessor.processFieldTransformation()` | `RuntimeException` | `ApexTransformationException` | New class |
| Transformation validation | `IllegalArgumentException` | `RuleConfigurationException` | Lines 1401-1415 |

**Static Factory Methods (per guide pattern):**
```java
// Per APEX Error Handling Guide Section "Exception Hierarchy"
ApexTransformationException.expressionError(transformationId, expression, message);
ApexCacheException.notInitialized(cacheName);
ApexCacheException.lookupFailed(cacheName, key, cause);
```

---

### Task 4: Ensure Error Propagation to RuleResult

**Priority:** Critical  
**Effort:** High  
**APEX Error Handling Guide Compliance:** Sections "Error Propagation Pattern" (lines 227-256), "Enrichment Error Handling" (lines 760-810), "Transformation Error Handling" (lines 812-860)

**Files to Modify:**

4.1. **Audit all error paths in:**
- `RulesEngine.evaluate()`
- `RulesEngine.evaluateSequential()` 
- `RulesEngine.evaluateYamlConfigurationSequentially()` (lines 1186-1201 per guide)
- `RulesEngine.processEnrichments()` (lines 1220-1230 per guide)
- `RulesEngine.processTransformations()` (lines 1370-1380 per guide)
- `UnifiedRuleEvaluator.evaluateRule()`
- `YamlEnrichmentProcessor.process()` (fail-fast pattern lines 1752-1760 per guide)
- `YamlTransformationProcessor.processTransformation()` (fail-fast pattern lines 143-152 per guide)
- `ScenarioStageExecutor.executeStage()`

**Guide-Mandated Error Handling Pattern:**

Per guide section "Enrichment Error Handling" (lines 780-800):
```java
// Check for enrichment errors
if (enrichmentResult.getResultType() == RuleResult.ResultType.ERROR) {
    overallSuccess = false;
    failureMessages.add("Enrichment processing failed: " + enrichmentResult.getMessage());
    logger.error("CRITICAL: Enrichment processing failed: {}", enrichmentResult.getMessage());
    // Return error immediately (fail-fast)
    return RuleResult.error("enrichments", enrichmentResult.getMessage(), SeverityConstants.ERROR);
}
```

Per guide section "Transformation Error Handling" (lines 825-845):
```java
// Check for transformation errors
if (transformationResult.getResultType() == RuleResult.ResultType.ERROR) {
    overallSuccess = false;
    failureMessages.add("Transformation processing failed: " + transformationResult.getMessage());
    logger.error("CRITICAL: Transformation processing failed: {}", transformationResult.getMessage());
    // Return error immediately (fail-fast)
    return RuleResult.error("transformations", transformationResult.getMessage(), SeverityConstants.ERROR);
}
```

4.2. **RuleResult Error Requirements:**

Per APEX Error Handling Guide "ResultType vs Severity" section (lines 128-175):

Every `RuleResult` returned from an error condition MUST contain:

```java
// Per guide "RulePerformanceMetrics" and error propagation patterns
RuleResult errorResult = RuleResult.builder()
    .resultType(RuleResult.ResultType.ERROR)      // or ENRICHMENT_FAILURE per guide
    .ruleName(ruleName)                           // REQUIRED
    .ruleId(ruleId)                               // REQUIRED
    .errorCode(ApexErrorCode.XXX)                 // REQUIRED - from standardized codes
    .errorMessage(userFriendlyMessage)            // REQUIRED - actionable message
    .severity(SeverityConstants.ERROR)            // REQUIRED - use SeverityConstants (guide section "Severity Constants")
    .errorDetails(technicalDetails)               // OPTIONAL - for debugging
    .originalException(exception)                 // OPTIONAL - preserved chain
    .processingTimeMs(duration)                   // REQUIRED - for metrics (guide "Monitoring and Metrics" section)
    .build();
```

**Severity Constant Usage (per guide section lines 259-275):**
```java
// CORRECT - Use SeverityConstants
import dev.mars.apex.core.constants.SeverityConstants;
String severity = SeverityConstants.ERROR;
String severity = SeverityConstants.WARNING;
String severity = SeverityConstants.CRITICAL;
String severity = SeverityConstants.INFO;

// INCORRECT - Never use hardcoded strings
String severity = "ERROR";  // Don't do this
```

4.3. **Error Message Format Standard:**

```
[ERROR_CODE] Brief description: specific context

Examples:
[APEX-TRANS-001] Transformation failed: Expression 'value.nonExistentMethod()' is invalid for field 'calculatedField'
[APEX-RULE-002] Rule evaluation failed: Property 'missingField' not found in context for rule 'validateCustomer'
[APEX-CACHE-001] Cache lookup failed: CacheDataSource 'customer-cache' not initialized
```

4.4. **Create Error Code Registry:**

Create `docs/APEX_ERROR_CODES.md` with standardized error codes:

| Code | Category | Description |
|------|----------|-------------|
| APEX-CFG-001 | Configuration | Missing required field |
| APEX-CFG-002 | Configuration | Invalid field value |
| APEX-CFG-003 | Configuration | Circular reference detected |
| APEX-RULE-001 | Rule | Rule evaluation failed |
| APEX-RULE-002 | Rule | Property not found |
| APEX-TRANS-001 | Transformation | Expression evaluation failed |
| APEX-TRANS-002 | Transformation | Type conversion failed |
| APEX-CACHE-001 | Cache | Cache not initialized |
| APEX-CACHE-002 | Cache | Cache lookup failed |
| APEX-ENRICH-001 | Enrichment | Lookup failed |
| APEX-ENRICH-002 | Enrichment | Data source unavailable |

---

### Task 5: Reduce Stack Trace Verbosity for Expected Errors

**Priority:** Medium  
**Effort:** Low  
**Files to Modify:**

5.1. **Update logging pattern for validation errors:**

```java
// Current (verbose)
log.warn("Failed to load file: {}", filePath, exception);

// Proposed (concise for expected validation errors)
log.warn("Failed to load file: {} - {}", filePath, exception.getMessage());
log.debug("Full stack trace for debugging:", exception);
```

5.2. **Locations to update:**
- `ConfigurationContext.loadSingleFile()` (line ~554)
- `ConfigurationContext.loadFromDirectory()` (line ~522)
- `ComponentLoader.loadComponent()` (line ~159)
- `YamlConfigurationLoader.validateRule()` (line ~1451)

---

### Task 6: Add Integration Tests for Error Propagation

**Priority:** High  
**Effort:** Medium  
**APEX Error Handling Guide Compliance:** Section "Best Practices" point 5 "Test Error Scenarios" (lines 977-992)

**Files to Create:**

6.1. Create `RuleResultErrorPropagationIntegrationTest.java`:

Test scenarios (per guide's error propagation patterns):
- Configuration validation error → RuleResult contains error details
- Rule evaluation error → RuleResult contains error details  
- Transformation error → RuleResult contains error details with `ResultType.ERROR` (fail-fast)
- Enrichment error → RuleResult contains error details with `ResultType.ERROR` or `ENRICHMENT_FAILURE` (fail-fast)
- Nested component error → RuleResult contains error details with full context
- Recovery scenarios → Test recovery strategies per guide section "Recovery Strategies" (lines 284-340)

6.2. Each test MUST verify (per guide "Error Propagation Pattern"):
```java
// Per guide section "How They Work Together" (lines 145-175)
assertThat(result.getResultType()).isIn(
    RuleResult.ResultType.ERROR, 
    RuleResult.ResultType.ENRICHMENT_FAILURE
);
assertThat(result.getErrorCode()).isNotNull();
assertThat(result.getMessage()).contains(expectedContext);
assertThat(result.getRuleName()).isNotNull();  // or transformationId, enrichmentId

// Per guide section "Severity Levels" - verify severity is from SeverityConstants
assertThat(result.getSeverity()).isIn(
    SeverityConstants.CRITICAL,
    SeverityConstants.ERROR,
    SeverityConstants.WARNING,
    SeverityConstants.INFO
);
```

6.3. Test Error Recovery Configuration (per guide sections lines 342-500):
```java
@Test
void testErrorRecoveryForWarnings() {
    // Per guide Best Practices section point 5
    ErrorRecoveryConfig config = new ErrorRecoveryConfig();
    SeverityRecoveryPolicy warningPolicy = new SeverityRecoveryPolicy();
    warningPolicy.setRecoveryEnabled(true);
    warningPolicy.setStrategy("CONTINUE_WITH_DEFAULT");
    config.setSeverityPolicy(SeverityConstants.WARNING, warningPolicy);

    assertTrue(config.isRecoveryEnabledForSeverity(SeverityConstants.WARNING));
}
```

---

## Implementation Order

1. **Phase 1 - Foundation** (Week 1)
   - Task 3: Create APEX-specific exception types
   - Task 4.4: Create error code registry document

2. **Phase 2 - Core Changes** (Week 2)
   - Task 4.1-4.3: Update error propagation to RuleResult
   - Task 2: Correct log levels

3. **Phase 3 - Cleanup** (Week 3)
   - Task 5: Reduce stack trace verbosity
   - Task 1: Add test context markers

4. **Phase 4 - Validation** (Week 4)
   - Task 6: Add integration tests
   - Full regression testing

---

## Acceptance Criteria

Per APEX Error Handling Guide v2.2 requirements:

1. **All intentional test errors** are prefixed with `[TEST-EXPECTED-*]` in log output
2. **No NullPointerException or RuntimeException** thrown from APEX core code (only APEX-specific exceptions per guide "Exception Hierarchy")
3. **All error conditions** at ERROR log level, warnings at WARN level (per guide "Severity Levels" table)
4. **Every failed operation** returns a `RuleResult` with:
   - Appropriate `ResultType` (ERROR or ENRICHMENT_FAILURE per guide section "ResultType Enum")
   - Non-null `errorCode` (per guide "Exception Handling Best Practices")
   - Meaningful `errorMessage` with context (per guide pattern)
   - Severity from `SeverityConstants` - never hardcoded strings (per guide lines 259-275)
5. **Stack traces** only appear at DEBUG level for expected validation errors
6. **Integration tests** verify error propagation for all error categories
7. **Enrichment/Transformation errors** use fail-fast behavior by default (per guide section lines 760-810)
8. **Exception classes** follow the guide's hierarchy pattern with `errorCode` and `context` fields

---

## Related Documentation

- **[APEX_ERROR_HANDLING_GUIDE.md](../APEX_ERROR_HANDLING_GUIDE.md)** - ⚠️ GOVERNING DOCUMENT (v2.2) - Must be followed for all error handling
- [APEX_SUCCESS_ERROR_CODES_GUIDE.md](../APEX_SUCCESS_ERROR_CODES_GUIDE.md)
- [APEX_TECHNICAL_REFERENCE.md](../APEX_TECHNICAL_REFERENCE.md)

### Key Guide Sections for Reference

| Task | Guide Section | Lines |
|------|---------------|-------|
| Task 3 (Exception Types) | Exception Hierarchy | 1319-1614 |
| Task 4 (RuleResult Propagation) | Error Propagation Pattern | 227-256 |
| Task 4 (Enrichment Errors) | Enrichment Error Handling | 760-810 |
| Task 4 (Transformation Errors) | Transformation Error Handling | 812-860 |
| Task 2 (Log Levels) | Severity Levels | 259-283 |
| Task 6 (Integration Tests) | Best Practices - Test Error Scenarios | 977-992 |
| All Tasks | ResultType vs Severity | 128-175 |
| All Tasks | SeverityConstants Usage | 259-275 |

---

## Appendix A: Specific Log Entries Requiring Attention

### A.1 CacheDataSource NullPointerException
**File:** `CacheDataSource.java:160`  
**Log Line:** 33680, 33771  
```
java.lang.NullPointerException: Cannot invoke "...CacheManager.get(String)" because "this.cacheManager" is null
```
**Fix:** Add null check, throw `IllegalStateException` with message "CacheDataSource not initialized"

### A.2 Configuration Validation Full Stack Traces
**File:** `ConfigurationContext.java:554`  
**Log Line:** 796-884  
```
WARN - Failed to load file: ...test-config.yaml
dev.mars.apex.core.config.yaml.YamlConfigurationException: Rule name is required...
    (85 lines of stack trace)
```
**Fix:** Log message only at WARN, full stack at DEBUG

### A.3 Transformation RuntimeException
**File:** `YamlTransformationProcessor.java:339`  
**Log Line:** 12420  
```
java.lang.RuntimeException: Field transformation failed: EL1004E...
```
**Fix:** Create and throw `ApexTransformationException`

### A.4 Missing Test Context Markers
**Log Lines:** 1304-1345 (various test triggers without clear markers)
```
TEST: Triggering intentional error - testing invalid sequential-dependency pattern
```
**Note:** These DO have markers but the resulting error logs don't include `[TEST-EXPECTED-*]`

### A.5 Java Logging FileHandler Error
**File:** Infrastructure/Test Setup  
**Log Line:** 1350-1430  
```
Can't load log handler "java.util.logging.FileHandler"
java.nio.file.NoSuchFileException: target\test-logs\apex-tests-0.log.0.lck
```
**Fix:** Configure test logging properly or suppress JUL warnings during tests

---

## Appendix B: APEX Error Handling Guide Compliance Checklist

Before marking any task as complete, verify compliance with the governing document:

### B.1 Exception Handling Compliance

| Requirement | Guide Reference | Status |
|-------------|-----------------|--------|
| Use specific exception types, not generic | Lines 1614-1620 | ☐ |
| Include error codes in all exceptions | Lines 1332-1345 | ☐ |
| Preserve exception chain with cause | Lines 1632-1640 | ☐ |
| Use static factory methods where available | Lines 1645-1655 | ☐ |
| Check `retryable` flag for DataSourceException | Lines 1660-1670 | ☐ |

### B.2 RuleResult Compliance

| Requirement | Guide Reference | Status |
|-------------|-----------------|--------|
| Set ResultType to ERROR or ENRICHMENT_FAILURE for failures | Lines 136-143 | ☐ |
| Include severity from SeverityConstants | Lines 259-275 | ☐ |
| Include meaningful error message | Lines 145-175 | ☐ |
| Include performance metrics | Lines 1020-1100 | ☐ |

### B.3 Fail-Fast Behavior Compliance

| Requirement | Guide Reference | Status |
|-------------|-----------------|--------|
| Enrichments use fail-fast by default | Lines 760-810 | ☐ |
| Transformations use fail-fast by default | Lines 812-860 | ☐ |
| Return RuleResult.error() immediately on failure | Lines 780-800, 825-845 | ☐ |
| Log CRITICAL prefix for fail-fast errors | Lines 785, 832 | ☐ |

### B.4 Severity Constants Compliance

| Requirement | Guide Reference | Status |
|-------------|-----------------|--------|
| Import SeverityConstants class | Lines 259-262 | ☐ |
| Never use hardcoded severity strings | Lines 268-275 | ☐ |
| Use correct severity for error type | Lines 254-258 | ☐ |
