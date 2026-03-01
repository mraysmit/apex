# APEX Rule Processing Optimisations — February 2026

**Status:** ✅ ALL PHASES COMPLETE (7-12 + DECOMPOSITION)  
**Branch:** `refactor/rules-engine-decomposition`  
**Last Updated:** March 1, 2026  
**Test Baseline:** apex-core: 2,892 tests, apex-demo: 919 tests — 0 failures, 0 errors  
**Predecessor:** `APEX_RULE_PROCESSING_OPTIMISATIONS_JAN_2026_COMPLETE.md` (6 phases, all complete)

## Overview

Following the completion of all 6 phases from the January 2026 optimisation effort, a deep analysis of the post-refactored codebase across 11 classes (~6,012 lines) revealed further opportunities for efficiency improvements. These ranged from O(n×m) performance bugs in hot paths to ~150 lines of verified dead code and structural hygiene improvements.

## Pipeline Classes (current state post-Feb 2026 refactoring)

| Class | Lines | Package | Responsibility |
|---|---|---|---|
| `RulesEngine` | 1,028 | `engine.core` | Public API, factory methods, delegation, sequential orchestration |
| `UnifiedRuleEvaluator` | 535 | `engine.core` | Canonical SpEL evaluation, delegates to collaborators for error recovery, message templating, field mapping |
| `MessageTemplateResolver` | 126 | `engine.core` | Resolves `{{#expr}}` and `#{expr}` placeholders in rule messages (extracted from UnifiedRuleEvaluator) |
| `FieldMappingProcessor` | 169 | `engine.core` | Evaluates success/error codes and applies map-to-field SpEL mappings (extracted from UnifiedRuleEvaluator) |
| `ErrorRecoveryHandler` | 302 | `engine.core` | Severity-based error recovery, enhanced error messages, error code classification (extracted from UnifiedRuleEvaluator) |
| `ExpressionEvaluatorService` | 254 | `engine.core` | Low-level SpEL parsing, context creation, used by REST API |
| `SpelParserHolder` | 49 | `engine.core` | Shared singleton `SpelExpressionParser` instance for the entire system (Phase 12) |
| `SequentialProcessor` | 691 | `engine.execution` | Document-order orchestration, item/section routing, index builders |
| `RuleGroup` | 476 | `engine.model` | Group AND/OR logic (evaluation delegated to `RuleGroupEvaluationService`) |
| `RuleResult` | 854 | `engine.model` | Result object — 1 private constructor (Builder), 25 factory methods |
| `RuleGroupExecutor` | 274 | `engine.execution` | Thin wrapper dispatching to RuleGroupEvaluationService/UnifiedRuleEvaluator |
| `RuleChainExecutor` | 373 | `engine.execution` | Chain pattern routing via `UnifiedRuleEvaluator` |
| `EnrichmentGroupExecutor` | 297 | `engine.execution` | Enrichment group logic (parallel/sequential) |
| `RuleFactory` | 1,283 | `core.config` | Rule/group construction from YAML configuration |
| `EnrichmentGroupFactory` | 321 | `core.config` | Enrichment group construction from YAML configuration |

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

This was identified as Problem 8 in the Jan 2026 document (`APEX_RULE_PROCESSING_OPTIMISATIONS_JAN_2026_COMPLETE.md`, Problem 8) but not addressed because it's a correctness concern rather than a structural refactoring. (Note: this is distinct from Problem 8 in *this* document, which concerns SpelExpressionParser instances.)

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
| `EnrichmentProcessor` | Instance field |
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

### Phase 7: Cache EnrichmentGroupFactory Results ✅

**Risk: Low | Impact: High | Effort: Small**

Apply the same caching pattern used for `YamlRuleFactory` in Phase 4 to `EnrichmentGroupFactory`. Build enrichment groups once per evaluation pass and look up by ID from a cached map.

**Solution:**
1. ✅ In `SequentialProcessor`, added `buildEnrichmentGroupIndex()`, `buildEnrichmentIndex()`, and `buildTransformationIndex()` — built once at the start of `processItemOrder()` alongside existing `ruleIndex`/`groupIndex`
2. ✅ In `SequentialProcessor.processEnrichmentGroupItem()`, replaced `buildEnrichmentGroups()` call with `enrichmentGroupIndex.get(groupId)` — O(1) lookup
3. ✅ In `RuleChainExecutor`, added `enrichmentGroupIndex` parameter to `processRuleChain()` and `executeResultBasedRoutingPattern()` — index flows from `SequentialProcessor` through the call chain
4. ✅ In `RuleChainExecutor.findEnrichmentGroup()`, replaced `EnrichmentGroupFactory.buildEnrichmentGroups()` call with index lookup — O(1)
5. ✅ Removed `YamlEnrichmentGroup` and `EnrichmentGroupFactory` imports from `RuleChainExecutor` (no longer needed)
6. ✅ Replaced O(n) linear scans `findEnrichmentById()` and `findTransformationById()` in `SequentialProcessor` with pre-built index maps — O(1) lookups
7. ✅ Updated `RuleChainExecutorSpELRoutingTest` — added convenience wrapper for the new 5-arg signature

**Net result:** SequentialProcessor 655→615 lines (−40), RuleChainExecutor 379→352 lines (−27). `EnrichmentGroupFactory.buildEnrichmentGroups()` called exactly once per evaluation pass. All enrichment, enrichment group, and transformation lookups are O(1).

### Phase 8: Remove Dead Code ✅

**Risk: None | Impact: Low | Effort: Trivial**

Remove verified dead code with zero callers.

**Solution:**
1. ✅ Deleted `UnifiedRuleEvaluator.extractVariableName()` — 49 lines removed (971→922)
2. ✅ Deleted `RuleGroup.updateMessage()` — 32 lines removed (458→426)
3. ✅ Converted `RulesEngine.parser` field to local variable in constructor (961→960)
4. ✅ Fixed `RuleResult` Javadoc: replaced duplicate block referencing "PeeGeeQ message queue system" with consolidated Javadoc (781→775)
5. ✅ Fixed "PeeGeeQ" Javadoc in 8 additional source files: `RuleStatus`, `RuleComplexity`, `RuleBase`, `Validator`, `DataLookup`, `ErrorContextService`, `RecordMatcher`, `ErrorRecoveryService`

**Net result:** −88 lines removed from 4 files. 9 stale "PeeGeeQ" Javadoc references corrected to "APEX Rules Engine" in Java source files.

> **Note:** 4 "PeeGeeQ" references remain in shell/PowerShell scripts (`update-java-headers.sh`, `update-java-headers.ps1`, `add-license-headers.sh`) and 1 in `docs-design/design/prompts.txt`. These are build/utility scripts outside the production codebase and were not in scope for this phase.

### Phase 9: Resolve evaluateRules() Semantic Split ✅

**Risk: Medium | Impact: Medium | Effort: Medium**

Eliminate the silent behavioural difference between the two `evaluateRules()` overloads.

**Solution:**
1. ✅ Audited all callers: 4 production callers (`RuleChainExecutor` ×2, `RuleGroupExecutor` ×1, `RulesEngine` ×1) and 4 test callers (`UnifiedRuleEvaluatorTest` ×4) — ALL use the `Map<String, Object>` overload
2. ✅ The `EvaluationContext` overload (short-circuit semantics) has **zero callers** — deleted as dead code
3. ✅ Improved Javadoc on the remaining `evaluateRules(List<Rule>, Map<String, Object>)` to explicitly document evaluate-all termination semantics

**Net result:** UnifiedRuleEvaluator 922→877 lines (−45). Semantic ambiguity eliminated — only one `evaluateRules()` method remains with clearly documented evaluate-all behaviour.

### Phase 10: Consolidate evaluateYaml() / evaluateYamlFile() ✅

**Risk: Low | Impact: Low | Effort: Small**

Extract shared error-handling pattern into a common helper method.

**Solution:**
1. ✅ Created `private static RuleResult safeEvaluate(Callable<YamlRuleConfiguration> configLoader, Map<String, Object> inputData)` (note: `Callable` not `Supplier` — to propagate checked exceptions from YAML parsing) that encapsulates the parse → create engine → evaluate pattern with both catch blocks (`YamlConfigurationException` → `[APEX-CFG-001]`, `Exception` → `[APEX-RULE-999]`)
2. ✅ Refactored `evaluateYaml()` to call `safeEvaluate(() -> loader.fromYamlString(yamlString), inputData)`
3. ✅ Refactored `evaluateYamlFile()` to call `safeEvaluate(() -> loader.loadFromFile(yamlFilePath), inputData)`

**Net result:** ~30 lines of near-clone error handling eliminated. Both methods reduced to 3 lines each (log + create loader + delegate to `safeEvaluate`).

### Phase 11: Structural Improvements ✅

**Risk: Low | Impact: Low | Effort: Small**

Address remaining code quality items.

**Solution:**
1. ✅ Removed `RuleResult.setExecutionPath()` — no callers remain in the codebase. The Builder + `toBuilder()` pattern is the only mutation path.
2. ✅ Eliminated double defensive copying in `RuleResult`: constructor uses `Collections.unmodifiableMap(new HashMap<>(...))` / `Collections.unmodifiableList(new ArrayList<>(...))` wrappers, getters return fields directly (no second copy). Null inputs default to `Collections.emptyMap()` / `Collections.emptyList()`.
3. ✅ Cached `TransformationProcessor` as a `private final` field in `SequentialProcessor` (line 71), initialized once in constructor (line 100). Used at line 562 via `transformationProcessor.processTransformationsWithResult()`.

### Decomposition: UnifiedRuleEvaluator → Focused Collaborators ✅

**Risk: Low | Impact: High | Effort: Medium**

Decompose the 958-line `UnifiedRuleEvaluator` monolith into three focused collaborators, each with a single responsibility.

**Problems addressed:**
- Problem 6 (handleEvaluationError() God Method)
- Problem 13 (INFO-Level Logging in Hot Loops)
- Implicit coupling of template resolution, field mapping, and error recovery within one class

**Solution:**
1. ✅ Extracted `MessageTemplateResolver` (117 lines) — resolves `{{#expr}}` and `#{expr}` placeholders in rule messages. Dependencies: `ExpressionParser` only.
2. ✅ Extracted `FieldMappingProcessor` (148 lines) — evaluates success/error code expressions and applies `map-to-field` SpEL mappings. Dependencies: `ExpressionParser` only.
3. ✅ Extracted `ErrorRecoveryHandler` (261 lines) — full error-recovery lifecycle: enhanced error messages, severity-based policy lookup, recovery execution, metrics construction, error code classification. Dependencies: `ErrorRecoveryConfig`, `ErrorRecoveryService`, `RulePerformanceMonitor`.
4. ✅ Retained `resolveMessageTemplate()` package-private delegation in `UnifiedRuleEvaluator` for test backward-compatibility (existing tests call it directly).
5. ✅ Downgraded 13 `logger.info()` calls to `logger.debug()` in hot evaluation paths (per-rule, per-batch, per-router calls). Recovery logging within `errorRecoveryConfig.isLogRecoveryAttempts()` guards retained at INFO.
6. ✅ Removed 6 unused imports (`Duration`, `Instant`, `Matcher`, `Pattern`, and constants/patterns moved to collaborators).

**Net result:** UnifiedRuleEvaluator 958→481 lines (−477, −50%). Three new collaborators created. All 3,748 tests pass (2,905 + 843, 0 failures).

---

## Known Remaining Issues

The following problem was identified in the analysis but intentionally deferred as low-priority:

| Problem | Severity | Status | Notes |
|---|---|---|---|
| **#11**: RuleGroup mutable evaluation state | Low | **Partially addressed** | `RuleGroupEvaluationResult` value object was created and is used by `RuleGroupEvaluationService`/`RuleGroupExecutor`. However, `RuleGroup` itself still carries mutable state fields (`ruleResults`, `groupResult`, `individualRuleResults`). Full cleanup would make `RuleGroup` a pure model/configuration class. |

---

## Priority Matrix

| Phase | Risk | Impact | Effort | Dependencies |
|---|---|---|---|---|
| **Phase 7: Cache EnrichmentGroupFactory** | Low | **High** | Small | None |
| **Phase 8: Remove Dead Code** | None | Low | Trivial | None |
| **Phase 9: Resolve evaluateRules() Split** | Medium | Medium | Medium | None |
| **Phase 10: Consolidate evaluateYaml** | Low | Low | Small | None |
| **Phase 11: Structural Improvements** | Low | Low | Small | Phase 8 (dead code first) |
| **Phase 12: Consolidate SpelExpressionParser** | Low | Low | Small | None |

**Recommended execution order:** Phase 7 → Phase 8 → Phase 9 → Phase 10 → Phase 11

Phases 7 and 8 are independent and could be executed in parallel. Phase 9 requires careful analysis of callers before implementation.

---

## Progress Tracker

- [x] **Phase 7: Cache EnrichmentGroupFactory** — ✅ Complete (Feb 26, 2026). Enrichment groups, enrichments, and transformations indexed once per evaluation pass. 2 × O(n×m) factory rebuilds and O(n) linear scans eliminated. SequentialProcessor 655→615, RuleChainExecutor 379→352.
- [x] **Phase 8: Remove Dead Code** — ✅ Complete (Feb 26, 2026). Removed `extractVariableName()` (49 lines), `updateMessage()` (32 lines), localised `parser` field, consolidated duplicate Javadoc. Fixed 9 "PeeGeeQ" references in Java source files. Net −88 lines.
- [x] **Phase 9: Resolve evaluateRules() Split** — ✅ Complete (Feb 27, 2026). Deleted dead `evaluateRules(List<Rule>, EvaluationContext)` overload (0 callers). Only evaluate-all semantics remain. UnifiedRuleEvaluator 922→877 (−45 lines).
- [x] **Decomposition: UnifiedRuleEvaluator** — ✅ Complete (Feb 28, 2026). Extracted 3 collaborator classes: `MessageTemplateResolver` (117 lines), `FieldMappingProcessor` (148 lines), `ErrorRecoveryHandler` (261 lines). Downgraded 13 hot-path `logger.info()` → `logger.debug()`. UnifiedRuleEvaluator 958→481 (−477 lines, −50%).
- [x] **Phase 10: Consolidate evaluateYaml** — ✅ Complete (Feb 28, 2026). Extracted `safeEvaluate(Callable, Map)` helper. Both `evaluateYaml()` and `evaluateYamlFile()` reduced to 3-line delegates. ~30 lines of cloned error handling eliminated.
- [x] **Phase 11: Structural Improvements** — ✅ Complete (Feb 28, 2026). `setExecutionPath()` removed (no callers). Double defensive copying eliminated (`Collections.unmodifiable*` in constructor, direct return in getters). `TransformationProcessor` cached as field in `SequentialProcessor`.
- [x] **Phase 12: Consolidate SpelExpressionParser** — ✅ Complete (March 1, 2026). Created `SpelParserHolder` with a single shared `public static final ExpressionParser INSTANCE`. Replaced 11 `new SpelExpressionParser()` calls across 8 files. Deleted dead `RulesEngineConfiguration.parser` field. `EnrichmentProcessor` now uses `evaluatorService.getParser()` instead of own parser (field type changed from concrete `SpelExpressionParser` to `ExpressionParser` interface). Removed `SpelExpressionParser` import from 7 files.

**Final test baseline (March 1, 2026):** apex-core: 2,892 tests, apex-demo: 919 tests = 3,811 total, 0 failures.
