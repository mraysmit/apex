# APEX Rule Processing Optimisations — February 2026

**Status:** 🟡 ANALYSIS COMPLETE — IMPLEMENTATION NOT STARTED  
**Branch:** `refactor/rules-engine-decomposition`  
**Last Updated:** February 26, 2026  
**Test Baseline:** apex-core: 2,873 tests, apex-demo: 908 tests — 0 failures, 0 errors  
**Predecessor:** `APEX_RULE_PROCESSING_OPTIMISATIONS_JAN_2026_COMPLETE.md` (6 phases, all complete)

## Overview

Following the completion of all 6 phases from the January 2026 optimisation effort, a deep analysis of the post-refactored codebase across 11 classes (~6,012 lines) reveals further opportunities for efficiency improvements. These range from O(n×m) performance bugs in hot paths to ~150 lines of verified dead code and structural hygiene improvements.

## Pipeline Classes (current state post-Jan 2026 refactoring)

| Class | Lines | Package | Responsibility |
|---|---|---|---|
| `RulesEngine` | 961 | `engine.core` | Public API, factory methods, delegation, sequential orchestration |
| `UnifiedRuleEvaluator` | 971 | `engine.core` | Canonical SpEL evaluation, error recovery, message templating, router expressions |
| `ExpressionEvaluatorService` | 227 | `engine.core` | Low-level SpEL parsing, context creation, used by REST API |
| `SequentialProcessor` | 581 | `engine.execution` | Document-order orchestration, item/section routing |
| `RuleGroup` | 458 | `engine.model` | Group AND/OR logic (evaluation delegated to `RuleGroupEvaluationService`) |
| `RuleResult` | 781 | `engine.model` | Result object — 1 private constructor (Builder), 25 factory methods |
| `RuleGroupExecutor` | 250 | `engine.execution` | Thin wrapper dispatching to RuleGroupEvaluationService/UnifiedRuleEvaluator |
| `RuleChainExecutor` | 357 | `engine.execution` | Chain pattern routing via `UnifiedRuleEvaluator` |
| `EnrichmentGroupExecutor` | 268 | `engine.execution` | Enrichment group logic (parallel/sequential) |
| `YamlRuleFactory` | 1,128 | `core.config` | Rule/group construction from YAML configuration |
| `EnrichmentGroupFactory` | 277 | `core.service.enrichment` | Enrichment group construction from YAML configuration |

---

## Problem 1: EnrichmentGroupFactory Rebuilds Per Lookup (×2 locations)

**Severity: Critical — Performance**

Both `SequentialProcessor.processEnrichmentGroupItem()` (line 462) and `RuleChainExecutor.findEnrichmentGroup()` (line 326) call `EnrichmentGroupFactory.buildEnrichmentGroups(yamlConfig)` **inside a per-item loop**. This rebuilds the entire enrichment group hierarchy from YAML on every single lookup — O(n×m) where n = number of items in document order and m = number of enrichment groups.

This is the same class of bug that was fixed for `YamlRuleFactory` in Phase 4 (Jan 2026), but the fix was not applied to `EnrichmentGroupFactory`.

**SequentialProcessor (line 462):**
```java
// Called once per enrichment-group item in document-order processing
for (YamlEnrichmentGroup yamlGroup : yamlConfig.getEnrichmentGroups()) {
    if (groupId.equals(yamlGroup.getId())) {
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(yamlConfig);  // ← FULL REBUILD
        for (EnrichmentGroup g : groups) {
            if (groupId.equals(g.getId())) {
                group = g;
                break;
            }
        }
        break;
    }
}
```

**RuleChainExecutor (line 326):**
```java
// Called once per enrichment group in chain routing
for (YamlEnrichmentGroup yamlGroup : yamlConfig.getEnrichmentGroups()) {
    if (groupId.equals(yamlGroup.getId())) {
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(yamlConfig);  // ← FULL REBUILD
        for (EnrichmentGroup g : groups) {
            if (groupId.equals(g.getId())) {
                return g;
            }
        }
    }
}
```

## Problem 2: Two evaluateRules() Overloads With Silently Different Semantics

**Severity: High — Correctness Concern**

`UnifiedRuleEvaluator` has two `evaluateRules()` overloads with identical naming but silently different termination behaviour:

| Method | Behaviour |
|---|---|
| `evaluateRules(List<Rule>, EvaluationContext)` (line 638) | **Short-circuits** — returns on first match OR first error |
| `evaluateRules(List<Rule>, Map<String, Object>)` (line 696) | **Evaluates ALL** rules to ensure result-field values are stored |

Both accumulate `enrichedData`, but the `EvaluationContext` overload stops evaluating after the first significant result, while the `Map` overload continues through all rules. A caller switching from one overload to the other would get different results with no compile-time warning.

This was identified as Problem 8 in the Jan 2026 document but not addressed because it's a correctness concern rather than a structural refactoring.

## Problem 3: Dead Code — Verified No Callers

**Severity: Medium — Code Hygiene**

| Location | Method/Field | Lines | Evidence |
|---|---|---|---|
| `UnifiedRuleEvaluator` (line 940) | `extractVariableName()` | 44 | 0 callers in entire codebase (grep: 1 match = declaration only) |
| `RuleGroup` (line 289) | `updateMessage()` | 28 | 0 callers in entire codebase (grep: 1 match = declaration only) |
| `RulesEngine` (line 93) | `parser` field | 2 | Only used to initialise `ExpressionEvaluatorService` (could be local) and 1 debug log |

**Total removable:** ~74 lines of dead code, zero risk.

## Problem 4: O(n) Linear Scans for Enrichments and Transformations

**Severity: Medium — Performance**

`SequentialProcessor` performs O(n) linear scans for every enrichment/transformation lookup:

- `findEnrichmentById()` (line 598) — iterates all enrichments to find one by ID
- `findTransformationById()` (line 584) — iterates all transformations to find one by ID

These are called once per item in document-order processing. The same pattern was fixed for rules/groups in Phase 4 with index maps. Building `Map<String, YamlEnrichment>` and `Map<String, YamlTransformation>` once at the start of `evaluateSequential()` would make all lookups O(1).

## Problem 5: evaluateYaml() / evaluateYamlFile() Near-Clone Error Handling

**Severity: Medium — Code Quality**

`RulesEngine.evaluateYaml()` (line 385) and `evaluateYamlFile()` (line 432) are structurally identical — both follow the same 3-step pattern (parse → create engine → evaluate) with identical `catch (YamlConfigurationException)` and `catch (Exception)` blocks. The only difference is step 1: `fromYamlString()` vs `loadFromFile()`.

~30 lines of duplicated error handling could be extracted into a private `safeEvaluate(Supplier<YamlRuleConfiguration>, Map<String, Object>)` helper.

## Problem 6: handleEvaluationError() God Method

**Severity: Medium — Maintainability**

`UnifiedRuleEvaluator.handleEvaluationError()` (line 389) is ~115 lines with multiple responsibilities:
1. Error message creation (delegates to `createEnhancedErrorMessage()`)
2. Severity-based recovery policy lookup
3. Rule-specific default value handling
4. Error recovery service invocation
5. Recovery timing and metrics
6. Severity-based log level selection
7. Error code classification
8. Final `RuleResult` construction

This is a candidate for decomposition into smaller, focused methods (e.g., `attemptDefaultValueRecovery()`, `attemptStrategyRecovery()`, `buildErrorResult()`).

## Problem 7: YamlTransformationProcessor Created Per Item

**Severity: Medium — Allocation Overhead**

`SequentialProcessor.processTransformationItem()` creates `new YamlTransformationProcessor()` for every transformation item in document-order processing. Since `YamlTransformationProcessor` holds an `ExpressionParser` and other dependencies, this creates unnecessary object allocations. The processor could be cached as a field (same pattern as the `YamlRuleFactory` fix in Phase 4).

## Problem 8: Multiple SpelExpressionParser Instances

**Severity: Low — Resource Efficiency**

`SpelExpressionParser` is thread-safe and stateless, yet 9+ independent instances exist across production code:

| Class | Scope |
|---|---|
| `RulesEngine` | Instance field |
| `UnifiedRuleEvaluator` | Instance field |
| `ExpressionEvaluatorService` | Instance field |
| `RulesEngineConfiguration` | Package-level static |
| `ScenarioRegistryManager` | Private static |
| `ErrorRecoveryService` | Private static |
| `PipelineExecutor` | Instance field |
| `YamlEnrichmentProcessor` | Instance field |
| `ExpressionEvaluationService` (REST) | Instance field |

A single shared static instance (or injection via constructor) would reduce allocations and make the codebase clearer about parser lifecycle.

## Problem 9: RuleResult Mutable Setter Breaks Builder Contract

**Severity: Low — Design Consistency**

`RuleResult` was migrated to a Builder pattern (Jan 2026 Phase 1) with a single private constructor and immutable intent. However, `setExecutionPath(List<ExecutionStep>)` (line 753) remains as a public mutable setter, breaking the immutability contract. Callers should use `toBuilder().executionPath(path).build()` instead.

## Problem 10: Double Defensive Copying in RuleResult

**Severity: Low — Micro-Optimisation**

`RuleResult`'s Builder constructor already defensive-copies all collection fields (`enrichedData`, `failureMessages`, `executionPath`, `ruleGroupResults`). The corresponding getters then copy again:

```java
// Constructor copies once
this.enrichedData = builder.enrichedData != null ? new HashMap<>(builder.enrichedData) : new HashMap<>();

// Getter copies AGAIN
public Map<String, Object> getEnrichedData() {
    return enrichedData != null ? new HashMap<>(enrichedData) : new HashMap<>();
}
```

One copy is sufficient — either constructor OR getter, not both. Constructor-copy with unmodifiable wrappers (`Collections.unmodifiableMap()`) would be more efficient and idiomatically correct.

## Problem 11: RuleGroup Mutable Evaluation State on Model Class

**Severity: Low — Separation of Concerns**

`RuleGroup` contains mutable evaluation state fields (`ruleResults`, `groupResult`, `evaluationDuration`) that are written during evaluation and read afterward. This mixes data/model concerns with runtime state, making the class harder to reason about and not thread-safe for concurrent evaluation.

These fields should ideally live in a separate `RuleGroupEvaluationResult` value object, with `RuleGroup` remaining a pure model/configuration class.

## Problem 12: Incorrect Javadoc Reference

**Severity: Low — Documentation**

`RuleResult` class Javadoc (line 29) references "PeeGeeQ message queue system" — an incorrect project name that should reference "APEX Rules Engine".

## Problem 13: INFO-Level Logging in Hot Evaluation Loops

**Severity: Low — Production Performance**

`UnifiedRuleEvaluator.evaluateRules()` uses `logger.info()` for per-rule and per-batch logging (e.g., "Evaluating {} rules", "No rules matched"). In production with thousands of rules, INFO-level logging in tight loops adds measurable overhead. These should be `logger.debug()`.

---

## Refactoring Plan

### Phase 7: Cache EnrichmentGroupFactory Results

**Risk: Low | Impact: High | Effort: Small**

Apply the same caching pattern used for `YamlRuleFactory` in Phase 4 to `EnrichmentGroupFactory`. Build enrichment groups once per evaluation pass and look up by ID from a cached map.

**Solution:**
1. In `SequentialProcessor`, add `Map<String, EnrichmentGroup> enrichmentGroupIndex` built once at the start of `evaluateSequential()` via `EnrichmentGroupFactory.buildEnrichmentGroups(yamlConfig)` → stream to `Map<String, EnrichmentGroup>` keyed by `getId()`
2. In `SequentialProcessor.processEnrichmentGroupItem()`, replace the `buildEnrichmentGroups()` call with `enrichmentGroupIndex.get(groupId)`
3. In `RuleChainExecutor`, accept a pre-built `Map<String, EnrichmentGroup>` (or the `List<EnrichmentGroup>`) via method parameter or constructor, and look up by ID instead of rebuilding
4. Add index maps for enrichments and transformations in `SequentialProcessor` at the same time (Problem 4)

**Eliminates:** 2 × O(n×m) factory calls per evaluation, O(n) linear scans per enrichment/transformation lookup

### Phase 8: Remove Dead Code

**Risk: None | Impact: Low | Effort: Trivial**

Remove verified dead code with zero callers.

**Solution:**
1. Delete `UnifiedRuleEvaluator.extractVariableName()` — 44 lines, 0 callers
2. Delete `RuleGroup.updateMessage()` — 28 lines, 0 callers
3. Convert `RulesEngine.parser` field to local variable in constructor (used only for `ExpressionEvaluatorService` init + 1 debug log)
4. Fix `RuleResult` Javadoc: replace "PeeGeeQ" with "APEX Rules Engine"

**Eliminates:** ~74 lines of dead code

### Phase 9: Resolve evaluateRules() Semantic Split

**Risk: Medium | Impact: Medium | Effort: Medium**

Eliminate the silent behavioural difference between the two `evaluateRules()` overloads.

**Solution:**
1. Rename `evaluateRules(List<Rule>, EvaluationContext)` to `evaluateRulesShortCircuit(List<Rule>, EvaluationContext)` to make the short-circuit behaviour explicit in the method name
2. Or: unify both methods to evaluate-all semantics (the `Map` overload's behaviour), since result-field storage requires all rules to be evaluated. The `EvaluationContext` overload's short-circuit was a pre-Phase-5 optimisation that is no longer correct when result-fields are in use
3. Add Javadoc to both methods documenting the termination semantics clearly
4. Audit callers to confirm which semantic each call site actually needs

### Phase 10: Consolidate evaluateYaml() / evaluateYamlFile()

**Risk: Low | Impact: Low | Effort: Small**

Extract shared error-handling pattern into a common helper method.

**Solution:**
1. Create `private static RuleResult safeEvaluate(Supplier<YamlRuleConfiguration> configLoader, Map<String, Object> inputData)` that encapsulates the parse → create engine → evaluate pattern with both catch blocks
2. Refactor `evaluateYaml()` to call `safeEvaluate(() -> loader.fromYamlString(yamlString), inputData)`
3. Refactor `evaluateYamlFile()` to call `safeEvaluate(() -> loader.loadFromFile(yamlFilePath), inputData)`

**Eliminates:** ~30 lines of near-clone error handling

### Phase 11: Structural Improvements

**Risk: Low | Impact: Low | Effort: Small**

Address remaining code quality items.

**Solution:**
1. Remove `RuleResult.setExecutionPath()` — replace all callers with `toBuilder().executionPath(path).build()`
2. Eliminate double defensive copying in `RuleResult`: use `Collections.unmodifiableMap()` / `Collections.unmodifiableList()` wrappers in constructor, return fields directly in getters
3. Cache `YamlTransformationProcessor` as a field in `SequentialProcessor` (same pattern as `YamlRuleFactory`)
4. Downgrade `logger.info()` to `logger.debug()` in `UnifiedRuleEvaluator.evaluateRules()` hot loops

---

## Priority Matrix

| Phase | Risk | Impact | Effort | Dependencies |
|---|---|---|---|---|
| **Phase 7: Cache EnrichmentGroupFactory** | Low | **High** | Small | None |
| **Phase 8: Remove Dead Code** | None | Low | Trivial | None |
| **Phase 9: Resolve evaluateRules() Split** | Medium | Medium | Medium | None |
| **Phase 10: Consolidate evaluateYaml** | Low | Low | Small | None |
| **Phase 11: Structural Improvements** | Low | Low | Small | Phase 8 (dead code first) |

**Recommended execution order:** Phase 7 → Phase 8 → Phase 9 → Phase 10 → Phase 11

Phases 7 and 8 are independent and could be executed in parallel. Phase 9 requires careful analysis of callers before implementation.

---

## Progress Tracker

- [ ] **Phase 7: Cache EnrichmentGroupFactory** — Cache enrichment groups once per evaluation, eliminate O(n×m) factory rebuilds, add enrichment/transformation index maps
- [ ] **Phase 8: Remove Dead Code** — Delete `extractVariableName()`, `updateMessage()`, localise `parser` field, fix "PeeGeeQ" Javadoc
- [ ] **Phase 9: Resolve evaluateRules() Split** — Rename or unify the two `evaluateRules()` overloads with silently different semantics
- [ ] **Phase 10: Consolidate evaluateYaml** — Extract shared error handling into `safeEvaluate()` helper
- [ ] **Phase 11: Structural Improvements** — Remove mutable setter, fix double defensive copying, cache transformation processor, downgrade INFO→DEBUG logging
