# APEX Conditional Processing Guide - Corrections Log

**Date:** 2025-10-09  
**Document:** APEX_CONDITIONAL_PROCESSING_GUIDE.md  
**Status:** Hallucinations Identified and Corrected

---

## Summary

During validation against actual APEX implementation, several hallucinated YAML keywords and incorrect syntax patterns were identified and corrected in the Conditional Processing Guide.

---

## Hallucinations Found and Fixed

### 1. Calculation Enrichment Syntax - CRITICAL ERROR

**Hallucinated Syntax:**
```yaml
enrichments:
  - id: "fee-calculation"
    type: "calculation-enrichment"
    calculations:                    # ❌ WRONG - This property doesn't exist
      - field: "baseFee"             # ❌ WRONG - Not a list structure
        expression: "#amount * 0.01"
      - field: "finalFee"
        expression: "#baseFee * 1.1"
```

**Correct Syntax:**
```yaml
enrichments:
  - id: "base-fee-calculation"
    type: "calculation-enrichment"
    calculation-config:              # ✅ CORRECT - Single calculation per enrichment
      expression: "#amount * 0.01"
      result-field: "baseFee"        # ✅ CORRECT - Explicit result field
    field-mappings:
      - source-field: "baseFee"
        target-field: "baseFee"
```

**Impact:** HIGH - This was used in 3 examples in the guide  
**Root Cause:** Confusion between desired API design and actual implementation  
**Locations Fixed:**
- Line 487-521: Pattern 4 - Conditional Calculations
- Line 604-623: Performance Considerations - Cache Expensive Calculations
- Line 931-962: Complete Example - Financial Transaction Processing

**Note:** The same error was also found and fixed in `APEX_YAML_REFERENCE.md` (lines 1675-1754)

---

### 2. REST API Lookup Configuration - CRITICAL ERROR

**Hallucinated Syntax:**
```yaml
enrichments:
  - id: "api-lookup"
    type: "lookup-enrichment"
    lookup-config:
      lookup-dataset:
        type: "rest-api"
        base-url: "https://api.example.com"  # ❌ WRONG - base-url not valid here
        endpoint: "/data/{id}"               # ❌ WRONG - endpoint not valid here
```

**Correct Syntax:**
```yaml
# Define data source first
data-sources:
  - name: "example-api"
    type: "rest-api"
    connection:
      base-url: "https://api.example.com"    # ✅ CORRECT - In data source connection
      timeout: 5000
    endpoints:
      get-data: "/data/{id}"                 # ✅ CORRECT - In data source endpoints

enrichments:
  - id: "api-lookup"
    type: "lookup-enrichment"
    lookup-config:
      lookup-key: "#id"
      lookup-dataset:
        type: "rest-api"
        data-source-ref: "example-api"       # ✅ CORRECT - Reference to data source
        operation-ref: "get-data"            # ✅ CORRECT - Reference to endpoint
```

**Impact:** HIGH - This was used in 2 examples in the guide  
**Root Cause:** Misunderstanding of APEX's data source architecture - REST API configuration must be defined in `data-sources` section and referenced, not inlined  
**Locations Fixed:**
- Line 399-471: Pattern 2 - Fallback Logic with Rule Results
- Line 1265-1295: Conditional Enrichment with Rule Results

---

## Validation Methodology

1. **Codebase Search:** Searched actual YAML test files in `apex-demo/src/test/java/dev/mars/apex/demo/` for real usage patterns
2. **Class Inspection:** Reviewed `YamlEnrichment.java` to verify actual `@JsonProperty` annotations
3. **Cross-Reference:** Compared guide examples against working test configurations

---

## Verified Correct Keywords

The following keywords were validated as correct in the guide:

### Conditional Mapping Enrichment
- ✅ `type: "conditional-mapping-enrichment"` - Correct
- ✅ `target-field` - Correct
- ✅ `mapping-rules` - Correct
- ✅ `execution-settings` - Correct
  - ✅ `stop-on-first-match` - Correct
  - ✅ `log-matched-rule` - Correct
  - ✅ `validate-result` - Correct

### Mapping Rules
- ✅ `id` - Correct
- ✅ `name` - Correct
- ✅ `priority` - Correct
- ✅ `conditions` - Correct
  - ✅ `operator` - Correct (AND/OR)
  - ✅ `rules` - Correct
- ✅ `mapping` - Correct
  - ✅ `type` - Correct (direct/lookup)
  - ✅ `transformation` - Correct
  - ✅ `source-field` - Correct

### Context Variables
- ✅ `#ruleResults['rule-id']` - Correct
- ✅ `#ruleGroupResults['group-id']['passed']` - Correct
- ✅ `#ruleGroupResults['group-id']['failedRules']` - Correct
- ✅ `#ruleGroupResults['group-id']['passedRules']` - Correct

### Field Enrichment
- ✅ `type: "field-enrichment"` - Correct
- ✅ `field-mappings` - Correct
- ✅ `condition` - Correct

### Lookup Enrichment
- ✅ `type: "lookup-enrichment"` - Correct
- ✅ `lookup-config` - Correct
- ✅ `lookup-key` - Correct
- ✅ `lookup-dataset` - Correct
  - ✅ `type: "inline"` - Correct
  - ✅ `type: "yaml-file"` - Correct
  - ✅ `type: "csv-file"` - Correct
  - ✅ `type: "database"` - Correct
  - ✅ `type: "rest-api"` - Correct (with data-source-ref)
  - ✅ `data-source-ref` - Correct
  - ✅ `operation-ref` - Correct
  - ✅ `connection-name` - Correct (alternative to data-source-ref)
  - ✅ `key-field` - Correct
  - ✅ `data` - Correct (for inline datasets)

---

## Lessons Learned

1. **Always Validate Against Implementation:** Never assume YAML structure without checking actual `@JsonProperty` annotations in Java classes
2. **Check Test Files:** Real test YAML files are the source of truth for valid syntax
3. **Beware of Logical Assumptions:** Just because a structure seems logical doesn't mean it's implemented that way
4. **Multi-Step Processes:** Some features (like calculations) require multiple enrichments, not list-based configurations

---

## Documents Updated

1. ✅ `docs/APEX_CONDITIONAL_PROCESSING_GUIDE.md` - Fixed 5 locations
2. ✅ `docs/APEX_YAML_REFERENCE.md` - Fixed 1 location (calculation-enrichment section)

---

## Verification Status

- ✅ All calculation-enrichment examples now use `calculation-config` with `expression` and `result-field`
- ✅ All REST API lookups now properly reference data sources defined in `data-sources` section
- ✅ All other YAML keywords verified against actual implementation
- ✅ No remaining hallucinated keywords identified

---

**Validation Complete:** 2025-10-09  
**Validator:** APEX Documentation Team  
**Status:** READY FOR PUBLICATION

