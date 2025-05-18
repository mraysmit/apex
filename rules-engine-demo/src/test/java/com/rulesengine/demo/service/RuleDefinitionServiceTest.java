package com.rulesengine.demo.service;

import com.rulesengine.core.engine.model.Rule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for rule definitions.
 * Uses the RuleDefinitionServiceDemo class that contains all example rules.
 */
public class RuleDefinitionServiceTest {

    @Test
    public void testCreateInvestmentRecommendationsRule() {
        Rule rule = RuleDefinitionServiceDemo.createInvestmentRecommendationsRule();

        assertNotNull(rule);
        assertEquals("Investment Recommendations", rule.getName());
        assertEquals("#inventory.?[#customer.preferredCategories.contains(category)]", rule.getCondition());
        assertEquals("Recommended financial instruments based on investor preferences", rule.getMessage());
    }

    @Test
    public void testCreateGoldTierInvestorOffersRule() {
        Rule rule = RuleDefinitionServiceDemo.createGoldTierInvestorOffersRule();

        assertNotNull(rule);
        assertEquals("Gold Tier Investor Offers", rule.getName());
        assertTrue(rule.getCondition().contains("#customer.membershipLevel == 'Gold'"));
        assertEquals("Special investment opportunities for Gold tier investors", rule.getMessage());
    }

    @Test
    public void testCreateLowCostInvestmentOptionsRule() {
        Rule rule = RuleDefinitionServiceDemo.createLowCostInvestmentOptionsRule();

        assertNotNull(rule);
        assertEquals("Low-Cost Investment Options", rule.getName());
        assertEquals("#inventory.?[price < 200].![name + ' - $' + price]", rule.getCondition());
        assertEquals("Low-cost investment options under $200", rule.getMessage());
    }

    @Test
    public void testCreateOrderProcessingRules() {
        List<Rule> rules = RuleDefinitionServiceDemo.createOrderProcessingRules();

        assertNotNull(rules);
        assertEquals(3, rules.size());

        // Check first rule
        Rule rule1 = rules.get(0);
        assertEquals("Free shipping eligibility", rule1.getName());
        assertEquals("order.calculateTotal() > 100", rule1.getCondition());

        // Check second rule
        Rule rule2 = rules.get(1);
        assertEquals("Premium discount", rule2.getName());
        assertEquals("customer.membershipLevel == 'Gold' and customer.age > 25", rule2.getCondition());

        // Check third rule
        Rule rule3 = rules.get(2);
        assertEquals("Express processing", rule3.getName());
        assertTrue(rule3.getCondition().contains("customer.isEligibleForDiscount()"));
    }

    @Test
    public void testCreateDiscountRules() {
        Map<String, String> discountRules = RuleDefinitionServiceDemo.createDiscountRules();

        assertNotNull(discountRules);
        assertEquals(3, discountRules.size());

        // Check rules for each membership level
        assertTrue(discountRules.containsKey("Basic"));
        assertTrue(discountRules.containsKey("Silver"));
        assertTrue(discountRules.containsKey("Gold"));

        // Check rule expressions
        assertTrue(discountRules.get("Basic").contains("customer.age > 60"));
        assertTrue(discountRules.get("Silver").contains("order.calculateTotal() > 200"));
        assertTrue(discountRules.get("Gold").contains("18 : 15"));
    }
}