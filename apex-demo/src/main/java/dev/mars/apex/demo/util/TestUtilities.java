package dev.mars.apex.demo.util;

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


import dev.mars.apex.demo.model.Customer;
import dev.mars.apex.demo.model.Product;
import dev.mars.apex.demo.model.Trade;
import dev.mars.apex.demo.model.FinancialTrade;
import dev.mars.apex.demo.model.CommodityTotalReturnSwap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Test utilities for APEX Rules Engine demonstrations.
 * Provides common test data generation, validation helpers, and utility methods
 * used across multiple demo classes.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class TestUtilities {

    private static final Random random = new Random(42); // Fixed seed for reproducible results

    /**
     * Creates a list of sample customers for testing.
     */
    public static List<Customer> createSampleCustomers() {
        List<Customer> customers = new ArrayList<>();

        // Customer 1: Premium customer
        Customer customer1 = new Customer("John Smith", 35, "john.smith@example.com");
        customer1.setMembershipLevel("Premium");
        customer1.setBalance(50000.0);
        customers.add(customer1);

        // Customer 2: Standard customer
        Customer customer2 = new Customer("Jane Doe", 28, "jane.doe@example.com");
        customer2.setMembershipLevel("Silver");
        customer2.setBalance(75000.0);
        customers.add(customer2);

        // Customer 3: Gold customer
        Customer customer3 = new Customer("Bob Johnson", 45, "bob.johnson@example.com");
        customer3.setMembershipLevel("Gold");
        customer3.setBalance(120000.0);
        customers.add(customer3);

        // Customer 4: Basic customer
        Customer customer4 = new Customer("Alice Brown", 32, "alice.brown@example.com");
        customer4.setMembershipLevel("Basic");
        customer4.setBalance(85000.0);
        customers.add(customer4);

        // Customer 5: Young customer (under 18)
        Customer customer5 = new Customer("Charlie Wilson", 17, "charlie.wilson@example.com");
        customer5.setMembershipLevel("Basic");
        customer5.setBalance(5000.0);
        customers.add(customer5);

        return customers;
    }

    /**
     * Creates a list of sample products for testing.
     */
    public static List<Product> createSampleProducts() {
        List<Product> products = new ArrayList<>();

        products.add(new Product("Premium Savings Account", 1000.00, "SAVINGS"));
        products.add(new Product("Business Checking Account", 5000.00, "CHECKING"));
        products.add(new Product("Investment Portfolio", 25000.00, "INVESTMENT"));
        products.add(new Product("Student Loan", 500.00, "LOAN"));
        products.add(new Product("Credit Card", 0.00, "CREDIT"));

        return products;
    }

    /**
     * Creates a list of sample trades for testing.
     */
    public static List<Trade> createSampleTrades() {
        List<Trade> trades = new ArrayList<>();

        trades.add(new Trade("TRD001", "10000.00", "EQUITY"));
        trades.add(new Trade("TRD002", "25000.00", "BOND"));
        trades.add(new Trade("TRD003", "100000.00", "DERIVATIVE"));
        trades.add(new Trade("TRD004", "50000.00", "COMMODITY"));
        trades.add(new Trade("TRD005", "5000.00", "FX"));

        return trades;
    }

    /**
     * Creates a list of sample financial trades for testing.
     */
    public static List<FinancialTrade> createSampleFinancialTrades() {
        List<FinancialTrade> trades = new ArrayList<>();

        trades.add(new FinancialTrade("FT001", new BigDecimal("15000.00"), "USD", "GOLDMAN_SACHS"));
        trades.add(new FinancialTrade("FT002", new BigDecimal("98500.00"), "USD", "JP_MORGAN"));
        trades.add(new FinancialTrade("FT003", new BigDecimal("108500.00"), "USD", "MORGAN_STANLEY"));
        trades.add(new FinancialTrade("FT004", new BigDecimal("21000.00"), "USD", "BARCLAYS"));
        trades.add(new FinancialTrade("FT005", new BigDecimal("262.50"), "USD", "CITI"));

        return trades;
    }

    /**
     * Creates a list of sample commodity swaps for testing.
     */
    public static List<CommodityTotalReturnSwap> createSampleCommoditySwaps() {
        List<CommodityTotalReturnSwap> swaps = new ArrayList<>();

        swaps.add(new CommodityTotalReturnSwap("SWAP001", "GOLDMAN_SACHS", "CLIENT001",
                                             "WTI_CRUDE", "NYMEX_WTI", new BigDecimal("1000000.00"), "USD",
                                             LocalDate.now(), LocalDate.now().plusMonths(6)));
        swaps.add(new CommodityTotalReturnSwap("SWAP002", "MORGAN_STANLEY", "CLIENT002",
                                             "NATURAL_GAS", "NYMEX_NG", new BigDecimal("500000.00"), "USD",
                                             LocalDate.now(), LocalDate.now().plusMonths(12)));
        swaps.add(new CommodityTotalReturnSwap("SWAP003", "BARCLAYS", "CLIENT003",
                                             "GOLD", "COMEX_GC", new BigDecimal("2000000.00"), "USD",
                                             LocalDate.now(), LocalDate.now().plusMonths(3)));

        return swaps;
    }

    /**
     * Converts an object to a Map for rule evaluation.
     */
    public static Map<String, Object> objectToMap(Object obj) {
        Map<String, Object> map = new HashMap<>();

        if (obj instanceof Customer customer) {
            map.put("name", customer.getName());
            map.put("email", customer.getEmail());
            map.put("age", customer.getAge());
            map.put("membershipLevel", customer.getMembershipLevel());
            map.put("balance", customer.getBalance());
            map.put("preferredCategories", customer.getPreferredCategories());
        } else if (obj instanceof Product product) {
            map.put("name", product.getName());
            map.put("price", product.getPrice());
            map.put("category", product.getCategory());
        } else if (obj instanceof Trade trade) {
            map.put("id", trade.getId());
            map.put("value", trade.getValue());
            map.put("category", trade.getCategory());
        } else if (obj instanceof FinancialTrade financialTrade) {
            map.put("tradeId", financialTrade.getTradeId());
            map.put("amount", financialTrade.getAmount());
            map.put("currency", financialTrade.getCurrency());
            map.put("counterparty", financialTrade.getCounterparty());
            map.put("tradeDate", financialTrade.getTradeDate());
        } else if (obj instanceof CommodityTotalReturnSwap swap) {
            map.put("tradeId", swap.getTradeId());
            map.put("counterpartyId", swap.getCounterpartyId());
            map.put("clientId", swap.getClientId());
            map.put("commodityType", swap.getCommodityType());
            map.put("notionalAmount", swap.getNotionalAmount());
            map.put("notionalCurrency", swap.getNotionalCurrency());
            map.put("maturityDate", swap.getMaturityDate());
        }

        return map;
    }

    /**
     * Generates random test data for performance testing.
     */
    public static List<Map<String, Object>> generateRandomTestData(int count) {
        List<Map<String, Object>> data = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", "TEST" + String.format("%06d", i + 1));
            record.put("amount", new BigDecimal(random.nextDouble() * 1000000));
            record.put("currency", getRandomCurrency());
            record.put("status", getRandomStatus());
            record.put("timestamp", LocalDateTime.now().minusMinutes(random.nextInt(1440)));
            record.put("priority", random.nextInt(10) + 1);
            record.put("region", getRandomRegion());

            data.add(record);
        }

        return data;
    }

    /**
     * Validates that a map contains required fields.
     */
    public static boolean hasRequiredFields(Map<String, Object> data, String... requiredFields) {
        for (String field : requiredFields) {
            if (!data.containsKey(field) || data.get(field) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Prints a formatted test result.
     */
    public static void printTestResult(String testName, boolean passed, String details) {
        String status = passed ? "[PASS]" : "[FAIL]";
        System.out.println(String.format("%-50s %s %s", testName, status, details != null ? details : ""));
    }

    /**
     * Prints a section header for test output.
     */
    public static void printSectionHeader(String title) {
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println(title.toUpperCase());
        System.out.println("=".repeat(60));
    }

    /**
     * Prints a subsection header for test output.
     */
    public static void printSubsectionHeader(String title) {
        System.out.println();
        System.out.println("-".repeat(40));
        System.out.println(title);
        System.out.println("-".repeat(40));
    }

    // Helper methods for random data generation
    private static String getRandomCurrency() {
        String[] currencies = {"USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY"};
        return currencies[random.nextInt(currencies.length)];
    }

    private static String getRandomStatus() {
        String[] statuses = {"ACTIVE", "PENDING", "COMPLETED", "CANCELLED", "SUSPENDED"};
        return statuses[random.nextInt(statuses.length)];
    }

    private static String getRandomRegion() {
        String[] regions = {"AMERICAS", "EMEA", "APAC", "LATAM"};
        return regions[random.nextInt(regions.length)];
    }

    /**
     * Creates a simple performance timer.
     */
    public static class SimpleTimer {
        private long startTime;

        public SimpleTimer() {
            this.startTime = System.currentTimeMillis();
        }

        public long getElapsedMillis() {
            return System.currentTimeMillis() - startTime;
        }

        public void reset() {
            this.startTime = System.currentTimeMillis();
        }

        public void printElapsed(String operation) {
            System.out.println(operation + " completed in " + getElapsedMillis() + "ms");
        }
    }

    /**
     * Simple result collector for test scenarios.
     */
    public static class TestResultCollector {
        private final List<String> results = new ArrayList<>();
        private int passCount = 0;
        private int failCount = 0;

        public void addResult(String testName, boolean passed, String details) {
            String status = passed ? "PASS" : "FAIL";
            results.add(String.format("%-40s [%s] %s", testName, status, details != null ? details : ""));

            if (passed) {
                passCount++;
            } else {
                failCount++;
            }
        }

        public void printSummary() {
            System.out.println();
            System.out.println("Test Results Summary:");
            System.out.println("-".repeat(50));

            for (String result : results) {
                System.out.println(result);
            }

            System.out.println("-".repeat(50));
            System.out.println(String.format("Total: %d, Passed: %d, Failed: %d",
                              passCount + failCount, passCount, failCount));

            if (failCount == 0) {
                System.out.println("All tests passed!");
            } else {
                System.out.println("Some tests failed - review results above.");
            }
        }

        public boolean allTestsPassed() {
            return failCount == 0;
        }

        public int getPassCount() { return passCount; }
        public int getFailCount() { return failCount; }
    }
}