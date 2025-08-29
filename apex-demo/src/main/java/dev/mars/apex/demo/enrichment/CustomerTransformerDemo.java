package dev.mars.apex.demo.enrichment;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.model.TransformerRule;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.transform.FieldTransformerAction;
import dev.mars.apex.core.service.transform.FieldTransformerActionBuilder;
import dev.mars.apex.core.service.transform.GenericTransformer;
import dev.mars.apex.core.service.transform.GenericTransformerService;
import dev.mars.apex.demo.model.Customer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

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

/**
 * Comprehensive demonstration of customer transformation functionality with GenericTransformer.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Comprehensive demonstration of customer transformation functionality with GenericTransformer.
 * This class shows the step-by-step process of creating and using a GenericTransformer
 * for Customer objects. It combines both the demonstration logic and the configuration
 * functionality in a single self-contained class.
 *
 * This is a demo class with a main method for running the demonstration and instance
 * methods for transformation operations.
 */
public class CustomerTransformerDemo {
    private static final Logger LOGGER = Logger.getLogger(CustomerTransformerDemo.class.getName());

    // Instance fields for transformer configuration
    private final RulesEngine rulesEngine;
    private final Map<String, Double> membershipDiscounts = new HashMap<>();

    /**
     * Private constructor to prevent instantiation.
     * This is a demo class that should only be run via the main method.
     */
    private CustomerTransformerDemo() {
        // Private constructor to prevent instantiation
        this.rulesEngine = null;
    }

    /**
     * Create a new CustomerTransformerDemo with the specified rules engine.
     *
     * @param rulesEngine The rules engine to use for transformation
     */
    private CustomerTransformerDemo(RulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
        initializeMembershipDiscounts();
    }

    /**
     * Initialize membership discounts.
     */
    private void initializeMembershipDiscounts() {
        membershipDiscounts.put("Gold", 0.15);    // 15% discount for Gold members
        membershipDiscounts.put("Silver", 0.10);  // 10% discount for Silver members
        membershipDiscounts.put("Bronze", 0.05);  // 5% discount for Bronze members
        membershipDiscounts.put("Basic", 0.02);   // 2% discount for Basic members
    }

    public static void main(String[] args) {
        // Run the demonstration
        runCustomerTransformationDemo();
    }

    /**
     * Run the customer transformation demonstration.
     * This method shows the step-by-step process of creating and using a GenericTransformer.
     */
    private static void runCustomerTransformationDemo() {
        LOGGER.info("Starting customer transformation demonstration");

        // Step 1: Create a RulesEngine
        LOGGER.info("Step 1: Creating a RulesEngine");
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Step 2: Create a LookupServiceRegistry
        LOGGER.info("Step 2: Creating a LookupServiceRegistry");
        LookupServiceRegistry registry = new LookupServiceRegistry();

        // Step 3: Create a CustomerTransformerDemo instance
        LOGGER.info("Step 3: Creating a CustomerTransformerDemo instance");
        CustomerTransformerDemo demo = new CustomerTransformerDemo(rulesEngine);

        // Step 4: Create a GenericTransformerService
        LOGGER.info("Step 4: Creating a GenericTransformerService");
        GenericTransformerService transformerService = new GenericTransformerService(registry, rulesEngine);

        // Step 5: Create transformer rules
        LOGGER.info("Step 5: Creating transformer rules");
        List<TransformerRule<Customer>> rules = new ArrayList<>();
        rules.add(demo.createGoldMemberRule());
        rules.add(demo.createSilverMemberRule());
        rules.add(demo.createBasicMemberRule());
        rules.add(demo.createYoungCustomerRule());
        rules.add(demo.createSeniorCustomerRule());

        // Step 6: Create a GenericTransformer
        LOGGER.info("Step 6: Creating a GenericTransformer");
        GenericTransformer<Customer> transformer = transformerService.createTransformer(
                "CustomerTransformer", Customer.class, rules);

        // Step 7: Create test customers
        LOGGER.info("Step 7: Creating test customers");
        List<Customer> customers = createTestCustomers();

        // Step 8: Transform each customer and display the results
        LOGGER.info("Step 8: Transforming customers and displaying results");
        for (Customer customer : customers) {
            LOGGER.info("Original customer: " + customer);
            LOGGER.info("Original categories: " + customer.getPreferredCategories());

            // Transform the customer
            Customer transformedCustomer = transformer.transform(customer);

            // Display the transformed customer
            LOGGER.info("Transformed customer: " + transformedCustomer);
            LOGGER.info("Transformed categories: " + transformedCustomer.getPreferredCategories());

            // Get the discount for the customer
            double discount = demo.getDiscountForCustomer(customer);
            LOGGER.info("Discount for " + customer.getName() + ": " + (discount * 100) + "%");

            // Get transformation result
            RuleResult result = transformer.transformWithResult(customer);
            LOGGER.info("Transformation result: " + result);

            // Add a separator
            LOGGER.info("----------------------------------------");
        }

        LOGGER.info("Customer transformation demonstration completed");
    }

    /**
     * Create a list of test customers.
     *
     * @return A list of test customers
     */
    private static List<Customer> createTestCustomers() {
        List<Customer> customers = new ArrayList<>();

        // Create customers with different membership levels and ages
        customers.add(new Customer("Alice", 25, "Gold", new ArrayList<>()));
        customers.add(new Customer("Bob", 35, "Silver", new ArrayList<>()));
        customers.add(new Customer("Charlie", 45, "Bronze", new ArrayList<>()));
        customers.add(new Customer("Diana", 65, "Basic", new ArrayList<>()));

        return customers;
    }

    // ========== Configuration Methods (from CustomerTransformerDemoConfig) ==========

    /**
     * Create a GenericTransformer for Customer objects using the GenericTransformerService.
     *
     * @param name The name of the transformer
     * @param transformerService The GenericTransformerService to use
     * @return A GenericTransformer for Customer objects
     */
    public GenericTransformer<Customer> createCustomerTransformer(String name, GenericTransformerService transformerService) {
        List<TransformerRule<Customer>> rules = new ArrayList<>();

        // Add rules for membership level-based category recommendations
        rules.add(createGoldMemberRule());
        rules.add(createSilverMemberRule());
        rules.add(createBasicMemberRule());

        // Add rules for age-based recommendations
        rules.add(createYoungCustomerRule());
        rules.add(createSeniorCustomerRule());

        // Create and register the transformer
        return transformerService.createTransformer(name, Customer.class, rules);
    }

    /**
     * Create a rule for Gold members.
     * Adds Equity, FixedIncome, and ETF to preferred categories.
     *
     * @return The transformer rule
     */
    public TransformerRule<Customer> createGoldMemberRule() {
        // Create a rule that matches Gold members
        Rule rule = new Rule(
                "GoldMemberRule",
                "#value.membershipLevel == 'Gold'",
                "Add recommended categories for Gold members"
        );

        // Create field transformer actions for adding categories
        List<FieldTransformerAction<Customer>> positiveActions = new ArrayList<>();
        positiveActions.add(createAddCategoryAction("Equity"));
        positiveActions.add(createAddCategoryAction("FixedIncome"));
        positiveActions.add(createAddCategoryAction("ETF"));

        // No negative actions
        List<FieldTransformerAction<Customer>> negativeActions = new ArrayList<>();

        // Create and return the transformer rule
        return new TransformerRule<>(rule, positiveActions, negativeActions);
    }

    /**
     * Create a rule for Silver members.
     * Adds FixedIncome and ETF to preferred categories.
     *
     * @return The transformer rule
     */
    public TransformerRule<Customer> createSilverMemberRule() {
        // Create a rule that matches Silver members
        Rule rule = new Rule(
                "SilverMemberRule",
                "#value.membershipLevel == 'Silver'",
                "Add recommended categories for Silver members"
        );

        // Create field transformer actions for adding categories
        List<FieldTransformerAction<Customer>> positiveActions = new ArrayList<>();
        positiveActions.add(createAddCategoryAction("FixedIncome"));
        positiveActions.add(createAddCategoryAction("ETF"));

        // No negative actions
        List<FieldTransformerAction<Customer>> negativeActions = new ArrayList<>();

        // Create and return the transformer rule
        return new TransformerRule<>(rule, positiveActions, negativeActions);
    }

    /**
     * Create a rule for Basic and Bronze members.
     * Adds ETF to preferred categories.
     *
     * @return The transformer rule
     */
    public TransformerRule<Customer> createBasicMemberRule() {
        // Create a rule that matches Basic or Bronze members
        Rule rule = new Rule(
                "BasicMemberRule",
                "#value.membershipLevel == 'Basic' or #value.membershipLevel == 'Bronze'",
                "Add recommended categories for Basic and Bronze members"
        );

        // Create field transformer actions for adding categories
        List<FieldTransformerAction<Customer>> positiveActions = new ArrayList<>();
        positiveActions.add(createAddCategoryAction("ETF"));

        // No negative actions
        List<FieldTransformerAction<Customer>> negativeActions = new ArrayList<>();

        // Create and return the transformer rule
        return new TransformerRule<>(rule, positiveActions, negativeActions);
    }

    /**
     * Create a rule for young customers (under 30).
     * Adds Equity to preferred categories.
     *
     * @return The transformer rule
     */
    public TransformerRule<Customer> createYoungCustomerRule() {
        // Create a rule that matches young customers
        Rule rule = new Rule(
                "YoungCustomerRule",
                "#value.age < 30",
                "Add recommended categories for young customers"
        );

        // Create field transformer actions for adding categories
        List<FieldTransformerAction<Customer>> positiveActions = new ArrayList<>();
        positiveActions.add(createAddCategoryAction("Equity"));

        // No negative actions
        List<FieldTransformerAction<Customer>> negativeActions = new ArrayList<>();

        // Create and return the transformer rule
        return new TransformerRule<>(rule, positiveActions, negativeActions);
    }

    /**
     * Create a rule for senior customers (60 and older).
     * Adds FixedIncome to preferred categories.
     *
     * @return The transformer rule
     */
    public TransformerRule<Customer> createSeniorCustomerRule() {
        // Create a rule that matches senior customers
        Rule rule = new Rule(
                "SeniorCustomerRule",
                "#value.age >= 60",
                "Add recommended categories for senior customers"
        );

        // Create field transformer actions for adding categories
        List<FieldTransformerAction<Customer>> positiveActions = new ArrayList<>();
        positiveActions.add(createAddCategoryAction("FixedIncome"));

        // No negative actions
        List<FieldTransformerAction<Customer>> negativeActions = new ArrayList<>();

        // Create and return the transformer rule
        return new TransformerRule<>(rule, positiveActions, negativeActions);
    }

    /**
     * Create a field transformer action for adding a category to a customer's preferred categories.
     *
     * @param category The category to add
     * @return The field transformer action
     */
    private FieldTransformerAction<Customer> createAddCategoryAction(String category) {
        LOGGER.info("Creating field transformer action for category: " + category);

        // Create a function to extract the preferred categories
        Function<Customer, Object> extractor = customer -> {
            List<String> categories = customer.getPreferredCategories();
            LOGGER.info("Extracted categories from customer: " + categories);
            return categories;
        };

        // Create a function to transform the preferred categories
        BiFunction<Object, Map<String, Object>, Object> transformer = (preferredCategories, facts) -> {
            @SuppressWarnings("unchecked")
            List<String> categories = new ArrayList<>((List<String>) preferredCategories);
            LOGGER.info("Transforming categories: " + categories);

            // Add the category if it's not already there
            if (!categories.contains(category)) {
                categories.add(category);
                LOGGER.info("Added category " + category + ", new categories: " + categories);
            } else {
                LOGGER.info("Category " + category + " already exists in categories: " + categories);
            }

            return categories;
        };

        // Create a function to set the preferred categories
        BiConsumer<Customer, Object> setter = (customer, preferredCategories) -> {
            @SuppressWarnings("unchecked")
            List<String> categories = (List<String>) preferredCategories;
            LOGGER.info("Setting categories on customer: " + categories);
            customer.setPreferredCategories(categories);
        };

        // Create and return the field transformer action
        return new FieldTransformerActionBuilder<Customer>()
                .withFieldName("preferredCategories")
                .withFieldValueExtractor(extractor)
                .withFieldValueTransformer(transformer)
                .withFieldValueSetter(setter)
                .build();
    }

    /**
     * Get the discount for a customer based on their membership level.
     *
     * @param customer The customer
     * @return The discount as a decimal (e.g., 0.15 for 15%)
     */
    public double getDiscountForCustomer(Customer customer) {
        if (customer == null || customer.getMembershipLevel() == null) {
            return 0.0;
        }

        return membershipDiscounts.getOrDefault(customer.getMembershipLevel(), 0.0);
    }

    /**
     * Get the rules engine used by this demo.
     *
     * @return The rules engine
     */
    public RulesEngine getRulesEngine() {
        return rulesEngine;
    }

    /**
     * Get the membership discounts map.
     *
     * @return The membership discounts map
     */
    public Map<String, Double> getMembershipDiscounts() {
        return new HashMap<>(membershipDiscounts);
    }
}
