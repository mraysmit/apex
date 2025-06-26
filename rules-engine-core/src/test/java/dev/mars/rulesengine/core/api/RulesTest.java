package dev.mars.rulesengine.core.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the simplified Rules API.
 */
public class RulesTest {

    @BeforeEach
    void setUp() {
        // Clear any previously defined rules
        Rules.clearNamedRules();
    }

    @Test
    void testSimpleCheck() {
        // Test basic condition checking
        Map<String, Object> facts = new HashMap<>();
        facts.put("age", 25);
        
        assertTrue(Rules.check("#age >= 18", facts));
        assertFalse(Rules.check("#age >= 30", facts));
    }

    @Test
    void testCheckWithObject() {
        // Test checking against a single object
        TestCustomer customer = new TestCustomer("John", 25, "john@example.com");
        
        assertTrue(Rules.check("#data.age >= 18", customer));
        assertTrue(Rules.check("#data.name == 'John'", customer));
        assertFalse(Rules.check("#data.age >= 30", customer));
    }

    @Test
    void testNamedRules() {
        // Test defining and using named rules
        Rules.define("adult", "#age >= 18");
        Rules.define("senior", "#age >= 65");
        
        assertTrue(Rules.isDefined("adult"));
        assertTrue(Rules.isDefined("senior"));
        assertFalse(Rules.isDefined("child"));
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("age", 25);
        
        assertTrue(Rules.test("adult", facts));
        assertFalse(Rules.test("senior", facts));
    }

    @Test
    void testNamedRulesWithObject() {
        // Test named rules with objects
        Rules.define("adult", "#data.age >= 18");
        Rules.define("has-email", "#data.email != null");
        
        TestCustomer customer = new TestCustomer("John", 25, "john@example.com");
        
        assertTrue(Rules.test("adult", customer));
        assertTrue(Rules.test("has-email", customer));
    }

    @Test
    void testNamedRulesWithCustomMessage() {
        // Test named rules with custom messages
        Rules.define("adult", "#age >= 18", "Customer is an adult");
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("age", 25);
        
        assertTrue(Rules.test("adult", facts));
    }

    @Test
    void testFluentValidation() {
        // Test fluent validation API
        TestCustomer customer = new TestCustomer("John", 25, "john@example.com");
        
        boolean valid = Rules.validate(customer)
                .that("#data.age >= 18", "Must be adult")
                .that("#data.email != null", "Email required")
                .passes();
        
        assertTrue(valid);
    }

    @Test
    void testFluentValidationWithFailure() {
        // Test fluent validation with failures
        TestCustomer customer = new TestCustomer("John", 16, null);
        
        boolean valid = Rules.validate(customer)
                .that("#data.age >= 18", "Must be adult")
                .that("#data.email != null", "Email required")
                .passes();
        
        assertFalse(valid);
    }

    @Test
    void testValidationResult() {
        // Test detailed validation results
        TestCustomer customer = new TestCustomer("John", 16, null);
        
        ValidationResult result = Rules.validate(customer)
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
        
        boolean valid = Rules.validate(customer)
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
        
        ValidationResult result = Rules.validate(customer)
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
        
        boolean valid = Rules.validate(data)
                .that("#age >= 18", "Must be adult")
                .that("#email != null", "Email required")
                .that("#balance >= 1000", "Minimum balance required")
                .passes();
        
        assertTrue(valid);
    }

    @Test
    void testGetDefinedRules() {
        // Test getting defined rule names
        Rules.define("rule1", "#age >= 18");
        Rules.define("rule2", "#balance > 1000");
        
        String[] ruleNames = Rules.getDefinedRules();
        assertEquals(2, ruleNames.length);
        assertTrue(java.util.Arrays.asList(ruleNames).contains("rule1"));
        assertTrue(java.util.Arrays.asList(ruleNames).contains("rule2"));
    }

    @Test
    void testClearNamedRules() {
        // Test clearing named rules
        Rules.define("rule1", "#age >= 18");
        Rules.define("rule2", "#balance > 1000");
        
        assertEquals(2, Rules.getDefinedRules().length);
        
        Rules.clearNamedRules();
        
        assertEquals(0, Rules.getDefinedRules().length);
        assertFalse(Rules.isDefined("rule1"));
        assertFalse(Rules.isDefined("rule2"));
    }

    @Test
    void testRuleNotFound() {
        // Test exception when rule not found
        Map<String, Object> facts = new HashMap<>();
        facts.put("age", 25);
        
        assertThrows(IllegalArgumentException.class, () -> {
            Rules.test("nonexistent", facts);
        });
    }

    @Test
    void testErrorHandling() {
        // Test that invalid conditions fail gracefully
        Map<String, Object> facts = new HashMap<>();
        facts.put("age", 25);
        
        // Invalid SpEL expression should return false
        assertFalse(Rules.check("#invalid.expression.that.will.fail", facts));
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
