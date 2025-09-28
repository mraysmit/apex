/*
 * Copyright 2024 APEX Process Consultants. This code is confidential and proprietary.
 * Unauthorized copying, distribution, or use is strictly prohibited.
 */
package dev.mars.apex.demo.config;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.config.yaml.YamlEnrichment;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Placeholder Resolution Test
 * 
 * This test suite validates various placeholder resolution scenarios:
 * 1. Database connection placeholders (host, port, credentials)
 * 2. Mixed placeholder syntax (${} and $() formats)
 * 3. Default value resolution
 * 4. Environment variable resolution
 * 5. Complex nested placeholders
 * 6. System property vs environment variable precedence
 * 
 * Demonstrates the full capability of APEX YAML property resolution system.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Comprehensive Placeholder Resolution Test")
class ComprehensivePlaceholderResolutionTest {

    private static final Logger logger = LoggerFactory.getLogger(ComprehensivePlaceholderResolutionTest.class);
    
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    void setUp() {
        logger.info("=== Setting up Comprehensive Placeholder Resolution Test ===");
        yamlLoader = new YamlConfigurationLoader();
        
        // Clear any existing system properties to ensure clean test state
        clearTestProperties();
    }

    @AfterEach
    void tearDown() {
        // Clean up all test properties
        clearTestProperties();
        logger.info("=== Cleaned up all test properties ===");
    }

    private void clearTestProperties() {
        // Database properties
        System.clearProperty("DB_HOST");
        System.clearProperty("DB_PORT");
        System.clearProperty("DB_NAME");
        System.clearProperty("DB_USER");
        System.clearProperty("DB_PASSWORD");
        System.clearProperty("DB_SCHEMA");
        System.clearProperty("DB_POOL_SIZE");
        System.clearProperty("DB_TIMEOUT");
        
        // API properties
        System.clearProperty("API_HOST");
        System.clearProperty("API_PORT");
        System.clearProperty("API_VERSION");
        System.clearProperty("API_KEY");
        System.clearProperty("API_SECRET");
        System.clearProperty("TIMEOUT");
        System.clearProperty("RETRY_COUNT");
        System.clearProperty("ENVIRONMENT");
        System.clearProperty("PAGE_SIZE");
        
        // Complex properties
        System.clearProperty("PROTOCOL");
        System.clearProperty("AUTH_HOST");
        System.clearProperty("AUTH_PORT");
        System.clearProperty("AUTH_TYPE");
        System.clearProperty("JWT_TOKEN");
        System.clearProperty("SYSTEM_NAME");
        
        // Environment-like properties
        System.clearProperty("HOSTNAME");
        System.clearProperty("SERVER_PORT");
        System.clearProperty("REQUEST_TIMEOUT");
        System.clearProperty("USERNAME");
        System.clearProperty("PASSWORD");
    }

    @Test
    @Order(1)
    @DisplayName("Should resolve database connection placeholders")
    void testDatabasePlaceholderResolution() {
        logger.info("=== Testing Database Placeholder Resolution ===");

        try {
            // Set database connection properties
            System.setProperty("DB_HOST", "db.example.com");
            System.setProperty("DB_PORT", "5432");
            System.setProperty("DB_NAME", "testdb");
            System.setProperty("DB_USER", "testuser");
            System.setProperty("DB_PASSWORD", "testpass123");
            // DB_SCHEMA not set - should use default "public"
            System.setProperty("DB_POOL_SIZE", "50");
            // DB_TIMEOUT not set - should use default "30000"
            
            logger.info("Set database properties");

            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/config/DatabasePlaceholderResolutionTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");
            
            // Verify configuration loaded successfully
            assertEquals("Database Placeholder Resolution Test", config.getMetadata().getName());
            logger.info("✓ YAML configuration loaded successfully");

            // Verify data source placeholders resolved
            YamlDataSource dataSource = config.getDataSources().get(0);
            Map<String, Object> connection = dataSource.getConnection();
            
            assertEquals("db.example.com", connection.get("host"));
            assertEquals(5432, connection.get("port"));
            assertEquals("testdb", connection.get("database"));
            assertEquals("testuser", connection.get("username"));
            assertEquals("testpass123", connection.get("password"));
            assertEquals("public", connection.get("schema")); // Default value
            assertEquals(50, connection.get("max-pool-size"));
            assertEquals(30000, connection.get("connection-timeout")); // Default value
            
            logger.info("✓ All database placeholders resolved correctly");
            logger.info("  - Host: {}", connection.get("host"));
            logger.info("  - Port: {}", connection.get("port"));
            logger.info("  - Database: {}", connection.get("database"));
            logger.info("  - Schema: {} (default)", connection.get("schema"));
            logger.info("  - Pool Size: {}", connection.get("max-pool-size"));
            logger.info("  - Timeout: {} (default)", connection.get("connection-timeout"));

            // Verify queries with placeholders
            Map<String, String> queries = dataSource.getQueries();
            assertTrue(queries.get("getUserById").contains("public.users"));
            assertTrue(queries.get("getOrdersByCustomer").contains("public.orders"));
            logger.info("✓ Query placeholders resolved correctly");

            logger.info("🎉 Database placeholder resolution test PASSED");

        } catch (Exception e) {
            logger.error("❌ Database placeholder resolution test FAILED: {}", e.getMessage(), e);
            fail("Database placeholder resolution failed: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should resolve mixed placeholder syntax")
    void testMixedPlaceholderSyntax() {
        logger.info("=== Testing Mixed Placeholder Syntax ===");

        try {
            // Set properties for both ${} and $() syntax
            System.setProperty("API_HOST", "api.example.com");
            System.setProperty("API_PORT", "443");
            System.setProperty("API_KEY", "key123");
            System.setProperty("API_SECRET", "secret456");
            // API_VERSION not set - should use default "1"
            // TIMEOUT not set - should use default "5000"
            System.setProperty("RETRY_COUNT", "5");
            System.setProperty("ENVIRONMENT", "production");
            System.setProperty("PAGE_SIZE", "100");
            
            logger.info("Set mixed syntax properties");

            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/config/MixedPlaceholderResolutionTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");
            
            // Verify data source placeholders resolved
            YamlDataSource dataSource = config.getDataSources().get(0);
            Map<String, Object> connection = dataSource.getConnection();
            
            // Test mixed syntax in base-url: "https://${API_HOST}:$(API_PORT)/api/v${API_VERSION:1}"
            String baseUrl = (String) connection.get("base-url");
            assertEquals("https://api.example.com:443/api/v1", baseUrl);
            logger.info("✓ Mixed syntax base URL: {}", baseUrl);
            
            // Test other mixed properties
            assertEquals(5000, connection.get("timeout")); // Default value
            assertEquals("key123", connection.get("api-key"));
            assertEquals("secret456", connection.get("secret"));
            assertEquals(5, connection.get("retry-attempts"));
            
            // Test endpoints with mixed syntax
            Map<String, String> endpoints = dataSource.getEndpoints();
            assertEquals("/health?env=production", endpoints.get("health"));
            assertEquals("/users?limit=100", endpoints.get("users"));
            
            logger.info("✓ All mixed syntax placeholders resolved correctly");
            logger.info("🎉 Mixed placeholder syntax test PASSED");

        } catch (Exception e) {
            logger.error("❌ Mixed placeholder syntax test FAILED: {}", e.getMessage(), e);
            fail("Mixed placeholder syntax failed: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should resolve default values when properties not set")
    void testDefaultValueResolution() {
        logger.info("=== Testing Default Value Resolution ===");

        try {
            // Intentionally NOT setting most properties to test defaults
            // Only set a few to verify non-default resolution still works
            System.setProperty("HOST", "custom.host.com");
            System.setProperty("SSL_ENABLED", "true");
            
            logger.info("Set minimal properties to test defaults");

            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/config/DefaultValuePlaceholderTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");
            
            // Verify data source default values
            YamlDataSource dataSource = config.getDataSources().get(0);
            Map<String, Object> connection = dataSource.getConnection();
            
            // Test base-url: "http://${HOST:localhost}:${PORT:8080}"
            String baseUrl = (String) connection.get("base-url");
            assertEquals("http://custom.host.com:8080", baseUrl); // HOST set, PORT default
            logger.info("✓ Base URL with mixed defaults: {}", baseUrl);
            
            // Test all default values
            assertEquals(5000, connection.get("timeout"));
            assertEquals(100, connection.get("max-connections"));
            assertEquals(true, connection.get("ssl-enabled")); // Explicitly set
            
            // Test cache defaults
            Map<String, Object> cache = dataSource.getCache();
            assertEquals(true, cache.get("enabled"));
            assertEquals(300, cache.get("ttl-seconds"));
            assertEquals(1000, cache.get("max-size"));
            
            // Test authentication defaults
            Map<String, Object> auth = dataSource.getAuthentication();
            assertEquals("none", auth.get("type"));
            assertEquals("guest", auth.get("username"));
            assertEquals("", auth.get("password")); // Empty string default
            
            logger.info("✓ All default values resolved correctly");
            logger.info("🎉 Default value resolution test PASSED");

        } catch (Exception e) {
            logger.error("❌ Default value resolution test FAILED: {}", e.getMessage(), e);
            fail("Default value resolution failed: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should resolve complex nested placeholders")
    void testComplexPlaceholderResolution() {
        logger.info("=== Testing Complex Placeholder Resolution ===");

        try {
            // Set complex nested properties
            System.setProperty("PROTOCOL", "https");
            System.setProperty("API_HOST", "complex.api.com");
            System.setProperty("API_PORT", "8443");
            System.setProperty("API_VERSION", "v2");
            System.setProperty("PAGE", "2");
            System.setProperty("SIZE", "25");
            System.setProperty("SORT", "name");
            System.setProperty("STATUS", "pending");
            System.setProperty("FROM_DATE", "2024-01-01");
            System.setProperty("TO_DATE", "2024-12-31");
            System.setProperty("AUTH_TYPE", "oauth2");
            System.setProperty("JWT_TOKEN", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9");
            System.setProperty("SYSTEM_NAME", "APEX-PROD");
            System.setProperty("ENVIRONMENT", "production");

            logger.info("Set complex nested properties");

            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/config/ComplexPlaceholderTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Verify complex base-url: "${PROTOCOL:https}://${API_HOST}:${API_PORT}/api/${API_VERSION:v1}"
            YamlDataSource dataSource = config.getDataSources().get(0);
            Map<String, Object> connection = dataSource.getConnection();
            String baseUrl = (String) connection.get("base-url");
            assertEquals("https://complex.api.com:8443/api/v2", baseUrl);
            logger.info("✓ Complex base URL: {}", baseUrl);

            // Verify complex endpoints
            Map<String, String> endpoints = dataSource.getEndpoints();
            assertEquals("/users?page=2&size=25&sort=name", endpoints.get("users"));
            assertEquals("/orders?status=pending&from=2024-01-01&to=2024-12-31", endpoints.get("orders"));
            logger.info("✓ Complex endpoints resolved correctly");

            // Verify nested placeholder in refresh-url: "${PROTOCOL:https}://${AUTH_HOST:${API_HOST}}:${AUTH_PORT:${API_PORT}}/auth/refresh"
            Map<String, Object> auth = dataSource.getAuthentication();
            String refreshUrl = (String) auth.get("refresh-url");
            assertEquals("https://complex.api.com:8443/auth/refresh", refreshUrl);
            logger.info("✓ Nested placeholder refresh URL: {}", refreshUrl);

            // Verify enrichment with complex expression
            YamlEnrichment enrichment = config.getEnrichments().get(0);
            YamlEnrichment.CalculationConfig calcConfig = enrichment.getCalculationConfig();
            String expression = calcConfig.getExpression();
            assertEquals("'APEX-PROD-' + #orderId + '-' + 'production'", expression);
            logger.info("✓ Complex enrichment expression: {}", expression);

            logger.info("✓ All complex placeholders resolved correctly");
            logger.info("🎉 Complex placeholder resolution test PASSED");

        } catch (Exception e) {
            logger.error("❌ Complex placeholder resolution test FAILED: {}", e.getMessage(), e);
            fail("Complex placeholder resolution failed: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should handle missing required properties")
    void testMissingRequiredProperties() {
        logger.info("=== Testing Missing Required Properties ===");

        try {
            // Set some properties but leave critical ones missing
            System.setProperty("API_HOST", "test.com");
            // API_KEY missing - should cause failure

            logger.info("Set partial properties - API_KEY missing");

            // Attempt to load YAML - should fail
            Exception exception = assertThrows(Exception.class, () -> {
                yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/config/MixedPlaceholderResolutionTest.yaml");
            });

            // Verify the exception indicates missing property
            String errorMessage = exception.getMessage();
            assertTrue(errorMessage.contains("Property not found: API_KEY"),
                      "Exception should indicate API_KEY property not found, but was: " + errorMessage);

            logger.info("✓ Expected failure for missing API_KEY: {}", errorMessage);
            logger.info("🎉 Missing required properties test PASSED");

        } catch (AssertionError e) {
            logger.error("❌ Missing required properties test FAILED: {}", e.getMessage());
            throw e;
        }
    }

    @Test
    @Order(6)
    @DisplayName("Should demonstrate system property vs environment variable precedence")
    void testPropertyPrecedence() {
        logger.info("=== Testing System Property vs Environment Variable Precedence ===");

        try {
            // Set system property that might conflict with environment variable
            System.setProperty("HOSTNAME", "system-property-host");
            System.setProperty("USERNAME", "system-user");
            // PASSWORD not set as system property - will check environment

            logger.info("Set system properties for precedence test");

            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/config/EnvironmentPlaceholderTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Verify system properties take precedence
            YamlDataSource dataSource = config.getDataSources().get(0);
            Map<String, Object> connection = dataSource.getConnection();
            String baseUrl = (String) connection.get("base-url");
            assertTrue(baseUrl.contains("system-property-host"),
                      "Should use system property value, got: " + baseUrl);

            Map<String, Object> auth = dataSource.getAuthentication();
            assertEquals("system-user", auth.get("username"));
            logger.info("✓ System properties take precedence over environment variables");

            logger.info("🎉 Property precedence test PASSED");

        } catch (Exception e) {
            logger.error("❌ Property precedence test FAILED: {}", e.getMessage(), e);
            fail("Property precedence test failed: " + e.getMessage());
        }
    }
}
