package dev.mars.apex.rest.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for API Version endpoints.
 * Tests the API versioning strategy implementation and version information endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {dev.mars.apex.rest.ApexRestApiApplication.class,
               dev.mars.apex.rest.config.IntegrationTestConfiguration.class})
@ActiveProfiles("test")
@DisplayName("API Version Integration Tests")
public class ApiVersionIntegrationTest {

    @BeforeAll
    static void setupTestEnvironment() {
        System.setProperty("test.environment", "true");
    }

    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private String getBaseUrl() {
        return "http://localhost:" + port;
    }
    
    @Nested
    @DisplayName("Version Information Tests")
    class VersionInformationTests {
        
        @Test
        @DisplayName("Should return current API version information")
        public void testGetVersionInfo() {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                getBaseUrl() + "/api/version",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            
            Map<String, Object> body = response.getBody();
            assertEquals("v1", body.get("currentVersion"));
            assertEquals("1.0.0", body.get("apiVersion"));
            assertEquals("stable", body.get("status"));
            assertEquals("2025-07-31", body.get("releaseDate"));
            assertNotNull(body.get("timestamp"));
            
            // Check supported versions
            @SuppressWarnings("unchecked")
            List<String> supportedVersions = (List<String>) body.get("supportedVersions");
            assertNotNull(supportedVersions);
            assertTrue(supportedVersions.contains("v1"));
            
            // Check documentation links
            @SuppressWarnings("unchecked")
            Map<String, Object> documentation = (Map<String, Object>) body.get("documentation");
            assertNotNull(documentation);
            assertTrue(documentation.containsKey("v1"));
            assertTrue(documentation.containsKey("swagger"));
        }
        
        @Test
        @DisplayName("Should return version compatibility information")
        public void testGetCompatibilityInfo() {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                getBaseUrl() + "/api/version/compatibility",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            
            Map<String, Object> body = response.getBody();
            assertNotNull(body.get("compatibilityMatrix"));
            assertNotNull(body.get("clientRequirements"));
            assertNotNull(body.get("featureSupport"));
            assertNotNull(body.get("timestamp"));
            
            // Check compatibility matrix
            @SuppressWarnings("unchecked")
            Map<String, Object> matrix = (Map<String, Object>) body.get("compatibilityMatrix");
            assertTrue(matrix.containsKey("v1"));
            
            // Check client requirements
            @SuppressWarnings("unchecked")
            Map<String, Object> requirements = (Map<String, Object>) body.get("clientRequirements");
            assertEquals("v1", requirements.get("minimumVersion"));
            assertEquals("v1", requirements.get("recommendedVersion"));
        }
        
        @Test
        @DisplayName("Should return deprecation information")
        public void testGetDeprecationInfo() {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                getBaseUrl() + "/api/version/deprecation",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            
            Map<String, Object> body = response.getBody();
            assertNotNull(body.get("currentDeprecations"));
            assertNotNull(body.get("plannedDeprecations"));
            assertNotNull(body.get("endOfLifeSchedule"));
            assertNotNull(body.get("migrationTimeline"));
            assertNotNull(body.get("policy"));
            assertNotNull(body.get("contact"));
            assertNotNull(body.get("timestamp"));
            
            // Check policy information
            @SuppressWarnings("unchecked")
            Map<String, Object> policy = (Map<String, Object>) body.get("policy");
            assertEquals("12 months", policy.get("minimumSupportPeriod"));
            assertTrue(policy.get("deprecationWarnings").toString().contains("HTTP headers"));
        }
        
        @Test
        @DisplayName("Should return version-aware health check")
        public void testGetVersionHealth() {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                getBaseUrl() + "/api/version/health",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            
            Map<String, Object> body = response.getBody();
            assertEquals("healthy", body.get("status"));
            assertEquals("v1", body.get("version"));
            assertEquals("1.0.0", body.get("apiVersion"));
            assertNotNull(body.get("compatibility"));
            assertNotNull(body.get("features"));
            assertNotNull(body.get("performance"));
            assertNotNull(body.get("timestamp"));
            
            // Check feature availability
            @SuppressWarnings("unchecked")
            Map<String, Object> features = (Map<String, Object>) body.get("features");
            assertEquals("available", features.get("ruleEvaluation"));
            assertEquals("available", features.get("validation"));
            assertEquals("available", features.get("configuration"));
            assertEquals("available", features.get("monitoring"));
        }
    }
    
    @Nested
    @DisplayName("Version Header Tests")
    class VersionHeaderTests {
        
        @Test
        @DisplayName("Should accept API-Version header")
        public void testApiVersionHeader() {
            HttpHeaders headers = new HttpHeaders();
            headers.set("API-Version", "v1");
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                getBaseUrl() + "/api/version",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }
        
        @Test
        @DisplayName("Should accept versioned Accept header")
        public void testVersionedAcceptHeader() {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.apex.v1+json");
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                getBaseUrl() + "/api/version",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }
    }
    
    @Nested
    @DisplayName("Version Documentation Tests")
    class VersionDocumentationTests {
        
        @Test
        @DisplayName("Should provide version information in all responses")
        public void testVersionInformationInResponses() {
            // Test that version information is consistently provided
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                getBaseUrl() + "/api/version",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            
            // Verify version consistency
            assertEquals("v1", body.get("currentVersion"));
            assertEquals("v1", body.get("latestVersion"));
            
            // Verify documentation links are accessible
            @SuppressWarnings("unchecked")
            Map<String, Object> docs = (Map<String, Object>) body.get("documentation");
            assertNotNull(docs.get("v1"));
            assertNotNull(docs.get("swagger"));
        }
    }
}
