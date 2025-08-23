package dev.mars.apex.playground.controller;

import dev.mars.apex.playground.model.PlaygroundRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple integration tests for API endpoints.
 * 
 * Tests the basic functionality with real Spring Boot context.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.main.web-application-type=servlet"
})
@DisplayName("Simple Integration Tests")
class SimpleIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Should return health status successfully")
    void shouldReturnHealthStatusSuccessfully() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity("/playground/api/health", Map.class);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("apex-playground", response.getBody().get("service"));
        assertEquals("1.0.0", response.getBody().get("version"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    @DisplayName("Should validate YAML successfully")
    void shouldValidateYamlSuccessfully() {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("yamlContent", "metadata:\n  name: \"Test Rules\"\n  version: \"1.0.0\"\nrules:\n  - id: \"age-check\"\n    name: \"Age Validation\"\n    condition: \"#age >= 18\"");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/playground/api/validate", entity, Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("valid"));
    }

    @Test
    @DisplayName("Should process data successfully")
    void shouldProcessDataSuccessfully() {
        // Given
        PlaygroundRequest request = new PlaygroundRequest();
        request.setSourceData("{\"name\": \"John Doe\", \"age\": 25}");
        request.setYamlRules("metadata:\n  name: \"Test Rules\"\n  version: \"1.0.0\"\nrules:\n  - id: \"age-check\"\n    name: \"Age Validation\"\n    condition: \"#age >= 18\"");
        request.setDataFormat("JSON");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PlaygroundRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/playground/api/process", entity, Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
    }
}
