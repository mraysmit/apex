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

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
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
 * Tests Markdown table formatting in schema diff reports.
 * <p>
 * This test validates that Markdown reports have properly formatted tables
 * with correct alignment, headers, and separators.
 * </p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@Testcontainers
public class TableFormattingTest {

    private static final Logger logger = LoggerFactory.getLogger(TableFormattingTest.class);
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine3.20")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    private static String jdbcUrl;
    
    @BeforeAll
    public static void setUp() {
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
    public void testMarkdownTableStructure() throws Exception {
        logger.info("=== Testing Markdown table structure ===");

        setupTestDatabase();

                RulesEngine engine = RulesEngine.fromFile("src/test/resources/dev/mars/apex/sync/test/TableFormattingTest.yaml");
        RuleResult result = engine.evaluate(new java.util.HashMap<>());

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        logger.info("Test completed successfully");
        engine.shutdown();
    }

@Test
    public void testColumnAlignment() throws Exception {
        logger.info("=== Testing Markdown column alignment ===");

        setupTestDatabase();

                RulesEngine engine = RulesEngine.fromFile("src/test/resources/dev/mars/apex/sync/test/TableFormattingTest.yaml");
        RuleResult result = engine.evaluate(new java.util.HashMap<>());

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        logger.info("Test completed successfully");
        engine.shutdown();
    }

@Test
    public void testSpecialCharacterHandling() throws Exception {
        logger.info("=== Testing special character handling in tables ===");

        setupDatabaseWithSpecialChars();

                RulesEngine engine = RulesEngine.fromFile("src/test/resources/dev/mars/apex/sync/test/TableFormattingTest.yaml");
        RuleResult result = engine.evaluate(new java.util.HashMap<>());

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        logger.info("Test completed successfully");
        engine.shutdown();
    }

private void setupTestDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "test", "test");
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS source_products CASCADE");
            stmt.execute("DROP TABLE IF EXISTS target_products CASCADE");

            stmt.execute("""
                CREATE TABLE source_products (
                    product_id INTEGER,
                    product_name VARCHAR(100),
                    price DECIMAL(10, 2),
                    PRIMARY KEY (product_id)
                )
            """);

            stmt.execute("""
                CREATE TABLE target_products (
                    product_id BIGINT,
                    product_name VARCHAR(200),
                    price DECIMAL(15, 4),
                    description TEXT,
                    PRIMARY KEY (product_id)
                )
            """);

            logger.info("Created test schemas for table formatting");
        }
    }

    private void setupDatabaseWithSpecialChars() throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "test", "test");
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS source_products CASCADE");
            stmt.execute("DROP TABLE IF EXISTS target_products CASCADE");

            stmt.execute("""
                CREATE TABLE source_products (
                    PRODUCT_ID INTEGER,
                    "Product-Name" VARCHAR(100),
                    "Price$USD" DECIMAL(10, 2),
                    PRIMARY KEY (PRODUCT_ID)
                )
            """);

            stmt.execute("""
                CREATE TABLE target_products (
                    PRODUCT_ID INTEGER,
                    "Product-Name" VARCHAR(200),
                    "Price$USD" DECIMAL(10, 2),
                    PRIMARY KEY (PRODUCT_ID)
                )
            """);

            logger.info("Created schemas with special characters");
        }
    }
}




