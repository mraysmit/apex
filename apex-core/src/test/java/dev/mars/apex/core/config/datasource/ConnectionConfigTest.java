package dev.mars.apex.core.config.datasource;

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


import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ConnectionConfig.
 * 
 * Tests cover:
 * - Constructor behavior and initialization
 * - Database connection properties (host, port, database, schema, credentials, SSL)
 * - HTTP/REST API properties (baseUrl, timeout, retries, headers)
 * - Message queue properties (bootstrap servers, security, SASL)
 * - File system properties (basePath, filePattern, polling, encoding)
 * - Connection pooling configuration
 * - Custom properties management
 * - Validation logic for all property types
 * - Copy method deep cloning behavior
 * - Equals and hashCode contracts
 * - ToString representation
 * - Edge cases and error handling
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class ConnectionConfigTest {

    private ConnectionConfig config;

    @BeforeEach
    void setUp() {
        config = new ConnectionConfig();
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    @DisplayName("Should initialize with default constructor")
    void testDefaultConstructor() {
        ConnectionConfig config = new ConnectionConfig();
        
        // Database properties should be null or default values
        assertNull(config.getHost());
        assertNull(config.getPort());
        assertNull(config.getDatabase());
        assertNull(config.getSchema());
        assertNull(config.getUsername());
        assertNull(config.getPassword());
        assertFalse(config.isSslEnabled()); // Default is false
        assertNull(config.getTrustStore());
        assertNull(config.getTrustStorePassword());
        
        // HTTP properties should be null or default values
        assertNull(config.getBaseUrl());
        assertEquals(30000, config.getTimeout()); // Default is 30 seconds
        assertEquals(3, config.getRetryAttempts()); // Default is 3
        assertEquals(1000, config.getRetryDelay()); // Default is 1 second
        
        // Message queue properties should be null
        assertNull(config.getBootstrapServers());
        assertNull(config.getSecurityProtocol());
        assertNull(config.getSaslMechanism());
        
        // File system properties should be null or default values
        assertNull(config.getBasePath());
        assertNull(config.getFilePattern());
        assertNull(config.getPollingInterval());
        assertEquals("UTF-8", config.getEncoding()); // Default is UTF-8
        
        // Connection pool should be null
        assertNull(config.getConnectionPool());
        
        // Maps should be initialized but empty
        assertNotNull(config.getHeaders());
        assertNotNull(config.getCustomProperties());
        assertTrue(config.getHeaders().isEmpty());
        assertTrue(config.getCustomProperties().isEmpty());
    }

    // ========================================
    // Database Connection Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get database connection properties")
    void testDatabaseProperties() {
        config.setHost("localhost");
        config.setPort(5432);
        config.setDatabase("testdb");
        config.setSchema("public");
        config.setUsername("testuser");
        config.setPassword("testpass");
        config.setSslEnabled(true);
        config.setTrustStore("/path/to/truststore");
        config.setTrustStorePassword("trustpass");
        
        assertEquals("localhost", config.getHost());
        assertEquals(5432, config.getPort());
        assertEquals("testdb", config.getDatabase());
        assertEquals("public", config.getSchema());
        assertEquals("testuser", config.getUsername());
        assertEquals("testpass", config.getPassword());
        assertTrue(config.isSslEnabled());
        assertEquals("/path/to/truststore", config.getTrustStore());
        assertEquals("trustpass", config.getTrustStorePassword());
    }

    @Test
    @DisplayName("Should handle null database properties")
    void testNullDatabaseProperties() {
        config.setHost(null);
        config.setPort(null);
        config.setDatabase(null);
        config.setSchema(null);
        config.setUsername(null);
        config.setPassword(null);
        config.setSslEnabled(false);
        config.setTrustStore(null);
        config.setTrustStorePassword(null);
        
        assertNull(config.getHost());
        assertNull(config.getPort());
        assertNull(config.getDatabase());
        assertNull(config.getSchema());
        assertNull(config.getUsername());
        assertNull(config.getPassword());
        assertFalse(config.isSslEnabled());
        assertNull(config.getTrustStore());
        assertNull(config.getTrustStorePassword());
    }

    // ========================================
    // HTTP/REST API Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get HTTP/REST API properties")
    void testHttpProperties() {
        config.setBaseUrl("https://api.example.com");
        config.setTimeout(60000);
        config.setRetryAttempts(5);
        config.setRetryDelay(2000);
        
        assertEquals("https://api.example.com", config.getBaseUrl());
        assertEquals(60000, config.getTimeout());
        assertEquals(5, config.getRetryAttempts());
        assertEquals(2000, config.getRetryDelay());
    }

    @Test
    @DisplayName("Should handle null HTTP properties")
    void testNullHttpProperties() {
        config.setBaseUrl(null);
        config.setTimeout(null);
        config.setRetryAttempts(null);
        config.setRetryDelay(null);
        
        assertNull(config.getBaseUrl());
        assertNull(config.getTimeout());
        assertNull(config.getRetryAttempts());
        assertNull(config.getRetryDelay());
    }

    @Test
    @DisplayName("Should manage HTTP headers")
    void testHttpHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token123");
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        config.setHeaders(headers);
        assertEquals(headers, config.getHeaders());

        // Test individual header addition
        config.addHeader("X-Custom-Header", "custom-value");
        assertEquals("custom-value", config.getHeaders().get("X-Custom-Header"));
    }

    @Test
    @DisplayName("Should handle null headers with defensive copying")
    void testNullHeaders() {
        config.setHeaders(null);
        
        assertNotNull(config.getHeaders());
        assertTrue(config.getHeaders().isEmpty());
        
        // Should be able to add headers after setting null
        config.addHeader("Test", "Value");
        assertEquals("Value", config.getHeaders().get("Test"));
    }

    @Test
    @DisplayName("Should allow modification of returned headers map")
    void testHeadersModification() {
        config.getHeaders().put("Test-Header", "test-value");
        assertEquals("test-value", config.getHeaders().get("Test-Header"));
        
        config.addHeader("Another-Header", "another-value");
        assertEquals("another-value", config.getHeaders().get("Another-Header"));
    }

    // ========================================
    // Message Queue Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get message queue properties")
    void testMessageQueueProperties() {
        config.setBootstrapServers("localhost:9092,localhost:9093");
        config.setSecurityProtocol("SASL_SSL");
        config.setSaslMechanism("PLAIN");
        
        assertEquals("localhost:9092,localhost:9093", config.getBootstrapServers());
        assertEquals("SASL_SSL", config.getSecurityProtocol());
        assertEquals("PLAIN", config.getSaslMechanism());
    }

    @Test
    @DisplayName("Should handle null message queue properties")
    void testNullMessageQueueProperties() {
        config.setBootstrapServers(null);
        config.setSecurityProtocol(null);
        config.setSaslMechanism(null);
        
        assertNull(config.getBootstrapServers());
        assertNull(config.getSecurityProtocol());
        assertNull(config.getSaslMechanism());
    }

    // ========================================
    // File System Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get file system properties")
    void testFileSystemProperties() {
        config.setBasePath("/data/files");
        config.setFilePattern("*.json");
        config.setPollingInterval(5000);
        config.setEncoding("ISO-8859-1");
        
        assertEquals("/data/files", config.getBasePath());
        assertEquals("*.json", config.getFilePattern());
        assertEquals(5000, config.getPollingInterval());
        assertEquals("ISO-8859-1", config.getEncoding());
    }

    @Test
    @DisplayName("Should handle null file system properties")
    void testNullFileSystemProperties() {
        config.setBasePath(null);
        config.setFilePattern(null);
        config.setPollingInterval(null);
        config.setEncoding(null);
        
        assertNull(config.getBasePath());
        assertNull(config.getFilePattern());
        assertNull(config.getPollingInterval());
        assertNull(config.getEncoding());
    }

    // ========================================
    // Connection Pool Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should set and get connection pool configuration")
    void testConnectionPoolConfiguration() {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMinSize(10);
        poolConfig.setMaxSize(50);
        
        config.setConnectionPool(poolConfig);
        
        assertSame(poolConfig, config.getConnectionPool());
        assertEquals(10, config.getConnectionPool().getMinSize());
        assertEquals(50, config.getConnectionPool().getMaxSize());
    }

    @Test
    @DisplayName("Should handle null connection pool configuration")
    void testNullConnectionPoolConfiguration() {
        config.setConnectionPool(null);
        assertNull(config.getConnectionPool());
    }

    // ========================================
    // Custom Properties Tests
    // ========================================

    @Test
    @DisplayName("Should set and get custom properties")
    void testCustomProperties() {
        Map<String, Object> customProps = new HashMap<>();
        customProps.put("maxConnections", 100);
        customProps.put("enableCompression", true);
        customProps.put("region", "us-east-1");
        customProps.put("priority", 5.5);

        config.setCustomProperties(customProps);
        assertEquals(customProps, config.getCustomProperties());
    }

    @Test
    @DisplayName("Should handle null custom properties")
    void testNullCustomProperties() {
        config.setCustomProperties(null);

        assertNotNull(config.getCustomProperties());
        assertTrue(config.getCustomProperties().isEmpty());
    }

    @Test
    @DisplayName("Should set and get individual custom properties")
    void testIndividualCustomProperties() {
        config.setCustomProperty("timeout", 5000);
        config.setCustomProperty("retries", 3);
        config.setCustomProperty("debug", true);
        config.setCustomProperty("factor", 2.5);

        assertEquals(5000, config.getCustomProperty("timeout"));
        assertEquals(3, config.getCustomProperty("retries"));
        assertEquals(true, config.getCustomProperty("debug"));
        assertEquals(2.5, config.getCustomProperty("factor"));

        assertNull(config.getCustomProperty("nonexistent"));
    }

    @Test
    @DisplayName("Should handle null keys and values in custom properties")
    void testCustomPropertiesNullHandling() {
        config.setCustomProperty("nullValue", null);
        config.setCustomProperty(null, "nullKey");

        assertNull(config.getCustomProperty("nullValue"));
        assertEquals("nullKey", config.getCustomProperty(null));
    }

    // ========================================
    // Validation Tests
    // ========================================

    @Test
    @DisplayName("Should validate successfully with valid configuration")
    void testValidConfiguration() {
        config.setTimeout(30000);
        config.setRetryAttempts(3);
        config.setRetryDelay(1000);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should fail validation with negative timeout")
    void testValidationNegativeTimeout() {
        config.setTimeout(-1000);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Timeout must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with zero timeout")
    void testValidationZeroTimeout() {
        config.setTimeout(0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Timeout must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with negative retry attempts")
    void testValidationNegativeRetryAttempts() {
        config.setRetryAttempts(-1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Retry attempts cannot be negative", exception.getMessage());
    }

    @Test
    @DisplayName("Should allow zero retry attempts")
    void testValidationZeroRetryAttempts() {
        config.setRetryAttempts(0);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should fail validation with negative retry delay")
    void testValidationNegativeRetryDelay() {
        config.setRetryDelay(-500);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Retry delay cannot be negative", exception.getMessage());
    }

    @Test
    @DisplayName("Should allow zero retry delay")
    void testValidationZeroRetryDelay() {
        config.setRetryDelay(0);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should validate connection pool if present")
    void testValidationWithConnectionPool() {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMinSize(10);
        poolConfig.setMaxSize(5); // Invalid: max < min
        config.setConnectionPool(poolConfig);

        // This should trigger validation of the connection pool
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertTrue(exception.getMessage().contains("max") || exception.getMessage().contains("Min"));
    }

    @Test
    @DisplayName("Should handle null values in validation")
    void testValidationWithNullValues() {
        config.setTimeout(null);
        config.setRetryAttempts(null);
        config.setRetryDelay(null);
        config.setConnectionPool(null);

        assertDoesNotThrow(() -> config.validate());
    }

    // ========================================
    // Copy Method Tests
    // ========================================

    @Test
    @DisplayName("Should create deep copy with all properties")
    void testCopyMethod() {
        // Set up original configuration with all properties
        config.setHost("localhost");
        config.setPort(5432);
        config.setDatabase("testdb");
        config.setSchema("public");
        config.setUsername("testuser");
        config.setPassword("testpass");
        config.setSslEnabled(true);
        config.setTrustStore("/path/to/truststore");
        config.setTrustStorePassword("trustpass");

        config.setBaseUrl("https://api.example.com");
        config.setTimeout(60000);
        config.setRetryAttempts(5);
        config.setRetryDelay(2000);
        config.getHeaders().put("Authorization", "Bearer token");

        config.setBootstrapServers("localhost:9092");
        config.setSecurityProtocol("SASL_SSL");
        config.setSaslMechanism("PLAIN");

        config.setBasePath("/data/files");
        config.setFilePattern("*.json");
        config.setPollingInterval(5000);
        config.setEncoding("ISO-8859-1");

        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMinSize(10);
        config.setConnectionPool(poolConfig);

        config.getCustomProperties().put("custom", "value");

        // Create copy
        ConnectionConfig copy = config.copy();

        // Verify database properties are copied
        assertEquals(config.getHost(), copy.getHost());
        assertEquals(config.getPort(), copy.getPort());
        assertEquals(config.getDatabase(), copy.getDatabase());
        assertEquals(config.getSchema(), copy.getSchema());
        assertEquals(config.getUsername(), copy.getUsername());
        assertEquals(config.getPassword(), copy.getPassword());
        assertEquals(config.isSslEnabled(), copy.isSslEnabled());
        assertEquals(config.getTrustStore(), copy.getTrustStore());
        assertEquals(config.getTrustStorePassword(), copy.getTrustStorePassword());

        // Verify HTTP properties are copied
        assertEquals(config.getBaseUrl(), copy.getBaseUrl());
        assertEquals(config.getTimeout(), copy.getTimeout());
        assertEquals(config.getRetryAttempts(), copy.getRetryAttempts());
        assertEquals(config.getRetryDelay(), copy.getRetryDelay());

        // Verify headers are deep copied
        assertNotSame(config.getHeaders(), copy.getHeaders());
        assertEquals(config.getHeaders(), copy.getHeaders());

        // Verify message queue properties are copied
        assertEquals(config.getBootstrapServers(), copy.getBootstrapServers());
        assertEquals(config.getSecurityProtocol(), copy.getSecurityProtocol());
        assertEquals(config.getSaslMechanism(), copy.getSaslMechanism());

        // Verify file system properties are copied
        assertEquals(config.getBasePath(), copy.getBasePath());
        assertEquals(config.getFilePattern(), copy.getFilePattern());
        assertEquals(config.getPollingInterval(), copy.getPollingInterval());
        assertEquals(config.getEncoding(), copy.getEncoding());

        // Verify connection pool is deep copied
        assertNotSame(config.getConnectionPool(), copy.getConnectionPool());
        assertEquals(config.getConnectionPool().getMinSize(), copy.getConnectionPool().getMinSize());

        // Verify custom properties are deep copied
        assertNotSame(config.getCustomProperties(), copy.getCustomProperties());
        assertEquals(config.getCustomProperties(), copy.getCustomProperties());
    }

    @Test
    @DisplayName("Should handle null values in copy method")
    void testCopyWithNullValues() {
        // Leave most properties as null, set only a few
        config.setHost("localhost");
        config.setTimeout(30000);

        ConnectionConfig copy = config.copy();

        assertEquals("localhost", copy.getHost());
        assertEquals(30000, copy.getTimeout());

        // All other properties should be null or default values
        assertNull(copy.getPort());
        assertNull(copy.getDatabase());
        assertNull(copy.getBaseUrl());
        assertNull(copy.getBootstrapServers());
        assertNull(copy.getBasePath());
        assertNull(copy.getConnectionPool());

        // Maps should be empty but not null
        assertNotNull(copy.getHeaders());
        assertNotNull(copy.getCustomProperties());
        assertTrue(copy.getHeaders().isEmpty());
        assertTrue(copy.getCustomProperties().isEmpty());

        // Default values should be preserved
        assertEquals(3, copy.getRetryAttempts());
        assertEquals(1000, copy.getRetryDelay());
        assertEquals("UTF-8", copy.getEncoding());
        assertFalse(copy.isSslEnabled());
    }

    @Test
    @DisplayName("Should create independent copy that can be modified")
    void testCopyIndependence() {
        config.setHost("original");
        config.getHeaders().put("original", "value");
        config.getCustomProperties().put("original", "prop");

        ConnectionConfig copy = config.copy();

        // Modify original
        config.setHost("modified");
        config.getHeaders().put("new", "header");
        config.getCustomProperties().put("new", "property");

        // Copy should remain unchanged
        assertEquals("original", copy.getHost());
        assertEquals(1, copy.getHeaders().size());
        assertEquals("value", copy.getHeaders().get("original"));
        assertNull(copy.getHeaders().get("new"));
        assertEquals(1, copy.getCustomProperties().size());
        assertEquals("prop", copy.getCustomProperties().get("original"));
        assertNull(copy.getCustomProperties().get("new"));

        // Modify copy
        copy.setHost("copy-modified");
        copy.getHeaders().put("copy", "header");
        copy.getCustomProperties().put("copy", "property");

        // Original should remain unchanged
        assertEquals("modified", config.getHost());
        assertEquals(2, config.getHeaders().size());
        assertNull(config.getHeaders().get("copy"));
        assertEquals(2, config.getCustomProperties().size());
        assertNull(config.getCustomProperties().get("copy"));
    }

    // ========================================
    // Equals and HashCode Tests
    // ========================================

    @Test
    @DisplayName("Should be equal to itself")
    void testEqualsReflexive() {
        config.setHost("localhost");
        config.setPort(5432);

        assertEquals(config, config);
        assertEquals(config.hashCode(), config.hashCode());
    }

    @Test
    @DisplayName("Should be equal to another instance with same properties")
    void testEqualsSymmetric() {
        config.setHost("localhost");
        config.setPort(5432);
        config.setDatabase("testdb");
        config.setUsername("testuser");
        config.setBaseUrl("https://api.example.com");
        config.setTimeout(30000);
        config.setSslEnabled(true);

        ConnectionConfig other = new ConnectionConfig();
        other.setHost("localhost");
        other.setPort(5432);
        other.setDatabase("testdb");
        other.setUsername("testuser");
        other.setBaseUrl("https://api.example.com");
        other.setTimeout(30000);
        other.setSslEnabled(true);

        assertEquals(config, other);
        assertEquals(other, config);
        assertEquals(config.hashCode(), other.hashCode());
    }

    @Test
    @DisplayName("Should not be equal to null")
    void testEqualsNull() {
        config.setHost("localhost");

        assertNotEquals(config, null);
    }

    @Test
    @DisplayName("Should not be equal to different class")
    void testEqualsDifferentClass() {
        config.setHost("localhost");

        assertNotEquals(config, "not a ConnectionConfig");
        assertNotEquals(config, new Object());
    }

    @Test
    @DisplayName("Should not be equal when hosts differ")
    void testEqualsHostDifference() {
        config.setHost("localhost");
        config.setPort(5432);

        ConnectionConfig other = new ConnectionConfig();
        other.setHost("remotehost");
        other.setPort(5432);

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when ports differ")
    void testEqualsPortDifference() {
        config.setHost("localhost");
        config.setPort(5432);

        ConnectionConfig other = new ConnectionConfig();
        other.setHost("localhost");
        other.setPort(3306);

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when databases differ")
    void testEqualsDatabaseDifference() {
        config.setHost("localhost");
        config.setDatabase("testdb");

        ConnectionConfig other = new ConnectionConfig();
        other.setHost("localhost");
        other.setDatabase("proddb");

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when usernames differ")
    void testEqualsUsernameDifference() {
        config.setHost("localhost");
        config.setUsername("user1");

        ConnectionConfig other = new ConnectionConfig();
        other.setHost("localhost");
        other.setUsername("user2");

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when base URLs differ")
    void testEqualsBaseUrlDifference() {
        config.setBaseUrl("https://api1.example.com");

        ConnectionConfig other = new ConnectionConfig();
        other.setBaseUrl("https://api2.example.com");

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when timeouts differ")
    void testEqualsTimeoutDifference() {
        config.setTimeout(30000);

        ConnectionConfig other = new ConnectionConfig();
        other.setTimeout(60000);

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when SSL settings differ")
    void testEqualsSslDifference() {
        config.setHost("localhost");
        config.setSslEnabled(true);

        ConnectionConfig other = new ConnectionConfig();
        other.setHost("localhost");
        other.setSslEnabled(false);

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should handle null values in equals comparison")
    void testEqualsWithNullValues() {
        ConnectionConfig config1 = new ConnectionConfig();
        ConnectionConfig config2 = new ConnectionConfig();

        // Both have all null values (except defaults)
        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());

        // One has null host, other has value
        config1.setHost("localhost");
        assertNotEquals(config1, config2);

        // Both have same host
        config2.setHost("localhost");
        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    // ========================================
    // ToString Tests
    // ========================================

    @Test
    @DisplayName("Should generate meaningful toString representation")
    void testToString() {
        config.setHost("localhost");
        config.setPort(5432);
        config.setDatabase("testdb");
        config.setBaseUrl("https://api.example.com");
        config.setTimeout(60000);
        config.setSslEnabled(true);

        String result = config.toString();

        assertNotNull(result);
        assertTrue(result.contains("localhost"));
        assertTrue(result.contains("5432"));
        assertTrue(result.contains("testdb"));
        assertTrue(result.contains("https://api.example.com"));
        assertTrue(result.contains("60000"));
        assertTrue(result.contains("true"));
        assertTrue(result.contains("ConnectionConfig"));
    }

    @Test
    @DisplayName("Should handle null values in toString")
    void testToStringWithNulls() {
        // Leave most properties as null, set only timeout
        config.setTimeout(30000);

        String result = config.toString();

        assertNotNull(result);
        assertTrue(result.contains("ConnectionConfig"));
        assertTrue(result.contains("30000"));
        assertTrue(result.contains("null"));
        assertTrue(result.contains("false")); // SSL enabled default
    }

    @Test
    @DisplayName("Should be consistent toString output")
    void testToStringConsistency() {
        config.setHost("consistent");
        config.setPort(1234);

        String result1 = config.toString();
        String result2 = config.toString();

        assertEquals(result1, result2);
    }

    @Test
    @DisplayName("Should include key connection properties in toString")
    void testToStringKeyProperties() {
        config.setHost("dbhost");
        config.setPort(3306);
        config.setDatabase("mydb");
        config.setBaseUrl("https://myapi.com");
        config.setTimeout(45000);
        config.setSslEnabled(false);

        String result = config.toString();

        // Verify all key properties are included
        assertTrue(result.contains("host='dbhost'"));
        assertTrue(result.contains("port=3306"));
        assertTrue(result.contains("database='mydb'"));
        assertTrue(result.contains("baseUrl='https://myapi.com'"));
        assertTrue(result.contains("timeout=45000"));
        assertTrue(result.contains("sslEnabled=false"));
    }
}
