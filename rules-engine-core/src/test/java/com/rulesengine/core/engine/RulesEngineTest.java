package com.rulesengine.core.engine;

import com.rulesengine.core.engine.config.RuleBuilder;
import com.rulesengine.core.engine.config.RuleGroupBuilder;
import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.config.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleBase;
import com.rulesengine.core.engine.model.RuleGroup;
import com.rulesengine.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RulesEngine.
 */
public class RulesEngineTest {
    private RulesEngine rulesEngine;
    private RulesEngineConfiguration configuration;
    private Map<String, Object> facts;

    @BeforeEach
    public void setUp() {
        configuration = new RulesEngineConfiguration();
        rulesEngine = new RulesEngine(configuration);

        // Set up some facts for testing
        facts = new HashMap<>();
        facts.put("age", 30);
        facts.put("income", 50000);
        facts.put("creditScore", 700);
        facts.put("isEmployed", true);
    }

    @Test
    public void testExecuteRulesListWithNoRules() {
        // Test executing an empty list of rules
        RuleResult result = rulesEngine.executeRulesList(new ArrayList<>(), facts);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isTriggered());
        assertEquals("no-rule", result.getRuleName());
        assertEquals("No rules provided", result.getMessage());
    }

    @Test
    public void testExecuteRulesListWithNullRules() {
        // Test executing a null list of rules
        RuleResult result = rulesEngine.executeRulesList(null, facts);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isTriggered());
        assertEquals("no-rule", result.getRuleName());
        assertEquals("No rules provided", result.getMessage());
    }

    @Test
    public void testExecuteRulesListWithMatchingRule() {
        // Create a rule that will match the facts
        Rule rule = new RuleBuilder()
            .withName("Age Rule")
            .withCondition("#age > 18")
            .withMessage("Person is an adult")
            .build();

        List<Rule> rules = new ArrayList<>();
        rules.add(rule);

        // Execute the rules
        RuleResult result = rulesEngine.executeRulesList(rules, facts);

        // Verify the result
        assertNotNull(result);
        assertTrue(result.isTriggered());
        assertEquals("Age Rule", result.getRuleName());
        assertEquals("Person is an adult", result.getMessage());
    }

    @Test
    public void testExecuteRulesListWithNonMatchingRule() {
        // Create a rule that will not match the facts
        Rule rule = new RuleBuilder()
            .withName("Age Rule")
            .withCondition("#age < 18")
            .withMessage("Person is a minor")
            .build();

        List<Rule> rules = new ArrayList<>();
        rules.add(rule);

        // Execute the rules
        RuleResult result = rulesEngine.executeRulesList(rules, facts);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isTriggered());
        assertEquals("no-match", result.getRuleName());
        assertEquals("No matching rules found", result.getMessage());
    }

    @Test
    public void testExecuteRulesListWithMultipleRules() {
        // Create multiple rules
        Rule rule1 = new RuleBuilder()
            .withName("Age Rule")
            .withCondition("#age < 18")
            .withMessage("Person is a minor")
            .build();

        Rule rule2 = new RuleBuilder()
            .withName("Income Rule")
            .withCondition("#income > 40000")
            .withMessage("Person has good income")
            .build();

        List<Rule> rules = new ArrayList<>();
        rules.add(rule1);
        rules.add(rule2);

        // Execute the rules
        RuleResult result = rulesEngine.executeRulesList(rules, facts);

        // Verify the result
        assertNotNull(result);
        assertTrue(result.isTriggered());
        assertEquals("Income Rule", result.getRuleName());
        assertEquals("Person has good income", result.getMessage());
    }

    @Test
    public void testExecuteRuleGroupsListWithNoRuleGroups() {
        // Test executing an empty list of rule groups
        RuleResult result = rulesEngine.executeRuleGroupsList(new ArrayList<>(), facts);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isTriggered());
        assertEquals("no-rule", result.getRuleName());
        assertEquals("No rules provided", result.getMessage());
    }

    @Test
    public void testExecuteRuleGroupsListWithNullRuleGroups() {
        // Test executing a null list of rule groups
        RuleResult result = rulesEngine.executeRuleGroupsList(null, facts);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isTriggered());
        assertEquals("no-rule", result.getRuleName());
        assertEquals("No rules provided", result.getMessage());
    }

    @Test
    public void testExecuteRuleGroupsListWithMatchingRuleGroup() {
        // Create a rule group with AND operator
        RuleGroup ruleGroup = new RuleGroupBuilder()
            .withName("Credit Check")
            .withDescription("Check if person has good credit")
            .withAndOperator()
            .build();

        // Create rules for the rule group
        Rule rule1 = new RuleBuilder()
            .withName("Credit Score Rule")
            .withCondition("#creditScore >= 700")
            .withMessage("Person has good credit score")
            .build();

        Rule rule2 = new RuleBuilder()
            .withName("Employment Rule")
            .withCondition("#isEmployed == true")
            .withMessage("Person is employed")
            .build();

        // Add rules to the rule group
        ruleGroup.addRule(rule1, 1);
        ruleGroup.addRule(rule2, 2);

        List<RuleGroup> ruleGroups = new ArrayList<>();
        ruleGroups.add(ruleGroup);

        // Execute the rule groups
        RuleResult result = rulesEngine.executeRuleGroupsList(ruleGroups, facts);

        // Verify the result
        assertNotNull(result);
        assertTrue(result.isTriggered());
        assertEquals("Credit Check", result.getRuleName());
    }

    @Test
    public void testExecuteRuleGroupsListWithNonMatchingRuleGroup() {
        // Create a rule group with AND operator
        RuleGroup ruleGroup = new RuleGroupBuilder()
            .withName("Credit Check")
            .withDescription("Check if person has good credit")
            .withAndOperator()
            .build();

        // Create rules for the rule group
        Rule rule1 = new RuleBuilder()
            .withName("Credit Score Rule")
            .withCondition("#creditScore >= 800")  // This will not match
            .withMessage("Person has excellent credit score")
            .build();

        Rule rule2 = new RuleBuilder()
            .withName("Employment Rule")
            .withCondition("#isEmployed == true")
            .withMessage("Person is employed")
            .build();

        // Add rules to the rule group
        ruleGroup.addRule(rule1, 1);
        ruleGroup.addRule(rule2, 2);

        List<RuleGroup> ruleGroups = new ArrayList<>();
        ruleGroups.add(ruleGroup);

        // Execute the rule groups
        RuleResult result = rulesEngine.executeRuleGroupsList(ruleGroups, facts);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isTriggered());
        assertEquals("no-match", result.getRuleName());
        assertEquals("No matching rules found", result.getMessage());
    }

    @Test
    public void testExecuteRuleGroupsListWithOrOperator() {
        // Create a rule group with OR operator
        RuleGroup ruleGroup = new RuleGroupBuilder()
            .withName("Credit Check")
            .withDescription("Check if person has good credit")
            .withOrOperator()
            .build();

        // Create rules for the rule group
        Rule rule1 = new RuleBuilder()
            .withName("Credit Score Rule")
            .withCondition("#creditScore >= 800")  // This will not match
            .withMessage("Person has excellent credit score")
            .build();

        Rule rule2 = new RuleBuilder()
            .withName("Employment Rule")
            .withCondition("#isEmployed == true")  // This will match
            .withMessage("Person is employed")
            .build();

        // Add rules to the rule group
        ruleGroup.addRule(rule1, 1);
        ruleGroup.addRule(rule2, 2);

        List<RuleGroup> ruleGroups = new ArrayList<>();
        ruleGroups.add(ruleGroup);

        // Execute the rule groups
        RuleResult result = rulesEngine.executeRuleGroupsList(ruleGroups, facts);

        // Verify the result
        assertNotNull(result);
        assertTrue(result.isTriggered());
        assertEquals("Credit Check", result.getRuleName());
    }

    @Test
    public void testExecuteRulesWithNoRules() {
        // Test executing an empty list of rules
        RuleResult result = rulesEngine.executeRules(new ArrayList<>(), facts);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isTriggered());
        assertEquals("no-rule", result.getRuleName());
        assertEquals("No rules provided", result.getMessage());
    }

    @Test
    public void testExecuteRulesWithNullRules() {
        // Test executing a null list of rules
        RuleResult result = rulesEngine.executeRules(null, facts);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isTriggered());
        assertEquals("no-rule", result.getRuleName());
        assertEquals("No rules provided", result.getMessage());
    }

    @Test
    public void testExecuteRulesWithMixedRulesAndRuleGroups() {
        // Create a rule
        Rule rule = new RuleBuilder()
            .withName("Age Rule")
            .withCondition("#age > 18")
            .withMessage("Person is an adult")
            .build();

        // Create a rule group with AND operator
        RuleGroup ruleGroup = new RuleGroupBuilder()
            .withName("Credit Check")
            .withDescription("Check if person has good credit")
            .withAndOperator()
            .build();

        // Create rules for the rule group
        Rule rule1 = new RuleBuilder()
            .withName("Credit Score Rule")
            .withCondition("#creditScore >= 700")
            .withMessage("Person has good credit score")
            .build();

        Rule rule2 = new RuleBuilder()
            .withName("Employment Rule")
            .withCondition("#isEmployed == true")
            .withMessage("Person is employed")
            .build();

        // Add rules to the rule group
        ruleGroup.addRule(rule1, 1);
        ruleGroup.addRule(rule2, 2);

        // Create a list of mixed rules and rule groups
        List<RuleBase> rules = new ArrayList<>();
        rules.add(rule);
        rules.add(ruleGroup);

        // Execute the rules
        RuleResult result = rulesEngine.executeRules(rules, facts);

        // Verify the result
        assertNotNull(result);
        assertTrue(result.isTriggered());
        assertEquals("Age Rule", result.getRuleName());
        assertEquals("Person is an adult", result.getMessage());
    }

    @Test
    public void testExecuteRulesForCategory() {
        // Create a rule and register it with the configuration
        Rule rule = configuration.rule()
            .withName("Age Rule")
            .withCondition("#age > 18")
            .withMessage("Person is an adult")
            .withCategory("demographic")
            .build();

        configuration.registerRule(rule);

        // Execute rules for the category
        RuleResult result = rulesEngine.executeRulesForCategory("demographic", facts);

        // Verify the result
        assertNotNull(result);
        assertTrue(result.isTriggered());
        assertEquals("Age Rule", result.getRuleName());
        assertEquals("Person is an adult", result.getMessage());
    }

    @Test
    public void testExecuteRulesForNonExistentCategory() {
        // Execute rules for a non-existent category
        RuleResult result = rulesEngine.executeRulesForCategory("nonExistentCategory", facts);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isTriggered());
        assertEquals("no-rule", result.getRuleName());
        assertEquals("No rules provided", result.getMessage());
    }

    @Test
    public void testExecuteRulesWithCustomParser() {
        // Create a rules engine with a custom parser
        RulesEngine customEngine = new RulesEngine(configuration, new SpelExpressionParser());

        // Create a rule
        Rule rule = new RuleBuilder()
            .withName("Age Rule")
            .withCondition("#age > 18")
            .withMessage("Person is an adult")
            .build();

        List<Rule> rules = new ArrayList<>();
        rules.add(rule);

        // Execute the rules
        RuleResult result = customEngine.executeRulesList(rules, facts);

        // Verify the result
        assertNotNull(result);
        assertTrue(result.isTriggered());
        assertEquals("Age Rule", result.getRuleName());
        assertEquals("Person is an adult", result.getMessage());
    }

    @Test
    public void testGetConfiguration() {
        // Verify that the configuration is accessible
        assertSame(configuration, rulesEngine.getConfiguration());
    }

    @Test
    public void testExecuteRuleWithNullRule() {
        // Test executing a null rule
        RuleResult result = rulesEngine.executeRule(null, facts);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isTriggered());
        assertEquals("no-rule", result.getRuleName());
        assertEquals("No rules provided", result.getMessage());
    }

    @Test
    public void testExecuteRuleWithMatchingRule() {
        // Create a rule that will match the facts
        Rule rule = new RuleBuilder()
            .withName("Age Rule")
            .withCondition("#age > 18")
            .withMessage("Person is an adult")
            .build();

        // Execute the rule
        RuleResult result = rulesEngine.executeRule(rule, facts);

        // Verify the result
        assertNotNull(result);
        assertTrue(result.isTriggered());
        assertEquals("Age Rule", result.getRuleName());
        assertEquals("Person is an adult", result.getMessage());
    }

    @Test
    public void testExecuteRuleWithNonMatchingRule() {
        // Create a rule that will not match the facts
        Rule rule = new RuleBuilder()
            .withName("Age Rule")
            .withCondition("#age < 18")
            .withMessage("Person is a minor")
            .build();

        // Execute the rule
        RuleResult result = rulesEngine.executeRule(rule, facts);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isTriggered());
        assertEquals("no-match", result.getRuleName());
        assertEquals("No matching rules found", result.getMessage());
    }

    @Test
    public void testExecuteRuleWithException() {
        // Create a rule with an invalid condition that will throw an exception
        Rule rule = new RuleBuilder()
            .withName("Invalid Rule")
            .withCondition("#nonExistentVariable > 10")
            .withMessage("This rule will throw an exception")
            .build();

        // Execute the rule
        RuleResult result = rulesEngine.executeRule(rule, facts);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isTriggered());
        assertEquals("Invalid Rule", result.getRuleName());
        assertTrue(result.getMessage().contains("Missing parameters"));
        assertTrue(result.getMessage().contains("nonExistentVariable"));
    }
}
