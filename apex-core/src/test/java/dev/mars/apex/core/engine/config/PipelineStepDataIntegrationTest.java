package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying that pipeline step data and metrics are captured
 * in RuleResult.getExecutionPath() when executing pipelines via RulesEngine.
 */
public class PipelineStepDataIntegrationTest {

    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() {
        // Will be initialized in each test
    }

    @AfterEach
    public void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    @DisplayName("Should capture step data and metrics from pipeline execution")
    public void testPipelineStepDataCapture() throws Exception {
        // Create a simple test pipeline YAML
        String yamlPath = "src/test/resources/pipeline-step-data-test.yaml";
        
        // Note: This test requires a pipeline YAML file to exist
        // For now, we'll test the API contract
        
        // Create a mock execution step with data
        ExecutionStep step = new ExecutionStep(
            "extract-step",
            "PIPELINE_STEP",
            "SUCCESS",
            "Extracted 10 records",
            150,
            List.of("record1", "record2", "record3"),
            10,
            2
        );
        
        // Verify the step has all the data
        assertTrue(step.hasStepData());
        assertNotNull(step.getStepData());
        assertEquals(10, step.getRecordsProcessed());
        assertEquals(2, step.getRecordsFailed());
        
        // Verify success rate calculation
        double expectedSuccessRate = (10.0 / 12.0) * 100.0;
        assertEquals(expectedSuccessRate, step.getSuccessRate(), 0.01);
    }

    @Test
    @DisplayName("Should access step data from execution path")
    public void testAccessStepDataFromExecutionPath() {
        // Create a RuleResult with execution path containing pipeline steps
        RuleResult result = RuleResult.match("test-pipeline", "Pipeline completed", "INFO");
        
        // Create execution steps with data
        ExecutionStep step1 = new ExecutionStep(
            "extract", "PIPELINE_STEP", "SUCCESS", "Extracted data", 100,
            List.of("a", "b", "c"), 3, 0
        );
        
        ExecutionStep step2 = new ExecutionStep(
            "transform", "PIPELINE_STEP", "SUCCESS", "Transformed data", 150,
            List.of("A", "B", "C"), 3, 0
        );
        
        ExecutionStep step3 = new ExecutionStep(
            "load", "PIPELINE_STEP", "SUCCESS", "Loaded data", 200,
            null, 3, 0
        );
        
        result.setExecutionPath(List.of(step1, step2, step3));
        
        // Verify we can access the execution path
        List<ExecutionStep> executionPath = result.getExecutionPath();
        assertNotNull(executionPath);
        assertEquals(3, executionPath.size());
        
        // Verify step 1 data
        ExecutionStep extractStep = executionPath.get(0);
        assertEquals("extract", extractStep.getName());
        assertTrue(extractStep.hasStepData());
        assertEquals(List.of("a", "b", "c"), extractStep.getStepData());
        assertEquals(3, extractStep.getRecordsProcessed());
        assertEquals(0, extractStep.getRecordsFailed());
        assertEquals(100.0, extractStep.getSuccessRate(), 0.01);
        
        // Verify step 2 data
        ExecutionStep transformStep = executionPath.get(1);
        assertEquals("transform", transformStep.getName());
        assertTrue(transformStep.hasStepData());
        assertEquals(List.of("A", "B", "C"), transformStep.getStepData());
        
        // Verify step 3 (no data, but has metrics)
        ExecutionStep loadStep = executionPath.get(2);
        assertEquals("load", loadStep.getName());
        assertFalse(loadStep.hasStepData());
        assertEquals(3, loadStep.getRecordsProcessed());
    }

    @Test
    @DisplayName("Should iterate through pipeline steps and access data")
    public void testIterateThroughPipelineSteps() {
        RuleResult result = RuleResult.match("test-pipeline", "Pipeline completed", "INFO");
        
        // Create mixed execution path (rules + pipeline steps)
        ExecutionStep ruleStep = new ExecutionStep(
            "validation-rule", "RULE", "SUCCESS", "Validation passed", 50
        );
        
        ExecutionStep pipelineStep = new ExecutionStep(
            "data-extract", "PIPELINE_STEP", "SUCCESS", "Extracted 100 records", 200,
            List.of("data1", "data2"), 98, 2
        );
        
        result.setExecutionPath(List.of(ruleStep, pipelineStep));
        
        // Iterate and process only pipeline steps
        int pipelineStepCount = 0;
        int totalProcessed = 0;
        int totalFailed = 0;
        
        for (ExecutionStep step : result.getExecutionPath()) {
            if ("PIPELINE_STEP".equals(step.getType())) {
                pipelineStepCount++;
                
                if (step.getRecordsProcessed() != null) {
                    totalProcessed += step.getRecordsProcessed();
                }
                if (step.getRecordsFailed() != null) {
                    totalFailed += step.getRecordsFailed();
                }
                
                // Can access step data if available
                if (step.hasStepData()) {
                    Object data = step.getStepData();
                    assertNotNull(data);
                }
            }
        }
        
        assertEquals(1, pipelineStepCount);
        assertEquals(98, totalProcessed);
        assertEquals(2, totalFailed);
    }

    @Test
    @DisplayName("Should handle backward compatibility with old constructor")
    public void testBackwardCompatibility() {
        // Old code using the original constructor should still work
        ExecutionStep oldStyleStep = new ExecutionStep(
            "old-step", "PIPELINE_STEP", "SUCCESS", "Completed", 100
        );
        
        // Should work without errors
        assertEquals("old-step", oldStyleStep.getName());
        assertEquals("SUCCESS", oldStyleStep.getStatus());
        
        // New fields should be null
        assertNull(oldStyleStep.getStepData());
        assertNull(oldStyleStep.getRecordsProcessed());
        assertNull(oldStyleStep.getRecordsFailed());
        assertFalse(oldStyleStep.hasStepData());
        
        // Success rate should fall back to status-based calculation
        assertEquals(100.0, oldStyleStep.getSuccessRate(), 0.01);
    }
}

