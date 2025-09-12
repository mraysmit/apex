package dev.mars.apex.core.integration;

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Error handling tests for rule reference resolution.
 * 
 * Tests various error scenarios including missing files, invalid YAML,
 * circular references, and other edge cases.
 */
@DisplayName("Rule Reference Error Handling Tests")
class RuleReferenceErrorHandlingTest {

    private YamlConfigurationLoader configLoader;
    private YamlRulesEngineService rulesEngineService;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        configLoader = new YamlConfigurationLoader();
        rulesEngineService = new YamlRulesEngineService();
    }

    @Test
    @DisplayName("Should throw exception for missing rule reference file")
    void testMissingRuleReferenceFile() throws Exception {
        String configYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
            
            rule-refs:
              - name: "missing-rules"
                source: "non-existent-file.yaml"
                enabled: true
                description: "This file does not exist"
            
            rule-groups:
              - id: "test-group"
                name: "Test Group"
                rule-ids:
                  - "some-rule"
            """;
        
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, configYaml);
        
        // Should throw YamlConfigurationException for missing file
        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> configLoader.loadFromFile(configFile.toString()),
            "Should throw exception for missing rule reference file"
        );
        
        assertTrue(exception.getMessage().contains("missing-rules"), 
                  "Exception message should contain reference name");
        assertTrue(exception.getMessage().contains("non-existent-file.yaml"), 
                  "Exception message should contain file path");
    }

    @Test
    @DisplayName("Should throw exception for invalid YAML in referenced file")
    void testInvalidYamlInReferencedFile() throws Exception {
        // Create invalid YAML file
        String invalidYaml = """
            metadata:
              name: "Invalid YAML"
              version: "1.0.0"
            
            rules:
              - id: "test-rule"
                name: "Test Rule"
                condition: "#value > 0
                # Missing closing quote - invalid YAML
                message: "Invalid YAML syntax"
            """;
        
        Path invalidRulesFile = tempDir.resolve("invalid-rules.yaml");
        Files.writeString(invalidRulesFile, invalidYaml);
        
        String configYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
            
            rule-refs:
              - name: "invalid-rules"
                source: "%s"
                enabled: true
            
            rule-groups:
              - id: "test-group"
                name: "Test Group"
                rule-ids:
                  - "test-rule"
            """.formatted(invalidRulesFile.toString().replace("\\", "\\\\"));
        
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, configYaml);
        
        // Should throw YamlConfigurationException for invalid YAML
        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> configLoader.loadFromFile(configFile.toString()),
            "Should throw exception for invalid YAML in referenced file"
        );
        
        assertTrue(exception.getMessage().contains("invalid-rules"), 
                  "Exception message should contain reference name");
    }

    @Test
    @DisplayName("Should handle empty referenced rule file gracefully")
    void testEmptyReferencedRuleFile() throws Exception {
        // Create empty rule file
        String emptyRulesYaml = """
            metadata:
              name: "Empty Rules"
              version: "1.0.0"
            # No rules defined
            """;
        
        Path emptyRulesFile = tempDir.resolve("empty-rules.yaml");
        Files.writeString(emptyRulesFile, emptyRulesYaml);
        
        String configYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
            
            rule-refs:
              - name: "empty-rules"
                source: "%s"
                enabled: true
            
            rules:
              - id: "local-rule"
                name: "Local Rule"
                condition: "#value > 0"
                message: "Local rule message"
            
            rule-groups:
              - id: "test-group"
                name: "Test Group"
                rule-ids:
                  - "local-rule"
            """.formatted(emptyRulesFile.toString().replace("\\", "\\\\"));
        
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, configYaml);
        
        // Should not throw exception for empty rule file
        assertDoesNotThrow(() -> {
            var config = configLoader.loadFromFile(configFile.toString());
            assertNotNull(config, "Configuration should be loaded successfully");
            assertEquals(1, config.getRules().size(), "Should have only the local rule");
        }, "Should handle empty referenced rule file gracefully");
    }

    @Test
    @DisplayName("Should handle rule reference with no rules section")
    void testRuleReferenceWithNoRulesSection() throws Exception {
        // Create rule file with no rules section
        String noRulesYaml = """
            metadata:
              name: "No Rules File"
              version: "1.0.0"
              description: "This file has no rules section"
            
            # Some other configuration but no rules
            categories:
              - id: "test-category"
                name: "Test Category"
            """;
        
        Path noRulesFile = tempDir.resolve("no-rules.yaml");
        Files.writeString(noRulesFile, noRulesYaml);
        
        String configYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
            
            rule-refs:
              - name: "no-rules"
                source: "%s"
                enabled: true
            
            rules:
              - id: "local-rule"
                name: "Local Rule"
                condition: "#value > 0"
                message: "Local rule message"
            
            rule-groups:
              - id: "test-group"
                name: "Test Group"
                rule-ids:
                  - "local-rule"
            """.formatted(noRulesFile.toString().replace("\\", "\\\\"));
        
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, configYaml);
        
        // Should not throw exception for file with no rules section
        assertDoesNotThrow(() -> {
            var config = configLoader.loadFromFile(configFile.toString());
            assertNotNull(config, "Configuration should be loaded successfully");
            assertEquals(1, config.getRules().size(), "Should have only the local rule");
        }, "Should handle rule reference with no rules section gracefully");
    }

    @Test
    @DisplayName("Should handle multiple disabled rule references")
    void testMultipleDisabledRuleReferences() throws Exception {
        // Create rule files that won't be loaded due to disabled references
        String rules1Yaml = """
            metadata:
              name: "Disabled Rules 1"
              version: "1.0.0"
            
            rules:
              - id: "disabled-rule-1"
                name: "Disabled Rule 1"
                condition: "#value1 > 0"
                message: "This should not be loaded"
            """;
        
        String rules2Yaml = """
            metadata:
              name: "Disabled Rules 2"
              version: "1.0.0"
            
            rules:
              - id: "disabled-rule-2"
                name: "Disabled Rule 2"
                condition: "#value2 > 0"
                message: "This should also not be loaded"
            """;
        
        Path rules1File = tempDir.resolve("disabled-rules-1.yaml");
        Path rules2File = tempDir.resolve("disabled-rules-2.yaml");
        Files.writeString(rules1File, rules1Yaml);
        Files.writeString(rules2File, rules2Yaml);
        
        String configYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
            
            rule-refs:
              - name: "disabled-rules-1"
                source: "%s"
                enabled: false
                description: "First disabled reference"
              - name: "disabled-rules-2"
                source: "%s"
                enabled: false
                description: "Second disabled reference"
            
            rules:
              - id: "local-rule"
                name: "Local Rule"
                condition: "#value > 0"
                message: "Local rule message"
            
            rule-groups:
              - id: "test-group"
                name: "Test Group"
                rule-ids:
                  - "local-rule"
            """.formatted(
                rules1File.toString().replace("\\", "\\\\"),
                rules2File.toString().replace("\\", "\\\\")
            );
        
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, configYaml);
        
        // Should not throw exception and should only have local rule
        assertDoesNotThrow(() -> {
            var config = configLoader.loadFromFile(configFile.toString());
            assertNotNull(config, "Configuration should be loaded successfully");
            assertEquals(1, config.getRules().size(), "Should have only the local rule");
            assertEquals("local-rule", config.getRules().get(0).getId(), "Should have the correct local rule");
        }, "Should handle multiple disabled rule references");
    }

    @Test
    @DisplayName("Should throw exception for rule group referencing non-existent rule from reference")
    void testRuleGroupReferencingNonExistentRuleFromReference() throws Exception {
        // Create rule file with specific rules
        String rulesYaml = """
            metadata:
              name: "Available Rules"
              version: "1.0.0"
            
            rules:
              - id: "existing-rule"
                name: "Existing Rule"
                condition: "#value > 0"
                message: "This rule exists"
            """;
        
        Path rulesFile = tempDir.resolve("available-rules.yaml");
        Files.writeString(rulesFile, rulesYaml);
        
        String configYaml = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
            
            rule-refs:
              - name: "available-rules"
                source: "%s"
                enabled: true
            
            rule-groups:
              - id: "test-group"
                name: "Test Group"
                rule-ids:
                  - "existing-rule"
                  - "non-existent-rule"  # This rule doesn't exist
            """.formatted(rulesFile.toString().replace("\\", "\\\\"));
        
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, configYaml);
        
        // Should throw YamlConfigurationException for non-existent rule reference
        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> configLoader.loadFromFile(configFile.toString()),
            "Should throw exception for rule group referencing non-existent rule"
        );
        
        assertTrue(exception.getMessage().contains("non-existent-rule"), 
                  "Exception message should contain the non-existent rule ID");
    }
}
