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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for database schema reading using test-read-schema-database.yaml configuration.
 * Validates schema metadata extraction from H2 database tables.
 *
 * <p><b>CRITICAL VALIDATION CHECKLIST:</b></p>
 * <ul>
 *   <li>✅ Extends SyncTestBase (provides APEX service setup/teardown)</li>
 *   <li>✅ Uses ColoredTestOutputExtension (via SyncTestBase)</li>
 *   <li>✅ Loads configuration from resources using naming convention</li>
 *   <li>✅ Validates execution rates (100% success expected)</li>
 *   <li>✅ Proper cleanup of test resources</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class ReadSchemaDatabaseTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaDatabaseTest.class);
    
    private Connection testConnection;

    @BeforeEach
    public void setUpTestDatabase() throws Exception {
        // Create H2 in-memory database matching YAML configuration (with MODE=PostgreSQL)
        String jdbcUrl = "jdbc:h2:mem:schema_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        testConnection = DriverManager.getConnection(jdbcUrl, "sa", "");
        
        // Create test table
        try (Statement stmt = testConnection.createStatement()) {
            // Drop table if exists from previous test run (DB_CLOSE_DELAY=-1 keeps DB alive)
            stmt.execute("DROP TABLE IF EXISTS customers");
            
            stmt.execute("CREATE TABLE customers (" +
                    "id INT PRIMARY KEY, " +
                    "name VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            
            // Insert test data
            stmt.execute("INSERT INTO customers (id, name, email) VALUES " +
                    "(1, 'John Doe', 'john@example.com'), " +
                    "(2, 'Jane Smith', 'jane@example.com')");
        }
        logger.info("Created test database and customers table");
    }

    @AfterEach
    public void tearDownDatabase() throws Exception {
        if (testConnection != null && !testConnection.isClosed()) {
            testConnection.close();
            logger.info("Closed test database connection");
        }
    }

    @Test
    @DisplayName("Should read schema from database table using YAML configuration")
    public void shouldReadSchemaFromDatabase() throws Exception {
        // Load configuration from Java test directory (APEX naming convention)
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/schema/ReadSchemaDatabaseTest.yaml");
        assertNotNull(rulesEngine, "RulesEngine should be initialized");

        // Execute the pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertNotNull(result, "RuleResult should not be null");

        // Validate execution
        logger.info("Pipeline execution completed");
        logger.info("Overall status: {}", result.isSuccess() ? "SUCCESS" : "FAILURE");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Find the read-schema step
        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertFalse(steps.isEmpty(), "Should have pipeline steps");

        ExecutionStep readSchemaStep = steps.stream()
            .filter(step -> step.getName().contains("read-schema"))
            .findFirst()
            .orElse(null);

        assertNotNull(readSchemaStep, "Should have read-schema step");
        assertTrue(readSchemaStep.hasStepData(), "Read-schema step should have data");

        // Verify schema metadata
        Object stepData = readSchemaStep.getStepData();
        assertInstanceOf(SchemaMetadata.class, stepData, "Step data should be SchemaMetadata");

        SchemaMetadata schema = (SchemaMetadata) stepData;
        logger.info("Read database schema: {}", schema);
        
        assertNotNull(schema.getColumns(), "Schema should have columns");
        assertEquals(4, schema.getColumns().size(), "Should have 4 columns (id, name, email, created_date)");

        // Verify column details
        SchemaMetadata.ColumnDefinition idColumn = schema.getColumns().stream()
            .filter(col -> "id".equalsIgnoreCase(col.getName()))
            .findFirst()
            .orElse(null);
        assertNotNull(idColumn, "Should have id column");
        assertFalse(idColumn.isNullable(), "ID should not be nullable");
        logger.info("✓ Database schema validated: {} columns", schema.getColumns().size());
        
        // Validate execution rate
        validateExecutionRate(1, 1, "Database schema reading");
    }
}
