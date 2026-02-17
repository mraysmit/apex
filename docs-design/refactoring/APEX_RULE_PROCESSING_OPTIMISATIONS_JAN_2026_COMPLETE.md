# APEX Rule Processing Optimisations

**Status:** ✅ ALL 6 PHASES COMPLETE  
**Branch:** `refactor/rules-engine-decomposition`  
**Last Updated:** February 14, 2026  
**Test Baseline:** apex-core: 2,877 tests, apex-demo: 908 tests — 0 failures, 0 errors

## Overview

Analysis of the evaluation pipeline across 8 core classes (~5,920 lines) reveals significant accidental complexity, duplicated evaluation paths, and inconsistent cross-cutting concerns. This document outlines the findings and a prioritised refactoring plan.

## Pipeline Classes

| Class | Lines | Responsibility |
|---|---|---|
| `RulesEngine` | 1,100 | Public API, factory methods, delegation, sequential orchestration |
| `UnifiedRuleEvaluator` | 920 | Core SpEL evaluation, error recovery, message templating |
| `SequentialProcessor` | 935 | Document-order orchestration, item/section routing |
| `RuleGroup` | 1,068 | Group AND/OR logic with **own** SpEL evaluation pipeline |
| `RuleResult` | ~700 | Result object — 1 private constructor (Builder), 22 factory methods |
| `RuleGroupExecutor` | 290 | Thin wrapper dispatching to RuleGroup/UnifiedRuleEvaluator |
| `RuleChainExecutor` | 375 | Chain pattern routing with **own** direct SpEL evaluation |
| `EnrichmentGroupExecutor` | 297 | Enrichment group logic (parallel/sequential) |

---

## Problem 1: RuleGroup is a Parallel Universe

**Severity: Critical**

`RuleGroup` is a model/data class that contains its own `static ExpressionParser` and **4 near-clone evaluation methods**: `evaluateSequential()`, `evaluateSequentialWithDetails()`, `evaluateParallel()`, `evaluateParallelWithDetails()`. Each performs its own SpEL parsing, completely bypassing `UnifiedRuleEvaluator`.

This means rules evaluated inside groups **lack**: error recovery, performance monitoring, severity-based fail-fast, message templating, success/error codes, and field mappings.

The `Sequential` / `SequentialWithDetails` pair are structurally identical (~105 lines each), as are the `Parallel` / `ParallelWithDetails` pair — ~400 lines of near-duplicate code.

## Problem 2: Four Independent SpEL Evaluation Paths

Despite `UnifiedRuleEvaluator` being designed as the "single evaluation engine", there are 4 independent SpEL paths:

| Path | Location | Missing vs Canonical |
|---|---|---|
| A | `UnifiedRuleEvaluator.evaluateRule()` | *(canonical — full featured)* |
| B | `RuleGroup` (4 evaluation methods) | Error recovery, perf monitoring, codes, mappings |
| C | `RuleChainExecutor` router rule | Error recovery, perf monitoring |
| D | `RuleChainExecutor` trigger rule | Error recovery, perf monitoring |

A bug fix or behavioural change in the canonical path does **not** propagate to paths B, C, or D.

## Problem 3: RuleResult Constructor Explosion

`RuleResult` has 12 constructors (2 to 13 parameters) and 22 static factory methods. In `UnifiedRuleEvaluator` alone, there are **8 direct `new RuleResult(...)` calls with 12-13 parameters** — often reconstructing an entire result just to change one field (e.g., merge enrichedData). No builder pattern exists.

## Problem 4: Enabled Checks in 11 Locations Across 6 Classes

| Location | Pattern |
|---|---|
| `UnifiedRuleEvaluator` (1 location) | `!rule.isEnabled()` (primitive boolean) |
| `RuleGroup` (4 methods) | `!rule.isEnabled()` (primitive boolean) |
| `SequentialProcessor` (3 methods) | `getEnabled() != null && !getEnabled()` (nullable Boolean) |
| `RuleChainExecutor` (1 location) | `!chain.isEnabled()` |
| `YamlEnrichmentProcessor` (1 location) | `getEnabled() != null && !getEnabled()` (nullable Boolean) |
| `YamlTransformationProcessor` (1 location) | `getEnabled() != null && !getEnabled()` (nullable Boolean) |

The inconsistency between nullable `Boolean` and primitive `boolean` patterns means a rule disabled at the YAML level could be re-evaluated as enabled through a different path.

## Problem 5: StandardEvaluationContext Created Multiple Times for Same Data

Three independent creation points exist:
1. `RulesEngine.createContext()` → `evaluatorService.createEvaluationContext(facts)`
2. `UnifiedRuleEvaluator.createEvaluationContext()` → `evaluatorService.createEvaluationContext(facts)`
3. Lambda passed through `SequentialProcessor`

In `RulesEngine.executeRules()`, a context is created and passed to `RuleGroupExecutor`, but when the list contains all `Rule` objects, `UnifiedRuleEvaluator.evaluateRules(rules, facts)` creates **another** context from the same facts, discarding the first.

## Problem 6: SequentialProcessor Rebuilds Everything Per Item

`processRuleGroupItem()` creates `new YamlRuleFactory()`, `new RulesEngineConfiguration()`, converts **all** rules, registers them all, converts **all** groups, then finds the one it needs. This O(n) work happens for **every** rule-group item in document-order processing. `new YamlRuleFactory()` is instantiated at 4 separate locations.

## Problem 7: Redundant Delegation Layers

```
RulesEngine.evaluateRule(Rule, Map) : boolean       ← thin wrapper
  └→ RulesEngine.executeRule(Rule, Map) : RuleResult  ← stores result-field
       └→ UnifiedRuleEvaluator.evaluateRule(Rule, Map)   ← ALSO stores result-field
```

`result-field` storage is duplicated between `RulesEngine.executeRule()` and `UnifiedRuleEvaluator.evaluateRule(Rule, Map)`.

`RulesEngine.evaluateSequential()` is a 5-line pass-through that adds zero logic.

## Problem 8: Two evaluateRules() With Silently Different Semantics

- `evaluateRules(List<Rule>, EvaluationContext)` — returns first match or error (short-circuits)
- `evaluateRules(List<Rule>, Map)` — evaluates **ALL** rules to ensure result-field storage

Same naming pattern, silently different termination behaviour.

## Problem 9: Five Independent Error Handling Strategies

| Class | Approach |
|---|---|
| `UnifiedRuleEvaluator` | Full severity-based recovery with config, retries, default values, metrics |
| `RuleGroup` | Basic try/catch → `return false` for AND, continue for OR |
| `RuleGroupExecutor` | Wrapping try/catch → `RuleResult.error()` |
| `RuleChainExecutor` | Basic try/catch → `RuleResult.error()` |
| `SequentialProcessor` | Outer try/catch → `RuleResult.evaluationFailure()` |

---

## Refactoring Plan

### Phase 1: RuleResult Builder

**Risk: Low | Impact: High | Effort: Small**

Add `RuleResult.Builder` and `RuleResult.toBuilder()` copy method. Eliminates all 12-13 parameter constructor calls and the "reconstruct everything to change one field" anti-pattern. Every subsequent refactoring benefits from this foundation.

**Solution:**
1. ✅ Added `public static class Builder` inside `RuleResult` with fluent setters for all 16 fields
2. ✅ Added `public Builder toBuilder()` instance method that pre-populates a new Builder from `this`
3. ✅ **All 12 public constructors REMOVED** (not just deprecated) — only 1 private `RuleResult(Builder)` constructor remains
4. ✅ Migrated all `new RuleResult(...)` call sites — zero remaining in production code, zero in test code
5. ✅ All 22 static factory methods now delegate to `Builder` internally (public API preserved for backward compatibility)

**Final State:** `RuleResult` has exactly 1 constructor (private, takes Builder). All creation goes through `builder()`, `toBuilder()`, or factory methods (`match()`, `noMatch()`, `error()`, etc.).

### Phase 2: Extract Evaluation from RuleGroup

**Risk: Medium | Impact: High | Effort: Medium**

Move SpEL evaluation out of `RuleGroup` (a model class) into a `RuleGroupEvaluationService` that delegates individual rule evaluation to `UnifiedRuleEvaluator`. This:
- Makes `UnifiedRuleEvaluator` truly the single evaluation path
- Eliminates 4 near-clone methods (~400 lines)
- Gives rule groups error recovery, performance monitoring, codes, and mappings for free
- Collapses `evaluateSequential` / `evaluateSequentialWithDetails` into one parameterised method

**Solution:**
1. Create `RuleGroupEvaluationService` in `service.engine` package with a constructor accepting `UnifiedRuleEvaluator`
2. Move the 4 evaluation methods from `RuleGroup` into the new service, refactored into 2 methods: `evaluateSequential(RuleGroup, context, boolean withDetails)` and `evaluateParallel(RuleGroup, context, boolean withDetails)` — the `withDetails` flag controls whether individual `RuleResult` objects are collected
3. Replace the per-rule `parser.parseExpression()` calls with `unifiedRuleEvaluator.evaluateRule(rule, context)` — this automatically gains error recovery, performance monitoring, message templating, codes, and field mappings
4. Remove the `private static final ExpressionParser parser` field from `RuleGroup`
5. `RuleGroup.evaluate(context)` and `evaluateWithDetails(context)` become thin delegates to the service (injected via `RuleGroupExecutor`)
6. Update `RuleGroupExecutor` to own the `RuleGroupEvaluationService` and pass it through

### Phase 3: Centralise Enabled Checks

**Risk: Low | Impact: Medium | Effort: Small**

Create a single `EnabledFilter` utility applied once at the earliest entry point. Remove all 8 downstream defence-in-depth checks. Standardise on one check pattern (primitive boolean with `isEnabled()`).

**Solution:**
1. Create `EnabledFilter` utility class in `core.util` with static methods: `filterRules(List<Rule>)`, `filterEnrichments(List<YamlEnrichment>)`, `filterTransformations(List<YamlTransformation>)`, `isEnabled(YamlRule)`, `isEnabled(Rule)` etc.
2. Apply filtering once at the entry boundary — in `SequentialProcessor.processItemOrder()` before dispatch, and in `YamlRuleFactory.createRules()` / `createRuleGroups()` during construction
3. Remove the 11 individual enabled checks from `UnifiedRuleEvaluator` (1), `RuleGroup` (4), `SequentialProcessor` (3), `RuleChainExecutor` (1), `YamlEnrichmentProcessor` (1), `YamlTransformationProcessor` (1)
4. Standardise all YAML model `getEnabled()` to return `boolean` (not `Boolean`) with default `true`, eliminating the `getEnabled() != null && !getEnabled()` null-safety pattern

### Phase 4: Cache YamlRuleFactory in SequentialProcessor

**Risk: Low | Impact: Medium | Effort: Small**

Make `YamlRuleFactory` a field instead of instantiating 4 times. Pre-build rules/groups once in `evaluateSequential()` instead of per-item. Build a lookup map (`Map<String, Rule>`, `Map<String, RuleGroup>`) at the start of processing.

**Solution:**
1. Add `private final YamlRuleFactory ruleFactory` field to `SequentialProcessor` constructor
2. At the start of `evaluateSequential()`, build lookup maps once:
   ```java
   Map<String, Rule> ruleIndex = ruleFactory.createRuleIndex(yamlConfig);
   Map<String, RuleGroup> groupIndex = ruleFactory.createRuleGroupIndex(yamlConfig, configuration);
   ```
3. Add `createRuleIndex()` and `createRuleGroupIndex()` methods to `YamlRuleFactory` that return `Map<String, T>` keyed by ID
4. Refactor `processRuleItem()` and `processRuleGroupItem()` to do a simple `ruleIndex.get(id)` lookup instead of iterating YAML lists and recreating all rules/groups
5. Remove all 4 `new YamlRuleFactory()` instantiations from `SequentialProcessor`

### Phase 5: Clean Up RulesEngine Delegation

**Risk: Low | Impact: Low | Effort: Small**

- Remove `evaluateRule(Rule, Map) : boolean` wrapper — callers can use `.isTriggered()`
- Inline `evaluateSequential()` pass-through
- Fix double `result-field` storage between `executeRule()` and `UnifiedRuleEvaluator`
- Stop creating `StandardEvaluationContext` in `executeRules()` when it will be re-created downstream

**Solution:**
1. Deprecate `RulesEngine.evaluateRule(Rule, Map) : boolean`, `evaluateRules(List, Map) : boolean`, and `evaluateRulesForCategory(String, Map) : boolean` — these are trivial wrappers that call `execute*().isTriggered()`
2. In `RulesEngine.executeRule()`, remove the `result-field` storage block (lines 596-600) since `UnifiedRuleEvaluator.evaluateRule(Rule, Map)` already handles this with richer behaviour (nested field support + enrichedData population)
3. Inline `evaluateSequential()` — move the `sequentialProcessor.evaluateSequential(...)` call with method references directly into `evaluate(YamlRuleConfiguration, Map)`
4. In `RulesEngine.executeRules(List<RuleBase>, Map)`, remove `createContext(facts)` call — let `RuleGroupExecutor` or `UnifiedRuleEvaluator` create context when needed, avoiding the discarded context

### Phase 6: Unify RuleChainExecutor SpEL Calls

**Risk: Medium | Impact: Medium | Effort: Medium**

Route chain router/trigger evaluation through `UnifiedRuleEvaluator` instead of using direct `parser.parseExpression()`. This brings error recovery and performance monitoring to chain evaluation. Consolidate the two `RuleChainExecutor` classes (`engine.config.execution.RuleChainExecutor` at 375 lines and `engine.executor.RuleChainExecutor` at 233 lines) into a single class.

**Solution:**
1. Audit both `RuleChainExecutor` classes to identify which patterns each supports — `config.execution` handles `conditional-chaining` and `result-based-routing`; `engine.executor` handles `sequential-dependency` and other patterns via `PatternExecutor` subclasses
2. Merge into a single `RuleChainExecutor` in `engine.config.execution`, incorporating any missing pattern handlers from `engine.executor`
3. For router-rule evaluation (line 157): create a transient `Rule` object from the chain's condition/result-field and evaluate via `unifiedRuleEvaluator.evaluateRule(rule, context)` instead of raw `parser.parseExpression(condition).getValue(context)`
4. For trigger-rule evaluation (line 274): same approach — wrap condition in a `Rule` and delegate to `UnifiedRuleEvaluator`
5. Remove the `ExpressionParser parser` field from `RuleChainExecutor` — it should only hold `UnifiedRuleEvaluator`
6. Delete `engine.executor.RuleChainExecutor` after migration; update any references from `PatternExecutor` subclasses

---

## Expected Outcomes

| Metric | Before | After (est.) |
|---|---|---|
| Total lines across pipeline | ~5,920 | ~4,200 |
| Independent SpEL evaluation paths | 4 | 1 |
| RuleResult constructors | 12 | 1 (private Builder constructor) |
| Enabled-check locations | 11 | 1-2 |
| YamlRuleFactory instantiations per evaluation | 4 | 1 |
| Near-clone method pairs | 2 | 0 |

---

## Progress Tracker

- [x] **Phase 1: RuleResult Builder** — Builder + toBuilder() added, all 12 public constructors **removed**, zero `new RuleResult(...)` calls remain (11 tests)
- [x] **Phase 2: Extract Evaluation from RuleGroup** — Move SpEL evaluation into `RuleGroupEvaluationService`, delegate to `UnifiedRuleEvaluator` (13 tests)
- [x] **Phase 3: Centralise Enabled Checks** — Single `EnabledFilter` utility, remove 10 downstream checks (31 tests)
- [x] **Phase 4: Cache YamlRuleFactory in SequentialProcessor** — Field-level factory, pre-built lookup maps (8 tests)
- [x] **Phase 5: Clean Up RulesEngine Delegation** — Remove boolean wrappers, inline pass-throughs, fix double result-field storage
- [x] **Phase 6: Unify RuleChainExecutor SpEL Calls** — Route through `UnifiedRuleEvaluator`, deprecate `engine.executor.RuleChainExecutor` (14 tests)

---

## Remaining Work (Other Documents)

The following tasks from separate task documents are **not part of this optimisation effort** but represent the outstanding refactoring work on the branch:

### ✅ COMPLETED (since initial document)

| Task | Document | Status |
|------|----------|--------|
| Error propagation to RuleResult | `ERROR_HANDLING_IMPROVEMENT_TASKS.md` Task 4 | ✅ Complete (Feb 13, 2026) |
| APEX-specific exceptions | `ERROR_HANDLING_IMPROVEMENT_TASKS.md` Task 3 | ✅ Complete (Feb 11, 2026) |
| Correct log levels (WARN→ERROR) | `ERROR_HANDLING_IMPROVEMENT_TASKS.md` Task 2 | ✅ Complete (Feb 11, 2026) — 16 changes across 8 files |
| Stack trace verbosity | `ERROR_HANDLING_IMPROVEMENT_TASKS.md` Task 5 | ✅ Complete — error+debug pattern in ConfigurationContext, ComponentLoader, YamlConfigurationLoader |
| APEX Error Codes document | `ERROR_HANDLING_IMPROVEMENT_TASKS.md` Task 4.4 | ✅ Complete — `docs/APEX_ERROR_CODES.md` exists |
| Exception logging fixes | `EXCEPTION_LOGGING_IMPROVEMENTS.md` | ✅ Complete (Feb 11, 2026) — 76 fixes across 28 files in 3 modules |
| Error propagation integration tests | `ERROR_HANDLING_IMPROVEMENT_TASKS.md` Task 6 | ✅ Complete (Feb 13, 2026) — 23 tests in 7 nested classes |
| Test context markers | `ERROR_HANDLING_IMPROVEMENT_TASKS.md` Task 1 | ✅ Complete (Feb 12, 2026) — 15 new + 4 previously done, MDC-based `[EXPECTED]` prefix |
| EnabledFilter adoption | Phase 3 residual + broader codebase | ✅ Complete (Feb 13, 2026) — 14 inline checks migrated across 5 files, 7 new overloads added to `EnabledFilter` |
| Remove deprecated pipeline classes | `APEX_TECHNICAL_DEBT_ANALYSIS.md` Phase 2 | ✅ Complete (Feb 13, 2026) — 4 classes removed: `PipelineExecutionResult` (dead code), `PipelineStepResult`, `YamlPipelineExecutionResult` (replaced by `ExecutionStep`/`RuleResult`), `DataPipelineException` converted to `RuntimeException`. `PipelineExecutor` now returns `RuleResult` directly. |
| Clean TODO placeholders | `APEX_TECHNICAL_DEBT_ANALYSIS.md` Phase 1 | ✅ Complete (Feb 13, 2026) — 8 `YamlDataSink` conversion methods implemented, stale `YamlEnrichmentProcessor` TODO fixed. 3 `SequentialYamlProcessor` TODOs retained (real feature work referencing non-existent processor classes). |
| Remove deprecated `YamlEnrichmentProcessor` methods | `APEX_TECHNICAL_DEBT_ANALYSIS.md` Section 1.2 | ✅ Complete (Feb 14, 2026) — 8 deprecated methods removed (314 lines), `processEnrichment(2-arg)` un-deprecated and made private. 7 test methods removed from `YamlEnrichmentProcessorComprehensiveTest`. Class retained as core infrastructure (3 non-deprecated methods actively used by `SequentialProcessor`, `EnrichmentGroupExecutor`, `RulesEngine`). |

### 🟡 REMAINING (Outstanding)

| Task | Document | Description |
|------|----------|-------------|
| `SequentialYamlProcessor` integration TODOs | `APEX_TECHNICAL_DEBT_ANALYSIS.md` Section 2 | 3 TODOs reference non-existent processor classes — real feature work, not cleanup |
