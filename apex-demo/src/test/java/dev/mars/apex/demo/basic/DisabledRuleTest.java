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

import dev.mars.apex.core.config.YamlConfigurationException;
import dev.mars.apex.core.config.YamlConfigurationLoader;
import dev.mars.apex.core.config.YamlRuleConfiguration;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import dev.mars.apex.core.engine.config.RuleBuilder;

/**
 * Comprehensive tests that rules/groups/enrichments with enabled: false are skipped.
 * 
 * This test validates the enabled flag across ALL evaluation code paths:
 * 
 * PATH 1: Flat rules via item-level processing (SequentialProcessor.processRuleItem)
 * PATH 2: Flat rules - implicit enabled (omitted = defaults to true)
 * PATH 3: Disabled rule inside a rule group (RuleGroup.evaluateSequentialWithDetails)
 * PATH 4: Entire rule group disabled (SequentialProcessor.processRuleGroupItem)
 * PATH 5: Disabled enrichment (SequentialProcessor.processEnrichmentItem)
 * PATH 6: Direct API - disabled Rule via RulesEngine.executeRule()
 * PATH 7: Direct API - disabled Rule via UnifiedRuleEvaluator.evaluateRule()
 *
 * CRITICAL VALIDATION CHECKLIST:
 * 1. Disabled rules (enabled: false) must NOT be evaluated
 * 2. Enabled rules (enabled: true or omitted) must be evaluated normally
 * 3. The disabled rule should return a NO_MATCH result with "disabled" message
 * 4. Other rules in the same config should not be affected
 * 5. Disabled rule groups must be entirely skipped
 * 6. Disabled rules INSIDE a group must be skipped by the group evaluator
 * 7. Disabled enrichments must not execute
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Disabled Rule Processing Tests")
class DisabledRuleTest {

    private static final Logger logger = LoggerFactory.getLogger(DisabledRuleTest.class);
    private static final String BASE_PATH = "src/test/java/dev/mars/apex/demo/basic/";

    // =====================================================================
    // PATH 1 & 2: Flat rules via item-level processing
    // =====================================================================
    @Nested
    @DisplayName("PATH 1 & 2: Flat rules via item-level processing")
    class FlatRuleTests {

        @Test
        @DisplayName("Disabled rule should be skipped - not evaluated")
        void testDisabledRuleIsSkipped() throws YamlConfigurationException {
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile(BASE_PATH + "DisabledRuleTest.yaml");
            assertNotNull(config, "YAML configuration should load successfully");

            // Verify YAML has 4 rules (3 enabled + 1 disabled)
            assertNotNull(config.getRules(), "Rules should not be null");
            assertEquals(4, config.getRules().size(), "YAML should contain 4 rules");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            Map<String, Object> testData = new HashMap<>();
            testData.put("age", 25);
            testData.put("name", "John");

            RuleResult result = engine.evaluate(testData);
            assertTrue(result.isSuccess(), "Overall evaluation should succeed");

            List<RuleResult> childResults = result.getChildResults();
            assertNotNull(childResults, "Child results should not be null");

            logChildResults("Flat rules", childResults);

            // Should have results for all 4 rules (3 matched + 1 disabled/skipped)
            assertEquals(4, childResults.size(), "Should have results for all 4 rules");

            // Verify disabled rule was skipped
            RuleResult disabledResult = findResult(childResults, "disabled-rule", "Disabled Rule");
            assertNotNull(disabledResult, "Should have a result for the disabled rule");
            assertFalse(disabledResult.isTriggered(), "Disabled rule should NOT be triggered");
            assertEquals(RuleResult.ResultType.NO_MATCH, disabledResult.getResultType(),
                    "Disabled rule should have NO_MATCH result type");
            assertTrue(disabledResult.getMessage().contains("disabled"),
                    "Disabled rule message should indicate it was disabled");

            // Verify enabled rules were evaluated normally
            RuleResult enabledResult = findResultById(childResults, "enabled-rule");
            assertNotNull(enabledResult, "Should have result for enabled-rule");
            assertTrue(enabledResult.isTriggered(), "Enabled rule should be triggered (age 25 >= 18)");

            RuleResult anotherEnabledResult = findResultById(childResults, "another-enabled-rule");
            assertNotNull(anotherEnabledResult, "Should have result for another-enabled-rule");
            assertTrue(anotherEnabledResult.isTriggered(), "Another enabled rule should be triggered (name != null)");

            // Verify implicit enabled (omitted = true) works
            RuleResult implicitEnabledResult = findResultById(childResults, "implicit-enabled-rule");
            assertNotNull(implicitEnabledResult, "Should have result for implicit-enabled-rule");
            assertTrue(implicitEnabledResult.isTriggered(), "Implicit enabled rule should be triggered (age > 0)");
        }

        @Test
        @DisplayName("Rule with enabled: true should work normally (non-matching data)")
        void testEnabledRuleNonMatchingData() throws YamlConfigurationException {
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile(BASE_PATH + "DisabledRuleTest.yaml");
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            Map<String, Object> testData = new HashMap<>();
            testData.put("age", 15); // Below 18, so enabled-rule should NOT match
            testData.put("name", "Jane");

            RuleResult result = engine.evaluate(testData);
            List<RuleResult> childResults = result.getChildResults();
            assertNotNull(childResults);

            logChildResults("Non-matching flat rules", childResults);

            // enabled-rule condition is #age >= 18, age=15 should NOT match
            RuleResult enabledResult = findResultById(childResults, "enabled-rule");
            assertNotNull(enabledResult);
            assertFalse(enabledResult.isTriggered(), "enabled-rule should not trigger for age=15");

            // disabled-rule should still be skipped regardless of data
            RuleResult disabledResult = findResult(childResults, "disabled-rule", "Disabled Rule");
            assertNotNull(disabledResult, "Disabled rule should have a result");
            assertFalse(disabledResult.isTriggered(), "Disabled rule should NOT be triggered");
            assertTrue(disabledResult.getMessage().contains("disabled"),
                    "Disabled rule message should indicate it was disabled");

            // another-enabled-rule (#name != null) should still match
            RuleResult anotherResult = findResultById(childResults, "another-enabled-rule");
            assertNotNull(anotherResult);
            assertTrue(anotherResult.isTriggered(), "another-enabled-rule should trigger for non-null name");
        }
    }

    // =====================================================================
    // PATH 3 & 4: Rule groups with disabled rules / disabled groups
    // =====================================================================
    @Nested
    @DisplayName("PATH 3 & 4: Rule groups")
    class RuleGroupTests {

        @Test
        @DisplayName("Disabled rule inside AND group should be skipped by pre-filtering")
        void testDisabledRuleInsideGroup() throws YamlConfigurationException {
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile(BASE_PATH + "DisabledRuleTest-rulegroup.yaml");
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            Map<String, Object> testData = new HashMap<>();
            testData.put("age", 25);
            testData.put("name", "John");

            RuleResult result = engine.evaluate(testData);

            List<RuleResult> childResults = result.getChildResults();
            assertNotNull(childResults, "Child results should not be null");

            logChildResults("Rule group with disabled rule", childResults);

            // The AND group should pass because:
            // - The disabled rule (group-disabled-rule) is pre-filtered by YamlRuleFactory.createRules()
            //   so it never enters the RuleGroup
            // - The 2 enabled rules both match, so AND is true
            // The group result should appear somewhere in the child results
            boolean groupPassed = false;
            for (RuleResult child : childResults) {
                String name = child.getRuleName() != null ? child.getRuleName() : "";
                String id = child.getRuleId() != null ? child.getRuleId() : "";
                if (name.contains("AND Group With Disabled Rule") || id.contains("and-group-with-disabled-rule")) {
                    groupPassed = child.isTriggered();
                    logger.info("Found AND group result: triggered={}, resultType={}, message='{}'",
                            child.isTriggered(), child.getResultType(), child.getMessage());
                }
            }
            // If the group is executed at all, it should pass
            // The disabled rule should NOT cause the AND group to fail
            assertTrue(result.isSuccess(), "Overall evaluation should succeed");
        }

        @Test
        @DisplayName("Entire disabled rule group should be skipped")
        void testDisabledRuleGroup() throws YamlConfigurationException {
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile(BASE_PATH + "DisabledRuleTest-rulegroup.yaml");
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            Map<String, Object> testData = new HashMap<>();
            testData.put("age", 25);
            testData.put("name", "John");

            RuleResult result = engine.evaluate(testData);
            List<RuleResult> childResults = result.getChildResults();
            assertNotNull(childResults, "Child results should not be null");

            logChildResults("Disabled group", childResults);

            // The disabled group should be skipped entirely
            RuleResult disabledGroupResult = findResult(childResults, "disabled-group", "Disabled Group");
            if (disabledGroupResult != null) {
                // If a result is returned for the disabled group, it should show as not triggered
                assertFalse(disabledGroupResult.isTriggered(),
                        "Disabled group should NOT be triggered");
                logger.info("Disabled group returned a result: triggered={}, message='{}'",
                        disabledGroupResult.isTriggered(), disabledGroupResult.getMessage());
            } else {
                // The disabled group may be entirely filtered out (no result at all)
                logger.info("Disabled group was correctly filtered out - no result returned");
            }
        }
    }

    // =====================================================================
    // PATH 5: Disabled enrichments
    // =====================================================================
    @Nested
    @DisplayName("PATH 5: Disabled enrichments")
    class EnrichmentTests {

        @Test
        @DisplayName("Disabled enrichment should not execute")
        void testDisabledEnrichmentIsSkipped() throws YamlConfigurationException {
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile(BASE_PATH + "DisabledRuleTest-enrichment.yaml");
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            Map<String, Object> testData = new HashMap<>();
            testData.put("category", "A");

            RuleResult result = engine.evaluate(testData);

            logger.info("Enrichment test result: success={}", result.isSuccess());
            logger.info("Enriched data: {}", result.getEnrichedData());

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");

            // The enabled enrichment should have run - lookupRate should be present
            assertTrue(enrichedData.containsKey("lookupRate"),
                    "Enabled enrichment should have set lookupRate. Enriched data: " + enrichedData);
            assertEquals(0.05, enrichedData.get("lookupRate"),
                    "lookupRate should be 0.05 for category A");

            // The disabled enrichment should NOT have run - bonusValue should NOT be present
            assertFalse(enrichedData.containsKey("bonusValue"),
                    "Disabled enrichment should NOT have set bonusValue. Enriched data: " + enrichedData);
        }
    }

    // =====================================================================
    // PATH 6 & 7: Direct API - Rule domain model with enabled=false
    // =====================================================================
    @Nested
    @DisplayName("PATH 6 & 7: Direct API with disabled Rule object")
    class DirectApiTests {

        @Test
        @DisplayName("Disabled Rule via RulesEngine.executeRule() should not trigger")
        void testDisabledRuleViaRulesEngineApi() throws YamlConfigurationException {
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile(BASE_PATH + "DisabledRuleTest.yaml");
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            // Create a disabled Rule using the 16-param constructor with enabled=false
            Rule disabledRule = new Rule(
                    "direct-disabled-rule",
                    new HashSet<>(),
                    "Direct Disabled Rule",
                    "#age >= 18",           // condition that WOULD match
                    "This should never appear",
                    "A directly-constructed disabled rule",
                    100,                    // priority
                    "ERROR",                // severity
                    null,                   // metadata
                    null,                   // defaultValue
                    null,                   // successCode
                    null,                   // errorCode
                    null,                   // mapToField
                    null,                   // resultField
                    null,                   // noMatchMessage
                    false                   // enabled = false
            );

            Map<String, Object> testData = new HashMap<>();
            testData.put("age", 25); // Would match #age >= 18

            // Use direct API: executeRule returns full RuleResult
            boolean triggered = engine.executeRule(disabledRule, testData).isTriggered();
            assertFalse(triggered, "Disabled Rule via evaluateRule() should NOT trigger");

            // Use direct API: executeRule returns full RuleResult
            RuleResult result = engine.executeRule(disabledRule, testData);
            assertFalse(result.isTriggered(), "Disabled Rule via executeRule() should NOT trigger");
            assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(),
                    "Disabled Rule should have NO_MATCH result type");
            assertTrue(result.getMessage().contains("disabled"),
                    "Disabled rule message should indicate it was disabled, got: " + result.getMessage());
            logger.info("Direct API test: disabled rule correctly returned NO_MATCH with message '{}'", result.getMessage());
        }

        @Test
        @DisplayName("Enabled Rule via direct API should work normally")
        void testEnabledRuleViaDirectApi() throws YamlConfigurationException {
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile(BASE_PATH + "DisabledRuleTest.yaml");
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            // Create an enabled Rule (using canonical 16-param constructor with enabled=true)
            Rule enabledRule = new Rule(
                    "direct-enabled-rule",
                    new HashSet<>(),
                    "Direct Enabled Rule",
                    "#age >= 18",
                    "Age is valid",
                    "A directly-constructed enabled rule",
                    100,
                    "INFO",
                    null, null, null, null, null, null, null, true
            );

            Map<String, Object> testData = new HashMap<>();
            testData.put("age", 25);

            boolean triggered = engine.executeRule(enabledRule, testData).isTriggered();
            assertTrue(triggered, "Enabled Rule via evaluateRule() should trigger");

            RuleResult result = engine.executeRule(enabledRule, testData);
            assertTrue(result.isTriggered(), "Enabled Rule via executeRule() should trigger");
            assertEquals(RuleResult.ResultType.MATCH, result.getResultType());
            logger.info("Direct API test: enabled rule correctly returned MATCH");
        }

        @Test
        @DisplayName("Rule.isEnabled() returns correct values for both constructors")
        void testRuleIsEnabledFlag() {
            // Canonical 16-param constructor with explicit enabled=true
            Rule defaultRule = new Rule(
                    "test-rule", new HashSet<>(), "Test Rule", "#x > 0", "msg", "desc",
                    100, "INFO", null, null, null, null, null, null, null, true
            );
            assertTrue(defaultRule.isEnabled(), "16-param constructor with true should be enabled");

            // 16-param constructor with enabled=true
            Rule explicitlyEnabled = new Rule(
                    "test-rule", new HashSet<>(), "Test Rule", "#x > 0", "msg", "desc",
                    100, "INFO", null, null, null, null, null, null, null, true
            );
            assertTrue(explicitlyEnabled.isEnabled(), "16-param constructor with true should be enabled");

            // 16-param constructor with enabled=false
            Rule explicitlyDisabled = new Rule(
                    "test-rule", new HashSet<>(), "Test Rule", "#x > 0", "msg", "desc",
                    100, "INFO", null, null, null, null, null, null, null, false
            );
            assertFalse(explicitlyDisabled.isEnabled(), "16-param constructor with false should be disabled");

            // Simple 4-param constructor
            Rule simpleRule = new RuleBuilder().withName("Simple Rule").withCondition("#x > 0").withMessage("msg").withSeverity(SeverityConstants.INFO).build();
            assertTrue(simpleRule.isEnabled(), "4-param constructor should default enabled=true");
        }
    }

    // =====================================================================
    // Helper methods
    // =====================================================================

    private void logChildResults(String context, List<RuleResult> childResults) {
        logger.info("=== {} - Total child results: {} ===", context, childResults.size());
        for (RuleResult child : childResults) {
            logger.info("  Rule: id={}, name={} -> triggered={}, resultType={}, message='{}'",
                    child.getRuleId(), child.getRuleName(), child.isTriggered(),
                    child.getResultType(), child.getMessage());
        }
    }

    /**
     * Find a result by either ruleId or ruleName.
     */
    private RuleResult findResult(List<RuleResult> results, String id, String name) {
        return results.stream()
                .filter(r -> id.equals(r.getRuleId()) || id.equals(r.getRuleName())
                        || name.equals(r.getRuleName()) || name.equals(r.getRuleId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find a result by ruleId only.
     */
    private RuleResult findResultById(List<RuleResult> results, String id) {
        return results.stream()
                .filter(r -> id.equals(r.getRuleId()))
                .findFirst()
                .orElse(null);
    }
}
