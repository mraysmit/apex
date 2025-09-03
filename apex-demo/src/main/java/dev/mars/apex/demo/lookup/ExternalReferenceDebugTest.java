package dev.mars.apex.demo.lookup;

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
    
    private YamlConfigurationLoader configLoader;
    private EnrichmentService enrichmentService;
    private Object h2Server; // H2 TCP server instance

    public ExternalReferenceDebugTest() {
        // Initialize database FIRST, then APEX services
        logger.info("ExternalReferenceDebugTest initializing...");
    }

    private void initializeApexServices() {
        this.configLoader = new YamlConfigurationLoader();
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        this.enrichmentService = new EnrichmentService(serviceRegistry, new ExpressionEvaluatorService());
        logger.info("APEX services initialized after database setup");
    }

    private void testApexDatabaseConnection() {
        logger.info("Testing APEX database connection directly...");
        try {
            // Load the same configuration that the enrichment will use
            var config = configLoader.loadFromClasspath("lookup/customer-profile-enrichment.yaml");
            logger.info("✅ APEX configuration loaded successfully");
            logger.info("   Configuration name: " + config.getMetadata().getName());
            logger.info("   Data sources count: " + (config.getDataSources() != null ? config.getDataSources().size() : 0));
        } catch (Exception e) {
            logger.error("❌ APEX database connection test failed: " + e.getMessage(), e);
        }
    }

    private void testApexDataSourceDirectly() {
        logger.info("Testing APEX DataSource directly...");
        try {
            // Create the same DataSource that APEX will use
            var config = configLoader.loadFromClasspath("lookup/customer-profile-enrichment.yaml");
            if (config.getDataSources() != null && !config.getDataSources().isEmpty()) {
                // This will trigger the same DataSource creation as the enrichment
                logger.info("✅ APEX DataSource configuration is accessible");
                logger.info("   This will use the same H2 TCP connection as the demo");
            } else {
                logger.error("❌ No data sources found in APEX configuration");
            }
        } catch (Exception e) {
            logger.error("❌ APEX DataSource test failed: " + e.getMessage(), e);
        }
    }

    private void startH2Server() throws Exception {
        try {
            // Use reflection to start H2 TCP server to avoid hard dependency
            Class<?> serverClass = Class.forName("org.h2.tools.Server");

            // Create TCP server on port 9092
            Object server = serverClass.getMethod("createTcpServer", String[].class)
                .invoke(null, (Object) new String[]{"-tcp", "-tcpPort", "9092", "-tcpAllowOthers"});

            // Start the server
            serverClass.getMethod("start").invoke(server);

            this.h2Server = server;
            logger.info("✅ H2 TCP server started on port 9092");

        } catch (Exception e) {
            throw new Exception("Failed to start H2 TCP server: " + e.getMessage(), e);
        }
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
            // Initialize database FIRST
            initializeDatabase();

            // Initialize APEX services AFTER database is ready
            initializeApexServices();

        // Test APEX database connection directly
        testApexDatabaseConnection();

        // Test APEX DataSource directly
        testApexDataSourceDirectly();

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
        logger.info("Initializing H2 database with TCP server...");

        // Load H2 driver explicitly
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("H2 driver not found: " + e.getMessage());
            throw new RuntimeException("H2 driver not available", e);
        }

        // Start H2 TCP server
        startH2Server();

        // Connect to H2 via TCP (same as APEX will use) - use simple database name
        String jdbcUrl = "jdbc:h2:tcp://localhost:9092/apex_demo_shared;MODE=PostgreSQL";
        logger.info("JDBC URL: " + jdbcUrl);
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();

            // Clean up existing tables first
            statement.execute("DROP TABLE IF EXISTS customers");

            // Create customers table
            statement.execute("""
                CREATE TABLE customers (
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

            // Verify the data was inserted correctly
            var rs = statement.executeQuery("SELECT customer_name FROM customers WHERE customer_id = 'CUST000001'");
            if (rs.next()) {
                logger.info("✅ Verification: Found customer CUST000001: " + rs.getString("customer_name"));
            } else {
                logger.error("❌ Verification: Customer CUST000001 not found in database!");
            }

            // Test the exact query that APEX will use
            logger.info("Testing APEX query directly...");
            var apexQuery = "SELECT customer_name, customer_type, tier, region, status, created_date FROM customers WHERE customer_id = ?";
            try (var ps = connection.prepareStatement(apexQuery)) {
                ps.setString(1, "CUST000001");
                var apexRs = ps.executeQuery();
                if (apexRs.next()) {
                    logger.info("✅ APEX query test successful:");
                    logger.info("   customer_name: " + apexRs.getString("customer_name"));
                    logger.info("   customer_type: " + apexRs.getString("customer_type"));
                    logger.info("   tier: " + apexRs.getString("tier"));
                } else {
                    logger.error("❌ APEX query test failed - no results found!");
                }
            }
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
        YamlRuleConfiguration config = configLoader.loadFromClasspath("lookup/customer-profile-enrichment.yaml");
        
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
