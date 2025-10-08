package dev.mars.apex.core.cache;

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
import dev.mars.apex.core.service.data.external.cache.CacheStatistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApexCacheManager.
 * 
 * Tests cover:
 * - Singleton pattern
 * - All cache scopes (dataset, lookup-result, expression, service-registry)
 * - CRUD operations (put, get, remove, containsKey)
 * - Statistics tracking
 * - Clear operations (scope-specific and all)
 * - Lifecycle management (shutdown)
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class ApexCacheManagerTest {

    private ApexCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Reset singleton before each test
        ApexCacheManager.resetInstance();
        cacheManager = ApexCacheManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        if (cacheManager != null) {
            cacheManager.shutdown();
        }
        ApexCacheManager.resetInstance();
    }

    // ========================================
    // Singleton Pattern Tests
    // ========================================

    @Test
    @DisplayName("Should return same instance for multiple getInstance calls")
    void testSingletonPattern() {
        ApexCacheManager instance1 = ApexCacheManager.getInstance();
        ApexCacheManager instance2 = ApexCacheManager.getInstance();
        
        assertSame(instance1, instance2, "Should return same instance");
    }

    @Test
    @DisplayName("Should create new instance after reset")
    void testResetInstance() {
        ApexCacheManager instance1 = ApexCacheManager.getInstance();
        
        ApexCacheManager.resetInstance();
        
        ApexCacheManager instance2 = ApexCacheManager.getInstance();
        
        assertNotSame(instance1, instance2, "Should create new instance after reset");
    }

    @Test
    @DisplayName("Should accept custom configuration")
    void testCustomConfiguration() {
        ApexCacheManager.resetInstance();
        
        CacheConfig config = new CacheConfig();
        config.setEnabled(true);
        config.setStatisticsEnabled(true);
        
        ApexCacheManager instance = ApexCacheManager.getInstance(config);
        
        assertNotNull(instance);
    }

    // ========================================
    // Dataset Cache Tests
    // ========================================

    @Test
    @DisplayName("Should put and get from dataset cache")
    void testDatasetCachePutGet() {
        String key = "inline-abc123";
        Object value = "DatasetLookupService instance";
        
        cacheManager.put(ApexCacheManager.DATASET_CACHE, key, value);
        Object retrieved = cacheManager.get(ApexCacheManager.DATASET_CACHE, key);
        
        assertEquals(value, retrieved);
    }

    @Test
    @DisplayName("Should check if key exists in dataset cache")
    void testDatasetCacheContainsKey() {
        String key = "inline-xyz789";
        
        assertFalse(cacheManager.containsKey(ApexCacheManager.DATASET_CACHE, key));
        
        cacheManager.put(ApexCacheManager.DATASET_CACHE, key, "value");
        
        assertTrue(cacheManager.containsKey(ApexCacheManager.DATASET_CACHE, key));
    }

    @Test
    @DisplayName("Should remove from dataset cache")
    void testDatasetCacheRemove() {
        String key = "inline-remove";
        cacheManager.put(ApexCacheManager.DATASET_CACHE, key, "value");
        
        assertTrue(cacheManager.containsKey(ApexCacheManager.DATASET_CACHE, key));
        
        boolean removed = cacheManager.remove(ApexCacheManager.DATASET_CACHE, key);
        
        assertTrue(removed);
        assertFalse(cacheManager.containsKey(ApexCacheManager.DATASET_CACHE, key));
    }

    // ========================================
    // Lookup Result Cache Tests
    // ========================================

    @Test
    @DisplayName("Should put and get from lookup result cache")
    void testLookupResultCachePutGet() {
        String key = "currency-service:USD";
        Map<String, Object> value = Map.of("code", "USD", "name", "US Dollar");
        
        cacheManager.put(ApexCacheManager.LOOKUP_RESULT_CACHE, key, value);
        Object retrieved = cacheManager.get(ApexCacheManager.LOOKUP_RESULT_CACHE, key);
        
        assertEquals(value, retrieved);
    }

    @Test
    @DisplayName("Should support custom TTL for lookup results")
    void testLookupResultCacheCustomTTL() {
        String key = "temp-result";
        Object value = "temporary value";
        
        cacheManager.put(ApexCacheManager.LOOKUP_RESULT_CACHE, key, value, 60L); // 60 seconds
        
        assertTrue(cacheManager.containsKey(ApexCacheManager.LOOKUP_RESULT_CACHE, key));
    }

    // ========================================
    // Expression Cache Tests
    // ========================================

    @Test
    @DisplayName("Should put and get from expression cache")
    void testExpressionCachePutGet() {
        String key = "#currencyCode != null";
        Object value = "Compiled SpEL Expression";
        
        cacheManager.put(ApexCacheManager.EXPRESSION_CACHE, key, value);
        Object retrieved = cacheManager.get(ApexCacheManager.EXPRESSION_CACHE, key);
        
        assertEquals(value, retrieved);
    }

    // ========================================
    // Service Registry Cache Tests
    // ========================================

    @Test
    @DisplayName("Should put and get from service registry cache")
    void testServiceRegistryCachePutGet() {
        String key = "currency-lookup-service";
        Object value = "LookupService instance";
        
        cacheManager.put(ApexCacheManager.SERVICE_REGISTRY_CACHE, key, value);
        Object retrieved = cacheManager.get(ApexCacheManager.SERVICE_REGISTRY_CACHE, key);
        
        assertEquals(value, retrieved);
    }

    // ========================================
    // Cache Isolation Tests
    // ========================================

    @Test
    @DisplayName("Should isolate different cache scopes")
    void testCacheScopeIsolation() {
        String key = "same-key";
        String value1 = "dataset-value";
        String value2 = "lookup-value";
        String value3 = "expression-value";
        
        cacheManager.put(ApexCacheManager.DATASET_CACHE, key, value1);
        cacheManager.put(ApexCacheManager.LOOKUP_RESULT_CACHE, key, value2);
        cacheManager.put(ApexCacheManager.EXPRESSION_CACHE, key, value3);
        
        assertEquals(value1, cacheManager.get(ApexCacheManager.DATASET_CACHE, key));
        assertEquals(value2, cacheManager.get(ApexCacheManager.LOOKUP_RESULT_CACHE, key));
        assertEquals(value3, cacheManager.get(ApexCacheManager.EXPRESSION_CACHE, key));
    }

    // ========================================
    // Size Tests
    // ========================================

    @Test
    @DisplayName("Should track cache size correctly")
    void testCacheSize() {
        assertEquals(0, cacheManager.size(ApexCacheManager.DATASET_CACHE));
        
        cacheManager.put(ApexCacheManager.DATASET_CACHE, "key1", "value1");
        cacheManager.put(ApexCacheManager.DATASET_CACHE, "key2", "value2");
        
        assertEquals(2, cacheManager.size(ApexCacheManager.DATASET_CACHE));
    }

    // ========================================
    // Clear Tests
    // ========================================

    @Test
    @DisplayName("Should clear specific cache scope")
    void testClearScope() {
        cacheManager.put(ApexCacheManager.DATASET_CACHE, "key1", "value1");
        cacheManager.put(ApexCacheManager.LOOKUP_RESULT_CACHE, "key2", "value2");
        
        assertEquals(1, cacheManager.size(ApexCacheManager.DATASET_CACHE));
        assertEquals(1, cacheManager.size(ApexCacheManager.LOOKUP_RESULT_CACHE));
        
        cacheManager.clearScope(ApexCacheManager.DATASET_CACHE);
        
        assertEquals(0, cacheManager.size(ApexCacheManager.DATASET_CACHE));
        assertEquals(1, cacheManager.size(ApexCacheManager.LOOKUP_RESULT_CACHE));
    }

    @Test
    @DisplayName("Should clear all cache scopes")
    void testClearAll() {
        cacheManager.put(ApexCacheManager.DATASET_CACHE, "key1", "value1");
        cacheManager.put(ApexCacheManager.LOOKUP_RESULT_CACHE, "key2", "value2");
        cacheManager.put(ApexCacheManager.EXPRESSION_CACHE, "key3", "value3");
        
        cacheManager.clearAll();
        
        assertEquals(0, cacheManager.size(ApexCacheManager.DATASET_CACHE));
        assertEquals(0, cacheManager.size(ApexCacheManager.LOOKUP_RESULT_CACHE));
        assertEquals(0, cacheManager.size(ApexCacheManager.EXPRESSION_CACHE));
    }

    // ========================================
    // Statistics Tests
    // ========================================

    @Test
    @DisplayName("Should track cache statistics")
    void testCacheStatistics() {
        String key = "test-key";
        
        // Miss
        cacheManager.get(ApexCacheManager.DATASET_CACHE, key);
        
        // Put and hit
        cacheManager.put(ApexCacheManager.DATASET_CACHE, key, "value");
        cacheManager.get(ApexCacheManager.DATASET_CACHE, key);
        
        CacheStatistics stats = cacheManager.getStatistics(ApexCacheManager.DATASET_CACHE);
        
        assertNotNull(stats);
        assertEquals(1, stats.getHits());
        assertEquals(1, stats.getMisses());
    }

    @Test
    @DisplayName("Should get statistics for all scopes")
    void testGetAllStatistics() {
        cacheManager.put(ApexCacheManager.DATASET_CACHE, "key1", "value1");
        cacheManager.put(ApexCacheManager.LOOKUP_RESULT_CACHE, "key2", "value2");
        
        Map<String, CacheStatistics> allStats = cacheManager.getAllStatistics();
        
        assertNotNull(allStats);
        assertEquals(4, allStats.size()); // 4 cache scopes
        assertTrue(allStats.containsKey(ApexCacheManager.DATASET_CACHE));
        assertTrue(allStats.containsKey(ApexCacheManager.LOOKUP_RESULT_CACHE));
        assertTrue(allStats.containsKey(ApexCacheManager.EXPRESSION_CACHE));
        assertTrue(allStats.containsKey(ApexCacheManager.SERVICE_REGISTRY_CACHE));
    }

    @Test
    @DisplayName("Should generate statistics summary")
    void testStatisticsSummary() {
        cacheManager.put(ApexCacheManager.DATASET_CACHE, "key1", "value1");
        cacheManager.get(ApexCacheManager.DATASET_CACHE, "key1"); // Hit
        
        String summary = cacheManager.getStatisticsSummary();
        
        assertNotNull(summary);
        assertTrue(summary.contains("APEX Cache Statistics"));
        assertTrue(summary.contains(ApexCacheManager.DATASET_CACHE));
        assertTrue(summary.contains("hits="));
        assertTrue(summary.contains("misses="));
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle unknown cache scope gracefully")
    void testUnknownCacheScope() {
        String unknownScope = "unknown-scope";
        
        // Should not throw exceptions
        cacheManager.put(unknownScope, "key", "value");
        Object result = cacheManager.get(unknownScope, "key");
        boolean removed = cacheManager.remove(unknownScope, "key");
        boolean contains = cacheManager.containsKey(unknownScope, "key");
        int size = cacheManager.size(unknownScope);
        
        assertNull(result);
        assertFalse(removed);
        assertFalse(contains);
        assertEquals(0, size);
    }

    // ========================================
    // Lifecycle Tests
    // ========================================

    @Test
    @DisplayName("Should shutdown cleanly")
    void testShutdown() {
        cacheManager.put(ApexCacheManager.DATASET_CACHE, "key", "value");
        
        // Should not throw
        cacheManager.shutdown();
    }
}

