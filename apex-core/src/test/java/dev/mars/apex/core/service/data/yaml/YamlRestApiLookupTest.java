package dev.mars.apex.core.service.data.yaml;

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


import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlDataSource;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;


import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for REST API data sources created from YAML configurations.
 *
 * This test class validates the complete YAML-to-RestAPI pipeline:
 * - Loading REST API configurations from YAML file
 * - REST API data source creation from YAML
 * - YAML configuration parsing for REST APIs
 * - Parameter binding in API configurations
 * - Authentication configuration
 * - Error handling for configuration issues
 *
 * The YAML configuration file is loaded from classpath: rest-api-lookup-test.yaml
 *
 * Note: This test focuses on YAML configuration validation rather than actual HTTP calls
 * to avoid external dependencies. For full HTTP testing, use integration test suites.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class YamlRestApiLookupTest {

    private static final String MOCK_API_URL = "https://api.example.com";

    private DataSourceFactory factory;
    private YamlConfigurationLoader yamlLoader;
    private YamlRuleConfiguration yamlConfig;
    private ExternalDataSource restApiSource;

    @BeforeEach
    void setUp() throws Exception {
        factory = DataSourceFactory.getInstance();
        yamlLoader = new YamlConfigurationLoader();

        // Set API base URL property for YAML resolution
        System.setProperty("API_BASE_URL", MOCK_API_URL);

        // Load YAML configuration from file
        yamlConfig = yamlLoader.loadFromClasspath("lookups/rest-api-lookup-test.yaml");
        assertNotNull(yamlConfig, "YAML configuration should be loaded");
        assertNotNull(yamlConfig.getDataSources(), "Data sources should be present");

        System.out.println("TEST: Loaded " + yamlConfig.getDataSources().size() + " data sources from YAML");
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

        // Clean up system properties
        System.clearProperty("API_BASE_URL");

        factory.clearCache();
    }

    /**
     * Find a data source by name from the loaded YAML configuration.
     */
    private YamlDataSource findDataSourceByName(String name) {
        return yamlConfig.getDataSources().stream()
            .filter(ds -> name.equals(ds.getName()))
            .findFirst()
            .orElse(null);
    }

    // ========================================
    // YAML Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should create REST API data source from YAML configuration")
    void testRestApiConfigurationFromYaml() throws DataSourceException {
        // Get REST API data source from YAML
        YamlDataSource yamlApi = findDataSourceByName("test-api");
        assertNotNull(yamlApi, "REST API data source should be in YAML");

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
        // Get parameterized REST API data source from YAML
        YamlDataSource yamlApi = findDataSourceByName("parameterized-api");
        assertNotNull(yamlApi, "Parameterized API data source should be in YAML");

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
        // Get authenticated REST API data source from YAML
        YamlDataSource yamlApi = findDataSourceByName("authenticated-api");
        assertNotNull(yamlApi, "Authenticated API data source should be in YAML");

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
        // Get complex REST API data source from YAML
        YamlDataSource yamlApi = findDataSourceByName("complex-api");
        assertNotNull(yamlApi, "Complex API data source should be in YAML");

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
}
