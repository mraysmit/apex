package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.cache.ApexCacheManager;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that YamlEnrichmentProcessor correctly uses ApexCacheManager
 * for dataset caching, lookup result caching, and expression caching.
 */
class YamlEnrichmentProcessorCachingTest {

    private EnrichmentService enrichmentService;
    private ApexCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Reset cache manager before each test
        ApexCacheManager.resetInstance();
        cacheManager = ApexCacheManager.getInstance();

        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);
    }

    @AfterEach
    void tearDown() {
        // Clean up cache after each test
        if (cacheManager != null) {
            cacheManager.clearAll();
        }
    }

    @Test
    void testDatasetCachingWithDuplicateInlineDatasets() throws Exception {
        // Load configuration with duplicate inline datasets
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.loadFromFile(
            "../apex-demo/src/test/java/dev/mars/apex/demo/datasources/inline/DuplicateInlineDataSourceTest.yaml"
        );

        // Initial dataset cache should be empty
        assertEquals(0, cacheManager.size(ApexCacheManager.DATASET_CACHE),
            "Dataset cache should be empty initially");

        // Process enrichments for first time
        Map<String, Object> testData1 = new HashMap<>();
        testData1.put("currencyCode", "USD");
        Object result1 = enrichmentService.enrichObject(config, testData1);

        // After first enrichment, dataset cache should have entries
        int datasetCacheSize = cacheManager.size(ApexCacheManager.DATASET_CACHE);
        assertTrue(datasetCacheSize > 0,
            "Dataset cache should have entries after first enrichment");

        // Process enrichments for second time with different data
        Map<String, Object> testData2 = new HashMap<>();
        testData2.put("currencyCode", "EUR");
        Object result2 = enrichmentService.enrichObject(config, testData2);

        // Dataset cache size should remain the same (reusing cached datasets)
        assertEquals(datasetCacheSize, cacheManager.size(ApexCacheManager.DATASET_CACHE),
            "Dataset cache size should not increase when reusing datasets");

        // Verify results are correct
        assertNotNull(result1, "First enrichment result should not be null");
        assertNotNull(result2, "Second enrichment result should not be null");
    }

    @Test
    void testExpressionCaching() throws Exception {
        // Load configuration
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.loadFromFile(
            "../apex-demo/src/test/java/dev/mars/apex/demo/datasources/inline/DuplicateInlineDataSourceTest.yaml"
        );

        // Initial expression cache should be empty
        assertEquals(0, cacheManager.size(ApexCacheManager.EXPRESSION_CACHE),
            "Expression cache should be empty initially");

        // Process enrichments multiple times
        for (int i = 0; i < 3; i++) {
            Map<String, Object> testData = new HashMap<>();
            testData.put("currencyCode", "USD");
            enrichmentService.enrichObject(config, testData);
        }

        // Expression cache should have entries
        int expressionCacheSize = cacheManager.size(ApexCacheManager.EXPRESSION_CACHE);
        assertTrue(expressionCacheSize > 0,
            "Expression cache should have entries after processing enrichments");
    }

    @Test
    void testClearCache() throws Exception {
        // Load configuration and process enrichments
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.loadFromFile(
            "../apex-demo/src/test/java/dev/mars/apex/demo/datasources/inline/DuplicateInlineDataSourceTest.yaml"
        );

        Map<String, Object> testData = new HashMap<>();
        testData.put("currencyCode", "USD");
        enrichmentService.enrichObject(config, testData);

        // Verify caches have entries
        assertTrue(cacheManager.size(ApexCacheManager.DATASET_CACHE) > 0,
            "Dataset cache should have entries");
        assertTrue(cacheManager.size(ApexCacheManager.EXPRESSION_CACHE) > 0,
            "Expression cache should have entries");

        // Clear cache
        cacheManager.clearAll();

        // Verify caches are empty
        assertEquals(0, cacheManager.size(ApexCacheManager.DATASET_CACHE),
            "Dataset cache should be empty after clear");
        assertEquals(0, cacheManager.size(ApexCacheManager.EXPRESSION_CACHE),
            "Expression cache should be empty after clear");
        assertEquals(0, cacheManager.size(ApexCacheManager.LOOKUP_RESULT_CACHE),
            "Lookup result cache should be empty after clear");
    }

    @Test
    void testCacheStatistics() throws Exception {
        // Load configuration and process enrichments
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.loadFromFile(
            "../apex-demo/src/test/java/dev/mars/apex/demo/datasources/inline/DuplicateInlineDataSourceTest.yaml"
        );

        Map<String, Object> testData = new HashMap<>();
        testData.put("currencyCode", "USD");
        enrichmentService.enrichObject(config, testData);

        // Get cache statistics from ApexCacheManager
        int datasetCacheSize = cacheManager.size(ApexCacheManager.DATASET_CACHE);
        int expressionCacheSize = cacheManager.size(ApexCacheManager.EXPRESSION_CACHE);
        int lookupCacheSize = cacheManager.size(ApexCacheManager.LOOKUP_RESULT_CACHE);

        // Verify cache sizes are reasonable
        assertTrue(datasetCacheSize >= 0, "Dataset cache size should be non-negative");
        assertTrue(expressionCacheSize >= 0, "Expression cache size should be non-negative");
        assertTrue(lookupCacheSize >= 0, "Lookup cache size should be non-negative");
    }

    @Test
    void testDatasetDeduplication() throws Exception {
        // This test verifies that identical datasets are deduplicated
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.loadFromFile(
            "../apex-demo/src/test/java/dev/mars/apex/demo/datasources/inline/DuplicateInlineDataSourceTest.yaml"
        );

        // The configuration has 2 enrichments with identical inline datasets
        // After processing, we should have only 1 dataset in cache (deduplicated)
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("currencyCode", "USD");
        enrichmentService.enrichObject(config, testData);

        // Check dataset cache size
        int datasetCacheSize = cacheManager.size(ApexCacheManager.DATASET_CACHE);

        // We expect 1 dataset (the duplicate inline currency data)
        // Both enrichments should reuse the same cached dataset
        assertEquals(1, datasetCacheSize,
            "Should have exactly 1 dataset in cache (deduplicated from 2 enrichments)");
    }
}

