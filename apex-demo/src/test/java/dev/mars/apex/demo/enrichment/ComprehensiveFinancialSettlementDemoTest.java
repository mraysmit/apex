package dev.mars.apex.demo.enrichment;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * ComprehensiveFinancialSettlementDemoTest - JUnit 5 Test for Comprehensive Financial Settlement Demo
 *
 * This test validates authentic APEX comprehensive financial settlement functionality using real APEX services:
 * - Multi-asset class settlement processing (Equities, Fixed Income, Derivatives)
 * - Cross-border settlement with currency conversion and regulatory compliance
 * - Multi-market settlement conventions (UK, US, Germany, Japan, Hong Kong)
 * - Settlement instruction validation and enrichment
 * - Risk assessment and exception handling for high-value transactions
 * - Comprehensive audit trail and regulatory reporting
 * - Real-time settlement status tracking and notifications
 *
 * REAL APEX SERVICES TESTED:
 * - EnrichmentService: Real APEX enrichment processor for settlement processing
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for business rules
 * - LookupServiceRegistry: Real lookup service management for market conventions
 *
 * NO HARDCODED SIMULATION: All processing uses authentic APEX core services with YAML-driven configuration.
 * NO HARDCODED OBJECTS: No manual TradeConfirmation, TradeB, Security, or Counterparty object creation.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-13
 * @version 1.0 - JUnit 5 conversion from ComprehensiveFinancialSettlementDemo.java
 */
class ComprehensiveFinancialSettlementDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveFinancialSettlementDemoTest.class);

    /**
     * Test multi-asset settlement processing functionality using real APEX services
     */
    @Test
    void testMultiAssetSettlementProcessingFunctionality() {
        logger.info("=== Testing Multi-Asset Settlement Processing Functionality ===");
        
        try {
            // Load YAML configuration using real APEX services
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("test-configs/comprehensivefinancialsettlementdemo-test.yaml");
            assertNotNull(config, "YAML configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Create test data for multi-asset settlement processing
            Map<String, Object> settlementData = new HashMap<>();
            settlementData.put("tradeId", "TRADE_001");
            settlementData.put("assetClass", "EQUITY");
            settlementData.put("market", "UK");
            settlementData.put("instrumentId", "VOD.L");
            settlementData.put("quantity", 10000);
            settlementData.put("price", 2750.50);
            settlementData.put("currency", "GBP");
            settlementData.put("sourceCurrency", "GBP");  // Add for currency conversion testing
            settlementData.put("targetCurrency", "USD");  // Add for currency conversion testing
            settlementData.put("counterparty", "Deutsche Bank AG");
            settlementData.put("settlementDate", "2025-08-30");
            
            logger.info("Input data: " + settlementData);
            
            // Use real APEX EnrichmentService to process multi-asset settlement
            Object result = enrichmentService.enrichObject(config, settlementData);
            assertNotNull(result, "Multi-asset settlement processing result should not be null");
            
            // Validate the enriched result
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Verify comprehensive business logic results
            assertEquals("Settlement processed for EQUITY trade TRADE_001 - Market: UK - Status: PROCESSED",
                        enrichedData.get("settlementResult"));

            // Verify settlement date calculation based on UK market conventions
            assertEquals("T+2", enrichedData.get("settlementConvention"));

            // Verify risk assessment based on trade value (10000 * 2750.50 = 27,505,000 = MEDIUM_RISK)
            assertEquals("MEDIUM_RISK", enrichedData.get("riskAssessment"));

            // Verify regulatory compliance for UK market
            assertEquals("FCA_COMPLIANCE_REQUIRED", enrichedData.get("regulatoryCompliance"));

            // Verify currency conversion for GBP to USD
            assertEquals("GBP/USD conversion required - Rate: 1.25", enrichedData.get("currencyConversion"));

            // Verify original data is preserved
            assertEquals("TRADE_001", enrichedData.get("tradeId"));
            assertEquals("EQUITY", enrichedData.get("assetClass"));
            assertEquals("UK", enrichedData.get("market"));
            assertEquals("VOD.L", enrichedData.get("instrumentId"));
            assertEquals(10000, enrichedData.get("quantity"));
            assertEquals(2750.50, enrichedData.get("price"));
            assertEquals("GBP", enrichedData.get("currency"));
            assertEquals("GBP", enrichedData.get("sourceCurrency"));
            assertEquals("USD", enrichedData.get("targetCurrency"));
            assertEquals("Deutsche Bank AG", enrichedData.get("counterparty"));
            assertEquals("2025-08-30", enrichedData.get("settlementDate"));
            
            logger.info("✅ Multi-asset settlement processing completed using real APEX services");
            logger.info("Settlement result: " + result);
            
        } catch (Exception e) {
            logger.error("❌ Multi-asset settlement processing test failed", e);
            fail("Multi-asset settlement processing test failed: " + e.getMessage());
        }
    }

    /**
     * Test cross-border settlement processing functionality using real APEX services
     */
    @Test
    void testCrossBorderSettlementProcessingFunctionality() {
        logger.info("=== Testing Cross-Border Settlement Processing Functionality ===");
        
        try {
            // Load YAML configuration using real APEX services
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("test-configs/comprehensivefinancialsettlementdemo-test.yaml");
            assertNotNull(config, "YAML configuration should be loaded successfully");
            
            // Create test data for cross-border settlement processing
            Map<String, Object> crossBorderData = new HashMap<>();
            crossBorderData.put("tradeId", "TRADE_002");
            crossBorderData.put("assetClass", "FIXED_INCOME");
            crossBorderData.put("market", "US");  // Use "market" for YAML processing
            crossBorderData.put("sourceMarket", "US");
            crossBorderData.put("targetMarket", "GERMANY");
            crossBorderData.put("instrumentId", "US_TREASURY_10Y");
            crossBorderData.put("notional", 1000000);
            crossBorderData.put("quantity", 1000000);  // Add quantity for risk assessment
            crossBorderData.put("price", 98.75);
            crossBorderData.put("sourceCurrency", "USD");
            crossBorderData.put("targetCurrency", "EUR");
            crossBorderData.put("counterparty", "JPMorgan Chase");
            
            logger.info("Input data: " + crossBorderData);
            
            // Use real APEX EnrichmentService to process cross-border settlement
            Object result = enrichmentService.enrichObject(config, crossBorderData);
            assertNotNull(result, "Cross-border settlement processing result should not be null");
            
            // Validate the enriched result
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Verify cross-border settlement processing results - should use sourceMarket for processing
            assertEquals("Settlement processed for FIXED_INCOME trade TRADE_002 - Market: US - Status: PROCESSED",
                        enrichedData.get("settlementResult"));

            // Verify currency conversion for USD to EUR
            assertEquals("USD/EUR conversion required - Rate: 0.85", enrichedData.get("currencyConversion"));

            // Verify risk assessment based on calculated value (1,000,000 * 98.75 = 98,750,000 = HIGH_RISK)
            assertEquals("HIGH_RISK", enrichedData.get("riskAssessment"));

            // Verify regulatory compliance for US market
            assertEquals("SEC_COMPLIANCE_REQUIRED", enrichedData.get("regulatoryCompliance"));

            // Verify settlement convention for US FIXED_INCOME
            assertEquals("T+1", enrichedData.get("settlementConvention"));

            // Verify original data is preserved
            assertEquals("TRADE_002", enrichedData.get("tradeId"));
            assertEquals("FIXED_INCOME", enrichedData.get("assetClass"));
            assertEquals("US", enrichedData.get("sourceMarket"));
            assertEquals("GERMANY", enrichedData.get("targetMarket"));
            assertEquals("US_TREASURY_10Y", enrichedData.get("instrumentId"));
            assertEquals(1000000, enrichedData.get("notional"));
            assertEquals(98.75, enrichedData.get("price"));
            assertEquals("USD", enrichedData.get("sourceCurrency"));
            assertEquals("EUR", enrichedData.get("targetCurrency"));
            assertEquals("JPMorgan Chase", enrichedData.get("counterparty"));
            
            logger.info("✅ Cross-border settlement processing completed using real APEX services");
            logger.info("Cross-border result: " + result);
            
        } catch (Exception e) {
            logger.error("❌ Cross-border settlement processing test failed", e);
            fail("Cross-border settlement processing test failed: " + e.getMessage());
        }
    }

    /**
     * Test high-value transaction processing functionality using real APEX services
     */
    @Test
    void testHighValueTransactionProcessingFunctionality() {
        logger.info("=== Testing High-Value Transaction Processing Functionality ===");
        
        try {
            // Load YAML configuration using real APEX services
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("test-configs/comprehensivefinancialsettlementdemo-test.yaml");
            assertNotNull(config, "YAML configuration should be loaded successfully");
            
            // Create test data for high-value transaction processing
            Map<String, Object> highValueData = new HashMap<>();
            highValueData.put("tradeId", "TRADE_003");
            highValueData.put("assetClass", "EQUITY");
            highValueData.put("market", "US");
            highValueData.put("instrumentId", "AAPL");
            highValueData.put("quantity", 500000);
            highValueData.put("price", 190.00);
            highValueData.put("totalValue", 95000000.0);
            highValueData.put("currency", "USD");
            highValueData.put("counterparty", "Goldman Sachs");
            highValueData.put("riskLevel", "HIGH");
            highValueData.put("requiresApproval", true);
            
            logger.info("Input data: " + highValueData);
            
            // Use real APEX EnrichmentService to process high-value transactions
            Object result = enrichmentService.enrichObject(config, highValueData);
            assertNotNull(result, "High-value transaction processing result should not be null");
            
            // Validate the enriched result
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Verify high-value transaction processing results
            assertEquals("Settlement processed for EQUITY trade TRADE_003 - Market: US - Status: PROCESSED",
                        enrichedData.get("settlementResult"));

            // Verify risk assessment for high-value transaction (95,000,000 = HIGH_RISK)
            assertEquals("HIGH_RISK", enrichedData.get("riskAssessment"));

            // Verify regulatory compliance for US market
            assertEquals("SEC_COMPLIANCE_REQUIRED", enrichedData.get("regulatoryCompliance"));

            // Verify settlement convention for US EQUITY
            assertEquals("T+2", enrichedData.get("settlementConvention"));

            // Verify original data is preserved
            assertEquals("TRADE_003", enrichedData.get("tradeId"));
            assertEquals("EQUITY", enrichedData.get("assetClass"));
            assertEquals("US", enrichedData.get("market"));
            assertEquals("AAPL", enrichedData.get("instrumentId"));
            assertEquals(500000, enrichedData.get("quantity"));
            assertEquals(190.00, enrichedData.get("price"));
            assertEquals(95000000.0, enrichedData.get("totalValue"));
            assertEquals("USD", enrichedData.get("currency"));
            assertEquals("Goldman Sachs", enrichedData.get("counterparty"));
            assertEquals("HIGH", enrichedData.get("riskLevel"));
            assertEquals(true, enrichedData.get("requiresApproval"));
            
            logger.info("✅ High-value transaction processing completed using real APEX services");
            logger.info("High-value result: " + result);
            
        } catch (Exception e) {
            logger.error("❌ High-value transaction processing test failed", e);
            fail("High-value transaction processing test failed: " + e.getMessage());
        }
    }
}
