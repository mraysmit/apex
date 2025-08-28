package dev.mars.apex.playground.service;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ExampleService
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
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
