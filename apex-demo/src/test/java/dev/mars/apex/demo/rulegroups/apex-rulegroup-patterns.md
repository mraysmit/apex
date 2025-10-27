# APEX Rule Group Usage Patterns

**Date:** 2025-01-24  
**Module:** apex-demo/rulegroups  
**Purpose:** Comprehensive documentation of rule group patterns demonstrated in APEX

---

## Overview

This document describes the various rule group usage patterns demonstrated in the APEX demo module. Rule groups provide powerful ways to organize, compose, and execute collections of rules with different logical operators and behaviors.

## 🔧 Rule Group Usage Patterns

### 1. Basic AND/OR Logic Patterns

**File:** `BasicYamlRuleGroupProcessingTest-combined-config.yaml`

```yaml
rule-groups:
  # AND group with all true rules
  - id: "separate-and-group"
    name: "Separate AND Group"
    operator: "AND"
    stop-on-first-failure: false
    rule-ids:
      - "separate-rule-1"  # true
      - "separate-rule-3"  # true
  
  # OR group with mixed rules
  - id: "separate-or-group"
    name: "Separate OR Group"
    operator: "OR"
    rule-ids:
      - "separate-rule-1"  # true
      - "separate-rule-2"  # false
      - "separate-rule-3"  # true
```

**Key Features:**
- **AND Logic**: All rules must pass for group to pass
- **OR Logic**: Any rule passing makes group pass
- **Mixed Results**: Demonstrates different outcomes based on operator

**Test Class:** `BasicYamlRuleGroupProcessingTest.java`

### 2. Stop-On-First-Failure Pattern

**Files:** `StopOnFirstFailureAndGroupTest.java`, `StopOnFirstFailureOrGroupTest.java`

```yaml
rule-groups:
  - id: "and-stop-first-false"
    name: "AND Stop First False"
    operator: "AND"
    stop-on-first-failure: true  # ← Short-circuit evaluation
    rule-ids:
      - "rule1"  # true
      - "rule2"  # false ← Stops here
      - "rule3"  # true (not executed)
```

**Behavior Patterns:**
- **AND Groups**: Stop on first `false` rule (short-circuit)
- **OR Groups**: Stop on first `true` rule (short-circuit)
- **Performance**: Reduces unnecessary rule evaluations
- **Control**: Can be enabled/disabled per rule group

**Benefits:**
- Improved performance for large rule sets
- Early failure detection
- Configurable per rule group

### 3. Cross-File Rule Group References

**Files:** `CrossFileBaseRuleGroups.yaml`, `CrossFileCompositeRuleGroups.yaml`

**Base File (CrossFileBaseRuleGroups.yaml):**
```yaml
rule-groups:
  - id: base_validation
    name: Base Validation (cross-file)
    operator: AND
    rule-ids: [ age-validation, email-validation ]
```

**Composite File (CrossFileCompositeRuleGroups.yaml):**
```yaml
rule-groups:
  - id: cf_composite
    name: Composite (income + base_validation from other file)
    operator: AND
    rule-ids: [ income-validation ]
    rule-group-references: [ base_validation ]  # ← References group from another file
```

**Cross-File Pattern Benefits:**
- **Modular Design**: Rules and rule groups in separate files
- **Automatic Resolution**: APEX resolves cross-file references
- **Composition**: Combine local rules with external rule groups
- **Maintainability**: Promotes reusable rule group libraries

**Test Class:** `CrossFileRuleGroupReferenceTest.java`

### 4. Inline Rule Group References

**File:** `SimpleInlineRuleGroupTest-rules.yaml`

```yaml
rule-groups:
  # Base rule group
  - id: "base-validation"
    name: "Base Validation Group"
    operator: "AND"
    rule-ids:
      - "simple-rule-1"
      - "simple-rule-2"

  # Composite group referencing base group in same file
  - id: "composite-validation"
    name: "Composite Validation Group"
    operator: "OR"
    rule-group-references:
      - "base-validation"  # ← Inline reference within same file
```

**Inline Pattern Features:**
- **Same-File References**: Rule groups can reference other groups in same file
- **Hierarchical Structure**: Build complex validation hierarchies
- **Rule Inheritance**: Composite groups inherit rules from referenced groups

**Test Class:** `SimpleInlineRuleGroupTest.java`

### 5. Severity Aggregation Pattern

**File:** `RuleGroupSeverityAggregationTest.java`

```yaml
rules:
  - id: "error-rule"
    condition: "false"
    severity: "ERROR"
  - id: "warning-rule"
    condition: "true"
    severity: "WARNING"

rule-groups:
  - id: "and-mixed-group"
    operator: "AND"
    rule-ids: ["error-rule", "warning-rule"]
    # Result severity: "ERROR" (highest from failed rules)
```

**Severity Logic:**
- **AND Groups**: Use highest severity of failed rules, or highest of all if all pass
- **OR Groups**: Use severity of first matching rule
- **Empty Groups**: Default behavior (typically do not pass)
- **Automatic Aggregation**: No explicit severity needed on rule groups

### 6. Multi-File Loading Pattern

**Implementation Example:**
```java
// Load multiple files with automatic rule reference resolution
String rulesPath = "BasicYamlRuleGroupProcessingTest-rules.yaml";
String ruleGroupsPath = "BasicYamlRuleGroupProcessingTest-rule-groups.yaml";

RulesEngine engine = rulesEngineService.createRulesEngineFromMultipleFiles(
    rulesPath,
    ruleGroupsPath
);
```

**Multi-File Benefits:**
- **Separation of Concerns**: Rules separate from rule groups
- **Team Collaboration**: Different teams can maintain different files
- **Modularity**: Easy to add/remove rule files
- **Automatic Merging**: APEX handles file merging transparently

## 📊 Pattern Summary Table

| Pattern | Use Case | Key Features | Files Demonstrated |
|---------|----------|--------------|-------------------|
| **Basic AND/OR** | Simple logical grouping | AND/OR operators, mixed results | `BasicYamlRuleGroupProcessingTest` |
| **Stop-On-First-Failure** | Performance optimization | Short-circuit evaluation | `StopOnFirstFailureAndGroupTest`, `StopOnFirstFailureOrGroupTest` |
| **Cross-File References** | Modular architecture | `rule-group-references` across files | `CrossFileRuleGroupReferenceTest` |
| **Inline References** | Same-file composition | `rule-group-references` within file | `SimpleInlineRuleGroupTest` |
| **Severity Aggregation** | Error handling | Automatic severity calculation | `RuleGroupSeverityAggregationTest` |
| **Multi-File Loading** | Enterprise organization | Separate rules/groups files | Multiple test classes |

## 🎯 Best Practices

### 1. Hierarchical Design
Use rule groups to build validation hierarchies from simple to complex validations.

### 2. Performance Optimization
Enable `stop-on-first-failure` for efficiency when appropriate:
- AND groups: Stop on first failure
- OR groups: Stop on first success

### 3. Modular Organization
Separate rules from rule groups for better maintainability and team collaboration.

### 4. Cross-File Composition
Reference rule groups across files for reusability and modular design.

### 5. Severity Management
Let APEX aggregate severities automatically rather than manually specifying group severities.

### 6. Testing Strategy
Test both individual groups and composite workflows to ensure proper behavior.

## 🔍 Key APEX Features Demonstrated

### Rule Group Operators
- `AND`: All rules must pass
- `OR`: Any rule can pass

### Control Features
- `stop-on-first-failure`: Short-circuit evaluation
- `priority`: Rule execution order
- `rule-ids`: Direct rule references
- `rule-group-references`: Group composition

### File Organization
- Single-file configurations
- Multi-file configurations with automatic merging
- Cross-file rule group references

### Automatic Features
- Severity aggregation from constituent rules
- Cross-file reference resolution
- Rule inheritance through group references

## 🚀 Running the Examples

```bash
# Run basic rule group tests
mvn test -Dtest=BasicYamlRuleGroupProcessingTest -pl apex-demo

# Run stop-on-first-failure tests
mvn test -Dtest=StopOnFirstFailure*Test -pl apex-demo

# Run cross-file reference tests
mvn test -Dtest=CrossFileRuleGroupReferenceTest -pl apex-demo

# Run all rule group tests
mvn test -Dtest=*RuleGroup*Test -pl apex-demo
```

## 📝 Conclusion

The rule group patterns in APEX provide a comprehensive framework for organizing complex business logic. From simple AND/OR operations to sophisticated cross-file compositions, these patterns enable scalable, maintainable rule management systems suitable for enterprise applications.

The demonstrated patterns show APEX's flexibility in handling everything from basic validations to complex multi-file rule orchestration, making it suitable for both simple applications and large-scale enterprise rule management systems.
