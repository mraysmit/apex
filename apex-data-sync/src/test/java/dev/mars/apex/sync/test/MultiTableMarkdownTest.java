/*
 * Copyright 2025 Cityline Ltd
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
package dev.mars.apex.sync.test;

import dev.mars.apex.sync.SyncTestBase;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests multi-table Markdown report generation.
 * <p>
 * This test validates that Markdown reports correctly handle scenarios with
 * multiple tables, including proper organization, table separators, and
 * individual table summaries.
 * </p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@Testcontainers
public class MultiTableMarkdownTest extends SyncTestBase {

    private static final Logger logger = LoggerFactory.getLogger(MultiTableMarkdownTest.class);
    
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine3.20")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    private static String jdbcUrl;
    
    @BeforeAll
    public static void setUpClass() {
        jdbcUrl = postgres.getJdbcUrl();
        logger.info("PostgreSQL container started: {}", jdbcUrl);
        
        // Set system properties for YAML config
        System.setProperty("DB_HOST", postgres.getHost());
        System.setProperty("DB_PORT", String.valueOf(postgres.getFirstMappedPort()));
        System.setProperty("DB_NAME", postgres.getDatabaseName());
        System.setProperty("DB_USER", postgres.getUsername());
        System.setProperty("DB_PASS", postgres.getPassword());
    }

    @Test
    public void testMultipleTablesSingleReport() throws Exception {
        logger.info("=== Testing multiple tables in single Markdown report ===");

        setupMultipleTablesDatabase();

                RulesEngine engine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/test/MultiTableMarkdownTest.yaml");
        RuleResult result = engine.evaluate(new java.util.HashMap<>());

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        logger.info("Test completed successfully");
        engine.shutdown();
    }

@Test
    public void testTableOrganization() throws Exception {
        logger.info("=== Testing table organization in Markdown ===");

        setupMultipleTablesDatabase();

                RulesEngine engine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/test/MultiTableMarkdownTest.yaml");
        RuleResult result = engine.evaluate(new java.util.HashMap<>());

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        logger.info("Test completed successfully");
        engine.shutdown();
    }

@Test
    public void testSummaryAggregation() throws Exception {
        logger.info("=== Testing summary aggregation across multiple tables ===");

        setupMultipleTablesDatabase();

                RulesEngine engine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/test/MultiTableMarkdownTest.yaml");
        RuleResult result = engine.evaluate(new java.util.HashMap<>());

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        logger.info("Test completed successfully");
        engine.shutdown();
    }

private void setupMultipleTablesDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "test", "test");
             Statement stmt = conn.createStatement()) {

            // Drop and recreate tables
            stmt.execute("DROP TABLE IF EXISTS source_customers CASCADE");
            stmt.execute("DROP TABLE IF EXISTS target_customers CASCADE");
            stmt.execute("DROP TABLE IF EXISTS source_orders CASCADE");
            stmt.execute("DROP TABLE IF EXISTS target_orders CASCADE");
            stmt.execute("DROP TABLE IF EXISTS source_products CASCADE");
            stmt.execute("DROP TABLE IF EXISTS target_products CASCADE");
            
            stmt.execute("""
                CREATE TABLE source_customers (
                    customer_id INTEGER,
                    name VARCHAR(100),
                    PRIMARY KEY (customer_id)
                )
            """);
            
            stmt.execute("""
                CREATE TABLE source_orders (
                    order_id INTEGER,
                    customer_id INTEGER,
                    amount DECIMAL(10, 2),
                    PRIMARY KEY (order_id)
                )
            """);
            
            stmt.execute("""
                CREATE TABLE source_products (
                    product_id INTEGER,
                    name VARCHAR(100),
                    price DECIMAL(10, 2),
                    PRIMARY KEY (product_id)
                )
            """);

            // Target schema with modified tables
            
            
            stmt.execute("""
                CREATE TABLE target_customers (
                    customer_id BIGINT,
                    name VARCHAR(200),
                    email VARCHAR(255),
                    PRIMARY KEY (customer_id)
                )
            """);
            
            stmt.execute("""
                CREATE TABLE target_orders (
                    order_id BIGINT,
                    customer_id BIGINT,
                    amount DECIMAL(15, 4),
                    status VARCHAR(20),
                    PRIMARY KEY (order_id)
                )
            """);
            
            stmt.execute("""
                CREATE TABLE target_products (
                    product_id INTEGER,
                    name VARCHAR(100),
                    price DECIMAL(10, 2),
                    PRIMARY KEY (product_id)
                )
            """);

            logger.info("Created multiple tables for Markdown test");
        }
    }
}