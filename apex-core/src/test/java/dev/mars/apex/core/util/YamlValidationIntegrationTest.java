package dev.mars.apex.core.util;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import dev.mars.apex.core.config.validation.MetadataValidator;
import dev.mars.apex.core.config.validation.ValidationResult;
import dev.mars.apex.core.config.validation.ValidationSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for YAML validation with actual project files.
 * 
 * These tests MUST fail if the referenced files don't exist or are invalid.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class YamlValidationIntegrationTest {
    
    @Test
    void testValidateActualProjectFiles() {
        // Use actual existing test files
        MetadataValidator validator = new MetadataValidator(
            "../apex-demo/src/test/java/dev/mars/apex/demo/scenario");
        
        // Test files that actually exist in the project and are known to be valid
        List<String> filesToTest = List.of(
            "BasicStageConfigurationTest-scenario.yaml",
            "BasicStageConfigurationTest-validation-rules.yaml",
            "BasicStageConfigurationTest-enrichment-rules.yaml",
            "ValidationFailureScenarioTest-scenario.yaml",
            "ValidationFailureScenarioTest-validation-rules.yaml"
        );
        
        System.out.println("=== YAML Validation Integration Test ===");
        System.out.println("Testing actual project files for proper metadata...\n");
        
        ValidationSummary summary = validator.validateFiles(filesToTest);
        
        // Print results
        System.out.println("Validation Results:");
        System.out.println("==================");
        System.out.println("Total Files: " + summary.getTotalCount());
        System.out.println("Valid Files: " + summary.getValidCount());
        System.out.println("Invalid Files: " + summary.getInvalidCount());
        System.out.println("Files with Warnings: " + summary.getWarningCount());
        System.out.println("Overall Status: " + (summary.isAllValid() ? "PASS" : "FAIL"));
        
        // Show detailed results
        System.out.println("\nDetailed Results:");
        for (ValidationResult result : summary.getResults()) {
            String status = result.getStatus();
            String indicator = switch (status) {
                case "VALID" -> "[OK]";
                case "VALID_WITH_WARNINGS" -> "⚠";
                case "INVALID" -> "✗";
                default -> "?";
            };
            
            System.out.printf("  %s %s (%s)%n", indicator, result.getFilePath(), status);
            
            // Show errors and warnings
            if (!result.getErrors().isEmpty()) {
                for (String error : result.getErrors()) {
                    System.out.println("      ERROR: " + error);
                }
            }
            if (!result.getWarnings().isEmpty()) {
                for (String warning : result.getWarnings()) {
                    System.out.println("      WARNING: " + warning);
                }
            }
        }
        
        // Print comprehensive report if there are issues
        if (!summary.isAllValid() || summary.getWarningCount() > 0) {
            System.out.println("\n" + summary.getReport());
        }
        
        // Assertions for the test - MUST actually validate!
        assertTrue(summary.getTotalCount() > 0, "Should have found files to validate");
        assertTrue(summary.isAllValid(), 
            "All files should be valid. Errors found:\n" + summary.getReport());
        
        System.out.println("\n=== Test completed successfully ===");
        System.out.println("Validation system is working correctly!");
    }
    
    @Test
    void testValidateSpecificScenarioFiles() {
        // Use actual existing test files, not non-existent production files
        MetadataValidator validator = new MetadataValidator(
            "../apex-demo/src/test/java/dev/mars/apex/demo/scenario");
        
        // Test actual scenario files that exist and are valid
        List<String> scenarioFiles = List.of(
            "BasicStageConfigurationTest-scenario.yaml",
            "ValidationFailureScenarioTest-scenario.yaml"
        );
        
        System.out.println("\n=== Scenario Files Validation ===");
        
        int validCount = 0;
        int invalidCount = 0;
        StringBuilder errors = new StringBuilder();
        
        for (String scenarioFile : scenarioFiles) {
            System.out.println("Validating: " + scenarioFile);
            
            ValidationResult result = validator.validateFile(scenarioFile);
            
            if (result.isValid()) {
                System.out.println("  [OK] VALID");
                validCount++;
            } else {
                System.out.println("  ✗ INVALID");
                invalidCount++;
                errors.append("\n").append(scenarioFile).append(":");
                for (String error : result.getErrors()) {
                    System.out.println("    ERROR: " + error);
                    errors.append("\n  - ").append(error);
                }
            }
            
            if (result.hasWarnings()) {
                for (String warning : result.getWarnings()) {
                    System.out.println("    WARNING: " + warning);
                }
            }
        }
        
        // Actually assert validation passes!
        assertEquals(0, invalidCount, 
            "All scenario files should be valid. Invalid files:" + errors);
        assertTrue(validCount > 0, "Should have validated at least one file");
    }
}
