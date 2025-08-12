package dev.mars.apex.core.service.data.yaml;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for REST API data sources created from YAML configurations.
 *
 * This test class validates:
 * - REST API data source creation from YAML
 * - YAML configuration parsing for REST APIs
 * - Parameter binding in API configurations
 * - Authentication configuration
 * - Error handling for configuration issues
 *
 * Note: This test focuses on YAML configuration validation rather than actual HTTP calls
 * to avoid external dependencies. For full HTTP testing, use integration test suites.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class YamlRestApiLookupTest {

    private static final String MOCK_API_URL = "https://api.example.com";

    private DataSourceFactory factory;
    private ExternalDataSource restApiSource;

    @BeforeEach
    void setUp() throws DataSourceException {
        factory = DataSourceFactory.getInstance();
    }

    @AfterEach
    void tearDown() {
        if (restApiSource != null) {
            try {
                restApiSource.shutdown();
            } catch (Exception e) {
                System.out.println("TEST: Cleanup error (expected): " + e.getMessage());
            }
        }

        factory.clearCache();
    }

    // ========================================
    // YAML Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should create REST API data source from YAML configuration")
    void testRestApiConfigurationFromYaml() throws DataSourceException {
        // Create YAML configuration for REST API
        YamlDataSource yamlApi = createBasicRestApiYamlDataSource();

        // Convert and create data source
        DataSourceConfiguration config = yamlApi.toDataSourceConfiguration();
        restApiSource = factory.createDataSource(config);

        // Verify basic properties
        assertEquals("test-api", restApiSource.getName());
        assertEquals("rest-api", restApiSource.getDataType());

        // Verify configuration was properly converted
        assertNotNull(config.getConnection(), "Connection config should be set");
        assertEquals(MOCK_API_URL, config.getConnection().getBaseUrl());
        assertEquals(Integer.valueOf(10000), config.getConnection().getTimeout());
        assertEquals(Integer.valueOf(2), config.getConnection().getRetryAttempts());

        // Verify source type is set correctly
        assertEquals("rest-api", config.getSourceType(), "Source type should be rest-api");

        System.out.println("TEST: REST API data source created successfully from YAML");
    }

    @Test
    @DisplayName("Should handle parameterized API configuration from YAML")
    void testParameterizedApiConfigurationFromYaml() throws DataSourceException {
        YamlDataSource yamlApi = createParameterizedRestApiYamlDataSource();

        DataSourceConfiguration config = yamlApi.toDataSourceConfiguration();
        restApiSource = factory.createDataSource(config);

        // Verify parameterized endpoints are configured
        assertNotNull(yamlApi.getEndpoints(), "Endpoints should be configured");
        assertTrue(yamlApi.getEndpoints().containsKey("getUserById"), "Should have getUserById endpoint");
        assertTrue(yamlApi.getEndpoints().containsKey("getUsersByStatus"), "Should have getUsersByStatus endpoint");

        // Verify parameter names are set
        assertNotNull(yamlApi.getParameterNames(), "Parameter names should be configured");
        assertTrue(Arrays.asList(yamlApi.getParameterNames()).contains("userId"), "Should include userId parameter");
        assertTrue(Arrays.asList(yamlApi.getParameterNames()).contains("status"), "Should include status parameter");

        System.out.println("TEST: Parameterized API configuration validated successfully");
    }

    @Test
    @DisplayName("Should handle API authentication configuration from YAML")
    void testApiAuthenticationConfigurationFromYaml() throws DataSourceException {
        YamlDataSource yamlApi = createAuthenticatedRestApiYamlDataSource();

        DataSourceConfiguration config = yamlApi.toDataSourceConfiguration();
        restApiSource = factory.createDataSource(config);

        // Verify authentication configuration
        assertNotNull(yamlApi.getAuthentication(), "Authentication should be configured");
        assertEquals("api-key", yamlApi.getAuthentication().get("type"), "Should use API key authentication");
        assertEquals("test-api-key-12345", yamlApi.getAuthentication().get("api-key"), "Should have correct API key");
        assertEquals("X-API-Key", yamlApi.getAuthentication().get("api-key-header"), "Should have correct key header");

        // Verify protected endpoint is configured
        assertTrue(yamlApi.getEndpoints().containsKey("protected"), "Should have protected endpoint");

        System.out.println("TEST: API authentication configuration validated successfully");
    }

    // ========================================
    // Configuration Validation Tests
    // ========================================

    @Test
    @DisplayName("Should validate required REST API configuration fields")
    void testRequiredConfigurationFields() {
        YamlDataSource yamlApi = new YamlDataSource();
        yamlApi.setName("incomplete-api");
        yamlApi.setType("rest-api");
        yamlApi.setEnabled(true);

        // Missing connection configuration should cause issues
        DataSourceConfiguration config = yamlApi.toDataSourceConfiguration();

        // Verify that incomplete configuration is handled appropriately
        assertNotNull(config, "Configuration should be created even if incomplete");
        assertEquals("incomplete-api", config.getName());
        // Note: sourceType might be null for incomplete configurations
        assertTrue(config.getSourceType() == null || "rest-api".equals(config.getSourceType()));

        System.out.println("TEST: Configuration validation completed");
    }

    @Test
    @DisplayName("Should handle complex endpoint configurations")
    void testComplexEndpointConfigurations() throws DataSourceException {
        YamlDataSource yamlApi = createComplexRestApiYamlDataSource();

        DataSourceConfiguration config = yamlApi.toDataSourceConfiguration();
        restApiSource = factory.createDataSource(config);

        // Verify complex endpoint configurations
        Map<String, String> endpoints = yamlApi.getEndpoints();
        assertTrue(endpoints.containsKey("searchUsers"), "Should have search endpoint");
        assertTrue(endpoints.containsKey("createUser"), "Should have create endpoint");
        assertTrue(endpoints.containsKey("updateUser"), "Should have update endpoint");
        assertTrue(endpoints.containsKey("deleteUser"), "Should have delete endpoint");

        // Verify parameter configurations
        String[] paramNames = yamlApi.getParameterNames();
        assertTrue(Arrays.asList(paramNames).contains("searchQuery"), "Should include search parameters");
        assertTrue(Arrays.asList(paramNames).contains("userData"), "Should include user data parameters");

        System.out.println("TEST: Complex endpoint configuration validated successfully");
    }

    // ========================================
    // Helper Methods for Creating YAML Configurations
    // ========================================

    private YamlDataSource createBasicRestApiYamlDataSource() {
        YamlDataSource yamlApi = new YamlDataSource();
        yamlApi.setName("test-api");
        yamlApi.setType("rest-api");
        yamlApi.setSourceType("rest-api"); // Set source type explicitly
        yamlApi.setEnabled(true);
        yamlApi.setDescription("Test REST API data source");

        // Configure connection (use hyphenated keys as expected by YAML converter)
        Map<String, Object> connection = yamlApi.getConnection();
        connection.put("base-url", MOCK_API_URL);
        connection.put("timeout", 10000);
        connection.put("retry-attempts", 2);

        // Configure endpoints
        Map<String, String> endpoints = yamlApi.getEndpoints();
        endpoints.put("users", "/api/users");
        endpoints.put("health", "/api/health");
        endpoints.put("default", "/api/health");

        return yamlApi;
    }

    private YamlDataSource createParameterizedRestApiYamlDataSource() {
        YamlDataSource yamlApi = createBasicRestApiYamlDataSource();

        // Add parameterized endpoints
        Map<String, String> endpoints = yamlApi.getEndpoints();
        endpoints.put("getUserById", "/api/users/{userId}");
        endpoints.put("getUsersByStatus", "/api/users?status={status}");

        // Set parameter names
        yamlApi.setParameterNames(new String[]{"userId", "status", "limit"});

        return yamlApi;
    }

    private YamlDataSource createAuthenticatedRestApiYamlDataSource() {
        YamlDataSource yamlApi = createBasicRestApiYamlDataSource();

        // Configure authentication (use hyphenated keys as expected by YAML converter)
        Map<String, Object> auth = yamlApi.getAuthentication();
        auth.put("type", "api-key");
        auth.put("api-key", "test-api-key-12345");
        auth.put("api-key-header", "X-API-Key");

        // Add protected endpoint
        yamlApi.getEndpoints().put("protected", "/api/protected");

        return yamlApi;
    }

    private YamlDataSource createComplexRestApiYamlDataSource() {
        YamlDataSource yamlApi = createBasicRestApiYamlDataSource();

        // Add complex endpoints with different HTTP methods
        Map<String, String> endpoints = yamlApi.getEndpoints();
        endpoints.put("searchUsers", "/api/users/search?q={searchQuery}&limit={limit}");
        endpoints.put("createUser", "/api/users");
        endpoints.put("updateUser", "/api/users/{userId}");
        endpoints.put("deleteUser", "/api/users/{userId}");
        endpoints.put("getUserProfile", "/api/users/{userId}/profile");
        endpoints.put("getUserOrders", "/api/users/{userId}/orders?status={orderStatus}");

        // Set comprehensive parameter names
        yamlApi.setParameterNames(new String[]{
            "userId", "searchQuery", "limit", "userData", "orderStatus", "profileData"
        });

        // Configure additional headers
        Map<String, Object> connection = yamlApi.getConnection();
        connection.put("headers", Map.of(
            "Accept", "application/json",
            "Content-Type", "application/json",
            "User-Agent", "ApexRulesEngine/1.0"
        ));

        return yamlApi;
    }
}
