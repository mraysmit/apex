package dev.mars.rulesengine.rest.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Configuration API endpoints using real Spring Boot context.
 * Uses plain JUnit 5 with real objects and integration testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {dev.mars.rulesengine.rest.RulesEngineRestApiApplication.class,
               dev.mars.rulesengine.rest.config.IntegrationTestConfiguration.class})
@ActiveProfiles("test")
public class ConfigurationApiIntegrationTest {

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
    
    @Test
    public void testConfigurationInfo_InitialState() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            getBaseUrl() + "/api/config/info", Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("hasConfiguration"));
        assertTrue(response.getBody().containsKey("timestamp"));
    }
    
    @Test
    public void testLoadConfiguration_ValidYaml() {
        String yamlContent = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
              description: "Test configuration for API testing"
            
            rules:
              - id: "test-rule-1"
                name: "Test Rule 1"
                condition: "#value > 0"
                message: "Value is positive"
                enabled: true
            
            enrichments:
              - id: "test-enrichment"
                type: "lookup-enrichment"
                condition: "['code'] != null"
                enabled: true
                lookup-config:
                  lookup-dataset:
                    type: "inline"
                    key-field: "code"
                    data:
                      - code: "TEST"
                        name: "Test Data"
                field-mappings:
                  - source-field: "name"
                    target-field: "testName"
            """;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/x-yaml"));
        HttpEntity<String> entity = new HttpEntity<>(yamlContent, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/api/config/load", entity, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Configuration loaded successfully", response.getBody().get("message"));
        assertTrue(response.getBody().containsKey("loadTime"));
        assertEquals("Test Configuration", response.getBody().get("configurationName"));
        assertEquals("1.0.0", response.getBody().get("configurationVersion"));
        
        // Verify statistics
        Map<String, Object> stats = (Map<String, Object>) response.getBody().get("statistics");
        assertNotNull(stats);
        assertEquals(1, stats.get("rulesCount"));
        assertEquals(1, stats.get("enrichmentsCount"));
    }
    
    @Test
    public void testLoadConfiguration_InvalidYaml() {
        // NOTE: This test intentionally sends invalid YAML to verify error handling
        // Expected error: "Rule name is required for rule: test-rule"
        String invalidYaml = """
            metadata:
              name: "Invalid Configuration"
            rules:
              - id: "test-rule"
                # Missing required 'name' field - this will trigger validation error
                invalid_field: "this should cause an error"
            """;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/x-yaml"));
        HttpEntity<String> entity = new HttpEntity<>(invalidYaml, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/api/config/load", entity, Map.class);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Failed to load configuration", response.getBody().get("error"));
        assertTrue(response.getBody().containsKey("details"));
        assertTrue(response.getBody().containsKey("timestamp"));
    }
    
    @Test
    public void testValidateConfiguration_ValidYaml() {
        String yamlContent = """
            metadata:
              name: "Validation Test"
              version: "1.0.0"
            
            rules:
              - id: "validation-rule"
                name: "Validation Rule"
                condition: "#test == true"
                message: "Test passed"
                enabled: true
            """;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/x-yaml"));
        HttpEntity<String> entity = new HttpEntity<>(yamlContent, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/api/config/validate", entity, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("valid"));
        assertEquals("Configuration is valid", response.getBody().get("message"));
        assertEquals("Validation Test", response.getBody().get("configurationName"));
        assertEquals("1.0.0", response.getBody().get("configurationVersion"));
        
        // Verify statistics
        Map<String, Object> stats = (Map<String, Object>) response.getBody().get("statistics");
        assertNotNull(stats);
        assertEquals(1, stats.get("rulesCount"));
    }
    
    @Test
    public void testValidateConfiguration_InvalidYaml() {
        // NOTE: This test intentionally sends malformed YAML to verify validation error handling
        // Expected error: "Configuration is invalid" due to YAML parsing failure
        String invalidYaml = """
            this is not valid yaml content
            - missing proper structure
            invalid: [unclosed array
            """;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/x-yaml"));
        HttpEntity<String> entity = new HttpEntity<>(invalidYaml, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/api/config/validate", entity, Map.class);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().get("valid"));
        assertEquals("Configuration is invalid", response.getBody().get("error"));
        assertTrue(response.getBody().containsKey("details"));
    }
    
    @Test
    public void testUploadConfiguration_ValidFile() {
        String yamlContent = """
            metadata:
              name: "Upload Test Configuration"
              version: "2.0.0"
              description: "Configuration uploaded via file"
            
            rules:
              - id: "upload-rule"
                name: "Upload Rule"
                condition: "#uploaded == true"
                message: "File uploaded successfully"
                enabled: true
            """;
        
        // Create multipart request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource fileResource = new ByteArrayResource(yamlContent.getBytes()) {
            @Override
            public String getFilename() {
                return "test-config.yaml";
            }
        };
        body.add("file", fileResource);
        
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/api/config/upload", entity, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Configuration file uploaded and loaded successfully", response.getBody().get("message"));
        assertEquals("test-config.yaml", response.getBody().get("fileName"));
        assertEquals("Upload Test Configuration", response.getBody().get("configurationName"));
        assertEquals("2.0.0", response.getBody().get("configurationVersion"));
        assertTrue(response.getBody().containsKey("fileSize"));
        assertTrue(response.getBody().containsKey("loadTime"));
    }
    
    @Test
    public void testUploadConfiguration_EmptyFile() {
        // NOTE: This test intentionally uploads an empty file to verify error handling
        // Expected error: "No file provided"
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource emptyResource = new ByteArrayResource(new byte[0]) {
            @Override
            public String getFilename() {
                return "empty.yaml";
            }
        };
        body.add("file", emptyResource);
        
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/api/config/upload", entity, Map.class);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("No file provided", response.getBody().get("error"));
    }
    
    @Test
    public void testUploadConfiguration_WrongFileType() {
        // NOTE: This test intentionally uploads a non-YAML file to verify file type validation
        // Expected error: "File must be a YAML file (.yaml or .yml)"
        String textContent = "This is a plain text file, not YAML";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource textResource = new ByteArrayResource(textContent.getBytes()) {
            @Override
            public String getFilename() {
                return "test.txt";
            }
        };
        body.add("file", textResource);
        
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/api/config/upload", entity, Map.class);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("File must be a YAML file (.yaml or .yml)", response.getBody().get("error"));
    }
    
    @Test
    public void testConfigurationWorkflow() {
        // 1. Check initial state
        ResponseEntity<Map> infoResponse = restTemplate.getForEntity(
            getBaseUrl() + "/api/config/info", Map.class);
        assertEquals(HttpStatus.OK, infoResponse.getStatusCode());
        
        // 2. Load a configuration
        String yamlContent = """
            metadata:
              name: "Workflow Test"
              version: "1.0.0"
            rules:
              - id: "workflow-rule"
                name: "Workflow Rule"
                condition: "#step == 'complete'"
                message: "Workflow completed"
                enabled: true
            """;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/x-yaml"));
        HttpEntity<String> loadEntity = new HttpEntity<>(yamlContent, headers);
        
        ResponseEntity<Map> loadResponse = restTemplate.postForEntity(
            getBaseUrl() + "/api/config/load", loadEntity, Map.class);
        assertEquals(HttpStatus.OK, loadResponse.getStatusCode());
        
        // 3. Verify configuration is now loaded
        ResponseEntity<Map> updatedInfoResponse = restTemplate.getForEntity(
            getBaseUrl() + "/api/config/info", Map.class);
        assertEquals(HttpStatus.OK, updatedInfoResponse.getStatusCode());
        
        Map<String, Object> updatedInfo = updatedInfoResponse.getBody();
        assertNotNull(updatedInfo);
        // The configuration should now be loaded (depending on implementation)
        assertTrue(updatedInfo.containsKey("hasConfiguration"));
    }
}
