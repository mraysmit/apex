# APEX Concurrency Remediation Plan

**Status:** WP-1 through WP-7 baseline complete  
**Last Updated:** 2026-03-15  
**Scope:** Remediation work for concurrency risks identified in `apex-core`.

---

## 1. Purpose

This plan turns the current concurrency review into concrete engineering work.

It is organized by risk priority, implementation order, and validation requirements so the team can improve concurrency safety without mixing unrelated refactors into the same change set.

Primary goals:

1. Remove known shared-state hazards from core evaluation paths.
2. Make concurrent behavior explicit and test-backed.
3. Prevent unsupported concurrency modes from being mistaken for production-safe features.
4. Preserve the existing external-data-source concurrency baseline while improving the rest of the engine.

---

## 1.1 Execution Status

| Work Package | Status | Notes |
|--------------|--------|-------|
| WP-1 | COMPLETE | Parallel rule-group evaluation now isolates evaluation context per task and has regression coverage. |
| WP-2 | COMPLETE | `EnrichmentProcessor` no longer relies on shared request-scoped YAML configuration state. |
| WP-3 | COMPLETE | Pipeline execution state is isolated per run instead of reusing mutable executor state. |
| WP-4 | COMPLETE | `RulesEngine` shutdown now coordinates with in-flight evaluations. |
| WP-5 | COMPLETE | Mutable registry and scenario exposures were hardened or made immutable-by-default. |
| WP-6 | COMPLETE | Enrichment-group aggregation now deep-merges nested results; scenario concurrency regressions cover nested shared input isolation. |
| WP-7 | COMPLETE | A repeatable `apex-core` benchmark now covers both inline and H2-backed lookup profiles, uses median-based sampling, and includes a captured 2026-03-15 comparison of `refactor/rules-engine-decomposition` versus `master`. |

---

## 2. Priority Summary

| Priority | Area | Why it matters | Recommended timing |
|----------|------|----------------|--------------------|
| P0 | Rule-group parallel execution context isolation | Current implementation shares mutable `StandardEvaluationContext` across threads | Fix first |
| P0 | `EnrichmentProcessor` request-state leakage | Shared `currentConfiguration` field can bleed across overlapping requests | Fix first |
| P0 | Pipeline executor shared execution state | Reused executor instance carries mutable per-run state | Fix first if pipelines can be invoked concurrently |
| P1 | Engine shutdown vs in-flight execution | Shared engine may be torn down while users are evaluating | Fix next |
| P1 | Mutable registries and maps exposed as shared state | Safe only if treated as immutable, but not enforced | Fix next |
| P2 | Broader scenario and lookup concurrency hardening | Fewer confirmed defects, but still worth codifying | After P0 and P1 |
| P3 | Throughput benchmarking and architecture optimization | Useful only once correctness issues are closed | Last |

---

## 3. Work Packages

### WP-1: Fix rule-group parallel execution

**Problem**

Parallel rule-group evaluation currently sends one mutable `StandardEvaluationContext` into multiple worker threads.

**Root cause**

- `RuleGroupEvaluationService.evaluateParallel(...)` reuses the same context for all tasks.
- `UnifiedRuleEvaluator` mutates that context by writing rule-result variables and applying field mappings.

**Required change**

- Stop sharing the same `StandardEvaluationContext` instance across parallel tasks.
- Create a per-task context clone or rebuild a fresh context from immutable facts for each rule.
- Ensure any rule-result variable propagation semantics are explicitly defined for parallel groups.

**Implementation options**

Option A:
- Disable parallel rule-group execution until isolated contexts are implemented.

Option B:
- Add a context-copy mechanism that preserves variables and root object safely for each task.

**Recommended approach**

- Use Option A only as a temporary guard if the final fix cannot be completed immediately.
- Implement Option B as the proper solution.

**Validation**

- Add a targeted test proving parallel rules do not leak variables into each other.
- Add a test where two parallel rules write different `result-field` values and verify deterministic outputs.
- Re-run `RulesEngineConcurrentEvaluationTest` and rule-group-specific tests under Maven.

**Exit criteria**

- No shared mutable evaluation context remains in parallel rule-group execution.
- Parallel rule-group semantics are documented and tested.

---

### WP-2: Remove shared request state from `EnrichmentProcessor`

**Problem**

`EnrichmentProcessor` stores the current YAML configuration in an instance field and overwrites it per request.

**Root cause**

- `currentConfiguration` is mutable instance state.
- Lookup logic accesses configuration through that shared field.

**Required change**

- Remove `currentConfiguration` from shared processor state.
- Pass configuration explicitly through the lookup and enrichment execution path.
- Treat enrichment execution as pure per-request logic over shared immutable collaborators.

**Implementation options**

Option A:
- Thread-local current configuration.

Option B:
- Explicit parameter threading through helper methods and collaborators.

**Recommended approach**

- Use explicit parameter threading. Thread-local state hides coupling and makes lifecycle bugs harder to reason about.

**Validation**

- Add a concurrency test with two overlapping enrichment evaluations using different YAML configurations.
- Verify no request observes the other request's datasource/query configuration.
- Re-run enrichment-group and external lookup integration slices.

**Exit criteria**

- `EnrichmentProcessor` has no mutable per-request configuration field.
- Concurrent enrichments with different configs are isolated.

---

### WP-3: Make pipeline execution per-run safe

**Problem**

`PipelineExecutionManager` caches a `PipelineExecutor` instance that holds mutable per-run state.

**Root cause**

- `PipelineExecutor` stores `currentPipeline`, `pipelineContext`, and `stepResults` as object fields.
- Generic context keys like `extractedData` make overlapping runs semantically unsafe.

**Required change**

- Make pipeline execution state local to a single run.
- Either instantiate a fresh `PipelineExecutor` per pipeline invocation or move all run-specific state into local method scope.
- Keep pipeline `parallel` mode as sequential fallback until step-output isolation is designed.

**Recommended approach**

- Create a fresh `PipelineExecutor` per execution first. That is the smallest correctness fix.
- Then decide whether a stateless executor refactor is worth the cleanup.

**Validation**

- Add a test that runs two pipeline evaluations concurrently on the same `RulesEngine` and verifies isolated results.
- Keep the current `parallel` flag behavior explicit in tests until true parallel mode exists.

**Exit criteria**

- Concurrent pipeline invocations do not share execution state.
- Pipeline state is isolated even when the same engine instance is reused.

---

### WP-4: Add lifecycle coordination for shutdown

**Problem**

`RulesEngine.shutdown()` can run while work is still in flight.

**Root cause**

- No coordination exists between evaluation calls and shutdown.

**Required change**

- Introduce engine lifecycle coordination.
- Prevent new work from starting after close begins.
- Decide whether shutdown waits for active work or fails fast with a clear exception.

**Implementation options**

Option A:
- Reference-count active evaluations and block shutdown until count reaches zero.

Option B:
- Add a read-write lock around evaluation and shutdown.

**Recommended approach**

- Use a simple lifecycle state plus active-evaluation counting. It makes intended semantics clearer than coarse locking.

**Validation**

- Add a test where shutdown begins during concurrent evaluation.
- Verify there is no resource corruption, partial clear, or hidden exception leakage.

**Exit criteria**

- Shared engine shutdown semantics are explicit and test-covered.

---

### WP-5: Freeze or harden mutable registries and maps

**Problem**

Several maps are only safe if callers treat them as immutable, but the code does not enforce that.

**Targets**

- `LookupServiceRegistry`
- `RulesEngine.dataSinks`
- scenario registry exposure via `getScenarioRegistry()`

**Required change**

- Decide which structures are mutable by design and which should be immutable after initialization.
- Return unmodifiable views or defensive copies where appropriate.
- Upgrade to concurrency-safe collections only where real runtime mutation is required.

**Recommended approach**

- Prefer immutability after startup over adding concurrency everywhere.

**Validation**

- Add tests ensuring external callers cannot mutate engine-owned registry state unexpectedly.
- Add tests covering any intentionally mutable registry behavior.

**Exit criteria**

- Shared registries have explicit mutation rules.
- Public getters no longer expose mutable engine internals unless that is intentional and tested.

---

### WP-6: Reassess enrichment-group and scenario parallel features

**Problem**

Once the P0 issues are fixed, the remaining parallel paths should be re-validated rather than assumed safe.

**Required change**

- Re-test enrichment-group parallel execution after `EnrichmentProcessor` is fixed.
- Re-test scenario concurrency with realistic stage combinations and mixed success/failure outcomes.

**Validation**

- Extend concurrency tests for overlapping scenario runs and enrichment-group execution.
- Include deep-copy and nested mutable object scenarios.

**Exit criteria**

- Parallel scenario-related paths have focused evidence rather than indirect confidence.

---

### WP-7: Benchmark only after correctness is closed

**Problem**

Performance work before concurrency correctness is fixed risks benchmarking the wrong design.

**Required change**

- Delay throughput claims and tuning work until P0 and P1 items are complete.
- Build representative workload benchmarks after correctness hardening.

**Validation**

- Use realistic YAML, enrichments, lookups, and downstream latencies.
- Record p50, p95, p99, throughput, heap, and queue behavior.

**Exit criteria**

- Capacity claims are evidence-based and traceable to benchmark runs.

**2026-03-15 benchmark status**

- Benchmark test: `apex-core/src/test/java/dev/mars/apex/core/performance/RulesEngineConcurrencyBenchmarkTest.java`
- Generated report: `apex-core/target/benchmark-reports/rules-engine-concurrency-benchmark.md`
- Comparison report: `apex-core/target/benchmark-reports/rules-engine-concurrency-branch-vs-master-comparison.md`
- Workloads:
	- shared `RulesEngine` instance with document-order rules plus inline lookup and calculation enrichments
	- shared `RulesEngine` instance with document-order rules plus H2-backed lookup and calculation enrichments
- Sampling note: each profile/concurrency pair now runs three measured samples and the report summarizes the median values so branch comparisons are less sensitive to single-run noise.
- Scope note: the benchmark now covers both core in-memory lookup behavior and a controlled local database-backed lookup path. It still does not model remote network latency distributions.

| Profile | Concurrency | Samples | Operations | Median Throughput (ops/s) | Median Avg (ms) | Median p50 (ms) | Median p95 (ms) | Median p99 (ms) | Median Max (ms) | Median Heap Delta (bytes) | Median Max Queue Depth | Failures |
|---------|-------------|---------|------------|---------------------------|-----------------|-----------------|-----------------|-----------------|-----------------|---------------------------|------------------------|----------|
| inline-lookup-baseline | 1 | 3 | 600 | 3,461.299 | 0.266 | 0.237 | 0.407 | 0.649 | 5.085 | 2,269,592 | 599 | 0 |
| inline-lookup-baseline | 8 | 3 | 600 | 13,836.904 | 0.558 | 0.457 | 0.826 | 4.647 | 4.916 | 2,138,904 | 592 | 0 |
| h2-database-lookup | 1 | 3 | 600 | 4,617.239 | 0.198 | 0.168 | 0.413 | 0.535 | 0.770 | 45,858,456 | 599 | 0 |
| h2-database-lookup | 8 | 3 | 600 | 19,158.740 | 0.403 | 0.370 | 0.666 | 1.019 | 1.213 | 50,331,768 | 592 | 0 |

**2026-03-15 lifecycle fast-path optimization**

- `RulesEngine` lifecycle coordination still blocks shutdown until in-flight work completes and still rejects new work once shutdown begins.
- The hot path no longer takes the lifecycle monitor on every evaluation begin/end.
- `activeEvaluations` now uses atomic accounting and only notifies the lifecycle monitor when shutdown is actually in progress and the last active evaluation exits.

**2026-03-15 prepared processing-state cache**

- `SequentialProcessor` now caches prepared rule, rule-group, enrichment-group, enrichment, and transformation indexes per `YamlRuleConfiguration` instance instead of rebuilding them on every evaluation.
- The steady-state path now uses a lock-free fast path for the primary config, so shared-engine workloads avoid both repeated rule construction and per-request cache synchronization.
- This specifically removes the repeated `RuleFactory.createRuleIndex(...)` and `createRuleGroupIndex(...)` work that was showing up in benchmark logs during document-order evaluation.

**2026-03-15 hot-path logging reduction**

- The shared-engine benchmark was still paying for per-evaluation `INFO` logs in `RulesEngine`, `SequentialProcessor`, and dataset lookup cache resolution.
- Those steady-state success-path logs are now `DEBUG`, and the dataset cache messages use parameterized logging so string formatting is skipped unless debug logging is enabled.
- The resulting benchmark jump indicates the prior medians were still strongly influenced by logger overhead at the default test logging level, not just core engine work.

**2026-03-15 `refactor/rules-engine-decomposition` vs `master` comparison**

Using the median-based harness, the 2026-03-15 optimized branch snapshot from `refactor/rules-engine-decomposition` is ahead of `master` in all four measured combinations.

| Profile | Concurrency | Current Throughput (ops/s) | Master Throughput (ops/s) | Throughput Delta | Current p95 (ms) | Master p95 (ms) | p95 Delta |
|---------|-------------|----------------------------|---------------------------|------------------|------------------|-----------------|-----------|
| inline-lookup-baseline | 1 | 3,461.299 | 1,295.650 | +167.15% | 0.407 | 1.229 | -66.88% |
| inline-lookup-baseline | 8 | 13,836.904 | 5,134.700 | +169.47% | 0.826 | 2.149 | -61.56% |
| h2-database-lookup | 1 | 4,617.239 | 1,749.264 | +163.95% | 0.413 | 0.860 | -51.98% |
| h2-database-lookup | 8 | 19,158.740 | 5,273.168 | +263.32% | 0.666 | 2.153 | -69.07% |

Interpretation:

- The correctness remediation work remains in place and the benchmark still covers both in-memory and controlled local database-backed lookup paths.
- The original single-run comparison overstated noise; once the harness moved to median-based sampling, the lifecycle fast path removed per-call monitor contention, `SequentialProcessor` stopped rebuilding per-evaluation processing indexes, and steady-state `INFO` logging was removed from the hot path, the captured `refactor/rules-engine-decomposition` snapshot outperformed `master` across all four combinations.
- The largest gains in the latest pass came from demoting per-evaluation success-path logging, which means the earlier benchmark still reflected logger overhead under the default test logging configuration.
- The current benchmark is a better measure of engine and lookup-path overhead, but production behavior will still depend on the effective logging level used in the deployment environment.

- Authoritative command: `mvn -pl apex-core clean -Dtest=RulesEngineConcurrencyBenchmarkTest test`
- Comparison command for `master`: `git worktree add .worktrees/master-benchmark master` followed by `mvn -pl apex-core -Dtest=RulesEngineConcurrencyBenchmarkTest test` inside that worktree

---

## 4. Suggested PR Sequence

Recommended pull-request order:

1. `PR-1`: Rule-group parallel context isolation.
2. `PR-2`: `EnrichmentProcessor` configuration-state removal.
3. `PR-3`: Pipeline executor per-run state isolation.
4. `PR-4`: Engine shutdown lifecycle coordination.
5. `PR-5`: Registry and mutable-map hardening.
6. `PR-6`: Follow-on concurrency coverage for scenarios and enrichment groups.
7. `PR-7`: Benchmarking and architecture tuning.

Rules for each PR:

- Keep each PR focused on one risk area.
- Add or update tests that would fail without the fix.
- Avoid opportunistic refactors unrelated to the risk being addressed.
- Use Maven concurrency slices as the acceptance gate.

---

## 5. Validation Matrix

| Work Package | Minimum validation |
|--------------|--------------------|
| WP-1 | New rule-group parallel isolation test plus targeted rules-engine slice |
| WP-2 | Overlapping enrichment config isolation test plus lookup integration slice |
| WP-3 | Concurrent pipeline invocation test plus pipeline integration slice |
| WP-4 | Evaluation-vs-shutdown race test |
| WP-5 | Registry immutability or concurrency behavior tests |
| WP-6 | Scenario and enrichment-group concurrency slices |
| WP-7 | Repeatable benchmark report with production-like payloads |

Authoritative execution path:

```bash
mvn -pl apex-core test
```

Useful focused slices should be added or updated as each work package lands.

---

## 6. Definition of Done

The remediation program is complete when all of the following are true:

1. No known high-severity shared-state concurrency issue remains unaddressed.
2. The remaining concurrent behaviors are either explicitly supported or explicitly disabled.
3. Maven tests cover the intended behavior of each supported concurrency mode.
4. Public documentation no longer overstates thread-safety claims.
5. Throughput guidance is based on benchmark evidence, not assumption.

---

## 7. Immediate Next Action

Use the current median-based comparison report as the starting point for any follow-on optimization work.

Reason:

- The correctness remediation work is now in place and has direct before/after evidence against `master`.
- The benchmark now uses median-based sampling across both in-memory and controlled local database-backed lookup paths.
- The lifecycle fast path recovered the hot-path overhead introduced by shutdown coordination, so the next optimization steps should focus on other measured bottlenecks rather than the lifecycle guard itself.