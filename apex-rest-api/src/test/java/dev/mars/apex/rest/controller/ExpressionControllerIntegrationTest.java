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
 * Integration tests for ExpressionController endpoints.
 */
@DisplayName("Expression Controller Integration Tests")
public class ExpressionControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should evaluate mathematical expression")
    void shouldEvaluateMathematicalExpression() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("expression", "2 + 3 * 4");

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/api/expressions/evaluate"), 
            createJsonEntity(request), 
            Map.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(14.0, ((Number) response.getBody().get("result")).doubleValue());
    }

    @Test
    @DisplayName("Should evaluate expression with context variables")
    void shouldEvaluateExpressionWithContextVariables() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("expression", "amount * taxRate");
        
        Map<String, Object> context = new HashMap<>();
        context.put("amount", 1000.0);
        context.put("taxRate", 0.08);
        request.put("context", context);

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/api/expressions/evaluate"), 
            createJsonEntity(request), 
            Map.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(80.0, ((Number) response.getBody().get("result")).doubleValue());
    }

    @Test
    @DisplayName("Should validate expression syntax")
    void shouldValidateExpressionSyntax() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("expression", "amount > 1000 && status == 'ACTIVE'");

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/api/expressions/validate"), 
            createJsonEntity(request), 
            Map.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("valid"));
        assertTrue(response.getBody().containsKey("message"));
    }

    @Test
    @DisplayName("Should handle invalid expression syntax")
    void shouldHandleInvalidExpressionSyntax() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("expression", "invalid syntax +++");

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/api/expressions/validate"), 
            createJsonEntity(request), 
            Map.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("valid"));
        assertTrue(response.getBody().containsKey("message"));
    }

    @Test
    @DisplayName("Should get available expression functions")
    void shouldGetAvailableExpressionFunctions() {
        // Act
        ResponseEntity<Map> response = restTemplate.getForEntity(
            url("/api/expressions/functions"), 
            Map.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("functions"));
    }
}
