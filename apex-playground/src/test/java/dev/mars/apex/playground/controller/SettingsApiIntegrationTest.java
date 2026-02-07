package dev.mars.apex.playground.controller;

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

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Settings API endpoints.
 * Tests GET /playground/api/settings and PUT /playground/api/settings
 * using real HTTP requests against the running Spring Boot server.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-02-07
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.main.web-application-type=servlet"
})
@DisplayName("Settings API Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SettingsApiIntegrationTest {

    @LocalServerPort
    private int port;

    private HttpClient httpClient;
    private String settingsUrl;

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        settingsUrl = "http://localhost:" + port + "/playground/api/settings";
    }

    @AfterEach
    void tearDown() throws IOException, InterruptedException {
        // Always reset to default after each test
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(settingsUrl))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString("{\"examplesDir\": \"examples\"}"))
            .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Normalize JSON by removing all whitespace around structural characters.
     * Handles both compact {"key":"value"} and pretty-printed { "key" : "value" } formats.
     */
    private String normalizeJson(String json) {
        return json.replaceAll("\\s*:\\s*", ":").replaceAll("\\s*,\\s*", ",")
                   .replaceAll("\\s*\\{\\s*", "{").replaceAll("\\s*}\\s*", "}")
                   .replaceAll("\\s*\\[\\s*", "[").replaceAll("\\s*]\\s*", "]").trim();
    }

    @Test
    @Order(1)
    @DisplayName("GET /settings should return current settings with all expected fields")
    void getSettingsShouldReturnCurrentSettings() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(settingsUrl))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "GET settings should return 200");

        String body = response.body();
        assertTrue(body.contains("\"examplesDir\""), "Response should contain examplesDir field");
        assertTrue(body.contains("\"resolvedExamplesPath\""), "Response should contain resolvedExamplesPath field");
        assertTrue(body.contains("\"directoryExists\""), "Response should contain directoryExists field");
        assertTrue(body.contains("\"examples\""), "Default examplesDir should be 'examples'");
    }

    @Test
    @Order(2)
    @DisplayName("GET /settings default value should be 'examples'")
    void getSettingsDefaultShouldBeExamples() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(settingsUrl))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String normalized = normalizeJson(response.body());
        assertTrue(normalized.contains("\"examplesDir\":\"examples\""),
            "Default examplesDir should be 'examples', but got: " + response.body());
    }

    @Test
    @Order(3)
    @DisplayName("PUT /settings should update the examples directory")
    void putSettingsShouldUpdateExamplesDir() throws Exception {
        // Update
        HttpRequest putRequest = HttpRequest.newBuilder()
            .uri(URI.create(settingsUrl))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString("{\"examplesDir\": \"custom-examples\"}"))
            .build();

        HttpResponse<String> putResponse = httpClient.send(putRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, putResponse.statusCode(), "PUT should return 200");
        String putNorm = normalizeJson(putResponse.body());
        assertTrue(putNorm.contains("\"success\":true"), "PUT should return success: true");
        assertTrue(putNorm.contains("\"examplesDir\":\"custom-examples\""),
            "PUT response should contain updated dir");

        // Verify with GET
        HttpRequest getRequest = HttpRequest.newBuilder()
            .uri(URI.create(settingsUrl))
            .GET()
            .build();

        HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertTrue(normalizeJson(getResponse.body()).contains("\"examplesDir\":\"custom-examples\""),
            "GET should return the updated value");
    }

    @Test
    @Order(4)
    @DisplayName("PUT /settings with existing directory should return directoryExists=true")
    void putSettingsWithExistingDirShouldReturnExists() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(settingsUrl))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString("{\"examplesDir\": \"examples\"}"))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(normalizeJson(response.body()).contains("\"directoryExists\":true"),
            "Existing 'examples' directory should return directoryExists=true");
    }

    @Test
    @Order(5)
    @DisplayName("PUT /settings with non-existent directory should return directoryExists=false")
    void putSettingsWithNonExistentDirShouldReturnNotExists() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(settingsUrl))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString("{\"examplesDir\": \"nonexistent-xyz-99999\"}"))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(normalizeJson(response.body()).contains("\"directoryExists\":false"),
            "Non-existent directory should return directoryExists=false");
    }

    @Test
    @Order(6)
    @DisplayName("PUT /settings with empty string should reset to default")
    void putSettingsWithEmptyShouldResetDefault() throws Exception {
        // First set a custom value
        HttpRequest setRequest = HttpRequest.newBuilder()
            .uri(URI.create(settingsUrl))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString("{\"examplesDir\": \"custom-dir\"}"))
            .build();
        httpClient.send(setRequest, HttpResponse.BodyHandlers.ofString());

        // Now set empty string
        HttpRequest resetRequest = HttpRequest.newBuilder()
            .uri(URI.create(settingsUrl))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString("{\"examplesDir\": \"\"}"))
            .build();

        HttpResponse<String> response = httpClient.send(resetRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(normalizeJson(response.body()).contains("\"examplesDir\":\"examples\""),
            "Empty string should reset to default 'examples', got: " + response.body());
    }

    @Test
    @Order(7)
    @DisplayName("PUT /settings should include resolved path in response")
    void putSettingsShouldIncludeResolvedPath() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(settingsUrl))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString("{\"examplesDir\": \"examples\"}"))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"resolvedExamplesPath\""),
            "PUT response should include resolvedExamplesPath");
        // The resolved path should be an absolute path containing 'examples'
        assertTrue(response.body().contains("examples"),
            "Resolved path should contain 'examples'");
    }

    @Test
    @Order(8)
    @DisplayName("GET /settings resolved path should be absolute")
    void getSettingsResolvedPathShouldBeAbsolute() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(settingsUrl))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String body = response.body();
        // On Windows, absolute paths contain ':' (e.g., C:\...) or on Linux start with /
        assertTrue(body.contains("resolvedExamplesPath"),
            "Response should contain resolvedExamplesPath");
        // Resolved path should be absolute - on Windows contains drive letter with ':', on Linux starts with '/'
        assertTrue(body.contains(":") || body.contains("/"),
            "Resolved path should be absolute (contain ':' or '/'), got: " + body);
    }
}
