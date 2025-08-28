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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SimpleRulesEngine API.
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

        // Evaluate condition
        boolean result = simpleRulesEngine.evaluate("amount > 100", facts);

        // Verify result
        assertTrue(result);
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
        assertTrue(simpleRulesEngine.isAgeEligible(25, 18));

        // Test ineligible age
        assertFalse(simpleRulesEngine.isAgeEligible(16, 18));

        // Test exact minimum age
        assertTrue(simpleRulesEngine.isAgeEligible(18, 18));
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

        assertTrue(result);
    }

    @Test
    @DisplayName("Should create business rule")
    void testBusinessRule() {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", 1500.0);

        boolean result = simpleRulesEngine.businessRule("highValue", "amount > 1000", "High value transaction")
                                         .priority(1)
                                         .test(data);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should create eligibility rule")
    void testEligibilityRule() {
        Map<String, Object> data = new HashMap<>();
        data.put("score", 85);

        boolean result = simpleRulesEngine.eligibilityRule("scoreCheck", "score >= 80", "Eligible score")
                                         .description("Score eligibility check")
                                         .test(data);

        assertTrue(result);
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
