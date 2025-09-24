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
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple YAML Validation Rule Demo Test
 *
 * This demonstrates the simplest possible YAML validation rule in APEX:
 * - Load a YAML configuration with a single validation rule
 * - Create a rules engine from the YAML configuration
 * - Test the validation rule with different data scenarios
 *
 * The YAML rule validates that a person's age is at least 18.
 *
 * Converted from demo to proper JUnit test following established patterns.
 */
@ExtendWith(ColoredTestOutputExtension.class)
public class SimpleYamlValidationDemo {

    private static final Logger logger = LoggerFactory.getLogger(SimpleYamlValidationDemo.class);

    @Test
    public void testSimpleYamlValidationDemo() {
        logger.info("=== APEX Simple YAML Validation Rule Demo ===");
        
        try {
            // Step 1: Load the YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.fromYamlString(getSimpleYamlRule());

            assertNotNull(config, "Configuration should be loaded");
            assertNotNull(config.getMetadata(), "Metadata should be present");
            assertEquals("Simple Age Validation", config.getMetadata().getName());
            assertEquals(1, config.getRules().size(), "Should have exactly 1 rule");

            logger.info("✓ YAML configuration loaded: {}", config.getMetadata().getName());
            logger.info("  Rules count: {}", config.getRules().size());
            
            // Step 2: Create rules engine from YAML
            YamlRulesEngineService yamlService = new YamlRulesEngineService();
            RulesEngine engine = yamlService.createRulesEngineFromYamlConfig(config);

            assertNotNull(engine, "RulesEngine should be created");
            logger.info("✓ Rules engine created successfully");
            
            // Step 3: Test with valid data (age >= 18)
            logger.info("--- Test 1: Valid Age (25) ---");
            Map<String, Object> validData = Map.of("age", 25);

            // Get the age validation rule from the configuration
            var ageValidationRule = engine.getConfiguration().getRuleById("age-check");
            assertNotNull(ageValidationRule, "Age check rule should be found in configuration");

            // Execute rule using correct API
            RuleResult validResult = engine.executeRule(ageValidationRule, validData);

            assertNotNull(validResult, "Result should not be null");
            logger.info("Input data: {}", validData);
            logger.info("Rule triggered: {}", validResult.isTriggered());
            logger.info("Validation passed: {}", validResult.isSuccess());
            logger.info("Message: {}", validResult.getMessage());

            // Validate business logic: Age 25 should pass validation (age >= 18)
            assertTrue(validResult.isTriggered(), "Rule should be triggered for age 25");
            assertTrue(validResult.isSuccess(), "Validation should succeed for age 25");
            
            // Step 4: Test with invalid data (age < 18)
            logger.info("--- Test 2: Invalid Age (16) ---");
            Map<String, Object> invalidData = Map.of("age", 16);

            // Use the same rule but with invalid data - the rule should NOT trigger for age < 18
            RuleResult invalidResult = engine.executeRule(ageValidationRule, invalidData);

            assertNotNull(invalidResult, "Result should not be null");
            logger.info("Input data: {}", invalidData);
            logger.info("Rule triggered: {}", invalidResult.isTriggered());
            logger.info("Validation passed: {}", invalidResult.isSuccess());
            logger.info("Message: {}", invalidResult.getMessage());

            // Validate business logic: Age 16 should NOT trigger the rule (age < 18)
            assertFalse(invalidResult.isTriggered(), "Rule should NOT be triggered for age 16 (< 18)");
            assertTrue(invalidResult.isSuccess(), "Rule execution should succeed (rule evaluated correctly)");
            
            // Step 5: Test with missing data
            logger.info("--- Test 3: Missing Age Data ---");
            Map<String, Object> missingData = Map.of("name", "John");

            // Use the same rule but with missing age data - should handle gracefully
            RuleResult missingResult = engine.executeRule(ageValidationRule, missingData);

            assertNotNull(missingResult, "Result should not be null");
            logger.info("Input data: {}", missingData);
            logger.info("Rule triggered: {}", missingResult.isTriggered());
            logger.info("Validation passed: {}", missingResult.isSuccess());
            logger.info("Message: {}", missingResult.getMessage());

            // Validate business logic: Missing age should not trigger rule and should fail validation
            assertFalse(missingResult.isTriggered(), "Rule should not be triggered when age is missing");
            assertFalse(missingResult.isSuccess(), "Validation should fail when required field is missing");

            logger.info("=== Demo completed successfully! ===");

        } catch (YamlConfigurationException e) {
            logger.error("❌ Demo failed: {}", e.getMessage());
            fail("Demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Returns the simplest possible YAML validation rule as a string.
     * This rule validates that age >= 18.
     */
    private static String getSimpleYamlRule() {
        return """
            metadata:
              name: "Simple Age Validation"
              version: "1.0.0"
              description: "The simplest possible YAML validation rule"
              type: "rule-config"
              author: "APEX Demo"
            
            rules:
              - id: "age-check"
                name: "Age Validation"
                condition: "#age >= 18"
                message: "Age must be at least 18"
                enabled: true
            """;
    }
}
