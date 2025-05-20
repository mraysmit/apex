package com.rulesengine.core.service.lookup;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.service.transform.GenericTransformer;
import com.rulesengine.core.service.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for LookupServiceRegistry.
 */
public class LookupServiceRegistryTest {
    private LookupServiceRegistry registry;
    private MockValidator mockValidator;
    private MockTransformer mockTransformer1;
    private MockTransformer mockTransformer2;
    private LookupService lookupService;

    @BeforeEach
    public void setUp() {
        registry = new LookupServiceRegistry();
        mockValidator = new MockValidator("TestValidator");
        mockTransformer1 = new MockTransformer("TestTransformer1");
        mockTransformer2 = new MockTransformer("TestTransformer2");
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
        registry.registerService(mockTransformer1);
        registry.registerService(mockTransformer2);
        registry.registerService(lookupService);

        // Verify all services were registered
        Validator retrievedValidator = registry.getService("TestValidator", Validator.class);
        assertNotNull(retrievedValidator);
        assertEquals("TestValidator", retrievedValidator.getName());

        GenericTransformer<?> retrievedTransformer1 = registry.getService("TestTransformer1", GenericTransformer.class);
        assertNotNull(retrievedTransformer1);
        assertEquals("TestTransformer1", retrievedTransformer1.getName());

        GenericTransformer<?> retrievedTransformer2 = registry.getService("TestTransformer2", GenericTransformer.class);
        assertNotNull(retrievedTransformer2);
        assertEquals("TestTransformer2", retrievedTransformer2.getName());

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

        // Try to get it as a transformer
        GenericTransformer<?> retrievedTransformer = registry.getService("TestValidator", GenericTransformer.class);
        assertNull(retrievedTransformer);
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

        // LookupService no longer implements GenericTransformer
        GenericTransformer<?> retrievedTransformer = registry.getService("TestLookupService", GenericTransformer.class);
        assertNull(retrievedTransformer);

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
     * Mock implementation of GenericTransformer for testing.
     */
    private static class MockTransformer extends GenericTransformer<Object> {
        public MockTransformer(String name) {
            super(name, Object.class, new RulesEngine(new RulesEngineConfiguration()), new ArrayList<>());
        }

        @Override
        public Object transform(Object value) {
            return value;
        }
    }
}
