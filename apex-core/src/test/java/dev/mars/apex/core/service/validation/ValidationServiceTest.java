package dev.mars.apex.core.service.validation;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.util.TestAwareLogger;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ValidationService.
 * 
 * Tests cover:
 * - Constructor variations and initialization
 * - Validation methods with type safety
 * - Error handling and edge cases
 * - Integration with rules engine and lookup registry
 * - Validator registration and retrieval
 * - Type checking and validation
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class ValidationServiceTest {

    private LookupServiceRegistry registry;
    private RulesEngine rulesEngine;
    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        registry = new LookupServiceRegistry();
        rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        validationService = new ValidationService(registry, rulesEngine);
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    @DisplayName("Should create ValidationService with registry and rules engine")
    void testConstructorWithRegistryAndRulesEngine() {
        ValidationService service = new ValidationService(registry, rulesEngine);
        
        assertNotNull(service);
        // Service should be properly initialized and ready to use
    }

    @Test
    @DisplayName("Should create ValidationService with registry only (default rules engine)")
    void testConstructorWithRegistryOnly() {
        ValidationService service = new ValidationService(registry);
        
        assertNotNull(service);
        // Service should create its own rules engine internally
    }

    @Test
    @DisplayName("Should handle null registry gracefully")
    void testConstructorWithNullRegistry() {
        // This should not throw an exception during construction
        assertDoesNotThrow(() -> {
            ValidationService service = new ValidationService(null, rulesEngine);
            assertNotNull(service);
        });
    }

    @Test
    @DisplayName("Should handle null rules engine gracefully")
    void testConstructorWithNullRulesEngine() {
        // This should not throw an exception during construction
        assertDoesNotThrow(() -> {
            ValidationService service = new ValidationService(registry, null);
            assertNotNull(service);
        });
    }

    // ========================================
    // Validation Method Tests
    // ========================================

    @Test
    @DisplayName("Should validate successfully with registered validator")
    void testValidateWithRegisteredValidator() {
        // Create and register a test validator
        TestStringValidator validator = new TestStringValidator("testValidator", 5);
        registry.registerService(validator);

        // Test valid input
        boolean result = validationService.validate("testValidator", "hello");
        assertTrue(result, "Valid string should pass validation");

        // Test invalid input
        boolean invalidResult = validationService.validate("testValidator", "hi");
        assertFalse(invalidResult, "Short string should fail validation");
    }

    @Test
    @DisplayName("Should return false for non-existent validator")
    void testValidateWithNonExistentValidator() {
        System.out.println("TEST: Triggering intentional error - testing validation with non-existent validator");
        
        boolean result = validationService.validate("nonExistentValidator", "test");
        assertFalse(result, "Validation with non-existent validator should return false");
    }

    @Test
    @DisplayName("Should handle type mismatch gracefully")
    void testValidateWithTypeMismatch() {
        System.out.println("TEST: Triggering intentional error - testing validation with type mismatch");
        
        // Register a string validator
        TestStringValidator validator = new TestStringValidator("stringValidator", 3);
        registry.registerService(validator);

        // Try to validate an integer with string validator
        boolean result = validationService.validate("stringValidator", 123);
        assertFalse(result, "Type mismatch should result in validation failure");
    }

    @Test
    @DisplayName("Should handle null value validation")
    void testValidateWithNullValue() {
        TestStringValidator validator = new TestStringValidator("nullTestValidator", 0);
        registry.registerService(validator);

        boolean result = validationService.validate("nullTestValidator", null);
        assertTrue(result, "Null value should be handled by validator");
    }

    // ========================================
    // ValidationWithResult Method Tests
    // ========================================

    @Test
    @DisplayName("Should return detailed result for successful validation")
    void testValidateWithResultSuccess() {
        TestStringValidator validator = new TestStringValidator("detailValidator", 3);
        registry.registerService(validator);

        RuleResult result = validationService.validateWithResult("detailValidator", "hello");
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Validation should succeed");
    }

    @Test
    @DisplayName("Should return error result for non-existent validator")
    void testValidateWithResultNonExistentValidator() {
        System.out.println("TEST: Triggering intentional error - testing detailed validation with non-existent validator");
        
        RuleResult result = validationService.validateWithResult("nonExistent", "test");
        
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Validation should fail");
        assertTrue(result.getMessage().contains("Validator not found"),
                  "Error message should indicate validator not found");
    }

    @Test
    @DisplayName("Should return error result for type mismatch")
    void testValidateWithResultTypeMismatch() {
        System.out.println("TEST: Triggering intentional error - testing detailed validation with type mismatch");
        
        TestStringValidator validator = new TestStringValidator("typeTestValidator", 3);
        registry.registerService(validator);

        RuleResult result = validationService.validateWithResult("typeTestValidator", 123);
        
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Validation should fail");
        assertTrue(result.getMessage().contains("cannot handle type"),
                  "Error message should indicate type mismatch");
    }

    // ========================================
    // Edge Cases and Error Handling
    // ========================================

    @Test
    @DisplayName("Should handle empty validator name")
    void testValidateWithEmptyValidatorName() {
        boolean result = validationService.validate("", "test");
        assertFalse(result, "Empty validator name should result in validation failure");
    }

    @Test
    @DisplayName("Should handle null validator name")
    void testValidateWithNullValidatorName() {
        boolean result = validationService.validate(null, "test");
        assertFalse(result, "Null validator name should result in validation failure");
    }

    @Test
    @DisplayName("Should handle complex object validation")
    void testValidateComplexObject() {
        TestComplexObjectValidator validator = new TestComplexObjectValidator("complexValidator");
        registry.registerService(validator);

        TestComplexObject validObject = new TestComplexObject("Valid", 25);
        TestComplexObject invalidObject = new TestComplexObject("", 15);

        assertTrue(validationService.validate("complexValidator", validObject), 
                  "Valid complex object should pass validation");
        assertFalse(validationService.validate("complexValidator", invalidObject), 
                   "Invalid complex object should fail validation");
    }

    // ========================================
    // Test Helper Classes
    // ========================================

    /**
     * Test validator for strings with minimum length requirement.
     */
    private static class TestStringValidator implements Validator<String> {
        private final String name;
        private final int minLength;

        public TestStringValidator(String name, int minLength) {
            this.name = name;
            this.minLength = minLength;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean validate(String value) {
            if (value == null) {
                return minLength == 0; // null is valid only if minLength is 0
            }
            return value.length() >= minLength;
        }

        @Override
        public Class<String> getType() {
            return String.class;
        }
    }

    /**
     * Test validator for complex objects.
     */
    private static class TestComplexObjectValidator implements Validator<TestComplexObject> {
        private final String name;

        public TestComplexObjectValidator(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean validate(TestComplexObject value) {
            if (value == null) {
                return false;
            }
            return value.getName() != null && !value.getName().isEmpty() && value.getAge() >= 18;
        }

        @Override
        public Class<TestComplexObject> getType() {
            return TestComplexObject.class;
        }
    }

    /**
     * Test complex object for validation.
     */
    private static class TestComplexObject {
        private final String name;
        private final int age;

        public TestComplexObject(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}
