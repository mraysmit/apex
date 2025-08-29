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
 * Working demonstration of external data-source reference system.
 * 
 * This demo ensures database initialization happens before any external data-source connections.
 * 
 * @author APEX Demo Team
 * @since 2025-08-28
 * @version 1.0.0
 */
public class ExternalDataSourceWorkingDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalDataSourceWorkingDemo.class);
    
    private final YamlConfigurationLoader configLoader;
    private final EnrichmentService enrichmentService;
    
    public ExternalDataSourceWorkingDemo() {
        this.configLoader = new YamlConfigurationLoader();
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        this.enrichmentService = new EnrichmentService(serviceRegistry, new ExpressionEvaluatorService());
        logger.info("ExternalDataSourceWorkingDemo initialized");
    }
    
    public static void main(String[] args) {
        ExternalDataSourceWorkingDemo demo = new ExternalDataSourceWorkingDemo();
        demo.runDemo();
    }
    
    public void runDemo() {
        logger.info("====================================================================================");
        logger.info("EXTERNAL DATA-SOURCE REFERENCE WORKING DEMONSTRATION");
        logger.info("====================================================================================");
        
        try {
            // Step 1: Initialize database FIRST (before any APEX services are used)
            initializeDatabaseEarly();
            
            // Step 2: Verify database has data
            verifyDatabaseData();
            
            // Step 3: Test external data-source reference enrichment
            testExternalReferenceEnrichment();
            
            logger.info("====================================================================================");
            logger.info("EXTERNAL DATA-SOURCE REFERENCE WORKING DEMONSTRATION COMPLETED SUCCESSFULLY!");
            logger.info("====================================================================================");
            
        } catch (Exception e) {
            logger.error("Demo failed: " + e.getMessage(), e);
        }
    }
    
    private void initializeDatabaseEarly() throws Exception {
        logger.info("Step 1: Initializing H2 database BEFORE any APEX services...");

        // CRITICAL: Load H2 driver explicitly to ensure it's available for DataSourceFactory
        try {
            Class.forName("org.h2.Driver");
            logger.info("✅ H2 driver loaded successfully");
        } catch (ClassNotFoundException e) {
            throw new Exception("❌ H2 driver not found in classpath: " + e.getMessage());
        }

        // Use the exact same JDBC URL as the external data-source configuration
        String jdbcUrl = "jdbc:h2:mem:apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        logger.info("Database URL: " + jdbcUrl);
        
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
                ('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2023-01-15'),
                ('CUST000002', 'Global Industries Ltd', 'CORPORATE', 'GOLD', 'EU', 'ACTIVE', '2023-02-20'),
                ('CUST000003', 'Tech Innovations Inc', 'CORPORATE', 'SILVER', 'APAC', 'ACTIVE', '2023-03-10')
            """);
            
            // Create counterparties table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS counterparties (
                    counterparty_id VARCHAR(20) PRIMARY KEY,
                    counterparty_name VARCHAR(100) NOT NULL,
                    counterparty_type VARCHAR(20) NOT NULL,
                    credit_rating VARCHAR(10),
                    status VARCHAR(20) NOT NULL,
                    created_date DATE NOT NULL
                )
            """);
            
            // Insert counterparty data
            statement.execute("""
                INSERT INTO counterparties (counterparty_id, counterparty_name, counterparty_type, credit_rating, status, created_date) VALUES
                ('CP_GS', 'Goldman Sachs', 'INVESTMENT_BANK', 'A+', 'ACTIVE', '2023-01-01'),
                ('CP_MS', 'Morgan Stanley', 'INVESTMENT_BANK', 'A', 'ACTIVE', '2023-01-01')
            """);
            
            logger.info("✅ Database initialized successfully with all tables and data");
        }
    }
    
    private void verifyDatabaseData() throws Exception {
        logger.info("\nStep 2: Verifying database data...");
        
        String jdbcUrl = "jdbc:h2:mem:apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();
            
            // Verify customers
            var rs = statement.executeQuery("SELECT COUNT(*) FROM customers WHERE status = 'ACTIVE'");
            rs.next();
            int customerCount = rs.getInt(1);
            logger.info("✅ Found " + customerCount + " active customers");
            
            // Verify specific customer
            rs = statement.executeQuery("SELECT customer_name FROM customers WHERE customer_id = 'CUST000001' AND status = 'ACTIVE'");
            if (rs.next()) {
                logger.info("✅ Found customer CUST000001: " + rs.getString("customer_name"));
            } else {
                throw new Exception("❌ Customer CUST000001 not found!");
            }
            
            // Verify counterparties
            rs = statement.executeQuery("SELECT COUNT(*) FROM counterparties WHERE status = 'ACTIVE'");
            rs.next();
            int counterpartyCount = rs.getInt(1);
            logger.info("✅ Found " + counterpartyCount + " active counterparties");
        }
    }
    
    private void testExternalReferenceEnrichment() throws Exception {
        logger.info("\nStep 3: Testing external data-source reference enrichment...");
        
        // Input data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("customerId", "CUST000001");
        
        logger.info("Input: customerId = " + inputData.get("customerId"));
        
        // Load configuration with external data-source references
        logger.info("Loading external reference configuration...");
        YamlRuleConfiguration config = configLoader.loadFromClasspath("lookup/customer-profile-enrichment.yaml");
        
        logger.info("Configuration loaded:");
        logger.info("  Name: " + config.getMetadata().getName());
        logger.info("  Enrichments: " + config.getEnrichments().size());
        logger.info("  Data Sources: " + (config.getDataSources() != null ? config.getDataSources().size() : 0));
        
        // Process enrichment
        logger.info("Processing enrichment with APEX EnrichmentService...");
        Map<String, Object> enrichedResult = new HashMap<>(inputData);
        enrichmentService.enrichObject(config, enrichedResult);
        
        // Display results
        logger.info("\n🎉 ENRICHMENT RESULTS:");
        logger.info("  Customer Name: " + enrichedResult.get("customerName"));
        logger.info("  Customer Type: " + enrichedResult.get("customerType"));
        logger.info("  Customer Tier: " + enrichedResult.get("customerTier"));
        logger.info("  Customer Region: " + enrichedResult.get("customerRegion"));
        logger.info("  Customer Status: " + enrichedResult.get("customerStatus"));
        
        // Validate results
        if (enrichedResult.get("customerName") != null) {
            logger.info("✅ SUCCESS: External data-source reference enrichment working!");
        } else {
            throw new Exception("❌ FAILED: External data-source reference enrichment not working!");
        }
    }
}
