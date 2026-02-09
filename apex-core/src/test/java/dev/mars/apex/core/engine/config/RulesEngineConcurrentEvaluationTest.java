package dev.mars.apex.core.engine.config;

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

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RulesEngine thread-safety with ConcurrentHashMap for dataSources registry.
 * 
 * Validates the fix for race conditions in parallel scenario evaluation:
 * - DataSources registry is thread-safe (ConcurrentHashMap)
 * - Parallel evaluate() calls don't cause race conditions
 * - Consistent behavior under concurrent access
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-12-01
 * @version 1.0
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RulesEngineConcurrentEvaluationTest {

    private static final Logger logger = LoggerFactory.getLogger(RulesEngineConcurrentEvaluationTest.class);

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Clear caches for test isolation
        dev.mars.apex.core.service.data.external.factory.DataSourceFactory.getInstance().clearCache();
        logger.info("DataSourceFactory cache cleared for test isolation");
    }

    @AfterEach
    void tearDown() {
        dev.mars.apex.core.service.data.external.factory.DataSourceFactory.getInstance().clearCache();
    }

    // ========================================
    // Concurrent Evaluation Tests
    // ========================================

    @Test
    @Order(1)
    @DisplayName("Should handle concurrent evaluate() calls safely")
    void testConcurrentEvaluateCalls() throws Exception {
        logger.info("=".repeat(80));
        logger.info("TEST: Concurrent evaluate() calls");
        logger.info("=".repeat(80));

        // Create a simple YAML configuration
        String yamlContent = """
                metadata:
                  name: "Concurrent Test Configuration"
                  version: "1.0"

                rules:
                  - id: "age-check-rule"
                    name: "Age Check Rule"
                    condition: "#age >= 18"
                    message: "Adult user"
                    priority: 100
                  - id: "score-check-rule"
                    name: "Score Check Rule"
                    condition: "#score >= 80"
                    message: "High score"
                    priority: 90
                """;

        Path yamlFile = tempDir.resolve("concurrent-test.yaml");
        Files.writeString(yamlFile, yamlContent);

        // Create engine
        RulesEngine engine = RulesEngine.fromFile(yamlFile.toString());
        assertNotNull(engine, "Engine should be created");

        final int threadCount = 20;
        final int evaluationsPerThread = 10;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        final List<Exception> errors = new CopyOnWriteArrayList<>();
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(threadCount);

        // Create tasks that evaluate rules concurrently
        for (int t = 0; t < threadCount; t++) {
            final int threadNum = t;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for synchronized start
                    
                    for (int i = 0; i < evaluationsPerThread; i++) {
                        // Create unique input data for each evaluation
                        Map<String, Object> inputData = new HashMap<>();
                        inputData.put("age", 20 + (threadNum % 10));
                        inputData.put("score", 75 + (i % 30));
                        inputData.put("threadId", threadNum);
                        inputData.put("iteration", i);

                        RuleResult result = engine.evaluate(inputData);
                        
                        if (result != null) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    logger.error("Thread {} failed: {}", threadNum, e.getMessage(), e);
                    errors.add(e);
                    failureCount.addAndGet(evaluationsPerThread);
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        logger.info("Releasing {} threads for concurrent evaluation...", threadCount);
        startLatch.countDown();

        // Wait for completion
        assertTrue(completionLatch.await(60, TimeUnit.SECONDS), 
            "All threads should complete within timeout");

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Report results
        int totalEvaluations = threadCount * evaluationsPerThread;
        logger.info("Concurrent evaluation results:");
        logger.info("  Total threads: {}", threadCount);
        logger.info("  Evaluations per thread: {}", evaluationsPerThread);
        logger.info("  Total evaluations: {}", totalEvaluations);
        logger.info("  Successful: {}", successCount.get());
        logger.info("  Failed: {}", failureCount.get());
        logger.info("  Errors: {}", errors.size());

        // Verify results
        assertTrue(errors.isEmpty(), "No exceptions should occur during concurrent evaluation: " + 
            (errors.isEmpty() ? "" : errors.get(0).getMessage()));
        assertEquals(totalEvaluations, successCount.get(), 
            "All evaluations should succeed");
        assertEquals(0, failureCount.get(), 
            "No evaluations should fail");

        logger.info("TEST PASSED: Concurrent evaluate() calls handled safely");
    }

    @Test
    @Order(2)
    @DisplayName("Should maintain consistent results under concurrent access")
    void testConcurrentResultConsistency() throws Exception {
        logger.info("=".repeat(80));
        logger.info("TEST: Concurrent result consistency");
        logger.info("=".repeat(80));

        // Create a deterministic YAML configuration
        String yamlContent = """
                metadata:
                  name: "Consistency Test Configuration"
                  version: "1.0"

                rules:
                  - id: "deterministic-rule"
                    name: "Deterministic Rule"
                    condition: "#value == 42"
                    message: "Value is 42"
                    priority: 100
                """;

        Path yamlFile = tempDir.resolve("consistency-test.yaml");
        Files.writeString(yamlFile, yamlContent);

        RulesEngine engine = RulesEngine.fromFile(yamlFile.toString());

        final int threadCount = 10;
        final int evaluationsPerThread = 50;
        final AtomicInteger matchCount = new AtomicInteger(0);
        final AtomicInteger noMatchCount = new AtomicInteger(0);
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch completionLatch = new CountDownLatch(threadCount);

        // All threads evaluate with value=42, should all get MATCH
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < evaluationsPerThread; i++) {
                        Map<String, Object> inputData = new HashMap<>();
                        inputData.put("value", 42);

                        RuleResult result = engine.evaluate(inputData);
                        
                        if (result != null && result.isTriggered()) {
                            matchCount.incrementAndGet();
                        } else {
                            noMatchCount.incrementAndGet();
                        }
                    }
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        assertTrue(completionLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        int totalEvaluations = threadCount * evaluationsPerThread;
        logger.info("Consistency check results:");
        logger.info("  Total evaluations: {}", totalEvaluations);
        logger.info("  Matches (expected): {}", matchCount.get());
        logger.info("  No matches (unexpected): {}", noMatchCount.get());

        // All evaluations with value=42 should match
        assertEquals(totalEvaluations, matchCount.get(), 
            "All evaluations with value=42 should match");
        assertEquals(0, noMatchCount.get(), 
            "No evaluations should fail to match");

        logger.info("TEST PASSED: Concurrent results are consistent");
    }

    @Test
    @Order(3)
    @DisplayName("Should handle mixed concurrent operations safely")
    void testMixedConcurrentOperations() throws Exception {
        logger.info("=".repeat(80));
        logger.info("TEST: Mixed concurrent operations");
        logger.info("=".repeat(80));

        // Create multiple YAML configurations
        String yamlContent1 = """
                metadata:
                  name: "Config 1"
                  version: "1.0"

                rules:
                  - id: "rule-1"
                    name: "Rule 1"
                    condition: "#type == 'A'"
                    message: "Type A"
                    priority: 100
                """;

        String yamlContent2 = """
                metadata:
                  name: "Config 2"
                  version: "1.0"

                rules:
                  - id: "rule-2"
                    name: "Rule 2"
                    condition: "#type == 'B'"
                    message: "Type B"
                    priority: 100
                """;

        Path yamlFile1 = tempDir.resolve("config1.yaml");
        Path yamlFile2 = tempDir.resolve("config2.yaml");
        Files.writeString(yamlFile1, yamlContent1);
        Files.writeString(yamlFile2, yamlContent2);

        // Create two separate engines
        RulesEngine engine1 = RulesEngine.fromFile(yamlFile1.toString());
        RulesEngine engine2 = RulesEngine.fromFile(yamlFile2.toString());

        final int threadCount = 8;
        final int evaluationsPerThread = 20;
        final AtomicInteger engine1Success = new AtomicInteger(0);
        final AtomicInteger engine2Success = new AtomicInteger(0);
        final List<Exception> errors = new CopyOnWriteArrayList<>();
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch completionLatch = new CountDownLatch(threadCount);

        // Half threads use engine1, half use engine2
        for (int t = 0; t < threadCount; t++) {
            final int threadNum = t;
            final RulesEngine engine = (t % 2 == 0) ? engine1 : engine2;
            final String type = (t % 2 == 0) ? "A" : "B";
            final AtomicInteger counter = (t % 2 == 0) ? engine1Success : engine2Success;

            executor.submit(() -> {
                try {
                    for (int i = 0; i < evaluationsPerThread; i++) {
                        Map<String, Object> inputData = new HashMap<>();
                        inputData.put("type", type);

                        RuleResult result = engine.evaluate(inputData);
                        
                        if (result != null && result.isTriggered()) {
                            counter.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    logger.error("Thread {} failed: {}", threadNum, e.getMessage());
                    errors.add(e);
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        assertTrue(completionLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        int expectedPerEngine = (threadCount / 2) * evaluationsPerThread;
        logger.info("Mixed operation results:");
        logger.info("  Engine 1 successes: {} (expected {})", engine1Success.get(), expectedPerEngine);
        logger.info("  Engine 2 successes: {} (expected {})", engine2Success.get(), expectedPerEngine);
        logger.info("  Errors: {}", errors.size());

        assertTrue(errors.isEmpty(), "No errors should occur");
        assertEquals(expectedPerEngine, engine1Success.get(), "All engine1 evaluations should succeed");
        assertEquals(expectedPerEngine, engine2Success.get(), "All engine2 evaluations should succeed");

        logger.info("TEST PASSED: Mixed concurrent operations handled safely");
    }

    @Test
    @Order(4)
    @DisplayName("Should verify dataSources map is ConcurrentHashMap")
    void testDataSourcesMapIsConcurrent() throws Exception {
        logger.info("=".repeat(80));
        logger.info("TEST: DataSources map is ConcurrentHashMap");
        logger.info("=".repeat(80));

        // Create a simple engine
        String yamlContent = """
                metadata:
                  name: "DataSources Test"
                  version: "1.0"

                rules:
                  - id: "test-rule"
                    name: "Test Rule"
                    condition: "true"
                    message: "Always matches"
                    priority: 100
                """;

        Path yamlFile = tempDir.resolve("datasources-test.yaml");
        Files.writeString(yamlFile, yamlContent);

        RulesEngine engine = RulesEngine.fromFile(yamlFile.toString());

        // Use reflection to verify the dataSources field is ConcurrentHashMap
        try {
            java.lang.reflect.Field dataSourcesField = RulesEngine.class.getDeclaredField("dataSources");
            dataSourcesField.setAccessible(true);
            Object dataSources = dataSourcesField.get(engine);

            assertNotNull(dataSources, "dataSources field should not be null");
            
            // Verify it's a ConcurrentHashMap
            assertTrue(dataSources instanceof ConcurrentHashMap, 
                "dataSources should be ConcurrentHashMap but was " + dataSources.getClass().getName());

            logger.info("DataSources map type: {}", dataSources.getClass().getName());
            logger.info("TEST PASSED: dataSources is ConcurrentHashMap");

        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Could not access dataSources field: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should handle rapid sequential evaluations without issues")
    void testRapidSequentialEvaluations() throws Exception {
        logger.info("=".repeat(80));
        logger.info("TEST: Rapid sequential evaluations");
        logger.info("=".repeat(80));

        String yamlContent = """
                metadata:
                  name: "Rapid Test Configuration"
                  version: "1.0"

                rules:
                  - id: "counter-rule"
                    name: "Counter Rule"
                    condition: "#counter > 0"
                    message: "Counter is positive"
                    priority: 100
                """;

        Path yamlFile = tempDir.resolve("rapid-test.yaml");
        Files.writeString(yamlFile, yamlContent);

        RulesEngine engine = RulesEngine.fromFile(yamlFile.toString());

        final int iterations = 1000;
        int successCount = 0;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("counter", i + 1);

            RuleResult result = engine.evaluate(inputData);
            if (result != null && result.isTriggered()) {
                successCount++;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        double avgTime = (double) elapsed / iterations;

        logger.info("Rapid evaluation results:");
        logger.info("  Total iterations: {}", iterations);
        logger.info("  Successful: {}", successCount);
        logger.info("  Total time: {} ms", elapsed);
        logger.info("  Average time per evaluation: {:.2f} ms", avgTime);

        assertEquals(iterations, successCount, "All evaluations should succeed");
        
        logger.info("TEST PASSED: Rapid sequential evaluations completed successfully");
    }
}
