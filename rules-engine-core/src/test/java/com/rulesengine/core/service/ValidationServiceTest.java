package com.rulesengine.core.service;

import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import com.rulesengine.core.service.validation.ValidationService;
import com.rulesengine.core.service.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ValidationService.
 */
public class ValidationServiceTest {

    private ValidationService validationService;
    private LookupServiceRegistry registry;
    private MockValidator mockValidator;

    @BeforeEach
    public void setUp() {
        registry = new LookupServiceRegistry();
        mockValidator = new MockValidator("testValidator");
        registry.registerService(mockValidator);
        validationService = new ValidationService(registry);
    }

    @Test
    public void testValidateWithValidValidator() {
        // Test with a valid value
        mockValidator.setValidationResult(true);
        assertTrue(validationService.validate("testValidator", "testValue"));

        // Verify the validator was called with the correct value
        assertEquals("testValue", mockValidator.getLastValidatedValue());
    }

    @Test
    public void testValidateWithInvalidValidator() {
        // Test with an invalid value
        mockValidator.setValidationResult(false);
        assertFalse(validationService.validate("testValidator", "testValue"));

        // Verify the validator was called with the correct value
        assertEquals("testValue", mockValidator.getLastValidatedValue());
    }

    @Test
    public void testValidateWithNonExistentValidator() {
        // Test with a validator that doesn't exist
        assertFalse(validationService.validate("nonExistentValidator", "testValue"));

        // Verify the mock validator wasn't called
        assertNull(mockValidator.getLastValidatedValue());
    }

    @Test
    public void testValidateWithNullValidatorName() {
        // Test with a null validator name
        assertFalse(validationService.validate(null, "testValue"));

        // Verify the mock validator wasn't called
        assertNull(mockValidator.getLastValidatedValue());
    }

    @Test
    public void testValidateWithNullValue() {
        // Test with a null value
        mockValidator.setValidationResult(true);
        assertTrue(validationService.validate("testValidator", null));

        // Verify the validator was called with null
        assertNull(mockValidator.getLastValidatedValue());

        // Test with a null value and validator returning false
        mockValidator.setValidationResult(false);
        assertFalse(validationService.validate("testValidator", null));

        // Verify the validator was called with null
        assertNull(mockValidator.getLastValidatedValue());
    }

    @Test
    public void testValidateWithDifferentValueTypes() {
        // Test with integer value
        mockValidator.setValidationResult(true);
        assertTrue(validationService.validate("testValidator", 123));
        assertEquals(123, mockValidator.getLastValidatedValue());

        // Test with boolean value
        mockValidator.setValidationResult(true);
        assertTrue(validationService.validate("testValidator", true));
        assertEquals(true, mockValidator.getLastValidatedValue());

        // Test with object value
        Object testObject = new Object();
        mockValidator.setValidationResult(true);
        assertTrue(validationService.validate("testValidator", testObject));
        assertSame(testObject, mockValidator.getLastValidatedValue());
    }

    /**
     * Mock implementation of Validator for testing.
     */
    private static class MockValidator implements Validator {
        private final String name;
        private boolean validationResult;
        private Object lastValidatedValue;

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

        public void setValidationResult(boolean validationResult) {
            this.validationResult = validationResult;
        }

        public Object getLastValidatedValue() {
            return lastValidatedValue;
        }
    }
}
