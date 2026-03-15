

## Concurrent Access Concerns Identified

### 1. **DataSourceFactory Singleton Pattern Issues**
- **Double-checked locking implementation**: While the current implementation uses `volatile` and synchronized blocks, it's correct but could benefit from additional concurrent access testing
- **Shared cache maps**: `jdbcDataSourceCache`, `httpClientCache`, and `customProviders` use `ConcurrentHashMap` which is good, but cache operations aren't atomic across multiple maps

### 2. **Cache Management Thread Safety**
- **InMemoryCacheManager**: Uses `ConcurrentHashMap` but has race conditions in:
    - `evictLRU()` method during size checking and eviction
    - `evictExpired()` iterator operations
    - Cache statistics updates during concurrent access

### 3. **DataSource Implementation Concerns**
- **DatabaseDataSource**: `resultCache` and `preparedQueries` use `ConcurrentHashMap` but cache key generation and TTL checking aren't atomic
- **FileSystemDataSource**: File operations and cache clearing aren't synchronized
- **RestApiDataSource**: Response caching has similar atomicity issues

### 4. **Metrics Collection Race Conditions**
- **DataSourceMetrics**: While using `AtomicLong` and `AtomicReference`, the min/max response time updates have potential race conditions in the compare-and-set loops

### 5. **Test-Specific Concurrency Issues**
- **Singleton thread safety test**: Only tests 10 threads, insufficient for stress testing
- **Cache clearing in tearDown**: Not synchronized with ongoing operations
- **Custom provider registration**: No concurrent registration/unregistration testing

## Actionable Engineering Plan

This section converts the concurrency note into a concrete implementation plan tied to the current APEX codebase. It assumes we will validate each suspected race with tests first, then harden only the paths that fail or remain structurally unsafe under load.

## Scope

Primary production targets:
- `apex-core/src/main/java/dev/mars/apex/core/service/data/external/factory/DataSourceFactory.java`
- `apex-core/src/main/java/dev/mars/apex/core/service/data/external/cache/InMemoryCacheManager.java`
- `apex-core/src/main/java/dev/mars/apex/core/service/data/external/DataSourceMetrics.java`
- `apex-core/src/main/java/dev/mars/apex/core/service/data/external/database/DatabaseDataSource.java`
- `apex-core/src/main/java/dev/mars/apex/core/service/data/external/file/FileSystemDataSource.java`
- `apex-core/src/main/java/dev/mars/apex/core/service/data/external/rest/RestApiDataSource.java`

Primary existing test targets to extend:
- `apex-core/src/test/java/dev/mars/apex/core/service/data/external/factory/DataSourceFactoryTest.java`
- `apex-core/src/test/java/dev/mars/apex/core/service/data/external/cache/InMemoryCacheManagerTest.java`
- `apex-core/src/test/java/dev/mars/apex/core/service/data/external/DataSourceMetricsTest.java`
- `apex-core/src/test/java/dev/mars/apex/core/service/data/external/ConcurrencyIntegrationTest.java`

Likely new test classes to add:
- `apex-core/src/test/java/dev/mars/apex/core/service/data/external/factory/DataSourceFactoryConcurrencyTest.java`
- `apex-core/src/test/java/dev/mars/apex/core/service/data/external/cache/InMemoryCacheManagerConcurrencyTest.java`
- `apex-core/src/test/java/dev/mars/apex/core/service/data/external/database/DatabaseDataSourceConcurrencyTest.java`
- `apex-core/src/test/java/dev/mars/apex/core/service/data/external/file/FileSystemDataSourceConcurrencyTest.java`
- `apex-core/src/test/java/dev/mars/apex/core/service/data/external/rest/RestApiDataSourceConcurrencyTest.java`

Execution boundary for the first sprint:
- In scope: external-data-source concurrency correctness, lifecycle safety, and invariant-based tests.
- Out of scope: broad rules-engine concurrency, YAML parser concurrency, large performance benchmarking, and registry redesign outside what is needed to fix confirmed defects.

## Work Package Matrix

| Work Package | Target | Primary Risk | Main Test Surface | Expected Outcome |
|--------------|--------|--------------|-------------------|------------------|
| WP-1 | `DataSourceFactory` | lifecycle and cache-clear races | new `DataSourceFactoryConcurrencyTest` | no lost cache integrity during concurrent create/clear |
| WP-2 | `InMemoryCacheManager` | size/accounting drift under contention | new `InMemoryCacheManagerConcurrencyTest` | bounded size, no negative counters, stable shutdown |
| WP-3 | `DataSourceMetrics` | inconsistent totals or reset races | extend `DataSourceMetricsTest` | counts remain logically consistent under load |
| WP-4 | `DatabaseDataSource` | query/cache/lifecycle interleavings | new `DatabaseDataSourceConcurrencyTest` | stable parallel reads and safe shutdown |
| WP-5 | `FileSystemDataSource` | monitor executor and invalidation races | new `FileSystemDataSourceConcurrencyTest` | no monitor leaks or stale-state corruption |
| WP-6 | `RestApiDataSource` | cache and circuit-breaker state races | new `RestApiDataSourceConcurrencyTest` | consistent breaker and metrics behavior |
| WP-7 | cross-component regression | flaky interactions across components | extend `ConcurrencyIntegrationTest` | repeatable suite with no intermittent failures |

## Guiding Rules

1. Validate suspected races with deterministic tests before changing synchronization.
2. Keep the public API stable; prefer internal synchronization and lifecycle hardening.
3. Do not block unrelated code paths with coarse global locks unless a narrower strategy fails.
4. Separate fast unit concurrency tests from slower stress tests so the default build stays usable.
5. Prefer proving invariants such as no duplicate creation, no negative sizes, no leaked executors, and no lost metric counts.

## Phase 0: Confirm the Risk Matrix

Goal: classify each item in this note as either confirmed defect, likely design risk, or acceptable current behavior.

Deliverables:
- A short table added to this document or a sibling design note with columns: concern, target class, current evidence, test coverage, action.
- One focused test per concern that reproduces the risk or demonstrates current safety.

Required output format for the risk matrix:
- `Concern`: short identifier such as `factory-clear-during-create`.
- `Current Evidence`: `code inspection`, `reproduced by test`, or `not reproduced`.
- `Decision`: `fix now`, `monitor`, or `close`.
- `PR`: planned PR number.

### Phase 0 Status Snapshot

| Concern | Target Class | Current Evidence | Decision | PR |
|---------|--------------|------------------|----------|----|
| `factory-singleton-contention` | `DataSourceFactory` | not reproduced by `DataSourceFactoryConcurrencyTest` | close | PR-1 |
| `factory-provider-register-remove-race` | `DataSourceFactory` | not reproduced by `DataSourceFactoryConcurrencyTest` | close | PR-1 |
| `factory-clear-during-create` | `DataSourceFactory` | code inspection plus targeted test coverage; no cache corruption reproduced so far | monitor | PR-3 |
| `cache-size-accounting-drift` | `InMemoryCacheManager` | reproduced by `InMemoryCacheManagerConcurrencyTest` | fix now | PR-1 |
| `cache-clear-shutdown-lifecycle-race` | `InMemoryCacheManager` | reproduced by test-driven inspection of clear/shutdown interleavings | fix now | PR-1 |
| `metrics-min-max-update-race` | `DataSourceMetrics` | not reproduced by extended `DataSourceMetricsTest` | close | PR-1 |
| `metrics-reset-during-update-race` | `DataSourceMetrics` | covered by extended `DataSourceMetricsTest`; weaker non-linearizable reset semantics appear acceptable | monitor | PR-2 |
| `database-query-shutdown-race` | `DatabaseDataSource` | not reproduced by `DatabaseDataSourceConcurrencyTest` | close | PR-4 |
| `filesystem-monitor-shutdown-race` | `FileSystemDataSource` | not reproduced by `FileSystemDataSourceConcurrencyTest` | close | PR-4 |
| `filesystem-request-cache-shape-race` | `FileSystemDataSource` | reproduced by combined concurrency slice; fixed by separating file-list cache writes from request-result cache writes | fix now | PR-4 |
| `rest-cache-breaker-race` | `RestApiDataSource` | not reproduced by `RestApiDataSourceConcurrencyTest` | close | PR-4 |

Evidence behind the current snapshot:
- `DataSourceFactoryConcurrencyTest`, `InMemoryCacheManagerConcurrencyTest`, and the extended `DataSourceMetricsTest` now run green together in `apex-core`.
- `DatabaseDataSourceConcurrencyTest`, `FileSystemDataSourceConcurrencyTest`, and `RestApiDataSourceConcurrencyTest` now provide focused coverage for query-versus-shutdown, file-monitor lifecycle, and REST cache/circuit-breaker concurrency paths.
- `ConcurrencyIntegrationTest` and `MixedExternalDataSourceConcurrencyIntegrationTest` now run green together under Maven, covering the broader factory/cache/file interleavings after the production fixes and logging cleanup.
- `InMemoryCacheManager` required production fixes after concurrency tests exposed size drift and lifecycle coordination defects.
- `DataSourceFactory` still warrants lifecycle hardening review around `clearCache()` and `shutdown()`, but the first targeted slice did not reproduce resource-cache corruption.
- `DataSourceMetrics` currently looks acceptable under the tested contention profile; no code change was required in the first slice.
- The current datasource-level slice did not reproduce shutdown, monitor, or circuit-breaker corruption in the targeted database, file-system, and REST paths.
- The combined Maven slice did expose a `FileSystemDataSource` cache-shape race: request-specific lookups could observe a full file-list `List` instead of a filtered record because the same cache key was used for two different value shapes. That path is now fixed.
- `ConcurrencyIntegrationTest` now includes a mixed scenario covering concurrent file queries, cache put/get operations, and `DataSourceFactory.clearCache()` interleavings.

### Latest Execution Update

Completed since the previous revision:
- Logging and observability cleanup is now applied across the external-data-source concurrency surface in `InMemoryCacheManager`, `FileSystemDataSource`, `RestApiDataSource`, and `DatabaseDataSource`.
- The logging pass removed noisy pseudo-trace `info` messages, promoted lifecycle transitions to clearer `info` logs, and standardized contextual `debug` and `error` logs around cache keys, queries, endpoints, file paths, connection summaries, shutdown, and refresh paths.
- The logging cleanup was validated with focused Maven slices for the database/file/REST concurrency classes and with the broader integration slice for `ConcurrencyIntegrationTest` plus `MixedExternalDataSourceConcurrencyIntegrationTest`.

Latest validation commands and outcomes:
- `mvn -pl apex-core "-Dtest=DatabaseDataSourceConcurrencyTest,FileSystemDataSourceConcurrencyTest,RestApiDataSourceConcurrencyTest" test` -> green.
- `mvn -pl apex-core "-Dtest=ConcurrencyIntegrationTest,MixedExternalDataSourceConcurrencyIntegrationTest" test` -> green.
- `mvn -pl apex-core "-Dtest=DataSourceFactoryConcurrencyTest,InMemoryCacheManagerConcurrencyTest,DatabaseDataSourceConcurrencyTest,FileSystemDataSourceConcurrencyTest,RestApiDataSourceConcurrencyTest,ConcurrencyIntegrationTest,MixedExternalDataSourceConcurrencyIntegrationTest,DataSourceRegistryGetOrCreateTest,ConfigurationContextTest,ResultThreadSafetyTest,ScenarioConcurrentAccessTest,RulesEngineConcurrentEvaluationTest" test` -> green (`Tests run: 91, Failures: 0, Errors: 0`, `BUILD SUCCESS`).

Validation caveat:
- The VS Code Java test runner produced false-negative failures for several concurrency classes because of module reflection access restrictions (`InaccessibleObjectException` / `AccessibleObject.checkAccess`). Those failures were runner-specific and not reproduced under Maven. Maven remains the source of truth for this concurrency slice.

Current state:
- Shared cache/lifecycle hardening defects identified so far have been addressed.
- Targeted datasource concurrency tests are green.
- Integration concurrency coverage is green.
- Broader curated apex-core concurrency coverage is green under Maven.
- Remaining work, if we continue this thread, is incremental hardening or additional stress coverage rather than a known failing correctness issue.

Concrete checks:
- `DataSourceFactory`: verify whether direct `createDataSource()` is intentionally allowed to create duplicate instances, since the class documentation already says deduplication belongs in `DataSourceRegistry`.
- `InMemoryCacheManager`: verify whether `currentSize` can drift from `cache.size()` under heavy `put`, `remove`, `get(expired)`, and `clear` interleavings.
- `DataSourceMetrics`: verify whether aggregate counts, min, max, and average remain logically consistent after high-volume concurrent updates.
- `DatabaseDataSource`, `FileSystemDataSource`, `RestApiDataSource`: verify whether their cache layers and mutable state remain consistent during concurrent reads and shutdown/clear paths.

Exit criteria:
- Every item in the “Concurrent Access Concerns Identified” section is tagged as `confirmed`, `not reproducible`, or `needs hardening despite no repro`.

## Phase 1: Test-First Hardening of Shared Infrastructure

### 1. DataSourceFactory

Target class:
- `DataSourceFactory`

What to verify:
- Singleton access remains correct under heavier contention than the current 50-thread test.
- Concurrent `registerProvider()` and `unregisterProvider()` calls do not corrupt visibility or supported-type queries.
- `clearCache()` is safe when concurrent resource creation is in flight.
- Existing `computeIfAbsent` use for JDBC and HTTP caches remains safe when creation races with `clearCache()` and `shutdown()`.

Existing tests to extend:
- `DataSourceFactoryTest`
- `ConcurrencyIntegrationTest`

New tests to add:
- `DataSourceFactoryConcurrencyTest`

Required test cases:
- `shouldReturnSingleFactoryInstanceUnderHighContention` with 100 threads and 1000 accesses per thread.
- `shouldHandleConcurrentProviderRegistrationAndRemoval` with repeated register/remove/read cycles.
- `shouldNotCorruptResourceCachesDuringConcurrentCreateAndClear` using coordinated latches.
- `shouldPreserveEquivalentJdbcOrHttpResourceReuseWhenNoClearOccurs` to verify current `computeIfAbsent` behavior remains intact.

Implementation options:
- Do not redesign the factory cache path first; it already uses `computeIfAbsent` for JDBC and HTTP resources.
- If `clearCache()` races with creation, add a lifecycle guard or narrow write lock around cache teardown only.
- Do not add data-source instance deduplication here unless it is explicitly required; that responsibility appears to belong to `DataSourceRegistry`.

Specific code paths to cover:
- `clearCache()` clearing `jdbcDataSourceCache` and `httpClientCache` without coordination.
- `shutdown()` clearing provider registrations while callers may still query `getSupportedTypes()` or `isCustomTypeSupported()`.

Exit criteria:
- No cache corruption or unexpected exception during create/clear or create/shutdown races.
- Equivalent configurations reuse JDBC/HTTP cached resources when no explicit clear occurs.
- No exceptions or leaked resources during create/clear races.
- Singleton and provider tests pass reliably across repeated runs.

### 2. InMemoryCacheManager

Target class:
- `InMemoryCacheManager`

What to verify:
- `currentSize` never becomes negative and never diverges from actual key count after concurrent operations.
- `put()` does not overshoot `maxSize` under contention.
- LRU eviction decisions are consistent enough to preserve bounded size.
- Expired entry removal from `get()` and `containsKey()` does not double-decrement size.
- `clear()` and background cleanup do not race with active readers/writers in a way that leaves stale stats or inconsistent size accounting.
- `shutdown()` does not leave a running cleanup executor or permit post-shutdown background mutation.

Existing tests to extend:
- `InMemoryCacheManagerTest`

New tests to add:
- `InMemoryCacheManagerConcurrencyTest`

Required test cases:
- `shouldMaintainConsistentSizeDuringConcurrentPutRemoveAndGet`.
- `shouldRespectMaxSizeUnderConcurrentPuts`.
- `shouldHandleConcurrentExpiryAndReadsWithoutNegativeSize`.
- `shouldClearSafelyDuringActiveOperations`.
- `shouldKeepStatisticsMonotonicUnderConcurrentAccess`.

Implementation options:
- Keep the current read/write lock design only if tests show it preserves invariants.
- If size drift appears, centralize all size transitions behind successful map mutations.
- If LRU eviction is non-deterministic but bounded, accept approximate LRU and document that behavior.
- If cleanup races with operations, add a shutdown flag and prevent executor overlap during teardown.

Specific code paths to cover:
- `put()` lock upgrade from read to write around `evictLRUInternal()`.
- `evictExpired()` iterator removal and `currentSize.decrementAndGet()`.
- `get()` and `containsKey()` removing expired entries with `cache.remove(key, entry)`.
- `clear()` resetting `currentSize` without taking the eviction lock.
- `shutdown()` stopping `cleanupExecutor` and then clearing internal state.

Exit criteria:
- `currentSize == cache.size()` after quiescence in all concurrency tests.
- No negative counts.
- Cache never remains above `maxSize` after competing operations settle.
- `shutdown()` reliably terminates background cleanup and leaves the manager in a quiescent state.

### 3. DataSourceMetrics

Target class:
- `DataSourceMetrics`

What to verify:
- Total requests equals successful plus failed requests after concurrent updates.
- Min and max response time remain within the submitted value range.
- Average response time is mathematically consistent with total time and total requests.
- Reset behavior is safe under concurrent update pressure if reset is used in production.
- `lastRequestTime` and `lastResetTime` remain plausible under interleaved update and reset calls.

Existing tests to extend:
- `DataSourceMetricsTest`

Required test cases:
- `shouldTrackConcurrentSuccessfulAndFailedRequestsWithoutLostCounts`.
- `shouldPreserveMinAndMaxUnderConcurrentUpdates`.
- `shouldKeepCacheAndConnectionCountersAccurateUnderParallelUpdates`.
- `shouldResetSafelyWhenConcurrentUpdatesArePresent` if reset is reachable in normal runtime paths.

Implementation options:
- Current CAS loops for min/max may be sufficient; the likely gap is proof, not necessarily code.
- Consider `LongAdder` for heavily contended additive counters if benchmarks show contention.
- Keep `AtomicLong` if tests pass and there is no measurable bottleneck.

Specific code paths to cover:
- `recordSuccessfulRequest()` and `recordFailedRequest()` interleaving with `reset()`.
- `recordResponseTime()` CAS loops for min and max.

Exit criteria:
- No lost increments.
- Min/max and totals remain logically valid in repeated high-contention runs.
- If `reset()` is not made linearizable, tests and documentation must explicitly state the weaker guarantee.

## Phase 2: Data Source Implementation Hardening

### 4. DatabaseDataSource

Target class:
- `DatabaseDataSource`

Observations from current code:
- Uses `EnhancedCacheManager` for result caching.
- Maintains mutable `configuration`, `connectionStatus`, `metrics`, and `healthIndicator` fields.
- Keeps `preparedQueries` in a `ConcurrentHashMap`, but query execution and cache population are multi-step.

What to verify:
- Cache read-miss-populate sequences do not create invalid state or duplicate side effects.
- Query execution remains correct during concurrent access to the same named query.
- Shutdown or reinitialize paths do not race with active queries.
- Metrics remain monotonic and consistent when cache hits and DB hits interleave.

New tests to add:
- `DatabaseDataSourceConcurrencyTest`

Required test cases:
- `shouldHandleConcurrentGetDataCallsForSameQuery`.
- `shouldHandleConcurrentQueryAndShutdown`.
- `shouldNotCorruptMetricsDuringParallelDatabaseAccess`.

Implementation options:
- Use a cache-level atomic loader or per-key synchronization only if duplicate expensive executions become a real issue.
- If lifecycle races appear, add a closed/running state guard and fail fast once shutdown begins.

Specific code paths to cover:
- `getData()` cache miss then `executeQuery()` then `cacheManager.put()`.
- `query()` using JDBC resources while other threads call `shutdown()`.

Exit criteria:
- Parallel reads are stable and metrics remain consistent.
- Shutdown does not produce partial corruption or hanging threads.

### 5. FileSystemDataSource

Target class:
- `FileSystemDataSource`

Observations from current code:
- `fileModificationTimes` is concurrent, but `dataLoaders` is a plain `HashMap` populated during initialization.
- Has a scheduled file monitor and mutable monitoring state.

What to verify:
- Post-construction access to `dataLoaders` is read-only and therefore safe.
- File monitoring start/stop does not race with data loading.
- Cache invalidation on file changes does not conflict with active reads.
- `connectionStatus` transitions remain valid when monitor startup and shutdown overlap.

New tests to add:
- `FileSystemDataSourceConcurrencyTest`

Required test cases:
- `shouldServeConcurrentReadsWhileMonitoringIsActive`.
- `shouldHandleConcurrentFileChangeDetectionAndGetData`.
- `shouldShutdownFileMonitoringCleanlyUnderLoad`.

Implementation options:
- If monitoring races appear, guard executor lifecycle transitions with synchronization.
- If invalidation and reads race, move invalidation to atomic cache replacement or narrow locks around watcher-triggered refresh.

Specific code paths to cover:
- `startFileMonitoring()` creating and scheduling `fileMonitorExecutor`.
- `shutdown()` stopping monitoring and updating connection status.
- any path that updates `fileModificationTimes` while concurrent reads occur.

Exit criteria:
- No executor leaks.
- No read failures or stale-state corruption during monitoring activity.

### 6. RestApiDataSource

Target class:
- `RestApiDataSource`

Observations from current code:
- Shared mutable state includes `configuration`, `connectionStatus`, `metrics`, `circuitBreaker`, and cache manager.
- `HttpClient` itself is thread-safe, so risk is around local state and cache/circuit-breaker interaction.

What to verify:
- Cache miss/populate paths behave correctly under concurrent identical requests.
- Circuit breaker state changes are safe under concurrent failures.
- Interrupted requests do not leave inconsistent metrics or breaker state.
- Successful and failed HTTP calls update metrics totals without drift.

New tests to add:
- `RestApiDataSourceConcurrencyTest`

Required test cases:
- `shouldHandleConcurrentCachedApiReads`.
- `shouldUpdateCircuitBreakerSafelyUnderConcurrentFailures`.
- `shouldRecordMetricsConsistentlyDuringParallelSuccessAndFailureMix`.

Implementation options:
- Add per-key request coalescing only if duplicate outbound calls are shown to be materially harmful.
- If circuit breaker is not internally thread-safe, harden it before changing this class.

Specific code paths to cover:
- `getData()` cache miss and response caching.
- `query()` with concurrent success and failure responses.
- any circuit-breaker transition path exercised by repeated failures.

Exit criteria:
- No inconsistent breaker state.
- Cache and metric invariants hold under mixed success/failure load.

## Phase 3: System-Level Concurrency Regression Suite

Target test class to extend:
- `ConcurrencyIntegrationTest`

New scenarios to add:
- Factory create, query, and clear-cache race scenario using mixed data-source types.
- Shared cache manager under mixed TTL expiry and explicit removals.
- End-to-end file or REST-backed lookup bursts with validation of result consistency and metrics totals.
- Repeated test execution wrapper to catch flaky timing-dependent failures.

Test design requirements:
- Use `CountDownLatch`, `CyclicBarrier`, or `Phaser` to coordinate deterministic contention.
- Prefer asserting invariants after quiescence instead of relying on exact interleavings.
- Keep time-based tests generous enough for CI but short enough for local iteration.
- Follow existing repo stress-test style where appropriate, for example `@RepeatedTest(3)` or `@RepeatedTest(5)` on high-contention scenarios instead of building custom retry loops into production code.

Command matrix:
- Fast targeted run for shared infrastructure: `mvn -pl apex-core -Dtest=DataSourceFactoryTest,DataSourceMetricsTest,InMemoryCacheManagerTest,ConcurrencyIntegrationTest test`
- Focused concurrency class run once added: `mvn -pl apex-core -Dtest=DataSourceFactoryConcurrencyTest,InMemoryCacheManagerConcurrencyTest,DatabaseDataSourceConcurrencyTest,FileSystemDataSourceConcurrencyTest,RestApiDataSourceConcurrencyTest test`
- Re-run a suspected flaky class locally: `mvn -pl apex-core -Dtest=DataSourceFactoryConcurrencyTest test`

Test grouping rule:
- Keep deterministic correctness tests in normal `test` scope.
- Put slower exploratory stress tests in separate classes or clearly named repeated tests so they can be isolated without disabling core coverage.

Exit criteria:
- A single command can run the full external-data-source concurrency suite repeatedly without intermittent failures.

## Phase 4: YAML Lookup and Integration Coverage Backlog

The note also identifies a larger data lookup testing gap. That work should follow the concurrency hardening, because these tests will depend on stable underlying components.

Targets:
- YAML-driven lookup wiring in `apex-core`
- Demo-backed end-to-end scenarios in `apex-demo`
- Registry and multi-source fallback paths where external data sources are composed

Recommended test additions:
- YAML-to-runtime lookup integration tests covering database, filesystem, cache, and REST sources.
- Multi-source failover tests using cache-first then database or API fallback.
- Enrichment chain tests where multiple lookups feed business rules.
- Error-handling tests for timeouts, unavailable sources, and malformed payloads.

Non-goal for the first concurrency sprint:
- Do not mix this larger integration backlog into the first cache/factory hardening PR. Keep it as a follow-on stream.

## PR Breakdown

Recommended pull-request sequence:

1. `PR-1`: Add concurrency repro tests only.
2. `PR-2`: Fix `InMemoryCacheManager` invariants and any immediately exposed metrics issues.
3. `PR-3`: Harden `DataSourceFactory` resource-cache behavior and lifecycle races.
4. `PR-4`: Harden `DatabaseDataSource`, `FileSystemDataSource`, and `RestApiDataSource` lifecycle and cache interaction.
5. `PR-5`: Add broader YAML lookup integration and failover coverage.

Review gate for each PR:
- Must include at least one failing or gap-exposing test before the fix in the same PR branch history.
- Must document any intentionally accepted non-linearizable behavior.
- Must avoid opportunistic refactors outside the targeted work package.

Rationale:
- This keeps evidence separate from fixes.
- It reduces the chance of masking bugs with large refactors.
- It makes regressions easier to attribute.

## Definition of Done

The concurrency work is done when all of the following are true:

1. Every concern in this document is either reproduced and fixed, or explicitly closed with evidence.
2. The external data-source concurrency test suite passes repeatedly without flakiness.
3. Cache-size, metrics-total, and lifecycle invariants are asserted in automated tests.
4. Any accepted non-determinism, such as approximate LRU behavior, is documented in code comments or test names.
5. No new public API is introduced unless there is a clear functional requirement.

## Immediate Next Step

`PR-1` is complete for the shared infrastructure slice: the dedicated concurrency tests were added, `InMemoryCacheManager` defects were reproduced and fixed, and the first risk-matrix statuses are now populated.

Concrete next checklist for `PR-4` preparation:
- Add `DatabaseDataSourceConcurrencyTest` focused on `getData()` and `query()` races against `shutdown()`.
- Add `FileSystemDataSourceConcurrencyTest` focused on monitor lifecycle, cache invalidation, and concurrent reads.
- Add `RestApiDataSourceConcurrencyTest` focused on cache population, circuit-breaker transitions, and mixed success/failure metrics.
- Extend `ConcurrencyIntegrationTest` only after the three class-level tests are stable and deterministic.




## Current Test Coverage Analysis

## Latest Follow-On Execution Update

- Added `ExternalDataSourceReferenceLookupIntegrationTest` in `apex-core` to cover the previously missing end-to-end path of:
    - `RulesEngine.fromFile(...)`
    - external `data-source-refs` resolution
    - `lookup-enrichment` execution
    - `query-ref` resolution against an external data-source config
- This closes the architectural gap between:
    - file-system/reference-resolution tests that stop before runtime lookup execution, and
    - lookup-enrichment runtime tests that only use inline `data-sources`
- Validation result:
    - `mvn -pl apex-core -Dtest=ExternalDataSourceReferenceLookupIntegrationTest test`
    - `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
    - `BUILD SUCCESS`
- Useful implementation note confirmed by this slice:
    - In the `RulesEngine`/`DataSourceRegistry` H2 runtime path, the external datasource config must use `connection.database` rather than `connection.url`; the registry/JDBC factory synthesizes the H2 JDBC URL from H2-specific connection fields.

I've identified several areas where test coverage for data services and data lookup functionality can be improved:

### **Existing Test Coverage (Good)**
- **Basic DataSource implementations** - CustomDataSourceTest (32 tests)
- **LookupServiceRegistry** - LookupServiceRegistryTest (comprehensive)
- **YamlDataSource configuration** - YamlDataSourceTest (extensive Map-based config tests)
- **External data source integration** - ExternalDataSourceIntegrationTest (8 tests)
- **Demo data services** - DataServiceManagerTest, MockDataSourceTest

### **Test Coverage Gaps (Need Improvement)**
- X **YAML-to-runtime data lookup integration** - Limited end-to-end testing
- X **Multi-data source lookup scenarios** - No comprehensive failover testing
- X **Data enrichment workflows** - Missing complex lookup chain testing
- X **Performance under load** - No performance benchmarks for lookup operations
- X **Error handling in lookup chains** - Limited error scenario coverage

## Follow-On Test Implementation Plan

### **Phase 1: YAML Data Lookup Integration Tests**
Create tests that validate YAML configurations translate correctly to working data lookup functionality:
- Database query execution from YAML configs
- File-based lookup operations (CSV, JSON)
- Cache-based lookup with TTL and eviction
- REST API lookup with authentication
- Parameter binding and query execution

### **Phase 2: Data Service Manager Lookup Tests**
Build comprehensive tests for DataServiceManager lookup operations:
- Multi-source lookup with prioritization
- Failover scenarios when primary sources fail
- Data source health monitoring during lookups
- Concurrent lookup operations
- Memory and resource management

### **Phase 3: End-to-End Scenario Tests**
Develop real-world scenario tests using the YAML examples:
- User enrichment workflows (cache → database → API)
- Session management with cache fallback
- Configuration-driven data processing
- Multi-step data transformation pipelines

### **Phase 4: Performance and Error Handling**
Create robust tests for production scenarios:
- Load testing with concurrent lookups
- Memory usage under high volume
- Network failure simulation
- Invalid configuration handling
- Data source unavailability scenarios

## Key Test Areas to Focus On

1. **YAML Configuration Validation**
    - Verify all YAML data source types can be loaded and executed
    - Test parameter binding and query execution
    - Validate connection pooling and caching configurations

2. **Data Lookup Chain Testing**
    - Test complex lookup scenarios from the mixed-example.yaml
    - Validate cache-first, database-fallback patterns
    - Test data enrichment from multiple sources

3. **Integration with Lookup Services**
    - Test LookupServiceRegistry with YAML-configured services
    - Validate lookup service discovery and execution
    - Test lookup service chaining and composition

4. **Error Handling and Resilience**
    - Test circuit breaker patterns
    - Validate retry logic and timeout handling
    - Test graceful degradation when sources are unavailable

Recommended sequencing for this follow-on stream:

1. Finish the core concurrency hardening work above.
2. Add YAML-to-runtime lookup integration coverage.
3. Add multi-source failover and enrichment-chain scenarios.
4. Add performance and resilience scenarios once correctness coverage is stable.


5. API Layer - Partial Coverage
RulesService - No dedicated test file (instance-based service)

