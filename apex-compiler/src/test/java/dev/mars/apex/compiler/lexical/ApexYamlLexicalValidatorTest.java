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

    @Test
    void shouldValidateValidPipelineConfigDocument(@TempDir Path tempDir) throws Exception {
        // Create a valid pipeline-config YAML file
        String validPipelineYaml = """
            metadata:
              id: "test-pipeline"
              name: "Test Pipeline"
              version: "1.0.0"
              description: "Test pipeline configuration"
              type: "pipeline-config"
              author: "test@example.com"

            pipeline:
              name: "test-pipeline"
              steps:
                - name: "extract-data"
                  type: "extract"
                  source: "test-source"

            data-sources:
              - name: "test-source"
                type: "file-system"

            data-sinks:
              - name: "test-sink"
                type: "database"
            """;

        Path yamlFile = tempDir.resolve("test-pipeline.yaml");
        Files.writeString(yamlFile, validPipelineYaml);

        // Validate
        ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);

        // Verify
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        System.out.println("✅ Valid pipeline-config document test passed");
    }

    @Test
    void shouldDetectMissingAuthorForPipelineConfig(@TempDir Path tempDir) throws Exception {
        String yamlMissingAuthor = """
            metadata:
              id: "test-pipeline"
              name: "Test Pipeline"
              version: "1.0.0"
              description: "Test pipeline configuration"
              type: "pipeline-config"
              # Missing required 'author' field for pipeline-config type

            pipeline:
              name: "test-pipeline"
              steps: []
            """;

        Path yamlFile = tempDir.resolve("missing-author-pipeline.yaml");
        Files.writeString(yamlFile, yamlMissingAuthor);

        ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error ->
            error.contains("Missing required field for type 'pipeline-config': author"));
        System.out.println("✅ Missing author for pipeline-config test passed");
    }

    @Test
    void shouldValidateCompletePipelineConfigDocument(@TempDir Path tempDir) throws Exception {
        // Test a complete pipeline configuration similar to the orchestration guide examples
        String completePipelineYaml = """
            metadata:
              id: "csv-to-h2-pipeline"
              name: "CSV to H2 ETL Pipeline Demo"
              version: "1.0.0"
              description: "Demonstration of CSV data processing with H2 database output"
              type: "pipeline-config"
              author: "APEX Demo Team"

            pipeline:
              name: "customer-etl-pipeline"
              description: "Extract customer data from CSV, transform, and load into H2 database"
              steps:
                - name: "extract-customers"
                  type: "extract"
                  source: "customer-csv-input"
                  operation: "getAllCustomers"

                - name: "load-to-database"
                  type: "load"
                  sink: "customer-h2-database"
                  operation: "insertCustomer"
                  depends-on: ["extract-customers"]

            data-sources:
              - name: "customer-csv-input"
                type: "file-system"
                connection:
                  basePath: "./data"
                  filePattern: "customers.csv"

            data-sinks:
              - name: "customer-h2-database"
                type: "database"
                sourceType: "h2"
                connection:
                  database: "./output/customer_database"
                  username: "sa"
                  password: ""
            """;

        Path yamlFile = tempDir.resolve("complete-pipeline.yaml");
        Files.writeString(yamlFile, completePipelineYaml);

        // Validate
        ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);

        // Verify
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        System.out.println("✅ Complete pipeline-config document validation test passed");
        System.out.println("   Pipeline configuration matches orchestration guide format");
    }

    @Test
    void shouldValidateFixedOrchestrationGuideExample(@TempDir Path tempDir) throws Exception {
        // Test the exact fixed example from the orchestration guide
        String fixedGuideExample = """
            metadata:
              id: "my-first-pipeline"
              name: "My First Pipeline"
              version: "1.0.0"
              description: "Simple CSV to database pipeline"
              type: "pipeline-config"
              author: "APEX Demo Team"

            pipeline:
              name: "csv-to-db-pipeline"
              description: "Read CSV data and write to database"

              steps:
                - name: "extract-data"
                  type: "extract"
                  source: "csv-input"
                  operation: "getAllRecords"

                - name: "load-data"
                  type: "load"
                  sink: "database-output"
                  operation: "insertRecord"
                  depends-on: ["extract-data"]

            data-sources:
              - name: "csv-input"
                type: "file-system"
                connection:
                  basePath: "./data"
                  filePattern: "input.csv"
                fileFormat:
                  type: "csv"
                  hasHeaderRow: true
                queries:
                  getAllRecords: "SELECT * FROM csv"

            data-sinks:
              - name: "database-output"
                type: "database"
                sourceType: "h2"
                connection:
                  database: "./output/data"
                  username: "sa"
                  password: ""
                operations:
                  insertRecord: |
                    INSERT INTO records (id, name, value)
                    VALUES (:id, :name, :value)
            """;

        Path yamlFile = tempDir.resolve("fixed-guide-example.yaml");
        Files.writeString(yamlFile, fixedGuideExample);

        // Validate
        ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);

        // Verify
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        System.out.println("✅ Fixed orchestration guide example validation test passed");
        System.out.println("   All required metadata fields present and valid");
        System.out.println("   Document type 'pipeline-config' is now supported");
    }
}
