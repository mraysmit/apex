package com.rulesengine.core.service.lookup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for LookupService.
 */
public class LookupServiceTest {
    private LookupService lookupService;
    private List<String> lookupValues;

    @BeforeEach
    public void setUp() {
        lookupValues = Arrays.asList("value1", "value2", "value3");
        lookupService = new LookupService("TestLookupService", lookupValues);
    }

    @Test
    public void testConstructor() {
        assertEquals("TestLookupService", lookupService.getName());
        assertEquals(lookupValues, lookupService.getLookupValues());
    }

    @Test
    public void testGetName() {
        assertEquals("TestLookupService", lookupService.getName());
    }

    @Test
    public void testGetLookupValues() {
        List<String> retrievedValues = lookupService.getLookupValues();
        assertNotNull(retrievedValues);
        assertEquals(3, retrievedValues.size());
        assertTrue(retrievedValues.contains("value1"));
        assertTrue(retrievedValues.contains("value2"));
        assertTrue(retrievedValues.contains("value3"));
    }

    @Test
    public void testContainsValue() {
        assertTrue(lookupService.containsValue("value1"));
        assertTrue(lookupService.containsValue("value2"));
        assertTrue(lookupService.containsValue("value3"));
        assertFalse(lookupService.containsValue("nonExistentValue"));
        assertFalse(lookupService.containsValue(null));
    }

    @Test
    public void testValidate() {
        assertTrue(lookupService.validate("value1"));
        assertTrue(lookupService.validate("value2"));
        assertTrue(lookupService.validate("value3"));
        assertFalse(lookupService.validate("nonExistentValue"));
        assertFalse(lookupService.validate(null));
        
        // Test with non-string value
        assertFalse(lookupService.validate(123));
        assertFalse(lookupService.validate(new Object()));
    }

    @Test
    public void testEnrichWithNoEnrichmentData() {
        // Test with no enrichment data set
        assertEquals("value1", lookupService.enrich("value1"));
        assertEquals("value2", lookupService.enrich("value2"));
        assertEquals("nonExistentValue", lookupService.enrich("nonExistentValue"));
        assertNull(lookupService.enrich(null));
    }

    @Test
    public void testEnrichWithEnrichmentData() {
        // Set up enrichment data
        Map<String, Object> enrichmentData = new HashMap<>();
        enrichmentData.put("value1", "enriched1");
        enrichmentData.put("value2", 123);
        enrichmentData.put("value3", Arrays.asList("a", "b", "c"));
        lookupService.setEnrichmentData(enrichmentData);
        
        // Test enrichment
        assertEquals("enriched1", lookupService.enrich("value1"));
        assertEquals(123, lookupService.enrich("value2"));
        assertEquals(Arrays.asList("a", "b", "c"), lookupService.enrich("value3"));
        assertEquals("nonExistentValue", lookupService.enrich("nonExistentValue"));
        assertNull(lookupService.enrich(null));
        
        // Test with non-string value
        assertEquals(123, lookupService.enrich(123));
        Object obj = new Object();
        assertSame(obj, lookupService.enrich(obj));
    }

    @Test
    public void testTransformWithDefaultFunction() {
        // Test with default identity transformation function
        assertEquals("value1", lookupService.transform("value1"));
        assertEquals(123, lookupService.transform(123));
        assertNull(lookupService.transform(null));
        
        List<String> list = Arrays.asList("a", "b", "c");
        assertSame(list, lookupService.transform(list));
    }

    @Test
    public void testTransformWithCustomFunction() {
        // Set up a custom transformation function that converts to uppercase for strings
        Function<Object, Object> toUpperCase = value -> {
            if (value instanceof String) {
                return ((String) value).toUpperCase();
            }
            return value;
        };
        lookupService.setTransformationFunction(toUpperCase);
        
        // Test transformation
        assertEquals("VALUE1", lookupService.transform("value1"));
        assertEquals("NONEXISTENTVALUE", lookupService.transform("nonExistentValue"));
        assertEquals(123, lookupService.transform(123));
        assertNull(lookupService.transform(null));
    }

    @Test
    public void testSetEnrichmentData() {
        // Set up enrichment data
        Map<String, Object> enrichmentData = new HashMap<>();
        enrichmentData.put("value1", "enriched1");
        lookupService.setEnrichmentData(enrichmentData);
        
        // Test enrichment
        assertEquals("enriched1", lookupService.enrich("value1"));
        
        // Update enrichment data
        Map<String, Object> updatedEnrichmentData = new HashMap<>();
        updatedEnrichmentData.put("value1", "updatedEnriched1");
        updatedEnrichmentData.put("value2", "enriched2");
        lookupService.setEnrichmentData(updatedEnrichmentData);
        
        // Test updated enrichment
        assertEquals("updatedEnriched1", lookupService.enrich("value1"));
        assertEquals("enriched2", lookupService.enrich("value2"));
    }

    @Test
    public void testSetTransformationFunction() {
        // Set up a transformation function that doubles integers
        Function<Object, Object> doubleInteger = value -> {
            if (value instanceof Integer) {
                return (Integer) value * 2;
            }
            return value;
        };
        lookupService.setTransformationFunction(doubleInteger);
        
        // Test transformation
        assertEquals(246, lookupService.transform(123));
        assertEquals("value1", lookupService.transform("value1"));
        
        // Update transformation function to triple integers
        Function<Object, Object> tripleInteger = value -> {
            if (value instanceof Integer) {
                return (Integer) value * 3;
            }
            return value;
        };
        lookupService.setTransformationFunction(tripleInteger);
        
        // Test updated transformation
        assertEquals(369, lookupService.transform(123));
        assertEquals("value1", lookupService.transform("value1"));
    }

    @Test
    public void testIntegrationOfValidateEnrichTransform() {
        // Set up enrichment data
        Map<String, Object> enrichmentData = new HashMap<>();
        enrichmentData.put("value1", "enriched1");
        lookupService.setEnrichmentData(enrichmentData);
        
        // Set up a transformation function that appends " - transformed" to strings
        Function<Object, Object> appendTransformed = value -> {
            if (value instanceof String) {
                return value + " - transformed";
            }
            return value;
        };
        lookupService.setTransformationFunction(appendTransformed);
        
        // Test the full pipeline: validate -> enrich -> transform
        
        // For a valid value with enrichment data
        assertTrue(lookupService.validate("value1"));
        Object enriched = lookupService.enrich("value1");
        assertEquals("enriched1", enriched);
        Object transformed = lookupService.transform(enriched);
        assertEquals("enriched1 - transformed", transformed);
        
        // For a valid value without enrichment data
        assertTrue(lookupService.validate("value2"));
        Object notEnriched = lookupService.enrich("value2");
        assertEquals("value2", notEnriched);
        Object transformedNotEnriched = lookupService.transform(notEnriched);
        assertEquals("value2 - transformed", transformedNotEnriched);
        
        // For an invalid value
        assertFalse(lookupService.validate("nonExistentValue"));
        // Even though it's invalid, we can still try to enrich and transform it
        Object invalidEnriched = lookupService.enrich("nonExistentValue");
        assertEquals("nonExistentValue", invalidEnriched);
        Object transformedInvalid = lookupService.transform(invalidEnriched);
        assertEquals("nonExistentValue - transformed", transformedInvalid);
    }
}