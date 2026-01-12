package dev.mars.apex.sync;

import dev.mars.apex.core.config.yaml.RulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
 * Integration test for Table Sync using real databases via TestContainers.
 * Uses a dedicated yaml file for container configuration.
 */
@Testcontainers
public class TableSyncIntegrationTestContainers {

    @Container
    private static final MSSQLServerContainer<?> sqlServer = new MSSQLServerContainer<>(TestConstants.MSSQL_IMAGE)
            .acceptLicense();

    @Container
    private static final PostgreSQLContainer<?> postgreSQL = new PostgreSQLContainer<>(TestConstants.POSTGRES_IMAGE);

    @Test
    public void testRealMSSqlToPostgresSync() throws Exception {
        // 1. Resolve Test YAML
        URL resource = getClass().getClassLoader().getResource("test-sync-pipeline-containers.yaml");
        assertNotNull(resource, "test-sync-pipeline-containers.yaml not found");
        String configPath = new File(resource.toURI()).getAbsolutePath();

        // 2. Setup System Properties to override YAML defaults
        System.setProperty("SOURCE_DB_URL", sqlServer.getJdbcUrl());
        System.setProperty("SOURCE_DB_USER", sqlServer.getUsername());
        System.setProperty("SOURCE_DB_PASS", sqlServer.getPassword());
        
        System.setProperty("TARGET_DB_URL", postgreSQL.getJdbcUrl());
        System.setProperty("TARGET_DB_USER", postgreSQL.getUsername());
        System.setProperty("TARGET_DB_PASS", postgreSQL.getPassword());

        // 3. Setup Source Data in SQL Server Container
        try (Connection conn = DriverManager.getConnection(sqlServer.getJdbcUrl(), sqlServer.getUsername(), sqlServer.getPassword())) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE customers (id INT PRIMARY KEY, name VARCHAR(255))");
                stmt.execute("INSERT INTO customers (id, name) VALUES (1, 'Alice from Real SQLServer')");
                stmt.execute("INSERT INTO customers (id, name) VALUES (2, 'Bob from Real SQLServer')");
            }
        }

        // 4. Setup Target Table in PostgreSQL Container
        try (Connection conn = DriverManager.getConnection(postgreSQL.getJdbcUrl(), postgreSQL.getUsername(), postgreSQL.getPassword())) {
            try (Statement stmt = conn.createStatement()) {
                // Initialize target table (sync might do this if auto-create is on, but ensuring for clarity)
                stmt.execute("CREATE TABLE IF NOT EXISTS customers (id INT PRIMARY KEY, name VARCHAR(255))");
            }
        }

        // 5. Run Sync via APEX Core
        RulesEngine engine = RulesEngine.fromFile(configPath);
        RuleResult result = engine.evaluate(new HashMap<>());

        // 6. Verify Result
        assertTrue(result.isSuccess(), "Sync failed: " + result.getMessage());

        // 7. Verify Target Data in PostgreSQL Container
        try (Connection conn = DriverManager.getConnection(postgreSQL.getJdbcUrl(), postgreSQL.getUsername(), postgreSQL.getPassword())) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT name FROM customers WHERE id = 1")) {
                    assertTrue(rs.next(), "Record 1 not found in target");
                    String name = rs.getString(1);
                    assertTrue(name.contains("Real SQLServer"), "Data should have been copied from Real SQL Server source");
                }

                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")) {
                    assertTrue(rs.next());
                    int count = rs.getInt(1);
                    assertTrue(count >= 2, "Target PostgreSQL table should have synced records");
                }
            }
        }
        
        // Cleanup System properties
        System.clearProperty("SOURCE_DB_URL");
        System.clearProperty("SOURCE_DB_USER");
        System.clearProperty("SOURCE_DB_PASS");
        System.clearProperty("TARGET_DB_URL");
        System.clearProperty("TARGET_DB_USER");
        System.clearProperty("TARGET_DB_PASS");
    }
}
