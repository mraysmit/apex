package dev.mars.apex.core.config.yaml;

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
}
