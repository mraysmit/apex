# APEX Scenario System - Design & Implementation Summary

> **⚠️ IMPORTANT API UPDATE (Version 3.0)**
>
> **The `DataTypeScenarioService` class has been deprecated** and will be removed in version 4.0.
>
> **Use `RulesEngine.fromScenarioRegistry()` instead** - the universal entry point that handles scenario-based processing automatically.
>
> **Migration Example:**
> ```java
> // OLD (Deprecated):
> DataTypeScenarioService service = new DataTypeScenarioService();
> service.loadScenarios("config/data-type-scenarios.yaml");
> ScenarioExecutionResult result = service.processMapData(data);
>
> // NEW - SIMPLEST (One Line):
> RuleResult result = RulesEngine.fromScenarioRegistry("config/data-type-scenarios.yaml").evaluateScenario(data);
>
> // NEW - REUSABLE (Two Lines):
> RulesEngine engine = RulesEngine.fromScenarioRegistry("config/data-type-scenarios.yaml");
> RuleResult result = engine.evaluateScenario(data);
> ```
>
> **Why this change?** Developers should not need to use different services for different YAML content types. `RulesEngine` provides ONE universal API for all YAML processing including scenarios.

## Overview

The APEX Scenario system is a sophisticated, production-ready framework for managing complex data processing pipelines through a **three-layer hierarchy** with automatic routing, stage-based execution, and comprehensive failure handling.

**Key Principle**: 100% generic, data-driven system with no hardcoded business logic. All routing and processing rules come from external YAML configuration.

---

## Core Architecture

### Three-Layer Hierarchy

1. **Discovery Layer** - Scenario Registry (`config/data-type-scenarios.yaml`)
   - Central catalog of all available scenarios
   - Maps scenario IDs to configuration files
   - Contains metadata (business domain, owner, risk category, SLA)

2. **Routing Layer** - Scenario Files (`scenarios/*.yaml`)
   - Lightweight routing configurations
   - Data type mappings (backward compatibility)
   - **Classification rules** using SpEL expressions for Map-based data
   - Processing stage definitions

3. **Processing Layer** - Rule Configuration Files (`config/*.yaml`)
   - Business rules, validation logic, enrichment configurations
   - Referenced by scenarios, not directly by applications

4. **Execution Layer** - Stage Executor
   - Dependency-aware execution
   - Failure policy enforcement
   - Performance monitoring and SLA tracking

---

## Core Components

### 1. ScenarioConfiguration
**Location**: `apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioConfiguration.java`

Represents a complete data type processing scenario.

**Key Properties**:
- `scenarioId` - Unique identifier
- `name`, `description` - Human-readable metadata
- `dataTypes` - Legacy support for type-based routing
- `classificationRuleCondition` - SpEL expression (e.g., `#data['tradeType'] == 'OTCOption'`)
- `classificationRuleDescription` - When scenario applies
- `processingStages` - List of ScenarioStage objects
- `metadata` - Business domain, owner, SLA, risk category

**Key Methods**:
```java
public boolean hasClassificationRule()
public boolean matchesClassificationRule(Map<String, Object> data)
public boolean hasStageConfiguration()
public List<ScenarioStage> getStagesByExecutionOrder()
```

### 2. DataTypeScenarioService (🔄 DEPRECATED)
**Location**: `apex-core/src/main/java/dev/mars/apex/core/service/scenario/DataTypeScenarioService.java`

> **⚠️ DEPRECATED** - This service is deprecated as of version 3.0. Use `RulesEngine.fromScenarioRegistry()` instead.

Central service for managing scenarios and automatic routing.

**Key Responsibilities**:
- Load scenario configurations from YAML registry
- Route data to appropriate scenarios based on type or classification rules
- Cache scenarios for performance (LinkedHashMap preserves insertion order)
- Execute stage-based processing

**Old Methods (Deprecated)**:
```java
public void loadScenarios(String registryPath)
public ScenarioConfiguration getScenarioForData(Object data)
public ScenarioConfiguration getScenarioForMapData(Map<String, Object> data)
public ScenarioExecutionResult processMapData(Map<String, Object> data)
public ScenarioExecutionResult processDataWithStages(Object data, String scenarioId)
```

**New Recommended API**:
```java
// ⭐ SIMPLEST - One line for single evaluation
RuleResult result = RulesEngine.fromScenarioRegistry("config/data-type-scenarios.yaml").evaluateScenario(data);

// ✅ REUSABLE - Two lines for multiple evaluations
RulesEngine engine = RulesEngine.fromScenarioRegistry("config/data-type-scenarios.yaml");
RuleResult result = engine.evaluateScenario(data);
```

### 3. ScenarioStage
**Location**: `apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioStage.java`

Represents a single processing stage within a scenario.

**Key Properties**:
- `stageName` - Unique stage identifier
- `configFile` - Path to rule configuration file
- `executionOrder` - Numeric order for execution
- `failurePolicy` - How to handle failures
- `dependsOn` - List of prerequisite stages
- `required` - Whether stage is mandatory
- `stageMetadata` - Description, SLA in ms

**Failure Policies**:
- `terminate` - Stop immediately on failure
- `continue-with-warnings` - Log warnings, continue to next stage
- `flag-for-review` - Mark for manual review, continue processing

**Validation**:
- Stage name and config file required
- Execution order must be positive
- No self-dependencies allowed
- Failure policy must be valid

### 4. ScenarioStageExecutor
**Location**: `apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioStageExecutor.java`

Executes scenario stages with dependency management and failure policies.

**Key Features**:
- Dependency-aware execution (skips stages if dependencies fail)
- Failure policy enforcement
- Performance monitoring and SLA tracking
- Comprehensive error handling
- Context sharing between stages

**Execution Flow**:
1. Sort stages by execution order
2. For each stage:
   - Check if dependencies are satisfied
   - Skip if dependencies failed
   - Execute stage with timeout
   - Apply failure policy
   - Terminate if policy requires it

### 5. ScenarioExecutionResult
**Location**: `apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioExecutionResult.java`

Aggregates results from all stages in a scenario execution.

**Key Properties**:
- `scenarioId` - Which scenario executed
- `successful` - Overall success status
- `terminated` - Whether processing was terminated
- `requiresReview` - Manual review needed
- `stageResults` - Results from each stage
- `warnings` - Accumulated warnings
- `reviewFlags` - Reasons for review
- `skippedStages` - Stages skipped with reasons
- `totalExecutionTimeMs` - Total execution time

---

## Classification-Based Routing

### SpEL Expression Support

Scenarios can use SpEL expressions to automatically match incoming Map data:

```yaml
classification-rule:
  condition: "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'"
  description: "US OTC Option trades"
```

**Supported Operations**:
- Equality: `==`, `!=`
- Comparison: `>`, `<`, `>=`, `<=`
- Logical: `&&`, `||`, `!`
- String operations: `.contains()`, `.startsWith()`, etc.

### Routing Priority

Scenarios are evaluated in **insertion order** (LinkedHashMap). First matching scenario wins.

---

## Stage-Based Processing

### Execution Model

```
Stage 1 (validation)
    ↓ (success)
Stage 2 (enrichment) ← depends on Stage 1
    ↓ (success)
Stage 3 (compliance) ← depends on Stage 1 (parallel with enrichment)
    ↓
Result
```

**Key Features**:
- Stages execute in `executionOrder` sequence
- Dependencies defined via `depends-on` array
- Parallel execution when stages share same dependencies
- Automatic skipping of stages with unsatisfied dependencies

### Dependency Resolution

**Algorithm**: Dependency-aware execution with topological sorting

**Execution Rules**:
1. Stages execute in `executionOrder` sequence
2. Before executing a stage, all dependencies in `depends-on` are checked
3. If any dependency failed, the stage is **skipped** (tracked in `skippedStages`)
4. Failure policies determine whether to continue or terminate
5. Stages with same dependencies can execute in parallel

**Example Dependency Chain**:
```yaml
processing-stages:
  - stage-name: "validation"
    execution-order: 1
    failure-policy: "terminate"
    # No dependencies - always runs first

  - stage-name: "market-data-enrichment"
    execution-order: 2
    depends-on: ["validation"]  # Only runs if validation passes
    failure-policy: "continue-with-warnings"

  - stage-name: "compliance"
    execution-order: 3
    depends-on: ["validation"]  # Parallel with enrichment (both depend only on validation)
    failure-policy: "flag-for-review"
```

**Dependency Validation**:
- **Self-referencing detection**: Stages cannot depend on themselves
- **Circular dependency detection**: Uses DFS with recursion stack to detect cycles (A→B→C→A)
- **Missing dependency handling**: Stages with non-existent dependencies are skipped gracefully
- **Clear error messages**: Validation errors indicate specific dependency issues

### Circular Dependency Detection

APEX uses **topological sorting** with DFS to detect circular dependencies:

**Detected Patterns**:
- Self-referencing: `stage-a` depends on `stage-a`
- Two-stage cycles: `stage-a` → `stage-b` → `stage-a`
- Three-stage cycles: `stage-a` → `stage-b` → `stage-c` → `stage-a`
- Complex multi-path cycles

**Detection Algorithm**:
```java
// From ComplexWorkflowExecutor.java
private void topologicalSort(String stageId, ...) {
    if (visiting.contains(stageId)) {
        throw new RuntimeException("Circular dependency detected involving stage: " + stageId);
    }
    // ... DFS traversal with recursion stack tracking
}
```

**Test Coverage**:
- `ScenarioStageCircularDependencyTest.java` - Tests all circular dependency patterns
- `ScenarioStageMissingDependencyTest.java` - Tests missing dependency handling

### Failure Policy Interaction with Dependencies

| Policy | Behavior | Impact on Dependent Stages |
|--------|----------|---------------------------|
| `terminate` | Stop processing immediately | All dependent stages are **skipped** |
| `continue-with-warnings` | Log warnings, continue | Dependent stages **may execute** (stage marked as "successful with warnings") |
| `flag-for-review` | Mark for review, continue | Dependent stages **may execute** (stage marked as "requires review") |

**Example**:
```yaml
- stage-name: "validation"
  failure-policy: "terminate"  # MUST pass
  # If validation fails → all dependent stages are skipped

- stage-name: "enrichment"
  failure-policy: "continue-with-warnings"  # Optional
  depends-on: ["validation"]
  # If enrichment fails → compliance can still run (if it doesn't depend on enrichment)
```

### Performance Monitoring

Each stage tracks:
- Execution time in milliseconds
- SLA compliance (from stage metadata)
- Success/failure status
- Warnings and errors
- Dependency satisfaction status
- Skip reason (if stage was skipped)

---

## Dependency Management

### Two Types of Dependencies

APEX scenarios support **two types of dependencies**:

#### 1. Stage Dependencies (Processing Stages)
Dependencies between **processing stages within a single scenario** - stages execute in order based on their dependencies.

**YAML Syntax**:
```yaml
processing-stages:
  - stage-name: "validation"
    execution-order: 1
    # No dependencies - runs first

  - stage-name: "enrichment"
    execution-order: 2
    depends-on: ["validation"]  # Array of stage names

  - stage-name: "compliance"
    execution-order: 3
    depends-on: ["validation", "enrichment"]  # Multiple dependencies
```

**Runtime Behavior**:
- Before executing a stage, `ScenarioStageExecutor` checks if all dependencies succeeded
- If any dependency failed, the stage is **skipped** with reason tracked
- Skipped stages appear in `ScenarioExecutionResult.skippedStages`

#### 2. File Dependencies (Configuration References)
Dependencies between **YAML configuration files** - scenarios reference other YAML files.

**YAML Keywords**:
| Keyword | Purpose | Example |
|---------|---------|---------|
| `rule-configurations` | References rule config files | `rule-configurations: [validation-rules.yaml]` |
| `enrichment-refs` | References enrichment files | `enrichment-refs: [market-data-enrichment.yaml]` |
| `config-files` | References general configs | `config-files: [shared-config.yaml]` |
| `rule-chains` | References rule chain files | `rule-chains: [chain-1.yaml]` |

**Example**:
```yaml
metadata:
  id: trade-processing-scenario
  type: scenario

# File-based dependencies
rule-configurations:
  - validation-rules.yaml
  - compliance-rules.yaml

enrichment-refs:
  - market-data-enrichment.yaml
  - pricing-enrichment.yaml

scenario:
  scenario-id: trade-processing
  processing-stages:
    - stage-name: validation
      config-file: validation-rules.yaml  # References file above
```

### Dependency Graph Analysis

The `YamlDependencyGraph` class analyzes file dependencies:
- Builds dependency graph from YAML file references
- Detects circular dependencies using DFS algorithm
- Provides topological sort for correct loading order
- Generates visual dependency trees for documentation

---

## Configuration Example

### Registry File
```yaml
scenarios:
  - scenario-id: "otc-option-us"
    config-file: "scenarios/otc-option-us-scenario.yaml"
    business-domain: "Derivatives Trading"
    owner: "derivatives.team@company.com"
    risk-category: "High"
```

### Scenario File (with Dependencies)
```yaml
scenario:
  scenario-id: "otc-option-us"
  name: "OTC Option US Processing"
  description: "Multi-stage processing for US OTC option trades"

  classification-rule:
    condition: "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'"
    description: "US OTC option trades"

  processing-stages:
    # Stage 1: Validation (no dependencies - always runs first)
    - stage-name: "validation"
      config-file: "config/otc-validation-rules.yaml"
      execution-order: 1
      failure-policy: "terminate"  # MUST pass
      required: true
      stage-metadata:
        description: "Validate trade data completeness and correctness"
        sla-ms: 100

    # Stage 2: Market Data Enrichment (depends on validation)
    - stage-name: "market-data-enrichment"
      config-file: "config/market-data-enrichment.yaml"
      execution-order: 2
      failure-policy: "continue-with-warnings"  # Optional
      depends-on: ["validation"]
      stage-metadata:
        description: "Enrich with market data (prices, volatility)"
        sla-ms: 500

    # Stage 3: Compliance (depends on validation, parallel with enrichment)
    - stage-name: "compliance"
      config-file: "config/compliance-rules.yaml"
      execution-order: 3
      failure-policy: "flag-for-review"
      depends-on: ["validation"]  # Only depends on validation, not enrichment
      required: true
      stage-metadata:
        description: "Apply regulatory compliance rules"
        sla-ms: 200
```

**Execution Flow**:
1. **Validation** runs first (execution-order: 1, no dependencies)
   - If fails with `terminate` policy → enrichment and compliance are **skipped**
   - If passes → continue to stages 2 and 3
2. **Market-data-enrichment** runs (depends on validation)
   - Can run in parallel with compliance (both depend only on validation)
   - If fails with `continue-with-warnings` → compliance still runs
3. **Compliance** runs (depends on validation)
   - Can run in parallel with enrichment
   - If fails with `flag-for-review` → processing completes but flagged

---

## Usage Pattern

### Recommended API (Version 3.0+)

**⭐ SIMPLEST (One Line) - For single evaluation:**
```java
// Prepare trade data
Map<String, Object> tradeData = Map.of(
    "tradeType", "OTCOption",
    "region", "US",
    "notional", 75000000
);

// Process with automatic scenario routing (one line!)
RuleResult result = RulesEngine.fromScenarioRegistry("config/scenario-registry.yaml").evaluateScenario(tradeData);

// Check results
if (result.isSuccess()) {
    logger.info("Processing successful: {}", result.getMessage());
} else {
    logger.error("Processing failed: {}", result.getFailureMessages());
}
```

**✅ REUSABLE (Two Lines) - For multiple evaluations:**
```java
// Create engine from scenario registry
RulesEngine engine = RulesEngine.fromScenarioRegistry("config/scenario-registry.yaml");

// Process multiple trades
for (Map<String, Object> tradeData : trades) {
    RuleResult result = engine.evaluateScenario(tradeData);

    if (result.isSuccess()) {
        logger.info("Processing successful: {}", result.getMessage());
    } else {
        logger.error("Processing failed: {}", result.getFailureMessages());
    }
}

// Cleanup
engine.shutdown();
```

### Legacy API (Deprecated - for reference only)

```java
// ⚠️ DEPRECATED - Do not use in new code
DataTypeScenarioService scenarioService = new DataTypeScenarioService();
scenarioService.loadScenarios("config/scenario-registry.yaml");

Map<String, Object> tradeData = new HashMap<>();
tradeData.put("tradeType", "OTCOption");
tradeData.put("region", "US");
tradeData.put("notional", 75000000);

ScenarioExecutionResult result = scenarioService.processMapData(tradeData);

if (result.isSuccessful()) {
    logger.info("Processed with scenario: {}", result.getScenarioId());
} else if (result.isTerminated()) {
    logger.error("Processing terminated: {}", result.getWarnings());
} else if (result.requiresReview()) {
    logger.warn("Flagged for review: {}", result.getReviewFlags());
}
```

---

## Design Principles

1. **100% Generic** - No hardcoded business logic; all rules from YAML
2. **Separation of Concerns** - Scenarios route; rules contain logic
3. **SpEL-Based Selection** - Automatic scenario matching via expressions
4. **Backward Compatible** - Supports legacy type-based routing
5. **Production-Ready** - Comprehensive error handling, monitoring, SLA tracking
6. **Extensible** - Easy to add new stages, failure policies, or routing strategies
7. **Dependency-Aware** - Topological sorting ensures correct execution order
8. **Fail-Safe** - Circular dependencies and missing dependencies detected early

---

## Best Practices

### Stage Dependency Design

1. **Use `execution-order`** to define intended sequence (1, 2, 3, ...)
2. **Use `depends-on`** to enforce actual dependencies between stages
3. **Set appropriate `failure-policy`** based on stage criticality:
   - `terminate` for critical stages (validation, authentication)
   - `continue-with-warnings` for optional stages (enrichment, analytics)
   - `flag-for-review` for compliance/risk stages
4. **Mark critical stages as `required: true`**
5. **Add `stage-metadata`** for monitoring and SLA tracking
6. **Test circular dependencies** explicitly in your test suite
7. **Validate missing dependencies** are handled gracefully

### Parallel Execution Optimization

To enable parallel execution of independent stages:
```yaml
processing-stages:
  - stage-name: "validation"
    execution-order: 1

  # These two stages can run in parallel (both depend only on validation)
  - stage-name: "market-data-enrichment"
    execution-order: 2
    depends-on: ["validation"]

  - stage-name: "compliance-check"
    execution-order: 2  # Same execution order
    depends-on: ["validation"]  # Same dependencies
```

### Error Handling

**Intentional Errors in Tests**:
When testing error conditions (circular dependencies, missing dependencies), use the `"TEST:"` prefix in logs:
```java
logger.info("TEST: Triggering intentional error - Circular dependency");
```

This makes it clear the error is expected and part of the test design.

---

## Test Coverage

### Core Tests (apex-core)

**Circular Dependency Tests**:
- `ScenarioStageCircularDependencyTest.java`
  - Self-referencing stages (A→A)
  - Two-stage cycles (A→B→A)
  - Three-stage cycles (A→B→C→A)
  - Complex multi-path cycles

**Missing Dependency Tests**:
- `ScenarioStageMissingDependencyTest.java`
  - Single missing dependency
  - Multiple missing dependencies
  - Graceful error handling

**Stage Execution Tests**:
- `ScenarioStageExecutorTest.java`
  - Dependency-aware execution
  - Failure policy enforcement
  - Performance monitoring
  - SLA tracking

**Classification Tests**:
- `ScenarioConfigurationClassificationTest.java`
  - SpEL-based classification rules
  - Map-based data routing
  - Multiple scenario matching

### Demo Tests (apex-demo)

**Real-World Scenarios**:
- `BasicStageConfigurationTest.java`
  - Multi-stage processing with dependencies
  - Failure policy handling
  - Validation and enrichment stages
  - Comprehensive result validation

**Business Domain Tests**:
- `TradeValidationCodesDemo.java`
  - OTC options trade processing
  - Error/success codes with field mappings
  - Real business calculations (Greeks, notional, pricing)

---

## Status

**Current Implementation**: Version 3.0 - Production Ready

**Features**:
- ✅ Automatic scenario selection via SpEL classification rules
- ✅ Stage-based processing with dependencies and failure policies
- ✅ Circular dependency detection with topological sorting
- ✅ Missing dependency handling with graceful skipping
- ✅ Parallel execution support for independent stages
- ✅ Flexible YAML configuration
- ✅ Performance monitoring with SLA tracking
- ✅ Multi-environment support (dev/test/prod)
- ✅ Comprehensive test coverage (circular deps, missing deps, failure policies)

**Dependency Features**:
- ✅ Stage dependencies with `depends-on` array
- ✅ File dependencies with `rule-configurations`, `enrichment-refs`, etc.
- ✅ Topological sorting for execution order
- ✅ DFS-based circular dependency detection
- ✅ Self-referencing detection
- ✅ Missing dependency tracking in execution results
- ✅ Clear error messages for dependency issues

