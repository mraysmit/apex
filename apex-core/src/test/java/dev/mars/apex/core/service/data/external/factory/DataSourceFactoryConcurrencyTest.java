package dev.mars.apex.core.service.data.external.factory;

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
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.ConnectionStatus;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.DataSourceMetrics;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataSourceFactoryConcurrencyTest {

    private DataSourceFactory factory;

    @BeforeEach
    void setUp() {
        factory = DataSourceFactory.getInstance();
        factory.clearCache();
        for (int index = 0; index < 24; index++) {
            factory.unregisterProvider("concurrency-provider-" + index);
        }
    }

    @Test
    @DisplayName("Should return single factory instance under high contention")
    void shouldReturnSingleFactoryInstanceUnderHighContention() throws Exception {
        int threadCount = 100;
        int accessesPerThread = 1000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        Set<DataSourceFactory> instances = ConcurrentHashMap.newKeySet();

        for (int index = 0; index < threadCount; index++) {
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();
                    for (int attempt = 0; attempt < accessesPerThread; attempt++) {
                        instances.add(DataSourceFactory.getInstance());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
            thread.start();
        }

        startLatch.countDown();
        assertTrue(completionLatch.await(30, TimeUnit.SECONDS));
        assertEquals(1, instances.size());
    }

    @RepeatedTest(3)
    @DisplayName("Should handle concurrent provider registration and removal")
    void shouldHandleConcurrentProviderRegistrationAndRemoval() throws Exception {
        int threadCount = 24;
        int operationsPerThread = 120;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

        try {
            for (int index = 0; index < threadCount; index++) {
                final int threadId = index;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int operation = 0; operation < operationsPerThread; operation++) {
                            String providerType = "concurrency-provider-" + threadId;
                            factory.registerProvider(providerType, new TestProvider(providerType));

                            boolean supported = factory.isCustomTypeSupported(providerType);
                            boolean visibleInTypes = factory.getSupportedTypes().contains(providerType);
                            if (!supported && !visibleInTypes) {
                                failures.add(new AssertionError("Provider was not visible: " + providerType));
                            }

                            if (operation % 2 == 0) {
                                factory.unregisterProvider(providerType);
                            }
                        }
                    } catch (Throwable throwable) {
                        failures.add(throwable);
                    }
                });
            }

            startLatch.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
            assertTrue(failures.isEmpty(), () -> "Unexpected failures: " + failures);

            for (int index = 0; index < 24; index++) {
                String providerType = "concurrency-provider-" + index;
                factory.unregisterProvider(providerType);
                assertFalse(factory.isCustomTypeSupported(providerType));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Should preserve equivalent HTTP client reuse when cache is intact")
    void shouldPreserveEquivalentHttpClientReuseWhenCacheIsIntact() throws Exception {
        ExternalDataSource firstDataSource = factory.createDataSource(createRestApiConfig("rest-a", "https://example.test/api"));
        ExternalDataSource secondDataSource = factory.createDataSource(createRestApiConfig("rest-b", "https://example.test/api"));

        HttpClient firstClient = (HttpClient) readField(firstDataSource, "httpClient");
        HttpClient secondClient = (HttpClient) readField(secondDataSource, "httpClient");

        assertSame(firstClient, secondClient);
        assertEquals(1, readMapSize(factory, "httpClientCache"));
    }

    @RepeatedTest(3)
    @DisplayName("Should remain usable during concurrent REST creation and cache clear")
    void shouldRemainUsableDuringConcurrentRestCreationAndCacheClear() throws Exception {
        int creatorThreads = 8;
        int creationsPerThread = 60;
        ExecutorService executor = Executors.newFixedThreadPool(creatorThreads + 1);
        CountDownLatch startLatch = new CountDownLatch(1);
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

        try {
            for (int index = 0; index < creatorThreads; index++) {
                final int threadId = index;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int attempt = 0; attempt < creationsPerThread; attempt++) {
                            DataSourceConfiguration config = createRestApiConfig(
                                "rest-creator-" + threadId + '-' + attempt,
                                "https://example.test/api"
                            );
                            ExternalDataSource dataSource = factory.createDataSource(config);
                            assertNotNull(readField(dataSource, "httpClient"));
                            dataSource.shutdown();
                        }
                    } catch (Throwable throwable) {
                        failures.add(throwable);
                    }
                });
            }

            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int iteration = 0; iteration < creationsPerThread; iteration++) {
                        factory.clearCache();
                        Thread.yield();
                    }
                } catch (Throwable throwable) {
                    failures.add(throwable);
                }
            });

            startLatch.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
            assertTrue(failures.isEmpty(), () -> "Unexpected failures: " + failures);

            ExternalDataSource finalDataSource = factory.createDataSource(createRestApiConfig("rest-final", "https://example.test/api"));
            assertNotNull(readField(finalDataSource, "httpClient"));
            assertTrue(readMapSize(factory, "httpClientCache") <= 1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Object readField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private int readMapSize(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        Map<?, ?> map = (Map<?, ?>) field.get(target);
        return map.size();
    }

    private DataSourceConfiguration createRestApiConfig(String name, String baseUrl) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName(name);
        config.setType("rest-api");
        config.setSourceType("api");
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setBaseUrl(baseUrl);
        connectionConfig.setTimeout(2_000);
        connectionConfig.setSslEnabled(true);
        config.setConnection(connectionConfig);

        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setMaxSize(128);
        cacheConfig.setTtlSeconds(60L);
        config.setCache(cacheConfig);

        return config;
    }

    private static class TestProvider implements DataSourceProvider {
        private final String type;

        private TestProvider(String type) {
            this.type = type;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public ExternalDataSource createDataSource(DataSourceConfiguration config) {
            return new TestExternalDataSource(config.getName(), type);
        }

        @Override
        public boolean supports(DataSourceConfiguration config) {
            return type.equals(config.getImplementation());
        }
    }

    private static class TestExternalDataSource implements ExternalDataSource {
        private final String name;
        private final String type;

        private TestExternalDataSource(String name, String type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDataType() {
            return type;
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        @Override
        public boolean supportsDataType(String dataType) {
            return type.equals(dataType);
        }

        @Override
        public <T> T getData(String dataType, Object... parameters) {
            return null;
        }

        @Override
        public <T> List<T> query(String query, Map<String, Object> parameters) {
            return List.of();
        }

        @Override
        public <T> T queryForObject(String query, Map<String, Object> parameters) {
            return null;
        }

        @Override
        public <T> List<List<T>> batchQuery(List<String> queries) {
            return List.of();
        }

        @Override
        public void batchUpdate(List<String> statements) {
        }

        @Override
        public boolean testConnection() {
            return true;
        }

        @Override
        public ConnectionStatus getConnectionStatus() {
            return ConnectionStatus.connected("test");
        }

        @Override
        public void shutdown() {
        }

        @Override
        public DataSourceType getSourceType() {
            return DataSourceType.CUSTOM;
        }

        @Override
        public DataSourceMetrics getMetrics() {
            return new DataSourceMetrics();
        }

        @Override
        public void initialize(DataSourceConfiguration config) throws DataSourceException {
        }

        @Override
        public DataSourceConfiguration getConfiguration() {
            return null;
        }

        @Override
        public void refresh() throws DataSourceException {
        }
    }
}
