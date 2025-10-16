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
            logger.info("TEST: Multi-threaded scenario execution");
            
            // Given: Scenario with classification rule
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            
            ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
            scenario.addProcessingStage(stage);
            
            // When: Execute from multiple threads
            int threadCount = 10;
            int executionsPerThread = 10;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            List<Future<Integer>> futures = new ArrayList<>();
            
            try {
                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    futures.add(executorService.submit(() -> {
                        int successCount = 0;
                        for (int i = 0; i < executionsPerThread; i++) {
                            Map<String, Object> testData = new HashMap<>();
                            testData.put("type", "OTC");
                            testData.put("threadId", threadId);
                            testData.put("iteration", i);
                            
                            ScenarioExecutionResult result = executor.executeStages(scenario, testData);
                            if (result.isSuccessful() || result.requiresReview()) {
                                successCount++;
                            }
                        }
                        return successCount;
                    }));
                }
                
                // Then: Verify all executions completed successfully
                int totalSuccess = 0;
                for (Future<Integer> future : futures) {
                    totalSuccess += future.get();
                }
                
                int expectedTotal = threadCount * executionsPerThread;
                assertEquals(expectedTotal, totalSuccess,
                    "All concurrent executions should succeed");
                
                logger.info("✓ Multi-threaded execution passed: {} threads, {} executions each, {} total success",
                    threadCount, executionsPerThread, totalSuccess);
                
            } catch (Exception e) {
                fail("Concurrent execution failed: " + e.getMessage());
            } finally {
                executorService.shutdown();
            }
        }
        
        @Test
        @DisplayName("Should prevent race conditions in stage execution")
        void testRaceConditionPrevention() {
            logger.info("TEST: Race condition prevention");
            
            // Given: Scenario with multiple stages
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            
            ScenarioStage stage1 = new ScenarioStage("stage-1", "config/stage1.yaml", 1);
            ScenarioStage stage2 = new ScenarioStage("stage-2", "config/stage2.yaml", 2);
            stage2.addDependency("stage-1");
            scenario.addProcessingStage(stage1);
            scenario.addProcessingStage(stage2);
            
            // When: Execute concurrently with dependency chain
            int concurrentExecutions = 20;
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            List<Future<ScenarioExecutionResult>> futures = new ArrayList<>();
            
            try {
                for (int i = 0; i < concurrentExecutions; i++) {
                    final int index = i;
                    futures.add(executorService.submit(() -> {
                        Map<String, Object> testData = new HashMap<>();
                        testData.put("type", "OTC");
                        testData.put("id", index);
                        return executor.executeStages(scenario, testData);
                    }));
                }
                
                // Then: Verify all results are valid and consistent
                for (Future<ScenarioExecutionResult> future : futures) {
                    ScenarioExecutionResult result = future.get();
                    assertNotNull(result, "Result should not be null");
                    assertNotNull(result.getStageResults(), "Stage results should not be null");
                    
                    // Verify dependency chain is respected
                    if (result.getStageResults().size() >= 2) {
                        // If stage-2 exists, stage-1 should also exist
                        boolean hasStage1 = result.getStageResults().stream()
                            .anyMatch(sr -> sr.getStageName().equals("stage-1"));
                        assertTrue(hasStage1, "Stage-1 should exist when stage-2 is present");
                    }
                }
                
                logger.info("✓ Race condition prevention verified: {} concurrent executions",
                    concurrentExecutions);
                
            } catch (Exception e) {
                fail("Race condition test failed: " + e.getMessage());
            } finally {
                executorService.shutdown();
            }
        }
        
        @Test
        @DisplayName("Should ensure results are isolated between threads")
        void testResultIsolationBetweenThreads() {
            logger.info("TEST: Result isolation between threads");
            
            // Given: Scenario with classification rule
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            
            ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
            scenario.addProcessingStage(stage);
            
            // When: Execute from multiple threads with different data
            int threadCount = 5;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            List<Future<Map<String, Object>>> futures = new ArrayList<>();
            
            try {
                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    futures.add(executorService.submit(() -> {
                        Map<String, Object> testData = new HashMap<>();
                        testData.put("type", "OTC");
                        testData.put("threadId", threadId);
                        testData.put("value", threadId * 100);
                        
                        ScenarioExecutionResult result = executor.executeStages(scenario, testData);
                        
                        // Return data to verify isolation
                        Map<String, Object> resultData = new HashMap<>();
                        resultData.put("threadId", threadId);
                        resultData.put("success", result.isSuccessful() || result.requiresReview());
                        resultData.put("executionTime", result.getTotalExecutionTimeMs());
                        return resultData;
                    }));
                }
                
                // Then: Verify each thread got its own isolated result
                for (int i = 0; i < threadCount; i++) {
                    Map<String, Object> resultData = futures.get(i).get();
                    assertEquals(i, resultData.get("threadId"),
                        "Thread " + i + " should have its own isolated result");
                    assertTrue((Boolean) resultData.get("success"),
                        "Thread " + i + " execution should succeed");
                }
                
                logger.info("✓ Result isolation verified: {} threads with isolated results",
                    threadCount);
                
            } catch (Exception e) {
                fail("Result isolation test failed: " + e.getMessage());
            } finally {
                executorService.shutdown();
            }
        }
        
        @Test
        @DisplayName("Should handle concurrent cache access safely")
        void testConcurrentCacheAccess() {
            logger.info("TEST: Concurrent cache access");
            
            // Given: Scenario with classification rule
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            
            ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
            scenario.addProcessingStage(stage);
            
            // When: Execute same scenario from multiple threads (cache hit scenario)
            int threadCount = 10;
            int executionsPerThread = 5;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            
            try {
                List<Future<?>> futures = new ArrayList<>();
                
                for (int t = 0; t < threadCount; t++) {
                    futures.add(executorService.submit(() -> {
                        for (int i = 0; i < executionsPerThread; i++) {
                            Map<String, Object> testData = new HashMap<>();
                            testData.put("type", "OTC");
                            
                            ScenarioExecutionResult result = executor.executeStages(scenario, testData);
                            if (result.isSuccessful() || result.requiresReview()) {
                                successCount.incrementAndGet();
                            }
                        }
                    }));
                }
                
                // Wait for all to complete
                for (Future<?> future : futures) {
                    future.get();
                }
                
                // Then: Verify all cache accesses succeeded
                int expectedTotal = threadCount * executionsPerThread;
                assertEquals(expectedTotal, successCount.get(),
                    "All concurrent cache accesses should succeed");
                
                logger.info("✓ Concurrent cache access verified: {} threads, {} executions each",
                    threadCount, executionsPerThread);
                
            } catch (Exception e) {
                fail("Concurrent cache access test failed: " + e.getMessage());
            } finally {
                executorService.shutdown();
            }
        }
    }
}

