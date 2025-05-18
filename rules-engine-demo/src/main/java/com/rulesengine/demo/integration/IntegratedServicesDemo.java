/**
 * This class demonstrates the integration of multiple services in the rules engine.
 * It shows how to use the ValidationService, EnrichmentService, and other services
 * together in a real-world investment portfolio management scenario.
 */
package com.rulesengine.demo.integration;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.service.data.DataServiceManager;
import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import com.rulesengine.core.service.transform.EnrichmentService;
import com.rulesengine.core.service.validation.ValidationService;
import com.rulesengine.demo.data.MockDataSources;
import com.rulesengine.demo.model.Customer;
import com.rulesengine.demo.model.Product;
import com.rulesengine.demo.service.enrichers.CustomerEnricher;
import com.rulesengine.demo.service.enrichers.ProductEnricher;
import com.rulesengine.demo.service.validators.CustomerValidator;
import com.rulesengine.demo.service.validators.ProductValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This demo showcases an investment portfolio management system that:
 * 1. Validates customers and products
 * 2. Enriches customer profiles with recommended investment categories
 * 3. Applies discounts to products based on customer membership level
 * 4. Generates investment recommendations based on customer profile and product availability
 */
public class IntegratedServicesDemo {
    // Services
    private final LookupServiceRegistry registry;
    private final ValidationService validationService;
    private final EnrichmentService enrichmentService;
    private final RulesEngine rulesEngine;
    private final DataServiceManager dataServiceManager;

    /**
     * Constructor with dependency injection.
     * 
     * @param registry The lookup service registry
     * @param validationService The validation service
     * @param enrichmentService The enrichment service
     * @param rulesEngine The rules engine
     * @param dataServiceManager The data service manager
     */
    public IntegratedServicesDemo(
            LookupServiceRegistry registry,
            ValidationService validationService,
            EnrichmentService enrichmentService,
            RulesEngine rulesEngine,
            DataServiceManager dataServiceManager) {
        this.registry = registry;
        this.validationService = validationService;
        this.enrichmentService = enrichmentService;
        this.rulesEngine = rulesEngine;
        this.dataServiceManager = dataServiceManager;
    }

    public static void main(String[] args) {
        // Create registry
        LookupServiceRegistry registry = new LookupServiceRegistry();

        // Create rules engine
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Create services
        ValidationService validationService = new ValidationService(registry, rulesEngine);
        EnrichmentService enrichmentService = new EnrichmentService(registry, rulesEngine);
        DataServiceManager dataServiceManager = new DataServiceManager();
        dataServiceManager.initializeWithMockData();

        // Create demo class
        IntegratedServicesDemo demo = new IntegratedServicesDemo(
            registry, 
            validationService, 
            enrichmentService, 
            rulesEngine,
            dataServiceManager
        );

        // Register services
        demo.registerServices();

        // Run demo
        demo.demonstrateInvestmentPortfolioManagement();
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

    /**
     * Demonstrate an integrated investment portfolio management system.
     */
    public void demonstrateInvestmentPortfolioManagement() {
        System.out.println("\n=== Investment Portfolio Management Demo ===");

        // Step 1: Get customers and products
        List<Customer> customers = createTestCustomers();
        List<Product> availableProducts = MockDataSources.getProducts();

        System.out.println("\nAvailable Products:");
        for (Product product : availableProducts) {
            System.out.println("- " + product.getName() + " ($" + product.getPrice() + ", " + product.getCategory() + ")");
        }

        // Process each customer
        for (Customer customer : customers) {
            System.out.println("\n\n=== Processing Customer: " + customer.getName() + " ===");
            System.out.println("Age: " + customer.getAge() + ", Membership: " + customer.getMembershipLevel());
            System.out.println("Initial Preferred Categories: " + customer.getPreferredCategories());

            // Step 2: Validate customer
            boolean isValidCustomer = validationService.validate("adultValidator", customer);
            if (!isValidCustomer) {
                System.out.println("Customer validation failed. Cannot process this customer.");
                continue;
            }

            // Step 3: Enrich customer profile with recommended investment categories
            Customer enrichedCustomer = (Customer) enrichmentService.enrich("customerEnricher", customer);
            System.out.println("Enriched Preferred Categories: " + enrichedCustomer.getPreferredCategories());

            // Step 4: Find suitable products based on customer preferences
            List<Product> suitableProducts = findSuitableProducts(enrichedCustomer, availableProducts);

            if (suitableProducts.isEmpty()) {
                System.out.println("No suitable products found for this customer.");
                continue;
            }

            System.out.println("\nSuitable Products:");
            for (Product product : suitableProducts) {
                System.out.println("- " + product.getName() + " ($" + product.getPrice() + ", " + product.getCategory() + ")");
            }

            // Step 5: Apply discounts based on customer membership level
            List<Product> discountedProducts = applyDiscounts(enrichedCustomer, suitableProducts);

            System.out.println("\nRecommended Products with Discounts:");
            for (Product product : discountedProducts) {
                System.out.println("- " + product.getName() + " ($" + product.getPrice() + ", " + product.getCategory() + ")");
            }

            // Step 6: Generate final investment recommendations
            generateInvestmentRecommendations(enrichedCustomer, discountedProducts);
        }
    }

    /**
     * Create test customers for the demo.
     * 
     * @return A list of test customers
     */
    private List<Customer> createTestCustomers() {
        List<Customer> customers = new ArrayList<>();

        // Gold member with some preferred categories
        customers.add(new Customer("Alice Smith", 35, "Gold", Arrays.asList("Equity")));

        // Silver member with no preferred categories
        customers.add(new Customer("Bob Johnson", 42, "Silver", new ArrayList<>()));

        // Minor (will fail validation)
        customers.add(new Customer("Charlie Brown", 17, "Basic", Arrays.asList("ETF")));

        // Senior gold member
        customers.add(new Customer("Diana Prince", 68, "Gold", Arrays.asList("FixedIncome")));

        return customers;
    }

    /**
     * Find suitable products for a customer based on their preferences.
     * 
     * @param customer The customer
     * @param availableProducts The available products
     * @return A list of suitable products
     */
    private List<Product> findSuitableProducts(Customer customer, List<Product> availableProducts) {
        List<Product> suitableProducts = new ArrayList<>();

        for (Product product : availableProducts) {
            // Check if the product category is in the customer's preferred categories
            if (customer.getPreferredCategories().contains(product.getCategory())) {
                // Apply additional validation based on membership level
                boolean isValid = true;

                // Gold members can access all products
                if (customer.getMembershipLevel().equals("Gold")) {
                    isValid = true;
                }
                // Silver members can only access products under $1000
                else if (customer.getMembershipLevel().equals("Silver")) {
                    isValid = product.getPrice() < 1000.0;
                }
                // Basic members can only access budget products
                else {
                    isValid = validationService.validate("budgetProductValidator", product);
                }

                if (isValid) {
                    suitableProducts.add(product);
                }
            }
        }

        return suitableProducts;
    }

    /**
     * Apply discounts to products based on customer membership level.
     * 
     * @param customer The customer
     * @param products The products to apply discounts to
     * @return A list of discounted products
     */
    private List<Product> applyDiscounts(Customer customer, List<Product> products) {
        List<Product> discountedProducts = new ArrayList<>();

        // Get customer discount
        CustomerEnricher customerEnricher = (CustomerEnricher) registry.getService("customerEnricher", CustomerEnricher.class);
        double discount = customerEnricher.getDiscountForCustomer(customer);

        for (Product product : products) {
            // Create a rule condition based on product price
            String condition;

            if (product.getPrice() > 1000.0) {
                condition = "#coreData.price > 1000.0";
            } else if (product.getPrice() > 500.0) {
                condition = "#coreData.price > 500.0 && #coreData.price <= 1000.0";
            } else {
                condition = "#coreData.price <= 500.0";
            }

            // Apply conditional enrichment
            Product discountedProduct = (Product) enrichmentService.applyRuleCondition(
                condition, 
                product, 
                discount, 
                "productEnricher"
            );

            discountedProducts.add(discountedProduct);
        }

        return discountedProducts;
    }

    /**
     * Generate investment recommendations for a customer based on their profile and available products.
     * 
     * @param customer The customer
     * @param products The available products
     */
    private void generateInvestmentRecommendations(Customer customer, List<Product> products) {
        System.out.println("\nInvestment Recommendations for " + customer.getName() + ":");

        // Sort products by price (descending)
        products.sort((p1, p2) -> Double.compare(p2.getPrice(), p1.getPrice()));

        // Generate recommendations based on membership level
        if (customer.getMembershipLevel().equals("Gold")) {
            System.out.println("As a Gold member, you qualify for our premium investment portfolio:");

            // Recommend a mix of products
            double totalInvestment = 0.0;
            System.out.println("Recommended Portfolio:");

            for (Product product : products) {
                double allocation = 0.0;

                if (product.getCategory().equals("Equity")) {
                    allocation = 0.4; // 40% allocation to equity
                    System.out.println("- " + product.getName() + " ($" + product.getPrice() + "): 40% allocation");
                } else if (product.getCategory().equals("FixedIncome")) {
                    allocation = 0.35; // 35% allocation to fixed income
                    System.out.println("- " + product.getName() + " ($" + product.getPrice() + "): 35% allocation");
                } else if (product.getCategory().equals("ETF")) {
                    allocation = 0.25; // 25% allocation to ETFs
                    System.out.println("- " + product.getName() + " ($" + product.getPrice() + "): 25% allocation");
                }

                totalInvestment += product.getPrice() * allocation;
            }

            System.out.println("Total Investment: $" + String.format("%.2f", totalInvestment));

        } else if (customer.getMembershipLevel().equals("Silver")) {
            System.out.println("As a Silver member, you qualify for our balanced investment portfolio:");

            // Recommend a mix of products
            double totalInvestment = 0.0;
            System.out.println("Recommended Portfolio:");

            for (Product product : products) {
                double allocation = 0.0;

                if (product.getCategory().equals("Equity")) {
                    allocation = 0.3; // 30% allocation to equity
                    System.out.println("- " + product.getName() + " ($" + product.getPrice() + "): 30% allocation");
                } else if (product.getCategory().equals("FixedIncome")) {
                    allocation = 0.4; // 40% allocation to fixed income
                    System.out.println("- " + product.getName() + " ($" + product.getPrice() + "): 40% allocation");
                } else if (product.getCategory().equals("ETF")) {
                    allocation = 0.3; // 30% allocation to ETFs
                    System.out.println("- " + product.getName() + " ($" + product.getPrice() + "): 30% allocation");
                }

                totalInvestment += product.getPrice() * allocation;
            }

            System.out.println("Total Investment: $" + String.format("%.2f", totalInvestment));

        } else {
            System.out.println("As a Basic member, you qualify for our conservative investment portfolio:");

            // Recommend a mix of products
            double totalInvestment = 0.0;
            System.out.println("Recommended Portfolio:");

            for (Product product : products) {
                double allocation = 0.0;

                if (product.getCategory().equals("Equity")) {
                    allocation = 0.2; // 20% allocation to equity
                    System.out.println("- " + product.getName() + " ($" + product.getPrice() + "): 20% allocation");
                } else if (product.getCategory().equals("FixedIncome")) {
                    allocation = 0.5; // 50% allocation to fixed income
                    System.out.println("- " + product.getName() + " ($" + product.getPrice() + "): 50% allocation");
                } else if (product.getCategory().equals("ETF")) {
                    allocation = 0.3; // 30% allocation to ETFs
                    System.out.println("- " + product.getName() + " ($" + product.getPrice() + "): 30% allocation");
                }

                totalInvestment += product.getPrice() * allocation;
            }

            System.out.println("Total Investment: $" + String.format("%.2f", totalInvestment));
        }
    }
}
