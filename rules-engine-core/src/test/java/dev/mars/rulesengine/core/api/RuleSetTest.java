package dev.mars.rulesengine.core.api;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the RuleSet template API.
 */
public class RuleSetTest {

    @Test
    void testValidationRuleSet() {
        // Test validation rule set creation
        RulesEngine engine = RuleSet.validation()
                .ageCheck(18)
                .emailRequired()
                .phoneRequired()
                .build();
        
        assertNotNull(engine);
        
        // Test with valid data
        Map<String, Object> validData = new HashMap<>();
        validData.put("age", 25);
        validData.put("email", "john@example.com");
        validData.put("phone", "123-456-7890");
        
        List<Rule> rules = engine.getConfiguration().getAllRules();
        assertEquals(3, rules.size());
        
        // All validation rules should pass
        for (Rule rule : rules) {
            RuleResult result = engine.executeRule(rule, validData);
            assertTrue(result.isTriggered(), "Rule should pass: " + rule.getName());
        }
    }

    @Test
    void testValidationRuleSetWithInvalidData() {
        // Test validation rule set with invalid data
        RulesEngine engine = RuleSet.validation()
                .ageCheck(18)
                .emailRequired()
                .build();
        
        // Test with invalid data
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("age", 16); // Too young
        invalidData.put("email", null); // Missing email
        
        List<Rule> rules = engine.getConfiguration().getAllRules();
        
        // Both validation rules should fail
        for (Rule rule : rules) {
            RuleResult result = engine.executeRule(rule, invalidData);
            assertFalse(result.isTriggered(), "Rule should fail: " + rule.getName());
        }
    }

    @Test
    void testBusinessRuleSet() {
        // Test business rule set creation
        RulesEngine engine = RuleSet.business()
                .premiumEligibility("#balance > 5000 && #membershipYears > 2")
                .discountEligibility("#age > 65 || #membershipLevel == 'Gold'")
                .vipStatus("#totalPurchases > 10000")
                .build();
        
        assertNotNull(engine);
        
        // Test with premium eligible customer
        Map<String, Object> premiumCustomer = new HashMap<>();
        premiumCustomer.put("balance", 6000.0);
        premiumCustomer.put("membershipYears", 3);
        premiumCustomer.put("age", 45);
        premiumCustomer.put("membershipLevel", "Silver");
        premiumCustomer.put("totalPurchases", 5000.0);
        
        List<Rule> rules = engine.getConfiguration().getAllRules();
        assertEquals(3, rules.size());
        
        // Check premium eligibility rule
        Rule premiumRule = rules.stream()
                .filter(r -> r.getName().equals("Premium Eligibility"))
                .findFirst()
                .orElse(null);
        assertNotNull(premiumRule);
        
        RuleResult premiumResult = engine.executeRule(premiumRule, premiumCustomer);
        assertTrue(premiumResult.isTriggered());
    }

    @Test
    void testEligibilityRuleSet() {
        // Test eligibility rule set creation
        RulesEngine engine = RuleSet.eligibility()
                .minimumAge(21)
                .minimumIncome(50000)
                .creditScoreCheck(650)
                .build();
        
        assertNotNull(engine);
        
        // Test with eligible customer
        Map<String, Object> eligibleCustomer = new HashMap<>();
        eligibleCustomer.put("age", 25);
        eligibleCustomer.put("income", 60000.0);
        eligibleCustomer.put("creditScore", 700);
        
        List<Rule> rules = engine.getConfiguration().getAllRules();
        assertEquals(3, rules.size());
        
        // All eligibility rules should pass
        for (Rule rule : rules) {
            RuleResult result = engine.executeRule(rule, eligibleCustomer);
            assertTrue(result.isTriggered(), "Rule should pass: " + rule.getName());
        }
    }

    @Test
    void testFinancialRuleSet() {
        // Test financial rule set creation
        RulesEngine engine = RuleSet.financial()
                .minimumBalance(1000)
                .transactionLimit(5000)
                .kycRequired()
                .build();
        
        assertNotNull(engine);
        
        // Test with valid financial data
        Map<String, Object> financialData = new HashMap<>();
        financialData.put("balance", 2000.0);
        financialData.put("amount", 3000.0);
        financialData.put("kycVerified", true);
        
        List<Rule> rules = engine.getConfiguration().getAllRules();
        assertEquals(3, rules.size());
        
        // All financial rules should pass
        for (Rule rule : rules) {
            RuleResult result = engine.executeRule(rule, financialData);
            assertTrue(result.isTriggered(), "Rule should pass: " + rule.getName());
        }
    }

    @Test
    void testCustomRuleInRuleSet() {
        // Test adding custom rules to rule sets
        RulesEngine engine = RuleSet.validation()
                .ageCheck(18)
                .customRule("Custom Validation", "#customField != null", "Custom field is required")
                .build();
        
        Map<String, Object> data = new HashMap<>();
        data.put("age", 25);
        data.put("customField", "value");
        
        List<Rule> rules = engine.getConfiguration().getAllRules();
        assertEquals(2, rules.size());
        
        // Find the custom rule
        Rule customRule = rules.stream()
                .filter(r -> r.getName().equals("Custom Validation"))
                .findFirst()
                .orElse(null);
        assertNotNull(customRule);
        
        RuleResult result = engine.executeRule(customRule, data);
        assertTrue(result.isTriggered());
    }

    @Test
    void testRuleSetGetRules() {
        // Test getting rules from rule set before building
        RuleSet.ValidationRuleSet ruleSet = RuleSet.validation()
                .ageCheck(18)
                .emailRequired();
        
        List<Rule> rules = ruleSet.getRules();
        assertEquals(2, rules.size());
        assertEquals(2, ruleSet.getRuleCount());
        
        // Verify rule names
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("Age Check")));
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("Email Required")));
    }

    @Test
    void testValidationRuleSetStringLength() {
        // Test string length validation
        RulesEngine engine = RuleSet.validation()
                .stringLength("username", 3, 20)
                .build();
        
        // Test with valid username
        Map<String, Object> validData = new HashMap<>();
        validData.put("username", "john_doe");
        
        Rule rule = engine.getConfiguration().getAllRules().get(0);
        RuleResult result = engine.executeRule(rule, validData);
        assertTrue(result.isTriggered());
        
        // Test with invalid username (too short)
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("username", "jo");
        
        result = engine.executeRule(rule, invalidData);
        assertFalse(result.isTriggered());
    }

    @Test
    void testValidationRuleSetFieldRequired() {
        // Test field required validation
        RulesEngine engine = RuleSet.validation()
                .fieldRequired("firstName")
                .fieldRequired("lastName")
                .build();
        
        // Test with all required fields
        Map<String, Object> validData = new HashMap<>();
        validData.put("firstName", "John");
        validData.put("lastName", "Doe");
        
        List<Rule> rules = engine.getConfiguration().getAllRules();
        assertEquals(2, rules.size());
        
        for (Rule rule : rules) {
            RuleResult result = engine.executeRule(rule, validData);
            assertTrue(result.isTriggered(), "Rule should pass: " + rule.getName());
        }
        
        // Test with missing field
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("firstName", "John");
        // lastName is missing
        
        Rule lastNameRule = rules.stream()
                .filter(r -> r.getName().equals("lastName Required"))
                .findFirst()
                .orElse(null);
        assertNotNull(lastNameRule);
        
        RuleResult result = engine.executeRule(lastNameRule, invalidData);
        assertFalse(result.isTriggered());
    }

    @Test
    void testRuleSetCategories() {
        // Test that rules are properly categorized
        RuleSet.ValidationRuleSet validationSet = RuleSet.validation().ageCheck(18);
        RuleSet.BusinessRuleSet businessSet = RuleSet.business().premiumEligibility("#balance > 1000");
        RuleSet.EligibilityRuleSet eligibilitySet = RuleSet.eligibility().minimumAge(21);
        RuleSet.FinancialRuleSet financialSet = RuleSet.financial().minimumBalance(500);
        
        Rule validationRule = validationSet.getRules().get(0);
        Rule businessRule = businessSet.getRules().get(0);
        Rule eligibilityRule = eligibilitySet.getRules().get(0);
        Rule financialRule = financialSet.getRules().get(0);
        
        assertTrue(validationRule.hasCategory("validation"));
        assertTrue(businessRule.hasCategory("business"));
        assertTrue(eligibilityRule.hasCategory("eligibility"));
        assertTrue(financialRule.hasCategory("financial"));
    }

    @Test
    void testRuleSetPriorities() {
        // Test that rules have proper priorities
        RuleSet.ValidationRuleSet ruleSet = RuleSet.validation()
                .ageCheck(18)
                .emailRequired()
                .phoneRequired();
        
        List<Rule> rules = ruleSet.getRules();
        
        // Rules should have increasing priorities
        assertEquals(1, rules.get(0).getPriority());
        assertEquals(2, rules.get(1).getPriority());
        assertEquals(3, rules.get(2).getPriority());
    }

    @Test
    void testEmptyRuleSet() {
        // Test building an empty rule set
        RulesEngine engine = RuleSet.validation().build();
        
        assertNotNull(engine);
        assertEquals(0, engine.getConfiguration().getAllRules().size());
    }
}
