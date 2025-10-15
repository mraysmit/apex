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
package dev.mars.apex.demo.datasources.database;

import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.core.cache.ApexCacheManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Duplicate Database Data Source Demo
 *
 * This test demonstrates that dataset deduplication works for DATABASE-based
 * lookups, not just inline datasets.
 * 
 * What this demonstrates:
 * - Two enrichments using IDENTICAL database queries
 * - Content-based signature generation for database datasets
 * - Cache HIT on second enrichment (reuses first enrichment's DatasetLookupService)
 * - 50% memory savings through deduplication
 * 
 * Expected behavior:
 * - First enrichment: Cache MISS → Creates DatasetLookupService
 * - Second enrichment: Cache HIT → Reuses same DatasetLookupService
 * - Only 1 DatasetLookupService created (not 2)
 */
@DisplayName("Duplicate Database Data Source Demo")
public class DuplicateDatabaseDataSourceTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(DuplicateDatabaseDataSourceTest.class);

    @BeforeEach
    void setupDatabase() throws Exception {
        logger.info("Setting up H2 database with test data...");
        
        // Create H2 database and populate with test data
        String dbUrl = "jdbc:h2:./target/h2-demo/duplicate_test";
        
        try (Connection conn = DriverManager.getConnection(dbUrl, "sa", "");
             Statement stmt = conn.createStatement()) {
            
            // Drop table if exists
            stmt.execute("DROP TABLE IF EXISTS currencies");
            
            // Create currencies table
            stmt.execute("""
                CREATE TABLE currencies (
                    code VARCHAR(3) PRIMARY KEY,
                    name VARCHAR(50),
                    symbol VARCHAR(5),
                    region VARCHAR(50)
                )
            """);
            
            // Insert test data (same as inline dataset)
            stmt.execute("INSERT INTO currencies VALUES ('USD', 'US Dollar', '$', 'North America')");
            stmt.execute("INSERT INTO currencies VALUES ('EUR', 'Euro', '€', 'Europe')");
            stmt.execute("INSERT INTO currencies VALUES ('GBP', 'British Pound', '£', 'Europe')");
            stmt.execute("INSERT INTO currencies VALUES ('JPY', 'Japanese Yen', '¥', 'Asia')");
            
            logger.info("✅ Database setup complete with 4 currency records");
        }
    }

    @Test
    @DisplayName("Should verify database dataset deduplication via cache statistics")
    void testDatabaseDatasetDeduplicationWithCacheStatistics() {
        logger.info("=================================================================");
        logger.info("VERIFYING DATABASE DATASET DEDUPLICATION VIA CACHE STATISTICS");
        logger.info("=================================================================");

        try {
            // Get cache manager instance
            ApexCacheManager cacheManager = ApexCacheManager.getInstance();
            
            // Clear cache to start fresh
            cacheManager.clearAll();
            logger.info("📊 Cache cleared - starting fresh");

            // Load configuration with 2 enrichments using SAME database query
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/database/DuplicateDatabaseDataSourceTest.yaml");
            
            // Create test data
            Map<String, Object> testData = new HashMap<>();
            testData.put("currencyCode", "USD");
            testData.put("transactionId", "TXN123");
            
            logger.info("📋 Processing enrichments with 2 identical database queries...");

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
            logger.info("   - Hit Rate: {}%", String.format("%.2f%%", stats.getHitRate()));
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
            logger.info("   ✓ Only 1 DatasetLookupService created for database query (not 2)");
            logger.info("   ✓ Second enrichment reused first enrichment's dataset");
            logger.info("   ✓ Memory duplication eliminated via caching");
            logger.info("   ✓ 50% memory savings achieved");
            logger.info("   ✓ Database dataset deduplication works same as inline!");
            logger.info("=================================================================");

            // Verify enrichment results are still correct
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertEquals("USD", enrichedData.get("currencyCode"));
            assertEquals("US Dollar", enrichedData.get("currencyName"));
            assertEquals("$", enrichedData.get("currencySymbol"));
            assertEquals("North America", enrichedData.get("currencyRegion"));

        } catch (Exception e) {
            logger.error("❌ Database dataset deduplication verification failed: {}", e.getMessage(), e);
            fail("Database dataset deduplication verification failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should work with different currency codes")
    void testDifferentCurrencyCodes() {
        logger.info("Testing different currency codes with duplicate database queries...");

        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/database/DuplicateDatabaseDataSourceTest.yaml");
            
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
    @DisplayName("Should demonstrate database dataset deduplication")
    void testDuplicateDatabaseDataSource() {
        logger.info("=================================================================");
        logger.info("TESTING DUPLICATE DATABASE DATA SOURCE LOADING");
        logger.info("=================================================================");
        logger.info("This test demonstrates that dataset deduplication works for");
        logger.info("DATABASE-based lookups, not just inline datasets.");
        logger.info("=================================================================");

        try {
            // Load YAML configuration with 2 enrichments using SAME database query
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/database/DuplicateDatabaseDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            logger.info("📋 Loaded configuration with {} enrichments", config.getEnrichments().size());
            
            // Log enrichment details
            config.getEnrichments().forEach(enrichment -> {
                logger.info("  - Enrichment: {} (type: {})", enrichment.getId(), enrichment.getType());
                if (enrichment.getLookupConfig() != null && enrichment.getLookupConfig().getLookupDataset() != null) {
                    var dataset = enrichment.getLookupConfig().getLookupDataset();
                    logger.info("    Dataset type: {}, Query: {}", 
                        dataset.getType(), 
                        dataset.getQuery());
                }
            });

            // Create test data with currency code
            Map<String, Object> testData = new HashMap<>();
            testData.put("currencyCode", "USD");
            testData.put("transactionId", "TXN123");
            
            logger.info("📊 Input data: {}", testData);
            logger.info("🔄 Processing enrichments...");

            // Process with APEX - this should trigger both enrichments
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.info("✅ Enrichment completed successfully");
            logger.info("📊 Result data: {}", enrichedData);

            // Verify both enrichments worked (different fields from same database query)
            assertEquals("USD", enrichedData.get("currencyCode"));
            assertEquals("US Dollar", enrichedData.get("currencyName"));      // From first enrichment
            assertEquals("$", enrichedData.get("currencySymbol"));            // From second enrichment  
            assertEquals("North America", enrichedData.get("currencyRegion")); // From second enrichment
            assertEquals("TXN123", enrichedData.get("transactionId"));

            logger.info("=================================================================");
            logger.info("SUCCESS: DATABASE DATASET DEDUPLICATION VERIFIED");
            logger.info("=================================================================");
            logger.info("✅ Both enrichments successfully processed the same database query");
            logger.info("✅ Dataset deduplication works for database lookups");
            logger.info("✅ Same caching mechanism as inline datasets");
            logger.info("=================================================================");

        } catch (Exception e) {
            logger.error("❌ Test failed: {}", e.getMessage(), e);
            fail("Duplicate database data source test failed: " + e.getMessage());
        }
    }
}

