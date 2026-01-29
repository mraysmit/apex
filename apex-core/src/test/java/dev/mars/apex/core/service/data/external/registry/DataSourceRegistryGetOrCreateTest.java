package dev.mars.apex.core.service.data.external.registry;

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
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the enhanced DataSourceRegistry with getOrCreate() functionality.
 * 
 * Phase 1 validation: Ensures the unified registry works correctly as
 * the single source of truth for data source management.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-01-29
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class DataSourceRegistryGetOrCreateTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceRegistryGetOrCreateTest.class);
    
    private DataSourceRegistry registry;
    
    @BeforeEach
    void setUp() {
        registry = DataSourceRegistry.getInstance();
        registry.clearForTesting();
        LOGGER.info("Registry cleared for test isolation");
    }
    
    @AfterEach
    void tearDown() {
        registry.clearForTesting();
    }
    
    @Test
    @DisplayName("Should create data source on first call to getOrCreate")
    void shouldCreateDataSourceOnFirstCall() throws DataSourceException {
        LOGGER.info("================================================================================");
        LOGGER.info("TEST: Create data source on first getOrCreate call");
        LOGGER.info("================================================================================");
        
        // Given: A cache data source configuration
        DataSourceConfiguration config = createCacheConfig("test-cache-1");
        
        // When: getOrCreate is called
        assertFalse(registry.contains("test-cache-1"), "Should not exist before creation");
        ExternalDataSource dataSource = registry.getOrCreate("test-cache-1", config);
        
        // Then: Data source is created and registered
        assertNotNull(dataSource, "Data source should be created");
        assertTrue(registry.contains("test-cache-1"), "Should be registered after creation");
        assertEquals("test-cache-1", dataSource.getName(), "Name should match");
        
        LOGGER.info("TEST PASSED: Data source created and registered on first call");
    }
    
    @Test
    @DisplayName("Should return same instance on subsequent calls to getOrCreate")
    void shouldReturnSameInstanceOnSubsequentCalls() throws DataSourceException {
        LOGGER.info("================================================================================");
        LOGGER.info("TEST: Return same instance on subsequent getOrCreate calls");
        LOGGER.info("================================================================================");
        
        // Given: A data source configuration
        DataSourceConfiguration config = createCacheConfig("test-cache-2");
        
        // When: getOrCreate is called multiple times
        ExternalDataSource first = registry.getOrCreate("test-cache-2", config);
        ExternalDataSource second = registry.getOrCreate("test-cache-2", config);
        ExternalDataSource third = registry.getOrCreate("test-cache-2", config);
        
        // Then: All calls return the same instance
        assertSame(first, second, "Second call should return same instance");
        assertSame(second, third, "Third call should return same instance");
        assertEquals(1, registry.size(), "Only one data source should be registered");
        
        LOGGER.info("TEST PASSED: Same instance returned on subsequent calls");
    }
    
    @Test
    @DisplayName("Should handle concurrent getOrCreate calls safely")
    void shouldHandleConcurrentGetOrCreateCalls() throws Exception {
        LOGGER.info("================================================================================");
        LOGGER.info("TEST: Concurrent getOrCreate calls");
        LOGGER.info("================================================================================");
        
        // Given: Multiple threads trying to create the same data source
        DataSourceConfiguration config = createCacheConfig("concurrent-cache");
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        Set<ExternalDataSource> dataSources = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // When: All threads call getOrCreate simultaneously
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    LOGGER.info("Thread {} calling getOrCreate", threadNum);
                    ExternalDataSource ds = registry.getOrCreate("concurrent-cache", config);
                    dataSources.add(ds);
                    LOGGER.info("Thread {} got data source: {}", threadNum, System.identityHashCode(ds));
                } catch (Exception e) {
                    LOGGER.error("Thread {} failed: {}", threadNum, e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        startLatch.countDown(); // Start all threads
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        // Then: All threads got the same instance
        assertEquals(1, dataSources.size(), "All threads should get the same instance");
        assertEquals(1, registry.size(), "Only one data source should be registered");
        
        LOGGER.info("Concurrent creation results:");
        LOGGER.info("  Thread count: {}", threadCount);
        LOGGER.info("  Unique instances: {}", dataSources.size());
        LOGGER.info("  Registry size: {}", registry.size());
        LOGGER.info("TEST PASSED: Concurrent getOrCreate properly deduplicated");
    }
    
    @Test
    @DisplayName("Should create different data sources for different names")
    void shouldCreateDifferentDataSourcesForDifferentNames() throws DataSourceException {
        LOGGER.info("================================================================================");
        LOGGER.info("TEST: Different names create different data sources");
        LOGGER.info("================================================================================");
        
        // Given: Different configurations with different names
        DataSourceConfiguration config1 = createCacheConfig("cache-a");
        DataSourceConfiguration config2 = createCacheConfig("cache-b");
        DataSourceConfiguration config3 = createCacheConfig("cache-c");
        
        // When: getOrCreate is called with different names
        ExternalDataSource ds1 = registry.getOrCreate("cache-a", config1);
        ExternalDataSource ds2 = registry.getOrCreate("cache-b", config2);
        ExternalDataSource ds3 = registry.getOrCreate("cache-c", config3);
        
        // Then: Different instances are created
        assertNotSame(ds1, ds2, "Different names should create different instances");
        assertNotSame(ds2, ds3, "Different names should create different instances");
        assertNotSame(ds1, ds3, "Different names should create different instances");
        assertEquals(3, registry.size(), "Three data sources should be registered");
        
        LOGGER.info("TEST PASSED: Different names create different data sources");
    }
    
    @Test
    @DisplayName("Should work with get() for lookup without creation")
    void shouldWorkWithGetForLookupOnly() throws DataSourceException {
        LOGGER.info("================================================================================");
        LOGGER.info("TEST: get() for lookup without creation");
        LOGGER.info("================================================================================");
        
        // Given: No data source registered
        
        // When: get() is called
        Optional<ExternalDataSource> notFound = registry.get("nonexistent");
        
        // Then: Returns empty optional (no creation)
        assertTrue(notFound.isEmpty(), "Should return empty for nonexistent data source");
        assertEquals(0, registry.size(), "No data source should be created");
        
        // When: getOrCreate is used to create, then get() is called
        DataSourceConfiguration config = createCacheConfig("lookup-test");
        registry.getOrCreate("lookup-test", config);
        Optional<ExternalDataSource> found = registry.get("lookup-test");
        
        // Then: Returns the data source
        assertTrue(found.isPresent(), "Should find existing data source");
        assertEquals("lookup-test", found.get().getName(), "Name should match");
        
        LOGGER.info("TEST PASSED: get() correctly handles lookup without creation");
    }
    
    @Test
    @DisplayName("Should track pool cache statistics")
    void shouldTrackPoolCacheStatistics() throws DataSourceException {
        LOGGER.info("================================================================================");
        LOGGER.info("TEST: Pool cache statistics tracking");
        LOGGER.info("================================================================================");
        
        // Given: Initial empty state
        Map<String, Integer> initialStats = registry.getPoolCacheStats();
        LOGGER.info("Initial stats: {}", initialStats);
        
        // When: Data sources are created
        registry.getOrCreate("cache-1", createCacheConfig("cache-1"));
        registry.getOrCreate("cache-2", createCacheConfig("cache-2"));
        
        // Then: Statistics are updated
        Map<String, Integer> stats = registry.getPoolCacheStats();
        LOGGER.info("Stats after creation: {}", stats);
        
        assertEquals(2, stats.get("dataSources"), "Should track data source count");
        assertEquals(0, stats.get("pendingCreations"), "No pending creations");
        
        LOGGER.info("TEST PASSED: Pool cache statistics tracked correctly");
    }
    
    @Test
    @DisplayName("Should support contains() check")
    void shouldSupportContainsCheck() throws DataSourceException {
        LOGGER.info("================================================================================");
        LOGGER.info("TEST: contains() method");
        LOGGER.info("================================================================================");
        
        // Given: A data source
        DataSourceConfiguration config = createCacheConfig("contains-test");
        
        // When/Then: contains() returns correct values
        assertFalse(registry.contains("contains-test"), "Should not contain before creation");
        assertFalse(registry.contains(null), "Should return false for null");
        
        registry.getOrCreate("contains-test", config);
        
        assertTrue(registry.contains("contains-test"), "Should contain after creation");
        
        LOGGER.info("TEST PASSED: contains() works correctly");
    }
    
    // Helper method to create cache data source configuration
    private DataSourceConfiguration createCacheConfig(String name) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName(name);
        config.setType("cache");
        
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setMaxSize(100);
        cacheConfig.setTtlSeconds(300L);
        config.setCache(cacheConfig);
        
        return config;
    }
}
