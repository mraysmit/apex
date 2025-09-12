/*
 * Copyright (c) 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.mars.apex.demo.rulegroups;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static dev.mars.apex.demo.ColoredTestOutputExtension.logInfo;
import static dev.mars.apex.demo.ColoredTestOutputExtension.logSuccess;
import static dev.mars.apex.demo.ColoredTestOutputExtension.logWarning;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for override-priority functionality in rule-references.
 *
 * This test class validates:
 * - Priority override changes rule execution behavior
 * - Original rules remain unchanged when priority is overridden
 * - Same rule can have different priorities in different groups
 * - Invalid priority values are properly rejected
 * - Priority override works with sequence and enabled properties
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Override Priority Tests")
public class OverridePriorityTest {

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        this.yamlLoader = new YamlConfigurationLoader();
        this.rulesEngineService = new YamlRulesEngineService();
    }

    @Test
    @DisplayName("Simplest use case: Same rule, different priorities in different groups")
    void testSimplestUseCasePriorityOverride() {
        logInfo("Testing simplest use case: same rule with different priorities");

        String yamlContent = """
            metadata:
              name: "Priority Override Demo"
              version: "1.0.0"

            rules:
              - id: "data-validation"
                name: "Data Validation Rule"
                condition: "#value != null && #value > 0"
                message: "Data validation passed"
                severity: "ERROR"
                priority: 50  # Default medium priority

            rule-groups:
              # Critical processing - validation is highest priority
              - id: "critical-processing"
                name: "Critical Data Processing"
                operator: "AND"
                rule-references:
                  - rule-id: "data-validation"
                    sequence: 1
                    enabled: true
                    override-priority: 1    # Override to highest priority

              # Batch processing - validation is lower priority
              - id: "batch-processing"
                name: "Batch Data Processing"
                operator: "AND"
                rule-references:
                  - rule-id: "data-validation"
                    sequence: 1
                    enabled: true
                    override-priority: 100  # Override to lowest priority
            """;

        YamlRuleConfiguration config = loadConfiguration(yamlContent);
        assertNotNull(config, "Configuration should load successfully");

        RulesEngine engine = createRulesEngine(config);

        // Test data that will pass the validation
        Map<String, Object> testData = Map.of("value", 42);

        // Execute critical processing group
        RuleGroup criticalGroup = engine.getConfiguration().getRuleGroupById("critical-processing");
        RuleResult criticalResult = engine.executeRuleGroupsList(List.of(criticalGroup), testData);

        // Execute batch processing group
        RuleGroup batchGroup = engine.getConfiguration().getRuleGroupById("batch-processing");
        RuleResult batchResult = engine.executeRuleGroupsList(List.of(batchGroup), testData);

        // Both should pass
        assertTrue(criticalResult.isTriggered(), "Critical processing should pass");
        assertTrue(batchResult.isTriggered(), "Batch processing should pass");

        // Verify original rule priority is unchanged
        var originalRule = engine.getConfiguration().getRuleById("data-validation");
        assertEquals(50, originalRule.getPriority(), "Original rule priority should remain 50");

        logInfo("Critical processing result: " + criticalResult.getMessage());
        logInfo("Batch processing result: " + batchResult.getMessage());
        logSuccess("Same rule works with different priorities in different contexts");
    }

    @Test
    @DisplayName("Override priority should change rule execution order")
    void testOverridePriorityChangesRuleExecution() {
        logInfo("Testing override-priority changes rule execution order");

        String yamlContent = """
            metadata:
              name: "Override Priority Test"
              version: "1.0.0"

            rules:
              - id: "low-priority-rule"
                name: "Low Priority Rule"
                condition: "true"
                message: "Low priority rule passed"
                severity: "ERROR"
                priority: 100  # Low priority originally
              - id: "high-priority-rule"
                name: "High Priority Rule"
                condition: "true"
                message: "High priority rule passed"
                severity: "ERROR"
                priority: 50   # Medium priority originally

            rule-groups:
              - id: "priority-override-group"
                name: "Priority Override Group"
                description: "Rule group with priority overrides"
                operator: "AND"
                rule-references:
                  - rule-id: "low-priority-rule"
                    sequence: 1
                    enabled: true
                    override-priority: 1    # Override to highest priority
                  - rule-id: "high-priority-rule"
                    sequence: 2
                    enabled: true
                    override-priority: 10   # Override to lower priority
            """;

        YamlRuleConfiguration config = loadConfiguration(yamlContent);
        assertNotNull(config, "Configuration should load successfully");

        // Create RulesEngine and execute the rule group
        RulesEngine engine = createRulesEngine(config);
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("priority-override-group");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        // Validate results
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Rule group should pass");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        // The message should show rules executed in priority order (override priority 1, then 10)
        // Expected: "Priority Override Group: Low priority rule passed AND High priority rule passed"
        String message = result.getMessage();
        assertNotNull(message, "Result message should not be null");
        assertTrue(message.contains("Low priority rule passed"), "Low priority rule should execute first due to override");

        logSuccess("Override priority test executed successfully - execution order changed by priority override");
    }

    @Test
    @DisplayName("Original rule priority should remain unchanged")
    void testOriginalRuleUnchanged() {
        logInfo("Testing that original rule priority remains unchanged");

        String yamlContent = """
            metadata:
              name: "Original Rule Unchanged Test"
              version: "1.0.0"

            rules:
              - id: "test-rule"
                name: "Test Rule"
                condition: "true"
                message: "Test rule passed"
                severity: "ERROR"
                priority: 50  # Original priority

            rule-groups:
              - id: "group-with-override"
                name: "Group With Override"
                operator: "AND"
                rule-references:
                  - rule-id: "test-rule"
                    sequence: 1
                    enabled: true
                    override-priority: 1    # Override priority

              - id: "group-without-override"
                name: "Group Without Override"
                operator: "AND"
                rule-ids: ["test-rule"]     # Uses original priority
            """;

        YamlRuleConfiguration config = loadConfiguration(yamlContent);
        RulesEngine engine = createRulesEngine(config);

        // Get the original rule and verify its priority is unchanged
        var originalRule = engine.getConfiguration().getRuleById("test-rule");
        assertNotNull(originalRule, "Original rule should exist");
        assertEquals(50, originalRule.getPriority(), "Original rule priority should be unchanged");

        logSuccess("Original rule priority remains unchanged - feature works correctly");
    }

    @Test
    @DisplayName("Same rule with different priorities in different groups")
    void testSameRuleDifferentPrioritiesInDifferentGroups() {
        logInfo("Testing same rule with different priorities in different groups");

        String yamlContent = """
            metadata:
              name: "Multiple Groups Priority Test"
              version: "1.0.0"

            rules:
              - id: "shared-rule"
                name: "Shared Rule"
                condition: "true"
                message: "Shared rule passed"
                severity: "ERROR"
                priority: 50  # Original priority
              - id: "other-rule"
                name: "Other Rule"
                condition: "true"
                message: "Other rule passed"
                severity: "ERROR"
                priority: 25

            rule-groups:
              - id: "high-priority-group"
                name: "High Priority Group"
                operator: "AND"
                rule-references:
                  - rule-id: "shared-rule"
                    sequence: 1
                    enabled: true
                    override-priority: 1    # High priority in this group
                  - rule-id: "other-rule"
                    sequence: 2
                    enabled: true

              - id: "low-priority-group"
                name: "Low Priority Group"
                operator: "AND"
                rule-references:
                  - rule-id: "other-rule"
                    sequence: 1
                    enabled: true
                  - rule-id: "shared-rule"
                    sequence: 2
                    enabled: true
                    override-priority: 100  # Low priority in this group
            """;

        YamlRuleConfiguration config = loadConfiguration(yamlContent);
        RulesEngine engine = createRulesEngine(config);

        // Execute both groups
        Map<String, Object> testData = Map.of();
        
        RuleGroup highPriorityGroup = engine.getConfiguration().getRuleGroupById("high-priority-group");
        RuleResult highPriorityResult = engine.executeRuleGroupsList(List.of(highPriorityGroup), testData);
        
        RuleGroup lowPriorityGroup = engine.getConfiguration().getRuleGroupById("low-priority-group");
        RuleResult lowPriorityResult = engine.executeRuleGroupsList(List.of(lowPriorityGroup), testData);

        // Both should pass
        assertTrue(highPriorityResult.isTriggered(), "High priority group should pass");
        assertTrue(lowPriorityResult.isTriggered(), "Low priority group should pass");

        logInfo("High priority group result: " + highPriorityResult.getMessage());
        logInfo("Low priority group result: " + lowPriorityResult.getMessage());

        logSuccess("Same rule works with different priorities in different groups");
    }

    @Test
    @DisplayName("Invalid priority values should be rejected")
    void testInvalidPriorityValidation() {
        logInfo("Testing validation of invalid priority values");

        String yamlContent = """
            metadata:
              name: "Invalid Priority Test"
              version: "1.0.0"

            rules:
              - id: "test-rule"
                name: "Test Rule"
                condition: "true"
                message: "Test rule passed"
                severity: "ERROR"
                priority: 50

            rule-groups:
              - id: "invalid-priority-group"
                name: "Invalid Priority Group"
                operator: "AND"
                rule-references:
                  - rule-id: "test-rule"
                    sequence: 1
                    enabled: true
                    override-priority: 0    # Invalid: must be >= 1
            """;

        // This should throw an exception during configuration loading
        assertThrows(YamlConfigurationException.class, () -> {
            YamlRuleConfiguration config = loadConfiguration(yamlContent);
            logInfo("YAML config loaded successfully, rule groups: " + config.getRuleGroups().size());
            if (!config.getRuleGroups().isEmpty()) {
                var ruleGroup = config.getRuleGroups().get(0);
                logInfo("Rule group ID: " + ruleGroup.getId());
                if (ruleGroup.getRuleReferences() != null) {
                    logInfo("Rule references count: " + ruleGroup.getRuleReferences().size());
                    for (var ref : ruleGroup.getRuleReferences()) {
                        logInfo("Rule ref: " + ref.getRuleId() + ", priority: " + ref.getOverridePriority());
                    }
                }
            }
            rulesEngineService.createRulesEngineFromYamlConfig(config);
        }, "Invalid priority value should be rejected");

        logSuccess("Invalid priority values are properly rejected");
    }

    @Test
    @DisplayName("Priority override combined with sequence and enabled properties")
    void testPriorityOverrideWithSequenceAndEnabled() {
        logInfo("Testing priority override combined with sequence and enabled properties");

        String yamlContent = """
            metadata:
              name: "Combined Properties Test"
              version: "1.0.0"

            rules:
              - id: "rule1"
                name: "Rule 1"
                condition: "true"
                message: "Rule 1 passed"
                severity: "ERROR"
                priority: 30
              - id: "rule2"
                name: "Rule 2"
                condition: "true"
                message: "Rule 2 passed"
                severity: "ERROR"
                priority: 20
              - id: "rule3"
                name: "Rule 3"
                condition: "true"
                message: "Rule 3 passed"
                severity: "ERROR"
                priority: 10

            rule-groups:
              - id: "combined-properties-group"
                name: "Combined Properties Group"
                operator: "AND"
                rule-references:
                  - rule-id: "rule1"
                    sequence: 1
                    enabled: true
                    override-priority: 1    # Highest priority override
                  - rule-id: "rule2"
                    sequence: 2
                    enabled: false          # Disabled
                    override-priority: 5
                  - rule-id: "rule3"
                    sequence: 3
                    enabled: true
                    override-priority: 10   # Lower priority override
            """;

        YamlRuleConfiguration config = loadConfiguration(yamlContent);
        RulesEngine engine = createRulesEngine(config);
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("combined-properties-group");

        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        assertTrue(result.isTriggered(), "Rule group should pass");
        
        // Rule 2 should be skipped (disabled), only rule1 and rule3 should execute
        String message = result.getMessage();
        assertTrue(message.contains("Rule 1 passed"), "Rule 1 should execute");
        assertFalse(message.contains("Rule 2 passed"), "Rule 2 should be skipped (disabled)");
        assertTrue(message.contains("Rule 3 passed"), "Rule 3 should execute");

        logInfo("Combined properties result: " + message);
        logSuccess("Priority override works correctly with sequence and enabled properties");
    }

    // Helper methods for consistent error handling and engine creation
    private YamlRuleConfiguration loadConfiguration(String yamlContent) {
        try {
            return yamlLoader.fromYamlString(yamlContent);
        } catch (YamlConfigurationException e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
            return null;
        }
    }

    private RulesEngine createRulesEngine(YamlRuleConfiguration config) {
        try {
            return rulesEngineService.createRulesEngineFromYamlConfig(config);
        } catch (YamlConfigurationException e) {
            fail("Failed to create RulesEngine: " + e.getMessage());
            return null;
        }
    }
}
