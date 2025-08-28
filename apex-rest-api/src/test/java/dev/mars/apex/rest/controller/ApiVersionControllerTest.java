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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ApiVersionController.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url("/api/version"), HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});

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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url("/api/version"), HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});

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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url("/api/version"), HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});

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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url("/api/version/deprecation"), HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});

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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url("/api/version/health"), HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});

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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url("/api/version/health"), HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});

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
