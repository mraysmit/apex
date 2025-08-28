package dev.mars.apex.demo.examples;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Debug test to identify the exact issue with external data-source reference field mapping.
 * 
 * @author APEX Demo Team
 * @since 2025-08-28
 * @version 1.0.0
 */
public class ExternalReferenceDebugTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalReferenceDebugTest.class);
    
    private final YamlConfigurationLoader configLoader;
    private final EnrichmentService enrichmentService;
    
    public ExternalReferenceDebugTest() {
        this.configLoader = new YamlConfigurationLoader();
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        this.enrichmentService = new EnrichmentService(serviceRegistry, new ExpressionEvaluatorService());
        logger.info("ExternalReferenceDebugTest initialized");
    }
    
    public static void main(String[] args) {
        ExternalReferenceDebugTest test = new ExternalReferenceDebugTest();
        test.runDebugTest();
    }
    
    public void runDebugTest() {
        logger.info("====================================================================================");
        logger.info("EXTERNAL REFERENCE DEBUG TEST");
        logger.info("====================================================================================");
        
        try {
            // Initialize database
            initializeDatabase();
            
            // Test external reference enrichment with detailed logging
            testExternalReferenceWithDebugLogging();
            
            logger.info("====================================================================================");
            logger.info("DEBUG TEST COMPLETED");
            logger.info("====================================================================================");
            
        } catch (Exception e) {
            logger.error("Debug test failed: " + e.getMessage(), e);
        }
    }
    
    private void initializeDatabase() throws Exception {
        logger.info("Initializing H2 database...");
        
        String jdbcUrl = "jdbc:h2:mem:apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();
            
            // Create customers table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    customer_id VARCHAR(20) PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    customer_type VARCHAR(20) NOT NULL,
                    tier VARCHAR(20) NOT NULL,
                    region VARCHAR(10) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    created_date DATE NOT NULL,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Insert customer data with ACTIVE status
            statement.execute("""
                INSERT INTO customers (customer_id, customer_name, customer_type, tier, region, status, created_date) VALUES
                ('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2023-01-15')
            """);
            
            logger.info("Database initialized with customer CUST000001");
        }
    }
    
    private void testExternalReferenceWithDebugLogging() throws Exception {
        logger.info("\n============================================================");
        logger.info("DEBUG TEST: External Reference Enrichment");
        logger.info("============================================================");
        
        // Input data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("customerId", "CUST000001");
        
        logger.info("Input Data: " + inputData);
        
        // Load configuration
        YamlRuleConfiguration config = configLoader.loadFromClasspath("enrichments/customer-profile-enrichment-lean.yaml");
        
        logger.info("Configuration loaded:");
        logger.info("  Name: " + config.getMetadata().getName());
        logger.info("  Enrichments: " + config.getEnrichments().size());
        logger.info("  Data Sources: " + (config.getDataSources() != null ? config.getDataSources().size() : 0));
        
        // Log enrichment details
        if (config.getEnrichments() != null && !config.getEnrichments().isEmpty()) {
            var enrichment = config.getEnrichments().get(0);
            logger.info("First Enrichment:");
            logger.info("  ID: " + enrichment.getId());
            logger.info("  Type: " + enrichment.getType());
            logger.info("  Condition: " + enrichment.getCondition());
            
            if (enrichment.getLookupConfig() != null) {
                var lookupConfig = enrichment.getLookupConfig();
                logger.info("  Lookup Key: " + lookupConfig.getLookupKey());
                
                if (lookupConfig.getLookupDataset() != null) {
                    var dataset = lookupConfig.getLookupDataset();
                    logger.info("  Dataset Type: " + dataset.getType());
                    logger.info("  Data Source Ref: " + dataset.getDataSourceRef());
                    logger.info("  Query Ref: " + dataset.getQueryRef());
                    logger.info("  Connection Name: " + dataset.getConnectionName());
                    logger.info("  Query: " + dataset.getQuery());
                    
                    if (dataset.getParameters() != null) {
                        logger.info("  Parameters: " + dataset.getParameters().size());
                        for (var param : dataset.getParameters()) {
                            logger.info("    - Field: " + param.getField() + ", Type: " + param.getType());
                        }
                    }
                }
                
            }

            if (enrichment.getFieldMappings() != null) {
                logger.info("  Field Mappings: " + enrichment.getFieldMappings().size());
                for (var mapping : enrichment.getFieldMappings()) {
                    logger.info("    - " + mapping.getSourceField() + " -> " + mapping.getTargetField() + " (required: " + mapping.getRequired() + ")");
                }
            } else {
                logger.warn("  Field Mappings: NULL - This is the problem!");
            }
        }
        
        // Process enrichment
        logger.info("\nProcessing enrichment...");
        Map<String, Object> enrichedResult = new HashMap<>(inputData);
        
        logger.info("Before enrichment: " + enrichedResult);
        
        try {
            enrichmentService.enrichObject(config, enrichedResult);
            logger.info("After enrichment: " + enrichedResult);
            
            // Check specific fields
            logger.info("\nField-by-field analysis:");
            logger.info("  customerName: " + enrichedResult.get("customerName"));
            logger.info("  customerType: " + enrichedResult.get("customerType"));
            logger.info("  customerTier: " + enrichedResult.get("customerTier"));
            logger.info("  customerRegion: " + enrichedResult.get("customerRegion"));
            logger.info("  customerStatus: " + enrichedResult.get("customerStatus"));
            
            // Check all keys
            logger.info("\nAll keys in result:");
            for (String key : enrichedResult.keySet()) {
                logger.info("  " + key + ": " + enrichedResult.get(key));
            }
            
        } catch (Exception e) {
            logger.error("Enrichment failed: " + e.getMessage(), e);
        }
    }
}
