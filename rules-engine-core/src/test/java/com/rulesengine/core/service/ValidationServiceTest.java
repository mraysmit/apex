package com.rulesengine.core.service;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import com.rulesengine.core.service.validation.ValidationService;
import com.rulesengine.core.service.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ValidationService.
 */
public class ValidationServiceTest {

    private ValidationService validationService;
    private LookupServiceRegistry registry;
    private MockValidator mockValidator;
    private TestRulesEngine testRulesEngine;

    @BeforeEach
    public void setUp() {
        registry = new LookupServiceRegistry();
        mockValidator = new MockValidator("testValidator");
        registry.registerService(mockValidator);
        testRulesEngine = new TestRulesEngine();
        validationService = new ValidationService(registry, testRulesEngine);
    }

    @Test
    public void testValidateWithValidValidator() {
        // Configure the test rules engine to return a successful result
        testRulesEngine.setRuleResult(RuleResult.match("Test Rule", "Test Message"));

        // Test with a valid value
        mockValidator.setValidationResult(true);
        assertTrue(validationService.validate("testValidator", "testValue"));

        // Verify the validator was called with the correct value
        assertEquals("testValue", mockValidator.getLastValidatedValue());

        // Verify the rules engine was called with the correct parameters
        assertNotNull(testRulesEngine.getLastRules());
        assertEquals(1, testRulesEngine.getLastRules().size());
        assertEquals("Validation Rule for testValidator", testRulesEngine.getLastRules().get(0).getName());
        assertEquals("#validator.validate(#value)", testRulesEngine.getLastRules().get(0).getCondition());

        assertNotNull(testRulesEngine.getLastFacts());
        assertEquals(mockValidator, testRulesEngine.getLastFacts().get("validator"));
        assertEquals("testValue", testRulesEngine.getLastFacts().get("value"));
    }

    @Test
    public void testValidateWithInvalidValidator() {
        // Configure the test rules engine to return a failed result
        testRulesEngine.setRuleResult(RuleResult.noMatch());

        // Test with an invalid value
        mockValidator.setValidationResult(false);
        assertFalse(validationService.validate("testValidator", "testValue"));

        // Verify the validator was called with the correct value
        assertEquals("testValue", mockValidator.getLastValidatedValue());

        // Verify the rules engine was called with the correct parameters
        assertNotNull(testRulesEngine.getLastRules());
        assertEquals(1, testRulesEngine.getLastRules().size());
        assertEquals("Validation Rule for testValidator", testRulesEngine.getLastRules().get(0).getName());
        assertEquals("#validator.validate(#value)", testRulesEngine.getLastRules().get(0).getCondition());

        assertNotNull(testRulesEngine.getLastFacts());
        assertEquals(mockValidator, testRulesEngine.getLastFacts().get("validator"));
        assertEquals("testValue", testRulesEngine.getLastFacts().get("value"));
    }

    @Test
    public void testValidateWithNonExistentValidator() {
        // Test with a validator that doesn't exist
        assertFalse(validationService.validate("nonExistentValidator", "testValue"));

        // Verify the mock validator wasn't called
        assertNull(mockValidator.getLastValidatedValue());

        // Verify the rules engine wasn't called
        assertNull(testRulesEngine.getLastRules());
        assertNull(testRulesEngine.getLastFacts());
    }

    @Test
    public void testValidateWithNullValidatorName() {
        // Test with a null validator name
        assertFalse(validationService.validate(null, "testValue"));

        // Verify the mock validator wasn't called
        assertNull(mockValidator.getLastValidatedValue());

        // Verify the rules engine wasn't called
        assertNull(testRulesEngine.getLastRules());
        assertNull(testRulesEngine.getLastFacts());
    }

    @Test
    public void testValidateWithNullValue() {
        // Configure the test rules engine to return a successful result
        testRulesEngine.setRuleResult(RuleResult.match("Test Rule", "Test Message"));

        // Test with a null value
        mockValidator.setValidationResult(true);
        assertTrue(validationService.validate("testValidator", null));

        // Verify the validator was called with null
        assertNull(mockValidator.getLastValidatedValue());

        // Verify the rules engine was called with the correct parameters
        assertNotNull(testRulesEngine.getLastRules());
        assertEquals(1, testRulesEngine.getLastRules().size());
        assertEquals("Validation Rule for testValidator", testRulesEngine.getLastRules().get(0).getName());
        assertEquals("#validator.validate(#value)", testRulesEngine.getLastRules().get(0).getCondition());

        assertNotNull(testRulesEngine.getLastFacts());
        assertEquals(mockValidator, testRulesEngine.getLastFacts().get("validator"));
        assertNull(testRulesEngine.getLastFacts().get("value"));

        // Configure the test rules engine to return a failed result
        testRulesEngine.setRuleResult(RuleResult.noMatch());

        // Test with a null value and validator returning false
        mockValidator.setValidationResult(false);
        assertFalse(validationService.validate("testValidator", null));

        // Verify the validator was called with null
        assertNull(mockValidator.getLastValidatedValue());
    }

    @Test
    public void testValidateWithDifferentValueTypes() {
        // Configure the test rules engine to return a successful result
        testRulesEngine.setRuleResult(RuleResult.match("Test Rule", "Test Message"));

        // Test with integer value
        mockValidator.setValidationResult(true);
        assertTrue(validationService.validate("testValidator", 123));
        assertEquals(123, mockValidator.getLastValidatedValue());
        assertEquals(123, testRulesEngine.getLastFacts().get("value"));

        // Test with boolean value
        mockValidator.setValidationResult(true);
        assertTrue(validationService.validate("testValidator", true));
        assertEquals(true, mockValidator.getLastValidatedValue());
        assertEquals(true, testRulesEngine.getLastFacts().get("value"));

        // Test with object value
        Object testObject = new Object();
        mockValidator.setValidationResult(true);
        assertTrue(validationService.validate("testValidator", testObject));
        assertSame(testObject, mockValidator.getLastValidatedValue());
        assertSame(testObject, testRulesEngine.getLastFacts().get("value"));
    }

    /**
     * Mock implementation of Validator for testing.
     */
    private static class MockValidator implements Validator<Object> {
        private final String name;
        private boolean validationResult;
        private Object lastValidatedValue;
        private RuleResult customRuleResult;

        public MockValidator(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean validate(Object value) {
            lastValidatedValue = value;
            return validationResult;
        }

        @Override
        public RuleResult validateWithResult(Object value) {
            lastValidatedValue = value;

            // If a custom RuleResult has been set, return it
            if (customRuleResult != null) {
                return customRuleResult;
            }

            // Otherwise, create a RuleResult based on the validation result
            if (validationResult) {
                return RuleResult.match(getName(), "Validation successful for " + getName());
            } else {
                return RuleResult.noMatch();
            }
        }

        @Override
        public Class<Object> getType() {
            return Object.class;
        }

        public void setValidationResult(boolean validationResult) {
            this.validationResult = validationResult;
        }

        public void setCustomRuleResult(RuleResult customRuleResult) {
            this.customRuleResult = customRuleResult;
        }

        public Object getLastValidatedValue() {
            return lastValidatedValue;
        }
    }

    /**
     * Test implementation of RulesEngine for testing.
     */
    private static class TestRulesEngine extends RulesEngine {
        private RuleResult ruleResult;
        private List<Rule> lastRules;
        private Map<String, Object> lastFacts;

        public TestRulesEngine() {
            super(new RulesEngineConfiguration());
            this.ruleResult = RuleResult.noMatch();
        }

        @Override
        public RuleResult executeRulesList(List<Rule> rules, Map<String, Object> facts) {
            this.lastRules = rules;
            this.lastFacts = facts;
            return ruleResult;
        }

        public void setRuleResult(RuleResult ruleResult) {
            this.ruleResult = ruleResult;
        }

        public List<Rule> getLastRules() {
            return lastRules;
        }

        public Map<String, Object> getLastFacts() {
            return lastFacts;
        }
    }
}
