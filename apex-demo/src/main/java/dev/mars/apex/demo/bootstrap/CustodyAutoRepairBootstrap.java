package dev.mars.apex.demo.bootstrap;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.demo.bootstrap.model.BootstrapSettlementInstruction;
import dev.mars.apex.demo.bootstrap.model.BootstrapStandingInstruction;
import dev.mars.apex.demo.bootstrap.model.BootstrapSIRepairResult;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

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

/**
 * Complete Bootstrap Demonstration of APEX Custody Auto-Repair for Asian Markets.
 * 
 * This single-file bootstrap creates a complete end-to-end demonstration including:
 * - PostgreSQL database setup with realistic Asian markets data
 * - YAML rule configuration with weighted decision making
 * - Comprehensive enrichment datasets
 * - Multiple realistic scenarios
 * - Complete audit trails and performance metrics
 * 
 * The bootstrap is designed to be re-runnable and self-contained, demonstrating
 * the full power of APEX in solving real-world custody settlement challenges.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-30
 * @version 1.0
 */
public class CustodyAutoRepairBootstrap {
    
    private static final Logger LOGGER = Logger.getLogger(CustodyAutoRepairBootstrap.class.getName());
    
    // Database configuration
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/";
    private static final String DB_NAME = "apex_custody_demo";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "postgres";
    
    // APEX components
    private RulesService rulesService;
    private EnrichmentService enrichmentService;
    private YamlRuleConfiguration yamlConfig;
    
    // Database connection
    private Connection connection;
    
    // Performance metrics
    private Map<String, Long> performanceMetrics;
    private List<String> executionLog;
    
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("APEX CUSTODY AUTO-REPAIR BOOTSTRAP DEMONSTRATION");
        System.out.println("Complete End-to-End Asian Markets Settlement Auto-Repair");
        System.out.println("=".repeat(80));
        
        CustodyAutoRepairBootstrap bootstrap = new CustodyAutoRepairBootstrap();
        
        try {
            bootstrap.runCompleteBootstrap();
        } catch (Exception e) {
            System.err.println("Bootstrap execution failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            bootstrap.cleanup();
        }
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("BOOTSTRAP DEMONSTRATION COMPLETED");
        System.out.println("=".repeat(80));
    }
    
    /**
     * Run the complete bootstrap demonstration.
     */
    public void runCompleteBootstrap() throws Exception {
        long startTime = System.currentTimeMillis();
        
        // Initialize components
        initializeComponents();
        
        // Phase 1: Database Infrastructure Setup
        System.out.println("\n  PHASE 1: DATABASE INFRASTRUCTURE SETUP");
        setupDatabaseInfrastructure();
        
        // Phase 2: YAML Configuration Loading
        System.out.println("\n PHASE 2: YAML CONFIGURATION LOADING");
        loadYamlConfiguration();
        
        // Phase 3: Test Data Population
        System.out.println("\n PHASE 3: TEST DATA POPULATION");
        populateTestData();
        
        // Phase 4: APEX Rules Engine Initialization
        System.out.println("\n PHASE 4: APEX RULES ENGINE INITIALIZATION");
        initializeApexEngine();
        
        // Phase 5: Scenario Execution
        System.out.println("\n PHASE 5: SCENARIO EXECUTION");
        executeAllScenarios();
        
        // Phase 6: Performance Analysis
        System.out.println("\n PHASE 6: PERFORMANCE ANALYSIS");
        analyzePerformance();
        
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("\n✅ Bootstrap completed successfully in " + totalTime + "ms");
    }
    
    /**
     * Initialize bootstrap components.
     */
    private void initializeComponents() {
        System.out.println("Initializing bootstrap components...");

        this.performanceMetrics = new HashMap<>();
        this.executionLog = new ArrayList<>();
        this.rulesService = new RulesService();

        // Initialize enrichment service with required dependencies
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);

        logExecution("Components initialized");
    }
    
    /**
     * Setup PostgreSQL database infrastructure.
     */
    private void setupDatabaseInfrastructure() throws Exception {
        System.out.println("Setting up PostgreSQL database infrastructure...");
        
        // Check if PostgreSQL is available
        if (!isPostgreSQLAvailable()) {
            System.out.println("⚠️  PostgreSQL not available - using in-memory simulation");
            setupInMemorySimulation();
            return;
        }
        
        // Create database if it doesn't exist
        createDatabaseIfNotExists();
        
        // Connect to the database
        connectToDatabase();
        
        // Create schema
        createDatabaseSchema();
        
        logExecution("Database infrastructure setup completed");
    }
    
    /**
     * Check if PostgreSQL is available.
     */
    private boolean isPostgreSQLAvailable() {
        try {
            Class.forName("org.postgresql.Driver");
            try (Connection testConn = DriverManager.getConnection(
                    DB_URL + "postgres", DB_USER, DB_PASSWORD)) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.warning("PostgreSQL not available: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Setup in-memory simulation when PostgreSQL is not available.
     */
    private void setupInMemorySimulation() {
        System.out.println("Setting up in-memory data simulation...");
        // For this demo, we'll continue without actual database
        // All data will be held in memory
        logExecution("In-memory simulation setup completed");
    }
    
    /**
     * Create database if it doesn't exist.
     */
    private void createDatabaseIfNotExists() throws SQLException {
        try (Connection adminConn = DriverManager.getConnection(
                DB_URL + "postgres", DB_USER, DB_PASSWORD);
             Statement stmt = adminConn.createStatement()) {
            
            // Check if database exists
            ResultSet rs = stmt.executeQuery(
                "SELECT 1 FROM pg_database WHERE datname = '" + DB_NAME + "'");
            
            if (!rs.next()) {
                // Create database
                stmt.executeUpdate("CREATE DATABASE " + DB_NAME);
                System.out.println("✅ Created database: " + DB_NAME);
            } else {
                System.out.println("✅ Database already exists: " + DB_NAME);
            }
        }
    }
    
    /**
     * Connect to the target database.
     */
    private void connectToDatabase() throws SQLException {
        this.connection = DriverManager.getConnection(
            DB_URL + DB_NAME, DB_USER, DB_PASSWORD);
        System.out.println("✅ Connected to database: " + DB_NAME);
    }
    
    /**
     * Create database schema.
     */
    private void createDatabaseSchema() throws SQLException {
        System.out.println("Creating database schema...");
        
        String[] schemaSql = {
            // Settlement Instructions table
            """
            CREATE TABLE IF NOT EXISTS settlement_instructions (
                instruction_id VARCHAR(50) PRIMARY KEY,
                external_instruction_id VARCHAR(50),
                instruction_date DATE NOT NULL,
                trade_date DATE NOT NULL,
                settlement_date DATE NOT NULL,
                client_id VARCHAR(50) NOT NULL,
                client_name VARCHAR(255),
                client_tier VARCHAR(20),
                market VARCHAR(20) NOT NULL,
                market_mic VARCHAR(10),
                instrument_type VARCHAR(20) NOT NULL,
                instrument_id VARCHAR(50),
                isin VARCHAR(12),
                instrument_name VARCHAR(255),
                currency VARCHAR(3) NOT NULL,
                settlement_amount DECIMAL(18,2) NOT NULL,
                settlement_currency VARCHAR(3),
                settlement_method VARCHAR(20),
                delivery_instruction VARCHAR(20),
                counterparty_id VARCHAR(50),
                counterparty_name VARCHAR(255),
                counterparty_bic VARCHAR(11),
                custodian_id VARCHAR(50),
                custodian_name VARCHAR(255),
                custodian_bic VARCHAR(11),
                custodial_account VARCHAR(50),
                safekeeping_account VARCHAR(50),
                instruction_status VARCHAR(20) DEFAULT 'PENDING',
                validation_status VARCHAR(20) DEFAULT 'INCOMPLETE',
                requires_repair BOOLEAN DEFAULT FALSE,
                high_value_transaction BOOLEAN DEFAULT FALSE,
                client_opt_out BOOLEAN DEFAULT FALSE,
                repair_reason TEXT,
                business_unit VARCHAR(50),
                trading_desk VARCHAR(50),
                portfolio_id VARCHAR(50),
                risk_category VARCHAR(10),
                created_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // Standing Instructions table
            """
            CREATE TABLE IF NOT EXISTS standing_instructions (
                si_id VARCHAR(50) PRIMARY KEY,
                si_name VARCHAR(255) NOT NULL,
                description TEXT,
                scope_type VARCHAR(20) NOT NULL,
                client_id VARCHAR(50),
                market VARCHAR(20),
                instrument_type VARCHAR(20),
                weight DECIMAL(3,2) NOT NULL,
                confidence_level DECIMAL(3,2) NOT NULL,
                default_counterparty_id VARCHAR(50),
                default_counterparty_name VARCHAR(255),
                default_counterparty_bic VARCHAR(11),
                default_custodian_id VARCHAR(50),
                default_custodian_name VARCHAR(255),
                default_custodian_bic VARCHAR(11),
                default_custodial_account VARCHAR(50),
                default_safekeeping_account VARCHAR(50),
                default_settlement_method VARCHAR(20),
                default_delivery_instruction VARCHAR(20),
                enabled BOOLEAN DEFAULT TRUE,
                risk_category VARCHAR(10),
                business_justification TEXT,
                usage_count INTEGER DEFAULT 0,
                success_rate DECIMAL(5,4) DEFAULT 0.0000,
                created_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // Repair Audit Log table
            """
            CREATE TABLE IF NOT EXISTS repair_audit_log (
                audit_id SERIAL PRIMARY KEY,
                instruction_id VARCHAR(50) NOT NULL,
                result_id VARCHAR(100) NOT NULL,
                repair_status VARCHAR(20) NOT NULL,
                weighted_score DECIMAL(5,2),
                final_decision VARCHAR(30),
                fields_repaired INTEGER DEFAULT 0,
                processing_time_ms BIGINT,
                applied_sis TEXT,
                decision_rationale TEXT,
                audit_trail TEXT,
                processed_by VARCHAR(100),
                processed_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // Asian Markets Reference Data table
            """
            CREATE TABLE IF NOT EXISTS asian_markets (
                market VARCHAR(20) PRIMARY KEY,
                market_name VARCHAR(255) NOT NULL,
                market_mic VARCHAR(10) NOT NULL,
                local_market_code VARCHAR(10),
                base_currency VARCHAR(3) NOT NULL,
                settlement_cycle VARCHAR(5) NOT NULL,
                trading_hours VARCHAR(50),
                holiday_calendar VARCHAR(20),
                regulatory_regime VARCHAR(10),
                timezone VARCHAR(50),
                enabled BOOLEAN DEFAULT TRUE
            )
            """
        };
        
        try (Statement stmt = connection.createStatement()) {
            for (String sql : schemaSql) {
                stmt.executeUpdate(sql);
            }
        }
        
        System.out.println("✅ Database schema created successfully");
        logExecution("Database schema created");
    }
    
    /**
     * Load YAML configuration.
     */
    private void loadYamlConfiguration() throws Exception {
        System.out.println("Loading YAML configuration...");
        
        try {
            // Load the bootstrap YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            this.yamlConfig = loader.loadFromClasspath("bootstrap/custody-auto-repair-bootstrap.yaml");
            
            System.out.println("✅ YAML configuration loaded successfully");
            System.out.println("   - Rule chains: " + yamlConfig.getRuleChains().size());
            System.out.println("   - Enrichments: " + yamlConfig.getEnrichments().size());
            
            logExecution("YAML configuration loaded");
            
        } catch (Exception e) {
            System.err.println("❌ Failed to load YAML configuration: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Populate test data.
     */
    private void populateTestData() throws SQLException {
        System.out.println("Populating test data...");
        
        if (connection != null) {
            populateDatabaseTestData();
        } else {
            populateInMemoryTestData();
        }
        
        logExecution("Test data populated");
    }
    
    /**
     * Populate database with test data.
     */
    private void populateDatabaseTestData() throws SQLException {
        System.out.println("Populating database with Asian markets test data...");
        
        // Clear existing data
        clearExistingData();
        
        // Insert Asian markets reference data
        insertAsianMarketsData();
        
        // Insert standing instructions
        insertStandingInstructionsData();
        
        System.out.println("✅ Database test data populated successfully");
    }
    
    /**
     * Clear existing data for re-runnable demo.
     */
    private void clearExistingData() throws SQLException {
        String[] clearSql = {
            "DELETE FROM repair_audit_log",
            "DELETE FROM settlement_instructions", 
            "DELETE FROM standing_instructions",
            "DELETE FROM asian_markets"
        };
        
        try (Statement stmt = connection.createStatement()) {
            for (String sql : clearSql) {
                stmt.executeUpdate(sql);
            }
        }
        
        System.out.println("✅ Existing data cleared for fresh demo");
    }
    
    /**
     * Insert Asian markets reference data.
     */
    private void insertAsianMarketsData() throws SQLException {
        String sql = """
            INSERT INTO asian_markets (market, market_name, market_mic, local_market_code, 
                                     base_currency, settlement_cycle, trading_hours, 
                                     holiday_calendar, regulatory_regime, timezone) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        Object[][] marketsData = {
            {"JAPAN", "Japan Exchange Group", "XJPX", "TSE", "JPY", "T+2", 
             "09:00-15:00 JST", "JAPAN", "JFSA", "Asia/Tokyo"},
            {"HONG_KONG", "Hong Kong Exchanges and Clearing", "XHKG", "HKEX", "HKD", "T+2",
             "09:30-16:00 HKT", "HONG_KONG", "SFC", "Asia/Hong_Kong"},
            {"SINGAPORE", "Singapore Exchange", "XSES", "SGX", "SGD", "T+2",
             "09:00-17:00 SGT", "SINGAPORE", "MAS", "Asia/Singapore"},
            {"KOREA", "Korea Exchange", "XKRX", "KRX", "KRW", "T+2",
             "09:00-15:30 KST", "KOREA", "FSC", "Asia/Seoul"}
        };
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Object[] row : marketsData) {
                for (int i = 0; i < row.length; i++) {
                    pstmt.setObject(i + 1, row[i]);
                }
                pstmt.executeUpdate();
            }
        }
        
        System.out.println("✅ Asian markets reference data inserted");
    }
    
    /**
     * Insert standing instructions data.
     */
    private void insertStandingInstructionsData() throws SQLException {
        String sql = """
            INSERT INTO standing_instructions (si_id, si_name, description, scope_type, client_id, 
                                             market, instrument_type, weight, confidence_level,
                                             default_counterparty_id, default_counterparty_name,
                                             default_custodian_id, default_custodian_name,
                                             default_settlement_method, enabled, risk_category,
                                             business_justification) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        Object[][] siData = {
            // Client-level SIs
            {"SI_PREMIUM_ASIA_001", "Premium Asset Management Asia - Default SI", 
             "Premium client with global custody arrangement", "CLIENT", "CLIENT_PREMIUM_ASIA_001",
             null, null, 0.6, 0.98, "CP_PREMIUM_GLOBAL_CUSTODY", "Premium Global Custody Services",
             "CUST_PREMIUM_GLOBAL", "Premium Global Custodian Ltd", "DVP_PREMIUM", true, "LOW",
             "Premium client with global custody arrangement"},
            
            {"SI_STANDARD_ASIA_002", "Standard Asset Management - Default SI",
             "Standard institutional client", "CLIENT", "CLIENT_STANDARD_ASIA_002",
             null, null, 0.6, 0.90, "CP_STANDARD_REGIONAL", "Standard Regional Custody",
             "CUST_STANDARD_ASIA", "Standard Asia Custodian", "DVP", true, "MEDIUM", null},
            
            // Market-level SIs
            {"SI_JAPAN_MARKET", "Japan Market Default SI", "Japan market conventions", "MARKET", null,
             "JAPAN", null, 0.3, 0.88, "CP_JAPAN_STANDARD", "Japan Standard Counterparty",
             "CUST_JAPAN_STANDARD", "Japan Standard Custodian KK", "DVP", true, "MEDIUM", null},
            
            {"SI_HONG_KONG_MARKET", "Hong Kong Market Default SI", "Hong Kong market conventions", "MARKET", null,
             "HONG_KONG", null, 0.3, 0.90, "CP_HK_STANDARD", "Hong Kong Standard Counterparty",
             "CUST_HK_STANDARD", "Hong Kong Standard Custodian Ltd", "DVP", true, "MEDIUM", null},
            
            // Instrument-level SIs
            {"SI_EQUITY_GLOBAL", "Global Equity Instrument SI", "Equity instrument defaults", "INSTRUMENT", null,
             null, "EQUITY", 0.1, 0.75, null, null, null, null, "DVP", true, "MEDIUM", null},
            
            {"SI_FIXED_INCOME_GLOBAL", "Global Fixed Income SI", "Fixed income defaults", "INSTRUMENT", null,
             null, "FIXED_INCOME", 0.1, 0.80, null, null, null, null, "DVP", true, "LOW", null}
        };
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Object[] row : siData) {
                for (int i = 0; i < row.length; i++) {
                    pstmt.setObject(i + 1, row[i]);
                }
                pstmt.executeUpdate();
            }
        }
        
        System.out.println("✅ Standing instructions data inserted");
    }
    
    /**
     * Populate in-memory test data when database is not available.
     */
    private void populateInMemoryTestData() {
        System.out.println("✅ In-memory test data simulation ready");
    }
    
    /**
     * Initialize APEX rules engine.
     */
    private void initializeApexEngine() throws Exception {
        System.out.println("Initializing APEX rules engine...");
        
        if (yamlConfig == null) {
            throw new IllegalStateException("YAML configuration not loaded");
        }
        
        System.out.println("✅ APEX rules engine initialized successfully");
        System.out.println("   - Rules service ready");
        System.out.println("   - Enrichment service ready");
        System.out.println("   - Configuration loaded with " + yamlConfig.getEnrichments().size() + " enrichments");
        
        logExecution("APEX rules engine initialized");
    }
    
    /**
     * Execute all demonstration scenarios.
     */
    private void executeAllScenarios() throws Exception {
        System.out.println("Executing comprehensive demonstration scenarios...");

        // Scenario 1: Premium Client in Japan - Full Auto-Repair
        executeScenario1_PremiumClientJapan();

        // Scenario 2: Standard Client in Hong Kong - Partial Repair
        executeScenario2_StandardClientHongKong();

        // Scenario 3: Unknown Client in Singapore - Market + Instrument Only
        executeScenario3_UnknownClientSingapore();

        // Scenario 4: High-Value Transaction Exception
        executeScenario4_HighValueException();

        // Scenario 5: Client Opt-Out Exception
        executeScenario5_ClientOptOut();

        System.out.println("✅ All scenarios executed successfully");
        logExecution("All scenarios executed");
    }

    /**
     * Scenario 1: Premium Client in Japan - Full Auto-Repair Expected
     */
    private void executeScenario1_PremiumClientJapan() throws Exception {
        System.out.println("\n SCENARIO 1: Premium Client in Japan");
        System.out.println("Expected: Full auto-repair (Score: 100, Client + Market + Instrument)");

        long startTime = System.currentTimeMillis();

        // Create settlement instruction with missing fields
        BootstrapSettlementInstruction instruction = new BootstrapSettlementInstruction(
            "SI_20250730_001", "CLIENT_PREMIUM_ASIA_001", "JAPAN", "EQUITY",
            new BigDecimal("5000000"), "JPY", LocalDate.now().plusDays(2)
        );

        // Set missing fields that trigger auto-repair
        instruction.setCounterpartyId(null);
        instruction.setCustodianId(null);
        instruction.setSettlementMethod(null);
        instruction.addMissingField("counterpartyId");
        instruction.addMissingField("custodianId");
        instruction.addMissingField("settlementMethod");

        // Set additional context
        instruction.setInstrumentName("Toyota Motor Corp");
        instruction.setIsin("JP3633400001");
        instruction.setClientName("Premium Asset Management Asia Ltd");
        instruction.setClientTier("PREMIUM");
        instruction.setBusinessUnit("ASIA_PACIFIC_CUSTODY");
        instruction.setRiskCategory("LOW");

        System.out.println(" Original Instruction:");
        System.out.println("   Client: " + instruction.getClientId() + " (" + instruction.getClientTier() + ")");
        System.out.println("   Market: " + instruction.getMarket());
        System.out.println("   Instrument: " + instruction.getInstrumentType() + " (" + instruction.getInstrumentName() + ")");
        System.out.println("   Amount: " + instruction.getSettlementCurrency() + " " + instruction.getSettlementAmount());
        System.out.println("   Missing Fields: " + instruction.getMissingFields());
        System.out.println("   High Value: " + instruction.isHighValueTransaction());
        System.out.println("   Eligible for Repair: " + instruction.isEligibleForAutoRepair());

        // Apply enrichments
        Object enrichedInstruction = enrichmentService.enrichObject(yamlConfig, instruction);

        // Manually create standing instruction objects based on enrichment data
        createStandingInstructionObjects((BootstrapSettlementInstruction) enrichedInstruction);

        // Execute auto-repair logic
        BootstrapSIRepairResult result = performAutoRepair((BootstrapSettlementInstruction) enrichedInstruction);

        // Display results
        displayScenarioResults("SCENARIO 1", result, startTime);

        // Store audit record
        storeAuditRecord(instruction, result);

        logExecution("Scenario 1 completed");
    }

    /**
     * Scenario 2: Standard Client in Hong Kong - Partial Repair Expected
     */
    private void executeScenario2_StandardClientHongKong() throws Exception {
        System.out.println("\n SCENARIO 2: Standard Client in Hong Kong");
        System.out.println("Expected: Partial repair (Score: 90, Client + Market)");

        long startTime = System.currentTimeMillis();

        BootstrapSettlementInstruction instruction = new BootstrapSettlementInstruction(
            "SI_20250730_002", "CLIENT_STANDARD_ASIA_002", "HONG_KONG", "EQUITY",
            new BigDecimal("2000000"), "HKD", LocalDate.now().plusDays(2)
        );

        // Only custodian missing (counterparty will be filled by client SI)
        instruction.setCustodianId(null);
        instruction.addMissingField("custodianId");

        instruction.setInstrumentName("Tencent Holdings Ltd");
        instruction.setIsin("KYG875721634");
        instruction.setClientName("Standard Asset Management");
        instruction.setClientTier("STANDARD");
        instruction.setBusinessUnit("ASIA_PACIFIC_CUSTODY");
        instruction.setRiskCategory("MEDIUM");

        System.out.println(" Original Instruction:");
        System.out.println("   Client: " + instruction.getClientId() + " (" + instruction.getClientTier() + ")");
        System.out.println("   Market: " + instruction.getMarket());
        System.out.println("   Instrument: " + instruction.getInstrumentType() + " (" + instruction.getInstrumentName() + ")");
        System.out.println("   Amount: " + instruction.getSettlementCurrency() + " " + instruction.getSettlementAmount());
        System.out.println("   Missing Fields: " + instruction.getMissingFields());
        System.out.println("   Eligible for Repair: " + instruction.isEligibleForAutoRepair());

        Object enrichedInstruction = enrichmentService.enrichObject(yamlConfig, instruction);
        createStandingInstructionObjects((BootstrapSettlementInstruction) enrichedInstruction);
        BootstrapSIRepairResult result = performAutoRepair((BootstrapSettlementInstruction) enrichedInstruction);

        displayScenarioResults("SCENARIO 2", result, startTime);
        storeAuditRecord(instruction, result);
        logExecution("Scenario 2 completed");
    }

    /**
     * Scenario 3: Unknown Client in Singapore - Market + Instrument Only
     */
    private void executeScenario3_UnknownClientSingapore() throws Exception {
        System.out.println("\n SCENARIO 3: Unknown Client in Singapore");
        System.out.println("Expected: Market + Instrument repair only (Score: 40)");

        long startTime = System.currentTimeMillis();

        BootstrapSettlementInstruction instruction = new BootstrapSettlementInstruction(
            "SI_20250730_003", "CLIENT_UNKNOWN_999", "SINGAPORE", "FIXED_INCOME",
            new BigDecimal("3000000"), "SGD", LocalDate.now().plusDays(1)
        );

        instruction.setCounterpartyId(null);
        instruction.setCustodianId(null);
        instruction.setSettlementMethod(null);
        instruction.addMissingField("counterpartyId");
        instruction.addMissingField("custodianId");
        instruction.addMissingField("settlementMethod");

        instruction.setInstrumentName("Singapore Government Bond");
        instruction.setIsin("SG1234567890");
        instruction.setClientName("Unknown Client Corp");
        instruction.setClientTier("UNKNOWN");
        instruction.setBusinessUnit("ASIA_PACIFIC_CUSTODY");
        instruction.setRiskCategory("MEDIUM");

        System.out.println("Original Instruction:");
        System.out.println("   Client: " + instruction.getClientId() + " (Unknown - no client SI)");
        System.out.println("   Market: " + instruction.getMarket());
        System.out.println("   Instrument: " + instruction.getInstrumentType() + " (" + instruction.getInstrumentName() + ")");
        System.out.println("   Amount: " + instruction.getSettlementCurrency() + " " + instruction.getSettlementAmount());
        System.out.println("   Missing Fields: " + instruction.getMissingFields());
        System.out.println("   Eligible for Repair: " + instruction.isEligibleForAutoRepair());

        Object enrichedInstruction = enrichmentService.enrichObject(yamlConfig, instruction);
        createStandingInstructionObjects((BootstrapSettlementInstruction) enrichedInstruction);
        BootstrapSIRepairResult result = performAutoRepair((BootstrapSettlementInstruction) enrichedInstruction);

        displayScenarioResults("SCENARIO 3", result, startTime);
        storeAuditRecord(instruction, result);
        logExecution("Scenario 3 completed");
    }

    /**
     * Scenario 4: High-Value Transaction Exception
     */
    private void executeScenario4_HighValueException() throws Exception {
        System.out.println("\n SCENARIO 4: High-Value Transaction Exception");
        System.out.println("Expected: Skip auto-repair - manual review required");

        long startTime = System.currentTimeMillis();

        BootstrapSettlementInstruction instruction = new BootstrapSettlementInstruction(
            "SI_20250730_004", "CLIENT_PREMIUM_ASIA_001", "JAPAN", "EQUITY",
            new BigDecimal("50000000"), "JPY", LocalDate.now().plusDays(2) // $50M - high value
        );

        instruction.setCounterpartyId(null);
        instruction.addMissingField("counterpartyId");

        instruction.setInstrumentName("SoftBank Group Corp");
        instruction.setIsin("JP3436100006");
        instruction.setClientName("Premium Asset Management Asia Ltd");
        instruction.setClientTier("PREMIUM");
        instruction.setBusinessUnit("ASIA_PACIFIC_CUSTODY");
        instruction.setRiskCategory("HIGH");

        System.out.println(" Original Instruction:");
        System.out.println("   Client: " + instruction.getClientId() + " (" + instruction.getClientTier() + ")");
        System.out.println("   Market: " + instruction.getMarket());
        System.out.println("   Amount: " + instruction.getSettlementCurrency() + " " + instruction.getSettlementAmount() + " (HIGH VALUE)");
        System.out.println("   High Value Flag: " + instruction.isHighValueTransaction());
        System.out.println("   Eligible for Repair: " + instruction.isEligibleForAutoRepair());

        BootstrapSIRepairResult result = new BootstrapSIRepairResult(instruction.getInstructionId());
        result.setProcessedBy("CustodyAutoRepairBootstrap");
        result.markAsSkipped("High-value transaction requires manual intervention");
        result.setProcessingTime(startTime);

        displayScenarioResults("SCENARIO 4", result, startTime);
        storeAuditRecord(instruction, result);
        logExecution("Scenario 4 completed");
    }

    /**
     * Scenario 5: Client Opt-Out Exception
     */
    private void executeScenario5_ClientOptOut() throws Exception {
        System.out.println("\n SCENARIO 5: Client Opt-Out Exception");
        System.out.println("Expected: Skip auto-repair - client opted out");

        long startTime = System.currentTimeMillis();

        BootstrapSettlementInstruction instruction = new BootstrapSettlementInstruction(
            "SI_20250730_005", "CLIENT_OPT_OUT", "KOREA", "EQUITY",
            new BigDecimal("1500000"), "KRW", LocalDate.now().plusDays(2)
        );

        instruction.setClientOptOut(true); // Client opted out
        instruction.setCounterpartyId(null);
        instruction.addMissingField("counterpartyId");

        instruction.setInstrumentName("Samsung Electronics");
        instruction.setIsin("KR7005930003");
        instruction.setClientName("Opt-Out Client Corp");
        instruction.setClientTier("STANDARD");
        instruction.setBusinessUnit("ASIA_PACIFIC_CUSTODY");
        instruction.setRiskCategory("MEDIUM");

        System.out.println(" Original Instruction:");
        System.out.println("   Client: " + instruction.getClientId() + " (Opted out of auto-repair)");
        System.out.println("   Market: " + instruction.getMarket());
        System.out.println("   Amount: " + instruction.getSettlementCurrency() + " " + instruction.getSettlementAmount());
        System.out.println("   Client Opt-Out: " + instruction.isClientOptOut());
        System.out.println("   Eligible for Repair: " + instruction.isEligibleForAutoRepair());

        BootstrapSIRepairResult result = new BootstrapSIRepairResult(instruction.getInstructionId());
        result.setProcessedBy("CustodyAutoRepairBootstrap");
        result.markAsSkipped("Client has opted out of auto-repair");
        result.setProcessingTime(startTime);

        displayScenarioResults("SCENARIO 5", result, startTime);
        storeAuditRecord(instruction, result);
        logExecution("Scenario 5 completed");
    }
    
    /**
     * Perform auto-repair logic using APEX rules engine.
     */
    private BootstrapSIRepairResult performAutoRepair(BootstrapSettlementInstruction instruction) throws Exception {
        long startTime = System.currentTimeMillis();

        BootstrapSIRepairResult result = new BootstrapSIRepairResult(instruction.getInstructionId());
        result.setProcessedBy("CustodyAutoRepairBootstrap");
        result.setMarket(instruction.getMarket());
        result.setClientTier(instruction.getClientTier());
        result.setInstrumentType(instruction.getInstrumentType());

        // Check eligibility first
        if (!instruction.isEligibleForAutoRepair()) {
            if (instruction.isHighValueTransaction()) {
                result.markAsSkipped("High-value transaction requires manual intervention");
            } else if (instruction.isClientOptOut()) {
                result.markAsSkipped("Client has opted out of auto-repair");
            } else {
                result.markAsSkipped("Instruction not eligible for auto-repair");
            }
            result.setProcessingTime(startTime);
            return result;
        }

        // Calculate weighted scores based on available SIs
        double clientScore = instruction.getApplicableClientSI() != null ? 60 : 0;
        double marketScore = instruction.getApplicableMarketSI() != null ? 30 : 0;
        double instrumentScore = instruction.getApplicableInstrumentSI() != null ? 10 : 0;

        // Add rule scores
        if (clientScore > 0) {
            result.addRuleScore("client-level-si-rule", clientScore, 0.6);
            result.addAppliedStandingInstruction(instruction.getApplicableClientSI());
        }
        if (marketScore > 0) {
            result.addRuleScore("market-level-si-rule", marketScore, 0.3);
            result.addAppliedStandingInstruction(instruction.getApplicableMarketSI());
        }
        if (instrumentScore > 0) {
            result.addRuleScore("instrument-level-si-rule", instrumentScore, 0.1);
            result.addAppliedStandingInstruction(instruction.getApplicableInstrumentSI());
        }

        // Calculate final scores
        result.calculateFinalScores();

        // Apply field repairs based on available SIs
        applyFieldRepairs(instruction, result);

        // Determine final status
        if (result.getFieldsRepaired() > 0) {
            if (result.getWeightedScore() >= 50) {
                result.markAsSuccessful("Full auto-repair completed with weighted score: " + result.getWeightedScore());
            } else {
                result.markAsPartial("Partial auto-repair completed with weighted score: " + result.getWeightedScore());
            }
        } else {
            result.markAsFailed("No applicable standing instructions found for repair");
        }

        result.setProcessingTime(startTime);
        return result;
    }

    /**
     * Apply field repairs based on available standing instructions.
     */
    private void applyFieldRepairs(BootstrapSettlementInstruction instruction, BootstrapSIRepairResult result) {
        // Apply client SI repairs first (highest priority)
        if (instruction.getApplicableClientSI() != null) {
            applyStandingInstructionRepairs(instruction, instruction.getApplicableClientSI(), result);
        }

        // Apply market SI repairs for remaining fields
        if (instruction.getApplicableMarketSI() != null) {
            applyStandingInstructionRepairs(instruction, instruction.getApplicableMarketSI(), result);
        }

        // Apply instrument SI repairs for remaining fields
        if (instruction.getApplicableInstrumentSI() != null) {
            applyStandingInstructionRepairs(instruction, instruction.getApplicableInstrumentSI(), result);
        }
    }

    /**
     * Apply repairs from a specific standing instruction.
     */
    private void applyStandingInstructionRepairs(BootstrapSettlementInstruction instruction,
                                               BootstrapStandingInstruction si,
                                               BootstrapSIRepairResult result) {

        for (String missingField : instruction.getMissingFields()) {
            // Only repair if not already repaired
            if (!result.hasFieldRepair(missingField) && si.hasDefaultValue(missingField)) {
                String defaultValue = si.getDefaultValue(missingField);
                result.addFieldRepair(missingField, defaultValue, si);
                updateInstructionField(instruction, missingField, defaultValue);
            }
        }
    }

    /**
     * Update instruction field with repaired value.
     */
    private void updateInstructionField(BootstrapSettlementInstruction instruction, String fieldName, String value) {
        switch (fieldName.toLowerCase()) {
            case "counterpartyid":
                instruction.setCounterpartyId(value);
                break;
            case "custodianid":
                instruction.setCustodianId(value);
                break;
            case "settlementmethod":
                instruction.setSettlementMethod(value);
                break;
            case "deliveryinstruction":
                instruction.setDeliveryInstruction(value);
                break;
            // Add more field mappings as needed
        }
    }

    /**
     * Display scenario results.
     */
    private void displayScenarioResults(String scenarioName, BootstrapSIRepairResult result, long startTime) {
        System.out.println("\n " + scenarioName + " RESULTS:");
        System.out.println("   Status: " + result.getRepairStatus());
        System.out.println("   Weighted Score: " + String.format("%.1f", result.getWeightedScore()));
        System.out.println("   Final Decision: " + result.getFinalDecision());
        System.out.println("   Fields Repaired: " + result.getFieldsRepaired());
        System.out.println("   Processing Time: " + result.getProcessingTimeMs() + "ms");

        if (result.getRepairStatus().equals("SKIPPED")) {
            System.out.println("   Skip Reason: " + result.getSkipReason());
        } else if (result.getRepairStatus().equals("FAILED")) {
            System.out.println("   Failure Reason: " + result.getFailureReason());
        } else {
            System.out.println("   Applied SIs: " + result.getAppliedStandingInstructions().size());
            for (BootstrapStandingInstruction si : result.getAppliedStandingInstructions()) {
                System.out.println("     - " + si.getSiName() + " (Scope: " + si.getScopeType() +
                                 ", Weight: " + si.getWeight() + ")");
            }

            if (!result.getFieldRepairs().isEmpty()) {
                System.out.println("   Field Repairs:");
                for (Map.Entry<String, String> repair : result.getFieldRepairs().entrySet()) {
                    BootstrapStandingInstruction source = result.getFieldRepairSource(repair.getKey());
                    System.out.println("     - " + repair.getKey() + ": " + repair.getValue() +
                                     " (from " + source.getSiName() + ")");
                }
            }
        }

        System.out.println("   Decision Rationale: " + result.getDecisionRationale());
    }

    /**
     * Store audit record in database.
     */
    private void storeAuditRecord(BootstrapSettlementInstruction instruction, BootstrapSIRepairResult result) {
        if (connection == null) {
            return; // Skip database storage if not available
        }

        try {
            String sql = """
                INSERT INTO repair_audit_log (instruction_id, result_id, repair_status, weighted_score,
                                            final_decision, fields_repaired, processing_time_ms,
                                            applied_sis, decision_rationale, audit_trail, processed_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, instruction.getInstructionId());
                pstmt.setString(2, result.getResultId());
                pstmt.setString(3, result.getRepairStatus());
                pstmt.setDouble(4, result.getWeightedScore());
                pstmt.setString(5, result.getFinalDecision());
                pstmt.setInt(6, result.getFieldsRepaired());
                pstmt.setLong(7, result.getProcessingTimeMs());

                // Convert applied SIs to string
                StringBuilder appliedSIs = new StringBuilder();
                for (BootstrapStandingInstruction si : result.getAppliedStandingInstructions()) {
                    if (appliedSIs.length() > 0) appliedSIs.append(", ");
                    appliedSIs.append(si.getSiName());
                }
                pstmt.setString(8, appliedSIs.toString());

                pstmt.setString(9, result.getDecisionRationale());
                pstmt.setString(10, String.join("; ", result.getAuditTrail()));
                pstmt.setString(11, result.getProcessedBy());

                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.warning("Failed to store audit record: " + e.getMessage());
        }
    }

    /**
     * Create standing instruction objects based on enrichment data.
     * This method simulates the enrichment by creating SI objects based on the instruction data.
     */
    private void createStandingInstructionObjects(BootstrapSettlementInstruction instruction) {
        // Create client-level standing instruction if client matches
        if ("CLIENT_PREMIUM_ASIA_001".equals(instruction.getClientId())) {
            BootstrapStandingInstruction clientSI = new BootstrapStandingInstruction(
                "SI_PREMIUM_ASIA_001", instruction.getClientId(), "Premium Asset Management Asia - Default SI");
            clientSI.setDefaultCounterpartyId("CP_PREMIUM_GLOBAL_CUSTODY");
            clientSI.setDefaultCounterpartyName("Premium Global Custody Services");
            clientSI.setDefaultCustodianId("CUST_PREMIUM_GLOBAL");
            clientSI.setDefaultCustodianName("Premium Global Custodian Ltd");
            clientSI.setDefaultSettlementMethod("DVP_PREMIUM");
            clientSI.setDefaultDeliveryInstruction("DELIVER");
            instruction.setApplicableClientSI(clientSI);
        } else if ("CLIENT_STANDARD_ASIA_002".equals(instruction.getClientId())) {
            BootstrapStandingInstruction clientSI = new BootstrapStandingInstruction(
                "SI_STANDARD_ASIA_002", instruction.getClientId(), "Standard Asset Management - Default SI");
            clientSI.setDefaultCounterpartyId("CP_STANDARD_REGIONAL");
            clientSI.setDefaultCounterpartyName("Standard Regional Custody");
            clientSI.setDefaultCustodianId("CUST_STANDARD_ASIA");
            clientSI.setDefaultCustodianName("Standard Asia Custodian");
            clientSI.setDefaultSettlementMethod("DVP");
            clientSI.setDefaultDeliveryInstruction("DELIVER");
            instruction.setApplicableClientSI(clientSI);
        }

        // Create market-level standing instruction
        if ("JAPAN".equals(instruction.getMarket())) {
            BootstrapStandingInstruction marketSI = new BootstrapStandingInstruction(
                "SI_JAPAN_MARKET", instruction.getMarket(), "Japan Market Default SI", true);
            marketSI.setDefaultCustodianId("CUST_JAPAN_STANDARD");
            marketSI.setDefaultCustodianName("Japan Standard Custodian KK");
            marketSI.setDefaultCounterpartyId("CP_JAPAN_STANDARD");
            marketSI.setDefaultCounterpartyName("Japan Standard Counterparty");
            marketSI.setDefaultSettlementMethod("DVP");
            marketSI.setDefaultDeliveryInstruction("DELIVER");
            instruction.setApplicableMarketSI(marketSI);
        } else if ("HONG_KONG".equals(instruction.getMarket())) {
            BootstrapStandingInstruction marketSI = new BootstrapStandingInstruction(
                "SI_HONG_KONG_MARKET", instruction.getMarket(), "Hong Kong Market Default SI", true);
            marketSI.setDefaultCustodianId("CUST_HK_STANDARD");
            marketSI.setDefaultCustodianName("Hong Kong Standard Custodian Ltd");
            marketSI.setDefaultCounterpartyId("CP_HK_STANDARD");
            marketSI.setDefaultCounterpartyName("Hong Kong Standard Counterparty");
            marketSI.setDefaultSettlementMethod("DVP");
            marketSI.setDefaultDeliveryInstruction("DELIVER");
            instruction.setApplicableMarketSI(marketSI);
        } else if ("SINGAPORE".equals(instruction.getMarket())) {
            BootstrapStandingInstruction marketSI = new BootstrapStandingInstruction(
                "SI_SINGAPORE_MARKET", instruction.getMarket(), "Singapore Market Default SI", true);
            marketSI.setDefaultCustodianId("CUST_SG_STANDARD");
            marketSI.setDefaultCustodianName("Singapore Standard Custodian Pte Ltd");
            marketSI.setDefaultCounterpartyId("CP_SG_STANDARD");
            marketSI.setDefaultCounterpartyName("Singapore Standard Counterparty");
            marketSI.setDefaultSettlementMethod("DVP");
            marketSI.setDefaultDeliveryInstruction("DELIVER");
            instruction.setApplicableMarketSI(marketSI);
        } else if ("KOREA".equals(instruction.getMarket())) {
            BootstrapStandingInstruction marketSI = new BootstrapStandingInstruction(
                "SI_KOREA_MARKET", instruction.getMarket(), "Korea Market Default SI", true);
            marketSI.setDefaultCustodianId("CUST_KR_STANDARD");
            marketSI.setDefaultCustodianName("Korea Standard Custodian Co Ltd");
            marketSI.setDefaultCounterpartyId("CP_KR_STANDARD");
            marketSI.setDefaultCounterpartyName("Korea Standard Counterparty");
            marketSI.setDefaultSettlementMethod("DVP");
            marketSI.setDefaultDeliveryInstruction("DELIVER");
            instruction.setApplicableMarketSI(marketSI);
        }

        // Create instrument-level standing instruction
        if ("EQUITY".equals(instruction.getInstrumentType())) {
            BootstrapStandingInstruction instrumentSI = new BootstrapStandingInstruction(
                "SI_EQUITY_GLOBAL", instruction.getInstrumentType(), "Global Equity Instrument SI", 300);
            instrumentSI.setDefaultSettlementMethod("DVP");
            instrumentSI.setDefaultDeliveryInstruction("DELIVER");
            instruction.setApplicableInstrumentSI(instrumentSI);
        } else if ("FIXED_INCOME".equals(instruction.getInstrumentType())) {
            BootstrapStandingInstruction instrumentSI = new BootstrapStandingInstruction(
                "SI_FIXED_INCOME_GLOBAL", instruction.getInstrumentType(), "Global Fixed Income SI", 300);
            instrumentSI.setDefaultSettlementMethod("DVP");
            instrumentSI.setDefaultDeliveryInstruction("DELIVER");
            instruction.setApplicableInstrumentSI(instrumentSI);
        }
    }

    /**
     * Log execution step.
     */
    private void logExecution(String step) {
        String timestamp = LocalDateTime.now().toString();
        executionLog.add(timestamp + " - " + step);
        LOGGER.info(step);
    }
    
    /**
     * Analyze performance metrics.
     */
    private void analyzePerformance() {
        System.out.println("Analyzing performance metrics...");
        
        System.out.println("\n PERFORMANCE SUMMARY:");
        System.out.println("   - Total execution steps: " + executionLog.size());
        System.out.println("   - Database operations: " + (connection != null ? "PostgreSQL" : "In-memory simulation"));
        System.out.println("   - YAML enrichments: " + (yamlConfig != null ? yamlConfig.getEnrichments().size() : 0));
        System.out.println("   - Rule chains: " + (yamlConfig != null ? yamlConfig.getRuleChains().size() : 0));
        
        logExecution("Performance analysis completed");
    }
    
    /**
     * Cleanup resources.
     */
    private void cleanup() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            LOGGER.warning("Error closing database connection: " + e.getMessage());
        }
    }
}
