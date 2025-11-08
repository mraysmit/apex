# APEX Transformations Section - Implementation Design

**Document Version:** 2.0
**Date:** November 8, 2025
**Author:** APEX Development Team
**Status:** Design Proposal
**Target Version:** APEX 3.1

---

## Executive Summary

This document provides the implementation design for the **top-level `transformations` section** in APEX YAML configurations. While transformations currently work in field-mappings and pipeline steps, they are **not implemented as a standalone processing section** like enrichments and rules.

**Goal:** Enable transformations to be defined and executed as first-class processing items in YAML document order.

**Important:** This design also addresses a critical **API inconsistency** where the same concept is called `transformation` in some places and `expression` in others. We standardize on **`expression`** throughout APEX.

---

## Critical API Inconsistency Issue

### **Problem: Inconsistent Naming**

APEX currently uses **two different property names** for the exact same concept:

| Context | Property Name | Purpose |
|---------|---------------|---------|
| **Field Mappings** | `transformation` | SpEL expression to transform a value |
| **Calculation Config** | `expression` | SpEL expression to calculate a value |
| **Transformation Actions** | `expression` | SpEL expression to transform a value |

**They all do the same thing:** Parse and evaluate a SpEL expression!

### **Example of the Inconsistency:**

```yaml
# Uses "transformation" property:
field-mappings:
  - source-field: "amount"
    target-field: "adjusted_amount"
    expression: "#value * 1.1"  # ← Called "transformation"

# Uses "expression" property:
calculation-config:
  expression: "#amount * 0.05"  # ← Called "expression"

# Uses "expression" property:
transformations:
  - id: "transform-1"
    source-field: "value"
    target-field: "result"
    expression: "#value.toUpperCase()"  # ← Called "expression"
```

### **Resolution: Standardize on `expression`**

**Rationale:**
1. ✅ **Technical accuracy** - It's literally a SpEL expression
2. ✅ **Consistency with SpEL** - Spring Expression Language calls them "expressions"
3. ✅ **Already used in 2 places** - `calculation-config` and `TransformationAction`
4. ✅ **Shorter and clearer** - More concise than "transformation"

**Strategy:**
- **Phase 1:** Support both `expression` and `transformation` (backward compatibility)
- **Phase 2:** Deprecate `transformation` with warnings
- **Phase 3:** Remove `transformation` in future major version

---

## Current State vs. Desired State

### ✅ **What Works Today:**

```yaml
# 1. Expressions in field-mappings (WORKING - but uses old "transformation" name)
enrichments:
  - id: "my-enrichment"
    field-mappings:
      - source-field: "amount"
        target-field: "adjusted_amount"
        expression: "#value * 1.1"  # ← Works but uses old name
        # NEW: expression: "#value * 1.1"  # ← Recommended new syntax

# 2. Transformations in pipeline steps (WORKING)
pipeline:
  steps:
    - name: "transform-data"
      type: "transform"
      transformations:  # ← Works!
        - name: "add-timestamp"
          type: "field-addition"
          field: "processed_at"
          value: "CURRENT_TIMESTAMP"
```

### ❌ **What Doesn't Work (Yet):**

```yaml
# 3. Top-level transformations section (NOT WORKING)
transformations:  # ← Recognized but not executed!
  - id: "transform-1"
    type: "field-transformation"
    source-field: "value"
    target-field: "result"
    expression: "#value.toUpperCase()"  # ← Uses standardized "expression"
```

---

## Design Goals

1. **Document Order Execution:** Transformations execute in exact YAML document order
2. **Consistency:** Transformations work like enrichments and rules (first-class processing items)
3. **Reusability:** Define transformations once, reference them multiple times
4. **Conditional Execution:** Support `condition` field for when to apply transformations
5. **API Standardization:** Use `expression` consistently across all APEX features
6. **Backward Compatibility:** Support both `expression` and `transformation` during transition period

---

## YAML Syntax Design

### Basic Transformation (Simple Syntax)

```yaml
transformations:
  - id: "uppercase-name"
    name: "Uppercase Name Transformation"
    type: "field-transformation"
    enabled: true
    priority: 100

    # When to apply this transformation
    condition: "#data.name != null"

    # Simple field transformation using standardized "expression" property
    source-field: "name"
    target-field: "name_upper"
    expression: "#value.toUpperCase()"  # ← Standardized on "expression"
```

**Note:** This simple syntax is similar to field-mappings but at the top level.

### Complex Transformation with Rules (Advanced Syntax)

```yaml
transformations:
  - id: "status-normalization"
    name: "Status Code Normalization"
    type: "conditional-transformation"
    enabled: true

    transformation-rules:
      # Rule 1: Handle active statuses
      - condition: "{'A', 'ACTIVE', '1'}.contains(#data.status)"
        actions:
          - type: "set-field"
            field: "normalized_status"
            value: "ACTIVE"
          - type: "set-field"
            field: "status_code"
            value: 1

      # Rule 2: Handle inactive statuses
      - condition: "{'I', 'INACTIVE', '0'}.contains(#data.status)"
        actions:
          - type: "set-field"
            field: "normalized_status"
            value: "INACTIVE"
          - type: "set-field"
            field: "status_code"
            value: 0

      # Default: Unknown status
      - condition: "true"
        actions:
          - type: "set-field"
            field: "normalized_status"
            value: "UNKNOWN"
          - type: "set-field"
            field: "status_code"
            value: -1
```

**Note:** Actions can use `expression` for dynamic values or `value` for static values.

### Multiple Field Transformations

```yaml
transformations:
  - id: "currency-conversion"
    name: "Currency Conversion"
    type: "multi-field-transformation"
    condition: "#data.currency == 'USD'"

    transformation-rules:
      - actions:
          - type: "calculate-field"
            field: "amount_eur"
            expression: "#data.amount * 0.85"  # ← Uses "expression"

          - type: "calculate-field"
            field: "amount_gbp"
            expression: "#data.amount * 0.73"  # ← Uses "expression"

          - type: "set-field"
            field: "conversion_date"
            expression: "T(java.time.LocalDateTime).now()"  # ← Uses "expression"
```

### Backward Compatibility Example

```yaml
# OLD SYNTAX (still supported during transition):
enrichments:
  - id: "my-enrichment"
    field-mappings:
      - source-field: "amount"
        target-field: "adjusted_amount"
        expression: "#value * 1.1"  # ← Old property name (deprecated)

# NEW SYNTAX (recommended):
enrichments:
  - id: "my-enrichment"
    field-mappings:
      - source-field: "amount"
        target-field: "adjusted_amount"
        expression: "#value * 1.1"  # ← New standardized property name
```

**Implementation Note:** The code will check for `expression` first, then fall back to `transformation` if not found, ensuring backward compatibility.

---

## Implementation Architecture

### 1. Core Components

```
YamlTransformationProcessor (NEW)
├── Executes transformations from top-level section
├── Applies transformations in document order
├── Evaluates conditions before execution
└── Integrates with SequentialYamlProcessor

YamlTransformation (EXISTS)
├── YAML configuration model
├── Already has all required fields
└── No changes needed

SequentialYamlProcessor (UPDATE)
├── processTransformations() - needs implementation
└── Call YamlTransformationProcessor

RulesEngine (UPDATE)
└── Ensure transformations are included in processing pipeline
```

### 2. Processing Flow

```
1. OrderedYamlParser parses YAML
   ↓
2. Transformations added to itemOrder (already works)
   ↓
3. SequentialYamlProcessor encounters "transformations" section
   ↓
4. processTransformations() calls YamlTransformationProcessor (NEW)
   ↓
5. YamlTransformationProcessor executes each expression:
   - Evaluate condition
   - Apply transformation rules/actions
   - Update data context
   ↓
6. Continue with next section in document order
```

### 3. Integration Points

**With Enrichments:**
```yaml
# Transformations can run before enrichments
transformations:
  - id: "normalize-input"
    # ... normalize data

enrichments:
  - id: "lookup-customer"
    # ... uses normalized data
```

**With Rules:**
```yaml
# Transformations can run before rules
transformations:
  - id: "calculate-risk-score"
    # ... calculate score

rules:
  - id: "validate-risk"
    condition: "#data.risk_score > 70"
    # ... validate using transformed data
```

---

## Implementation Plan

### Phase 1: API Standardization (1 day)

**Task 1.1: Add `expression` Property to YamlEnrichment.FieldMapping**
- Add `@JsonProperty("expression")` field
- Update getter to check `expression` first, then fall back to `transformation`
- Add deprecation warning when `transformation` is used
- Update unit tests

**Task 1.2: Update YamlEnrichmentProcessor**
- Modify `applyFieldMappings()` to use new getter logic
- Log deprecation warning when `transformation` property is detected
- Ensure backward compatibility

**Task 1.3: Update Documentation**
- Update all examples in `APEX_YAML_REFERENCE.md` to use `expression`
- Add migration guide for users
- Document backward compatibility period

### Phase 2: Core Transformation Implementation (2-3 days)

**Task 2.1: Create YamlTransformationProcessor**
- Create new service class: `YamlTransformationProcessor.java`
- Implement transformation execution logic
- Support basic field transformations with SpEL expressions
- Handle conditional execution
- Use standardized `expression` property

**Task 2.2: Update SequentialYamlProcessor**
- Implement `processTransformations()` method
- Call YamlTransformationProcessor for each transformation
- Ensure document order is preserved

**Task 2.3: Integration with RulesEngine**
- Ensure RulesEngine processes transformations section
- Add transformations to execution pipeline
- Test with existing enrichments and rules

### Phase 3: Advanced Features (1-2 days)

**Task 3.1: Transformation Rules Support**
- Implement `transformation-rules` with conditions
- Support `actions` and `else-actions`
- Handle multiple action types:
  - `set-field`
  - `calculate-field`
  - `copy-field`
  - `remove-field`
- All actions use `expression` property consistently

**Task 3.2: Error Handling**
- Add error recovery for transformation failures
- Support `error-recovery` configuration
- Log transformation errors appropriately

### Phase 4: Testing (1 day)

**Task 4.1: Fix Existing Test**
- Fix `Test8_TransformationsBasicTest` (currently failing)
- Verify document order execution
- Test with ExecutionTracker

**Task 4.2: Backward Compatibility Tests**
- Test that old `transformation` property still works
- Test that new `expression` property works
- Test that `expression` takes precedence over `transformation`
- Verify deprecation warnings are logged

**Task 4.3: Comprehensive Tests**
- Test transformations with enrichments
- Test transformations with rules
- Test conditional transformations
- Test transformation-rules with actions
- Test error handling

**Task 4.4: Integration Tests**
- Test in sequential processing mode
- Test with numbered suffixes (transformations-1, transformations-2)
- Test with transformation-refs (if needed)

### Phase 5: Documentation (0.5 days)

**Task 5.1: Update Documentation**
- Update `APEX_YAML_REFERENCE.md` with transformations section
- Update all examples to use `expression` instead of `transformation`
- Add migration guide for `transformation` → `expression`
- Add examples to `APEX_RULES_ENGINE_USER_GUIDE.md`
- Update `UNIMPLEMENTED_KEYWORDS_DESIGN_STATUS.md`
- Document deprecation timeline for `transformation` property

---

## Example Use Cases

### Use Case 1: Data Normalization

```yaml
# Normalize incoming data before enrichment
transformations:
  - id: "normalize-currency"
    condition: "#data.currency != null"
    source-field: "currency"
    target-field: "currency"
    expression: "#value.toUpperCase().trim()"  # ← Uses "expression"

  - id: "normalize-amount"
    condition: "#data.amount != null"
    source-field: "amount"
    target-field: "amount"
    expression: "T(java.math.BigDecimal).valueOf(#value)"  # ← Uses "expression"

enrichments:
  - id: "lookup-fx-rate"
    # Uses normalized currency
    field-mappings:
      - source-field: "currency"
        target-field: "fx_currency"
        expression: "#value"  # ← NEW: Uses "expression" instead of "transformation"
```

### Use Case 2: Calculated Fields

```yaml
# Calculate derived fields before validation
transformations:
  - id: "calculate-total"
    type: "multi-field-transformation"
    transformation-rules:
      - actions:
          - type: "calculate-field"
            field: "total_amount"
            expression: "#data.quantity * #data.unit_price"
          
          - type: "calculate-field"
            field: "tax_amount"
            expression: "#data.total_amount * 0.20"
          
          - type: "calculate-field"
            field: "grand_total"
            expression: "#data.total_amount + #data.tax_amount"

rules:
  - id: "validate-total"
    condition: "#data.grand_total > 10000"
    # Validate calculated total
```

### Use Case 3: Conditional Data Mapping

```yaml
# Map data differently based on source system
transformations:
  - id: "system-specific-mapping"
    type: "conditional-transformation"
    transformation-rules:
      - condition: "#data.source_system == 'SWIFT'"
        actions:
          - type: "set-field"
            field: "is_ndf"
            expression: "#data.ndf_flag == '1' ? true : false"
      
      - condition: "#data.source_system == 'REUTERS'"
        actions:
          - type: "set-field"
            field: "is_ndf"
            expression: "#data.ndf_indicator?.toUpperCase() == 'TRUE'"
      
      - condition: "true"
        actions:
          - type: "set-field"
            field: "is_ndf"
            value: false
```

---

## Technical Considerations

### 1. Performance
- **Concern:** Additional processing overhead
- **Mitigation:** 
  - Cache compiled SpEL expressions
  - Skip disabled transformations early
  - Use condition evaluation to avoid unnecessary work

### 2. Error Handling
- **Concern:** Transformation failures breaking processing
- **Mitigation:**
  - Integrate with existing error-recovery system
  - Support severity levels (ERROR, WARNING, INFO)
  - Provide clear error messages with transformation ID

### 3. Backward Compatibility
- **Concern:** Breaking existing configurations
- **Mitigation:**
  - Top-level transformations are NEW feature (no breaking changes)
  - Field-mapping transformations continue to work unchanged
  - Pipeline transformations continue to work unchanged

### 4. Document Order
- **Concern:** Transformations must respect YAML order
- **Mitigation:**
  - Already handled by OrderedYamlParser
  - SequentialYamlProcessor ensures order preservation
  - Tests validate execution order with ExecutionTracker

---

## Success Criteria

1. ✅ `Test8_TransformationsBasicTest` passes
2. ✅ Transformations execute in exact document order
3. ✅ Transformations work with enrichments and rules
4. ✅ Conditional transformations work correctly
5. ✅ Transformation-rules with actions work correctly
6. ✅ Error handling integrates with error-recovery system
7. ✅ Documentation is complete and accurate
8. ✅ No breaking changes to existing functionality

---

## Estimated Effort

| Phase | Tasks | Effort |
|-------|-------|--------|
| Phase 1: API Standardization | 3 tasks | 1 day |
| Phase 2: Core Implementation | 3 tasks | 2-3 days |
| Phase 3: Advanced Features | 2 tasks | 1-2 days |
| Phase 4: Testing | 4 tasks | 1 day |
| Phase 5: Documentation | 1 task | 0.5 days |
| **TOTAL** | **13 tasks** | **5.5-7.5 days** |

**Note:** The additional 1 day is for API standardization work to address the `transformation` vs `expression` inconsistency.

---

## Next Steps

1. **Review and Approve Design** - Get stakeholder approval for API standardization
2. **Create Implementation Tasks** - Break down into JIRA tickets
3. **Implement Phase 1** - API Standardization (`transformation` → `expression`)
4. **Implement Phase 2** - Core transformation functionality
5. **Test and Iterate** - Fix Test8_TransformationsBasicTest
6. **Complete Phases 3-5** - Advanced features, testing, documentation
7. **Release** - Include in APEX 3.1

## Deprecation Timeline

### Version 3.1 (Current)
- ✅ Add `expression` property to all relevant classes
- ✅ Support both `expression` and `transformation` (backward compatible)
- ✅ Log deprecation warnings when `transformation` is used
- ✅ Update all documentation to use `expression`

### Version 3.2 (Next Minor Release)
- ⚠️ Continue supporting both properties
- ⚠️ Increase deprecation warning visibility
- ⚠️ Add migration guide to release notes

### Version 4.0 (Next Major Release)
- ❌ Remove `transformation` property entirely
- ✅ Only support `expression` property
- ✅ Breaking change documented in migration guide

---

## Appendix A: YamlTransformation Class Structure

The `YamlTransformation.java` class already exists with the following structure:

```java
public class YamlTransformation {
    private String id;
    private String name;
    private String description;
    private String type;  // field-transformation, conditional-transformation, etc.
    private Boolean enabled;
    private Integer priority;
    private String condition;  // SpEL condition
    private List<TransformationRule> transformationRules;

    // NEW: Add simple transformation support
    private String sourceField;
    private String targetField;
    private String expression;  // ← Already uses "expression" (good!)

    public static class TransformationRule {
        private String condition;
        private List<TransformationAction> actions;
        private List<TransformationAction> elseActions;
    }

    public static class TransformationAction {
        private String type;  // set-field, calculate-field, copy-field, remove-field
        private String field;
        private String sourceField;
        private String expression;  // ← Already uses "expression" (good!)
        private Object value;
    }
}
```

**Note:** The `YamlTransformation` class already uses `expression` consistently! We need to add `sourceField` and `targetField` for simple transformations.

---

## Appendix B: YamlEnrichment.FieldMapping Changes

**Current Structure:**
```java
public static class FieldMapping {
    @JsonProperty("source-field")
    private String sourceField;

    @JsonProperty("target-field")
    private String targetField;

    @JsonProperty("transformation")  // ← OLD property name
    private String transformation;

    @JsonProperty("default-value")
    private Object defaultValue;

    @JsonProperty("required")
    private Boolean required;
}
```

**Updated Structure (Backward Compatible):**
```java
public static class FieldMapping {
    @JsonProperty("source-field")
    private String sourceField;

    @JsonProperty("target-field")
    private String targetField;

    @JsonProperty("transformation")  // ← OLD property (deprecated)
    private String transformation;

    @JsonProperty("expression")  // ← NEW property (recommended)
    private String expression;

    @JsonProperty("default-value")
    private Object defaultValue;

    @JsonProperty("required")
    private Boolean required;

    // Updated getter with backward compatibility
    public String getExpression() {
        // Prefer new "expression" property, fall back to old "transformation"
        if (expression != null) {
            return expression;
        }
        if (transformation != null) {
            LOGGER.warning("DEPRECATED: 'transformation' property is deprecated. " +
                          "Please use 'expression' instead.");
            return transformation;
        }
        return null;
    }

    // Keep old getter for backward compatibility
    @Deprecated
    public String getTransformation() {
        return getExpression();  // Delegate to new getter
    }
}
```

---

## Appendix C: Migration Examples

### Before (Old Syntax):
```yaml
enrichments:
  - id: "price-adjustment"
    field-mappings:
      - source-field: "price"
        target-field: "adjusted_price"
        expression: "#value * 1.1"  # ← OLD

      - source-field: "status"
        target-field: "status_code"
        expression: |  # ← OLD
          #value == 'ACTIVE' ? 1 : 0
```

### After (New Syntax):
```yaml
enrichments:
  - id: "price-adjustment"
    field-mappings:
      - source-field: "price"
        target-field: "adjusted_price"
        expression: "#value * 1.1"  # ← NEW

      - source-field: "status"
        target-field: "status_code"
        expression: |  # ← NEW
          #value == 'ACTIVE' ? 1 : 0
```

### Mixed (Transition Period):
```yaml
enrichments:
  - id: "price-adjustment"
    field-mappings:
      # Old syntax still works (with deprecation warning)
      - source-field: "price"
        target-field: "adjusted_price"
        expression: "#value * 1.1"  # ← Still works

      # New syntax preferred
      - source-field: "status"
        target-field: "status_code"
        expression: "#value == 'ACTIVE' ? 1 : 0"  # ← Recommended
```

---

**End of Document**

