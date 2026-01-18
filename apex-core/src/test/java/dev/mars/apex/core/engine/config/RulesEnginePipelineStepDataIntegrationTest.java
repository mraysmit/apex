package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for RulesEngine pipeline execution with step data capture.
 * 
 * This test suite validates that pipeline step data and metrics are correctly captured
 * when executing real pipelines through RulesEngine.evaluate().
 * 
 * TEST COVERAGE:
 * - Real pipeline execution with data capture (8 tests)
 * - Simple extract pipelines
 * - Multi-step pipelines (Extract → Transform → Load)
 * - Partial failure scenarios
 * - Null data handling
 * - Database and file extracts
 * - Serialization preservation
 * - Execution path access patterns
 * 
 * @author APEX Core Team
 * @since 2026-01-11
 * @version 1.0.0
 */
@DisplayName("RulesEngine Pipeline Step Data Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RulesEnginePipelineStepDataIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(RulesEnginePipelineStepDataIntegrationTest.class);
    private static final String TEST_YAML_BASE_PATH = "src/test/resources/pipeline-step-data/";

    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() throws Exception {
        logger.info("=== Setting up test ===");

        // Set up H2 databases for tests
        setupSimpleExtractDatabase();
        setupMultiStepDatabase();
        setupPartialFailureDatabase();
        setupNullDataDatabase();
        setupDatabaseExtractDatabase();
    }

    /**
     * Set up H2 database for simple extract test.
     */
    private void setupSimpleExtractDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:simple_extract_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS test_records (" +
                "id INTEGER PRIMARY KEY, " +
                "name VARCHAR(255), " +
                "amount INTEGER, " +
                "status VARCHAR(50))");

            // Clear existing data
            stmt.execute("DELETE FROM test_records");

            stmt.execute("INSERT INTO test_records (id, name, amount, status) VALUES " +
                "(1, 'Record 1', 100, 'active'), " +
                "(2, 'Record 2', 200, 'active'), " +
                "(3, 'Record 3', 300, 'inactive'), " +
                "(4, 'Record 4', 400, 'active'), " +
                "(5, 'Record 5', 500, 'active')");

            logger.info("✓ Simple extract database initialized");
        }
    }

    /**
     * Set up H2 database for multi-step test.
     */
    private void setupMultiStepDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:multi_step_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
             Statement stmt = conn.createStatement()) {

            // Source table
            stmt.execute("CREATE TABLE IF NOT EXISTS source_items (" +
                "id INTEGER PRIMARY KEY, " +
                "name VARCHAR(255), " +
                "quantity INTEGER, " +
                "price DECIMAL(10,2))");

            // Clear existing data
            stmt.execute("DELETE FROM source_items");

            stmt.execute("INSERT INTO source_items (id, name, quantity, price) VALUES " +
                "(1, 'Item A', 10, 25.50), " +
                "(2, 'Item B', 5, 50.00), " +
                "(3, 'Item C', 15, 10.00)");

            // Target table
            stmt.execute("CREATE TABLE IF NOT EXISTS target_items (" +
                "id INTEGER PRIMARY KEY, " +
                "name VARCHAR(255), " +
                "quantity INTEGER, " +
                "price DECIMAL(10,2), " +
                "total_value DECIMAL(10,2), " +
                "processed_at VARCHAR(255))");

            // Clear existing data
            stmt.execute("DELETE FROM target_items");

            logger.info("✓ Multi-step database initialized");
        }
    }

    /**
     * Set up H2 database for partial failure test.
     */
    private void setupPartialFailureDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:partial_failure_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS mixed_records (" +
                "id INTEGER PRIMARY KEY, " +
                "name VARCHAR(255), " +
                "amount INTEGER, " +
                "status VARCHAR(50))");

            // Clear existing data
            stmt.execute("DELETE FROM mixed_records");

            stmt.execute("INSERT INTO mixed_records (id, name, amount, status) VALUES " +
                "(1, 'Valid Record 1', 100, 'valid'), " +
                "(2, 'Valid Record 2', 200, 'valid'), " +
                "(3, 'Invalid Record', null, 'invalid')");

            logger.info("✓ Partial failure database initialized");
        }
    }

    /**
     * Set up H2 database for null data test.
     */
    private void setupNullDataDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:null_data_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS empty_records (" +
                "id INTEGER PRIMARY KEY, " +
                "name VARCHAR(255))");

            // Clear existing data
            stmt.execute("DELETE FROM empty_records");

            // No data inserted - table is empty

            logger.info("✓ Null data database initialized");
        }
    }

    /**
     * Set up H2 database for database extract test.
     */
    private void setupDatabaseExtractDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:database_extract_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                "id INTEGER PRIMARY KEY, " +
                "name VARCHAR(255), " +
                "price DECIMAL(10,2), " +
                "category VARCHAR(100))");

            // Clear existing data
            stmt.execute("DELETE FROM products");

            stmt.execute("INSERT INTO products (id, name, price, category) VALUES " +
                "(1, 'Product A', 99.99, 'Electronics'), " +
                "(2, 'Product B', 149.50, 'Electronics'), " +
                "(3, 'Product C', 29.99, 'Books'), " +
                "(4, 'Product D', 199.00, 'Electronics')");

            logger.info("✓ Database extract database initialized");
        }
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
    // TEST 1: Simple Extract Pipeline
    // ========================================================================
    
    @Test
    @Order(1)
    @DisplayName("Should capture step data from simple extract pipeline")
    public void shouldCaptureStepDataFromSimpleExtractPipeline() throws Exception {
        logger.info("=== Test 1: Simple Extract Pipeline ===");
        
        // Given: A pipeline with a simple extract step
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/core/engine/config/simple-extract-pipeline.yaml");
        Map<String, Object> inputData = new HashMap<>();
        
        // When: Execute the pipeline
        RuleResult result = rulesEngine.evaluate(inputData);

        // Then: Verify result is successful
        assertNotNull(result, "Result should not be null");
        logger.info("Result success: {}, message: {}", result.isSuccess(), result.getMessage());
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        // And: Verify execution path contains pipeline steps
        List<ExecutionStep> executionPath = result.getExecutionPath();
        assertNotNull(executionPath, "Execution path should not be null");
        assertFalse(executionPath.isEmpty(), "Execution path should contain steps");

        // And: Find the extract step
        ExecutionStep extractStep = executionPath.stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .filter(step -> step.getName().contains("extract"))
            .findFirst()
            .orElse(null);

        assertNotNull(extractStep, "Extract step should be in execution path");
        logger.info("Extract step status: {}, message: {}", extractStep.getStatus(), extractStep.getMessage());
        assertEquals("SUCCESS", extractStep.getStatus(), "Extract step should be successful");
        
        // And: Verify step data is captured
        assertTrue(extractStep.hasStepData(), "Extract step should have data");
        Object stepData = extractStep.getStepData();
        assertNotNull(stepData, "Step data should not be null");
        
        // And: Verify metrics are captured
        assertNotNull(extractStep.getRecordsProcessed(), "Records processed should be captured");
        assertNotNull(extractStep.getRecordsFailed(), "Records failed should be captured");
        assertTrue(extractStep.getRecordsProcessed() >= 0, "Records processed should be non-negative");
        assertTrue(extractStep.getRecordsFailed() >= 0, "Records failed should be non-negative");
        
        // And: Verify success rate is calculated
        double successRate = extractStep.getSuccessRate();
        assertTrue(successRate >= 0.0 && successRate <= 100.0, "Success rate should be between 0 and 100");
        
        logger.info("✓ Extract step data captured: {} records processed, {} failed, {}% success rate",
            extractStep.getRecordsProcessed(), extractStep.getRecordsFailed(), successRate);
    }

    // ========================================================================
    // TEST 2: Multi-Step Pipeline
    // ========================================================================
    
    @Test
    @Order(2)
    @DisplayName("Should capture step data from multi-step pipeline")
    public void shouldCaptureStepDataFromMultiStepPipeline() throws Exception {
        logger.info("=== Test 2: Multi-Step Pipeline ===");
        
        // Given: A pipeline with Extract → Transform → Load steps
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/core/engine/config/multi-step-pipeline.yaml");
        Map<String, Object> inputData = new HashMap<>();
        
        // When: Execute the pipeline
        RuleResult result = rulesEngine.evaluate(inputData);

        // Then: Verify result is successful
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Multi-step pipeline should execute successfully");
        
        // And: Verify execution path contains all pipeline steps
        List<ExecutionStep> executionPath = result.getExecutionPath();
        assertNotNull(executionPath, "Execution path should not be null");
        
        List<ExecutionStep> pipelineSteps = executionPath.stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();
        
        assertTrue(pipelineSteps.size() >= 2, "Should have at least 2 pipeline steps");
        
        logger.info("Found {} pipeline steps in execution path", pipelineSteps.size());
        
        // And: Verify each step has data and metrics
        for (ExecutionStep step : pipelineSteps) {
            logger.info("  Step: {} - Status: {}, Has Data: {}, Records: {}/{}",
                step.getName(), step.getStatus(), step.hasStepData(),
                step.getRecordsProcessed(), step.getRecordsFailed());
            
            assertEquals("SUCCESS", step.getStatus(), "Step " + step.getName() + " should be successful");
            assertNotNull(step.getRecordsProcessed(), "Step should have records processed metric");
            assertNotNull(step.getRecordsFailed(), "Step should have records failed metric");
        }
        
        logger.info("✓ All pipeline steps captured with data and metrics");
    }

    // ========================================================================
    // TEST 3: Pipeline with Partial Failures
    // ========================================================================

    @Test
    @Order(3)
    @DisplayName("Should capture metrics from pipeline with partial failures")
    public void shouldCaptureMetricsFromPipelineWithPartialFailures() throws Exception {
        logger.info("=== Test 3: Pipeline with Partial Failures ===");

        // Given: A pipeline that processes some records successfully and fails others
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/core/engine/config/partial-failure-pipeline.yaml");
        Map<String, Object> inputData = new HashMap<>();

        // When: Execute the pipeline
        RuleResult result = rulesEngine.evaluate(inputData);

        // Then: Verify result (may be success or partial success)
        assertNotNull(result, "Result should not be null");

        // And: Verify execution path contains pipeline steps
        List<ExecutionStep> executionPath = result.getExecutionPath();
        assertNotNull(executionPath, "Execution path should not be null");

        List<ExecutionStep> pipelineSteps = executionPath.stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertFalse(pipelineSteps.isEmpty(), "Should have pipeline steps");

        // And: Verify metrics are captured for all steps
        for (ExecutionStep step : pipelineSteps) {
            assertNotNull(step.getRecordsProcessed(), "Records processed should be captured");
            assertNotNull(step.getRecordsFailed(), "Records failed should be captured");

            double successRate = step.getSuccessRate();
            assertTrue(successRate >= 0.0 && successRate <= 100.0,
                "Success rate should be between 0 and 100");

            logger.info("  Step: {} - Processed: {}, Failed: {}, Success Rate: {}%",
                step.getName(), step.getRecordsProcessed(), step.getRecordsFailed(),
                String.format("%.1f", successRate));
        }

        logger.info("✓ Partial failure metrics captured correctly");
    }

    // ========================================================================
    // TEST 4: Null Data Handling
    // ========================================================================

    @Test
    @Order(4)
    @DisplayName("Should handle null data gracefully")
    public void shouldHandleNullDataGracefully() throws Exception {
        logger.info("=== Test 4: Null Data Handling ===");

        // Given: A pipeline step that might return null data (e.g., load step)
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/core/engine/config/null-data-pipeline.yaml");
        Map<String, Object> inputData = new HashMap<>();

        // When: Execute the pipeline
        RuleResult result = rulesEngine.evaluate(inputData);

        // Then: Verify no exceptions thrown
        assertNotNull(result, "Result should not be null");

        // And: Verify execution path is accessible
        List<ExecutionStep> executionPath = result.getExecutionPath();
        assertNotNull(executionPath, "Execution path should not be null");

        // And: Verify steps can have null data without errors
        for (ExecutionStep step : executionPath) {
            // Should not throw NPE
            boolean hasData = step.hasStepData();
            Object data = step.getStepData();

            if (!hasData) {
                assertNull(data, "If hasStepData is false, data should be null");
                logger.info("  Step: {} - No data (expected for some step types)", step.getName());
            }
        }

        logger.info("✓ Null data handled gracefully without errors");
    }

    // ========================================================================
    // TEST 5: Database Extract
    // ========================================================================

    @Test
    @Order(5)
    @DisplayName("Should capture data from database extract")
    public void shouldCaptureDataFromDatabaseExtract() throws Exception {
        logger.info("=== Test 5: Database Extract ===");

        // Given: A pipeline with database extract step
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/core/engine/config/database-extract-pipeline.yaml");
        Map<String, Object> inputData = new HashMap<>();

        // When: Execute the pipeline
        RuleResult result = rulesEngine.evaluate(inputData);

        // Then: Verify result is successful
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Database extract should execute successfully");

        // And: Find the database extract step
        List<ExecutionStep> pipelineSteps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertFalse(pipelineSteps.isEmpty(), "Should have pipeline steps");

        ExecutionStep extractStep = pipelineSteps.get(0);

        // And: Verify data is captured as List<Map>
        assertTrue(extractStep.hasStepData(), "Database extract should have data");
        Object stepData = extractStep.getStepData();
        assertNotNull(stepData, "Step data should not be null");
        assertTrue(stepData instanceof List, "Database extract data should be a List");

        @SuppressWarnings("unchecked")
        List<?> records = (List<?>) stepData;

        // And: Verify metrics match the data
        assertEquals(records.size(), extractStep.getRecordsProcessed(),
            "Records processed should match data size");

        logger.info("✓ Database extract captured {} records", records.size());
    }

    // ========================================================================
    // TEST 6: File Extract
    // ========================================================================

    @Test
    @Order(6)
    @DisplayName("Should capture data from file extract")
    public void shouldCaptureDataFromFileExtract() throws Exception {
        logger.info("=== Test 6: File Extract ===");

        // Given: A pipeline with CSV/JSON file extract step
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/core/engine/config/file-extract-pipeline.yaml");
        Map<String, Object> inputData = new HashMap<>();

        // When: Execute the pipeline
        RuleResult result = rulesEngine.evaluate(inputData);

        // Then: Verify result is successful
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "File extract should execute successfully");

        // And: Find the file extract step
        List<ExecutionStep> pipelineSteps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertFalse(pipelineSteps.isEmpty(), "Should have pipeline steps");

        ExecutionStep extractStep = pipelineSteps.get(0);

        // And: Verify data is captured
        assertTrue(extractStep.hasStepData(), "File extract should have data");
        Object stepData = extractStep.getStepData();
        assertNotNull(stepData, "Step data should not be null");
        assertTrue(stepData instanceof List, "File extract data should be a List");

        @SuppressWarnings("unchecked")
        List<?> records = (List<?>) stepData;

        // And: Verify metrics are accurate
        assertNotNull(extractStep.getRecordsProcessed(), "Records processed should be set");
        assertTrue(extractStep.getRecordsProcessed() > 0, "Should have processed some records");
        assertEquals(records.size(), extractStep.getRecordsProcessed(),
            "Records processed should match file record count");

        logger.info("✓ File extract captured {} records from file", records.size());
    }

    // ========================================================================
    // TEST 7: Serialization Preservation
    // ========================================================================

    @Test
    @Order(7)
    @DisplayName("Should preserve data through RuleResult serialization")
    public void shouldPreserveDataThroughRuleResultSerialization() throws Exception {
        logger.info("=== Test 7: Serialization Preservation ===");

        // Given: A pipeline execution result with step data
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/core/engine/config/simple-extract-pipeline.yaml");
        Map<String, Object> inputData = new HashMap<>();

        RuleResult originalResult = rulesEngine.evaluate(inputData);
        assertNotNull(originalResult, "Original result should not be null");

        List<ExecutionStep> originalPath = originalResult.getExecutionPath();
        assertNotNull(originalPath, "Original execution path should not be null");
        assertFalse(originalPath.isEmpty(), "Original execution path should have steps");

        // When: Serialize and deserialize the RuleResult
        // Note: This tests Java serialization - in production you might use JSON
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
        oos.writeObject(originalResult);
        oos.close();

        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais);
        RuleResult deserializedResult = (RuleResult) ois.readObject();
        ois.close();

        // Then: Verify deserialized result has all data
        assertNotNull(deserializedResult, "Deserialized result should not be null");
        assertEquals(originalResult.isSuccess(), deserializedResult.isSuccess(),
            "Success status should be preserved");

        List<ExecutionStep> deserializedPath = deserializedResult.getExecutionPath();
        assertNotNull(deserializedPath, "Deserialized execution path should not be null");
        assertEquals(originalPath.size(), deserializedPath.size(),
            "Execution path size should be preserved");

        // And: Verify step data and metrics are preserved
        for (int i = 0; i < originalPath.size(); i++) {
            ExecutionStep originalStep = originalPath.get(i);
            ExecutionStep deserializedStep = deserializedPath.get(i);

            assertEquals(originalStep.getName(), deserializedStep.getName(),
                "Step name should be preserved");
            assertEquals(originalStep.getStatus(), deserializedStep.getStatus(),
                "Step status should be preserved");
            assertEquals(originalStep.hasStepData(), deserializedStep.hasStepData(),
                "Step data presence should be preserved");

            if (originalStep.getRecordsProcessed() != null) {
                assertEquals(originalStep.getRecordsProcessed(), deserializedStep.getRecordsProcessed(),
                    "Records processed should be preserved");
            }

            if (originalStep.getRecordsFailed() != null) {
                assertEquals(originalStep.getRecordsFailed(), deserializedStep.getRecordsFailed(),
                    "Records failed should be preserved");
            }
        }

        logger.info("✓ Step data and metrics preserved through serialization");
    }

    // ========================================================================
    // TEST 8: Execution Path Access Patterns
    // ========================================================================

    @Test
    @Order(8)
    @DisplayName("Should access step data from RuleResult execution path")
    public void shouldAccessStepDataFromRuleResultExecutionPath() throws Exception {
        logger.info("=== Test 8: Execution Path Access Patterns ===");

        // Given: A multi-step pipeline execution
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/core/engine/config/multi-step-pipeline.yaml");
        Map<String, Object> inputData = new HashMap<>();

        RuleResult result = rulesEngine.evaluate(inputData);
        assertNotNull(result, "Result should not be null");

        // When: Access execution path and filter for pipeline steps
        List<ExecutionStep> allSteps = result.getExecutionPath();
        List<ExecutionStep> pipelineSteps = allSteps.stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        // Then: Verify we can iterate and access data
        assertFalse(pipelineSteps.isEmpty(), "Should have pipeline steps");

        int totalRecordsProcessed = 0;
        int totalRecordsFailed = 0;
        int stepsWithData = 0;

        for (ExecutionStep step : pipelineSteps) {
            logger.info("  Processing step: {}", step.getName());

            // Access metrics
            if (step.getRecordsProcessed() != null) {
                totalRecordsProcessed += step.getRecordsProcessed();
            }
            if (step.getRecordsFailed() != null) {
                totalRecordsFailed += step.getRecordsFailed();
            }

            // Access data if available
            if (step.hasStepData()) {
                stepsWithData++;
                Object data = step.getStepData();
                assertNotNull(data, "Step data should not be null when hasStepData is true");
                logger.info("    - Has data: {} (type: {})",
                    data instanceof List ? ((List<?>) data).size() + " records" : "object",
                    data.getClass().getSimpleName());
            }

            // Calculate success rate
            double successRate = step.getSuccessRate();
            logger.info("    - Success rate: {}%", String.format("%.1f", successRate));
        }

        // And: Verify aggregated metrics
        logger.info("  Total records processed across all steps: {}", totalRecordsProcessed);
        logger.info("  Total records failed across all steps: {}", totalRecordsFailed);
        logger.info("  Steps with data: {}/{}", stepsWithData, pipelineSteps.size());

        assertTrue(totalRecordsProcessed >= 0, "Total records processed should be non-negative");
        assertTrue(totalRecordsFailed >= 0, "Total records failed should be non-negative");

        logger.info("✓ Execution path access patterns work correctly");
    }
}
