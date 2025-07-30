package dev.mars.apex.core.service.data.external.database;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.data.external.ConnectionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DatabaseDataSource.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class DatabaseDataSourceTest {
    
    @Mock
    private DataSource mockDataSource;
    
    @Mock
    private Connection mockConnection;
    
    @Mock
    private PreparedStatement mockStatement;
    
    @Mock
    private ResultSet mockResultSet;
    
    @Mock
    private ResultSetMetaData mockMetaData;
    
    private DataSourceConfiguration configuration;
    private DatabaseDataSource databaseDataSource;
    
    @BeforeEach
    void setUp() throws SQLException {
        // Setup configuration
        configuration = new DataSourceConfiguration();
        configuration.setName("test-database");
        configuration.setSourceType("postgresql");
        configuration.setDataSourceType(DataSourceType.DATABASE);
        
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setHost("localhost");
        connectionConfig.setPort(5432);
        connectionConfig.setDatabase("testdb");
        connectionConfig.setUsername("testuser");
        connectionConfig.setPassword("testpass");
        configuration.setConnection(connectionConfig);
        
        // Setup queries
        Map<String, String> queries = new HashMap<>();
        queries.put("users", "SELECT * FROM users WHERE id = :id");
        queries.put("default", "SELECT 1");
        configuration.setQueries(queries);
        
        // Setup parameter names
        configuration.setParameterNames(new String[]{"id", "name"});
        
        // Setup mock behaviors
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
        
        // Create instance
        databaseDataSource = new DatabaseDataSource(mockDataSource, configuration);
    }
    
    @Test
    void testInitialize_Success() throws DataSourceException, SQLException {
        // Act
        databaseDataSource.initialize(configuration);
        
        // Assert
        assertEquals(DataSourceType.DATABASE, databaseDataSource.getSourceType());
        assertEquals("test-database", databaseDataSource.getName());
        assertEquals("postgresql", databaseDataSource.getDataType());
        assertTrue(databaseDataSource.supportsDataType("postgresql"));
        
        // Verify connection was tested
        verify(mockDataSource, atLeastOnce()).getConnection();
    }
    
    @Test
    void testInitialize_ConnectionFailure() throws SQLException {
        // Arrange
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            databaseDataSource.initialize(configuration);
        });
    }
    
    @Test
    void testTestConnection_Success() throws SQLException {
        // Arrange
        databaseDataSource.initialize(configuration);
        
        // Act
        boolean result = databaseDataSource.testConnection();
        
        // Assert
        assertTrue(result);
        verify(mockDataSource, atLeastOnce()).getConnection();
    }
    
    @Test
    void testTestConnection_Failure() throws SQLException {
        // Arrange
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));
        
        // Act
        boolean result = databaseDataSource.testConnection();
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void testGetData_WithoutCache() throws DataSourceException, SQLException {
        // Arrange
        databaseDataSource.initialize(configuration);
        
        // Setup result set
        when(mockMetaData.getColumnCount()).thenReturn(2);
        when(mockMetaData.getColumnLabel(1)).thenReturn("id");
        when(mockMetaData.getColumnLabel(2)).thenReturn("name");
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getObject(1)).thenReturn(1);
        when(mockResultSet.getObject(2)).thenReturn("John");
        
        // Act
        Object result = databaseDataSource.getData("users", 1);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals(1, resultMap.get("id"));
        assertEquals("John", resultMap.get("name"));
    }
    
    @Test
    void testGetData_WithCache() throws DataSourceException, SQLException {
        // Arrange
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(300L);
        configuration.setCache(cacheConfig);
        
        databaseDataSource.initialize(configuration);
        
        // Setup result set
        when(mockMetaData.getColumnCount()).thenReturn(1);
        when(mockMetaData.getColumnLabel(1)).thenReturn("id");
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getObject(1)).thenReturn(1);
        
        // Act - First call should hit database
        Object result1 = databaseDataSource.getData("users", 1);
        
        // Act - Second call should hit cache
        Object result2 = databaseDataSource.getData("users", 1);
        
        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1, result2);
        
        // Verify database was called only once (second call used cache)
        verify(mockStatement, times(1)).executeQuery();
    }
    
    @Test
    void testQuery_Success() throws DataSourceException, SQLException {
        // Arrange
        databaseDataSource.initialize(configuration);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 1);
        
        // Setup result set
        when(mockMetaData.getColumnCount()).thenReturn(2);
        when(mockMetaData.getColumnLabel(1)).thenReturn("id");
        when(mockMetaData.getColumnLabel(2)).thenReturn("name");
        when(mockResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(mockResultSet.getObject(1)).thenReturn(1, 2);
        when(mockResultSet.getObject(2)).thenReturn("John", "Jane");
        
        // Act
        List<Object> results = databaseDataSource.query("SELECT * FROM users", parameters);
        
        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> firstResult = (Map<String, Object>) results.get(0);
        assertEquals(1, firstResult.get("id"));
        assertEquals("John", firstResult.get("name"));
    }
    
    @Test
    void testQuery_SQLException() throws SQLException {
        // Arrange
        databaseDataSource.initialize(configuration);
        when(mockStatement.executeQuery()).thenThrow(new SQLException("Query failed"));
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 1);
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            databaseDataSource.query("SELECT * FROM users", parameters);
        });
    }
    
    @Test
    void testQueryForObject_Success() throws DataSourceException, SQLException {
        // Arrange
        databaseDataSource.initialize(configuration);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 1);
        
        // Setup result set
        when(mockMetaData.getColumnCount()).thenReturn(1);
        when(mockMetaData.getColumnLabel(1)).thenReturn("id");
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getObject(1)).thenReturn(1);
        
        // Act
        Object result = databaseDataSource.queryForObject("SELECT * FROM users WHERE id = :id", parameters);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals(1, resultMap.get("id"));
    }
    
    @Test
    void testQueryForObject_NoResults() throws DataSourceException, SQLException {
        // Arrange
        databaseDataSource.initialize(configuration);
        when(mockResultSet.next()).thenReturn(false);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 999);
        
        // Act
        Object result = databaseDataSource.queryForObject("SELECT * FROM users WHERE id = :id", parameters);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testGetConnectionStatus() throws DataSourceException {
        // Act
        ConnectionStatus status = databaseDataSource.getConnectionStatus();
        
        // Assert
        assertNotNull(status);
        assertEquals(ConnectionStatus.State.NOT_INITIALIZED, status.getState());
        
        // After initialization
        databaseDataSource.initialize(configuration);
        status = databaseDataSource.getConnectionStatus();
        assertEquals(ConnectionStatus.State.CONNECTED, status.getState());
    }
    
    @Test
    void testGetMetrics() throws DataSourceException {
        // Arrange
        databaseDataSource.initialize(configuration);
        
        // Act
        var metrics = databaseDataSource.getMetrics();
        
        // Assert
        assertNotNull(metrics);
        assertEquals(0, metrics.getSuccessfulRequests());
        assertEquals(0, metrics.getFailedRequests());
    }
    
    @Test
    void testRefresh() throws DataSourceException {
        // Arrange
        databaseDataSource.initialize(configuration);
        
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> databaseDataSource.refresh());
    }
    
    @Test
    void testShutdown() throws DataSourceException {
        // Arrange
        databaseDataSource.initialize(configuration);
        
        // Act
        databaseDataSource.shutdown();
        
        // Assert
        ConnectionStatus status = databaseDataSource.getConnectionStatus();
        assertEquals(ConnectionStatus.State.SHUTDOWN, status.getState());
    }
    
    @Test
    void testSupportsDataType() throws DataSourceException {
        // Arrange
        databaseDataSource.initialize(configuration);
        
        // Act & Assert
        assertTrue(databaseDataSource.supportsDataType("database"));
        assertTrue(databaseDataSource.supportsDataType("postgresql"));
        assertFalse(databaseDataSource.supportsDataType("rest-api"));
    }
}
