package dev.mars.apex.demo.examples;

import dev.mars.apex.core.config.yaml.YamlDataset;
import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRule;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.service.enrichment.YamlEnrichmentProcessor;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.demo.core.DemoRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive demonstration of data management capabilities from the Data Management Guide.
 * 
 * This demo showcases:
 * - YAML dataset configuration and loading
 * - Data enrichment with lookup operations
 * - Validation with enriched data
 * - Nested object structures
 * - Multi-dataset scenarios
 * - Error handling and defaults
 * 
 * Based on scenarios from APEX_DATA_MANAGEMENT_GUIDE.md
 * 
 * @author APEX Rules Engine Team
 * @since 1.0.0
 */
public class DataManagementDemo implements DemoRunner.Demo {

    private YamlEnrichmentProcessor enrichmentProcessor;
    private LookupServiceRegistry serviceRegistry;
    private ExpressionEvaluatorService evaluatorService;

    @Override
    public void run() {
        System.out.println("=".repeat(80));
        System.out.println("APEX DATA MANAGEMENT DEMONSTRATION");
        System.out.println("=".repeat(80));
        System.out.println();

        initializeServices();
        
        demonstrateBasicDatasetStructure();
        demonstrateSimpleEnrichment();
        demonstrateComplexEnrichment();
        demonstrateNestedStructures();
        demonstrateMultiDatasetScenario();
        demonstrateValidationWithEnrichedData();
        demonstrateErrorHandlingAndDefaults();
        
        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("DATA MANAGEMENT DEMONSTRATION COMPLETED");
        System.out.println("=".repeat(80));
    }

    private void initializeServices() {
        System.out.println("🔧 Initializing Data Management Services...");
        serviceRegistry = new LookupServiceRegistry();
        evaluatorService = new ExpressionEvaluatorService();
        enrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
        System.out.println("✅ Services initialized successfully");
        System.out.println();
    }

    private void demonstrateBasicDatasetStructure() {
        System.out.println("📊 BASIC DATASET STRUCTURE DEMONSTRATION");
        System.out.println("-".repeat(50));
        
        // Create a basic dataset as shown in Data Management Guide Section 3
        YamlDataset currencyDataset = new YamlDataset();
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", "Basic Currency Data");
        metadata.put("version", "1.0.0");
        metadata.put("description", "Simple currency reference data for learning");
        metadata.put("type", "dataset");
        currencyDataset.setMetadata(metadata);
        
        List<Map<String, Object>> currencyData = List.of(
            Map.of("code", "USD", "name", "US Dollar", "active", true, "region", "North America"),
            Map.of("code", "EUR", "name", "Euro", "active", true, "region", "Europe"),
            Map.of("code", "GBP", "name", "British Pound", "active", true, "region", "Europe"),
            Map.of("code", "JPY", "name", "Japanese Yen", "active", true, "region", "Asia")
        );
        currencyDataset.setData(currencyData);
        
        System.out.println("Dataset Name: " + currencyDataset.getName());
        System.out.println("Dataset Version: " + currencyDataset.getVersion());
        System.out.println("Dataset Type: " + currencyDataset.getType());
        System.out.println("Number of Records: " + currencyDataset.getData().size());
        
        System.out.println("\nSample Currency Records:");
        currencyDataset.getData().forEach(record -> {
            System.out.printf("  %s - %s (%s) - Active: %s%n", 
                record.get("code"), record.get("name"), 
                record.get("region"), record.get("active"));
        });
        
        System.out.println("✅ Basic dataset structure demonstrated");
        System.out.println();
    }

    private void demonstrateSimpleEnrichment() {
        System.out.println("🔍 SIMPLE DATA ENRICHMENT DEMONSTRATION");
        System.out.println("-".repeat(50));
        
        // Create enrichment configuration
        YamlEnrichment enrichment = createCurrencyEnrichment();
        
        // Create test transaction
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("currency", "USD");
        transaction.put("amount", 1000.00);
        transaction.put("customerId", "CUST001");
        
        System.out.println("Original Transaction:");
        printMap(transaction, "  ");
        
        // Process enrichment
        System.out.println("\n🔄 Processing currency enrichment...");
        Object result = enrichmentProcessor.processEnrichment(enrichment, transaction);
        
        System.out.println("\nEnriched Transaction:");
        printMap((Map<String, Object>) result, "  ");
        
        System.out.println("✅ Simple enrichment demonstrated");
        System.out.println();
    }

    private void demonstrateComplexEnrichment() {
        System.out.println("🔍 COMPLEX DATA ENRICHMENT DEMONSTRATION");
        System.out.println("-".repeat(50));
        
        // Create complex product enrichment
        YamlEnrichment enrichment = createProductEnrichment();
        
        // Create test order
        Map<String, Object> order = new HashMap<>();
        order.put("productId", "LAPTOP001");
        order.put("quantity", 2);
        order.put("customerId", "CUST001");
        
        System.out.println("Original Order:");
        printMap(order, "  ");
        
        // Process enrichment
        System.out.println("\n🔄 Processing product enrichment...");
        Object result = enrichmentProcessor.processEnrichment(enrichment, order);
        
        System.out.println("\nEnriched Order:");
        printMap((Map<String, Object>) result, "  ");
        
        System.out.println("✅ Complex enrichment demonstrated");
        System.out.println();
    }

    private void demonstrateNestedStructures() {
        System.out.println("🏗️ NESTED OBJECT STRUCTURES DEMONSTRATION");
        System.out.println("-".repeat(50));
        
        // Create nested product data structure
        Map<String, Object> nestedProduct = Map.of(
            "id", "LAPTOP001",
            "name", "Business Laptop",
            "pricing", Map.of(
                "basePrice", 1299.99,
                "currency", "USD",
                "discounts", Map.of(
                    "volumeDiscount", 0.10,
                    "loyaltyDiscount", 0.05
                )
            ),
            "inventory", Map.of(
                "available", true,
                "stockLevel", 150,
                "availableForSale", 125
            ),
            "specifications", Map.of(
                "dimensions", Map.of(
                    "length", 35.5,
                    "width", 24.2,
                    "height", 2.1
                ),
                "weight", Map.of(
                    "value", 1.8,
                    "unit", "kg"
                ),
                "technical", Map.of(
                    "processor", "Intel i7",
                    "memory", "16GB",
                    "storage", "512GB SSD"
                )
            )
        );
        
        System.out.println("Nested Product Structure:");
        printNestedMap(nestedProduct, "  ");
        
        System.out.println("✅ Nested structures demonstrated");
        System.out.println();
    }

    private void demonstrateMultiDatasetScenario() {
        System.out.println("🔗 MULTI-DATASET SCENARIO DEMONSTRATION");
        System.out.println("-".repeat(50));
        
        // Create multiple enrichments
        YamlEnrichment currencyEnrichment = createCurrencyEnrichment();
        YamlEnrichment productEnrichment = createProductEnrichment();
        YamlEnrichment customerEnrichment = createCustomerEnrichment();
        
        // Create complex order
        Map<String, Object> order = new HashMap<>();
        order.put("orderId", "ORD001");
        order.put("customerId", "CUST001");
        order.put("productId", "LAPTOP001");
        order.put("currency", "USD");
        order.put("quantity", 2);
        order.put("amount", 2599.98);
        
        System.out.println("Original Order:");
        printMap(order, "  ");
        
        // Apply multiple enrichments
        System.out.println("\n🔄 Applying multiple enrichments...");
        
        System.out.println("  1. Currency enrichment...");
        enrichmentProcessor.processEnrichment(currencyEnrichment, order);
        
        System.out.println("  2. Product enrichment...");
        enrichmentProcessor.processEnrichment(productEnrichment, order);
        
        System.out.println("  3. Customer enrichment...");
        enrichmentProcessor.processEnrichment(customerEnrichment, order);
        
        System.out.println("\nFully Enriched Order:");
        printMap(order, "  ");
        
        System.out.println("✅ Multi-dataset scenario demonstrated");
        System.out.println();
    }

    private void demonstrateValidationWithEnrichedData() {
        System.out.println("✅ VALIDATION WITH ENRICHED DATA DEMONSTRATION");
        System.out.println("-".repeat(50));
        
        // Create enrichment and validation rule
        YamlEnrichment enrichment = createCurrencyEnrichment();
        
        // Test valid currency
        Map<String, Object> validTransaction = new HashMap<>();
        validTransaction.put("currency", "USD");
        validTransaction.put("amount", 1000.00);
        
        System.out.println("Testing Valid Currency Transaction:");
        printMap(validTransaction, "  ");
        
        enrichmentProcessor.processEnrichment(enrichment, validTransaction);
        
        System.out.println("\nAfter Enrichment:");
        printMap(validTransaction, "  ");
        
        // Simulate validation check
        boolean isValid = validTransaction.containsKey("currencyName") && 
                         validTransaction.get("currencyName") != null;
        System.out.println("Validation Result: " + (isValid ? "✅ VALID" : "❌ INVALID"));
        
        // Test invalid currency
        System.out.println("\nTesting Invalid Currency Transaction:");
        Map<String, Object> invalidTransaction = new HashMap<>();
        invalidTransaction.put("currency", "XYZ");
        invalidTransaction.put("amount", 1000.00);
        
        printMap(invalidTransaction, "  ");
        
        enrichmentProcessor.processEnrichment(enrichment, invalidTransaction);
        
        System.out.println("\nAfter Enrichment:");
        printMap(invalidTransaction, "  ");
        
        boolean isInvalid = invalidTransaction.containsKey("currencyName") && 
                           invalidTransaction.get("currencyName") != null;
        System.out.println("Validation Result: " + (isInvalid ? "✅ VALID" : "❌ INVALID"));
        
        System.out.println("✅ Validation with enriched data demonstrated");
        System.out.println();
    }

    private void demonstrateErrorHandlingAndDefaults() {
        System.out.println("🛡️ ERROR HANDLING AND DEFAULTS DEMONSTRATION");
        System.out.println("-".repeat(50));
        
        // Create enrichment with default values
        YamlEnrichment enrichment = createEnrichmentWithDefaults();
        
        // Test with unknown currency
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("currency", "UNKNOWN");
        transaction.put("amount", 500.00);
        
        System.out.println("Transaction with Unknown Currency:");
        printMap(transaction, "  ");
        
        System.out.println("\n🔄 Processing enrichment with defaults...");
        enrichmentProcessor.processEnrichment(enrichment, transaction);
        
        System.out.println("\nAfter Enrichment (with defaults):");
        printMap(transaction, "  ");
        
        System.out.println("✅ Error handling and defaults demonstrated");
        System.out.println();
    }

    // Helper methods to create enrichment configurations
    private YamlEnrichment createCurrencyEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("currency-lookup");
        enrichment.setType("lookup-enrichment");
        enrichment.setCondition("#currency != null");

        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        YamlEnrichment.LookupDataset lookupDataset = new YamlEnrichment.LookupDataset();
        lookupDataset.setType("inline");
        lookupDataset.setKeyField("code");

        List<Map<String, Object>> currencyData = List.of(
            Map.of("code", "USD", "name", "US Dollar", "active", true, "region", "North America"),
            Map.of("code", "EUR", "name", "Euro", "active", true, "region", "Europe"),
            Map.of("code", "GBP", "name", "British Pound", "active", true, "region", "Europe")
        );
        lookupDataset.setData(currencyData);
        lookupConfig.setLookupDataset(lookupDataset);
        lookupConfig.setLookupKey("#currency");
        enrichment.setLookupConfig(lookupConfig);

        List<YamlEnrichment.FieldMapping> fieldMappings = List.of(
            createFieldMapping("name", "currencyName"),
            createFieldMapping("active", "currencyActive"),
            createFieldMapping("region", "currencyRegion")
        );
        enrichment.setFieldMappings(fieldMappings);

        return enrichment;
    }

    private YamlEnrichment createProductEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("product-lookup");
        enrichment.setType("lookup-enrichment");
        enrichment.setCondition("#productId != null");

        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        YamlEnrichment.LookupDataset lookupDataset = new YamlEnrichment.LookupDataset();
        lookupDataset.setType("inline");
        lookupDataset.setKeyField("id");

        List<Map<String, Object>> productData = List.of(
            Map.of(
                "id", "LAPTOP001",
                "name", "Business Laptop",
                "price", 1299.99,
                "category", "Electronics",
                "available", true,
                "minQuantity", 1,
                "maxQuantity", 10
            )
        );
        lookupDataset.setData(productData);
        lookupConfig.setLookupDataset(lookupDataset);
        lookupConfig.setLookupKey("#productId");
        enrichment.setLookupConfig(lookupConfig);

        List<YamlEnrichment.FieldMapping> fieldMappings = List.of(
            createFieldMapping("name", "productName"),
            createFieldMapping("price", "productPrice"),
            createFieldMapping("category", "productCategory"),
            createFieldMapping("available", "productAvailable"),
            createFieldMapping("minQuantity", "productMinQuantity"),
            createFieldMapping("maxQuantity", "productMaxQuantity")
        );
        enrichment.setFieldMappings(fieldMappings);

        return enrichment;
    }

    private YamlEnrichment createCustomerEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("customer-lookup");
        enrichment.setType("lookup-enrichment");
        enrichment.setCondition("#customerId != null");

        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        YamlEnrichment.LookupDataset lookupDataset = new YamlEnrichment.LookupDataset();
        lookupDataset.setType("inline");
        lookupDataset.setKeyField("id");

        List<Map<String, Object>> customerData = List.of(
            Map.of(
                "id", "CUST001",
                "name", "John Doe",
                "email", "john@example.com",
                "category", "PREMIUM",
                "creditLimit", 50000.00,
                "discountRate", 0.15
            )
        );
        lookupDataset.setData(customerData);
        lookupConfig.setLookupDataset(lookupDataset);
        lookupConfig.setLookupKey("#customerId");
        enrichment.setLookupConfig(lookupConfig);

        List<YamlEnrichment.FieldMapping> fieldMappings = List.of(
            createFieldMapping("name", "customerName"),
            createFieldMapping("email", "customerEmail"),
            createFieldMapping("category", "customerCategory"),
            createFieldMapping("creditLimit", "customerCreditLimit"),
            createFieldMapping("discountRate", "customerDiscountRate")
        );
        enrichment.setFieldMappings(fieldMappings);

        return enrichment;
    }

    private YamlEnrichment createEnrichmentWithDefaults() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("currency-with-defaults");
        enrichment.setType("lookup-enrichment");
        enrichment.setCondition("#currency != null");

        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        YamlEnrichment.LookupDataset lookupDataset = new YamlEnrichment.LookupDataset();
        lookupDataset.setType("inline");
        lookupDataset.setKeyField("code");

        // Only include known currencies
        List<Map<String, Object>> currencyData = List.of(
            Map.of("code", "USD", "name", "US Dollar", "active", true, "region", "North America")
        );
        lookupDataset.setData(currencyData);

        // Set default values for missing data
        Map<String, Object> defaultValues = Map.of(
            "name", "Unknown Currency",
            "active", false,
            "region", "Unknown"
        );
        lookupDataset.setDefaultValues(defaultValues);

        lookupConfig.setLookupDataset(lookupDataset);
        lookupConfig.setLookupKey("#currency");
        enrichment.setLookupConfig(lookupConfig);

        List<YamlEnrichment.FieldMapping> fieldMappings = List.of(
            createFieldMappingWithDefault("name", "currencyName", "Unknown Currency"),
            createFieldMappingWithDefault("active", "currencyActive", false),
            createFieldMappingWithDefault("region", "currencyRegion", "Unknown")
        );
        enrichment.setFieldMappings(fieldMappings);

        return enrichment;
    }

    private YamlEnrichment.FieldMapping createFieldMapping(String sourceField, String targetField) {
        YamlEnrichment.FieldMapping mapping = new YamlEnrichment.FieldMapping();
        mapping.setSourceField(sourceField);
        mapping.setTargetField(targetField);
        return mapping;
    }

    private YamlEnrichment.FieldMapping createFieldMappingWithDefault(String sourceField, String targetField, Object defaultValue) {
        YamlEnrichment.FieldMapping mapping = new YamlEnrichment.FieldMapping();
        mapping.setSourceField(sourceField);
        mapping.setTargetField(targetField);
        mapping.setDefaultValue(defaultValue);
        return mapping;
    }

    // Utility methods for display
    private void printMap(Map<String, Object> map, String indent) {
        map.forEach((key, value) -> {
            if (value instanceof Map) {
                System.out.println(indent + key + ":");
                printNestedMap((Map<String, Object>) value, indent + "  ");
            } else {
                System.out.println(indent + key + ": " + value);
            }
        });
    }

    private void printNestedMap(Map<String, Object> map, String indent) {
        map.forEach((key, value) -> {
            if (value instanceof Map) {
                System.out.println(indent + key + ":");
                printNestedMap((Map<String, Object>) value, indent + "  ");
            } else {
                System.out.println(indent + key + ": " + value);
            }
        });
    }
}
