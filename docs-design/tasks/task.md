
I tested latest fix for improved reference loading, but I am still seeing a gap for data-source-refs and some nested enrichment-refs. In our setup, YAMLs are externalized in a separate shared config repository, and stage YAMLs live under folders like:

configuration/FX/NEW/

configuration/DB/

configuration/RuleBuilder/

 

With the current resolver behavior, references such as:

data-source-refs:

  - name: "postgres-db"

    source: "DB/database-postgres.yaml"

 

enrichment-refs:

  - name: "Map Currency Code"

    source: "RuleBuilder/RB_Map_Currency_Code.yaml"

 

do not resolve correctly when the parent YAML is under NEW. They only work if I manually change them to forms like:

../../DB/database-postgres.yaml

../../RuleBuilder/RB_Map_Currency_Code.yaml

 

That workaround works, but it makes the YAMLs brittle and harder to maintain.

 

My understanding is that the current logic resolves references:

1.            as-is

2.            relative to the current YAML’s sourceDirectory

For a file in NEW, that means:

•              DB/database-postgres.yaml becomes configuration/FX/NEW/DB/database-postgres.yaml

•              RuleBuilder/RB_Map_Currency_Code.yaml becomes configuration/FX/NEW/RuleBuilder/RB_Map_Currency_Code.yaml

Those locations are not correct in a shared config-repo layout where DB and RuleBuilder are sibling folders under configuration.

 

Proposed fix:

For non-absolute references that are not found as-is, resolve them against:

1.            the current YAML directory

2.            then each ancestor directory in turn

So for a YAML loaded from configuration/FX/NEW/file.yaml, the resolver would try:

configuration/FX/NEW/<ref>

configuration/FX/<ref>

configuration/<ref>

...

 

This allows short references like:

•              DB/database-postgres.yaml

•              RuleBuilder/RB_Map_Currency_Code.yaml

to work naturally without hardcoding repository names and without requiring stm-ffs.

# Rule-Level `conditions` Not Scanned by ItemOrderProcessor

## Problem

When a YAML rule defines a structured `conditions` block containing a function condition with `enrichment-group-ref`, the referenced enrichment group is not filtered from `itemOrder`. This causes the group to auto-execute at its definition position instead of executing only when invoked by the function condition at runtime.

```yaml
rules:
  - id: "trade-currency-pass"
    conditions:
      operator: "OR"
      rules:
        - type: "function"
          enrichment-group-ref: "Is_Map_to_Internal_Code_Available_RB"   # ← invisible to ItemOrderProcessor
          output-field: "validation_status"
          condition: "#validation_status != null"
    condition: "#validation_status != null"
```

The enrichment group `Is_Map_to_Internal_Code_Available_RB` appears in `itemOrder` and runs unconditionally at its definition position, breaking the intended on-demand invocation pattern.

## Root Causes

Two independent defects combine to produce this bug:

1. **Missing model field.** `YamlRule` has a `condition` field (a SpEL string) but no `conditions` field (a `ConditionGroup`). Jackson silently drops the entire `conditions` block during deserialization because `OrderedYamlParser` configures `FAIL_ON_UNKNOWN_PROPERTIES = false`. The data never reaches the engine.

2. **Missing scan path.** `ItemOrderProcessor.applyGroupsOnlyLogic()` collects `enrichment-group-ref` values from enrichment mapping-rules and conditional-mappings, but never iterates `config.getRules()`. Even if `conditions` were parsed, rule-level function references would still be missed.

## Impact

Any enrichment group referenced by a rule-level function condition auto-executes at its YAML definition position. For groups that perform database lookups, REST calls, or expensive computations, this produces incorrect ordering, redundant execution, and potentially wrong results when the group's output is consumed before prerequisite data is available.

## Fix

**`YamlRule.java`** — Added `conditions` field of type `YamlEnrichment.ConditionGroup` with `@JsonProperty("conditions")`, getter, and setter. Jackson now deserializes the structured conditions block into the model.

**`ItemOrderProcessor.java`** — Added a scan block in `applyGroupsOnlyLogic()` that iterates `config.getRules()` and calls the existing `collectFunctionConditionGroupRefs()` helper on each rule's `conditions`. This reuses the same extraction logic already applied to enrichment mapping-rules and conditional-mappings.

```java
// New block in applyGroupsOnlyLogic()
if (config.getRules() != null && !config.getRules().isEmpty()) {
    for (YamlRule rule : config.getRules()) {
        collectFunctionConditionGroupRefs(rule.getConditions(), referencedEnrichmentGroupIds);
    }
}
```

## Verification

A new test (`RuleConditionRefsTest`) confirms:

| Assertion | Result |
|-----------|--------|
| Group referenced by rule-level function condition is filtered from `itemOrder` | PASS |
| Unreferenced group remains in `itemOrder` and auto-executes | PASS |

Existing tests (`FunctionConditionRefsTest`, `FunctionMappingRefsTest`) pass with zero regressions — 12/12 green.

## Files Changed

| File | Change |
|------|--------|
| `apex-core/.../model/YamlRule.java` | Added `conditions` field, getter, setter |
| `apex-core/.../loader/ItemOrderProcessor.java` | Added rule-scanning block + `YamlRule` import |
| `apex-demo/.../conditional/RuleConditionRefsTest.java` | New test class |
| `apex-demo/.../conditional/RuleConditionRefsTest.yaml` | New test configuration |


