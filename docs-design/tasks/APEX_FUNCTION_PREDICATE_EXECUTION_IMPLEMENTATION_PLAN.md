# APEX Function Predicate Execution — Implementation Plan

## Objective

Wire the `"function"` type predicate in `UnifiedRuleEvaluator.evaluateStructuredConditionRule`
so that it actually **executes the referenced enrichment group**, stashes the output into the
SpEL evaluation context, and then evaluates the gate condition — instead of logging a
placeholder message and falling straight through to gate-only evaluation.

## Current State

The `"function"` branch inside `evaluateStructuredConditionRule` is a stub:

```java
} else if ("function".equalsIgnoreCase(type)) {
    logger.debug("Function predicate '{}' — function execution requires EnrichmentGroupExecutor (not yet wired); evaluating gate only",
            rule.getDescription());
}
```

The `"lookup"` branch was fully wired in Phase 3 (see `executeLookupPredicate`).
The `"function"` branch has never been wired.

## Why Function Is Harder Than Lookup

| Concern | Lookup (already wired) | Function (this task) |
|---------|------------------------|----------------------|
| Resolve service/group | `LookupServiceRegistry.getService(name)` | `EnrichmentGroupFactory.buildEnrichmentGroups(yamlConfig)` → find by `enrichmentGroupRef` |
| New deps needed in `UnifiedRuleEvaluator` | `LookupServiceRegistry` ✅ done | `YamlRuleConfiguration` + `Supplier<EnrichmentGroupExecutor>` ❌ missing |
| Inputs | `lookup-key` SpEL → scalar | `input-parameters` (list of `FieldMapping`) → bind values into facts Map |
| Execute | `lookupService.transform(key)` | `executor.processEnrichmentGroup(group, facts, yamlConfig)` → writes fields into facts Map |
| Stash output | `context.setVariable(resultField, result)` | `context.setVariable(outputField, facts.get(outputField))` |
| Facts Map required | No | **Yes** — `processEnrichmentGroup` takes `Object targetObject`; the facts Map serves this role |

## Key Structural Problem

`evaluateStructuredConditionGroup(group, context)` is called from
`evaluateRule(Rule, EvaluationContext)` — there is no facts Map in scope at that call site.
The facts Map only exists in `evaluateRule(Rule, Map<String, Object>)`.

Function execution mutates the facts Map (the enrichment group writes `outputField` into it),
so the Map must be threaded down through the private evaluation methods as a nullable parameter.
When the Map is absent (the context-only path), function predicates fall back to gate-only
evaluation — exactly the same as the no-registry fallback for lookup predicates.

## Circular Dependency Pattern

`RulesEngine` creates `UnifiedRuleEvaluator` at line 255, then creates
`EnrichmentGroupExecutor` at line ~270. The same circular dependency problem was already
solved for `EnrichmentProcessor` using a `Supplier<EnrichmentGroupExecutor>` setter
(see `enrichmentProcessor.setEnrichmentGroupExecutorSupplier`).

This task uses the identical pattern for `UnifiedRuleEvaluator`.

## Reference Implementation

`ConditionActionExecutor.executeFunction()` already performs the same operation for
enrichment conditions. Study it for the input-parameter binding loop and the
`processEnrichmentGroup` call pattern.

Key method signature in `EnrichmentGroupExecutor`:

```java
public EnrichmentGroupResult processEnrichmentGroup(
        EnrichmentGroup group,
        Object targetObject,          // pass the facts Map here
        YamlRuleConfiguration yamlConfig)
```

## YAML Shape Being Targeted

The following YAML must execute correctly at the end of this task:

```yaml
rules:
  - id: "risk-tier-check"
    name: "Risk Tier Check"
    severity: "ERROR"
    message: "Trade passes risk classification"
    no-match-message: "Trade failed risk classification"
    conditions:
      operator: "AND"
      rules:
        - type: "function"
          description: "Classify trade risk level"
          enrichment-group-ref: "risk-classifier-group"
          input-parameters:
            - source-field: "#tradeValue"
              target-field: "compute_input"
          output-field: "riskLevel"
          condition: "#riskLevel == 'HIGH'"
```

After function execution, the enrichment group writes `riskLevel` into the facts Map.
The evaluator reads it from the Map, stashes it into the SpEL context via
`context.setVariable("riskLevel", facts.get("riskLevel"))`, then evaluates the gate
condition `#riskLevel == 'HIGH'`.

---

## TDD Approach

This task uses strict TDD:

1. Write RED tests first (they compile but fail because the function is not yet executed)
2. Implement the production code
3. Verify tests go GREEN
4. Confirm all existing tests remain GREEN

The tests are written before the implementation to define the expected contract precisely.
The RED state proves the test is actually testing the new behaviour, not existing behaviour.

---

## Implementation Plan

### Phase A — Inject missing dependencies into `UnifiedRuleEvaluator`

**Affected file:** `apex-core/src/main/java/dev/mars/apex/engine/core/UnifiedRuleEvaluator.java`

**Step A1 — Add new imports:**

```java
import dev.mars.apex.core.config.EnrichmentGroupFactory;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.execution.EnrichmentGroupExecutor;
import dev.mars.apex.engine.model.EnrichmentGroup;
import java.util.function.Supplier;
```

**Step A2 — Add two new optional fields** (after the existing `serviceRegistry` field):

```java
// Optional: enables function predicate execution in structured condition rules
private YamlRuleConfiguration yamlRuleConfiguration;
private Supplier<EnrichmentGroupExecutor> enrichmentGroupExecutorSupplier;
```

**Step A3 — Add public setters** (after the `LookupServiceRegistry` constructor):

```java
/**
 * Set the YAML rule configuration for function predicate execution.
 * Required for function-type structured condition predicates.
 * Called by {@link dev.mars.apex.engine.core.RulesEngine} after construction.
 */
public void setYamlRuleConfiguration(YamlRuleConfiguration config) {
    this.yamlRuleConfiguration = config;
}

/**
 * Set the enrichment group executor supplier for function predicate execution.
 * Uses a supplier to break the circular construction dependency in
 * {@link dev.mars.apex.engine.core.RulesEngine} (same pattern as
 * {@code EnrichmentProcessor.setEnrichmentGroupExecutorSupplier}).
 */
public void setEnrichmentGroupExecutorSupplier(Supplier<EnrichmentGroupExecutor> supplier) {
    this.enrichmentGroupExecutorSupplier = supplier;
}
```

**Affected file:** `apex-core/src/main/java/dev/mars/apex/engine/core/RulesEngine.java`

**Step A4 — Wire the setters** immediately after `this.enrichmentGroupExecutor` is created
(after line `this.enrichmentProcessor.setEnrichmentGroupExecutorSupplier(...)`):

```java
// Wire function predicate dependencies into the unified evaluator
this.unifiedEvaluator.setEnrichmentGroupExecutorSupplier(() -> this.enrichmentGroupExecutor);
this.unifiedEvaluator.setYamlRuleConfiguration(yamlConfig);
```

---

### Phase B — Thread the facts Map through private evaluation methods

**Affected file:** `apex-core/src/main/java/dev/mars/apex/engine/core/UnifiedRuleEvaluator.java`

The facts Map must be available inside `evaluateStructuredConditionRule` so that
`executeFunctionPredicate` can pass it as `targetObject` to `processEnrichmentGroup`.

**Step B1 — Introduce `evaluateRuleInternal`**

The existing `evaluateRule(Rule rule, EvaluationContext context)` body is extracted into a
new private method:

```java
private RuleResult evaluateRuleInternal(Rule rule, EvaluationContext context, Map<String, Object> facts)
```

The public `evaluateRule(Rule, EvaluationContext)` becomes a thin delegate:

```java
public RuleResult evaluateRule(Rule rule, EvaluationContext context) {
    return evaluateRuleInternal(rule, context, null);
}
```

The existing `evaluateRule(Rule, Map<String, Object>)` changes its call to:

```java
RuleResult result = evaluateRuleInternal(rule, context, facts);
```

**Step B2 — Add facts-aware overloads for the private methods**

New overloads (existing no-facts overloads delegate to them):

```java
private boolean evaluateStructuredConditionGroup(
        SharedConditionGroup group, EvaluationContext context, Map<String, Object> facts)

private boolean evaluateStructuredConditionRule(
        SharedConditionRule rule, EvaluationContext context, Map<String, Object> facts)
```

The existing no-facts signatures delegate with `null`:

```java
private boolean evaluateStructuredConditionGroup(SharedConditionGroup group, EvaluationContext context) {
    return evaluateStructuredConditionGroup(group, context, null);
}

private boolean evaluateStructuredConditionRule(SharedConditionRule rule, EvaluationContext context) {
    return evaluateStructuredConditionRule(rule, context, null);
}
```

Inside `evaluateRuleInternal`, the structured-condition call becomes:

```java
result = evaluateStructuredConditionGroup(rule.getConditions(), context, facts);
```

---

### Phase C — Implement `executeFunctionPredicate`

**Affected file:** `apex-core/src/main/java/dev/mars/apex/engine/core/UnifiedRuleEvaluator.java`

**Step C1 — Update the function branch in `evaluateStructuredConditionRule`**

Replace the logging stub with:

```java
} else if ("function".equalsIgnoreCase(type)) {
    if (enrichmentGroupExecutorSupplier != null && yamlRuleConfiguration != null && facts != null) {
        executeFunctionPredicate(rule, context, facts);
    } else {
        logger.debug("Function predicate '{}' — EnrichmentGroupExecutor/YamlRuleConfiguration/facts not available; evaluating gate only",
                rule.getDescription());
    }
}
```

Update the Javadoc bullet for `function`:

```
 *   <li>{@code function}: Executes the referenced enrichment group via the injected
 *       {@link EnrichmentGroupExecutor} (if present), binds input-parameters into the facts Map,
 *       stashes the {@code output-field} value into the context, then evaluates the SpEL condition
 *       gate. Falls back to gate-only evaluation when executor/config/facts are unavailable.</li>
```

**Step C2 — Implement `executeFunctionPredicate`**

```java
/**
 * Execute a function predicate: binds input-parameters, invokes the referenced enrichment group,
 * and stashes the {@code output-field} value from the facts Map into the evaluation context so
 * the gate condition can access it via {@code #outputField}.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>Guard: {@code enrichment-group-ref} must be non-empty.</li>
 *   <li>Guard: {@code enrichmentGroupExecutorSupplier}, {@code yamlRuleConfiguration}, and
 *       {@code facts} must all be non-null.</li>
 *   <li>Apply {@code input-parameters}: for each {@link YamlEnrichment.FieldMapping}, evaluate
 *       the {@code source-field} SpEL expression against the context and put the result into
 *       {@code facts.put(targetField, value)}.</li>
 *   <li>Resolve enrichment group by {@code enrichment-group-ref} using
 *       {@link EnrichmentGroupFactory#buildEnrichmentGroups(YamlRuleConfiguration)}.</li>
 *   <li>Execute via {@link EnrichmentGroupExecutor#processEnrichmentGroup(EnrichmentGroup, Object, YamlRuleConfiguration)}
 *       passing the facts Map as {@code targetObject}.</li>
 *   <li>If execution succeeds, read {@code output-field} from the facts Map and stash into the
 *       context via {@link StandardEvaluationContext#setVariable(String, Object)}.</li>
 * </ol>
 *
 * <p>All failures are logged as warnings and silently swallowed so the evaluator falls through
 * to gate evaluation with whatever value (or absence) is currently in the context.</p>
 *
 * @param rule    The condition predicate with {@code enrichment-group-ref}, {@code input-parameters},
 *                and {@code output-field}
 * @param context The SpEL evaluation context to mutate with the stashed output
 * @param facts   The facts Map — serves as {@code targetObject} for the enrichment group
 *                and as the source of the output value after execution
 */
private void executeFunctionPredicate(SharedConditionRule rule,
                                       EvaluationContext context,
                                       Map<String, Object> facts) {
    String groupRef = rule.getEnrichmentGroupRef();
    if (groupRef == null || groupRef.trim().isEmpty()) {
        logger.warn("Function predicate '{}' has no enrichment-group-ref; skipping execution",
                rule.getDescription());
        return;
    }

    try {
        // Step 1: Apply input-parameters — bind values into the facts Map
        List<YamlEnrichment.FieldMapping> inputParams = rule.getInputParameters();
        if (inputParams != null) {
            for (YamlEnrichment.FieldMapping param : inputParams) {
                try {
                    String src = param.getSourceField();
                    if (src == null || src.trim().isEmpty()) {
                        continue;
                    }
                    String spel = src.startsWith("#") ? src : "#" + src;
                    Object value = parser.parseExpression(spel).getValue(context);
                    if (param.getTargetField() != null) {
                        facts.put(param.getTargetField(), value);
                        logger.debug("Function predicate '{}' input: {} -> {} = {}",
                                rule.getDescription(), src, param.getTargetField(), value);
                    }
                } catch (Exception e) {
                    logger.warn("Function predicate '{}' failed to bind input-parameter '{}': {}",
                            rule.getDescription(), param.getSourceField(), e.getMessage());
                }
            }
        }

        // Step 2: Resolve enrichment group by ref
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(yamlRuleConfiguration);
        EnrichmentGroup targetGroup = groups.stream()
                .filter(g -> groupRef.equals(g.getId()))
                .findFirst()
                .orElse(null);

        if (targetGroup == null) {
            logger.warn("Function predicate '{}': enrichment-group-ref '{}' not found in configuration",
                    rule.getDescription(), groupRef);
            return;
        }

        // Step 3: Execute the enrichment group — mutates the facts Map in place
        EnrichmentGroupExecutor executor = enrichmentGroupExecutorSupplier.get();
        dev.mars.apex.engine.model.EnrichmentGroupResult groupResult =
                executor.processEnrichmentGroup(targetGroup, facts, yamlRuleConfiguration);

        if (!groupResult.isSuccess()) {
            logger.warn("Function predicate '{}': enrichment group '{}' execution failed: {}",
                    rule.getDescription(), groupRef, groupResult.getMessage());
            return;
        }

        // Step 4: Stash output-field from facts Map into context variable
        String outputField = rule.getOutputField();
        if (outputField != null && !outputField.trim().isEmpty()
                && context instanceof StandardEvaluationContext) {
            Object outputValue = facts.get(outputField);
            ((StandardEvaluationContext) context).setVariable(outputField, outputValue);
            logger.debug("Function predicate '{}' stashed output into context variable '{}': {}",
                    rule.getDescription(), outputField, outputValue);
        }

    } catch (Exception e) {
        logger.warn("Function predicate '{}' execution failed: {}", rule.getDescription(), e.getMessage());
        logger.debug("Full stack trace for function predicate execution failure:", e);
    }
}
```

---

### Phase D — TDD: Write RED Tests First

**Affected file:** `apex-core/src/test/java/dev/mars/apex/engine/core/UnifiedRuleEvaluatorTest.java`

The RED tests must be written **before** Phase C is implemented (or with Phase A+B stub only).
They compile because the constructor and setters exist, but fail because `executeFunctionPredicate`
does not yet exist.

#### What the tests need — minimal test infrastructure

Function predicate tests require a real `EnrichmentGroupExecutor` backed by a minimal
`EnrichmentProcessor`. The test creates a small inline `YamlRuleConfiguration` that contains
one enrichment group with a single `expression`-type enrichment that writes a constant value
into `outputField`.

This follows the APEX no-mock policy (real services only; no mocks in demo tests).

**Step D1 — Add required imports to `UnifiedRuleEvaluatorTest`:**

```java
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.condition.SharedConditionGroup;
import dev.mars.apex.core.config.model.condition.SharedConditionRule;
import dev.mars.apex.core.service.enrichment.EnrichmentProcessor;
import dev.mars.apex.engine.execution.EnrichmentGroupExecutor;
```

**Step D2 — Add a private helper to build a minimal configuration:**

```java
/**
 * Build a minimal YamlRuleConfiguration containing one enrichment group that writes
 * a constant value into the specified output field. Used to test function predicates
 * without a real database or REST service.
 *
 * @param groupId     The enrichment group id to reference from the function predicate
 * @param outputField The field name the enrichment writes to
 * @param outputValue The constant value the enrichment writes
 */
private YamlRuleConfiguration buildFunctionTestConfig(
        String groupId, String outputField, String outputValue) {
    // Build the enrichment
    YamlEnrichment enrichment = new YamlEnrichment();
    enrichment.setId(outputField + "-enrichment");
    enrichment.setType("expression");
    YamlEnrichment.FieldMapping mapping = new YamlEnrichment.FieldMapping();
    mapping.setTargetField(outputField);
    mapping.setExpression("'" + outputValue + "'");
    enrichment.setFieldMappings(List.of(mapping));

    // Build the enrichment group
    // (Use YamlEnrichment.EnrichmentGroup or however the YAML model represents groups)
    // ...see YamlRuleConfiguration schema for exact field names...

    YamlRuleConfiguration config = new YamlRuleConfiguration();
    // populate as required...
    return config;
}
```

> Note: The exact YAML model classes for enrichment groups must be verified against
> `YamlRuleConfiguration` before coding this helper. The helper's purpose is to produce
> a config where `EnrichmentGroupFactory.buildEnrichmentGroups(config)` returns a group
> with id `groupId` that writes `outputValue` into `outputField` on the facts Map.

**Step D3 — Write RED test cases:**

```java
// =========================================================================
// Phase 3 — Function Predicate Execution Tests (TDD: RED first, then GREEN)
// =========================================================================
//
// RED STATE (Phase A+B only — no executeFunctionPredicate):
//   The enrichment group never runs. The context variable for output-field is
//   never set. Gate evaluations that depend on the stashed value fail.
//
// GREEN STATE (Phase C implemented):
//   Enrichment group runs, output-field stashed, gate re-evaluated correctly.

@Test
@DisplayName("[Phase 3] Function predicate executes enrichment group and stashes output; gate passes")
void testPhase3_FunctionExecuted_StashesOutput_GatePasses() {
    // Given: facts have riskLevel = "LOW" — without function execution gate reads "LOW" → NO_MATCH
    Map<String, Object> facts = new HashMap<>(testFacts);
    facts.put("riskLevel", "LOW");  // RED: gate reads "LOW" → false; GREEN: group overrides → "HIGH" → true

    // ... build config with enrichment group that writes riskLevel = "HIGH" ...
    YamlRuleConfiguration config = buildFunctionTestConfig("risk-classifier-group", "riskLevel", "HIGH");

    EnrichmentProcessor ep = new EnrichmentProcessor(new dev.mars.apex.core.service.lookup.LookupServiceRegistry(),
            new dev.mars.apex.core.service.ExpressionEvaluatorService(), null, null);
    EnrichmentGroupExecutor executor = new EnrichmentGroupExecutor(ep);

    UnifiedRuleEvaluator evaluatorWithFunction = new UnifiedRuleEvaluator();
    evaluatorWithFunction.setYamlRuleConfiguration(config);
    evaluatorWithFunction.setEnrichmentGroupExecutorSupplier(() -> executor);

    SharedConditionRule funcPred = new SharedConditionRule();
    funcPred.setType("function");
    funcPred.setDescription("Risk classification");
    funcPred.setEnrichmentGroupRef("risk-classifier-group");
    funcPred.setOutputField("riskLevel");
    funcPred.setCondition("#riskLevel == 'HIGH'");

    SharedConditionGroup group = new SharedConditionGroup();
    group.setOperator("AND");
    group.setRules(List.of(funcPred));

    Rule rule = new RuleBuilder()
            .withName("Function Execution Test")
            .withConditions(group)
            .withMessage("Risk is HIGH")
            .withSeverity("INFO")
            .build();

    // When
    RuleResult result = evaluatorWithFunction.evaluateRule(rule, facts);

    // Then: MATCH — function writes "HIGH" into context, gate passes
    // RED: NO_MATCH — function not executed, gate reads "LOW"
    assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Function should execute, stash 'HIGH' into context, gate should pass");
    assertTrue(result.isTriggered());
}

@Test
@DisplayName("[Phase 3] Function predicate gate fails when group output does not satisfy gate condition")
void testPhase3_FunctionExecuted_GateFails_WhenOutputIsWrong() {
    // Given: enrichment group writes riskLevel = "LOW"; gate requires "HIGH"
    Map<String, Object> facts = new HashMap<>(testFacts);

    YamlRuleConfiguration config = buildFunctionTestConfig("risk-classifier-group-2", "riskLevel", "LOW");

    EnrichmentProcessor ep = new EnrichmentProcessor(new dev.mars.apex.core.service.lookup.LookupServiceRegistry(),
            new dev.mars.apex.core.service.ExpressionEvaluatorService(), null, null);
    EnrichmentGroupExecutor executor = new EnrichmentGroupExecutor(ep);

    UnifiedRuleEvaluator evaluatorWithFunction = new UnifiedRuleEvaluator();
    evaluatorWithFunction.setYamlRuleConfiguration(config);
    evaluatorWithFunction.setEnrichmentGroupExecutorSupplier(() -> executor);

    SharedConditionRule funcPred = new SharedConditionRule();
    funcPred.setType("function");
    funcPred.setEnrichmentGroupRef("risk-classifier-group-2");
    funcPred.setOutputField("riskLevel");
    funcPred.setCondition("#riskLevel == 'HIGH'");  // false: group writes "LOW"

    SharedConditionGroup group = new SharedConditionGroup();
    group.setOperator("AND");
    group.setRules(List.of(funcPred));

    Rule rule = new RuleBuilder()
            .withName("Function Gate Fail Test")
            .withConditions(group)
            .withMessage("Should not match")
            .withSeverity("INFO")
            .build();

    // When
    RuleResult result = evaluatorWithFunction.evaluateRule(rule, facts);

    // Then: NO_MATCH — function writes "LOW", gate #riskLevel == 'HIGH' fails
    assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(),
            "Gate should fail when function output does not satisfy condition");
    assertFalse(result.isTriggered());
}

@Test
@DisplayName("[Phase 3] Function predicate with no executor — falls back to gate-only evaluation")
void testPhase3_FunctionWithNoExecutor_FallsBackToGateOnly() {
    // Given: evaluator with NO executor — gate reads existing fact value
    Map<String, Object> facts = new HashMap<>(testFacts);
    facts.put("riskLevel", "HIGH");  // pre-seeded so gate passes without any function execution

    SharedConditionRule funcPred = new SharedConditionRule();
    funcPred.setType("function");
    funcPred.setDescription("Function no executor");
    funcPred.setEnrichmentGroupRef("any-group");
    funcPred.setOutputField("riskLevel");
    funcPred.setCondition("#riskLevel == 'HIGH'");

    SharedConditionGroup group = new SharedConditionGroup();
    group.setOperator("AND");
    group.setRules(List.of(funcPred));

    Rule rule = new RuleBuilder()
            .withName("No Executor Fallback")
            .withConditions(group)
            .withMessage("Gate only")
            .withSeverity("INFO")
            .build();

    // When: plain evaluator (no executor, no config)
    RuleResult result = evaluator.evaluateRule(rule, facts);

    // Then: MATCH — no function executed, gate reads pre-seeded "HIGH" → true
    // This verifies backward compatibility: plain evaluator without function support still works.
    assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Without executor, gate should evaluate against existing fact value");
    assertTrue(result.isTriggered());
}

@Test
@DisplayName("[Phase 3] Function predicate with input-parameters — parameters bound before group execution")
void testPhase3_FunctionExecuted_InputParametersBound() {
    // Given: enrichment group reads compute_input from facts and writes riskLevel accordingly.
    // Test verifies the input-parameter value reaches the group (by using a group that
    // echoes the input into the output field).
    // ... detailed test scenario tbd based on how inline expression enrichment works ...
}
```

---

### Phase E — Verification

Run after all phases complete:

```powershell
# Unit tests for the core evaluator
cd apex-core
mvn test -Dtest=UnifiedRuleEvaluatorTest 2>&1 | Tee-Object -FilePath ..\logs\function-predicate-tests.txt

# Structured condition validation — must be unaffected
mvn test -Dtest=StructuredConditionValidationTest 2>&1 | Tee-Object -FilePath ..\logs\function-predicate-validation-tests.txt

# Demo function condition ref tests — must be unaffected
cd ..\apex-demo
mvn test -Dtest=FunctionConditionRefsTest 2>&1 | Tee-Object -FilePath ..\logs\function-condition-refs-tests.txt

# Full apex-core regression
cd ..\apex-core
mvn test 2>&1 | Tee-Object -FilePath ..\logs\function-predicate-full.txt
```

Expected result:
- All 3 new Phase 3 function tests: GREEN
- All existing 18 `UnifiedRuleEvaluatorTest` tests: GREEN (no regressions)
- All `StructuredConditionValidationTest` tests: GREEN
- All `FunctionConditionRefsTest` tests: GREEN

---

## File Change Summary

| File | Change Type | Description |
|------|-------------|-------------|
| `apex-core/.../engine/core/UnifiedRuleEvaluator.java` | Modify | Add fields, setters, `evaluateRuleInternal`, facts-threaded overloads, `executeFunctionPredicate` |
| `apex-core/.../engine/core/RulesEngine.java` | Modify | Call `setEnrichmentGroupExecutorSupplier` and `setYamlRuleConfiguration` after executor created |
| `apex-core/.../engine/core/UnifiedRuleEvaluatorTest.java` | Modify | Add 3-4 Phase 3 function tests (RED first, GREEN after implementation) |

---

## Design Decisions

### Facts Map as `targetObject`

`EnrichmentGroupExecutor.processEnrichmentGroup` takes `Object targetObject`. The engine uses
`Map<String, Object>` everywhere as the shared data carrier. Passing the facts Map directly is
the correct approach and is consistent with how `enrichmentGroupExecutor.executeEnrichmentGroupsList`
works.

### No `FieldAccessor` reflection needed

Input-parameter binding in this context does not need reflection. Values are evaluated from the
SpEL context and written into the facts Map via `facts.put(targetField, value)`. This is simpler
than the `FieldAccessor.setFieldValue` path in `ConditionActionExecutor` which supports
arbitrary target objects with reflection. The facts Map is always a plain `Map<String, Object>`.

### `EnrichmentGroupFactory.buildEnrichmentGroups()` called per evaluation

Same behaviour as `ConditionActionExecutor.executeFunction()`. The factory call is not cached.
This is acceptable for correctness at this stage; caching can be added later if performance
profiling identifies it as a bottleneck.

### Function predicates gate-only when called via `evaluateRule(Rule, EvaluationContext)`

When `UnifiedRuleEvaluator` is called with a pre-built `EvaluationContext` (no facts Map),
function predicates fall back to gate-only evaluation. This matches the no-registry fallback
for lookup predicates and preserves backward compatibility for callers that construct the
context themselves.

### `yamlRuleConfiguration` set to `null` by default

The field is `null` when no YAML configuration is available (e.g., tests that use
`RulesEngine.fromYamlConfig` without a function predicate). Null check gates all function
execution attempts, so there is no NPE risk.

---

## Scope Boundaries — What This Task Does NOT Do

- This task does **not** extract `SharedConditionEvaluator` or `SharedConditionActionExecutor`
  (that refactor is described in the broader structured conditions plan and is a separate task).
- This task does **not** implement function predicates for the enrichment path — that path already
  works via `ConditionActionExecutor.executeFunction()`.
- This task does **not** add YAML validation for function predicates — existing validation in
  `EnrichmentValidator` / `InlineConfigurationValidator` already enforces `enrichment-group-ref`
  presence for `type: "function"`.
- This task does **not** cache `EnrichmentGroupFactory.buildEnrichmentGroups()` results.
