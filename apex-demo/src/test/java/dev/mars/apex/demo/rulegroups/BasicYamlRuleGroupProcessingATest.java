package dev.mars.apex.demo.rulegroups;

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

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static dev.mars.apex.demo.ColoredTestOutputExtension.*;

/**
 * Basic YAML Rule Group Processing Tests.
 * 
 * Tests fundamental AND/OR logic with hardcoded true/false rules in YAML.
 * This is the simplest possible rule group test focusing purely on group 
 * processing logic rather than rule evaluation complexity.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Basic YAML Rule Group Processing Tests")
public class BasicYamlRuleGroupProcessingATest {

    private static final Logger logger = LoggerFactory.getLogger(BasicYamlRuleGroupProcessingATest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        this.yamlLoader = new YamlConfigurationLoader();
        this.rulesEngineService = new YamlRulesEngineService();
    }
    
    @Nested
    @DisplayName("AND Group Logic")
    class AndGroupTests {
        
        @Test
        @DisplayName("AND group with all true rules should pass")
        void testAndGroupAllTrue() {
            logInfo("Testing AND group with all true rules");
            
            String yamlContent = """
                metadata:
                  name: "AND Group All True Test"
                  version: "1.0.0"
                  description: "Test AND group with all true rules"

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
                  - id: "and-all-true"
                    name: "AND All True Group"
                    description: "AND group with all true rules"
                    operator: "AND"
                    rule-ids:
                      - "rule1"
                      - "rule2"
                      - "rule3"
                """;
            
            YamlRuleConfiguration config;
            try {
                config = yamlLoader.fromYamlString(yamlContent);
            } catch (YamlConfigurationException e) {
                logError("Failed to load YAML configuration: " + e.getMessage());
                fail("Failed to load YAML configuration: " + e.getMessage());
                return;
            }
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

            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-all-true");
            assertNotNull(ruleGroup, "Rule group should be found");

            // Execute rule group - all rules are true, so AND group should pass
            Map<String, Object> testData = Map.of();
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

            // Validate results
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isTriggered(), "AND group with all true rules should pass");

            // Print RuleResult message
            logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

            logSuccess("AND group with all true rules executed successfully - group passed");
        }

        //=======================================================================================================

        @Test
        @DisplayName("AND group with one false rule should fail")
        void testAndGroupOneFalse() {
            logInfo("Testing AND group with one false rule");
            
            String yamlContent = """
                metadata:
                  name: "AND Group One False Test"
                  version: "1.0.0"

                rules:
                  - id: "rule1"
                    name: "Always True Rule"
                    condition: "true"
                    message: "Rule 1 passed"
                    severity: "ERROR"
                  - id: "rule2"
                    name: "Always False Rule"
                    condition: "false"
                    message: "Rule 2 failed"
                    severity: "ERROR"
                  - id: "rule3"
                    name: "Always True Rule"
                    condition: "true"
                    message: "Rule 3 passed"
                    severity: "ERROR"

                rule-groups:
                  - id: "and-one-false"
                    name: "AND One False Group"
                    description: "AND group with one false rule"
                    operator: "AND"
                    rule-ids:
                      - "rule1"
                      - "rule2"
                      - "rule3"
                """;
            
            YamlRuleConfiguration config;
            try {
                config = yamlLoader.fromYamlString(yamlContent);
            } catch (YamlConfigurationException e) {
                logError("Failed to load YAML configuration: " + e.getMessage());
                fail("Failed to load YAML configuration: " + e.getMessage());
                return;
            }
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

            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-one-false");
            assertNotNull(ruleGroup, "Rule group should be found");

            // Execute rule group - one rule is false, so AND group should fail
            Map<String, Object> testData = Map.of();
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

            // Validate results
            assertNotNull(result, "Result should not be null");
            assertFalse(result.isTriggered(), "AND group with one false rule should fail");

            // Print RuleResult message
            logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

            logSuccess("AND group with mixed rules executed successfully - group failed as expected");
        }

        //=======================================================================================================
        
        @Test
        @DisplayName("AND group with all false rules should fail")
        void testAndGroupAllFalse() {
            logger.info("TEST: AND group with all false rules");
            
            String yamlContent = """
                metadata:
                  name: "AND Group All False Test"
                  version: "1.0.0"

                rules:
                  - id: "rule1"
                    name: "Always False Rule 1"
                    condition: "false"
                    message: "Rule 1 failed"
                    severity: "ERROR"
                  - id: "rule2"
                    name: "Always False Rule 2"
                    condition: "false"
                    message: "Rule 2 failed"
                    severity: "ERROR"

                rule-groups:
                  - id: "and-all-false"
                    name: "AND All False Group"
                    operator: "AND"
                    rule-ids:
                      - "rule1"
                      - "rule2"
                """;
            
            YamlRuleConfiguration config;
            try {
                config = yamlLoader.fromYamlString(yamlContent);
            } catch (YamlConfigurationException e) {
                fail("Failed to load YAML configuration: " + e.getMessage());
                return;
            }
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

            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-all-false");
            assertNotNull(ruleGroup, "Rule group should be found");

            // Execute rule group - all rules are false, so AND group should fail
            Map<String, Object> testData = Map.of();
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

            // Validate results
            assertNotNull(result, "Result should not be null");
            assertFalse(result.isTriggered(), "AND group with all false rules should fail");

            // Print RuleResult message
            logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

            logSuccess("AND group with all false rules executed successfully - group failed as expected");
        }
    }

    //=======================================================================================================
    
    @Nested
    @DisplayName("OR Group Logic")
    class OrGroupTests {
        
        @Test
        @DisplayName("OR group with one true rule should pass")
        void testOrGroupOneTrue() {
            logger.info("TEST: OR group with one true rule");
            
            String yamlContent = """
                metadata:
                  name: "OR Group One True Test"
                  version: "1.0.0"

                rules:
                  - id: "rule1"
                    name: "Always False Rule 1"
                    condition: "false"
                    message: "Rule 1 failed"
                    severity: "ERROR"
                  - id: "rule2"
                    name: "Always True Rule 2"
                    condition: "true"
                    message: "Rule 2 passed"
                    severity: "ERROR"
                  - id: "rule3"
                    name: "Always False Rule 3"
                    condition: "false"
                    message: "Rule 3 failed"
                    severity: "ERROR"

                rule-groups:
                  - id: "or-one-true"
                    name: "OR One True Group"
                    operator: "OR"
                    rule-ids:
                      - "rule1"
                      - "rule2"
                      - "rule3"
                """;
            
            YamlRuleConfiguration config;
            try {
                config = yamlLoader.fromYamlString(yamlContent);
            } catch (YamlConfigurationException e) {
                fail("Failed to load YAML configuration: " + e.getMessage());
                return;
            }
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

            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("or-one-true");
            assertNotNull(ruleGroup, "Rule group should be found");

            // Execute rule group - one rule is true, so OR group should pass
            Map<String, Object> testData = Map.of();
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

            // Validate results
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isTriggered(), "OR group with one true rule should pass");

            // Print RuleResult message
            logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

            logSuccess("OR group with one true rule executed successfully - group passed");
        }

        //=======================================================================================================
        
        @Test
        @DisplayName("OR group with all true rules should pass")
        void testOrGroupAllTrue() {
            logger.info("TEST: OR group with all true rules");
            
            String yamlContent = """
                metadata:
                  name: "OR Group All True Test"
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
                  - id: "or-all-true"
                    name: "OR All True Group"
                    operator: "OR"
                    rule-ids:
                      - "rule1"
                      - "rule2"
                """;
            
            YamlRuleConfiguration config;
            try {
                config = yamlLoader.fromYamlString(yamlContent);
            } catch (YamlConfigurationException e) {
                fail("Failed to load YAML configuration: " + e.getMessage());
                return;
            }
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

            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("or-all-true");
            assertNotNull(ruleGroup, "Rule group should be found");

            // Execute rule group - all rules are true, so OR group should pass
            Map<String, Object> testData = Map.of();
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

            // Validate results
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isTriggered(), "OR group with all true rules should pass");

            // Print RuleResult message
            logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

            logSuccess("OR group with all true rules executed successfully - group passed");
        }

        //=======================================================================================================
        
        @Test
        @DisplayName("OR group with all false rules should fail")
        void testOrGroupAllFalse() {
            logger.info("TEST: OR group with all false rules");
            
            String yamlContent = """
                metadata:
                  name: "OR Group All False Test"
                  version: "1.0.0"

                rules:
                  - id: "rule1"
                    name: "Always False Rule 1"
                    condition: "false"
                    message: "Rule 1 failed"
                    severity: "ERROR"
                  - id: "rule2"
                    name: "Always False Rule 2"
                    condition: "false"
                    message: "Rule 2 failed"
                    severity: "ERROR"
                  - id: "rule3"
                    name: "Always False Rule 3"
                    condition: "false"
                    message: "Rule 3 failed"
                    severity: "ERROR"

                rule-groups:
                  - id: "or-all-false"
                    name: "OR All False Group"
                    operator: "OR"
                    rule-ids:
                      - "rule1"
                      - "rule2"
                      - "rule3"
                """;
            
            YamlRuleConfiguration config;
            try {
                config = yamlLoader.fromYamlString(yamlContent);
            } catch (YamlConfigurationException e) {
                fail("Failed to load YAML configuration: " + e.getMessage());
                return;
            }
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

            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("or-all-false");
            assertNotNull(ruleGroup, "Rule group should be found");

            // Execute rule group - all rules are false, so OR group should fail
            Map<String, Object> testData = Map.of();
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

            // Validate results
            assertNotNull(result, "Result should not be null");
            assertFalse(result.isTriggered(), "OR group with all false rules should fail");

            // Print RuleResult message
            logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

            logSuccess("OR group with all false rules executed successfully - group failed as expected");
        }
    }
}
