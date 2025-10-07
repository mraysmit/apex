# Enrichment Stage Simulation in BasicStageConfigurationTest

## Purpose

This document explains why the `BasicStageConfigurationTest-enrichment-rules.yaml` file uses validation rules instead of actual enrichment configuration.

## The Confusion (Now Resolved)

The file naming and content initially appeared contradictory:

| Aspect | Value | Explanation |
|--------|-------|-------------|
| **Filename** | `enrichment-rules.yaml` | Refers to the STAGE name (enrichment stage) |
| **Metadata type** | `"rule-config"` | Correct - contains validation rules |
| **Content section** | `rules:` | Correct - validation rules that simulate enrichment |
| **Stage name** | `"enrichment"` | Correct - this is the enrichment STAGE in the scenario |
| **Actual purpose** | Simulate enrichment using validation rules | Correct - test simplification |

## Why Use Validation Rules to Simulate Enrichment?

### Testing Goals

The `BasicStageConfigurationTest` focuses on demonstrating:
1. **Stage-based execution** - multiple stages executing in order
2. **Stage dependencies** - enrichment depends on validation
3. **Failure policies** - terminate vs continue-with-warnings
4. **Execution order** - stages run in configured sequence

### Why Not Use Real Enrichment?

Real enrichment would require:
- External data sources (databases, APIs, files)
- Complex lookup configurations
- Field mapping logic
- Error handling for external service failures
- Mock services or test databases

This would:
- **Complicate the test** - focus shifts from stage execution to enrichment logic
- **Add dependencies** - external services, test data setup
- **Increase maintenance** - more moving parts to maintain
- **Obscure the lesson** - harder to understand what's being tested

### The Simplified Approach

Instead, we use **validation rules that always pass** to simulate enrichment:

```yaml
rules:
  - id: "processing-timestamp-check"
    name: "Processing Timestamp Check"
    description: "Verify trade has processing context (simulates enrichment)"
    condition: "true"  # Always passes to simulate successful enrichment
    severity: "INFO"
    category: "enrichment"
    message: "Trade processing timestamp verified"
    enabled: true
```

This approach:
- ✅ **Keeps test simple** - no external dependencies
- ✅ **Focuses on stage execution** - demonstrates stage-based processing
- ✅ **Easy to understand** - clear what's being tested
- ✅ **Easy to maintain** - no complex setup required

## File Naming Convention

The filename `BasicStageConfigurationTest-enrichment-rules.yaml` follows this pattern:

```
[TestClassName]-[stage-name]-rules.yaml
```

Where:
- `BasicStageConfigurationTest` = test class name
- `enrichment` = stage name in the scenario
- `rules` = contains rule-config (not actual enrichment config)

## What Real Enrichment Would Look Like

In a production scenario, the enrichment stage would use:

```yaml
metadata:
  id: "trade-enrichment"
  name: "Trade Enrichment"
  version: "1.0.0"
  description: "Actual trade data enrichment"
  type: "enrichment"  # ← Different type
  author: "APEX Demo"

enrichments:  # ← Different section
  - id: "currency-lookup"
    name: "Currency Lookup Enrichment"
    type: "lookup-enrichment"
    condition: "#data.currency != null"
    
    lookup-config:
      lookup-dataset:
        type: "database"
        connection: "currency-db"
        query: "SELECT * FROM currencies WHERE code = ?"
      
      field-mappings:
        - source-field: "currencyName"
          target-field: "currencyFullName"
        - source-field: "region"
          target-field: "currencyRegion"
```

## Summary

The `BasicStageConfigurationTest-enrichment-rules.yaml` file:

1. **Represents the enrichment STAGE** in a multi-stage scenario
2. **Uses validation rules** (type: rule-config) to keep the test simple
3. **Simulates enrichment** without requiring external dependencies
4. **Demonstrates stage-based processing** - the actual test objective
5. **Is clearly documented** to avoid confusion about its purpose

This is a **test simplification pattern** - using simpler constructs to demonstrate complex functionality without unnecessary complexity.

## Related Files

- `BasicStageConfigurationTest.java` - Main test class
- `BasicStageConfigurationTest-scenario.yaml` - Scenario definition with 2 stages
- `BasicStageConfigurationTest-validation-rules.yaml` - Stage 1: actual validation rules
- `BasicStageConfigurationTest-enrichment-rules.yaml` - Stage 2: validation rules simulating enrichment
- `BasicStageConfigurationTest-failing-*.yaml` - Negative test scenarios

## Key Takeaway

When you see `enrichment-rules.yaml` in this test:
- It's the configuration for the **enrichment STAGE**
- It uses **validation rules** (not actual enrichment) for simplicity
- This is a **test pattern** to demonstrate stage-based execution
- Production scenarios would use actual enrichment with `type: "enrichment"`

