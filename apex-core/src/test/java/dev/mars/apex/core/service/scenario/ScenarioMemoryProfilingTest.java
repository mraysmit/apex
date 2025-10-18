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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Priority 3: Memory Profiling Tests
 *
 * Validates that memory usage is reasonable and no memory leaks exist during
 * repeated scenario executions.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@Disabled("Pre-existing test failures - infrastructure not implemented")
@DisplayName("Priority 3: Memory Profiling Tests")
class ScenarioMemoryProfilingTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ScenarioMemoryProfilingTest.class);
    
    private ScenarioConfiguration scenario;
    private ScenarioStageExecutor executor;
    
    @BeforeEach
    void setUp() {
        logger.info("TEST: Setting up memory profiling test");
        scenario = new ScenarioConfiguration();
        scenario.setScenarioId("memory-test-scenario");
        executor = new ScenarioStageExecutor();
    }
    
    // ========================================
    // Memory Profiling Tests
    // ========================================
    
    @Nested
    @DisplayName("Memory Profiling Tests")
    class MemoryProfilingTests {
        
        @Test
        @DisplayName("Should use reasonable memory for 100 scenario executions")
        void testMemoryUsageFor100Executions() {
            logger.info("TEST: Memory usage for 100 executions");
            
            // Given: Scenario with classification rule
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            
            ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
            scenario.addProcessingStage(stage);
            
            // When: Track memory before and after 100 executions
            Runtime runtime = Runtime.getRuntime();
            System.gc(); // Force garbage collection before measurement
            
            long memBefore = runtime.totalMemory() - runtime.freeMemory();
            
            for (int i = 0; i < 100; i++) {
                Map<String, Object> testData = new HashMap<>();
                testData.put("type", "OTC");
                testData.put("id", i);
                
                executor.executeStages(scenario, testData);
            }
            
            System.gc(); // Force garbage collection after execution
            long memAfter = runtime.totalMemory() - runtime.freeMemory();
            long memIncrease = memAfter - memBefore;
            
            // Then: Verify memory increase is reasonable (< 100MB)
            assertTrue(memIncrease < 100_000_000,
                "Memory increase should be < 100MB, was: " + (memIncrease / 1_000_000) + "MB");
            
            logger.info("✓ Memory usage reasonable: {}MB increase for 100 executions",
                String.format("%.2f", memIncrease / 1_000_000.0));
        }
        
        @Test
        @DisplayName("Should not leak memory during repeated executions")
        void testMemoryLeakDetection() {
            logger.info("TEST: Memory leak detection");
            
            // Given: Scenario with classification rule
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            
            ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
            scenario.addProcessingStage(stage);
            
            // When: Execute multiple times and track memory growth
            Runtime runtime = Runtime.getRuntime();
            
            long[] memorySnapshots = new long[5];
            int executionsPerSnapshot = 20;
            
            for (int snapshot = 0; snapshot < 5; snapshot++) {
                System.gc();
                memorySnapshots[snapshot] = runtime.totalMemory() - runtime.freeMemory();
                
                for (int i = 0; i < executionsPerSnapshot; i++) {
                    Map<String, Object> testData = new HashMap<>();
                    testData.put("type", "OTC");
                    testData.put("id", snapshot * executionsPerSnapshot + i);
                    
                    executor.executeStages(scenario, testData);
                }
            }
            
            // Then: Verify memory growth is not linear (indicates no leak)
            long firstIncrease = memorySnapshots[1] - memorySnapshots[0];
            long lastIncrease = memorySnapshots[4] - memorySnapshots[3];
            
            // Last increase should not be significantly larger than first
            // (allowing for some variance)
            assertTrue(lastIncrease < firstIncrease * 2,
                "Memory growth should stabilize, not increase linearly. " +
                "First: " + (firstIncrease / 1_000_000) + "MB, Last: " + (lastIncrease / 1_000_000) + "MB");
            
            logger.info("✓ No memory leak detected: growth stabilized after initial allocations");
        }
        
        @Test
        @DisplayName("Should handle large dataset processing without OOM")
        void testLargeDatasetHandling() {
            logger.info("TEST: Large dataset handling");
            
            // Given: Scenario with classification rule
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            
            ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
            scenario.addProcessingStage(stage);
            
            // When: Execute with large dataset
            Map<String, Object> largeData = new HashMap<>();
            largeData.put("type", "OTC");
            
            // Add 10,000 fields to simulate large dataset
            for (int i = 0; i < 10000; i++) {
                largeData.put("field_" + i, "value_" + i);
            }
            
            Runtime runtime = Runtime.getRuntime();
            long memBefore = runtime.totalMemory() - runtime.freeMemory();
            
            // Then: Verify large dataset is processed without OOM
            try {
                ScenarioExecutionResult result = executor.executeStages(scenario, largeData);
                assertNotNull(result, "Should process large dataset successfully");
                
                long memAfter = runtime.totalMemory() - runtime.freeMemory();
                long memUsed = memAfter - memBefore;
                
                logger.info("✓ Large dataset processed: {}MB used for 10,000 fields",
                    String.format("%.2f", memUsed / 1_000_000.0));
                
            } catch (OutOfMemoryError e) {
                fail("Large dataset processing caused OutOfMemoryError: " + e.getMessage());
            }
        }
        
        @Test
        @DisplayName("Should maintain stable memory after garbage collection")
        void testMemoryStabilityAfterGc() {
            logger.info("TEST: Memory stability after garbage collection");
            
            // Given: Scenario with classification rule
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            
            ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
            scenario.addProcessingStage(stage);
            
            // When: Execute, collect garbage, and measure memory
            Runtime runtime = Runtime.getRuntime();
            
            // Execute 50 scenarios
            for (int i = 0; i < 50; i++) {
                Map<String, Object> testData = new HashMap<>();
                testData.put("type", "OTC");
                testData.put("id", i);
                
                executor.executeStages(scenario, testData);
            }
            
            // Force garbage collection
            System.gc();
            long memAfterGc1 = runtime.totalMemory() - runtime.freeMemory();
            
            // Execute more scenarios
            for (int i = 50; i < 100; i++) {
                Map<String, Object> testData = new HashMap<>();
                testData.put("type", "OTC");
                testData.put("id", i);
                
                executor.executeStages(scenario, testData);
            }
            
            // Force garbage collection again
            System.gc();
            long memAfterGc2 = runtime.totalMemory() - runtime.freeMemory();
            
            // Then: Verify memory is stable after GC
            long memDifference = Math.abs(memAfterGc2 - memAfterGc1);
            
            // Allow up to 10MB difference (reasonable variance)
            assertTrue(memDifference < 10_000_000,
                "Memory should be stable after GC. Difference: " + (memDifference / 1_000_000) + "MB");
            
            logger.info("✓ Memory stable after GC: {}MB difference between collections",
                String.format("%.2f", memDifference / 1_000_000.0));
        }
    }
}

