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

import dev.mars.apex.sync.SyncTestBase;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.schema.diff.SchemaComparisonResult;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.DockerClientFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import dev.mars.apex.sync.TestContainerImages;

/**
 * Validates safe schema evolution when adding nullable columns during migration.
 * Tests backward-compatible schema changes using real PostgreSQL databases via Testcontainers.
 *
 * CRITICAL VALIDATION CHECKLIST:
 * ✅ Docker availability - Gracefully skip if Docker not available
 * ✅ Schema compatibility - Added nullable columns should be compatible
 * ✅ Migration safety - Backward-compatible changes should pass validation
 * ✅ Schema comparison - Source v1 vs Target v2 (with added columns)
 * ✅ Report generation - HTML report with migration analysis
 *
 * This test uses REAL PostgreSQL via Testcontainers - NO MOCKING
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@Testcontainers
@DisplayName("Schema Diff: Safe Evolution - Added Columns (Testcontainers)")
class SchemaDiffMigrationValidationTest_AddedColumns_Testcontainers extends SyncTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SchemaDiffMigrationValidationTest_AddedColumns_Testcontainers.class);
    
    @BeforeAll
    static void checkDockerAvailability() {
        try {
            DockerClientFactory.instance().client();
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                "Docker is not available. Skipping PostgreSQL integration tests. " +
                "To run these tests, ensure Docker is installed and running. Error: " + e.getMessage());
        }
    }
    
    @Container
    private static final PostgreSQLContainer<?> sourceDb = new PostgreSQLContainer<>(TestContainerImages.POSTGRES)
            .withDatabaseName("source_db")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    private static final PostgreSQLContainer<?> targetDb = new PostgreSQLContainer<>(TestContainerImages.POSTGRES)
            .withDatabaseName("target_db")
            .withUsername("test")
            .withPassword("test");
    
    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUpTestContainers() throws Exception {
        logger.info("=== Setting up Safe Schema Evolution Test (Testcontainers) ===");
        setupSourceSchema_v1();
        setupTargetSchema_v2_AddedColumns();
        
        // Set system properties for YAML to use (host, port, database format)
        System.setProperty("SOURCE_DB_HOST", sourceDb.getHost());
        System.setProperty("SOURCE_DB_PORT", String.valueOf(sourceDb.getFirstMappedPort()));
        System.setProperty("SOURCE_DB_NAME", sourceDb.getDatabaseName());
        System.setProperty("SOURCE_DB_USER", sourceDb.getUsername());
        System.setProperty("SOURCE_DB_PASS", sourceDb.getPassword());
        
        System.setProperty("TARGET_DB_HOST", targetDb.getHost());
        System.setProperty("TARGET_DB_PORT", String.valueOf(targetDb.getFirstMappedPort()));
        System.setProperty("TARGET_DB_NAME", targetDb.getDatabaseName());
        System.setProperty("TARGET_DB_USER", targetDb.getUsername());
        System.setProperty("TARGET_DB_PASS", targetDb.getPassword());
    }

    @AfterEach
    public void tearDownTestContainers() {
        // Call parent tearDown first
        super.tearDown();
        
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
        
        // Clear system properties
        System.clearProperty("SOURCE_DB_HOST");
        System.clearProperty("SOURCE_DB_PORT");
        System.clearProperty("SOURCE_DB_NAME");
        System.clearProperty("SOURCE_DB_USER");
        System.clearProperty("SOURCE_DB_PASS");
        System.clearProperty("TARGET_DB_HOST");
        System.clearProperty("TARGET_DB_PORT");
        System.clearProperty("TARGET_DB_NAME");
        System.clearProperty("TARGET_DB_USER");
        System.clearProperty("TARGET_DB_PASS");
    }

    @Test
    @DisplayName("Should validate safe schema evolution with added nullable columns")
    void shouldValidateSafeSchemaEvolution() throws Exception {
        logger.info("\n=== Test: Safe Schema Evolution (Added Nullable Columns) ===\n");

        // Execute schema diff pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/sync/validation/SchemaDiffDatabaseMigrationValidationTest_AddedColumns_Testcontainers.yaml");
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
        
        // Verify HTML report was generated
        Path reportPath = Path.of("target/reports/safe-evolution-migration-report-testcontainers.html");
        assertTrue(Files.exists(reportPath), "HTML report should be generated");
        logger.info("✓ HTML report generated: {}", reportPath);
        
        String reportContent = Files.readString(reportPath);
        assertTrue(reportContent.contains("phone") || reportContent.contains("status"), 
            "Report should mention added columns");
        assertTrue(reportContent.contains("Added Columns"), "Report should have added columns section");
        
        logger.info("✓ Safe schema evolution validated: 2 columns added, no breaking changes");
        logger.info("✓ Migration is backward compatible - existing applications will continue to work");
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
        try (Connection conn = DriverManager.getConnection(sourceDb.getJdbcUrl(), sourceDb.getUsername(), sourceDb.getPassword());
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

    private void setupTargetSchema_v2_AddedColumns() throws Exception {
        try (Connection conn = DriverManager.getConnection(targetDb.getJdbcUrl(), targetDb.getUsername(), targetDb.getPassword());
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
            
            logger.info("Created target schema v2: customers table with 6 columns (2 added)");
        }
    }
}
