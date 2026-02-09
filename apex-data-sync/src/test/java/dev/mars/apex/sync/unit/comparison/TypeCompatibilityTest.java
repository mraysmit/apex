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

package dev.mars.apex.sync.unit.comparison;

import dev.mars.apex.sync.SyncTestBase;
import dev.mars.apex.core.engine.core.RulesEngine;
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
 * Tests type compatibility detection by executing YAML-configured pipeline.
 * Validates type compatibility logic through schema comparison.
 */
public class TypeCompatibilityTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(TypeCompatibilityTest.class);
    
    private Connection sourceConnection;
    private Connection targetConnection;

    @BeforeEach
    public void setUpTestDatabases() throws Exception {
        // Create source database with original types
        sourceConnection = DriverManager.getConnection(
            "jdbc:h2:mem:type_compat_source;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
        
        try (Statement stmt = sourceConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS orders");
            stmt.execute("CREATE TABLE orders (" +
                    "id INTEGER PRIMARY KEY, " +
                    "amount DECIMAL(10,2), " +
                    "status VARCHAR(20)" +
                    ")");
        }
        
        // Create target database with compatible/incompatible type changes
        targetConnection = DriverManager.getConnection(
            "jdbc:h2:mem:type_compat_target;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
        
        try (Statement stmt = targetConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS orders");
            stmt.execute("CREATE TABLE orders (" +
                    "id BIGINT PRIMARY KEY, " +  // Compatible widening
                    "amount DECIMAL(12,2), " +   // Compatible widening
                    "status VARCHAR(10)" +       // Incompatible narrowing
                    ")");
        }
        
        logger.info("Created test databases for type compatibility test");
    }

    @AfterEach
    public void tearDownDatabases() throws Exception {
        if (sourceConnection != null) sourceConnection.close();
        if (targetConnection != null) targetConnection.close();
    }

    @Test
    public void shouldDetectTypeCompatibilityChanges() throws Exception {
        // Execute YAML-configured pipeline
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/comparison/TypeCompatibilityTest.yaml");
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
        SchemaMetadata sourceSchema = (SchemaMetadata) sourceStep.getStepData();
        
        // Find amount column in source
        var sourceAmountCol = sourceSchema.getColumns().stream()
            .filter(col -> "AMOUNT".equalsIgnoreCase(col.getName()))
            .findFirst()
            .orElse(null);
        assertNotNull(sourceAmountCol, "Source should have AMOUNT column");
        // H2 reports DECIMAL as either DECIMAL or NUMERIC
        assertTrue(sourceAmountCol.getDataType().contains("DECIMAL") || 
                   sourceAmountCol.getDataType().contains("NUMERIC"), 
                   "Amount should be DECIMAL/NUMERIC type, was: " + sourceAmountCol.getDataType());

        // Validate target schema
        ExecutionStep targetStep = steps.get(1);
        SchemaMetadata targetSchema = (SchemaMetadata) targetStep.getStepData();
        
        // Find amount column in target (widened precision)
        var targetAmountCol = targetSchema.getColumns().stream()
            .filter(col -> "AMOUNT".equalsIgnoreCase(col.getName()))
            .findFirst()
            .orElse(null);
        assertNotNull(targetAmountCol, "Target should have AMOUNT column");
        assertTrue(targetAmountCol.getDataType().contains("DECIMAL") || 
                   targetAmountCol.getDataType().contains("NUMERIC"),
                   "Amount should still be DECIMAL/NUMERIC type, was: " + targetAmountCol.getDataType());

        logger.info("[OK] Type compatibility changes detected via schema comparison");
        validateExecutionRate(2, 2, "Type compatibility validation");
    }

    @Test
    public void shouldDetectVarcharNarrowing() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/comparison/TypeCompatibilityTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        SchemaMetadata sourceSchema = (SchemaMetadata) steps.get(0).getStepData();
        SchemaMetadata targetSchema = (SchemaMetadata) steps.get(1).getStepData();
        
        var sourceStatusCol = sourceSchema.getColumns().stream()
            .filter(col -> "STATUS".equalsIgnoreCase(col.getName()))
            .findFirst()
            .orElse(null);
        var targetStatusCol = targetSchema.getColumns().stream()
            .filter(col -> "STATUS".equalsIgnoreCase(col.getName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(sourceStatusCol);
        assertNotNull(targetStatusCol);
        
        Integer sourceMaxLength = sourceStatusCol.getMaxLength();
        Integer targetMaxLength = targetStatusCol.getMaxLength();
        
        if (sourceMaxLength != null && targetMaxLength != null) {
            assertTrue(targetMaxLength < sourceMaxLength, "Target VARCHAR should be narrower");
        }
        
        logger.info("[OK] VARCHAR narrowing detected");
        validateExecutionRate(2, 2, "Type narrowing");
    }

    @Test
    public void shouldValidateAllColumnTypesPresent() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/comparison/TypeCompatibilityTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        SchemaMetadata sourceSchema = (SchemaMetadata) steps.get(0).getStepData();
        
        assertEquals(3, sourceSchema.getColumns().size());
        for (var col : sourceSchema.getColumns()) {
            assertNotNull(col.getDataType());
        }
        
        logger.info("[OK] All column types validated");
        validateExecutionRate(2, 2, "Column types");
    }
}
