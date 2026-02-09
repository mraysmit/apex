package dev.mars.apex.core.dbschema;

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.model.YamlDataSource;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.test.TestContainerImages;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
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

/**
 * MultiSchemaDataSourceTest - APEX multi-schema data source configuration testing.
 *
 * <p>Tests APEX's ability to configure multiple PostgreSQL data sources with different
 * schemas from YAML and execute enrichments against the correct schemas with proper isolation.</p>
 *
 * <h2>APEX Functionality Tested:</h2>
 * <ul>
 *   <li>YAML multi-schema data source configuration parsing</li>
 *   <li>Multiple data sources with different schemas</li>
 *   <li>Enrichments executing against correct schemas</li>
 *   <li>Schema isolation validation between data sources</li>
 *   <li>Default schema behavior</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-01-15
 * @version 1.0
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MultiSchemaDataSourceTest {

    private static final Logger logger = LoggerFactory.getLogger(MultiSchemaDataSourceTest.class);
    private static final DockerImageName POSTGRES_IMAGE = 
        DockerImageName.parse(TestContainerImages.POSTGRES)
                       .asCompatibleSubstituteFor("postgres");

    @Container
    @SuppressWarnings("resource")
    GenericContainer<?> postgres = new GenericContainer<>(POSTGRES_IMAGE)
            .withEnv("POSTGRES_DB", "apex_jdbc_test")
            .withEnv("POSTGRES_USER", "apex_user")
            .withEnv("POSTGRES_PASSWORD", "apex_pass")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());

    private YamlConfigurationLoader yamlLoader = new YamlConfigurationLoader();

    private String jdbcUrl() {
        return "jdbc:postgresql://" + postgres.getHost() + ":" 
            + postgres.getMappedPort(5432) + "/apex_jdbc_test";
    }

    @BeforeEach
    void setupTestSchemas() throws Exception {
        if (!postgres.isRunning()) {
            return;
        }
        
        logger.info("Setting up test schemas for APEX schema configuration testing");

        int maxRetries = 3;
        int retryDelayMs = 1000;
        Connection conn = null;
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                conn = DriverManager.getConnection(jdbcUrl(), "apex_user", "apex_pass");
                break;
            } catch (Exception e) {
                if (i < maxRetries - 1) {
                    logger.info("Connection attempt {} failed, retrying in {}ms...", i + 1, retryDelayMs);
                    Thread.sleep(retryDelayMs);
                } else {
                    throw e;
                }
            }
        }

        try (Connection finalConn = conn;
             Statement stmt = finalConn.createStatement()) {

            stmt.execute("CREATE SCHEMA trading");
            stmt.execute("CREATE SCHEMA sales");
            stmt.execute("CREATE SCHEMA inventory");

            for (String schema : new String[]{"trading", "sales", "inventory"}) {
                stmt.execute(String.format("""
                    CREATE TABLE %s.products (
                        product_id VARCHAR(20) PRIMARY KEY,
                        product_name VARCHAR(100) NOT NULL,
                        schema_name VARCHAR(50) NOT NULL
                    )
                    """, schema));

                stmt.execute(String.format("""
                    INSERT INTO %s.products (product_id, product_name, schema_name)
                    VALUES ('AAPL', 'Apple Inc. (%s)', '%s')
                    """, schema, schema, schema));
            }

            logger.info("[OK] Created schemas with test data: trading, sales, inventory");
        }
    }

    private void updateDataSourceConnection(YamlRuleConfiguration config, String dataSourceName) {
        for (YamlDataSource ds : config.getDataSources()) {
            if (ds.getName().equals(dataSourceName)) {
                Map<String, Object> conn = ds.getConnection();
                conn.put("host", postgres.getHost());
                conn.put("port", postgres.getMappedPort(5432));
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("APEX should read schema parameter from YAML configuration")
    void testSchemaFromYamlConfiguration() throws Exception {
        logger.info("TEST: Validating APEX reads schema from YAML");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/resources/dbschema/MultiSchemaDataSourceTest.yaml");

        assertNotNull(config, "Configuration should load");
        assertEquals(3, config.getDataSources().size(), "Should have 3 data sources");

        var tradingDs = config.getDataSources().stream()
                .filter(ds -> ds.getName().equals("trading-schema-db"))
                .findFirst().orElse(null);
        assertNotNull(tradingDs);
        assertEquals("trading", tradingDs.getConnection().get("schema"));

        logger.info("[OK] APEX correctly read schema parameters from YAML");
    }

    @Test
    @Order(2)
    @DisplayName("APEX enrichments should execute against correct schemas")
    void testEnrichmentsAccessCorrectSchemas() throws Exception {
        logger.info("TEST: Validating APEX enrichments access correct schemas");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/resources/dbschema/MultiSchemaDataSourceTest.yaml");

        updateDataSourceConnection(config, "trading-schema-db");
        updateDataSourceConnection(config, "sales-schema-db");
        updateDataSourceConnection(config, "inventory-schema-db");

        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> testData = new HashMap<>();
        testData.put("product_id", "AAPL");

        RuleResult result = engine.evaluate(config, testData);
        Map<String, Object> enrichedData = result.getEnrichedData();

        assertEquals("Apple Inc. (trading)", enrichedData.get("trading_product"));
        assertEquals("trading", enrichedData.get("trading_schema"));
        assertEquals("Apple Inc. (sales)", enrichedData.get("sales_product"));
        assertEquals("sales", enrichedData.get("sales_schema"));
        assertEquals("Apple Inc. (inventory)", enrichedData.get("inventory_product"));
        assertEquals("inventory", enrichedData.get("inventory_schema"));

        logger.info("[OK] All enrichments accessed their configured schemas correctly");
    }

    @Test
    @Order(3)
    @DisplayName("Schema isolation - no cross-schema data leakage")
    void testSchemaIsolation() throws Exception {
        logger.info("TEST: Validating schema isolation via APEX");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/resources/dbschema/MultiSchemaDataSourceTest.yaml");

        updateDataSourceConnection(config, "trading-schema-db");
        updateDataSourceConnection(config, "sales-schema-db");
        updateDataSourceConnection(config, "inventory-schema-db");

        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> testData = new HashMap<>();
        testData.put("product_id", "AAPL");

        RuleResult result = engine.evaluate(config, testData);
        
        Map<String, Object> enrichedData = result.getEnrichedData();
        
        // Verify each schema returns distinct values
        String tradingProduct = (String) enrichedData.get("trading_product");
        String salesProduct = (String) enrichedData.get("sales_product");
        String inventoryProduct = (String) enrichedData.get("inventory_product");
        
        assertNotNull(tradingProduct, "Trading schema should return data");
        assertNotNull(salesProduct, "Sales schema should return data");
        assertNotNull(inventoryProduct, "Inventory schema should return data");
        
        assertNotEquals(tradingProduct, salesProduct, "Trading and sales schemas should have different data");
        assertNotEquals(salesProduct, inventoryProduct, "Sales and inventory schemas should have different data");

        logger.info("[OK] Schema isolation validated - no cross-schema leakage");
    }

    @Test
    @Order(4)
    @DisplayName("APEX should handle default public schema")
    void testDefaultPublicSchema() throws Exception {
        logger.info("TEST: Validating APEX handles default public schema");

        try (Connection conn = DriverManager.getConnection(jdbcUrl(), "apex_user", "apex_pass");
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("""
                CREATE TABLE products (
                    product_id VARCHAR(20) PRIMARY KEY,
                    product_name VARCHAR(100) NOT NULL,
                    schema_name VARCHAR(50) NOT NULL
                )
                """);
            
            stmt.execute("""
                INSERT INTO products VALUES ('MSFT', 'Microsoft', 'public')
                """);
        }

        String yamlConfig = """
            metadata:
              type: "rule-config"
              version: "2.1"
              author: "APEX Test Suite"
            
            data-sources:
              - name: "public-db"
                type: "database"
                source-type: "postgresql"
                connection:
                  database: "apex_jdbc_test"
                  username: "apex_user"
                  password: "apex_pass"
                  host: "%s"
                  port: %d
            
            enrichments:
              - id: "public-lookup"
                type: "lookup-enrichment"
                lookup-config:
                  lookup-key: "#product_id"
                  lookup-dataset:
                    type: "database"
                    data-source-ref: "public-db"
                    key-field: "key"
                    query: "SELECT schema_name FROM products WHERE product_id = :key"
                field-mappings:
                  - source-field: "schema_name"
                    target-field: "schema_name"
            """.formatted(postgres.getHost(), postgres.getMappedPort(5432));

        YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlConfig);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> testData = new HashMap<>();
        testData.put("product_id", "MSFT");

        RuleResult result = engine.evaluate(config, testData);
        assertEquals("public", result.getEnrichedData().get("schema_name"));

        logger.info("[OK] APEX correctly defaults to public schema");
    }

    @Test
    @Order(5)
    @DisplayName("Multiple APEX data sources with different schemas")
    void testMultipleDataSourcesConfiguration() throws Exception {
        logger.info("TEST: Validating APEX multiple data source configuration");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/resources/dbschema/MultiSchemaDataSourceTest.yaml");

        assertEquals(3, config.getDataSources().size());
        assertEquals(3, config.getEnrichments().size());

        var schemas = config.getDataSources().stream()
                .map(ds -> (String) ds.getConnection().get("schema"))
                .distinct()
                .toList();
        
        assertEquals(3, schemas.size());
        assertTrue(schemas.contains("trading"));
        assertTrue(schemas.contains("sales"));
        assertTrue(schemas.contains("inventory"));

        logger.info("[OK] Multiple data sources configured with distinct schemas");
    }
}
