package dev.mars.rulesengine.demo.service.transformers;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.engine.model.TransformerRule;
import dev.mars.rulesengine.core.service.lookup.LookupServiceRegistry;
import dev.mars.rulesengine.core.service.transform.GenericTransformer;
import dev.mars.rulesengine.core.service.transform.GenericTransformerService;
import dev.mars.rulesengine.demo.model.Customer;

import java.util.ArrayList;
import java.util.List;
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
 * Demonstration of how to use the CustomerTransformerDemoConfig class with GenericTransformer.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Demonstration of how to use the CustomerTransformerDemoConfig class with GenericTransformer.
 * This class shows the step-by-step process of creating and using a GenericTransformer
 * for Customer objects using the CustomerTransformerDemoConfig to define transformation rules.
 *
 * This is a demo class with no public constructors or methods except for the main method.
 */
public class CustomerTransformerDemo {
    private static final Logger LOGGER = Logger.getLogger(CustomerTransformerDemo.class.getName());

    private CustomerTransformerDemo() {
        // Private constructor to prevent instantiation
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

        // Step 3: Create a CustomerTransformerDemoConfig
        LOGGER.info("Step 3: Creating a CustomerTransformerDemoConfig");
        CustomerTransformerDemoConfig config = new CustomerTransformerDemoConfig(rulesEngine);

        // Step 4: Create a GenericTransformerService
        LOGGER.info("Step 4: Creating a GenericTransformerService");
        GenericTransformerService transformerService = new GenericTransformerService(registry, rulesEngine);

        // Step 5: Create transformer rules
        LOGGER.info("Step 5: Creating transformer rules");
        List<TransformerRule<Customer>> rules = new ArrayList<>();
        rules.add(config.createGoldMemberRule());
        rules.add(config.createSilverMemberRule());
        rules.add(config.createBasicMemberRule());
        rules.add(config.createYoungCustomerRule());
        rules.add(config.createSeniorCustomerRule());

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
            double discount = config.getDiscountForCustomer(customer);
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
}