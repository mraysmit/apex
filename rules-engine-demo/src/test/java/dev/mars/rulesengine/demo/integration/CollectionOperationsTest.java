package dev.mars.rulesengine.demo.integration;

import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for collection operations using SpEL.
 * This class tests collection operations without depending on demo classes.
 */
public class CollectionOperationsTest {
    
    private ExpressionEvaluatorService evaluatorService;
    private StandardEvaluationContext context;
    
    @BeforeEach
    public void setUp() {
        // Initialize services
        evaluatorService = new ExpressionEvaluatorService();
        
        // Initialize context
        context = new StandardEvaluationContext();
        
        // Create test data
        List<TestProduct> products = createTestProducts();
        context.setVariable("products", products);
        
        // Add price threshold variable
        context.setVariable("priceThreshold", 500.0);
    }
    
    /**
     * Test collection selection (filtering) using SpEL.
     */
    @Test
    public void testCollectionSelection() {
        // Test collection selection - filter products with category "FixedIncome"
        RuleResult result = evaluatorService.evaluateWithResult(
            "#products.?[category == 'FixedIncome']", context, List.class);
        
        // Verify result
        assertTrue(result.isTriggered(), "Rule should be triggered");
        
        // Get the actual value using evaluate
        List<TestProduct> fixedIncomeProducts = evaluatorService.evaluate(
            "#products.?[category == 'FixedIncome']", context, List.class);
        
        // Verify the filtered products
        assertNotNull(fixedIncomeProducts, "Fixed income products should not be null");
        assertEquals(2, fixedIncomeProducts.size(), "Should find 2 fixed income products");
        for (TestProduct product : fixedIncomeProducts) {
            assertEquals("FixedIncome", product.getCategory(), "Product category should be FixedIncome");
        }
    }
    
    /**
     * Test collection projection using SpEL.
     */
    @Test
    public void testCollectionProjection() {
        // Test collection projection - get all product names
        RuleResult result = evaluatorService.evaluateWithResult(
            "#products.![name]", context, List.class);
        
        // Verify result
        assertTrue(result.isTriggered(), "Rule should be triggered");
        
        // Get the actual value using evaluate
        List<String> productNames = evaluatorService.evaluate(
            "#products.![name]", context, List.class);
        
        // Verify the projected names
        assertNotNull(productNames, "Product names should not be null");
        assertEquals(5, productNames.size(), "Should find 5 product names");
        assertTrue(productNames.contains("US Treasury Bond"), "Should contain US Treasury Bond");
        assertTrue(productNames.contains("Apple Stock"), "Should contain Apple Stock");
    }
    
    /**
     * Test combining selection and projection using SpEL.
     */
    @Test
    public void testCombinedSelectionAndProjection() {
        // Test combining selection and projection - names of equity products
        RuleResult result = evaluatorService.evaluateWithResult(
            "#products.?[category == 'Equity'].![name]", context, List.class);
        
        // Verify result
        assertTrue(result.isTriggered(), "Rule should be triggered");
        
        // Get the actual value using evaluate
        List<String> equityProductNames = evaluatorService.evaluate(
            "#products.?[category == 'Equity'].![name]", context, List.class);
        
        // Verify the combined selection and projection
        assertNotNull(equityProductNames, "Equity product names should not be null");
        assertEquals(1, equityProductNames.size(), "Should find 1 equity product name");
        assertEquals("Apple Stock", equityProductNames.get(0), "Equity product should be Apple Stock");
    }
    
    /**
     * Test first and last element selection using SpEL.
     */
    @Test
    public void testFirstAndLastElementSelection() {
        // Test first element selection - first product with price > priceThreshold
        RuleResult result1 = evaluatorService.evaluateWithResult(
            "#products.^[price > #priceThreshold].name", context, String.class);
        
        // Verify result
        assertTrue(result1.isTriggered(), "Rule should be triggered");
        
        // Get the actual value using evaluate
        String firstExpensiveProduct = evaluatorService.evaluate(
            "#products.^[price > #priceThreshold].name", context, String.class);
        
        // Verify the first element selection
        assertEquals("US Treasury Bond", firstExpensiveProduct, "First expensive product should be US Treasury Bond");
        
        // Test last element selection - last product with price < 200
        RuleResult result2 = evaluatorService.evaluateWithResult(
            "#products.$[price < 200].name", context, String.class);
        
        // Verify result
        assertTrue(result2.isTriggered(), "Rule should be triggered");
        
        // Get the actual value using evaluate
        String lastCheapProduct = evaluatorService.evaluate(
            "#products.$[price < 200].name", context, String.class);
        
        // Verify the last element selection
        assertEquals("Corporate Bond", lastCheapProduct, "Last cheap product should be Corporate Bond");
    }
    
    /**
     * Create test products for the tests.
     * 
     * @return List of test products
     */
    private List<TestProduct> createTestProducts() {
        List<TestProduct> products = new ArrayList<>();
        products.add(new TestProduct("US Treasury Bond", 1200.0, "FixedIncome"));
        products.add(new TestProduct("Apple Stock", 800.0, "Equity"));
        products.add(new TestProduct("S&P 500 ETF", 150.0, "ETF"));
        products.add(new TestProduct("Gold Futures", 200.0, "Derivative"));
        products.add(new TestProduct("Corporate Bond", 100.0, "FixedIncome"));
        return products;
    }
    
    /**
     * Simple product class for testing.
     */
    public static class TestProduct {
        private String name;
        private double price;
        private String category;
        
        public TestProduct(String name, double price, String category) {
            this.name = name;
            this.price = price;
            this.category = category;
        }
        
        public String getName() {
            return name;
        }
        
        public double getPrice() {
            return price;
        }
        
        public String getCategory() {
            return category;
        }
    }
}