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
package dev.mars.apex.demo.basic;

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
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

/**
 * Basic YAML Rule Group Processing Tests - Alternative Implementation.
 * 
 * This test class demonstrates APEX rule group processing using the YAML files
 * in the basic-rules directory. It tests both combined configuration and 
 * separate file loading approaches.
 * 
 * YAML Files Used:
 * - combined-config.yaml: Complete configuration with rules and rule groups
 * - rules.yaml: Rules-only configuration
 * - rule-groups.yaml: Rule groups-only configuration
 * 
 * Test Coverage:
 * - Combined YAML configuration loading and processing
 * - Separate file loading with rules and rule groups
 * - AND/OR rule group logic validation
 * - Rule execution and result validation
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Basic YAML Rule Group Processing Tests - Alternative")
public class BasicYamlRuleGroupProcessingATest {

    private static final Logger logger = LoggerFactory.getLogger(BasicYamlRuleGroupProcessingATest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        logger.info("Setting up APEX services for basic rule group processing tests...");
        this.yamlLoader = new YamlConfigurationLoader();
        this.rulesEngineService = new YamlRulesEngineService();
        logger.info(" APEX services initialized successfully");
    }

    /**
     * Test basic rule group processing using combined configuration.
     * Uses: combined-config.yaml
     * 
     * This test validates:
     * - Loading combined YAML configuration with rules and rule groups
     * - AND group logic with true rules (should pass)
     * - OR group logic with mixed rules (should pass)
     * - AND group logic with mixed rules (should fail)
     */
    @Test
    @DisplayName("Test basic rule group processing with combined configuration")
    void testBasicRuleGroupProcessing() {
        logger.info("=== Testing Basic Rule Group Processing with Combined Configuration ===");
        
        try {
            // Load combined configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/basic/BasicYamlRuleGroupProcessingATest.yaml"
            );
            
            assertNotNull(config, "Configuration should be loaded");
            assertEquals("Separate Rules Test - Combined Configuration", config.getMetadata().getName());
            assertEquals(3, config.getRules().size(), "Should have 3 rules");
            assertEquals(3, config.getRuleGroups().size(), "Should have 3 rule groups");
            
            logger.info(" Configuration loaded: {} rules, {} rule groups", 
                config.getRules().size(), config.getRuleGroups().size());
            
            // Create RulesEngine
            RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
            assertNotNull(engine, "RulesEngine should be created");
            
            // Test AND group with true rules (should pass)
            testAndGroupWithTrueRules(engine);
            
            // Test OR group with mixed rules (should pass)
            testOrGroupWithMixedRules(engine);
            
            // Test AND group with mixed rules (should fail)
            testAndGroupWithMixedRules(engine);
            
            logger.info(" All combined configuration tests passed");
            
        } catch (YamlConfigurationException e) {
            logger.error("❌ Failed to load or process configuration: {}", e.getMessage());
            fail("Failed to load or process configuration: " + e.getMessage());
        }
    }

    /**
     * Test basic rule group processing using separate files.
     * Uses: rules.yaml + rule-groups.yaml
     * 
     * This test validates:
     * - Loading separate YAML files for rules and rule groups
     * - Merging configurations from multiple files
     * - Rule group processing with separated configuration
     */
    @Test
    @DisplayName("Test basic rule group processing with separate files")
    void testBasicRuleGroupProcessingWithSeparateFiles() {
        logger.info("=== Testing Basic Rule Group Processing with Separate Files ===");
        
        try {
            // Use APEX's automatic rule reference resolution
            // Load only the rule-groups.yaml file which contains rule-refs to rules.yaml
            // APEX will automatically resolve and load the referenced rules
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/basic/BasicYamlRuleGroupProcessingATest-A.yaml"
            );

            assertNotNull(config, "Configuration should be loaded");
            assertEquals("Separate Rules Test - Rule Groups Only", config.getMetadata().getName());

            // Verify that APEX automatically loaded the referenced rules
            assertNotNull(config.getRules(), "Rules should be automatically loaded via rule-refs");
            assertEquals(3, config.getRules().size(), "Should have 3 rules from referenced file");
            assertEquals(3, config.getRuleGroups().size(), "Should have 3 rule groups");

            logger.info(" Configuration with automatic rule references loaded: {} rules, {} rule groups",
                config.getRules().size(), config.getRuleGroups().size());

            // Create RulesEngine from the configuration with resolved references
            RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
            assertNotNull(engine, "RulesEngine should be created from configuration with resolved references");

            // Verify the engine has the expected rules and rule groups
            assertNotNull(engine.getConfiguration().getRuleGroupById("separate-and-group"),
                "AND group should be found");
            assertNotNull(engine.getConfiguration().getRuleGroupById("separate-or-group"),
                "OR group should be found");
            assertNotNull(engine.getConfiguration().getRuleGroupById("separate-and-mixed-group"),
                "AND mixed group should be found");

            logger.info(" Automatic rule reference resolution successful");
            
            // Test rule group processing with merged configuration
            testAndGroupWithTrueRules(engine);
            testOrGroupWithMixedRules(engine);
            
            logger.info(" All separate files tests passed");
            
        } catch (YamlConfigurationException e) {
            logger.error("❌ Failed to load or process separate configurations: {}", e.getMessage());
            fail("Failed to load or process separate configurations: " + e.getMessage());
        }
    }

    /**
     * Test AND group with true rules (should pass).
     * Enhanced with comprehensive RuleResult API validation.
     */
    private void testAndGroupWithTrueRules(RulesEngine engine) {
        logger.info("Testing AND group with true rules...");

        RuleGroup andGroup = engine.getConfiguration().getRuleGroupById("separate-and-group");
        assertNotNull(andGroup, "AND group should be found");
        assertTrue(andGroup.isAndOperator(), "Should be AND operator");

        // Execute rule group - uses separate-rule-1 (true) and separate-rule-3 (true)
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(andGroup), testData);

        // Enhanced RuleResult validation using new API methods
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "AND group with all true rules should pass");
        assertTrue(result.isSuccess(), "RuleResult.isSuccess() should return true for successful rule group");
        assertFalse(result.hasFailures(), "RuleResult.hasFailures() should return false for successful rule group");
        assertTrue(result.getFailureMessages().isEmpty(), "RuleResult.getFailureMessages() should be empty for success");
        assertNotNull(result.getEnrichedData(), "RuleResult.getEnrichedData() should not be null");

        // Log comprehensive RuleResult details
        logger.info("=== AND Group Success - RuleResult API Details ===");
        logger.info("result.isTriggered(): {}", result.isTriggered());
        logger.info("result.isSuccess(): {}", result.isSuccess());
        logger.info("result.hasFailures(): {}", result.hasFailures());
        logger.info("result.getFailureMessages(): {}", result.getFailureMessages());
        logger.info("result.getEnrichedData(): {}", result.getEnrichedData());
        logger.info("result.getMessage(): {}", result.getMessage() != null ? result.getMessage() : "No message");

        logger.info(" AND group with true rules passed with comprehensive validation");
    }

    /**
     * Test OR group with mixed rules (should pass).
     * Enhanced with comprehensive RuleResult API validation.
     */
    private void testOrGroupWithMixedRules(RulesEngine engine) {
        logger.info("Testing OR group with mixed rules...");

        RuleGroup orGroup = engine.getConfiguration().getRuleGroupById("separate-or-group");
        assertNotNull(orGroup, "OR group should be found");
        assertFalse(orGroup.isAndOperator(), "Should be OR operator");

        // Execute rule group - uses separate-rule-1 (true), separate-rule-2 (false), separate-rule-3 (true)
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(orGroup), testData);

        // Enhanced RuleResult validation using new API methods
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "OR group with at least one true rule should pass");
        assertTrue(result.isSuccess(), "RuleResult.isSuccess() should return true for successful OR group");
        assertFalse(result.hasFailures(), "RuleResult.hasFailures() should return false for successful OR group");
        assertTrue(result.getFailureMessages().isEmpty(), "RuleResult.getFailureMessages() should be empty for success");
        assertNotNull(result.getEnrichedData(), "RuleResult.getEnrichedData() should not be null");

        // Log comprehensive RuleResult details
        logger.info("=== OR Group Success - RuleResult API Details ===");
        logger.info("result.isTriggered(): {}", result.isTriggered());
        logger.info("result.isSuccess(): {}", result.isSuccess());
        logger.info("result.hasFailures(): {}", result.hasFailures());
        logger.info("result.getFailureMessages(): {}", result.getFailureMessages());
        logger.info("result.getEnrichedData(): {}", result.getEnrichedData());
        logger.info("result.getMessage(): {}", result.getMessage() != null ? result.getMessage() : "No message");

        logger.info(" OR group with mixed rules passed with comprehensive validation");
    }

    /**
     * Test AND group with mixed rules (should fail).
     * Enhanced with comprehensive RuleResult API validation for failure scenarios.
     */
    private void testAndGroupWithMixedRules(RulesEngine engine) {
        logger.info("Testing AND group with mixed rules...");

        RuleGroup andMixedGroup = engine.getConfiguration().getRuleGroupById("separate-and-mixed-group");
        assertNotNull(andMixedGroup, "AND mixed group should be found");
        assertTrue(andMixedGroup.isAndOperator(), "Should be AND operator");

        // Execute rule group - uses separate-rule-1 (true), separate-rule-2 (false), separate-rule-3 (true)
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(andMixedGroup), testData);

        // Enhanced RuleResult validation for failure scenario
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "AND group with mixed rules should fail");

        // Note: For rule group failures, isSuccess() may still be true as the evaluation completed successfully
        // The failure is in the business logic (rule group didn't match), not in the execution
        assertNotNull(result.getEnrichedData(), "RuleResult.getEnrichedData() should not be null even for failures");

        // Log comprehensive RuleResult details for failure case
        logger.info("=== AND Group Failure - RuleResult API Details ===");
        logger.info("result.isTriggered(): {}", result.isTriggered());
        logger.info("result.isSuccess(): {}", result.isSuccess());
        logger.info("result.hasFailures(): {}", result.hasFailures());
        logger.info("result.getFailureMessages(): {}", result.getFailureMessages());
        logger.info("result.getEnrichedData(): {}", result.getEnrichedData());
        logger.info("result.getMessage(): {}", result.getMessage() != null ? result.getMessage() : "No message");

        logger.info(" AND group with mixed rules failed as expected with comprehensive validation");
    }

    /**
     * Test comprehensive RuleResult API methods for rule group processing.
     * This test demonstrates all the enhanced RuleResult API methods added for
     * comprehensive result validation and programmatic access.
     */
    @Test
    @DisplayName("Test comprehensive RuleResult API methods for rule groups")
    void testRuleResultApiMethodsForRuleGroups() {
        logger.info("=== Testing Comprehensive RuleResult API Methods for Rule Groups ===");

        try {
            // Load combined configuration for this test
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/basic/BasicYamlRuleGroupProcessingATest.yaml"
            );

            assertNotNull(config, "Configuration should be loaded");

            // Create RulesEngine
            RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
            assertNotNull(engine, "RulesEngine should be created");

            // Test 1: Successful rule group (AND with all true rules)
            logger.info("--- Test 1: Successful AND Rule Group ---");
            RuleGroup successGroup = engine.getConfiguration().getRuleGroupById("separate-and-group");
            assertNotNull(successGroup, "Success group should be found");

            Map<String, Object> testData = Map.of();
            RuleResult successResult = engine.executeRuleGroupsList(List.of(successGroup), testData);

            // Demonstrate all RuleResult API methods for success case
            logger.info("=== Demonstrating RuleResult API Methods - Success Case ===");
            logger.info("result.isTriggered(): {}", successResult.isTriggered());
            logger.info("result.isSuccess(): {}", successResult.isSuccess());
            logger.info("result.hasFailures(): {}", successResult.hasFailures());
            logger.info("result.getFailureMessages(): {}", successResult.getFailureMessages());
            logger.info("result.getEnrichedData(): {}", successResult.getEnrichedData());
            logger.info("result.getRuleName(): {}", successResult.getRuleName());
            logger.info("result.getMessage(): {}", successResult.getMessage());
            logger.info("result.getResultType(): {}", successResult.getResultType());
            logger.info("result.getTimestamp(): {}", successResult.getTimestamp());

            // Verify API methods work correctly for success case
            assertTrue(successResult.isTriggered(), "isTriggered() should return true for successful rule group");
            assertTrue(successResult.isSuccess(), "isSuccess() should return true for successful rule group");
            assertFalse(successResult.hasFailures(), "hasFailures() should return false for successful rule group");
            assertTrue(successResult.getFailureMessages().isEmpty(), "getFailureMessages() should return empty list for success");
            assertNotNull(successResult.getEnrichedData(), "getEnrichedData() should return data map");
            assertNotNull(successResult.getRuleName(), "getRuleName() should not be null");
            assertNotNull(successResult.getTimestamp(), "getTimestamp() should not be null");

            // Test 2: Failed rule group (AND with mixed rules)
            logger.info("--- Test 2: Failed AND Rule Group ---");
            RuleGroup failGroup = engine.getConfiguration().getRuleGroupById("separate-and-mixed-group");
            assertNotNull(failGroup, "Fail group should be found");

            RuleResult failResult = engine.executeRuleGroupsList(List.of(failGroup), testData);

            // Demonstrate all RuleResult API methods for failure case
            logger.info("=== Demonstrating RuleResult API Methods - Failure Case ===");
            logger.info("result.isTriggered(): {}", failResult.isTriggered());
            logger.info("result.isSuccess(): {}", failResult.isSuccess());
            logger.info("result.hasFailures(): {}", failResult.hasFailures());
            logger.info("result.getFailureMessages(): {}", failResult.getFailureMessages());
            logger.info("result.getEnrichedData(): {}", failResult.getEnrichedData());
            logger.info("result.getRuleName(): {}", failResult.getRuleName());
            logger.info("result.getMessage(): {}", failResult.getMessage());
            logger.info("result.getResultType(): {}", failResult.getResultType());
            logger.info("result.getTimestamp(): {}", failResult.getTimestamp());

            // Verify API methods work correctly for failure case
            assertFalse(failResult.isTriggered(), "isTriggered() should return false for failed rule group");
            assertNotNull(failResult.getEnrichedData(), "getEnrichedData() should still return data map even on failure");
            assertNotNull(failResult.getRuleName(), "getRuleName() should not be null");
            assertNotNull(failResult.getTimestamp(), "getTimestamp() should not be null");

            logger.info(" All RuleResult API methods working correctly for rule groups");
            logger.info(" APEX system provides complete programmatic access to rule group results");

        } catch (YamlConfigurationException e) {
            logger.error("❌ Failed to load or process configuration: {}", e.getMessage());
            fail("Failed to load or process configuration: " + e.getMessage());
        }
    }


}
