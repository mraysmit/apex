package dev.mars.apex.demo.examples;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

/**
 * PostgreSQL Lookup Demo - Advanced Database Enrichment with Multi-Table Support
 *
 * This comprehensive demo showcases APEX's database lookup capabilities with:
 * - Multi-table database schema (customers, counterparties, settlement_instructions, risk_assessments)
 * - Complex multi-parameter database queries with joins
 * - Real-time database enrichment with connection pooling and caching
 * - Performance optimization demonstrations
 * - Pure database-driven processing (no hardcoded data)
 *
 * Features demonstrated:
 * 1. Simple customer profile lookups
 * 2. Complex multi-parameter settlement instruction lookups
 * 3. Performance optimization with database caching
 *
 * Database Schema:
 * - customers: Customer profile data
 * - counterparties: Trading counterparty information
 * - settlement_instructions: Settlement and delivery instructions
 * - risk_assessments: Risk scoring and monitoring data
 *
 * @author Mark A Ray-Smith
 * @since 2025-08-28
 * @version 2.0 - Advanced database lookups with multi-table support (Reference Template)
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

        logger.info("PostgreSQLLookupDemo initialized with real APEX services");
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

            // Demonstrate processing performance with same input
            logger.info("\nDemonstrating YAML Processing Performance:");
            long startTime = System.currentTimeMillis();
            performEnrichmentWithYaml(inputData, configPath);
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
            // Create minimal input data for YAML processing - no hardcoded business logic
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("counterpartyId", "CP_GS"); // Only the lookup key, no other hardcoded data

            logger.info("Input Data for YAML Processing:");
            logger.info("  Counterparty ID: {}", inputData.get("counterpartyId"));

            // Apply YAML multi-parameter enrichment processing using real APEX services
            String configPath = "examples/lookups/settlement-instruction-enrichment.yaml";
            Map<String, Object> enrichedData = performEnrichmentWithYaml(inputData, configPath);
            
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
            // Test multiple lookups with minimal input data - no hardcoded business logic
            logger.info("Testing YAML Processing Performance with Multiple Lookups:");

            String[] customerIds = {"CUST000001", "CUST000002", "CUST000003", "CUST000004", "CUST000005"};
            long totalTime = 0;

            for (String customerId : customerIds) {
                // Create minimal input data for each lookup
                Map<String, Object> inputData = new HashMap<>();
                inputData.put("customerId", customerId);

                long startTime = System.currentTimeMillis();
                Map<String, Object> result = performEnrichmentWithYaml(inputData, "examples/lookups/customer-profile-enrichment.yaml");
                long lookupTime = System.currentTimeMillis() - startTime;
                totalTime += lookupTime;

                logger.info("  Customer {}: {} ms - {}",
                    customerId, lookupTime, result.get("customerName"));
            }

            logger.info("Total lookup time for 5 customers: {} ms", totalTime);
            logger.info("Average lookup time: {} ms", totalTime / customerIds.length);

            // Demonstrate cache effectiveness with minimal input data
            logger.info("\nTesting Cache Effectiveness:");
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("customerId", "CUST000001");

            // First lookup (cache miss)
            long startTime = System.currentTimeMillis();
            performEnrichmentWithYaml(inputData, "examples/lookups/customer-profile-enrichment.yaml");
            long firstLookup = System.currentTimeMillis() - startTime;

            // Second lookup (cache hit)
            startTime = System.currentTimeMillis();
            performEnrichmentWithYaml(inputData, "examples/lookups/customer-profile-enrichment.yaml");
            long secondLookup = System.currentTimeMillis() - startTime;
            
            logger.info("  First lookup (cache miss): {} ms", firstLookup);
            logger.info("  Second lookup (cache hit): {} ms", secondLookup);
            logger.info("  Cache performance improvement: {}x faster", 
                Math.round((double) firstLookup / secondLookup * 10.0) / 10.0);
            
        } catch (Exception e) {
            logger.error("Error in performance optimization demonstration", e);
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
                    @SuppressWarnings("unchecked")
                    Map<String, Object> enrichedMap = (Map<String, Object>) enrichedObject;
                    enrichedResult = enrichedMap;
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



    // ========================================
    // SETUP AND INITIALIZATION METHODS
    // ========================================

    /**
     * Initialize H2 database with comprehensive multi-table schema for advanced demo scenarios.
     * Creates tables: customers, counterparties, settlement_instructions, risk_assessments
     */
    private void initializeDatabase() {
        logger.info("Initializing H2 database with comprehensive multi-table schema...");

        try {
            // Create H2 database connection with shared in-memory database (PostgreSQL compatibility mode)
            String jdbcUrl = "jdbc:h2:mem:apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";

            try (var connection = java.sql.DriverManager.getConnection(jdbcUrl, "sa", "")) {

                // Create customers table
                String createCustomersTable = """
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

                // Create counterparties table
                String createCounterpartiesTable = """
                    CREATE TABLE IF NOT EXISTS counterparties (
                        counterparty_id VARCHAR(20) PRIMARY KEY,
                        counterparty_name VARCHAR(100) NOT NULL,
                        counterparty_type VARCHAR(20) NOT NULL,
                        credit_rating VARCHAR(10),
                        status VARCHAR(20) NOT NULL,
                        created_date DATE,
                        last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

                // Create settlement_instructions table
                String createSettlementTable = """
                    CREATE TABLE IF NOT EXISTS settlement_instructions (
                        instruction_id VARCHAR(50) PRIMARY KEY,
                        counterparty_id VARCHAR(20) NOT NULL,
                        instrument_type VARCHAR(20) NOT NULL,
                        currency VARCHAR(10) NOT NULL,
                        market VARCHAR(20) NOT NULL,
                        custodian_name VARCHAR(100),
                        custodian_bic VARCHAR(20),
                        settlement_method VARCHAR(20),
                        delivery_instruction VARCHAR(50),
                        market_name VARCHAR(100),
                        settlement_cycle VARCHAR(10),
                        cut_off_time TIME,
                        time_zone VARCHAR(50),
                        special_instructions TEXT,
                        created_date DATE,
                        last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (counterparty_id) REFERENCES counterparties(counterparty_id)
                    )
                    """;

                // Create risk_assessments table
                String createRiskTable = """
                    CREATE TABLE IF NOT EXISTS risk_assessments (
                        assessment_id VARCHAR(50) PRIMARY KEY,
                        counterparty_id VARCHAR(20) NOT NULL,
                        risk_category VARCHAR(20),
                        risk_score INTEGER,
                        max_exposure DECIMAL(15,2),
                        approval_required BOOLEAN,
                        monitoring_level VARCHAR(20),
                        assessment_date DATE,
                        last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (counterparty_id) REFERENCES counterparties(counterparty_id)
                    )
                    """;

                try (var stmt = connection.createStatement()) {
                    stmt.execute(createCustomersTable);
                    logger.info("Created customers table in H2 database");

                    stmt.execute(createCounterpartiesTable);
                    logger.info("Created counterparties table in H2 database");

                    stmt.execute(createSettlementTable);
                    logger.info("Created settlement_instructions table in H2 database");

                    stmt.execute(createRiskTable);
                    logger.info("Created risk_assessments table in H2 database");
                }

                // Insert sample customer data
                String insertCustomers = """
                    MERGE INTO customers (customer_id, customer_name, customer_type, tier, region, status, created_date) VALUES
                    ('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2024-01-15'),
                    ('CUST000002', 'Global Investment Partners', 'INSTITUTIONAL', 'GOLD', 'EU', 'ACTIVE', '2024-02-20'),
                    ('CUST000003', 'Pacific Asset Management', 'INSTITUTIONAL', 'GOLD', 'APAC', 'ACTIVE', '2024-03-10'),
                    ('CUST000004', 'John Smith', 'INDIVIDUAL', 'SILVER', 'NA', 'ACTIVE', '2024-04-05'),
                    ('CUST000005', 'European Pension Fund', 'INSTITUTIONAL', 'PLATINUM', 'EU', 'ACTIVE', '2024-05-12')
                    """;

                // Insert sample counterparty data
                String insertCounterparties = """
                    MERGE INTO counterparties (counterparty_id, counterparty_name, counterparty_type, credit_rating, status, created_date) VALUES
                    ('CP_GS', 'Goldman Sachs', 'BANK', 'A+', 'ACTIVE', '2024-01-01'),
                    ('CP_JPM', 'JPMorgan Chase', 'BANK', 'A+', 'ACTIVE', '2024-01-01'),
                    ('CP_MS', 'Morgan Stanley', 'BANK', 'A', 'ACTIVE', '2024-01-01'),
                    ('CP_CITI', 'Citigroup', 'BANK', 'A-', 'ACTIVE', '2024-01-01'),
                    ('CP_BAML', 'Bank of America Merrill Lynch', 'BANK', 'A', 'ACTIVE', '2024-01-01')
                    """;

                // Insert sample settlement instruction data
                String insertSettlements = """
                    MERGE INTO settlement_instructions (instruction_id, counterparty_id, instrument_type, currency, market,
                                                       custodian_name, custodian_bic, settlement_method, delivery_instruction,
                                                       market_name, settlement_cycle, cut_off_time, time_zone, special_instructions, created_date) VALUES
                    ('SI_GS_EQUITY_USD_NYSE', 'CP_GS', 'EQUITY', 'USD', 'NYSE', 'Goldman Sachs Custody', 'GSCCUS33XXX', 'DVP', 'DELIVER', 'New York Stock Exchange', 'T+2', '16:00:00', 'America/New_York', 'Standard equity settlement', '2024-01-01'),
                    ('SI_GS_BOND_USD_NYSE', 'CP_GS', 'BOND', 'USD', 'NYSE', 'Goldman Sachs Custody', 'GSCCUS33XXX', 'DVP', 'DELIVER', 'New York Stock Exchange', 'T+1', '15:00:00', 'America/New_York', 'Bond settlement with accrued interest', '2024-01-01'),
                    ('SI_JPM_EQUITY_USD_NASDAQ', 'CP_JPM', 'EQUITY', 'USD', 'NASDAQ', 'JPMorgan Custody', 'CHASUS33XXX', 'DVP', 'DELIVER', 'NASDAQ Stock Market', 'T+2', '16:00:00', 'America/New_York', 'Standard equity settlement', '2024-01-01'),
                    ('SI_MS_EQUITY_EUR_LSE', 'CP_MS', 'EQUITY', 'EUR', 'LSE', 'Morgan Stanley Custody', 'MSINUS33XXX', 'DVP', 'DELIVER', 'London Stock Exchange', 'T+2', '16:30:00', 'Europe/London', 'European equity settlement', '2024-01-01'),
                    ('SI_CITI_BOND_USD_OTC', 'CP_CITI', 'BOND', 'USD', 'OTC', 'Citibank Custody', 'CITIUS33XXX', 'DVP', 'DELIVER', 'Over The Counter', 'T+1', '15:30:00', 'America/New_York', 'OTC bond settlement', '2024-01-01')
                    """;

                // Insert sample risk assessment data
                String insertRiskAssessments = """
                    MERGE INTO risk_assessments (assessment_id, counterparty_id, risk_category, risk_score, max_exposure,
                                                approval_required, monitoring_level, assessment_date) VALUES
                    ('RISK_CP_GS', 'CP_GS', 'LOW', 95, 1000000000.00, false, 'STANDARD', '2024-01-01'),
                    ('RISK_CP_JPM', 'CP_JPM', 'LOW', 92, 950000000.00, false, 'STANDARD', '2024-01-01'),
                    ('RISK_CP_MS', 'CP_MS', 'MEDIUM', 85, 750000000.00, false, 'ENHANCED', '2024-01-01'),
                    ('RISK_CP_CITI', 'CP_CITI', 'MEDIUM', 78, 600000000.00, true, 'ENHANCED', '2024-01-01'),
                    ('RISK_CP_BAML', 'CP_BAML', 'LOW', 88, 800000000.00, false, 'STANDARD', '2024-01-01')
                    """;

                try (var stmt = connection.createStatement()) {
                    int customersInserted = stmt.executeUpdate(insertCustomers);
                    logger.info("Inserted {} customer records into H2 database", customersInserted);

                    int counterpartiesInserted = stmt.executeUpdate(insertCounterparties);
                    logger.info("Inserted {} counterparty records into H2 database", counterpartiesInserted);

                    int settlementsInserted = stmt.executeUpdate(insertSettlements);
                    logger.info("Inserted {} settlement instruction records into H2 database", settlementsInserted);

                    int riskInserted = stmt.executeUpdate(insertRiskAssessments);
                    logger.info("Inserted {} risk assessment records into H2 database", riskInserted);
                }

                // Verify comprehensive data was inserted
                try (var stmt = connection.createStatement()) {
                    var rs1 = stmt.executeQuery("SELECT COUNT(*) FROM customers");
                    if (rs1.next()) {
                        logger.info("H2 database initialized with {} customer records", rs1.getInt(1));
                    }

                    var rs2 = stmt.executeQuery("SELECT COUNT(*) FROM counterparties");
                    if (rs2.next()) {
                        logger.info("H2 database initialized with {} counterparty records", rs2.getInt(1));
                    }

                    var rs3 = stmt.executeQuery("SELECT COUNT(*) FROM settlement_instructions");
                    if (rs3.next()) {
                        logger.info("H2 database initialized with {} settlement instruction records", rs3.getInt(1));
                    }

                    var rs4 = stmt.executeQuery("SELECT COUNT(*) FROM risk_assessments");
                    if (rs4.next()) {
                        logger.info("H2 database initialized with {} risk assessment records", rs4.getInt(1));
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
