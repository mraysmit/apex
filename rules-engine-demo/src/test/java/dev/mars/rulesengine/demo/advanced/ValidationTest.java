package dev.mars.rulesengine.demo.advanced;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.service.lookup.LookupServiceRegistry;
import dev.mars.rulesengine.core.service.validation.ValidationService;
import dev.mars.rulesengine.core.service.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
 * Test class for validation functionality.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for validation functionality.
 * This class tests validation without depending on demo classes.
 */
public class ValidationTest {
    
    private LookupServiceRegistry registry;
    private ValidationService validationService;
    private RulesEngine rulesEngine;
    
    @BeforeEach
    public void setUp() {
        // Initialize registry and services
        registry = new LookupServiceRegistry();
        rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        validationService = new ValidationService(registry, rulesEngine);
        
        // Register validators
        registerValidators();
    }
    
    /**
     * Register validators with the registry.
     */
    private void registerValidators() {
        // Register customer validators
        registry.registerService(new TestCustomerValidator("adultValidator", 18, 120));
        registry.registerService(new TestCustomerValidator("seniorValidator", 65, 120));
        registry.registerService(new TestCustomerValidator("childValidator", 0, 17));
        
        // Register product validators
        registry.registerService(new TestProductValidator("premiumProductValidator", 500.0, Double.MAX_VALUE));
        registry.registerService(new TestProductValidator("budgetProductValidator", 0.0, 200.0));
        registry.registerService(new TestProductValidator("midRangeProductValidator", 200.0, 500.0));
    }
    
    /**
     * Test basic validation functionality.
     */
    @Test
    public void testBasicValidation() {
        // Create test customers
        TestCustomer adultCustomer = new TestCustomer("John Doe", 35);
        TestCustomer seniorCustomer = new TestCustomer("Jane Smith", 70);
        TestCustomer childCustomer = new TestCustomer("Billy Kid", 12);
        
        // Test adult validation
        boolean isAdult = validationService.validate("adultValidator", adultCustomer);
        boolean isSeniorAdult = validationService.validate("adultValidator", seniorCustomer);
        boolean isChildAdult = validationService.validate("adultValidator", childCustomer);
        
        // Verify results
        assertTrue(isAdult, "Adult customer should be valid for adult validation");
        assertTrue(isSeniorAdult, "Senior customer should be valid for adult validation");
        assertFalse(isChildAdult, "Child customer should not be valid for adult validation");
        
        // Test senior validation
        boolean isSenior = validationService.validate("seniorValidator", seniorCustomer);
        boolean isAdultSenior = validationService.validate("seniorValidator", adultCustomer);
        
        // Verify results
        assertTrue(isSenior, "Senior customer should be valid for senior validation");
        assertFalse(isAdultSenior, "Adult customer should not be valid for senior validation");
        
        // Test child validation
        boolean isChild = validationService.validate("childValidator", childCustomer);
        boolean isAdultChild = validationService.validate("childValidator", adultCustomer);
        
        // Verify results
        assertTrue(isChild, "Child customer should be valid for child validation");
        assertFalse(isAdultChild, "Adult customer should not be valid for child validation");
    }
    
    /**
     * Test validation with RuleResult.
     */
    @Test
    public void testValidationWithRuleResult() {
        // Create test customers
        TestCustomer adultCustomer = new TestCustomer("John Doe", 35);
        TestCustomer childCustomer = new TestCustomer("Billy Kid", 12);
        
        // Test adult validation with RuleResult
        RuleResult adultResult = validationService.validateWithResult("adultValidator", adultCustomer);
        RuleResult childResult = validationService.validateWithResult("adultValidator", childCustomer);
        
        // Verify results
        assertTrue(adultResult.isTriggered(), "Adult customer should be valid for adult validation");
        assertEquals(RuleResult.ResultType.MATCH, adultResult.getResultType(), "Adult result type should be MATCH");
        assertEquals("Validation Rule for adultValidator", adultResult.getRuleName(), "Adult rule name should match");
        
        assertFalse(childResult.isTriggered(), "Child customer should not be valid for adult validation");
        assertEquals(RuleResult.ResultType.NO_MATCH, childResult.getResultType(), "Child result type should be NO_MATCH");
    }
    
    /**
     * Test product validation.
     */
    @Test
    public void testProductValidation() {
        // Create test products
        TestProduct premiumProduct = new TestProduct("Premium Laptop", 1200.0);
        TestProduct midRangeProduct = new TestProduct("Mid-Range Tablet", 350.0);
        TestProduct budgetProduct = new TestProduct("Budget Phone", 150.0);
        
        // Test premium product validation
        boolean isPremium = validationService.validate("premiumProductValidator", premiumProduct);
        boolean isMidRangePremium = validationService.validate("premiumProductValidator", midRangeProduct);
        boolean isBudgetPremium = validationService.validate("premiumProductValidator", budgetProduct);
        
        // Verify results
        assertTrue(isPremium, "Premium product should be valid for premium validation");
        assertFalse(isMidRangePremium, "Mid-range product should not be valid for premium validation");
        assertFalse(isBudgetPremium, "Budget product should not be valid for premium validation");
        
        // Test mid-range product validation
        boolean isMidRange = validationService.validate("midRangeProductValidator", midRangeProduct);
        boolean isPremiumMidRange = validationService.validate("midRangeProductValidator", premiumProduct);
        boolean isBudgetMidRange = validationService.validate("midRangeProductValidator", budgetProduct);
        
        // Verify results
        assertTrue(isMidRange, "Mid-range product should be valid for mid-range validation");
        assertFalse(isPremiumMidRange, "Premium product should not be valid for mid-range validation");
        assertFalse(isBudgetMidRange, "Budget product should not be valid for mid-range validation");
        
        // Test budget product validation
        boolean isBudget = validationService.validate("budgetProductValidator", budgetProduct);
        boolean isPremiumBudget = validationService.validate("budgetProductValidator", premiumProduct);
        boolean isMidRangeBudget = validationService.validate("budgetProductValidator", midRangeProduct);
        
        // Verify results
        assertTrue(isBudget, "Budget product should be valid for budget validation");
        assertFalse(isPremiumBudget, "Premium product should not be valid for budget validation");
        assertFalse(isMidRangeBudget, "Mid-range product should not be valid for budget validation");
    }
    
    /**
     * Test validation with non-existent validation.
     */
    @Test
    public void testNonExistentValidator() {
        // Create test customer
        TestCustomer customer = new TestCustomer("John Doe", 35);
        
        // Test non-existent validation
        RuleResult result = validationService.validateWithResult("nonExistentValidator", customer);
        
        // Verify result
        assertFalse(result.isTriggered(), "Non-existent validation should not be triggered");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), "Result type should be ERROR");
        assertTrue(result.getMessage().contains("not found"), "Message should indicate validation not found");
    }
    
    /**
     * Test validation with type mismatch.
     */
    @Test
    public void testTypeMismatch() {
        // Create test customer and product
        TestCustomer customer = new TestCustomer("John Doe", 35);
        TestProduct product = new TestProduct("Test Product", 100.0);
        
        // Test type mismatch
        RuleResult result1 = validationService.validateWithResult("adultValidator", product);
        RuleResult result2 = validationService.validateWithResult("premiumProductValidator", customer);
        
        // Verify results
        assertFalse(result1.isTriggered(), "Type mismatch should not be triggered");
        assertEquals(RuleResult.ResultType.ERROR, result1.getResultType(), "Result type should be ERROR");
        assertTrue(result1.getMessage().contains("cannot handle type"), "Message should indicate type mismatch");
        
        assertFalse(result2.isTriggered(), "Type mismatch should not be triggered");
        assertEquals(RuleResult.ResultType.ERROR, result2.getResultType(), "Result type should be ERROR");
        assertTrue(result2.getMessage().contains("cannot handle type"), "Message should indicate type mismatch");
    }
    
    /**
     * Test complex validation scenarios.
     */
    @Test
    public void testComplexValidation() {
        // Create test customer and product
        TestCustomer adultCustomer = new TestCustomer("John Doe", 35);
        TestProduct premiumProduct = new TestProduct("Premium Laptop", 1200.0);
        
        // Test adult customer buying premium product
        boolean isAdult = validationService.validate("adultValidator", adultCustomer);
        boolean isPremium = validationService.validate("premiumProductValidator", premiumProduct);
        
        // Verify results
        assertTrue(isAdult && isPremium, "Adult customer should be able to buy premium product");
        
        // Create test customer and product
        TestCustomer childCustomer = new TestCustomer("Billy Kid", 12);
        TestProduct budgetProduct = new TestProduct("Budget Phone", 150.0);
        
        // Test child customer buying budget product
        boolean isChild = validationService.validate("childValidator", childCustomer);
        boolean isBudget = validationService.validate("budgetProductValidator", budgetProduct);
        
        // Verify results
        assertTrue(isChild && isBudget, "Child customer should be able to buy budget product");
        
        // Test child customer buying premium product
        boolean isChildBuyingPremium = validationService.validate("childValidator", childCustomer) &&
                                      validationService.validate("premiumProductValidator", premiumProduct);
        
        // Verify results
        assertTrue(isChildBuyingPremium, "Child customer should be able to buy premium product (no business rule against it)");
    }
    
    /**
     * Simple customer validation for testing.
     */
    private static class TestCustomerValidator implements Validator<TestCustomer> {
        private final String name;
        private final int minAge;
        private final int maxAge;
        
        public TestCustomerValidator(String name, int minAge, int maxAge) {
            this.name = name;
            this.minAge = minAge;
            this.maxAge = maxAge;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public boolean validate(TestCustomer value) {
            return value != null && value.getAge() >= minAge && value.getAge() <= maxAge;
        }
        
        @Override
        public RuleResult validateWithResult(TestCustomer value) {
            boolean isValid = validate(value);
            if (isValid) {
                return RuleResult.match(getName(), "Customer validation successful");
            } else {
                return RuleResult.noMatch();
            }
        }
        
        @Override
        public Class<TestCustomer> getType() {
            return TestCustomer.class;
        }
    }
    
    /**
     * Simple product validation for testing.
     */
    private static class TestProductValidator implements Validator<TestProduct> {
        private final String name;
        private final double minPrice;
        private final double maxPrice;
        
        public TestProductValidator(String name, double minPrice, double maxPrice) {
            this.name = name;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public boolean validate(TestProduct value) {
            return value != null && value.getPrice() >= minPrice && value.getPrice() <= maxPrice;
        }
        
        @Override
        public RuleResult validateWithResult(TestProduct value) {
            boolean isValid = validate(value);
            if (isValid) {
                return RuleResult.match(getName(), "Product validation successful");
            } else {
                return RuleResult.noMatch();
            }
        }
        
        @Override
        public Class<TestProduct> getType() {
            return TestProduct.class;
        }
    }
    
    /**
     * Simple customer class for testing.
     */
    public static class TestCustomer {
        private String name;
        private int age;
        
        public TestCustomer(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        public String getName() {
            return name;
        }
        
        public int getAge() {
            return age;
        }
    }
    
    /**
     * Simple product class for testing.
     */
    public static class TestProduct {
        private String name;
        private double price;
        
        public TestProduct(String name, double price) {
            this.name = name;
            this.price = price;
        }
        
        public String getName() {
            return name;
        }
        
        public double getPrice() {
            return price;
        }
    }
}