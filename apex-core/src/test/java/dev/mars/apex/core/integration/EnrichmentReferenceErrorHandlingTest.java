package dev.mars.apex.core.integration;

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;


import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Error handling tests for enrichment reference resolution.
 * 
 * Tests various error scenarios including missing files, invalid YAML,
 * disabled references, and other edge cases for the enrichment-refs feature.
 * 
 * Mirrors the pattern from RuleReferenceErrorHandlingTest to ensure
 * consistent error handling between rule-refs and enrichment-refs.
 */
@DisplayName("Enrichment Reference Error Handling Tests")
class EnrichmentReferenceErrorHandlingTest {

    private YamlConfigurationLoader configLoader;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        configLoader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("Should throw exception for missing enrichment reference file")
    void testMissingEnrichmentReferenceFile() throws Exception {
        String configYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
            
            enrichment-refs:
              - name: "missing-enrichments"
                source: "non-existent-enrichments.yaml"
                enabled: true
                description: "This file does not exist"
            
            enrichment-groups:
              - id: "test-group"
                name: "Test Group"
                enrichment-ids:
                  - "some-enrichment"
            """;
        
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, configYaml);
        
        // Should throw YamlConfigurationException for missing file
        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> configLoader.loadFromFile(configFile.toString()),
            "Should throw exception for missing enrichment reference file"
        );
        
        assertTrue(exception.getMessage().contains("missing-enrichments"), 
                  "Exception message should contain reference name");
        assertTrue(exception.getMessage().contains("non-existent-enrichments.yaml"), 
                  "Exception message should contain file path");
    }

    @Test
    @DisplayName("Should throw exception for invalid YAML in referenced enrichment file")
    void testInvalidYamlInReferencedEnrichmentFile() throws Exception {
        // Create invalid YAML file
        String invalidYaml = """
            metadata:
              name: "Invalid YAML"
              version: "1.0.0"
            
            enrichments:
              - id: "test-enrichment"
                name: "Test Enrichment"
                type: field-enrichment
                field-mappings:
                  - source-field: "input
                # Missing closing quote - invalid YAML
                    target-field: "output"
            """;
        
        Path invalidEnrichmentsFile = tempDir.resolve("invalid-enrichments.yaml");
        Files.writeString(invalidEnrichmentsFile, invalidYaml);
        
        String configYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
            
            enrichment-refs:
              - name: "invalid-enrichments"
                source: "%s"
                enabled: true
            
            enrichment-groups:
              - id: "test-group"
                name: "Test Group"
                enrichment-ids:
                  - "test-enrichment"
            """.formatted(invalidEnrichmentsFile.toString().replace("\\", "\\\\"));
        
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, configYaml);
        
        // Should throw YamlConfigurationException for invalid YAML
        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> configLoader.loadFromFile(configFile.toString()),
            "Should throw exception for invalid YAML in referenced enrichment file"
        );
        
        assertTrue(exception.getMessage().contains("invalid-enrichments"), 
                  "Exception message should contain reference name");
    }

    @Test
    @DisplayName("Should handle empty referenced enrichment file gracefully")
    void testEmptyReferencedEnrichmentFile() throws Exception {
        // Create empty enrichment file
        String emptyEnrichmentsYaml = """
            metadata:
              name: "Empty Enrichments"
              version: "1.0.0"
            # No enrichments or enrichment groups defined
            """;
        
        Path emptyEnrichmentsFile = tempDir.resolve("empty-enrichments.yaml");
        Files.writeString(emptyEnrichmentsFile, emptyEnrichmentsYaml);
        
        String configYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
            
            enrichment-refs:
              - name: "empty-enrichments"
                source: "%s"
                enabled: true
            
            enrichments:
              - id: "local-enrichment"
                name: "Local Enrichment"
                type: field-enrichment
                field-mappings:
                  - source-field: "input"
                    target-field: "output"
            
            enrichment-groups:
              - id: "test-group"
                name: "Test Group"
                enrichment-ids:
                  - "local-enrichment"
            """.formatted(emptyEnrichmentsFile.toString().replace("\\", "\\\\"));
        
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, configYaml);
        
        // Should not throw exception and should only have local enrichment
        assertDoesNotThrow(() -> {
            var config = configLoader.loadFromFile(configFile.toString());
            assertNotNull(config, "Configuration should be loaded successfully");
            assertEquals(1, config.getEnrichments().size(), "Should have only the local enrichment");
            assertEquals("local-enrichment", config.getEnrichments().get(0).getId(), 
                        "Should have the correct local enrichment");
        }, "Should handle empty enrichment reference gracefully");
    }

    @Test
    @DisplayName("Should skip disabled enrichment reference")
    void testDisabledEnrichmentReference() throws Exception {
        // Create enrichment file that should not be loaded
        String enrichmentsYaml = """
            metadata:
              name: "Disabled Enrichments"
              version: "1.0.0"
            
            enrichments:
              - id: "disabled-enrichment"
                name: "Disabled Enrichment"
                type: field-enrichment
                field-mappings:
                  - source-field: "input"
                    target-field: "output"
            """;
        
        Path enrichmentsFile = tempDir.resolve("disabled-enrichments.yaml");
        Files.writeString(enrichmentsFile, enrichmentsYaml);
        
        String configYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
            
            enrichment-refs:
              - name: "disabled-enrichments"
                source: "%s"
                enabled: false
                description: "This reference is disabled"
            
            enrichments:
              - id: "local-enrichment"
                name: "Local Enrichment"
                type: field-enrichment
                field-mappings:
                  - source-field: "input"
                    target-field: "output"
            """.formatted(enrichmentsFile.toString().replace("\\", "\\\\"));
        
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, configYaml);
        
        // Should not throw exception and should only have local enrichment
        assertDoesNotThrow(() -> {
            var config = configLoader.loadFromFile(configFile.toString());
            assertNotNull(config, "Configuration should be loaded successfully");
            assertEquals(1, config.getEnrichments().size(), "Should have only the local enrichment");
            assertEquals("local-enrichment", config.getEnrichments().get(0).getId(),
                        "Should not load disabled enrichment reference");
        }, "Should skip disabled enrichment reference");
    }

    @Test
    @DisplayName("Should handle multiple disabled enrichment references")
    void testMultipleDisabledEnrichmentReferences() throws Exception {
        // Create enrichment files that won't be loaded due to disabled references
        String enrichments1Yaml = """
            metadata:
              name: "Disabled Enrichments 1"
              version: "1.0.0"

            enrichments:
              - id: "disabled-enrichment-1"
                name: "Disabled Enrichment 1"
                type: field-enrichment
                field-mappings:
                  - source-field: "input1"
                    target-field: "output1"
            """;

        String enrichments2Yaml = """
            metadata:
              name: "Disabled Enrichments 2"
              version: "1.0.0"

            enrichments:
              - id: "disabled-enrichment-2"
                name: "Disabled Enrichment 2"
                type: field-enrichment
                field-mappings:
                  - source-field: "input2"
                    target-field: "output2"
            """;

        Path enrichments1File = tempDir.resolve("disabled-enrichments-1.yaml");
        Path enrichments2File = tempDir.resolve("disabled-enrichments-2.yaml");
        Files.writeString(enrichments1File, enrichments1Yaml);
        Files.writeString(enrichments2File, enrichments2Yaml);

        String configYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"

            enrichment-refs:
              - name: "disabled-enrichments-1"
                source: "%s"
                enabled: false
                description: "First disabled reference"
              - name: "disabled-enrichments-2"
                source: "%s"
                enabled: false
                description: "Second disabled reference"

            enrichments:
              - id: "local-enrichment"
                name: "Local Enrichment"
                type: field-enrichment
                field-mappings:
                  - source-field: "input"
                    target-field: "output"
            """.formatted(
                enrichments1File.toString().replace("\\", "\\\\"),
                enrichments2File.toString().replace("\\", "\\\\")
            );

        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, configYaml);

        // Should not throw exception and should only have local enrichment
        assertDoesNotThrow(() -> {
            var config = configLoader.loadFromFile(configFile.toString());
            assertNotNull(config, "Configuration should be loaded successfully");
            assertEquals(1, config.getEnrichments().size(), "Should have only the local enrichment");
            assertEquals("local-enrichment", config.getEnrichments().get(0).getId(),
                        "Should have the correct local enrichment");
        }, "Should handle multiple disabled enrichment references");
    }

    @Test
    @DisplayName("Should load from multiple enabled enrichment references")
    void testMultipleEnabledEnrichmentReferences() throws Exception {
        // Create first enrichment file
        String enrichments1Yaml = """
            metadata:
              name: "Enrichments Set 1"
              version: "1.0.0"

            enrichments:
              - id: "enrichment-1"
                name: "Enrichment 1"
                type: field-enrichment
                field-mappings:
                  - source-field: "input1"
                    target-field: "output1"
            """;

        // Create second enrichment file
        String enrichments2Yaml = """
            metadata:
              name: "Enrichments Set 2"
              version: "1.0.0"

            enrichments:
              - id: "enrichment-2"
                name: "Enrichment 2"
                type: field-enrichment
                field-mappings:
                  - source-field: "input2"
                    target-field: "output2"
            """;

        Path enrichments1File = tempDir.resolve("enrichments-1.yaml");
        Path enrichments2File = tempDir.resolve("enrichments-2.yaml");
        Files.writeString(enrichments1File, enrichments1Yaml);
        Files.writeString(enrichments2File, enrichments2Yaml);

        String configYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"

            enrichment-refs:
              - name: "enrichments-set-1"
                source: "%s"
                enabled: true
                description: "First enrichment set"
              - name: "enrichments-set-2"
                source: "%s"
                enabled: true
                description: "Second enrichment set"

            enrichments:
              - id: "local-enrichment"
                name: "Local Enrichment"
                type: field-enrichment
                field-mappings:
                  - source-field: "input"
                    target-field: "output"
            """.formatted(
                enrichments1File.toString().replace("\\", "\\\\"),
                enrichments2File.toString().replace("\\", "\\\\")
            );

        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, configYaml);

        // Should load all enrichments from both files plus local
        assertDoesNotThrow(() -> {
            var config = configLoader.loadFromFile(configFile.toString());
            assertNotNull(config, "Configuration should be loaded successfully");
            assertEquals(3, config.getEnrichments().size(),
                        "Should have 3 enrichments (2 from refs + 1 local)");

            // Verify all enrichments are present
            assertTrue(config.getEnrichments().stream().anyMatch(e -> "enrichment-1".equals(e.getId())),
                      "Should have enrichment-1 from first file");
            assertTrue(config.getEnrichments().stream().anyMatch(e -> "enrichment-2".equals(e.getId())),
                      "Should have enrichment-2 from second file");
            assertTrue(config.getEnrichments().stream().anyMatch(e -> "local-enrichment".equals(e.getId())),
                      "Should have local-enrichment");
        }, "Should load from multiple enabled enrichment references");
    }

    @Test
    @DisplayName("Should load both enrichments and enrichment groups from external file")
    void testLoadBothEnrichmentsAndGroupsFromExternalFile() throws Exception {
        // Create enrichment file with both enrichments and enrichment groups
        String enrichmentsYaml = """
            metadata:
              name: "Complete Enrichments"
              version: "1.0.0"

            enrichments:
              - id: "ext-enrichment-1"
                name: "External Enrichment 1"
                type: field-enrichment
                field-mappings:
                  - source-field: "input1"
                    target-field: "output1"

              - id: "ext-enrichment-2"
                name: "External Enrichment 2"
                type: field-enrichment
                field-mappings:
                  - source-field: "input2"
                    target-field: "output2"

            enrichment-groups:
              - id: "ext-group"
                name: "External Group"
                operator: AND
                enrichment-ids:
                  - ext-enrichment-1
                  - ext-enrichment-2
            """;

        Path enrichmentsFile = tempDir.resolve("complete-enrichments.yaml");
        Files.writeString(enrichmentsFile, enrichmentsYaml);

        String configYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"

            enrichment-refs:
              - name: "complete-enrichments"
                source: "%s"
                enabled: true
            """.formatted(enrichmentsFile.toString().replace("\\", "\\\\"));

        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, configYaml);

        // Should load both enrichments and enrichment groups
        assertDoesNotThrow(() -> {
            var config = configLoader.loadFromFile(configFile.toString());
            assertNotNull(config, "Configuration should be loaded successfully");

            // Verify enrichments were loaded
            assertNotNull(config.getEnrichments(), "Enrichments should not be null");
            assertEquals(2, config.getEnrichments().size(), "Should have 2 enrichments");
            assertTrue(config.getEnrichments().stream().anyMatch(e -> "ext-enrichment-1".equals(e.getId())),
                      "Should have ext-enrichment-1");
            assertTrue(config.getEnrichments().stream().anyMatch(e -> "ext-enrichment-2".equals(e.getId())),
                      "Should have ext-enrichment-2");

            // Verify enrichment groups were loaded
            assertNotNull(config.getEnrichmentGroups(), "Enrichment groups should not be null");
            assertEquals(1, config.getEnrichmentGroups().size(), "Should have 1 enrichment group");
            assertEquals("ext-group", config.getEnrichmentGroups().get(0).getId(),
                        "Should have ext-group");
        }, "Should load both enrichments and enrichment groups from external file");
    }

    @Test
    @DisplayName("Should throw exception when enrichment-group-reference points to non-existent group")
    void testNonExistentEnrichmentGroupReference() throws Exception {
        String configYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"

            enrichments:
              - id: "test-enrichment"
                name: "Test Enrichment"
                type: field-enrichment
                field-mappings:
                  - source-field: "input"
                    target-field: "output"

            enrichment-groups:
              - id: "test-group"
                name: "Test Group"
                operator: AND
                enrichment-group-references:
                  - non-existent-group
            """;

        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, configYaml);

        // Should throw YamlConfigurationException for non-existent enrichment group reference
        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> configLoader.loadFromFile(configFile.toString()),
            "Should throw exception for enrichment group referencing non-existent group"
        );

        assertTrue(exception.getMessage().contains("non-existent-group"),
                  "Exception message should contain the non-existent group ID");
    }

    @Test
    @DisplayName("Should handle mix of enabled and disabled enrichment references")
    void testMixOfEnabledAndDisabledReferences() throws Exception {
        // Create two enrichment files
        String enabledEnrichmentsYaml = """
            metadata:
              name: "Enabled Enrichments"
              version: "1.0.0"

            enrichments:
              - id: "enabled-enrichment"
                name: "Enabled Enrichment"
                type: field-enrichment
                field-mappings:
                  - source-field: "input1"
                    target-field: "output1"
            """;

        String disabledEnrichmentsYaml = """
            metadata:
              name: "Disabled Enrichments"
              version: "1.0.0"

            enrichments:
              - id: "disabled-enrichment"
                name: "Disabled Enrichment"
                type: field-enrichment
                field-mappings:
                  - source-field: "input2"
                    target-field: "output2"
            """;

        Path enabledFile = tempDir.resolve("enabled-enrichments.yaml");
        Path disabledFile = tempDir.resolve("disabled-enrichments.yaml");
        Files.writeString(enabledFile, enabledEnrichmentsYaml);
        Files.writeString(disabledFile, disabledEnrichmentsYaml);

        String configYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"

            enrichment-refs:
              - name: "enabled-ref"
                source: "%s"
                enabled: true
              - name: "disabled-ref"
                source: "%s"
                enabled: false
            """.formatted(
                enabledFile.toString().replace("\\", "\\\\"),
                disabledFile.toString().replace("\\", "\\\\")
            );

        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, configYaml);

        // Should only load from enabled reference
        assertDoesNotThrow(() -> {
            var config = configLoader.loadFromFile(configFile.toString());
            assertNotNull(config, "Configuration should be loaded successfully");
            assertEquals(1, config.getEnrichments().size(), "Should have only 1 enrichment");
            assertEquals("enabled-enrichment", config.getEnrichments().get(0).getId(),
                        "Should only load from enabled reference");
        }, "Should handle mix of enabled and disabled references");
    }
}

