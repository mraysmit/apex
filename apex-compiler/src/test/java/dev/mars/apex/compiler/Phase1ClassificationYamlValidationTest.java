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
 * Validates Phase 1 Classification YAML files using APEX compiler.
 * 
 * This test ensures all YAML files created for Phase 1 classification feature
 * are valid according to APEX YAML grammar and lexical rules.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Phase 1 Classification YAML Validation Test")
class Phase1ClassificationYamlValidationTest {

    private ApexYamlLexicalValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new ApexYamlLexicalValidator();
    }
    
    @Test
    @DisplayName("Should validate all Phase 1 classification YAML files")
    void testValidateAllPhase1YamlFiles() {
        System.out.println("\n=== VALIDATING PHASE 1 CLASSIFICATION YAML FILES ===\n");
        
        List<String> yamlFiles = List.of(
            "apex-core/src/test/java/dev/mars/apex/core/service/scenario/DataTypeScenarioServiceClassificationTest-registry.yaml",
            "apex-core/src/test/java/dev/mars/apex/core/service/scenario/DataTypeScenarioServiceClassificationTest-otc-scenario.yaml",
            "apex-core/src/test/java/dev/mars/apex/core/service/scenario/DataTypeScenarioServiceClassificationTest-us-otc-scenario.yaml",
            "apex-core/src/test/java/dev/mars/apex/core/service/scenario/DataTypeScenarioServiceClassificationTest-high-notional-scenario.yaml",
            "apex-core/src/test/java/dev/mars/apex/core/service/scenario/DataTypeScenarioServiceClassificationTest-swap-scenario.yaml",
            "apex-core/src/test/java/dev/mars/apex/core/service/scenario/DataTypeScenarioServiceClassificationTest-validation-rules.yaml",
            "apex-core/src/test/java/dev/mars/apex/core/service/scenario/DataTypeScenarioServiceClassificationTest-enrichment-rules.yaml"
        );
        
        int validCount = 0;
        int totalCount = 0;
        List<String> invalidFiles = new ArrayList<>();
        
        for (String filePath : yamlFiles) {
            totalCount++;
            Path yamlFile = Paths.get("..").resolve(filePath);
            
            System.out.println("Validating: " + yamlFile.getFileName());
            
            try {
                ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);
                
                if (result.isValid()) {
                    validCount++;
                    System.out.println("✅ VALID - " + yamlFile.getFileName());
                } else {
                    invalidFiles.add(filePath);
                    System.out.println("❌ INVALID - " + yamlFile.getFileName());
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
                System.out.println("💥 ERROR - " + yamlFile.getFileName());
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
        }
        
        // Assert all files are valid
        assertEquals(totalCount, validCount, 
            "All Phase 1 classification YAML files should be valid. Invalid files: " + invalidFiles);
    }
}

