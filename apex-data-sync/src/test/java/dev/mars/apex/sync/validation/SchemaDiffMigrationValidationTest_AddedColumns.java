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
package dev.mars.apex.sync.validation;

import dev.mars.apex.core.engine.core.RulesEngine;
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
 * Validates safe schema evolution when adding nullable columns during migration.
 * Tests backward-compatible schema changes that should not break existing applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Schema Diff: Safe Evolution - Added Columns")
class SchemaDiffMigrationValidationTest_AddedColumns {

    private static final Logger logger = LoggerFactory.getLogger(SchemaDiffMigrationValidationTest_AddedColumns.class);
    // Use file-based H2 to allow connection sharing between test code and APEX engine
    private static final String H2_SOURCE_URL = "jdbc:h2:./target/test/schema-diff/migration_source_added;DB_CLOSE_DELAY=-1";
    private static final String H2_TARGET_URL = "jdbc:h2:./target/test/schema-diff/migration_target_added;DB_CLOSE_DELAY=-1";
    
    private RulesEngine rulesEngine;

    @BeforeEach
    void setUp() throws Exception {
        logger.info("=== Setting up Safe Schema Evolution Test ===");
        setupSourceSchema_v1();
        setupTargetSchema_v2_AddedColumns();
    }

    @AfterEach
    void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    @DisplayName("Should validate safe schema evolution with added nullable columns")
    void shouldValidateSafeSchemaEvolution() throws Exception {
        logger.info("\n=== Test: Safe Schema Evolution (Added Nullable Columns) ===\n");

        // Execute schema diff pipeline
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/validation/SchemaDiffDatabaseMigrationValidationTest_AddedColumns.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Verify pipeline execution
        assertTrue(result.isSuccess(), "Migration validation should succeed");

        // Verify comparison result
        SchemaComparisonResult comparison = extractComparisonResult(result);
        assertNotNull(comparison, "Should have comparison result");
        
        // Should detect added columns
        assertEquals(2, comparison.getAddedColumns().size(), "Should detect 2 new columns (phone, status)");
        assertEquals(4, comparison.getMatchingColumns().size(), "Should have 4 matching columns");
        assertEquals(0, comparison.getRemovedColumns().size(), "Should have no removed columns");
        
        // Verify added column names
        var addedNames = comparison.getAddedColumns().stream()
            .map(col -> col.getColumnName().toLowerCase())
            .toList();
        assertTrue(addedNames.contains("phone"), "Should detect phone column");
        assertTrue(addedNames.contains("status"), "Should detect status column");
        
        // Should be compatible (safe migration)
        assertTrue(comparison.isCompatible(), "Adding nullable columns should be backward compatible");
        assertEquals(0, comparison.getBreakingChanges().size(), "Should have no breaking changes");
        
        // Verify HTML report generated
        Path reportPath = Path.of("target/reports/safe-evolution-migration-report.html");
        assertTrue(Files.exists(reportPath), "HTML report should be generated");
        
        String reportContent = Files.readString(reportPath);
        assertTrue(reportContent.contains("phone"), "Report should mention phone column");
        assertTrue(reportContent.contains("status"), "Report should mention status column");
        assertTrue(reportContent.contains("Added Columns"), "Report should have added columns section");
        
        logger.info("[OK] Safe schema evolution validated: 2 columns added, no breaking changes");
        logger.info("[OK] Migration is backward compatible - existing applications will continue to work");
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
            
            conn.commit(); // Ensure changes are committed to file-based database
            
            logger.info("Created source schema v1: customers table with 4 columns");
        }
    }

    private void setupTargetSchema_v2_AddedColumns() throws Exception {
        try (Connection conn = DriverManager.getConnection(H2_TARGET_URL, "sa", "");
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("DROP TABLE IF EXISTS customers");
            stmt.execute("""
                CREATE TABLE customers (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100),
                    created_date DATE,
                    phone VARCHAR(20),
                    status VARCHAR(50)
                )
            """);
            
            conn.commit(); // Ensure changes are committed to file-based database
            
            logger.info("Created target schema v2: customers table with 6 columns (2 added)");
        }
    }
}
