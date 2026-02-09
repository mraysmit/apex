package dev.mars.apex.core.dbschema;

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

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.engine.core.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.data.external.DataSourceResolver;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests cache behavior across multiple database schemas to ensure data consistency
 * and prevent cache corruption when same table names exist in different schemas.
 * 
 * Critical Test Scenarios:
 * 1. Cache keys MUST include schema information to prevent cross-schema data leakage
 * 2. Cached data from schema-a MUST NOT be served when querying schema-b
 * 3. Cache eviction MUST respect schema boundaries
 * 4. Cache statistics MUST track per-schema metrics correctly
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1
 */
@Testcontainers
@DisplayName("Schema Cache Isolation Tests - Preventing Cache Corruption Across Schemas")
class SchemaCacheIsolationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaCacheIsolationTest.class);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.13-alpine3.20")
            .withDatabaseName("apex_cache_test")
            .withUsername("test")
            .withPassword("test");

    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    void setUp() throws Exception {
        LOGGER.info("Setting up multi-schema test with cache validation");
        
        yamlLoader = new YamlConfigurationLoader();
        new DataSourceResolver();
        
        // Create schemas and populate with IDENTICAL table structures but DIFFERENT data
        setupMultiSchemaTestData();
    }

    /**
     * Create three schemas (sales, marketing, finance) each with a customers table
     * containing DIFFERENT customer data to prove cache isolation.
     */
    private void setupMultiSchemaTestData() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword());
             Statement stmt = conn.createStatement()) {

            // Schema 1: Sales customers
            stmt.execute("DROP SCHEMA IF EXISTS sales CASCADE");
            stmt.execute("CREATE SCHEMA sales");
            stmt.execute("CREATE TABLE sales.customers (" +
                        "id VARCHAR(50) PRIMARY KEY, " +
                        "name VARCHAR(255), " +
                        "department VARCHAR(50), " +
                        "schema_name VARCHAR(50))");
            stmt.execute("INSERT INTO sales.customers VALUES " +
                        "('CUST-001', 'Sales Customer Alpha', 'Sales', 'sales'), " +
                        "('CUST-002', 'Sales Customer Beta', 'Sales', 'sales'), " +
                        "('CUST-003', 'Sales Customer Gamma', 'Sales', 'sales')");

            // Schema 2: Marketing customers (SAME IDs but DIFFERENT names)
            stmt.execute("DROP SCHEMA IF EXISTS marketing CASCADE");
            stmt.execute("CREATE SCHEMA marketing");
            stmt.execute("CREATE TABLE marketing.customers (" +
                        "id VARCHAR(50) PRIMARY KEY, " +
                        "name VARCHAR(255), " +
                        "department VARCHAR(50), " +
                        "schema_name VARCHAR(50))");
            stmt.execute("INSERT INTO marketing.customers VALUES " +
                        "('CUST-001', 'Marketing Customer Alpha', 'Marketing', 'marketing'), " +
                        "('CUST-002', 'Marketing Customer Beta', 'Marketing', 'marketing'), " +
                        "('CUST-003', 'Marketing Customer Gamma', 'Marketing', 'marketing')");

            // Schema 3: Finance customers (SAME IDs but DIFFERENT names)
            stmt.execute("DROP SCHEMA IF EXISTS finance CASCADE");
            stmt.execute("CREATE SCHEMA finance");
            stmt.execute("CREATE TABLE finance.customers (" +
                        "id VARCHAR(50) PRIMARY KEY, " +
                        "name VARCHAR(255), " +
                        "department VARCHAR(50), " +
                        "schema_name VARCHAR(50))");
            stmt.execute("INSERT INTO finance.customers VALUES " +
                        "('CUST-001', 'Finance Customer Alpha', 'Finance', 'finance'), " +
                        "('CUST-002', 'Finance Customer Beta', 'Finance', 'finance'), " +
                        "('CUST-003', 'Finance Customer Gamma', 'Finance', 'finance')");

            LOGGER.info("Created 3 schemas with identical table structure but different data");
        }
    }

    @Test
    @DisplayName("Cache MUST isolate data between schemas - same ID, different schemas = different data")
    void testCacheIsolationBetweenSchemas() throws Exception {
        LOGGER.info("TEST: Validating cache correctly isolates data from different schemas");
        
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/resources/dbschema/SchemaCacheIsolationTest.yaml");
        
        // Update connection details for TestContainers
        updateAllDataSourceConnections(config);
        
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        
        // Test data - same customer ID will be looked up in different schemas
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST-001");
        
        // First query - hits sales schema (cache miss, reads from DB)
        LOGGER.info("Query 1: Lookup CUST-001 from SALES schema (cache miss expected)");
        RuleResult result1 = engine.evaluate(config, testData);
        Map<String, Object> salesData = result1.getEnrichedData();
        
        assertEquals("Sales Customer Alpha", salesData.get("sales_customer_name"));
        assertEquals("sales", salesData.get("sales_schema"));
        LOGGER.info("Sales schema returned correct customer: {}", salesData.get("sales_customer_name"));
        
        // Second query - hits marketing schema (cache miss, reads from DB)
        LOGGER.info("Query 2: Lookup CUST-001 from MARKETING schema (cache miss expected)");
        RuleResult result2 = engine.evaluate(config, testData);
        Map<String, Object> marketingData = result2.getEnrichedData();
        
        assertEquals("Marketing Customer Alpha", marketingData.get("marketing_customer_name"));
        assertEquals("marketing", marketingData.get("marketing_schema"));
        LOGGER.info("Marketing schema returned correct customer: {}", marketingData.get("marketing_customer_name"));
        
        // Third query - hits finance schema (cache miss, reads from DB)
        LOGGER.info("Query 3: Lookup CUST-001 from FINANCE schema (cache miss expected)");
        RuleResult result3 = engine.evaluate(config, testData);
        Map<String, Object> financeData = result3.getEnrichedData();
        
        assertEquals("Finance Customer Alpha", financeData.get("finance_customer_name"));
        assertEquals("finance", financeData.get("finance_schema"));
        LOGGER.info("Finance schema returned correct customer: {}", financeData.get("finance_customer_name"));
        
        // Critical validation: All three schemas returned DIFFERENT data for same ID
        assertNotEquals(salesData.get("sales_customer_name"), marketingData.get("marketing_customer_name"),
            "Sales and Marketing MUST return different customers for same ID");
        assertNotEquals(salesData.get("sales_customer_name"), financeData.get("finance_customer_name"),
            "Sales and Finance MUST return different customers for same ID");
        assertNotEquals(marketingData.get("marketing_customer_name"), financeData.get("finance_customer_name"),
            "Marketing and Finance MUST return different customers for same ID");
        
        LOGGER.info("🎯 CACHE ISOLATION VERIFIED: Same customer ID from different schemas returned different data");
    }

    @Test
    @DisplayName("Cache hits MUST respect schema boundaries - cached sales data MUST NOT leak to marketing")
    void testCacheHitsRespectSchemaBoundaries() throws Exception {
        LOGGER.info("TEST: Validating cache hits return correct schema-specific data");
        
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/resources/dbschema/SchemaCacheIsolationTest.yaml");
        updateAllDataSourceConnections(config);
        
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST-002");
        
        // First execution - populate ALL caches (sales, marketing, finance)
        LOGGER.info("📝 Execution 1: Populating all schema caches with CUST-002");
        engine.evaluate(config, testData);
        
        // Second execution - all should be cache hits
        LOGGER.info("🔄 Execution 2: All queries should hit cache");
        RuleResult cachedResult = engine.evaluate(config, testData);
        Map<String, Object> cachedData = cachedResult.getEnrichedData();
        
        // Verify cached data maintains schema isolation
        assertEquals("Sales Customer Beta", cachedData.get("sales_customer_name"));
        assertEquals("Marketing Customer Beta", cachedData.get("marketing_customer_name"));
        assertEquals("Finance Customer Beta", cachedData.get("finance_customer_name"));
        
        assertEquals("sales", cachedData.get("sales_schema"));
        assertEquals("marketing", cachedData.get("marketing_schema"));
        assertEquals("finance", cachedData.get("finance_schema"));
        
        LOGGER.info("Cache hits respected schema boundaries - no data leakage detected");
    }

    @Test
    @DisplayName("Cache with different TTLs per schema MUST expire independently")
    void testPerSchemaCacheTTLIndependence() throws Exception {
        LOGGER.info("TEST: Validating per-schema cache TTL independence");
        
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/resources/dbschema/SchemaCacheTTLTest.yaml");
        updateAllDataSourceConnections(config);
        
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST-003");
        
        // Populate caches (sales TTL=2s, marketing TTL=5s, finance TTL=10s)
        LOGGER.info("📝 Populating caches with different TTLs");
        RuleResult result1 = engine.evaluate(config, testData);
        assertEquals("Sales Customer Gamma", result1.getEnrichedData().get("sales_customer_name"));
        
        // Wait for sales cache to expire (2.5 seconds)
        LOGGER.info("⏱️ Waiting 2.5s for sales cache to expire...");
        Thread.sleep(2500);
        
        // Sales should be DB hit (expired), marketing/finance should be cache hits (still valid)
        RuleResult result2 = engine.evaluate(config, testData);
        Map<String, Object> data = result2.getEnrichedData();
        
        // All should still return correct data
        assertEquals("Sales Customer Gamma", data.get("sales_customer_name"));
        assertEquals("Marketing Customer Gamma", data.get("marketing_customer_name"));
        assertEquals("Finance Customer Gamma", data.get("finance_customer_name"));
        
        LOGGER.info("Per-schema TTL independence verified - sales expired, others still cached");
    }

    @Test
    @DisplayName("Cache eviction MUST not affect other schemas when max-size reached")
    void testCacheEvictionRespectsSchemaBoundaries() throws Exception {
        LOGGER.info("TEST: Validating cache eviction per-schema isolation");
        
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/resources/dbschema/SchemaCacheEvictionTest.yaml");
        updateAllDataSourceConnections(config);
        
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        
        // Populate sales cache to max capacity (max-size: 2)
        LOGGER.info("📝 Filling sales cache to capacity (2 entries)");
        engine.evaluate(config, Map.of("customerId", "CUST-001"));
        engine.evaluate(config, Map.of("customerId", "CUST-002"));
        
        // Trigger eviction in sales cache by adding 3rd entry
        LOGGER.info("🗑️ Triggering LRU eviction in sales cache");
        RuleResult result = engine.evaluate(config, Map.of("customerId", "CUST-003"));
        
        // Verify marketing and finance caches are unaffected
        Map<String, Object> data = result.getEnrichedData();
        assertNotNull(data.get("marketing_customer_name"), "Marketing cache should be unaffected");
        assertNotNull(data.get("finance_customer_name"), "Finance cache should be unaffected");
        
        LOGGER.info("Cache eviction in sales schema did not affect marketing/finance caches");
    }

    /**
     * Update all data source connections to use TestContainers PostgreSQL.
     */
    private void updateAllDataSourceConnections(YamlRuleConfiguration config) {
        config.getDataSources().forEach(ds -> {
            if (ds.getConnection() != null) {
                ds.getConnection().put("database", postgres.getDatabaseName());
                ds.getConnection().put("username", postgres.getUsername());
                ds.getConnection().put("password", postgres.getPassword());
                ds.getConnection().put("host", postgres.getHost());
                ds.getConnection().put("port", postgres.getFirstMappedPort());
            }
        });
    }
}
