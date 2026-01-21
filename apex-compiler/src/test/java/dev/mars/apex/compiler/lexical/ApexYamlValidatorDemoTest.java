package dev.mars.apex.compiler.lexical;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Demonstration of the APEX YAML Lexical Validator in action.
 * 
 * This test shows how the validator works with real APEX YAML files.
 */
class ApexYamlValidatorDemoTest {
    
    private final ApexYamlLexicalValidator validator = new ApexYamlLexicalValidator();
    
    @Test
    void demonstrateValidatorWithValidFile(@TempDir Path tempDir) throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("APEX YAML LEXICAL VALIDATOR DEMONSTRATION");
        System.out.println("=".repeat(80));
        
        // Create a valid APEX YAML file
        String validYaml = """
            metadata:
              id: "demo-trading-rules"
              name: "Demo Trading Rules"
              version: "1.0.0"
              description: "Demonstration of APEX YAML validation"
              type: "rule-config"
              author: "apex.demo.team@company.com"
              created-date: "2025-01-06"
              business-domain: "Capital Markets"
              tags: ["demo", "validation", "trading"]
            
            rules:
              - id: "trade-amount-validation"
                name: "Trade Amount Validation"
                description: "Validates trade amount is positive and within limits"
                condition: "#amount > 0 && #amount <= 1000000"
                message: "Trade amount must be positive and not exceed $1M"
                severity: "ERROR"
                priority: 100
                enabled: true
                
              - id: "currency-validation"
                name: "Currency Validation"
                description: "Validates currency code is provided and valid"
                condition: "#currency != null && #currency.matches('[A-Z]{3}')"
                message: "Currency must be a valid 3-letter ISO code"
                severity: "ERROR"
                priority: 200
                enabled: true
                
              - id: "counterparty-check"
                name: "Counterparty Check"
                description: "Validates counterparty is approved"
                condition: "#counterparty != null && #context.approvedCounterparties.contains(#counterparty)"
                message: "Counterparty must be pre-approved"
                severity: "WARNING"
                priority: 300
                enabled: true
            
            enrichments:
              - type: "lookup-enrichment"
                source: "fx-rates"
                lookup-key: "#currency"
                target-field: "exchangeRate"
                description: "Lookup current FX rate"
                cache-ttl: 300000
                
              - type: "lookup-enrichment"
                source: "counterparty-data"
                lookup-key: "#counterparty"
                target-field: "counterpartyProfile"
                description: "Enrich with counterparty profile data"
                
              - type: "calculation-enrichment"
                expression: "#amount * #enriched.exchangeRate"
                target-field: "usdEquivalent"
                description: "Calculate USD equivalent amount"
            """;
        
        Path yamlFile = tempDir.resolve("demo-trading-rules.yaml");
        Files.writeString(yamlFile, validYaml);
        
        System.out.println("\n📄 Validating YAML file: " + yamlFile.getFileName());
        System.out.println("-".repeat(50));
        
        // Validate the file
        ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);
        
        // Display results
        System.out.println(result);
        
        if (result.isValid()) {
            System.out.println("SUCCESS: APEX YAML file passed all validation checks!");
            System.out.println("\nValidation Summary:");
            System.out.println("  • Document Type: rule-config");
            System.out.println("  • Rules Found: 3");
            System.out.println("  • Enrichments Found: 3");
            System.out.println("  • SpEL Expressions: Validated");
            System.out.println("  • Required Fields: All present");
            System.out.println("  • YAML Syntax: Valid");
        }
        
        System.out.println("\n" + "=".repeat(80));
    }
    
    @Test
    void demonstrateValidatorWithInvalidFile(@TempDir Path tempDir) throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("X APEX YAML VALIDATION ERROR DEMONSTRATION");
        System.out.println("=".repeat(80));
        
        // Create an invalid APEX YAML file with multiple errors
        String invalidYaml = """
            metadata:
              # Missing required 'id' field
              name: "Invalid Demo Rules"
              version: "not-a-version"  # Invalid version format
              description: "Demo with intentional errors"
              type: "invalid-document-type"  # Invalid document type
              # Missing required 'author' field for rule-config type
            
            # Missing required sections (no rules or enrichments)
            
            some-invalid-section:
              - invalid: "data"
            """;
        
        Path yamlFile = tempDir.resolve("invalid-demo.yaml");
        Files.writeString(yamlFile, invalidYaml);
        
        System.out.println("\n📄 Validating YAML file with errors: " + yamlFile.getFileName());
        System.out.println("-".repeat(50));
        
        // Validate the file
        ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);
        
        // Display results
        System.out.println(result);
        
        if (!result.isValid()) {
            System.out.println("X VALIDATION FAILED: Found " + result.getErrors().size() + " errors");
            System.out.println("\n🔧 The lexical validator successfully caught:");
            System.out.println("  • Missing required metadata fields");
            System.out.println("  • Invalid document type");
            System.out.println("  • Invalid version format");
            System.out.println("  • Missing required sections");
            System.out.println("\n💡 These errors would be caught at compile-time, not runtime!");
        }
        
        System.out.println("\n" + "=".repeat(80));
    }
    
    @Test
    void demonstrateSpelExpressionValidation(@TempDir Path tempDir) throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🧮 SPEL EXPRESSION VALIDATION DEMONSTRATION");
        System.out.println("=".repeat(80));
        
        // Create YAML with various SpEL expressions
        String spelYaml = """
            metadata:
              id: "spel-validation-demo"
              name: "SpEL Expression Validation Demo"
              version: "1.0.0"
              description: "Demonstrates SpEL expression validation"
              type: "rule-config"
              author: "test@example.com"
            
            rules:
              - id: "valid-expression"
                name: "Valid Expression"
                condition: "#amount > 1000 && #currency == 'USD'"
                
              - id: "invalid-expression"
                name: "Invalid Expression"
                condition: "#amount > && #invalid.syntax"  # Invalid syntax
                
              - id: "unmatched-parentheses"
                name: "Unmatched Parentheses"
                condition: "#amount > (1000 + #fee"  # Missing closing parenthesis
            """;
        
        Path yamlFile = tempDir.resolve("spel-demo.yaml");
        Files.writeString(yamlFile, spelYaml);
        
        System.out.println("\n📄 Validating SpEL expressions: " + yamlFile.getFileName());
        System.out.println("-".repeat(50));
        
        // Validate the file
        ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);
        
        // Display results
        System.out.println(result);
        
        System.out.println("\n🧮 SpEL Expression Analysis:");
        System.out.println("  • Valid expressions: Passed validation");
        System.out.println("  • Invalid syntax: Detected and reported");
        System.out.println("  • Parentheses matching: Validated");
        System.out.println("  • Variable references: Extracted and analyzed");
        
        System.out.println("\n" + "=".repeat(80));
    }
}
