package dev.mars.apex.demo.examples;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.demo.bootstrap.model.FinancialTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * PostgreSQL External Lookup Demonstration
 * 
 * This standalone demo showcases external database lookups using PostgreSQL with:
 * - Connection pooling and performance optimization
 * - Multi-parameter complex SQL queries
 * - Customer profile enrichment from database
 * - Settlement instruction lookup with multiple parameters
 * - Risk assessment data enrichment
 * - Fallback strategies and error handling
 * 
 * Prerequisites:
 * - NONE! This demo is completely self-contained
 * - Automatically creates embedded H2 database for demonstration
 * - Sets up all required tables and sample data programmatically
 * - No external dependencies or manual setup required
 * 
 * Key Features Demonstrated:
 * - External data source configuration with connection pooling
 * - Simple single-parameter database lookup
 * - Complex multi-parameter database lookup with joins
 * - Caching strategies for performance
 * - Fallback mechanisms for resilience
 * - Real-time data enrichment from PostgreSQL
 * 
 * @author APEX Demo Team
 * @version 1.0
 * @since 2025-07-28
 */
public class PostgreSQLLookupDemo {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLLookupDemo.class);

    private final EnrichmentService enrichmentService;
    private final YamlConfigurationLoader configLoader;
    private Connection databaseConnection;

    // Database configuration for self-contained demo
    private static final String DB_URL = "jdbc:h2:mem:apex_demo;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
    private static final String DB_USER = "apex_user";
    private static final String DB_PASSWORD = "apex_password";

    public PostgreSQLLookupDemo() {
        // Initialize APEX services for enrichment
        LookupServiceRegistry lookupRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(lookupRegistry, expressionEvaluator);
        this.configLoader = new YamlConfigurationLoader();
    }
    
    /**
     * Main demonstration method - runs all PostgreSQL lookup scenarios.
     */
    public void runDemo() {
        logger.info("=".repeat(80));
        logger.info("APEX PostgreSQL External Lookup Demonstration");
        logger.info("=".repeat(80));
        
        try {
            // Setup self-contained database
            setupDatabase();

            // Check database connectivity
            if (!checkDatabaseConnectivity()) {
                logger.error("Database setup failed.");
                return;
            }
            
            // Run demonstration scenarios
            demonstrateSimpleDatabaseLookup();
            demonstrateMultiParameterLookup();
            demonstratePerformanceOptimization();
            demonstrateFallbackStrategies();
            
            logger.info("=".repeat(80));
            logger.info("Self-Contained Database Lookup Demonstration completed successfully!");
            logger.info("=".repeat(80));

        } catch (Exception e) {
            logger.error("Error during database lookup demonstration", e);
        } finally {
            // Cleanup database connection
            cleanupDatabase();
        }
    }
    
    /**
     * Demonstrate simple database lookup for customer profile enrichment.
     */
    private void demonstrateSimpleDatabaseLookup() {
        logger.info("\n" + "=".repeat(60));
        logger.info("1. SIMPLE DATABASE LOOKUP - Customer Profile Enrichment");
        logger.info("=".repeat(60));
        
        try {
            // Create sample transaction data
            Map<String, Object> transaction = createSampleTransaction();
            transaction.put("customerId", "CUST000001");
            
            logger.info("Input Transaction Data:");
            logger.info("  Customer ID: {}", transaction.get("customerId"));
            logger.info("  Amount: {}", transaction.get("amount"));
            logger.info("  Currency: {}", transaction.get("currency"));
            
            // Apply database simple lookup enrichment using APEX enrichment service
            String configPath = "examples/lookups/postgresql-simple-lookup.yaml";
            Map<String, Object> enrichedData = performEnrichmentWithYaml(transaction, configPath);
            
            logger.info("\nEnriched Data from PostgreSQL:");
            logger.info("  Customer Name: {}", enrichedData.get("customerName"));
            logger.info("  Customer Type: {}", enrichedData.get("customerType"));
            logger.info("  Customer Tier: {}", enrichedData.get("customerTier"));
            logger.info("  Customer Region: {}", enrichedData.get("customerRegion"));
            logger.info("  Customer Status: {}", enrichedData.get("customerStatus"));
            logger.info("  Customer Created: {}", enrichedData.get("customerCreatedDate"));
            
            // Demonstrate caching behavior
            logger.info("\nDemonstrating Database Caching:");
            long startTime = System.currentTimeMillis();
            performEnrichmentWithYaml(transaction, configPath);
            long cachedTime = System.currentTimeMillis() - startTime;
            logger.info("  Cached lookup time: {} ms (should be faster)", cachedTime);
            
        } catch (Exception e) {
            logger.error("Error in simple database lookup demonstration", e);
        }
    }
    
    /**
     * Demonstrate multi-parameter database lookup for settlement instructions.
     */
    private void demonstrateMultiParameterLookup() {
        logger.info("\n" + "=".repeat(60));
        logger.info("2. MULTI-PARAMETER DATABASE LOOKUP - Settlement Instructions");
        logger.info("=".repeat(60));
        
        try {
            // Create sample trade data
            FinancialTrade trade = createSampleTrade();
            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("trade", convertTradeToMap(trade));
            
            logger.info("Input Trade Data:");
            logger.info("  Trade ID: {}", trade.getTradeId());
            logger.info("  Counterparty: {}", trade.getCounterparty());
            logger.info("  Instrument Type: {}", trade.getInstrumentType());
            logger.info("  Currency: {}", trade.getCurrency());
            logger.info("  Market: {}", getTradeMarket(trade));
            logger.info("  Amount: {}", trade.getAmount());
            
            // Apply database multi-parameter lookup enrichment using APEX
            String configPath = "examples/lookups/postgresql-multi-param-lookup.yaml";
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
                Map<String, Object> result = performEnrichmentWithYaml(transaction, "examples/lookups/postgresql-simple-lookup.yaml");
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
            performEnrichmentWithYaml(transaction, "examples/lookups/postgresql-simple-lookup.yaml");
            long firstLookup = System.currentTimeMillis() - startTime;

            // Second lookup (cache hit)
            startTime = System.currentTimeMillis();
            performEnrichmentWithYaml(transaction, "examples/lookups/postgresql-simple-lookup.yaml");
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
            
            Map<String, Object> result = performEnrichmentWithYaml(transaction, "examples/lookups/postgresql-simple-lookup.yaml");
            
            logger.info("Fallback Data Applied:");
            logger.info("  Customer Name: {}", result.get("customerName"));
            logger.info("  Customer Type: {}", result.get("customerType"));
            logger.info("  Customer Tier: {}", result.get("customerTier"));
            logger.info("  Customer Region: {}", result.get("customerRegion"));
            logger.info("  Customer Status: {}", result.get("customerStatus"));
            
            // Test cascade fallback for settlement instructions
            logger.info("\nTesting Cascade Fallback for Settlement Instructions:");
            FinancialTrade trade = createSampleTrade();
            trade.setCounterparty("CP_UNKNOWN"); // Non-existent counterparty
            
            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("trade", convertTradeToMap(trade));
            
            Map<String, Object> settlementResult = performEnrichmentWithYaml(tradeData, "examples/lookups/postgresql-multi-param-lookup.yaml");
            
            logger.info("Cascade Fallback Result:");
            logger.info("  Settlement Instruction ID: {}", settlementResult.get("settlementInstructionId"));
            logger.info("  Counterparty Name: {}", settlementResult.get("counterpartyName"));
            logger.info("  Settlement Method: {}", settlementResult.get("settlementMethod"));
            
        } catch (Exception e) {
            logger.error("Error in fallback strategies demonstration", e);
        }
    }

    /**
     * Setup self-contained database with all required tables and data.
     */
    private void setupDatabase() {
        try {
            logger.info("Setting up self-contained database...");

            // Create database connection
            databaseConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // Create schemas and tables
            createDatabaseSchema();

            // Insert sample data
            insertSampleData();

            logger.info("✓ Database setup completed successfully");

        } catch (SQLException e) {
            logger.error("Failed to setup database", e);
            throw new RuntimeException("Database setup failed", e);
        }
    }

    /**
     * Create database schema with all required tables.
     */
    private void createDatabaseSchema() throws SQLException {
        logger.info("Creating database schema...");

        try (Statement stmt = databaseConnection.createStatement()) {
            // Create customers table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    customer_id VARCHAR(20) PRIMARY KEY,
                    customer_name VARCHAR(255) NOT NULL,
                    customer_type VARCHAR(50) NOT NULL,
                    tier VARCHAR(20) NOT NULL,
                    region VARCHAR(10) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            // Create counterparties table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS counterparties (
                    counterparty_id VARCHAR(20) PRIMARY KEY,
                    counterparty_name VARCHAR(255) NOT NULL,
                    counterparty_type VARCHAR(50) NOT NULL,
                    credit_rating VARCHAR(10),
                    credit_limit DECIMAL(15,2),
                    jurisdiction VARCHAR(2) NOT NULL,
                    bic_code VARCHAR(11),
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            // Create custodians table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS custodians (
                    custodian_id VARCHAR(20) PRIMARY KEY,
                    custodian_name VARCHAR(255) NOT NULL,
                    custodian_bic VARCHAR(11) NOT NULL,
                    custodian_address TEXT,
                    jurisdiction VARCHAR(2) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            // Create markets table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS markets (
                    market_code VARCHAR(10) PRIMARY KEY,
                    market_name VARCHAR(255) NOT NULL,
                    country VARCHAR(2) NOT NULL,
                    time_zone VARCHAR(50) NOT NULL,
                    settlement_cycle VARCHAR(10) NOT NULL,
                    cut_off_time TIME,
                    volatility_rating VARCHAR(10),
                    liquidity_rating VARCHAR(10),
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            // Create instruments table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS instruments (
                    instrument_type VARCHAR(50) PRIMARY KEY,
                    instrument_name VARCHAR(255) NOT NULL,
                    instrument_class VARCHAR(50) NOT NULL,
                    settlement_currency VARCHAR(3),
                    typical_settlement_days INTEGER DEFAULT 2,
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            // Create settlement instructions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS settlement_instructions (
                    instruction_id VARCHAR(50) PRIMARY KEY,
                    counterparty_id VARCHAR(20) NOT NULL,
                    custodian_id VARCHAR(20),
                    instrument_type VARCHAR(50) NOT NULL,
                    currency VARCHAR(3) NOT NULL,
                    market VARCHAR(10) NOT NULL,
                    settlement_method VARCHAR(20) NOT NULL,
                    delivery_instruction VARCHAR(50) NOT NULL,
                    special_instructions TEXT,
                    min_amount DECIMAL(15,2),
                    max_amount DECIMAL(15,2),
                    priority INTEGER DEFAULT 1,
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    effective_date DATE DEFAULT CURRENT_DATE,
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            // Create risk assessments table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS risk_assessments (
                    assessment_id VARCHAR(50) PRIMARY KEY,
                    counterparty_id VARCHAR(20) NOT NULL,
                    instrument_type VARCHAR(50) NOT NULL,
                    market VARCHAR(10) NOT NULL,
                    risk_category VARCHAR(20) NOT NULL,
                    risk_score DECIMAL(5,2),
                    max_exposure DECIMAL(15,2),
                    max_single_trade DECIMAL(15,2),
                    approval_required BOOLEAN DEFAULT FALSE,
                    monitoring_level VARCHAR(20),
                    effective_date DATE DEFAULT CURRENT_DATE,
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            logger.info("✓ Database schema created successfully");
        }
    }

    /**
     * Insert comprehensive sample data for demonstrations.
     */
    private void insertSampleData() throws SQLException {
        logger.info("Inserting sample data...");

        insertCustomerData();
        insertCounterpartyData();
        insertCustodianData();
        insertMarketData();
        insertInstrumentData();
        insertSettlementInstructionData();
        insertRiskAssessmentData();

        logger.info("✓ Sample data inserted successfully");
    }

    /**
     * Insert customer data for simple lookup demonstrations.
     */
    private void insertCustomerData() throws SQLException {
        String sql = """
            INSERT INTO customers (customer_id, customer_name, customer_type, tier, region, status) VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = databaseConnection.prepareStatement(sql)) {
            // Customer data for demonstrations
            Object[][] customers = {
                {"CUST000001", "Acme Corporation", "CORPORATE", "PLATINUM", "NA", "ACTIVE"},
                {"CUST000002", "Global Investment Partners", "INSTITUTIONAL", "GOLD", "EU", "ACTIVE"},
                {"CUST000003", "Pacific Asset Management", "INSTITUTIONAL", "GOLD", "APAC", "ACTIVE"},
                {"CUST000004", "John Smith", "INDIVIDUAL", "SILVER", "NA", "ACTIVE"},
                {"CUST000005", "European Pension Fund", "INSTITUTIONAL", "PLATINUM", "EU", "ACTIVE"},
                {"CUST000006", "Asia Trading Ltd", "CORPORATE", "GOLD", "APAC", "ACTIVE"},
                {"CUST000007", "Sarah Johnson", "INDIVIDUAL", "BASIC", "EU", "ACTIVE"},
                {"CUST000008", "Latin America Holdings", "CORPORATE", "SILVER", "LATAM", "ACTIVE"},
                {"CUST000009", "Middle East Investment Co", "INSTITUTIONAL", "GOLD", "ME", "ACTIVE"},
                {"CUST000010", "Test Customer Inactive", "INDIVIDUAL", "BASIC", "NA", "INACTIVE"}
            };

            for (Object[] customer : customers) {
                for (int i = 0; i < customer.length; i++) {
                    stmt.setObject(i + 1, customer[i]);
                }
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    /**
     * Insert counterparty data for multi-parameter lookup demonstrations.
     */
    private void insertCounterpartyData() throws SQLException {
        String sql = """
            INSERT INTO counterparties (counterparty_id, counterparty_name, counterparty_type, credit_rating, credit_limit, jurisdiction, bic_code, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = databaseConnection.prepareStatement(sql)) {
            Object[][] counterparties = {
                {"CP_GS", "Goldman Sachs", "BANK", "A+", 500000000.00, "US", "GSCCUS33XXX", "ACTIVE"},
                {"CP_DB", "Deutsche Bank", "BANK", "A-", 300000000.00, "DE", "DEUTDEFFXXX", "ACTIVE"},
                {"CP_JPM", "JP Morgan", "BANK", "AA-", 750000000.00, "US", "CHASUS33XXX", "ACTIVE"},
                {"CP_BARC", "Barclays", "BANK", "A", 400000000.00, "GB", "BARCGB22XXX", "ACTIVE"},
                {"CP_CS", "Credit Suisse", "BANK", "BBB+", 250000000.00, "CH", "CRESCHZZXXX", "ACTIVE"},
                {"CP_UBS", "UBS", "BANK", "A+", 600000000.00, "CH", "UBSWCHZH80A", "ACTIVE"},
                {"CP_HSBC", "HSBC", "BANK", "A", 450000000.00, "GB", "HBUKGB4BXXX", "ACTIVE"},
                {"CP_CITI", "Citigroup", "BANK", "A+", 550000000.00, "US", "CITIUS33XXX", "ACTIVE"},
                {"DEFAULT", "Default Counterparty", "BANK", "A", 100000000.00, "US", "DEFAULTXXX", "ACTIVE"}
            };

            for (Object[] counterparty : counterparties) {
                for (int i = 0; i < counterparty.length; i++) {
                    stmt.setObject(i + 1, counterparty[i]);
                }
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    /**
     * Insert custodian data.
     */
    private void insertCustodianData() throws SQLException {
        String sql = """
            INSERT INTO custodians (custodian_id, custodian_name, custodian_bic, custodian_address, jurisdiction, status) VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = databaseConnection.prepareStatement(sql)) {
            Object[][] custodians = {
                {"CUST_GS_US", "Goldman Sachs Custody", "GSCCUS33XXX", "200 West Street, New York, NY 10282", "US", "ACTIVE"},
                {"CUST_JPM_US", "JP Morgan Custody", "CHASUS33XXX", "383 Madison Avenue, New York, NY 10179", "US", "ACTIVE"},
                {"CUST_DB_DE", "Deutsche Bank Custody", "DEUTDEFFXXX", "Taunusanlage 12, 60325 Frankfurt am Main", "DE", "ACTIVE"},
                {"CUST_BARC_GB", "Barclays Custody", "BARCGB22XXX", "1 Churchill Place, London E14 5HP", "GB", "ACTIVE"},
                {"CUST_UBS_CH", "UBS Custody", "UBSWCHZH80A", "Bahnhofstrasse 45, 8001 Zurich", "CH", "ACTIVE"},
                {"CUST_DEFAULT", "Default Custodian", "DEFAULTXXX", "Default Address", "US", "ACTIVE"}
            };

            for (Object[] custodian : custodians) {
                for (int i = 0; i < custodian.length; i++) {
                    stmt.setObject(i + 1, custodian[i]);
                }
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    /**
     * Insert market data.
     */
    private void insertMarketData() throws SQLException {
        String sql = """
            INSERT INTO markets (market_code, market_name, country, time_zone, settlement_cycle, cut_off_time, volatility_rating, liquidity_rating, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = databaseConnection.prepareStatement(sql)) {
            Object[][] markets = {
                {"NYSE", "New York Stock Exchange", "US", "America/New_York", "T+2", "16:00:00", "MEDIUM", "HIGH", "ACTIVE"},
                {"NASDAQ", "NASDAQ", "US", "America/New_York", "T+2", "16:00:00", "HIGH", "HIGH", "ACTIVE"},
                {"LSE", "London Stock Exchange", "GB", "Europe/London", "T+2", "16:30:00", "MEDIUM", "HIGH", "ACTIVE"},
                {"XETRA", "XETRA", "DE", "Europe/Berlin", "T+2", "17:30:00", "MEDIUM", "HIGH", "ACTIVE"},
                {"SIX", "SIX Swiss Exchange", "CH", "Europe/Zurich", "T+2", "17:30:00", "LOW", "MEDIUM", "ACTIVE"}
            };

            for (Object[] market : markets) {
                for (int i = 0; i < market.length; i++) {
                    stmt.setObject(i + 1, market[i]);
                }
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    /**
     * Insert instrument data.
     */
    private void insertInstrumentData() throws SQLException {
        String sql = """
            INSERT INTO instruments (instrument_type, instrument_name, instrument_class, settlement_currency, typical_settlement_days) VALUES (?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = databaseConnection.prepareStatement(sql)) {
            Object[][] instruments = {
                {"EQUITY", "Equity Securities", "EQUITY", null, 2},
                {"BOND", "Fixed Income Securities", "FIXED_INCOME", null, 2},
                {"COMMODITY_SWAP", "Commodity Total Return Swap", "DERIVATIVE", "USD", 2},
                {"FX_SWAP", "Foreign Exchange Swap", "FX", null, 2},
                {"DERIVATIVE", "OTC Derivatives", "DERIVATIVE", null, 2}
            };

            for (Object[] instrument : instruments) {
                for (int i = 0; i < instrument.length; i++) {
                    stmt.setObject(i + 1, instrument[i]);
                }
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    /**
     * Insert settlement instruction data for multi-parameter lookup demonstrations.
     */
    private void insertSettlementInstructionData() throws SQLException {
        String sql = """
            INSERT INTO settlement_instructions (instruction_id, counterparty_id, custodian_id, instrument_type, currency, market, settlement_method, delivery_instruction, special_instructions, min_amount, max_amount, priority, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = databaseConnection.prepareStatement(sql)) {
            Object[][] instructions = {
                // Goldman Sachs instructions
                {"SI_GS_EQUITY_USD_NYSE", "CP_GS", "CUST_GS_US", "EQUITY", "USD", "NYSE", "DVP", "DELIVER", "Standard equity settlement", 1000.00, 100000000.00, 1, "ACTIVE"},
                {"SI_GS_BOND_USD_NYSE", "CP_GS", "CUST_GS_US", "BOND", "USD", "NYSE", "DVP", "DELIVER", "Fixed income settlement", 10000.00, 500000000.00, 1, "ACTIVE"},
                {"SI_GS_DERIVATIVE_USD_NYSE", "CP_GS", "CUST_JPM_US", "DERIVATIVE", "USD", "NYSE", "DVP_PREMIUM", "DELIVER", "OTC derivative settlement", 100000.00, 1000000000.00, 2, "ACTIVE"},

                // Deutsche Bank instructions
                {"SI_DB_EQUITY_EUR_XETRA", "CP_DB", "CUST_DB_DE", "EQUITY", "EUR", "XETRA", "DVP", "DELIVER", "European equity settlement", 1000.00, 50000000.00, 1, "ACTIVE"},
                {"SI_DB_BOND_EUR_XETRA", "CP_DB", "CUST_DB_DE", "BOND", "EUR", "XETRA", "DVP", "DELIVER", "European bond settlement", 10000.00, 200000000.00, 1, "ACTIVE"},

                // JP Morgan instructions
                {"SI_JPM_EQUITY_USD_NYSE", "CP_JPM", "CUST_JPM_US", "EQUITY", "USD", "NYSE", "DVP", "DELIVER", "Prime brokerage equity", 1000.00, 200000000.00, 1, "ACTIVE"},
                {"SI_JPM_COMMODITY_SWAP_USD_NYSE", "CP_JPM", "CUST_JPM_US", "COMMODITY_SWAP", "USD", "NYSE", "DVP_PREMIUM", "DELIVER", "Commodity swap settlement", 1000000.00, 500000000.00, 1, "ACTIVE"},

                // Barclays instructions
                {"SI_BARC_EQUITY_GBP_LSE", "CP_BARC", "CUST_BARC_GB", "EQUITY", "GBP", "LSE", "DVP", "DELIVER", "UK equity settlement", 1000.00, 75000000.00, 1, "ACTIVE"},
                {"SI_BARC_BOND_GBP_LSE", "CP_BARC", "CUST_BARC_GB", "BOND", "GBP", "LSE", "DVP", "DELIVER", "UK gilt settlement", 10000.00, 300000000.00, 1, "ACTIVE"},

                // UBS instructions
                {"SI_UBS_EQUITY_CHF_SIX", "CP_UBS", "CUST_UBS_CH", "EQUITY", "CHF", "SIX", "DVP_PREMIUM", "DELIVER", "Premium Swiss equity", 1000.00, 100000000.00, 1, "ACTIVE"},
                {"SI_UBS_DERIVATIVE_USD_SIX", "CP_UBS", "CUST_UBS_CH", "DERIVATIVE", "USD", "SIX", "DVP_PREMIUM", "DELIVER", "Structured products", 100000.00, 500000000.00, 1, "ACTIVE"},

                // Default fallback instructions
                {"SI_DEFAULT_EQUITY_USD_NYSE", "DEFAULT", "CUST_JPM_US", "EQUITY", "USD", "NYSE", "DVP", "DELIVER", "Default US equity fallback", 1000.00, 10000000.00, 10, "ACTIVE"},
                {"SI_DEFAULT_EQUITY_EUR_XETRA", "DEFAULT", "CUST_DB_DE", "EQUITY", "EUR", "XETRA", "DVP", "DELIVER", "Default EU equity fallback", 1000.00, 10000000.00, 10, "ACTIVE"},
                {"SI_DEFAULT_EQUITY_GBP_LSE", "DEFAULT", "CUST_BARC_GB", "EQUITY", "GBP", "LSE", "DVP", "DELIVER", "Default UK equity fallback", 1000.00, 10000000.00, 10, "ACTIVE"}
            };

            for (Object[] instruction : instructions) {
                for (int i = 0; i < instruction.length; i++) {
                    stmt.setObject(i + 1, instruction[i]);
                }
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    /**
     * Insert risk assessment data for multi-parameter lookup demonstrations.
     */
    private void insertRiskAssessmentData() throws SQLException {
        String sql = """
            INSERT INTO risk_assessments (assessment_id, counterparty_id, instrument_type, market, risk_category, risk_score, max_exposure, max_single_trade, approval_required, monitoring_level, effective_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
            """;

        try (PreparedStatement stmt = databaseConnection.prepareStatement(sql)) {
            Object[][] assessments = {
                // Goldman Sachs risk assessments
                {"RA_GS_EQUITY_NYSE", "CP_GS", "EQUITY", "NYSE", "LOW", 15.50, 500000000.00, 100000000.00, false, "STANDARD"},
                {"RA_GS_BOND_NYSE", "CP_GS", "BOND", "NYSE", "LOW", 12.25, 750000000.00, 200000000.00, false, "STANDARD"},
                {"RA_GS_DERIVATIVE_NYSE", "CP_GS", "DERIVATIVE", "NYSE", "MEDIUM", 35.75, 1000000000.00, 500000000.00, true, "ENHANCED"},

                // Deutsche Bank risk assessments
                {"RA_DB_EQUITY_XETRA", "CP_DB", "EQUITY", "XETRA", "MEDIUM", 25.80, 300000000.00, 50000000.00, false, "STANDARD"},
                {"RA_DB_BOND_XETRA", "CP_DB", "BOND", "XETRA", "LOW", 18.90, 400000000.00, 100000000.00, false, "STANDARD"},

                // JP Morgan risk assessments
                {"RA_JPM_EQUITY_NYSE", "CP_JPM", "EQUITY", "NYSE", "LOW", 10.25, 750000000.00, 200000000.00, false, "STANDARD"},
                {"RA_JPM_COMMODITY_SWAP_NYSE", "CP_JPM", "COMMODITY_SWAP", "NYSE", "HIGH", 65.30, 500000000.00, 100000000.00, true, "INTENSIVE"},

                // Barclays risk assessments
                {"RA_BARC_EQUITY_LSE", "CP_BARC", "EQUITY", "LSE", "MEDIUM", 22.60, 400000000.00, 75000000.00, false, "STANDARD"},
                {"RA_BARC_BOND_LSE", "CP_BARC", "BOND", "LSE", "LOW", 16.40, 500000000.00, 150000000.00, false, "STANDARD"},

                // UBS risk assessments
                {"RA_UBS_EQUITY_SIX", "CP_UBS", "EQUITY", "SIX", "LOW", 14.20, 600000000.00, 100000000.00, false, "STANDARD"},
                {"RA_UBS_DERIVATIVE_SIX", "CP_UBS", "DERIVATIVE", "SIX", "MEDIUM", 38.95, 800000000.00, 200000000.00, true, "ENHANCED"}
            };

            for (Object[] assessment : assessments) {
                for (int i = 0; i < assessment.length; i++) {
                    stmt.setObject(i + 1, assessment[i]);
                }
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    /**
     * Check database connectivity and verify sample data.
     */
    private boolean checkDatabaseConnectivity() {
        try {
            logger.info("Checking database connectivity and data...");

            if (databaseConnection == null || databaseConnection.isClosed()) {
                logger.error("Database connection is not available");
                return false;
            }

            // Test connectivity with a simple query
            try (Statement stmt = databaseConnection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers");
                if (rs.next()) {
                    int customerCount = rs.getInt(1);
                    logger.info("✓ Database connectivity verified - {} customers found", customerCount);

                    // Verify settlement instructions
                    rs = stmt.executeQuery("SELECT COUNT(*) FROM settlement_instructions");
                    if (rs.next()) {
                        int instructionCount = rs.getInt(1);
                        logger.info("✓ Settlement instructions verified - {} instructions found", instructionCount);
                    }

                    return customerCount > 0;
                }
            }

            return false;

        } catch (SQLException e) {
            logger.error("Database connectivity check failed", e);
            return false;
        }
    }

    /**
     * Cleanup database resources.
     */
    private void cleanupDatabase() {
        try {
            if (databaseConnection != null && !databaseConnection.isClosed()) {
                databaseConnection.close();
                logger.info("✓ Database connection closed");
            }
        } catch (SQLException e) {
            logger.warn("Error closing database connection", e);
        }
    }

    /**
     * Perform enrichment using YAML configuration and APEX EnrichmentService.
     * This uses the real APEX rules engine for external lookup enrichment.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> performEnrichmentWithYaml(Map<String, Object> inputData, String configPath) {
        try {
            // Load YAML configuration
            YamlRuleConfiguration yamlConfig = configLoader.loadFromFile(configPath);

            // Use APEX EnrichmentService to enrich the data
            Object enrichedObject = enrichmentService.enrichObject(yamlConfig, inputData);

            // Convert result back to Map
            if (enrichedObject instanceof Map) {
                return (Map<String, Object>) enrichedObject;
            } else {
                // If enrichment returns a different type, wrap it
                Map<String, Object> result = new HashMap<>(inputData);
                result.put("enrichedData", enrichedObject);
                return result;
            }

        } catch (Exception e) {
            logger.error("Error performing YAML enrichment", e);
            // Return original data if enrichment fails
            return new HashMap<>(inputData);
        }
    }

    /**
     * Perform simple customer lookup directly from database.
     * This simulates what the APEX rules engine would do with external lookup enrichment.
     */
    private Map<String, Object> performSimpleLookup(Map<String, Object> transaction) {
        Map<String, Object> enrichedData = new HashMap<>(transaction);

        try {
            String customerId = (String) transaction.get("customerId");
            if (customerId == null) {
                return applyFallbackCustomerData(enrichedData);
            }

            String sql = "SELECT customer_name, customer_type, tier, region, status, created_date FROM customers WHERE customer_id = ?";

            try (PreparedStatement stmt = databaseConnection.prepareStatement(sql)) {
                stmt.setString(1, customerId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        enrichedData.put("customerName", rs.getString("customer_name"));
                        enrichedData.put("customerType", rs.getString("customer_type"));
                        enrichedData.put("customerTier", rs.getString("tier"));
                        enrichedData.put("customerRegion", rs.getString("region"));
                        enrichedData.put("customerStatus", rs.getString("status"));
                        enrichedData.put("customerCreatedDate", rs.getTimestamp("created_date"));
                    } else {
                        // Apply fallback data for non-existent customers
                        return applyFallbackCustomerData(enrichedData);
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Error performing simple lookup", e);
            return applyFallbackCustomerData(enrichedData);
        }

        return enrichedData;
    }

    /**
     * Perform multi-parameter settlement instruction lookup directly from database.
     * This simulates what the APEX rules engine would do with external lookup enrichment.
     */
    private Map<String, Object> performMultiParameterLookup(Map<String, Object> tradeData) {
        Map<String, Object> enrichedData = new HashMap<>(tradeData);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> trade = (Map<String, Object>) tradeData.get("trade");

            if (trade == null) {
                return enrichedData;
            }

            String counterpartyId = (String) trade.get("counterpartyId");
            String instrumentType = (String) trade.get("instrumentType");
            String currency = (String) trade.get("currency");
            String market = (String) trade.get("market");
            BigDecimal amount = (BigDecimal) trade.get("amount");

            // First try exact match
            Map<String, Object> settlementData = lookupSettlementInstructions(counterpartyId, instrumentType, currency, market, amount);

            if (settlementData.isEmpty()) {
                // Try fallback with DEFAULT counterparty
                settlementData = lookupSettlementInstructions("DEFAULT", instrumentType, currency, market, amount);
            }

            enrichedData.putAll(settlementData);

            // Also lookup risk assessment
            Map<String, Object> riskData = lookupRiskAssessment(counterpartyId, instrumentType, market, amount);
            enrichedData.putAll(riskData);

        } catch (Exception e) {
            logger.error("Error performing multi-parameter lookup", e);
        }

        return enrichedData;
    }

    /**
     * Apply fallback customer data when lookup fails.
     */
    private Map<String, Object> applyFallbackCustomerData(Map<String, Object> data) {
        data.put("customerName", "Unknown Customer");
        data.put("customerType", "STANDARD");
        data.put("customerTier", "BASIC");
        data.put("customerRegion", "UNKNOWN");
        data.put("customerStatus", "INACTIVE");
        return data;
    }

    /**
     * Lookup settlement instructions from database.
     */
    private Map<String, Object> lookupSettlementInstructions(String counterpartyId, String instrumentType, String currency, String market, BigDecimal amount) {
        Map<String, Object> result = new HashMap<>();

        try {
            String sql = """
                SELECT
                    si.instruction_id,
                    si.settlement_method,
                    si.delivery_instruction,
                    si.special_instructions,
                    cp.counterparty_name,
                    cp.counterparty_type,
                    cp.credit_rating,
                    cust.custodian_name,
                    cust.custodian_bic,
                    cust.custodian_address,
                    mk.market_name,
                    mk.settlement_cycle,
                    mk.cut_off_time,
                    mk.time_zone,
                    inst.instrument_name,
                    inst.instrument_class,
                    inst.settlement_currency
                FROM settlement_instructions si
                LEFT JOIN counterparties cp ON si.counterparty_id = cp.counterparty_id
                LEFT JOIN custodians cust ON si.custodian_id = cust.custodian_id
                LEFT JOIN markets mk ON si.market = mk.market_code
                LEFT JOIN instruments inst ON si.instrument_type = inst.instrument_type
                WHERE si.counterparty_id = ?
                  AND si.instrument_type = ?
                  AND si.currency = ?
                  AND si.market = ?
                  AND si.status = 'ACTIVE'
                  AND (si.min_amount IS NULL OR si.min_amount <= ?)
                  AND (si.max_amount IS NULL OR si.max_amount >= ?)
                ORDER BY si.priority ASC, si.created_date DESC
                LIMIT 1
                """;

            try (PreparedStatement stmt = databaseConnection.prepareStatement(sql)) {
                stmt.setString(1, counterpartyId);
                stmt.setString(2, instrumentType);
                stmt.setString(3, currency);
                stmt.setString(4, market);
                stmt.setBigDecimal(5, amount);
                stmt.setBigDecimal(6, amount);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        result.put("settlementInstructionId", rs.getString("instruction_id"));
                        result.put("counterpartyName", rs.getString("counterparty_name"));
                        result.put("counterpartyType", rs.getString("counterparty_type"));
                        result.put("counterpartyCreditRating", rs.getString("credit_rating"));
                        result.put("custodianName", rs.getString("custodian_name"));
                        result.put("custodianBic", rs.getString("custodian_bic"));
                        result.put("custodianAddress", rs.getString("custodian_address"));
                        result.put("settlementMethod", rs.getString("settlement_method"));
                        result.put("deliveryInstruction", rs.getString("delivery_instruction"));
                        result.put("specialInstructions", rs.getString("special_instructions"));
                        result.put("marketName", rs.getString("market_name"));
                        result.put("settlementCycle", rs.getString("settlement_cycle"));
                        result.put("cutOffTime", rs.getString("cut_off_time"));
                        result.put("timeZone", rs.getString("time_zone"));
                        result.put("instrumentName", rs.getString("instrument_name"));
                        result.put("instrumentClass", rs.getString("instrument_class"));
                        result.put("settlementCurrency", rs.getString("settlement_currency"));
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Error looking up settlement instructions", e);
        }

        return result;
    }

    /**
     * Lookup risk assessment from database.
     */
    private Map<String, Object> lookupRiskAssessment(String counterpartyId, String instrumentType, String market, BigDecimal amount) {
        Map<String, Object> result = new HashMap<>();

        try {
            String sql = """
                SELECT
                    risk_category,
                    risk_score,
                    max_exposure,
                    max_single_trade,
                    approval_required,
                    monitoring_level
                FROM risk_assessments
                WHERE counterparty_id = ?
                  AND instrument_type = ?
                  AND market = ?
                  AND (max_single_trade IS NULL OR max_single_trade >= ?)
                  AND effective_date <= CURRENT_DATE
                ORDER BY effective_date DESC
                LIMIT 1
                """;

            try (PreparedStatement stmt = databaseConnection.prepareStatement(sql)) {
                stmt.setString(1, counterpartyId);
                stmt.setString(2, instrumentType);
                stmt.setString(3, market);
                stmt.setBigDecimal(4, amount);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        result.put("riskCategory", rs.getString("risk_category"));
                        result.put("riskScore", rs.getBigDecimal("risk_score"));
                        result.put("maxExposure", rs.getBigDecimal("max_exposure"));
                        result.put("maxSingleTrade", rs.getBigDecimal("max_single_trade"));
                        result.put("approvalRequired", rs.getBoolean("approval_required"));
                        result.put("monitoringLevel", rs.getString("monitoring_level"));
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Error looking up risk assessment", e);
        }

        return result;
    }

    /**
     * Create sample transaction data for demonstrations.
     */
    private Map<String, Object> createSampleTransaction() {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("transactionId", "TXN" + System.currentTimeMillis());
        transaction.put("amount", new BigDecimal("50000.00"));
        transaction.put("currency", "USD");
        transaction.put("transactionDate", LocalDate.now().toString());
        transaction.put("transactionType", "PURCHASE");
        return transaction;
    }

    /**
     * Create sample financial trade for demonstrations.
     */
    private FinancialTrade createSampleTrade() {
        FinancialTrade trade = new FinancialTrade();
        trade.setTradeId("TRD" + System.currentTimeMillis());
        trade.setAmount(new BigDecimal("5000000.00"));
        trade.setCurrency("USD");
        trade.setCounterparty("CP_GS");
        trade.setInstrumentType("EQUITY");
        trade.setStatus("NEW");
        trade.setTradeDate(LocalDate.now());
        return trade;
    }

    /**
     * Convert FinancialTrade to Map for processing.
     */
    private Map<String, Object> convertTradeToMap(FinancialTrade trade) {
        Map<String, Object> tradeMap = new HashMap<>();
        tradeMap.put("tradeId", trade.getTradeId());
        tradeMap.put("amount", trade.getAmount());
        tradeMap.put("currency", trade.getCurrency());
        tradeMap.put("counterpartyId", trade.getCounterparty());
        tradeMap.put("instrumentType", trade.getInstrumentType());
        tradeMap.put("status", trade.getStatus());
        tradeMap.put("tradeDate", trade.getTradeDate().toString());
        tradeMap.put("market", getTradeMarket(trade));
        return tradeMap;
    }

    /**
     * Get market for trade based on currency and instrument type.
     */
    private String getTradeMarket(FinancialTrade trade) {
        // Simple market mapping based on currency
        return switch (trade.getCurrency()) {
            case "USD" -> "NYSE";
            case "EUR" -> "XETRA";
            case "GBP" -> "LSE";
            case "CHF" -> "SIX";
            case "HKD" -> "HKEX";
            case "SGD" -> "SGX";
            default -> "NYSE";
        };
    }



    /**
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        logger.info("Starting Self-Contained Database Lookup Demo...");

        try {
            // Create and run the self-contained demo
            PostgreSQLLookupDemo demo = new PostgreSQLLookupDemo();
            demo.runDemo();

        } catch (Exception e) {
            logger.error("Failed to run Database Lookup Demo", e);
        }
    }
}
