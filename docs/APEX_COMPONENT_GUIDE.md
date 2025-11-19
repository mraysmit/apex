# APEX Component User Guide

**Version:** 2.2.0  
**Date:** 2025-11-19  
**Author:** Mark Andrew Ray-Smith Cityline Ltd  
**Status:** Production Ready

---

## Table of Contents

1. [Introduction](#introduction)
2. [What are Components?](#what-are-components)
3. [Getting Started](#getting-started)
4. [Component Structure](#component-structure)
5. [Execution Order](#execution-order)
6. [Failure Policies](#failure-policies)
7. [Nested Components](#nested-components)
8. [Using Components in Scenarios](#using-components-in-scenarios)
9. [Best Practices](#best-practices)
10. [Common Patterns](#common-patterns)
11. [Troubleshooting](#troubleshooting)
12. [Migration Guide](#migration-guide)

---

## 1. Introduction

APEX Components are a powerful feature that allows you to group multiple YAML configuration files into reusable, logical units. This guide will help you understand how to create, use, and manage components in your APEX applications.

### What You'll Learn
- How to create component files
- How to structure components for maximum reusability
- How to control execution order and failure handling
- How to use components in scenarios
- Best practices and common patterns

### Prerequisites
- Basic understanding of APEX YAML configuration
- Familiarity with APEX scenarios
- Knowledge of APEX rules and enrichments

---

## 2. What are Components?

### Overview

A **component** is a special YAML file (type: `"component"`) that groups multiple configuration files together. Think of it as a container that organizes related rules, enrichments, and other configuration files into a single, reusable unit.

### Key Benefits

| Benefit | Description |
|---------|-------------|
| **Modularity** | Break complex processing into logical, manageable pieces |
| **Reusability** | Share common configurations across multiple scenarios |
| **Maintainability** | Update related configurations in one place |
| **Clarity** | Make scenarios cleaner and more focused on orchestration |
| **Flexibility** | Mix and match components to create different workflows |

### When to Use Components

✅ **Use components when you have:**
- Multiple related configuration files that are always used together
- Common validation or enrichment logic used across scenarios
- Complex processing pipelines that need better organization
- Configurations that are reused in multiple scenarios

❌ **Don't use components when:**
- You only have a single configuration file
- Files are never reused or grouped logically
- Simplicity is more important than reusability

---

## 3. Getting Started

### Creating Your First Component

Let's create a simple component that groups basic trade validation rules.

**Step 1: Create the component file**

```yaml
# components/basic-trade-validation.yaml
metadata:
  id: "basic-trade-validation"
  name: "Basic Trade Validation"
  type: "component"
  version: "1.0.0"
  description: "Common validation rules for all trades"
  business-domain: "Trading"
  owner: "trading-team@example.com"

rule-configurations:
  - file: "rules/trade-id-validation.yaml"
  - file: "rules/amount-validation.yaml"
  - file: "rules/currency-validation.yaml"
```

**Step 2: Create the referenced rule files**

```yaml
# rules/trade-id-validation.yaml
metadata:
  type: "rule-config"
  id: "trade-id-validation"

rules:
  - id: "trade-id-required"
    condition: "tradeId != null && !tradeId.isEmpty()"
    message: "Trade ID is required"
    severity: "ERROR"
```

```yaml
# rules/amount-validation.yaml
metadata:
  type: "rule-config"
  id: "amount-validation"

rules:
  - id: "amount-positive"
    condition: "amount > 0"
    message: "Amount must be positive"
    severity: "ERROR"
```

**Step 3: Use the component in a scenario**

```yaml
# scenarios/trade-processing.yaml
metadata:
  type: "scenario"
  id: "trade-processing"

scenario:
  scenario-id: "trade-processing"
  
  processing-stages:
    - stage-name: "validation"
      config-file: "components/basic-trade-validation.yaml"
      execution-order: 1
      failure-policy: "terminate"
```

**Step 4: Run your scenario**

```java
// Load and execute the scenario
ScenarioService scenarioService = new ScenarioService();
Map<String, Object> tradeData = Map.of(
    "tradeId", "TR-12345",
    "amount", 100000.00,
    "currency", "USD"
);

scenarioService.executeScenario("trade-processing", tradeData);
```

---

## 4. Component Structure

### Basic Structure

Every component file must have:
1. **Metadata section** with `type: "component"`
2. **At least one reference section** (rule-configurations, enrichment-refs, component-refs, or config-files)

```yaml
metadata:
  id: "my-component"           # Required: Unique identifier
  name: "My Component"         # Required: Human-readable name
  type: "component"            # Required: Must be "component"
  version: "1.0.0"            # Required: Version number
  description: "..."           # Optional but recommended
  business-domain: "..."       # Optional
  owner: "..."                 # Optional
  criticality: "high"          # Optional: low, medium, high, critical
  sla-ms: 5000                # Optional: SLA in milliseconds
  tags: ["validation", "otc"] # Optional: Tags for categorization

# Reference sections (at least one required)
rule-configurations:          # References to rule config files
  - file: "path/to/rules.yaml"

enrichment-refs:             # References to enrichment files
  - file: "path/to/enrichment.yaml"

component-refs:              # References to other components
  - file: "path/to/another-component.yaml"

config-files:                # References to other config files
  - file: "path/to/data-sources.yaml"
```

### File Reference Options

Each file reference can specify:

```yaml
rule-configurations:
  - file: "path/to/rules.yaml"           # Required: Path to file
    execution-order: 1                    # Optional: Explicit order
    failure-policy: "terminate"           # Optional: Override default
```

**Field Descriptions:**

- **file** (Required): Path to the configuration file
  - Can be relative to the component file
  - Can be absolute path starting with "src/"
  
- **execution-order** (Optional): Integer controlling execution sequence
  - If not specified, uses document order (position in YAML)
  - Lower numbers execute first
  - Allows gaps (1, 10, 20, etc.)
  
- **failure-policy** (Optional): Override stage-level policy
  - Valid values: `"terminate"`, `"continue-with-warnings"`, `"flag-for-review"`
  - If not specified, inherits from scenario stage

---

## 5. Execution Order

### Document Order (Default)

By default, files execute in the order they appear in the YAML file (document order).

```yaml
# Files execute in order: 1, 2, 3
rule-configurations:
  - file: "rules/basic-validation.yaml"      # Executes first
  - file: "rules/advanced-validation.yaml"   # Executes second
  - file: "rules/compliance-rules.yaml"      # Executes third
```

### Explicit Execution Order

For precise control, use `execution-order`:

```yaml
# Files execute in order: 1, 10, 20
rule-configurations:
  - file: "rules/compliance-rules.yaml"
    execution-order: 20                      # Executes last
  - file: "rules/basic-validation.yaml"
    execution-order: 1                       # Executes first
  - file: "rules/advanced-validation.yaml"
    execution-order: 10                      # Executes second
```

### Mixed Mode

You can mix explicit orders with document order:

```yaml
enrichment-refs:
  # This has explicit order - runs first
  - file: "enrichments/critical-enrichment.yaml"
    execution-order: 1
  
  # These use document order - run in sequence
  - file: "enrichments/standard-enrichment-1.yaml"
  - file: "enrichments/standard-enrichment-2.yaml"
  
  # This has explicit order - runs last
  - file: "enrichments/final-enrichment.yaml"
    execution-order: 100
```

**Execution sequence:**
1. critical-enrichment.yaml (order: 1)
2. standard-enrichment-1.yaml (document order)
3. standard-enrichment-2.yaml (document order)
4. final-enrichment.yaml (order: 100)

### Best Practices for Execution Order

✅ **Use document order when:**
- Files have a natural sequential relationship
- Order is obvious from the file names
- You want to keep the configuration simple

✅ **Use explicit order when:**
- Files from different sections need to interleave
- Order is not obvious from file position
- You need gaps for future insertions

---

## 6. Failure Policies

### Overview

Failure policies control what happens when a configuration file encounters an error during execution.

### Policy Types

| Policy | Behavior | When to Use |
|--------|----------|-------------|
| `terminate` | Stop processing immediately | Critical validations that must pass |
| `continue-with-warnings` | Log warning and continue | Optional checks, best-effort processing |
| `flag-for-review` | Mark for manual review | Issues requiring human judgment |

### Stage-Level Policy (Default)

The scenario stage defines the default failure policy:

```yaml
processing-stages:
  - stage-name: "validation"
    config-file: "components/validation-component.yaml"
    failure-policy: "terminate"  # Default for all files in component
```

### File-Level Policy (Override)

Individual files can override the stage-level policy:

```yaml
# components/validation-component.yaml
rule-configurations:
  - file: "rules/critical-validation.yaml"
    failure-policy: "terminate"              # Must pass

  - file: "rules/warning-validation.yaml"
    failure-policy: "continue-with-warnings" # Optional

  - file: "rules/review-validation.yaml"
    failure-policy: "flag-for-review"        # Needs review
```

### Policy Inheritance

```
Scenario Stage Policy
        ↓
Component File Policy (overrides if specified)
        ↓
Actual Execution Policy
```

### Example: Mixed Policies

```yaml
metadata:
  id: "mixed-validation-component"
  type: "component"

rule-configurations:
  # Critical validations - must pass
  - file: "rules/trade-id-validation.yaml"
    failure-policy: "terminate"
  
  - file: "rules/counterparty-validation.yaml"
    failure-policy: "terminate"
  
  # Nice-to-have validations - warn but continue
  - file: "rules/optional-fields-validation.yaml"
    failure-policy: "continue-with-warnings"
  
  - file: "rules/data-quality-checks.yaml"
    failure-policy: "continue-with-warnings"
```

---

## 7. Nested Components

### Overview

Components can reference other components, creating a hierarchical structure. This enables powerful composition patterns.

### Nesting Depth Limits

| Depth Level | Status | Warning |
|-------------|--------|---------|
| 1-2 | ✅ Normal | No warnings |
| 3-5 | ⚠️ Warning | Log WARNING message |
| 6+ | ❌ Error | CRITICAL ERROR - fails to load |

### Creating Nested Components

**Level 1 Component (Parent):**

```yaml
# components/comprehensive-otc-processing.yaml
metadata:
  id: "comprehensive-otc-processing"
  type: "component"
  description: "Complete OTC option processing"

# Reference common validation component
component-refs:
  - file: "components/common-trade-validation.yaml"  # Nesting level 2

# Add OTC-specific rules
rule-configurations:
  - file: "rules/otc-option-validation.yaml"
  - file: "rules/greeks-validation.yaml"
```

**Level 2 Component (Child):**

```yaml
# components/common-trade-validation.yaml
metadata:
  id: "common-trade-validation"
  type: "component"
  description: "Common validation for all trades"

rule-configurations:
  - file: "rules/trade-id-validation.yaml"
  - file: "rules/amount-validation.yaml"
  - file: "rules/currency-validation.yaml"
```

### Circular Reference Detection

APEX automatically detects and prevents circular references:

```yaml
# ❌ This will fail:
# Component A references Component B
# Component B references Component A
```

**Error Message:**
```
ERROR: Circular component reference detected:
  comprehensive-validation → common-validation → comprehensive-validation
```

### Best Practices for Nesting

✅ **Do:**
- Keep nesting shallow (1-2 levels preferred)
- Use nesting for logical composition
- Create reusable base components

❌ **Don't:**
- Create deep nesting hierarchies (>3 levels)
- Create circular references
- Nest components just for organization

---

## 8. Using Components in Scenarios

### Basic Usage

Reference a component in a scenario stage using `config-file`:

```yaml
scenario:
  scenario-id: "trade-processing"
  
  processing-stages:
    - stage-name: "validation"
      config-file: "components/validation-component.yaml"
      execution-order: 1
      failure-policy: "terminate"
```

### Multiple Component Stages

```yaml
scenario:
  scenario-id: "full-trade-processing"
  
  processing-stages:
    # Stage 1: Validation
    - stage-name: "validation"
      config-file: "components/comprehensive-validation.yaml"
      execution-order: 1
      failure-policy: "terminate"
    
    # Stage 2: Enrichment
    - stage-name: "enrichment"
      config-file: "components/market-data-enrichment.yaml"
      execution-order: 2
      failure-policy: "continue-with-warnings"
    
    # Stage 3: Risk Calculation
    - stage-name: "risk-calculation"
      config-file: "components/risk-calculation.yaml"
      execution-order: 3
      failure-policy: "terminate"
```

### Mixing Components and Direct Files

You can mix component references with direct file references:

```yaml
processing-stages:
  # Use a component for common validation
  - stage-name: "common-validation"
    config-file: "components/common-validation.yaml"
    execution-order: 1
  
  # Use a direct file for scenario-specific rules
  - stage-name: "scenario-specific-validation"
    config-file: "rules/otc-specific-validation.yaml"
    execution-order: 2
```

### Component Execution Flow

When a scenario executes a component stage:

1. **Load Component** - Parse the component YAML file
2. **Validate** - Check component structure and nesting depth
3. **Resolve References** - Load all referenced files recursively
4. **Detect Circular References** - Ensure no cycles exist
5. **Determine Order** - Sort files by execution-order or document order
6. **Execute Files** - Process each file in sequence
7. **Apply Policies** - Handle failures according to policies
8. **Aggregate Output** - Combine results from all files

---

## 9. Best Practices

### Naming Conventions

✅ **Component Files:**
- Use descriptive names: `validation-component.yaml`, `enrichment-component.yaml`
- Include domain or purpose: `otc-validation-component.yaml`
- Use hyphens, not underscores: `trade-validation.yaml` not `trade_validation.yaml`

✅ **Component IDs:**
- Use lowercase with hyphens: `common-trade-validation`
- Be descriptive: `comprehensive-otc-validation`
- Avoid abbreviations unless very common: `otc` is OK, `cmprhnsv` is not

### Organization

✅ **Directory Structure:**
```
project/
├── components/
│   ├── validation/
│   │   ├── common-validation.yaml
│   │   └── otc-validation.yaml
│   ├── enrichment/
│   │   ├── market-data-enrichment.yaml
│   │   └── counterparty-enrichment.yaml
│   └── risk/
│       └── risk-calculation.yaml
├── rules/
│   ├── trade-id-validation.yaml
│   └── amount-validation.yaml
└── scenarios/
    └── trade-processing.yaml
```

### Metadata Best Practices

✅ **Always Include:**
- `id`, `name`, `type`, `version`, `description` - Required or strongly recommended
- `owner` - Who maintains this component
- `business-domain` - What business area it serves

✅ **Consider Including:**
- `tags` - For categorization and search
- `criticality` - For monitoring and alerting
- `sla-ms` - For performance tracking
- `documentation-url` - Link to detailed docs

### Reusability Guidelines

✅ **Create reusable components for:**
- Common validation patterns used across scenarios
- Standard enrichment workflows
- Shared business logic

✅ **Create scenario-specific components for:**
- Complex multi-file workflows unique to one scenario
- Tightly coupled rules and enrichments
- Temporary or experimental configurations

### Performance Considerations

✅ **Optimize execution order:**
- Put fast validations before slow ones (fail fast)
- Group related operations together
- Avoid unnecessary file loading

✅ **Manage component size:**
- Keep components focused (5-10 files max)
- Break large components into smaller, composable pieces
- Use nesting for logical grouping, not just size reduction

---

## 10. Common Patterns

### Pattern 1: Validation Component

Group all validation rules for a specific domain:

```yaml
metadata:
  id: "otc-validation"
  type: "component"
  description: "Complete OTC option validation"

rule-configurations:
  - file: "rules/basic-trade-validation.yaml"
    failure-policy: "terminate"
  
  - file: "rules/otc-specific-validation.yaml"
    failure-policy: "terminate"
  
  - file: "rules/greeks-validation.yaml"
    failure-policy: "continue-with-warnings"
  
  - file: "rules/data-quality-checks.yaml"
    failure-policy: "continue-with-warnings"
```

### Pattern 2: Multi-Source Enrichment Component

Combine enrichments from multiple data sources:

```yaml
metadata:
  id: "multi-source-enrichment"
  type: "component"
  description: "Enrich from multiple sources"

# Load data sources first
config-files:
  - file: "config/data-sources.yaml"
    execution-order: 1

# Then execute enrichments
enrichment-refs:
  - file: "enrichments/market-data-enrichment.yaml"
    execution-order: 10
  
  - file: "enrichments/counterparty-enrichment.yaml"
    execution-order: 20
  
  - file: "enrichments/reference-data-enrichment.yaml"
    execution-order: 30
```

### Pattern 3: Composition Component

Combine smaller components into a larger workflow:

```yaml
metadata:
  id: "comprehensive-trade-processing"
  type: "component"
  description: "Complete trade processing pipeline"

# Common validation first
component-refs:
  - file: "components/common-validation.yaml"
    execution-order: 1

# Domain-specific validation
rule-configurations:
  - file: "rules/otc-validation.yaml"
    execution-order: 10

# Enrichments
component-refs:
  - file: "components/market-enrichment.yaml"
    execution-order: 20

# Risk calculations
enrichment-refs:
  - file: "enrichments/risk-calculation.yaml"
    execution-order: 30
```

### Pattern 4: Pre-Validation Enrichment

Enrich data needed for validation before running validation rules:

```yaml
metadata:
  id: "validation-with-enrichment"
  type: "component"
  description: "Enrich then validate"

# First: Enrich data needed for validation
enrichment-refs:
  - file: "enrichments/reference-data-enrichment.yaml"
    execution-order: 1

# Then: Validate enriched data
rule-configurations:
  - file: "rules/validation-rules.yaml"
    execution-order: 10
```

### Pattern 5: Reusable Base + Extension

Create a reusable base component and extend it:

```yaml
# Base component (reusable)
# components/base-trade-validation.yaml
metadata:
  id: "base-trade-validation"
  type: "component"

rule-configurations:
  - file: "rules/trade-id-validation.yaml"
  - file: "rules/amount-validation.yaml"
  - file: "rules/currency-validation.yaml"
```

```yaml
# Extended component (specific)
# components/fx-trade-validation.yaml
metadata:
  id: "fx-trade-validation"
  type: "component"

# Reuse base validation
component-refs:
  - file: "components/base-trade-validation.yaml"

# Add FX-specific rules
rule-configurations:
  - file: "rules/fx-currency-pair-validation.yaml"
  - file: "rules/fx-settlement-date-validation.yaml"
```

---

## 11. Troubleshooting

### Common Issues

#### Issue 1: "Component file not found"

**Error:**
```
ERROR: Component file not found: components/my-component.yaml
```

**Solutions:**
- Check file path is correct (relative to project root)
- Verify file exists in the expected location
- Check for typos in filename
- Ensure file has `.yaml` extension (not `.yml`)

#### Issue 2: "Unrecognized property in component file"

**Error:**
```
UnrecognizedPropertyException: Unrecognized field "unknown-field"
```

**Solutions:**
- Check for typos in field names
- Verify you're using valid component fields
- Ensure YAML structure matches component schema
- Remove any custom/unknown fields

#### Issue 3: "Circular reference detected"

**Error:**
```
ERROR: Circular component reference detected:
  component-a → component-b → component-a
```

**Solutions:**
- Review component dependencies
- Break the circular reference by restructuring
- Create a shared base component instead
- Consider if nesting is really necessary

#### Issue 4: "Component nesting depth exceeds limit"

**Error:**
```
CRITICAL ERROR: Component nesting depth 6 exceeds maximum limit of 5
```

**Solutions:**
- Flatten your component hierarchy
- Combine deeply nested components
- Restructure to use fewer nesting levels
- Consider using direct file references instead

#### Issue 5: "Files not executing in expected order"

**Problem:** Files execute in unexpected sequence

**Solutions:**
- Check if you're mixing explicit order with document order
- Verify `execution-order` values are correct
- Remember: lower numbers execute first
- Use document order if sequence is simple

#### Issue 6: "Failure policy not working"

**Problem:** Stage continues when it should terminate (or vice versa)

**Solutions:**
- Check file-level `failure-policy` overrides stage-level
- Verify policy values: `terminate`, `continue-with-warnings`, `flag-for-review`
- Check for typos in policy names
- Review logs to see which policy is being applied

### Debugging Tips

**Enable Debug Logging:**
```yaml
# Add to your logging configuration
logging:
  level:
    dev.mars.apex.core.config.component: DEBUG
    dev.mars.apex.core.service.scenario: DEBUG
```

**Check Component Loading:**
```
DEBUG - Loading component: comprehensive-validation (id: comprehensive-validation)
DEBUG - Component contains 4 file references
INFO  - Processing component file [1/4]: rules/basic-validation.yaml
```

**Verify Execution Order:**
```
DEBUG - Execution order determined: [1, 2, 3, 10]
INFO  - Executing file with order 1: critical-enrichment.yaml
INFO  - Executing file with order 2: standard-enrichment-1.yaml
```

**Monitor Nesting Depth:**
```
DEBUG - Component nesting depth: 1
WARN  - Component nesting depth 3 detected for: nested-component-level3
```

---

## 12. Migration Guide

### Converting Scenarios to Use Components

Follow these steps to migrate existing scenarios to use components.

### Step 1: Identify Grouping Opportunities

Look for:
- Multiple config files that are always used together
- Related rules or enrichments used across scenarios
- Complex stage configurations with many files

**Before:**
```yaml
processing-stages:
  - stage-name: "validation-1"
    config-file: "rules/trade-id-validation.yaml"
    execution-order: 1
  
  - stage-name: "validation-2"
    config-file: "rules/amount-validation.yaml"
    execution-order: 2
  
  - stage-name: "validation-3"
    config-file: "rules/currency-validation.yaml"
    execution-order: 3
```

### Step 2: Create Component File

Group related files into a component:

**After:**
```yaml
# components/basic-validation-component.yaml
metadata:
  id: "basic-validation"
  type: "component"
  description: "Basic trade validation rules"

rule-configurations:
  - file: "rules/trade-id-validation.yaml"
  - file: "rules/amount-validation.yaml"
  - file: "rules/currency-validation.yaml"
```

### Step 3: Update Scenario

Replace multiple stages with single component stage:

**After:**
```yaml
processing-stages:
  - stage-name: "validation"
    config-file: "components/basic-validation-component.yaml"
    execution-order: 1
    failure-policy: "terminate"
```

### Step 4: Test Thoroughly

✅ **Testing checklist:**
- [ ] Component loads successfully
- [ ] All files execute in correct order
- [ ] Failure policies work as expected
- [ ] Results match pre-migration behavior
- [ ] No performance degradation
- [ ] Logs show correct component execution

### Migration Examples

#### Example 1: Simple Consolidation

**Before:**
```yaml
processing-stages:
  - stage-name: "enrich-1"
    config-file: "enrichments/market-data.yaml"
  - stage-name: "enrich-2"
    config-file: "enrichments/counterparty.yaml"
  - stage-name: "enrich-3"
    config-file: "enrichments/pricing.yaml"
```

**After:**
```yaml
# components/multi-source-enrichment.yaml
metadata:
  id: "multi-source-enrichment"
  type: "component"

enrichment-refs:
  - file: "enrichments/market-data.yaml"
  - file: "enrichments/counterparty.yaml"
  - file: "enrichments/pricing.yaml"
```

```yaml
processing-stages:
  - stage-name: "enrichment"
    config-file: "components/multi-source-enrichment.yaml"
```

#### Example 2: Complex Workflow

**Before:**
```yaml
processing-stages:
  - stage-name: "validation-basic"
    config-file: "rules/basic-validation.yaml"
  - stage-name: "enrich-reference"
    config-file: "enrichments/reference-data.yaml"
  - stage-name: "validation-advanced"
    config-file: "rules/advanced-validation.yaml"
  - stage-name: "enrich-market"
    config-file: "enrichments/market-data.yaml"
```

**After:**
```yaml
# components/validation-enrichment-workflow.yaml
metadata:
  id: "validation-enrichment-workflow"
  type: "component"

rule-configurations:
  - file: "rules/basic-validation.yaml"
    execution-order: 1

enrichment-refs:
  - file: "enrichments/reference-data.yaml"
    execution-order: 10

rule-configurations:
  - file: "rules/advanced-validation.yaml"
    execution-order: 20

enrichment-refs:
  - file: "enrichments/market-data.yaml"
    execution-order: 30
```

```yaml
processing-stages:
  - stage-name: "validation-and-enrichment"
    config-file: "components/validation-enrichment-workflow.yaml"
```

### Rollback Procedures

If migration causes issues:

1. **Keep original scenario files** as backups
2. **Test in non-production** environment first
3. **Have rollback plan** ready
4. **Monitor closely** after migration

**Rollback Steps:**
1. Restore original scenario YAML file
2. Restart APEX services
3. Verify original functionality restored
4. Investigate component issues
5. Re-attempt migration with fixes

---

## Summary

APEX Components provide a powerful way to organize, reuse, and maintain your APEX configurations. By following the patterns and best practices in this guide, you can:

✅ Create clean, maintainable component structures  
✅ Leverage reusability across scenarios  
✅ Control execution order and failure handling  
✅ Build complex processing pipelines with composition  
✅ Migrate existing scenarios smoothly  

For more information, see:
- [APEX YAML Reference Guide](APEX_YAML_REFERENCE.md) - Complete YAML syntax reference
- [APEX Component Design Specification](design/APEX_COMPONENT_DESIGN.md) - Technical design details
- [APEX Scenario User Guide](APEX_SCENARIO_USER_GUIDE.md) - Scenario processing guide

---

**Document Version:** 2.2.0  
**Last Updated:** 2025-11-19  
**For Support:** Contact APEX team or raise an issue in the repository
