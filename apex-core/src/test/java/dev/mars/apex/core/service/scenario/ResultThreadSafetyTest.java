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

import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive thread-safety tests for result classes.
 * 
 * Tests concurrent access to ScenarioExecutionResult and StageExecutionResult
 * to ensure thread-safety under high load and stress conditions.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Result Thread-Safety Tests")
class ResultThreadSafetyTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ResultThreadSafetyTest.class);
    
    @Test
    @DisplayName("ScenarioExecutionResult should handle concurrent addStageResult calls")
    void testScenarioExecutionResult_ConcurrentAddStageResult() throws Exception {
        logger.info("TEST: Concurrent addStageResult");
        
        ScenarioExecutionResult result = new ScenarioExecutionResult("test-scenario");
        
        int threadCount = 20;
        int stagesPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        try {
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready
                        
                        for (int i = 0; i < stagesPerThread; i++) {
                            String stageName = "stage-" + threadId + "-" + i;
                            StageExecutionResult stageResult = StageExecutionResult.success(
                                stageName,
                                RuleResult.match("test-rule", "Test message")
                            );
                            stageResult.setExecutionTimeMs(10);
                            
                            result.addStageResult(stageResult);
                        }
                    } catch (Exception e) {
                        logger.error("Thread {} failed: {}", threadId, e.getMessage(), e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown(); // Start all threads simultaneously
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
            
            // Verify all stages were added
            int expectedStages = threadCount * stagesPerThread;
            assertEquals(expectedStages, result.getStageResults().size(),
                "All stage results should be added without loss");
            
            // Verify total execution time was accumulated correctly
            long expectedTime = expectedStages * 10L;
            assertEquals(expectedTime, result.getTotalExecutionTimeMs(),
                "Total execution time should be accumulated correctly");
            
            logger.info("[OK] Successfully handled {} concurrent addStageResult calls", expectedStages);
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
    
    @Test
    @DisplayName("ScenarioExecutionResult should handle concurrent addWarning calls")
    void testScenarioExecutionResult_ConcurrentAddWarning() throws Exception {
        logger.info("TEST: Concurrent addWarning");
        
        ScenarioExecutionResult result = new ScenarioExecutionResult("test-scenario");
        
        int threadCount = 20;
        int warningsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        try {
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        for (int i = 0; i < warningsPerThread; i++) {
                            result.addWarning("Warning from thread " + threadId + " #" + i);
                        }
                    } catch (Exception e) {
                        logger.error("Thread {} failed: {}", threadId, e.getMessage(), e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
            
            int expectedWarnings = threadCount * warningsPerThread;
            assertEquals(expectedWarnings, result.getWarnings().size(),
                "All warnings should be added without loss");
            
            logger.info("[OK] Successfully handled {} concurrent addWarning calls", expectedWarnings);
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
    
    @Test
    @DisplayName("ScenarioExecutionResult should handle concurrent mixed operations")
    void testScenarioExecutionResult_ConcurrentMixedOperations() throws Exception {
        logger.info("TEST: Concurrent mixed operations");
        
        ScenarioExecutionResult result = new ScenarioExecutionResult("test-scenario");
        
        int threadCount = 20;
        int operationsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger stageCount = new AtomicInteger(0);
        AtomicInteger warningCount = new AtomicInteger(0);
        AtomicInteger reviewFlagCount = new AtomicInteger(0);
        
        try {
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        for (int i = 0; i < operationsPerThread; i++) {
                            int operation = i % 3;
                            
                            switch (operation) {
                                case 0:
                                    // Add stage result
                                    StageExecutionResult stageResult = StageExecutionResult.success(
                                        "stage-" + threadId + "-" + i,
                                        RuleResult.match("test-rule", "Test")
                                    );
                                    stageResult.setExecutionTimeMs(5);
                                    result.addStageResult(stageResult);
                                    stageCount.incrementAndGet();
                                    break;
                                    
                                case 1:
                                    // Add warning
                                    result.addWarning("Warning " + threadId + "-" + i);
                                    warningCount.incrementAndGet();
                                    break;
                                    
                                case 2:
                                    // Add review flag
                                    result.addReviewFlag("Review " + threadId + "-" + i);
                                    reviewFlagCount.incrementAndGet();
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Thread {} failed: {}", threadId, e.getMessage(), e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
            
            // Verify all operations were recorded
            assertEquals(stageCount.get(), result.getStageResults().size(),
                "All stage results should be recorded");
            assertEquals(warningCount.get(), result.getWarnings().size(),
                "All warnings should be recorded");
            assertEquals(reviewFlagCount.get(), result.getReviewFlags().size(),
                "All review flags should be recorded");
            
            logger.info("[OK] Successfully handled {} mixed concurrent operations", 
                threadCount * operationsPerThread);
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
    
    @Test
    @DisplayName("StageExecutionResult should handle concurrent addStageOutput calls")
    void testStageExecutionResult_ConcurrentAddStageOutput() throws Exception {
        logger.info("TEST: Concurrent addStageOutput");
        
        StageExecutionResult result = StageExecutionResult.success(
            "test-stage",
            RuleResult.match("test-rule", "Test")
        );
        
        int threadCount = 20;
        int outputsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        try {
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        for (int i = 0; i < outputsPerThread; i++) {
                            result.addStageOutput("key-" + threadId + "-" + i, "value-" + i);
                        }
                    } catch (Exception e) {
                        logger.error("Thread {} failed: {}", threadId, e.getMessage(), e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
            
            int expectedOutputs = threadCount * outputsPerThread;
            assertEquals(expectedOutputs, result.getStageOutputs().size(),
                "All stage outputs should be added without loss");
            
            logger.info("[OK] Successfully handled {} concurrent addStageOutput calls", expectedOutputs);
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
    
    @RepeatedTest(5)
    @DisplayName("Stress test: High-load concurrent scenario execution")
    void stressTest_HighLoadConcurrentExecution() throws Exception {
        logger.info("STRESS TEST: High-load concurrent execution");
        
        ScenarioExecutionResult result = new ScenarioExecutionResult("stress-test-scenario");
        
        int threadCount = 50;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();
        
        try {
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < operationsPerThread; i++) {
                        StageExecutionResult stageResult = StageExecutionResult.success(
                            "stage-" + threadId + "-" + i,
                            RuleResult.match("rule-" + i, "Message " + i)
                        );
                        stageResult.setExecutionTimeMs(1);
                        result.addStageResult(stageResult);
                    }
                }));
            }
            
            // Wait for all to complete
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
            
            int expectedStages = threadCount * operationsPerThread;
            assertEquals(expectedStages, result.getStageResults().size(),
                "All stage results should be recorded under high load");
            
            long expectedTime = expectedStages * 1L;
            assertEquals(expectedTime, result.getTotalExecutionTimeMs(),
                "Total execution time should be correct under high load");
            
            logger.info("[OK] Stress test passed: {} operations completed successfully", expectedStages);
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}

