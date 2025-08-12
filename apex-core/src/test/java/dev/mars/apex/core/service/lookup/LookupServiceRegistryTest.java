package dev.mars.apex.core.service.lookup;

import dev.mars.apex.core.service.common.NamedService;
import dev.mars.apex.core.service.validation.Validator;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for LookupServiceRegistry.
 * 
 * Tests cover:
 * - Service registration and retrieval
 * - Type-safe service lookup
 * - Service name queries by type
 * - Edge cases with null/invalid inputs
 * - Multiple services of same type
 * - Service overwriting behavior
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class LookupServiceRegistryTest {

    private LookupServiceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new LookupServiceRegistry();
    }

    // ========================================
    // Service Registration Tests
    // ========================================

    @Test
    @DisplayName("Should register service successfully")
    void testRegisterService() {
        TestLookupService service = new TestLookupService("testService");
        
        assertDoesNotThrow(() -> registry.registerService(service));
        
        // Verify service can be retrieved
        LookupService retrieved = registry.getService("testService", LookupService.class);
        assertNotNull(retrieved, "Registered service should be retrievable");
        assertEquals("testService", retrieved.getName(), "Retrieved service should have correct name");
    }

    @Test
    @DisplayName("Should handle null service registration gracefully")
    void testRegisterNullService() {
        System.out.println("TEST: Triggering intentional error - testing null service registration");
        
        assertThrows(NullPointerException.class, () -> {
            registry.registerService(null);
        }, "Registering null service should throw NullPointerException");
    }

    @Test
    @DisplayName("Should overwrite existing service with same name")
    void testRegisterServiceOverwrite() {
        TestLookupService service1 = new TestLookupService("sameName");
        TestValidator service2 = new TestValidator("sameName");
        
        registry.registerService(service1);
        registry.registerService(service2);
        
        // The second service should overwrite the first
        NamedService retrieved = registry.getService("sameName", NamedService.class);
        assertNotNull(retrieved, "Service should be retrievable");
        assertTrue(retrieved instanceof TestValidator, "Retrieved service should be the second one registered");
    }

    // ========================================
    // Service Retrieval Tests
    // ========================================

    @Test
    @DisplayName("Should retrieve service by name and type")
    void testGetServiceByNameAndType() {
        TestLookupService lookupService = new TestLookupService("lookup1");
        TestValidator validator = new TestValidator("validator1");
        
        registry.registerService(lookupService);
        registry.registerService(validator);
        
        // Retrieve by specific type
        LookupService retrievedLookup = registry.getService("lookup1", LookupService.class);
        assertNotNull(retrievedLookup, "LookupService should be retrievable");
        assertEquals("lookup1", retrievedLookup.getName());
        
        Validator<?> retrievedValidator = registry.getService("validator1", Validator.class);
        assertNotNull(retrievedValidator, "Validator should be retrievable");
        assertEquals("validator1", retrievedValidator.getName());
    }

    @Test
    @DisplayName("Should return null for non-existent service")
    void testGetNonExistentService() {
        LookupService result = registry.getService("nonExistent", LookupService.class);
        assertNull(result, "Non-existent service should return null");
    }

    @Test
    @DisplayName("Should return null for wrong type")
    void testGetServiceWithWrongType() {
        TestLookupService service = new TestLookupService("testService");
        registry.registerService(service);

        // Try to retrieve as a completely unrelated type
        TestTransformer result = registry.getService("testService", TestTransformer.class);
        assertNull(result, "Service retrieved with wrong type should return null");
    }

    @Test
    @DisplayName("Should handle null service name gracefully")
    void testGetServiceWithNullName() {
        LookupService result = registry.getService(null, LookupService.class);
        assertNull(result, "Null service name should return null");
    }

    @Test
    @DisplayName("Should handle null type gracefully")
    void testGetServiceWithNullType() {
        TestLookupService service = new TestLookupService("testService");
        registry.registerService(service);
        
        assertThrows(NullPointerException.class, () -> {
            registry.getService("testService", null);
        }, "Null type should throw NullPointerException");
    }

    // ========================================
    // Service Names Query Tests
    // ========================================

    @Test
    @DisplayName("Should return service names by type")
    void testGetServiceNamesByType() {
        TestLookupService lookup1 = new TestLookupService("lookup1");
        TestLookupService lookup2 = new TestLookupService("lookup2");
        TestValidator validator1 = new TestValidator("validator1");

        registry.registerService(lookup1);
        registry.registerService(lookup2);
        registry.registerService(validator1);

        String[] lookupNames = registry.getServiceNames(LookupService.class);
        String[] validatorNames = registry.getServiceNames(TestValidator.class);

        assertEquals(2, lookupNames.length, "Should find 2 lookup services");
        assertEquals(1, validatorNames.length, "Should find 1 TestValidator");

        List<String> lookupNamesList = Arrays.asList(lookupNames);
        assertTrue(lookupNamesList.contains("lookup1"), "Should contain lookup1");
        assertTrue(lookupNamesList.contains("lookup2"), "Should contain lookup2");

        assertEquals("validator1", validatorNames[0], "Should contain validator1");
    }

    @Test
    @DisplayName("Should return empty array for type with no services")
    void testGetServiceNamesForEmptyType() {
        String[] names = registry.getServiceNames(Validator.class);
        assertNotNull(names, "Result should not be null");
        assertEquals(0, names.length, "Should return empty array for type with no services");
    }

    @Test
    @DisplayName("Should handle null type in service names query")
    void testGetServiceNamesWithNullType() {
        // The actual implementation may handle null gracefully rather than throwing
        assertDoesNotThrow(() -> {
            String[] names = registry.getServiceNames(null);
            assertNotNull(names, "Should return empty array for null type");
        }, "Null type should be handled gracefully");
    }

    // ========================================
    // Integration and Complex Scenarios
    // ========================================

    @Test
    @DisplayName("Should handle multiple services of different types")
    void testMultipleServiceTypes() {
        TestLookupService lookup = new TestLookupService("lookup");
        TestValidator validator = new TestValidator("validator");
        TestTransformer transformer = new TestTransformer("transformer");
        
        registry.registerService(lookup);
        registry.registerService(validator);
        registry.registerService(transformer);
        
        // Verify all can be retrieved by their specific types
        assertNotNull(registry.getService("lookup", LookupService.class));
        assertNotNull(registry.getService("validator", Validator.class));
        assertNotNull(registry.getService("transformer", TestTransformer.class));
        
        // Verify all can be retrieved as NamedService
        assertNotNull(registry.getService("lookup", NamedService.class));
        assertNotNull(registry.getService("validator", NamedService.class));
        assertNotNull(registry.getService("transformer", NamedService.class));
    }

    @Test
    @DisplayName("Should handle service registration and retrieval with inheritance")
    void testServiceInheritance() {
        TestSpecialLookupService specialService = new TestSpecialLookupService("special");
        registry.registerService(specialService);
        
        // Should be retrievable as both specific type and parent type
        TestSpecialLookupService asSpecial = registry.getService("special", TestSpecialLookupService.class);
        LookupService asLookup = registry.getService("special", LookupService.class);
        NamedService asNamed = registry.getService("special", NamedService.class);
        
        assertNotNull(asSpecial, "Should be retrievable as specific type");
        assertNotNull(asLookup, "Should be retrievable as parent type");
        assertNotNull(asNamed, "Should be retrievable as base interface");
        
        assertSame(asSpecial, asLookup, "Should be same instance");
        assertSame(asSpecial, asNamed, "Should be same instance");
    }

    // ========================================
    // Test Helper Classes
    // ========================================

    /**
     * Test implementation of LookupService.
     */
    private static class TestLookupService extends LookupService {
        public TestLookupService(String name) {
            super(name, Arrays.asList("value1", "value2", "value3"));
        }
    }

    /**
     * Test implementation of Validator.
     */
    private static class TestValidator implements Validator<String> {
        private final String name;

        public TestValidator(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean validate(String value) {
            return value != null && !value.isEmpty();
        }

        @Override
        public Class<String> getType() {
            return String.class;
        }
    }

    /**
     * Test transformer service.
     */
    private static class TestTransformer implements NamedService {
        private final String name;

        public TestTransformer(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * Test special lookup service that extends TestLookupService.
     */
    private static class TestSpecialLookupService extends TestLookupService {
        public TestSpecialLookupService(String name) {
            super(name);
        }

        public String getSpecialFeature() {
            return "special";
        }
    }
}
