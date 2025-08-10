package dev.mars.apex.demo.support;

import dev.mars.apex.demo.model.Customer;
import dev.mars.apex.demo.model.Product;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Support data provider for APEX Rules Engine demonstrations.
 * Provides specialized test data sets and utility methods for demo support.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class DemoDataProvider {

    /**
     * Creates edge case customers for testing boundary conditions.
     */
    public static List<Customer> createEdgeCaseCustomers() {
        List<Customer> customers = new ArrayList<>();

        // Customer with exactly minimum age
        Customer minAge = new Customer("Min Age", 18, "minage@example.com");
        minAge.setMembershipLevel("Basic");
        minAge.setBalance(0.0);
        customers.add(minAge);

        // Customer with very high balance
        Customer highBalance = new Customer("High Balance", 65, "highbalance@example.com");
        highBalance.setMembershipLevel("Premium");
        highBalance.setBalance(10000000.0);
        customers.add(highBalance);

        // Customer with empty email
        Customer emptyEmail = new Customer("Empty Email", 30, "");
        emptyEmail.setMembershipLevel("Basic");
        emptyEmail.setBalance(5000.0);
        customers.add(emptyEmail);

        return customers;
    }

    /**
     * Creates test data for specific rule scenarios.
     */
    public static Map<String, Object> createRuleTestData(String scenario) {
        Map<String, Object> data = new HashMap<>();

        switch (scenario.toLowerCase()) {
            case "high_value":
                data.put("amount", 1000000.0);
                data.put("currency", "USD");
                data.put("riskLevel", "HIGH");
                break;

            case "low_value":
                data.put("amount", 100.0);
                data.put("currency", "USD");
                data.put("riskLevel", "LOW");
                break;

            case "foreign_currency":
                data.put("amount", 50000.0);
                data.put("currency", "EUR");
                data.put("riskLevel", "MEDIUM");
                break;

            case "invalid_data":
                data.put("amount", -1000.0);
                data.put("currency", null);
                data.put("riskLevel", "UNKNOWN");
                break;

            default:
                data.put("amount", 10000.0);
                data.put("currency", "USD");
                data.put("riskLevel", "MEDIUM");
        }

        return data;
    }

    /**
     * Creates test products for specific categories.
     */
    public static List<Product> createCategorySpecificProducts(String category) {
        List<Product> products = new ArrayList<>();

        switch (category.toLowerCase()) {
            case "investment":
                products.add(new Product("Mutual Fund", 10000.0, "Investment"));
                products.add(new Product("ETF Portfolio", 25000.0, "Investment"));
                products.add(new Product("Hedge Fund", 1000000.0, "Investment"));
                break;

            case "savings":
                products.add(new Product("High Yield Savings", 1000.0, "Savings"));
                products.add(new Product("Money Market", 5000.0, "Savings"));
                products.add(new Product("CD Account", 10000.0, "Savings"));
                break;

            case "credit":
                products.add(new Product("Platinum Card", 0.0, "Credit"));
                products.add(new Product("Business Card", 0.0, "Credit"));
                products.add(new Product("Rewards Card", 0.0, "Credit"));
                break;

            default:
                products.add(new Product("Standard Account", 100.0, "Basic"));
        }

        return products;
    }

    /**
     * Creates customers with specific membership levels.
     */
    public static List<Customer> createMembershipLevelCustomers(String level) {
        List<Customer> customers = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            Customer customer = new Customer(level + " Customer " + i, 30 + i,
                                           level.toLowerCase() + i + "@example.com");
            customer.setMembershipLevel(level);
            customer.setBalance(getBalanceForLevel(level) * i);
            customer.setPreferredCategories(getCategoriesForLevel(level));
            customers.add(customer);
        }

        return customers;
    }

    /**
     * Helper method to get balance based on membership level.
     */
    private static double getBalanceForLevel(String level) {
        return switch (level.toLowerCase()) {
            case "premium" -> 100000.0;
            case "gold" -> 50000.0;
            case "silver" -> 25000.0;
            default -> 5000.0;
        };
    }

    /**
     * Helper method to get categories based on membership level.
     */
    private static List<String> getCategoriesForLevel(String level) {
        return switch (level.toLowerCase()) {
            case "premium" -> Arrays.asList("Investment", "Premium Banking", "Concierge");
            case "gold" -> Arrays.asList("Investment", "Premium Banking");
            case "silver" -> Arrays.asList("Savings", "Checking");
            default -> Arrays.asList("Basic Banking");
        };
    }

    /**
     * Creates test data for performance benchmarking.
     */
    public static List<Map<String, Object>> createBenchmarkData(int recordCount) {
        List<Map<String, Object>> data = new ArrayList<>();

        for (int i = 0; i < recordCount; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", "BENCH_" + String.format("%06d", i));
            record.put("value", 1000.0 + (i * 10.0));
            record.put("category", "Category" + (i % 5));
            record.put("active", i % 3 == 0);
            record.put("priority", i % 10);

            data.add(record);
        }

        return data;
    }
}