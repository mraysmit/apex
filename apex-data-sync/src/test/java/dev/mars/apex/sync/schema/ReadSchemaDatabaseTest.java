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
 * Basic database schema reading test validating fundamental table metadata
 * extraction from H2 database.
 *
 * CRITICAL VALIDATION CHECKLIST:
 * H2 database created with CUSTOMERS table
 * Schema read successfully via pipeline
 * Column metadata extracted correctly
 * Pipeline executes successfully
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Read Schema Database Test")
class ReadSchemaDatabaseTest extends SyncTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaDatabaseTest.class);
    private static final String H2_URL = "jdbc:h2:mem:schema_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
    
    private RulesEngine rulesEngine;
    private Connection testConnection;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
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
        super.tearDown();
    }

    @Test
    @DisplayName("Should read schema from database table")
    void shouldReadSchemaFromDatabase() throws Exception {
        logger.info("\n=== Test: Read Schema from Database ===\n");

        // Load pipeline configuration
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/schema/ReadSchemaDatabaseTest.yaml");
        assertNotNull(rulesEngine, "Rules engine should be created");

        // Execute pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Verify execution success
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Verify pipeline steps executed
        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertFalse(steps.isEmpty(), "Should have pipeline steps");

        // Find the read-schema step
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
        assertTrue(schema.getColumns().size() >= 3, "Should have at least 3 columns");

        logger.info("[OK] Successfully read database schema with {} columns", schema.getColumns().size());
    }

    /**
     * Create test database with CUSTOMERS table.
     */
    private void setupTestDatabase() throws Exception {
        logger.info("Creating test database with CUSTOMERS table...");
        
        testConnection = DriverManager.getConnection(H2_URL, "sa", "");
        
        try (Statement stmt = testConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS CUSTOMERS");
            stmt.execute("""
                CREATE TABLE CUSTOMERS (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100),
                    created_date DATE
                )
            """);
        }
        
        logger.info("Test database created successfully with CUSTOMERS table");
    }
}
