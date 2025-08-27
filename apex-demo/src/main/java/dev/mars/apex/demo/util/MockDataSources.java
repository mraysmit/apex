package dev.mars.apex.demo.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock data sources for APEX Rules Engine demonstrations.
 * Simulates external data sources like databases, web services, and file systems
 * for demo purposes without requiring actual external dependencies.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class MockDataSources {

    // Simulated database tables
    private static final Map<String, Map<String, Object>> CUSTOMER_TABLE = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> PRODUCT_TABLE = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> REFERENCE_DATA_TABLE = new ConcurrentHashMap<>();

    static {
        initializeMockData();
    }

    /**
     * Initialize mock data sources with sample data.
     */
    private static void initializeMockData() {
        // Customer reference data
        addCustomerRecord("CUST001", "John Smith", "Premium", "US", "john.smith@example.com");
        addCustomerRecord("CUST002", "Jane Doe", "Gold", "UK", "jane.doe@example.com");
        addCustomerRecord("CUST003", "Bob Johnson", "Silver", "CA", "bob.johnson@example.com");
        addCustomerRecord("CUST004", "Alice Brown", "Basic", "AU", "alice.brown@example.com");

        // Product reference data
        addProductRecord("PROD001", "Premium Investment", "Investment", 100000.0, true);
        addProductRecord("PROD002", "Gold Savings", "Savings", 10000.0, true);
        addProductRecord("PROD003", "Silver Checking", "Checking", 1000.0, true);
        addProductRecord("PROD004", "Basic Account", "Basic", 100.0, true);
        addProductRecord("PROD005", "Discontinued Product", "Legacy", 0.0, false);

        // Reference data (currencies, countries, etc.)
        addReferenceRecord("CURRENCY", "USD", "US Dollar", "United States", true);
        addReferenceRecord("CURRENCY", "EUR", "Euro", "European Union", true);
        addReferenceRecord("CURRENCY", "GBP", "British Pound", "United Kingdom", true);
        addReferenceRecord("CURRENCY", "JPY", "Japanese Yen", "Japan", true);
        addReferenceRecord("CURRENCY", "CAD", "Canadian Dollar", "Canada", true);

        addReferenceRecord("COUNTRY", "US", "United States", "Americas", true);
        addReferenceRecord("COUNTRY", "UK", "United Kingdom", "EMEA", true);
        addReferenceRecord("COUNTRY", "CA", "Canada", "Americas", true);
        addReferenceRecord("COUNTRY", "AU", "Australia", "APAC", true);
        addReferenceRecord("COUNTRY", "JP", "Japan", "APAC", true);
    }

    /**
     * Simulates a database lookup for customer information.
     */
    public static Map<String, Object> lookupCustomer(String customerId) {
        return CUSTOMER_TABLE.get(customerId);
    }

    /**
     * Simulates a database lookup for product information.
     */
    public static Map<String, Object> lookupProduct(String productId) {
        return PRODUCT_TABLE.get(productId);
    }

    /**
     * Simulates a reference data lookup.
     */
    public static Map<String, Object> lookupReferenceData(String type, String code) {
        String key = type + ":" + code;
        return REFERENCE_DATA_TABLE.get(key);
    }

    /**
     * Simulates a web service call with response time.
     */
    public static Map<String, Object> simulateWebServiceCall(String endpoint, Map<String, Object> request) {
        // Simulate network delay
        try {
            Thread.sleep(50 + (int)(Math.random() * 100)); // 50-150ms delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("timestamp", System.currentTimeMillis());
        response.put("endpoint", endpoint);
        response.put("requestId", "REQ_" + System.currentTimeMillis());

        // Simulate different responses based on endpoint
        switch (endpoint.toLowerCase()) {
            case "credit_check":
                response.put("creditScore", 700 + (int)(Math.random() * 150));
                response.put("creditRating", "GOOD");
                break;
            case "risk_assessment":
                response.put("riskScore", (int)(Math.random() * 100));
                response.put("riskLevel", "MEDIUM");
                break;
            case "compliance_check":
                response.put("compliant", Math.random() > 0.1); // 90% compliance rate
                response.put("violations", new String[0]);
                break;
            default:
                response.put("data", "Mock response for " + endpoint);
        }

        return response;
    }

    /**
     * Simulates file system access.
     */
    public static String simulateFileRead(String filePath) {
        // Simulate file read delay
        try {
            Thread.sleep(10 + (int)(Math.random() * 20)); // 10-30ms delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Return mock file content based on file type
        if (filePath.endsWith(".json")) {
            return "{\"mockData\": true, \"source\": \"" + filePath + "\"}";
        } else if (filePath.endsWith(".xml")) {
            return "<?xml version=\"1.0\"?><mockData source=\"" + filePath + "\">true</mockData>";
        } else if (filePath.endsWith(".yaml") || filePath.endsWith(".yml")) {
            return "mockData: true\nsource: \"" + filePath + "\"";
        } else {
            return "Mock content for " + filePath;
        }
    }

    // Helper methods for adding mock data
    private static void addCustomerRecord(String id, String name, String level, String country, String email) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("name", name);
        record.put("membershipLevel", level);
        record.put("country", country);
        record.put("email", email);
        record.put("active", true);
        CUSTOMER_TABLE.put(id, record);
    }

    private static void addProductRecord(String id, String name, String category, double minBalance, boolean active) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("name", name);
        record.put("category", category);
        record.put("minBalance", minBalance);
        record.put("active", active);
        PRODUCT_TABLE.put(id, record);
    }

    private static void addReferenceRecord(String type, String code, String name, String region, boolean active) {
        Map<String, Object> record = new HashMap<>();
        record.put("type", type);
        record.put("code", code);
        record.put("name", name);
        record.put("region", region);
        record.put("active", active);
        REFERENCE_DATA_TABLE.put(type + ":" + code, record);
    }

    /**
     * Clears all mock data (useful for testing).
     */
    public static void clearAllData() {
        CUSTOMER_TABLE.clear();
        PRODUCT_TABLE.clear();
        REFERENCE_DATA_TABLE.clear();
    }

    /**
     * Resets mock data to initial state.
     */
    public static void resetToDefaults() {
        clearAllData();
        initializeMockData();
    }

    /**
     * Gets statistics about mock data sources.
     */
    public static Map<String, Integer> getDataSourceStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("customers", CUSTOMER_TABLE.size());
        stats.put("products", PRODUCT_TABLE.size());
        stats.put("referenceData", REFERENCE_DATA_TABLE.size());
        return stats;
    }
}