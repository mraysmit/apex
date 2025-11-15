package dev.mars.apex.rest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.apex.rest.ApexRestApiApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for REST API error handling (Day 10).
 * 
 * Tests verify that business logic failures (ResultType.ERROR) are properly
 * propagated to REST API clients with HTTP 500 status codes.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-15
 * @version 1.0
 */
@SpringBootTest(
    classes = ApexRestApiApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@DisplayName("REST API Error Handling Integration Tests (Day 10)")
class ErrorHandlingRestApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    @DisplayName("Test 1: HTTP 500 returned on enrichment required field failure")
    void testHttp500OnEnrichmentRequiredFieldFailure() throws Exception {
        // Given: YAML configuration with required field that will fail
        String yamlConfig = """
            metadata:
              name: "Required Field Failure Test"
              version: "1.0.0"
            
            enrichments:
              - id: "test-enrichment"
                type: "lookup-enrichment"
                condition: "#customerId != null"
                lookup-config:
                  lookup-key: "#customerId"
                  lookup-dataset:
                    type: "inline"
                    key-field: "customerId"
                    data:
                      - customerId: "CUST123"
                        customerName: "Test Customer"
                field-mappings:
                  - source-field: "missingField"
                    target-field: "targetField"
                    required: true
            """;

        Map<String, Object> request = new HashMap<>();
        request.put("yamlConfiguration", yamlConfig);
        
        Map<String, Object> targetObject = new HashMap<>();
        targetObject.put("customerId", "CUST123");
        request.put("targetObject", targetObject);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // When: Call enrichment endpoint
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl() + "/api/enrichment/enrich",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then: Should return HTTP 500 (not HTTP 200)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(),
            "Business logic failure should return HTTP 500");
        
        // And: Response should contain error details
        assertNotNull(response.getBody());
        Map<String, Object> body = response.getBody();
        assertEquals(false, body.get("success"), "Success flag should be false");
        assertNotNull(body.get("error"), "Error message should be present");
    }

    @Test
    @DisplayName("Test 2: HTTP 500 returned on transformation error")
    void testHttp500OnTransformationError() throws Exception {
        // Given: YAML configuration with invalid transformation expression
        String yamlConfig = """
            metadata:
              name: "Transformation Error Test"
              version: "1.0.0"
            
            transformations:
              - id: "invalid-transformation"
                expression: "#invalidObject.nonExistentMethod()"
                target-field: "result"
            """;

        Map<String, Object> request = new HashMap<>();
        request.put("yamlConfiguration", yamlConfig);
        
        Map<String, Object> targetObject = new HashMap<>();
        targetObject.put("testField", "testValue");
        request.put("targetObject", targetObject);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // When: Call enrichment endpoint
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl() + "/api/enrichment/enrich",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then: Should return HTTP 500 (not HTTP 200)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(),
            "Transformation error should return HTTP 500");
        
        // And: Response should contain error details
        assertNotNull(response.getBody());
        Map<String, Object> body = response.getBody();
        assertEquals(false, body.get("success"), "Success flag should be false");
        assertNotNull(body.get("error"), "Error message should be present");
    }

    @Test
    @DisplayName("Test 3: HTTP 200 returned on successful enrichment")
    void testHttp200OnSuccessfulEnrichment() throws Exception {
        // Given: Valid YAML configuration with successful enrichment
        String yamlConfig = """
            metadata:
              name: "Successful Enrichment Test"
              version: "1.0.0"

            enrichments:
              - id: "test-enrichment"
                type: "lookup-enrichment"
                condition: "#customerId != null"
                lookup-config:
                  lookup-key: "#customerId"
                  lookup-dataset:
                    type: "inline"
                    key-field: "customerId"
                    data:
                      - customerId: "CUST123"
                        customerName: "Test Customer"
                        customerTier: "GOLD"
                field-mappings:
                  - source-field: "customerName"
                    target-field: "customerName"
                  - source-field: "customerTier"
                    target-field: "customerTier"
            """;

        Map<String, Object> request = new HashMap<>();
        request.put("yamlConfiguration", yamlConfig);

        Map<String, Object> targetObject = new HashMap<>();
        targetObject.put("customerId", "CUST123");
        request.put("targetObject", targetObject);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // When: Call enrichment endpoint
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl() + "/api/enrichment/enrich",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then: Should return HTTP 200
        assertEquals(HttpStatus.OK, response.getStatusCode(),
            "Successful enrichment should return HTTP 200");

        // And: Response should indicate success
        assertNotNull(response.getBody());
        Map<String, Object> body = response.getBody();
        assertEquals(true, body.get("success"), "Success flag should be true");
        assertNotNull(body.get("enrichedObject"), "Enriched object should be present");
    }

    @Test
    @DisplayName("Test 4: Error details included in HTTP 500 response body")
    void testErrorDetailsInResponseBody() throws Exception {
        // Given: YAML configuration that will cause enrichment failure
        String yamlConfig = """
            metadata:
              name: "Error Details Test"
              version: "1.0.0"

            enrichments:
              - id: "failing-enrichment"
                type: "lookup-enrichment"
                condition: "#customerId != null"
                lookup-config:
                  lookup-key: "#customerId"
                  lookup-dataset:
                    type: "inline"
                    key-field: "customerId"
                    data:
                      - customerId: "CUST123"
                        existingField: "value"
                field-mappings:
                  - source-field: "missingRequiredField"
                    target-field: "targetField"
                    required: true
            """;

        Map<String, Object> request = new HashMap<>();
        request.put("yamlConfiguration", yamlConfig);

        Map<String, Object> targetObject = new HashMap<>();
        targetObject.put("customerId", "CUST123");
        request.put("targetObject", targetObject);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // When: Call enrichment endpoint
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl() + "/api/enrichment/enrich",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then: Should return HTTP 500 with error details
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        assertNotNull(response.getBody());
        Map<String, Object> body = response.getBody();

        // Verify error structure
        assertEquals(false, body.get("success"), "Success flag should be false");
        assertNotNull(body.get("error"), "Error field should be present");
        assertNotNull(body.get("message"), "Message field should be present");
        assertNotNull(body.get("timestamp"), "Timestamp should be present");

        // Error message should contain meaningful information
        String errorMessage = body.get("message").toString();
        assertFalse(errorMessage.isEmpty(), "Error message should not be empty");
    }
}

