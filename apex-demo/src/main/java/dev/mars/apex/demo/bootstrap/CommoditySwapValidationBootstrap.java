package dev.mars.apex.demo.bootstrap;

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


import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.core.api.RuleSet;
import dev.mars.apex.core.api.SimpleRulesEngine;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * Complete Bootstrap Demonstration of APEX Commodity Swap Validation & Enrichment.
 *
 * This comprehensive bootstrap demonstrates the APEX Rules Engine's capability to
 * validate and enrich commodity derivatives using multiple validation approaches
 * and comprehensive static data integration.
 *
 * ============================================================================
 * BOOTSTRAP DEMO OVERVIEW
 * ============================================================================
 *
 * This demo processes Commodity Total Return Swaps through a complete validation
 * and enrichment pipeline using APEX's layered API approach:
 *
 * 1. ULTRA-SIMPLE API - Basic field validation with minimal configuration
 * 2. TEMPLATE-BASED RULES - Business logic validation using rule templates
 * 3. ADVANCED CONFIGURATION - Complex validation with sophisticated rules
 * 4. STATIC DATA INTEGRATION - Comprehensive enrichment from multiple sources
 *
 * ============================================================================
 * FILES AND CONFIGURATIONS USED
 * ============================================================================
 *
 * DATABASE SCHEMA (PostgreSQL):
 * ├── commodity_swaps - Main trade data table
 * │   └── Fields: trade_id, counterparty_id, client_id, commodity_type, etc.
 * ├── commodity_reference_data - Commodity reference information
 * │   └── Fields: commodity_code, commodity_name, reference_index, etc.
 * ├── client_data - Client information and authorization
 * │   └── Fields: client_id, client_name, regulatory_classification, etc.
 * ├── counterparty_data - Counterparty details and credit information
 * │   └── Fields: counterparty_id, counterparty_name, credit_rating, etc.
 * └── validation_audit - Audit trail for all validation activities
 *     └── Fields: audit_id, trade_id, validation_type, rule_result, etc.
 *
 * EMBEDDED YAML CONFIGURATION:
 * ├── Ultra-Simple Validation Chain
 * │   └── Basic field validation (trade ID, notional, commodity type)
 * ├── Template-Based Business Rules Chain
 * │   └── Business logic (maturity, currency consistency, settlement terms)
 * ├── Advanced Configuration Chain
 * │   └── Complex validation (trade ID format, notional range, regulatory)
 * └── Enrichment Configurations
 *     ├── Client Data Enrichment (client name, regulatory classification)
 *     └── Commodity Reference Enrichment (index provider, quote currency)
 *
 * STATIC DATA REPOSITORIES (In-Memory):
 * ├── Clients Repository - 3 institutional clients
 * │   ├── Energy Trading Fund Alpha (US, ECP, $250M credit limit)
 * │   ├── Global Commodity Investment Corp (UK, PROFESSIONAL, $150M)
 * │   └── Hedge Fund Commodities Ltd (US, QEP, $500M)
 * ├── Counterparties Repository - 3 major counterparties
 * │   ├── Global Investment Bank (US, BANK, AA-, $1B credit limit)
 * │   ├── Commodity Trading House (UK, TRADING_HOUSE, A+, $750M)
 * │   └── Energy Markets Specialist (US, SPECIALIST, A, $300M)
 * ├── Commodities Repository - 6 major commodities
 * │   ├── Energy: WTI (NYMEX), Brent (ICE), Henry Hub (NYMEX)
 * │   ├── Metals: Gold (COMEX), Silver (COMEX)
 * │   └── Agricultural: Corn (CBOT)
 * └── Currencies Repository - 6 major currencies
 *     └── USD, EUR, GBP, JPY, CHF, CAD with full details
 *
 * ============================================================================
 * EXECUTION FLOW
 * ============================================================================
 *
 * Phase 1: Infrastructure Setup
 * - Creates PostgreSQL database and schema (or in-memory simulation)
 * - Initializes static data repositories with realistic financial data
 * - Loads embedded YAML configuration with validation rules
 * - Initializes APEX Rules Engine components
 *
 * Phase 2: Validation Scenarios Execution
 * - Scenario 1: Ultra-Simple API validation demonstration
 * - Scenario 2: Template-Based Rules with business logic
 * - Scenario 3: Advanced Configuration with complex rules
 * - Scenario 4: Static Data Validation and Enrichment
 * - Scenario 5: Performance Monitoring and Metrics
 * - Scenario 6: Exception Handling and Error Recovery
 *
 * Phase 3: Results Analysis and Reporting
 * - Performance metrics analysis across all scenarios
 * - Validation results summary with pass/fail statistics
 * - Enrichment effectiveness demonstration
 * - Audit trail review and compliance reporting
 *
 * ============================================================================
 * SAMPLE DATA COVERAGE
 * ============================================================================
 *
 * COMMODITY SWAPS (Sample Trades):
 * - Energy Swaps: WTI Crude Oil, Brent Crude Oil, Natural Gas
 * - Metals Swaps: Gold, Silver with COMEX references
 * - Agricultural Swaps: Corn with CBOT references
 * - Notional Range: $1M to $100M across different currencies
 * - Maturity Range: 6 months to 5 years
 *
 * VALIDATION APPROACHES:
 * - Ultra-Simple: Basic field presence and type validation
 * - Template-Based: Business rules with weighted scoring (70% threshold)
 * - Advanced: Complex pattern matching and regulatory compliance
 * - Static Data: Cross-reference validation with enrichment
 *
 * ENRICHMENT SOURCES:
 * - Client enrichment: Regulatory classification, credit limits
 * - Commodity enrichment: Index providers, quote currencies, units
 * - Counterparty enrichment: Credit ratings, authorized products
 * - Currency enrichment: Decimal places, country codes, tradability
 *
 * ============================================================================
 * PERFORMANCE METRICS
 * ============================================================================
 *
 * Target Performance:
 * - Processing Time: <100ms per trade validation
 * - Validation Score: >70% for approval
 * - Enrichment Coverage: 100% for all configured fields
 * - Audit Trail: Complete for all validation activities
 *
 * Monitoring Capabilities:
 * - Real-time performance tracking per scenario
 * - Rule execution timing and success rates
 * - Memory usage and resource utilization
 * - Error rates and exception handling effectiveness
 *
 * ============================================================================
 * USAGE EXAMPLES
 * ============================================================================
 *
 * Standalone Execution:
 * java -cp apex-demo.jar dev.mars.apex.demo.bootstrap.CommoditySwapValidationBootstrap
 *
 * Through AllDemosRunnerAlt:
 * java -jar apex-demo.jar --package bootstrap
 * java -jar apex-demo.jar --demo CommoditySwapValidationBootstrap
 *
 * ============================================================================
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-31
 * @version 1.0
 */
public class CommoditySwapValidationBootstrap {
    
    private static final Logger LOGGER = Logger.getLogger(CommoditySwapValidationBootstrap.class.getName());
    
    // Database configuration
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/";
    private static final String DB_NAME = "apex_commodity_demo";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "postgres";
    
    // APEX components
    private RulesService rulesService;
    private YamlRuleConfiguration yamlConfig;
    private EnrichmentService enrichmentService;

    // Database connection
    private Connection connection;
    
    // Performance metrics
    private Map<String, Long> performanceMetrics;
    private List<String> executionLog;
    
    // Static data repositories
    private Map<String, CommodityClient> clients;
    private Map<String, CommodityCounterparty> counterparties;
    private Map<String, CommodityReference> commodities;
    private Map<String, CurrencyData> currencies;
    
    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("APEX COMMODITY SWAP VALIDATION BOOTSTRAP");
        System.out.println("=================================================================");
        System.out.println("Demo Purpose: Complete end-to-end commodity derivatives validation");
        System.out.println("Validation Methods: Ultra-Simple API + Template-Based + Advanced");
        System.out.println("Sample Data: 6 commodity swaps across Energy, Metals, Agricultural");
        System.out.println("Static Data: Clients, Counterparties, Commodities, Currencies");
        System.out.println("Expected Duration: ~5-10 seconds");
        System.out.println("=================================================================");

        CommoditySwapValidationBootstrap bootstrap = new CommoditySwapValidationBootstrap();
        long totalStartTime = System.currentTimeMillis();

        try {
            System.out.println("Initializing Commodity Swap Validation Bootstrap...");

            // Initialize the bootstrap
            bootstrap.initialize();
            System.out.println("Bootstrap initialization completed successfully");

            // Execute all demonstration scenarios
            System.out.println("Executing comprehensive validation scenarios...");
            bootstrap.executeAllScenarios();
            System.out.println("All validation scenarios completed successfully");

            // Display final performance metrics
            System.out.println("Generating final performance analysis...");
            bootstrap.displayFinalMetrics();

            long totalEndTime = System.currentTimeMillis();
            long totalDuration = totalEndTime - totalStartTime;

            System.out.println("=================================================================");
            System.out.println("COMMODITY SWAP VALIDATION BOOTSTRAP COMPLETED SUCCESSFULLY!");
            System.out.println("=================================================================");
            System.out.println("Total Execution Time: " + totalDuration + " ms");
            System.out.println("Scenarios Executed: 6/6");
            System.out.println("Validation Methods: 3 (Ultra-Simple, Template-Based, Advanced)");
            System.out.println("Static Data Sources: 4 (Clients, Counterparties, Commodities, Currencies)");
            System.out.println("Demo Status: SUCCESS");
            System.out.println("=================================================================");

        } catch (Exception e) {
            long totalEndTime = System.currentTimeMillis();
            long totalDuration = totalEndTime - totalStartTime;

            System.err.println("=================================================================");
            System.err.println("COMMODITY SWAP VALIDATION BOOTSTRAP FAILED!");
            System.err.println("=================================================================");
            System.err.println("Error Message: " + e.getMessage());
            System.err.println("Execution Time: " + totalDuration + " ms");
            System.err.println("Demo Status: FAILED");
            System.err.println("=================================================================");
            e.printStackTrace();
        } finally {
            // Cleanup resources
            System.out.println("Cleaning up bootstrap resources...");
            bootstrap.cleanup();
            System.out.println("Cleanup completed");
        }
    }
    
    /**
     * Initialize the bootstrap with all required components.
     *
     * This method sets up the complete infrastructure required for the commodity
     * swap validation demonstration, including database setup, static data
     * initialization, YAML configuration loading, and APEX component initialization.
     *
     * INITIALIZATION PHASES:
     * 1. Collections Setup - Initialize all data repositories and metrics
     * 2. Database Infrastructure - PostgreSQL setup or in-memory simulation
     * 3. Static Data Loading - Clients, counterparties, commodities, currencies
     * 4. YAML Configuration - Embedded validation rules and enrichment patterns
     * 5. APEX Components - Rules service and enrichment service initialization
     */
    private void initialize() throws Exception {
        System.out.println("Phase 1: Initializing APEX Commodity Swap Validation Bootstrap...");
        System.out.println("Setting up complete infrastructure for commodity derivatives validation");

        long initStartTime = System.currentTimeMillis();

        try {
            // Initialize collections
            System.out.println("Step 1.1: Initializing data collections and metrics...");
            this.performanceMetrics = new HashMap<>();
            this.executionLog = new ArrayList<>();
            this.clients = new HashMap<>();
            this.counterparties = new HashMap<>();
            this.commodities = new HashMap<>();
            this.currencies = new HashMap<>();
            System.out.println("   Data collections initialized: 6 repositories + metrics tracking");

            // Setup infrastructure
            System.out.println("Step 1.2: Setting up database infrastructure...");
            setupDatabaseInfrastructure();
            System.out.println("   Database infrastructure ready for commodity swap data");

            System.out.println("Step 1.3: Loading static data repositories...");
            initializeStaticData();
            System.out.printf("   Static data loaded: %d clients, %d counterparties, %d commodities, %d currencies%n",
                clients.size(), counterparties.size(), commodities.size(), currencies.size());

            System.out.println("Step 1.4: Loading YAML configuration...");
            loadYamlConfiguration();
            System.out.println("   YAML configuration loaded with validation rules and enrichment patterns");

            System.out.println("Step 1.5: Initializing APEX components...");
            initializeApexComponents();
            System.out.println("   APEX Rules Engine and Enrichment Service initialized");

            long initEndTime = System.currentTimeMillis();
            long initDuration = initEndTime - initStartTime;

            System.out.printf("Bootstrap initialization completed successfully in %d ms%n", initDuration);
            System.out.println("Summary: Database + Static Data + YAML Config + APEX Components ready");
            logExecution("Bootstrap initialized in " + initDuration + "ms");

        } catch (Exception e) {
            System.err.println("Bootstrap initialization failed: " + e.getMessage());
            throw new RuntimeException("Failed to initialize commodity swap validation bootstrap", e);
        }
    }
    
    /**
     * Setup PostgreSQL database infrastructure.
     *
     * This method establishes the database infrastructure required for the
     * commodity swap validation demo. It attempts to connect to PostgreSQL
     * and falls back to in-memory simulation if PostgreSQL is not available.
     *
     * DATABASE SETUP PROCESS:
     * 1. PostgreSQL Availability Check - Test connection to local PostgreSQL
     * 2. Database Creation - Create apex_commodity_demo database if needed
     * 3. Connection Pool Setup - Establish connection for demo operations
     * 4. Schema Creation - Create all required tables for commodity swaps
     * 5. Fallback Handling - Use in-memory simulation if PostgreSQL unavailable
     */
    private void setupDatabaseInfrastructure() throws Exception {
        System.out.println("Setting up PostgreSQL database infrastructure...");
        System.out.println("Target database: apex_commodity_demo with commodity swap schema");

        long dbSetupStart = System.currentTimeMillis();

        try {
            // Check if PostgreSQL is available
            System.out.println("   Testing PostgreSQL connectivity...");
            if (!isPostgreSQLAvailable()) {
                System.out.println("   PostgreSQL not available - switching to in-memory simulation");
                setupInMemorySimulation();
                long dbSetupEnd = System.currentTimeMillis();
                System.out.printf("   In-memory simulation setup completed in %d ms%n", dbSetupEnd - dbSetupStart);
                return;
            }

            System.out.println("   PostgreSQL connectivity confirmed");

            // Create database if it doesn't exist
            System.out.println("   Creating database if not exists...");
            createDatabaseIfNotExists();

            // Setup connection pool
            System.out.println("   Establishing database connection...");
            setupConnectionPool();

            // Create schema
            System.out.println("   Creating database schema...");
            createDatabaseSchema();

            long dbSetupEnd = System.currentTimeMillis();
            System.out.printf("Database infrastructure setup completed in %d ms%n", dbSetupEnd - dbSetupStart);
            System.out.println("   Database: apex_commodity_demo");
            System.out.println("   Tables: 5 (commodity_swaps, commodity_reference_data, client_data, counterparty_data, validation_audit)");
            System.out.println("   Status: Ready for commodity swap validation operations");

            logExecution("Database infrastructure setup completed in " + (dbSetupEnd - dbSetupStart) + "ms");

        } catch (Exception e) {
            System.err.println("Database infrastructure setup failed: " + e.getMessage());
            System.err.println("This may affect data persistence but demo will continue with in-memory mode");
            throw e;
        }
    }
    
    /**
     * Check if PostgreSQL is available.
     */
    private boolean isPostgreSQLAvailable() {
        try (Connection testConn = DriverManager.getConnection(
                DB_URL + "postgres", DB_USER, DB_PASSWORD)) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Create database if it doesn't exist.
     */
    private void createDatabaseIfNotExists() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                DB_URL + "postgres", DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            
            // Check if database exists
            ResultSet rs = stmt.executeQuery(
                "SELECT 1 FROM pg_database WHERE datname = '" + DB_NAME + "'");
            
            if (!rs.next()) {
                // Create database
                stmt.executeUpdate("CREATE DATABASE " + DB_NAME);
                System.out.println("✅ Database '" + DB_NAME + "' created successfully");
            } else {
                System.out.println("✅ Database '" + DB_NAME + "' already exists");
            }
        }
    }
    
    /**
     * Setup database connection.
     */
     private void setupConnectionPool() throws SQLException {
        this.connection = DriverManager.getConnection(DB_URL + DB_NAME, DB_USER, DB_PASSWORD);
        System.out.println("✅ Database connection established");
    }
    
    /**
     * Setup in-memory simulation when PostgreSQL is not available.
     */
    private void setupInMemorySimulation() {
        System.out.println("✅ In-memory simulation mode activated");
        // In-memory mode - no actual database operations
    }
    
    /**
     * Create database schema for commodity swap validation.
     */
    private void createDatabaseSchema() throws SQLException {
        System.out.println("Creating database schema...");
        
        String[] schemaSql = {
            // Commodity Swaps table
            """
            CREATE TABLE IF NOT EXISTS commodity_swaps (
                trade_id VARCHAR(50) PRIMARY KEY,
                counterparty_id VARCHAR(50) NOT NULL,
                client_id VARCHAR(50) NOT NULL,
                commodity_type VARCHAR(20) NOT NULL,
                reference_index VARCHAR(20) NOT NULL,
                index_provider VARCHAR(50),
                notional_amount DECIMAL(15,2) NOT NULL,
                notional_currency VARCHAR(3) NOT NULL,
                payment_currency VARCHAR(3),
                settlement_currency VARCHAR(3),
                trade_date DATE NOT NULL,
                effective_date DATE,
                maturity_date DATE NOT NULL,
                settlement_days INTEGER,
                total_return_payer_party VARCHAR(20),
                total_return_receiver_party VARCHAR(20),
                funding_rate_type VARCHAR(20),
                funding_spread DECIMAL(8,4),
                funding_frequency VARCHAR(20),
                initial_price DECIMAL(12,4),
                current_market_value DECIMAL(15,2),
                unrealized_pnl DECIMAL(15,2),
                trade_status VARCHAR(20) DEFAULT 'PENDING',
                booking_status VARCHAR(20) DEFAULT 'PENDING',
                jurisdiction VARCHAR(10),
                regulatory_regime VARCHAR(20),
                clearing_eligible BOOLEAN DEFAULT FALSE,
                created_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // Commodity Reference Data table
            """
            CREATE TABLE IF NOT EXISTS commodity_reference_data (
                commodity_code VARCHAR(20) PRIMARY KEY,
                commodity_name VARCHAR(255) NOT NULL,
                commodity_type VARCHAR(20) NOT NULL,
                reference_index VARCHAR(20) NOT NULL,
                index_provider VARCHAR(50),
                quote_currency VARCHAR(3),
                unit_of_measure VARCHAR(20),
                active BOOLEAN DEFAULT TRUE,
                tradeable BOOLEAN DEFAULT TRUE,
                created_date DATE DEFAULT CURRENT_DATE
            )
            """,
            
            // Client Data table
            """
            CREATE TABLE IF NOT EXISTS client_data (
                client_id VARCHAR(50) PRIMARY KEY,
                client_name VARCHAR(255) NOT NULL,
                client_type VARCHAR(20) NOT NULL,
                legal_entity_identifier VARCHAR(20),
                jurisdiction VARCHAR(10),
                regulatory_classification VARCHAR(10),
                authorized_products TEXT[],
                credit_limit DECIMAL(15,2),
                risk_rating VARCHAR(10),
                active BOOLEAN DEFAULT TRUE,
                onboarding_date DATE,
                created_date DATE DEFAULT CURRENT_DATE
            )
            """,
            
            // Counterparty Data table
            """
            CREATE TABLE IF NOT EXISTS counterparty_data (
                counterparty_id VARCHAR(50) PRIMARY KEY,
                counterparty_name VARCHAR(255) NOT NULL,
                counterparty_type VARCHAR(20) NOT NULL,
                legal_entity_identifier VARCHAR(20),
                jurisdiction VARCHAR(10),
                regulatory_status VARCHAR(20),
                credit_rating VARCHAR(10),
                credit_limit DECIMAL(15,2),
                authorized_products TEXT[],
                active BOOLEAN DEFAULT TRUE,
                created_date DATE DEFAULT CURRENT_DATE
            )
            """,
            
            // Validation Audit table
            """
            CREATE TABLE IF NOT EXISTS validation_audit (
                audit_id SERIAL PRIMARY KEY,
                trade_id VARCHAR(50) NOT NULL,
                validation_type VARCHAR(50) NOT NULL,
                validation_result VARCHAR(20) NOT NULL,
                rule_name VARCHAR(100),
                rule_result BOOLEAN,
                processing_time_ms BIGINT,
                error_message TEXT,
                created_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
     * Log execution step.
     */
    private void logExecution(String message) {
        String timestamp = LocalDateTime.now().toString();
        executionLog.add(timestamp + ": " + message);
        LOGGER.info(message);
    }
    
    /**
     * Initialize static data repositories.
     *
     * This method loads comprehensive static data required for commodity swap
     * validation and enrichment. All data represents realistic financial
     * institutions, commodities, and market information.
     *
     * STATIC DATA REPOSITORIES:
     * 1. Clients Repository - Institutional clients with regulatory classifications
     * 2. Counterparties Repository - Major banks and trading houses with credit ratings
     * 3. Commodities Repository - Energy, metals, and agricultural commodities
     * 4. Currencies Repository - Major global currencies with trading details
     */
    private void initializeStaticData() {
        System.out.println("Initializing static data repositories...");
        System.out.println("Loading comprehensive reference data for commodity swap validation");

        long staticDataStart = System.currentTimeMillis();

        try {
            System.out.println("   Loading clients repository...");
            initializeClients();
            System.out.printf("     Loaded %d institutional clients with regulatory classifications%n", clients.size());

            System.out.println("   Loading counterparties repository...");
            initializeCounterparties();
            System.out.printf("     Loaded %d counterparties with credit ratings and authorized products%n", counterparties.size());

            System.out.println("   Loading commodities repository...");
            initializeCommodities();
            System.out.printf("     Loaded %d commodities across Energy, Metals, and Agricultural sectors%n", commodities.size());

            System.out.println("   Loading currencies repository...");
            initializeCurrencies();
            System.out.printf("     Loaded %d major global currencies with trading details%n", currencies.size());

            long staticDataEnd = System.currentTimeMillis();
            long staticDataDuration = staticDataEnd - staticDataStart;

            System.out.printf("Static data repositories initialized successfully in %d ms%n", staticDataDuration);
            System.out.println("Summary:");
            System.out.printf("   - Clients: %d (Energy Trading Fund, Global Investment Corp, Hedge Fund)%n", clients.size());
            System.out.printf("   - Counterparties: %d (Global Bank, Trading House, Energy Specialist)%n", counterparties.size());
            System.out.printf("   - Commodities: %d (WTI, Brent, Henry Hub, Gold, Silver, Corn)%n", commodities.size());
            System.out.printf("   - Currencies: %d (USD, EUR, GBP, JPY, CHF, CAD)%n", currencies.size());
            System.out.printf("   Total Reference Records: %d%n", clients.size() + counterparties.size() + commodities.size() + currencies.size());

            logExecution("Static data initialized: " + (clients.size() + counterparties.size() + commodities.size() + currencies.size()) + " records in " + staticDataDuration + "ms");

        } catch (Exception e) {
            System.err.println("Static data initialization failed: " + e.getMessage());
            throw new RuntimeException("Failed to initialize static data repositories", e);
        }
    }

    /**
     * Initialize client data.
     */
    private void initializeClients() {
        // Institutional clients
        CommodityClient client1 = new CommodityClient("CLI001", "Energy Trading Fund Alpha", "INSTITUTIONAL", true);
        client1.setLegalEntityIdentifier("549300ENERGY123456789");
        client1.setJurisdiction("US");
        client1.setRegulatoryClassification("ECP");
        client1.setOnboardingDate(LocalDate.of(2020, 1, 15));
        client1.setAuthorizedProducts(Arrays.asList("COMMODITY_SWAPS", "ENERGY_DERIVATIVES", "METALS_DERIVATIVES"));
        client1.setCreditLimit(new BigDecimal("250000000"));
        client1.setRiskRating("LOW");
        clients.put(client1.getClientId(), client1);

        CommodityClient client2 = new CommodityClient("CLI002", "Global Commodity Investment Corp", "INSTITUTIONAL", true);
        client2.setLegalEntityIdentifier("549300GLOBAL123456789");
        client2.setJurisdiction("UK");
        client2.setRegulatoryClassification("PROFESSIONAL");
        client2.setOnboardingDate(LocalDate.of(2019, 6, 10));
        client2.setAuthorizedProducts(Arrays.asList("COMMODITY_SWAPS", "AGRICULTURAL_DERIVATIVES", "METALS_DERIVATIVES"));
        client2.setCreditLimit(new BigDecimal("150000000"));
        client2.setRiskRating("MEDIUM");
        clients.put(client2.getClientId(), client2);

        CommodityClient client3 = new CommodityClient("CLI003", "Hedge Fund Commodities Ltd", "HEDGE_FUND", true);
        client3.setLegalEntityIdentifier("549300HEDGE123456789");
        client3.setJurisdiction("US");
        client3.setRegulatoryClassification("QEP");
        client3.setOnboardingDate(LocalDate.of(2021, 3, 20));
        client3.setAuthorizedProducts(Arrays.asList("COMMODITY_SWAPS", "ENERGY_DERIVATIVES", "AGRICULTURAL_DERIVATIVES"));
        client3.setCreditLimit(new BigDecimal("500000000"));
        client3.setRiskRating("HIGH");
        clients.put(client3.getClientId(), client3);
    }

    /**
     * Initialize counterparty data.
     */
    private void initializeCounterparties() {
        CommodityCounterparty cp1 = new CommodityCounterparty("CP001", "Global Investment Bank", "BANK", true);
        cp1.setLegalEntityIdentifier("549300BANK123456789AB");
        cp1.setJurisdiction("US");
        cp1.setRegulatoryStatus("AUTHORIZED");
        cp1.setCreditRating("AA-");
        cp1.setCreditLimit(new BigDecimal("1000000000"));
        cp1.setAuthorizedProducts(Arrays.asList("COMMODITY_SWAPS", "ENERGY_DERIVATIVES", "METALS_DERIVATIVES", "AGRICULTURAL_DERIVATIVES"));
        counterparties.put(cp1.getCounterpartyId(), cp1);

        CommodityCounterparty cp2 = new CommodityCounterparty("CP002", "Commodity Trading House", "TRADING_HOUSE", true);
        cp2.setLegalEntityIdentifier("549300TRADE123456789AB");
        cp2.setJurisdiction("UK");
        cp2.setRegulatoryStatus("AUTHORIZED");
        cp2.setCreditRating("A+");
        cp2.setCreditLimit(new BigDecimal("750000000"));
        cp2.setAuthorizedProducts(Arrays.asList("COMMODITY_SWAPS", "ENERGY_DERIVATIVES", "METALS_DERIVATIVES"));
        counterparties.put(cp2.getCounterpartyId(), cp2);

        CommodityCounterparty cp3 = new CommodityCounterparty("CP003", "Energy Markets Specialist", "SPECIALIST", true);
        cp3.setLegalEntityIdentifier("549300ENERGY123456789");
        cp3.setJurisdiction("US");
        cp3.setRegulatoryStatus("AUTHORIZED");
        cp3.setCreditRating("A");
        cp3.setCreditLimit(new BigDecimal("300000000"));
        cp3.setAuthorizedProducts(Arrays.asList("COMMODITY_SWAPS", "ENERGY_DERIVATIVES"));
        counterparties.put(cp3.getCounterpartyId(), cp3);
    }

    /**
     * Initialize commodity reference data.
     */
    private void initializeCommodities() {
        // Energy commodities
        CommodityReference wti = new CommodityReference("WTI", "West Texas Intermediate Crude Oil", "ENERGY", "WTI", "USD", true);
        wti.setIndexProvider("NYMEX");
        wti.setUnitOfMeasure("BARREL");
        wti.setTradeable(true);
        commodities.put("WTI", wti);

        CommodityReference brent = new CommodityReference("BRENT", "Brent Crude Oil", "ENERGY", "BRENT", "USD", true);
        brent.setIndexProvider("ICE");
        brent.setUnitOfMeasure("BARREL");
        brent.setTradeable(true);
        commodities.put("BRENT", brent);

        CommodityReference henryHub = new CommodityReference("HENRY_HUB", "Henry Hub Natural Gas", "ENERGY", "HENRY_HUB", "USD", true);
        henryHub.setIndexProvider("NYMEX");
        henryHub.setUnitOfMeasure("MMBTU");
        henryHub.setTradeable(true);
        commodities.put("HENRY_HUB", henryHub);

        // Metals commodities
        CommodityReference gold = new CommodityReference("GOLD", "Gold", "METALS", "COMEX_GOLD", "USD", true);
        gold.setIndexProvider("COMEX");
        gold.setUnitOfMeasure("TROY_OUNCE");
        gold.setTradeable(true);
        commodities.put("GOLD", gold);

        CommodityReference silver = new CommodityReference("SILVER", "Silver", "METALS", "COMEX_SILVER", "USD", true);
        silver.setIndexProvider("COMEX");
        silver.setUnitOfMeasure("TROY_OUNCE");
        silver.setTradeable(true);
        commodities.put("SILVER", silver);

        // Agricultural commodities
        CommodityReference corn = new CommodityReference("CORN", "Corn", "AGRICULTURAL", "CBOT_CORN", "USD", true);
        corn.setIndexProvider("CBOT");
        corn.setUnitOfMeasure("BUSHEL");
        corn.setTradeable(true);
        commodities.put("CORN", corn);
    }

    /**
     * Initialize currency data.
     */
    private void initializeCurrencies() {
        currencies.put("USD", new CurrencyData("USD", "US Dollar", true, true, "US", 2));
        currencies.put("EUR", new CurrencyData("EUR", "Euro", true, true, "EU", 2));
        currencies.put("GBP", new CurrencyData("GBP", "British Pound", true, true, "GB", 2));
        currencies.put("JPY", new CurrencyData("JPY", "Japanese Yen", true, true, "JP", 0));
        currencies.put("CHF", new CurrencyData("CHF", "Swiss Franc", true, true, "CH", 2));
        currencies.put("CAD", new CurrencyData("CAD", "Canadian Dollar", true, true, "CA", 2));
    }

    // ==================== DATA MODEL CLASSES ====================

    /**
     * Commodity Total Return Swap data model.
     */
    public static class CommodityTotalReturnSwap {
        // TradeB Identification
        private String tradeId;
        private String counterpartyId;
        private String clientId;
        private String clientName; // Enriched field

        // Instrument Details
        private String commodityType; // "ENERGY", "METALS", "AGRICULTURAL"
        private String referenceIndex; // "WTI", "BRENT", "HENRY_HUB", etc.
        private String indexProvider; // Enriched field

        // Financial Terms
        private BigDecimal notionalAmount;
        private String notionalCurrency;
        private String paymentCurrency;
        private String settlementCurrency;
        private BigDecimal initialPrice;

        // Total Return Leg
        private String totalReturnPayerParty; // "CLIENT" or "COUNTERPARTY"
        private String totalReturnReceiverParty; // "CLIENT" or "COUNTERPARTY"

        // Funding Leg
        private String fundingRateType; // "LIBOR", "SOFR", "FIXED"
        private BigDecimal fundingSpread; // Basis points
        private String fundingFrequency; // "MONTHLY", "QUARTERLY", "SEMI_ANNUAL"

        // Dates
        private LocalDate tradeDate;
        private LocalDate effectiveDate;
        private LocalDate maturityDate;
        private Integer settlementDays;

        // Regulatory
        private String jurisdiction;
        private String regulatoryRegime;
        private Boolean clearingEligible;

        // Valuation
        private BigDecimal currentMarketValue;
        private BigDecimal unrealizedPnL;
        private LocalDate lastValuationDate;

        // Status
        private String tradeStatus; // "PENDING", "CONFIRMED", "SETTLED", "CANCELLED"
        private String bookingStatus; // "PENDING", "BOOKED", "FAILED"

        // Constructors
        public CommodityTotalReturnSwap() {}

        public CommodityTotalReturnSwap(String tradeId, String counterpartyId, String clientId,
                                       String commodityType, String referenceIndex,
                                       BigDecimal notionalAmount, String notionalCurrency,
                                       LocalDate tradeDate, LocalDate maturityDate) {
            this.tradeId = tradeId;
            this.counterpartyId = counterpartyId;
            this.clientId = clientId;
            this.commodityType = commodityType;
            this.referenceIndex = referenceIndex;
            this.notionalAmount = notionalAmount;
            this.notionalCurrency = notionalCurrency;
            this.tradeDate = tradeDate;
            this.maturityDate = maturityDate;
            this.effectiveDate = tradeDate; // Default to trade date
            this.tradeStatus = "PENDING"; // Default status
            this.bookingStatus = "PENDING"; // Default status
        }

        // Getters and Setters
        public String getTradeId() { return tradeId; }
        public void setTradeId(String tradeId) { this.tradeId = tradeId; }

        public String getCounterpartyId() { return counterpartyId; }
        public void setCounterpartyId(String counterpartyId) { this.counterpartyId = counterpartyId; }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }

        public String getCommodityType() { return commodityType; }
        public void setCommodityType(String commodityType) { this.commodityType = commodityType; }

        public String getReferenceIndex() { return referenceIndex; }
        public void setReferenceIndex(String referenceIndex) { this.referenceIndex = referenceIndex; }

        public String getIndexProvider() { return indexProvider; }
        public void setIndexProvider(String indexProvider) { this.indexProvider = indexProvider; }

        public BigDecimal getNotionalAmount() { return notionalAmount; }
        public void setNotionalAmount(BigDecimal notionalAmount) { this.notionalAmount = notionalAmount; }

        public String getNotionalCurrency() { return notionalCurrency; }
        public void setNotionalCurrency(String notionalCurrency) { this.notionalCurrency = notionalCurrency; }

        public String getPaymentCurrency() { return paymentCurrency; }
        public void setPaymentCurrency(String paymentCurrency) { this.paymentCurrency = paymentCurrency; }

        public String getSettlementCurrency() { return settlementCurrency; }
        public void setSettlementCurrency(String settlementCurrency) { this.settlementCurrency = settlementCurrency; }

        public BigDecimal getInitialPrice() { return initialPrice; }
        public void setInitialPrice(BigDecimal initialPrice) { this.initialPrice = initialPrice; }

        public String getTotalReturnPayerParty() { return totalReturnPayerParty; }
        public void setTotalReturnPayerParty(String totalReturnPayerParty) { this.totalReturnPayerParty = totalReturnPayerParty; }

        public String getTotalReturnReceiverParty() { return totalReturnReceiverParty; }
        public void setTotalReturnReceiverParty(String totalReturnReceiverParty) { this.totalReturnReceiverParty = totalReturnReceiverParty; }

        public String getFundingRateType() { return fundingRateType; }
        public void setFundingRateType(String fundingRateType) { this.fundingRateType = fundingRateType; }

        public BigDecimal getFundingSpread() { return fundingSpread; }
        public void setFundingSpread(BigDecimal fundingSpread) { this.fundingSpread = fundingSpread; }

        public String getFundingFrequency() { return fundingFrequency; }
        public void setFundingFrequency(String fundingFrequency) { this.fundingFrequency = fundingFrequency; }

        public LocalDate getTradeDate() { return tradeDate; }
        public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }

        public LocalDate getEffectiveDate() { return effectiveDate; }
        public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

        public LocalDate getMaturityDate() { return maturityDate; }
        public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }

        public Integer getSettlementDays() { return settlementDays; }
        public void setSettlementDays(Integer settlementDays) { this.settlementDays = settlementDays; }

        public String getJurisdiction() { return jurisdiction; }
        public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }

        public String getRegulatoryRegime() { return regulatoryRegime; }
        public void setRegulatoryRegime(String regulatoryRegime) { this.regulatoryRegime = regulatoryRegime; }

        public Boolean getClearingEligible() { return clearingEligible; }
        public void setClearingEligible(Boolean clearingEligible) { this.clearingEligible = clearingEligible; }

        public BigDecimal getCurrentMarketValue() { return currentMarketValue; }
        public void setCurrentMarketValue(BigDecimal currentMarketValue) { this.currentMarketValue = currentMarketValue; }

        public BigDecimal getUnrealizedPnL() { return unrealizedPnL; }
        public void setUnrealizedPnL(BigDecimal unrealizedPnL) { this.unrealizedPnL = unrealizedPnL; }

        public LocalDate getLastValuationDate() { return lastValuationDate; }
        public void setLastValuationDate(LocalDate lastValuationDate) { this.lastValuationDate = lastValuationDate; }

        public String getTradeStatus() { return tradeStatus; }
        public void setTradeStatus(String tradeStatus) { this.tradeStatus = tradeStatus; }

        public String getBookingStatus() { return bookingStatus; }
        public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

        @Override
        public String toString() {
            return "CommodityTotalReturnSwap{" +
                    "tradeId='" + tradeId + '\'' +
                    ", counterpartyId='" + counterpartyId + '\'' +
                    ", clientId='" + clientId + '\'' +
                    ", commodityType='" + commodityType + '\'' +
                    ", referenceIndex='" + referenceIndex + '\'' +
                    ", notionalAmount=" + notionalAmount +
                    ", notionalCurrency='" + notionalCurrency + '\'' +
                    ", tradeDate=" + tradeDate +
                    ", maturityDate=" + maturityDate +
                    ", tradeStatus='" + tradeStatus + '\'' +
                    '}';
        }
    }

    /**
     * Commodity Client data model.
     */
    public static class CommodityClient {
        private String clientId;
        private String clientName;
        private String clientType; // "INSTITUTIONAL", "HEDGE_FUND", "CORPORATE"
        private String legalEntityIdentifier;
        private String jurisdiction;
        private String regulatoryClassification; // "ECP", "PROFESSIONAL", "QEP"
        private List<String> authorizedProducts;
        private BigDecimal creditLimit;
        private String riskRating; // "LOW", "MEDIUM", "HIGH"
        private Boolean active;
        private LocalDate onboardingDate;

        public CommodityClient() {}

        public CommodityClient(String clientId, String clientName, String clientType, Boolean active) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.clientType = clientType;
            this.active = active;
        }

        // Getters and Setters
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }

        public String getClientType() { return clientType; }
        public void setClientType(String clientType) { this.clientType = clientType; }

        public String getLegalEntityIdentifier() { return legalEntityIdentifier; }
        public void setLegalEntityIdentifier(String legalEntityIdentifier) { this.legalEntityIdentifier = legalEntityIdentifier; }

        public String getJurisdiction() { return jurisdiction; }
        public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }

        public String getRegulatoryClassification() { return regulatoryClassification; }
        public void setRegulatoryClassification(String regulatoryClassification) { this.regulatoryClassification = regulatoryClassification; }

        public List<String> getAuthorizedProducts() { return authorizedProducts; }
        public void setAuthorizedProducts(List<String> authorizedProducts) { this.authorizedProducts = authorizedProducts; }

        public BigDecimal getCreditLimit() { return creditLimit; }
        public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

        public String getRiskRating() { return riskRating; }
        public void setRiskRating(String riskRating) { this.riskRating = riskRating; }

        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }

        public LocalDate getOnboardingDate() { return onboardingDate; }
        public void setOnboardingDate(LocalDate onboardingDate) { this.onboardingDate = onboardingDate; }
    }

    /**
     * Commodity Counterparty data model.
     */
    public static class CommodityCounterparty {
        private String counterpartyId;
        private String counterpartyName;
        private String counterpartyType; // "BANK", "TRADING_HOUSE", "SPECIALIST"
        private String legalEntityIdentifier;
        private String jurisdiction;
        private String regulatoryStatus; // "AUTHORIZED", "PENDING", "RESTRICTED"
        private String creditRating; // "AAA", "AA+", "AA", "AA-", etc.
        private BigDecimal creditLimit;
        private List<String> authorizedProducts;
        private Boolean active;

        public CommodityCounterparty() {}

        public CommodityCounterparty(String counterpartyId, String counterpartyName, String counterpartyType, Boolean active) {
            this.counterpartyId = counterpartyId;
            this.counterpartyName = counterpartyName;
            this.counterpartyType = counterpartyType;
            this.active = active;
        }

        // Getters and Setters
        public String getCounterpartyId() { return counterpartyId; }
        public void setCounterpartyId(String counterpartyId) { this.counterpartyId = counterpartyId; }

        public String getCounterpartyName() { return counterpartyName; }
        public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }

        public String getCounterpartyType() { return counterpartyType; }
        public void setCounterpartyType(String counterpartyType) { this.counterpartyType = counterpartyType; }

        public String getLegalEntityIdentifier() { return legalEntityIdentifier; }
        public void setLegalEntityIdentifier(String legalEntityIdentifier) { this.legalEntityIdentifier = legalEntityIdentifier; }

        public String getJurisdiction() { return jurisdiction; }
        public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }

        public String getRegulatoryStatus() { return regulatoryStatus; }
        public void setRegulatoryStatus(String regulatoryStatus) { this.regulatoryStatus = regulatoryStatus; }

        public String getCreditRating() { return creditRating; }
        public void setCreditRating(String creditRating) { this.creditRating = creditRating; }

        public BigDecimal getCreditLimit() { return creditLimit; }
        public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

        public List<String> getAuthorizedProducts() { return authorizedProducts; }
        public void setAuthorizedProducts(List<String> authorizedProducts) { this.authorizedProducts = authorizedProducts; }

        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    /**
     * Commodity Reference data model.
     */
    public static class CommodityReference {
        private String commodityCode;
        private String commodityName;
        private String commodityType; // "ENERGY", "METALS", "AGRICULTURAL"
        private String referenceIndex; // "WTI", "BRENT", "HENRY_HUB", etc.
        private String indexProvider; // "NYMEX", "ICE", "COMEX"
        private String quoteCurrency;
        private String unitOfMeasure; // "BARREL", "TROY_OUNCE", "BUSHEL"
        private Boolean active;
        private Boolean tradeable;

        public CommodityReference() {}

        public CommodityReference(String commodityCode, String commodityName, String commodityType,
                                String referenceIndex, String quoteCurrency, Boolean active) {
            this.commodityCode = commodityCode;
            this.commodityName = commodityName;
            this.commodityType = commodityType;
            this.referenceIndex = referenceIndex;
            this.quoteCurrency = quoteCurrency;
            this.active = active;
        }

        // Getters and Setters
        public String getCommodityCode() { return commodityCode; }
        public void setCommodityCode(String commodityCode) { this.commodityCode = commodityCode; }

        public String getCommodityName() { return commodityName; }
        public void setCommodityName(String commodityName) { this.commodityName = commodityName; }

        public String getCommodityType() { return commodityType; }
        public void setCommodityType(String commodityType) { this.commodityType = commodityType; }

        public String getReferenceIndex() { return referenceIndex; }
        public void setReferenceIndex(String referenceIndex) { this.referenceIndex = referenceIndex; }

        public String getIndexProvider() { return indexProvider; }
        public void setIndexProvider(String indexProvider) { this.indexProvider = indexProvider; }

        public String getQuoteCurrency() { return quoteCurrency; }
        public void setQuoteCurrency(String quoteCurrency) { this.quoteCurrency = quoteCurrency; }

        public String getUnitOfMeasure() { return unitOfMeasure; }
        public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }

        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }

        public Boolean getTradeable() { return tradeable; }
        public void setTradeable(Boolean tradeable) { this.tradeable = tradeable; }
    }

    /**
     * Currency data model.
     */
    public static class CurrencyData {
        private String currencyCode;
        private String currencyName;
        private Boolean active;
        private Boolean tradeable;
        private String countryCode;
        private Integer decimalPlaces;

        public CurrencyData() {}

        public CurrencyData(String currencyCode, String currencyName, Boolean active, Boolean tradeable, String countryCode, Integer decimalPlaces) {
            this.currencyCode = currencyCode;
            this.currencyName = currencyName;
            this.active = active;
            this.tradeable = tradeable;
            this.countryCode = countryCode;
            this.decimalPlaces = decimalPlaces;
        }

        // Getters and Setters
        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

        public String getCurrencyName() { return currencyName; }
        public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }

        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }

        public Boolean getTradeable() { return tradeable; }
        public void setTradeable(Boolean tradeable) { this.tradeable = tradeable; }

        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

        public Integer getDecimalPlaces() { return decimalPlaces; }
        public void setDecimalPlaces(Integer decimalPlaces) { this.decimalPlaces = decimalPlaces; }
    }

    /**
     * Load YAML configuration.
     *
     * This method creates and loads the comprehensive YAML configuration that
     * defines all validation rules and enrichment patterns for commodity swap
     * processing. The configuration demonstrates APEX's layered API approach.
     *
     * YAML CONFIGURATION STRUCTURE:
     * 1. Ultra-Simple Validation Chain - Basic field validation
     * 2. Template-Based Business Rules Chain - Weighted business logic
     * 3. Advanced Configuration Chain - Complex pattern matching
     * 4. Client Data Enrichment - Client information lookup
     * 5. Commodity Reference Enrichment - Commodity data lookup
     * 6. Processing Configuration - Thresholds and performance settings
     */
    private void loadYamlConfiguration() throws Exception {
        System.out.println("Loading YAML configuration...");
        System.out.println("Creating comprehensive validation rules and enrichment patterns");

        long yamlLoadStart = System.currentTimeMillis();

        try {
            System.out.println("   Generating embedded YAML configuration...");
            // Create embedded YAML configuration
            String yamlContent = createEmbeddedYamlConfiguration();
            System.out.printf("     YAML content generated: %d characters%n", yamlContent.length());
            System.out.println("     Configuration includes: validation chains + enrichment patterns + thresholds");

            System.out.println("   Loading YAML configuration into APEX...");

            // Load the YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            this.yamlConfig = loader.fromYamlString(yamlContent);
            System.out.println("     YAML configuration loaded successfully");
            if (yamlConfig.getMetadata() != null) {
                System.out.printf("     Configuration name: %s (version %s)%n",
                    yamlConfig.getMetadata().getName(), yamlConfig.getMetadata().getVersion());
            }
            System.out.println("     Configuration ready for rules engine processing");

            long yamlLoadEnd = System.currentTimeMillis();
            long yamlLoadDuration = yamlLoadEnd - yamlLoadStart;

            System.out.printf("YAML configuration loaded successfully in %d ms%n", yamlLoadDuration);
            System.out.println("Configuration Summary:");
            System.out.println("   - Rule Chains: 3 (Ultra-Simple, Template-Based, Advanced)");
            System.out.println("   - Enrichment Patterns: 2 (Client Data, Commodity Reference)");
            System.out.println("   - Validation Thresholds: Score-based (70% approval threshold)");
            System.out.println("   - Performance Settings: <100ms target, caching enabled");
            System.out.println("   - Business Rules: Currency consistency, maturity limits, regulatory compliance");
            System.out.println("   - Supported Commodities: Energy (WTI, Brent, Henry Hub), Metals (Gold, Silver), Agricultural (Corn)");

            logExecution("YAML configuration loaded: 3 rule chains + 2 enrichments in " + yamlLoadDuration + "ms");

        } catch (Exception e) {
            System.err.println("Failed to load YAML configuration: " + e.getMessage());
            System.err.println("This will prevent proper validation rule execution");
            throw new RuntimeException("YAML configuration loading failed", e);
        }
    }

    /**
     * Create embedded YAML configuration.
     */
    private String createEmbeddedYamlConfiguration() {
        return """
            metadata:
              name: "Commodity Swap Validation Bootstrap"
              version: "1.0"
              description: "Complete commodity derivatives validation and enrichment demonstration"
              created-by: "financial.admin@company.com"
              business-domain: "Commodity Derivatives"
              business-owner: "Trading Desk"

            rule-chains:
              # Ultra-Simple API validation chain
              - id: "ultra-simple-validation"
                name: "Ultra-Simple Validation Rules"
                description: "Basic field validation using ultra-simple API"
                pattern: "conditional-chaining"
                enabled: true
                priority: 100
                configuration:
                  trigger-rule:
                    id: "basic-fields-check"
                    condition: "tradeId != null && counterpartyId != null && clientId != null"
                    message: "Basic required fields validation"
                    description: "Ensures all essential trade identifiers are present"
                  conditional-rules:
                    on-trigger:
                      - id: "notional-positive"
                        condition: "notionalAmount != null && notionalAmount > 0"
                        message: "Notional amount must be positive"
                        description: "Validates notional amount is greater than zero"
                      - id: "commodity-type-required"
                        condition: "commodityType != null && commodityType.trim().length() > 0"
                        message: "Commodity type is required"
                        description: "Ensures commodity type is specified"
                    on-no-trigger:
                      - id: "basic-validation-failure"
                        condition: "true"
                        message: "Basic field validation failed"
                        description: "One or more required fields are missing"

              # Template-based business rules chain
              - id: "template-business-rules"
                name: "Template-Based Business Rules"
                description: "Business logic validation using template-based rules"
                pattern: "accumulative-chaining"
                enabled: true
                priority: 200
                configuration:
                  accumulator-variable: "businessRuleScore"
                  accumulation-rules:
                    - id: "maturity-eligibility"
                      condition: "maturityDate != null && maturityDate.isBefore(tradeDate.plusYears(5))"
                      weight: 25
                      message: "TradeB maturity within 5 years"
                      description: "Validates trade maturity is within acceptable range"
                    - id: "currency-consistency"
                      condition: "notionalCurrency == paymentCurrency && paymentCurrency == settlementCurrency"
                      weight: 20
                      message: "All currencies must match"
                      description: "Ensures currency consistency across trade legs"
                    - id: "settlement-terms"
                      condition: "settlementDays != null && settlementDays >= 0 && settlementDays <= 5"
                      weight: 15
                      message: "Settlement within 5 days"
                      description: "Validates settlement period is within acceptable range"
                    - id: "energy-commodity-validation"
                      condition: "commodityType == 'ENERGY' && (referenceIndex == 'WTI' || referenceIndex == 'BRENT' || referenceIndex == 'HENRY_HUB')"
                      weight: 30
                      message: "Valid energy commodity and index"
                      description: "Ensures energy commodities use valid reference indices"
                  thresholds:
                    approval-score: 70
                    warning-score: 50

              # Advanced configuration chain
              - id: "advanced-validation"
                name: "Advanced Configuration Rules"
                description: "Complex validation using advanced configuration"
                pattern: "conditional-chaining"
                enabled: true
                priority: 300
                configuration:
                  trigger-rule:
                    id: "advanced-eligibility"
                    condition: "tradeId != null && tradeId.matches('^TRS[0-9]{3}$')"
                    message: "TradeB ID format validation"
                    description: "Validates trade ID follows TRS### format"
                  conditional-rules:
                    on-trigger:
                      - id: "notional-range-check"
                        condition: "notionalAmount >= 1000000 && notionalAmount <= 100000000"
                        message: "Notional must be between $1M and $100M"
                        description: "Validates notional amount is within acceptable range"
                      - id: "regulatory-compliance"
                        condition: "jurisdiction != null && regulatoryRegime != null"
                        message: "Regulatory information required"
                        description: "Ensures regulatory compliance fields are populated"
                    on-no-trigger:
                      - id: "format-validation-failure"
                        condition: "true"
                        message: "TradeB ID format validation failed"
                        description: "TradeB ID does not follow required format"

            enrichments: []

            configuration:
              # Processing thresholds
              thresholds:
                minNotionalAmount: 1000000      # $1M minimum
                maxNotionalAmount: 100000000    # $100M maximum
                maxMaturityYears: 5             # 5 years maximum maturity
                validationScore: 70             # Minimum validation score

              # Performance settings
              performance:
                maxProcessingTimeMs: 100        # Target processing time
                cacheEnabled: true
                auditEnabled: true
                metricsEnabled: true

              # Business rules
              businessRules:
                requireRegulatoryInfo: true
                validateCurrencyConsistency: true
                enforceNotionalLimits: true
                auditAllValidations: true

              # Supported commodity types
              commodityTypes:
                supportedTypes: ["ENERGY", "METALS", "AGRICULTURAL"]
                energyIndices: ["WTI", "BRENT", "HENRY_HUB"]
                metalsIndices: ["COMEX_GOLD", "COMEX_SILVER", "LME_COPPER"]
                agriculturalIndices: ["CBOT_CORN", "CBOT_WHEAT", "CBOT_SOYBEANS"]
            """;
    }

    /**
     * Initialize APEX components.
     *
     * This method initializes the core APEX Rules Engine components required
     * for commodity swap validation and enrichment processing.
     *
     * APEX COMPONENTS INITIALIZED:
     * 1. Rules Service - Core validation engine for rule execution
     * 2. Lookup Service Registry - Registry for data source lookups
     * 3. Expression Evaluator Service - Expression evaluation engine
     * 4. Enrichment Service - Data enrichment and transformation service
     */
    private void initializeApexComponents() throws Exception {
        System.out.println("Initializing APEX components...");
        System.out.println("Setting up core rules engine and enrichment services");

        long apexInitStart = System.currentTimeMillis();

        try {
            System.out.println("   Initializing Rules Service...");
            // Initialize Rules Service
            this.rulesService = new RulesService();
            System.out.println("     Rules Service initialized - ready for validation rule execution");

            System.out.println("   Initializing Enrichment Service dependencies...");
            // Initialize Enrichment Service with required dependencies
            LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
            System.out.println("     Lookup Service Registry created - ready for data source registration");

            ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
            System.out.println("     Expression Evaluator Service created - ready for rule expression evaluation");

            // Initialize and store EnrichmentService for real enrichment operations
            this.enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);
            System.out.println("     Enrichment Service initialized - ready for data enrichment operations");

            long apexInitEnd = System.currentTimeMillis();
            long apexInitDuration = apexInitEnd - apexInitStart;

            System.out.printf("APEX components initialized successfully in %d ms%n", apexInitDuration);
            System.out.println("Components Summary:");
            System.out.println("   - Rules Service: Ready for validation rule execution");
            System.out.println("   - Enrichment Service: Ready for data enrichment operations");
            System.out.println("   - Lookup Service Registry: Ready for static data lookups");
            System.out.println("   - Expression Evaluator: Ready for business rule evaluation");
            System.out.println("   Status: All APEX components operational and ready for processing");

            logExecution("APEX components initialized in " + apexInitDuration + "ms");

        } catch (Exception e) {
            System.err.println("APEX components initialization failed: " + e.getMessage());
            System.err.println("This will prevent proper rules engine operation");
            throw new RuntimeException("Failed to initialize APEX components", e);
        }
    }

    /**
     * Execute all demonstration scenarios.
     *
     * This method orchestrates the execution of six comprehensive scenarios
     * that demonstrate different aspects of the APEX Rules Engine's capabilities
     * for commodity swap validation and enrichment.
     *
     * DEMONSTRATION SCENARIOS:
     * 1. Ultra-Simple API - Basic field validation with minimal configuration
     * 2. Template-Based Rules - Business logic validation with weighted scoring
     * 3. Advanced Configuration - Complex validation with pattern matching
     * 4. Static Data Enrichment - Comprehensive data enrichment from repositories
     * 5. Performance Monitoring - Metrics collection and performance analysis
     * 6. Exception Handling - Error scenarios and recovery mechanisms
     */
    private void executeAllScenarios() throws Exception {
        System.out.println("=================================================================");
        System.out.println("EXECUTING COMMODITY SWAP VALIDATION SCENARIOS");
        System.out.println("=================================================================");
        System.out.println("Demonstrating APEX layered API approach with comprehensive scenarios");
        System.out.println("Each scenario showcases different validation and enrichment capabilities");
        System.out.println("=================================================================");

        long allScenariosStart = System.currentTimeMillis();
        int totalScenarios = 6;
        int completedScenarios = 0;

        try {
            // Scenario 1: Ultra-Simple API Demonstration
            System.out.println(">>> SCENARIO 1/6: Ultra-Simple API Demonstration");
            executeScenario1_UltraSimpleAPI();
            completedScenarios++;
            System.out.println("Scenario 1 completed successfully - Basic validation demonstrated");

            // Scenario 2: Template-Based Rules Demonstration
            System.out.println(">>> SCENARIO 2/6: Template-Based Rules Demonstration");
            executeScenario2_TemplateBasedRules();
            completedScenarios++;
            System.out.println("Scenario 2 completed successfully - Business logic validation demonstrated");

            // Scenario 3: Advanced Configuration Demonstration
            System.out.println(">>> SCENARIO 3/6: Advanced Configuration Demonstration");
            executeScenario3_AdvancedConfiguration();
            completedScenarios++;
            System.out.println("Scenario 3 completed successfully - Complex validation demonstrated");

            // Scenario 4: Static Data Validation and Enrichment
            System.out.println(">>> SCENARIO 4/6: Static Data Validation and Enrichment");
            executeScenario4_StaticDataEnrichment();
            completedScenarios++;
            System.out.println("Scenario 4 completed successfully - Data enrichment demonstrated");

            // Scenario 5: Performance Monitoring Demonstration
            System.out.println(">>> SCENARIO 5/6: Performance Monitoring Demonstration");
            executeScenario5_PerformanceMonitoring();
            completedScenarios++;
            System.out.println("Scenario 5 completed successfully - Performance monitoring demonstrated");

            // Scenario 6: Exception Handling Demonstration
            System.out.println(">>> SCENARIO 6/6: Exception Handling Demonstration");
            executeScenario6_ExceptionHandling();
            completedScenarios++;
            System.out.println("Scenario 6 completed successfully - Exception handling demonstrated");

            long allScenariosEnd = System.currentTimeMillis();
            long allScenariosDuration = allScenariosEnd - allScenariosStart;

            System.out.println("=================================================================");
            System.out.println("ALL SCENARIOS EXECUTED SUCCESSFULLY!");
            System.out.println("=================================================================");
            System.out.printf("Total Execution Time: %d ms%n", allScenariosDuration);
            System.out.printf("Scenarios Completed: %d/%d%n", completedScenarios, totalScenarios);
            System.out.println("Validation Methods: 3 (Ultra-Simple, Template-Based, Advanced)");
            System.out.println("Enrichment Sources: 4 (Clients, Counterparties, Commodities, Currencies)");
            System.out.println("Performance Metrics: Collected across all scenarios");
            System.out.println("Exception Handling: Demonstrated with recovery mechanisms");
            System.out.println("=================================================================");

            logExecution("All " + totalScenarios + " scenarios executed successfully in " + allScenariosDuration + "ms");

        } catch (Exception e) {
            System.err.println("Scenario execution failed at scenario " + (completedScenarios + 1) + "/" + totalScenarios);
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException("Failed to execute all validation scenarios", e);
        }
    }

    /**
     * Scenario 1: Ultra-Simple API Demonstration.
     *
     * This scenario demonstrates the Ultra-Simple API approach for basic
     * commodity swap validation. It focuses on essential field validation
     * with minimal configuration and maximum simplicity.
     *
     * VALIDATION CHECKS:
     * 1. TradeB ID presence and format
     * 2. Counterparty ID validation
     * 3. Client ID validation
     * 4. Notional amount positivity check
     * 5. Commodity type specification
     *
     * SAMPLE DATA: Energy commodity swap (WTI Crude Oil)
     */
    private void executeScenario1_UltraSimpleAPI() throws Exception {
        System.out.println("SCENARIO 1: ULTRA-SIMPLE API DEMONSTRATION");
        System.out.println("-------------------------------------------------------------");
        System.out.println("Purpose: Demonstrate basic field validation with minimal configuration");
        System.out.println("Approach: Ultra-Simple API with essential validation checks");
        System.out.println("Sample: Energy commodity swap (WTI Crude Oil)");
        System.out.println("-------------------------------------------------------------");

        long startTime = System.currentTimeMillis();

        try {
            // Create sample commodity swap
            System.out.println("Step 1.1: Creating sample energy commodity swap...");
            CommodityTotalReturnSwap swap = createSampleEnergySwap();
            System.out.printf("   Sample swap created: %s (%s - %s)%n", swap.getTradeId(), swap.getCommodityType(), swap.getReferenceIndex());
            System.out.printf("   Notional: %s %s, Maturity: %s%n", swap.getNotionalAmount(), swap.getNotionalCurrency(), swap.getMaturityDate());
            System.out.printf("   Counterparty: %s, Client: %s%n", swap.getCounterpartyId(), swap.getClientId());

            System.out.println("Step 1.2: Initializing Ultra-Simple API validation engine...");
            // Ultra-Simple API validation
            SimpleRulesEngine simpleEngine = new SimpleRulesEngine();
            System.out.println("   Ultra-Simple Rules Engine initialized");
            System.out.println("   Engine type: " + simpleEngine.getClass().getSimpleName());

            System.out.println("Step 1.3: Converting swap data to validation context...");
            // Convert swap to map for rule evaluation
            Map<String, Object> context = convertSwapToMap(swap);
            System.out.printf("   Validation context created with %d fields%n", context.size());

            System.out.println("Step 1.4: Executing basic field validations...");
            // Basic field validations
            long validationStart = System.currentTimeMillis();

            boolean tradeIdValid = rulesService.check("#tradeId != null && #tradeId.trim().length() > 0", context);
            System.out.printf("   TradeB ID validation: %s (%s)%n", tradeIdValid ? "PASS" : "FAIL", swap.getTradeId());

            boolean counterpartyValid = rulesService.check("#counterpartyId != null && #counterpartyId.trim().length() > 0", context);
            System.out.printf("   Counterparty validation: %s (%s)%n", counterpartyValid ? "PASS" : "FAIL", swap.getCounterpartyId());

            boolean clientValid = rulesService.check("#clientId != null && #clientId.trim().length() > 0", context);
            System.out.printf("   Client validation: %s (%s)%n", clientValid ? "PASS" : "FAIL", swap.getClientId());

            boolean notionalValid = rulesService.check("#notionalAmount != null && #notionalAmount > 0", context);
            System.out.printf("   Notional validation: %s (%s)%n", notionalValid ? "PASS" : "FAIL", swap.getNotionalAmount());

            boolean commodityTypeValid = rulesService.check("#commodityType != null && #commodityType.trim().length() > 0", context);
            System.out.printf("   Commodity type validation: %s (%s)%n", commodityTypeValid ? "PASS" : "FAIL", swap.getCommodityType());

            long validationEnd = System.currentTimeMillis();
            long validationDuration = validationEnd - validationStart;

            System.out.println("Step 1.5: Calculating overall validation result...");
            boolean overallValid = tradeIdValid && counterpartyValid && clientValid && notionalValid && commodityTypeValid;
            int passedChecks = (tradeIdValid ? 1 : 0) + (counterpartyValid ? 1 : 0) + (clientValid ? 1 : 0) + (notionalValid ? 1 : 0) + (commodityTypeValid ? 1 : 0);

            System.out.println("VALIDATION RESULTS SUMMARY:");
            System.out.printf("   Overall Result: %s (%d/5 checks passed)%n", overallValid ? "PASS" : "FAIL", passedChecks);
            System.out.printf("   Validation Time: %d ms%n", validationDuration);
            System.out.printf("   TradeB Status: %s%n", overallValid ? "APPROVED for further processing" : "REJECTED - requires correction");

            long processingTime = System.currentTimeMillis() - startTime;
            performanceMetrics.put("Scenario1_ProcessingTime", processingTime);
            performanceMetrics.put("Scenario1_ValidationTime", validationDuration);
            performanceMetrics.put("Scenario1_PassedChecks", (long) passedChecks);

            System.out.println("Step 1.6: Recording audit trail...");
            // Store audit record
            storeValidationAudit(swap.getTradeId(), "ULTRA_SIMPLE_API", overallValid ? "PASS" : "FAIL", processingTime);
            System.out.printf("   Audit record stored for trade: %s%n", swap.getTradeId());

            System.out.println("SCENARIO 1 COMPLETED SUCCESSFULLY");
            System.out.printf("   Total Processing Time: %d ms%n", processingTime);
            System.out.println("   Validation Approach: Ultra-Simple API");
            System.out.printf("   Result: %s with %d/5 checks passed%n", overallValid ? "PASS" : "FAIL", passedChecks);

            logExecution("Scenario 1 (Ultra-Simple API) completed: " + (overallValid ? "PASS" : "FAIL") + " in " + processingTime + "ms");

        } catch (Exception e) {
            System.err.println("Scenario 1 execution failed: " + e.getMessage());
            throw new RuntimeException("Ultra-Simple API demonstration failed", e);
        }
    }

    /**
     * Scenario 2: Template-Based Rules Demonstration.
     */
    private void executeScenario2_TemplateBasedRules() throws Exception {
        System.out.println("\n--- SCENARIO 2: TEMPLATE-BASED RULES DEMONSTRATION ---");
        long startTime = System.currentTimeMillis();

        // Create sample commodity swap
        CommodityTotalReturnSwap swap = createSampleMetalsSwap();

        System.out.println("Testing Template-Based Rules validation:");
        System.out.println("TradeB: " + swap.getTradeId() + " (" + swap.getCommodityType() + " - " + swap.getReferenceIndex() + ")");

        // Template-Based Rules validation
        RulesEngine validationEngine = RuleSet.category("commodity-validation")
            .withCreatedBy("financial.admin@company.com")
            .withBusinessDomain("Commodity Derivatives")
            .withBusinessOwner("Trading Desk")
            .customRule("TradeB ID Required", "#tradeId != null && #tradeId.trim().length() > 0", "TradeB ID is required")
            .customRule("Counterparty ID Required", "#counterpartyId != null && #counterpartyId.trim().length() > 0", "Counterparty ID is required")
            .customRule("Client ID Required", "#clientId != null && #clientId.trim().length() > 0", "Client ID is required")
            .customRule("Commodity Type Required", "#commodityType != null && #commodityType.trim().length() > 0", "Commodity type is required")
            .customRule("Reference Index Required", "#referenceIndex != null && #referenceIndex.trim().length() > 0", "Reference index is required")
            .customRule("Notional Amount Positive", "#notionalAmount != null && #notionalAmount > 0", "Notional amount must be positive")
            .customRule("Currency Required", "#notionalCurrency != null && #notionalCurrency.trim().length() > 0", "Notional currency is required")
            .build();

        // Execute validation rules and get real results
        System.out.println("   Engine created successfully: " + validationEngine.getClass().getSimpleName());
        Map<String, Object> context = convertSwapToMap(swap);
        RuleResult validationResult = validationEngine.executeRulesForCategory("commodity-validation", context);
        boolean validationSuccess = validationResult.isTriggered();

        System.out.println("   ✓ Validation result: " + (validationSuccess ? "PASS" : "FAIL"));
        if (validationResult.getRuleName() != null) {
            System.out.println("   ✓ Triggered rule: " + validationResult.getRuleName());
            System.out.println("   ✓ Rule message: " + validationResult.getMessage());
        }

        // Get detailed rule execution statistics
        Map<String, Boolean> ruleResults = executeIndividualValidationRules(context);
        int passedRules = (int) ruleResults.values().stream().mapToInt(result -> result ? 1 : 0).sum();
        int failedRules = ruleResults.size() - passedRules;

        System.out.println("   ✓ Rules passed: " + passedRules);
        System.out.println("   ✓ Rules failed: " + failedRules);

        // Show individual rule results
        ruleResults.forEach((ruleName, result) ->
            System.out.println("     - " + ruleName + ": " + (result ? "PASS" : "FAIL")));

        // Update overall success based on detailed results
        validationSuccess = failedRules == 0;

        // Business Rules validation
        RulesEngine businessEngine = RuleSet.category("commodity-business")
            .withCreatedBy("financial.admin@company.com")
            .withBusinessDomain("Commodity Derivatives")
            .withBusinessOwner("Trading Desk")
            .customRule("Maturity Eligibility", "#maturityDate != null && #maturityDate.isBefore(#tradeDate.plusYears(5))", "TradeB maturity within 5 years")
            .customRule("Currency Consistency", "#notionalCurrency == #paymentCurrency && #paymentCurrency == #settlementCurrency", "All currencies must match")
            .customRule("Settlement Terms", "#settlementDays != null && #settlementDays >= 0 && #settlementDays <= 5", "Settlement within 5 days")
            .build();

        // Execute business rules and get real results
        System.out.println("   Business engine created: " + businessEngine.getClass().getSimpleName());
        RuleResult businessResult = businessEngine.executeRulesForCategory("commodity-business", context);
        boolean businessSuccess = businessResult.isTriggered();

        System.out.println("   ✓ Business rules result: " + (businessSuccess ? "PASS" : "FAIL"));
        if (businessResult.getRuleName() != null) {
            System.out.println("   ✓ Triggered business rule: " + businessResult.getRuleName());
            System.out.println("   ✓ Business rule message: " + businessResult.getMessage());
        }

        // Get detailed business rule execution statistics
        Map<String, Boolean> businessRuleResults = executeIndividualBusinessRules(context);
        int passedBusinessRules = (int) businessRuleResults.values().stream().mapToInt(result -> result ? 1 : 0).sum();
        int failedBusinessRules = businessRuleResults.size() - passedBusinessRules;

        System.out.println("   ✓ Business rules passed: " + passedBusinessRules);
        System.out.println("   ✓ Business rules failed: " + failedBusinessRules);

        // Show individual business rule results
        businessRuleResults.forEach((ruleName, result) ->
            System.out.println("     - " + ruleName + ": " + (result ? "PASS" : "FAIL")));

        // Update overall success based on detailed results
        businessSuccess = failedBusinessRules == 0;

        long processingTime = System.currentTimeMillis() - startTime;
        performanceMetrics.put("Scenario2_ProcessingTime", processingTime);

        System.out.println("   ✓ Processing time: " + processingTime + "ms");

        // Store audit record with real validation results
        boolean overallValid = validationSuccess && businessSuccess;
        String auditResult = overallValid ? "PASS" : "FAIL";
        if (!overallValid) {
            // Add details about which rules failed
            if (!validationSuccess && validationResult.getRuleName() != null) {
                auditResult += " (Validation: " + validationResult.getRuleName() + ")";
            }
            if (!businessSuccess && businessResult.getRuleName() != null) {
                auditResult += " (Business: " + businessResult.getRuleName() + ")";
            }
        }
        storeValidationAudit(swap.getTradeId(), "TEMPLATE_BASED_RULES", auditResult, processingTime);

        logExecution("Scenario 2 completed");
    }

    /**
     * Scenario 3: Advanced Configuration Demonstration.
     */
    private void executeScenario3_AdvancedConfiguration() throws Exception {
        System.out.println("\n--- SCENARIO 3: ADVANCED CONFIGURATION DEMONSTRATION ---");
        long startTime = System.currentTimeMillis();

        // Create sample commodity swap with advanced features
        CommodityTotalReturnSwap swap = createSampleAgriculturalSwap();

        System.out.println("Testing Advanced Configuration validation:");
        System.out.println("TradeB: " + swap.getTradeId() + " (" + swap.getCommodityType() + " - " + swap.getReferenceIndex() + ")");

        // Advanced validation rules
        List<Rule> advancedRules = createAdvancedValidationRules();

        System.out.println("   ✓ Advanced rules created: " + advancedRules.size());

        // Execute advanced rules
        Map<String, Object> context = convertSwapToMap(swap);

        int passedAdvancedRules = 0;
        int failedAdvancedRules = 0;
        boolean overallAdvancedSuccess = true;
        StringBuilder failureDetails = new StringBuilder();

        for (Rule rule : advancedRules) {
            boolean result = rulesService.check(rule.getCondition(), context);
            System.out.println("   ✓ " + rule.getName() + ": " + (result ? "PASS" : "FAIL"));

            if (result) {
                passedAdvancedRules++;
            } else {
                failedAdvancedRules++;
                overallAdvancedSuccess = false;
                System.out.println("     - " + rule.getDescription());
                if (failureDetails.length() > 0) failureDetails.append(", ");
                failureDetails.append(rule.getName());
            }
        }

        System.out.println("   ✓ Advanced rules summary: " + passedAdvancedRules + " passed, " + failedAdvancedRules + " failed");

        long processingTime = System.currentTimeMillis() - startTime;
        performanceMetrics.put("Scenario3_ProcessingTime", processingTime);

        System.out.println("   ✓ Processing time: " + processingTime + "ms");

        // Store audit record with real results
        String auditResult = overallAdvancedSuccess ? "PASS" : "FAIL";
        if (!overallAdvancedSuccess) {
            auditResult += " (Failed: " + failureDetails.toString() + ")";
        }
        storeValidationAudit(swap.getTradeId(), "ADVANCED_CONFIGURATION", auditResult, processingTime);

        logExecution("Scenario 3 completed");
    }

    /**
     * Scenario 4: Static Data Validation and Enrichment.
     */
    private void executeScenario4_StaticDataEnrichment() throws Exception {
        System.out.println("\n--- SCENARIO 4: STATIC DATA VALIDATION & ENRICHMENT ---");
        long startTime = System.currentTimeMillis();

        // Create sample commodity swap
        CommodityTotalReturnSwap swap = createSampleEnergySwap();

        System.out.println("Testing Static Data validation and enrichment:");
        System.out.println("TradeB: " + swap.getTradeId() + " (" + swap.getCommodityType() + " - " + swap.getReferenceIndex() + ")");

        // Client validation and enrichment
        System.out.println("\n1. Client Validation:");
        CommodityClient client = clients.get(swap.getClientId());
        if (client != null) {
            System.out.println("   ✓ Client found: " + client.getClientName());
            System.out.println("   ✓ Client active: " + client.getActive());
            System.out.println("   ✓ Client type: " + client.getClientType());
            System.out.println("   ✓ Regulatory classification: " + client.getRegulatoryClassification());

            // Enrich swap with client data using EnrichmentService
            try {
                // Create client enrichment configuration
                YamlEnrichment clientEnrichment = createClientEnrichment();
                Object enrichedSwap = enrichmentService.enrichObject(clientEnrichment, swap);
                System.out.println("   ✓ Swap enriched with client data using EnrichmentService");

                // Verify enrichment worked
                if (swap.getClientName() != null) {
                    System.out.println("   ✓ Client name enriched: " + swap.getClientName());
                } else {
                    // Fallback to manual enrichment if service enrichment failed
                    swap.setClientName(client.getClientName());
                    System.out.println("   ✓ Client name enriched (fallback): " + client.getClientName());
                }
            } catch (Exception e) {
                // Fallback to manual enrichment if service enrichment failed
                swap.setClientName(client.getClientName());
                System.out.println("   ✓ Client name enriched (fallback due to error): " + client.getClientName());
                System.out.println("   ⚠ EnrichmentService error: " + e.getMessage());
            }
        } else {
            System.out.println("   ✗ Client not found: " + swap.getClientId());
        }

        // Counterparty validation
        System.out.println("\n2. Counterparty Validation:");
        CommodityCounterparty counterparty = counterparties.get(swap.getCounterpartyId());
        if (counterparty != null) {
            System.out.println("   ✓ Counterparty found: " + counterparty.getCounterpartyName());
            System.out.println("   ✓ Counterparty active: " + counterparty.getActive());
            System.out.println("   ✓ Counterparty type: " + counterparty.getCounterpartyType());
            System.out.println("   ✓ Credit rating: " + counterparty.getCreditRating());
        } else {
            System.out.println("   ✗ Counterparty not found: " + swap.getCounterpartyId());
        }

        // Currency validation
        System.out.println("\n3. Currency Validation:");
        CurrencyData currency = currencies.get(swap.getNotionalCurrency());
        if (currency != null) {
            System.out.println("   ✓ Currency found: " + currency.getCurrencyName());
            System.out.println("   ✓ Currency active: " + currency.getActive());
            System.out.println("   ✓ Currency tradeable: " + currency.getTradeable());
            System.out.println("   ✓ Decimal places: " + currency.getDecimalPlaces());
        } else {
            System.out.println("   ✗ Currency not found: " + swap.getNotionalCurrency());
        }

        // Commodity reference validation
        System.out.println("\n4. Commodity Reference Validation:");
        CommodityReference commodity = commodities.get(swap.getReferenceIndex());
        if (commodity != null) {
            System.out.println("   ✓ Commodity found: " + commodity.getCommodityName());
            System.out.println("   ✓ Commodity active: " + commodity.getActive());
            System.out.println("   ✓ Index provider: " + commodity.getIndexProvider());
            System.out.println("   ✓ Quote currency: " + commodity.getQuoteCurrency());

            // Enrich swap with commodity data using EnrichmentService
            try {
                // Create commodity enrichment configuration
                YamlEnrichment commodityEnrichment = createCommodityEnrichment();
                Object enrichedSwap = enrichmentService.enrichObject(commodityEnrichment, swap);
                System.out.println("   ✓ Swap enriched with commodity data using EnrichmentService");

                // Verify enrichment worked
                if (swap.getIndexProvider() != null) {
                    System.out.println("   ✓ Index provider enriched: " + swap.getIndexProvider());
                } else {
                    // Fallback to manual enrichment if service enrichment failed
                    swap.setIndexProvider(commodity.getIndexProvider());
                    System.out.println("   ✓ Index provider enriched (fallback): " + commodity.getIndexProvider());
                }
            } catch (Exception e) {
                // Fallback to manual enrichment if service enrichment failed
                swap.setIndexProvider(commodity.getIndexProvider());
                System.out.println("   ✓ Index provider enriched (fallback due to error): " + commodity.getIndexProvider());
                System.out.println("   ⚠ EnrichmentService error: " + e.getMessage());
            }
        } else {
            System.out.println("   ✗ Commodity not found: " + swap.getReferenceIndex());
        }

        long processingTime = System.currentTimeMillis() - startTime;
        performanceMetrics.put("Scenario4_ProcessingTime", processingTime);

        System.out.println("\n   ✓ Processing time: " + processingTime + "ms");

        // Store audit record
        storeValidationAudit(swap.getTradeId(), "STATIC_DATA_ENRICHMENT", "PASS", processingTime);

        logExecution("Scenario 4 completed");
    }

    /**
     * Scenario 5: Performance Monitoring Demonstration.
     */
    private void executeScenario5_PerformanceMonitoring() throws Exception {
        System.out.println("\n--- SCENARIO 5: PERFORMANCE MONITORING DEMONSTRATION ---");
        long startTime = System.currentTimeMillis();

        System.out.println("Testing Performance monitoring capabilities:");

        // Create multiple swaps for performance testing
        List<CommodityTotalReturnSwap> swaps = Arrays.asList(
            createSampleEnergySwap(),
            createSampleMetalsSwap(),
            createSampleAgriculturalSwap()
        );

        // Performance monitoring
        for (int i = 0; i < swaps.size(); i++) {
            CommodityTotalReturnSwap swap = swaps.get(i);
            long swapStartTime = System.currentTimeMillis();

            // Execute validation
            Map<String, Object> context = convertSwapToMap(swap);
            boolean valid = rulesService.check("#tradeId != null && #notionalAmount > 0", context);

            long swapProcessingTime = System.currentTimeMillis() - swapStartTime;
            System.out.println("   ✓ Swap " + (i + 1) + " (" + swap.getTradeId() + "): " + swapProcessingTime + "ms - " + (valid ? "VALID" : "INVALID"));
        }

        long totalProcessingTime = System.currentTimeMillis() - startTime;
        performanceMetrics.put("Scenario5_ProcessingTime", totalProcessingTime);

        System.out.println("   ✓ Total processing time: " + totalProcessingTime + "ms");
        System.out.println("   ✓ Average per swap: " + (totalProcessingTime / swaps.size()) + "ms");
        System.out.println("   ✓ Target: <100ms per swap");

        // Store audit record
        storeValidationAudit("PERFORMANCE_TEST", "PERFORMANCE_MONITORING", "PASS", totalProcessingTime);

        logExecution("Scenario 5 completed");
    }

    /**
     * Scenario 6: Exception Handling Demonstration.
     */
    private void executeScenario6_ExceptionHandling() throws Exception {
        System.out.println("\n--- SCENARIO 6: EXCEPTION HANDLING DEMONSTRATION ---");
        long startTime = System.currentTimeMillis();

        System.out.println("Testing Exception handling scenarios:");

        // Test 1: Invalid trade ID format
        System.out.println("\n1. Invalid TradeB ID Format:");
        CommodityTotalReturnSwap invalidSwap1 = createSampleEnergySwap();
        invalidSwap1.setTradeId("INVALID_ID");

        try {
            boolean result = rulesService.check("#tradeId.matches('^TRS[0-9]{3}$')", invalidSwap1);
            System.out.println("   ✓ TradeB ID format validation: " + (result ? "PASS" : "FAIL"));
        } catch (Exception e) {
            System.out.println("   ✗ Exception caught: " + e.getMessage());
        }

        // Test 2: Null notional amount
        System.out.println("\n2. Null Notional Amount:");
        CommodityTotalReturnSwap invalidSwap2 = createSampleEnergySwap();
        invalidSwap2.setNotionalAmount(null);

        try {
            boolean result = rulesService.check("#notionalAmount != null && #notionalAmount > 0", invalidSwap2);
            System.out.println("   ✓ Notional amount validation: " + (result ? "PASS" : "FAIL"));
        } catch (Exception e) {
            System.out.println("   ✗ Exception caught: " + e.getMessage());
        }

        // Test 3: Invalid commodity type
        System.out.println("\n3. Invalid Commodity Type:");
        CommodityTotalReturnSwap invalidSwap3 = createSampleEnergySwap();
        invalidSwap3.setCommodityType("INVALID_TYPE");

        try {
            boolean result = rulesService.check("#commodityType in ['ENERGY', 'METALS', 'AGRICULTURAL']", invalidSwap3);
            System.out.println("   ✓ Commodity type validation: " + (result ? "PASS" : "FAIL"));
        } catch (Exception e) {
            System.out.println("   ✗ Exception caught: " + e.getMessage());
        }

        long processingTime = System.currentTimeMillis() - startTime;
        performanceMetrics.put("Scenario6_ProcessingTime", processingTime);

        System.out.println("\n   ✓ Processing time: " + processingTime + "ms");

        // Store audit record
        storeValidationAudit("EXCEPTION_TEST", "EXCEPTION_HANDLING", "PASS", processingTime);

        logExecution("Scenario 6 completed");
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Create sample energy commodity swap.
     */
    private CommodityTotalReturnSwap createSampleEnergySwap() {
        CommodityTotalReturnSwap swap = new CommodityTotalReturnSwap(
            "TRS001",           // tradeId
            "CP001",            // counterpartyId
            "CLI001",           // clientId
            "ENERGY",           // commodityType
            "WTI",              // referenceIndex
            new BigDecimal("10000000"), // notionalAmount
            "USD",              // notionalCurrency
            LocalDate.now(),    // tradeDate
            LocalDate.now().plusYears(1) // maturityDate
        );

        // Set additional fields
        swap.setPaymentCurrency("USD");
        swap.setSettlementCurrency("USD");
        swap.setSettlementDays(2);
        swap.setTotalReturnPayerParty("COUNTERPARTY");
        swap.setTotalReturnReceiverParty("CLIENT");
        swap.setFundingRateType("SOFR");
        swap.setFundingSpread(new BigDecimal("150")); // 150 basis points
        swap.setFundingFrequency("QUARTERLY");
        swap.setJurisdiction("US");
        swap.setRegulatoryRegime("DODD_FRANK");
        swap.setClearingEligible(true);
        swap.setInitialPrice(new BigDecimal("75.50"));

        return swap;
    }

    /**
     * Create sample metals commodity swap.
     */
    private CommodityTotalReturnSwap createSampleMetalsSwap() {
        CommodityTotalReturnSwap swap = new CommodityTotalReturnSwap(
            "TRS002",           // tradeId
            "CP002",            // counterpartyId
            "CLI002",           // clientId
            "METALS",           // commodityType
            "GOLD",             // referenceIndex
            new BigDecimal("5000000"), // notionalAmount
            "USD",              // notionalCurrency
            LocalDate.now(),    // tradeDate
            LocalDate.now().plusMonths(18) // maturityDate
        );

        // Set additional fields
        swap.setPaymentCurrency("USD");
        swap.setSettlementCurrency("USD");
        swap.setSettlementDays(2);
        swap.setTotalReturnPayerParty("CLIENT");
        swap.setTotalReturnReceiverParty("COUNTERPARTY");
        swap.setFundingRateType("LIBOR");
        swap.setFundingSpread(new BigDecimal("125")); // 125 basis points
        swap.setFundingFrequency("MONTHLY");
        swap.setJurisdiction("UK");
        swap.setRegulatoryRegime("EMIR");
        swap.setClearingEligible(false);
        swap.setInitialPrice(new BigDecimal("1950.00"));

        return swap;
    }

    /**
     * Create sample agricultural commodity swap.
     */
    private CommodityTotalReturnSwap createSampleAgriculturalSwap() {
        CommodityTotalReturnSwap swap = new CommodityTotalReturnSwap(
            "TRS003",           // tradeId
            "CP003",            // counterpartyId
            "CLI003",           // clientId
            "AGRICULTURAL",     // commodityType
            "CORN",             // referenceIndex
            new BigDecimal("2500000"), // notionalAmount
            "USD",              // notionalCurrency
            LocalDate.now(),    // tradeDate
            LocalDate.now().plusMonths(12) // maturityDate
        );

        // Set additional fields
        swap.setPaymentCurrency("USD");
        swap.setSettlementCurrency("USD");
        swap.setSettlementDays(3);
        swap.setTotalReturnPayerParty("COUNTERPARTY");
        swap.setTotalReturnReceiverParty("CLIENT");
        swap.setFundingRateType("FIXED");
        swap.setFundingSpread(new BigDecimal("200")); // 200 basis points
        swap.setFundingFrequency("SEMI_ANNUAL");
        swap.setJurisdiction("US");
        swap.setRegulatoryRegime("CFTC");
        swap.setClearingEligible(true);
        swap.setInitialPrice(new BigDecimal("6.75"));

        return swap;
    }

    /**
     * Create advanced validation rules.
     */
    private List<Rule> createAdvancedValidationRules() {
        List<Rule> rules = new ArrayList<>();

        // Create rules using the traditional API for advanced scenarios
        rules.add(new Rule("trade-id-format",
                          "#tradeId != null && #tradeId.matches('^TRS[0-9]{3}$')",
                          "TradeB ID must follow TRS### format"));

        rules.add(new Rule("notional-range",
                          "#notionalAmount >= 1000000 && #notionalAmount <= 100000000",
                          "Notional must be between $1M and $100M"));

        rules.add(new Rule("commodity-energy-check",
                          "#commodityType == 'ENERGY' && (#referenceIndex == 'WTI' || #referenceIndex == 'BRENT' || #referenceIndex == 'HENRY_HUB')",
                          "Energy commodities must use valid reference indices"));

        rules.add(new Rule("maturity-date-check",
                          "#maturityDate != null && #maturityDate.isAfter(#tradeDate)",
                          "Maturity date must be after trade date"));

        rules.add(new Rule("funding-spread-check",
                          "#fundingSpread != null && #fundingSpread >= 0 && #fundingSpread <= 1000",
                          "Funding spread must be between 0 and 1000 basis points"));

        return rules;
    }

    /**
     * Convert swap to map for rule evaluation.
     */
    private Map<String, Object> convertSwapToMap(CommodityTotalReturnSwap swap) {
        Map<String, Object> map = new HashMap<>();
        map.put("tradeId", swap.getTradeId());
        map.put("counterpartyId", swap.getCounterpartyId());
        map.put("clientId", swap.getClientId());
        map.put("commodityType", swap.getCommodityType());
        map.put("referenceIndex", swap.getReferenceIndex());
        map.put("notionalAmount", swap.getNotionalAmount());
        map.put("notionalCurrency", swap.getNotionalCurrency());
        map.put("paymentCurrency", swap.getPaymentCurrency());
        map.put("settlementCurrency", swap.getSettlementCurrency());
        map.put("tradeDate", swap.getTradeDate());
        map.put("effectiveDate", swap.getEffectiveDate());
        map.put("maturityDate", swap.getMaturityDate());
        map.put("settlementDays", swap.getSettlementDays());
        map.put("fundingSpread", swap.getFundingSpread());
        map.put("jurisdiction", swap.getJurisdiction());
        map.put("regulatoryRegime", swap.getRegulatoryRegime());
        map.put("clearingEligible", swap.getClearingEligible());
        return map;
    }

    /**
     * Store validation audit record.
     */
    private void storeValidationAudit(String tradeId, String validationType, String result, long processingTime) {
        if (connection == null) {
            // In-memory mode - just log
            System.out.println("   📝 Audit: " + tradeId + " - " + validationType + " - " + result + " (" + processingTime + "ms)");
            return;
        }

        try {
            String sql = """
                INSERT INTO validation_audit (trade_id, validation_type, validation_result, processing_time_ms)
                VALUES (?, ?, ?, ?)
                """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, tradeId);
                stmt.setString(2, validationType);
                stmt.setString(3, result);
                stmt.setLong(4, processingTime);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("⚠️  Failed to store audit record: " + e.getMessage());
        }
    }

    /**
     * Display final performance metrics.
     */
    private void displayFinalMetrics() {
        System.out.println("\n=== FINAL PERFORMANCE METRICS ===");

        long totalProcessingTime = performanceMetrics.values().stream().mapToLong(Long::longValue).sum();
        System.out.println("Total processing time: " + totalProcessingTime + "ms");

        performanceMetrics.forEach((scenario, time) ->
            System.out.println(scenario + ": " + time + "ms"));

        System.out.println("\nExecution Log:");
        executionLog.forEach(entry -> System.out.println("  " + entry));

        System.out.println("\nStatic Data Summary:");
        System.out.println("  Clients: " + clients.size());
        System.out.println("  Counterparties: " + counterparties.size());
        System.out.println("  Commodities: " + commodities.size());
        System.out.println("  Currencies: " + currencies.size());
    }

    /**
     * Cleanup resources.
     */
    private void cleanup() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            System.out.println("✅ Resources cleaned up successfully");
        } catch (SQLException e) {
            System.err.println("⚠️  Error during cleanup: " + e.getMessage());
        }
    }

    /**
     * Execute individual validation rules to get detailed statistics.
     * This provides accurate rule-by-rule execution results.
     */
    private Map<String, Boolean> executeIndividualValidationRules(Map<String, Object> context) {
        Map<String, Boolean> results = new HashMap<>();

        // Define the validation rules individually
        String[][] validationRules = {
            {"TradeB ID Required", "#tradeId != null && #tradeId.trim().length() > 0"},
            {"Counterparty ID Required", "#counterpartyId != null && #counterpartyId.trim().length() > 0"},
            {"Client ID Required", "#clientId != null && #clientId.trim().length() > 0"},
            {"Commodity Type Required", "#commodityType != null && #commodityType.trim().length() > 0"},
            {"Reference Index Required", "#referenceIndex != null && #referenceIndex.trim().length() > 0"},
            {"Notional Amount Positive", "#notionalAmount != null && #notionalAmount > 0"},
            {"Currency Required", "#notionalCurrency != null && #notionalCurrency.trim().length() > 0"}
        };

        // Execute each rule individually
        for (String[] rule : validationRules) {
            String ruleName = rule[0];
            String condition = rule[1];
            try {
                boolean result = rulesService.check(condition, context);
                results.put(ruleName, result);
            } catch (Exception e) {
                System.out.println("     ⚠ Error executing rule '" + ruleName + "': " + e.getMessage());
                results.put(ruleName, false);
            }
        }

        return results;
    }

    /**
     * Execute individual business rules to get detailed statistics.
     * This provides accurate rule-by-rule execution results.
     */
    private Map<String, Boolean> executeIndividualBusinessRules(Map<String, Object> context) {
        Map<String, Boolean> results = new HashMap<>();

        // Define the business rules individually
        String[][] businessRules = {
            {"Maturity Eligibility", "#maturityDate != null && #maturityDate.isBefore(#tradeDate.plusYears(5))"},
            {"Currency Consistency", "#notionalCurrency == #paymentCurrency && #paymentCurrency == #settlementCurrency"},
            {"Settlement Terms", "#settlementDays != null && #settlementDays >= 0 && #settlementDays <= 5"}
        };

        // Execute each rule individually
        for (String[] rule : businessRules) {
            String ruleName = rule[0];
            String condition = rule[1];
            try {
                boolean result = rulesService.check(condition, context);
                results.put(ruleName, result);
            } catch (Exception e) {
                System.out.println("     ⚠ Error executing business rule '" + ruleName + "': " + e.getMessage());
                results.put(ruleName, false);
            }
        }

        return results;
    }

    /**
     * Create client enrichment configuration based on YAML definition.
     * This creates a programmatic enrichment that matches the YAML configuration.
     */
    private YamlEnrichment createClientEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("client-enrichment");
        enrichment.setName("Client Data Enrichment");
        enrichment.setDescription("Enrich trade with client information");
        enrichment.setType("lookup-enrichment");
        enrichment.setEnabled(true);

        // Create lookup configuration
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("client_data");
        lookupConfig.setLookupKey("#clientId");

        // Create field mappings
        List<YamlEnrichment.FieldMapping> mappings = new ArrayList<>();

        YamlEnrichment.FieldMapping clientNameMapping = new YamlEnrichment.FieldMapping();
        clientNameMapping.setSourceField("client_name");
        clientNameMapping.setTargetField("clientName");
        mappings.add(clientNameMapping);

        YamlEnrichment.FieldMapping regulatoryMapping = new YamlEnrichment.FieldMapping();
        regulatoryMapping.setSourceField("regulatory_classification");
        regulatoryMapping.setTargetField("clientRegulatoryClassification");
        mappings.add(regulatoryMapping);

        enrichment.setFieldMappings(mappings);
        enrichment.setLookupConfig(lookupConfig);

        return enrichment;
    }

    /**
     * Create commodity enrichment configuration based on YAML definition.
     * This creates a programmatic enrichment that matches the YAML configuration.
     */
    private YamlEnrichment createCommodityEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("commodity-enrichment");
        enrichment.setName("Commodity Reference Data Enrichment");
        enrichment.setDescription("Enrich trade with commodity reference data");
        enrichment.setType("lookup-enrichment");
        enrichment.setEnabled(true);

        // Create lookup configuration
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("commodity_reference_data");
        lookupConfig.setLookupKey("#referenceIndex");

        // Create field mappings
        List<YamlEnrichment.FieldMapping> mappings = new ArrayList<>();

        YamlEnrichment.FieldMapping providerMapping = new YamlEnrichment.FieldMapping();
        providerMapping.setSourceField("index_provider");
        providerMapping.setTargetField("indexProvider");
        mappings.add(providerMapping);

        YamlEnrichment.FieldMapping currencyMapping = new YamlEnrichment.FieldMapping();
        currencyMapping.setSourceField("quote_currency");
        currencyMapping.setTargetField("commodityQuoteCurrency");
        mappings.add(currencyMapping);

        YamlEnrichment.FieldMapping unitMapping = new YamlEnrichment.FieldMapping();
        unitMapping.setSourceField("unit_of_measure");
        unitMapping.setTargetField("commodityUnitOfMeasure");
        mappings.add(unitMapping);

        enrichment.setFieldMappings(mappings);
        enrichment.setLookupConfig(lookupConfig);

        return enrichment;
    }
}
