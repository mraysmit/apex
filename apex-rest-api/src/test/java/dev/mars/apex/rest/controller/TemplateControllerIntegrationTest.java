package dev.mars.apex.rest.controller;

import dev.mars.apex.rest.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TemplateController endpoints.
 */
@DisplayName("Template Controller Integration Tests")
public class TemplateControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should process JSON template")
    void shouldProcessJsonTemplate() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("template", "{\"customerName\": \"#{customerName}\", \"amount\": #{amount}}");
        
        Map<String, Object> context = new HashMap<>();
        context.put("customerName", "John Doe");
        context.put("amount", 1500.0);
        request.put("context", context);

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/api/templates/json"), 
            createJsonEntity(request), 
            Map.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("processedTemplate"));
        assertTrue(response.getBody().containsKey("success"));
        assertTrue((Boolean) response.getBody().get("success"));
    }

    @Test
    @DisplayName("Should process XML template")
    void shouldProcessXmlTemplate() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("template", "<customer><name>#{customerName}</name><amount>#{amount}</amount></customer>");
        
        Map<String, Object> context = new HashMap<>();
        context.put("customerName", "Jane Smith");
        context.put("amount", 2500.0);
        request.put("context", context);

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/api/templates/xml"), 
            createJsonEntity(request), 
            Map.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("processedTemplate"));
        assertTrue(response.getBody().containsKey("success"));
        assertTrue((Boolean) response.getBody().get("success"));
    }

    @Test
    @DisplayName("Should process text template")
    void shouldProcessTextTemplate() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("template", "Dear #{customerName}, your transaction amount is #{amount}.");
        
        Map<String, Object> context = new HashMap<>();
        context.put("customerName", "Bob Johnson");
        context.put("amount", 750.0);
        request.put("context", context);

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/api/templates/text"), 
            createJsonEntity(request), 
            Map.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("processedTemplate"));
        assertTrue(response.getBody().containsKey("success"));
        assertTrue((Boolean) response.getBody().get("success"));
    }

    @Test
    @DisplayName("Should handle template processing error gracefully")
    void shouldHandleTemplateProcessingErrorGracefully() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("template", "Invalid template with #{nonExistentVariable}");
        
        Map<String, Object> context = new HashMap<>();
        context.put("customerName", "Test User");
        request.put("context", context);

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/api/templates/text"), 
            createJsonEntity(request), 
            Map.class
        );

        // Assert
        // Should handle gracefully - either success with default value or error response
        assertTrue(response.getStatusCode() == HttpStatus.OK || 
                  response.getStatusCode() == HttpStatus.BAD_REQUEST);
    }
}
