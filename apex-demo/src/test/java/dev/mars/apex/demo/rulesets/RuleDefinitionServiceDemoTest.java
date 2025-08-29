package dev.mars.apex.demo.rulesets;

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



import dev.mars.apex.demo.evaluation.RuleDefinitionServiceDemo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RuleDefinitionServiceDemo to verify comprehensive rule definition functionality.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
/**
 * Test class for RuleDefinitionServiceDemo to verify comprehensive rule definition functionality.
 */
public class RuleDefinitionServiceDemoTest {

    @Test
    void testCreateInvestmentRecommendationsRule() {
        dev.mars.apex.core.engine.model.Rule rule = RuleDefinitionServiceDemo.createInvestmentRecommendationsRule();
        
        assertNotNull(rule);
        assertEquals("Investment Recommendations", rule.getName());
        assertNotNull(rule.getCondition());
        assertNotNull(rule.getDescription());
        assertTrue(rule.getDescription().contains("investment") || rule.getDescription().contains("financial"));
    }

    @Test
    void testCreateHighValueTransactionRule() {
        dev.mars.apex.core.engine.model.Rule rule = RuleDefinitionServiceDemo.createHighValueTransactionRule();
        
        assertNotNull(rule);
        assertEquals("High Value Transaction", rule.getName());
        assertTrue(rule.getCondition().contains("#transaction.amount"));
        assertTrue(rule.getCondition().contains("10000"));
        assertTrue(rule.getDescription().contains("high-value"));
    }

    @Test
    void testCreateCustomerEligibilityRule() {
        dev.mars.apex.core.engine.model.Rule rule = RuleDefinitionServiceDemo.createCustomerEligibilityRule();
        
        assertNotNull(rule);
        assertEquals("Customer Eligibility", rule.getName());
        assertTrue(rule.getCondition().contains("#customer.creditScore"));
        assertTrue(rule.getCondition().contains("700"));
        assertTrue(rule.getDescription().contains("eligibility"));
    }

    @Test
    void testCreateProductAvailabilityRule() {
        dev.mars.apex.core.engine.model.Rule rule = RuleDefinitionServiceDemo.createProductAvailabilityRule();
        
        assertNotNull(rule);
        assertEquals("Product Availability", rule.getName());
        assertTrue(rule.getCondition().contains("#product.stockLevel"));
        assertTrue(rule.getCondition().contains("#product.isActive"));
        assertTrue(rule.getDescription().contains("availability") || rule.getDescription().contains("available"));
    }

    @Test
    void testCreateFinancialRules() {
        List<dev.mars.apex.core.engine.model.Rule> rules = RuleDefinitionServiceDemo.createFinancialRules();
        
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        assertTrue(rules.size() >= 4);
        
        // Verify all rules have names and conditions
        for (dev.mars.apex.core.engine.model.Rule rule : rules) {
            assertNotNull(rule.getName());
            assertNotNull(rule.getCondition());
            assertNotNull(rule.getDescription());
            assertFalse(rule.getName().trim().isEmpty());
            assertFalse(rule.getCondition().trim().isEmpty());
        }
        
        // Verify specific financial rules exist
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("High Value Transaction")));
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("Credit Limit Check")));
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("Currency Validation")));
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("Daily Limit Check")));
    }

    @Test
    void testCreateCustomerRules() {
        List<dev.mars.apex.core.engine.model.Rule> rules = RuleDefinitionServiceDemo.createCustomerRules();
        
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        assertTrue(rules.size() >= 4);
        
        // Verify all rules have proper structure
        for (dev.mars.apex.core.engine.model.Rule rule : rules) {
            assertNotNull(rule.getName());
            assertNotNull(rule.getCondition());
            assertNotNull(rule.getDescription());
        }
        
        // Verify specific customer rules exist
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("Customer Eligibility")));
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("Age Verification")));
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("KYC Compliance")));
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("Account Status")));
    }

    @Test
    void testCreateProductRules() {
        List<dev.mars.apex.core.engine.model.Rule> rules = RuleDefinitionServiceDemo.createProductRules();
        
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        assertTrue(rules.size() >= 4);
        
        // Verify all rules have proper structure
        for (dev.mars.apex.core.engine.model.Rule rule : rules) {
            assertNotNull(rule.getName());
            assertNotNull(rule.getCondition());
            assertNotNull(rule.getDescription());
        }
        
        // Verify specific product rules exist
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("Product Availability")));
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("Price Range")));
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("Category Active")));
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("Seasonal Availability")));
    }

    @Test
    void testRuleConditionsAreValid() {
        // Test that all rule conditions contain valid SpEL expressions
        List<dev.mars.apex.core.engine.model.Rule> allRules = new ArrayList<>(RuleDefinitionServiceDemo.createFinancialRules());
        allRules.addAll(RuleDefinitionServiceDemo.createCustomerRules());
        allRules.addAll(RuleDefinitionServiceDemo.createProductRules());
        
        for (dev.mars.apex.core.engine.model.Rule rule : allRules) {
            String condition = rule.getCondition();
            
            // Basic validation - should contain SpEL variable references
            assertTrue(condition.contains("#") || condition.contains("'") || condition.contains("\""),
                      "Rule condition should contain SpEL expressions: " + rule.getName());
            
            // Should not be empty or just whitespace
            assertFalse(condition.trim().isEmpty(), "Rule condition should not be empty: " + rule.getName());
            
            // Should not contain obvious syntax errors
            assertFalse(condition.contains("##"), "Rule condition should not have double # symbols: " + rule.getName());
        }
    }

    @Test
    void testRuleDescriptionsAreInformative() {
        List<dev.mars.apex.core.engine.model.Rule> allRules = new ArrayList<>(RuleDefinitionServiceDemo.createFinancialRules());
        allRules.addAll(RuleDefinitionServiceDemo.createCustomerRules());
        allRules.addAll(RuleDefinitionServiceDemo.createProductRules());
        
        for (dev.mars.apex.core.engine.model.Rule rule : allRules) {
            String description = rule.getDescription();
            
            // Should have meaningful descriptions
            assertNotNull(description, "Rule description should not be null: " + rule.getName());
            assertTrue(description.length() > 10, "Rule description should be informative: " + rule.getName());
            
            // Should not just be the rule name
            assertNotEquals(rule.getName(), description, "Rule description should be different from name: " + rule.getName());
        }
    }

    @Test
    void testMainMethodDoesNotThrowException() {
        // Test that the main method can be called without throwing exceptions
        assertDoesNotThrow(() -> {
            RuleDefinitionServiceDemo.main(new String[]{});
        }, "Main method should execute without throwing exceptions");
    }

    @Test
    void testRuleCollectionsAreDistinct() {
        List<dev.mars.apex.core.engine.model.Rule> financialRules = RuleDefinitionServiceDemo.createFinancialRules();
        List<dev.mars.apex.core.engine.model.Rule> customerRules = RuleDefinitionServiceDemo.createCustomerRules();
        List<dev.mars.apex.core.engine.model.Rule> productRules = RuleDefinitionServiceDemo.createProductRules();
        
        // Verify collections have different focuses
        assertTrue(financialRules.stream().anyMatch(r -> r.getCondition().contains("transaction") || r.getCondition().contains("currency")),
                  "Financial rules should contain transaction or currency related conditions");
        
        assertTrue(customerRules.stream().anyMatch(r -> r.getCondition().contains("customer") || r.getCondition().contains("age")),
                  "Customer rules should contain customer or age related conditions");
        
        assertTrue(productRules.stream().anyMatch(r -> r.getCondition().contains("product") || r.getCondition().contains("price")),
                  "Product rules should contain product or price related conditions");
    }
}
