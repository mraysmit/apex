package dev.mars.apex.core.service.data.external.database;

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

import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for H2 database connection strings as provided by YAML configurations.
 * 
 * This test verifies that various H2 database connection formats work correctly:
 * - File-based databases with relative paths
 * - File-based databases with absolute paths  
 * - In-memory databases
 * - H2 with custom parameters
 * - H2 with different compatibility modes
 * - H2 TCP server connections
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
class H2ConnectionStringTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(H2ConnectionStringTest.class);
    
    @TempDir
    Path tempDir;
    
    // ========================================
    // File-based H2 Database Tests
    // ========================================
    
    @Test
    @DisplayName("Should connect to H2 file-based database with relative path")
    void testH2FileBasedRelativePath() throws Exception {
        LOGGER.info("Testing H2 file-based database with relative path");
        
        String yamlContent = """
            metadata:
              name: "H2 File-based Relative Path Test"
              version: "1.0.0"
            
            data-sources:
              - name: "h2-file-relative"
                type: "database"
                source-type: "h2"
                enabled: true
                description: "H2 file-based database with relative path"
                
                connection:
                  database: "./target/test-h2/relative_db"
                  username: "sa"
                  password: ""
                  mode: "PostgreSQL"
            """;
        
        testH2ConnectionFromYaml(yamlContent, "h2-file-relative");
    }
    
    @Test
    @DisplayName("Should connect to H2 file-based database with absolute path")
    void testH2FileBasedAbsolutePath() throws Exception {
        LOGGER.info("Testing H2 file-based database with absolute path");
        
        String absolutePath = tempDir.resolve("absolute_db").toString().replace("\\", "/");
        
        String yamlContent = String.format("""
            metadata:
              name: "H2 File-based Absolute Path Test"
              version: "1.0.0"
            
            data-sources:
              - name: "h2-file-absolute"
                type: "database"
                source-type: "h2"
                enabled: true
                description: "H2 file-based database with absolute path"
                
                connection:
                  database: "%s"
                  username: "sa"
                  password: ""
                  mode: "PostgreSQL"
            """, absolutePath);
        
        testH2ConnectionFromYaml(yamlContent, "h2-file-absolute");
    }
    
    // ========================================
    // In-memory H2 Database Tests
    // ========================================
    
    @Test
    @DisplayName("Should connect to H2 in-memory database")
    void testH2InMemoryDatabase() throws Exception {
        LOGGER.info("Testing H2 in-memory database");
        
        String yamlContent = """
            metadata:
              name: "H2 In-memory Test"
              version: "1.0.0"
            
            data-sources:
              - name: "h2-memory"
                type: "database"
                source-type: "h2"
                enabled: true
                description: "H2 in-memory database"
                
                connection:
                  database: "mem:testdb"
                  username: "sa"
                  password: ""
                  mode: "PostgreSQL"
            """;
        
        testH2ConnectionFromYaml(yamlContent, "h2-memory");
    }
    
    @Test
    @DisplayName("Should connect to H2 private in-memory database")
    void testH2PrivateInMemoryDatabase() throws Exception {
        LOGGER.info("Testing H2 private in-memory database");
        
        String yamlContent = """
            metadata:
              name: "H2 Private In-memory Test"
              version: "1.0.0"
            
            data-sources:
              - name: "h2-private-memory"
                type: "database"
                source-type: "h2"
                enabled: true
                description: "H2 private in-memory database"
                
                connection:
                  database: "mem"
                  username: "sa"
                  password: ""
                  mode: "PostgreSQL"
            """;
        
        testH2ConnectionFromYaml(yamlContent, "h2-private-memory");
    }
    
    // ========================================
    // H2 Custom Parameters Tests
    // ========================================
    
    @Test
    @DisplayName("Should connect to H2 with custom parameters")
    void testH2WithCustomParameters() throws Exception {
        LOGGER.info("Testing H2 with custom parameters");
        
        String yamlContent = """
            metadata:
              name: "H2 Custom Parameters Test"
              version: "1.0.0"
            
            data-sources:
              - name: "h2-custom-params"
                type: "database"
                source-type: "h2"
                enabled: true
                description: "H2 database with custom parameters"
                
                connection:
                  database: "./target/test-h2/custom_params;MODE=MySQL;TRACE_LEVEL_FILE=1;CACHE_SIZE=32768"
                  username: "sa"
                  password: ""
            """;
        
        testH2ConnectionFromYaml(yamlContent, "h2-custom-params");
    }
    
    @Test
    @DisplayName("Should connect to H2 in-memory with custom parameters")
    void testH2InMemoryWithCustomParameters() throws Exception {
        LOGGER.info("Testing H2 in-memory with custom parameters");
        
        String yamlContent = """
            metadata:
              name: "H2 In-memory Custom Parameters Test"
              version: "1.0.0"
            
            data-sources:
              - name: "h2-memory-custom"
                type: "database"
                source-type: "h2"
                enabled: true
                description: "H2 in-memory database with custom parameters"
                
                connection:
                  database: "mem:customtest;TRACE_LEVEL_SYSTEM_OUT=2;MODE=Oracle;CACHE_SIZE=16384"
                  username: "sa"
                  password: ""
            """;
        
        testH2ConnectionFromYaml(yamlContent, "h2-memory-custom");
    }
    
    // ========================================
    // H2 Compatibility Mode Tests
    // ========================================
    
    @Test
    @DisplayName("Should connect to H2 with MySQL compatibility mode")
    void testH2MySQLMode() throws Exception {
        LOGGER.info("Testing H2 with MySQL compatibility mode");
        
        String yamlContent = """
            metadata:
              name: "H2 MySQL Mode Test"
              version: "1.0.0"
            
            data-sources:
              - name: "h2-mysql-mode"
                type: "database"
                source-type: "h2"
                enabled: true
                description: "H2 database with MySQL compatibility mode"
                
                connection:
                  database: "./target/test-h2/mysql_mode"
                  username: "sa"
                  password: ""
                  mode: "MySQL"
            """;
        
        testH2ConnectionFromYaml(yamlContent, "h2-mysql-mode");
    }
    
    @Test
    @DisplayName("Should connect to H2 with Oracle compatibility mode")
    void testH2OracleMode() throws Exception {
        LOGGER.info("Testing H2 with Oracle compatibility mode");
        
        String yamlContent = """
            metadata:
              name: "H2 Oracle Mode Test"
              version: "1.0.0"
            
            data-sources:
              - name: "h2-oracle-mode"
                type: "database"
                source-type: "h2"
                enabled: true
                description: "H2 database with Oracle compatibility mode"
                
                connection:
                  database: "./target/test-h2/oracle_mode"
                  username: "sa"
                  password: ""
                  mode: "Oracle"
            """;
        
        testH2ConnectionFromYaml(yamlContent, "h2-oracle-mode");
    }

    // ========================================
    // H2 TCP Server Tests (Expected to Fail)
    // ========================================

    @Test
    @DisplayName("Should handle H2 TCP server connection configuration (expected to fail)")
    void testH2TcpServerConnection() throws Exception {
        LOGGER.info("Testing H2 TCP server connection (expected to fail - no server running)");

        String yamlContent = """
            metadata:
              name: "H2 TCP Server Test"
              version: "1.0.0"

            data-sources:
              - name: "h2-tcp-server"
                type: "database"
                source-type: "h2"
                enabled: true
                description: "H2 TCP server connection"

                connection:
                  host: "localhost"
                  port: 9092
                  database: "testdb"
                  username: "sa"
                  password: ""
                  mode: "PostgreSQL"
            """;

        // Load YAML configuration
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.loadFromStream(new ByteArrayInputStream(yamlContent.getBytes()));

        var yamlDataSource = config.getDataSources().stream()
            .filter(ds -> "h2-tcp-server".equals(ds.getName()))
            .findFirst()
            .orElseThrow();

        DataSourceConfiguration dataSourceConfig = yamlDataSource.toDataSourceConfiguration();

        // This should throw an exception since no H2 TCP server is running
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            JdbcTemplateFactory.createDataSource(dataSourceConfig);
        });

        LOGGER.info("✅ Expected exception for H2 TCP connection: {}", exception.getMessage());
        assertTrue(exception.getErrorType() == DataSourceException.ErrorType.CONNECTION_ERROR ||
                   exception.getErrorType() == DataSourceException.ErrorType.CONFIGURATION_ERROR,
                   "Should be connection or configuration error");
    }

    // ========================================
    // Edge Cases and Error Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle empty database path gracefully")
    void testH2EmptyDatabasePath() throws Exception {
        LOGGER.info("Testing H2 with empty database path");

        String yamlContent = """
            metadata:
              name: "H2 Empty Database Path Test"
              version: "1.0.0"

            data-sources:
              - name: "h2-empty-path"
                type: "database"
                source-type: "h2"
                enabled: true
                description: "H2 database with empty path"

                connection:
                  database: ""
                  username: "sa"
                  password: ""
                  mode: "PostgreSQL"
            """;

        // This should still work - APEX should handle empty database path
        testH2ConnectionFromYaml(yamlContent, "h2-empty-path");
    }

    @Test
    @DisplayName("Should handle null database path gracefully")
    void testH2NullDatabasePath() throws Exception {
        LOGGER.info("Testing H2 with null database path");

        String yamlContent = """
            metadata:
              name: "H2 Null Database Path Test"
              version: "1.0.0"

            data-sources:
              - name: "h2-null-path"
                type: "database"
                source-type: "h2"
                enabled: true
                description: "H2 database with null path"

                connection:
                  username: "sa"
                  password: ""
                  mode: "PostgreSQL"
            """;

        // This should still work - APEX should handle null database path
        testH2ConnectionFromYaml(yamlContent, "h2-null-path");
    }

    @Test
    @DisplayName("Should verify JDBC URL construction for various formats")
    void testJdbcUrlConstruction() throws Exception {
        LOGGER.info("Testing JDBC URL construction for various H2 formats");

        // Test different database path formats and verify the constructed JDBC URLs
        String[] testCases = {
            "./target/test-h2/simple_db",
            "./target/test-h2/with_params;MODE=MySQL;CACHE_SIZE=16384",
            "mem:testdb",
            "mem:testdb;TRACE_LEVEL_FILE=1",
            "mem",
            ""
        };

        for (String databasePath : testCases) {
            LOGGER.info("Testing database path: '{}'", databasePath);

            DataSourceConfiguration config = createH2Configuration();
            config.getConnection().setDatabase(databasePath);

            try {
                DataSource dataSource = JdbcTemplateFactory.createDataSource(config);
                assertNotNull(dataSource, "DataSource should be created for path: " + databasePath);

                // Test connection to verify URL is valid
                try (Connection connection = dataSource.getConnection()) {
                    String actualUrl = connection.getMetaData().getURL();
                    LOGGER.info("  ✅ Database path '{}' -> JDBC URL: {}", databasePath, actualUrl);

                    // Verify it's a valid H2 URL
                    assertTrue(actualUrl.startsWith("jdbc:h2:"), "Should be H2 JDBC URL");

                } catch (SQLException e) {
                    LOGGER.error("  X Failed to connect with database path: {}", databasePath, e);
                    throw e;
                }

            } catch (DataSourceException e) {
                LOGGER.error("  X Failed to create DataSource for database path: {}", databasePath, e);
                throw e;
            }
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Create a basic H2 configuration for testing.
     */
    private DataSourceConfiguration createH2Configuration() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-h2-db-" + System.nanoTime());
        config.setType("database");
        config.setSourceType("h2");
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setHost(null);
        connectionConfig.setDatabase("testdb_" + System.nanoTime());
        connectionConfig.setUsername("sa");
        connectionConfig.setPassword("");

        config.setConnection(connectionConfig);
        return config;
    }
    
    /**
     * Test H2 connection from YAML configuration.
     */
    private void testH2ConnectionFromYaml(String yamlContent, String dataSourceName) throws Exception {
        // Load YAML configuration
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.loadFromStream(new ByteArrayInputStream(yamlContent.getBytes()));
        
        assertNotNull(config, "Configuration should be loaded");
        assertNotNull(config.getDataSources(), "Data sources should be present");
        assertFalse(config.getDataSources().isEmpty(), "Data sources should not be empty");
        
        // Find the specific data source
        var yamlDataSource = config.getDataSources().stream()
            .filter(ds -> dataSourceName.equals(ds.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Data source '" + dataSourceName + "' not found"));
        
        // Convert to DataSourceConfiguration
        DataSourceConfiguration dataSourceConfig = yamlDataSource.toDataSourceConfiguration();
        
        assertNotNull(dataSourceConfig, "DataSourceConfiguration should be created");
        assertEquals("h2", dataSourceConfig.getSourceType(), "Source type should be h2");
        
        // Create DataSource and test connection
        DataSource dataSource = JdbcTemplateFactory.createDataSource(dataSourceConfig);
        assertNotNull(dataSource, "DataSource should be created");
        
        // Test actual database connection
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Connection should be established");
            assertFalse(connection.isClosed(), "Connection should be open");
            
            // Verify it's actually H2
            String productName = connection.getMetaData().getDatabaseProductName();
            assertTrue(productName.contains("H2"), "Should be H2 database, got: " + productName);
            
            // Test basic query execution
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery("SELECT 1 as test_value");
            assertTrue(resultSet.next(), "Query should return results");
            assertEquals(1, resultSet.getInt("test_value"), "Query should return expected value");
            
            // Log connection details for verification
            String url = connection.getMetaData().getURL();
            String version = connection.getMetaData().getDatabaseProductVersion();
            LOGGER.info("✅ Successfully connected to H2 database:");
            LOGGER.info("  Data Source: {}", dataSourceName);
            LOGGER.info("  JDBC URL: {}", url);
            LOGGER.info("  H2 Version: {}", version);
            LOGGER.info("  Connection Valid: {}", connection.isValid(5));
            
        } catch (SQLException e) {
            LOGGER.error("X Failed to connect to H2 database: {}", dataSourceName, e);
            throw e;
        }
    }
}
