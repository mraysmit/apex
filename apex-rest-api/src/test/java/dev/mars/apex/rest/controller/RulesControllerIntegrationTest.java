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
 * Integration tests for RulesController endpoints.
 */
@DisplayName("Rules Controller Integration Tests")
public class RulesControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should evaluate simple rule successfully")
    void shouldEvaluateSimpleRule() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("expression", "amount > 1000");
        
        Map<String, Object> context = new HashMap<>();
        context.put("amount", 1500.0);
        request.put("context", context);

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/api/rules/evaluate"), 
            createJsonEntity(request), 
            Map.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("result"));
        assertTrue(response.getBody().containsKey("executionTime"));
    }

    @Test
    @DisplayName("Should handle rule evaluation with false result")
    void shouldHandleRuleEvaluationWithFalseResult() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("expression", "amount > 1000");
        
        Map<String, Object> context = new HashMap<>();
        context.put("amount", 500.0);
        request.put("context", context);

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/api/rules/evaluate"), 
            createJsonEntity(request), 
            Map.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("result"));
    }

    @Test
    @DisplayName("Should handle invalid expression gracefully")
    void shouldHandleInvalidExpressionGracefully() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("expression", "invalid expression syntax +++");
        
        Map<String, Object> context = new HashMap<>();
        context.put("amount", 1500.0);
        request.put("context", context);

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/api/rules/evaluate"), 
            createJsonEntity(request), 
            Map.class
        );

        // Assert
        // Should return error response but not crash
        assertTrue(response.getStatusCode() == HttpStatus.BAD_REQUEST || 
                  response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("Should validate rule expression")
    void shouldValidateRuleExpression() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("expression", "amount > 1000 && customerTier == 'GOLD'");

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/api/rules/validate"), 
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
    @DisplayName("Should get available rule functions")
    void shouldGetAvailableRuleFunctions() {
        // Act
        ResponseEntity<Map> response = restTemplate.getForEntity(
            url("/api/rules/functions"), 
            Map.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("functions"));
    }
}
