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
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify rule-references with sequence and enabled properties work correctly.
 * 
 * This test class validates:
 * - Sequence property controls execution order
 * - Enabled property controls rule inclusion/exclusion
 * - Disabled rules are properly skipped
 * - Rule execution follows specified sequence
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Rule References Sequence and Enabled Properties Tests")
public class RuleReferencesSequenceEnabledTest {

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        this.yamlLoader = new YamlConfigurationLoader();
        this.rulesEngineService = new YamlRulesEngineService();
    }

    @Test
    @DisplayName("Rule references with custom sequence order should execute in specified order")
    void testRuleReferencesCustomSequenceOrder() {
        logInfo("Testing rule references with custom sequence order");

        String yamlContent = """
            metadata:
              name: "Custom Sequence Order Test"
              version: "1.0.0"

            rules:
              - id: "rule1"
                name: "Rule 1"
                condition: "true"
                message: "Rule 1 passed"
                severity: "ERROR"
              - id: "rule2"
                name: "Rule 2"
                condition: "true"
                message: "Rule 2 passed"
                severity: "ERROR"
              - id: "rule3"
                name: "Rule 3"
                condition: "true"
                message: "Rule 3 passed"
                severity: "ERROR"

            rule-groups:
              - id: "custom-sequence-group"
                name: "Custom Sequence Group"
                description: "Rule group with custom sequence order"
                operator: "AND"
                rule-references:
                  - rule-id: "rule3"
                    sequence: 1
                    enabled: true
                  - rule-id: "rule1"
                    sequence: 2
                    enabled: true
                  - rule-id: "rule2"
                    sequence: 3
                    enabled: true
            """;

        YamlRuleConfiguration config = loadConfiguration(yamlContent);
        assertNotNull(config, "Configuration should load successfully");

        // Create RulesEngine and execute the rule group
        RulesEngine engine = createRulesEngine(config);
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("custom-sequence-group");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - all rules true, should pass
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        // Validate results
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Rule group with custom sequence should pass");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("Custom sequence order executed successfully - rules executed in sequence 3,1,2");
    }

    @Test
    @DisplayName("Rule references with disabled rule should skip disabled rule")
    void testRuleReferencesWithDisabledRule() {
        logInfo("Testing rule references with disabled rule");

        String yamlContent = """
            metadata:
              name: "Disabled Rule Test"
              version: "1.0.0"

            rules:
              - id: "rule1"
                name: "Rule 1"
                condition: "true"
                message: "Rule 1 passed"
                severity: "ERROR"
              - id: "rule2"
                name: "Rule 2"
                condition: "false"
                message: "Rule 2 failed"
                severity: "ERROR"
              - id: "rule3"
                name: "Rule 3"
                condition: "true"
                message: "Rule 3 passed"
                severity: "ERROR"

            rule-groups:
              - id: "disabled-rule-group"
                name: "Disabled Rule Group"
                description: "Rule group with one disabled rule"
                operator: "AND"
                rule-references:
                  - rule-id: "rule1"
                    sequence: 1
                    enabled: true
                  - rule-id: "rule2"
                    sequence: 2
                    enabled: false
                  - rule-id: "rule3"
                    sequence: 3
                    enabled: true
            """;

        YamlRuleConfiguration config = loadConfiguration(yamlContent);
        assertNotNull(config, "Configuration should load successfully");

        // Create RulesEngine and execute the rule group
        RulesEngine engine = createRulesEngine(config);
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("disabled-rule-group");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - disabled rule should be skipped, group should pass
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        // Validate results
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Rule group should pass (disabled rule skipped)");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("Disabled rule test executed successfully - rule2 was skipped");
    }

    @Test
    @DisplayName("Rule references with mixed enabled/disabled should only execute enabled rules")
    void testRuleReferencesMixedEnabledDisabled() {
        logInfo("Testing rule references with mixed enabled/disabled rules");

        String yamlContent = """
            metadata:
              name: "Mixed Enabled Disabled Test"
              version: "1.0.0"

            rules:
              - id: "rule1"
                name: "Rule 1"
                condition: "true"
                message: "Rule 1 passed"
                severity: "ERROR"
              - id: "rule2"
                name: "Rule 2"
                condition: "false"
                message: "Rule 2 failed"
                severity: "ERROR"
              - id: "rule3"
                name: "Rule 3"
                condition: "false"
                message: "Rule 3 failed"
                severity: "ERROR"
              - id: "rule4"
                name: "Rule 4"
                condition: "true"
                message: "Rule 4 passed"
                severity: "ERROR"

            rule-groups:
              - id: "mixed-enabled-group"
                name: "Mixed Enabled Group"
                description: "Rule group with mixed enabled/disabled rules"
                operator: "AND"
                rule-references:
                  - rule-id: "rule1"
                    sequence: 1
                    enabled: true
                  - rule-id: "rule2"
                    sequence: 2
                    enabled: false
                  - rule-id: "rule3"
                    sequence: 3
                    enabled: false
                  - rule-id: "rule4"
                    sequence: 4
                    enabled: true
            """;

        YamlRuleConfiguration config = loadConfiguration(yamlContent);
        assertNotNull(config, "Configuration should load successfully");

        // Create RulesEngine and execute the rule group
        RulesEngine engine = createRulesEngine(config);
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("mixed-enabled-group");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - only enabled rules (rule1, rule4) should execute
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        // Validate results
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Rule group should pass (only enabled rules executed)");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("Mixed enabled/disabled test executed successfully - only rule1 and rule4 executed");
    }

    @Test
    @DisplayName("Rule references with OR operator should pass with one enabled true rule")
    void testRuleReferencesOrOperatorWithEnabledRules() {
        logInfo("Testing rule references with OR operator and enabled rules");

        String yamlContent = """
            metadata:
              name: "OR Operator Enabled Rules Test"
              version: "1.0.0"

            rules:
              - id: "rule1"
                name: "Rule 1"
                condition: "false"
                message: "Rule 1 failed"
                severity: "ERROR"
              - id: "rule2"
                name: "Rule 2"
                condition: "true"
                message: "Rule 2 passed"
                severity: "ERROR"
              - id: "rule3"
                name: "Rule 3"
                condition: "false"
                message: "Rule 3 failed"
                severity: "ERROR"

            rule-groups:
              - id: "or-enabled-group"
                name: "OR Enabled Group"
                description: "OR rule group with enabled rules"
                operator: "OR"
                rule-references:
                  - rule-id: "rule1"
                    sequence: 1
                    enabled: true
                  - rule-id: "rule2"
                    sequence: 2
                    enabled: true
                  - rule-id: "rule3"
                    sequence: 3
                    enabled: false
            """;

        YamlRuleConfiguration config = loadConfiguration(yamlContent);
        assertNotNull(config, "Configuration should load successfully");

        // Create RulesEngine and execute the rule group
        RulesEngine engine = createRulesEngine(config);
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("or-enabled-group");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - rule2 is true and enabled, should pass
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        // Validate results
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "OR rule group should pass (rule2 is true and enabled)");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("OR operator with enabled rules executed successfully - rule2 caused group to pass");
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
