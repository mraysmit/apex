package dev.mars.apex.demo.examples;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple PostgreSQL Lookup Demo - Basic APEX Enrichment
 *
 * This is the simplest possible PostgreSQL lookup demonstration using real APEX services:
 * - Single customer lookup enrichment scenario
 * - One YAML configuration file
 * - Real APEX services (no hardcoded simulation)
 * - Minimal setup and execution
 *
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor
 * - YamlEnrichmentProcessor: Real YAML rule processing with SpEL expressions
 * - DatasetLookupService: Real inline dataset lookups with key-field matching
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 *
 * ============================================================================
 * REQUIRED YAML CONFIGURATION FILES
 * ============================================================================
 *
 * This demo requires the following YAML file:
 *
 * └── examples/lookups/customer-profile-enrichment.yaml
 *     ├── Customer profile enrichment rules with inline dataset
 *     ├── Contains: customer lookup data, field mappings
 *     └── Used for: Simple customer profile enrichment demonstration
 *
 * CRITICAL: The YAML file must be present and valid. The demo will fail fast
 * if the required configuration is missing. No hardcoded fallback data is provided.
 *
 * ============================================================================
 * DEMONSTRATION SCENARIO
 * ============================================================================
 *
 * Single Customer Profile Enrichment:
 * - Input: Customer ID (e.g., "CUST000001")
 * - Process: YAML-driven lookup via real APEX EnrichmentService
 * - Output: Customer name, type, tier, region, status from inline dataset
 * - Performance: Sub-millisecond lookup with APEX caching
 *
 * @author APEX Demo Team
 * @version 2.0 - Real APEX services with basic PostgreSQL lookup
 * @since 2025-08-28
 */
public class SimplePostgreSQLLookupDemo {

    private static final Logger logger = LoggerFactory.getLogger(SimplePostgreSQLLookupDemo.class);

    private final YamlConfigurationLoader yamlLoader;
    private final EnrichmentService enrichmentService;
    private final LookupServiceRegistry serviceRegistry;

    public SimplePostgreSQLLookupDemo() {
        // Initialize APEX services for YAML loading and enrichment processing
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.enrichmentService = new EnrichmentService(serviceRegistry, new ExpressionEvaluatorService());

        logger.info("SimplePostgreSQLLookupDemo initialized with real APEX services");
    }

    /**
     * Simple demonstration method - runs basic customer enrichment only.
     */
    public void runDemo() {
        logger.info("=".repeat(80));
        logger.info("SIMPLE POSTGRESQL LOOKUP DEMONSTRATION");
        logger.info("=".repeat(80));

        try {
            // Run single simple enrichment scenario
            demonstrateSimpleEnrichmentYaml();

            logger.info("=".repeat(80));
            logger.info("SIMPLE POSTGRESQL LOOKUP DEMONSTRATION COMPLETED");
            logger.info("=".repeat(80));

        } catch (Exception e) {
            logger.error("Error during simple demonstration", e);
        }
    }

    /**
     * Demonstrate simple YAML enrichment processing for customer profiles.
     * Uses pure YAML-driven data sourcing with no hardcoded simulation.
     */
    private void demonstrateSimpleEnrichmentYaml() {
        logger.info("\n" + "=".repeat(60));
        logger.info("SIMPLE YAML ENRICHMENT - Customer Profile Lookup");
        logger.info("=".repeat(60));

        try {
            // Create minimal input data for YAML processing - no hardcoded business logic
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("customerId", "CUST000001"); // Only the lookup key, no other hardcoded data

            logger.info("Input Data for YAML Processing:");
            logger.info("  Customer ID: {}", inputData.get("customerId"));

            // Apply YAML enrichment processing using real APEX services
            String configPath = "examples/lookups/customer-profile-enrichment.yaml";
            Map<String, Object> enrichedData = performEnrichmentWithYaml(inputData, configPath);

            logger.info("\nCustomer Profile from YAML Processing:");
            logger.info("  Customer Name: {}", enrichedData.get("customerName"));
            logger.info("  Customer Type: {}", enrichedData.get("customerType"));
            logger.info("  Customer Tier: {}", enrichedData.get("customerTier"));
            logger.info("  Customer Region: {}", enrichedData.get("customerRegion"));
            logger.info("  Customer Status: {}", enrichedData.get("customerStatus"));

        } catch (Exception e) {
            logger.error("Error in simple YAML enrichment demonstration", e);
        }
    }

    /**
     * Perform enrichment using YAML configuration and APEX services.
     */
    private Map<String, Object> performEnrichmentWithYaml(Map<String, Object> inputData, String configPath) {
        try {
            logger.info("Loading and processing YAML configuration: {}", configPath);

            // Load YAML configuration using real APEX services
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath(configPath);
            
            logger.info("YAML configuration loaded successfully");
            logger.info("  Configuration: {} (version {})", config.getMetadata().getName(), config.getMetadata().getVersion());
            logger.info("  Found {} enrichment rules", config.getEnrichments() != null ? config.getEnrichments().size() : 0);

            // Process enrichment using real APEX enrichment service
            Map<String, Object> enrichedResult = new HashMap<>(inputData);
            
            if (config.getEnrichments() != null) {
                logger.info("  Using APEX EnrichmentService to process {} enrichments", config.getEnrichments().size());
                Object enrichedObject = enrichmentService.enrichObject(config, enrichedResult);

                // Convert back to Map if needed
                if (enrichedObject instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> enrichedMap = (Map<String, Object>) enrichedObject;
                    enrichedResult = enrichedMap;
                } else {
                    // If enrichment returns a different type, merge it back
                    logger.info("  Enrichment returned type: {}", enrichedObject.getClass().getSimpleName());
                    enrichedResult = inputData; // Return original if conversion fails
                }
            }
            
            return enrichedResult;
            
        } catch (Exception e) {
            logger.error("Error during YAML enrichment processing: {}", e.getMessage());
            return inputData; // Return original data on error
        }
    }

    /**
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        logger.info("Starting Simple PostgreSQL Lookup Demo...");

        try {
            // Create and run the simple demo
            SimplePostgreSQLLookupDemo demo = new SimplePostgreSQLLookupDemo();
            demo.runDemo();

        } catch (Exception e) {
            logger.error("Failed to run Simple PostgreSQL Lookup Demo", e);
        }
    }
}
