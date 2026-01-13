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
 * Detects incompatible type narrowing changes during migration.
 * Tests scenarios where column types are reduced (e.g., VARCHAR(100) to VARCHAR(50))
 * which can cause data truncation and are flagged as breaking changes.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Schema Diff: Incompatible Type Narrowing")
class SchemaDiffMigrationValidationTest_TypeNarrowing {

    private static final Logger logger = LoggerFactory.getLogger(SchemaDiffMigrationValidationTest_TypeNarrowing.class);
    private static final String H2_SOURCE_URL = "jdbc:h2:mem:migration_source_narrowing;DB_CLOSE_DELAY=-1";
    private static final String H2_TARGET_URL = "jdbc:h2:mem:migration_target_narrowing;DB_CLOSE_DELAY=-1";
    
    private RulesEngine rulesEngine;

    @BeforeEach
    void setUp() throws Exception {
        logger.info("=== Setting up Type Narrowing Detection Test ===");
        setupSourceSchema_v1();
        setupTargetSchema_v5_TypeNarrowing();
    }

    @AfterEach
    void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    @DisplayName("Should detect incompatible type narrowing as breaking changes")
    void shouldDetectIncompatibleTypeChanges() throws Exception {
        logger.info("\n=== Test: Incompatible Type Narrowing (VARCHAR 100→50) ===\n");
        
        // Execute schema diff pipeline
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/SchemaDiffDatabaseMigrationValidationTest_TypeNarrowing.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Verify pipeline execution
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        // Verify comparison result
        SchemaComparisonResult comparison = extractComparisonResult(result);
        assertNotNull(comparison, "Should have comparison result");
        
        // Type narrowing should be flagged
        assertFalse(comparison.isCompatible(), "Type narrowing should be incompatible");
        assertTrue(comparison.getBreakingChanges().size() > 0, "Should have breaking changes");
        
        // Should detect changed columns
        assertTrue(comparison.getChangedColumns().size() > 0, "Should detect type changes");
        
        // Verify breaking changes mention type issues
        String breakingChangesStr = String.join(", ", comparison.getBreakingChanges()).toLowerCase();
        assertTrue(breakingChangesStr.contains("type") || breakingChangesStr.contains("narrow") || 
                   breakingChangesStr.contains("incompatible"),
            "Breaking changes should mention type incompatibility");
        
        // Verify HTML report generated
        Path reportPath = Path.of("target/reports/type-narrowing-migration-report.html");
        assertTrue(Files.exists(reportPath), "HTML report should be generated");
        
        String reportContent = Files.readString(reportPath);
        assertTrue(reportContent.contains("Changed Columns") || reportContent.contains("changed"),
            "Report should have changed columns section");
        
        logger.info("✓ Incompatible type narrowing detected");
        logger.info("✓ VARCHAR narrowing can cause data truncation - migration requires data validation");
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
            
            logger.info("Created source schema v1: VARCHAR(100)");
        }
    }

    private void setupTargetSchema_v5_TypeNarrowing() throws Exception {
        try (Connection conn = DriverManager.getConnection(H2_TARGET_URL, "sa", "");
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("DROP TABLE IF EXISTS customers");
            stmt.execute("""
                CREATE TABLE customers (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(50) NOT NULL,
                    email VARCHAR(50),
                    created_date DATE
                )
            """);
            
            logger.info("Created target schema v5: VARCHAR(50) - type narrowing applied (potential data loss)");
        }
    }
}
