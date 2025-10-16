package dev.mars.apex.core.service.data.external.cache;

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
import dev.mars.apex.core.service.data.external.*;
import org.junit.jupiter.api.*;


import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Comprehensive unit tests for CacheDataSource.
 * 
 * Tests cover:
 * - Constructor and initialization
 * - Cache operations (put, get, remove, clear)
 * - TTL behavior and expiration
 * - Statistics tracking
 * - Configuration handling
 * - Error handling and edge cases
 * - ExternalDataSource interface compliance
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class CacheDataSourceTest {



    private CacheDataSource cacheDataSource;
    private DataSourceConfiguration validConfig;

    @BeforeEach
    void setUp() {
        // Create valid configuration
        validConfig = createValidCacheConfiguration();

        // Create cache data source (not initialized yet)
        cacheDataSource = new CacheDataSource(validConfig);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (cacheDataSource != null) {
            cacheDataSource.shutdown();
        }
    }

    // ========================================
    // Constructor and Initialization Tests
    // ========================================

    @Test
    @DisplayName("Should create CacheDataSource with valid configuration")
    void testConstructorWithValidConfiguration() {
        CacheDataSource ds = new CacheDataSource(validConfig);
        
        assertNotNull(ds);
        assertEquals("test-cache", ds.getName());
        assertEquals("memory", ds.getDataType());
        assertFalse(ds.isHealthy()); // Not initialized yet
    }

    @Test
    @DisplayName("Should handle null configuration in constructor")
    void testConstructorWithNullConfiguration() {
        CacheDataSource ds = new CacheDataSource(null);
        
        assertNotNull(ds);
        assertEquals("cache-source", ds.getName()); // Default name
        assertEquals("cache", ds.getDataType()); // Default type
    }

    @Test
    @DisplayName("Should initialize successfully with valid configuration")
    void testInitializeWithValidConfiguration() throws Exception {
        CacheDataSource ds = new CacheDataSource(validConfig);
        assertDoesNotThrow(() -> ds.initialize(validConfig));
        assertTrue(ds.isHealthy());
        assertEquals(validConfig, ds.getConfiguration());
        assertNotNull(ds.getConnectionStatus());
        ds.shutdown();
    }

    @Test
    @DisplayName("Should handle initialization with invalid configuration")
    void testInitializeWithInvalidConfiguration() {
        DataSourceConfiguration invalidConfig = new DataSourceConfiguration();
        invalidConfig.setName("invalid-cache");
        invalidConfig.setType("cache");
        invalidConfig.setSourceType("unsupported-cache-type");
        
        CacheDataSource ds = new CacheDataSource(invalidConfig);
        
        // Should not throw during construction, but initialization would fail
        assertNotNull(ds);
        assertEquals("invalid-cache", ds.getName());
    }

    // ========================================
    // Basic Interface Implementation Tests
    // ========================================

    @Test
    @DisplayName("Should return correct name and data type")
    void testBasicProperties() {
        assertEquals("test-cache", cacheDataSource.getName());
        assertEquals("memory", cacheDataSource.getDataType());
        assertEquals(validConfig, cacheDataSource.getConfiguration());
    }

    @Test
    @DisplayName("Should support cache data types")
    void testSupportsDataType() {
        assertTrue(cacheDataSource.supportsDataType("memory"));
        assertTrue(cacheDataSource.supportsDataType("cache"));
        assertFalse(cacheDataSource.supportsDataType("database"));
        assertFalse(cacheDataSource.supportsDataType(null));
    }

    @Test
    @DisplayName("Should handle connection status correctly")
    void testConnectionStatus() {
        ConnectionStatus status = cacheDataSource.getConnectionStatus();
        assertNotNull(status);
        // Initially should be not initialized
        assertFalse(status.isConnected());
    }

    @Test
    @DisplayName("Should provide metrics")
    void testMetrics() {
        DataSourceMetrics metrics = cacheDataSource.getMetrics();
        assertNotNull(metrics);
    }

    // ========================================
    // Cache Operations Tests (Direct Methods)
    // ========================================

    @Test
    @DisplayName("Should handle put operation when cache manager is null")
    void testPutWithNullCacheManager() {
        // Cache manager is null before initialization
        assertDoesNotThrow(() -> {
            cacheDataSource.put("test-key", "test-value");
            cacheDataSource.put("test-key", "test-value", 300);
        });
    }

    @Test
    @DisplayName("Should handle get operation when cache manager is null")
    void testGetWithNullCacheManager() {
        Object result = cacheDataSource.get("test-key");
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle remove operation when cache manager is null")
    void testRemoveWithNullCacheManager() {
        boolean result = cacheDataSource.remove("test-key");
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle containsKey operation when cache manager is null")
    void testContainsKeyWithNullCacheManager() {
        boolean result = cacheDataSource.containsKey("test-key");
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle clear operation when cache manager is null")
    void testClearWithNullCacheManager() {
        assertDoesNotThrow(() -> cacheDataSource.clear());
    }

    @Test
    @DisplayName("Should handle size operation when cache manager is null")
    void testSizeWithNullCacheManager() {
        long size = cacheDataSource.size();
        assertEquals(0, size);
    }

    // ========================================
    // ExternalDataSource Interface Tests
    // ========================================

    @Test
    @DisplayName("Should handle getData with null cache manager")
    void testGetDataWithNullCacheManager() {
        Object result = cacheDataSource.getData("memory", "test-key");
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle getData with unsupported data type")
    void testGetDataWithUnsupportedType() {
        Object result = cacheDataSource.getData("database", "test-key");
        assertNull(result);
    }

    @Test
    @DisplayName("Should throw exception for query operation when cache manager is null")
    void testQueryOperation() {
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            cacheDataSource.query("test-pattern", new HashMap<>());
        });

        assertEquals(DataSourceException.ErrorType.EXECUTION_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Cache query failed"));
    }

    @Test
    @DisplayName("Should throw exception for queryForObject operation when cache manager is null")
    void testQueryForObjectOperation() {
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            cacheDataSource.queryForObject("test-key", new HashMap<>());
        });

        assertEquals(DataSourceException.ErrorType.EXECUTION_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Cache query failed"));
    }

    @Test
    @DisplayName("Should throw exception for batchQuery operation when cache manager is null")
    void testBatchQueryOperation() {
        List<String> queries = Arrays.asList("pattern1", "pattern2", "pattern3");

        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            cacheDataSource.batchQuery(queries);
        });

        assertEquals(DataSourceException.ErrorType.EXECUTION_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Cache query failed"));
    }

    @Test
    @DisplayName("Should throw exception for batchUpdate operation when cache manager is null")
    void testBatchUpdateOperation() {
        List<String> updates = Arrays.asList(
            "key1=value1",
            "key2=value2",
            "key3=value3"
        );

        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            cacheDataSource.batchUpdate(updates);
        });

        assertEquals(DataSourceException.ErrorType.EXECUTION_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Cache batch update failed"));
    }

    @Test
    @DisplayName("Should throw exception for invalid batch update format when cache manager is null")
    void testBatchUpdateWithInvalidFormat() {
        List<String> updates = Arrays.asList(
            "key1=value1",
            "invalid-format", // Missing equals sign
            "key3=value3"
        );

        // Should throw exception because cache manager is null
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            cacheDataSource.batchUpdate(updates);
        });

        assertEquals(DataSourceException.ErrorType.EXECUTION_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Cache batch update failed"));
    }

    // ========================================
    // Connection and Health Tests
    // ========================================

    @Test
    @DisplayName("Should return false for testConnection when cache manager is null")
    void testConnectionWithNullCacheManager() {
        boolean result = cacheDataSource.testConnection();
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for isHealthy when not initialized")
    void testIsHealthyWhenNotInitialized() {
        boolean result = cacheDataSource.isHealthy();
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle refresh operation")
    void testRefreshOperation() {
        assertDoesNotThrow(() -> {
            cacheDataSource.refresh();
        });
    }

    @Test
    @DisplayName("Should handle shutdown operation")
    void testShutdownOperation() {
        assertDoesNotThrow(() -> {
            cacheDataSource.shutdown();
        });
        
        // After shutdown, connection status should indicate shutdown
        ConnectionStatus status = cacheDataSource.getConnectionStatus();
        assertNotNull(status);
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle null parameters gracefully")
    void testNullParameterHandling() {
        assertDoesNotThrow(() -> {
            cacheDataSource.put(null, "value");
            cacheDataSource.put("key", null);
            cacheDataSource.put(null, null);
            
            Object result1 = cacheDataSource.get(null);
            assertNull(result1);
            
            boolean result2 = cacheDataSource.remove(null);
            assertFalse(result2);
            
            boolean result3 = cacheDataSource.containsKey(null);
            assertFalse(result3);
        });
    }

    @Test
    @DisplayName("Should handle empty string keys")
    void testEmptyStringKeys() {
        assertDoesNotThrow(() -> {
            cacheDataSource.put("", "value");
            Object result = cacheDataSource.get("");
            assertNull(result); // Null because cache manager is null
        });
    }

    // ========================================
    // Cache Operations with Mock Tests
    // ========================================

    @Test
    @DisplayName("Should perform cache operations with real in-memory cache")
    void testCacheOperationsWithRealManager() throws Exception {
        CacheDataSource ds = new CacheDataSource(validConfig);
        ds.initialize(validConfig);

        // Put operations
        ds.put("test-key", "test-value");
        ds.put("ttl-key", "ttl-value", 1);

        // Get operation
        Object result = ds.get("test-key");
        assertEquals("test-value", result);

        // contains/remove/size
        assertTrue(ds.containsKey("test-key"));
        boolean removed = ds.remove("test-key");
        assertTrue(removed);
        assertFalse(ds.containsKey("test-key"));

        // Clear and size
        ds.put("a", 1);
        ds.put("b", 2);
        assertTrue(ds.size() >= 2);
        ds.clear();
        assertEquals(0, ds.size());
        ds.shutdown();
    }

    @Test
    @DisplayName("Should handle getData with real in-memory cache")
    void testGetDataWithRealManager() throws Exception {
        CacheDataSource ds = new CacheDataSource(validConfig);
        ds.initialize(validConfig);

        // Build key per CacheDataSource's buildCacheKey logic: dataType + ":" + params
        String expectedKey = "memory:test-key";
        Map<String, Object> params = new HashMap<>();
        params.put("key", expectedKey);
        params.put("value", "cached-value");
        ds.query("put", params);

        String result = ds.getData("memory", "test-key");
        assertEquals("cached-value", result);
        ds.shutdown();
    }

    @Test
    @DisplayName("Should handle query operations with real in-memory cache")
    void testQueryWithRealManager() throws Exception {
        CacheDataSource ds = new CacheDataSource(validConfig);
        ds.initialize(validConfig);

        // Seed values
        ds.put("user:1", "value1");
        ds.put("user:2", "value2");
        ds.put("user:3", "value3");

        List<Object> results = ds.query("user:*", new HashMap<>());
        assertNotNull(results);
        assertEquals(3, results.size());
        assertTrue(results.contains("value1"));
        assertTrue(results.contains("value2"));
        assertTrue(results.contains("value3"));
        ds.shutdown();
    }

    @Test
    @DisplayName("Should pass connection test after initialize")
    void testConnectionTestWithRealManager() throws Exception {
        CacheDataSource ds = new CacheDataSource(validConfig);
        ds.initialize(validConfig);
        assertTrue(ds.testConnection());
        ds.shutdown();
    }



    @Test
    @DisplayName("Should refresh without errors")
    void testRefreshWithRealManager() throws Exception {
        CacheDataSource ds = new CacheDataSource(validConfig);
        ds.initialize(validConfig);
        assertDoesNotThrow(ds::refresh);
        ds.shutdown();
    }

    @Test
    @DisplayName("Should handle shutdown cleanly")
    void testShutdownWithRealManager() throws Exception {
        CacheDataSource ds = new CacheDataSource(validConfig);
        ds.initialize(validConfig);
        assertDoesNotThrow(ds::shutdown);
        assertNotNull(ds.getConnectionStatus());
    }

    // ========================================
    // Statistics and Monitoring Tests
    // ========================================

    @Test
    @DisplayName("Should track cache statistics (real manager)")
    void testCacheStatistics() throws Exception {
        CacheDataSource ds = new CacheDataSource(validConfig);
        ds.initialize(validConfig);

        // Miss
        Object miss = ds.get("missing");
        assertNull(miss);

        // Put + Hit
        ds.put("k", "v");
        Object hit = ds.get("k");
        assertEquals("v", hit);

        CacheStatistics stats = ds.getStatistics();
        assertNotNull(stats);
        assertTrue(stats.getHits() >= 1);
        assertTrue(stats.getMisses() >= 1);
        ds.shutdown();
    }

    @Test
    @DisplayName("Should handle null cache statistics")
    void testNullCacheStatistics() {
        // Without initialization, should return new empty statistics
        CacheStatistics stats = cacheDataSource.getStatistics();
        assertNotNull(stats);
        assertEquals(0L, stats.getHits());
        assertEquals(0L, stats.getMisses());
    }

    // ========================================
    // Helper Methods
    // ========================================

    private DataSourceConfiguration createValidCacheConfiguration() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-cache");
        config.setType("cache");
        config.setSourceType("memory");
        config.setEnabled(true);

        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setMaxSize(1000);
        cacheConfig.setTtlSeconds(300L);
        config.setCache(cacheConfig);

        return config;
    }
}
