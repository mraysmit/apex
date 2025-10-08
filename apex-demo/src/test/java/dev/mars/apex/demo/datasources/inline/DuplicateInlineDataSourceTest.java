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
package dev.mars.apex.demo.datasources.inline;

import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.core.cache.ApexCacheManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Duplicate Inline Data Source Demo
 *
 * This example demonstrates a potential issue where multiple enrichments
 * using the SAME inline dataset may cause the data to be loaded multiple
 * times into memory instead of being shared.
 * 
 * What this demonstrates:
 * - Two enrichments using identical inline data
 * - Each enrichment creates its own DatasetLookupService instance
 * - Same data gets loaded twice into memory (potential inefficiency)
 * - Different field mappings from the same lookup result
 * 
 * Expected behavior:
 * - Both enrichments should work correctly
 * - Data should ideally be shared between enrichments
 * - Memory usage should be optimized for duplicate datasets
 * 
 * Current behavior (potential issue):
 * - Each enrichment loads its own copy of the data
 * - Memory usage is duplicated for identical datasets
 * - No sharing mechanism between enrichments
 */
@DisplayName("Duplicate Inline Data Source Demo")
public class DuplicateInlineDataSourceTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(DuplicateInlineDataSourceTest.class);

    @Test
    @DisplayName("Should demonstrate duplicate loading of same inline data")
    void testDuplicateInlineDataSource() {
        logger.info("=================================================================");
        logger.info("TESTING DUPLICATE INLINE DATA SOURCE LOADING");
        logger.info("=================================================================");
        logger.info("This test demonstrates potential memory duplication when");
        logger.info("multiple enrichments use the same inline dataset.");
        logger.info("=================================================================");

        try {
            // Load YAML configuration with 2 enrichments using SAME inline data
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/inline/DuplicateInlineDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            logger.info("📋 Loaded configuration with {} enrichments", config.getEnrichments().size());
            
            // Log enrichment details
            config.getEnrichments().forEach(enrichment -> {
                logger.info("  - Enrichment: {} (type: {})", enrichment.getId(), enrichment.getType());
                if (enrichment.getLookupConfig() != null && enrichment.getLookupConfig().getLookupDataset() != null) {
                    var dataset = enrichment.getLookupConfig().getLookupDataset();
                    logger.info("    Dataset type: {}, Records: {}", 
                        dataset.getType(), 
                        dataset.getData() != null ? dataset.getData().size() : 0);
                }
            });

            // Create test data with currency code
            Map<String, Object> testData = new HashMap<>();
            testData.put("currencyCode", "USD");
            testData.put("transactionId", "TXN123");
            
            logger.info(" Input data: {}", testData);
            logger.info(" Processing enrichments...");

            // Process with APEX - this should trigger both enrichments
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.info("✅ Enrichment completed successfully");
            logger.info(" Result data: {}", enrichedData);

            // Verify both enrichments worked (different fields from same data)
            assertEquals("USD", enrichedData.get("currencyCode"));
            assertEquals("US Dollar", enrichedData.get("currencyName"));      // From first enrichment
            assertEquals("$", enrichedData.get("currencySymbol"));            // From second enrichment  
            assertEquals("North America", enrichedData.get("currencyRegion")); // From second enrichment
            assertEquals("TXN123", enrichedData.get("transactionId"));

            logger.info("=================================================================");
            logger.info("ANALYSIS: POTENTIAL MEMORY DUPLICATION ISSUE");
            logger.info("=================================================================");
            logger.info("✅ Both enrichments successfully processed the same data");
            logger.info(" Each enrichment likely created its own DatasetLookupService");
            logger.info(" Same inline data (4 currency records) loaded twice in memory");
            logger.info(" Optimization opportunity: Share datasets between enrichments");
            logger.info("=================================================================");

        } catch (Exception e) {
            logger.error("❌ Test failed: {}", e.getMessage(), e);
            fail("Duplicate inline data source test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should work with different currency codes")
    void testDifferentCurrencyCodes() {
        logger.info("Testing different currency codes with duplicate datasets...");

        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/inline/DuplicateInlineDataSourceTest.yaml");

            // Test EUR
            Map<String, Object> testData = new HashMap<>();
            testData.put("currencyCode", "EUR");

            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            assertEquals("EUR", enrichedData.get("currencyCode"));
            assertEquals("Euro", enrichedData.get("currencyName"));
            assertEquals("€", enrichedData.get("currencySymbol"));
            assertEquals("Europe", enrichedData.get("currencyRegion"));

            logger.info("✅ EUR enrichment successful: {}", enrichedData);

        } catch (Exception e) {
            fail("Different currency test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should verify dataset deduplication via cache statistics")
    void testDatasetDeduplicationWithCacheStatistics() {
        logger.info("=================================================================");
        logger.info("VERIFYING DATASET DEDUPLICATION VIA CACHE STATISTICS");
        logger.info("=================================================================");

        try {
            // Get cache manager instance
            ApexCacheManager cacheManager = ApexCacheManager.getInstance();

            // Clear cache to start fresh
            cacheManager.clearAll();
            logger.info("📊 Cache cleared - starting fresh");

            // Load configuration with 2 enrichments using SAME inline data
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/inline/DuplicateInlineDataSourceTest.yaml");

            // Create test data
            Map<String, Object> testData = new HashMap<>();
            testData.put("currencyCode", "USD");
            testData.put("transactionId", "TXN123");

            logger.info("📋 Processing enrichments with 2 identical datasets...");

            // Process enrichments - this should create 1 dataset service and reuse it
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");

            // Get cache statistics
            var stats = cacheManager.getStatistics(ApexCacheManager.DATASET_CACHE);
            assertNotNull(stats, "Dataset cache statistics should not be null");

            logger.info("=================================================================");
            logger.info("CACHE STATISTICS ANALYSIS");
            logger.info("=================================================================");
            logger.info("📊 Dataset Cache Statistics:");
            logger.info("   - Cache Hits: {}", stats.getHits());
            logger.info("   - Cache Misses: {}", stats.getMisses());
            logger.info("   - Hit Rate: {}%", String.format("%.2f", stats.getHitRate()));
            logger.info("   - Total Requests: {}", stats.getRequestCount());
            logger.info("=================================================================");

            // Verify deduplication: 1 miss (first enrichment) + 1 hit (second enrichment)
            assertEquals(1, stats.getMisses(),
                "Should have exactly 1 cache miss (first enrichment creates dataset)");
            assertEquals(1, stats.getHits(),
                "Should have exactly 1 cache hit (second enrichment reuses dataset)");
            assertEquals(2, stats.getRequestCount(),
                "Should have exactly 2 total requests (2 enrichments)");
            assertEquals(50.0, stats.getHitRate(), 0.01,
                "Hit rate should be 50% (1 hit out of 2 accesses)");

            logger.info("✅ VERIFICATION SUCCESSFUL:");
            logger.info("   ✓ Only 1 DatasetLookupService created (not 2)");
            logger.info("   ✓ Second enrichment reused first enrichment's dataset");
            logger.info("   ✓ Memory duplication eliminated via caching");
            logger.info("   ✓ 50% memory savings achieved");
            logger.info("=================================================================");

            // Verify enrichment results are still correct
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertEquals("USD", enrichedData.get("currencyCode"));
            assertEquals("US Dollar", enrichedData.get("currencyName"));
            assertEquals("$", enrichedData.get("currencySymbol"));
            assertEquals("North America", enrichedData.get("currencyRegion"));

        } catch (Exception e) {
            logger.error("❌ Cache statistics verification failed: {}", e.getMessage(), e);
            fail("Dataset deduplication verification failed: " + e.getMessage());
        }
    }
}
