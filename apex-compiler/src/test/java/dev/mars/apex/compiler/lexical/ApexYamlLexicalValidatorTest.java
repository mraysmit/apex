package dev.mars.apex.compiler.lexical;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for APEX YAML Lexical Validator.
 * 
 * Tests the formal grammar validation without generating Java code.
 * Each test validates a specific aspect of APEX YAML grammar.
 */
class ApexYamlLexicalValidatorTest {
    
    private final ApexYamlLexicalValidator validator = new ApexYamlLexicalValidator();
    
    @Test
    void shouldValidateValidRuleConfigDocument(@TempDir Path tempDir) throws Exception {
        // Create a valid APEX YAML file
        String validYaml = """
            metadata:
              id: "test-rules"
              name: "Test Rules"
              version: "1.0.0"
              description: "Test rule configuration"
              type: "rule-config"
              author: "test@example.com"
            
            rules:
              - id: "test-rule"
                name: "Test Rule"
                condition: "#data.amount > 1000"
                message: "Amount must be greater than 1000"
                severity: "ERROR"
            """;
        
        Path yamlFile = tempDir.resolve("test-rules.yaml");
        Files.writeString(yamlFile, validYaml);
        
        // Validate
        ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);
        
        // Verify
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        System.out.println("✅ Valid document test passed");
    }
    
    @Test
    void shouldDetectMissingMetadata(@TempDir Path tempDir) throws Exception {
        String invalidYaml = """
            rules:
              - id: "test-rule"
                condition: "#data.amount > 1000"
            """;
        
        Path yamlFile = tempDir.resolve("invalid.yaml");
        Files.writeString(yamlFile, invalidYaml);
        
        ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("Missing required 'metadata' section");
        System.out.println("✅ Missing metadata test passed");
    }
    
    @Test
    void shouldDetectInvalidDocumentType(@TempDir Path tempDir) throws Exception {
        String invalidYaml = """
            metadata:
              id: "test"
              name: "Test"
              version: "1.0.0"
              description: "Test"
              type: "invalid-type"
            """;
        
        Path yamlFile = tempDir.resolve("invalid-type.yaml");
        Files.writeString(yamlFile, invalidYaml);
        
        ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("Invalid document type: invalid-type"));
        System.out.println("✅ Invalid document type test passed");
    }
    
    @Test
    void shouldDetectMissingRequiredFields(@TempDir Path tempDir) throws Exception {
        String invalidYaml = """
            metadata:
              id: "test"
              name: "Test"
              # Missing version, description, type
            """;
        
        Path yamlFile = tempDir.resolve("missing-fields.yaml");
        Files.writeString(yamlFile, invalidYaml);
        
        ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("Missing required metadata field: version"));
        assertThat(result.getErrors()).anyMatch(error -> error.contains("Missing required metadata field: description"));
        assertThat(result.getErrors()).anyMatch(error -> error.contains("Missing required metadata field: type"));
        System.out.println("✅ Missing required fields test passed");
    }
    
    @Test
    void shouldDetectInvalidYamlSyntax(@TempDir Path tempDir) throws Exception {
        String invalidYaml = """
            metadata:
              id: "test"
              name: "Test
              # Missing closing quote - invalid YAML syntax
            """;
        
        Path yamlFile = tempDir.resolve("invalid-syntax.yaml");
        Files.writeString(yamlFile, invalidYaml);
        
        ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("YAML syntax error"));
        System.out.println("✅ Invalid YAML syntax test passed");
    }
    
    @Test
    void shouldValidateVersionFormat(@TempDir Path tempDir) throws Exception {
        String yamlWithBadVersion = """
            metadata:
              id: "test"
              name: "Test"
              version: "not-a-version"
              description: "Test"
              type: "rule-config"
              author: "test@example.com"
            
            rules:
              - id: "test-rule"
                condition: "true"
            """;
        
        Path yamlFile = tempDir.resolve("bad-version.yaml");
        Files.writeString(yamlFile, yamlWithBadVersion);
        
        ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);
        
        // Should be valid but have warnings
        assertThat(result.isValid()).isTrue();
        assertThat(result.hasWarnings()).isTrue();
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("Version should follow semantic versioning"));
        System.out.println("✅ Version format validation test passed");
    }
    
    @Test
    void shouldDetectTypeSpecificRequiredFields(@TempDir Path tempDir) throws Exception {
        String yamlMissingAuthor = """
            metadata:
              id: "test"
              name: "Test"
              version: "1.0.0"
              description: "Test"
              type: "rule-config"
              # Missing required 'author' field for rule-config type
            
            rules:
              - id: "test-rule"
                condition: "true"
            """;
        
        Path yamlFile = tempDir.resolve("missing-author.yaml");
        Files.writeString(yamlFile, yamlMissingAuthor);
        
        ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("Missing required field for type 'rule-config': author"));
        System.out.println("✅ Type-specific required fields test passed");
    }
    
    @Test
    void shouldDetectMissingRequiredSections(@TempDir Path tempDir) throws Exception {
        String yamlMissingSections = """
            metadata:
              id: "test"
              name: "Test"
              version: "1.0.0"
              description: "Test"
              type: "rule-config"
              author: "test@example.com"
            # Missing both 'rules' and 'enrichments' sections
            """;
        
        Path yamlFile = tempDir.resolve("missing-sections.yaml");
        Files.writeString(yamlFile, yamlMissingSections);
        
        ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error ->
            error.contains("Document type 'rule-config' requires at least one of") &&
            error.contains("rules") && error.contains("enrichments"));
        System.out.println("✅ Missing required sections test passed");
    }
}
