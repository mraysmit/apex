package dev.mars.apex.rest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.apex.rest.ApexRestApiApplication;
import dev.mars.apex.rest.dto.RuleEvaluationRequest;
import dev.mars.apex.rest.dto.RuleEvaluationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for the APEX REST API.
 * Tests all endpoints, error scenarios, and data binding functionality.
 * Uses real objects and integration testing instead of Mockito.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = ApexRestApiApplication.class
)
@ActiveProfiles("test")
class ApexRestApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return UP status for health check")
        void shouldReturnHealthyStatus() {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getBaseUrl() + "/actuator/health", Map.class);
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("UP", response.getBody().get("status"));
        }
    }

    @Nested
    @DisplayName("Rule Check API Tests")
    class RuleCheckTests {

        @Test
        @DisplayName("Should evaluate simple age rule correctly - adult")
        void shouldEvaluateAgeRuleForAdult() {
            // Given
            RuleEvaluationRequest request = new RuleEvaluationRequest();
            request.setCondition("age >= 18");
            Map<String, Object> data = new HashMap<>();
            data.put("age", 25);
            request.setData(data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RuleEvaluationRequest> entity = new HttpEntity<>(request, headers);

            // When
            ResponseEntity<RuleEvaluationResponse> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/rules/check", entity, RuleEvaluationResponse.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertTrue(response.getBody().isMatched());
            assertEquals("check-rule", response.getBody().getRuleName());
            assertEquals("Rule matched", response.getBody().getMessage());
            assertNotNull(response.getBody().getTimestamp());
            assertNotNull(response.getBody().getEvaluationId());
        }

        @Test
        @DisplayName("Should evaluate simple age rule correctly - minor")
        void shouldEvaluateAgeRuleForMinor() {
            // Given
            RuleEvaluationRequest request = new RuleEvaluationRequest();
            request.setCondition("age >= 18");
            Map<String, Object> data = new HashMap<>();
            data.put("age", 16);
            request.setData(data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RuleEvaluationRequest> entity = new HttpEntity<>(request, headers);

            // When
            ResponseEntity<RuleEvaluationResponse> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/rules/check", entity, RuleEvaluationResponse.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertFalse(response.getBody().isMatched());
            assertEquals("check-rule", response.getBody().getRuleName());
            assertEquals("Rule did not match", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should handle complex expressions with multiple properties")
        void shouldHandleComplexExpressions() {
            // Given
            RuleEvaluationRequest request = new RuleEvaluationRequest();
            request.setCondition("age >= 18 && status == 'active' && score > 75");
            Map<String, Object> data = new HashMap<>();
            data.put("age", 25);
            data.put("status", "active");
            data.put("score", 85);
            request.setData(data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RuleEvaluationRequest> entity = new HttpEntity<>(request, headers);

            // When
            ResponseEntity<RuleEvaluationResponse> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/rules/check", entity, RuleEvaluationResponse.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertTrue(response.getBody().isMatched());
        }

        @Test
        @DisplayName("Should handle string operations")
        void shouldHandleStringOperations() {
            // Given
            RuleEvaluationRequest request = new RuleEvaluationRequest();
            request.setCondition("name.length() > 3 && email.contains('@')");
            Map<String, Object> data = new HashMap<>();
            data.put("name", "John");
            data.put("email", "john@example.com");
            request.setData(data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RuleEvaluationRequest> entity = new HttpEntity<>(request, headers);

            // When
            ResponseEntity<RuleEvaluationResponse> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/rules/check", entity, RuleEvaluationResponse.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertTrue(response.getBody().isMatched());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle invalid SpEL expressions gracefully")
        void shouldHandleInvalidSpelExpressions() {
            // Given
            RuleEvaluationRequest request = new RuleEvaluationRequest();
            request.setCondition("invalid.expression.syntax(");
            Map<String, Object> data = new HashMap<>();
            data.put("age", 25);
            request.setData(data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RuleEvaluationRequest> entity = new HttpEntity<>(request, headers);

            // When
            ResponseEntity<RuleEvaluationResponse> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/rules/check", entity, RuleEvaluationResponse.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertFalse(response.getBody().isMatched()); // Error recovery returns false
        }

        @Test
        @DisplayName("Should handle missing properties gracefully")
        void shouldHandleMissingProperties() {
            // Given
            RuleEvaluationRequest request = new RuleEvaluationRequest();
            request.setCondition("nonExistentProperty > 10");
            Map<String, Object> data = new HashMap<>();
            data.put("age", 25);
            request.setData(data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RuleEvaluationRequest> entity = new HttpEntity<>(request, headers);

            // When
            ResponseEntity<RuleEvaluationResponse> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/rules/check", entity, RuleEvaluationResponse.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertFalse(response.getBody().isMatched()); // Error recovery returns false
        }

        @Test
        @DisplayName("Should validate null data and return 400 Bad Request")
        void shouldValidateNullData() {
            // Given
            RuleEvaluationRequest request = new RuleEvaluationRequest();
            request.setCondition("age > 18");
            request.setData(null);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RuleEvaluationRequest> entity = new HttpEntity<>(request, headers);

            // When
            ResponseEntity<String> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/rules/check", entity, String.class);

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            // The response should contain validation error information
            String responseBody = response.getBody().toLowerCase();
            assertTrue(responseBody.contains("data cannot be null") ||
                      responseBody.contains("validation") ||
                      responseBody.contains("notnull") ||
                      responseBody.contains("bad request"),
                      "Response body should contain validation error: " + response.getBody());
        }
    }

    @Nested
    @DisplayName("Rules Management Tests")
    class RulesManagementTests {

        @Test
        @DisplayName("Should return defined rules")
        void shouldReturnDefinedRules() {
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                getBaseUrl() + "/api/rules/defined", Map.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().containsKey("count"));
            assertTrue(response.getBody().containsKey("definedRules"));
            assertTrue(response.getBody().containsKey("timestamp"));
        }
    }
}
