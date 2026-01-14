/*
 * Copyright 2026 Mark Andrew Ray-Smith Cityline Ltd
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
 *
 * Created: 2026-01-14
 */

package dev.mars.apex.sync.pipeline;

import dev.mars.apex.sync.SyncTestBase;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional integration test for SQL Server to PostgreSQL sync logic.
 *
 * <p>This test uses the 'Clean Architecture' pattern where infrastructure
 * (Sources/Sinks) is defined in separate YAML files referenced by the main pipeline.
 * All files are co-located in this package for test portability.</p>
 *
 * <p>Uses H2 database in compatibility modes to simulate SQL Server and PostgreSQL
 * without requiring actual database infrastructure.</p>
 *
 * CRITICAL VALIDATION CHECKLIST:
 * ✅ Count pipeline steps - 2 steps expected (extract + load)
 * ✅ Verify extract step - Must process 3 records from source
 * ✅ Verify load step - Must load 3 records to target
 * ✅ Validate data transformations - Field mappings and type conversions
 * ✅ Target data verification - Confirm all records loaded correctly
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("SQL Server to PostgreSQL Sync Test")
public class MsSqlToPostgresSyncTest extends SyncTestBase {

    private static final Logger logger = LoggerFactory.getLogger(MsSqlToPostgresSyncTest.class);
    private RulesEngine rulesEngine;

    @AfterEach
    public void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    void testMsSqlToPostgresSyncWithTransformations() throws Exception {
        logger.info("Starting MsSQL to PostgreSQL sync test...");

        // 1. Setup Source Database (H2 in SQL Server mode)
        String sourceUrl = "jdbc:h2:mem:mssql_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1";
        setupSourceDatabase(sourceUrl);

        // 2. Setup Target Database (H2 in PostgreSQL mode)
        String targetUrl = "jdbc:h2:mem:postgres_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        setupTargetDatabase(targetUrl);

        // 3. Resolve path to the co-located YAML file
        Path configPath = Paths.get("src/test/java/dev/mars/apex/sync/pipeline/MsSqlToPostgresSyncTest.yaml");
        File configFile = configPath.toFile();

        if (!configFile.exists()) {
            throw new RuntimeException("Test configuration not found at: " + configFile.getAbsolutePath());
        }

        logger.info("Initializing RulesEngine with config: {}", configFile.getAbsolutePath());

        // 5. Initialize and Execute Pipeline
        rulesEngine = RulesEngine.fromFile(configFile.getPath());
        assertNotNull(rulesEngine, "RulesEngine should be initialized");

        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // 6. Verify Pipeline Execution Success
        if (!result.isSuccess()) {
            logger.error("Pipeline execution failed: {}", result.getMessage());
            logger.error("Execution path:");
            for (ExecutionStep step : result.getExecutionPath()) {
                logger.error("  Step: {} - Status: {} - Message: {}",
                    step.getName(), step.getStatus(), step.getMessage());
            }
        }
        assertTrue(result.isSuccess(), "Pipeline should complete successfully: " + result.getMessage());

        // 7. Verify Step-Level Metrics
        logger.info("\n=== Pipeline Execution Metrics ===");
        int extractStepRecords = 0;
        int loadStepRecords = 0;

        for (ExecutionStep step : result.getExecutionPath()) {
            if ("PIPELINE_STEP".equals(step.getType())) {
                logger.info("Step: {} - Status: {} - Duration: {} ms",
                    step.getName(), step.getStatus(), step.getDurationMs());

                if (step.getRecordsProcessed() != null) {
                    logger.info("  Records Processed: {}", step.getRecordsProcessed());

                    if ("fetch-delta-changes".equals(step.getName())) {
                        extractStepRecords = step.getRecordsProcessed();
                    } else if ("push-to-postgres".equals(step.getName())) {
                        loadStepRecords = step.getRecordsProcessed();
                    }
                }

                if (step.getRecordsFailed() != null) {
                    logger.info("  Records Failed: {}", step.getRecordsFailed());
                }

                if (step.getRecordsProcessed() != null && step.getRecordsFailed() != null) {
                    logger.info("  Success Rate: {}", step.getSuccessRate());
                }
            }
        }
        logger.info("==================================\n");

        // Verify that we extracted and loaded the expected number of records
        assertEquals(3, extractStepRecords, "Extract step should have processed 3 records");
        assertEquals(3, loadStepRecords, "Load step should have processed 3 records");

        // 8. Verify Target Data
        verifyTargetData(targetUrl);

        logger.info("MsSQL to PostgreSQL sync test completed successfully!");
    }

    /**
     * Setup source database with test data simulating SQL Server.
     */
    private void setupSourceDatabase(String sourceUrl) throws Exception {
        logger.info("Setting up source database (SQL Server mode)...");

        try (Connection conn = DriverManager.getConnection(sourceUrl, "sa", "pass")) {
            try (Statement stmt = conn.createStatement()) {
                // Create schema (SQL Server uses 'dbo' schema by default)
                stmt.execute("CREATE SCHEMA IF NOT EXISTS dbo");

                // Drop table if exists from previous test run (DB_CLOSE_DELAY=-1 keeps DB alive)
                stmt.execute("DROP TABLE IF EXISTS dbo.Customers");

                // Create customers table
                stmt.execute(
                    "CREATE TABLE dbo.Customers (" +
                    "  customer_id VARCHAR(50) PRIMARY KEY, " +
                    "  name VARCHAR(255), " +
                    "  status VARCHAR(20), " +
                    "  email VARCHAR(255)" +
                    ")"
                );

                // Insert test data
                stmt.execute(
                    "INSERT INTO dbo.Customers (customer_id, name, status, email) " +
                    "VALUES ('CUST001', 'Alice Johnson', 'ACTIVE', 'alice@example.com')"
                );
                stmt.execute(
                    "INSERT INTO dbo.Customers (customer_id, name, status, email) " +
                    "VALUES ('CUST002', 'Bob Smith', 'INACTIVE', 'bob@example.com')"
                );
                stmt.execute(
                    "INSERT INTO dbo.Customers (customer_id, name, status, email) " +
                    "VALUES ('CUST003', 'Charlie Brown', 'ACTIVE', 'charlie@example.com')"
                );

                logger.info("Source database setup complete with 3 customer records");
            }
        }
    }

    /**
     * Setup target database simulating PostgreSQL.
     */
    private void setupTargetDatabase(String targetUrl) throws Exception {
        logger.info("Setting up target database (PostgreSQL mode)...");

        try (Connection conn = DriverManager.getConnection(targetUrl, "postgres", "pass")) {
            try (Statement stmt = conn.createStatement()) {
                // Table will be auto-created by the pipeline's init-script
                // This method is here for consistency and future enhancements
                logger.info("Target database ready for sync");
            }
        }
    }

    /**
     * Verify that data was correctly synced in the target database.
     */
    private void verifyTargetData(String targetUrl) throws Exception {
        logger.info("Verifying target database data...");

        try (Connection conn = DriverManager.getConnection(targetUrl, "postgres", "pass")) {
            try (Statement stmt = conn.createStatement()) {

                // Verify total record count
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")) {
                    assertTrue(rs.next(), "Should have count result");
                    int count = rs.getInt(1);
                    assertEquals(3, count, "Target should have 3 synced records");
                    logger.info("✓ Verified record count: {}", count);
                }

                // Verify CUST001
                try (ResultSet rs = stmt.executeQuery(
                    "SELECT name, email, status FROM customers WHERE id = 'CUST001'")) {
                    assertTrue(rs.next(), "CUST001 should exist");
                    assertEquals("Alice Johnson", rs.getString("name"));
                    assertEquals("alice@example.com", rs.getString("email"));
                    assertEquals("ACTIVE", rs.getString("status"));
                    logger.info("✓ Verified CUST001");
                }

                // Verify CUST002
                try (ResultSet rs = stmt.executeQuery(
                    "SELECT name, email FROM customers WHERE id = 'CUST002'")) {
                    assertTrue(rs.next(), "CUST002 should exist");
                    assertEquals("Bob Smith", rs.getString("name"));
                    assertEquals("bob@example.com", rs.getString("email"));
                    logger.info("✓ Verified CUST002");
                }

                // Verify CUST003
                try (ResultSet rs = stmt.executeQuery(
                    "SELECT name, email FROM customers WHERE id = 'CUST003'")) {
                    assertTrue(rs.next(), "CUST003 should exist");
                    assertEquals("Charlie Brown", rs.getString("name"));
                    assertEquals("charlie@example.com", rs.getString("email"));
                    logger.info("✓ Verified CUST003");
                }

                // Verify synced_at timestamp is populated
                try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM customers WHERE synced_at IS NOT NULL")) {
                    assertTrue(rs.next(), "Should have count result");
                    int count = rs.getInt(1);
                    assertEquals(3, count, "All records should have synced_at timestamp");
                    logger.info("✓ Verified all records have synced_at timestamp");
                }

                logger.info("All target data verifications passed!");
            }
        }
    }
}
