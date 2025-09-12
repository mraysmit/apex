package dev.mars.apex.core.config.yaml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify consistency between different loadFromFile methods in YamlConfigurationLoader.
 * 
 * This test ensures that both loadFromFile(String) and loadFromFile(File) methods
 * have identical behavior, particularly around processing external data source references.
 */
@DisplayName("YamlConfigurationLoader Consistency Tests")
public class YamlConfigurationLoaderConsistencyTest {

    @TempDir
    Path tempDir;

    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("Both loadFromFile methods should process external data source references consistently")
    void testLoadFromFileMethodsConsistency() throws Exception {
        // Create a YAML configuration with external data source references
        String yamlContent = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
              type: "rule-config"
            
            # This will test if processDataSourceReferences is called
            data-source-refs:
              - name: "test-external-source"
                source: "test-data-source.yaml"
                enabled: false  # Disabled to avoid actual resolution during test
                description: "Test external data source reference"
            
            rules:
              - id: "test-rule"
                name: "Test Rule"
                condition: "#value > 0"
                message: "Test rule message"
            """;

        // Write to temporary file
        Path configFile = tempDir.resolve("test-config.yaml");
        Files.writeString(configFile, yamlContent);

        // Test both loading methods
        YamlRuleConfiguration configFromString = loader.loadFromFile(configFile.toString());
        YamlRuleConfiguration configFromFile = loader.loadFromFile(configFile.toFile());

        // Verify both configurations are loaded successfully
        assertNotNull(configFromString, "Configuration loaded from String path should not be null");
        assertNotNull(configFromFile, "Configuration loaded from File object should not be null");

        // Verify metadata is consistent
        assertEquals(configFromString.getMetadata().getName(), configFromFile.getMetadata().getName(),
                "Metadata name should be consistent between loading methods");
        assertEquals(configFromString.getMetadata().getVersion(), configFromFile.getMetadata().getVersion(),
                "Metadata version should be consistent between loading methods");

        // Verify rules are consistent
        assertEquals(configFromString.getRules().size(), configFromFile.getRules().size(),
                "Number of rules should be consistent between loading methods");
        
        if (!configFromString.getRules().isEmpty() && !configFromFile.getRules().isEmpty()) {
            assertEquals(configFromString.getRules().get(0).getId(), configFromFile.getRules().get(0).getId(),
                    "Rule ID should be consistent between loading methods");
        }

        // Verify data source references are processed consistently
        // Both should have the same data source refs (even if disabled)
        boolean stringHasDataSourceRefs = configFromString.getDataSourceRefs() != null && !configFromString.getDataSourceRefs().isEmpty();
        boolean fileHasDataSourceRefs = configFromFile.getDataSourceRefs() != null && !configFromFile.getDataSourceRefs().isEmpty();
        
        assertEquals(stringHasDataSourceRefs, fileHasDataSourceRefs,
                "Data source references processing should be consistent between loading methods");

        if (stringHasDataSourceRefs && fileHasDataSourceRefs) {
            assertEquals(configFromString.getDataSourceRefs().size(), configFromFile.getDataSourceRefs().size(),
                    "Number of data source references should be consistent");
            assertEquals(configFromString.getDataSourceRefs().get(0).getName(), 
                        configFromFile.getDataSourceRefs().get(0).getName(),
                    "Data source reference name should be consistent");
        }
    }

    @Test
    @DisplayName("Both loadFromFile methods should handle property resolution consistently")
    void testPropertyResolutionConsistency() throws Exception {
        // Set a system property for testing
        String testPropertyKey = "apex.test.property";
        String testPropertyValue = "test-value-123";
        System.setProperty(testPropertyKey, testPropertyValue);

        try {
            String yamlContent = """
                metadata:
                  name: "Property Test Configuration"
                  version: "${apex.test.property}"
                  type: "rule-config"
                
                rules:
                  - id: "property-test-rule"
                    name: "Property Test Rule"
                    condition: "#value > 0"
                    message: "Property resolved: ${apex.test.property}"
                """;

            // Write to temporary file
            Path configFile = tempDir.resolve("property-test-config.yaml");
            Files.writeString(configFile, yamlContent);

            // Test both loading methods
            YamlRuleConfiguration configFromString = loader.loadFromFile(configFile.toString());
            YamlRuleConfiguration configFromFile = loader.loadFromFile(configFile.toFile());

            // Verify property resolution is consistent
            assertEquals(testPropertyValue, configFromString.getMetadata().getVersion(),
                    "Property should be resolved in String path loading");
            assertEquals(testPropertyValue, configFromFile.getMetadata().getVersion(),
                    "Property should be resolved in File object loading");
            
            assertEquals(configFromString.getMetadata().getVersion(), configFromFile.getMetadata().getVersion(),
                    "Property resolution should be consistent between loading methods");

            // Verify rule message property resolution
            String expectedMessage = "Property resolved: " + testPropertyValue;
            assertEquals(expectedMessage, configFromString.getRules().get(0).getMessage(),
                    "Rule message property should be resolved in String path loading");
            assertEquals(expectedMessage, configFromFile.getRules().get(0).getMessage(),
                    "Rule message property should be resolved in File object loading");

        } finally {
            // Clean up system property
            System.clearProperty(testPropertyKey);
        }
    }

    @Test
    @DisplayName("Both loadFromFile methods should handle validation errors consistently")
    void testValidationErrorConsistency() throws Exception {
        // Create invalid YAML (missing required metadata)
        String invalidYaml = """
            rules:
              - id: "test-rule"
                condition: "#value > 0"
                # Missing required fields like name, message
            """;

        Path invalidConfigFile = tempDir.resolve("invalid-config.yaml");
        Files.writeString(invalidConfigFile, invalidYaml);

        // Both methods should throw the same type of exception
        YamlConfigurationException exceptionFromString = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(invalidConfigFile.toString());
        }, "String path loading should throw YamlConfigurationException for invalid config");

        YamlConfigurationException exceptionFromFile = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(invalidConfigFile.toFile());
        }, "File object loading should throw YamlConfigurationException for invalid config");

        // Verify both exceptions have meaningful messages
        assertNotNull(exceptionFromString.getMessage(), "Exception from String loading should have a message");
        assertNotNull(exceptionFromFile.getMessage(), "Exception from File loading should have a message");
    }
}
