# Analysis: Supporting Numbered Suffixes for Reference Sections

## Current Status

**Reference sections (`enrichment-refs`, `rule-refs`) do NOT support numbered suffixes.**

When you write:
```yaml
enrichment-refs:
  - name: "External 1"
    source: "file1.yaml"

enrichment-refs-1:  # ❌ IGNORED with warning
  - name: "External 2"
    source: "file2.yaml"
```

**Result**: `enrichment-refs-1` is treated as an **unknown section** and ignored with warning:
```
WARNING [dev.mars.apex.core.config.yaml.OrderedYamlParser] Unknown YAML section encountered: enrichment-refs-1
```

---

## Why This Happens

### 1. **NUMBERED_SUFFIX_SECTIONS Does Not Include Reference Sections**

<augment_code_snippet path="apex-core/src/main/java/dev/mars/apex/core/config/yaml/OrderedYamlParser.java" mode="EXCERPT">
````java
// Sections that support numbered suffixes (e.g., enrichments-1, enrichments-2)
private static final Set<String> NUMBERED_SUFFIX_SECTIONS = Set.of(
    "enrichments", "rules", "enrichment-groups", "rule-groups",
    "transformations", "rule-chains"
    // ❌ "enrichment-refs" and "rule-refs" are NOT included
);
````
</augment_code_snippet>

### 2. **normalizeSectionName() Only Normalizes NUMBERED_SUFFIX_SECTIONS**

<augment_code_snippet path="apex-core/src/main/java/dev/mars/apex/core/config/yaml/OrderedYamlParser.java" mode="EXCERPT">
````java
private String normalizeSectionName(String sectionName) {
    if (sectionName.matches(".*-\\d+$")) {
        String baseName = sectionName.replaceAll("-\\d+$", "");
        // Only normalize if the base name is in NUMBERED_SUFFIX_SECTIONS
        if (NUMBERED_SUFFIX_SECTIONS.contains(baseName)) {
            return baseName;
        }
    }
    return sectionName;  // ❌ "enrichment-refs-1" stays as-is
}
````
</augment_code_snippet>

### 3. **isKnownSection() Rejects enrichment-refs-1**

Since `enrichment-refs-1` doesn't normalize to `enrichment-refs`, it's not found in `KNOWN_SECTIONS`:

<augment_code_snippet path="apex-core/src/main/java/dev/mars/apex/core/config/yaml/OrderedYamlParser.java" mode="EXCERPT">
````java
private boolean isKnownSection(String sectionName) {
    String normalized = normalizeSectionName(sectionName);
    return KNOWN_SECTIONS.contains(normalized);
    // ❌ enrichment-refs-1 -> enrichment-refs-1 (not normalized)
    // ❌ KNOWN_SECTIONS.contains("enrichment-refs-1") -> false
}
````
</augment_code_snippet>

---

## What's Involved in Supporting This Feature

### **OPTION 1: Full Support (Recommended)**

Add numbered suffix support for reference sections just like other sections.

#### **Changes Required:**

### 1. **Add Reference Sections to NUMBERED_SUFFIX_SECTIONS**

<augment_code_snippet path="apex-core/src/main/java/dev/mars/apex/core/config/yaml/OrderedYamlParser.java" mode="EXCERPT">
````java
private static final Set<String> NUMBERED_SUFFIX_SECTIONS = Set.of(
    "enrichments", "rules", "enrichment-groups", "rule-groups",
    "transformations", "rule-chains",
    "enrichment-refs", "rule-refs"  // ✅ ADD THESE
);
````
</augment_code_snippet>

### 2. **Add Merge Logic for Reference Sections**

Add cases to `mergeNumberedSections()` switch statement:

<augment_code_snippet path="apex-core/src/main/java/dev/mars/apex/core/config/yaml/OrderedYamlParser.java" mode="EXCERPT">
````java
switch (baseSectionName) {
    case "enrichments":
        mergeEnrichments(config, itemsToAdd);
        break;
    // ... existing cases ...
    case "enrichment-refs":  // ✅ ADD THIS
        mergeEnrichmentRefs(config, itemsToAdd);
        break;
    case "rule-refs":  // ✅ ADD THIS
        mergeRuleRefs(config, itemsToAdd);
        break;
    default:
        LOGGER.warning("Unknown base section for merging: " + baseSectionName);
}
````
</augment_code_snippet>

### 3. **Implement Merge Methods**

Create new merge methods similar to existing ones:

```java
/**
 * Merge enrichment refs from numbered sections into the main enrichment-refs list.
 */
private void mergeEnrichmentRefs(YamlRuleConfiguration config, List<Object> itemsToAdd) {
    ObjectMapper mapper = createYamlMapper();
    List<YamlEnrichmentRef> existingRefs = config.getEnrichmentRefs();
    if (existingRefs == null) {
        existingRefs = new ArrayList<>();
        config.setEnrichmentRefs(existingRefs);
    }

    for (Object item : itemsToAdd) {
        YamlEnrichmentRef ref = mapper.convertValue(item, YamlEnrichmentRef.class);
        existingRefs.add(ref);
    }

    LOGGER.info("Merged " + itemsToAdd.size() + " enrichment refs (total now: " + existingRefs.size() + ")");
}

/**
 * Merge rule refs from numbered sections into the main rule-refs list.
 */
private void mergeRuleRefs(YamlRuleConfiguration config, List<Object> itemsToAdd) {
    ObjectMapper mapper = createYamlMapper();
    List<YamlRuleRef> existingRefs = config.getRuleRefs();
    if (existingRefs == null) {
        existingRefs = new ArrayList<>();
        config.setRuleRefs(existingRefs);
    }

    for (Object item : itemsToAdd) {
        YamlRuleRef ref = mapper.convertValue(item, YamlRuleRef.class);
        existingRefs.add(ref);
    }

    LOGGER.info("Merged " + itemsToAdd.size() + " rule refs (total now: " + existingRefs.size() + ")");
}
```

### 4. **Update Item Order Extraction**

The `extractItemOrder()` method already handles reference sections correctly - it will automatically work once normalization is enabled:

<augment_code_snippet path="apex-core/src/main/java/dev/mars/apex/core/config/yaml/OrderedYamlParser.java" mode="EXCERPT">
````java
} else if (REFERENCE_SECTIONS.contains(normalizedSectionName) && sectionValue instanceof List) {
    // Insert placeholder for reference sections
    // ✅ This will now work for enrichment-refs-1, enrichment-refs-2, etc.
    itemOrder.add(new ProcessingItem(normalizedSectionName, "*"));
    LOGGER.fine("Added placeholder for reference section: " + sectionName);
}
````
</augment_code_snippet>

---

## Expected Behavior After Implementation

### **Before (Current):**
```yaml
enrichment-refs:
  - name: "External 1"
    source: "file1.yaml"

enrichment-refs-1:  # ❌ IGNORED
  - name: "External 2"
    source: "file2.yaml"
```
**Result**: Only `file1.yaml` is loaded. Warning logged for `enrichment-refs-1`.

### **After (With Support):**
```yaml
enrichment-refs:
  - name: "External 1"
    source: "file1.yaml"

enrichment-refs-1:  # ✅ MERGED
  - name: "External 2"
    source: "file2.yaml"
```
**Result**: Both `file1.yaml` and `file2.yaml` are loaded in document order.

---

## Testing Requirements

### 1. **Update TestEdge6_MultipleRefsWithNumberedSuffixesTest**

Change test expectation from "should work with warning" to "should work without warning":

```java
@Test
@DisplayName("Should support multiple enrichment-refs sections with numbered suffixes")
void shouldSupportMultipleRefsWithNumberedSuffixes() throws Exception {
    // Should load successfully WITHOUT warnings
    RulesEngine engine = RulesEngine.fromFile("...");
    assertNotNull(engine);
    
    // Verify both external files were loaded
    // (Check that enrichments from both files are present)
}
```

### 2. **Add Test for Rule-Refs**

Create similar test for `rule-refs` and `rule-refs-1`.

### 3. **Add Test for Mixed Refs**

Test interleaving of refs sections:
```yaml
enrichment-refs:
  - source: "file1.yaml"
enrichments:
  - id: "inline-1"
enrichment-refs-1:
  - source: "file2.yaml"
```

---

## Complexity Assessment

### **Effort Level: LOW** ⭐

**Estimated Time**: 1-2 hours

**Why Low Complexity:**
1. ✅ Pattern already established for other sections (enrichments, rules, etc.)
2. ✅ Infrastructure already in place (normalization, merging, item order)
3. ✅ Only need to add 2 lines to NUMBERED_SUFFIX_SECTIONS + 2 merge methods
4. ✅ No changes to YamlConfigurationLoader or reference resolution logic
5. ✅ Existing tests can be updated to verify behavior

**Risk Level: LOW** ⭐

**Why Low Risk:**
1. ✅ Change is additive (doesn't break existing functionality)
2. ✅ Reference processing happens AFTER merging (no order dependencies)
3. ✅ Existing validation will catch any issues (duplicate IDs, missing files, etc.)
4. ✅ Well-tested pattern (same logic used for 6 other section types)

---

## Recommendation

**✅ IMPLEMENT THIS FEATURE**

**Reasons:**
1. **Consistency**: All other list sections support numbered suffixes - refs should too
2. **User Expectation**: Users naturally expect `enrichment-refs-1` to work like `enrichments-1`
3. **Low Cost**: Very simple change with minimal risk
4. **High Value**: Enables better organization of large YAML files with many external references
5. **Already Tested**: TestEdge6 already exists and just needs expectation updated

**Use Case:**
```yaml
# Main scenario file
enrichment-refs:
  - source: "customer-enrichments.yaml"
  - source: "product-enrichments.yaml"

enrichments:
  - id: "scenario-specific-1"
    # ... inline enrichment ...

enrichment-refs-1:
  - source: "order-enrichments.yaml"
  - source: "shipping-enrichments.yaml"
```

This allows users to organize external references around inline definitions while maintaining document order.

---

## Implementation Checklist

- [ ] Add `enrichment-refs` and `rule-refs` to `NUMBERED_SUFFIX_SECTIONS`
- [ ] Implement `mergeEnrichmentRefs()` method
- [ ] Implement `mergeRuleRefs()` method
- [ ] Add merge cases to `mergeNumberedSections()` switch
- [ ] Update TestEdge6 to expect success without warnings
- [ ] Add test for rule-refs with numbered suffixes
- [ ] Add test for mixed/interleaved refs sections
- [ ] Run full test suite to verify no regressions
- [ ] Update APEX_YAML_REFERENCE.md to document this feature

