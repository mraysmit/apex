package dev.mars.rulesengine.core.engine.model;

import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RuleGroup.
 * This class tests the functionality of the RuleGroup class.
 */
public class RuleGroupTest {

    /**
     * Test creating a rule group with AND operator.
     */
    @Test
    public void testCreateRuleGroupWithAnd() {
        // Create a rule group with AND operator
        RuleGroup ruleGroup = new RuleGroup(
            "group-001",
            "test-category",
            "TestRuleGroup",
            "This is a test rule group",
            5,
            true // AND operator
        );
        
        // Verify rule group properties
        assertEquals("group-001", ruleGroup.getId(), "Rule group ID should match");
        assertEquals("TestRuleGroup", ruleGroup.getName(), "Rule group name should match");
        assertEquals("This is a test rule group", ruleGroup.getDescription(), "Rule group description should match");
        assertEquals(5, ruleGroup.getPriority(), "Rule group priority should match");
        assertTrue(ruleGroup.isAndOperator(), "Rule group should use AND operator");
        assertNotNull(ruleGroup.getUuid(), "Rule group UUID should not be null");
        assertEquals(1, ruleGroup.getCategories().size(), "Rule group should have one category");
        assertTrue(ruleGroup.hasCategory("test-category"), "Rule group should have test-category");
        assertEquals(0, ruleGroup.getRules().size(), "Rule group should have no rules initially");
    }
    
    /**
     * Test creating a rule group with OR operator.
     */
    @Test
    public void testCreateRuleGroupWithOr() {
        // Create a rule group with OR operator
        RuleGroup ruleGroup = new RuleGroup(
            "group-002",
            "test-category",
            "TestRuleGroup",
            "This is a test rule group",
            5,
            false // OR operator
        );
        
        // Verify rule group properties
        assertEquals("group-002", ruleGroup.getId(), "Rule group ID should match");
        assertEquals("TestRuleGroup", ruleGroup.getName(), "Rule group name should match");
        assertEquals("This is a test rule group", ruleGroup.getDescription(), "Rule group description should match");
        assertEquals(5, ruleGroup.getPriority(), "Rule group priority should match");
        assertFalse(ruleGroup.isAndOperator(), "Rule group should use OR operator");
        assertNotNull(ruleGroup.getUuid(), "Rule group UUID should not be null");
        assertEquals(1, ruleGroup.getCategories().size(), "Rule group should have one category");
        assertTrue(ruleGroup.hasCategory("test-category"), "Rule group should have test-category");
        assertEquals(0, ruleGroup.getRules().size(), "Rule group should have no rules initially");
    }
    
    /**
     * Test creating a rule group with multiple categories.
     */
    @Test
    public void testCreateRuleGroupWithMultipleCategories() {
        // Create a set of category names
        Set<String> categoryNames = new HashSet<>();
        categoryNames.add("category1");
        categoryNames.add("category2");
        categoryNames.add("category3");
        
        // Create a rule group with multiple categories
        RuleGroup ruleGroup = RuleGroup.fromCategoryNames(
            "group-003",
            categoryNames,
            "MultiCategoryRuleGroup",
            "This rule group has multiple categories",
            10,
            true // AND operator
        );
        
        // Verify rule group properties
        assertEquals("group-003", ruleGroup.getId(), "Rule group ID should match");
        assertEquals("MultiCategoryRuleGroup", ruleGroup.getName(), "Rule group name should match");
        assertEquals("This rule group has multiple categories", ruleGroup.getDescription(), "Rule group description should match");
        assertEquals(10, ruleGroup.getPriority(), "Rule group priority should match");
        assertTrue(ruleGroup.isAndOperator(), "Rule group should use AND operator");
        assertNotNull(ruleGroup.getUuid(), "Rule group UUID should not be null");
        assertEquals(3, ruleGroup.getCategories().size(), "Rule group should have three categories");
        assertTrue(ruleGroup.hasCategory("category1"), "Rule group should have category1");
        assertTrue(ruleGroup.hasCategory("category2"), "Rule group should have category2");
        assertTrue(ruleGroup.hasCategory("category3"), "Rule group should have category3");
        assertEquals(0, ruleGroup.getRules().size(), "Rule group should have no rules initially");
    }
    
    /**
     * Test adding rules to a rule group.
     */
    @Test
    public void testAddRules() {
        // Create a rule group
        RuleGroup ruleGroup = new RuleGroup(
            "group-004",
            "test-category",
            "TestRuleGroup",
            "This is a test rule group",
            5,
            true // AND operator
        );
        
        // Create some rules
        Rule rule1 = new Rule("Rule1", "#value > 10", "Value is greater than 10");
        Rule rule2 = new Rule("Rule2", "#value < 20", "Value is less than 20");
        Rule rule3 = new Rule("Rule3", "#value != 15", "Value is not 15");
        
        // Add the rules to the rule group
        ruleGroup.addRule(rule1, 1);
        ruleGroup.addRule(rule2, 2);
        ruleGroup.addRule(rule3, 3);
        
        // Verify the rules were added
        assertEquals(3, ruleGroup.getRules().size(), "Rule group should have three rules");
    }
    
    /**
     * Test evaluating a rule group with AND operator.
     */
    @Test
    public void testEvaluateRuleGroupWithAnd() {
        // Create a rule group with AND operator
        RuleGroup ruleGroup = new RuleGroup(
            "group-005",
            "test-category",
            "TestRuleGroup",
            "This is a test rule group",
            5,
            true // AND operator
        );
        
        // Create some rules
        Rule rule1 = new Rule("Rule1", "#value > 10", "Value is greater than 10");
        Rule rule2 = new Rule("Rule2", "#value < 20", "Value is less than 20");
        Rule rule3 = new Rule("Rule3", "#value != 15", "Value is not 15");
        
        // Add the rules to the rule group
        ruleGroup.addRule(rule1, 1);
        ruleGroup.addRule(rule2, 2);
        ruleGroup.addRule(rule3, 3);
        
        // Create an evaluation context
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // Test with a value that satisfies all rules
        context.setVariable("value", 12);
        assertTrue(ruleGroup.evaluate(context), "Rule group should evaluate to true when all rules are satisfied");
        
        // Test with a value that doesn't satisfy one rule
        context.setVariable("value", 5);
        assertFalse(ruleGroup.evaluate(context), "Rule group should evaluate to false when one rule is not satisfied");
        
        // Test with a value that doesn't satisfy another rule
        context.setVariable("value", 25);
        assertFalse(ruleGroup.evaluate(context), "Rule group should evaluate to false when one rule is not satisfied");
        
        // Test with a value that doesn't satisfy yet another rule
        context.setVariable("value", 15);
        assertFalse(ruleGroup.evaluate(context), "Rule group should evaluate to false when one rule is not satisfied");
    }
    
    /**
     * Test evaluating a rule group with OR operator.
     */
    @Test
    public void testEvaluateRuleGroupWithOr() {
        // Create a rule group with OR operator
        RuleGroup ruleGroup = new RuleGroup(
            "group-006",
            "test-category",
            "TestRuleGroup",
            "This is a test rule group",
            5,
            false // OR operator
        );
        
        // Create some rules
        Rule rule1 = new Rule("Rule1", "#value > 10", "Value is greater than 10");
        Rule rule2 = new Rule("Rule2", "#value < 5", "Value is less than 5");
        Rule rule3 = new Rule("Rule3", "#value == 7", "Value is 7");
        
        // Add the rules to the rule group
        ruleGroup.addRule(rule1, 1);
        ruleGroup.addRule(rule2, 2);
        ruleGroup.addRule(rule3, 3);
        
        // Create an evaluation context
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // Test with a value that satisfies the first rule
        context.setVariable("value", 15);
        assertTrue(ruleGroup.evaluate(context), "Rule group should evaluate to true when one rule is satisfied");
        
        // Test with a value that satisfies the second rule
        context.setVariable("value", 3);
        assertTrue(ruleGroup.evaluate(context), "Rule group should evaluate to true when one rule is satisfied");
        
        // Test with a value that satisfies the third rule
        context.setVariable("value", 7);
        assertTrue(ruleGroup.evaluate(context), "Rule group should evaluate to true when one rule is satisfied");
        
        // Test with a value that doesn't satisfy any rule
        context.setVariable("value", 8);
        assertFalse(ruleGroup.evaluate(context), "Rule group should evaluate to false when no rules are satisfied");
    }
}