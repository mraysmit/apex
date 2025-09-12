/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
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

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static dev.mars.apex.demo.ColoredTestOutputExtension.*;

/**
 * Stop-On-First-Failure AND Group Tests.
 * 
 * Tests AND rule groups with stop-on-first-failure behavior enabled and disabled.
 * Demonstrates short-circuit evaluation where AND groups stop on the first false result
 * when stop-on-first-failure is enabled.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Stop-On-First-Failure AND Group Tests")
public class StopOnFirstFailureAndGroupTest {

    private static final Logger logger = LoggerFactory.getLogger(StopOnFirstFailureAndGroupTest.class);

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
              description: "AND group with all true rules should execute all rules"

            rules:
              - id: "rule1"
                name: "Always True Rule 1"
                condition: "true"
                message: "Rule 1 passed"
                severity: "ERROR"
                priority: 1
              - id: "rule2"
                name: "Always True Rule 2"
                condition: "true"
                message: "Rule 2 passed"
                severity: "ERROR"
                priority: 2
              - id: "rule3"
                name: "Always True Rule 3"
                condition: "true"
                message: "Rule 3 passed"
                severity: "ERROR"
                priority: 3

            rule-groups:
              - id: "and-stop-all-true"
                name: "AND Stop All True"
                description: "AND group with stop-on-first-failure and all true rules"
                operator: "AND"
                stop-on-first-failure: true
                rule-ids:
                  - "rule1"
                  - "rule2"
                  - "rule3"
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
        assertNotNull(engine, "RulesEngine should be created successfully");

        // Get the rule group and execute it with empty test data
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-stop-all-true");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - all rules are true, so group should pass
        Map<String, Object> testData = Map.of(); // Empty data since rules use literal true/false
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        // Validate results
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Rule group should pass (all rules true)");
        assertEquals("AND Stop All True", result.getRuleName(), "Should return rule group name");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("All true rules executed successfully - all rules ran, group passed");
    }

    @Test
    @DisplayName("AND group with stop-on-first-failure: first rule false")
    void testAndGroupStopOnFirstFailure_FirstFalse() {
        logInfo("Testing AND group stop-on-first-failure with first rule false");
        
        String yamlContent = """
            metadata:
              name: "Stop On First Failure - First False"
              version: "1.0.0"
              description: "AND group should stop immediately on first false rule"

            rules:
              - id: "rule1"
                name: "Always False Rule 1"
                condition: "false"
                message: "Rule 1 failed"
                severity: "ERROR"
                priority: 1
              - id: "rule2"
                name: "Always True Rule 2"
                condition: "true"
                message: "Rule 2 passed"
                severity: "ERROR"
                priority: 2
              - id: "rule3"
                name: "Always True Rule 3"
                condition: "true"
                message: "Rule 3 passed"
                severity: "ERROR"
                priority: 3

            rule-groups:
              - id: "and-stop-first-false"
                name: "AND Stop First False"
                description: "AND group should stop on first false rule"
                operator: "AND"
                stop-on-first-failure: true
                rule-ids:
                  - "rule1"
                  - "rule2"
                  - "rule3"
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
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-stop-first-false");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - first rule is false, should stop immediately
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        // Validate results - AND group with first rule false should fail
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Rule group should fail (first rule false, stop immediately)");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("First false rule executed - stopped immediately as expected");
    }

    @Test
    @DisplayName("AND group with stop-on-first-failure: middle rule false")
    void testAndGroupStopOnFirstFailure_MiddleFalse() {
        logInfo("Testing AND group stop-on-first-failure with middle rule false");
        
        String yamlContent = """
            metadata:
              name: "Stop On First Failure - Middle False"
              version: "1.0.0"
              description: "AND group should stop at middle false rule"

            rules:
              - id: "rule1"
                name: "Always True Rule 1"
                condition: "true"
                message: "Rule 1 passed"
                severity: "ERROR"
                priority: 1
              - id: "rule2"
                name: "Always False Rule 2"
                condition: "false"
                message: "Rule 2 failed"
                severity: "ERROR"
                priority: 2
              - id: "rule3"
                name: "Always True Rule 3"
                condition: "true"
                message: "Rule 3 passed"
                severity: "ERROR"
                priority: 3

            rule-groups:
              - id: "and-stop-middle-false"
                name: "AND Stop Middle False"
                description: "AND group should stop at middle false rule"
                operator: "AND"
                stop-on-first-failure: true
                rule-ids:
                  - "rule1"
                  - "rule2"
                  - "rule3"
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
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-stop-middle-false");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - middle rule is false, should stop at rule 2
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        // Validate results - AND group with middle rule false should fail
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Rule group should fail (middle rule false, stopped at rule 2)");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("Middle false rule executed - stopped at rule 2 as expected");
    }

    @Test
    @DisplayName("AND group with stop-on-first-failure: last rule false")
    void testAndGroupStopOnFirstFailure_LastFalse() {
        logInfo("Testing AND group stop-on-first-failure with last rule false");
        
        String yamlContent = """
            metadata:
              name: "Stop On First Failure - Last False"
              version: "1.0.0"
              description: "AND group should execute all rules when last is false"

            rules:
              - id: "rule1"
                name: "Always True Rule 1"
                condition: "true"
                message: "Rule 1 passed"
                severity: "ERROR"
                priority: 1
              - id: "rule2"
                name: "Always True Rule 2"
                condition: "true"
                message: "Rule 2 passed"
                severity: "ERROR"
                priority: 2
              - id: "rule3"
                name: "Always False Rule 3"
                condition: "false"
                message: "Rule 3 failed"
                severity: "ERROR"
                priority: 3

            rule-groups:
              - id: "and-stop-last-false"
                name: "AND Stop Last False"
                description: "AND group executes all rules when last is false"
                operator: "AND"
                stop-on-first-failure: true
                rule-ids:
                  - "rule1"
                  - "rule2"
                  - "rule3"
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
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-stop-last-false");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - last rule is false, should execute all rules but fail
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        // Validate results - AND group with last rule false should fail (but all rules executed)
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Rule group should fail (last rule false, all rules executed)");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("Last false rule executed - all rules ran, group failed as expected");
    }

    @Test
    @DisplayName("AND group with stop-on-first-failure: multiple rules with middle failure")
    void testAndGroupStopOnFirstFailure_MultipleRules() {
        logInfo("Testing AND group stop-on-first-failure with 5 rules (T,T,F,T,T)");

        String yamlContent = """
            metadata:
              name: "Stop On First Failure - Multiple Rules"
              version: "1.0.0"
              description: "AND group with 5 rules should stop at 3rd rule failure"

            rules:
              - id: "rule1"
                name: "Always True Rule 1"
                condition: "true"
                message: "Rule 1 passed"
                severity: "ERROR"
                priority: 1
              - id: "rule2"
                name: "Always True Rule 2"
                condition: "true"
                message: "Rule 2 passed"
                severity: "ERROR"
                priority: 2
              - id: "rule3"
                name: "Always False Rule 3"
                condition: "false"
                message: "Rule 3 failed"
                severity: "ERROR"
                priority: 3
              - id: "rule4"
                name: "Always True Rule 4"
                condition: "true"
                message: "Rule 4 passed"
                severity: "ERROR"
                priority: 4
              - id: "rule5"
                name: "Always True Rule 5"
                condition: "true"
                message: "Rule 5 passed"
                severity: "ERROR"
                priority: 5

            rule-groups:
              - id: "and-stop-multiple-rules"
                name: "AND Stop Multiple Rules"
                description: "AND group with 5 rules should stop at 3rd rule"
                operator: "AND"
                stop-on-first-failure: true
                rule-ids:
                  - "rule1"
                  - "rule2"
                  - "rule3"
                  - "rule4"
                  - "rule5"
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
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-stop-multiple-rules");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - 5 rules (T,T,F,T,T), should stop at rule 3
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        // Validate results - AND group should fail at rule 3, rules 4 and 5 not executed
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Rule group should fail (stopped at rule 3)");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("Multiple rules executed - stopped at rule 3 as expected, rules 4 and 5 not executed");
    }

    @Test
    @DisplayName("AND group with stop-on-first-failure disabled")
    void testAndGroupStopOnFirstFailure_Disabled() {
        logInfo("Testing AND group with stop-on-first-failure disabled");

        String yamlContent = """
            metadata:
              name: "Stop On First Failure - Disabled"
              version: "1.0.0"
              description: "AND group should execute all rules when stop-on-first-failure is disabled"

            rules:
              - id: "rule1"
                name: "Always True Rule 1"
                condition: "true"
                message: "Rule 1 passed"
                severity: "ERROR"
                priority: 1
              - id: "rule2"
                name: "Always False Rule 2"
                condition: "false"
                message: "Rule 2 failed"
                severity: "ERROR"
                priority: 2
              - id: "rule3"
                name: "Always True Rule 3"
                condition: "true"
                message: "Rule 3 passed"
                severity: "ERROR"
                priority: 3
              - id: "rule4"
                name: "Always False Rule 4"
                condition: "false"
                message: "Rule 4 failed"
                severity: "ERROR"
                priority: 4

            rule-groups:
              - id: "and-stop-disabled"
                name: "AND Stop Disabled"
                description: "AND group with stop-on-first-failure disabled"
                operator: "AND"
                stop-on-first-failure: false
                rule-ids:
                  - "rule1"
                  - "rule2"
                  - "rule3"
                  - "rule4"
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
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-stop-disabled");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - stop-on-first-failure disabled, should execute all rules
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        // Validate results - AND group should fail but all rules executed
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Rule group should fail (has false rules) but all rules executed");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("Stop-on-first-failure disabled - all 4 rules executed despite failures");
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
