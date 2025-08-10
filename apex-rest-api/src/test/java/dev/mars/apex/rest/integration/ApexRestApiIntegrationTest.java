package dev.mars.apex.rest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.apex.rest.ApexRestApiApplication;
import dev.mars.apex.rest.dto.RuleEvaluationRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.core.ParameterizedTypeReference;
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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                getBaseUrl() + "/actuator/health",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {});
            
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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                getBaseUrl() + "/api/rules/check",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(true, response.getBody().get("success"));
            assertEquals(true, response.getBody().get("matched"));
            assertEquals("check-rule", response.getBody().get("ruleName"));
            assertEquals("Rule matched", response.getBody().get("message"));
            assertNotNull(response.getBody().get("timestamp"));
            assertNotNull(response.getBody().get("evaluationId"));
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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                getBaseUrl() + "/api/rules/check",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(true, response.getBody().get("success"));
            assertEquals(false, response.getBody().get("matched"));
            assertEquals("check-rule", response.getBody().get("ruleName"));
            assertEquals("Rule did not match", response.getBody().get("message"));
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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                getBaseUrl() + "/api/rules/check",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(true, response.getBody().get("success"));
            assertEquals(true, response.getBody().get("matched"));
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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                getBaseUrl() + "/api/rules/check",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(true, response.getBody().get("success"));
            assertEquals(true, response.getBody().get("matched"));
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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                getBaseUrl() + "/api/rules/check",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(true, response.getBody().get("success"));
            assertEquals(false, response.getBody().get("matched")); // Error recovery returns false
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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                getBaseUrl() + "/api/rules/check",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(true, response.getBody().get("success"));
            assertEquals(false, response.getBody().get("matched")); // Error recovery returns false
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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                getBaseUrl() + "/api/rules/defined",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().containsKey("count"));
            assertTrue(response.getBody().containsKey("definedRules"));

            // Validate JSON structure using ObjectMapper
            try {
                String jsonResponse = objectMapper.writeValueAsString(response.getBody());
                assertNotNull(jsonResponse, "Response should be serializable to JSON");
                assertTrue(jsonResponse.contains("\"count\""), "JSON should contain count field");
                assertTrue(jsonResponse.contains("\"definedRules\""), "JSON should contain definedRules field");

                // Verify we can deserialize back to Map
                @SuppressWarnings("unchecked")
                Map<String, Object> deserializedResponse = objectMapper.readValue(jsonResponse, Map.class);
                assertEquals(response.getBody().get("count"), deserializedResponse.get("count"),
                           "Deserialized count should match original");
            } catch (Exception e) {
                fail("JSON serialization/deserialization should work: " + e.getMessage());
            }
            assertTrue(response.getBody().containsKey("timestamp"));
        }
    }
}
