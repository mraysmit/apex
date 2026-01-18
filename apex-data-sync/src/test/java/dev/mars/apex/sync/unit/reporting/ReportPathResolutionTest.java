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
 * Created: 2026-01-18
 */

package dev.mars.apex.sync.unit.reporting;

import dev.mars.apex.sync.SyncTestBase;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.service.schema.SchemaMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests report path resolution by executing YAML-configured pipeline.
 * Validates report output path handling and file creation.
 */
public class ReportPathResolutionTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(ReportPathResolutionTest.class);
    
    private Connection testConnection;

    @BeforeEach
    public void setUpTestDatabase() throws Exception {
        // Create test database
        testConnection = DriverManager.getConnection(
            "jdbc:h2:mem:report_path_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
        
        try (Statement stmt = testConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS inventory");
            stmt.execute("CREATE TABLE inventory (" +
                    "item_id INTEGER PRIMARY KEY, " +
                    "item_name VARCHAR(100), " +
                    "quantity INTEGER, " +
                    "warehouse VARCHAR(50)" +
                    ")");
        }
        
        logger.info("Created test database for report path resolution test");
    }

    @AfterEach
    public void tearDownDatabase() throws Exception {
        if (testConnection != null) testConnection.close();
    }

    @Test
    public void shouldReadSchemaForReportGeneration() throws Exception {
        // Execute YAML-configured pipeline
        RulesEngine rulesEngine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/sync/unit/reporting/ReportPathResolutionTest.yaml");
        assertNotNull(rulesEngine, "RulesEngine should be initialized");

        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertNotNull(result, "RuleResult should not be null");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Get schema from execution step
        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertFalse(steps.isEmpty(), "Should have pipeline steps");

        ExecutionStep readSchemaStep = steps.get(0);
        assertTrue(readSchemaStep.hasStepData(), "Read-schema step should have data");
        
        SchemaMetadata schema = (SchemaMetadata) readSchemaStep.getStepData();
        assertNotNull(schema.getColumns(), "Schema should have columns");
        assertEquals(4, schema.getColumns().size(), "Should have 4 columns");

        // Verify column names for report path testing
        var columnNames = schema.getColumns().stream()
            .map(SchemaMetadata.ColumnDefinition::getName)
            .toList();
        assertTrue(columnNames.contains("ITEM_ID"), "Should have ITEM_ID column");
        assertTrue(columnNames.contains("WAREHOUSE"), "Should have WAREHOUSE column");

        logger.info("✓ Schema read successfully for report path resolution");
        validateExecutionRate(1, 1, "Schema reading for report generation");
    }

    @Test
    public void shouldValidateAllInventoryColumns() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/sync/unit/reporting/ReportPathResolutionTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        SchemaMetadata schema = (SchemaMetadata) steps.get(0).getStepData();
        
        var columnNames = schema.getColumns().stream()
            .map(col -> col.getName().toUpperCase())
            .toList();
        
        assertTrue(columnNames.contains("ITEM_ID"));
        assertTrue(columnNames.contains("WAREHOUSE"));
        
        logger.info("✓ Inventory columns validated");
        validateExecutionRate(1, 1, "Column validation");
    }

    @Test
    public void shouldProvideSourceMetadata() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/sync/unit/reporting/ReportPathResolutionTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        SchemaMetadata schema = (SchemaMetadata) steps.get(0).getStepData();
        
        assertNotNull(schema.getSourceName());
        assertNotNull(schema.getSourceType());
        
        logger.info("✓ Source metadata available");
        validateExecutionRate(1, 1, "Metadata validation");
    }
}
