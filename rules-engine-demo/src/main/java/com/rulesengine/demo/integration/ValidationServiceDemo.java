/**
 * This class demonstrates the ValidationService functionality.
 * It shows how to use the ValidationService to validate different types of objects
 * using custom validators and the rules engine.
 */
package com.rulesengine.demo.integration;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import com.rulesengine.core.service.validation.ValidationService;
import com.rulesengine.demo.data.MockDataSources;
import com.rulesengine.demo.model.Customer;
import com.rulesengine.demo.model.Product;
import com.rulesengine.demo.model.Trade;
import com.rulesengine.demo.service.validators.CustomerValidator;
import com.rulesengine.demo.service.validators.ProductValidator;
import com.rulesengine.demo.service.validators.TradeValidatorDemo;

import java.util.Arrays;
import java.util.List;

public class ValidationServiceDemo {
    // Services
    private final LookupServiceRegistry registry;
    private final ValidationService validationService;
    
    /**
     * Constructor with dependency injection.
     * 
     * @param registry The lookup service registry
     * @param validationService The validation service
     */
    public ValidationServiceDemo(LookupServiceRegistry registry, ValidationService validationService) {
        this.registry = registry;
        this.validationService = validationService;
    }
    
    public static void main(String[] args) {
        // Create registry and register validators
        LookupServiceRegistry registry = new LookupServiceRegistry();
        
        // Create rules engine
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        
        // Create validation service
        ValidationService validationService = new ValidationService(registry, rulesEngine);
        
        // Create demo class
        ValidationServiceDemo demo = new ValidationServiceDemo(registry, validationService);
        
        // Register validators
        demo.registerValidators();
        
        // Run demos
        demo.demonstrateProductValidation();
        demo.demonstrateCustomerValidation();
        demo.demonstrateTradeValidation();
        demo.demonstrateComplexValidation();
    }
    
    /**
     * Register validators with the registry.
     */
    private void registerValidators() {
        // Register product validators
        registry.registerService(new ProductValidator("premiumProductValidator", 500.0, Double.MAX_VALUE, null));
        registry.registerService(new ProductValidator("budgetProductValidator", 0.0, 200.0, null));
        registry.registerService(new ProductValidator("equityValidator", 0.0, Double.MAX_VALUE, "Equity"));
        registry.registerService(new ProductValidator("fixedIncomeValidator", 0.0, Double.MAX_VALUE, "FixedIncome"));
        registry.registerService(new ProductValidator("etfValidator", 0.0, Double.MAX_VALUE, "ETF"));
        
        // Register customer validators
        registry.registerService(new CustomerValidator("adultValidator", 18, 120, "Gold", "Silver", "Bronze", "Basic"));
        registry.registerService(new CustomerValidator("seniorValidator", 65, 120, "Gold", "Silver"));
        registry.registerService(new CustomerValidator("goldMemberValidator", 0, 120, "Gold"));
        registry.registerService(new CustomerValidator("silverMemberValidator", 0, 120, "Silver"));
        
        // Register trade validators
        registry.registerService(new TradeValidatorDemo("equityTradeValidator",
            new String[]{"Equity"}, 
            new String[]{"InstrumentType", "AssetClass"}));
        registry.registerService(new TradeValidatorDemo("fixedIncomeTradeValidator",
            new String[]{"Bond", "FixedIncome"}, 
            new String[]{"InstrumentType", "AssetClass"}));
        registry.registerService(new TradeValidatorDemo("etfTradeValidator",
            new String[]{"ETF"}, 
            new String[]{"InstrumentType"}));
    }
    
    /**
     * Demonstrate product validation.
     */
    private void demonstrateProductValidation() {
        System.out.println("\n=== Product Validation Demo ===");
        
        // Get products from mock data
        List<Product> products = MockDataSources.getProducts();
        
        // Validate each product with different validators
        for (Product product : products) {
            System.out.println("\nValidating product: " + product.getName() + " ($" + product.getPrice() + ", " + product.getCategory() + ")");
            
            boolean isPremium = validationService.validate("premiumProductValidator", product);
            System.out.println("Is premium product? " + isPremium);
            
            boolean isBudget = validationService.validate("budgetProductValidator", product);
            System.out.println("Is budget product? " + isBudget);
            
            boolean isEquity = validationService.validate("equityValidator", product);
            System.out.println("Is equity product? " + isEquity);
            
            boolean isFixedIncome = validationService.validate("fixedIncomeValidator", product);
            System.out.println("Is fixed income product? " + isFixedIncome);
            
            boolean isETF = validationService.validate("etfValidator", product);
            System.out.println("Is ETF product? " + isETF);
        }
    }
    
    /**
     * Demonstrate customer validation.
     */
    private void demonstrateCustomerValidation() {
        System.out.println("\n=== Customer Validation Demo ===");
        
        // Create test customers
        Customer alice = new Customer("Alice Smith", 35, "Gold", Arrays.asList("Equity", "FixedIncome"));
        Customer bob = new Customer("Bob Johnson", 42, "Silver", Arrays.asList("FixedIncome"));
        Customer charlie = new Customer("Charlie Brown", 67, "Gold", Arrays.asList("FixedIncome", "ETF"));
        Customer dave = new Customer("Dave Wilson", 17, "Basic", Arrays.asList("ETF"));
        
        List<Customer> customers = Arrays.asList(alice, bob, charlie, dave);
        
        // Validate each customer with different validators
        for (Customer customer : customers) {
            System.out.println("\nValidating customer: " + customer.getName() + " (Age: " + customer.getAge() + 
                ", Membership: " + customer.getMembershipLevel() + ")");
            
            boolean isAdult = validationService.validate("adultValidator", customer);
            System.out.println("Is adult? " + isAdult);
            
            boolean isSenior = validationService.validate("seniorValidator", customer);
            System.out.println("Is senior? " + isSenior);
            
            boolean isGoldMember = validationService.validate("goldMemberValidator", customer);
            System.out.println("Is gold member? " + isGoldMember);
            
            boolean isSilverMember = validationService.validate("silverMemberValidator", customer);
            System.out.println("Is silver member? " + isSilverMember);
        }
    }
    
    /**
     * Demonstrate trade validation.
     */
    private void demonstrateTradeValidation() {
        System.out.println("\n=== Trade Validation Demo ===");
        
        // Create test trades
        Trade equityTrade = new Trade("T001", "Equity", "InstrumentType");
        Trade bondTrade = new Trade("T002", "Bond", "InstrumentType");
        Trade etfTrade = new Trade("T003", "ETF", "InstrumentType");
        Trade fixedIncomeTrade = new Trade("T004", "FixedIncome", "AssetClass");
        
        List<Trade> trades = Arrays.asList(equityTrade, bondTrade, etfTrade, fixedIncomeTrade);
        
        // Validate each trade with different validators
        for (Trade trade : trades) {
            System.out.println("\nValidating trade: " + trade.getId() + " (Value: " + trade.getValue() + 
                ", Category: " + trade.getCategory() + ")");
            
            boolean isEquityTrade = validationService.validate("equityTradeValidator", trade);
            System.out.println("Is equity trade? " + isEquityTrade);
            
            boolean isFixedIncomeTrade = validationService.validate("fixedIncomeTradeValidator", trade);
            System.out.println("Is fixed income trade? " + isFixedIncomeTrade);
            
            boolean isETFTrade = validationService.validate("etfTradeValidator", trade);
            System.out.println("Is ETF trade? " + isETFTrade);
        }
    }
    
    /**
     * Demonstrate complex validation scenarios.
     */
    private void demonstrateComplexValidation() {
        System.out.println("\n=== Complex Validation Demo ===");
        
        // Create test objects
        Customer goldCustomer = new Customer("Alice Smith", 35, "Gold", Arrays.asList("Equity", "FixedIncome"));
        Product premiumProduct = new Product("US Treasury Bond", 1200.0, "FixedIncome");
        Product budgetProduct = new Product("Corporate Bond", 150.0, "FixedIncome");
        
        // Scenario 1: Gold customer buying premium fixed income product
        System.out.println("\nScenario 1: Gold customer buying premium fixed income product");
        boolean isGoldMember = validationService.validate("goldMemberValidator", goldCustomer);
        boolean isPremiumProduct = validationService.validate("premiumProductValidator", premiumProduct);
        boolean isFixedIncomeProduct = validationService.validate("fixedIncomeValidator", premiumProduct);
        
        System.out.println("Is gold member? " + isGoldMember);
        System.out.println("Is premium product? " + isPremiumProduct);
        System.out.println("Is fixed income product? " + isFixedIncomeProduct);
        System.out.println("Validation result: " + (isGoldMember && isPremiumProduct && isFixedIncomeProduct));
        
        // Scenario 2: Gold customer buying budget fixed income product
        System.out.println("\nScenario 2: Gold customer buying budget fixed income product");
        boolean isBudgetProduct = validationService.validate("budgetProductValidator", budgetProduct);
        boolean isBudgetFixedIncome = validationService.validate("fixedIncomeValidator", budgetProduct);
        
        System.out.println("Is gold member? " + isGoldMember);
        System.out.println("Is budget product? " + isBudgetProduct);
        System.out.println("Is fixed income product? " + isBudgetFixedIncome);
        System.out.println("Validation result: " + (isGoldMember && isBudgetProduct && isBudgetFixedIncome));
    }
}