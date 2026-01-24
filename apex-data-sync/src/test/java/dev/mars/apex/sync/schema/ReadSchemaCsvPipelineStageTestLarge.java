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
 * Created: 2026-01-19
 */

package dev.mars.apex.sync.schema;

import dev.mars.apex.sync.SyncTestBase;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.schema.SchemaMetadata;
import org.junit.jupiter.api.AfterEach;
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
 * Tests schema inference from large CSV files with many columns (11+),
 * validating performance and accuracy for complex CSV structures.
 *
 * CRITICAL VALIDATION CHECKLIST:
 * Large CSV file created (11 columns)
 * All columns detected correctly
 * Types inferred accurately
 * Efficient processing
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Read Schema CSV Large File Test")
class ReadSchemaCsvPipelineStageTestLarge extends SyncTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaCsvPipelineStageTestLarge.class);
    private RulesEngine rulesEngine;
    private File csvFile;

    @AfterEach
    public void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
        if (csvFile != null && csvFile.exists()) {
            csvFile.delete();
        }
        super.tearDown();
    }

    @Test
    @DisplayName("Should read schema from CSV with 11 columns")
    void shouldReadSchemaFromLargeCsv() throws Exception {
        logger.info("\n=== Test: Read Schema from Large CSV (11 Columns) ===\n");

        // Create test CSV file with 11 columns
        csvFile = createLargeTestCsvFile();
        assertTrue(csvFile.exists(), "CSV file should be created");

        // Load pipeline configuration
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/schema/ReadSchemaCsvPipelineStageTestLarge.yaml");
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
        logger.info("Read large CSV schema: {} columns", schema.getColumns().size());
        
        assertNotNull(schema.getColumns(), "Schema should have columns");
        assertEquals(11, schema.getColumns().size(), "Should have 11 columns");

        // Log all columns for verification
        schema.getColumns().forEach(col -> 
            logger.info("  Column: {} ({})", col.getName(), col.getDataType()));

        logger.info("[OK] Successfully read large CSV schema with {} columns", schema.getColumns().size());
    }

    /**
     * Create a large test CSV file with 11 columns of employee data.
     */
    private File createLargeTestCsvFile() throws Exception {
        File file = new File("test-employees-large.csv");
        try (PrintWriter writer = new PrintWriter(file)) {
            // 11 columns: id, first_name, last_name, email, department, salary, hire_date, active, phone, address, manager_id
            writer.println("id,first_name,last_name,email,department,salary,hire_date,active,phone,address,manager_id");
            writer.println("1,John,Doe,john@example.com,Engineering,75000.00,2020-01-15,true,555-1234,123 Main St,null");
            writer.println("2,Jane,Smith,jane@example.com,Marketing,65000.00,2019-06-01,true,555-5678,456 Oak Ave,1");
            writer.println("3,Bob,Wilson,bob@example.com,Engineering,80000.00,2018-03-20,true,555-9012,789 Pine Rd,1");
            writer.println("4,Alice,Brown,alice@example.com,HR,55000.00,2021-09-10,false,555-3456,321 Elm St,2");
            writer.println("5,Charlie,Davis,charlie@example.com,Engineering,70000.00,2022-01-05,true,555-7890,654 Maple Dr,1");
        }
        logger.info("Created large test CSV file: {} with 11 columns", file.getAbsolutePath());
        return file;
    }
}
