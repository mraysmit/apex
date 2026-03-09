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

import dev.mars.apex.core.config.loader.ConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minimal YAML Rule Functionality Test
 * Tests actual rule execution functionality using minimal YAML configuration.
 *
 * COMPLIANCE: Follows "Always test actual functionality" principle by testing
 * rule execution logic rather than YAML syntax validation.
 */
@ExtendWith(ColoredTestOutputExtension.class)
class MinimalYamlValidationTest {

    @Test
    @DisplayName("Test minimal rule functionality with multiple scenarios")
    void testMinimalRuleFunctionality() {
        try {
            // Load YAML configuration from file (not inline string)
            ConfigurationLoader loader = new ConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/basic/MinimalRuleTest.yaml");

            // Create rules engine using static factory method
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            var rule = engine.getConfiguration().getRuleById("simple-check");

            // Test Case 1: Valid scenario (age >= 18, should trigger)
            Map<String, Object> validData = Map.of("age", 25);
            RuleResult validResult = engine.executeRule(rule, validData);

            assertTrue(validResult.isTriggered(), "Rule should trigger for valid age");
            assertTrue(validResult.isSuccess(), "Rule execution should be successful");
            assertEquals("Simple Age Check", validResult.getRuleName(), "Rule name should match");

            // Test Case 2: Invalid scenario (age < 18, should not trigger)
            Map<String, Object> invalidData = Map.of("age", 16);
            RuleResult invalidResult = engine.executeRule(rule, invalidData);

            assertFalse(invalidResult.isTriggered(), "Rule should not trigger for invalid age");
            assertTrue(invalidResult.isSuccess(), "Rule execution should still be successful");

            // Test Case 3: Missing data scenario
            Map<String, Object> emptyData = Map.of("name", "John");
            RuleResult emptyResult = engine.executeRule(rule, emptyData);

            assertFalse(emptyResult.isTriggered(), "Rule should not trigger for missing age");
            // SpEL null-safe: #age >= 18 evaluates to false when age is null, no error
            assertTrue(emptyResult.isSuccess(), "Rule execution should succeed (null-safe condition)");

        } catch (Exception e) {
            fail("Failed to execute minimal rule functionality test: " + e.getMessage());
        }
    }
}
