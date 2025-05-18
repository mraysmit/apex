package com.rulesengine.core.service.lookup;

import com.rulesengine.core.service.transform.Enricher;
import com.rulesengine.core.service.transform.Transformer;
import com.rulesengine.core.service.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for LookupServiceRegistry.
 */
public class LookupServiceRegistryTest {
    private LookupServiceRegistry registry;
    private MockValidator mockValidator;
    private MockEnricher mockEnricher;
    private MockTransformer mockTransformer;
    private LookupService lookupService;

    @BeforeEach
    public void setUp() {
        registry = new LookupServiceRegistry();
        mockValidator = new MockValidator("TestValidator");
        mockEnricher = new MockEnricher("TestEnricher");
        mockTransformer = new MockTransformer("TestTransformer");
        lookupService = new LookupService("TestLookupService", Arrays.asList("value1", "value2"));
    }

    @Test
    public void testRegisterService() {
        // Register a service
        registry.registerService(mockValidator);

        // Verify the service was registered
        Validator retrievedValidator = registry.getService("TestValidator", Validator.class);
        assertNotNull(retrievedValidator);
        assertEquals("TestValidator", retrievedValidator.getName());
    }

    @Test
    public void testRegisterMultipleServices() {
        // Register multiple services
        registry.registerService(mockValidator);
        registry.registerService(mockEnricher);
        registry.registerService(mockTransformer);
        registry.registerService(lookupService);

        // Verify all services were registered
        Validator retrievedValidator = registry.getService("TestValidator", Validator.class);
        assertNotNull(retrievedValidator);
        assertEquals("TestValidator", retrievedValidator.getName());

        Enricher retrievedEnricher = registry.getService("TestEnricher", Enricher.class);
        assertNotNull(retrievedEnricher);
        assertEquals("TestEnricher", retrievedEnricher.getName());

        Transformer retrievedTransformer = registry.getService("TestTransformer", Transformer.class);
        assertNotNull(retrievedTransformer);
        assertEquals("TestTransformer", retrievedTransformer.getName());

        LookupService retrievedLookupService = registry.getService("TestLookupService", LookupService.class);
        assertNotNull(retrievedLookupService);
        assertEquals("TestLookupService", retrievedLookupService.getName());
    }

    @Test
    public void testRegisterServiceWithNullService() {
        // This should not throw an exception, but the service should not be registered
        assertThrows(NullPointerException.class, () -> {
            registry.registerService(null);
        });
    }

    @Test
    public void testRegisterServiceWithDuplicateName() {
        // Register a service
        registry.registerService(mockValidator);

        // Register another service with the same name
        MockValidator anotherValidator = new MockValidator("TestValidator");
        registry.registerService(anotherValidator);

        // Verify the service was overwritten
        Validator retrievedValidator = registry.getService("TestValidator", Validator.class);
        assertNotNull(retrievedValidator);
        assertSame(anotherValidator, retrievedValidator);
    }

    @Test
    public void testGetServiceWithNonExistentName() {
        // Get a service with a non-existent name
        Validator retrievedValidator = registry.getService("NonExistentService", Validator.class);
        assertNull(retrievedValidator);
    }

    @Test
    public void testGetServiceWithWrongType() {
        // Register a validator service
        registry.registerService(mockValidator);

        // Try to get it as an enricher
        Enricher retrievedEnricher = registry.getService("TestValidator", Enricher.class);
        assertNull(retrievedEnricher);
    }

    @Test
    public void testGetServiceWithNullName() {
        // Get a service with a null name
        Validator retrievedValidator = registry.getService(null, Validator.class);
        assertNull(retrievedValidator);
    }

    @Test
    public void testGetServiceWithNullType() {
        // Register a service
        registry.registerService(mockValidator);

        // Get a service with a null type
        assertThrows(NullPointerException.class, () -> {
            registry.getService("TestValidator", null);
        });
    }

    @Test
    public void testGetServiceWithCompatibleType() {
        // Register a lookup service that implements multiple interfaces
        registry.registerService(lookupService);

        // Get it as different types
        LookupService retrievedLookupService = registry.getService("TestLookupService", LookupService.class);
        assertNotNull(retrievedLookupService);
        assertEquals("TestLookupService", retrievedLookupService.getName());

        Validator retrievedValidator = registry.getService("TestLookupService", Validator.class);
        assertNotNull(retrievedValidator);
        assertEquals("TestLookupService", retrievedValidator.getName());

        Enricher retrievedEnricher = registry.getService("TestLookupService", Enricher.class);
        assertNotNull(retrievedEnricher);
        assertEquals("TestLookupService", retrievedEnricher.getName());

        Transformer retrievedTransformer = registry.getService("TestLookupService", Transformer.class);
        assertNotNull(retrievedTransformer);
        assertEquals("TestLookupService", retrievedTransformer.getName());

        IDataLookup retrievedDataLookup = registry.getService("TestLookupService", IDataLookup.class);
        assertNotNull(retrievedDataLookup);
        assertEquals("TestLookupService", retrievedDataLookup.getName());
    }

    /**
     * Mock implementation of Validator for testing.
     */
    private static class MockValidator implements Validator<Object> {
        private final String name;

        public MockValidator(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean validate(Object value) {
            return true;
        }

        @Override
        public Class<Object> getType() {
            return Object.class;
        }
    }

    /**
     * Mock implementation of Enricher for testing.
     */
    private static class MockEnricher implements Enricher<Object> {
        private final String name;

        public MockEnricher(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object enrich(Object value) {
            return value;
        }

        @Override
        public Class<Object> getType() {
            return Object.class;
        }
    }

    /**
     * Mock implementation of Transformer for testing.
     */
    private static class MockTransformer implements Transformer<Object> {
        private final String name;

        public MockTransformer(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object transform(Object value) {
            return value;
        }

        @Override
        public Class<Object> getType() {
            return Object.class;
        }
    }
}
