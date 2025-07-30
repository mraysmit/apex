package dev.mars.apex.core.service.data.external.factory;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DataSourceFactory.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class DataSourceFactoryTest {
    
    @Mock
    private DataSourceProvider mockProvider;
    
    @Mock
    private ExternalDataSource mockDataSource;
    
    private DataSourceFactory factory;
    private DataSourceConfiguration databaseConfig;
    private DataSourceConfiguration restApiConfig;
    private DataSourceConfiguration fileSystemConfig;
    private DataSourceConfiguration cacheConfig;
    
    @BeforeEach
    void setUp() {
        factory = DataSourceFactory.getInstance();
        
        // Clear any existing custom providers
        factory.clearCache();
        
        // Setup database configuration
        databaseConfig = new DataSourceConfiguration();
        databaseConfig.setName("test-database");
        databaseConfig.setSourceType("postgresql");
        databaseConfig.setDataSourceType(DataSourceType.DATABASE);
        
        ConnectionConfig dbConnection = new ConnectionConfig();
        dbConnection.setHost("localhost");
        dbConnection.setPort(5432);
        dbConnection.setDatabase("testdb");
        dbConnection.setUsername("testuser");
        dbConnection.setPassword("testpass");
        databaseConfig.setConnection(dbConnection);
        
        // Setup REST API configuration
        restApiConfig = new DataSourceConfiguration();
        restApiConfig.setName("test-api");
        restApiConfig.setSourceType("rest-api");
        restApiConfig.setDataSourceType(DataSourceType.REST_API);
        
        ConnectionConfig apiConnection = new ConnectionConfig();
        apiConnection.setBaseUrl("https://api.example.com");
        apiConnection.setTimeout(30000L);
        restApiConfig.setConnection(apiConnection);
        
        // Setup file system configuration
        fileSystemConfig = new DataSourceConfiguration();
        fileSystemConfig.setName("test-files");
        fileSystemConfig.setSourceType("file-system");
        fileSystemConfig.setDataSourceType(DataSourceType.FILE_SYSTEM);
        
        ConnectionConfig fileConnection = new ConnectionConfig();
        fileConnection.setBasePath("/data/files");
        fileConnection.setFilePattern("*.csv");
        fileSystemConfig.setConnection(fileConnection);
        
        // Setup cache configuration
        cacheConfig = new DataSourceConfiguration();
        cacheConfig.setName("test-cache");
        cacheConfig.setSourceType("memory");
        cacheConfig.setDataSourceType(DataSourceType.CACHE);
    }
    
    @Test
    void testSingletonInstance() {
        // Act
        DataSourceFactory instance1 = DataSourceFactory.getInstance();
        DataSourceFactory instance2 = DataSourceFactory.getInstance();
        
        // Assert
        assertSame(instance1, instance2);
    }
    
    @Test
    void testCreateDataSource_Database() throws DataSourceException {
        // Act
        ExternalDataSource dataSource = factory.createDataSource(databaseConfig);
        
        // Assert
        assertNotNull(dataSource);
        assertEquals(DataSourceType.DATABASE, dataSource.getSourceType());
        assertEquals("test-database", dataSource.getName());
    }
    
    @Test
    void testCreateDataSource_RestApi() throws DataSourceException {
        // Act
        ExternalDataSource dataSource = factory.createDataSource(restApiConfig);
        
        // Assert
        assertNotNull(dataSource);
        assertEquals(DataSourceType.REST_API, dataSource.getSourceType());
        assertEquals("test-api", dataSource.getName());
    }
    
    @Test
    void testCreateDataSource_FileSystem() throws DataSourceException {
        // Act
        ExternalDataSource dataSource = factory.createDataSource(fileSystemConfig);
        
        // Assert
        assertNotNull(dataSource);
        assertEquals(DataSourceType.FILE_SYSTEM, dataSource.getSourceType());
        assertEquals("test-files", dataSource.getName());
    }
    
    @Test
    void testCreateDataSource_Cache() throws DataSourceException {
        // Act
        ExternalDataSource dataSource = factory.createDataSource(cacheConfig);
        
        // Assert
        assertNotNull(dataSource);
        assertEquals(DataSourceType.CACHE, dataSource.getSourceType());
        assertEquals("test-cache", dataSource.getName());
    }
    
    @Test
    void testCreateDataSource_NullConfiguration() {
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            factory.createDataSource(null);
        });
    }
    
    @Test
    void testCreateDataSource_InvalidConfiguration() {
        // Arrange
        DataSourceConfiguration invalidConfig = new DataSourceConfiguration();
        // Missing required fields
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            factory.createDataSource(invalidConfig);
        });
    }
    
    @Test
    void testCreateDataSource_UnsupportedType() {
        // Arrange
        DataSourceConfiguration unsupportedConfig = new DataSourceConfiguration();
        unsupportedConfig.setName("test-unsupported");
        unsupportedConfig.setSourceType("unsupported");
        unsupportedConfig.setDataSourceType(DataSourceType.MESSAGE_QUEUE); // Not implemented yet
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            factory.createDataSource(unsupportedConfig);
        });
    }
    
    @Test
    void testCreateDataSources_Success() throws DataSourceException {
        // Arrange
        List<DataSourceConfiguration> configurations = List.of(databaseConfig, restApiConfig);
        
        // Act
        Map<String, ExternalDataSource> dataSources = factory.createDataSources(configurations);
        
        // Assert
        assertEquals(2, dataSources.size());
        assertTrue(dataSources.containsKey("test-database"));
        assertTrue(dataSources.containsKey("test-api"));
        
        ExternalDataSource dbSource = dataSources.get("test-database");
        assertEquals(DataSourceType.DATABASE, dbSource.getSourceType());
        
        ExternalDataSource apiSource = dataSources.get("test-api");
        assertEquals(DataSourceType.REST_API, apiSource.getSourceType());
    }
    
    @Test
    void testCreateDataSources_PartialFailure() {
        // Arrange
        DataSourceConfiguration invalidConfig = new DataSourceConfiguration();
        // Missing required fields
        
        List<DataSourceConfiguration> configurations = List.of(databaseConfig, invalidConfig);
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            factory.createDataSources(configurations);
        });
    }
    
    @Test
    void testRegisterProvider_Success() throws DataSourceException {
        // Arrange
        when(mockProvider.getType()).thenReturn("custom");
        when(mockProvider.createDataSource(any())).thenReturn(mockDataSource);
        when(mockDataSource.getName()).thenReturn("test-custom");
        
        // Act
        factory.registerProvider("custom", mockProvider);
        
        // Assert
        assertTrue(factory.isCustomTypeSupported("custom"));
        
        // Test creating with custom provider
        DataSourceConfiguration customConfig = new DataSourceConfiguration();
        customConfig.setName("test-custom");
        customConfig.setSourceType("custom");
        customConfig.setDataSourceType(DataSourceType.CUSTOM);
        customConfig.setImplementation("custom");
        
        ExternalDataSource dataSource = factory.createDataSource(customConfig);
        assertEquals(mockDataSource, dataSource);
    }
    
    @Test
    void testUnregisterProvider() {
        // Arrange
        when(mockProvider.getType()).thenReturn("custom");
        factory.registerProvider("custom", mockProvider);
        
        // Act
        factory.unregisterProvider("custom");
        
        // Assert
        assertFalse(factory.isCustomTypeSupported("custom"));
    }
    
    @Test
    void testIsTypeSupported() {
        // Act & Assert
        assertTrue(factory.isTypeSupported(DataSourceType.DATABASE));
        assertTrue(factory.isTypeSupported(DataSourceType.REST_API));
        assertTrue(factory.isTypeSupported(DataSourceType.FILE_SYSTEM));
        assertTrue(factory.isTypeSupported(DataSourceType.CACHE));
        assertFalse(factory.isTypeSupported(DataSourceType.MESSAGE_QUEUE)); // Not implemented yet
    }
    
    @Test
    void testIsCustomTypeSupported() {
        // Arrange
        when(mockProvider.getType()).thenReturn("custom");
        factory.registerProvider("custom", mockProvider);
        
        // Act & Assert
        assertTrue(factory.isCustomTypeSupported("custom"));
        assertFalse(factory.isCustomTypeSupported("non-existent"));
    }
    
    @Test
    void testGetSupportedTypes() {
        // Arrange
        when(mockProvider.getType()).thenReturn("custom");
        factory.registerProvider("custom", mockProvider);
        
        // Act
        Set<String> supportedTypes = factory.getSupportedTypes();
        
        // Assert
        assertTrue(supportedTypes.contains("database"));
        assertTrue(supportedTypes.contains("rest_api"));
        assertTrue(supportedTypes.contains("file_system"));
        assertTrue(supportedTypes.contains("cache"));
        assertTrue(supportedTypes.contains("custom"));
    }
    
    @Test
    void testClearCache() throws DataSourceException {
        // Arrange - Create a data source to populate cache
        factory.createDataSource(databaseConfig);
        
        // Act
        factory.clearCache();
        
        // Assert - Should not throw exception
        assertDoesNotThrow(() -> factory.clearCache());
    }
    
    @Test
    void testShutdown() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> factory.shutdown());
    }
    
    @Test
    void testCreateDataSource_CustomProvider_NotFound() {
        // Arrange
        DataSourceConfiguration customConfig = new DataSourceConfiguration();
        customConfig.setName("test-custom");
        customConfig.setSourceType("custom");
        customConfig.setDataSourceType(DataSourceType.CUSTOM);
        customConfig.setImplementation("non-existent");
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            factory.createDataSource(customConfig);
        });
    }
    
    @Test
    void testCreateDataSource_CustomProvider_NoImplementation() {
        // Arrange
        DataSourceConfiguration customConfig = new DataSourceConfiguration();
        customConfig.setName("test-custom");
        customConfig.setSourceType("custom");
        customConfig.setDataSourceType(DataSourceType.CUSTOM);
        // Missing implementation field
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            factory.createDataSource(customConfig);
        });
    }
    
    @Test
    void testResourceCaching() throws DataSourceException {
        // Act - Create two database data sources with same connection details
        ExternalDataSource dataSource1 = factory.createDataSource(databaseConfig);
        ExternalDataSource dataSource2 = factory.createDataSource(databaseConfig);
        
        // Assert - Both should be created successfully (caching is internal)
        assertNotNull(dataSource1);
        assertNotNull(dataSource2);
        assertEquals(dataSource1.getName(), dataSource2.getName());
    }
}
