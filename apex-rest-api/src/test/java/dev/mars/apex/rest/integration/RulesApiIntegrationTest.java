package dev.mars.apex.rest.integration;

import dev.mars.apex.rest.dto.RuleEvaluationRequest;
import dev.mars.apex.rest.dto.ValidationRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Rules Engine REST API using real Spring Boot context.
 * These tests use plain JUnit 5 with real objects and advanced testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {dev.mars.apex.rest.ApexRestApiApplication.class,
               dev.mars.apex.rest.config.IntegrationTestConfiguration.class})
@ActiveProfiles("test")
public class RulesApiIntegrationTest {

    @BeforeAll
    static void setupTestEnvironment() {
        // Set system property for test-aware logging in core module
        System.setProperty("test.environment", "true");
    }

    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    private Map<String, Object> getResponseBody(ResponseEntity<Map<String, Object>> response) {
        assertNotNull(response.getBody());
        return response.getBody();
    }
    
    @Test
    public void testHealthEndpoint() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl() + "/actuator/health",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = getResponseBody(response);
        assertEquals("UP", body.get("status"));
    }
    
    @Test
    public void testMonitoringHealthEndpoint() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl() + "/api/monitoring/health",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = getResponseBody(response);
        assertEquals("UP", body.get("status"));
        assertTrue(body.containsKey("memory"));
        assertTrue(body.containsKey("system"));
    }
    
    @Test
    public void testRuleCheckEndpoint_Success() {
        RuleEvaluationRequest request = new RuleEvaluationRequest(
            "#age >= 18",
            Map.of("age", 25),
            "age-check",
            "User is an adult",
            false
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RuleEvaluationRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl() + "/api/rules/check",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Map<String, Object>>() {});
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody); // Explicit null check for IDE
        assertEquals(true, responseBody.get("success"));
        assertEquals(true, responseBody.get("matched"));
        assertEquals("age-check", responseBody.get("ruleName"));
    }
    
    @Test
    public void testRuleCheckEndpoint_Failure() {
        RuleEvaluationRequest request = new RuleEvaluationRequest(
            "#age >= 18",
            Map.of("age", 16),
            "age-check",
            "User is an adult",
            false
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RuleEvaluationRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl() + "/api/rules/check",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Map<String, Object>>() {});
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(true, responseBody.get("success"));
        assertEquals(false, responseBody.get("matched"));
        assertEquals("age-check", responseBody.get("ruleName"));
    }
    
    @Test
    public void testValidationEndpoint_Success() {
        ValidationRequest request = new ValidationRequest();
        request.setData(Map.of("age", 25, "email", "john@example.com"));
        
        ValidationRequest.ValidationRuleDto rule1 = new ValidationRequest.ValidationRuleDto(
            "age-check", "#age >= 18", "Must be at least 18", "ERROR"
        );
        ValidationRequest.ValidationRuleDto rule2 = new ValidationRequest.ValidationRuleDto(
            "email-check", "#email != null", "Email is required", "ERROR"
        );
        
        request.setValidationRules(List.of(rule1, rule2));
        request.setIncludeDetails(true);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ValidationRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl() + "/api/rules/validate",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Map<String, Object>>() {});
        
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(true, responseBody.get("valid"));
        assertEquals(2, responseBody.get("totalRules"));
        assertEquals(2, responseBody.get("passedRules"));
        assertEquals(0, responseBody.get("failedRules"));
    }
    
    @Test
    public void testValidationEndpoint_WithErrors() {
        // NOTE: This test intentionally sends data that fails validation to verify error handling
        // Expected warnings: "Missing parameters for rule 'Check': [email]" and age validation failure
        ValidationRequest request = new ValidationRequest();
        Map<String, Object> testData = new HashMap<>();
        testData.put("age", 16);
        testData.put("email", null);
        request.setData(testData);
        
        ValidationRequest.ValidationRuleDto rule1 = new ValidationRequest.ValidationRuleDto(
            "age-check", "#age >= 18", "Must be at least 18", "ERROR"
        );
        ValidationRequest.ValidationRuleDto rule2 = new ValidationRequest.ValidationRuleDto(
            "email-check", "#email != null", "Email is required", "ERROR"
        );
        
        request.setValidationRules(List.of(rule1, rule2));
        request.setIncludeDetails(true);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ValidationRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl() + "/api/rules/validate",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Map<String, Object>>() {});
        
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(false, responseBody.get("valid"));
        assertEquals(2, responseBody.get("totalRules"));
        assertEquals(0, responseBody.get("passedRules"));
        assertEquals(2, responseBody.get("failedRules"));
        assertTrue(responseBody.containsKey("errors"));
    }
    
    @Test
    public void testDefineRuleEndpoint() {
        Map<String, String> ruleDefinition = Map.of(
            "condition", "#value > 0",
            "message", "Value must be positive"
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(ruleDefinition, headers);
        
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl() + "/api/rules/define/positive-check",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Map<String, Object>>() {});
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals("Rule defined successfully", responseBody.get("message"));
        assertEquals("positive-check", responseBody.get("ruleName"));
        assertEquals("#value > 0", responseBody.get("condition"));
    }
    
    @Test
    public void testGetDefinedRulesEndpoint() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl() + "/api/rules/defined",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {});
        
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue(responseBody.containsKey("definedRules"));
        assertTrue(responseBody.containsKey("count"));
        assertTrue(responseBody.containsKey("timestamp"));
    }
    
    @Test
    public void testConfigurationInfoEndpoint() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl() + "/api/config/info",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {});
        
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue(responseBody.containsKey("hasConfiguration"));
        assertTrue(responseBody.containsKey("timestamp"));
    }
    
    @Test
    public void testSystemStatsEndpoint() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl() + "/api/monitoring/stats",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {});
        
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue(responseBody.containsKey("totalMemory"));
        assertTrue(responseBody.containsKey("definedRulesCount"));
        assertTrue(responseBody.containsKey("timestamp"));
    }
    
    @Test
    public void testInvalidRuleCondition() {
        // NOTE: This test intentionally sends an invalid rule condition to verify error handling
        // Expected warning: "Missing parameters for rule 'Check': [invalid]"
        RuleEvaluationRequest request = new RuleEvaluationRequest(
            "#invalid.syntax.here",
            Map.of("age", 25),
            "invalid-rule",
            "Test message",
            false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RuleEvaluationRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl() + "/api/rules/check",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Map<String, Object>>() {});

        // The API handles errors gracefully and returns 200 with success=false
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(false, responseBody.get("matched"));
        assertEquals("invalid-rule", responseBody.get("ruleName"));
    }
}
