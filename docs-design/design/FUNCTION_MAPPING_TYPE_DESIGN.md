# Design: `function` Mapping Type for Conditional Mapping Enrichment

**Status:** Implemented  
**Date:** 2026-03-17 (implemented 2026-03-18)  
**Module:** apex-core

---

## Problem

Invoking a reusable enrichment group as a function requires two separate, implicitly coupled enrichments:

1. A `field-enrichment` to map input parameters into the enrichment group's context variables
2. A second `field-enrichment` to extract the result back into the target field

There is no explicit call/invoke relationship — the coupling is purely by naming convention. This is fragile, verbose, and not self-documenting.

### Current Pattern (2 enrichments per function call)

```yaml
# Enrichment 1: set up input parameters
- id: is-ndf-r2-rbparams
  type: field-enrichment
  condition: '#is_ndf_r2_cond_result ?: false'
  field-mappings:
    - source-field: constant
      target-field: "#map_external_to_internal_code.Translation_Type"
      expression: "'IS_NDF'"
    - source-field: "#control.client_code"
      target-field: "#map_external_to_internal_code.Client_Code"
    # ... more parameter mappings

# Enrichment 2: extract result
- id: is-ndf-r2-then
  type: field-enrichment
  condition: '#is_ndf_r2_cond_result ?: false'
  field-mappings:
    - source-field: Map_External_to_Internal_Code_rulebuilder
      target-field: "#fx.is_ndf"
```

**Issues:**
- Implicit coupling between the two enrichments (only connected by variable naming)
- Duplicated condition expressions
- No way to tell from YAML that these form a single logical operation
- Brittle if enrichments are reordered or separated

---

## Existing Construct Analysis

Five existing APEX YAML constructs were evaluated for their ability to support the conditional function-call pattern (conditionally invoke an enrichment group with bound input parameters and extract a specific output field).

### 1. Enrichment Group References (`enrichment-group-references`)

Static composition at load time. Referenced group enrichments are flattened into the parent group and always execute. No support for conditional invocation, input parameter binding, or output extraction.

```yaml
enrichment-groups:
  - id: "composite"
    enrichment-ids: [ "is-ndf-r2-rbparams" ]
    enrichment-group-references: [ "Map_External_to_Internal_Code_RB" ]
```

**Gap:** Solves reuse and ordering, but not conditional function-call semantics.

### 2. Field Enrichment with `conditional-mappings`

Applies different field mappings based on conditions within a single enrichment. Cannot invoke another enrichment group — field mappings are direct field assignments only.

```yaml
enrichments:
  - id: "conditional-field-mapping"
    type: "field-enrichment"
    conditional-mappings:
      - condition: "#status == 'A'"
        field-mappings:
          - target-field: "displayStatus"
            expression: "'Active'"
```

**Gap:** Handles conditional field assignment, but has no delegation/invocation capability.

### 3. The Current 2-Enrichment Pattern (what exists today)

Two separate `field-enrichment` entries — one to set up input parameters, one to extract the result — with the enrichment group executing between them via group ordering. **This is functionally equivalent** to the proposed feature. The limitations are:
- Implicit coupling (naming convention only)
- Duplicated conditions
- Not self-documenting as a single logical operation
- Brittle to reordering

**This is exactly the pattern the proposal replaces.**

### 4. Rule Chains (`sequential-dependency`)

Executes rules in strict sequence with output variables flowing between stages. Operates on **rules** (condition → message/severity), not enrichments. Cannot execute enrichment groups or bind input/output parameters.

**Gap:** Similar staged-execution concept, but scoped to rules not enrichments.

### 5. Components (`component-refs`)

Groups multiple YAML files into reusable units. Operates at the **file level** — cannot pass parameters or extract outputs. A deployment/organization mechanism, not a runtime invocation mechanism.

**Gap:** Wrong granularity for enrichment-level function calls.

### Conclusion

No existing APEX YAML construct supports the specific pattern: *conditionally invoke an enrichment group with bound input parameters and extract a specific output field*. The closest workaround is pattern #3, which achieves functional equivalence through implicit coupling and manual orchestration. The proposed `"function"` mapping type makes this pattern explicit, self-contained, and less brittle.

---

## Proposed Solution

Add `"function"` as a third `mapping.type` value alongside `"direct"` and `"lookup"` within the existing `conditional-mapping-enrichment` type.

### Proposed YAML Schema

```yaml
- id: "conditional-ndf-mapping"
  type: "conditional-mapping-enrichment"
  name: "NDF Conditional Field Mapping"
  enabled: true
  target-field: "IS_NDF"

  mapping-rules:
    # Existing types still work unchanged
    - id: "direct-fallback"
      priority: 10
      conditions:
        operator: "AND"
        rules:
          - condition: "#IS_NDF == '1'"
      mapping:
        type: "direct"
        expression: "'YES'"

    # NEW: function mapping type
    - id: "translate-via-function"
      priority: 20
      conditions:
        operator: "AND"
        rules:
          - condition: "#IS_NDF != null"
      mapping:
        type: "function"
        enrichment-group-ref: "Map_External_to_Internal_Code_RB"
        input-parameters:
          - source-field: "constant"
            target-field: "#map_external_to_internal_code.Translation_Type"
            expression: "'IS_NDF'"
          - source-field: "#control.client_code"
            target-field: "#map_external_to_internal_code.Client_Code"
          - source-field: "#control.system_code"
            target-field: "#map_external_to_internal_code.System_Code"
          - source-field: "#fx.is_ndf"
            target-field: "#map_external_to_internal_code.External_Code"
          - source-field: "constant"
            target-field: "#map_external_to_internal_code.External_Subcode"
            expression: "'NULL'"
          - source-field: "constant"
            target-field: "#map_external_to_internal_code.exec_flag"
            expression: "'true'"
        output-field: "Map_External_to_Internal_Code_rulebuilder"
```

**Read as:** *When `#IS_NDF != null`, call `Map_External_to_Internal_Code_RB` with these inputs, extract the result from `Map_External_to_Internal_Code_rulebuilder`, and write it to `IS_NDF`.*

---

## Design Decisions

### 1. Single `enrichment-group-ref` (not a list)

One mapping rule invokes one enrichment group. Chain multiple groups via multiple mapping rules with different priorities. Keeps execution simple: one call, one result.

**Departure from original proposal:** The original request used `enrichment-group-references` (plural, list) consistent with the existing `YamlEnrichmentGroup` schema. This design simplifies to a singular string because a single mapping rule should produce a single mapped value for `target-field`. If multi-group invocation is needed, use multiple mapping rules.

### 2. `input-parameters` reuses existing `FieldMapping` structure

Same `source-field` / `target-field` / `expression` as `field-enrichment` field-mappings. No new schema to learn.

**Note:** `FieldAccessor.applyFieldMappings(List<FieldMapping>, sourceObject, targetObject)` is not directly reusable for input-parameter binding. That method expects a `sourceObject` for field lookups (e.g., a lookup result row), whereas `input-parameters` bind constants and SpEL context variables into context variables — there is no distinct source object. A new helper or an overload that operates purely against the SpEL `EvaluationContext` is needed for this use case.

### 3. `output-field` (single string, not a list)

The conditional mapping enrichment already has a `target-field` at the top level. The function just declares *which field to extract* from the invoked group's context:

```
context[output-field] → target-field
```

**Departure from original proposal:** The original request used `output-parameters` (list of `FieldMapping`) which allows mapping to a target other than the enrichment's `target-field`. This design simplifies to a single `output-field` string since the conditional mapping enrichment already defines a single `target-field`. If multi-output use cases emerge, `output-parameters` (list of field mappings) can be added later as a backward-compatible extension.

---

## Execution Flow

```
Conditional Mapping Enrichment
│
├─ for each mapping-rule (by priority):
│   ├─ Evaluate conditions
│   └─ If matched:
│       ├─ type: "direct"    → applyDirectMapping()      [existing]
│       ├─ type: "lookup"    → applyLookupMapping()       [existing, stub — not fully implemented]
│       └─ type: "function"  → applyFunctionMapping()     [NEW]
│           ├─ 1. Apply input-parameters into SpEL context
│           ├─ 2. Resolve enrichment-group-ref from config
│           ├─ 3. Execute group via EnrichmentGroupExecutor
│           ├─ 4. Extract output-field from context
│           └─ 5. Return value → written to target-field
│
└─ If stop-on-first-match → break
```

---

## Implementation Scope

### Model Changes

**`YamlEnrichment.MappingConfig`** — 3 new optional fields:

| Field | Type | Description |
|-------|------|-------------|
| `enrichment-group-ref` | `String` | ID of enrichment group to invoke |
| `input-parameters` | `List<FieldMapping>` | Parameter bindings applied before invocation |
| `output-field` | `String` | Field name to extract from context after invocation |

No new classes. `FieldMapping` already exists as an inner class of `YamlEnrichment`.

### Processor Changes

**`EnrichmentProcessor`**
- Add third branch in `applyMappingRule()`: `"function"` → `applyFunctionMapping()`
- New method `applyFunctionMapping(MappingConfig, Object)`:
  1. Apply `input-parameters` via `FieldAccessor`
  2. Resolve group from `YamlRuleConfiguration`
  3. Delegate to `EnrichmentGroupExecutor.processEnrichmentGroup()`
  4. Extract `output-field` value
  5. Return value

**Dependency injection:** `EnrichmentProcessor` needs a reference to `EnrichmentGroupExecutor` (does not have one today). Today the dependency is one-way: `EnrichmentGroupExecutor` (package `dev.mars.apex.engine.execution`) holds an `EnrichmentProcessor` reference and calls `processEnrichmentWithResult()`. Adding the reverse reference creates a **circular object dependency** (`EnrichmentProcessor` → `EnrichmentGroupExecutor` → `EnrichmentProcessor`). Constructor injection would fail. Use **setter injection** or a **lazy provider** (e.g., `Supplier<EnrichmentGroupExecutor>`) to break the initialization cycle — set the reference after both objects are constructed.

### Additional Plumbing Changes

**`processConditionalMappingEnrichment()` signature:** Today this method takes `(YamlEnrichment, Object)` — it does not receive a `YamlRuleConfiguration`. But `applyFunctionMapping()` needs the configuration to resolve the enrichment group by ID. Similarly, `EnrichmentGroupExecutor.processEnrichmentGroup()` requires a `YamlRuleConfiguration` parameter. The `configuration` must be threaded from `processEnrichment()` (which already has it) through `processConditionalMappingEnrichment()` to `applyMappingRule()` and into `applyFunctionMapping()`. This is a signature change to existing private methods.

### No Changes Required

- `EnrichmentGroupFactory` — resolution logic unchanged
- `EnrichmentGroupExecutor` — called as-is, no modifications
- Existing `"direct"` and `"lookup"` mapping types — untouched

### Changes Required (not previously listed)

- `FieldAccessor` — needs a new overload or helper for input-parameter binding against `EvaluationContext` (see Design Decision #2)

---

## Error Handling

| Error | Behavior |
|-------|----------|
| `enrichment-group-ref` not found | Log warning, return `null` |
| Input parameter mapping fails | Log warning per field, continue with partial params |
| Enrichment group execution fails | Respect group's own failure settings; try `fallback-value` if configured |
| `output-field` not found in context | Return `null`, log warning |
| Recursion depth exceeded | Fail with error, configurable max depth (default 5, matching component nesting limit). Track depth via a `ThreadLocal<Integer>` counter incremented on entry to `applyFunctionMapping()` and decremented on exit. `EnrichmentGroupExecutor.processEnrichmentGroup()` has no depth parameter today, so the counter must be external to the call chain. |

---

## Backward Compatibility

- Existing `"direct"` and `"lookup"` types unchanged
- `MappingConfig` gets optional fields with `@JsonProperty` annotations — new fields are simply absent in old YAML files and default to `null`
- **Note:** `ConfigurationLoader.createYamlMapper()` sets `FAIL_ON_UNKNOWN_PROPERTIES = true` (strict validation), while `OrderedYamlParser` and `ComponentLoader` set it to `false`. Since the new fields are added to the model class (`MappingConfig`), they are *known* properties — deserialization succeeds regardless of strict mode. No loader changes needed.
- No changes to group resolution or execution

---

## Alternative Considered

**Standalone `type: "function-enrichment"`** at the enrichment level (not inside conditional mapping).

Rejected because:
- Duplicates condition evaluation logic already in conditional mapping
- Function calls are inherently conditional (always gated by a condition in the current usage)
- Keeping it within conditional mapping allows mixing function/direct/lookup rules in the same priority chain

Can be added later as a thin wrapper if unconditional function calls are needed.

---

## Resolved Design Notes

1. **SpEL context model:** `input-parameters` mutate the caller's `targetObject` (shared state). Today enrichments in a group share the same `targetObject` reference but the SpEL `EvaluationContext` is **rebuilt per enrichment** from that object (see `EnrichmentProcessor.createEvaluationContext()`). The current 2-enrichment pattern communicates through mutations on the shared `targetObject` (e.g., setting `#map_external_to_internal_code.*` fields), not through a shared SpEL context. The function mapping follows the same model: mutate `targetObject` via `input-parameters`, invoke the group (which reads from the same `targetObject`), then extract `output-field` from the mutated `targetObject`. No context cloning needed.

2. **`enrichment-group-ref` resolution — build on demand (Option A1):** The enrichment group index (`Map<String, EnrichmentGroup>`) is built in `SequentialProcessor.buildEnrichmentGroupIndex()` at evaluation time and cached in `PreparedProcessingState`. `EnrichmentProcessor` has no access to this index — it sits at the bottom of the dependency chain (`RulesEngine` → `EnrichmentProcessor` → `EnrichmentGroupExecutor` → `SequentialProcessor`).

   Three options were considered:

   | Option | Approach | Pro | Con |
   |--------|----------|-----|-----|
   | **A1: Build on demand** | Call `EnrichmentGroupFactory.buildEnrichmentGroups(config)` inside `applyFunctionMapping()` | No new fields, no mutable state, no lifecycle coupling | Rebuilds index per function call (cheap — list-to-map, no I/O) |
   | **A2: Thread pre-built index** | Pass `Map<String, EnrichmentGroup>` through 4 private method signatures | Avoids rebuild | Signature changes cascade through internal methods |
   | **B: Setter injection** | `setEnrichmentGroupIndex()` on `EnrichmentProcessor`, called by `SequentialProcessor` before each evaluation | No signature changes | Temporal coupling, not thread-safe (same issue as existing `RuleResultTracker`) |
   | **C: Store on `YamlRuleConfiguration`** | Add transient `@JsonIgnore` field on the config model | Config already threaded everywhere | Mixes runtime state into config model; creates dependency from config layer to engine layer |

   **Decision: Option A1.** Build on demand inside `applyFunctionMapping()`. The `YamlRuleConfiguration` is already being threaded through the method chain (identified as a required plumbing change). `EnrichmentGroupFactory.buildEnrichmentGroups()` is cheap (iterates YAML groups, no I/O). This adds zero coupling and keeps `EnrichmentProcessor` stateless with respect to group resolution. If profiling shows a bottleneck, upgrade to A2 without changing any external contract.

   ```java
   private Object applyFunctionMapping(MappingConfig mapping, Object targetObject,
                                        YamlRuleConfiguration config) {
       List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
       EnrichmentGroup target = groups.stream()
           .filter(g -> g.getId().equals(mapping.getEnrichmentGroupRef()))
           .findFirst().orElse(null);

       if (target == null) {
           logger.warn("enrichment-group-ref '{}' not found", mapping.getEnrichmentGroupRef());
           return null;
       }
       // ... apply input-parameters, execute group, extract output-field
   }
   ```
