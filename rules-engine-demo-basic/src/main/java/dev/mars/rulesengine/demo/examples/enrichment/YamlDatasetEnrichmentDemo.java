package dev.mars.rulesengine.demo.examples.enrichment;

import dev.mars.rulesengine.core.config.yaml.YamlConfigurationLoader;
import dev.mars.rulesengine.core.config.yaml.YamlRuleConfiguration;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import dev.mars.rulesengine.core.service.enrichment.EnrichmentService;
import dev.mars.rulesengine.core.service.lookup.LookupServiceRegistry;

/**
 * Comprehensive demonstration of YAML dataset-based enrichment system.
 * Shows how small static datasets can be embedded directly in YAML configuration.
 */
public class YamlDatasetEnrichmentDemo {

    public static void main(String[] args) {
        System.out.println("=== YAML DATASET ENRICHMENT DEMO ===");
        System.out.println("Demonstrating inline YAML datasets for financial services enrichment\n");

        YamlDatasetEnrichmentDemo demo = new YamlDatasetEnrichmentDemo();
        
        try {
            // Step 1: Initialize enrichment service (no external services needed!)
            demo.initializeEnrichmentService();
            
            // Step 2: Load YAML configuration with inline datasets
            demo.loadDatasetConfiguration();
            
            // Step 3: Execute dataset-based enrichments
            demo.executeDatasetEnrichments();
            
            // Step 4: Show performance and benefits
            demo.showDatasetBenefits();
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== YAML DATASET BENEFITS ===");
        System.out.println("✓ No External Services: Small datasets embedded in YAML");
        System.out.println("✓ Version Controlled: Datasets stored with configuration");
        System.out.println("✓ Business Editable: Non-technical users can modify data");
        System.out.println("✓ Environment Specific: Different datasets per environment");
        System.out.println("✓ Fast Performance: In-memory lookups with caching");
        System.out.println("✓ Easy Testing: Controlled test data in configuration");
        
        System.out.println("\n=== DEMO COMPLETED ===");
    }

    private EnrichmentService enrichmentService;
    private YamlRuleConfiguration yamlConfig;

    /**
     * Initialize enrichment service - no external lookup services needed!
     */
    private void initializeEnrichmentService() {
        System.out.println("=== STEP 1: INITIALIZING ENRICHMENT SERVICE ===");
        System.out.println("No external lookup services required - datasets are inline!\n");

        // Create empty service registry (datasets don't need external services)
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);

        System.out.println("✓ EnrichmentService initialized");
        System.out.println("✓ Service registry created (empty - datasets are self-contained)");
        System.out.println("✓ Expression evaluator ready for SpEL processing\n");
    }

    /**
     * Load YAML configuration containing inline datasets.
     */
    private void loadDatasetConfiguration() throws Exception {
        System.out.println("=== STEP 2: LOADING DATASET CONFIGURATION ===");
        System.out.println("Loading enrichments with embedded YAML datasets\n");

        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        yamlConfig = loader.loadFromFile("config/financial-dataset-enrichment-rules.yaml");

        System.out.println("✓ Loaded YAML configuration: " + yamlConfig.getMetadata().getName());
        System.out.println("  Version: " + yamlConfig.getMetadata().getVersion());
        System.out.println("  Description: " + yamlConfig.getMetadata().getDescription());
        
        if (yamlConfig.getEnrichments() != null) {
            System.out.println("  Dataset enrichments found: " + yamlConfig.getEnrichments().size());
            yamlConfig.getEnrichments().forEach(enrichment -> {
                System.out.println("    • " + enrichment.getId() + " - " + enrichment.getName());
            });
        }

        System.out.println();
    }

    /**
     * Execute dataset-based enrichments on sample financial objects.
     */
    private void executeDatasetEnrichments() {
        System.out.println("=== STEP 3: EXECUTING DATASET ENRICHMENTS ===");
        System.out.println("Applying inline dataset enrichments to financial trades\n");

        // Test Case 1: Currency enrichment
        System.out.println("Test Case 1: Currency Dataset Enrichment");
        FinancialTrade trade1 = new FinancialTrade();
        trade1.notionalCurrency = "USD";
        trade1.tradingVenue = "XNYS";
        trade1.counterpartyJurisdiction = "US";
        trade1.settlementInstructionCode = "DVP";

        System.out.println("  Before enrichment:");
        System.out.println("    Currency: " + trade1.notionalCurrency);
        System.out.println("    Trading Venue: " + trade1.tradingVenue);
        System.out.println("    Jurisdiction: " + trade1.counterpartyJurisdiction);
        System.out.println("    Settlement Code: " + trade1.settlementInstructionCode);

        // Apply all dataset enrichments
        enrichmentService.enrichObject(yamlConfig, trade1);

        System.out.println("  After enrichment:");
        System.out.println("    Currency Name: " + trade1.currencyName);
        System.out.println("    Currency Region: " + trade1.currencyRegion);
        System.out.println("    Currency Central Bank: " + trade1.currencyCentralBank);
        System.out.println("    Is Major Currency: " + trade1.isMajorCurrency);
        System.out.println("    Venue Name: " + trade1.venueName);
        System.out.println("    Venue Country: " + trade1.venueCountry);
        System.out.println("    Venue Timezone: " + trade1.venueTimezone);
        System.out.println("    Jurisdiction Name: " + trade1.jurisdictionName);
        System.out.println("    Regulatory Regime: " + trade1.regulatoryRegime);
        System.out.println("    Settlement Description: " + trade1.settlementInstructionDescription);
        System.out.println("    Settlement Cycle: " + trade1.settlementCycle);

        // Test Case 2: European trade
        System.out.println("\nTest Case 2: European Trade (EUR/London)");
        FinancialTrade trade2 = new FinancialTrade();
        trade2.notionalCurrency = "EUR";
        trade2.tradingVenue = "XLON";
        trade2.counterpartyJurisdiction = "GB";
        trade2.settlementInstructionCode = "RVP";

        System.out.println("  Before enrichment:");
        System.out.println("    Currency: " + trade2.notionalCurrency);
        System.out.println("    Trading Venue: " + trade2.tradingVenue);
        System.out.println("    Jurisdiction: " + trade2.counterpartyJurisdiction);

        enrichmentService.enrichObject(yamlConfig, trade2);

        System.out.println("  After enrichment:");
        System.out.println("    Currency Name: " + trade2.currencyName);
        System.out.println("    Currency Central Bank: " + trade2.currencyCentralBank);
        System.out.println("    Venue Name: " + trade2.venueName);
        System.out.println("    Venue Operating Hours: " + trade2.venueOperatingHours);
        System.out.println("    Jurisdiction Name: " + trade2.jurisdictionName);
        System.out.println("    MiFID Applicable: " + trade2.mifidApplicable);
        System.out.println("    Settlement Description: " + trade2.settlementInstructionDescription);

        // Test Case 3: Unknown values (default handling)
        System.out.println("\nTest Case 3: Unknown Values (Default Handling)");
        FinancialTrade trade3 = new FinancialTrade();
        trade3.notionalCurrency = "XYZ"; // Unknown currency
        trade3.tradingVenue = "UNKNOWN"; // Unknown venue
        trade3.counterpartyJurisdiction = "ZZ"; // Unknown jurisdiction

        System.out.println("  Before enrichment:");
        System.out.println("    Currency: " + trade3.notionalCurrency + " (unknown)");
        System.out.println("    Trading Venue: " + trade3.tradingVenue + " (unknown)");
        System.out.println("    Jurisdiction: " + trade3.counterpartyJurisdiction + " (unknown)");

        enrichmentService.enrichObject(yamlConfig, trade3);

        System.out.println("  After enrichment (showing default values):");
        System.out.println("    Currency Region: " + trade3.currencyRegion + " (default)");
        System.out.println("    Is Major Currency: " + trade3.isMajorCurrency + " (default)");
        System.out.println("    Venue Country: " + trade3.venueCountry + " (default)");
        System.out.println("    Venue Timezone: " + trade3.venueTimezone + " (default)");
        System.out.println("    Jurisdiction Region: " + trade3.jurisdictionRegion + " (default)");
        System.out.println("    Regulatory Regime: " + trade3.regulatoryRegime + " (default)");

        System.out.println();
    }

    /**
     * Show performance metrics and dataset benefits.
     */
    private void showDatasetBenefits() {
        System.out.println("=== STEP 4: DATASET BENEFITS ANALYSIS ===");
        System.out.println("Analyzing performance and configuration benefits\n");

        System.out.println("Performance Characteristics:");
        System.out.println("  • In-memory lookups: Sub-millisecond response times");
        System.out.println("  • Built-in caching: Configurable TTL per dataset");
        System.out.println("  • No network calls: All data embedded in configuration");
        System.out.println("  • Startup time: Instant - no external service dependencies");

        System.out.println("\nConfiguration Benefits:");
        System.out.println("  • Version control: Datasets stored with application code");
        System.out.println("  • Environment specific: Different data per environment");
        System.out.println("  • Business editable: YAML format accessible to non-developers");
        System.out.println("  • Testing friendly: Controlled test data in configuration");

        System.out.println("\nIdeal Use Cases:");
        System.out.println("  • Currency codes and properties (< 200 records)");
        System.out.println("  • Country/jurisdiction mappings (< 300 records)");
        System.out.println("  • Market identifier codes (< 500 records)");
        System.out.println("  • Settlement instruction codes (< 50 records)");
        System.out.println("  • Regulatory regime mappings (< 100 records)");

        System.out.println("\nWhen NOT to use inline datasets:");
        System.out.println("  • Large datasets (> 1000 records)");
        System.out.println("  • Frequently changing data");
        System.out.println("  • Data requiring real-time updates");
        System.out.println("  • Sensitive data that shouldn't be in configuration");

        System.out.println();
    }

    /**
     * Financial trade class for enrichment demonstration.
     */
    public static class FinancialTrade {
        // Input fields
        public String notionalCurrency;
        public String tradingVenue;
        public String counterpartyJurisdiction;
        public String settlementInstructionCode;
        
        // Currency enrichment fields
        public String currencyName;
        public Integer currencyDecimalPlaces;
        public String currencyRegion;
        public String currencyCentralBank;
        public Boolean isMajorCurrency;
        
        // Venue enrichment fields
        public String venueName;
        public String venueCountry;
        public String venueTimezone;
        public String venueOperatingHours;
        public Boolean isRegulatedMarket;
        
        // Jurisdiction enrichment fields
        public String jurisdictionName;
        public String jurisdictionRegion;
        public String regulatoryRegime;
        public String jurisdictionTimeZone;
        public Boolean mifidApplicable;
        
        // Settlement instruction enrichment fields
        public String settlementInstructionDescription;
        public String settlementCycle;
        public Boolean requiresSettlementConfirmation;
    }
}
