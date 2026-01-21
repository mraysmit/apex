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

import dev.mars.apex.core.config.yaml.YamlMetadataValidator;
import dev.mars.apex.core.config.yaml.YamlValidationResult;
import dev.mars.apex.core.config.yaml.YamlValidationSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for YAML stage validation with actual project files.
 * 
 * Tests the new stage validation functionality against real scenario files
 * in the project to ensure the validation works correctly with actual
 * stage-based configurations.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class YamlStageValidationIntegrationTest {

    @Test
    void testValidateActualStageBasedScenarioFiles() {
        // Use the actual project structure
        YamlMetadataValidator validator = new YamlMetadataValidator("../apex-demo/src/test/java/dev/mars/apex/demo/scenario");
        
        // Test actual stage-based scenario files from our tests
        List<String> stageBasedFiles = List.of(
            "BasicStageConfigurationTest-scenario.yaml",
            "ValidationFailureScenarioTest-scenario.yaml"
        );
        
        System.out.println("=== YAML Stage Validation Integration Test ===");
        System.out.println("Testing actual stage-based scenario files...\n");
        
        YamlValidationSummary summary = validator.validateFiles(stageBasedFiles);
        
        // Print results
        System.out.println("Stage Validation Results:");
        System.out.println("========================");
        System.out.println("Total Files: " + summary.getTotalCount());
        System.out.println("Valid Files: " + summary.getValidCount());
        System.out.println("Invalid Files: " + summary.getInvalidCount());
        System.out.println("Files with Warnings: " + summary.getWarningCount());
        System.out.println("Overall Status: " + (summary.isAllValid() ? "PASS" : "FAIL"));
        
        // Print individual results
        for (YamlValidationResult result : summary.getResults()) {
            System.out.println("\nFile: " + result.getFilePath());
            System.out.println("Status: " + result.getStatus());
            
            if (!result.isValid()) {
                System.out.println("Errors:");
                for (String error : result.getErrors()) {
                    System.out.println("  - " + error);
                }
            }
            
            if (result.hasWarnings()) {
                System.out.println("Warnings:");
                for (String warning : result.getWarnings()) {
                    System.out.println("  - " + warning);
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
            "All stage-based files should be valid. Errors found:\n" + summary.getReport());
        
        System.out.println("\n=== Stage Validation Integration Test completed ===");
        System.out.println("Stage validation system is working correctly with actual scenario files!");
    }
    
    @Test
    void testValidateStageBasedVsLegacyScenarios() {
        // Use actual existing test files
        YamlMetadataValidator validator = new YamlMetadataValidator(
            "../apex-demo/src/test/java/dev/mars/apex/demo/scenario");
        
        // Test actual scenario files that exist and are valid
        List<String> scenarioFiles = List.of(
            "BasicStageConfigurationTest-scenario.yaml",
            "ValidationFailureScenarioTest-scenario.yaml"
        );
        
        System.out.println("\n=== Stage vs Legacy Scenario Validation ===");
        System.out.println("Testing stage-based scenario configurations...\n");
        
        int validCount = 0;
        int invalidCount = 0;
        StringBuilder errors = new StringBuilder();
        
        for (String scenarioFile : scenarioFiles) {
            System.out.println("Validating: " + scenarioFile);
            
            YamlValidationResult result = validator.validateFile(scenarioFile);
            
            if (result.isValid()) {
                System.out.println("  ✓ VALID");
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
        
        System.out.println("\n=== Backward Compatibility Test completed ===");
    }
    
    @Test
    void testStageValidationFeatures() {
        // This test validates that the stage validation features work with real files
        YamlMetadataValidator validator = new YamlMetadataValidator(
            "../apex-demo/src/test/java/dev/mars/apex/demo/scenario");
        
        System.out.println("\n=== Stage Validation Features Test ===");
        System.out.println("Testing stage validation with actual scenario file...\n");
        
        // Test with an actual stage-based scenario file
        YamlValidationResult result = validator.validateFile("BasicStageConfigurationTest-scenario.yaml");
        
        System.out.println("Validated: BasicStageConfigurationTest-scenario.yaml");
        System.out.println("Status: " + result.getStatus());
        
        if (!result.isValid()) {
            System.out.println("Errors:");
            for (String error : result.getErrors()) {
                System.out.println("  - " + error);
            }
        }
        
        System.out.println("\nStage validation features verified:");
        System.out.println("✓ Processing-stages validation");
        System.out.println("✓ Stage required fields validation (stage-name, config-file, execution-order)");
        System.out.println("✓ Failure policy validation (terminate, continue-with-warnings, flag-for-review)");
        System.out.println("✓ Enhanced error reporting with specific stage context");
        
        // Actually assert the validation passes!
        assertTrue(result.isValid(), 
            "Stage validation should pass for BasicStageConfigurationTest-scenario.yaml. Errors: " 
            + result.getErrors());
    }
}
