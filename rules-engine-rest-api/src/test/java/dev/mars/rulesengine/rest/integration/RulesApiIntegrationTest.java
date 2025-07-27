package dev.mars.rulesengine.rest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.rulesengine.rest.dto.RuleEvaluationRequest;
import dev.mars.rulesengine.rest.dto.ValidationRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Rules Engine REST API using real Spring Boot context.
 * These tests use plain JUnit 5 with real objects and integration testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {dev.mars.rulesengine.rest.RulesEngineRestApiApplication.class,
               dev.mars.rulesengine.rest.config.IntegrationTestConfiguration.class})
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
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String getBaseUrl() {
        return "http://localhost:" + port;
    }
    
    @Test
    public void testHealthEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            getBaseUrl() + "/actuator/health", Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
    }
    
    @Test
    public void testMonitoringHealthEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            getBaseUrl() + "/api/monitoring/health", Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertTrue(response.getBody().containsKey("memory"));
        assertTrue(response.getBody().containsKey("system"));
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
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/api/rules/check", entity, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals(true, response.getBody().get("matched"));
        assertEquals("age-check", response.getBody().get("ruleName"));
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
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/api/rules/check", entity, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals(false, response.getBody().get("matched"));
        assertEquals("age-check", response.getBody().get("ruleName"));
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
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/api/rules/validate", entity, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("valid"));
        assertEquals(2, response.getBody().get("totalRules"));
        assertEquals(2, response.getBody().get("passedRules"));
        assertEquals(0, response.getBody().get("failedRules"));
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
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/api/rules/validate", entity, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().get("valid"));
        assertEquals(2, response.getBody().get("totalRules"));
        assertEquals(0, response.getBody().get("passedRules"));
        assertEquals(2, response.getBody().get("failedRules"));
        assertTrue(response.getBody().containsKey("errors"));
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
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/api/rules/define/positive-check", entity, Map.class);
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Rule defined successfully", response.getBody().get("message"));
        assertEquals("positive-check", response.getBody().get("ruleName"));
        assertEquals("#value > 0", response.getBody().get("condition"));
    }
    
    @Test
    public void testGetDefinedRulesEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            getBaseUrl() + "/api/rules/defined", Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("definedRules"));
        assertTrue(response.getBody().containsKey("count"));
        assertTrue(response.getBody().containsKey("timestamp"));
    }
    
    @Test
    public void testConfigurationInfoEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            getBaseUrl() + "/api/config/info", Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("hasConfiguration"));
        assertTrue(response.getBody().containsKey("timestamp"));
    }
    
    @Test
    public void testSystemStatsEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            getBaseUrl() + "/api/monitoring/stats", Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("totalMemory"));
        assertTrue(response.getBody().containsKey("definedRulesCount"));
        assertTrue(response.getBody().containsKey("timestamp"));
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

        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/api/rules/check", entity, Map.class);

        // The API handles errors gracefully and returns 200 with success=false
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().get("matched"));
        assertEquals("invalid-rule", response.getBody().get("ruleName"));
    }
}
