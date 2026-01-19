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

package dev.mars.apex.sync;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Integration test for Table Sync simulating SQL Server to PostgreSQL flow.
 * Leverages H2 compatibility modes for a Zero-Custom, Zero-Infrastructure test.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class TableSyncIntegrationTestH2 {

    @Test
    public void testSimulatedMSSqlToPostgresSync() throws Exception {
        // 1. Setup Source Data (Simulating SQL Server via H2 MODE=MSSQLServer)
        // Note: The YAML file has hardcoded connection details, we just need to ensure the DB exists and has data.
        String sourceUrl = "jdbc:h2:mem:source_sqlserver;MODE=MSSQLServer;DB_CLOSE_DELAY=-1";

        try (Connection conn = DriverManager.getConnection(sourceUrl, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS customers (id INT PRIMARY KEY, name VARCHAR(255))");
                stmt.execute("INSERT INTO customers (id, name) VALUES (1, 'Alice from SQLServer')");
                stmt.execute("INSERT INTO customers (id, name) VALUES (2, 'Bob from SQLServer')");
            }
        }

        // 2. Run Sync via APEX Core (using YAML file)
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration yamlConfig = loader.loadFromFile("src/test/java/dev/mars/apex/sync/pipeline/SyncPipelineH2Test.yaml");
        RulesEngine engine = RulesEngine.fromYamlConfig(yamlConfig);
        RuleResult result = engine.evaluate(new HashMap<>());

        // 4. Verify Result
        assertTrue(result.isSuccess(), "Sync failed: " + result.getMessage());

        // 4a. Verify Step-Level Metrics
        System.out.println("\n=== Pipeline Execution Metrics ===");
        int extractStepRecords = 0;
        int loadStepRecords = 0;

        for (ExecutionStep step : result.getExecutionPath()) {
            if ("PIPELINE_STEP".equals(step.getType())) {
                System.out.printf("Step: %s - Status: %s - Duration: %d ms%n",
                    step.getName(), step.getStatus(), step.getDurationMs());

                if (step.getRecordsProcessed() != null) {
                    System.out.printf("  Records Processed: %d%n", step.getRecordsProcessed());

                    if ("extract-from-sqlserver".equals(step.getName())) {
                        extractStepRecords = step.getRecordsProcessed();
                    } else if ("load-into-postgresql".equals(step.getName())) {
                        loadStepRecords = step.getRecordsProcessed();
                    }
                }

                if (step.getRecordsFailed() != null) {
                    System.out.printf("  Records Failed: %d%n", step.getRecordsFailed());
                }

                if (step.getRecordsProcessed() != null && step.getRecordsFailed() != null) {
                    System.out.printf("  Success Rate: %.2f%%%n", step.getSuccessRate());
                }
            }
        }
        System.out.println("==================================\n");

        // Verify that we extracted and loaded the expected number of records
        assertEquals(2, extractStepRecords, "Extract step should have processed 2 records");
        assertEquals(2, loadStepRecords, "Load step should have processed 2 records");

        // 5. Verify Target Data (Simulating PostgreSQL via H2 MODE=PostgreSQL)
        String targetUrl = "jdbc:h2:mem:target_postgres;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

        try (Connection conn = DriverManager.getConnection(targetUrl, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT name FROM customers WHERE id = 1")) {
                    assertTrue(rs.next());
                    String name = rs.getString(1);
                    assertTrue(name.contains("SQLServer"), "Data should have been copied from 'SQL Server' source");
                }

                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")) {
                    assertTrue(rs.next());
                    int count = rs.getInt(1);
                    assertTrue(count >= 2, "Target PostgreSQL-mode table should have synced records");
                }
            }
        }

        // 6. Cleanup
        engine.shutdown();
    }
}
