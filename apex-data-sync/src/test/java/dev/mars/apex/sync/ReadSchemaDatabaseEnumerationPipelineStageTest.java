package dev.mars.apex.sync;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.schema.SchemaMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for database table enumeration in read-schema pipeline stage.
 * Tests the ability to automatically discover and read schemas for multiple tables.
 * 
 * <p>How to access enumerated table data in your application:</p>
 * <pre>
 * // Execute pipeline
 * RuleResult result = rulesEngine.evaluate(data);
 * 
 * // Get the read-schema step from execution path
 * ExecutionStep readSchemaStep = result.getExecutionPath().stream()
 *     .filter(step -> "PIPELINE_STEP".equals(step.getType()))
 *     .filter(step -> step.getName().contains("read-schema"))
 *     .findFirst()
 *     .orElse(null);
 * 
 * // Access the enumerated table schemas
 * if (readSchemaStep != null && readSchemaStep.hasStepData()) {
 *     Object stepData = readSchemaStep.getStepData();
 *     
 *     if (stepData instanceof Map) {
 *         // Multiple tables enumerated
 *         Map&lt;String, SchemaMetadata&gt; tableSchemas = (Map&lt;String, SchemaMetadata&gt;) stepData;
 *         
 *         // Access each table's schema
 *         tableSchemas.forEach((tableName, schema) -> {
 *             System.out.println("Table: " + tableName);
 *             System.out.println("  Columns: " + schema.getColumns().size());
 *             
 *             // Access individual columns
 *             schema.getColumns().forEach(column -> {
 *                 System.out.println("    - " + column.getName() + 
 *                                  " (" + column.getDataType() + ")");
 *             });
 *         });
 *     } else if (stepData instanceof SchemaMetadata) {
 *         // Single table read
 *         SchemaMetadata schema = (SchemaMetadata) stepData;
 *         System.out.println("Table: " + schema.getSourceName());
 *         System.out.println("  Columns: " + schema.getColumns().size());
 *     }
 * }
 * </pre>
 *
 * @author APEX Team
 * @since 2.1.0
 */
@DisplayName("Read Schema Database Enumeration Pipeline Stage Integration Test")
class ReadSchemaDatabaseEnumerationPipelineStageTest {

    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaDatabaseEnumerationPipelineStageTest.class);
    private RulesEngine rulesEngine;

    @BeforeEach
    void setUp() throws Exception {
        logger.info("=== Setting up Read Schema Database Enumeration Pipeline Stage Test ===");
        setupTestDatabase();
    }

    @AfterEach
    void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }
    
    private void setupTestDatabase() throws Exception {
        logger.info("Creating test database with multiple tables...");
        
        // Create in-memory H2 database with multiple tables
        Connection testConnection = DriverManager.getConnection("jdbc:h2:mem:schema_enum_test;DB_CLOSE_DELAY=-1");
        
        try (Statement stmt = testConnection.createStatement()) {
            // Drop tables if they exist (to handle multiple test runs)
            stmt.execute("DROP TABLE IF EXISTS temp_data");
            stmt.execute("DROP TABLE IF EXISTS system_log");
            stmt.execute("DROP TABLE IF EXISTS reviews");
            stmt.execute("DROP TABLE IF EXISTS categories");
            stmt.execute("DROP TABLE IF EXISTS products");
            stmt.execute("DROP TABLE IF EXISTS orders");
            stmt.execute("DROP TABLE IF EXISTS users");
            
            // Create 5 main tables with different structures
            stmt.execute("CREATE TABLE users (id INT PRIMARY KEY, username VARCHAR(50), email VARCHAR(100))");
            stmt.execute("CREATE TABLE orders (order_id INT PRIMARY KEY, user_id INT, total DECIMAL(10,2), status VARCHAR(20))");
            stmt.execute("CREATE TABLE products (product_id INT PRIMARY KEY, name VARCHAR(100), price DECIMAL(10,2), stock INT)");
            stmt.execute("CREATE TABLE categories (cat_id INT PRIMARY KEY, name VARCHAR(50), description VARCHAR(200))");
            stmt.execute("CREATE TABLE reviews (review_id INT PRIMARY KEY, product_id INT, rating INT, comment TEXT)");
            
            // Create tables that should be excluded in some tests (VALUE is reserved word in H2, use quotes)
            stmt.execute("CREATE TABLE temp_data (id INT, \"VALUE\" VARCHAR(50))");
            stmt.execute("CREATE TABLE system_log (log_id INT, message VARCHAR(500))");
        }
        
        logger.info("Test database created successfully with 7 tables");
    }

    @Test
    @DisplayName("Should enumerate all tables without any filters")
    void shouldEnumerateAllTablesWithoutFilters() throws Exception {
        logger.info("\n=== Test: Enumerate All Tables Without Filters ===\n");

        // Load pipeline configuration using RulesEngine.fromFile()
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/ReadSchemaDatabaseEnumerationPipelineStageTest.yaml");
        assertNotNull(rulesEngine, "Rules engine should be created");

        // Execute pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        
        // Log rule result details
        logger.info("RuleResult: success={}, message={}", result.isSuccess(), result.getMessage());
        logger.info("RuleResult: executionPath size={}", result.getExecutionPath().size());
        logger.info("RuleResult: {}", result);

        // Verify execution success
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Verify pipeline steps executed
        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertFalse(steps.isEmpty(), "Should have pipeline steps");

        // Find the read-schema step
        ExecutionStep readSchemaStep = steps.stream()
            .filter(step -> step.getName().contains("read-all-table-schemas"))
            .findFirst()
            .orElse(null);

        assertNotNull(readSchemaStep, "Should have read-all-table-schemas step");
        assertTrue(readSchemaStep.hasStepData(), "Read-schema step should have data");

        // Verify the step data is a Map of table schemas
        Object stepData = readSchemaStep.getStepData();
        assertInstanceOf(Map.class, stepData, "Step data should be a Map");

        @SuppressWarnings("unchecked")
        Map<String, SchemaMetadata> tableSchemas = (Map<String, SchemaMetadata>) stepData;
        
        logger.info("Enumerated {} tables", tableSchemas.size());
        
        // Log details of each enumerated table
        tableSchemas.forEach((tableName, schema) -> {
            logger.info("  Table: {} - {} columns", tableName, schema.getColumns().size());
            schema.getColumns().forEach(col -> {
                logger.info("    - {} ({}) nullable={}, pk={}", 
                           col.getName(), col.getDataType(), col.isNullable(), col.isPrimaryKey());
            });
        });
        
        // Should have 7 tables (5 main + 2 temp/system)
        assertEquals(7, tableSchemas.size(), "Should enumerate all 7 tables");
        
        // Verify specific tables exist
        assertTrue(tableSchemas.containsKey("USERS"), "Should include USERS table");
        assertTrue(tableSchemas.containsKey("ORDERS"), "Should include ORDERS table");
        assertTrue(tableSchemas.containsKey("PRODUCTS"), "Should include PRODUCTS table");
        assertTrue(tableSchemas.containsKey("CATEGORIES"), "Should include CATEGORIES table");
        assertTrue(tableSchemas.containsKey("REVIEWS"), "Should include REVIEWS table");
        
        // Verify schema details for USERS table
        SchemaMetadata usersSchema = tableSchemas.get("USERS");
        assertNotNull(usersSchema, "USERS schema should exist");
        assertEquals(3, usersSchema.getColumns().size(), "USERS should have 3 columns");
        assertEquals("USERS", usersSchema.getSourceName(), "Source name should be USERS");
        
        logger.info("✓ All table enumeration assertions passed");
    }

    @Test
    @DisplayName("Should enumerate tables with table name pattern filter")
    void shouldEnumerateTablesWithPattern() throws Exception {
        logger.info("\n=== Test: Enumerate Tables With Pattern Filter ===\n");

        // Load pipeline configuration using RulesEngine.fromFile()
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/ReadSchemaDatabaseEnumerationPipelineStageTestPattern.yaml");
        assertNotNull(rulesEngine, "Rules engine should be created");

        // Execute pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        
        // Log rule result details
        logger.info("RuleResult: success={}, message={}", result.isSuccess(), result.getMessage());
        logger.info("RuleResult: executionPath size={}", result.getExecutionPath().size());
        logger.info("RuleResult: {}", result);

        // Verify execution success
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Find the read-schema step
        ExecutionStep readSchemaStep = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .filter(step -> step.getName().contains("read-temp-table-schemas"))
            .findFirst()
            .orElse(null);

        assertNotNull(readSchemaStep, "Should have read-temp-table-schemas step");
        assertTrue(readSchemaStep.hasStepData(), "Read-schema step should have data");

        @SuppressWarnings("unchecked")
        Map<String, SchemaMetadata> tableSchemas = (Map<String, SchemaMetadata>) readSchemaStep.getStepData();
        
        logger.info("Enumerated {} tables matching pattern", tableSchemas.size());
        
        // Should only have tables matching "temp%" pattern (temp_data)
        assertEquals(1, tableSchemas.size(), "Should enumerate only tables matching pattern");
        assertTrue(tableSchemas.containsKey("TEMP_DATA"), "Should include TEMP_DATA table");
        
        logger.info("✓ Pattern filter assertions passed");
    }
    
    @Test
    @DisplayName("Should enumerate tables with exclusion list")
    void shouldEnumerateTablesWithExclusions() throws Exception {
        logger.info("\n=== Test: Enumerate Tables With Exclusion List ===\n");

        // Load pipeline configuration using RulesEngine.fromFile()
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/ReadSchemaDatabaseEnumerationPipelineStageTestExclusions.yaml");
        assertNotNull(rulesEngine, "Rules engine should be created");

        // Execute pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        
        // Log rule result details
        logger.info("RuleResult: success={}, message={}", result.isSuccess(), result.getMessage());
        logger.info("RuleResult: executionPath size={}", result.getExecutionPath().size());
        logger.info("RuleResult: {}", result);

        // Verify execution success
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Find the read-schema step
        ExecutionStep readSchemaStep = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .filter(step -> step.getName().contains("read-filtered-table-schemas"))
            .findFirst()
            .orElse(null);

        assertNotNull(readSchemaStep, "Should have read-filtered-table-schemas step");
        assertTrue(readSchemaStep.hasStepData(), "Read-schema step should have data");

        @SuppressWarnings("unchecked")
        Map<String, SchemaMetadata> tableSchemas = (Map<String, SchemaMetadata>) readSchemaStep.getStepData();
        
        logger.info("Enumerated {} tables after exclusions", tableSchemas.size());
        
        // Should have 5 tables (excluding temp_data and system_log)
        assertEquals(5, tableSchemas.size(), "Should enumerate tables excluding specified ones");
        
        // Verify excluded tables are not present
        assertFalse(tableSchemas.containsKey("TEMP_DATA"), "Should exclude TEMP_DATA");
        assertFalse(tableSchemas.containsKey("SYSTEM_LOG"), "Should exclude SYSTEM_LOG");
        
        // Verify included tables are present
        assertTrue(tableSchemas.containsKey("USERS"), "Should include USERS table");
        assertTrue(tableSchemas.containsKey("ORDERS"), "Should include ORDERS table");
        assertTrue(tableSchemas.containsKey("PRODUCTS"), "Should include PRODUCTS table");
        
        logger.info("✓ Exclusion filter assertions passed");
    }
    
    @Test
    @DisplayName("Should generate HTML report for enumerated tables")
    void shouldGenerateHtmlReportForEnumeratedTables() throws Exception {
        logger.info("\n=== Test: Generate HTML Report for Enumerated Tables ===\n");

        // Load pipeline configuration using RulesEngine.fromFile()
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/ReadSchemaDatabaseEnumerationPipelineStageTestReport.yaml");
        assertNotNull(rulesEngine, "Rules engine should be created");

        // Execute pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        
        // Log rule result details
        logger.info("RuleResult: success={}, message={}", result.isSuccess(), result.getMessage());
        logger.info("RuleResult: executionPath size={}", result.getExecutionPath().size());
        logger.info("RuleResult: {}", result);

        // Verify execution success
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Find the read-schema step
        ExecutionStep readSchemaStep = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .filter(step -> step.getName().contains("read-all-table-schemas-with-report"))
            .findFirst()
            .orElse(null);

        assertNotNull(readSchemaStep, "Should have read-all-table-schemas-with-report step");
        assertTrue(readSchemaStep.hasStepData(), "Read-schema step should have data");

        @SuppressWarnings("unchecked")
        Map<String, SchemaMetadata> tableSchemas = (Map<String, SchemaMetadata>) readSchemaStep.getStepData();
        
        logger.info("Enumerated {} tables with HTML report", tableSchemas.size());
        
        // Verify HTML report was generated
        Path reportPath = Paths.get("target/schema-report.html");
        assertTrue(Files.exists(reportPath), "HTML report should be generated");
        
        // Read and verify report contains expected content
        String reportContent = Files.readString(reportPath);
        assertTrue(reportContent.contains("<html"), "Report should be valid HTML");
        assertTrue(reportContent.contains("Database Schema Report"), "Report should have title");
        assertTrue(reportContent.contains("USERS"), "Report should contain USERS table");
        assertTrue(reportContent.contains("ORDERS"), "Report should contain ORDERS table");
        
        logger.info("✓ HTML report generated successfully: {}", reportPath.toAbsolutePath());
        logger.info("✓ Report size: {} bytes", Files.size(reportPath));
    }
}
