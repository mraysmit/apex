# APEX Conditional Logic Demo

This directory demonstrates APEX's powerful conditional processing capabilities, from simple ternary expressions to complex rule-based business logic.

## Overview

APEX provides multiple approaches for conditional logic, each suited for different complexity levels:

1. **Ultra Simple Ternary** - Direct SpEL expressions (simplest)
2. **Rule OR Logic** - Rule groups with conditional enrichments
3. **Rule Result References** - Advanced rule-based conditional processing
4. **Complex Business Logic** - Real-world financial transaction scenarios

## Test Classes & Results

All tests are **✅ PASSING** (24 tests total):

### 1. UltraSimpleTernaryTest (6 tests)
**Simplest approach** - No rules, no rule groups, just pure SpEL ternary logic
- **File**: `ultra-simple-ternary-test.yaml`
- **Pattern**: Single enrichment with nested ternary operators
- **Use Case**: Simple A→FIRST, B→SECOND, C→THIRD mappings

### 2. UltraSimpleRuleOrTest (7 tests)  
**Rule-based approach** - 3 rules + OR group + conditional enrichments
- **File**: `ultra-simple-rule-or-test.yaml`
- **Pattern**: Rules → Rule Group → Conditional Enrichments
- **Use Case**: Same logic as ternary but using rule framework

### 3. RuleResultReferencesTest (4 tests)
**Advanced rule integration** - Rule results drive conditional processing
- **File**: `rule-result-references-demo.yaml`
- **Pattern**: Rules → `#ruleResults['rule-id']` → Conditional Logic
- **Use Case**: Financial transaction processing with rule-driven decisions

### 4. ConditionalMappingDesignV2Test (7 tests)
**Real-world complexity** - FX transaction processing with multiple conditions
- **File**: `conditional-mapping-design-v2-test.yaml`
- **Pattern**: Complex rules → OR groups → System-specific mappings
- **Use Case**: SWIFT system NDF field mapping with fallbacks

## Key APEX Patterns Demonstrated

### Pattern 1: Ultra Simple Ternary
```yaml
# NO RULES! NO RULE GROUPS! Just pure conditional logic
enrichments:
  - field-mappings:
      - source-field: "input"
        target-field: "output"
        transformation: |
          #input == 'A' ? 'FIRST' :
          #input == 'B' ? 'SECOND' :
          #input == 'C' ? 'THIRD' : null
```

### Pattern 2: Rule OR Logic
```yaml
rules:
  - id: "rule-1"
    condition: "#input == 'A'"
  - id: "rule-2"
    condition: "#input == 'B'"

rule-groups:
  - id: "simple-or-group"
    operator: "OR"
    stop-on-first-failure: true
    rule-ids: ["rule-1", "rule-2"]

enrichments:
  - condition: "#ruleResults['rule-1'] == true"
    transformation: "'FIRST'"
  - condition: "#ruleResults['rule-2'] == true"
    transformation: "'SECOND'"
```

### Pattern 3: Rule Result References
```yaml
rules:
  - id: "high-value-rule"
    condition: "#amount > 10000"
  - id: "premium-customer-rule"
    condition: "#customerType == 'PREMIUM'"

enrichments:
  - condition: "#ruleResults['high-value-rule'] == true"
    field-mappings:
      - target-field: "processedAmount"
        transformation: "#amount * 1.05"  # 5% fee
      - target-field: "processingPriority"
        transformation: "'HIGH'"
```

### Pattern 4: Complex Business Logic
```yaml
rules:
  - id: "swift-valid-ndf-rule"
    condition: "({'0', '1'}.contains(#IS_NDF)) && (#SYSTEM_CODE == 'SWIFT')"
  - id: "swift-y-flag-rule"
    condition: "#IS_NDF == 'Y' && #SYSTEM_CODE == 'SWIFT'"
  - id: "translation-required-rule"
    condition: "#IS_NDF != null && #SYSTEM_CODE == 'SWIFT' && !({'0', '1', 'Y'}.contains(#IS_NDF))"

rule-groups:
  - id: "ndf-mapping-group"
    operator: "OR"
    stop-on-first-failure: true
    rule-ids: ["swift-valid-ndf-rule", "swift-y-flag-rule", "translation-required-rule"]

enrichments:
  - condition: "#ruleResults['swift-valid-ndf-rule'] == true"
    field-mappings:
      - source-field: "IS_NDF"
        target-field: "IS_NDF"  # Direct mapping
  - condition: "#ruleResults['swift-y-flag-rule'] == true"
    field-mappings:
      - target-field: "IS_NDF"
        transformation: "'1'"  # Y → 1
  - condition: "#ruleResults['translation-required-rule'] == true"
    field-mappings:
      - target-field: "IS_NDF"
        transformation: "'DEFAULT_TRANSLATION'"
```

## When to Use Each Pattern

### ✅ Use Ternary When:
- Simple value mappings (A→X, B→Y, C→Z)
- No complex business logic needed
- Performance is critical
- Configuration should be minimal

### ✅ Use Rule OR Logic When:
- Need rule documentation and traceability
- Conditions might become more complex
- Want separation between conditions and actions
- Need rule result logging

### ✅ Use Rule Result References When:
- Multiple enrichments depend on same rules
- Complex conditional processing chains
- Need rule group results
- Advanced business logic scenarios

### ✅ Use Complex Business Logic When:
- Real-world financial/business scenarios
- Multiple systems with different behaviors
- Fallback and default handling required
- Regulatory compliance needs

## Running the Tests

```bash
# Run all conditional tests
mvn test -Dtest="UltraSimpleRuleOrTest,UltraSimpleTernaryTest,ConditionalMappingDesignV2Test,RuleResultReferencesTest" -pl apex-demo

# Run individual test classes
mvn test -Dtest=UltraSimpleTernaryTest -pl apex-demo
mvn test -Dtest=UltraSimpleRuleOrTest -pl apex-demo
mvn test -Dtest=RuleResultReferencesTest -pl apex-demo
mvn test -Dtest=ConditionalMappingDesignV2Test -pl apex-demo
```

## Key APEX Features Showcased

### 🔧 SpEL Expression Language
- Ternary operators: `condition ? value1 : value2`
- Set operations: `{'A', 'B'}.contains(#value)`
- Null safety and complex expressions

### 🎯 Rule Groups with OR Logic
- `operator: "OR"` - Any rule can pass
- `stop-on-first-failure: true` - First success wins
- Sequential rule evaluation

### 📊 Rule Result Integration
- `#ruleResults['rule-id']` - Access individual rule results
- `#ruleGroupResults['group-id']` - Access rule group results
- Conditional enrichments based on rule outcomes

### 🏗️ Field Enrichment Patterns
- Direct field mapping with transformations
- Conditional enrichment execution
- Multiple enrichment strategies

## Architecture Benefits

1. **Flexibility**: Multiple approaches for different complexity levels
2. **Performance**: Choose optimal pattern for your use case
3. **Maintainability**: Clear separation of concerns
4. **Testability**: Comprehensive test coverage for all patterns
5. **Scalability**: From simple mappings to complex business logic
6. **Real-world Ready**: Production-tested patterns and examples

## Additional Files

- **Map-External-to-Internal-Code.yaml** - External code mapping examples
- **Update-Stage-FX-Transaction.yaml** - FX transaction update scenarios
- **conditional-fx-transaction-working-example.yaml** - Complete FX workflow
- **conditional-mapping-design-v*.yaml** - Evolution of mapping designs
- **postgresql-database-localtest.yaml** - Database integration example

This demo provides a complete guide to APEX's conditional processing capabilities! 🚀
