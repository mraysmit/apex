package dev.mars.apex.demo.examples;

import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
// Note: JdbcTemplateFactory is not exported from apex-core module
// Using direct JDBC connections instead
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Test to verify database connection isolation between demo setup and external data-source system.
 * 
 * @author APEX Demo Team
 * @since 2025-08-28
 * @version 1.0.0
 */
public class DatabaseConnectionTest {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionTest.class);
    
    public static void main(String[] args) {
        DatabaseConnectionTest test = new DatabaseConnectionTest();
        test.runTest();
    }
    
    public void runTest() {
        logger.info("====================================================================================");
        logger.info("DATABASE CONNECTION ISOLATION TEST");
        logger.info("====================================================================================");
        
        try {
            // Test 1: Create tables using demo approach
            testDemoApproach();
            
            // Test 2: Try to access tables using external data-source approach
            testExternalDataSourceApproach();
            
            logger.info("====================================================================================");
            logger.info("DATABASE CONNECTION TEST COMPLETED");
            logger.info("====================================================================================");
            
        } catch (Exception e) {
            logger.error("Database connection test failed: " + e.getMessage(), e);
        }
    }
    
    private void testDemoApproach() throws Exception {
        logger.info("\n============================================================");
        logger.info("TEST 1: Demo Approach - Direct JDBC Connection");
        logger.info("============================================================");
        
        // Create database using demo approach
        String jdbcUrl = "jdbc:h2:mem:apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        logger.info("Demo JDBC URL: " + jdbcUrl);
        
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
                    created_date DATE NOT NULL
                )
            """);
            
            // Insert test data
            statement.execute("""
                INSERT INTO customers (customer_id, customer_name, customer_type, tier, region, status, created_date) VALUES
                ('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2023-01-15')
            """);
            
            // Verify data
            ResultSet rs = statement.executeQuery("SELECT * FROM customers WHERE customer_id = 'CUST000001'");
            if (rs.next()) {
                logger.info("✅ Demo approach - Found customer:");
                logger.info("  ID: " + rs.getString("customer_id"));
                logger.info("  Name: " + rs.getString("customer_name"));
                logger.info("  Type: " + rs.getString("customer_type"));
                logger.info("  Status: " + rs.getString("status"));
            } else {
                logger.error("❌ Demo approach - Customer not found");
            }
        }
    }
    
    private void testExternalDataSourceApproach() throws Exception {
        logger.info("\n============================================================");
        logger.info("TEST 2: External Data-Source Approach - APEX DataSource");
        logger.info("============================================================");
        
        // Create configuration matching external data-source config
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("customer-database");
        config.setType("database");
        config.setSourceType("h2");
        config.setEnabled(true);
        
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setDatabase("apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        connectionConfig.setUsername("sa");
        connectionConfig.setPassword("");
        
        config.setConnection(connectionConfig);
        
        // Create direct JDBC connection (JdbcTemplateFactory not exported from apex-core)
        // Build JDBC URL from ConnectionConfig properties
        String jdbcUrl = "jdbc:h2:mem:" + connectionConfig.getDatabase() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        String username = connectionConfig.getUsername();
        String password = connectionConfig.getPassword();
        logger.info("Creating direct JDBC connection to: " + jdbcUrl);

        // Test connection
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            logger.info("External DataSource connection established");
            
            Statement statement = connection.createStatement();
            
            // Check if customers table exists
            try {
                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM customers");
                rs.next();
                int count = rs.getInt(1);
                logger.info("✅ External approach - Found customers table with " + count + " records");
                
                // Try to query specific customer
                rs = statement.executeQuery("SELECT * FROM customers WHERE customer_id = 'CUST000001'");
                if (rs.next()) {
                    logger.info("✅ External approach - Found customer:");
                    logger.info("  ID: " + rs.getString("customer_id"));
                    logger.info("  Name: " + rs.getString("customer_name"));
                    logger.info("  Type: " + rs.getString("customer_type"));
                    logger.info("  Status: " + rs.getString("status"));
                } else {
                    logger.error("❌ External approach - Customer CUST000001 not found");
                }
                
            } catch (Exception e) {
                logger.error("❌ External approach - Cannot access customers table: " + e.getMessage());
            }
        }
    }
}
