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

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.mars.apex.sync.TestContainerImages;

/**
 * Integration test for Table Sync using real databases via TestContainers.
 * Uses a dedicated yaml file for container configuration.
 * 
 * <p>Updated to use the new GenericContainer pattern with:</p>
 * <ul>
 *   <li>GenericContainer instead of deprecated specialized containers</li>
 *   <li>DockerImageName with explicit compatibility declaration</li>
 *   <li>Dynamic port mapping and JDBC URL building</li>
 *   <li>Wait strategies for reliable container startup</li>
 *   <li>Connection retry logic for robustness</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@Testcontainers
public class TableSyncIntegrationTestContainers {
    private static final Logger logger = LoggerFactory.getLogger(TableSyncIntegrationTestContainers.class);

    private static final DockerImageName MSSQL_IMAGE = 
        DockerImageName.parse(TestContainerImages.MSSQL_SERVER)
                       .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server");

    private static final DockerImageName POSTGRES_IMAGE = 
        DockerImageName.parse(TestContainerImages.POSTGRES)
                       .asCompatibleSubstituteFor("postgres");

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> sqlServer = new GenericContainer<>(MSSQL_IMAGE)
            .withEnv("ACCEPT_EULA", "Y")
            .withEnv("SA_PASSWORD", "Passw0rd")
            .withEnv("MSSQL_PID", "Developer")
            .withExposedPorts(1433)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> postgreSQL = new GenericContainer<>(POSTGRES_IMAGE)
            .withEnv("POSTGRES_DB", "apex_sync_test")
            .withEnv("POSTGRES_USER", "postgres")
            .withEnv("POSTGRES_PASSWORD", "postgres")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());

    private static String getSqlServerJdbcUrl() {
        return "jdbc:sqlserver://" + sqlServer.getHost() + ":" 
            + sqlServer.getMappedPort(1433) + ";encrypt=false;trustServerCertificate=true";
    }

    private static String getPostgresJdbcUrl() {
        return "jdbc:postgresql://" + postgreSQL.getHost() + ":" 
            + postgreSQL.getMappedPort(5432) + "/apex_sync_test";
    }

    @Test
    public void testRealMSSqlToPostgresSync() throws Exception {
        logger.info("Starting SQL Server to PostgreSQL sync test");
        logger.info("SQL Server: {}:{}", sqlServer.getHost(), sqlServer.getMappedPort(1433));
        logger.info("PostgreSQL: {}:{}", postgreSQL.getHost(), postgreSQL.getMappedPort(5432));

        // 1. Setup System Properties to override YAML defaults
        System.setProperty("SOURCE_DB_HOST", sqlServer.getHost());
        System.setProperty("SOURCE_DB_PORT", String.valueOf(sqlServer.getMappedPort(1433)));
        System.setProperty("SOURCE_DB_NAME", "master");
        System.setProperty("SOURCE_DB_USER", "sa");
        System.setProperty("SOURCE_DB_PASS", "YourStrong@Passw0rd");

        System.setProperty("TARGET_DB_HOST", postgreSQL.getHost());
        System.setProperty("TARGET_DB_PORT", String.valueOf(postgreSQL.getMappedPort(5432)));
        System.setProperty("TARGET_DB_NAME", "apex_sync_test");
        System.setProperty("TARGET_DB_USER", "postgres");
        System.setProperty("TARGET_DB_PASS", "postgres");

        // 2. Setup Source Data in SQL Server Container with retry logic
        Connection sqlConn = null;
        int maxRetries = 5;
        int retryDelayMs = 2000;
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                sqlConn = DriverManager.getConnection(getSqlServerJdbcUrl(), "sa", "Passw0rd");
                logger.info("Successfully connected to SQL Server on attempt {}", i + 1);
                break;
            } catch (Exception e) {
                if (i < maxRetries - 1) {
                    logger.info("SQL Server connection attempt {} failed, retrying in {}ms...", i + 1, retryDelayMs);
                    Thread.sleep(retryDelayMs);
                } else {
                    throw new RuntimeException("Failed to connect to SQL Server after " + maxRetries + " attempts", e);
                }
            }
        }

        try (Connection conn = sqlConn) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE customers (id INT PRIMARY KEY, name VARCHAR(255))");
                stmt.execute("INSERT INTO customers (id, name) VALUES (1, 'Alice from Real SQLServer')");
                stmt.execute("INSERT INTO customers (id, name) VALUES (2, 'Bob from Real SQLServer')");
                logger.info("Created source table in SQL Server with test data");
            }
        }

        // 3. Setup Target Table in PostgreSQL Container with retry logic
        Connection pgConn = null;
        maxRetries = 3;
        retryDelayMs = 1000;
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                pgConn = DriverManager.getConnection(getPostgresJdbcUrl(), "postgres", "postgres");
                logger.info("Successfully connected to PostgreSQL on attempt {}", i + 1);
                break;
            } catch (Exception e) {
                if (i < maxRetries - 1) {
                    logger.info("PostgreSQL connection attempt {} failed, retrying in {}ms...", i + 1, retryDelayMs);
                    Thread.sleep(retryDelayMs);
                } else {
                    throw new RuntimeException("Failed to connect to PostgreSQL after " + maxRetries + " attempts", e);
                }
            }
        }

        try (Connection conn = pgConn) {
            try (Statement stmt = conn.createStatement()) {
                // Initialize target table (sync might do this if auto-create is on, but ensuring for clarity)
                stmt.execute("CREATE TABLE IF NOT EXISTS customers (id INT PRIMARY KEY, name VARCHAR(255))");
                logger.info("Created target table in PostgreSQL");
            }
        }

        // 4. Run Sync via APEX Core (using YAML file)
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration yamlConfig = loader.loadFromFile("src/test/java/dev/mars/apex/sync/pipeline/SyncPipelineContainersTest.yaml");
        RulesEngine engine = RulesEngine.fromYamlConfig(yamlConfig);
        RuleResult result = engine.evaluate(new HashMap<>());

        // 6. Verify Result
        assertTrue(result.isSuccess(), "Sync failed: " + result.getMessage());
        logger.info("Sync completed successfully");

        // 7. Verify Target Data in PostgreSQL Container
        try (Connection conn = DriverManager.getConnection(getPostgresJdbcUrl(), "postgres", "postgres")) {
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
                    logger.info("Verified {} records in target PostgreSQL table", count);
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
        
        logger.info("Test completed successfully");
    }
}
