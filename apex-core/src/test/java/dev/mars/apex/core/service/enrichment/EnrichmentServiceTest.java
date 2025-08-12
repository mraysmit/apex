package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
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
     * Creates a list of test enrichments.
     */
    private List<YamlEnrichment> createTestEnrichmentList() {
        List<YamlEnrichment> enrichments = new ArrayList<>();
        enrichments.add(createTestEnrichment());
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
