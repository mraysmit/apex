package dev.mars.apex.playground.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.apex.playground.model.PlaygroundRequest;
import dev.mars.apex.playground.model.PlaygroundResponse;

import dev.mars.apex.playground.model.YamlValidationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests that verify actual functionality.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Comprehensive Integration Tests")
class ComprehensiveIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/playground/api";
    }

    @Test
    @DisplayName("Should validate complete YAML configuration correctly")
    void shouldValidateCompleteYamlConfiguration() {
        // Given - A complete, valid YAML configuration
        String validYaml = """
            metadata:
              name: "Integration Test Rules"
              version: "1.0.0"
              description: "Test configuration for integration testing"
              type: "rule-configuration"
            
            rules:
              - id: "age-check"
                name: "Age Verification"
                condition: "#age >= 18"
                message: "Must be 18 or older"
                enabled: true
                priority: 100
              
              - id: "status-check"
                name: "Status Verification"
                condition: "#status == 'active'"
                message: "Account must be active"
                enabled: true
                priority: 200
            """;

        Map<String, Object> request = new HashMap<>();
        request.put("yamlContent", validYaml);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<YamlValidationResponse> response = restTemplate.postForEntity(
            getBaseUrl() + "/validate", entity, YamlValidationResponse.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        YamlValidationResponse validationResponse = response.getBody();
        assertTrue(validationResponse.isValid(), "YAML should be valid");
        assertEquals("YAML configuration is valid", validationResponse.getMessage());
        
        // Verify metadata extraction
        assertNotNull(validationResponse.getMetadata());
        assertEquals("Integration Test Rules", validationResponse.getMetadata().getName());
        assertEquals("1.0.0", validationResponse.getMetadata().getVersion());
        assertEquals("Test configuration for integration testing", validationResponse.getMetadata().getDescription());
        
        // Verify statistics
        assertNotNull(validationResponse.getStatistics());
        assertEquals(2, validationResponse.getStatistics().getRulesCount());
        assertEquals(0, validationResponse.getStatistics().getEnrichmentsCount());
        assertEquals(0, validationResponse.getStatistics().getErrorCount());
        assertEquals(0, validationResponse.getStatistics().getWarningCount());
        
        // Verify no errors or warnings
        assertTrue(validationResponse.getErrors().isEmpty());
        assertTrue(validationResponse.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("Should detect invalid YAML syntax")
    void shouldDetectInvalidYamlSyntax() {
        // Given - Invalid YAML with syntax errors
        String invalidYaml = """
            metadata:
              name: "Invalid YAML"
              version: "1.0.0"
            rules:
              - id: "test-rule"
                name: "Test Rule"
                condition: "#age > 18"
                message: "Unclosed string
            """;

        Map<String, Object> request = new HashMap<>();
        request.put("yamlContent", invalidYaml);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<YamlValidationResponse> response = restTemplate.postForEntity(
            getBaseUrl() + "/validate", entity, YamlValidationResponse.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        YamlValidationResponse validationResponse = response.getBody();
        assertFalse(validationResponse.isValid(), "Invalid YAML should be detected");
        assertEquals("YAML configuration has validation errors", validationResponse.getMessage());
        
        // Should have errors
        assertFalse(validationResponse.getErrors().isEmpty(), "Should have validation errors");
        assertTrue(validationResponse.getStatistics().getErrorCount() > 0);
    }

    @Test
    @DisplayName("Should process XML data with numeric type conversion")
    void shouldProcessXmlDataWithNumericTypeConversion() {
        // Given - XML data with numeric values and YAML rules
        String xmlData = """
            <customer>
                <name>John Doe</name>
                <age>25</age>
                <balance>1500.75</balance>
                <status>active</status>
            </customer>
            """;

        String yamlRules = """
            metadata:
              name: "XML Processing Test"
              version: "1.0.0"
            
            rules:
              - id: "age-rule"
                name: "Age Check"
                condition: "#age >= 21"
                message: "Customer is of legal age"
              
              - id: "balance-rule"
                name: "Balance Check"
                condition: "#balance > 1000"
                message: "High value customer"
            """;

        PlaygroundRequest request = new PlaygroundRequest();
        request.setSourceData(xmlData);
        request.setYamlRules(yamlRules);
        request.setDataFormat("XML");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PlaygroundRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<PlaygroundResponse> response = restTemplate.postForEntity(
            getBaseUrl() + "/process", entity, PlaygroundResponse.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        PlaygroundResponse playgroundResponse = response.getBody();
        assertTrue(playgroundResponse.isSuccess(), "Processing should succeed");
        
        // Verify enriched data has correct types
        assertNotNull(playgroundResponse.getEnrichment());
        assertNotNull(playgroundResponse.getEnrichment().getEnrichedData());
        
        Object age = playgroundResponse.getEnrichment().getEnrichedData().get("age");
        Object balance = playgroundResponse.getEnrichment().getEnrichedData().get("balance");
        
        // Critical test: XML numeric values should be converted to actual numbers
        assertTrue(age instanceof Number, "Age should be converted to a number, but was: " + age.getClass().getSimpleName());
        assertTrue(balance instanceof Number, "Balance should be converted to a number, but was: " + balance.getClass().getSimpleName());
        
        assertEquals(25, ((Number) age).intValue());
        assertEquals(1500.75, ((Number) balance).doubleValue(), 0.01);
        
        // Verify rules executed correctly with numeric comparisons
        // Note: APEX rules engine stops after first matching rule
        assertNotNull(playgroundResponse.getValidation());
        assertTrue(playgroundResponse.getValidation().isValid());
        assertEquals(1, playgroundResponse.getValidation().getRulesExecuted());
        assertEquals(1, playgroundResponse.getValidation().getRulesPassed());
        assertEquals(0, playgroundResponse.getValidation().getRulesFailed());
    }

    @Test
    @DisplayName("Should handle JSON data processing correctly")
    void shouldHandleJsonDataProcessingCorrectly() {
        // Given - JSON data and YAML rules
        String jsonData = """
            {
                "name": "Jane Smith",
                "age": 30,
                "department": "Engineering",
                "salary": 75000,
                "active": true
            }
            """;

        String yamlRules = """
            metadata:
              name: "JSON Processing Test"
              version: "1.0.0"
            
            rules:
              - id: "salary-rule"
                name: "Salary Check"
                condition: "#salary >= 50000"
                message: "Above minimum salary"
              
              - id: "active-rule"
                name: "Active Status"
                condition: "#active == true"
                message: "Employee is active"
            """;

        PlaygroundRequest request = new PlaygroundRequest();
        request.setSourceData(jsonData);
        request.setYamlRules(yamlRules);
        request.setDataFormat("JSON");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PlaygroundRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<PlaygroundResponse> response = restTemplate.postForEntity(
            getBaseUrl() + "/process", entity, PlaygroundResponse.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        PlaygroundResponse playgroundResponse = response.getBody();
        assertTrue(playgroundResponse.isSuccess(), "JSON processing should succeed");
        
        // Verify data types are preserved
        assertNotNull(playgroundResponse.getEnrichment().getEnrichedData());
        Object salary = playgroundResponse.getEnrichment().getEnrichedData().get("salary");
        Object active = playgroundResponse.getEnrichment().getEnrichedData().get("active");
        
        assertTrue(salary instanceof Number, "Salary should be a number");
        assertTrue(active instanceof Boolean, "Active should be a boolean");
        
        assertEquals(75000, ((Number) salary).intValue());
        assertEquals(true, active);
        
        // Verify rules passed (engine stops after first match)
        assertEquals(1, playgroundResponse.getValidation().getRulesPassed());
    }

    @Test
    @DisplayName("Should handle CSV data processing correctly")
    void shouldHandleCsvDataProcessingCorrectly() {
        // Given - CSV data and YAML rules
        String csvData = """
            name,age,department,salary
            Alice Johnson,28,Marketing,65000
            """;

        String yamlRules = """
            metadata:
              name: "CSV Processing Test"
              version: "1.0.0"
            
            rules:
              - id: "age-rule"
                name: "Age Check"
                condition: "#age >= 25"
                message: "Minimum age requirement met"
            """;

        PlaygroundRequest request = new PlaygroundRequest();
        request.setSourceData(csvData);
        request.setYamlRules(yamlRules);
        request.setDataFormat("CSV");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PlaygroundRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<PlaygroundResponse> response = restTemplate.postForEntity(
            getBaseUrl() + "/process", entity, PlaygroundResponse.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        PlaygroundResponse playgroundResponse = response.getBody();
        assertTrue(playgroundResponse.isSuccess(), "CSV processing should succeed");
        
        // Verify CSV parsing worked
        assertNotNull(playgroundResponse.getEnrichment().getEnrichedData());
        assertEquals("Alice Johnson", playgroundResponse.getEnrichment().getEnrichedData().get("name"));
        
        // Age should be converted to number for rule evaluation
        Object age = playgroundResponse.getEnrichment().getEnrichedData().get("age");
        assertTrue(age instanceof Number, "Age should be converted to number for rule evaluation");
        assertEquals(28, ((Number) age).intValue());
    }
}
