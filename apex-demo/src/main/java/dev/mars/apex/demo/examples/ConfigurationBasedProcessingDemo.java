package dev.mars.apex.demo.examples;

import dev.mars.apex.core.api.RulesService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simplified demonstration of configuration-based JSON/XML processing.
 * 
 * This example shows how to:
 * 1. Use RulesService for validation
 * 2. Apply manual enrichment logic
 * 3. Generate processing reports
 * 4. Handle different data sources consistently
 * 
 * @author APEX Demo Team
 * @since 1.0.0
 */
public class ConfigurationBasedProcessingDemo {

    private final RulesService rulesService;

    public ConfigurationBasedProcessingDemo() throws Exception {
        // Initialize APEX services
        this.rulesService = new RulesService();

        // Setup validation rules
        setupValidationRules();

        System.out.println(" Initialized configuration-based processing demo");
        System.out.println("   Using RulesService for validation");
        System.out.println("   Manual enrichment logic configured");
    }
    
    /**
     * Setup validation rules.
     */
    private void setupValidationRules() {
        rulesService.define("requiredFields", "#customerId != null && #firstName != null && #lastName != null");
        rulesService.define("adult", "#age >= 18");
        rulesService.define("validEmail", "#email != null && #email.contains('@')");
        rulesService.define("hasBalance", "#accountBalance != null && #accountBalance > 0");
        rulesService.define("activeStatus", "#status == 'ACTIVE' || #status == 'PENDING'");
    }
    
    /**
     * Main demonstration method.
     */
    public static void main(String[] args) {
        try {
            ConfigurationBasedProcessingDemo demo = new ConfigurationBasedProcessingDemo();
            
            System.out.println("\n APEX Configuration-Based Processing Demo");
            System.out.println("=" .repeat(55));
            
            demo.demonstrateBasicProcessing();
            demo.demonstrateEnrichmentLogic();
            demo.demonstrateBatchValidation();
            
            System.out.println("\n Configuration-based processing demonstration completed!");
            
        } catch (Exception e) {
            System.err.println(" Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrate basic processing with validation.
     */
    private void demonstrateBasicProcessing() {
        System.out.println("\n 1. Basic Processing with Validation");
        System.out.println("-".repeat(40));
        
        Map<String, Object> customer = createSampleCustomer();
        System.out.println("Processing customer: " + customer.get("customerId"));
        
        // Apply validation rules
        boolean hasRequired = rulesService.test("requiredFields", customer);
        boolean isAdult = rulesService.test("adult", customer);
        boolean hasValidEmail = rulesService.test("validEmail", customer);
        boolean hasBalance = rulesService.test("hasBalance", customer);
        boolean isActive = rulesService.test("activeStatus", customer);
        
        System.out.println("Validation Results:");
        System.out.println("  * Required fields: " + (hasRequired ? "PASS" : "FAIL"));
        System.out.println("  * Age >= 18: " + (isAdult ? "PASS" : "FAIL"));
        System.out.println("  * Valid email: " + (hasValidEmail ? "PASS" : "FAIL"));
        System.out.println("  * Has balance: " + (hasBalance ? "PASS" : "FAIL"));
        System.out.println("  * Active status: " + (isActive ? "PASS" : "FAIL"));
        
        boolean allValid = hasRequired && isAdult && hasValidEmail && hasBalance && isActive;
        System.out.println("Overall Status: " + (allValid ? "APPROVED " : "REJECTED "));
    }
    
    /**
     * Demonstrate enrichment logic.
     */
    private void demonstrateEnrichmentLogic() {
        System.out.println("\n 2. Enrichment Logic");
        System.out.println("-".repeat(25));
        
        Map<String, Object> customer = createSampleCustomer();
        System.out.println("Original customer: " + customer);
        
        // Apply enrichments manually
        enrichCustomer(customer);
        
        System.out.println("Enriched customer:");
        System.out.println("  -> Customer Tier: " + customer.get("customerTier"));
        System.out.println("  -> Discount Rate: " + customer.get("discountRate"));
        System.out.println("  -> Currency: " + customer.get("currency"));
        System.out.println("  -> Risk Category: " + customer.get("riskCategory"));
    }
    
    /**
     * Demonstrate batch validation.
     */
    private void demonstrateBatchValidation() {
        System.out.println("\n 3. Batch Validation");
        System.out.println("-".repeat(25));
        
        List<Map<String, Object>> customers = createSampleBatch();
        
        int approved = 0, rejected = 0;
        
        for (Map<String, Object> customer : customers) {
            String customerId = (String) customer.get("customerId");
            
            // Validate and enrich
            boolean isValid = validateCustomer(customer);
            enrichCustomer(customer);
            
            if (isValid) {
                approved++;
                System.out.println("  " + customerId + ": APPROVED ");
            } else {
                rejected++;
                System.out.println("  " + customerId + ": REJECTED ");
            }
        }
        
        System.out.println("\nBatch Results:");
        System.out.println("  Total: " + customers.size());
        System.out.println("  Approved: " + approved);
        System.out.println("  Rejected: " + rejected);
    }
    
    /**
     * Validate customer using rules.
     */
    private boolean validateCustomer(Map<String, Object> customer) {
        boolean hasRequired = rulesService.test("requiredFields", customer);
        boolean isAdult = rulesService.test("adult", customer);
        boolean hasValidEmail = rulesService.test("validEmail", customer);
        boolean hasBalance = rulesService.test("hasBalance", customer);
        boolean isActive = rulesService.test("activeStatus", customer);
        
        return hasRequired && isAdult && hasValidEmail && hasBalance && isActive;
    }
    
    /**
     * Enrich customer with calculated fields.
     */
    private void enrichCustomer(Map<String, Object> customer) {
        // Calculate tier
        Double balance = (Double) customer.get("accountBalance");
        String tier = balance >= 20000 ? "GOLD" : (balance >= 10000 ? "SILVER" : "BRONZE");
        customer.put("customerTier", tier);
        
        // Calculate discount
        Integer age = (Integer) customer.get("age");
        Double discount = age >= 30 ? 0.15 : (age >= 21 ? 0.10 : 0.05);
        customer.put("discountRate", discount);
        
        // Add country info
        String country = (String) customer.get("country");
        switch (country) {
            case "US":
                customer.put("currency", "USD");
                break;
            case "GB":
                customer.put("currency", "GBP");
                break;
            case "DE":
                customer.put("currency", "EUR");
                break;
            default:
                customer.put("currency", "USD");
        }
        
        // Calculate risk
        String riskCategory = balance >= 20000 ? "LOW" : "MEDIUM";
        customer.put("riskCategory", riskCategory);
    }
    
    /**
     * Create a sample customer.
     */
    private Map<String, Object> createSampleCustomer() {
        Map<String, Object> customer = new HashMap<>();
        customer.put("customerId", "CUST003");
        customer.put("firstName", "Alice");
        customer.put("lastName", "Johnson");
        customer.put("email", "alice.johnson@example.com");
        customer.put("age", 28);
        customer.put("country", "GB");
        customer.put("accountBalance", 15000.0);
        customer.put("status", "ACTIVE");
        return customer;
    }
    
    /**
     * Create sample batch data.
     */
    private List<Map<String, Object>> createSampleBatch() {
        List<Map<String, Object>> customers = new ArrayList<>();
        
        // Valid customer
        Map<String, Object> customer1 = new HashMap<>();
        customer1.put("customerId", "CUST001");
        customer1.put("firstName", "John");
        customer1.put("lastName", "Smith");
        customer1.put("email", "john.smith@example.com");
        customer1.put("age", 35);
        customer1.put("country", "US");
        customer1.put("accountBalance", 25000.0);
        customer1.put("status", "ACTIVE");
        customers.add(customer1);
        
        // Invalid customer
        Map<String, Object> customer2 = new HashMap<>();
        customer2.put("customerId", "CUST002");
        customer2.put("firstName", "Jane");
        customer2.put("lastName", "Doe");
        customer2.put("email", "invalid-email");
        customer2.put("age", 16);
        customer2.put("country", "XX");
        customer2.put("accountBalance", -100.0);
        customer2.put("status", "SUSPENDED");
        customers.add(customer2);
        
        return customers;
    }
}
