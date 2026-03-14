package dev.mars.apex.core.service.data.external.database;

import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.HealthCheckConfig;
import dev.mars.apex.core.service.data.external.ConnectionStatus;
import dev.mars.apex.core.service.data.external.DataSourceMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseDataSourceConcurrencyTest {

    private DataSource jdbcDataSource;
    private DatabaseDataSource databaseDataSource;
    private DataSourceConfiguration configuration;

    @BeforeEach
    void setUp() throws Exception {
        configuration = createTestConfiguration();
        jdbcDataSource = JdbcTemplateFactory.createDataSource(configuration);
        initializeTestDatabase();

        databaseDataSource = new DatabaseDataSource(jdbcDataSource, configuration);
        databaseDataSource.initialize(configuration);
    }

    @AfterEach
    void tearDown() {
        if (databaseDataSource != null) {
            databaseDataSource.shutdown();
        }
        JdbcTemplateFactory.clearCache();
    }

    @Test
    @DisplayName("Should handle concurrent getData calls for same query")
    void shouldHandleConcurrentGetDataCallsForSameQuery() throws Exception {
        int operations = 48;
        ExecutorService executor = Executors.newFixedThreadPool(12);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Map<String, Object>>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < operations; i++) {
                futures.add(executor.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) databaseDataSource.getData("users", 1);
                    return result;
                }));
            }

            start.countDown();

            for (Future<Map<String, Object>> future : futures) {
                Map<String, Object> result = future.get(10, TimeUnit.SECONDS);
                assertNotNull(result);
                assertEquals(1, result.get("ID"));
                assertEquals("Test User 1", result.get("NAME"));
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        DataSourceMetrics metrics = databaseDataSource.getMetrics();
        assertEquals(operations, metrics.getSuccessfulRequests());
        assertEquals(0, metrics.getFailedRequests());
        assertTrue(metrics.getCacheHits() > 0);
        assertEquals(metrics.getTotalRequests(), metrics.getSuccessfulRequests() + metrics.getFailedRequests());
    }

    @Test
    @DisplayName("Should handle concurrent query and shutdown")
    void shouldHandleConcurrentQueryAndShutdown() throws Exception {
        int queryThreads = 8;
        int iterationsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(queryThreads + 1);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        try {
            for (int thread = 0; thread < queryThreads; thread++) {
                futures.add(executor.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    for (int i = 0; i < iterationsPerThread; i++) {
                        try {
                            List<Map<String, Object>> result = databaseDataSource.query(
                                "SELECT * FROM test_users WHERE id = 1",
                                Collections.emptyMap()
                            );
                            assertEquals(1, result.size());
                            assertEquals("Test User 1", result.get(0).get("NAME"));
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
                databaseDataSource.shutdown();
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
        assertEquals(ConnectionStatus.State.SHUTDOWN, databaseDataSource.getConnectionStatus().getState());

        DataSourceMetrics metrics = databaseDataSource.getMetrics();
        assertEquals(queryThreads * iterationsPerThread, metrics.getSuccessfulRequests());
        assertEquals(0, metrics.getFailedRequests());
        assertEquals(metrics.getTotalRequests(), metrics.getSuccessfulRequests() + metrics.getFailedRequests());
    }

    private DataSourceConfiguration createTestConfiguration() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("database-concurrency-test");
        config.setType("database");
        config.setSourceType("h2");
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setDatabase("mem:database_concurrency_" + System.nanoTime());
        connectionConfig.setUsername("sa");
        connectionConfig.setPassword("");
        config.setConnection(connectionConfig);

        Map<String, String> queries = new HashMap<>();
        queries.put("default", "SELECT * FROM test_users");
        queries.put("users", "SELECT * FROM test_users WHERE id = :id");
        config.setQueries(queries);
        config.setParameterNames(new String[]{"id"});

        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(300L);
        cacheConfig.setMaxSize(100);
        config.setCache(cacheConfig);

        HealthCheckConfig healthCheckConfig = new HealthCheckConfig();
        healthCheckConfig.setEnabled(false);
        config.setHealthCheck(healthCheckConfig);

        return config;
    }

    private void initializeTestDatabase() throws SQLException {
        try (Connection connection = jdbcDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS test_users");
            statement.execute("CREATE TABLE test_users (id INTEGER PRIMARY KEY, name VARCHAR(50))");
            statement.execute("INSERT INTO test_users VALUES (1, 'Test User 1')");
            statement.execute("INSERT INTO test_users VALUES (2, 'Test User 2')");
        }
    }
}