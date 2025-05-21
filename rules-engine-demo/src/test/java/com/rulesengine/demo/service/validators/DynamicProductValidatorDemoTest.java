package com.rulesengine.demo.service.validators;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.config.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.demo.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ProductValidator using DynamicProductValidatorDemoConfig.
 */
public class DynamicProductValidatorDemoTest {
    private ProductValidator fixedIncomeValidator;
    private DynamicProductValidatorDemoConfig config;
    private Product validFixedIncomeProduct;
    private Product invalidPriceProduct;
    private Product invalidCategoryProduct;
    private Product etfProduct;

    @BeforeEach
    public void setUp() {
        // Create a RulesEngine
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Create a DynamicProductValidatorDemoConfig
        config = new DynamicProductValidatorDemoConfig(rulesEngine);

        // Create parameters map for fixed income products
        Map<String, Object> fixedIncomeParams = new HashMap<>();
        fixedIncomeParams.put("minPrice", 100.0);
        fixedIncomeParams.put("maxPrice", 2000.0);
        fixedIncomeParams.put("requiredCategory", "FixedIncome");

        // Create a validator for fixed income products
        fixedIncomeValidator = new ProductValidator(
            "fixedIncomeValidator", 
            fixedIncomeParams,
            config
        );

        // Create sample products
        validFixedIncomeProduct = new Product("US Treasury Bond", 1200.0, "FixedIncome");
        invalidPriceProduct = new Product("Expensive Bond", 2500.0, "FixedIncome");
        invalidCategoryProduct = new Product("Apple Stock", 180.0, "Equity");
        etfProduct = new Product("Gold ETF", 1500.0, "ETF");
    }

    @Test
    public void testValidateWithValidProduct() {
        // Test standard validation with valid product
        boolean result = fixedIncomeValidator.validate(validFixedIncomeProduct);
        assertTrue(result, "Valid fixed income product should be valid");

        // Test detailed result
        RuleResult detailedResult = fixedIncomeValidator.validateWithResult(validFixedIncomeProduct);
        assertTrue(detailedResult.isTriggered(), "Valid fixed income product should trigger rule");
        assertEquals("fixedIncomeValidator", detailedResult.getRuleName(), "Rule name should match");
    }

    @Test
    public void testValidateWithInvalidPriceProduct() {
        // Test standard validation with invalid price product
        boolean result = fixedIncomeValidator.validate(invalidPriceProduct);
        assertFalse(result, "Invalid price product should be invalid");

        // Test detailed result
        RuleResult detailedResult = fixedIncomeValidator.validateWithResult(invalidPriceProduct);
        assertFalse(detailedResult.isTriggered(), "Invalid price product should not trigger rule");
    }

    @Test
    public void testValidateWithInvalidCategoryProduct() {
        // Test standard validation with invalid category product
        boolean result = fixedIncomeValidator.validate(invalidCategoryProduct);
        assertFalse(result, "Invalid category product should be invalid");

        // Test detailed result
        RuleResult detailedResult = fixedIncomeValidator.validateWithResult(invalidCategoryProduct);
        assertFalse(detailedResult.isTriggered(), "Invalid category product should not trigger rule");
    }

    @Test
    public void testValidateWithExpression() {
        // Test dynamic expression validation
        String customExpression = "#product != null && #product.price < 2000.0 && #product.category == 'FixedIncome'";

        // Valid product with custom expression
        boolean validResult = fixedIncomeValidator.validateWithExpression(validFixedIncomeProduct, customExpression, config);
        assertTrue(validResult, "Valid fixed income product should be valid with custom expression");

        // Invalid price product with custom expression
        boolean invalidPriceResult = fixedIncomeValidator.validateWithExpression(invalidPriceProduct, customExpression, config);
        assertFalse(invalidPriceResult, "Invalid price product should be invalid with custom expression");

        // Invalid category product with custom expression
        boolean invalidCategoryResult = fixedIncomeValidator.validateWithExpression(invalidCategoryProduct, customExpression, config);
        assertFalse(invalidCategoryResult, "Invalid category product should be invalid with custom expression");
    }

    @Test
    public void testComplexExpression() {
        // Test complex expression that allows both FixedIncome and ETF products with price > 1000.0
        String complexExpression = "#product != null && #product.price > 1000.0 && (#product.category == 'FixedIncome' || #product.category == 'ETF')";

        // ETF product with standard validation (should fail)
        boolean etfStandardResult = fixedIncomeValidator.validate(etfProduct);
        assertFalse(etfStandardResult, "ETF product should be invalid with standard validation");

        // ETF product with complex expression (should pass)
        boolean etfComplexResult = fixedIncomeValidator.validateWithExpression(etfProduct, complexExpression, config);
        assertTrue(etfComplexResult, "ETF product should be valid with complex expression");

        // Fixed income product with complex expression (should pass)
        boolean fixedIncomeComplexResult = fixedIncomeValidator.validateWithExpression(validFixedIncomeProduct, complexExpression, config);
        assertTrue(fixedIncomeComplexResult, "Fixed income product should be valid with complex expression");
    }

    @Test
    public void testNullProduct() {
        // Test with null product
        boolean result = fixedIncomeValidator.validate(null);
        assertFalse(result, "Null product should be invalid");

        // Test with null product and custom expression
        String customExpression = "#product != null";
        boolean expressionResult = fixedIncomeValidator.validateWithExpression(null, customExpression, config);
        assertFalse(expressionResult, "Null product should be invalid with custom expression");
    }

    @Test
    public void testDiscountedPriceExpression() {
        // Test expression using the getDiscountedPrice method
        String discountExpression = "#product != null && #product.getDiscountedPrice(10) < 1500.0";

        // Valid product with discount expression
        boolean validResult = fixedIncomeValidator.validateWithExpression(validFixedIncomeProduct, discountExpression, config);
        assertTrue(validResult, "Valid fixed income product with 10% discount should be valid");

        // Invalid price product with discount expression
        boolean invalidResult = fixedIncomeValidator.validateWithExpression(invalidPriceProduct, discountExpression, config);
        assertFalse(invalidResult, "Invalid price product with 10% discount should be invalid");
    }
}
