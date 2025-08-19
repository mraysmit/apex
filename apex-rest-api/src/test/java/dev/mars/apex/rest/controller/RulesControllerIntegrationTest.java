package dev.mars.apex.rest.controller;

import dev.mars.apex.rest.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        request.put("condition", "amount > 1000");

        Map<String, Object> data = new HashMap<>();
        data.put("amount", 1500.0);
        request.put("data", data);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/rules/check"),
            HttpMethod.POST,
            createJsonEntity(request),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("matched"));
        assertTrue(response.getBody().containsKey("success"));
    }

    @Test
    @DisplayName("Should handle rule evaluation with false result")
    void shouldHandleRuleEvaluationWithFalseResult() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("condition", "amount > 1000");

        Map<String, Object> data = new HashMap<>();
        data.put("amount", 500.0);
        request.put("data", data);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/rules/check"),
            HttpMethod.POST,
            createJsonEntity(request),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("matched"));
    }

    @Test
    @DisplayName("Should handle invalid expression gracefully")
    void shouldHandleInvalidExpressionGracefully() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("condition", "invalid expression syntax +++");

        Map<String, Object> data = new HashMap<>();
        data.put("amount", 1500.0);
        request.put("data", data);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/rules/check"),
            HttpMethod.POST,
            createJsonEntity(request),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        // Should handle invalid expression gracefully with error recovery
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Error recovery should return matched=false for invalid expressions
        assertFalse((Boolean) response.getBody().get("matched"));
    }

    @Test
    @DisplayName("Should validate rule expression")
    void shouldValidateRuleExpression() {
        // Arrange
        Map<String, Object> request = new HashMap<>();

        Map<String, Object> data = new HashMap<>();
        data.put("amount", 1500.0);
        data.put("customerTier", "GOLD");
        request.put("data", data);

        // Create validation rules array
        List<Map<String, Object>> validationRules = new ArrayList<>();
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "customer-validation");
        rule.put("condition", "amount > 1000 && customerTier == 'GOLD'");
        rule.put("message", "Customer validation passed");
        rule.put("severity", "ERROR");
        validationRules.add(rule);
        request.put("validationRules", validationRules);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/rules/validate"),
            HttpMethod.POST,
            createJsonEntity(request),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("valid"));
        assertTrue(response.getBody().containsKey("totalRules"));
        // Should be valid since the condition matches the data
        assertTrue((Boolean) response.getBody().get("valid"));
    }

    @Test
    @DisplayName("Should get available rule functions")
    void shouldGetAvailableRuleFunctions() {
        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/rules/functions"),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("functions"));
    }
}
