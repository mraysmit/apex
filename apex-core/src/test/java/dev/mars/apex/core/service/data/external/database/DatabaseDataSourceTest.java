package dev.mars.apex.core.service.data.external.database;

import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.HealthCheckConfig;
import dev.mars.apex.core.service.data.external.*;
import org.junit.jupiter.api.*;


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for DatabaseDataSource.
 * 
 * Tests cover:
 * - Initialization and configuration
 * - Connection management and testing
 * - Query execution and result mapping
 * - Caching functionality
 * - Batch operations
 * - Error handling and recovery
 * - Metrics collection
 * - Health monitoring integration
 * - Resource management and cleanup
 * 
 * Uses H2 in-memory database for real database testing without external dependencies.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class DatabaseDataSourceTest {

    private DataSource dataSource;
    private DatabaseDataSource databaseDataSource;
    private DataSourceConfiguration configuration;

    @BeforeEach
    void setUp() throws DataSourceException, SQLException {
        // Setup configuration
        configuration = createTestConfiguration();
        
        // Create real PostgreSQL DataSource
        dataSource = JdbcTemplateFactory.createDataSource(configuration);
        
        // Initialize database with test data
        initializeTestDatabase();
        
        // Create DatabaseDataSource instance
        databaseDataSource = new DatabaseDataSource(dataSource, configuration);
        databaseDataSource.initialize(configuration);
    }

    @AfterEach
    void tearDown() {
        if (databaseDataSource != null) {
            databaseDataSource.shutdown();
        }
        JdbcTemplateFactory.clearCache();
    }

    // ========================================
    // Initialization Tests
    // ========================================

    @Test
    @DisplayName("Should initialize successfully with valid configuration")
    void testSuccessfulInitialization() {
        assertEquals(DataSourceType.DATABASE, databaseDataSource.getSourceType());
        assertEquals("test-database", databaseDataSource.getName());
        assertEquals("h2", databaseDataSource.getDataType()); // Returns sourceType from configuration
        assertNotNull(databaseDataSource.getConnectionStatus());
        assertNotNull(databaseDataSource.getMetrics());
    }

    @Test
    @DisplayName("Should update configuration during initialization")
    void testConfigurationUpdate() throws DataSourceException {
        DataSourceConfiguration newConfig = createTestConfiguration();
        newConfig.setName("updated-database");
        
        databaseDataSource.initialize(newConfig);
        
        assertEquals("updated-database", databaseDataSource.getName());
        assertEquals(newConfig, databaseDataSource.getConfiguration());
    }

    // ========================================
    // Connection Management Tests
    // ========================================

    @Test
    @DisplayName("Should test connection successfully")
    void testConnectionSuccess() {
        boolean result = databaseDataSource.testConnection();
        
        assertTrue(result);
        assertEquals(ConnectionStatus.State.CONNECTED, databaseDataSource.getConnectionStatus().getState());
    }

    // ========================================
    // Data Type Support Tests
    // ========================================

    @Test
    @DisplayName("Should support database data type")
    void testDataTypeSupport() {
        assertTrue(databaseDataSource.supportsDataType("database"));
        assertFalse(databaseDataSource.supportsDataType("cache"));
        assertFalse(databaseDataSource.supportsDataType("file"));
    }

    @Test
    @DisplayName("Should support configured source type")
    void testConfiguredSourceTypeSupport() {
        assertTrue(databaseDataSource.supportsDataType("h2"));
        assertTrue(databaseDataSource.supportsDataType("database"));
    }

    // ========================================
    // Query Execution Tests
    // ========================================

    @Test
    @DisplayName("Should execute simple query successfully")
    void testSimpleQueryExecution() throws DataSourceException {
        List<Map<String, Object>> results = databaseDataSource.query("SELECT * FROM test_users ORDER BY id", Collections.emptyMap());
        
        assertNotNull(results);
        assertEquals(2, results.size());
        
        Map<String, Object> firstRow = results.get(0);
        assertEquals(1, firstRow.get("ID")); // H2 returns uppercase column names
        assertEquals("Test User 1", firstRow.get("NAME"));
    }

    @Test
    @DisplayName("Should execute parameterized query successfully")
    void testParameterizedQueryExecution() throws DataSourceException {
        Map<String, Object> params = Map.of("id", 1);
        List<Map<String, Object>> results = databaseDataSource.query("SELECT * FROM test_users WHERE id = :id", params);
        
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).get("ID")); // H2 returns uppercase column names
        assertEquals("Test User 1", results.get(0).get("NAME"));
    }

    @Test
    @DisplayName("Should execute queryForObject successfully")
    void testQueryForObject() throws DataSourceException {
        Map<String, Object> result = databaseDataSource.queryForObject("SELECT * FROM test_users WHERE id = 1", Collections.emptyMap());
        
        assertNotNull(result);
        assertEquals(1, result.get("ID")); // H2 returns uppercase column names
        assertEquals("Test User 1", result.get("NAME"));
    }

    @Test
    @DisplayName("Should return null for queryForObject with no results")
    void testQueryForObjectNoResults() throws DataSourceException {
        Map<String, Object> result = databaseDataSource.queryForObject("SELECT * FROM test_users WHERE id = 999", Collections.emptyMap());
        
        assertNull(result);
    }

    @Test
    @DisplayName("Should execute getData with query name")
    void testGetDataWithQueryName() {
        Object result = databaseDataSource.getData("users", 1);

        assertNotNull(result);
        assertTrue(result instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals(1, resultMap.get("ID")); // H2 returns uppercase column names
        assertEquals("Test User 1", resultMap.get("NAME"));
    }

    @Test
    @DisplayName("Should handle getData with non-existent query name")
    void testGetDataWithNonExistentQueryName() {
        // When query name doesn't exist, it falls back to "default" query
        Object result = databaseDataSource.getData("nonexistent", 1);

        assertNotNull(result); // Falls back to default query
        assertTrue(result instanceof Map);
    }

    // ========================================
    // Batch Operations Tests
    // ========================================

    @Test
    @DisplayName("Should execute batch queries successfully")
    void testBatchQueryExecution() throws DataSourceException {
        List<String> queries = Arrays.asList(
            "SELECT COUNT(*) as count FROM test_users",
            "SELECT * FROM test_users WHERE id = 1",
            "SELECT name FROM test_users WHERE id = 2"
        );
        
        List<List<Map<String, Object>>> results = databaseDataSource.batchQuery(queries);
        
        assertNotNull(results);
        assertEquals(3, results.size());
        
        // First query should return count
        assertEquals(1, results.get(0).size());
        assertEquals(2L, results.get(0).get(0).get("COUNT")); // H2 returns uppercase column names

        // Second query should return user 1
        assertEquals(1, results.get(1).size());
        assertEquals("Test User 1", results.get(1).get(0).get("NAME")); // H2 returns uppercase column names

        // Third query should return user 2
        assertEquals(1, results.get(2).size());
        assertEquals("Test User 2", results.get(2).get(0).get("NAME")); // H2 returns uppercase column names
    }

    @Test
    @DisplayName("Should execute batch updates successfully")
    void testBatchUpdateExecution() throws DataSourceException {
        List<String> updates = Arrays.asList(
            "UPDATE test_users SET name = 'Updated User 1' WHERE id = 1",
            "UPDATE test_users SET name = 'Updated User 2' WHERE id = 2",
            "INSERT INTO test_users VALUES (3, 'New User 3')"
        );
        
        assertDoesNotThrow(() -> {
            databaseDataSource.batchUpdate(updates);
        });
        
        // Verify updates were applied
        List<Map<String, Object>> results = databaseDataSource.query("SELECT * FROM test_users ORDER BY id", Collections.emptyMap());
        assertEquals(3, results.size());
        assertEquals("Updated User 1", results.get(0).get("NAME")); // H2 returns uppercase column names
        assertEquals("Updated User 2", results.get(1).get("NAME"));
        assertEquals("New User 3", results.get(2).get("NAME"));
    }

    @Test
    @DisplayName("Should handle batch update failure with rollback")
    void testBatchUpdateFailureRollback() {
        List<String> updates = Arrays.asList(
            "UPDATE test_users SET name = 'Updated User 1' WHERE id = 1",
            "INVALID SQL STATEMENT", // This will cause failure
            "UPDATE test_users SET name = 'Updated User 2' WHERE id = 2"
        );
        
        assertThrows(DataSourceException.class, () -> {
            databaseDataSource.batchUpdate(updates);
        });
        
        // Verify no changes were applied due to rollback
        try {
            List<Map<String, Object>> results = databaseDataSource.query("SELECT * FROM test_users WHERE id = 1", Collections.emptyMap());
            assertEquals("Test User 1", results.get(0).get("NAME")); // Should still be original name (H2 uppercase)
        } catch (DataSourceException e) {
            fail("Failed to query after rollback: " + e.getMessage());
        }
    }

    // ========================================
    // Caching Tests
    // ========================================

    @Test
    @DisplayName("Should cache query results when caching is enabled")
    void testCachingEnabled() throws DataSourceException, SQLException {
        // Enable caching
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(300L);
        configuration.setCache(cacheConfig);

        databaseDataSource = new DatabaseDataSource(dataSource, configuration);
        databaseDataSource.initialize(configuration);

        // First call should execute query and cache result
        Object result1 = databaseDataSource.getData("users", 1);
        assertNotNull(result1);
        assertTrue(result1 instanceof Map);

        // Modify database
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE test_users SET name = 'Modified' WHERE id = 1");
        }

        // Second call should return cached result (not modified data)
        Object result2 = databaseDataSource.getData("users", 1);
        assertNotNull(result2);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result2;
        assertEquals("Test User 1", resultMap.get("NAME")); // Should be cached original value (H2 uppercase)
    }

    @Test
    @DisplayName("Should not cache when caching is disabled")
    void testCachingDisabled() throws DataSourceException, SQLException {
        // Caching is disabled by default

        // First call
        Object result1 = databaseDataSource.getData("users", 1);
        assertNotNull(result1);
        assertTrue(result1 instanceof Map);

        // Modify database
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE test_users SET name = 'Modified' WHERE id = 1");
        }

        // Second call should return fresh data
        Object result2 = databaseDataSource.getData("users", 1);
        assertNotNull(result2);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result2;
        assertEquals("Modified", resultMap.get("NAME")); // Should be updated value (H2 uppercase)
    }

    // ========================================
    // Metrics Tests
    // ========================================

    @Test
    @DisplayName("Should record successful request metrics")
    void testSuccessfulRequestMetrics() {
        DataSourceMetrics initialMetrics = databaseDataSource.getMetrics();
        long initialSuccessCount = initialMetrics.getSuccessfulRequests();

        databaseDataSource.getData("users", 1);

        DataSourceMetrics updatedMetrics = databaseDataSource.getMetrics();
        assertEquals(initialSuccessCount + 1, updatedMetrics.getSuccessfulRequests());
        assertTrue(updatedMetrics.getAverageResponseTime() >= 0);
    }

    @Test
    @DisplayName("Should record failed request metrics")
    void testFailedRequestMetrics() {
        DataSourceMetrics initialMetrics = databaseDataSource.getMetrics();
        long initialFailureCount = initialMetrics.getFailedRequests();

        // Execute query that will cause an exception (invalid SQL)
        try {
            databaseDataSource.query("INVALID SQL STATEMENT", Collections.emptyMap());
            fail("Expected DataSourceException");
        } catch (DataSourceException e) {
            // Expected
        }

        DataSourceMetrics updatedMetrics = databaseDataSource.getMetrics();
        assertEquals(initialFailureCount + 1, updatedMetrics.getFailedRequests());
    }

    // ========================================
    // Health Monitoring Tests
    // ========================================

    @Test
    @DisplayName("Should delegate health check to health indicator")
    void testHealthCheckDelegation() {
        boolean healthy = databaseDataSource.isHealthy();

        assertTrue(healthy);
    }

    // ========================================
    // Resource Management Tests
    // ========================================

    @Test
    @DisplayName("Should refresh successfully")
    void testRefresh() throws DataSourceException {
        assertDoesNotThrow(() -> {
            databaseDataSource.refresh();
        });

        assertTrue(databaseDataSource.testConnection());
    }

    @Test
    @DisplayName("Should shutdown gracefully")
    void testShutdown() {
        assertDoesNotThrow(() -> {
            databaseDataSource.shutdown();
        });

        assertEquals(ConnectionStatus.State.SHUTDOWN, databaseDataSource.getConnectionStatus().getState());
    }

    // ========================================
    // Edge Cases and Error Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle null configuration gracefully")
    void testNullConfiguration() {
        // Note: DatabaseDataSource with null configuration will fail in constructor
        // due to DatabaseHealthIndicator requiring configuration
        assertThrows(NullPointerException.class, () -> {
            new DatabaseDataSource(dataSource, null);
        });
    }

    @Test
    @DisplayName("Should handle empty query results")
    void testEmptyQueryResults() throws DataSourceException {
        List<Map<String, Object>> results = databaseDataSource.query("SELECT * FROM test_users WHERE id = 999", Collections.emptyMap());

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should handle SQL exceptions in query execution")
    void testSQLExceptionHandling() {
        assertThrows(DataSourceException.class, () -> {
            databaseDataSource.query("SELECT * FROM nonexistent_table", Collections.emptyMap());
        });
    }

    @Test
    @DisplayName("Should handle null parameters in query")
    void testNullParametersInQuery() {
        // The implementation doesn't handle null parameters gracefully
        assertThrows(NullPointerException.class, () -> {
            databaseDataSource.query("SELECT * FROM test_users", null);
        });
    }

    @Test
    @DisplayName("Should handle empty query string")
    void testEmptyQueryString() {
        assertThrows(DataSourceException.class, () -> {
            databaseDataSource.query("", Collections.emptyMap());
        });
    }

    @Test
    @DisplayName("Should handle null query string")
    void testNullQueryString() {
        assertThrows(DataSourceException.class, () -> {
            databaseDataSource.query(null, Collections.emptyMap());
        });
    }

    // ========================================
    // Helper Methods
    // ========================================

    private DataSourceConfiguration createTestConfiguration() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-database");
        config.setType("database");
        config.setSourceType("h2");
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setHost(null); // In-memory H2
        connectionConfig.setDatabase("testdb_" + System.nanoTime()); // Unique database name per test
        connectionConfig.setUsername("sa");
        connectionConfig.setPassword("");
        
        config.setConnection(connectionConfig);
        
        // Add some test queries
        Map<String, String> queries = new HashMap<>();
        queries.put("default", "SELECT * FROM test_users");
        queries.put("users", "SELECT * FROM test_users WHERE id = :id");
        queries.put("count", "SELECT COUNT(*) as count FROM test_users");
        config.setQueries(queries);

        // Set parameter names for parameterized queries
        config.setParameterNames(new String[]{"id"});
        
        // Add health check configuration
        HealthCheckConfig healthCheck = new HealthCheckConfig();
        healthCheck.setEnabled(false); // Disable health monitoring for tests
        healthCheck.setTimeoutSeconds(10L);
        healthCheck.setFailureThreshold(3);
        healthCheck.setSuccessThreshold(1);
        config.setHealthCheck(healthCheck);
        
        return config;
    }

    private void initializeTestDatabase() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {

            // Drop table if exists and recreate to ensure clean state
            stmt.execute("DROP TABLE IF EXISTS test_users");
            stmt.execute("CREATE TABLE test_users (id INTEGER PRIMARY KEY, name VARCHAR(50))");

            // Insert test data
            stmt.execute("INSERT INTO test_users VALUES (1, 'Test User 1')");
            stmt.execute("INSERT INTO test_users VALUES (2, 'Test User 2')");
        }
    }
}
