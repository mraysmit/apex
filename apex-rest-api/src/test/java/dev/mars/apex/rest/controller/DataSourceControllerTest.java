package dev.mars.apex.rest.controller;

import dev.mars.apex.core.service.data.DataServiceManager;
import dev.mars.apex.core.service.data.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DataSourceController.
 * Tests data source management operations with mocked services for isolated testing.
 */
@ExtendWith(MockitoExtension.class)
class DataSourceControllerTest {

    @Mock
    private DataServiceManager dataServiceManager;

    @Mock
    private DataSource mockDataSource;

    @InjectMocks
    private DataSourceController dataSourceController;

    private String[] testDataSourceNames;
    private Map<String, Object> testParams;

    @BeforeEach
    void setUp() {
        testDataSourceNames = new String[]{"customerLookup", "productCatalog", "riskAssessment"};
        
        testParams = new HashMap<>();
        testParams.put("testKey", "CUST001");
        testParams.put("expectedFields", Arrays.asList("customerName", "customerTier", "riskRating"));
    }

    @Test
    @DisplayName("Should get all data sources successfully")
    void testGetAllDataSourcesSuccess() {
        // Arrange
        when(dataServiceManager.getRegisteredDataSources()).thenReturn(testDataSourceNames);
        when(dataServiceManager.getDataSource("customerLookup")).thenReturn(mockDataSource);
        when(dataServiceManager.getDataSource("productCatalog")).thenReturn(mockDataSource);
        when(dataServiceManager.getDataSource("riskAssessment")).thenReturn(mockDataSource);
        when(mockDataSource.getClass()).thenReturn(MockDataSource.class);

        // Act
        ResponseEntity<Map<String, Object>> response = dataSourceController.getAllDataSources();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(3, responseBody.get("count"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dataSources = (List<Map<String, Object>>) responseBody.get("dataSources");
        assertEquals(3, dataSources.size());
        
        // Verify each data source has required fields
        for (Map<String, Object> ds : dataSources) {
            assertNotNull(ds.get("name"));
            assertNotNull(ds.get("type"));
            assertNotNull(ds.get("description"));
            assertTrue((Boolean) ds.get("available"));
        }

        verify(dataServiceManager).getRegisteredDataSources();
        verify(dataServiceManager, times(3)).getDataSource(anyString());
    }

    @Test
    @DisplayName("Should handle error when getting all data sources")
    void testGetAllDataSourcesError() {
        // Arrange
        when(dataServiceManager.getRegisteredDataSources())
            .thenThrow(new RuntimeException("Service unavailable"));

        // Act
        ResponseEntity<Map<String, Object>> response = dataSourceController.getAllDataSources();

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Failed to retrieve data sources", responseBody.get("error"));
        assertTrue(responseBody.get("message").toString().contains("Service unavailable"));
    }

    @Test
    @DisplayName("Should get specific data source successfully")
    void testGetDataSourceSuccess() {
        // Arrange
        String dataSourceName = "customerLookup";
        when(dataServiceManager.getDataSource(dataSourceName)).thenReturn(mockDataSource);
        when(mockDataSource.getClass()).thenReturn(MockDataSource.class);

        // Act
        ResponseEntity<Map<String, Object>> response = dataSourceController.getDataSource(dataSourceName);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(dataSourceName, responseBody.get("name"));
        assertEquals("MockDataSource", responseBody.get("type"));
        assertTrue((Boolean) responseBody.get("available"));
        assertNotNull(responseBody.get("description"));

        verify(dataServiceManager).getDataSource(dataSourceName);
    }

    @Test
    @DisplayName("Should return 404 when data source not found")
    void testGetDataSourceNotFound() {
        // Arrange
        String dataSourceName = "non-existent-source";
        when(dataServiceManager.getDataSource(dataSourceName)).thenReturn(null);

        // Act
        ResponseEntity<Map<String, Object>> response = dataSourceController.getDataSource(dataSourceName);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Data source not found", responseBody.get("error"));
        assertEquals(dataSourceName, responseBody.get("name"));
    }

    @Test
    @DisplayName("Should test data source successfully")
    void testDataSourceSuccess() {
        // Arrange
        String dataSourceName = "customerLookup";
        String testKey = "CUST001";
        Map<String, Object> testResult = Map.of(
            "customerName", "John Doe",
            "customerTier", "GOLD",
            "riskRating", "LOW"
        );

        when(dataServiceManager.getDataSource(dataSourceName)).thenReturn(mockDataSource);
        when(mockDataSource.lookup(testKey)).thenReturn(testResult);

        // Act
        ResponseEntity<Map<String, Object>> response = dataSourceController.testDataSource(dataSourceName, testParams);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(dataSourceName, responseBody.get("name"));
        assertEquals(testKey, responseBody.get("testKey"));
        assertEquals(testResult, responseBody.get("testResult"));
        assertTrue((Boolean) responseBody.get("available"));
        assertNotNull(responseBody.get("responseTimeMs"));

        verify(dataServiceManager).getDataSource(dataSourceName);
        verify(mockDataSource).lookup(testKey);
    }

    @Test
    @DisplayName("Should handle test failure gracefully")
    void testDataSourceTestFailure() {
        // Arrange
        String dataSourceName = "customerLookup";
        String testKey = "CUST001";

        when(dataServiceManager.getDataSource(dataSourceName)).thenReturn(mockDataSource);
        when(mockDataSource.lookup(testKey)).thenThrow(new RuntimeException("Connection timeout"));

        // Act
        ResponseEntity<Map<String, Object>> response = dataSourceController.testDataSource(dataSourceName, testParams);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Still 200 but with failure details
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals(dataSourceName, responseBody.get("name"));
        assertEquals(testKey, responseBody.get("testKey"));
        assertEquals("Test lookup failed", responseBody.get("error"));
        assertFalse((Boolean) responseBody.get("available"));
        assertTrue(responseBody.get("message").toString().contains("Connection timeout"));
    }

    @Test
    @DisplayName("Should perform lookup successfully")
    void testPerformLookupSuccess() {
        // Arrange
        String dataSourceName = "customerLookup";
        String lookupKey = "CUST001";
        DataSourceController.LookupRequest request = new DataSourceController.LookupRequest();
        request.setKey(lookupKey);

        Map<String, Object> lookupResult = Map.of(
            "customerId", "CUST001",
            "customerName", "John Doe",
            "customerTier", "GOLD"
        );

        when(dataServiceManager.getDataSource(dataSourceName)).thenReturn(mockDataSource);
        when(mockDataSource.lookup(lookupKey)).thenReturn(lookupResult);

        // Act
        ResponseEntity<Map<String, Object>> response = dataSourceController.performLookup(dataSourceName, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(dataSourceName, responseBody.get("dataSource"));
        assertEquals(lookupKey, responseBody.get("key"));
        assertEquals(lookupResult, responseBody.get("result"));
        assertNotNull(responseBody.get("responseTimeMs"));

        verify(dataServiceManager).getDataSource(dataSourceName);
        verify(mockDataSource).lookup(lookupKey);
    }

    @Test
    @DisplayName("Should return 404 for lookup on non-existent data source")
    void testPerformLookupDataSourceNotFound() {
        // Arrange
        String dataSourceName = "non-existent-source";
        DataSourceController.LookupRequest request = new DataSourceController.LookupRequest();
        request.setKey("TEST_KEY");

        when(dataServiceManager.getDataSource(dataSourceName)).thenReturn(null);

        // Act
        ResponseEntity<Map<String, Object>> response = dataSourceController.performLookup(dataSourceName, request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Data source not found", responseBody.get("error"));
        assertEquals(dataSourceName, responseBody.get("name"));
    }

    @Test
    @DisplayName("Should handle lookup error")
    void testPerformLookupError() {
        // Arrange
        String dataSourceName = "customerLookup";
        String lookupKey = "INVALID_KEY";
        DataSourceController.LookupRequest request = new DataSourceController.LookupRequest();
        request.setKey(lookupKey);

        when(dataServiceManager.getDataSource(dataSourceName)).thenReturn(mockDataSource);
        when(mockDataSource.lookup(lookupKey)).thenThrow(new RuntimeException("Lookup failed"));

        // Act
        ResponseEntity<Map<String, Object>> response = dataSourceController.performLookup(dataSourceName, request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Lookup failed", responseBody.get("error"));
        assertEquals(dataSourceName, responseBody.get("dataSource"));
        assertEquals(lookupKey, responseBody.get("key"));
    }

    @Test
    @DisplayName("Should validate LookupRequest DTO")
    void testLookupRequestDto() {
        // Test default constructor
        DataSourceController.LookupRequest request1 = new DataSourceController.LookupRequest();
        assertNotNull(request1);

        // Test setters and getters
        request1.setKey("TEST_KEY");
        assertEquals("TEST_KEY", request1.getKey());
    }

    @Test
    @DisplayName("Should test data source with default parameters")
    void testDataSourceWithDefaultParams() {
        // Arrange
        String dataSourceName = "customerLookup";
        when(dataServiceManager.getDataSource(dataSourceName)).thenReturn(mockDataSource);
        when(mockDataSource.lookup("TEST_KEY")).thenReturn("test result");

        // Act - passing null for test params
        ResponseEntity<Map<String, Object>> response = dataSourceController.testDataSource(dataSourceName, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals("TEST_KEY", responseBody.get("testKey")); // Default test key

        verify(mockDataSource).lookup("TEST_KEY");
    }

    @Test
    @DisplayName("Should handle data source with null result")
    void testDataSourceWithNullResult() {
        // Arrange
        String dataSourceName = "customerLookup";
        when(dataServiceManager.getDataSource(dataSourceName)).thenReturn(mockDataSource);
        when(mockDataSource.lookup(anyString())).thenReturn(null);

        // Act
        ResponseEntity<Map<String, Object>> response = dataSourceController.testDataSource(dataSourceName, testParams);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertNull(responseBody.get("testResult"));
        assertTrue((Boolean) responseBody.get("available"));
    }

    // Mock class for testing
    private static class MockDataSource implements DataSource {
        @Override
        public String getName() { return "MockDataSource"; }
        
        @Override
        public String getDataType() { return "mock"; }
        
        @Override
        public boolean supportsDataType(String dataType) { return "mock".equals(dataType); }
        
        @Override
        public <T> T getData(String dataType, Object... parameters) { return null; }
        
        @Override
        public Object lookup(String key) { return null; }
    }
}
