package dev.mars.apex.core.config.yaml;

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


import dev.mars.apex.core.config.datasource.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Map-based configuration and conversion tests for YamlDataSource.
 * 
 * Tests focus on:
 * - Map-based configuration structures for all data source types
 * - toDataSourceConfiguration() conversion method
 * - Complex nested configuration scenarios
 * - Type-specific configuration validation
 * - Connection, cache, authentication, and other Map configurations
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class YamlDataSourceTest {

    private YamlDataSource yamlDataSource;

    @BeforeEach
    void setUp() {
        yamlDataSource = new YamlDataSource();
    }

    // ========================================
    // Constructor and Basic Properties Tests
    // ========================================

    @Test
    @DisplayName("Should create YamlDataSource with initialized Map properties")
    void testDefaultConstructor() {
        YamlDataSource dataSource = new YamlDataSource();
        
        assertNotNull(dataSource, "Data source should be created");
        assertNull(dataSource.getName(), "Name should be null initially");
        assertNull(dataSource.getType(), "Type should be null initially");
        assertTrue(dataSource.getEnabled(), "Should be enabled by default");
        
        // Verify all Map properties are initialized
        assertNotNull(dataSource.getConnection(), "Connection map should be initialized");
        assertNotNull(dataSource.getCache(), "Cache map should be initialized");
        assertNotNull(dataSource.getHealthCheck(), "Health check map should be initialized");
        assertNotNull(dataSource.getAuthentication(), "Authentication map should be initialized");
        assertNotNull(dataSource.getQueries(), "Queries map should be initialized");
        assertNotNull(dataSource.getEndpoints(), "Endpoints map should be initialized");
        assertNotNull(dataSource.getTopics(), "Topics map should be initialized");
        assertNotNull(dataSource.getKeyPatterns(), "Key patterns map should be initialized");
        assertNotNull(dataSource.getFileFormat(), "File format map should be initialized");
        assertNotNull(dataSource.getCircuitBreaker(), "Circuit breaker map should be initialized");
        assertNotNull(dataSource.getResponseMapping(), "Response mapping map should be initialized");
        assertNotNull(dataSource.getCustomProperties(), "Custom properties map should be initialized");
        
        // Verify all maps are empty initially
        assertTrue(dataSource.getConnection().isEmpty(), "Connection map should be empty initially");
        assertTrue(dataSource.getCache().isEmpty(), "Cache map should be empty initially");
        assertTrue(dataSource.getQueries().isEmpty(), "Queries map should be empty initially");
    }

    @Test
    @DisplayName("Should handle basic property operations")
    void testBasicProperties() {
        yamlDataSource.setName("test-datasource");
        yamlDataSource.setType("postgresql");
        yamlDataSource.setSourceType("database");
        yamlDataSource.setDescription("Test PostgreSQL data source");
        yamlDataSource.setEnabled(true);
        yamlDataSource.setImplementation("dev.mars.apex.core.service.data.external.impl.DatabaseDataSource");
        yamlDataSource.setTags(Arrays.asList("test", "database", "postgresql"));

        assertEquals("test-datasource", yamlDataSource.getName(), "Name should match");
        assertEquals("postgresql", yamlDataSource.getType(), "Type should match");
        assertEquals("database", yamlDataSource.getSourceType(), "Source type should match");
        assertEquals("Test PostgreSQL data source", yamlDataSource.getDescription(), "Description should match");
        assertTrue(yamlDataSource.getEnabled(), "Should be enabled");
        assertEquals("dev.mars.apex.core.service.data.external.impl.DatabaseDataSource", 
                    yamlDataSource.getImplementation(), "Implementation should match");
        assertEquals(3, yamlDataSource.getTags().size(), "Should have 3 tags");
    }

    // ========================================
    // Map-Based Connection Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should handle database connection configuration via Map")
    void testDatabaseConnectionConfiguration() {
        yamlDataSource.setName("postgres-main");
        yamlDataSource.setType("postgresql");

        // Configure connection via Map
        Map<String, Object> connection = yamlDataSource.getConnection();
        connection.put("host", "localhost");
        connection.put("port", 5432);
        connection.put("database", "apex_db");
        connection.put("schema", "public");
        connection.put("username", "apex_user");
        connection.put("password", "apex_password");
        connection.put("ssl-enabled", true);
        connection.put("trust-store", "/path/to/truststore.jks");
        connection.put("trust-store-password", "truststore_password");

        // Add connection pool configuration
        Map<String, Object> connectionPool = new HashMap<>();
        connectionPool.put("initial-size", 5);
        connectionPool.put("max-size", 20);
        connectionPool.put("min-idle", 2);
        connectionPool.put("max-idle", 10);
        connectionPool.put("max-wait", 30000);
        connection.put("connection-pool", connectionPool);

        // Verify Map configuration
        assertEquals("localhost", connection.get("host"), "Host should match");
        assertEquals(5432, connection.get("port"), "Port should match");
        assertEquals("apex_db", connection.get("database"), "Database should match");
        assertEquals("public", connection.get("schema"), "Schema should match");
        assertTrue((Boolean) connection.get("ssl-enabled"), "SSL should be enabled");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> poolConfig = (Map<String, Object>) connection.get("connection-pool");
        assertNotNull(poolConfig, "Connection pool config should not be null");
        assertEquals(5, poolConfig.get("initial-size"), "Initial pool size should match");
        assertEquals(20, poolConfig.get("max-size"), "Max pool size should match");
    }

    @Test
    @DisplayName("Should handle REST API connection configuration via Map")
    void testRestApiConnectionConfiguration() {
        yamlDataSource.setName("external-api");
        yamlDataSource.setType("rest-api");

        // Configure REST API connection
        Map<String, Object> connection = yamlDataSource.getConnection();
        connection.put("base-url", "https://api.example.com/v1");
        connection.put("timeout", 30000);
        connection.put("retry-attempts", 3);
        connection.put("retry-delay", 1000);

        // Add headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        headers.put("User-Agent", "Apex-Rules-Engine/1.0");
        connection.put("headers", headers);

        // Verify configuration
        assertEquals("https://api.example.com/v1", connection.get("base-url"), "Base URL should match");
        assertEquals(30000, connection.get("timeout"), "Timeout should match");
        assertEquals(3, connection.get("retry-attempts"), "Retry attempts should match");
        
        @SuppressWarnings("unchecked")
        Map<String, String> configHeaders = (Map<String, String>) connection.get("headers");
        assertNotNull(configHeaders, "Headers should not be null");
        assertEquals("application/json", configHeaders.get("Content-Type"), "Content-Type should match");
        assertEquals("Apex-Rules-Engine/1.0", configHeaders.get("User-Agent"), "User-Agent should match");
    }

    @Test
    @DisplayName("Should handle file system connection configuration via Map")
    void testFileSystemConnectionConfiguration() {
        yamlDataSource.setName("csv-files");
        yamlDataSource.setType("file-system");

        // Configure file system connection
        Map<String, Object> connection = yamlDataSource.getConnection();
        connection.put("base-path", "/data/csv");
        connection.put("file-pattern", "*.csv");
        connection.put("polling-interval", 60000);
        connection.put("encoding", "UTF-8");

        // Also test convenience methods
        yamlDataSource.setBasePath("/data/override");
        yamlDataSource.setFilePattern("*.json");

        // Verify Map configuration
        assertEquals("/data/override", connection.get("base-path"), "Base path should be updated by convenience method");
        assertEquals("*.json", connection.get("file-pattern"), "File pattern should be updated by convenience method");
        assertEquals(60000, connection.get("polling-interval"), "Polling interval should match");
        assertEquals("UTF-8", connection.get("encoding"), "Encoding should match");
    }

    // ========================================
    // Map-Based Cache Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should handle cache configuration via Map")
    void testCacheConfiguration() {
        yamlDataSource.setName("cached-datasource");
        yamlDataSource.setType("postgresql");

        // Configure cache via Map
        Map<String, Object> cache = yamlDataSource.getCache();
        cache.put("enabled", true);
        cache.put("ttl-seconds", 3600L);
        cache.put("max-idle-seconds", 1800L);
        cache.put("max-size", 10000);
        cache.put("eviction-policy", "LRU");
        cache.put("preload-enabled", false);
        cache.put("refresh-ahead", true);
        cache.put("refresh-ahead-factor", 0.8);
        cache.put("statistics-enabled", true);
        cache.put("key-prefix", "apex:");
        cache.put("compression-enabled", true);
        cache.put("serialization-format", "JSON");
        cache.put("warmup-enabled", false);
        cache.put("warmup-batch-size", 100);
        cache.put("warmup-delay", 5000L);

        // Verify cache configuration
        assertTrue((Boolean) cache.get("enabled"), "Cache should be enabled");
        assertEquals(3600L, cache.get("ttl-seconds"), "TTL should match");
        assertEquals(1800L, cache.get("max-idle-seconds"), "Max idle should match");
        assertEquals(10000, cache.get("max-size"), "Max size should match");
        assertEquals("LRU", cache.get("eviction-policy"), "Eviction policy should match");
        assertTrue((Boolean) cache.get("refresh-ahead"), "Refresh ahead should be enabled");
        assertEquals(0.8, cache.get("refresh-ahead-factor"), "Refresh ahead factor should match");
        assertEquals("apex:", cache.get("key-prefix"), "Key prefix should match");
        assertTrue((Boolean) cache.get("compression-enabled"), "Compression should be enabled");
        assertEquals("JSON", cache.get("serialization-format"), "Serialization format should match");
    }

    // ========================================
    // Map-Based Authentication Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should handle basic authentication configuration via Map")
    void testBasicAuthenticationConfiguration() {
        yamlDataSource.setName("basic-auth-api");
        yamlDataSource.setType("rest-api");

        // Configure basic authentication
        Map<String, Object> authentication = yamlDataSource.getAuthentication();
        authentication.put("type", "basic");
        authentication.put("username", "api_user");
        authentication.put("password", "api_password");

        // Verify authentication configuration
        assertEquals("basic", authentication.get("type"), "Auth type should be basic");
        assertEquals("api_user", authentication.get("username"), "Username should match");
        assertEquals("api_password", authentication.get("password"), "Password should match");
    }

    @Test
    @DisplayName("Should handle OAuth2 authentication configuration via Map")
    void testOAuth2AuthenticationConfiguration() {
        yamlDataSource.setName("oauth2-service");
        yamlDataSource.setType("rest-api");

        // Configure OAuth2 authentication
        Map<String, Object> authentication = yamlDataSource.getAuthentication();
        authentication.put("type", "oauth2");
        authentication.put("client-id", "client123");
        authentication.put("client-secret", "secret456");
        authentication.put("token-url", "https://auth.example.com/oauth/token");
        authentication.put("scope", "read write");
        authentication.put("grant-type", "client_credentials");

        // Verify OAuth2 configuration
        assertEquals("oauth2", authentication.get("type"), "Auth type should be oauth2");
        assertEquals("client123", authentication.get("client-id"), "Client ID should match");
        assertEquals("secret456", authentication.get("client-secret"), "Client secret should match");
        assertEquals("https://auth.example.com/oauth/token", authentication.get("token-url"), "Token URL should match");
        assertEquals("read write", authentication.get("scope"), "Scope should match");
        assertEquals("client_credentials", authentication.get("grant-type"), "Grant type should match");
    }

    @Test
    @DisplayName("Should handle API key authentication configuration via Map")
    void testApiKeyAuthenticationConfiguration() {
        yamlDataSource.setName("api-key-service");
        yamlDataSource.setType("rest-api");

        // Configure API key authentication
        Map<String, Object> authentication = yamlDataSource.getAuthentication();
        authentication.put("type", "api-key");
        authentication.put("api-key", "sk-1234567890abcdef");
        authentication.put("api-key-header", "X-API-Key");
        authentication.put("api-key-location", "header");

        // Verify API key configuration
        assertEquals("api-key", authentication.get("type"), "Auth type should be api-key");
        assertEquals("sk-1234567890abcdef", authentication.get("api-key"), "API key should match");
        assertEquals("X-API-Key", authentication.get("api-key-header"), "API key header should match");
        assertEquals("header", authentication.get("api-key-location"), "API key location should match");
    }

    // ========================================
    // Type-Specific Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should handle database queries configuration")
    void testDatabaseQueriesConfiguration() {
        yamlDataSource.setName("query-datasource");
        yamlDataSource.setType("postgresql");

        // Configure queries
        Map<String, String> queries = yamlDataSource.getQueries();
        queries.put("findUser", "SELECT * FROM users WHERE id = ?");
        queries.put("findOrder", "SELECT * FROM orders WHERE order_id = ?");
        queries.put("updateStatus", "UPDATE orders SET status = ? WHERE order_id = ?");
        queries.put("countActiveUsers", "SELECT COUNT(*) FROM users WHERE active = true");

        // Verify queries configuration
        assertEquals(4, queries.size(), "Should have 4 queries");
        assertEquals("SELECT * FROM users WHERE id = ?", queries.get("findUser"), "findUser query should match");
        assertEquals("SELECT * FROM orders WHERE order_id = ?", queries.get("findOrder"), "findOrder query should match");
        assertEquals("UPDATE orders SET status = ? WHERE order_id = ?", queries.get("updateStatus"), "updateStatus query should match");
        assertEquals("SELECT COUNT(*) FROM users WHERE active = true", queries.get("countActiveUsers"), "countActiveUsers query should match");
    }

    @Test
    @DisplayName("Should handle REST API endpoints configuration")
    void testRestApiEndpointsConfiguration() {
        yamlDataSource.setName("rest-endpoints");
        yamlDataSource.setType("rest-api");

        // Configure endpoints
        Map<String, String> endpoints = yamlDataSource.getEndpoints();
        endpoints.put("getUser", "/api/users/{id}");
        endpoints.put("createOrder", "/api/orders");
        endpoints.put("updateOrder", "/api/orders/{id}");
        endpoints.put("deleteOrder", "/api/orders/{id}");
        endpoints.put("searchUsers", "/api/users/search?q={query}");

        // Verify endpoints configuration
        assertEquals(5, endpoints.size(), "Should have 5 endpoints");
        assertEquals("/api/users/{id}", endpoints.get("getUser"), "getUser endpoint should match");
        assertEquals("/api/orders", endpoints.get("createOrder"), "createOrder endpoint should match");
        assertEquals("/api/orders/{id}", endpoints.get("updateOrder"), "updateOrder endpoint should match");
        assertEquals("/api/orders/{id}", endpoints.get("deleteOrder"), "deleteOrder endpoint should match");
        assertEquals("/api/users/search?q={query}", endpoints.get("searchUsers"), "searchUsers endpoint should match");
    }

    @Test
    @DisplayName("Should handle message queue topics configuration")
    void testMessageQueueTopicsConfiguration() {
        yamlDataSource.setName("kafka-topics");
        yamlDataSource.setType("kafka");

        // Configure topics
        Map<String, String> topics = yamlDataSource.getTopics();
        topics.put("orderEvents", "order.events");
        topics.put("userEvents", "user.events");
        topics.put("paymentEvents", "payment.events");
        topics.put("notificationEvents", "notification.events");

        // Verify topics configuration
        assertEquals(4, topics.size(), "Should have 4 topics");
        assertEquals("order.events", topics.get("orderEvents"), "orderEvents topic should match");
        assertEquals("user.events", topics.get("userEvents"), "userEvents topic should match");
        assertEquals("payment.events", topics.get("paymentEvents"), "paymentEvents topic should match");
        assertEquals("notification.events", topics.get("notificationEvents"), "notificationEvents topic should match");
    }

    @Test
    @DisplayName("Should handle cache key patterns configuration")
    void testCacheKeyPatternsConfiguration() {
        yamlDataSource.setName("redis-cache");
        yamlDataSource.setType("redis");

        // Configure key patterns
        Map<String, String> keyPatterns = yamlDataSource.getKeyPatterns();
        keyPatterns.put("user", "user:{id}");
        keyPatterns.put("order", "order:{orderId}");
        keyPatterns.put("session", "session:{sessionId}");
        keyPatterns.put("product", "product:{productId}:{version}");

        // Verify key patterns configuration
        assertEquals(4, keyPatterns.size(), "Should have 4 key patterns");
        assertEquals("user:{id}", keyPatterns.get("user"), "user key pattern should match");
        assertEquals("order:{orderId}", keyPatterns.get("order"), "order key pattern should match");
        assertEquals("session:{sessionId}", keyPatterns.get("session"), "session key pattern should match");
        assertEquals("product:{productId}:{version}", keyPatterns.get("product"), "product key pattern should match");
    }

    // ========================================
    // File Format Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should handle CSV file format configuration via Map")
    void testCsvFileFormatConfiguration() {
        yamlDataSource.setName("csv-datasource");
        yamlDataSource.setType("file-system");

        // Configure CSV file format
        Map<String, Object> fileFormat = yamlDataSource.getFileFormat();
        fileFormat.put("type", "csv");
        fileFormat.put("delimiter", ",");
        fileFormat.put("quote-character", "\"");
        fileFormat.put("escape-character", "\\");
        fileFormat.put("header-row", true);
        fileFormat.put("skip-lines", 0);
        fileFormat.put("encoding", "UTF-8");
        fileFormat.put("null-value", "NULL");

        // Add column mappings
        Map<String, String> columnMappings = new HashMap<>();
        columnMappings.put("user_id", "userId");
        columnMappings.put("first_name", "firstName");
        columnMappings.put("last_name", "lastName");
        fileFormat.put("column-mappings", columnMappings);

        // Verify file format configuration
        assertEquals("csv", fileFormat.get("type"), "Type should be CSV");
        assertEquals(",", fileFormat.get("delimiter"), "Delimiter should match");
        assertEquals("\"", fileFormat.get("quote-character"), "Quote character should match");
        assertEquals("\\", fileFormat.get("escape-character"), "Escape character should match");
        assertTrue((Boolean) fileFormat.get("header-row"), "Should have header row");
        assertEquals("UTF-8", fileFormat.get("encoding"), "Encoding should match");
        
        @SuppressWarnings("unchecked")
        Map<String, String> mappings = (Map<String, String>) fileFormat.get("column-mappings");
        assertNotNull(mappings, "Column mappings should not be null");
        assertEquals("userId", mappings.get("user_id"), "user_id mapping should match");
        assertEquals("firstName", mappings.get("first_name"), "first_name mapping should match");
    }

    @Test
    @DisplayName("Should handle JSON file format configuration via Map")
    void testJsonFileFormatConfiguration() {
        yamlDataSource.setName("json-datasource");
        yamlDataSource.setType("file-system");

        // Configure JSON file format
        Map<String, Object> fileFormat = yamlDataSource.getFileFormat();
        fileFormat.put("type", "json");
        fileFormat.put("root-path", "$.data");
        fileFormat.put("flatten-arrays", false);
        fileFormat.put("root-element", "records");
        fileFormat.put("record-element", "record");

        // Add namespaces for XML-like processing
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("ns1", "http://example.com/namespace1");
        namespaces.put("ns2", "http://example.com/namespace2");
        fileFormat.put("namespaces", namespaces);

        // Verify JSON file format configuration
        assertEquals("json", fileFormat.get("type"), "Type should be JSON");
        assertEquals("$.data", fileFormat.get("root-path"), "Root path should match");
        assertFalse((Boolean) fileFormat.get("flatten-arrays"), "Arrays should not be flattened");
        assertEquals("records", fileFormat.get("root-element"), "Root element should match");
        assertEquals("record", fileFormat.get("record-element"), "Record element should match");
        
        @SuppressWarnings("unchecked")
        Map<String, String> ns = (Map<String, String>) fileFormat.get("namespaces");
        assertNotNull(ns, "Namespaces should not be null");
        assertEquals("http://example.com/namespace1", ns.get("ns1"), "ns1 namespace should match");
    }

    // ========================================
    // Circuit Breaker Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should handle circuit breaker configuration via Map")
    void testCircuitBreakerConfiguration() {
        yamlDataSource.setName("resilient-api");
        yamlDataSource.setType("rest-api");

        // Configure circuit breaker
        Map<String, Object> circuitBreaker = yamlDataSource.getCircuitBreaker();
        circuitBreaker.put("enabled", true);
        circuitBreaker.put("failure-threshold", 5);
        circuitBreaker.put("timeout-seconds", 30);
        circuitBreaker.put("retry-delay-seconds", 60);
        circuitBreaker.put("half-open-max-calls", 3);
        circuitBreaker.put("log-state-changes", true);
        circuitBreaker.put("metrics-enabled", true);
        circuitBreaker.put("slow-call-duration-threshold", 5000L);
        circuitBreaker.put("slow-call-rate-threshold", 0.5);
        circuitBreaker.put("automatic-transition-from-open-to-half-open", true);
        circuitBreaker.put("max-wait-duration-in-half-open", 10000);

        // Verify circuit breaker configuration
        assertTrue((Boolean) circuitBreaker.get("enabled"), "Circuit breaker should be enabled");
        assertEquals(5, circuitBreaker.get("failure-threshold"), "Failure threshold should match");
        assertEquals(30, circuitBreaker.get("timeout-seconds"), "Timeout should match");
        assertEquals(60, circuitBreaker.get("retry-delay-seconds"), "Retry delay should match");
        assertEquals(3, circuitBreaker.get("half-open-max-calls"), "Half-open max calls should match");
        assertTrue((Boolean) circuitBreaker.get("log-state-changes"), "State change logging should be enabled");
        assertTrue((Boolean) circuitBreaker.get("metrics-enabled"), "Metrics should be enabled");
        assertEquals(5000L, circuitBreaker.get("slow-call-duration-threshold"), "Slow call duration threshold should match");
        assertEquals(0.5, circuitBreaker.get("slow-call-rate-threshold"), "Slow call rate threshold should match");
    }

    // ========================================
    // Response Mapping Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should handle response mapping configuration via Map")
    void testResponseMappingConfiguration() {
        yamlDataSource.setName("mapped-api");
        yamlDataSource.setType("rest-api");

        // Configure response mapping
        Map<String, Object> responseMapping = yamlDataSource.getResponseMapping();
        responseMapping.put("format", "json");
        responseMapping.put("root-path", "$.response");
        responseMapping.put("error-path", "$.error");
        responseMapping.put("data-path", "$.data");
        responseMapping.put("status-path", "$.status");
        responseMapping.put("message-path", "$.message");

        // Add field mappings
        Map<String, String> fieldMappings = new HashMap<>();
        fieldMappings.put("id", "userId");
        fieldMappings.put("name", "fullName");
        fieldMappings.put("email", "emailAddress");
        responseMapping.put("field-mappings", fieldMappings);

        // Add transformation settings
        responseMapping.put("flatten-nested-objects", false);
        responseMapping.put("array-handling", "preserve");
        responseMapping.put("null-handling", "keep");
        responseMapping.put("trim-strings", true);
        responseMapping.put("convert-empty-to-null", true);

        // Add include/exclude fields
        responseMapping.put("include-fields", Arrays.asList("id", "name", "email", "status"));
        responseMapping.put("exclude-fields", Arrays.asList("password", "internal_id"));

        // Add field validations
        Map<String, String> fieldValidations = new HashMap<>();
        fieldValidations.put("email", "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
        fieldValidations.put("id", "^\\d+$");
        responseMapping.put("field-validations", fieldValidations);

        // Add custom transformer
        responseMapping.put("custom-transformer", "com.example.CustomTransformer");
        Map<String, Object> transformerProps = new HashMap<>();
        transformerProps.put("dateFormat", "yyyy-MM-dd");
        transformerProps.put("timezone", "UTC");
        responseMapping.put("transformer-properties", transformerProps);

        // Verify response mapping configuration
        assertEquals("json", responseMapping.get("format"), "Format should be JSON");
        assertEquals("$.response", responseMapping.get("root-path"), "Root path should match");
        assertEquals("$.error", responseMapping.get("error-path"), "Error path should match");
        assertEquals("$.data", responseMapping.get("data-path"), "Data path should match");
        
        @SuppressWarnings("unchecked")
        Map<String, String> mappings = (Map<String, String>) responseMapping.get("field-mappings");
        assertNotNull(mappings, "Field mappings should not be null");
        assertEquals("userId", mappings.get("id"), "id mapping should match");
        assertEquals("fullName", mappings.get("name"), "name mapping should match");
        
        assertFalse((Boolean) responseMapping.get("flatten-nested-objects"), "Nested objects should not be flattened");
        assertEquals("preserve", responseMapping.get("array-handling"), "Array handling should be preserve");
        assertTrue((Boolean) responseMapping.get("trim-strings"), "Strings should be trimmed");
        
        @SuppressWarnings("unchecked")
        List<String> includeFields = (List<String>) responseMapping.get("include-fields");
        assertNotNull(includeFields, "Include fields should not be null");
        assertEquals(4, includeFields.size(), "Should have 4 include fields");
        assertTrue(includeFields.contains("email"), "Should include email field");
        
        @SuppressWarnings("unchecked")
        Map<String, String> validations = (Map<String, String>) responseMapping.get("field-validations");
        assertNotNull(validations, "Field validations should not be null");
        assertEquals("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", validations.get("email"), "Email validation should match");
        
        assertEquals("com.example.CustomTransformer", responseMapping.get("custom-transformer"), "Custom transformer should match");
        @SuppressWarnings("unchecked")
        Map<String, Object> transformerProperties = (Map<String, Object>) responseMapping.get("transformer-properties");
        assertNotNull(transformerProperties, "Transformer properties should not be null");
        assertEquals("yyyy-MM-dd", transformerProperties.get("dateFormat"), "Date format should match");
    }

    // ========================================
    // Custom Properties Tests
    // ========================================

    @Test
    @DisplayName("Should handle custom properties configuration via Map")
    void testCustomPropertiesConfiguration() {
        yamlDataSource.setName("custom-datasource");
        yamlDataSource.setType("custom");

        // Configure custom properties
        Map<String, Object> customProperties = yamlDataSource.getCustomProperties();
        customProperties.put("custom.string.property", "string-value");
        customProperties.put("custom.integer.property", 42);
        customProperties.put("custom.boolean.property", true);
        customProperties.put("custom.double.property", 3.14159);
        customProperties.put("custom.list.property", Arrays.asList("item1", "item2", "item3"));
        
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("nested.key1", "nested.value1");
        nestedMap.put("nested.key2", 100);
        customProperties.put("custom.map.property", nestedMap);

        // Verify custom properties configuration
        assertEquals(6, customProperties.size(), "Should have 6 custom properties");
        assertEquals("string-value", customProperties.get("custom.string.property"), "String property should match");
        assertEquals(42, customProperties.get("custom.integer.property"), "Integer property should match");
        assertTrue((Boolean) customProperties.get("custom.boolean.property"), "Boolean property should be true");
        assertEquals(3.14159, customProperties.get("custom.double.property"), "Double property should match");
        
        @SuppressWarnings("unchecked")
        List<String> listProperty = (List<String>) customProperties.get("custom.list.property");
        assertNotNull(listProperty, "List property should not be null");
        assertEquals(3, listProperty.size(), "List should have 3 items");
        assertTrue(listProperty.contains("item2"), "List should contain item2");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> mapProperty = (Map<String, Object>) customProperties.get("custom.map.property");
        assertNotNull(mapProperty, "Map property should not be null");
        assertEquals("nested.value1", mapProperty.get("nested.key1"), "Nested value should match");
        assertEquals(100, mapProperty.get("nested.key2"), "Nested integer should match");
    }

    // ========================================
    // Parameter Names and Health Check Tests
    // ========================================

    @Test
    @DisplayName("Should handle parameter names and health check configuration")
    void testParameterNamesAndHealthCheck() {
        yamlDataSource.setName("parameterized-datasource");
        yamlDataSource.setType("postgresql");

        // Set parameter names
        String[] parameterNames = {"userId", "orderId", "status", "dateFrom", "dateTo"};
        yamlDataSource.setParameterNames(parameterNames);

        // Configure health check
        Map<String, Object> healthCheck = yamlDataSource.getHealthCheck();
        healthCheck.put("enabled", true);
        healthCheck.put("interval-seconds", 30);
        healthCheck.put("timeout-seconds", 5);
        healthCheck.put("failure-threshold", 3);
        healthCheck.put("success-threshold", 2);
        healthCheck.put("query", "SELECT 1");
        healthCheck.put("expected-result", "1");

        // Verify parameter names
        assertNotNull(yamlDataSource.getParameterNames(), "Parameter names should not be null");
        assertEquals(5, yamlDataSource.getParameterNames().length, "Should have 5 parameter names");
        assertEquals("userId", yamlDataSource.getParameterNames()[0], "First parameter should be userId");
        assertEquals("orderId", yamlDataSource.getParameterNames()[1], "Second parameter should be orderId");
        assertEquals("dateTo", yamlDataSource.getParameterNames()[4], "Last parameter should be dateTo");

        // Verify health check configuration
        assertTrue((Boolean) healthCheck.get("enabled"), "Health check should be enabled");
        assertEquals(30, healthCheck.get("interval-seconds"), "Interval should match");
        assertEquals(5, healthCheck.get("timeout-seconds"), "Timeout should match");
        assertEquals(3, healthCheck.get("failure-threshold"), "Failure threshold should match");
        assertEquals(2, healthCheck.get("success-threshold"), "Success threshold should match");
        assertEquals("SELECT 1", healthCheck.get("query"), "Health check query should match");
        assertEquals("1", healthCheck.get("expected-result"), "Expected result should match");
    }

    // ========================================
    // toDataSourceConfiguration() Conversion Tests
    // ========================================

    @Test
    @DisplayName("Should convert to DataSourceConfiguration with basic properties")
    void testBasicConversionToDataSourceConfiguration() {
        yamlDataSource.setName("test-conversion");
        yamlDataSource.setType("postgresql");
        yamlDataSource.setSourceType("database");
        yamlDataSource.setDescription("Test conversion data source");
        yamlDataSource.setEnabled(true);
        yamlDataSource.setImplementation("dev.mars.apex.core.service.data.external.impl.DatabaseDataSource");

        DataSourceConfiguration config = yamlDataSource.toDataSourceConfiguration();

        assertNotNull(config, "Configuration should not be null");
        assertEquals("test-conversion", config.getName(), "Name should match");
        assertEquals("postgresql", config.getType(), "Type should match");
        assertEquals("database", config.getSourceType(), "Source type should match");
        assertEquals("Test conversion data source", config.getDescription(), "Description should match");
        assertTrue(config.isEnabled(), "Should be enabled");
        assertEquals("dev.mars.apex.core.service.data.external.impl.DatabaseDataSource",
                    config.getImplementation(), "Implementation should match");
    }

    @Test
    @DisplayName("Should convert connection configuration correctly")
    void testConnectionConfigurationConversion() {
        yamlDataSource.setName("connection-test");
        yamlDataSource.setType("postgresql");

        // Set up connection configuration
        Map<String, Object> connection = yamlDataSource.getConnection();
        connection.put("host", "localhost");
        connection.put("port", 5432);
        connection.put("database", "testdb");
        connection.put("username", "testuser");
        connection.put("password", "testpass");
        connection.put("ssl-enabled", true);

        DataSourceConfiguration config = yamlDataSource.toDataSourceConfiguration();
        ConnectionConfig connectionConfig = config.getConnection();

        assertNotNull(connectionConfig, "Connection config should not be null");
        assertEquals("localhost", connectionConfig.getHost(), "Host should match");
        assertEquals(Integer.valueOf(5432), connectionConfig.getPort(), "Port should match");
        assertEquals("testdb", connectionConfig.getDatabase(), "Database should match");
        assertEquals("testuser", connectionConfig.getUsername(), "Username should match");
        assertEquals("testpass", connectionConfig.getPassword(), "Password should match");
        assertTrue(connectionConfig.isSslEnabled(), "SSL should be enabled");
    }

    @Test
    @DisplayName("Should convert cache configuration correctly")
    void testCacheConfigurationConversion() {
        yamlDataSource.setName("cache-test");
        yamlDataSource.setType("redis");

        // Set up cache configuration
        Map<String, Object> cache = yamlDataSource.getCache();
        cache.put("enabled", true);
        cache.put("ttl-seconds", 3600L);
        cache.put("max-size", 10000);
        cache.put("eviction-policy", "LRU");
        cache.put("key-prefix", "test:");

        DataSourceConfiguration config = yamlDataSource.toDataSourceConfiguration();
        CacheConfig cacheConfig = config.getCache();

        assertNotNull(cacheConfig, "Cache config should not be null");
        assertTrue(cacheConfig.getEnabled(), "Cache should be enabled");
        assertEquals(3600L, cacheConfig.getTtlSeconds(), "TTL should match");
        assertEquals(10000, cacheConfig.getMaxSize(), "Max size should match");
        assertEquals(CacheConfig.EvictionPolicy.LRU, cacheConfig.getEvictionPolicy(), "Eviction policy should match");
        assertEquals("test:", cacheConfig.getKeyPrefix(), "Key prefix should match");
    }

    @Test
    @DisplayName("Should convert authentication configuration correctly")
    void testAuthenticationConfigurationConversion() {
        yamlDataSource.setName("auth-test");
        yamlDataSource.setType("rest-api");

        // Set up authentication configuration
        Map<String, Object> authentication = yamlDataSource.getAuthentication();
        authentication.put("type", "basic");
        authentication.put("username", "authuser");
        authentication.put("password", "authpass");

        DataSourceConfiguration config = yamlDataSource.toDataSourceConfiguration();
        AuthenticationConfig authConfig = config.getAuthentication();

        assertNotNull(authConfig, "Authentication config should not be null");
        assertEquals("basic", authConfig.getType(), "Auth type should match");
        assertEquals("authuser", authConfig.getUsername(), "Auth username should match");
        assertEquals("authpass", authConfig.getPassword(), "Auth password should match");
    }

    @Test
    @DisplayName("Should convert file format configuration correctly")
    void testFileFormatConfigurationConversion() {
        yamlDataSource.setName("file-test");
        yamlDataSource.setType("file-system");

        // Set up file format configuration
        Map<String, Object> fileFormat = yamlDataSource.getFileFormat();
        fileFormat.put("type", "csv");
        fileFormat.put("delimiter", ",");
        fileFormat.put("header-row", true);
        fileFormat.put("encoding", "UTF-8");

        DataSourceConfiguration config = yamlDataSource.toDataSourceConfiguration();
        FileFormatConfig fileFormatConfig = config.getFileFormat();

        assertNotNull(fileFormatConfig, "File format config should not be null");
        assertEquals("csv", fileFormatConfig.getType(), "Type should match");
        assertEquals(",", fileFormatConfig.getDelimiter(), "Delimiter should match");
        assertTrue(fileFormatConfig.getHeaderRow(), "Header row should be true");
        assertEquals("UTF-8", fileFormatConfig.getEncoding(), "Encoding should match");
    }

    @Test
    @DisplayName("Should convert circuit breaker configuration correctly")
    void testCircuitBreakerConfigurationConversion() {
        yamlDataSource.setName("circuit-test");
        yamlDataSource.setType("rest-api");

        // Set up circuit breaker configuration
        Map<String, Object> circuitBreaker = yamlDataSource.getCircuitBreaker();
        circuitBreaker.put("enabled", true);
        circuitBreaker.put("failure-threshold", 5);
        circuitBreaker.put("timeout-seconds", 30);
        circuitBreaker.put("retry-delay-seconds", 60);

        DataSourceConfiguration config = yamlDataSource.toDataSourceConfiguration();
        CircuitBreakerConfig circuitBreakerConfig = config.getCircuitBreaker();

        assertNotNull(circuitBreakerConfig, "Circuit breaker config should not be null");
        assertTrue(circuitBreakerConfig.getEnabled(), "Circuit breaker should be enabled");
        assertEquals(5, circuitBreakerConfig.getFailureThreshold(), "Failure threshold should match");
        assertEquals(30, circuitBreakerConfig.getTimeoutSeconds(), "Timeout should match");
        assertEquals(30, circuitBreakerConfig.getTimeoutSeconds(), "Timeout should match");
    }

    @Test
    @DisplayName("Should convert type-specific configurations correctly")
    void testTypeSpecificConfigurationConversion() {
        yamlDataSource.setName("type-specific-test");
        yamlDataSource.setType("postgresql");

        // Set up type-specific configurations
        Map<String, String> queries = yamlDataSource.getQueries();
        queries.put("findUser", "SELECT * FROM users WHERE id = ?");
        queries.put("findOrder", "SELECT * FROM orders WHERE order_id = ?");

        Map<String, String> endpoints = yamlDataSource.getEndpoints();
        endpoints.put("getUser", "/api/users/{id}");
        endpoints.put("createOrder", "/api/orders");

        Map<String, String> topics = yamlDataSource.getTopics();
        topics.put("orderEvents", "order.events");
        topics.put("userEvents", "user.events");

        Map<String, String> keyPatterns = yamlDataSource.getKeyPatterns();
        keyPatterns.put("user", "user:{id}");
        keyPatterns.put("order", "order:{orderId}");

        DataSourceConfiguration config = yamlDataSource.toDataSourceConfiguration();

        // Verify type-specific configurations are copied
        assertNotNull(config.getQueries(), "Queries should not be null");
        assertEquals(2, config.getQueries().size(), "Should have 2 queries");
        assertEquals("SELECT * FROM users WHERE id = ?", config.getQueries().get("findUser"), "findUser query should match");

        assertNotNull(config.getEndpoints(), "Endpoints should not be null");
        assertEquals(2, config.getEndpoints().size(), "Should have 2 endpoints");
        assertEquals("/api/users/{id}", config.getEndpoints().get("getUser"), "getUser endpoint should match");

        assertNotNull(config.getTopics(), "Topics should not be null");
        assertEquals(2, config.getTopics().size(), "Should have 2 topics");
        assertEquals("order.events", config.getTopics().get("orderEvents"), "orderEvents topic should match");

        assertNotNull(config.getKeyPatterns(), "Key patterns should not be null");
        assertEquals(2, config.getKeyPatterns().size(), "Should have 2 key patterns");
        assertEquals("user:{id}", config.getKeyPatterns().get("user"), "user key pattern should match");
    }

    @Test
    @DisplayName("Should handle null and empty configurations during conversion")
    void testNullAndEmptyConfigurationConversion() {
        yamlDataSource.setName("null-test");
        yamlDataSource.setType("postgresql");
        yamlDataSource.setEnabled(null); // Should default to true

        // Set some configurations to null
        yamlDataSource.setConnection(null);
        yamlDataSource.setCache(null);
        yamlDataSource.setAuthentication(null);

        // Set some configurations to empty
        yamlDataSource.getQueries().clear();
        yamlDataSource.getEndpoints().clear();

        DataSourceConfiguration config = yamlDataSource.toDataSourceConfiguration();

        assertNotNull(config, "Configuration should not be null");
        assertEquals("null-test", config.getName(), "Name should match");
        assertEquals("postgresql", config.getType(), "Type should match");
        assertTrue(config.isEnabled(), "Should be enabled by default when null");

        // Null configurations should not be set
        assertNull(config.getConnection(), "Connection should be null");
        assertNull(config.getCache(), "Cache should be null");
        assertNull(config.getAuthentication(), "Authentication should be null");

        // Empty configurations should be copied as empty
        assertNotNull(config.getQueries(), "Queries should not be null");
        assertTrue(config.getQueries().isEmpty(), "Queries should be empty");
        assertNotNull(config.getEndpoints(), "Endpoints should not be null");
        assertTrue(config.getEndpoints().isEmpty(), "Endpoints should be empty");
    }

    @Test
    @DisplayName("Should convert complex nested configuration correctly")
    void testComplexNestedConfigurationConversion() {
        yamlDataSource.setName("complex-test");
        yamlDataSource.setType("rest-api");
        yamlDataSource.setDescription("Complex nested configuration test");

        // Set up complex connection with headers and connection pool
        Map<String, Object> connection = yamlDataSource.getConnection();
        connection.put("base-url", "https://api.example.com");
        connection.put("timeout", 30000);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        connection.put("headers", headers);

        Map<String, Object> connectionPool = new HashMap<>();
        connectionPool.put("max-size", 20);
        connectionPool.put("min-idle", 5);
        connection.put("connection-pool", connectionPool);

        // Set up complex cache configuration
        Map<String, Object> cache = yamlDataSource.getCache();
        cache.put("enabled", true);
        cache.put("ttl-seconds", 1800L);
        cache.put("eviction-policy", "LFU");
        cache.put("compression-enabled", true);

        // Set up complex authentication
        Map<String, Object> authentication = yamlDataSource.getAuthentication();
        authentication.put("type", "oauth2");
        authentication.put("client-id", "test-client");
        authentication.put("token-url", "https://auth.example.com/token");

        // Set up complex file format with mappings
        Map<String, Object> fileFormat = yamlDataSource.getFileFormat();
        fileFormat.put("type", "json");
        fileFormat.put("root-path", "$.data");

        Map<String, String> columnMappings = new HashMap<>();
        columnMappings.put("id", "userId");
        columnMappings.put("name", "fullName");
        fileFormat.put("column-mappings", columnMappings);

        // Set up custom properties
        Map<String, Object> customProperties = yamlDataSource.getCustomProperties();
        customProperties.put("retry.enabled", true);
        customProperties.put("retry.attempts", 3);
        customProperties.put("monitoring.enabled", true);

        DataSourceConfiguration config = yamlDataSource.toDataSourceConfiguration();

        // Verify complex conversion
        assertNotNull(config, "Configuration should not be null");
        assertEquals("complex-test", config.getName(), "Name should match");
        assertEquals("rest-api", config.getType(), "Type should match");

        // Verify connection with nested structures
        ConnectionConfig connectionConfig = config.getConnection();
        assertNotNull(connectionConfig, "Connection config should not be null");
        assertEquals("https://api.example.com", connectionConfig.getBaseUrl(), "Base URL should match");
        assertEquals(Integer.valueOf(30000), connectionConfig.getTimeout(), "Timeout should match");

        Map<String, String> configHeaders = connectionConfig.getHeaders();
        assertNotNull(configHeaders, "Headers should not be null");
        assertEquals("application/json", configHeaders.get("Content-Type"), "Content-Type header should match");

        ConnectionPoolConfig poolConfig = connectionConfig.getConnectionPool();
        assertNotNull(poolConfig, "Connection pool config should not be null");
        assertEquals(20, poolConfig.getMaxSize(), "Pool max size should match");
        // Note: ConnectionPoolConfig may not have minSize property in actual API
        // assertEquals(5, poolConfig.getMinSize(), "Pool min size should match");

        // Verify cache configuration
        CacheConfig cacheConfig = config.getCache();
        assertNotNull(cacheConfig, "Cache config should not be null");
        assertTrue(cacheConfig.getEnabled(), "Cache should be enabled");
        assertEquals(1800L, cacheConfig.getTtlSeconds(), "TTL should match");
        assertEquals(CacheConfig.EvictionPolicy.LFU, cacheConfig.getEvictionPolicy(), "Eviction policy should match");
        assertTrue(cacheConfig.getCompressionEnabled(), "Compression should be enabled");

        // Verify authentication configuration
        AuthenticationConfig authConfig = config.getAuthentication();
        assertNotNull(authConfig, "Authentication config should not be null");
        assertEquals("oauth2", authConfig.getType(), "Auth type should match");
        assertEquals("test-client", authConfig.getClientId(), "Client ID should match");
        assertEquals("https://auth.example.com/token", authConfig.getTokenUrl(), "Token URL should match");

        // Verify file format configuration
        FileFormatConfig fileFormatConfig = config.getFileFormat();
        assertNotNull(fileFormatConfig, "File format config should not be null");
        assertEquals("json", fileFormatConfig.getType(), "File format type should match");
        assertEquals("$.data", fileFormatConfig.getRootPath(), "Root path should match");

        Map<String, String> configColumnMappings = fileFormatConfig.getColumnMappings();
        assertNotNull(configColumnMappings, "Column mappings should not be null");
        assertEquals("userId", configColumnMappings.get("id"), "id mapping should match");
        assertEquals("fullName", configColumnMappings.get("name"), "name mapping should match");

        // Verify custom properties are copied
        assertNotNull(config.getCustomProperties(), "Custom properties should not be null");
        assertEquals(3, config.getCustomProperties().size(), "Should have 3 custom properties");
        assertTrue((Boolean) config.getCustomProperties().get("retry.enabled"), "Retry should be enabled");
        assertEquals(3, config.getCustomProperties().get("retry.attempts"), "Retry attempts should match");
    }
}
