# APEX Enrichment-Refs Enhancement Analysis

## Problem Statement

Currently, APEX supports `rule-refs` for loading rules from external files, but there is **no equivalent mechanism for enrichments and enrichment groups**. This creates an asymmetry where:

1. **Rule Groups** can reference rules from external files via `rule-refs`
2. **Enrichment Groups** cannot reference enrichments from external files
3. Enrichment groups must be defined in the same file or manually merged

### Current Limitation Example

**LeifMultiFileEnrichmentGroups.yaml** (main file):
```yaml
enrichment-groups:
  - id: g2_rule_builder
    name: call rule builder group
    operator: AND
    enrichment-group-references: [ rule_builder_group ]  # ❌ FAILS - rule_builder_group not found
```

**LeifMultiFileEnrichments.yaml** (external file):
```yaml
enrichment-groups:
  - id: rule_builder_group
    name: g1
    operator: AND
    enrichment-ids: [ r1, r2 ]
```

**Problem**: `rule_builder_group` is not loaded because there's no `enrichment-refs` mechanism.

---

## Proposed Solution: enrichment-refs

Implement `enrichment-refs` following the exact same pattern as `rule-refs`:

```yaml
# LeifMultiFileEnrichmentGroups.yaml
enrichment-refs:
  - name: "rule-builder-enrichments"
    source: "src/test/java/dev/mars/apex/demo/enrichmentgroups/LeifMultiFileEnrichments.yaml"
    enabled: true
    description: "Rule builder enrichment definitions"

enrichment-groups:
  - id: g2_rule_builder
    name: call rule builder group
    operator: AND
    enrichment-group-references: [ rule_builder_group ]  # ✅ NOW WORKS
```

---

## Implementation Architecture

### 1. **YamlEnrichmentRef Class** (NEW)
Mirror `YamlRuleRef` exactly:
- `name`: Reference identifier
- `source`: File path to external enrichment file
- `enabled`: Boolean flag (default: true)
- `description`: Optional description

### 2. **YamlRuleConfiguration Enhancement**
Add field:
```java
@JsonProperty("enrichment-refs")
private List<YamlEnrichmentRef> enrichmentRefs;
```

### 3. **YamlConfigurationLoader Enhancement**
Add two new methods:
- `processEnrichmentReferences()`: Load enrichments from external files
- `processEnrichmentGroupReferences()`: Load enrichment groups from external files

Call these in:
- `loadFromFile()`
- `loadFromString()`
- `loadFromStream()`
- `processReferencesAndValidate()`

### 4. **Processing Flow**
```
1. Load main YAML file
2. Parse enrichment-refs section
3. For each enabled enrichment-ref:
   a. Load external enrichment file
   b. Extract enrichments section
   c. Merge into main config.enrichments
   d. Extract enrichment-groups section
   e. Merge into main config.enrichmentGroups
4. Validate all references resolved
```

### 5. **Validation**
- Unique enrichment IDs across all files
- Unique enrichment group IDs across all files
- All referenced enrichment groups exist
- No circular dependencies

---

## Benefits

✅ **Consistency**: Mirrors proven `rule-refs` pattern  
✅ **Modularity**: Separate enrichment definitions by domain  
✅ **Reusability**: Share enrichments across multiple scenarios  
✅ **Maintainability**: Easier to organize large enrichment collections  
✅ **Backward Compatible**: Existing inline enrichments continue to work  

---

## Implementation Steps

1. Create `YamlEnrichmentRef` class
2. Add `enrichmentRefs` to `YamlRuleConfiguration`
3. Implement `processEnrichmentReferences()` in `YamlConfigurationLoader`
4. Implement `processEnrichmentGroupReferences()` in `YamlConfigurationLoader`
5. Update loader methods to call new processing methods
6. Add validation in `YamlMetadataValidator`
7. Create comprehensive test cases
8. Update documentation

---

## Files to Modify

**New Files:**
- `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlEnrichmentRef.java`

**Modified Files:**
- `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleConfiguration.java`
- `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlConfigurationLoader.java`
- `apex-core/src/main/java/dev/mars/apex/core/util/YamlMetadataValidator.java`
- `docs/APEX_YAML_REFERENCE.md`

**Test Files:**
- `apex-core/src/test/java/dev/mars/apex/core/config/yaml/YamlConfigurationLoaderEnrichmentReferencesTest.java`
- `apex-demo/src/test/java/dev/mars/apex/demo/enrichmentgroups/LeifMultiFileEnrichmentGroupsTest.java`

