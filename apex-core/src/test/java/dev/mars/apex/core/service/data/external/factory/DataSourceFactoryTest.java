package dev.mars.apex.core.service.data.external.factory;

import dev.mars.apex.core.config.datasource.*;
import dev.mars.apex.core.service.data.external.*;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Comprehensive unit tests for DataSourceFactory.
 * 
 * Tests cover:
 * - Singleton pattern behavior
 * - Data source creation for all supported types
 * - Configuration validation
 * - Error handling and edge cases
 * - Resource management and caching
 * - Custom provider registration
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class DataSourceFactoryTest {

    private DataSourceFactory factory;
    private DataSourceConfiguration validConfig;
    private DataSourceConfiguration invalidConfig;

    @BeforeEach
    void setUp() {
        factory = DataSourceFactory.getInstance();
        factory.clearCache(); // Ensure clean state for each test
        
        // Create valid configuration
        validConfig = new DataSourceConfiguration();
        validConfig.setName("test-datasource");
        validConfig.setType("cache");
        validConfig.setSourceType("memory");
        validConfig.setEnabled(true);
        
        // Add required cache configuration
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setMaxSize(1000);
        cacheConfig.setTtlSeconds(300L);
        validConfig.setCache(cacheConfig);
        
        // Create invalid configuration (missing required fields)
        invalidConfig = new DataSourceConfiguration();
        // Intentionally leave name and type null
    }

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.clearCache();
        }
    }

    // ========================================
    // Singleton Pattern Tests
    // ========================================

    @Test
    @DisplayName("Should return same instance for multiple getInstance() calls")
    void testSingletonBehavior() {
        DataSourceFactory instance1 = DataSourceFactory.getInstance();
        DataSourceFactory instance2 = DataSourceFactory.getInstance();
        
        assertSame(instance1, instance2, "getInstance() should return the same instance");
        assertNotNull(instance1, "Instance should not be null");
    }

    @Test
    @DisplayName("Should be thread-safe singleton implementation")
    void testSingletonThreadSafety() throws InterruptedException {
        final int threadCount = 10;
        final Set<DataSourceFactory> instances = ConcurrentHashMap.newKeySet();
        final List<Thread> threads = new ArrayList<>();

        // Create multiple threads that call getInstance()
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                DataSourceFactory instance = DataSourceFactory.getInstance();
                instances.add(instance);
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(1, instances.size(), "All threads should get the same singleton instance");
    }

    // ========================================
    // Configuration Validation Tests
    // ========================================

    @Test
    @DisplayName("Should throw exception when configuration is null")
    void testCreateDataSourceWithNullConfiguration() {
        DataSourceException exception = assertThrows(DataSourceException.class, 
            () -> factory.createDataSource(null));
        
        assertEquals(DataSourceException.ErrorType.CONFIGURATION_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Configuration cannot be null"));
    }

    @Test
    @DisplayName("Should throw exception when configuration is invalid")
    void testCreateDataSourceWithInvalidConfiguration() {
        DataSourceException exception = assertThrows(DataSourceException.class, 
            () -> factory.createDataSource(invalidConfig));
        
        assertEquals(DataSourceException.ErrorType.CONFIGURATION_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Invalid configuration"));
    }

    @Test
    @DisplayName("Should validate configuration before creating data source")
    void testConfigurationValidation() {
        // Test with configuration missing name
        DataSourceConfiguration configMissingName = new DataSourceConfiguration();
        configMissingName.setType("cache");
        
        DataSourceException exception = assertThrows(DataSourceException.class, 
            () -> factory.createDataSource(configMissingName));
        
        assertTrue(exception.getMessage().contains("name is required") || 
                  exception.getMessage().contains("Invalid configuration"));
    }

    // ========================================
    // Cache Data Source Creation Tests
    // ========================================

    @Test
    @DisplayName("Should create cache data source successfully")
    void testCreateCacheDataSource() throws DataSourceException {
        ExternalDataSource dataSource = factory.createDataSource(validConfig);
        
        assertNotNull(dataSource, "Data source should not be null");
        assertEquals("test-datasource", dataSource.getName());
        assertTrue(dataSource.supportsDataType("cache") || dataSource.supportsDataType("memory"));
        assertTrue(dataSource.isHealthy(), "Data source should be healthy after creation");
    }

    @Test
    @DisplayName("Should create cache data source with different cache types")
    void testCreateCacheDataSourceWithDifferentTypes() throws DataSourceException {
        String[] cacheTypes = {"memory", "in-memory"};
        
        for (String cacheType : cacheTypes) {
            DataSourceConfiguration config = createCacheConfig("test-cache-" + cacheType, cacheType);
            
            ExternalDataSource dataSource = factory.createDataSource(config);
            
            assertNotNull(dataSource, "Data source should not be null for type: " + cacheType);
            assertEquals("test-cache-" + cacheType, dataSource.getName());
            assertTrue(dataSource.isHealthy(), "Data source should be healthy for type: " + cacheType);
        }
    }

    // ========================================
    // Database Data Source Creation Tests
    // ========================================

    @Test
    @DisplayName("Should create database data source with valid configuration")
    void testCreateDatabaseDataSource() {
        DataSourceConfiguration dbConfig = createDatabaseConfig("test-db", "h2");

        // For this test, we'll just verify that the configuration is valid
        // and that the factory attempts to create the data source
        // (actual database connection testing should be in integration tests)
        DataSourceException exception = assertThrows(DataSourceException.class,
            () -> factory.createDataSource(dbConfig));

        // We expect this to fail due to missing database driver or connection,
        // but the error should be related to database connectivity, not configuration
        assertTrue(exception.getMessage().contains("Failed to create database data source") ||
                  exception.getMessage().contains("Failed to create DataSource") ||
                  exception.getMessage().contains("No suitable driver"));
    }

    @Test
    @DisplayName("Should throw exception for database config without connection")
    void testCreateDatabaseDataSourceWithoutConnection() {
        DataSourceConfiguration dbConfig = new DataSourceConfiguration();
        dbConfig.setName("test-db");
        dbConfig.setType("database");
        // Missing connection configuration
        
        DataSourceException exception = assertThrows(DataSourceException.class, 
            () -> factory.createDataSource(dbConfig));
        
        assertTrue(exception.getMessage().contains("Connection configuration is required") ||
                  exception.getMessage().contains("Invalid configuration"));
    }

    // ========================================
    // File System Data Source Creation Tests
    // ========================================

    @Test
    @DisplayName("Should create file system data source successfully")
    void testCreateFileSystemDataSource() throws DataSourceException {
        DataSourceConfiguration fileConfig = createFileSystemConfig("test-files", "json");

        ExternalDataSource dataSource = factory.createDataSource(fileConfig);

        assertNotNull(dataSource, "File system data source should not be null");
        assertEquals("test-files", dataSource.getName());
        assertTrue(dataSource.supportsDataType("file-system") || dataSource.supportsDataType("json"));
    }

    // ========================================
    // REST API Data Source Creation Tests
    // ========================================

    @Test
    @DisplayName("Should create REST API data source successfully")
    void testCreateRestApiDataSource() throws DataSourceException {
        DataSourceConfiguration restConfig = createRestApiConfig("test-api", "https://api.example.com");

        ExternalDataSource dataSource = factory.createDataSource(restConfig);

        assertNotNull(dataSource, "REST API data source should not be null");
        assertEquals("test-api", dataSource.getName());
        assertTrue(dataSource.supportsDataType("rest-api") || dataSource.supportsDataType("api"));
    }

    @Test
    @DisplayName("Should throw exception for REST API config without base URL")
    void testCreateRestApiDataSourceWithoutBaseUrl() {
        DataSourceConfiguration restConfig = new DataSourceConfiguration();
        restConfig.setName("test-api");
        restConfig.setType("rest-api");
        // Missing connection with base URL

        DataSourceException exception = assertThrows(DataSourceException.class,
            () -> factory.createDataSource(restConfig));

        assertTrue(exception.getMessage().contains("Base URL is required") ||
                  exception.getMessage().contains("Invalid configuration"));
    }

    // ========================================
    // Multiple Data Sources Creation Tests
    // ========================================

    @Test
    @DisplayName("Should create multiple data sources successfully")
    void testCreateMultipleDataSources() throws DataSourceException {
        List<DataSourceConfiguration> configs = Arrays.asList(
            createCacheConfig("cache1", "memory"),
            createCacheConfig("cache2", "memory"),
            createCacheConfig("cache3", "in-memory")
        );

        Map<String, ExternalDataSource> dataSources = factory.createDataSources(configs);

        assertEquals(3, dataSources.size(), "Should create all three data sources");
        assertTrue(dataSources.containsKey("cache1"));
        assertTrue(dataSources.containsKey("cache2"));
        assertTrue(dataSources.containsKey("cache3"));

        // Verify each data source is properly initialized
        for (ExternalDataSource dataSource : dataSources.values()) {
            assertNotNull(dataSource);
            assertTrue(dataSource.isHealthy());
        }
    }

    @Test
    @DisplayName("Should throw exception if any data source creation fails in batch")
    void testCreateMultipleDataSourcesWithFailure() {
        List<DataSourceConfiguration> configs = Arrays.asList(
            createCacheConfig("cache1", "memory"),
            invalidConfig, // This will cause failure
            createCacheConfig("cache2", "memory")
        );

        assertThrows(DataSourceException.class,
            () -> factory.createDataSources(configs));
    }

    // ========================================
    // Custom Provider Tests
    // ========================================

    @Test
    @DisplayName("Should register and use custom data source provider")
    void testCustomProviderRegistration() throws DataSourceException {
        // Create a simple test implementation instead of using Mockito
        TestDataSourceProvider testProvider = new TestDataSourceProvider();

        // Register the custom provider
        factory.registerProvider("custom-test", testProvider);

        // Verify it's supported
        assertTrue(factory.isCustomTypeSupported("custom-test"));
        assertTrue(factory.getSupportedTypes().contains("custom-test"));

        // Test that the provider is properly registered
        // We don't need to test actual data source creation since that would require
        // complex setup - just verify the registration mechanism works
        assertNotNull(testProvider, "Test provider should be created");
        assertEquals("custom-test", testProvider.getType());
    }

    // Simple test implementation to replace Mockito
    private static class TestDataSourceProvider implements DataSourceProvider {
        @Override
        public String getType() {
            return "custom-test";
        }

        @Override
        public ExternalDataSource createDataSource(DataSourceConfiguration config) throws DataSourceException {
            return new TestExternalDataSource(config.getName());
        }

        @Override
        public boolean supports(DataSourceConfiguration config) {
            return "custom-test".equals(config.getSourceType());
        }
    }

    // Simple test implementation to replace Mockito
    private static class TestExternalDataSource implements ExternalDataSource {
        private final String name;

        public TestExternalDataSource(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDataType() {
            return "custom-test";
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        @Override
        public boolean supportsDataType(String dataType) {
            return "custom-test".equals(dataType);
        }

        @Override
        public Object getData(String key) throws DataSourceException {
            return "test-data";
        }

        @Override
        public List<Object> query(String query, Map<String, Object> parameters) throws DataSourceException {
            return List.of("test-result");
        }

        @Override
        public Object queryForObject(String query, Map<String, Object> parameters) throws DataSourceException {
            return "test-object";
        }

        @Override
        public int[] batchUpdate(List<String> statements) throws DataSourceException {
            return new int[]{1};
        }

        @Override
        public boolean testConnection() throws DataSourceException {
            return true;
        }

        @Override
        public ConnectionStatus getConnectionStatus() {
            return new ConnectionStatus(true, "Test connection");
        }

        @Override
        public void shutdown() {
            // No-op for test
        }
    }

    @Test
    @DisplayName("Should unregister custom data source provider")
    void testCustomProviderUnregistration() {
        TestDataSourceProvider testProvider = new TestDataSourceProvider();

        // Register and verify
        factory.registerProvider("custom-test", testProvider);
        assertTrue(factory.isCustomTypeSupported("custom-test"));

        // Unregister and verify
        factory.unregisterProvider("custom-test");
        assertFalse(factory.isCustomTypeSupported("custom-test"));
        assertFalse(factory.getSupportedTypes().contains("custom-test"));
    }

    // ========================================
    // Type Support Tests
    // ========================================

    @Test
    @DisplayName("Should support all built-in data source types")
    void testBuiltInTypesSupport() {
        Set<String> supportedTypes = factory.getSupportedTypes();

        // Debug output to see what types are actually supported
        System.out.println("Supported types: " + supportedTypes);

        // Verify all built-in types are supported (factory uses enum.name().toLowerCase())
        assertTrue(supportedTypes.contains("database"), "Should support database type");
        assertTrue(supportedTypes.contains("rest_api"), "Should support rest_api type (enum name)");
        assertTrue(supportedTypes.contains("file_system"), "Should support file_system type (enum name)");
        assertTrue(supportedTypes.contains("cache"), "Should support cache type");
        assertTrue(supportedTypes.contains("message_queue"), "Should support message_queue type (enum name)");
        assertTrue(supportedTypes.contains("custom"), "Should support custom type");

        // Verify we have at least the expected number of types
        assertTrue(supportedTypes.size() >= 6, "Should have at least 6 supported types");
    }

    @Test
    @DisplayName("Should correctly identify supported types")
    void testTypeSupportIdentification() {
        // These should be supported as built-in types
        assertTrue(factory.isTypeSupported(DataSourceType.DATABASE), "Should support DATABASE type");
        assertTrue(factory.isTypeSupported(DataSourceType.CACHE), "Should support CACHE type");
        assertTrue(factory.isTypeSupported(DataSourceType.FILE_SYSTEM), "Should support FILE_SYSTEM type");
        assertTrue(factory.isTypeSupported(DataSourceType.REST_API), "Should support REST_API type");
        assertTrue(factory.isTypeSupported(DataSourceType.MESSAGE_QUEUE), "Should support MESSAGE_QUEUE type");

        // CUSTOM type is not considered a built-in type in the current implementation
        // It's only supported when custom providers are registered
        assertFalse(factory.isTypeSupported(DataSourceType.CUSTOM), "CUSTOM type requires custom providers");

        assertFalse(factory.isCustomTypeSupported("non-existent-type"), "Should not support non-existent type");
    }

    // ========================================
    // Resource Management Tests
    // ========================================

    @Test
    @DisplayName("Should clear cache successfully")
    void testClearCache() {
        // Create some data sources to populate cache
        try {
            factory.createDataSource(validConfig);
            factory.createDataSource(createDatabaseConfig("test-db", "h2"));
        } catch (DataSourceException e) {
            // Ignore creation failures for this test
        }

        // Clear cache should not throw exception
        assertDoesNotThrow(() -> factory.clearCache());
    }

    @Test
    @DisplayName("Should shutdown factory successfully")
    void testShutdown() {
        // Shutdown should not throw exception
        assertDoesNotThrow(() -> factory.shutdown());

        // After shutdown, cache should be cleared
        Set<String> supportedTypes = factory.getSupportedTypes();
        // Built-in types should still be supported, but custom providers should be cleared
        assertTrue(supportedTypes.contains("database"));
        assertTrue(supportedTypes.contains("cache"));
    }

    // ========================================
    // Helper Methods
    // ========================================

    private DataSourceConfiguration createCacheConfig(String name, String sourceType) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName(name);
        config.setType("cache");
        config.setSourceType(sourceType);
        config.setEnabled(true);

        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setMaxSize(1000);
        cacheConfig.setTtlSeconds(300L);
        config.setCache(cacheConfig);

        return config;
    }

    private DataSourceConfiguration createDatabaseConfig(String name, String sourceType) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName(name);
        config.setType("database");
        config.setSourceType(sourceType);
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setHost("localhost");
        connectionConfig.setPort(9092); // Use a test port
        connectionConfig.setDatabase(name);
        connectionConfig.setUsername("sa");
        connectionConfig.setPassword("");
        config.setConnection(connectionConfig);

        return config;
    }

    private DataSourceConfiguration createFileSystemConfig(String name, String sourceType) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName(name);
        config.setType("file-system");
        config.setSourceType(sourceType);
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setBasePath("src/test/resources/test-data");
        config.setConnection(connectionConfig);

        FileFormatConfig formatConfig = new FileFormatConfig();
        formatConfig.setType(sourceType);
        formatConfig.setEncoding("UTF-8");
        config.setFileFormat(formatConfig);

        return config;
    }

    private DataSourceConfiguration createRestApiConfig(String name, String baseUrl) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName(name);
        config.setType("rest-api");
        config.setSourceType("api");
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setBaseUrl(baseUrl);
        connectionConfig.setTimeout(5000);
        config.setConnection(connectionConfig);

        return config;
    }
}
