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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests database sync using H2 compatibility modes to simulate SQL Server to PostgreSQL.
 * Uses H2 in-memory databases without requiring actual database infrastructure.
 *
 * <p><b>CRITICAL VALIDATION CHECKLIST:</b></p>
 * <ul>
 *   <li>✅ Extends SyncTestBase (provides APEX service setup/teardown)</li>
 *   <li>✅ Uses ColoredTestOutputExtension (via SyncTestBase)</li>
 *   <li>✅ Validates extract step - records from source database</li>
 *   <li>✅ Validates load step - records to target database</li>
 *   <li>✅ Verifies target data integrity</li>
 *   <li>✅ Proper cleanup of test resources</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class SyncPipelineH2Test extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(SyncPipelineH2Test.class);
    
    private RulesEngine rulesEngine;
    private String sourceUrl;
    private String targetUrl;

    @BeforeEach
    public void setUpTestData() throws Exception {
        // Setup source database (H2 in SQL Server mode)
        sourceUrl = "jdbc:h2:mem:source_sqlserver;MODE=MSSQLServer;DB_CLOSE_DELAY=-1";
        try (Connection conn = DriverManager.getConnection(sourceUrl, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE customers (id INT PRIMARY KEY, name VARCHAR(255))");
                stmt.execute("INSERT INTO customers (id, name) VALUES (1, 'Alice from H2 SQL Server')");
                stmt.execute("INSERT INTO customers (id, name) VALUES (2, 'Bob from H2 SQL Server')");
                stmt.execute("INSERT INTO customers (id, name) VALUES (3, 'Charlie from H2 SQL Server')");
            }
        }
        logger.info("Created and populated source database (H2 SQL Server mode)");

        // Setup target database (H2 in PostgreSQL mode)
        targetUrl = "jdbc:h2:mem:target_postgres;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        try (Connection conn = DriverManager.getConnection(targetUrl, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS customers (id INT PRIMARY KEY, name VARCHAR(255))");
            }
        }
        logger.info("Created target database (H2 PostgreSQL mode)");
    }

    @AfterEach
    public void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    @DisplayName("Should sync data using H2 compatibility modes")
    public void shouldSyncUsingH2Modes() throws Exception {
        // Load configuration from Java test directory (APEX naming convention)
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/pipeline/SyncPipelineH2Test.yaml");
        assertNotNull(rulesEngine, "RulesEngine should be initialized");

        // Execute the pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertNotNull(result, "RuleResult should not be null");

        // Validate execution
        logger.info("Pipeline execution completed");
        logger.info("Overall status: {}", result.isSuccess() ? "SUCCESS" : "FAILURE");
        
        if (!result.isSuccess()) {
            logger.error("Pipeline failed: {}", result.getMessage());
            for (ExecutionStep step : result.getExecutionPath()) {
                logger.error("  Step: {} - Status: {} - Message: {}", 
                    step.getName(), step.getStatus(), step.getMessage());
            }
        }
        
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Verify pipeline steps
        List<ExecutionStep> pipelineSteps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertTrue(pipelineSteps.size() >= 2, "Should have at least 2 steps (extract + load)");

        // Verify extract step
        ExecutionStep extractStep = pipelineSteps.stream()
            .filter(step -> step.getName().contains("extract"))
            .findFirst()
            .orElse(null);
        assertNotNull(extractStep, "Should have extract step");
        if (extractStep.getRecordsProcessed() != null) {
            assertEquals(3, extractStep.getRecordsProcessed(), "Extract step should process 3 records");
        }

        // Verify load/transform step
        ExecutionStep loadStep = pipelineSteps.stream()
            .filter(step -> step.getName().contains("load") || step.getName().contains("transform"))
            .findFirst()
            .orElse(null);
        assertNotNull(loadStep, "Should have load/transform step");
        if (loadStep.getRecordsProcessed() != null) {
            assertEquals(3, loadStep.getRecordsProcessed(), "Load step should process 3 records");
        }

        // Verify target data
        try (Connection conn = DriverManager.getConnection(targetUrl, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")) {
                    assertTrue(rs.next(), "Should have result");
                    int count = rs.getInt(1);
                    assertEquals(3, count, "Target should have 3 synced records");
                }

                try (ResultSet rs = stmt.executeQuery("SELECT name FROM customers WHERE id = 1")) {
                    assertTrue(rs.next(), "Record 1 should exist");
                    String name = rs.getString(1);
                    assertTrue(name.contains("Alice"), "Data should be synced from source");
                }
            }
        }

        logger.info("H2 compatibility mode sync completed successfully");
    }
}
