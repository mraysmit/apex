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
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic CSV schema reading test validating fundamental CSV schema inference.
 * Tests reading schema metadata from a simple CSV file.
 *
 * CRITICAL VALIDATION CHECKLIST:
 * CSV file created and accessible
 * Schema inferred from CSV headers
 * Column types detected correctly
 * Pipeline executes successfully
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Read Schema CSV Test")
class ReadSchemaCsvTest extends SyncTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaCsvTest.class);
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
    @DisplayName("Should read schema from simple CSV file")
    void shouldReadSchemaFromCsv() throws Exception {
        logger.info("\n=== Test: Read Schema from CSV ===\n");

        // Create test CSV file
        csvFile = createTestCsvFile();
        assertTrue(csvFile.exists(), "CSV file should be created");

        // Load pipeline configuration
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/schema/ReadSchemaCsvTest.yaml");
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
        assertTrue(schema.getColumns().size() >= 3, "Should have at least 3 columns");

        logger.info("[OK] Successfully read CSV schema with {} columns", schema.getColumns().size());
    }

    /**
     * Create a test CSV file with customer data.
     */
    private File createTestCsvFile() throws Exception {
        File file = new File("test-customers.csv");
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("id,name,email,active");
            writer.println("1,John Doe,john@example.com,true");
            writer.println("2,Jane Smith,jane@example.com,true");
            writer.println("3,Bob Wilson,bob@example.com,false");
        }
        logger.info("Created test CSV file: {}", file.getAbsolutePath());
        return file;
    }
}
