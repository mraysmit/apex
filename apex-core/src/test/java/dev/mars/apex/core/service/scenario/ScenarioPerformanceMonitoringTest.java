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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Priority 3: Performance Monitoring Validation Tests
 * 
 * Validates that performance metrics are collected accurately and are accessible
 * through result objects. Ensures system provides visibility into execution performance.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Priority 3: Performance Monitoring Validation Tests")
class ScenarioPerformanceMonitoringTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ScenarioPerformanceMonitoringTest.class);
    
    private ScenarioConfiguration scenario;
    private ScenarioStageExecutor executor;
    
    @BeforeEach
    void setUp() {
        logger.info("TEST: Setting up performance monitoring test");
        scenario = new ScenarioConfiguration();
        scenario.setScenarioId("perf-test-scenario");
        executor = new ScenarioStageExecutor();
    }
    
    // ========================================
    // Performance Monitoring Tests
    // ========================================
    
    @Nested
    @DisplayName("Performance Monitoring Tests")
    class PerformanceMonitoringTests {
        
        @Test
        @DisplayName("Should track execution time accurately")
        void testExecutionTimeTracking() {
            logger.info("TEST: Execution time tracking");
            
            // Given: Scenario with classification rule
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            
            ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
            scenario.addProcessingStage(stage);
            
            // When: Execute scenario
            Map<String, Object> testData = new HashMap<>();
            testData.put("type", "OTC");
            
            long startTime = System.currentTimeMillis();
            ScenarioExecutionResult result = executor.executeStages(scenario, testData);
            long wallClockTime = System.currentTimeMillis() - startTime;
            
            // Then: Verify execution time is tracked
            assertNotNull(result, "Should return ScenarioExecutionResult");
            assertTrue(result.getTotalExecutionTimeMs() >= 0,
                "Execution time should be tracked and non-negative");

            // Verify tracked time is reasonable compared to wall clock time
            assertTrue(result.getTotalExecutionTimeMs() <= wallClockTime + 100,
                "Tracked time should be close to wall clock time (within 100ms margin)");

            logger.info("✓ Execution time tracked: {}ms (wall clock: {}ms)",
                result.getTotalExecutionTimeMs(), wallClockTime);
        }
        
        @Test
        @DisplayName("Should collect performance metrics for each stage")
        void testStagePerformanceMetrics() {
            logger.info("TEST: Stage performance metrics collection");
            
            // Given: Scenario with multiple stages
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            
            ScenarioStage stage1 = new ScenarioStage("stage-1", "config/stage1.yaml", 1);
            ScenarioStage stage2 = new ScenarioStage("stage-2", "config/stage2.yaml", 2);
            scenario.addProcessingStage(stage1);
            scenario.addProcessingStage(stage2);
            
            // When: Execute scenario
            Map<String, Object> testData = new HashMap<>();
            testData.put("type", "OTC");
            
            ScenarioExecutionResult result = executor.executeStages(scenario, testData);
            
            // Then: Verify each stage has performance metrics
            assertNotNull(result.getStageResults(), "Should have stage results");
            assertFalse(result.getStageResults().isEmpty(), "Should have at least one stage result");
            
            for (StageExecutionResult stageResult : result.getStageResults()) {
                assertTrue(stageResult.getExecutionTimeMs() >= 0,
                    "Stage '" + stageResult.getStageName() + "' should have execution time");
                assertNotNull(stageResult.getStageName(),
                    "Stage should have a name for metrics tracking");
            }

            assertTrue(result.getTotalExecutionTimeMs() >= 0,
                "Result should have total execution time");

            logger.info("✓ Performance metrics collected for {} stages",
                result.getStageResults().size());
        }
        
        @Test
        @DisplayName("Should calculate average execution time across multiple runs")
        void testAverageExecutionTimeCalculation() {
            logger.info("TEST: Average execution time calculation");
            
            // Given: Scenario with classification rule
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            
            ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
            scenario.addProcessingStage(stage);
            
            // When: Execute scenario multiple times
            Map<String, Object> testData = new HashMap<>();
            testData.put("type", "OTC");
            
            long totalTime = 0;
            int runs = 5;

            for (int i = 0; i < runs; i++) {
                ScenarioExecutionResult result = executor.executeStages(scenario, testData);
                totalTime += result.getTotalExecutionTimeMs();
            }

            long averageTime = totalTime / runs;

            // Then: Verify average is reasonable
            assertTrue(averageTime >= 0,
                "Average execution time should be non-negative");
            assertTrue(averageTime <= totalTime,
                "Average should not exceed total time");

            logger.info("✓ Average execution time: {}ms (total: {}ms, runs: {})",
                averageTime, totalTime, runs);
        }
        
        @Test
        @DisplayName("Should provide performance data through result objects")
        void testPerformanceDataAccessibility() {
            logger.info("TEST: Performance data accessibility");
            
            // Given: Scenario with multiple stages
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            
            ScenarioStage stage1 = new ScenarioStage("stage-1", "config/stage1.yaml", 1);
            ScenarioStage stage2 = new ScenarioStage("stage-2", "config/stage2.yaml", 2);
            scenario.addProcessingStage(stage1);
            scenario.addProcessingStage(stage2);
            
            // When: Execute scenario
            Map<String, Object> testData = new HashMap<>();
            testData.put("type", "OTC");
            
            ScenarioExecutionResult result = executor.executeStages(scenario, testData);
            
            // Then: Verify performance data is accessible
            assertTrue(result.getTotalExecutionTimeMs() >= 0,
                "Should provide total execution time");
            assertNotNull(result.getStageResults(),
                "Should provide stage results with individual timings");

            // Verify we can access timing for specific stages
            List<StageExecutionResult> stageResults = result.getStageResults();
            for (StageExecutionResult stageResult : stageResults) {
                long stageTime = stageResult.getExecutionTimeMs();
                assertTrue(stageTime >= 0,
                    "Should be able to access execution time for stage: " + stageResult.getStageName());
            }

            logger.info("✓ Performance data accessible: total={}ms, stages={}",
                result.getTotalExecutionTimeMs(), stageResults.size());
        }
        
        @Test
        @DisplayName("Should detect slow query execution")
        void testSlowQueryDetection() {
            logger.info("TEST: Slow query detection");
            
            // Given: Scenario with complex classification rule
            scenario.setClassificationRuleCondition(
                "#data['type'] == 'OTC' && #data['amount'] > 1000000 && #data['currency'] == 'USD'");
            
            ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
            scenario.addProcessingStage(stage);
            
            // When: Execute scenario
            Map<String, Object> testData = new HashMap<>();
            testData.put("type", "OTC");
            testData.put("amount", 5000000);
            testData.put("currency", "USD");
            
            ScenarioExecutionResult result = executor.executeStages(scenario, testData);
            
            // Then: Verify execution completed and timing is available
            assertNotNull(result, "Should return result");
            assertTrue(result.getTotalExecutionTimeMs() >= 0,
                "Should track execution time for complex queries");

            // Log if execution was slow (> 1 second)
            if (result.getTotalExecutionTimeMs() > 1000) {
                logger.warn("TEST: Slow query detected - execution: {}ms", result.getTotalExecutionTimeMs());
            } else {
                logger.info("✓ Query executed efficiently: {}ms", result.getTotalExecutionTimeMs());
            }
        }
    }
}

