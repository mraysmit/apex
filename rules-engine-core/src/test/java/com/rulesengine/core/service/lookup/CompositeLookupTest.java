package com.rulesengine.core.service.lookup;

import com.rulesengine.core.service.transform.Enricher;
import com.rulesengine.core.service.transform.Transformer;
import com.rulesengine.core.service.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CompositeLookup.
 */
public class CompositeLookupTest {
    private CompositeLookup compositeLookup;
    private MockValidator mockValidator;
    private MockEnricher mockEnricher;
    private MockTransformer mockTransformer;

    @BeforeEach
    public void setUp() {
        compositeLookup = new CompositeLookup("TestCompositeLookup");
        mockValidator = new MockValidator("TestValidator");
        mockEnricher = new MockEnricher("TestEnricher");
        mockTransformer = new MockTransformer("TestTransformer");
    }

    @Test
    public void testConstructor() {
        assertEquals("TestCompositeLookup", compositeLookup.getName());
    }

    @Test
    public void testGetName() {
        assertEquals("TestCompositeLookup", compositeLookup.getName());
    }

    @Test
    public void testValidateWithNoValidator() {
        // Test validate with no validator set
        assertTrue(compositeLookup.validate("testValue"));
        assertTrue(compositeLookup.validate(null));
        assertTrue(compositeLookup.validate(123));
    }

    @Test
    public void testValidateWithValidator() {
        // Configure the mock validator
        mockValidator.setValidationResult(true);
        
        // Set the validator
        compositeLookup.withValidator(mockValidator);
        
        // Test validate with validator set
        assertTrue(compositeLookup.validate("testValue"));
        
        // Verify the validator was called with the correct value
        assertEquals("testValue", mockValidator.getLastValidatedValue());
        
        // Configure the mock validator to return false
        mockValidator.setValidationResult(false);
        
        // Test validate with validator set
        assertFalse(compositeLookup.validate("testValue"));
        
        // Verify the validator was called with the correct value
        assertEquals("testValue", mockValidator.getLastValidatedValue());
    }

    @Test
    public void testEnrichWithNoEnricher() {
        // Test enrich with no enricher set
        assertEquals("testValue", compositeLookup.enrich("testValue"));
        assertNull(compositeLookup.enrich(null));
        assertEquals(123, compositeLookup.enrich(123));
    }

    @Test
    public void testEnrichWithEnricher() {
        // Configure the mock enricher
        mockEnricher.setEnrichmentResult("enrichedValue");
        
        // Set the enricher
        compositeLookup.withEnricher(mockEnricher);
        
        // Test enrich with enricher set
        assertEquals("enrichedValue", compositeLookup.enrich("testValue"));
        
        // Verify the enricher was called with the correct value
        assertEquals("testValue", mockEnricher.getLastEnrichedValue());
        
        // Configure the mock enricher to return a different value
        mockEnricher.setEnrichmentResult(456);
        
        // Test enrich with enricher set
        assertEquals(456, compositeLookup.enrich("testValue"));
        
        // Verify the enricher was called with the correct value
        assertEquals("testValue", mockEnricher.getLastEnrichedValue());
    }

    @Test
    public void testTransformWithNoTransformer() {
        // Test transform with no transformer set
        assertEquals("testValue", compositeLookup.transform("testValue"));
        assertNull(compositeLookup.transform(null));
        assertEquals(123, compositeLookup.transform(123));
    }

    @Test
    public void testTransformWithTransformer() {
        // Configure the mock transformer
        mockTransformer.setTransformationResult("transformedValue");
        
        // Set the transformer
        compositeLookup.withTransformer(mockTransformer);
        
        // Test transform with transformer set
        assertEquals("transformedValue", compositeLookup.transform("testValue"));
        
        // Verify the transformer was called with the correct value
        assertEquals("testValue", mockTransformer.getLastTransformedValue());
        
        // Configure the mock transformer to return a different value
        mockTransformer.setTransformationResult(789);
        
        // Test transform with transformer set
        assertEquals(789, compositeLookup.transform("testValue"));
        
        // Verify the transformer was called with the correct value
        assertEquals("testValue", mockTransformer.getLastTransformedValue());
    }

    @Test
    public void testFluentInterface() {
        // Test the fluent interface
        CompositeLookup result = compositeLookup
            .withValidator(mockValidator)
            .withEnricher(mockEnricher)
            .withTransformer(mockTransformer);
        
        // Verify the result is the same instance
        assertSame(compositeLookup, result);
        
        // Verify the components were set
        mockValidator.setValidationResult(true);
        assertTrue(compositeLookup.validate("testValue"));
        assertEquals("testValue", mockValidator.getLastValidatedValue());
        
        mockEnricher.setEnrichmentResult("enrichedValue");
        assertEquals("enrichedValue", compositeLookup.enrich("testValue"));
        assertEquals("testValue", mockEnricher.getLastEnrichedValue());
        
        mockTransformer.setTransformationResult("transformedValue");
        assertEquals("transformedValue", compositeLookup.transform("testValue"));
        assertEquals("testValue", mockTransformer.getLastTransformedValue());
    }

    @Test
    public void testIntegrationOfValidateEnrichTransform() {
        // Configure the mock components
        mockValidator.setValidationResult(true);
        mockEnricher.setEnrichmentResult("enrichedValue");
        mockTransformer.setTransformationResult("transformedValue");
        
        // Set the components
        compositeLookup
            .withValidator(mockValidator)
            .withEnricher(mockEnricher)
            .withTransformer(mockTransformer);
        
        // Test the full pipeline: validate -> enrich -> transform
        
        // For a valid value
        assertTrue(compositeLookup.validate("testValue"));
        assertEquals("testValue", mockValidator.getLastValidatedValue());
        
        Object enriched = compositeLookup.enrich("testValue");
        assertEquals("enrichedValue", enriched);
        assertEquals("testValue", mockEnricher.getLastEnrichedValue());
        
        Object transformed = compositeLookup.transform(enriched);
        assertEquals("transformedValue", transformed);
        assertEquals("enrichedValue", mockTransformer.getLastTransformedValue());
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

    /**
     * Mock implementation of Transformer for testing.
     */
    private static class MockTransformer implements Transformer {
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

        public void setTransformationResult(Object transformationResult) {
            this.transformationResult = transformationResult;
        }

        public Object getLastTransformedValue() {
            return lastTransformedValue;
        }
    }
}