package dev.mars.apex.demo.lookup;

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

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.service.data.external.database.DatabaseDataSource;
import dev.mars.apex.core.service.data.external.database.JdbcTemplateFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify comprehensive debug logging in all database operations.
 */
public class DatabaseDebugLoggingTest {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseDebugLoggingTest.class);

    private DatabaseDataSource databaseDataSource;

    @BeforeEach
    void setUp() {
        setupDatabase();
        setupDatabaseDataSource();
    }

    @Test
    @DisplayName("Should demonstrate comprehensive debug logging for all database operations")
    void testComprehensiveDebugLogging() {
        logger.info("=".repeat(80));
        logger.info("COMPREHENSIVE DATABASE DEBUG LOGGING TEST");
        logger.info("=".repeat(80));
        logger.info("Testing all database operations with debug logging enabled");
        logger.info("");

        // Test query operation
        testQueryOperation();
        
        // Test queryForObject operation
        testQueryForObjectOperation();
        
        // Test batch query operation
        testBatchQueryOperation();
        
        logger.info("\n" + "=".repeat(80));
        logger.info("COMPREHENSIVE DATABASE DEBUG LOGGING TEST COMPLETED");
        logger.info("=".repeat(80));
    }

    private void testQueryOperation() {
        logger.info("Testing query() operation with debug logging...");
        
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("counterpartyId", "CP001");
            
            List<Map<String, Object>> results = databaseDataSource.query(
                "SELECT counterparty_id, counterparty_name FROM counterparties WHERE counterparty_id = :counterpartyId", 
                parameters
            );
            
            logger.info("✓ Query operation completed: {} results", results.size());
            
        } catch (Exception e) {
            logger.error("Query operation failed: {}", e.getMessage(), e);
            fail("Query operation failed: " + e.getMessage());
        }
    }

    private void testQueryForObjectOperation() {
        logger.info("Testing queryForObject() operation with debug logging...");
        
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("counterpartyId", "CP001");
            
            Map<String, Object> result = databaseDataSource.queryForObject(
                "SELECT counterparty_id, counterparty_name FROM counterparties WHERE counterparty_id = :counterpartyId", 
                parameters
            );
            
            logger.info("✓ QueryForObject operation completed: {} result", result != null ? "found" : "no");
            
        } catch (Exception e) {
            logger.error("QueryForObject operation failed: {}", e.getMessage(), e);
            fail("QueryForObject operation failed: " + e.getMessage());
        }
    }

    private void testBatchQueryOperation() {
        logger.info("Testing batchQuery() operation with debug logging...");
        
        try {
            List<String> queries = List.of(
                "SELECT COUNT(*) as total FROM counterparties",
                "SELECT COUNT(*) as total FROM settlement_instructions"
            );
            
            List<List<Map<String, Object>>> results = databaseDataSource.batchQuery(queries);
            
            logger.info("✓ Batch query operation completed: {} query results", results.size());
            
        } catch (Exception e) {
            logger.error("Batch query operation failed: {}", e.getMessage(), e);
            fail("Batch query operation failed: " + e.getMessage());
        }
    }

    private void setupDatabase() {
        logger.info("Setting up H2 database for debug logging test...");
        
        String jdbcUrl = "jdbc:h2:./target/h2-demo/debug_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();

            // Drop existing tables
            statement.execute("DROP TABLE IF EXISTS settlement_instructions");
            statement.execute("DROP TABLE IF EXISTS counterparties");

            // Create counterparties table
            statement.execute("""
                CREATE TABLE counterparties (
                    counterparty_id VARCHAR(20) PRIMARY KEY,
                    counterparty_name VARCHAR(100) NOT NULL,
                    counterparty_type VARCHAR(20),
                    credit_rating VARCHAR(10)
                )
                """);

            // Create settlement_instructions table
            statement.execute("""
                CREATE TABLE settlement_instructions (
                    instruction_id VARCHAR(20) PRIMARY KEY,
                    counterparty_id VARCHAR(20) NOT NULL,
                    instrument_type VARCHAR(20) NOT NULL,
                    currency VARCHAR(3) NOT NULL,
                    market VARCHAR(10) NOT NULL,
                    min_amount DECIMAL(15,2),
                    max_amount DECIMAL(15,2),
                    priority INTEGER DEFAULT 1,
                    FOREIGN KEY (counterparty_id) REFERENCES counterparties(counterparty_id)
                )
                """);

            // Insert test data
            statement.execute("""
                INSERT INTO counterparties (counterparty_id, counterparty_name, counterparty_type, credit_rating) VALUES
                ('CP001', 'Goldman Sachs', 'BANK', 'AAA'),
                ('CP002', 'JP Morgan', 'BANK', 'AAA')
                """);

            statement.execute("""
                INSERT INTO settlement_instructions (instruction_id, counterparty_id, instrument_type, currency, market, min_amount, max_amount, priority) VALUES
                ('SI001', 'CP001', 'BOND', 'USD', 'NYSE', 1000.00, 1000000.00, 1),
                ('SI002', 'CP002', 'EQUITY', 'USD', 'NASDAQ', 500.00, 500000.00, 2)
                """);

            logger.info("✓ Database setup completed successfully");

        } catch (Exception e) {
            logger.error("Failed to setup database: {}", e.getMessage(), e);
            throw new RuntimeException("Database setup failed", e);
        }
    }

    private void setupDatabaseDataSource() {
        logger.info("Setting up DatabaseDataSource for debug logging test...");
        
        try {
            // Create configuration
            DataSourceConfiguration config = new DataSourceConfiguration();
            config.setName("debug-test-database");
            config.setSourceType("h2");
            config.setEnabled(true);
            
            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setDatabase("./target/h2-demo/debug_test");
            connectionConfig.setUsername("sa");
            connectionConfig.setPassword("");
            config.setConnection(connectionConfig);

            // Create DataSource using JdbcTemplateFactory
            DataSource dataSource = JdbcTemplateFactory.createDataSource(config);
            
            // Create DatabaseDataSource
            this.databaseDataSource = new DatabaseDataSource(dataSource, config);
            this.databaseDataSource.initialize(config);
            
            logger.info("✓ DatabaseDataSource setup completed successfully");
            
        } catch (Exception e) {
            logger.error("Failed to setup DatabaseDataSource: {}", e.getMessage(), e);
            throw new RuntimeException("DatabaseDataSource setup failed", e);
        }
    }
}
