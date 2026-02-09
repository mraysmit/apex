package dev.mars.apex.core.config;
import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.config.loader.*;
import dev.mars.apex.core.config.exception.*;
import dev.mars.apex.core.config.service.*;

import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Implicit Constant validation logic.
 * Verifies that source-field can be omitted if expression/transformation is present.
 */
@DisplayName("Implicit Constant Validation Tests")
class ImplicitConstantValidationTest {

    private YamlConfigurationLoader configurationLoader;

    @BeforeEach
    void setUp() {
        configurationLoader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("Should validate explicit constant (legacy pattern) successfully")
    void shouldValidateExplicitConstant() {
        String yaml = """
            metadata:
              name: "Test Config"
              version: "1.0"
              type: "rule-config"
            enrichments:
              - id: "explicit-constant"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "constant"
                    target-field: "status"
                    expression: "'ACTIVE'"
            """;
        
        assertDoesNotThrow(() -> {
            InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(is);
        });
    }

    @Test
    @DisplayName("Should validate implicit constant with expression")
    void shouldValidateImplicitConstantWithExpression() {
        String yaml = """
            metadata:
              name: "Test Config"
              version: "1.0"
              type: "rule-config"
            enrichments:
              - id: "implicit-constant-expr"
                type: "field-enrichment"
                field-mappings:
                  - target-field: "status"
                    expression: "'ACTIVE'"
            """;
        
        // CURRENTLY: This should throw because source-field is missing.
        // FUTURE: This should pass.
        // For TDD, we assert that it currently throws, then we will change the test to assertDoesNotThrow after implementation?
        // Or better, I write the test as I WANT it to behave, see it fail, then fix it.
        // The user said "make sure we have ... coverage before we implemented".
        // So I will write it expecting success, and confirm it fails.
        
        assertDoesNotThrow(() -> {
            InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(is);
        });
    }

    @Test
    @DisplayName("Should validate implicit constant with transformation")
    void shouldValidateImplicitConstantWithTransformation() {
        String yaml = """
            metadata:
              name: "Test Config"
              version: "1.0"
              type: "rule-config"
            enrichments:
              - id: "implicit-constant-transform"
                type: "field-enrichment"
                field-mappings:
                  - target-field: "status"
                    transformation: "'ACTIVE'"
            """;
        
        assertDoesNotThrow(() -> {
            InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(is);
        });
    }

    @Test
    @DisplayName("Should fail validation when source-field, expression, and transformation are ALL missing")
    void shouldFailWhenAllSourceMechanismsMissing() {
        String yaml = """
            metadata:
              name: "Test Config"
              version: "1.0"
              type: "rule-config"
            enrichments:
              - id: "missing-all"
                type: "field-enrichment"
                field-mappings:
                  - target-field: "status"
            """;
        
        YamlConfigurationException ex = assertThrows(YamlConfigurationException.class, () -> {
            InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(is);
        });
        
        assertTrue(ex.getMessage().contains("Must provide at least one of: source-field, expression (or transformation)"));
    }

    @Test
    @DisplayName("Should fail validation when expression is empty string")
    void shouldFailWhenExpressionIsEmpty() {
        String yaml = """
            metadata:
              name: "Test Config"
              version: "1.0"
              type: "rule-config"
            enrichments:
              - id: "empty-expression"
                type: "field-enrichment"
                field-mappings:
                  - target-field: "status"
                    expression: ""
            """;
        
        YamlConfigurationException ex = assertThrows(YamlConfigurationException.class, () -> {
            InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(is);
        });
        
        assertTrue(ex.getMessage().contains("Must provide at least one of: source-field, expression (or transformation)"));
    }


    @Test
    @DisplayName("Should fail validation when only default-value is present")
    void shouldFailWhenOnlyDefaultValuePresent() {
        String yaml = """
            metadata:
              name: "Test Config"
              version: "1.0"
              type: "rule-config"
            enrichments:
              - id: "default-only"
                type: "field-enrichment"
                field-mappings:
                  - target-field: "status"
                    default-value: "PENDING"
            """;
        
        YamlConfigurationException ex = assertThrows(YamlConfigurationException.class, () -> {
            InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(is);
        });
        
        assertTrue(ex.getMessage().contains("Must provide at least one of: source-field, expression (or transformation)"));
    }

    @Test
    @DisplayName("Should validate implicit constant with required flag")
    void shouldValidateImplicitConstantWithRequiredFlag() {
        String yaml = """
            metadata:
              name: "Test Config"
              version: "1.0"
              type: "rule-config"
            enrichments:
              - id: "implicit-required"
                type: "field-enrichment"
                field-mappings:
                  - target-field: "status"
                    expression: "'ACTIVE'"
                    required: true
            """;
        
        assertDoesNotThrow(() -> {
            InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
            configurationLoader.loadFromStream(is);
        });
    }
}
