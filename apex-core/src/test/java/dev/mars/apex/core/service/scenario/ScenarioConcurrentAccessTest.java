package dev.mars.apex.core.service.scenario;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Priority 3: Concurrent Access Testing
 * 
 * Validates that scenario execution is thread-safe and handles concurrent access
 * without race conditions or data corruption.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Priority 3: Concurrent Access Testing")
class ScenarioConcurrentAccessTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ScenarioConcurrentAccessTest.class);
    
    private ScenarioConfiguration scenario;
    private ScenarioStageExecutor executor;
    
    @BeforeEach
    void setUp() {
        logger.info("TEST: Setting up concurrent access test");
        scenario = new ScenarioConfiguration();
        scenario.setScenarioId("concurrent-test-scenario");
        executor = new ScenarioStageExecutor();
    }
    
    // ========================================
    // Concurrent Access Tests
    // ========================================
    
    @Nested
    @DisplayName("Concurrent Access Tests")
    class ConcurrentAccessTests {

        @Test
        @DisplayName("Should handle multiple threads executing scenarios concurrently")
        void testMultiThreadedScenarioExecution() {
            logger.info("========== TEST START: Multi-threaded scenario execution ==========");

            try {
                // Given: Scenario with classification rule
                logger.info("Setting up scenario with classification rule");
                scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");

                String configPath = new java.io.File("src/test/resources/config/test.yaml").getAbsolutePath();
                logger.info("Config path: {}", configPath);
                ScenarioStage stage = new ScenarioStage("test-stage", configPath, 1);
                scenario.addProcessingStage(stage);
                logger.info("Scenario setup complete");

                // When: Execute from multiple threads
                int threadCount = 10;
                int executionsPerThread = 10;
                logger.info("Starting concurrent execution: {} threads, {} executions per thread", threadCount, executionsPerThread);
                ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
                List<Future<Integer>> futures = new ArrayList<>();

                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    futures.add(executorService.submit(() -> {
                        int successCount = 0;
                        for (int i = 0; i < executionsPerThread; i++) {
                            Map<String, Object> testData = new HashMap<>();
                            testData.put("type", "OTC");
                            testData.put("threadId", threadId);
                            testData.put("iteration", i);

                            try {
                                ScenarioExecutionResult result = executor.executeStages(scenario, testData);
                                if (result.isSuccessful() || result.requiresReview()) {
                                    successCount++;
                                } else {
                                    logger.warn("TEST: Execution failed - Thread: {}, Iteration: {}, Status: {}, Warnings: {}",
                                        threadId, i, result.getExecutionStatus(), result.getWarnings());
                                }
                            } catch (Exception e) {
                                logger.error("TEST: Exception during execution - Thread: {}, Iteration: {}", threadId, i, e);
                            }
                        }
                        logger.debug("Thread {} completed with {} successes", threadId, successCount);
                        return successCount;
                    }));
                }

                // Then: Verify all executions completed successfully
                logger.info("Waiting for all threads to complete...");
                int totalSuccess = 0;
                for (Future<Integer> future : futures) {
                    totalSuccess += future.get();
                }

                int expectedTotal = threadCount * executionsPerThread;
                logger.info("All threads completed. Total success: {}/{}", totalSuccess, expectedTotal);
                assertEquals(expectedTotal, totalSuccess,
                    "All concurrent executions should succeed");

                logger.info("✓ Multi-threaded execution passed: {} threads, {} executions each, {} total success",
                    threadCount, executionsPerThread, totalSuccess);
                logger.info("========== TEST PASSED: Multi-threaded scenario execution ==========");

            } catch (Exception e) {
                logger.error("========== TEST FAILED: Multi-threaded scenario execution ==========", e);
                fail("Concurrent execution failed: " + e.getMessage());
            }
        }
        
        @Test
        @DisplayName("Should prevent race conditions in stage execution")
        void testRaceConditionPrevention() {
            logger.info("========== TEST START: Race condition prevention ==========");

            ExecutorService executorService = null;
            try {
                // Given: Scenario with multiple stages
                logger.info("Setting up scenario with multiple stages and dependencies");
                scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");

                String stage1Path = new java.io.File("src/test/resources/config/stage1.yaml").getAbsolutePath();
                String stage2Path = new java.io.File("src/test/resources/config/stage2.yaml").getAbsolutePath();
                logger.info("Stage 1 path: {}", stage1Path);
                logger.info("Stage 2 path: {}", stage2Path);
                ScenarioStage stage1 = new ScenarioStage("stage-1", stage1Path, 1);
                ScenarioStage stage2 = new ScenarioStage("stage-2", stage2Path, 2);
                stage2.addDependency("stage-1");
                scenario.addProcessingStage(stage1);
                scenario.addProcessingStage(stage2);
                logger.info("Scenario setup complete with 2 stages and dependency chain");

                // When: Execute concurrently with dependency chain
                int concurrentExecutions = 20;
                logger.info("Starting concurrent execution with dependency chain: {} executions", concurrentExecutions);
                executorService = Executors.newFixedThreadPool(5);
                List<Future<ScenarioExecutionResult>> futures = new ArrayList<>();

                for (int i = 0; i < concurrentExecutions; i++) {
                    final int index = i;
                    futures.add(executorService.submit(() -> {
                        Map<String, Object> testData = new HashMap<>();
                        testData.put("type", "OTC");
                        testData.put("id", index);
                        logger.debug("Executing scenario for id: {}", index);
                        return executor.executeStages(scenario, testData);
                    }));
                }

                // Then: Verify all results are valid and consistent
                logger.info("Verifying all execution results...");
                int validResults = 0;
                for (Future<ScenarioExecutionResult> future : futures) {
                    ScenarioExecutionResult result = future.get();
                    assertNotNull(result, "Result should not be null");
                    assertNotNull(result.getStageResults(), "Stage results should not be null");
                    validResults++;

                    // Verify dependency chain is respected
                    if (result.getStageResults().size() >= 2) {
                        // If stage-2 exists, stage-1 should also exist
                        boolean hasStage1 = result.getStageResults().stream()
                            .anyMatch(sr -> sr.getStageName().equals("stage-1"));
                        assertTrue(hasStage1, "Stage-1 should exist when stage-2 is present");
                    }
                }

                logger.info("✓ Race condition prevention verified: {} concurrent executions, {} valid results",
                    concurrentExecutions, validResults);
                logger.info("========== TEST PASSED: Race condition prevention ==========");

            } catch (Exception e) {
                logger.error("========== TEST FAILED: Race condition prevention ==========", e);
                fail("Race condition test failed: " + e.getMessage());
            } finally {
                if (executorService != null) {
                    executorService.shutdown();
                }
            }
        }
        
        @Test
        @DisplayName("Should ensure results are isolated between threads")
        void testResultIsolationBetweenThreads() {
            logger.info("========== TEST START: Result isolation between threads ==========");

            try {
                // Given: Scenario with classification rule
                logger.info("Setting up scenario for result isolation test");
                scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");

                String configPath = new java.io.File("src/test/resources/config/test.yaml").getAbsolutePath();
                logger.info("Config path: {}", configPath);
                ScenarioStage stage = new ScenarioStage("test-stage", configPath, 1);
                scenario.addProcessingStage(stage);
                logger.info("Scenario setup complete");

                // When: Execute from multiple threads with different data
                int threadCount = 5;
                logger.info("Starting concurrent execution with {} threads for result isolation", threadCount);
                ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
                List<Future<Map<String, Object>>> futures = new ArrayList<>();

                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    futures.add(executorService.submit(() -> {
                        Map<String, Object> testData = new HashMap<>();
                        testData.put("type", "OTC");
                        testData.put("threadId", threadId);
                        testData.put("value", threadId * 100);
                        logger.debug("Thread {} executing with data: {}", threadId, testData);

                        try {
                            ScenarioExecutionResult result = executor.executeStages(scenario, testData);

                            // Return data to verify isolation
                            Map<String, Object> resultData = new HashMap<>();
                            resultData.put("threadId", threadId);
                            boolean success = result.isSuccessful() || result.requiresReview();
                            resultData.put("success", success);
                            resultData.put("executionTime", result.getTotalExecutionTimeMs());
                            logger.debug("Thread {} completed with success={}, executionTime={}ms",
                                threadId, success, result.getTotalExecutionTimeMs());

                            if (!success) {
                                logger.warn("TEST: Thread {} execution failed - Status: {}, Warnings: {}",
                                    threadId, result.getExecutionStatus(), result.getWarnings());
                            }
                            return resultData;
                        } catch (Exception e) {
                            logger.error("TEST: Thread {} exception", threadId, e);
                            Map<String, Object> errorData = new HashMap<>();
                            errorData.put("threadId", threadId);
                            errorData.put("success", false);
                            errorData.put("error", e.getMessage());
                            return errorData;
                        }
                    }));
                }

                // Then: Verify each thread got its own isolated result
                logger.info("Verifying result isolation for {} threads", threadCount);
                int successCount = 0;
                for (int i = 0; i < threadCount; i++) {
                    Map<String, Object> resultData = futures.get(i).get();
                    assertEquals(i, resultData.get("threadId"),
                        "Thread " + i + " should have its own isolated result");
                    assertTrue((Boolean) resultData.get("success"),
                        "Thread " + i + " execution should succeed");
                    successCount++;
                    logger.debug("Thread {} result verified: isolated and successful", i);
                }

                logger.info("✓ Result isolation verified: {} threads with isolated results, {} successful",
                    threadCount, successCount);
                logger.info("========== TEST PASSED: Result isolation between threads ==========");

            } catch (Exception e) {
                logger.error("========== TEST FAILED: Result isolation between threads ==========", e);
                fail("Result isolation test failed: " + e.getMessage());
            }
        }
        
        @Test
        @DisplayName("Should handle concurrent cache access safely")
        void testConcurrentCacheAccess() {
            logger.info("========== TEST START: Concurrent cache access ==========");

            ExecutorService executorService = null;
            try {
                // Given: Scenario with classification rule
                logger.info("Setting up scenario for concurrent cache access test");
                scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");

                String configPath = new java.io.File("src/test/resources/config/test.yaml").getAbsolutePath();
                logger.info("Config path: {}", configPath);
                ScenarioStage stage = new ScenarioStage("test-stage", configPath, 1);
                scenario.addProcessingStage(stage);
                logger.info("Scenario setup complete");

                // When: Execute same scenario from multiple threads (cache hit scenario)
                int threadCount = 10;
                int executionsPerThread = 5;
                logger.info("Starting concurrent cache access test: {} threads, {} executions per thread",
                    threadCount, executionsPerThread);
                executorService = Executors.newFixedThreadPool(threadCount);
                AtomicInteger successCount = new AtomicInteger(0);

                List<Future<?>> futures = new ArrayList<>();

                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    futures.add(executorService.submit(() -> {
                        logger.debug("Thread {} starting cache access test", threadId);
                        for (int i = 0; i < executionsPerThread; i++) {
                            Map<String, Object> testData = new HashMap<>();
                            testData.put("type", "OTC");

                            try {
                                ScenarioExecutionResult result = executor.executeStages(scenario, testData);
                                if (result.isSuccessful() || result.requiresReview()) {
                                    successCount.incrementAndGet();
                                    logger.debug("Thread {} iteration {} succeeded", threadId, i);
                                } else {
                                    logger.warn("TEST: Thread {} iteration {} failed - Status: {}",
                                        threadId, i, result.getExecutionStatus());
                                }
                            } catch (Exception e) {
                                logger.error("TEST: Thread {} iteration {} exception", threadId, i, e);
                            }
                        }
                        logger.debug("Thread {} completed cache access test", threadId);
                    }));
                }

                // Wait for all to complete
                logger.info("Waiting for all threads to complete cache access test...");
                for (Future<?> future : futures) {
                    future.get();
                }

                // Then: Verify all cache accesses succeeded
                int expectedTotal = threadCount * executionsPerThread;
                logger.info("All threads completed. Total success: {}/{}", successCount.get(), expectedTotal);
                assertEquals(expectedTotal, successCount.get(),
                    "All concurrent cache accesses should succeed");

                logger.info("✓ Concurrent cache access verified: {} threads, {} executions each, {} total success",
                    threadCount, executionsPerThread, successCount.get());
                logger.info("========== TEST PASSED: Concurrent cache access ==========");

            } catch (Exception e) {
                logger.error("========== TEST FAILED: Concurrent cache access ==========", e);
                fail("Concurrent cache access test failed: " + e.getMessage());
            } finally {
                if (executorService != null) {
                    executorService.shutdown();
                }
            }
        }
    }
}

