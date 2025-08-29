package dev.mars.apex.demo;


import dev.mars.apex.demo.examples.FinancialServicesDemo;
import dev.mars.apex.demo.infrastructure.StaticDataEntities.*;
import dev.mars.apex.demo.infrastructure.FinancialStaticDataProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

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
 * Test to verify that the restored functionality works correctly after
 * the icon removal process corrupted some files.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class RestorationVerificationTest {
    
    @Test
    public void testCommodityTotalReturnSwapModel() {
        // Test that the model can be created and used
        CommodityTotalReturnSwap swap = new CommodityTotalReturnSwap();
        assertNotNull(swap);
        
        // Test basic properties
        swap.setTradeId("TEST-001");
        swap.setClientId("CLI001");
        swap.setCounterpartyId("CP001");
        swap.setNotionalAmount(new BigDecimal("10000000"));
        swap.setNotionalCurrency("USD");
        swap.setReferenceIndex("WTI");
        swap.setTradeDate(LocalDate.now());
        swap.setMaturityDate(LocalDate.now().plusMonths(12));
        
        assertEquals("TEST-001", swap.getTradeId());
        assertEquals("CLI001", swap.getClientId());
        assertEquals("CP001", swap.getCounterpartyId());
        assertEquals(new BigDecimal("10000000"), swap.getNotionalAmount());
        assertEquals("USD", swap.getNotionalCurrency());
        assertEquals("WTI", swap.getReferenceIndex());
        assertNotNull(swap.getTradeDate());
        assertNotNull(swap.getMaturityDate());
    }
    
    @Test
    public void testStaticDataEntities() {
        // Test CurrencyData
        CurrencyData currency = new CurrencyData("USD", "US Dollar", 2, true, true);
        currency.setRegion("AMERICAS");
        assertEquals("USD", currency.getCurrencyCode());
        assertEquals("US Dollar", currency.getCurrencyName());
        assertEquals(Integer.valueOf(2), currency.getDecimalPlaces());
        assertTrue(currency.getActive());
        assertTrue(currency.getTradeable());
        assertEquals("AMERICAS", currency.getRegion());
        
        // Test Counterparty
        Counterparty counterparty = new Counterparty("CP001", "Global Investment Bank", "INVESTMENT_BANK", true);
        assertEquals("CP001", counterparty.getCounterpartyId());
        assertEquals("Global Investment Bank", counterparty.getCounterpartyName());
        assertEquals("INVESTMENT_BANK", counterparty.getCounterpartyType());
        assertTrue(counterparty.getActive());
        
        // Test Client
        Client client = new Client("CLI001", "Pension Fund Alpha", "INSTITUTIONAL", true);
        assertEquals("CLI001", client.getClientId());
        assertEquals("Pension Fund Alpha", client.getClientName());
        assertEquals("INSTITUTIONAL", client.getClientType());
        assertTrue(client.getActive());
        
        // Test CommodityReference
        CommodityReference commodity = new CommodityReference("WTI", "West Texas Intermediate", "CRUDE_OIL", "WTI", "USD", true);
        commodity.setIndexProvider("NYMEX");
        assertEquals("WTI", commodity.getCommodityCode());
        assertEquals("West Texas Intermediate", commodity.getCommodityName());
        assertEquals("CRUDE_OIL", commodity.getCommodityType());
        assertEquals("NYMEX", commodity.getIndexProvider());
        assertEquals("USD", commodity.getQuoteCurrency());
        assertTrue(commodity.getActive());
    }
    
    @Test
    public void testFinancialStaticDataProvider() {
        // Test currency lookup
        CurrencyData usd = FinancialStaticDataProvider.getCurrency("USD");
        assertNotNull(usd);
        assertEquals("US Dollar", usd.getCurrencyName());

        CurrencyData invalid = FinancialStaticDataProvider.getCurrency("INVALID");
        assertNull(invalid);

        // Test currency validation
        assertTrue(FinancialStaticDataProvider.isValidCurrency("USD"));
        assertTrue(FinancialStaticDataProvider.isValidCurrency("EUR"));
        assertFalse(FinancialStaticDataProvider.isValidCurrency("INVALID"));
        
        // Test counterparty lookup
        Counterparty cp = FinancialStaticDataProvider.getCounterparty("CP001");
        assertNotNull(cp);
        assertEquals("Global Investment Bank", cp.getCounterpartyName());

        // Test counterparty validation
        assertTrue(FinancialStaticDataProvider.isValidCounterparty("CP001"));
        assertFalse(FinancialStaticDataProvider.isValidCounterparty("INVALID"));

        // Test client lookup
        Client client = FinancialStaticDataProvider.getClient("CLI001");
        assertNotNull(client);
        assertEquals("Pension Fund Alpha", client.getClientName());

        // Test client validation
        assertTrue(FinancialStaticDataProvider.isValidClient("CLI001"));
        assertFalse(FinancialStaticDataProvider.isValidClient("INVALID"));
        
        // Test commodity reference lookup
        CommodityReference commodity = FinancialStaticDataProvider.getCommodity("WTI");
        assertNotNull(commodity);
        assertEquals("West Texas Intermediate Crude Oil", commodity.getCommodityName());

        // Test commodity reference validation
        assertTrue(FinancialStaticDataProvider.isValidCommodity("WTI"));
        assertFalse(FinancialStaticDataProvider.isValidCommodity("INVALID"));
    }
    
    @Test
    public void testFinancialServicesDemo() {
        // Test that the demo can be instantiated and basic methods work
        FinancialServicesDemo demo = new FinancialServicesDemo();
        assertNotNull(demo);
        
        // Test that the demo doesn't throw exceptions during construction
        // This verifies that the RulesService is properly initialized
    }
    

    
    @Test
    public void testIntegrationScenario() {
        // Test a complete advanced scenario
        CommodityTotalReturnSwap swap = new CommodityTotalReturnSwap();
        swap.setTradeId("INTEGRATION-TEST-001");
        swap.setClientId("CLI001");
        swap.setCounterpartyId("CP001");
        swap.setNotionalAmount(new BigDecimal("5000000"));
        swap.setNotionalCurrency("USD");
        swap.setReferenceIndex("WTI");
        swap.setTradeDate(LocalDate.now());
        swap.setMaturityDate(LocalDate.now().plusMonths(6));
        
        // Verify static data lookups work
        assertTrue(FinancialStaticDataProvider.isValidClient(swap.getClientId()));
        assertTrue(FinancialStaticDataProvider.isValidCounterparty(swap.getCounterpartyId()));
        assertTrue(FinancialStaticDataProvider.isValidCurrency(swap.getNotionalCurrency()));
        assertTrue(FinancialStaticDataProvider.isValidCommodity(swap.getReferenceIndex()));

        // Verify enrichment works
        Client client = FinancialStaticDataProvider.getClient(swap.getClientId());
        assertNotNull(client);
        swap.setClientName(client.getClientName());
        assertEquals("Pension Fund Alpha", swap.getClientName());

        Counterparty counterparty = FinancialStaticDataProvider.getCounterparty(swap.getCounterpartyId());
        assertNotNull(counterparty);
        swap.setCounterpartyName(counterparty.getCounterpartyName());
        assertEquals("Global Investment Bank", swap.getCounterpartyName());

        CommodityReference commodity = FinancialStaticDataProvider.getCommodity(swap.getReferenceIndex());
        assertNotNull(commodity);
        swap.setIndexProvider(commodity.getIndexProvider());
        assertEquals("NYMEX", swap.getIndexProvider());
    }
}
