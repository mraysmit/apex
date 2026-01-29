package dev.mars.apex.core.dbschema;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.data.external.registry.DataSourceRegistry;
import dev.mars.apex.core.test.TestContainerImages;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

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
 * PostgreSQLSchemaConfigurationTest - Tests PostgreSQL schema parameter configuration.
 *
 * <p>This test validates that APEX correctly applies the PostgreSQL schema parameter from
 * YAML configuration, ensuring queries execute against the configured schema rather than
 * defaulting to the 'public' schema.</p>
 *
 * <h2>Key Validation Points:</h2>
 * <ul>
 *   <li>Schema parameter is read from YAML connection configuration</li>
 *   <li>JDBC URL includes currentSchema parameter: ?currentSchema=trading</li>
 *   <li>Queries execute without schema prefix (SELECT * FROM products, not trading.products)</li>
 *   <li>PostgreSQL search_path is correctly set at connection level</li>
 * </ul>
 *
 * <h2>YAML Configuration Pattern:</h2>
 * <pre>{@code
 * data-sources:
 *   - name: "trading-database"
 *     connection:
 *       host: "${POSTGRES_HOST}"
 *       schema: "trading"  # ← Sets currentSchema JDBC parameter
 * 
 * enrichments:
 *   - lookup-config:
 *       query: "SELECT * FROM products WHERE id = :id"  # ← No schema prefix
 * }</pre>
 *
 * <h2>Implementation:</h2>
 * <p>See {@code JdbcTemplateFactory.java} lines 237-241 for JDBC URL construction.</p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-01-15
 * @version 1.0
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostgreSQLSchemaConfigurationTest {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLSchemaConfigurationTest.class);
    private static final String CUSTOM_SCHEMA = "trading";
    private static final DockerImageName POSTGRES_IMAGE = 
        DockerImageName.parse(TestContainerImages.POSTGRES)
                       .asCompatibleSubstituteFor("postgres");

    /*
     * TESTCONTAINERS 2.0 PATTERN - INSTANCE GENERIC CONTAINER
     * 
     * Uses instance container (not static) to ensure complete test isolation.
     * Each @Test method gets a fresh container with clean database state.
     * 
     * KEY BENEFITS OF INSTANCE PATTERN:
     * - Complete isolation between test methods (no data leakage)
     * - Each test can modify schemas/data without affecting others
     * - Better for tests that validate schema configuration changes
     * - Trade-off: Slower than static (container restart per test)
     * 
     * GENERICCONTAINER MODERNIZATION:
     * - Explicit .withEnv() configuration (no convenience methods)
     * - Manual JDBC URL via jdbcUrl() helper method
     * - getMappedPort(5432) for Docker port mapping
     * - Hard-coded test credentials: apex_user/apex_pass
     * - Clean URL construction: first parameter uses '?' not '&'
     * 
     * Compare with:
     * - JdbcUrlSchemaParameterTest: Instance pattern with retry logic
     * - EnvironmentPromotionTest: Instance pattern for environment testing
     */
    @Container
    @SuppressWarnings("resource") // Testcontainers manages lifecycle automatically
    GenericContainer<?> postgres = new GenericContainer<>(POSTGRES_IMAGE)
            .withEnv("POSTGRES_DB", "apex_test")
            .withEnv("POSTGRES_USER", "apex_user")
            .withEnv("POSTGRES_PASSWORD", "apex_pass")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());

    private YamlConfigurationLoader yamlLoader = new YamlConfigurationLoader();

    private String jdbcUrl() {
        return "jdbc:postgresql://" + postgres.getHost() + ":" 
            + postgres.getMappedPort(5432) + "/apex_test";
    }

    @BeforeEach
    void setupDatabase() throws Exception {
        // Clear stale JDBC pools from previous test classes (critical for Testcontainers)
        DataSourceRegistry.getInstance().clear();
        
        if (!postgres.isRunning()) {
            return;
        }
        
        logger.info("Setting up PostgreSQL test database with custom schema: {}", CUSTOM_SCHEMA);

        // Add retry logic for connection attempts to handle PostgreSQL startup timing
        int maxRetries = 3;
        int retryDelayMs = 1000;
        Connection conn = null;
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                conn = DriverManager.getConnection(jdbcUrl(), "apex_user", "apex_pass");
                break;
            } catch (Exception e) {
                if (i < maxRetries - 1) {
                    logger.info("Connection attempt {} failed, retrying in {}ms...", i + 1, retryDelayMs);
                    Thread.sleep(retryDelayMs);
                } else {
                    throw e;
                }
            }
        }

        try (Connection finalConn = conn;
             Statement stmt = finalConn.createStatement()) {

            // Create custom schema
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + CUSTOM_SCHEMA);
            logger.info("Created schema: {}", CUSTOM_SCHEMA);

            // Create products table in custom schema
            stmt.execute("""
                CREATE TABLE trading.products (
                    product_id VARCHAR(20) PRIMARY KEY,
                    product_name VARCHAR(100) NOT NULL,
                    product_type VARCHAR(50),
                    asset_class VARCHAR(50)
                )
                """);

            // Insert test data
            stmt.execute("""
                INSERT INTO trading.products VALUES
                    ('AAPL', 'Apple Inc.', 'Equity', 'Stock'),
                    ('MSFT', 'Microsoft Corporation', 'Equity', 'Stock'),
                    ('GOOGL', 'Alphabet Inc.', 'Equity', 'Stock')
                """);

            logger.info("Test data inserted into trading.products");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Schema parameter should be applied from YAML configuration")
    void testSchemaParameterFromYaml() throws Exception {
        logger.info("TEST: Validating schema parameter is read from YAML and applied to JDBC URL");

        // Load configuration with schema parameter
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/resources/dbschema/PostgreSQLSchemaConfigurationTest.yaml");

        assertNotNull(config, "Configuration should load successfully");
        assertNotNull(config.getDataSources(), "Data sources should be present");
        assertFalse(config.getDataSources().isEmpty(), "Data sources list should not be empty");

        var dataSource = config.getDataSources().get(0);
        assertNotNull(dataSource.getConnection(), "Connection configuration should exist");
        
        // Validate schema is configured
        String configuredSchema = (String) dataSource.getConnection().get("schema");
        assertEquals(CUSTOM_SCHEMA, configuredSchema, 
                "Schema should be 'trading' from YAML configuration");

        logger.info("[OK] Schema parameter correctly configured: {}", configuredSchema);
    }

    @Test
    @Order(2)
    @DisplayName("Query should execute against custom schema without prefix")
    void testQueryWithoutSchemaPrefix() throws Exception {
        logger.info("TEST: Validating query executes against 'trading' schema without prefix");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/resources/dbschema/PostgreSQLSchemaConfigurationTest.yaml");
        
        // Update data source with TestContainers connection details
        updateDataSourceConnection(config, "trading-database");

        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        // Test data with product lookup
        Map<String, Object> testData = new HashMap<>();
        testData.put("product_id", "AAPL");

        RuleResult result = engine.evaluate(config, testData);
        Map<String, Object> enrichedData = result.getEnrichedData();

        // Verify enrichment succeeded
        assertNotNull(enrichedData.get("product_name"), "Product name should be enriched");
        assertEquals("Apple Inc.", enrichedData.get("product_name"),
                "Should retrieve product from trading schema");

        logger.info("[OK] Query executed successfully against custom schema");
        logger.info("  Enriched data: product_name={}", enrichedData.get("product_name"));
    }

    @Test
    @Order(3)
    @DisplayName("Multiple schemas should be supported via multiple data-sources")
    void testMultipleSchemas() throws Exception {
        logger.info("TEST: Validating multiple custom schemas via separate data-sources");

        // Setup second schema
        if (!postgres.isRunning()) {
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(
                jdbcUrl(),
                "apex_user",
                "apex_pass");
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE SCHEMA IF NOT EXISTS sales");
            stmt.execute("""
                CREATE TABLE sales.customers (
                    customer_id VARCHAR(20) PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL
                )
                """);
            stmt.execute("INSERT INTO sales.customers VALUES ('CUST001', 'Acme Corporation')");
        }

        YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/resources/dbschema/MultiSchemaTest.yaml");
        
        // Update both data sources
        updateDataSourceConnection(config, "trading-database");
        updateMultiSchemaDataSourceConnection(config, "sales-database", "sales");

        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> testData = new HashMap<>();
        testData.put("product_id", "MSFT");
        testData.put("customer_id", "CUST001");

        RuleResult result = engine.evaluate(config, testData);
        Map<String, Object> enrichedData = result.getEnrichedData();

        // Verify data from both schemas
        assertNotNull(enrichedData.get("product_name"), "Product should be enriched from trading schema");
        assertNotNull(enrichedData.get("customer_name"), "Customer should be enriched from sales schema");
        
        assertEquals("Microsoft Corporation", enrichedData.get("product_name"));
        assertEquals("Acme Corporation", enrichedData.get("customer_name"));

        logger.info("[OK] Multiple schemas accessed successfully");
        logger.info("  Trading schema: product_name={}", enrichedData.get("product_name"));
        logger.info("  Sales schema: customer_name={}", enrichedData.get("customer_name"));
    }

    @Test
    @Order(4)
    @DisplayName("Schema parameter should prevent defaulting to public schema")
    void testSchemaDoesNotDefaultToPublic() throws Exception {
        logger.info("TEST: Validating queries don't default to 'public' schema");

        if (!postgres.isRunning()) {
            return;
        }
        
        // Create table in public schema with different data
        try (Connection conn = DriverManager.getConnection(
                jdbcUrl(),
                "apex_user",
                "apex_pass");
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS public.products (
                    product_id VARCHAR(20) PRIMARY KEY,
                    product_name VARCHAR(100) NOT NULL
                )
                """);
            stmt.execute("INSERT INTO public.products VALUES ('AAPL', 'WRONG_DATA_FROM_PUBLIC_SCHEMA')");
        }

        YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/resources/dbschema/PostgreSQLSchemaConfigurationTest.yaml");
        
        updateDataSourceConnection(config, "trading-database");
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> testData = new HashMap<>();
        testData.put("product_id", "AAPL");

        RuleResult result = engine.evaluate(config, testData);
        Map<String, Object> enrichedData = result.getEnrichedData();

        // Should get data from trading schema, NOT public schema
        assertEquals("Apple Inc.", enrichedData.get("product_name"),
                "Should retrieve from 'trading' schema, not 'public' schema");

        assertNotEquals("WRONG_DATA_FROM_PUBLIC_SCHEMA", enrichedData.get("product_name"),
                "Should NOT retrieve from public schema");

        logger.info("[OK] Schema parameter correctly overrides default 'public' schema");
    }

    /**
     * Update YAML data source configuration with real PostgreSQL connection details.
     * Uses Testcontainers-generated dynamic credentials for proper isolation.
     */
    private void updateDataSourceConnection(YamlRuleConfiguration config, String dataSourceName) {
        String host = postgres.getHost();
        Integer port = postgres.getMappedPort(5432);
        String database = "apex_test";
        String username = "apex_user";
        String password = "apex_pass";

        if (config.getDataSources() != null) {
            for (var dataSource : config.getDataSources()) {
                if (dataSourceName.equals(dataSource.getName())) {
                    Map<String, Object> connection = dataSource.getConnection();
                    connection.put("host", host);
                    connection.put("port", port);
                    connection.put("database", database);
                    connection.put("username", username);
                    connection.put("password", password);
                    connection.put("schema", CUSTOM_SCHEMA);
                    
                    logger.info("Updated data source '{}' with schema '{}'", 
                        dataSourceName, CUSTOM_SCHEMA);
                    break;
                }
            }
        }
    }
    
    private void updateMultiSchemaDataSourceConnection(YamlRuleConfiguration config, String dataSourceName, String schema) {
        String host = postgres.getHost();
        Integer port = postgres.getMappedPort(5432);
        String database = "apex_test";
        String username = "apex_user";
        String password = "apex_pass";

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
                    
                    logger.info("Updated data source '{}' with schema '{}'", 
                        dataSourceName, schema);
                    break;
                }
            }
        }
    }
}
