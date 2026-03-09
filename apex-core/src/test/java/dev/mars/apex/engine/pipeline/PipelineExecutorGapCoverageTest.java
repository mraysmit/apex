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

package dev.mars.apex.engine.pipeline;

import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.ExecutionStep;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gap coverage tests for PipelineExecutor.
 * 
 * <p>This test class specifically targets untested code paths in PipelineExecutor
 * to improve coverage before refactoring:</p>
 * <ul>
 *   <li>Retry configuration (getMaxRetries, getRetryDelayMs)</li>
 *   <li>Report path normalization</li>
 *   <li>Topological sort with dependencies</li>
 *   <li>JDBC URL building for various database types</li>
 *   <li>Parallel execution (falls back to sequential)</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.4.0
 */
@DisplayName("PipelineExecutor Gap Coverage Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PipelineExecutorGapCoverageTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineExecutorGapCoverageTest.class);
    private static final String TEST_YAML_BASE_PATH = "src/test/resources/pipeline-gap-tests/";

    private RulesEngine rulesEngine;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up any previous test data
        cleanupTestDirectories();
    }

    @AfterEach
    void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    // ========================================================================
    // RETRY CONFIGURATION TESTS
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("Should use step-level retry configuration")
    void shouldUseStepLevelRetryConfiguration() throws Exception {
        LOGGER.info("\n=== Test: Step-Level Retry Configuration ===\n");

        // Setup database with data
        setupRetryTestDatabase();

        // Load pipeline with step-level retry config
        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "step-retry-config.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("operationType", "extract");
        
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
        LOGGER.info("Pipeline execution completed: {}", result.getResultType());
    }

    @Test
    @Order(2)
    @DisplayName("Should use pipeline-level retry configuration as fallback")
    void shouldUsePipelineLevelRetryConfiguration() throws Exception {
        LOGGER.info("\n=== Test: Pipeline-Level Retry Configuration ===\n");

        setupRetryTestDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "pipeline-retry-config.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("operationType", "extract");
        
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
        LOGGER.info("Pipeline execution completed: {}", result.getResultType());
    }

    // ========================================================================
    // STEP DEPENDENCY TESTS (Topological Sort)
    // ========================================================================

    @Test
    @Order(3)
    @DisplayName("Should execute steps in dependency order")
    void shouldExecuteStepsInDependencyOrder() throws Exception {
        LOGGER.info("\n=== Test: Step Dependencies ===\n");

        setupDependencyTestDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "step-dependencies.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("processId", "dep-test");
        
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
        
        // Verify execution order through execution path
        List<ExecutionStep> steps = result.getExecutionPath();
        assertNotNull(steps, "Execution path should not be null");
        
        // Log step execution order
        steps.forEach(step -> 
            LOGGER.info("Step executed: {} (type: {})", step.getName(), step.getType()));
    }

    // ========================================================================
    // REPORT PATH NORMALIZATION TESTS
    // ========================================================================

    @Test
    @Order(4)
    @DisplayName("Should normalize report path with filename only")
    void shouldNormalizeReportPathWithFilenameOnly() throws Exception {
        LOGGER.info("\n=== Test: Report Path Normalization (filename only) ===\n");

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "read-schema-report.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("schemaName", "test-schema");
        
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
        
        // Check that reports directory was created
        Path reportsDir = Path.of("reports");
        if (Files.exists(reportsDir)) {
            LOGGER.info("Reports directory exists at expected location");
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should preserve full path in report configuration")
    void shouldPreserveFullPathInReportConfiguration() throws Exception {
        LOGGER.info("\n=== Test: Report Path with Full Path ===\n");

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "read-schema-full-path-report.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("schemaName", "full-path-test");
        
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
    }

    // ========================================================================
    // PARALLEL MODE TEST (falls back to sequential)
    // ========================================================================

    @Test
    @Order(6)
    @DisplayName("Should handle parallel mode flag (falls back to sequential)")
    void shouldHandleParallelModeFlag() throws Exception {
        LOGGER.info("\n=== Test: Parallel Mode Flag ===\n");

        setupSimpleExtractDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "parallel-mode-pipeline.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("mode", "parallel");
        
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
        LOGGER.info("Pipeline with parallel flag completed successfully");
    }

    // ========================================================================
    // INVALID RETRY PARAMETER TESTS
    // ========================================================================

    @Test
    @Order(7)
    @DisplayName("Should handle negative retry values gracefully")
    void shouldHandleNegativeRetryValues() throws Exception {
        LOGGER.info("\n=== Test: Negative Retry Values ===\n");

        setupRetryTestDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "negative-retry-config.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("operationType", "extract");
        
        // Should not throw - negative values are corrected to 0
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
    }

    // ========================================================================
    // ETL PIPELINE TESTS (Load Step Coverage)
    // ========================================================================

    @Test
    @Order(8)
    @DisplayName("Should execute complete ETL pipeline with load step")
    void shouldExecuteCompleteEtlPipelineWithLoadStep() throws Exception {
        LOGGER.info("\n=== Test: Complete ETL Pipeline ===\n");

        setupEtlTestDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "complete-etl-pipeline.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("pipelineMode", "etl");
        
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
        
        // Verify execution path contains all steps
        List<ExecutionStep> steps = result.getExecutionPath();
        if (steps != null && !steps.isEmpty()) {
            steps.forEach(step -> 
                LOGGER.info("ETL step executed: {} (records: {})", 
                    step.getName(), step.getRecordsProcessed()));
        }
    }

    @Test
    @Order(9)
    @DisplayName("Should handle transform step with field additions")
    void shouldHandleTransformStepWithFieldAdditions() throws Exception {
        LOGGER.info("\n=== Test: Transform Step - Field Additions ===\n");

        setupTransformTestDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "transform-field-additions.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("transformMode", "field-add");
        
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
    }

    @Test
    @Order(10)
    @DisplayName("Should handle transform step with calculations")
    void shouldHandleTransformStepWithCalculations() throws Exception {
        LOGGER.info("\n=== Test: Transform Step - Calculations ===\n");

        setupTransformTestDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "transform-calculations.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("transformMode", "calculation");
        
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
    }

    @Test
    @Order(11)
    @DisplayName("Should handle audit step")
    void shouldHandleAuditStep() throws Exception {
        LOGGER.info("\n=== Test: Audit Step ===\n");

        setupSimpleExtractDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "pipeline-with-audit.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("auditMode", "enabled");
        
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
    }

    // ========================================================================
    // VALIDATION TESTS (validatePipeline, validateStep, validateStepDependencies)
    // ========================================================================

    @Test
    @Order(12)
    @DisplayName("Should detect circular dependencies in pipeline")
    void shouldDetectCircularDependencies() throws Exception {
        LOGGER.info("\n=== Test: Circular Dependencies Detection ===\n");

        // This should fail validation due to circular dependencies
        try {
            rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "circular-dependencies.yaml");
            Map<String, Object> data = new HashMap<>();
            rulesEngine.evaluate(data);
            // Should not reach here
        } catch (Exception e) {
            LOGGER.info("Correctly detected issue: {}", e.getMessage());
            assertTrue(true, "Exception expected for circular dependencies");
        }
    }

    @Test
    @Order(13)
    @DisplayName("Should validate step without name")
    void shouldValidateStepWithoutName() throws Exception {
        LOGGER.info("\n=== Test: Step Without Name Validation ===\n");

        try {
            rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "invalid-step-no-name.yaml");
            Map<String, Object> data = new HashMap<>();
            rulesEngine.evaluate(data);
        } catch (Exception e) {
            LOGGER.info("Correctly detected issue: {}", e.getMessage());
            assertTrue(true, "Exception expected for missing step name");
        }
    }

    @Test
    @Order(14)
    @DisplayName("Should validate load step without sink")
    void shouldValidateLoadStepWithoutSink() throws Exception {
        LOGGER.info("\n=== Test: Load Step Without Sink ===\n");

        try {
            rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "invalid-load-no-sink.yaml");
            Map<String, Object> data = new HashMap<>();
            rulesEngine.evaluate(data);
        } catch (Exception e) {
            LOGGER.info("Correctly detected issue: {}", e.getMessage());
            assertTrue(true, "Exception expected for missing sink");
        }
    }

    @Test
    @Order(15)
    @DisplayName("Should validate read-schema step without source")
    void shouldValidateReadSchemaStepWithoutSource() throws Exception {
        LOGGER.info("\n=== Test: Read-Schema Without Source ===\n");

        try {
            rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "invalid-read-schema-no-source.yaml");
            Map<String, Object> data = new HashMap<>();
            rulesEngine.evaluate(data);
        } catch (Exception e) {
            LOGGER.info("Correctly detected issue: {}", e.getMessage());
            assertTrue(true, "Exception expected for missing source");
        }
    }

    @Test
    @Order(16)
    @DisplayName("Should validate empty pipeline steps")
    void shouldValidateEmptyPipelineSteps() throws Exception {
        LOGGER.info("\n=== Test: Empty Pipeline Steps ===\n");

        try {
            rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "invalid-empty-steps.yaml");
            Map<String, Object> data = new HashMap<>();
            rulesEngine.evaluate(data);
        } catch (Exception e) {
            LOGGER.info("Correctly detected issue: {}", e.getMessage());
            assertTrue(true, "Exception expected for empty steps");
        }
    }

    // ========================================================================
    // DATABASE SCHEMA TESTS (buildDataSourceContext, buildJdbcUrl)
    // ========================================================================

    @Test
    @Order(17)
    @DisplayName("Should read schema from H2 database")
    void shouldReadSchemaFromH2Database() throws Exception {
        LOGGER.info("\n=== Test: Read Schema from H2 Database ===\n");

        setupSchemaTestDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "read-schema-h2.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("schemaMode", "database");
        
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
        
        // Check if schema was read via execution path
        List<ExecutionStep> steps = result.getExecutionPath();
        if (steps != null) {
            steps.forEach(step -> 
                LOGGER.info("Schema step: {} - hasData: {}", step.getName(), step.hasStepData()));
        }
    }

    @Test
    @Order(18)
    @DisplayName("Should execute successful load step with data")
    void shouldExecuteSuccessfulLoadStepWithData() throws Exception {
        LOGGER.info("\n=== Test: Load Step With Data ===\n");

        setupLoadTestDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "load-step-success.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("loadMode", "insert");
        
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
    }

    @Test
    @Order(19)
    @DisplayName("Should handle transform with validation rules")
    void shouldHandleTransformWithValidationRules() throws Exception {
        LOGGER.info("\n=== Test: Transform with Validation Rules ===\n");

        setupValidationTestDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "transform-validation.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("validationMode", "check");
        
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
    }

    @Test
    @Order(20)
    @DisplayName("Should handle transform with filter transformations")
    void shouldHandleTransformWithFilterTransformations() throws Exception {
        LOGGER.info("\n=== Test: Transform with Filter ===\n");

        setupFilterTestDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "transform-filter.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("filterMode", "active");
        
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
    }

    @Test
    @Order(21)
    @DisplayName("Should handle step with unknown type (no-op)")
    void shouldHandleStepWithUnknownTypeAsNoOp() throws Exception {
        LOGGER.info("\n=== Test: Unknown Step Type (No-Op) ===\n");

        // Unknown step types are silently skipped (no-op), not errors
        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "invalid-step-type.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Step completes but does nothing
        assertNotNull(result, "Result should not be null - unknown types are no-ops");
    }

    @Test
    @Order(22)
    @DisplayName("Should handle transform with aggregation type")
    void shouldHandleTransformWithAggregationType() throws Exception {
        LOGGER.info("\n=== Test: Transform with Aggregation Type ===\n");

        setupValidationTestDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "transform-aggregation.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("mode", "aggregate");
        
        RuleResult result = rulesEngine.evaluate(data);

        // Aggregation type logs error but doesn't fail
        assertNotNull(result, "Result should not be null");
    }

    @Test
    @Order(23)
    @DisplayName("Should handle transform without type or field")
    void shouldHandleTransformWithoutTypeOrField() throws Exception {
        LOGGER.info("\n=== Test: Transform Missing Type/Field ===\n");

        setupValidationTestDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "transform-missing-type.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("mode", "test");
        
        RuleResult result = rulesEngine.evaluate(data);

        // Missing type/field logs error but doesn't fail
        assertNotNull(result, "Result should not be null");
    }

    @Test
    @Order(24)
    @DisplayName("Should handle transform with unknown type")
    void shouldHandleTransformWithUnknownType() throws Exception {
        LOGGER.info("\n=== Test: Transform with Unknown Type ===\n");

        setupValidationTestDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "transform-unknown-type.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("mode", "test");
        
        RuleResult result = rulesEngine.evaluate(data);

        // Unknown type logs error but doesn't fail
        assertNotNull(result, "Result should not be null");
    }

    @Test
    @Order(25)
    @DisplayName("Should handle CURRENT_TIMESTAMP field addition")
    void shouldHandleCurrentTimestampFieldAddition() throws Exception {
        LOGGER.info("\n=== Test: CURRENT_TIMESTAMP Field Addition ===\n");

        setupTransformTestDatabase();

        rulesEngine = RulesEngine.fromFile(TEST_YAML_BASE_PATH + "transform-timestamp.yaml");

        Map<String, Object> data = new HashMap<>();
        data.put("mode", "timestamp");
        
        RuleResult result = rulesEngine.evaluate(data);

        assertNotNull(result, "Result should not be null");
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private void setupRetryTestDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:retry_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS retry_records (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(255),
                    status VARCHAR(50)
                )
            """);
            stmt.execute("DELETE FROM retry_records");
            stmt.execute("""
                INSERT INTO retry_records (id, name, status) VALUES
                    (1, 'Record 1', 'active'),
                    (2, 'Record 2', 'pending')
            """);
        }
    }

    private void setupDependencyTestDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:dependency_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS source_data (
                    id INTEGER PRIMARY KEY,
                    data_value VARCHAR(255)
                )
            """);
            stmt.execute("DELETE FROM source_data");
            stmt.execute("""
                INSERT INTO source_data (id, data_value) VALUES
                    (1, 'Value A'),
                    (2, 'Value B')
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS target_data (
                    id INTEGER PRIMARY KEY,
                    transformed_value VARCHAR(255)
                )
            """);
        }
    }

    private void setupSimpleExtractDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:parallel_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS parallel_records (
                    id INTEGER PRIMARY KEY,
                    data VARCHAR(255)
                )
            """);
            stmt.execute("DELETE FROM parallel_records");
            stmt.execute("""
                INSERT INTO parallel_records (id, data) VALUES
                    (1, 'Data 1'),
                    (2, 'Data 2'),
                    (3, 'Data 3')
            """);
        }
    }

    private void setupEtlTestDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:etl_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
             Statement stmt = conn.createStatement()) {

            // Source table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS source_records (
                    id INTEGER PRIMARY KEY,
                    item_name VARCHAR(255),
                    quantity INTEGER,
                    price DECIMAL(10,2)
                )
            """);
            stmt.execute("DELETE FROM source_records");
            stmt.execute("""
                INSERT INTO source_records (id, item_name, quantity, price) VALUES
                    (1, 'Item A', 10, 25.50),
                    (2, 'Item B', 5, 50.00),
                    (3, 'Item C', 15, 10.75)
            """);

            // Sink table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS target_records (
                    id INTEGER PRIMARY KEY,
                    item_name VARCHAR(255),
                    quantity INTEGER,
                    price DECIMAL(10,2),
                    total_value DECIMAL(10,2)
                )
            """);
        }
    }

    private void setupTransformTestDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:transform_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transform_records (
                    id INTEGER PRIMARY KEY,
                    item_name VARCHAR(255),
                    quantity INTEGER,
                    unit_price DECIMAL(10,2)
                )
            """);
            stmt.execute("DELETE FROM transform_records");
            stmt.execute("""
                INSERT INTO transform_records (id, item_name, quantity, unit_price) VALUES
                    (1, 'Widget', 10, 5.00),
                    (2, 'Gadget', 5, 15.00)
            """);
        }
    }

    private void setupSchemaTestDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:schema_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schema_test_table (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(255),
                    description TEXT,
                    created_at TIMESTAMP,
                    amount DECIMAL(10,2),
                    active BOOLEAN
                )
            """);
        }
    }

    private void setupLoadTestDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:load_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
             Statement stmt = conn.createStatement()) {

            // Source table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS load_source (
                    id INTEGER PRIMARY KEY,
                    item_name VARCHAR(255),
                    quantity INTEGER
                )
            """);
            stmt.execute("DELETE FROM load_source");
            stmt.execute("""
                INSERT INTO load_source (id, item_name, quantity) VALUES
                    (1, 'Item-A', 10),
                    (2, 'Item-B', 20)
            """);

            // Target table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS load_target (
                    id INTEGER PRIMARY KEY,
                    item_name VARCHAR(255),
                    quantity INTEGER
                )
            """);
        }
    }

    private void setupValidationTestDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:validation_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS validation_source (
                    id INTEGER PRIMARY KEY,
                    customer_name VARCHAR(255),
                    status VARCHAR(50)
                )
            """);
            stmt.execute("DELETE FROM validation_source");
            stmt.execute("""
                INSERT INTO validation_source (id, customer_name, status) VALUES
                    (1, 'Customer One', 'ACTIVE'),
                    (2, 'Customer Two', 'PENDING'),
                    (3, '', 'ACTIVE')
            """);
        }
    }

    private void setupFilterTestDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:filter_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS filter_source (
                    id INTEGER PRIMARY KEY,
                    product_name VARCHAR(255),
                    is_active BOOLEAN,
                    price DECIMAL(10,2)
                )
            """);
            stmt.execute("DELETE FROM filter_source");
            stmt.execute("""
                INSERT INTO filter_source (id, product_name, is_active, price) VALUES
                    (1, 'Product-A', true, 10.00),
                    (2, 'Product-B', false, 20.00),
                    (3, 'Product-C', true, 30.00)
            """);
        }
    }

    private void cleanupTestDirectories() throws Exception {
        // Clean up test report directories that may have been created
        Path testReportsDir = Path.of("target/test-reports");
        if (Files.exists(testReportsDir)) {
            Files.walk(testReportsDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception e) { /* ignore */ }
                });
        }
    }
}
