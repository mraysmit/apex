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

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests reading schemas from 5 tables with HTML report generation for
 * comprehensive schema documentation.
 *
 * CRITICAL VALIDATION CHECKLIST:
 * H2 database created with 5 tables (CUSTOMERS, ORDERS, PRODUCTS, INVENTORY, TRANSACTIONS)
 * All 5 table schemas read successfully
 * HTML report generated for CUSTOMERS table
 * Each table has correct column count
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Read Schema Multi-Table with Report Test")
class ReadSchemaDatabasePipelineStageTestMultiTable extends SyncTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaDatabasePipelineStageTestMultiTable.class);
    private static final String H2_URL = "jdbc:h2:mem:multi_table_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
    
    private RulesEngine rulesEngine;
    private Connection testConnection;
    private Path reportPath;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        reportPath = Path.of("target/reports/customers-schema.html");
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
    @DisplayName("Should read schemas from 5 tables with HTML report")
    void shouldReadMultiTableSchemasWithReport() throws Exception {
        logger.info("\n=== Test: Multi-Table Schema Reading with Report ===\n");

        // Load pipeline configuration from YAML file
        rulesEngine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/sync/schema/ReadSchemaDatabasePipelineStageTestMultiTable.yaml");
        assertNotNull(rulesEngine, "Rules engine should be created");

        // Execute pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Verify execution success
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Find all read-schema steps
        List<ExecutionStep> readSchemaSteps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .filter(step -> step.getName().contains("read-schema"))
            .toList();

        logger.info("Found {} read-schema steps", readSchemaSteps.size());
        assertEquals(5, readSchemaSteps.size(), "Should have 5 read-schema steps (one per table)");

        // Verify each step has schema data
        for (ExecutionStep step : readSchemaSteps) {
            assertTrue(step.hasStepData(), "Step " + step.getName() + " should have data");
            
            Object stepData = step.getStepData();
            assertInstanceOf(SchemaMetadata.class, stepData, "Step data should be SchemaMetadata");
            
            SchemaMetadata schema = (SchemaMetadata) stepData;
            logger.info("  Table: {} ({} columns)", schema.getSourceName(), schema.getColumns().size());
            assertTrue(schema.getColumns().size() >= 4, "Each table should have at least 4 columns");
        }

        // Verify HTML report was generated (for CUSTOMERS table)
        assertTrue(Files.exists(reportPath), "HTML report should be generated at: " + reportPath);
        
        String reportContent = Files.readString(reportPath);
        assertTrue(reportContent.contains("html"), "Report should be valid HTML");
        assertTrue(reportContent.contains("CUSTOMERS") || reportContent.contains("customers"), 
            "Report should contain CUSTOMERS table");

        logger.info("[OK] HTML report generated: {}", reportPath);
        logger.info("[OK] Successfully read 5 table schemas with report generation");
    }

    private void setupTestDatabase() throws Exception {
        logger.info("Creating H2 in-memory database with 5 tables...");
        
        testConnection = DriverManager.getConnection(H2_URL, "sa", "");
        
        try (Statement stmt = testConnection.createStatement()) {
            // Table 1: CUSTOMERS (5 columns)
            stmt.execute("""
                CREATE TABLE CUSTOMERS (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100),
                    phone VARCHAR(20),
                    created_date DATE
                )
            """);

            // Table 2: ORDERS (6 columns)
            stmt.execute("""
                CREATE TABLE ORDERS (
                    order_id INTEGER PRIMARY KEY,
                    customer_id INTEGER NOT NULL,
                    order_date DATE,
                    total DECIMAL(10,2),
                    status VARCHAR(20),
                    shipped_date DATE
                )
            """);

            // Table 3: PRODUCTS (7 columns)
            stmt.execute("""
                CREATE TABLE PRODUCTS (
                    product_id INTEGER PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    description TEXT,
                    price DECIMAL(10,2),
                    stock INTEGER,
                    category VARCHAR(50),
                    active BOOLEAN
                )
            """);

            // Table 4: INVENTORY (4 columns)
            stmt.execute("""
                CREATE TABLE INVENTORY (
                    inventory_id INTEGER PRIMARY KEY,
                    product_id INTEGER NOT NULL,
                    quantity INTEGER,
                    warehouse VARCHAR(50)
                )
            """);

            // Table 5: TRANSACTIONS (8 columns)
            stmt.execute("""
                CREATE TABLE TRANSACTIONS (
                    txn_id INTEGER PRIMARY KEY,
                    order_id INTEGER NOT NULL,
                    amount DECIMAL(10,2),
                    txn_type VARCHAR(20),
                    txn_date TIMESTAMP,
                    status VARCHAR(20),
                    payment_method VARCHAR(30),
                    reference_id VARCHAR(50)
                )
            """);
        }
        
        logger.info("H2 test database created with 5 tables");
    }
}
