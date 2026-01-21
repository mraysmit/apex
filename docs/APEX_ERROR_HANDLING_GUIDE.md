# APEX Error Handling Guide


**Version:** 2.2  
**Date:** 2025-10-09  
**Author:** Mark Andrew Ray-Smith Cityline Ltd  

## Overview

The APEX Rules Engine provides a comprehensive, configurable error handling and recovery system that allows you to control how the system responds to failures during rule evaluation. This guide explains how to configure and use error handling in your APEX applications.

---

## Table of Contents

1. [Core Concepts](#core-concepts)
2. [ResultType vs Severity](#resulttype-vs-severity)
3. [Severity Levels](#severity-levels)
4. [Recovery Strategies](#recovery-strategies)
5. [Configuration](#configuration)
6. [YAML Configuration](#yaml-configuration)
7. [Programmatic Configuration](#programmatic-configuration)
8. [Default Behavior](#default-behavior)
9. [Usage Examples](#usage-examples)
10. [Enrichment and Transformation Error Handling](#enrichment-and-transformation-error-handling)
11. [Best Practices](#best-practices)
12. [Monitoring and Metrics](#monitoring-and-metrics)
13. [Advanced Topics](#advanced-topics)
14. [Exception Hierarchy](#exception-hierarchy)
15. [Troubleshooting](#troubleshooting)
16. [API Reference](#api-reference)
17. [Summary](#summary)

---

## Core Concepts

### Error Recovery System

The APEX error recovery system is built on three key principles:

1. **Severity-Based Policies**: Different error severities can have different recovery behaviors
2. **Configurable Strategies**: Choose how the system responds to failures (fail fast, continue, retry, skip)
3. **Backward Compatibility**: Default policies ensure existing applications continue to work without changes

### How It Works

When a rule evaluation fails:

1. The system determines the **severity** of the failure (CRITICAL, ERROR, WARNING, INFO)
2. It looks up the **recovery policy** for that severity level
3. If recovery is enabled, it applies the configured **recovery strategy**
4. The system logs the attempt and collects metrics (if enabled)
5. Returns either a recovered result or an error result

### Understanding Rule Conditions and Severity

**CRITICAL CONCEPT**: Severity-based error recovery **only applies when a rule's condition evaluates to FALSE**.

#### Rule Evaluation Logic

```
IF rule condition evaluates to TRUE:
   → Rule matched successfully
   → Always returns success (severity is irrelevant)
   → ResultType = MATCH, isSuccess = true, isTriggered = true

ELSE IF rule condition evaluates to FALSE:
   → Rule did not match
   → Check severity and error recovery configuration
   → IF severity is ERROR or CRITICAL AND recovery is disabled:
      → ResultType = ERROR, isSuccess = false (FAIL_FAST)
   → ELSE:
      → ResultType = NO_MATCH, isSuccess = true (continue processing)
```

#### Truth Tables for Rule Evaluation

**CRITICAL PRINCIPLE**: Business rule conditions are fixed business logic from your catalogue. You cannot change them. You must match your test data and configuration to the condition.

##### Example 1: Business Rule Condition `#'tradeId'] != null`

| Test Data (tradeId) | Condition Result | Severity | Recovery | Outcome | What This Tests |
|---------------------|------------------|----------|----------|---------|-----------------|
| "TRADE-001" | TRUE | ERROR | disabled | SUCCESS | Business rule matches - happy path |
| null | FALSE | ERROR | disabled | FAIL_FAST | Business rule doesn't match - error handling |
| "TRADE-001" | TRUE | WARNING | enabled | SUCCESS | Business rule matches - happy path |
| null | FALSE | WARNING | enabled | CONTINUE | Business rule doesn't match - recovery works |

##### Example 2: Business Rule Condition `#'tradeId'] == null`

| Test Data (tradeId) | Condition Result | Severity | Recovery | Outcome | What This Tests |
|---------------------|------------------|----------|----------|---------|-----------------|
| null | TRUE | ERROR | disabled | SUCCESS | Business rule matches - happy path |
| "TRADE-001" | FALSE | ERROR | disabled | FAIL_FAST | Business rule doesn't match - error handling |
| null | TRUE | WARNING | enabled | SUCCESS | Business rule matches - happy path |
| "TRADE-001" | FALSE | WARNING | enabled | CONTINUE | Business rule doesn't match - recovery works |

#### Testing Strategy

When writing tests, you need separate tests for different scenarios:

1. **Happy Path Test (Condition = TRUE)**
   - Test data must make the condition evaluate to TRUE
   - Expected outcome: SUCCESS (rule matches)
   - Tests that the business rule correctly identifies the case it's designed to detect

2. **Error Handling Test (Condition = FALSE with ERROR severity)**
   - Test data must make the condition evaluate to FALSE
   - Severity: ERROR or CRITICAL with recovery disabled
   - Expected outcome: FAIL_FAST
   - Tests that the system correctly fails when the business rule doesn't match

3. **Recovery Test (Condition = FALSE with recovery enabled)**
   - Test data must make the condition evaluate to FALSE
   - Severity: WARNING or INFO with recovery enabled
   - Expected outcome: CONTINUE (with recovery)
   - Tests that the recovery mechanism works correctly

#### Key Principles

1. **Business rule conditions are fixed** - they represent your business logic catalogue and cannot be changed
2. **Match test data to the condition** - ensure your test data produces the expected condition result (TRUE or FALSE)
3. **Configure severity appropriately** - use ERROR/CRITICAL for fail-fast, WARNING/INFO for recovery
4. **Test both paths** - every business rule should have tests for both condition = TRUE and condition = FALSE scenarios

---

## ResultType vs Severity

### Understanding the Distinction

APEX uses **two separate concepts** for error classification that work together but serve different purposes:

1. **ResultType** (System-Level): Indicates the **outcome** of an operation
2. **Severity** (Business-Level): Indicates the **importance** of a rule or validation

### ResultType Enum

`RuleResult.ResultType` represents the **actual outcome** of a rule evaluation or processing operation:

| ResultType | Meaning | Use Case |
|------------|---------|----------|
| **MATCH** | Rule condition evaluated to TRUE | Rule matched successfully, validation passed |
| **NO_MATCH** | Rule condition evaluated to FALSE (non-critical) | Rule didn't match, but processing can continue |
| **ERROR** | System failure or exception occurred | Rule evaluation failed due to exception, enrichment failed, transformation failed |
| **ENRICHMENT_FAILURE** | Enrichment processing failed | Specific type of error for enrichment operations |

### Severity Levels (Business Classification)

Severity is a **business classification** assigned to rules and validations:

- **CRITICAL**: Highest priority business rules
- **ERROR**: Important business validations
- **WARNING**: Optional validations
- **INFO**: Informational rules

### How They Work Together

The key pattern used throughout APEX is:

```java
// Pattern from RulesEngine.java
if (itemResult.getResultType() == RuleResult.ResultType.ERROR) {
    overallSuccess = false;
    failureMessages.add(item.getSectionType() + " '" + item.getItemId() + "' error: " + itemResult.getMessage());
}
```

**Critical Distinction**:

1. **When a rule condition = TRUE**: ResultType is MATCH, severity is irrelevant
2. **When a rule condition = FALSE**:
   - If severity is ERROR/CRITICAL AND recovery is disabled → ResultType becomes ERROR
   - If severity is WARNING/INFO OR recovery is enabled → ResultType is NO_MATCH

### Example Scenarios

#### Scenario 1: Validation Rule with ERROR Severity (Recovery Disabled)

```yaml
rules:
  - id: "trade-id-required"
    condition: "#'tradeId'] != null"
    severity: "ERROR"
```

**When tradeId exists**:
- Condition evaluates to TRUE
- ResultType = MATCH
- Processing continues ✅

**When tradeId is missing**:
- Condition evaluates to FALSE
- Severity = ERROR (recovery disabled by default)
- ResultType = ERROR
- Processing fails immediately ❌

#### Scenario 2: Enrichment Exception

```java
// From YamlEnrichmentProcessor
try {
    return processEnrichmentWithResult(enrichment, targetObject);
} catch (Exception e) {
    return RuleResult.enrichmentFailure(msgs, data, SeverityConstants.ERROR);
}
```

**Result**:
- ResultType = ENRICHMENT_FAILURE (or ERROR)
- Severity = ERROR
- Processing fails immediately ❌

#### Scenario 3: Rule with WARNING Severity (Recovery Enabled)

```yaml
rules:
  - id: "email-format-check"
    condition: "#'email'] != null && #'email'].contains('@')"
    severity: "WARNING"
```

**When email is invalid**:
- Condition evaluates to FALSE
- Severity = WARNING (recovery enabled by default)
- ResultType = NO_MATCH
- Processing continues ✅

### Error Propagation Pattern

Throughout the APEX codebase, errors propagate using this pattern:

```java
// 1. Process item (rule, enrichment, transformation)
RuleResult itemResult = processItem(item, yamlConfig, enrichedData);

// 2. Check for ERROR result type
if (itemResult.getResultType() == RuleResult.ResultType.ERROR) {
    overallSuccess = false;
    failureMessages.add("Processing failed: " + itemResult.getMessage());
    // Optionally fail-fast or continue collecting errors
}

// 3. Update enriched data if successful
if (itemResult.getEnrichedData() != null) {
    enrichedData.putAll(itemResult.getEnrichedData());
}
```

This pattern is used in:
- `RulesEngine.evaluateYamlConfigurationSequentially()` (lines 1186-1201)
- `RulesEngine.evaluateYamlConfiguration()` (lines 1035-1050)
- `RulesEngine.processEnrichments()` (lines 1220-1230)
- `RulesEngine.processTransformations()` (lines 1370-1380)

### Key Takeaways

**ResultType** = What happened (MATCH, NO_MATCH, ERROR)
**Severity** = How important it is (CRITICAL, ERROR, WARNING, INFO)
**ERROR ResultType** = System failure, always stops processing (unless explicitly handled)
**ERROR Severity** = Important business rule, may or may not stop processing (depends on recovery config)

---

## Severity Levels

APEX supports four severity levels, each with a specific priority and default behavior:

| Severity | Priority | Default Recovery | Default Strategy | Use Case |
|----------|----------|------------------|------------------|----------|
| **CRITICAL** | 4 (Highest) | Disabled | FAIL_FAST | System failures, data corruption, critical business rule violations |
| **ERROR** | 3 | Disabled | FAIL_FAST | Business logic failures, validation errors, missing required data |
| **WARNING** | 2 | Enabled | CONTINUE_WITH_DEFAULT | Potential issues that don't prevent processing, optional validations |
| **INFO** | 1 (Lowest) | Enabled | CONTINUE_WITH_DEFAULT | Informational messages, successful processing, audit trails |

### Severity Constants

Always use the `SeverityConstants` class to reference severity levels:

```java
import dev.mars.apex.core.constants.SeverityConstants;

// Correct usage
String severity = SeverityConstants.ERROR;
String severity = SeverityConstants.WARNING;
String severity = SeverityConstants.CRITICAL;
String severity = SeverityConstants.INFO;

// Avoid hardcoded strings
String severity = "ERROR";  // Don't do this
```

---

## Recovery Strategies

APEX provides four recovery strategies:

### 1. FAIL_FAST

**Behavior**: Immediately fail and return an error result. No recovery attempted.

**Use Case**: Critical errors where continuing would be dangerous or meaningless.

**Example**:
```yaml
severity-policies:
  CRITICAL:
    recovery-enabled: false
    strategy: "FAIL_FAST"
```

### 2. CONTINUE_WITH_DEFAULT

**Behavior**: Log the error and continue processing with a default/safe value.

**Use Case**: Non-critical failures where processing can continue with reasonable defaults.

**Example**:
```yaml
severity-policies:
  WARNING:
    recovery-enabled: true
    strategy: "CONTINUE_WITH_DEFAULT"
    max-retries: 0
```

### 3. RETRY_WITH_SAFE_EXPRESSION

**Behavior**: Attempt to retry the rule evaluation with a simplified/safe expression.

**Use Case**: Expression evaluation failures that might succeed with a simpler approach.

**Example**:
```yaml
severity-policies:
  ERROR:
    recovery-enabled: true
    strategy: "RETRY_WITH_SAFE_EXPRESSION"
    max-retries: 2
    retry-delay: 100
```

### 4. SKIP_RULE

**Behavior**: Skip the failed rule and continue with the next rule.

**Use Case**: Optional rules where failure should not block processing.

**Example**:
```yaml
severity-policies:
  WARNING:
    recovery-enabled: true
    strategy: "SKIP_RULE"
```

---

## Configuration

Error recovery can be configured in two ways:

1. **YAML Configuration** (Recommended): Define policies in your APEX YAML files
2. **Programmatic Configuration**: Configure policies in Java code

---

## YAML Configuration

### Basic Structure

Add an `error-recovery` section to your APEX YAML configuration file:

```yaml
metadata:
  id: "my-rules"
  name: "My Rules Configuration"
  version: "1.0"

error-recovery:
  enabled: true
  log-recovery-attempts: true
  metrics-enabled: true
  default-strategy: "CONTINUE_WITH_DEFAULT"
  
  severity-policies:
    CRITICAL:
      recovery-enabled: false
      strategy: "FAIL_FAST"
    
    ERROR:
      recovery-enabled: false
      strategy: "FAIL_FAST"
    
    WARNING:
      recovery-enabled: true
      strategy: "CONTINUE_WITH_DEFAULT"
      max-retries: 1
      retry-delay: 100
    
    INFO:
      recovery-enabled: true
      strategy: "CONTINUE_WITH_DEFAULT"
      max-retries: 0

rules:
  - name: "my-rule"
    condition: "#amount > 1000"
    message: "High value transaction"
    severity: "ERROR"
```

### Configuration Properties

#### Global Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | Boolean | `true` | Master switch for error recovery system |
| `log-recovery-attempts` | Boolean | `true` | Log all recovery attempts for debugging |
| `metrics-enabled` | Boolean | `true` | Collect metrics on recovery success/failure rates |
| `default-strategy` | String | `"CONTINUE_WITH_DEFAULT"` | Fallback strategy when no severity-specific policy exists |

#### Severity Policy Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `recovery-enabled` | Boolean | Varies by severity | Whether recovery is enabled for this severity |
| `strategy` | String | Varies by severity | Recovery strategy to use (FAIL_FAST, CONTINUE_WITH_DEFAULT, RETRY_WITH_SAFE_EXPRESSION, SKIP_RULE) |
| `max-retries` | Integer | `0` | Maximum number of retry attempts (for RETRY_WITH_SAFE_EXPRESSION) |
| `retry-delay` | Long | `0` | Delay in milliseconds between retry attempts |

### Partial Configuration

You don't need to define policies for all severities. The system uses defaults for any severity not explicitly configured:

```yaml
error-recovery:
  enabled: true

  severity-policies:
    # Only override WARNING behavior
    WARNING:
      recovery-enabled: true
      strategy: "SKIP_RULE"

    # CRITICAL, ERROR, and INFO use default policies
```

---

## Programmatic Configuration

### Creating ErrorRecoveryConfig

```java
import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
import dev.mars.apex.core.config.error.SeverityRecoveryPolicy;
import dev.mars.apex.core.constants.SeverityConstants;

// Create configuration with defaults
ErrorRecoveryConfig config = new ErrorRecoveryConfig();

// Customize global settings
config.setEnabled(true);
config.setLogRecoveryAttempts(true);
config.setMetricsEnabled(true);
config.setDefaultStrategy("CONTINUE_WITH_DEFAULT");

// Configure ERROR severity policy
SeverityRecoveryPolicy errorPolicy = new SeverityRecoveryPolicy();
errorPolicy.setRecoveryEnabled(false);
errorPolicy.setStrategy("FAIL_FAST");
config.setSeverityPolicy(SeverityConstants.ERROR, errorPolicy);

// Configure WARNING severity policy
SeverityRecoveryPolicy warningPolicy = new SeverityRecoveryPolicy();
warningPolicy.setRecoveryEnabled(true);
warningPolicy.setStrategy("CONTINUE_WITH_DEFAULT");
warningPolicy.setMaxRetries(1);
warningPolicy.setRetryDelay(100L);
config.setSeverityPolicy(SeverityConstants.WARNING, warningPolicy);
```

### Using with RulesEngine

```java
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;

// Create rules engine configuration
RulesEngineConfiguration engineConfig = new RulesEngineConfiguration();

// Create custom error recovery config
ErrorRecoveryConfig errorRecoveryConfig = new ErrorRecoveryConfig();
// ... configure as needed ...

// Create rules engine with custom error recovery
RulesEngine engine = new RulesEngine(engineConfig);
// Note: Currently, error recovery config is passed to UnifiedRuleEvaluator
// Future versions may support setting it directly on RulesEngine
```

---

## Default Behavior

### How Defaults Work

When you create an `ErrorRecoveryConfig` instance, the constructor automatically calls `initializeDefaults()`:

```java
public ErrorRecoveryConfig() {
    initializeDefaults();  // Sets up all 4 severity policies
}
```

This ensures every severity level has a valid policy, even if you don't configure anything.

### Default Policies

| Severity | Recovery Enabled | Strategy | Max Retries | Retry Delay |
|----------|------------------|----------|-------------|-------------|
| CRITICAL | `false` | FAIL_FAST | 0 | 0 |
| ERROR | `false` | FAIL_FAST | 0 | 0 |
| WARNING | `true` | CONTINUE_WITH_DEFAULT | 0 | 0 |
| INFO | `true` | CONTINUE_WITH_DEFAULT | 0 | 0 |

### Override Mechanism

When you load a YAML configuration:

1. **Step 1**: `ErrorRecoveryConfig` constructor creates default policies for all 4 severities
2. **Step 2**: YAML parser reads the `error-recovery` section into `YamlErrorRecoveryConfig`
3. **Step 3**: `YamlErrorRecoveryConfig.toErrorRecoveryConfig()` creates a new `ErrorRecoveryConfig` (with defaults)
4. **Step 4**: YAML-defined severity policies **override** the defaults using `Map.put()`
5. **Step 5**: Severities not defined in YAML keep their default policies

**Example**:

```yaml
error-recovery:
  severity-policies:
    WARNING:
      recovery-enabled: true
      strategy: "SKIP_RULE"
```

**Result**:
- CRITICAL: Uses default (FAIL_FAST, no recovery)
- ERROR: Uses default (FAIL_FAST, no recovery)
- WARNING: **Overridden** (SKIP_RULE, recovery enabled)
- INFO: Uses default (CONTINUE_WITH_DEFAULT, recovery enabled)

---

## Usage Examples

### Example 1: Strict Error Handling (Production)

Disable recovery for all errors, only allow warnings to continue:

```yaml
error-recovery:
  enabled: true
  log-recovery-attempts: true
  metrics-enabled: true

  severity-policies:
    CRITICAL:
      recovery-enabled: false
      strategy: "FAIL_FAST"

    ERROR:
      recovery-enabled: false
      strategy: "FAIL_FAST"

    WARNING:
      recovery-enabled: true
      strategy: "CONTINUE_WITH_DEFAULT"

    INFO:
      recovery-enabled: true
      strategy: "CONTINUE_WITH_DEFAULT"
```

### Example 2: Lenient Error Handling (Development/Testing)

Allow recovery for most errors with retries:

```yaml
error-recovery:
  enabled: true
  log-recovery-attempts: true
  metrics-enabled: true

  severity-policies:
    CRITICAL:
      recovery-enabled: false
      strategy: "FAIL_FAST"

    ERROR:
      recovery-enabled: true
      strategy: "RETRY_WITH_SAFE_EXPRESSION"
      max-retries: 3
      retry-delay: 200

    WARNING:
      recovery-enabled: true
      strategy: "SKIP_RULE"

    INFO:
      recovery-enabled: true
      strategy: "CONTINUE_WITH_DEFAULT"
```

### Example 3: Minimal Configuration

Only override what you need:

```yaml
error-recovery:
  enabled: true

  severity-policies:
    ERROR:
      recovery-enabled: true
      strategy: "CONTINUE_WITH_DEFAULT"
      max-retries: 1
```

All other severities use their default policies.

### Example 4: Rule-Specific Severity

Define severity at the rule level and let error recovery handle it:

```yaml
error-recovery:
  enabled: true

  severity-policies:
    ERROR:
      recovery-enabled: false
      strategy: "FAIL_FAST"

    WARNING:
      recovery-enabled: true
      strategy: "CONTINUE_WITH_DEFAULT"

rules:
  # Validation rule: Condition evaluates to TRUE when accountId exists
  - name: "critical-validation"
    condition: "#accountId != null"  # TRUE when accountId exists
    message: "Account ID is required"
    severity: "ERROR"
    # When accountId exists: condition TRUE → rule matches → success
    # When accountId missing: condition FALSE → rule doesn't match + ERROR severity → FAIL_FAST

  # Optional validation: Condition evaluates to TRUE when email format is correct
  - name: "optional-check"
    condition: "#email != null && #email.contains('@')"  # TRUE when email has @
    message: "Email format validation"
    severity: "WARNING"
    # When email has @: condition TRUE → rule matches → success
    # When email missing @: condition FALSE → rule doesn't match + WARNING severity → CONTINUE_WITH_DEFAULT
```

**Key Point**: Write rule conditions to return TRUE for the case you want to detect/match. When the condition returns FALSE (rule doesn't match), the severity determines whether processing continues or fails.

---

## Enrichment and Transformation Error Handling

### Overview

While the error recovery system primarily focuses on **rule evaluation** errors, APEX also provides robust error handling for **enrichments** and **transformations**. These operations have different error handling characteristics because they modify data rather than just evaluating conditions.

### Enrichment Error Handling

#### Fail-Fast Behavior

Enrichments use **fail-fast** error handling by default. When an enrichment fails, processing stops immediately:

```java
// From YamlEnrichmentProcessor.java (lines 1752-1760)
tasks.add(() -> {
    try {
        return processEnrichmentWithResult(enrichment, targetObject);
    } catch (Exception e) {
        List<String> msgs = new ArrayList<>();
        msgs.add("Parallel enrichment exception: " + e.getMessage());
        Map<String, Object> data = convertToMap(targetObject);
        return RuleResult.enrichmentFailure(msgs, data, SeverityConstants.ERROR);
    }
});
```

#### Error Result Types

Enrichment failures return `RuleResult` with:
- **ResultType**: `ENRICHMENT_FAILURE` or `ERROR`
- **Severity**: `ERROR` (by default)
- **Failure Messages**: List of error descriptions
- **Enriched Data**: Partial data if available

#### Example: Enrichment Failure Detection

```java
// From RulesEngine.java (lines 1220-1230)
RuleResult enrichmentResult = enrichmentProcessor.processEnrichmentsWithResult(
    yamlConfig.getEnrichments(), enrichedData);

// Check for enrichment errors
if (enrichmentResult.getResultType() == RuleResult.ResultType.ERROR) {
    overallSuccess = false;
    failureMessages.add("Enrichment processing failed: " + enrichmentResult.getMessage());
    logger.error("CRITICAL: Enrichment processing failed: {}", enrichmentResult.getMessage());
    // Return error immediately (fail-fast)
    return RuleResult.error("enrichments", enrichmentResult.getMessage(), SeverityConstants.ERROR);
}
```

#### Required Field Validation

Enrichments can specify required fields that must be present in the data:

```yaml
enrichments:
  - id: "calculate-premium"
    type: "calculation"
    condition: "#'notional'] != null"
    expression: "#'notional'] * 0.05"
    result-field: "premium"
    required-fields:
      - "notional"
      - "currency"
```

**Behavior**:
- If required fields are missing → enrichment fails
- ResultType = ERROR
- Processing stops immediately

### Transformation Error Handling

#### Fail-Fast Behavior

Transformations also use **fail-fast** error handling:

```java
// From YamlTransformationProcessor.java (lines 143-152)
try {
    if (shouldProcessTransformation(transformation, transformedObject)) {
        transformedObject = processTransformation(transformation, transformedObject);
        processedCount++;
        logger.debug("Successfully processed transformation: {}", transformation.getId());
    }
} catch (Exception e) {
    logger.error("CRITICAL: Transformation failed: {} - {}", transformation.getId(), e.getMessage(), e);

    // Return error result immediately (fail-fast behavior)
    return RuleResult.error(
        "transformation:" + transformation.getId(),
        "Transformation processing failed: " + e.getMessage(),
        SeverityConstants.ERROR
    );
}
```

#### Error Detection Pattern

```java
// From RulesEngine.java (lines 1370-1380)
RuleResult transformationResult = transformationProcessor.processTransformationsWithResult(
    yamlConfig.getTransformations(), enrichedData);

// Check for transformation errors
if (transformationResult.getResultType() == RuleResult.ResultType.ERROR) {
    overallSuccess = false;
    failureMessages.add("Transformation processing failed: " + transformationResult.getMessage());
    logger.error("CRITICAL: Transformation processing failed: {}", transformationResult.getMessage());
    // Return error immediately (fail-fast)
    return RuleResult.error("transformations", transformationResult.getMessage(), SeverityConstants.ERROR);
}
```

### Parallel Execution Error Handling

When enrichments are executed in parallel (using `parallel-execution: true`), errors are caught and converted to `RuleResult`:

```java
// From RulesEngine.java (lines 717-726)
List<Callable<RuleResult>> tasks = new ArrayList<>();
for (YamlEnrichment enrichment : enrichments) {
    tasks.add(() -> {
        try {
            return enrichmentProcessor.processEnrichmentWithResult(enrichment, targetObject);
        } catch (Exception e) {
            List<String> msgs = new ArrayList<>();
            msgs.add("Parallel enrichment exception: " + e.getMessage());
            Map<String, Object> data = convertToMap(targetObject);
            return RuleResult.enrichmentFailure(msgs, data, SeverityConstants.ERROR);
        }
    });
}

ExecutorService executor = Executors.newFixedThreadPool(
    Math.min(tasks.size(), Runtime.getRuntime().availableProcessors())
);
```

**Key Points**:
- Each enrichment runs in its own thread
- Exceptions are caught and converted to `RuleResult.enrichmentFailure()`
- All results are collected before checking for errors
- If any enrichment fails, the overall result is an error

### YAML Configuration Examples

#### Example 1: Enrichment with Error Handling

```yaml
metadata:
  id: "trade-enrichment"
  name: "Trade Enrichment with Error Handling"
  version: "1.0"

enrichments:
  - id: "calculate-notional"
    type: "calculation"
    condition: "#'quantity'] != null && #'price'] != null"
    expression: "#'quantity'] * #'price']"
    result-field: "notional"
    required-fields:
      - "quantity"
      - "price"
    # If quantity or price is missing, enrichment fails with ERROR
```

#### Example 2: Transformation with Error Handling

```yaml
metadata:
  id: "data-transformation"
  name: "Data Transformation with Error Handling"
  version: "1.0"

transformations:
  - id: "normalize-currency"
    source-field: "currency"
    target-field: "currencyCode"
    expression: "#source.toUpperCase()"
    # If expression fails (e.g., currency is null), transformation fails with ERROR
```

### Best Practices for Enrichment/Transformation Error Handling

1. **Use Conditions to Prevent Errors**:
   ```yaml
   enrichments:
     - id: "safe-calculation"
       condition: "#'amount'] != null && #'amount'] > 0"
       expression: "#'amount'] * 1.1"
       result-field: "adjustedAmount"
   ```

2. **Specify Required Fields**:
   ```yaml
   enrichments:
     - id: "premium-calculation"
       required-fields:
         - "notional"
         - "rate"
       expression: "#'notional'] * #'rate']"
       result-field: "premium"
   ```

3. **Use Null-Safe Expressions**:
   ```yaml
   enrichments:
     - id: "safe-string-operation"
       expression: "#'name'] != null ? #'name'].toUpperCase() : 'UNKNOWN'"
       result-field: "upperName"
   ```

4. **Monitor Enrichment Failures**:
   ```java
   RuleResult result = rulesEngine.evaluate(data);
   if (result.hasFailures()) {
       for (String failure : result.getFailureMessages()) {
           logger.error("Enrichment/Transformation failure: {}", failure);
       }
   }
   ```

### Key Differences from Rule Error Handling

| Aspect | Rule Evaluation | Enrichment/Transformation |
|--------|----------------|---------------------------|
| **Default Behavior** | Configurable via error recovery | Always fail-fast |
| **Recovery** | Supports recovery strategies | No recovery (always fails) |
| **Severity** | Configurable per rule | Always ERROR |
| **ResultType** | MATCH, NO_MATCH, or ERROR | ERROR or ENRICHMENT_FAILURE |
| **Impact** | May continue processing | Always stops processing |

---

## Best Practices

### 1. Use Appropriate Severity Levels

Choose severity based on business impact:

- **CRITICAL**: System failures, data corruption, security violations
- **ERROR**: Business rule violations, missing required data, validation failures
- **WARNING**: Optional validations, potential issues, data quality concerns
- **INFO**: Successful processing, audit trails, informational messages

### 2. Configure Recovery Based on Environment

**Production**:
```yaml
error-recovery:
  enabled: true
  severity-policies:
    CRITICAL:
      recovery-enabled: false
      strategy: "FAIL_FAST"
    ERROR:
      recovery-enabled: false
      strategy: "FAIL_FAST"
    WARNING:
      recovery-enabled: true
      strategy: "CONTINUE_WITH_DEFAULT"
```

**Development/Testing**:
```yaml
error-recovery:
  enabled: true
  severity-policies:
    ERROR:
      recovery-enabled: true
      strategy: "RETRY_WITH_SAFE_EXPRESSION"
      max-retries: 2
    WARNING:
      recovery-enabled: true
      strategy: "SKIP_RULE"
```

### 3. Enable Logging and Metrics

Always enable logging and metrics in production for troubleshooting:

```yaml
error-recovery:
  enabled: true
  log-recovery-attempts: true  # Enable for debugging
  metrics-enabled: true        # Enable for monitoring
```

### 4. Use SeverityConstants in Code

Never use hardcoded severity strings:

```java
// Correct
import dev.mars.apex.core.constants.SeverityConstants;
String severity = SeverityConstants.ERROR;

// Incorrect
String severity = "ERROR";
```

### 5. Test Error Scenarios

Always test your error recovery configuration:

```java
@Test
void testErrorRecoveryForWarnings() {
    // Create config with WARNING recovery enabled
    ErrorRecoveryConfig config = new ErrorRecoveryConfig();
    SeverityRecoveryPolicy warningPolicy = new SeverityRecoveryPolicy();
    warningPolicy.setRecoveryEnabled(true);
    warningPolicy.setStrategy("CONTINUE_WITH_DEFAULT");
    config.setSeverityPolicy(SeverityConstants.WARNING, warningPolicy);

    // Test that WARNING failures are recovered
    assertTrue(config.isRecoveryEnabledForSeverity(SeverityConstants.WARNING));
}
```

### 6. Write Validation Rules with Positive Conditions

**CRITICAL**: Always write validation rule conditions that return TRUE when data is **valid**, not when it's invalid.

```yaml
# CORRECT: Returns TRUE for valid data
rules:
  - id: "amount-validation"
    condition: "#amount != null && #amount > 0"
    message: "Amount must be positive"
    severity: "ERROR"
    # Valid data (amount > 0): condition TRUE → success
    # Invalid data (amount ≤ 0): condition FALSE + ERROR → fail-fast

# INCORRECT: Returns TRUE for invalid data
rules:
  - id: "amount-validation-wrong"
    condition: "#amount == null || #amount <= 0"
    message: "Amount must be positive"
    severity: "ERROR"
    # Valid data (amount > 0): condition FALSE → unexpected behavior
    # Invalid data (amount ≤ 0): condition TRUE → success (wrong!)
```

**Why this matters:**
- Severity-based fail-fast **only applies when condition = FALSE**
- If you write conditions that return TRUE for invalid data, the rule will report success instead of failing
- This is a common mistake when migrating from other rules engines

### 7. Document Your Recovery Strategy

Add comments to your YAML configuration explaining why you chose specific strategies:

```yaml
error-recovery:
  enabled: true

  severity-policies:
    ERROR:
      # Production requirement: All business rule failures must fail fast
      # to prevent invalid data from being processed
      recovery-enabled: false
      strategy: "FAIL_FAST"

    WARNING:
      # Allow processing to continue for optional validations
      # Default values will be used when validation fails
      recovery-enabled: true
      strategy: "CONTINUE_WITH_DEFAULT"
```

---

## Monitoring and Metrics

### Metrics Collection

When `metrics-enabled: true`, the system collects:

- **Recovery Attempts**: Total number of recovery attempts per severity
- **Recovery Success Rate**: Percentage of successful recoveries
- **Recovery Failure Rate**: Percentage of failed recoveries
- **Recovery Time**: Duration of recovery attempts
- **Strategy Usage**: Which strategies are used most frequently

### Accessing Metrics

Metrics are included in `RuleResult` objects via the `RulePerformanceMetrics` class:

```java
RuleResult result = rulesEngine.evaluate(data);

if (result.getMetrics() != null) {
    RulePerformanceMetrics metrics = result.getMetrics();

    // Check if recovery was attempted
    if (metrics.isRecoveryAttempted()) {
        boolean successful = metrics.isRecoverySuccessful();
        String strategy = metrics.getRecoveryStrategy();
        String reason = metrics.getRecoveryReason();
        Duration recoveryTime = metrics.getRecoveryTime();

        System.out.println("Recovery attempted: " + successful);
        System.out.println("Strategy used: " + strategy);
        System.out.println("Recovery time: " + recoveryTime.toMillis() + "ms");
    }
}
```

### RulePerformanceMetrics Details

The `RulePerformanceMetrics` class provides comprehensive performance and recovery tracking:

#### Core Performance Metrics

```java
RulePerformanceMetrics metrics = result.getMetrics();

// Execution timing
Duration executionTime = metrics.getExecutionTime();
Instant startTime = metrics.getStartTime();
Instant endTime = metrics.getEndTime();

// Rule information
String ruleName = metrics.getRuleName();
String severity = metrics.getSeverity();

System.out.println("Rule '" + ruleName + "' executed in " + executionTime.toMillis() + "ms");
```

#### Recovery Metrics

```java
// Recovery tracking
boolean recoveryAttempted = metrics.isRecoveryAttempted();
boolean recoverySuccessful = metrics.isRecoverySuccessful();
String recoveryStrategy = metrics.getRecoveryStrategy();
String recoveryReason = metrics.getRecoveryReason();
Duration recoveryTime = metrics.getRecoveryTime();

if (recoveryAttempted) {
    System.out.println("Recovery Strategy: " + recoveryStrategy);
    System.out.println("Recovery Successful: " + recoverySuccessful);
    System.out.println("Recovery Time: " + recoveryTime.toMillis() + "ms");
    System.out.println("Recovery Reason: " + recoveryReason);
}
```

#### Metrics Builder Pattern

Metrics are built using the builder pattern in `UnifiedRuleEvaluator`:

```java
// From UnifiedRuleEvaluator.java (lines 367-372)
RulePerformanceMetrics metrics = buildMetricsWithRecovery(
    metricsBuilder,
    rule,
    exception,
    recoveryAttempted,
    recoverySuccessful,
    recoveryStrategy,
    recoveryReason,
    recoveryTime
);
```

### Aggregating Metrics Across Rules

For batch processing or scenario execution, you can aggregate metrics:

```java
List<RuleResult> results = new ArrayList<>();
// ... execute multiple rules ...

// Aggregate metrics
long totalExecutionTime = 0;
int recoveryAttempts = 0;
int successfulRecoveries = 0;

for (RuleResult result : results) {
    if (result.getMetrics() != null) {
        RulePerformanceMetrics metrics = result.getMetrics();
        totalExecutionTime += metrics.getExecutionTime().toMillis();

        if (metrics.isRecoveryAttempted()) {
            recoveryAttempts++;
            if (metrics.isRecoverySuccessful()) {
                successfulRecoveries++;
            }
        }
    }
}

double recoverySuccessRate = recoveryAttempts > 0
    ? (double) successfulRecoveries / recoveryAttempts * 100
    : 0;

System.out.println("Total execution time: " + totalExecutionTime + "ms");
System.out.println("Recovery attempts: " + recoveryAttempts);
System.out.println("Recovery success rate: " + String.format("%.2f%%", recoverySuccessRate));
```

### Log Output

When `log-recovery-attempts: true`, you'll see log entries like:

```
INFO  - Attempting error recovery for rule 'validate-email' with severity 'WARNING' using strategy 'CONTINUE_WITH_DEFAULT'
INFO  - Recovered from error for rule 'validate-email': Continued with default value
```

```
ERROR - Rule evaluation failed for 'validate-account': Account ID is required
DEBUG - Full exception details for rule 'validate-account': ...
```

---

## Advanced Topics

### Custom Recovery Strategies

While APEX provides four built-in strategies, you can extend the system by:

1. Implementing custom `ErrorRecoveryService` logic
2. Using the `default-strategy` property to define fallback behavior
3. Combining strategies based on context

### Integration with External Systems

Error recovery can be integrated with:

- **Monitoring Systems**: Send metrics to Prometheus, Grafana, etc.
- **Alerting Systems**: Trigger alerts on high failure rates
- **Logging Aggregators**: Send recovery logs to ELK, Splunk, etc.

### Parallel Execution Error Handling

APEX supports parallel execution of enrichments and rule groups for improved performance. Error handling in parallel contexts requires special consideration.

#### Parallel Enrichment Execution

When `parallel-execution: true` is set on an enrichment group, enrichments execute concurrently:

```yaml
enrichment-groups:
  - id: "parallel-enrichments"
    name: "Parallel Enrichment Group"
    parallel-execution: true
    enrichments:
      - id: "enrich-1"
        # ... enrichment config ...
      - id: "enrich-2"
        # ... enrichment config ...
      - id: "enrich-3"
        # ... enrichment config ...
```

#### Error Handling Pattern

Each parallel task is wrapped in a try-catch block that converts exceptions to `RuleResult`:

```java
// From RulesEngine.java (lines 717-726)
List<Callable<RuleResult>> tasks = new ArrayList<>();
for (YamlEnrichment enrichment : enrichments) {
    tasks.add(() -> {
        try {
            return enrichmentProcessor.processEnrichmentWithResult(enrichment, targetObject);
        } catch (Exception e) {
            List<String> msgs = new ArrayList<>();
            msgs.add("Parallel enrichment exception: " + e.getMessage());
            Map<String, Object> data = convertToMap(targetObject);
            return RuleResult.enrichmentFailure(msgs, data, SeverityConstants.ERROR);
        }
    });
}

ExecutorService executor = Executors.newFixedThreadPool(
    Math.min(tasks.size(), Runtime.getRuntime().availableProcessors())
);
```

#### Key Characteristics

1. **Thread Pool Sizing**: Uses `min(task_count, available_processors)` threads
2. **Exception Isolation**: Each task's exceptions are caught independently
3. **Result Collection**: All tasks complete before results are checked
4. **Fail-Fast**: If any task returns ERROR, overall result is ERROR

#### Example: Parallel Execution with Error Handling

```java
// From YamlEnrichmentProcessor.java (lines 1752-1760)
if (group.isParallelExecution() && ordered.size() > 1) {
    // Parallel branch: disable short-circuit and execute all enrichments
    shortCircuit = false;

    List<Callable<RuleResult>> tasks = new ArrayList<>();
    for (YamlEnrichment enrichment : ordered) {
        tasks.add(() -> {
            try {
                return processEnrichmentWithResult(enrichment, targetObject);
            } catch (Exception e) {
                List<String> msgs = new ArrayList<>();
                msgs.add("Parallel enrichment exception: " + e.getMessage());
                Map<String, Object> data = convertToMap(targetObject);
                return RuleResult.enrichmentFailure(msgs, data, SeverityConstants.ERROR);
            }
        });
    }

    ExecutorService executor = Executors.newFixedThreadPool(
        Math.min(tasks.size(), Runtime.getRuntime().availableProcessors())
    );

    try {
        List<Future<RuleResult>> futures = executor.invokeAll(tasks);
        // Check results for errors...
    } finally {
        executor.shutdown();
    }
}
```

#### Best Practices for Parallel Execution

1. **Use for Independent Operations**: Only parallelize enrichments that don't depend on each other
2. **Monitor Thread Pool**: Be aware of thread pool sizing for large enrichment groups
3. **Handle Partial Failures**: Design enrichments to be idempotent in case of retries
4. **Test Error Scenarios**: Verify behavior when some parallel tasks fail

### Performance Considerations

- **Recovery Overhead**: Recovery attempts add latency (typically 1-10ms)
- **Retry Delays**: Configure `retry-delay` based on your performance requirements
- **Max Retries**: Limit `max-retries` to prevent excessive delays (recommended: 0-3)
- **Parallel Execution**: Thread pool overhead vs. performance gain (test with your workload)
- **Metrics Collection**: Minimal overhead (~1-2ms per rule evaluation)

---

## Exception Hierarchy

APEX provides a comprehensive exception hierarchy for different types of errors. Understanding these exceptions helps with error handling and debugging.

### Core Exception Classes

#### RuleEngineException (Base Class)

**Package**: `dev.mars.apex.core.exception`

Base exception for all rules engine related errors:

```java
public class RuleEngineException extends Exception {
    private final String errorCode;
    private final String context;

    public RuleEngineException(String errorCode, String message, String context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context;
    }

    public String getErrorCode() { return errorCode; }
    public String getContext() { return context; }
}
```

**Usage**:
```java
try {
    // Rule engine operations
} catch (RuleEngineException e) {
    logger.error("Rule engine error [{}]: {} (Context: {})",
        e.getErrorCode(), e.getMessage(), e.getContext());
}
```

#### RuleEvaluationException

**Package**: `dev.mars.apex.core.exception`

Thrown when a rule evaluation fails:

```java
public class RuleEvaluationException extends RuleEngineException {
    private final String ruleName;
    private final String expression;
    private final String suggestion;

    public RuleEvaluationException(String ruleName, String expression, String message) {
        super("RULE_EVALUATION_ERROR", message, "Rule: " + ruleName + ", Expression: " + expression);
        this.ruleName = ruleName;
        this.expression = expression;
        this.suggestion = generateSuggestion(message);
    }
}
```

**Example**:
```java
throw new RuleEvaluationException(
    "validate-amount",
    "#amount > 1000",
    "Property 'amount' not found in data context"
);
```

#### RuleValidationException

**Package**: `dev.mars.apex.core.exception`

Thrown when rule validation fails (configuration errors):

```java
public class RuleValidationException extends RuleEngineException {
    private final String ruleName;
    private final List<ValidationError> validationErrors;

    public void addValidationError(ValidationError error) {
        this.validationErrors.add(error);
    }

    public static class ValidationError {
        private final String field;
        private final String message;
        private final String suggestion;
    }
}
```

**Example**:
```java
RuleValidationException exception = new RuleValidationException("my-rule", "Invalid configuration");
exception.addValidationError(new ValidationError("condition", "Condition cannot be null", "Add a valid SpEL expression"));
throw exception;
```

#### RuleConfigurationException

**Package**: `dev.mars.apex.core.exception`

Thrown when there are issues with rule configuration:

```java
public class RuleConfigurationException extends RuleEngineException {
    private final String configurationElement;
    private final String expectedFormat;

    public RuleConfigurationException(String configurationElement, String message, String expectedFormat) {
        super("RULE_CONFIGURATION_ERROR", message, "Configuration element: " + configurationElement);
        this.configurationElement = configurationElement;
        this.expectedFormat = expectedFormat;
    }
}
```

**Example**:
```java
throw new RuleConfigurationException(
    "error-recovery.severity-policies.ERROR",
    "Invalid strategy value",
    "Expected: FAIL_FAST, CONTINUE_WITH_DEFAULT, RETRY_WITH_SAFE_EXPRESSION, or SKIP_RULE"
);
```

### Data Source Exceptions

#### DataSourceException

**Package**: `dev.mars.apex.core.service.data.external`

Thrown when external data source operations fail:

```java
public class DataSourceException extends RuntimeException {
    public enum ErrorType {
        CONNECTION_ERROR,
        CONFIGURATION_ERROR,
        EXECUTION_ERROR,
        DATA_FORMAT_ERROR,
        TIMEOUT_ERROR,
        AUTHENTICATION_ERROR,
        NOT_FOUND_ERROR,
        CIRCUIT_BREAKER_ERROR,
        GENERAL_ERROR
    }

    private final ErrorType errorType;
    private final String dataSourceId;
    private final String operation;
    private final boolean retryable;
}
```

**Static Factory Methods**:
```java
// Connection error
DataSourceException.connectionError("Failed to connect to database", cause);

// Configuration error
DataSourceException.configurationError("Missing required parameter: url");

// Execution error
DataSourceException.executionError("Query execution failed", cause, "SELECT * FROM trades");

// Timeout error
DataSourceException.timeoutError("Query timed out after 30s", "SELECT * FROM large_table");
```

#### DataSinkException

**Package**: `dev.mars.apex.core.service.data.external`

Thrown when data sink (write) operations fail:

```java
public class DataSinkException extends RuntimeException {
    public enum ErrorType {
        CONNECTION_ERROR,
        WRITE_ERROR,
        DATA_INTEGRITY_ERROR,
        BATCH_ERROR,
        SCHEMA_ERROR,
        SECURITY_ERROR,
        RESOURCE_ERROR,
        TIMEOUT_ERROR,
        DATA_ERROR,
        UNKNOWN_ERROR
    }

    private final ErrorType errorType;
    private final String sinkId;
    private final String context;
    private final boolean retryable;
}
```

**Static Factory Methods**:
```java
// Write error
DataSinkException.writeError("Failed to insert record", cause);

// Data integrity error
DataSinkException.dataIntegrityError("Primary key violation", cause);

// Batch error
DataSinkException.batchError("Batch insert failed", 50, 100); // 50 of 100 processed
```

### Enrichment Exceptions

#### EnrichmentException

**Package**: `dev.mars.apex.core.service.enrichment`

Thrown when enrichment processing fails:

```java
public class EnrichmentException extends RuntimeException {
    public EnrichmentException(String message) {
        super(message);
    }

    public EnrichmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Example**:
```java
throw new EnrichmentException(
    "Failed to calculate premium: notional field is null",
    originalException
);
```

### Database-Specific Exceptions

#### SqlErrorClassifier

**Package**: `dev.mars.apex.core.service.data.external.database`

Utility for classifying SQL errors:

```java
public class SqlErrorClassifier {
    public enum SqlErrorType {
        CONNECTION_ERROR,
        TIMEOUT_ERROR,
        DATA_INTEGRITY_VIOLATION,
        SYNTAX_ERROR,
        PERMISSION_ERROR,
        RESOURCE_ERROR,
        FATAL_ERROR
    }

    public static SqlErrorType classifyError(SQLException e) {
        String sqlState = e.getSQLState();
        int errorCode = e.getErrorCode();

        // Classify based on SQL state and error code
        if (isConstraintViolation(sqlState, errorCode, message)) {
            return SqlErrorType.DATA_INTEGRITY_VIOLATION;
        }
        // ... more classification logic
    }
}
```

**Usage**:
```java
try {
    // Database operation
} catch (SQLException e) {
    SqlErrorType errorType = SqlErrorClassifier.classifyError(e);

    switch (errorType) {
        case DATA_INTEGRITY_VIOLATION:
            // Handle gracefully (e.g., duplicate key)
            logger.warn("Data integrity violation: {}", e.getMessage());
            break;
        case CONNECTION_ERROR:
        case TIMEOUT_ERROR:
            // Retry
            logger.error("Retryable error: {}", e.getMessage());
            throw DataSourceException.connectionError("Database connection failed", e);
        default:
            // Fail fast
            throw DataSourceException.executionError("Database operation failed", e, operation);
    }
}
```

### Exception Handling Best Practices

1. **Use Specific Exceptions**: Throw the most specific exception type for the error
   ```java
   // Good
   throw new RuleEvaluationException(ruleName, expression, "Property not found");

   // Bad
   throw new RuntimeException("Error in rule");
   ```

2. **Include Context**: Always provide error codes and context
   ```java
   throw new RuleEngineException(
       "RULE_EVAL_001",
       "Failed to evaluate rule condition",
       "Rule: " + ruleName + ", Data: " + dataContext
   );
   ```

3. **Chain Exceptions**: Preserve the original exception
   ```java
   try {
       // Operation
   } catch (SQLException e) {
       throw new DataSourceException(ErrorType.EXECUTION_ERROR, "Query failed", e, dataSourceId, query, true);
   }
   ```

4. **Use Factory Methods**: Leverage static factory methods for common cases
   ```java
   // Good
   throw DataSourceException.connectionError("Connection failed", cause);

   // Verbose
   throw new DataSourceException(ErrorType.CONNECTION_ERROR, "Connection failed", cause, null, null, true);
   ```

5. **Check Retryability**: Use the `retryable` flag to determine if operations should be retried
   ```java
   try {
       // Data source operation
   } catch (DataSourceException e) {
       if (e.isRetryable()) {
           // Retry logic
       } else {
           // Fail fast
       }
   }
   ```

---

## Troubleshooting

### Validation Rules Not Behaving As Expected

**Problem**: Validation rules with ERROR severity are not producing the expected results.

**Cause**: Misunderstanding of how rule conditions work with severity.

**Solution**: Understand that the condition is just a boolean expression. Write it to return TRUE for the case you want to detect:

```yaml
# Example 1: Detect when field is PRESENT (rule matches when tradeId exists)
- condition: "#'tradeId'] != null && !#'tradeId'].toString().trim().isEmpty()"
  severity: "ERROR"
  # tradeId present → condition TRUE → rule matches → success ✓
  # tradeId missing → condition FALSE → rule doesn't match + ERROR severity → fail-fast ✓

# Example 2: Detect when field is MISSING (rule matches when tradeId is null)
- condition: "#'tradeId'] == null"
  severity: "ERROR"
  # tradeId missing → condition TRUE → rule matches → success ✓
  # tradeId present → condition FALSE → rule doesn't match + ERROR severity → fail-fast ✓
```

**Key Concept**: Severity-based fail-fast **only applies when condition evaluates to FALSE**. Write your condition to return TRUE for whatever case you want to detect. The severity only matters when the condition returns FALSE.

### Recovery Not Working

**Problem**: Recovery is configured but errors still fail fast.

**Solutions**:
1. Check that `enabled: true` at the global level
2. Verify `recovery-enabled: true` for the specific severity
3. Confirm the rule's severity matches your policy configuration
4. Check logs for recovery attempt messages

### Too Many Recovery Attempts

**Problem**: System is attempting recovery too frequently.

**Solutions**:
1. Review your severity assignments (are too many rules using WARNING?)
2. Reduce `max-retries` to limit retry attempts
3. Consider using FAIL_FAST for more critical validations
4. Analyze metrics to identify problematic rules

### Inconsistent Behavior

**Problem**: Same error sometimes recovers, sometimes doesn't.

**Solutions**:
1. Check if multiple YAML files define different error-recovery sections
2. Verify that programmatic configuration isn't overriding YAML config
3. Ensure severity values are consistent (use SeverityConstants)
4. Review logs to see which policy is being applied

---

## API Reference

### ErrorRecoveryConfig

**Package**: `dev.mars.apex.core.config.error`

**Key Methods**:
- `isEnabled()`: Check if error recovery is globally enabled
- `isRecoveryEnabledForSeverity(String severity)`: Check if recovery is enabled for a specific severity
- `getSeverityPolicy(String severity)`: Get the recovery policy for a severity
- `setSeverityPolicy(String severity, SeverityRecoveryPolicy policy)`: Set a severity-specific policy

### SeverityRecoveryPolicy

**Package**: `dev.mars.apex.core.config.error`

**Key Methods**:
- `isRecoveryEnabled()`: Check if recovery is enabled
- `getStrategy()`: Get the recovery strategy
- `getMaxRetries()`: Get maximum retry attempts
- `getRetryDelay()`: Get delay between retries in milliseconds

### SeverityConstants

**Package**: `dev.mars.apex.core.constants`

**Constants**:
- `SeverityConstants.CRITICAL`: "CRITICAL" (Priority 4)
- `SeverityConstants.ERROR`: "ERROR" (Priority 3)
- `SeverityConstants.WARNING`: "WARNING" (Priority 2)
- `SeverityConstants.INFO`: "INFO" (Priority 1)

---

## Summary

The APEX error handling system provides:

**Flexible Configuration**: Define policies in YAML or Java code  
**Severity-Based Control**: Different behaviors for different error types  
**Multiple Strategies**: Choose how to handle failures (fail fast, continue, retry, skip)  
**Backward Compatibility**: Sensible defaults ensure existing code works  
**Monitoring & Metrics**: Track recovery attempts and success rates

### Critical Concepts to Remember

1. **Severity applies when condition = FALSE**: Error recovery and fail-fast behavior only activate when a rule's condition evaluates to FALSE (rule did not match). When condition = TRUE (rule matched), the evaluation always succeeds regardless of severity.

2. **Conditions are boolean expressions**: Rule conditions simply return TRUE or FALSE. Write the condition to return TRUE for whatever case you want to detect/match. The severity only matters when the condition returns FALSE.

3. **Default policies for ERROR/CRITICAL**: By default, ERROR and CRITICAL severities have recovery disabled and use FAIL_FAST strategy, meaning when a rule with ERROR/CRITICAL severity doesn't match (condition = FALSE), processing will stop immediately.

4. **Test your validation rules**: Always verify that validation rules behave correctly by testing both cases where the condition returns TRUE and where it returns FALSE.

### Quick Reference

```
Rule Condition TRUE  → Rule matched  → Always success (severity irrelevant)
Rule Condition FALSE → Rule failed   → Check severity:
                                        - ERROR/CRITICAL (recovery disabled) → FAIL_FAST
                                        - WARNING/INFO (recovery enabled)    → CONTINUE
```

For more information, see:
- [APEX Rules Engine User Guide](APEX_RULES_ENGINE_USER_GUIDE.md)
- [APEX YAML Reference](APEX_YAML_REFERENCE.md)
- [APEX Technical Reference](APEX_TECHNICAL_REFERENCE.md)

---

**Last Updated**: November 17, 2025
**Version**: 2.2 (Enhanced with ResultType vs Severity, Enrichment/Transformation error handling, Parallel execution, Exception hierarchy, and expanded metrics documentation)
**Production-Ready**: Battle-tested with comprehensive logging and debugging support


