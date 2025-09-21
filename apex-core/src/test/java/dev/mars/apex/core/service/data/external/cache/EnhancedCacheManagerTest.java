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

import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for EnhancedCacheManager covering all advanced cache features.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
class EnhancedCacheManagerTest {
    
    private DataSourceConfiguration configuration;
    private CacheConfig cacheConfig;
    private EnhancedCacheManager cacheManager;
    
    @BeforeEach
    void setUp() {
        configuration = new DataSourceConfiguration();
        configuration.setName("test-cache");
        
        cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(60L);
        cacheConfig.setMaxIdleSeconds(30L);
        cacheConfig.setMaxSize(5);
        cacheConfig.setKeyPrefix("test");
        
        configuration.setCache(cacheConfig);
        cacheManager = new EnhancedCacheManager(configuration);
    }
    
    @Test
    @DisplayName("Should enable/disable caching based on configuration")
    void testCacheEnabled() {
        assertTrue(cacheManager.isEnabled());
        
        // Test with disabled cache
        cacheConfig.setEnabled(false);
        EnhancedCacheManager disabledCache = new EnhancedCacheManager(configuration);
        assertFalse(disabledCache.isEnabled());
    }
    
    @Test
    @DisplayName("Should put and get cache entries with key prefix")
    void testBasicCacheOperations() {
        // Put data
        cacheManager.put("key1", "value1");
        cacheManager.put("key2", "value2");
        
        // Get data
        assertEquals("value1", cacheManager.get("key1"));
        assertEquals("value2", cacheManager.get("key2"));
        assertNull(cacheManager.get("nonexistent"));
        
        // Check size
        assertEquals(2, cacheManager.size());
        assertFalse(cacheManager.isEmpty());
    }
    
    @Test
    @DisplayName("Should generate cache keys with parameters")
    void testCacheKeyGeneration() {
        String key1 = cacheManager.generateCacheKey("user", "123", "profile");
        String key2 = cacheManager.generateCacheKey("user", "456", "profile");
        String key3 = cacheManager.generateCacheKey("user", "123", null);
        
        assertEquals("user:123:profile", key1);
        assertEquals("user:456:profile", key2);
        assertEquals("user:123:null", key3);
        
        assertNotEquals(key1, key2);
        assertNotEquals(key1, key3);
    }
    
    @Test
    @DisplayName("Should enforce max size with LRU eviction")
    void testLruEviction() throws InterruptedException {
        // Fill cache to max size
        cacheManager.put("key1", "value1");
        Thread.sleep(10); // Ensure different access times
        cacheManager.put("key2", "value2");
        Thread.sleep(10);
        cacheManager.put("key3", "value3");
        Thread.sleep(10);
        cacheManager.put("key4", "value4");
        Thread.sleep(10);
        cacheManager.put("key5", "value5");
        
        assertEquals(5, cacheManager.size());
        
        // Access key1 to make it more recently used
        cacheManager.get("key1");
        Thread.sleep(10);
        
        // Add one more entry - should evict key2 (least recently used)
        cacheManager.put("key6", "value6");
        
        assertEquals(5, cacheManager.size());
        assertNotNull(cacheManager.get("key1")); // Should still be there (recently accessed)
        assertNull(cacheManager.get("key2"));    // Should be evicted (least recently used)
        assertNotNull(cacheManager.get("key6")); // Should be there (newly added)
    }
    
    @Test
    @DisplayName("Should expire entries based on TTL")
    void testTtlExpiration() throws InterruptedException {
        // Use short TTL for testing
        cacheConfig.setTtlSeconds(1L);
        EnhancedCacheManager shortTtlCache = new EnhancedCacheManager(configuration);
        
        shortTtlCache.put("key1", "value1");
        assertEquals("value1", shortTtlCache.get("key1"));
        
        // Wait for TTL expiration
        Thread.sleep(1100);
        
        assertNull(shortTtlCache.get("key1"));
        assertEquals(0, shortTtlCache.size());
    }
    
    @Test
    @DisplayName("Should expire entries based on max idle time")
    void testMaxIdleExpiration() throws InterruptedException {
        // Use short max idle time for testing
        cacheConfig.setMaxIdleSeconds(1L);
        EnhancedCacheManager shortIdleCache = new EnhancedCacheManager(configuration);
        
        shortIdleCache.put("key1", "value1");
        assertEquals("value1", shortIdleCache.get("key1"));
        
        // Wait for idle expiration
        Thread.sleep(1100);
        
        assertNull(shortIdleCache.get("key1"));
        assertEquals(0, shortIdleCache.size());
    }
    
    @Test
    @DisplayName("Should track cache statistics")
    void testCacheStatistics() {
        CacheStatistics stats = cacheManager.getStatistics();
        
        // Initial state
        assertEquals(0, stats.getHits());
        assertEquals(0, stats.getMisses());
        assertEquals(0, stats.getPuts());

        // Put operations
        cacheManager.put("key1", "value1");
        cacheManager.put("key2", "value2");
        assertEquals(2, stats.getPuts());

        // Hit operations
        cacheManager.get("key1");
        cacheManager.get("key1");
        assertEquals(2, stats.getHits());

        // Miss operations
        cacheManager.get("nonexistent");
        cacheManager.get("missing");
        assertEquals(2, stats.getMisses());

        // Hit ratio (as percentage)
        assertEquals(50.0, stats.getHitRate(), 0.01);
    }
    
    @Test
    @DisplayName("Should remove entries")
    void testRemoveOperations() {
        cacheManager.put("key1", "value1");
        cacheManager.put("key2", "value2");
        assertEquals(2, cacheManager.size());
        
        // Remove existing entry
        Object removed = cacheManager.remove("key1");
        assertEquals("value1", removed);
        assertEquals(1, cacheManager.size());
        assertNull(cacheManager.get("key1"));
        
        // Remove non-existent entry
        Object notRemoved = cacheManager.remove("nonexistent");
        assertNull(notRemoved);
        assertEquals(1, cacheManager.size());
    }
    
    @Test
    @DisplayName("Should clear all entries")
    void testClearOperations() {
        cacheManager.put("key1", "value1");
        cacheManager.put("key2", "value2");
        cacheManager.put("key3", "value3");
        assertEquals(3, cacheManager.size());
        
        cacheManager.clear();
        assertEquals(0, cacheManager.size());
        assertTrue(cacheManager.isEmpty());
        assertNull(cacheManager.get("key1"));
        assertNull(cacheManager.get("key2"));
        assertNull(cacheManager.get("key3"));
    }
    
    @Test
    @DisplayName("Should clean up expired entries")
    void testExpiredEntryCleanup() throws InterruptedException {
        // Use short TTL for testing
        cacheConfig.setTtlSeconds(1L);
        EnhancedCacheManager shortTtlCache = new EnhancedCacheManager(configuration);
        
        shortTtlCache.put("key1", "value1");
        shortTtlCache.put("key2", "value2");
        shortTtlCache.put("key3", "value3");
        assertEquals(3, shortTtlCache.size());
        
        // Wait for expiration
        Thread.sleep(1100);
        
        // Cleanup expired entries
        int removedCount = shortTtlCache.cleanupExpiredEntries();
        assertEquals(3, removedCount);
        assertEquals(0, shortTtlCache.size());
    }
    
    @Test
    @DisplayName("Should handle disabled cache gracefully")
    void testDisabledCache() {
        cacheConfig.setEnabled(false);
        EnhancedCacheManager disabledCache = new EnhancedCacheManager(configuration);
        
        // Operations should be no-ops
        disabledCache.put("key1", "value1");
        assertNull(disabledCache.get("key1"));
        assertEquals(0, disabledCache.size());
        assertTrue(disabledCache.isEmpty());
        
        // Remove and clear should also be no-ops
        assertNull(disabledCache.remove("key1"));
        disabledCache.clear(); // Should not throw exception
    }
    
    @Test
    @DisplayName("Should handle null values gracefully")
    void testNullValueHandling() {
        // Null key should be ignored
        cacheManager.put(null, "value1");
        assertEquals(0, cacheManager.size());
        
        // Null data should be ignored
        cacheManager.put("key1", null);
        assertEquals(0, cacheManager.size());
        
        // Get with null key should return null
        assertNull(cacheManager.get(null));
        
        // Remove with null key should return null
        assertNull(cacheManager.remove(null));
    }
}
