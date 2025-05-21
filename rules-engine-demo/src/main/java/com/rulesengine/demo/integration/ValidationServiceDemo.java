/**
 * This class demonstrates the ValidationService functionality.
 * It shows how to use the ValidationService to validate different types of objects
 * using custom validators and the rules engine.
 */
package com.rulesengine.demo.integration;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.config.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import com.rulesengine.core.service.validation.ValidationService;
import com.rulesengine.demo.data.MockDataSources;
import com.rulesengine.demo.model.Customer;
import com.rulesengine.demo.model.Product;
import com.rulesengine.demo.model.Trade;
import com.rulesengine.demo.service.validators.CustomerValidator;
import com.rulesengine.demo.service.validators.DynamicCustomerValidatorDemoConfig;
import com.rulesengine.demo.service.validators.ProductValidator;
import com.rulesengine.demo.service.validators.TradeValidatorDemo;

import java.util.*;

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

        // Demonstrate RuleResult usage
        demo.demonstrateRuleResultUsage();
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

        // Create a DynamicCustomerValidatorDemoConfig
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        DynamicCustomerValidatorDemoConfig config = new DynamicCustomerValidatorDemoConfig(rulesEngine);

        // Register customer validators with parameter maps
        // Adult validator (18-120, all membership levels)
        Map<String, Object> adultParams = new HashMap<>();
        adultParams.put("minAge", 18);
        adultParams.put("maxAge", 120);
        adultParams.put("allowedMembershipLevels", Arrays.asList("Gold", "Silver", "Bronze", "Basic"));
        registry.registerService(new CustomerValidator("adultValidator", adultParams, config));

        // Senior validator (65-120, Gold and Silver)
        Map<String, Object> seniorParams = new HashMap<>();
        seniorParams.put("minAge", 65);
        seniorParams.put("maxAge", 120);
        seniorParams.put("allowedMembershipLevels", Arrays.asList("Gold", "Silver"));
        registry.registerService(new CustomerValidator("seniorValidator", seniorParams, config));

        // Gold member validator (any age, Gold only)
        Map<String, Object> goldParams = new HashMap<>();
        goldParams.put("minAge", 0);
        goldParams.put("maxAge", 120);
        goldParams.put("allowedMembershipLevels", Collections.singletonList("Gold"));
        registry.registerService(new CustomerValidator("goldMemberValidator", goldParams, config));

        // Silver member validator (any age, Silver only)
        Map<String, Object> silverParams = new HashMap<>();
        silverParams.put("minAge", 0);
        silverParams.put("maxAge", 120);
        silverParams.put("allowedMembershipLevels", Collections.singletonList("Silver"));
        registry.registerService(new CustomerValidator("silverMemberValidator", silverParams, config));

        // Register trade validators
        registry.registerService(new TradeValidatorDemo("equityTradeValidator", new String[]{"Equity"}, new String[]{"InstrumentType", "AssetClass"}));
        registry.registerService(new TradeValidatorDemo("fixedIncomeTradeValidator", new String[]{"Bond", "FixedIncome"}, new String[]{"InstrumentType", "AssetClass"}));
        registry.registerService(new TradeValidatorDemo("etfTradeValidator", new String[]{"ETF"}, new String[]{"InstrumentType"}));
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

    /**
     * Demonstrate RuleResult usage.
     * This method shows how to use the validateWithResult method and properly utilize RuleResult objects.
     */
    private void demonstrateRuleResultUsage() {
        System.out.println("\n=== RuleResult Usage Demo ===");

        // Create test objects
        Customer goldCustomer = new Customer("Alice Smith", 35, "Gold", Arrays.asList("Equity", "FixedIncome"));
        Product premiumProduct = new Product("US Treasury Bond", 1200.0, "FixedIncome");

        // Example 1: Successful validation with RuleResult
        System.out.println("\nExample 1: Successful validation with RuleResult");
        RuleResult goldMemberResult = validationService.validateWithResult("goldMemberValidator", goldCustomer);

        System.out.println("Rule Name: " + goldMemberResult.getRuleName());
        System.out.println("Message: " + goldMemberResult.getMessage());
        System.out.println("Is Triggered: " + goldMemberResult.isTriggered());
        System.out.println("Result Type: " + goldMemberResult.getResultType());
        System.out.println("Timestamp: " + goldMemberResult.getTimestamp());

        // Example 2: Failed validation with RuleResult
        System.out.println("\nExample 2: Failed validation with RuleResult");
        RuleResult seniorResult = validationService.validateWithResult("seniorValidator", goldCustomer);

        System.out.println("Rule Name: " + seniorResult.getRuleName());
        System.out.println("Message: " + seniorResult.getMessage());
        System.out.println("Is Triggered: " + seniorResult.isTriggered());
        System.out.println("Result Type: " + seniorResult.getResultType());

        // Example 3: Error validation with RuleResult (non-existent validator)
        System.out.println("\nExample 3: Error validation with RuleResult (non-existent validator)");
        RuleResult errorResult = validationService.validateWithResult("nonExistentValidator", goldCustomer);

        System.out.println("Rule Name: " + errorResult.getRuleName());
        System.out.println("Message: " + errorResult.getMessage());
        System.out.println("Is Triggered: " + errorResult.isTriggered());
        System.out.println("Result Type: " + errorResult.getResultType());

        // Example 4: Type mismatch with RuleResult
        System.out.println("\nExample 4: Type mismatch with RuleResult");
        RuleResult typeMismatchResult = validationService.validateWithResult("goldMemberValidator", premiumProduct);

        System.out.println("Rule Name: " + typeMismatchResult.getRuleName());
        System.out.println("Message: " + typeMismatchResult.getMessage());
        System.out.println("Is Triggered: " + typeMismatchResult.isTriggered());
        System.out.println("Result Type: " + typeMismatchResult.getResultType());

        // Example 5: Using RuleResult in conditional logic
        System.out.println("\nExample 5: Using RuleResult in conditional logic");
        RuleResult premiumProductResult = validationService.validateWithResult("premiumProductValidator", premiumProduct);

        if (goldMemberResult.isTriggered() && premiumProductResult.isTriggered()) {
            System.out.println("Gold member can purchase premium product");
        } else {
            System.out.println("Validation failed");
        }

        // Example 6: Handling different result types
        System.out.println("\nExample 6: Handling different result types");
        RuleResult result = validationService.validateWithResult("equityValidator", premiumProduct);

        switch (result.getResultType()) {
            case MATCH:
                System.out.println("Validation successful: " + result.getMessage());
                break;
            case NO_MATCH:
                System.out.println("Validation failed: Rule conditions not met");
                break;
            case ERROR:
                System.out.println("Validation error: " + result.getMessage());
                break;
            case NO_RULES:
                System.out.println("No rules to evaluate");
                break;
            default:
                System.out.println("Unknown result type");
        }
    }
}
