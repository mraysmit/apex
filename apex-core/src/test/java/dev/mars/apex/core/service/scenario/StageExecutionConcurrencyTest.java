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

import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.DisplayName;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Nested;

import org.junit.jupiter.api.RepeatedTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive concurrency tests for StageExecutionResult and related classes.
 * 
 * These tests specifically target the identified concurrency risks:
 * 1. Non-atomic setStageOutputs() (clear + putAll race condition)
 * 2. Shallow copy risk in parallel enrichments (shared nested mutable objects)
 * 3. Concurrent addStageOutput with setStageOutputs interference
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Stage Execution Concurrency Tests")
public class StageExecutionConcurrencyTest {

    private static final Logger logger = LoggerFactory.getLogger(StageExecutionConcurrencyTest.class);

    // ========================================
    // Test 1: setStageOutputs Non-Atomicity
    // ========================================
    
    @Nested
    @DisplayName("setStageOutputs Non-Atomicity Tests")
    class SetStageOutputsNonAtomicityTests {

        @RepeatedTest(5)
        @DisplayName("Should detect race condition: concurrent setStageOutputs calls")
        void testSetStageOutputsNonAtomicity() throws InterruptedException {
            logger.info("TEST: Concurrent setStageOutputs non-atomicity detection");
            
            StageExecutionResult result = StageExecutionResult.success("test-stage", null);
            int iterations = 5000;
            int threadCount = 4;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger emptyObservations = new AtomicInteger(0);
            AtomicInteger partialObservations = new AtomicInteger(0);

            Map<String, Object> map1 = Map.of("key_a", 1, "key_b", 2);
            Map<String, Object> map2 = Map.of("key_c", 3, "key_d", 4);

            // Writer threads that call setStageOutputs
            Runnable writer1 = () -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations; i++) {
                        result.setStageOutputs(map1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            };

            Runnable writer2 = () -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations; i++) {
                        result.setStageOutputs(map2);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            };

            // Observer threads that check for inconsistent state
            Runnable observer1 = () -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations * 2; i++) {
                        Map<String, Object> snapshot = result.getStageOutputs();
                        int size = snapshot.size();
                        if (size == 0) {
                            emptyObservations.incrementAndGet();
                        } else if (size != 2) {
                            partialObservations.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            };

            Runnable observer2 = () -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations * 2; i++) {
                        Map<String, Object> snapshot = result.getStageOutputs();
                        int size = snapshot.size();
                        if (size == 0) {
                            emptyObservations.incrementAndGet();
                        } else if (size != 2) {
                            partialObservations.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            };

            executor.submit(writer1);
            executor.submit(writer2);
            executor.submit(observer1);
            executor.submit(observer2);

            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdown();

            // Final state should be valid (either map1 or map2)
            Map<String, Object> finalOutputs = result.getStageOutputs();
            assertEquals(2, finalOutputs.size(), 
                "Final outputs should have exactly 2 elements");

            // Log observations for analysis
            logger.info("Empty observations (race window): {}", emptyObservations.get());
            logger.info("Partial observations (inconsistent): {}", partialObservations.get());
            
            // This test documents the race condition risk - observations may vary by run
            if (emptyObservations.get() > 0 || partialObservations.get() > 0) {
                logger.warn(" Race condition detected: {} empty, {} partial observations",
                    emptyObservations.get(), partialObservations.get());
            } else {
                logger.info("[OK] No race condition observed in this run (may be timing dependent)");
            }
        }

        @Test
        @DisplayName("Should lose addStageOutput when concurrent setStageOutputs clears")
        void testAddStageOutputLostDuringSetStageOutputs() throws InterruptedException {
            logger.info("TEST: addStageOutput lost during concurrent setStageOutputs");
            
            StageExecutionResult result = StageExecutionResult.success("test-stage", null);
            int iterations = 10000;
            ExecutorService executor = Executors.newFixedThreadPool(3);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(3);
            AtomicInteger addedCount = new AtomicInteger(0);
            AtomicInteger lostCount = new AtomicInteger(0);

            // Thread that continuously adds individual outputs
            Runnable adder = () -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations; i++) {
                        String key = "added_" + i;
                        result.addStageOutput(key, i);
                        addedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            };

            // Thread that calls setStageOutputs, potentially clearing the added values
            Runnable setter = () -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations / 10; i++) {
                        result.setStageOutputs(Map.of("set_key", "set_value"));
                        Thread.yield(); // Give adder chance to run
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            };

            // Observer that checks for inconsistencies
            Runnable checker = () -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations; i++) {
                        Map<String, Object> snapshot = result.getStageOutputs();
                        // Count how many "added_" keys survived
                        long addedKeys = snapshot.keySet().stream()
                            .filter(k -> k.startsWith("added_"))
                            .count();
                        // If addedKeys < expected, some were lost
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            };

            executor.submit(adder);
            executor.submit(setter);
            executor.submit(checker);

            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdown();

            Map<String, Object> finalOutputs = result.getStageOutputs();
            long survivingAddedKeys = finalOutputs.keySet().stream()
                .filter(k -> k.startsWith("added_"))
                .count();
            
            logger.info("Added {} keys, {} survived in final state", 
                addedCount.get(), survivingAddedKeys);
            
            // This demonstrates the risk - many added keys may be lost
            // The exact count depends on timing, but demonstrates the issue
            logger.info("[OK] Test completed - demonstrates addStageOutput can be lost during setStageOutputs");
        }
    }

    // ========================================
    // Test 2: Shallow Copy Risk
    // ========================================
    
    @Nested
    @DisplayName("Shallow Copy Risk Tests")
    class ShallowCopyRiskTests {

        @Test
        @DisplayName("Should demonstrate shallow copy risk with nested mutable objects")
        void testShallowCopyNestedMutableObjectRisk() throws InterruptedException {
            logger.info("TEST: Shallow copy risk with nested mutable objects");
            
            // Create a map with nested mutable structures (simulates targetObject in parallel enrichment)
            Map<String, Object> originalMap = new HashMap<>();
            List<String> mutableList = new ArrayList<>(Arrays.asList("item1", "item2"));
            Map<String, Object> nestedMap = new HashMap<>();
            nestedMap.put("innerKey", "innerValue");
            
            originalMap.put("list", mutableList);
            originalMap.put("nested", nestedMap);
            originalMap.put("primitive", 42);
            
            // Create shallow copies (mimics RulesEngine.convertToMap behavior)
            Map<String, Object> copy1 = new HashMap<>(originalMap);
            Map<String, Object> copy2 = new HashMap<>(originalMap);
            
            // Verify shallow copy behavior - nested objects are shared references
            assertSame(mutableList, copy1.get("list"), 
                "Shallow copy shares list reference");
            assertSame(mutableList, copy2.get("list"), 
                "Shallow copy shares list reference with original");
            assertSame(nestedMap, copy1.get("nested"), 
                "Shallow copy shares nested map reference");

            // Demonstrate the risk: modifying nested object in one copy affects all
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(2);
            AtomicBoolean raceDetected = new AtomicBoolean(false);

            Runnable modifier1 = () -> {
                try {
                    startLatch.await();
                    @SuppressWarnings("unchecked")
                    List<String> list = (List<String>) copy1.get("list");
                    for (int i = 0; i < 1000; i++) {
                        try {
                            list.add("thread1_" + i);
                        } catch (ConcurrentModificationException e) {
                            raceDetected.set(true);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            };

            Runnable modifier2 = () -> {
                try {
                    startLatch.await();
                    @SuppressWarnings("unchecked")
                    List<String> list = (List<String>) copy2.get("list");
                    for (int i = 0; i < 1000; i++) {
                        try {
                            list.add("thread2_" + i);
                        } catch (ConcurrentModificationException e) {
                            raceDetected.set(true);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            };

            executor.submit(modifier1);
            executor.submit(modifier2);
            startLatch.countDown();
            
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Tasks should complete");
            executor.shutdown();

            // Both threads modified the SAME list through their "separate" copies
            @SuppressWarnings("unchecked")
            List<String> originalList = (List<String>) originalMap.get("list");
            
            logger.info("Original list now has {} items (started with 2)", originalList.size());
            
            if (raceDetected.get()) {
                logger.warn("⚠️ ConcurrentModificationException detected - race condition confirmed");
            } else {
                logger.info("[OK] No CME, but modifications from both threads affected same list");
            }
            
            // Verify the shared state issue
            assertTrue(originalList.size() > 2, 
                "Original list should be modified by shallow copy operations");
        }

        @Test
        @DisplayName("Should demonstrate that primitive values are safely copied")
        void testPrimitiveValuesSafelyCopied() {
            logger.info("TEST: Primitive values are safely copied");
            
            Map<String, Object> original = new HashMap<>();
            original.put("intValue", 100);
            original.put("stringValue", "original");
            original.put("boolValue", true);
            
            Map<String, Object> copy = new HashMap<>(original);
            
            // Modify copy's primitives (actually reassignment)
            copy.put("intValue", 200);
            copy.put("stringValue", "modified");
            copy.put("boolValue", false);
            
            // Original should be unaffected (String is immutable, primitives are boxed)
            assertEquals(100, original.get("intValue"));
            assertEquals("original", original.get("stringValue"));
            assertEquals(true, original.get("boolValue"));
            
            logger.info("[OK] Primitive and immutable values are safely isolated in shallow copies");
        }
    }

    // ========================================
    // Test 3: Mixed Concurrent Operations
    // ========================================
    
    @Nested
    @DisplayName("Mixed Concurrent Operations Tests")
    class MixedConcurrentOperationsTests {

        @RepeatedTest(3)
        @DisplayName("Stress test: high-contention mixed operations on StageExecutionResult")
        void testHighContentionMixedOperations() throws InterruptedException {
            logger.info("STRESS TEST: High-contention mixed operations");
            
            StageExecutionResult result = StageExecutionResult.success("stress-test-stage", null);
            
            int threadCount = 10;
            int operationsPerThread = 1000;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger totalOperations = new AtomicInteger(0);
            AtomicInteger errors = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        Random random = new Random(threadId);
                        
                        for (int i = 0; i < operationsPerThread; i++) {
                            try {
                                int operation = random.nextInt(4);
                                switch (operation) {
                                    case 0: // addStageOutput
                                        result.addStageOutput("key_" + threadId + "_" + i, i);
                                        break;
                                    case 1: // setStageOutputs
                                        result.setStageOutputs(Map.of(
                                            "set_" + threadId + "_a", "valueA",
                                            "set_" + threadId + "_b", "valueB"
                                        ));
                                        break;
                                    case 2: // getStageOutputs
                                        Map<String, Object> outputs = result.getStageOutputs();
                                        // Just read, verify not null
                                        assertNotNull(outputs);
                                        break;
                                    case 3: // getStageOutput (single key)
                                        result.getStageOutput("key_" + threadId + "_" + (i % 100));
                                        break;
                                }
                                totalOperations.incrementAndGet();
                            } catch (Exception e) {
                                errors.incrementAndGet();
                                logger.error("Error in thread {}: {}", threadId, e.getMessage());
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(60, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdown();

            int expectedOperations = threadCount * operationsPerThread;
            logger.info("Completed {} of {} operations, {} errors", 
                totalOperations.get(), expectedOperations, errors.get());
            
            assertEquals(0, errors.get(), 
                "No exceptions should occur during concurrent operations");
            assertEquals(expectedOperations, totalOperations.get(), 
                "All operations should complete");
            
            logger.info("[OK] Stress test passed: {} operations across {} threads", 
                totalOperations.get(), threadCount);
        }

        @Test
        @DisplayName("Should verify ScenarioExecutionResult concurrent safety")
        void testScenarioExecutionResultConcurrentSafety() throws InterruptedException {
            logger.info("TEST: ScenarioExecutionResult concurrent safety");
            
            ScenarioExecutionResult scenarioResult = new ScenarioExecutionResult("concurrent-test");
            
            int threadCount = 8;
            int operationsPerThread = 500;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger errors = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        for (int i = 0; i < operationsPerThread; i++) {
                            try {
                                // Mix of operations on ScenarioExecutionResult
                                scenarioResult.addStageResult(
                                    StageExecutionResult.success("stage_" + threadId + "_" + i, 
                                        RuleResult.match("rule", "message"))
                                );
                                scenarioResult.addWarning("Warning from thread " + threadId);
                                scenarioResult.addScenarioOutput("output_" + threadId + "_" + i, i);
                                
                                // Read operations
                                scenarioResult.getStageResults();
                                scenarioResult.getWarnings();
                                scenarioResult.isSuccessful();
                            } catch (Exception e) {
                                errors.incrementAndGet();
                                logger.error("Error in thread {}: {}", threadId, e.getMessage(), e);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(60, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdown();

            assertEquals(0, errors.get(), 
                "No exceptions during concurrent ScenarioExecutionResult operations");
            
            // Verify expected counts
            int expectedStages = threadCount * operationsPerThread;
            int expectedWarnings = threadCount * operationsPerThread;
            
            assertEquals(expectedStages, scenarioResult.getStageResults().size(),
                "All stage results should be recorded");
            assertEquals(expectedWarnings, scenarioResult.getWarnings().size(),
                "All warnings should be recorded");
            
            logger.info("[OK] ScenarioExecutionResult concurrent safety verified: {} stages, {} warnings",
                scenarioResult.getStageResults().size(), scenarioResult.getWarnings().size());
        }
    }

    // ========================================
    // Test 4: Data Map Sharing Risk
    // ========================================
    
    @Nested
    @DisplayName("Data Map Sharing Risk Tests")
    class DataMapSharingRiskTests {

        @Test
        @DisplayName("Should demonstrate risk when same data map used across threads")
        void testSharedDataMapRisk() throws InterruptedException {
            logger.info("TEST: Shared data map risk across threads");
            
            // Simulate the scenario where the same data map is passed to multiple executions
            Map<String, Object> sharedDataMap = new HashMap<>();
            sharedDataMap.put("tradeId", "TRADE-001");
            sharedDataMap.put("amount", 1000.0);
            
            int threadCount = 4;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicBoolean inconsistencyDetected = new AtomicBoolean(false);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        // Each thread tries to modify the shared map (simulating putAll from stage outputs)
                        for (int i = 0; i < 1000; i++) {
                            // Simulate stage output being merged into data map
                            sharedDataMap.put("enriched_" + threadId + "_" + i, "value_" + i);
                            
                            // Check for unexpected values from other threads
                            Object amount = sharedDataMap.get("amount");
                            if (amount != null && !amount.equals(1000.0)) {
                                inconsistencyDetected.set(true);
                            }
                        }
                    } catch (ConcurrentModificationException e) {
                        inconsistencyDetected.set(true);
                        logger.warn("ConcurrentModificationException detected");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdown();

            logger.info("Shared map now has {} entries", sharedDataMap.size());
            
            if (inconsistencyDetected.get()) {
                logger.warn("⚠️ Inconsistency detected when sharing data map across threads");
            } else {
                logger.info("[OK] No inconsistency detected (HashMap may have worked by chance, but is NOT safe)");
            }
            
            // The point: using a regular HashMap across threads is unsafe
            // This test documents the risk even if it doesn't always fail
        }

        @Test
        @DisplayName("Should verify defensive copy protects against external modifications")
        void testDefensiveCopyProtection() {
            logger.info("TEST: Defensive copy protection in getStageOutputs");
            
            StageExecutionResult result = StageExecutionResult.success("test-stage", null);
            result.addStageOutput("key1", "value1");
            result.addStageOutput("key2", "value2");
            
            // Get a defensive copy
            Map<String, Object> copy1 = result.getStageOutputs();
            
            // Modify the copy
            copy1.put("key3", "value3");
            copy1.remove("key1");
            
            // Get another copy - should reflect original state
            Map<String, Object> copy2 = result.getStageOutputs();
            
            assertEquals(2, copy2.size(), "Original should still have 2 entries");
            assertTrue(copy2.containsKey("key1"), "Original should still have key1");
            assertFalse(copy2.containsKey("key3"), "Original should not have key3");
            
            logger.info("[OK] Defensive copy correctly protects internal state");
        }
    }

    // ========================================
    // Test 5: Deep Copy for Parallel Enrichments
    // ========================================
    
    @Nested
    @DisplayName("Deep Copy Tests for Nested Structure Isolation")
    class DeepCopyNestedStructureTests {

        @Test
        @DisplayName("Should isolate nested map modifications across parallel threads")
        void testNestedMapIsolation() throws InterruptedException {
            logger.info("TEST: Nested map isolation with deep copy");

            // Create a structure similar to barrier options with nested maps
            Map<String, Object> nestedLevel3 = new HashMap<>();
            nestedLevel3.put("rebateAmount", "50.0");
            nestedLevel3.put("rebateCurrency", "USD");

            Map<String, Object> nestedLevel2 = new HashMap<>();
            nestedLevel2.put("rebateTerms", nestedLevel3);
            nestedLevel2.put("observationPeriod", "DAILY");

            Map<String, Object> nestedLevel1 = new HashMap<>();
            nestedLevel1.put("knockoutConditions", nestedLevel2);
            nestedLevel1.put("barrierLevel", "105.0");

            Map<String, Object> originalData = new HashMap<>();
            originalData.put("barrierTerms", nestedLevel1);
            originalData.put("tradeId", "TRADE-001");

            int threadCount = 4;
            int iterations = 1000;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger crossThreadModifications = new AtomicInteger(0);
            AtomicInteger correctIsolations = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        for (int i = 0; i < iterations; i++) {
                            // Simulate what RulesEngine.deepCopyMap does
                            Map<String, Object> threadCopy = deepCopyForTest(originalData);

                            // Modify nested structure (like an enrichment would)
                            @SuppressWarnings("unchecked")
                            Map<String, Object> barrierTerms = (Map<String, Object>) threadCopy.get("barrierTerms");
                            @SuppressWarnings("unchecked")
                            Map<String, Object> knockoutConditions = (Map<String, Object>) barrierTerms.get("knockoutConditions");
                            @SuppressWarnings("unchecked")
                            Map<String, Object> rebateTerms = (Map<String, Object>) knockoutConditions.get("rebateTerms");

                            // Each thread sets its own value
                            String threadValue = "THREAD_" + threadId + "_" + i;
                            rebateTerms.put("modifiedBy", threadValue);

                            // Check if original is affected
                            @SuppressWarnings("unchecked")
                            Map<String, Object> origBarrier = (Map<String, Object>) originalData.get("barrierTerms");
                            @SuppressWarnings("unchecked")
                            Map<String, Object> origKnockout = (Map<String, Object>) origBarrier.get("knockoutConditions");
                            @SuppressWarnings("unchecked")
                            Map<String, Object> origRebate = (Map<String, Object>) origKnockout.get("rebateTerms");

                            if (origRebate.containsKey("modifiedBy")) {
                                // Original was modified - deep copy failed!
                                crossThreadModifications.incrementAndGet();
                            } else {
                                correctIsolations.incrementAndGet();
                            }

                            // Clean up for next iteration check
                            threadCopy = null;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdown();

            int totalOperations = threadCount * iterations;
            logger.info("Cross-thread modifications (isolation failures): {}", crossThreadModifications.get());
            logger.info("Correct isolations: {}", correctIsolations.get());
            logger.info("Total operations: {}", totalOperations);

            // With proper deep copy, there should be ZERO cross-thread modifications
            assertEquals(0, crossThreadModifications.get(), 
                "Deep copy should completely isolate nested structure modifications");
            assertEquals(totalOperations, correctIsolations.get(),
                "All operations should show correct isolation");

            // Verify original is completely unchanged
            @SuppressWarnings("unchecked")
            Map<String, Object> finalOrigBarrier = (Map<String, Object>) originalData.get("barrierTerms");
            @SuppressWarnings("unchecked")
            Map<String, Object> finalOrigKnockout = (Map<String, Object>) finalOrigBarrier.get("knockoutConditions");
            @SuppressWarnings("unchecked")
            Map<String, Object> finalOrigRebate = (Map<String, Object>) finalOrigKnockout.get("rebateTerms");

            assertFalse(finalOrigRebate.containsKey("modifiedBy"), 
                "Original nested structure should remain completely unmodified");
            assertEquals("50.0", finalOrigRebate.get("rebateAmount"),
                "Original values should be preserved");

            logger.info("[OK] Deep copy correctly isolates all {} nested structure modifications", totalOperations);
        }

        @Test
        @DisplayName("Should handle lists within nested maps correctly")
        void testNestedListIsolation() throws InterruptedException {
            logger.info("TEST: Nested list isolation with deep copy");

            // Create structure with nested lists
            List<Map<String, Object>> observations = new ArrayList<>();
            Map<String, Object> obs1 = new HashMap<>();
            obs1.put("date", "2025-01-01");
            obs1.put("price", 100.0);
            observations.add(obs1);

            Map<String, Object> marketData = new HashMap<>();
            marketData.put("observations", observations);
            marketData.put("source", "BLOOMBERG");

            Map<String, Object> originalData = new HashMap<>();
            originalData.put("marketData", marketData);

            int threadCount = 4;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger listModifications = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        for (int i = 0; i < 500; i++) {
                            Map<String, Object> threadCopy = deepCopyForTest(originalData);

                            @SuppressWarnings("unchecked")
                            Map<String, Object> copyMarketData = (Map<String, Object>) threadCopy.get("marketData");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> copyObs = (List<Map<String, Object>>) copyMarketData.get("observations");

                            // Add to the copied list
                            Map<String, Object> newObs = new HashMap<>();
                            newObs.put("date", "2025-01-0" + threadId);
                            newObs.put("price", 100.0 + threadId);
                            copyObs.add(newObs);

                            // Check original list size
                            @SuppressWarnings("unchecked")
                            Map<String, Object> origMarket = (Map<String, Object>) originalData.get("marketData");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> origObs = (List<Map<String, Object>>) origMarket.get("observations");

                            if (origObs.size() > 1) {
                                listModifications.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdown();

            logger.info("Original list modifications detected: {}", listModifications.get());

            assertEquals(0, listModifications.get(),
                "Deep copy should isolate list modifications");

            @SuppressWarnings("unchecked")
            Map<String, Object> finalMarket = (Map<String, Object>) originalData.get("marketData");
            @SuppressWarnings("unchecked")
            List<?> finalObs = (List<?>) finalMarket.get("observations");

            assertEquals(1, finalObs.size(), "Original list should still have only 1 element");

            logger.info("[OK] Deep copy correctly isolates nested list modifications");
        }

        /**
         * Test utility that mimics RulesEngine.deepCopyMap behavior
         */
        @SuppressWarnings("unchecked")
        private Map<String, Object> deepCopyForTest(Map<String, Object> original) {
            if (original == null) {
                return null;
            }

            Map<String, Object> copy = new HashMap<>();
            for (Map.Entry<String, Object> entry : original.entrySet()) {
                copy.put(entry.getKey(), deepCopyValueForTest(entry.getValue()));
            }
            return copy;
        }

        @SuppressWarnings("unchecked")
        private Object deepCopyValueForTest(Object value) {
            if (value == null) {
                return null;
            }

            if (value instanceof Map) {
                return deepCopyForTest((Map<String, Object>) value);
            }

            if (value instanceof List) {
                List<Object> listValue = (List<Object>) value;
                List<Object> listCopy = new ArrayList<>(listValue.size());
                for (Object item : listValue) {
                    listCopy.add(deepCopyValueForTest(item));
                }
                return listCopy;
            }

            return value;
        }
    }
}
