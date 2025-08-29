package dev.mars.apex.demo.advanced;

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



import dev.mars.apex.demo.evaluation.DynamicMethodExecutionDemo;
import dev.mars.apex.demo.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DynamicMethodExecutionDemo to verify the merged functionality.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
/**
 * Test class for DynamicMethodExecutionDemo to verify the merged functionality.
 */
public class DynamicMethodExecutionDemoConfigTest {

    private DynamicMethodExecutionDemo demo;
    private StandardEvaluationContext context;

    @BeforeEach
    void setUp() {
        // Create demo with merged functionality
        demo = new DynamicMethodExecutionDemo();

        // Create context
        context = demo.createContext();
    }

    @Test
    void testGetAvailableRules() {
        Map<String, String> rules = demo.getAvailableRules();
        
        assertNotNull(rules);
        assertTrue(rules.containsKey("SettlementDays"));
        assertTrue(rules.containsKey("SettlementMethod"));
        assertTrue(rules.containsKey("MarketRisk"));
        assertTrue(rules.containsKey("StandardPrice"));
        assertTrue(rules.containsKey("CreditRisk"));
        assertTrue(rules.containsKey("ComplianceCheck"));
        assertTrue(rules.containsKey("PremiumPrice"));
        assertTrue(rules.containsKey("SalePrice"));
        assertTrue(rules.containsKey("ClearancePrice"));
        
        assertEquals("Calculate settlement days for trade", rules.get("SettlementDays"));
        assertEquals("Calculate standard price", rules.get("StandardPrice"));
    }

    @Test
    void testDemonstratePricingVariations() {
        double basePrice = 100.0;
        Map<String, Double> pricingResults = demo.demonstratePricingVariations(basePrice, context);
        
        assertNotNull(pricingResults);
        assertEquals(4, pricingResults.size());
        
        // Test standard price
        assertEquals(100.0, pricingResults.get("standard"), 0.01);
        
        // Test premium price (+20%)
        assertEquals(120.0, pricingResults.get("premium"), 0.01);
        
        // Test sale price (-20%)
        assertEquals(80.0, pricingResults.get("sale"), 0.01);
        
        // Test clearance price (-50%)
        assertEquals(50.0, pricingResults.get("clearance"), 0.01);
    }

    @Test
    void testValidateValidTrade() {
        Trade validTrade = new Trade("T001", "Equity", "InstrumentType");
        Map<String, Object> validationResults = demo.validateTrade(validTrade, context);
        
        assertNotNull(validationResults);
        assertEquals(true, validationResults.get("hasId"));
        assertEquals(true, validationResults.get("hasValue"));
        assertEquals(true, validationResults.get("hasCategory"));
        assertEquals(2, validationResults.get("settlementDays")); // Equity has 2 settlement days
        assertEquals(true, validationResults.get("validSettlementDays"));
    }

    @Test
    void testValidateInvalidTrade() {
        Trade invalidTrade = new Trade();
        invalidTrade.setValue("Equity");
        invalidTrade.setCategory("InstrumentType");
        // Default constructor sets ID to "Unknown", so this trade actually has an ID

        Map<String, Object> validationResults = demo.validateTrade(invalidTrade, context);

        assertNotNull(validationResults);
        assertEquals(true, validationResults.get("hasId")); // Default constructor sets ID to "Unknown"
        assertEquals(true, validationResults.get("hasValue"));
        assertEquals(true, validationResults.get("hasCategory"));
    }

    @Test
    void testValidateTrulyInvalidTrade() {
        System.out.println("TEST: Triggering intentional error - testing validation of truly invalid trade with null/empty fields");

        Trade invalidTrade = new Trade();
        invalidTrade.setId(null); // Explicitly set ID to null
        invalidTrade.setValue(""); // Empty value
        invalidTrade.setCategory(null); // Null category

        Map<String, Object> validationResults = demo.validateTrade(invalidTrade, context);

        assertNotNull(validationResults);
        assertEquals(false, validationResults.get("hasId"));
        assertEquals(false, validationResults.get("hasValue"));
        assertEquals(false, validationResults.get("hasCategory"));
        assertEquals(false, validationResults.get("validSettlementDays"));
    }

    @Test
    void testValidateNullTrade() {
        System.out.println("TEST: Triggering intentional error - testing validation of null trade object");

        Map<String, Object> validationResults = demo.validateTrade(null, context);

        assertNotNull(validationResults);
        assertEquals(false, validationResults.get("hasId"));
        assertEquals(false, validationResults.get("hasValue"));
        assertEquals(false, validationResults.get("hasCategory"));
        assertEquals(false, validationResults.get("validSettlementDays"));
    }

    @Test
    void testExecuteRule() {
        // Set up context with a trade
        @SuppressWarnings("unchecked")
        Map<String, Trade> trades = (Map<String, Trade>) context.lookupVariable("trades");
        Trade equityTrade = trades.get("equity");
        context.setVariable("trade", equityTrade);
        
        // Test settlement days rule
        Integer settlementDays = demo.executeRule("SettlementDays", context, Integer.class);
        assertEquals(2, settlementDays); // Equity should have 2 settlement days
        
        // Test settlement method rule
        String settlementMethod = demo.executeRule("SettlementMethod", context, String.class);
        assertEquals("DTC", settlementMethod); // Equity should use DTC
        
        // Test market risk rule
        Double marketRisk = demo.executeRule("MarketRisk", context, Double.class);
        assertEquals(0.15, marketRisk, 0.01); // Equity market risk factor
    }

    @Test
    void testExecuteAllRules() {
        // Set up context
        @SuppressWarnings("unchecked")
        Map<String, Trade> trades = (Map<String, Trade>) context.lookupVariable("trades");
        Trade derivativeTrade = trades.get("derivative");
        context.setVariable("trade", derivativeTrade);
        context.setVariable("basePrice", 500.0);
        
        Map<String, Object> results = demo.executeAllRules(context);
        
        assertNotNull(results);
        assertTrue(results.size() >= 4); // At least the original 4 rules
        
        // Check some expected results
        assertEquals(1, results.get("SettlementDays")); // Derivative has 1 settlement day
        assertEquals("Clearstream", results.get("SettlementMethod")); // Derivative uses Clearstream
        assertEquals(0.25, (Double) results.get("MarketRisk"), 0.01); // Derivative market risk
        assertEquals(500.0, (Double) results.get("StandardPrice"), 0.01); // Standard price
    }
}
