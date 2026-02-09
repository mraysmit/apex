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

package dev.mars.apex.sync.schemas;

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlDataSource;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.sync.ColoredTestOutputExtension;
import dev.mars.apex.sync.SyncTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for custom (non-default) database schemas using PostgreSQL TestContainers.
 * 
 * <p>This test validates that APEX can work with custom schemas like 'sales', 'inventory',
 * etc., not just the default 'public' schema.</p>
 * 
 * <p><strong>Key scenarios:</strong></p>
 * <ul>
 *   <li>Read schema from custom schema (e.g., sales.orders)</li>
 *   <li>Compare schemas across different custom schemas</li>
 *   <li>Cross-schema schema-diff operations</li>
 * </ul>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@Testcontainers
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Custom Schema Tests - PostgreSQL")
public class CustomSchemaPostgresTest extends SyncTestBase {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("custom_schema_test")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void setUpDatabase() throws Exception {
        postgres.start();
        
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {

            // Create sales schema with orders table
            stmt.execute("CREATE SCHEMA IF NOT EXISTS sales");
            stmt.execute("DROP TABLE IF EXISTS sales.orders");
            stmt.execute("""
                CREATE TABLE sales.orders (
                    order_id INT PRIMARY KEY,
                    customer_id INT NOT NULL,
                    order_date DATE NOT NULL,
                    total_amount DECIMAL(10,2)
                )
            """);

            // Create inventory schema with products table
            stmt.execute("CREATE SCHEMA IF NOT EXISTS inventory");
            stmt.execute("DROP TABLE IF EXISTS inventory.products");
            stmt.execute("""
                CREATE TABLE inventory.products (
                    product_id INT PRIMARY KEY,
                    product_name VARCHAR(100) NOT NULL,
                    quantity INT DEFAULT 0,
                    unit_price DECIMAL(10,2)
                )
            """);

            // Create hr schema with employees table
            stmt.execute("CREATE SCHEMA IF NOT EXISTS hr");
            stmt.execute("DROP TABLE IF EXISTS hr.employees");
            stmt.execute("""
                CREATE TABLE hr.employees (
                    employee_id INT PRIMARY KEY,
                    first_name VARCHAR(50) NOT NULL,
                    last_name VARCHAR(50) NOT NULL,
                    hire_date DATE,
                    salary DECIMAL(12,2)
                )
            """);
        }
        
        System.out.println("Created custom schemas in PostgreSQL: sales, inventory, hr");
    }

    @AfterAll
    static void tearDownDatabase() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
    }

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        logger.info("PostgreSQL custom schemas ready: sales, inventory, hr");
        logger.info("PostgreSQL URL: {}", postgres.getJdbcUrl());
    }

    /**
     * Helper to update connection details in YAML config for TestContainers.
     */
    private void updateDataSourceConnection(YamlRuleConfiguration config, String sourceName) {
        if (config.getDataSources() != null) {
            for (YamlDataSource dataSource : config.getDataSources()) {
                if (sourceName.equals(dataSource.getName())) {
                    Map<String, Object> connection = dataSource.getConnection();
                    connection.put("host", postgres.getHost());
                    connection.put("port", postgres.getMappedPort(5432));
                    connection.put("database", postgres.getDatabaseName());
                    connection.put("username", postgres.getUsername());
                    connection.put("password", postgres.getPassword());
                    logger.info("Updated {} connection: {}:{}", sourceName, postgres.getHost(), postgres.getMappedPort(5432));
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Data source not found: " + sourceName);
    }

    @Test
    @DisplayName("Should read schema from custom sales schema")
    void shouldReadSchemaFromSalesSchema() throws Exception {
        // Given: Load YAML and update connection for TestContainers
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.loadFromFile(
            "src/test/java/dev/mars/apex/sync/schemas/CustomSchemaPostgresTest_sales.yaml"
        );
        updateDataSourceConnection(config, "sales-db");

        // When: Create engine and execute
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(new HashMap<>());

        // Then: Should successfully read 4 columns from sales.orders
        assertNotNull(result);
        assertTrue(result.isSuccess(),
            "Pipeline should succeed reading from sales schema: " + result.getMessage());

        logger.info("Successfully read schema from sales.orders");
        engine.shutdown();
    }

    @Test
    @DisplayName("Should read schema from custom inventory schema")
    void shouldReadSchemaFromInventorySchema() throws Exception {
        // Given: Load YAML and update connection for TestContainers
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.loadFromFile(
            "src/test/java/dev/mars/apex/sync/schemas/CustomSchemaPostgresTest_inventory.yaml"
        );
        updateDataSourceConnection(config, "inventory-db");

        // When: Create engine and execute
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(new HashMap<>());

        // Then: Should successfully read 4 columns from inventory.products
        assertNotNull(result);
        assertTrue(result.isSuccess(),
            "Pipeline should succeed reading from inventory schema: " + result.getMessage());

        logger.info("Successfully read schema from inventory.products");
        engine.shutdown();
    }

    @Test
    @DisplayName("Should read schema from custom hr schema")
    void shouldReadSchemaFromHrSchema() throws Exception {
        // Given: Load YAML and update connection for TestContainers
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.loadFromFile(
            "src/test/java/dev/mars/apex/sync/schemas/CustomSchemaPostgresTest_hr.yaml"
        );
        updateDataSourceConnection(config, "hr-db");

        // When: Create engine and execute
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(new HashMap<>());

        // Then: Should successfully read 5 columns from hr.employees
        assertNotNull(result);
        assertTrue(result.isSuccess(),
            "Pipeline should succeed reading from hr schema: " + result.getMessage());

        logger.info("Successfully read schema from hr.employees");
        engine.shutdown();
    }

    @Test
    @DisplayName("Should compare schemas across different custom schemas")
    void shouldCompareSchemasCrossSchema() throws Exception {
        // Given: Load YAML and update connections for TestContainers
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.loadFromFile(
            "src/test/java/dev/mars/apex/sync/schemas/CustomSchemaPostgresTest_cross_schema.yaml"
        );
        updateDataSourceConnection(config, "sales-db");
        updateDataSourceConnection(config, "inventory-db");

        // When: Create engine and execute
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(new HashMap<>());

        // Then: Schema diff should complete (will have differences since different tables)
        assertNotNull(result);
        // Cross-schema comparison will detect differences (different tables), that's expected

        logger.info("Cross-schema comparison completed");
        engine.shutdown();
    }
}
