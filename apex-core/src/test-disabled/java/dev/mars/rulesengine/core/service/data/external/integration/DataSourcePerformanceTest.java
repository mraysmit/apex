package dev.mars.apex.core.service.data.external.integration;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.manager.DataSourceManager;
import dev.mars.apex.core.service.data.external.registry.DataSourceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and load tests for data source integration.
 * 
 * These tests verify the performance characteristics and concurrent
 * behavior of the data source management system under load.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
class DataSourcePerformanceTest {
    
    private DataSourceManager manager;
    private DataSourceRegistry registry;
    
    @BeforeEach
    void setUp() {
        registry = DataSourceRegistry.getInstance();
        manager = new DataSourceManager();
        
        // Clear any existing registrations
        for (String name : registry.getDataSourceNames()) {
            registry.unregister(name);
        }
    }
    
    @AfterEach
    void tearDown() {
        if (manager.isRunning()) {
            manager.shutdown();
        }
        
        // Clear registrations
        for (String name : registry.getDataSourceNames()) {
            registry.unregister(name);
        }
    }
    
    @Test
    @Timeout(30)
    void testConcurrentDataSourceRegistration() throws DataSourceException, InterruptedException {
        // Arrange
        int threadCount = 10;
        int dataSourcesPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        // Act - Concurrent registration
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    List<DataSourceConfiguration> configs = new ArrayList<>();
                    
                    for (int i = 0; i < dataSourcesPerThread; i++) {
                        DataSourceConfiguration config = new DataSourceConfiguration();
                        config.setName("thread-" + threadId + "-source-" + i);
                        config.setSourceType("memory");
                        config.setDataSourceType(DataSourceType.CACHE);
                        
                        CacheConfig cache = new CacheConfig();
                        cache.setEnabled(true);
                        cache.setMaxSize(100);
                        config.setCache(cache);
                        
                        configs.add(config);
                    }
                    
                    // Initialize manager for this thread's data sources
                    DataSourceManager threadManager = new DataSourceManager();
                    threadManager.initialize(configs);
                    
                    successCount.addAndGet(dataSourcesPerThread);
                    
                    // Clean up
                    threadManager.shutdown();
                    
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Assert - Wait for completion
        assertTrue(latch.await(25, TimeUnit.SECONDS));
        
        // Verify results
        int expectedSuccess = threadCount * dataSourcesPerThread;
        assertEquals(expectedSuccess, successCount.get());
        assertEquals(0, errorCount.get());
        
        executor.shutdown();
    }
    
    @Test
    @Timeout(20)
    void testHighVolumeDataSourceOperations() throws DataSourceException, InterruptedException {
        // Arrange - Create multiple cache data sources
        List<DataSourceConfiguration> configs = new ArrayList<>();
        int dataSourceCount = 20;
        
        for (int i = 0; i < dataSourceCount; i++) {
            DataSourceConfiguration config = new DataSourceConfiguration();
            config.setName("perf-source-" + i);
            config.setSourceType("memory");
            config.setDataSourceType(DataSourceType.CACHE);
            
            CacheConfig cache = new CacheConfig();
            cache.setEnabled(true);
            cache.setMaxSize(1000);
            config.setCache(cache);
            
            configs.add(config);
        }
        
        manager.initialize(configs);
        
        // Act - Perform high volume operations
        int operationCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(operationCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < operationCount; i++) {
            final int operationId = i;
            executor.submit(() -> {
                try {
                    // Get random data source
                    String sourceName = "perf-source-" + (operationId % dataSourceCount);
                    ExternalDataSource dataSource = manager.getDataSource(sourceName);
                    
                    if (dataSource != null && dataSource.isHealthy()) {
                        // Perform query operation
                        Map<String, Object> parameters = new HashMap<>();
                        parameters.put("key", "test-key-" + operationId);
                        
                        dataSource.query("test-pattern", parameters);
                        successCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    // Expected for some operations
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Assert - Wait for completion and measure performance
        assertTrue(latch.await(15, TimeUnit.SECONDS));
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify performance metrics
        assertTrue(successCount.get() > 0);
        double operationsPerSecond = (double) operationCount / (duration / 1000.0);
        
        // Should handle at least 100 operations per second
        assertTrue(operationsPerSecond > 100, 
            "Performance too low: " + operationsPerSecond + " ops/sec");
        
        System.out.println("Performance: " + operationsPerSecond + " operations/second");
        System.out.println("Success rate: " + (successCount.get() * 100.0 / operationCount) + "%");
        
        executor.shutdown();
    }
    
    @Test
    @Timeout(15)
    void testDataSourceHealthMonitoringPerformance() throws DataSourceException, InterruptedException {
        // Arrange - Create data sources with varying health
        List<DataSourceConfiguration> configs = new ArrayList<>();
        int dataSourceCount = 50;
        
        for (int i = 0; i < dataSourceCount; i++) {
            DataSourceConfiguration config = new DataSourceConfiguration();
            config.setName("health-source-" + i);
            config.setSourceType("memory");
            config.setDataSourceType(DataSourceType.CACHE);
            
            CacheConfig cache = new CacheConfig();
            cache.setEnabled(true);
            cache.setMaxSize(100);
            config.setCache(cache);
            
            configs.add(config);
        }
        
        manager.initialize(configs);
        
        // Act - Perform concurrent health checks
        int healthCheckCount = 500;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(healthCheckCount);
        AtomicInteger healthyCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < healthCheckCount; i++) {
            executor.submit(() -> {
                try {
                    List<ExternalDataSource> healthySources = manager.getHealthyDataSources();
                    healthyCount.addAndGet(healthySources.size());
                    
                } catch (Exception e) {
                    // Handle errors
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Assert - Verify health monitoring performance
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        double healthChecksPerSecond = (double) healthCheckCount / (duration / 1000.0);
        
        // Should handle at least 50 health checks per second
        assertTrue(healthChecksPerSecond > 50, 
            "Health monitoring performance too low: " + healthChecksPerSecond + " checks/sec");
        
        System.out.println("Health monitoring: " + healthChecksPerSecond + " checks/second");
        
        executor.shutdown();
    }
    
    @Test
    @Timeout(10)
    void testDataSourceStatisticsPerformance() throws DataSourceException, InterruptedException {
        // Arrange - Create data sources
        List<DataSourceConfiguration> configs = new ArrayList<>();
        int dataSourceCount = 30;
        
        for (int i = 0; i < dataSourceCount; i++) {
            DataSourceConfiguration config = new DataSourceConfiguration();
            config.setName("stats-source-" + i);
            config.setSourceType("memory");
            config.setDataSourceType(DataSourceType.CACHE);
            
            CacheConfig cache = new CacheConfig();
            cache.setEnabled(true);
            cache.setMaxSize(100);
            config.setCache(cache);
            
            configs.add(config);
        }
        
        manager.initialize(configs);
        
        // Act - Perform concurrent statistics collection
        int statsCollectionCount = 200;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(statsCollectionCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < statsCollectionCount; i++) {
            executor.submit(() -> {
                try {
                    var managerStats = manager.getStatistics();
                    var registryStats = registry.getStatistics();
                    
                    if (managerStats != null && registryStats != null) {
                        successCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    // Handle errors
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Assert - Verify statistics collection performance
        assertTrue(latch.await(8, TimeUnit.SECONDS));
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        double statsPerSecond = (double) statsCollectionCount / (duration / 1000.0);
        
        // Should handle at least 25 statistics collections per second
        assertTrue(statsPerSecond > 25, 
            "Statistics collection performance too low: " + statsPerSecond + " collections/sec");
        
        System.out.println("Statistics collection: " + statsPerSecond + " collections/second");
        System.out.println("Success rate: " + (successCount.get() * 100.0 / statsCollectionCount) + "%");
        
        executor.shutdown();
    }
    
    @Test
    @Timeout(10)
    void testMemoryUsageUnderLoad() throws DataSourceException, InterruptedException {
        // Arrange - Record initial memory usage
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest garbage collection
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create many data sources
        List<DataSourceConfiguration> configs = new ArrayList<>();
        int dataSourceCount = 100;
        
        for (int i = 0; i < dataSourceCount; i++) {
            DataSourceConfiguration config = new DataSourceConfiguration();
            config.setName("memory-source-" + i);
            config.setSourceType("memory");
            config.setDataSourceType(DataSourceType.CACHE);
            
            CacheConfig cache = new CacheConfig();
            cache.setEnabled(true);
            cache.setMaxSize(50);
            config.setCache(cache);
            
            configs.add(config);
        }
        
        // Act - Initialize and perform operations
        manager.initialize(configs);
        
        // Perform operations to stress memory
        for (int i = 0; i < 1000; i++) {
            String sourceName = "memory-source-" + (i % dataSourceCount);
            ExternalDataSource dataSource = manager.getDataSource(sourceName);
            
            if (dataSource != null) {
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("key", "test-" + i);
                
                try {
                    dataSource.query("pattern", parameters);
                } catch (Exception e) {
                    // Expected for some operations
                }
            }
        }
        
        // Assert - Check memory usage
        runtime.gc(); // Suggest garbage collection
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        // Memory increase should be reasonable (less than 100MB for this test)
        long maxAcceptableIncrease = 100 * 1024 * 1024; // 100MB
        assertTrue(memoryIncrease < maxAcceptableIncrease, 
            "Memory usage increased too much: " + (memoryIncrease / 1024 / 1024) + "MB");
        
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + "MB");
    }
}
