# APEX Scenario Coding and Implementation Guidelines
**Purpose:** Prevent serious mistakes and ensure quality in APEX scenario development  
**Last Updated:** 2025-10-08  
**Status:** MANDATORY - Must follow for all scenario work

---

## Critical Mistakes to Avoid

### 1. NEVER Hallucinate YAML Syntax

**WRONG:**
```yaml
type: "business-rules"           # ❌ Not a valid APEX type
type: "enrichment-config"        # ❌ Not a valid APEX type
type: "classification-rules"     # ❌ Not a valid APEX type

business-rules-config:           # ❌ Not a valid APEX section
confidence-scoring:              # ❌ Not a valid APEX section
performance:                     # ❌ Not a valid APEX section
error-handling:                  # ❌ Not a valid APEX section
enrichment-config:               # ❌ Not a valid APEX section
```

**CORRECT:**
```yaml
type: "rule-config"              # ✅ Valid APEX type
type: "enrichment"               # ✅ Valid APEX type
type: "scenario"                 # ✅ Valid APEX type
type: "scenario-registry"        # ✅ Valid APEX type

rules:                           # ✅ Valid APEX section
enrichments:                     # ✅ Valid APEX section
scenario:                        # ✅ Valid APEX section
scenarios:                       # ✅ Valid APEX section (for registries)
```

**Valid APEX Document Types (ONLY these):**
- `rule-config`
- `enrichment`
- `dataset`
- `scenario`
- `scenario-registry`
- `bootstrap`
- `rule-chain`
- `external-data-config`
- `pipeline-config`

**How to Verify:**
1. Check `apex-compiler/src/main/java/dev/mars/apex/compiler/lexical/ApexYamlLexicalValidator.java`
2. Check `docs/APEX_YAML_REFERENCE.md`
3. Run APEX compiler validation test

---

### 2. ALWAYS Validate YAML with APEX Compiler

**MANDATORY PROCESS:**

1. **Create YAML file**
2. **Create compiler validation test** (or add to existing)
3. **Run validation BEFORE claiming success**
4. **Fix all errors**
5. **Only then run functional tests**

**Example Validation Test:**
```java
@Test
void testValidateMyNewYamlFiles() {
    ApexYamlLexicalValidator validator = new ApexYamlLexicalValidator();
    
    List<String> yamlFiles = List.of(
        "path/to/my-new-file.yaml"
    );
    
    for (String filePath : yamlFiles) {
        Path yamlFile = Paths.get("..").resolve(filePath);
        ValidationResult result = validator.validateFile(yamlFile);
        
        assertTrue(result.isValid(), 
            "YAML validation failed: " + result.getErrors());
    }
}
```

**Why This Matters:**
- Functional tests may pass even with invalid YAML (lenient parser)
- Compiler catches specification violations
- Prevents invalid examples from entering codebase
- Ensures consistency across all YAML files

---

### 3. Keep Examples SIMPLE

**WRONG - Over-complicated (370 lines!):**
```yaml
# APEX Input Data Classification Phase 2 Test - Business Rules
#
# PURPOSE:
# This file contains advanced business classification rules for Phase 2 testing,
# demonstrating sophisticated SpEL-based business logic, complex decision trees,
# and context-aware classification algorithms.
#
# PHASE 2 FEATURES DEMONSTRATED:
# - Complex SpEL expressions for business classification
# - Multi-criteria decision making using SpEL
# - Context-aware business rule evaluation
# - Advanced confidence scoring algorithms
# - Performance-optimized rule evaluation with caching
# ...
# (365 more lines of garbage)
```

**CORRECT - Simple and focused (20-30 lines):**
```yaml
metadata:
  id: "simple-validation-test"
  name: "Simple Validation Test"
  version: "1.0.0"
  description: "Simple validation rules for testing"
  type: "rule-config"
  author: "test@example.com"

rules:
  - id: "trade-id-required"
    name: "Trade ID Required"
    condition: "#data['tradeId'] != null"
    message: "Trade ID is required"
    severity: "ERROR"
    enabled: true
```

**Guidelines:**
- **Maximum 50 lines** for test YAML files (unless demonstrating complex real-world scenario)
- **One concept per file** - don't try to demonstrate everything at once
- **No promotional language** - "advanced", "sophisticated", "performance-optimized"
- **No fake features** - only demonstrate what actually exists
- **Clear purpose** - what specific functionality does this test?

---

### 4. Use Correct Metadata Fields

**Type-Specific Required Fields:**

| Document Type | Required Fields |
|---------------|-----------------|
| `rule-config` | `id`, `name`, `version`, `description`, `type`, `author` |
| `enrichment` | `id`, `name`, `version`, `description`, `type`, `author` |
| `dataset` | `id`, `name`, `version`, `description`, `type`, `source` |
| `scenario` | `id`, `name`, `version`, `description`, `type`, `business-domain`, `owner` |
| `scenario-registry` | `id`, `name`, `version`, `description`, `type`, `created-by` |
| `bootstrap` | `id`, `name`, `version`, `description`, `type`, `business-domain`, `created-by` |

**Common Mistakes:**
```yaml
# ❌ WRONG - Using 'author' for scenario
metadata:
  type: "scenario"
  author: "test@example.com"  # Wrong field!

# ✅ CORRECT - Using 'owner' for scenario
metadata:
  type: "scenario"
  owner: "test@example.com"   # Correct field!

# ❌ WRONG - Using 'scenario-registry' section
scenario-registry:
  - scenario-id: "test"

# ✅ CORRECT - Using 'scenarios' section
scenarios:
  - scenario-id: "test"
```

---

### 5. Follow Existing Patterns

**Before creating new code:**

1. **Search for similar functionality:**
   ```
   Use codebase-retrieval to find existing patterns
   ```

2. **Study the reference implementation:**
   ```
   View the file and understand the pattern
   ```

3. **Follow the same structure:**
   - Same naming conventions
   - Same file organization
   - Same test structure
   - Same logging patterns

**Reference Patterns for Scenarios:**
- `BasicYamlRuleGroupProcessingTest.java` - Test structure
- `ScenarioStageSpelErrorPropagationTest.java` - Logging patterns
- Existing scenario YAML files - YAML structure

---

### 6. Test File Naming and Organization

**Naming Convention:**
```
[TestClassName]-[purpose].yaml
```

**Examples:**
```
✅ DataTypeScenarioServiceClassificationTest-registry.yaml
✅ DataTypeScenarioServiceClassificationTest-otc-scenario.yaml
✅ DataTypeScenarioServiceClassificationTest-validation-rules.yaml
```

**File Size Guidelines:**
- Test YAML files: **< 50 lines** (simple examples)
- Test Java files: **< 300 lines** (focused tests)
- If larger, break into multiple focused tests

---

### 7. Mandatory Validation Checklist

**Before claiming work is complete:**

- [ ] All YAML files validated with APEX compiler
- [ ] All functional tests passing
- [ ] No hallucinated YAML keywords
- [ ] Correct metadata fields for document type
- [ ] Files are simple and focused (not over-complicated)
- [ ] Following existing patterns from codebase
- [ ] No promotional language or fake features
- [ ] Test baseline verified (no new failures introduced)

---

### 8. Error Recovery

**If you make a mistake:**

1. **Acknowledge it immediately** - Don't try to hide or minimize
2. **Identify root cause** - What went wrong?
3. **Fix it properly** - Don't just patch over
4. **Verify the fix** - Run all relevant tests
5. **Document the lesson** - Update guidelines if needed

**Example from Phase 1:**
- **Mistake:** Created YAML files without compiler validation
- **Result:** 5 out of 7 files failed validation
- **Root Cause:** Hallucinated field names without checking spec
- **Fix:** Corrected all field names, updated Java code
- **Verification:** 100% compiler validation, all tests passing
- **Lesson:** ALWAYS validate YAML with compiler BEFORE claiming success

---

## Quick Reference

### Valid YAML Structure for Scenarios

```yaml
metadata:
  id: "unique-id"
  name: "Human Readable Name"
  version: "1.0.0"
  description: "Clear description"
  type: "scenario"
  business-domain: "Trading"
  owner: "team@example.com"

scenario:
  scenario-id: "unique-id"
  name: "Scenario Name"
  description: "What this scenario does"

  # REQUIRED: Classification rule for automatic scenario selection
  classification-rule:
    condition: "#data['field'] == 'value'"
    description: "When to use this scenario"

  processing-stages:
    - stage-name: "validation"
      config-file: "path/to/validation-rules.yaml"
      execution-order: 1
      failure-policy: "terminate"
```

**Important Notes**:
- `classification-rule` is **REQUIRED** for automatic scenario selection
- Scenarios without `classification-rule` require manual selection
- All incoming data is `Map<String, Object>` from XML conversion
- Use SpEL expressions to match data content

### Valid YAML Structure for Scenario Registry

```yaml
metadata:
  id: "registry-id"
  name: "Registry Name"
  version: "1.0.0"
  description: "Registry description"
  type: "scenario-registry"
  created-by: "team@example.com"

scenarios:
  - scenario-id: "scenario-1"
    config-file: "path/to/scenario-1.yaml"

  - scenario-id: "scenario-2"
    config-file: "path/to/scenario-2.yaml"

routing:
  strategy: "classification-based"
  default-scenario: "scenario-1"
```

**Important Notes**:
- `routing` section is optional but recommended
- `default-scenario` provides fallback when no classification matches
- `strategy` should be "classification-based" for automatic routing

---

## Resources

**APEX Specification:**
- `docs/APEX_YAML_REFERENCE.md` - Official YAML specification
- `apex-compiler/APEX_COMPILER_YAML_VALIDATION_GUIDE.md` - Validation guide

**Compiler Validation:**
- `apex-compiler/src/main/java/dev/mars/apex/compiler/lexical/ApexYamlLexicalValidator.java`
- `apex-compiler/src/test/java/dev/mars/apex/compiler/ProjectYamlValidationTest.java`

**Reference Patterns:**
- `apex-demo/src/test/java/dev/mars/apex/demo/rulegroups/BasicYamlRuleGroupProcessingTest.java`
- `apex-core/src/test/java/dev/mars/apex/core/service/scenario/ScenarioStageSpelErrorPropagationTest.java`

**User Preferences:**
- `docs/prompts.txt` - Coding principles and guidelines
- User memory - Preferences and past mistakes

---

## Final Reminder

**When in doubt:**
1. Check the specification
2. Look at existing examples
3. Validate with compiler
4. Ask the user

**Never:**
1. Hallucinate YAML syntax
2. Create over-complicated examples
3. Skip compiler validation
4. Claim success without verification

