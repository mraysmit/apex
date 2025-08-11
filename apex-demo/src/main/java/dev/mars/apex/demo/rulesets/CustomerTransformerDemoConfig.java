package dev.mars.apex.demo.rulesets;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.TransformerRule;
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
 * Configuration factory for Customer transformers.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Configuration factory for Customer transformers.
 * This class creates configurations for the functionality of the CustomerTransformerDemo class
 * using the GenericTransformer for processing.
 */
public class CustomerTransformerDemoConfig {
    private static final Logger LOGGER = Logger.getLogger(CustomerTransformerDemoConfig.class.getName());
    private final RulesEngine rulesEngine;
    private final Map<String, Double> membershipDiscounts = new HashMap<>();

    /**
     * Create a new CustomerTransformerDemoConfig with the specified rules engine.
     *
     * @param rulesEngine The rules engine to use for transformation
     */
    public CustomerTransformerDemoConfig(RulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;

        // Initialize membership discounts
        membershipDiscounts.put("Gold", 0.15);    // 15% discount for Gold members
        membershipDiscounts.put("Silver", 0.10);  // 10% discount for Silver members
        membershipDiscounts.put("Bronze", 0.05);  // 5% discount for Bronze members
        membershipDiscounts.put("Basic", 0.02);   // 2% discount for Basic members
    }

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
     * Get the rules engine used by this factory.
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
