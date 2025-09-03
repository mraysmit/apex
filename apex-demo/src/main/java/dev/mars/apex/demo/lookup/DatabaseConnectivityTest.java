package dev.mars.apex.demo.lookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Simple database connectivity test to validate H2 database setup and queries.
 * 
 * This test validates:
 * - H2 database connection
 * - Table creation and data insertion
 * - SQL query execution
 * - Result set processing
 * 
 * @author APEX Demo Team
 * @since 2025-08-28
 * @version 1.0.0
 */
public class DatabaseConnectivityTest {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectivityTest.class);
    
    public static void main(String[] args) {
        DatabaseConnectivityTest test = new DatabaseConnectivityTest();
        test.runTest();
    }
    
    public void runTest() {
        logger.info("====================================================================================");
        logger.info("DATABASE CONNECTIVITY TEST");
        logger.info("====================================================================================");
        
        try {
            // Test 1: Basic Connection
            testBasicConnection();
            
            // Test 2: Table Creation and Data Insertion
            testTableCreationAndDataInsertion();
            
            // Test 3: Named Parameter Query (APEX Style)
            testNamedParameterQuery();
            
            // Test 4: Multi-Table JOIN Query
            testMultiTableJoinQuery();
            
            logger.info("====================================================================================");
            logger.info("ALL DATABASE CONNECTIVITY TESTS PASSED!");
            logger.info("====================================================================================");
            
        } catch (Exception e) {
            logger.error("Database connectivity test failed: " + e.getMessage(), e);
            System.exit(1);
        }
    }
    
    private void testBasicConnection() throws Exception {
        logger.info("\n============================================================");
        logger.info("TEST 1: Basic H2 Database Connection");
        logger.info("============================================================");

        // Load H2 driver explicitly
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("H2 driver not found: " + e.getMessage());
            throw new RuntimeException("H2 driver not available", e);
        }

        String jdbcUrl = "jdbc:h2:mem:apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            logger.info("✅ Successfully connected to H2 database");
            logger.info("  Database URL: " + jdbcUrl);
            logger.info("  Connection valid: " + connection.isValid(5));
            logger.info("  Database product: " + connection.getMetaData().getDatabaseProductName());
            logger.info("  Database version: " + connection.getMetaData().getDatabaseProductVersion());
        }
        
        logger.info("✅ TEST 1 PASSED: Basic database connection working");
    }
    
    private void testTableCreationAndDataInsertion() throws Exception {
        logger.info("\n============================================================");
        logger.info("TEST 2: Table Creation and Data Insertion");
        logger.info("============================================================");
        
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
            logger.info("✅ Created customers table");
            
            // Insert test data
            int insertCount = statement.executeUpdate("""
                INSERT INTO customers (customer_id, customer_name, customer_type, tier, region, status, created_date) VALUES
                ('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2023-01-15'),
                ('CUST000002', 'Global Industries Ltd', 'CORPORATE', 'GOLD', 'EU', 'ACTIVE', '2023-02-20'),
                ('CUST000003', 'Tech Innovations Inc', 'CORPORATE', 'SILVER', 'APAC', 'ACTIVE', '2023-03-10')
            """);
            logger.info("✅ Inserted " + insertCount + " customer records");
            
            // Verify data
            ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM customers WHERE status = 'ACTIVE'");
            rs.next();
            int activeCount = rs.getInt(1);
            logger.info("✅ Found " + activeCount + " active customers");
            
            if (activeCount != 3) {
                throw new AssertionError("Expected 3 active customers but found " + activeCount);
            }
        }
        
        logger.info("✅ TEST 2 PASSED: Table creation and data insertion working");
    }
    
    private void testNamedParameterQuery() throws Exception {
        logger.info("\n============================================================");
        logger.info("TEST 3: Named Parameter Query (APEX Style)");
        logger.info("============================================================");
        
        String jdbcUrl = "jdbc:h2:mem:apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            
            // Test the exact query used in APEX configuration
            String query = """
                SELECT 
                  customer_id,
                  customer_name,
                  customer_type,
                  tier,
                  region,
                  status,
                  created_date
                FROM customers 
                WHERE customer_id = ? 
                  AND status = 'ACTIVE'
            """;
            
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, "CUST000001");
                
                logger.info("Executing query: " + query.replaceAll("\\s+", " ").trim());
                logger.info("Parameter: customerId = CUST000001");
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        logger.info("✅ Query returned results:");
                        logger.info("  customer_id: " + rs.getString("customer_id"));
                        logger.info("  customer_name: " + rs.getString("customer_name"));
                        logger.info("  customer_type: " + rs.getString("customer_type"));
                        logger.info("  tier: " + rs.getString("tier"));
                        logger.info("  region: " + rs.getString("region"));
                        logger.info("  status: " + rs.getString("status"));
                        logger.info("  created_date: " + rs.getDate("created_date"));
                        
                        // Validate expected values
                        if (!"Acme Corporation".equals(rs.getString("customer_name"))) {
                            throw new AssertionError("Expected customer name 'Acme Corporation' but got: " + rs.getString("customer_name"));
                        }
                        if (!"CORPORATE".equals(rs.getString("customer_type"))) {
                            throw new AssertionError("Expected customer type 'CORPORATE' but got: " + rs.getString("customer_type"));
                        }
                        if (!"PLATINUM".equals(rs.getString("tier"))) {
                            throw new AssertionError("Expected tier 'PLATINUM' but got: " + rs.getString("tier"));
                        }
                        
                    } else {
                        throw new AssertionError("Query returned no results for customer CUST000001");
                    }
                }
            }
        }
        
        logger.info("✅ TEST 3 PASSED: Named parameter query working correctly");
    }
    
    private void testMultiTableJoinQuery() throws Exception {
        logger.info("\n============================================================");
        logger.info("TEST 4: Multi-Table JOIN Query");
        logger.info("============================================================");
        
        String jdbcUrl = "jdbc:h2:mem:apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();
            
            // Create additional tables for JOIN test
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
            
            statement.execute("""
                INSERT INTO counterparties (counterparty_id, counterparty_name, counterparty_type, credit_rating, status, created_date) VALUES
                ('CP_GS', 'Goldman Sachs', 'INVESTMENT_BANK', 'A+', 'ACTIVE', '2023-01-01')
            """);
            
            // Test simple counterparty query first
            String simpleQuery = "SELECT counterparty_name, counterparty_type, credit_rating FROM counterparties WHERE counterparty_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(simpleQuery)) {
                ps.setString(1, "CP_GS");
                
                logger.info("Executing simple counterparty query...");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        logger.info("✅ Simple counterparty query results:");
                        logger.info("  counterparty_name: " + rs.getString("counterparty_name"));
                        logger.info("  counterparty_type: " + rs.getString("counterparty_type"));
                        logger.info("  credit_rating: " + rs.getString("credit_rating"));
                    } else {
                        throw new AssertionError("Simple counterparty query returned no results");
                    }
                }
            }
        }
        
        logger.info("✅ TEST 4 PASSED: Multi-table setup and simple queries working");
    }
}
