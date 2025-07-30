package dev.mars.apex.core.service.data.external.integration;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.data.external.manager.DataSourceManager;
import dev.mars.apex.core.service.data.external.registry.DataSourceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for end-to-end data source workflows.
 * 
 * These tests verify the complete integration between all components
 * including factory, registry, manager, and actual data source implementations.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
class DataSourceIntegrationTest {
    
    @TempDir
    Path tempDir;
    
    private DataSourceFactory factory;
    private DataSourceRegistry registry;
    private DataSourceManager manager;
    
    @BeforeEach
    void setUp() {
        factory = DataSourceFactory.getInstance();
        registry = DataSourceRegistry.getInstance();
        manager = new DataSourceManager(registry, factory);
        
        // Clear any existing registrations
        for (String name : registry.getDataSourceNames()) {
            registry.unregister(name);
        }
        
        factory.clearCache();
    }
    
    @AfterEach
    void tearDown() {
        if (manager.isRunning()) {
            manager.shutdown();
        }
        
        // Clear registrations
        for (String name : registry.getDataSourceNames()) {
            registry.unregister(name);
        }
        
        factory.clearCache();
    }
    
    @Test
    void testFileSystemDataSourceIntegration() throws DataSourceException, IOException {
        // Arrange - Create test CSV file
        Path csvFile = tempDir.resolve("test-data.csv");
        String csvContent = "id,name,email\n1,John Doe,john@example.com\n2,Jane Smith,jane@example.com";
        Files.writeString(csvFile, csvContent);
        
        // Create file system configuration
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-file-source");
        config.setSourceType("file-system");
        config.setDataSourceType(DataSourceType.FILE_SYSTEM);
        
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setBasePath(tempDir.toString());
        connectionConfig.setFilePattern("*.csv");
        config.setConnection(connectionConfig);
        
        // Act - Initialize manager with file system data source
        manager.initialize(List.of(config));
        
        // Assert - Verify data source is registered and accessible
        assertTrue(manager.isInitialized());
        assertTrue(manager.isRunning());
        assertEquals(1, registry.size());
        
        ExternalDataSource dataSource = manager.getDataSource("test-file-source");
        assertNotNull(dataSource);
        assertEquals(DataSourceType.FILE_SYSTEM, dataSource.getSourceType());
        assertTrue(dataSource.isHealthy());
        
        // Test data retrieval
        Object data = dataSource.getData("csv", "test-data.csv");
        assertNotNull(data);
    }
    
    @Test
    void testCacheDataSourceIntegration() throws DataSourceException {
        // Arrange - Create cache configuration
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-cache-source");
        config.setSourceType("memory");
        config.setDataSourceType(DataSourceType.CACHE);
        
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setMaxSize(1000);
        cacheConfig.setTtlSeconds(300L);
        config.setCache(cacheConfig);
        
        // Act - Initialize manager with cache data source
        manager.initialize(List.of(config));
        
        // Assert - Verify data source is registered and accessible
        assertTrue(manager.isInitialized());
        assertTrue(manager.isRunning());
        assertEquals(1, registry.size());
        
        ExternalDataSource dataSource = manager.getDataSource("test-cache-source");
        assertNotNull(dataSource);
        assertEquals(DataSourceType.CACHE, dataSource.getSourceType());
        assertTrue(dataSource.isHealthy());
        
        // Test cache operations through query interface
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("key", "test-key");
        
        // Query should return empty initially
        List<Object> results = dataSource.query("test-*", parameters);
        assertNotNull(results);
    }
    
    @Test
    void testMultipleDataSourcesIntegration() throws DataSourceException, IOException {
        // Arrange - Create multiple data source configurations
        
        // File system data source
        Path csvFile = tempDir.resolve("users.csv");
        Files.writeString(csvFile, "id,name\n1,John\n2,Jane");
        
        DataSourceConfiguration fileConfig = new DataSourceConfiguration();
        fileConfig.setName("file-source");
        fileConfig.setSourceType("file-system");
        fileConfig.setDataSourceType(DataSourceType.FILE_SYSTEM);
        
        ConnectionConfig fileConnection = new ConnectionConfig();
        fileConnection.setBasePath(tempDir.toString());
        fileConnection.setFilePattern("*.csv");
        fileConfig.setConnection(fileConnection);
        
        // Cache data source
        DataSourceConfiguration cacheConfig = new DataSourceConfiguration();
        cacheConfig.setName("cache-source");
        cacheConfig.setSourceType("memory");
        cacheConfig.setDataSourceType(DataSourceType.CACHE);
        
        CacheConfig cache = new CacheConfig();
        cache.setEnabled(true);
        cache.setMaxSize(500);
        cacheConfig.setCache(cache);
        
        // Act - Initialize manager with multiple data sources
        manager.initialize(List.of(fileConfig, cacheConfig));
        
        // Assert - Verify all data sources are registered
        assertTrue(manager.isInitialized());
        assertTrue(manager.isRunning());
        assertEquals(2, registry.size());
        
        // Verify individual data sources
        ExternalDataSource fileSource = manager.getDataSource("file-source");
        assertNotNull(fileSource);
        assertEquals(DataSourceType.FILE_SYSTEM, fileSource.getSourceType());
        
        ExternalDataSource cacheSource = manager.getDataSource("cache-source");
        assertNotNull(cacheSource);
        assertEquals(DataSourceType.CACHE, cacheSource.getSourceType());
        
        // Verify data sources by type
        List<ExternalDataSource> fileSources = manager.getDataSourcesByType(DataSourceType.FILE_SYSTEM);
        assertEquals(1, fileSources.size());
        
        List<ExternalDataSource> cacheSources = manager.getDataSourcesByType(DataSourceType.CACHE);
        assertEquals(1, cacheSources.size());
        
        // Test load balancing
        ExternalDataSource fileSourceLB = manager.getDataSourceWithLoadBalancing(DataSourceType.FILE_SYSTEM);
        assertEquals(fileSource, fileSourceLB);
    }
    
    @Test
    void testDataSourceHealthMonitoring() throws DataSourceException, InterruptedException {
        // Arrange - Create configuration
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("health-test-source");
        config.setSourceType("memory");
        config.setDataSourceType(DataSourceType.CACHE);
        
        // Act - Initialize manager
        manager.initialize(List.of(config));
        
        // Assert - Verify initial health
        ExternalDataSource dataSource = manager.getDataSource("health-test-source");
        assertNotNull(dataSource);
        assertTrue(dataSource.isHealthy());
        
        // Verify healthy data sources
        List<ExternalDataSource> healthySources = manager.getHealthyDataSources();
        assertEquals(1, healthySources.size());
        assertEquals(dataSource, healthySources.get(0));
        
        List<ExternalDataSource> unhealthySources = manager.getUnhealthyDataSources();
        assertEquals(0, unhealthySources.size());
    }
    
    @Test
    void testDataSourceFailover() throws DataSourceException, IOException {
        // Arrange - Create multiple file system data sources
        Path csvFile1 = tempDir.resolve("data1.csv");
        Path csvFile2 = tempDir.resolve("data2.csv");
        Files.writeString(csvFile1, "id,name\n1,John");
        Files.writeString(csvFile2, "id,name\n2,Jane");
        
        DataSourceConfiguration config1 = createFileSystemConfig("file-source-1", tempDir);
        DataSourceConfiguration config2 = createFileSystemConfig("file-source-2", tempDir);
        
        // Act - Initialize manager with multiple data sources
        manager.initialize(List.of(config1, config2));
        
        // Assert - Test failover query
        Map<String, Object> parameters = new HashMap<>();
        
        try {
            List<Object> results = manager.queryWithFailover(DataSourceType.FILE_SYSTEM, "*.csv", parameters);
            assertNotNull(results);
            // Results depend on which data source responds first
        } catch (DataSourceException e) {
            // Expected if no healthy data sources available
            assertTrue(e.getMessage().contains("No healthy data sources available"));
        }
    }
    
    @Test
    void testDataSourceRefresh() throws DataSourceException, IOException {
        // Arrange - Create file system data source
        Path csvFile = tempDir.resolve("refresh-test.csv");
        Files.writeString(csvFile, "id,name\n1,Original");
        
        DataSourceConfiguration config = createFileSystemConfig("refresh-source", tempDir);
        
        // Act - Initialize and refresh
        manager.initialize(List.of(config));
        manager.refreshAll();
        
        // Assert - Verify refresh completed without errors
        ExternalDataSource dataSource = manager.getDataSource("refresh-source");
        assertNotNull(dataSource);
        assertTrue(dataSource.isHealthy());
    }
    
    @Test
    void testDataSourceStatistics() throws DataSourceException {
        // Arrange - Create multiple data sources
        DataSourceConfiguration cacheConfig = new DataSourceConfiguration();
        cacheConfig.setName("stats-cache");
        cacheConfig.setSourceType("memory");
        cacheConfig.setDataSourceType(DataSourceType.CACHE);
        
        CacheConfig cache = new CacheConfig();
        cache.setEnabled(true);
        cacheConfig.setCache(cache);
        
        // Act - Initialize manager
        manager.initialize(List.of(cacheConfig));
        
        // Assert - Verify statistics
        var managerStats = manager.getStatistics();
        assertNotNull(managerStats);
        assertNotNull(managerStats.getRegistryStatistics());
        assertEquals(1, managerStats.getRegistryStatistics().getTotalDataSources());
        
        var registryStats = registry.getStatistics();
        assertNotNull(registryStats);
        assertEquals(1, registryStats.getTotalDataSources());
        assertTrue(registryStats.isAllHealthy());
    }
    
    @Test
    void testDataSourceAddRemove() throws DataSourceException {
        // Arrange - Initialize empty manager
        manager.initialize(List.of());
        assertEquals(0, registry.size());
        
        // Act - Add data source
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("dynamic-source");
        config.setSourceType("memory");
        config.setDataSourceType(DataSourceType.CACHE);
        
        manager.addDataSource(config);
        
        // Assert - Verify addition
        assertEquals(1, registry.size());
        assertNotNull(manager.getDataSource("dynamic-source"));
        
        // Act - Remove data source
        boolean removed = manager.removeDataSource("dynamic-source");
        
        // Assert - Verify removal
        assertTrue(removed);
        assertEquals(0, registry.size());
        assertNull(manager.getDataSource("dynamic-source"));
    }
    
    @Test
    void testDataSourceShutdown() throws DataSourceException {
        // Arrange - Initialize manager with data source
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("shutdown-test");
        config.setSourceType("memory");
        config.setDataSourceType(DataSourceType.CACHE);
        
        manager.initialize(List.of(config));
        assertTrue(manager.isRunning());
        
        // Act - Shutdown manager
        manager.shutdown();
        
        // Assert - Verify shutdown
        assertFalse(manager.isRunning());
        assertFalse(manager.isInitialized());
    }
    
    /**
     * Helper method to create file system configuration.
     */
    private DataSourceConfiguration createFileSystemConfig(String name, Path basePath) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName(name);
        config.setSourceType("file-system");
        config.setDataSourceType(DataSourceType.FILE_SYSTEM);
        
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setBasePath(basePath.toString());
        connectionConfig.setFilePattern("*.csv");
        config.setConnection(connectionConfig);
        
        return config;
    }
}
