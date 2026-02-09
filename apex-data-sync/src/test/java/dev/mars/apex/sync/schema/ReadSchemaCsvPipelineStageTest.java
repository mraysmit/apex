/*
 * Copyright 2026 Mark Andrew Ray-Smith Cityline Ltd
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
 *
 * Created: 2026-01-14
 */

package dev.mars.apex.sync.schema;

import dev.mars.apex.sync.SyncTestBase;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.ExecutionStep;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.core.service.schema.SchemaMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the read-schema pipeline stage with CSV file sources.
 * Tests reading schema metadata from CSV files with automatic type inference.
 *
 * CRITICAL VALIDATION CHECKLIST:
 * Count pipeline steps - 1 read-schema step expected per CSV file
 * Verify type inference - INTEGER, VARCHAR, DECIMAL, BOOLEAN, TIMESTAMP detection
 * Column count validation - Must match exact number of CSV columns
 * Small CSV test - 4 columns (id, name, column_c, column_d)
 * Large CSV test - 11 columns with diverse data types
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Read Schema CSV Pipeline Stage Integration Test")
class ReadSchemaCsvPipelineStageTest extends SyncTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaCsvPipelineStageTest.class);
    private RulesEngine rulesEngine;

    @AfterEach
    public void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    @DisplayName("Should read schema from CSV file")
    void shouldReadSchemaFromCsv() throws Exception {
        logger.info("\n=== Test: Read Schema from CSV ===\n");

        // Create test CSV file  
        File csvFile = createTestCsvFile();
        
        // Setup database for CSV test (pipeline might use it)
        setupTestDatabase();

        // Load pipeline configuration using RulesEngine.fromFile()
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/schema/ReadSchemaCsvPipelineStageTest.yaml");
        assertNotNull(rulesEngine, "Rules engine should be created");

        // Execute pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Verify execution success
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Verify pipeline steps executed
        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertFalse(steps.isEmpty(), "Should have pipeline steps");

        // Find the read-schema step
        ExecutionStep readSchemaStep = steps.stream()
            .filter(step -> step.getName().contains("read-schema"))
            .findFirst()
            .orElse(null);

        assertNotNull(readSchemaStep, "Should have read-schema step");
        assertTrue(readSchemaStep.hasStepData(), "Read-schema step should have data");

        // Verify schema metadata
        Object stepData = readSchemaStep.getStepData();
        assertInstanceOf(SchemaMetadata.class, stepData, "Step data should be SchemaMetadata");

        SchemaMetadata schema = (SchemaMetadata) stepData;
        logger.info("Read CSV schema: {}", schema);
        
        assertNotNull(schema.getColumns(), "Schema should have columns");
        assertEquals(4, schema.getColumns().size(), "Should have 4 columns");

        // Verify inferred types
        SchemaMetadata.ColumnDefinition idColumn = schema.getColumns().get(0);
        assertEquals("id", idColumn.getName());
        assertEquals("INTEGER", idColumn.getDataType());

        SchemaMetadata.ColumnDefinition nameColumn = schema.getColumns().get(1);
        assertEquals("name", nameColumn.getName());
        assertEquals("VARCHAR", nameColumn.getDataType());

        logger.info("[OK] Successfully read CSV schema with {} columns", schema.getColumns().size());
        
        // Display metrics
        displayPipelineMetrics(result);

        // Cleanup
        csvFile.delete();
    }

    @Test
    @DisplayName("Should read schema from CSV with 11+ columns")
    void shouldReadSchemaFromLargeCsv() throws Exception {
        logger.info("\n=== Test: Read Schema from CSV with 11 Columns ===\n");

        // Create test CSV file with 11 columns
        File csvFile = createLargeTestCsvFile();
        
        // Setup database (might be needed by pipeline)
        setupTestDatabase();

        // Load pipeline configuration
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/schema/ReadSchemaCsvPipelineStageTestLarge.yaml");
        assertNotNull(rulesEngine, "Rules engine should be created");

        // Execute pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Verify execution success
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Find the read-schema step
        ExecutionStep readSchemaStep = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .filter(step -> step.getName().contains("read-schema"))
            .findFirst()
            .orElse(null);

        assertNotNull(readSchemaStep, "Should have read-schema step");
        assertTrue(readSchemaStep.hasStepData(), "Read-schema step should have data");

        // Verify schema metadata
        SchemaMetadata schema = (SchemaMetadata) readSchemaStep.getStepData();
        logger.info("Read large CSV schema: {}", schema);
        
        assertNotNull(schema.getColumns(), "Schema should have columns");
        assertEquals(11, schema.getColumns().size(), "Should have 11 columns");

        // Verify column types are properly inferred
        assertEquals("employee_id", schema.getColumns().get(0).getName());
        assertEquals("INTEGER", schema.getColumns().get(0).getDataType());
        
        assertEquals("first_name", schema.getColumns().get(1).getName());
        assertEquals("VARCHAR", schema.getColumns().get(1).getDataType());
        
        assertEquals("salary", schema.getColumns().get(4).getName());
        assertEquals("DECIMAL", schema.getColumns().get(4).getDataType());
        
        assertEquals("is_active", schema.getColumns().get(8).getName());
        assertEquals("BOOLEAN", schema.getColumns().get(8).getDataType());

        // Log all columns
        logger.info("CSV Column Details:");
        schema.getColumns().forEach(col -> {
            logger.info("  Column: {} - Type: {}", col.getName(), col.getDataType());
        });

        logger.info("[OK] Successfully read CSV schema with {} columns", schema.getColumns().size());
        displayPipelineMetrics(result);

        // Cleanup
        csvFile.delete();
    }

    /**
     * Setup H2 test database (needed by some pipeline configurations).
     */
    private void setupTestDatabase() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:schema_test;DB_CLOSE_DELAY=-1";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement stmt = conn.createStatement()) {

            // Drop table if exists to ensure clean state
            stmt.execute("DROP TABLE IF EXISTS customers");
            
            // Create test table
            stmt.execute("CREATE TABLE customers (" +
                        "id INT PRIMARY KEY, " +
                        "name VARCHAR(255) NOT NULL, " +
                        "email VARCHAR(255))");

            // Insert sample data
            stmt.execute("INSERT INTO customers VALUES (1, 'John Doe', 'john@example.com')");
            stmt.execute("INSERT INTO customers VALUES (2, 'Jane Smith', 'jane@example.com')");

            logger.info("[OK] Test database initialized with customers table");
        }
    }

    /**
     * Create a test CSV file.
     */
    private File createTestCsvFile() throws Exception {
        File csvFile = new File("test-customers.csv");
        try (PrintWriter writer = new PrintWriter(csvFile)) {
            writer.write("id,name,email,active\n");
            writer.write("1,John Doe,john@example.com,true\n");
            writer.write("2,Jane Smith,jane@example.com,false\n");
            writer.write("3,Bob Johnson,bob@example.com,true\n");
        }
        logger.info("[OK] Created test CSV file: {}", csvFile.getAbsolutePath());
        return csvFile;
    }

    /**
     * Create a test CSV file with 11 columns covering various data types.
     */
    private File createLargeTestCsvFile() throws Exception {
        File csvFile = new File("test-employees-large.csv");
        try (PrintWriter writer = new PrintWriter(csvFile)) {
            // Header with 11 columns
            writer.write("employee_id,first_name,last_name,email,salary,department,hire_date,birth_date,is_active,phone,manager_id\n");
            
            // Sample data rows demonstrating different data types
            writer.write("1001,Alice,Johnson,alice.johnson@company.com,75000.50,Engineering,2020-01-15,1985-03-22,true,555-0101,5001\n");
            writer.write("1002,Bob,Smith,bob.smith@company.com,82000.00,Sales,2019-06-10,1990-07-18,true,555-0102,5002\n");
            writer.write("1003,Carol,Williams,carol.w@company.com,68000.75,Marketing,2021-03-01,1988-11-05,false,555-0103,5003\n");
            writer.write("1004,David,Brown,david.brown@company.com,95000.25,Engineering,2018-09-20,1982-01-30,true,555-0104,5001\n");
        }
        logger.info("[OK] Created large test CSV file with 11 columns: {}", csvFile.getAbsolutePath());
        return csvFile;
    }

    /**
     * Display pipeline execution metrics.
     */
    private void displayPipelineMetrics(RuleResult result) {
        logger.info("\n=== Pipeline Execution Metrics ===");
        logger.info("Overall Success: {}", result.isSuccess());
        logger.info("Message: {}", result.getMessage());

        result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .forEach(step -> {
                logger.info("\nStep: {}", step.getName());
                logger.info("  Status: {}", step.getStatus());
                logger.info("  Duration: {} ms", step.getDurationMs());
                if (step.getRecordsProcessed() != null) {
                    logger.info("  Records Processed: {}", step.getRecordsProcessed());
                }
            });
        logger.info("=".repeat(40));
    }
}
