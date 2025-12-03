# APEX Scenario User Guide

**Version:** 3.0  
**Date:** 2025-11-19  
**Author:** Mark Andrew Ray-Smith Cityline Ltd  
**Status:** Production Ready

---

## Table of Contents

1. [Introduction](#introduction)
2. [What are Scenarios?](#what-are-scenarios)
3. [Getting Started](#getting-started)
4. [Scenario Structure](#scenario-structure)
5. [Classification Rules](#classification-rules)
6. [Processing Stages](#processing-stages)
7. [Stage Dependencies](#stage-dependencies)
8. [Failure Policies](#failure-policies)
9. [Scenario Registry](#scenario-registry)
10. [Best Practices](#best-practices)
11. [Common Patterns](#common-patterns)
12. [Troubleshooting](#troubleshooting)
13. [Migration Guide](#migration-guide)

---

## 1. Introduction

APEX Scenarios provide a powerful way to route and process data through multiple stages based on data content. This guide will help you understand how to create, configure, and use scenarios in your APEX applications.

### What You'll Learn
- How to create scenario configurations
- How to write classification rules for automatic routing
- How to define processing stages with dependencies
- How to handle failures with different policies
- Best practices and common patterns

### Prerequisites
- Basic understanding of APEX YAML configuration
- Familiarity with APEX rules and enrichments
- Knowledge of SpEL (Spring Expression Language) basics

---

## 2. What are Scenarios?

### Overview

A **scenario** is a named configuration that automatically routes data through a multi-stage processing pipeline based on data content.

### Key Concepts

| Concept | Description |
|---------|-------------|
| **Classification Rules** | SpEL expressions that match incoming data to scenarios |
| **Processing Stages** | Sequential phases of data processing |
| **Stage Dependencies** | Requirements that must be met before a stage executes |
| **Failure Policies** | Rules for how to handle errors in each stage |
| **Scenario Registry** | Central catalog of all available scenarios |

### Why Use Scenarios?

✅ **Automatic Routing** - Data is automatically routed to the right scenario based on content  
✅ **Multi-Stage Processing** - Break complex workflows into manageable stages  
✅ **Dependency Management** - Ensure stages execute in the correct order  
✅ **Error Handling** - Control how failures are handled at each stage  
✅ **Monitoring** - Track performance and SLA compliance per stage

### When to Use Scenarios

✅ **Use scenarios when you have:**
- Data that needs different processing based on its content
- Complex workflows with multiple sequential stages
- Stages that depend on results from previous stages
- Different error handling requirements per stage

❌ **Don't use scenarios when:**
- You only have simple, single-step processing
- All data follows the same processing path
- You don't need automatic routing

---

## 3. Getting Started

### Creating Your First Scenario

Let's create a simple scenario for processing trade data.

**Step 1: Create the scenario registry**

```yaml
# config/scenario-registry.yaml
metadata:
  id: "scenario-registry"
  name: "Trade Processing Scenarios"
  type: "scenario-registry"
  version: "1.0.0"

scenarios:
  - scenario-id: "otc-option-us"
    config-file: "scenarios/otc-option-us.yaml"
    business-domain: "Derivatives Trading"
    owner: "trading-team@example.com"
```

**Step 2: Create the scenario file**

```yaml
# scenarios/otc-option-us.yaml
metadata:
  id: "otc-option-us"
  name: "OTC Option US Processing"
  type: "scenario"
  version: "1.0.0"
  description: "Processing pipeline for US OTC options"
  business-domain: "Derivatives Trading"
  owner: "trading-team@example.com"

scenario:
  scenario-id: "otc-option-us"
  
  # Classification rule for automatic routing
  classification-rule:
    condition: "#'tradeType'] == 'OTCOption' && #'region'] == 'US'"
    description: "Matches US OTC option trades"

  processing-stages:
    - stage-name: "validation"
      config-file: "config/otc-validation-rules.yaml"
      execution-order: 1
      failure-policy: "terminate"
```

**Step 3: Create the validation rules**

```yaml
# config/otc-validation-rules.yaml
metadata:
  type: "rule-config"
  id: "otc-validation"
  version: "1.0.0"

rules:
  - id: "trade-id-required"
    condition: "tradeId != null && !tradeId.isEmpty()"
    message: "Trade ID is required"
    severity: "ERROR"

  - id: "notional-positive"
    condition: "notional > 0"
    message: "Notional must be positive"
    severity: "ERROR"
```

**Step 4: Process data with your scenario**

```java
// Create engine from scenario registry
RulesEngine engine = RulesEngine.fromScenarioRegistry("config/scenario-registry.yaml");

// Prepare trade data
Map<String, Object> tradeData = Map.of(
    "tradeType", "OTCOption",
    "region", "US",
    "tradeId", "TR-12345",
    "notional", 100000.00
);

// Process with automatic scenario routing
RuleResult result = engine.evaluateScenario(tradeData);

// Check results
if (result.isSuccess()) {
    System.out.println("Trade processed successfully!");
} else {
    System.out.println("Processing failed: " + result.getFailureMessages());
}

// Cleanup
engine.shutdown();
```

---

## 4. Scenario Structure

### Basic Structure

Every scenario file must have:
1. **Metadata section** with `type: "scenario"`
2. **Scenario section** with `scenario-id` and `classification-rule`
3. **Processing stages** defining the workflow

```yaml
metadata:
  id: "my-scenario"              # Required: Unique identifier
  name: "My Scenario"            # Required: Human-readable name
  type: "scenario"               # Required: Must be "scenario"
  version: "1.0.0"               # Required: Version number
  description: "..."             # Optional but recommended
  business-domain: "..."         # Optional
  owner: "..."                   # Optional
  criticality: "high"            # Optional: low, medium, high, critical
  sla-ms: 5000                   # Optional: SLA in milliseconds

scenario:
  scenario-id: "my-scenario"     # Required: Must match metadata.id
  name: "My Scenario"            # Optional: Display name
  description: "..."             # Optional: Detailed description
  
  # Required: Classification rule for routing
  classification-rule:
    condition: "#'field'] == 'value'"
    description: "When this scenario applies"
  
  # Required: At least one processing stage
  processing-stages:
    - stage-name: "validation"
      config-file: "path/to/rules.yaml"
      execution-order: 1
      failure-policy: "terminate"
```

### Metadata Fields

| Field | Required | Description |
|-------|----------|-------------|
| `id` | Required | Unique identifier, matches scenario-id |
| `name` | Required | Human-readable name |
| `type` | Required | Must be "scenario" |
| `version` | Required | Semantic version (e.g., "1.0.0") |
| `description` | Recommended | What the scenario does |
| `business-domain` | Recommended | Business area (e.g., "Trading") |
| `owner` | Recommended | Team/person email |
| `criticality` | Optional | low, medium, high, critical |
| `sla-ms` | Optional | Target execution time in ms |
| `tags` | Optional | Tags for categorization |

---

## 5. Classification Rules

### Overview

Classification rules use **SpEL (Spring Expression Language)** expressions to automatically match incoming data to the appropriate scenario.

### Basic Syntax

The data being evaluated is available as #context in SpEL expressions:

```yaml
classification-rule:
  condition: "#'fieldName'] == 'expectedValue'"
  description: "Human-readable explanation"
```

### SpEL Expression Examples

**Simple field match:**
```yaml
condition: "#'tradeType'] == 'OTCOption'"
```

**AND conditions:**
```yaml
condition: "#'tradeType'] == 'OTCOption' && #'region'] == 'US'"
```

**OR conditions:**
```yaml
condition: "#'region'] == 'US' || #'currency'] == 'USD'"
```

**Numeric comparisons:**
```yaml
condition: "#'notional'] > 100000000"
```

**String operations:**
```yaml
condition: "#'tradeId'].startsWith('TR-') && #'tradeId'].length() == 10"
```

**Complex business logic:**
```yaml
condition: |
  #'tradeType'] == 'OTCOption' &&
  #'notional'] > 50000000 &&
  (#'region'] == 'US' || #'currency'] == 'USD')
```

**Null checks:**
```yaml
condition: "#'counterparty'] != null && !#'counterparty'].isEmpty()"
```

### Best Practices for Classification Rules

✅ **Be specific** - Make rules as specific as possible to avoid ambiguity
```yaml
# Good
condition: "#'tradeType'] == 'OTCOption' && #'region'] == 'US' && #'assetClass'] == 'Equity'"

# Too broad
condition: "#'tradeType'] == 'OTCOption'"
```

✅ **Test null values** - Always check for null before accessing properties
```yaml
# Good
condition: "#'region'] != null && #'region'] == 'US'"

# Risky (NullPointerException if region is null)
condition: "#'region'] == 'US'"
```

✅ **Document the logic** - Use clear descriptions
```yaml
classification-rule:
  condition: "#'tradeType'] == 'OTCOption' && #'notional'] > 50000000"
  description: "Large OTC option trades (notional > 50M)"
```

✅ **Order matters** - Scenarios are evaluated in registry order; first match wins
```yaml
scenarios:
  # More specific scenario first
  - scenario-id: "otc-option-us-large"
    condition: "#'tradeType'] == 'OTCOption' && #'region'] == 'US' && #'notional'] > 100000000"
  
  # More general scenario second
  - scenario-id: "otc-option-us"
    condition: "#'tradeType'] == 'OTCOption' && #'region'] == 'US'"
```

---

## 6. Processing Stages

### Overview

Processing stages define the sequential steps in your scenario's workflow. Each stage references a configuration file and can have its own execution order, failure policy, and dependencies.

### Stage Configuration

```yaml
processing-stages:
  - stage-name: "validation"              # Required: Unique stage name
    config-file: "path/to/config.yaml"    # Required: Configuration file path
    execution-order: 1                    # Required: Numeric execution order
    condition: "#'region'] == 'US'"  # Optional: SpEL condition for execution
    failure-policy: "terminate"           # Optional: Override default policy
    depends-on: []                        # Optional: List of prerequisite stages
    required: true                        # Optional: Whether stage is mandatory
    stage-metadata:                       # Optional: Stage-specific metadata
      description: "Validate trade data"
      sla-ms: 1000
      critical: true
```

### Field Descriptions

| Field | Required | Description |
|-------|----------|-------------|
| `stage-name` | Required | Unique identifier for the stage |
| `config-file` | Required | Path to rule/enrichment configuration file |
| `execution-order` | Required | Numeric order (1, 2, 3, ...) |
| `condition` | Optional | SpEL expression controlling stage execution |
| `failure-policy` | Optional | How to handle failures (see below) |
| `depends-on` | Optional | Array of stage names that must succeed first |
| `required` | Optional | Whether stage must succeed (default: true) |
| `stage-metadata` | Optional | Additional metadata for monitoring |

### Execution Order

Stages execute in ascending order based on `execution-order`:

```yaml
processing-stages:
  - stage-name: "validation"
    execution-order: 1        # Executes first

  - stage-name: "enrichment"
    execution-order: 2        # Executes second

  - stage-name: "compliance"
    execution-order: 3        # Executes third
```

**Gaps are allowed** - Use gaps to leave room for future stages:
```yaml
processing-stages:
  - stage-name: "validation"
    execution-order: 1

  - stage-name: "enrichment"
    execution-order: 10       # Gap allows inserting stages between

  - stage-name: "compliance"
    execution-order: 20
```

### Stage Metadata

Add metadata for monitoring and documentation:

```yaml
stage-metadata:
  description: "Validate trade data for completeness and correctness"
  sla-ms: 500                    # Target execution time in milliseconds
  critical: true                 # Whether stage is business-critical
  owner: "validation-team@example.com"
  tags: ["validation", "compliance"]
```

### Conditional Stage Execution

Stages can have optional SpEL `condition` expressions that control whether the stage executes:

```yaml
processing-stages:
  # Always executes (no condition)
  - stage-name: "base-validation"
    config-file: "config/base-validation.yaml"
    execution-order: 1

  # Only executes if region is US
  - stage-name: "us-compliance"
    config-file: "config/us-compliance.yaml"
    execution-order: 2
    condition: "#'region'] == 'US'"

  # Only executes if notional > $10M
  - stage-name: "high-value-checks"
    config-file: "config/high-value.yaml"
    execution-order: 3
    condition: "#'notionalAmount'] > 10000000"
```

**How Conditions Work:**
1. **Evaluated before execution** - Condition checked before stage runs
2. **Boolean result** - Must evaluate to true/false
3. **Skip if false** - Stage skipped if condition is false or evaluation fails
4. **No condition = always execute** - Backward compatible (stages without conditions always run)
5. **Access to data** - Condition has full access to `#context` and other variables in context

**Condition Examples:**

```yaml
# Region-based conditions
condition: "#'region'] == 'US'"
condition: "#'region'] == 'EMEA' || #'region'] == 'APAC'"

# Value-based conditions
condition: "#'notionalAmount'] > 10000000"
condition: "#'quantity'] >= 1000"

# Type-based conditions
condition: "#'productType'] == 'OTC_OPTION'"
condition: "#'instrumentType'] == 'EQUITY' || #'instrumentType'] == 'BOND'"

# Complex conditions
condition: "#'region'] == 'US' && #'notionalAmount'] > 10000000"
condition: "#'approvedBy'] != null && #'creditLimitChecked'] == true"
```

---

## 7. Stage Dependencies

### Overview

Stage dependencies ensure that stages execute only when their prerequisite stages have succeeded. This is critical for workflows where later stages depend on data or validations from earlier stages.

> **Note:** Conditions are evaluated **before** dependencies. If a condition is false, the stage is skipped without checking dependencies.

### Defining Dependencies

Use the `depends-on` array to specify prerequisite stages:

```yaml
processing-stages:
  - stage-name: "validation"
    execution-order: 1
    # No dependencies - always runs first

  - stage-name: "enrichment"
    execution-order: 2
    depends-on: ["validation"]  # Only runs if validation succeeds

  - stage-name: "compliance"
    execution-order: 3
    depends-on: ["validation", "enrichment"]  # Needs both to succeed
```

### Dependency Rules

1. **Dependencies must exist** - All stages in `depends-on` must be defined
2. **No circular dependencies** - Stage A cannot depend on Stage B if Stage B depends on Stage A
3. **No self-dependencies** - Stages cannot depend on themselves
4. **Execution order respected** - Dependencies should have lower execution-order numbers

### Dependency Behavior

| Scenario | Behavior |
|----------|----------|
| All dependencies succeeded | Stage executes normally |
| Any dependency failed with `terminate` | Stage is **skipped** (tracked in result) |
| Dependency failed with `continue-with-warnings` | Stage **may execute** (dependency marked as succeeded with warnings) |
| Dependency failed with `flag-for-review` | Stage **may execute** (dependency marked as succeeded with review flag) |

### Example: Complex Dependencies

```yaml
processing-stages:
  # Stage 1: Basic validation (no dependencies)
  - stage-name: "basic-validation"
    execution-order: 1
    failure-policy: "terminate"

  # Stage 2: Data enrichment (depends on validation)
  - stage-name: "market-data-enrichment"
    execution-order: 2
    depends-on: ["basic-validation"]
    failure-policy: "continue-with-warnings"

  # Stage 3: Advanced validation (depends on validation and enrichment)
  - stage-name: "advanced-validation"
    execution-order: 3
    depends-on: ["basic-validation", "market-data-enrichment"]
    failure-policy: "terminate"

  # Stage 4: Compliance check (only depends on basic validation)
  - stage-name: "compliance-check"
    execution-order: 4
    depends-on: ["basic-validation"]
    failure-policy: "flag-for-review"
```

**Execution Flow:**
1. `basic-validation` runs first
   - If fails → all other stages are skipped (terminate policy)
   - If succeeds → continue
2. `market-data-enrichment` runs (depends on basic-validation)
   - If fails → advanced-validation is skipped, but compliance-check can still run
   - If succeeds → continue
3. `advanced-validation` runs (depends on both previous stages)
   - Only runs if both succeeded (or enrichment has warnings)
4. `compliance-check` runs (only depends on basic-validation)
   - Can run even if enrichment or advanced-validation failed

### Parallel Execution

Stages with the same dependencies can execute in parallel:

```yaml
processing-stages:
  - stage-name: "validation"
    execution-order: 1

  # These two stages can run in parallel (both depend only on validation)
  - stage-name: "market-data-enrichment"
    execution-order: 2
    depends-on: ["validation"]

  - stage-name: "counterparty-enrichment"
    execution-order: 2          # Same order
    depends-on: ["validation"]  # Same dependencies
```

### Circular Dependency Detection

APEX automatically detects circular dependencies using DFS (Depth-First Search) algorithm:

**Detected patterns:**
- Self-referencing: `stage-a` → `stage-a`
- Two-stage cycles: `stage-a` → `stage-b` → `stage-a`
- Three-stage cycles: `stage-a` → `stage-b` → `stage-c` → `stage-a`
- Complex multi-path cycles

**Error message:**
```
ERROR: Circular dependency detected involving stage: stage-a
Dependency chain: stage-a → stage-b → stage-c → stage-a
```

---

## 8. Failure Policies

### Overview

Failure policies determine what happens when a stage encounters an error. Each stage can have its own failure policy.

### Policy Types

| Policy | Behavior | When to Use |
|--------|----------|-------------|
| `terminate` | Stop processing immediately | Critical validations, data integrity checks |
| `continue-with-warnings` | Log warnings and continue | Optional enrichments, best-effort processing |
| `flag-for-review` | Mark for manual review and continue | Risk management, compliance checks |

### Policy Details

#### 1. terminate

**Behavior:**
- Stops scenario execution immediately
- Marks all remaining stages as skipped
- Returns error result to caller
- Logs error message

**When to use:**
- Mandatory field validation
- Data integrity checks
- Security validations
- Regulatory compliance rules
- Critical business logic

**Example:**
```yaml
- stage-name: "validation"
  config-file: "config/validation-rules.yaml"
  failure-policy: "terminate"  # MUST pass
  stage-metadata:
    description: "Critical validation - must pass"
```

#### 2. continue-with-warnings

**Behavior:**
- Logs warnings but continues to next stage
- Marks stage as completed with warnings
- Allows dependent stages to execute
- Warnings included in final result

**When to use:**
- Market data enrichment (may be temporarily unavailable)
- Optional calculations
- Non-critical data augmentation
- Best-effort processing

**Example:**
```yaml
- stage-name: "market-data-enrichment"
  config-file: "config/market-data-enrichment.yaml"
  failure-policy: "continue-with-warnings"  # Optional
  stage-metadata:
    description: "Enrich with market data (optional)"
```

#### 3. flag-for-review

**Behavior:**
- Flags scenario for manual review
- Continues processing remaining stages
- Adds review flag to result
- Logs reason for review

**When to use:**
- Risk management rules
- Complex business validations
- Unusual trade patterns
- Threshold breaches requiring judgment
- Compliance checks needing human review

**Example:**
```yaml
- stage-name: "risk-assessment"
  config-file: "config/risk-rules.yaml"
  failure-policy: "flag-for-review"
  stage-metadata:
    description: "Assess risk and flag if needed"
```

### Policy Comparison

| Aspect | terminate | continue-with-warnings | flag-for-review |
|--------|-----------|------------------------|-----------------|
| **Stops processing?** | ✅ Yes | ❌ No | ❌ No |
| **Logs error?** | ✅ Yes (ERROR) | ✅ Yes (WARN) | ✅ Yes (INFO) |
| **Flags for review?** | ❌ No | ❌ No | ✅ Yes |
| **Dependent stages execute?** | ❌ No (skipped) | ✅ Yes | ✅ Yes |
| **Final result status** | FAILED | SUCCESS (with warnings) | SUCCESS (flagged) |

### Mixed Policy Example

```yaml
processing-stages:
  # Critical validation - must pass
  - stage-name: "basic-validation"
    failure-policy: "terminate"
    stage-metadata:
      description: "Critical fields validation"

  # Optional enrichment - best effort
  - stage-name: "market-data"
    depends-on: ["basic-validation"]
    failure-policy: "continue-with-warnings"
    stage-metadata:
      description: "Market data enrichment (optional)"

  # Risk check - needs review if issues found
  - stage-name: "risk-assessment"
    depends-on: ["basic-validation"]
    failure-policy: "flag-for-review"
    stage-metadata:
      description: "Risk assessment and thresholds"
```

---

## 9. Scenario Registry

### Overview

The scenario registry is a central catalog that lists all available scenarios and their configuration files.

### Registry Structure

```yaml
metadata:
  id: "scenario-registry"
  name: "Scenario Registry"
  type: "scenario-registry"
  version: "1.0.0"
  description: "Central registry of all data processing scenarios"
  created-by: "platform-team@example.com"

scenarios:
  - scenario-id: "otc-option-us"
    config-file: "scenarios/otc-option-us.yaml"
    business-domain: "Derivatives Trading"
    owner: "derivatives-team@example.com"
    risk-category: "High"
    enabled: true

  - scenario-id: "bond-us"
    config-file: "scenarios/bond-us.yaml"
    business-domain: "Fixed Income"
    owner: "fixed-income-team@example.com"
    risk-category: "Medium"
    enabled: true

routing:
  strategy: "classification-based"
  default-scenario: "generic-trade-processing"
```

### Registry Fields

| Field | Required | Description |
|-------|----------|-------------|
| `scenario-id` | Required | Unique identifier for the scenario |
| `config-file` | Required | Path to scenario configuration file |
| `business-domain` | Recommended | Business area (e.g., "Trading") |
| `owner` | Recommended | Team/person email responsible |
| `risk-category` | Optional | Low, Medium, High, Critical |
| `enabled` | Optional | Whether scenario is active (default: true) |
| `tags` | Optional | Tags for categorization |

### Routing Configuration

```yaml
routing:
  strategy: "classification-based"      # How scenarios are selected
  default-scenario: "generic-fallback"  # Fallback if no match found
  enable-caching: true                  # Cache scenario configs
  cache-ttl-seconds: 3600              # Cache time-to-live
```

### Using the Registry

```java
// Load scenarios from registry
RulesEngine engine = RulesEngine.fromScenarioRegistry("config/scenario-registry.yaml");

// Process data - automatic scenario selection
Map<String, Object> data = Map.of(
    "tradeType", "OTCOption",
    "region", "US"
);

RuleResult result = engine.evaluateScenario(data);

// Cleanup
engine.shutdown();
```

---

## 10. Best Practices

### Naming Conventions

✅ **Scenario IDs:**
- Use lowercase with hyphens: `otc-option-us-processing`
- Include domain and purpose: `derivatives-otc-validation`
- Be descriptive: `large-trade-enhanced-compliance`

✅ **Stage Names:**
- Use action-oriented names: `validation`, `enrichment`, `calculation`
- Be specific: `market-data-enrichment` not just `enrichment`
- Use consistent naming across scenarios

✅ **Configuration Files:**
- Group by type: `scenarios/`, `config/`, `rules/`
- Use descriptive names: `otc-validation-rules.yaml`
- Match scenario names: `otc-option-us-scenario.yaml` → `otc-option-validation.yaml`

### Organization

✅ **Directory Structure:**
```
project/
├── config/
│   ├── scenario-registry.yaml
│   ├── otc-validation-rules.yaml
│   └── market-data-enrichment.yaml
├── scenarios/
│   ├── otc-option-us.yaml
│   ├── otc-option-eu.yaml
│   └── bond-processing.yaml
└── docs/
    └── scenario-documentation.md
```

### Metadata Best Practices

✅ **Always include:**
- `id`, `name`, `type`, `version` - Required
- `description` - What the scenario does
- `business-domain` - What business area
- `owner` - Who maintains it

✅ **Consider including:**
- `tags` - For categorization and search
- `criticality` - For monitoring priorities
- `sla-ms` - For performance tracking
- `risk-category` - For risk management

### Performance Optimization

✅ **Execution order:**
- Put fast validations before slow ones (fail fast)
- Group related operations together
- Use parallel execution when possible (same dependencies)

✅ **Stage design:**
- Keep stages focused (single responsibility)
- Avoid unnecessary data loading
- Use caching for reference data

✅ **Classification rules:**
- Make rules efficient (avoid complex computations)
- Put most common scenarios first in registry
- Use specific conditions to avoid unnecessary evaluations

### Testing Strategies

✅ **Test scenarios:**
- Test each stage independently first
- Test full scenario end-to-end
- Test all failure paths and policies
- Test with missing/invalid data
- Test dependency chains
- Test circular dependency detection
- Test conditional execution with different data values

✅ **Test classification rules:**
- Test rule matching with various data
- Test null/missing field handling
- Test boundary conditions
- Test fallback behavior (no match found)

✅ **Test conditional stages:**
- Test stages execute when condition is true
- Test stages skip when condition is false
- Test condition evaluation errors (safe skip)
- Test null/missing fields in conditions
- Test multiple conditional stages with different data

---

## 11. Common Patterns

### Pattern 1: Simple Validation Pipeline

Single-stage validation with terminate on failure:

```yaml
scenario:
  scenario-id: "trade-validation"
  
  classification-rule:
    condition: "#'dataType'] == 'trade'"
    description: "All trade data"

  processing-stages:
    - stage-name: "validation"
      config-file: "config/trade-validation-rules.yaml"
      execution-order: 1
      failure-policy: "terminate"
```

### Pattern 2: Validate-Enrich-Validate

Common pattern: validate → enrich → validate enriched data:

```yaml
processing-stages:
  # Stage 1: Basic validation
  - stage-name: "basic-validation"
    config-file: "config/basic-validation.yaml"
    execution-order: 1
    failure-policy: "terminate"

  # Stage 2: Enrich with external data
  - stage-name: "enrichment"
    config-file: "config/market-data-enrichment.yaml"
    execution-order: 2
    depends-on: ["basic-validation"]
    failure-policy: "continue-with-warnings"

  # Stage 3: Validate enriched data
  - stage-name: "enriched-validation"
    config-file: "config/enriched-validation.yaml"
    execution-order: 3
    depends-on: ["enrichment"]
    failure-policy: "terminate"
```

### Pattern 3: Conditional Region-Specific Processing

Use conditions for region-specific compliance stages:

```yaml
processing-stages:
  # Stage 1: Base validation (always runs)
  - stage-name: "base-validation"
    config-file: "config/base-validation.yaml"
    execution-order: 1
    failure-policy: "terminate"

  # Stage 2: US-specific compliance (conditional)
  - stage-name: "us-compliance"
    config-file: "config/us-compliance.yaml"
    execution-order: 2
    condition: "#'region'] == 'US'"
    failure-policy: "terminate"
    depends-on: ["base-validation"]

  # Stage 3: EMEA-specific compliance (conditional)
  - stage-name: "emea-compliance"
    config-file: "config/emea-compliance.yaml"
    execution-order: 3
    condition: "#'region'] == 'EMEA'"
    failure-policy: "terminate"
    depends-on: ["base-validation"]

  # Stage 4: High-value checks (conditional)
  - stage-name: "high-value-validation"
    config-file: "config/high-value.yaml"
    execution-order: 4
    condition: "#'notionalAmount'] > 10000000"
    failure-policy: "flag-for-review"
    depends-on: ["base-validation"]
```

**Execution examples:**
- US trade with $5M → base-validation, us-compliance
- EMEA trade with $15M → base-validation, emea-compliance, high-value-validation
- APAC trade with $2M → base-validation only

### Pattern 4: Parallel Independent Stages

Multiple stages that don't depend on each other:

```yaml
processing-stages:
  # Common prerequisite
  - stage-name: "validation"
    execution-order: 1
    failure-policy: "terminate"

  # These run in parallel (same order, same dependencies)
  - stage-name: "market-data-enrichment"
    execution-order: 2
    depends-on: ["validation"]
    failure-policy: "continue-with-warnings"

  - stage-name: "counterparty-enrichment"
    execution-order: 2
    depends-on: ["validation"]
    failure-policy: "continue-with-warnings"

  - stage-name: "reference-data-enrichment"
    execution-order: 2
    depends-on: ["validation"]
    failure-policy: "continue-with-warnings"
```

### Pattern 5: Progressive Validation

Multiple validation stages with increasing strictness:

```yaml
processing-stages:
  # Stage 1: Basic structural validation
  - stage-name: "structural-validation"
    execution-order: 1
    failure-policy: "terminate"
    stage-metadata:
      description: "Check required fields exist"

  # Stage 2: Business rule validation
  - stage-name: "business-validation"
    execution-order: 2
    depends-on: ["structural-validation"]
    failure-policy: "terminate"
    stage-metadata:
      description: "Apply business rules"

  # Stage 3: Compliance validation
  - stage-name: "compliance-validation"
    execution-order: 3
    depends-on: ["business-validation"]
    failure-policy: "flag-for-review"
    stage-metadata:
      description: "Check regulatory compliance"
```

### Pattern 6: Multi-Source Aggregation

Gather data from multiple sources, then process:

```yaml
processing-stages:
  # Stage 1: Validation
  - stage-name: "validation"
    execution-order: 1
    failure-policy: "terminate"

  # Stage 2-4: Parallel enrichment from different sources
  - stage-name: "market-data"
    execution-order: 2
    depends-on: ["validation"]
    failure-policy: "continue-with-warnings"

  - stage-name: "reference-data"
    execution-order: 2
    depends-on: ["validation"]
    failure-policy: "continue-with-warnings"

  - stage-name: "historical-data"
    execution-order: 2
    depends-on: ["validation"]
    failure-policy: "continue-with-warnings"

  # Stage 5: Process aggregated data
  - stage-name: "calculation"
    execution-order: 3
    depends-on: ["market-data", "reference-data", "historical-data"]
    failure-policy: "terminate"
```

---

## 12. Troubleshooting

### Common Issues

#### Issue 1: "No scenario found for data"

**Error:**
```
ERROR: No scenario found matching data: {tradeType=OTCOption, region=US}
```

**Solutions:**
1. Check classification rules in scenarios
2. Verify field names match exactly (case-sensitive)
3. Add logging to see what data is being evaluated
4. Test classification rule directly:
```java
ScenarioConfiguration scenario = ...; // load scenario
boolean matches = scenario.matchesClassificationRule(testData);
System.out.println("Matches: " + matches);
```

#### Issue 2: "Stage skipped due to failed dependencies"

**Problem:** Stages are skipped unexpectedly

**Solutions:**
1. Check logs for which dependencies failed:
```
INFO: Stage 'enrichment' skipped: dependency 'validation' failed
```
2. Review failure policies of prerequisite stages
3. Verify dependencies are spelled correctly
4. Check execution order is logical

#### Issue 3: "Circular dependency detected"

**Error:**
```
ERROR: Circular dependency detected involving stage: stage-a
Dependency chain: stage-a → stage-b → stage-c → stage-a
```

**Solutions:**
1. Review dependency chain in error message
2. Break the circular reference by restructuring stages
3. Consider if all dependencies are truly necessary
4. Use execution-order without depends-on if sequence is simple

#### Issue 4: "Stage execution timeout"

**Problem:** Stage takes too long and times out

**Solutions:**
1. Check SLA settings in stage-metadata
2. Optimize rule configuration files
3. Use parallel execution for independent stages
4. Consider breaking large stages into smaller ones

#### Issue 5: "Classification rule syntax error"

**Error:**
```
ERROR: Failed to evaluate classification rule: SpelEvaluationException
```

**Solutions:**
1. Verify SpEL syntax is correct
2. Check for typos in field names
3. Test rule in isolation:
```java
ExpressionParser parser = new SpelExpressionParser();
StandardEvaluationContext context = new StandardEvaluationContext();
context.setVariable("data", testData);
Object result = parser.parseExpression(ruleCondition).getValue(context);
```

#### Issue 6: "Stage not executing with valid data"

**Problem:** Stage has a condition but doesn't execute even when condition should be true

**Solutions:**
1. Check logs for condition evaluation:
```
INFO: Stage 'us-compliance' condition not met - skipping: #'region'] == 'US'
```
2. Verify field names match exactly (case-sensitive)
3. Check data types (string vs number)
4. Test condition in isolation
5. Add debug logging to see actual data values

#### Issue 7: "Condition evaluation failed"

**Problem:** Stage skipped due to condition evaluation error

**Solutions:**
1. Check for null values in condition:
```yaml
# Bad (fails if region is null)
condition: "#'region'] == 'US'"

# Good (handles null)
condition: "#'region'] != null && #'region'] == 'US'"
```
2. Verify field exists in data
3. Check data types match condition expectations
4. Use safe navigation operator if supported

### Debugging Tips

**Enable debug logging:**
```properties
logging.level.dev.mars.apex.core.service.scenario=DEBUG
logging.level.dev.mars.apex.core.engine=DEBUG
```

**Check scenario loading:**
```
DEBUG - Loading scenario registry: config/scenario-registry.yaml
DEBUG - Loaded 5 scenarios from registry
DEBUG - Scenario 'otc-option-us' classification rule: #'tradeType'] == 'OTCOption'
```

**Monitor stage execution:**
```
INFO  - Executing stage 'validation' (order: 1)
DEBUG - Stage 'validation' completed in 45ms
INFO  - Executing stage 'enrichment' (order: 2)
WARN  - Stage 'enrichment' failed: Market data unavailable
DEBUG - Applying failure policy: continue-with-warnings
INFO  - Stage 'enrichment' completed with warnings in 120ms
```

**Verify dependencies:**
```
DEBUG - Checking dependencies for stage 'compliance'
DEBUG - Dependency 'validation' status: SUCCESS
DEBUG - Dependency 'enrichment' status: SUCCESS_WITH_WARNINGS
INFO  - All dependencies satisfied for stage 'compliance'
```

---

## 13. Migration Guide

### Migrating from DataTypeScenarioService to RulesEngine

APEX 3.0 introduces a unified API through `RulesEngine`. Here's how to migrate:

#### Old API (Deprecated)

```java
// ⚠️ DEPRECATED - Do not use in new code
DataTypeScenarioService service = new DataTypeScenarioService();
service.loadScenarios("config/scenario-registry.yaml");

Map<String, Object> data = Map.of(
    "tradeType", "OTCOption",
    "region", "US"
);

ScenarioExecutionResult result = service.processMapData(data);

if (result.isSuccessful()) {
    System.out.println("Scenario: " + result.getScenarioId());
    
    for (StageExecutionResult stage : result.getStageResults()) {
        System.out.println("Stage: " + stage.getStageName());
    }
} else if (result.isTerminated()) {
    System.err.println("Terminated: " + result.getWarnings());
} else if (result.requiresReview()) {
    System.out.println("Review: " + result.getReviewFlags());
}
```

#### New API (Version 3.0)

**One-line usage (for single evaluation):**
```java
// ⭐ SIMPLEST - One line
RuleResult result = RulesEngine.fromScenarioRegistry("config/scenario-registry.yaml")
    .evaluateScenario(data);

if (result.isSuccess()) {
    System.out.println("Success: " + result.getMessage());
} else {
    System.err.println("Failed: " + result.getFailureMessages());
}
```

**Reusable usage (for multiple evaluations):**
```java
// ✅ REUSABLE - Create once, use many times
RulesEngine engine = RulesEngine.fromScenarioRegistry("config/scenario-registry.yaml");

// Process multiple data items
for (Map<String, Object> data : dataList) {
    RuleResult result = engine.evaluateScenario(data);
    
    if (result.isSuccess()) {
        System.out.println("Success: " + result.getMessage());
    } else {
        System.err.println("Failed: " + result.getFailureMessages());
    }
}

// Cleanup
engine.shutdown();
```

### Migration Checklist

✅ **Step 1: Update imports**
```java
// Remove old import
// import dev.mars.apex.core.service.scenario.DataTypeScenarioService;
// import dev.mars.apex.core.service.scenario.ScenarioExecutionResult;

// Add new import
import dev.mars.apex.core.engine.RulesEngine;
import dev.mars.apex.core.model.RuleResult;
```

✅ **Step 2: Replace service initialization**
```java
// OLD
DataTypeScenarioService service = new DataTypeScenarioService();
service.loadScenarios("config/scenario-registry.yaml");

// NEW
RulesEngine engine = RulesEngine.fromScenarioRegistry("config/scenario-registry.yaml");
```

✅ **Step 3: Update processing calls**
```java
// OLD
ScenarioExecutionResult result = service.processMapData(data);

// NEW
RuleResult result = engine.evaluateScenario(data);
```

✅ **Step 4: Update result handling**
```java
// OLD
if (result.isSuccessful()) { ... }
if (result.isTerminated()) { ... }
if (result.requiresReview()) { ... }

// NEW
if (result.isSuccess()) { ... }
// RuleResult has unified success/failure handling
```

✅ **Step 5: Add cleanup**
```java
// NEW - Add cleanup when done
engine.shutdown();
```

### Testing Your Migration

1. **Test with existing data** - Verify same results
2. **Check error handling** - Ensure failures are caught
3. **Verify performance** - Should be same or better
4. **Review logs** - Check for deprecation warnings

---

## Summary

APEX Scenarios provide a powerful, flexible way to route and process data through multi-stage pipelines. By following the patterns and best practices in this guide, you can:

✅ Create robust scenario configurations  
✅ Write effective classification rules  
✅ Design dependency-aware processing stages  
✅ Handle failures appropriately  
✅ Build maintainable, testable workflows  

For more information, see:
- [APEX YAML Reference Guide](APEX_YAML_REFERENCE.md) - Complete YAML syntax reference
- [APEX Scenario Design Specification](design/APEX_SCENARIO_MASTER.md) - Technical design details
- [APEX Rules Engine User Guide](APEX_RULES_ENGINE_USER_GUIDE.md) - Core rules engine guide

---

**Document Version:** 3.0  
**Last Updated:** 2025-11-19  
**For Support:** Contact APEX team or raise an issue in the repository
