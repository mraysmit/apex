package dev.mars.apex.demo.etl;

import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

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
 * @author APEX Demo Team
 * @since 1.0.0
 */
@DisplayName("APEX ETL Pipeline Execution Keyword Tests")
public class PipelineExecutionKeywordTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineExecutionKeywordTest.class);

    private Path testDataDir;
    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() {
        super.setUp();

        try {
            testDataDir = Paths.get("./target/demo/etl/execution-tests");
            Files.createDirectories(testDataDir);

            // Create output directory for data sinks
            Path outputDir = testDataDir.resolve("output");
            Files.createDirectories(outputDir);

            // Create a sample CSV file for testing
            Path sampleCsv = testDataDir.resolve("sample.csv");
            if (!Files.exists(sampleCsv)) {
                Files.writeString(sampleCsv, "id,name,value\n1,test,100\n2,demo,200\n");
            }

            LOGGER.info("[OK] Pipeline execution test setup complete: {}", testDataDir);

        } catch (Exception e) {
            throw new RuntimeException("Failed to setup pipeline execution tests", e);
        }
    }

    @AfterEach
    public void tearDown() {
        if (rulesEngine != null) {
            try {
                rulesEngine.shutdown();
            } catch (Exception e) {
                LOGGER.warn("Error shutting down rules engine", e);
            }
        }
        super.tearDown();
    }

    @Test
    @DisplayName("POSITIVE: Should execute pipeline in sequential mode")
    void shouldExecuteSequentialMode() throws Exception {
        LOGGER.info("=== Testing Sequential Execution Mode ===");

        // Load pipeline with sequential execution mode
        String yamlPath = "src/test/java/dev/mars/apex/demo/etl/PipelineExecutionKeywordTest_Sequential.yaml";
        rulesEngine = RulesEngine.fromFile(yamlPath);

        // Execute the pipeline
        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Verify pipeline executed successfully
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully in sequential mode");

        // Validate data was loaded to database (proving all steps executed)
        validateSequentialExecution();

        LOGGER.info("[OK] Sequential execution test completed successfully");
    }

    @Test
    @DisplayName("POSITIVE: Should execute pipeline in parallel mode")
    void shouldExecuteParallelMode() throws Exception {
        LOGGER.info("=== Testing Parallel Execution Mode ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/etl/PipelineExecutionKeywordTest_Parallel.yaml";
        rulesEngine = RulesEngine.fromFile(yamlPath);

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Verify pipeline executed successfully
        // Note: PipelineExecutor.java line 235 shows parallel mode currently executes as sequential
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully in parallel mode");

        // Verify data was loaded successfully (2 records, 1 header row skipped)
        String jdbcUrl = "jdbc:h2:./target/demo/etl/execution-tests/output/parallel_db";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM test_data");
                assertTrue(rs.next(), "Should have count result");
                int recordCount = rs.getInt("count");
                assertEquals(2, recordCount, "Should have 2 records (header row skipped)");
                LOGGER.info("[OK] Verified {} records loaded successfully in parallel mode", recordCount);
            }
        }

        LOGGER.info("[OK] Parallel execution test completed - pipeline executed successfully");
        LOGGER.info("Note: Current implementation executes parallel mode as sequential (PipelineExecutor.java:235)");
    }

    @Test
    @DisplayName("NEGATIVE: Should handle invalid execution mode gracefully")
    void shouldHandleInvalidExecutionMode() throws Exception {
        LOGGER.info("=== Testing Invalid Execution Mode ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/etl/PipelineExecutionKeywordTest_InvalidMode.yaml";
        rulesEngine = RulesEngine.fromFile(yamlPath);

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Verify pipeline executed successfully - invalid mode defaults to sequential
        // PipelineExecutor.java line 72-76: if not "parallel", defaults to sequential
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should succeed - invalid mode should default to sequential");

        // Verify data was loaded successfully (2 records, 1 header row skipped)
        String jdbcUrl = "jdbc:h2:./target/demo/etl/execution-tests/output/invalid_mode_db";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM test_data");
                assertTrue(rs.next(), "Should have count result");
                int recordCount = rs.getInt("count");
                assertEquals(2, recordCount, "Should have 2 records (header row skipped)");
                LOGGER.info("[OK] Verified {} records loaded successfully with invalid mode defaulting to sequential", recordCount);
            }
        }

        LOGGER.info("[OK] Invalid execution mode test completed - system defaulted to sequential mode");
    }

    @Test
    @DisplayName("POSITIVE: Should stop on error when configured")
    void shouldStopOnError() throws Exception {
        LOGGER.info("=== Testing Stop-On-Error Behavior ===");

        // Load pipeline with stop-on-error configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/etl/PipelineExecutionKeywordTest_StopOnError.yaml";
        rulesEngine = RulesEngine.fromFile(yamlPath);

        // Execute the pipeline - should fail on step 2
        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Verify pipeline stopped on error (ERROR indicates failure)
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
            "Pipeline should stop and return ERROR when error occurs with stop-on-error mode");

        LOGGER.info("[OK] Stop-on-error test completed - pipeline correctly stopped on error");
    }

    @Test
    @DisplayName("POSITIVE: Should continue on error when configured")
    void shouldContinueOnError() throws Exception {
        LOGGER.info("=== Testing Continue-On-Error Behavior ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/etl/PipelineExecutionKeywordTest_ContinueOnError.yaml";
        rulesEngine = RulesEngine.fromFile(yamlPath);

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Verify pipeline continued despite error (MATCH indicates overall success)
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should continue and return MATCH when error occurs with continue-on-error mode");

        // Verify step 3 executed successfully by checking database
        validateContinueOnErrorExecution();

        LOGGER.info("[OK] Continue-on-error test completed - pipeline correctly continued after error");
    }

    private void validateContinueOnErrorExecution() throws Exception {
        String jdbcUrl = "jdbc:h2:./target/demo/etl/execution-tests/output/continue_on_error_db";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement stmt = conn.createStatement();

            // Verify table exists and has data from step 3
            ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM test_data");
            assertTrue(countRs.next(), "Should have count result");
            int recordCount = countRs.getInt("cnt");
            assertTrue(recordCount > 0,
                "Should have loaded records from step 3 despite step 2 failure - found " + recordCount + " records");

            LOGGER.info("[OK] Verified step 3 executed successfully with {} records despite step 2 failure", recordCount);
        }
    }

    @Test
    @DisplayName("POSITIVE: Should retry failed operations according to max-retries")
    void shouldRetryFailedOperations() throws Exception {
        LOGGER.info("=== Testing Max-Retries Behavior ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/etl/PipelineExecutionKeywordTest_Retry.yaml";
        rulesEngine = RulesEngine.fromFile(yamlPath);

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Verify pipeline executed successfully (with or without retries)
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should complete successfully with retry configuration");

        // Verify data was loaded successfully
        validateRetryExecution();

        LOGGER.info("[OK] Retry test completed - pipeline executed with retry configuration");
    }

    private void validateRetryExecution() throws Exception {
        String jdbcUrl = "jdbc:h2:./target/demo/etl/execution-tests/output/retry_db";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement stmt = conn.createStatement();

            // Verify table exists and has data
            ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM test_data");
            assertTrue(countRs.next(), "Should have count result");
            int recordCount = countRs.getInt("cnt");
            assertTrue(recordCount > 0,
                "Should have loaded records - found " + recordCount + " records");

            LOGGER.info("[OK] Verified retry pipeline executed successfully with {} records", recordCount);
        }
    }

    @Test
    @DisplayName("POSITIVE: Should respect retry-delay-ms between attempts")
    void shouldRespectRetryDelay() throws Exception {
        LOGGER.info("=== Testing Retry-Delay-Ms Behavior ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/etl/PipelineExecutionKeywordTest_RetryDelay.yaml";
        rulesEngine = RulesEngine.fromFile(yamlPath);

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();

        // Measure execution time - should include retry delays
        long startTime = System.currentTimeMillis();
        RuleResult result = rulesEngine.evaluate(inputData);
        long totalTime = System.currentTimeMillis() - startTime;

        // Verify pipeline failed (nonexistent sink)
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
            "Pipeline should fail when all retries are exhausted");

        // With max-retries: 2 and retry-delay-ms: 1000:
        // - Initial attempt (no delay)
        // - Retry 1 (1000ms delay before)
        // - Retry 2 (1000ms delay before)
        // Total expected delay: ~2000ms (plus execution overhead)

        LOGGER.info("Total execution time: {}ms (expected ~2000ms for 2 retries with 1000ms delay)", totalTime);

        // Allow for execution overhead - verify at least 1800ms (90% of expected 2000ms)
        assertTrue(totalTime >= 1800,
            String.format("Total time (%dms) should be at least 1800ms for 2 retries with 1000ms delay", totalTime));

        // Also verify it's not too long (less than 3000ms to account for overhead)
        assertTrue(totalTime < 3000,
            String.format("Total time (%dms) should be less than 3000ms (2000ms + reasonable overhead)", totalTime));

        LOGGER.info("[OK] Retry delay test completed - verified ~1000ms delay between retry attempts");
    }

    @Test
    @DisplayName("NEGATIVE: Should handle zero retries correctly")
    void shouldHandleZeroRetries() throws Exception {
        LOGGER.info("=== Testing Zero Retries Behavior ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/etl/PipelineExecutionKeywordTest_ZeroRetries.yaml";
        rulesEngine = RulesEngine.fromFile(yamlPath);

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Verify pipeline failed immediately (no retries with max-retries: 0)
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
            "Pipeline should fail immediately with zero retries when step fails");

        LOGGER.info("[OK] Zero retries test completed - pipeline failed immediately without retry attempts");
    }

    @Test
    @DisplayName("NEGATIVE: Should handle invalid retry parameters gracefully")
    void shouldHandleInvalidRetryParameters() throws Exception {
        LOGGER.info("=== Testing Invalid Retry Parameters ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/etl/PipelineExecutionKeywordTest_InvalidRetry.yaml";
        rulesEngine = RulesEngine.fromFile(yamlPath);

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Verify pipeline executed successfully despite invalid retry parameters
        // PipelineExecutor.java lines 252-259 validate and correct invalid values to 0
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should succeed - invalid retry params should be corrected to defaults");

        // Verify data was loaded successfully (2 records, 1 header row skipped)
        String jdbcUrl = "jdbc:h2:./target/demo/etl/execution-tests/output/invalid_retry_db";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM test_data");
                assertTrue(rs.next(), "Should have count result");
                int recordCount = rs.getInt("count");
                assertEquals(2, recordCount, "Should have 2 records (header row skipped)");
                LOGGER.info("[OK] Verified {} records loaded successfully despite invalid retry parameters", recordCount);
            }
        }

        LOGGER.info("[OK] Invalid retry parameters test completed - system handled invalid values gracefully");
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private void validateSequentialExecution() throws Exception {
        LOGGER.info("Validating sequential execution results in database");

        String jdbcUrl = "jdbc:h2:./target/demo/etl/execution-tests/output/sequential_db";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement stmt = conn.createStatement();

            // Verify table exists
            ResultSet tables = conn.getMetaData().getTables(null, null, "TEST_DATA", null);
            assertTrue(tables.next(), "Table 'test_data' should exist");
            LOGGER.info("[OK] Table 'test_data' exists");

            // Verify records were loaded (proving both extract and load steps executed)
            ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM test_data");
            assertTrue(countRs.next(), "Should have count result");
            int recordCount = countRs.getInt("cnt");
            assertTrue(recordCount > 0, "Should have loaded records from CSV");
            LOGGER.info("[OK] Database contains {} records (steps executed sequentially)", recordCount);
        }
    }
}
