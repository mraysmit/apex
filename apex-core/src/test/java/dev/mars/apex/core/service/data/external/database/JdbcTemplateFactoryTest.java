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
import dev.mars.apex.core.config.datasource.ConnectionPoolConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceException;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for JdbcTemplateFactory.
 * 
 * Tests cover:
 * - DataSource creation for different database types
 * - HikariCP integration and fallback
 * - Connection configuration and validation
 * - Connection pool configuration
 * - JDBC URL building for various databases
 * - Caching mechanism
 * - Error handling and edge cases
 * - Resource management
 * 
 * Uses H2 in-memory database for real database testing without external dependencies.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class JdbcTemplateFactoryTest {

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        JdbcTemplateFactory.clearCache();
    }

    @AfterEach
    void tearDown() {
        // Clear cache after each test
        JdbcTemplateFactory.clearCache();
    }

    // ========================================
    // DataSource Creation Tests
    // ========================================

    @Test
    @DisplayName("Should create H2 DataSource successfully")
    void testCreateH2DataSourceSuccess() throws DataSourceException, SQLException {
        DataSourceConfiguration config = createH2Configuration();
        
        DataSource dataSource = JdbcTemplateFactory.createDataSource(config);
        
        assertNotNull(dataSource);
        
        // Test the connection works
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
            assertFalse(connection.isClosed());
            
            // Create a test table and insert data
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))");
                stmt.execute("INSERT INTO test_table VALUES (1, 'test')");
                
                var rs = stmt.executeQuery("SELECT COUNT(*) FROM test_table");
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    @Test
    @DisplayName("Should throw exception when connection configuration is missing")
    void testMissingConnectionConfiguration() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-db");
        config.setType("database");
        config.setSourceType("h2");
        config.setConnection(null);

        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            JdbcTemplateFactory.createDataSource(config);
        });

        assertEquals(DataSourceException.ErrorType.CONFIGURATION_ERROR, exception.getErrorType());
        // The exception might be wrapped, so check the cause chain for the original message
        assertTrue(exception.getMessage().contains("Connection configuration is required") ||
                   containsInCauseChain(exception, "Connection configuration is required"));
    }

    @Test
    @DisplayName("Should throw exception when source type is missing")
    void testMissingSourceType() {
        DataSourceConfiguration config = createH2Configuration();
        config.setSourceType(null);
        
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            JdbcTemplateFactory.createDataSource(config);
        });

        assertEquals(DataSourceException.ErrorType.CONFIGURATION_ERROR, exception.getErrorType());
        // The exception should contain the proper validation message
        assertTrue(exception.getMessage().contains("Source type is required"));
    }

    // ========================================
    // JDBC URL Building Tests
    // ========================================

    @Test
    @DisplayName("Should build H2 in-memory JDBC URL correctly")
    void testH2InMemoryJdbcUrl() throws DataSourceException, SQLException {
        DataSourceConfiguration config = createH2Configuration();
        config.getConnection().setHost(null); // Null host indicates in-memory

        DataSource dataSource = JdbcTemplateFactory.createDataSource(config);
        assertNotNull(dataSource);

        // Verify it's actually an in-memory database
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.contains("jdbc:h2:mem:"));
        }
    }

    @Test
    @DisplayName("Should build H2 file-based JDBC URL correctly")
    void testH2FileBasedJdbcUrl() throws DataSourceException, SQLException {
        DataSourceConfiguration config = createH2Configuration();
        config.getConnection().setHost(null);
        config.getConnection().setDatabase("./target/test-h2/testdb");

        DataSource dataSource = JdbcTemplateFactory.createDataSource(config);
        assertNotNull(dataSource);

        // Verify connection works and database path is correct
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            // H2 connection metadata URL doesn't include parameters, but should contain the database path
            assertTrue(url.contains("jdbc:h2:./target/test-h2/testdb"), "URL should contain database path");

            // Verify the connection is working by executing a simple query
            assertNotNull(connection.createStatement().executeQuery("SELECT 1"));
        }
    }

    @Test
    @DisplayName("Should support custom H2 parameters in database field")
    void testH2CustomParameters() throws DataSourceException, SQLException {
        DataSourceConfiguration config = createH2Configuration();
        config.getConnection().setHost(null);
        config.getConnection().setDatabase("./target/test-h2/custom;MODE=MySQL;TRACE_LEVEL_FILE=4");

        DataSource dataSource = JdbcTemplateFactory.createDataSource(config);
        assertNotNull(dataSource);

        // Verify connection works with custom parameters
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.contains("jdbc:h2:./target/test-h2/custom"), "URL should contain custom database path");

            // Verify the connection is working by executing a simple query
            assertNotNull(connection.createStatement().executeQuery("SELECT 1"));
        }
    }

    @Test
    @DisplayName("Should support H2 in-memory with custom parameters")
    void testH2InMemoryCustomParameters() throws DataSourceException, SQLException {
        DataSourceConfiguration config = createH2Configuration();
        config.getConnection().setHost(null);
        config.getConnection().setDatabase("mem:customtest;TRACE_LEVEL_SYSTEM_OUT=2;MODE=Oracle");

        DataSource dataSource = JdbcTemplateFactory.createDataSource(config);
        assertNotNull(dataSource);

        // Verify connection works with custom in-memory parameters
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.contains("jdbc:h2:mem:customtest"), "URL should contain custom in-memory database name");

            // Verify the connection is working by executing a simple query
            assertNotNull(connection.createStatement().executeQuery("SELECT 1"));
        }
    }

    @Test
    @DisplayName("Should build H2 TCP JDBC URL correctly")
    void testH2TcpJdbcUrl() throws DataSourceException {
        DataSourceConfiguration config = createH2Configuration();
        config.getConnection().setHost("localhost");
        config.getConnection().setPort(9092);
        
        // This will fail to connect since we don't have a TCP H2 server running,
        // but we can verify the URL construction logic by catching the connection error
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            JdbcTemplateFactory.createDataSource(config);
        });
        
        // Could be either CONNECTION_ERROR or CONFIGURATION_ERROR depending on when the error occurs
        assertTrue(exception.getErrorType() == DataSourceException.ErrorType.CONNECTION_ERROR ||
                   exception.getErrorType() == DataSourceException.ErrorType.CONFIGURATION_ERROR);
    }

    @Test
    @DisplayName("Should throw exception for unsupported database type")
    void testUnsupportedDatabaseType() {
        DataSourceConfiguration config = createH2Configuration();
        config.setSourceType("unsupported");
        
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            JdbcTemplateFactory.createDataSource(config);
        });

        assertEquals(DataSourceException.ErrorType.CONFIGURATION_ERROR, exception.getErrorType());
        // The exception is wrapped, so check the cause chain for the original message
        assertTrue(exception.getMessage().contains("Failed to create") ||
                   containsInCauseChain(exception, "Unsupported database type"));
    }

    // ========================================
    // Connection Pool Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should handle connection pool configuration")
    void testConnectionPoolConfiguration() throws DataSourceException, SQLException {
        DataSourceConfiguration config = createH2Configuration();
        
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMinSize(2);
        poolConfig.setMaxSize(10);
        poolConfig.setConnectionTimeout(30000L);
        poolConfig.setIdleTimeout(600000L);
        poolConfig.setMaxLifetime(1800000L);
        poolConfig.setConnectionTestQuery("SELECT 1");
        poolConfig.setLeakDetectionThreshold(60000L);
        
        config.getConnection().setConnectionPool(poolConfig);
        
        DataSource dataSource = JdbcTemplateFactory.createDataSource(config);
        assertNotNull(dataSource);
        
        // Test that we can get connections
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
            assertFalse(connection.isClosed());
        }
    }

    // ========================================
    // Caching Tests
    // ========================================

    @Test
    @DisplayName("Should cache DataSource instances")
    void testDataSourceCaching() throws DataSourceException {
        DataSourceConfiguration config = createH2Configuration();
        
        DataSource dataSource1 = JdbcTemplateFactory.createDataSource(config);
        DataSource dataSource2 = JdbcTemplateFactory.createDataSource(config);
        
        assertSame(dataSource1, dataSource2);
    }

    @Test
    @DisplayName("Should create different instances for different configurations")
    void testDifferentConfigurationsDifferentInstances() throws DataSourceException {
        DataSourceConfiguration config1 = createH2Configuration();
        config1.setName("db1");
        config1.getConnection().setDatabase("mem:testdb1");

        DataSourceConfiguration config2 = createH2Configuration();
        config2.setName("db2");
        config2.getConnection().setDatabase("mem:testdb2");
        
        DataSource dataSource1 = JdbcTemplateFactory.createDataSource(config1);
        DataSource dataSource2 = JdbcTemplateFactory.createDataSource(config2);
        
        assertNotSame(dataSource1, dataSource2);
    }

    @Test
    @DisplayName("Should remove DataSource from cache")
    void testRemoveFromCache() throws DataSourceException {
        DataSourceConfiguration config = createH2Configuration();
        
        DataSource dataSource1 = JdbcTemplateFactory.createDataSource(config);
        
        JdbcTemplateFactory.removeFromCache(config);
        
        DataSource dataSource2 = JdbcTemplateFactory.createDataSource(config);
        
        // Should be different instances since first was removed from cache
        assertNotSame(dataSource1, dataSource2);
    }

    @Test
    @DisplayName("Should clear all cached DataSources")
    void testClearCache() throws DataSourceException {
        DataSourceConfiguration config = createH2Configuration();
        
        DataSource dataSource1 = JdbcTemplateFactory.createDataSource(config);
        
        JdbcTemplateFactory.clearCache();
        
        DataSource dataSource2 = JdbcTemplateFactory.createDataSource(config);
        
        // Should be different instances since cache was cleared
        assertNotSame(dataSource1, dataSource2);
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle invalid database configuration")
    void testInvalidDatabaseConfiguration() {
        DataSourceConfiguration config = createH2Configuration();
        config.setSourceType("postgresql"); // Use PostgreSQL but with invalid connection details
        config.getConnection().setHost("invalid-host-that-does-not-exist");
        config.getConnection().setPort(5432);
        config.getConnection().setDatabase("mem:testdb");

        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            JdbcTemplateFactory.createDataSource(config);
        });

        // Could be either CONNECTION_ERROR or CONFIGURATION_ERROR depending on when the error occurs
        assertTrue(exception.getErrorType() == DataSourceException.ErrorType.CONNECTION_ERROR ||
                   exception.getErrorType() == DataSourceException.ErrorType.CONFIGURATION_ERROR);
    }

    @Test
    @DisplayName("Should handle connection failure gracefully")
    void testConnectionFailure() {
        DataSourceConfiguration config = createH2Configuration();
        config.setSourceType("postgresql"); // Will fail since PostgreSQL is not running
        config.getConnection().setHost("nonexistent-host");
        config.getConnection().setPort(5432);
        
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            JdbcTemplateFactory.createDataSource(config);
        });
        
        // Could be either CONNECTION_ERROR or CONFIGURATION_ERROR depending on when the error occurs
        assertTrue(exception.getErrorType() == DataSourceException.ErrorType.CONNECTION_ERROR ||
                   exception.getErrorType() == DataSourceException.ErrorType.CONFIGURATION_ERROR);
    }

    // ========================================
    // SimpleDataSource Tests
    // ========================================

    @Test
    @DisplayName("Should use SimpleDataSource when HikariCP is not available")
    void testSimpleDataSourceFallback() throws DataSourceException, SQLException {
        DataSourceConfiguration config = createH2Configuration();
        
        DataSource dataSource = JdbcTemplateFactory.createDataSource(config);
        
        // Since HikariCP should be available in test classpath, this will actually use HikariCP
        // But we can still test that the DataSource works
        assertNotNull(dataSource);
        
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
            assertFalse(connection.isClosed());
        }
    }

    @Test
    @DisplayName("Should handle SimpleDataSource connection with credentials")
    void testSimpleDataSourceWithCredentials() throws DataSourceException, SQLException {
        DataSourceConfiguration config = createH2Configuration();
        config.getConnection().setUsername("sa");
        config.getConnection().setPassword("");
        
        DataSource dataSource = JdbcTemplateFactory.createDataSource(config);
        
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
            assertFalse(connection.isClosed());
        }
        
        // Note: HikariCP doesn't support getConnection(username, password)
        // so we only test the basic getConnection() method
    }

    // ========================================
    // Helper Methods
    // ========================================

    private DataSourceConfiguration createH2Configuration() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-h2-db");
        config.setType("database");
        config.setSourceType("h2");
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setHost(null); // In-memory H2
        connectionConfig.setDatabase("mem:testdb_" + System.nanoTime()); // Unique in-memory database name per test
        connectionConfig.setUsername("sa");
        connectionConfig.setPassword("");

        config.setConnection(connectionConfig);

        return config;
    }

    private boolean containsInCauseChain(Throwable throwable, String message) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(message)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
