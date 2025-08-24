package dev.mars.apex.playground.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ExampleService
 */
@DisplayName("Example Service Tests")
class ExampleServiceTest {

    private ExampleService exampleService;

    @BeforeEach
    void setUp() {
        exampleService = new ExampleService();
    }

    @Test
    @DisplayName("Should get all examples successfully")
    void shouldGetAllExamplesSuccessfully() {
        // When
        Map<String, Object> examples = exampleService.getAllExamples();
        
        // Then
        assertNotNull(examples);
        assertTrue(examples.size() > 0);
        
        // Should have expected categories
        assertTrue(examples.containsKey("quickstart"));
        assertTrue(examples.containsKey("financial"));
        assertTrue(examples.containsKey("validation"));
        assertTrue(examples.containsKey("lookup"));
        assertTrue(examples.containsKey("advanced"));
    }

    @Test
    @DisplayName("Should get specific example successfully")
    void shouldGetSpecificExampleSuccessfully() {
        // When
        Map<String, Object> example = exampleService.getExample("quickstart", "basic-validation");
        
        // Then
        assertNotNull(example);
        assertEquals("basic-validation", example.get("name"));
        assertEquals("quickstart", example.get("category"));
        assertNotNull(example.get("sampleData"));
    }

    @Test
    @DisplayName("Should handle non-existent example gracefully")
    void shouldHandleNonExistentExampleGracefully() {
        // When
        Map<String, Object> example = exampleService.getExample("nonexistent", "example");
        
        // Then
        assertNotNull(example);
        assertTrue(example.containsKey("error"));
    }

    @Test
    @DisplayName("Should provide sample data for different categories")
    void shouldProvideSampleDataForDifferentCategories() {
        // Test financial example
        Map<String, Object> financialExample = exampleService.getExample("financial", "trade-validation");
        assertNotNull(financialExample.get("sampleData"));
        
        // Test validation example
        Map<String, Object> validationExample = exampleService.getExample("validation", "data-validation");
        assertNotNull(validationExample.get("sampleData"));
        
        // Test lookup example
        Map<String, Object> lookupExample = exampleService.getExample("lookup", "simple-lookup");
        assertNotNull(lookupExample.get("sampleData"));
    }
}
