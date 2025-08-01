package dev.mars.apex.demo.bootstrap;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.core.api.RuleSet;
import dev.mars.apex.core.api.SimpleRulesEngine;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.core.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.yaml.YamlRuleConfiguration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.yaml.snakeyaml.Yaml;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * Complete Bootstrap Demonstration of APEX Commodity Swap Validation & Enrichment.
 * 
 * This single-file bootstrap creates a complete end-to-end demonstration including:
 * - PostgreSQL database setup with commodity derivatives data
 * - YAML rule configuration with layered validation approaches
 * - Comprehensive static data enrichment (clients, counterparties, commodities)
 * - Multiple realistic commodity swap scenarios
 * - Complete audit trails and performance metrics
 * 
 * The bootstrap demonstrates APEX's layered API approach:
 * - Ultra-Simple API for basic validation
 * - Template-Based Rules for business logic
 * - Advanced Configuration for complex scenarios
 * - Static Data Integration for enrichment
 * 
 * The bootstrap is designed to be re-runnable and self-contained, demonstrating
 * the full power of APEX in solving commodity derivatives validation challenges.
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
    private EnrichmentService enrichmentService;
    private YamlRuleConfiguration yamlConfig;
    
    // Database connection
    private Connection connection;
    private HikariDataSource dataSource;
    
    // Performance metrics
    private Map<String, Long> performanceMetrics;
    private List<String> executionLog;
    
    // Static data repositories
    private Map<String, CommodityClient> clients;
    private Map<String, CommodityCounterparty> counterparties;
    private Map<String, CommodityReference> commodities;
    private Map<String, CurrencyData> currencies;
    
    public static void main(String[] args) {
        System.out.println("=== APEX COMMODITY SWAP VALIDATION BOOTSTRAP ===");
        System.out.println("Complete end-to-end commodity derivatives validation demonstration");
        System.out.println("Demonstrating layered APIs, static data enrichment, and performance monitoring\n");
        
        CommoditySwapValidationBootstrap bootstrap = new CommoditySwapValidationBootstrap();
        
        try {
            // Initialize the bootstrap
            bootstrap.initialize();
            
            // Execute all demonstration scenarios
            bootstrap.executeAllScenarios();
            
            // Display final performance metrics
            bootstrap.displayFinalMetrics();
            
            System.out.println("\n=== COMMODITY SWAP VALIDATION BOOTSTRAP COMPLETED ===");
            
        } catch (Exception e) {
            System.err.println("❌ Bootstrap execution failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup resources
            bootstrap.cleanup();
        }
    }
    
    /**
     * Initialize the bootstrap with all required components.
     */
    private void initialize() throws Exception {
        System.out.println("Initializing APEX Commodity Swap Validation Bootstrap...");
        
        // Initialize collections
        this.performanceMetrics = new HashMap<>();
        this.executionLog = new ArrayList<>();
        this.clients = new HashMap<>();
        this.counterparties = new HashMap<>();
        this.commodities = new HashMap<>();
        this.currencies = new HashMap<>();
        
        // Setup infrastructure
        setupDatabaseInfrastructure();
        initializeStaticData();
        loadYamlConfiguration();
        initializeApexComponents();
        
        System.out.println("✅ Bootstrap initialization completed successfully");
        logExecution("Bootstrap initialized");
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
        
        // Setup connection pool
        setupConnectionPool();
        
        // Create schema
        createDatabaseSchema();
        
        logExecution("Database infrastructure setup completed");
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
     * Setup HikariCP connection pool.
     */
    private void setupConnectionPool() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL + DB_NAME);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        this.dataSource = new HikariDataSource(config);
        this.connection = dataSource.getConnection();
        
        System.out.println("✅ Database connection pool established");
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
     * Cleanup resources.
     */
    private void cleanup() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
            System.out.println("✅ Resources cleaned up successfully");
        } catch (SQLException e) {
            System.err.println("⚠️  Error during cleanup: " + e.getMessage());
        }
    }
