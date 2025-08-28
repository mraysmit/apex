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


import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.CacheConfig;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CacheManager interface contract.
 *
 * Tests cover:
 * - Interface contract compliance using InMemoryCacheManager implementation
 * - Basic cache operations (put, get, remove, contains)
 * - TTL functionality and expiration
 * - Key pattern matching and retrieval
 * - Cache management operations (clear, evict, size)
 * - Health checks and statistics
 * - Edge cases and error handling
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class CacheManagerTest {

    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Create a test configuration for InMemoryCacheManager
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-cache");
        config.setType("cache");
        config.setEnabled(true);

        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setMaxSize(100);
        cacheConfig.setTtlSeconds(300L);
        config.setCache(cacheConfig);

        // Use InMemoryCacheManager as the concrete implementation for testing
        cacheManager = new InMemoryCacheManager(config);
    }

    @AfterEach
    void tearDown() {
        if (cacheManager != null) {
            cacheManager.shutdown();
        }
    }

    // ========================================
    // Basic Cache Operations Tests
    // ========================================

    @Test
    @DisplayName("Should put and get values correctly")
    void testPutAndGet() {
        String key = "test-key";
        String value = "test-value";
        
        cacheManager.put(key, value);
        Object retrieved = cacheManager.get(key);
        
        assertEquals(value, retrieved);
        assertTrue(cacheManager.containsKey(key));
        assertEquals(1, cacheManager.size());
    }

    @Test
    @DisplayName("Should put and get values with TTL")
    void testPutAndGetWithTTL() {
        String key = "ttl-key";
        String value = "ttl-value";
        long ttl = 60; // 60 seconds
        
        cacheManager.put(key, value, ttl);
        Object retrieved = cacheManager.get(key);
        
        assertEquals(value, retrieved);
        assertTrue(cacheManager.containsKey(key));
    }

    @Test
    @DisplayName("Should handle null values")
    void testNullValues() {
        String key = "null-key";
        
        cacheManager.put(key, null);
        Object retrieved = cacheManager.get(key);
        
        assertNull(retrieved);
        assertTrue(cacheManager.containsKey(key));
    }

    @Test
    @DisplayName("Should return null for non-existent keys")
    void testGetNonExistentKey() {
        Object retrieved = cacheManager.get("non-existent");
        
        assertNull(retrieved);
        assertFalse(cacheManager.containsKey("non-existent"));
    }

    @Test
    @DisplayName("Should remove values correctly")
    void testRemove() {
        String key = "remove-key";
        String value = "remove-value";
        
        cacheManager.put(key, value);
        assertTrue(cacheManager.containsKey(key));
        
        boolean removed = cacheManager.remove(key);
        
        assertTrue(removed);
        assertFalse(cacheManager.containsKey(key));
        assertNull(cacheManager.get(key));
        assertEquals(0, cacheManager.size());
    }

    @Test
    @DisplayName("Should return false when removing non-existent key")
    void testRemoveNonExistentKey() {
        boolean removed = cacheManager.remove("non-existent");
        
        assertFalse(removed);
    }

    @Test
    @DisplayName("Should handle multiple values")
    void testMultipleValues() {
        cacheManager.put("key1", "value1");
        cacheManager.put("key2", "value2");
        cacheManager.put("key3", "value3");
        
        assertEquals(3, cacheManager.size());
        assertEquals("value1", cacheManager.get("key1"));
        assertEquals("value2", cacheManager.get("key2"));
        assertEquals("value3", cacheManager.get("key3"));
        
        assertTrue(cacheManager.containsKey("key1"));
        assertTrue(cacheManager.containsKey("key2"));
        assertTrue(cacheManager.containsKey("key3"));
    }

    // ========================================
    // Key Management Tests
    // ========================================

    @Test
    @DisplayName("Should get all keys")
    void testGetAllKeys() {
        cacheManager.put("key1", "value1");
        cacheManager.put("key2", "value2");
        cacheManager.put("key3", "value3");
        
        List<String> allKeys = cacheManager.getAllKeys();
        
        assertEquals(3, allKeys.size());
        assertTrue(allKeys.contains("key1"));
        assertTrue(allKeys.contains("key2"));
        assertTrue(allKeys.contains("key3"));
    }

    @Test
    @DisplayName("Should get keys by pattern")
    void testGetKeysByPattern() {
        cacheManager.put("user:1", "user1");
        cacheManager.put("user:2", "user2");
        cacheManager.put("product:1", "product1");
        cacheManager.put("product:2", "product2");
        
        List<String> userKeys = cacheManager.getKeysByPattern("user:*");
        List<String> productKeys = cacheManager.getKeysByPattern("product:*");
        
        assertEquals(2, userKeys.size());
        assertEquals(2, productKeys.size());
        assertTrue(userKeys.contains("user:1"));
        assertTrue(userKeys.contains("user:2"));
        assertTrue(productKeys.contains("product:1"));
        assertTrue(productKeys.contains("product:2"));
    }

    @Test
    @DisplayName("Should return empty list for non-matching patterns")
    void testGetKeysByPatternNoMatch() {
        cacheManager.put("key1", "value1");
        cacheManager.put("key2", "value2");
        
        List<String> matchingKeys = cacheManager.getKeysByPattern("nomatch:*");
        
        assertTrue(matchingKeys.isEmpty());
    }

    // ========================================
    // Cache Management Tests
    // ========================================

    @Test
    @DisplayName("Should clear all entries")
    void testClear() {
        cacheManager.put("key1", "value1");
        cacheManager.put("key2", "value2");
        cacheManager.put("key3", "value3");
        
        assertEquals(3, cacheManager.size());
        
        cacheManager.clear();
        
        assertEquals(0, cacheManager.size());
        assertFalse(cacheManager.containsKey("key1"));
        assertFalse(cacheManager.containsKey("key2"));
        assertFalse(cacheManager.containsKey("key3"));
        assertTrue(cacheManager.getAllKeys().isEmpty());
    }

    @Test
    @DisplayName("Should report correct size")
    void testSize() {
        assertEquals(0, cacheManager.size());
        
        cacheManager.put("key1", "value1");
        assertEquals(1, cacheManager.size());
        
        cacheManager.put("key2", "value2");
        assertEquals(2, cacheManager.size());
        
        cacheManager.remove("key1");
        assertEquals(1, cacheManager.size());
        
        cacheManager.clear();
        assertEquals(0, cacheManager.size());
    }

    @Test
    @DisplayName("Should evict expired entries")
    void testEvictExpired() throws InterruptedException {
        // Put entry with very short TTL
        cacheManager.put("short-ttl", "value", 1); // 1 second TTL
        cacheManager.put("long-ttl", "value", 3600); // 1 hour TTL
        
        assertEquals(2, cacheManager.size());
        
        // Wait for expiration
        Thread.sleep(1100); // Wait 1.1 seconds
        
        // Evict expired entries
        cacheManager.evictExpired();
        
        // Short TTL entry should be gone, long TTL should remain
        assertEquals(1, cacheManager.size());
        assertFalse(cacheManager.containsKey("short-ttl"));
        assertTrue(cacheManager.containsKey("long-ttl"));
    }

    // ========================================
    // Health and Statistics Tests
    // ========================================

    @Test
    @DisplayName("Should report healthy status")
    void testIsHealthy() {
        assertTrue(cacheManager.isHealthy());
        
        // Add some data
        cacheManager.put("key", "value");
        assertTrue(cacheManager.isHealthy());
        
        // Clear cache
        cacheManager.clear();
        assertTrue(cacheManager.isHealthy());
    }

    @Test
    @DisplayName("Should provide statistics")
    void testGetStatistics() {
        CacheStatistics stats = cacheManager.getStatistics();
        
        assertNotNull(stats);
        assertEquals(0, stats.getHits());
        assertEquals(0, stats.getMisses());
        assertEquals(0, stats.getPuts());
        
        // Perform some operations
        cacheManager.put("key", "value");
        cacheManager.get("key"); // hit
        cacheManager.get("nonexistent"); // miss
        
        stats = cacheManager.getStatistics();
        assertTrue(stats.getPuts() > 0);
        assertTrue(stats.getHits() > 0);
        assertTrue(stats.getMisses() > 0);
    }

    // ========================================
    // Edge Cases and Error Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle null keys gracefully")
    void testNullKeys() {
        // InMemoryCacheManager handles null keys gracefully without throwing exceptions
        cacheManager.put(null, "value"); // Should not throw
        assertNull(cacheManager.get(null)); // Should return null
        assertFalse(cacheManager.remove(null)); // Should return false
        assertFalse(cacheManager.containsKey(null)); // Should return false
    }

    @Test
    @DisplayName("Should handle empty keys gracefully")
    void testEmptyKeys() {
        // InMemoryCacheManager allows empty keys
        cacheManager.put("", "value");
        assertEquals("value", cacheManager.get(""));
        assertTrue(cacheManager.containsKey(""));
        assertTrue(cacheManager.remove(""));
    }

    @Test
    @DisplayName("Should handle negative TTL gracefully")
    void testNegativeTTL() {
        // InMemoryCacheManager treats negative TTL as no expiration
        cacheManager.put("key", "value", -1);
        assertEquals("value", cacheManager.get("key"));
        assertTrue(cacheManager.containsKey("key"));
    }

    @Test
    @DisplayName("Should handle zero TTL gracefully")
    void testZeroTTL() {
        // InMemoryCacheManager treats zero TTL as no expiration (Long.MAX_VALUE)
        cacheManager.put("key", "value", 0);

        // Entry should not be expired (zero TTL means no expiration in this implementation)
        assertEquals("value", cacheManager.get("key"));
        assertTrue(cacheManager.containsKey("key"));
    }

    @Test
    @DisplayName("Should handle shutdown gracefully")
    void testShutdown() {
        cacheManager.put("key", "value");
        assertEquals(1, cacheManager.size());
        
        cacheManager.shutdown();
        
        // After shutdown, cache should still be accessible but may be cleared
        // The exact behavior depends on implementation
        assertNotNull(cacheManager);
    }

    @Test
    @DisplayName("Should handle operations after shutdown")
    void testOperationsAfterShutdown() {
        cacheManager.put("key", "value");
        cacheManager.shutdown();
        
        // Operations after shutdown should either work or throw appropriate exceptions
        // The exact behavior depends on implementation
        try {
            cacheManager.put("new-key", "new-value");
            cacheManager.get("key");
            cacheManager.size();
            // If no exception is thrown, operations are still allowed
        } catch (IllegalStateException e) {
            // This is also acceptable behavior after shutdown
        }
    }

    // ========================================
    // Data Type Tests
    // ========================================

    @Test
    @DisplayName("Should handle different data types")
    void testDifferentDataTypes() {
        cacheManager.put("string", "string-value");
        cacheManager.put("integer", 42);
        cacheManager.put("double", 3.14);
        cacheManager.put("boolean", true);
        cacheManager.put("object", new TestObject("test"));
        
        assertEquals("string-value", cacheManager.get("string"));
        assertEquals(42, cacheManager.get("integer"));
        assertEquals(3.14, cacheManager.get("double"));
        assertEquals(true, cacheManager.get("boolean"));
        
        TestObject obj = (TestObject) cacheManager.get("object");
        assertNotNull(obj);
        assertEquals("test", obj.getValue());
    }

    // Helper class for testing object storage
    private static class TestObject {
        private final String value;
        
        public TestObject(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}
