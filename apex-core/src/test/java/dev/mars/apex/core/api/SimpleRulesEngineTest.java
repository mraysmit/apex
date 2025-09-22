package dev.mars.apex.core.api;

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


import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SimpleRulesEngine API.
 *
 * This test class demonstrates both boolean and RuleResult approaches for
 * SimpleRulesEngine validation, providing developers with clear guidance
 * on when to use each method:
 *
 * - Boolean methods: Quick, simple validation for straightforward scenarios
 * - RuleResult methods: Detailed validation with comprehensive error reporting
 *
 * Enhanced in Phase 5B to include RuleResult validation alongside existing
 * boolean tests, serving as living documentation for dual approach usage.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
class SimpleRulesEngineTest {

    private SimpleRulesEngine simpleRulesEngine;

    @BeforeEach
    void setUp() {
        simpleRulesEngine = new SimpleRulesEngine();
    }

    @Test
    @DisplayName("Should create simple rules engine successfully")
    void testSimpleRulesEngineCreation() {
        assertNotNull(simpleRulesEngine);
    }

    @Test
    @DisplayName("Should evaluate simple condition")
    void testEvaluateSimpleCondition() {
        // Create test data
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 150.0);

        // Test boolean approach (existing functionality)
        boolean result = simpleRulesEngine.evaluate("amount > 100", facts);
        assertTrue(result, "Boolean result should be true for amount > 100");
    }

    @Test
    @DisplayName("Should evaluate simple condition with RuleResult validation")
    void testEvaluateSimpleCondition_WithRuleResult() {
        // Create test data
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 150.0);

        // Test boolean approach (quick validation)
        boolean boolResult = simpleRulesEngine.evaluate("amount > 100", facts);
        assertTrue(boolResult, "Boolean result should be true for amount > 100");

        // Test detailed RuleResult approach (comprehensive validation)
        // Create a rule manually to get detailed results
        Rule testRule = new Rule("simple-condition-test", "amount > 100", "Amount exceeds threshold");
        RuleResult detailedResult = simpleRulesEngine.getEngine().executeRule(testRule, facts);

        // Validate detailed results
        assertNotNull(detailedResult, "RuleResult should not be null");
        assertTrue(detailedResult.isTriggered(), "Rule should be triggered for amount > 100");
        assertEquals("simple-condition-test", detailedResult.getRuleName(), "Rule name should match");
        assertEquals("Amount exceeds threshold", detailedResult.getMessage(), "Rule message should match");

        // Demonstrate when to use each approach:
        // - Boolean: Quick validation, simple pass/fail scenarios
        // - RuleResult: Detailed analysis, debugging, comprehensive reporting
        System.out.println("Dual Approach Demonstration:");
        System.out.println("  Boolean result: " + boolResult + " (quick validation)");
        System.out.println("  RuleResult triggered: " + detailedResult.isTriggered() + " (detailed validation)");
        System.out.println("  Rule name: " + detailedResult.getRuleName());
        System.out.println("  Rule message: " + detailedResult.getMessage());
    }

    @Test
    @DisplayName("Should evaluate condition with object")
    void testEvaluateConditionWithObject() {
        // Create test object
        TestObject testObj = new TestObject();
        testObj.amount = 150.0;

        // Evaluate condition (using 'data' to reference the object)
        boolean result = simpleRulesEngine.evaluate("data.amount > 100", testObj);

        // Verify result
        assertTrue(result);
    }

    @Test
    @DisplayName("Should check age eligibility")
    void testAgeEligibility() {
        // Test eligible age
        assertTrue(simpleRulesEngine.isAgeEligible(25, 18), "25 should be eligible for minimum age 18");

        // Test ineligible age
        assertFalse(simpleRulesEngine.isAgeEligible(16, 18), "16 should not be eligible for minimum age 18");

        // Test exact minimum age
        assertTrue(simpleRulesEngine.isAgeEligible(18, 18), "18 should be eligible for minimum age 18");
    }

    @Test
    @DisplayName("Should check age eligibility with RuleResult validation")
    void testAgeEligibility_WithRuleResult() {
        // Test data for age eligibility scenarios
        int customerAge = 25;
        int minimumAge = 18;

        // Test boolean approach (quick eligibility check)
        boolean isEligible = simpleRulesEngine.isAgeEligible(customerAge, minimumAge);
        assertTrue(isEligible, "Customer should be eligible based on age");

        // Test detailed RuleResult approach (comprehensive eligibility analysis)
        Map<String, Object> facts = new HashMap<>();
        facts.put("customerAge", customerAge);
        facts.put("minimumAge", minimumAge);

        // Create the same rule that SimpleRulesEngine uses internally
        Rule ageRule = new Rule("age-eligibility-test", "#customerAge >= #minimumAge", "Customer meets age requirement");
        RuleResult detailedResult = simpleRulesEngine.getEngine().executeRule(ageRule, facts);

        // Validate detailed eligibility results
        assertNotNull(detailedResult, "RuleResult should not be null");
        assertTrue(detailedResult.isTriggered(), "Age eligibility rule should be triggered");
        assertEquals("age-eligibility-test", detailedResult.getRuleName(), "Rule name should match");
        assertEquals("Customer meets age requirement", detailedResult.getMessage(), "Rule message should match");

        // Test ineligible scenario with detailed analysis
        int underageCustomer = 16;
        Map<String, Object> underageFacts = new HashMap<>();
        underageFacts.put("customerAge", underageCustomer);
        underageFacts.put("minimumAge", minimumAge);

        boolean underageEligible = simpleRulesEngine.isAgeEligible(underageCustomer, minimumAge);
        assertFalse(underageEligible, "Underage customer should not be eligible");

        RuleResult underageResult = simpleRulesEngine.getEngine().executeRule(ageRule, underageFacts);
        assertFalse(underageResult.isTriggered(), "Age eligibility rule should not be triggered for underage customer");

        // Demonstrate usage scenarios:
        System.out.println("Age Eligibility Dual Approach:");
        System.out.println("  Eligible customer (age " + customerAge + "):");
        System.out.println("    Boolean result: " + isEligible + " (quick check)");
        System.out.println("    RuleResult triggered: " + detailedResult.isTriggered() + " (detailed analysis)");
        System.out.println("  Underage customer (age " + underageCustomer + "):");
        System.out.println("    Boolean result: " + underageEligible + " (quick check)");
        System.out.println("    RuleResult triggered: " + underageResult.isTriggered() + " (detailed analysis)");
        System.out.println("  Use boolean for: Simple eligibility gates, performance-critical checks");
        System.out.println("  Use RuleResult for: Audit trails, detailed reporting, debugging");
    }

    @Test
    @DisplayName("Should check amount in range")
    void testAmountInRange() {
        // Test amount within range
        assertTrue(simpleRulesEngine.isAmountInRange(50.0, 10.0, 100.0));

        // Test amount below range
        assertFalse(simpleRulesEngine.isAmountInRange(5.0, 10.0, 100.0));

        // Test amount above range
        assertFalse(simpleRulesEngine.isAmountInRange(150.0, 10.0, 100.0));

        // Test amount at boundaries
        assertTrue(simpleRulesEngine.isAmountInRange(10.0, 10.0, 100.0));
        assertTrue(simpleRulesEngine.isAmountInRange(100.0, 10.0, 100.0));
    }

    @Test
    @DisplayName("Should validate required fields")
    void testValidateRequiredFields() {
        TestObject testObj = new TestObject();
        testObj.name = "John";
        testObj.email = "john@example.com";

        // Test with all required fields present
        assertTrue(simpleRulesEngine.validateRequiredFields(testObj, "name", "email"));

        // Test with missing field
        testObj.email = null;
        assertFalse(simpleRulesEngine.validateRequiredFields(testObj, "name", "email"));
    }

    @Test
    @DisplayName("Should create validation rule")
    void testValidationRule() {
        Map<String, Object> data = new HashMap<>();
        data.put("age", 25);

        boolean result = simpleRulesEngine.validationRule("ageCheck", "age >= 18", "Valid age")
                                         .test(data);

        assertTrue(result, "Validation rule should pass for valid age");
    }

    @Test
    @DisplayName("Should create validation rule with RuleResult validation")
    void testValidationRule_WithRuleResult() {
        Map<String, Object> data = new HashMap<>();
        data.put("age", 25);

        // Test boolean approach (quick validation check)
        boolean boolResult = simpleRulesEngine.validationRule("ageCheck", "age >= 18", "Valid age")
                                             .test(data);
        assertTrue(boolResult, "Validation rule should pass for valid age");

        // Test detailed RuleResult approach (comprehensive validation analysis)
        Rule validationRule = simpleRulesEngine.validationRule("ageCheckDetailed", "age >= 18", "Valid age")
                                              .description("Age validation rule with detailed reporting")
                                              .build();

        RuleResult detailedResult = simpleRulesEngine.getEngine().executeRule(validationRule, data);

        // Validate detailed validation results
        assertNotNull(detailedResult, "RuleResult should not be null");
        assertTrue(detailedResult.isTriggered(), "Validation rule should be triggered for valid age");
        assertTrue(detailedResult.getRuleName().contains("ageCheckDetailed"), "Rule name should contain identifier");
        assertEquals("Valid age", detailedResult.getMessage(), "Rule message should match");

        // Test validation failure scenario
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("age", 16);

        boolean invalidResult = simpleRulesEngine.validationRule("ageCheck", "age >= 18", "Valid age")
                                                .test(invalidData);
        assertFalse(invalidResult, "Validation rule should fail for invalid age");

        RuleResult invalidDetailedResult = simpleRulesEngine.getEngine().executeRule(validationRule, invalidData);
        assertFalse(invalidDetailedResult.isTriggered(), "Validation rule should not be triggered for invalid age");

        // Demonstrate validation rule usage patterns:
        System.out.println("Validation Rule Dual Approach:");
        System.out.println("  Valid age (25):");
        System.out.println("    Boolean result: " + boolResult + " (quick validation)");
        System.out.println("    RuleResult triggered: " + detailedResult.isTriggered() + " (detailed validation)");
        System.out.println("  Invalid age (16):");
        System.out.println("    Boolean result: " + invalidResult + " (quick validation)");
        System.out.println("    RuleResult triggered: " + invalidDetailedResult.isTriggered() + " (detailed validation)");
        System.out.println("  Use boolean for: Input validation, form validation, quick checks");
        System.out.println("  Use RuleResult for: Validation error reporting, detailed diagnostics, audit trails");
    }

    @Test
    @DisplayName("Should create business rule")
    void testBusinessRule() {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", 1500.0);

        boolean result = simpleRulesEngine.businessRule("highValue", "amount > 1000", "High value transaction")
                                         .priority(1)
                                         .test(data);

        assertTrue(result, "Business rule should pass for high value transaction");
    }

    @Test
    @DisplayName("Should create business rule with RuleResult validation")
    void testBusinessRule_WithRuleResult() {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", 1500.0);

        // Test boolean approach (quick business rule validation)
        boolean boolResult = simpleRulesEngine.businessRule("highValue", "amount > 1000", "High value transaction")
                                             .priority(1)
                                             .test(data);
        assertTrue(boolResult, "Business rule should pass for high value transaction");

        // Test detailed RuleResult approach (comprehensive business rule analysis)
        Rule businessRule = simpleRulesEngine.businessRule("highValueDetailed", "amount > 1000", "High value transaction")
                                            .priority(1)
                                            .description("Business rule for high value transaction detection")
                                            .build();

        RuleResult detailedResult = simpleRulesEngine.getEngine().executeRule(businessRule, data);

        // Validate detailed business rule results
        assertNotNull(detailedResult, "RuleResult should not be null");
        assertTrue(detailedResult.isTriggered(), "Business rule should be triggered for high value transaction");
        assertTrue(detailedResult.getRuleName().contains("highValueDetailed"), "Rule name should contain identifier");
        assertEquals("High value transaction", detailedResult.getMessage(), "Rule message should match");

        // Test business rule failure scenario
        Map<String, Object> lowValueData = new HashMap<>();
        lowValueData.put("amount", 500.0);

        boolean lowValueResult = simpleRulesEngine.businessRule("highValue", "amount > 1000", "High value transaction")
                                                 .test(lowValueData);
        assertFalse(lowValueResult, "Business rule should fail for low value transaction");

        RuleResult lowValueDetailedResult = simpleRulesEngine.getEngine().executeRule(businessRule, lowValueData);
        assertFalse(lowValueDetailedResult.isTriggered(), "Business rule should not be triggered for low value transaction");

        // Demonstrate business rule usage patterns:
        System.out.println("Business Rule Dual Approach:");
        System.out.println("  High value transaction ($1500):");
        System.out.println("    Boolean result: " + boolResult + " (quick business validation)");
        System.out.println("    RuleResult triggered: " + detailedResult.isTriggered() + " (detailed business analysis)");
        System.out.println("    Rule category: business, Priority: " + businessRule.getPriority());
        System.out.println("  Low value transaction ($500):");
        System.out.println("    Boolean result: " + lowValueResult + " (quick business validation)");
        System.out.println("    RuleResult triggered: " + lowValueDetailedResult.isTriggered() + " (detailed business analysis)");
        System.out.println("  Use boolean for: Quick business decisions, workflow gates");
        System.out.println("  Use RuleResult for: Business rule auditing, compliance reporting, rule debugging");
    }

    @Test
    @DisplayName("Should create eligibility rule")
    void testEligibilityRule() {
        Map<String, Object> data = new HashMap<>();
        data.put("score", 85);

        boolean result = simpleRulesEngine.eligibilityRule("scoreCheck", "score >= 80", "Eligible score")
                                         .description("Score eligibility check")
                                         .test(data);

        assertTrue(result, "Eligibility rule should pass for score >= 80");
    }

    @Test
    @DisplayName("Should create eligibility rule with RuleResult validation")
    void testEligibilityRule_WithRuleResult() {
        Map<String, Object> data = new HashMap<>();
        data.put("score", 85);

        // Test boolean approach (quick eligibility determination)
        boolean boolResult = simpleRulesEngine.eligibilityRule("scoreCheck", "score >= 80", "Eligible score")
                                             .description("Score eligibility check")
                                             .test(data);
        assertTrue(boolResult, "Eligibility rule should pass for score >= 80");

        // Test detailed RuleResult approach (comprehensive eligibility analysis)
        Rule eligibilityRule = simpleRulesEngine.eligibilityRule("scoreCheckDetailed", "score >= 80", "Eligible score")
                                               .description("Detailed score eligibility check")
                                               .build();

        RuleResult detailedResult = simpleRulesEngine.getEngine().executeRule(eligibilityRule, data);

        // Validate detailed eligibility results
        assertNotNull(detailedResult, "RuleResult should not be null");
        assertTrue(detailedResult.isTriggered(), "Eligibility rule should be triggered for qualifying score");
        assertTrue(detailedResult.getRuleName().contains("scoreCheckDetailed"), "Rule name should contain identifier");
        assertEquals("Eligible score", detailedResult.getMessage(), "Rule message should match");

        // Test eligibility failure scenario
        Map<String, Object> lowScoreData = new HashMap<>();
        lowScoreData.put("score", 75);

        boolean lowScoreResult = simpleRulesEngine.eligibilityRule("scoreCheck", "score >= 80", "Eligible score")
                                                 .test(lowScoreData);
        assertFalse(lowScoreResult, "Eligibility rule should fail for score < 80");

        RuleResult lowScoreDetailedResult = simpleRulesEngine.getEngine().executeRule(eligibilityRule, lowScoreData);
        assertFalse(lowScoreDetailedResult.isTriggered(), "Eligibility rule should not be triggered for low score");

        // Test edge case - exact threshold
        Map<String, Object> thresholdData = new HashMap<>();
        thresholdData.put("score", 80);

        boolean thresholdResult = simpleRulesEngine.eligibilityRule("scoreCheck", "score >= 80", "Eligible score")
                                                  .test(thresholdData);
        assertTrue(thresholdResult, "Eligibility rule should pass for score exactly at threshold");

        RuleResult thresholdDetailedResult = simpleRulesEngine.getEngine().executeRule(eligibilityRule, thresholdData);
        assertTrue(thresholdDetailedResult.isTriggered(), "Eligibility rule should be triggered for threshold score");

        // Demonstrate eligibility rule usage patterns:
        System.out.println("Eligibility Rule Dual Approach:");
        System.out.println("  High score (85):");
        System.out.println("    Boolean result: " + boolResult + " (quick eligibility check)");
        System.out.println("    RuleResult triggered: " + detailedResult.isTriggered() + " (detailed eligibility analysis)");
        System.out.println("  Low score (75):");
        System.out.println("    Boolean result: " + lowScoreResult + " (quick eligibility check)");
        System.out.println("    RuleResult triggered: " + lowScoreDetailedResult.isTriggered() + " (detailed eligibility analysis)");
        System.out.println("  Threshold score (80):");
        System.out.println("    Boolean result: " + thresholdResult + " (quick eligibility check)");
        System.out.println("    RuleResult triggered: " + thresholdDetailedResult.isTriggered() + " (detailed eligibility analysis)");
        System.out.println("  Use boolean for: Quick eligibility gates, performance-sensitive checks");
        System.out.println("  Use RuleResult for: Eligibility auditing, detailed qualification analysis, compliance tracking");
    }

    @Test
    @DisplayName("Should provide access to underlying engine")
    void testGetEngine() {
        assertNotNull(simpleRulesEngine.getEngine());
        assertNotNull(simpleRulesEngine.getConfiguration());
    }

    // Test helper class
    public static class TestObject {
        public String name;
        public String email;
        public double amount;
    }
}
