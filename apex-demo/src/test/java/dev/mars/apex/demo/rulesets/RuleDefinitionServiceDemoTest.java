package dev.mars.apex.demo.rulesets;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RuleDefinitionServiceDemo to verify comprehensive rule definition functionality.
 */
public class RuleDefinitionServiceDemoTest {

    @Test
    void testCreateInvestmentRecommendationsRule() {
        Rule rule = RuleDefinitionServiceDemo.createInvestmentRecommendationsRule();
        
        assertNotNull(rule);
        assertEquals("Investment Recommendations", rule.getName());
        assertNotNull(rule.getCondition());
        assertNotNull(rule.getDescription());
        assertTrue(rule.getDescription().contains("investment") || rule.getDescription().contains("financial"));
    }

    @Test
    void testCreateHighValueTransactionRule() {
        Rule rule = RuleDefinitionServiceDemo.createHighValueTransactionRule();
        
        assertNotNull(rule);
        assertEquals("High Value Transaction", rule.getName());
        assertTrue(rule.getCondition().contains("#transaction.amount"));
        assertTrue(rule.getCondition().contains("10000"));
        assertTrue(rule.getDescription().contains("high-value"));
    }

    @Test
    void testCreateCustomerEligibilityRule() {
        Rule rule = RuleDefinitionServiceDemo.createCustomerEligibilityRule();
        
        assertNotNull(rule);
        assertEquals("Customer Eligibility", rule.getName());
        assertTrue(rule.getCondition().contains("#customer.creditScore"));
        assertTrue(rule.getCondition().contains("700"));
        assertTrue(rule.getDescription().contains("eligibility"));
    }

    @Test
    void testCreateProductAvailabilityRule() {
        Rule rule = RuleDefinitionServiceDemo.createProductAvailabilityRule();
        
        assertNotNull(rule);
        assertEquals("Product Availability", rule.getName());
        assertTrue(rule.getCondition().contains("#product.stockLevel"));
        assertTrue(rule.getCondition().contains("#product.isActive"));
        assertTrue(rule.getDescription().contains("availability") || rule.getDescription().contains("available"));
    }

    @Test
    void testCreateFinancialRules() {
        List<Rule> rules = RuleDefinitionServiceDemo.createFinancialRules();
        
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        assertTrue(rules.size() >= 4);
        
        // Verify all rules have names and conditions
        for (Rule rule : rules) {
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
        List<Rule> rules = RuleDefinitionServiceDemo.createCustomerRules();
        
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        assertTrue(rules.size() >= 4);
        
        // Verify all rules have proper structure
        for (Rule rule : rules) {
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
        List<Rule> rules = RuleDefinitionServiceDemo.createProductRules();
        
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        assertTrue(rules.size() >= 4);
        
        // Verify all rules have proper structure
        for (Rule rule : rules) {
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
        List<Rule> allRules = new ArrayList<>(RuleDefinitionServiceDemo.createFinancialRules());
        allRules.addAll(RuleDefinitionServiceDemo.createCustomerRules());
        allRules.addAll(RuleDefinitionServiceDemo.createProductRules());
        
        for (Rule rule : allRules) {
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
        List<Rule> allRules = new ArrayList<>(RuleDefinitionServiceDemo.createFinancialRules());
        allRules.addAll(RuleDefinitionServiceDemo.createCustomerRules());
        allRules.addAll(RuleDefinitionServiceDemo.createProductRules());
        
        for (Rule rule : allRules) {
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
        List<Rule> financialRules = RuleDefinitionServiceDemo.createFinancialRules();
        List<Rule> customerRules = RuleDefinitionServiceDemo.createCustomerRules();
        List<Rule> productRules = RuleDefinitionServiceDemo.createProductRules();
        
        // Verify collections have different focuses
        assertTrue(financialRules.stream().anyMatch(r -> r.getCondition().contains("transaction") || r.getCondition().contains("currency")),
                  "Financial rules should contain transaction or currency related conditions");
        
        assertTrue(customerRules.stream().anyMatch(r -> r.getCondition().contains("customer") || r.getCondition().contains("age")),
                  "Customer rules should contain customer or age related conditions");
        
        assertTrue(productRules.stream().anyMatch(r -> r.getCondition().contains("product") || r.getCondition().contains("price")),
                  "Product rules should contain product or price related conditions");
    }
}
