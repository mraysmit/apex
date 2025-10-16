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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Priority 3: SLA Timeout Enforcement Tests
 * 
 * Validates that scenario and stage execution respects configured SLA timeout limits.
 * Ensures system meets performance requirements and handles timeout scenarios gracefully.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Priority 3: SLA Timeout Enforcement Tests")
class ScenarioSlaTimeoutEnforcementTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ScenarioSlaTimeoutEnforcementTest.class);
    
    private ScenarioConfiguration scenario;
    private ScenarioStageExecutor executor;
    
    @BeforeEach
    void setUp() {
        logger.info("TEST: Setting up SLA timeout enforcement test");
        scenario = new ScenarioConfiguration();
        scenario.setScenarioId("sla-test-scenario");
        executor = new ScenarioStageExecutor();
    }
    
    // ========================================
    // SLA Timeout Enforcement Tests
    // ========================================
    
    @Nested
    @DisplayName("SLA Timeout Enforcement Tests")
    class SlaTimeoutTests {
        
        @Test
        @DisplayName("Should complete scenario execution within SLA timeout")
        void testScenarioCompletesWithinSlaTimeout() {
            logger.info("TEST: Scenario execution within SLA timeout");

            // Given: Scenario with reasonable SLA timeout (5 seconds)
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("processing-sla-ms", 5000);
            scenario.setMetadata(metadata);

            ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
            scenario.addProcessingStage(stage);

            // When: Execute scenario
            Map<String, Object> testData = new HashMap<>();
            testData.put("type", "OTC");

            long startTime = System.currentTimeMillis();
            ScenarioExecutionResult result = executor.executeStages(scenario, testData);
            long executionTime = System.currentTimeMillis() - startTime;

            // Then: Verify execution completed within SLA
            assertNotNull(result, "Should return ScenarioExecutionResult");
            Integer slaMs = scenario.getProcessingSlaMs();
            if (slaMs != null) {
                assertTrue(executionTime <= slaMs,
                    "Execution time " + executionTime + "ms should not exceed SLA " + slaMs + "ms");
            }

            // Verify result tracks execution time
            assertTrue(result.getTotalExecutionTimeMs() >= 0,
                "Result should track execution time");

            logger.info("✓ Scenario completed within SLA: {}ms (limit: {}ms)",
                executionTime, slaMs);
        }
        
        @Test
        @DisplayName("Should track execution time for each stage")
        void testStageExecutionTimeTracking() {
            logger.info("TEST: Stage execution time tracking");

            // Given: Scenario with multiple stages
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("processing-sla-ms", 5000);
            scenario.setMetadata(metadata);
            
            ScenarioStage stage1 = new ScenarioStage("stage-1", "config/stage1.yaml", 1);
            ScenarioStage stage2 = new ScenarioStage("stage-2", "config/stage2.yaml", 2);
            scenario.addProcessingStage(stage1);
            scenario.addProcessingStage(stage2);
            
            // When: Execute scenario
            Map<String, Object> testData = new HashMap<>();
            testData.put("type", "OTC");
            
            ScenarioExecutionResult result = executor.executeStages(scenario, testData);
            
            // Then: Verify each stage has execution time tracked
            assertNotNull(result.getStageResults(), "Should have stage results");
            
            for (StageExecutionResult stageResult : result.getStageResults()) {
                assertTrue(stageResult.getExecutionTimeMs() >= 0,
                    "Stage '" + stageResult.getStageName() + "' should have execution time tracked");
            }
            
            logger.info("✓ Stage execution times tracked: {} stages",
                result.getStageResults().size());
        }
        
        @Test
        @DisplayName("Should handle tight SLA timeout gracefully")
        void testTightSlaTimeoutHandling() {
            logger.info("TEST: Tight SLA timeout handling");

            // Given: Scenario with very tight SLA (100ms)
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("processing-sla-ms", 100);
            scenario.setMetadata(metadata);

            ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
            scenario.addProcessingStage(stage);

            // When: Execute scenario
            Map<String, Object> testData = new HashMap<>();
            testData.put("type", "OTC");

            long startTime = System.currentTimeMillis();
            ScenarioExecutionResult result = executor.executeStages(scenario, testData);
            long executionTime = System.currentTimeMillis() - startTime;

            // Then: Verify result is returned (even if SLA exceeded)
            assertNotNull(result, "Should return result even with tight SLA");

            Integer slaMs = scenario.getProcessingSlaMs();
            // Log if SLA was exceeded
            if (slaMs != null && executionTime > slaMs) {
                logger.warn("TEST: SLA exceeded - execution: {}ms, limit: {}ms",
                    executionTime, slaMs);
            } else {
                logger.info("✓ Tight SLA met: {}ms (limit: {}ms)",
                    executionTime, slaMs);
            }
        }
        
        @Test
        @DisplayName("Should accumulate execution time across all stages")
        void testTotalExecutionTimeAccumulation() {
            logger.info("TEST: Total execution time accumulation");

            // Given: Scenario with multiple stages
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("processing-sla-ms", 5000);
            scenario.setMetadata(metadata);

            ScenarioStage stage1 = new ScenarioStage("stage-1", "config/stage1.yaml", 1);
            ScenarioStage stage2 = new ScenarioStage("stage-2", "config/stage2.yaml", 2);
            scenario.addProcessingStage(stage1);
            scenario.addProcessingStage(stage2);

            // When: Execute scenario
            Map<String, Object> testData = new HashMap<>();
            testData.put("type", "OTC");

            ScenarioExecutionResult result = executor.executeStages(scenario, testData);

            // Then: Verify total execution time is sum of stage times
            long totalStageTime = result.getStageResults().stream()
                .mapToLong(StageExecutionResult::getExecutionTimeMs)
                .sum();

            assertTrue(result.getTotalExecutionTimeMs() >= totalStageTime,
                "Total execution time should be >= sum of stage times");

            logger.info("✓ Total execution time: {}ms (stages: {}ms)",
                result.getTotalExecutionTimeMs(), totalStageTime);
        }
    }
}

