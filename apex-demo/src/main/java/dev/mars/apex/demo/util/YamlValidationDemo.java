package dev.mars.apex.demo.util;

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


import dev.mars.apex.core.service.yaml.YamlValidationService;
import dev.mars.apex.core.util.YamlValidationResult;
import dev.mars.apex.core.util.YamlValidationSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Demonstration of YAML metadata validation across all project files.
 * 
 * This demo validates all YAML files in the project to ensure they have
 * proper metadata structure including required fields like 'type', and
 * validates content against expected schemas for different file types.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class YamlValidationDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(YamlValidationDemo.class);
    
    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("YAML METADATA VALIDATION DEMONSTRATION");
        System.out.println("=================================================================");
        System.out.println("Demo Purpose: Validate all YAML files in the project for proper metadata");
        System.out.println("Validation: Check required fields, file types, and content structure");
        System.out.println("Expected Duration: ~3-5 seconds");
        System.out.println("=================================================================");

        YamlValidationDemo demo = new YamlValidationDemo();
        long totalStartTime = System.currentTimeMillis();

        try {
            System.out.println("Initializing YAML Validation Demo...");
            demo.runDemo();

            long totalEndTime = System.currentTimeMillis();
            long totalDuration = totalEndTime - totalStartTime;

            System.out.println("=================================================================");
            System.out.println("YAML VALIDATION DEMO COMPLETED SUCCESSFULLY!");
            System.out.println("=================================================================");
            System.out.println("Total Execution Time: " + totalDuration + " ms");
            System.out.println("Demo Status: SUCCESS");
            System.out.println("=================================================================");

        } catch (Exception e) {
            long totalEndTime = System.currentTimeMillis();
            long totalDuration = totalEndTime - totalStartTime;

            System.err.println("=================================================================");
            System.err.println("YAML VALIDATION DEMO FAILED!");
            System.err.println("=================================================================");
            System.err.println("Error Message: " + e.getMessage());
            System.err.println("Execution Time: " + totalDuration + " ms");
            System.err.println("Demo Status: FAILED");
            System.err.println("=================================================================");
            e.printStackTrace();
        }
    }
    
    private void runDemo() {
        logger.info("Starting YAML metadata validation demonstration...");

        YamlValidationService validationService = new YamlValidationService("src/main/resources");

        // Discover all YAML files in the project
        List<String> yamlFiles = validationService.discoverYamlFiles("src/main/resources");

        System.out.println("\nDiscovered YAML Files:");
        System.out.println("======================");
        for (String file : yamlFiles) {
            System.out.println("  " + file);
        }
        System.out.println("Total files found: " + yamlFiles.size());

        // Validate all files
        System.out.println("\nValidating YAML Files...");
        System.out.println("=========================");

        YamlValidationSummary summary = validationService.validateFiles(yamlFiles);
        
        // Display summary
        System.out.println("\nValidation Summary:");
        System.out.println("==================");
        System.out.println("Total Files: " + summary.getTotalCount());
        System.out.println("Valid Files: " + summary.getValidCount());
        System.out.println("Invalid Files: " + summary.getInvalidCount());
        System.out.println("Files with Warnings: " + summary.getWarningCount());
        System.out.println("Overall Status: " + (summary.isAllValid() ? "PASS" : "FAIL"));
        
        // Show detailed results for each file
        System.out.println("\nDetailed Results:");
        System.out.println("=================");
        
        for (YamlValidationResult result : summary.getResults()) {
            String status = result.getStatus();
            String indicator = switch (status) {
                case "VALID" -> "✓";
                case "VALID_WITH_WARNINGS" -> "⚠";
                case "INVALID" -> "✗";
                default -> "?";
            };
            
            System.out.printf("  %s %s (%s)%n", indicator, result.getFilePath(), status);
            
            // Show errors
            if (!result.getErrors().isEmpty()) {
                for (String error : result.getErrors()) {
                    System.out.println("      ERROR: " + error);
                }
            }
            
            // Show warnings
            if (!result.getWarnings().isEmpty()) {
                for (String warning : result.getWarnings()) {
                    System.out.println("      WARNING: " + warning);
                }
            }
        }
        
        // Generate comprehensive report
        if (!summary.isAllValid() || summary.getWarningCount() > 0) {
            System.out.println("\nComprehensive Validation Report:");
            System.out.println("=================================");
            System.out.println(summary.getReport());
        }
        
        // Demonstrate individual file validation
        demonstrateIndividualValidation(validationService);
        
        // Show recommendations
        showRecommendations(summary);
    }
    
    /**
     * Discovers all YAML files in the specified directory.
     */
    private List<String> discoverYamlFiles(String baseDir) {
        List<String> yamlFiles = new ArrayList<>();
        
        try {
            Path basePath = Paths.get(baseDir);
            if (!Files.exists(basePath)) {
                logger.warn("Base directory does not exist: {}", baseDir);
                return yamlFiles;
            }
            
            try (Stream<Path> paths = Files.walk(basePath)) {
                paths.filter(Files::isRegularFile)
                     .filter(path -> path.toString().toLowerCase().endsWith(".yaml") || 
                                   path.toString().toLowerCase().endsWith(".yml"))
                     .forEach(path -> {
                         String relativePath = basePath.relativize(path).toString().replace('\\', '/');
                         yamlFiles.add(relativePath);
                     });
            }
            
        } catch (Exception e) {
            logger.error("Failed to discover YAML files in: {}", baseDir, e);
        }
        
        return yamlFiles;
    }
    
    /**
     * Demonstrates individual file validation with detailed output.
     */
    private void demonstrateIndividualValidation(YamlValidationService validationService) {
        System.out.println("\nIndividual File Validation Example:");
        System.out.println("===================================");
        
        // Pick a scenario file for detailed validation
        String exampleFile = "validation/otc-options-scenario.yaml";
        
        System.out.println("Validating: " + exampleFile);
        
        YamlValidationResult result = validationService.validateFile(exampleFile);
        
        System.out.println("Result Summary:");
        System.out.println(result.getSummary());
    }
    
    /**
     * Shows recommendations based on validation results.
     */
    private void showRecommendations(YamlValidationSummary summary) {
        System.out.println("\nRecommendations:");
        System.out.println("================");
        
        if (summary.isAllValid() && summary.getWarningCount() == 0) {
            System.out.println("✓ All YAML files are valid and well-structured!");
            System.out.println("✓ No action required - excellent metadata hygiene!");
        } else {
            if (summary.getInvalidCount() > 0) {
                System.out.println("⚠ Fix invalid files:");
                System.out.println("  - Add missing 'type' fields to metadata sections");
                System.out.println("  - Ensure all required metadata fields are present");
                System.out.println("  - Fix YAML syntax errors");
                System.out.println("  - Validate file type values against allowed types");
            }
            
            if (summary.getWarningCount() > 0) {
                System.out.println("📝 Address warnings:");
                System.out.println("  - Update version numbers to semantic versioning format");
                System.out.println("  - Add missing optional metadata fields");
                System.out.println("  - Review content structure recommendations");
            }
            
            System.out.println("\n💡 Best Practices:");
            System.out.println("  - Always include complete metadata sections");
            System.out.println("  - Use semantic versioning (e.g., 1.0.0)");
            System.out.println("  - Specify clear, descriptive names and descriptions");
            System.out.println("  - Include ownership and contact information");
            System.out.println("  - Run validation as part of CI/CD pipeline");
        }
        
        System.out.println("\n🔧 Tools Available:");
        System.out.println("  - YamlMetadataValidator: Comprehensive validation");
        System.out.println("  - YamlDependencyAnalyzer: Dependency chain analysis");
        System.out.println("  - Integration tests: Automated validation in builds");
    }
}
