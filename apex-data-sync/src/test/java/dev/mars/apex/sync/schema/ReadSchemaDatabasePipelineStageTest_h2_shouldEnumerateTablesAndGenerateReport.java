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

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * H2 database test for multi-table enumeration and HTML report generation.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("H2: Enumerate All Tables and Generate HTML Report")
class ReadSchemaDatabasePipelineStageTest_h2_shouldEnumerateTablesAndGenerateReport {

    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaDatabasePipelineStageTest_h2_shouldEnumerateTablesAndGenerateReport.class);
    private RulesEngine rulesEngine;

    @BeforeEach
    void setUp() throws Exception {
        logger.info("=== Setting up H2 Enumeration Test ===");
    }

    @AfterEach
    void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    @DisplayName("H2: Should enumerate all tables and generate HTML report")
    void h2_shouldEnumerateTablesAndGenerateReport() throws Exception {
        logger.info("\n=== Test: H2 - Enumerate All Tables and Generate HTML Report ===\n");

        // Create test database with multiple tables in a dedicated schema
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:enumeration_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
             Statement stmt = conn.createStatement()) {

            // Create dedicated schema for test tables
            stmt.execute("CREATE SCHEMA IF NOT EXISTS test_schema");
            
            // Drop tables if they exist
            stmt.execute("DROP TABLE IF EXISTS test_schema.customers");
            stmt.execute("DROP TABLE IF EXISTS test_schema.orders");
            stmt.execute("DROP TABLE IF EXISTS test_schema.products");

            // Create customers table (5 columns)
            stmt.execute("""
                CREATE TABLE test_schema.customers (
                    customer_id INT PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    email VARCHAR(100),
                    registration_date DATE,
                    loyalty_points INT
                )
            """);

            // Create orders table (4 columns)
            stmt.execute("""
                CREATE TABLE test_schema.orders (
                    order_id INT PRIMARY KEY,
                    customer_id INT NOT NULL,
                    order_date TIMESTAMP,
                    total_amount DECIMAL(10,2)
                )
            """);

            // Create products table (6 columns)
            stmt.execute("""
                CREATE TABLE test_schema.products (
                    product_id INT PRIMARY KEY,
                    product_name VARCHAR(200) NOT NULL,
                    category VARCHAR(50),
                    price DECIMAL(10,2),
                    stock_quantity INT,
                    created_at TIMESTAMP
                )
            """);

            // Insert sample data
            stmt.execute("INSERT INTO test_schema.customers VALUES (1, 'Test Customer', 'test@example.com', '2024-01-01', 100)");
            stmt.execute("INSERT INTO test_schema.orders VALUES (1, 1, '2024-01-15 10:30:00', 150.50)");
            stmt.execute("INSERT INTO test_schema.products VALUES (1, 'Test Product', 'Electronics', 99.99, 50, '2024-01-01 00:00:00')");
        }

        logger.info("✓ H2 test database initialized with 3 tables: customers, orders, products");

        // Execute pipeline with table enumeration and report generation
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/sync/schema/ReadSchemaDatabasePipelineStageTest_h2_shouldEnumerateTablesAndGenerateReport.yaml");

        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Log RuleResult
        logger.info("RuleResult: success={}, message={}", result.isSuccess(), result.getMessage());
        logger.info("RuleResult execution path size: {}", result.getExecutionPath().size());
        logger.info("RuleResult: {}", result);

        // Verify execution was successful
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Verify HTML report was generated
        java.io.File reportFile = new java.io.File("target/reports/h2-all-tables-schema-report.html");
        assertTrue(reportFile.exists(), "HTML report file should exist");
        assertTrue(reportFile.length() > 0, "HTML report file should not be empty");

        logger.info("✓ HTML report generated: {} ({} bytes)", reportFile.getPath(), reportFile.length());

        // Read and verify report content
        String reportContent = java.nio.file.Files.readString(reportFile.toPath());
        assertTrue(reportContent.contains("<!DOCTYPE html>"), "Report should be valid HTML");
        assertTrue(reportContent.contains("Database Schema Report"), "Report should have title");
        assertTrue(reportContent.contains("customers"), "Report should include customers table");
        assertTrue(reportContent.contains("orders"), "Report should include orders table");
        assertTrue(reportContent.contains("products"), "Report should include products table");

        logger.info("✓ HTML report contains all 3 enumerated tables");
        logger.info("✓ Successfully enumerated tables and generated comprehensive HTML report");

        displayPipelineMetrics(result);
    }

    /**
     * Display pipeline execution metrics.
     */
    private void displayPipelineMetrics(RuleResult result) {
        logger.info("\n=== Pipeline Execution Metrics ===");
        logger.info("Overall Success: {}", result.isSuccess());
        logger.info("Message: {}", result.getMessage());

        result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .forEach(step -> {
                logger.info("\nStep: {}", step.getName());
                logger.info("  Status: {}", step.getStatus());
                logger.info("  Duration: {} ms", step.getDurationMs());
                if (step.getRecordsProcessed() != null) {
                    logger.info("  Records Processed: {}", step.getRecordsProcessed());
                }
            });
        logger.info("=".repeat(40));
    }
}
