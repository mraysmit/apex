package dev.mars.apex.core.engine.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

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

/**
 * Test class for the new RuleResult API methods added in Phase 1.
 * Tests the comprehensive evaluation result functionality.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-22
 * @version 1.0
 */
@DisplayName("RuleResult Extension API Tests")
class RuleResultExtensionTest {

    @Test
    @DisplayName("Test backward compatibility - existing constructors work with new fields")
    void testBackwardCompatibility() {
        // Test basic constructor
        RuleResult result1 = new RuleResult("test-rule", "Test message", true, RuleResult.ResultType.MATCH);
        
        assertNotNull(result1);
        assertEquals("test-rule", result1.getRuleName());
        assertEquals("Test message", result1.getMessage());
        assertTrue(result1.isTriggered());
        assertEquals(RuleResult.ResultType.MATCH, result1.getResultType());
        
        // Test new API methods with defaults
        assertTrue(result1.isSuccess(), "Should default to success for MATCH result");
        assertFalse(result1.hasFailures(), "Should not have failures by default");
        assertTrue(result1.getFailureMessages().isEmpty(), "Should have empty failure messages");
        assertTrue(result1.getEnrichedData().isEmpty(), "Should have empty enriched data");
    }

    @Test
    @DisplayName("Test enrichment success factory method")
    void testEnrichmentSuccess() {
        Map<String, Object> enrichedData = new HashMap<>();
        enrichedData.put("customerName", "John Doe");
        enrichedData.put("creditScore", 750);
        
        RuleResult result = RuleResult.enrichmentSuccess(enrichedData);
        
        assertNotNull(result);
        assertEquals("enrichment", result.getRuleName());
        assertEquals("Enrichment completed successfully", result.getMessage());
        assertTrue(result.isTriggered());
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType());
        
        // Test new API methods
        assertTrue(result.isSuccess(), "Enrichment success should be successful");
        assertFalse(result.hasFailures(), "Enrichment success should not have failures");
        assertTrue(result.getFailureMessages().isEmpty(), "Should have no failure messages");
        
        Map<String, Object> returnedData = result.getEnrichedData();
        assertEquals("John Doe", returnedData.get("customerName"));
        assertEquals(750, returnedData.get("creditScore"));
    }

    @Test
    @DisplayName("Test enrichment failure factory method")
    void testEnrichmentFailure() {
        List<String> failureMessages = Arrays.asList(
            "Required field 'name' is missing from lookup result",
            "Database connection failed"
        );
        
        Map<String, Object> partialData = new HashMap<>();
        partialData.put("id", "123");
        
        RuleResult result = RuleResult.enrichmentFailure(failureMessages, partialData);
        
        assertNotNull(result);
        assertEquals("enrichment", result.getRuleName());
        assertEquals("Enrichment failed", result.getMessage());
        assertFalse(result.isTriggered());
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType());
        
        // Test new API methods
        assertFalse(result.isSuccess(), "Enrichment failure should not be successful");
        assertTrue(result.hasFailures(), "Enrichment failure should have failures");
        
        List<String> returnedMessages = result.getFailureMessages();
        assertEquals(2, returnedMessages.size());
        assertTrue(returnedMessages.contains("Required field 'name' is missing from lookup result"));
        assertTrue(returnedMessages.contains("Database connection failed"));
        
        Map<String, Object> returnedData = result.getEnrichedData();
        assertEquals("123", returnedData.get("id"));
    }

    @Test
    @DisplayName("Test evaluation success factory method")
    void testEvaluationSuccess() {
        Map<String, Object> enrichedData = new HashMap<>();
        enrichedData.put("statusName", "Active");
        enrichedData.put("canTransact", true);
        
        RuleResult result = RuleResult.evaluationSuccess(enrichedData, "final-rule", "All validations passed");
        
        assertNotNull(result);
        assertEquals("final-rule", result.getRuleName());
        assertEquals("All validations passed", result.getMessage());
        assertTrue(result.isTriggered());
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType());
        
        // Test new API methods
        assertTrue(result.isSuccess(), "Evaluation success should be successful");
        assertFalse(result.hasFailures(), "Evaluation success should not have failures");
        assertTrue(result.getFailureMessages().isEmpty(), "Should have no failure messages");
        
        Map<String, Object> returnedData = result.getEnrichedData();
        assertEquals("Active", returnedData.get("statusName"));
        assertEquals(true, returnedData.get("canTransact"));
    }

    @Test
    @DisplayName("Test evaluation failure factory method")
    void testEvaluationFailure() {
        List<String> failureMessages = Arrays.asList(
            "Age validation failed: must be >= 18",
            "Email validation failed: invalid format"
        );
        
        Map<String, Object> enrichedData = new HashMap<>();
        enrichedData.put("age", 16);
        enrichedData.put("email", "invalid-email");
        
        RuleResult result = RuleResult.evaluationFailure(failureMessages, enrichedData, 
                                                        "validation-rule", "Validation failed");
        
        assertNotNull(result);
        assertEquals("validation-rule", result.getRuleName());
        assertEquals("Validation failed", result.getMessage());
        assertFalse(result.isTriggered());
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType());
        
        // Test new API methods
        assertFalse(result.isSuccess(), "Evaluation failure should not be successful");
        assertTrue(result.hasFailures(), "Evaluation failure should have failures");
        
        List<String> returnedMessages = result.getFailureMessages();
        assertEquals(2, returnedMessages.size());
        assertTrue(returnedMessages.contains("Age validation failed: must be >= 18"));
        assertTrue(returnedMessages.contains("Email validation failed: invalid format"));
        
        Map<String, Object> returnedData = result.getEnrichedData();
        assertEquals(16, returnedData.get("age"));
        assertEquals("invalid-email", returnedData.get("email"));
    }

    @Test
    @DisplayName("Test comprehensive constructor")
    void testComprehensiveConstructor() {
        Map<String, Object> enrichedData = new HashMap<>();
        enrichedData.put("result", "processed");
        
        List<String> failureMessages = Arrays.asList("Warning: partial processing");
        
        RuleResult result = new RuleResult("test-rule", "Test completed", true, 
                                          RuleResult.ResultType.MATCH, null, 
                                          enrichedData, failureMessages, true);
        
        assertNotNull(result);
        assertEquals("test-rule", result.getRuleName());
        assertEquals("Test completed", result.getMessage());
        assertTrue(result.isTriggered());
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType());
        
        // Test new API methods
        assertTrue(result.isSuccess(), "Should be successful as specified");
        assertTrue(result.hasFailures(), "Should have failures due to failure messages");
        
        List<String> returnedMessages = result.getFailureMessages();
        assertEquals(1, returnedMessages.size());
        assertEquals("Warning: partial processing", returnedMessages.get(0));
        
        Map<String, Object> returnedData = result.getEnrichedData();
        assertEquals("processed", returnedData.get("result"));
    }

    @Test
    @DisplayName("Test data immutability - returned collections are defensive copies")
    void testDataImmutability() {
        Map<String, Object> originalData = new HashMap<>();
        originalData.put("key", "value");
        
        List<String> originalMessages = new ArrayList<>();
        originalMessages.add("original message");
        
        RuleResult result = RuleResult.enrichmentFailure(originalMessages, originalData);
        
        // Get the returned collections
        Map<String, Object> returnedData = result.getEnrichedData();
        List<String> returnedMessages = result.getFailureMessages();
        
        // Modify the returned collections
        returnedData.put("newKey", "newValue");
        returnedMessages.add("new message");
        
        // Verify original result is unchanged
        Map<String, Object> freshData = result.getEnrichedData();
        List<String> freshMessages = result.getFailureMessages();
        
        assertFalse(freshData.containsKey("newKey"), "Original enriched data should be unchanged");
        assertEquals(1, freshMessages.size(), "Original failure messages should be unchanged");
        assertEquals("original message", freshMessages.get(0));
    }
}
