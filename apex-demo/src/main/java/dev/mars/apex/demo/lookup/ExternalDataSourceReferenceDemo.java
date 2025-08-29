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
 * Demonstration of external data-source reference functionality.
 * 
 * This demo showcases the new capability to separate infrastructure configuration
 * (data-sources) from business logic configuration (enrichments) using external
 * references.
 * 
 * Key Features Demonstrated:
 * - External data-source YAML configurations
 * - Lean enrichment configurations with data-source references
 * - Named query references from external configurations
 * - Separation of concerns between infrastructure and business logic
 * 
 * @author APEX Demo Team
 * @since 2025-08-28
 * @version 1.0.0
 */
public class ExternalDataSourceReferenceDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalDataSourceReferenceDemo.class);
    
    private final YamlConfigurationLoader configLoader;
    private final EnrichmentService enrichmentService;
    private final LookupServiceRegistry serviceRegistry;

    public ExternalDataSourceReferenceDemo() {
        this.configLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.enrichmentService = new EnrichmentService(serviceRegistry, new ExpressionEvaluatorService());
        logger.info("ExternalDataSourceReferenceDemo initialized with APEX services");
    }
    
    public static void main(String[] args) {
        ExternalDataSourceReferenceDemo demo = new ExternalDataSourceReferenceDemo();
        demo.runDemo();
    }
    
    public void runDemo() {
        logger.info("====================================================================================");
        logger.info("EXTERNAL DATA-SOURCE REFERENCE DEMONSTRATION");
        logger.info("====================================================================================");
        
        try {
            // Initialize database
            initializeDatabase();
            
            // Demo 1: Customer Profile Enrichment with External Reference
            demonstrateCustomerProfileEnrichment();
            
            // Demo 2: Settlement Instruction Enrichment with External Reference
            demonstrateSettlementInstructionEnrichment();
            
            logger.info("====================================================================================");
            logger.info("EXTERNAL DATA-SOURCE REFERENCE DEMONSTRATION completed successfully!");
            logger.info("====================================================================================");
            
        } catch (Exception e) {
            logger.error("Demo failed with error: " + e.getMessage(), e);
        }
    }
    
    private void initializeDatabase() throws Exception {
        logger.info("Initializing H2 database with shared access...");
        
        // Create H2 database connection
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
            
            // Insert customer data
            statement.execute("""
                INSERT INTO customers (customer_id, customer_name, customer_type, tier, region, status, created_date) VALUES
                ('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2023-01-15'),
                ('CUST000002', 'Global Industries Ltd', 'CORPORATE', 'GOLD', 'EU', 'ACTIVE', '2023-02-20'),
                ('CUST000003', 'Tech Innovations Inc', 'CORPORATE', 'SILVER', 'APAC', 'ACTIVE', '2023-03-10'),
                ('CUST000004', 'Financial Services Co', 'INSTITUTIONAL', 'PLATINUM', 'NA', 'ACTIVE', '2023-04-05'),
                ('CUST000005', 'Investment Partners', 'INSTITUTIONAL', 'GOLD', 'EU', 'ACTIVE', '2023-05-12')
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
                ('CP_MS', 'Morgan Stanley', 'INVESTMENT_BANK', 'A', 'ACTIVE', '2023-01-01'),
                ('CP_JPM', 'JPMorgan Chase', 'COMMERCIAL_BANK', 'A+', 'ACTIVE', '2023-01-01'),
                ('CP_CITI', 'Citigroup', 'COMMERCIAL_BANK', 'A-', 'ACTIVE', '2023-01-01'),
                ('CP_BOA', 'Bank of America', 'COMMERCIAL_BANK', 'A', 'ACTIVE', '2023-01-01')
            """);
            
            logger.info("Database initialized successfully with customer and counterparty data");
        }
    }
    
    private void demonstrateCustomerProfileEnrichment() {
        logger.info("\n============================================================");
        logger.info("1. CUSTOMER PROFILE ENRICHMENT - External Data-Source Reference");
        logger.info("============================================================");
        
        try {
            // Input data
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("customerId", "CUST000001");
            
            logger.info("Input Data:");
            logger.info("  Customer ID: " + inputData.get("customerId"));
            
            // Load lean enrichment configuration with external data-source reference
            logger.info("Loading lean enrichment configuration: lookup/enrichments/customer-profile-enrichment-lean.yaml");
            YamlRuleConfiguration config = configLoader.loadFromClasspath("lookup/enrichments/customer-profile-enrichment-lean.yaml");
            
            logger.info("Configuration loaded successfully:");
            logger.info("  Configuration: " + config.getMetadata().getName() + " (version " + config.getMetadata().getVersion() + ")");
            logger.info("  Found " + config.getEnrichments().size() + " enrichment rules");
            logger.info("  Found " + (config.getDataSourceRefs() != null ? config.getDataSourceRefs().size() : 0) + " external data-source references");
            
            // Process enrichments
            logger.info("Processing enrichments using APEX EnrichmentService...");
            Map<String, Object> enrichedResult = new HashMap<>(inputData);
            Object enrichedObject = enrichmentService.enrichObject(config, enrichedResult);

            // Convert back to Map if needed
            Map<String, Object> enrichedData = enrichedResult;
            if (enrichedObject instanceof Map) {
                enrichedData = (Map<String, Object>) enrichedObject;
            }

            // Display results
            logger.info("\nCustomer Profile from External Data-Source Reference:");
            logger.info("  Customer Name: " + enrichedData.get("customerName"));
            logger.info("  Customer Type: " + enrichedData.get("customerType"));
            logger.info("  Customer Tier: " + enrichedData.get("customerTier"));
            logger.info("  Customer Region: " + enrichedData.get("customerRegion"));
            logger.info("  Customer Status: " + enrichedData.get("customerStatus"));
            
        } catch (Exception e) {
            logger.error("Customer profile enrichment demo failed: " + e.getMessage(), e);
        }
    }
    
    private void demonstrateSettlementInstructionEnrichment() {
        logger.info("\n============================================================");
        logger.info("2. SETTLEMENT INSTRUCTION ENRICHMENT - External Data-Source Reference");
        logger.info("============================================================");
        
        try {
            // Input data
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("counterpartyId", "CP_GS");
            
            logger.info("Input Data:");
            logger.info("  Counterparty ID: " + inputData.get("counterpartyId"));
            
            // Load lean enrichment configuration with external data-source reference
            logger.info("Loading lean enrichment configuration: lookup/enrichments/settlement-instruction-enrichment-lean.yaml");
            YamlRuleConfiguration config = configLoader.loadFromClasspath("lookup/enrichments/settlement-instruction-enrichment-lean.yaml");
            
            logger.info("Configuration loaded successfully:");
            logger.info("  Configuration: " + config.getMetadata().getName() + " (version " + config.getMetadata().getVersion() + ")");
            logger.info("  Found " + config.getEnrichments().size() + " enrichment rules");
            logger.info("  Found " + (config.getDataSourceRefs() != null ? config.getDataSourceRefs().size() : 0) + " external data-source references");
            
            // Process enrichments
            logger.info("Processing enrichments using APEX EnrichmentService...");
            Map<String, Object> enrichedResult = new HashMap<>(inputData);
            Object enrichedObject = enrichmentService.enrichObject(config, enrichedResult);

            // Convert back to Map if needed
            Map<String, Object> enrichedData = enrichedResult;
            if (enrichedObject instanceof Map) {
                enrichedData = (Map<String, Object>) enrichedObject;
            }

            // Display results
            logger.info("\nSettlement Instructions from External Data-Source Reference:");
            logger.info("  Counterparty Name: " + enrichedData.get("counterpartyName"));
            logger.info("  Counterparty Type: " + enrichedData.get("counterpartyType"));
            logger.info("  Credit Rating: " + enrichedData.get("creditRating"));
            
        } catch (Exception e) {
            logger.error("Settlement instruction enrichment demo failed: " + e.getMessage(), e);
        }
    }
}
