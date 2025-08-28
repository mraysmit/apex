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
 * Test PostgreSQL external data-source reference system.
 * 
 * @author APEX Demo Team
 * @since 2025-08-28
 * @version 1.0.0
 */
public class PostgreSQLExternalReferenceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLExternalReferenceTest.class);
    
    public static void main(String[] args) {
        PostgreSQLExternalReferenceTest test = new PostgreSQLExternalReferenceTest();
        test.runTest();
    }
    
    public void runTest() {
        logger.info("====================================================================================");
        logger.info("POSTGRESQL EXTERNAL DATA-SOURCE REFERENCE TEST");
        logger.info("====================================================================================");
        
        try {
            // Step 1: Initialize database
            initializeDatabase();
            
            // Step 2: Test external reference enrichment
            testPostgreSQLExternalReference();
            
            logger.info("====================================================================================");
            logger.info("POSTGRESQL EXTERNAL REFERENCE TEST COMPLETED SUCCESSFULLY!");
            logger.info("====================================================================================");
            
        } catch (Exception e) {
            logger.error("PostgreSQL external reference test failed: " + e.getMessage(), e);
        }
    }
    
    private void initializeDatabase() throws Exception {
        logger.info("Step 1: Initializing H2 database (PostgreSQL mode)...");
        
        // Load H2 driver
        Class.forName("org.h2.Driver");
        
        // Create database connection
        String jdbcUrl = "jdbc:h2:mem:apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                
                // Create customers table
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS customers (
                        customer_id VARCHAR(20) PRIMARY KEY,
                        customer_name VARCHAR(100) NOT NULL,
                        customer_type VARCHAR(50) NOT NULL,
                        tier VARCHAR(20) NOT NULL,
                        region VARCHAR(10) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        created_date DATE NOT NULL,
                        last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                
                // Insert test data
                statement.execute("""
                    INSERT INTO customers (customer_id, customer_name, customer_type, tier, region, status, created_date) VALUES
                    ('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2023-01-15'),
                    ('CUST000002', 'Global Industries', 'CORPORATE', 'GOLD', 'EU', 'ACTIVE', '2023-02-20'),
                    ('CUST000003', 'Tech Solutions Inc', 'CORPORATE', 'SILVER', 'APAC', 'ACTIVE', '2023-03-10'),
                    ('CUST000004', 'Inactive Corp', 'CORPORATE', 'BRONZE', 'NA', 'INACTIVE', '2023-01-01')
                """);
                
                logger.info("✅ Database initialized with customer data");
            }
        }
    }
    
    private void testPostgreSQLExternalReference() throws Exception {
        logger.info("\nStep 2: Testing PostgreSQL external data-source reference enrichment...");
        
        // Load configuration with external reference
        YamlConfigurationLoader configLoader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = configLoader.loadFromClasspath("enrichments/postgresql-customer-profile-external-ref.yaml");
        
        logger.info("Configuration loaded:");
        logger.info("  Name: " + config.getMetadata().getName());
        logger.info("  Enrichments: " + (config.getEnrichments() != null ? config.getEnrichments().size() : 0));
        logger.info("  Data Sources: " + (config.getDataSources() != null ? config.getDataSources().size() : 0));
        
        // Create enrichment service with required dependencies
        LookupServiceRegistry lookupRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        EnrichmentService enrichmentService = new EnrichmentService(lookupRegistry, expressionEvaluator);

        // Test data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("customerId", "CUST000002");

        logger.info("Input: customerId = CUST000002");

        // Process enrichment
        Object enrichedResult = enrichmentService.enrichObject(config, inputData);
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) enrichedResult;
        
        // Verify results
        logger.info("\n✅ ENRICHMENT RESULTS:");
        logger.info("  Customer Name: " + enrichedData.get("customerName"));
        logger.info("  Customer Type: " + enrichedData.get("customerType"));
        logger.info("  Customer Tier: " + enrichedData.get("customerTier"));
        logger.info("  Customer Region: " + enrichedData.get("customerRegion"));
        logger.info("  Customer Status: " + enrichedData.get("customerStatus"));
        logger.info("  Valid Customer: " + enrichedData.get("validCustomer"));
        
        // Validate results
        if (enrichedData.get("customerName") != null && 
            enrichedData.get("customerType") != null &&
            enrichedData.get("customerTier") != null) {
            logger.info("✅ SUCCESS: PostgreSQL external data-source reference working!");
        } else {
            throw new Exception("❌ FAILED: PostgreSQL external data-source reference not working!");
        }
    }
}
