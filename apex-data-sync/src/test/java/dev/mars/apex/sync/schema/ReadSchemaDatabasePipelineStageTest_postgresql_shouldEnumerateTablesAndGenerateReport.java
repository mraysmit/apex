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
 */
package dev.mars.apex.sync.schema;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import dev.mars.apex.sync.TestContainerImages;

/**
 * PostgreSQL-specific test for table enumeration and HTML report generation.
 * Tests ReadSchemaDatabasePipelineStage with PostgreSQL using Testcontainers.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@Testcontainers

@DisplayName("PostgreSQL: Enumerate All Tables and Generate HTML Report")
public class ReadSchemaDatabasePipelineStageTest_postgresql_shouldEnumerateTablesAndGenerateReport {

    private static final Logger logger = LoggerFactory.getLogger(ReadSchemaDatabasePipelineStageTest_postgresql_shouldEnumerateTablesAndGenerateReport.class);

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(TestContainerImages.POSTGRES);

    @BeforeEach
    void setUp() throws Exception {
        logger.info("=== Setting up PostgreSQL Enumeration Test ===");
        
        // Create test schema and tables
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();
        
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             Statement statement = connection.createStatement()) {
            
            // Create dedicated test schema
            statement.execute("CREATE SCHEMA IF NOT EXISTS test_schema");
            
            // Create test tables in test_schema
            statement.execute("""
                CREATE TABLE IF NOT EXISTS test_schema.customers (
                    customer_id SERIAL PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    email VARCHAR(100),
                    phone VARCHAR(20),
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            statement.execute("""
                CREATE TABLE IF NOT EXISTS test_schema.orders (
                    order_id SERIAL PRIMARY KEY,
                    customer_id INTEGER REFERENCES test_schema.customers(customer_id),
                    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    total_amount DECIMAL(10,2)
                )
            """);
            
            statement.execute("""
                CREATE TABLE IF NOT EXISTS test_schema.products (
                    product_id SERIAL PRIMARY KEY,
                    product_name VARCHAR(100) NOT NULL,
                    description TEXT,
                    price DECIMAL(10,2),
                    stock_quantity INTEGER,
                    category VARCHAR(50)
                )
            """);
            
            logger.info("✅ PostgreSQL test database initialized with 3 tables: customers, orders, products");
        }
    }

    @Test
    @Order(1)
    @DisplayName("PostgreSQL: Should enumerate all tables and generate HTML report")
    void postgresql_shouldEnumerateTablesAndGenerateReport() throws Exception {
        logger.info("  === Test: PostgreSQL - Enumerate All Tables and Generate HTML Report ===");
        
        // Setup System Properties for database connection
        System.setProperty("PG_HOST", postgres.getHost());
        System.setProperty("PG_PORT", String.valueOf(postgres.getFirstMappedPort()));
        System.setProperty("PG_DATABASE", postgres.getDatabaseName());
        System.setProperty("PG_USERNAME", postgres.getUsername());
        System.setProperty("PG_PASSWORD", postgres.getPassword());
        
        logger.info("PostgreSQL Connection Details:");
        logger.info("  Host: {}", postgres.getHost());
        logger.info("  Port: {}", postgres.getFirstMappedPort());
        logger.info("  Database: {}", postgres.getDatabaseName());
        logger.info("  Username: {}", postgres.getUsername());
        
        // Load the configuration
        String yamlConfigPath = "src/test/java/dev/mars/apex/sync/" +
            "ReadSchemaDatabasePipelineStageTest_postgresql_shouldEnumerateTablesAndGenerateReport.yaml";
        
        RulesEngine engine = RulesEngine.fromFile(yamlConfigPath);
        RuleResult result = engine.evaluate(new HashMap<>());
        
        // Verify execution
        logger.info("RuleResult: success={}, message={}", result.isSuccess(), result.getMessage());
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");
        
        // Verify HTML report was generated
        Path reportPath = Path.of("reports/postgresql-all-tables-schema-report.html");
        assertTrue(Files.exists(reportPath), "HTML report should be generated");
        
        File reportFile = reportPath.toFile();
        long reportSize = reportFile.length();
        logger.info("✅ HTML report generated: {} ({} bytes)", reportPath, reportSize);
        
        // Verify report content
        String reportContent = Files.readString(reportPath);
        assertTrue(reportContent.contains("customers"), "Report should contain 'customers' table");
        assertTrue(reportContent.contains("orders"), "Report should contain 'orders' table");
        assertTrue(reportContent.contains("products"), "Report should contain 'products' table");
        logger.info("✅ HTML report contains all 3 enumerated tables");
        
        logger.info("✅ Successfully enumerated tables and generated comprehensive HTML report");
        
        // Display metrics
        displayPipelineMetrics(result);
        
        // Cleanup System properties
        System.clearProperty("PG_HOST");
        System.clearProperty("PG_PORT");
        System.clearProperty("PG_DATABASE");
        System.clearProperty("PG_USERNAME");
        System.clearProperty("PG_PASSWORD");
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
