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
 * Created: 2026-01-13
 */
package dev.mars.apex.sync;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.schema.diff.SchemaComparisonResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Detects breaking changes when columns are removed during migration.
 * Tests that removed columns are properly flagged as incompatible changes
 * that could break existing applications expecting those columns.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Schema Diff: Breaking Changes - Removed Columns")
class SchemaDiffMigrationValidationTest_RemovedColumns {

    private static final Logger logger = LoggerFactory.getLogger(SchemaDiffMigrationValidationTest_RemovedColumns.class);
    private static final String H2_SOURCE_URL = "jdbc:h2:mem:migration_source_removed;DB_CLOSE_DELAY=-1";
    private static final String H2_TARGET_URL = "jdbc:h2:mem:migration_target_removed;DB_CLOSE_DELAY=-1";
    
    private RulesEngine rulesEngine;

    @BeforeEach
    void setUp() throws Exception {
        logger.info("=== Setting up Breaking Changes Detection Test ===");
        setupSourceSchema_v1();
        setupTargetSchema_v3_RemovedColumns();
    }

    @AfterEach
    void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    @DisplayName("Should detect removed columns as breaking changes")
    void shouldDetectBreakingChanges_RemovedColumns() throws Exception {
        logger.info("\n=== Test: Breaking Changes (Removed Columns) ===\n");
        
        // Execute schema diff pipeline
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/SchemaDiffDatabaseMigrationValidationTest_RemovedColumns.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Pipeline should execute successfully
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        // Verify comparison result
        SchemaComparisonResult comparison = extractComparisonResult(result);
        assertNotNull(comparison, "Should have comparison result");
        
        // Should detect removed columns
        assertEquals(2, comparison.getRemovedColumns().size(), "Should detect 2 removed columns (email, created_date)");
        assertEquals(2, comparison.getMatchingColumns().size(), "Should have 2 matching columns (id, name)");
        assertEquals(0, comparison.getAddedColumns().size(), "Should have no added columns");
        
        // Verify removed column names
        var removedNames = comparison.getRemovedColumns().stream()
            .map(col -> col.getColumnName().toLowerCase())
            .toList();
        assertTrue(removedNames.contains("email"), "Should detect email column removal");
        assertTrue(removedNames.contains("created_date"), "Should detect created_date column removal");
        
        // Should be incompatible (breaking migration)
        assertFalse(comparison.isCompatible(), "Removing columns should be incompatible");
        assertTrue(comparison.getBreakingChanges().size() >= 2, "Should have at least 2 breaking changes");
        
        // Verify breaking changes contain column names
        String breakingChangesStr = String.join(", ", comparison.getBreakingChanges()).toLowerCase();
        assertTrue(breakingChangesStr.contains("removed") || breakingChangesStr.contains("email"), 
            "Breaking changes should mention removed columns");
        
        // Verify HTML report generated
        Path reportPath = Path.of("target/reports/breaking-changes-migration-report.html");
        assertTrue(Files.exists(reportPath), "HTML report should be generated");
        
        String reportContent = Files.readString(reportPath);
        assertTrue(reportContent.contains("email") || reportContent.contains("EMAIL"), 
            "Report should mention email column");
        assertTrue(reportContent.contains("Removed Columns"), "Report should have removed columns section");
        
        logger.info("✓ Breaking changes detected: {} removed columns flagged", comparison.getRemovedColumns().size());
        logger.info("✓ Migration is NOT backward compatible - existing applications may break");
    }

    private SchemaComparisonResult extractComparisonResult(RuleResult result) {
        var step = result.getExecutionPath().stream()
            .filter(s -> "PIPELINE_STEP".equals(s.getType()))
            .filter(s -> s.getName().contains("compare") || s.getName().contains("validate"))
            .findFirst()
            .orElse(null);

        assertNotNull(step, "Should have schema-diff step");
        assertTrue(step.hasStepData(), "Step should have comparison result");

        Object stepData = step.getStepData();
        assertInstanceOf(SchemaComparisonResult.class, stepData);

        return (SchemaComparisonResult) stepData;
    }

    private void setupSourceSchema_v1() throws Exception {
        try (Connection conn = DriverManager.getConnection(H2_SOURCE_URL, "sa", "");
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("DROP TABLE IF EXISTS customers");
            stmt.execute("""
                CREATE TABLE customers (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100),
                    created_date DATE
                )
            """);
            
            logger.info("Created source schema v1: customers table with 4 columns");
        }
    }

    private void setupTargetSchema_v3_RemovedColumns() throws Exception {
        try (Connection conn = DriverManager.getConnection(H2_TARGET_URL, "sa", "");
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("DROP TABLE IF EXISTS customers");
            stmt.execute("""
                CREATE TABLE customers (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(100) NOT NULL
                )
            """);
            
            logger.info("Created target schema v3: customers table with 2 columns (email, created_date removed)");
        }
    }
}
