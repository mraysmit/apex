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
 * Created: 2026-01-14
 */

package dev.mars.apex.sync.schema;

import dev.mars.apex.sync.SyncTestBase;
import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.ExecutionStep;
import dev.mars.apex.engine.model.RuleResult;
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
 * CRITICAL VALIDATION CHECKLIST:
 * Database enumeration - Automatically discover all tables in schema
 * Multiple table schemas - Read schema for each discovered table
 * Map structure validation - stepData should be Map<String, SchemaMetadata>
 * Column count verification - Each table has expected number of columns
 * HTML report generation - Schema enumeration report with all tables
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Read Schema Database Enumeration Pipeline Stage Integration Test")
class ReadSchemaDatabaseEnumerationPipelineStageTest extends SyncTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaDatabaseEnumerationPipelineStageTest.class);
    private final YamlConfigurationLoader yamlLoader = new YamlConfigurationLoader();
    private RulesEngine rulesEngine;
    private Connection testConnection;
    private String dbName;  // Unique per test instance

    @BeforeEach
    public void setUpEnumerationTest() throws Exception {
        dbName = "schema_enum_test_" + System.nanoTime();  // Unique per test
        setupTestDatabase();
    }
    
    /**
     * Create RulesEngine from YAML string.
     */
    private RulesEngine createRulesEngine(String yamlConfig) throws Exception {
        YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlConfig);
        return RulesEngine.fromYamlConfig(config);
    }

    @AfterEach
    public void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
        // Shutdown H2 database to release file locks (pattern from apex-demo)
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:./target/h2-demo/" + dbName, "sa", "")) {
            connection.createStatement().execute("SHUTDOWN");
            logger.info("Database shutdown completed");
        } catch (Exception e) {
            logger.debug("Database shutdown (expected if not connected): {}", e.getMessage());
        }
        if (testConnection != null) {
            try {
                if (!testConnection.isClosed()) {
                    testConnection.close();
                    logger.info("Closed test database connection");
                }
            } catch (Exception e) {
                logger.warn("Error closing test database connection", e);
            }
        }
        // Call parent tearDown
        super.tearDown();
    }
    
    private void setupTestDatabase() throws Exception {
        logger.info("Creating test database with multiple tables (DB: {})...", dbName);
        
        // Create file-based H2 database (proven pattern from apex-demo)
        String jdbcUrl = "jdbc:h2:./target/h2-demo/" + dbName + ";MODE=PostgreSQL";
        testConnection = DriverManager.getConnection(jdbcUrl, "sa", "");
        
        try (Statement stmt = testConnection.createStatement()) {
            // Create 5 main tables with different structures (uppercase table names for PostgreSQL mode)
            stmt.execute("CREATE TABLE USERS (id INT PRIMARY KEY, username VARCHAR(50), email VARCHAR(100))");
            stmt.execute("CREATE TABLE ORDERS (order_id INT PRIMARY KEY, user_id INT, total DECIMAL(10,2), status VARCHAR(20))");
            stmt.execute("CREATE TABLE PRODUCTS (product_id INT PRIMARY KEY, name VARCHAR(100), price DECIMAL(10,2), stock INT)");
            stmt.execute("CREATE TABLE CATEGORIES (cat_id INT PRIMARY KEY, name VARCHAR(50), description VARCHAR(200))");
            stmt.execute("CREATE TABLE REVIEWS (review_id INT PRIMARY KEY, product_id INT, rating INT, comment TEXT)");
            
            // Create tables that should be excluded in some tests (uppercase names and quoted VALUE keyword)
            stmt.execute("CREATE TABLE TEMP_DATA (id INT, \"VALUE\" VARCHAR(50))");
            stmt.execute("CREATE TABLE SYSTEM_LOG (log_id INT, message VARCHAR(500))");
            
            // DEBUG: Check all USERS tables across all schemas
            try (java.sql.ResultSet rs = stmt.executeQuery(
                    "SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'USERS' ORDER BY TABLE_SCHEMA, ORDINAL_POSITION")) {
                logger.info("DEBUG: All USERS columns across all schemas:");
                while (rs.next()) {
                    logger.info("  Schema: {}, Table: {}, Column: {}", 
                        rs.getString("TABLE_SCHEMA"), rs.getString("TABLE_NAME"), rs.getString("COLUMN_NAME"));
                }
            }
        }
        
        logger.info("Test database created successfully with 7 tables");
    }
    
    /**
     * Generate YAML configuration with unique database name for test isolation.
     * @param excludeTables Array of table names/patterns to exclude (e.g., ["TEMP%", "SYSTEM%"])
     */
    private String generateYamlConfig(String schema, String tablePattern, String[] excludeTables) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("metadata:\n");
        yaml.append("  id: \"test-db-enumeration-").append(dbName).append("\"\n");
        yaml.append("  name: \"Database Table Enumeration Test\"\n");
        yaml.append("  type: \"pipeline-config\"\n");
        yaml.append("  version: \"1.0\"\n");
        yaml.append("\n");
        yaml.append("data-sources:\n");
        yaml.append("  - name: \"test-h2-db\"\n");
        yaml.append("    type: \"database\"\n");
        yaml.append("    source-type: \"h2\"\n");
        yaml.append("    connection:\n");
        yaml.append("      database: \"./target/h2-demo/").append(dbName).append("\"\n");
        yaml.append("      username: \"sa\"\n");
        yaml.append("      password: \"\"\n");
        yaml.append("    enabled: true\n");
        yaml.append("\n");
        yaml.append("pipeline:\n");
        yaml.append("  name: \"enumerate-tables\"\n");
        yaml.append("  execution:\n");
        yaml.append("    max-retries: 0\n");
        yaml.append("    timeout-seconds: 30\n");
        yaml.append("  steps:\n");
        yaml.append("    - name: \"read-all-table-schemas\"\n");
        yaml.append("      type: \"read-schema\"\n");
        yaml.append("      source: \"test-h2-db\"\n");
        yaml.append("      description: \"Enumerate tables in the database\"\n");
        yaml.append("      parameters:\n");
        if (schema != null) {
            yaml.append("        schema: \"").append(schema).append("\"\n");
        }
        if (tablePattern != null) {
            yaml.append("        table-pattern: \"").append(tablePattern).append("\"\n");
        }
        if (excludeTables != null && excludeTables.length > 0) {
            yaml.append("        exclude-tables:\n");
            for (String table : excludeTables) {
                yaml.append("          - \"").append(table).append("\"\n");
            }
        }
        return yaml.toString();
    }
    
    /**
     * Generate YAML configuration with custom step name.
     * @param excludeTables Array of table names/patterns to exclude (e.g., ["TEMP%", "SYSTEM%"])
     */
    private String generateYamlConfigWithStepName(String schema, String tablePattern, String[] excludeTables, String stepName) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("metadata:\n");
        yaml.append("  id: \"test-db-enumeration-").append(dbName).append("\"\n");
        yaml.append("  name: \"Database Table Enumeration Test\"\n");
        yaml.append("  type: \"pipeline-config\"\n");
        yaml.append("  version: \"1.0\"\n");
        yaml.append("\n");
        yaml.append("data-sources:\n");
        yaml.append("  - name: \"test-h2-db\"\n");
        yaml.append("    type: \"database\"\n");
        yaml.append("    source-type: \"h2\"\n");
        yaml.append("    connection:\n");
        yaml.append("      database: \"./target/h2-demo/").append(dbName).append("\"\n");
        yaml.append("      username: \"sa\"\n");
        yaml.append("      password: \"\"\n");
        yaml.append("    enabled: true\n");
        yaml.append("\n");
        yaml.append("pipeline:\n");
        yaml.append("  name: \"enumerate-tables\"\n");
        yaml.append("  execution:\n");
        yaml.append("    max-retries: 0\n");
        yaml.append("    timeout-seconds: 30\n");
        yaml.append("  steps:\n");
        yaml.append("    - name: \"").append(stepName).append("\"\n");
        yaml.append("      type: \"read-schema\"\n");
        yaml.append("      source: \"test-h2-db\"\n");
        yaml.append("      description: \"Enumerate tables in the database\"\n");
        yaml.append("      parameters:\n");
        if (schema != null) {
            yaml.append("        schema: \"").append(schema).append("\"\n");
        }
        if (tablePattern != null) {
            yaml.append("        table-pattern: \"").append(tablePattern).append("\"\n");
        }
        if (excludeTables != null && excludeTables.length > 0) {
            yaml.append("        exclude-tables:\n");
            for (String table : excludeTables) {
                yaml.append("          - \"").append(table).append("\"\n");
            }
        }
        return yaml.toString();
    }
    
    /**
     * Generate YAML configuration with HTML report output.
     */
    private String generateYamlConfigWithReport(String schema, String stepName, String reportPath) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("metadata:\n");
        yaml.append("  id: \"test-db-enumeration-").append(dbName).append("\"\n");
        yaml.append("  name: \"Database Table Enumeration Test\"\n");
        yaml.append("  type: \"pipeline-config\"\n");
        yaml.append("  version: \"1.0\"\n");
        yaml.append("\n");
        yaml.append("data-sources:\n");
        yaml.append("  - name: \"test-h2-db\"\n");
        yaml.append("    type: \"database\"\n");
        yaml.append("    source-type: \"h2\"\n");
        yaml.append("    connection:\n");
        yaml.append("      database: \"./target/h2-demo/").append(dbName).append("\"\n");
        yaml.append("      username: \"sa\"\n");
        yaml.append("      password: \"\"\n");
        yaml.append("    enabled: true\n");
        yaml.append("\n");
        yaml.append("pipeline:\n");
        yaml.append("  name: \"enumerate-tables\"\n");
        yaml.append("  execution:\n");
        yaml.append("    max-retries: 0\n");
        yaml.append("    timeout-seconds: 30\n");
        yaml.append("  steps:\n");
        yaml.append("    - name: \"").append(stepName).append("\"\n");
        yaml.append("      type: \"read-schema\"\n");
        yaml.append("      source: \"test-h2-db\"\n");
        yaml.append("      description: \"Enumerate tables with report\"\n");
        yaml.append("      parameters:\n");
        if (schema != null) {
            yaml.append("        schema: \"").append(schema).append("\"\n");
        }
        yaml.append("        report-output: \"").append(reportPath).append("\"\n");
        return yaml.toString();
    }

    @Test
    @DisplayName("Should enumerate all tables without any filters")
    void shouldEnumerateAllTablesWithoutFilters() throws Exception {
        logger.info("\n=== Test: Enumerate All Tables Without Filters ===\n");

        // Load pipeline configuration with unique database name
        String yamlConfig = generateYamlConfig("PUBLIC", null, null);
        rulesEngine = createRulesEngine(yamlConfig);
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
        assertEquals("test-h2-db", usersSchema.getSourceName(), "Source name should be the data source name");
        
        logger.info("[OK] All table enumeration assertions passed");
    }

    @Test
    @DisplayName("Should enumerate tables with table name pattern filter")
    void shouldEnumerateTablesWithPattern() throws Exception {
        logger.info("\n=== Test: Enumerate Tables With Pattern Filter ===\n");

        // Load pipeline configuration with pattern filter
        String yamlConfig = generateYamlConfigWithStepName("PUBLIC", "TEMP%", null, "read-temp-table-schemas");
        rulesEngine = createRulesEngine(yamlConfig);
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
        
        logger.info("[OK] Pattern filter assertions passed");
    }
    
    @Test
    @DisplayName("Should enumerate tables with exclusion list")
    void shouldEnumerateTablesWithExclusions() throws Exception {
        logger.info("\n=== Test: Enumerate Tables With Exclusion List ===\n");

        // Load pipeline configuration with exclusion list (TEMP% and SYSTEM% patterns)
        String yamlConfig = generateYamlConfigWithStepName("PUBLIC", null, 
            new String[]{"TEMP%", "SYSTEM%"}, "read-filtered-table-schemas");
        rulesEngine = createRulesEngine(yamlConfig);
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
        
        logger.info("[OK] Exclusion filter assertions passed");
    }
    
    @Test
    @DisplayName("Should generate HTML report for enumerated tables")
    void shouldGenerateHtmlReportForEnumeratedTables() throws Exception {
        logger.info("\n=== Test: Generate HTML Report for Enumerated Tables ===\n");

        // Load pipeline configuration with HTML report output
        String yamlConfig = generateYamlConfigWithReport("PUBLIC", "read-all-table-schemas-with-report", "target/schema-report-" + dbName + ".html");
        rulesEngine = createRulesEngine(yamlConfig);
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
        
        // Verify HTML report was generated (using unique report name)
        Path reportPath = Paths.get("target/schema-report-" + dbName + ".html");
        assertTrue(Files.exists(reportPath), "HTML report should be generated at " + reportPath);
        
        // Read and verify report contains expected content
        String reportContent = Files.readString(reportPath);
        assertTrue(reportContent.contains("<html"), "Report should be valid HTML");
        assertTrue(reportContent.contains("Database Schema Report"), "Report should have title");
        assertTrue(reportContent.contains("USERS"), "Report should contain USERS table");
        assertTrue(reportContent.contains("ORDERS"), "Report should contain ORDERS table");
        
        logger.info("[OK] HTML report generated successfully: {}", reportPath.toAbsolutePath());
        logger.info("[OK] Report size: {} bytes", Files.size(reportPath));
    }
}
