package dev.mars.apex.core.config.yaml;

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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for YAML enrichment validation functionality.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
/**
 * Comprehensive unit tests for YAML enrichment validation functionality.
 * Tests all enrichment validation patterns documented in lookups.md.
 * 
 * This test class validates the comprehensive enrichment validation system
 * without using Mockito, following project guidelines.
 */
@DisplayName("YAML Enrichment Validation Tests")
class YamlEnrichmentValidationTest {

    private YamlConfigurationLoader configurationLoader;

    @BeforeEach
    void setUp() {
        configurationLoader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("Should validate basic enrichment structure successfully")
    void shouldValidateBasicEnrichmentStructureSuccessfully() {
        // Given
        String validYaml = createValidEnrichmentYaml();

        // When & Then
        assertDoesNotThrow(() -> {
            InputStream inputStream = new ByteArrayInputStream(validYaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(inputStream);
        });
    }

    @Test
    @DisplayName("Should fail validation when enrichment ID is missing")
    void shouldFailValidationWhenEnrichmentIdIsMissing() {
        // Given
        String invalidYaml = createEnrichmentYamlWithMissingId();

        // When & Then
        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            InputStream inputStream = new ByteArrayInputStream(invalidYaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(inputStream);
        });
        assertTrue(exception.getMessage().contains("Enrichment ID is required"));
    }

    @Test
    @DisplayName("Should fail validation when enrichment type is invalid")
    void shouldFailValidationWhenEnrichmentTypeIsInvalid() {
        // Given
        String invalidYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
              description: "Test configuration for enrichment validation"
              type: "rule-config"

            enrichments:
              - id: "test-enrichment"
                type: "invalid-type"
                condition: "#customerId != null"
                lookup-config:
                  lookup-service: "testService"
                  lookup-key: "#customerId"
            """;

        // When & Then
        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            InputStream inputStream = new ByteArrayInputStream(invalidYaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(inputStream);
        });
        assertTrue(exception.getMessage().contains("Invalid enrichment type 'invalid-type'"));
    }

    @Test
    @DisplayName("Should fail validation when lookup enrichment missing lookup config")
    void shouldFailValidationWhenLookupEnrichmentMissingLookupConfig() {
        // Given
        String invalidYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
              description: "Test configuration for enrichment validation"
              type: "rule-config"

            enrichments:
              - id: "test-lookup"
                type: "lookup-enrichment"
                condition: "#customerId != null"
            """;

        // When & Then
        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            InputStream inputStream = new ByteArrayInputStream(invalidYaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(inputStream);
        });
        assertTrue(exception.getMessage().contains("lookup-enrichment type requires 'lookup-config'"));
    }

    @Test
    @DisplayName("Should validate valid SpEL condition expression")
    void shouldValidateValidSpELConditionExpression() {
        // Given
        String validYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
              description: "Test configuration for enrichment validation"
              type: "rule-config"

            data-sources:
              - name: "testService"
                type: "inline"
                description: "Test data source for validation"

            enrichments:
              - id: "test-enrichment"
                type: "lookup-enrichment"
                condition: "#customerId != null && #region != null"
                lookup-config:
                  lookup-service: "testService"
                  lookup-key: "#customerId"
            """;

        // When & Then
        assertDoesNotThrow(() -> {
            InputStream inputStream = new ByteArrayInputStream(validYaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(inputStream);
        });
    }

    @Test
    @DisplayName("Should validate compound lookup key with string concatenation")
    void shouldValidateCompoundLookupKeyWithStringConcatenation() {
        // Given
        String validYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
              description: "Test configuration for enrichment validation"
              type: "rule-config"

            data-sources:
              - name: "testService"
                type: "inline"
                description: "Test data source for validation"

            enrichments:
              - id: "test-enrichment"
                type: "lookup-enrichment"
                condition: "#customerId != null && #region != null"
                lookup-config:
                  lookup-service: "testService"
                  lookup-key: "#customerId + '-' + #region"
            """;

        // When & Then
        assertDoesNotThrow(() -> {
            InputStream inputStream = new ByteArrayInputStream(validYaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(inputStream);
        });
    }

    @Test
    @DisplayName("Should validate conditional lookup key expression")
    void shouldValidateConditionalLookupKeyExpression() {
        // Given
        String validYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
              description: "Test configuration for enrichment validation"
              type: "rule-config"

            data-sources:
              - name: "testService"
                type: "inline"
                description: "Test data source for validation"

            enrichments:
              - id: "test-enrichment"
                type: "lookup-enrichment"
                condition: "#type != null"
                lookup-config:
                  lookup-service: "testService"
                  lookup-key: "#type == 'CUSTOMER' ? #customerId : #vendorId"
            """;

        // When & Then
        assertDoesNotThrow(() -> {
            InputStream inputStream = new ByteArrayInputStream(validYaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(inputStream);
        });
    }

    // Helper methods

    private String createValidEnrichmentYaml() {
        return """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
              description: "Test configuration for enrichment validation"
              type: "rule-config"

            data-sources:
              - name: "testService"
                type: "inline"
                description: "Test data source for validation"

            enrichments:
              - id: "test-enrichment"
                type: "lookup-enrichment"
                condition: "#customerId != null"
                lookup-config:
                  lookup-service: "testService"
                  lookup-key: "#customerId"
                field-mappings:
                  - source-field: "name"
                    target-field: "customerName"
            """;
    }

    private String createEnrichmentYamlWithMissingId() {
        return """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
              description: "Test configuration for enrichment validation"
              type: "rule-config"

            enrichments:
              - type: "lookup-enrichment"
                condition: "#customerId != null"
                lookup-config:
                  lookup-service: "testService"
                  lookup-key: "#customerId"
                field-mappings:
                  - source-field: "name"
                    target-field: "customerName"
            """;
    }

    @Test
    @DisplayName("Should validate enrichment with valid severity successfully")
    void shouldValidateEnrichmentWithValidSeveritySuccessfully() {
        // Given
        String validYaml = createEnrichmentYamlWithValidSeverity();

        // When & Then
        assertDoesNotThrow(() -> {
            InputStream inputStream = new ByteArrayInputStream(validYaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(inputStream);
        });
    }

    @Test
    @DisplayName("Should fail validation when enrichment has invalid severity")
    void shouldFailValidationWhenEnrichmentHasInvalidSeverity() {
        // Given
        String invalidYaml = createEnrichmentYamlWithInvalidSeverity();

        // When & Then
        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            InputStream inputStream = new ByteArrayInputStream(invalidYaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(inputStream);
        });

        assertTrue(exception.getMessage().contains("invalid severity"),
                   "Expected message to contain 'invalid severity', but was: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("Must be one of:"),
                   "Expected message to contain 'Must be one of:', but was: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("INFO") &&
                   exception.getMessage().contains("WARNING") &&
                   exception.getMessage().contains("ERROR"),
                   "Expected message to contain all severity levels, but was: " + exception.getMessage());
    }

    private String createEnrichmentYamlWithValidSeverity() {
        return """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
              description: "Test configuration for enrichment validation"
              type: "rule-config"

            data-sources:
              - name: "testService"
                type: "inline"

            enrichments:
              - id: "test-enrichment"
                type: "lookup-enrichment"
                severity: "WARNING"
                condition: "#customerId != null"
                lookup-config:
                  lookup-service: "testService"
                  lookup-key: "#customerId"
                field-mappings:
                  - source-field: "name"
                    target-field: "customerName"
            """;
    }

    private String createEnrichmentYamlWithInvalidSeverity() {
        return """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
              description: "Test configuration for enrichment validation"
              type: "rule-config"

            data-sources:
              - name: "testService"
                type: "inline"

            enrichments:
              - id: "test-enrichment"
                type: "lookup-enrichment"
                severity: "CRITICAL"
                condition: "#customerId != null"
                lookup-config:
                  lookup-service: "testService"
                  lookup-key: "#customerId"
                field-mappings:
                  - source-field: "name"
                    target-field: "customerName"
            """;
    }
}
