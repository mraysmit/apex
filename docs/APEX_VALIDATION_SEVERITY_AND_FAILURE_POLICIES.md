# APEX Validation Severity and Failure Policy Configuration Guide

## Overview

APEX Rules Engine provides sophisticated **multi-layer** control over validation processing through:

1. **Rule-Level Severity** (`ERROR`, `WARNING`, `INFO`)
2. **Stage-Level Failure Policies** (`terminate`, `continue-with-warnings`, `flag-for-review`)
3. **Rule Group Behavior** (`stop-on-first-failure`, operator types)

This creates a flexible architecture where business logic determines whether processing continues or stops based on validation results.

---

## 🎯 Rule-Level Severity Configuration

### Individual Rule Severity

```yaml
rules:
  # CRITICAL ERROR - Causes stage failure when matched
  - id: "critical-validation"
    name: "Critical Data Validation"
    condition: "#data.amount > 0"
    message: "Amount must be positive"
    severity: "ERROR"     # ⚠️ Causes stage failure
    enabled: true
    
  # BUSINESS WARNING - Logs warning but doesn't fail stage  
  - id: "business-warning"
    name: "Business Rule Warning"
    condition: "#data.amount <= 1000000"
    message: "Large amount detected"
    severity: "WARNING"   # ⚠️ Logs warning, continues processing
    enabled: true
    
  # INFORMATIONAL - Provides audit information
  - id: "audit-info"
    name: "Processing Audit"
    condition: "true"
    message: "Trade processed successfully"
    severity: "INFO"      # ℹ️ Information only
    enabled: true
```

### Rule Group Behavior Control

```yaml
rule-groups:
  - id: "critical-checks"
    name: "Critical Validation Group"
    operator: "AND"                    # All rules must pass
    stop-on-first-failure: true       # Stop at first ERROR
    rules:
      - "mandatory-field-check"
      - "data-type-validation"
      
  - id: "warning-checks"  
    name: "Warning Checks Group"
    operator: "OR"                     # Any rule can match
    stop-on-first-failure: false      # Process all warnings
    rules:
      - "unusual-amount-warning"
      - "missing-optional-field"
```

---

## 🏗️ Stage-Level Failure Policy Configuration

### 1. `terminate` Policy - Stop Immediately

```yaml
processing-stages:
  - stage-name: "critical-validation"
    config-file: "config/critical-rules.yaml"
    execution-order: 1
    failure-policy: "terminate"        # 🛑 STOP on any stage failure
    required: true
    
  - stage-name: "subsequent-processing"
    execution-order: 2
    depends-on: ["critical-validation"] # ❌ Will be SKIPPED if validation fails
```

**Behavior:**
- Any `ERROR` severity rule match causes immediate stage failure
- Stage failure triggers scenario termination
- All subsequent stages are **SKIPPED**
- Used for: Regulatory compliance, mandatory business rules

### 2. `continue-with-warnings` Policy - Log and Continue

```yaml
processing-stages:
  - stage-name: "flexible-validation"
    config-file: "config/business-rules.yaml"
    execution-order: 1
    failure-policy: "continue-with-warnings"  # ⚠️ LOG warnings, CONTINUE processing
    required: true
    
  - stage-name: "next-stage"
    execution-order: 2
    depends-on: ["flexible-validation"]       # ✅ Will EXECUTE even if warnings occur
```

**Behavior:**
- `ERROR` severity rules are logged as warnings
- Processing continues to next stage
- Warnings are collected in scenario results
- Used for: Best-effort processing, non-critical validations

### 3. `flag-for-review` Policy - Manual Review Workflow

```yaml
processing-stages:
  - stage-name: "risk-assessment"
    config-file: "config/risk-rules.yaml"
    execution-order: 1
    failure-policy: "flag-for-review"         # 🏷️ FLAG for review, CONTINUE processing
    required: true
    
  - stage-name: "automated-processing"
    execution-order: 2
    depends-on: ["risk-assessment"]           # ✅ Will EXECUTE but marked for review
```

**Behavior:**
- `ERROR` severity rules trigger review flags
- Processing continues to completion
- Scenario marked `requiresReview = true`
- Used for: Risk management, complex business decisions

---

## 🔄 Multi-Layer Error Handling Architecture

### Layer 1: Rules Engine (Individual Rules)
```java
// My recent fix: SpEL exceptions now return RuleResult.error()
RuleResult result = rulesEngine.executeRulesList(rules, facts);
if (result.getResultType() == RuleResult.ResultType.ERROR) {
    // Structured error with rule name, message, severity
}
```

### Layer 2: Stage Executor (Stage-Level Policies)
```java
// ScenarioStageExecutor applies failure policies
switch (stage.getFailurePolicy()) {
    case "terminate":
        scenarioResult.setTerminated(true);
        return false; // Stop processing
        
    case "continue-with-warnings":
        scenarioResult.addWarning(errorMessage);
        return true;  // Continue processing
        
    case "flag-for-review":
        scenarioResult.setRequiresReview(true);
        return true;  // Continue but flag for review
}
```

### Layer 3: Scenario Configuration (YAML-Driven Behavior)
```yaml
# YAML controls which policy applies to each stage
failure-policy: "terminate" | "continue-with-warnings" | "flag-for-review"
```

---

## 🧪 Real-World Examples

### Example 1: Financial Trade Processing

```yaml
scenario:
  scenario-id: "trade-processing"
  processing-stages:
  
    # MUST PASS - Regulatory requirements
    - stage-name: "regulatory-validation"
      failure-policy: "terminate"           # 🛑 STOP if compliance fails
      rules:
        - condition: "#data.amount <= 10000000"  # MiFID II limits
          severity: "ERROR"
        - condition: "#data.counterparty != null"
          severity: "ERROR"
    
    # BUSINESS RULES - Flag unusual patterns  
    - stage-name: "business-validation"
      failure-policy: "flag-for-review"     # 🏷️ FLAG unusual trades
      depends-on: ["regulatory-validation"]
      rules:
        - condition: "#data.amount <= 1000000"   # Large trade warning
          severity: "WARNING"
        - condition: "#data.settlementDate <= T(java.time.LocalDate).now().plusDays(3)"
          severity: "WARNING"
    
    # OPTIONAL ENRICHMENT - Best effort
    - stage-name: "market-data-enrichment"  
      failure-policy: "continue-with-warnings"  # ⚠️ LOG issues, continue
      depends-on: ["regulatory-validation"]
      required: false
```

### Example 2: Customer Onboarding

```yaml
scenario:
  scenario-id: "customer-onboarding"
  processing-stages:
  
    # CRITICAL - Identity verification
    - stage-name: "identity-verification"
      failure-policy: "terminate"
      rules:
        - condition: "#data.ssn != null && #data.ssn.length() == 9"
          severity: "ERROR"
        - condition: "#data.documentVerified == true"
          severity: "ERROR"
    
    # RISK ASSESSMENT - Manual review for edge cases
    - stage-name: "risk-scoring" 
      failure-policy: "flag-for-review"
      rules:
        - condition: "#data.creditScore >= 600"
          severity: "WARNING"      # Low score = manual review
        - condition: "#data.income >= 30000"
          severity: "WARNING"
    
    # OPTIONAL - Marketing preferences  
    - stage-name: "preference-setup"
      failure-policy: "continue-with-warnings"
      required: false
      rules:
        - condition: "#data.emailPreferences != null"
          severity: "INFO"
```

---

## 💡 Best Practices

### 1. **Severity Assignment Guidelines**
- `ERROR`: Regulatory requirements, data integrity, mandatory business rules
- `WARNING`: Business preferences, unusual patterns, optional validations  
- `INFO`: Audit trails, processing confirmations, metrics

### 2. **Failure Policy Selection**
- `terminate`: Compliance, security, data corruption prevention
- `flag-for-review`: Risk management, complex business decisions
- `continue-with-warnings`: Optional enrichments, best-effort processing

### 3. **Stage Dependencies**
```yaml
# Critical path: Each stage depends on previous success
- stage-name: "validation"
  failure-policy: "terminate"
- stage-name: "enrichment"  
  depends-on: ["validation"]    # Only runs if validation passes
  
# Parallel processing: Independent stages
- stage-name: "audit-logging"
  failure-policy: "continue-with-warnings"
  # No dependencies - always attempts to run
```

### 4. **Testing Strategy**
- Test each failure policy with appropriate test data
- Verify stage dependencies work correctly
- Confirm warnings are collected and review flags are set
- Validate that `terminate` policy stops processing as expected

---

## 🔍 Monitoring and Observability

### Scenario Results Inspection
```java
ScenarioExecutionResult result = scenarioService.processData(tradeData);

// Check overall status
if (result.isTerminated()) {
    // Processing stopped due to critical failure
}

if (result.hasWarnings()) {
    List<String> warnings = result.getWarnings();
    // Log or display warnings to users
}

if (result.requiresReview()) {
    List<String> reviewFlags = result.getReviewFlags();
    // Route to manual review queue
}

// Examine individual stage results
for (StageExecutionResult stageResult : result.getStageResults()) {
    if (!stageResult.isSuccessful()) {
        // Handle stage-specific failures
    }
}
```

---

## 🚀 Recent Enhancements

### ✅ Extended Default-Value Support - COMPLETED

**Phase 3A Implementation** - Successfully extended the existing `default-value` functionality from field mappings to work in rule conditions and calculations.

#### **Key Features**
- **Rule-Level Defaults**: Rules can specify default values when evaluation fails
- **Calculation Defaults**: Enrichment calculations can provide safe fallback values
- **Type Support**: String, boolean, numeric, and decimal default values
- **Backward Compatible**: Existing configurations continue to work unchanged

#### **YAML Syntax Examples**

**Rules with Default Values:**
```yaml
rules:
  - id: "customer-validation"
    name: "Customer Validation Rule"
    condition: "#data.customer != null && #data.customer.name != null"
    message: "Customer validation passed"
    severity: "INFO"
    default-value: "Customer validation skipped - using default"

  - id: "age-check"
    name: "Age Verification Rule"
    condition: "#data.customer.age >= 18"
    severity: "WARNING"
    default-value: true
```

**Calculations with Default Values:**
```yaml
enrichments:
  - id: "risk-calculation"
    name: "Risk Score Calculation"
    type: "calculation-enrichment"
    condition: "#data.customer != null"
    calculation-config:
      expression: "#data.customer.creditScore * 0.7 + #data.customer.age * 0.3"
      result-field: "riskScore"
      default-value: 500.0
```

#### **Integration with Error Recovery**
- Rule-specific defaults take precedence over global recovery strategies
- Works seamlessly with existing severity-based error recovery
- Enhanced `UnifiedRuleEvaluator` checks for rule defaults before standard recovery
- Enhanced `YamlEnrichmentProcessor` uses calculation defaults for error recovery

#### **Business Value**
1. **Simple Configuration**: Business users specify appropriate defaults using familiar YAML syntax
2. **Rule-Level Control**: Each rule can have its own default behavior when evaluation fails
3. **Calculation Safety**: Calculations provide safe fallback values for complex expressions
4. **Operational Resilience**: Systems continue operating with sensible defaults instead of failing

#### **Test Coverage**
- **23 comprehensive tests** covering all aspects of the enhancement
- **Unit tests** for YAML configuration classes
- **Integration tests** for end-to-end functionality
- **Demonstration tests** showing practical usage
- **Backward compatibility** verified

---

This architecture provides **fine-grained control** over validation processing, allowing businesses to implement sophisticated error handling workflows that match their operational requirements while maintaining system resilience and audit capabilities.