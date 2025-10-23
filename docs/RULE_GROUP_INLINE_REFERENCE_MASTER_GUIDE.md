# Rule-Group Inline Reference - Master Guide

## Executive Summary

This guide consolidates all information about implementing rule-group inline references in APEX, enabling scenarios to reference rule-groups by their **ID** directly instead of through file paths.

### Current vs Desired Behavior

**Current (File-Based):**
```yaml
scenario:
  processing-stages:
    - stage-name: validation
      config-file: groups/validation-groups.yaml  # ❌ File reference
```

**Desired (ID-Based):**
```yaml
scenario:
  processing-stages:
    - stage-name: validation
      rule-group-id: "mandatory-validation"  # ✓ ID reference
```

## Current State Analysis

### What Currently Works ✓

**Rule-groups CAN be referenced by ID within the same YAML file:**

```yaml
# File: base-groups.yaml
rule-groups:
  - id: "customer-basic-validation"
    name: "Customer Basic Validation"
    operator: "AND"
    rule-ids:
      - "name-validation"
      - "age-validation"

  - id: "complete-onboarding"
    name: "Complete Customer Onboarding"
    operator: "AND"
    rule-group-references:
      - "customer-basic-validation"  # ✓ References another group in SAME file
```

### What DOESN'T Work Yet ✗

**Rule-groups CANNOT be referenced by ID from a DIFFERENT YAML file:**

```yaml
# File: scenario.yaml
scenario:
  id: my-scenario
  processing-stages:
    - stage-name: validation
      rule-group-id: "customer-basic-validation"  # ✗ This doesn't work!
      # Currently you must reference the FILE, not the rule-group ID
```

### Why It Doesn't Work

1. **No Global Registry** - Rule-groups are only registered in their local configuration
2. **No Cross-File Lookup** - No method to find a rule-group by ID across multiple loaded files
3. **No Scenario Support** - Scenarios don't have a `rule-group-id` field in processing stages
4. **No Runtime Resolution** - No mechanism to resolve rule-group-id to actual RuleGroup object

## Current Code Flow

```
Scenario File
    ↓
    references file path
    ↓
Groups File (loaded)
    ↓
    references file path
    ↓
Rules File (loaded)
    ↓
    Rules registered in RulesEngineConfiguration
    ↓
    Rule-groups created and registered locally
    ↓
    Scenario uses rule-group by ID (within same config)
```

## Desired Code Flow

```
Scenario File
    ↓
    references rule-group ID
    ↓
    Global Rule-Group Registry lookup
    ↓
    Rule-group found (from any loaded file)
    ↓
    Scenario uses rule-group directly
```

## Implementation Roadmap

### Current Code Locations

#### 1. YAML Model Classes

**File:** `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleGroup.java`
- Lines 78-79: `rule-group-references` field (already supports ID references within same file)
- **Status:** ✓ Already supports within-file references

**File:** `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleConfiguration.java`
- Lines 60-61: `ruleGroups` field
- **Status:** ✓ Loads rule-groups from YAML

#### 2. Factory Classes

**File:** `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleFactory.java`
- Lines 504-550: `createRuleGroup()` method
  - Creates RuleGroup from YamlRuleGroup
  - Registers in RulesEngineConfiguration
  
- Lines 662-707: `addRuleGroupReferencesToGroup()` method
  - Handles rule-group-references within same file
  - Looks up referenced groups by ID
  - **Issue:** Only searches within current configuration

- Lines 774-785: `createRuleGroups()` method
  - Public API for creating rule-groups
  - **Status:** ✓ Can be reused

#### 3. Engine Configuration

**File:** `apex-core/src/main/java/dev/mars/apex/core/engine/RulesEngineConfiguration.java`
- **Missing:** Global rule-group registry across all loaded configurations
- **Current:** Only stores rule-groups from current configuration

#### 4. Scenario Processing

**File:** `apex-core/src/main/java/dev/mars/apex/core/config/scenario/` (location TBD)
- **Missing:** Support for `rule-group-id` in processing stages
- **Current:** Only supports `config-file` references

### Implementation Steps

#### Step 1: Extend RulesEngineConfiguration

**File:** `apex-core/src/main/java/dev/mars/apex/core/engine/RulesEngineConfiguration.java`

Add global rule-group registry:

```java
// Add field
private Map<String, RuleGroup> globalRuleGroupRegistry = new ConcurrentHashMap<>();

// Add method
public RuleGroup getRuleGroupByIdGlobal(String id) {
    return globalRuleGroupRegistry.get(id);
}

// Add method
public void registerRuleGroupGlobal(RuleGroup group) {
    globalRuleGroupRegistry.put(group.getId(), group);
}
```

#### Step 2: Update YamlRuleFactory

**File:** `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleFactory.java`

Modify `createRuleGroup()` to register globally:

```java
public RuleGroup createRuleGroup(YamlRuleGroup yamlGroup, RulesEngineConfiguration config) {
    RuleGroup group = new RuleGroup(...);
    config.registerRuleGroup(group);
    config.registerRuleGroupGlobal(group);  // NEW: Register globally
    return group;
}
```

#### Step 3: Add Scenario Processing Support

**File:** `apex-core/src/main/java/dev/mars/apex/core/config/scenario/ScenarioConfiguration.java`

Add to processing stage:

```java
@JsonProperty("rule-group-id")
private String ruleGroupId;  // NEW: Direct rule-group ID reference

public String getRuleGroupId() {
    return ruleGroupId;
}

public void setRuleGroupId(String ruleGroupId) {
    this.ruleGroupId = ruleGroupId;
}
```

#### Step 4: Update Scenario Processor

**File:** `apex-core/src/main/java/dev/mars/apex/core/engine/scenario/ScenarioProcessor.java`

Add rule-group resolution:

```java
private void processStage(ProcessingStage stage, RulesEngineConfiguration config) {
    // NEW: Check for rule-group-id first
    if (stage.getRuleGroupId() != null) {
        RuleGroup group = config.getRuleGroupByIdGlobal(stage.getRuleGroupId());
        if (group == null) {
            throw new ScenarioException(
                "Rule-group not found: " + stage.getRuleGroupId()
            );
        }
        // Use the rule-group
        executeRuleGroup(group, stage);
    } 
    // EXISTING: Fall back to config-file
    else if (stage.getConfigFile() != null) {
        // Existing logic
    }
}
```

#### Step 5: Update YAML Reference Documentation

**File:** `docs/APEX_YAML_REFERENCE.md`

Add new section:

```markdown
### Rule-Group ID References in Scenarios

Reference rule-groups by ID directly in scenario processing stages:

```yaml
scenario:
  processing-stages:
    - stage-name: validation
      rule-group-id: "mandatory-validation"  # Direct ID reference
      failure-policy: terminate
```

This requires the rule-group to be defined in any loaded configuration file.
```

## Key Files to Modify

| File | Change | Priority |
|------|--------|----------|
| `RulesEngineConfiguration.java` | Add global registry | HIGH |
| `YamlRuleFactory.java` | Register globally | HIGH |
| `ScenarioConfiguration.java` | Add rule-group-id field | HIGH |
| `ScenarioProcessor.java` | Resolve rule-group-id | HIGH |
| `APEX_YAML_REFERENCE.md` | Document new syntax | MEDIUM |

## YAML Configuration Examples

### Example 1: Simple Rule-Group Reference

**File: rules/base-rules.yaml**
```yaml
metadata:
  id: base-rules
  type: rule-config

rules:
  - id: "amount-validation"
    condition: "#amount > 0"
    message: "Amount must be positive"
  
  - id: "date-validation"
    condition: "#date != null"
    message: "Date is required"
```

**File: groups/validation-groups.yaml**
```yaml
metadata:
  id: validation-groups
  type: rule-config

rule-configurations:
  - rules/base-rules.yaml

rule-groups:
  - id: "mandatory-validation"
    name: "Mandatory Validation"
    operator: "AND"
    stop-on-first-failure: true
    rule-ids:
      - "amount-validation"
      - "date-validation"
```

**File: scenarios/trade-processing.yaml**
```yaml
metadata:
  id: trade-processing-scenario
  type: scenario

rule-configurations:
  - groups/validation-groups.yaml

scenario:
  scenario-id: trade-processing
  description: Process trade transactions
  
  processing-stages:
    - stage-name: validation
      rule-group-id: "mandatory-validation"  # ✓ Direct ID reference
      failure-policy: terminate
      execution-order: 1
    
    - stage-name: enrichment
      enrichment-id: "trade-enrichment"
      execution-order: 2
```

### Example 2: Multiple Rule-Groups in One Scenario

**File: groups/all-validations.yaml**
```yaml
metadata:
  id: all-validations
  type: rule-config

rule-configurations:
  - rules/base-rules.yaml
  - rules/business-rules.yaml

rule-groups:
  - id: "basic-validation"
    operator: "AND"
    rule-ids:
      - "amount-validation"
      - "date-validation"
  
  - id: "business-validation"
    operator: "AND"
    rule-ids:
      - "counterparty-check"
      - "credit-limit-check"
  
  - id: "compliance-validation"
    operator: "AND"
    rule-ids:
      - "aml-check"
      - "sanctions-check"
```

**File: scenarios/comprehensive-processing.yaml**
```yaml
metadata:
  id: comprehensive-scenario
  type: scenario

rule-configurations:
  - groups/all-validations.yaml

scenario:
  scenario-id: comprehensive-processing
  
  processing-stages:
    - stage-name: basic-validation
      rule-group-id: "basic-validation"
      failure-policy: terminate
      execution-order: 1
    
    - stage-name: business-validation
      rule-group-id: "business-validation"
      failure-policy: continue
      execution-order: 2
    
    - stage-name: compliance-validation
      rule-group-id: "compliance-validation"
      failure-policy: terminate
      execution-order: 3
```

### Example 3: Composite Rule-Groups

**File: groups/composite-groups.yaml**
```yaml
metadata:
  id: composite-groups
  type: rule-config

rule-configurations:
  - groups/all-validations.yaml

rule-groups:
  # Base groups (from all-validations.yaml)
  # - basic-validation
  # - business-validation
  # - compliance-validation
  
  # Composite group combining multiple groups
  - id: "complete-validation"
    name: "Complete Validation"
    operator: "AND"
    rule-group-references:
      - "basic-validation"
      - "business-validation"
      - "compliance-validation"
```

**File: scenarios/complete-processing.yaml**
```yaml
metadata:
  id: complete-scenario
  type: scenario

rule-configurations:
  - groups/composite-groups.yaml

scenario:
  scenario-id: complete-processing
  
  processing-stages:
    - stage-name: all-validations
      rule-group-id: "complete-validation"  # ✓ References composite group
      failure-policy: terminate
      execution-order: 1
```

## Java Code Examples

### Example 1: Registering Rule-Groups Globally

```java
// In YamlRuleFactory.java
public RuleGroup createRuleGroup(YamlRuleGroup yamlGroup,
                                 RulesEngineConfiguration config)
        throws YamlConfigurationException {

    // Create the rule group
    RuleGroup group = new RuleGroup(
        yamlGroup.getId(),
        yamlGroup.getCategory(),
        yamlGroup.getName(),
        yamlGroup.getDescription(),
        yamlGroup.getPriority() != null ? yamlGroup.getPriority() : 100,
        "AND".equalsIgnoreCase(yamlGroup.getOperator()),
        yamlGroup.getStopOnFirstFailure() != null ?
            yamlGroup.getStopOnFirstFailure() : false,
        yamlGroup.getParallelExecution() != null ?
            yamlGroup.getParallelExecution() : false,
        yamlGroup.getDebugMode() != null ?
            yamlGroup.getDebugMode() : false
    );

    // Register locally
    config.registerRuleGroup(group);

    // NEW: Register globally for cross-file references
    config.registerRuleGroupGlobal(group);

    return group;
}
```

### Example 2: Resolving Rule-Group by ID in Scenario

```java
// In ScenarioProcessor.java
private void processStage(ProcessingStage stage,
                         RulesEngineConfiguration config,
                         Map<String, Object> context)
        throws ScenarioException {

    RuleGroup ruleGroup = null;

    // NEW: Check for rule-group-id first
    if (stage.getRuleGroupId() != null &&
        !stage.getRuleGroupId().isEmpty()) {

        ruleGroup = config.getRuleGroupByIdGlobal(stage.getRuleGroupId());

        if (ruleGroup == null) {
            throw new ScenarioException(
                "Rule-group not found: " + stage.getRuleGroupId() +
                " in stage: " + stage.getStageName()
            );
        }

        LOGGER.info("Resolved rule-group by ID: " + stage.getRuleGroupId());
    }
    // EXISTING: Fall back to config-file
    else if (stage.getConfigFile() != null &&
             !stage.getConfigFile().isEmpty()) {

        // Load configuration from file
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration yamlConfig =
            loader.fromFile(stage.getConfigFile());

        // Get first rule-group from file (or specified one)
        if (yamlConfig.getRuleGroups() != null &&
            !yamlConfig.getRuleGroups().isEmpty()) {

            YamlRuleFactory factory = new YamlRuleFactory();
            ruleGroup = factory.createRuleGroup(
                yamlConfig.getRuleGroups().get(0),
                config
            );
        }
    }

    if (ruleGroup == null) {
        throw new ScenarioException(
            "No rule-group specified in stage: " + stage.getStageName()
        );
    }

    // Execute the rule-group
    executeRuleGroup(ruleGroup, stage, context);
}
```

### Example 3: Global Rule-Group Registry

```java
// In RulesEngineConfiguration.java
public class RulesEngineConfiguration {

    // Existing fields
    private Map<String, Rule> rules = new ConcurrentHashMap<>();
    private Map<String, RuleGroup> ruleGroups = new ConcurrentHashMap<>();

    // NEW: Global registry for cross-file references
    private Map<String, RuleGroup> globalRuleGroupRegistry =
        new ConcurrentHashMap<>();

    /**
     * Register a rule-group globally for cross-file references.
     * This allows scenarios to reference rule-groups by ID
     * regardless of which configuration file they were defined in.
     */
    public void registerRuleGroupGlobal(RuleGroup group) {
        if (group == null || group.getId() == null) {
            throw new IllegalArgumentException(
                "Rule-group and ID cannot be null"
            );
        }

        globalRuleGroupRegistry.put(group.getId(), group);
        LOGGER.fine("Registered rule-group globally: " + group.getId());
    }

    /**
     * Lookup a rule-group by ID in the global registry.
     * This searches across all loaded configurations.
     */
    public RuleGroup getRuleGroupByIdGlobal(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        RuleGroup group = globalRuleGroupRegistry.get(id);

        if (group == null) {
            LOGGER.warning("Rule-group not found in global registry: " + id);
        }

        return group;
    }

    /**
     * Get all rule-groups in the global registry.
     */
    public Collection<RuleGroup> getAllRuleGroupsGlobal() {
        return Collections.unmodifiableCollection(
            globalRuleGroupRegistry.values()
        );
    }
}
```

## Test Examples

### Unit Test: Global Rule-Group Registration

```java
@Test
public void testRuleGroupGlobalRegistration() {
    RulesEngineConfiguration config =
        new RulesEngineConfiguration();

    RuleGroup group = new RuleGroup(
        "test-group",
        "test-category",
        "Test Group",
        "Test Description",
        10,
        true,  // AND operator
        false,
        false,
        false
    );

    // Register globally
    config.registerRuleGroupGlobal(group);

    // Verify lookup works
    RuleGroup retrieved = config.getRuleGroupByIdGlobal("test-group");
    assertNotNull(retrieved);
    assertEquals("test-group", retrieved.getId());
}
```

### Integration Test: Scenario with Rule-Group ID

```java
@Test
public void testScenarioWithRuleGroupId() throws Exception {
    // Load configuration with rule-groups
    YamlConfigurationLoader loader = new YamlConfigurationLoader();
    YamlRuleConfiguration yamlConfig =
        loader.fromFile("groups/validation-groups.yaml");

    // Create engine configuration
    RulesEngineConfiguration config =
        new RulesEngineConfiguration();

    // Create rule-groups and register globally
    YamlRuleFactory factory = new YamlRuleFactory();
    factory.createRulesFromYaml(yamlConfig, config);

    // Load scenario
    YamlRuleConfiguration scenarioYaml =
        loader.fromFile("scenarios/trade-processing.yaml");

    // Verify rule-group can be resolved by ID
    RuleGroup group = config.getRuleGroupByIdGlobal(
        "mandatory-validation"
    );
    assertNotNull(group);
    assertEquals("mandatory-validation", group.getId());
}
```

## Testing Strategy

### Unit Tests

1. **Test rule-group global registration**
   - Verify rule-groups are registered globally
   - Verify lookup by ID works

2. **Test scenario processing with rule-group-id**
   - Load scenario with rule-group-id reference
   - Verify rule-group is resolved correctly
   - Verify error handling for missing rule-group

3. **Test backward compatibility**
   - Verify config-file references still work
   - Verify rule-group-id takes precedence if both specified

### Integration Tests

1. **Multi-file scenario**
   - Define rules in one file
   - Define rule-groups in another file
   - Reference rule-group by ID in scenario
   - Verify end-to-end execution

2. **Reusability test**
   - Multiple scenarios reference same rule-group
   - Verify all scenarios use same rule-group instance

## Benefits

✓ **Cleaner YAML** - Semantic ID instead of file path
✓ **Decoupling** - Scenarios independent of file structure
✓ **Reusability** - Same rule-group used by multiple scenarios
✓ **Maintainability** - Move files without breaking references
✓ **Runtime Flexibility** - Swap rule-groups without changing YAML

## Backward Compatibility

- Keep `config-file` support for existing scenarios
- If both `rule-group-id` and `config-file` specified, use `rule-group-id`
- No breaking changes to existing YAML files

## Implementation Complexity

- **Effort:** Medium (4-6 hours)
- **Risk:** Low (isolated changes, backward compatible)
- **Testing:** Unit tests + integration tests
- **Documentation:** Update YAML reference guide

## Next Steps

1. Review the implementation roadmap
2. Decide if this feature is a priority
3. If yes, implement in this order:
   - Add global registry to RulesEngineConfiguration
   - Update YamlRuleFactory to register globally
   - Add rule-group-id field to scenario configuration
   - Update scenario processor to resolve rule-group-id
   - Write unit and integration tests
   - Update documentation

## Cross-File Rule-Group References (IMPLEMENTED)

**Note:** Cross-file rule-group references have been successfully implemented! Rule groups can now reference other rule groups across different YAML files using the `rule-group-references` field. This follows the same pattern as enrichment groups.

### Example of Working Cross-File References

```yaml
# File: CrossFileBaseRuleGroups.yaml
rule-groups:
  - id: base_validation
    name: Base Validation (cross-file)
    operator: AND
    rule-ids: [ age-validation, email-validation ]

# File: CrossFileCompositeRuleGroups.yaml
rule-groups:
  - id: cf_composite
    name: Composite (income + base_validation from other file)
    operator: AND
    rule-ids: [ income-validation ]
    rule-group-references: [ base_validation ]  # ✓ Cross-file reference works!
```

The implementation uses a two-phase approach:
1. **Phase 1**: Create all rule groups and register them in a global registry
2. **Phase 2**: Resolve cross-file rule-group-references using the global registry

This enables enterprise-scale rule organization with proper separation of concerns across multiple YAML files.
