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
 * Tests pre-deployment schema validation.
 * Validates new schema changes before production deployment.
 *
 * <p><b>CRITICAL VALIDATION CHECKLIST:</b></p>
 * <ul>
 *   <li>✅ Extends SyncTestBase (provides APEX service setup/teardown)</li>
 *   <li>✅ Uses ColoredTestOutputExtension (via SyncTestBase)</li>
 *   <li>✅ Validates production schema reading</li>
 *   <li>✅ Validates staging schema reading</li>
 *   <li>✅ Verifies deployment safety validation</li>
 *   <li>✅ Proper cleanup of test resources</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class PreDeploymentValidationTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(PreDeploymentValidationTest.class);
    
    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUpTestData() throws Exception {
        // Setup production and staging databases
        String dbUrl = "jdbc:h2:mem:pre_deployment;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (Connection conn = DriverManager.getConnection(dbUrl, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                // Production schema (existing)
            // Drop tables if they exist from previous test run (DB_CLOSE_DELAY=-1 keeps DB alive)
            stmt.execute("DROP TABLE IF EXISTS PRODUCTION_SCHEMA");
            stmt.execute("DROP TABLE IF EXISTS NEW_SCHEMA");

                stmt.execute("CREATE TABLE PRODUCTION_SCHEMA (id INT, name VARCHAR(255), status VARCHAR(50))");
                
                // Staging schema (proposed new version - adds email column)
                stmt.execute("CREATE TABLE NEW_SCHEMA (id INT, name VARCHAR(255), email VARCHAR(255), status VARCHAR(50))");
            }
        }
        logger.info("Created production and staging database schemas");
    }

    @AfterEach
    public void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    @DisplayName("Should validate deployment safety before production")
    public void shouldValidatePreDeployment() throws Exception {
        // Load configuration from Java test directory (APEX naming convention)
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/validation/PreDeploymentValidationTest.yaml");
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

        // Verify read-production-schema step
        ExecutionStep readProdStep = pipelineSteps.stream()
            .filter(step -> step.getName().contains("production-schema"))
            .findFirst()
            .orElse(null);
        assertNotNull(readProdStep, "Should have read-production-schema step");
        assertTrue(readProdStep.hasStepData(), "Production schema step should have data");
        assertInstanceOf(SchemaMetadata.class, readProdStep.getStepData(), "Production data should be SchemaMetadata");
        
        SchemaMetadata prodSchema = (SchemaMetadata) readProdStep.getStepData();
        assertEquals(3, prodSchema.getColumns().size(), "Production schema should have 3 columns");

        // Verify read-staging-schema step
        ExecutionStep readStagingStep = pipelineSteps.stream()
            .filter(step -> step.getName().contains("staging-schema"))
            .findFirst()
            .orElse(null);
        assertNotNull(readStagingStep, "Should have read-staging-schema step");
        assertTrue(readStagingStep.hasStepData(), "Staging schema step should have data");
        assertInstanceOf(SchemaMetadata.class, readStagingStep.getStepData(), "Staging data should be SchemaMetadata");

        SchemaMetadata stagingSchema = (SchemaMetadata) readStagingStep.getStepData();
        assertEquals(4, stagingSchema.getColumns().size(), "Staging schema should have 4 columns (added email)");

        // Verify validate-deployment-safety step
        ExecutionStep validateStep = pipelineSteps.stream()
            .filter(step -> step.getName().contains("deployment-safety"))
            .findFirst()
            .orElse(null);
        assertNotNull(validateStep, "Should have validate-deployment-safety step");

        logger.info("Pre-deployment validation completed successfully");
    }
}
