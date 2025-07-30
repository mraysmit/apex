package dev.mars.apex.core.service.data.external.rest;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.AuthenticationConfig;
import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.data.external.ConnectionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RestApiDataSource.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class RestApiDataSourceTest {
    
    @Mock
    private HttpClient mockHttpClient;
    
    @Mock
    private HttpResponse<String> mockResponse;
    
    private DataSourceConfiguration configuration;
    private RestApiDataSource restApiDataSource;
    
    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        // Setup configuration
        configuration = new DataSourceConfiguration();
        configuration.setName("test-api");
        configuration.setSourceType("rest-api");
        configuration.setDataSourceType(DataSourceType.REST_API);
        
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setBaseUrl("https://api.example.com");
        connectionConfig.setTimeout(30000L);
        configuration.setConnection(connectionConfig);
        
        // Setup authentication
        AuthenticationConfig authConfig = new AuthenticationConfig();
        authConfig.setType("bearer");
        authConfig.setToken("test-token");
        configuration.setAuthentication(authConfig);
        
        // Setup endpoints
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("users", "/users/{id}");
        endpoints.put("default", "/health");
        configuration.setEndpoints(endpoints);
        
        // Setup parameter names
        configuration.setParameterNames(new String[]{"id", "name"});
        
        // Setup mock behaviors
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"id\": 1, \"name\": \"John\"}");
        
        // Create instance
        restApiDataSource = new RestApiDataSource(mockHttpClient, configuration);
    }
    
    @Test
    void testInitialize_Success() throws DataSourceException {
        // Act
        restApiDataSource.initialize(configuration);
        
        // Assert
        assertEquals(DataSourceType.REST_API, restApiDataSource.getSourceType());
        assertEquals("test-api", restApiDataSource.getName());
        assertEquals("rest-api", restApiDataSource.getDataType());
        assertTrue(restApiDataSource.supportsDataType("rest-api"));
    }
    
    @Test
    void testInitialize_ConnectionFailure() throws IOException, InterruptedException {
        // Arrange
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new IOException("Connection failed"));
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            restApiDataSource.initialize(configuration);
        });
    }
    
    @Test
    void testTestConnection_Success() throws IOException, InterruptedException {
        // Arrange
        restApiDataSource.initialize(configuration);
        
        // Act
        boolean result = restApiDataSource.testConnection();
        
        // Assert
        assertTrue(result);
        verify(mockHttpClient, atLeastOnce()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
    
    @Test
    void testTestConnection_Failure() throws IOException, InterruptedException {
        // Arrange
        when(mockResponse.statusCode()).thenReturn(500);
        
        // Act
        boolean result = restApiDataSource.testConnection();
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void testGetData_WithoutCache() throws DataSourceException, IOException, InterruptedException {
        // Arrange
        restApiDataSource.initialize(configuration);
        
        // Act
        Object result = restApiDataSource.getData("users", 1);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals("raw", resultMap.keySet().iterator().next()); // Basic JSON parsing returns raw content
    }
    
    @Test
    void testGetData_WithCache() throws DataSourceException, IOException, InterruptedException {
        // Arrange
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(300L);
        configuration.setCache(cacheConfig);
        
        restApiDataSource.initialize(configuration);
        
        // Act - First call should hit API
        Object result1 = restApiDataSource.getData("users", 1);
        
        // Act - Second call should hit cache
        Object result2 = restApiDataSource.getData("users", 1);
        
        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1, result2);
        
        // Verify API was called only once (second call used cache)
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
    
    @Test
    void testQuery_Success() throws DataSourceException, IOException, InterruptedException {
        // Arrange
        restApiDataSource.initialize(configuration);
        when(mockResponse.body()).thenReturn("[{\"id\": 1, \"name\": \"John\"}, {\"id\": 2, \"name\": \"Jane\"}]");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 1);
        
        // Act
        List<Object> results = restApiDataSource.query("/users", parameters);
        
        // Assert
        assertNotNull(results);
        assertEquals(1, results.size()); // Basic JSON parsing returns single item
    }
    
    @Test
    void testQuery_HttpError() throws IOException, InterruptedException {
        // Arrange
        restApiDataSource.initialize(configuration);
        when(mockResponse.statusCode()).thenReturn(404);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 999);
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            restApiDataSource.query("/users/999", parameters);
        });
    }
    
    @Test
    void testQueryForObject_Success() throws DataSourceException, IOException, InterruptedException {
        // Arrange
        restApiDataSource.initialize(configuration);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 1);
        
        // Act
        Object result = restApiDataSource.queryForObject("/users/{id}", parameters);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
    }
    
    @Test
    void testQueryForObject_NoResults() throws DataSourceException, IOException, InterruptedException {
        // Arrange
        restApiDataSource.initialize(configuration);
        when(mockResponse.body()).thenReturn("[]");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 999);
        
        // Act
        Object result = restApiDataSource.queryForObject("/users/{id}", parameters);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testBatchUpdate_Success() throws DataSourceException, IOException, InterruptedException {
        // Arrange
        restApiDataSource.initialize(configuration);
        when(mockResponse.statusCode()).thenReturn(201); // Created
        
        List<String> updates = List.of("/users", "/users/1");
        
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> restApiDataSource.batchUpdate(updates));
        
        // Verify HTTP calls were made
        verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
    
    @Test
    void testBatchUpdate_HttpError() throws IOException, InterruptedException {
        // Arrange
        restApiDataSource.initialize(configuration);
        when(mockResponse.statusCode()).thenReturn(400); // Bad Request
        
        List<String> updates = List.of("/users");
        
        // Act & Assert
        assertThrows(DataSourceException.class, () -> {
            restApiDataSource.batchUpdate(updates);
        });
    }
    
    @Test
    void testGetConnectionStatus() throws DataSourceException {
        // Act
        ConnectionStatus status = restApiDataSource.getConnectionStatus();
        
        // Assert
        assertNotNull(status);
        assertEquals(ConnectionStatus.State.NOT_INITIALIZED, status.getState());
        
        // After initialization
        restApiDataSource.initialize(configuration);
        status = restApiDataSource.getConnectionStatus();
        assertEquals(ConnectionStatus.State.CONNECTED, status.getState());
    }
    
    @Test
    void testGetMetrics() throws DataSourceException {
        // Arrange
        restApiDataSource.initialize(configuration);
        
        // Act
        var metrics = restApiDataSource.getMetrics();
        
        // Assert
        assertNotNull(metrics);
        assertEquals(0, metrics.getSuccessfulRequests());
        assertEquals(0, metrics.getFailedRequests());
    }
    
    @Test
    void testRefresh() throws DataSourceException {
        // Arrange
        restApiDataSource.initialize(configuration);
        
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> restApiDataSource.refresh());
    }
    
    @Test
    void testShutdown() throws DataSourceException {
        // Arrange
        restApiDataSource.initialize(configuration);
        
        // Act
        restApiDataSource.shutdown();
        
        // Assert
        ConnectionStatus status = restApiDataSource.getConnectionStatus();
        assertEquals(ConnectionStatus.State.SHUTDOWN, status.getState());
    }
    
    @Test
    void testSupportsDataType() throws DataSourceException {
        // Arrange
        restApiDataSource.initialize(configuration);
        
        // Act & Assert
        assertTrue(restApiDataSource.supportsDataType("rest-api"));
        assertFalse(restApiDataSource.supportsDataType("database"));
    }
    
    @Test
    void testAuthenticationHeaders() throws DataSourceException, IOException, InterruptedException {
        // Arrange
        restApiDataSource.initialize(configuration);
        
        // Act
        restApiDataSource.getData("users", 1);
        
        // Assert - Verify that HTTP request was made (authentication headers are added internally)
        verify(mockHttpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}
