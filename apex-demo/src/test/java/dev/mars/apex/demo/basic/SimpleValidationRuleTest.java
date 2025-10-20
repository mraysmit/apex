/*
 * Copyright 2024 APEX Demo Team
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

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic test for SimpleValidationRuleTest.yaml
 * Tests the most basic YAML rule loading and execution.
 */
@ExtendWith(ColoredTestOutputExtension.class)
class SimpleValidationRuleTest {

    private final YamlRulesEngineService yamlService = new YamlRulesEngineService();

    @Test
    @DisplayName("Test simple validation rule with valid age")
    void testValidAge() {
        try {
            // Load YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/basic/SimpleValidationRuleTest.yaml");

            // Create rules engine
            RulesEngine engine = yamlService.createRulesEngineFromYamlConfig(config);

            // Test data - age 25 (should pass)
            Map<String, Object> testData = Map.of("age", 25);

            // Execute rule
            var rule = engine.getConfiguration().getRuleById("age-check");
            RuleResult result = engine.executeRule(rule, testData);

            // Validate result
            assertTrue(result.isTriggered(), "Rule should trigger for valid age");
            assertTrue(result.isSuccess(), "Rule execution should be successful");
            assertEquals("Age Validation", result.getRuleName(), "Rule name should match");
            assertNotNull(result.getMessage(), "Result should have a message");
        } catch (Exception e) {
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test simple validation rule with invalid age")
    void testInvalidAge() {
        try {
            // Load YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/basic/SimpleValidationRuleTest.yaml");

            // Create rules engine
            RulesEngine engine = yamlService.createRulesEngineFromYamlConfig(config);

            // Test data - age 16 (should not pass)
            Map<String, Object> testData = Map.of("age", 16);

            // Execute rule
            var rule = engine.getConfiguration().getRuleById("age-check");
            RuleResult result = engine.executeRule(rule, testData);

            // Validate result
            assertFalse(result.isTriggered(), "Rule should not trigger for invalid age");
            assertTrue(result.isSuccess(), "Rule execution should still be successful");
            assertEquals("Age Validation", result.getRuleName(), "Rule name should be preserved for non-triggered rule");
        } catch (Exception e) {
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test simple validation rule with missing age")
    void testMissingAge() {
        try {
            // Load YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/basic/SimpleValidationRuleTest.yaml");

            // Create rules engine
            RulesEngine engine = yamlService.createRulesEngineFromYamlConfig(config);

            // Test data - no age field
            Map<String, Object> testData = Map.of("name", "John");

            // Execute rule
            var rule = engine.getConfiguration().getRuleById("age-check");
            RuleResult result = engine.executeRule(rule, testData);

            // Validate result
            assertFalse(result.isTriggered(), "Rule should not trigger for missing age");
            assertFalse(result.isSuccess(), "Rule execution should fail for missing data");
            assertEquals("Age Validation", result.getRuleName(), "Rule name should match");
        } catch (Exception e) {
            fail("Test should not throw exception: " + e.getMessage());
        }
    }
}
