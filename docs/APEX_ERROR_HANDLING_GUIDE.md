# APEX Error Handling Guide

## Overview

The APEX Rules Engine provides a comprehensive, configurable error handling and recovery system that allows you to control how the system responds to failures during rule evaluation. This guide explains how to configure and use error handling in your APEX applications.

---

## Table of Contents

1. [Core Concepts](#core-concepts)
2. [Severity Levels](#severity-levels)
3. [Recovery Strategies](#recovery-strategies)
4. [Configuration](#configuration)
5. [YAML Configuration](#yaml-configuration)
6. [Programmatic Configuration](#programmatic-configuration)
7. [Default Behavior](#default-behavior)
8. [Usage Examples](#usage-examples)
9. [Best Practices](#best-practices)
10. [Monitoring and Metrics](#monitoring-and-metrics)
11. [Troubleshooting](#troubleshooting)
12. [API Reference](#api-reference)
13. [Summary](#summary)

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

#### Validation Rule Pattern

For validation rules, write conditions that return TRUE when data is **valid**:

```yaml
rules:
  # ✅ CORRECT: Condition returns TRUE when field exists (valid)
  - id: "trade-id-required"
    condition: "#data['tradeId'] != null && !#data['tradeId'].toString().trim().isEmpty()"
    message: "Trade ID is required"
    severity: "ERROR"

  # ❌ INCORRECT: Condition returns TRUE when field is missing (invalid)
  - id: "trade-id-required-wrong"
    condition: "#data['tradeId'] == null"
    message: "Trade ID is required"
    severity: "ERROR"
```

**Why this matters:**
- When `tradeId` **exists** → condition TRUE → rule matches → **success** ✅
- When `tradeId` **missing** → condition FALSE → severity ERROR + recovery disabled → **fail-fast** ❌

This pattern ensures that:
1. Valid data passes validation (condition TRUE → success)
2. Invalid data triggers fail-fast (condition FALSE + ERROR severity → failure)

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
String severity = "ERROR";  // ❌ Don't do this
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
    condition: "#data.amount > 1000"
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
  # Validation rule: Condition TRUE when data is valid
  - name: "critical-validation"
    condition: "#data.accountId != null"  # TRUE when accountId exists
    message: "Account ID is required"
    severity: "ERROR"
    # When accountId exists: condition TRUE → success
    # When accountId missing: condition FALSE + ERROR severity → FAIL_FAST

  # Optional validation: Condition TRUE when email format is valid
  - name: "optional-check"
    condition: "#data.email != null && #data.email.contains('@')"  # TRUE when valid
    message: "Email format validation"
    severity: "WARNING"
    # When email valid: condition TRUE → success
    # When email invalid: condition FALSE + WARNING severity → CONTINUE_WITH_DEFAULT
```

**Key Point**: Write validation rule conditions that return TRUE for valid data. When the condition returns FALSE (invalid data), the severity determines whether processing continues or fails.

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
  log-recovery-attempts: true  # ✅ Enable for debugging
  metrics-enabled: true        # ✅ Enable for monitoring
```

### 4. Use SeverityConstants in Code

Never use hardcoded severity strings:

```java
// ✅ Correct
import dev.mars.apex.core.constants.SeverityConstants;
String severity = SeverityConstants.ERROR;

// ❌ Incorrect
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
# ✅ CORRECT: Returns TRUE for valid data
rules:
  - id: "amount-validation"
    condition: "#data.amount != null && #data.amount > 0"
    message: "Amount must be positive"
    severity: "ERROR"
    # Valid data (amount > 0): condition TRUE → success
    # Invalid data (amount ≤ 0): condition FALSE + ERROR → fail-fast

# ❌ INCORRECT: Returns TRUE for invalid data
rules:
  - id: "amount-validation-wrong"
    condition: "#data.amount == null || #data.amount <= 0"
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

Metrics are included in `RuleResult` objects:

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

### Performance Considerations

- **Recovery Overhead**: Recovery attempts add latency (typically 1-10ms)
- **Retry Delays**: Configure `retry-delay` based on your performance requirements
- **Max Retries**: Limit `max-retries` to prevent excessive delays (recommended: 0-3)

---

## Troubleshooting

### Validation Rules Always Succeed (Even with Invalid Data)

**Problem**: Validation rules with ERROR severity always return success, even when data is invalid.

**Cause**: Rule condition is written to return TRUE when data is **invalid** instead of when it's **valid**.

**Solution**: Rewrite rule conditions to return TRUE for valid data:

```yaml
# ❌ WRONG: Returns TRUE when field is missing
- condition: "#data['tradeId'] == null"
  severity: "ERROR"
  # Missing field → condition TRUE → success (wrong!)
  # Present field → condition FALSE → should fail (also wrong!)

# ✅ CORRECT: Returns TRUE when field is present
- condition: "#data['tradeId'] != null && !#data['tradeId'].toString().trim().isEmpty()"
  severity: "ERROR"
  # Present field → condition TRUE → success ✓
  # Missing field → condition FALSE + ERROR severity → fail-fast ✓
```

**Key Concept**: Severity-based fail-fast **only applies when condition evaluates to FALSE**. If your validation condition returns TRUE for invalid data, the rule will incorrectly report success.

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

✅ **Flexible Configuration**: Define policies in YAML or Java code  
✅ **Severity-Based Control**: Different behaviors for different error types  
✅ **Multiple Strategies**: Choose how to handle failures (fail fast, continue, retry, skip)  
✅ **Backward Compatibility**: Sensible defaults ensure existing code works  
✅ **Monitoring & Metrics**: Track recovery attempts and success rates

### Critical Concepts to Remember

1. **Severity applies when condition = FALSE**: Error recovery and fail-fast behavior only activate when a rule's condition evaluates to FALSE (rule did not match). When condition = TRUE (rule matched), the evaluation always succeeds regardless of severity.

2. **Write positive validation conditions**: Validation rules should return TRUE for valid data and FALSE for invalid data. This ensures that invalid data triggers the appropriate severity-based response.

3. **Default policies for ERROR/CRITICAL**: By default, ERROR and CRITICAL severities have recovery disabled and use FAIL_FAST strategy, meaning any validation failure will stop processing immediately.

4. **Test your validation rules**: Always verify that validation rules behave correctly with both valid and invalid data to ensure severity-based fail-fast works as expected.

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
**Version**: 2.1 (Updated with rule condition behavior clarification)
✅ **Production-Ready**: Battle-tested with comprehensive logging and debugging support


