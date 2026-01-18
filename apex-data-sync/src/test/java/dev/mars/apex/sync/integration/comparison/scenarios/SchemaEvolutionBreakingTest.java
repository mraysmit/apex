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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests schema evolution breaking change detection.
 * Detects breaking changes when evolving database schema.
 *
 * <p><b>CRITICAL VALIDATION CHECKLIST:</b></p>
 * <ul>
 *   <li>✅ Extends SyncTestBase (provides APEX service setup/teardown)</li>
 *   <li>✅ Uses ColoredTestOutputExtension (via SyncTestBase)</li>
 *   <li>✅ Validates legacy schema reading</li>
 *   <li>✅ Validates new schema reading</li>
 *   <li>✅ Detects breaking changes (removed columns, incompatible types)</li>
 *   <li>✅ Proper cleanup of test resources</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class SchemaEvolutionBreakingTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(SchemaEvolutionBreakingTest.class);
    
    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUpTestData() throws Exception {
        // Setup legacy and new database schemas with breaking changes
        String dbUrl = "jdbc:h2:mem:schema_evolution;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (Connection conn = DriverManager.getConnection(dbUrl, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                // Legacy schema (original)
            // Drop tables if they exist from previous test run (DB_CLOSE_DELAY=-1 keeps DB alive)
            stmt.execute("DROP TABLE IF EXISTS LEGACY_CUSTOMERS");
            stmt.execute("DROP TABLE IF EXISTS NEW_CUSTOMERS");

                stmt.execute("CREATE TABLE LEGACY_CUSTOMERS (id INT, name VARCHAR(255), email VARCHAR(255), phone VARCHAR(20), status VARCHAR(50))");
                
                // New schema with BREAKING CHANGES:
                // - Removed 'phone' column (breaking!)
                // - Changed 'status' from VARCHAR to INT (breaking!)
                stmt.execute("CREATE TABLE NEW_CUSTOMERS (id INT, name VARCHAR(255), email VARCHAR(255), status INT)");
            }
        }
        logger.info("Created legacy and new database schemas with breaking changes");
    }

    @AfterEach
    public void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    @DisplayName("Should detect breaking changes in schema evolution")
    public void shouldDetectBreakingChanges() throws Exception {
        // Load configuration from Java test directory (APEX naming convention)
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/validation/SchemaEvolutionBreakingTest.yaml");
        assertNotNull(rulesEngine, "RulesEngine should be initialized");

        // Execute the pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertNotNull(result, "RuleResult should not be null");

        // Validate execution - should FAIL because breaking changes were detected
        logger.info("Pipeline execution completed");
        logger.info("Overall status: {}", result.isSuccess() ? "SUCCESS" : "FAILURE");
        
        if (!result.isSuccess()) {
            logger.info("Pipeline correctly failed due to breaking changes: {}", result.getMessage());
            for (ExecutionStep step : result.getExecutionPath()) {
                logger.info("  Step: {} - Status: {} - Message: {}", 
                    step.getName(), step.getStatus(), step.getMessage());
            }
        }
        
        assertFalse(result.isSuccess(), "Pipeline should fail when breaking changes are detected: " + result.getMessage());
        assertTrue(result.getMessage().contains("failures") || result.getMessage().contains("failed"), 
                  "Failure message should indicate pipeline failure: " + result.getMessage());

        // Verify pipeline steps
        List<ExecutionStep> pipelineSteps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertEquals(3, pipelineSteps.size(), "Should have 3 pipeline steps");

        // Verify read-legacy-schema step
        ExecutionStep readLegacyStep = pipelineSteps.stream()
            .filter(step -> step.getName().contains("legacy-schema"))
            .findFirst()
            .orElse(null);
        assertNotNull(readLegacyStep, "Should have read-legacy-schema step");
        assertTrue(readLegacyStep.hasStepData(), "Legacy schema step should have data");
        assertInstanceOf(SchemaMetadata.class, readLegacyStep.getStepData(), "Legacy data should be SchemaMetadata");
        
        SchemaMetadata legacySchema = (SchemaMetadata) readLegacyStep.getStepData();
        assertEquals(5, legacySchema.getColumns().size(), "Legacy schema should have 5 columns");

        // Verify read-new-schema step
        ExecutionStep readNewStep = pipelineSteps.stream()
            .filter(step -> step.getName().contains("new-schema"))
            .findFirst()
            .orElse(null);
        assertNotNull(readNewStep, "Should have read-new-schema step");
        assertTrue(readNewStep.hasStepData(), "New schema step should have data");
        assertInstanceOf(SchemaMetadata.class, readNewStep.getStepData(), "New data should be SchemaMetadata");

        SchemaMetadata newSchema = (SchemaMetadata) readNewStep.getStepData();
        assertEquals(4, newSchema.getColumns().size(), "New schema should have 4 columns (removed phone)");

        // Verify validate-schema-evolution step (should detect breaking changes)
        ExecutionStep validateStep = pipelineSteps.stream()
            .filter(step -> step.getName().contains("schema-evolution"))
            .findFirst()
            .orElse(null);
        assertNotNull(validateStep, "Should have validate-schema-evolution step");

        logger.info("Schema evolution breaking change detection completed");
    }
}
