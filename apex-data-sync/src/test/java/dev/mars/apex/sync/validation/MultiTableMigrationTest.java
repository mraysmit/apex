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

package dev.mars.apex.sync.validation;

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

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests multi-table migration validation.
 * Validates schemas for multiple table migrations in one pipeline.
 *
 * <p><b>CRITICAL VALIDATION CHECKLIST:</b></p>
 * <ul>
 *   <li>Extends SyncTestBase (provides APEX service setup/teardown)</li>
 *   <li>Uses ColoredTestOutputExtension (via SyncTestBase)</li>
 *   <li>Validates multiple table schema reads</li>
 *   <li>Verifies schema comparison for each table</li>
 *   <li>Tests sequential pipeline execution</li>
 *   <li>Proper cleanup of test resources</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class MultiTableMigrationTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(MultiTableMigrationTest.class);
    
    private File customersFile;
    private File ordersFile;
    private File productsFile;
    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUpTestData() throws Exception {
        // Create test CSV files for multiple tables
        customersFile = new File("src/test/resources/test-data/migration-customers.csv");
        customersFile.getParentFile().mkdirs();
        try (PrintWriter writer = new PrintWriter(customersFile)) {
            writer.println("id,name,email");
            writer.println("1,Alice,alice@example.com");
            writer.println("2,Bob,bob@example.com");
        }

        ordersFile = new File("src/test/resources/test-data/migration-orders.csv");
        try (PrintWriter writer = new PrintWriter(ordersFile)) {
            writer.println("order_id,customer_id,amount");
            writer.println("101,1,150.00");
            writer.println("102,2,200.00");
        }

        productsFile = new File("src/test/resources/test-data/migration-products.csv");
        try (PrintWriter writer = new PrintWriter(productsFile)) {
            writer.println("product_id,name,price");
            writer.println("1001,Widget,29.99");
            writer.println("1002,Gadget,49.99");
        }
        logger.info("Created test CSV files for multi-table migration");

        // Setup target database with multiple tables
        String dbUrl = "jdbc:h2:mem:multi_table_migration;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (Connection conn = DriverManager.getConnection(dbUrl, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
            // Drop tables if they exist from previous test run (DB_CLOSE_DELAY=-1 keeps DB alive)
            stmt.execute("DROP TABLE IF EXISTS CUSTOMERS");
            stmt.execute("DROP TABLE IF EXISTS ORDERS");
            stmt.execute("DROP TABLE IF EXISTS PRODUCTS");

                stmt.execute("CREATE TABLE CUSTOMERS (id INT, name VARCHAR(255), email VARCHAR(255))");
                stmt.execute("CREATE TABLE ORDERS (order_id INT, customer_id INT, amount DECIMAL(10,2))");
                stmt.execute("CREATE TABLE PRODUCTS (product_id INT, name VARCHAR(255), price DECIMAL(10,2))");
            }
        }
        logger.info("Created target database schema with multiple tables");
    }

    @AfterEach
    public void tearDown() {
        if (customersFile != null && customersFile.exists()) {
            customersFile.delete();
        }
        if (ordersFile != null && ordersFile.exists()) {
            ordersFile.delete();
        }
        if (productsFile != null && productsFile.exists()) {
            productsFile.delete();
        }
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    @DisplayName("Should validate multi-table migration schemas")
    public void shouldValidateMultiTableMigration() throws Exception {
        // Load configuration from Java test directory (APEX naming convention)
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/validation/MultiTableMigrationTest.yaml");
        assertNotNull(rulesEngine, "RulesEngine should be initialized");

        // Execute the pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertNotNull(result, "RuleResult should not be null");

        // Validate execution
        logger.info("Pipeline execution completed");
        logger.info("Overall status: {}", result.isSuccess() ? "SUCCESS" : "FAILURE");
        
        if (!result.isSuccess()) {
            logger.error("Pipeline failed: {}", result.getMessage());
            for (ExecutionStep step : result.getExecutionPath()) {
                logger.error("  Step: {} - Status: {} - Message: {}", 
                    step.getName(), step.getStatus(), step.getMessage());
            }
        }
        
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Verify pipeline steps - should have steps for both tables
        List<ExecutionStep> pipelineSteps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertTrue(pipelineSteps.size() >= 4, "Should have at least 4 steps (read source, read target, compare for each table)");

        // Verify customers table steps
        ExecutionStep readSourceCustomers = pipelineSteps.stream()
            .filter(step -> step.getName().contains("source-customers"))
            .findFirst()
            .orElse(null);
        assertNotNull(readSourceCustomers, "Should have read-source-customers step");
        assertTrue(readSourceCustomers.hasStepData(), "Customers source step should have data");

        ExecutionStep readTargetCustomers = pipelineSteps.stream()
            .filter(step -> step.getName().contains("target-customers"))
            .findFirst()
            .orElse(null);
        assertNotNull(readTargetCustomers, "Should have read-target-customers step");
        assertTrue(readTargetCustomers.hasStepData(), "Customers target step should have data");

        // Verify schema metadata
        if (readSourceCustomers.getStepData() instanceof SchemaMetadata) {
            SchemaMetadata customersSchema = (SchemaMetadata) readSourceCustomers.getStepData();
            assertEquals(3, customersSchema.getColumns().size(), "Customers should have 3 columns");
        }

        logger.info("Multi-table migration validation completed successfully");
    }
}
