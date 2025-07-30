package dev.mars.apex.core.service.data.external.integration;

import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.config.yaml.YamlDataSourceLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.config.DataSourceConfigurationService;
import dev.mars.apex.core.service.data.external.manager.DataSourceManager;
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
 * Integration tests for YAML configuration loading and data source integration.
 * 
 * These tests verify the complete integration between YAML configuration
 * and the data source management system.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
class YamlConfigurationIntegrationTest {
    
    @TempDir
    Path tempDir;
    
    private YamlDataSourceLoader yamlLoader;
    private DataSourceConfigurationService configService;
    private DataSourceManager manager;
    
    @BeforeEach
    void setUp() {
        manager = new DataSourceManager();
        yamlLoader = new YamlDataSourceLoader(manager);
        configService = DataSourceConfigurationService.getInstance();
        
        // Clear any existing state
        if (configService.isRunning()) {
            configService.shutdown();
        }
    }
    
    @AfterEach
    void tearDown() {
        if (yamlLoader.isRunning()) {
            yamlLoader.shutdown();
        }
        
        if (configService.isRunning()) {
            configService.shutdown();
        }
        
        if (manager.isRunning()) {
            manager.shutdown();
        }
    }
    
    @Test
    void testYamlFileSystemDataSourceLoading() throws DataSourceException, IOException {
        // Arrange - Create test data file
        Path csvFile = tempDir.resolve("test-data.csv");
        Files.writeString(csvFile, "id,name,status\n1,John,active\n2,Jane,inactive");
        
        // Create YAML configuration
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
        
        YamlDataSource yamlDataSource = new YamlDataSource();
        yamlDataSource.setName("yaml-file-source");
        yamlDataSource.setType("file-system");
        yamlDataSource.setBasePath(tempDir.toString());
        yamlDataSource.setFilePattern("*.csv");
        yamlDataSource.setEnabled(true);
        
        // Set file format
        Map<String, Object> fileFormat = new HashMap<>();
        fileFormat.put("type", "csv");
        fileFormat.put("hasHeaderRow", true);
        fileFormat.put("delimiter", ",");
        yamlDataSource.setFileFormat(fileFormat);
        
        yamlConfig.setDataSources(List.of(yamlDataSource));
        
        // Act - Load data sources from YAML
        yamlLoader.loadDataSources(yamlConfig);
        
        // Assert - Verify data source was loaded
        assertTrue(yamlLoader.isInitialized());
        assertTrue(yamlLoader.isRunning());
        
        DataSourceManager loadedManager = yamlLoader.getDataSourceManager();
        ExternalDataSource dataSource = loadedManager.getDataSource("yaml-file-source");
        
        assertNotNull(dataSource);
        assertEquals("yaml-file-source", dataSource.getName());
        assertEquals(DataSourceType.FILE_SYSTEM, dataSource.getSourceType());
        assertTrue(dataSource.isHealthy());
    }
    
    @Test
    void testYamlCacheDataSourceLoading() throws DataSourceException {
        // Arrange - Create YAML configuration
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
        
        YamlDataSource yamlDataSource = new YamlDataSource();
        yamlDataSource.setName("yaml-cache-source");
        yamlDataSource.setType("cache");
        yamlDataSource.setSourceType("memory");
        yamlDataSource.setEnabled(true);
        
        // Set cache configuration
        Map<String, Object> cache = new HashMap<>();
        cache.put("enabled", true);
        cache.put("maxSize", 1000);
        cache.put("ttlSeconds", 300);
        yamlDataSource.setCache(cache);
        
        yamlConfig.setDataSources(List.of(yamlDataSource));
        
        // Act - Load data sources from YAML
        yamlLoader.loadDataSources(yamlConfig);
        
        // Assert - Verify data source was loaded
        assertTrue(yamlLoader.isInitialized());
        
        DataSourceManager loadedManager = yamlLoader.getDataSourceManager();
        ExternalDataSource dataSource = loadedManager.getDataSource("yaml-cache-source");
        
        assertNotNull(dataSource);
        assertEquals("yaml-cache-source", dataSource.getName());
        assertEquals(DataSourceType.CACHE, dataSource.getSourceType());
        assertTrue(dataSource.isHealthy());
    }
    
    @Test
    void testYamlMultipleDataSourcesLoading() throws DataSourceException, IOException {
        // Arrange - Create test files
        Path csvFile = tempDir.resolve("users.csv");
        Files.writeString(csvFile, "id,name\n1,John\n2,Jane");
        
        Path jsonFile = tempDir.resolve("config.json");
        Files.writeString(jsonFile, "{\"setting\": \"value\"}");
        
        // Create YAML configuration with multiple data sources
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
        
        // File system data source
        YamlDataSource fileDataSource = new YamlDataSource();
        fileDataSource.setName("file-source");
        fileDataSource.setType("file-system");
        fileDataSource.setBasePath(tempDir.toString());
        fileDataSource.setFilePattern("*.csv");
        fileDataSource.setEnabled(true);
        
        // Cache data source
        YamlDataSource cacheDataSource = new YamlDataSource();
        cacheDataSource.setName("cache-source");
        cacheDataSource.setType("cache");
        cacheDataSource.setSourceType("memory");
        cacheDataSource.setEnabled(true);
        
        Map<String, Object> cache = new HashMap<>();
        cache.put("enabled", true);
        cache.put("maxSize", 500);
        cacheDataSource.setCache(cache);
        
        yamlConfig.setDataSources(List.of(fileDataSource, cacheDataSource));
        
        // Act - Load data sources from YAML
        yamlLoader.loadDataSources(yamlConfig);
        
        // Assert - Verify both data sources were loaded
        DataSourceManager loadedManager = yamlLoader.getDataSourceManager();
        
        ExternalDataSource fileSource = loadedManager.getDataSource("file-source");
        assertNotNull(fileSource);
        assertEquals(DataSourceType.FILE_SYSTEM, fileSource.getSourceType());
        
        ExternalDataSource cacheSource = loadedManager.getDataSource("cache-source");
        assertNotNull(cacheSource);
        assertEquals(DataSourceType.CACHE, cacheSource.getSourceType());
        
        // Verify manager statistics
        var stats = loadedManager.getStatistics();
        assertEquals(2, stats.getRegistryStatistics().getTotalDataSources());
    }
    
    @Test
    void testYamlConfigurationServiceIntegration() throws DataSourceException {
        // Arrange - Create YAML configuration
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
        
        YamlDataSource yamlDataSource = new YamlDataSource();
        yamlDataSource.setName("service-test-source");
        yamlDataSource.setType("cache");
        yamlDataSource.setSourceType("memory");
        yamlDataSource.setEnabled(true);
        
        Map<String, Object> cache = new HashMap<>();
        cache.put("enabled", true);
        cache.put("maxSize", 100);
        yamlDataSource.setCache(cache);
        
        yamlConfig.setDataSources(List.of(yamlDataSource));
        
        // Act - Initialize configuration service
        configService.initialize(yamlConfig);
        
        // Assert - Verify service initialization
        assertTrue(configService.isInitialized());
        assertTrue(configService.isRunning());
        
        // Verify data source is accessible through service
        ExternalDataSource dataSource = configService.getDataSource("service-test-source");
        assertNotNull(dataSource);
        assertEquals("service-test-source", dataSource.getName());
        
        // Verify configuration is tracked
        var config = configService.getConfiguration("service-test-source");
        assertNotNull(config);
        assertEquals("service-test-source", config.getName());
    }
    
    @Test
    void testYamlConfigurationReloading() throws DataSourceException {
        // Arrange - Create initial YAML configuration
        YamlRuleConfiguration initialConfig = new YamlRuleConfiguration();
        
        YamlDataSource initialDataSource = new YamlDataSource();
        initialDataSource.setName("reload-test-source");
        initialDataSource.setType("cache");
        initialDataSource.setSourceType("memory");
        initialDataSource.setEnabled(true);
        
        initialConfig.setDataSources(List.of(initialDataSource));
        
        // Initialize service
        configService.initialize(initialConfig);
        assertEquals(1, configService.getConfigurationNames().size());
        
        // Arrange - Create new YAML configuration
        YamlRuleConfiguration newConfig = new YamlRuleConfiguration();
        
        YamlDataSource newDataSource1 = new YamlDataSource();
        newDataSource1.setName("reload-source-1");
        newDataSource1.setType("cache");
        newDataSource1.setSourceType("memory");
        newDataSource1.setEnabled(true);
        
        YamlDataSource newDataSource2 = new YamlDataSource();
        newDataSource2.setName("reload-source-2");
        newDataSource2.setType("cache");
        newDataSource2.setSourceType("memory");
        newDataSource2.setEnabled(true);
        
        newConfig.setDataSources(List.of(newDataSource1, newDataSource2));
        
        // Act - Reload configuration
        configService.reloadFromYaml(newConfig);
        
        // Assert - Verify new configuration is loaded
        assertEquals(2, configService.getConfigurationNames().size());
        assertNotNull(configService.getDataSource("reload-source-1"));
        assertNotNull(configService.getDataSource("reload-source-2"));
        assertNull(configService.getDataSource("reload-test-source")); // Old source should be gone
    }
    
    @Test
    void testYamlConfigurationValidation() throws DataSourceException {
        // Arrange - Create valid YAML data source
        YamlDataSource validDataSource = new YamlDataSource();
        validDataSource.setName("valid-source");
        validDataSource.setType("cache");
        validDataSource.setSourceType("memory");
        validDataSource.setEnabled(true);
        
        // Act & Assert - Valid configuration should pass
        assertDoesNotThrow(() -> {
            yamlLoader.validateConfiguration(validDataSource);
        });
        
        // Arrange - Create invalid YAML data source (missing name)
        YamlDataSource invalidDataSource = new YamlDataSource();
        invalidDataSource.setType("cache");
        invalidDataSource.setEnabled(true);
        // Missing name
        
        // Act & Assert - Invalid configuration should fail
        assertThrows(DataSourceException.class, () -> {
            yamlLoader.validateConfiguration(invalidDataSource);
        });
    }
    
    @Test
    void testYamlDataSourceWithTags() throws DataSourceException {
        // Arrange - Create YAML configuration with tags
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
        
        YamlDataSource yamlDataSource = new YamlDataSource();
        yamlDataSource.setName("tagged-source");
        yamlDataSource.setType("cache");
        yamlDataSource.setSourceType("memory");
        yamlDataSource.setEnabled(true);
        yamlDataSource.setTags(List.of("production", "primary", "cache"));
        
        yamlConfig.setDataSources(List.of(yamlDataSource));
        
        // Act - Load data sources from YAML
        configService.initialize(yamlConfig);
        
        // Assert - Verify tags are preserved
        var config = configService.getConfiguration("tagged-source");
        assertNotNull(config);
        assertNotNull(config.getTags());
        assertEquals(3, config.getTags().size());
        assertTrue(config.getTags().contains("production"));
        assertTrue(config.getTags().contains("primary"));
        assertTrue(config.getTags().contains("cache"));
    }
    
    @Test
    void testYamlDataSourceDisabled() throws DataSourceException {
        // Arrange - Create YAML configuration with disabled data source
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
        
        YamlDataSource enabledDataSource = new YamlDataSource();
        enabledDataSource.setName("enabled-source");
        enabledDataSource.setType("cache");
        enabledDataSource.setSourceType("memory");
        enabledDataSource.setEnabled(true);
        
        YamlDataSource disabledDataSource = new YamlDataSource();
        disabledDataSource.setName("disabled-source");
        disabledDataSource.setType("cache");
        disabledDataSource.setSourceType("memory");
        disabledDataSource.setEnabled(false); // Disabled
        
        yamlConfig.setDataSources(List.of(enabledDataSource, disabledDataSource));
        
        // Act - Load data sources from YAML
        configService.initialize(yamlConfig);
        
        // Assert - Only enabled data source should be loaded
        assertNotNull(configService.getDataSource("enabled-source"));
        assertNull(configService.getDataSource("disabled-source"));
        assertEquals(1, configService.getConfigurationNames().size());
    }
    
    @Test
    void testYamlConfigurationErrorHandling() {
        // Arrange - Create YAML configuration with invalid data source
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
        
        YamlDataSource invalidDataSource = new YamlDataSource();
        // Missing required fields
        invalidDataSource.setEnabled(true);
        
        yamlConfig.setDataSources(List.of(invalidDataSource));
        
        // Act & Assert - Should throw exception for invalid configuration
        assertThrows(DataSourceException.class, () -> {
            configService.initialize(yamlConfig);
        });
        
        // Verify service is not initialized
        assertFalse(configService.isInitialized());
    }
}
