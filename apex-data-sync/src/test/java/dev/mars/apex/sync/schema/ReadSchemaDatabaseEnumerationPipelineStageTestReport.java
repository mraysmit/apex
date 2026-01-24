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
 * Created: 2026-01-19
 */

package dev.mars.apex.sync.schema;

import dev.mars.apex.sync.SyncTestBase;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests table enumeration with HTML report generation, creating comprehensive
 * database schema documentation.
 *
 * CRITICAL VALIDATION CHECKLIST:
 * Database created with multiple tables
 * All tables enumerated successfully
 * HTML report generated at specified path
 * Report contains table and column information
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Read Schema Database Enumeration with Report Test")
class ReadSchemaDatabaseEnumerationPipelineStageTestReport extends SyncTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaDatabaseEnumerationPipelineStageTestReport.class);
    private final YamlConfigurationLoader yamlLoader = new YamlConfigurationLoader();
    private RulesEngine rulesEngine;
    private Connection testConnection;
    private String dbName;
    private Path reportPath;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        dbName = "schema_enum_report_" + System.nanoTime();
        reportPath = Path.of("target/reports/enumeration-schema-report-" + dbName + ".html");
        try {
            setupTestDatabase();
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup test database", e);
        }
    }

    @AfterEach
    @Override
    public void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
        // Shutdown H2 database
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:./target/h2-demo/" + dbName, "sa", "")) {
            conn.createStatement().execute("SHUTDOWN");
        } catch (Exception e) {
            logger.debug("Database shutdown: {}", e.getMessage());
        }
        if (testConnection != null) {
            try {
                if (!testConnection.isClosed()) {
                    testConnection.close();
                }
            } catch (Exception e) {
                logger.warn("Error closing test connection", e);
            }
        }
        // Clean up report file
        try {
            Files.deleteIfExists(reportPath);
        } catch (Exception e) {
            logger.debug("Could not delete report file: {}", e.getMessage());
        }
        super.tearDown();
    }

    @Test
    @DisplayName("Should enumerate tables and generate HTML report")
    void shouldEnumerateTablesAndGenerateReport() throws Exception {
        logger.info("\n=== Test: Table Enumeration with HTML Report ===\n");

        // Generate YAML config with unique database name
        String yamlConfig = generateYamlConfig();
        
        // Create engine from YAML string
        YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlConfig);
        rulesEngine = RulesEngine.fromYamlConfig(config);
        assertNotNull(rulesEngine, "Rules engine should be created");

        // Execute pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Verify execution success
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Find the read-schema step
        ExecutionStep readSchemaStep = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .filter(step -> step.getName().contains("read-schema") || step.getName().contains("read-all"))
            .findFirst()
            .orElse(null);

        assertNotNull(readSchemaStep, "Should have read-schema step");
        assertTrue(readSchemaStep.hasStepData(), "Read-schema step should have data");

        // Verify enumerated tables
        Object stepData = readSchemaStep.getStepData();
        if (stepData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, SchemaMetadata> tableSchemas = (Map<String, SchemaMetadata>) stepData;
            
            logger.info("Enumerated {} tables", tableSchemas.size());
            assertTrue(tableSchemas.size() >= 3, "Should have at least 3 tables");
            
            tableSchemas.keySet().forEach(table -> 
                logger.info("  Table: {}", table));
        }

        // Verify HTML report was generated
        assertTrue(Files.exists(reportPath), "HTML report should be generated at: " + reportPath);
        
        String reportContent = Files.readString(reportPath);
        assertTrue(reportContent.contains("html"), "Report should be valid HTML");
        assertTrue(reportContent.contains("USERS") || reportContent.contains("users"), 
            "Report should contain USERS table");
        assertTrue(reportContent.contains("ORDERS") || reportContent.contains("orders"), 
            "Report should contain ORDERS table");

        logger.info("[OK] HTML report generated: {}", reportPath);
        logger.info("[OK] Successfully enumerated tables with report generation");
    }

    private void setupTestDatabase() throws Exception {
        logger.info("Creating test database (DB: {})...", dbName);
        
        String jdbcUrl = "jdbc:h2:./target/h2-demo/" + dbName + ";MODE=PostgreSQL";
        testConnection = DriverManager.getConnection(jdbcUrl, "sa", "");
        
        try (Statement stmt = testConnection.createStatement()) {
            stmt.execute("CREATE TABLE USERS (id INT PRIMARY KEY, username VARCHAR(50), email VARCHAR(100))");
            stmt.execute("CREATE TABLE ORDERS (order_id INT PRIMARY KEY, user_id INT, total DECIMAL(10,2))");
            stmt.execute("CREATE TABLE PRODUCTS (product_id INT PRIMARY KEY, name VARCHAR(100), price DECIMAL(10,2))");
        }
        
        logger.info("Test database created with 3 tables");
    }

    private String generateYamlConfig() {
        return """
            metadata:
              id: "test-db-enumeration-report"
              name: "Database Table Enumeration with Report Test"
              type: "pipeline-config"
              version: "1.0"
            
            data-sources:
              - name: "test-h2-db"
                type: "database"
                source-type: "h2"
                connection:
                  database: "./target/h2-demo/%s"
                  username: "sa"
                  password: ""
                enabled: true
            
            pipeline:
              name: "enumerate-tables-with-report"
              execution:
                max-retries: 0
                timeout-seconds: 30
              steps:
                - name: "read-all-table-schemas-with-report"
                  type: "read-schema"
                  source: "test-h2-db"
                  description: "Enumerate all tables and generate HTML report"
                  parameters:
                    schema: "PUBLIC"
                    report-output: "%s"
            """.formatted(dbName, reportPath.toString().replace("\\", "/"));
    }
}
