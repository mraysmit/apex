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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests table enumeration with pattern matching, validating ability to filter
 * tables by name pattern.
 *
 * CRITICAL VALIDATION CHECKLIST:
 * ✅ Database created with multiple tables including temp* tables
 * ✅ Pattern filter (temp%) applied correctly
 * ✅ Only matching tables included in results
 * ✅ Pipeline executes successfully
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Read Schema Database Enumeration with Pattern Test")
class ReadSchemaDatabaseEnumerationPipelineStageTestPattern extends SyncTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaDatabaseEnumerationPipelineStageTestPattern.class);
    private final YamlConfigurationLoader yamlLoader = new YamlConfigurationLoader();
    private RulesEngine rulesEngine;
    private Connection testConnection;
    private String dbName;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        dbName = "schema_enum_pattern_" + System.nanoTime();
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
        super.tearDown();
    }

    @Test
    @DisplayName("Should enumerate tables matching pattern")
    void shouldEnumerateTablesWithPattern() throws Exception {
        logger.info("\n=== Test: Table Enumeration with Pattern Matching ===\n");

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
            .filter(step -> step.getName().contains("read-schema") || step.getName().contains("read-temp"))
            .findFirst()
            .orElse(null);

        assertNotNull(readSchemaStep, "Should have read-schema step");
        assertTrue(readSchemaStep.hasStepData(), "Read-schema step should have data");

        // Verify enumerated tables (should only include TEMP* tables)
        Object stepData = readSchemaStep.getStepData();
        if (stepData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, SchemaMetadata> tableSchemas = (Map<String, SchemaMetadata>) stepData;
            
            logger.info("Enumerated {} tables matching 'TEMP%' pattern", tableSchemas.size());
            
            // Verify all tables match the pattern
            tableSchemas.keySet().forEach(table -> {
                assertTrue(table.toUpperCase().startsWith("TEMP"), 
                    "Table " + table + " should match TEMP% pattern");
                logger.info("  Matched table: {}", table);
            });
            
            // Should have at least the TEMP tables we created
            assertTrue(tableSchemas.size() >= 2, "Should have at least 2 TEMP tables");
        }

        logger.info("✓ Successfully enumerated tables with pattern filter");
    }

    private void setupTestDatabase() throws Exception {
        logger.info("Creating test database with TEMP* tables (DB: {})...", dbName);
        
        String jdbcUrl = "jdbc:h2:./target/h2-demo/" + dbName + ";MODE=PostgreSQL";
        testConnection = DriverManager.getConnection(jdbcUrl, "sa", "");
        
        try (Statement stmt = testConnection.createStatement()) {
            // Create main application tables (should NOT be matched)
            stmt.execute("CREATE TABLE USERS (id INT PRIMARY KEY, username VARCHAR(50))");
            stmt.execute("CREATE TABLE ORDERS (order_id INT PRIMARY KEY, total DECIMAL(10,2))");
            
            // Create temp tables (should be matched by TEMP% pattern)
            stmt.execute("CREATE TABLE TEMP_DATA (id INT, value VARCHAR(50))");
            stmt.execute("CREATE TABLE TEMP_CACHE (cache_id INT, data TEXT)");
            stmt.execute("CREATE TABLE TEMP_STAGING (staging_id INT, content VARCHAR(200))");
        }
        
        logger.info("Test database created with 5 tables (2 app + 3 temp)");
    }

    private String generateYamlConfig() {
        return """
            metadata:
              id: "test-db-enumeration-pattern"
              name: "Database Table Enumeration with Pattern Test"
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
              name: "enumerate-tables-with-pattern"
              execution:
                max-retries: 0
                timeout-seconds: 30
              steps:
                - name: "read-temp-table-schemas"
                  type: "read-schema"
                  source: "test-h2-db"
                  description: "Enumerate tables matching 'TEMP%%' pattern"
                  parameters:
                    schema: "PUBLIC"
                    table-pattern: "TEMP%%"
            """.formatted(dbName);
    }
}
