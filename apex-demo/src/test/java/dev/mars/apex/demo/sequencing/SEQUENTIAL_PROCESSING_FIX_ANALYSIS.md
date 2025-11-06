# APEX Sequential Processing - Problem Analysis and Fix Plan

**Date**: 2025-11-06  
**Status**: ANALYSIS - AWAITING REVIEW  
**Severity**: CRITICAL - Current implementation does NOT work as documented

---

## Executive Summary

The current "sequential processing" implementation is **fundamentally broken**. It only supports **section-level ordering** (all enrichments → all rules), NOT **item-level ordering** (E1 → E2 → R1 → R2).

**Current Behavior**:
```yaml
enrichments:
  - id: "enrich-1"    # Processed 1st
  - id: "enrich-2"    # Processed 2nd
rules:
  - id: "rule-1"      # Processed 3rd
  - id: "rule-2"      # Processed 4th
```
✅ This works - items within a section are processed in order.

**Broken Behavior** (what user needs):
```yaml
enrichments:
  - id: "enrich-1"    # Should be processed 1st
rules:
  - id: "rule-1"      # Should be processed 2nd (depends on enrich-1)
enrichments:
  - id: "enrich-2"    # Should be processed 3rd (depends on rule-1)
rules:
  - id: "rule-2"      # Should be processed 4th (depends on enrich-2)
```
❌ This does NOT work - YAML doesn't allow duplicate keys, so only the LAST `enrichments:` and `rules:` sections are kept.

---

## Root Cause Analysis

### Problem 1: YAML Duplicate Keys
YAML specification does NOT allow duplicate keys. When you have:
```yaml
enrichments:
  - id: "enrich-1"
enrichments:  # ← This OVERWRITES the first enrichments section!
  - id: "enrich-2"
```

The YAML parser (SnakeYAML) will only keep the LAST occurrence. The first `enrichments:` section is completely lost.

### Problem 2: Section-Level Processing
Even if we could capture multiple sections, the current `RulesEngine.evaluateInDocumentOrder()` processes entire sections at once:

**Line 1104-1110** (RulesEngine.java):
```java
case "enrichments":
    enrichmentProcessor.processEnrichments(
        yamlConfig.getEnrichments(), enrichedData, yamlConfig);  // ← ALL enrichments at once
```

**Line 1139-1143** (RulesEngine.java):
```java
case "rules":
    List<Rule> allRules = configuration.getAllRules();
    RuleResult ruleResult = executeRulesList(allRules, enrichedData);  // ← ALL rules at once
```

### Problem 3: Section Order Only
`OrderedYamlParser.extractSectionOrder()` only captures section names, not items:

**Line 135-151** (OrderedYamlParser.java):
```java
private List<String> extractSectionOrder(Map<String, Object> yamlMap) {
    List<String> sectionOrder = new ArrayList<>();
    for (String key : yamlMap.keySet()) {
        if (KNOWN_SECTIONS.contains(key)) {
            sectionOrder.add(key);  // ← Only section NAME, not items
        }
    }
    return sectionOrder;
}
```

Result: `sectionOrder = ["metadata", "enrichments", "rules"]`  
NOT: `itemOrder = ["enrich-1", "enrich-2", "rule-1", "rule-2"]`

---

## Note on Pipeline Section

**Current Implementation**: The `pipeline` section is currently defined as a **single object** (not a list) in `YamlRuleConfiguration`:

```java
@JsonProperty("pipeline")
private PipelineConfiguration pipeline;  // ← Single object, NOT List<PipelineConfiguration>
```

This means:
- Only **one pipeline per document** is currently supported
- Pipeline is processed at **section-level** (as a complete unit), not item-level
- Pipeline **DOES appear in section order** (e.g., `["enrichments", "pipeline", "rules"]`)
- Pipeline **does NOT appear in item order** (it's not broken down into individual items)
- When section-level processing reaches `"pipeline"`, it calls `executePipeline()` to execute the entire pipeline
- Pipeline's internal steps are managed by `PipelineExecutor`, separate from document-level ordering

**Processing Example**:
```yaml
enrichments:
  - id: "enrich-1"

pipeline:
  name: "my-etl-pipeline"
  steps: [...]

rules:
  - id: "rule-1"
```

**Section order**: `["enrichments", "pipeline", "rules"]` ✅ Pipeline is here
**Item order**: `["enrichments:enrich-1", "rules:rule-1"]` ✅ Pipeline is NOT here (correct)

**Execution**: enrich-1 → entire pipeline → rule-1

**Future Consideration**: If support for **multiple pipelines per document** is needed in the future:
1. Change field to `List<PipelineConfiguration> pipelines`
2. Add `"pipelines"` to the list of executable sections
3. Include pipelines in item-level processing order
4. Each pipeline would be processed as a single item (not broken down into steps)

---

## What Actually Works

### ✅ Items Within Same Section
Items within a single section ARE processed in document order:

```yaml
enrichments:
  - id: "calculate-risk-score"     # Processed 1st
  - id: "determine-approval-level" # Processed 2nd (can depend on 1st)
```

**Why it works**: `YamlEnrichmentProcessor.processEnrichments()` line 158:
```java
for (YamlEnrichment enrichment : enrichments) {
    enrichedObject = processEnrichment(enrichment, enrichedObject);
}
```

### ✅ Section-Level Ordering
Sections ARE processed in document order:

```yaml
enrichments:      # Section processed 1st
  - id: "e1"
  - id: "e2"
rules:            # Section processed 2nd
  - id: "r1"
  - id: "r2"
```

**Why it works**: `RulesEngine.evaluateInDocumentOrder()` line 1100:
```java
for (String section : sectionOrder) {
    switch (section) {
        case "enrichments": // Process ALL enrichments
        case "rules":       // Process ALL rules
    }
}
```

---

## What Does NOT Work

### ❌ Item-Level Interleaving Across Sections
Cannot interleave enrichments and rules at item level:

```yaml
# DESIRED (but impossible with current YAML structure):
enrichments:
  - id: "enrich-1"
rules:
  - id: "rule-1"      # Depends on enrich-1
enrichments:          # ❌ Duplicate key - YAML parser will ignore first enrichments
  - id: "enrich-2"
rules:                # ❌ Duplicate key - YAML parser will ignore first rules
  - id: "rule-2"
```

---

## Complete List of Processable Sections

Based on `OrderedYamlParser.KNOWN_SECTIONS` and `RulesEngine.evaluateInDocumentOrder()`:

### Executable Sections (Process Items):
1. **enrichments** - Individual enrichment items
2. **rules** - Individual rule items
3. **enrichment-groups** - Groups of enrichments with AND/OR logic
4. **rule-groups** - Groups of rules with AND/OR logic
5. **pipeline** - Multi-stage processing pipeline (single item)
6. **transformations** - Data transformation steps
7. **rule-chains** - Sequential rule execution chains

### Configuration Sections (Not Executed):
8. **metadata** - Document metadata
9. **data-sources** - Data source definitions
10. **data-source-refs** - References to external data sources
11. **rule-refs** - References to external rule files
12. **enrichment-refs** - References to external enrichment files
13. **data-sinks** - Output destinations
14. **categories** - Rule categorization
15. **error-recovery** - Error handling configuration

### Special Sections:
16. **scenario** - Scenario configuration (complex structure)

---

## Proposed Solution

### Option 1: Extract Item-Level Order from Existing YAML Structure

**Key Insight**: We don't need duplicate keys. Items are ALREADY in document order within their sections. We just need to capture the order of ALL items across ALL sections.

**YAML Structure** (NO CHANGES REQUIRED):
```yaml
enrichments:
  - id: "enrich-1"          # Position 1
  - id: "enrich-2"          # Position 2
enrichment-groups:
  - id: "enrich-group-1"    # Position 3
rules:
  - id: "rule-1"            # Position 4
  - id: "rule-2"            # Position 5
rule-groups:
  - id: "rule-group-1"      # Position 6
```

**Implementation**:
1. **OrderedYamlParser**: Extract item-level order across all sections
   - Parse YAML with SnakeYAML (preserves order)
   - For each section, extract items in order
   - Build unified list: `[(enrichments, enrich-1), (enrichments, enrich-2), (enrichment-groups, enrich-group-1), (rules, rule-1), (rules, rule-2), (rule-groups, rule-group-1)]`

2. **YamlRuleConfiguration**: Store item order
   - Add field: `List<ProcessingItem> itemOrder`
   - ProcessingItem: `{sectionType: "enrichments", itemId: "enrich-1"}`

3. **RulesEngine.evaluateInDocumentOrder()**: Process items one at a time
   - Instead of: `for (String section : sectionOrder)` → process ALL enrichments, then ALL rules
   - Use: `for (ProcessingItem item : itemOrder)` → process items one at a time in document order
   - Process each item individually based on section type

**Sections to Support**:
- ✅ `enrichments` - Process individual enrichments
- ✅ `rules` - Process individual rules
- ✅ `enrichment-groups` - Process individual enrichment groups
- ✅ `rule-groups` - Process individual rule groups
- ✅ `transformations` - Process individual transformations
- ✅ `rule-chains` - Process individual rule chains
- ⚠️ `pipeline` - Single item, not a list (process entire pipeline)

**Backward Compatibility**: ✅ FULL
- Existing YAML files work unchanged
- If `itemOrder` is null/empty, fall back to section-level processing
- No changes to YAML structure required

---

## Implementation Steps

### Step 1: Create ProcessingItem Class
**File**: `apex-core/src/main/java/dev/mars/apex/core/config/yaml/ProcessingItem.java` (NEW FILE)

**Content**:
```java
package dev.mars.apex.core.config.yaml;

/**
 * Represents a single processing item in document order.
 * Used to track the order of enrichments, rules, groups, etc. as they appear in YAML.
 */
public class ProcessingItem {
    private final String sectionType;  // e.g., "enrichments", "rules", "enrichment-groups"
    private final String itemId;       // e.g., "enrich-1", "rule-1"

    public ProcessingItem(String sectionType, String itemId) {
        this.sectionType = sectionType;
        this.itemId = itemId;
    }

    public String getSectionType() { return sectionType; }
    public String getItemId() { return itemId; }

    @Override
    public String toString() {
        return sectionType + ":" + itemId;
    }
}
```

**Risk**: NONE - New file, no impact on existing code

### Step 2: Enhance OrderedYamlParser
**File**: `apex-core/src/main/java/dev/mars/apex/core/config/yaml/OrderedYamlParser.java`

**Add new method** `extractItemOrder()` alongside existing `extractSectionOrder()`:

```java
/**
 * Extract item-level order from YAML document.
 * This captures the order of individual items within sections.
 *
 * @param yamlMap Ordered map from SnakeYAML parsing
 * @return List of processing items in document order
 */
private List<ProcessingItem> extractItemOrder(Map<String, Object> yamlMap) {
    List<ProcessingItem> itemOrder = new ArrayList<>();

    // Sections that contain lists of items
    Set<String> LIST_SECTIONS = Set.of(
        "enrichments", "rules", "enrichment-groups", "rule-groups",
        "transformations", "rule-chains"
    );

    // Note: 'pipeline' is currently a single object (not a list) in YamlRuleConfiguration
    // and is processed at section-level, not item-level. If pipeline becomes a list in
    // the future (to support multiple pipelines per document), add it to LIST_SECTIONS.

    // LinkedHashMap from SnakeYAML preserves insertion order
    for (String sectionName : yamlMap.keySet()) {
        if (!KNOWN_SECTIONS.contains(sectionName)) {
            continue; // Skip unknown sections
        }

        Object sectionValue = yamlMap.get(sectionName);

        if (LIST_SECTIONS.contains(sectionName) && sectionValue instanceof List) {
            // Process list sections (enrichments, rules, etc.)
            List<?> items = (List<?>) sectionValue;
            for (Object item : items) {
                if (item instanceof Map) {
                    Map<?, ?> itemMap = (Map<?, ?>) item;
                    String itemId = (String) itemMap.get("id");
                    if (itemId != null) {
                        itemOrder.add(new ProcessingItem(sectionName, itemId));
                        LOGGER.fine("Found item in order: " + sectionName + " -> " + itemId);
                    } else {
                        LOGGER.warning("Item in section '" + sectionName + "' has no ID");
                    }
                }
            }
        }
        // Single-object sections (like 'pipeline') are not included in item order
        // They are processed at section-level only
    }

    LOGGER.info("Extracted " + itemOrder.size() + " items in document order");
    return itemOrder;
}
```

**Update `parseWithOrder()` method** to extract both section order AND item order:
```java
public YamlRuleConfiguration parseWithOrder(String yamlContent) throws YamlConfigurationException {
    // ... existing code ...

    // Extract section order (existing)
    List<String> sectionOrder = extractSectionOrder(yamlMap);

    // Extract item order (NEW)
    List<ProcessingItem> itemOrder = extractItemOrder(yamlMap);

    // ... existing code ...

    config.setSectionOrder(sectionOrder);
    config.setItemOrder(itemOrder);  // NEW

    return config;
}
```

**Risk**: LOW - Only adds new functionality, doesn't change existing behavior

### Step 3: Add Item Order to YamlRuleConfiguration
**File**: `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleConfiguration.java`

**Add new fields**:
```java
/**
 * Item-level order as it appears in the YAML document.
 * This field is populated by OrderedYamlParser to enable item-level sequential processing.
 * Not serialized to YAML - only used internally for execution order.
 */
private List<ProcessingItem> itemOrder;

/**
 * Track which enrichment IDs came from external references.
 * Used to expand reference placeholders in item order.
 */
private Set<String> referencedEnrichmentIds;

/**
 * Track which rule IDs came from external references.
 * Used to expand reference placeholders in item order.
 */
private Set<String> referencedRuleIds;

/**
 * Track which enrichment group IDs came from external references.
 */
private Set<String> referencedEnrichmentGroupIds;

/**
 * Track which rule group IDs came from external references.
 */
private Set<String> referencedRuleGroupIds;

public List<ProcessingItem> getItemOrder() {
    return itemOrder;
}

public void setItemOrder(List<ProcessingItem> itemOrder) {
    this.itemOrder = itemOrder;
}

public Set<String> getReferencedEnrichmentIds() {
    return referencedEnrichmentIds;
}

public void setReferencedEnrichmentIds(Set<String> referencedEnrichmentIds) {
    this.referencedEnrichmentIds = referencedEnrichmentIds;
}

public Set<String> getReferencedRuleIds() {
    return referencedRuleIds;
}

public void setReferencedRuleIds(Set<String> referencedRuleIds) {
    this.referencedRuleIds = referencedRuleIds;
}

public Set<String> getReferencedEnrichmentGroupIds() {
    return referencedEnrichmentGroupIds;
}

public void setReferencedEnrichmentGroupIds(Set<String> referencedEnrichmentGroupIds) {
    this.referencedEnrichmentGroupIds = referencedEnrichmentGroupIds;
}

public Set<String> getReferencedRuleGroupIds() {
    return referencedRuleGroupIds;
}

public void setReferencedRuleGroupIds(Set<String> referencedRuleGroupIds) {
    this.referencedRuleGroupIds = referencedRuleGroupIds;
}
```

**Risk**: LOW - Just adds new fields

### Step 4: Track Referenced Items in YamlConfigurationLoader
**File**: `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlConfigurationLoader.java`

**Modify `processRuleReferences()`** to track which rules came from references:
```java
private void processRuleReferences(YamlRuleConfiguration config) throws YamlConfigurationException {
    if (config.getRuleRefs() == null || config.getRuleRefs().isEmpty()) {
        return;
    }

    // Track referenced rule IDs
    Set<String> referencedRuleIds = new HashSet<>();

    for (YamlRuleRef ref : config.getRuleRefs()) {
        if (!ref.isEnabled()) continue;

        YamlRuleConfiguration referencedConfig = loadRuleFile(ref.getSource());

        if (referencedConfig.getRules() != null) {
            if (config.getRules() == null) {
                config.setRules(new ArrayList<>());
            }

            // Track IDs before adding
            for (YamlRule rule : referencedConfig.getRules()) {
                referencedRuleIds.add(rule.getId());
            }

            config.getRules().addAll(referencedConfig.getRules());
        }
    }

    // Store tracked IDs
    config.setReferencedRuleIds(referencedRuleIds);
}
```

**Modify `processEnrichmentReferences()`** similarly:
```java
private void processEnrichmentReferences(YamlRuleConfiguration config) throws YamlConfigurationException {
    if (config.getEnrichmentRefs() == null || config.getEnrichmentRefs().isEmpty()) {
        return;
    }

    // Track referenced enrichment and enrichment group IDs
    Set<String> referencedEnrichmentIds = new HashSet<>();
    Set<String> referencedEnrichmentGroupIds = new HashSet<>();

    for (YamlEnrichmentRef ref : config.getEnrichmentRefs()) {
        if (!ref.isEnabled()) continue;

        YamlRuleConfiguration referencedConfig = loadRuleFile(ref.getSource());

        if (referencedConfig.getEnrichments() != null) {
            if (config.getEnrichments() == null) {
                config.setEnrichments(new ArrayList<>());
            }

            // Track IDs before adding
            for (YamlEnrichment enrichment : referencedConfig.getEnrichments()) {
                referencedEnrichmentIds.add(enrichment.getId());
            }

            config.getEnrichments().addAll(referencedConfig.getEnrichments());
        }

        if (referencedConfig.getEnrichmentGroups() != null) {
            if (config.getEnrichmentGroups() == null) {
                config.setEnrichmentGroups(new ArrayList<>());
            }

            // Track IDs before adding
            for (YamlEnrichmentGroup group : referencedConfig.getEnrichmentGroups()) {
                referencedEnrichmentGroupIds.add(group.getId());
            }

            config.getEnrichmentGroups().addAll(referencedConfig.getEnrichmentGroups());
        }
    }

    // Store tracked IDs
    config.setReferencedEnrichmentIds(referencedEnrichmentIds);
    config.setReferencedEnrichmentGroupIds(referencedEnrichmentGroupIds);
}
```

**Risk**: LOW - Only adds tracking, doesn't change existing behavior

### Step 5: Expand Reference Placeholders in YamlConfigurationLoader
**File**: `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlConfigurationLoader.java`

**Add new method** to expand reference placeholders AFTER processing references:
```java
/**
 * Expand reference placeholders in item order.
 * This replaces "*-refs" placeholders with actual items from referenced files.
 */
private void expandReferencePlaceholders(YamlRuleConfiguration config) {
    if (config.getItemOrder() == null || config.getItemOrder().isEmpty()) {
        return;
    }

    List<ProcessingItem> expandedOrder = new ArrayList<>();

    for (ProcessingItem item : config.getItemOrder()) {
        String sectionType = item.getSectionType();
        String itemId = item.getItemId();

        if (sectionType.equals("enrichment-refs") && itemId.equals("*")) {
            // Expand enrichment references
            if (config.getReferencedEnrichmentIds() != null) {
                for (String enrichmentId : config.getReferencedEnrichmentIds()) {
                    expandedOrder.add(new ProcessingItem("enrichments", enrichmentId));
                }
            }
            if (config.getReferencedEnrichmentGroupIds() != null) {
                for (String groupId : config.getReferencedEnrichmentGroupIds()) {
                    expandedOrder.add(new ProcessingItem("enrichment-groups", groupId));
                }
            }
        } else if (sectionType.equals("rule-refs") && itemId.equals("*")) {
            // Expand rule references
            if (config.getReferencedRuleIds() != null) {
                for (String ruleId : config.getReferencedRuleIds()) {
                    expandedOrder.add(new ProcessingItem("rules", ruleId));
                }
            }
            if (config.getReferencedRuleGroupIds() != null) {
                for (String groupId : config.getReferencedRuleGroupIds()) {
                    expandedOrder.add(new ProcessingItem("rule-groups", groupId));
                }
            }
        } else {
            // Keep non-placeholder items as-is
            expandedOrder.add(item);
        }
    }

    config.setItemOrder(expandedOrder);
    LOGGER.info("Expanded item order from " + config.getItemOrder().size() + " to " + expandedOrder.size() + " items");
}
```

**Call this method** in `load()`, `loadFromFile()`, `loadFromInputStream()` AFTER processing references:
```java
// Process external rule references
processRuleReferences(config);

// Process external enrichment references
processEnrichmentReferences(config);

// Process external data-source references
processDataSourceReferences(config);

// Expand reference placeholders in item order (NEW)
expandReferencePlaceholders(config);

validateConfiguration(config);
```

**Risk**: MEDIUM - Changes how item order is constructed, but only affects new functionality

### Step 6: Modify RulesEngine.evaluateInDocumentOrder()
**File**: `apex-core/src/main/java/dev/mars/apex/core/engine/config/RulesEngine.java`

**Replace the section-level loop** with item-level processing:

```java
private RuleResult evaluateInDocumentOrder(YamlRuleConfiguration yamlConfig, Map<String, Object> inputData, List<String> sectionOrder) {
    List<String> failureMessages = new ArrayList<>();
    Map<String, Object> enrichedData = new HashMap<>(inputData);
    boolean overallSuccess = true;

    try {
        // Check if item-level order is available
        List<ProcessingItem> itemOrder = yamlConfig.getItemOrder();

        if (itemOrder != null && !itemOrder.isEmpty()) {
            // NEW: Item-level processing
            logger.info("Processing {} items in document order", itemOrder.size());

            for (ProcessingItem item : itemOrder) {
                logger.debug("Processing item: {} ({})", item.getItemId(), item.getSectionType());

                RuleResult itemResult = processItem(item, yamlConfig, enrichedData);

                if (itemResult.getResultType() == RuleResult.ResultType.ERROR) {
                    overallSuccess = false;
                    failureMessages.add(item.getSectionType() + " '" + item.getItemId() + "' error: " + itemResult.getMessage());
                }

                // Update enriched data with results
                if (itemResult.getEnrichedData() != null) {
                    enrichedData.putAll(itemResult.getEnrichedData());
                }
            }
        } else {
            // FALLBACK: Section-level processing (existing code)
            logger.info("No item order available, falling back to section-level processing");
            logger.info("Processing {} sections in document order", sectionOrder.size());

            // ... existing section-level processing code ...
        }

        // ... rest of method unchanged ...
    }
}
```

**Add new method** to dispatch to appropriate processor:

```java
/**
 * Process a single item based on its section type.
 */
private RuleResult processItem(ProcessingItem item, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
    String sectionType = item.getSectionType();
    String itemId = item.getItemId();

    switch (sectionType) {
        case "enrichments":
            return processEnrichmentItem(itemId, yamlConfig, data);
        case "rules":
            return processRuleItem(itemId, yamlConfig, data);
        case "enrichment-groups":
            return processEnrichmentGroupItem(itemId, yamlConfig, data);
        case "rule-groups":
            return processRuleGroupItem(itemId, yamlConfig, data);
        case "transformations":
        case "rule-chains":
            logger.warn("Section type '{}' not yet supported for item-level processing", sectionType);
            return RuleResult.success(sectionType + ":" + itemId, "Skipped");
        default:
            logger.warn("Unknown section type: {}", sectionType);
            return RuleResult.error(sectionType + ":" + itemId, "Unknown section type");
    }
}
```

**Risk**: MEDIUM - Changes core execution logic, needs careful testing

### Step 7: Add Helper Methods to Process Individual Items
**File**: `apex-core/src/main/java/dev/mars/apex/core/engine/config/RulesEngine.java`

**New Methods** (based on existing lookup methods in RulesEngineConfiguration):

```java
/**
 * Process a single enrichment by ID.
 */
private RuleResult processEnrichmentItem(String enrichmentId, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
    // Find enrichment in yamlConfig.getEnrichments()
    YamlEnrichment enrichment = findEnrichmentById(yamlConfig, enrichmentId);
    if (enrichment == null) {
        logger.warn("Enrichment not found: {}", enrichmentId);
        return RuleResult.error("enrichment:" + enrichmentId, "Enrichment not found");
    }

    // Process single enrichment using YamlEnrichmentProcessor.processEnrichmentWithResult()
    // This method returns RuleResult directly (unlike processEnrichment() which returns Object)
    return enrichmentProcessor.processEnrichmentWithResult(enrichment, data);
}

/**
 * Process a single rule by ID.
 */
private RuleResult processRuleItem(String ruleId, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
    // Look up rule in configuration.getRuleById()
    Rule rule = configuration.getRuleById(ruleId);
    if (rule == null) {
        logger.warn("Rule not found: {}", ruleId);
        return RuleResult.error("rule:" + ruleId, "Rule not found");
    }

    // Execute single rule using executeRulesList()
    return executeRulesList(List.of(rule), data);
}

/**
 * Process a single enrichment group by ID.
 */
private RuleResult processEnrichmentGroupItem(String groupId, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
    // Look up enrichment group in configuration.getEnrichmentGroupById()
    EnrichmentGroup group = configuration.getEnrichmentGroupById(groupId);
    if (group == null) {
        logger.warn("Enrichment group not found: {}", groupId);
        return RuleResult.error("enrichment-group:" + groupId, "Enrichment group not found");
    }

    // Execute single enrichment group using executeEnrichmentGroupsList()
    return executeEnrichmentGroupsList(List.of(group), data);
}

/**
 * Process a single rule group by ID.
 */
private RuleResult processRuleGroupItem(String groupId, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
    // Look up rule group in configuration.getRuleGroupById()
    RuleGroup group = configuration.getRuleGroupById(groupId);
    if (group == null) {
        logger.warn("Rule group not found: {}", groupId);
        return RuleResult.error("rule-group:" + groupId, "Rule group not found");
    }

    // Execute single rule group using executeRuleGroupsList()
    return executeRuleGroupsList(List.of(group), data);
}
```

**Helper Methods**:

**Note**: `YamlEnrichmentProcessor` already has `findRuleById()` (line 1288), but does NOT have `findEnrichmentById()`. We need to add it:

```java
/**
 * Find an enrichment by ID in the configuration.
 * Similar to existing findRuleById() method in YamlEnrichmentProcessor.
 *
 * @param config The YAML configuration
 * @param enrichmentId The enrichment ID to find
 * @return The YamlEnrichment if found, null otherwise
 */
private YamlEnrichment findEnrichmentById(YamlRuleConfiguration config, String enrichmentId) {
    if (config.getEnrichments() != null) {
        for (YamlEnrichment enrichment : config.getEnrichments()) {
            if (enrichmentId.equals(enrichment.getId())) {
                return enrichment;
            }
        }
    }
    return null;
}
```

**Risk**: LOW - New methods, don't affect existing code. Reuse existing processing methods.

---

### Step 7.5: Error Handling Strategy

**Principle** (from `docs/design/prompts.txt`):
> "Configuration errors should NEVER throw exceptions that break application flow"
> "Log, Don't Throw: Configuration issues should be logged as warnings, not thrown as exceptions"

**Error Handling Approach**:

#### 1. Missing Item IDs
**Scenario**: Item referenced in order but not found in configuration

**Handling**:
```java
if (enrichment == null) {
    logger.warn("Enrichment not found: {}", enrichmentId);
    return RuleResult.error("enrichment:" + enrichmentId, "Enrichment not found");
}
```

**Behavior**: Log warning, return error result, **continue processing next items** (graceful degradation)

#### 2. Enrichment/Rule Execution Failures
**Scenario**: Individual enrichment or rule fails during execution

**Handling**: Already handled by `processEnrichmentWithResult()` and `executeRule()`:
- Returns `RuleResult` with error status
- Does NOT throw exceptions
- Accumulates failure messages in overall result
- Continues processing remaining items

**Example**:
```java
RuleResult itemResult = processItem(item, yamlConfig, enrichedData);

if (itemResult.getResultType() == RuleResult.ResultType.ERROR) {
    overallSuccess = false;
    failureMessages.add(item.getSectionType() + " '" + item.getItemId() + "' error: " + itemResult.getMessage());
}

// Continue processing next item (graceful degradation)
```

#### 3. SpEL Evaluation Errors
**Scenario**: SpEL expression fails during condition evaluation

**Handling**: Already handled by `UnifiedRuleEvaluator`:
- Catches all SpEL exceptions
- Returns `RuleResult.error()` instead of throwing
- Logs warning with details
- No changes needed ✅

**Evidence** (from `UnifiedRuleEvaluator.java`):
```java
try {
    Boolean result = expression.getValue(context, Boolean.class);
    // ... process result
} catch (Exception e) {
    logger.warn("SpEL evaluation error: {}", e.getMessage());
    return RuleResult.error(rule.getName(), "Evaluation error: " + e.getMessage());
}
```

#### 4. Circular Dependencies
**Scenario**: Items reference each other in a circular manner

**Analysis**: **NOT POSSIBLE** with current design ✅
- Items are processed in **linear document order**
- No runtime dependency resolution
- References are resolved during loading (before execution)
- File-level references are expanded to item order before execution

**Conclusion**: No circular dependency detection needed

#### 5. Null/Empty Item Order
**Scenario**: `itemOrder` is null or empty

**Handling**: Fallback to section-level processing (backward compatibility)
```java
if (itemOrder != null && !itemOrder.isEmpty()) {
    // Item-level processing
} else {
    // Fallback to section-level processing (existing code)
    logger.info("No item order available, falling back to section-level processing");
}
```

**Risk Assessment**:
- **Missing Items**: LOW - Logged and handled gracefully ✅
- **Execution Failures**: LOW - Already handled by existing code ✅
- **SpEL Errors**: LOW - Already handled by UnifiedRuleEvaluator ✅
- **Circular Dependencies**: NONE - Not possible with design ✅
- **Null Item Order**: LOW - Fallback to existing behavior ✅

---

### Step 8: Comprehensive Testing
**Files**: `apex-demo/src/test/java/dev/mars/apex/demo/sequencing/`

**Test Cases**:

**Note**: Following user preference, all test examples use **middle office trade processing domain (OTC options)** with **real data sources** (databases, not inline data).

1. **Basic Item-Level Ordering**:
   - E1 → E2 → R1 → R2 (all in order within sections)
   - E1 → R1 → E2 → R2 (NOT POSSIBLE - YAML duplicate keys)

2. **Groups and Individual Items**:
   - E1 → EG1 → R1 → RG1 (enrichment → enrichment-group → rule → rule-group)
   - EG1 → E1 → RG1 → R1 (group before individual items)

3. **All Section Types**:
   - enrichments → enrichment-groups → rules → rule-groups → transformations → rule-chains
   - Verify each section's items are processed in document order

4. **Backward Compatibility**:
   - Files without item order fall back to section-level processing
   - Existing tests continue to pass

5. **Error Handling**:
   - Missing item IDs (item referenced in order but not defined)
   - Invalid section types
   - Null/empty item order

6. **Reference Expansion** ← NEW:
   - Test enrichment-refs placeholder expansion
   - Test rule-refs placeholder expansion
   - Verify referenced items appear at correct position in execution order
   - Example: E1 → [enrichment-refs] → R1 should expand to E1 → E2 (from ref) → E3 (from ref) → R1

7. **Middle Office OTC Option Trade Processing** ← NEW (User Preference):
   ```yaml
   # Test: Sequential processing with data dependencies
   # Domain: Middle office OTC option trade processing

   enrichments:
     - id: "enrich-counterparty-credit-rating"
       name: "Lookup Counterparty Credit Rating"
       type: "lookup-enrichment"
       lookup-config:
         lookup-service: "counterparty-db"  # Real database, not inline
         lookup-key: "#counterparty"
       field-mappings:
         - source-field: "creditRating"
           target-field: "counterpartyCreditRating"
         - source-field: "creditLimit"
           target-field: "counterpartyCreditLimit"

   rules:
     - id: "validate-credit-limit"
       name: "Validate Trade Against Credit Limit"
       condition: "#counterpartyCreditRating != null && #notionalAmount < #counterpartyCreditLimit"
       message: "Trade within credit limit"
       severity: "INFO"

   enrichments:
     - id: "calculate-value-at-risk"
       name: "Calculate Value at Risk"
       type: "calculation-enrichment"
       condition: "#counterpartyCreditRating != null"  # Depends on first enrichment
       calculation-config:
         expression: "#notionalAmount * 0.15"  # Simplified VaR calculation
         result-field: "valueAtRisk"

   rules:
     - id: "approve-or-reject-trade"
       name: "Final Trade Approval Decision"
       condition: "#valueAtRisk != null && #valueAtRisk < 10000000"  # Depends on VaR
       message: "Trade approved - VaR within limits"
       severity: "INFO"
   ```

   **Test Data**:
   ```java
   Map<String, Object> tradeData = new HashMap<>();
   tradeData.put("tradeType", "OTC_OPTION");
   tradeData.put("counterparty", "HEDGE_FUND_X");
   tradeData.put("notionalAmount", 50000000.0);
   tradeData.put("optionType", "CALL");
   tradeData.put("strike", 105.0);
   tradeData.put("maturity", "2025-12-31");
   ```

   **Expected Execution Order**:
   1. `enrich-counterparty-credit-rating` → Adds `counterpartyCreditRating`, `counterpartyCreditLimit`
   2. `validate-credit-limit` → Uses credit rating from step 1
   3. `calculate-value-at-risk` → Uses credit rating from step 1
   4. `approve-or-reject-trade` → Uses VaR from step 3

   **Assertions**:
   - Verify each step can access data from previous steps
   - Verify execution order matches document order
   - Verify real database lookup is used (not inline data)

**Risk**: CRITICAL - Without proper tests, we'll repeat the same mistake

---

## Testing Strategy

### Phase 1: Unit Tests (Prove It Works)
1. Test OrderedYamlParser extracts correct item order
2. Test RulesEngine processes items in correct order
3. Test individual item processing methods

### Phase 2: Integration Tests (Prove It Works End-to-End)
1. Create YAML file with E1 → E2 → R1 → R2 pattern
2. Run through RulesEngine
3. Verify execution order through logging
4. Verify data dependencies work (R1 can access E1 results)

### Phase 3: Regression Tests (Prove We Didn't Break Anything)
1. Run ALL existing tests (2,672 tests)
2. Verify zero regressions
3. Verify backward compatibility

---

## Critical Limitation: What This Solution CANNOT Do

**IMPORTANT**: This solution processes items within sections in document order, but **CANNOT interleave items from different section types** due to YAML duplicate key limitation.

### ✅ SUPPORTED (Items Within Sections):
```yaml
enrichments:
  - id: "enrich-1"          # Processed 1st
  - id: "enrich-2"          # Processed 2nd (can depend on enrich-1)
rules:
  - id: "rule-1"            # Processed 3rd (can depend on enrich-1, enrich-2)
  - id: "rule-2"            # Processed 4th (can depend on rule-1)
```

### ✅ SUPPORTED (Groups and Items):
```yaml
enrichments:
  - id: "enrich-1"          # Processed 1st
enrichment-groups:
  - id: "enrich-group-1"    # Processed 2nd
rules:
  - id: "rule-1"            # Processed 3rd
rule-groups:
  - id: "rule-group-1"      # Processed 4th
```

### ❌ NOT SUPPORTED (Interleaved Items Across Sections):
```yaml
# THIS IS IMPOSSIBLE WITH YAML:
enrichments:
  - id: "enrich-1"
rules:
  - id: "rule-1"            # Depends on enrich-1
enrichments:                # ❌ Duplicate key - YAML parser overwrites first enrichments
  - id: "enrich-2"          # Depends on rule-1
rules:                      # ❌ Duplicate key - YAML parser overwrites first rules
  - id: "rule-2"
```

**Workaround for Interleaving**: Use groups to control execution order:
```yaml
enrichments:
  - id: "enrich-1"
enrichment-groups:
  - id: "step-1"
    enrichment-ids: ["enrich-1"]
rules:
  - id: "rule-1"
rule-groups:
  - id: "step-2"
    rule-ids: ["rule-1"]
enrichments:                # Still duplicate key problem!
```

**Real Workaround**: Put all enrichments in one section, all rules in another:
```yaml
enrichments:
  - id: "enrich-1"
  - id: "enrich-2"          # Even if this depends on rule-1, must be here
rules:
  - id: "rule-1"
  - id: "rule-2"
```

**Conclusion**: This solution improves ordering within sections and across section types, but cannot solve the fundamental YAML duplicate key limitation.

---

## CRITICAL ISSUE: Two Types of References in APEX

### Understanding APEX References

APEX has **TWO completely different types of references** that work in fundamentally different ways:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         APEX REFERENCE TYPES                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. FILE-LEVEL REFERENCES (Load External Files)                        │
│     ┌───────────────────────────────────────────────────────┐          │
│     │ enrichment-refs:                                      │          │
│     │   - source: "external.yaml"  ← FILE PATH              │          │
│     └───────────────────────────────────────────────────────┘          │
│     • Resolved: During loading (YamlConfigurationLoader)              │
│     • Effect: Loads file, appends items to lists                      │
│     • Problem: Breaks document order ❌                                │
│     • Fix needed: YES ✅                                               │
│                                                                         │
│  2. ID-LEVEL REFERENCES (Reference Items by ID)                        │
│     ┌───────────────────────────────────────────────────────┐          │
│     │ enrichment-groups:                                    │          │
│     │   - id: "group-1"                                     │          │
│     │     enrichment-ids: ["e1", "e2"]  ← ITEM IDs          │          │
│     │     enrichment-group-references: ["base"]  ← GROUP ID │          │
│     └───────────────────────────────────────────────────────┘          │
│     • Resolved: During execution (EnrichmentGroupFactory)             │
│     • Effect: Looks up items from merged configuration                │
│     • Problem: No - group position determines execution order ✅       │
│     • Fix needed: NO ❌                                                │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 1. File-Level References (External File Loading)

**Purpose**: Load entire YAML files and merge their contents into the main configuration.

**Reference Types**:
- `rule-refs` - Load rules from external files
- `enrichment-refs` - Load enrichments and enrichment-groups from external files
- `data-source-refs` - Load data sources from external files

**YAML Structure**:
```yaml
enrichment-refs:
  - name: "customer-enrichments"
    source: "enrichments/customer.yaml"  # ← FILE PATH (not an ID)
    enabled: true
    description: "Customer data enrichment rules"
```

**How They Work**:
1. Main file is parsed
2. `YamlConfigurationLoader.processEnrichmentReferences()` is called
3. External file is loaded: `loadRuleFile(ref.getSource())`
4. Items from external file are **appended** to main file's lists: `config.getEnrichments().addAll(referencedConfig.getEnrichments())`

**Problem**: Items from referenced files are appended to the END of lists, destroying document order.

#### 2. ID-Level References (Within-Configuration References)

**Purpose**: Reference individual items by ID within the already-merged configuration.

**Reference Types**:
- `enrichment-ids` - List of enrichment IDs (within enrichment-groups)
- `rule-ids` - List of rule IDs (within rule-groups)
- `enrichment-group-references` - List of enrichment group IDs (within enrichment-groups)
- `rule-group-references` - List of rule group IDs (within rule-groups)
- `enrichment-references` - Structured references with sequence control (within enrichment-groups)
- `rule-references` - Structured references with sequence control (within rule-groups)

**YAML Structure**:
```yaml
enrichments:
  - id: "enrich-1"
    name: "Customer Lookup"
    type: "lookup-enrichment"
    # ... enrichment config ...

  - id: "enrich-2"
    name: "Credit Check"
    type: "lookup-enrichment"
    # ... enrichment config ...

enrichment-groups:
  - id: "customer-processing"
    name: "Customer Processing Group"
    operator: "AND"
    enrichment-ids: ["enrich-1", "enrich-2"]  # ← ENRICHMENT IDs (not file paths)
    enrichment-group-references: ["base-enrichments"]  # ← GROUP ID (not file path)
```

**How They Work**:
1. All files are loaded and merged (including file-level references)
2. Groups are created with ID references stored as strings
3. **During execution**, groups look up items by ID from the merged configuration
4. Example: `EnrichmentGroupFactory` calls `enrichmentById.get(ref.getEnrichmentId())`

**Key Difference**: ID-level references are resolved **during execution**, not during loading.

### The Problem with File-Level References

**File-level references are processed AFTER parsing**, which means items from referenced files are **appended to the end** of the main file's lists, **destroying document order**.

**Example**:
```yaml
# main.yaml
enrichments:
  - id: "enrich-1"          # Position 1 in document

enrichment-refs:            # Position 2 in document
  - name: "external-enrichments"
    source: "external.yaml"

rules:
  - id: "rule-1"            # Position 3 in document
```

**Current Processing** (in `YamlConfigurationLoader`):
1. Parse main.yaml → extract section order: `["enrichments", "enrichment-refs", "rules"]`
2. Extract item order: `[enrichments:enrich-1, rules:rule-1]` ← **Missing external items!**
3. Process enrichment-refs → Load external.yaml
4. **Append** external enrichments to config.enrichments list
5. Final enrichments list: `[enrich-1, external-enrich-1, external-enrich-2]`

**Problem**: The item order was extracted BEFORE file-level references were processed, so external items are missing!

### ID-Level References Don't Break Document Order

**ID-level references work differently** - they don't load external files, they just reference items by ID within the already-merged configuration.

**Example**:
```yaml
enrichments:
  - id: "enrich-1"          # Position 1
  - id: "enrich-2"          # Position 2

enrichment-groups:
  - id: "group-1"           # Position 3
    enrichment-ids: ["enrich-1", "enrich-2"]  # ← References by ID, not file
```

**How it works**:
1. All enrichments are loaded and merged (from inline + file-refs)
2. Item order is extracted: `[enrich-1, enrich-2, group-1]`
3. When processing `group-1`, it looks up `enrich-1` and `enrich-2` by ID from the merged enrichments list
4. **Document order is preserved** because groups are processed in document order

**Key insight**: ID-level references are resolved **during execution**, not during loading. The group itself appears in document order, and when executed, it looks up its referenced items.

### Detailed Example: File-Level vs ID-Level References

**Scenario**: You want to process enrichments in this order: E1 → E2 (from external file) → E3 → Group (containing E1, E2)

#### ❌ WRONG: Using only file-level references
```yaml
# main.yaml
enrichments:
  - id: "E1"

enrichment-refs:
  - source: "external.yaml"  # Contains E2

enrichments:  # ❌ DUPLICATE KEY - YAML parser overwrites first enrichments section!
  - id: "E3"

enrichment-groups:
  - id: "group-1"
```

**Result**: Only E3 exists (first enrichments section was overwritten)

#### ✅ CORRECT: Using file-level refs + ID-level refs
```yaml
# main.yaml
enrichments:
  - id: "E1"
  - id: "E3"

enrichment-refs:
  - source: "external.yaml"  # Contains E2

enrichment-groups:
  - id: "group-1"
    enrichment-ids: ["E1", "E2"]  # ← ID-level references
```

**Current behavior** (WITHOUT the fix):
- Item order extracted: `[E1, E3, group-1]` (E2 is missing!)
- Then E2 is appended from external.yaml
- Final enrichments list: `[E1, E3, E2]`
- Execution order: E1 → E3 → group-1 (which contains E1, E2)
- **Problem**: E2 is processed AFTER E3, not between E1 and E3

**With the fix** (placeholder expansion):
- Item order extracted: `[E1, enrichment-refs:*, E3, group-1]`
- Placeholder expanded: `[E1, E2, E3, group-1]`
- Execution order: E1 → E2 → E3 → group-1 (which contains E1, E2)
- **Correct**: E2 is processed between E1 and E3 as intended

### Summary: What Needs to Be Fixed

| Reference Type | Example | When Resolved | Breaks Document Order? | Needs Fix? |
|----------------|---------|---------------|------------------------|------------|
| **File-level** | `enrichment-refs: [{source: "file.yaml"}]` | During loading | ✅ YES | ✅ YES - Need placeholder expansion |
| **ID-level (simple)** | `enrichment-ids: ["enrich-1", "enrich-2"]` | During execution | ❌ NO | ❌ NO - Already works correctly |
| **ID-level (structured)** | `enrichment-references: [{enrichment-id: "e1", sequence: 1}]` | During execution | ❌ NO | ❌ NO - Already works correctly |
| **Group-level** | `enrichment-group-references: ["group-1"]` | During execution | ❌ NO | ❌ NO - Already works correctly |

**Conclusion**:
- Only **file-level references** (`rule-refs`, `enrichment-refs`, `data-source-refs`) need special handling
- **ID-level references** within groups work correctly because:
  1. The group itself appears in document order
  2. ID references are resolved during execution, not loading
  3. Groups look up items from the already-merged configuration

**Example showing both types**:
```yaml
# File-level reference (NEEDS FIX)
enrichment-refs:
  - source: "external.yaml"  # ← Loads file, appends items to end

# ID-level references (ALREADY WORKS)
enrichment-groups:
  - id: "group-1"
    enrichment-ids: ["enrich-1", "enrich-2"]  # ← Looks up by ID during execution
    enrichment-group-references: ["base-group"]  # ← Looks up by ID during execution
```

### How ID-Level References Are Processed

**During Loading** (in `EnrichmentGroupFactory.createEnrichmentGroup()`):
```java
// 1. Create the group
EnrichmentGroup group = new EnrichmentGroup(yg.getId(), yg.getName());

// 2. Store ID references as strings (NOT resolved yet)
List<String> enrichmentIds = yg.getEnrichmentIds();  // ["enrich-1", "enrich-2"]
List<String> groupRefs = yg.getEnrichmentGroupReferences();  // ["base-group"]
```

**During Execution** (when the group is processed):
```java
// 3. Look up enrichments by ID from merged configuration
for (String enrichmentId : enrichmentIds) {
    YamlEnrichment enrichment = enrichmentById.get(enrichmentId);  // ← Lookup by ID
    if (enrichment != null) {
        group.addEnrichment(enrichment);
    }
}

// 4. Look up referenced groups by ID
for (String groupId : groupRefs) {
    EnrichmentGroup referencedGroup = enrichmentGroupsById.get(groupId);  // ← Lookup by ID
    if (referencedGroup != null) {
        // Add all enrichments from referenced group
        for (Enrichment e : referencedGroup.getEnrichments()) {
            group.addEnrichment(e);
        }
    }
}
```

**Why this works for document order**:
- The **group itself** appears in document order (e.g., position 5)
- When position 5 is reached, the group executes
- The group looks up its referenced items from the merged configuration
- The items within the group execute in the order specified by `enrichment-ids`
- **Document order is preserved** because the group's position in the document determines when it executes

### The Solution for File-Level References

**Option 1: Extract item order AFTER processing references**

Modify `YamlConfigurationLoader` to:
1. Parse YAML
2. Process all references (rule-refs, enrichment-refs, data-source-refs)
3. **THEN** extract item order from the merged configuration
4. Track where reference sections appear in document order

**Option 2: Track reference positions and expand them**

When extracting item order:
1. When encountering `enrichment-refs` section, mark position
2. After processing references, insert referenced items at that position
3. Maintain document order with references expanded inline

### Recommended Approach: Option 2 (Reference Expansion)

**Why**: Preserves the intent that `enrichment-refs` at position 2 means "insert external enrichments here"

**Implementation**:
```java
// In OrderedYamlParser.extractItemOrder()
for (String sectionName : yamlMap.keySet()) {
    if (sectionName.equals("enrichment-refs")) {
        // Add placeholder for reference expansion
        itemOrder.add(new ProcessingItem("enrichment-refs", "*"));
    } else if (sectionName.equals("rule-refs")) {
        itemOrder.add(new ProcessingItem("rule-refs", "*"));
    } else if (LIST_SECTIONS.contains(sectionName)) {
        // ... existing code to add items ...
    }
}
```

**Then in YamlConfigurationLoader, AFTER processing references**:
```java
// After processRuleReferences(), processEnrichmentReferences(), etc.
// Expand reference placeholders in item order
expandReferencePlaceholders(config);
```

**Expansion logic**:
```java
private void expandReferencePlaceholders(YamlRuleConfiguration config) {
    List<ProcessingItem> expandedOrder = new ArrayList<>();

    for (ProcessingItem item : config.getItemOrder()) {
        if (item.getSectionType().equals("enrichment-refs") && item.getItemId().equals("*")) {
            // Replace placeholder with actual enrichments from referenced files
            // Track which enrichments came from refs vs. inline
            for (YamlEnrichment enrichment : getReferencedEnrichments(config)) {
                expandedOrder.add(new ProcessingItem("enrichments", enrichment.getId()));
            }
        } else if (item.getSectionType().equals("rule-refs") && item.getItemId().equals("*")) {
            // Replace placeholder with actual rules from referenced files
            for (YamlRule rule : getReferencedRules(config)) {
                expandedOrder.add(new ProcessingItem("rules", rule.getId()));
            }
        } else {
            // Keep non-placeholder items as-is
            expandedOrder.add(item);
        }
    }

    config.setItemOrder(expandedOrder);
}
```

**Challenge**: How to distinguish between inline items and referenced items?

**Solution**: Track referenced items during reference processing:
```java
// In processEnrichmentReferences()
Set<String> referencedEnrichmentIds = new HashSet<>();
for (YamlEnrichmentRef ref : config.getEnrichmentRefs()) {
    YamlRuleConfiguration referencedConfig = loadRuleFile(ref.getSource());
    if (referencedConfig.getEnrichments() != null) {
        for (YamlEnrichment enrichment : referencedConfig.getEnrichments()) {
            referencedEnrichmentIds.add(enrichment.getId());
        }
        config.getEnrichments().addAll(referencedConfig.getEnrichments());
    }
}
// Store for later use
config.setReferencedEnrichmentIds(referencedEnrichmentIds);
```

## Open Questions

1. **Should we support item-level ordering for groups?**
   - enrichment-groups and rule-groups
   - Answer: YES - same approach

2. **What if an item ID doesn't exist?**
   - Log error and skip? Or fail fast?
   - Answer: TBD - need to decide

3. **Performance impact?**
   - Processing items one at a time vs. batch processing
   - Answer: Minimal - enrichments already processed one at a time

4. **Should this be opt-in or automatic?**
   - Always use item-level order if available?
   - Or require metadata flag?
   - Answer: TBD - need to decide

5. **How to handle dependencies that cross section boundaries?**
   - E.g., enrich-2 depends on rule-1, but all enrichments execute before all rules
   - Answer: TBD - may need to document this limitation clearly

6. **How to handle multiple reference sections?** ← NEW
   - Can you have multiple `enrichment-refs` sections?
   - Answer: NO - YAML duplicate key limitation applies here too
   - But you CAN have multiple references within one section

7. **What if a referenced file also has references?** ← NEW
   - Nested references
   - Answer: Currently handled by `loadRuleFile()` which skips reference processing to avoid recursion
   - Need to ensure item order extraction happens at the right level

8. **Do ID-level references need special handling?** ← NEW
   - Answer: NO - ID-level references (`enrichment-ids`, `rule-ids`, `enrichment-group-references`, `rule-group-references`) are resolved during execution, not loading
   - The group itself appears in document order, and when executed, it looks up items by ID
   - Document order is preserved because the group's position determines when it executes
   - **No changes needed** for ID-level references

9. **Should pipeline support multiple instances per document?** ← NEW
   - Current: `pipeline` is a single object (`PipelineConfiguration pipeline`)
   - Future: May need to change to `List<PipelineConfiguration> pipelines` to support multiple pipelines
   - If changed, pipelines would need to be added to LIST_SECTIONS and included in item-level processing
   - Each pipeline would be processed as a single item (not broken down into steps)
   - **Decision needed**: Is this a future requirement?

---

## Document Revision History

### 2025-11-06 - Analysis Review and Corrections

**Review Conducted**: Comprehensive review against coding principles, codebase patterns, and user preferences

**Critical Corrections Made**:

1. **Fixed Method Signature Mismatch** (Lines 712-726):
   - **Issue**: `processEnrichmentItem()` was using `processEnrichment()` which returns `Object`, not `RuleResult`
   - **Fix**: Changed to use `processEnrichmentWithResult()` which returns `RuleResult` directly
   - **Impact**: HIGH - Implementation would have failed without this fix

2. **Added Error Handling Strategy** (Lines 799-898):
   - Added comprehensive error handling section covering:
     - Missing item IDs (graceful degradation)
     - Enrichment/rule execution failures (already handled)
     - SpEL evaluation errors (already handled by UnifiedRuleEvaluator)
     - Circular dependencies (not possible with design)
     - Null/empty item order (fallback to section-level processing)
   - **Principle**: "Configuration errors should NEVER throw exceptions that break application flow"

3. **Enhanced Test Strategy with Domain Examples** (Lines 901-996):
   - Added Test Case #7: Middle Office OTC Option Trade Processing
   - Uses real database lookups (not inline data)
   - Domain-specific examples: counterparty credit rating, VaR calculation, trade approval
   - **User Preference**: "User prefers documentation and test examples to use middle office trade processing domain (OTC options)"

4. **Clarified Helper Methods** (Lines 774-799):
   - Documented that `findRuleById()` already exists in `YamlEnrichmentProcessor` (line 1288)
   - Documented that `findEnrichmentById()` does NOT exist and needs to be created
   - Added detailed JavaDoc for new helper method

**Review Report**: See `ANALYSIS_REVIEW_REPORT.md` for complete review findings

**Overall Assessment**: ✅ **APPROVED WITH CORRECTIONS APPLIED**

**Status**: Ready for implementation after user approval

---

## Next Steps

1. **REVIEW THIS DOCUMENT** with user - especially the reference handling approach
2. Get approval on approach
3. Create detailed test plan including reference expansion tests
4. Implement Step 1 (ProcessingItem class)
5. Implement Step 2 (OrderedYamlParser enhancement with reference placeholders)
6. Implement Step 3 (YamlRuleConfiguration fields)
7. Implement Step 4 (Track referenced items)
8. Implement Step 5 (Expand reference placeholders)
9. Write tests for Steps 1-5
10. Verify tests pass
11. Continue with Steps 6-8 incrementally

---

## Summary of Changes

### Files to Create:
1. `apex-core/src/main/java/dev/mars/apex/core/config/yaml/ProcessingItem.java` - New class

### Files to Modify:
1. `apex-core/src/main/java/dev/mars/apex/core/config/yaml/OrderedYamlParser.java` - Add `extractItemOrder()` method with reference placeholder support
2. `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleConfiguration.java` - Add `itemOrder` field and referenced item tracking fields
3. `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlConfigurationLoader.java` - Track referenced items and expand placeholders
4. `apex-core/src/main/java/dev/mars/apex/core/engine/config/RulesEngine.java` - Add item-level processing logic

### Test Files to Create:
1. `apex-demo/src/test/java/dev/mars/apex/demo/sequencing/ItemLevelOrderingTest.java`
2. `apex-demo/src/test/java/dev/mars/apex/demo/sequencing/ItemLevelOrderingTest.yaml`
3. `apex-demo/src/test/java/dev/mars/apex/demo/sequencing/ReferenceExpansionOrderingTest.java` ← NEW
4. `apex-demo/src/test/java/dev/mars/apex/demo/sequencing/ReferenceExpansionOrderingTest.yaml` ← NEW
5. `apex-demo/src/test/resources/sequencing/external-enrichments.yaml` ← NEW (referenced file)
6. `apex-demo/src/test/resources/sequencing/external-rules.yaml` ← NEW (referenced file)
7. Additional test files for all section types

### Backward Compatibility:
- ✅ Existing YAML files work unchanged
- ✅ If `itemOrder` is null/empty, falls back to section-level processing
- ✅ All existing tests continue to pass

### Risk Assessment:
- **Step 1 (ProcessingItem)**: NONE - New file
- **Step 2 (OrderedYamlParser)**: LOW - Only adds new functionality, includes reference placeholder support
- **Step 3 (YamlRuleConfiguration)**: LOW - Just adds fields
- **Step 4 (Track referenced items)**: LOW - Only adds tracking, doesn't change existing behavior
- **Step 5 (Expand placeholders)**: MEDIUM - New logic to expand references, but only affects new functionality
- **Step 6 (RulesEngine)**: MEDIUM - Changes core execution logic
- **Step 7 (Helper methods)**: LOW - New methods, reuse existing code
- **Step 8 (Testing)**: CRITICAL - Must be comprehensive, including reference expansion tests

---

**CRITICAL**: Do NOT proceed with implementation until this analysis is reviewed and approved.

