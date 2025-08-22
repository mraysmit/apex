package dev.mars.apex.demo.examples.lookups;

import dev.mars.apex.demo.model.lookups.CustomerOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
                
                // Simulate enrichment based on compound key lookup
                CustomerOrder enriched = simulateCompoundKeyEnrichment(order);
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
     * Simulate compound key enrichment based on the lookup configuration.
     * This demonstrates what the YAML configuration would do.
     */
    private CustomerOrder simulateCompoundKeyEnrichment(CustomerOrder original) {
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

        // Create compound key
        String compoundKey = original.getCustomerId() + "-" + original.getRegion();

        // Simulate lookup based on compound key
        switch (compoundKey) {
            case "CUST001-NA":
                enriched.setCustomerTier("PLATINUM");
                enriched.setRegionalDiscount(new BigDecimal("0.15"));
                enriched.setSpecialPricing("VOLUME_DISCOUNT");
                enriched.setCustomerName("TechCorp Solutions");
                enriched.setRegionName("North America");
                enriched.setCurrency("USD");
                enriched.setTaxRate(new BigDecimal("0.08"));
                break;

            case "CUST002-NA":
                enriched.setCustomerTier("GOLD");
                enriched.setRegionalDiscount(new BigDecimal("0.12"));
                enriched.setSpecialPricing("STANDARD_DISCOUNT");
                enriched.setCustomerName("InnovateTech Inc");
                enriched.setRegionName("North America");
                enriched.setCurrency("USD");
                enriched.setTaxRate(new BigDecimal("0.08"));
                break;

            case "CUST001-EU":
                enriched.setCustomerTier("GOLD");
                enriched.setRegionalDiscount(new BigDecimal("0.10"));
                enriched.setSpecialPricing("EU_PRICING");
                enriched.setCustomerName("TechCorp Solutions Europe");
                enriched.setRegionName("Europe");
                enriched.setCurrency("EUR");
                enriched.setTaxRate(new BigDecimal("0.20"));
                break;

            case "CUST003-EU":
                enriched.setCustomerTier("SILVER");
                enriched.setRegionalDiscount(new BigDecimal("0.08"));
                enriched.setSpecialPricing("STANDARD_DISCOUNT");
                enriched.setCustomerName("EuroTech GmbH");
                enriched.setRegionName("Europe");
                enriched.setCurrency("EUR");
                enriched.setTaxRate(new BigDecimal("0.19"));
                break;

            case "CUST004-APAC":
                enriched.setCustomerTier("PLATINUM");
                enriched.setRegionalDiscount(new BigDecimal("0.18"));
                enriched.setSpecialPricing("APAC_PREMIUM");
                enriched.setCustomerName("Asia Pacific Technologies");
                enriched.setRegionName("Asia Pacific");
                enriched.setCurrency("USD");
                enriched.setTaxRate(new BigDecimal("0.10"));
                break;

            case "CUST005-APAC":
                enriched.setCustomerTier("GOLD");
                enriched.setRegionalDiscount(new BigDecimal("0.14"));
                enriched.setSpecialPricing("VOLUME_DISCOUNT");
                enriched.setCustomerName("Singapore Tech Solutions");
                enriched.setRegionName("Asia Pacific");
                enriched.setCurrency("SGD");
                enriched.setTaxRate(new BigDecimal("0.07"));
                break;

            case "CUST006-LATAM":
                enriched.setCustomerTier("SILVER");
                enriched.setRegionalDiscount(new BigDecimal("0.12"));
                enriched.setSpecialPricing("EMERGING_MARKET");
                enriched.setCustomerName("LatAm Technology Partners");
                enriched.setRegionName("Latin America");
                enriched.setCurrency("USD");
                enriched.setTaxRate(new BigDecimal("0.15"));
                break;

            case "CUST007-ME":
                enriched.setCustomerTier("GOLD");
                enriched.setRegionalDiscount(new BigDecimal("0.13"));
                enriched.setSpecialPricing("REGIONAL_PARTNER");
                enriched.setCustomerName("Middle East Tech Hub");
                enriched.setRegionName("Middle East");
                enriched.setCurrency("USD");
                enriched.setTaxRate(new BigDecimal("0.05"));
                break;

            case "CUST002-EU":
                enriched.setCustomerTier("GOLD");
                enriched.setRegionalDiscount(new BigDecimal("0.11"));
                enriched.setSpecialPricing("EU_PRICING");
                enriched.setCustomerName("InnovateTech Europe");
                enriched.setRegionName("Europe");
                enriched.setCurrency("EUR");
                enriched.setTaxRate(new BigDecimal("0.21"));
                break;

            case "CUST004-NA":
                enriched.setCustomerTier("PLATINUM");
                enriched.setRegionalDiscount(new BigDecimal("0.16"));
                enriched.setSpecialPricing("CROSS_REGION_PREMIUM");
                enriched.setCustomerName("Asia Pacific Technologies USA");
                enriched.setRegionName("North America");
                enriched.setCurrency("USD");
                enriched.setTaxRate(new BigDecimal("0.09"));
                break;

            default:
                // Unknown compound key - set defaults
                enriched.setCustomerTier("BRONZE");
                enriched.setRegionalDiscount(new BigDecimal("0.05"));
                enriched.setSpecialPricing("STANDARD");
                enriched.setCustomerName("Unknown Customer");
                enriched.setRegionName("Unknown Region");
                enriched.setCurrency("USD");
                enriched.setTaxRate(new BigDecimal("0.10"));
                break;
        }

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
}
