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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic test for MinimalRuleTest.yaml
 * Tests the minimal YAML rule configuration.
 */
@ExtendWith(ColoredTestOutputExtension.class)
class MinimalRuleTest {

    private final YamlRulesEngineService yamlService = new YamlRulesEngineService();

    @Test
    @DisplayName("Test minimal rule execution")
    void testMinimalRule() {
        try {
            // Load YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/basic/MinimalRuleTest.yaml");

            // Create rules engine
            RulesEngine engine = yamlService.createRulesEngineFromYamlConfig(config);

            // Test data - age 20 (should pass)
            Map<String, Object> testData = Map.of("age", 20);

            // Execute rule
            var rule = engine.getConfiguration().getRuleById("simple-check");
            RuleResult result = engine.executeRule(rule, testData);

            // Validate result
            assertTrue(result.isTriggered());
            assertTrue(result.isSuccess());
            assertNotNull(result.getMessage());
        } catch (Exception e) {
            fail("Test should not throw exception: " + e.getMessage());
        }
    }
}
