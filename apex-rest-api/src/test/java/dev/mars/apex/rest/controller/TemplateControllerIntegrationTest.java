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
 * Integration tests for TemplateController endpoints.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
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
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/templates/json"),
            HttpMethod.POST,
            createJsonEntity(request),
            new ParameterizedTypeReference<Map<String, Object>>() {}
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
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/templates/xml"),
            HttpMethod.POST,
            createJsonEntity(request),
            new ParameterizedTypeReference<Map<String, Object>>() {}
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
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/templates/text"),
            HttpMethod.POST,
            createJsonEntity(request),
            new ParameterizedTypeReference<Map<String, Object>>() {}
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
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/templates/text"),
            HttpMethod.POST,
            createJsonEntity(request),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        // Should handle gracefully - either success with default value or error response
        assertTrue(response.getStatusCode() == HttpStatus.OK || 
                  response.getStatusCode() == HttpStatus.BAD_REQUEST);
    }
}
