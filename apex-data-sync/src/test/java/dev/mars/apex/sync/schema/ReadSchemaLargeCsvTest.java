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
 * Tests for large CSV schema reading using test-read-schema-large-csv.yaml configuration.
 * Validates schema metadata extraction from CSV files with many columns (11 columns).
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
public class ReadSchemaLargeCsvTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaLargeCsvTest.class);
    
    private File testCsvFile;

    @BeforeEach
    public void setUpTestData() throws Exception {
        // Create large test CSV file with 11 columns
        testCsvFile = new File("test-large-customers.csv");
        try (PrintWriter writer = new PrintWriter(testCsvFile)) {
            // Header with 11 columns
            writer.println("id,first_name,last_name,email,phone,address,city,state,zip_code,country,created_date");
            
            // Sample data rows
            writer.println("1,John,Doe,john@example.com,555-0101,123 Main St,Springfield,IL,62701,USA,2026-01-01");
            writer.println("2,Jane,Smith,jane@example.com,555-0102,456 Oak Ave,Chicago,IL,60601,USA,2026-01-02");
            writer.println("3,Bob,Johnson,bob@example.com,555-0103,789 Elm St,Peoria,IL,61602,USA,2026-01-03");
            writer.println("4,Alice,Williams,alice@example.com,555-0104,321 Pine Rd,Rockford,IL,61101,USA,2026-01-04");
        }
        logger.info("Created large test CSV file with 11 columns: {}", testCsvFile.getAbsolutePath());
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
    @DisplayName("Should read schema from large CSV file (11 columns) using YAML configuration")
    public void shouldReadSchemaFromLargeCsv() throws Exception {
        // Load configuration from Java test directory (APEX naming convention)
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/schema/ReadSchemaLargeCsvTest.yaml");
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
        logger.info("Read large CSV schema: {}", schema);
        
        assertNotNull(schema.getColumns(), "Schema should have columns");
        assertEquals(11, schema.getColumns().size(), "Should have 11 columns");

        // Verify some key columns
        SchemaMetadata.ColumnDefinition idColumn = schema.getColumns().stream()
            .filter(col -> "id".equals(col.getName()))
            .findFirst()
            .orElse(null);
        assertNotNull(idColumn, "Should have id column");

        SchemaMetadata.ColumnDefinition emailColumn = schema.getColumns().stream()
            .filter(col -> "email".equals(col.getName()))
            .findFirst()
            .orElse(null);
        assertNotNull(emailColumn, "Should have email column");
        logger.info("[OK] Large CSV schema validated: {} columns", schema.getColumns().size());
        
        // Validate execution rate
        validateExecutionRate(1, 1, "Large CSV schema reading");
    }
}
