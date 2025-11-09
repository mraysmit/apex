# Rule-Group Inline Reference - Master Guide

**Version:** 3.0
**Date:** 2025-11-09
**Author:** Mark Andrew Ray-Smith Cityline Ltd
**Status:** Updated to reflect cross-file rule-group reference implementation

## Executive Summary

This guide documents rule-group reference capabilities in APEX. **IMPORTANT:** Cross-file rule-group references are now **FULLY IMPLEMENTED** at the rule-group level, but scenario-level rule-group-id references are not yet supported.

### Current State (Updated 2025-11-09)

#### ✅ What Works - Rule-Group Level References

**1. Within-File Rule-Group References:**
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
      - "customer-basic-validation"  # ✅ References another group in SAME file
```

**2. Cross-File Rule-Group References (IMPLEMENTED):**
```yaml
# File: CrossFileBaseRuleGroups.yaml
rule-groups:
  - id: "base-validation"
    name: "Base Validation (cross-file)"
    operator: "AND"
    rule-ids: ["age-validation", "email-validation"]

# File: CrossFileCompositeRuleGroups.yaml
rule-groups:
  - id: "composite-validation"
    name: "Composite Validation"
    operator: "AND"
    rule-ids: ["income-validation"]
    rule-group-references: ["base-validation"]  # ✅ Cross-file reference WORKS!
```

**Implementation Details:**
- Uses two-phase approach in `YamlRuleFactory`
- Phase 1: Create all rule groups and register in global map
- Phase 2: Resolve cross-file `rule-group-references` using global registry
- Tested in `CrossFileRuleGroupReferenceTest.java`

#### ❌ What Doesn't Work - Scenario Level References

**Scenarios CANNOT reference rule-groups by ID:**

```yaml
# File: scenario.yaml
scenario:
  id: my-scenario
  processing-stages:
    - stage-name: validation
      rule-group-id: "customer-basic-validation"  # ❌ NOT SUPPORTED
      # Must use: config-file: "groups/validation-groups.yaml"
```

**Why Scenario-Level References Don't Work:**

1. **No Scenario Field** - `ScenarioStage` class has no `ruleGroupId` field
2. **No Scenario Processor Support** - `ScenarioRegistryLoader` doesn't check for `rule-group-id`
3. **No Runtime Resolution** - No mechanism to resolve rule-group-id in scenario processing stages

**Current Requirement:** Scenarios must use `config-file` references to load rule configurations

## Current Implementation Architecture

### Rule-Group Level (WORKING)

```
Phase 1: Rule Group Creation
    ↓
Base Groups File (loaded)
    ↓
Rule groups created and added to global map
    ↓
Composite Groups File (loaded)
    ↓
Rule groups created and added to global map
    ↓
Phase 2: Cross-File Reference Resolution
    ↓
YamlRuleFactory.addRuleGroupReferencesToGroupWithGlobalRegistry()
    ↓
Looks up referenced groups in global map
    ↓
Adds rules from referenced groups to composite groups
    ↓
✅ Cross-file rule-group references resolved
```

### Scenario Level (NOT WORKING)

```
Scenario File
    ↓
processing-stages with config-file references
    ↓
ScenarioRegistryLoader.parseScenarioStage()
    ↓
Reads config-file field only
    ↓
Loads entire YAML file for each stage
    ↓
❌ No rule-group-id field support
❌ No direct rule-group lookup
```

## Implementation Status

### ✅ Implemented Features (Rule-Group Level)

#### 1. YAML Model Classes

**File:** `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleGroup.java`
- Lines 78-79: `rule-group-references` field
- **Status:** ✅ Supports both within-file AND cross-file references

**File:** `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleConfiguration.java`
- Lines 60-61: `ruleGroups` field
- **Status:** ✅ Loads rule-groups from YAML

#### 2. Factory Classes (Cross-File Support)

**File:** `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleFactory.java`

**Key Method:** `addRuleGroupReferencesToGroupWithGlobalRegistry()` (Lines 857-882)
```java
private void addRuleGroupReferencesToGroupWithGlobalRegistry(
    YamlRuleGroup yamlGroup,
    RuleGroup targetGroup,
    Map<String, RuleGroup> globalRuleGroupsById) {

    // Use global registry instead of config.getRuleGroupById()
    // This enables cross-file references!
    RuleGroup referencedGroup = globalRuleGroupsById.get(referencedGroupId);
    if (referencedGroup != null) {
        // Add all rules from the referenced group to the target group
        for (Rule rule : referencedGroup.getRules()) {
            targetGroup.addRule(rule, nextSequence++);
        }
    }
}
```

**Status:** ✅ Cross-file rule-group references fully working

#### 3. Engine Configuration

**File:** `apex-core/src/main/java/dev/mars/apex/core/engine/config/RulesEngineConfiguration.java`
- Lines 56-59: `ruleGroupsById` map for local registry
- **Status:** ✅ Works for within-configuration lookups
- **Note:** Global registry is managed in YamlRuleFactory, not here

### ❌ Not Implemented Features (Scenario Level)

#### 4. Scenario Processing

**File:** `apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioStage.java`
- Lines 64-70: Only has `configFile` field
- **Missing:** No `ruleGroupId` field
- **Status:** ❌ Scenarios cannot reference rule-groups by ID

**File:** `apex-core/src/main/java/dev/mars/apex/core/config/yaml/ScenarioRegistryLoader.java`
- Lines 365-369: Only parses `config-file` field
- **Missing:** No parsing of `rule-group-id` field
- **Status:** ❌ No scenario-level rule-group-id support

## Roadmap for Scenario-Level Rule-Group-ID Support

**Status:** NOT YET IMPLEMENTED - This section describes future work needed

### Step 1: Add ruleGroupId Field to ScenarioStage

**File:** `apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioStage.java`

Add new field:

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

### Step 2: Update ScenarioRegistryLoader to Parse rule-group-id

**File:** `apex-core/src/main/java/dev/mars/apex/core/config/yaml/ScenarioRegistryLoader.java`

Modify `parseScenarioStage()` method:

```java
private ScenarioStage parseScenarioStage(Map<String, Object> stageData) {
    ScenarioStage stage = new ScenarioStage();

    // Existing fields
    stage.setStageName((String) stageData.get("stage-name"));
    stage.setConfigFile((String) stageData.get("config-file"));

    // NEW: Parse rule-group-id field
    String ruleGroupId = (String) stageData.get("rule-group-id");
    if (ruleGroupId != null) {
        stage.setRuleGroupId(ruleGroupId);
    }

    // ... rest of parsing
}
```

### Step 3: Add Global Rule-Group Registry to RulesEngineConfiguration

**File:** `apex-core/src/main/java/dev/mars/apex/core/engine/config/RulesEngineConfiguration.java`

Add global registry:

```java
// Add field
private Map<String, RuleGroup> globalRuleGroupRegistry = new ConcurrentHashMap<>();

// Add method
public RuleGroup getRuleGroupByIdGlobal(String id) {
    return globalRuleGroupRegistry.get(id);
}

// Add method
public void registerRuleGroupGlobal(RuleGroup group) {
    if (group == null || group.getId() == null) {
        throw new IllegalArgumentException("Rule-group and ID cannot be null");
    }
    globalRuleGroupRegistry.put(group.getId(), group);
}
```

### Step 4: Update ScenarioStageExecutor to Resolve rule-group-id

**File:** `apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioStageExecutor.java`

Add rule-group resolution logic:

```java
private StageResult executeStage(ScenarioStage stage, Map<String, Object> data) {
    RuleGroup ruleGroup = null;

    // NEW: Check for rule-group-id first
    if (stage.getRuleGroupId() != null && !stage.getRuleGroupId().isEmpty()) {
        ruleGroup = rulesEngine.getConfiguration().getRuleGroupByIdGlobal(stage.getRuleGroupId());

        if (ruleGroup == null) {
            throw new ScenarioException(
                "Rule-group not found: " + stage.getRuleGroupId() +
                " in stage: " + stage.getStageName()
            );
        }

        logger.info("Resolved rule-group by ID: {}", stage.getRuleGroupId());
    }
    // EXISTING: Fall back to config-file
    else if (stage.getConfigFile() != null) {
        // Existing file-based loading logic
    }

    // Execute the rule group
    return executeRuleGroup(ruleGroup, stage, data);
}
```

### Step 5: Update YAML Reference Documentation

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

## Key Files to Modify (For Scenario-Level Support)

**Status:** These changes are needed to implement scenario-level rule-group-id references

| File | Change | Priority | Status |
|------|--------|----------|--------|
| `ScenarioStage.java` | Add ruleGroupId field | HIGH | ❌ Not done |
| `ScenarioRegistryLoader.java` | Parse rule-group-id | HIGH | ❌ Not done |
| `RulesEngineConfiguration.java` | Add global registry | HIGH | ⚠️ Partial (exists in YamlRuleFactory) |
| `ScenarioStageExecutor.java` | Resolve rule-group-id | HIGH | ❌ Not done |
| `APEX_YAML_REFERENCE.md` | Document new syntax | MEDIUM | ❌ Not done |

## YAML Configuration Examples

### Example 1: Cross-File Rule-Group References (✅ WORKING)

**File: CrossFileBaseRuleGroups.yaml**
```yaml
metadata:
  id: base-rule-groups
  type: rule-config

rule-configurations:
  - rules/base-rules.yaml

rule-groups:
  - id: "base-validation"
    name: "Base Validation (cross-file)"
    operator: "AND"
    rule-ids:
      - "age-validation"
      - "email-validation"
```

**File: CrossFileCompositeRuleGroups.yaml**
```yaml
metadata:
  id: composite-rule-groups
  type: rule-config

rule-configurations:
  - CrossFileBaseRuleGroups.yaml  # Load base groups first

rule-groups:
  - id: "composite-validation"
    name: "Composite Validation"
    operator: "AND"
    rule-ids:
      - "income-validation"
    rule-group-references:
      - "base-validation"  # ✅ Cross-file reference WORKS!
```

**Status:** ✅ This pattern is fully implemented and tested in `CrossFileRuleGroupReferenceTest.java`

### Example 2: Scenario with config-file (✅ CURRENT APPROACH)

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
      config-file: "groups/validation-groups.yaml"  # ✅ File reference works
      failure-policy: terminate
      execution-order: 1

    - stage-name: enrichment
      config-file: "enrichments/trade-enrichment.yaml"
      execution-order: 2
```

**Status:** ✅ This is the current working approach for scenarios

### Example 3: Scenario with rule-group-id (❌ FUTURE - NOT YET SUPPORTED)

**File: scenarios/trade-processing-future.yaml**
```yaml
metadata:
  id: trade-processing-future
  type: scenario

rule-configurations:
  - groups/validation-groups.yaml

scenario:
  scenario-id: trade-processing-future
  description: Future approach with rule-group-id

  processing-stages:
    - stage-name: validation
      rule-group-id: "mandatory-validation"  # ❌ NOT SUPPORTED YET
      failure-policy: terminate
      execution-order: 1
```

**Status:** ❌ This will be supported after implementing the roadmap steps

### Example 4: Composite Rule-Groups with Cross-File References (✅ WORKING)

**File: groups/base-validations.yaml**
```yaml
metadata:
  id: base-validations
  type: rule-config

rule-configurations:
  - rules/base-rules.yaml

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
```

**File: groups/composite-groups.yaml**
```yaml
metadata:
  id: composite-groups
  type: rule-config

rule-configurations:
  - groups/base-validations.yaml  # Load base groups

rule-groups:
  # Composite group combining multiple groups from different file
  - id: "complete-validation"
    name: "Complete Validation"
    operator: "AND"
    rule-group-references:
      - "basic-validation"      # ✅ Cross-file reference
      - "business-validation"   # ✅ Cross-file reference
```

**Status:** ✅ This pattern is fully working - rule groups can reference other rule groups from different files

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

## Summary: What's Implemented vs What's Not

### ✅ IMPLEMENTED: Cross-File Rule-Group References

**Status:** FULLY WORKING since implementation in YamlRuleFactory

Rule groups can now reference other rule groups across different YAML files using the `rule-group-references` field:

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
    rule-group-references: [ base_validation ]  # ✅ Cross-file reference works!
```

**Implementation Details:**
- Location: `YamlRuleFactory.addRuleGroupReferencesToGroupWithGlobalRegistry()` (lines 857-882)
- Uses two-phase approach:
  1. **Phase 1**: Create all rule groups and register them in a global registry map
  2. **Phase 2**: Resolve cross-file `rule-group-references` using the global registry
- Test: `CrossFileRuleGroupReferenceTest.java`

### ❌ NOT IMPLEMENTED: Scenario-Level Rule-Group-ID References

**Status:** NOT YET SUPPORTED

Scenarios cannot yet reference rule groups by ID in processing stages. They must use `config-file` references:

```yaml
# ❌ This DOESN'T work yet
scenario:
  processing-stages:
    - stage-name: validation
      rule-group-id: "mandatory-validation"  # ❌ NOT SUPPORTED

# ✅ This DOES work (current approach)
scenario:
  processing-stages:
    - stage-name: validation
      config-file: "groups/validation-groups.yaml"  # ✅ SUPPORTED
```

**Why It Doesn't Work:**
- `ScenarioStage` class has no `ruleGroupId` field
- `ScenarioRegistryLoader` doesn't parse `rule-group-id`
- `ScenarioStageExecutor` doesn't resolve rule-group-id references
- No persistent global registry in `RulesEngineConfiguration` for scenario lookups

**To Implement:** Follow the roadmap in the "Roadmap for Scenario-Level Rule-Group-ID Support" section above.

---

## Conclusion

**Current State (2025-11-09):**

| Feature | Status | Notes |
|---------|--------|-------|
| Within-file rule-group references | ✅ WORKING | Always worked |
| Cross-file rule-group references | ✅ WORKING | Implemented in YamlRuleFactory |
| Scenario rule-group-id references | ❌ NOT WORKING | Requires roadmap implementation |

**Key Takeaway:** Rule groups can reference other rule groups across files (✅), but scenarios cannot yet reference rule groups by ID (❌). Scenarios must continue using `config-file` references until the scenario-level roadmap is implemented.

**Benefits of Cross-File Rule-Group References (Already Achieved):**
- ✅ Enterprise-scale rule organization
- ✅ Proper separation of concerns across multiple YAML files
- ✅ Reusable rule groups across different configurations
- ✅ Tested and production-ready

**Benefits of Scenario-Level Rule-Group-ID (Future):**
- 🔮 Cleaner scenario YAML (semantic ID instead of file path)
- 🔮 Decoupling scenarios from file structure
- 🔮 Runtime flexibility to swap rule-groups
- 🔮 Better maintainability

---

**End of Document**
