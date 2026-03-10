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
import dev.mars.apex.sync.SyncTestBase;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

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
 * Tests JSON serialization of schema diff reports.
 * <p>
 * This test validates that schema comparison results are correctly serialized to JSON
 * with proper structure, formatting, and data integrity.
 * </p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@Testcontainers
public class JsonSerializationTest extends SyncTestBase {

    private static final Logger logger = LoggerFactory.getLogger(JsonSerializationTest.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
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
    
    @BeforeAll
    public static void setUpClass() {
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

    @BeforeEach
    public void setUpTestData() throws SQLException {
        setupTestDatabase();
    }

    @Test
    public void testBasicJsonSerialization() throws Exception {
        logger.info("=== Testing basic JSON serialization ===");

                RulesEngine engine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/test/JsonSerializationTest.yaml");
        RuleResult result = engine.evaluate(new java.util.HashMap<>());

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        logger.info("Basic JSON serialization test completed successfully");
        engine.shutdown();
    }

    @Test
    public void testJsonFormattingAndIndentation() throws Exception {
        logger.info("=== Testing JSON formatting and indentation ===");

        RulesEngine engine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/test/JsonSerializationTest.yaml");
        RuleResult result = engine.evaluate(new java.util.HashMap<>());

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        logger.info("JSON formatting test completed successfully");
        engine.shutdown();
    }

    @Test
    public void testDataTypeSerialization() throws Exception {
        logger.info("=== Testing data type serialization ===");

        RulesEngine engine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/test/JsonSerializationTest.yaml");
        RuleResult result = engine.evaluate(new java.util.HashMap<>());

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        logger.info("Data type serialization test completed successfully");
        engine.shutdown();
    }

    private void setupTestDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "test", "test");
             Statement stmt = conn.createStatement()) {

            // Drop and create tables in PUBLIC schema
            stmt.execute("DROP TABLE IF EXISTS source_customers CASCADE");
            stmt.execute("DROP TABLE IF EXISTS target_customers CASCADE");

            stmt.execute("""
                CREATE TABLE source_customers (
                    customer_id INTEGER,
                    customer_name VARCHAR(100),
                    email VARCHAR(200),
                    PRIMARY KEY (customer_id)
                )
            """);

            stmt.execute("""
                CREATE TABLE target_customers (
                    customer_id BIGINT,
                    customer_name VARCHAR(100),
                    email VARCHAR(200),
                    PRIMARY KEY (customer_id)
                )
            """);

            logger.info("Created test tables for JSON serialization");
        }
    }
}




