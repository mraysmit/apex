package dev.mars.rulesengine.core.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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
 * Test class for the RulesService API.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for the RulesService API.
 */
public class RulesTest {

    private RulesService rulesService;

    @BeforeEach
    void setUp() {
        // Create a fresh RulesService instance for each test
        rulesService = new RulesService();
    }

    @Test
    void testSimpleCheck() {
        // Test basic condition checking
        Map<String, Object> facts = new HashMap<>();
        facts.put("age", 25);

        assertTrue(rulesService.check("#age >= 18", facts));
        assertFalse(rulesService.check("#age >= 30", facts));
    }

    @Test
    void testCheckWithObject() {
        // Test checking against a single object
        TestCustomer customer = new TestCustomer("John", 25, "john@example.com");

        assertTrue(rulesService.check("#data.age >= 18", customer));
        assertTrue(rulesService.check("#data.name == 'John'", customer));
        assertFalse(rulesService.check("#data.age >= 30", customer));
    }

    @Test
    void testNamedRules() {
        // Test defining and using named rules
        rulesService.define("adult", "#age >= 18");
        rulesService.define("senior", "#age >= 65");

        assertTrue(rulesService.isDefined("adult"));
        assertTrue(rulesService.isDefined("senior"));
        assertFalse(rulesService.isDefined("child"));

        Map<String, Object> facts = new HashMap<>();
        facts.put("age", 25);

        assertTrue(rulesService.test("adult", facts));
        assertFalse(rulesService.test("senior", facts));
    }

    @Test
    void testNamedRulesWithObject() {
        // Test named rules with objects
        rulesService.define("adult", "#data.age >= 18");
        rulesService.define("has-email", "#data.email != null");

        TestCustomer customer = new TestCustomer("John", 25, "john@example.com");

        assertTrue(rulesService.test("adult", customer));
        assertTrue(rulesService.test("has-email", customer));
    }

    @Test
    void testNamedRulesWithCustomMessage() {
        // Test named rules with custom messages
        rulesService.define("adult", "#age >= 18", "Customer is an adult");

        Map<String, Object> facts = new HashMap<>();
        facts.put("age", 25);

        assertTrue(rulesService.test("adult", facts));
    }

    @Test
    void testFluentValidation() {
        // Test fluent validation API
        TestCustomer customer = new TestCustomer("John", 25, "john@example.com");

        boolean valid = rulesService.validate(customer)
                .that("#data.age >= 18", "Must be adult")
                .that("#data.email != null", "Email required")
                .passes();

        assertTrue(valid);
    }

    @Test
    void testFluentValidationWithFailure() {
        // Test fluent validation with failures
        TestCustomer customer = new TestCustomer("John", 16, null);

        boolean valid = rulesService.validate(customer)
                .that("#data.age >= 18", "Must be adult")
                .that("#data.email != null", "Email required")
                .passes();

        assertFalse(valid);
    }

    @Test
    void testValidationResult() {
        // Test detailed validation results
        TestCustomer customer = new TestCustomer("John", 16, null);

        ValidationResult result = rulesService.validate(customer)
                .that("#data.age >= 18", "Must be adult")
                .that("#data.email != null", "Email required")
                .validate();
        
        assertFalse(result.isValid());
        assertTrue(result.isInvalid());
        assertEquals(2, result.getErrorCount());
        assertTrue(result.hasErrors());
        
        assertTrue(result.getErrors().contains("Must be adult"));
        assertTrue(result.getErrors().contains("Email required"));
    }

    @Test
    void testValidationBuilderHelpers() {
        // Test validation builder helper methods
        TestCustomer customer = new TestCustomer("John", 25, "john@example.com");

        boolean valid = rulesService.validate(customer)
                .minimumAge(18)
                .emailRequired()
                .notNull("name")
                .passes();

        assertTrue(valid);
    }

    @Test
    void testValidationBuilderHelpersWithFailure() {
        // Test validation builder helpers with failures
        TestCustomer customer = new TestCustomer(null, 16, null);

        ValidationResult result = rulesService.validate(customer)
                .minimumAge(18)
                .emailRequired()
                .notNull("name")
                .validate();

        assertFalse(result.isValid());
        assertEquals(3, result.getErrorCount());
    }

    @Test
    void testValidationWithMap() {
        // Test validation with map data
        Map<String, Object> data = new HashMap<>();
        data.put("age", 25);
        data.put("email", "john@example.com");
        data.put("balance", 1500.0);

        boolean valid = rulesService.validate(data)
                .that("#age >= 18", "Must be adult")
                .that("#email != null", "Email required")
                .that("#balance >= 1000", "Minimum balance required")
                .passes();

        assertTrue(valid);
    }

    @Test
    void testGetDefinedRules() {
        // Test getting defined rule names
        rulesService.define("rule1", "#age >= 18");
        rulesService.define("rule2", "#balance > 1000");

        String[] ruleNames = rulesService.getDefinedRules();
        assertEquals(2, ruleNames.length);
        assertTrue(java.util.Arrays.asList(ruleNames).contains("rule1"));
        assertTrue(java.util.Arrays.asList(ruleNames).contains("rule2"));
    }

    @Test
    void testClearNamedRules() {
        // Test clearing named rules
        rulesService.define("rule1", "#age >= 18");
        rulesService.define("rule2", "#balance > 1000");

        assertEquals(2, rulesService.getDefinedRules().length);

        rulesService.clearNamedRules();

        assertEquals(0, rulesService.getDefinedRules().length);
        assertFalse(rulesService.isDefined("rule1"));
        assertFalse(rulesService.isDefined("rule2"));
    }

    @Test
    void testRuleNotFound() {
        // Test exception when rule not found
        Map<String, Object> facts = new HashMap<>();
        facts.put("age", 25);

        assertThrows(IllegalArgumentException.class, () -> {
            rulesService.test("nonexistent", facts);
        });
    }

    @Test
    void testErrorHandling() {
        // Test that invalid conditions fail gracefully
        Map<String, Object> facts = new HashMap<>();
        facts.put("age", 25);

        // Invalid SpEL expression should return false
        assertFalse(rulesService.check("#invalid.expression.that.will.fail", facts));
    }

    @Test
    void testValidationResultCombine() {
        // Test combining validation results
        ValidationResult result1 = ValidationResult.success();
        ValidationResult result2 = ValidationResult.failure("Error 1");
        ValidationResult result3 = ValidationResult.failure("Error 2");
        
        ValidationResult combined = result1.combine(result2).combine(result3);
        
        assertFalse(combined.isValid());
        assertEquals(2, combined.getErrorCount());
        assertTrue(combined.getErrors().contains("Error 1"));
        assertTrue(combined.getErrors().contains("Error 2"));
    }

    @Test
    void testValidationResultThrowIfInvalid() {
        // Test throwing exception on invalid result
        ValidationResult invalidResult = ValidationResult.failure("Test error");
        
        assertThrows(ValidationResult.ValidationException.class, () -> {
            invalidResult.throwIfInvalid();
        });
        
        // Valid result should not throw
        ValidationResult validResult = ValidationResult.success();
        assertDoesNotThrow(() -> {
            validResult.throwIfInvalid();
        });
    }

    // Helper class for testing
    private static class TestCustomer {
        private final String name;
        private final int age;
        private final String email;
        
        public TestCustomer(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }
        
        public String getName() { return name; }
        public int getAge() { return age; }
        public String getEmail() { return email; }
    }
}
