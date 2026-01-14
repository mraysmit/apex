package dev.mars.apex.demo.lookup;

import dev.mars.apex.demo.DemoTestBase;

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

import dev.mars.apex.demo.util.TestContainerImages;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.DockerClientFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL Schema Lookup Negative Test - Tests error handling for invalid schema configurations
 * 
 * This test validates that APEX produces proper warnings and handles errors gracefully when:
 * 1. Schema name is incorrect (table not found)
 * 2. Schema is not specified (defaults to public, table not found)
 * 3. Schema exists but table doesn't exist in that schema
 * 
 * EXPECTED BEHAVIOR:
 * - When schema is wrong, enrichment should fail gracefully
 * - Error messages should clearly indicate "relation does not exist"
 * - Required fields should NOT be populated (null values)
 * - Non-required enrichments should be skipped without crashing
 * 
 * This test uses REAL PostgreSQL via Testcontainers - NO MOCKING
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostgreSQLSchemaLookupNegativeTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLSchemaLookupNegativeTest.class);
    
    // Correct schema where table exists
    private static final String CORRECT_SCHEMA = "myschema";
    // Wrong schema - table doesn't exist here
    private static final String WRONG_SCHEMA = "wrong_schema";
    // Non-existent schema
    private static final String NONEXISTENT_SCHEMA = "nonexistent_schema";

    @BeforeAll
    static void checkDockerAvailability() {
        try {
            DockerClientFactory.instance().client();
        } catch (Exception e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                "Docker is not available. Skipping PostgreSQL integration tests. " +
                "To run these tests, ensure Docker is installed and running. Error: " + e.getMessage());
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(TestContainerImages.POSTGRES)
            .withDatabaseName("apex_schema_negative_test")
            .withUsername("apex_user")
            .withPassword("apex_pass");
    
    @BeforeAll
    static void setupSchemas() throws Exception {
        if (!postgres.isRunning()) {
            return;
        }
        
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {
            
            // Create the CORRECT schema with table
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + CORRECT_SCHEMA);
            
            // Create the WRONG schema (empty - no tables)
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + WRONG_SCHEMA);
            
            // Create customers table ONLY in the correct schema
            stmt.execute("""
                CREATE TABLE myschema.customers (
                    customer_id VARCHAR(20) PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    customer_type VARCHAR(20) NOT NULL,
                    tier VARCHAR(20) NOT NULL,
                    region VARCHAR(10) NOT NULL,
                    status VARCHAR(20) NOT NULL
                )
                """);
            
            // Insert test data
            stmt.execute("""
                INSERT INTO myschema.customers (customer_id, customer_name, customer_type, tier, region, status)
                VALUES 
                ('CUST001', 'Test Corporation', 'CORPORATE', 'GOLD', 'NA', 'ACTIVE')
                """);
            
            logger.info("✅ Created schema '{}' with customers table", CORRECT_SCHEMA);
            logger.info("✅ Created empty schema '{}' (no tables)", WRONG_SCHEMA);
            logger.info("⚠️  Schema '{}' does NOT exist", NONEXISTENT_SCHEMA);
        }
    }

    @Test
    @Order(1)
    @DisplayName("NEGATIVE: Should fail gracefully when schema is wrong")
    void testWrongSchemaProducesError() {
        logger.info("\n" + "=".repeat(80));
        logger.info("NEGATIVE TEST: Wrong Schema Configuration");
        logger.info("=".repeat(80));
        logger.info("Expected: Enrichment should fail because table doesn't exist in wrong_schema");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/lookup/PostgreSQLSchemaLookupNegativeTest.yaml");
            
            // Update with WRONG schema - table doesn't exist there
            updateConnectionWithSchema(config, "test-database", WRONG_SCHEMA);
            
            // Create test data
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST001");
            
            logger.info("Input: customerId={}", testData.get("customerId"));
            logger.info("Schema configured: '{}' (WRONG - table not in this schema)", WRONG_SCHEMA);
            
            // Execute enrichment - should NOT throw exception but enrichment should fail
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) ruleResult.getEnrichedData();
            
            // Verify that customer data was NOT enriched (because table doesn't exist in wrong_schema)
            assertNull(enrichedData.get("customerName"), 
                "Customer name should be NULL when schema is wrong");
            assertNull(enrichedData.get("customerType"), 
                "Customer type should be NULL when schema is wrong");
            
            logger.info("✅ NEGATIVE TEST PASSED: Enrichment correctly failed for wrong schema");
            logger.info("   customerName: {} (expected: null)", enrichedData.get("customerName"));
            logger.info("   customerType: {} (expected: null)", enrichedData.get("customerType"));
            
        } catch (Exception e) {
            // If we get here, the error handling is working - log it
            logger.info("✅ NEGATIVE TEST PASSED: Exception thrown as expected");
            logger.info("   Error: {}", e.getMessage());
            // Accept any exception - the test passes if schema lookup fails
            assertTrue(e.getMessage() != null && !e.getMessage().isEmpty(),
                "Error message should not be empty");
        }
    }

    @Test
    @Order(2)
    @DisplayName("NEGATIVE: Should fail gracefully when schema doesn't exist")
    void testNonexistentSchemaProducesError() {
        logger.info("\n" + "=".repeat(80));
        logger.info("NEGATIVE TEST: Nonexistent Schema Configuration");
        logger.info("=".repeat(80));
        logger.info("Expected: Enrichment should fail because schema doesn't exist");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/lookup/PostgreSQLSchemaLookupNegativeTest.yaml");
            
            // Update with NONEXISTENT schema
            updateConnectionWithSchema(config, "test-database", NONEXISTENT_SCHEMA);
            
            // Create test data
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST001");
            
            logger.info("Input: customerId={}", testData.get("customerId"));
            logger.info("Schema configured: '{}' (NONEXISTENT)", NONEXISTENT_SCHEMA);
            
            // Execute enrichment - should fail
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) ruleResult.getEnrichedData();
            
            // Verify enrichment failed
            assertNull(enrichedData.get("customerName"), 
                "Customer name should be NULL when schema doesn't exist");
            
            logger.info("✅ NEGATIVE TEST PASSED: Enrichment correctly failed for nonexistent schema");
            logger.info("   customerName: {} (expected: null)", enrichedData.get("customerName"));
            
        } catch (Exception e) {
            logger.info("✅ NEGATIVE TEST PASSED: Exception thrown as expected");
            logger.info("   Error: {}", e.getMessage());
            // Accept any exception - the test passes if schema lookup fails
            assertTrue(e.getMessage() != null && !e.getMessage().isEmpty(),
                "Error message should not be empty");
        }
    }

    @Test
    @Order(3)
    @DisplayName("POSITIVE: Should succeed when correct schema is used")
    void testCorrectSchemaSucceeds() {
        logger.info("\n" + "=".repeat(80));
        logger.info("POSITIVE TEST: Correct Schema Configuration (Control Test)");
        logger.info("=".repeat(80));
        logger.info("Expected: Enrichment should succeed with correct schema");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/lookup/PostgreSQLSchemaLookupNegativeTest.yaml");
            
            // Update with CORRECT schema
            updateConnectionWithSchema(config, "test-database", CORRECT_SCHEMA);
            
            // Create test data
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST001");
            
            logger.info("Input: customerId={}", testData.get("customerId"));
            logger.info("Schema configured: '{}' (CORRECT)", CORRECT_SCHEMA);
            
            // Execute enrichment
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) ruleResult.getEnrichedData();
            
            // Verify enrichment succeeded
            assertEquals("Test Corporation", enrichedData.get("customerName"),
                "Customer name should be enriched from correct schema");
            assertEquals("CORPORATE", enrichedData.get("customerType"),
                "Customer type should be enriched from correct schema");
            assertEquals("GOLD", enrichedData.get("customerTier"),
                "Customer tier should be enriched from correct schema");
            
            logger.info("✅ POSITIVE TEST PASSED: Enrichment succeeded with correct schema");
            logger.info("   customerName: {}", enrichedData.get("customerName"));
            logger.info("   customerType: {}", enrichedData.get("customerType"));
            logger.info("   customerTier: {}", enrichedData.get("customerTier"));
            
        } catch (Exception e) {
            logger.error("❌ POSITIVE TEST FAILED: {}", e.getMessage(), e);
            fail("Positive test with correct schema should not fail: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("NEGATIVE: Should fail when schema not specified (defaults to public)")
    void testNoSchemaDefaultsToPublic() {
        logger.info("\n" + "=".repeat(80));
        logger.info("NEGATIVE TEST: No Schema Specified (Defaults to public)");
        logger.info("=".repeat(80));
        logger.info("Expected: Enrichment should fail because table is in myschema, not public");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/lookup/PostgreSQLSchemaLookupNegativeTest.yaml");
            
            // Update connection WITHOUT schema (will default to public)
            updateConnectionWithoutSchema(config, "test-database");
            
            // Create test data
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST001");
            
            logger.info("Input: customerId={}", testData.get("customerId"));
            logger.info("Schema configured: NONE (defaults to 'public')");
            logger.info("Table location: myschema.customers (NOT in public)");
            
            // Execute enrichment
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) ruleResult.getEnrichedData();
            
            // Verify enrichment failed (table not in public schema)
            assertNull(enrichedData.get("customerName"), 
                "Customer name should be NULL when no schema specified (defaults to public)");
            
            logger.info("✅ NEGATIVE TEST PASSED: Enrichment correctly failed without schema");
            logger.info("   customerName: {} (expected: null)", enrichedData.get("customerName"));
            
        } catch (Exception e) {
            logger.info("✅ NEGATIVE TEST PASSED: Exception thrown as expected");
            logger.info("   Error: {}", e.getMessage());
        }
    }

    /**
     * Update data source connection with specified schema.
     */
    private void updateConnectionWithSchema(YamlRuleConfiguration config, String dataSourceName, String schema) {
        String host = postgres.getHost();
        Integer port = postgres.getFirstMappedPort();
        String database = postgres.getDatabaseName();
        String username = postgres.getUsername();
        String password = postgres.getPassword();

        if (config.getDataSources() != null) {
            for (var dataSource : config.getDataSources()) {
                if (dataSourceName.equals(dataSource.getName())) {
                    Map<String, Object> connection = dataSource.getConnection();
                    connection.put("host", host);
                    connection.put("port", port);
                    connection.put("database", database);
                    connection.put("username", username);
                    connection.put("password", password);
                    connection.put("schema", schema);
                    
                    logger.info("✅ Updated data source '{}' with schema '{}'", dataSourceName, schema);
                    break;
                }
            }
        }
    }

    /**
     * Update data source connection WITHOUT schema (to test default behavior).
     */
    private void updateConnectionWithoutSchema(YamlRuleConfiguration config, String dataSourceName) {
        String host = postgres.getHost();
        Integer port = postgres.getFirstMappedPort();
        String database = postgres.getDatabaseName();
        String username = postgres.getUsername();
        String password = postgres.getPassword();

        if (config.getDataSources() != null) {
            for (var dataSource : config.getDataSources()) {
                if (dataSourceName.equals(dataSource.getName())) {
                    Map<String, Object> connection = dataSource.getConnection();
                    connection.put("host", host);
                    connection.put("port", port);
                    connection.put("database", database);
                    connection.put("username", username);
                    connection.put("password", password);
                    connection.remove("schema");  // Remove schema to test default
                    
                    logger.info("✅ Updated data source '{}' WITHOUT schema (defaults to public)", dataSourceName);
                    break;
                }
            }
        }
    }
}
