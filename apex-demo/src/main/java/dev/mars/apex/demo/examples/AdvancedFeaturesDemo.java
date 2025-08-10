package dev.mars.apex.demo.examples;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.demo.model.Customer;
import dev.mars.apex.demo.model.FinancialTrade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

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
 * Advanced Features Demo - Consolidated advanced SpEL and rules engine features.
 *
 * This demo consolidates functionality from:
 * - ApexAdvancedFeaturesDemo
 * - DynamicMethodExecutionDemo
 * - CollectionOperationsDemo
 * - TemplateProcessingDemo
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class AdvancedFeaturesDemo {

    private final RulesService rulesService;

    public AdvancedFeaturesDemo() {
        this.rulesService = new RulesService();
    }

    /**
     * Run the complete Advanced Features demonstration.
     */
    public void run() {
        System.out.println(" Advanced SpEL Features Demo");
        System.out.println("=" .repeat(50));
        System.out.println("Exploring advanced capabilities of the SpEL Rules Engine");
        System.out.println();

        demonstrateCollectionOperations();
        System.out.println();

        demonstrateDynamicMethodExecution();
        System.out.println();

        demonstrateTemplateProcessing();
        System.out.println();

        demonstrateAdvancedExpressions();
        System.out.println();

        System.out.println("PASSED Advanced Features demonstration completed!");
    }

    /**
     * Demonstrate collection operations with SpEL.
     */
    private void demonstrateCollectionOperations() {
        System.out.println(" Collection Operations");
        System.out.println("-".repeat(30));

        // Create sample data
        List<FinancialTrade> trades = Arrays.asList(
            createTrade("T001", new BigDecimal("100000"), "USD"),
            createTrade("T002", new BigDecimal("250000"), "EUR"),
            createTrade("T003", new BigDecimal("75000"), "GBP"),
            createTrade("T004", new BigDecimal("500000"), "USD")
        );

        Map<String, Object> context = new HashMap<>();
        context.put("trades", trades);

        // Collection filtering
        boolean hasLargeTrades = rulesService.check(
            "#trades.?[amount > 200000].size() > 0",
            context
        );
        System.out.println("Has large trades (>200k): " + hasLargeTrades);

        // Collection projection
        boolean hasUSDTrades = rulesService.check(
            "#trades.![currency].contains('USD')",
            context
        );
        System.out.println("Has USD trades: " + hasUSDTrades);

        // Collection aggregation
        boolean totalExceedsLimit = rulesService.check(
            "#trades.stream().mapToDouble(t -> t.amount.doubleValue()).sum() > 800000",
            context
        );
        System.out.println("Total exceeds 800k limit: " + totalExceedsLimit);
    }

    /**
     * Demonstrate dynamic method execution.
     */
    private void demonstrateDynamicMethodExecution() {
        System.out.println("⚡ Dynamic Method Execution");
        System.out.println("-".repeat(30));

        Customer customer = new Customer();
        customer.setName("John Doe");
        customer.setAge(35);
        customer.setEmail("john.doe@example.com");

        // Dynamic method calls
        boolean nameValid = rulesService.check(
            "#data.getName().length() > 0 && #data.getName().matches('[A-Za-z ]++')",
            customer
        );
        System.out.println("Name validation (dynamic): " + nameValid);

        // Method chaining
        boolean emailDomainValid = rulesService.check(
            "#data.getEmail().toLowerCase().endsWith('.com')",
            customer
        );
        System.out.println("Email domain validation: " + emailDomainValid);

        // Conditional method execution
        boolean ageGroupValid = rulesService.check(
            "#data.getAge() >= 18 ? #data.getAge() <= 65 : false",
            customer
        );
        System.out.println("Age group validation: " + ageGroupValid);
    }

    /**
     * Demonstrate template processing capabilities.
     */
    private void demonstrateTemplateProcessing() {
        System.out.println("Template Processing");
        System.out.println("-".repeat(30));

        Map<String, Object> context = new HashMap<>();
        context.put("customerName", "Alice Smith");
        context.put("accountBalance", new BigDecimal("15000.50"));
        context.put("accountType", "PREMIUM");
        context.put("lastLoginDays", 5);

        // Template-based validation
        boolean premiumAccountValid = rulesService.check(
            "#accountType == 'PREMIUM' && #accountBalance > 10000",
            context
        );
        System.out.println("Premium account validation: " + premiumAccountValid);

        // Complex template logic
        boolean accountStatusGood = rulesService.check(
            "#lastLoginDays <= 30 && (#accountType == 'PREMIUM' ? #accountBalance > 5000 : #accountBalance > 1000)",
            context
        );
        System.out.println("Account status validation: " + accountStatusGood);

        // String template processing
        boolean nameFormatValid = rulesService.check(
            "#customerName.matches('[A-Z][a-z]+ [A-Z][a-z]+')",
            context
        );
        System.out.println("Name format validation: " + nameFormatValid);
    }

    /**
     * Demonstrate advanced SpEL expressions.
     */
    private void demonstrateAdvancedExpressions() {
        System.out.println("Advanced Expressions");
        System.out.println("-".repeat(30));

        Map<String, Object> context = new HashMap<>();
        context.put("riskScore", 75);
        context.put("creditRating", "A");
        context.put("transactionAmount", new BigDecimal("50000"));
        context.put("customerTier", "GOLD");
        context.put("regionCode", "US");

        // Complex conditional logic
        boolean riskAssessmentPassed = rulesService.check(
            "(#riskScore <= 80 && #creditRating matches '[A-B]') || " +
            "(#customerTier == 'GOLD' && #riskScore <= 90)",
            context
        );
        System.out.println("Risk assessment passed: " + riskAssessmentPassed);

        // Mathematical expressions
        boolean transactionLimitValid = rulesService.check(
            "#transactionAmount <= (#customerTier == 'GOLD' ? 100000 : " +
            "#customerTier == 'SILVER' ? 50000 : 25000)",
            context
        );
        System.out.println("Transaction limit valid: " + transactionLimitValid);

        // Regular expression matching
        boolean regionCodeValid = rulesService.check(
            "#regionCode matches '[A-Z]{2}'",
            context
        );
        System.out.println("Region code format valid: " + regionCodeValid);

        // Null-safe operations
        context.put("optionalField", null);
        boolean nullSafeCheck = rulesService.check(
            "#optionalField?.toString()?.length() > 0 ?: false",
            context
        );
        System.out.println("Null-safe validation: " + nullSafeCheck);
    }

    /**
     * Create a sample trade for testing.
     */
    private FinancialTrade createTrade(String id, BigDecimal amount, String currency) {
        FinancialTrade trade = new FinancialTrade();
        trade.setTradeId(id);
        trade.setAmount(amount);
        trade.setCurrency(currency);
        trade.setTradeDate(LocalDate.now());
        return trade;
    }

    /**
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        new AdvancedFeaturesDemo().run();
    }
}



