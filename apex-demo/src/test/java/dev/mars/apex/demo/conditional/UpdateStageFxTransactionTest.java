/*
 * Copyright (c) 2024 APEX Rules Engine Contributors
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

package dev.mars.apex.demo.conditional;

import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for Update-Stage-FX-Transaction.yaml
 * 
 * This test demonstrates FX transaction stage updates with:
 * - Currency rank enrichment for buy/sell currencies
 * - Complex conditional NDF (Non-Deliverable Forward) logic
 * - External to internal code mapping
 * - Database lookups and field mappings
 * - Multi-stage conditional processing
 * 
 * Following the latest patterns from demo.lookup package tests.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Update Stage FX Transaction Processing Test")
public class UpdateStageFxTransactionTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(UpdateStageFxTransactionTest.class);

    @BeforeAll
    static void setUpDatabase() {
        logger.info("================================================================================");
        logger.info("Setting up H2 database for FX transaction processing demo...");
        logger.info("================================================================================");
        
        try (Connection conn = DriverManager.getConnection("jdbc:h2:./target/h2-demo/fx_transaction_demo", "sa", "")) {
            
            // Create currency rank table
            String createCurrencyRankTable = """
                CREATE TABLE IF NOT EXISTS VW_REF_CURRENCY_RANK (
                    currency_code VARCHAR(3) PRIMARY KEY,
                    currency_rank INTEGER NOT NULL,
                    currency_name VARCHAR(50),
                    is_major BOOLEAN DEFAULT FALSE
                )
                """;
            
            // Create translation parameters table
            String createTranslationParamsTable = """
                CREATE TABLE IF NOT EXISTS T_TRANSLATION_PARAMS (
                    translation_type VARCHAR(20),
                    client_code VARCHAR(10),
                    system_code VARCHAR(10),
                    is_translate INTEGER DEFAULT 0,
                    PRIMARY KEY (translation_type, client_code, system_code)
                )
                """;
            
            // Create code mapping table
            String createCodeMappingTable = """
                CREATE TABLE IF NOT EXISTS T_CODE_MAPPING (
                    translation_type VARCHAR(20),
                    client_code VARCHAR(10),
                    system_code VARCHAR(10),
                    external_code VARCHAR(20),
                    external_subcode VARCHAR(20),
                    internal_code VARCHAR(20),
                    PRIMARY KEY (translation_type, client_code, system_code, external_code)
                )
                """;
            
            try (PreparedStatement stmt1 = conn.prepareStatement(createCurrencyRankTable);
                 PreparedStatement stmt2 = conn.prepareStatement(createTranslationParamsTable);
                 PreparedStatement stmt3 = conn.prepareStatement(createCodeMappingTable)) {
                
                stmt1.executeUpdate();
                stmt2.executeUpdate();
                stmt3.executeUpdate();
            }
            
            // Insert test data for currency ranks (H2 compatible)
            String insertCurrencyData = """
                MERGE INTO VW_REF_CURRENCY_RANK (currency_code, currency_rank, currency_name, is_major) VALUES
                ('USD', 1, 'US Dollar', TRUE),
                ('EUR', 2, 'Euro', TRUE),
                ('GBP', 3, 'British Pound', TRUE),
                ('JPY', 4, 'Japanese Yen', TRUE),
                ('CHF', 5, 'Swiss Franc', TRUE),
                ('CAD', 6, 'Canadian Dollar', FALSE),
                ('AUD', 7, 'Australian Dollar', FALSE),
                ('SGD', 8, 'Singapore Dollar', FALSE)
                """;

            // Insert test data for translation parameters (H2 compatible)
            String insertTranslationData = """
                MERGE INTO T_TRANSLATION_PARAMS (translation_type, client_code, system_code, is_translate) VALUES
                ('IS_NDF', 'CLIENT1', 'SWIFT', 1),
                ('IS_NDF', 'CLIENT2', 'SWIFT', 0),
                ('CURRENCY', 'CLIENT1', 'SWIFT', 1)
                """;

            // Insert test data for code mappings (H2 compatible)
            String insertCodeMappingData = """
                MERGE INTO T_CODE_MAPPING (translation_type, client_code, system_code, external_code, internal_code) VALUES
                ('IS_NDF', 'CLIENT1', 'SWIFT', '0', 'NO'),
                ('IS_NDF', 'CLIENT1', 'SWIFT', '1', 'YES'),
                ('IS_NDF', 'CLIENT1', 'SWIFT', 'N', 'NO'),
                ('IS_NDF', 'CLIENT1', 'SWIFT', 'Y', 'YES')
                """;
            
            try (PreparedStatement stmt1 = conn.prepareStatement(insertCurrencyData);
                 PreparedStatement stmt2 = conn.prepareStatement(insertTranslationData);
                 PreparedStatement stmt3 = conn.prepareStatement(insertCodeMappingData)) {
                
                stmt1.executeUpdate();
                stmt2.executeUpdate();
                stmt3.executeUpdate();
            }
            
            logger.info("✅ H2 database setup completed successfully");
            
        } catch (SQLException e) {
            logger.error("Failed to set up H2 database", e);
            throw new RuntimeException("Database setup failed", e);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test Currency Rank Enrichment for Buy and Sell Currencies")
    void testCurrencyRankEnrichment() {
        logger.info("=== Testing Currency Rank Enrichment ===");
        
        try {
            // Load simplified YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/UpdateStageFxTransactionSimpleTest.yaml");

            // Verify we have the expected enrichments
            assertEquals(9, config.getEnrichments().size(), "Should have exactly 9 enrichments");
            
            // Create test data with FX transaction
            Map<String, Object> testData = new HashMap<>();
            testData.put("BUY_CURRENCY", "USD");
            testData.put("SELL_CURRENCY", "EUR");
            testData.put("TRADE_AMOUNT", 1000000.00);
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("CLIENT_CODE", "CLIENT1");
            
            logger.debug("Input FX transaction data: {}", testData);
            
            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            
            // Validate enrichment results
            assertNotNull(result, "Currency rank enrichment result should not be null");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            logger.debug("Enriched FX transaction: {}", enrichedData);
            
            // Validate currency rank enrichments
            assertNotNull(enrichedData.get("BUY_CURRENCY_RANK"), "Buy currency rank should be enriched");
            assertNotNull(enrichedData.get("SELL_CURRENCY_RANK"), "Sell currency rank should be enriched");
            
            // Validate specific currency ranks
            assertEquals(1, enrichedData.get("BUY_CURRENCY_RANK"), "USD should have rank 1");
            assertEquals(2, enrichedData.get("SELL_CURRENCY_RANK"), "EUR should have rank 2");
            
            logger.info("✅ Currency rank enrichment completed successfully");
            logger.info("  - Buy Currency (USD) Rank: {}", enrichedData.get("BUY_CURRENCY_RANK"));
            logger.info("  - Sell Currency (EUR) Rank: {}", enrichedData.get("SELL_CURRENCY_RANK"));
            
        } catch (Exception e) {
            logger.error("Currency rank enrichment test failed", e);
            fail("Currency rank enrichment test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test NDF Conditional Logic - Rule 1 (IS_NDF in {0,1} and SWIFT)")
    void testNdfConditionalLogicRule1() {
        logger.info("=== Testing NDF Conditional Logic - Rule 1 ===");
        
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/UpdateStageFxTransactionSimpleTest.yaml");
            
            // Test data that should trigger Rule 1 (IS_NDF = '1' and SYSTEM_CODE = 'SWIFT')
            Map<String, Object> testData = new HashMap<>();
            testData.put("IS_NDF", "1");
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("BUY_CURRENCY", "USD");
            testData.put("SELL_CURRENCY", "JPY");
            testData.put("CLIENT_CODE", "CLIENT1");
            
            logger.debug("NDF Rule 1 test data: {}", testData);
            
            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            
            // Validate results
            assertNotNull(result, "NDF Rule 1 result should not be null");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            logger.debug("NDF Rule 1 enriched data: {}", enrichedData);
            
            // In the simplified version, IS_NDF='1' triggers rule builder function (not direct mapping)
            // Validate that Rule 2 flag should be true (since IS_NDF='1' is not in the direct mapping set)
            assertTrue((Boolean) enrichedData.get("run_rule_builder_function_flag"), "Rule 2 flag should be true for IS_NDF='1'");

            // Validate final result after rule builder processing
            assertEquals("YES", enrichedData.get("FINAL_IS_NDF"), "Final IS_NDF should be YES after processing '1'");
            
            logger.info("✅ NDF Rule 1 conditional logic completed successfully");
            logger.info("  - Rule Builder Flag: {}", enrichedData.get("run_rule_builder_function_flag"));
            logger.info("  - Final IS_NDF: {}", enrichedData.get("FINAL_IS_NDF"));
            
        } catch (Exception e) {
            logger.error("NDF Rule 1 test failed", e);
            fail("NDF Rule 1 test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test NDF Conditional Logic - Rule 2 (Complex Translation Logic)")
    void testNdfConditionalLogicRule2() {
        logger.info("=== Testing NDF Conditional Logic - Rule 2 ===");
        
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/UpdateStageFxTransactionSimpleTest.yaml");
            
            // Test data that should trigger Rule 2 (IS_NDF not in {0,1} but not null, and SWIFT)
            Map<String, Object> testData = new HashMap<>();
            testData.put("IS_NDF", "Y");  // Not in {0,1} but not null
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("BUY_CURRENCY", "GBP");
            testData.put("SELL_CURRENCY", "CHF");
            testData.put("CLIENT_CODE", "CLIENT1");
            
            logger.debug("NDF Rule 2 test data: {}", testData);
            
            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            
            // Validate results
            assertNotNull(result, "NDF Rule 2 result should not be null");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            logger.debug("NDF Rule 2 enriched data: {}", enrichedData);
            
            // Validate that Rule 2 flag should be true (since IS_NDF='Y' triggers rule builder)
            assertTrue((Boolean) enrichedData.get("run_rule_builder_function_flag"), "Rule 2 flag should be true for IS_NDF='Y'");

            // Validate final result after rule builder processing
            assertEquals("YES", enrichedData.get("FINAL_IS_NDF"), "Final IS_NDF should be YES after processing 'Y'");
            
            // Validate translation parameters are set
            assertEquals("IS_NDF", enrichedData.get("TRANSLATION_TYPE"), "Translation type should be set to IS_NDF");
            assertEquals("Y", enrichedData.get("EXTERNAL_CODE"), "External code should be set to IS_NDF value");

            // Validate internal code mapping result
            assertEquals("YES", enrichedData.get("INTERNAL_CODE"), "Internal code should be mapped to YES for Y");

            logger.info("✅ NDF Rule 2 conditional logic completed successfully");
            logger.info("  - Rule Builder Flag: {}", enrichedData.get("run_rule_builder_function_flag"));
            logger.info("  - Translation Type: {}", enrichedData.get("TRANSLATION_TYPE"));
            logger.info("  - External Code: {}", enrichedData.get("EXTERNAL_CODE"));
            logger.info("  - Internal Code: {}", enrichedData.get("INTERNAL_CODE"));
            logger.info("  - Final IS_NDF: {}", enrichedData.get("FINAL_IS_NDF"));
            
        } catch (Exception e) {
            logger.error("NDF Rule 2 test failed", e);
            fail("NDF Rule 2 test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test Complete FX Transaction Processing Workflow")
    void testCompleteFxTransactionWorkflow() {
        logger.info("=== Testing Complete FX Transaction Processing Workflow ===");
        
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/UpdateStageFxTransactionSimpleTest.yaml");
            
            // Comprehensive test data
            Map<String, Object> testData = new HashMap<>();
            testData.put("TRADE_ID", "FX-2025-001");
            testData.put("BUY_CURRENCY", "USD");
            testData.put("SELL_CURRENCY", "EUR");
            testData.put("TRADE_AMOUNT", 5000000.00);
            testData.put("IS_NDF", "N");  // Will trigger Rule 2 processing
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("CLIENT_CODE", "CLIENT1");
            testData.put("SETTLEMENT_DATE", "2025-10-15");
            
            logger.debug("Complete workflow test data: {}", testData);
            
            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            
            // Validate comprehensive results
            assertNotNull(result, "Complete workflow result should not be null");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            logger.debug("Complete workflow enriched data: {}", enrichedData);
            
            // Validate all enrichments were processed
            assertNotNull(enrichedData.get("BUY_CURRENCY_RANK"), "Buy currency rank should be enriched");
            assertNotNull(enrichedData.get("SELL_CURRENCY_RANK"), "Sell currency rank should be enriched");
            assertNotNull(enrichedData.get("run_rule_builder_function_flag"), "Rule 2 flag should be present");
            
            // Validate currency ranks
            assertEquals(1, enrichedData.get("BUY_CURRENCY_RANK"), "USD should have rank 1");
            assertEquals(2, enrichedData.get("SELL_CURRENCY_RANK"), "EUR should have rank 2");

            // Validate conditional logic results
            assertTrue((Boolean) enrichedData.get("run_rule_builder_function_flag"), "Rule 2 should be triggered");

            // Validate final NDF result
            assertEquals("NO", enrichedData.get("FINAL_IS_NDF"), "Final IS_NDF should be NO after processing 'N'");
            
            // Validate translation setup
            assertEquals("IS_NDF", enrichedData.get("TRANSLATION_TYPE"), "Translation type should be IS_NDF");
            assertEquals("N", enrichedData.get("EXTERNAL_CODE"), "External code should be N");
            
            // Validate original data preservation
            assertEquals("FX-2025-001", enrichedData.get("TRADE_ID"), "Trade ID should be preserved");
            assertEquals(5000000.00, enrichedData.get("TRADE_AMOUNT"), "Trade amount should be preserved");
            assertEquals("2025-10-15", enrichedData.get("SETTLEMENT_DATE"), "Settlement date should be preserved");
            
            logger.info("✅ Complete FX transaction workflow completed successfully");
            logger.info("  - Trade ID: {}", enrichedData.get("TRADE_ID"));
            logger.info("  - Buy Currency: {} (Rank: {})", enrichedData.get("BUY_CURRENCY"), enrichedData.get("BUY_CURRENCY_RANK"));
            logger.info("  - Sell Currency: {} (Rank: {})", enrichedData.get("SELL_CURRENCY"), enrichedData.get("SELL_CURRENCY_RANK"));
            logger.info("  - NDF Processing: Rule1={}, Rule2={}", enrichedData.get("is_ndf_if_result"), enrichedData.get("run_rule_builder_function_flag"));
            logger.info("  - Translation Setup: Type={}, Code={}", enrichedData.get("TRANSLATION_TYPE"), enrichedData.get("EXTERNAL_CODE"));
            logger.info("  - All {} enrichments processed successfully", config.getEnrichments().size());
            
        } catch (Exception e) {
            logger.error("Complete FX transaction workflow test failed", e);
            fail("Complete FX transaction workflow test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test APEX Services Initialization - FX Transaction Specific")
    public void testApexServicesInitializationFxTransaction() {
        logger.info("=== Testing APEX Services Initialization - FX Transaction Specific ===");

        // Call parent test first
        super.testApexServicesInitialization();

        // Additional FX-specific validations
        assertNotNull(yamlLoader, "YAML configuration loader should be initialized for FX processing");
        assertNotNull(enrichmentService, "Enrichment service should be initialized for FX processing");

        logger.info("✅ All APEX services properly initialized for FX transaction processing");
    }
}
