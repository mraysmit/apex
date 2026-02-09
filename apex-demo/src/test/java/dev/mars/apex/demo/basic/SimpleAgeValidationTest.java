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

import dev.mars.apex.core.config.exception.YamlConfigurationException;
import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.engine.core.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple Age Validation Test
 * 
 * This test demonstrates the simplest possible YAML validation rule in APEX.
 * It follows the established patterns from existing tests and validates:
 * - YAML configuration loading
 * - Rules engine creation
 * - Rule execution with different test data scenarios
 * - RuleResult API validation
 * 
 * YAML File: simple-age-validation.yaml
 * Rules Tested:
 * - age-check: Validates age >= 18
 * - age-required: Validates age field is present
 * - age-too-young: Identifies age < 18
 * 
 * Following prompts.txt principles:
 * - Test actual functionality, not just YAML syntax
 * - Use real APEX operations with RuleResult validation
 * - Follow existing working patterns from BasicYamlRuleGroupProcessingATest
 */
@ExtendWith(ColoredTestOutputExtension.class)
public class SimpleAgeValidationTest {

    private static final Logger logger = LoggerFactory.getLogger(SimpleAgeValidationTest.class);

    @Test
    public void testValidAgeScenario() {
        logger.info("=== Testing Valid Age Scenario (age = 25) ===");
        
        try {
            // Load YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/basic/SimpleAgeValidationTest.yaml");
            assertNotNull(config, "Configuration should be loaded");
            assertEquals("Simple Age Validation", config.getMetadata().getName());
            assertEquals(3, config.getRules().size(), "Should have exactly 3 validation rules");
            
            // Create RulesEngine using static factory method
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            assertNotNull(engine, "RulesEngine should be created");
            
            // Get the age validation rule from the configuration
            var ageCheckRule = engine.getConfiguration().getRuleById("age-check");
            assertNotNull(ageCheckRule, "Age check rule should be found in configuration");

            // Test data: Valid adult age
            Map<String, Object> validPersonData = Map.of("age", 25);

            // Execute rule and validate results
            RuleResult result = engine.executeRule(ageCheckRule, validPersonData);
            assertNotNull(result, "RuleResult should not be null");

            // Validate RuleResult API methods following established patterns
            logger.info("RuleResult.isTriggered(): {}", result.isTriggered());
            logger.info("RuleResult.isSuccess(): {}", result.isSuccess());
            logger.info("RuleResult.getMessage(): {}", result.getMessage());
            logger.info("RuleResult.getRuleName(): {}", result.getRuleName());

            // Business logic validation: Age 25 should pass age-check rule
            assertTrue(result.isTriggered(), "Age check rule should be triggered for age 25");
            assertTrue(result.isSuccess(), "Age validation should succeed for age 25");
            assertNotNull(result.getMessage(), "Result should have a message");
            assertTrue(result.getMessage().contains("25"), 
                      "Message should contain the resolved age value '25', got: " + result.getMessage());
            assertFalse(result.getMessage().contains("{{#"), 
                      "Message should not contain unresolved {{#}} placeholders, got: " + result.getMessage());
            
            logger.info("[OK] Valid age scenario test passed");
            
        } catch (YamlConfigurationException e) {
            logger.error("X Failed to load configuration: {}", e.getMessage());
            fail("Failed to load configuration: " + e.getMessage());
        }
    }

    @Test
    public void testInvalidAgeScenario() {
        logger.info("=== Testing Invalid Age Scenario (age = 16) ===");
        
        try {
            // Load YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/basic/SimpleAgeValidationTest.yaml");
            assertNotNull(config, "Configuration should be loaded");
            
            // Create RulesEngine using static factory method
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            assertNotNull(engine, "RulesEngine should be created");
            
            // Get the age-too-young rule from the configuration
            var ageTooYoungRule = engine.getConfiguration().getRuleById("age-too-young");
            assertNotNull(ageTooYoungRule, "Age too young rule should be found in configuration");

            // Test data: Invalid underage
            Map<String, Object> underagePersonData = Map.of("age", 16);

            // Execute rule and validate results
            RuleResult result = engine.executeRule(ageTooYoungRule, underagePersonData);
            assertNotNull(result, "RuleResult should not be null");

            // Validate RuleResult API methods
            logger.info("RuleResult.isTriggered(): {}", result.isTriggered());
            logger.info("RuleResult.isSuccess(): {}", result.isSuccess());
            logger.info("RuleResult.getMessage(): {}", result.getMessage());
            logger.info("RuleResult.getRuleName(): {}", result.getRuleName());

            // Business logic validation: Age 16 should trigger age-too-young rule
            assertTrue(result.isTriggered(), "Age too young rule should be triggered for age 16");
            assertTrue(result.isSuccess(), "Rule execution should succeed");
            assertNotNull(result.getMessage(), "Result should have a message");
            assertTrue(result.getMessage().contains("16"), 
                      "Message should contain the resolved age value '16', got: " + result.getMessage());
            assertFalse(result.getMessage().contains("{{#"), 
                      "Message should not contain unresolved {{#}} placeholders, got: " + result.getMessage());
            
            logger.info("[OK] Invalid age scenario test passed");
            
        } catch (YamlConfigurationException e) {
            logger.error("X Failed to load configuration: {}", e.getMessage());
            fail("Failed to load configuration: " + e.getMessage());
        }
    }

    @Test
    public void testMissingAgeScenario() {
        logger.info("=== Testing Missing Age Scenario (no age field) ===");
        
        try {
            // Load YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/basic/SimpleAgeValidationTest.yaml");
            assertNotNull(config, "Configuration should be loaded");
            
            // Create RulesEngine using static factory method
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            assertNotNull(engine, "RulesEngine should be created");
            
            // Get the age-required rule from the configuration
            var ageRequiredRule = engine.getConfiguration().getRuleById("age-required");
            assertNotNull(ageRequiredRule, "Age required rule should be found in configuration");

            // Test data: Missing age field
            Map<String, Object> noAgeData = Map.of("name", "John Doe");

            // Execute rule and validate results
            RuleResult result = engine.executeRule(ageRequiredRule, noAgeData);
            assertNotNull(result, "RuleResult should not be null");

            // Validate RuleResult API methods
            logger.info("RuleResult.isTriggered(): {}", result.isTriggered());
            logger.info("RuleResult.isSuccess(): {}", result.isSuccess());
            logger.info("RuleResult.getMessage(): {}", result.getMessage());
            logger.info("RuleResult.getRuleName(): {}", result.getRuleName());

            // Business logic validation: Missing age should not trigger age-required rule
            // and rule execution should fail due to missing parameters
            assertFalse(result.isTriggered(), "Age required rule should not be triggered when age is missing");
            assertFalse(result.isSuccess(), "Rule execution should fail due to missing parameters");
            assertNotNull(result.getMessage(), "Result should have a message");
            // When age is missing, the error message comes from the error recovery system
            // (not the rule's template message), so check for error-related content
            assertTrue(result.getMessage().contains("Age") || result.getMessage().contains("evaluation failed"), 
                      "Message should reference age field or evaluation failure");
            
            logger.info("[OK] Missing age scenario test passed");
            
        } catch (YamlConfigurationException e) {
            logger.error("X Failed to load configuration: {}", e.getMessage());
            fail("Failed to load configuration: " + e.getMessage());
        }
    }

    @Test
    public void testNoMatchMessageScenario() {
        logger.info("=== Testing No-Match-Message Scenario (age = 16, age-check rule) ===");
        
        try {
            // Load YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/basic/SimpleAgeValidationTest.yaml");
            assertNotNull(config, "Configuration should be loaded");
            
            // Create RulesEngine using static factory method
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            assertNotNull(engine, "RulesEngine should be created");
            
            // Get the age-check rule (has both message and no-match-message)
            var ageCheckRule = engine.getConfiguration().getRuleById("age-check");
            assertNotNull(ageCheckRule, "Age check rule should be found in configuration");

            // Test data: Age 16 does NOT meet condition (#age >= 18) → should use no-match-message
            Map<String, Object> underageData = Map.of("age", 16);

            // Execute rule and validate results
            RuleResult result = engine.executeRule(ageCheckRule, underageData);
            assertNotNull(result, "RuleResult should not be null");

            logger.info("RuleResult.isTriggered(): {}", result.isTriggered());
            logger.info("RuleResult.getMessage(): {}", result.getMessage());

            // Rule condition (#age >= 18) is FALSE for age=16 → NO_MATCH
            assertFalse(result.isTriggered(), "Age check rule should NOT match for age 16");
            
            // The no-match-message should be used with resolved {{#age}} placeholder
            assertEquals("Age 16 does not meet minimum requirement of 18", result.getMessage(),
                        "NO_MATCH should use no-match-message with resolved {{#age}} placeholder");
            assertFalse(result.getMessage().contains("{{#"), 
                      "Message should not contain unresolved {{#}} placeholders");
            
            logger.info("[OK] No-match-message scenario test passed");
            
        } catch (YamlConfigurationException e) {
            logger.error("X Failed to load configuration: {}", e.getMessage());
            fail("Failed to load configuration: " + e.getMessage());
        }
    }

    @Test
    public void testMatchMessageStillWorksWithNoMatchMessage() {
        logger.info("=== Testing Match Message with no-match-message configured (age = 25) ===");
        
        try {
            // Load YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/basic/SimpleAgeValidationTest.yaml");
            
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            var ageCheckRule = engine.getConfiguration().getRuleById("age-check");
            assertNotNull(ageCheckRule, "Age check rule should be found");

            // Test data: Age 25 MEETS condition → should use standard message (not no-match-message)
            Map<String, Object> validData = Map.of("age", 25);
            RuleResult result = engine.executeRule(ageCheckRule, validData);

            logger.info("RuleResult.isTriggered(): {}", result.isTriggered());
            logger.info("RuleResult.getMessage(): {}", result.getMessage());

            // MATCH path should use the standard message
            assertTrue(result.isTriggered(), "Age check rule should match for age 25");
            assertEquals("Age 25 is valid (18 or older)", result.getMessage(),
                        "MATCH should use standard message even when no-match-message is configured");
            
            logger.info("[OK] Match message with no-match-message configured test passed");
            
        } catch (YamlConfigurationException e) {
            fail("Failed to load configuration: " + e.getMessage());
        }
    }
}
