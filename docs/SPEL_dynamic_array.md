# SpEL Dynamic Array Access in APEX Rules

This guide provides comprehensive examples and best practices for accessing dynamic arrays in Spring Expression Language (SpEL) expressions within APEX rules.

## Table of Contents

- [Basic Array Access](#basic-array-access)
- [Safe Navigation](#safe-navigation)
- [Dynamic Index Access](#dynamic-index-access)
- [Array Bounds Checking](#array-bounds-checking)
- [Collection Operations](#collection-operations)
- [Financial Data Examples](#financial-data-examples)
- [Advanced Patterns](#advanced-patterns)
- [Error-Safe Patterns](#error-safe-patterns)
- [Best Practices](#best-practices)
- [Common Pitfalls](#common-pitfalls)

## Basic Array Access

### Bracket Notation Syntax

```yaml
# Correct syntax for nested array access
condition: "#trade['otcTrade']['otcLeg'][0]['stbRuleName'] != null"

# Alternative with mixed notation (when you know some property names)
condition: "#trade.otcTrade.otcLeg[0]['stbRuleName'] != null"

# Pure dot notation (when all properties are known)
condition: "#trade.otcTrade.otcLeg[0].stbRuleName != null"
```

### Array Element Access

```yaml
# Access first element
condition: "#positions[0].instrumentId != null"

# Access specific index
condition: "#trades[2].tradeId != null"

# Access last element (if size is known)
condition: "#items[#items.size() - 1].status == 'COMPLETE'"
```

## Safe Navigation

Always use safe navigation (`?.`) to prevent null pointer exceptions:

```yaml
# Safe array access - prevents errors if any level is null
condition: "#trade?.otcTrade?.otcLeg?.[0]?.stbRuleName != null"

# Safe access with bracket notation throughout
condition: "#trade?.['otcTrade']?.['otcLeg']?.[0]?.['stbRuleName'] != null"

# Mixed safe navigation
condition: "#portfolio?.positions?.[0]?.trades?.size() > 0"
```

## Dynamic Index Access

For truly dynamic array access where the index itself is variable:

### How Dynamic Index Resolution Works

Understanding the step-by-step process of how SpEL resolves dynamic array indices:

#### Expression Resolution Logic

**Expression:** `#trade.otcTrade.otcLeg[#trade.selectedLegIndex].stbRuleName != null`

**Step-by-Step Resolution:**

1. **`#trade`** → Resolves to the `trade` HashMap from context data
2. **`#trade.selectedLegIndex`** → Resolves to the integer value (e.g., `1`)
3. **`#trade.otcTrade`** → Resolves to the `otcTrade` HashMap
4. **`#trade.otcTrade.otcLeg`** → Resolves to the `List<Map<String, Object>>` array
5. **`#trade.otcTrade.otcLeg[#trade.selectedLegIndex]`** → Becomes `otcLeg[1]` → Resolves to element at index 1
6. **`#trade.otcTrade.otcLeg[#trade.selectedLegIndex].stbRuleName`** → Resolves to the property value
7. **Final evaluation:** `propertyValue != null` → `true` or `false`

#### Data Structure Example

```yaml
# Test data structure:
trade:
  selectedLegIndex: 1           # ← Dynamic index value
  otcTrade:
    otcLeg:
      - stbRuleName: "RULE_A"   # ← Index 0
      - stbRuleName: "RULE_B"   # ← Index 1 ← Selected by selectedLegIndex
      - stbRuleName: "RULE_C"   # ← Index 2

# SpEL Resolution Process:
#trade.selectedLegIndex        → 1
#trade.otcTrade.otcLeg[1]      → { stbRuleName: "RULE_B" }
#trade.otcTrade.otcLeg[1].stbRuleName → "RULE_B"
```

#### Why Dynamic Indexing Works

1. **SpEL evaluates expressions inside brackets first** - `[#trade.selectedLegIndex]` is evaluated before array access
2. **The result becomes the array index** - The integer `1` becomes the literal index
3. **Array access happens with resolved index** - `otcLeg[1]` accesses the second element
4. **Property access continues normally** - `.stbRuleName` accesses the property of the resolved element

#### Runtime Index Selection

The index is determined at runtime based on data values:

```yaml
# Different selectedLegIndex values produce different results:

# If selectedLegIndex = 0
otcLeg[#trade.selectedLegIndex] → otcLeg[0] → "RULE_A"

# If selectedLegIndex = 1
otcLeg[#trade.selectedLegIndex] → otcLeg[1] → "RULE_B"

# If selectedLegIndex = 2
otcLeg[#trade.selectedLegIndex] → otcLeg[2] → "RULE_C"
```

### Dynamic Index Examples

```yaml
# Using a variable index
condition: "#trade.otcTrade.otcLeg[#legIndex].stbRuleName != null"
# Resolution: #legIndex → 2, otcLeg[2] → third element

# Dynamic index from another field
condition: "#trade.otcTrade.otcLeg[#trade.selectedLegIndex].stbRuleName != null"
# Resolution: #trade.selectedLegIndex → 1, otcLeg[1] → second element

# Safe dynamic index access
condition: "#trade?.otcTrade?.otcLeg?.size() > #legIndex && #trade.otcTrade.otcLeg[#legIndex]?.stbRuleName != null"
# Resolution: Checks bounds (size > index) before accessing element

# Dynamic index with calculation
condition: "#items[#currentIndex + 1]?.status != null"
# Resolution: #currentIndex + 1 → 0 + 1 = 1, items[1] → second element
```

### Real-World Dynamic Index Applications

```yaml
# Financial trading scenarios
condition: "#trade.legs[#trade.payLegIndex].currency == 'USD'"
condition: "#portfolio.positions[#portfolio.primaryPositionIndex].quantity > 0"
condition: "#basket.instruments[#basket.selectedInstrumentIndex].maturityDate != null"

# Risk management scenarios
condition: "#riskLimits.thresholds[#riskProfile.severityLevel].maxExposure > #currentExposure"
condition: "#counterparties[#trade.counterpartyIndex].creditRating in {'AAA', 'AA+'}"

# Regulatory reporting scenarios
condition: "#reportingRules[#jurisdiction.ruleSetIndex].mandatoryFields.contains('LEI')"
condition: "#complianceChecks[#trade.productType.checkIndex].required == true"
```

## Array Bounds Checking

Always check array bounds before accessing elements:

```yaml
# Check array exists and has elements before accessing
condition: "#trade?.otcTrade?.otcLeg != null && #trade.otcTrade.otcLeg.size() > 0 && #trade.otcTrade.otcLeg[0].stbRuleName != null"

# Check specific index exists
condition: "#trade?.otcTrade?.otcLeg != null && #trade.otcTrade.otcLeg.size() > 2 && #trade.otcTrade.otcLeg[2].stbRuleName != null"

# More concise with safe navigation
condition: "#trade?.otcTrade?.otcLeg?.size() > 0 && #trade.otcTrade.otcLeg[0]?.stbRuleName != null"

# Check minimum array size
condition: "#positions?.size() >= 3 && #positions[2].quantity > 0"
```

## Collection Operations

SpEL provides powerful collection operations for dynamic arrays:

### Filtering Operations

```yaml
# Find first element matching condition
condition: "#trade?.otcTrade?.otcLeg?.^[stbRuleName == 'SPECIFIC_RULE'] != null"

# Find last element matching condition
condition: "#trade?.otcTrade?.otcLeg?.$[stbRuleName != null] != null"

# Check if any element matches condition
condition: "#trade?.otcTrade?.otcLeg?.?[stbRuleName != null].size() > 0"

# Filter elements by multiple conditions
condition: "#positions?.?[quantity > 0 && instrumentType == 'EQUITY'].size() > 0"
```

### Projection Operations

```yaml
# Get all stbRuleNames from the array
expression: "#trade?.otcTrade?.otcLeg?.![stbRuleName]"

# Get all quantities from positions
expression: "#positions?.![quantity]"

# Project nested properties
expression: "#trades?.![legs?.![ruleName]]"

# Project with null safety
expression: "#items?.![name != null ? name : 'UNKNOWN']"
```

### Aggregation Operations

```yaml
# Count elements matching condition
expression: "#trade?.otcTrade?.otcLeg?.?[stbRuleName != null].size()"

# Sum all quantities
expression: "#positions?.![quantity].sum()"

# Get maximum value
expression: "#trades?.![notionalAmount].max()"

# Get minimum value
expression: "#trades?.![notionalAmount].min()"
```

## Financial Data Examples

Real-world examples for OTC trade processing:

```yaml
rules:
  # Check if any leg has a specific rule
  - id: "otc-leg-rule-check"
    name: "OTC Leg Rule Validation"
    condition: "#trade?.otcTrade?.otcLeg?.?[stbRuleName == 'MARGIN_RULE'].size() > 0"
    message: "At least one leg must have margin rule"
    severity: "ERROR"

  # Validate all legs have required fields
  - id: "all-legs-complete"
    name: "All Legs Complete Validation"
    condition: "#trade?.otcTrade?.otcLeg?.?[stbRuleName == null || stbRuleName.trim().isEmpty()].size() == 0"
    message: "All legs must have stbRuleName specified"
    severity: "ERROR"

  # Access specific leg by position with safety
  - id: "first-leg-validation"
    name: "First Leg Validation"
    condition: "#trade?.otcTrade?.otcLeg?.size() > 0 && #trade.otcTrade.otcLeg[0]?.stbRuleName?.matches('[A-Z_]+')"
    message: "First leg must have valid rule name format"
    severity: "WARNING"

  # Dynamic leg access based on trade type
  - id: "dynamic-leg-access"
    name: "Dynamic Leg Access"
    condition: "#trade?.tradeType == 'SWAP' && #trade?.otcTrade?.otcLeg?.size() >= 2 && #trade.otcTrade.otcLeg[1]?.stbRuleName != null"
    message: "Swap trades must have second leg with rule name"
    severity: "ERROR"

  # Portfolio position validation
  - id: "portfolio-position-check"
    name: "Portfolio Position Validation"
    condition: "#portfolio?.positions?.?[quantity <= 0 || instrumentId == null].size() == 0"
    message: "All positions must have positive quantity and valid instrument ID"
    severity: "ERROR"

  # Risk limit validation across positions
  - id: "risk-limit-check"
    name: "Risk Limit Validation"
    condition: "#portfolio?.positions?.![notionalValue].sum() <= #riskLimits.maxPortfolioValue"
    message: "Portfolio value exceeds risk limits"
    severity: "ERROR"
```

## Advanced Patterns

For more complex scenarios:

```yaml
# Nested array access with dynamic indices
condition: "#portfolio?.positions?.[#positionIndex]?.trades?.[#tradeIndex]?.legs?.[0]?.ruleName != null"

# Multiple array levels with filtering
expression: "#portfolio?.positions?.![trades?.![legs?.![ruleName]]].flatten()"

# Conditional array access based on data structure
condition: >
  #trade?.structure == 'SIMPLE' ? 
    (#trade?.otcTrade?.otcLeg?.[0]?.stbRuleName != null) : 
    (#trade?.otcTrade?.otcLeg?.?[stbRuleName != null].size() == #trade.otcTrade.otcLeg.size())

# Complex filtering with multiple conditions
expression: "#trades?.?[notionalAmount > 1000000 && counterparty?.rating in {'AAA', 'AA+', 'AA'}]?.![tradeId]"

# Dynamic property access with fallback
expression: "#data[#propertyName] != null ? #data[#propertyName] : #data['defaultProperty']"
```

## Error-Safe Patterns

### Comprehensive Safe Dynamic Array Access

```yaml
# Complete safe dynamic array access
condition: >
  #trade != null &&
  #trade.containsKey('otcTrade') &&
  #trade.otcTrade != null &&
  #trade.otcTrade.containsKey('otcLeg') &&
  #trade.otcTrade.otcLeg != null &&
  #trade.otcTrade.otcLeg instanceof T(java.util.List) &&
  #trade.otcTrade.otcLeg.size() > 0 &&
  #trade.otcTrade.otcLeg[0] != null &&
  #trade.otcTrade.otcLeg[0].containsKey('stbRuleName') &&
  #trade.otcTrade.otcLeg[0].stbRuleName != null

# More concise version using safe navigation
condition: "#trade?.otcTrade?.otcLeg?.size() > 0 && #trade.otcTrade.otcLeg[0]?.stbRuleName != null"
```

### Type-Safe Array Access

```yaml
# Verify array type before access
condition: "#data.items instanceof T(java.util.List) && #data.items.size() > 0"

# Safe casting with type check
expression: "#data.items instanceof T(java.util.List) ? #data.items[0] : null"

# Multiple type checks
condition: "#trade?.legs instanceof T(java.util.List) && #trade.legs.size() > 0 && #trade.legs[0] instanceof T(java.util.Map)"
```

### Null-Safe Collection Operations

```yaml
# Safe filtering with null checks
expression: "#items?.?[# != null && #.status != null && #.status == 'ACTIVE'] ?: {}"

# Safe projection with fallbacks
expression: "#positions?.![quantity != null ? quantity : 0] ?: {}"

# Safe aggregation
expression: "#values?.![# != null ? # : 0].sum() ?: 0"
```

## Best Practices

### 1. Always Use Safe Navigation

```yaml
# ✅ Good - safe navigation prevents NPE
condition: "#trade?.otcTrade?.otcLeg?.size() > 0"

# ❌ Bad - can throw NullPointerException
condition: "#trade.otcTrade.otcLeg.size() > 0"
```

### 2. Check Array Bounds

```yaml
# ✅ Good - bounds checking
condition: "#items?.size() > 2 && #items[2]?.status == 'ACTIVE'"

# ❌ Bad - no bounds checking
condition: "#items[2].status == 'ACTIVE'"
```

### 3. Use Collection Operations for Filtering

```yaml
# ✅ Good - use collection operations
condition: "#trades?.?[status == 'PENDING'].size() > 0"

# ❌ Less efficient - manual iteration would be needed
```

### 4. Validate Data Types

```yaml
# ✅ Good - type validation
condition: "#data.items instanceof T(java.util.List) && #data.items.size() > 0"

# ❌ Risky - assumes type without checking
condition: "#data.items.size() > 0"
```

### 5. Use Meaningful Variable Names

```yaml
# ✅ Good - clear variable names
condition: "#currentLegIndex < #trade.otcTrade.otcLeg.size()"

# ❌ Less clear - generic names
condition: "#i < #trade.otcTrade.otcLeg.size()"
```

## Common Pitfalls

### Syntax Errors

```yaml
# ❌ Wrong - incorrect bracket syntax
condition: "#trade.['otcTrade'].['otcLeg'][0].['stbRuleName'] != null"

# ✅ Correct - proper bracket syntax
condition: "#trade['otcTrade']['otcLeg'][0]['stbRuleName'] != null"
```

### Null Pointer Exceptions

```yaml
# ❌ Wrong - can cause NullPointerException
condition: "#trade.otcTrade.otcLeg[0].stbRuleName != null"

# ✅ Correct - safe navigation
condition: "#trade?.otcTrade?.otcLeg?.[0]?.stbRuleName != null"
```

### Array Bounds Errors

```yaml
# ❌ Wrong - no bounds checking
condition: "#trade.otcTrade.otcLeg[5].stbRuleName != null"

# ✅ Correct - bounds checking
condition: "#trade?.otcTrade?.otcLeg?.size() > 5 && #trade.otcTrade.otcLeg[5]?.stbRuleName != null"
```

### Type Assumptions

```yaml
# ❌ Wrong - assumes array type
condition: "#data.items[0].name != null"

# ✅ Correct - validates type first
condition: "#data.items instanceof T(java.util.List) && #data.items.size() > 0 && #data.items[0]?.name != null"
```

### Performance Issues

```yaml
# ❌ Inefficient - repeated expensive operations
condition: "#expensiveCalculation()[0] != null && #expensiveCalculation()[0].value > 100"

# ✅ Efficient - calculate once, store in variable
condition: "#result = #expensiveCalculation(); #result?.size() > 0 && #result[0]?.value > 100"
```

## Summary

Dynamic array access in APEX SpEL expressions requires careful attention to:

- **Safe navigation** to prevent null pointer exceptions
- **Bounds checking** to avoid array index errors
- **Type validation** to ensure data structure assumptions are correct
- **Collection operations** for efficient filtering and projection
- **Performance considerations** to avoid repeated expensive operations

By following these patterns and best practices, you can create robust SpEL expressions that handle dynamic arrays safely and efficiently in your APEX rules.
