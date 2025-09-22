package dev.mars.apex.core.service.enrichment;

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


import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for EnrichmentService.
 * 
 * Tests cover:
 * - Service initialization and dependencies
 * - YAML-based enrichment processing
 * - Object enrichment with different configurations
 * - Integration with YamlEnrichmentProcessor
 * - Error handling and edge cases
 * - Multiple enrichment scenarios
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class EnrichmentServiceTest {

    private LookupServiceRegistry serviceRegistry;
    private ExpressionEvaluatorService evaluatorService;
    private EnrichmentService enrichmentService;

    @BeforeEach
    void setUp() {
        serviceRegistry = new LookupServiceRegistry();
        evaluatorService = new ExpressionEvaluatorService();
        enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);
    }

    // ========================================
    // Constructor and Initialization Tests
    // ========================================

    @Test
    @DisplayName("Should create EnrichmentService with valid dependencies")
    void testConstructor() {
        EnrichmentService service = new EnrichmentService(serviceRegistry, evaluatorService);
        
        assertNotNull(service, "Service should be created successfully");
        assertSame(serviceRegistry, service.getServiceRegistry(), "Service registry should be set correctly");
        assertNotNull(service.getProcessor(), "Processor should be initialized");
    }

    @Test
    @DisplayName("Should handle null service registry gracefully")
    void testConstructorWithNullRegistry() {
        assertDoesNotThrow(() -> {
            EnrichmentService service = new EnrichmentService(null, evaluatorService);
            assertNotNull(service);
        });
    }

    @Test
    @DisplayName("Should handle null evaluator service gracefully")
    void testConstructorWithNullEvaluator() {
        assertDoesNotThrow(() -> {
            EnrichmentService service = new EnrichmentService(serviceRegistry, null);
            assertNotNull(service);
        });
    }

    // ========================================
    // YAML Configuration Enrichment Tests
    // ========================================

    @Test
    @DisplayName("Should enrich object using YAML configuration")
    void testEnrichObjectWithYamlConfig() {
        YamlRuleConfiguration yamlConfig = createTestYamlConfiguration();
        TestDataObject targetObject = new TestDataObject("USD", 1000.0);
        
        Object enrichedObject = enrichmentService.enrichObject(yamlConfig, targetObject);
        
        assertNotNull(enrichedObject, "Enriched object should not be null");
        // The exact enrichment behavior depends on the YAML configuration and processor implementation
    }

    @Test
    @DisplayName("Should handle null YAML configuration gracefully")
    void testEnrichObjectWithNullYamlConfig() {
        TestDataObject targetObject = new TestDataObject("USD", 1000.0);
        
        Object result = enrichmentService.enrichObject((YamlRuleConfiguration) null, targetObject);
        
        assertSame(targetObject, result, "Null YAML config should return original object");
    }

    @Test
    @DisplayName("Should handle YAML configuration with no enrichments")
    void testEnrichObjectWithEmptyYamlConfig() {
        YamlRuleConfiguration emptyConfig = new YamlRuleConfiguration();
        TestDataObject targetObject = new TestDataObject("USD", 1000.0);
        
        Object result = enrichmentService.enrichObject(emptyConfig, targetObject);
        
        assertSame(targetObject, result, "Empty YAML config should return original object");
    }

    // ========================================
    // List-based Enrichment Tests
    // ========================================

    @Test
    @DisplayName("Should enrich object using list of enrichments")
    void testEnrichObjectWithEnrichmentList() {
        List<YamlEnrichment> enrichments = createTestEnrichmentList();
        TestDataObject targetObject = new TestDataObject("EUR", 500.0);
        
        Object enrichedObject = enrichmentService.enrichObject(enrichments, targetObject);
        
        assertNotNull(enrichedObject, "Enriched object should not be null");
    }

    @Test
    @DisplayName("Should handle empty enrichment list")
    void testEnrichObjectWithEmptyList() {
        List<YamlEnrichment> emptyList = new ArrayList<>();
        TestDataObject targetObject = new TestDataObject("GBP", 750.0);
        
        Object result = enrichmentService.enrichObject(emptyList, targetObject);
        
        assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("Should handle null enrichment list")
    void testEnrichObjectWithNullList() {
        TestDataObject targetObject = new TestDataObject("JPY", 100000.0);
        
        Object result = enrichmentService.enrichObject((List<YamlEnrichment>) null, targetObject);
        
        assertNotNull(result, "Result should not be null");
    }

    // ========================================
    // Single Enrichment Tests
    // ========================================

    @Test
    @DisplayName("Should enrich object using single enrichment")
    void testEnrichObjectWithSingleEnrichment() {
        YamlEnrichment enrichment = createTestEnrichment();
        TestDataObject targetObject = new TestDataObject("CAD", 800.0);
        
        Object enrichedObject = enrichmentService.enrichObject(enrichment, targetObject);
        
        assertNotNull(enrichedObject, "Enriched object should not be null");
    }

    @Test
    @DisplayName("Should handle null single enrichment")
    void testEnrichObjectWithNullEnrichment() {
        System.out.println("TEST: Triggering intentional error - testing enrichment with null enrichment configuration");

        TestDataObject targetObject = new TestDataObject("AUD", 900.0);

        assertThrows(NullPointerException.class, () -> {
            enrichmentService.enrichObject((YamlEnrichment) null, targetObject);
        }, "Null enrichment should throw NullPointerException");
    }

    // ========================================
    // Target Object Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle null target object")
    void testEnrichNullTargetObject() {
        System.out.println("TEST: Triggering intentional error - testing enrichment with null target object");

        YamlEnrichment enrichment = createTestEnrichment();

        assertThrows(NullPointerException.class, () -> {
            enrichmentService.enrichObject(enrichment, null);
        }, "Null target object should throw NullPointerException");
    }

    @Test
    @DisplayName("Should handle different target object types")
    void testEnrichDifferentObjectTypes() {
        YamlEnrichment enrichment = createTestEnrichment();
        
        // Test with Map
        Map<String, Object> mapObject = new HashMap<>();
        mapObject.put("currency", "USD");
        mapObject.put("amount", 1000.0);
        
        Object enrichedMap = enrichmentService.enrichObject(enrichment, mapObject);
        assertNotNull(enrichedMap, "Map enrichment should work");
        
        // Test with String
        String stringObject = "test";
        Object enrichedString = enrichmentService.enrichObject(enrichment, stringObject);
        assertNotNull(enrichedString, "String enrichment should work");
    }

    // ========================================
    // Integration Tests
    // ========================================

    @Test
    @DisplayName("Should integrate with service registry for enrichment")
    void testEnrichmentWithServiceRegistry() {
        // Register a lookup service in the registry
        // (This would typically be done in a real scenario)
        
        YamlEnrichment enrichment = createTestEnrichment();
        TestDataObject targetObject = new TestDataObject("CHF", 1200.0);
        
        Object result = enrichmentService.enrichObject(enrichment, targetObject);
        
        assertNotNull(result, "Enrichment with service registry should work");
    }

    @Test
    @DisplayName("Should handle multiple enrichments in sequence")
    void testMultipleEnrichmentsInSequence() {
        List<YamlEnrichment> enrichments = createMultipleTestEnrichments();
        TestDataObject targetObject = new TestDataObject("SEK", 600.0);
        
        Object result = enrichmentService.enrichObject(enrichments, targetObject);
        
        assertNotNull(result, "Multiple enrichments should be processed");
    }

    // ========================================
    // RuleResult Enhanced Tests (Phase 4 Demonstration)
    // ========================================

    @Test
    @DisplayName("Should enrich object using YAML configuration with RuleResult")
    void testEnrichObjectWithYamlConfig_WithRuleResult() {
        YamlRuleConfiguration yamlConfig = createTestYamlConfiguration();
        TestDataObject targetObject = new TestDataObject("USD", 1000.0);

        // Test with RuleResult for comprehensive validation
        RuleResult result = enrichmentService.enrichObjectWithResult(yamlConfig, targetObject);

        assertNotNull(result, "RuleResult should not be null");
        assertTrue(result.isSuccess(), "Enrichment should succeed");
        assertFalse(result.hasFailures(), "Should have no failures");
        assertTrue(result.getFailureMessages().isEmpty(), "Should have no failure messages");
        assertNotNull(result.getEnrichedData(), "Should have enriched data");

        // Verify enriched data contains expected content
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertFalse(enrichedData.isEmpty(), "Enriched data should not be empty");
    }

    @Test
    @DisplayName("Should handle null YAML configuration gracefully with RuleResult")
    void testEnrichObjectWithNullYamlConfig_WithRuleResult() {
        TestDataObject targetObject = new TestDataObject("USD", 1000.0);

        RuleResult result = enrichmentService.enrichObjectWithResult((YamlRuleConfiguration) null, targetObject);

        assertNotNull(result, "RuleResult should not be null");
        assertTrue(result.isSuccess(), "Null YAML config should succeed (returns original object)");
        assertFalse(result.hasFailures(), "Should have no failures");
        assertNotNull(result.getEnrichedData(), "Should have enriched data (original object)");
    }

    @Test
    @DisplayName("Should enrich object using list of enrichments with RuleResult")
    void testEnrichObjectWithEnrichmentList_WithRuleResult() {
        List<YamlEnrichment> enrichments = createTestEnrichmentList();
        TestDataObject targetObject = new TestDataObject("EUR", 500.0);

        RuleResult result = enrichmentService.enrichObjectWithResult(enrichments, targetObject);

        assertNotNull(result, "RuleResult should not be null");
        assertTrue(result.isSuccess(), "List enrichment should succeed");
        assertFalse(result.hasFailures(), "Should have no failures");
        assertNotNull(result.getEnrichedData(), "Should have enriched data");

        // Verify the enriched data is accessible
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data map should not be null");
    }

    @Test
    @DisplayName("Should handle empty enrichment list with RuleResult")
    void testEnrichObjectWithEmptyList_WithRuleResult() {
        List<YamlEnrichment> emptyList = new ArrayList<>();
        TestDataObject targetObject = new TestDataObject("GBP", 750.0);

        RuleResult result = enrichmentService.enrichObjectWithResult(emptyList, targetObject);

        assertNotNull(result, "RuleResult should not be null");
        assertTrue(result.isSuccess(), "Empty list should succeed");
        assertFalse(result.hasFailures(), "Should have no failures");
        assertNotNull(result.getEnrichedData(), "Should have enriched data");
    }

    @Test
    @DisplayName("Should handle null enrichment list with RuleResult")
    void testEnrichObjectWithNullList_WithRuleResult() {
        TestDataObject targetObject = new TestDataObject("JPY", 100000.0);

        RuleResult result = enrichmentService.enrichObjectWithResult((List<YamlEnrichment>) null, targetObject);

        assertNotNull(result, "RuleResult should not be null");
        assertTrue(result.isSuccess(), "Null list should succeed");
        assertFalse(result.hasFailures(), "Should have no failures");
        assertNotNull(result.getEnrichedData(), "Should have enriched data");
    }

    @Test
    @DisplayName("Should enrich object using single enrichment with RuleResult")
    void testEnrichObjectWithSingleEnrichment_WithRuleResult() {
        YamlEnrichment enrichment = createTestEnrichment();
        TestDataObject targetObject = new TestDataObject("CAD", 800.0);

        RuleResult result = enrichmentService.enrichObjectWithResult(enrichment, targetObject);

        assertNotNull(result, "RuleResult should not be null");
        assertTrue(result.isSuccess(), "Single enrichment should succeed");
        assertFalse(result.hasFailures(), "Should have no failures");
        assertNotNull(result.getEnrichedData(), "Should have enriched data");

        // Verify defensive copying - modifying returned data shouldn't affect internal state
        Map<String, Object> enrichedData = result.getEnrichedData();
        int originalSize = enrichedData.size();
        enrichedData.put("testModification", "testValue");

        // Get result again to verify internal state wasn't modified
        RuleResult secondResult = enrichmentService.enrichObjectWithResult(enrichment, targetObject);
        assertEquals(originalSize, secondResult.getEnrichedData().size(),
                    "Internal state should not be affected by external modifications");
    }

    @Test
    @DisplayName("Should handle different target object types with RuleResult")
    void testEnrichDifferentObjectTypes_WithRuleResult() {
        YamlEnrichment enrichment = createTestEnrichment();

        // Test with Map
        Map<String, Object> mapObject = new HashMap<>();
        mapObject.put("currency", "USD");
        mapObject.put("amount", 1000.0);

        RuleResult mapResult = enrichmentService.enrichObjectWithResult(enrichment, mapObject);
        assertNotNull(mapResult, "Map RuleResult should not be null");
        assertTrue(mapResult.isSuccess(), "Map enrichment should succeed");
        assertNotNull(mapResult.getEnrichedData(), "Map should have enriched data");

        // Test with String
        String stringObject = "test";
        RuleResult stringResult = enrichmentService.enrichObjectWithResult(enrichment, stringObject);
        assertNotNull(stringResult, "String RuleResult should not be null");
        assertTrue(stringResult.isSuccess(), "String enrichment should succeed");
        assertNotNull(stringResult.getEnrichedData(), "String should have enriched data");
    }

    @Test
    @DisplayName("Should integrate with service registry for enrichment with RuleResult")
    void testEnrichmentWithServiceRegistry_WithRuleResult() {
        // Register a lookup service in the registry
        // (This would typically be done in a real scenario)

        YamlEnrichment enrichment = createTestEnrichment();
        TestDataObject targetObject = new TestDataObject("CHF", 1200.0);

        RuleResult result = enrichmentService.enrichObjectWithResult(enrichment, targetObject);

        assertNotNull(result, "RuleResult should not be null");
        assertTrue(result.isSuccess(), "Service registry enrichment should succeed");
        assertFalse(result.hasFailures(), "Should have no failures");
        assertNotNull(result.getEnrichedData(), "Should have enriched data");
    }

    @Test
    @DisplayName("Should handle multiple enrichments in sequence with RuleResult")
    void testMultipleEnrichmentsInSequence_WithRuleResult() {
        List<YamlEnrichment> enrichments = createMultipleTestEnrichments();
        TestDataObject targetObject = new TestDataObject("SEK", 600.0);

        RuleResult result = enrichmentService.enrichObjectWithResult(enrichments, targetObject);

        assertNotNull(result, "RuleResult should not be null");
        assertTrue(result.isSuccess(), "Multiple enrichments should succeed");
        assertFalse(result.hasFailures(), "Should have no failures");
        assertNotNull(result.getEnrichedData(), "Should have enriched data");

        // Verify that multiple enrichments were processed
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertFalse(enrichedData.isEmpty(), "Multiple enrichments should produce data");
    }

    @Test
    @DisplayName("Should demonstrate failure detection with RuleResult")
    void testFailureDetectionWithRuleResult() {
        // Create a configuration that might cause enrichment failures
        YamlRuleConfiguration yamlConfig = createTestYamlConfigurationWithRequiredFields();
        TestDataObject targetObject = new TestDataObject("USD", 1000.0);

        RuleResult result = enrichmentService.enrichObjectWithResult(yamlConfig, targetObject);

        assertNotNull(result, "RuleResult should not be null");

        // Test both success and failure scenarios
        if (result.hasFailures()) {
            assertFalse(result.isSuccess(), "Should not be successful if has failures");
            assertFalse(result.getFailureMessages().isEmpty(), "Should have failure messages");

            // Verify failure messages are informative
            List<String> failures = result.getFailureMessages();
            for (String failure : failures) {
                assertNotNull(failure, "Failure message should not be null");
                assertFalse(failure.trim().isEmpty(), "Failure message should not be empty");
            }
        } else {
            assertTrue(result.isSuccess(), "Should be successful if no failures");
            assertTrue(result.getFailureMessages().isEmpty(), "Should have no failure messages");
        }

        // Enriched data should always be available (even on failures)
        assertNotNull(result.getEnrichedData(), "Should always have enriched data");
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle enrichment processing errors gracefully")
    void testEnrichmentProcessingErrors() {
        System.out.println("TEST: Triggering intentional error - testing enrichment with invalid configuration");

        YamlEnrichment invalidEnrichment = createInvalidEnrichment();
        TestDataObject targetObject = new TestDataObject("NOK", 700.0);

        assertDoesNotThrow(() -> {
            Object result = enrichmentService.enrichObject(invalidEnrichment, targetObject);
            assertNotNull(result, "Should handle errors gracefully");
        });
    }

    @Test
    @DisplayName("Should handle enrichment processing errors gracefully with RuleResult")
    void testEnrichmentProcessingErrors_WithRuleResult() {
        System.out.println("TEST: Triggering intentional error - testing enrichment with invalid configuration using RuleResult");

        YamlEnrichment invalidEnrichment = createInvalidEnrichment();
        TestDataObject targetObject = new TestDataObject("NOK", 700.0);

        assertDoesNotThrow(() -> {
            RuleResult result = enrichmentService.enrichObjectWithResult(invalidEnrichment, targetObject);
            assertNotNull(result, "RuleResult should not be null even on errors");

            // With RuleResult, we can detect and analyze errors
            if (result.hasFailures()) {
                assertFalse(result.isSuccess(), "Should not be successful on errors");
                assertFalse(result.getFailureMessages().isEmpty(), "Should have error messages");
                System.out.println("Detected enrichment failures: " + result.getFailureMessages());
            }

            // Enriched data should still be available (original or partial)
            assertNotNull(result.getEnrichedData(), "Should have enriched data even on errors");
        });
    }

    // ========================================
    // Test Helper Methods
    // ========================================

    /**
     * Creates a test YAML configuration with enrichments.
     */
    private YamlRuleConfiguration createTestYamlConfiguration() {
        YamlRuleConfiguration config = new YamlRuleConfiguration();

        List<YamlEnrichment> enrichments = createTestEnrichmentList();
        config.setEnrichments(enrichments);

        return config;
    }

    /**
     * Creates a test YAML rule configuration with required fields for failure testing.
     */
    private YamlRuleConfiguration createTestYamlConfigurationWithRequiredFields() {
        YamlRuleConfiguration config = new YamlRuleConfiguration();

        // Set metadata
        YamlRuleConfiguration.ConfigurationMetadata metadata = new YamlRuleConfiguration.ConfigurationMetadata();
        metadata.setName("testConfigurationWithRequiredFields");
        metadata.setDescription("Test configuration with required fields for failure detection");
        config.setMetadata(metadata);

        List<YamlEnrichment> enrichments = createTestEnrichmentListWithRequiredFields();
        config.setEnrichments(enrichments);

        return config;
    }

    /**
     * Creates a list of test enrichments.
     */
    private List<YamlEnrichment> createTestEnrichmentList() {
        List<YamlEnrichment> enrichments = new ArrayList<>();
        enrichments.add(createTestEnrichment());
        return enrichments;
    }

    /**
     * Creates a list of test enrichments with required fields for failure testing.
     */
    private List<YamlEnrichment> createTestEnrichmentListWithRequiredFields() {
        List<YamlEnrichment> enrichments = new ArrayList<>();
        enrichments.add(createTestEnrichmentWithRequiredField());
        return enrichments;
    }

    /**
     * Creates multiple test enrichments.
     */
    private List<YamlEnrichment> createMultipleTestEnrichments() {
        List<YamlEnrichment> enrichments = new ArrayList<>();
        enrichments.add(createTestEnrichment());
        enrichments.add(createSecondTestEnrichment());
        return enrichments;
    }

    /**
     * Creates a test enrichment configuration.
     */
    private YamlEnrichment createTestEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setName("currencyEnrichment");
        enrichment.setType("lookup");
        
        // Set up basic enrichment configuration
        Map<String, Object> config = new HashMap<>();
        config.put("sourceField", "currency");
        config.put("targetField", "currencyName");
        
        return enrichment;
    }

    /**
     * Creates a second test enrichment configuration.
     */
    private YamlEnrichment createSecondTestEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setName("amountEnrichment");
        enrichment.setType("calculation");
        
        return enrichment;
    }

    /**
     * Creates a test enrichment with required field for failure testing.
     */
    private YamlEnrichment createTestEnrichmentWithRequiredField() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setName("requiredFieldEnrichment");
        enrichment.setType("lookup");

        // Set up enrichment configuration with required field
        Map<String, Object> config = new HashMap<>();
        config.put("sourceField", "currency");
        config.put("targetField", "requiredCurrencyName");
        config.put("required", true); // This field is required

        return enrichment;
    }

    /**
     * Creates an invalid enrichment configuration for error testing.
     */
    private YamlEnrichment createInvalidEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setName("invalidEnrichment");
        enrichment.setType("nonExistentType");

        return enrichment;
    }

    /**
     * Test data object for enrichment testing.
     */
    private static class TestDataObject {
        private String currency;
        private Double amount;
        private String currencyName; // Will be enriched
        private String region; // Will be enriched

        public TestDataObject(String currency, Double amount) {
            this.currency = currency;
            this.amount = amount;
        }

        // Getters and setters
        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public String getCurrencyName() {
            return currencyName;
        }

        public void setCurrencyName(String currencyName) {
            this.currencyName = currencyName;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }
}
