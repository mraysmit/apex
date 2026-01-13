package dev.mars.apex.sync;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.schema.diff.SchemaComparisonResult;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for schema-diff pipeline stage in apex-data-sync.
 * Tests real-world data migration scenarios.
 * 
 * USE CASES:
 * - CSV to PostgreSQL migration validation
 * - SQL Server to PostgreSQL migration validation
 * - Multi-table migration validation
 * - Breaking change detection
 * 
 * @author APEX Team
 * @since 2.1.0
 */
@DisplayName("Schema Diff Migration Validation Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SchemaDiffMigrationValidationTest {

    private static final Logger logger = LoggerFactory.getLogger(SchemaDiffMigrationValidationTest.class);
    private RulesEngine rulesEngine;

    @AfterEach
    void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Use Case: Validate CSV to PostgreSQL migration - All columns compatible")
    void validateCsvToPostgresMigration() throws Exception {
        logger.info("\n" + "=".repeat(80));
        logger.info("USE CASE: CSV to PostgreSQL Migration Validation");
        logger.info("Scenario: Legacy CSV data being migrated to PostgreSQL");
        logger.info("Expected: All columns should be compatible");
        logger.info("=".repeat(80) + "\n");

        // Setup target PostgreSQL-like database
        setupPostgresTargetDatabase();

        // Load migration validation pipeline
        rulesEngine = RulesEngine.fromFile("src/test/resources/test-csv-to-postgres-migration.yaml");

        // Execute validation
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Verify migration is valid
        assertTrue(result.isSuccess(), "Migration validation should pass");

        SchemaComparisonResult comparison = extractComparisonResult(result);
        
        logger.info("Migration Validation Results:");
        logger.info("  ✓ Matching columns: {}", comparison.getMatchingColumns().size());
        logger.info("  → Added columns: {}", comparison.getAddedColumns().size());
        logger.info("  ⚠ Removed columns: {}", comparison.getRemovedColumns().size());
        logger.info("  ⚠ Changed columns: {}", comparison.getChangedColumns().size());
        logger.info("  Compatible: {}", comparison.isCompatible() ? "YES" : "NO");

        assertTrue(comparison.isCompatible(), "Migration should be compatible");
        assertEquals(0, comparison.getBreakingChanges().size(), "Should have no breaking changes");
        
        logger.info("\n✓ CSV to PostgreSQL migration validated successfully\n");
    }

    @Test
    @Order(2)
    @DisplayName("Use Case: Detect breaking changes in database schema evolution")
    void detectBreakingChangesInSchemaEvolution() throws Exception {
        logger.info("\n" + "=".repeat(80));
        logger.info("USE CASE: Database Schema Evolution - Breaking Change Detection");
        logger.info("Scenario: Database schema updated, column removed");
        logger.info("Expected: Detect breaking change and fail validation");
        logger.info("=".repeat(80) + "\n");

        setupLegacyAndNewDatabase();

        rulesEngine = RulesEngine.fromFile("src/test/resources/test-schema-evolution-breaking.yaml");

        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Should fail due to breaking change
        assertFalse(result.isSuccess(), "Pipeline should fail on breaking change");
        
        logger.info("Breaking Change Detection:");
        logger.info("  ✓ Pipeline failed as expected");
        logger.info("  ✓ Error: {}", result.getMessage());
        
        logger.info("\n✓ Breaking change detected successfully\n");
    }

    @Test
    @Order(3)
    @DisplayName("Use Case: Multi-table migration validation")
    void validateMultiTableMigration() throws Exception {
        logger.info("\n" + "=".repeat(80));
        logger.info("USE CASE: Multi-Table Migration Validation");
        logger.info("Scenario: Migrating 3 tables from legacy to new system");
        logger.info("Expected: All table schemas should be validated");
        logger.info("=".repeat(80) + "\n");

        setupMultiTableMigrationDatabase();

        rulesEngine = RulesEngine.fromFile("src/test/resources/test-multi-table-migration.yaml");

        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        assertTrue(result.isSuccess(), "Multi-table migration should pass");

        // Count schema-diff steps
        long diffSteps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .filter(step -> step.getName().contains("compare"))
            .count();

        assertEquals(3, diffSteps, "Should have validated 3 table pairs");
        
        logger.info("Multi-Table Validation Results:");
        logger.info("  ✓ Tables validated: 3");
        logger.info("  ✓ All validations passed");
        
        logger.info("\n✓ Multi-table migration validated successfully\n");
    }

    @Test
    @Order(4)
    @DisplayName("Use Case: SQL Server to PostgreSQL migration with type mappings")
    void validateSqlServerToPostgresMigration() throws Exception {
        logger.info("\n" + "=".repeat(80));
        logger.info("USE CASE: SQL Server to PostgreSQL Migration");
        logger.info("Scenario: Cross-platform migration with type conversions");
        logger.info("Expected: Type mappings should allow compatible migration");
        logger.info("=".repeat(80) + "\n");

        setupSqlServerPostgresMigration();

        rulesEngine = RulesEngine.fromFile("src/test/resources/test-sqlserver-postgres-migration.yaml");

        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        assertTrue(result.isSuccess(), "Migration with type mappings should pass");

        SchemaComparisonResult comparison = extractComparisonResult(result);
        
        logger.info("Cross-Platform Migration Results:");
        logger.info("  ✓ Type mappings applied successfully");
        logger.info("  ✓ Matching columns: {}", comparison.getMatchingColumns().size());
        logger.info("  ✓ Compatible: {}", comparison.isCompatible());
        
        assertTrue(comparison.isCompatible(), "Should be compatible with type mappings");
        
        logger.info("\n✓ SQL Server to PostgreSQL migration validated\n");
    }

    @Test
    @Order(5)
    @DisplayName("Use Case: Pre-deployment schema validation")
    void preDeploymentSchemaValidation() throws Exception {
        logger.info("\n" + "=".repeat(80));
        logger.info("USE CASE: Pre-Deployment Schema Validation");
        logger.info("Scenario: Validate new schema before deploying to production");
        logger.info("Expected: Detect incompatible changes before deployment");
        logger.info("=".repeat(80) + "\n");

        setupPreDeploymentDatabase();

        rulesEngine = RulesEngine.fromFile("src/test/resources/test-pre-deployment-validation.yaml");

        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        SchemaComparisonResult comparison = extractComparisonResult(result);
        
        logger.info("Pre-Deployment Validation:");
        logger.info("  Schema changes detected:");
        logger.info("    - Added columns: {}", comparison.getAddedColumns().size());
        logger.info("    - Removed columns: {}", comparison.getRemovedColumns().size());
        logger.info("    - Modified columns: {}", comparison.getChangedColumns().size());
        logger.info("  Breaking changes: {}", comparison.getBreakingChanges().size());
        
        if (comparison.getBreakingChanges().isEmpty()) {
            logger.info("  ✓ Safe to deploy - no breaking changes");
        } else {
            logger.info("  ⚠ Deployment blocked - breaking changes detected:");
            comparison.getBreakingChanges().forEach(change -> 
                logger.info("      - {}", change));
        }
        
        logger.info("\n✓ Pre-deployment validation complete\n");
    }

    // Helper Methods

    private void setupPostgresTargetDatabase() throws Exception {
        Connection conn = DriverManager.getConnection(
            "jdbc:h2:mem:postgres_target;DB_CLOSE_DELAY=-1", "sa", "");
        
        Statement stmt = conn.createStatement();
        stmt.execute("DROP TABLE IF EXISTS customers");
        stmt.execute(
            "CREATE TABLE customers (" +
            "  customer_id INTEGER PRIMARY KEY, " +
            "  customer_name VARCHAR(255) NOT NULL, " +
            "  customer_email VARCHAR(255), " +
            "  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")");
        stmt.close();
        conn.close();
    }

    private void setupLegacyAndNewDatabase() throws Exception {
        Connection conn = DriverManager.getConnection(
            "jdbc:h2:mem:schema_evolution;DB_CLOSE_DELAY=-1", "sa", "");
        
        Statement stmt = conn.createStatement();
        
        // Legacy schema (has 'legacy_field')
        stmt.execute("DROP TABLE IF EXISTS legacy_customers");
        stmt.execute(
            "CREATE TABLE legacy_customers (" +
            "  id INTEGER PRIMARY KEY, " +
            "  name VARCHAR(100), " +
            "  email VARCHAR(100), " +
            "  legacy_field VARCHAR(50)" +  // This will be removed
            ")");
        
        // New schema (removed 'legacy_field')
        stmt.execute("DROP TABLE IF EXISTS new_customers");
        stmt.execute(
            "CREATE TABLE new_customers (" +
            "  id INTEGER PRIMARY KEY, " +
            "  name VARCHAR(100), " +
            "  email VARCHAR(100)" +
            ")");
        
        stmt.close();
        conn.close();
    }

    private void setupMultiTableMigrationDatabase() throws Exception {
        Connection conn = DriverManager.getConnection(
            "jdbc:h2:mem:multi_table_migration;DB_CLOSE_DELAY=-1", "sa", "");
        
        Statement stmt = conn.createStatement();
        
        // Table 1: customers
        stmt.execute("DROP TABLE IF EXISTS customers");
        stmt.execute("CREATE TABLE customers (id INTEGER, name VARCHAR(100))");
        
        // Table 2: orders
        stmt.execute("DROP TABLE IF EXISTS orders");
        stmt.execute("CREATE TABLE orders (id INTEGER, customer_id INTEGER, amount DECIMAL(10,2))");
        
        // Table 3: products
        stmt.execute("DROP TABLE IF EXISTS products");
        stmt.execute("CREATE TABLE products (id INTEGER, name VARCHAR(200), price DECIMAL(10,2))");
        
        stmt.close();
        conn.close();
    }

    private void setupSqlServerPostgresMigration() throws Exception {
        // Simulate SQL Server and PostgreSQL schemas with type differences
        Connection conn = DriverManager.getConnection(
            "jdbc:h2:mem:cross_platform;DB_CLOSE_DELAY=-1", "sa", "");
        
        Statement stmt = conn.createStatement();
        stmt.execute("DROP TABLE IF EXISTS target_customers");
        stmt.execute(
            "CREATE TABLE target_customers (" +
            "  id INTEGER PRIMARY KEY, " +
            "  name VARCHAR(255), " +  // PostgreSQL: VARCHAR
            "  active BOOLEAN" +       // PostgreSQL: BOOLEAN
            ")");
        stmt.close();
        conn.close();
    }

    private void setupPreDeploymentDatabase() throws Exception {
        Connection conn = DriverManager.getConnection(
            "jdbc:h2:mem:pre_deployment;DB_CLOSE_DELAY=-1", "sa", "");
        
        Statement stmt = conn.createStatement();
        
        stmt.execute("DROP TABLE IF EXISTS production_schema");
        stmt.execute(
            "CREATE TABLE production_schema (" +
            "  id INTEGER PRIMARY KEY, " +
            "  name VARCHAR(100), " +
            "  email VARCHAR(100)" +
            ")");
        
        stmt.execute("DROP TABLE IF EXISTS new_schema");
        stmt.execute(
            "CREATE TABLE new_schema (" +
            "  id INTEGER PRIMARY KEY, " +
            "  name VARCHAR(100), " +
            "  email VARCHAR(100), " +
            "  phone VARCHAR(20)" +  // New column added
            ")");
        
        stmt.close();
        conn.close();
    }

    private SchemaComparisonResult extractComparisonResult(RuleResult result) {
        ExecutionStep step = result.getExecutionPath().stream()
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
}
