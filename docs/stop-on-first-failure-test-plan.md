# Stop-On-First-Failure Test Implementation Plan

## Overview

This document outlines the implementation plan for SOLID test classes focusing on the `stop-on-first-failure` processing option in APEX rule groups. Each test class will contain a maximum of 6 tests and cover specific variations of rule group design.

**IMPORTANT**: These tests validate actual rule execution behavior and RuleResult objects, not just YAML configuration loading. The purpose is to verify that the stop-on-first-failure logic works correctly during rule processing.

## Test Class Structure

### Design Principles

Each test class follows **SOLID principles**:

- **Single Responsibility**: Each class tests only one processing option with actual rule execution
- **Open/Closed**: Easy to extend with new test scenarios
- **Liskov Substitution**: All tests follow same contract/interface
- **Interface Segregation**: Focused test methods, no unnecessary dependencies
- **Dependency Inversion**: Tests depend on abstractions (YamlRulesEngineService, RulesEngine)

### Test Validation Focus

**Primary Purpose**: Validate that stop-on-first-failure behavior works correctly during actual rule execution:
- ✅ **Rule Execution**: Tests create RulesEngine instances and execute rule groups
- ✅ **RuleResult Validation**: Tests validate `result.isTriggered()` for success/failure outcomes
- ✅ **Behavior Verification**: Tests verify short-circuit evaluation through engine logs and RuleResult messages
- ✅ **Message Inspection**: Tests display RuleResult messages showing detailed execution results

## Test Classes

### Class 1: `StopOnFirstFailureAndGroupTest.java`

**Focus**: AND groups with stop-on-first-failure behavior - **ACTUAL RULE EXECUTION**

| Test # | Test Method | Scenario | Expected Execution Behavior | RuleResult Validation |
|--------|-------------|----------|------------------------------|----------------------|
| 1 | `testAndGroupStopOnFirstFailure_AllTrue()` | All rules return `true` | All rules execute, group passes | `result.isTriggered() == true` |
| 2 | `testAndGroupStopOnFirstFailure_FirstFalse()` | First rule returns `false` | Stops immediately, group fails | `result.isTriggered() == false` |
| 3 | `testAndGroupStopOnFirstFailure_MiddleFalse()` | Middle rule returns `false` | Stops at middle rule, group fails | `result.isTriggered() == false` |
| 4 | `testAndGroupStopOnFirstFailure_LastFalse()` | Last rule returns `false` | Executes all rules, group fails | `result.isTriggered() == false` |
| 5 | `testAndGroupStopOnFirstFailure_MultipleRules()` | 5 rules: T,T,F,T,T | Stops at 3rd rule, group fails | `result.isTriggered() == false` |
| 6 | `testAndGroupStopOnFirstFailure_Disabled()` | `stop-on-first-failure: false` | All rules execute despite failures | `result.isTriggered() == false` |

### Class 2: `StopOnFirstFailureOrGroupTest.java`

**Focus**: OR groups with stop-on-first-failure behavior - **ACTUAL RULE EXECUTION**

| Test # | Test Method | Scenario | Expected Execution Behavior | RuleResult Validation |
|--------|-------------|----------|------------------------------|----------------------|
| 1 | `testOrGroupStopOnFirstFailure_AllFalse()` | All rules return `false` | All rules execute, group fails | `result.isTriggered() == false` |
| 2 | `testOrGroupStopOnFirstFailure_FirstTrue()` | First rule returns `true` | Stops immediately, group passes | `result.isTriggered() == true` |
| 3 | `testOrGroupStopOnFirstFailure_MiddleTrue()` | Middle rule returns `true` | Stops at middle rule, group passes | `result.isTriggered() == true` |
| 4 | `testOrGroupStopOnFirstFailure_LastTrue()` | Last rule returns `true` | Executes all rules, group passes | `result.isTriggered() == true` |
| 5 | `testOrGroupStopOnFirstFailure_MultipleRules()` | 5 rules: F,F,T,F,F | Stops at 3rd rule, group passes | `result.isTriggered() == true` |
| 6 | `testOrGroupStopOnFirstFailure_Disabled()` | `stop-on-first-failure: false` | All rules execute despite success | `result.isTriggered() == true` |

## Test Structure Pattern - ACTUAL RULE EXECUTION

```java
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Stop-On-First-Failure AND Group Tests")
public class StopOnFirstFailureAndGroupTest {

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        this.yamlLoader = new YamlConfigurationLoader();
        this.rulesEngineService = new YamlRulesEngineService();
    }

    @Test
    @DisplayName("AND group with stop-on-first-failure: all true rules")
    void testAndGroupStopOnFirstFailure_AllTrue() {
        logInfo("Testing AND group stop-on-first-failure with all true rules");

        String yamlContent = """
            metadata:
              name: "Stop On First Failure - All True"
              version: "1.0.0"

            rules:
              - id: "rule1"
                name: "Always True Rule 1"
                condition: "true"
                message: "Rule 1 passed"
                severity: "ERROR"
              - id: "rule2"
                name: "Always True Rule 2"
                condition: "true"
                message: "Rule 2 passed"
                severity: "ERROR"

            rule-groups:
              - id: "and-stop-all-true"
                name: "AND Stop All True"
                operator: "AND"
                stop-on-first-failure: true
                rule-ids:
                  - "rule1"
                  - "rule2"
            """;

        YamlRuleConfiguration config = loadConfiguration(yamlContent);
        assertNotNull(config, "Configuration should load successfully");

        // Create RulesEngine and execute the rule group
        RulesEngine engine;
        try {
            engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
        } catch (YamlConfigurationException e) {
            logError("Failed to create RulesEngine: " + e.getMessage());
            fail("Failed to create RulesEngine: " + e.getMessage());
            return;
        }

        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-stop-all-true");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - all rules true, so AND group should pass
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        // Validate results
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "AND group with all true rules should pass");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("All true rules executed successfully - all rules ran, group passed");
    }

    // Helper method for consistent error handling
    private YamlRuleConfiguration loadConfiguration(String yamlContent) {
        try {
            return yamlLoader.fromYamlString(yamlContent);
        } catch (YamlConfigurationException e) {
            logError("Failed to load YAML configuration: " + e.getMessage());
            fail("Failed to load YAML configuration: " + e.getMessage());
            return null;
        }
    }
}
```

## YAML Configuration Patterns

### Pattern 1: Sequential Rule Conditions

```yaml
rules:
  - id: "rule1"
    condition: "true"    # or "false"
  - id: "rule2"
    condition: "true"    # or "false"
  - id: "rule3"
    condition: "false"   # Failure point
```

### Pattern 2: Stop-On-First-Failure Control

```yaml
rule-groups:
  - id: "test-group"
    operator: "AND"      # or "OR"
    stop-on-first-failure: true   # or false
    rule-ids: ["rule1", "rule2", "rule3"]
```

## Test Coverage Matrix

| Scenario | AND Group | OR Group | Stop Enabled | Stop Disabled |
|----------|-----------|----------|--------------|---------------|
| All Pass | ✅ Class 1 | ✅ Class 2 | ✅ | ✅ |
| First Fails | ✅ Class 1 | ✅ Class 2 | ✅ | ✅ |
| Middle Fails | ✅ Class 1 | ✅ Class 2 | ✅ | ✅ |
| Last Fails | ✅ Class 1 | ✅ Class 2 | ✅ | ✅ |
| Multiple Rules | ✅ Class 1 | ✅ Class 2 | ✅ | ✅ |
| Control Test | ✅ Class 1 | ✅ Class 2 | ❌ | ✅ |

## Implementation Steps

1. **Create `StopOnFirstFailureAndGroupTest.java`** - 6 tests for AND group behavior
2. **Create `StopOnFirstFailureOrGroupTest.java`** - 6 tests for OR group behavior
3. **Run tests individually** to verify each class works independently
4. **Run both classes together** to ensure no conflicts
5. **Validate colored output** shows clear test progression

## File Structure

```
apex-demo/src/test/java/dev/mars/apex/demo/rulegroups/
├── BasicYamlRuleGroupProcessingATest.java     # Existing (rule-ids)
├── BasicYamlRuleGroupProcessingBTest.java     # Existing (rule-references)
├── StopOnFirstFailureAndGroupTest.java        # New - AND group stop behavior
└── StopOnFirstFailureOrGroupTest.java         # New - OR group stop behavior
```

## Success Criteria

- ✅ Each test class has exactly 6 tests
- ✅ Each test class focuses on single processing option with **ACTUAL RULE EXECUTION**
- ✅ All tests use colored output extension
- ✅ YAML configurations are inline (no external files)
- ✅ Tests demonstrate clear stop-on-first-failure behavior through **RuleResult validation**
- ✅ Both enabled and disabled stop behavior covered
- ✅ **RuleResult messages displayed** showing detailed execution results
- ✅ **Engine logs verified** for rule group matching behavior
- ✅ **Short-circuit evaluation confirmed** through execution patterns

## Next Steps

1. Implement `StopOnFirstFailureAndGroupTest.java` first
2. Validate all 6 tests pass with colored output
3. Implement `StopOnFirstFailureOrGroupTest.java`
4. Run comprehensive test suite to ensure no regressions
5. Document any findings or behavioral insights discovered during testing
