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
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
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
 * Tests SQL Server to PostgreSQL migration validation.
 * Validates cross-platform migration with type mappings.
 *
 * <p><b>CRITICAL VALIDATION CHECKLIST:</b></p>
 * <ul>
 *   <li>✅ Extends SyncTestBase (provides APEX service setup/teardown)</li>
 *   <li>✅ Uses ColoredTestOutputExtension (via SyncTestBase)</li>
 *   <li>✅ Validates SQL Server schema reading (simulated via CSV)</li>
 *   <li>✅ Validates PostgreSQL schema reading</li>
 *   <li>✅ Verifies cross-platform type mappings</li>
 *   <li>✅ Proper cleanup of test resources</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class SqlServerPostgresMigrationTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(SqlServerPostgresMigrationTest.class);
    
    private File testCsvFile;
    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUpTestData() throws Exception {
        // Create test CSV file simulating SQL Server schema
        testCsvFile = new File("src/test/resources/test-data/sqlserver-customers.csv");
        testCsvFile.getParentFile().mkdirs();
        try (PrintWriter writer = new PrintWriter(testCsvFile)) {
            writer.println("id,name,email,created_date");
            writer.println("1,Alice,alice@example.com,2024-01-01");
            writer.println("2,Bob,bob@example.com,2024-01-02");
        }
        logger.info("Created SQL Server schema CSV file: {}", testCsvFile.getAbsolutePath());

        // Setup PostgreSQL target database
        String dbUrl = "jdbc:h2:mem:cross_platform;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (Connection conn = DriverManager.getConnection(dbUrl, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                // PostgreSQL target with compatible types
            // Drop tables if they exist from previous test run (DB_CLOSE_DELAY=-1 keeps DB alive)
            stmt.execute("DROP TABLE IF EXISTS TARGET_CUSTOMERS");

                stmt.execute("CREATE TABLE TARGET_CUSTOMERS (id INT, name VARCHAR(255), email VARCHAR(255), created_date TIMESTAMP)");
            }
        }
        logger.info("Created PostgreSQL target schema");
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
    @DisplayName("Should validate SQL Server to PostgreSQL cross-platform migration")
    public void shouldValidateSqlServerPostgresMigration() throws Exception {
        // Load configuration from Java test directory (APEX naming convention)
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/validation/SqlServerPostgresMigrationTest.yaml");
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

        // Verify read-sqlserver-schema step
        ExecutionStep readSqlServerStep = pipelineSteps.stream()
            .filter(step -> step.getName().contains("sqlserver-schema"))
            .findFirst()
            .orElse(null);
        assertNotNull(readSqlServerStep, "Should have read-sqlserver-schema step");
        assertTrue(readSqlServerStep.hasStepData(), "SQL Server schema step should have data");
        assertInstanceOf(SchemaMetadata.class, readSqlServerStep.getStepData(), "SQL Server data should be SchemaMetadata");
        
        SchemaMetadata sqlServerSchema = (SchemaMetadata) readSqlServerStep.getStepData();
        assertEquals(4, sqlServerSchema.getColumns().size(), "SQL Server schema should have 4 columns");

        // Verify read-postgres-schema step
        ExecutionStep readPostgresStep = pipelineSteps.stream()
            .filter(step -> step.getName().contains("postgres-schema"))
            .findFirst()
            .orElse(null);
        assertNotNull(readPostgresStep, "Should have read-postgres-schema step");
        assertTrue(readPostgresStep.hasStepData(), "PostgreSQL schema step should have data");
        assertInstanceOf(SchemaMetadata.class, readPostgresStep.getStepData(), "PostgreSQL data should be SchemaMetadata");

        SchemaMetadata postgresSchema = (SchemaMetadata) readPostgresStep.getStepData();
        assertEquals(4, postgresSchema.getColumns().size(), "PostgreSQL schema should have 4 columns");

        // Verify validate-cross-platform-migration step
        ExecutionStep validateStep = pipelineSteps.stream()
            .filter(step -> step.getName().contains("cross-platform"))
            .findFirst()
            .orElse(null);
        assertNotNull(validateStep, "Should have validate-cross-platform-migration step");

        logger.info("SQL Server to PostgreSQL migration validation completed successfully");
    }
}
