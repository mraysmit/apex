package dev.mars.apex.core.service.data.external.manager;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.data.external.registry.DataSourceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DataSourceManager.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class DataSourceManagerTest {
    
    @Mock
    private DataSourceRegistry mockRegistry;
    
    @Mock
    private DataSourceFactory mockFactory;
    
    @Mock
    private ExternalDataSource mockDataSource1;
    
    @Mock
    private ExternalDataSource mockDataSource2;
    
    @Mock
    private DataSourceConfiguration mockConfig1;
    
    @Mock
    private DataSourceConfiguration mockConfig2;
    
    private DataSourceManager manager;
    
    @BeforeEach
    void setUp() throws DataSourceException {
        // Setup mock data sources
        when(mockDataSource1.getName()).thenReturn("test-source-1");
        when(mockDataSource1.getSourceType()).thenReturn(DataSourceType.DATABASE);
        when(mockDataSource1.isHealthy()).thenReturn(true);
        when(mockDataSource1.getConfiguration()).thenReturn(mockConfig1);
        
        when(mockDataSource2.getName()).thenReturn("test-source-2");
        when(mockDataSource2.getSourceType()).thenReturn(DataSourceType.REST_API);
        when(mockDataSource2.isHealthy()).thenReturn(true);
        when(mockDataSource2.getConfiguration()).thenReturn(mockConfig2);
        
        // Setup mock configurations
        when(mockConfig1.getName()).thenReturn("test-source-1");
        when(mockConfig2.getName()).thenReturn("test-source-2");
        
        // Setup mock factory
        when(mockFactory.createDataSource(mockConfig1)).thenReturn(mockDataSource1);
        when(mockFactory.createDataSource(mockConfig2)).thenReturn(mockDataSource2);
        
        // Setup mock registry
        when(mockRegistry.getDataSourceNames()).thenReturn(Set.of());
        when(mockRegistry.size()).thenReturn(0);
        
        // Create manager with mocks
        manager = new DataSourceManager(mockRegistry, mockFactory);
    }
    
    @Test
    void testInitialize_Success() throws DataSourceException {
        // Arrange
        List<DataSourceConfiguration> configurations = List.of(mockConfig1, mockConfig2);
        
        // Act
        manager.initialize(configurations);
        
        // Assert
        assertTrue(manager.isInitialized());
        assertTrue(manager.isRunning());
        
        // Verify factory and registry interactions
        verify(mockFactory).createDataSource(mockConfig1);
        verify(mockFactory).createDataSource(mockConfig2);
        verify(mockRegistry).register(mockDataSource1);
        verify(mockRegistry).register(mockDataSource2);
    }
    
    @Test
    void testInitialize_AlreadyInitialized() throws DataSourceException {
        // Arrange
        List<DataSourceConfiguration> configurations = List.of(mockConfig1);
        manager.initialize(configurations);
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            manager.initialize(configurations);
        });
    }
    
    @Test
    void testInitialize_FactoryFailure() throws DataSourceException {
        // Arrange
        when(mockFactory.createDataSource(mockConfig1)).thenThrow(new DataSourceException(
            DataSourceException.ErrorType.CONFIGURATION_ERROR, "Factory failed"));
        
        List<DataSourceConfiguration> configurations = List.of(mockConfig1);
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            manager.initialize(configurations);
        });
        
        assertFalse(manager.isInitialized());
        assertFalse(manager.isRunning());
    }
    
    @Test
    void testAddDataSource_Success() throws DataSourceException {
        // Arrange
        manager.initialize(List.of());
        
        // Act
        manager.addDataSource(mockConfig1);
        
        // Assert
        verify(mockFactory).createDataSource(mockConfig1);
        verify(mockRegistry).register(mockDataSource1);
    }
    
    @Test
    void testAddDataSource_NotRunning() {
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            manager.addDataSource(mockConfig1);
        });
    }
    
    @Test
    void testRemoveDataSource_Success() throws DataSourceException {
        // Arrange
        manager.initialize(List.of(mockConfig1));
        when(mockRegistry.unregister("test-source-1")).thenReturn(true);
        
        // Act
        boolean result = manager.removeDataSource("test-source-1");
        
        // Assert
        assertTrue(result);
        verify(mockRegistry).unregister("test-source-1");
    }
    
    @Test
    void testRemoveDataSource_NotRunning() throws DataSourceException {
        // Act
        boolean result = manager.removeDataSource("test-source-1");
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void testGetDataSource() throws DataSourceException {
        // Arrange
        manager.initialize(List.of());
        when(mockRegistry.getDataSource("test-source-1")).thenReturn(mockDataSource1);
        
        // Act
        ExternalDataSource result = manager.getDataSource("test-source-1");
        
        // Assert
        assertEquals(mockDataSource1, result);
        verify(mockRegistry).getDataSource("test-source-1");
    }
    
    @Test
    void testGetDataSourceNames() throws DataSourceException {
        // Arrange
        manager.initialize(List.of());
        when(mockRegistry.getDataSourceNames()).thenReturn(Set.of("test-source-1", "test-source-2"));
        
        // Act
        Set<String> names = manager.getDataSourceNames();
        
        // Assert
        assertEquals(2, names.size());
        assertTrue(names.contains("test-source-1"));
        assertTrue(names.contains("test-source-2"));
    }
    
    @Test
    void testGetDataSourcesByType() throws DataSourceException {
        // Arrange
        manager.initialize(List.of());
        when(mockRegistry.getDataSourcesByType(DataSourceType.DATABASE))
            .thenReturn(List.of(mockDataSource1));
        
        // Act
        List<ExternalDataSource> sources = manager.getDataSourcesByType(DataSourceType.DATABASE);
        
        // Assert
        assertEquals(1, sources.size());
        assertEquals(mockDataSource1, sources.get(0));
    }
    
    @Test
    void testGetDataSourceWithLoadBalancing() throws DataSourceException {
        // Arrange
        manager.initialize(List.of(mockConfig1, mockConfig2));
        
        // Mock registry to return data sources by type
        when(mockRegistry.getDataSourceNames()).thenReturn(Set.of("test-source-1", "test-source-2"));
        when(mockRegistry.getDataSource("test-source-1")).thenReturn(mockDataSource1);
        when(mockRegistry.getDataSource("test-source-2")).thenReturn(mockDataSource2);
        
        // Act
        ExternalDataSource result1 = manager.getDataSourceWithLoadBalancing(DataSourceType.DATABASE);
        ExternalDataSource result2 = manager.getDataSourceWithLoadBalancing(DataSourceType.DATABASE);
        
        // Assert - Should return data sources (load balancing logic is internal)
        assertNotNull(result1);
        assertNotNull(result2);
    }
    
    @Test
    void testQueryWithFailover_Success() throws DataSourceException {
        // Arrange
        manager.initialize(List.of());
        when(mockRegistry.getDataSourcesByType(DataSourceType.DATABASE))
            .thenReturn(List.of(mockDataSource1));
        when(mockDataSource1.query(anyString(), any())).thenReturn(List.of("result"));
        
        Map<String, Object> parameters = new HashMap<>();
        
        // Act
        List<Object> results = manager.queryWithFailover(DataSourceType.DATABASE, "SELECT 1", parameters);
        
        // Assert
        assertEquals(1, results.size());
        assertEquals("result", results.get(0));
        verify(mockDataSource1).query("SELECT 1", parameters);
    }
    
    @Test
    void testQueryWithFailover_NoHealthyDataSources() throws DataSourceException {
        // Arrange
        manager.initialize(List.of());
        when(mockRegistry.getDataSourcesByType(DataSourceType.DATABASE))
            .thenReturn(List.of());
        
        Map<String, Object> parameters = new HashMap<>();
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            manager.queryWithFailover(DataSourceType.DATABASE, "SELECT 1", parameters);
        });
    }
    
    @Test
    void testQueryWithFailover_AllDataSourcesFail() throws DataSourceException {
        // Arrange
        manager.initialize(List.of());
        when(mockRegistry.getDataSourcesByType(DataSourceType.DATABASE))
            .thenReturn(List.of(mockDataSource1));
        when(mockDataSource1.query(anyString(), any()))
            .thenThrow(new DataSourceException(DataSourceException.ErrorType.EXECUTION_ERROR, "Query failed"));
        
        Map<String, Object> parameters = new HashMap<>();
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            manager.queryWithFailover(DataSourceType.DATABASE, "SELECT 1", parameters);
        });
    }
    
    @Test
    void testQueryAsync_Success() throws Exception {
        // Arrange
        manager.initialize(List.of());
        when(mockRegistry.getDataSource("test-source-1")).thenReturn(mockDataSource1);
        when(mockDataSource1.query(anyString(), any())).thenReturn(List.of("result"));
        
        Map<String, Object> parameters = new HashMap<>();
        
        // Act
        CompletableFuture<List<Object>> future = manager.queryAsync("test-source-1", "SELECT 1", parameters);
        List<Object> results = future.get(1, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(1, results.size());
        assertEquals("result", results.get(0));
    }
    
    @Test
    void testQueryAsync_DataSourceNotFound() throws Exception {
        // Arrange
        manager.initialize(List.of());
        when(mockRegistry.getDataSource("non-existent")).thenReturn(null);
        
        Map<String, Object> parameters = new HashMap<>();
        
        // Act
        CompletableFuture<List<Object>> future = manager.queryAsync("non-existent", "SELECT 1", parameters);
        
        // Assert
        assertThrows(Exception.class, () -> {
            future.get(1, TimeUnit.SECONDS);
        });
    }
    
    @Test
    void testGetHealthyDataSourcesByType() throws DataSourceException {
        // Arrange
        manager.initialize(List.of());
        when(mockDataSource2.isHealthy()).thenReturn(false);
        when(mockRegistry.getDataSourcesByType(DataSourceType.DATABASE))
            .thenReturn(List.of(mockDataSource1, mockDataSource2));
        
        // Act
        List<ExternalDataSource> healthySources = manager.getHealthyDataSourcesByType(DataSourceType.DATABASE);
        
        // Assert
        assertEquals(1, healthySources.size());
        assertEquals(mockDataSource1, healthySources.get(0));
    }
    
    @Test
    void testGetHealthyDataSources() throws DataSourceException {
        // Arrange
        manager.initialize(List.of());
        when(mockRegistry.getHealthyDataSources()).thenReturn(List.of(mockDataSource1));
        
        // Act
        List<ExternalDataSource> healthySources = manager.getHealthyDataSources();
        
        // Assert
        assertEquals(1, healthySources.size());
        assertEquals(mockDataSource1, healthySources.get(0));
    }
    
    @Test
    void testGetUnhealthyDataSources() throws DataSourceException {
        // Arrange
        manager.initialize(List.of());
        when(mockRegistry.getUnhealthyDataSources()).thenReturn(List.of(mockDataSource2));
        
        // Act
        List<ExternalDataSource> unhealthySources = manager.getUnhealthyDataSources();
        
        // Assert
        assertEquals(1, unhealthySources.size());
        assertEquals(mockDataSource2, unhealthySources.get(0));
    }
    
    @Test
    void testRefreshAll() throws DataSourceException {
        // Arrange
        manager.initialize(List.of());
        
        // Act
        manager.refreshAll();
        
        // Assert
        verify(mockRegistry).refreshAll();
    }
    
    @Test
    void testGetStatistics() throws DataSourceException {
        // Arrange
        manager.initialize(List.of());
        
        // Act
        DataSourceManagerStatistics stats = manager.getStatistics();
        
        // Assert
        assertNotNull(stats);
        assertNotNull(stats.getRegistryStatistics());
    }
    
    @Test
    void testEventListener() throws DataSourceException, InterruptedException {
        // Arrange
        CountDownLatch latch = new CountDownLatch(1);
        DataSourceManagerEvent capturedEvent[] = new DataSourceManagerEvent[1];
        
        DataSourceManagerListener listener = event -> {
            capturedEvent[0] = event;
            latch.countDown();
        };
        
        manager.addListener(listener);
        
        // Act
        manager.initialize(List.of(mockConfig1));
        
        // Assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(capturedEvent[0]);
        assertEquals(DataSourceManagerEvent.EventType.INITIALIZED, capturedEvent[0].getEventType());
    }
    
    @Test
    void testRemoveListener() throws DataSourceException, InterruptedException {
        // Arrange
        CountDownLatch latch = new CountDownLatch(1);
        DataSourceManagerListener listener = event -> latch.countDown();
        
        manager.addListener(listener);
        manager.removeListener(listener);
        
        // Act
        manager.initialize(List.of(mockConfig1));
        
        // Assert
        assertFalse(latch.await(100, TimeUnit.MILLISECONDS));
    }
    
    @Test
    void testShutdown() throws DataSourceException {
        // Arrange
        manager.initialize(List.of(mockConfig1));
        
        // Act
        manager.shutdown();
        
        // Assert
        assertFalse(manager.isRunning());
        assertFalse(manager.isInitialized());
        verify(mockRegistry).shutdown();
    }
}
