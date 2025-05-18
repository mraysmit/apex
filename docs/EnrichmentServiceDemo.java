/**
 * This class demonstrates the EnrichmentService functionality.
 * It shows how to use the EnrichmentService to enrich different types of objects
 * using custom enrichers and the rules engine.
 */
package com.rulesengine.demo.integration;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import com.rulesengine.core.service.transform.EnrichmentService;
import com.rulesengine.demo.data.MockDataSources;
import com.rulesengine.demo.model.Customer;
import com.rulesengine.demo.model.Product;
import com.rulesengine.demo.model.Trade;
import com.rulesengine.demo.service.enrichers.CustomerEnricher;
import com.rulesengine.demo.service.enrichers.ProductEnricher;
import com.rulesengine.demo.service.enrichers.TradeEnricher;

import java.util.Arrays;
import java.util.List;

public class EnrichmentServiceDemo {
    // Services
    private final LookupServiceRegistry registry;
    private final EnrichmentService enrichmentService;
    private final RulesEngine rulesEngine;
    
    /**
     * Constructor with dependency injection.
     * 
     * @param registry The lookup service registry
     * @param enrichmentService The enrichment service
     * @param rulesEngine The rules engine
     */
    public EnrichmentServiceDemo(LookupServiceRegistry registry, EnrichmentService enrichmentService, RulesEngine rulesEngine) {
        this.registry = registry;
        this.enrichmentService = enrichmentService;
        this.rulesEngine = rulesEngine;
    }
    
    public static void main(String[] args) {
        // Create registry and register enrichers
        LookupServiceRegistry registry = new LookupServiceRegistry();
        
        // Create rules engine
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        
        // Create enrichment service
        EnrichmentService enrichmentService = new EnrichmentService(registry, rulesEngine);
        
        // Create demo class
        EnrichmentServiceDemo demo = new EnrichmentServiceDemo(registry, enrichmentService, rulesEngine);
        
        // Register enrichers
        demo.registerEnrichers();
        
        // Run demos
        demo.demonstrateBasicEnrichment();
        demo.demonstrateRuleBasedEnrichment();
        demo.demonstrateConditionalEnrichment();
        demo.demonstrateComplexEnrichment();
    }
    
    /**
     * Register enrichers with the registry.
     */
    private void registerEnrichers() {
        // Register product enrichers
        registry.registerService(new ProductEnricher("productEnricher"));
        
        // Register customer enrichers
        registry.registerService(new CustomerEnricher("customerEnricher"));
        
        // Register trade enrichers
        registry.registerService(new TradeEnricher("tradeEnricher"));
    }
    
    /**
     * Demonstrate basic enrichment without rules.
     */
    private void demonstrateBasicEnrichment() {
        System.out.println("\n=== Basic Enrichment Demo ===");
        
        // Get products from mock data
        List<Product> products = MockDataSources.getProducts();
        
        // Enrich each product
        System.out.println("\nEnriching products:");
        for (Product product : products) {
            System.out.println("\nOriginal product: " + product.getName() + " ($" + product.getPrice() + ", " + product.getCategory() + ")");
            
            Product enrichedProduct = (Product) enrichmentService.enrich("productEnricher", product);
            
            System.out.println("Enriched product: " + enrichedProduct.getName() + " ($" + enrichedProduct.getPrice() + ", " + enrichedProduct.getCategory() + ")");
        }
        
        // Create test customers
        Customer alice = new Customer("Alice Smith", 35, "Gold", Arrays.asList("Equity"));
        Customer bob = new Customer("Bob Johnson", 42, "Silver", Arrays.asList());
        
        // Enrich each customer
        System.out.println("\nEnriching customers:");
        for (Customer customer : Arrays.asList(alice, bob)) {
            System.out.println("\nOriginal customer: " + customer.getName() + " (Age: " + customer.getAge() + 
                ", Membership: " + customer.getMembershipLevel() + ", Preferred: " + customer.getPreferredCategories() + ")");
            
            Customer enrichedCustomer = (Customer) enrichmentService.enrich("customerEnricher", customer);
            
            System.out.println("Enriched customer: " + enrichedCustomer.getName() + " (Age: " + enrichedCustomer.getAge() + 
                ", Membership: " + enrichedCustomer.getMembershipLevel() + ", Preferred: " + enrichedCustomer.getPreferredCategories() + ")");
        }
        
        // Create test trades
        Trade equityTrade = new Trade("T001", "Equity", "InstrumentType");
        Trade bondTrade = new Trade("T002", "Bond", "InstrumentType");
        
        // Enrich each trade
        System.out.println("\nEnriching trades:");
        for (Trade trade : Arrays.asList(equityTrade, bondTrade)) {
            System.out.println("\nOriginal trade: " + trade.getId() + " (Value: " + trade.getValue() + 
                ", Category: " + trade.getCategory() + ")");
            
            Trade enrichedTrade = (Trade) enrichmentService.enrich("tradeEnricher", trade);
            
            System.out.println("Enriched trade: " + enrichedTrade.getId() + " (Value: " + enrichedTrade.getValue() + 
                ", Category: " + enrichedTrade.getCategory() + ")");
        }
    }
    
    /**
     * Demonstrate rule-based enrichment using the applyRule method.
     */
    private void demonstrateRuleBasedEnrichment() {
        System.out.println("\n=== Rule-Based Enrichment Demo ===");
        
        // Create test objects
        Product product = new Product("US Treasury Bond", 1200.0, "FixedIncome");
        Customer customer = new Customer("Alice Smith", 35, "Gold", Arrays.asList("Equity"));
        
        // Create lookup data
        CustomerEnricher customerEnricher = (CustomerEnricher) registry.getService("customerEnricher", CustomerEnricher.class);
        double discount = customerEnricher.getDiscountForCustomer(customer);
        
        // Create a rule to check if the product is expensive and the customer is eligible for a discount
        Rule expensiveProductRule = new Rule(
            "Expensive Product Rule",
            "#coreData.price > 1000.0 && #lookupData > 0.1",
            "Product is expensive and customer is eligible for a significant discount"
        );
        
        // Apply the rule
        System.out.println("\nApplying rule to product with customer discount as lookup data:");
        System.out.println("Original product: " + product.getName() + " ($" + product.getPrice() + ", " + product.getCategory() + ")");
        System.out.println("Customer discount: " + (discount * 100) + "%");
        
        Product enrichedProduct = (Product) enrichmentService.applyRule(
            expensiveProductRule, 
            product, 
            discount, 
            "productEnricher"
        );
        
        System.out.println("Rule condition: " + expensiveProductRule.getCondition());
        System.out.println("Enriched product: " + enrichedProduct.getName() + " ($" + enrichedProduct.getPrice() + ", " + enrichedProduct.getCategory() + ")");
    }
    
    /**
     * Demonstrate conditional enrichment using the applyRuleCondition method.
     */
    private void demonstrateConditionalEnrichment() {
        System.out.println("\n=== Conditional Enrichment Demo ===");
        
        // Create test objects
        Trade equityTrade = new Trade("T001", "Equity", "InstrumentType");
        Trade bondTrade = new Trade("T002", "Bond", "InstrumentType");
        
        // Create lookup data - a list of preferred categories
        List<String> preferredCategories = Arrays.asList("Equity", "ETF");
        
        // Apply conditional enrichment
        System.out.println("\nApplying conditional enrichment to trades with preferred categories as lookup data:");
        
        for (Trade trade : Arrays.asList(equityTrade, bondTrade)) {
            System.out.println("\nOriginal trade: " + trade.getId() + " (Value: " + trade.getValue() + 
                ", Category: " + trade.getCategory() + ")");
            
            // Create a condition to check if the trade's value is in the preferred categories
            String condition = "#lookupData.contains(#coreData.value)";
            
            Trade enrichedTrade = (Trade) enrichmentService.applyRuleCondition(
                condition, 
                trade, 
                preferredCategories, 
                "tradeEnricher"
            );
            
            System.out.println("Rule condition: " + condition);
            System.out.println("Preferred categories: " + preferredCategories);
            System.out.println("Enriched trade: " + enrichedTrade.getId() + " (Value: " + enrichedTrade.getValue() + 
                ", Category: " + enrichedTrade.getCategory() + ")");
        }
    }
    
    /**
     * Demonstrate complex enrichment scenarios.
     */
    private void demonstrateComplexEnrichment() {
        System.out.println("\n=== Complex Enrichment Demo ===");
        
        // Create test objects
        Customer goldCustomer = new Customer("Alice Smith", 35, "Gold", Arrays.asList("Equity"));
        Customer silverCustomer = new Customer("Bob Johnson", 42, "Silver", Arrays.asList());
        Product expensiveProduct = new Product("US Treasury Bond", 1200.0, "FixedIncome");
        Product cheapProduct = new Product("Corporate Bond", 150.0, "FixedIncome");
        
        // Scenario 1: Gold customer buying expensive fixed income product
        System.out.println("\nScenario 1: Gold customer buying expensive fixed income product");
        
        // First, enrich the customer to get recommended categories
        Customer enrichedGoldCustomer = (Customer) enrichmentService.enrich("customerEnricher", goldCustomer);
        System.out.println("Enriched customer: " + enrichedGoldCustomer.getName() + " (Preferred: " + enrichedGoldCustomer.getPreferredCategories() + ")");
        
        // Check if the customer is interested in the product category
        boolean isInterestedInCategory = enrichedGoldCustomer.getPreferredCategories().contains(expensiveProduct.getCategory());
        System.out.println("Is customer interested in " + expensiveProduct.getCategory() + "? " + isInterestedInCategory);
        
        // If interested, apply discount based on membership level
        if (isInterestedInCategory) {
            CustomerEnricher customerEnricher = (CustomerEnricher) registry.getService("customerEnricher", CustomerEnricher.class);
            double discount = customerEnricher.getDiscountForCustomer(enrichedGoldCustomer);
            
            // Create a rule condition
            String condition = "#coreData.price > 1000.0";
            
            // Apply conditional enrichment
            Product enrichedProduct = (Product) enrichmentService.applyRuleCondition(
                condition, 
                expensiveProduct, 
                discount, 
                "productEnricher"
            );
            
            System.out.println("Rule condition: " + condition);
            System.out.println("Customer discount: " + (discount * 100) + "%");
            System.out.println("Original product: " + expensiveProduct.getName() + " ($" + expensiveProduct.getPrice() + ")");
            System.out.println("Enriched product: " + enrichedProduct.getName() + " ($" + enrichedProduct.getPrice() + ")");
        }
        
        // Scenario 2: Silver customer buying cheap fixed income product
        System.out.println("\nScenario 2: Silver customer buying cheap fixed income product");
        
        // First, enrich the customer to get recommended categories
        Customer enrichedSilverCustomer = (Customer) enrichmentService.enrich("customerEnricher", silverCustomer);
        System.out.println("Enriched customer: " + enrichedSilverCustomer.getName() + " (Preferred: " + enrichedSilverCustomer.getPreferredCategories() + ")");
        
        // Check if the customer is interested in the product category
        isInterestedInCategory = enrichedSilverCustomer.getPreferredCategories().contains(cheapProduct.getCategory());
        System.out.println("Is customer interested in " + cheapProduct.getCategory() + "? " + isInterestedInCategory);
        
        // If interested, apply discount based on membership level
        if (isInterestedInCategory) {
            CustomerEnricher customerEnricher = (CustomerEnricher) registry.getService("customerEnricher", CustomerEnricher.class);
            double discount = customerEnricher.getDiscountForCustomer(enrichedSilverCustomer);
            
            // Create a rule condition
            String condition = "#coreData.price <= 1000.0";
            
            // Apply conditional enrichment
            Product enrichedProduct = (Product) enrichmentService.applyRuleCondition(
                condition, 
                cheapProduct, 
                discount, 
                "productEnricher"
            );
            
            System.out.println("Rule condition: " + condition);
            System.out.println("Customer discount: " + (discount * 100) + "%");
            System.out.println("Original product: " + cheapProduct.getName() + " ($" + cheapProduct.getPrice() + ")");
            System.out.println("Enriched product: " + enrichedProduct.getName() + " ($" + enrichedProduct.getPrice() + ")");
        }
    }
}