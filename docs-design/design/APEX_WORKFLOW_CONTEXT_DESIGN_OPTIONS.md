# APEX Workflow Context Design Options

## Document Information

| Field | Value |
|-------|-------|
| **Document ID** | APEX-DESIGN-2026-001 |
| **Version** | 1.0.0 |
| **Status** | Draft - For Discussion |
| **Author** | APEX Development Team |
| **Created** | 2026-01-12 |
| **Last Updated** | 2026-01-12 |

---

## 1. Executive Summary

This document outlines design options for implementing temporary inter-stage workflow variables in the APEX Rules Engine. The requirement stems from the need to pass temporary values (such as condition evaluation results) between scenario processing stages without polluting the final `inputData` map.

### Problem Statement

Currently, all stage outputs are merged directly into the `inputData` map:

```java
// In ScenarioStageExecutor.java (lines 163-173)
if (!stageResult.getStageOutputs().isEmpty()) {
    dataMap.putAll(stageResult.getStageOutputs());
}
```

This means:
- **Every enrichment result** ends up in the final data
- **Temporary calculation values** pollute the output
- **Condition evaluation results** cannot be stored and reused without affecting final data
- **No separation** between workflow-internal variables and business data

### Requirements

1. Temporary values from one stage must be accessible to subsequent stages
2. Temporary values must NOT appear in the final `inputData`/`dataObjectMap`
3. Solution should be declarative (YAML-driven)
4. Solution should follow APEX's clean architecture principles
5. Backward compatibility with existing configurations

---

## 2. Current Architecture Analysis

### 2.1 Data Flow in Stage Execution

```
┌─────────────────────────────────────────────────────────────────┐
│                    ScenarioStageExecutor                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Stage 1 Execution                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │  Input Data  │───▶│   Execute    │───▶│ Stage Outputs│      │
│  │  (Map)       │    │   Rules      │    │              │      │
│  └──────────────┘    └──────────────┘    └──────┬───────┘      │
│                                                  │               │
│                            ┌─────────────────────┘               │
│                            ▼                                     │
│                    dataMap.putAll(stageOutputs)  ◀── PROBLEM    │
│                            │                                     │
│                            ▼                                     │
│  Stage 2 Execution                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │  Input Data  │───▶│   Execute    │───▶│ Stage Outputs│      │
│  │  + Stage1    │    │   Rules      │    │              │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Key Code Locations

| File | Method | Description |
|------|--------|-------------|
| `ScenarioStageExecutor.java` | `executeStages()` | Main stage orchestration |
| `ScenarioStageExecutor.java` | `createFactsMap()` | Prepares data for rule evaluation |
| `ScenarioStageExecutor.java` | `filterScenarioMetadata()` | Filters injected metadata |
| `StageExecutionResult.java` | `stageOutputs` | Stores stage output data |
| `ScenarioExecutionResult.java` | `scenarioOutputs` | Final scenario outputs |

### 2.3 Existing Patterns for Temporary Data

#### Pattern: calculation-config with result-field

```yaml
enrichments:
  - id: "trade-value-enrichment"
    type: "calculation-enrichment"
    calculation-config:
      expression: "#tradeValue > 1000000 ? 'LARGE' : 'SMALL'"
      result-field: "valueCategory"
    field-mappings:
      - source-field: "valueCategory"
        target-field: "valueCategory"
```

**Limitation**: Result still ends up in `inputData`.

#### Pattern: remove-field transformation

```yaml
transformations:
  - id: "cleanup"
    transformation-rules:
      - condition: "true"
        actions:
          - type: "remove-field"
            field: "temporaryFlag"
```

**Limitation**: Requires explicit cleanup, error-prone.

---

## 3. Design Options

### 3.1 Option 1: Dedicated workflowContext Map

#### Concept

Add a dedicated `workflowContext` map to `ScenarioExecutionResult` that is:
- Available to all stages for reading/writing
- Automatically excluded from final output
- Explicitly marked in YAML with `scope: "workflow"`

#### Implementation

**ScenarioExecutionResult.java changes:**

```java
public class ScenarioExecutionResult {
    // Existing fields...
    
    /** Workflow-scoped variables for inter-stage communication */
    private final Map<String, Object> workflowContext = new ConcurrentHashMap<>();
    
    public void setWorkflowVariable(String key, Object value) {
        workflowContext.put(key, value);
        logger.debug("Set workflow variable: {} = {}", key, value);
    }
    
    public Object getWorkflowVariable(String key) {
        return workflowContext.get(key);
    }
    
    public Map<String, Object> getWorkflowContext() {
        return Collections.unmodifiableMap(workflowContext);
    }
    
    public boolean hasWorkflowVariable(String key) {
        return workflowContext.containsKey(key);
    }
}
```

**ScenarioStageExecutor.java changes:**

```java
private FactsWithMetadata createFactsMap(Object data, ScenarioExecutionResult context) {
    Map<String, Object> facts = new HashMap<>();
    Set<String> originalInputKeys = new HashSet<>();
    
    // Existing code...
    
    // NEW: Add workflow context variables (accessible but filtered)
    facts.put("workflowVars", context.getWorkflowContext());
    
    // Also make workflow vars available at top level with prefix
    context.getWorkflowContext().forEach((key, value) -> {
        facts.put("workflow_" + key, value);
    });
    
    return new FactsWithMetadata(facts, originalInputKeys);
}
```

**YAML Usage:**

```yaml
enrichments:
  - id: "calculate-risk-flag"
    type: "calculation-enrichment"
    calculation-config:
      expression: "#amount > 10000000"
      result-field: "isHighRisk"
      scope: "workflow"  # NEW ATTRIBUTE
```

**Access in subsequent stage:**

```yaml
processing-stages:
  - stage-name: "high-risk-processing"
    condition: "#workflowVars['isHighRisk'] == true"
    # OR: condition: "#workflow_isHighRisk == true"
```

#### Pros

| Advantage | Description |
|-----------|-------------|
| Clean Separation | Clear distinction between business data and workflow data |
| Explicit | YAML explicitly marks what's temporary |
| Debuggable | Easy to inspect workflow variables at any point |
| Backward Compatible | Existing configs without `scope` work unchanged |

#### Cons

| Disadvantage | Description |
|--------------|-------------|
| Schema Change | Requires new `scope` attribute in YAML |
| Core Modification | Modifies `ScenarioExecutionResult` |
| Learning Curve | Users must learn new pattern |

#### Effort Estimate

- **Development**: 2-3 days
- **Testing**: 2 days
- **Documentation**: 1 day

---

### 3.2 Option 2: Stage-Level transientOutputs vs persistentOutputs

#### Concept

Split `StageExecutionResult.stageOutputs` into two separate maps:
- `persistentOutputs`: Merged to inputData (current behavior)
- `transientOutputs`: Available to next stage, not merged to final data

#### Implementation

**StageExecutionResult.java changes:**

```java
public class StageExecutionResult {
    // Existing fields...
    
    /** Outputs that persist to inputData */
    private Map<String, Object> persistentOutputs = new ConcurrentHashMap<>();
    
    /** Outputs available to next stage only, not persisted */
    private Map<String, Object> transientOutputs = new ConcurrentHashMap<>();
    
    public void addPersistentOutput(String key, Object value) {
        persistentOutputs.put(key, value);
    }
    
    public void addTransientOutput(String key, Object value) {
        transientOutputs.put(key, value);
    }
    
    public Map<String, Object> getPersistentOutputs() {
        return new HashMap<>(persistentOutputs);
    }
    
    public Map<String, Object> getTransientOutputs() {
        return new HashMap<>(transientOutputs);
    }
    
    // Deprecated: for backward compatibility
    @Deprecated
    public Map<String, Object> getStageOutputs() {
        Map<String, Object> combined = new HashMap<>(persistentOutputs);
        combined.putAll(transientOutputs);
        return combined;
    }
}
```

**ScenarioStageExecutor.java changes:**

```java
// In executeStages() method
if (data instanceof Map && stageResult.isSuccessful()) {
    Map<String, Object> dataMap = (Map<String, Object>) data;
    
    // Only merge persistent outputs
    if (!stageResult.getPersistentOutputs().isEmpty()) {
        dataMap.putAll(stageResult.getPersistentOutputs());
    }
    
    // Transient outputs available via context, not merged to data
}
```

**YAML Usage:**

```yaml
enrichments:
  - id: "condition-result"
    type: "field-enrichment"
    field-mappings:
      - source-field: "constant"
        expression: "#region == 'US'"
        target-field: "isUSRegion"
        transient: true  # NEW ATTRIBUTE
```

#### Pros

| Advantage | Description |
|-----------|-------------|
| Per-Field Control | Granular control over each field |
| Natural Extension | Extends existing field-mappings pattern |
| Minimal Core Changes | Changes localized to stage results |

#### Cons

| Disadvantage | Description |
|--------------|-------------|
| Complexity | Two maps to manage per stage |
| Migration | Need to handle backward compatibility carefully |
| Scattered Declaration | Transient flag at field level, not centralized |

#### Effort Estimate

- **Development**: 3-4 days
- **Testing**: 2-3 days
- **Documentation**: 1 day

---

### 3.3 Option 3: Namespace Prefix Convention

#### Concept

Use a naming convention where fields with a specific prefix (e.g., `_` or `temp_`) are automatically filtered from final output.

#### Implementation

**ScenarioStageExecutor.java changes:**

```java
private static final String TRANSIENT_PREFIX = "_";

private Map<String, Object> filterTransientFields(Map<String, Object> outputs) {
    return outputs.entrySet().stream()
        .filter(e -> !e.getKey().startsWith(TRANSIENT_PREFIX))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
}

// In executeStages()
if (data instanceof Map && stageResult.isSuccessful()) {
    Map<String, Object> dataMap = (Map<String, Object>) data;
    
    // Filter transient fields before merging
    Map<String, Object> persistentOutputs = filterTransientFields(stageResult.getStageOutputs());
    if (!persistentOutputs.isEmpty()) {
        dataMap.putAll(persistentOutputs);
    }
}
```

**YAML Usage:**

```yaml
enrichments:
  - id: "temp-calculation"
    type: "calculation-enrichment"
    calculation-config:
      expression: "#amount > 10000000"
      result-field: "_isHighRisk"  # Underscore prefix = transient
```

**Access in subsequent stage:**

```yaml
processing-stages:
  - stage-name: "high-risk-processing"
    condition: "#_isHighRisk == true"
```

#### Pros

| Advantage | Description |
|-----------|-------------|
| No Schema Changes | No new YAML attributes needed |
| Simple Implementation | Minimal code changes |
| Convention-Based | Works with existing patterns |
| Backward Compatible | No breaking changes |

#### Cons

| Disadvantage | Description |
|--------------|-------------|
| Implicit Behavior | Magic prefix may be confusing |
| Name Collision Risk | Users might accidentally use prefix |
| Less Discoverable | Not obvious from YAML what's transient |
| Validation Harder | Can't statically validate transient usage |

#### Effort Estimate

- **Development**: 0.5-1 day
- **Testing**: 1 day
- **Documentation**: 0.5 day

---

### 3.4 Option 4: Explicit workflow-variables Section

#### Concept

Add a dedicated `workflow-variables` section to stage configuration for declaring workflow-scoped variables.

#### Implementation

**ScenarioStage.java changes:**

```java
public class ScenarioStage {
    // Existing fields...
    
    @JsonProperty("workflow-variables")
    private List<WorkflowVariable> workflowVariables;
    
    public List<WorkflowVariable> getWorkflowVariables() {
        return workflowVariables != null ? workflowVariables : Collections.emptyList();
    }
}

public class WorkflowVariable {
    private String name;
    private String expression;
    private String description;
    
    // Getters/setters...
}
```

**ScenarioStageExecutor.java changes:**

```java
private void evaluateWorkflowVariables(ScenarioStage stage, Map<String, Object> facts, 
                                        ScenarioExecutionResult context) {
    for (WorkflowVariable var : stage.getWorkflowVariables()) {
        try {
            Object value = expressionEvaluator.evaluateWithEnhancedContext(
                var.getExpression(), facts, Object.class);
            context.setWorkflowVariable(var.getName(), value);
            logger.info("Set workflow variable '{}' = {}", var.getName(), value);
        } catch (Exception e) {
            logger.warn("Failed to evaluate workflow variable '{}': {}", 
                       var.getName(), e.getMessage());
        }
    }
}
```

**YAML Usage:**

```yaml
processing-stages:
  - stage-name: "classification"
    config-file: "classification-rules.yaml"
    execution-order: 1
    workflow-variables:
      - name: "isHighValue"
        expression: "#notionalAmount > 10000000"
        description: "Flag for high-value trade processing"
      - name: "requiresReview"
        expression: "#counterparty == 'NEW' && #amount > 500000"
        description: "Flag for manual review requirement"
      - name: "regionCode"
        expression: "#region == 'US' ? 'NA' : (#region == 'EMEA' ? 'EU' : 'APAC')"
        description: "Normalized region code for routing"
    
  - stage-name: "high-value-processing"
    config-file: "high-value-rules.yaml"
    execution-order: 2
    condition: "#workflowVars['isHighValue'] == true"  # Uses workflow variable
```

#### Pros

| Advantage | Description |
|-----------|-------------|
| Very Explicit | Clear what's workflow-scoped |
| Self-Documenting | Variables with descriptions in one place |
| Centralized | All workflow vars for a stage in one section |
| Flexible | Any SpEL expression supported |

#### Cons

| Disadvantage | Description |
|--------------|-------------|
| New YAML Structure | New section to parse and validate |
| Stage Coupling | Variables tied to specific stages |
| Potential Duplication | May duplicate some enrichment logic |

#### Effort Estimate

- **Development**: 2-3 days
- **Testing**: 2 days
- **Documentation**: 1 day

---

### 3.5 Option 5: ScenarioExecutionResult as SpEL Evaluation Context

#### Concept

Make `ScenarioExecutionResult` directly accessible in SpEL expressions, allowing programmatic read/write of workflow state.

#### Implementation

**ScenarioExecutionResult.java changes:**

```java
public class ScenarioExecutionResult {
    private final Map<String, Object> workflowContext = new ConcurrentHashMap<>();
    
    // Methods accessible via SpEL
    public void setVar(String key, Object value) {
        workflowContext.put(key, value);
    }
    
    public Object getVar(String key) {
        return workflowContext.get(key);
    }
    
    public boolean hasVar(String key) {
        return workflowContext.containsKey(key);
    }
}
```

**ScenarioStageExecutor.java changes:**

```java
private FactsWithMetadata createFactsMap(Object data, ScenarioExecutionResult context) {
    Map<String, Object> facts = new HashMap<>();
    // ... existing code ...
    
    // Make context available with methods accessible
    facts.put("workflow", context);  // Allows #workflow.setVar(), #workflow.getVar()
    
    return new FactsWithMetadata(facts, originalInputKeys);
}
```

**YAML Usage:**

```yaml
enrichments:
  - id: "set-workflow-var"
    type: "calculation-enrichment"
    calculation-config:
      # Side-effect: sets workflow variable
      expression: "#workflow.setVar('isUSRegion', #region == 'US')"
      result-field: "_ignored"

  - id: "conditional-enrichment"
    type: "field-enrichment"
    condition: "#workflow.getVar('isUSRegion') == true"
    field-mappings:
      - source-field: "constant"
        expression: "'US-COMPLIANT'"
        target-field: "complianceStatus"
```

**Alternative: Method chaining**

```yaml
calculation-config:
  # Returns the value after setting it
  expression: "#workflow.setVar('riskLevel', #amount > 1000000 ? 'HIGH' : 'LOW')"
```

#### Pros

| Advantage | Description |
|-----------|-------------|
| Flexible | Full programmatic control |
| No New Syntax | Uses existing SpEL infrastructure |
| Dynamic | Can conditionally set variables |
| Powerful | Complex logic possible |

#### Cons

| Disadvantage | Description |
|--------------|-------------|
| Side Effects | Expressions with side effects (impure) |
| Less Declarative | More like programming than configuration |
| Harder to Validate | Static analysis difficult |
| Error Prone | Easy to make mistakes |

#### Effort Estimate

- **Development**: 1 day
- **Testing**: 1-2 days
- **Documentation**: 1 day

---

## 4. Comparison Matrix

| Criteria | Option 1 | Option 2 | Option 3 | Option 4 | Option 5 |
|----------|----------|----------|----------|----------|----------|
| **Name** | workflowContext | transient/persistent | Prefix Convention | workflow-variables | SpEL Context |
| **Explicitness** | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| **Simplicity** | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Backward Compat** | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Declarative** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| **Flexibility** | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Maintainability** | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |
| **Dev Effort** | Medium | Medium-High | Low | Medium | Low |

---

## 5. Recommendation

### Primary Recommendation: Option 1 + Option 4 (Hybrid)

Combine the strengths of Options 1 and 4:

1. **Storage**: Use `workflowContext` map in `ScenarioExecutionResult` (Option 1)
2. **Declaration**: Support both:
   - `scope: "workflow"` attribute on enrichments (Option 1)
   - `workflow-variables` section on stages (Option 4)

#### Rationale

- **Option 1** provides clean storage mechanism
- **Option 4** provides most declarative, self-documenting YAML syntax
- Combined approach offers flexibility: simple cases use `scope`, complex cases use `workflow-variables`
- Both approaches are explicit and easy to understand

### Implementation Priority

1. **Phase 1**: Implement Option 1 (`scope: "workflow"`)
   - Add `workflowContext` to `ScenarioExecutionResult`
   - Add `scope` attribute to enrichments
   - Update `ScenarioStageExecutor` to handle workflow scope
   
2. **Phase 2**: Implement Option 4 (`workflow-variables`)
   - Add `workflow-variables` section to `ScenarioStage`
   - Add evaluation logic for stage-level workflow variables

3. **Phase 3** (Optional): Implement Option 3 (prefix convention)
   - As additional convenience for quick prototyping
   - Documented as "shortcut" approach

---

## 6. Open Questions

1. **Scope Visibility**: Should workflow variables be visible to all subsequent stages, or only the immediately following stage?

2. **Naming Convention**: What SpEL access pattern should be used?
   - `#workflowVars['name']`
   - `#workflow_name`
   - `#workflow.name`
   - `#wf.name`

3. **Condition Results**: Should stage condition evaluation results be automatically captured as workflow variables?

4. **Serialization**: Should workflow context be serializable for debugging/logging purposes?

5. **Validation**: Should there be schema validation for workflow variable names?

6. **Default Values**: Should workflow variables support default values if not set?

---

## 7. Next Steps

1. Review and discuss options with team
2. Select preferred approach
3. Create detailed technical design document
4. Implement proof-of-concept
5. Write comprehensive tests
6. Update documentation
7. Create migration guide (if needed)

---

## Appendix A: Code References

### Current Implementation Files

| File | Path |
|------|------|
| Stage Executor | `apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioStageExecutor.java` |
| Execution Result | `apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioExecutionResult.java` |
| Stage Result | `apex-core/src/main/java/dev/mars/apex/core/service/scenario/StageExecutionResult.java` |
| Scenario Stage | `apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioStage.java` |
| Enrichment Config | `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlEnrichment.java` |

### Relevant Tests

| Test | Path | Purpose |
|------|------|---------|
| ConditionalStageExecutionTest | `apex-demo/.../scenario/ConditionalStageExecutionTest.java` | Stage conditions |
| BasicStageConfigurationTest | `apex-demo/.../scenario/BasicStageConfigurationTest.java` | Stage processing |
| TradeTransformerDemoTest | `apex-demo/.../lookup/TradeTransformerDemoTest.yaml` | Temporary fields |
| ConditionalTransformationDemoTest | `apex-demo/.../conditional/ConditionalTransformationDemoTest.java` | Remove-field |

---

## Appendix B: YAML Examples

### Example: Complete Workflow with Temporary Variables

```yaml
# scenario-with-workflow-vars.yaml
metadata:
  id: "trade-processing-with-workflow"
  type: "scenario"
  version: "1.0.0"

scenario:
  scenario-id: "trade-processing"
  name: "Trade Processing with Workflow Variables"
  
  processing-stages:
    # Stage 1: Classification
    - stage-name: "classification"
      config-file: "classification-rules.yaml"
      execution-order: 1
      workflow-variables:
        - name: "isHighValue"
          expression: "#notionalAmount > 10000000"
        - name: "isUSRegion"
          expression: "#region == 'US'"
        - name: "requiresEnhancedKYC"
          expression: "#counterpartyType == 'NEW' && #amount > 1000000"
    
    # Stage 2: US Compliance (conditional)
    - stage-name: "us-compliance"
      config-file: "us-compliance-rules.yaml"
      execution-order: 2
      condition: "#workflowVars['isUSRegion'] == true"
      depends-on: ["classification"]
    
    # Stage 3: High Value Processing (conditional)
    - stage-name: "high-value-processing"
      config-file: "high-value-rules.yaml"
      execution-order: 3
      condition: "#workflowVars['isHighValue'] == true"
      depends-on: ["classification"]
      workflow-variables:
        - name: "requiresSeniorApproval"
          expression: "#notionalAmount > 50000000"
    
    # Stage 4: Final Enrichment
    - stage-name: "final-enrichment"
      config-file: "final-enrichment.yaml"
      execution-order: 4
      depends-on: ["classification"]
```

### Example: Enrichment with Workflow Scope

```yaml
# enrichment-rules.yaml
enrichments:
  - id: "risk-classification"
    type: "calculation-enrichment"
    calculation-config:
      expression: "#amount > 10000000 ? 'HIGH' : (#amount > 1000000 ? 'MEDIUM' : 'LOW')"
      result-field: "riskLevel"
      scope: "workflow"  # Goes to workflow context, not inputData
  
  - id: "counterparty-enrichment"
    type: "lookup-enrichment"
    lookup-config:
      # ... lookup config ...
    field-mappings:
      - source-field: "credit_rating"
        target-field: "counterpartyCreditRating"
        # No scope = default = persistent (goes to inputData)
```

---

*End of Document*
