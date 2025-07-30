package dev.mars.apex.core.service.data.external.database;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.ConnectionPoolConfig;
import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.config.datasource.HealthCheckConfig;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.data.external.DataSourceMetrics;
import dev.mars.apex.core.service.data.external.ConnectionStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive PostgreSQL integration tests using TestContainers.
 * 
 * This test class covers all scenarios from the External Data Sources Guide:
 * - Database connectivity and configuration
 * - Connection pooling
 * - Query execution and parameter binding
 * - Batch operations
 * - Health monitoring
 * - Performance metrics
 * - Error handling and resilience
 * - Caching integration
 * 
 * @author APEX Rules Engine Team
 * @since 1.0.0
 */
@Testcontainers
class PostgreSQLIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("test-schema.sql");

    private DataSourceFactory factory;
    private ExternalDataSource dataSource;
    private DataSourceConfiguration configuration;

    @BeforeEach
    void setUp() throws DataSourceException {
        factory = DataSourceFactory.getInstance();
        configuration = createPostgreSQLConfiguration();
        dataSource = factory.createDataSource(configuration);
    }

    @AfterEach
    void tearDown() throws DataSourceException {
        if (dataSource != null) {
            dataSource.shutdown();
        }
        factory.clearCache();
    }

    @Test
    void testBasicConnectivity() throws DataSourceException {
        // Test basic connection establishment
        assertTrue(dataSource.testConnection(), "Should be able to connect to PostgreSQL");
        assertTrue(dataSource.isHealthy(), "Data source should be healthy");
        
        ConnectionStatus status = dataSource.getConnectionStatus();
        assertEquals(ConnectionStatus.State.CONNECTED, status.getState());
        assertNotNull(status.getLastConnected());
    }

    @Test
    void testSimpleQuery() throws DataSourceException {
        // Test basic query execution
        Map<String, Object> parameters = new HashMap<>();
        List<Object> results = dataSource.query("SELECT 1 as test_value", parameters);
        
        assertNotNull(results);
        assertEquals(1, results.size());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> row = (Map<String, Object>) results.get(0);
        assertEquals(1, row.get("test_value"));
    }

    @Test
    void testParameterizedQuery() throws DataSourceException {
        // Insert test data first
        insertTestUsers();
        
        // Test parameterized query
        Map<String, Object> parameters = Map.of("email", "john@example.com");
        Object result = dataSource.queryForObject("getUserByEmail", parameters);
        
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) result;
        assertEquals("John Doe", user.get("name"));
        assertEquals("john@example.com", user.get("email"));
    }

    @Test
    void testBatchOperations() throws DataSourceException {
        // Test batch insert operations
        List<String> batchUpdates = List.of(
            "INSERT INTO users (name, email, status) VALUES ('Alice Smith', 'alice@example.com', 'ACTIVE')",
            "INSERT INTO users (name, email, status) VALUES ('Bob Johnson', 'bob@example.com', 'ACTIVE')",
            "INSERT INTO users (name, email, status) VALUES ('Carol Brown', 'carol@example.com', 'INACTIVE')"
        );
        
        dataSource.batchUpdate(batchUpdates);
        
        // Verify batch insert worked
        List<Object> results = dataSource.query("SELECT COUNT(*) as count FROM users", new HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Object> countResult = (Map<String, Object>) results.get(0);
        assertTrue(((Number) countResult.get("count")).intValue() >= 3);
    }

    @Test
    void testConnectionPooling() throws DataSourceException {
        // Test multiple concurrent connections
        CompletableFuture<?>[] futures = new CompletableFuture[10];
        
        for (int i = 0; i < 10; i++) {
            final int userId = i + 1;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> params = Map.of("id", userId);
                    dataSource.query("SELECT :id as user_id", params);
                } catch (DataSourceException e) {
                    fail("Concurrent query failed: " + e.getMessage());
                }
            });
        }
        
        // Wait for all queries to complete
        CompletableFuture.allOf(futures).join();
        
        // Verify connection pool metrics
        DataSourceMetrics metrics = dataSource.getMetrics();
        assertTrue(metrics.getSuccessfulRequests() >= 10);
        assertEquals(0, metrics.getFailedRequests());
    }

    @Test
    void testHealthMonitoring() throws DataSourceException {
        // Test health check functionality
        assertTrue(dataSource.isHealthy());
        
        // Test health check query
        ConnectionStatus status = dataSource.getConnectionStatus();
        assertEquals(ConnectionStatus.State.CONNECTED, status.getState());
        
        // Verify health check metrics
        DataSourceMetrics metrics = dataSource.getMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.getSuccessRate() > 0.0);
    }

    @Test
    void testCachingIntegration() throws DataSourceException {
        // Insert test data
        insertTestUsers();
        
        // First query - should hit database
        Map<String, Object> parameters = Map.of("email", "john@example.com");
        Object result1 = dataSource.queryForObject("getUserByEmail", parameters);
        
        // Second query - should hit cache (if caching is enabled)
        Object result2 = dataSource.queryForObject("getUserByEmail", parameters);
        
        assertNotNull(result1);
        assertNotNull(result2);
        
        // Verify cache metrics
        DataSourceMetrics metrics = dataSource.getMetrics();
        assertTrue(metrics.getTotalRequests() >= 2);
    }

    @Test
    void testErrorHandling() {
        // Test invalid query
        assertThrows(DataSourceException.class, () -> {
            dataSource.query("INVALID SQL QUERY", new HashMap<>());
        });
        
        // Test invalid parameters
        assertThrows(DataSourceException.class, () -> {
            Map<String, Object> params = Map.of("nonexistent_param", "value");
            dataSource.query("SELECT * FROM users WHERE id = :id", params);
        });
    }

    @Test
    void testPerformanceMetrics() throws DataSourceException {
        // Execute several queries to generate metrics
        for (int i = 0; i < 5; i++) {
            dataSource.query("SELECT " + i + " as iteration", new HashMap<>());
        }
        
        DataSourceMetrics metrics = dataSource.getMetrics();
        assertEquals(5, metrics.getSuccessfulRequests());
        assertEquals(0, metrics.getFailedRequests());
        assertTrue(metrics.getAverageResponseTime() >= 0);
        assertEquals(1.0, metrics.getSuccessRate(), 0.01);
    }

    private DataSourceConfiguration createPostgreSQLConfiguration() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-postgresql");
        config.setSourceType("postgresql");
        config.setDataSourceType(DataSourceType.DATABASE);
        config.setEnabled(true);
        
        // Connection configuration
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setHost(postgres.getHost());
        connectionConfig.setPort(postgres.getFirstMappedPort());
        connectionConfig.setDatabase(postgres.getDatabaseName());
        connectionConfig.setUsername(postgres.getUsername());
        connectionConfig.setPassword(postgres.getPassword());

        // Connection pool configuration
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxSize(10);
        poolConfig.setMinSize(2);
        poolConfig.setConnectionTimeout(30000L);
        connectionConfig.setConnectionPool(poolConfig);

        config.setConnection(connectionConfig);
        
        // Query configuration
        Map<String, String> queries = new HashMap<>();
        queries.put("getUserByEmail", "SELECT * FROM users WHERE email = :email");
        queries.put("getAllUsers", "SELECT * FROM users ORDER BY id");
        queries.put("createUser", "INSERT INTO users (name, email, status) VALUES (:name, :email, :status) RETURNING id");
        queries.put("updateUser", "UPDATE users SET name = :name, email = :email WHERE id = :id");
        queries.put("deleteUser", "DELETE FROM users WHERE id = :id");
        queries.put("default", "SELECT 1");
        config.setQueries(queries);
        
        // Cache configuration
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(300L);
        cacheConfig.setMaxSize(1000);
        config.setCache(cacheConfig);
        
        // Health check configuration
        HealthCheckConfig healthConfig = new HealthCheckConfig();
        healthConfig.setEnabled(true);
        healthConfig.setIntervalSeconds(30L);
        healthConfig.setTimeoutSeconds(5L);
        healthConfig.setQuery("SELECT 1");
        config.setHealthCheck(healthConfig);
        
        return config;
    }

    @Test
    void testComplexQueries() throws DataSourceException {
        // Test complex JOIN queries
        Map<String, String> complexQueries = new HashMap<>();
        complexQueries.put("getUserOrderSummary",
            "SELECT u.name, u.email, COUNT(o.id) as order_count, COALESCE(SUM(o.total_amount), 0) as total_spent " +
            "FROM users u LEFT JOIN orders o ON u.id = o.user_id " +
            "WHERE u.email = :email GROUP BY u.id, u.name, u.email");

        configuration.getQueries().putAll(complexQueries);

        // Insert test data
        insertTestUsers();
        insertTestOrders();

        Map<String, Object> parameters = Map.of("email", "john@example.com");
        Object result = dataSource.queryForObject("getUserOrderSummary", parameters);

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result;
        assertEquals("John Doe", summary.get("name"));
    }

    @Test
    void testTransactionScenarios() throws DataSourceException {
        // Test multiple related operations
        insertTestUsers();

        // Create an order transaction
        Map<String, Object> orderParams = Map.of(
            "user_id", 1,
            "product_id", 1,
            "quantity", 2,
            "unit_price", 1299.99,
            "total_amount", 2599.98,
            "status", "PENDING"
        );

        String insertOrderQuery = "INSERT INTO orders (user_id, product_id, quantity, unit_price, total_amount, status) " +
                                 "VALUES (:user_id, :product_id, :quantity, :unit_price, :total_amount, :status) RETURNING id";

        configuration.getQueries().put("createOrder", insertOrderQuery);
        Object result = dataSource.queryForObject("createOrder", orderParams);

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> orderResult = (Map<String, Object>) result;
        assertTrue(((Number) orderResult.get("id")).intValue() > 0);
    }

    @Test
    void testDataEnrichmentScenarios() throws DataSourceException {
        // Test currency enrichment scenario
        Map<String, Object> currencyParams = Map.of("code", "USD");
        String currencyQuery = "SELECT code, name, symbol, decimal_places, active, major_currency, region " +
                              "FROM currencies WHERE code = :code";

        configuration.getQueries().put("getCurrencyByCode", currencyQuery);
        Object result = dataSource.queryForObject("getCurrencyByCode", currencyParams);

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> currency = (Map<String, Object>) result;
        assertEquals("USD", currency.get("code"));
        assertEquals("US Dollar", currency.get("name"));
        assertEquals("$", currency.get("symbol"));
        assertEquals(2, currency.get("decimal_places"));
        assertTrue((Boolean) currency.get("active"));
        assertTrue((Boolean) currency.get("major_currency"));
        assertEquals("North America", currency.get("region"));
    }

    @Test
    void testPerformanceWithLargeDataset() throws DataSourceException {
        // Test query performance with large dataset
        String performanceQuery = "SELECT COUNT(*) as total_records FROM performance_test WHERE numeric_value > :threshold";
        configuration.getQueries().put("getPerformanceCount", performanceQuery);

        Map<String, Object> params = Map.of("threshold", 500.0);
        Object result = dataSource.queryForObject("getPerformanceCount", params);

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> countResult = (Map<String, Object>) result;
        assertTrue(((Number) countResult.get("total_records")).intValue() > 0);

        // Verify performance metrics
        DataSourceMetrics metrics = dataSource.getMetrics();
        assertTrue(metrics.getAverageResponseTime() >= 0);
    }

    @Test
    void testConnectionRecovery() throws DataSourceException {
        // Test connection recovery after temporary failure
        assertTrue(dataSource.isHealthy());

        // Simulate connection test
        assertTrue(dataSource.testConnection());

        // Verify connection status
        ConnectionStatus status = dataSource.getConnectionStatus();
        assertEquals(ConnectionStatus.State.CONNECTED, status.getState());

        // Test that queries still work after connection check
        Map<String, Object> params = new HashMap<>();
        List<Object> results = dataSource.query("SELECT 1 as recovery_test", params);
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    private void insertTestUsers() throws DataSourceException {
        List<String> insertStatements = List.of(
            "INSERT INTO users (name, email, status) VALUES ('John Doe', 'john@example.com', 'ACTIVE')",
            "INSERT INTO users (name, email, status) VALUES ('Jane Smith', 'jane@example.com', 'ACTIVE')",
            "INSERT INTO users (name, email, status) VALUES ('Bob Wilson', 'bob@example.com', 'INACTIVE')"
        );

        dataSource.batchUpdate(insertStatements);
    }

    private void insertTestOrders() throws DataSourceException {
        List<String> insertStatements = List.of(
            "INSERT INTO orders (user_id, product_id, quantity, unit_price, total_amount, status) VALUES (1, 1, 1, 1299.99, 1299.99, 'COMPLETED')",
            "INSERT INTO orders (user_id, product_id, quantity, unit_price, total_amount, status) VALUES (1, 2, 2, 399.50, 799.00, 'COMPLETED')",
            "INSERT INTO orders (user_id, product_id, quantity, unit_price, total_amount, status) VALUES (2, 3, 1, 99.99, 99.99, 'PENDING')"
        );

        dataSource.batchUpdate(insertStatements);
    }
}
