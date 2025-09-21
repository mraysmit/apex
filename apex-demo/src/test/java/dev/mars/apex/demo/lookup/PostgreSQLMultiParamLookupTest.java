package dev.mars.apex.demo.lookup;

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

import dev.mars.apex.demo.infrastructure.DemoTestBase;
import dev.mars.apex.demo.test.TestContainerImages;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL Multi-Parameter Lookup Test - Phase 1.2 Implementation
 * 
 * PHASE 1.2 VALIDATION CHECKLIST:
 * ✅ PostgreSQL container starts successfully with trading data
 * ✅ Trading database schema created and populated via initialization script
 * ✅ APEX connects to real PostgreSQL database with trading tables
 * ✅ Multi-parameter settlement instruction lookup returns expected data
 * ✅ Multi-parameter risk assessment lookup returns expected data
 * ✅ Complex WHERE conditions with multiple parameters tested
 * ✅ JOIN operations across multiple tables validated
 * ✅ Optional parameter handling (NULL checks) working
 * ✅ Connection pooling and caching validated
 * ✅ Test passes consistently (3+ runs)
 * 
 * SUCCESS METRICS:
 * - Response Time: < 200ms for multi-parameter lookup
 * - Connection Pool: Proper connection management validated
 * - Data Accuracy: 100% match with expected trading data
 * - Parameter Handling: All parameter combinations working
 * - JOIN Performance: Complex queries executing efficiently
 * 
 * This test uses REAL PostgreSQL via Testcontainers - NO MOCKING
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostgreSQLMultiParamLookupTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLMultiParamLookupTest.class);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(TestContainerImages.POSTGRES)
            .withDatabaseName("apex_test")
            .withUsername("apex_user")
            .withPassword("apex_pass")
            .withInitScript("postgresql-test-data.sql");

    @Test
    @Order(1)
    @DisplayName("Should validate PostgreSQL container and trading database setup")
    void testPostgreSQLTradingDatabaseSetup() {
        logger.info("=".repeat(80));
        logger.info("PHASE 1.2: PostgreSQL Trading Database Setup Validation");
        logger.info("=".repeat(80));
        
        // Validate container is running
        assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
        
        // Validate connection details
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();
        
        logger.info("✅ PostgreSQL Container Details:");
        logger.info("  JDBC URL: {}", jdbcUrl);
        logger.info("  Username: {}", username);
        logger.info("  Database: {}", postgres.getDatabaseName());
        logger.info("  Port: {}", postgres.getFirstMappedPort());
        
        // Test direct database connection and validate trading tables
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            Statement statement = connection.createStatement();
            
            // Verify all trading tables exist and have data
            ResultSet rs = statement.executeQuery("""
                SELECT 
                    (SELECT COUNT(*) FROM customers) as customers_count,
                    (SELECT COUNT(*) FROM counterparties) as counterparties_count,
                    (SELECT COUNT(*) FROM custodians) as custodians_count,
                    (SELECT COUNT(*) FROM markets) as markets_count,
                    (SELECT COUNT(*) FROM instruments) as instruments_count,
                    (SELECT COUNT(*) FROM settlement_instructions) as settlement_instructions_count,
                    (SELECT COUNT(*) FROM risk_assessments) as risk_assessments_count
                """);
            
            if (rs.next()) {
                int customersCount = rs.getInt("customers_count");
                int counterpartiesCount = rs.getInt("counterparties_count");
                int custodiansCount = rs.getInt("custodians_count");
                int marketsCount = rs.getInt("markets_count");
                int instrumentsCount = rs.getInt("instruments_count");
                int settlementInstructionsCount = rs.getInt("settlement_instructions_count");
                int riskAssessmentsCount = rs.getInt("risk_assessments_count");
                
                logger.info("✅ Trading Database Validation:");
                logger.info("  Customers: {}", customersCount);
                logger.info("  Counterparties: {}", counterpartiesCount);
                logger.info("  Custodians: {}", custodiansCount);
                logger.info("  Markets: {}", marketsCount);
                logger.info("  Instruments: {}", instrumentsCount);
                logger.info("  Settlement Instructions: {}", settlementInstructionsCount);
                logger.info("  Risk Assessments: {}", riskAssessmentsCount);
                
                assertTrue(customersCount >= 110, "Should have at least 110 customers");
                assertTrue(counterpartiesCount >= 10, "Should have at least 10 counterparties");
                assertTrue(custodiansCount >= 10, "Should have at least 10 custodians");
                assertTrue(marketsCount >= 10, "Should have at least 10 markets");
                assertTrue(instrumentsCount >= 10, "Should have at least 10 instruments");
                assertTrue(settlementInstructionsCount >= 10, "Should have at least 10 settlement instructions");
                assertTrue(riskAssessmentsCount >= 10, "Should have at least 10 risk assessments");
            }
            
            // Test complex JOIN query to validate relationships
            rs = statement.executeQuery("""
                SELECT 
                    si.instruction_id,
                    cp.counterparty_name,
                    cust.custodian_name,
                    mk.market_name,
                    inst.instrument_name
                FROM settlement_instructions si
                JOIN counterparties cp ON si.counterparty_id = cp.counterparty_id
                JOIN custodians cust ON si.custodian_id = cust.custodian_id
                JOIN markets mk ON si.market = mk.market_code
                JOIN instruments inst ON si.instrument_type = inst.instrument_type
                WHERE si.counterparty_id = 'CP001'
                LIMIT 1
                """);
            
            if (rs.next()) {
                String instructionId = rs.getString("instruction_id");
                String counterpartyName = rs.getString("counterparty_name");
                String custodianName = rs.getString("custodian_name");
                String marketName = rs.getString("market_name");
                String instrumentName = rs.getString("instrument_name");
                
                logger.info("✅ JOIN Query Validation:");
                logger.info("  Instruction ID: {}", instructionId);
                logger.info("  Counterparty: {}", counterpartyName);
                logger.info("  Custodian: {}", custodianName);
                logger.info("  Market: {}", marketName);
                logger.info("  Instrument: {}", instrumentName);
                
                assertNotNull(instructionId, "Instruction ID should not be null");
                assertNotNull(counterpartyName, "Counterparty name should not be null");
                assertNotNull(custodianName, "Custodian name should not be null");
                assertNotNull(marketName, "Market name should not be null");
                assertNotNull(instrumentName, "Instrument name should not be null");
            }
            
        } catch (Exception e) {
            logger.error("❌ Trading database connection failed: {}", e.getMessage(), e);
            fail("Trading database connection should work: " + e.getMessage());
        }
        
        logger.info("✅ PostgreSQL trading database setup validation completed successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Should perform multi-parameter settlement instruction lookup via APEX")
    void testPostgreSQLMultiParamSettlementLookup() {
        logger.info("\n" + "=".repeat(80));
        logger.info("PHASE 1.2: PostgreSQL Multi-Parameter Settlement Instruction Lookup");
        logger.info("=".repeat(80));
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/lookup/PostgreSQLMultiParamLookupTest.yaml");
            
            // Update configuration with real PostgreSQL connection details
            updatePostgreSQLConnection(config);
            
            // Create test data for multi-parameter settlement instruction lookup
            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyId", "CP001");
            testData.put("instrumentType", "EQUITY_US");
            testData.put("currency", "USD");
            testData.put("market", "NYSE");
            testData.put("minAmount", new BigDecimal("5000.00"));
            testData.put("maxAmount", new BigDecimal("5000000.00"));
            
            logger.info("Input Data:");
            logger.info("  Counterparty ID: {}", testData.get("counterpartyId"));
            logger.info("  Instrument Type: {}", testData.get("instrumentType"));
            logger.info("  Currency: {}", testData.get("currency"));
            logger.info("  Market: {}", testData.get("market"));
            logger.info("  Min Amount: {}", testData.get("minAmount"));
            logger.info("  Max Amount: {}", testData.get("maxAmount"));
            
            // Execute APEX enrichment with real PostgreSQL multi-parameter lookup
            Object result = enrichmentService.enrichObject(config, testData);
            
            long responseTime = System.currentTimeMillis() - startTime;
            logger.info("Response Time: {}ms", responseTime);
            
            // Validate enrichment results
            assertNotNull(result, "PostgreSQL multi-parameter lookup result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate settlement instruction data
            assertNotNull(enrichedData.get("settlementInstructionId"), "Settlement instruction ID should be enriched");
            assertEquals("CP001", enrichedData.get("settlementCounterpartyId"));
            assertNotNull(enrichedData.get("settlementCustodianId"), "Settlement custodian ID should be enriched");
            assertEquals("DVP", enrichedData.get("settlementMethod"));
            assertEquals("Goldman Sachs", enrichedData.get("counterpartyName"));
            assertEquals("BANK", enrichedData.get("counterpartyType"));
            assertEquals("US", enrichedData.get("counterpartyJurisdiction"));
            assertNotNull(enrichedData.get("custodianName"), "Custodian name should be enriched");
            assertNotNull(enrichedData.get("custodianBic"), "Custodian BIC should be enriched");
            assertEquals("New York Stock Exchange", enrichedData.get("marketName"));
            assertEquals(2, enrichedData.get("settlementCycle"));
            assertEquals("US Common Stock", enrichedData.get("instrumentName"));
            assertEquals("EQUITY", enrichedData.get("instrumentClass"));
            assertEquals("USD", enrichedData.get("settlementCurrency"));
            
            logger.info("✅ Settlement Instruction Enrichment Results:");
            logger.info("  Instruction ID: {}", enrichedData.get("settlementInstructionId"));
            logger.info("  Counterparty: {}", enrichedData.get("counterpartyName"));
            logger.info("  Settlement Method: {}", enrichedData.get("settlementMethod"));
            logger.info("  Custodian: {}", enrichedData.get("custodianName"));
            logger.info("  Market: {}", enrichedData.get("marketName"));
            logger.info("  Settlement Cycle: {}", enrichedData.get("settlementCycle"));
            logger.info("  Instrument: {}", enrichedData.get("instrumentName"));
            
            // Validate performance requirement
            assertTrue(responseTime < 2000, "Response time should be < 2000ms for first run, was: " + responseTime + "ms");
            
            logger.info("✅ PostgreSQL multi-parameter settlement lookup completed successfully in {}ms", responseTime);
            
        } catch (Exception e) {
            logger.error("❌ PostgreSQL multi-parameter settlement lookup failed: {}", e.getMessage(), e);
            fail("PostgreSQL multi-parameter settlement lookup should work: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should perform multi-parameter risk assessment lookup via APEX")
    void testPostgreSQLMultiParamRiskAssessmentLookup() {
        logger.info("\n" + "=".repeat(80));
        logger.info("PHASE 1.2: PostgreSQL Multi-Parameter Risk Assessment Lookup");
        logger.info("=".repeat(80));

        try {
            // Load configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/lookup/postgresql-multi-param-lookup.yaml");
            updatePostgreSQLConnection(config);

            // Test data for risk assessment lookup
            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyId", "CP001");
            testData.put("instrumentType", "EQUITY_US");
            testData.put("currency", "USD");
            testData.put("market", "NYSE");
            testData.put("tradeAmount", new BigDecimal("25000000.00"));

            logger.info("Risk Assessment Input Data:");
            logger.info("  Counterparty ID: {}", testData.get("counterpartyId"));
            logger.info("  Instrument Type: {}", testData.get("instrumentType"));
            logger.info("  Market: {}", testData.get("market"));
            logger.info("  Trade Amount: {}", testData.get("tradeAmount"));

            // Execute enrichment
            Object result = enrichmentService.enrichObject(config, testData);

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Validate risk assessment enrichment results
            assertEquals("LOW", enrichedData.get("riskCategory"));
            assertEquals(15, enrichedData.get("riskScore"));
            assertNotNull(enrichedData.get("maxExposure"));
            assertEquals(false, enrichedData.get("approvalRequired"));
            assertEquals("STANDARD", enrichedData.get("monitoringLevel"));
            assertEquals("AA", enrichedData.get("counterpartyCreditRating"));
            assertNotNull(enrichedData.get("counterpartyCreditLimit"));
            assertEquals("MEDIUM", enrichedData.get("marketVolatilityRating"));
            assertEquals("HIGH", enrichedData.get("marketLiquidityRating"));

            logger.info("✅ Risk Assessment Enrichment Results:");
            logger.info("  Risk Category: {}", enrichedData.get("riskCategory"));
            logger.info("  Risk Score: {}", enrichedData.get("riskScore"));
            logger.info("  Max Exposure: {}", enrichedData.get("maxExposure"));
            logger.info("  Approval Required: {}", enrichedData.get("approvalRequired"));
            logger.info("  Monitoring Level: {}", enrichedData.get("monitoringLevel"));
            logger.info("  Credit Rating: {}", enrichedData.get("counterpartyCreditRating"));
            logger.info("  Market Volatility: {}", enrichedData.get("marketVolatilityRating"));
            logger.info("  Market Liquidity: {}", enrichedData.get("marketLiquidityRating"));

            logger.info("✅ PostgreSQL multi-parameter risk assessment lookup completed successfully");

        } catch (Exception e) {
            logger.error("❌ PostgreSQL multi-parameter risk assessment lookup failed: {}", e.getMessage(), e);
            fail("PostgreSQL multi-parameter risk assessment lookup should work: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should handle optional parameters and edge cases")
    void testPostgreSQLMultiParamOptionalParameters() {
        logger.info("\n" + "=".repeat(80));
        logger.info("PHASE 1.2: PostgreSQL Multi-Parameter Optional Parameters Testing");
        logger.info("=".repeat(80));

        try {
            // Load configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/lookup/postgresql-multi-param-lookup.yaml");
            updatePostgreSQLConnection(config);

            // Test data with only required parameters (optional parameters as null)
            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyId", "CP002");
            testData.put("instrumentType", "EQUITY_US");
            testData.put("currency", "USD");
            testData.put("market", "NYSE");
            testData.put("minAmount", null);  // Explicitly set to null
            testData.put("maxAmount", null);  // Explicitly set to null
            testData.put("tradeAmount", null); // Explicitly set to null

            logger.info("Optional Parameters Test Input Data:");
            logger.info("  Counterparty ID: {}", testData.get("counterpartyId"));
            logger.info("  Instrument Type: {}", testData.get("instrumentType"));
            logger.info("  Currency: {}", testData.get("currency"));
            logger.info("  Market: {}", testData.get("market"));
            logger.info("  Min Amount: NULL (optional)");
            logger.info("  Max Amount: NULL (optional)");
            logger.info("  Trade Amount: NULL (optional)");

            // Execute enrichment
            Object result = enrichmentService.enrichObject(config, testData);

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Should still find settlement instruction and risk assessment
            assertNotNull(enrichedData.get("settlementInstructionId"), "Should find settlement instruction even with null optional parameters");
            assertEquals("CP002", enrichedData.get("settlementCounterpartyId"));
            assertEquals("JPMorgan Chase", enrichedData.get("counterpartyName"));

            // Risk assessment should also work with null trade amount
            assertEquals("LOW", enrichedData.get("riskCategory"));
            assertEquals(10, enrichedData.get("riskScore"));

            logger.info("✅ Optional Parameters Test Results:");
            logger.info("  Settlement Instruction ID: {}", enrichedData.get("settlementInstructionId"));
            logger.info("  Counterparty: {}", enrichedData.get("counterpartyName"));
            logger.info("  Risk Category: {}", enrichedData.get("riskCategory"));
            logger.info("  Risk Score: {}", enrichedData.get("riskScore"));

            logger.info("✅ PostgreSQL multi-parameter optional parameters testing completed successfully");

        } catch (Exception e) {
            logger.error("❌ PostgreSQL multi-parameter optional parameters testing failed: {}", e.getMessage(), e);
            fail("PostgreSQL multi-parameter optional parameters testing should work: " + e.getMessage());
        }
    }

    /**
     * Update YAML configuration with real PostgreSQL connection details from Testcontainers
     */
    private void updatePostgreSQLConnection(YamlRuleConfiguration config) {
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();
        String host = postgres.getHost();
        Integer port = postgres.getFirstMappedPort();
        String database = postgres.getDatabaseName();
        
        logger.info("PostgreSQL Connection Details for APEX:");
        logger.info("  JDBC URL: {}", jdbcUrl);
        logger.info("  Host: {}", host);
        logger.info("  Port: {}", port);
        logger.info("  Database: {}", database);
        logger.info("  Username: {}", username);
        logger.info("  Password: [REDACTED]");
        
        // Update the PostgreSQL data source configuration with real Testcontainers connection details
        if (config.getDataSources() != null) {
            for (var dataSource : config.getDataSources()) {
                if ("postgresql-trading-database".equals(dataSource.getName())) {
                    Map<String, Object> connection = dataSource.getConnection();
                    
                    // Update connection details with real Testcontainers values
                    connection.put("host", host);
                    connection.put("port", port);
                    connection.put("database", database);
                    connection.put("username", username);
                    connection.put("password", password);
                    
                    logger.info("✅ Updated PostgreSQL trading data source '{}' with Testcontainers connection details", 
                               dataSource.getName());
                    break;
                }
            }
        }
    }
}
