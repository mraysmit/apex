package dev.mars.apex.demo.enrichment;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.demo.util.TestContainerImages;
import org.junit.jupiter.api.*;
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
 * CustomSchemaEnrichmentTest - Demonstrates database enrichments with dynamic schema configuration.
 *
 * This test validates APEX's ability to:
 * 1. Use dynamic schema configuration via YAML (schema property in data-source connection)
 * 2. Execute queries without hardcoded schema prefixes
 * 3. Enrich data from tables and views in custom schemas
 * 
 * KEY FEATURE: The schema is configured in the data source's connection settings,
 * NOT hardcoded in SQL queries. This enables clean separation of business logic (YAML queries)
 * from infrastructure configuration (schema names).
 *
 * YAML CONFIGURATION PATTERN:
 * ```yaml
 * data-sources:
 *   - name: "my-database"
 *     connection:
 *       schema: "custom_schema"  # <-- Schema configured here, not in queries!
 * 
 * enrichments:
 *   - lookup-config:
 *       query: "SELECT * FROM products WHERE id = :id"  # <-- No schema prefix needed!
 * ```
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-01-14
 * @version 1.0 - Initial implementation demonstrating dynamic schema enrichment
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomSchemaEnrichmentTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CustomSchemaEnrichmentTest.class);
    private static final String CUSTOM_SCHEMA = "trading";

    private static final DockerImageName POSTGRES_IMAGE = 
        DockerImageName.parse(TestContainerImages.POSTGRES)
                       .asCompatibleSubstituteFor("postgres");


    /*
     * TESTCONTAINERS 2.0 PATTERN - DEMO/ENRICHMENT TESTING
     * 
     * Demonstrates APEX enrichment functionality with custom PostgreSQL schemas.
     * Uses instance GenericContainer for vendor-agnostic database testing.
     * 
     * DEMO-SPECIFIC PATTERN:
     * - Instance container (not static) for test isolation
     * - Each @Test method gets fresh database with clean schema state
     * - Manual JDBC URL via jdbcUrl() helper method
     * - TestContainerImages.POSTGRES constant ensures version consistency
     * - Hard-coded test credentials: apex_user/apex_pass
     * - getMappedPort(5432) for Docker dynamic port mapping
     * 
     * ENRICHMENT VALIDATION:
     * - Tests lookup enrichment against custom 'trading' schema
     * - Validates schema isolation (no accidental public schema queries)
     * - Demonstrates YAML-driven schema configuration
     * - Proves queries work without hard-coded schema prefixes
     * 
     * See apex-core tests for additional pattern examples:
     * - JdbcUrlSchemaParameterTest: Instance containers with retry logic
     * - PostgreSQLSchemaConfigurationTest: Instance pattern for schema config
     * - EnvironmentPromotionTest: Multi-environment simulation
     */
    @Container
    @SuppressWarnings("resource") // Testcontainers manages lifecycle automatically
    GenericContainer<?> postgres = new GenericContainer<>(POSTGRES_IMAGE)
            .withEnv("POSTGRES_DB", "apex_trading_test")
            .withEnv("POSTGRES_USER", "apex_user")
            .withEnv("POSTGRES_PASSWORD", "apex_pass")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());

    private String jdbcUrl() {
        return "jdbc:postgresql://" + postgres.getHost() + ":"
            + postgres.getMappedPort(5432) + "/apex_trading_test";
    }

    @BeforeEach
    void setupSchema() throws Exception {
        if (!postgres.isRunning()) {
            return;
        }

        logger.info("Setting up PostgreSQL test database with custom schema: {}", CUSTOM_SCHEMA);

        // Add retry logic for connection attempts to handle PostgreSQL startup timing
        int maxRetries = 3;
        int retryDelayMs = 1000;
        Connection conn = null;
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                conn = DriverManager.getConnection(jdbcUrl(), "apex_user", "apex_pass");
                logger.info("Successfully connected to PostgreSQL on attempt {}", i + 1);
                break;
            } catch (Exception e) {
                if (i < maxRetries - 1) {
                    logger.info("Connection attempt {} failed, retrying in {}ms... Error: {}", 
                        i + 1, retryDelayMs, e.getMessage());
                    Thread.sleep(retryDelayMs);
                } else {
                    logger.error("All connection attempts failed after {} retries", maxRetries);
                    throw e;
                }
            }
        }

        try (Connection finalConn = conn;
             Statement stmt = finalConn.createStatement()) {

            // Create custom schema
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + CUSTOM_SCHEMA);
            
            // Set search_path for the current user so all queries use the trading schema by default
            stmt.execute("ALTER DATABASE apex_trading_test SET search_path TO " + CUSTOM_SCHEMA + ", public");

            // Create products table in custom schema
            stmt.execute("""
                CREATE TABLE trading.products (
                    product_id VARCHAR(20) PRIMARY KEY,
                    product_name VARCHAR(100) NOT NULL,
                    product_type VARCHAR(30) NOT NULL,
                    asset_class VARCHAR(30) NOT NULL,
                    currency VARCHAR(3) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    min_trade_size DECIMAL(18,2),
                    max_trade_size DECIMAL(18,2),
                    created_date DATE DEFAULT CURRENT_DATE
                )
                """);

            // Create counterparties table
            stmt.execute("""
                CREATE TABLE trading.counterparties (
                    counterparty_id VARCHAR(20) PRIMARY KEY,
                    counterparty_name VARCHAR(100) NOT NULL,
                    lei_code VARCHAR(20),
                    credit_rating VARCHAR(10),
                    country_code VARCHAR(3) NOT NULL,
                    is_active BOOLEAN DEFAULT TRUE
                )
                """);

            // Create a view that joins products and counterparties (for complex lookups)
            stmt.execute("""
                CREATE VIEW trading.active_products_view AS
                SELECT 
                    product_id,
                    product_name,
                    product_type,
                    asset_class,
                    currency,
                    min_trade_size,
                    max_trade_size
                FROM trading.products
                WHERE status = 'ACTIVE'
                """);

            // Insert test data - Products
            stmt.execute("""
                INSERT INTO trading.products (product_id, product_name, product_type, asset_class, currency, min_trade_size, max_trade_size)
                VALUES 
                ('PROD001', 'EUR/USD FX Forward', 'FX_FORWARD', 'FX', 'USD', 100000.00, 50000000.00),
                ('PROD002', 'Gold Swap', 'COMMODITY_SWAP', 'COMMODITY', 'USD', 10000.00, 10000000.00),
                ('PROD003', 'Interest Rate Swap', 'IRS', 'RATES', 'EUR', 1000000.00, 100000000.00),
                ('PROD004', 'Credit Default Swap', 'CDS', 'CREDIT', 'USD', 500000.00, 25000000.00),
                ('PROD005', 'Equity Total Return Swap', 'TRS', 'EQUITY', 'GBP', 250000.00, 20000000.00)
                """);

            // Insert test data - Counterparties
            stmt.execute("""
                INSERT INTO trading.counterparties (counterparty_id, counterparty_name, lei_code, credit_rating, country_code)
                VALUES 
                ('CP001', 'Global Investment Bank', 'LEI123456789012', 'AA+', 'USA'),
                ('CP002', 'European Asset Manager', 'LEI987654321098', 'A', 'DEU'),
                ('CP003', 'Asian Hedge Fund', 'LEI456789012345', 'BBB+', 'SGP')
                """);

            logger.info("✅ Created custom schema '{}' with products, counterparties tables, and active_products_view", CUSTOM_SCHEMA);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should enrich trade data with product details from custom schema")
    void testProductEnrichmentFromCustomSchema() {
        logger.info("=== Test 1: Product Enrichment from Custom Schema ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/CustomSchemaEnrichmentTest.yaml");
            assertNotNull(config, "Configuration should load");

            // Update data source with Testcontainers connection
            updateDataSourceConnection(config, "trading-database");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("productId", "PROD001");
            tradeData.put("tradeAmount", 5000000.0);

            logger.info("Input: productId={}, tradeAmount={}", 
                tradeData.get("productId"), tradeData.get("tradeAmount"));

            RuleResult result = engine.evaluate(config, tradeData);
            assertNotNull(result, "Result should not be null");

            Map<String, Object> enrichedData = result.getEnrichedData();

            // Verify product data was enriched from custom schema
            assertEquals("EUR/USD FX Forward", enrichedData.get("productName"),
                "Product name should be enriched from trading.products");
            assertEquals("FX_FORWARD", enrichedData.get("productType"),
                "Product type should be enriched");
            assertEquals("FX", enrichedData.get("assetClass"),
                "Asset class should be enriched");
            assertEquals("USD", enrichedData.get("currency"),
                "Currency should be enriched");

            logger.info("✅ Product enrichment successful from schema '{}':", CUSTOM_SCHEMA);
            logger.info("   productName: {}", enrichedData.get("productName"));
            logger.info("   productType: {}", enrichedData.get("productType"));
            logger.info("   assetClass: {}", enrichedData.get("assetClass"));
            logger.info("   currency: {}", enrichedData.get("currency"));

        } catch (Exception e) {
            logger.error("❌ Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should enrich trade with counterparty details from custom schema")
    void testCounterpartyEnrichmentFromCustomSchema() {
        logger.info("=== Test 2: Counterparty Enrichment from Custom Schema ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/CustomSchemaEnrichmentTest.yaml");
            assertNotNull(config, "Configuration should load");

            updateDataSourceConnection(config, "trading-database");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("counterpartyId", "CP001");
            tradeData.put("productId", "PROD002");

            logger.info("Input: counterpartyId={}, productId={}", 
                tradeData.get("counterpartyId"), tradeData.get("productId"));

            RuleResult result = engine.evaluate(config, tradeData);
            Map<String, Object> enrichedData = result.getEnrichedData();

            // Verify counterparty data was enriched
            assertEquals("Global Investment Bank", enrichedData.get("counterpartyName"),
                "Counterparty name should be enriched");
            assertEquals("LEI123456789012", enrichedData.get("leiCode"),
                "LEI code should be enriched");
            assertEquals("AA+", enrichedData.get("creditRating"),
                "Credit rating should be enriched");
            assertEquals("USA", enrichedData.get("countryCode"),
                "Country code should be enriched");

            logger.info("✅ Counterparty enrichment successful from schema '{}':", CUSTOM_SCHEMA);
            logger.info("   counterpartyName: {}", enrichedData.get("counterpartyName"));
            logger.info("   leiCode: {}", enrichedData.get("leiCode"));
            logger.info("   creditRating: {}", enrichedData.get("creditRating"));
            logger.info("   countryCode: {}", enrichedData.get("countryCode"));

        } catch (Exception e) {
            logger.error("❌ Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should enrich from VIEW in custom schema")
    void testViewEnrichmentFromCustomSchema() {
        logger.info("=== Test 3: VIEW Enrichment from Custom Schema ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/CustomSchemaEnrichmentTest.yaml");
            assertNotNull(config, "Configuration should load");

            updateDataSourceConnection(config, "trading-database");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("productId", "PROD003");

            logger.info("Input: productId={} (querying VIEW)", tradeData.get("productId"));

            RuleResult result = engine.evaluate(config, tradeData);
            Map<String, Object> enrichedData = result.getEnrichedData();

            // Verify data from VIEW was enriched
            assertEquals("Interest Rate Swap", enrichedData.get("viewProductName"),
                "Product name should be enriched from VIEW");
            assertEquals("IRS", enrichedData.get("viewProductType"),
                "Product type should be enriched from VIEW");
            assertEquals("RATES", enrichedData.get("viewAssetClass"),
                "Asset class should be enriched from VIEW");
            assertNotNull(enrichedData.get("viewMinTradeSize"),
                "Min trade size should be enriched from VIEW");
            assertNotNull(enrichedData.get("viewMaxTradeSize"),
                "Max trade size should be enriched from VIEW");

            logger.info("✅ VIEW enrichment successful from schema '{}':", CUSTOM_SCHEMA);
            logger.info("   viewProductName: {}", enrichedData.get("viewProductName"));
            logger.info("   viewProductType: {}", enrichedData.get("viewProductType"));
            logger.info("   viewAssetClass: {}", enrichedData.get("viewAssetClass"));
            logger.info("   viewMinTradeSize: {}", enrichedData.get("viewMinTradeSize"));
            logger.info("   viewMaxTradeSize: {}", enrichedData.get("viewMaxTradeSize"));

        } catch (Exception e) {
            logger.error("❌ Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should validate trade size against product limits from custom schema")
    void testTradeSizeValidationWithSchemaEnrichment() {
        logger.info("=== Test 4: Trade Size Validation with Schema Enrichment ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/CustomSchemaEnrichmentTest.yaml");
            assertNotNull(config, "Configuration should load");

            updateDataSourceConnection(config, "trading-database");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            // Test with amount within limits
            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("productId", "PROD001");  // EUR/USD FX Forward: min=100000, max=50000000
            tradeData.put("tradeAmount", 5000000.0);

            logger.info("Input: productId={}, tradeAmount={}", 
                tradeData.get("productId"), tradeData.get("tradeAmount"));

            RuleResult result = engine.evaluate(config, tradeData);
            Map<String, Object> enrichedData = result.getEnrichedData();

            // Verify enrichment includes trade limits
            assertNotNull(enrichedData.get("minTradeSize"), "Min trade size should be enriched");
            assertNotNull(enrichedData.get("maxTradeSize"), "Max trade size should be enriched");

            // The trade amount (5M) should be within limits (100K - 50M)
            Number minSize = (Number) enrichedData.get("minTradeSize");
            Number maxSize = (Number) enrichedData.get("maxTradeSize");
            double tradeAmount = (Double) tradeData.get("tradeAmount");

            assertTrue(tradeAmount >= minSize.doubleValue(), 
                "Trade amount should be >= min trade size");
            assertTrue(tradeAmount <= maxSize.doubleValue(), 
                "Trade amount should be <= max trade size");

            logger.info("✅ Trade size validation successful:");
            logger.info("   tradeAmount: {} (within limits: {} - {})", 
                tradeAmount, minSize, maxSize);

        } catch (Exception e) {
            logger.error("❌ Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Update YAML data source configuration with real PostgreSQL connection details.
     */
    private void updateDataSourceConnection(YamlRuleConfiguration config, String dataSourceName) {
        String host = postgres.getHost();
        Integer port = postgres.getMappedPort(5432);
        String database = "apex_trading_test";
        String username = "apex_user";
        String password = "apex_pass";

        if (config.getDataSources() != null) {
            for (var dataSource : config.getDataSources()) {
                if (dataSourceName.equals(dataSource.getName())) {
                    Map<String, Object> connection = dataSource.getConnection();
                    connection.put("host", host);
                    connection.put("port", port);
                    connection.put("database", database);
                    connection.put("username", username);
                    connection.put("password", password);
                    connection.put("schema", CUSTOM_SCHEMA);  // Dynamic schema!
                    
                    logger.info("✅ Updated data source '{}' with schema '{}'", 
                        dataSourceName, CUSTOM_SCHEMA);
                    break;
                }
            }
        }
    }
}
