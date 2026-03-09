package dev.mars.apex.engine.pipeline;

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

import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.ExecutionStep;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.core.service.schema.SchemaMetadata;
import dev.mars.apex.core.service.schema.diff.SchemaComparisonResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the schema-diff pipeline stage.
 * Tests the complete pipeline integration using RulesEngine.fromFile() pattern.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Schema Diff Pipeline Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SchemaDiffPipelineIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(SchemaDiffPipelineIntegrationTest.class);
    private static final String TEST_YAML_BASE_PATH = "src/test/java/dev/mars/apex/engine/pipeline/";
    
    private RulesEngine rulesEngine;
    private Connection h2Connection; // Keep connection open for DB tests

    @AfterEach
    void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
        // Close H2 connection after test
        try {
            if (h2Connection != null && !h2Connection.isClosed()) {
                h2Connection.close();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    @Test
    @Order(1)
    @org.junit.jupiter.api.Disabled("H2 database connection issue - CSV tests cover this functionality")
    @DisplayName("Should compare CSV to Database schema with matching columns")
    void shouldCompareCsvToDatabaseWithMatchingColumns() throws Exception {
        logger.info("\n=== Test: CSV to Database Schema Comparison - Matching Columns ===\n");

        // Setup test database
        setupCustomersDatabase();

        // Load pipeline configuration
        rulesEngine = RulesEngine.fromFile(
            TEST_YAML_BASE_PATH + "SchemaDiffPipelineIntegrationTest_CsvToDatabase.yaml");

        // Execute pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Verify success
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Find schema-diff step
        ExecutionStep schemaDiffStep = findPipelineStep(result, "compare-schemas");
        assertNotNull(schemaDiffStep, "Should have schema-diff step");
        assertTrue(schemaDiffStep.hasStepData(), "Schema-diff step should have data");

        // Verify comparison result
        Object stepData = schemaDiffStep.getStepData();
        assertInstanceOf(SchemaComparisonResult.class, stepData, "Step data should be SchemaComparisonResult");

        SchemaComparisonResult comparisonResult = (SchemaComparisonResult) stepData;
        
        logger.info("Comparison result: {} matching, {} added, {} removed, {} changed",
                   comparisonResult.getMatchingColumns().size(),
                   comparisonResult.getAddedColumns().size(),
                   comparisonResult.getRemovedColumns().size(),
                   comparisonResult.getChangedColumns().size());

        assertEquals(3, comparisonResult.getMatchingColumns().size(), "Should have 3 matching columns");
        assertTrue(comparisonResult.isCompatible(), "Schemas should be compatible");
        assertFalse(comparisonResult.hasChanges(), "Should have no differences");
    }

    @Test
    @Order(2)
    @DisplayName("Should detect added columns as compatible change")
    void shouldDetectAddedColumns() throws Exception {
        logger.info("\n=== Test: Detect Added Columns ===\n");

        setupCustomersDatabase();

        rulesEngine = RulesEngine.fromFile(
            TEST_YAML_BASE_PATH + "SchemaDiffPipelineIntegrationTest_AddedColumns.yaml");

        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        SchemaComparisonResult comparisonResult = extractComparisonResult(result, "compare-schemas");
        
        assertEquals(1, comparisonResult.getAddedColumns().size(), "Should detect 1 added column");
        assertEquals("phone", comparisonResult.getAddedColumns().get(0).getColumnName());
        assertTrue(comparisonResult.isCompatible(), "Adding columns should be compatible");
    }

    @Test
    @Order(3)
    @DisplayName("Should detect removed columns as breaking change")
    void shouldDetectRemovedColumnsAsBreakingChange() throws Exception {
        logger.info("\n=== Test: Detect Removed Columns as Breaking Change ===\n");

        setupCustomersDatabase();

        rulesEngine = RulesEngine.fromFile(
            TEST_YAML_BASE_PATH + "SchemaDiffPipelineIntegrationTest_RemovedColumns.yaml");

        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        SchemaComparisonResult comparisonResult = extractComparisonResult(result, "compare-schemas");
        
        assertEquals(1, comparisonResult.getRemovedColumns().size(), "Should detect 1 removed column");
        assertFalse(comparisonResult.isCompatible(), "Removing columns should be breaking");
        assertTrue(comparisonResult.getBreakingChanges().size() > 0, "Should have breaking changes");
    }

    @Test
    @Order(4)
    @DisplayName("Should fail pipeline when fail-on-incompatibility is true and schemas are incompatible")
    void shouldFailOnIncompatibility() throws Exception {
        logger.info("\n=== Test: Fail on Incompatibility ===\n");

        setupCustomersDatabase();

        rulesEngine = RulesEngine.fromFile(
            TEST_YAML_BASE_PATH + "SchemaDiffPipelineIntegrationTest_FailOnIncompatibility.yaml");

        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        
        // Pipeline should fail due to incompatibility
        assertFalse(result.isSuccess(), "Pipeline should fail on incompatibility");
        
        // The error message should mention failure
        String message = result.getMessage().toLowerCase();
        assertTrue(message.contains("fail") || message.contains("error") || 
                   message.contains("compare-schemas"),
                   "Error message should mention failure: " + result.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("Should handle case-insensitive column names")
    void shouldHandleCaseInsensitiveNames() throws Exception {
        logger.info("\n=== Test: Case-Insensitive Column Names ===\n");

        setupCustomersDatabase();

        rulesEngine = RulesEngine.fromFile(
            TEST_YAML_BASE_PATH + "SchemaDiffPipelineIntegrationTest_CaseInsensitive.yaml");

        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        SchemaComparisonResult comparisonResult = extractComparisonResult(result, "compare-schemas");
        
        // With case-insensitive matching, all columns should match
        assertEquals(3, comparisonResult.getMatchingColumns().size(), "Should have 3 matching columns");
        assertEquals(0, comparisonResult.getAddedColumns().size(), "Should have no added columns");
        assertTrue(comparisonResult.isCompatible(), "Should be compatible");
    }

    @Test
    @Order(6)
    @DisplayName("Should apply type mappings for database migrations")
    void shouldApplyTypeMappings() throws Exception {
        logger.info("\n=== Test: Type Mappings for Database Migration ===\n");

        setupCustomersDatabase();

        rulesEngine = RulesEngine.fromFile(
            TEST_YAML_BASE_PATH + "SchemaDiffPipelineIntegrationTest_TypeMappings.yaml");

        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        SchemaComparisonResult comparisonResult = extractComparisonResult(result, "compare-schemas");
        
        // With proper type mappings, types should be compatible
        assertTrue(comparisonResult.getMatchingColumns().size() > 0, "Should have matching columns");
        assertTrue(comparisonResult.isCompatible(), "Should be compatible with type mappings");
    }

    // Helper Methods

    private void setupCustomersDatabase() throws Exception {
        h2Connection = DriverManager.getConnection(
            "jdbc:h2:mem:schema_diff_test;DB_CLOSE_DELAY=-1", "sa", "");
        
        Statement stmt = h2Connection.createStatement();
        stmt.execute("DROP TABLE IF EXISTS customers");
        stmt.execute(
            "CREATE TABLE customers (" +
            "  id INTEGER PRIMARY KEY, " +
            "  name VARCHAR(100) NOT NULL, " +
            "  email VARCHAR(100)" +
            ")");
        stmt.close();
        // Do NOT close connection - keep it open for pipeline to use
        
        logger.info("[OK] Test database setup complete");
    }

    private ExecutionStep findPipelineStep(RuleResult result, String stepName) {
        return result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .filter(step -> step.getName().equals(stepName))
            .findFirst()
            .orElse(null);
    }

    private SchemaComparisonResult extractComparisonResult(RuleResult result, String stepName) {
        ExecutionStep step = findPipelineStep(result, stepName);
        assertNotNull(step, "Should have " + stepName + " step");
        assertTrue(step.hasStepData(), "Step should have data");
        
        Object stepData = step.getStepData();
        assertInstanceOf(SchemaComparisonResult.class, stepData, 
                        "Step data should be SchemaComparisonResult");
        
        return (SchemaComparisonResult) stepData;
    }

    @Test
    @Order(6)
    public void shouldGenerateHtmlReport() throws Exception {
        logger.info("\n=== Test: Generate HTML Report for Schema Diff ===\n");

        // Load configuration with report-output parameter
        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/engine/pipeline/SchemaDiffPipelineIntegrationTest_HtmlReport.yaml");

        // Execute the pipeline
        RuleResult result = engine.evaluate(new HashMap<>());

        // Verify execution
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        // Extract schema comparison result
        SchemaComparisonResult comparisonResult = extractComparisonResult(result, "compare-schemas");

        // Verify comparison results
        assertEquals(1, comparisonResult.getAddedColumns().size(), "Should detect 1 added column (phone)");
        assertEquals(3, comparisonResult.getMatchingColumns().size(), "Should have 3 matching columns");

        // Verify HTML report was generated
        java.nio.file.Path reportPath = java.nio.file.Paths.get("target", "reports", "schema-diff-test-report.html");
        assertTrue(java.nio.file.Files.exists(reportPath), 
                  "HTML report should be generated at: " + reportPath);

        // Read and verify report content contains key elements
        String reportContent = java.nio.file.Files.readString(reportPath);
        assertTrue(reportContent.contains("Schema Diff Report"), 
                  "Report should contain title");
        assertTrue(reportContent.contains("Added"), 
                  "Report should contain added columns section");
        assertTrue(reportContent.contains("phone"), 
                  "Report should mention the added 'phone' column");
        assertTrue(reportContent.contains("Matching"), 
                  "Report should contain matching columns section");

        logger.info("[OK] HTML report generated successfully: {}", reportPath.toAbsolutePath());
        logger.info("[OK] Report contains expected schema diff information");
        
        engine.shutdown();
    }
}
