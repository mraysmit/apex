package dev.mars.apex.demo.advanced;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleBase;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.RuleConfigurationService;
import dev.mars.apex.core.service.engine.RuleEngineService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.HashMap;
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
 * Test class for rule engine functionality.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for rule engine functionality.
 * This class tests rule engine functionality, including rule groups, without depending on demo classes.
 */
public class RuleEngineTest {

    private RulesEngine rulesEngine;
    private RulesEngineConfiguration config;
    private RuleConfigurationService ruleConfigService;
    private RuleEngineService ruleEngineService;

    @BeforeEach
    public void setUp() {
        // Initialize configuration and rules engine
        config = new RulesEngineConfiguration();
        rulesEngine = new RulesEngine(config);
        ruleConfigService = new RuleConfigurationService(config);
        ruleEngineService = new RuleEngineService(new ExpressionEvaluatorService());
    }

    /**
     * Test simple rule evaluation.
     */
    @Test
    public void testSimpleRuleEvaluation() {
        // Create a simple rule
        Rule rule = new Rule(
            "AgeRule",
            "#age >= 18",
            "Person is an adult"
        );

        // Create facts for rule evaluation
        Map<String, Object> adultFacts = new HashMap<>();
        adultFacts.put("age", 35);

        Map<String, Object> minorFacts = new HashMap<>();
        minorFacts.put("age", 17);

        // Evaluate rule for adult
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);
        RuleResult adultResult = rulesEngine.executeRulesList(rules, adultFacts);

        // Verify adult result
        assertTrue(adultResult.isTriggered(), "Rule should be triggered for adult");
        assertEquals("AgeRule", adultResult.getRuleName(), "Rule name should match");
        assertEquals("Person is an adult", adultResult.getMessage(), "Rule message should match");

        // Evaluate rule for minor
        RuleResult minorResult = rulesEngine.executeRulesList(rules, minorFacts);

        // Verify minor result
        assertFalse(minorResult.isTriggered(), "Rule should not be triggered for minor");
    }

    /**
     * Test rule group with AND operator.
     */
    @Test
    public void testRuleGroupWithAnd() {
        // Create rules for the group
        Rule ageRule = new Rule(
            "AgeRule",
            "#age >= 18",
            "Person is an adult"
        );

        Rule incomeRule = new Rule(
            "IncomeRule",
            "#income >= 50000",
            "Person has sufficient income"
        );

        // Create a rule group with AND operator
        RuleGroup ruleGroup = new RuleGroup(
            "group-001",
            "test-category",
            "AdultWithSufficientIncome",
            "Person is an adult with sufficient income",
            5,
            true // AND operator
        );

        // Add rules to the group
        ruleGroup.addRule(ageRule, 1);
        ruleGroup.addRule(incomeRule, 2);

        // Create facts for rule evaluation
        Map<String, Object> adultWithSufficientIncome = new HashMap<>();
        adultWithSufficientIncome.put("age", 35);
        adultWithSufficientIncome.put("income", 60000);

        Map<String, Object> adultWithInsufficientIncome = new HashMap<>();
        adultWithInsufficientIncome.put("age", 35);
        adultWithInsufficientIncome.put("income", 40000);

        Map<String, Object> minorWithSufficientIncome = new HashMap<>();
        minorWithSufficientIncome.put("age", 17);
        minorWithSufficientIncome.put("income", 60000);

        // Evaluate rule group for adult with sufficient income
        List<RuleGroup> ruleGroups = new ArrayList<>();
        ruleGroups.add(ruleGroup);
        RuleResult result1 = rulesEngine.executeRuleGroupsList(ruleGroups, adultWithSufficientIncome);

        // Verify result
        assertTrue(result1.isTriggered(), "Rule group should be triggered for adult with sufficient income");
        assertEquals("AdultWithSufficientIncome", result1.getRuleName(), "Rule name should match");

        // Evaluate rule group for adult with insufficient income
        RuleResult result2 = rulesEngine.executeRuleGroupsList(ruleGroups, adultWithInsufficientIncome);

        // Verify result
        assertFalse(result2.isTriggered(), "Rule group should not be triggered for adult with insufficient income");

        // Evaluate rule group for minor with sufficient income
        RuleResult result3 = rulesEngine.executeRuleGroupsList(ruleGroups, minorWithSufficientIncome);

        // Verify result
        assertFalse(result3.isTriggered(), "Rule group should not be triggered for minor with sufficient income");
    }

    /**
     * Test rule group with OR operator.
     */
    @Test
    public void testRuleGroupWithOr() {
        // Create rules for the group
        Rule ageRule = new Rule(
            "AgeRule",
            "#age >= 65",
            "Person is a senior"
        );

        Rule incomeRule = new Rule(
            "IncomeRule",
            "#income < 30000",
            "Person has low income"
        );

        // Create a rule group with OR operator
        RuleGroup ruleGroup = new RuleGroup(
            "group-002",
            "test-category",
            "SeniorOrLowIncome",
            "Person is a senior or has low income",
            5,
            false // OR operator
        );

        // Add rules to the group
        ruleGroup.addRule(ageRule, 1);
        ruleGroup.addRule(incomeRule, 2);

        // Create facts for rule evaluation
        Map<String, Object> seniorWithHighIncome = new HashMap<>();
        seniorWithHighIncome.put("age", 70);
        seniorWithHighIncome.put("income", 60000);

        Map<String, Object> youngWithLowIncome = new HashMap<>();
        youngWithLowIncome.put("age", 25);
        youngWithLowIncome.put("income", 25000);

        Map<String, Object> youngWithHighIncome = new HashMap<>();
        youngWithHighIncome.put("age", 25);
        youngWithHighIncome.put("income", 60000);

        // Evaluate rule group for senior with high income
        List<RuleGroup> ruleGroups = new ArrayList<>();
        ruleGroups.add(ruleGroup);
        RuleResult result1 = rulesEngine.executeRuleGroupsList(ruleGroups, seniorWithHighIncome);

        // Verify result
        assertTrue(result1.isTriggered(), "Rule group should be triggered for senior with high income");
        assertEquals("SeniorOrLowIncome", result1.getRuleName(), "Rule name should match");

        // Evaluate rule group for young with low income
        RuleResult result2 = rulesEngine.executeRuleGroupsList(ruleGroups, youngWithLowIncome);

        // Verify result
        assertTrue(result2.isTriggered(), "Rule group should be triggered for young with low income");

        // Evaluate rule group for young with high income
        RuleResult result3 = rulesEngine.executeRuleGroupsList(ruleGroups, youngWithHighIncome);

        // Verify result
        assertFalse(result3.isTriggered(), "Rule group should not be triggered for young with high income");
    }

    /**
     * Test rule configuration service.
     */
    @Test
    public void testRuleConfigurationService() {
        // Register rules using the rule configuration service
        Rule rule1 = ruleConfigService.registerRule(
            "R001",
            "test-category",
            "AgeRule",
            "#age >= 18",
            "Person is an adult",
            "Identifies adults",
            10
        );

        Rule rule2 = ruleConfigService.registerRule(
            "R002",
            "test-category",
            "IncomeRule",
            "#income >= 50000",
            "Person has sufficient income",
            "Identifies people with sufficient income",
            20
        );

        // Verify rules were registered
        assertNotNull(rule1, "Rule 1 should not be null");
        assertNotNull(rule2, "Rule 2 should not be null");

        // Get rules by ID
        Rule retrievedRule1 = config.getRuleById("R001");
        Rule retrievedRule2 = config.getRuleById("R002");

        // Verify retrieved rules
        assertNotNull(retrievedRule1, "Retrieved rule 1 should not be null");
        assertEquals("AgeRule", retrievedRule1.getName(), "Rule 1 name should match");

        assertNotNull(retrievedRule2, "Retrieved rule 2 should not be null");
        assertEquals("IncomeRule", retrievedRule2.getName(), "Rule 2 name should match");

        // Get rules by category
        List<RuleBase> rulesByCategory = config.getRulesForCategory("test-category");

        // Verify rules by category
        assertEquals(2, rulesByCategory.size(), "Should find 2 rules in the category");
    }

    /**
     * Test rule engine service.
     */
    @Test
    public void testRuleEngineService() {
        // Create rules
        Rule rule1 = new Rule(
            "AgeRule",
            "#age >= 18",
            "Person is an adult"
        );

        Rule rule2 = new Rule(
            "IncomeRule",
            "#income >= 50000",
            "Person has sufficient income"
        );

        List<Rule> rules = new ArrayList<>();
        rules.add(rule1);
        rules.add(rule2);

        // Create facts for rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("age", 35);
        facts.put("income", 60000);

        // Create evaluation context from facts
        StandardEvaluationContext context = new StandardEvaluationContext();
        for (Map.Entry<String, Object> entry : facts.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        // Evaluate rules using the rule engine service
        List<RuleResult> results = ruleEngineService.evaluateRules(rules, context);

        // Verify results
        assertEquals(2, results.size(), "Should have 2 results");
        assertTrue(results.get(0).isTriggered(), "Rule 1 should be triggered");
        assertTrue(results.get(1).isTriggered(), "Rule 2 should be triggered");
    }
}
