/**
 * Test class for IntegratedServicesDemo.
 * This class verifies that the integrated services demo works correctly.
 */
package com.rulesengine.demo.integration;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.service.data.DataServiceManager;
import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import com.rulesengine.core.service.transform.EnrichmentService;
import com.rulesengine.core.service.validation.ValidationService;
import com.rulesengine.demo.model.Customer;
import com.rulesengine.demo.model.Product;
import com.rulesengine.demo.service.enrichers.CustomerEnricher;
import com.rulesengine.demo.service.enrichers.ProductEnricher;
import com.rulesengine.demo.service.validators.CustomerValidator;
import com.rulesengine.demo.service.validators.ProductValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IntegratedServicesDemoTest {
    private LookupServiceRegistry registry;
    private ValidationService validationService;
    private EnrichmentService enrichmentService;
    private RulesEngine rulesEngine;
    private DataServiceManager dataServiceManager;
    private IntegratedServicesDemo demo;
    
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;
    
    @BeforeEach
    public void setUp() {
        // Create registry
        registry = new LookupServiceRegistry();
        
        // Create rules engine
        rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        
        // Create services
        validationService = new ValidationService(registry, rulesEngine);
        enrichmentService = new EnrichmentService(registry, rulesEngine);
        dataServiceManager = new DataServiceManager();
        dataServiceManager.initializeWithMockData();
        
        // Create demo class
        demo = new IntegratedServicesDemo(
            registry, 
            validationService, 
            enrichmentService, 
            rulesEngine,
            dataServiceManager
        );
        
        // Register services
        registerServices();
        
        // Capture System.out to verify output
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
    }
    
    /**
     * Register services with the registry.
     */
    private void registerServices() {
        // Register validators
        registry.registerService(new CustomerValidator("adultValidator", 18, 120, "Gold", "Silver", "Bronze", "Basic"));
        registry.registerService(new CustomerValidator("goldMemberValidator", 0, 120, "Gold"));
        registry.registerService(new CustomerValidator("silverMemberValidator", 0, 120, "Silver"));
        registry.registerService(new ProductValidator("premiumProductValidator", 500.0, Double.MAX_VALUE, null));
        registry.registerService(new ProductValidator("budgetProductValidator", 0.0, 200.0, null));
        registry.registerService(new ProductValidator("equityValidator", 0.0, Double.MAX_VALUE, "Equity"));
        registry.registerService(new ProductValidator("fixedIncomeValidator", 0.0, Double.MAX_VALUE, "FixedIncome"));
        registry.registerService(new ProductValidator("etfValidator", 0.0, Double.MAX_VALUE, "ETF"));
        
        // Register enrichers
        registry.registerService(new CustomerEnricher("customerEnricher"));
        registry.registerService(new ProductEnricher("productEnricher"));
    }
    
    @Test
    public void testCustomerValidation() {
        // Create test customers
        Customer validCustomer = new Customer("Alice Smith", 35, "Gold", Arrays.asList("Equity"));
        Customer invalidCustomer = new Customer("Charlie Brown", 17, "Basic", Arrays.asList("ETF"));
        
        // Test validation
        boolean isValidCustomer = validationService.validate("adultValidator", validCustomer);
        boolean isInvalidCustomer = validationService.validate("adultValidator", invalidCustomer);
        
        // Verify results
        assertTrue(isValidCustomer, "Adult customer should be valid");
        assertFalse(isInvalidCustomer, "Minor customer should be invalid");
    }
    
    @Test
    public void testCustomerEnrichment() {
        // Create test customer
        Customer customer = new Customer("Alice Smith", 35, "Gold", Arrays.asList("Equity"));
        
        // Enrich customer
        Customer enrichedCustomer = (Customer) enrichmentService.enrich("customerEnricher", customer);
        
        // Verify enrichment
        assertNotNull(enrichedCustomer, "Enriched customer should not be null");
        assertTrue(enrichedCustomer.getPreferredCategories().size() > customer.getPreferredCategories().size(),
            "Enriched customer should have more preferred categories");
        assertTrue(enrichedCustomer.getPreferredCategories().contains("FixedIncome"),
            "Gold customer should have FixedIncome in preferred categories");
        assertTrue(enrichedCustomer.getPreferredCategories().contains("ETF"),
            "Gold customer should have ETF in preferred categories");
    }
    
    @Test
    public void testProductDiscounts() {
        // Create test customer and product
        Customer goldCustomer = new Customer("Alice Smith", 35, "Gold", Arrays.asList("Equity", "FixedIncome"));
        Product expensiveProduct = new Product("US Treasury Bond", 1200.0, "FixedIncome");
        
        // Get customer discount
        CustomerEnricher customerEnricher = (CustomerEnricher) registry.getService("customerEnricher", CustomerEnricher.class);
        double discount = customerEnricher.getDiscountForCustomer(goldCustomer);
        
        // Apply discount using rule condition
        String condition = "#coreData.price > 1000.0";
        Product discountedProduct = (Product) enrichmentService.applyRuleCondition(
            condition, 
            expensiveProduct, 
            discount, 
            "productEnricher"
        );
        
        // Verify discount
        assertNotNull(discountedProduct, "Discounted product should not be null");
        assertTrue(discountedProduct.getPrice() < expensiveProduct.getPrice(),
            "Discounted product should have lower price");
        assertEquals(expensiveProduct.getPrice() * (1 - discount), discountedProduct.getPrice(), 0.01,
            "Discount should be applied correctly");
    }
    
    @Test
    public void testFullDemo() {
        // Run the full demo
        demo.demonstrateInvestmentPortfolioManagement();
        
        // Verify output contains expected strings
        String output = outContent.toString();
        
        // Verify customer processing
        assertTrue(output.contains("=== Processing Customer: Alice Smith ==="),
            "Output should contain processing for Alice Smith");
        assertTrue(output.contains("=== Processing Customer: Bob Johnson ==="),
            "Output should contain processing for Bob Johnson");
        assertTrue(output.contains("=== Processing Customer: Charlie Brown ==="),
            "Output should contain processing for Charlie Brown");
        assertTrue(output.contains("=== Processing Customer: Diana Prince ==="),
            "Output should contain processing for Diana Prince");
        
        // Verify validation failure for minor
        assertTrue(output.contains("Customer validation failed"),
            "Output should contain validation failure message");
        
        // Verify enrichment
        assertTrue(output.contains("Enriched Preferred Categories"),
            "Output should contain enriched categories");
        
        // Verify product recommendations
        assertTrue(output.contains("Suitable Products"),
            "Output should contain suitable products");
        assertTrue(output.contains("Recommended Products with Discounts"),
            "Output should contain discounted products");
        
        // Verify investment recommendations
        assertTrue(output.contains("Investment Recommendations"),
            "Output should contain investment recommendations");
        assertTrue(output.contains("As a Gold member"),
            "Output should contain gold member recommendations");
        assertTrue(output.contains("As a Silver member"),
            "Output should contain silver member recommendations");
        
        // Print debug log for test results
        System.setOut(originalOut);
        System.out.println("[DEBUG_LOG] IntegratedServicesDemo test completed successfully");
        System.out.println("[DEBUG_LOG] Output contains all expected sections");
    }
}