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

package dev.mars.apex.sync.validation;

import dev.mars.apex.sync.SyncTestBase;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.ExecutionStep;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.core.service.schema.SchemaMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
 * Tests CSV to PostgreSQL migration validation.
 * Validates schema compatibility between legacy CSV files and PostgreSQL target.
 *
 * <p><b>CRITICAL VALIDATION CHECKLIST:</b></p>
 * <ul>
 *   <li>Extends SyncTestBase (provides APEX service setup/teardown)</li>
 *   <li>Uses ColoredTestOutputExtension (via SyncTestBase)</li>
 *   <li>Validates pipeline steps - 3 steps expected (read CSV, read DB, validate)</li>
 *   <li>Verifies schema metadata extraction from both sources</li>
 *   <li>Validates migration compatibility</li>
 *   <li>Proper cleanup of test resources</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class CsvToPostgresMigrationTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(CsvToPostgresMigrationTest.class);
    
    private File testCsvFile;
    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUpTestData() throws Exception {
        // Create test CSV file
        testCsvFile = new File("src/test/resources/test-data/legacy-customers.csv");
        testCsvFile.getParentFile().mkdirs();
        try (PrintWriter writer = new PrintWriter(testCsvFile)) {
            writer.println("id,name,email,phone");
            writer.println("1,John Doe,john@example.com,555-1234");
            writer.println("2,Jane Smith,jane@example.com,555-5678");
            writer.println("3,Bob Johnson,bob@example.com,555-9012");
        }
        logger.info("Created test CSV file: {}", testCsvFile.getAbsolutePath());

        // Setup target database with matching schema
        String dbUrl = "jdbc:h2:mem:postgres_target;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (Connection conn = DriverManager.getConnection(dbUrl, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
            // Drop tables if they exist from previous test run (DB_CLOSE_DELAY=-1 keeps DB alive)
            stmt.execute("DROP TABLE IF EXISTS CUSTOMERS");

                stmt.execute("CREATE TABLE CUSTOMERS (id INT, name VARCHAR(255), email VARCHAR(255), phone VARCHAR(20))");
            }
        }
        logger.info("Created target database schema");
    }

    @AfterEach
    public void tearDown() {
        if (testCsvFile != null && testCsvFile.exists()) {
            boolean deleted = testCsvFile.delete();
            logger.info("Deleted test CSV file: {} (success: {})", testCsvFile.getName(), deleted);
        }
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    @DisplayName("Should validate CSV to PostgreSQL migration compatibility")
    public void shouldValidateCsvToPostgresMigration() throws Exception {
        // Load configuration from Java test directory (APEX naming convention)
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/validation/CsvToPostgresMigrationTest.yaml");
        assertNotNull(rulesEngine, "RulesEngine should be initialized");

        // Execute the pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertNotNull(result, "RuleResult should not be null");

        // Validate execution
        logger.info("Pipeline execution completed");
        logger.info("Overall status: {}", result.isSuccess() ? "SUCCESS" : "FAILURE");
        
        if (!result.isSuccess()) {
            logger.error("Pipeline failed: {}", result.getMessage());
            for (ExecutionStep step : result.getExecutionPath()) {
                logger.error("  Step: {} - Status: {} - Message: {}", 
                    step.getName(), step.getStatus(), step.getMessage());
            }
        }
        
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Verify pipeline steps
        List<ExecutionStep> pipelineSteps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertEquals(3, pipelineSteps.size(), "Should have 3 pipeline steps");

        // Verify read-legacy-csv-schema step
        ExecutionStep readCsvStep = pipelineSteps.stream()
            .filter(step -> step.getName().contains("legacy-csv"))
            .findFirst()
            .orElse(null);
        assertNotNull(readCsvStep, "Should have read-legacy-csv-schema step");
        assertTrue(readCsvStep.hasStepData(), "Read CSV step should have schema data");
        assertInstanceOf(SchemaMetadata.class, readCsvStep.getStepData(), "CSV step data should be SchemaMetadata");
        
        SchemaMetadata csvSchema = (SchemaMetadata) readCsvStep.getStepData();
        assertEquals(4, csvSchema.getColumns().size(), "CSV should have 4 columns");

        // Verify read-postgres-target-schema step
        ExecutionStep readDbStep = pipelineSteps.stream()
            .filter(step -> step.getName().contains("postgres-target"))
            .findFirst()
            .orElse(null);
        assertNotNull(readDbStep, "Should have read-postgres-target-schema step");
        assertTrue(readDbStep.hasStepData(), "Read DB step should have schema data");
        assertInstanceOf(SchemaMetadata.class, readDbStep.getStepData(), "DB step data should be SchemaMetadata");

        SchemaMetadata dbSchema = (SchemaMetadata) readDbStep.getStepData();
        assertEquals(4, dbSchema.getColumns().size(), "Database should have 4 columns");

        // Verify validate-migration-compatibility step
        ExecutionStep validateStep = pipelineSteps.stream()
            .filter(step -> step.getName().contains("validate-migration"))
            .findFirst()
            .orElse(null);
        assertNotNull(validateStep, "Should have validate-migration-compatibility step");

        logger.info("CSV to PostgreSQL migration validation completed successfully");
    }
}
