package dev.mars.apex.demo.service;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.demo.rulesets.RuleDefinitionServiceDemo;
import org.junit.jupiter.api.Test;

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
 * Test class for rule definitions.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
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
