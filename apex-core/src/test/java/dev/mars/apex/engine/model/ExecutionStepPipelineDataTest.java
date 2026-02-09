package dev.mars.apex.engine.model;

import org.junit.jupiter.api.DisplayName;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExecutionStep pipeline data and metrics functionality.
 * Verifies that ExecutionStep can store and retrieve step data and metrics.
 */
public class ExecutionStepPipelineDataTest {

    @Test
    @DisplayName("Should create ExecutionStep with basic constructor (backward compatibility)")
    public void testBasicConstructor() {
        ExecutionStep step = new ExecutionStep("test-step", "PIPELINE_STEP", "SUCCESS", "Step completed", 100);
        
        assertEquals("test-step", step.getName());
        assertEquals("PIPELINE_STEP", step.getType());
        assertEquals("SUCCESS", step.getStatus());
        assertEquals("Step completed", step.getMessage());
        assertEquals(100, step.getDurationMs());
        assertNotNull(step.getTimestamp());
        
        // New fields should be null when using old constructor
        assertNull(step.getStepData());
        assertNull(step.getRecordsProcessed());
        assertNull(step.getRecordsFailed());
        assertFalse(step.hasStepData());
    }

    @Test
    @DisplayName("Should create ExecutionStep with pipeline data and metrics")
    public void testPipelineConstructor() {
        List<String> testData = Arrays.asList("record1", "record2", "record3");
        
        ExecutionStep step = new ExecutionStep(
            "extract-step",
            "PIPELINE_STEP",
            "SUCCESS",
            "Extracted 3 records",
            250,
            testData,
            3,
            0
        );
        
        assertEquals("extract-step", step.getName());
        assertEquals("PIPELINE_STEP", step.getType());
        assertEquals("SUCCESS", step.getStatus());
        assertEquals("Extracted 3 records", step.getMessage());
        assertEquals(250, step.getDurationMs());
        assertNotNull(step.getTimestamp());
        
        // Verify pipeline-specific fields
        assertTrue(step.hasStepData());
        assertNotNull(step.getStepData());
        assertEquals(testData, step.getStepData());
        assertEquals(3, step.getRecordsProcessed());
        assertEquals(0, step.getRecordsFailed());
    }

    @Test
    @DisplayName("Should calculate success rate correctly")
    public void testSuccessRateCalculation() {
        // Test 100% success rate
        ExecutionStep step1 = new ExecutionStep(
            "step1", "PIPELINE_STEP", "SUCCESS", "All succeeded", 100,
            null, 100, 0
        );
        assertEquals(100.0, step1.getSuccessRate(), 0.01);
        
        // Test 75% success rate
        ExecutionStep step2 = new ExecutionStep(
            "step2", "PIPELINE_STEP", "SUCCESS", "Partial success", 100,
            null, 75, 25
        );
        assertEquals(75.0, step2.getSuccessRate(), 0.01);
        
        // Test 0% success rate
        ExecutionStep step3 = new ExecutionStep(
            "step3", "PIPELINE_STEP", "FAILURE", "All failed", 100,
            null, 0, 100
        );
        assertEquals(0.0, step3.getSuccessRate(), 0.01);
        
        // Test with no records (should use status)
        ExecutionStep step4 = new ExecutionStep(
            "step4", "PIPELINE_STEP", "SUCCESS", "No records", 100,
            null, 0, 0
        );
        assertEquals(100.0, step4.getSuccessRate(), 0.01);
    }

    @Test
    @DisplayName("Should handle null metrics gracefully")
    public void testNullMetrics() {
        // Create step with old constructor (no metrics)
        ExecutionStep successStep = new ExecutionStep(
            "step1", "PIPELINE_STEP", "SUCCESS", "Completed", 100
        );
        assertEquals(100.0, successStep.getSuccessRate(), 0.01);
        
        ExecutionStep failureStep = new ExecutionStep(
            "step2", "PIPELINE_STEP", "FAILURE", "Failed", 100
        );
        assertEquals(0.0, failureStep.getSuccessRate(), 0.01);
    }

    @Test
    @DisplayName("Should store different data types")
    public void testDifferentDataTypes() {
        // Test with List
        List<String> listData = Arrays.asList("a", "b", "c");
        ExecutionStep step1 = new ExecutionStep(
            "step1", "PIPELINE_STEP", "SUCCESS", "List data", 100,
            listData, 3, 0
        );
        assertTrue(step1.getStepData() instanceof List);
        assertEquals(listData, step1.getStepData());
        
        // Test with String
        String stringData = "test data";
        ExecutionStep step2 = new ExecutionStep(
            "step2", "PIPELINE_STEP", "SUCCESS", "String data", 100,
            stringData, 1, 0
        );
        assertTrue(step2.getStepData() instanceof String);
        assertEquals(stringData, step2.getStepData());
        
        // Test with null data
        ExecutionStep step3 = new ExecutionStep(
            "step3", "PIPELINE_STEP", "SUCCESS", "No data", 100,
            null, 0, 0
        );
        assertNull(step3.getStepData());
        assertFalse(step3.hasStepData());
    }

    @Test
    @DisplayName("Should include metrics in toString()")
    public void testToStringWithMetrics() {
        ExecutionStep step = new ExecutionStep(
            "test-step", "PIPELINE_STEP", "SUCCESS", "Completed", 100,
            null, 95, 5
        );
        
        String toString = step.toString();
        assertTrue(toString.contains("test-step"));
        assertTrue(toString.contains("PIPELINE_STEP"));
        assertTrue(toString.contains("SUCCESS"));
        assertTrue(toString.contains("recordsProcessed=95"));
        assertTrue(toString.contains("recordsFailed=5"));
        assertTrue(toString.contains("successRate=95.0%"));
    }
}

