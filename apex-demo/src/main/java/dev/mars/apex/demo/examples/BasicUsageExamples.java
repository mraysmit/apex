package dev.mars.apex.demo.examples;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.demo.model.Customer;
import dev.mars.apex.demo.model.Product;
import dev.mars.apex.demo.model.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * This comprehensive demo covers the essential patterns for using APEX Rules Engine
 * in real-world scenarios. Each section builds upon the previous one, showing
 * progressively more complex validation scenarios.
 *
 * Sections covered:
 * 1. Simple field validations - Basic null checks and range validations
 * 2. Customer validation - Domain object validation patterns
 * 3. Product validation - Business rule enforcement
 * 4. Trade validation - Financial domain examples
 * 5. Numeric operations - Mathematical expressions and calculations
 * 6. Date operations - Temporal logic and date comparisons
 * 7. String operations - Text processing and pattern matching
 *
 * Each section includes:
 * - Step-by-step explanations
 * - Performance timing information
 * - Best practice recommendations
 * - Common pitfalls and how to avoid them
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class BasicUsageExamples {

    private static final Logger logger = LoggerFactory.getLogger(BasicUsageExamples.class);
    private final RulesService rulesService;
    
    public BasicUsageExamples() {
        logger.info("Initializing Basic Usage Examples Demo");
        this.rulesService = new RulesService();
        logger.debug("RulesService initialized for basic usage demonstrations");
    }

    /**
     * Run the complete Basic Usage Examples demonstration.
     *
     * This method orchestrates a comprehensive tour of APEX Rules Engine capabilities,
     * starting with simple validations and progressing to complex business scenarios.
     * Each section is timed and logged for performance analysis.
     */
    public void run() {
        long startTime = System.currentTimeMillis();
        logger.info("Starting Basic Usage Examples demonstration");

        System.out.println("=".repeat(60));
        System.out.println("APEX RULES ENGINE - BASIC USAGE EXAMPLES");
        System.out.println("=".repeat(60));
        System.out.println("This demo covers fundamental patterns for real-world rule usage");
        System.out.println("Estimated duration: 3-5 minutes");
        System.out.println();

        // Execute each demonstration section with timing
        executeTimedSection("Simple Validations", this::demonstrateSimpleValidations);
        executeTimedSection("Customer Validation", this::demonstrateCustomerValidation);
        executeTimedSection("Product Validation", this::demonstrateProductValidation);
        executeTimedSection("Trade Validation", this::demonstrateTradeValidation);
        executeTimedSection("Numeric Operations", this::demonstrateNumericOperations);
        executeTimedSection("Date Operations", this::demonstrateDateOperations);
        executeTimedSection("String Operations", this::demonstrateStringOperations);

        long totalDuration = System.currentTimeMillis() - startTime;
        System.out.println("\n" + "=".repeat(60));
        System.out.println("BASIC USAGE EXAMPLES COMPLETED");
        System.out.println("Total duration: " + totalDuration + "ms");
        System.out.println("You've learned the fundamental patterns for using APEX Rules Engine!");
        System.out.println("=".repeat(60));

        logger.info("Basic Usage Examples demonstration completed in {}ms", totalDuration);
    }

    /**
     * Execute a demonstration section with timing and error handling.
     *
     * @param sectionName The name of the section for logging and display
     * @param section The runnable section to execute
     */
    private void executeTimedSection(String sectionName, Runnable section) {
        long sectionStart = System.currentTimeMillis();
        logger.debug("Starting section: {}", sectionName);

        try {
            section.run();
            long sectionDuration = System.currentTimeMillis() - sectionStart;
            logger.info("Section '{}' completed in {}ms", sectionName, sectionDuration);
            System.out.println("   (Section completed in " + sectionDuration + "ms)");
        } catch (Exception e) {
            logger.error("Error in section '{}': {}", sectionName, e.getMessage(), e);
            System.out.println("   ERROR: " + e.getMessage());
        }

        System.out.println();
    }
    
    /**
     * Demonstrate simple field validations.
     *
     * This section covers the most common validation patterns you'll use in
     * real applications. These examples show how to validate individual fields
     * using simple, readable expressions.
     *
     * Patterns demonstrated:
     * - Required field validation (null and empty checks)
     * - Range validation (numeric bounds)
     * - Format validation (basic pattern matching)
     * - Performance characteristics of simple validations
     */
    private void demonstrateSimpleValidations() {
        System.out.println("1. Simple Field Validations");
        System.out.println("-".repeat(40));
        System.out.println("Foundation patterns for field-level validation");
        System.out.println("Use cases: Form validation, data quality, input sanitization");
        System.out.println();

        // Required field validation
        System.out.println("Example 1: Required field validation");
        long evalStart = System.nanoTime();
        boolean hasName = rulesService.check("#name != null && #name.length() > 0",
                                           Map.of("name", "John Doe"));
        long evalDuration = System.nanoTime() - evalStart;

        System.out.println("   Rule: \"#name != null && #name.length() > 0\"");
        System.out.println("   Data: {name: \"John Doe\"}");
        System.out.println("   Result: " + hasName + " ✓ (evaluated in " + evalDuration/1000 + " μs)");
        logger.debug("Name validation completed in {} nanoseconds", evalDuration);
        System.out.println();

        // Age range validation
        System.out.println("Example 2: Range validation");
        evalStart = System.nanoTime();
        boolean validAge = rulesService.check("#age >= 18 && #age <= 120",
                                            Map.of("age", 25));
        evalDuration = System.nanoTime() - evalStart;

        System.out.println("   Rule: \"#age >= 18 && #age <= 120\"");
        System.out.println("   Data: {age: 25}");
        System.out.println("   Result: " + validAge + " ✓ (evaluated in " + evalDuration/1000 + " μs)");
        logger.debug("Age validation completed in {} nanoseconds", evalDuration);
        System.out.println();

        // Email format validation
        System.out.println("Example 3: Format validation");
        evalStart = System.nanoTime();
        boolean validEmail = rulesService.check("#email != null && #email.contains('@')",
                                              Map.of("email", "user@example.com"));
        evalDuration = System.nanoTime() - evalStart;

        System.out.println("   Rule: \"#email != null && #email.contains('@')\"");
        System.out.println("   Data: {email: \"user@example.com\"}");
        System.out.println("   Result: " + validEmail + " ✓ (evaluated in " + evalDuration/1000 + " μs)");
        logger.debug("Email validation completed in {} nanoseconds", evalDuration);
        System.out.println();

        // Boolean validation
        System.out.println("Example 4: Boolean validation");
        evalStart = System.nanoTime();
        boolean isActive = rulesService.check("#active == true",
                                            Map.of("active", true));
        evalDuration = System.nanoTime() - evalStart;

        System.out.println("   Rule: \"#active == true\"");
        System.out.println("   Data: {active: true}");
        System.out.println("   Result: " + isActive + " ✓ (evaluated in " + evalDuration/1000 + " μs)");
        logger.debug("Boolean validation completed in {} nanoseconds", evalDuration);

        System.out.println("\nKey Points:");
        System.out.println("   • Simple validations typically complete in < 50 microseconds");
        System.out.println("   • Use && and || for combining multiple conditions");
        System.out.println("   • Always check for null before calling methods on objects");
        System.out.println("   • Map-based context is fastest for simple key-value data");
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
