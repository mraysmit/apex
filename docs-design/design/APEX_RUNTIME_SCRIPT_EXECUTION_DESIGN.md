# APEX Runtime Script Execution Design

## Summary
Enable dynamic, runtime-loaded Groovy (or JVM script) functions callable from SpEL expressions in APEX YAML, without requiring application restart.

Primary expression pattern:
- `#script('risk-score', #trade)`

This introduces one controlled SpEL entrypoint instead of exposing arbitrary bean/method access.

Status:
- **IMPLEMENTED** — All 10 steps complete. See `dev.mars.apex.core.script` package.
- `#script(...)` is a registered SpEL bridge function available in all APEX evaluation contexts.

## Goals
- Load scripts from external locations at runtime.
- Compile and cache scripts for performance.
- Support hot reload when script files change.
- Keep YAML authoring simple and stable.
- Add strong safety controls (allowlists, timeout, audit).

## Non-Goals
- Full arbitrary script sandboxing across all JVM capabilities.
- General-purpose remote code execution endpoint.
- Replacing existing Java-based rule/enrichment paths.

## Why This Is Required

Dynamic runtime script execution cannot be achieved by plain SpEL syntax alone in a reliable way for APEX. The required pieces below are not optional if we want runtime loading, reload, and safe operation.

### 1) SpEL Reachability Limitation

SpEL can only invoke:
- variables already present in `StandardEvaluationContext`
- explicitly registered functions
- static methods via `T(...)`
- beans (only when a bean resolver is configured)

Runtime script files on disk are not automatically reachable through any of these paths. A bridge is required to map a script ID in expression text to an executable function.

### 2) Runtime Discovery and Versioning

Without a registry, there is no deterministic answer to:
- where scripts are loaded from
- which version/checksum is active
- whether a script is enabled/disabled

Using direct class calls (`T(com.example.X)`) would force compile-time binding and restart for updates, which violates runtime dynamism.

### 3) Reload Lifecycle and Consistency

Runtime loading needs stateful behavior SpEL does not provide:
- detect file changes
- invalidate only changed compiled scripts
- preserve last known good version when a new script has compile errors
- avoid race conditions between evaluation threads and reload operations

These concerns require a dedicated reload manager and compilation cache.

### 4) Parameter and Return Type Contract

SpEL invocation text does not define a system-wide contract for script entrypoints. We need explicit rules for:
- positional and named arguments
- numeric coercion behavior
- map/list/scalar return value handling
- null handling and exception translation

Without a single bridge contract, each caller path (rules, enrichments, transformations) would implement incompatible behavior.

### 5) Safety, Governance, and Auditability

Dynamic scripting introduces operational risk. A controlled execution path is required for:
- script allowlist enforcement
- base-path restrictions
- timeouts/circuit breaker policy
- invocation and reload audit logs

Allowing ad hoc direct method invocation from expressions would make policy enforcement fragmented and difficult to audit.

### 6) Alignment with Existing APEX Error Recovery

APEX already has severity-based recovery semantics. Runtime script failures must be normalized into the same model so existing fail-fast/continue behavior remains consistent.

This normalization belongs in one integration layer, not in each expression site.

### Alternative Considered and Rejected

Alternative: expose all script engine internals directly to SpEL variables and call methods ad hoc.

Rejected because it causes:
- weak governance (no central allowlist or timeout policy)
- inconsistent argument/return behavior
- no clean reload/version model
- higher risk of accidental unsafe method exposure

Conclusion:
- A dedicated script bridge + registry + cache/reload lifecycle is required to satisfy runtime dynamism, safety, and predictable behavior.

## Proposed Architecture

### 1) Script Registry
Tracks script metadata and source content.

Responsibilities:
- Resolve script by ID from configured locations (filesystem first, optional classpath fallback).
- Store metadata: `id`, `path`, `checksum`, `lastModified`, `enabled`, `version`.
- Expose `getScript(id)` and `refresh()`.

### 2) Script Compiler/Executor
Compiles scripts and executes a standard function signature.

Responsibilities:
- Compile script source (GroovyShell or JSR-223 engine).
- Cache compiled artifact by `id + checksum`.
- Invoke standard entrypoint, for example `run(Map payload)`.
- Enforce execution timeout.

### 3) SpEL Bridge Function
Register one function in `StandardEvaluationContext`:
- Function name: `script`
- Signature: `script(String scriptId, Object payload)`

Implementation status:
- Proposed API for this feature; current codebase does not yet register this function.

Add an extended signature for multi-parameter and named function support:
- `script(String scriptId, String functionName, Object... args)`

Bridge API behavior:
- If `functionName` omitted, invoke default `run(...)` function.
- If `functionName` provided, invoke that exact Groovy function.
- Preserve argument order from SpEL call into Groovy function call.
- Return raw function result to SpEL (no stringification).

YAML usage examples:
- `condition: "#script('eligibility-check', #data) == true"`
- `expression: "#script('risk-score', #trade)"`

Additional usage examples:
- `condition: "#script('cp-risk', 'isEligible', #counterpartyId, #notional, #currency)"`
- `expression: "#script('fx-utils', 'pipValue', #pair, #notional, #price)"`

## Groovy Function Contracts

### Standard Script File Shape

Each script exports one or more functions. The default function is `run`.

```groovy
// file: risk-score.groovy
BigDecimal run(Map payload) {
    BigDecimal notional = (payload.notional ?: 0) as BigDecimal
    String rating = (payload.creditRating ?: 'B').toString()
    BigDecimal base = rating in ['AAA', 'AA'] ? 70 : 40
    return base + (notional > 1_000_000 ? 10 : 0)
}

boolean isEligible(String counterpartyId, BigDecimal notional, String currency) {
    return counterpartyId?.startsWith('CP') && notional > 0 && currency in ['USD', 'EUR', 'GBP']
}

Map settlementInstruction(Map trade, Map contextVars) {
    def previousPass = contextVars.ruleResults?.get('sanctions-check') == true
    return [
        method: previousPass ? 'DVP' : 'MANUAL_REVIEW',
        reason: previousPass ? 'auto-approved' : 'blocked-by-prior-rule'
    ]
}
```

### Parameter Mapping Rules

- SpEL args are passed positionally to Groovy functions.
- APEX maps Java values directly (Map, List, String, BigDecimal, Boolean).
- Numeric values are converted to `BigDecimal` before invocation when target parameter type is numeric.
- A function may optionally receive a final `contextVars` Map injected by bridge.

### Return Value Rules

- `boolean` return: usable directly in `condition` expressions.
- `number` return: usable in numeric comparisons and calculations.
- `Map` return: fields accessible from SpEL via property/map access.
- `List` return: usable in collection filters/projections.

## SpEL Context Interaction Model

Bridge injects an immutable context map into Groovy when function signature accepts it:

```text
contextVars = {
  facts: <current input/enriched data map>,
  ruleResults: <#ruleResults if present>,
  ruleGroupResults: <#ruleGroupResults if present>,
  now: <Instant.now()>,
  scriptId: <current script id>
}
```

This allows script logic to interact with current APEX state without direct engine coupling.

## End-to-End Examples (APEX YAML + Groovy)

### Example 1: Boolean decision from Groovy

```yaml
rules:
  - id: "cp-eligibility"
    condition: "#script('risk-score', 'isEligible', #counterpartyId, #notional, #currency)"
    message: "Counterparty is eligible"
    severity: "ERROR"
```

### Example 2: Numeric score from Groovy

```yaml
transformations:
  - id: "calc-risk-score"
    expression: "#script('risk-score', #data)"
    target-field: "riskScore"
```

### Example 3: Map return with prior rule interaction

```yaml
enrichments:
  - id: "settlement-strategy"
    type: "field-enrichment"
    condition: "#script('risk-score', 'isEligible', #counterpartyId, #notional, #currency)"
    field-mappings:
      - source-field: "#script('risk-score', 'settlementInstruction', #data, {'ruleResults': #ruleResults})['method']"
        target-field: "settlement.method"
      - source-field: "#script('risk-score', 'settlementInstruction', #data, {'ruleResults': #ruleResults})['reason']"
        target-field: "settlement.reason"
```

Note: for performance, the bridge should support single-call memoization per evaluation cycle when same function+args repeats.

### 4) Hot Reload Manager
Keeps registry/cache current while system is running.

Options:
- Polling (default): periodic scan for checksum/mtime changes.
- Watch service (optional): file-change notifications where reliable.

Behavior:
- On change: invalidate only impacted cache entry.
- On compile failure: keep last good compiled version active.

## Configuration
Add runtime script settings in engine configuration:

```yaml
runtime-scripts:
  enabled: true
  locations:
    - "./config/scripts"
  engine: "groovy"
  polling-interval-ms: 5000
  execution-timeout-ms: 200
  allowlist:
    - "risk-score"
    - "eligibility-check"
  fail-mode: "use-last-good" # or "fail-fast"
```

## Execution Flow
1. SpEL calls `#script('risk-score', #trade)`.
2. Bridge validates script ID against allowlist.
3. Registry resolves script and metadata.
4. Compiler cache returns compiled artifact (or compiles if stale/missing).
5. Executor runs `run(payload)` with timeout.
6. Result returns to SpEL expression.
7. Invocation is logged with script ID/version and duration.

## Safety and Governance
- Script allowlist required in production.
- Deny loading scripts outside configured base paths.
- Timeouts per invocation.
- Optional per-script max execution count / circuit breaker.
- Audit events: load, reload, compile fail, invocation fail, timeout.
- Restrict bridge to explicit exported function names only (no reflective method traversal).
- Pass `contextVars` as read-only map wrapper.

## Error Handling
- Script missing: return controlled APEX error.
- Compile error on reload:
  - `use-last-good`: continue using previous compiled artifact.
  - `fail-fast`: mark script unavailable and fail evaluations.
- Runtime exception: return expression error (severity governed by existing APEX error recovery policies).

## Integration Points in APEX
- Register SpEL function during context creation in `ExpressionEvaluatorService`.
- Wire runtime script services from `RulesEngine` initialization path.
- Reuse existing error recovery and metrics conventions.

## Testing Strategy
- Unit tests:
  - registry resolution and allowlist enforcement
  - cache hit/miss and checksum invalidation
  - timeout behavior
  - fail-mode behavior (`use-last-good`, `fail-fast`)
  - positional parameter mapping and numeric coercion
  - return type mapping (`boolean`, `BigDecimal`, `Map`, `List`)
  - contextVars visibility (`facts`, `ruleResults`, `ruleGroupResults`)
- Integration tests:
  - YAML rule/enrichment calling `#script(...)`
  - live script file update reflected without restart
  - script function invocation with multiple parameters and named functions
- Concurrency tests:
  - parallel evaluations while script reload occurs

## Rollout Plan
Phase 1 (MVP)
- Local filesystem loading
- Polling-based reload
- Groovy engine support
- `#script(...)` function registration

Phase 2
- Optional watch-service reload
- richer observability and per-script metrics
- optional signature verification for script artifacts

## Open Decisions
- Preferred script contract:
  - `run(Map payload)` only, or named exported methods?
- Default fail mode in production:
  - `use-last-good` vs `fail-fast`
- Whether to support multiple script engines initially (Groovy only vs extensible SPI)

## Minimum Viable Implementation Decisions (Iteration 1)

To remove ambiguity and start coding immediately, Iteration 1 will use the following fixed decisions.

### Fixed API Surface
- Register one SpEL bridge function named `script`.
- Support two call forms:
  - `#script(scriptId, payload)` -> invokes `run(Map payload)`
  - `#script(scriptId, functionName, arg1, arg2, ...)` -> invokes named exported function

### Script Contract
- Script file extension: `.groovy`.
- Required default function for short form: `run(Map payload)`.
- Named function calls allowed only for explicitly exported top-level functions.
- Optional final parameter `Map contextVars` supported for context interaction.

### Runtime Source and Reload
- Source: filesystem only (`runtime-scripts.locations`).
- Reload mode: polling only in Iteration 1.
- Cache key: `scriptId + checksum`.
- Reload behavior on compile failure: keep last known good compiled version.

### Type and Error Behavior
- Argument passing: positional.
- Numeric coercion target: `BigDecimal` for numeric parameters where applicable.
- Return values passed through as-is to SpEL (`Boolean`, `Number`, `Map`, `List`, `String`, `null`).
- Invocation exception handling: convert to APEX evaluation error and route through existing severity/error-recovery behavior.

### Safety Defaults
- Enforce script allowlist when configured (recommended enabled by default in production profiles).
- Enforce base-path restriction for script file resolution.
- Per-invocation timeout required (`execution-timeout-ms`).
- Audit log at INFO for load/reload/invoke failures; DEBUG for successful invocations.

### Not in Iteration 1
- `@beanName` SpEL bean resolver support.
- WatchService-based live file notifications.
- Multi-engine plugin SPI.
- Remote script sources.

### Acceptance Criteria
- YAML rule condition can call Groovy boolean function and pass/fail deterministically.
- YAML transformation can store numeric return from Groovy function.
- YAML enrichment can consume `Map` return fields from Groovy function.
- Updating a script file changes behavior without application restart after polling interval.
- If updated script fails compilation, previous compiled version remains active and evaluation does not regress unexpectedly.

---

## Implementation Plan

### Prerequisites
- Add `groovy` (or `groovy-jsr223`) dependency to `apex-core/pom.xml`.
- Confirm Java 21 compatibility with chosen Groovy version (recommend Groovy 4.x).

### Step 1: Script Model and Configuration (no runtime behavior)

**Goal**: Define the data model and YAML configuration parsing for runtime scripts.

New classes:
- `dev.mars.apex.core.config.model.YamlRuntimeScriptConfig` — YAML binding for the `runtime-scripts` block (`enabled`, `locations`, `engine`, `polling-interval-ms`, `execution-timeout-ms`, `allowlist`, `fail-mode`).
- `dev.mars.apex.core.script.ScriptMetadata` — immutable record holding `id`, `path`, `checksum`, `lastModified`, `enabled`, `version`.

Changes:
- `YamlRuleConfiguration` — add optional `runtime-scripts` field mapped to `YamlRuntimeScriptConfig`.

Tests:
- Unit test: parse a YAML file containing a `runtime-scripts` block and verify all fields bind correctly.
- Unit test: `YamlRuleConfiguration` without `runtime-scripts` still loads normally (backward compat).

**Done when**: YAML round-trip test passes; no runtime behavior yet.

### Step 2: Script Registry

**Goal**: Resolve script files from configured filesystem locations, track metadata, and enforce the allowlist.

New class:
- `dev.mars.apex.core.script.RuntimeScriptRegistry`
  - `loadScripts(List<String> locations, List<String> allowlist)` — scan directories, build `ScriptMetadata` map keyed by script ID (filename without extension).
  - `getScript(String id)` — return metadata or throw controlled error if missing/not-allowed.
  - `refresh()` — re-scan, detect changed checksums, return set of changed IDs.
  - Enforce: script ID must be on allowlist (when allowlist is non-empty). Script path must be under a configured base location (no path traversal).

Tests:
- Unit test: load scripts from a temp directory, verify metadata.
- Unit test: allowlist enforcement — script present on disk but not in allowlist → controlled error.
- Unit test: path traversal attempt (e.g., `../../etc/passwd`) → rejected.
- Unit test: `refresh()` detects file change (write new content, verify checksum differs).
- Unit test: `getScript()` for missing ID → controlled error with clear message.

**Done when**: registry resolves scripts by ID with allowlist and path safety enforced; no compilation yet.

### Step 3: Script Compiler and Cache

**Goal**: Compile Groovy scripts, cache compiled artifacts, and invalidate on checksum change.

New class:
- `dev.mars.apex.core.script.GroovyScriptCompiler`
  - `compile(ScriptMetadata meta)` — compile source with `GroovyShell`, return compiled `Script` (or `Class`).
  - Internal cache: `ConcurrentHashMap<String, CompiledScript>` keyed by `id + checksum`.
  - `invalidate(String scriptId)` — remove cached entry.
  - `getOrCompile(ScriptMetadata meta)` — cache-hit returns immediately; cache-miss compiles and stores.

Compile failure handling:
- If `fail-mode` is `use-last-good`: log error, keep previous entry in cache, return it.
- If `fail-mode` is `fail-fast`: remove cache entry, throw compilation error.

Tests:
- Unit test: compile a valid `.groovy` file, invoke `run(Map)`, verify return value.
- Unit test: cache hit — second call does not recompile (verify via compile counter or mock).
- Unit test: checksum change triggers recompilation.
- Unit test: compile error with `use-last-good` → previous version returned.
- Unit test: compile error with `fail-fast` → exception thrown.

**Done when**: Groovy scripts compile, cache, and invalidate correctly; no SpEL integration yet.

### Step 4: Script Executor with Timeout

**Goal**: Invoke a named function on a compiled script with positional arguments, enforce execution timeout.

New class:
- `dev.mars.apex.core.script.ScriptExecutor`
  - `execute(CompiledScript script, String functionName, Object[] args, long timeoutMs)` — invoke function, enforce timeout via `ExecutorService` + `Future.get(timeout)`.
  - Default function: if `functionName` is null, invoke `run`.
  - Numeric coercion: convert numeric args to `BigDecimal` when target parameter type is numeric.
  - Return raw result (`Boolean`, `Number`, `Map`, `List`, `String`, `null`).
  - On timeout: cancel execution, throw controlled error.

Tests:
- Unit test: invoke `run(Map)` → returns expected value.
- Unit test: invoke named function with positional args → returns expected value.
- Unit test: invoke function that exceeds timeout → controlled timeout error.
- Unit test: numeric coercion — pass `int` to `BigDecimal` parameter.
- Unit test: null return handled gracefully.

**Done when**: executor invokes functions with timeout enforcement; not yet wired to SpEL.

### Step 5: SpEL Bridge Function Registration

**Goal**: Register `#script(...)` as a callable function in `StandardEvaluationContext`.

Changes:
- `ExpressionEvaluatorService`
  - Add field: `RuntimeScriptRegistry` (optional, nullable — keeps backward compat when scripts not configured).
  - New method: `scriptBridge(String scriptId, Object... args)` — delegates to registry → compiler → executor pipeline.
  - In `createEvaluationContext()`: if registry is present, call `context.registerFunction("script", ...)` pointing to `scriptBridge`.
- `EnrichmentProcessor.createEvaluationContext()` — inherits registration automatically because it delegates to `evaluatorService.createEvaluationContext()`.

Bridge method dispatch:
- 1 arg after scriptId → treat as `run(Map payload)`.
- 2+ args after scriptId where first is `String` → treat as `(functionName, arg1, arg2, ...)`.
- Inject `contextVars` map as final argument if target function signature accepts it.

Tests:
- Integration test: YAML rule with `condition: "#script('test-script', #data)"` evaluates correctly.
- Integration test: YAML rule with `condition: "#script('test-script', 'isEligible', #id, #amount)"` evaluates correctly.
- Integration test: context without runtime scripts configured → `#script(...)` call produces clear error, not NPE.
- Unit test: verify `registerFunction` is called when registry present, not called when absent.

**Done when**: `#script(...)` works end-to-end in a YAML rule condition.

### Step 6: Polling Reload Manager

**Goal**: Periodically detect script file changes and refresh the registry/cache.

New class:
- `dev.mars.apex.core.script.ScriptReloadManager`
  - Constructor: takes registry, compiler, polling interval.
  - `start()` — schedule periodic task via `ScheduledExecutorService`.
  - Each tick: call `registry.refresh()`, for each changed ID call `compiler.invalidate(id)`.
  - `stop()` — shutdown executor.
  - Thread safety: reload must not disrupt in-flight evaluations (compiler cache is `ConcurrentHashMap`, old entry remains readable until replaced).

Tests:
- Integration test: start manager, modify script file on disk, wait > polling interval, verify new behavior.
- Integration test: modify script to introduce compile error → `use-last-good` behavior preserved.
- Concurrency test: parallel evaluations during reload → no exceptions or inconsistent state.

**Done when**: script changes on disk are picked up automatically without restart.

### Step 7: RulesEngine Wiring and Lifecycle

**Goal**: Wire all script components from `RulesEngine` initialization and ensure clean shutdown.

Changes:
- `RulesEngine` (or `RulesEngineBuilder`)
  - During initialization: if `YamlRuleConfiguration` contains `runtime-scripts` config:
    1. Create `RuntimeScriptRegistry`, load scripts.
    2. Create `GroovyScriptCompiler`.
    3. Create `ScriptExecutor`.
    4. Pass registry to `ExpressionEvaluatorService`.
    5. Create and start `ScriptReloadManager`.
  - `shutdown()`: stop reload manager, shutdown executor.

Tests:
- Integration test: `RulesEngine.fromFile("config-with-scripts.yaml")` → engine loads, evaluates rule calling `#script(...)`, shuts down cleanly.
- Integration test: `RulesEngine.fromFile("config-without-scripts.yaml")` → no script components created, no errors.

**Done when**: full lifecycle works via `RulesEngine` API.

### Step 8: Error Recovery Integration

**Goal**: Script failures route through existing APEX error recovery.

Changes:
- `ScriptExecutor` and `scriptBridge`: wrap script exceptions into APEX error model (`ApexExpressionException` or equivalent) with severity.
- Error recovery system handles these the same as any SpEL evaluation failure.

Tests:
- Integration test: script throws runtime exception + severity ERROR + recovery disabled → `ResultType.ERROR` / fail-fast.
- Integration test: script throws runtime exception + severity WARNING + recovery enabled → continues with default.

**Done when**: script errors are indistinguishable from other SpEL errors in APEX error recovery.

### Step 9: Audit Logging

**Goal**: Log script lifecycle and invocation events.

Changes:
- Registry: log at INFO on load, reload, allowlist rejection.
- Compiler: log at WARN on compile failure, INFO on successful compile/recompile.
- Executor: log at DEBUG on successful invocation (with script ID, function name, duration), INFO on failure/timeout.

Tests:
- Verify log output contains expected entries for load, invoke, timeout, compile-error scenarios (log-capture in tests).

**Done when**: all audit events listed in Safety and Governance section are emitted.

### Step 10: Demo and Documentation

**Goal**: Add a working demo and update user-facing docs.

New files:
- `apex-demo/src/test/java/.../script/RuntimeScriptDemoTest.java` — demo test following `DemoTestBase` pattern.
- `apex-demo/src/test/resources/.../RuntimeScriptDemoTest.yaml` — YAML config with `runtime-scripts` block and rules/enrichments calling `#script(...)`.
- `apex-demo/src/test/resources/.../scripts/risk-score.groovy` — sample Groovy script.

Documentation:
- Update `docs/APEX_SPEL_GUIDE.md` with `#script(...)` usage.
- Update `docs/APEX_YAML_REFERENCE.md` with `runtime-scripts` configuration block.

**Done when**: demo passes, docs updated, ready for review.

### Dependency Graph

```
Step 1 (model)
  └─> Step 2 (registry)
        └─> Step 3 (compiler)
              └─> Step 4 (executor)
                    └─> Step 5 (SpEL bridge)  ← first end-to-end test possible here
                          └─> Step 6 (reload manager)
                          └─> Step 7 (RulesEngine wiring)
                                └─> Step 8 (error recovery)
                                └─> Step 9 (audit logging)
                                      └─> Step 10 (demo + docs)
```

Steps 1-5 are the critical path to the first working `#script(...)` call.
Steps 6-9 can be partially parallelized after Step 5.
Step 10 is final.
