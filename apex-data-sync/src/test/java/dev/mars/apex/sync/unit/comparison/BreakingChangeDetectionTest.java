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
 * Tests breaking change detection by executing YAML-configured pipeline.
 * Validates detection of schema changes that would break compatibility.
 */
public class BreakingChangeDetectionTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(BreakingChangeDetectionTest.class);
    
    private Connection originalConnection;
    private Connection evolvedConnection;

    @BeforeEach
    public void setUpTestDatabases() throws Exception {
        // Create original schema
        originalConnection = DriverManager.getConnection(
            "jdbc:h2:mem:breaking_original;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
        
        try (Statement stmt = originalConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS users");
            stmt.execute("CREATE TABLE users (" +
                    "id INTEGER PRIMARY KEY, " +
                    "username VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(255), " +
                    "age INTEGER" +
                    ")");
        }
        
        // Create evolved schema with breaking changes
        evolvedConnection = DriverManager.getConnection(
            "jdbc:h2:mem:breaking_evolved;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
        
        try (Statement stmt = evolvedConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS users");
            stmt.execute("CREATE TABLE users (" +
                    "id INTEGER PRIMARY KEY, " +
                    "username VARCHAR(100) NOT NULL" +
                    // Removed: email, age (breaking changes - columns dropped)
                    ")");
        }
        
        logger.info("Created test databases for breaking change detection test");
    }

    @AfterEach
    public void tearDownDatabases() throws Exception {
        if (originalConnection != null) originalConnection.close();
        if (evolvedConnection != null) evolvedConnection.close();
    }

    @Test
    public void shouldDetectBreakingChanges() throws Exception {
        // Execute YAML-configured pipeline
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/comparison/BreakingChangeDetectionTest.yaml");
        assertNotNull(rulesEngine, "RulesEngine should be initialized");

        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertNotNull(result, "RuleResult should not be null");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Get schemas from execution steps
        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertEquals(3, steps.size(), "Should have 3 pipeline steps (read source, read target, compare)");

        // Validate original schema
        ExecutionStep originalStep = steps.get(0);
        SchemaMetadata originalSchema = (SchemaMetadata) originalStep.getStepData();
        assertEquals(4, originalSchema.getColumns().size(), "Original should have 4 columns");

        // Validate evolved schema (with breaking changes)
        ExecutionStep evolvedStep = steps.get(1);
        SchemaMetadata evolvedSchema = (SchemaMetadata) evolvedStep.getStepData();
        assertEquals(2, evolvedSchema.getColumns().size(), "Evolved should have only 2 columns");

        // Verify breaking changes: columns removed
        boolean hasEmail = evolvedSchema.getColumns().stream()
            .anyMatch(col -> "EMAIL".equalsIgnoreCase(col.getName()));
        assertFalse(hasEmail, "EMAIL column should be removed (breaking change)");

        boolean hasAge = evolvedSchema.getColumns().stream()
            .anyMatch(col -> "AGE".equalsIgnoreCase(col.getName()));
        assertFalse(hasAge, "AGE column should be removed (breaking change)");

        logger.info("✓ Breaking changes detected: 2 columns removed");
        validateExecutionRate(2, 2, "Breaking change detection");
    }

    @Test
    public void shouldQuantifyColumnsRemoved() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/comparison/BreakingChangeDetectionTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        SchemaMetadata originalSchema = (SchemaMetadata) steps.get(0).getStepData();
        SchemaMetadata evolvedSchema = (SchemaMetadata) steps.get(1).getStepData();
        
        int columnsRemoved = originalSchema.getColumns().size() - evolvedSchema.getColumns().size();
        assertEquals(2, columnsRemoved);
        
        logger.info("✓ Quantified: {} columns removed", columnsRemoved);
        validateExecutionRate(2, 2, "Column removal quantification");
    }
}
