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
import dev.mars.apex.core.engine.core.RulesEngine;
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
 * Tests for multi-table schema reading using test-read-schema-multi-table.yaml configuration.
 * Validates schema metadata extraction from multiple H2 database tables (5 tables).
 *
 * <p><b>CRITICAL VALIDATION CHECKLIST:</b></p>
 * <ul>
 *   <li>Extends SyncTestBase (provides APEX service setup/teardown)</li>
 *   <li>Uses ColoredTestOutputExtension (via SyncTestBase)</li>
 *   <li>Loads configuration from resources using naming convention</li>
 *   <li>Validates execution rates (100% success expected)</li>
 *   <li>Proper cleanup of test resources</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class ReadSchemaMultiTableTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaMultiTableTest.class);
    
    private Connection testConnection;

    @BeforeEach
    public void setUpTestDatabase() throws Exception {
        // Create H2 in-memory database matching YAML configuration
        String jdbcUrl = "jdbc:h2:mem:multi_table_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        testConnection = DriverManager.getConnection(jdbcUrl, "sa", "");
        
        try (Statement stmt = testConnection.createStatement()) {
            // Table 1: customers (5 columns)
            // Drop tables if they exist from previous test run (DB_CLOSE_DELAY=-1 keeps DB alive)
            stmt.execute("DROP TABLE IF EXISTS customers");
            stmt.execute("DROP TABLE IF EXISTS orders");
            stmt.execute("DROP TABLE IF EXISTS products");
            stmt.execute("DROP TABLE IF EXISTS inventory");
            stmt.execute("DROP TABLE IF EXISTS transactions");

            stmt.execute("CREATE TABLE customers (" +
                    "id INT PRIMARY KEY, " +
                    "name VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "phone VARCHAR(20), " +
                    "created_date TIMESTAMP" +
                    ")");
            
            // Table 2: orders (6 columns)
            stmt.execute("CREATE TABLE orders (" +
                    "order_id INT PRIMARY KEY, " +
                    "customer_id INT, " +
                    "order_date TIMESTAMP, " +
                    "total_amount DECIMAL(10,2), " +
                    "status VARCHAR(50), " +
                    "payment_method VARCHAR(50)" +
                    ")");
            
            // Table 3: products (7 columns)
            stmt.execute("CREATE TABLE products (" +
                    "product_id INT PRIMARY KEY, " +
                    "product_name VARCHAR(255), " +
                    "description TEXT, " +
                    "price DECIMAL(10,2), " +
                    "stock_quantity INT, " +
                    "category VARCHAR(100), " +
                    "supplier VARCHAR(255)" +
                    ")");
            
            // Table 4: inventory (4 columns)
            stmt.execute("CREATE TABLE inventory (" +
                    "inventory_id INT PRIMARY KEY, " +
                    "product_id INT, " +
                    "warehouse_location VARCHAR(100), " +
                    "quantity INT" +
                    ")");
            
            // Table 5: transactions (8 columns)
            stmt.execute("CREATE TABLE transactions (" +
                    "transaction_id INT PRIMARY KEY, " +
                    "order_id INT, " +
                    "transaction_date TIMESTAMP, " +
                    "amount DECIMAL(10,2), " +
                    "currency VARCHAR(3), " +
                    "payment_status VARCHAR(50), " +
                    "payment_gateway VARCHAR(100), " +
                    "reference_number VARCHAR(100)" +
                    ")");
        }
        logger.info("Created test database with 5 tables: customers, orders, products, inventory, transactions");
    }

    @AfterEach
    public void tearDownDatabase() throws Exception {
        if (testConnection != null && !testConnection.isClosed()) {
            testConnection.close();
            logger.info("Closed test database connection");
        }
    }

    @Test
    @DisplayName("Should read schemas from 5 database tables using YAML configuration")
    public void shouldReadSchemaFromMultipleTables() throws Exception {
        // Load configuration from Java test directory (APEX naming convention)
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/schema/ReadSchemaMultiTableTest.yaml");
        assertNotNull(rulesEngine, "RulesEngine should be initialized");

        // Execute the pipeline (5 schema read operations)
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertNotNull(result, "RuleResult should not be null");

        // Validate execution
        logger.info("Pipeline execution completed");
        logger.info("Overall status: {}", result.isSuccess() ? "SUCCESS" : "FAILURE");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Get all read-schema steps
        List<ExecutionStep> readSchemaSteps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .filter(step -> step.getName().contains("read-schema"))
            .toList();

        assertEquals(5, readSchemaSteps.size(), "Should have 5 read-schema steps (one per table)");

        // Verify each table schema
        verifyTableSchema(readSchemaSteps, "customers", 5);
        verifyTableSchema(readSchemaSteps, "orders", 6);
        verifyTableSchema(readSchemaSteps, "products", 7);
        verifyTableSchema(readSchemaSteps, "inventory", 4);
        verifyTableSchema(readSchemaSteps, "transactions", 8);

        logger.info("[OK] Successfully validated schemas from 5 database tables");
        
        // Validate execution rate (5 steps, all should succeed)
        validateExecutionRate(5, 5, "Multi-table schema reading");
    }

    /**
     * Verify a table schema from execution steps.
     */
    private void verifyTableSchema(List<ExecutionStep> steps, String tableName, int expectedColumnCount) {
        ExecutionStep tableStep = steps.stream()
            .filter(step -> step.getName().toLowerCase().contains(tableName.toLowerCase()))
            .findFirst()
            .orElse(null);

        assertNotNull(tableStep, "Should have step for table: " + tableName);
        assertTrue(tableStep.hasStepData(), "Step for " + tableName + " should have data");

        Object stepData = tableStep.getStepData();
        assertInstanceOf(SchemaMetadata.class, stepData, "Step data should be SchemaMetadata");

        SchemaMetadata schema = (SchemaMetadata) stepData;
        logger.info("Table {}: {} columns", tableName, schema.getColumns().size());
        assertEquals(expectedColumnCount, schema.getColumns().size(), 
            "Table " + tableName + " should have " + expectedColumnCount + " columns");
    }
}
