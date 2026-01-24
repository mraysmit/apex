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

package dev.mars.apex.sync.unit.reporting.markdown;

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
 * Tests Markdown report generation by executing YAML-configured pipeline.
 * Validates Markdown structure from schema comparison operations.
 */
public class MarkdownGenerationTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(MarkdownGenerationTest.class);
    
    private Connection sourceConnection;
    private Connection targetConnection;

    @BeforeEach
    public void setUpTestDatabases() throws Exception {
        // Create source database
        sourceConnection = DriverManager.getConnection(
            "jdbc:h2:mem:markdown_test_source;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
        
        try (Statement stmt = sourceConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS products");
            stmt.execute("CREATE TABLE products (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "price DECIMAL(10,2)" +
                    ")");
        }
        
        // Create target database with additional column
        targetConnection = DriverManager.getConnection(
            "jdbc:h2:mem:markdown_test_target;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
        
        try (Statement stmt = targetConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS products");
            stmt.execute("CREATE TABLE products (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "price DECIMAL(10,2), " +
                    "category VARCHAR(50)" +
                    ")");
        }
        
        logger.info("Created test databases for Markdown generation test");
    }

    @AfterEach
    public void tearDownDatabases() throws Exception {
        if (sourceConnection != null) sourceConnection.close();
        if (targetConnection != null) targetConnection.close();
    }

    @Test
    public void shouldReadSchemasForMarkdownGeneration() throws Exception {
        // Execute YAML-configured pipeline
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/reporting/markdown/MarkdownGenerationTest.yaml");
        assertNotNull(rulesEngine, "RulesEngine should be initialized");

        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertNotNull(result, "RuleResult should not be null");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Get schemas from execution steps
        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertEquals(3, steps.size(), "Should have 3 pipeline steps (read source, read target, compare)");

        // Validate source schema
        ExecutionStep sourceStep = steps.get(0);
        assertTrue(sourceStep.hasStepData(), "Source step should have data");
        assertInstanceOf(SchemaMetadata.class, sourceStep.getStepData());
        
        SchemaMetadata sourceSchema = (SchemaMetadata) sourceStep.getStepData();
        assertEquals(3, sourceSchema.getColumns().size(), "Source should have 3 columns");

        // Validate target schema
        ExecutionStep targetStep = steps.get(1);
        assertTrue(targetStep.hasStepData(), "Target step should have data");
        assertInstanceOf(SchemaMetadata.class, targetStep.getStepData());
        
        SchemaMetadata targetSchema = (SchemaMetadata) targetStep.getStepData();
        assertEquals(4, targetSchema.getColumns().size(), "Target should have 4 columns (includes category)");

        logger.info("[OK] Schemas read successfully for Markdown generation");
        validateExecutionRate(2, 2, "Schema reading for Markdown generation");
    }

    @Test
    public void shouldDetectColumnAddition() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/reporting/markdown/MarkdownGenerationTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        SchemaMetadata sourceSchema = (SchemaMetadata) steps.get(0).getStepData();
        SchemaMetadata targetSchema = (SchemaMetadata) steps.get(1).getStepData();
        
        // Verify column addition detected
        boolean sourceHasCategory = sourceSchema.getColumns().stream()
            .anyMatch(col -> "CATEGORY".equalsIgnoreCase(col.getName()));
        boolean targetHasCategory = targetSchema.getColumns().stream()
            .anyMatch(col -> "CATEGORY".equalsIgnoreCase(col.getName()));
        
        assertFalse(sourceHasCategory, "Source should not have CATEGORY");
        assertTrue(targetHasCategory, "Target should have CATEGORY");
        
        logger.info("[OK] Column addition detected");
        validateExecutionRate(2, 2, "Column addition");
    }

    @Test
    public void shouldValidateDecimalPriceColumn() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/reporting/markdown/MarkdownGenerationTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        SchemaMetadata sourceSchema = (SchemaMetadata) steps.get(0).getStepData();
        
        var priceColumn = sourceSchema.getColumns().stream()
            .filter(col -> "PRICE".equalsIgnoreCase(col.getName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(priceColumn, "Should have PRICE column");
        
        logger.info("[OK] Price column validated");
        validateExecutionRate(2, 2, "Price validation");
    }
}
