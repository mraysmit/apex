package dev.mars.apex.demo.etl;

import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
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
 * Test class for PipelineEtlExecutionTestExtractDatabase.yaml
 * Tests database extraction functionality
 */
@DisplayName("Database Extract Pipeline Test")
class PipelineEtlExecutionTestExtractDatabase extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlExecutionTestExtractDatabase.class);

    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up Database Extract Pipeline Test...");

        try {
            // Ensure database directory exists
            Path dbDir = Paths.get("./target/test/etl/database");
            Files.createDirectories(dbDir);

            // Setup H2 database with customers table and test data
            setupCustomerDatabase();

        } catch (IOException e) {
            throw new RuntimeException("Failed to create database directory", e);
        }

        logger.info("[OK] Database Extract Pipeline Test setup completed");
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
     * Setup H2 database with customers table and test data.
     * Following the pattern from other ETL tests.
     */
    private void setupCustomerDatabase() {
        logger.info("Setting up H2 database with customer test data...");

        String jdbcUrl = "jdbc:h2:./target/test/etl/database/test_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();

            // Drop existing table
            statement.execute("DROP TABLE IF EXISTS customers");

            // Create customers table with columns expected by the query
            statement.execute("""
                CREATE TABLE customers (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255),
                    status VARCHAR(50) DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            // Insert test data
            statement.execute("""
                INSERT INTO customers (id, name, email, status) VALUES
                (1, 'John Doe', 'john.doe@example.com', 'ACTIVE'),
                (2, 'Jane Smith', 'jane.smith@example.com', 'ACTIVE'),
                (3, 'Bob Johnson', 'bob.johnson@example.com', 'INACTIVE')
                """);

            logger.info("[OK] H2 database setup completed successfully with 3 customer records");

        } catch (Exception e) {
            logger.error("Failed to setup H2 database: " + e.getMessage(), e);
            throw new RuntimeException("Database setup failed", e);
        }
    }

    @Test
    @DisplayName("Should extract data from H2 database")
    void shouldExtractDataFromH2Database() throws Exception {
        logger.info("=== Testing Database Extract Pipeline ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineEtlExecutionTestExtractDatabase.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");

        logger.info("[OK] Database extract pipeline test completed successfully");
    }
}
