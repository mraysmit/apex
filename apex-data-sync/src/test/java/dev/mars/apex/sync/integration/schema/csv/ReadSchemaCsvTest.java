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
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CSV schema reading using test-read-schema-csv.yaml configuration.
 * Validates schema metadata extraction from CSV files.
 *
 * <p><b>CRITICAL VALIDATION CHECKLIST:</b></p>
 * <ul>
 *   <li>✅ Extends SyncTestBase (provides APEX service setup/teardown)</li>
 *   <li>✅ Uses ColoredTestOutputExtension (via SyncTestBase)</li>
 *   <li>✅ Loads configuration from resources using naming convention</li>
 *   <li>✅ Validates execution rates (100% success expected)</li>
 *   <li>✅ Proper cleanup of test resources</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class ReadSchemaCsvTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaCsvTest.class);
    
    private File testCsvFile;

    @BeforeEach
    public void setUpTestData() throws Exception {
        // Create test CSV file that matches the YAML configuration
        testCsvFile = new File("test-customers.csv");
        try (PrintWriter writer = new PrintWriter(testCsvFile)) {
            writer.println("id,name,email,age");
            writer.println("1,John Doe,john@example.com,30");
            writer.println("2,Jane Smith,jane@example.com,25");
            writer.println("3,Bob Johnson,bob@example.com,35");
        }
        logger.info("Created test CSV file: {}", testCsvFile.getAbsolutePath());
    }

    @AfterEach
    public void tearDownCsv() {
        // Clean up test CSV file
        if (testCsvFile != null && testCsvFile.exists()) {
            boolean deleted = testCsvFile.delete();
            logger.info("Deleted test CSV file: {} (success: {})", testCsvFile.getName(), deleted);
        }
    }

    @Test
    @DisplayName("Should read schema from CSV file using YAML configuration")
    public void shouldReadSchemaFromCsv() throws Exception {
        // Load configuration from Java test directory (APEX naming convention)
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/schema/ReadSchemaCsvTest.yaml");
        assertNotNull(rulesEngine, "RulesEngine should be initialized");

        // Execute the pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertNotNull(result, "RuleResult should not be null");

        // Validate execution
        logger.info("Pipeline execution completed");
        logger.info("Overall status: {}", result.isSuccess() ? "SUCCESS" : "FAILURE");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Find the read-schema step
        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertFalse(steps.isEmpty(), "Should have pipeline steps");

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

        // Verify column details
        SchemaMetadata.ColumnDefinition idColumn = schema.getColumns().stream()
            .filter(col -> "id".equals(col.getName()))
            .findFirst()
            .orElse(null);
        assertNotNull(idColumn, "Should have id column");
        logger.info("✓ CSV schema validated: {} columns", schema.getColumns().size());
        
        // Validate execution rate
        validateExecutionRate(1, 1, "CSV schema reading");
    }
}
