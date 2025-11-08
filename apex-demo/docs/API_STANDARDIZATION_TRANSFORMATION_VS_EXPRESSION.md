# APEX API Standardization: `transformation` vs `expression`

**Document Version:** 1.0  
**Date:** November 8, 2025  
**Author:** APEX Development Team  
**Status:** Approved Design  
**Target Version:** APEX 3.1

---

## Executive Summary

This document addresses a critical **API inconsistency** in APEX where the same concept (SpEL expressions for transforming/calculating values) is called `transformation` in some places and `expression` in others.

**Decision:** Standardize on **`expression`** throughout APEX, with backward compatibility for `transformation`.

---

## The Problem

### Current Inconsistent State

| Context | Property Name | Purpose |
|---------|---------------|---------|
| **Field Mappings** | `transformation` | SpEL expression to transform a value |
| **Calculation Config** | `expression` | SpEL expression to calculate a value |
| **Transformation Actions** | `expression` | SpEL expression to transform a value |

**They all do exactly the same thing:** Parse and evaluate a SpEL expression!

### Code Evidence

**Field Mappings (uses `transformation`):**
```java
// YamlEnrichment.FieldMapping
@JsonProperty("transformation")
private String transformation; // SpEL expression for field transformation
```

**Calculation Config (uses `expression`):**
```java
// YamlEnrichment.CalculationConfig
@JsonProperty("expression")
private String expression; // SpEL expression for calculation
```

**Transformation Actions (uses `expression`):**
```java
// YamlTransformation.TransformationAction
@JsonProperty("expression")
private String expression; // SpEL expression for the transformation
```

### Processing Evidence

Both properties are processed **identically**:

```java
// Line 780-781 in YamlEnrichmentProcessor.java
if (mapping.getTransformation() != null && !mapping.getTransformation().trim().isEmpty()) {
    valueToSet = applyTransformation(mapping.getTransformation(), valueToSet, targetObject);
}

// Line 404 in YamlEnrichmentProcessor.java
Expression calcExpr = getOrCompileExpression(calcConfig.getExpression());
Object result = calcExpr.getValue(context);
```

**Both call the same underlying SpEL evaluation logic!**

---

## The Solution

### Standardize on `expression`

**Rationale:**
1. ✅ **Technical accuracy** - It's literally a SpEL expression
2. ✅ **Consistency with SpEL** - Spring Expression Language calls them "expressions"
3. ✅ **Already used in 2 places** - `calculation-config` and `TransformationAction`
4. ✅ **Shorter and clearer** - More concise than "transformation"
5. ✅ **Industry standard** - Most expression languages use "expression" terminology

---

## Implementation Strategy

### Phase 1: Add Support for Both (APEX 3.1)

**Changes to YamlEnrichment.FieldMapping:**

```java
public static class FieldMapping {
    @JsonProperty("source-field")
    private String sourceField;
    
    @JsonProperty("target-field")
    private String targetField;
    
    @JsonProperty("transformation")  // OLD - keep for backward compatibility
    private String transformation;
    
    @JsonProperty("expression")  // NEW - recommended property
    private String expression;
    
    @JsonProperty("default-value")
    private Object defaultValue;
    
    @JsonProperty("required")
    private Boolean required;
    
    /**
     * Get the expression/transformation value.
     * Prefers new "expression" property, falls back to old "transformation".
     */
    public String getExpression() {
        if (expression != null) {
            return expression;
        }
        if (transformation != null) {
            LOGGER.warning("DEPRECATED: 'transformation' property is deprecated in field-mappings. " +
                          "Please use 'expression' instead. Field: " + targetField);
            return transformation;
        }
        return null;
    }
    
    /**
     * @deprecated Use getExpression() instead
     */
    @Deprecated
    public String getTransformation() {
        return getExpression();
    }
}
```

**Changes to YamlEnrichmentProcessor:**

```java
// Update all usages to call getExpression() instead of getTransformation()
if (mapping.getExpression() != null && !mapping.getExpression().trim().isEmpty()) {
    valueToSet = applyTransformation(mapping.getExpression(), valueToSet, targetObject);
}
```

### Phase 2: Deprecation Warnings (APEX 3.2)

- Increase visibility of deprecation warnings
- Add to release notes
- Provide migration guide

### Phase 3: Remove Old Property (APEX 4.0)

- Remove `transformation` property entirely
- Breaking change in major version
- Full migration guide provided

---

## YAML Syntax Changes

### Before (Old Syntax - Still Works):

```yaml
enrichments:
  - id: "price-adjustment"
    field-mappings:
      - source-field: "price"
        target-field: "adjusted_price"
        expression: "#value * 1.1"  # ← OLD property name
```

### After (New Syntax - Recommended):

```yaml
enrichments:
  - id: "price-adjustment"
    field-mappings:
      - source-field: "price"
        target-field: "adjusted_price"
        expression: "#value * 1.1"  # ← NEW property name
```

### Transition Period (Both Work):

```yaml
enrichments:
  - id: "price-adjustment"
    field-mappings:
      # Old syntax (deprecated but still works)
      - source-field: "price"
        target-field: "adjusted_price"
        expression: "#value * 1.1"  # ← Logs deprecation warning
      
      # New syntax (recommended)
      - source-field: "discount"
        target-field: "discount_amount"
        expression: "#value * 0.1"  # ← No warning
```

---

## Impact Analysis

### Files Requiring Changes

1. **apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlEnrichment.java**
   - Add `expression` property to `FieldMapping` class
   - Update getter logic for backward compatibility

2. **apex-core/src/main/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessor.java**
   - Update all calls from `getTransformation()` to `getExpression()`
   - Add deprecation logging

3. **apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlTransformation.java**
   - Add `sourceField`, `targetField`, `expression` for simple transformations
   - Already uses `expression` in `TransformationAction` (no change needed)

4. **Documentation Files**
   - Update all YAML examples to use `expression`
   - Add migration guide
   - Document deprecation timeline

### Test Files Requiring Updates

- Update all test YAML files to use `expression` (optional - old syntax still works)
- Add tests for backward compatibility
- Add tests for deprecation warnings

---

## Benefits

1. **Consistency** - One property name for one concept
2. **Clarity** - Clear that it's a SpEL expression
3. **Maintainability** - Easier to understand and maintain
4. **Standards Compliance** - Aligns with Spring Expression Language terminology
5. **Future-Proof** - Cleaner API for future development

---

## Risks and Mitigation

### Risk 1: Breaking Existing Configurations

**Mitigation:**
- Support both properties during transition (3.1, 3.2)
- Only remove old property in major version (4.0)
- Provide clear migration guide
- Log deprecation warnings

### Risk 2: User Confusion During Transition

**Mitigation:**
- Clear documentation of both syntaxes
- Deprecation warnings guide users to new syntax
- Migration examples in release notes
- Update all official examples immediately

### Risk 3: Large Codebase Updates

**Mitigation:**
- Provide automated migration script (optional)
- Gradual rollout over multiple versions
- Backward compatibility ensures no immediate breakage

---

## Migration Guide for Users

### Step 1: Identify Usage (APEX 3.1+)

Run your APEX application and check logs for deprecation warnings:

```
WARN: DEPRECATED: 'transformation' property is deprecated in field-mappings. 
Please use 'expression' instead. Field: adjusted_price
```

### Step 2: Update YAML Files

Use find-and-replace in your YAML files:

**Find:** `expression:`  
**Replace:** `expression:`

**Context:** Only within `field-mappings` sections

### Step 3: Test

Run your tests to ensure everything still works with the new syntax.

### Step 4: Remove Old Syntax (Before APEX 4.0)

Ensure all YAML files use `expression` before upgrading to APEX 4.0.

---

## Timeline

| Version | Date | Action |
|---------|------|--------|
| **APEX 3.1** | Q1 2026 | Add `expression` property, support both, log warnings |
| **APEX 3.2** | Q2 2026 | Continue support, increase warning visibility |
| **APEX 4.0** | Q4 2026 | Remove `transformation` property (breaking change) |

---

## Success Criteria

1. ✅ Both `expression` and `transformation` work in APEX 3.1
2. ✅ Deprecation warnings logged when `transformation` is used
3. ✅ All documentation updated to use `expression`
4. ✅ Migration guide provided
5. ✅ Backward compatibility tests pass
6. ✅ No breaking changes in APEX 3.x
7. ✅ Clean removal in APEX 4.0

---

## Approval

- [ ] Technical Lead Approval
- [ ] Product Owner Approval
- [ ] Documentation Team Approval
- [ ] QA Team Approval

---

**End of Document**

