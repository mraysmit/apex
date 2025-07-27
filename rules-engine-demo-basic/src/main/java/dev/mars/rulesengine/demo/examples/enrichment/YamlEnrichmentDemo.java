package dev.mars.rulesengine.demo.examples.enrichment;

import dev.mars.rulesengine.core.config.yaml.YamlConfigurationLoader;
import dev.mars.rulesengine.core.config.yaml.YamlRuleConfiguration;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import dev.mars.rulesengine.core.service.enrichment.EnrichmentService;
import dev.mars.rulesengine.core.service.lookup.LookupService;
import dev.mars.rulesengine.core.service.lookup.LookupServiceRegistry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive demonstration of YAML-based enrichment system.
 * This demo shows how YAML enrichment configurations are executed at runtime.
 */
public class YamlEnrichmentDemo {

    public static void main(String[] args) {
        System.out.println("=== YAML ENRICHMENT SYSTEM DEMO ===");
        System.out.println("Demonstrating runtime execution of YAML-defined enrichments\n");

        YamlEnrichmentDemo demo = new YamlEnrichmentDemo();
        
        try {
            // Step 1: Set up lookup services
            demo.setupLookupServices();
            
            // Step 2: Load YAML configuration
            demo.loadYamlConfiguration();
            
            // Step 3: Execute enrichments
            demo.executeEnrichments();
            
            // Step 4: Show performance metrics
            demo.showPerformanceMetrics();
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== YAML ENRICHMENT BENEFITS ===");
        System.out.println("✓ Configuration-Driven: Enrichments defined in YAML, not code");
        System.out.println("✓ Runtime Execution: Actual lookup and field mapping operations");
        System.out.println("✓ Service Discovery: Automatic resolution of lookup services by name");
        System.out.println("✓ Caching Support: Built-in caching with TTL for performance");
        System.out.println("✓ Field Mapping: Flexible source-to-target field transformations");
        System.out.println("✓ Condition Evaluation: SpEL-based conditional enrichment execution");
        
        System.out.println("\n=== DEMO COMPLETED ===");
    }

    private LookupServiceRegistry serviceRegistry;
    private EnrichmentService enrichmentService;
    private YamlRuleConfiguration yamlConfig;

    /**
     * Set up lookup services that will be resolved by name from YAML configuration.
     */
    private void setupLookupServices() {
        System.out.println("=== STEP 1: SETTING UP LOOKUP SERVICES ===");
        System.out.println("Registering lookup services that match YAML configuration names\n");

        serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);

        // Create counterparty lookup service
        LookupService counterpartyService = new LookupService(
            "counterpartyLookupService",  // This name must match YAML configuration
            Arrays.asList("CPTY001", "CPTY002", "CPTY003")
        );

        // Set up enrichment data
        Map<String, Object> counterpartyData = new HashMap<>();
        counterpartyData.put("CPTY001", createCounterpartyData("Goldman Sachs International", "A+", "W22LROWP2IHZNBB6K528", "UK"));
        counterpartyData.put("CPTY002", createCounterpartyData("JP Morgan Chase Bank", "AA-", "7H6GLXDRUGQFU57RNE97", "US"));
        counterpartyData.put("CPTY003", createCounterpartyData("Deutsche Bank AG", "A-", "7LTWFZYICNSX8D621K86", "DE"));
        counterpartyService.setEnrichmentData(counterpartyData);

        serviceRegistry.registerService(counterpartyService);
        System.out.println("✓ Registered counterpartyLookupService with 3 counterparties");

        // Create currency lookup service
        LookupService currencyService = new LookupService(
            "currencyLookupService",  // This name must match YAML configuration
            Arrays.asList("USD", "EUR", "GBP", "JPY")
        );

        Map<String, Object> currencyData = new HashMap<>();
        currencyData.put("USD", createCurrencyData("US Dollar", 2, true));
        currencyData.put("EUR", createCurrencyData("Euro", 2, true));
        currencyData.put("GBP", createCurrencyData("British Pound", 2, true));
        currencyData.put("JPY", createCurrencyData("Japanese Yen", 0, true));
        currencyService.setEnrichmentData(currencyData);

        serviceRegistry.registerService(currencyService);
        System.out.println("✓ Registered currencyLookupService with 4 currencies");

        // Create commodity lookup service
        LookupService commodityService = new LookupService(
            "commodityLookupService",  // This name must match YAML configuration
            Arrays.asList("BRENT_CRUDE", "WTI_CRUDE", "NATURAL_GAS")
        );

        Map<String, Object> commodityData = new HashMap<>();
        commodityData.put("BRENT_CRUDE", createCommodityData("ICE Brent Crude Oil", "ENERGY", "BBL", "ICE"));
        commodityData.put("WTI_CRUDE", createCommodityData("NYMEX WTI Crude Oil", "ENERGY", "BBL", "NYMEX"));
        commodityData.put("NATURAL_GAS", createCommodityData("Henry Hub Natural Gas", "ENERGY", "MMBTU", "NYMEX"));
        commodityService.setEnrichmentData(commodityData);

        serviceRegistry.registerService(commodityService);
        System.out.println("✓ Registered commodityLookupService with 3 commodities");

        System.out.println("\nService registry populated with " + 
                          serviceRegistry.getCacheStatistics().get("expressionCacheSize") + " services\n");
    }

    /**
     * Load YAML configuration containing enrichment definitions.
     */
    private void loadYamlConfiguration() throws Exception {
        System.out.println("=== STEP 2: LOADING YAML CONFIGURATION ===");
        System.out.println("Loading enrichment configurations from YAML file\n");

        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        yamlConfig = loader.loadFromFile("config/financial-enrichment-rules.yaml");

        System.out.println("✓ Loaded YAML configuration: " + yamlConfig.getMetadata().getName());
        System.out.println("  Version: " + yamlConfig.getMetadata().getVersion());
        System.out.println("  Enrichments found: " + 
                          (yamlConfig.getEnrichments() != null ? yamlConfig.getEnrichments().size() : 0));

        if (yamlConfig.getEnrichments() != null) {
            yamlConfig.getEnrichments().forEach(enrichment -> {
                System.out.println("    • " + enrichment.getId() + " (" + enrichment.getType() + ")");
            });
        }

        System.out.println();
    }

    /**
     * Execute enrichments on sample financial objects.
     */
    private void executeEnrichments() {
        System.out.println("=== STEP 3: EXECUTING ENRICHMENTS ===");
        System.out.println("Applying YAML-defined enrichments to financial objects\n");

        // Test Case 1: OTC Commodity Total Return Swap
        System.out.println("Test Case 1: OTC Commodity Total Return Swap");
        CommodityTotalReturnSwap swap = new CommodityTotalReturnSwap();
        swap.counterpartyId = "CPTY001";
        swap.notionalCurrency = "USD";
        swap.referenceIndex = "BRENT_CRUDE";
        swap.notionalAmount = 10000000.0;

        System.out.println("  Before enrichment:");
        System.out.println("    Counterparty ID: " + swap.counterpartyId);
        System.out.println("    Currency: " + swap.notionalCurrency);
        System.out.println("    Reference Index: " + swap.referenceIndex);

        // Apply enrichments
        Object enrichedSwap = enrichmentService.enrichObject(yamlConfig, swap);

        System.out.println("  After enrichment:");
        System.out.println("    Counterparty Name: " + swap.counterpartyName);
        System.out.println("    Counterparty Rating: " + swap.counterpartyRating);
        System.out.println("    Counterparty LEI: " + swap.counterpartyLei);
        System.out.println("    Currency Name: " + swap.currencyName);
        System.out.println("    Currency Decimals: " + swap.currencyDecimalPlaces);
        System.out.println("    Reference Index Name: " + swap.referenceIndexName);
        System.out.println("    Commodity Type: " + swap.commodityType);
        System.out.println("    Commodity Unit: " + swap.commodityUnit);

        // Test Case 2: Regular Trade
        System.out.println("\nTest Case 2: Regular Trade");
        Trade trade = new Trade();
        trade.counterpartyId = "CPTY002";
        trade.notionalCurrency = "EUR";

        System.out.println("  Before enrichment:");
        System.out.println("    Counterparty ID: " + trade.counterpartyId);
        System.out.println("    Currency: " + trade.notionalCurrency);

        // Apply enrichments
        Object enrichedTrade = enrichmentService.enrichObject(yamlConfig, trade);

        System.out.println("  After enrichment:");
        System.out.println("    Counterparty Name: " + trade.counterpartyName);
        System.out.println("    Counterparty Rating: " + trade.counterpartyRating);
        System.out.println("    Currency Name: " + trade.currencyName);
        System.out.println("    Currency Active: " + trade.currencyActive);

        // Test Case 3: Conditional Enrichment (missing data)
        System.out.println("\nTest Case 3: Conditional Enrichment (missing counterparty)");
        Trade incompleteTrade = new Trade();
        incompleteTrade.counterpartyId = null;  // This should prevent counterparty enrichment
        incompleteTrade.notionalCurrency = "GBP";

        System.out.println("  Before enrichment:");
        System.out.println("    Counterparty ID: " + incompleteTrade.counterpartyId);
        System.out.println("    Currency: " + incompleteTrade.notionalCurrency);

        // Apply enrichments
        Object enrichedIncompleteTrade = enrichmentService.enrichObject(yamlConfig, incompleteTrade);

        System.out.println("  After enrichment:");
        System.out.println("    Counterparty Name: " + incompleteTrade.counterpartyName + " (should be null - condition not met)");
        System.out.println("    Currency Name: " + incompleteTrade.currencyName + " (should be enriched)");

        System.out.println();
    }

    /**
     * Show performance metrics and caching statistics.
     */
    private void showPerformanceMetrics() {
        System.out.println("=== STEP 4: PERFORMANCE METRICS ===");
        System.out.println("Showing caching and performance statistics\n");

        Map<String, Object> stats = enrichmentService.getStatistics();
        System.out.println("Cache Statistics:");
        System.out.println("  Lookup cache size: " + stats.get("cacheSize"));
        System.out.println("  Expression cache size: " + stats.get("expressionCacheSize"));
        System.out.println("  Expired entries: " + stats.get("expiredEntries"));

        System.out.println("\nKey Insight: Caching improves performance for repeated lookups!");
        System.out.println();
    }

    // Helper methods for creating test data
    private Map<String, Object> createCounterpartyData(String name, String rating, String lei, String jurisdiction) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("rating", rating);
        data.put("lei", lei);
        data.put("jurisdiction", jurisdiction);
        return data;
    }

    private Map<String, Object> createCurrencyData(String name, int decimalPlaces, boolean isActive) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("decimalPlaces", decimalPlaces);
        data.put("isActive", isActive);
        return data;
    }

    private Map<String, Object> createCommodityData(String indexName, String commodityType, String unit, String exchange) {
        Map<String, Object> data = new HashMap<>();
        data.put("indexName", indexName);
        data.put("commodityType", commodityType);
        data.put("unit", unit);
        data.put("exchange", exchange);
        return data;
    }

    /**
     * Test trade class.
     */
    public static class Trade {
        public String counterpartyId;
        public String notionalCurrency;
        
        // Fields to be enriched
        public String counterpartyName;
        public String counterpartyRating;
        public String counterpartyLei;
        public String counterpartyJurisdiction;
        
        public String currencyName;
        public Integer currencyDecimalPlaces;
        public Boolean currencyActive;
    }

    /**
     * Test commodity swap class.
     */
    public static class CommodityTotalReturnSwap extends Trade {
        public String referenceIndex;
        public Double notionalAmount;
        
        // Additional fields to be enriched
        public String referenceIndexName;
        public String commodityType;
        public String commodityUnit;
        public String referenceExchange;
    }
}
