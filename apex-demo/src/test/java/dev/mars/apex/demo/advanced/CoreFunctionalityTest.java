package dev.mars.apex.demo.advanced;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.transform.GenericTransformerService;
import dev.mars.apex.core.service.validation.ValidationService;
import dev.mars.apex.demo.model.Customer;
import dev.mars.apex.demo.model.Product;
import dev.mars.apex.core.service.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
 * Test class for core functionality of the rules engine.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for core functionality of the rules engine.
 * This class tests the core features directly without using demo classes.
 */
public class CoreFunctionalityTest {
    private LookupServiceRegistry registry;
    private ValidationService validationService;
    private GenericTransformerService transformerService;
    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() {
        // Create registry
        registry = new LookupServiceRegistry();

        // Create rules engine
        rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Create services
        validationService = new ValidationService(registry, rulesEngine);
        transformerService = new GenericTransformerService(registry, rulesEngine);
    }

    /**
     * Test basic validation functionality.
     */
    @Test
    public void testValidation() {
        // Create a simple validation
        Validator<Customer> ageValidator = new Validator<Customer>() {
            @Override
            public String getName() {
                return "ageValidator";
            }

            @Override
            public boolean validate(Customer value) {
                return value != null && value.getAge() >= 18;
            }

            @Override
            public Class<Customer> getType() {
                return Customer.class;
            }
        };

        // Register the validation
        registry.registerService(ageValidator);

        // Create test customers
        Customer validCustomer = new Customer("Alice Smith", 35, "Gold", Arrays.asList("Equity"));
        Customer invalidCustomer = new Customer("Charlie Brown", 17, "Basic", Arrays.asList("ETF"));

        // Test validation
        boolean isValidCustomer = validationService.validate("ageValidator", validCustomer);
        boolean isInvalidCustomer = validationService.validate("ageValidator", invalidCustomer);

        // Verify results
        assertTrue(isValidCustomer, "Adult customer should be valid");
        assertFalse(isInvalidCustomer, "Minor customer should be invalid");
    }

    /**
     * Test rule engine functionality.
     */
    @Test
    public void testRuleEngineSimple() {
        // Create a simple rule
        Rule rule = new Rule(
            "AgeRule",
            "#customer.age >= 18",
            "Customer is an adult"
        );

        // Create test customers
        Customer adultCustomer = new Customer("Alice Smith", 35, "Gold", Arrays.asList("Equity"));
        Customer youngCustomer = new Customer("Charlie Brown", 17, "Basic", Arrays.asList("ETF"));

        // Create facts for rule evaluation
        Map<String, Object> adultCustomerFacts = new HashMap<>();
        adultCustomerFacts.put("customer", adultCustomer);

        Map<String, Object> youngCustomerFacts = new HashMap<>();
        youngCustomerFacts.put("customer", youngCustomer);

        // Evaluate rule for adult customer
        List<Rule> rules = Arrays.asList(rule);
        RuleResult adultCustomerResult = rulesEngine.executeRulesList(rules, adultCustomerFacts);

        // Evaluate rule for minor customer
        RuleResult youngCustomerResult = rulesEngine.executeRulesList(rules, youngCustomerFacts);

        // Verify results
        assertTrue(adultCustomerResult.isTriggered(), "Adult customer should pass the age rule");
        assertFalse(youngCustomerResult.isTriggered(), "Under age customer should fail the age rule");
    }

    /**
     * Test product price calculation.
     */
    @Test
    public void testProductPriceCalculation() {
        // Create a rule with a Boolean condition
        Rule discountRule = new Rule(
            "DiscountRule",
            "#product.price > 0",  // Boolean condition that will evaluate to true
            "Product is eligible for discount"
        );

        // Create a test product
        Product product = new Product("Test Product", 100.0, "Test");

        // Create facts for rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("product", product);
        facts.put("discountRate", 0.1); // 10% discount

        // Evaluate rule
        List<Rule> rules = Arrays.asList(discountRule);
        RuleResult result = rulesEngine.executeRulesList(rules, facts);

        // Verify rule triggered
        assertTrue(result.isTriggered(), "Discount rule should be triggered");

        // Use transformer service to calculate the discounted price
        Map<String, Object> transformationContext = new HashMap<>();
        transformationContext.put("originalPrice", product.getPrice());
        transformationContext.put("discountRate", 0.1);

        // Apply price transformation using transformer service
        Object transformedPrice = transformerService.transform(
            "#originalPrice * (1 - #discountRate)",
            transformationContext
        );

        // Verify the transformation
        assertNotNull(transformedPrice, "Transformed price should not be null");
        assertTrue(transformedPrice instanceof Number, "Transformed price should be a number");
        double discountedPrice = ((Number) transformedPrice).doubleValue();
        assertEquals(90.0, discountedPrice, 0.01, "Discounted price should be 90.0");
    }
}
