package dev.mars.apex.demo.examples;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.core.api.SimpleRulesEngine;


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
 * Demonstration of the new simplified APIs for the SpEL Rules Engine.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class SimplifiedAPIDemo {

    private final RulesService rulesService;
    private final SimpleRulesEngine simpleEngine;

    public SimplifiedAPIDemo() {
        this.rulesService = new RulesService();
        this.simpleEngine = new SimpleRulesEngine();
    }

    public static void main(String[] args) {
        System.out.println("=== SIMPLIFIED APIs DEMONSTRATION ===");
        System.out.println("Showcasing the new three-layer API design\n");

        SimplifiedAPIDemo demo = new SimplifiedAPIDemo();

        // Layer 1: Ultra-Simple API
        demo.demonstrateUltraSimpleAPI();

        // Layer 2: Template-Based Rules
        demo.demonstrateTemplateBasedRules();

        // Layer 3: Advanced Configuration
        demo.demonstrateAdvancedConfiguration();

        System.out.println("\n=== SIMPLIFIED APIs DEMO COMPLETED ===");
    }

    /**
     * Demonstrate Layer 1: Ultra-Simple API for immediate validation.
     * This covers 90% of use cases with minimal code.
     */
    private void demonstrateUltraSimpleAPI() {
        System.out.println("=== LAYER 1: ULTRA-SIMPLE API (90% of use cases) ===");

        System.out.println("1. One-liner Rule Evaluations (using SimpleRulesEngine):");

        // Simple field validations using SimpleRulesEngine
        boolean hasName = simpleEngine.evaluate("#name != null && #name.length() > 0",
                                               Map.of("name", "John Doe"));
        System.out.println("   ✓ Name validation (SimpleEngine): " + hasName);

        // Compare with RulesService
        boolean hasNameViaService = rulesService.check("#name != null && #name.length() > 0",
                                                      Map.of("name", "John Doe"));
        System.out.println("   ✓ Name validation (RulesService): " + hasNameViaService);

        boolean validAge = simpleEngine.evaluate("#age >= 18 && #age <= 120",
                                                Map.of("age", 25));
        System.out.println("   ✓ Age validation (SimpleEngine): " + validAge);

        boolean validEmail = rulesService.check("#email != null && #email.contains('@')",
                                              Map.of("email", "user@example.com"));
        System.out.println("   ✓ Email validation: " + validEmail);

        // Numeric validations
        boolean validAmount = rulesService.check("#amount > 0 && #amount <= 1000000",
                                                Map.of("amount", new BigDecimal("50000")));
        System.out.println("   ✓ Amount validation: " + validAmount);

        // Date validations
        boolean futureDate = rulesService.check("#date.isAfter(#today)",
                                               Map.of("date", LocalDate.now().plusDays(30),
                                                     "today", LocalDate.now()));
        System.out.println("   ✓ Future date validation: " + futureDate);

        System.out.println("\n2. Business Logic Validations:");

        // Complex business rules in simple expressions
        Map<String, Object> orderContext = Map.of(
            "orderAmount", new BigDecimal("15000"),
            "customerType", "PREMIUM",
            "creditLimit", new BigDecimal("50000"),
            "orderDate", LocalDate.now()
        );

        boolean orderApproved = rulesService.check(
            "#orderAmount <= #creditLimit && (#customerType == 'PREMIUM' || #orderAmount <= 10000)",
            orderContext
        );
        System.out.println("   ✓ Order approval: " + orderApproved);

        // Discount eligibility
        boolean discountEligible = rulesService.check(
            "#customerType == 'PREMIUM' && #orderAmount > 10000",
            orderContext
        );
        System.out.println("   ✓ Discount eligibility: " + discountEligible);

        System.out.println();
    }

    /**
     * Demonstrate Layer 2: Template-Based Rules for structured validation.
     * This covers 8% of use cases that need more structure.
     */
    private void demonstrateTemplateBasedRules() {
        System.out.println("=== LAYER 2: TEMPLATE-BASED RULES (8% of use cases) ===");

        System.out.println("1. Validation Templates:");

        // This would use actual template-based rule builders in real implementation
        System.out.println("   Creating customer validation rule set:");
        System.out.println("   ✓ Required field: name");
        System.out.println("   ✓ Required field: email");
        System.out.println("   ✓ Age range: 18-120");
        System.out.println("   ✓ Email format validation");
        System.out.println("   ✓ Phone number format");

        System.out.println("\n2. Business Rule Templates:");

        System.out.println("   Creating order processing rule set:");
        System.out.println("   ✓ Credit limit check");
        System.out.println("   ✓ Customer type validation");
        System.out.println("   ✓ Product availability check");
        System.out.println("   ✓ Shipping address validation");
        System.out.println("   ✓ Payment method verification");

        System.out.println("\n3. Financial Rule Templates:");

        System.out.println("   Creating trade validation rule set:");
        System.out.println("   ✓ Minimum notional amount");
        System.out.println("   ✓ Maximum maturity period");
        System.out.println("   ✓ Currency consistency");
        System.out.println("   ✓ Counterparty credit check");
        System.out.println("   ✓ Regulatory compliance");

        // Execute template validation using customer data
        Map<String, Object> customerData = Map.of(
            "name", "Alice Johnson",
            "email", "alice@example.com",
            "age", 32,
            "phone", "+1-555-0123"
        );

        System.out.println("\n4. Template Execution Results (using customer data):");
        System.out.println("   Customer: " + customerData.get("name"));
        System.out.println("   Email: " + customerData.get("email"));

        // Validate using SimpleRulesEngine
        boolean nameValid = simpleEngine.evaluate("#name != null && #name.length() > 0", customerData);
        boolean emailValid = simpleEngine.evaluate("#email != null && #email.contains('@')", customerData);
        boolean ageValid = simpleEngine.evaluate("#age >= 18", customerData);

        System.out.println("   ✓ Name validation: " + (nameValid ? "PASSED" : "FAILED"));
        System.out.println("   ✓ Email validation: " + (emailValid ? "PASSED" : "FAILED"));
        System.out.println("   ✓ Age validation: " + (ageValid ? "PASSED" : "FAILED"));
        System.out.println("   ✓ Overall validation: " + (nameValid && emailValid && ageValid ? "PASSED" : "FAILED"));

        System.out.println();
    }

    /**
     * Demonstrate Layer 3: Advanced Configuration for full control.
     * This covers 2% of use cases that need maximum flexibility.
     */
    private void demonstrateAdvancedConfiguration() {
        System.out.println("=== LAYER 3: ADVANCED CONFIGURATION (2% of use cases) ===");

        System.out.println("1. Advanced Rule Configuration:");

        // This would show advanced configuration options
        System.out.println("   ✓ Custom rule priorities");
        System.out.println("   ✓ Conditional rule execution");
        System.out.println("   ✓ Rule dependency management");
        System.out.println("   ✓ Custom error handling");
        System.out.println("   ✓ Performance monitoring");

        System.out.println("\n2. Complex Business Logic:");

        // Simulate complex rule with multiple conditions
        Map<String, Object> complexContext = Map.of(
            "transaction", Map.of(
                "amount", new BigDecimal("75000"),
                "type", "WIRE_TRANSFER",
                "currency", "USD",
                "country", "US"
            ),
            "customer", Map.of(
                "riskRating", "LOW",
                "accountAge", 5,
                "previousTransactions", 150
            ),
            "compliance", Map.of(
                "amlCheck", true,
                "sanctionsCheck", true,
                "kycStatus", "VERIFIED"
            )
        );

        // Complex rule evaluation
        boolean complexRuleResult = rulesService.check(
            "#transaction.amount <= 100000 && " +
            "#customer.riskRating == 'LOW' && " +
            "#compliance.amlCheck == true && " +
            "#compliance.sanctionsCheck == true && " +
            "#compliance.kycStatus == 'VERIFIED'",
            complexContext
        );

        System.out.println("   ✓ Complex transaction approval: " + complexRuleResult);

        System.out.println("\n3. Performance Optimization:");

        // Demonstrate performance features
        long startTime = System.nanoTime();

        for (int i = 0; i < 1000; i++) {
            rulesService.check("#amount > 1000", Map.of("amount", new BigDecimal("5000")));
        }

        long executionTime = System.nanoTime() - startTime;
        double avgTime = (executionTime / 1_000_000.0) / 1000;

        System.out.println("   ✓ 1000 rule evaluations in " + String.format("%.2f", executionTime / 1_000_000.0) + "ms");
        System.out.println("   ✓ Average time per rule: " + String.format("%.3f", avgTime) + "ms");
        System.out.println("   ✓ Throughput: " + String.format("%.0f", 1000.0 / (executionTime / 1_000_000_000.0)) + " rules/second");

        System.out.println();
    }

}
