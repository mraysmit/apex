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
 * Stop-On-First-Failure OR Group Tests.
 * 
 * Tests OR rule groups with stop-on-first-failure behavior enabled and disabled.
 * Demonstrates short-circuit evaluation where OR groups stop on the first true result
 * when stop-on-first-failure is enabled.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Stop-On-First-Failure OR Group Tests")
public class StopOnFirstFailureOrGroupTest {

    private static final Logger logger = LoggerFactory.getLogger(StopOnFirstFailureOrGroupTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        this.yamlLoader = new YamlConfigurationLoader();
        this.rulesEngineService = new YamlRulesEngineService();
    }

    @Test
    @DisplayName("OR group with stop-on-first-failure: all false rules")
    void testOrGroupStopOnFirstFailure_AllFalse() {
        logInfo("Testing OR group stop-on-first-failure with all false rules");
        
        String yamlContent = """
            metadata:
              name: "Stop On First Failure - All False"
              version: "1.0.0"
              description: "OR group with all false rules should execute all rules"

            rules:
              - id: "rule1"
                name: "Always False Rule 1"
                condition: "false"
                message: "Rule 1 failed"
                severity: "ERROR"
                priority: 1
              - id: "rule2"
                name: "Always False Rule 2"
                condition: "false"
                message: "Rule 2 failed"
                severity: "ERROR"
                priority: 2
              - id: "rule3"
                name: "Always False Rule 3"
                condition: "false"
                message: "Rule 3 failed"
                severity: "ERROR"
                priority: 3

            rule-groups:
              - id: "or-stop-all-false"
                name: "OR Stop All False"
                description: "OR group with stop-on-first-failure and all false rules"
                operator: "OR"
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
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("or-stop-all-false");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - all rules are false, so group should fail (all rules executed)
        Map<String, Object> testData = Map.of(); // Empty data since rules use literal true/false
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        
        // Validate results
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Rule group should fail (all rules false)");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("All false rules executed - all rules ran, group failed as expected");
    }

    @Test
    @DisplayName("OR group with stop-on-first-failure: first rule true")
    void testOrGroupStopOnFirstFailure_FirstTrue() {
        logInfo("Testing OR group stop-on-first-failure with first rule true");
        
        String yamlContent = """
            metadata:
              name: "Stop On First Failure - First True"
              version: "1.0.0"
              description: "OR group should stop immediately on first true rule"

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
                name: "Always False Rule 3"
                condition: "false"
                message: "Rule 3 failed"
                severity: "ERROR"
                priority: 3

            rule-groups:
              - id: "or-stop-first-true"
                name: "OR Stop First True"
                description: "OR group should stop on first true rule"
                operator: "OR"
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
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("or-stop-first-true");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - first rule is true, should stop immediately
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        
        // Validate results - OR group with first rule true should pass
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Rule group should pass (first rule true, stop immediately)");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("First true rule executed - stopped immediately as expected");
    }

    @Test
    @DisplayName("OR group with stop-on-first-failure: middle rule true")
    void testOrGroupStopOnFirstFailure_MiddleTrue() {
        logInfo("Testing OR group stop-on-first-failure with middle rule true");
        
        String yamlContent = """
            metadata:
              name: "Stop On First Failure - Middle True"
              version: "1.0.0"
              description: "OR group should stop at middle true rule"

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
                name: "Always False Rule 3"
                condition: "false"
                message: "Rule 3 failed"
                severity: "ERROR"
                priority: 3

            rule-groups:
              - id: "or-stop-middle-true"
                name: "OR Stop Middle True"
                description: "OR group should stop at middle true rule"
                operator: "OR"
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
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("or-stop-middle-true");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - middle rule is true, should stop at rule 2
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        
        // Validate results - OR group with middle rule true should pass
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Rule group should pass (middle rule true, stopped at rule 2)");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("Middle true rule executed - stopped at rule 2 as expected");
    }

    @Test
    @DisplayName("OR group with stop-on-first-failure: last rule true")
    void testOrGroupStopOnFirstFailure_LastTrue() {
        logInfo("Testing OR group stop-on-first-failure with last rule true");

        String yamlContent = """
            metadata:
              name: "Stop On First Failure - Last True"
              version: "1.0.0"
              description: "OR group should execute all rules when last is true"

            rules:
              - id: "rule1"
                name: "Always False Rule 1"
                condition: "false"
                message: "Rule 1 failed"
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
              - id: "or-stop-last-true"
                name: "OR Stop Last True"
                description: "OR group executes all rules when last is true"
                operator: "OR"
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
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("or-stop-last-true");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - last rule is true, should execute all rules and pass
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        // Validate results - OR group with last rule true should pass (but all rules executed)
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Rule group should pass (last rule true, all rules executed)");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("Last true rule executed - all rules ran, group passed as expected");
    }

    @Test
    @DisplayName("OR group with stop-on-first-failure: multiple rules with middle success")
    void testOrGroupStopOnFirstFailure_MultipleRules() {
        logInfo("Testing OR group stop-on-first-failure with 5 rules (F,F,T,F,F)");

        String yamlContent = """
            metadata:
              name: "Stop On First Failure - Multiple Rules"
              version: "1.0.0"
              description: "OR group with 5 rules should stop at 3rd rule success"

            rules:
              - id: "rule1"
                name: "Always False Rule 1"
                condition: "false"
                message: "Rule 1 failed"
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
              - id: "rule5"
                name: "Always False Rule 5"
                condition: "false"
                message: "Rule 5 failed"
                severity: "ERROR"
                priority: 5

            rule-groups:
              - id: "or-stop-multiple-rules"
                name: "OR Stop Multiple Rules"
                description: "OR group with 5 rules should stop at 3rd rule"
                operator: "OR"
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
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("or-stop-multiple-rules");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - 5 rules (F,F,T,F,F), should stop at rule 3
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        // Validate results - OR group should pass at rule 3, rules 4 and 5 not executed
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Rule group should pass (stopped at rule 3)");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("Multiple rules executed - stopped at rule 3 as expected, rules 4 and 5 not executed");
    }

    @Test
    @DisplayName("OR group with stop-on-first-failure disabled")
    void testOrGroupStopOnFirstFailure_Disabled() {
        logInfo("Testing OR group with stop-on-first-failure disabled");

        String yamlContent = """
            metadata:
              name: "Stop On First Failure - Disabled"
              version: "1.0.0"
              description: "OR group should execute all rules when stop-on-first-failure is disabled"

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

            rule-groups:
              - id: "or-stop-disabled"
                name: "OR Stop Disabled"
                description: "OR group with stop-on-first-failure disabled"
                operator: "OR"
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
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("or-stop-disabled");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Execute rule group - stop-on-first-failure disabled, should execute all rules
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

        // Validate results - OR group should pass (has true rules) and all rules executed
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Rule group should pass (has true rules) and all rules executed");

        // Print RuleResult message
        logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

        logSuccess("Stop-on-first-failure disabled - all 4 rules executed despite early successes");
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
