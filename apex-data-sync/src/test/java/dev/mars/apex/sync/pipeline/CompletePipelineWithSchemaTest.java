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

package dev.mars.apex.sync.pipeline;

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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for complete ETL pipeline with schema validation using test-complete-pipeline-with-schema.yaml.
 * Validates a full pipeline: read-schema → extract → transform → load.
 *
 * <p><b>CRITICAL VALIDATION CHECKLIST:</b></p>
 * <ul>
 *   <li>Extends SyncTestBase (provides APEX service setup/teardown)</li>
 *   <li>Uses ColoredTestOutputExtension (via SyncTestBase)</li>
 *   <li>Loads configuration from resources using naming convention</li>
 *   <li>Validates execution rates (100% success expected)</li>
 *   <li>Proper cleanup of test resources</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class CompletePipelineWithSchemaTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(CompletePipelineWithSchemaTest.class);
    
    private Connection sourceConnection;
    private Connection targetConnection;

    @BeforeEach
    public void setUpTestDatabases() throws Exception {
        // Create source database (SQL Server mode)
        String sourceJdbcUrl = "jdbc:h2:mem:source_db;MODE=MSSQLServer;DB_CLOSE_DELAY=-1";
        sourceConnection = DriverManager.getConnection(sourceJdbcUrl, "sa", "");
        
        try (Statement stmt = sourceConnection.createStatement()) {
            // Drop tables if they exist from previous test run (DB_CLOSE_DELAY=-1 keeps DB alive)
            stmt.execute("DROP TABLE IF EXISTS customers");

            stmt.execute("CREATE TABLE customers (" +
                    "id INT PRIMARY KEY, " +
                    "name VARCHAR(255), " +
                    "email VARCHAR(255)" +
                    ")");
            
            // Insert test data
            stmt.execute("INSERT INTO customers (id, name, email) VALUES " +
                    "(1, 'John Doe', 'john@example.com'), " +
                    "(2, 'Jane Smith', 'jane@example.com'), " +
                    "(3, 'Bob Johnson', 'bob@example.com')");
        }
        logger.info("Created source database with 3 customer records");

        // Create target database (PostgreSQL mode)
        String targetJdbcUrl = "jdbc:h2:mem:target_db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        targetConnection = DriverManager.getConnection(targetJdbcUrl, "sa", "");
        
        try (Statement stmt = targetConnection.createStatement()) {
            stmt.execute("CREATE TABLE customers (" +
                    "id INT PRIMARY KEY, " +
                    "name VARCHAR(255), " +
                    "email VARCHAR(255)" +
                    ")");
        }
        logger.info("Created target database (initially empty)");
    }

    @AfterEach
    public void tearDownDatabases() throws Exception {
        if (sourceConnection != null && !sourceConnection.isClosed()) {
            sourceConnection.close();
            logger.info("Closed source database connection");
        }
        if (targetConnection != null && !targetConnection.isClosed()) {
            targetConnection.close();
            logger.info("Closed target database connection");
        }
    }

    @Test
    @DisplayName("Should execute complete ETL pipeline with schema validation")
    public void shouldExecuteCompletePipelineWithSchema() throws Exception {
        // Load configuration from Java test directory (APEX naming convention)
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/pipeline/CompletePipelineWithSchemaTest.yaml");
        assertNotNull(rulesEngine, "RulesEngine should be initialized");

        // Execute the complete pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertNotNull(result, "RuleResult should not be null");

        // Validate execution
        logger.info("Pipeline execution completed");
        logger.info("Overall status: {}", result.isSuccess() ? "SUCCESS" : "FAILURE");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Verify pipeline steps
        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertEquals(4, steps.size(), "Should have 4 pipeline steps (read-schema, extract, transform, load)");

        // Verify read-schema step
        ExecutionStep readSchemaStep = steps.stream()
            .filter(step -> step.getName().contains("read-source-schema"))
            .findFirst()
            .orElse(null);
        assertNotNull(readSchemaStep, "Should have read-source-schema step");
        assertTrue(readSchemaStep.hasStepData(), "Read-schema step should have data");

        // Verify schema metadata
        Object schemaData = readSchemaStep.getStepData();
        assertInstanceOf(SchemaMetadata.class, schemaData, "Step data should be SchemaMetadata");
        SchemaMetadata schema = (SchemaMetadata) schemaData;
        assertEquals(3, schema.getColumns().size(), "Source schema should have 3 columns (id, name, email)");
        logger.info("[OK] Schema validated: {} columns", schema.getColumns().size());

        // Verify extract step
        ExecutionStep extractStep = steps.stream()
            .filter(step -> step.getName().contains("extract-from-source"))
            .findFirst()
            .orElse(null);
        assertNotNull(extractStep, "Should have extract-from-source step");
        logger.info("[OK] Extract step completed");

        // Verify transform step
        ExecutionStep transformStep = steps.stream()
            .filter(step -> step.getName().contains("transform-data"))
            .findFirst()
            .orElse(null);
        assertNotNull(transformStep, "Should have transform-data step");
        logger.info("[OK] Transform step completed");

        // Verify load step
        ExecutionStep loadStep = steps.stream()
            .filter(step -> step.getName().contains("load-to-target"))
            .findFirst()
            .orElse(null);
        assertNotNull(loadStep, "Should have load-to-target step");
        logger.info("[OK] Load step completed");
        
        // Validate execution rate (4 steps: read-schema, extract, transform, load)
        validateExecutionRate(4, 4, "Complete ETL pipeline");

        // Verify data was loaded to target database
        try (Statement stmt = targetConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM customers")) {
            assertTrue(rs.next(), "Should have count result");
            int count = rs.getInt("cnt");
            logger.info("Target database has {} customer records", count);
            assertEquals(3, count, "Should have loaded 3 customers to target");
        }
    }
}
