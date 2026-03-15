package dev.mars.apex.core.service.data.external.file;

import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.FileFormatConfig;
import dev.mars.apex.core.service.data.external.ConnectionStatus;
import dev.mars.apex.core.service.data.external.DataSourceMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileSystemDataSourceConcurrencyTest {

    @TempDir
    Path tempDir;

    private FileSystemDataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(tempDir.resolve("users.csv"), "id,name\n1,Test User 1\n2,Test User 2\n");

        DataSourceConfiguration configuration = createConfiguration(tempDir);
        dataSource = new FileSystemDataSource(configuration);
        dataSource.initialize(configuration);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.shutdown();
        }
    }

    @Test
    @DisplayName("Should serve concurrent reads while monitoring is active")
    void shouldServeConcurrentReadsWhileMonitoringIsActive() throws Exception {
        int operations = 40;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Map<String, Object>>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < operations; i++) {
                futures.add(executor.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) dataSource.getData("users", 1);
                    return result;
                }));
            }

            start.countDown();

            for (Future<Map<String, Object>> future : futures) {
                Map<String, Object> result = future.get(10, TimeUnit.SECONDS);
                assertNotNull(result);
                assertEquals(1L, ((Number) result.get("id")).longValue());
                assertEquals("Test User 1", result.get("name"));
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        assertTrue(readMonitoringFlag());

        DataSourceMetrics metrics = dataSource.getMetrics();
        assertEquals(operations, metrics.getSuccessfulRequests());
        assertEquals(0, metrics.getFailedRequests());
        assertEquals(metrics.getTotalRequests(), metrics.getSuccessfulRequests() + metrics.getFailedRequests());
    }

    @Test
    @DisplayName("Should shutdown file monitoring cleanly under load")
    void shouldShutdownFileMonitoringCleanlyUnderLoad() throws Exception {
        int readerThreads = 6;
        int iterationsPerThread = 15;
        ExecutorService executor = Executors.newFixedThreadPool(readerThreads + 1);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        try {
            for (int thread = 0; thread < readerThreads; thread++) {
                futures.add(executor.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    for (int i = 0; i < iterationsPerThread; i++) {
                        try {
                            Object result = dataSource.getData("users", 1);
                            assertNotNull(result);
                        } catch (Throwable throwable) {
                            failures.add(throwable);
                        }
                    }
                    return null;
                }));
            }

            futures.add(executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                Thread.sleep(25L);
                dataSource.shutdown();
                return null;
            }));

            start.countDown();

            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        assertTrue(failures.isEmpty(), () -> "Unexpected failures: " + failures);
        assertEquals(ConnectionStatus.State.SHUTDOWN, dataSource.getConnectionStatus().getState());
        assertFalse(readMonitoringFlag());

        ScheduledExecutorService monitorExecutor = readMonitorExecutor();
        assertNotNull(monitorExecutor);
        assertTrue(monitorExecutor.isShutdown());
    }

    private DataSourceConfiguration createConfiguration(Path basePath) {
        DataSourceConfiguration configuration = new DataSourceConfiguration();
        configuration.setName("file-concurrency-test");
        configuration.setType("file-system");
        configuration.setSourceType("csv");
        configuration.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setBasePath(basePath.toString());
        connectionConfig.setFilePattern("*.csv");
        connectionConfig.setPollingInterval(1);
        configuration.setConnection(connectionConfig);

        FileFormatConfig fileFormatConfig = new FileFormatConfig("csv");
        fileFormatConfig.setHeaderRow(true);
        fileFormatConfig.setKeyColumn("id");
        configuration.setFileFormat(fileFormatConfig);

        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(60L);
        cacheConfig.setMaxSize(100);
        configuration.setCache(cacheConfig);

        return configuration;
    }

    private boolean readMonitoringFlag() throws Exception {
        Field field = FileSystemDataSource.class.getDeclaredField("monitoring");
        field.setAccessible(true);
        return field.getBoolean(dataSource);
    }

    private ScheduledExecutorService readMonitorExecutor() throws Exception {
        Field field = FileSystemDataSource.class.getDeclaredField("fileMonitorExecutor");
        field.setAccessible(true);
        return (ScheduledExecutorService) field.get(dataSource);
    }
}