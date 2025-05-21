package com.rulesengine.demo.service.validators;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.config.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.demo.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for TradeValidator using DynamicTradeValidatorDemoConfig.
 */
public class DynamicTradeValidatorDemoTest {
    private TradeValidator equityValidator;
    private DynamicTradeValidatorDemoConfig config;
    private Trade validEquityTrade;
    private Trade invalidEquityTrade;
    private Trade etfTrade;

    @BeforeEach
    public void setUp() {
        // Create a RulesEngine
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Create a DynamicTradeValidatorDemoConfig
        config = new DynamicTradeValidatorDemoConfig(rulesEngine);

        // Create parameters map for equity trades
        Map<String, Object> equityParams = new HashMap<>();
        equityParams.put("allowedValues", Arrays.asList("Equity"));
        equityParams.put("allowedCategories", Arrays.asList("InstrumentType"));

        // Create a validator for equity trades
        equityValidator = new TradeValidator(
            "equityValidator", 
            equityParams,
            config
        );

        // Create sample trades
        validEquityTrade = new Trade("T001", "Equity", "InstrumentType");
        invalidEquityTrade = new Trade("T002", "Bond", "InstrumentType");
        etfTrade = new Trade("T003", "ETF", "InstrumentType");
    }

    @Test
    public void testValidateWithValidTrade() {
        // Test standard validation with valid trade
        boolean result = equityValidator.validate(validEquityTrade);
        assertTrue(result, "Valid equity trade should be valid");

        // Test detailed result
        RuleResult detailedResult = equityValidator.validateWithResult(validEquityTrade);
        assertTrue(detailedResult.isTriggered(), "Valid equity trade should trigger rule");
        assertEquals("equityValidator", detailedResult.getRuleName(), "Rule name should match");
    }

    @Test
    public void testValidateWithInvalidTrade() {
        // Test standard validation with invalid trade
        boolean result = equityValidator.validate(invalidEquityTrade);
        assertFalse(result, "Invalid equity trade should be invalid");

        // Test detailed result
        RuleResult detailedResult = equityValidator.validateWithResult(invalidEquityTrade);
        assertFalse(detailedResult.isTriggered(), "Invalid equity trade should not trigger rule");
    }

    @Test
    public void testValidateWithExpression() {
        // Test dynamic expression validation
        String customExpression = "#trade != null && #trade.value == 'Equity' && #trade.id.startsWith('T')";

        // Valid trade with custom expression
        boolean validResult = equityValidator.validateWithExpression(validEquityTrade, customExpression, config);
        assertTrue(validResult, "Valid equity trade should be valid with custom expression");

        // Invalid trade with custom expression
        boolean invalidResult = equityValidator.validateWithExpression(invalidEquityTrade, customExpression, config);
        assertFalse(invalidResult, "Invalid equity trade should be invalid with custom expression");
    }

    @Test
    public void testComplexExpression() {
        // Test complex expression that allows both Equity and ETF
        String complexExpression = "#trade != null && (#trade.value == 'Equity' || #trade.value == 'ETF') && #trade.category == 'InstrumentType'";

        // ETF trade with standard validation (should fail)
        boolean etfStandardResult = equityValidator.validate(etfTrade);
        assertFalse(etfStandardResult, "ETF trade should be invalid with standard validation");

        // ETF trade with complex expression (should pass)
        boolean etfComplexResult = equityValidator.validateWithExpression(etfTrade, complexExpression, config);
        assertTrue(etfComplexResult, "ETF trade should be valid with complex expression");

        // Equity trade with complex expression (should pass)
        boolean equityComplexResult = equityValidator.validateWithExpression(validEquityTrade, complexExpression, config);
        assertTrue(equityComplexResult, "Equity trade should be valid with complex expression");
    }

    @Test
    public void testNullTrade() {
        // Test with null trade
        boolean result = equityValidator.validate(null);
        assertFalse(result, "Null trade should be invalid");

        // Test with null trade and custom expression
        String customExpression = "#trade != null";
        boolean expressionResult = equityValidator.validateWithExpression(null, customExpression, config);
        assertFalse(expressionResult, "Null trade should be invalid with custom expression");
    }
}
