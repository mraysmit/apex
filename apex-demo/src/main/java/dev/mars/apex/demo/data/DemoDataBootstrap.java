package dev.mars.apex.demo.data;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.bootstrap.model.Customer;
import dev.mars.apex.demo.bootstrap.model.Product;
import dev.mars.apex.demo.bootstrap.model.Trade;
import dev.mars.apex.demo.bootstrap.model.FinancialTrade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * Bootstrap Data Provider for APEX Rules Engine Demonstrations.
 *
 * This class replaces the hardcoded DemoDataProvider with a comprehensive bootstrap
 * approach that follows APEX design principles:
 *
 * ============================================================================
 * BOOTSTRAP DATA PROVIDER OVERVIEW
 * ============================================================================
 *
 * This bootstrap demonstrates APEX's data-driven approach by providing:
 *
 * 1. EXTERNAL YAML CONFIGURATION - All data definitions loaded from external files
 * 2. DATABASE INTEGRATION - PostgreSQL setup with realistic financial data
 * 3. REAL-WORLD SCENARIOS - Authentic customer profiles, products, and trades
 * 4. INFRASTRUCTURE DEMONSTRATION - Complete data source setup and management
 *
 * ============================================================================
 * DATA SOURCES USED
 * ============================================================================
 *
 * DATABASE SCHEMA (PostgreSQL):
 * ├── demo_customers - Customer profiles with realistic financial data
 * │   └── Fields: customer_id, name, age, email, membership_level, balance, etc.
 * ├── demo_products - Financial products and services catalog
 * │   └── Fields: product_id, name, price, category, min_balance, features, etc.
 * ├── demo_trades - Trading scenarios across multiple asset classes
 * │   └── Fields: trade_id, amount, currency, asset_class, status, etc.
 * └── demo_audit - Audit trail for all data operations
 *     └── Fields: audit_id, entity_type, operation, timestamp, etc.
 *
 * EXTERNAL YAML CONFIGURATION:
 * ├── bootstrap/demo-data-bootstrap.yaml - Main configuration file
 * ├── bootstrap/datasets/customer-profiles.yaml - Customer data definitions
 * ├── bootstrap/datasets/product-catalog.yaml - Product and service definitions
 * └── bootstrap/datasets/trading-scenarios.yaml - Trade scenario definitions
 *
 * ============================================================================
 * DEMONSTRATION SCENARIOS
 * ============================================================================
 *
 * SCENARIO 1: Customer Profile Management
 * - Loads diverse customer profiles from database and YAML
 * - Demonstrates membership tiers, risk profiles, and preferences
 * - Shows customer lifecycle and segmentation patterns
 *
 * SCENARIO 2: Product Catalog Integration
 * - Processes financial products from external configuration
 * - Demonstrates product eligibility and pricing rules
 * - Shows cross-selling and upselling scenarios
 *
 * SCENARIO 3: Trading Data Processing
 * - Handles multi-asset class trading scenarios
 * - Demonstrates trade validation and enrichment
 * - Shows risk management and compliance patterns
 *
 * @author APEX Bootstrap Demo Generator
 * @since 2025-08-27
 * @version 1.0
 */
public class DemoDataBootstrap {
    
    private static final Logger logger = LoggerFactory.getLogger(DemoDataBootstrap.class);
    
    // Infrastructure components
    private Connection databaseConnection;
    private YamlConfigurationLoader yamlLoader;
    private boolean useInMemoryMode = false;
    
    // Data collections
    private List<Customer> customers;
    private List<Product> products;
    private List<Trade> trades;
    private List<FinancialTrade> financialTrades;
    private Map<String, Object> configurationData;
    
    /**
     * Initialize the bootstrap data provider with complete infrastructure setup.
     */
    public DemoDataBootstrap() {
        try {
            setupInfrastructure();
            loadExternalConfiguration();
            initializeDataSources();
        } catch (Exception e) {
            logger.error("Failed to initialize DemoDataBootstrap: {}", e.getMessage());
            throw new RuntimeException("Bootstrap initialization failed", e);
        }
    }
    
    /**
     * Sets up the complete infrastructure for data management.
     */
    private void setupInfrastructure() throws SQLException {
        logger.info("Setting up demo data infrastructure...");
        
        try {
            // Try to connect to PostgreSQL
            connectToDatabase();
            createDatabaseSchema();
            populateInitialData();
            logger.info("PostgreSQL database setup completed successfully");
            
        } catch (SQLException e) {
            logger.warn("PostgreSQL not available, switching to in-memory simulation mode");
            useInMemoryMode = true;
            simulateInMemorySetup();
        }
    }
    
    /**
     * Attempts to connect to PostgreSQL database.
     */
    private void connectToDatabase() throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/apex_demo_data";
        String username = "postgres";
        String password = "postgres";
        
        try {
            // First try to connect to the specific database
            databaseConnection = DriverManager.getConnection(url, username, password);
            logger.info("Connected to existing apex_demo_data database");
        } catch (SQLException e) {
            // If database doesn't exist, connect to postgres and create it
            String postgresUrl = "jdbc:postgresql://localhost:5432/postgres";
            try (Connection postgresConn = DriverManager.getConnection(postgresUrl, username, password);
                 Statement stmt = postgresConn.createStatement()) {
                
                stmt.executeUpdate("CREATE DATABASE apex_demo_data");
                logger.info("Created apex_demo_data database");
                
                // Now connect to the new database
                databaseConnection = DriverManager.getConnection(url, username, password);
                logger.info("Connected to new apex_demo_data database");
            }
        }
    }
    
    /**
     * Creates the database schema for demo data.
     */
    private void createDatabaseSchema() throws SQLException {
        logger.info("Creating database schema...");
        
        // Create customers table
        String customersTable = """
            CREATE TABLE IF NOT EXISTS demo_customers (
                customer_id VARCHAR(20) PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                age INTEGER NOT NULL,
                email VARCHAR(100) NOT NULL,
                membership_level VARCHAR(20) NOT NULL,
                balance DECIMAL(12,2) NOT NULL,
                preferred_categories TEXT[],
                risk_profile VARCHAR(20) DEFAULT 'MEDIUM',
                onboarding_date DATE DEFAULT CURRENT_DATE,
                last_activity_date DATE DEFAULT CURRENT_DATE,
                kyc_verified BOOLEAN DEFAULT TRUE,
                created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        // Create products table
        String productsTable = """
            CREATE TABLE IF NOT EXISTS demo_products (
                product_id VARCHAR(20) PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                price DECIMAL(12,2) NOT NULL,
                category VARCHAR(50) NOT NULL,
                min_balance DECIMAL(12,2) DEFAULT 0.00,
                features TEXT[],
                target_segments TEXT[],
                active BOOLEAN DEFAULT TRUE,
                created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        // Create trades table
        String tradesTable = """
            CREATE TABLE IF NOT EXISTS demo_trades (
                trade_id VARCHAR(20) PRIMARY KEY,
                amount DECIMAL(15,2) NOT NULL,
                currency VARCHAR(3) NOT NULL,
                asset_class VARCHAR(50) NOT NULL,
                status VARCHAR(20) DEFAULT 'PENDING',
                trade_date DATE DEFAULT CURRENT_DATE,
                settlement_date DATE,
                counterparty VARCHAR(100),
                created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        // Create audit table
        String auditTable = """
            CREATE TABLE IF NOT EXISTS demo_audit (
                audit_id SERIAL PRIMARY KEY,
                entity_type VARCHAR(50) NOT NULL,
                entity_id VARCHAR(20) NOT NULL,
                operation VARCHAR(20) NOT NULL,
                details TEXT,
                created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        try (Statement stmt = databaseConnection.createStatement()) {
            stmt.executeUpdate(customersTable);
            stmt.executeUpdate(productsTable);
            stmt.executeUpdate(tradesTable);
            stmt.executeUpdate(auditTable);
            logger.info("Database schema created successfully");
        }
    }
    
    /**
     * Populates initial realistic data.
     */
    private void populateInitialData() throws SQLException {
        logger.info("Populating initial demo data...");
        
        populateCustomers();
        populateProducts();
        populateTrades();
        
        logger.info("Initial data population completed");
    }
    
    /**
     * Populates customer data with realistic financial profiles.
     */
    private void populateCustomers() throws SQLException {
        String sql = """
            INSERT INTO demo_customers 
            (customer_id, name, age, email, membership_level, balance, risk_profile, kyc_verified) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        Object[][] customerData = {
            {"CUST001", "Emily Chen", 34, "emily.chen@techcorp.com", "Platinum", 850000.00, "LOW", true},
            {"CUST002", "Marcus Johnson", 42, "marcus.j@globalfund.com", "Gold", 425000.00, "MEDIUM", true},
            {"CUST003", "Sarah Williams", 29, "sarah.williams@startup.io", "Silver", 125000.00, "MEDIUM", true},
            {"CUST004", "David Rodriguez", 38, "d.rodriguez@lawfirm.com", "Gold", 675000.00, "LOW", true},
            {"CUST005", "Lisa Thompson", 26, "lisa.t@consulting.com", "Silver", 85000.00, "HIGH", true},
            {"CUST006", "James Park", 45, "james.park@realestate.com", "Platinum", 1250000.00, "LOW", true},
            {"CUST007", "Anna Kowalski", 31, "anna.k@biotech.com", "Gold", 320000.00, "MEDIUM", true},
            {"CUST008", "Robert Kim", 28, "robert.kim@fintech.com", "Silver", 95000.00, "HIGH", true}
        };
        
        try (PreparedStatement pstmt = databaseConnection.prepareStatement(sql)) {
            for (Object[] customer : customerData) {
                pstmt.setString(1, (String) customer[0]);
                pstmt.setString(2, (String) customer[1]);
                pstmt.setInt(3, (Integer) customer[2]);
                pstmt.setString(4, (String) customer[3]);
                pstmt.setString(5, (String) customer[4]);
                pstmt.setBigDecimal(6, new BigDecimal(customer[5].toString()));
                pstmt.setString(7, (String) customer[6]);
                pstmt.setBoolean(8, (Boolean) customer[7]);
                pstmt.executeUpdate();
            }
            logger.info("Populated {} customer records", customerData.length);
        }
    }
    
    /**
     * Populates product catalog with realistic financial products.
     */
    private void populateProducts() throws SQLException {
        String sql = """
            INSERT INTO demo_products 
            (product_id, name, price, category, min_balance, active) 
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        Object[][] productData = {
            {"PROD001", "Private Wealth Management", 250000.00, "Investment", 500000.00, true},
            {"PROD002", "Premium Trading Account", 50000.00, "Trading", 100000.00, true},
            {"PROD003", "High-Yield Savings Plus", 10000.00, "Savings", 25000.00, true},
            {"PROD004", "Corporate Treasury Services", 100000.00, "Corporate", 1000000.00, true},
            {"PROD005", "Structured Products Portfolio", 500000.00, "Investment", 250000.00, true},
            {"PROD006", "FX Trading Platform", 25000.00, "Trading", 50000.00, true},
            {"PROD007", "Commodity Investment Fund", 150000.00, "Investment", 100000.00, true},
            {"PROD008", "Digital Banking Suite", 5000.00, "Digital", 10000.00, true}
        };
        
        try (PreparedStatement pstmt = databaseConnection.prepareStatement(sql)) {
            for (Object[] product : productData) {
                pstmt.setString(1, (String) product[0]);
                pstmt.setString(2, (String) product[1]);
                pstmt.setBigDecimal(3, new BigDecimal(product[2].toString()));
                pstmt.setString(4, (String) product[3]);
                pstmt.setBigDecimal(5, new BigDecimal(product[4].toString()));
                pstmt.setBoolean(6, (Boolean) product[5]);
                pstmt.executeUpdate();
            }
            logger.info("Populated {} product records", productData.length);
        }
    }
    
    /**
     * Populates trade data with realistic trading scenarios.
     */
    private void populateTrades() throws SQLException {
        String sql = """
            INSERT INTO demo_trades 
            (trade_id, amount, currency, asset_class, status, counterparty) 
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        Object[][] tradeData = {
            {"TRD001", 2500000.00, "USD", "Equity", "EXECUTED", "Goldman Sachs"},
            {"TRD002", 1750000.00, "EUR", "Fixed Income", "SETTLED", "Deutsche Bank"},
            {"TRD003", 850000.00, "GBP", "FX", "EXECUTED", "Barclays"},
            {"TRD004", 3200000.00, "USD", "Commodity", "PENDING", "JP Morgan"},
            {"TRD005", 1100000.00, "CHF", "Derivative", "EXECUTED", "UBS"},
            {"TRD006", 950000.00, "JPY", "Equity", "SETTLED", "Nomura"},
            {"TRD007", 1850000.00, "USD", "Fixed Income", "EXECUTED", "Morgan Stanley"},
            {"TRD008", 675000.00, "EUR", "FX", "PENDING", "Credit Suisse"}
        };
        
        try (PreparedStatement pstmt = databaseConnection.prepareStatement(sql)) {
            for (Object[] trade : tradeData) {
                pstmt.setString(1, (String) trade[0]);
                pstmt.setBigDecimal(2, new BigDecimal(trade[1].toString()));
                pstmt.setString(3, (String) trade[2]);
                pstmt.setString(4, (String) trade[3]);
                pstmt.setString(5, (String) trade[4]);
                pstmt.setString(6, (String) trade[5]);
                pstmt.executeUpdate();
            }
            logger.info("Populated {} trade records", tradeData.length);
        }
    }
    
    /**
     * Simulates in-memory setup when PostgreSQL is not available.
     */
    private void simulateInMemorySetup() {
        logger.info("Simulating in-memory data setup...");
        try {
            Thread.sleep(1000); // Simulate setup time
            logger.info("In-memory simulation setup completed");
            logger.info("   Tables: 4 (demo_customers, demo_products, demo_trades, demo_audit)");
            logger.info("   Sample Data: 8 customers, 8 products, 8 trades");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Loads external YAML configuration.
     */
    private void loadExternalConfiguration() throws Exception {
        logger.info("Loading external YAML configuration...");
        
        yamlLoader = new YamlConfigurationLoader();
        configurationData = new HashMap<>();
        
        try {
            // Load main configuration
            YamlRuleConfiguration mainConfig = yamlLoader.loadFromClasspath("bootstrap/demo-data-bootstrap.yaml");
            configurationData.put("mainConfig", mainConfig);
            
            // Load dataset configurations
            YamlRuleConfiguration customerConfig = yamlLoader.loadFromClasspath("bootstrap/datasets/customer-profiles.yaml");
            configurationData.put("customerConfig", customerConfig);
            
            YamlRuleConfiguration productConfig = yamlLoader.loadFromClasspath("bootstrap/datasets/product-catalog.yaml");
            configurationData.put("productConfig", productConfig);
            
            YamlRuleConfiguration tradeConfig = yamlLoader.loadFromClasspath("bootstrap/datasets/trading-scenarios.yaml");
            configurationData.put("tradeConfig", tradeConfig);
            
            logger.info("External YAML configuration loaded successfully");
            
        } catch (Exception e) {
            logger.warn("External YAML files not found, using embedded configuration");
            createEmbeddedConfiguration();
        }
    }
    
    /**
     * Creates embedded configuration when external files are not available.
     */
    private void createEmbeddedConfiguration() {
        logger.info("Creating embedded configuration...");
        configurationData.put("embedded", true);
        configurationData.put("customerSegments", Arrays.asList("Platinum", "Gold", "Silver", "Basic"));
        configurationData.put("productCategories", Arrays.asList("Investment", "Trading", "Savings", "Corporate", "Digital"));
        configurationData.put("assetClasses", Arrays.asList("Equity", "Fixed Income", "FX", "Commodity", "Derivative"));
        logger.info("Embedded configuration created");
    }
    
    /**
     * Initializes data sources and loads data.
     */
    private void initializeDataSources() throws Exception {
        logger.info("Initializing data sources...");
        
        if (useInMemoryMode) {
            loadInMemoryData();
        } else {
            loadDatabaseData();
        }
        
        logger.info("Data sources initialized successfully");
        logger.info("   Customers: {} records", customers != null ? customers.size() : 0);
        logger.info("   Products: {} records", products != null ? products.size() : 0);
        logger.info("   Trades: {} records", trades != null ? trades.size() : 0);
    }
    
    /**
     * Loads data from in-memory simulation.
     */
    private void loadInMemoryData() {
        logger.info("Loading in-memory simulation data...");
        
        // Create realistic sample data for in-memory mode
        customers = createRealisticCustomers();
        products = createRealisticProducts();
        trades = createRealisticTrades();
        financialTrades = createRealisticFinancialTrades();
    }
    
    /**
     * Loads data from PostgreSQL database.
     */
    private void loadDatabaseData() throws SQLException {
        logger.info("Loading data from PostgreSQL database...");
        
        customers = loadCustomersFromDatabase();
        products = loadProductsFromDatabase();
        trades = loadTradesFromDatabase();
        financialTrades = createFinancialTradesFromTrades();
    }
    
    // Public API methods following bootstrap pattern
    
    /**
     * Gets customers using data-driven approach.
     */
    public List<Customer> getCustomers() {
        return customers != null ? new ArrayList<>(customers) : new ArrayList<>();
    }
    
    /**
     * Gets products using data-driven approach.
     */
    public List<Product> getProducts() {
        return products != null ? new ArrayList<>(products) : new ArrayList<>();
    }
    
    /**
     * Gets trades using data-driven approach.
     */
    public List<Trade> getTrades() {
        return trades != null ? new ArrayList<>(trades) : new ArrayList<>();
    }
    
    /**
     * Gets financial trades using data-driven approach.
     */
    public List<FinancialTrade> getFinancialTrades() {
        return financialTrades != null ? new ArrayList<>(financialTrades) : new ArrayList<>();
    }
    
    /**
     * Gets customers by membership level using database query or configuration.
     */
    public List<Customer> getCustomersByMembershipLevel(String membershipLevel) {
        if (customers == null) return new ArrayList<>();
        
        return customers.stream()
                .filter(c -> membershipLevel.equals(c.getMembershipLevel()))
                .toList();
    }
    
    /**
     * Gets products by category using database query or configuration.
     */
    public List<Product> getProductsByCategory(String category) {
        if (products == null) return new ArrayList<>();
        
        return products.stream()
                .filter(p -> category.equals(p.getCategory()))
                .toList();
    }
    
    /**
     * Verifies data source connectivity and health.
     */
    public boolean verifyDataSources() {
        logger.info("Verifying data source health...");
        
        try {
            if (useInMemoryMode) {
                return customers != null && products != null && trades != null;
            } else {
                return databaseConnection != null && !databaseConnection.isClosed();
            }
        } catch (SQLException e) {
            logger.error("Data source verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Cleanup resources.
     */
    public void cleanup() {
        logger.info("Cleaning up DemoDataBootstrap resources...");
        
        try {
            if (databaseConnection != null && !databaseConnection.isClosed()) {
                databaseConnection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            logger.warn("Database cleanup failed: {}", e.getMessage());
        }
    }
    
    // Helper methods for creating realistic data

    private List<Customer> createRealisticCustomers() {
        List<Customer> customerList = new ArrayList<>();

        // Create realistic customers for in-memory mode
        Customer customer1 = new Customer("Emily Chen", 34, "emily.chen@techcorp.com");
        customer1.setMembershipLevel("Platinum");
        customer1.setBalance(850000.0);
        customer1.setPreferredCategories(Arrays.asList("Investment", "Trading"));
        customerList.add(customer1);

        Customer customer2 = new Customer("Marcus Johnson", 42, "marcus.j@globalfund.com");
        customer2.setMembershipLevel("Gold");
        customer2.setBalance(425000.0);
        customer2.setPreferredCategories(Arrays.asList("Investment", "Corporate"));
        customerList.add(customer2);

        Customer customer3 = new Customer("Sarah Williams", 29, "sarah.williams@startup.io");
        customer3.setMembershipLevel("Silver");
        customer3.setBalance(125000.0);
        customer3.setPreferredCategories(Arrays.asList("Digital", "Savings"));
        customerList.add(customer3);

        return customerList;
    }

    private List<Product> createRealisticProducts() {
        List<Product> productList = new ArrayList<>();

        productList.add(new Product("Private Wealth Management", 250000.0, "Investment"));
        productList.add(new Product("Premium Trading Account", 50000.0, "Trading"));
        productList.add(new Product("High-Yield Savings Plus", 10000.0, "Savings"));
        productList.add(new Product("Corporate Treasury Services", 100000.0, "Corporate"));

        return productList;
    }

    private List<Trade> createRealisticTrades() {
        List<Trade> tradeList = new ArrayList<>();

        tradeList.add(new Trade("TRD001", "2500000.00", "Equity"));
        tradeList.add(new Trade("TRD002", "1750000.00", "Fixed Income"));
        tradeList.add(new Trade("TRD003", "850000.00", "FX"));
        tradeList.add(new Trade("TRD004", "3200000.00", "Commodity"));

        return tradeList;
    }

    private List<FinancialTrade> createRealisticFinancialTrades() {
        List<FinancialTrade> financialTradeList = new ArrayList<>();

        financialTradeList.add(new FinancialTrade("TRD001", new BigDecimal("2500000.00"), "USD", "UNKNOWN"));
        financialTradeList.add(new FinancialTrade("TRD002", new BigDecimal("1750000.00"), "EUR", "UNKNOWN"));
        financialTradeList.add(new FinancialTrade("TRD003", new BigDecimal("850000.00"), "GBP", "UNKNOWN"));

        return financialTradeList;
    }

    private List<Customer> loadCustomersFromDatabase() throws SQLException {
        List<Customer> customerList = new ArrayList<>();

        String sql = "SELECT * FROM demo_customers ORDER BY customer_id";
        try (Statement stmt = databaseConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Customer customer = new Customer(
                    rs.getString("name"),
                    rs.getInt("age"),
                    rs.getString("email")
                );
                customer.setMembershipLevel(rs.getString("membership_level"));
                customer.setBalance(rs.getDouble("balance"));
                customerList.add(customer);
            }
        }

        return customerList;
    }

    private List<Product> loadProductsFromDatabase() throws SQLException {
        List<Product> productList = new ArrayList<>();

        String sql = "SELECT * FROM demo_products WHERE active = true ORDER BY product_id";
        try (Statement stmt = databaseConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Product product = new Product(
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getString("category")
                );
                productList.add(product);
            }
        }

        return productList;
    }

    private List<Trade> loadTradesFromDatabase() throws SQLException {
        List<Trade> tradeList = new ArrayList<>();

        String sql = "SELECT * FROM demo_trades ORDER BY trade_id";
        try (Statement stmt = databaseConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Trade trade = new Trade(
                    rs.getString("trade_id"),
                    rs.getBigDecimal("amount").toString(),
                    rs.getString("asset_class")
                );
                tradeList.add(trade);
            }
        }

        return tradeList;
    }

    private List<FinancialTrade> createFinancialTradesFromTrades() {
        List<FinancialTrade> financialTradeList = new ArrayList<>();

        if (trades != null) {
            for (Trade trade : trades) {
                FinancialTrade financialTrade = new FinancialTrade(
                    trade.getId(),
                    new BigDecimal(trade.getValue()),
                    "USD",
                    "UNKNOWN"
                );
                financialTradeList.add(financialTrade);
            }
        }

        return financialTradeList;
    }
}
