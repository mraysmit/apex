package com.rulesengine.core.service;

import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import com.rulesengine.core.service.transform.TransformationService;
import com.rulesengine.core.service.transform.Transformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for TransformationService.
 */
public class TransformationServiceTest {

    private TransformationService transformationService;
    private LookupServiceRegistry registry;
    private MockTransformer mockTransformer;

    @BeforeEach
    public void setUp() {
        registry = new LookupServiceRegistry();
        mockTransformer = new MockTransformer("testTransformer");
        registry.registerService(mockTransformer);
        transformationService = new TransformationService(registry);
    }

    @Test
    public void testTransformWithExistingTransformer() {
        // Set up the mock transformer to return a transformed value
        mockTransformer.setTransformedValue("transformedValue");
        
        // Test the transformation
        Object result = transformationService.transform("testTransformer", "originalValue");
        
        // Verify the result
        assertEquals("transformedValue", result);
        
        // Verify the transformer was called with the correct value
        assertEquals("originalValue", mockTransformer.getLastTransformedValue());
    }

    @Test
    public void testTransformWithNonExistentTransformer() {
        // Test with a transformer that doesn't exist
        Object originalValue = "originalValue";
        Object result = transformationService.transform("nonExistentTransformer", originalValue);
        
        // Verify the original value is returned
        assertSame(originalValue, result);
        
        // Verify the mock transformer wasn't called
        assertNull(mockTransformer.getLastTransformedValue());
    }

    @Test
    public void testTransformWithNullValue() {
        // Set up the mock transformer to return a transformed value
        mockTransformer.setTransformedValue("transformedValue");
        
        // Test with a null value
        Object result = transformationService.transform("testTransformer", null);
        
        // Verify the result
        assertEquals("transformedValue", result);
        
        // Verify the transformer was called with null
        assertNull(mockTransformer.getLastTransformedValue());
    }

    /**
     * Mock implementation of Transformer for testing.
     */
    private static class MockTransformer implements Transformer {
        private final String name;
        private Object transformedValue;
        private Object lastTransformedValue;

        public MockTransformer(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object transform(Object value) {
            lastTransformedValue = value;
            return transformedValue;
        }

        public void setTransformedValue(Object transformedValue) {
            this.transformedValue = transformedValue;
        }

        public Object getLastTransformedValue() {
            return lastTransformedValue;
        }
    }
}