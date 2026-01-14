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
 * Validates compatible type widening changes during migration.
 * Tests scenarios where column types are expanded (e.g., VARCHAR(50) to VARCHAR(100),
 * DATE to TIMESTAMP) which are typically safe, backward-compatible changes.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Schema Diff: Compatible Type Widening")
class SchemaDiffMigrationValidationTest_TypeWidening {

    private static final Logger logger = LoggerFactory.getLogger(SchemaDiffMigrationValidationTest_TypeWidening.class);
    private static final String H2_SOURCE_URL = "jdbc:h2:mem:migration_source_widening;DB_CLOSE_DELAY=-1";
    private static final String H2_TARGET_URL = "jdbc:h2:mem:migration_target_widening;DB_CLOSE_DELAY=-1";
    
    private RulesEngine rulesEngine;

    @BeforeEach
    void setUp() throws Exception {
        logger.info("=== Setting up Type Widening Validation Test ===");
        setupSourceSchema_v1();
        setupTargetSchema_v4_TypeWidening();
    }

    @AfterEach
    void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    @DisplayName("Should validate compatible type widening changes")
    void shouldValidateCompatibleTypeChanges() throws Exception {
        logger.info("\n=== Test: Compatible Type Widening (VARCHAR 100→200, DATE→TIMESTAMP) ===\n");
        
        // Execute schema diff pipeline
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/validation/SchemaDiffDatabaseMigrationValidationTest_TypeWidening.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Verify pipeline execution
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        // Verify comparison result
        SchemaComparisonResult comparison = extractComparisonResult(result);
        assertNotNull(comparison, "Should have comparison result");
        
        // Type widening may be detected as changed columns
        assertTrue(comparison.getChangedColumns().size() > 0, "Should detect type changes");
        assertEquals(4, comparison.getMatchingColumns().size() + comparison.getChangedColumns().size(), 
            "Should have 4 total columns");
        
        // Should be compatible despite type changes
        assertTrue(comparison.isCompatible(), "Type widening should be compatible");
        
        // May have warnings but should not have breaking changes that prevent migration
        // (Breaking changes list might be empty or contain only warnings)
        logger.info("Changed columns: {}", comparison.getChangedColumns().size());
        logger.info("Breaking changes: {}", comparison.getBreakingChanges().size());
        
        // Verify HTML report generated
        Path reportPath = Path.of("target/reports/type-widening-migration-report.html");
        assertTrue(Files.exists(reportPath), "HTML report should be generated");
        
        String reportContent = Files.readString(reportPath);
        assertTrue(reportContent.contains("name") || reportContent.contains("NAME"), 
            "Report should mention name column");
        
        logger.info("✓ Compatible type widening validated");
        logger.info("✓ VARCHAR widening and DATE→TIMESTAMP are safe migrations");
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
            
            logger.info("Created source schema v1: VARCHAR(100), DATE");
        }
    }

    private void setupTargetSchema_v4_TypeWidening() throws Exception {
        try (Connection conn = DriverManager.getConnection(H2_TARGET_URL, "sa", "");
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("DROP TABLE IF EXISTS customers");
            stmt.execute("""
                CREATE TABLE customers (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(200) NOT NULL,
                    email VARCHAR(200),
                    created_date TIMESTAMP
                )
            """);
            
            logger.info("Created target schema v4: VARCHAR(200), TIMESTAMP - type widening applied");
        }
    }
}
