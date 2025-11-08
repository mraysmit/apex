# APEX Sequential Processing - Design Decisions

## Document Purpose
Track all design decisions for APEX sequential processing and groups-only execution logic.
This is a complex area - we need clear agreement before implementing.

## Quick Summary

### ✅ All Decisions Confirmed

1. **Groups-Only Logic**: Enrichments referenced by groups are definitions only; standalone enrichments execute directly
2. **Per-File Scoping**: Groups-only logic applies per file (inline enrichments not affected by external groups)
3. **Numbered Suffixes**: Use `enrichments-1`, `enrichments-2` to work around YAML duplicate key limitation
4. **Test Strategy**: ALL tests MUST use ExecutionTracker to definitively prove YAML order execution

### 🎯 Current Focus

**Phase 1**: Create Test 4 (Standalone Enrichments) to prove current groups-only logic fix works

### 📋 Test Status

- ✅ Test 1: enrichment-refs position (passing but has duplicate key issue)
- ✅ Test 2: Groups-only basic (passing but has duplicate key issue)
- ✅ Test 3: Enrichment-groups can execute (passing)
- ❌ Test 4: Standalone enrichments (NOT CREATED - CRITICAL)
- ❌ Test 5: Numbered suffixes basic (NOT CREATED)
- ❌ Test 6: Numbered suffixes with groups (NOT CREATED)
- ❌ Test 7: Cross-file scoping (NOT CREATED)
- ❌ Test 8: Rules and rule-groups (NOT CREATED)

---

## Critical Requirement (User Stated)

> "the most important guarantee in apex is that the yaml must without any exception be processed in the order of the yaml files. The yaml files is the definitive control for the business logic."

**YAML document order is SACRED and MUST be respected WITHOUT ANY EXCEPTION.**

---

## Issue 1: enrichment-refs Position

### Problem
enrichment-refs placeholder was removed at parse time instead of being executed at reference position.

### Solution
✅ **FIXED** - enrichment-refs is now a placeholder in itemOrder that gets expanded at the correct position.

### Status
**RESOLVED** - Test 1 passes

---

## Issue 2: Groups and Individual Items Coexistence

### User Requirement (Original Statement)

> "within one yaml file, whenever enrichments and enrichment-groups co-exists, we only want to execute enrichment-groups because enrichments are included in groups to control the enrichments execution order."

### User Clarification (Latest)

> "that is correct but there may be other enrichments that are not part of the enrichment-group in the same yaml file. These other enrichments need to be executed."

### Correct Understanding

When a file has BOTH enrichments and enrichment-groups:

1. **Enrichments referenced by enrichment-groups** → Definitions only (executed by groups)
2. **Enrichments NOT referenced by enrichment-groups** → Execute directly (standalone)
3. **Enrichment-groups** → Execute (orchestrate their referenced enrichments)

### Example

```yaml
enrichments:
  - id: "enrich-1"  # Referenced by group-A → definition only
  - id: "enrich-2"  # Referenced by group-A → definition only
  - id: "enrich-3"  # NOT referenced → execute directly
  - id: "enrich-4"  # NOT referenced → execute directly

enrichment-groups:
  - id: "group-A"
    enrichment-ids: ["enrich-1", "enrich-2"]
```

**Expected itemOrder:**
```
enrichments:enrich-3
enrichments:enrich-4
enrichment-groups:group-A
```

**Expected Execution:**
1. enrich-3 executes (standalone)
2. enrich-4 executes (standalone)
3. group-A executes → calls enrich-1, then enrich-2

### Current Implementation Status

❌ **INCORRECT** - Current fix skips ALL enrichments when groups exist:

```java
if (!hasEnrichmentGroups) {
    // Track enrichment IDs
} else {
    LOGGER.info("Skipped tracking enrichment IDs (enrichment-groups present - enrichments are definitions only)");
}
```

### Required Fix

```java
// Collect enrichment IDs that are referenced by enrichment-groups
Set<String> referencedByGroups = new HashSet<>();
if (hasEnrichmentGroups) {
    for (YamlEnrichmentGroup group : referencedConfig.getEnrichmentGroups()) {
        if (group.getEnrichmentIds() != null) {
            referencedByGroups.addAll(group.getEnrichmentIds());
        }
    }
}

// Track enrichment IDs that are NOT referenced by any enrichment-group
for (YamlEnrichment enrichment : referencedConfig.getEnrichments()) {
    if (enrichment.getId() != null) {
        if (!referencedByGroups.contains(enrichment.getId())) {
            // Not referenced by any group - track for direct execution
            referencedEnrichmentIds.add(enrichment.getId());
        }
    }
}
```

### Same Logic for Rules

The same logic applies to rules and rule-groups:
- Rules referenced by rule-groups → definitions only
- Rules NOT referenced by rule-groups → execute directly
- Rule-groups → execute (orchestrate their referenced rules)

### Status
✅ **CONFIRMED BY USER** - This is the correct logic

---

## Issue 3: Multiple Sections with Same Name (YAML Limitation)

### Problem

YAML specification does NOT allow duplicate keys. This prevents:

```yaml
enrichments:
  - id: "inline-before"
    ...

enrichment-refs:
  - name: "external-groups"
    source: "external.yaml"

enrichments:  # ❌ This OVERWRITES the first enrichments section!
  - id: "inline-after"
    ...
```

### User Use Case

> "users might have an enrichment group that is referencing an external configuration yaml. This is a common practice to support reusability and avoid repetition. But we also might add some additional inline enrichments for a specific business case."

**Example:**
- Load external enrichment-groups (reusable)
- Add inline enrichments before/after external groups (specific business logic)
- Both should execute in document order

### Proposed Solution: Numbered Suffixes

**User Preference:** "I prefer the concept of enrichments-1"

Allow numbered suffixes to work around YAML duplicate key limitation:

```yaml
enrichments-1:
  - id: "inline-before"
    ...

enrichment-refs:
  - name: "external-groups"
    source: "external.yaml"

enrichments-2:
  - id: "inline-after"
    ...
```

### Implementation Approach

1. **Parser recognizes numbered suffixes**: `enrichments-1`, `enrichments-2`, etc.
2. **Normalize section names**: Strip `-\d+$` suffix → `enrichments`
3. **Merge all numbered sections**: Preserve document order
4. **Apply groups-only logic**: After merging, check which enrichments are referenced by groups

### Sections Supporting Numbered Suffixes

- `enrichments-1`, `enrichments-2`, ...
- `rules-1`, `rules-2`, ...
- `enrichment-groups-1`, `enrichment-groups-2`, ...
- `rule-groups-1`, `rule-groups-2`, ...
- `transformations-1`, `transformations-2`, ...

### Key Question: How to Apply Groups-Only Logic?

**Scenario A: All in main file**
```yaml
enrichments-1:
  - id: "e1"  # Referenced by group
  - id: "e2"  # Standalone

enrichment-groups-1:
  - id: "group-A"
    enrichment-ids: ["e1"]

enrichments-2:
  - id: "e3"  # Standalone
```

**Question**: Should we:
- **Option A**: Merge all `enrichments-*` sections first, then check which are referenced by all `enrichment-groups-*`?
- **Option B**: Check per numbered section (e.g., enrichments-1 vs enrichment-groups-1)?

**Recommendation**: Option A - Merge all numbered sections of same type, then apply groups-only logic globally.

**Scenario B: External file with groups + inline enrichments**
```yaml
# main.yaml
enrichments-1:
  - id: "inline-before"  # Should execute (not part of external groups)

enrichment-refs:
  - name: "external-groups"
    source: "external.yaml"  # Has enrichment-groups

enrichments-2:
  - id: "inline-after"  # Should execute (not part of external groups)
```

```yaml
# external.yaml
enrichments:
  - id: "ext-1"  # Referenced by ext-group
  - id: "ext-2"  # Standalone

enrichment-groups:
  - id: "ext-group"
    enrichment-ids: ["ext-1"]
```

**Expected itemOrder:**
```
enrichments:inline-before
enrichments:ext-2
enrichment-groups:ext-group
enrichments:inline-after
```

**Answer**: YES - User confirmed: "yes of course they should execute"

**Clarification**: The groups-only logic applies **PER FILE**:
- External file's enrichments are checked against external file's enrichment-groups
- Main file's enrichments are checked against main file's enrichment-groups
- Enrichments in main file are NOT affected by groups in external file

### Status
✅ **CONFIRMED BY USER** - Numbered suffix approach is correct. Groups-only logic applies per file.

---

## Summary of Confirmed Decisions

### ✅ Decision 1: Groups-Only Logic (Issue 2)
**CONFIRMED** - Enrichments referenced by groups → definitions only
- Enrichments NOT referenced by groups → execute directly
- Same for rules and rule-groups

### ✅ Decision 2: Numbered Suffixes (Issue 3)
**CONFIRMED** - Implement numbered suffixes (`enrichments-1`, `enrichments-2`)
- User preference: "I prefer the concept of enrichments-1"

### ✅ Decision 3: Groups-Only Logic with Numbered Suffixes
**CONFIRMED** - Merge all numbered sections first, then apply groups-only logic globally
- All `enrichments-*` sections merge into one list
- All `enrichment-groups-*` sections merge into one list
- Check which enrichments are referenced by ANY enrichment-group

### ✅ Decision 4: Cross-File Groups-Only Logic (PER FILE SCOPING)
**CONFIRMED** - Groups-only logic applies **PER FILE**
- External file: Check enrichments against enrichment-groups in THAT file
- Main file: Check enrichments against enrichment-groups in THAT file
- Inline enrichments in main file ALWAYS execute (not affected by external groups)
- User confirmed: "yes of course they should execute"

**Example:**
```yaml
# main.yaml
enrichments-1:
  - id: "inline-before"  # ✅ Executes (not part of external groups)

enrichment-refs:
  - source: "external.yaml"  # Has enrichment-groups

enrichments-2:
  - id: "inline-after"  # ✅ Executes (not part of external groups)
```

```yaml
# external.yaml
enrichments:
  - id: "ext-1"  # ❌ Definition only (referenced by ext-group)
  - id: "ext-2"  # ✅ Executes (NOT referenced by ext-group)

enrichment-groups:
  - id: "ext-group"
    enrichment-ids: ["ext-1"]
```

**Expected itemOrder:**
```
enrichments:inline-before
enrichments:ext-2
enrichment-groups:ext-group
enrichments:inline-after
```

---

## Implementation Plan

### Phase 1: Fix Groups-Only Logic (Issue 2)
**Status**: ❌ PARTIALLY IMPLEMENTED - Test 4 FAILS

**Test 4 Results**:
- Expected: `[standalone-1, standalone-2, grouped-1, grouped-2]` (4 items)
- Actual: `[standalone-1, grouped-1, standalone-2, grouped-2, grouped-1, grouped-2]` (6 items)
- **Problem**: grouped-1 and grouped-2 execute TWICE (once directly, once via group)

**Root Cause Analysis**:
1. `OrderedYamlParser.extractItemOrder()` adds ALL enrichments to itemOrder (lines 240-254)
2. `OrderedYamlParser.extractItemOrder()` adds ALL enrichment-groups to itemOrder (lines 240-254)
3. Current fix in `processEnrichmentReferences()` only applies to EXTERNAL files (enrichment-refs)
4. Main file's enrichments are NOT checked against main file's enrichment-groups

**Solution**:
Need to filter itemOrder AFTER it's built to remove enrichments that are referenced by groups.

**Implementation Location**:
Add new method `applyGroupsOnlyLogic()` in `YamlConfigurationLoader` and call it after line 98:

```java
// Copy section order and item order into the configuration
config.setSectionOrder(orderedConfig.getSectionOrder());
config.setItemOrder(orderedConfig.getItemOrder());

// Apply groups-only logic: Remove enrichments/rules from itemOrder if they're referenced by groups
applyGroupsOnlyLogic(config);  // <-- NEW METHOD

// Process external rule references
processRuleReferences(config);
```

**New Method Logic**:
```java
private void applyGroupsOnlyLogic(YamlRuleConfiguration config) {
    if (config.getItemOrder() == null) return;

    // Collect enrichment IDs referenced by enrichment-groups
    Set<String> referencedEnrichmentIds = new HashSet<>();
    if (config.getEnrichmentGroups() != null) {
        for (YamlEnrichmentGroup group : config.getEnrichmentGroups()) {
            if (group.getEnrichmentIds() != null) {
                referencedEnrichmentIds.addAll(group.getEnrichmentIds());
            }
        }
    }

    // Collect rule IDs referenced by rule-groups
    Set<String> referencedRuleIds = new HashSet<>();
    if (config.getRuleGroups() != null) {
        for (YamlRuleGroup group : config.getRuleGroups()) {
            if (group.getRuleIds() != null) {
                referencedRuleIds.addAll(group.getRuleIds());
            }
        }
    }

    // Filter itemOrder: Remove enrichments/rules that are referenced by groups
    List<ProcessingItem> filteredOrder = new ArrayList<>();
    int removedCount = 0;

    for (ProcessingItem item : config.getItemOrder()) {
        boolean shouldRemove = false;

        if ("enrichments".equals(item.getSectionType()) &&
            referencedEnrichmentIds.contains(item.getItemId())) {
            shouldRemove = true;
        } else if ("rules".equals(item.getSectionType()) &&
                   referencedRuleIds.contains(item.getItemId())) {
            shouldRemove = true;
        }

        if (shouldRemove) {
            LOGGER.fine("Removed " + item.getSectionType() + ":" + item.getItemId() +
                       " from itemOrder (referenced by group - definition only)");
            removedCount++;
        } else {
            filteredOrder.add(item);
        }
    }

    config.setItemOrder(filteredOrder);
    LOGGER.info("Applied groups-only logic: removed " + removedCount +
               " items from itemOrder (definitions referenced by groups)");
}
```

**TODO**:
1. Implement `applyGroupsOnlyLogic()` method
2. Call it after itemOrder is set
3. Run Test 4 to verify fix
4. Run Tests 1, 2, 3 to ensure no regression
5. Keep existing fix in `processEnrichmentReferences()` for external files

### Phase 2: Implement Numbered Suffixes (Issue 3)
**Status**: Partially started

**Changes Made**:
- Added `NUMBERED_SUFFIX_SECTIONS` constant
- Added `normalizeSectionName()` method
- Added `isKnownSection()` method

**TODO**:
- Modify `OrderedYamlParser.parseYamlString()` to handle numbered sections
- Merge numbered sections while preserving document order
- Update `extractItemOrder()` to handle numbered sections
- Modify Jackson parsing to merge numbered sections into single list
- Test numbered suffixes work correctly

### Phase 3: Integration Testing
**TODO**:
- Test all scenarios from this document
- Verify 86 failing tests and categorize them
- Update tests to expect correct behavior
- Ensure no regressions

---

## Next Steps

1. ✅ **Get user agreement on all open questions** - DONE
2. ✅ **Update this document with confirmed decisions** - DONE
3. ⏳ **Implement the agreed-upon logic** - IN PROGRESS
4. ⏳ **Create comprehensive tests to verify all scenarios** - PENDING
5. ⏳ **Update failing tests to expect correct behavior** - PENDING

---

## Test Design: Definitive Proof of YAML Order Execution

### Critical Requirement
**Every test MUST use ExecutionTracker to record execution order and verify EXACT sequence.**

Tests that don't use ExecutionTracker cannot definitively prove YAML order is preserved.

### Test Strategy

All tests use calculation-enrichment with ExecutionTracker to record execution:

```yaml
- id: "test-item"
  type: "calculation-enrichment"
  calculation-config:
    expression: "T(dev.mars.apex.demo.sequencing.ExecutionTracker).recordAndReturn('test-item', 'value')"
    result-field: "resultField"
  field-mappings:
    - source-field: "resultField"
      target-field: "resultField"
```

Then verify: `assertEquals(expectedOrder, ExecutionTracker.getExecutionLog())`

---

## Test Suite Design

### Test 1: enrichment-refs Position ✅ PASSING
**Purpose**: Prove enrichment-refs executes at EXACT reference position

**YAML Structure**:
```yaml
enrichments:
  - id: "inline-before"

enrichment-refs:
  - source: "external.yaml"  # Has: external-1, external-2

enrichments:  # ❌ DUPLICATE KEY - will be overwritten!
  - id: "inline-after"
```

**Expected Order**: `[inline-before, external-1, external-2, inline-after]`

**Current Status**: ❌ FAILS - inline-before is missing (duplicate key problem)

**Action**: Keep this test to prove numbered suffixes fix the problem

---

### Test 2: Groups-Only Execution (Basic) ✅ PASSING
**Purpose**: Prove ONLY enrichment-groups execute (not individual enrichments)

**YAML Structure**:
```yaml
# main.yaml
enrichments:
  - id: "inline-before"

enrichment-refs:
  - source: "external.yaml"  # Has enrichments + enrichment-groups

enrichments:  # ❌ DUPLICATE KEY
  - id: "inline-after"
```

```yaml
# external.yaml
enrichments:
  - id: "e1"  # Referenced by group
  - id: "e2"  # Referenced by group

enrichment-groups:
  - id: "group"
    enrichment-ids: ["e1", "e2"]
```

**Expected Order**: `[inline-before, e1, e2, inline-after]` (4 items, not 6)

**Current Status**: ✅ PASSES (but has duplicate key problem)

**Action**: Keep this test, will work better with numbered suffixes

---

### Test 3: Enrichment-Groups Can Execute Enrichments ✅ PASSING
**Purpose**: Prove enrichment-groups can find and execute enrichments

**YAML Structure**:
```yaml
enrichment-refs:
  - source: "external.yaml"

# external.yaml has:
enrichment-groups:
  - id: "group"
    enrichment-ids: ["lookup-enrichment"]

enrichments:
  - id: "lookup-enrichment"
    type: "lookup-enrichment"
    ...
```

**Expected**: Lookup enrichment executes and maps fields

**Current Status**: ✅ PASSES

**Action**: Keep this test

---

### Test 4: Standalone Enrichments (NEW - CRITICAL)
**Purpose**: Prove enrichments NOT referenced by groups execute directly

**YAML Structure**:
```yaml
enrichments:
  - id: "standalone-1"  # NOT referenced by any group
  - id: "grouped-1"     # Referenced by group
  - id: "standalone-2"  # NOT referenced by any group
  - id: "grouped-2"     # Referenced by group

enrichment-groups:
  - id: "group-A"
    enrichment-ids: ["grouped-1", "grouped-2"]
```

**Expected Order**: `[standalone-1, standalone-2, group-A]`

**Expected Execution**:
1. standalone-1 executes directly
2. standalone-2 executes directly
3. group-A executes → calls grouped-1, then grouped-2

**ExecutionTracker Log**: `[standalone-1, standalone-2, grouped-1, grouped-2]`

**Current Status**: ❌ NOT IMPLEMENTED

**Action**: CREATE THIS TEST - This is CRITICAL to prove groups-only logic

---

### Test 5: Numbered Suffixes Basic (NEW - CRITICAL)
**Purpose**: Prove numbered suffixes work and preserve document order

**YAML Structure**:
```yaml
enrichments-1:
  - id: "e1"

enrichment-refs:
  - source: "external.yaml"  # Has: ext-1, ext-2

enrichments-2:
  - id: "e2"

enrichments-3:
  - id: "e3"
```

**Expected Order**: `[e1, ext-1, ext-2, e2, e3]`

**Current Status**: ❌ NOT IMPLEMENTED

**Action**: CREATE THIS TEST - Proves numbered suffixes solve duplicate key problem

---

### Test 6: Numbered Suffixes with Groups-Only Logic (NEW - CRITICAL)
**Purpose**: Prove numbered suffixes work with groups-only logic

**YAML Structure**:
```yaml
enrichments-1:
  - id: "standalone-1"  # NOT referenced
  - id: "grouped-1"     # Referenced by group-1

enrichment-groups-1:
  - id: "group-1"
    enrichment-ids: ["grouped-1"]

enrichments-2:
  - id: "standalone-2"  # NOT referenced
  - id: "grouped-2"     # Referenced by group-2

enrichment-groups-2:
  - id: "group-2"
    enrichment-ids: ["grouped-2"]
```

**Expected Order**: `[standalone-1, group-1, standalone-2, group-2]`

**ExecutionTracker Log**: `[standalone-1, grouped-1, standalone-2, grouped-2]`

**Current Status**: ❌ NOT IMPLEMENTED

**Action**: CREATE THIS TEST - Proves numbered suffixes + groups-only logic work together

---

### Test 7: Cross-File Groups-Only Logic (NEW - CRITICAL)
**Purpose**: Prove per-file scoping - inline enrichments NOT affected by external groups

**YAML Structure**:
```yaml
# main.yaml
enrichments-1:
  - id: "inline-before"  # Should execute (not part of external groups)

enrichment-refs:
  - source: "external.yaml"

enrichments-2:
  - id: "inline-after"  # Should execute (not part of external groups)
```

```yaml
# external.yaml
enrichments:
  - id: "ext-grouped"    # Referenced by ext-group
  - id: "ext-standalone" # NOT referenced

enrichment-groups:
  - id: "ext-group"
    enrichment-ids: ["ext-grouped"]
```

**Expected Order**: `[inline-before, ext-standalone, ext-group, inline-after]`

**ExecutionTracker Log**: `[inline-before, ext-standalone, ext-grouped, inline-after]`

**Current Status**: ❌ NOT IMPLEMENTED

**Action**: CREATE THIS TEST - Proves per-file scoping works correctly

---

### Test 8: Rules and Rule-Groups (NEW)
**Purpose**: Prove same logic works for rules and rule-groups

**YAML Structure**:
```yaml
rules-1:
  - id: "standalone-rule"  # NOT referenced
  - id: "grouped-rule"     # Referenced by group

rule-groups-1:
  - id: "rule-group"
    rule-ids: ["grouped-rule"]
```

**Expected Order**: `[standalone-rule, rule-group]`

**Expected Execution**:
1. standalone-rule executes directly
2. rule-group executes → calls grouped-rule

**Current Status**: ❌ NOT IMPLEMENTED

**Action**: CREATE THIS TEST - Proves logic works for rules too

---

## Test Implementation Priority

### Phase 1: Prove Current Fix Works
1. **Test 4**: Standalone enrichments (proves groups-only logic)
2. Run Test 1, 2, 3 to ensure no regression

### Phase 2: Implement Numbered Suffixes
3. **Test 5**: Numbered suffixes basic
4. **Test 6**: Numbered suffixes with groups-only logic
5. **Test 7**: Cross-file groups-only logic

### Phase 3: Complete Coverage
6. **Test 8**: Rules and rule-groups
7. Fix Test 1 and Test 2 to use numbered suffixes

---

## Test Naming Convention

- `Test4_StandaloneEnrichmentsTest.java` - Standalone enrichments with groups
- `Test5_NumberedSuffixesBasicTest.java` - Numbered suffixes basic case
- `Test6_NumberedSuffixesWithGroupsTest.java` - Numbered suffixes + groups-only
- `Test7_CrossFileGroupsScopingTest.java` - Per-file scoping
- `Test8_RulesAndRuleGroupsTest.java` - Rules and rule-groups logic

---

## 🚨 IMPLEMENTATION STATUS & TEST RESULTS (2025-11-07)

### Test Execution Results

| Test | Status | Expected Order | Actual Order | Analysis |
|------|--------|----------------|--------------|----------|
| Test 1 | ✅ PASS | [inline-before, external-1, external-2, inline-after] | [inline-before, external-1, external-2, inline-after] | enrichment-refs executes at correct position |
| Test 2 | ✅ PASS | [inline-before, e1, e2, inline-after] | [inline-before, e1, e2, inline-after] | Groups-only logic works for EXTERNAL files |
| Test 3 | ⚠️ N/A | N/A | N/A | Different test (minimal enrichment-group) |
| Test 4 | ❌ FAIL | [standalone-1, standalone-2, grouped-1, grouped-2] | [standalone-1, grouped-1, standalone-2, grouped-2, grouped-1, grouped-2] | **CRITICAL: Groups-only logic BROKEN for MAIN file** |

### Critical Finding: Test 4 Failure Analysis

**Test 4 YAML Structure** (test4-main.yaml):
```
Position 1: standalone-1 (enrichment, NOT in any group)
Position 2: grouped-1 (enrichment, IN group-A)
Position 3: standalone-2 (enrichment, NOT in any group)
Position 4: grouped-2 (enrichment, IN group-A)
Position 5: group-A (enrichment-group, references grouped-1 and grouped-2)
```

**Expected Execution** (respecting YAML document order):
1. standalone-1 (position 1) - executes directly
2. standalone-2 (position 3) - executes directly (grouped-1 SKIPPED at position 2)
3. grouped-1 (position 5) - executed by group-A
4. grouped-2 (position 5) - executed by group-A

**Actual Execution** (VIOLATES document order):
1. standalone-1 (position 1) - ✅ correct
2. grouped-1 (position 2) - ❌ WRONG (should be skipped)
3. standalone-2 (position 3) - ✅ correct
4. grouped-2 (position 4) - ❌ WRONG (should be skipped)
5. grouped-1 (position 5) - ✅ correct (from group-A)
6. grouped-2 (position 6) - ✅ correct (from group-A)

**Root Cause**: `OrderedYamlParser.extractItemOrder()` adds ALL enrichments to itemOrder (including grouped-1 and grouped-2 at positions 2 and 4). The missing `applyGroupsOnlyLogic()` method should filter these out so they only execute at position 5 via group-A.

### Implementation Status

| Issue | Design Status | Implementation Status | Test Status |
|-------|---------------|----------------------|-------------|
| Issue 1: enrichment-refs Position | ✅ Designed | ✅ Implemented | ✅ Test 1 PASSES |
| Issue 2: Groups-Only Logic (External) | ✅ Designed | ✅ Implemented | ✅ Test 2 PASSES |
| Issue 2: Groups-Only Logic (Main File) | ✅ Designed | ❌ NOT Implemented | ❌ Test 4 FAILS |
| Issue 3: Numbered Suffixes | ✅ Designed | ❌ NOT Implemented | ❌ Tests 5-6 NOT CREATED |

### The Missing Fix: applyGroupsOnlyLogic()

**Location**: `YamlConfigurationLoader.java` after line 98

**Complete Implementation**:
```java
private void applyGroupsOnlyLogic(YamlRuleConfiguration config) {
    if (config.getItemOrder() == null) return;

    // Collect enrichment IDs referenced by enrichment-groups
    Set<String> referencedEnrichmentIds = new HashSet<>();
    if (config.getEnrichmentGroups() != null) {
        for (YamlEnrichmentGroup group : config.getEnrichmentGroups()) {
            if (group.getEnrichmentIds() != null) {
                referencedEnrichmentIds.addAll(group.getEnrichmentIds());
            }
        }
    }

    // Collect rule IDs referenced by rule-groups
    Set<String> referencedRuleIds = new HashSet<>();
    if (config.getRuleGroups() != null) {
        for (YamlRuleGroup group : config.getRuleGroups()) {
            if (group.getRuleIds() != null) {
                referencedRuleIds.addAll(group.getRuleIds());
            }
        }
    }

    // Filter itemOrder: Remove enrichments/rules referenced by groups
    List<ProcessingItem> filteredOrder = new ArrayList<>();
    for (ProcessingItem item : config.getItemOrder()) {
        boolean shouldRemove = false;

        if ("enrichments".equals(item.getSectionType()) &&
            referencedEnrichmentIds.contains(item.getItemId())) {
            shouldRemove = true;  // Skip - will execute via group
        } else if ("rules".equals(item.getSectionType()) &&
                   referencedRuleIds.contains(item.getItemId())) {
            shouldRemove = true;  // Skip - will execute via group
        }

        if (!shouldRemove) {
            filteredOrder.add(item);
        }
    }

    config.setItemOrder(filteredOrder);
}
```

**Call Site**: Add after line 98 in `YamlConfigurationLoader.loadConfiguration()`:
```java
config.setItemOrder(itemOrder);
applyGroupsOnlyLogic(config);  // ← ADD THIS LINE
```

### Test Coverage Gaps

**What Tests 1-4 Prove**:
- ✅ enrichment-refs position expansion works correctly
- ✅ Groups-only logic works for EXTERNAL files
- ❌ Groups-only logic BROKEN for MAIN file

**What Tests 1-4 Do NOT Prove**:
- ❓ rule-refs position expansion (not tested)
- ❓ rule-group-refs groups-only logic (not tested)
- ❓ Multiple enrichment-refs in sequence (not tested)
- ❓ Mixed enrichments, rules, and groups in complex order (not tested)
- ❓ enrichment-group-refs position expansion (not tested)
- ❓ rule-group-refs position expansion (not tested)

**Potential Undiscovered Order Violations**:
1. **rule-refs expansion**: May not execute at correct position
2. **rule-group-refs**: Groups-only logic may be broken for main file (same as enrichments)
3. **Multiple refs in sequence**: Order may not be preserved
4. **Complex interleaving**: Enrichments, rules, and groups may not execute in correct order
5. **Nested groups**: Group references within groups may violate order

### Critical Recommendation

**DO NOT ASSUME** the groups-only issue is the ONLY order violation. Current test coverage (4 of 8 tests) is insufficient to make that claim.

**Next Steps**:
1. **IMMEDIATE**: Implement `applyGroupsOnlyLogic()` to fix Test 4
2. **VERIFY**: Re-run all tests to ensure Test 4 passes and no regressions
3. **EXPAND**: Create Tests 5-8 to validate other scenarios
4. **VALIDATE**: Only after ALL 8 tests pass can we claim YAML document order is fully respected

---

## 🎯 IMPLEMENTATION PLAN (Following prompts.txt Principles)

### Core Principles from prompts.txt

1. **Investigation Before Implementation**: Understand before you change
2. **Follow Existing Patterns**: Learn from existing code in the same project
3. **Work Incrementally**: Test after every change
4. **Read Logs Carefully**: Scan test logs properly for errors
5. **Do Not Guess**: Verify assumptions with actual code
6. **Do Not Reinvent**: Use existing APEX infrastructure
7. **Fail Honestly**: Let tests fail when there are real problems

### Phase 1: Fix Groups-Only Logic for Main File (IMMEDIATE)

**Goal**: Make Test 4 pass by implementing `applyGroupsOnlyLogic()`

#### Step 1.1: Investigate Existing Implementation
- ✅ **DONE**: Found existing groups-only logic in `processEnrichmentReferences()` (lines 539-647)
- ✅ **DONE**: Confirmed it works for EXTERNAL files (Test 2 passes)
- ✅ **DONE**: Confirmed it's missing for MAIN file (Test 4 fails)

#### Step 1.2: Implement applyGroupsOnlyLogic() Method
**Location**: `YamlConfigurationLoader.java`

**Actions**:
1. Add method after line 647 (after `processEnrichmentReferences()`)
2. Follow EXACT pattern from `processEnrichmentReferences()` lines 539-647
3. Handle both enrichments and rules (not just enrichments)
4. Use existing `ProcessingItem` class (already in codebase)

**Implementation**:
```java
private void applyGroupsOnlyLogic(YamlRuleConfiguration config) {
    if (config.getItemOrder() == null) return;

    // Collect enrichment IDs referenced by enrichment-groups
    Set<String> referencedEnrichmentIds = new HashSet<>();
    if (config.getEnrichmentGroups() != null) {
        for (YamlEnrichmentGroup group : config.getEnrichmentGroups()) {
            if (group.getEnrichmentIds() != null) {
                referencedEnrichmentIds.addAll(group.getEnrichmentIds());
            }
        }
    }

    // Collect rule IDs referenced by rule-groups
    Set<String> referencedRuleIds = new HashSet<>();
    if (config.getRuleGroups() != null) {
        for (YamlRuleGroup group : config.getRuleGroups()) {
            if (group.getRuleIds() != null) {
                referencedRuleIds.addAll(group.getRuleIds());
            }
        }
    }

    // Filter itemOrder: Remove enrichments/rules referenced by groups
    List<ProcessingItem> filteredOrder = new ArrayList<>();
    for (ProcessingItem item : config.getItemOrder()) {
        boolean shouldRemove = false;

        if ("enrichments".equals(item.getSectionType()) &&
            referencedEnrichmentIds.contains(item.getItemId())) {
            shouldRemove = true;  // Skip - will execute via group
        } else if ("rules".equals(item.getSectionType()) &&
                   referencedRuleIds.contains(item.getItemId())) {
            shouldRemove = true;  // Skip - will execute via group
        }

        if (!shouldRemove) {
            filteredOrder.add(item);
        }
    }

    config.setItemOrder(filteredOrder);
}
```

#### Step 1.3: Add Method Call
**Location**: `YamlConfigurationLoader.loadConfiguration()` after line 98

**Actions**:
1. Add call to `applyGroupsOnlyLogic(config)` after `config.setItemOrder(itemOrder)`
2. Verify placement is correct (after itemOrder is set, before returning config)

**Implementation**:
```java
config.setItemOrder(itemOrder);
applyGroupsOnlyLogic(config);  // ← ADD THIS LINE
```

#### Step 1.4: Test Incrementally
**Actions**:
1. Run Test 4 ONLY: `mvn test -Dtest=Test4_StandaloneEnrichmentsTest -pl apex-demo`
2. **Read logs carefully** - look for execution order: `[standalone-1, standalone-2, grouped-1, grouped-2]`
3. **Verify count**: Should be 4 executions, not 6
4. If fails, investigate logs line by line (do not guess)

#### Step 1.5: Verify No Regressions
**Actions**:
1. Run Test 1: `mvn test -Dtest=Test1_EnrichmentRefsPositionTest -pl apex-demo`
2. Run Test 2: `mvn test -Dtest=Test2_EnrichmentGroupsOnlyTest -pl apex-demo`
3. Run Test 3: `mvn test -Dtest=Test3_MinimalEnrichmentGroupTest -pl apex-demo`
4. **Read logs for each test** - verify execution order is correct
5. All 4 tests must pass before proceeding

**Success Criteria for Phase 1**:
- ✅ Test 4 passes with 4 executions (not 6)
- ✅ Test 1 still passes (no regression)
- ✅ Test 2 still passes (no regression)
- ✅ Test 3 still passes (no regression)
- ✅ Logs show correct execution order for all tests

### Phase 2: Implement Numbered Suffixes (AFTER Phase 1 Complete)

**Goal**: Support `enrichments-1`, `enrichments-2` syntax to avoid YAML duplicate keys

#### Step 2.1: Investigate Existing Parser Code
**Actions**:
1. Use `codebase-retrieval` to find how `OrderedYamlParser` handles section names
2. Find where section names are normalized (if at all)
3. Find where sections are merged (if at all)
4. **Do not guess** - verify actual implementation

#### Step 2.2: Implement Numbered Suffix Recognition
**Location**: `OrderedYamlParser.java`

**Actions**:
1. Add constant: `private static final Pattern NUMBERED_SUFFIX = Pattern.compile("^(.+)-(\\d+)$");`
2. Add method: `private String normalizeSectionName(String sectionName)`
3. Follow existing patterns in `OrderedYamlParser` (do not reinvent)
4. Test with simple example first

#### Step 2.3: Update Section Merging Logic
**Actions**:
1. Find where sections are collected (likely in `extractItemOrder()`)
2. Modify to merge `enrichments-1`, `enrichments-2` → `enrichments`
3. Preserve document order during merge
4. Test incrementally with logs

#### Step 2.4: Create Test 5 (Numbered Suffixes Basic)
**Actions**:
1. Create `Test5_NumberedSuffixesBasicTest.java`
2. Create `test5-main.yaml` with `enrichments-1`, `enrichments-2`, `enrichments-3`
3. Use `ExecutionTracker` to prove order
4. Run test and read logs carefully
5. Fix issues before proceeding

**Success Criteria for Phase 2**:
- ✅ Test 5 passes with correct execution order
- ✅ All previous tests (1-4) still pass
- ✅ Logs show numbered suffixes are merged correctly

### Phase 3: Numbered Suffixes with Groups-Only Logic (AFTER Phase 2 Complete)

**Goal**: Prove numbered suffixes work with groups-only logic

#### Step 3.1: Create Test 6 (Numbered Suffixes + Groups)
**Actions**:
1. Create `Test6_NumberedSuffixesWithGroupsTest.java`
2. Create `test6-main.yaml` with `enrichments-1`, `enrichment-groups-1`, `enrichments-2`
3. Test that standalone enrichments execute, grouped enrichments skip
4. Run test and read logs carefully

**Success Criteria for Phase 3**:
- ✅ Test 6 passes with correct execution order
- ✅ Standalone enrichments execute directly
- ✅ Grouped enrichments only execute via groups
- ✅ All previous tests (1-5) still pass

### Phase 4: Cross-File Scoping (AFTER Phase 3 Complete)

**Goal**: Prove per-file scoping works correctly

#### Step 4.1: Create Test 7 (Cross-File Scoping)
**Actions**:
1. Create `Test7_CrossFileGroupsScopingTest.java`
2. Create `test7-main.yaml` with inline enrichments + enrichment-refs
3. Create `test7-external.yaml` with enrichment-groups
4. Prove inline enrichments in main file are NOT affected by external groups
5. Run test and read logs carefully

**Success Criteria for Phase 4**:
- ✅ Test 7 passes with correct execution order
- ✅ Inline enrichments in main file execute directly
- ✅ External groups do not affect main file enrichments
- ✅ All previous tests (1-6) still pass

### Phase 5: Final Validation (AFTER Phase 4 Complete)

**Goal**: Prove YAML document order is fully respected

#### Step 5.1: Run All Tests
**Actions**:
1. Run all tests: `mvn test -Dtest="Test4*,Test5*,Test6*,Test7*" -pl apex-demo`
2. **Read logs carefully** for each test
3. Verify execution order is correct for all tests
4. Verify no warnings or errors in logs

**Success Criteria for Phase 5**:
- ✅ All tests pass (Test 4A-4F, Test 5, Test 6A-6B, Test 7A-7B)
- ✅ No duplicate key warnings in logs
- ✅ Execution order is correct for all tests
- ✅ YAML document order is fully respected

### Critical Checkpoints

**After EVERY step**:
1. ✅ Run the relevant test(s)
2. ✅ Read logs line by line
3. ✅ Verify execution order is correct
4. ✅ Verify no new warnings or errors
5. ✅ Do NOT proceed until tests pass

**If ANY test fails**:
1. ❌ STOP immediately
2. 🔍 Investigate logs carefully (do not guess)
3. 🔧 Fix the issue
4. ✅ Re-run test to verify fix
5. ✅ Only then proceed to next step

### Estimated Timeline

- **Phase 1**: 1-2 hours (critical fix)
- **Phase 2**: 2-3 hours (parser changes)
- **Phase 3**: 1 hour (test creation)
- **Phase 4**: 1 hour (test creation)
- **Phase 5**: 1 hour (final validation)

**Total**: 6-8 hours of focused, incremental work

### Key Success Factors

1. **Follow existing patterns** - Mirror `processEnrichmentReferences()` exactly
2. **Test incrementally** - Never make multiple changes without testing
3. **Read logs carefully** - Execution order tells the truth
4. **Do not guess** - Investigate when something fails
5. **Fail honestly** - Let tests fail, fix the problem, don't mask it

---

## 🔬 DEFINITIVE TEST DESIGN (Proves Implementation Works)

### Critical Problem with Previous Tests

**Previous tests were too weak** - they didn't definitively prove the implementation worked because:
1. ❌ Didn't test edge cases (empty groups, missing references)
2. ❌ Didn't test negative cases (what should NOT happen)
3. ❌ Didn't verify exact execution counts
4. ❌ Didn't test all combinations of standalone vs grouped items
5. ❌ Didn't verify filtering logic removes correct items

### Definitive Test Principles

**Each test MUST prove**:
1. ✅ **Exact execution order** - Not just "it works", but "items execute in THIS exact order"
2. ✅ **Exact execution count** - Not just "some items execute", but "EXACTLY N items execute"
3. ✅ **What executes** - Explicitly verify which items DID execute
4. ✅ **What doesn't execute** - Explicitly verify which items DID NOT execute (at their definition position)
5. ✅ **Double execution detection** - Verify items don't execute twice
6. ✅ **Edge cases** - Empty groups, missing references, all standalone, all grouped

---

## Phase 1 Definitive Tests: Groups-Only Logic for Main File

### Test 4A: Basic Groups-Only Logic (EXISTING - ENHANCE)

**Purpose**: Prove grouped enrichments skip their definition position and only execute via group

**YAML Structure**:
```yaml
enrichments:
  - id: "standalone-1"     # Position 1 - NOT in any group
  - id: "grouped-1"        # Position 2 - IN group-A (should skip here)
  - id: "standalone-2"     # Position 3 - NOT in any group
  - id: "grouped-2"        # Position 4 - IN group-A (should skip here)

enrichment-groups:
  - id: "group-A"          # Position 5
    enrichment-ids: ["grouped-1", "grouped-2"]
```

**Definitive Assertions**:
```java
// 1. EXACT execution count
assertEquals(4, executionLog.size(),
    "Should execute EXACTLY 4 items: 2 standalone + 2 via group");

// 2. EXACT execution order
List<String> expected = List.of("standalone-1", "standalone-2", "grouped-1", "grouped-2");
assertEquals(expected, executionLog,
    "Execution order MUST be: standalone-1, standalone-2, grouped-1 (via group), grouped-2 (via group)");

// 3. Verify what executed
assertTrue(executionLog.contains("standalone-1"), "standalone-1 MUST execute at position 1");
assertTrue(executionLog.contains("standalone-2"), "standalone-2 MUST execute at position 3");
assertTrue(executionLog.contains("grouped-1"), "grouped-1 MUST execute via group-A");
assertTrue(executionLog.contains("grouped-2"), "grouped-2 MUST execute via group-A");

// 4. Verify NO double execution
assertEquals(1, Collections.frequency(executionLog, "grouped-1"),
    "grouped-1 MUST execute EXACTLY ONCE (via group only, NOT at position 2)");
assertEquals(1, Collections.frequency(executionLog, "grouped-2"),
    "grouped-2 MUST execute EXACTLY ONCE (via group only, NOT at position 4)");

// 5. Verify execution positions
assertEquals("standalone-1", executionLog.get(0), "Position 0 MUST be standalone-1");
assertEquals("standalone-2", executionLog.get(1), "Position 1 MUST be standalone-2");
assertEquals("grouped-1", executionLog.get(2), "Position 2 MUST be grouped-1 (from group-A)");
assertEquals("grouped-2", executionLog.get(3), "Position 3 MUST be grouped-2 (from group-A)");
```

**What This Proves**:
- ✅ Grouped enrichments skip their definition position (positions 2 and 4)
- ✅ Grouped enrichments execute via group at correct position (position 5)
- ✅ Standalone enrichments execute at their definition position
- ✅ No double execution
- ✅ Document order is preserved

---

### Test 4B: All Standalone (NEW - CRITICAL)

**Purpose**: Prove when NO groups exist, ALL enrichments execute at their definition position

**YAML Structure**:
```yaml
enrichments:
  - id: "standalone-1"     # Position 1
  - id: "standalone-2"     # Position 2
  - id: "standalone-3"     # Position 3
  - id: "standalone-4"     # Position 4

# NO enrichment-groups section
```

**Definitive Assertions**:
```java
// 1. EXACT execution count
assertEquals(4, executionLog.size(),
    "Should execute EXACTLY 4 items: all standalone");

// 2. EXACT execution order
List<String> expected = List.of("standalone-1", "standalone-2", "standalone-3", "standalone-4");
assertEquals(expected, executionLog,
    "Execution order MUST match YAML document order exactly");

// 3. Verify NO filtering occurred
assertEquals(1, Collections.frequency(executionLog, "standalone-1"), "standalone-1 executes once");
assertEquals(1, Collections.frequency(executionLog, "standalone-2"), "standalone-2 executes once");
assertEquals(1, Collections.frequency(executionLog, "standalone-3"), "standalone-3 executes once");
assertEquals(1, Collections.frequency(executionLog, "standalone-4"), "standalone-4 executes once");
```

**What This Proves**:
- ✅ When no groups exist, applyGroupsOnlyLogic() doesn't break anything
- ✅ All enrichments execute at their definition position
- ✅ Document order is preserved
- ✅ No items are incorrectly filtered out

---

### Test 4C: All Grouped (NEW - CRITICAL)

**Purpose**: Prove when ALL enrichments are in groups, NONE execute at definition position

**YAML Structure**:
```yaml
enrichments:
  - id: "grouped-1"        # Position 1 - IN group-A (should skip)
  - id: "grouped-2"        # Position 2 - IN group-A (should skip)
  - id: "grouped-3"        # Position 3 - IN group-B (should skip)
  - id: "grouped-4"        # Position 4 - IN group-B (should skip)

enrichment-groups:
  - id: "group-A"          # Position 5
    enrichment-ids: ["grouped-1", "grouped-2"]
  - id: "group-B"          # Position 6
    enrichment-ids: ["grouped-3", "grouped-4"]
```

**Definitive Assertions**:
```java
// 1. EXACT execution count
assertEquals(4, executionLog.size(),
    "Should execute EXACTLY 4 items: all via groups");

// 2. EXACT execution order
List<String> expected = List.of("grouped-1", "grouped-2", "grouped-3", "grouped-4");
assertEquals(expected, executionLog,
    "Execution order MUST be: group-A items, then group-B items");

// 3. Verify NO execution at definition positions
// If any item executed at definition position, we'd have 8 executions (4 + 4)
assertNotEquals(8, executionLog.size(),
    "MUST NOT have double execution - proves filtering worked");

// 4. Verify each item executes EXACTLY once
assertEquals(1, Collections.frequency(executionLog, "grouped-1"), "grouped-1 executes once via group-A");
assertEquals(1, Collections.frequency(executionLog, "grouped-2"), "grouped-2 executes once via group-A");
assertEquals(1, Collections.frequency(executionLog, "grouped-3"), "grouped-3 executes once via group-B");
assertEquals(1, Collections.frequency(executionLog, "grouped-4"), "grouped-4 executes once via group-B");

// 5. Verify execution positions
assertEquals("grouped-1", executionLog.get(0), "Position 0 MUST be grouped-1 (from group-A)");
assertEquals("grouped-2", executionLog.get(1), "Position 1 MUST be grouped-2 (from group-A)");
assertEquals("grouped-3", executionLog.get(2), "Position 2 MUST be grouped-3 (from group-B)");
assertEquals("grouped-4", executionLog.get(3), "Position 3 MUST be grouped-4 (from group-B)");
```

**What This Proves**:
- ✅ ALL enrichments are correctly filtered from itemOrder
- ✅ NO enrichments execute at their definition position
- ✅ ALL enrichments execute via groups only
- ✅ Multiple groups work correctly
- ✅ No double execution

---

### Test 4D: Empty Group (NEW - EDGE CASE)

**Purpose**: Prove empty groups don't break the system

**YAML Structure**:
```yaml
enrichments:
  - id: "standalone-1"     # Position 1
  - id: "grouped-1"        # Position 2 - IN group-A (should skip)

enrichment-groups:
  - id: "group-A"          # Position 3
    enrichment-ids: ["grouped-1"]
  - id: "group-B"          # Position 4 - EMPTY GROUP
    enrichment-ids: []
```

**Definitive Assertions**:
```java
// 1. EXACT execution count
assertEquals(2, executionLog.size(),
    "Should execute EXACTLY 2 items: 1 standalone + 1 via group-A");

// 2. EXACT execution order
List<String> expected = List.of("standalone-1", "grouped-1");
assertEquals(expected, executionLog,
    "Execution order MUST be: standalone-1, grouped-1 (via group-A)");

// 3. Verify empty group doesn't cause errors
// Test passes = empty group handled gracefully
```

**What This Proves**:
- ✅ Empty groups don't cause exceptions
- ✅ Empty groups don't affect other groups
- ✅ System handles edge case gracefully

---

### Test 4E: Missing Reference (NEW - EDGE CASE)

**Purpose**: Prove groups referencing non-existent enrichments don't break the system

**YAML Structure**:
```yaml
enrichments:
  - id: "standalone-1"     # Position 1
  - id: "grouped-1"        # Position 2 - IN group-A (should skip)

enrichment-groups:
  - id: "group-A"          # Position 3
    enrichment-ids: ["grouped-1", "non-existent"]  # non-existent doesn't exist
```

**Definitive Assertions**:
```java
// 1. EXACT execution count
assertEquals(2, executionLog.size(),
    "Should execute EXACTLY 2 items: 1 standalone + 1 via group-A");

// 2. EXACT execution order
List<String> expected = List.of("standalone-1", "grouped-1");
assertEquals(expected, executionLog,
    "Execution order MUST be: standalone-1, grouped-1 (via group-A)");

// 3. Verify non-existent reference doesn't cause errors
// Test passes = missing reference handled gracefully

// 4. Verify grouped-1 still filtered correctly
assertEquals(1, Collections.frequency(executionLog, "grouped-1"),
    "grouped-1 MUST execute EXACTLY ONCE (via group only)");
```

**What This Proves**:
- ✅ Missing references don't cause exceptions
- ✅ Existing enrichments in group still work correctly
- ✅ Filtering logic is robust

---

### Test 4F: Complex Interleaving (NEW - COMPREHENSIVE)

**Purpose**: Prove complex patterns work correctly

**YAML Structure**:
```yaml
enrichments:
  - id: "standalone-1"     # Position 1 - standalone
  - id: "grouped-1"        # Position 2 - IN group-A (skip)
  - id: "standalone-2"     # Position 3 - standalone
  - id: "grouped-2"        # Position 4 - IN group-B (skip)
  - id: "standalone-3"     # Position 5 - standalone
  - id: "grouped-3"        # Position 6 - IN group-A (skip)

enrichment-groups:
  - id: "group-A"          # Position 7
    enrichment-ids: ["grouped-1", "grouped-3"]
  - id: "group-B"          # Position 8
    enrichment-ids: ["grouped-2"]
```

**Definitive Assertions**:
```java
// 1. EXACT execution count
assertEquals(6, executionLog.size(),
    "Should execute EXACTLY 6 items: 3 standalone + 3 via groups");

// 2. EXACT execution order
List<String> expected = List.of(
    "standalone-1",  // Position 1
    "standalone-2",  // Position 3 (grouped-1 skipped at position 2)
    "standalone-3",  // Position 5 (grouped-2 skipped at position 4)
    "grouped-1",     // Position 7 (via group-A)
    "grouped-3",     // Position 7 (via group-A)
    "grouped-2"      // Position 8 (via group-B)
);
assertEquals(expected, executionLog,
    "Execution order MUST preserve document order with correct filtering");

// 3. Verify NO double execution
assertEquals(1, Collections.frequency(executionLog, "grouped-1"), "grouped-1 executes once");
assertEquals(1, Collections.frequency(executionLog, "grouped-2"), "grouped-2 executes once");
assertEquals(1, Collections.frequency(executionLog, "grouped-3"), "grouped-3 executes once");

// 4. Verify standalone items execute at correct positions
assertEquals("standalone-1", executionLog.get(0), "First execution MUST be standalone-1");
assertEquals("standalone-2", executionLog.get(1), "Second execution MUST be standalone-2");
assertEquals("standalone-3", executionLog.get(2), "Third execution MUST be standalone-3");

// 5. Verify grouped items execute via groups
assertEquals("grouped-1", executionLog.get(3), "Fourth execution MUST be grouped-1 (via group-A)");
assertEquals("grouped-3", executionLog.get(4), "Fifth execution MUST be grouped-3 (via group-A)");
assertEquals("grouped-2", executionLog.get(5), "Sixth execution MUST be grouped-2 (via group-B)");
```

**What This Proves**:
- ✅ Complex interleaving of standalone and grouped items works
- ✅ Multiple groups with different enrichments work
- ✅ Document order is preserved correctly
- ✅ Filtering logic handles complex patterns
- ✅ No double execution in complex scenarios

---

## Phase 1 Test Summary

**6 Definitive Tests** that prove groups-only logic works:

| Test | Purpose | Key Proof |
|------|---------|-----------|
| 4A | Basic groups-only | Grouped items skip definition position, execute via group |
| 4B | All standalone | No groups = no filtering, all execute at definition position |
| 4C | All grouped | All items filtered, none execute at definition position |
| 4D | Empty group | Empty groups don't break system |
| 4E | Missing reference | Missing references don't break system |
| 4F | Complex interleaving | Complex patterns work correctly |

**Success Criteria for Phase 1**:
- ✅ ALL 6 tests pass
- ✅ Each test has EXACT execution count assertions
- ✅ Each test has EXACT execution order assertions
- ✅ Each test verifies NO double execution
- ✅ Each test verifies specific items execute/don't execute
- ✅ Edge cases handled gracefully

**If ANY test fails**:
- ❌ Phase 1 is NOT complete
- 🔍 Investigate which assertion failed
- 🔧 Fix the implementation
- ✅ Re-run ALL 6 tests
- ✅ Only proceed to Phase 2 when ALL 6 tests pass

---

## Phase 2 Definitive Tests: Numbered Suffixes

### Test 5A: Basic Numbered Suffixes (NEW - CRITICAL)

**Purpose**: Prove `enrichments-1`, `enrichments-2`, `enrichments-3` are merged and execute in document order

**YAML Structure**:
```yaml
enrichments-1:
  - id: "e1"               # Position 1

enrichment-refs:
  - source: "test5-external.yaml"  # Contains: ext-1, ext-2

enrichments-2:
  - id: "e2"               # Position 3

enrichments-3:
  - id: "e3"               # Position 4
```

**Definitive Assertions**:
```java
// 1. EXACT execution count
assertEquals(5, executionLog.size(),
    "Should execute EXACTLY 5 items: e1, ext-1, ext-2, e2, e3");

// 2. EXACT execution order
List<String> expected = List.of("e1", "ext-1", "ext-2", "e2", "e3");
assertEquals(expected, executionLog,
    "Execution order MUST be: e1, external items, e2, e3");

// 3. Verify numbered suffixes were merged
// If not merged, only enrichments-3 would execute (YAML duplicate key)
assertTrue(executionLog.contains("e1"), "e1 from enrichments-1 MUST execute");
assertTrue(executionLog.contains("e2"), "e2 from enrichments-2 MUST execute");
assertTrue(executionLog.contains("e3"), "e3 from enrichments-3 MUST execute");

// 4. Verify document order preserved
assertEquals("e1", executionLog.get(0), "Position 0 MUST be e1 (from enrichments-1)");
assertEquals("ext-1", executionLog.get(1), "Position 1 MUST be ext-1 (from enrichment-refs)");
assertEquals("ext-2", executionLog.get(2), "Position 2 MUST be ext-2 (from enrichment-refs)");
assertEquals("e2", executionLog.get(3), "Position 3 MUST be e2 (from enrichments-2)");
assertEquals("e3", executionLog.get(4), "Position 4 MUST be e3 (from enrichments-3)");

// 5. Verify NO duplicate execution
assertEquals(1, Collections.frequency(executionLog, "e1"), "e1 executes once");
assertEquals(1, Collections.frequency(executionLog, "e2"), "e2 executes once");
assertEquals(1, Collections.frequency(executionLog, "e3"), "e3 executes once");
```

**What This Proves**:
- ✅ Numbered suffixes are recognized and merged
- ✅ All numbered sections execute (not just the last one)
- ✅ Document order is preserved across numbered sections
- ✅ Numbered suffixes work with enrichment-refs
- ✅ No duplicate execution

---

### Test 5B: Numbered Suffixes Out of Order (NEW - EDGE CASE)

**Purpose**: Prove numbered suffixes work even if not in sequential order

**YAML Structure**:
```yaml
enrichments-3:
  - id: "e3"               # Position 1 (suffix 3)

enrichments-1:
  - id: "e1"               # Position 2 (suffix 1)

enrichments-2:
  - id: "e2"               # Position 3 (suffix 2)
```

**Definitive Assertions**:
```java
// 1. EXACT execution count
assertEquals(3, executionLog.size(),
    "Should execute EXACTLY 3 items: e3, e1, e2");

// 2. EXACT execution order (DOCUMENT ORDER, not suffix order)
List<String> expected = List.of("e3", "e1", "e2");
assertEquals(expected, executionLog,
    "Execution order MUST follow DOCUMENT ORDER (e3, e1, e2), NOT suffix order");

// 3. Verify all items execute
assertTrue(executionLog.contains("e1"), "e1 from enrichments-1 MUST execute");
assertTrue(executionLog.contains("e2"), "e2 from enrichments-2 MUST execute");
assertTrue(executionLog.contains("e3"), "e3 from enrichments-3 MUST execute");

// 4. Verify document order preserved (NOT sorted by suffix)
assertEquals("e3", executionLog.get(0), "Position 0 MUST be e3 (appears first in YAML)");
assertEquals("e1", executionLog.get(1), "Position 1 MUST be e1 (appears second in YAML)");
assertEquals("e2", executionLog.get(2), "Position 2 MUST be e2 (appears third in YAML)");
```

**What This Proves**:
- ✅ Suffix numbers don't affect execution order
- ✅ Document order is preserved regardless of suffix values
- ✅ Parser doesn't sort by suffix number

---

### Test 5C: Mixed Numbered and Non-Numbered (NEW - EDGE CASE)

**Purpose**: Prove numbered and non-numbered sections can coexist

**YAML Structure**:
```yaml
enrichments:
  - id: "e0"               # Position 1 (no suffix)

enrichments-1:
  - id: "e1"               # Position 2 (suffix 1)

enrichments-2:
  - id: "e2"               # Position 3 (suffix 2)
```

**Definitive Assertions**:
```java
// 1. EXACT execution count
assertEquals(3, executionLog.size(),
    "Should execute EXACTLY 3 items: e0, e1, e2");

// 2. EXACT execution order
List<String> expected = List.of("e0", "e1", "e2");
assertEquals(expected, executionLog,
    "Execution order MUST be: e0, e1, e2");

// 3. Verify all sections execute
assertTrue(executionLog.contains("e0"), "e0 from enrichments MUST execute");
assertTrue(executionLog.contains("e1"), "e1 from enrichments-1 MUST execute");
assertTrue(executionLog.contains("e2"), "e2 from enrichments-2 MUST execute");
```

**What This Proves**:
- ✅ Non-numbered and numbered sections can coexist
- ✅ Both types are merged correctly
- ✅ Document order preserved across both types

---

## Phase 3 Definitive Tests: Numbered Suffixes + Groups-Only Logic

### Test 6A: Numbered Suffixes with Groups (NEW - CRITICAL)

**Purpose**: Prove numbered suffixes work with groups-only logic

**YAML Structure**:
```yaml
enrichments-1:
  - id: "standalone-1"     # Position 1 - NOT in group
  - id: "grouped-1"        # Position 2 - IN group-1 (should skip)

enrichment-groups-1:
  - id: "group-1"          # Position 3
    enrichment-ids: ["grouped-1"]

enrichments-2:
  - id: "standalone-2"     # Position 4 - NOT in group
  - id: "grouped-2"        # Position 5 - IN group-2 (should skip)

enrichment-groups-2:
  - id: "group-2"          # Position 6
    enrichment-ids: ["grouped-2"]
```

**Definitive Assertions**:
```java
// 1. EXACT execution count
assertEquals(4, executionLog.size(),
    "Should execute EXACTLY 4 items: 2 standalone + 2 via groups");

// 2. EXACT execution order
List<String> expected = List.of("standalone-1", "standalone-2", "grouped-1", "grouped-2");
assertEquals(expected, executionLog,
    "Execution order MUST be: standalone-1, standalone-2, grouped-1 (via group-1), grouped-2 (via group-2)");

// 3. Verify NO double execution
assertEquals(1, Collections.frequency(executionLog, "grouped-1"),
    "grouped-1 MUST execute EXACTLY ONCE (via group-1 only, NOT at position 2)");
assertEquals(1, Collections.frequency(executionLog, "grouped-2"),
    "grouped-2 MUST execute EXACTLY ONCE (via group-2 only, NOT at position 5)");

// 4. Verify standalone items execute at correct positions
assertEquals("standalone-1", executionLog.get(0), "Position 0 MUST be standalone-1");
assertEquals("standalone-2", executionLog.get(1), "Position 1 MUST be standalone-2");

// 5. Verify grouped items execute via groups
assertEquals("grouped-1", executionLog.get(2), "Position 2 MUST be grouped-1 (via group-1)");
assertEquals("grouped-2", executionLog.get(3), "Position 3 MUST be grouped-2 (via group-2)");
```

**What This Proves**:
- ✅ Numbered suffixes work with groups-only logic
- ✅ Groups-only logic applies AFTER numbered sections are merged
- ✅ Standalone enrichments in numbered sections execute directly
- ✅ Grouped enrichments in numbered sections skip definition position
- ✅ No double execution

---

### Test 6B: Cross-Section Group References (NEW - COMPREHENSIVE)

**Purpose**: Prove groups can reference enrichments from different numbered sections

**YAML Structure**:
```yaml
enrichments-1:
  - id: "grouped-1"        # Position 1 - IN group-A (should skip)
  - id: "standalone-1"     # Position 2 - NOT in group

enrichments-2:
  - id: "grouped-2"        # Position 3 - IN group-A (should skip)
  - id: "standalone-2"     # Position 4 - NOT in group

enrichment-groups-1:
  - id: "group-A"          # Position 5
    enrichment-ids: ["grouped-1", "grouped-2"]  # References from BOTH enrichments-1 and enrichments-2
```

**Definitive Assertions**:
```java
// 1. EXACT execution count
assertEquals(4, executionLog.size(),
    "Should execute EXACTLY 4 items: 2 standalone + 2 via group-A");

// 2. EXACT execution order
List<String> expected = List.of("standalone-1", "standalone-2", "grouped-1", "grouped-2");
assertEquals(expected, executionLog,
    "Execution order MUST be: standalone-1, standalone-2, grouped-1 (via group-A), grouped-2 (via group-A)");

// 3. Verify NO double execution
assertEquals(1, Collections.frequency(executionLog, "grouped-1"),
    "grouped-1 from enrichments-1 MUST execute EXACTLY ONCE (via group-A only)");
assertEquals(1, Collections.frequency(executionLog, "grouped-2"),
    "grouped-2 from enrichments-2 MUST execute EXACTLY ONCE (via group-A only)");

// 4. Verify cross-section filtering works
// grouped-1 from enrichments-1 should be filtered
// grouped-2 from enrichments-2 should be filtered
// Both should only execute via group-A
```

**What This Proves**:
- ✅ Groups can reference enrichments from different numbered sections
- ✅ Filtering works across all merged enrichments
- ✅ Groups-only logic is applied globally after merge
- ✅ No double execution across sections

---

## Phase 4 Definitive Tests: Cross-File Scoping

### Test 7A: Per-File Scoping (NEW - CRITICAL)

**Purpose**: Prove inline enrichments in main file are NOT affected by groups in external file

**YAML Structure (main file)**:
```yaml
enrichments-1:
  - id: "inline-before"    # Position 1 - NOT in external groups

enrichment-refs:
  - source: "test7-external.yaml"  # Has enrichment-groups

enrichments-2:
  - id: "inline-after"     # Position 3 - NOT in external groups
```

**YAML Structure (external file)**:
```yaml
enrichments:
  - id: "ext-grouped-1"    # IN group-A (should skip)
  - id: "ext-standalone"   # NOT in group

enrichment-groups:
  - id: "group-A"
    enrichment-ids: ["ext-grouped-1"]
```

**Definitive Assertions**:
```java
// 1. EXACT execution count
assertEquals(4, executionLog.size(),
    "Should execute EXACTLY 4 items: inline-before, ext-standalone, ext-grouped-1 (via group), inline-after");

// 2. EXACT execution order
List<String> expected = List.of("inline-before", "ext-standalone", "ext-grouped-1", "inline-after");
assertEquals(expected, executionLog,
    "Execution order MUST be: inline-before, external items, inline-after");

// 3. Verify inline enrichments execute (NOT affected by external groups)
assertTrue(executionLog.contains("inline-before"),
    "inline-before MUST execute (NOT affected by external group-A)");
assertTrue(executionLog.contains("inline-after"),
    "inline-after MUST execute (NOT affected by external group-A)");

// 4. Verify external groups-only logic works
assertEquals(1, Collections.frequency(executionLog, "ext-grouped-1"),
    "ext-grouped-1 MUST execute EXACTLY ONCE (via group-A only)");
assertTrue(executionLog.contains("ext-standalone"),
    "ext-standalone MUST execute (not in any group)");

// 5. Verify per-file scoping
// inline-before and inline-after are in MAIN file
// group-A is in EXTERNAL file
// group-A should NOT affect inline-before or inline-after
```

**What This Proves**:
- ✅ Groups-only logic applies per file
- ✅ Inline enrichments in main file are NOT affected by external groups
- ✅ External groups-only logic still works for external file
- ✅ Per-file scoping is correctly implemented

---

## Complete Test Matrix

### Phase 1: Groups-Only Logic (6 tests)
- ✅ Test 4A: Basic groups-only
- ✅ Test 4B: All standalone
- ✅ Test 4C: All grouped
- ✅ Test 4D: Empty group
- ✅ Test 4E: Missing reference
- ✅ Test 4F: Complex interleaving

### Phase 2: Numbered Suffixes (1 test)
- ✅ Test 5: Basic numbered suffixes

### Phase 3: Numbered Suffixes + Enrichment Groups (2 tests)
- ✅ Test 6A: Numbered suffixes with groups
- ✅ Test 6B: Complex numbered with multiple groups

### Phase 4: Rule Groups (2 tests)
- ✅ Test 7A: Rule groups basic
- ✅ Test 7B: Numbered suffixes with rule groups

**Total: 11 Test Classes, 22 Test Methods**

### Success Criteria for Complete Implementation

**ALL tests MUST pass with**:
- ✅ EXACT execution count assertions
- ✅ EXACT execution order assertions
- ✅ NO double execution assertions
- ✅ Specific item execution verification
- ✅ Edge case handling verification

**If ANY test fails**:
- ❌ Implementation is NOT complete
- 🔍 Investigate which assertion failed
- 🔧 Fix the implementation
- ✅ Re-run ALL tests in that phase
- ✅ Re-run ALL previous phase tests (regression check)
- ✅ Only proceed when ALL tests pass

