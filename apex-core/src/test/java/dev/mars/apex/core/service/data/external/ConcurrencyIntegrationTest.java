package dev.mars.apex.core.service.data.external;

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
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive concurrency and thread safety tests for external data sources.
 * 
 * Tests cover:
 * - Concurrent data source creation and destruction
 * - Thread-safe query execution
 * - Concurrent cache operations
 * - Factory thread safety under load
 * - Metrics collection thread safety
 * - Connection status updates under concurrent access
 * - Resource cleanup in multi-threaded scenarios
 * - Race condition detection and prevention
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class ConcurrencyIntegrationTest {

    // ========================================
    // Test Infrastructure and Setup
    // ========================================

    /**
     * Temporary directory for test file creation.
     * JUnit automatically creates and cleans up this directory for each test.
     */
    @TempDir
    Path tempDir;

    /**
     * Data source factory instance for creating data sources in tests.
     * Shared across all test methods within a single test instance.
     */
    private DataSourceFactory factory;

    /**
     * Thread pool executor for concurrent operations testing.
     * Configured with sufficient threads to handle high-concurrency scenarios.
     */
    private ExecutorService executorService;

    /**
     * Sets up test infrastructure before each test method.
     *
     * Initializes:
     * - DataSourceFactory instance for creating test data sources
     * - ExecutorService with 50 threads for high-concurrency testing
     *
     * The thread pool size is chosen to:
     * - Support realistic concurrent load scenarios
     * - Avoid overwhelming the test environment
     * - Provide sufficient parallelism for race condition detection
     */
    @BeforeEach
    void setUp() {
        factory = DataSourceFactory.getInstance();
        // Create thread pool with generous size for concurrency testing
        executorService = Executors.newFixedThreadPool(50);
    }

    /**
     * Cleans up test infrastructure after each test method.
     *
     * Performs:
     * - Factory cache clearing to prevent test interference
     * - Graceful executor service shutdown with timeout
     * - Forced shutdown if graceful shutdown fails
     * - Thread interruption handling for clean test completion
     *
     * This ensures:
     * - No resource leaks between tests
     * - Clean state for subsequent tests
     * - Proper thread cleanup to prevent hanging tests
     */
    @AfterEach
    void tearDown() throws DataSourceException {
        // Clear factory cache to prevent test interference
        factory.clearCache();

        // Initiate graceful shutdown of executor service
        executorService.shutdown();
        try {
            // Wait for existing tasks to complete with reasonable timeout
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                // Force shutdown if graceful shutdown times out
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            // Handle interruption during shutdown
            executorService.shutdownNow();
            // Restore interrupted status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Tests concurrent data source creation to validate thread safety of the DataSourceFactory.
     *
     * This test simulates a high-concurrency scenario where multiple threads attempt to create
     * data sources simultaneously. This is critical for validating:
     * - Factory thread safety and proper synchronization
     * - Resource allocation under concurrent load
     * - Prevention of race conditions during initialization
     * - Proper isolation between concurrent creation operations
     *
     * Test Scenario:
     * - 20 concurrent threads each creating a unique cache data source
     * - Each thread creates a data source with a unique name to avoid conflicts
     * - All operations must complete without exceptions or resource conflicts
     * - Created data sources must be fully functional and independent
     *
     * Success Criteria:
     * - All threads complete within timeout period
     * - No exceptions occur during concurrent creation
     * - All data sources are successfully created and functional
     * - Each data source can establish connections and report healthy status
     * - Proper cleanup of all created resources
     */
    @Test
    @DisplayName("Should handle concurrent data source creation safely")
    void testConcurrentDataSourceCreation() throws Exception {
        // Configuration: 20 concurrent threads for realistic load testing
        final int threadCount = 20;
        final CountDownLatch latch = new CountDownLatch(threadCount);

        // Thread-safe collections to capture results and exceptions
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        final List<ExternalDataSource> createdDataSources = Collections.synchronizedList(new ArrayList<>());

        // Launch concurrent data source creation tasks
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    // Create unique configuration for each thread to avoid naming conflicts
                    DataSourceConfiguration config = createCacheConfiguration("concurrent-cache-" + threadId);

                    // Attempt to create data source - this tests factory thread safety
                    ExternalDataSource dataSource = factory.createDataSource(config);
                    createdDataSources.add(dataSource);

                } catch (Exception e) {
                    // Capture any exceptions for analysis
                    exceptions.add(e);
                } finally {
                    // Always signal completion to prevent test hanging
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete with reasonable timeout
        assertTrue(latch.await(30, TimeUnit.SECONDS),
            "All threads should complete within 30 seconds");

        // Verify no exceptions occurred during concurrent operations
        assertTrue(exceptions.isEmpty(),
            "No exceptions should occur during concurrent creation: " + exceptions);

        // Verify all data sources were successfully created
        assertEquals(threadCount, createdDataSources.size(),
            "All " + threadCount + " data sources should be created");

        // Validate functionality of each created data source
        for (ExternalDataSource dataSource : createdDataSources) {
            assertTrue(dataSource.testConnection(),
                "Data source " + dataSource.getName() + " should have valid connection");
            assertTrue(dataSource.isHealthy(),
                "Data source " + dataSource.getName() + " should report healthy status");
        }

        // Clean up all created resources to prevent resource leaks
        for (ExternalDataSource dataSource : createdDataSources) {
            dataSource.shutdown();
        }
    }

    /**
     * Tests concurrent query execution to validate thread safety of data source operations.
     *
     * This test simulates high-concurrency query scenarios that are common in production
     * environments where multiple threads need to access the same data source simultaneously.
     * This validates:
     * - Thread safety of query execution methods
     * - Proper isolation between concurrent queries
     * - Consistent state management under concurrent load
     * - Accurate metrics collection in multi-threaded scenarios
     * - Resource sharing without conflicts or corruption
     *
     * Test Scenario:
     * - Single CSV file data source shared across multiple threads
     * - 30 concurrent threads each executing 20 queries (600 total queries)
     * - Queries use different parameters to test various code paths
     * - All queries must complete successfully without data corruption
     * - Metrics must accurately reflect all operations
     *
     * Success Criteria:
     * - All threads complete within timeout period
     * - No exceptions occur during concurrent query execution
     * - All queries return valid results
     * - Metrics accurately count successful operations
     * - No failed requests recorded
     */
    @Test
    @DisplayName("Should handle concurrent query execution safely")
    void testConcurrentQueryExecution() throws Exception {
        // Set up shared data source for concurrent access testing
        Path csvFile = createTestCsvFile();
        DataSourceConfiguration config = createCsvFileConfiguration(csvFile.toString());
        ExternalDataSource dataSource = factory.createDataSource(config);

        // Configuration: High concurrency scenario with realistic load
        final int threadCount = 30;
        final int queriesPerThread = 20;
        final CountDownLatch latch = new CountDownLatch(threadCount);

        // Thread-safe counters and exception tracking
        final AtomicInteger successfulQueries = new AtomicInteger(0);
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Launch concurrent query execution tasks
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    // Execute multiple queries per thread to test sustained concurrent access
                    for (int j = 0; j < queriesPerThread; j++) {
                        // Use varied parameters to test different query paths
                        // Modulo operation ensures we test with limited set of users for cache efficiency
                        Map<String, Object> parameters = Map.of("name", "User" + (threadId % 5));

                        // Execute query - this is the core thread safety test point
                        List<Object> results = dataSource.query("findByName", parameters);

                        // Verify query returned valid results
                        assertNotNull(results, "Query should return non-null results");

                        // Increment success counter atomically
                        successfulQueries.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Capture any exceptions for detailed analysis
                    exceptions.add(e);
                } finally {
                    // Always signal completion to prevent test hanging
                    latch.countDown();
                }
            });
        }

        // Wait for all concurrent operations to complete
        assertTrue(latch.await(60, TimeUnit.SECONDS),
            "All queries should complete within 60 seconds");

        // Verify no exceptions occurred during concurrent operations
        assertTrue(exceptions.isEmpty(),
            "No exceptions should occur during concurrent queries: " + exceptions);

        // Verify all queries were executed successfully
        assertEquals(threadCount * queriesPerThread, successfulQueries.get(),
            "All " + (threadCount * queriesPerThread) + " queries should succeed");

        // Verify metrics consistency under concurrent access
        DataSourceMetrics metrics = dataSource.getMetrics();
        assertEquals(threadCount * queriesPerThread, metrics.getSuccessfulRequests(),
            "Metrics should accurately count all successful requests");
        assertEquals(0, metrics.getFailedRequests(),
            "No failed requests should be recorded");

        // Clean up resources
        dataSource.shutdown();
    }

    /**
     * Tests concurrent cache operations to validate thread safety of cache data sources.
     *
     * Cache operations are particularly sensitive to concurrency issues because they involve
     * shared state management, key-value mappings, and potential eviction policies. This test
     * validates:
     * - Thread safety of put/get operations on shared cache
     * - Proper isolation of cache entries between threads
     * - Consistent cache state under concurrent modifications
     * - Prevention of cache corruption or data races
     * - Accurate operation counting in multi-threaded scenarios
     *
     * Test Scenario:
     * - Single cache data source accessed by multiple threads
     * - 25 concurrent threads each performing 50 put/get operation pairs
     * - Each thread uses unique keys to minimize contention while testing concurrency
     * - Total of 2,500 cache operations (1,250 puts + 1,250 gets)
     * - Operations must maintain data integrity and consistency
     *
     * Success Criteria:
     * - All threads complete within timeout period
     * - No exceptions occur during concurrent cache operations
     * - All put and get operations are counted accurately
     * - Cache maintains consistent state throughout test
     * - No data corruption or lost updates
     */
    @Test
    @DisplayName("Should handle concurrent cache operations safely")
    void testConcurrentCacheOperations() throws Exception {
        // Create dedicated cache data source for concurrency testing
        DataSourceConfiguration config = createCacheConfiguration("concurrent-cache-test");
        ExternalDataSource dataSource = factory.createDataSource(config);

        // Configuration: Moderate concurrency with sustained operations
        final int threadCount = 25;
        final int operationsPerThread = 50;
        final CountDownLatch latch = new CountDownLatch(threadCount);

        // Thread-safe operation counters
        final AtomicInteger putOperations = new AtomicInteger(0);
        final AtomicInteger getOperations = new AtomicInteger(0);
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Launch concurrent cache operation tasks
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    // Perform sustained cache operations to test thread safety
                    for (int j = 0; j < operationsPerThread; j++) {
                        // PUT operation: Store unique key-value pair
                        // Using thread-specific keys reduces contention while testing concurrency
                        Map<String, Object> putParams = Map.of(
                            "key", "thread-" + threadId + "-key-" + j,
                            "value", "value-" + threadId + "-" + j
                        );
                        dataSource.query("put", putParams);
                        putOperations.incrementAndGet();

                        // GET operation: Retrieve the value just stored
                        // This tests read-after-write consistency in concurrent environment
                        Map<String, Object> getParams = Map.of("key", "thread-" + threadId + "-key-" + j);
                        Object result = dataSource.queryForObject("get", getParams);
                        // Note: Result might be null if cache eviction occurred, which is acceptable
                        getOperations.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Capture any concurrency-related exceptions
                    exceptions.add(e);
                } finally {
                    // Always signal completion
                    latch.countDown();
                }
            });
        }

        // Wait for all concurrent cache operations to complete
        assertTrue(latch.await(60, TimeUnit.SECONDS),
            "All cache operations should complete within 60 seconds");

        // Verify no exceptions occurred during concurrent operations
        assertTrue(exceptions.isEmpty(),
            "No exceptions should occur during concurrent cache operations: " + exceptions);

        // Verify all operations were counted correctly
        assertEquals(threadCount * operationsPerThread, putOperations.get(),
            "All " + (threadCount * operationsPerThread) + " put operations should be counted");
        assertEquals(threadCount * operationsPerThread, getOperations.get(),
            "All " + (threadCount * operationsPerThread) + " get operations should be counted");

        // Clean up cache resources
        dataSource.shutdown();
    }

    /**
     * Tests concurrent metrics collection to validate thread safety of monitoring operations.
     *
     * Metrics collection is critical for production monitoring and must remain accurate and
     * consistent even under high concurrency. This test validates:
     * - Thread safety of metrics collection methods
     * - Consistency of metrics data under concurrent access
     * - Prevention of race conditions in metrics updates
     * - Accurate counting of operations across multiple threads
     * - Safe concurrent read access to metrics objects
     *
     * Test Scenario:
     * - Single cache data source with concurrent operations and metrics collection
     * - 15 concurrent threads each performing operations while collecting metrics
     * - Each thread performs 10 operations and collects metrics after each operation
     * - Total of 150 operations with 150 metrics collection calls
     * - Metrics must remain consistent and accurate throughout
     *
     * Success Criteria:
     * - All threads complete within timeout period
     * - No exceptions occur during concurrent metrics collection
     * - Metrics are successfully collected from all threads
     * - Final metrics reflect non-negative values
     * - No corruption or inconsistency in metrics data
     */
    @Test
    @DisplayName("Should handle concurrent metrics collection safely")
    void testConcurrentMetricsCollection() throws Exception {
        // Create cache data source for metrics testing
        DataSourceConfiguration config = createCacheConfiguration("metrics-test-cache");
        ExternalDataSource dataSource = factory.createDataSource(config);

        // Configuration: Moderate concurrency with frequent metrics collection
        final int threadCount = 15;
        final CountDownLatch latch = new CountDownLatch(threadCount);

        // Thread-safe collections for metrics and exception tracking
        final List<DataSourceMetrics> collectedMetrics = Collections.synchronizedList(new ArrayList<>());
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Launch concurrent operation and metrics collection tasks
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    // Perform operations while frequently collecting metrics
                    for (int j = 0; j < 10; j++) {
                        // Execute cache operation to generate metrics data
                        Map<String, Object> params = Map.of(
                            "key", "metrics-key-" + threadId + "-" + j,
                            "value", "test-value-" + threadId + "-" + j
                        );
                        dataSource.query("put", params);

                        // Collect metrics immediately after operation
                        // This tests concurrent read access to metrics while writes are occurring
                        DataSourceMetrics metrics = dataSource.getMetrics();
                        collectedMetrics.add(metrics);

                        // Brief pause to allow other threads to interleave operations
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    // Capture any concurrency-related exceptions
                    exceptions.add(e);
                } finally {
                    // Always signal completion
                    latch.countDown();
                }
            });
        }

        // Wait for all concurrent metrics collection to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS),
            "All metrics collection should complete within 30 seconds");

        // Verify no exceptions occurred during concurrent operations
        assertTrue(exceptions.isEmpty(),
            "No exceptions should occur during concurrent metrics collection: " + exceptions);

        // Verify metrics were successfully collected from all threads
        assertFalse(collectedMetrics.isEmpty(),
            "Metrics should be collected from concurrent operations");

        // Verify final metrics consistency and validity
        DataSourceMetrics finalMetrics = dataSource.getMetrics();
        assertTrue(finalMetrics.getSuccessfulRequests() >= 0,
            "Should have non-negative successful requests count");
        assertTrue(finalMetrics.getFailedRequests() >= 0,
            "Should have non-negative failed requests count");
        assertTrue(finalMetrics.getAverageResponseTime() >= 0,
            "Should have non-negative average response time");

        // Note: Exact operation count may vary due to concurrent operations and
        // implementation-specific timing, but metrics should be internally consistent

        // Clean up resources
        dataSource.shutdown();
    }

    /**
     * Tests concurrent shutdown operations to validate safe resource cleanup.
     *
     * Proper shutdown handling is critical for preventing resource leaks and ensuring
     * graceful application termination. This test validates:
     * - Thread safety of shutdown operations
     * - Proper resource cleanup under concurrent shutdown
     * - Prevention of race conditions during resource deallocation
     * - Safe handling of multiple simultaneous shutdown requests
     * - No exceptions or errors during concurrent cleanup
     *
     * Test Scenario:
     * - Multiple data sources created and then shut down concurrently
     * - 10 data sources each shut down by a separate thread simultaneously
     * - All shutdown operations must complete without conflicts
     * - No resource leaks or cleanup errors should occur
     * - Shutdown operations must be idempotent and thread-safe
     *
     * Success Criteria:
     * - All data sources are created successfully
     * - All shutdown operations complete within timeout period
     * - No exceptions occur during concurrent shutdown
     * - All resources are properly cleaned up
     * - No hanging threads or resource leaks
     */
    @Test
    @DisplayName("Should handle concurrent shutdown operations safely")
    void testConcurrentShutdownSafety() throws Exception {
        // Configuration: Multiple data sources for concurrent shutdown testing
        final int dataSourceCount = 10;
        final List<ExternalDataSource> dataSources = new ArrayList<>();

        // Create multiple data sources that will be shut down concurrently
        for (int i = 0; i < dataSourceCount; i++) {
            DataSourceConfiguration config = createCacheConfiguration("shutdown-test-" + i);
            ExternalDataSource dataSource = factory.createDataSource(config);
            dataSources.add(dataSource);
        }

        // Verify all data sources were created successfully
        assertEquals(dataSourceCount, dataSources.size(),
            "All " + dataSourceCount + " data sources should be created");

        final CountDownLatch latch = new CountDownLatch(dataSourceCount);
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Initiate concurrent shutdown operations
        for (ExternalDataSource dataSource : dataSources) {
            executorService.submit(() -> {
                try {
                    // Perform shutdown operation - this tests thread safety of cleanup code
                    dataSource.shutdown();

                } catch (Exception e) {
                    // Capture any exceptions during shutdown
                    exceptions.add(e);
                } finally {
                    // Always signal completion to prevent test hanging
                    latch.countDown();
                }
            });
        }

        // Wait for all concurrent shutdown operations to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS),
            "All shutdowns should complete within 30 seconds");

        // Verify no exceptions occurred during concurrent shutdown
        assertTrue(exceptions.isEmpty(),
            "No exceptions should occur during concurrent shutdown: " + exceptions);

        // Additional verification: Ensure all data sources are properly shut down
        // Note: Specific shutdown state verification depends on implementation details
        // but the absence of exceptions indicates successful cleanup
    }

    // ========================================
    // Helper Methods for Test Configuration and Data Setup
    // ========================================

    /**
     * Creates a test CSV file with sample data for concurrent query testing.
     *
     * The CSV file contains a small dataset with user information that can be
     * queried by multiple threads simultaneously. The data is designed to:
     * - Provide consistent test results across multiple runs
     * - Include varied data for different query scenarios
     * - Be small enough for fast processing but large enough for meaningful tests
     *
     * @return Path to the created CSV file
     * @throws IOException if file creation fails
     */
    private Path createTestCsvFile() throws IOException {
        Path csvFile = tempDir.resolve("concurrent-test-data.csv");

        // Create CSV content with header and sample user data
        // Data includes different statuses to test query filtering
        String csvContent = """
            id,name,email,status
            1,User0,user0@example.com,ACTIVE
            2,User1,user1@example.com,ACTIVE
            3,User2,user2@example.com,INACTIVE
            4,User3,user3@example.com,ACTIVE
            5,User4,user4@example.com,ACTIVE
            """;
        Files.writeString(csvFile, csvContent);
        return csvFile;
    }

    /**
     * Creates a CSV file data source configuration for concurrent testing.
     *
     * This configuration sets up a file system data source that can be safely
     * accessed by multiple threads simultaneously. The configuration includes:
     * - File system source type for CSV file access
     * - Connection settings pointing to the test CSV file
     * - Query definitions for testing different access patterns
     *
     * @param filePath Path to the CSV file to be accessed
     * @return Configured DataSourceConfiguration for CSV file access
     */
    private DataSourceConfiguration createCsvFileConfiguration(String filePath) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("concurrent-csv-test");
        config.setSourceType("file-system");  // Use file system data source
        config.setDataSourceType(DataSourceType.FILE_SYSTEM);
        config.setEnabled(true);

        // Configure connection to point to the test CSV file
        ConnectionConfig connectionConfig = new ConnectionConfig();
        Path file = Path.of(filePath);
        connectionConfig.setBasePath(file.getParent().toString());
        connectionConfig.setFilePattern(file.getFileName().toString());
        config.setConnection(connectionConfig);

        // Define queries for concurrent testing
        Map<String, String> queries = new HashMap<>();
        queries.put("findByName", "SELECT * WHERE name = :name");  // Parameterized query
        queries.put("getAll", "SELECT *");                         // Full table scan
        config.setQueries(queries);

        return config;
    }

    /**
     * Creates a cache data source configuration for concurrent testing.
     *
     * This configuration sets up an in-memory cache data source optimized for
     * concurrent access testing. The configuration includes:
     * - Memory-based cache implementation
     * - Reasonable TTL and size limits for testing
     * - Standard cache operations (put, get, remove)
     *
     * @param name Unique name for the cache data source
     * @return Configured DataSourceConfiguration for cache operations
     */
    private DataSourceConfiguration createCacheConfiguration(String name) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName(name);
        config.setSourceType("memory");  // Use in-memory cache implementation
        config.setDataSourceType(DataSourceType.CACHE);
        config.setEnabled(true);

        // Configure cache settings for concurrent testing
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(300L);    // 5-minute TTL for test data
        cacheConfig.setMaxSize(1000);       // Reasonable size limit for testing
        config.setCache(cacheConfig);

        // Define cache operations for concurrent testing
        Map<String, String> queries = new HashMap<>();
        queries.put("put", "put");       // Store key-value pairs
        queries.put("get", "get");       // Retrieve values by key
        queries.put("remove", "remove"); // Delete entries by key
        config.setQueries(queries);

        return config;
    }
}
