# APEX Scenario System - Design & Implementation Summary

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

### 2. DataTypeScenarioService
**Location**: `apex-core/src/main/java/dev/mars/apex/core/service/scenario/DataTypeScenarioService.java`

Central service for managing scenarios and automatic routing.

**Key Responsibilities**:
- Load scenario configurations from YAML registry
- Route data to appropriate scenarios based on type or classification rules
- Cache scenarios for performance (LinkedHashMap preserves insertion order)
- Execute stage-based processing

**Key Methods**:
```java
public void loadScenarios(String registryPath)
public ScenarioConfiguration getScenarioForData(Object data)
public ScenarioConfiguration getScenarioForMapData(Map<String, Object> data)
public ScenarioExecutionResult processMapData(Map<String, Object> data)
public ScenarioExecutionResult processDataWithStages(Object data, String scenarioId)
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
Stage 3 (compliance) ← depends on Stage 2
    ↓
Result
```

### Dependency Resolution

- Stages execute in `executionOrder` sequence
- Stages with `dependsOn` are skipped if dependencies fail
- Failure policies determine whether to continue or terminate

### Performance Monitoring

Each stage tracks:
- Execution time in milliseconds
- SLA compliance (from stage metadata)
- Success/failure status
- Warnings and errors

---

## Configuration Example

### Registry File
```yaml
scenarios:
  - scenario-id: "otc-option-us"
    config-file: "scenarios/otc-option-us-scenario.yaml"
    business-domain: "Derivatives Trading"
    owner: "derivatives.team@company.com"
```

### Scenario File
```yaml
scenario:
  scenario-id: "otc-option-us"
  name: "OTC Option US Processing"
  
  classification-rule:
    condition: "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'"
    description: "US OTC option trades"
  
  processing-stages:
    - stage-name: "validation"
      config-file: "config/otc-validation-rules.yaml"
      execution-order: 1
      failure-policy: "terminate"
      required: true
    
    - stage-name: "enrichment"
      config-file: "config/otc-enrichment-rules.yaml"
      execution-order: 2
      failure-policy: "continue-with-warnings"
      depends-on: ["validation"]
```

---

## Usage Pattern

```java
// 1. Create service
DataTypeScenarioService scenarioService = new DataTypeScenarioService();

// 2. Load scenarios
scenarioService.loadScenarios("config/scenario-registry.yaml");

// 3. Process Map data with automatic routing
Map<String, Object> tradeData = new HashMap<>();
tradeData.put("tradeType", "OTCOption");
tradeData.put("region", "US");
tradeData.put("notional", 75000000);

ScenarioExecutionResult result = scenarioService.processMapData(tradeData);

// 4. Check results
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

---

## Status

**Current Implementation**: Version 3.0 - Production Ready
- Automatic scenario selection via SpEL classification rules
- Stage-based processing with dependencies and failure policies
- Flexible YAML configuration
- Performance monitoring with SLA tracking
- Multi-environment support (dev/test/prod)

