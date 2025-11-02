package dev.mars.apex.demo.etl;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for advanced database extraction in ETL pipelines.
 *
 * This test validates:
 * - Complex SQL queries with JOINs
 * - Aggregation queries (COUNT, SUM, AVG, MAX)
 * - Multi-table queries
 * - GROUP BY and HAVING clauses
 * - LEFT JOIN and INNER JOIN operations
 */
@DisplayName("Pipeline ETL Execution Test - Advanced Database Extract")
public class PipelineEtlExecutionTestExtractDatabaseAdvanced extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlExecutionTestExtractDatabaseAdvanced.class);

    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up Advanced Database Extract Pipeline Test...");

        try {
            // Ensure database directory exists
            Path dbDir = Paths.get("./target/test/etl/database");
            Files.createDirectories(dbDir);

            // Setup H2 database with customers, orders, and order_items tables
            setupAdvancedDatabase();

        } catch (IOException e) {
            throw new RuntimeException("Failed to create database directory", e);
        }

        logger.info("✓ Advanced Database Extract Pipeline Test setup completed");
    }

    @AfterEach
    public void tearDown() {
        if (rulesEngine != null) {
            try {
                rulesEngine.shutdown();
            } catch (Exception e) {
                logger.warn("Error shutting down rules engine", e);
            }
        }
        super.tearDown();
    }

    /**
     * Setup H2 database with customers, orders, and order_items tables.
     */
    private void setupAdvancedDatabase() {
        logger.info("Setting up H2 database with advanced test data...");

        String jdbcUrl = "jdbc:h2:./target/test/etl/database/advanced_test_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();

            // Drop existing tables
            statement.execute("DROP TABLE IF EXISTS order_items");
            statement.execute("DROP TABLE IF EXISTS orders");
            statement.execute("DROP TABLE IF EXISTS customers");

            // Create customers table
            statement.execute("""
                CREATE TABLE customers (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255),
                    status VARCHAR(50) DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            // Create orders table
            statement.execute("""
                CREATE TABLE orders (
                    id INTEGER PRIMARY KEY,
                    customer_id INTEGER NOT NULL,
                    order_date DATE NOT NULL,
                    total_amount DECIMAL(10, 2) NOT NULL,
                    status VARCHAR(50) DEFAULT 'PENDING',
                    FOREIGN KEY (customer_id) REFERENCES customers(id)
                )
                """);

            // Create order_items table
            statement.execute("""
                CREATE TABLE order_items (
                    id INTEGER PRIMARY KEY,
                    order_id INTEGER NOT NULL,
                    product_id VARCHAR(50) NOT NULL,
                    product_name VARCHAR(255) NOT NULL,
                    quantity INTEGER NOT NULL,
                    unit_price DECIMAL(10, 2) NOT NULL,
                    subtotal DECIMAL(10, 2) NOT NULL,
                    FOREIGN KEY (order_id) REFERENCES orders(id)
                )
                """);

            // Insert test customers
            statement.execute("""
                INSERT INTO customers (id, name, email, status) VALUES
                (1, 'John Smith', 'john.smith@example.com', 'ACTIVE'),
                (2, 'Jane Doe', 'jane.doe@example.com', 'ACTIVE'),
                (3, 'Bob Johnson', 'bob.johnson@example.com', 'INACTIVE'),
                (4, 'Alice Williams', 'alice.williams@example.com', 'ACTIVE')
                """);

            // Insert test orders
            statement.execute("""
                INSERT INTO orders (id, customer_id, order_date, total_amount, status) VALUES
                (1, 1, '2025-10-15', 1329.98, 'COMPLETED'),
                (2, 2, '2025-10-20', 579.97, 'PROCESSING'),
                (3, 1, '2025-10-22', 89.99, 'SHIPPED'),
                (4, 4, '2025-10-25', 179.97, 'PENDING')
                """);

            // Insert test order items
            statement.execute("""
                INSERT INTO order_items (id, order_id, product_id, product_name, quantity, unit_price, subtotal) VALUES
                (1, 1, 'PROD-001', 'Laptop Pro 15', 1, 1299.99, 1299.99),
                (2, 1, 'PROD-002', 'Wireless Mouse', 1, 29.99, 29.99),
                (3, 2, 'PROD-004', '4K Monitor 27"', 1, 399.99, 399.99),
                (4, 2, 'PROD-005', 'Mechanical Keyboard', 1, 129.99, 129.99),
                (5, 2, 'PROD-003', 'USB-C Hub', 1, 49.99, 49.99),
                (6, 3, 'PROD-006', 'Webcam HD Pro', 1, 89.99, 89.99),
                (7, 4, 'PROD-002', 'Wireless Mouse', 3, 29.99, 89.97),
                (8, 4, 'PROD-006', 'Webcam HD Pro', 1, 89.99, 89.99)
                """);

            logger.info("✓ H2 database setup completed successfully");
            logger.info("  - 4 customers");
            logger.info("  - 4 orders");
            logger.info("  - 8 order items");

        } catch (Exception e) {
            logger.error("Failed to setup H2 database: " + e.getMessage(), e);
            throw new RuntimeException("Database setup failed", e);
        }
    }

    @Test
    @DisplayName("Should extract customer order summary with aggregations")
    void shouldExtractCustomerOrderSummary() throws Exception {
        logger.info("=== Testing Advanced Database Extract - Customer Order Summary ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/etl/PipelineEtlExecutionTestExtractDatabaseAdvanced.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");

        logger.info("✓ Customer order summary extracted successfully");
    }
}

