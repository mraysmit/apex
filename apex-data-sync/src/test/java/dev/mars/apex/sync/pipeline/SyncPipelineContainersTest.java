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
import dev.mars.apex.core.config.model.YamlDataSource;
import dev.mars.apex.core.config.model.YamlDataSink;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.ExecutionStep;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import dev.mars.apex.sync.TestContainerImages;

/**
 * Integration test for Table Sync using real SQL Server and PostgreSQL via TestContainers.
 * Tests complete ETL pipeline with extract, transform, and load steps.
 *
 * <p>This test follows the proven pattern from apex-demo PostgreSQLSimpleLookupTest:</p>
 * <ol>
 *   <li>Load YAML configuration</li>
 *   <li>Programmatically update connection details with TestContainers values</li>
 *   <li>Execute pipeline</li>
 * </ol>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SyncPipelineContainersTest {

    private static final Logger logger = LoggerFactory.getLogger(SyncPipelineContainersTest.class);
    
    private static GenericContainer<?> sqlServer;
    private static GenericContainer<?> postgres;
    private final YamlConfigurationLoader yamlLoader = new YamlConfigurationLoader();

    @BeforeAll
    static void checkDockerAndStartContainers() {
        // Check Docker availability BEFORE attempting to start containers
        try {
            DockerClientFactory.instance().client();
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                "Docker is not available. Skipping TestContainers integration tests. " +
                "To run these tests, ensure Docker is installed and running. Error: " + e.getMessage());
            return;
        }
        
        // Start containers only if Docker is available
        try {
            sqlServer = new GenericContainer<>(DockerImageName.parse(TestContainerImages.MSSQL_SERVER)
                    .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server"))
                    .withEnv("ACCEPT_EULA", "Y")
                    .withEnv("SA_PASSWORD", "YourStrong@Passw0rd")
                    .withExposedPorts(1433)
                    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));
            sqlServer.start();
            
            postgres = new GenericContainer<>(DockerImageName.parse(TestContainerImages.POSTGRES)
                    .asCompatibleSubstituteFor("postgres"))
                    .withEnv("POSTGRES_DB", "test")
                    .withEnv("POSTGRES_USER", "test")
                    .withEnv("POSTGRES_PASSWORD", "test")
                    .withExposedPorts(5432)
                    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));
            postgres.start();
            
            // Set system properties for YAML placeholders
            System.setProperty("SOURCE_DB_HOST", sqlServer.getHost());
            System.setProperty("SOURCE_DB_PORT", String.valueOf(sqlServer.getMappedPort(1433)));
            System.setProperty("SOURCE_DB_NAME", "master");
            System.setProperty("SOURCE_DB_USER", "sa");
            System.setProperty("SOURCE_DB_PASS", "YourStrong@Passw0rd");
            
            System.setProperty("TARGET_DB_HOST", postgres.getHost());
            System.setProperty("TARGET_DB_PORT", String.valueOf(postgres.getMappedPort(5432)));
            System.setProperty("TARGET_DB_NAME", "test");
            System.setProperty("TARGET_DB_USER", "test");
            System.setProperty("TARGET_DB_PASS", "test");
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                "Failed to start containers. Skipping test. Error: " + e.getMessage());
        }
    }
    
    @AfterAll
    static void stopContainers() {
        if (sqlServer != null && sqlServer.isRunning()) {
            sqlServer.stop();
        }
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should sync data from SQL Server to PostgreSQL using TestContainers")
    public void shouldSyncFromSqlServerToPostgres() throws Exception {
        logger.info("=== Starting SQL Server to PostgreSQL Sync Test (TestContainers) ===");

        // Setup source data in SQL Server
        String sqlServerUrl = "jdbc:sqlserver://" + sqlServer.getHost() + ":" 
            + sqlServer.getMappedPort(1433) + ";encrypt=false;trustServerCertificate=true";
        try (Connection conn = DriverManager.getConnection(sqlServerUrl, "sa", "YourStrong@Passw0rd")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE customers (id INT PRIMARY KEY, name VARCHAR(255))");
                stmt.execute("INSERT INTO customers (id, name) VALUES (1, 'Alice from SQL Server')");
                stmt.execute("INSERT INTO customers (id, name) VALUES (2, 'Bob from SQL Server')");
                stmt.execute("INSERT INTO customers (id, name) VALUES (3, 'Charlie from SQL Server')");
            }
        }
        logger.info("Created and populated SQL Server source table");

        // Setup target table in PostgreSQL
        String postgresUrl = "jdbc:postgresql://" + postgres.getHost() + ":" 
            + postgres.getMappedPort(5432) + "/test";
        try (Connection conn = DriverManager.getConnection(postgresUrl, "test", "test")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS customers (id INT PRIMARY KEY, name VARCHAR(255))");
            }
        }
        logger.info("Created PostgreSQL target table");

        // Load configuration from Java test directory (APEX naming convention)
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/sync/pipeline/SyncPipelineContainersTest.yaml");
        
        // Programmatically update connection details with TestContainers values
        // This is the proven pattern from apex-demo PostgreSQLSimpleLookupTest
        updateDataSourceConnection(config, "sqlserver-source", 
            sqlServer.getHost(), sqlServer.getMappedPort(1433), "master",
            "sa", "YourStrong@Passw0rd");
        
        updateDataSinkConnection(config, "postgresql-target",
            postgres.getHost(), postgres.getMappedPort(5432), "test",
            "test", "test");
        
        // Create RulesEngine from updated config
        RulesEngine rulesEngine = RulesEngine.fromYamlConfig(config);
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

        // Verify load step
        ExecutionStep loadStep = pipelineSteps.stream()
            .filter(step -> step.getName().contains("load") || step.getName().contains("transform"))
            .findFirst()
            .orElse(null);
        assertNotNull(loadStep, "Should have load/transform step");

        // Verify target data in PostgreSQL
        try (Connection conn = DriverManager.getConnection(postgresUrl, "test", "test")) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")) {
                    assertTrue(rs.next(), "Should have result");
                    int count = rs.getInt(1);
                    assertEquals(3, count, "Target should have 3 synced records");
                }

                try (ResultSet rs = stmt.executeQuery("SELECT name FROM customers WHERE id = 1")) {
                    assertTrue(rs.next(), "Record 1 should exist");
                    String name = rs.getString(1);
                    assertTrue(name.contains("Alice"), "Data should be synced from SQL Server");
                }
            }
        }

        logger.info("SQL Server to PostgreSQL sync completed successfully");
        rulesEngine.shutdown();
    }
    
    /**
     * Update data source connection details with TestContainers values.
     * This is the proven pattern from apex-demo PostgreSQLSimpleLookupTest.
     */
    private void updateDataSourceConnection(YamlRuleConfiguration config, String dataSourceName,
            String host, Integer port, String database, String username, String password) {
        
        logger.info("Updating data source '{}' connection details:", dataSourceName);
        logger.info("  Host: {}", host);
        logger.info("  Port: {}", port);
        logger.info("  Database: {}", database);
        logger.info("  Username: {}", username);
        
        if (config.getDataSources() != null) {
            for (YamlDataSource dataSource : config.getDataSources()) {
                if (dataSourceName.equals(dataSource.getName())) {
                    Map<String, Object> connection = dataSource.getConnection();
                    connection.put("host", host);
                    connection.put("port", port);
                    connection.put("database", database);
                    connection.put("username", username);
                    connection.put("password", password);
                    logger.info("Updated data source '{}' with TestContainers connection details", dataSourceName);
                    return;
                }
            }
        }
        logger.warn("Data source '{}' not found in configuration", dataSourceName);
    }
    
    /**
     * Update data sink connection details with TestContainers values.
     * This is the proven pattern from apex-demo PostgreSQLSimpleLookupTest.
     */
    private void updateDataSinkConnection(YamlRuleConfiguration config, String dataSinkName,
            String host, Integer port, String database, String username, String password) {
        
        logger.info("Updating data sink '{}' connection details:", dataSinkName);
        logger.info("  Host: {}", host);
        logger.info("  Port: {}", port);
        logger.info("  Database: {}", database);
        logger.info("  Username: {}", username);
        
        if (config.getDataSinks() != null) {
            for (YamlDataSink dataSink : config.getDataSinks()) {
                if (dataSinkName.equals(dataSink.getName())) {
                    Map<String, Object> connection = dataSink.getConnection();
                    connection.put("host", host);
                    connection.put("port", port);
                    connection.put("database", database);
                    connection.put("username", username);
                    connection.put("password", password);
                    logger.info("Updated data sink '{}' with TestContainers connection details", dataSinkName);
                    return;
                }
            }
        }
        logger.warn("Data sink '{}' not found in configuration", dataSinkName);
    }
}
