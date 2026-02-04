# APEX Error Code and Success Code Keywords Design

## Overview
Add two optional keywords `error-code` and `success-code` to Rules and Enrichments that allow applications to attach business-specific codes to rule/enrichment outcomes. These codes can be constants or SpEL expressions, enabling integration with external systems that require specific error/success codes.

## Problem Statement
- Applications need to map APEX rule/enrichment results to external system codes (e.g., HTTP status codes, business error codes)
- Currently, only `message` field is available for result communication
- No standardized way to attach machine-readable codes to outcomes
- Requires post-processing in application code to map results to codes

## Solution Design

### 1. YAML Syntax

**For Rules:**
```yaml
rules:
  - id: "high-value-check"
    name: "High Value Transaction Check"
    condition: "#amount > 100000"
    message: "Transaction exceeds limit"
    severity: "ERROR"
    success-code: "HV001"              # Constant code when rule matches
    error-code: "HV_FAILED"            # Constant code when rule fails
    map-to-field: "validationCode = #success-code"  # Map code to dataset field
```

**For Enrichments:**
```yaml
enrichments:
  - id: "customer-lookup"
    type: "lookup-enrichment"
    condition: "#customerId != null"
    success-code: "CUST_ENRICHED"      # Constant code on success
    error-code: "#errorCode"           # SpEL expression on failure
    map-to-field: "lookupStatus = #success-code"  # Map code to dataset field
    lookup-config:
      lookup-key: "#customerId"
      lookup-dataset:
        type: "inline"
        data: [...]
    field-mappings: [...]
```

**Multiple Field Mappings:**
```yaml
rules:
  - id: "complex-validation"
    condition: "#amount > 0"
    success-code: "VALID"
    error-code: "INVALID"
    map-to-field:
      - "status = #success-code"
      - "timestamp = T(java.time.Instant).now()"
      - "severity = #severity"
```

### 2. Implementation Scope

**Add to YamlRule class:**
- `@JsonProperty("success-code")` - String field (constant or SpEL)
- `@JsonProperty("error-code")` - String field (constant or SpEL)
- `@JsonProperty("map-to-field")` - String or List<String> field (SpEL field mapping expressions)

**Add to YamlEnrichment class:**
- `@JsonProperty("success-code")` - String field (constant or SpEL)
- `@JsonProperty("error-code")` - String field (constant or SpEL)
- `@JsonProperty("map-to-field")` - String or List<String> field (SpEL field mapping expressions)

**Add to Rule class:**
- `successCode` - String field
- `errorCode` - String field
- `mapToField` - String or List<String> field

**Add to RuleResult class:**
- `successCode` - String field (populated when rule matches)
- `errorCode` - String field (populated when rule fails)
- `mapToField` - String or List<String> field (field mapping expressions)

### 3. Evaluation Logic

**For Rules:**
- When rule condition evaluates to TRUE: evaluate `success-code` (if present), store in `RuleResult.successCode`
- When rule condition evaluates to FALSE: evaluate `error-code` (if present), store in `RuleResult.errorCode`

**For Enrichments:**
- When enrichment succeeds: evaluate `success-code` (if present), store in result
- When enrichment fails: evaluate `error-code` (if present), store in result

**Code Evaluation:**
- If code is a SpEL expression (contains `#` or `T(`): parse and evaluate using ExpressionEvaluatorService
- If code is a constant string: use as-is
- If code is null/empty: leave result field null

### 3a. Generic Field Mapping with `map-to-field`

**Optional Field: `map-to-field`**
- Generic SpEL-based field mapping that writes any value to the dataset being processed
- Enables flexible enrichment of data with codes, messages, timestamps, or any computed values
- Works with both single mappings (string) and multiple mappings (list)
- Available context: `#success-code`, `#error-code`, `#message`, `#severity`, and all input data fields

**Single Field Mapping:**
```yaml
rules:
  - id: "validation-rule"
    condition: "#amount > 0"
    success-code: "VALID"
    error-code: "INVALID"
    map-to-field: "validationCode = #success-code"  # Map code to dataset field
```

**Multiple Field Mappings:**
```yaml
enrichments:
  - id: "customer-lookup"
    type: "lookup-enrichment"
    success-code: "CUST_FOUND"
    error-code: "CUST_NOT_FOUND"
    map-to-field:
      - "lookupStatus = #success-code"
      - "lookupTimestamp = T(java.time.Instant).now()"
      - "lookupMessage = #message"
    field-mappings: [...]
```

**Behavior:**
- After rule/enrichment execution, evaluate each mapping expression
- Left side of `=` is the target field name in the dataset
- Right side of `=` is a SpEL expression evaluated with available context
- Allows downstream rules/enrichments to reference mapped values via SpEL
- Enables audit trails, status tracking, and flexible data enrichment through the pipeline

### 4. Backward Compatibility
- Both keywords are optional (default: null)
- Existing YAML files without these keywords continue to work unchanged
- No changes to existing RuleResult API - new fields are additive
- No changes to rule/enrichment execution logic

### 5. Use Cases

**HTTP Status Codes with Field Mapping:**
```yaml
- id: "validation-rule"
  success-code: "200"
  error-code: "400"
  map-to-field: "httpStatus = #success-code"
```

**Business Error Codes with Enrichment:**
```yaml
- id: "compliance-check"
  success-code: "COMPLIANT"
  error-code: "#complianceFailureCode"
  map-to-field: "complianceStatus = #success-code"
```

**Comprehensive Audit Trail:**
```yaml
- id: "transaction-audit"
  success-code: "TX_APPROVED"
  error-code: "TX_REJECTED"
  map-to-field:
    - "auditCode = #success-code"
    - "auditMessage = #message"
    - "auditTimestamp = T(java.time.Instant).now()"
    - "auditSeverity = #severity"
```

**Chained Rules Using Field Mapping:**
```yaml
rules:
  - id: "first-validation"
    condition: "#amount > 0"
    success-code: "AMOUNT_VALID"
    map-to-field: "amountStatus = #success-code"

  - id: "second-validation"
    condition: "#amountStatus == 'AMOUNT_VALID' && #currency != null"
    success-code: "FULL_VALIDATION_PASSED"
    map-to-field: "validationResult = #success-code"
```

**Dynamic Mapping with Expressions:**
```yaml
- id: "risk-assessment"
  success-code: "LOW_RISK"
  error-code: "HIGH_RISK"
  map-to-field:
    - "riskLevel = #success-code"
    - "riskScore = #amount > 100000 ? 'HIGH' : 'LOW'"
    - "requiresApproval = #amount > 50000"
```

## Implementation Plan

### Phase 1: YAML Configuration Layer (YamlRule and YamlEnrichment)

**Files to Modify:**
- `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRule.java`
- `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlEnrichment.java`

**Changes:**
1. Add three new fields with @JsonProperty annotations (kebab-case):
   ```java
   @JsonProperty("success-code")
   private String successCode;

   @JsonProperty("error-code")
   private String errorCode;

   @JsonProperty("map-to-field")
   private Object mapToField;  // String or List<String>
   ```
2. Add getters and setters for all three fields
3. Add null-safe handling in constructors
4. No validation needed at YAML layer - fields are optional

**Testing:**
- Unit tests for YAML parsing with constant codes
- Unit tests for YAML parsing with SpEL codes
- Unit tests for single and multiple field mappings
- Verify Jackson deserialization works correctly

---

### Phase 2: Core Model Layer (Rule and Enrichment)

**Files to Modify:**
- `apex-core/src/main/java/dev/mars/apex/core/engine/model/Rule.java`
- `apex-core/src/main/java/dev/mars/apex/core/engine/model/Enrichment.java` (if exists)
- `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleFactory.java`

**Changes:**
1. Add three new immutable fields to Rule class:
   ```java
   private final String successCode;
   private final String errorCode;
   private final Object mapToField;  // String or List<String>
   ```
2. Update all Rule constructors to accept these fields
3. Add getters for all three fields
4. Update YamlRuleFactory.createRuleWithMetadata() to propagate fields from YamlRule to Rule:
   ```java
   String successCode = yamlRule.getSuccessCode();
   String errorCode = yamlRule.getErrorCode();
   Object mapToField = yamlRule.getMapToField();

   Rule createdRule = new Rule(ruleId, categories, name, condition, message,
                               description, priority, severity, metadata,
                               defaultValue, successCode, errorCode, mapToField);
   ```
5. Update RuleBuilder to support these fields

**Testing:**
- Unit tests for Rule construction with codes
- Unit tests for field propagation from YamlRule to Rule
- Verify immutability of new fields
- Backward compatibility tests with existing Rule constructors

---

### Phase 3: Result Layer (RuleResult and EnrichmentResult)

**Files to Modify:**
- `apex-core/src/main/java/dev/mars/apex/core/engine/model/RuleResult.java`
- `apex-core/src/main/java/dev/mars/apex/core/engine/model/EnrichmentResult.java` (if exists)

**Changes:**
1. Add three new fields to RuleResult:
   ```java
   private final String successCode;
   private final String errorCode;
   private final Object mapToField;
   ```
2. Update RuleResult constructors and factory methods (match, noMatch, error)
3. Add getters for all three fields
4. Update builder pattern if used
5. Ensure backward compatibility - new fields default to null

**Testing:**
- Unit tests for RuleResult with codes populated
- Unit tests for RuleResult with null codes
- Verify factory methods work with new fields
- Backward compatibility tests

---

### Phase 4: Evaluation and Mapping Logic

**Files to Modify:**
- `apex-core/src/main/java/dev/mars/apex/core/engine/config/RulesEngine.java`
- `apex-core/src/main/java/dev/mars/apex/core/service/enrichment/EnrichmentService.java`
- `apex-core/src/main/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessor.java`

**Changes:**

**In RulesEngine (for rules):**
1. After rule condition evaluation, evaluate success-code or error-code:
   ```java
   if (result != null && result) {
       String successCode = evaluateCode(rule.getSuccessCode(), context);
       RuleResult ruleResult = RuleResult.match(rule, rule.getMessage(),
                                                rule.getSeverity(), successCode);
       applyFieldMappings(ruleResult, rule.getMapToField(), context, enrichedData);
       return ruleResult;
   } else {
       String errorCode = evaluateCode(rule.getErrorCode(), context);
       RuleResult ruleResult = RuleResult.noMatch(rule.getName(), errorCode);
       applyFieldMappings(ruleResult, rule.getMapToField(), context, enrichedData);
       return ruleResult;
   }
   ```

2. Create helper methods:
   ```java
   private String evaluateCode(String code, StandardEvaluationContext context) {
       if (code == null || code.isEmpty()) return null;
       if (code.contains("#") || code.contains("T(")) {
           // SpEL expression
           Expression expr = parser.parseExpression(code);
           return expr.getValue(context, String.class);
       }
       // Constant string
       return code;
   }

   private void applyFieldMappings(RuleResult result, Object mapToField,
                                   StandardEvaluationContext context,
                                   Map<String, Object> enrichedData) {
       if (mapToField == null) return;

       List<String> mappings = mapToField instanceof List ?
           (List<String>) mapToField : List.of((String) mapToField);

       for (String mapping : mappings) {
           // Parse "fieldName = #expression"
           String[] parts = mapping.split("=", 2);
           if (parts.length == 2) {
               String fieldName = parts[0].trim();
               String expression = parts[1].trim();
               Expression expr = parser.parseExpression(expression);
               Object value = expr.getValue(context, Object.class);
               enrichedData.put(fieldName, value);
           }
       }
   }
   ```

**In EnrichmentService (for enrichments):**
- Apply same logic for enrichment success/error codes
- Populate enriched data with mapped fields
- Make mapped fields available to subsequent rules via context

**Testing:**
- Integration tests for code evaluation with real data
- Tests for SpEL expression evaluation in codes
- Tests for field mapping with single and multiple mappings
- Tests for downstream rule access to mapped fields
- Tests for null/empty code handling
- Tests for invalid SpEL expressions (should log warning, not break)

---

### Phase 5: Comprehensive Testing

**Test Files to Create:**
- `apex-demo/src/test/java/dev/mars/apex/demo/codes/ErrorSuccessCodesTest.java`
- `apex-demo/src/test/java/dev/mars/apex/demo/codes/FieldMappingTest.java`
- `apex-demo/src/test/java/dev/mars/apex/demo/codes/ChainedRulesWithCodesTest.java`

**Test Coverage:**
- Positive: Rules with success codes
- Positive: Rules with error codes
- Positive: Enrichments with codes
- Positive: Field mapping with single mapping
- Positive: Field mapping with multiple mappings
- Positive: Chained rules using mapped fields
- Positive: Dynamic expressions in mappings
- Negative: Invalid SpEL expressions (should not break)
- Negative: Null/empty codes (should be handled gracefully)
- Negative: Missing target fields in mappings
- Backward compatibility: Existing YAML without codes still works

**YAML Test Files:**
- `apex-demo/src/test/resources/codes/error-success-codes.yaml`
- `apex-demo/src/test/resources/codes/field-mapping.yaml`
- `apex-demo/src/test/resources/codes/chained-rules.yaml`

---

### Implementation Order

1. **Start with Phase 1** - Add YAML fields, test YAML parsing
2. **Then Phase 2** - Add model fields, test propagation
3. **Then Phase 3** - Add result fields, test result creation
4. **Then Phase 4** - Implement evaluation logic, test end-to-end
5. **Finally Phase 5** - Create comprehensive demo tests

**Key Principles:**
- Follow existing patterns from severity field implementation
- Use kebab-case for YAML keywords
- Maintain backward compatibility at every phase
- Test after each phase before moving to next
- Use existing ExpressionEvaluatorService for SpEL evaluation
- Log warnings for invalid expressions, don't throw exceptions

## Testing Strategy
- Unit tests for YAML parsing with constant and SpEL codes
- Integration tests for code evaluation with real data
- Tests for null/empty code handling
- Tests for SpEL expression evaluation in codes
- Tests for single field mapping with `map-to-field`
- Tests for multiple field mappings (list syntax)
- Tests for complex SpEL expressions in mappings
- Tests for chained rules using mapped fields
- Tests for field availability in downstream rules via SpEL
- Tests for dynamic expressions in mappings (ternary operators, method calls)
- Backward compatibility tests with existing YAML files

## Validation
- Codes are optional - no validation errors if missing
- SpEL expressions in codes use same validation as rule conditions
- Invalid SpEL expressions in codes log warnings, don't break execution
- Codes are strings only - no type conversion

## Documentation
- Update APEX_YAML_REFERENCE.md with new keywords
- Add examples to demo YAML files
- Document SpEL expression support in codes
- Add to user guide with use case examples

