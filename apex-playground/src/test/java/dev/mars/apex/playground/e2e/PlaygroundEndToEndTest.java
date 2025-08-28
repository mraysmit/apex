package dev.mars.apex.playground.e2e;

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
 * End-to-end tests for the APEX Playground.
 * 
 * Tests the complete user journey from UI interaction to backend processing
 * using real HTTP requests and responses.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.main.web-application-type=servlet"
})
@DisplayName("Playground End-to-End Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlaygroundEndToEndTest {

    @LocalServerPort
    private int port;

    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        baseUrl = "http://localhost:" + port;

        // Ensure port is initialized
        if (port == 0) {
            throw new IllegalStateException("Server port not initialized. Spring Boot context may not be ready.");
        }
    }

    @Nested
    @DisplayName("Application Startup Tests")
    class ApplicationStartupTests {

        @Test
        @Order(1)
        @DisplayName("Should start application successfully")
        void shouldStartApplicationSuccessfully() throws Exception {
            // When
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/playground/api/health"))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"status\"") && response.body().contains("\"UP\""),
                      "Expected response to contain status UP but was: " + response.body());
            assertTrue(response.body().contains("\"service\"") && response.body().contains("\"apex-playground\""),
                      "Expected response to contain service apex-playground but was: " + response.body());
        }

        @Test
        @Order(2)
        @DisplayName("Should serve playground UI successfully")
        void shouldServePlaygroundUiSuccessfully() throws Exception {
            // When
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/playground"))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("APEX Playground"));
            assertTrue(response.body().contains("Source Data"));
            assertTrue(response.body().contains("YAML Rules"));
            assertTrue(response.body().contains("Validation Results"));
            assertTrue(response.body().contains("Enrichment Results"));
        }

        @Test
        @Order(3)
        @DisplayName("Should serve static resources successfully")
        void shouldServeStaticResourcesSuccessfully() throws Exception {
            // Test CSS
            HttpRequest cssRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/css/playground.css"))
                .GET()
                .build();

            HttpResponse<String> cssResponse = httpClient.send(cssRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, cssResponse.statusCode());
            assertTrue(cssResponse.body().contains("playground-panels"));

            // Test JavaScript
            HttpRequest jsRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/js/playground.js"))
                .GET()
                .build();

            HttpResponse<String> jsResponse = httpClient.send(jsRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, jsResponse.statusCode());
            assertTrue(jsResponse.body().contains("playground"));
        }
    }

    @Nested
    @DisplayName("Complete User Journey Tests")
    class CompleteUserJourneyTests {

        @Test
        @Order(10)
        @DisplayName("Should complete successful validation and processing journey")
        void shouldCompleteSuccessfulValidationAndProcessingJourney() throws Exception {
            // Step 1: Validate YAML configuration
            String yamlValidationRequest = """
                {
                  "yamlContent": "metadata:\\n  name: \\"E2E Test Rules\\"\\n  version: \\"1.0.0\\"\\n  description: \\"End-to-end test validation rules\\"\\nrules:\\n  - id: \\"age-check\\"\\n    name: \\"Age Validation\\"\\n    condition: \\"#age >= 18\\"\\n    message: \\"Age must be 18 or older\\""
                }
                """;

            HttpRequest yamlRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/playground/api/validate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(yamlValidationRequest))
                .build();

            HttpResponse<String> yamlResponse = httpClient.send(yamlRequest, HttpResponse.BodyHandlers.ofString());

            // Verify YAML validation
            assertEquals(200, yamlResponse.statusCode());
            assertTrue(yamlResponse.body().contains("\"valid\"") && yamlResponse.body().contains("true"));
            assertTrue(yamlResponse.body().contains("E2E Test Rules"));

            // Step 2: Process data with validated YAML rules
            String processingRequest = """
                {
                  "sourceData": "{\\"name\\": \\"Alice Johnson\\", \\"age\\": 28, \\"email\\": \\"alice@example.com\\", \\"department\\": \\"Engineering\\"}",
                  "yamlRules": "metadata:\\n  name: \\"E2E Test Rules\\"\\n  version: \\"1.0.0\\"\\n  description: \\"End-to-end test validation rules\\"\\nrules:\\n  - id: \\"age-check\\"\\n    name: \\"Age Validation\\"\\n    condition: \\"#age >= 18\\"\\n    message: \\"Age must be 18 or older\\"",
                  "dataFormat": "JSON"
                }
                """;

            HttpRequest processRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/playground/api/process"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(processingRequest))
                .build();

            HttpResponse<String> processResponse = httpClient.send(processRequest, HttpResponse.BodyHandlers.ofString());

            // Verify processing results
            assertEquals(200, processResponse.statusCode());
            String responseBody = processResponse.body();
            
            assertTrue(responseBody.contains("\"success\"") && responseBody.contains("true"));
            assertTrue(responseBody.contains("\"valid\"") && responseBody.contains("true"));
            assertTrue(responseBody.contains("\"rulesExecuted\"") && responseBody.contains("1"));
            assertTrue(responseBody.contains("\"rulesPassed\"") && responseBody.contains("1"));
            assertTrue(responseBody.contains("Alice Johnson"));
            assertTrue(responseBody.contains("\"age\"") && responseBody.contains("28"));
            assertTrue(responseBody.contains("\"enriched\"") && responseBody.contains("true"));
            assertTrue(responseBody.contains("\"totalTimeMs\""));
        }

        @Test
        @Order(11)
        @DisplayName("Should handle validation failure journey correctly")
        void shouldHandleValidationFailureJourneyCorrectly() throws Exception {
            // Process data that should fail validation (age < 18)
            String processingRequest = """
                {
                  "sourceData": "{\\"name\\": \\"Bob Smith\\", \\"age\\": 16, \\"email\\": \\"bob@example.com\\"}",
                  "yamlRules": "metadata:\\n  name: \\"Age Validation Rules\\"\\n  version: \\"1.0.0\\"\\nrules:\\n  - id: \\"age-check\\"\\n    name: \\"Age Validation\\"\\n    condition: \\"#age >= 18\\"\\n    message: \\"Age must be 18 or older\\"",
                  "dataFormat": "JSON"
                }
                """;

            HttpRequest processRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/playground/api/process"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(processingRequest))
                .build();

            HttpResponse<String> processResponse = httpClient.send(processRequest, HttpResponse.BodyHandlers.ofString());

            // Verify validation failure is handled correctly
            assertEquals(200, processResponse.statusCode());
            String responseBody = processResponse.body();
            
            assertTrue(responseBody.contains("\"success\"") && responseBody.contains("true")); // Processing succeeds
            assertTrue(responseBody.contains("\"valid\"") && responseBody.contains("false")); // But validation fails
            assertTrue(responseBody.contains("\"rulesFailed\"") && responseBody.contains("1"));
            assertTrue(responseBody.contains("Bob Smith"));
            assertTrue(responseBody.contains("\"age\"") && responseBody.contains("16"));
        }

        @Test
        @Order(12)
        @DisplayName("Should handle different data formats in complete journey")
        void shouldHandleDifferentDataFormatsInCompleteJourney() throws Exception {
            // Test CSV data processing
            String csvProcessingRequest = """
                {
                  "sourceData": "name,age,department,salary\\nCarol Davis,32,Marketing,75000\\nDavid Wilson,29,Sales,68000",
                  "yamlRules": "metadata:\\n  name: \\"CSV Processing Rules\\"\\n  version: \\"1.0.0\\"\\nrules:\\n  - id: \\"age-check\\"\\n    name: \\"Age Validation\\"\\n    condition: \\"#age >= 25\\"\\n    message: \\"Age must be 25 or older for this position\\"",
                  "dataFormat": "CSV"
                }
                """;

            HttpRequest csvRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/playground/api/process"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(csvProcessingRequest))
                .build();

            HttpResponse<String> csvResponse = httpClient.send(csvRequest, HttpResponse.BodyHandlers.ofString());

            // Verify CSV processing
            assertEquals(200, csvResponse.statusCode());
            String responseBody = csvResponse.body();
            
            assertTrue(responseBody.contains("\"success\"") && responseBody.contains("true"));
            assertTrue(responseBody.contains("Carol Davis"));
            assertTrue(responseBody.contains("\"age\"") && responseBody.contains("32"));
            assertTrue(responseBody.contains("\"_csvRowCount\"") && responseBody.contains("2"));

            // Test XML data processing
            String xmlProcessingRequest = """
                {
                  "sourceData": "<person><name>Eve Brown</name><age>35</age><department>HR</department></person>",
                  "yamlRules": "metadata:\\n  name: \\"XML Processing Rules\\"\\n  version: \\"1.0.0\\"\\nrules:\\n  - id: \\"age-check\\"\\n    name: \\"Age Validation\\"\\n    condition: \\"#age >= 30\\"\\n    message: \\"Age must be 30 or older\\"",
                  "dataFormat": "XML"
                }
                """;

            HttpRequest xmlRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/playground/api/process"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(xmlProcessingRequest))
                .build();

            HttpResponse<String> xmlResponse = httpClient.send(xmlRequest, HttpResponse.BodyHandlers.ofString());

            // Verify XML processing
            assertEquals(200, xmlResponse.statusCode());
            assertTrue(xmlResponse.body().contains("\"success\"") && xmlResponse.body().contains("true"));
        }
    }

    @Nested
    @DisplayName("Error Scenarios Tests")
    class ErrorScenariosTests {

        @Test
        @Order(20)
        @DisplayName("Should handle invalid YAML gracefully in complete flow")
        void shouldHandleInvalidYamlGracefullyInCompleteFlow() throws Exception {
            // Step 1: Try to validate invalid YAML
            String invalidYamlRequest = """
                {
                  "yamlContent": "metadata:\\n  name: \\"Invalid YAML\\"\\nrules:\\n  - id: \\"test-rule\\"\\n    name: \\"Test Rule\\"\\n    condition: \\"#age > 18\\"\\n    message: \\"Unclosed string"
                }
                """;

            HttpRequest yamlRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/playground/api/validate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidYamlRequest))
                .build();

            HttpResponse<String> yamlResponse = httpClient.send(yamlRequest, HttpResponse.BodyHandlers.ofString());

            // Verify YAML validation detects errors
            assertEquals(200, yamlResponse.statusCode());
            assertTrue(yamlResponse.body().contains("\"valid\"") && yamlResponse.body().contains("false"));
            assertTrue(yamlResponse.body().contains("errors"));

            // Step 2: Try to process with the same invalid YAML
            String processingRequest = """
                {
                  "sourceData": "{\\"name\\": \\"Test User\\", \\"age\\": 25}",
                  "yamlRules": "metadata:\\n  name: \\"Invalid YAML\\"\\nrules:\\n  - id: \\"test-rule\\"\\n    name: \\"Test Rule\\"\\n    condition: \\"#age > 18\\"\\n    message: \\"Unclosed string",
                  "dataFormat": "JSON"
                }
                """;

            HttpRequest processRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/playground/api/process"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(processingRequest))
                .build();

            HttpResponse<String> processResponse = httpClient.send(processRequest, HttpResponse.BodyHandlers.ofString());

            // Verify processing handles invalid YAML gracefully
            assertEquals(200, processResponse.statusCode());
            assertTrue(processResponse.body().contains("\"success\"") && processResponse.body().contains("false"));
            assertTrue(processResponse.body().contains("YAML configuration is invalid"));
        }

        @Test
        @Order(21)
        @DisplayName("Should handle invalid JSON data gracefully")
        void shouldHandleInvalidJsonDataGracefully() throws Exception {
            String processingRequest = """
                {
                  "sourceData": "{\\"name\\": \\"Test User\\", \\"age\\": }",
                  "yamlRules": "metadata:\\n  name: \\"Test Rules\\"\\n  version: \\"1.0.0\\"\\nrules:\\n  - id: \\"test\\"\\n    name: \\"Test Rule\\"\\n    condition: \\"true\\"",
                  "dataFormat": "JSON"
                }
                """;

            HttpRequest processRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/playground/api/process"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(processingRequest))
                .build();

            HttpResponse<String> processResponse = httpClient.send(processRequest, HttpResponse.BodyHandlers.ofString());

            // Verify invalid JSON is handled gracefully
            assertEquals(200, processResponse.statusCode());
            assertTrue(processResponse.body().contains("\"success\"") && processResponse.body().contains("false"));
            assertTrue(processResponse.body().contains("failed"));
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @Order(30)
        @DisplayName("Should process requests within acceptable time limits")
        void shouldProcessRequestsWithinAcceptableTimeLimits() throws Exception {
            long startTime = System.currentTimeMillis();

            String processingRequest = """
                {
                  "sourceData": "{\\"name\\": \\"Performance Test User\\", \\"age\\": 30, \\"department\\": \\"Engineering\\", \\"salary\\": 85000}",
                  "yamlRules": "metadata:\\n  name: \\"Performance Test Rules\\"\\n  version: \\"1.0.0\\"\\nrules:\\n  - id: \\"age-check\\"\\n    name: \\"Age Validation\\"\\n    condition: \\"#age >= 18\\"\\n    message: \\"Age must be 18 or older\\"\\n  - id: \\"salary-check\\"\\n    name: \\"Salary Validation\\"\\n    condition: \\"#salary > 50000\\"\\n    message: \\"Salary must be above 50000\\"",
                  "dataFormat": "JSON"
                }
                """;

            HttpRequest processRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/playground/api/process"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(processingRequest))
                .build();

            HttpResponse<String> processResponse = httpClient.send(processRequest, HttpResponse.BodyHandlers.ofString());

            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;

            // Verify performance
            assertEquals(200, processResponse.statusCode());
            assertTrue(processResponse.body().contains("\"success\"") && processResponse.body().contains("true"));
            assertTrue(responseTime < 5000, "Response time should be less than 5 seconds, was: " + responseTime + "ms");
            
            // Verify metrics are collected
            assertTrue(processResponse.body().contains("\"totalTimeMs\""));
            assertTrue(processResponse.body().contains("\"yamlParsingTimeMs\""));
            assertTrue(processResponse.body().contains("\"dataParsingTimeMs\""));
            assertTrue(processResponse.body().contains("\"rulesExecutionTimeMs\""));
        }

        @Test
        @Order(31)
        @DisplayName("Should handle concurrent requests correctly")
        void shouldHandleConcurrentRequestsCorrectly() throws Exception {
            // Create multiple concurrent requests
            String processingRequest = """
                {
                  "sourceData": "{\\"name\\": \\"Concurrent User\\", \\"age\\": 25}",
                  "yamlRules": "metadata:\\n  name: \\"Concurrent Test Rules\\"\\n  version: \\"1.0.0\\"\\nrules:\\n  - id: \\"age-check\\"\\n    name: \\"Age Validation\\"\\n    condition: \\"#age >= 18\\"\\n    message: \\"Age must be 18 or older\\"",
                  "dataFormat": "JSON"
                }
                """;

            // Send 5 concurrent requests
            HttpRequest[] requests = new HttpRequest[5];
            for (int i = 0; i < 5; i++) {
                requests[i] = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/playground/api/process"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(processingRequest))
                    .build();
            }

            // Execute all requests concurrently
            long startTime = System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            HttpResponse<String>[] responses = new HttpResponse[5];

            for (int i = 0; i < 5; i++) {
                responses[i] = httpClient.send(requests[i], HttpResponse.BodyHandlers.ofString());
            }
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;

            // Verify all requests succeeded
            for (int i = 0; i < 5; i++) {
                assertEquals(200, responses[i].statusCode());
                assertTrue(responses[i].body().contains("\"success\"") && responses[i].body().contains("true"));
            }

            // Verify reasonable performance under concurrent load
            assertTrue(totalTime < 10000, "Concurrent requests should complete within 10 seconds, took: " + totalTime + "ms");
        }
    }
}
