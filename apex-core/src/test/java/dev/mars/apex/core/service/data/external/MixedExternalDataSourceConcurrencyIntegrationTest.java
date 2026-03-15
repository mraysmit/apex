package dev.mars.apex.core.service.data.external;

import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MixedExternalDataSourceConcurrencyIntegrationTest {

    @TempDir
    Path tempDir;

    private DataSourceFactory factory;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        factory = DataSourceFactory.getInstance();
        executorService = Executors.newFixedThreadPool(12);
    }

    @AfterEach
    void tearDown() throws DataSourceException {
        factory.clearCache();
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("Should handle mixed data source activity during factory cache clears")
    void shouldHandleMixedDataSourceActivityDuringFactoryCacheClears() throws Exception {
        Path csvFile = createTestCsvFile();
        ExternalDataSource fileDataSource = factory.createDataSource(createCsvFileConfiguration(csvFile.toString()));
        ExternalDataSource cacheDataSource = factory.createDataSource(createCacheConfiguration("mixed-cache-test"));

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(3);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger fileQueries = new AtomicInteger();
        AtomicInteger cacheOperations = new AtomicInteger();

        executorService.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 50; i++) {
                    List<Object> results = fileDataSource.query("findByName", Map.of("name", "User" + (i % 5)));
                    assertNotNull(results);
                    fileQueries.incrementAndGet();
                }
            } catch (Throwable throwable) {
                failures.add(throwable);
            } finally {
                doneLatch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 50; i++) {
                    String key = "mixed-key-" + i;
                    cacheDataSource.query("put", Map.of("key", key, "value", "value-" + i));
                    Object value = cacheDataSource.queryForObject("get", Map.of("key", key));
                    assertNotNull(value);
                    cacheOperations.incrementAndGet();
                }
            } catch (Throwable throwable) {
                failures.add(throwable);
            } finally {
                doneLatch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 25; i++) {
                    factory.clearCache();
                    Thread.sleep(2L);
                }
            } catch (Throwable throwable) {
                failures.add(throwable);
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();

        assertTrue(doneLatch.await(30, TimeUnit.SECONDS),
            "Mixed data source activity should complete within 30 seconds");
        assertTrue(failures.isEmpty(),
            "No exceptions should occur during mixed data source activity: " + failures);
        assertEquals(50, fileQueries.get(), "All file queries should complete");
        assertEquals(50, cacheOperations.get(), "All cache operations should complete");

        DataSourceMetrics fileMetrics = fileDataSource.getMetrics();
        assertEquals(fileQueries.get(), fileMetrics.getSuccessfulRequests(),
            "File data source should record all successful queries");

        DataSourceMetrics cacheMetrics = cacheDataSource.getMetrics();
        assertEquals(cacheOperations.get(), cacheMetrics.getCacheHits(),
            "Cache data source should record cache hits for every successful get");
        assertEquals(0, fileMetrics.getFailedRequests(), "File data source should not record failures");
        assertEquals(0, cacheMetrics.getFailedRequests(), "Cache data source should not record failures");

        fileDataSource.shutdown();
        cacheDataSource.shutdown();
    }

    private Path createTestCsvFile() throws IOException {
        Path csvFile = tempDir.resolve("mixed-concurrency-test-data.csv");
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

    private DataSourceConfiguration createCsvFileConfiguration(String filePath) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("mixed-concurrent-csv-test");
        config.setSourceType("file-system");
        config.setDataSourceType(DataSourceType.FILE_SYSTEM);
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        Path file = Path.of(filePath);
        connectionConfig.setBasePath(file.getParent().toString());
        connectionConfig.setFilePattern(file.getFileName().toString());
        config.setConnection(connectionConfig);

        Map<String, String> queries = new HashMap<>();
        queries.put("findByName", "SELECT * WHERE name = :name");
        queries.put("getAll", "SELECT *");
        config.setQueries(queries);

        return config;
    }

    private DataSourceConfiguration createCacheConfiguration(String name) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName(name);
        config.setSourceType("memory");
        config.setDataSourceType(DataSourceType.CACHE);
        config.setEnabled(true);

        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(300L);
        cacheConfig.setMaxSize(1000);
        config.setCache(cacheConfig);

        Map<String, String> queries = new HashMap<>();
        queries.put("put", "put");
        queries.put("get", "get");
        queries.put("remove", "remove");
        config.setQueries(queries);

        return config;
    }
}