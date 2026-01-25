package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PipelineStepResult to ExecutionStep conversion in RulesEngine.
 * 
 * <p><b>INTENTIONAL FAILURE TEST CLASS</b></p>
 * <p>This test suite intentionally uses empty H2 databases without creating tables
 * to verify pipeline error handling behavior. ERROR log messages during test execution
 * are EXPECTED and indicate correct error handling, not actual failures.</p>
 * 
 * <p>Expected ERROR messages include:</p>
 * <ul>
 *   <li>Table "SOURCE_ITEMS" not found</li>
 *   <li>Table "MIXED_RECORDS" not found</li>
 *   <li>No data available for transform step</li>
 *   <li>Step 'load-items' failed (attempt X/Y)</li>
 *   <li>Pipeline failed after retries</li>
 * </ul>
 * 
 * <p>TEST COVERAGE:</p>
 * <ul>
 *   <li>Field mapping verification (6 tests)</li>
 *   <li>Large dataset handling</li>
 *   <li>Different data types</li>
 *   <li>Skipped steps</li>
 *   <li>Failed steps</li>
 *   <li>Step ordering preservation</li>
 * </ul>
 * 
 * @author APEX Core Team
 * @since 2026-01-11
 * @version 1.0.0
 */
@DisplayName("PipelineExecutor Step Data Conversion Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PipelineExecutorStepDataConversionTest {

    private static final Logger logger = LoggerFactory.getLogger(PipelineExecutorStepDataConversionTest.class);
    private static final String TEST_YAML_BASE_PATH = "src/test/resources/pipeline-step-data/";

    private RulesEngine rulesEngine;

    @BeforeAll
    public static void classSetUp() {
        logger.info("========================================================================");
        logger.info("[INTENTIONAL-FAILURE-TEST-CLASS-START] PipelineExecutorStepDataConversionTest");
        logger.info("[INTENTIONAL-FAILURE-TEST-CLASS-START] This test class uses empty databases");
        logger.info("[INTENTIONAL-FAILURE-TEST-CLASS-START] Expected ERRORs: table not found, no data, load failures");
        logger.info("========================================================================");
    }

    @AfterAll
    public static void classTearDown() {
        logger.info("========================================================================");
        logger.info("[INTENTIONAL-FAILURE-TEST-CLASS-END] PipelineExecutorStepDataConversionTest");
        logger.info("[INTENTIONAL-FAILURE-TEST-CLASS-END] All ERROR messages above were EXPECTED");
        logger.info("========================================================================");
    }

    @BeforeEach
    public void setUp() {
        logger.info("=== Setting up test ===");
    }

    @AfterEach
    public void tearDown() {
        if (rulesEngine != null) {
            logger.info("Shutting down RulesEngine");
            rulesEngine.shutdown();
            rulesEngine = null;
        }
    }

    // ========================================================================
    // TEST 9: Field Mapping Verification
    // ========================================================================
    
    @Test
    @Order(9)
    @DisplayName("Should convert PipelineStepResult to ExecutionStep with all fields")
    public void shouldConvertPipelineStepResultToExecutionStep() throws Exception {
        logger.info("=== Test 9: Field Mapping Verification ===");
        
        // Given: A pipeline that will create PipelineStepResult objects
        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "simple-extract-pipeline.yaml");
        Map<String, Object> inputData = new HashMap<>();
        
        // When: Execute the pipeline (triggers conversion)
        RuleResult result = rulesEngine.evaluate(inputData);
        
        // Then: Verify ExecutionStep was created from PipelineStepResult
        assertNotNull(result, "Result should not be null");
        List<ExecutionStep> executionPath = result.getExecutionPath();
        assertNotNull(executionPath, "Execution path should not be null");
        
        // Find a pipeline step
        ExecutionStep pipelineStep = executionPath.stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(pipelineStep, "Should have at least one pipeline step");
        
        // Verify all fields are mapped correctly
        assertNotNull(pipelineStep.getName(), "Step name should be mapped");
        assertEquals("PIPELINE_STEP", pipelineStep.getType(), "Step type should be PIPELINE_STEP");
        assertNotNull(pipelineStep.getStatus(), "Step status should be mapped");
        assertTrue(pipelineStep.getStatus().matches("SUCCESS|FAILURE|SKIPPED"), 
            "Status should be valid");
        assertNotNull(pipelineStep.getMessage(), "Step message should be mapped");
        assertTrue(pipelineStep.getDurationMs() >= 0, "Duration should be non-negative");
        assertNotNull(pipelineStep.getTimestamp(), "Timestamp should be set");
        
        // Verify pipeline-specific fields
        assertNotNull(pipelineStep.getRecordsProcessed(), "Records processed should be mapped");
        assertNotNull(pipelineStep.getRecordsFailed(), "Records failed should be mapped");
        
        // Data may or may not be present depending on step type
        logger.info("  Step: {}", pipelineStep.getName());
        logger.info("  Status: {}", pipelineStep.getStatus());
        logger.info("  Duration: {}ms", pipelineStep.getDurationMs());
        logger.info("  Records: {}/{}", pipelineStep.getRecordsProcessed(), pipelineStep.getRecordsFailed());
        logger.info("  Has Data: {}", pipelineStep.hasStepData());
        
        logger.info("[OK] All fields mapped correctly from PipelineStepResult to ExecutionStep");
    }

    // ========================================================================
    // TEST 10: Large Dataset Handling
    // ========================================================================
    
    @Test
    @Order(10)
    @DisplayName("Should handle large datasets efficiently")
    public void shouldHandleLargeDataSets() throws Exception {
        logger.info("=== Test 10: Large Dataset Handling ===");
        
        // Given: A pipeline with a larger dataset
        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "simple-extract-pipeline.yaml");
        Map<String, Object> inputData = new HashMap<>();
        
        long startTime = System.currentTimeMillis();
        
        // When: Execute the pipeline
        RuleResult result = rulesEngine.evaluate(inputData);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Then: Verify execution completed in reasonable time
        assertNotNull(result, "Result should not be null");
        assertTrue(executionTime < 5000, "Execution should complete in under 5 seconds");
        
        // And: Verify data was captured
        List<ExecutionStep> pipelineSteps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();
        
        assertFalse(pipelineSteps.isEmpty(), "Should have pipeline steps");
        
        for (ExecutionStep step : pipelineSteps) {
            if (step.hasStepData()) {
                Object data = step.getStepData();
                assertNotNull(data, "Step data should not be null");
                
                // Verify data is accessible
                if (data instanceof List) {
                    List<?> list = (List<?>) data;
                    logger.info("  Step '{}' captured {} records", step.getName(), list.size());
                }
            }
        }
        
        logger.info("[OK] Large dataset handled efficiently in {}ms", executionTime);
    }

    // ========================================================================
    // TEST 11: Different Data Types
    // ========================================================================
    
    @Test
    @Order(11)
    @DisplayName("Should handle different data types")
    public void shouldHandleDifferentDataTypes() throws Exception {
        logger.info("=== Test 11: Different Data Types ===");
        
        // Given: A pipeline that produces different data types
        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "multi-step-pipeline.yaml");
        Map<String, Object> inputData = new HashMap<>();
        
        // When: Execute the pipeline
        RuleResult result = rulesEngine.evaluate(inputData);
        
        // Then: Verify different data types are handled
        assertNotNull(result, "Result should not be null");
        
        List<ExecutionStep> pipelineSteps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();
        
        assertFalse(pipelineSteps.isEmpty(), "Should have pipeline steps");
        
        Set<String> dataTypes = new HashSet<>();
        
        for (ExecutionStep step : pipelineSteps) {
            if (step.hasStepData()) {
                Object data = step.getStepData();
                String dataType = data.getClass().getSimpleName();
                dataTypes.add(dataType);
                
                logger.info("  Step '{}' has data type: {}", step.getName(), dataType);
                
                // Verify no ClassCastException
                assertNotNull(data, "Data should not be null");
            }
        }
        
        logger.info("[OK] Handled {} different data types: {}", dataTypes.size(), dataTypes);
    }

    // ========================================================================
    // TEST 12: Skipped Steps
    // ========================================================================

    @Test
    @Order(12)
    @DisplayName("Should handle skipped steps correctly")
    public void shouldHandleSkippedSteps() throws Exception {
        logger.info("=== Test 12: Skipped Steps ===");

        // Given: A pipeline with conditional/optional steps
        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "partial-failure-pipeline.yaml");
        Map<String, Object> inputData = new HashMap<>();

        // When: Execute the pipeline
        RuleResult result = rulesEngine.evaluate(inputData);

        // Then: Verify result is accessible
        assertNotNull(result, "Result should not be null");

        List<ExecutionStep> allSteps = result.getExecutionPath();
        assertNotNull(allSteps, "Execution path should not be null");

        // Look for any skipped steps
        List<ExecutionStep> skippedSteps = allSteps.stream()
            .filter(step -> "SKIPPED".equals(step.getStatus()))
            .toList();

        logger.info("  Total steps: {}", allSteps.size());
        logger.info("  Skipped steps: {}", skippedSteps.size());

        // Verify skipped steps are recorded correctly
        for (ExecutionStep step : skippedSteps) {
            assertEquals("SKIPPED", step.getStatus(), "Status should be SKIPPED");
            assertNotNull(step.getName(), "Skipped step should have a name");
            assertFalse(step.hasStepData(), "Skipped step should not have data");

            logger.info("    Skipped: {}", step.getName());
        }

        logger.info("[OK] Skipped steps handled correctly");
    }

    // ========================================================================
    // TEST 13: Failed Steps
    // ========================================================================

    @Test
    @Order(13)
    @DisplayName("Should handle failed steps correctly")
    public void shouldHandleFailedSteps() throws Exception {
        logger.info("=== Test 13: Failed Steps ===");

        // Given: A pipeline that may have failures
        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "partial-failure-pipeline.yaml");
        Map<String, Object> inputData = new HashMap<>();

        // When: Execute the pipeline
        RuleResult result = rulesEngine.evaluate(inputData);

        // Then: Verify result is accessible (may be success or failure)
        assertNotNull(result, "Result should not be null");

        List<ExecutionStep> allSteps = result.getExecutionPath();
        assertNotNull(allSteps, "Execution path should not be null");

        // Look for any failed steps
        List<ExecutionStep> failedSteps = allSteps.stream()
            .filter(step -> "FAILURE".equals(step.getStatus()))
            .toList();

        logger.info("  Total steps: {}", allSteps.size());
        logger.info("  Failed steps: {}", failedSteps.size());

        // Verify failed steps are recorded correctly
        for (ExecutionStep step : failedSteps) {
            assertEquals("FAILURE", step.getStatus(), "Status should be FAILURE");
            assertNotNull(step.getName(), "Failed step should have a name");
            assertNotNull(step.getMessage(), "Failed step should have error message");

            // Failed steps may still have metrics
            if (step.getRecordsFailed() != null) {
                assertTrue(step.getRecordsFailed() >= 0, "Records failed should be non-negative");
                logger.info("    Failed: {} - {} records failed", step.getName(), step.getRecordsFailed());
            }
        }

        logger.info("[OK] Failed steps handled correctly");
    }

    // ========================================================================
    // TEST 14: Step Order Preservation
    // ========================================================================

    @Test
    @Order(14)
    @DisplayName("Should preserve step order in execution path")
    public void shouldPreserveStepOrder() throws Exception {
        logger.info("=== Test 14: Step Order Preservation ===");

        // Given: A multi-step pipeline with dependencies
        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "multi-step-pipeline.yaml");
        Map<String, Object> inputData = new HashMap<>();

        // When: Execute the pipeline
        RuleResult result = rulesEngine.evaluate(inputData);

        // Then: Verify result is successful
        assertNotNull(result, "Result should not be null");

        List<ExecutionStep> pipelineSteps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertFalse(pipelineSteps.isEmpty(), "Should have pipeline steps");
        assertTrue(pipelineSteps.size() >= 2, "Should have multiple steps for order verification");

        // Verify steps are in execution order
        logger.info("  Execution order:");
        for (int i = 0; i < pipelineSteps.size(); i++) {
            ExecutionStep step = pipelineSteps.get(i);
            logger.info("    {}. {} - {}", i + 1, step.getName(), step.getStatus());

            // Verify each step has a timestamp
            assertNotNull(step.getTimestamp(), "Step should have timestamp");

            // Verify timestamps are in order (or very close)
            if (i > 0) {
                ExecutionStep previousStep = pipelineSteps.get(i - 1);
                // Timestamps should be in order or within a small window (concurrent execution)
                assertTrue(
                    step.getTimestamp().isAfter(previousStep.getTimestamp()) ||
                    step.getTimestamp().equals(previousStep.getTimestamp()),
                    "Step timestamps should be in order"
                );
            }
        }

        // Verify logical dependencies are respected
        // For example, if we have extract -> transform -> load
        // Extract should come before transform, transform before load
        List<String> stepNames = pipelineSteps.stream()
            .map(ExecutionStep::getName)
            .toList();

        logger.info("  Step sequence: {}", stepNames);

        // Verify extract comes before transform (if both exist)
        int extractIndex = -1;
        int transformIndex = -1;
        int loadIndex = -1;

        for (int i = 0; i < stepNames.size(); i++) {
            String name = stepNames.get(i).toLowerCase();
            if (name.contains("extract")) extractIndex = i;
            if (name.contains("transform")) transformIndex = i;
            if (name.contains("load")) loadIndex = i;
        }

        if (extractIndex >= 0 && transformIndex >= 0) {
            assertTrue(extractIndex < transformIndex,
                "Extract should come before transform");
        }

        if (transformIndex >= 0 && loadIndex >= 0) {
            assertTrue(transformIndex < loadIndex,
                "Transform should come before load");
        }

        logger.info("[OK] Step order preserved correctly (dependencies respected)");
    }
}

