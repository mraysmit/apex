package dev.mars.apex.demo.examples;

import dev.mars.apex.core.config.yaml.YamlDataset;
import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.service.enrichment.YamlEnrichmentProcessor;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.demo.DemoRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive demonstration of data management capabilities from the Data Management Guide.
 *
 * This demo showcases APEX's powerful data enrichment and management features:
 *
 * Core Features Demonstrated:
 * - YAML dataset configuration and loading
 * - Data enrichment with lookup operations
 * - Validation with enriched data
 * - Nested object structures and complex mappings
 * - Multi-dataset scenarios and relationships
 * - Error handling and default value strategies
 * - Performance optimization for data operations
 *
 * Real-world Use Cases:
 * - Currency and country code enrichment
 * - Product catalog lookups
 * - Customer data validation and enhancement
 * - Reference data management
 * - Data quality and completeness checks
 *
 * Each section includes detailed explanations, performance metrics, and best practices
 * for implementing data management patterns in production systems.
 *
 * Based on scenarios from APEX_DATA_MANAGEMENT_GUIDE.md
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class DataManagementDemo implements DemoRunner.Demo {

    private static final Logger logger = LoggerFactory.getLogger(DataManagementDemo.class);

    private YamlEnrichmentProcessor enrichmentProcessor;
    private LookupServiceRegistry serviceRegistry;
    private ExpressionEvaluatorService evaluatorService;

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        logger.info("Starting Data Management demonstration");

        System.out.println("=".repeat(80));
        System.out.println("APEX DATA MANAGEMENT DEMONSTRATION");
        System.out.println("=".repeat(80));
        System.out.println("Comprehensive showcase of APEX's data enrichment and management capabilities");
        System.out.println("Estimated duration: 8-10 minutes");
        System.out.println();
        System.out.println("Sections covered:");
        System.out.println("1. Basic Dataset Structure - Foundation concepts");
        System.out.println("2. Simple Enrichment - Single field lookups");
        System.out.println("3. Complex Enrichment - Multi-field transformations");
        System.out.println("4. Nested Structures - Hierarchical data handling");
        System.out.println("5. Multi-Dataset Scenarios - Related data sources");
        System.out.println("6. Validation with Enriched Data - Quality assurance");
        System.out.println("7. Error Handling and Defaults - Resilience patterns");
        System.out.println();

        initializeServices();

        // Execute each demonstration section with timing and error handling
        executeTimedSection("Basic Dataset Structure", this::demonstrateBasicDatasetStructure);
        executeTimedSection("Simple Enrichment", this::demonstrateSimpleEnrichment);
        executeTimedSection("Complex Enrichment", this::demonstrateComplexEnrichment);
        executeTimedSection("Nested Structures", this::demonstrateNestedStructures);
        executeTimedSection("Multi-Dataset Scenario", this::demonstrateMultiDatasetScenario);
        executeTimedSection("Validation with Enriched Data", this::demonstrateValidationWithEnrichedData);
        executeTimedSection("Error Handling and Defaults", this::demonstrateErrorHandlingAndDefaults);

        long totalDuration = System.currentTimeMillis() - startTime;
        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("DATA MANAGEMENT DEMONSTRATION COMPLETED");
        System.out.println("Total duration: " + totalDuration + "ms");
        System.out.println("You've learned advanced data management patterns with APEX!");
        System.out.println("=".repeat(80));

        logger.info("Data Management demonstration completed in {}ms", totalDuration);
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
            System.out.println("   This demonstrates error handling in data management operations");
        }

        System.out.println();
    }

    /**
     * Initialize the core services required for data management operations.
     *
     * This method sets up the service infrastructure needed for:
     * - Lookup service registry for managing data sources
     * - Expression evaluator for rule processing
     * - Enrichment processor for data transformation
     *
     * Each service is initialized with appropriate configuration and
     * error handling to ensure robust operation.
     */
    private void initializeServices() {
        long initStart = System.currentTimeMillis();
        logger.info("Initializing Data Management Services");

        System.out.println("INITIALIZATION PHASE");
        System.out.println("-".repeat(40));
        System.out.println("Setting up core data management services...");

        try {
            // Initialize lookup service registry
            System.out.println("1. Initializing Lookup Service Registry...");
            serviceRegistry = new LookupServiceRegistry();
            logger.debug("LookupServiceRegistry initialized");

            // Initialize expression evaluator
            System.out.println("2. Initializing Expression Evaluator Service...");
            evaluatorService = new ExpressionEvaluatorService();
            logger.debug("ExpressionEvaluatorService initialized");

            // Initialize enrichment processor
            System.out.println("3. Initializing YAML Enrichment Processor...");
            enrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
            logger.debug("YamlEnrichmentProcessor initialized");

            long initDuration = System.currentTimeMillis() - initStart;
            System.out.println("Services initialized successfully (in " + initDuration + "ms)");

            logger.info("All data management services initialized in {}ms", initDuration);

        } catch (Exception e) {
            logger.error("Failed to initialize data management services", e);
            System.out.println("ERROR: Failed to initialize services - " + e.getMessage());
            throw new RuntimeException("Service initialization failed", e);
        }

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
        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            printMap(resultMap, "  ");
        } else {
            System.out.println("  Result is not a Map: " + result);
        }
        
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
        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            printMap(resultMap, "  ");
        } else {
            System.out.println("  Result is not a Map: " + result);
        }
        
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
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                printNestedMap(nestedMap, indent + "  ");
            } else {
                System.out.println(indent + key + ": " + value);
            }
        });
    }

    private void printNestedMap(Map<String, Object> map, String indent) {
        map.forEach((key, value) -> {
            if (value instanceof Map) {
                System.out.println(indent + key + ":");
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                printNestedMap(nestedMap, indent + "  ");
            } else {
                System.out.println(indent + key + ": " + value);
            }
        });
    }
}
