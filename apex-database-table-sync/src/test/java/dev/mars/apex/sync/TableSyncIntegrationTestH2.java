package dev.mars.apex.sync;

import dev.mars.apex.core.config.yaml.RulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for Table Sync simulating SQL Server to PostgreSQL flow.
 * Leverages H2 compatibility modes for a Zero-Custom, Zero-Infrastructure test.
 */
public class TableSyncIntegrationTestH2 {

    @Test
    public void testSimulatedMSSqlToPostgresSync() throws Exception {
        // 1. Resolve Test YAML
        URL resource = getClass().getClassLoader().getResource("test-sync-pipeline-h2.yaml");
        assertNotNull(resource, "test-sync-pipeline-h2.yaml not found");
        String configPath = new File(resource.toURI()).getAbsolutePath();

        // 2. Setup Source Data (Simulating SQL Server via H2 MODE=MSSQLServer)
        // Note: The YAML file has hardcoded connection details, we just need to ensure the DB exists and has data.
        String sourceUrl = "jdbc:h2:mem:source_sqlserver;MODE=MSSQLServer;DB_CLOSE_DELAY=-1";
        
        try (Connection conn = DriverManager.getConnection(sourceUrl, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS customers (id INT PRIMARY KEY, name VARCHAR(255))");
                stmt.execute("INSERT INTO customers (id, name) VALUES (1, 'Alice from SQLServer')");
                stmt.execute("INSERT INTO customers (id, name) VALUES (2, 'Bob from SQLServer')");
            }
        }

        // 3. Run Sync via APEX Core
        RulesEngine engine = RulesEngine.fromFile(configPath);
        RuleResult result = engine.evaluate(new HashMap<>());

        // 4. Verify Result
        assertTrue(result.isSuccess(), "Sync failed: " + result.getMessage());

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
    }
}
