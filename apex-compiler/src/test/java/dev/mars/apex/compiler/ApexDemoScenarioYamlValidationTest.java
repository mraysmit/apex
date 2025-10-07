package dev.mars.apex.compiler;

import dev.mars.apex.compiler.lexical.ApexYamlLexicalValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates all YAML files in apex-demo scenario package using APEX compiler.
 * 
 * This test ensures all scenario YAML files comply with APEX YAML specification
 * and coding guidelines.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("APEX Demo Scenario YAML Validation Test")
class ApexDemoScenarioYamlValidationTest {

    private ApexYamlLexicalValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new ApexYamlLexicalValidator();
    }
    
    @Test
    @DisplayName("Should validate all apex-demo scenario YAML files")
    void testValidateAllApexDemoScenarioYamlFiles() {
        System.out.println("\n=== VALIDATING APEX-DEMO SCENARIO YAML FILES ===\n");
        
        List<String> yamlFiles = List.of(
            // BasicStageConfigurationTest files
            "apex-demo/src/test/java/dev/mars/apex/demo/scenario/BasicStageConfigurationTest.yaml",
            "apex-demo/src/test/java/dev/mars/apex/demo/scenario/BasicStageConfigurationTest-scenario.yaml",
            "apex-demo/src/test/java/dev/mars/apex/demo/scenario/BasicStageConfigurationTest-validation-rules.yaml",
            "apex-demo/src/test/java/dev/mars/apex/demo/scenario/BasicStageConfigurationTest-enrichment-rules.yaml",
            "apex-demo/src/test/java/dev/mars/apex/demo/scenario/BasicStageConfigurationTest-failing-registry.yaml",
            "apex-demo/src/test/java/dev/mars/apex/demo/scenario/BasicStageConfigurationTest-failing-scenario.yaml",
            "apex-demo/src/test/java/dev/mars/apex/demo/scenario/BasicStageConfigurationTest-failing-validation-rules.yaml",
            
            // InputDataClassificationPhase1Test files
            "apex-demo/src/test/java/dev/mars/apex/demo/scenario/InputDataClassificationPhase1Test.yaml",
            "apex-demo/src/test/java/dev/mars/apex/demo/scenario/InputDataClassificationPhase1Test-scenario.yaml",
            "apex-demo/src/test/java/dev/mars/apex/demo/scenario/InputDataClassificationPhase1Test-validation-rules.yaml",
            "apex-demo/src/test/java/dev/mars/apex/demo/scenario/InputDataClassificationPhase1Test-enrichment-rules.yaml",
            
            // ValidationFailureScenarioTest files
            "apex-demo/src/test/java/dev/mars/apex/demo/scenario/ValidationFailureScenarioTest.yaml",
            "apex-demo/src/test/java/dev/mars/apex/demo/scenario/ValidationFailureScenarioTest-scenario.yaml",
            "apex-demo/src/test/java/dev/mars/apex/demo/scenario/ValidationFailureScenarioTest-validation-rules.yaml",
            "apex-demo/src/test/java/dev/mars/apex/demo/scenario/ValidationFailureScenarioTest-enrichment-rules.yaml"
        );
        
        int validCount = 0;
        int totalCount = 0;
        List<String> invalidFiles = new ArrayList<>();
        
        for (String filePath : yamlFiles) {
            totalCount++;
            Path yamlFile = Paths.get("..").resolve(filePath);
            
            String fileName = yamlFile.getFileName().toString();
            System.out.println("Validating: " + fileName);
            
            try {
                ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);
                
                if (result.isValid()) {
                    validCount++;
                    System.out.println("✅ VALID - " + fileName);
                } else {
                    invalidFiles.add(filePath);
                    System.out.println("❌ INVALID - " + fileName);
                    System.out.println("   Errors:");
                    result.getErrors().forEach(error ->
                        System.out.println("     • " + error));
                    
                    if (!result.getWarnings().isEmpty()) {
                        System.out.println("   Warnings:");
                        result.getWarnings().forEach(warning ->
                            System.out.println("     ⚠️  " + warning));
                    }
                }
                
            } catch (Exception e) {
                invalidFiles.add(filePath);
                System.out.println("💥 ERROR - " + fileName);
                System.out.println("   " + e.getMessage());
            }
            
            System.out.println();
        }
        
        // Summary
        System.out.println("=".repeat(60));
        System.out.println("VALIDATION SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("Total files:   " + totalCount);
        System.out.println("Valid files:   " + validCount);
        System.out.println("Invalid files: " + (totalCount - validCount));
        System.out.println("Success rate:  " + String.format("%.1f%%", (validCount * 100.0) / totalCount));
        System.out.println("=".repeat(60));
        
        if (!invalidFiles.isEmpty()) {
            System.out.println("\n❌ INVALID FILES:");
            invalidFiles.forEach(file -> System.out.println("  • " + file));
            System.out.println("\nCommon issues to fix:");
            System.out.println("  1. Change 'type: enrichment-config' to 'type: rule-config'");
            System.out.println("  2. Change 'author:' to 'owner:' for scenario files");
            System.out.println("  3. Change 'author:' to 'created-by:' for scenario-registry files");
            System.out.println("  4. Change 'scenario-registry:' to 'scenarios:' section");
        }
        
        // Assert all files are valid
        assertEquals(totalCount, validCount, 
            "All apex-demo scenario YAML files should be valid. Invalid files: " + invalidFiles);
    }
}

