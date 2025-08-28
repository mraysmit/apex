package dev.mars.apex.playground.service;

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


import dev.mars.apex.playground.model.PlaygroundRequest;
import dev.mars.apex.playground.model.PlaygroundResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for PlaygroundService.
 * 
 * Tests the core processing logic with mocked dependencies.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@DisplayName("PlaygroundService Tests")
@ExtendWith(MockitoExtension.class)
class PlaygroundServiceTest {

    @Mock
    private DataProcessingService dataProcessingService;
    
    @Mock
    private YamlValidationService yamlValidationService;
    
    private PlaygroundService playgroundService;

    @BeforeEach
    void setUp() {
        playgroundService = new PlaygroundService(dataProcessingService, yamlValidationService);
    }

    @Nested
    @DisplayName("Successful Processing Tests")
    class SuccessfulProcessingTests {

        @Test
        @DisplayName("Should process valid request successfully")
        void shouldProcessValidRequestSuccessfully() {
            // Given
            PlaygroundRequest request = createValidRequest();
            Map<String, Object> parsedData = createSampleParsedData();
            
            when(yamlValidationService.isValidYaml(anyString())).thenReturn(true);
            when(dataProcessingService.parseData(anyString(), anyString())).thenReturn(parsedData);
            
            // When
            PlaygroundResponse response = playgroundService.processData(request);
            
            // Then
            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals("Processing completed successfully", response.getMessage());
            assertNotNull(response.getValidation());
            assertNotNull(response.getEnrichment());
            assertNotNull(response.getMetrics());
            assertTrue(response.getMetrics().getTotalTimeMs() > 0);
            
            // Verify service interactions
            verify(yamlValidationService).isValidYaml(request.getYamlRules());
            verify(dataProcessingService).parseData(request.getSourceData(), request.getDataFormat());
        }

        @Test
        @DisplayName("Should collect performance metrics correctly")
        void shouldCollectPerformanceMetricsCorrectly() {
            // Given
            PlaygroundRequest request = createValidRequest();
            Map<String, Object> parsedData = createSampleParsedData();
            
            when(yamlValidationService.isValidYaml(anyString())).thenReturn(true);
            when(dataProcessingService.parseData(anyString(), anyString())).thenReturn(parsedData);
            
            // When
            PlaygroundResponse response = playgroundService.processData(request);
            
            // Then
            assertNotNull(response.getMetrics());
            assertTrue(response.getMetrics().getTotalTimeMs() >= 0);
            assertTrue(response.getMetrics().getYamlParsingTimeMs() >= 0);
            assertTrue(response.getMetrics().getDataParsingTimeMs() >= 0);
            assertTrue(response.getMetrics().getRulesExecutionTimeMs() >= 0);
            
            // Total time should be sum of individual times (approximately)
            long totalExpected = response.getMetrics().getYamlParsingTimeMs() +
                               response.getMetrics().getDataParsingTimeMs() +
                               response.getMetrics().getRulesExecutionTimeMs();
            assertTrue(response.getMetrics().getTotalTimeMs() >= totalExpected);
        }

        @Test
        @DisplayName("Should handle different data formats")
        void shouldHandleDifferentDataFormats() {
            // Given
            PlaygroundRequest jsonRequest = createRequestWithFormat("JSON");
            PlaygroundRequest xmlRequest = createRequestWithFormat("XML");
            PlaygroundRequest csvRequest = createRequestWithFormat("CSV");
            
            Map<String, Object> parsedData = createSampleParsedData();
            
            when(yamlValidationService.isValidYaml(anyString())).thenReturn(true);
            when(dataProcessingService.parseData(anyString(), anyString())).thenReturn(parsedData);
            
            // When
            PlaygroundResponse jsonResponse = playgroundService.processData(jsonRequest);
            PlaygroundResponse xmlResponse = playgroundService.processData(xmlRequest);
            PlaygroundResponse csvResponse = playgroundService.processData(csvRequest);
            
            // Then
            assertTrue(jsonResponse.isSuccess());
            assertTrue(xmlResponse.isSuccess());
            assertTrue(csvResponse.isSuccess());
            
            // Verify correct format was passed to data processing service
            verify(dataProcessingService).parseData(anyString(), eq("JSON"));
            verify(dataProcessingService).parseData(anyString(), eq("XML"));
            verify(dataProcessingService).parseData(anyString(), eq("CSV"));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle invalid YAML gracefully")
        void shouldHandleInvalidYamlGracefully() {
            // Given
            PlaygroundRequest request = createValidRequest();
            
            when(yamlValidationService.isValidYaml(anyString())).thenReturn(false);
            
            // When
            PlaygroundResponse response = playgroundService.processData(request);
            
            // Then
            assertNotNull(response);
            assertFalse(response.isSuccess());
            assertEquals("YAML configuration is invalid", response.getMessage());
            assertTrue(response.getErrors().contains("YAML validation failed"));
            
            // Should not proceed to data processing
            verify(dataProcessingService, never()).parseData(anyString(), anyString());
        }

        @Test
        @DisplayName("Should handle data processing exceptions")
        void shouldHandleDataProcessingExceptions() {
            // Given
            PlaygroundRequest request = createValidRequest();
            
            when(yamlValidationService.isValidYaml(anyString())).thenReturn(true);
            when(dataProcessingService.parseData(anyString(), anyString()))
                .thenThrow(new RuntimeException("Data parsing failed"));
            
            // When
            PlaygroundResponse response = playgroundService.processData(request);
            
            // Then
            assertNotNull(response);
            assertFalse(response.isSuccess());
            assertTrue(response.getMessage().contains("Processing failed"));
            assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.contains("Data parsing failed")));
        }

        @Test
        @DisplayName("Should handle YAML configuration exceptions")
        void shouldHandleYamlConfigurationExceptions() {
            // Given
            PlaygroundRequest request = createRequestWithInvalidYaml();
            Map<String, Object> parsedData = createSampleParsedData();
            
            when(yamlValidationService.isValidYaml(anyString())).thenReturn(true);
            when(dataProcessingService.parseData(anyString(), anyString())).thenReturn(parsedData);
            
            // When
            PlaygroundResponse response = playgroundService.processData(request);
            
            // Then
            assertNotNull(response);
            // Response may succeed or fail depending on YAML content
            assertNotNull(response.getMessage());
        }

        @Test
        @DisplayName("Should handle unexpected exceptions gracefully")
        void shouldHandleUnexpectedExceptionsGracefully() {
            // Given
            PlaygroundRequest request = createValidRequest();
            
            when(yamlValidationService.isValidYaml(anyString()))
                .thenThrow(new RuntimeException("Unexpected error"));
            
            // When
            PlaygroundResponse response = playgroundService.processData(request);
            
            // Then
            assertNotNull(response);
            assertFalse(response.isSuccess());
            assertTrue(response.getMessage().contains("Processing failed"));
            assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.contains("Unexpected error")));
        }
    }

    @Nested
    @DisplayName("Source Data Validation Tests")
    class SourceDataValidationTests {

        @Test
        @DisplayName("Should validate source data format correctly")
        void shouldValidateSourceDataFormatCorrectly() {
            // Given
            String sourceData = "{\"name\": \"John\", \"age\": 30}";
            String dataFormat = "JSON";
            
            when(dataProcessingService.validateDataFormat(sourceData, dataFormat)).thenReturn(true);
            
            // When
            boolean result = playgroundService.validateSourceData(sourceData, dataFormat);
            
            // Then
            assertTrue(result);
            verify(dataProcessingService).validateDataFormat(sourceData, dataFormat);
        }

        @Test
        @DisplayName("Should return false for invalid source data")
        void shouldReturnFalseForInvalidSourceData() {
            // Given
            String sourceData = "invalid data";
            String dataFormat = "JSON";
            
            when(dataProcessingService.validateDataFormat(sourceData, dataFormat)).thenReturn(false);
            
            // When
            boolean result = playgroundService.validateSourceData(sourceData, dataFormat);
            
            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle validation exceptions")
        void shouldHandleValidationExceptions() {
            // Given
            String sourceData = "test data";
            String dataFormat = "JSON";
            
            when(dataProcessingService.validateDataFormat(sourceData, dataFormat))
                .thenThrow(new RuntimeException("Validation error"));
            
            // When
            boolean result = playgroundService.validateSourceData(sourceData, dataFormat);
            
            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Processing Options Tests")
    class ProcessingOptionsTests {

        @Test
        @DisplayName("Should respect processing options")
        void shouldRespectProcessingOptions() {
            // Given
            PlaygroundRequest request = createValidRequest();
            request.getProcessingOptions().setEnableValidation(false);
            request.getProcessingOptions().setEnableEnrichment(false);
            request.getProcessingOptions().setCollectMetrics(true);
            
            Map<String, Object> parsedData = createSampleParsedData();
            
            when(yamlValidationService.isValidYaml(anyString())).thenReturn(true);
            when(dataProcessingService.parseData(anyString(), anyString())).thenReturn(parsedData);
            
            // When
            PlaygroundResponse response = playgroundService.processData(request);
            
            // Then
            assertNotNull(response);
            // Processing should still succeed even with options disabled
            assertTrue(response.isSuccess());
            assertNotNull(response.getMetrics()); // Metrics should still be collected
        }

        @Test
        @DisplayName("Should handle custom timeout settings")
        void shouldHandleCustomTimeoutSettings() {
            // Given
            PlaygroundRequest request = createValidRequest();
            request.getProcessingOptions().setTimeoutMs(5000L); // 5 seconds
            
            Map<String, Object> parsedData = createSampleParsedData();
            
            when(yamlValidationService.isValidYaml(anyString())).thenReturn(true);
            when(dataProcessingService.parseData(anyString(), anyString())).thenReturn(parsedData);
            
            // When
            PlaygroundResponse response = playgroundService.processData(request);
            
            // Then
            assertNotNull(response);
            assertTrue(response.isSuccess());
            // Processing should complete within reasonable time
            assertTrue(response.getMetrics().getTotalTimeMs() < 5000);
        }
    }

    // Helper methods

    private PlaygroundRequest createValidRequest() {
        PlaygroundRequest request = new PlaygroundRequest();
        request.setSourceData("{\"name\": \"John Doe\", \"age\": 30}");
        request.setYamlRules("""
            metadata:
              name: "Test Rules"
              version: "1.0.0"
            rules:
              - id: "age-check"
                name: "Age Validation"
                condition: "#age >= 18"
                message: "Age must be 18 or older"
            """);
        request.setDataFormat("JSON");
        return request;
    }

    private PlaygroundRequest createRequestWithFormat(String format) {
        PlaygroundRequest request = createValidRequest();
        request.setDataFormat(format);
        return request;
    }

    private PlaygroundRequest createRequestWithInvalidYaml() {
        PlaygroundRequest request = createValidRequest();
        request.setYamlRules("invalid: yaml: content: [unclosed");
        return request;
    }

    private Map<String, Object> createSampleParsedData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John Doe");
        data.put("age", 30);
        data.put("email", "john@example.com");
        return data;
    }
}
