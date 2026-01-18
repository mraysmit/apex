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

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.mars.apex.sync.TestContainerImages;

/**
 * Integration test for Table Sync using real databases via TestContainers.
 * Uses a dedicated yaml file for container configuration.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@Testcontainers
public class TableSyncIntegrationTestContainers {

    @Container
    private static final MSSQLServerContainer<?> sqlServer = new MSSQLServerContainer<>(TestContainerImages.MSSQL_SERVER)
            .acceptLicense();

    @Container
    private static final PostgreSQLContainer<?> postgreSQL = new PostgreSQLContainer<>(TestContainerImages.POSTGRES);

    @Test
    public void testRealMSSqlToPostgresSync() throws Exception {
        // 1. Setup System Properties to override YAML defaults
        System.setProperty("SOURCE_DB_URL", sqlServer.getJdbcUrl());
        System.setProperty("SOURCE_DB_USER", sqlServer.getUsername());
        System.setProperty("SOURCE_DB_PASS", sqlServer.getPassword());

        System.setProperty("TARGET_DB_URL", postgreSQL.getJdbcUrl());
        System.setProperty("TARGET_DB_USER", postgreSQL.getUsername());
        System.setProperty("TARGET_DB_PASS", postgreSQL.getPassword());

        // 2. Setup Source Data in SQL Server Container
        try (Connection conn = DriverManager.getConnection(sqlServer.getJdbcUrl(), sqlServer.getUsername(), sqlServer.getPassword())) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE customers (id INT PRIMARY KEY, name VARCHAR(255))");
                stmt.execute("INSERT INTO customers (id, name) VALUES (1, 'Alice from Real SQLServer')");
                stmt.execute("INSERT INTO customers (id, name) VALUES (2, 'Bob from Real SQLServer')");
            }
        }

        // 3. Setup Target Table in PostgreSQL Container
        try (Connection conn = DriverManager.getConnection(postgreSQL.getJdbcUrl(), postgreSQL.getUsername(), postgreSQL.getPassword())) {
            try (Statement stmt = conn.createStatement()) {
                // Initialize target table (sync might do this if auto-create is on, but ensuring for clarity)
                stmt.execute("CREATE TABLE IF NOT EXISTS customers (id INT PRIMARY KEY, name VARCHAR(255))");
            }
        }

        // 4. Run Sync via APEX Core (using classpath resource)
        RulesEngine engine = RulesEngine.fromClasspath("test-sync-pipeline-containers.yaml");
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
