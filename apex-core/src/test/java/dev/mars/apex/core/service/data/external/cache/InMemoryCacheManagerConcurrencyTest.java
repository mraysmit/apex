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
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
public class InMemoryCacheManagerConcurrencyTest {

    @RepeatedTest(3)
    @DisplayName("Should maintain consistent size during concurrent put remove and get")
    void shouldMaintainConsistentSizeDuringConcurrentPutRemoveAndGet() throws Exception {
        InMemoryCacheManager cacheManager = new InMemoryCacheManager(createConfiguration(512, 300L));
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch startLatch = new CountDownLatch(1);
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

        try {
            for (int index = 0; index < 16; index++) {
                final int threadId = index;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int operation = 0; operation < 400; operation++) {
                            String key = "key-" + ((threadId * 400 + operation) % 128);
                            switch (operation % 3) {
                                case 0 -> cacheManager.put(key, "value-" + threadId + '-' + operation);
                                case 1 -> cacheManager.get(key);
                                default -> cacheManager.remove(key);
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
            assertTrue(cacheManager.size() >= 0);
            assertEquals(cacheManager.getAllKeys().size(), cacheManager.size());
        } finally {
            executor.shutdownNow();
            cacheManager.shutdown();
        }
    }

    @RepeatedTest(3)
    @DisplayName("Should respect max size under concurrent puts")
    void shouldRespectMaxSizeUnderConcurrentPuts() throws Exception {
        InMemoryCacheManager cacheManager = new InMemoryCacheManager(createConfiguration(25, 300L));
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

        try {
            for (int index = 0; index < 20; index++) {
                final int threadId = index;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int operation = 0; operation < 60; operation++) {
                            cacheManager.put("put-" + threadId + '-' + operation, operation);
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
            assertTrue(cacheManager.size() <= 25);
            assertTrue(cacheManager.getAllKeys().size() <= 25);
        } finally {
            executor.shutdownNow();
            cacheManager.shutdown();
        }
    }

    @Test
    @DisplayName("Should handle concurrent expiry and reads without negative size")
    void shouldHandleConcurrentExpiryAndReadsWithoutNegativeSize() throws Exception {
        InMemoryCacheManager cacheManager = new InMemoryCacheManager(createConfiguration(256, 1L));
        for (int index = 0; index < 120; index++) {
            cacheManager.put("expire-" + index, "value-" + index, 1L);
        }

        Thread.sleep(1150L);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

        try {
            for (int index = 0; index < 8; index++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int keyIndex = 0; keyIndex < 120; keyIndex++) {
                            String key = "expire-" + keyIndex;
                            cacheManager.get(key);
                            cacheManager.containsKey(key);
                        }
                    } catch (Throwable throwable) {
                        failures.add(throwable);
                    }
                });
            }

            for (int index = 0; index < 2; index++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int iteration = 0; iteration < 20; iteration++) {
                            cacheManager.evictExpired();
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
            assertEquals(0, cacheManager.size());
            assertTrue(cacheManager.getAllKeys().isEmpty());
        } finally {
            executor.shutdownNow();
            cacheManager.shutdown();
        }
    }

    @Test
    @DisplayName("Should clear and shutdown safely during active operations")
    void shouldClearAndShutdownSafelyDuringActiveOperations() throws Exception {
        InMemoryCacheManager cacheManager = new InMemoryCacheManager(createConfiguration(128, 300L));
        ExecutorService executor = Executors.newFixedThreadPool(9);
        CountDownLatch startLatch = new CountDownLatch(1);
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

        try {
            for (int index = 0; index < 8; index++) {
                final int threadId = index;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int operation = 0; operation < 200; operation++) {
                            String key = "mixed-" + ((threadId + operation) % 64);
                            cacheManager.put(key, operation);
                            cacheManager.get(key);
                            if (operation % 5 == 0) {
                                cacheManager.remove(key);
                            }
                        }
                    } catch (Throwable throwable) {
                        failures.add(throwable);
                    }
                });
            }

            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int iteration = 0; iteration < 40; iteration++) {
                        cacheManager.clear();
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
            assertTrue(cacheManager.size() >= 0);
            assertEquals(cacheManager.getAllKeys().size(), cacheManager.size());

            cacheManager.shutdown();
            assertFalse(cacheManager.isHealthy());
            assertEquals(0, cacheManager.size());
        } finally {
            executor.shutdownNow();
            if (cacheManager.isHealthy()) {
                cacheManager.shutdown();
            }
        }
    }

    private DataSourceConfiguration createConfiguration(int maxSize, long ttlSeconds) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("concurrency-cache");
        config.setType("cache");
        config.setSourceType("memory");
        config.setEnabled(true);

        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setMaxSize(maxSize);
        cacheConfig.setTtlSeconds(ttlSeconds);
        config.setCache(cacheConfig);

        return config;
    }
}