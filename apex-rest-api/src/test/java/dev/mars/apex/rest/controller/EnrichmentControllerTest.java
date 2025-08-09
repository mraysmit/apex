package dev.mars.apex.rest.controller;

import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnrichmentController.
 * Tests enrichment operations with mocked services for isolated testing.
 */
@ExtendWith(MockitoExtension.class)
class EnrichmentControllerTest {

    @Mock
    private EnrichmentService enrichmentService;

    @Mock
    private YamlConfigurationLoader yamlConfigurationLoader;

    @InjectMocks
    private EnrichmentController enrichmentController;

    private Map<String, Object> testObject;
    private String testYamlConfig;
    private YamlRuleConfiguration mockYamlConfig;

    @BeforeEach
    void setUp() {
        testObject = new HashMap<>();
        testObject.put("customerId", "CUST001");
        testObject.put("transactionAmount", 1000.0);
        testObject.put("currency", "USD");

        testYamlConfig = """
            metadata:
              name: "Customer Enrichment"
              version: "1.0.0"
            
            enrichments:
              - name: "customer-profile"
                condition: "#customerId != null"
                enrichmentType: "lookup"
                sourceField: "customerId"
                targetFields:
                  - "customerName"
                  - "customerTier"
                lookupService: "customerLookup"
            """;

        mockYamlConfig = mock(YamlRuleConfiguration.class);
    }

    @Test
    @DisplayName("Should enrich object using YAML configuration successfully")
    void testEnrichObjectSuccess() throws Exception {
        // Arrange
        EnrichmentController.EnrichmentRequest request = new EnrichmentController.EnrichmentRequest();
        request.setYamlConfiguration(testYamlConfig);
        request.setTargetObject(testObject);

        Map<String, Object> enrichedObject = new HashMap<>(testObject);
        enrichedObject.put("customerName", "John Doe");
        enrichedObject.put("customerTier", "GOLD");

        when(yamlConfigurationLoader.loadFromStream(any(ByteArrayInputStream.class)))
            .thenReturn(mockYamlConfig);
        when(enrichmentService.enrichObject(eq(mockYamlConfig), eq(testObject)))
            .thenReturn(enrichedObject);
        when(mockYamlConfig.getEnrichments()).thenReturn(Arrays.asList(mock(Object.class)));

        // Act
        ResponseEntity<Map<String, Object>> response = enrichmentController.enrichObject(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(testObject, responseBody.get("originalObject"));
        assertEquals(enrichedObject, responseBody.get("enrichedObject"));
        assertEquals(1, responseBody.get("enrichmentCount"));

        verify(yamlConfigurationLoader).loadFromStream(any(ByteArrayInputStream.class));
        verify(enrichmentService).enrichObject(mockYamlConfig, testObject);
    }

    @Test
    @DisplayName("Should handle enrichment error gracefully")
    void testEnrichObjectError() throws Exception {
        // Arrange
        EnrichmentController.EnrichmentRequest request = new EnrichmentController.EnrichmentRequest();
        request.setYamlConfiguration(testYamlConfig);
        request.setTargetObject(testObject);

        when(yamlConfigurationLoader.loadFromStream(any(ByteArrayInputStream.class)))
            .thenThrow(new RuntimeException("Invalid YAML configuration"));

        // Act
        ResponseEntity<Map<String, Object>> response = enrichmentController.enrichObject(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Object enrichment failed", responseBody.get("error"));
        assertTrue(responseBody.get("message").toString().contains("Invalid YAML configuration"));
    }

    @Test
    @DisplayName("Should enrich multiple objects in batch successfully")
    void testEnrichBatchSuccess() throws Exception {
        // Arrange
        List<Object> targetObjects = Arrays.asList(
            Map.of("customerId", "CUST001", "amount", 1000.0),
            Map.of("customerId", "CUST002", "amount", 2500.0),
            Map.of("customerId", "CUST003", "amount", 750.0)
        );

        EnrichmentController.BatchEnrichmentRequest request = new EnrichmentController.BatchEnrichmentRequest();
        request.setYamlConfiguration(testYamlConfig);
        request.setTargetObjects(targetObjects);

        when(yamlConfigurationLoader.loadFromStream(any(ByteArrayInputStream.class)))
            .thenReturn(mockYamlConfig);
        when(mockYamlConfig.getEnrichments()).thenReturn(Arrays.asList(mock(Object.class)));

        // Mock enrichment for each object
        when(enrichmentService.enrichObject(eq(mockYamlConfig), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> obj = (Map<String, Object>) invocation.getArgument(1);
                Map<String, Object> enriched = new HashMap<>(obj);
                enriched.put("customerName", "Customer " + obj.get("customerId"));
                enriched.put("customerTier", "STANDARD");
                return enriched;
            });

        // Act
        ResponseEntity<Map<String, Object>> response = enrichmentController.enrichBatch(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(targetObjects, responseBody.get("originalObjects"));
        assertEquals(3, responseBody.get("processedCount"));
        assertEquals(1, responseBody.get("enrichmentCount"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> enrichedObjects = (List<Map<String, Object>>) responseBody.get("enrichedObjects");
        assertEquals(3, enrichedObjects.size());
        
        // Verify each object was enriched
        for (Map<String, Object> enriched : enrichedObjects) {
            assertTrue(enriched.containsKey("customerName"));
            assertTrue(enriched.containsKey("customerTier"));
        }

        verify(enrichmentService, times(3)).enrichObject(eq(mockYamlConfig), any());
    }

    @Test
    @DisplayName("Should enrich using predefined configuration successfully")
    void testEnrichWithPredefinedConfigSuccess() throws Exception {
        // Arrange
        String configName = "customer-profile";
        
        when(yamlConfigurationLoader.loadFromStream(any(ByteArrayInputStream.class)))
            .thenReturn(mockYamlConfig);
        when(enrichmentService.enrichObject(eq(mockYamlConfig), eq(testObject)))
            .thenReturn(testObject);

        // Act
        ResponseEntity<Map<String, Object>> response = enrichmentController.enrichWithPredefinedConfig(configName, testObject);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(configName, responseBody.get("configName"));
        assertEquals(testObject, responseBody.get("originalObject"));
        assertEquals(testObject, responseBody.get("enrichedObject"));

        verify(enrichmentService).enrichObject(mockYamlConfig, testObject);
    }

    @Test
    @DisplayName("Should return 404 for non-existent predefined configuration")
    void testEnrichWithPredefinedConfigNotFound() {
        // Arrange
        String configName = "non-existent-config";

        // Act
        ResponseEntity<Map<String, Object>> response = enrichmentController.enrichWithPredefinedConfig(configName, testObject);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Predefined configuration not found", responseBody.get("error"));
        assertEquals(configName, responseBody.get("configName"));
        assertTrue(responseBody.containsKey("availableConfigs"));
    }

    @Test
    @DisplayName("Should get available predefined configurations successfully")
    void testGetPredefinedConfigurations() {
        // Act
        ResponseEntity<Map<String, Object>> response = enrichmentController.getPredefinedConfigurations();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        
        @SuppressWarnings("unchecked")
        Set<String> configurations = (Set<String>) responseBody.get("configurations");
        assertTrue(configurations.contains("customer-profile"));
        assertTrue(configurations.contains("trade-enrichment"));
        assertTrue((Integer) responseBody.get("count") > 0);
    }

    @Test
    @DisplayName("Should validate EnrichmentRequest DTO")
    void testEnrichmentRequestDto() {
        // Test default constructor
        EnrichmentController.EnrichmentRequest request1 = new EnrichmentController.EnrichmentRequest();
        assertNotNull(request1);

        // Test setters and getters
        request1.setYamlConfiguration(testYamlConfig);
        request1.setTargetObject(testObject);

        assertEquals(testYamlConfig, request1.getYamlConfiguration());
        assertEquals(testObject, request1.getTargetObject());
    }

    @Test
    @DisplayName("Should validate BatchEnrichmentRequest DTO")
    void testBatchEnrichmentRequestDto() {
        // Test default constructor
        EnrichmentController.BatchEnrichmentRequest request1 = new EnrichmentController.BatchEnrichmentRequest();
        assertNotNull(request1);

        // Test setters and getters
        List<Object> objects = Arrays.asList(testObject, Map.of("id", "test"));
        request1.setYamlConfiguration(testYamlConfig);
        request1.setTargetObjects(objects);

        assertEquals(testYamlConfig, request1.getYamlConfiguration());
        assertEquals(objects, request1.getTargetObjects());
    }

    @Test
    @DisplayName("Should handle batch enrichment with empty list")
    void testEnrichBatchEmptyList() throws Exception {
        // Arrange
        EnrichmentController.BatchEnrichmentRequest request = new EnrichmentController.BatchEnrichmentRequest();
        request.setYamlConfiguration(testYamlConfig);
        request.setTargetObjects(new ArrayList<>());

        when(yamlConfigurationLoader.loadFromStream(any(ByteArrayInputStream.class)))
            .thenReturn(mockYamlConfig);
        when(mockYamlConfig.getEnrichments()).thenReturn(Arrays.asList(mock(Object.class)));

        // Act
        ResponseEntity<Map<String, Object>> response = enrichmentController.enrichBatch(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(0, responseBody.get("processedCount"));
        
        @SuppressWarnings("unchecked")
        List<Object> enrichedObjects = (List<Object>) responseBody.get("enrichedObjects");
        assertTrue(enrichedObjects.isEmpty());

        verify(enrichmentService, never()).enrichObject(any(), any());
    }

    @Test
    @DisplayName("Should handle enrichment with metadata")
    void testEnrichObjectWithMetadata() throws Exception {
        // Arrange
        EnrichmentController.EnrichmentRequest request = new EnrichmentController.EnrichmentRequest();
        request.setYamlConfiguration(testYamlConfig);
        request.setTargetObject(testObject);

        Map<String, Object> enrichedObject = new HashMap<>(testObject);
        enrichedObject.put("customerName", "John Doe");

        when(yamlConfigurationLoader.loadFromStream(any(ByteArrayInputStream.class)))
            .thenReturn(mockYamlConfig);
        when(enrichmentService.enrichObject(eq(mockYamlConfig), eq(testObject)))
            .thenReturn(enrichedObject);
        when(mockYamlConfig.getEnrichments()).thenReturn(Arrays.asList(mock(Object.class)));
        
        // Mock metadata
        when(mockYamlConfig.getMetadata()).thenReturn(mock(Object.class));
        when(mockYamlConfig.getMetadata().getName()).thenReturn("Customer Enrichment");
        when(mockYamlConfig.getMetadata().getVersion()).thenReturn("1.0.0");

        // Act
        ResponseEntity<Map<String, Object>> response = enrichmentController.enrichObject(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals("Customer Enrichment", responseBody.get("configurationName"));
        assertEquals("1.0.0", responseBody.get("configurationVersion"));
    }
}
