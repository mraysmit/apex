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
package dev.mars.apex.core.engine.config.execution;

import dev.mars.apex.core.config.yaml.YamlRuleChain;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.engine.UnifiedRuleEvaluator;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 6 Tests: Verifies that RuleChainExecutor routes SpEL evaluation through
 * ExpressionEvaluatorService and UnifiedRuleEvaluator instead of raw parser calls.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Trigger-rule evaluation routes through UnifiedRuleEvaluator (conditional-chaining)</li>
 *   <li>Router-rule evaluation routes through ExpressionEvaluatorService (result-based-routing)</li>
 *   <li>Result-field storage by UnifiedRuleEvaluator (no double storage)</li>
 *   <li>Conditional rules on trigger/no-trigger paths</li>
 *   <li>Disabled chain handling</li>
 *   <li>Error handling for invalid expressions</li>
 * </ul>
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Phase 6: RuleChainExecutor SpEL Routing")
class RuleChainExecutorSpELRoutingTest {

    private ExpressionEvaluatorService evaluatorService;
    private UnifiedRuleEvaluator unifiedEvaluator;
    private RuleChainExecutor executor;
    private java.util.function.Function<Map<String, Object>, StandardEvaluationContext> contextFactory;

    @BeforeEach
    void setUp() {
        evaluatorService = new ExpressionEvaluatorService();
        unifiedEvaluator = new UnifiedRuleEvaluator();
        // EnrichmentGroupExecutor not needed for these tests (no enrichment groups used)
        executor = new RuleChainExecutor(evaluatorService, unifiedEvaluator, null);

        // Context factory: creates a StandardEvaluationContext with data variables
        contextFactory = data -> {
            StandardEvaluationContext ctx = new StandardEvaluationContext();
            data.forEach(ctx::setVariable);
            return ctx;
        };
    }

    // ================================
    // Helper methods
    // ================================

    private YamlRuleChain createConditionalChainingChain(String id, String triggerCondition,
                                                         String triggerMessage, String resultField) {
        YamlRuleChain chain = new YamlRuleChain();
        chain.setId(id);
        chain.setName("Test Chain " + id);
        chain.setPattern("conditional-chaining");

        Map<String, Object> triggerRule = new HashMap<>();
        triggerRule.put("condition", triggerCondition);
        if (triggerMessage != null) triggerRule.put("message", triggerMessage);
        if (resultField != null) triggerRule.put("result-field", resultField);

        Map<String, Object> config = new HashMap<>();
        config.put("trigger-rule", triggerRule);
        chain.setConfiguration(config);
        return chain;
    }

    private YamlRuleChain createResultBasedRoutingChain(String id, String routerCondition,
                                                        String resultField,
                                                        Map<String, List<Map<String, Object>>> routes) {
        YamlRuleChain chain = new YamlRuleChain();
        chain.setId(id);
        chain.setName("Router Chain " + id);
        chain.setPattern("result-based-routing");

        Map<String, Object> routerRule = new HashMap<>();
        routerRule.put("condition", routerCondition);
        if (resultField != null) routerRule.put("result-field", resultField);

        Map<String, Object> routesConfig = new HashMap<>();
        if (routes != null) {
            routes.forEach((key, rules) -> {
                Map<String, Object> routeEntry = new HashMap<>();
                routeEntry.put("rules", rules);
                routesConfig.put(key, routeEntry);
            });
        }

        Map<String, Object> config = new HashMap<>();
        config.put("router-rule", routerRule);
        config.put("routes", routesConfig);
        chain.setConfiguration(config);
        return chain;
    }

    private YamlRuleConfiguration wrapInConfig(YamlRuleChain chain) {
        YamlRuleConfiguration config = new YamlRuleConfiguration();
        config.setRuleChains(Collections.singletonList(chain));
        return config;
    }

    private Map<String, Object> createRuleConfig(String id, String condition, String message) {
        Map<String, Object> rule = new HashMap<>();
        rule.put("id", id);
        rule.put("condition", condition);
        rule.put("message", message);
        return rule;
    }

    // ================================
    // Conditional-Chaining Pattern Tests
    // ================================

    @Nested
    @DisplayName("Conditional-Chaining Pattern")
    class ConditionalChainingTests {

        @Test
        @DisplayName("Should evaluate trigger condition to TRUE via UnifiedRuleEvaluator")
        void triggerConditionTrue() {
            YamlRuleChain chain = createConditionalChainingChain(
                    "chain-1", "#amount > 100", "High amount detected", null);
            YamlRuleConfiguration yamlConfig = wrapInConfig(chain);

            Map<String, Object> data = new HashMap<>();
            data.put("amount", 500);

            RuleResult result = executor.processRuleChain("chain-1", yamlConfig, data, contextFactory);

            assertNotNull(result, "Result should not be null");
            assertTrue(result.isTriggered(), "Trigger condition should be TRUE for amount=500 > 100");
            assertEquals("chain-1", result.getRuleName());
        }

        @Test
        @DisplayName("Should evaluate trigger condition to FALSE via UnifiedRuleEvaluator")
        void triggerConditionFalse() {
            YamlRuleChain chain = createConditionalChainingChain(
                    "chain-2", "#amount > 100", "High amount", null);
            YamlRuleConfiguration yamlConfig = wrapInConfig(chain);

            Map<String, Object> data = new HashMap<>();
            data.put("amount", 50);

            RuleResult result = executor.processRuleChain("chain-2", yamlConfig, data, contextFactory);

            assertNotNull(result, "Result should not be null");
            assertFalse(result.isTriggered(), "Trigger condition should be FALSE for amount=50 <= 100");
        }

        @Test
        @DisplayName("Should store result-field via UnifiedRuleEvaluator (no double storage)")
        void resultFieldStoredByEvaluator() {
            YamlRuleChain chain = createConditionalChainingChain(
                    "chain-3", "#amount > 100", "High amount", "triggerResult");
            YamlRuleConfiguration yamlConfig = wrapInConfig(chain);

            Map<String, Object> data = new HashMap<>();
            data.put("amount", 500);

            executor.processRuleChain("chain-3", yamlConfig, data, contextFactory);

            // UnifiedRuleEvaluator stores Boolean result via resultField
            assertTrue(data.containsKey("triggerResult"), "Result-field should be stored in data");
            assertEquals(true, data.get("triggerResult"),
                    "Result-field value should be TRUE (stored by UnifiedRuleEvaluator)");
        }

        @Test
        @DisplayName("Should execute on-trigger conditional rules when trigger is TRUE")
        void executesOnTriggerPath() {
            YamlRuleChain chain = createConditionalChainingChain(
                    "chain-4", "#amount > 100", "Triggered", "triggerFlag");

            // Add conditional-rules with on-trigger path
            Map<String, Object> onTriggerRule = createRuleConfig(
                    "on-trigger-rule", "#amount > 200", "Very high amount");
            onTriggerRule.put("result-field", "veryHigh");

            Map<String, Object> conditionalRules = new HashMap<>();
            conditionalRules.put("on-trigger", Collections.singletonList(onTriggerRule));

            chain.getConfiguration().put("conditional-rules", conditionalRules);
            YamlRuleConfiguration yamlConfig = wrapInConfig(chain);

            Map<String, Object> data = new HashMap<>();
            data.put("amount", 500);

            RuleResult result = executor.processRuleChain("chain-4", yamlConfig, data, contextFactory);

            assertTrue(result.isTriggered(), "Chain should be triggered");
            // The on-trigger conditional rule should also have been evaluated
            assertTrue(data.containsKey("veryHigh"), "On-trigger path rule result-field should be stored");
            assertEquals(true, data.get("veryHigh"),
                    "VeryHigh result should be TRUE for amount=500 > 200");
        }

        @Test
        @DisplayName("Should execute on-no-trigger path when trigger is FALSE")
        void executesOnNoTriggerPath() {
            YamlRuleChain chain = createConditionalChainingChain(
                    "chain-5", "#amount > 1000", "Over threshold", null);

            Map<String, Object> noTriggerRule = createRuleConfig(
                    "low-amount-rule", "#amount < 100", "Low amount detected");
            noTriggerRule.put("result-field", "isLow");

            Map<String, Object> conditionalRules = new HashMap<>();
            conditionalRules.put("on-no-trigger", Collections.singletonList(noTriggerRule));

            chain.getConfiguration().put("conditional-rules", conditionalRules);
            YamlRuleConfiguration yamlConfig = wrapInConfig(chain);

            Map<String, Object> data = new HashMap<>();
            data.put("amount", 50);

            RuleResult result = executor.processRuleChain("chain-5", yamlConfig, data, contextFactory);

            assertFalse(result.isTriggered(), "Chain should NOT be triggered");
            assertTrue(data.containsKey("isLow"), "On-no-trigger path rule result-field should be stored");
            assertEquals(true, data.get("isLow"),
                    "isLow should be TRUE for amount=50 < 100");
        }

        @Test
        @DisplayName("Should handle invalid trigger expression gracefully")
        void invalidTriggerExpression() {
            YamlRuleChain chain = createConditionalChainingChain(
                    "chain-err", "#nonExistentVar.method()", "Bad expression", null);
            YamlRuleConfiguration yamlConfig = wrapInConfig(chain);

            Map<String, Object> data = new HashMap<>();
            data.put("amount", 100);

            // Should not throw — error is captured in RuleResult
            RuleResult result = executor.processRuleChain("chain-err", yamlConfig, data, contextFactory);
            assertNotNull(result, "Result should not be null even for errors");
        }

        @Test
        @DisplayName("Should return error for missing trigger-rule config")
        void missingTriggerRuleConfig() {
            YamlRuleChain chain = new YamlRuleChain();
            chain.setId("chain-no-trigger");
            chain.setName("No Trigger");
            chain.setPattern("conditional-chaining");
            chain.setConfiguration(new HashMap<>());  // No trigger-rule

            YamlRuleConfiguration yamlConfig = wrapInConfig(chain);
            Map<String, Object> data = new HashMap<>();

            RuleResult result = executor.processRuleChain("chain-no-trigger", yamlConfig, data, contextFactory);

            assertNotNull(result);
            assertEquals("ERROR", result.getResultType().name(),
                    "Missing trigger-rule should produce ERROR result");
        }
    }

    // ================================
    // Result-Based-Routing Pattern Tests
    // ================================

    @Nested
    @DisplayName("Result-Based-Routing Pattern")
    class ResultBasedRoutingTests {

        @Test
        @DisplayName("Should evaluate router condition via ExpressionEvaluatorService")
        void routerConditionEvaluated() {
            Map<String, List<Map<String, Object>>> routes = new HashMap<>();
            routes.put("HIGH", Collections.singletonList(
                    createRuleConfig("high-rule", "#amount > 500", "Very high")));
            routes.put("LOW", Collections.singletonList(
                    createRuleConfig("low-rule", "#amount < 100", "Low amount")));

            YamlRuleChain chain = createResultBasedRoutingChain(
                    "router-1", "#amount > 300 ? 'HIGH' : 'LOW'", "routeKey", routes);
            YamlRuleConfiguration yamlConfig = wrapInConfig(chain);

            Map<String, Object> data = new HashMap<>();
            data.put("amount", 500);

            RuleResult result = executor.processRuleChain("router-1", yamlConfig, data, contextFactory);

            assertNotNull(result, "Result should not be null");
            // Router stores the route key
            assertEquals("HIGH", data.get("routeKey"),
                    "Route key should be HIGH for amount=500 > 300");
        }

        @Test
        @DisplayName("Should route to LOW path and execute rules")
        void routesToLowPath() {
            Map<String, List<Map<String, Object>>> routes = new HashMap<>();
            Map<String, Object> lowRule = createRuleConfig("low-check", "#amount < 50", "Below minimum");
            lowRule.put("result-field", "belowMin");
            routes.put("LOW", Collections.singletonList(lowRule));

            YamlRuleChain chain = createResultBasedRoutingChain(
                    "router-2", "#amount > 300 ? 'HIGH' : 'LOW'", null, routes);
            YamlRuleConfiguration yamlConfig = wrapInConfig(chain);

            Map<String, Object> data = new HashMap<>();
            data.put("amount", 25);

            executor.processRuleChain("router-2", yamlConfig, data, contextFactory);

            assertTrue(data.containsKey("belowMin"),
                    "LOW route rule result-field should be stored");
            assertEquals(true, data.get("belowMin"),
                    "belowMin should be TRUE for amount=25 < 50");
        }

        @Test
        @DisplayName("Should handle missing route gracefully")
        void missingRouteHandled() {
            Map<String, List<Map<String, Object>>> routes = new HashMap<>();
            routes.put("HIGH", Collections.singletonList(
                    createRuleConfig("high-rule", "true", "High path")));

            YamlRuleChain chain = createResultBasedRoutingChain(
                    "router-3", "'UNKNOWN'", null, routes);
            YamlRuleConfiguration yamlConfig = wrapInConfig(chain);

            Map<String, Object> data = new HashMap<>();

            // Route key 'UNKNOWN' has no matching route — should return no-match
            RuleResult result = executor.processRuleChain("router-3", yamlConfig, data, contextFactory);

            assertNotNull(result, "Should handle missing route without NPE");
        }

        @Test
        @DisplayName("Should handle invalid router expression gracefully")
        void invalidRouterExpression() {
            YamlRuleChain chain = createResultBasedRoutingChain(
                    "router-err", "#badVar.bad()", null, new HashMap<>());
            YamlRuleConfiguration yamlConfig = wrapInConfig(chain);

            Map<String, Object> data = new HashMap<>();

            RuleResult result = executor.processRuleChain("router-err", yamlConfig, data, contextFactory);

            assertNotNull(result, "Should handle invalid router expression without NPE");
            // ExpressionEvaluatorService returns null for errors, which produces route "null" → no match
            assertFalse(result.isTriggered(),
                    "Invalid router expression should not trigger");
        }
    }

    // ================================
    // General Tests
    // ================================

    @Nested
    @DisplayName("General Chain Handling")
    class GeneralTests {

        @Test
        @DisplayName("Should skip disabled chain and return no-match")
        void disabledChainSkipped() {
            YamlRuleChain chain = createConditionalChainingChain(
                    "disabled-chain", "#amount > 0", "Test", null);
            chain.setEnabled(false);
            YamlRuleConfiguration yamlConfig = wrapInConfig(chain);

            Map<String, Object> data = new HashMap<>();
            data.put("amount", 100);

            RuleResult result = executor.processRuleChain("disabled-chain", yamlConfig, data, contextFactory);

            assertNotNull(result);
            assertFalse(result.isTriggered(), "Disabled chain should not trigger");
        }

        @Test
        @DisplayName("Should return error for non-existent chain ID")
        void nonExistentChain() {
            YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();

            RuleResult result = executor.processRuleChain("no-such-chain", yamlConfig,
                    new HashMap<>(), contextFactory);

            assertNotNull(result);
            assertEquals("ERROR", result.getResultType().name(),
                    "Non-existent chain should produce ERROR");
        }

        @Test
        @DisplayName("Should return no-match for unsupported pattern")
        void unsupportedPattern() {
            YamlRuleChain chain = new YamlRuleChain();
            chain.setId("unknown-pattern");
            chain.setName("Unknown");
            chain.setPattern("unknown-pattern-type");
            chain.setConfiguration(new HashMap<>());
            YamlRuleConfiguration yamlConfig = wrapInConfig(chain);

            RuleResult result = executor.processRuleChain("unknown-pattern", yamlConfig,
                    new HashMap<>(), contextFactory);

            assertNotNull(result);
            assertFalse(result.isTriggered());
        }
    }
}
