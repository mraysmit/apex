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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @Mock
    private CacheManager mockCacheManager;
    
    @Mock
    private CacheStatistics mockCacheStatistics;

    private CacheDataSource cacheDataSource;
    private DataSourceConfiguration validConfig;
    private AutoCloseable mockitoCloseable;

    @BeforeEach
    void setUp() {
        mockitoCloseable = MockitoAnnotations.openMocks(this);
        
        // Create valid configuration
        validConfig = createValidCacheConfiguration();
        
        // Create cache data source
        cacheDataSource = new CacheDataSource(validConfig);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockitoCloseable != null) {
            mockitoCloseable.close();
        }
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
    void testInitializeWithValidConfiguration() throws DataSourceException {
        // Mock the cache manager creation and behavior
        when(mockCacheManager.isHealthy()).thenReturn(true);
        when(mockCacheManager.get(anyString())).thenReturn("test");
        doNothing().when(mockCacheManager).put(anyString(), any());
        when(mockCacheManager.remove(anyString())).thenReturn(true);
        
        // Use reflection or create a testable version
        // For now, we'll test the initialization logic indirectly
        assertDoesNotThrow(() -> {
            CacheDataSource ds = new CacheDataSource(validConfig);
            // The actual initialization happens in the factory, but we can test the setup
            assertNotNull(ds.getConfiguration());
            assertEquals(validConfig, ds.getConfiguration());
        });
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
    @DisplayName("Should perform cache operations with mocked cache manager")
    void testCacheOperationsWithMock() throws Exception {
        // Create a cache data source with mocked cache manager
        CacheDataSource dsWithMock = createCacheDataSourceWithMock();

        // Test put operations
        dsWithMock.put("test-key", "test-value");
        verify(mockCacheManager).put("test-key", "test-value");

        dsWithMock.put("ttl-key", "ttl-value", 300);
        verify(mockCacheManager).put("ttl-key", "ttl-value", 300);

        // Test get operation
        when(mockCacheManager.get("test-key")).thenReturn("test-value");
        Object result = dsWithMock.get("test-key");
        assertEquals("test-value", result);
        verify(mockCacheManager).get("test-key");

        // Test remove operation
        when(mockCacheManager.remove("test-key")).thenReturn(true);
        boolean removed = dsWithMock.remove("test-key");
        assertTrue(removed);
        verify(mockCacheManager).remove("test-key");

        // Test containsKey operation
        when(mockCacheManager.containsKey("test-key")).thenReturn(true);
        boolean contains = dsWithMock.containsKey("test-key");
        assertTrue(contains);
        verify(mockCacheManager).containsKey("test-key");

        // Test clear operation
        dsWithMock.clear();
        verify(mockCacheManager).clear();

        // Test size operation
        when(mockCacheManager.size()).thenReturn(5);
        long size = dsWithMock.size();
        assertEquals(5, size);
        verify(mockCacheManager).size();
    }

    @Test
    @DisplayName("Should handle getData with mocked cache manager")
    void testGetDataWithMock() throws Exception {
        CacheDataSource dsWithMock = createCacheDataSourceWithMock();

        // Mock cache manager behavior
        when(mockCacheManager.get(anyString())).thenReturn("cached-value");

        // Test getData
        String result = dsWithMock.getData("memory", "test-key");
        assertEquals("cached-value", result);

        // Verify cache manager was called with built key
        verify(mockCacheManager).get(anyString());
    }

    @Test
    @DisplayName("Should handle query operations with mocked cache manager")
    void testQueryWithMock() throws Exception {
        CacheDataSource dsWithMock = createCacheDataSourceWithMock();

        // Mock cache manager behavior
        List<String> mockKeys = Arrays.asList("key1", "key2", "key3");
        when(mockCacheManager.getKeysByPattern("test-pattern")).thenReturn(mockKeys);
        when(mockCacheManager.get("key1")).thenReturn("value1");
        when(mockCacheManager.get("key2")).thenReturn("value2");
        when(mockCacheManager.get("key3")).thenReturn("value3");

        // Test query
        List<Object> results = dsWithMock.query("test-pattern", new HashMap<>());

        assertNotNull(results);
        assertEquals(3, results.size());
        assertTrue(results.contains("value1"));
        assertTrue(results.contains("value2"));
        assertTrue(results.contains("value3"));

        verify(mockCacheManager).getKeysByPattern("test-pattern");
        verify(mockCacheManager).get("key1");
        verify(mockCacheManager).get("key2");
        verify(mockCacheManager).get("key3");
    }

    @Test
    @DisplayName("Should handle connection test with mocked cache manager")
    void testConnectionTestWithMock() throws Exception {
        CacheDataSource dsWithMock = createCacheDataSourceWithMock();

        // Mock successful connection test
        when(mockCacheManager.get(anyString())).thenReturn("test");
        doNothing().when(mockCacheManager).put(anyString(), any());
        when(mockCacheManager.remove(anyString())).thenReturn(true);

        boolean result = dsWithMock.testConnection();
        assertTrue(result);

        verify(mockCacheManager).put(anyString(), eq("test"));
        verify(mockCacheManager).get(anyString());
        verify(mockCacheManager).remove(anyString());
    }

    @Test
    @DisplayName("Should handle connection test failure with mocked cache manager")
    void testConnectionTestFailureWithMock() throws Exception {
        CacheDataSource dsWithMock = createCacheDataSourceWithMock();

        // Mock failed connection test
        when(mockCacheManager.get(anyString())).thenReturn("wrong-value");
        doNothing().when(mockCacheManager).put(anyString(), any());
        when(mockCacheManager.remove(anyString())).thenReturn(true);

        boolean result = dsWithMock.testConnection();
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle refresh with mocked cache manager")
    void testRefreshWithMock() throws Exception {
        CacheDataSource dsWithMock = createCacheDataSourceWithMock();

        doNothing().when(mockCacheManager).evictExpired();

        assertDoesNotThrow(() -> dsWithMock.refresh());

        verify(mockCacheManager).evictExpired();
    }

    @Test
    @DisplayName("Should handle shutdown with mocked cache manager")
    void testShutdownWithMock() throws Exception {
        CacheDataSource dsWithMock = createCacheDataSourceWithMock();

        doNothing().when(mockCacheManager).shutdown();

        dsWithMock.shutdown();

        verify(mockCacheManager).shutdown();
    }

    // ========================================
    // Statistics and Monitoring Tests
    // ========================================

    @Test
    @DisplayName("Should track cache statistics")
    void testCacheStatistics() throws Exception {
        CacheDataSource dsWithMock = createCacheDataSourceWithMock();

        // Mock cache statistics
        when(mockCacheManager.getStatistics()).thenReturn(mockCacheStatistics);
        when(mockCacheStatistics.getHits()).thenReturn(10L);
        when(mockCacheStatistics.getMisses()).thenReturn(2L);
        when(mockCacheStatistics.getHitRate()).thenReturn(83.33); // Hit rate as percentage

        CacheStatistics stats = dsWithMock.getStatistics();

        assertNotNull(stats);
        assertEquals(10L, stats.getHits());
        assertEquals(2L, stats.getMisses());
        assertEquals(83.33, stats.getHitRate(), 0.01);

        verify(mockCacheManager).getStatistics();
    }

    @Test
    @DisplayName("Should handle null cache statistics")
    void testNullCacheStatistics() {
        // Without mock cache manager, should return new empty statistics
        CacheStatistics stats = cacheDataSource.getStatistics();
        assertNotNull(stats); // Returns new CacheStatistics() when cache manager is null
        assertEquals(0L, stats.getHits());
        assertEquals(0L, stats.getMisses());
    }

    // ========================================
    // Edge Cases and Error Handling
    // ========================================

    @Test
    @DisplayName("Should handle cache manager exceptions gracefully")
    void testCacheManagerExceptions() throws Exception {
        CacheDataSource dsWithMock = createCacheDataSourceWithMock();

        // Mock cache manager to throw exceptions
        when(mockCacheManager.get(anyString())).thenThrow(new RuntimeException("Cache error"));
        doThrow(new RuntimeException("Cache error")).when(mockCacheManager).put(anyString(), any());

        // getData should handle exceptions gracefully and return null
        Object result = dsWithMock.getData("memory", "test-key");
        assertNull(result); // Should return null on error

        // put operation should propagate the exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            dsWithMock.put("test-key", "test-value");
        });

        assertEquals("Cache error", exception.getMessage());
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

    private CacheDataSource createCacheDataSourceWithMock() throws Exception {
        CacheDataSource ds = new CacheDataSource(validConfig);

        // Use reflection to inject the mock cache manager
        java.lang.reflect.Field cacheManagerField = CacheDataSource.class.getDeclaredField("cacheManager");
        cacheManagerField.setAccessible(true);
        cacheManagerField.set(ds, mockCacheManager);

        return ds;
    }
}
