package com.rulesengine.core.service.transform;

import com.rulesengine.core.service.common.NamedService;
import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for TransformationService.
 */
public class TransformationServiceTest {
    private TransformationService transformationService;
    private MockLookupServiceRegistry registry;
    private MockTransformer mockTransformer;

    @BeforeEach
    public void setUp() {
        registry = new MockLookupServiceRegistry();
        transformationService = new TransformationService(registry);
        mockTransformer = new MockTransformer("TestTransformer");
        registry.registerService(mockTransformer);
    }

    @Test
    public void testTransformWithExistingTransformer() {
        // Configure the mock transformer
        mockTransformer.setTransformationResult("transformedValue");

        // Test transformation
        Object result = transformationService.transform("TestTransformer", "testValue");

        // Verify the result
        assertEquals("transformedValue", result);

        // Verify the transformer was called with the correct value
        assertEquals("testValue", mockTransformer.getLastTransformedValue());
    }

    @Test
    public void testTransformWithNonExistentTransformer() {
        // Test transformation with a non-existent transformer
        Object originalValue = "testValue";
        Object result = transformationService.transform("NonExistentTransformer", originalValue);

        // Verify the original value is returned
        assertSame(originalValue, result);

        // Verify the mock transformer wasn't called
        assertNull(mockTransformer.getLastTransformedValue());
    }

    @Test
    public void testTransformWithNullTransformerName() {
        // Test transformation with a null transformer name
        Object originalValue = "testValue";
        Object result = transformationService.transform(null, originalValue);

        // Verify the original value is returned
        assertSame(originalValue, result);

        // Verify the mock transformer wasn't called
        assertNull(mockTransformer.getLastTransformedValue());
    }

    @Test
    public void testTransformWithNullValue() {
        // Configure the mock transformer
        mockTransformer.setTransformationResult("transformedValue");

        // Test transformation with a null value
        Object result = transformationService.transform("TestTransformer", null);

        // Verify the result
        assertEquals("transformedValue", result);

        // Verify the transformer was called with null
        assertNull(mockTransformer.getLastTransformedValue());
    }

    @Test
    public void testTransformWithDifferentValueTypes() {
        // Test with integer value
        mockTransformer.setTransformationResult(456);
        Object intResult = transformationService.transform("TestTransformer", 123);
        assertEquals(456, intResult);
        assertEquals(123, mockTransformer.getLastTransformedValue());

        // Test with boolean value
        mockTransformer.setTransformationResult(false);
        Object boolResult = transformationService.transform("TestTransformer", true);
        assertEquals(false, boolResult);
        assertEquals(true, mockTransformer.getLastTransformedValue());

        // Test with object value
        Object originalObject = new Object();
        Object transformedObject = new Object();
        mockTransformer.setTransformationResult(transformedObject);
        Object objResult = transformationService.transform("TestTransformer", originalObject);
        assertSame(transformedObject, objResult);
        assertSame(originalObject, mockTransformer.getLastTransformedValue());
    }

    /**
     * Mock implementation of LookupServiceRegistry for testing.
     */
    private static class MockLookupServiceRegistry extends LookupServiceRegistry {
        private Transformer<?> transformer;

        public void registerService(Transformer<?> transformer) {
            this.transformer = transformer;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends NamedService> T getService(String name, Class<T> type) {
            if (transformer != null && transformer.getName().equals(name) && type.isInstance(transformer)) {
                return (T) transformer;
            }
            return null;
        }
    }

    /**
     * Mock implementation of Transformer for testing.
     */
    private static class MockTransformer implements Transformer<Object> {
        private final String name;
        private Object transformationResult;
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
            return transformationResult;
        }

        @Override
        public Class<Object> getType() {
            return Object.class;
        }

        public void setTransformationResult(Object transformationResult) {
            this.transformationResult = transformationResult;
        }

        public Object getLastTransformedValue() {
            return lastTransformedValue;
        }
    }
}
