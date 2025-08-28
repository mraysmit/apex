package dev.mars.apex.rest.controller;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import dev.mars.apex.rest.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ExpressionController endpoints.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
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
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/expressions/evaluate"),
            HttpMethod.POST,
            createJsonEntity(request),
            new ParameterizedTypeReference<Map<String, Object>>() {}
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
        request.put("expression", "#amount * #taxRate");
        
        Map<String, Object> context = new HashMap<>();
        context.put("amount", 1000.0);
        context.put("taxRate", 0.08);
        request.put("context", context);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/expressions/evaluate"),
            HttpMethod.POST,
            createJsonEntity(request),
            new ParameterizedTypeReference<Map<String, Object>>() {}
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
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/expressions/validate"),
            HttpMethod.POST,
            createJsonEntity(request),
            new ParameterizedTypeReference<Map<String, Object>>() {}
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
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/expressions/validate"),
            HttpMethod.POST,
            createJsonEntity(request),
            new ParameterizedTypeReference<Map<String, Object>>() {}
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
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/expressions/functions"),
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
