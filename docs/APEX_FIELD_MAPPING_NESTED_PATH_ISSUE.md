# APEX Field Mapping SpEL Support Enhancement

**Date:** 2025-10-09
**Issue Type:** Enhancement Request
**Severity:** High (Consistency & User Experience)
**Status:** Approved - Ready for Implementation
**Effort:** 2.5 hours
**Priority:** High
**Target Version:** 2.3

---

## Quick Summary

**Problem:** Field mappings don't support SpEL expressions for nested field access, creating inconsistency with other APEX features.

**Solution:** Allow `source-field` and `target-field` to accept SpEL expressions (prefixed with `#`).

**Example:**
```yaml
enrichments:
  - id: field-enrichment-demo
    condition: '#data.currency != null'
    field-mappings:
      - source-field: '#data.currency'    # ✅ NEW - SpEL support!
        target-field: buy_currency
```

**Benefits:**
- ✅ Consistency across all APEX features (conditions, transformations, lookup-keys)
- ✅ Full SpEL power (nested fields, safe navigation, arrays, expressions)
- ✅ 100% backward compatible (via `#` prefix detection)
- ✅ Simple implementation (reuses existing SpEL infrastructure)

---

## Detailed Issue Summary

**Problem:** Field mappings in enrichments do not support SpEL expressions for nested field paths, while conditions, transformations, and lookup-keys all use SpEL.

This creates an inconsistency where:
- ✅ **Conditions** use SpEL: `condition: '#data.currency != null'`
- ✅ **Transformations** use SpEL: `transformation: '#data.currency'`
- ✅ **Lookup keys** use SpEL: `lookup-key: '#symbol'`
- ✅ **Calculations** use SpEL: `expression: '#amount * 0.01'`
- ❌ **Field mappings** don't support SpEL: `source-field: currency` (cannot use `#data.currency`)

**Solution:** Allow `source-field` and `target-field` to accept SpEL expressions (prefixed with `#`) for consistency across all APEX features.

---

## Current Behavior

### What Works (Conditions):
```yaml
enrichments:
  - id: field-enrichment-demo
    type: field-enrichment
    condition: '#data.currency != null'  # ✅ Works - can access nested field
    field-mappings:
      - source-field: currency            # ❌ Fails - looks for 'currency' at root level
        target-field: buy_currency
```

### The Problem:

When the object structure is:
```json
{
  "data": {
    "currency": "USD",
    "amount": 1000
  }
}
```

**Condition evaluation:**
- `#data.currency` → ✅ Works correctly, accesses `data.currency`

**Field mapping:**
- `source-field: currency` → ❌ Looks for `currency` at root level (not found)
- `source-field: data.currency` → ❌ Looks for field named "data.currency" literally (not found)

---

## Root Cause Analysis

### Code Location: `YamlEnrichmentProcessor.java`

#### Line 675: `getFieldValue()` Call
```java
sourceValue = getFieldValue(sourceObject, mapping.getSourceField());
```

#### Lines 750-787: `getFieldValue()` Implementation
```java
private Object getFieldValue(Object object, String fieldName) {
    // Handle Map objects
    if (object instanceof Map) {
        Object value = ((Map<?, ?>) object).get(fieldName);  // ❌ Simple key lookup only
        return value;
    }
    
    // Handle regular objects using getter methods
    String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    Method getter = object.getClass().getMethod(getterName);  // ❌ Simple getter only
    return getter.invoke(object);
}
```

**Problem:** The `getFieldValue()` method does NOT support:
1. Dot notation for nested paths (e.g., `data.currency`)
2. Recursive navigation through nested objects/maps

---

## Comparison: How Conditions Work

Conditions use **SpEL (Spring Expression Language)** which natively supports:
- Nested property access: `#data.currency`
- Safe navigation: `#data?.currency`
- Complex expressions: `#data.items[0].price`

**Code Location:** Lines 1214-1226 (createEvaluationContext)
```java
StandardEvaluationContext context = createEvaluationContext(targetObject);
Expression expr = parser.parseExpression("#data.currency");  // ✅ SpEL handles nesting
return expr.getValue(context);
```

---

## Impact Assessment

### Affected Features:
1. **field-enrichment** - Cannot map from nested source fields
2. **lookup-enrichment** - Field mappings cannot access nested lookup results
3. **calculation-enrichment** - Field mappings cannot access nested calculated results

### Workaround (Current):

Use `transformation` with SpEL instead of `source-field`:

```yaml
enrichments:
  - id: field-enrichment-demo
    type: field-enrichment
    condition: '#data.currency != null'
    field-mappings:
      - target-field: buy_currency
        transformation: '#data.currency'  # ✅ Workaround - use transformation instead
```

**Limitation:** This workaround requires knowing SpEL and is not intuitive for simple field copying.

---

## Proposed Solution: SpEL Support in `source-field` (RECOMMENDED)

### Why SpEL is the Right Choice

**Consistency Across APEX:**
- ✅ **Conditions** already use SpEL: `condition: '#data.currency != null'`
- ✅ **Transformations** already use SpEL: `transformation: '#data.currency'`
- ✅ **Lookup keys** already use SpEL: `lookup-key: '#symbol'`
- ✅ **Calculations** already use SpEL: `expression: '#amount * 0.01'`

**Making `source-field` support SpEL creates a unified, consistent API!**

---

### Implementation: Detect `#` Prefix for SpEL

**Strategy:** If `source-field` starts with `#`, treat it as a SpEL expression. Otherwise, use simple field lookup (backward compatible).

**Code Changes:**

```java
private Object getFieldValue(Object object, String fieldName) {
    if (object == null || fieldName == null) {
        LOGGER.fine("getFieldValue called with null object or fieldName");
        return null;
    }

    // NEW: If fieldName starts with #, treat it as a SpEL expression
    if (fieldName.startsWith("#")) {
        try {
            LOGGER.finest("Evaluating SpEL expression for field: " + fieldName);
            StandardEvaluationContext context = createEvaluationContext(object);
            Expression expr = getOrCompileExpression(fieldName);
            Object value = expr.getValue(context);
            LOGGER.finest("SpEL expression '" + fieldName + "' evaluated to: " + value);
            return value;
        } catch (Exception e) {
            LOGGER.warning("Failed to evaluate SpEL expression '" + fieldName + "': " + e.getMessage());
            return null;
        }
    }

    // EXISTING: Simple field lookup for non-SpEL field names
    LOGGER.finest("Getting field '" + fieldName + "' from object of type: " + object.getClass().getSimpleName());

    // Handle Map objects
    if (object instanceof Map) {
        Object value = ((Map<?, ?>) object).get(fieldName);
        LOGGER.finest("Map lookup for '" + fieldName + "' returned: " + value);
        return value;
    }

    // Handle regular objects using proper getter methods
    // ... existing getter logic ...
}
```

**Same enhancement for `setFieldValue()`:**

```java
private void setFieldValue(Object object, String fieldName, Object value) {
    if (object == null || fieldName == null) {
        LOGGER.fine("setFieldValue called with null object or fieldName");
        return;
    }

    // NEW: If fieldName starts with #, treat it as a SpEL expression for setting
    if (fieldName.startsWith("#")) {
        try {
            LOGGER.finest("Setting value via SpEL expression: " + fieldName);
            StandardEvaluationContext context = createEvaluationContext(object);
            Expression expr = getOrCompileExpression(fieldName);
            expr.setValue(context, value);
            LOGGER.finest("Successfully set field via SpEL '" + fieldName + "' to: " + value);
            return;
        } catch (Exception e) {
            LOGGER.warning("Failed to set field via SpEL expression '" + fieldName + "': " + e.getMessage());
            return;
        }
    }

    // EXISTING: Simple field setting for non-SpEL field names
    // ... existing setter logic ...
}
```

---

### Benefits

**1. Full SpEL Power:**
- ✅ Nested fields: `#data.currency`
- ✅ Safe navigation: `#data?.currency`
- ✅ Array indexing: `#items[0].price`
- ✅ Complex expressions: `#data.trade.counterparty.name`
- ✅ Conditional logic: `#status == 'ACTIVE' ? #activePrice : #inactivePrice`

**2. Perfect Consistency:**
```yaml
enrichments:
  - id: example
    type: field-enrichment
    condition: '#data.currency != null'        # SpEL in condition
    field-mappings:
      - source-field: '#data.currency'         # SpEL in source-field (NEW!)
        target-field: buy_currency
      - source-field: '#data.amount'           # SpEL in source-field (NEW!)
        target-field: trade_amount
        transformation: '#value * 1.1'         # SpEL in transformation (existing)
```

**3. Backward Compatible:**
```yaml
field-mappings:
  # Existing simple field names still work
  - source-field: currency
    target-field: buy_currency

  # New SpEL expressions work too
  - source-field: '#data.currency'
    target-field: buy_currency
```

**4. Clear Intent:**
- `source-field: currency` → Simple field lookup
- `source-field: '#data.currency'` → SpEL expression (explicit via `#` prefix)

**5. No Performance Impact:**
- Simple fields use fast reflection/map lookup
- Only SpEL expressions (with `#` prefix) use SpEL evaluation

---

### Comparison: Before vs After

**Before (Current Workaround):**
```yaml
field-mappings:
  # Can't use source-field for nested fields
  # Must use transformation instead
  - target-field: buy_currency
    transformation: '#data.currency'  # Workaround - not intuitive
```

**After (With SpEL Support):**
```yaml
field-mappings:
  # Natural and intuitive
  - source-field: '#data.currency'
    target-field: buy_currency

  # Can still use transformation for actual transformations
  - source-field: '#data.amount'
    target-field: adjusted_amount
    transformation: '#value * 1.1'  # Transform the source value
```

---

## Implementation Plan

### Step 1: Update `getFieldValue()` Method

**File:** `apex-core/src/main/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessor.java`
**Line:** ~750

**Changes:**
1. Add SpEL detection: Check if `fieldName.startsWith("#")`
2. If SpEL: Use `createEvaluationContext()` and `getOrCompileExpression()`
3. If not SpEL: Use existing simple field lookup logic
4. Add logging for SpEL expression evaluation

### Step 2: Update `setFieldValue()` Method

**File:** Same as above
**Line:** ~796

**Changes:**
1. Add SpEL detection: Check if `fieldName.startsWith("#")`
2. If SpEL: Use `expr.setValue(context, value)` for setting
3. If not SpEL: Use existing simple field setting logic
4. Add logging for SpEL expression setting

### Step 3: Add Tests

**File:** `apex-core/src/test/java/dev/mars/apex/core/service/enrichment/SpelFieldMappingTest.java` (NEW)

**Test Cases:**
1. **Simple nested field access** - `source-field: '#data.currency'`
2. **Multi-level nesting** - `source-field: '#data.trade.counterparty'`
3. **Safe navigation** - `source-field: '#data?.currency'`
4. **Array indexing** - `source-field: '#items[0].price'`
5. **Complex expressions** - `source-field: '#data.trade?.amount'`
6. **Nested target fields** - `target-field: '#data.result.status'`
7. **Mixed simple and SpEL** - Both in same enrichment
8. **Backward compatibility** - Simple field names still work
9. **Null handling** - SpEL expressions with null values
10. **Error handling** - Invalid SpEL expressions

### Step 4: Update Documentation

**Files to Update:**

1. **`docs/APEX_YAML_REFERENCE.md`**
   - Section 6.1 (Field Enrichments) - Add SpEL examples
   - Section 6.3 (Lookup Enrichments) - Add SpEL in field-mappings
   - Add note about `#` prefix for SpEL expressions

2. **`docs/APEX_CONDITIONAL_PROCESSING_GUIDE.md`**
   - Add section on SpEL in field mappings
   - Show consistency across all APEX features

3. **`docs/APEX_RULES_ENGINE_USER_GUIDE.md`**
   - Add "SpEL in Field Mappings" section
   - Show examples of nested field access

---

## Complete Examples After Enhancement

### Example 1: Basic Nested Field Access

```yaml
enrichments:
  - id: field-enrichment-demo
    type: field-enrichment
    condition: '#data.currency != null'
    field-mappings:
      # ✅ Simple field (existing behavior - backward compatible)
      - source-field: status
        target-field: trade_status

      # ✅ Nested field with SpEL (new behavior)
      - source-field: '#data.currency'
        target-field: buy_currency

      # ✅ Multi-level nesting with SpEL (new behavior)
      - source-field: '#data.trade.counterparty'
        target-field: counterparty_name

      # ✅ Safe navigation with SpEL (new behavior)
      - source-field: '#data?.trade?.amount'
        target-field: trade_amount
```

### Example 2: Lookup Enrichment with Nested Results

```yaml
enrichments:
  - id: instrument-lookup
    type: lookup-enrichment
    lookup-config:
      lookup-key: '#symbol'
      lookup-dataset:
        type: rest-api
        data-source-ref: market-data-api
        operation-ref: get-instrument
    field-mappings:
      # Lookup result structure: { "data": { "instrument": { "name": "...", "type": "..." } } }

      # ✅ Access nested fields in lookup result with SpEL
      - source-field: '#data.instrument.name'
        target-field: instrument_name

      - source-field: '#data.instrument.type'
        target-field: instrument_type

      - source-field: '#data.pricing?.bid'
        target-field: bid_price
        default-value: 0.0
```

### Example 3: Complex SpEL Expressions

```yaml
enrichments:
  - id: advanced-mapping
    type: field-enrichment
    field-mappings:
      # ✅ Array indexing
      - source-field: '#data.trades[0].amount'
        target-field: first_trade_amount

      # ✅ Conditional expression in source-field
      - source-field: '#data.status == "ACTIVE" ? #data.activePrice : #data.inactivePrice'
        target-field: current_price

      # ✅ Method calls
      - source-field: '#data.currency.toUpperCase()'
        target-field: currency_code

      # ✅ Combination with transformation
      - source-field: '#data.amount'
        target-field: adjusted_amount
        transformation: '#value * 1.1'  # Apply 10% markup
```

### Example 4: Your Original Use Case (SOLVED!)

```yaml
enrichments:
  - id: field-enrichment-demo
    name: field-enrichment-demo
    description: field-enrichment-demo
    enabled: true
    condition: '#data.currency != null'
    type: field-enrichment
    field-mappings:
      # ✅ NOW WORKS! Access nested field with SpEL
      - source-field: '#data.currency'
        target-field: buy_currency

      - source-field: '#data.amount'
        target-field: trade_amount
```

---

## Testing Strategy

### Unit Tests (SpelFieldMappingTest.java):

**1. Basic SpEL Field Access:**
```java
@Test
void testSpelNestedFieldAccess() {
    Map<String, Object> data = Map.of(
        "data", Map.of("currency", "USD", "amount", 1000)
    );

    // Test: source-field: '#data.currency'
    // Expected: Extracts "USD" from nested structure
}
```

**2. Safe Navigation:**
```java
@Test
void testSpelSafeNavigation() {
    Map<String, Object> data = Map.of("data", Map.of());

    // Test: source-field: '#data?.currency'
    // Expected: Returns null without error
}
```

**3. Array Indexing:**
```java
@Test
void testSpelArrayIndexing() {
    Map<String, Object> data = Map.of(
        "items", List.of(
            Map.of("price", 100),
            Map.of("price", 200)
        )
    );

    // Test: source-field: '#items[0].price'
    // Expected: Extracts 100
}
```

**4. Backward Compatibility:**
```java
@Test
void testSimpleFieldStillWorks() {
    Map<String, Object> data = Map.of("currency", "USD");

    // Test: source-field: 'currency' (no # prefix)
    // Expected: Works as before
}
```

**5. Complex Expressions:**
```java
@Test
void testComplexSpelExpression() {
    Map<String, Object> data = Map.of(
        "status", "ACTIVE",
        "activePrice", 100,
        "inactivePrice", 50
    );

    // Test: source-field: '#status == "ACTIVE" ? #activePrice : #inactivePrice'
    // Expected: Returns 100
}
```

**6. Error Handling:**
```java
@Test
void testInvalidSpelExpression() {
    Map<String, Object> data = Map.of("currency", "USD");

    // Test: source-field: '#invalid..syntax'
    // Expected: Returns null, logs warning
}
```

### Integration Tests:

**1. Lookup Enrichment with Nested Results:**
- Test REST API lookup returning nested JSON
- Field mappings extract nested fields with SpEL
- Verify correct values mapped to target

**2. Field Enrichment with Mixed Mappings:**
- Some field-mappings use simple names
- Some use SpEL expressions
- Verify both work correctly in same enrichment

**3. Calculation + Field Mapping:**
- Calculation creates nested result structure
- Field mappings extract with SpEL
- Verify end-to-end flow

---

## Backward Compatibility

✅ **100% Backward Compatible**

**Detection Logic:** If `source-field` starts with `#`, it's SpEL. Otherwise, it's a simple field name.

**Existing configurations continue to work unchanged:**
```yaml
field-mappings:
  - source-field: currency  # ✅ Still works - simple field lookup
    target-field: buy_currency

  - source-field: amount    # ✅ Still works - simple field lookup
    target-field: trade_amount
```

**New SpEL syntax is additive (opt-in via `#` prefix):**
```yaml
field-mappings:
  - source-field: '#data.currency'  # ✅ New - SpEL expression
    target-field: buy_currency

  - source-field: '#data.amount'    # ✅ New - SpEL expression
    target-field: trade_amount
```

**Mixed usage works perfectly:**
```yaml
field-mappings:
  - source-field: status              # Simple field
    target-field: trade_status

  - source-field: '#data.currency'    # SpEL expression
    target-field: buy_currency
```

---

## Priority and Effort

**Priority:** High (Consistency & User Experience)
**Effort:** Small (1-2 hours)
**Risk:** Very Low (backward compatible, uses existing SpEL infrastructure)

**Recommended Timeline:**
- Implementation: 1 hour (simple `if` check + SpEL call)
- Testing: 1 hour (unit + integration tests)
- Documentation: 30 minutes (update YAML reference)
- **Total: 2.5 hours**

**Why This is Easy:**
- ✅ SpEL infrastructure already exists (`createEvaluationContext`, `getOrCompileExpression`)
- ✅ Just add detection logic: `if (fieldName.startsWith("#"))`
- ✅ No new dependencies or complex logic
- ✅ Reuses existing, proven code paths

---

## Summary: Why SpEL is the Right Choice

| Aspect | SpEL Support | Dot Notation Only |
|--------|-------------|-------------------|
| **Consistency** | ✅ Matches conditions, transformations, lookup-keys | ❌ Different syntax than rest of APEX |
| **Power** | ✅ Arrays, safe navigation, expressions | ❌ Only simple nesting |
| **Implementation** | ✅ Reuses existing SpEL code | ⚠️ New custom parsing logic |
| **Backward Compatible** | ✅ Yes (via `#` prefix detection) | ✅ Yes |
| **User Learning Curve** | ✅ Learn once, use everywhere | ⚠️ Learn different syntax |
| **Future-Proof** | ✅ Handles all future needs | ❌ Limited to dot notation |

**Decision: Implement SpEL support in `source-field` and `target-field`**

---

---

## Quick Implementation Reference

### Code Changes Required

**File:** `apex-core/src/main/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessor.java`

#### Change 1: Update `getFieldValue()` (Line ~750)

```java
private Object getFieldValue(Object object, String fieldName) {
    if (object == null || fieldName == null) {
        LOGGER.fine("getFieldValue called with null object or fieldName");
        return null;
    }

    // NEW: If fieldName starts with #, treat it as SpEL
    if (fieldName.startsWith("#")) {
        try {
            LOGGER.finest("Evaluating SpEL expression for field: " + fieldName);
            StandardEvaluationContext context = createEvaluationContext(object);
            Expression expr = getOrCompileExpression(fieldName);
            Object value = expr.getValue(context);
            LOGGER.finest("SpEL expression '" + fieldName + "' evaluated to: " + value);
            return value;
        } catch (Exception e) {
            LOGGER.warning("Failed to evaluate SpEL expression '" + fieldName + "': " + e.getMessage());
            return null;
        }
    }

    // EXISTING: Simple field lookup for non-SpEL field names
    LOGGER.finest("Getting field '" + fieldName + "' from object of type: " + object.getClass().getSimpleName());

    // Handle Map objects
    if (object instanceof Map) {
        Object value = ((Map<?, ?>) object).get(fieldName);
        LOGGER.finest("Map lookup for '" + fieldName + "' returned: " + value);
        return value;
    }

    // Handle regular objects using proper getter methods
    try {
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Method getter = object.getClass().getMethod(getterName);
        Object value = getter.invoke(object);
        LOGGER.finest("Getter method lookup for '" + fieldName + "' returned: " + value);
        return value;
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        // Try boolean getter (isXxx)
        try {
            String booleanGetterName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method booleanGetter = object.getClass().getMethod(booleanGetterName);
            Object value = booleanGetter.invoke(object);
            LOGGER.finest("Boolean getter method lookup for '" + fieldName + "' returned: " + value);
            return value;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e2) {
            LOGGER.fine("No getter method found for field '" + fieldName + "' on object of type " +
                       object.getClass().getSimpleName());
            return null;
        }
    }
}
```

#### Change 2: Update `setFieldValue()` (Line ~796)

```java
private void setFieldValue(Object object, String fieldName, Object value) {
    if (object == null || fieldName == null) {
        LOGGER.fine("setFieldValue called with null object or fieldName");
        return;
    }

    // NEW: If fieldName starts with #, treat it as SpEL
    if (fieldName.startsWith("#")) {
        try {
            LOGGER.finest("Setting value via SpEL expression: " + fieldName);
            StandardEvaluationContext context = createEvaluationContext(object);
            Expression expr = getOrCompileExpression(fieldName);
            expr.setValue(context, value);
            LOGGER.finest("Successfully set field via SpEL '" + fieldName + "' to: " + value);
            return;
        } catch (Exception e) {
            LOGGER.warning("Failed to set field via SpEL expression '" + fieldName + "': " + e.getMessage());
            return;
        }
    }

    // EXISTING: Simple field setting for non-SpEL field names
    LOGGER.finest("Setting field '" + fieldName + "' to value: " + value +
                 " on object of type: " + object.getClass().getSimpleName());

    // Handle Map objects
    if (object instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) object;
        map.put(fieldName, value);
        LOGGER.finest("Successfully set map key '" + fieldName + "' to: " + value);
        return;
    }

    // Handle regular objects using proper setter methods
    try {
        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Method setter = object.getClass().getMethod(setterName, value.getClass());
        setter.invoke(object, value);
        LOGGER.finest("Successfully set field '" + fieldName + "' to: " + value);
    } catch (NoSuchMethodException e) {
        // Try with different parameter types if exact match fails
        try {
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method[] methods = object.getClass().getMethods();
            for (Method method : methods) {
                if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                    Class<?> paramType = method.getParameterTypes()[0];
                    if (paramType.isAssignableFrom(value.getClass())) {
                        method.invoke(object, value);
                        LOGGER.finest("Successfully set field '" + fieldName + "' to: " + value);
                        return;
                    }
                }
            }
            LOGGER.warning("No suitable setter method found for field '" + fieldName + "' on object of type " +
                          object.getClass().getSimpleName());
        } catch (IllegalAccessException | InvocationTargetException e2) {
            LOGGER.log(Level.WARNING, "Could not invoke setter for field '" + fieldName + "' on object of type " +
                      object.getClass().getSimpleName() + ": " + e2.getMessage(), e2);
        }
    } catch (IllegalAccessException | InvocationTargetException e) {
        LOGGER.log(Level.WARNING, "Could not invoke setter for field '" + fieldName + "' on object of type " +
                  object.getClass().getSimpleName() + ": " + e.getMessage(), e);
    }
}
```

---

## Test Checklist

### Unit Tests (Create: `SpelFieldMappingTest.java`)

- [ ] Basic nested field: `source-field: '#data.currency'`
- [ ] Safe navigation: `source-field: '#data?.currency'`
- [ ] Array indexing: `source-field: '#items[0].price'`
- [ ] Complex expressions: `source-field: '#status == "ACTIVE" ? #activePrice : #inactivePrice'`
- [ ] Backward compatibility: `source-field: 'currency'` (no `#`)
- [ ] Error handling: Invalid SpEL expression
- [ ] Null handling: SpEL with null values
- [ ] Mixed usage: Simple + SpEL in same enrichment
- [ ] Nested target: `target-field: '#data.result.status'`
- [ ] Method calls: `source-field: '#data.currency.toUpperCase()'`

### Integration Tests

- [ ] Lookup enrichment with nested REST API results
- [ ] Field enrichment with mixed simple/SpEL mappings
- [ ] Calculation + field mapping with SpEL

---

## Documentation Updates Required

- [ ] `docs/APEX_YAML_REFERENCE.md` - Add SpEL examples to field-mappings section
- [ ] `docs/APEX_CONDITIONAL_PROCESSING_GUIDE.md` - Add section on SpEL in field mappings
- [ ] `docs/APEX_RULES_ENGINE_USER_GUIDE.md` - Add "SpEL in Field Mappings" section

---

**Status:** Ready for Implementation
**Assigned To:** TBD
**Approved By:** User (2025-10-09)

