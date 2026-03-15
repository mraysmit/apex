# APEX Concurrency Architecture and Implementation Guide

**Status:** Active design document  
**Last Updated:** 2026-03-15  
**Scope:** `apex-core` concurrency behavior, external data-source hardening, high-volume processing guidance, and pipeline parallel-execution design.

---

## 1. Purpose

This document replaces the previous concurrency notes, high-volume architecture draft, and pipeline parallel-execution guide.

It has four goals:

1. Capture the current verified concurrency state of the APEX codebase.
2. Record the external data-source concurrency work already completed and the few areas still worth monitoring.
3. Provide a realistic architecture direction for high-volume concurrent processing.
4. Define the remaining design work required before pipeline parallel mode can be implemented safely.

This document is intentionally conservative. Claims in this file should be either:

- verified against the current codebase,
- verified by automated tests, or
- explicitly labeled as planning guidance rather than an established runtime fact.

---

## 2. Executive Summary

### Current position

APEX already supports meaningful concurrent use in the core rules-engine and external-data-source layers, but the concurrency story is not uniform across the entire codebase.

What is currently true:

- `RulesEngine` exposes stable static factory entry points and is suitable for concurrent evaluation patterns already covered by Maven tests.
- The external-data-source concurrency slice has dedicated test coverage for factory, cache, database, filesystem, REST, and integration scenarios.
- The current pipeline `parallel` execution mode still falls back to sequential execution.
- The current pipeline executor is not yet safe for real same-level parallel step execution because it stores shared step outputs under generic context keys.

What is not yet justified as a hard claim:

- That APEX is universally free of mutable shared state.
- That all registries and supporting services are thread-safe.
- That the system can sustain specific six-figure throughput targets without benchmark evidence.
- That engine pooling or reactive-stream execution is necessary for near-term delivery.

### Recommended direction

- Keep the current external-data-source concurrency hardening as the baseline.
- Use a simple worker-pool architecture for high-volume processing first.
- Treat `RulesEngine.fromFile(...)` or `RulesEngine.fromYamlConfig(...)` as the canonical engine creation path.
- Do not implement true pipeline parallel mode until step output isolation and dependency-aware data flow are designed explicitly.

---

## 3. Current Verified Codebase State

### 3.1 Canonical rules-engine entry points

Current code should use the `RulesEngine` static factories:

- `RulesEngine.fromFile(...)`
- `RulesEngine.fromClasspath(...)`
- `RulesEngine.fromYamlConfig(...)`
- `RulesEngine.fromScenarioRegistry(...)` where scenario-based processing is needed

These are the active entry points for normal runtime creation. Planning examples and architecture notes should align with them.

### 3.2 Concurrency model by component

The following table summarizes the current concurrency posture.

| Component | Current State | Guidance |
|----------|---------------|----------|
| `RulesEngine` | Safe enough for the currently tested concurrent evaluation scenarios | Use factory-created engines; validate with Maven for concurrency slices |
| `DataSourceFactory` | Shared singleton with targeted concurrency coverage | Safe for current supported use; keep lifecycle races under observation |
| `ApexCacheManager` and external cache managers | Shared, concurrency-sensitive infrastructure | Keep invariant-based tests authoritative |
| `StandardEvaluationContext` | Per-evaluation object, not safe to share | Create fresh contexts per evaluation |
| `LookupServiceRegistry` | Backed by `ConcurrentHashMap`; `getRegisteredServices()` returns an unmodifiable defensive copy | Safe for concurrent reads; mutation is confined to registration-time setup |
| `PipelineExecutor` | Context maps are concurrent, but execution is still sequential in parallel mode | Do not assume true parallel step safety |

### 3.3 Important caveats

These caveats should shape all future concurrency work:

- A thread-safe collection does not automatically make the surrounding workflow thread-safe.
- Pipeline context mutation is currently organized around generic keys like `extractedData`, not per-step outputs.
- Registry thread safety must be proven from implementation details, not inferred from how often concurrent maps are used elsewhere.
- Maven test runs are the source of truth for the concurrency slice; IDE runners may produce false negatives because of module reflection restrictions.

---

## 4. External Data-Source Concurrency Status

### 4.1 Scope covered so far

The first major concurrency hardening stream focused on the external data-source stack in `apex-core`:

- `DataSourceFactory`
- `InMemoryCacheManager`
- `DataSourceMetrics`
- `DatabaseDataSource`
- `FileSystemDataSource`
- `RestApiDataSource`
- integration behavior across these components

### 4.2 Risk matrix snapshot

| Concern | Status | Decision |
|---------|--------|----------|
| `factory-singleton-contention` | Not reproduced by targeted tests | Closed |
| `factory-provider-register-remove-race` | Not reproduced by targeted tests | Closed |
| `factory-clear-during-create` | Not reproduced as cache corruption; lifecycle still deserves review | Monitor |
| `cache-size-accounting-drift` | Reproduced and fixed | Closed with fix |
| `cache-clear-shutdown-lifecycle-race` | Reproduced and fixed | Closed with fix |
| `metrics-min-max-update-race` | Not reproduced by extended tests | Closed |
| `metrics-reset-during-update-race` | Semantics acceptable but not strongly linearized | Monitor |
| `database-query-shutdown-race` | Not reproduced | Closed |
| `filesystem-monitor-shutdown-race` | Not reproduced | Closed |
| `filesystem-request-cache-shape-race` | Reproduced and fixed | Closed with fix |
| `rest-cache-breaker-race` | Not reproduced | Closed |

### 4.3 Test coverage now present

The following tests are part of the current concurrency baseline:

- `DataSourceFactoryConcurrencyTest`
- `InMemoryCacheManagerConcurrencyTest`
- `DatabaseDataSourceConcurrencyTest`
- `FileSystemDataSourceConcurrencyTest`
- `RestApiDataSourceConcurrencyTest`
- `ConcurrencyIntegrationTest`
- `MixedExternalDataSourceConcurrencyIntegrationTest`
- `RulesEngineConcurrentEvaluationTest`
- `RulesEngineLifecycleCoordinationTest`
- `ScenarioConcurrentAccessTest`
- `StageExecutionConcurrencyTest`
- `ResultThreadSafetyTest`
- `ConfigurationContextTest`
- `DataSourceRegistryGetOrCreateTest`
- `EnrichmentProcessorConcurrentConfigurationTest`
- `PipelineExecutionManagerStateIsolationTest`
- `ExternalDataSourceReferenceLookupIntegrationTest`

### 4.4 Validation commands

Useful Maven slices:

```bash
mvn -pl apex-core "-Dtest=DataSourceFactoryConcurrencyTest,InMemoryCacheManagerConcurrencyTest,DatabaseDataSourceConcurrencyTest,FileSystemDataSourceConcurrencyTest,RestApiDataSourceConcurrencyTest" test

mvn -pl apex-core "-Dtest=ConcurrencyIntegrationTest,MixedExternalDataSourceConcurrencyIntegrationTest" test

mvn -pl apex-core -Dtest=ExternalDataSourceReferenceLookupIntegrationTest test
```

Representative broad slice covering all baseline tests:

```bash
mvn -pl apex-core "-Dtest=DataSourceFactoryConcurrencyTest,InMemoryCacheManagerConcurrencyTest,DatabaseDataSourceConcurrencyTest,FileSystemDataSourceConcurrencyTest,RestApiDataSourceConcurrencyTest,ConcurrencyIntegrationTest,MixedExternalDataSourceConcurrencyIntegrationTest,RulesEngineConcurrentEvaluationTest,RulesEngineLifecycleCoordinationTest,ScenarioConcurrentAccessTest,StageExecutionConcurrencyTest,ResultThreadSafetyTest,ConfigurationContextTest,DataSourceRegistryGetOrCreateTest,EnrichmentProcessorConcurrentConfigurationTest,PipelineExecutionManagerStateIsolationTest,ExternalDataSourceReferenceLookupIntegrationTest" test
```

### 4.5 Confirmed implementation note for H2 integration tests

For the `RulesEngine` and `DataSourceRegistry` H2 runtime path, prefer `connection.database` over `connection.url` in external datasource configurations. The JDBC path synthesizes the H2 JDBC URL from H2-specific connection fields in that flow.

---

## 5. High-Volume Concurrent Processing Architecture

### 5.1 Goal

Support concurrent processing of messages from multiple upstream systems while preserving APEX correctness, observability, and operational simplicity.

Potential workloads include:

- OTC derivatives
- FX messages
- equity or bond processing
- internal risk or control events

### 5.2 Recommended phase-one runtime model

The preferred near-term architecture is a bounded worker-pool model:

1. Read messages from a queue or API ingress.
2. Convert each message into an immutable or isolated request payload.
3. Create or obtain the required `RulesEngine` using current factory APIs.
4. Evaluate the request.
5. Emit the result to downstream systems.
6. Record latency, throughput, and failure metrics.

This approach is recommended first because it:

- matches the current codebase better than speculative engine pooling,
- keeps failure isolation simple,
- avoids premature concurrency patterns in unsupported areas, and
- is easy to benchmark with realistic production data.

### 5.3 Recommended worker design

Use a bounded executor or equivalent worker pool with explicit backpressure.

Principles:

- keep queue capacity finite,
- prefer caller-runs or upstream throttling over unbounded buffering,
- isolate request state completely,
- avoid blocking event-loop frameworks with synchronous joins,
- shut down engines and downstream resources cleanly where lifecycle ownership requires it.

### 5.4 What to avoid for now

Do not treat the following as baseline architecture until benchmarked and justified:

- engine pooling,
- reactive-stream migration,
- six-figure throughput promises,
- broad claims that object allocation is the dominant bottleneck.

These may become relevant later, but they are optimization strategies, not starting assumptions.

### 5.5 Planning throughput targets

The previous draft included aggressive throughput figures. Those numbers should be treated as planning hypotheses, not verified runtime guarantees.

Use a benchmark ladder instead:

| Phase | Intent | Validation Requirement |
|-------|--------|------------------------|
| Phase 1 | Prove correctness and bounded parallelism | Stable Maven concurrency suite plus representative load test |
| Phase 2 | Tune caches, pool sizes, and downstream latency | Repeatable performance test results with realistic payloads |
| Phase 3 | Evaluate architecture upgrades only if needed | Evidence that simpler design no longer meets SLOs |

### 5.6 Observability requirements

Any serious high-volume deployment should capture at least:

- throughput by message type,
- p50, p95, and p99 latency,
- failure counts and failure categories,
- queue depth or backlog,
- active worker count,
- downstream lookup latency and error rates,
- cache hit rates where relevant.

### 5.7 Deployment guidance

The deployment pattern should stay horizontally scalable and operationally boring:

- stateless processing nodes,
- shared external databases and APIs behind bounded client pools,
- queue-driven or partition-driven load distribution,
- rollout strategy that avoids in-place partial config mutation.

Blue-green or equivalent controlled configuration rollout is preferred over ad hoc runtime mutation.

---

## 6. Pipeline Parallel Execution

### 6.1 Current actual status

Pipeline execution currently supports dependency validation and a `parallel` mode flag, but the implementation still falls back to sequential execution.

This is the correct current summary:

- dependency ordering exists,
- context maps are backed by concurrent collections,
- true same-level parallel execution is not implemented yet.

### 6.2 Why the remaining work is non-trivial

The missing work is not just scheduling. The harder problem is safe data flow.

Current pipeline execution stores outputs under shared keys such as:

- `extractedData`
- `schemaMetadata`
- `tableSchemas`
- `schemaDiffResult`

If two independent same-level steps run together and both publish to a shared key, the last writer wins and downstream behavior becomes order-dependent.

That means replacing `HashMap` with `ConcurrentHashMap` is not enough. The parallel design must prevent semantic collisions, not just data-structure corruption.

### 6.3 Preconditions for safe parallel mode

Before implementing real parallel execution, define all of the following:

1. **Dependency graph semantics**
   Build a DAG from step dependencies and reject cycles or missing references.

2. **Execution-level planning**
   Group steps into levels only when all dependencies are satisfied.

3. **Step-scoped outputs**
   Store outputs per step name or per declared output key, not under generic shared names.

4. **Explicit input binding**
   Downstream steps must reference the output of specific predecessor steps.

5. **Parallel error semantics**
   `stop-on-error` and `continue-on-error` behavior must be defined for in-flight siblings.

6. **Lifecycle ownership**
   Shared resources used by parallel steps must not be shut down while work is in flight.

7. **Deterministic tests**
   Use barriers or latches to prove correctness under controlled contention.

### 6.4 Minimum implementation plan

The minimal safe plan for pipeline parallel mode is:

1. Introduce step-scoped result storage.
2. Add explicit output-to-input wiring semantics.
3. Build dependency graph validation and execution levels.
4. Execute each level in parallel only after the first three items are complete.
5. Add tests for same-level write isolation, failure handling, and repeated execution stability.

### 6.5 Testing expectations for pipeline concurrency

Required coverage before enabling true parallel mode:

- steps in the same level do not overwrite each other,
- dependent transforms consume the correct predecessor output,
- `stop-on-error` cancels or blocks downstream work as defined,
- repeated runs do not show flaky order-sensitive failures,
- timing tests prove concurrency without becoming CI-fragile.

Until those conditions are met, pipeline `parallel` should remain an intentionally conservative sequential fallback.

---

## 7. Testing Strategy

### 7.1 Source of truth

Use Maven as the authoritative validation path for concurrency work in `apex-core`.

Reason:

- IDE Java runners can report false negatives for these slices because of module reflection restrictions.

### 7.2 Test design rules

Concurrency tests should prefer invariant-based assertions over exact interleavings.

Recommended rules:

- coordinate contention with `CountDownLatch`, `CyclicBarrier`, or `Phaser`,
- assert state after quiescence,
- keep default test scope fast and deterministic,
- isolate exploratory stress tests rather than weakening core tests,
- re-run focused slices when a race looks timing-sensitive.

### 7.3 Definition of done for concurrency changes

A concurrency change is done when:

1. the relevant race is either reproduced and fixed or explicitly closed with evidence,
2. the targeted Maven slice passes repeatedly,
3. invariant assertions cover the intended behavior, and
4. any accepted non-determinism is documented rather than implied.

---

## 8. Remaining Backlog

### 8.1 Short-term backlog

- Continue monitoring `DataSourceFactory` lifecycle behavior around clear and shutdown paths.
- Keep validating `DataSourceMetrics` reset semantics if reset becomes more important in runtime code.
- Preserve the current external-data-source concurrency suite as a guarded baseline.

Note: The prioritized remediation work in `APEX_CONCURRENCY_REMEDIATION_PLAN.md` (WP-1 through WP-7) is now complete. See that document for execution status and benchmark results.

### 8.2 Medium-term backlog

- Add broader YAML lookup and failover integration coverage where business flows depend on multiple external sources.
- Add representative throughput benchmarks using realistic financial payloads and downstream latency distributions.
- Define the pipeline output model before any attempt to enable true parallel step execution.

### 8.3 Long-term backlog

- Re-evaluate engine pooling only if profiling shows engine creation or teardown is a measurable bottleneck.
- Re-evaluate reactive-stream processing only if queue-driven worker pools cannot meet operational targets.

---

## 9. Operational Recommendations

For current APEX concurrency work:

- prefer simple, explicit lifecycle management,
- favor isolated request processing over shared mutable orchestration state,
- benchmark before promoting capacity claims into architecture commitments,
- keep documentation aligned with the actual code and test suite.

This document should be updated whenever one of the following changes:

- the concurrency test baseline changes,
- pipeline parallel execution becomes real,
- a formerly monitored race is reproduced or closed, or
- benchmark evidence materially changes the recommended architecture.

---

## 10. Document History

This document consolidates and replaces:

- `APEX_CONCURRENCY_CONCERNS_NOTES.md`
- `APEX_HIGH_VOLUME_CONCURRENT_PROCESSING_ARCHITECTURE.md`
- `PARALLEL_EXECUTION_IMPLEMENTATION_GUIDE.md`

The replaced documents contained useful material, but they had diverged in status, terminology, and accuracy. This consolidated guide is intended to be the single maintained source for concurrency design in `docs-design/concurrency`.