package dev.mars.rulesengine.demo.examples.financial;

import dev.mars.rulesengine.core.api.RulesService;
import dev.mars.rulesengine.demo.examples.financial.model.CommodityTotalReturnSwap;
import dev.mars.rulesengine.demo.examples.financial.model.StaticDataEntities.*;
import dev.mars.rulesengine.demo.datasets.FinancialStaticDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Test class for commodity swap validation functionality.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for commodity swap validation functionality.
 * Verifies that the new layered APIs work correctly with financial instruments.
 */
public class CommoditySwapValidationTest {
    
    private RulesService rulesService;
    private CommodityTotalReturnSwap validSwap;
    
    @BeforeEach
    void setUp() {
        rulesService = new RulesService();
        validSwap = createValidCommoditySwap();
    }
    
    @Test
    void testBasicFieldValidation() {
        // Test trade ID validation
        boolean hasTradeId = rulesService.check("#tradeId != null && #tradeId.length() > 0", 
                                              Map.of("tradeId", validSwap.getTradeId()));
        assertTrue(hasTradeId, "Valid trade ID should pass validation");
        
        // Test notional amount validation
        boolean validNotional = rulesService.check("#notionalAmount != null && #notionalAmount > 0", 
                                                  Map.of("notionalAmount", validSwap.getNotionalAmount()));
        assertTrue(validNotional, "Valid notional amount should pass validation");
        
        // Test date validation
        boolean validDates = rulesService.check("#tradeDate != null && #maturityDate != null && #maturityDate.isAfter(#tradeDate)", 
                                               Map.of("tradeDate", validSwap.getTradeDate(), 
                                                     "maturityDate", validSwap.getMaturityDate()));
        assertTrue(validDates, "Valid dates should pass validation");
    }
    
    @Test
    void testBusinessLogicValidation() {
        // Test minimum notional check
        boolean meetsMinimum = rulesService.check("#notionalAmount >= 1000000", 
                                                 Map.of("notionalAmount", validSwap.getNotionalAmount()));
        assertTrue(meetsMinimum, "Swap should meet minimum notional requirement");
        
        // Test maturity validation (max 5 years)
        boolean validMaturity = rulesService.check("#maturityDate.isBefore(#tradeDate.plusYears(5))", 
                                                  Map.of("tradeDate", validSwap.getTradeDate(),
                                                        "maturityDate", validSwap.getMaturityDate()));
        assertTrue(validMaturity, "Swap maturity should be within 5 years");
        
        // Test currency consistency
        boolean sameCurrency = rulesService.check("#notionalCurrency == #paymentCurrency", 
                                                 Map.of("notionalCurrency", validSwap.getNotionalCurrency(),
                                                       "paymentCurrency", validSwap.getPaymentCurrency()));
        assertTrue(sameCurrency, "Currencies should be consistent");
    }
    
    @Test
    void testStaticDataValidation() {
        // Test client validation
        Client client = FinancialStaticDataProvider.getClient(validSwap.getClientId());
        assertNotNull(client, "Client should exist in static data");
        assertTrue(client.getActive(), "Client should be active");
        assertEquals("INSTITUTIONAL", client.getClientType(), "Client should be institutional");
        
        // Test counterparty validation
        Counterparty counterparty = FinancialStaticDataProvider.getCounterparty(validSwap.getCounterpartyId());
        assertNotNull(counterparty, "Counterparty should exist in static data");
        assertTrue(counterparty.getActive(), "Counterparty should be active");
        
        // Test currency validation
        CurrencyData currency = FinancialStaticDataProvider.getCurrency(validSwap.getNotionalCurrency());
        assertNotNull(currency, "Currency should exist in static data");
        assertTrue(currency.getActive(), "Currency should be active");
        assertTrue(currency.getTradeable(), "Currency should be tradeable");
        
        // Test commodity validation
        CommodityReference commodity = FinancialStaticDataProvider.getCommodity(validSwap.getReferenceIndex());
        assertNotNull(commodity, "Commodity should exist in static data");
        assertTrue(commodity.getActive(), "Commodity should be active");
        assertEquals("ENERGY", commodity.getCommodityType(), "Commodity should be energy type");
    }
    
    @Test
    void testInvalidSwapValidation() {
        // Create invalid swap with null trade ID
        CommodityTotalReturnSwap invalidSwap = createValidCommoditySwap();
        invalidSwap.setTradeId(null);
        
        Map<String, Object> contextWithNullTradeId = new HashMap<>();
        contextWithNullTradeId.put("tradeId", invalidSwap.getTradeId());
        boolean hasTradeId = rulesService.check("#tradeId != null && #tradeId.length() > 0",
                                              contextWithNullTradeId);
        assertFalse(hasTradeId, "Null trade ID should fail validation");
        
        // Test with invalid notional amount
        invalidSwap = createValidCommoditySwap();
        invalidSwap.setNotionalAmount(new BigDecimal("500000")); // Below minimum
        
        boolean meetsMinimum = rulesService.check("#notionalAmount >= 1000000", 
                                                 Map.of("notionalAmount", invalidSwap.getNotionalAmount()));
        assertFalse(meetsMinimum, "Below minimum notional should fail validation");
        
        // Test with invalid dates
        invalidSwap = createValidCommoditySwap();
        invalidSwap.setMaturityDate(LocalDate.now().minusDays(1)); // Past date
        
        boolean validDates = rulesService.check("#tradeDate != null && #maturityDate != null && #maturityDate.isAfter(#tradeDate)", 
                                               Map.of("tradeDate", invalidSwap.getTradeDate(), 
                                                     "maturityDate", invalidSwap.getMaturityDate()));
        assertFalse(validDates, "Past maturity date should fail validation");
    }
    
    @Test
    void testStaticDataProviderUtilityMethods() {
        // Test utility methods
        assertTrue(FinancialStaticDataProvider.isValidClient("CLI001"), "CLI001 should be valid client");
        assertFalse(FinancialStaticDataProvider.isValidClient("CLI004"), "CLI004 should be invalid (inactive)");
        assertFalse(FinancialStaticDataProvider.isValidClient("NONEXISTENT"), "Non-existent client should be invalid");
        
        assertTrue(FinancialStaticDataProvider.isValidCounterparty("CP001"), "CP001 should be valid counterparty");
        assertFalse(FinancialStaticDataProvider.isValidCounterparty("CP003"), "CP003 should be invalid (inactive)");
        
        assertTrue(FinancialStaticDataProvider.isValidCurrency("USD"), "USD should be valid currency");
        assertFalse(FinancialStaticDataProvider.isValidCurrency("XYZ"), "XYZ should be invalid currency");
        
        assertTrue(FinancialStaticDataProvider.isValidCommodity("WTI"), "WTI should be valid commodity");
        assertFalse(FinancialStaticDataProvider.isValidCommodity("INACTIVE"), "INACTIVE should be invalid commodity");
    }
    
    @Test
    void testComplexBusinessRules() {
        Map<String, Object> context = convertSwapToMap(validSwap);
        
        // Test complex energy commodity rule
        boolean energyRule = rulesService.check(
            "#commodityType == 'ENERGY' && (#referenceIndex == 'WTI' || #referenceIndex == 'BRENT' || #referenceIndex == 'HENRY_HUB')",
            context
        );
        assertTrue(energyRule, "Energy commodity with valid index should pass");
        
        // Test notional range rule
        boolean notionalRange = rulesService.check(
            "#notionalAmount >= 1000000 && #notionalAmount <= 100000000",
            context
        );
        assertTrue(notionalRange, "Notional amount should be within valid range");
        
        // Test settlement days rule
        context.put("settlementDays", validSwap.getSettlementDays());
        boolean settlementDays = rulesService.check(
            "#settlementDays != null && #settlementDays >= 0 && #settlementDays <= 10",
            context
        );
        assertTrue(settlementDays, "Settlement days should be within valid range");
    }
    
    @Test
    void testRulePerformance() {
        // Test that rules execute quickly
        Map<String, Object> context = convertSwapToMap(validSwap);
        
        long startTime = System.nanoTime();
        
        // Execute multiple rules
        for (int i = 0; i < 1000; i++) {
            rulesService.check("#notionalAmount > 1000000", context);
            rulesService.check("#commodityType == 'ENERGY'", context);
            rulesService.check("#tradeDate != null", context);
        }
        
        long executionTime = System.nanoTime() - startTime;
        double avgTimeMs = (executionTime / 1_000_000.0) / 3000; // 3000 total rule executions
        
        // Assert that average execution time is reasonable (less than 1ms per rule)
        assertTrue(avgTimeMs < 1.0, "Average rule execution time should be less than 1ms, was: " + avgTimeMs + "ms");
    }
    
    // Helper methods
    
    private CommodityTotalReturnSwap createValidCommoditySwap() {
        CommodityTotalReturnSwap swap = new CommodityTotalReturnSwap(
            "TRS001",           // tradeId
            "CP001",            // counterpartyId
            "CLI001",           // clientId
            "ENERGY",           // commodityType
            "WTI",              // referenceIndex
            new BigDecimal("10000000"), // notionalAmount
            "USD",              // notionalCurrency
            LocalDate.now(),    // tradeDate
            LocalDate.now().plusYears(1) // maturityDate
        );
        
        // Set additional fields
        swap.setClientAccountId("ACC001");
        swap.setPaymentCurrency("USD");
        swap.setSettlementCurrency("USD");
        swap.setSettlementDays(2);
        swap.setTotalReturnPayerParty("COUNTERPARTY");
        swap.setTotalReturnReceiverParty("CLIENT");
        swap.setFundingRateType("LIBOR");
        swap.setFundingSpread(new BigDecimal("150"));
        swap.setFundingFrequency("QUARTERLY");
        swap.setJurisdiction("US");
        swap.setRegulatoryRegime("DODD_FRANK");
        swap.setClearingEligible(true);
        swap.setInitialPrice(new BigDecimal("75.50"));
        
        return swap;
    }
    
    private Map<String, Object> convertSwapToMap(CommodityTotalReturnSwap swap) {
        Map<String, Object> map = new HashMap<>();
        map.put("tradeId", swap.getTradeId());
        map.put("counterpartyId", swap.getCounterpartyId());
        map.put("clientId", swap.getClientId());
        map.put("commodityType", swap.getCommodityType());
        map.put("referenceIndex", swap.getReferenceIndex());
        map.put("notionalAmount", swap.getNotionalAmount());
        map.put("notionalCurrency", swap.getNotionalCurrency());
        map.put("paymentCurrency", swap.getPaymentCurrency());
        map.put("settlementCurrency", swap.getSettlementCurrency());
        map.put("settlementDays", swap.getSettlementDays());
        map.put("tradeDate", swap.getTradeDate());
        map.put("maturityDate", swap.getMaturityDate());
        return map;
    }
}
