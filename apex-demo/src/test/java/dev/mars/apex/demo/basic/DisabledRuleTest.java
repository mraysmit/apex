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
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that rules with enabled: false are skipped during evaluation.
 * 
 * This test validates the fix for a bug where the item-level processing path
 * (APEX 2.1+) did not check the enabled flag on individual rules, causing
 * disabled rules to still be evaluated.
 *
 * CRITICAL VALIDATION CHECKLIST:
 * 1. Disabled rules (enabled: false) must NOT be evaluated
 * 2. Enabled rules (enabled: true or omitted) must be evaluated normally
 * 3. The disabled rule should return a NO_MATCH result with "disabled" message
 * 4. Other rules in the same config should not be affected
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Disabled Rule Processing Tests")
class DisabledRuleTest {

    private static final Logger logger = LoggerFactory.getLogger(DisabledRuleTest.class);

    @Test
    @DisplayName("Disabled rule should be skipped - not evaluated")
    void testDisabledRuleIsSkipped() throws YamlConfigurationException {
        // Load YAML configuration
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/basic/DisabledRuleTest.yaml");
        assertNotNull(config, "YAML configuration should load successfully");

        // Verify YAML has 3 rules (2 enabled + 1 disabled)
        assertNotNull(config.getRules(), "Rules should not be null");
        assertEquals(3, config.getRules().size(), "YAML should contain 3 rules");

        // Create rules engine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        // Test data that would match all conditions
        Map<String, Object> testData = new HashMap<>();
        testData.put("age", 25);
        testData.put("name", "John");

        // Execute evaluation
        RuleResult result = engine.evaluate(testData);

        // Result should be successful overall
        assertTrue(result.isSuccess(), "Overall evaluation should succeed");

        // Get individual rule results
        List<RuleResult> childResults = result.getChildResults();
        assertNotNull(childResults, "Child results should not be null");

        logger.info("Total child results: {}", childResults.size());
        for (RuleResult child : childResults) {
            logger.info("Rule: id={}, name={} -> triggered={}, resultType={}, message='{}'",
                    child.getRuleId(), child.getRuleName(), child.isTriggered(), child.getResultType(), child.getMessage());
        }

        // Should have results for all 3 rules (2 matched + 1 disabled/skipped)
        assertEquals(3, childResults.size(), "Should have results for all 3 rules");

        // Find the disabled rule result (disabled rules get ID as ruleName from noMatch factory)
        RuleResult disabledResult = childResults.stream()
                .filter(r -> "disabled-rule".equals(r.getRuleName()) || "disabled-rule".equals(r.getRuleId()))
                .findFirst()
                .orElse(null);
        assertNotNull(disabledResult, "Should have a result for the disabled rule");
        assertFalse(disabledResult.isTriggered(), "Disabled rule should NOT be triggered");
        assertEquals(RuleResult.ResultType.NO_MATCH, disabledResult.getResultType(),
                "Disabled rule should have NO_MATCH result type");
        assertTrue(disabledResult.getMessage().contains("disabled"),
                "Disabled rule message should indicate it was disabled");

        // Verify enabled rules were evaluated normally (normal rules have ruleId set)
        RuleResult enabledResult = childResults.stream()
                .filter(r -> "enabled-rule".equals(r.getRuleId()))
                .findFirst()
                .orElse(null);
        assertNotNull(enabledResult, "Should have result for enabled-rule");
        assertTrue(enabledResult.isTriggered(), "Enabled rule should be triggered (age 25 >= 18)");

        RuleResult anotherEnabledResult = childResults.stream()
                .filter(r -> "another-enabled-rule".equals(r.getRuleId()))
                .findFirst()
                .orElse(null);
        assertNotNull(anotherEnabledResult, "Should have result for another-enabled-rule");
        assertTrue(anotherEnabledResult.isTriggered(), "Another enabled rule should be triggered (name != null)");
    }

    @Test
    @DisplayName("Rule with enabled: true should work normally")
    void testEnabledRuleWorksNormally() throws YamlConfigurationException {
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/basic/DisabledRuleTest.yaml");
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> testData = new HashMap<>();
        testData.put("age", 15); // Below 18, so enabled-rule should NOT match
        testData.put("name", "Jane");

        RuleResult result = engine.evaluate(testData);

        List<RuleResult> childResults = result.getChildResults();
        assertNotNull(childResults);

        // enabled-rule condition is #age >= 18, age=15 should NOT match
        RuleResult enabledResult = childResults.stream()
                .filter(r -> "enabled-rule".equals(r.getRuleId()))
                .findFirst()
                .orElse(null);
        assertNotNull(enabledResult);
        assertFalse(enabledResult.isTriggered(), "enabled-rule should not trigger for age=15");

        // disabled-rule should still be skipped regardless of data
        RuleResult disabledResult = childResults.stream()
                .filter(r -> "disabled-rule".equals(r.getRuleName()) || "disabled-rule".equals(r.getRuleId()))
                .findFirst()
                .orElse(null);
        assertNotNull(disabledResult, "Disabled rule should have a result");
        assertFalse(disabledResult.isTriggered(), "Disabled rule should NOT be triggered");
        assertTrue(disabledResult.getMessage().contains("disabled"),
                "Disabled rule message should indicate it was disabled");

        // another-enabled-rule (#name != null) should still match
        RuleResult anotherResult = childResults.stream()
                .filter(r -> "another-enabled-rule".equals(r.getRuleId()))
                .findFirst()
                .orElse(null);
        assertNotNull(anotherResult);
        assertTrue(anotherResult.isTriggered(), "another-enabled-rule should trigger for non-null name");
    }
}
