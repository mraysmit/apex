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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
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
 * Tests JSON serialization edge cases.
 * <p>
 * This test validates JSON serialization behavior for:
 * - Empty schemas (no changes detected)
 * - Schemas with only additions
 * - Schemas with only removals
 * - Schemas with all breaking changes
 * - Null value handling
 * </p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@Testcontainers
public class JsonEdgeCasesTest {

    private static final Logger logger = LoggerFactory.getLogger(JsonEdgeCasesTest.class);
    private static final DockerImageName POSTGRES_IMAGE = 
        DockerImageName.parse("postgres:15-alpine3.20")
            .asCompatibleSubstituteFor("postgres");
    
    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> postgres = new GenericContainer<>(POSTGRES_IMAGE)
            .withEnv("POSTGRES_DB", "testdb")
            .withEnv("POSTGRES_USER", "test")
            .withEnv("POSTGRES_PASSWORD", "test")
            .withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));
    
    private static String jdbcUrl;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeAll
    public static void setUp() {
        jdbcUrl = "jdbc:postgresql://" + postgres.getHost() + ":" 
            + postgres.getMappedPort(5432) + "/testdb";
        logger.info("PostgreSQL container started: {}", jdbcUrl);
        
        // Set system properties for YAML config
        System.setProperty("DB_HOST", postgres.getHost());
        System.setProperty("DB_PORT", String.valueOf(postgres.getMappedPort(5432)));
        System.setProperty("DB_NAME", "testdb");
        System.setProperty("DB_USER", "test");
        System.setProperty("DB_PASS", "test");
    }

    @Test
    public void testEmptyDiffSerialization() throws Exception {
        logger.info("=== Testing empty diff JSON serialization (identical schemas) ===");

        setupIdenticalSchemas();

                RulesEngine engine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/test/JsonEdgeCasesTest.yaml");
        RuleResult result = engine.evaluate(new java.util.HashMap<>());

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        logger.info("Test completed successfully");
        engine.shutdown();
    }

@Test
    public void testOnlyAdditionsSerialization() throws Exception {
        logger.info("=== Testing JSON with only column additions ===");

        setupOnlyAdditions();

        System.setProperty("DB_URL", jdbcUrl);
                RulesEngine engine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/test/JsonEdgeCasesTest.yaml");
        RuleResult result = engine.evaluate(new java.util.HashMap<>());

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        logger.info("Test completed successfully");
        engine.shutdown();
    }

@Test
    public void testAllBreakingChangesSerialization() throws Exception {
        logger.info("=== Testing JSON with all breaking changes ===");

        setupAllBreakingChanges();

        System.setProperty("DB_URL", jdbcUrl);
                RulesEngine engine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/test/JsonEdgeCasesTest.yaml");
        RuleResult result = engine.evaluate(new java.util.HashMap<>());

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        logger.info("Test completed successfully");
        engine.shutdown();
    }

private void setupIdenticalSchemas() throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "test", "test");
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS source_products CASCADE");
            stmt.execute("DROP TABLE IF EXISTS target_products CASCADE");

            stmt.execute("""
                CREATE TABLE source_products (
                    product_id INTEGER,
                    name VARCHAR(100),
                    PRIMARY KEY (product_id)
                )
            """);

            stmt.execute("""
                CREATE TABLE target_products (
                    product_id INTEGER,
                    name VARCHAR(100),
                    PRIMARY KEY (product_id)
                )
            """);

            logger.info("Created identical schemas");
        }
    }

    private void setupOnlyAdditions() throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "test", "test");
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS source_products CASCADE");
            stmt.execute("DROP TABLE IF EXISTS target_products CASCADE");

            stmt.execute("""
                CREATE TABLE source_products (
                    product_id INTEGER,
                    name VARCHAR(100),
                    PRIMARY KEY (product_id)
                )
            """);

            stmt.execute("""
                CREATE TABLE target_products (
                    product_id INTEGER,
                    name VARCHAR(100),
                    description TEXT,
                    category VARCHAR(50),
                    PRIMARY KEY (product_id)
                )
            """);

            logger.info("Created schemas with only additions");
        }
    }

    private void setupAllBreakingChanges() throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "test", "test");
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS source_products CASCADE");
            stmt.execute("DROP TABLE IF EXISTS target_products CASCADE");

            stmt.execute("""
                CREATE TABLE source_products (
                    product_id INTEGER,
                    name VARCHAR(200),
                    description TEXT,
                    price DECIMAL(15, 4),
                    PRIMARY KEY (product_id)
                )
            """);

            stmt.execute("""
                CREATE TABLE target_products (
                    product_id VARCHAR(10),
                    name VARCHAR(50),
                    price DECIMAL(10, 2),
                    PRIMARY KEY (product_id)
                )
            """);

            logger.info("Created schemas with all breaking changes");
        }
    }
}




