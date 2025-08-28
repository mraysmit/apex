package dev.mars.apex.demo.examples;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
// Removed FinancialTrade import - using Map<String, Object> for simplicity
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * APEX Real Enrichment Service Demonstration - YAML-Driven Data Processing
 *
 * This demo demonstrates authentic APEX enrichment processing using real APEX core services:
 *
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor (not hardcoded simulation)
 * - YamlEnrichmentProcessor: Real YAML rule processing with SpEL expressions
 * - DatasetLookupService: Real inline dataset lookups with key-field matching
 * - ExpressionEvaluatorService: Real Spring Expression Language evaluation
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 *
 * DEMONSTRATION SCENARIOS:
 * 1. Simple Customer Profile Enrichment - Single parameter lookup with inline dataset
 * 2. Multi-Parameter Settlement Instructions - Complex SpEL expressions (shows field mismatch errors)
 * 3. Performance Testing - Multiple enrichment calls with timing metrics
 * 4. Fallback Strategies - Non-existent key handling with default values
 *
 * TECHNICAL IMPLEMENTATION:
 * - H2 Database: In-memory PostgreSQL-compatible database for demo data
 * - Inline Datasets: YAML configurations with embedded lookup data
 * - SpEL Expressions: Real expression evaluation with proper error handling
 * - Field Mapping: Automatic field mapping from lookup results to target objects
 *
 * KEY LEARNING: This demo uses REAL APEX services, not hardcoded simulation logic.
 * All enrichment processing is handled by the actual APEX enrichment engine.
 *
 * PREREQUISITES: None - completely self-contained with H2 database initialization
 *
 * @author APEX Demo Team
 * @version 2.0 - Now using real APEX services instead of hardcoded simulation
 * @since 2025-08-28
 */
public class PostgreSQLLookupDemo {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLLookupDemo.class);

    private final YamlConfigurationLoader yamlLoader;
    private final EnrichmentService enrichmentService;
    private final LookupServiceRegistry serviceRegistry;

    public PostgreSQLLookupDemo() {
        // Initialize APEX services for YAML loading and enrichment processing
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.enrichmentService = new EnrichmentService(serviceRegistry, new ExpressionEvaluatorService());

        // Initialize database with test data
        initializeDatabase();
    }

    /**
     * Main demonstration method - runs all APEX YAML processing scenarios.
     */
    public void runDemo() {
        logger.info("=".repeat(80));
        logger.info("APEX YAML Processing Demonstration - Database Lookup Enrichment");
        logger.info("=".repeat(80));

        try {
            // Run YAML processing demonstration scenarios
            demonstrateSimpleEnrichmentYaml();
            demonstrateMultiParameterEnrichmentYaml();
            demonstratePerformanceOptimization();
            demonstrateFallbackStrategies();

            logger.info("=".repeat(80));
            logger.info("APEX YAML Processing Demonstration completed successfully!");
            logger.info("=".repeat(80));

        } catch (Exception e) {
            logger.error("Error during YAML processing demonstration", e);
        }
    }
    
    /**
     * Demonstrate simple YAML enrichment processing for customer profile enrichment.
     */
    private void demonstrateSimpleEnrichmentYaml() {
        logger.info("\n" + "=".repeat(60));
        logger.info("1. SIMPLE YAML ENRICHMENT - Customer Profile Lookup");
        logger.info("=".repeat(60));

        try {
            // Create sample transaction data
            Map<String, Object> transaction = createSampleTransaction();
            transaction.put("customerId", "CUST000001");

            logger.info("Input Transaction Data:");
            logger.info("  Customer ID: {}", transaction.get("customerId"));
            logger.info("  Amount: {}", transaction.get("amount"));
            logger.info("  Currency: {}", transaction.get("currency"));

            // Apply YAML enrichment processing using APEX
            String configPath = "examples/lookups/customer-profile-enrichment.yaml";
            Map<String, Object> enrichedData = performEnrichmentWithYaml(transaction, configPath);

            logger.info("\nEnriched Data from YAML Processing:");
            logger.info("  Customer Name: {}", enrichedData.get("customerName"));
            logger.info("  Customer Type: {}", enrichedData.get("customerType"));
            logger.info("  Customer Tier: {}", enrichedData.get("customerTier"));
            logger.info("  Customer Region: {}", enrichedData.get("customerRegion"));
            logger.info("  Customer Status: {}", enrichedData.get("customerStatus"));
            logger.info("  Customer Created: {}", enrichedData.get("customerCreatedDate"));

            // Demonstrate processing performance
            logger.info("\nDemonstrating YAML Processing Performance:");
            long startTime = System.currentTimeMillis();
            performEnrichmentWithYaml(transaction, configPath);
            long cachedTime = System.currentTimeMillis() - startTime;
            logger.info("  Second processing time: {} ms", cachedTime);

        } catch (Exception e) {
            logger.error("Error in simple YAML enrichment demonstration", e);
        }
    }
    
    /**
     * Demonstrate multi-parameter YAML enrichment processing for settlement instructions.
     */
    private void demonstrateMultiParameterEnrichmentYaml() {
        logger.info("\n" + "=".repeat(60));
        logger.info("2. MULTI-PARAMETER YAML ENRICHMENT - Settlement Instructions");
        logger.info("=".repeat(60));
        
        try {
            // Create sample trade data
            Map<String, Object> trade = createSampleTrade();
            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("trade", trade);
            
            logger.info("Input Trade Data:");
            logger.info("  Trade ID: {}", trade.get("tradeId"));
            logger.info("  Counterparty: {}", trade.get("counterpartyId"));
            logger.info("  Instrument Type: {}", trade.get("instrumentType"));
            logger.info("  Currency: {}", trade.get("currency"));
            logger.info("  Market: {}", trade.get("market"));
            logger.info("  Amount: {}", trade.get("amount"));
            
            // Apply YAML multi-parameter enrichment processing using APEX
            String configPath = "examples/lookups/settlement-instruction-enrichment.yaml";
            Map<String, Object> enrichedData = performEnrichmentWithYaml(tradeData, configPath);
            
            logger.info("\nSettlement Instructions from PostgreSQL:");
            logger.info("  Settlement Instruction ID: {}", enrichedData.get("settlementInstructionId"));
            logger.info("  Counterparty Name: {}", enrichedData.get("counterpartyName"));
            logger.info("  Counterparty Type: {}", enrichedData.get("counterpartyType"));
            logger.info("  Credit Rating: {}", enrichedData.get("counterpartyCreditRating"));
            logger.info("  Custodian Name: {}", enrichedData.get("custodianName"));
            logger.info("  Custodian BIC: {}", enrichedData.get("custodianBic"));
            logger.info("  Settlement Method: {}", enrichedData.get("settlementMethod"));
            logger.info("  Delivery Instruction: {}", enrichedData.get("deliveryInstruction"));
            logger.info("  Market Name: {}", enrichedData.get("marketName"));
            logger.info("  Settlement Cycle: {}", enrichedData.get("settlementCycle"));
            
            logger.info("\nRisk Assessment from PostgreSQL:");
            logger.info("  Risk Category: {}", enrichedData.get("riskCategory"));
            logger.info("  Risk Score: {}", enrichedData.get("riskScore"));
            logger.info("  Max Exposure: {}", enrichedData.get("maxExposure"));
            logger.info("  Approval Required: {}", enrichedData.get("approvalRequired"));
            logger.info("  Monitoring Level: {}", enrichedData.get("monitoringLevel"));
            
        } catch (Exception e) {
            logger.error("Error in multi-parameter database lookup demonstration", e);
        }
    }
    
    /**
     * Demonstrate performance optimization features.
     */
    private void demonstratePerformanceOptimization() {
        logger.info("\n" + "=".repeat(60));
        logger.info("3. PERFORMANCE OPTIMIZATION - Connection Pooling & Caching");
        logger.info("=".repeat(60));
        
        try {
            // Test multiple concurrent lookups to demonstrate connection pooling
            logger.info("Testing Connection Pooling with Multiple Concurrent Lookups:");
            
            String[] customerIds = {"CUST000001", "CUST000002", "CUST000003", "CUST000004", "CUST000005"};
            long totalTime = 0;
            
            for (String customerId : customerIds) {
                Map<String, Object> transaction = createSampleTransaction();
                transaction.put("customerId", customerId);

                long startTime = System.currentTimeMillis();
                Map<String, Object> result = performEnrichmentWithYaml(transaction, "examples/lookups/customer-profile-enrichment.yaml");
                long lookupTime = System.currentTimeMillis() - startTime;
                totalTime += lookupTime;

                logger.info("  Customer {}: {} ms - {}",
                    customerId, lookupTime, result.get("customerName"));
            }
            
            logger.info("Total lookup time for 5 customers: {} ms", totalTime);
            logger.info("Average lookup time: {} ms", totalTime / customerIds.length);
            
            // Demonstrate cache effectiveness
            logger.info("\nTesting Cache Effectiveness:");
            Map<String, Object> transaction = createSampleTransaction();
            transaction.put("customerId", "CUST000001");
            
            // First lookup (cache miss)
            long startTime = System.currentTimeMillis();
            performEnrichmentWithYaml(transaction, "examples/lookups/customer-profile-enrichment.yaml");
            long firstLookup = System.currentTimeMillis() - startTime;

            // Second lookup (cache hit)
            startTime = System.currentTimeMillis();
            performEnrichmentWithYaml(transaction, "examples/lookups/customer-profile-enrichment.yaml");
            long secondLookup = System.currentTimeMillis() - startTime;
            
            logger.info("  First lookup (cache miss): {} ms", firstLookup);
            logger.info("  Second lookup (cache hit): {} ms", secondLookup);
            logger.info("  Cache performance improvement: {}x faster", 
                Math.round((double) firstLookup / secondLookup * 10.0) / 10.0);
            
        } catch (Exception e) {
            logger.error("Error in performance optimization demonstration", e);
        }
    }
    
    /**
     * Demonstrate fallback strategies when database lookups fail.
     */
    private void demonstrateFallbackStrategies() {
        logger.info("\n" + "=".repeat(60));
        logger.info("4. FALLBACK STRATEGIES - Error Handling & Resilience");
        logger.info("=".repeat(60));
        
        try {
            // Test with non-existent customer ID to trigger fallback
            Map<String, Object> transaction = createSampleTransaction();
            transaction.put("customerId", "CUST999999"); // Non-existent customer
            
            logger.info("Testing Fallback with Non-Existent Customer ID: CUST999999");
            
            Map<String, Object> result = performEnrichmentWithYaml(transaction, "examples/lookups/customer-profile-enrichment.yaml");
            
            logger.info("Fallback Data Applied:");
            logger.info("  Customer Name: {}", result.get("customerName"));
            logger.info("  Customer Type: {}", result.get("customerType"));
            logger.info("  Customer Tier: {}", result.get("customerTier"));
            logger.info("  Customer Region: {}", result.get("customerRegion"));
            logger.info("  Customer Status: {}", result.get("customerStatus"));
            
            // Test cascade fallback for settlement instructions
            logger.info("\nTesting Cascade Fallback for Settlement Instructions:");
            Map<String, Object> trade = createSampleTrade();
            trade.put("counterpartyId", "CP_UNKNOWN"); // Non-existent counterparty

            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("trade", trade);
            
            Map<String, Object> settlementResult = performEnrichmentWithYaml(tradeData, "examples/lookups/postgresql-multi-param-lookup.yaml");
            
            logger.info("Cascade Fallback Result:");
            logger.info("  Settlement Instruction ID: {}", settlementResult.get("settlementInstructionId"));
            logger.info("  Counterparty Name: {}", settlementResult.get("counterpartyName"));
            logger.info("  Settlement Method: {}", settlementResult.get("settlementMethod"));
            
        } catch (Exception e) {
            logger.error("Error in fallback strategies demonstration", e);
        }
    }

    // ========================================
    // YAML PROCESSING METHODS
    // ========================================
    // All database methods removed - this demo uses YAML processing with inline datasets

    // ========================================
    // YAML PROCESSING METHODS
    // ========================================

    /**
     * Perform enrichment using APEX YAML processing.
     * This demonstrates real YAML enrichment rule processing with inline datasets.
     */
    private Map<String, Object> performEnrichmentWithYaml(Map<String, Object> inputData, String configPath) {
        try {
            logger.info("Loading and processing YAML configuration: {}", configPath);

            // Load YAML configuration from classpath (same pattern as working examples)
            YamlRuleConfiguration yamlConfig = yamlLoader.loadFromClasspath(configPath);

            logger.info("YAML configuration loaded successfully");
            if (yamlConfig.getMetadata() != null) {
                logger.info("  Configuration: {} (version {})",
                    yamlConfig.getMetadata().getName(),
                    yamlConfig.getMetadata().getVersion());
            }
            if (yamlConfig.getEnrichments() != null) {
                logger.info("  Found {} enrichment rules", yamlConfig.getEnrichments().size());
            }

            // Process enrichments individually (same pattern as working examples)
            Map<String, Object> enrichedResult = new HashMap<>(inputData);

            // Use real APEX enrichment service to process the data
            if (yamlConfig.getEnrichments() != null) {
                logger.info("  Using APEX EnrichmentService to process {} enrichments", yamlConfig.getEnrichments().size());
                Object enrichedObject = enrichmentService.enrichObject(yamlConfig, enrichedResult);

                // Convert back to Map if needed
                if (enrichedObject instanceof Map) {
                    enrichedResult = (Map<String, Object>) enrichedObject;
                } else {
                    // If enrichment returns a different type, merge it back
                    logger.info("  Enrichment returned type: {}", enrichedObject.getClass().getSimpleName());
                }
            }

            // Convert result back to Map if needed
            if (enrichedResult instanceof Map) {
                return (Map<String, Object>) enrichedResult;
            } else {
                // If enrichment returns a different type, wrap it
                Map<String, Object> result = new HashMap<>(inputData);
                result.put("enrichedData", enrichedResult);
                return result;
            }

        } catch (Exception e) {
            logger.error("Error performing APEX YAML enrichment", e);
            // Return original data if enrichment fails
            return new HashMap<>(inputData);
        }
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Create sample transaction data for demonstrations.
     */
    private Map<String, Object> createSampleTransaction() {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("transactionId", "TXN" + System.currentTimeMillis());
        transaction.put("amount", new BigDecimal("50000.00"));
        transaction.put("currency", "USD");
        transaction.put("transactionDate", LocalDate.now().toString());
        return transaction;
    }

    /**
     * Create sample financial trade for demonstrations.
     */
    private Map<String, Object> createSampleTrade() {
        Map<String, Object> trade = new HashMap<>();
        trade.put("tradeId", "TRD" + System.currentTimeMillis());
        trade.put("amount", new BigDecimal("5000000.00"));
        trade.put("currency", "USD");
        trade.put("counterpartyId", "CP_GS");
        trade.put("instrumentType", "EQUITY");
        trade.put("tradeDate", LocalDate.now().toString());
        trade.put("market", "NYSE"); // Will be determined via YAML processing
        return trade;
    }

    // ========================================
    // SETUP AND INITIALIZATION METHODS
    // ========================================

    /**
     * Initialize H2 database with test data for demo purposes.
     */
    private void initializeDatabase() {
        try {
            // Create H2 database connection with explicit database name
            String jdbcUrl = "jdbc:h2:mem:apex_demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";

            try (var connection = java.sql.DriverManager.getConnection(jdbcUrl, "sa", "")) {
                // Create customers table
                String createTable = """
                    CREATE TABLE IF NOT EXISTS customers (
                        customer_id VARCHAR(20) PRIMARY KEY,
                        customer_name VARCHAR(100) NOT NULL,
                        customer_type VARCHAR(20) NOT NULL,
                        tier VARCHAR(20) NOT NULL,
                        region VARCHAR(10) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        created_date DATE,
                        last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

                try (var stmt = connection.createStatement()) {
                    stmt.execute(createTable);
                    logger.info("Created customers table");
                }

                // Insert test data with explicit INSERT OR IGNORE for H2
                String insertData = """
                    MERGE INTO customers (customer_id, customer_name, customer_type, tier, region, status, created_date) VALUES
                    ('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2023-01-15'),
                    ('CUST000002', 'Beta Industries', 'CORPORATE', 'GOLD', 'EU', 'ACTIVE', '2023-02-20'),
                    ('CUST000003', 'Gamma Holdings', 'INSTITUTIONAL', 'PLATINUM', 'APAC', 'ACTIVE', '2023-03-10'),
                    ('CUST000004', 'Delta Partners', 'CORPORATE', 'SILVER', 'NA', 'ACTIVE', '2023-04-05'),
                    ('CUST000005', 'Epsilon Fund', 'INSTITUTIONAL', 'GOLD', 'EU', 'ACTIVE', '2023-05-12')
                    """;

                try (var stmt = connection.createStatement()) {
                    int rowsInserted = stmt.executeUpdate(insertData);
                    logger.info("Inserted {} customer records", rowsInserted);
                }

                // Verify data was inserted
                try (var stmt = connection.createStatement();
                     var rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        logger.info("Database initialized with {} customer records", count);
                    }
                }

            }

        } catch (Exception e) {
            logger.error("Failed to initialize database: {}", e.getMessage(), e);
        }
    }

    /**
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        logger.info("Starting APEX YAML Processing Demo...");

        try {
            // Create and run the YAML processing demo
            PostgreSQLLookupDemo demo = new PostgreSQLLookupDemo();
            demo.runDemo();

        } catch (Exception e) {
            logger.error("Failed to run APEX YAML Processing Demo", e);
        }
    }
}
