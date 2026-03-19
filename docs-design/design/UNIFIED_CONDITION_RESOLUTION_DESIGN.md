# Design: Unified Resolution Types for Conditional Mapping Predicates

**Status:** Implemented  
**Date:** 2026-03-19  
**Module:** apex-core  
**Depends on:** `function` mapping type (FUNCTION_MAPPING_TYPE_DESIGN.md — implemented)

---

## Solution Summary

APEX conditional-mapping-enrichment had an asymmetry: the THEN (mapping) side supported pluggable resolution types (`"direct"`, `"lookup"`, `"function"`), but the WHEN (condition) side was hard-coded to SpEL expressions only. This blocked three Markit Rule Builder conversion patterns.

**The fix:** add the same `type` dispatch to condition predicates. A `ConditionRule` now accepts `type: "expression"` (default — pure SpEL, unchanged), `type: "lookup"` (execute a lookup, store the result, then evaluate SpEL against it), or `type: "function"` (invoke an enrichment group, store the output, then evaluate SpEL). The `condition:` SpEL string is always the final boolean gate regardless of type.

**Key design properties:**
- **Symmetric resolution** — both WHEN and THEN sides have pluggable type dispatch with aligned vocabulary
- **Cross-phase data flow** — lookup/function results are stored into the shared context (`result-field` / `output-field`) and are available to subsequent conditions and to the mapping side
- **Short-circuit evaluation** — AND/OR operators evaluate left-to-right; cheap SpEL guards placed before expensive lookups prevent unnecessary execution
- **No new model classes** — 6 optional fields added to `ConditionRule`; reuses existing `LookupConfig` and `FieldMapping`
- **Full backward compatibility** — `type` absent = `"expression"` = today's behavior; new fields default to `null`

**Implementation artifacts:**
- `YamlEnrichment.ConditionRule` — expanded with `type`, `lookup-config`, `result-field`, `enrichment-group-ref`, `input-parameters`, `output-field`
- `ConditionActionExecutor` (new) — executes lookup and function actions for typed condition predicates
- `EnrichmentConditionEvaluator` — rewritten with type dispatch
- `EnrichmentProcessor` — `applyLookupMapping()` implemented; `YamlRuleConfiguration` threaded through condition evaluation

**Three Markit patterns now supported:**

| Pattern | Condition (WHEN) | Mapping (THEN) | Demo test |
|---------|-----------------|----------------|-----------|
| IF=Function, THEN=Function | `type: "function"` | `type: "function"` | `FunctionConditionDemoTest` |
| IF=DB Lookup | `type: "lookup"` | `type: "direct"` | `LookupConditionDemoTest` |
| RefLookup (pure lookup) | _(none)_ | `type: "lookup"` | `LookupMappingDemoTest` |

---

## Problem

APEX conditional-mapping-enrichment has an asymmetry between its WHEN (condition) and THEN (mapping) sides.

The **THEN side** has pluggable resolution — `mapping.type` dispatches to `"direct"`, `"lookup"`, or `"function"`:

```yaml
mapping:
  type: "function"                    # pluggable resolution
  enrichment-group-ref: "translate-group"
  input-parameters: [ ... ]
  output-field: "translated_result"
```

The **WHEN side** is hard-coded to SpEL:

```yaml
conditions:
  operator: "AND"
  rules:
    - condition: "#trade.broker_code != null"   # SpEL only — no lookup, no function
```

This prevents three patterns found in Markit Rule Builder conversions:

### Pattern 1: IF = Function, THEN = Function

Both the condition and the action invoke enrichment groups (Rule Builders). Today only the THEN side supports `type: "function"`.

### Pattern 2: IF = Database Lookup

The condition itself is a database lookup — "look up the exec_broker row; if it exists, proceed." Today conditions cannot trigger lookups. The only workaround is running the lookup as a separate top-level enrichment before the conditional mapping, which creates implicit coupling.

### Pattern 3: RefLookup — Pure DB Lookup, No Condition

A direct database lookup with no conditional logic — resolve a value from a DB table and assign it. This requires `type: "lookup"` on the mapping (THEN) side, but `applyLookupMapping()` is currently an unimplemented stub.

### Root cause

Both sides of a mapping rule perform the same fundamental operation: **resolve something**. The WHEN side resolves to a boolean. The THEN side resolves to a value. The resolution mechanism (SpEL evaluation, database lookup, enrichment group invocation) is orthogonal to which side it serves. The current model only recognizes this on the THEN side.

---

## Proposed Solution

Add `type` to condition predicates (`ConditionRule`) with the same vocabulary as the mapping side: `"expression"` (default/existing), `"lookup"`, and `"function"`.

For non-expression types, the system executes the resolution action (lookup or function), storees the result into the shared context, then evaluates the `condition` SpEL string against that context. The `condition:` field is **always present** and is always the final boolean gate.

### Design Principle

Both sides of a mapping rule are expressions with pluggable resolution strategies. The only difference is what the resolution produces:

| Side | Resolves to | `type` vocabulary |
|------|-------------|-------------------|
| Condition (WHEN) | Boolean | `"expression"` (default), `"lookup"`, `"function"` |
| Mapping (THEN) | Value | `"direct"` (default), `"lookup"`, `"function"` |

### What `condition:` means across types

| Type | What happens first | What `condition:` checks |
|------|-------------------|-------------------------|
| `expression` (or absent) | Nothing | The SpEL expression itself IS the predicate |
| `lookup` | Execute lookup, store result into `result-field` | SpEL on the stored result |
| `function` | Invoke enrichment group, store output into `output-field` | SpEL on the stored output |

For the expression type, `condition` does all the work. For lookup/function types, `condition` is the boolean interpretation of the resolved data. The YAML author always reads `condition:` to understand what makes this predicate true.

---

## YAML Schema

### Expression predicate (existing behavior — no change)

```yaml
conditions:
  operator: "AND"
  rules:
    - condition: "#trade.broker_code != null"
```

`type` absent or `"expression"` — pure SpEL evaluation. Identical to today.

### Lookup predicate

```yaml
conditions:
  operator: "AND"
  rules:
    - type: "lookup"
      lookup-config:
        lookup-dataset:
          data-source-ref: "broker-database"
          query-ref: "getBrokerByCode"
        lookup-key: "#trade.broker_code"
      result-field: "broker_row"
      condition: "#broker_row != null"
```

Execute a lookup, store the result into `result-field`, then evaluate `condition` as SpEL. The stored data persists in the shared context — available to subsequent conditions and to the mapping (THEN) side.

`lookup-config` is the existing `LookupConfig` model class. All existing lookup features (inline data, database, filesystem, REST API, data-source-ref, query-ref, caching) are available.

### Function predicate

```yaml
conditions:
  operator: "AND"
  rules:
    - type: "function"
      enrichment-group-ref: "validate-broker-group"
      input-parameters:
        - source-field: "#trade.broker_code"
          target-field: "broker_input"
      output-field: "broker_valid"
      condition: "#broker_valid == true"
```

Invoke an enrichment group with bound input parameters, store the output into `output-field`, then evaluate `condition` as SpEL. Uses the same mechanism as `mapping.type: "function"`.

### Containment vs reference

Lookup predicates **contain** their config inline (the `lookup-config:` block), but APEX lookups already support external references within that config:

```yaml
# Contained — inline data (small static datasets)
- type: "lookup"
  lookup-config:
    lookup-dataset:
      type: "inline"
      data: [ { code: "EUR", name: "Euro" } ]
      key-field: "code"
    lookup-key: "#trade.currency"
  result-field: "currency_row"
  condition: "#currency_row != null"

# Referenced — external data source
- type: "lookup"
  lookup-config:
    lookup-dataset:
      data-source-ref: "currency-database"
      query-ref: "getCurrencyByCode"
    lookup-key: "#trade.currency"
  result-field: "currency_row"
  condition: "#currency_row != null"
```

Function predicates **reference** by definition — `enrichment-group-ref` points to a group defined elsewhere.

Both containment and reference are naturally supported through existing APEX patterns. No additional mechanism needed.

---

## Markit Pattern Examples

### Pattern 1: IF = Function, THEN = Function

```yaml
mapping-rules:
  - id: "ndf-translate"
    priority: 5
    conditions:
      operator: "AND"
      rules:
        - type: "function"
          enrichment-group-ref: "validate-ndf-group"
          input-parameters:
            - source-field: "#fx.is_ndf"
              target-field: "ndf_input"
          output-field: "ndf_check_result"
          condition: "#ndf_check_result == true"
    mapping:
      type: "function"
      enrichment-group-ref: "translate-ndf-group"
      input-parameters:
        - source-field: "#fx.is_ndf"
          target-field: "translate_input"
      output-field: "translated_ndf"
```

### Pattern 2: IF = Database Lookup (exec_broker)

```yaml
mapping-rules:
  - id: "exec-broker"
    priority: 5
    conditions:
      operator: "AND"
      rules:
        - type: "lookup"
          lookup-config:
            lookup-dataset:
              data-source-ref: "broker-database"
              query-ref: "getBrokerByCode"
            lookup-key: "#trade.broker_code"
          result-field: "broker_row"
          condition: "#broker_row != null"
    mapping:
      type: "direct"
      expression: "#broker_row.internal_code"
```

The lookup executes once in the condition. The mapping reads the stored `broker_row` from context — no redundant DB call.

### Pattern 3: RefLookup (no condition — pure lookup)

```yaml
mapping-rules:
  - id: "ref-lookup"
    priority: 5
    # No conditions → always true
    mapping:
      type: "lookup"
      lookup-config:
        lookup-dataset:
          data-source-ref: "broker-database"
          query-ref: "getBrokerByCode"
        lookup-key: "#trade.broker_code"
      result-field: "broker_row"
      output-field: "broker_row.internal_code"
```

This pattern requires only `applyLookupMapping()` to be implemented. No condition changes needed.

### Mixed pattern: SpEL guard + lookup condition + function mapping

```yaml
mapping-rules:
  - id: "guarded-broker-translate"
    priority: 5
    conditions:
      operator: "AND"
      rules:
        # Cheap SpEL guard first — short-circuits before expensive DB call
        - condition: "#trade.broker_code != null"

        # DB lookup — only runs if guard passes
        - type: "lookup"
          lookup-config:
            lookup-service: "exec-broker-service"
            lookup-key: "#trade.broker_code"
          result-field: "broker_row"
          condition: "#broker_row != null"

    mapping:
      type: "function"
      enrichment-group-ref: "translate-broker-group"
      input-parameters:
        - source-field: "#broker_row.internal_code"
          target-field: "translate_input"
      output-field: "translated_broker"
```

Demonstrates cost-ordered evaluation: cheap SpEL check prevents unnecessary DB lookup.

---

## Design Decisions

### 1. `condition:` is always the final boolean gate

Every predicate — regardless of type — has a `condition` SpEL string that produces the boolean result. For `"expression"` type (the default), `condition` does all the work. For `"lookup"` and `"function"` types, `condition` interprets the resolved data.

This means the YAML author always reads `condition:` to understand the boolean semantics. The resolution mechanism is additional context, not a replacement.

### 2. Flat fields on ConditionRule (not a nested action block)

Two structural options were considered:

**(a) Flat** — `type`, `lookup-config`, `enrichment-group-ref`, etc. as direct fields on `ConditionRule`:

```yaml
- type: "lookup"
  lookup-config: { ... }
  result-field: "broker_row"
  condition: "#broker_row != null"
```

**(b) Nested `action` block** — resolution config grouped under an `action:` key:

```yaml
- condition: "#broker_row != null"
  action:
    type: "lookup"
    lookup-config: { ... }
    result-field: "broker_row"
```

**Decision: Flat (a).** Consistent with how `MappingConfig` works — flat fields with `type` dispatch. No additional nesting. The symmetry between condition and mapping is visible at the YAML structure level.

### 3. Cross-phase data flow via shared context

Stashed results (`result-field`, `output-field`) are written to the shared `targetObject` context. This enables the key pattern where the condition resolves data and the mapping consumes it:

```yaml
conditions:
  rules:
    - type: "lookup"
      result-field: "broker_row"          # stored here
      condition: "#broker_row != null"    # tested here
mapping:
  type: "direct"
  expression: "#broker_row.internal_code" # consumed here
```

The data flows through the same shared context mechanism that enrichments already use. No new data-passing infrastructure.

### 4. Short-circuit semantics

AND/OR operators short-circuit on condition results:

- **AND**: Evaluates left-to-right, stops on first `false`. If a cheap SpEL guard fails, subsequent expensive lookup/function conditions are never executed.
- **OR**: Evaluates left-to-right, stops on first `true`.

If a condition is skipped due to short-circuit, its `result-field`/`output-field` is never stored. If the mapping references that data, it gets `null`. This is consistent — the entire mapping rule didn't match (AND failed), so the mapping doesn't execute.

The YAML author controls cost ordering via condition sequence. Place cheap SpEL guards before expensive lookups.

### 5. `result-field` (lookup) vs `output-field` (function)

Different names for the same concept (store location) because they align with the corresponding mapping-side terminology:

| Condition type | Stash field | Mapping-side equivalent |
|---|---|---|
| `lookup` | `result-field` | `MappingConfig.resultField` |
| `function` | `output-field` | `MappingConfig.outputField` |

This maintains vocabulary consistency between the WHEN and THEN sides.

---

## Execution Flow

```
Conditional Mapping Enrichment
│
├─ for each mapping-rule (by priority):
│   │
│   ├─ PHASE 1: Evaluate conditions (AND/OR with short-circuit)
│   │   │
│   │   └─ for each predicate in conditions.rules:
│   │       ├─ type: "expression" (or absent)
│   │       │   └─ evaluateSpEL(condition) → boolean
│   │       │
│   │       ├─ type: "lookup"
│   │       │   ├─ 1. Execute lookup via LookupConfig
│   │       │   ├─ 2. Stash result into result-field on targetObject
│   │       │   └─ 3. evaluateSpEL(condition) → boolean
│   │       │
│   │       └─ type: "function"
│   │           ├─ 1. Apply input-parameters into targetObject
│   │           ├─ 2. Resolve enrichment-group-ref from config
│   │           ├─ 3. Execute group via EnrichmentGroupExecutor
│   │           ├─ 4. Stash output into output-field on targetObject
│   │           └─ 5. evaluateSpEL(condition) → boolean
│   │
│   ├─ PHASE 2: Apply mapping (if conditions matched)
│   │   ├─ type: "direct"   → applyDirectMapping()
│   │   ├─ type: "lookup"   → applyLookupMapping()
│   │   └─ type: "function" → applyFunctionMapping()
│   │
│   └─ stop-on-first-match → break
│
└─ Store result-field (boolean: any rule matched?)
```

---

## Implementation Scope

### Prerequisite: Implement `applyLookupMapping()`

The existing stub in `EnrichmentProcessor` must be wired to real lookup infrastructure before any of the three Markit patterns work fully. `MappingConfig` already has a `LookupConfig lookupConfig` field — the model is ready.

**Changes:**
- `EnrichmentProcessor.applyLookupMapping()` — replace stub with delegation to `LookupEnrichmentHandler` lookup resolution and execution logic
- `EnrichmentProcessor` may need a reference to `LookupEnrichmentHandler` (or its underlying `LookupServiceRegistry` and `DatasetLookupServiceFactory`)

This is a prerequisite for Pattern 3 (RefLookup) and for lookup conditions.

### Model Changes

**`YamlEnrichment.ConditionRule`** — 6 new optional fields:

| Field | Type | For type | Description |
|-------|------|----------|-------------|
| `type` | `String` | all | `"expression"` (default), `"lookup"`, `"function"` |
| `lookup-config` | `LookupConfig` | `lookup` | Lookup resolution configuration (existing class) |
| `result-field` | `String` | `lookup` | Context field to store lookup result |
| `enrichment-group-ref` | `String` | `function` | ID of enrichment group to invoke |
| `input-parameters` | `List<FieldMapping>` | `function` | Parameter bindings (existing class) |
| `output-field` | `String` | `function` | Context field to store function output |

No new model classes. `LookupConfig` and `FieldMapping` are existing inner classes of `YamlEnrichment`.

**Internal class rename consideration:** `ConditionRule` → `ConditionPredicate` or `PredicateConfig`. The YAML key `rules:` is unchanged. This is optional and can be deferred.

### Processor Changes

**`EnrichmentConditionEvaluator`** — type dispatch in `evaluateConditionRule()`:

```java
private boolean evaluateConditionRule(ConditionRule rule, Object targetObject,
                                       YamlRuleConfiguration config) {
    String type = rule.getType();
    if (type == null || "expression".equalsIgnoreCase(type)) {
        return evaluateSpEL(rule.getCondition(), targetObject);
    }

    if ("lookup".equalsIgnoreCase(type)) {
        Object result = conditionActionExecutor.executeLookup(rule, targetObject, config);
        setFieldValue(targetObject, rule.getResultField(), result);
        return evaluateSpEL(rule.getCondition(), targetObject);
    }

    if ("function".equalsIgnoreCase(type)) {
        Object result = conditionActionExecutor.executeFunction(rule, targetObject, config);
        setFieldValue(targetObject, rule.getOutputField(), result);
        return evaluateSpEL(rule.getCondition(), targetObject);
    }

    logger.warn("Unknown condition type: {}", type);
    return false;
}
```

**New: `ConditionActionExecutor`** — delegate injected into `EnrichmentConditionEvaluator` to handle lookup and function execution. This keeps the evaluator thin (dispatch + SpEL) and isolates the heavy dependencies (`LookupEnrichmentHandler`, `EnrichmentGroupExecutor`, `YamlRuleConfiguration`).

```java
class ConditionActionExecutor {
    Object executeLookup(ConditionRule rule, Object targetObject, YamlRuleConfiguration config);
    Object executeFunction(ConditionRule rule, Object targetObject, YamlRuleConfiguration config);
}
```

**Signature change:** `evaluateMappingRuleConditions()` and `evaluateConditionGroup()` must thread `YamlRuleConfiguration` through to `evaluateConditionRule()` for lookup service resolution and enrichment group resolution. Same pattern as the `processConditionalMappingEnrichment()` → `applyMappingRule()` signature change in the function mapping implementation.

### No Changes Required

- `ConditionGroup` — unchanged (operator + list of rules)
- `MappingConfig` — unchanged
- `EnrichmentProcessor.processConditionalMappingEnrichment()` — unchanged (already passes config to `applyMappingRule`)
- Existing `"direct"` and `"function"` mapping types — unchanged
- `EnrichmentGroupExecutor` — called as-is for function predicates

---

## Error Handling

| Failure | Behavior |
|---------|----------|
| Lookup returns null | `result-field` set to null; `condition` evaluates normally (e.g., `#broker_row != null` → `false`) |
| Lookup throws exception | Log warning; condition evaluates to `false` (safe default — rule doesn't match) |
| Function group execution fails | Log warning; condition evaluates to `false` |
| `result-field` / `output-field` not specified | Log warning; data not stored; `condition` evaluates against unmodified context |
| SpEL `condition` evaluation fails | Existing behavior (log warning, return `false`) |
| Unknown `type` value | Log warning, return `false` |

No new failure modes. A failed resolution defaults to condition=`false`, which means the mapping rule doesn't match. The system proceeds to the next priority rule.

---

## Backward Compatibility

- `type` absent on `ConditionRule` = `"expression"` = exactly today's behavior
- New fields (`lookup-config`, `result-field`, `enrichment-group-ref`, `input-parameters`, `output-field`) are optional `@JsonProperty` annotations — absent in existing YAML, default to `null`
- `ConfigurationLoader.createYamlMapper()` uses `FAIL_ON_UNKNOWN_PROPERTIES = true`, but new fields are added to the model class — they are *known* properties. No loader changes needed.
- The YAML key `rules:` is unchanged
- No changes to condition group semantics (AND/OR operators, short-circuit behavior)

---

## Implementation Sequencing

### Phase 1: `applyLookupMapping()` implementation

Wire the existing stub to real `LookupEnrichmentHandler` infrastructure. Enables Pattern 3 (RefLookup) immediately. No condition-side changes.

### Phase 2: Typed condition predicates

Add `type` field and resolution dispatch to `ConditionRule` / `EnrichmentConditionEvaluator`. Introduce `ConditionActionExecutor` delegate. Enables Patterns 1 and 2.

### Phase 3: Demo tests

Three Markit pattern tests following existing `DemoTestBase` conventions:
1. IF=Function, THEN=Function
2. IF=DB Lookup, THEN=Direct
3. RefLookup (no condition, THEN=Lookup)

Plus a mixed test: SpEL guard + lookup condition + function mapping.

---

## Alternatives Considered

### Pre-enrichments block on MappingRule (rejected)

A `pre-enrichments` list executing before conditions — data-gathering as an explicit phase.

**Rejected because:**
- Introduces a procedural execution phase ("run these steps first") into the otherwise composable YAML flow
- New concept with no precedent in APEX
- Pre-enrichments always execute even if conditions will fail (wasted work)
- Context mutations from failed rules persist for later rules

### Two-pass pattern — lookup as earlier mapping rule (rejected)

Split into two mapping rules: first does an unconditional function/lookup to populate context, second checks the result conditionally.

**Rejected because:**
- Two rules for one logical operation — the same implicit coupling fragility that the `"function"` mapping type was designed to eliminate
- Requires `stop-on-first-match: false` for the first rule, conflicting with default behavior
- Not self-documenting as a single operation

### Nested `action` block on ConditionRule (deferred)

Group resolution config under an `action:` key instead of flat fields.

**Deferred because:**
- Adds a nesting level in YAML
- Inconsistent with how `MappingConfig` structures its fields (flat)
- Can be introduced later as an alternative syntax if YAML complexity grows
