package dev.mars.apex.core.service.data.external.registry;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DataSourceRegistry.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class DataSourceRegistryTest {
    
    @Mock
    private ExternalDataSource mockDataSource1;
    
    @Mock
    private ExternalDataSource mockDataSource2;
    
    @Mock
    private DataSourceConfiguration mockConfig1;
    
    @Mock
    private DataSourceConfiguration mockConfig2;
    
    private DataSourceRegistry registry;
    
    @BeforeEach
    void setUp() {
        // Create a new registry instance for each test
        registry = DataSourceRegistry.getInstance();
        
        // Clear any existing registrations
        for (String name : registry.getDataSourceNames()) {
            registry.unregister(name);
        }
        
        // Setup mock data sources
        when(mockDataSource1.getName()).thenReturn("test-source-1");
        when(mockDataSource1.getSourceType()).thenReturn(DataSourceType.DATABASE);
        when(mockDataSource1.getDataType()).thenReturn("postgresql");
        when(mockDataSource1.isHealthy()).thenReturn(true);
        when(mockDataSource1.getConfiguration()).thenReturn(mockConfig1);
        
        when(mockDataSource2.getName()).thenReturn("test-source-2");
        when(mockDataSource2.getSourceType()).thenReturn(DataSourceType.REST_API);
        when(mockDataSource2.getDataType()).thenReturn("rest-api");
        when(mockDataSource2.isHealthy()).thenReturn(true);
        when(mockDataSource2.getConfiguration()).thenReturn(mockConfig2);
        
        // Setup mock configurations
        when(mockConfig1.getTags()).thenReturn(List.of("production", "primary"));
        when(mockConfig2.getTags()).thenReturn(List.of("development", "secondary"));
    }
    
    @Test
    void testSingletonInstance() {
        // Act
        DataSourceRegistry instance1 = DataSourceRegistry.getInstance();
        DataSourceRegistry instance2 = DataSourceRegistry.getInstance();
        
        // Assert
        assertSame(instance1, instance2);
    }
    
    @Test
    void testRegister_Success() throws DataSourceException {
        // Act
        registry.register(mockDataSource1);
        
        // Assert
        assertTrue(registry.isRegistered("test-source-1"));
        assertEquals(1, registry.size());
        assertEquals(mockDataSource1, registry.getDataSource("test-source-1"));
    }
    
    @Test
    void testRegister_NullDataSource() {
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            registry.register(null);
        });
    }
    
    @Test
    void testRegister_NullName() throws DataSourceException {
        // Arrange
        when(mockDataSource1.getName()).thenReturn(null);
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            registry.register(mockDataSource1);
        });
    }
    
    @Test
    void testRegister_DuplicateName() throws DataSourceException {
        // Arrange
        registry.register(mockDataSource1);
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            registry.register(mockDataSource1);
        });
    }
    
    @Test
    void testUnregister_Success() throws DataSourceException {
        // Arrange
        registry.register(mockDataSource1);
        
        // Act
        boolean result = registry.unregister("test-source-1");
        
        // Assert
        assertTrue(result);
        assertFalse(registry.isRegistered("test-source-1"));
        assertEquals(0, registry.size());
        
        // Verify shutdown was called
        verify(mockDataSource1).shutdown();
    }
    
    @Test
    void testUnregister_NotFound() {
        // Act
        boolean result = registry.unregister("non-existent");
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void testUnregister_NullName() {
        // Act
        boolean result = registry.unregister(null);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void testGetDataSource_Success() throws DataSourceException {
        // Arrange
        registry.register(mockDataSource1);
        
        // Act
        ExternalDataSource result = registry.getDataSource("test-source-1");
        
        // Assert
        assertEquals(mockDataSource1, result);
    }
    
    @Test
    void testGetDataSource_NotFound() {
        // Act
        ExternalDataSource result = registry.getDataSource("non-existent");
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testGetDataSourceNames() throws DataSourceException {
        // Arrange
        registry.register(mockDataSource1);
        registry.register(mockDataSource2);
        
        // Act
        Set<String> names = registry.getDataSourceNames();
        
        // Assert
        assertEquals(2, names.size());
        assertTrue(names.contains("test-source-1"));
        assertTrue(names.contains("test-source-2"));
    }
    
    @Test
    void testGetDataSourcesByType() throws DataSourceException {
        // Arrange
        registry.register(mockDataSource1);
        registry.register(mockDataSource2);
        
        // Act
        List<ExternalDataSource> databaseSources = registry.getDataSourcesByType(DataSourceType.DATABASE);
        List<ExternalDataSource> apiSources = registry.getDataSourcesByType(DataSourceType.REST_API);
        
        // Assert
        assertEquals(1, databaseSources.size());
        assertEquals(mockDataSource1, databaseSources.get(0));
        
        assertEquals(1, apiSources.size());
        assertEquals(mockDataSource2, apiSources.get(0));
    }
    
    @Test
    void testGetDataSourcesByTag() throws DataSourceException {
        // Arrange
        registry.register(mockDataSource1);
        registry.register(mockDataSource2);
        
        // Act
        List<ExternalDataSource> productionSources = registry.getDataSourcesByTag("production");
        List<ExternalDataSource> developmentSources = registry.getDataSourcesByTag("development");
        
        // Assert
        assertEquals(1, productionSources.size());
        assertEquals(mockDataSource1, productionSources.get(0));
        
        assertEquals(1, developmentSources.size());
        assertEquals(mockDataSource2, developmentSources.get(0));
    }
    
    @Test
    void testGetHealthyDataSources() throws DataSourceException {
        // Arrange
        when(mockDataSource2.isHealthy()).thenReturn(false);
        registry.register(mockDataSource1);
        registry.register(mockDataSource2);
        
        // Act
        List<ExternalDataSource> healthySources = registry.getHealthyDataSources();
        
        // Assert
        assertEquals(1, healthySources.size());
        assertEquals(mockDataSource1, healthySources.get(0));
    }
    
    @Test
    void testGetUnhealthyDataSources() throws DataSourceException {
        // Arrange
        when(mockDataSource2.isHealthy()).thenReturn(false);
        registry.register(mockDataSource1);
        registry.register(mockDataSource2);
        
        // Act
        List<ExternalDataSource> unhealthySources = registry.getUnhealthyDataSources();
        
        // Assert
        assertEquals(1, unhealthySources.size());
        assertEquals(mockDataSource2, unhealthySources.get(0));
    }
    
    @Test
    void testGetStatistics() throws DataSourceException {
        // Arrange
        when(mockDataSource2.isHealthy()).thenReturn(false);
        registry.register(mockDataSource1);
        registry.register(mockDataSource2);
        
        // Act
        RegistryStatistics stats = registry.getStatistics();
        
        // Assert
        assertEquals(2, stats.getTotalDataSources());
        assertEquals(1, stats.getHealthyDataSources());
        assertEquals(1, stats.getUnhealthyDataSources());
        assertEquals(50.0, stats.getHealthPercentage(), 0.1);
        assertEquals(1, stats.getCountByType(DataSourceType.DATABASE));
        assertEquals(1, stats.getCountByType(DataSourceType.REST_API));
    }
    
    @Test
    void testEventListener() throws DataSourceException, InterruptedException {
        // Arrange
        CountDownLatch latch = new CountDownLatch(1);
        DataSourceRegistryEvent capturedEvent[] = new DataSourceRegistryEvent[1];
        
        DataSourceRegistryListener listener = event -> {
            capturedEvent[0] = event;
            latch.countDown();
        };
        
        registry.addListener(listener);
        
        // Act
        registry.register(mockDataSource1);
        
        // Assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(capturedEvent[0]);
        assertEquals(DataSourceRegistryEvent.EventType.REGISTERED, capturedEvent[0].getEventType());
        assertEquals("test-source-1", capturedEvent[0].getDataSourceName());
    }
    
    @Test
    void testRemoveListener() throws DataSourceException, InterruptedException {
        // Arrange
        CountDownLatch latch = new CountDownLatch(1);
        DataSourceRegistryListener listener = event -> latch.countDown();
        
        registry.addListener(listener);
        registry.removeListener(listener);
        
        // Act
        registry.register(mockDataSource1);
        
        // Assert
        assertFalse(latch.await(100, TimeUnit.MILLISECONDS));
    }
    
    @Test
    void testRefreshAll() throws DataSourceException {
        // Arrange
        registry.register(mockDataSource1);
        registry.register(mockDataSource2);
        
        // Act
        registry.refreshAll();
        
        // Assert
        verify(mockDataSource1).refresh();
        verify(mockDataSource2).refresh();
    }
    
    @Test
    void testIsRegistered() throws DataSourceException {
        // Arrange
        registry.register(mockDataSource1);
        
        // Act & Assert
        assertTrue(registry.isRegistered("test-source-1"));
        assertFalse(registry.isRegistered("non-existent"));
        assertFalse(registry.isRegistered(null));
    }
    
    @Test
    void testSize() throws DataSourceException {
        // Assert initial state
        assertEquals(0, registry.size());
        
        // Add data sources
        registry.register(mockDataSource1);
        assertEquals(1, registry.size());
        
        registry.register(mockDataSource2);
        assertEquals(2, registry.size());
        
        // Remove data source
        registry.unregister("test-source-1");
        assertEquals(1, registry.size());
    }
}
