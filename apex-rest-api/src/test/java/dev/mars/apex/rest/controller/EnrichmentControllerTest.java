package dev.mars.apex.rest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit tests for EnrichmentController without mocking frameworks.
 * Tests basic object creation and structure validation.
 */
class EnrichmentControllerTest {

    private EnrichmentController enrichmentController;
    private Map<String, Object> testTargetObject;

    @BeforeEach
    void setUp() {
        // Create controller - basic instantiation test
        enrichmentController = new EnrichmentController();

        testTargetObject = new HashMap<>();
        testTargetObject.put("customerId", "CUST001");
        testTargetObject.put("transactionAmount", 1500.0);
    }

    @Test
    @DisplayName("Should create controller successfully")
    void shouldCreateControllerSuccessfully() {
        // Test basic controller creation
        assertNotNull(enrichmentController);
    }

    @Test
    @DisplayName("Should create enrichment request DTO successfully")
    void shouldCreateEnrichmentRequestSuccessfully() {
        // Test DTO creation and basic functionality
        EnrichmentController.EnrichmentRequest request = new EnrichmentController.EnrichmentRequest();
        assertNotNull(request);
        
        // Test setters and getters
        String yamlConfig = "test: config";
        request.setYamlConfiguration(yamlConfig);
        request.setTargetObject(testTargetObject);
        
        assertEquals(yamlConfig, request.getYamlConfiguration());
        assertEquals(testTargetObject, request.getTargetObject());
    }

    @Test
    @DisplayName("Should create batch enrichment request DTO successfully")
    void shouldCreateBatchEnrichmentRequestSuccessfully() {
        // Test batch DTO creation
        EnrichmentController.BatchEnrichmentRequest request = new EnrichmentController.BatchEnrichmentRequest();
        assertNotNull(request);
        
        // Test basic functionality
        String yamlConfig = "test: config";
        request.setYamlConfiguration(yamlConfig);
        
        assertEquals(yamlConfig, request.getYamlConfiguration());
    }

    @Test
    @DisplayName("Should validate test data structure")
    void shouldValidateTestDataStructure() {
        // Validate our test data is properly structured
        assertNotNull(testTargetObject);
        assertEquals("CUST001", testTargetObject.get("customerId"));
        assertEquals(1500.0, testTargetObject.get("transactionAmount"));
        assertEquals(2, testTargetObject.size());
    }
}
