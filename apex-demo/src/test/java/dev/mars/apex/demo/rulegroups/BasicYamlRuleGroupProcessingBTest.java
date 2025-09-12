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
 * Basic YAML Rule Group Processing Tests with Rule References.
 * 
 * Tests rule groups using advanced "rule-references" instead of simple "rule-ids".
 * Rule references allow additional configuration like sequence, enabled status, 
 * and priority overrides. Tests progressively from 1 to 6 rule references.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Basic YAML Rule Group Processing Tests - Rule References")
public class BasicYamlRuleGroupProcessingBTest {

    private static final Logger logger = LoggerFactory.getLogger(BasicYamlRuleGroupProcessingBTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        this.yamlLoader = new YamlConfigurationLoader();
        this.rulesEngineService = new YamlRulesEngineService();
    }
    
    @Nested
    @DisplayName("AND Group Logic with Rule References")
    class AndGroupTests {
        
        @Test
        @DisplayName("AND group with 1 rule reference should pass")
        void testAndGroupOneReference() {
            logInfo("Testing AND group with 1 rule reference");
            
            String yamlContent = """
                metadata:
                  name: "AND Group One Reference Test"
                  version: "1.0.0"
                  description: "Test AND group with 1 rule reference"

                rules:
                  - id: "rule1"
                    name: "Always True Rule 1"
                    condition: "true"
                    message: "Rule 1 passed"
                    severity: "ERROR"
                    priority: 1

                rule-groups:
                  - id: "and-one-ref"
                    name: "AND One Reference Group"
                    description: "AND group with 1 rule reference"
                    operator: "AND"
                    rule-references:
                      - rule-id: "rule1"
                        sequence: 1
                        enabled: true
                        override-priority: 5
                """;
            
            // Load configuration - focus on rule group processing, not YAML structure
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

            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-one-ref");
            assertNotNull(ruleGroup, "Rule group should be found");

            // Execute rule group - single true rule, so AND group should pass
            Map<String, Object> testData = Map.of();
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

            // Validate results
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isTriggered(), "AND group with 1 true rule reference should pass");

            // Print RuleResult message
            logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

            logSuccess("AND group with 1 rule reference executed successfully - group passed");
        }

        //=======================================================================================================

        @Test
        @DisplayName("AND group with 2 rule references should pass")
        void testAndGroupTwoReferences() {
            logInfo("Testing AND group with 2 rule references");
            
            String yamlContent = """
                metadata:
                  name: "AND Group Two References Test"
                  version: "1.0.0"

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
                    severity: "WARNING"
                    priority: 2

                rule-groups:
                  - id: "and-two-refs"
                    name: "AND Two References Group"
                    description: "AND group with 2 rule references"
                    operator: "AND"
                    rule-references:
                      - rule-id: "rule1"
                        sequence: 1
                        enabled: true
                        override-priority: 5
                      - rule-id: "rule2"
                        sequence: 2
                        enabled: true
                        override-priority: 10
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

            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-two-refs");
            assertNotNull(ruleGroup, "Rule group should be found");

            // Execute rule group - both rules true, so AND group should pass
            Map<String, Object> testData = Map.of();
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

            // Validate results
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isTriggered(), "AND group with 2 true rule references should pass");

            // Print RuleResult message
            logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

            logSuccess("AND group with 2 rule references executed successfully - group passed");
        }

        @Test
        @DisplayName("AND group with 3 rule references should pass")
        void testAndGroupThreeReferences() {
            logInfo("Testing AND group with 3 rule references");
            
            String yamlContent = """
                metadata:
                  name: "AND Group Three References Test"
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
                    severity: "WARNING"
                  - id: "rule3"
                    name: "Always True Rule 3"
                    condition: "true"
                    message: "Rule 3 passed"
                    severity: "INFO"

                rule-groups:
                  - id: "and-three-refs"
                    name: "AND Three References Group"
                    description: "AND group with 3 rule references"
                    operator: "AND"
                    rule-references:
                      - rule-id: "rule1"
                        sequence: 1
                        enabled: true
                        override-priority: 5
                      - rule-id: "rule2"
                        sequence: 2
                        enabled: true
                        override-priority: 10
                      - rule-id: "rule3"
                        sequence: 3
                        enabled: true
                        override-priority: 15
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

            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-three-refs");
            assertNotNull(ruleGroup, "Rule group should be found");

            // Execute rule group - all 3 rules true, so AND group should pass
            Map<String, Object> testData = Map.of();
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

            // Validate results
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isTriggered(), "AND group with 3 true rule references should pass");

            // Print RuleResult message
            logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

            logSuccess("AND group with 3 rule references executed successfully - group passed");
        }

        //=======================================================================================================

        @Test
        @DisplayName("AND group with 4 rule references should pass")
        void testAndGroupFourReferences() {
            logInfo("Testing AND group with 4 rule references");

            String yamlContent = """
                metadata:
                  name: "AND Group Four References Test"
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
                    severity: "WARNING"
                  - id: "rule3"
                    name: "Always True Rule 3"
                    condition: "true"
                    message: "Rule 3 passed"
                    severity: "INFO"
                  - id: "rule4"
                    name: "Always True Rule 4"
                    condition: "true"
                    message: "Rule 4 passed"
                    severity: "ERROR"

                rule-groups:
                  - id: "and-four-refs"
                    name: "AND Four References Group"
                    description: "AND group with 4 rule references"
                    operator: "AND"
                    rule-references:
                      - rule-id: "rule1"
                        sequence: 1
                        enabled: true
                        override-priority: 5
                      - rule-id: "rule2"
                        sequence: 2
                        enabled: true
                        override-priority: 10
                      - rule-id: "rule3"
                        sequence: 3
                        enabled: true
                        override-priority: 15
                      - rule-id: "rule4"
                        sequence: 4
                        enabled: true
                        override-priority: 20
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

            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-four-refs");
            assertNotNull(ruleGroup, "Rule group should be found");

            // Execute rule group - all 4 rules true, so AND group should pass
            Map<String, Object> testData = Map.of();
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

            // Validate results
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isTriggered(), "AND group with 4 true rule references should pass");

            // Print RuleResult message
            logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

            logSuccess("AND group with 4 rule references executed successfully - group passed");
        }

        //=======================================================================================================

        @Test
        @DisplayName("AND group with 5 rule references should pass")
        void testAndGroupFiveReferences() {
            logInfo("Testing AND group with 5 rule references");

            String yamlContent = """
                metadata:
                  name: "AND Group Five References Test"
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
                    severity: "WARNING"
                  - id: "rule3"
                    name: "Always True Rule 3"
                    condition: "true"
                    message: "Rule 3 passed"
                    severity: "INFO"
                  - id: "rule4"
                    name: "Always True Rule 4"
                    condition: "true"
                    message: "Rule 4 passed"
                    severity: "ERROR"
                  - id: "rule5"
                    name: "Always True Rule 5"
                    condition: "true"
                    message: "Rule 5 passed"
                    severity: "WARNING"

                rule-groups:
                  - id: "and-five-refs"
                    name: "AND Five References Group"
                    description: "AND group with 5 rule references"
                    operator: "AND"
                    rule-references:
                      - rule-id: "rule1"
                        sequence: 1
                        enabled: true
                        override-priority: 5
                      - rule-id: "rule2"
                        sequence: 2
                        enabled: true
                        override-priority: 10
                      - rule-id: "rule3"
                        sequence: 3
                        enabled: true
                        override-priority: 15
                      - rule-id: "rule4"
                        sequence: 4
                        enabled: true
                        override-priority: 20
                      - rule-id: "rule5"
                        sequence: 5
                        enabled: true
                        override-priority: 25
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

            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-five-refs");
            assertNotNull(ruleGroup, "Rule group should be found");

            // Execute rule group - all 5 rules true, so AND group should pass
            Map<String, Object> testData = Map.of();
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

            // Validate results
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isTriggered(), "AND group with 5 true rule references should pass");

            // Print RuleResult message
            logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

            logSuccess("AND group with 5 rule references executed successfully - group passed");
        }

        @Test
        @DisplayName("AND group with 6 rule references should pass")
        void testAndGroupSixReferences() {
            logInfo("Testing AND group with 6 rule references");

            String yamlContent = """
                metadata:
                  name: "AND Group Six References Test"
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
                    severity: "WARNING"
                  - id: "rule3"
                    name: "Always True Rule 3"
                    condition: "true"
                    message: "Rule 3 passed"
                    severity: "INFO"
                  - id: "rule4"
                    name: "Always True Rule 4"
                    condition: "true"
                    message: "Rule 4 passed"
                    severity: "ERROR"
                  - id: "rule5"
                    name: "Always True Rule 5"
                    condition: "true"
                    message: "Rule 5 passed"
                    severity: "WARNING"
                  - id: "rule6"
                    name: "Always True Rule 6"
                    condition: "true"
                    message: "Rule 6 passed"
                    severity: "INFO"

                rule-groups:
                  - id: "and-six-refs"
                    name: "AND Six References Group"
                    description: "AND group with 6 rule references"
                    operator: "AND"
                    rule-references:
                      - rule-id: "rule1"
                        sequence: 1
                        enabled: true
                        override-priority: 5
                      - rule-id: "rule2"
                        sequence: 2
                        enabled: true
                        override-priority: 10
                      - rule-id: "rule3"
                        sequence: 3
                        enabled: true
                        override-priority: 15
                      - rule-id: "rule4"
                        sequence: 4
                        enabled: true
                        override-priority: 20
                      - rule-id: "rule5"
                        sequence: 5
                        enabled: true
                        override-priority: 25
                      - rule-id: "rule6"
                        sequence: 6
                        enabled: true
                        override-priority: 30
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

            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("and-six-refs");
            assertNotNull(ruleGroup, "Rule group should be found");

            // Execute rule group - all 6 rules true, so AND group should pass
            Map<String, Object> testData = Map.of();
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

            // Validate results
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isTriggered(), "AND group with 6 true rule references should pass");

            // Print RuleResult message
            logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

            logSuccess("AND group with 6 rule references executed successfully - group passed");
        }
    }

    @Nested
    @DisplayName("OR Group Logic with Rule References")
    class OrGroupTests {

        @Test
        @DisplayName("OR group with 3 rule references should pass")
        void testOrGroupThreeReferences() {
            logInfo("Testing OR group with 3 rule references");

            String yamlContent = """
                metadata:
                  name: "OR Group Three References Test"
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
                    severity: "WARNING"
                  - id: "rule3"
                    name: "Always False Rule 3"
                    condition: "false"
                    message: "Rule 3 failed"
                    severity: "INFO"

                rule-groups:
                  - id: "or-three-refs"
                    name: "OR Three References Group"
                    description: "OR group with 3 rule references"
                    operator: "OR"
                    rule-references:
                      - rule-id: "rule1"
                        sequence: 1
                        enabled: true
                        override-priority: 5
                      - rule-id: "rule2"
                        sequence: 2
                        enabled: true
                        override-priority: 10
                      - rule-id: "rule3"
                        sequence: 3
                        enabled: true
                        override-priority: 15
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

            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("or-three-refs");
            assertNotNull(ruleGroup, "Rule group should be found");

            // Execute rule group - has one true rule, so OR group should pass
            Map<String, Object> testData = Map.of();
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

            // Validate results
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isTriggered(), "OR group with 3 rule references should pass");

            // Print RuleResult message
            logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

            logSuccess("OR group with 3 rule references executed successfully - group passed");
        }

        @Test
        @DisplayName("OR group with disabled rule reference should pass")
        void testOrGroupWithDisabledReference() {
            logInfo("Testing OR group with disabled rule reference");

            String yamlContent = """
                metadata:
                  name: "OR Group Disabled Reference Test"
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
                    severity: "WARNING"
                  - id: "rule3"
                    name: "Always False Rule 3"
                    condition: "false"
                    message: "Rule 3 failed"
                    severity: "INFO"

                rule-groups:
                  - id: "or-disabled-ref"
                    name: "OR Disabled Reference Group"
                    description: "OR group with one disabled rule reference"
                    operator: "OR"
                    rule-references:
                      - rule-id: "rule1"
                        sequence: 1
                        enabled: true
                        override-priority: 5
                      - rule-id: "rule2"
                        sequence: 2
                        enabled: true
                        override-priority: 10
                      - rule-id: "rule3"
                        sequence: 3
                        enabled: false
                        override-priority: 15
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

            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("or-disabled-ref");
            assertNotNull(ruleGroup, "Rule group should be found");

            // Execute rule group - has enabled true rule, disabled false rule ignored, so OR group should pass
            Map<String, Object> testData = Map.of();
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);

            // Validate results
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isTriggered(), "OR group with disabled rule reference should pass (disabled rule ignored)");

            // Print RuleResult message
            logInfo("RuleResult message: " + (result.getMessage() != null ? result.getMessage() : "No message"));

            logSuccess("OR group with disabled rule reference executed successfully - group passed (disabled rule ignored)");
        }
    }
}
