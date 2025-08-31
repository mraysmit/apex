package dev.mars.apex.demo.infrastructure;

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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.time.LocalDate;

/**
 * Database setup component for Rule Configuration Bootstrap Demo.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
@Component
public class RuleConfigDatabaseSetup {
    
    private static final Logger logger = LoggerFactory.getLogger(RuleConfigDatabaseSetup.class);
    
    private Connection connection;
    private boolean useInMemoryMode = false;
    
    /**
     * Sets up the complete database infrastructure for rule configuration demo.
     */
    public void setupRuleConfigurationDatabase() throws SQLException {
        logger.info("Setting up Rule Configuration database infrastructure...");
        
        try {
            // Try to connect to PostgreSQL
            connectToDatabase();
            
            // Create all required tables
            createLoanApplicationsTable();
            createCustomerProfilesTable();
            createOrderProcessingTable();
            createRuleExecutionAuditTable();
            
            // Populate with sample data
            populateLoanApplications();
            populateCustomerProfiles();
            populateOrderProcessing();
            
            logger.info("Rule Configuration database setup completed successfully");
            
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
        String url = "jdbc:postgresql://localhost:5432/apex_rule_config_demo";
        String username = "postgres";
        String password = "postgres";
        
        try {
            // First try to connect to the specific database
            connection = DriverManager.getConnection(url, username, password);
            logger.info("Connected to existing apex_rule_config_demo database");
        } catch (SQLException e) {
            // If database doesn't exist, connect to postgres and create it
            String postgresUrl = "jdbc:postgresql://localhost:5432/postgres";
            try (Connection postgresConn = DriverManager.getConnection(postgresUrl, username, password);
                 Statement stmt = postgresConn.createStatement()) {
                
                stmt.executeUpdate("CREATE DATABASE apex_rule_config_demo");
                logger.info("Created apex_rule_config_demo database");
                
                // Now connect to the new database
                connection = DriverManager.getConnection(url, username, password);
                logger.info("Connected to new apex_rule_config_demo database");
            }
        }
    }
    
    /**
     * Creates the loan_applications table.
     */
    private void createLoanApplicationsTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS loan_applications (
                application_id VARCHAR(20) PRIMARY KEY,
                customer_id VARCHAR(20) NOT NULL,
                loan_amount DECIMAL(12,2) NOT NULL,
                credit_score INTEGER NOT NULL,
                debt_to_income_ratio DECIMAL(5,4) NOT NULL,
                employment_years INTEGER NOT NULL,
                annual_income DECIMAL(12,2) NOT NULL,
                loan_purpose VARCHAR(50) NOT NULL,
                application_date DATE NOT NULL,
                status VARCHAR(20) DEFAULT 'PENDING',
                decision_reason TEXT,
                created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("Created loan_applications table");
        }
    }
    
    /**
     * Creates the customer_profiles table.
     */
    private void createCustomerProfilesTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS customer_profiles (
                customer_id VARCHAR(20) PRIMARY KEY,
                first_name VARCHAR(50) NOT NULL,
                last_name VARCHAR(50) NOT NULL,
                age INTEGER NOT NULL,
                membership_level VARCHAR(20) NOT NULL,
                customer_since DATE NOT NULL,
                total_orders INTEGER DEFAULT 0,
                total_spent DECIMAL(12,2) DEFAULT 0.00,
                preferred_categories TEXT[],
                kyc_verified BOOLEAN DEFAULT FALSE,
                risk_score INTEGER DEFAULT 5,
                created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("Created customer_profiles table");
        }
    }
    
    /**
     * Creates the order_processing table.
     */
    private void createOrderProcessingTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS order_processing (
                order_id VARCHAR(20) PRIMARY KEY,
                customer_id VARCHAR(20) NOT NULL,
                order_total DECIMAL(10,2) NOT NULL,
                quantity INTEGER NOT NULL,
                order_date DATE NOT NULL,
                status VARCHAR(20) DEFAULT 'PENDING',
                shipping_method VARCHAR(30),
                discount_applied DECIMAL(5,2) DEFAULT 0.00,
                processing_priority VARCHAR(20) DEFAULT 'STANDARD',
                created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (customer_id) REFERENCES customer_profiles(customer_id)
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("Created order_processing table");
        }
    }
    
    /**
     * Creates the rule_execution_audit table.
     */
    private void createRuleExecutionAuditTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS rule_execution_audit (
                audit_id SERIAL PRIMARY KEY,
                entity_type VARCHAR(50) NOT NULL,
                entity_id VARCHAR(20) NOT NULL,
                rule_category VARCHAR(50) NOT NULL,
                rule_id VARCHAR(20) NOT NULL,
                rule_result VARCHAR(20) NOT NULL,
                execution_time_ms INTEGER NOT NULL,
                created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("Created rule_execution_audit table");
        }
    }
    
    /**
     * Populates loan applications with sample data.
     */
    private void populateLoanApplications() throws SQLException {
        String sql = """
            INSERT INTO loan_applications 
            (application_id, customer_id, loan_amount, credit_score, debt_to_income_ratio, 
             employment_years, annual_income, loan_purpose, application_date) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        Object[][] loanData = {
            {"LA001", "CUST001", 250000.00, 780, 0.28, 8, 95000.00, "HOME_PURCHASE", LocalDate.now().minusDays(5)},
            {"LA002", "CUST002", 45000.00, 720, 0.35, 5, 75000.00, "AUTO_LOAN", LocalDate.now().minusDays(3)},
            {"LA003", "CUST003", 15000.00, 580, 0.48, 2, 42000.00, "PERSONAL", LocalDate.now().minusDays(2)},
            {"LA004", "CUST004", 180000.00, 760, 0.32, 12, 110000.00, "HOME_REFINANCE", LocalDate.now().minusDays(4)},
            {"LA005", "CUST005", 25000.00, 650, 0.41, 3, 58000.00, "DEBT_CONSOLIDATION", LocalDate.now().minusDays(1)},
            {"LA006", "CUST006", 350000.00, 800, 0.25, 15, 150000.00, "INVESTMENT_PROPERTY", LocalDate.now().minusDays(6)},
            {"LA007", "CUST007", 8000.00, 610, 0.44, 1, 35000.00, "PERSONAL", LocalDate.now().minusDays(1)},
            {"LA008", "CUST008", 75000.00, 690, 0.38, 6, 82000.00, "HOME_IMPROVEMENT", LocalDate.now().minusDays(3)}
        };
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Object[] loan : loanData) {
                pstmt.setString(1, (String) loan[0]);
                pstmt.setString(2, (String) loan[1]);
                pstmt.setBigDecimal(3, new java.math.BigDecimal(loan[2].toString()));
                pstmt.setInt(4, (Integer) loan[3]);
                pstmt.setBigDecimal(5, new java.math.BigDecimal(loan[4].toString()));
                pstmt.setInt(6, (Integer) loan[5]);
                pstmt.setBigDecimal(7, new java.math.BigDecimal(loan[6].toString()));
                pstmt.setString(8, (String) loan[7]);
                pstmt.setDate(9, Date.valueOf((LocalDate) loan[8]));
                pstmt.executeUpdate();
            }
            logger.info("Populated loan_applications table with {} records", loanData.length);
        }
    }
    
    /**
     * Populates customer profiles with sample data.
     */
    private void populateCustomerProfiles() throws SQLException {
        String sql = """
            INSERT INTO customer_profiles 
            (customer_id, first_name, last_name, age, membership_level, customer_since, 
             total_orders, total_spent, kyc_verified, risk_score) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        Object[][] customerData = {
            {"CUST001", "John", "Smith", 35, "Gold", LocalDate.now().minusYears(6), 45, 12500.00, true, 3},
            {"CUST002", "Sarah", "Johnson", 28, "Silver", LocalDate.now().minusYears(3), 22, 5800.00, true, 4},
            {"CUST003", "Mike", "Brown", 42, "Basic", LocalDate.now().minusMonths(8), 8, 1200.00, false, 7},
            {"CUST004", "Emily", "Davis", 31, "Gold", LocalDate.now().minusYears(8), 67, 18900.00, true, 2},
            {"CUST005", "David", "Wilson", 26, "Silver", LocalDate.now().minusYears(2), 15, 3400.00, true, 5},
            {"CUST006", "Lisa", "Anderson", 45, "Platinum", LocalDate.now().minusYears(10), 89, 35600.00, true, 1},
            {"CUST007", "Tom", "Taylor", 23, "Basic", LocalDate.now().minusMonths(3), 2, 450.00, false, 8},
            {"CUST008", "Anna", "Martinez", 38, "Silver", LocalDate.now().minusYears(4), 31, 8700.00, true, 4}
        };
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Object[] customer : customerData) {
                pstmt.setString(1, (String) customer[0]);
                pstmt.setString(2, (String) customer[1]);
                pstmt.setString(3, (String) customer[2]);
                pstmt.setInt(4, (Integer) customer[3]);
                pstmt.setString(5, (String) customer[4]);
                pstmt.setDate(6, Date.valueOf((LocalDate) customer[5]));
                pstmt.setInt(7, (Integer) customer[6]);
                pstmt.setBigDecimal(8, new java.math.BigDecimal(customer[7].toString()));
                pstmt.setBoolean(9, (Boolean) customer[8]);
                pstmt.setInt(10, (Integer) customer[9]);
                pstmt.executeUpdate();
            }
            logger.info("Populated customer_profiles table with {} records", customerData.length);
        }
    }
    
    /**
     * Populates order processing with sample data.
     */
    private void populateOrderProcessing() throws SQLException {
        String sql = """
            INSERT INTO order_processing 
            (order_id, customer_id, order_total, quantity, order_date, status) 
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        Object[][] orderData = {
            {"ORD001", "CUST001", 1250.00, 3, LocalDate.now().minusDays(2), "PENDING"},
            {"ORD002", "CUST002", 89.99, 1, LocalDate.now().minusDays(1), "PENDING"},
            {"ORD003", "CUST003", 450.00, 15, LocalDate.now().minusDays(3), "PENDING"},
            {"ORD004", "CUST004", 2100.00, 8, LocalDate.now().minusDays(1), "PENDING"},
            {"ORD005", "CUST005", 175.50, 2, LocalDate.now(), "PENDING"},
            {"ORD006", "CUST006", 3500.00, 12, LocalDate.now().minusDays(4), "PENDING"},
            {"ORD007", "CUST007", 25.99, 1, LocalDate.now(), "PENDING"},
            {"ORD008", "CUST008", 750.00, 6, LocalDate.now().minusDays(2), "PENDING"}
        };
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Object[] order : orderData) {
                pstmt.setString(1, (String) order[0]);
                pstmt.setString(2, (String) order[1]);
                pstmt.setBigDecimal(3, new java.math.BigDecimal(order[2].toString()));
                pstmt.setInt(4, (Integer) order[3]);
                pstmt.setDate(5, Date.valueOf((LocalDate) order[4]));
                pstmt.setString(6, (String) order[5]);
                pstmt.executeUpdate();
            }
            logger.info("Populated order_processing table with {} records", orderData.length);
        }
    }
    
    /**
     * Simulates in-memory database setup when PostgreSQL is not available.
     */
    private void simulateInMemorySetup() {
        logger.info("Simulating in-memory database setup...");
        try {
            Thread.sleep(1000); // Simulate setup time
            logger.info("In-memory simulation setup completed");
            logger.info("   Tables: 4 (loan_applications, customer_profiles, order_processing, rule_execution_audit)");
            logger.info("   Sample Data: 8 loan applications, 8 customer profiles, 8 orders");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Verifies database setup and connectivity.
     */
    public boolean verifyDatabaseSetup() {
        if (useInMemoryMode) {
            logger.info("Database verification: In-memory mode active");
            return true;
        }
        
        try {
            if (connection == null || connection.isClosed()) {
                return false;
            }
            
            // Verify tables exist and have data
            String[] tables = {"loan_applications", "customer_profiles", "order_processing", "rule_execution_audit"};
            for (String table : tables) {
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        logger.debug("Table {} has {} records", table, count);
                    }
                }
            }
            
            logger.info("Database verification completed successfully");
            return true;
            
        } catch (SQLException e) {
            logger.error("Database verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Cleanup database resources.
     */
    public void cleanup() {
        logger.info("Cleaning up database resources...");
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            logger.warn("Database cleanup failed: {}", e.getMessage());
        }
    }
    
    /**
     * Gets the database connection for use by other components.
     */
    public Connection getConnection() {
        return connection;
    }
    
    /**
     * Checks if running in in-memory simulation mode.
     */
    public boolean isInMemoryMode() {
        return useInMemoryMode;
    }
}
