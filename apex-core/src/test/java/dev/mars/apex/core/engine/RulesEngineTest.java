package dev.mars.apex.core.engine;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the core RulesEngine functionality.
 * Tests rule creation, configuration, and basic engine operations.
 */
class RulesEngineTest {

    private RulesEngine rulesEngine;
    private RulesEngineConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new RulesEngineConfiguration();
        rulesEngine = new RulesEngine(configuration);
    }

    @Test
    @DisplayName("Should create rules engine with valid configuration")
    void testRulesEngineCreation() {
        assertNotNull(rulesEngine);
        assertNotNull(rulesEngine.getConfiguration());
    }

    @Test
    @DisplayName("Should create rule with basic constructor")
    void testRuleCreation() {
        // Test the basic Rule constructor
        Rule rule = new Rule("testRule", "amount > 100", "HIGH_VALUE");

        assertNotNull(rule);
        assertEquals("testRule", rule.getName());
        assertEquals("amount > 100", rule.getCondition());
        assertEquals("HIGH_VALUE", rule.getMessage());
        assertNotNull(rule.getId());
        assertNotNull(rule.getUuid());
        assertEquals(100, rule.getPriority()); // Default priority
    }

    @Test
    @DisplayName("Should create rule with full constructor")
    void testRuleCreationWithFullConstructor() {
        // Test the full Rule constructor
        Rule rule = new Rule("R001", "validation", "testRule", "amount > 100", 
                           "HIGH_VALUE", "Rule for high value transactions", 1);

        assertNotNull(rule);
        assertEquals("R001", rule.getId());
        assertEquals("testRule", rule.getName());
        assertEquals("amount > 100", rule.getCondition());
        assertEquals("HIGH_VALUE", rule.getMessage());
        assertEquals("Rule for high value transactions", rule.getDescription());
        assertEquals(1, rule.getPriority());
    }

    @Test
    @DisplayName("Should create rule group")
    void testRuleGroupCreation() {
        // Test RuleGroup creation
        RuleGroup ruleGroup = new RuleGroup("RG001", "validation", "Test Group", 
                                           "Test group description", 1, true);

        assertNotNull(ruleGroup);
        assertEquals("RG001", ruleGroup.getId());
        assertEquals("Test Group", ruleGroup.getName());
        assertEquals("Test group description", ruleGroup.getDescription());
        assertEquals(1, ruleGroup.getPriority());
        assertTrue(ruleGroup.isAndOperator());
    }

    @Test
    @DisplayName("Should handle rule evaluation context")
    void testRuleEvaluationContext() {
        Rule rule = new Rule("contextRule", "name != null && age > 18", "VALID_ADULT");

        // Create evaluation context
        Map<String, Object> context = new HashMap<>();
        context.put("name", "John Doe");
        context.put("age", 25);

        // Test that rule can access its properties
        assertNotNull(rule.getCondition());
        assertTrue(rule.getCondition().contains("name"));
        assertTrue(rule.getCondition().contains("age"));
    }

    @Test
    @DisplayName("Should handle rule metadata")
    void testRuleMetadata() {
        Rule rule = new Rule("metadataRule", "true", "Always true");

        assertNotNull(rule.getMetadata());
        assertNotNull(rule.getMetadata().getCreatedDate());
        assertEquals("system", rule.getMetadata().getCreatedByUser());
    }

    @Test
    @DisplayName("Should handle rule categories")
    void testRuleCategories() {
        Rule rule = new Rule("categoryRule", "true", "Test rule");

        assertNotNull(rule.getCategories());
        assertFalse(rule.getCategories().isEmpty());
        // Should have default category
        assertEquals(1, rule.getCategories().size());
    }

    @Test
    @DisplayName("Should handle rule group with multiple categories")
    void testRuleGroupWithMultipleCategories() {
        // Test creating rule group with multiple categories
        java.util.Set<String> categoryNames = new java.util.HashSet<>();
        categoryNames.add("validation");
        categoryNames.add("business");

        RuleGroup ruleGroup = RuleGroup.fromCategoryNames("RG002", categoryNames, 
                                                         "Multi-Category Group", 
                                                         "Group with multiple categories", 
                                                         1, false);

        assertNotNull(ruleGroup);
        assertEquals("RG002", ruleGroup.getId());
        assertEquals("Multi-Category Group", ruleGroup.getName());
        assertFalse(ruleGroup.isAndOperator()); // OR operator
        assertEquals(2, ruleGroup.getCategories().size());
    }
}
