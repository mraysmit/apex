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
 * Created: 2026-01-19
 */

package dev.mars.apex.sync.pipeline;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.sync.TestContainerImages;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.DockerClientFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Table Sync with Real SQL Server to Real PostgreSQL flow.
 * 
 * This test demonstrates:
 * - Extract data from REAL SQL Server database (via Testcontainers)
 * - Load data into REAL PostgreSQL database (via Testcontainers)
 * - Complete ETL pipeline execution with actual databases
 * - Validation of data transfer and cross-platform type mapping
 * 
 * Architecture:
 * - Source: Real SQL Server 2022 via Testcontainers
 * - Target: Real PostgreSQL 15 via Testcontainers
 * - Pipeline: Extract → Load with dependency management
 * 
 * Benefits:
 * - Tests real SQL Server and PostgreSQL behavior
 * - Validates actual cross-platform type mapping (NVARCHAR → VARCHAR)
 * - Catches database-specific issues (collation, encoding, etc.)
 * - Production-like validation without manual database setup
 * - Still fast with Testcontainers (auto-cleanup, no manual setup)
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@Testcontainers
@DisplayName("Table Sync: SQL Server → Real PostgreSQL")
class TableSyncIntegrationTestPostgres {

    @BeforeAll
    static void checkDockerAvailability() {
        try {
            DockerClientFactory.instance().client();
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                "Docker is not available. Skipping PostgreSQL integration test. " +
                "To run this test, ensure Docker is installed and running. Error: " + e.getMessage());
        }
    }

    private static final DockerImageName SQLSERVER_IMAGE = 
        DockerImageName.parse(TestContainerImages.MSSQL_SERVER)
            .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server");

    private static final DockerImageName POSTGRES_IMAGE = 
        DockerImageName.parse(TestContainerImages.POSTGRES)
            .asCompatibleSubstituteFor("postgres");

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> sqlServerDb = new GenericContainer<>(SQLSERVER_IMAGE)
            .withEnv("ACCEPT_EULA", "Y")
            .withEnv("SA_PASSWORD", "YourStrong!Passw0rd")
            .withEnv("MSSQL_PID", "Developer")
            .withExposedPorts(1433)
            .waitingFor(Wait.forLogMessage(".*SQL Server is now ready for client connections.*", 1));

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> postgresDb = new GenericContainer<>(POSTGRES_IMAGE)
            .withEnv("POSTGRES_DB", "target_postgres")
            .withEnv("POSTGRES_USER", "test")
            .withEnv("POSTGRES_PASSWORD", "test")
            .withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));

    @BeforeAll
    static void setupDatabases() throws Exception {
        // Set system properties for SQL Server source connection
        System.setProperty("SQLSERVER_SOURCE_HOST", sqlServerDb.getHost());
        System.setProperty("SQLSERVER_SOURCE_PORT", String.valueOf(sqlServerDb.getMappedPort(1433)));
        System.setProperty("SQLSERVER_SOURCE_USER", "sa");
        System.setProperty("SQLSERVER_SOURCE_PASS", "YourStrong!Passw0rd");

        // Set system properties for PostgreSQL target connection
        System.setProperty("POSTGRES_TARGET_HOST", postgresDb.getHost());
        System.setProperty("POSTGRES_TARGET_PORT", String.valueOf(postgresDb.getMappedPort(5432)));
        System.setProperty("POSTGRES_TARGET_DB", "target_postgres");
        System.setProperty("POSTGRES_TARGET_USER", "test");
        System.setProperty("POSTGRES_TARGET_PASS", "test");

        // Create source table in SQL Server
        String sourceUrl = String.format(
            "jdbc:sqlserver://%s:%d;databaseName=master;encrypt=false;trustServerCertificate=true",
            sqlServerDb.getHost(),
            sqlServerDb.getMappedPort(1433)
        );

        try (Connection conn = DriverManager.getConnection(sourceUrl, "sa", "YourStrong!Passw0rd");
             Statement stmt = conn.createStatement()) {
            stmt.execute("IF OBJECT_ID('customers', 'U') IS NOT NULL DROP TABLE customers");
            stmt.execute("CREATE TABLE customers (id INT PRIMARY KEY, name NVARCHAR(255))");
            stmt.execute("INSERT INTO customers (id, name) VALUES (1, 'Alice from SQL Server')");
            stmt.execute("INSERT INTO customers (id, name) VALUES (2, 'Bob from SQL Server')");
            System.out.println("[OK] Created and populated customers table in SQL Server");
        }

        // Create target table in PostgreSQL
        String targetUrl = String.format(
            "jdbc:postgresql://%s:%d/%s",
            postgresDb.getHost(),
            postgresDb.getMappedPort(5432),
            "target_postgres"
        );

        try (Connection conn = DriverManager.getConnection(targetUrl, "test", "test");
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS customers");
            stmt.execute("CREATE TABLE customers (id INT PRIMARY KEY, name VARCHAR(255))");
            System.out.println("[OK] Created customers table in PostgreSQL");
        }
    }

    @Test
    @DisplayName("Should sync data from real SQL Server to real PostgreSQL")
    void shouldSyncSqlServerToPostgres() throws Exception {
        // 1. Verify Source Data in SQL Server (already created in setupDatabases)
        String sourceUrl = String.format(
            "jdbc:sqlserver://%s:%d;databaseName=master;encrypt=false;trustServerCertificate=true",
            sqlServerDb.getHost(),
            sqlServerDb.getMappedPort(1433)
        );

        try (Connection conn = DriverManager.getConnection(sourceUrl, "sa", "YourStrong!Passw0rd");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")) {
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1), "SQL Server should have 2 source records");
            System.out.println("[OK] Verified SQL Server source data");
        }

        // 2. Run Sync via APEX Core (using co-located YAML file)
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration yamlConfig = loader.loadFromFile(
            "src/test/java/dev/mars/apex/sync/pipeline/TableSyncIntegrationTestPostgres.yaml"
        );
        RulesEngine engine = RulesEngine.fromYamlConfig(yamlConfig);
        RuleResult result = engine.evaluate(new HashMap<>());

        // 3. Verify Pipeline Result
        assertTrue(result.isSuccess(), "Sync pipeline failed: " + result.getMessage());

        // 4. Verify Step-Level Metrics
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

        // Verify extract and load record counts
        assertEquals(2, extractStepRecords, "Extract step should have processed 2 records");
        assertEquals(2, loadStepRecords, "Load step should have processed 2 records");

        // 5. Verify Target Data in Real PostgreSQL
        String targetUrl = String.format(
            "jdbc:postgresql://%s:%d/%s",
            postgresDb.getHost(),
            postgresDb.getMappedPort(5432),
            "target_postgres"
        );

        try (Connection conn = DriverManager.getConnection(targetUrl, "test", "test");
             Statement stmt = conn.createStatement()) {
            
            // Verify specific record
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM customers WHERE id = 1")) {
                assertTrue(rs.next(), "Record with id=1 should exist in PostgreSQL");
                String name = rs.getString(1);
                assertTrue(name.contains("SQL Server"), 
                    "Data should have been copied from SQL Server source, got: " + name);
                System.out.println("[OK] Verified record 1: " + name);
            }

            // Verify total count
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")) {
                assertTrue(rs.next());
                int count = rs.getInt(1);
                assertEquals(2, count, "PostgreSQL table should have 2 synced records");
                System.out.println("[OK] Verified record count: " + count);
            }

            // Verify all records
            try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM customers ORDER BY id")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt("id"));
                assertEquals("Alice from SQL Server", rs.getString("name"));
                
                assertTrue(rs.next());
                assertEquals(2, rs.getInt("id"));
                assertEquals("Bob from SQL Server", rs.getString("name"));
                
                assertFalse(rs.next(), "Should only have 2 records");
                System.out.println("[OK] All records verified successfully");
            }
        }

        // 6. Cleanup
        engine.shutdown();
        System.out.println("[OK] Pipeline completed successfully");
    }
}
