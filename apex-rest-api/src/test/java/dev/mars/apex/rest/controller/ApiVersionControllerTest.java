package dev.mars.apex.rest.controller;

import dev.mars.apex.rest.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ApiVersionController.
 * Tests controller endpoints using real HTTP calls.
 */
@DisplayName("API Version Controller Integration Tests")
public class ApiVersionControllerTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("Version Information Tests")
    class VersionInformationTests {

        @Test
        @DisplayName("Should return API version information")
        void shouldReturnApiVersionInformation() {
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/version"), Map.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().containsKey("currentVersion"));
            assertTrue(response.getBody().containsKey("apiVersion"));
            assertTrue(response.getBody().containsKey("status"));
            assertTrue(response.getBody().containsKey("timestamp"));
        }

        @Test
        @DisplayName("Should include API version details")
        void shouldIncludeApiVersionDetails() {
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/version"), Map.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("1.0.0", response.getBody().get("apiVersion"));
            assertTrue(response.getBody().containsKey("supportedVersions"));
            assertTrue(response.getBody().containsKey("deprecatedVersions"));
        }

        @Test
        @DisplayName("Should include build information")
        void shouldIncludeBuildInformation() {
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/version"), Map.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().containsKey("releaseDate"));
            assertTrue(response.getBody().containsKey("status"));
            assertEquals("stable", response.getBody().get("status"));
        }
    }

    @Nested
    @DisplayName("Deprecation Information Tests")
    class DeprecationInformationTests {

        @Test
        @DisplayName("Should return deprecation information")
        void shouldReturnDeprecationInformation() {
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/version/deprecation"), Map.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().containsKey("currentDeprecations"));
            assertTrue(response.getBody().containsKey("plannedDeprecations"));
            assertTrue(response.getBody().containsKey("policy"));
            assertTrue(response.getBody().containsKey("migrationTimeline"));
            assertTrue(response.getBody().containsKey("contact"));
            assertTrue(response.getBody().containsKey("timestamp"));
        }
    }

    @Nested
    @DisplayName("Version Health Tests")
    class VersionHealthTests {

        @Test
        @DisplayName("Should return version health status")
        void shouldReturnVersionHealthStatus() {
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/version/health"), Map.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("healthy", response.getBody().get("status"));
            assertTrue(response.getBody().containsKey("version"));
            assertTrue(response.getBody().containsKey("apiVersion"));
        }

        @Test
        @DisplayName("Should include compatibility information")
        void shouldIncludeCompatibilityInformation() {
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/version/health"), Map.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().containsKey("compatibility"));
            assertTrue(response.getBody().containsKey("features"));
        }
    }

    // Note: The ApiVersionController only has getVersionInfo(), getDeprecationInfo(), and getVersionHealth() methods
    // Additional endpoints like getApiInfo(), getCompatibility(), getDocumentation(), getFeatures()
    // would be useful additions for comprehensive API information
}
