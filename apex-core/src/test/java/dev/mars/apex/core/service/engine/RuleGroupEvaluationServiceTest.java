package dev.mars.apex.core.service.engine;

import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.model.Category;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.core.RuleBuilder;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleGroupEvaluationResult;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;
import java.util.Set;

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
 * Tests for {@link RuleGroupEvaluationService} (Phase 2 refactoring).
 * 
 * Validates that rule group evaluation through the service produces equivalent
 * results to the original RuleGroup.evaluate()/evaluateWithDetails() methods,
 * while routing individual rule evaluation through {@link UnifiedRuleEvaluator}.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-02-08
 */
@DisplayName("RuleGroupEvaluationService Tests (Phase 2)")
@ExtendWith(ColoredTestOutputExtension.class)
class RuleGroupEvaluationServiceTest {

    private UnifiedRuleEvaluator unifiedRuleEvaluator;
    private RuleGroupEvaluationService service;

    @BeforeEach
    void setUp() {
        unifiedRuleEvaluator = new UnifiedRuleEvaluator();
        service = new RuleGroupEvaluationService(unifiedRuleEvaluator);
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private StandardEvaluationContext createContext(Map<String, Object> facts) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        if (facts != null) {
            facts.forEach(context::setVariable);
        }
        return context;
    }

    private Rule createRule(String id, String name, String condition, String severity) {
        return new Rule(id, Set.of(new Category("test", 1)),
                name, condition, "Test rule: " + name, "Description", 1, severity,
                null, null, null, null, null, null, null, true);
    }

    private RuleGroup createAndGroup(String id, String name, Rule... rules) {
        RuleGroup group = new RuleGroup(id, "test", name, "Test AND group", 1, true);
        for (int i = 0; i < rules.length; i++) {
            group.addRule(rules[i], i + 1);
        }
        return group;
    }

    private RuleGroup createOrGroup(String id, String name, Rule... rules) {
        RuleGroup group = new RuleGroup(id, "test", name, "Test OR group", 1, false);
        for (int i = 0; i < rules.length; i++) {
            group.addRule(rules[i], i + 1);
        }
        return group;
    }

    // =========================================================================
    // 1. Basic AND Group Evaluation
    // =========================================================================

    @Test
    @DisplayName("AND group: all rules pass -> group passes")
    void testAndGroupAllPass() {
        Rule rule1 = createRule("r1", "check-amount", "#amount > 0", SeverityConstants.INFO);
        Rule rule2 = createRule("r2", "check-currency", "#currency == 'USD'", SeverityConstants.INFO);
        RuleGroup group = createAndGroup("g1", "validation-group", rule1, rule2);

        StandardEvaluationContext context = createContext(Map.of("amount", 100, "currency", "USD"));

        RuleGroupEvaluationResult result = service.evaluateWithDetails(group, context);

        assertTrue(result.isGroupResult(), "AND group should pass when all rules pass");
        assertEquals(2, result.getIndividualResults().size());
        assertTrue(result.getIndividualResults().get(0).isTriggered());
        assertTrue(result.getIndividualResults().get(1).isTriggered());
    }

    @Test
    @DisplayName("AND group: one rule fails -> group fails")
    void testAndGroupOneFails() {
        Rule rule1 = createRule("r1", "check-amount", "#amount > 0", SeverityConstants.INFO);
        Rule rule2 = createRule("r2", "check-currency", "#currency == 'EUR'", SeverityConstants.INFO);
        RuleGroup group = createAndGroup("g1", "validation-group", rule1, rule2);

        StandardEvaluationContext context = createContext(Map.of("amount", 100, "currency", "USD"));

        RuleGroupEvaluationResult result = service.evaluateWithDetails(group, context);

        assertFalse(result.isGroupResult(), "AND group should fail when any rule fails");
    }

    // =========================================================================
    // 2. Basic OR Group Evaluation
    // =========================================================================

    @Test
    @DisplayName("OR group: one rule passes -> group passes")
    void testOrGroupOnePasses() {
        Rule rule1 = createRule("r1", "check-amount", "#amount > 1000", SeverityConstants.INFO);
        Rule rule2 = createRule("r2", "check-vip", "#vip == true", SeverityConstants.INFO);
        RuleGroup group = createOrGroup("g1", "eligibility-group", rule1, rule2);

        StandardEvaluationContext context = createContext(Map.of("amount", 500, "vip", true));

        RuleGroupEvaluationResult result = service.evaluateWithDetails(group, context);

        assertTrue(result.isGroupResult(), "OR group should pass when any rule passes");
    }

    @Test
    @DisplayName("OR group: all rules fail -> group fails")
    void testOrGroupAllFail() {
        Rule rule1 = createRule("r1", "check-amount", "#amount > 1000", SeverityConstants.INFO);
        Rule rule2 = createRule("r2", "check-vip", "#vip == true", SeverityConstants.INFO);
        RuleGroup group = createOrGroup("g1", "eligibility-group", rule1, rule2);

        StandardEvaluationContext context = createContext(Map.of("amount", 500, "vip", false));

        RuleGroupEvaluationResult result = service.evaluateWithDetails(group, context);

        assertFalse(result.isGroupResult(), "OR group should fail when all rules fail");
    }

    // =========================================================================
    // 3. Empty and Disabled Rules
    // =========================================================================

    @Test
    @DisplayName("Empty group returns false")
    void testEmptyGroup() {
        RuleGroup group = new RuleGroup("g1", "test", "empty-group", "Empty", 1, true);

        StandardEvaluationContext context = createContext(Map.of());

        RuleGroupEvaluationResult result = service.evaluateWithDetails(group, context);

        assertFalse(result.isGroupResult());
        assertTrue(result.getIndividualResults().isEmpty());
    }

    @Test
    @DisplayName("Disabled rules are skipped and do not affect group result")
    void testDisabledRulesSkipped() {
        Rule enabled = createRule("r1", "enabled-rule", "#flag == true", SeverityConstants.INFO);
        Rule disabled = createRule("r2", "disabled-rule", "#flag == false", SeverityConstants.INFO);
        // Create a disabled version by using a wrapper approach
        // The simplest way: use the full 16-parameter constructor with enabled=false
        java.util.Set<dev.mars.apex.core.engine.model.Category> cats = 
                java.util.Set.of(new dev.mars.apex.core.engine.model.Category("test", 1));
        Rule disabledRule = new Rule("r2", cats, "disabled-rule", "#flag == false",
                "Disabled", "Description", 1, SeverityConstants.INFO,
                null, null, null, null, null, null, null, false);
        RuleGroup group = createAndGroup("g1", "mixed-group", enabled, disabledRule);

        StandardEvaluationContext context = createContext(Map.of("flag", true));

        RuleGroupEvaluationResult result = service.evaluateWithDetails(group, context);

        assertTrue(result.isGroupResult(), "Group should pass – only enabled rule matters");
        assertEquals(2, result.getIndividualResults().size());
    }

    // =========================================================================
    // 4. Simple boolean evaluate()
    // =========================================================================

    @Test
    @DisplayName("evaluate() returns simple boolean matching evaluateWithDetails()")
    void testEvaluateReturnsBooleanConsistent() {
        Rule rule1 = createRule("r1", "check", "#x > 5", SeverityConstants.INFO);
        RuleGroup group = createAndGroup("g1", "simple", rule1);
        StandardEvaluationContext context = createContext(Map.of("x", 10));

        boolean boolResult = service.evaluate(group, context);
        RuleGroupEvaluationResult detailedResult = service.evaluateWithDetails(group, context);

        assertEquals(boolResult, detailedResult.isGroupResult(),
                "evaluate() and evaluateWithDetails() should give the same boolean result");
        assertTrue(boolResult);
    }

    // =========================================================================
    // 5. Severity Aggregation
    // =========================================================================

    @Test
    @DisplayName("Aggregated severity reflects individual rule severities")
    void testSeverityAggregation() {
        Rule infoRule = createRule("r1", "info-rule", "#x > 0", SeverityConstants.INFO);
        Rule warningRule = createRule("r2", "warning-rule", "#x > 0", SeverityConstants.WARNING);
        RuleGroup group = createAndGroup("g1", "severity-group", infoRule, warningRule);

        StandardEvaluationContext context = createContext(Map.of("x", 10));

        RuleGroupEvaluationResult result = service.evaluateWithDetails(group, context);

        assertTrue(result.isGroupResult());
        assertNotNull(result.getAggregatedSeverity());
    }

    // =========================================================================
    // 6. Service-based evaluation (replaces legacy RuleGroup.evaluate())
    // =========================================================================

    @Test
    @DisplayName("Service evaluation returns true when all AND rules pass")
    void testServiceEvaluateAllPass() {
        Rule rule1 = createRule("r1", "check-a", "#a > 10", SeverityConstants.INFO);
        Rule rule2 = createRule("r2", "check-b", "#b == true", SeverityConstants.WARNING);

        RuleGroup serviceGroup = createAndGroup("g1", "service", rule1, rule2);

        StandardEvaluationContext context = createContext(Map.of("a", 20, "b", true));

        boolean serviceResult = service.evaluate(serviceGroup, context);

        assertTrue(serviceResult, "AND group should pass when all rules pass");
    }

    @Test
    @DisplayName("Service evaluation returns false when an AND rule fails")
    void testServiceEvaluateFailing() {
        Rule rule1 = createRule("r1", "check-a", "#a > 10", SeverityConstants.INFO);
        Rule rule2 = createRule("r2", "check-b", "#b == true", SeverityConstants.WARNING);

        RuleGroup serviceGroup = createAndGroup("g1", "service", rule1, rule2);

        StandardEvaluationContext context = createContext(Map.of("a", 5, "b", true));

        boolean serviceResult = service.evaluate(serviceGroup, context);

        assertFalse(serviceResult, "AND group should fail since a <= 10");
    }

    // =========================================================================
    // 7. Error handling
    // =========================================================================

    @Test
    @DisplayName("Rule with invalid SpEL does not crash the group evaluation")
    void testInvalidSpelHandledGracefully() {
        Rule badRule = createRule("r1", "bad-rule", "this is not valid spel !!!", SeverityConstants.ERROR);
        Rule goodRule = createRule("r2", "good-rule", "#x > 0", SeverityConstants.INFO);
        RuleGroup group = createOrGroup("g1", "error-group", badRule, goodRule);

        StandardEvaluationContext context = createContext(Map.of("x", 10));

        // Through the service, the error path in UnifiedRuleEvaluator should handle
        // the SpEL parse error and produce an ERROR RuleResult instead of throwing
        RuleGroupEvaluationResult result = service.evaluateWithDetails(group, context);

        // OR group: good rule passes, so group passes
        assertTrue(result.isGroupResult(), "OR group should still pass if the good rule passes");
        assertEquals(2, result.getIndividualResults().size());
    }

    // =========================================================================
    // 8. Individual results have ruleId populated (benefit of canonical path)
    // =========================================================================

    @Test
    @DisplayName("Individual results from service have ruleId populated via UnifiedRuleEvaluator")
    void testIndividualResultsHaveRuleId() {
        Rule rule1 = createRule("rule-abc-123", "check-amount", "#amount > 0", SeverityConstants.INFO);
        RuleGroup group = createAndGroup("g1", "id-group", rule1);

        StandardEvaluationContext context = createContext(Map.of("amount", 50));

        RuleGroupEvaluationResult result = service.evaluateWithDetails(group, context);

        assertEquals(1, result.getIndividualResults().size());
        RuleResult individual = result.getIndividualResults().get(0);
        assertEquals("rule-abc-123", individual.getRuleId(),
                "UnifiedRuleEvaluator should populate ruleId — this was missing in legacy RuleGroup evaluation");
    }

    // =========================================================================
    // 9. Constructor validation
    // =========================================================================

    @Test
    @DisplayName("Constructor rejects null UnifiedRuleEvaluator")
    void testConstructorRejectsNull() {
        assertThrows(NullPointerException.class, () -> new RuleGroupEvaluationService(null));
    }
}
