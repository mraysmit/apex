package com.rulesengine.demo.integration;

import com.rulesengine.core.service.engine.ExpressionEvaluatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for dynamic method execution using SpEL.
 * This class tests dynamic method execution without depending on demo classes.
 */
public class DynamicMethodExecutionTest {
    
    private ExpressionEvaluatorService evaluatorService;
    private StandardEvaluationContext context;
    
    @BeforeEach
    public void setUp() {
        // Initialize services
        evaluatorService = new ExpressionEvaluatorService();
        
        // Initialize context
        context = new StandardEvaluationContext();
        
        // Create test service and add to context
        TestPricingService pricingService = new TestPricingService();
        context.setVariable("pricingService", pricingService);
        
        // Add base price variable
        context.setVariable("basePrice", 100.0);
    }
    
    /**
     * Test dynamic method execution with different pricing strategies.
     */
    @Test
    public void testDynamicMethodExecution() {
        // Test standard pricing
        Double standardPrice = evaluatorService.evaluate(
            "#pricingService.calculateStandardPrice(#basePrice)", context, Double.class);
        assertEquals(100.0, standardPrice, 0.01, "Standard price should be 100.0");
        
        // Test premium pricing
        Double premiumPrice = evaluatorService.evaluate(
            "#pricingService.calculatePremiumPrice(#basePrice)", context, Double.class);
        assertEquals(120.0, premiumPrice, 0.01, "Premium price should be 120.0");
        
        // Test sale pricing
        Double salePrice = evaluatorService.evaluate(
            "#pricingService.calculateSalePrice(#basePrice)", context, Double.class);
        assertEquals(80.0, salePrice, 0.01, "Sale price should be 80.0");
        
        // Test clearance pricing
        Double clearancePrice = evaluatorService.evaluate(
            "#pricingService.calculateClearancePrice(#basePrice)", context, Double.class);
        assertEquals(50.0, clearancePrice, 0.01, "Clearance price should be 50.0");
    }
    
    /**
     * Test dynamic method selection based on a condition.
     */
    @Test
    public void testDynamicMethodSelection() {
        // Test dynamic method selection with high base price
        context.setVariable("basePrice", 100.0);
        Double highBasePrice = evaluatorService.evaluate(
            "#basePrice > 50 ? #pricingService.calculatePremiumPrice(#basePrice) : #pricingService.calculateSalePrice(#basePrice)",
            context, Double.class);
        assertEquals(120.0, highBasePrice, 0.01, "High base price should use premium pricing");
        
        // Test dynamic method selection with low base price
        context.setVariable("basePrice", 40.0);
        Double lowBasePrice = evaluatorService.evaluate(
            "#basePrice > 50 ? #pricingService.calculatePremiumPrice(#basePrice) : #pricingService.calculateSalePrice(#basePrice)",
            context, Double.class);
        assertEquals(32.0, lowBasePrice, 0.01, "Low base price should use sale pricing");
    }
    
    /**
     * Test dynamic method execution with variable method name.
     */
    @Test
    public void testDynamicMethodName() {
        // Test different pricing strategies with dynamic method names
        String[] strategies = {"Standard", "Premium", "Sale", "Clearance"};
        double[] expectedPrices = {100.0, 120.0, 80.0, 50.0};
        
        for (int i = 0; i < strategies.length; i++) {
            String methodName = "calculate" + strategies[i] + "Price";
            String expression = "#pricingService." + methodName + "(#basePrice)";
            
            Double price = evaluatorService.evaluate(expression, context, Double.class);
            assertEquals(expectedPrices[i], price, 0.01, 
                strategies[i] + " price should be " + expectedPrices[i]);
        }
    }
    
    /**
     * Test dynamic method execution with method chaining.
     */
    @Test
    public void testMethodChaining() {
        // Add a discount variable
        context.setVariable("discount", 0.1);
        
        // Test method chaining
        Double discountedPrice = evaluatorService.evaluate(
            "#pricingService.calculatePremiumPrice(#basePrice).doubleValue() * (1 - #discount)",
            context, Double.class);
        
        // Premium price is 120.0, with 10% discount should be 108.0
        assertEquals(108.0, discountedPrice, 0.01, "Discounted premium price should be 108.0");
    }
    
    /**
     * Simple pricing service for testing.
     */
    public static class TestPricingService {
        
        public double calculateStandardPrice(double basePrice) {
            return basePrice;
        }
        
        public double calculatePremiumPrice(double basePrice) {
            return basePrice * 1.2;
        }
        
        public double calculateSalePrice(double basePrice) {
            return basePrice * 0.8;
        }
        
        public double calculateClearancePrice(double basePrice) {
            return basePrice * 0.5;
        }
    }
}