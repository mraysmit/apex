# Enrichment-Groups Implementation Plan for RulesEngine

## Executive Summary

This document provides a thoroughly researched implementation plan to add enrichment-groups processing support to `RulesEngine.evaluate()`, enabling migration of 7 test files from the deprecated `YamlEnrichmentProcessor.processEnrichmentGroup()` API to the universal `RulesEngine` pattern.

**Goal**: Achieve 93% compliance (91 out of 99 files) by implementing enrichment-groups support in apex-core.

---

## 🎯 Core Architectural Principle: Universal Single Entry Point

### The Fundamental Rule

**The APEX rules engine MUST have ONE and ONLY ONE public interface for processing ANY type of APEX YAML configuration:**

```java
RulesEngine.evaluate(yamlConfig, data)
```

### What This Means

**NO Type-Specific Public Methods**
- NO `processEnrichments()`
- NO `processEnrichmentGroup()`
- NO `processRuleGroup()`
- NO `processScenario()`
- NO `processLookup()`

**YES Universal Entry Point**
- **ONLY** `RulesEngine.evaluate()`
- Content-agnostic processing
- Automatic detection of YAML type
- Complete and reliable processing of ALL sections

### Implementation Implications

1. **No Dependencies on Deprecated Services**
   - `EnrichmentGroup` model should NOT have `evaluate(processor, config)` method
   - Models are data structures, NOT execution engines
   - Execution logic belongs in `RulesEngine`, NOT in models

2. **Internal Processing Only**
   - `RulesEngine` has `enrichmentProcessor` as a private field
   - All enrichment processing happens internally
   - No need to pass services between methods

3. **Mirrors Rule-Groups Pattern**
   - `RuleGroup.evaluate(context)` takes only a context, NOT a service
   - `RulesEngine.executeRuleGroupsList()` is PRIVATE, NOT public
   - Same pattern applies to enrichment-groups

### User Experience

A user should be able to take **ANY** APEX YAML file and simply call:
```java
RulesEngine engine = RulesEngine.fromFile("any-apex-config.yaml");
RuleResult result = engine.evaluate(config, data);
```

And it should **just work** - regardless of whether the YAML contains:
- Individual enrichments
- Enrichment-groups
- Individual rules
- Rule-groups
- Scenarios
- Or any combination thereof

---

## Research Findings

### 1. Current State Analysis

#### Rule-Groups Implementation (COMPLETE ✅)
Rule-groups are fully implemented in `RulesEngine` with the following architecture:

**Data Flow**:
```
YAML File → YamlRuleConfiguration → YamlRuleFactory → RulesEngineConfiguration → RulesEngine
```

**Key Components**:
1. **Model**: `RuleGroup` (apex-core/src/main/java/dev/mars/apex/core/engine/model/RuleGroup.java)
   - Stores rules by sequence
   - Implements `evaluate(context)` and `evaluateWithDetails(context)` methods
   - Supports AND/OR logic, stop-on-first-failure, parallel execution
   - Handles severity aggregation

2. **YAML Mapping**: `YamlRuleGroup` (apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleGroup.java)
   - Deserializes `rule-groups` section from YAML
   - Contains: id, name, operator, rule-ids, rule-group-references, etc.

3. **Factory**: `YamlRuleFactory.createRuleGroups()` (apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleFactory.java)
   - Two-phase creation:
     - **Phase 1**: Create all rule groups without references
     - **Phase 2**: Process rule-group-references after all groups exist
   - Applies category metadata inheritance
   - Registers groups in `RulesEngineConfiguration`

4. **Storage**: `RulesEngineConfiguration`
   - Stores rule groups in `Map<String, RuleGroup> ruleGroupsById`
   - Provides `getAllRuleGroups()` method

5. **Execution**: `RulesEngine.executeRuleGroupsList()`
   - Called from `evaluateInStandardOrder()` (line 599-608)
   - Called from `evaluateInDocumentOrder()` (line 692-703)
   - Iterates through groups, calls `group.evaluateWithDetails(context)`
   - Returns first matching group or aggregates failures

#### Enrichment-Groups Implementation (PARTIAL ⚠️)
Enrichment-groups are **partially implemented** but NOT integrated into `RulesEngine`:

**Existing Components**:
1. **Model**: `EnrichmentGroup` (apex-core/src/main/java/dev/mars/apex/core/engine/model/EnrichmentGroup.java)
   - Similar structure to `RuleGroup`
   - Stores enrichments by sequence
   - Supports AND/OR logic, stop-on-first-failure, parallel execution
   - **MISSING**: No `evaluate()` method - execution logic is in `YamlEnrichmentProcessor`

2. **YAML Mapping**: `YamlEnrichmentGroup` (apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlEnrichmentGroup.java)
   - Deserializes `enrichment-groups` section from YAML
   - Contains: id, name, operator, enrichment-ids, enrichment-group-references, etc.

3. **Factory**: `EnrichmentGroupFactory.buildEnrichmentGroups()` (apex-core/src/main/java/dev/mars/apex/core/service/enrichment/EnrichmentGroupFactory.java)
   - Two-phase creation (same pattern as rule groups)
   - Applies category metadata inheritance
   - **PROBLEM**: Returns `List<EnrichmentGroup>` but doesn't register in `RulesEngineConfiguration`

4. **Storage**: `RulesEngineConfiguration` ❌
   - **MISSING**: No storage for enrichment groups
   - No `enrichmentGroupsById` map
   - No `getAllEnrichmentGroups()` method

5. **Execution**: `YamlEnrichmentProcessor.processEnrichmentGroup()` ⚠️ (DEPRECATED)
   - Lines 1523-1622: Implements AND/OR logic, short-circuiting, parallel execution
   - Returns `EnrichmentGroupResult`
   - **PROBLEM**: This is the deprecated API we're trying to replace

6. **RulesEngine Integration**: NOT IMPLEMENTED
   - Line 705-708 in `RulesEngine.java`: TODO comment
   - No `executeEnrichmentGroupsList()` method
   - No call to enrichment-groups processing

### 2. Gap Analysis

| Component | Rule-Groups | Enrichment-Groups | Gap |
|-----------|-------------|-------------------|-----|
| Model with evaluate() | RuleGroup | EnrichmentGroup (no evaluate) | Need to add evaluate() |
| YAML Mapping | YamlRuleGroup | YamlEnrichmentGroup | None |
| Factory | YamlRuleFactory | EnrichmentGroupFactory | Need integration |
| Storage in Config | ruleGroupsById | Missing | Need to add |
| RulesEngine execution | executeRuleGroupsList() | Missing | Need to add |
| Document order processing | case "rule-groups" | TODO | Need to implement |

---

## Implementation Plan

### Phase 1: Add Storage to RulesEngineConfiguration

**File**: `apex-core/src/main/java/dev/mars/apex/core/engine/config/RulesEngineConfiguration.java`

**Changes**:
1. Add field: `private final Map<String, EnrichmentGroup> enrichmentGroupsById = new HashMap<>();`
2. Add method: `public void registerEnrichmentGroup(EnrichmentGroup group)`
3. Add method: `public EnrichmentGroup getEnrichmentGroupById(String id)`
4. Add method: `public List<EnrichmentGroup> getAllEnrichmentGroups()`

**Code Example**:
```java
// Add after line 53 (after ruleGroupsById declaration)
private final Map<String, EnrichmentGroup> enrichmentGroupsById = new HashMap<>();

// Add after getAllRuleGroups() method (around line 383)
/**
 * Register an enrichment group.
 * 
 * @param group The enrichment group to register
 */
public void registerEnrichmentGroup(EnrichmentGroup group) {
    enrichmentGroupsById.put(group.getId(), group);
    LOGGER.fine("Registered enrichment group: " + group.getId());
}

/**
 * Get an enrichment group by ID.
 * 
 * @param id The enrichment group ID
 * @return The enrichment group, or null if not found
 */
public EnrichmentGroup getEnrichmentGroupById(String id) {
    return enrichmentGroupsById.get(id);
}

/**
 * Get all registered enrichment groups.
 * 
 * @return A list of all registered enrichment groups
 */
public List<EnrichmentGroup> getAllEnrichmentGroups() {
    return new ArrayList<>(enrichmentGroupsById.values());
}
```

**Estimated Effort**: 30 minutes  
**Risk**: LOW - Simple data structure addition

---

### Phase 2: Integrate EnrichmentGroupFactory into YamlRuleFactory

**File**: `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleFactory.java`

**Changes**:
1. Call `EnrichmentGroupFactory.buildEnrichmentGroups()` in `createRulesEngineConfiguration()`
2. Register enrichment groups in `RulesEngineConfiguration`

**Code Example**:
```java
// Add after line 154 (after processing rule group references)
// Phase 3: Create and register enrichment groups
if (yamlConfig.getEnrichmentGroups() != null && !yamlConfig.getEnrichmentGroups().isEmpty()) {
    LOGGER.info("Creating enrichment groups from YAML configuration");
    List<EnrichmentGroup> enrichmentGroups = EnrichmentGroupFactory.buildEnrichmentGroups(yamlConfig);
    
    for (EnrichmentGroup group : enrichmentGroups) {
        config.registerEnrichmentGroup(group);
        LOGGER.info("Registered enrichment group: " + group.getId());
    }
}

LOGGER.info("Successfully created RulesEngineConfiguration with " +
           config.getAllRules().size() + " rules, " +
           config.getAllRuleGroups().size() + " rule groups, and " +
           config.getAllEnrichmentGroups().size() + " enrichment groups");
```

**Estimated Effort**: 20 minutes  
**Risk**: LOW - Following existing pattern

---

### Phase 3: Move Enrichment-Group Logic to RulesEngine

**CRITICAL PRINCIPLE**: EnrichmentGroup should NOT have dependencies on YamlEnrichmentProcessor. All execution logic stays in `RulesEngine`, which already has `enrichmentProcessor` as a field.

**File**: `apex-core/src/main/java/dev/mars/apex/core/engine/config/RulesEngine.java`

**Changes**:
1. Add `executeEnrichmentGroupsList()` method that processes enrichment groups
2. Move logic from `YamlEnrichmentProcessor.processEnrichmentGroup()` into this method
3. Use the existing `enrichmentProcessor` field to process individual enrichments

**Why This Approach**:
- Follows the universal entry point principle - NO public type-specific methods
- Mirrors how individual enrichments are processed (lines 544-584)
- `RulesEngine` already has `enrichmentProcessor` field (line 119)
- `EnrichmentGroup` remains a simple data model (like `RuleGroup`)
- No need to pass deprecated services around

**Code Example**:
```java
// Add after executeRuleGroupsList() method (around line 335)
/**
 * Execute a list of EnrichmentGroup objects against the provided target object.
 * This method processes enrichment groups with AND/OR logic, short-circuiting, and parallel execution.
 *
 * @param enrichmentGroups The list of EnrichmentGroup objects to execute
 * @param targetObject The object to enrich (can be Map or POJO)
 * @return RuleResult with enrichment results
 */
private RuleResult executeEnrichmentGroupsList(List<EnrichmentGroup> enrichmentGroups,
                                               Object targetObject) {
    if (enrichmentGroups == null || enrichmentGroups.isEmpty()) {
        logger.info("No enrichment groups provided for execution");
        return RuleResult.noRules();
    }

    logger.info("Executing {} enrichment groups", enrichmentGroups.size());

    List<String> failureMessages = new ArrayList<>();
    boolean overallSuccess = true;

    for (EnrichmentGroup group : enrichmentGroups) {
        logger.debug("Evaluating enrichment group: {}", group.getName());
        try {
            // Process enrichment group using the same logic as YamlEnrichmentProcessor.processEnrichmentGroup()
            EnrichmentGroupResult result = processEnrichmentGroup(group, targetObject);

            if (!result.isSuccess()) {
                overallSuccess = false;
                failureMessages.add("Enrichment group '" + group.getId() + "' failed: " + result.getMessage());
            }

            logger.debug("Enrichment group '{}' evaluated to: {} with severity: {}",
                       group.getName(), result.isSuccess(), result.getAggregatedSeverity());
        } catch (Exception e) {
            logger.error("Enrichment group '{}' failed with exception: {}", group.getName(), e.getMessage());
            overallSuccess = false;
            failureMessages.add("Enrichment group '" + group.getId() + "' exception: " + e.getMessage());
        }
    }

    Map<String, Object> enrichedData = convertToMap(targetObject);

    if (overallSuccess) {
        return RuleResult.enrichmentSuccess(enrichedData, "enrichment-groups", "All enrichment groups succeeded");
    } else {
        return RuleResult.enrichmentFailure(failureMessages, enrichedData, SeverityConstants.ERROR);
    }
}

/**
 * Process a single enrichment group.
 * This is the internal implementation that mirrors YamlEnrichmentProcessor.processEnrichmentGroup().
 *
 * @param group The enrichment group to process
 * @param targetObject The object to enrich
 * @return EnrichmentGroupResult with detailed execution information
 */
private EnrichmentGroupResult processEnrichmentGroup(EnrichmentGroup group, Object targetObject) {
    long start = System.currentTimeMillis();
    List<RuleResult> results = new ArrayList<>();

    List<YamlEnrichment> ordered = group.getEnrichmentsInOrder();
    if (ordered.isEmpty()) {
        return EnrichmentGroupResult.of(group.getId(), true, "No enrichments", List.of(), 0L);
    }

    boolean andOp = group.isAndOperator();
    boolean shortCircuit = group.isStopOnFirstFailure();

    // Parallel execution branch
    if (group.isParallelExecution() && ordered.size() > 1) {
        return processEnrichmentGroupParallel(group, targetObject, ordered, andOp);
    }

    // Sequential execution branch
    return processEnrichmentGroupSequential(group, targetObject, ordered, andOp, shortCircuit, start);
}

private EnrichmentGroupResult processEnrichmentGroupSequential(EnrichmentGroup group,
                                                               Object targetObject,
                                                               List<YamlEnrichment> enrichments,
                                                               boolean andOp,
                                                               boolean shortCircuit,
                                                               long start) {
    List<RuleResult> results = new ArrayList<>();
    boolean overall = andOp; // AND starts true, OR starts false

    for (YamlEnrichment enrichment : enrichments) {
        // Use the existing enrichmentProcessor field to process individual enrichments
        RuleResult r = enrichmentProcessor.processEnrichmentWithResult(enrichment, targetObject);
        results.add(r);
        boolean ok = r.isSuccess();

        if (andOp) {
            if (!ok) {
                overall = false;
                if (shortCircuit) break;
            }
        } else { // OR
            if (ok) {
                overall = true;
                if (shortCircuit) break;
            }
        }
    }

    long elapsed = System.currentTimeMillis() - start;
    String message = overall ? "Enrichment group succeeded" : "Enrichment group failed";
    return EnrichmentGroupResult.of(group.getId(), overall, message, results, elapsed);
}

private EnrichmentGroupResult processEnrichmentGroupParallel(EnrichmentGroup group,
                                                             Object targetObject,
                                                             List<YamlEnrichment> enrichments,
                                                             boolean andOp) {
    // Implementation similar to YamlEnrichmentProcessor lines 1534-1595
    // Uses ExecutorService for parallel execution
    List<RuleResult> results = new ArrayList<>();

    List<Callable<RuleResult>> tasks = new ArrayList<>();
    for (YamlEnrichment enrichment : enrichments) {
        tasks.add(() -> {
            try {
                return enrichmentProcessor.processEnrichmentWithResult(enrichment, targetObject);
            } catch (Exception e) {
                List<String> msgs = new ArrayList<>();
                msgs.add("Parallel enrichment exception: " + e.getMessage());
                Map<String, Object> data = convertToMap(targetObject);
                return RuleResult.enrichmentFailure(msgs, data, SeverityConstants.ERROR);
            }
        });
    }

    ExecutorService executor = Executors.newFixedThreadPool(
        Math.min(tasks.size(), Runtime.getRuntime().availableProcessors())
    );
    try {
        List<Future<RuleResult>> futures = executor.invokeAll(tasks);
        for (Future<RuleResult> f : futures) {
            try {
                results.add(f.get());
            } catch (Exception e) {
                List<String> msgs = new ArrayList<>();
                msgs.add("Error getting parallel enrichment result: " + e.getMessage());
                Map<String, Object> data = convertToMap(targetObject);
                results.add(RuleResult.enrichmentFailure(msgs, data, SeverityConstants.ERROR));
            }
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return EnrichmentGroupResult.of(group.getId(), false, "Parallel execution interrupted", results, 0L);
    } finally {
        executor.shutdown();
    }

    // Evaluate overall success based on AND/OR logic
    boolean overall = andOp; // AND starts true, OR starts false
    for (RuleResult r : results) {
        boolean ok = r.isSuccess();
        if (andOp) {
            if (!ok) overall = false;
        } else { // OR
            if (ok) {
                overall = true;
                break;
            }
        }
    }

    long elapsed = System.currentTimeMillis() - System.currentTimeMillis();
    String message = overall ? "Enrichment group succeeded (parallel)" : "Enrichment group failed (parallel)";
    return EnrichmentGroupResult.of(group.getId(), overall, message, results, elapsed);
}
```

**Estimated Effort**: 3 hours
**Risk**: MEDIUM - Need to carefully migrate logic from YamlEnrichmentProcessor, but approach is sound

---

### Phase 4: Integrate Enrichment-Groups into Standard Order Processing

**File**: `apex-core/src/main/java/dev/mars/apex/core/engine/config/RulesEngine.java`

**Changes**:
Add enrichment-groups processing to `evaluateInStandardOrder()` method

**Code Example**:
```java
// Add after Phase 2 (individual rules processing) around line 596
// Phase 2.5: Process enrichment groups if available
List<EnrichmentGroup> allEnrichmentGroups = configuration.getAllEnrichmentGroups();
if (allEnrichmentGroups != null && !allEnrichmentGroups.isEmpty()) {
    logger.info("Processing {} enrichment groups", allEnrichmentGroups.size());
    RuleResult enrichmentGroupResult = executeEnrichmentGroupsList(allEnrichmentGroups, enrichedData);

    if (enrichmentGroupResult.getResultType() == RuleResult.ResultType.ERROR) {
        overallSuccess = false;
        failureMessages.add("Enrichment group evaluation error: " +
                          enrichmentGroupResult.getMessage());
    }

    // Update enrichedData with results
    if (enrichmentGroupResult.getEnrichedData() != null) {
        enrichedData.putAll(enrichmentGroupResult.getEnrichedData());
    }
}
```

**Estimated Effort**: 30 minutes
**Risk**: LOW - Simple integration following existing pattern

---

### Phase 5: Implement enrichment-groups Case in Document Order Processing

**File**: `apex-core/src/main/java/dev/mars/apex/core/engine/config/RulesEngine.java`

**Changes**:
Replace TODO at lines 705-708 with actual implementation

**Code Example**:
```java
// Replace lines 705-708
case "enrichment-groups":
    List<EnrichmentGroup> allEnrichmentGroups = configuration.getAllEnrichmentGroups();
    if (allEnrichmentGroups != null && !allEnrichmentGroups.isEmpty()) {
        logger.info("Processing {} enrichment groups", allEnrichmentGroups.size());
        RuleResult enrichmentGroupResult = executeEnrichmentGroupsList(
            allEnrichmentGroups, enrichedData, yamlConfig);
        
        if (enrichmentGroupResult.getResultType() == RuleResult.ResultType.ERROR) {
            overallSuccess = false;
            failureMessages.add("Enrichment group evaluation error: " + 
                              enrichmentGroupResult.getMessage());
        }
        
        // Update enrichedData with results
        if (enrichmentGroupResult.getEnrichedData() != null) {
            enrichedData.putAll(enrichmentGroupResult.getEnrichedData());
        }
    }
    break;
```

**Estimated Effort**: 30 minutes  
**Risk**: LOW - Simple case statement implementation

---

## Testing Strategy

### Unit Tests

1. **EnrichmentGroup.evaluate() Tests**
   - Test AND logic with all success
   - Test AND logic with one failure (stop-on-first-failure)
   - Test OR logic with first success (short-circuit)
   - Test OR logic with all failures
   - Test parallel execution (no short-circuit)
   - Test enrichment-group-references

2. **RulesEngine.executeEnrichmentGroupsList() Tests**
   - Test single enrichment group
   - Test multiple enrichment groups
   - Test empty list
   - Test exception handling

3. **Integration Tests**
   - Test enrichment-groups in document order
   - Test enrichment-groups in standard order
   - Test enrichment-groups with enrichments and rules

### Migration Tests

Migrate existing 7 test files:
1. `BasicYamlEnrichmentGroupProcessingTest.java`
2. `EnrichmentGroupSeverityAggregationTest.java`
3. `EnrichmentRefsFeatureTest.java`
4. `MultiFileYamlEnrichmentGroupProcessingTest.java`
5. `SimpleInlineEnrichmentGroupTest.java`
6. `StopOnFirstFailureAndEnrichmentGroupTest.java`
7. `StopOnFirstFailureOrEnrichmentGroupTest.java`

**Migration Pattern**:
```java
// OLD (DEPRECATED)
EnrichmentGroup group = groups.stream()
    .filter(g -> g.getId().equals("base_and"))
    .findFirst().orElse(null);
EnrichmentGroupResult result = enrichmentProcessor.processEnrichmentGroup(group, data, config);

// NEW (RECOMMENDED)
RulesEngine engine = RulesEngine.fromFile(yamlPath);
RuleResult result = engine.evaluate(config, data);
// Enrichment groups are processed automatically as part of evaluate()
```

---

## Implementation Timeline

| Phase | Task | Effort | Dependencies |
|-------|------|--------|--------------|
| 1 | Add storage to RulesEngineConfiguration | 30 min | None |
| 2 | Integrate EnrichmentGroupFactory | 20 min | Phase 1 |
| 3 | Add executeEnrichmentGroupsList() to RulesEngine | 3 hours | Phase 1, 2 |
| 4 | Integrate into standard order processing | 30 min | Phase 3 |
| 5 | Implement document order case | 30 min | Phase 3 |
| 6 | Write unit tests | 2 hours | Phase 3, 4, 5 |
| 7 | Migrate 7 test files | 2 hours | Phase 6 |
| 8 | Clean up DemoTestBase | 15 min | Phase 7 |
| **TOTAL** | **9 hours** | | |

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Breaking existing enrichment processing | LOW | HIGH | Comprehensive unit tests, backward compatibility |
| Performance degradation | LOW | MEDIUM | Benchmark before/after, optimize if needed |
| Parallel execution issues | MEDIUM | MEDIUM | Thorough testing of parallel branch |
| Test migration complexity | LOW | LOW | Follow existing patterns, incremental migration |

---

## Success Criteria

1. All 693 existing tests pass
2. 7 enrichmentgroups tests migrated to use `RulesEngine.evaluate()`
3. DemoTestBase cleaned up (remove deprecated enrichmentProcessor field)
4. Zero deprecation warnings from enrichment-groups tests
5. 93% compliance achieved (91 out of 99 files)
6. No performance regression (< 5% overhead)

---

## Next Steps

1. **Review and approve this plan** with stakeholders
2. **Create feature branch**: `feature/enrichment-groups-rulesengine`
3. **Implement phases 1-5** in apex-core
4. **Write comprehensive tests** (phase 6)
5. **Migrate test files** (phase 7)
6. **Update documentation** and compliance summary
7. **Merge to main** after all tests pass

---

## References

- **Rule-Groups Implementation**: `apex-core/src/main/java/dev/mars/apex/core/engine/config/RulesEngine.java` (lines 281-335, 599-608, 692-703)
- **EnrichmentGroupFactory**: `apex-core/src/main/java/dev/mars/apex/core/service/enrichment/EnrichmentGroupFactory.java`
- **YamlEnrichmentProcessor**: `apex-core/src/main/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessor.java` (lines 1523-1622)
- **Compliance Summary**: `docs/apex-refactoring/APEX_TEST_COMPLIANCE_SUMMARY.md`

