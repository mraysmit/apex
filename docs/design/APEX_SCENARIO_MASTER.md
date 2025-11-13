# APEX Scenario System - Master Documentation

**Version:** 5.0 (Consolidated)  
**Date:** 2025-10-16  
**Status:** Current Production Implementation + Proposed Enhancements

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [System Overview](#system-overview)
3. [Architecture](#architecture)
4. [Core Components](#core-components)
5. [Configuration Guide](#configuration-guide)
6. [Classification Rules](#classification-rules)
7. [Stage-Based Processing](#stage-based-processing)
8. [Failure Policies](#failure-policies)
9. [Usage Examples](#usage-examples)
10. [Best Practices & Guidelines](#best-practices--guidelines)
11. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Simplest Example (Option B - Embedded Classification)

```java
// 1. Initialize service
DataTypeScenarioService scenarioService = new DataTypeScenarioService();
scenarioService.loadScenarios("config/scenario-registry.yaml");

// 2. Create Map data
Map<String, Object> data = new HashMap<>();
data.put("tradeType", "OTCOption");
data.put("region", "US");
data.put("notional", 75000000);

// 3. Process with automatic classification-based routing
ScenarioExecutionResult result = scenarioService.processMapData(data);

// 4. Check results
if (result.isSuccessful()) {
    logger.info("Scenario: {}", result.getScenarioId());
}
```

### Scenario YAML with Embedded Classification

```yaml
metadata:
  id: "otc-option-us"
  name: "OTC Option US Processing"
  type: "scenario"

scenario:
  scenario-id: "otc-option-us"
  
  # CRITICAL: Classification rule for automatic routing
  classification-rule:
    condition: "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'"
    description: "US OTC option trades"

  processing-stages:
    - stage-name: "validation"
      config-file: "config/otc-validation-rules.yaml"
      execution-order: 1
      failure-policy: "terminate"
```

---

## System Overview

### What is a Scenario?

A **scenario** is a named configuration that:
1. **Defines classification rules** using SpEL to match incoming Map data
2. **Defines processing stages** with execution order and dependencies
3. **Specifies failure policies** for error handling
4. **Provides metadata** for monitoring and compliance

### Key Capabilities

- **Automatic Scenario Selection** - Classification rules route data to appropriate scenarios
- **Stage-Based Processing** - Sequential execution with dependency management
- **SpEL-Based Classification** - Business rules evaluate Map<String, Object> data
- **Error Recovery** - Configurable failure policies (terminate, continue-with-warnings, flag-for-review)
- **Performance Monitoring** - SLA tracking and execution metrics

### Design Principles

1. **100% Generic** - No default rules; all rules are data-driven
2. **Lightweight Scenarios** - Scenarios contain routing logic only
3. **Separation of Concerns** - Scenarios route; rule files contain business logic
4. **SpEL-Based Selection** - Automatic routing based on data content

---

## Architecture

### Three-Layer Hierarchy

```
Discovery Layer
  ↓ (Registry lists scenarios)
Routing Layer
  ↓ (Scenarios with classification rules)
Processing Layer
  ↓ (Rule configuration files)
```

**Layer 1: Discovery** - Scenario Registry (`config/data-type-scenarios.yaml`)
- Central catalog of all available scenarios
- Maps scenario IDs to configuration files

**Layer 2: Routing** - Individual Scenario Files (`scenarios/*.yaml`)
- Lightweight files with classification rules
- Reference rule configuration files
- Contain minimal routing logic

**Layer 3: Processing** - Rule Configuration Files (`config/*.yaml`)
- Actual business rules and validation logic
- Reusable across multiple scenarios

---

## Core Components

### 1. ScenarioConfiguration Class

**Location**: `apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioConfiguration.java`

**Key Properties**:
- `scenarioId` - Unique identifier
- `classificationRuleCondition` - SpEL expression for automatic selection
- `classificationRuleDescription` - Human-readable description
- `processingStages` - List of stage configurations
- `metadata` - Business domain, owner, SLA, etc.

**Key Methods**:
```java
public boolean hasClassificationRule();
public boolean matchesClassificationRule(Map<String, Object> data);
public List<ScenarioStage> getStagesByExecutionOrder();
```

### 2. DataTypeScenarioService Class

**Location**: `apex-core/src/main/java/dev/mars/apex/core/service/scenario/DataTypeScenarioService.java`

**Key Responsibilities**:
- Loads scenario configurations from YAML registry
- Routes data to appropriate scenarios based on classification rules
- Evaluates classification rules against Map<String, Object> data
- Caches scenario configurations

**Key Methods**:
```java
public void loadScenarios(String registryPath) throws Exception;
public ScenarioConfiguration getScenarioForMapData(Map<String, Object> data);
public ScenarioExecutionResult processMapData(Map<String, Object> data);
public ScenarioExecutionResult processDataWithStages(Object data, String scenarioId);
```

### 3. ScenarioStageExecutor Class

**Location**: `apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioStageExecutor.java`

**Key Features**:
- Handles stage dependencies and execution order
- Implements configurable failure policies per stage
- Provides comprehensive error handling
- Tracks performance and SLA compliance
- Supports context sharing between stages

### 4. Result Classes

**ScenarioExecutionResult** - Overall scenario execution result
- `scenarioId` - Scenario that was executed
- `successful` - Overall success status
- `stageResults` - Individual stage results
- `warnings` - Warning messages
- `totalExecutionTimeMs` - Total execution time

**StageExecutionResult** - Individual stage result
- `stageName` - Name of the stage
- `successful` - Whether stage succeeded
- `errorMessage` - Error message if failed
- `executionTimeMs` - Stage execution time

---

## Configuration Guide

### Scenario Registry File

```yaml
metadata:
  id: "scenario-registry"
  name: "Scenario Registry"
  type: "scenario-registry"

scenarios:
  - scenario-id: "otc-option-us"
    config-file: "scenarios/otc-option-us-scenario.yaml"
    business-domain: "Derivatives Trading"
    owner: "derivatives.team@company.com"

  - scenario-id: "bond-us"
    config-file: "scenarios/bond-us-scenario.yaml"
    business-domain: "Fixed Income"

routing:
  strategy: "classification-based"
  default-scenario: "generic-trade-processing"
```

### Individual Scenario File

```yaml
metadata:
  id: "otc-option-us"
  name: "OTC Option US Processing"
  type: "scenario"
  business-domain: "Derivatives Trading"
  owner: "derivatives.team@company.com"

scenario:
  scenario-id: "otc-option-us"
  name: "OTC Option US Processing"
  description: "Multi-stage processing for US OTC options"

  # REQUIRED: Classification rule for automatic selection
  classification-rule:
    condition: "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'"
    description: "Matches US OTC option trades"

  processing-stages:
    - stage-name: "validation"
      config-file: "config/otc-validation-rules.yaml"
      execution-order: 1
      failure-policy: "terminate"
      required: true
      stage-metadata:
        description: "Validate trade data"
        sla-ms: 1000

    - stage-name: "enrichment"
      config-file: "config/otc-enrichment-rules.yaml"
      execution-order: 2
      failure-policy: "continue-with-warnings"
      depends-on: ["validation"]
      stage-metadata:
        description: "Enrich trade data"
        sla-ms: 2000
```

---

## Classification Rules

### Option B: Embedded Classification (Recommended)

**Purpose**: Classification rule embedded directly in scenario file

**Advantages**:
- Simple (one file per scenario)
- Classification tightly coupled to scenario
- Easy to understand and maintain
- No intermediate classification string needed

**Example**:
```yaml
classification-rule:
  condition: "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'"
  description: "Matches US OTC option trades"
```

### Option A: Separate Classification File (Advanced)

**Purpose**: Separate classification rules file for shared logic

**When to use**:
- Multiple scenarios share same classification logic
- Classification rules managed separately from scenarios
- Need centralized classification management

**Classification Rules File**:
```yaml
metadata:
  id: "trade-classification-rules"
  type: "classification-rules"

classification-rules:
  - rule-id: "otc-option-us"
    condition: "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'"
    classification: "otc-option-us"
    priority: 1
```

### SpEL Expression Examples

```
Simple field match:
  #data['tradeType'] == 'OTCOption'

AND conditions:
  #data['tradeType'] == 'OTCOption' && #data['region'] == 'US'

OR conditions:
  #data['region'] == 'US' || #data['currency'] == 'USD'

Numeric comparisons:
  #data['notional'] > 100000000

Complex business logic:
  #data['tradeType'] == 'OTCOption' &&
  #data['notional'] > 50000000 &&
  (#data['region'] == 'US' || #data['currency'] == 'USD')
```

---

## Stage-Based Processing

### What are Processing Stages?

Sequential phases of data processing, each with:
- **Unique name** and purpose
- **Execution order** defining when it runs
- **Dependencies** on other stages
- **Failure policy** determining error handling
- **Metadata** for monitoring and SLA tracking

### Stage Dependencies

```yaml
processing-stages:
  - stage-name: "validation"
    execution-order: 1
    failure-policy: "terminate"

  - stage-name: "enrichment"
    execution-order: 2
    depends-on: ["validation"]  # Only runs if validation succeeds

  - stage-name: "compliance"
    execution-order: 3
    depends-on: ["validation", "enrichment"]  # Multiple dependencies
```

**Dependency Resolution**:
1. Stages sorted by execution order
2. Before executing, check if all dependencies succeeded
3. If dependencies not met, skip the stage
4. If dependencies met, execute the stage

---

## Failure Policies

### 1. `terminate` - Stop Processing Immediately

**Use Case**: Critical validation failures, regulatory compliance violations

**Behavior**:
- Stops scenario execution immediately
- Marks remaining stages as skipped
- Returns error result to caller

**When to Use**:
- Mandatory field validation
- Data integrity checks
- Security validations
- Regulatory compliance rules

### 2. `continue-with-warnings` - Log Warnings and Continue

**Use Case**: Optional enrichments, best-effort processing

**Behavior**:
- Logs warnings but continues to next stage
- Marks stage as completed with warnings
- Allows dependent stages to execute

**When to Use**:
- Market data enrichment (may be unavailable)
- Optional calculations
- Non-critical data augmentation

### 3. `flag-for-review` - Mark for Manual Review

**Use Case**: Complex business decisions, unusual patterns

**Behavior**:
- Flags scenario for manual review
- Continues processing remaining stages
- Adds review flag to result

**When to Use**:
- Risk management rules
- Complex business validations
- Unusual trade patterns
- Threshold breaches requiring review

### Failure Policy Comparison

| Policy | Stops? | Logs Error? | Flags Review? | Use Case |
|--------|--------|------------|---------------|----------|
| `terminate` | Yes | Yes | No | Critical failures |
| `continue-with-warnings` | No | Yes (warning) | No | Optional processing |
| `flag-for-review` | No | Yes | Yes | Manual review needed |

---

## Usage Examples

### Automatic Scenario Selection

```java
DataTypeScenarioService scenarioService = new DataTypeScenarioService();
scenarioService.loadScenarios("config/scenario-registry.yaml");

Map<String, Object> tradeData = new HashMap<>();
tradeData.put("tradeType", "OTCOption");
tradeData.put("region", "US");
tradeData.put("notional", 75000000);

ScenarioExecutionResult result = scenarioService.processMapData(tradeData);

if (result.isSuccessful()) {
    logger.info("Successfully processed with scenario: {}", result.getScenarioId());
}
```

### Stage-Based Processing with Error Handling

```java
ScenarioExecutionResult result = scenarioService.processDataWithStages(data, "scenario-id");

if (result.isTerminated()) {
    logger.warn("Processing terminated due to critical failure");
    handleTermination(result);
}

if (result.hasWarnings()) {
    logger.info("Processing completed with warnings: {}", result.getWarnings());
    handleWarnings(result);
}

if (result.requiresReview()) {
    logger.info("Scenario flagged for manual review");
    routeToReviewQueue(result);
}
```

### Monitoring Stage Execution

```java
for (StageExecutionResult stageResult : result.getStageResults()) {
    logger.info("Stage '{}': {} ({}ms)",
        stageResult.getStageName(),
        stageResult.isSuccessful() ? "SUCCESS" : "FAILED",
        stageResult.getExecutionTimeMs());
    
    if (stageResult.isSkipped()) {
        logger.info("Stage skipped: {}", stageResult.getSkipReason());
    }
}
```

---

## Best Practices & Guidelines

### YAML Validation

**MANDATORY PROCESS**:
1. Create YAML file
2. Create compiler validation test
3. Run validation BEFORE claiming success
4. Fix all errors
5. Only then run functional tests

**Valid APEX Document Types**:
- `rule-config`
- `enrichment`
- `dataset`
- `scenario`
- `scenario-registry`
- `bootstrap`
- `rule-chain`
- `external-data-config`
- `pipeline-config`

### Keep Examples Simple

**Guidelines**:
- **Maximum 50 lines** for test YAML files
- **One concept per file** - don't demonstrate everything at once
- **No promotional language** - "advanced", "sophisticated"
- **No fake features** - only demonstrate what actually exists
- **Clear purpose** - what specific functionality does this test?

### Metadata Fields by Type

| Document Type | Required Fields |
|---------------|-----------------|
| `rule-config` | `id`, `name`, `version`, `description`, `type`, `author` |
| `scenario` | `id`, `name`, `version`, `description`, `type`, `business-domain`, `owner` |
| `scenario-registry` | `id`, `name`, `version`, `description`, `type`, `created-by` |

### Stage Configuration Guidelines

**Failure Policy Selection**:
- Use `terminate` for compliance, security, data corruption prevention
- Use `flag-for-review` for risk management, complex business decisions
- Use `continue-with-warnings` for optional enrichments, best-effort processing

**SLA Configuration**:
```yaml
- stage-name: "validation"
  stage-metadata:
    sla-ms: 500              # Fast validation
    critical: true

- stage-name: "market-data"
  stage-metadata:
    sla-ms: 5000            # Allow time for external calls
    critical: false
```

**Naming Conventions**:
- **Stage Names**: Use descriptive, action-oriented names (`"validation"`, `"market-data-enrichment"`)
- **Scenario IDs**: Use domain-datatype-purpose pattern (`"otc-options-standard"`)

---

## Troubleshooting

### Common Issues

**1. Stage Dependencies Not Met**

Symptom: Stages are skipped unexpectedly

Solution:
```java
for (StageExecutionResult stage : result.getStageResults()) {
    if (stage.isSkipped()) {
        logger.info("Stage '{}' skipped: {}",
            stage.getStageName(), stage.getSkipReason());
    }
}
```

**2. Classification Rule Not Matching**

Symptom: No scenario found for data

Solution: Verify classification rule condition matches data content
```java
// Check what data is being evaluated
logger.debug("Data for classification: {}", data);

// Verify classification rule in scenario YAML
classification-rule:
  condition: "#data['tradeType'] == 'OTCOption'"  # Ensure field names match
```

**3. SLA Violations**

Symptom: Processing takes longer than expected

Solution:
```java
result.getStageResults().forEach(stage -> {
    if (stage.getExecutionTimeMs() > stage.getSlaMs()) {
        logger.warn("SLA violation in stage '{}': {}ms > {}ms",
            stage.getStageName(),
            stage.getExecutionTimeMs(),
            stage.getSlaMs());
    }
});
```

### Debugging Tips

1. **Enable Debug Logging**:
```properties
logging.level.dev.mars.apex.core.service.scenario=DEBUG
```

2. **Validate Configuration**:
```java
YamlMetadataValidator validator = new YamlMetadataValidator();
YamlValidationResult result = validator.validateFile("scenarios/my-scenario.yaml");
if (!result.isValid()) {
    result.getErrors().forEach(System.err::println);
}
```

3. **Check Scenario Discovery**:
```java
List<String> availableScenarios = scenarioService.getAvailableScenarios();

// Test classification rule directly
ScenarioConfiguration scenario = scenarioService.getScenario("otc-option-us");
boolean matches = scenario.matchesClassificationRule(testData);
logger.info("Classification rule matches: {}", matches);
```

---

## Critical Mistakes to Avoid

### 1. NEVER Hallucinate YAML Syntax

**WRONG**:
```yaml
type: "business-rules"           # ❌ Not valid
business-rules-config:           # ❌ Not valid
confidence-scoring:              # ❌ Not valid
```

**CORRECT**:
```yaml
type: "rule-config"              # ✅ Valid
rules:                           # ✅ Valid
enrichments:                     # ✅ Valid
```

### 2. ALWAYS Validate YAML with APEX Compiler

- Create YAML file
- Create compiler validation test
- Run validation BEFORE claiming success
- Fix all errors
- Only then run functional tests

### 3. Follow Existing Patterns

Before creating new code:
1. Search for similar functionality
2. Study the reference implementation
3. Follow the same structure

### 4. Test File Naming

```
[TestClassName]-[purpose].yaml
```

Examples:
```
✅ DataTypeScenarioServiceClassificationTest-registry.yaml
✅ DataTypeScenarioServiceClassificationTest-otc-scenario.yaml
✅ DataTypeScenarioServiceClassificationTest-validation-rules.yaml
```

### 5. Error Recovery

If you make a mistake:
1. Acknowledge it immediately
2. Identify root cause
3. Fix it properly
4. Verify the fix
5. Document the lesson

---

## Summary

The APEX scenario system provides:
- **Automatic scenario selection** based on SpEL classification rules
- **Stage-based processing** with dependencies and failure policies
- **Flexible configuration** using YAML and SpEL expressions
- **Performance monitoring** with SLA tracking
- **Multi-environment support** for dev/test/prod

The system is production-ready and handles Map<String, Object> data with automatic scenario selection based on classification rules.

**Key Takeaway**: Classification rules are the critical routing mechanism that determines which scenario processes the incoming data!

