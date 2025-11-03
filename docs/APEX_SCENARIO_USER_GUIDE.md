# APEX Scenario Processing - User Guide

**Version:** 2.0
**Author:** APEX Development Team
**Date:** 2025-11-02

## Overview

APEX Scenario Processing provides a powerful, YAML-driven framework for orchestrating complex data processing pipelines. This guide focuses on **how to configure and use scenarios** through YAML files.

**Key Concept**: Scenarios organize your processing logic into stages (validation, enrichment, compliance, etc.) with automatic routing, dependency management, and flexible failure handling—all configured through YAML.

---

## Quick Start

### 1. Create a Scenario Registry

The **scenario registry** is your entry point—a catalog of all available scenarios:

```yaml
metadata:
  id: "my-scenario-registry"
  name: "My Scenario Registry"
  version: "1.0.0"
  description: "Registry for trade processing scenarios"
  type: "scenario-registry"
  created-by: "your-team@company.com"
  created-date: "2025-01-15"

scenarios:
  - scenario-id: "trade-processing"
    config-file: "scenarios/trade-processing.yaml"
```

### 2. Create a Scenario Configuration

Define your processing pipeline with stages:

```yaml
metadata:
  id: "trade-processing"
  name: "Trade Processing Scenario"
  version: "1.0.0"
  description: "Complete trade processing pipeline"
  type: "scenario"
  business-domain: "Trading"
  owner: "trading.team@company.com"
  created-date: "2025-01-15"
  tags: [scenario, trading, validation, enrichment]

scenario:
  scenario-id: "trade-processing"
  name: "Trade Processing Scenario"
  description: "Complete trade processing pipeline"
  
  processing-stages:
    - stage-name: "validation"
      config-file: "config/trade-validation-rules.yaml"
      execution-order: 1
      failure-policy: "terminate"
      required: true
      stage-metadata:
        description: "Validate trade data"
        sla-ms: 1000
    
    - stage-name: "enrichment"
      config-file: "config/trade-enrichment-rules.yaml"
      execution-order: 2
      failure-policy: "continue-with-warnings"
      required: false
      depends-on: ["validation"]
      stage-metadata:
        description: "Enrich trade data"
        sla-ms: 2000
```

### 3. Use in Java Code

```java
// Create engine from scenario registry
RulesEngine engine = RulesEngine.fromScenarioRegistry("config/scenario-registry.yaml");

// Prepare your data
Map<String, Object> tradeData = Map.of(
    "tradeId", "TRD-12345",
    "instrumentType", "EQUITY",
    "quantity", 1000,
    "price", 150.50
);

// Process with specific scenario
ScenarioExecutionResult result = engine.evaluateScenario("trade-processing", tradeData);

// Check results
if (result.isSuccessful()) {
    System.out.println("Processing successful!");
} else if (result.isTerminated()) {
    System.out.println("Processing terminated: " + result.getWarnings());
} else if (result.requiresReview()) {
    System.out.println("Flagged for review: " + result.getReviewFlags());
}
```

---

## YAML Configuration Reference

### Scenario Registry Structure

```yaml
metadata:
  id: "unique-registry-id"              # Required: Unique identifier
  name: "Human Readable Name"           # Required: Display name
  version: "1.0.0"                      # Required: Version number
  description: "Registry description"   # Required: Description
  type: "scenario-registry"             # Required: Must be "scenario-registry"
  created-by: "team@company.com"        # Required: Creator information
  created-date: "2025-01-15"            # Optional: Creation date (YYYY-MM-DD)

scenarios:
  - scenario-id: "scenario-1"           # Required: Unique scenario ID
    config-file: "path/to/scenario.yaml" # Required: Path to scenario YAML

  - scenario-id: "scenario-2"
    config-file: "path/to/scenario2.yaml"

routing:
  strategy: "type-based"                # Optional: Routing strategy
  default-scenario: "scenario-1"        # Optional: Default scenario
```

### Scenario Configuration Structure

```yaml
metadata:
  id: "unique-scenario-id"              # Required: Unique identifier
  name: "Scenario Name"                 # Required: Display name
  version: "1.0.0"                      # Required: Version number
  description: "Scenario description"   # Required: Description
  type: "scenario"                      # Required: Must be "scenario"
  business-domain: "Trading"            # Required: Business domain
  owner: "team@company.com"             # Required: Owner contact
  created-date: "2025-01-15"            # Optional: Creation date (YYYY-MM-DD)
  tags: [scenario, trading, validation] # Optional: Tags for categorization

scenario:
  scenario-id: "unique-scenario-id"     # Required: Must match metadata.id
  name: "Scenario Name"                 # Required: Display name
  description: "What this scenario does" # Optional: Description

  # Optional: Type-based routing
  data-types:
    - "Trade"
    - "java.util.Map"

  # Optional: Classification-based routing (see below)
  classification-rule:
    condition: "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'"
    description: "US OTC option trades"

  # Required: Processing stages
  processing-stages:
    - stage-name: "validation"          # Required: Unique stage name
      config-file: "config/rules.yaml"  # Required: Path to rule config
      execution-order: 1                # Required: Numeric order (1, 2, 3...)
      failure-policy: "terminate"       # Required: See failure policies below
      required: true                    # Optional: Is stage mandatory? (default: false)
      depends-on: []                    # Optional: List of prerequisite stages
      stage-metadata:                   # Optional: Stage metadata
        description: "Stage description"
        sla-ms: 1000                    # SLA in milliseconds
```

---

## Classification-Based Routing

**Automatic scenario selection** based on data content using SpEL expressions.

### Basic Classification

```yaml
scenario:
  scenario-id: "otc-option-us"
  name: "OTC Option US Processing"
  
  classification-rule:
    condition: "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'"
    description: "US OTC option trades"
  
  processing-stages:
    # ... stages ...
```

### Supported SpEL Operations

```yaml
# Equality checks
condition: "#data['status'] == 'ACTIVE'"
condition: "#data['type'] != 'CANCELLED'"

# Numeric comparisons
condition: "#data['amount'] > 1000000"
condition: "#data['quantity'] >= 100"
condition: "#data['price'] < 50.0"

# Logical operators
condition: "#data['region'] == 'US' && #data['amount'] > 50000000"
condition: "#data['type'] == 'EQUITY' || #data['type'] == 'BOND'"
condition: "!#data['cancelled']"

# String operations
condition: "#data['currency'].startsWith('USD')"
condition: "#data['id'].contains('OTC')"
```

### Using Classification in Java

```java
// Create engine from registry with multiple scenarios
RulesEngine engine = RulesEngine.fromScenarioRegistry("config/multi-scenario-registry.yaml");

// Prepare data - APEX automatically selects the right scenario
Map<String, Object> tradeData = Map.of(
    "tradeType", "OTCOption",
    "region", "US",
    "notional", 75000000
);

// Automatic scenario selection based on classification rules
ScenarioExecutionResult result = engine.evaluateWithClassification(tradeData);

System.out.println("Matched scenario: " + result.getScenarioId());
```

### Multiple Scenarios with Classification

```yaml
scenarios:
  # First matching scenario wins (order matters!)
  - scenario-id: "otc-option-us"
    config-file: "scenarios/otc-option-us.yaml"
    
  - scenario-id: "bond-us"
    config-file: "scenarios/bond-us.yaml"
    
  - scenario-id: "equity-us"
    config-file: "scenarios/equity-us.yaml"
```

Each scenario file has its own classification rule:

```yaml
# otc-option-us.yaml
classification-rule:
  condition: "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'"
  description: "US OTC option trades"

# bond-us.yaml
classification-rule:
  condition: "#data['instrumentType'] == 'BOND' && #data['region'] == 'US'"
  description: "US bond trades"
```

---

## Failure Policies

Control how stages handle failures with three policies:

### 1. `terminate` - Stop Immediately

**Use for**: Critical validations, regulatory compliance, data integrity

```yaml
- stage-name: "regulatory-validation"
  config-file: "config/regulatory-rules.yaml"
  execution-order: 1
  failure-policy: "terminate"           # ⛔ Stop if this fails
  required: true
```

**Behavior**:
- Stage failure stops scenario execution immediately
- All remaining stages are skipped
- `result.isTerminated()` returns `true`
- Use for mandatory validations that must pass

### 2. `continue-with-warnings` - Log and Continue

**Use for**: Optional enrichments, best-effort processing, non-critical validations

```yaml
- stage-name: "market-data-enrichment"
  config-file: "config/market-data-rules.yaml"
  execution-order: 2
  failure-policy: "continue-with-warnings"  # ⚠️ Log warning, continue
  required: false
```

**Behavior**:
- Stage failure is logged as a warning
- Processing continues to next stage
- Warnings collected in `result.getWarnings()`
- Use for optional processing that shouldn't block the pipeline

### 3. `flag-for-review` - Mark for Manual Review

**Use for**: Risk assessment, complex business decisions, unusual patterns

```yaml
- stage-name: "risk-assessment"
  config-file: "config/risk-rules.yaml"
  execution-order: 2
  failure-policy: "flag-for-review"     # 🏷️ Flag for review, continue
  required: true
```

**Behavior**:
- Stage failure flags scenario for manual review
- Processing continues to next stage
- `result.requiresReview()` returns `true`
- Review flags collected in `result.getReviewFlags()`
- Use when human judgment is needed

---

## Stage Dependencies

Control execution order with `depends-on`:

```yaml
processing-stages:
  # Stage 1: Independent
  - stage-name: "validation"
    config-file: "config/validation-rules.yaml"
    execution-order: 1
    failure-policy: "terminate"
    depends-on: []                      # No dependencies
  
  # Stage 2: Depends on validation
  - stage-name: "enrichment"
    config-file: "config/enrichment-rules.yaml"
    execution-order: 2
    failure-policy: "continue-with-warnings"
    depends-on: ["validation"]          # Only runs if validation succeeds
  
  # Stage 3: Depends on enrichment
  - stage-name: "compliance"
    config-file: "config/compliance-rules.yaml"
    execution-order: 3
    failure-policy: "flag-for-review"
    depends-on: ["enrichment"]          # Only runs if enrichment succeeds
```

**Dependency Rules**:
- Stages execute in `execution-order` sequence
- If a dependency fails, the stage is **skipped**
- Skipped stages appear in `result.getSkippedStages()`
- Multiple dependencies: ALL must succeed for stage to run

---

## Complete Example: Trade Processing

### Registry File: `config/trade-scenario-registry.yaml`

```yaml
metadata:
  id: "trade-processing-registry"
  name: "Trade Processing Scenario Registry"
  version: "1.0.0"
  description: "Registry for trade processing scenarios"
  type: "scenario-registry"
  created-by: "trading.team@company.com"
  created-date: "2025-01-15"

scenarios:
  - scenario-id: "otc-option-us"
    config-file: "scenarios/otc-option-us-scenario.yaml"

  - scenario-id: "equity-us"
    config-file: "scenarios/equity-us-scenario.yaml"

routing:
  strategy: "type-based"
  default-scenario: "otc-option-us"
```

### Scenario File: `scenarios/otc-option-us-scenario.yaml`

```yaml
metadata:
  id: "otc-option-us"
  name: "OTC Option US Processing"
  version: "1.0.0"
  description: "Complete processing pipeline for US OTC options"
  type: "scenario"
  business-domain: "Derivatives Trading"
  owner: "derivatives.team@company.com"
  created-date: "2025-01-15"
  tags: [scenario, otc, options, derivatives, us]

scenario:
  scenario-id: "otc-option-us"
  name: "OTC Option US Processing"
  description: "Complete processing pipeline for US OTC options"
  
  classification-rule:
    condition: "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'"
    description: "US OTC option trades"
  
  processing-stages:
    # Stage 1: Critical validation
    - stage-name: "validation"
      config-file: "config/otc-validation-rules.yaml"
      execution-order: 1
      failure-policy: "terminate"
      required: true
      stage-metadata:
        description: "Validate trade data integrity"
        sla-ms: 1000
    
    # Stage 2: Enrich with market data
    - stage-name: "enrichment"
      config-file: "config/otc-enrichment-rules.yaml"
      execution-order: 2
      failure-policy: "continue-with-warnings"
      required: false
      depends-on: ["validation"]
      stage-metadata:
        description: "Enrich with market data"
        sla-ms: 2000
    
    # Stage 3: Risk assessment
    - stage-name: "risk-assessment"
      config-file: "config/otc-risk-rules.yaml"
      execution-order: 3
      failure-policy: "flag-for-review"
      required: true
      depends-on: ["validation", "enrichment"]
      stage-metadata:
        description: "Assess trade risk"
        sla-ms: 1500
```

### Java Usage

```java
// Initialize engine once
RulesEngine engine = RulesEngine.fromScenarioRegistry("config/trade-scenario-registry.yaml");

// Process OTC option trade
Map<String, Object> otcTrade = Map.of(
    "tradeType", "OTCOption",
    "region", "US",
    "notional", 75000000,
    "strike", 150.0,
    "expiry", "2025-12-31"
);

// Automatic scenario selection
ScenarioExecutionResult result = engine.evaluateWithClassification(otcTrade);

// Check results
if (result.isSuccessful()) {
    System.out.println("✅ Trade processed successfully");
    System.out.println("Scenario: " + result.getScenarioId());
    System.out.println("Execution time: " + result.getTotalExecutionTimeMs() + "ms");
} else if (result.isTerminated()) {
    System.out.println("⛔ Processing terminated");
    result.getWarnings().forEach(System.out::println);
} else if (result.requiresReview()) {
    System.out.println("🏷️ Flagged for manual review");
    result.getReviewFlags().forEach(System.out::println);
}

// Cleanup when done
engine.shutdown();
```

---

## Best Practices

### 1. Failure Policy Selection

| Use Case | Policy | Rationale |
|----------|--------|-----------|
| Regulatory compliance | `terminate` | Must pass, no exceptions |
| Data integrity checks | `terminate` | Corrupted data shouldn't proceed |
| Market data enrichment | `continue-with-warnings` | Best effort, not critical |
| Risk scoring | `flag-for-review` | Needs human judgment |
| Optional calculations | `continue-with-warnings` | Nice to have, not required |

### 2. Stage Organization

```yaml
# ✅ GOOD: Clear separation of concerns
processing-stages:
  - stage-name: "validation"          # Data integrity
  - stage-name: "enrichment"          # Add context
  - stage-name: "compliance"          # Business rules
  - stage-name: "risk-assessment"     # Risk analysis

# ❌ BAD: Mixed concerns in one stage
processing-stages:
  - stage-name: "validation-and-enrichment-and-compliance"
```

### 3. Dependency Management

```yaml
# ✅ GOOD: Clear dependency chain
- stage-name: "validation"
  depends-on: []
- stage-name: "enrichment"
  depends-on: ["validation"]
- stage-name: "compliance"
  depends-on: ["validation", "enrichment"]

# ❌ BAD: Circular dependencies (will fail validation)
- stage-name: "stage-a"
  depends-on: ["stage-b"]
- stage-name: "stage-b"
  depends-on: ["stage-a"]
```

### 4. SLA Monitoring

```yaml
stage-metadata:
  description: "Clear description of what this stage does"
  sla-ms: 1000                        # Set realistic SLA targets
```

Monitor SLA compliance:
```java
result.getStageResults().forEach(stage -> {
    if (stage.getExecutionTimeMs() > stage.getSlaMs()) {
        System.out.println("⚠️ SLA breach: " + stage.getStageName());
    }
});
```

---

## Common Patterns

### Pattern 1: Validation → Enrichment → Compliance

```yaml
processing-stages:
  - stage-name: "validation"
    failure-policy: "terminate"       # Must pass
    execution-order: 1
  
  - stage-name: "enrichment"
    failure-policy: "continue-with-warnings"
    execution-order: 2
    depends-on: ["validation"]
  
  - stage-name: "compliance"
    failure-policy: "flag-for-review"
    execution-order: 3
    depends-on: ["validation", "enrichment"]
```

### Pattern 2: Parallel Independent Stages

```yaml
processing-stages:
  - stage-name: "validation"
    execution-order: 1
    depends-on: []
  
  # These can run in any order after validation
  - stage-name: "market-data"
    execution-order: 2
    depends-on: ["validation"]
  
  - stage-name: "reference-data"
    execution-order: 3
    depends-on: ["validation"]
  
  - stage-name: "risk-data"
    execution-order: 4
    depends-on: ["validation"]
```

### Pattern 3: Critical Path with Optional Branches

```yaml
processing-stages:
  # Critical path
  - stage-name: "validation"
    failure-policy: "terminate"
    required: true
  
  - stage-name: "compliance"
    failure-policy: "terminate"
    required: true
    depends-on: ["validation"]
  
  # Optional branches
  - stage-name: "analytics"
    failure-policy: "continue-with-warnings"
    required: false
    depends-on: ["validation"]
  
  - stage-name: "reporting"
    failure-policy: "continue-with-warnings"
    required: false
    depends-on: ["compliance"]
```

---

## Troubleshooting

### Scenario Not Found

```
IllegalArgumentException: Scenario 'my-scenario' not found in registry
```

**Solution**: Check `scenario-id` in registry matches the ID you're using:
```yaml
scenarios:
  - scenario-id: "my-scenario"        # Must match exactly
    config-file: "scenarios/my-scenario.yaml"
```

### No Matching Scenario

```
IllegalStateException: No matching scenario found for the provided input data
```

**Solution**: Ensure at least one scenario's classification rule matches your data:
```yaml
classification-rule:
  condition: "#data['tradeType'] == 'OTCOption'"  # Check field names and values
```

### Stage Configuration Not Found

```
Stage configuration file not found: config/rules.yaml
```

**Solution**: Verify file paths are correct relative to your working directory:
```yaml
- stage-name: "validation"
  config-file: "config/validation-rules.yaml"  # Check path exists
```

### Circular Dependencies

```
Circular dependency detected in stage dependencies
```

**Solution**: Remove circular references in `depends-on`:
```yaml
# ❌ BAD
- stage-name: "a"
  depends-on: ["b"]
- stage-name: "b"
  depends-on: ["a"]

# ✅ GOOD
- stage-name: "a"
  depends-on: []
- stage-name: "b"
  depends-on: ["a"]
```

---

## API Reference

### RulesEngine Methods

```java
// Create from scenario registry
RulesEngine engine = RulesEngine.fromScenarioRegistry("registry.yaml");

// Evaluate specific scenario by ID
ScenarioExecutionResult result = engine.evaluateScenario("scenario-id", data);

// Automatic classification-based routing
ScenarioExecutionResult result = engine.evaluateWithClassification(data);

// Fluent API
ScenarioExecutionResult result = engine.asScenario().evaluate("scenario-id", data);
ScenarioExecutionResult result = engine.asScenario().evaluateWithClassification(data);

// Cleanup
engine.shutdown();
```

### ScenarioExecutionResult Methods

```java
// Overall status
boolean isSuccessful()
boolean isTerminated()
boolean requiresReview()

// Scenario info
String getScenarioId()
long getTotalExecutionTimeMs()

// Stage results
List<StageExecutionResult> getStageResults()
List<StageExecutionResult> getSuccessfulStages()
List<StageExecutionResult> getFailedStages()
Map<String, String> getSkippedStages()

// Warnings and review flags
List<String> getWarnings()
boolean hasWarnings()
List<String> getReviewFlags()
boolean hasReviewFlags()

// Outputs
Map<String, Object> getScenarioOutputs()
Object getScenarioOutput(String key)

// Summary
String getExecutionSummary()
```

---

## Next Steps

1. **Start Simple**: Create a basic scenario with 2-3 stages
2. **Test Failure Policies**: Experiment with different policies to understand behavior
3. **Add Classification**: Use classification rules for automatic routing
4. **Monitor Performance**: Track SLA compliance and execution times
5. **Iterate**: Refine your stages based on real-world usage

For implementation details and advanced topics, see `SCENARIO_DESIGN_SUMMARY.md`.

