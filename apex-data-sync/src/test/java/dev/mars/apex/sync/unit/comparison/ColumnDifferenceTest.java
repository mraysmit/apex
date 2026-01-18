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
 * Tests column-level difference detection via YAML-configured pipeline.
 */
public class ColumnDifferenceTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(ColumnDifferenceTest.class);
    
    private Connection beforeConnection;
    private Connection afterConnection;

    @BeforeEach
    public void setUpTestDatabases() throws Exception {
        beforeConnection = DriverManager.getConnection(
            "jdbc:h2:mem:col_diff_before;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
        
        try (Statement stmt = beforeConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS accounts");
            stmt.execute("CREATE TABLE accounts (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "balance DECIMAL(15,2)" +
                    ")");
        }
        
        afterConnection = DriverManager.getConnection(
            "jdbc:h2:mem:col_diff_after;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
        
        try (Statement stmt = afterConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS accounts");
            stmt.execute("CREATE TABLE accounts (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name VARCHAR(200) NOT NULL, " +
                    "balance DECIMAL(15,2), " +
                    "created_at TIMESTAMP" +
                    ")");
        }
        
        logger.info("Created test databases for column difference test");
    }

    @AfterEach
    public void tearDownDatabases() throws Exception {
        if (beforeConnection != null) beforeConnection.close();
        if (afterConnection != null) afterConnection.close();
    }

    @Test
    public void shouldDetectColumnSizeIncrease() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/comparison/ColumnDifferenceTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        SchemaMetadata beforeSchema = (SchemaMetadata) steps.get(0).getStepData();
        SchemaMetadata afterSchema = (SchemaMetadata) steps.get(1).getStepData();
        
        var beforeName = beforeSchema.getColumns().stream()
            .filter(col -> "NAME".equalsIgnoreCase(col.getName()))
            .findFirst()
            .orElse(null);
        var afterName = afterSchema.getColumns().stream()
            .filter(col -> "NAME".equalsIgnoreCase(col.getName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(beforeName);
        assertNotNull(afterName);
        
        if (beforeName.getMaxLength() != null && afterName.getMaxLength() != null) {
            assertTrue(afterName.getMaxLength() > beforeName.getMaxLength(), 
                "Name column should be wider in after schema");
        }
        
        logger.info("✓ Column size increase detected");
        validateExecutionRate(2, 2, "Column size increase");
    }

    @Test
    public void shouldDetectNewColumn() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/comparison/ColumnDifferenceTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        SchemaMetadata beforeSchema = (SchemaMetadata) steps.get(0).getStepData();
        SchemaMetadata afterSchema = (SchemaMetadata) steps.get(1).getStepData();
        
        assertEquals(3, beforeSchema.getColumns().size());
        assertEquals(4, afterSchema.getColumns().size());
        
        boolean hasCreatedAt = afterSchema.getColumns().stream()
            .anyMatch(col -> "CREATED_AT".equalsIgnoreCase(col.getName()));
        assertTrue(hasCreatedAt, "After schema should have created_at column");
        
        logger.info("✓ New column detected");
        validateExecutionRate(2, 2, "New column detection");
    }

    @Test
    public void shouldCompareColumnCounts() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/comparison/ColumnDifferenceTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        SchemaMetadata beforeSchema = (SchemaMetadata) steps.get(0).getStepData();
        SchemaMetadata afterSchema = (SchemaMetadata) steps.get(1).getStepData();
        
        int columnDifference = afterSchema.getColumns().size() - beforeSchema.getColumns().size();
        assertEquals(1, columnDifference, "Should have 1 additional column");
        
        logger.info("✓ Column count comparison: {} → {}", 
            beforeSchema.getColumns().size(), afterSchema.getColumns().size());
        validateExecutionRate(2, 2, "Column count comparison");
    }
}
