package dev.mars.apex.demo.examples;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.demo.model.Customer;
import dev.mars.apex.demo.model.Product;
import dev.mars.apex.demo.model.Trade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

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
 * Basic usage examples demonstrating fundamental concepts and simple operations.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class BasicUsageExamples {
    
    private final RulesService rulesService;
    
    public BasicUsageExamples() {
        this.rulesService = new RulesService();
    }

    /**
     * Run the complete Basic Usage Examples demonstration.
     */
    public void run() {
        System.out.println("=== BASIC USAGE EXAMPLES ===");
        
        demonstrateSimpleValidations();
        demonstrateCustomerValidation();
        demonstrateProductValidation();
        demonstrateTradeValidation();
        demonstrateNumericOperations();
        demonstrateDateOperations();
        demonstrateStringOperations();
        
        System.out.println("\n✅ Basic usage examples completed!");
    }
    
    /**
     * Demonstrate simple field validations.
     */
    private void demonstrateSimpleValidations() {
        System.out.println("\n1. Simple Field Validations:");
        
        // Required field validation
        boolean hasName = rulesService.check("#name != null && #name.length() > 0",
                                           Map.of("name", "John Doe"));
        System.out.println("   ✓ Name validation: " + hasName);
        
        // Age range validation
        boolean validAge = rulesService.check("#age >= 18 && #age <= 120",
                                            Map.of("age", 25));
        System.out.println("   ✓ Age validation: " + validAge);
        
        // Email format validation
        boolean validEmail = rulesService.check("#email != null && #email.contains('@')",
                                              Map.of("email", "user@example.com"));
        System.out.println("   ✓ Email validation: " + validEmail);
        
        // Boolean validation
        boolean isActive = rulesService.check("#active == true",
                                            Map.of("active", true));
        System.out.println("   ✓ Active status validation: " + isActive);
    }
    
    /**
     * Demonstrate customer validation using domain objects.
     */
    private void demonstrateCustomerValidation() {
        System.out.println("\n2. Customer Validation:");

        Customer customer = new Customer();
        customer.setName("Alice Johnson");
        customer.setAge(32);
        customer.setMembershipLevel("Gold");
        
        // Customer completeness validation
        boolean isComplete = rulesService.check(
            "#customer.name != null && #customer.age > 0",
            Map.of("customer", customer)
        );
        System.out.println("   ✓ Customer completeness: " + isComplete);

        // Customer eligibility validation
        boolean isEligible = rulesService.check(
            "#customer.age >= 18 && #customer.membershipLevel != null",
            Map.of("customer", customer)
        );
        System.out.println("   ✓ Customer eligibility: " + isEligible);

        // Membership level validation
        boolean isPremium = rulesService.check(
            "#customer.membershipLevel == 'Gold' || #customer.membershipLevel == 'Platinum'",
            Map.of("customer", customer)
        );
        System.out.println("   ✓ Premium membership: " + isPremium);
    }
    
    /**
     * Demonstrate product validation.
     */
    private void demonstrateProductValidation() {
        System.out.println("\n3. Product Validation:");

        Product product = new Product();
        product.setName("Premium Widget");
        product.setPrice(99.99);
        product.setCategory("Electronics");
        
        // Product pricing validation
        boolean validPrice = rulesService.check(
            "#product.price != null && #product.price > 0",
            Map.of("product", product)
        );
        System.out.println("   ✓ Product pricing: " + validPrice);
        
        // Product category validation
        boolean validCategory = rulesService.check(
            "#product.category != null && #product.category.length() > 0",
            Map.of("product", product)
        );
        System.out.println("   ✓ Product category: " + validCategory);
        
        // Premium product validation
        boolean isPremium = rulesService.check(
            "#product.price >= 50 && #product.name.contains('Premium')",
            Map.of("product", product)
        );
        System.out.println("   ✓ Premium product: " + isPremium);
    }
    
    /**
     * Demonstrate trade validation.
     */
    private void demonstrateTradeValidation() {
        System.out.println("\n4. Trade Validation:");

        Trade trade = new Trade();
        trade.setId("TRD001");
        trade.setValue("10000");
        trade.setCategory("Equity");
        
        // Trade value validation
        boolean validValue = rulesService.check(
            "#trade.value != null && #trade.value.length() > 0",
            Map.of("trade", trade)
        );
        System.out.println("   ✓ Trade value: " + validValue);

        // Trade ID validation
        boolean validId = rulesService.check(
            "#trade.id != null && #trade.id.startsWith('TRD')",
            Map.of("trade", trade)
        );
        System.out.println("   ✓ Trade ID format: " + validId);

        // Large trade validation (convert string to number)
        boolean isLargeTrade = rulesService.check(
            "#trade.value != null && T(java.lang.Double).parseDouble(#trade.value) >= 5000",
            Map.of("trade", trade)
        );
        System.out.println("   ✓ Large trade: " + isLargeTrade);
    }
    
    /**
     * Demonstrate numeric operations.
     */
    private void demonstrateNumericOperations() {
        System.out.println("\n5. Numeric Operations:");
        
        Map<String, Object> context = Map.of(
            "amount", new BigDecimal("1000"),
            "rate", 0.05,
            "quantity", 10,
            "price", new BigDecimal("99.99")
        );
        
        // Range validation
        boolean inRange = rulesService.check(
            "#amount >= 100 && #amount <= 10000",
            context
        );
        System.out.println("   ✓ Amount in range: " + inRange);
        
        // Percentage validation
        boolean validRate = rulesService.check(
            "#rate > 0 && #rate <= 1",
            context
        );
        System.out.println("   ✓ Valid rate: " + validRate);
        
        // Calculation validation
        boolean validTotal = rulesService.check(
            "#quantity * #price > 500",
            context
        );
        System.out.println("   ✓ Valid total: " + validTotal);
    }
    
    /**
     * Demonstrate date operations.
     */
    private void demonstrateDateOperations() {
        System.out.println("\n6. Date Operations:");
        
        Map<String, Object> context = Map.of(
            "startDate", LocalDate.now(),
            "endDate", LocalDate.now().plusDays(30),
            "birthDate", LocalDate.of(1990, 5, 15)
        );
        
        // Date range validation
        boolean validRange = rulesService.check(
            "#endDate.isAfter(#startDate)",
            context
        );
        System.out.println("   ✓ Valid date range: " + validRange);
        
        // Future date validation
        boolean isFuture = rulesService.check(
            "#endDate.isAfter(T(java.time.LocalDate).now())",
            context
        );
        System.out.println("   ✓ Future date: " + isFuture);
        
        // Age calculation validation
        boolean isAdult = rulesService.check(
            "#birthDate.isBefore(T(java.time.LocalDate).now().minusYears(18))",
            context
        );
        System.out.println("   ✓ Is adult: " + isAdult);
    }
    
    /**
     * Demonstrate string operations.
     */
    private void demonstrateStringOperations() {
        System.out.println("\n7. String Operations:");
        
        Map<String, Object> context = Map.of(
            "text", "Hello World",
            "email", "user@example.com",
            "code", "ABC123",
            "description", "This is a test description"
        );
        
        // String length validation
        boolean validLength = rulesService.check(
            "#text.length() >= 5 && #text.length() <= 50",
            context
        );
        System.out.println("   ✓ Valid text length: " + validLength);
        
        // Pattern matching
        boolean validEmail = rulesService.check(
            "#email.matches('^[\\\\w.-]+@[\\\\w.-]+\\\\.[a-zA-Z]{2,}$')",
            context
        );
        System.out.println("   ✓ Valid email pattern: " + validEmail);
        
        // Code format validation
        boolean validCode = rulesService.check(
            "#code.matches('^[A-Z]{3}[0-9]{3}$')",
            context
        );
        System.out.println("   ✓ Valid code format: " + validCode);
        
        // Content validation
        boolean hasContent = rulesService.check(
            "#description != null && #description.trim().length() > 10",
            context
        );
        System.out.println("   ✓ Has sufficient content: " + hasContent);
    }
}
