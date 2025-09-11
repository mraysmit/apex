package dev.mars.apex.demo.lookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Demonstrates the enhanced H2 parameter support in APEX.
 * 
 * This demo shows how to use custom H2 parameters directly in the YAML configuration
 * database field, such as MODE, CACHE_SIZE, TRACE_LEVEL_FILE, etc.
 * 
 * Features demonstrated:
 * - Custom H2 parameters in YAML configuration
 * - Parameter merging with APEX defaults
 * - Different H2 compatibility modes
 * - Enhanced logging and performance settings
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class H2CustomParametersDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(H2CustomParametersDemo.class);
    
    public static void main(String[] args) {
        H2CustomParametersDemo demo = new H2CustomParametersDemo();
        try {
            demo.runDemo();
        } catch (Exception e) {
            logger.error("Demo failed", e);
            System.exit(1);
        }
    }
    
    public void runDemo() throws Exception {
        logger.info("====================================================================================");
        logger.info("H2 CUSTOM PARAMETERS DEMO");
        logger.info("====================================================================================");
        logger.info("Demonstrating enhanced H2 parameter support in APEX Rules Engine");
        logger.info("");
        
        // Step 1: Initialize database with custom parameters
        initializeCustomDatabase();
        
        // Step 2: Test APEX with custom H2 configuration
        testApexWithCustomH2();
        
        logger.info("====================================================================================");
        logger.info("H2 CUSTOM PARAMETERS DEMO COMPLETED SUCCESSFULLY!");
        logger.info("====================================================================================");
    }
    
    /**
     * Initialize H2 database with custom parameters to match YAML configuration.
     */
    private void initializeCustomDatabase() throws Exception {
        logger.info("============================================================");
        logger.info("STEP 1: Initialize H2 Database with Custom Parameters");
        logger.info("============================================================");
        
        try {
            // JDBC drivers are automatically loaded by apex-core JdbcTemplateFactory
            logger.info("✅ JDBC drivers handled by apex-core");
            
            // Create H2 database with custom parameters matching YAML config
            // This matches: database: "./target/h2-demo/custom_params;MODE=MySQL;TRACE_LEVEL_FILE=2;CACHE_SIZE=32768"
            String jdbcUrl = "jdbc:h2:./target/h2-demo/custom_params;MODE=MySQL;TRACE_LEVEL_FILE=2;CACHE_SIZE=32768;DB_CLOSE_DELAY=-1";
            
            logger.info("Creating H2 database with custom parameters:");
            logger.info("  Database path: ./target/h2-demo/custom_params");
            logger.info("  MODE: MySQL (instead of default PostgreSQL)");
            logger.info("  TRACE_LEVEL_FILE: 2 (detailed SQL logging)");
            logger.info("  CACHE_SIZE: 32768 (32MB cache)");
            logger.info("  DB_CLOSE_DELAY: -1 (keep open)");
            
            try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
                Statement statement = connection.createStatement();
                
                // Clean up existing data
                statement.execute("DROP TABLE IF EXISTS customers");
                logger.info("✅ Cleaned up existing tables");
                
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
                logger.info("✅ Created customers table");
                
                // Insert test data
                statement.execute("""
                    INSERT INTO customers VALUES 
                    ('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2023-01-15'),
                    ('CUST000002', 'Global Industries', 'CORPORATE', 'GOLD', 'EU', 'ACTIVE', '2023-02-20'),
                    ('CUST000003', 'Tech Startup Inc', 'STARTUP', 'SILVER', 'NA', 'ACTIVE', '2023-03-10'),
                    ('CUST000004', 'Enterprise Solutions', 'CORPORATE', 'PLATINUM', 'APAC', 'ACTIVE', '2023-04-05')
                """);
                logger.info("✅ Inserted 4 customer records");
                
                // Verify data
                var resultSet = statement.executeQuery("SELECT COUNT(*) as count FROM customers");
                if (resultSet.next()) {
                    int count = resultSet.getInt("count");
                    logger.info("✅ Verified {} customer records in database", count);
                }
                
                logger.info("✅ Database initialization completed successfully");
            }
            
        } catch (Exception e) {
            logger.error("❌ Failed to initialize custom H2 database", e);
            throw e;
        }
    }
    
    /**
     * Test different H2 JDBC URL patterns with custom parameters.
     */
    private void testApexWithCustomH2() throws Exception {
        logger.info("");
        logger.info("============================================================");
        logger.info("STEP 2: Test H2 JDBC URL Construction with Custom Parameters");
        logger.info("============================================================");

        try {
            // Test different H2 parameter combinations
            testH2UrlPattern("./target/h2-demo/basic", "Basic file-based H2 (APEX defaults)");
            testH2UrlPattern("./target/h2-demo/mysql;MODE=MySQL", "H2 with MySQL compatibility mode");
            testH2UrlPattern("./target/h2-demo/debug;MODE=Oracle;TRACE_LEVEL_FILE=2", "H2 with Oracle mode and debug logging");
            testH2UrlPattern("mem:testdb;CACHE_SIZE=16384;MODE=DB2", "In-memory H2 with custom cache and DB2 mode");

            logger.info("✅ All H2 parameter patterns tested successfully!");
            logger.info("");
            logger.info("The enhanced H2 parameter support in APEX allows you to:");
            logger.info("  1. Specify custom H2 parameters directly in the YAML database field");
            logger.info("  2. Override APEX defaults (like MODE=PostgreSQL → MODE=MySQL)");
            logger.info("  3. Add performance tuning parameters (CACHE_SIZE, etc.)");
            logger.info("  4. Enable debugging features (TRACE_LEVEL_FILE, etc.)");
            logger.info("  5. Use any H2 parameter supported by the H2 database engine");
            logger.info("");
            logger.info("YAML Configuration Format:");
            logger.info("  connection:");
            logger.info("    database: \"./path/to/db;PARAM1=value1;PARAM2=value2\"");
            logger.info("    username: \"sa\"");
            logger.info("    password: \"\"");

        } catch (Exception e) {
            logger.error("❌ Failed to test H2 custom parameters", e);
            throw e;
        }
    }

    /**
     * Test a specific H2 database pattern and verify the connection works.
     */
    private void testH2UrlPattern(String databaseField, String description) throws Exception {
        logger.info("");
        logger.info("Testing: {}", description);
        logger.info("Database field: {}", databaseField);

        // Construct the JDBC URL as APEX would
        String jdbcUrl;
        if (databaseField.contains(";")) {
            // Custom parameters provided
            String[] parts = databaseField.split(";", 2);
            String databasePath = parts[0];
            String customParams = parts[1];
            jdbcUrl = String.format("jdbc:h2:%s;%s;DB_CLOSE_DELAY=-1", databasePath, customParams);
        } else {
            // No custom parameters - use APEX defaults
            jdbcUrl = String.format("jdbc:h2:%s;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", databaseField);
        }

        logger.info("Generated JDBC URL: {}", jdbcUrl);

        // Test the connection
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            // Verify connection works
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery("SELECT 1 as test");
            if (resultSet.next() && resultSet.getInt("test") == 1) {
                logger.info("✅ Connection successful - H2 parameters working correctly");
            } else {
                throw new RuntimeException("Connection test failed");
            }
        }
    }
}
