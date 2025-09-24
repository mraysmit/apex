package dev.mars.apex.demo.lookup;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that external-data-config YAML files can be loaded and validated.
 * This test validates the external-data-config-database-test.yaml file structure and content.
 */
@DisplayName("External Data Config Validation Test")
class ExternalDataConfigValidationTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ExternalDataConfigValidationTest.class);

    @Test
    @DisplayName("Should successfully load and validate external-data-config YAML file")
    void testExternalDataConfigYamlLoading() {
        logger.info("=== Testing External Data Config YAML Loading ===");

        try {
            // Load the external data config YAML file
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/lookup/external-data-config-database-test.yaml");

            // Verify basic metadata
            assertNotNull(config, "Configuration should not be null");
            assertNotNull(config.getMetadata(), "Metadata should not be null");
            assertEquals("external-data-config-database-test", config.getMetadata().getId());
            assertEquals("External Data Config Database Test", config.getMetadata().getName());
            assertEquals("external-data-config", config.getMetadata().getType());

            // Verify data sources are loaded
            assertNotNull(config.getDataSources(), "Data sources should not be null");
            assertEquals(3, config.getDataSources().size(), "Should have 3 data sources");

            // Verify specific data sources
            var customerDb = config.getDataSources().stream()
                .filter(ds -> "customer-database".equals(ds.getName()))
                .findFirst()
                .orElse(null);
            assertNotNull(customerDb, "Customer database should be present");
            assertEquals("database", customerDb.getType());
            assertEquals("postgresql", customerDb.getSourceType());

            var transactionDb = config.getDataSources().stream()
                .filter(ds -> "transaction-database".equals(ds.getName()))
                .findFirst()
                .orElse(null);
            assertNotNull(transactionDb, "Transaction database should be present");
            assertEquals("database", transactionDb.getType());
            assertEquals("h2", transactionDb.getSourceType());

            var referenceDb = config.getDataSources().stream()
                .filter(ds -> "reference-database".equals(ds.getName()))
                .findFirst()
                .orElse(null);
            assertNotNull(referenceDb, "Reference database should be present");
            assertEquals("database", referenceDb.getType());
            assertEquals("mysql", referenceDb.getSourceType());

            logger.info("✓ External data config YAML loaded successfully");
            logger.info("  - Configuration ID: {}", config.getMetadata().getId());
            logger.info("  - Configuration Type: {}", config.getMetadata().getType());
            logger.info("  - Data Sources Count: {}", config.getDataSources().size());
            logger.info("  - Customer DB Type: {}", customerDb.getSourceType());
            logger.info("  - Transaction DB Type: {}", transactionDb.getSourceType());
            logger.info("  - Reference DB Type: {}", referenceDb.getSourceType());

        } catch (Exception e) {
            logger.error("Failed to load external data config YAML: {}", e.getMessage(), e);
            fail("External data config YAML loading failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should validate data source configurations")
    void testDataSourceConfigurationValidation() {
        logger.info("=== Testing Data Source Configuration Validation ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/lookup/external-data-config-database-test.yaml");

            // Test customer database configuration
            var customerDb = config.getDataSources().stream()
                .filter(ds -> "customer-database".equals(ds.getName()))
                .findFirst()
                .orElseThrow();

            // Verify connection details (accessing as Map)
            assertNotNull(customerDb.getConnection(), "Connection should not be null");
            @SuppressWarnings("unchecked")
            var connection = (java.util.Map<String, Object>) customerDb.getConnection();
            assertEquals("localhost", connection.get("host"));
            assertEquals(5432, connection.get("port"));
            assertEquals("customer_db", connection.get("database"));
            assertEquals("app_user", connection.get("username"));
            assertEquals("${DB_PASSWORD}", connection.get("password"));

            // Verify queries
            assertNotNull(customerDb.getQueries(), "Queries should not be null");
            assertTrue(customerDb.getQueries().containsKey("getCustomerById"));
            assertTrue(customerDb.getQueries().containsKey("getCustomerByEmail"));
            assertTrue(customerDb.getQueries().containsKey("getAllActiveCustomers"));

            // Verify cache configuration (accessing as Map)
            assertNotNull(customerDb.getCache(), "Cache config should not be null");
            @SuppressWarnings("unchecked")
            var cache = (java.util.Map<String, Object>) customerDb.getCache();
            assertEquals(true, cache.get("enabled"));
            assertEquals(300, cache.get("ttlSeconds"));
            assertEquals(1000, cache.get("maxSize"));

            // Verify health check configuration (accessing as Map)
            assertNotNull(customerDb.getHealthCheck(), "Health check config should not be null");
            @SuppressWarnings("unchecked")
            var healthCheck = (java.util.Map<String, Object>) customerDb.getHealthCheck();
            assertEquals(true, healthCheck.get("enabled"));
            assertEquals("SELECT 1", healthCheck.get("query"));
            assertEquals(60, healthCheck.get("intervalSeconds"));

            logger.info("✓ Data source configuration validation passed");
            logger.info("  - Connection Host: {}", connection.get("host"));
            logger.info("  - Connection Port: {}", connection.get("port"));
            logger.info("  - Queries Count: {}", customerDb.getQueries().size());
            logger.info("  - Cache Enabled: {}", cache.get("enabled"));
            logger.info("  - Health Check Enabled: {}", healthCheck.get("enabled"));

        } catch (Exception e) {
            logger.error("Data source configuration validation failed: {}", e.getMessage(), e);
            fail("Data source configuration validation failed: " + e.getMessage());
        }
    }
}
