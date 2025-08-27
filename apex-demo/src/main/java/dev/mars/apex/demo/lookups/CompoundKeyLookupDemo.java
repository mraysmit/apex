package dev.mars.apex.demo.lookups;

import dev.mars.apex.demo.bootstrap.model.CustomerOrder;
import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRule;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.util.TypeSafeDataExtractor;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates compound key lookup using string concatenation.
 * This example shows how to combine multiple field values to create a compound lookup key
 * for customer-region specific pricing and tier information.
 *
 * Pattern Demonstrated: lookup-key: "#customerId + '-' + #region"
 * Use Case: Customer-region specific pricing lookup
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-22
 * @version 1.0
 */
public class CompoundKeyLookupDemo extends AbstractLookupDemo {
    
    public static void main(String[] args) {
        new CompoundKeyLookupDemo().runDemo();
    }
    
    @Override
    protected String getDemoTitle() {
        return "Compound Key Lookup Demo - Customer-Region Pricing";
    }
    
    @Override
    protected String getDemoDescription() {
        return "Demonstrates compound lookup-key pattern '#customerId + \"-\" + #region' to enrich orders\n" +
               "   with customer-region specific pricing and tier information. This pattern shows how to\n" +
               "   combine multiple field values to create a unique lookup key that matches against\n" +
               "   a compound key in the reference dataset. Each order's customer ID and region are\n" +
               "   concatenated to find the appropriate pricing tier and regional discounts.";
    }
    
    @Override
    protected String getYamlConfigPath() {
        return "examples/lookups/compound-key-lookup.yaml";
    }
    
    @Override
    protected void loadConfiguration() throws Exception {
        System.out.println("📁 Loading YAML configuration from: " + getYamlConfigPath());
        
        // Load the YAML configuration from classpath
        ruleConfiguration = yamlLoader.loadFromClasspath(getYamlConfigPath());
        rulesEngine = yamlService.createRulesEngineFromYamlConfig(ruleConfiguration);
        
        System.out.println("✅ Configuration loaded successfully");
        System.out.println("   - Enrichments: 1 (customer-region-pricing-enrichment)");
        System.out.println("   - Validations: 3 (customer-id-format, region-code-validation, quantity-positive)");
        System.out.println("   - Lookup Dataset: 10 customer-region combinations");
        System.out.println("   - Compound Key Pattern: customerId + '-' + region");
    }
    
    @Override
    protected List<CustomerOrder> generateTestData() {
        System.out.println("🏭 Generating test customer orders...");
        
        List<CustomerOrder> orders = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(3);
        
        // Create diverse order data with different customer-region combinations
        orders.add(new CustomerOrder(
            "ORD-001", 
            "CUST001", 
            "NA", 
            "PROD-LAPTOP-001",
            5,
            new BigDecimal("1299.99"), 
            baseTime.plusHours(2),
            "CONFIRMED"
        ));
        
        orders.add(new CustomerOrder(
            "ORD-002", 
            "CUST002", 
            "NA", 
            "PROD-SERVER-001",
            2,
            new BigDecimal("4999.99"), 
            baseTime.plusHours(8),
            "PENDING"
        ));
        
        orders.add(new CustomerOrder(
            "ORD-003", 
            "CUST001", 
            "EU", 
            "PROD-LAPTOP-001",
            10,
            new BigDecimal("1199.99"), 
            baseTime.plusHours(14),
            "CONFIRMED"
        ));
        
        orders.add(new CustomerOrder(
            "ORD-004", 
            "CUST003", 
            "EU", 
            "PROD-TABLET-001",
            25,
            new BigDecimal("599.99"), 
            baseTime.plusDays(1),
            "PROCESSING"
        ));
        
        orders.add(new CustomerOrder(
            "ORD-005", 
            "CUST004", 
            "APAC", 
            "PROD-WORKSTATION-001",
            3,
            new BigDecimal("2899.99"), 
            baseTime.plusDays(1).plusHours(6),
            "CONFIRMED"
        ));
        
        orders.add(new CustomerOrder(
            "ORD-006", 
            "CUST005", 
            "APAC", 
            "PROD-MONITOR-001",
            15,
            new BigDecimal("399.99"), 
            baseTime.plusDays(2),
            "PENDING"
        ));
        
        orders.add(new CustomerOrder(
            "ORD-007", 
            "CUST006", 
            "LATAM", 
            "PROD-PRINTER-001",
            8,
            new BigDecimal("299.99"), 
            baseTime.plusDays(2).plusHours(4),
            "CONFIRMED"
        ));
        
        orders.add(new CustomerOrder(
            "ORD-008", 
            "CUST007", 
            "ME", 
            "PROD-LAPTOP-001",
            12,
            new BigDecimal("1299.99"), 
            baseTime.plusDays(2).plusHours(10),
            "PROCESSING"
        ));
        
        // Cross-region orders (same customer, different regions)
        orders.add(new CustomerOrder(
            "ORD-009", 
            "CUST002", 
            "EU", 
            "PROD-SERVER-001",
            1,
            new BigDecimal("4999.99"), 
            baseTime.plusDays(3),
            "PENDING"
        ));
        
        orders.add(new CustomerOrder(
            "ORD-010", 
            "CUST004", 
            "NA", 
            "PROD-WORKSTATION-001",
            4,
            new BigDecimal("2899.99"), 
            baseTime.plusDays(3).plusHours(3),
            "CONFIRMED"
        ));
        
        System.out.println("✅ Generated " + orders.size() + " test orders");
        System.out.println("   - Customers: CUST001-CUST007");
        System.out.println("   - Regions: NA, EU, APAC, LATAM, ME");
        System.out.println("   - Products: Laptops, Servers, Tablets, Workstations, Monitors, Printers");
        System.out.println("   - Compound Keys: " + orders.size() + " unique customer-region combinations");
        
        return orders;
    }
    
    @Override
    protected List<CustomerOrder> processData(List<?> data) throws Exception {
        System.out.println("⚙️  Processing orders with customer-region pricing enrichment...");

        List<CustomerOrder> results = new ArrayList<>();

        for (Object item : data) {
            if (item instanceof CustomerOrder) {
                CustomerOrder order = (CustomerOrder) item;

                // Use actual APEX rules engine to process the order
                CustomerOrder enriched = processOrderWithApexEngine(order);
                results.add(enriched);

                // Log the lookup process
                String compoundKey = order.getCustomerId() + "-" + order.getRegion();
                System.out.println("   🔍 Processed " + order.getOrderId() +
                                 " (Key: " + compoundKey + ") -> " +
                                 "Tier: " + enriched.getCustomerTier() +
                                 ", Discount: " + (enriched.getRegionalDiscount() != null ?
                                     String.format("%.1f%%", enriched.getRegionalDiscount().multiply(BigDecimal.valueOf(100)).doubleValue()) : "N/A"));
            }
        }

        return results;
    }

    /**
     * Process order using the actual APEX rules engine with the loaded YAML configuration.
     * This replaces the previous simulation approach with real APEX functionality.
     */
    private CustomerOrder processOrderWithApexEngine(CustomerOrder original) throws Exception {
        // Create a copy of the original order to enrich
        CustomerOrder enriched = new CustomerOrder(
            original.getOrderId(),
            original.getCustomerId(),
            original.getRegion(),
            original.getProductId(),
            original.getQuantity(),
            original.getUnitPrice(),
            original.getOrderDate(),
            original.getStatus()
        );

        // Convert order to Map for APEX processing
        Map<String, Object> orderData = convertOrderToMap(enriched);

        // Apply enrichments using the APEX rules engine
        if (ruleConfiguration != null && ruleConfiguration.getEnrichments() != null) {
            for (YamlEnrichment enrichment : ruleConfiguration.getEnrichments()) {
                if (enrichment.getEnabled() != null && enrichment.getEnabled()) {
                    applyEnrichmentToOrder(enrichment, orderData);
                }
            }
        }

        // Apply validation rules using the APEX rules engine
        if (ruleConfiguration != null && ruleConfiguration.getRules() != null) {
            for (YamlRule rule : ruleConfiguration.getRules()) {
                if (rule.getEnabled() != null && rule.getEnabled()) {
                    applyValidationRuleToOrder(rule, orderData);
                }
            }
        }

        // Convert enriched data back to CustomerOrder object
        updateOrderFromMap(enriched, orderData);

        return enriched;
    }

    @Override
    protected List<CustomerOrder> generateErrorTestData() {
        System.out.println("⚠️  Generating error scenario test data...");

        List<CustomerOrder> errorOrders = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now();

        // Invalid customer ID format
        errorOrders.add(new CustomerOrder(
            "ERR-001",
            "INVALID-CUSTOMER", // Invalid format
            "NA",
            "PROD-TEST-001",
            1,
            new BigDecimal("100.00"),
            baseTime
        ));

        // Invalid region code
        errorOrders.add(new CustomerOrder(
            "ERR-002",
            "CUST001",
            "INVALID", // Invalid region
            "PROD-TEST-001",
            1,
            new BigDecimal("100.00"),
            baseTime
        ));

        // Unknown compound key (valid format but not in dataset)
        errorOrders.add(new CustomerOrder(
            "ERR-003",
            "CUST999",
            "AFRICA", // Valid region but unknown customer
            "PROD-TEST-001",
            1,
            new BigDecimal("100.00"),
            baseTime
        ));

        // Zero quantity
        errorOrders.add(new CustomerOrder(
            "ERR-004",
            "CUST001",
            "NA",
            "PROD-TEST-001",
            0, // Invalid quantity
            new BigDecimal("100.00"),
            baseTime
        ));

        // Null customer ID
        CustomerOrder nullCustomerOrder = new CustomerOrder(
            "ERR-005",
            null, // Null customer ID
            "NA",
            "PROD-TEST-001",
            1,
            new BigDecimal("100.00"),
            baseTime
        );
        errorOrders.add(nullCustomerOrder);

        System.out.println("✅ Generated " + errorOrders.size() + " error scenario orders");

        return errorOrders;
    }

    /**
     * Convert CustomerOrder to Map for APEX processing.
     */
    private Map<String, Object> convertOrderToMap(CustomerOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", order.getOrderId());
        map.put("customerId", order.getCustomerId());
        map.put("region", order.getRegion());
        map.put("productId", order.getProductId());
        map.put("quantity", order.getQuantity());
        map.put("unitPrice", order.getUnitPrice());
        map.put("orderDate", order.getOrderDate());
        map.put("status", order.getStatus());

        // Add existing enriched fields if present
        if (order.getCustomerTier() != null) map.put("customerTier", order.getCustomerTier());
        if (order.getRegionalDiscount() != null) map.put("regionalDiscount", order.getRegionalDiscount());
        if (order.getSpecialPricing() != null) map.put("specialPricing", order.getSpecialPricing());
        if (order.getCustomerName() != null) map.put("customerName", order.getCustomerName());
        if (order.getRegionName() != null) map.put("regionName", order.getRegionName());
        if (order.getCurrency() != null) map.put("currency", order.getCurrency());
        if (order.getTaxRate() != null) map.put("taxRate", order.getTaxRate());

        return map;
    }

    /**
     * Apply enrichment to order data using APEX engine.
     */
    private void applyEnrichmentToOrder(YamlEnrichment enrichment, Map<String, Object> orderData) throws Exception {
        // Apply lookup enrichment directly using the YAML configuration
        if (enrichment.getLookupConfig() != null) {
            applyLookupEnrichment(enrichment, orderData);
        }
    }

    /**
     * Apply lookup enrichment using the APEX engine.
     */
    private void applyLookupEnrichment(YamlEnrichment enrichment, Map<String, Object> orderData) throws Exception {
        // Create evaluation context
        StandardEvaluationContext context = new StandardEvaluationContext();
        orderData.forEach(context::setVariable);

        // Evaluate condition
        if (enrichment.getCondition() != null) {
            ExpressionEvaluatorService evaluator = new ExpressionEvaluatorService();
            Boolean conditionResult = evaluator.evaluate(enrichment.getCondition(), context, Boolean.class);
            if (conditionResult == null || !conditionResult) {
                return; // Condition not met, skip enrichment
            }
        }

        // Apply lookup enrichment using the dataset from YAML
        if (enrichment.getLookupConfig() != null && enrichment.getLookupConfig().getLookupDataset() != null) {
            String lookupKey = enrichment.getLookupConfig().getLookupKey();
            if (lookupKey != null) {
                ExpressionEvaluatorService evaluator = new ExpressionEvaluatorService();
                String keyValue = evaluator.evaluate(lookupKey, context, String.class);

                // Find matching data in the lookup dataset with type safety
                var dataset = enrichment.getLookupConfig().getLookupDataset();
                if (dataset.getData() != null) {
                    String keyField = dataset.getKeyField();

                    // Validate dataset structure before processing
                    if (!TypeSafeDataExtractor.validateDatasetStructure(dataset.getData(), keyField, "compound-key-lookup")) {
                        System.out.println("   ⚠️  Invalid dataset structure, skipping enrichment");
                        return;
                    }

                    // Safe iteration over validated dataset
                    var dataList = TypeSafeDataExtractor.safeListMapCast(dataset.getData(), "compound-key-lookup-data");
                    if (dataList.isPresent()) {
                        for (Map<String, Object> dataRow : dataList.get()) {
                            String rowKeyValue = TypeSafeDataExtractor.safeGetString(dataRow, keyField, null);
                            if (keyValue != null && keyValue.equals(rowKeyValue)) {
                                // Apply field mappings
                                if (enrichment.getFieldMappings() != null) {
                                    for (var mapping : enrichment.getFieldMappings()) {
                                        Object sourceValue = dataRow.get(mapping.getSourceField());
                                        if (sourceValue != null) {
                                            orderData.put(mapping.getTargetField(), sourceValue);
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Apply validation rule to order data.
     */
    private void applyValidationRuleToOrder(YamlRule rule, Map<String, Object> orderData) throws Exception {
        if (rule.getCondition() != null) {
            StandardEvaluationContext context = new StandardEvaluationContext();
            orderData.forEach(context::setVariable);

            ExpressionEvaluatorService evaluator = new ExpressionEvaluatorService();
            Boolean result = evaluator.evaluate(rule.getCondition(), context, Boolean.class);

            if (result == null || !result) {
                System.out.println("   ⚠️  Validation failed: " + rule.getName() + " - " + rule.getMessage());
            }
        }
    }

    /**
     * Update CustomerOrder from enriched Map data.
     */
    private void updateOrderFromMap(CustomerOrder order, Map<String, Object> orderData) {
        // Update enriched fields
        if (orderData.containsKey("customerTier")) {
            order.setCustomerTier((String) orderData.get("customerTier"));
        }
        if (orderData.containsKey("regionalDiscount")) {
            Object discount = orderData.get("regionalDiscount");
            if (discount instanceof Number) {
                order.setRegionalDiscount(new BigDecimal(discount.toString()));
            }
        }
        if (orderData.containsKey("specialPricing")) {
            order.setSpecialPricing((String) orderData.get("specialPricing"));
        }
        if (orderData.containsKey("customerName")) {
            order.setCustomerName((String) orderData.get("customerName"));
        }
        if (orderData.containsKey("regionName")) {
            order.setRegionName((String) orderData.get("regionName"));
        }
        if (orderData.containsKey("currency")) {
            order.setCurrency((String) orderData.get("currency"));
        }
        if (orderData.containsKey("taxRate")) {
            Object taxRate = orderData.get("taxRate");
            if (taxRate instanceof Number) {
                order.setTaxRate(new BigDecimal(taxRate.toString()));
            }
        }
    }
}
