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
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.DockerClientFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL Custom Schema Lookup Test - Tests lookups in a custom schema ("myschema")
 * 
 * This test validates APEX's ability to work with non-default PostgreSQL schemas.
 * The test creates a custom schema "myschema" and demonstrates:
 * - Creating tables in custom schemas
 * - Executing lookups against custom schema tables
 * - PostgreSQL-specific features (JSONB, arrays) in custom schemas
 * 
 * SUCCESS METRICS:
 * - Response Time: < 100ms for simple lookup
 * - Custom Schema: Tables correctly created and queried in "myschema"
 * - Data Accuracy: 100% match with expected customer data
 * - PostgreSQL Features: JSONB and array columns working in custom schema
 * 
 * This test uses REAL PostgreSQL via Testcontainers - NO MOCKING
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostgreSQLSchemaSimpleLookupTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLSchemaSimpleLookupTest.class);
    
    private static final String CUSTOM_SCHEMA = "myschema";

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
            .withDatabaseName("apex_schema_test")
            .withUsername("apex_user")
            .withPassword("apex_pass");
    
    @BeforeAll
    static void setupCustomSchema() throws Exception {
        // Wait for container to be ready
        if (!postgres.isRunning()) {
            return;
        }
        
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();
        String database = postgres.getDatabaseName();
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {
            
            // Create custom schema
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + CUSTOM_SCHEMA);
            
            // Set search_path for the database so all queries use the custom schema by default
            stmt.execute("ALTER DATABASE " + database + " SET search_path TO " + CUSTOM_SCHEMA + ", public");
            
            // Create customers table in custom schema with PostgreSQL-specific features
            stmt.execute("""
                CREATE TABLE myschema.customers (
                    customer_id VARCHAR(20) PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    customer_type VARCHAR(20) NOT NULL,
                    tier VARCHAR(20) NOT NULL,
                    region VARCHAR(10) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    created_date DATE DEFAULT CURRENT_DATE,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    metadata JSONB,
                    tags TEXT[],
                    CONSTRAINT chk_customer_type CHECK (customer_type IN ('CORPORATE', 'INSTITUTIONAL', 'RETAIL')),
                    CONSTRAINT chk_tier CHECK (tier IN ('PLATINUM', 'GOLD', 'SILVER', 'BRONZE')),
                    CONSTRAINT chk_region CHECK (region IN ('NA', 'EU', 'APAC', 'LATAM')),
                    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
                )
                """);
            
            // Create indexes
            stmt.execute("CREATE INDEX idx_myschema_customers_type_tier ON myschema.customers(customer_type, tier)");
            stmt.execute("CREATE INDEX idx_myschema_customers_region_status ON myschema.customers(region, status)");
            
            // Insert test data
            stmt.execute("""
                INSERT INTO myschema.customers (customer_id, customer_name, customer_type, tier, region, status, created_date, metadata, tags)
                VALUES 
                ('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2023-01-15',
                 '{"industry": "Technology", "employees": 5000, "revenue": 1000000000}',
                 ARRAY['tech', 'large-cap', 'nasdaq']),
                ('CUST000002', 'Beta Industries', 'CORPORATE', 'GOLD', 'EU', 'ACTIVE', '2023-02-20',
                 '{"industry": "Manufacturing", "employees": 2500, "revenue": 500000000}',
                 ARRAY['manufacturing', 'mid-cap', 'ftse']),
                ('CUST000003', 'Gamma Holdings', 'INSTITUTIONAL', 'PLATINUM', 'APAC', 'ACTIVE', '2023-03-10',
                 '{"industry": "Financial Services", "aum": 50000000000, "fund_type": "hedge"}',
                 ARRAY['finance', 'institutional', 'hedge-fund']),
                ('CUST000004', 'Delta Partners', 'CORPORATE', 'SILVER', 'NA', 'ACTIVE', '2023-04-05',
                 '{"industry": "Real Estate", "employees": 500, "revenue": 100000000}',
                 ARRAY['real-estate', 'small-cap', 'reit']),
                ('CUST000005', 'Epsilon Fund', 'INSTITUTIONAL', 'GOLD', 'EU', 'ACTIVE', '2023-05-12',
                 '{"industry": "Asset Management", "aum": 25000000000, "fund_type": "mutual"}',
                 ARRAY['asset-mgmt', 'institutional', 'mutual-fund']),
                ('CUST000006', 'Zeta Ventures', 'CORPORATE', 'BRONZE', 'LATAM', 'INACTIVE', '2023-06-01',
                 '{"industry": "Venture Capital", "employees": 50, "revenue": 10000000}',
                 ARRAY['vc', 'startup', 'early-stage']),
                ('CUST000007', 'Eta Systems', 'CORPORATE', 'GOLD', 'NA', 'ACTIVE', '2023-07-20',
                 '{"industry": "Healthcare", "employees": 1500, "revenue": 300000000}',
                 ARRAY['healthcare', 'mid-cap', 'nyse']),
                ('CUST000008', 'Theta Capital', 'INSTITUTIONAL', 'SILVER', 'APAC', 'ACTIVE', '2023-08-15',
                 '{"industry": "Private Equity", "aum": 10000000000, "fund_type": "pe"}',
                 ARRAY['private-equity', 'institutional', 'buyout']),
                ('CUST000009', 'Iota Retail', 'RETAIL', 'BRONZE', 'EU', 'ACTIVE', '2023-09-01',
                 '{"industry": "Consumer", "employees": 100, "revenue": 25000000}',
                 ARRAY['retail', 'small-cap', 'consumer']),
                ('CUST000010', 'Kappa Energy', 'CORPORATE', 'PLATINUM', 'NA', 'SUSPENDED', '2023-10-10',
                 '{"industry": "Energy", "employees": 8000, "revenue": 2000000000}',
                 ARRAY['energy', 'large-cap', 'oil-gas'])
                """);
            
            // Create a VIEW in the custom schema for testing view lookups
            stmt.execute("""
                CREATE VIEW myschema.customer_summary_view AS
                SELECT 
                    customer_id,
                    customer_name,
                    customer_type,
                    tier,
                    region,
                    status,
                    metadata->>'industry' as industry,
                    CASE 
                        WHEN metadata->>'revenue' IS NOT NULL THEN (metadata->>'revenue')::BIGINT
                        WHEN metadata->>'aum' IS NOT NULL THEN (metadata->>'aum')::BIGINT
                        ELSE 0
                    END as financial_value,
                    array_length(tags, 1) as tag_count
                FROM myschema.customers
                WHERE status = 'ACTIVE'
                """);
            
            logger.info("Created custom schema '{}' with customers table, customer_summary_view, and test data", CUSTOM_SCHEMA);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should validate PostgreSQL container and custom schema setup")
    void testPostgreSQLContainerAndSchemaSetup() {
        logger.info("=".repeat(80));
        logger.info("PostgreSQL Custom Schema Setup Validation");
        logger.info("=".repeat(80));
        
        // Validate container is running
        assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
        
        // Validate connection details
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();
        
        logger.info("PostgreSQL Container Details:");
        logger.info("  JDBC URL: {}", jdbcUrl);
        logger.info("  Username: {}", username);
        logger.info("  Database: {}", postgres.getDatabaseName());
        logger.info("  Port: {}", postgres.getFirstMappedPort());
        logger.info("  Custom Schema: {}", CUSTOM_SCHEMA);
        
        // Test direct database connection and verify custom schema
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            Statement statement = connection.createStatement();
            
            // Verify custom schema exists
            ResultSet rs = statement.executeQuery(
                "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '" + CUSTOM_SCHEMA + "'");
            assertTrue(rs.next(), "Custom schema '" + CUSTOM_SCHEMA + "' should exist");
            logger.info("Custom schema '{}' verified", CUSTOM_SCHEMA);
            
            // Verify customers table exists in custom schema
            rs = statement.executeQuery("SELECT COUNT(*) FROM " + CUSTOM_SCHEMA + ".customers");
            rs.next();
            int customerCount = rs.getInt(1);
            
            logger.info("Database Validation:");
            logger.info("  Schema: {}", CUSTOM_SCHEMA);
            logger.info("  Total customers in {}.customers: {}", CUSTOM_SCHEMA, customerCount);
            assertTrue(customerCount >= 10, "Should have at least 10 customers in custom schema");
            
            // Verify PostgreSQL-specific features in custom schema
            rs = statement.executeQuery("SELECT customer_id, metadata, tags FROM " + CUSTOM_SCHEMA + ".customers WHERE customer_id = 'CUST000001'");
            if (rs.next()) {
                String customerId = rs.getString("customer_id");
                String metadata = rs.getString("metadata");
                String[] tags = (String[]) rs.getArray("tags").getArray();
                
                logger.info("PostgreSQL Features Validation in Custom Schema:");
                logger.info("  Customer ID: {}", customerId);
                logger.info("  JSONB Metadata: {}", metadata);
                logger.info("  Array Tags: {}", String.join(", ", tags));
                
                assertNotNull(metadata, "JSONB metadata should not be null");
                assertTrue(tags.length > 0, "Tags array should not be empty");
            }
            
        } catch (Exception e) {
            logger.error("X Database connection failed: {}", e.getMessage(), e);
            fail("Direct database connection should work: " + e.getMessage());
        }
        
        logger.info("PostgreSQL custom schema setup validation completed successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Should perform simple PostgreSQL customer lookup in custom schema via APEX")
    void testPostgreSQLSimpleLookupInCustomSchema() {
        logger.info("\n" + "=".repeat(80));
        logger.info("PostgreSQL Simple Lookup via APEX (Custom Schema: {})", CUSTOM_SCHEMA);
        logger.info("=".repeat(80));
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/lookup/PostgreSQLSchemaSimpleLookupTest.yaml");
            
            // Update configuration with real PostgreSQL connection details
            updatePostgreSQLConnection(config);
            
            // Create test data for customer lookup
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST000001");
            
            logger.info("Input Data:");
            logger.info("  Customer ID: {}", testData.get("customerId"));
            logger.info("  Target Schema: {}", CUSTOM_SCHEMA);
            
            // Execute APEX enrichment with real PostgreSQL
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Object result = ruleResult.getEnrichedData();
            
            long responseTime = System.currentTimeMillis() - startTime;
            logger.info("Response Time: {}ms", responseTime);
            
            // Validate enrichment results
            assertNotNull(result, "PostgreSQL lookup result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate basic customer data from custom schema
            assertEquals("Acme Corporation", enrichedData.get("customerName"));
            assertEquals("CORPORATE", enrichedData.get("customerType"));
            assertEquals("PLATINUM", enrichedData.get("customerTier"));
            assertEquals("NA", enrichedData.get("customerRegion"));
            assertEquals("ACTIVE", enrichedData.get("customerStatus"));
            assertNotNull(enrichedData.get("customerCreatedDate"));
            
            // Validate PostgreSQL-specific features
            assertNotNull(enrichedData.get("customerMetadata"), "JSONB metadata should be enriched");
            assertNotNull(enrichedData.get("customerTags"), "Array tags should be enriched");
            
            logger.info("Basic Enrichment Results from {}.customers:", CUSTOM_SCHEMA);
            logger.info("  Customer Name: {}", enrichedData.get("customerName"));
            logger.info("  Customer Type: {}", enrichedData.get("customerType"));
            logger.info("  Customer Tier: {}", enrichedData.get("customerTier"));
            logger.info("  Customer Region: {}", enrichedData.get("customerRegion"));
            logger.info("  Customer Status: {}", enrichedData.get("customerStatus"));
            
            // Validate performance requirement (allow extra time for first run with container startup)
            assertTrue(responseTime < 1000, "Response time should be < 1000ms for first run, was: " + responseTime + "ms");
            
            logger.info("PostgreSQL simple lookup in custom schema completed successfully in {}ms", responseTime);
            
        } catch (Exception e) {
            logger.error("X PostgreSQL simple lookup failed: {}", e.getMessage(), e);
            fail("PostgreSQL simple lookup should work: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should test PostgreSQL JSON enrichment features in custom schema")
    void testPostgreSQLJsonEnrichmentInCustomSchema() {
        logger.info("\n" + "=".repeat(80));
        logger.info("PostgreSQL JSON Features Testing (Custom Schema: {})", CUSTOM_SCHEMA);
        logger.info("=".repeat(80));
        
        try {
            // Load configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/lookup/PostgreSQLSchemaSimpleLookupTest.yaml");
            updatePostgreSQLConnection(config);
            
            // Test data that will trigger JSON enrichment
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST000001");
            
            // Execute enrichment
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Object result = ruleResult.getEnrichedData();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // First enrichment should populate customerMetadata
            assertNotNull(enrichedData.get("customerMetadata"), "Customer metadata should be populated");
            
            // Second enrichment should extract JSON fields - these MUST be present
            assertEquals("Technology", enrichedData.get("customerIndustry"),
                "Industry should be extracted from JSON metadata");
            assertEquals(5000, enrichedData.get("customerEmployeeCount"),
                "Employee count should be extracted from JSON metadata");
            assertEquals(1000000000L, enrichedData.get("customerFinancialValue"),
                "Financial value (revenue) should be extracted from JSON metadata");
            assertNotNull(enrichedData.get("customerTagList"),
                "Tag list should be extracted from array");
            
            logger.info("JSON Enrichment Results from {}.customers:", CUSTOM_SCHEMA);
            logger.info("  Industry: {}", enrichedData.get("customerIndustry"));
            logger.info("  Employee Count: {}", enrichedData.get("customerEmployeeCount"));
            logger.info("  Financial Value: {}", enrichedData.get("customerFinancialValue"));
            logger.info("  Tag List: {}", enrichedData.get("customerTagList"));
            
            logger.info("PostgreSQL JSON enrichment features validated successfully in custom schema '{}'", CUSTOM_SCHEMA);
            
        } catch (Exception e) {
            logger.error("X PostgreSQL JSON enrichment failed: {}", e.getMessage(), e);
            fail("PostgreSQL JSON enrichment should work: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should perform lookup against PostgreSQL VIEW via YAML configuration")
    void testPostgreSQLViewLookupViaYaml() {
        logger.info("\n" + "=".repeat(80));
        logger.info("PostgreSQL VIEW Lookup via YAML (Custom Schema: {})", CUSTOM_SCHEMA);
        logger.info("=".repeat(80));
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Load YAML configuration which includes view-based enrichment
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/lookup/PostgreSQLSchemaSimpleLookupTest.yaml");
            
            // Update configuration with real PostgreSQL connection details
            updatePostgreSQLConnection(config);
            
            // Create test data for customer lookup against the VIEW
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST000001");
            
            logger.info("Input Data:");
            logger.info("  Customer ID: {}", testData.get("customerId"));
            logger.info("  Target: myschema.customer_summary_view (VIEW)");
            
            // Execute APEX enrichment - the YAML config queries the VIEW
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Object result = ruleResult.getEnrichedData();
            
            long responseTime = System.currentTimeMillis() - startTime;
            logger.info("Response Time: {}ms", responseTime);
            
            // Validate enrichment results from the VIEW
            assertNotNull(result, "PostgreSQL VIEW lookup result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate data came from the VIEW (which filters to ACTIVE customers only)
            // CUST000001 is ACTIVE so should be found
            assertEquals("Acme Corporation", enrichedData.get("viewCustomerName"), 
                "Customer name should be enriched from VIEW");
            assertEquals("CORPORATE", enrichedData.get("viewCustomerType"),
                "Customer type should be enriched from VIEW");
            assertEquals("PLATINUM", enrichedData.get("viewTier"),
                "Tier should be enriched from VIEW");
            assertEquals("Technology", enrichedData.get("viewIndustry"),
                "Industry should be extracted from VIEW (via JSON)");
            assertEquals(1000000000L, enrichedData.get("viewFinancialValue"),
                "Financial value should be calculated in VIEW");
            assertEquals(3, enrichedData.get("viewTagCount"),
                "Tag count should be calculated in VIEW (Acme has 3 tags: tech, large-cap, nasdaq)");
            
            logger.info("VIEW Enrichment Results from {}.customer_summary_view:", CUSTOM_SCHEMA);
            logger.info("  Customer Name: {}", enrichedData.get("viewCustomerName"));
            logger.info("  Customer Type: {}", enrichedData.get("viewCustomerType"));
            logger.info("  Tier: {}", enrichedData.get("viewTier"));
            logger.info("  Industry: {}", enrichedData.get("viewIndustry"));
            logger.info("  Financial Value: {}", enrichedData.get("viewFinancialValue"));
            logger.info("  Tag Count: {}", enrichedData.get("viewTagCount"));
            
            // Validate performance
            assertTrue(responseTime < 1000, "Response time should be < 1000ms, was: " + responseTime + "ms");
            
            logger.info("PostgreSQL VIEW lookup via YAML completed successfully in {}ms", responseTime);
            
        } catch (Exception e) {
            logger.error("X PostgreSQL VIEW lookup failed: {}", e.getMessage(), e);
            fail("PostgreSQL VIEW lookup should work: " + e.getMessage());
        }
    }

    /**
     * Update YAML configuration with real PostgreSQL connection details from Testcontainers
     */
    private void updatePostgreSQLConnection(YamlRuleConfiguration config) {
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();
        String host = postgres.getHost();
        Integer port = postgres.getFirstMappedPort();
        String database = postgres.getDatabaseName();

        logger.info("PostgreSQL Connection Details for APEX:");
        logger.info("  JDBC URL: {}", jdbcUrl);
        logger.info("  Host: {}", host);
        logger.info("  Port: {}", port);
        logger.info("  Database: {}", database);
        logger.info("  Schema: {}", CUSTOM_SCHEMA);
        logger.info("  Username: {}", username);
        logger.info("  Password: [REDACTED]");

        // Update the PostgreSQL data source configuration with real Testcontainers connection details
        if (config.getDataSources() != null) {
            for (var dataSource : config.getDataSources()) {
                if ("postgresql-myschema-database".equals(dataSource.getName())) {
                    Map<String, Object> connection = dataSource.getConnection();

                    // Update connection details with real Testcontainers values
                    connection.put("host", host);
                    connection.put("port", port);
                    connection.put("database", database);
                    connection.put("username", username);
                    connection.put("password", password);
                    connection.put("schema", CUSTOM_SCHEMA);

                    logger.info("Updated PostgreSQL data source '{}' with Testcontainers connection details (schema: {})",
                               dataSource.getName(), CUSTOM_SCHEMA);
                    break;
                }
            }
        }
    }
}
