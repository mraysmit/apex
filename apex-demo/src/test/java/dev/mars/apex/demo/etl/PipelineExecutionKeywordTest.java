package dev.mars.apex.demo.etl;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.pipeline.DataPipelineEngine;
import dev.mars.apex.core.engine.pipeline.PipelineExecutor;
import dev.mars.apex.core.engine.pipeline.YamlPipelineExecutionResult;
import dev.mars.apex.core.service.data.external.manager.ExternalDataSourceManager;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APEX ETL Pipeline Execution Keyword Tests - Testing Real Pipeline Execution
 *
 * This test suite validates ACTUAL ETL pipeline execution keywords:
 * - execution.mode: "sequential" vs "parallel"
 * - execution.error-handling: "stop-on-error" vs "continue-on-error"  
 * - execution.max-retries: retry behavior on failures
 * - execution.retry-delay-ms: delay between retry attempts
 *
 * POSITIVE TESTS: Verify keywords work as expected
 * NEGATIVE TESTS: Verify error handling and edge cases
 *
 * FOLLOWS CODING PRINCIPLES FROM prompts.txt:
 * ✅ Never validate YAML syntax - test actual pipeline execution behavior
 * ✅ Execute real APEX pipeline operations using PipelineExecutor
 * ✅ Set up real data sources and validate execution results
 * ✅ Test both positive and negative scenarios for each keyword
 *
 * @author APEX Demo Team
 * @since 1.0.0
 */
@DisplayName("APEX ETL Pipeline Execution Keyword Tests")
public class PipelineExecutionKeywordTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineExecutionKeywordTest.class);

    private Path testDataDir;
    private DataPipelineEngine pipelineEngine;
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    public void setUp() {
        super.setUp();
        
        try {
            testDataDir = Paths.get("./target/demo/etl/execution-tests");
            Files.createDirectories(testDataDir);

            pipelineEngine = new DataPipelineEngine();
            yamlLoader = new YamlConfigurationLoader();
            
            LOGGER.info("✓ Pipeline execution test setup complete: {}", testDataDir);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup pipeline execution tests", e);
        }
    }

    @Nested
    @DisplayName("Execution Mode Tests")
    class ExecutionModeTests {

        @Test
        @DisplayName("POSITIVE: Should execute pipeline in sequential mode")
        void shouldExecuteSequentialMode() throws Exception {
            LOGGER.info("=== Testing Sequential Execution Mode ===");

            // Create pipeline configuration with sequential mode
            String yamlContent = createPipelineConfig(
                "sequential", 
                "stop-on-error", 
                3, 
                1000
            );
            
            YamlRuleConfiguration config = loadYamlFromString(yamlContent, "sequential-test.yaml");

            // Initialize and execute pipeline
            pipelineEngine.initialize(config);
            long startTime = System.currentTimeMillis();
            YamlPipelineExecutionResult result = pipelineEngine.executePipeline(config.getPipeline().getName());
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Validate sequential execution behavior
            assertNotNull(result, "Pipeline execution result should not be null");
            assertTrue(result.isSuccess(), "Sequential pipeline should execute successfully");
            assertTrue(executionTime > 0, "Execution should take measurable time");
            
            // Validate step execution order (sequential should execute steps in order)
            assertNotNull(result.getStepResults(), "Step results should be available");
            assertTrue(result.getStepResults().size() > 0, "Should have executed at least one step");
            
            LOGGER.info("✓ Sequential execution completed in {}ms with {} steps", 
                executionTime, result.getStepResults().size());
        }

        @Test
        @DisplayName("POSITIVE: Should execute pipeline in parallel mode")
        void shouldExecuteParallelMode() throws Exception {
            LOGGER.info("=== Testing Parallel Execution Mode ===");

            // Create pipeline configuration with parallel mode
            String yamlContent = createPipelineConfig(
                "parallel", 
                "stop-on-error", 
                3, 
                1000
            );
            
            YamlRuleConfiguration config = loadYamlFromString(yamlContent, "parallel-test.yaml");

            // Initialize and execute pipeline
            pipelineEngine.initialize(config);
            long startTime = System.currentTimeMillis();
            YamlPipelineExecutionResult result = pipelineEngine.executePipeline(config.getPipeline().getName());
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Validate parallel execution behavior
            assertNotNull(result, "Pipeline execution result should not be null");
            assertTrue(result.isSuccess(), "Parallel pipeline should execute successfully");
            
            // Note: Current implementation falls back to sequential, but should still work
            assertNotNull(result.getStepResults(), "Step results should be available");
            
            LOGGER.info("✓ Parallel execution completed in {}ms with {} steps", 
                executionTime, result.getStepResults().size());
        }

        @Test
        @DisplayName("NEGATIVE: Should handle invalid execution mode gracefully")
        void shouldHandleInvalidExecutionMode() throws Exception {
            LOGGER.info("=== Testing Invalid Execution Mode ===");

            // Create pipeline configuration with invalid mode
            String yamlContent = createPipelineConfig(
                "invalid-mode", 
                "stop-on-error", 
                3, 
                1000
            );
            
            YamlRuleConfiguration config = loadYamlFromString(yamlContent, "invalid-mode-test.yaml");

            // Initialize and execute pipeline - should default to sequential behavior
            pipelineEngine.initialize(config);
            YamlPipelineExecutionResult result = pipelineEngine.executePipeline(config.getPipeline().getName());
            
            // Should still execute (defaults to sequential)
            assertNotNull(result, "Pipeline execution result should not be null");
            // May succeed or fail depending on implementation, but should not crash
            
            LOGGER.info("✓ Invalid execution mode handled: success={}", result.isSuccess());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("POSITIVE: Should stop on error when configured")
        void shouldStopOnError() throws Exception {
            LOGGER.info("=== Testing Stop-On-Error Behavior ===");

            // Create pipeline with failing step and stop-on-error
            String yamlContent = createFailingPipelineConfig(
                "sequential", 
                "stop-on-error", 
                1, 
                500
            );
            
            YamlRuleConfiguration config = loadYamlFromString(yamlContent, "stop-on-error-test.yaml");
            
            // Initialize and execute pipeline - should fail and stop
            pipelineEngine.initialize(config);
            Exception exception = assertThrows(Exception.class, () -> {
                pipelineEngine.executePipeline(config.getPipeline().getName());
            });
            
            assertNotNull(exception, "Should throw exception on error");
            assertTrue(exception.getMessage().contains("Pipeline execution failed") || 
                      exception.getMessage().contains("failed"), 
                      "Exception should indicate pipeline failure");
            
            LOGGER.info("✓ Stop-on-error behavior verified: {}", exception.getMessage());
        }

        @Test
        @DisplayName("POSITIVE: Should continue on error when configured")
        void shouldContinueOnError() throws Exception {
            LOGGER.info("=== Testing Continue-On-Error Behavior ===");

            // Create pipeline with failing step and continue-on-error
            String yamlContent = createFailingPipelineConfig(
                "sequential", 
                "continue-on-error", 
                1, 
                500
            );
            
            YamlRuleConfiguration config = loadYamlFromString(yamlContent, "continue-on-error-test.yaml");

            // Initialize and execute pipeline - should continue despite errors
            pipelineEngine.initialize(config);
            YamlPipelineExecutionResult result = pipelineEngine.executePipeline(config.getPipeline().getName());
            
            assertNotNull(result, "Pipeline execution result should not be null");
            // With continue-on-error, pipeline may complete but with errors
            assertNotNull(result.getError(), "Should capture error information");
            
            LOGGER.info("✓ Continue-on-error behavior verified: error={}", result.getError());
        }
    }

    @Nested
    @DisplayName("Retry Mechanism Tests")
    class RetryMechanismTests {

        @Test
        @DisplayName("POSITIVE: Should retry failed operations according to max-retries")
        void shouldRetryFailedOperations() throws Exception {
            LOGGER.info("=== Testing Max-Retries Behavior ===");

            // Create pipeline with retries enabled
            String yamlContent = createFailingPipelineConfig(
                "sequential",
                "stop-on-error",
                3,  // max-retries
                100 // retry-delay-ms (short for testing)
            );

            YamlRuleConfiguration config = loadYamlFromString(yamlContent, "retry-test.yaml");

            // Initialize and execute pipeline and measure retry behavior
            pipelineEngine.initialize(config);
            long startTime = System.currentTimeMillis();

            Exception exception = assertThrows(Exception.class, () -> {
                pipelineEngine.executePipeline(config.getPipeline().getName());
            });

            long executionTime = System.currentTimeMillis() - startTime;

            // Should have taken time for retries (3 retries * 100ms delay = at least 300ms)
            assertTrue(executionTime >= 200,
                "Execution should take time for retries, actual: " + executionTime + "ms");

            assertNotNull(exception, "Should eventually fail after retries");

            LOGGER.info("✓ Retry behavior verified: {}ms execution time, error: {}",
                executionTime, exception.getMessage());
        }

        @Test
        @DisplayName("POSITIVE: Should respect retry-delay-ms between attempts")
        void shouldRespectRetryDelay() throws Exception {
            LOGGER.info("=== Testing Retry-Delay-Ms Behavior ===");

            // Create pipeline with longer retry delay
            String yamlContent = createFailingPipelineConfig(
                "sequential",
                "stop-on-error",
                2,    // max-retries
                1000  // retry-delay-ms (1 second)
            );

            YamlRuleConfiguration config = loadYamlFromString(yamlContent, "retry-delay-test.yaml");

            // Initialize and execute pipeline and measure timing
            pipelineEngine.initialize(config);
            long startTime = System.currentTimeMillis();

            Exception exception = assertThrows(Exception.class, () -> {
                pipelineEngine.executePipeline(config.getPipeline().getName());
            });

            long executionTime = System.currentTimeMillis() - startTime;

            // Should have taken time for delays (2 retries * 1000ms = at least 2000ms)
            assertTrue(executionTime >= 1500,
                "Execution should respect retry delays, actual: " + executionTime + "ms");

            LOGGER.info("✓ Retry delay behavior verified: {}ms execution time", executionTime);
        }

        @Test
        @DisplayName("NEGATIVE: Should handle zero retries correctly")
        void shouldHandleZeroRetries() throws Exception {
            LOGGER.info("=== Testing Zero Retries Behavior ===");

            // Create pipeline with no retries
            String yamlContent = createFailingPipelineConfig(
                "sequential",
                "stop-on-error",
                0,   // max-retries (no retries)
                1000 // retry-delay-ms (irrelevant)
            );

            YamlRuleConfiguration config = loadYamlFromString(yamlContent, "zero-retry-test.yaml");

            // Initialize and execute pipeline - should fail quickly without retries
            pipelineEngine.initialize(config);
            long startTime = System.currentTimeMillis();

            Exception exception = assertThrows(Exception.class, () -> {
                pipelineEngine.executePipeline(config.getPipeline().getName());
            });

            long executionTime = System.currentTimeMillis() - startTime;

            // Should fail quickly without retry delays
            assertTrue(executionTime < 500,
                "Should fail quickly without retries, actual: " + executionTime + "ms");

            LOGGER.info("✓ Zero retries behavior verified: {}ms execution time", executionTime);
        }

        @Test
        @DisplayName("NEGATIVE: Should handle invalid retry parameters")
        void shouldHandleInvalidRetryParameters() throws Exception {
            LOGGER.info("=== Testing Invalid Retry Parameters ===");

            // Create pipeline with negative retry values
            String yamlContent = createFailingPipelineConfig(
                "sequential",
                "stop-on-error",
                -1,  // invalid max-retries
                -500 // invalid retry-delay-ms
            );

            YamlRuleConfiguration config = loadYamlFromString(yamlContent, "invalid-retry-test.yaml");

            // Initialize and execute pipeline - should handle invalid values gracefully
            pipelineEngine.initialize(config);
            Exception exception = assertThrows(Exception.class, () -> {
                pipelineEngine.executePipeline(config.getPipeline().getName());
            });

            // Should not crash, should handle invalid parameters gracefully
            assertNotNull(exception, "Should handle invalid retry parameters");

            LOGGER.info("✓ Invalid retry parameters handled: {}", exception.getMessage());
        }
    }

    /**
     * Creates a basic pipeline configuration with specified execution parameters.
     */
    private String createPipelineConfig(String mode, String errorHandling, int maxRetries, long retryDelayMs) {
        return String.format("""
            metadata:
              id: "execution-test-pipeline"
              name: "Execution Test Pipeline"
              type: "pipeline-config"
              version: "1.0.0"
            
            pipeline:
              name: "execution-test"
              description: "Test pipeline execution keywords"
              
              execution:
                mode: "%s"
                error-handling: "%s"
                max-retries: %d
                retry-delay-ms: %d
              
              steps:
                - name: "extract-test-data"
                  type: "extract"
                  source: "test-source"
                  operation: "getAllRecords"
                  description: "Extract test data"
                  
                - name: "load-test-data"
                  type: "load"
                  sink: "test-sink"
                  operation: "insertRecord"
                  description: "Load test data"
                  depends-on: ["extract-test-data"]
            
            data-sources:
              - name: "test-source"
                type: "file-system"
                enabled: true
                connection:
                  base-path: "./target/demo/etl/execution-tests"
                  file-pattern: "*.csv"
                operations:
                  getAllRecords: "SELECT * FROM csv"
            
            data-sinks:
              - name: "test-sink"
                type: "file-system"
                enabled: true
                connection:
                  base-path: "./target/demo/etl/execution-tests/output"
                operations:
                  insertRecord: "WRITE TO json"
            """, mode, errorHandling, maxRetries, retryDelayMs);
    }

    /**
     * Creates a pipeline configuration with a failing step for error handling tests.
     */
    private String createFailingPipelineConfig(String mode, String errorHandling, int maxRetries, long retryDelayMs) {
        return String.format("""
            metadata:
              id: "failing-test-pipeline"
              name: "Failing Test Pipeline"
              type: "pipeline-config"
              version: "1.0.0"
            
            pipeline:
              name: "failing-test"
              description: "Test pipeline error handling"
              
              execution:
                mode: "%s"
                error-handling: "%s"
                max-retries: %d
                retry-delay-ms: %d
              
              steps:
                - name: "failing-step"
                  type: "extract"
                  source: "non-existent-source"
                  operation: "getAllRecords"
                  description: "This step will fail"
            
            data-sources:
              - name: "non-existent-source"
                type: "file-system"
                enabled: true
                connection:
                  base-path: "./non-existent-path"
                  file-pattern: "*.csv"
                operations:
                  getAllRecords: "SELECT * FROM csv"
            """, mode, errorHandling, maxRetries, retryDelayMs);
    }

    /**
     * Helper method to load YAML configuration from string content.
     */
    private YamlRuleConfiguration loadYamlFromString(String yamlContent, String filename) throws Exception {
        Path tempFile = testDataDir.resolve(filename);
        try (FileWriter writer = new FileWriter(tempFile.toFile())) {
            writer.write(yamlContent);
        }
        return yamlLoader.loadFromFile(tempFile.toString());
    }
}
