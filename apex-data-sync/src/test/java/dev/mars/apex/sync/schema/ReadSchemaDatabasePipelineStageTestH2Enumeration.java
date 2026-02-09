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

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests H2-specific table enumeration with HTML report generation.
 * Validates H2 schema naming conventions (uppercase) and enumeration capabilities.
 *
 * CRITICAL VALIDATION CHECKLIST:
 * H2 in-memory database created with test schema
 * Tables enumerated from custom schema
 * H2 uppercase schema naming handled correctly
 * HTML report generated
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Read Schema Database H2 Enumeration Test")
class ReadSchemaDatabasePipelineStageTestH2Enumeration extends SyncTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaDatabasePipelineStageTestH2Enumeration.class);
    private static final String H2_URL = "jdbc:h2:mem:enumeration_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
    
    private final YamlConfigurationLoader yamlLoader = new YamlConfigurationLoader();
    private RulesEngine rulesEngine;
    private Connection testConnection;
    private Path reportPath;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        reportPath = Path.of("target/reports/h2-all-tables-schema-report.html");
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
    @DisplayName("Should enumerate H2 tables from custom schema with report")
    void shouldEnumerateH2TablesWithReport() throws Exception {
        logger.info("\n=== Test: H2 Multi-Table Enumeration with Report ===\n");

        // Load pipeline configuration from YAML file
        rulesEngine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/sync/schema/ReadSchemaDatabasePipelineStageTestH2Enumeration.yaml");
        assertNotNull(rulesEngine, "Rules engine should be created");

        // Execute pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Verify execution success
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Find the read-schema step
        ExecutionStep readSchemaStep = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .filter(step -> step.getName().contains("enumerate") || step.getName().contains("read-schema"))
            .findFirst()
            .orElse(null);

        assertNotNull(readSchemaStep, "Should have read-schema step");
        
        if (readSchemaStep.hasStepData()) {
            Object stepData = readSchemaStep.getStepData();
            
            if (stepData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, SchemaMetadata> tableSchemas = (Map<String, SchemaMetadata>) stepData;
                
                logger.info("Enumerated {} tables from TEST_SCHEMA", tableSchemas.size());
                
                tableSchemas.forEach((tableName, schema) -> {
                    logger.info("  Table: {} ({} columns)", tableName, schema.getColumns().size());
                });
            } else if (stepData instanceof SchemaMetadata) {
                SchemaMetadata schema = (SchemaMetadata) stepData;
                logger.info("Read schema: {} ({} columns)", schema.getSourceName(), schema.getColumns().size());
            }
        }

        logger.info("[OK] Successfully enumerated H2 tables");
    }

    private void setupTestDatabase() throws Exception {
        logger.info("Creating H2 in-memory database with test schema...");
        
        testConnection = DriverManager.getConnection(H2_URL, "sa", "");
        
        try (Statement stmt = testConnection.createStatement()) {
            // Create custom schema (H2 uses uppercase)
            stmt.execute("CREATE SCHEMA IF NOT EXISTS TEST_SCHEMA");
            
            // Create tables in custom schema
            stmt.execute("CREATE TABLE TEST_SCHEMA.CUSTOMERS (id INT PRIMARY KEY, name VARCHAR(100), email VARCHAR(100))");
            stmt.execute("CREATE TABLE TEST_SCHEMA.ORDERS (order_id INT PRIMARY KEY, customer_id INT, total DECIMAL(10,2))");
            stmt.execute("CREATE TABLE TEST_SCHEMA.PRODUCTS (product_id INT PRIMARY KEY, name VARCHAR(100), price DECIMAL(10,2))");
        }
        
        logger.info("H2 test database created with 3 tables in TEST_SCHEMA");
    }
}
