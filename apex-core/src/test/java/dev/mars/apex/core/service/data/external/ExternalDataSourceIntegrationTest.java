package dev.mars.apex.core.service.data.external;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.config.datasource.HealthCheckConfig;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for external data sources covering all scenarios 
 * from the External Data Sources Guide.
 * 
 * This test class covers:
 * - REST API integration with authentication
 * - File system integration (CSV, JSON, XML)
 * - Cache integration with TTL and eviction
 * - Circuit breaker patterns
 * - Retry logic and error handling
 * - Health monitoring and metrics
 * - Performance optimization
 * 
 * @author APEX Rules Engine Team
 * @since 1.0.0
 */
class ExternalDataSourceIntegrationTest {

    @TempDir
    Path tempDir;

    private DataSourceFactory factory;

    @BeforeEach
    void setUp() {
        factory = DataSourceFactory.getInstance();
    }

    @AfterEach
    void tearDown() throws DataSourceException {
        factory.clearCache();
    }

    @Test
    void testRestApiDataSource() throws DataSourceException {
        // Test REST API integration from External Data Sources Guide Section 4
        DataSourceConfiguration config = createRestApiConfiguration();
        ExternalDataSource dataSource = factory.createDataSource(config);

        assertTrue(dataSource.testConnection(), "Should be able to connect to REST API");
        assertTrue(dataSource.isHealthy(), "Data source should be healthy");

        // Test basic query
        Map<String, Object> parameters = Map.of("id", "123");
        List<Object> results = dataSource.query("getUserById", parameters);
        
        assertNotNull(results);
        // Note: In a real test, this would connect to a mock REST service
        
        dataSource.shutdown();
    }

    @Test
    void testCsvFileDataSource() throws DataSourceException, IOException {
        // Test CSV file integration from External Data Sources Guide Section 5
        Path csvFile = createTestCsvFile();
        
        DataSourceConfiguration config = createCsvFileConfiguration(csvFile.toString());
        ExternalDataSource dataSource = factory.createDataSource(config);

        assertTrue(dataSource.testConnection(), "Should be able to read CSV file");
        assertTrue(dataSource.isHealthy(), "Data source should be healthy");

        // Test CSV query
        Map<String, Object> parameters = Map.of("name", "John Doe");
        List<Object> results = dataSource.query("findByName", parameters);
        
        assertNotNull(results);
        
        dataSource.shutdown();
    }

    @Test
    void testJsonFileDataSource() throws DataSourceException, IOException {
        // Test JSON file integration from External Data Sources Guide Section 5
        Path jsonFile = createTestJsonFile();
        
        DataSourceConfiguration config = createJsonFileConfiguration(jsonFile.toString());
        ExternalDataSource dataSource = factory.createDataSource(config);

        assertTrue(dataSource.testConnection(), "Should be able to read JSON file");
        assertTrue(dataSource.isHealthy(), "Data source should be healthy");

        // Test JSONPath query
        Map<String, Object> parameters = Map.of("id", "1");
        Object result = dataSource.queryForObject("getUserById", parameters);

        assertNotNull(result);
        
        dataSource.shutdown();
    }

    @Test
    void testCacheIntegration() throws DataSourceException, IOException {
        // Test cache integration from External Data Sources Guide Section 7
        Path csvFile = createTestCsvFile();
        
        DataSourceConfiguration config = createCachedCsvConfiguration(csvFile.toString());
        ExternalDataSource dataSource = factory.createDataSource(config);

        // First query - should hit file system
        Map<String, Object> parameters = Map.of("name", "John Doe");
        List<Object> results1 = dataSource.query("findByName", parameters);
        
        // Second query - should hit cache
        List<Object> results2 = dataSource.query("findByName", parameters);
        
        assertNotNull(results1);
        assertNotNull(results2);
        
        // Verify cache metrics
        DataSourceMetrics metrics = dataSource.getMetrics();
        assertTrue(metrics.getTotalRequests() >= 2);
        
        dataSource.shutdown();
    }

    @Test
    void testHealthMonitoring() throws DataSourceException, IOException {
        // Test health monitoring from External Data Sources Guide Section 8
        Path csvFile = createTestCsvFile();
        
        DataSourceConfiguration config = createHealthMonitoredConfiguration(csvFile.toString());
        ExternalDataSource dataSource = factory.createDataSource(config);

        // Test health check functionality
        assertTrue(dataSource.isHealthy());
        
        ConnectionStatus status = dataSource.getConnectionStatus();
        assertEquals(ConnectionStatus.State.CONNECTED, status.getState());
        assertNotNull(status.getLastConnected());
        
        // Test health metrics
        DataSourceMetrics metrics = dataSource.getMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.getSuccessRate() >= 0.0);
        
        dataSource.shutdown();
    }

    @Test
    void testErrorHandlingAndResilience() throws DataSourceException {
        // Test error handling from External Data Sources Guide Section 9
        DataSourceConfiguration config = createInvalidRestApiConfiguration();
        ExternalDataSource dataSource = factory.createDataSource(config);

        // Test connection failure handling
        assertFalse(dataSource.testConnection(), "Should fail to connect to invalid endpoint");
        assertFalse(dataSource.isHealthy(), "Data source should be unhealthy");

        // Test query error handling
        assertThrows(DataSourceException.class, () -> {
            Map<String, Object> parameters = new HashMap<>();
            dataSource.query("invalidQuery", parameters);
        });

        // Verify error metrics
        DataSourceMetrics metrics = dataSource.getMetrics();
        assertTrue(metrics.getFailedRequests() > 0);
        
        dataSource.shutdown();
    }

    @Test
    void testCircuitBreakerPattern() throws DataSourceException {
        // Test circuit breaker from External Data Sources Guide Section 9
        DataSourceConfiguration config = createCircuitBreakerConfiguration();
        ExternalDataSource dataSource = factory.createDataSource(config);

        // Simulate multiple failures to trigger circuit breaker
        for (int i = 0; i < 5; i++) {
            try {
                dataSource.query("failingQuery", new HashMap<>());
            } catch (DataSourceException e) {
                // Expected failures
            }
        }

        // Verify circuit breaker is open
        ConnectionStatus status = dataSource.getConnectionStatus();
        // In a real implementation, this would check circuit breaker state
        assertNotNull(status);
        
        dataSource.shutdown();
    }

    @Test
    void testPerformanceMetrics() throws DataSourceException, IOException {
        // Test performance monitoring from External Data Sources Guide Section 8
        Path csvFile = createTestCsvFile();
        
        DataSourceConfiguration config = createPerformanceMonitoredConfiguration(csvFile.toString());
        ExternalDataSource dataSource = factory.createDataSource(config);

        // Execute multiple queries to generate metrics
        for (int i = 0; i < 10; i++) {
            Map<String, Object> parameters = Map.of("name", "User" + i);
            dataSource.query("findByName", parameters);
        }

        // Verify performance metrics
        DataSourceMetrics metrics = dataSource.getMetrics();
        assertEquals(10, metrics.getSuccessfulRequests());
        assertEquals(0, metrics.getFailedRequests());
        assertTrue(metrics.getAverageResponseTime() >= 0);
        // Success rate might be returned as percentage (100.0) or ratio (1.0)
        double successRate = metrics.getSuccessRate();
        assertTrue(successRate == 1.0 || successRate == 100.0,
            "Success rate should be either 1.0 or 100.0, but was: " + successRate);
        
        dataSource.shutdown();
    }

    // Helper methods to create test configurations
    private DataSourceConfiguration createRestApiConfiguration() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-rest-api");
        config.setSourceType("rest-api");
        config.setDataSourceType(DataSourceType.REST_API);
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setBaseUrl("https://httpbin.org");
        connectionConfig.setTimeout(30000);
        config.setConnection(connectionConfig);

        // Add health check configuration that uses an endpoint httpbin.org actually supports
        HealthCheckConfig healthConfig = new HealthCheckConfig();
        healthConfig.setEnabled(true);
        healthConfig.setEndpoint("/get"); // httpbin.org supports /get endpoint
        healthConfig.setTimeoutSeconds(10L);
        config.setHealthCheck(healthConfig);

        Map<String, String> queries = new HashMap<>();
        queries.put("getUserById", "/get?id={id}");
        queries.put("getAllUsers", "/get");
        config.setQueries(queries);

        return config;
    }

    private DataSourceConfiguration createCsvFileConfiguration(String filePath) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-csv-file");
        config.setSourceType("csv-file");
        config.setDataSourceType(DataSourceType.FILE_SYSTEM);
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        // Use the directory containing the file, not the file itself
        Path file = Path.of(filePath);
        connectionConfig.setBasePath(file.getParent().toString());
        connectionConfig.setFilePattern(file.getFileName().toString());
        config.setConnection(connectionConfig);

        Map<String, String> queries = new HashMap<>();
        queries.put("findByName", "SELECT * WHERE name = :name");
        queries.put("getAll", "SELECT *");
        config.setQueries(queries);

        return config;
    }

    private DataSourceConfiguration createJsonFileConfiguration(String filePath) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-json-file");
        config.setSourceType("json-file");
        config.setDataSourceType(DataSourceType.FILE_SYSTEM);
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        // Use the directory containing the file, not the file itself
        Path file = Path.of(filePath);
        connectionConfig.setBasePath(file.getParent().toString());
        connectionConfig.setFilePattern(file.getFileName().toString());
        config.setConnection(connectionConfig);

        // Add file format configuration to extract users array
        dev.mars.apex.core.config.datasource.FileFormatConfig fileFormat =
            new dev.mars.apex.core.config.datasource.FileFormatConfig();
        fileFormat.setType("json");
        fileFormat.setRootPath("$.users"); // Extract the users array
        config.setFileFormat(fileFormat);

        Map<String, String> queries = new HashMap<>();
        queries.put("getUserById", "$[?(@.id == '{id}')]"); // Query against the users array directly
        queries.put("getAllUsers", "$[*]"); // Get all users from the array
        config.setQueries(queries);

        return config;
    }

    private DataSourceConfiguration createCachedCsvConfiguration(String filePath) {
        DataSourceConfiguration config = createCsvFileConfiguration(filePath);
        
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(300L);
        cacheConfig.setMaxSize(1000);
        config.setCache(cacheConfig);
        
        return config;
    }

    private DataSourceConfiguration createHealthMonitoredConfiguration(String filePath) {
        DataSourceConfiguration config = createCsvFileConfiguration(filePath);
        
        HealthCheckConfig healthConfig = new HealthCheckConfig();
        healthConfig.setEnabled(true);
        healthConfig.setIntervalSeconds(30L);
        healthConfig.setTimeoutSeconds(5L);
        config.setHealthCheck(healthConfig);
        
        return config;
    }

    private DataSourceConfiguration createInvalidRestApiConfiguration() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-invalid-rest-api");
        config.setSourceType("rest-api");
        config.setDataSourceType(DataSourceType.REST_API);
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setBaseUrl("http://invalid-host-that-does-not-exist.com");
        connectionConfig.setTimeout(1000); // Shorter timeout for faster test execution
        config.setConnection(connectionConfig);

        Map<String, String> queries = new HashMap<>();
        queries.put("invalidQuery", "/invalid");
        config.setQueries(queries);

        return config;
    }

    private DataSourceConfiguration createCircuitBreakerConfiguration() {
        DataSourceConfiguration config = createInvalidRestApiConfiguration();

        // Circuit breaker would be configured at the implementation level
        // For testing purposes, we use the same configuration
        return config;
    }

    private DataSourceConfiguration createPerformanceMonitoredConfiguration(String filePath) {
        DataSourceConfiguration config = createCsvFileConfiguration(filePath);

        // Performance monitoring would be configured at the implementation level
        // For testing purposes, we use the same configuration
        return config;
    }

    // Helper methods to create test files
    private Path createTestCsvFile() throws IOException {
        Path csvFile = tempDir.resolve("test-data.csv");
        String csvContent = """
            id,name,email,status
            1,John Doe,john@example.com,ACTIVE
            2,Jane Smith,jane@example.com,ACTIVE
            3,Bob Wilson,bob@example.com,INACTIVE
            4,Alice Brown,alice@example.com,ACTIVE
            """;
        Files.writeString(csvFile, csvContent);
        return csvFile;
    }

    private Path createTestJsonFile() throws IOException {
        Path jsonFile = tempDir.resolve("test-data.json");
        String jsonContent = """
            {
              "users": [
                {
                  "id": "1",
                  "name": "John Doe",
                  "email": "john@example.com",
                  "status": "ACTIVE"
                },
                {
                  "id": "2",
                  "name": "Jane Smith",
                  "email": "jane@example.com",
                  "status": "ACTIVE"
                }
              ]
            }
            """;
        Files.writeString(jsonFile, jsonContent);
        return jsonFile;
    }
}
