package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.model.condition.SharedConditionGroup;
import dev.mars.apex.core.config.model.condition.SharedConditionRule;
import dev.mars.apex.engine.core.ExpressionEvaluatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

/**
 * Systematic coverage tests for {@link EnrichmentConditionEvaluator}.
 *
 * <p>Covers code paths that integration tests miss because they only exercise
 * the common Map-target + inline-lookup path. Each test names the specific
 * branch it targets.</p>
 *
 * <p>Uses a stub {@link ConditionActionExecutor} to control lookup/function
 * return values without requiring real data sources or enrichment groups.</p>
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("EnrichmentConditionEvaluator — Branch Coverage")
class EnrichmentConditionEvaluatorBranchTest {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentConditionEvaluatorBranchTest.class);

    private ExpressionEvaluatorService evaluatorService;
    private EnrichmentConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluatorService = new ExpressionEvaluatorService();
        evaluator = new EnrichmentConditionEvaluator(
                evaluatorService.getParser(), evaluatorService::createEvaluationContext);
    }

    // ─── evaluateLookupCondition branches ────────────────────────────────

    @Nested
    @DisplayName("evaluateLookupCondition")
    class LookupConditionTests {

        @Test
        @DisplayName("actionExecutor is null → returns false")
        void lookupWithNullExecutor_returnsFalse() {
            // evaluator has no actionExecutor set — the null guard branch
            SharedConditionRule rule = new SharedConditionRule();
            rule.setType("lookup");
            rule.setCondition("#flag == true");

            SharedConditionGroup group = conditionGroup("AND", rule);
            Map<String, Object> data = new HashMap<>();

            boolean result = evaluator.evaluateConditionGroup(group, data, null);
            assertFalse(result, "Lookup condition with null actionExecutor must return false");
            logger.info("PASSED: null actionExecutor guard returns false");
        }

        @Test
        @DisplayName("Lookup returns Map with key matching resultField → extracts scalar (double-nesting fix)")
        void lookupReturnsMapWithMatchingKey_extractsScalar() {
            // The core regression: result-field collides with a key in the lookup result Map
            Map<String, Object> lookupResult = new LinkedHashMap<>();
            lookupResult.put("exists_flag", false);

            evaluator.setActionExecutor(stubLookupExecutor(lookupResult));

            SharedConditionRule rule = lookupRule("exists_flag", "#exists_flag == false");

            SharedConditionGroup group = conditionGroup("AND", rule);
            Map<String, Object> data = new HashMap<>();

            boolean result = evaluator.evaluateConditionGroup(group, data, null);
            assertTrue(result, "Condition must see scalar false, not the Map wrapper");
            assertEquals(false, data.get("exists_flag"),
                    "Stashed value must be the scalar false, not the entire Map");
            logger.info("PASSED: scalar extraction from Map (exists_flag=false)");
        }

        @Test
        @DisplayName("Lookup returns Map with key matching resultField (true case)")
        void lookupReturnsMapWithMatchingKey_trueScalar() {
            Map<String, Object> lookupResult = new LinkedHashMap<>();
            lookupResult.put("exists_flag", true);

            evaluator.setActionExecutor(stubLookupExecutor(lookupResult));

            SharedConditionRule rule = lookupRule("exists_flag", "#exists_flag == true");

            SharedConditionGroup group = conditionGroup("AND", rule);
            Map<String, Object> data = new HashMap<>();

            boolean result = evaluator.evaluateConditionGroup(group, data, null);
            assertTrue(result, "Condition must see scalar true from Map extraction");
            assertEquals(true, data.get("exists_flag"));
            logger.info("PASSED: scalar extraction from Map (exists_flag=true)");
        }

        @Test
        @DisplayName("Lookup returns Map WITHOUT matching key → stashes entire Map")
        void lookupReturnsMapWithoutMatchingKey_stashesWholeMap() {
            Map<String, Object> lookupResult = new LinkedHashMap<>();
            lookupResult.put("code", "USD");
            lookupResult.put("region", "AMER");

            evaluator.setActionExecutor(stubLookupExecutor(lookupResult));

            SharedConditionRule rule = lookupRule("currency_info", "#currency_info['region'] == 'AMER'");

            SharedConditionGroup group = conditionGroup("AND", rule);
            Map<String, Object> data = new HashMap<>();

            boolean result = evaluator.evaluateConditionGroup(group, data, null);
            assertTrue(result, "Condition must work against the full stashed Map");
            assertInstanceOf(Map.class, data.get("currency_info"),
                    "Full Map must be stashed when result-field doesn't match any key");
            logger.info("PASSED: entire Map stashed when key doesn't collide");
        }

        @Test
        @DisplayName("Lookup returns non-Map result → stashes raw value")
        void lookupReturnsNonMap_stashesRawValue() {
            evaluator.setActionExecutor(stubLookupExecutor("ACTIVE"));

            SharedConditionRule rule = lookupRule("status", "#status == 'ACTIVE'");

            SharedConditionGroup group = conditionGroup("AND", rule);
            Map<String, Object> data = new HashMap<>();

            boolean result = evaluator.evaluateConditionGroup(group, data, null);
            assertTrue(result, "Condition must work with raw non-Map result");
            assertEquals("ACTIVE", data.get("status"));
            logger.info("PASSED: non-Map scalar stashed directly");
        }

        @Test
        @DisplayName("Lookup returns null result → condition with null-check evaluates correctly")
        void lookupReturnsNull_conditionSeeNull() {
            evaluator.setActionExecutor(stubLookupExecutor(null));

            SharedConditionRule rule = lookupRule("result", "#result == null");

            SharedConditionGroup group = conditionGroup("AND", rule);
            Map<String, Object> data = new HashMap<>();

            boolean result = evaluator.evaluateConditionGroup(group, data, null);
            assertTrue(result, "Null lookup result must be stashed and visible as null in SpEL");
            logger.info("PASSED: null lookup result stashed and checkable");
        }

        @Test
        @DisplayName("Lookup with null resultField → skips stashing, evaluates condition directly")
        void lookupWithNullResultField_skipsStash() {
            Map<String, Object> lookupResult = new LinkedHashMap<>();
            lookupResult.put("value", 42);

            evaluator.setActionExecutor(stubLookupExecutor(lookupResult));

            SharedConditionRule rule = new SharedConditionRule();
            rule.setType("lookup");
            rule.setResultField(null);  // No result-field — skip stashing
            rule.setCondition("true");  // Always-true gate (no stashed data needed)
            rule.setLookupConfig(minimalLookupConfig());

            SharedConditionGroup group = conditionGroup("AND", rule);
            Map<String, Object> data = new HashMap<>();

            boolean result = evaluator.evaluateConditionGroup(group, data, null);
            assertTrue(result, "Lookup with null result-field should skip stashing and eval condition");
            assertFalse(data.containsKey("value"),
                    "No field should be stashed when result-field is null");
            logger.info("PASSED: null resultField skips stashing");
        }

        @Test
        @DisplayName("Lookup with empty resultField → skips stashing")
        void lookupWithEmptyResultField_skipsStash() {
            evaluator.setActionExecutor(stubLookupExecutor("anything"));

            SharedConditionRule rule = new SharedConditionRule();
            rule.setType("lookup");
            rule.setResultField("   ");  // Blank
            rule.setCondition("true");
            rule.setLookupConfig(minimalLookupConfig());

            SharedConditionGroup group = conditionGroup("AND", rule);
            Map<String, Object> data = new HashMap<>();

            boolean result = evaluator.evaluateConditionGroup(group, data, null);
            assertTrue(result);
            assertTrue(data.isEmpty(), "Nothing should be stashed for blank result-field");
            logger.info("PASSED: blank resultField skips stashing");
        }
    }

    // ─── evaluateFunctionCondition branches ──────────────────────────────

    @Nested
    @DisplayName("evaluateFunctionCondition")
    class FunctionConditionTests {

        @Test
        @DisplayName("actionExecutor is null → returns false")
        void functionWithNullExecutor_returnsFalse() {
            SharedConditionRule rule = new SharedConditionRule();
            rule.setType("function");
            rule.setCondition("#output == 'HIGH'");

            SharedConditionGroup group = conditionGroup("AND", rule);
            Map<String, Object> data = new HashMap<>();

            boolean result = evaluator.evaluateConditionGroup(group, data, null);
            assertFalse(result, "Function condition with null actionExecutor must return false");
            logger.info("PASSED: null actionExecutor guard (function) returns false");
        }

        @Test
        @DisplayName("Function result stashed via outputField → SpEL condition works")
        void functionResultStashed_conditionEvaluates() {
            evaluator.setActionExecutor(stubFunctionExecutor("HIGH"));

            SharedConditionRule rule = new SharedConditionRule();
            rule.setType("function");
            rule.setOutputField("risk_level");
            rule.setCondition("#risk_level == 'HIGH'");
            rule.setEnrichmentGroupRef("fake-group");

            SharedConditionGroup group = conditionGroup("AND", rule);
            Map<String, Object> data = new HashMap<>();

            boolean result = evaluator.evaluateConditionGroup(group, data, null);
            assertTrue(result, "Function output stashed and condition should match");
            assertEquals("HIGH", data.get("risk_level"));
            logger.info("PASSED: function output stashed and evaluated");
        }

        @Test
        @DisplayName("Function with null outputField → skips stashing")
        void functionWithNullOutputField_skipsStash() {
            evaluator.setActionExecutor(stubFunctionExecutor("MEDIUM"));

            SharedConditionRule rule = new SharedConditionRule();
            rule.setType("function");
            rule.setOutputField(null);  // No output-field
            rule.setCondition("true");
            rule.setEnrichmentGroupRef("fake-group");

            SharedConditionGroup group = conditionGroup("AND", rule);
            Map<String, Object> data = new HashMap<>();

            boolean result = evaluator.evaluateConditionGroup(group, data, null);
            assertTrue(result, "Function with null output-field should skip stashing and eval condition");
            assertTrue(data.isEmpty(), "No field should be stashed when output-field is null");
            logger.info("PASSED: null outputField skips stashing");
        }
    }

    // ─── evaluateSpEL edge cases ─────────────────────────────────────────

    @Nested
    @DisplayName("evaluateSpEL edge cases")
    class SpELEdgeCaseTests {

        @Test
        @DisplayName("SpEL that returns null → false")
        void spelReturnsNull_isFalse() {
            SharedConditionRule rule = new SharedConditionRule();
            rule.setCondition("#nonExistentVar");

            SharedConditionGroup group = conditionGroup("AND", rule);
            Map<String, Object> data = new HashMap<>();

            // #nonExistentVar will be null → should return false
            boolean result = evaluator.evaluateConditionGroup(group, data, null);
            assertFalse(result, "SpEL returning null must evaluate to false");
            logger.info("PASSED: null SpEL result → false");
        }

        @Test
        @DisplayName("SpEL that returns non-boolean non-null → true")
        void spelReturnsNonBooleanNonNull_isTrue() {
            SharedConditionRule rule = new SharedConditionRule();
            rule.setCondition("42");

            SharedConditionGroup group = conditionGroup("AND", rule);
            Map<String, Object> data = new HashMap<>();

            boolean result = evaluator.evaluateConditionGroup(group, data, null);
            assertTrue(result, "Non-null non-boolean SpEL result must evaluate to true");
            logger.info("PASSED: non-boolean non-null → true");
        }
    }

    // ─── evaluateMappingRuleConditions ────────────────────────────────────

    @Nested
    @DisplayName("evaluateMappingRuleConditions")
    class MappingRuleConditionTests {

        @Test
        @DisplayName("MappingRule with null conditions → returns true (default/catch-all rule)")
        void mappingRuleWithNullConditions_returnsTrue() {
            YamlEnrichment.MappingRule rule = new YamlEnrichment.MappingRule();
            rule.setId("fallback-rule");
            // conditions not set → null

            boolean result = evaluator.evaluateMappingRuleConditions(rule, new HashMap<>());
            assertTrue(result, "Mapping rule with no conditions must return true (catch-all)");
            logger.info("PASSED: null conditions → true (catch-all)");
        }

        @Test
        @DisplayName("MappingRule delegates to evaluateConditionGroup")
        void mappingRuleDelegatesToConditionGroup() {
            YamlEnrichment.MappingRule rule = new YamlEnrichment.MappingRule();
            rule.setId("test-rule");

            SharedConditionGroup conditions = new SharedConditionGroup();
            conditions.setOperator("AND");
            SharedConditionRule cond = new SharedConditionRule();
            cond.setCondition("#amount > 100");
            conditions.setRules(List.of(cond));
            rule.setConditions(conditions);

            Map<String, Object> data = new HashMap<>();
            data.put("amount", 200);
            assertTrue(evaluator.evaluateMappingRuleConditions(rule, data),
                    "Conditions met → true");

            data.put("amount", 50);
            assertFalse(evaluator.evaluateMappingRuleConditions(rule, data),
                    "Conditions not met → false");

            logger.info("PASSED: mapping rule delegates to condition group correctly");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private static SharedConditionGroup conditionGroup(String operator,
                                                                 SharedConditionRule... rules) {
        SharedConditionGroup group = new SharedConditionGroup();
        group.setOperator(operator);
        group.setRules(List.of(rules));
        return group;
    }

    private static SharedConditionRule lookupRule(String resultField, String condition) {
        SharedConditionRule rule = new SharedConditionRule();
        rule.setType("lookup");
        rule.setResultField(resultField);
        rule.setCondition(condition);
        rule.setLookupConfig(minimalLookupConfig());
        return rule;
    }

    private static YamlEnrichment.LookupConfig minimalLookupConfig() {
        YamlEnrichment.LookupConfig config = new YamlEnrichment.LookupConfig();
        config.setLookupKey("'dummy'");
        return config;
    }

    /**
     * Stub that returns a fixed value from executeLookup, ignoring the actual lookup config.
     */
    private static ConditionActionExecutor stubLookupExecutor(Object fixedResult) {
        return new ConditionActionExecutor(null, null, null, null) {
            @Override
            public Object executeLookup(SharedConditionRule rule, Object targetObject,
                                        YamlRuleConfiguration config) {
                return fixedResult;
            }
        };
    }

    /**
     * Stub that returns a fixed value from executeFunction.
     */
    private static ConditionActionExecutor stubFunctionExecutor(Object fixedResult) {
        return new ConditionActionExecutor(null, null, null, null) {
            @Override
            public Object executeFunction(SharedConditionRule rule, Object targetObject,
                                          YamlRuleConfiguration config) {
                return fixedResult;
            }
        };
    }
}
