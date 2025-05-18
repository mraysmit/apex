package com.rulesengine.core.service.transform;

import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for EnrichmentService.
 */
public class EnrichmentServiceTest {
    private EnrichmentService enrichmentService;
    private MockLookupServiceRegistry registry;
    private MockEnricher mockEnricher;

    @BeforeEach
    public void setUp() {
        registry = new MockLookupServiceRegistry();
        enrichmentService = new EnrichmentService(registry);
        mockEnricher = new MockEnricher("TestEnricher");
        registry.registerService(mockEnricher);
    }

    @Test
    public void testEnrichWithExistingEnricher() {
        // Configure the mock enricher
        mockEnricher.setEnrichmentResult("enrichedValue");

        // Test enrichment
        Object result = enrichmentService.enrich("TestEnricher", "testValue");

        // Verify the result
        assertEquals("enrichedValue", result);

        // Verify the enricher was called with the correct value
        assertEquals("testValue", mockEnricher.getLastEnrichedValue());
    }

    @Test
    public void testEnrichWithNonExistentEnricher() {
        // Test enrichment with a non-existent enricher
        Object originalValue = "testValue";
        Object result = enrichmentService.enrich("NonExistentEnricher", originalValue);

        // Verify the original value is returned
        assertSame(originalValue, result);

        // Verify the mock enricher wasn't called
        assertNull(mockEnricher.getLastEnrichedValue());
    }

    @Test
    public void testEnrichWithNullEnricherName() {
        // Test enrichment with a null enricher name
        Object originalValue = "testValue";
        Object result = enrichmentService.enrich(null, originalValue);

        // Verify the original value is returned
        assertSame(originalValue, result);

        // Verify the mock enricher wasn't called
        assertNull(mockEnricher.getLastEnrichedValue());
    }

    @Test
    public void testEnrichWithNullValue() {
        // Configure the mock enricher
        mockEnricher.setEnrichmentResult("enrichedValue");

        // Test enrichment with a null value
        Object result = enrichmentService.enrich("TestEnricher", null);

        // Verify the result
        assertEquals("enrichedValue", result);

        // Verify the enricher was called with null
        assertNull(mockEnricher.getLastEnrichedValue());
    }

    @Test
    public void testEnrichWithDifferentValueTypes() {
        // Test with integer value
        mockEnricher.setEnrichmentResult(456);
        Object intResult = enrichmentService.enrich("TestEnricher", 123);
        assertEquals(456, intResult);
        assertEquals(123, mockEnricher.getLastEnrichedValue());

        // Test with boolean value
        mockEnricher.setEnrichmentResult(false);
        Object boolResult = enrichmentService.enrich("TestEnricher", true);
        assertEquals(false, boolResult);
        assertEquals(true, mockEnricher.getLastEnrichedValue());

        // Test with object value
        Object originalObject = new Object();
        Object enrichedObject = new Object();
        mockEnricher.setEnrichmentResult(enrichedObject);
        Object objResult = enrichmentService.enrich("TestEnricher", originalObject);
        assertSame(enrichedObject, objResult);
        assertSame(originalObject, mockEnricher.getLastEnrichedValue());
    }

    /**
     * Mock implementation of LookupServiceRegistry for testing.
     */
    private static class MockLookupServiceRegistry extends LookupServiceRegistry {
        private Enricher enricher;

        public void registerService(Enricher enricher) {
            this.enricher = enricher;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends com.rulesengine.core.service.NamedService> T getService(String name, Class<T> type) {
            if (enricher != null && enricher.getName().equals(name) && type.isInstance(enricher)) {
                return (T) enricher;
            }
            return null;
        }
    }

    /**
     * Mock implementation of Enricher for testing.
     */
    private static class MockEnricher implements Enricher {
        private final String name;
        private Object enrichmentResult;
        private Object lastEnrichedValue;

        public MockEnricher(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object enrich(Object value) {
            lastEnrichedValue = value;
            return enrichmentResult;
        }

        public void setEnrichmentResult(Object enrichmentResult) {
            this.enrichmentResult = enrichmentResult;
        }

        public Object getLastEnrichedValue() {
            return lastEnrichedValue;
        }
    }
}
