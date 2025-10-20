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


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test to validate ALL YAML files in the project have proper metadata.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
class ComprehensiveYamlValidationTest {
    
    @Test
    void testAllYamlFilesHaveTypeAttribute() throws IOException {
        // Use the actual project structure - YAML files are in apex-core test resources
        // Try multiple possible paths to handle different execution contexts
        String basePath = findYamlBasePath();

        YamlMetadataValidator validator = new YamlMetadataValidator(basePath);

        // Discover ALL YAML files in the project
        List<String> allYamlFiles = discoverAllYamlFiles(basePath);
        
        System.out.println("=== COMPREHENSIVE YAML VALIDATION TEST ===");
        System.out.println("Discovered " + allYamlFiles.size() + " YAML files:");
        
        for (String file : allYamlFiles) {
            System.out.println("  " + file);
        }
        
        // Validate all files
        YamlValidationSummary summary = validator.validateFiles(allYamlFiles);
        
        // Print comprehensive results
        System.out.println("\n=== VALIDATION RESULTS ===");
        System.out.println("Total Files: " + summary.getTotalCount());
        System.out.println("Valid Files: " + summary.getValidCount());
        System.out.println("Invalid Files: " + summary.getInvalidCount());
        System.out.println("Files with Warnings: " + summary.getWarningCount());
        System.out.println("Overall Status: " + (summary.isAllValid() ? "PASS" : "FAIL"));
        
        // Show detailed results for each file
        System.out.println("\n=== DETAILED RESULTS ===");
        for (YamlValidationResult result : summary.getResults()) {
            String status = result.getStatus();
            String indicator = switch (status) {
                case "VALID" -> "✓";
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
        
        // Assertions
        assertTrue(summary.getTotalCount() > 0, "Should have found YAML files to validate");
        
        // Check that all files are valid
        if (!summary.isAllValid()) {
            List<YamlValidationResult> invalidResults = summary.getInvalidResults();
            StringBuilder errorMessage = new StringBuilder("The following YAML files are invalid:\n");
            for (YamlValidationResult result : invalidResults) {
                errorMessage.append("  - ").append(result.getFilePath()).append(":\n");
                for (String error : result.getErrors()) {
                    errorMessage.append("    * ").append(error).append("\n");
                }
            }
            fail(errorMessage.toString());
        }
        
        System.out.println("\n=== TEST COMPLETED SUCCESSFULLY ===");
        System.out.println("All " + summary.getTotalCount() + " YAML files have proper metadata with 'type' attributes!");
    }
    
    /**
     * Finds the correct base path for YAML files by trying multiple possible locations.
     */
    private String findYamlBasePath() {
        String[] possiblePaths = {
            "apex-core/src/test/resources",
            "./apex-core/src/test/resources",
            "src/test/resources"
        };

        for (String path : possiblePaths) {
            Path p = Paths.get(path);
            if (Files.exists(p)) {
                System.out.println("Found YAML base path: " + p.toAbsolutePath());
                return path;
            }
        }

        // Default to the first option if none exist
        System.out.println("Warning: Could not find YAML base path, using default: " + possiblePaths[0]);
        return possiblePaths[0];
    }

    /**
     * Discovers all YAML files in the specified directory recursively.
     */
    private List<String> discoverAllYamlFiles(String baseDir) throws IOException {
        List<String> yamlFiles = new ArrayList<>();

        Path basePath = Paths.get(baseDir);
        if (!Files.exists(basePath)) {
            System.err.println("Base directory does not exist: " + baseDir);
            System.err.println("Current working directory: " + System.getProperty("user.dir"));
            System.err.println("Absolute path would be: " + basePath.toAbsolutePath());
            return yamlFiles;
        }
        
        try (Stream<Path> paths = Files.walk(basePath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> {
                     String fileName = path.toString().toLowerCase();
                     return fileName.endsWith(".yaml") || fileName.endsWith(".yml");
                 })
                 .filter(path -> {
                     // Exclude Docker Compose files and other non-APEX YAML files
                     String fileName = path.getFileName().toString().toLowerCase();
                     return !fileName.equals("docker-compose.yml") &&
                            !fileName.equals("docker-compose.yaml");
                 })
                 .forEach(path -> {
                     String relativePath = basePath.relativize(path).toString().replace('\\', '/');
                     yamlFiles.add(relativePath);
                 });
        }
        
        return yamlFiles;
    }
}
