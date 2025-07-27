package dev.mars.rulesengine.demo.examples.enrichment;

import dev.mars.rulesengine.core.config.yaml.YamlEnrichment;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import dev.mars.rulesengine.core.service.enrichment.YamlEnrichmentProcessor;
import dev.mars.rulesengine.core.service.lookup.LookupServiceRegistry;

import java.util.*;

/**
 * Simple demonstration of YAML dataset functionality.
 * Shows the core dataset enrichment capabilities working.
 */
public class SimpleDatasetDemo {

    public static void main(String[] args) {
        System.out.println("=== SIMPLE YAML DATASET DEMO ===");
        System.out.println("Demonstrating inline dataset enrichment functionality\n");

        SimpleDatasetDemo demo = new SimpleDatasetDemo();
        
        try {
            demo.demonstrateInlineDatasetEnrichment();
            demo.demonstrateMultipleDatasets();
            demo.demonstrateDefaultValues();
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== DATASET ENRICHMENT BENEFITS ===");
        System.out.println("✓ No External Services: Data embedded in YAML configuration");
        System.out.println("✓ Fast Lookups: In-memory hash map performance");
        System.out.println("✓ Default Values: Graceful handling of missing data");
        System.out.println("✓ Type Safety: Proper field mapping and validation");
        System.out.println("✓ Configuration Driven: Business users can modify datasets");
        
        System.out.println("\n=== DEMO COMPLETED ===");
    }

    /**
     * Demonstrate basic inline dataset enrichment.
     */
    private void demonstrateInlineDatasetEnrichment() {
        System.out.println("=== STEP 1: INLINE DATASET ENRICHMENT ===");
        System.out.println("Creating currency enrichment with inline dataset\n");

        // Create enrichment processor
        LookupServiceRegistry registry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluator = new ExpressionEvaluatorService();
        YamlEnrichmentProcessor processor = new YamlEnrichmentProcessor(registry, evaluator);

        // Create currency enrichment with inline dataset
        YamlEnrichment enrichment = createCurrencyEnrichment();

        // Create test trade
        TestTrade trade = new TestTrade();
        trade.currency = "USD";

        System.out.println("Before enrichment:");
        System.out.println("  Currency: " + trade.currency);
        System.out.println("  Currency Name: " + trade.currencyName);
        System.out.println("  Currency Region: " + trade.currencyRegion);

        // Apply enrichment
        processor.processEnrichment(enrichment, trade);

        System.out.println("\nAfter enrichment:");
        System.out.println("  Currency: " + trade.currency);
        System.out.println("  Currency Name: " + trade.currencyName);
        System.out.println("  Currency Region: " + trade.currencyRegion);
        System.out.println("  Currency Decimals: " + trade.currencyDecimals);
        System.out.println("  Is Active: " + trade.isActive);

        System.out.println("\n✓ Inline dataset enrichment completed successfully!\n");
    }

    /**
     * Demonstrate multiple dataset enrichments.
     */
    private void demonstrateMultipleDatasets() {
        System.out.println("=== STEP 2: MULTIPLE DATASET ENRICHMENTS ===");
        System.out.println("Applying currency and country enrichments\n");

        // Create enrichment processor
        LookupServiceRegistry registry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluator = new ExpressionEvaluatorService();
        YamlEnrichmentProcessor processor = new YamlEnrichmentProcessor(registry, evaluator);

        // Create multiple enrichments
        List<YamlEnrichment> enrichments = Arrays.asList(
            createCurrencyEnrichment(),
            createCountryEnrichment()
        );

        // Create test trade
        TestTrade trade = new TestTrade();
        trade.currency = "EUR";
        trade.country = "DE";

        System.out.println("Before enrichment:");
        System.out.println("  Currency: " + trade.currency);
        System.out.println("  Country: " + trade.country);

        // Apply all enrichments
        processor.processEnrichments(enrichments, trade);

        System.out.println("\nAfter enrichment:");
        System.out.println("  Currency Name: " + trade.currencyName);
        System.out.println("  Currency Region: " + trade.currencyRegion);
        System.out.println("  Country Name: " + trade.countryName);
        System.out.println("  Country Region: " + trade.countryRegion);

        System.out.println("\n✓ Multiple dataset enrichments completed successfully!\n");
    }

    /**
     * Demonstrate default value handling.
     */
    private void demonstrateDefaultValues() {
        System.out.println("=== STEP 3: DEFAULT VALUE HANDLING ===");
        System.out.println("Testing enrichment with unknown values\n");

        // Create enrichment processor
        LookupServiceRegistry registry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluator = new ExpressionEvaluatorService();
        YamlEnrichmentProcessor processor = new YamlEnrichmentProcessor(registry, evaluator);

        // Create currency enrichment
        YamlEnrichment enrichment = createCurrencyEnrichment();

        // Create test trade with unknown currency
        TestTrade trade = new TestTrade();
        trade.currency = "XYZ"; // Unknown currency

        System.out.println("Before enrichment:");
        System.out.println("  Currency: " + trade.currency + " (unknown)");

        // Apply enrichment
        processor.processEnrichment(enrichment, trade);

        System.out.println("\nAfter enrichment (with defaults):");
        System.out.println("  Currency Name: " + trade.currencyName + " (should be null - no default)");
        System.out.println("  Currency Region: " + trade.currencyRegion + " (should be 'Unknown' - default)");
        System.out.println("  Is Active: " + trade.isActive + " (should be false - default)");

        System.out.println("\n✓ Default value handling completed successfully!\n");
    }

    /**
     * Create currency enrichment with inline dataset.
     */
    private YamlEnrichment createCurrencyEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("currency-enrichment");
        enrichment.setName("Currency Enrichment");
        enrichment.setType("lookup-enrichment");
        enrichment.setEnabled(true);
        enrichment.setPriority(10);
        enrichment.setCondition("#currency != null");

        // Create lookup configuration with inline dataset
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupKey("#currency");

        // Create inline dataset
        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("code");

        // Create currency data
        List<Map<String, Object>> data = Arrays.asList(
            createCurrencyRecord("USD", "US Dollar", 2, true, "North America"),
            createCurrencyRecord("EUR", "Euro", 2, true, "Europe"),
            createCurrencyRecord("GBP", "British Pound", 2, true, "Europe"),
            createCurrencyRecord("JPY", "Japanese Yen", 0, true, "Asia")
        );
        dataset.setData(data);

        // Set default values
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("region", "Unknown");
        defaults.put("isActive", false);
        dataset.setDefaultValues(defaults);

        lookupConfig.setLookupDataset(dataset);
        enrichment.setLookupConfig(lookupConfig);

        // Set field mappings
        List<YamlEnrichment.FieldMapping> fieldMappings = Arrays.asList(
            createFieldMapping("name", "currencyName", false, null),
            createFieldMapping("decimalPlaces", "currencyDecimals", false, 2),
            createFieldMapping("region", "currencyRegion", false, "Unknown"),
            createFieldMapping("isActive", "isActive", false, false)
        );
        enrichment.setFieldMappings(fieldMappings);

        return enrichment;
    }

    /**
     * Create country enrichment with inline dataset.
     */
    private YamlEnrichment createCountryEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("country-enrichment");
        enrichment.setName("Country Enrichment");
        enrichment.setType("lookup-enrichment");
        enrichment.setEnabled(true);
        enrichment.setPriority(11);
        enrichment.setCondition("#country != null");

        // Create lookup configuration with inline dataset
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupKey("#country");

        // Create inline dataset
        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("code");

        // Create country data
        List<Map<String, Object>> data = Arrays.asList(
            createCountryRecord("US", "United States", "North America"),
            createCountryRecord("DE", "Germany", "Europe"),
            createCountryRecord("GB", "United Kingdom", "Europe"),
            createCountryRecord("JP", "Japan", "Asia")
        );
        dataset.setData(data);

        lookupConfig.setLookupDataset(dataset);
        enrichment.setLookupConfig(lookupConfig);

        // Set field mappings
        List<YamlEnrichment.FieldMapping> fieldMappings = Arrays.asList(
            createFieldMapping("name", "countryName", false, null),
            createFieldMapping("region", "countryRegion", false, "Unknown")
        );
        enrichment.setFieldMappings(fieldMappings);

        return enrichment;
    }

    private Map<String, Object> createCurrencyRecord(String code, String name, int decimals, 
                                                     boolean active, String region) {
        Map<String, Object> record = new HashMap<>();
        record.put("code", code);
        record.put("name", name);
        record.put("decimalPlaces", decimals);
        record.put("isActive", active);
        record.put("region", region);
        return record;
    }

    private Map<String, Object> createCountryRecord(String code, String name, String region) {
        Map<String, Object> record = new HashMap<>();
        record.put("code", code);
        record.put("name", name);
        record.put("region", region);
        return record;
    }

    private YamlEnrichment.FieldMapping createFieldMapping(String sourceField, String targetField, 
                                                          boolean required, Object defaultValue) {
        YamlEnrichment.FieldMapping mapping = new YamlEnrichment.FieldMapping();
        mapping.setSourceField(sourceField);
        mapping.setTargetField(targetField);
        mapping.setRequired(required);
        mapping.setDefaultValue(defaultValue);
        return mapping;
    }

    /**
     * Simple test trade class.
     */
    public static class TestTrade {
        public String currency;
        public String country;
        
        // Fields to be enriched
        public String currencyName;
        public String currencyRegion;
        public Integer currencyDecimals;
        public Boolean isActive;
        
        public String countryName;
        public String countryRegion;
    }
}
