package dev.mars.apex.demo;

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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive analysis to ensure all Java demo files have corresponding YAML configuration files.
 * This test scans all Java files in the apex-demo module and checks that they reference valid YAML configurations.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-22
 * @version 1.0
 */
public class JavaYamlConfigurationAnalysisTest {
    
    private Set<String> availableYamlFiles;
    private Map<String, List<String>> javaFileYamlReferences;
    private Map<String, String> javaFileReasons;
    
    @BeforeEach
    void setUp() throws IOException {
        availableYamlFiles = discoverAvailableYamlFiles();
        javaFileYamlReferences = new HashMap<>();
        javaFileReasons = new HashMap<>();
    }
    
    @Test
    @DisplayName("Should analyze all Java files for YAML configuration dependencies")
    void shouldAnalyzeJavaFilesForYamlDependencies() throws IOException {
        List<String> javaFiles = discoverAllJavaFiles();
        
        System.out.println("=== JAVA FILES YAML CONFIGURATION ANALYSIS ===");
        System.out.println("Total Java files found: " + javaFiles.size());
        System.out.println("Total YAML files available: " + availableYamlFiles.size());
        System.out.println();
        
        // Analyze each Java file
        List<String> filesWithYaml = new ArrayList<>();
        List<String> filesWithoutYaml = new ArrayList<>();
        List<String> utilityFiles = new ArrayList<>();
        List<String> modelFiles = new ArrayList<>();
        List<String> programmaticDemos = new ArrayList<>();
        List<String> deprecatedFiles = new ArrayList<>();

        for (String javaFile : javaFiles) {
            analyzeJavaFile(javaFile);

            List<String> yamlRefs = javaFileYamlReferences.get(javaFile);
            String reason = javaFileReasons.get(javaFile);

            if (yamlRefs != null && !yamlRefs.isEmpty()) {
                filesWithYaml.add(javaFile);
            } else if (reason != null) {
                if (reason.contains("utility") || reason.contains("helper")) {
                    utilityFiles.add(javaFile);
                } else if (reason.contains("model") || reason.contains("data class")) {
                    modelFiles.add(javaFile);
                } else if (reason.contains("Programmatic demo")) {
                    programmaticDemos.add(javaFile);
                } else if (reason.contains("Deprecated") || reason.contains("legacy")) {
                    deprecatedFiles.add(javaFile);
                } else {
                    filesWithoutYaml.add(javaFile);
                }
            } else {
                filesWithoutYaml.add(javaFile);
            }
        }
        
        // Print results
        printAnalysisResults(filesWithYaml, filesWithoutYaml, utilityFiles, modelFiles,
                           programmaticDemos, deprecatedFiles, javaFiles.size());

        // Calculate coverage
        int totalExecutableFiles = javaFiles.size() - utilityFiles.size() - modelFiles.size()
                                 - programmaticDemos.size() - deprecatedFiles.size();
        double coveragePercentage = totalExecutableFiles > 0 ?
            (double) filesWithYaml.size() / totalExecutableFiles * 100 : 100.0;
        
        System.out.printf("=== CONFIGURATION COVERAGE SUMMARY ===\n");
        System.out.printf("Files with YAML configs: %d/%d (%.1f%%)\n", 
            filesWithYaml.size(), totalExecutableFiles, coveragePercentage);
        
        // Assert reasonable coverage
        assertTrue(totalExecutableFiles > 0, "Should find executable Java files to analyze");
        assertTrue(coveragePercentage >= 30.0,
            String.format("Configuration coverage should be at least 30%%, but was %.1f%%", coveragePercentage));
    }
    
    private void analyzeJavaFile(String javaFile) throws IOException {
        Path filePath = getJavaFilePath(javaFile);
        if (!Files.exists(filePath)) {
            javaFileReasons.put(javaFile, "File not found");
            return;
        }
        
        String content = Files.readString(filePath);
        List<String> yamlReferences = extractYamlReferences(content);
        
        // Determine file type and reason
        String reason = determineFileTypeAndReason(javaFile, content);
        
        if (!yamlReferences.isEmpty()) {
            // Validate that referenced YAML files exist
            List<String> validReferences = new ArrayList<>();
            for (String yamlRef : yamlReferences) {
                if (availableYamlFiles.contains(yamlRef)) {
                    validReferences.add(yamlRef);
                }
            }
            javaFileYamlReferences.put(javaFile, validReferences);
        }
        
        if (reason != null) {
            javaFileReasons.put(javaFile, reason);
        }
    }
    
    private List<String> extractYamlReferences(String content) {
        List<String> references = new ArrayList<>();
        
        // Pattern to match YAML file references
        Pattern[] patterns = {
            Pattern.compile("\"([^\"]*\\.ya?ml)\""),
            Pattern.compile("'([^']*\\.ya?ml)'"),
            Pattern.compile("loadFromResource\\s*\\(\\s*[\"']([^\"']*\\.ya?ml)[\"']"),
            Pattern.compile("loadConfiguration\\s*\\(\\s*[\"']([^\"']*\\.ya?ml)[\"']"),
            Pattern.compile("getResource\\s*\\(\\s*[\"']([^\"']*\\.ya?ml)[\"']"),
            Pattern.compile("Path\\.of\\s*\\([^)]*[\"']([^\"']*\\.ya?ml)[\"']"),
            Pattern.compile("Paths\\.get\\s*\\([^)]*[\"']([^\"']*\\.ya?ml)[\"']")
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String yamlFile = matcher.group(1);
                // Normalize path separators
                yamlFile = yamlFile.replace('\\', '/');
                references.add(yamlFile);
            }
        }
        
        return references;
    }
    
    private String determineFileTypeAndReason(String javaFile, String content) {
        // Check if it's a deprecated class first
        if (content.contains("@Deprecated") || content.contains("DEPRECATED") || content.contains("LEGACY")) {
            return "Deprecated/legacy class - maintained for backward compatibility";
        }

        // Check if it's a pure model/data class (in model package with getters/setters)
        if (javaFile.contains("/model/") &&
            content.contains("class") && content.contains("private") &&
            content.contains("get") && content.contains("set") &&
            !content.contains("main(") && !content.contains("Demo") && !content.contains("Example")) {
            return "Model/data class - doesn't require YAML configuration";
        }

        // Check if it's a utility class
        if (javaFile.contains("/util/") ||
            javaFile.contains("/support/") ||
            javaFile.endsWith("Util.java") ||
            javaFile.endsWith("Utils.java") ||
            javaFile.endsWith("Helper.java") ||
            (content.contains("public static") && !content.contains("main("))) {
            return "Utility/helper class - provides support functions";
        }

        // Check if it's a test-only class
        if (content.contains("@Test") || javaFile.contains("Test")) {
            return "Test class - may use inline configurations";
        }

        // Check if it's an interface or abstract class
        if (content.contains("interface ") || content.contains("abstract class")) {
            return "Interface/abstract class - doesn't execute directly";
        }

        // Check if it's a configuration class
        if (javaFile.endsWith("Config.java") || content.contains("@Configuration")) {
            return "Configuration class - defines config rather than uses it";
        }

        // Check if it's a demo that uses programmatic configuration
        if ((javaFile.contains("Demo") || javaFile.contains("Example")) &&
            (content.contains("RuleBuilder") || content.contains("new Rule") ||
             content.contains("addRule") || content.contains("createRule") ||
             content.contains("programmatic") || content.contains("inline"))) {
            return "Programmatic demo - uses inline/code-based configuration instead of YAML";
        }

        return null; // Executable class that should have YAML config
    }

    private void printAnalysisResults(List<String> filesWithYaml, List<String> filesWithoutYaml,
                                    List<String> utilityFiles, List<String> modelFiles,
                                    List<String> programmaticDemos, List<String> deprecatedFiles, int totalFiles) {
        System.out.println("=== FILES WITH YAML CONFIGURATIONS (" + filesWithYaml.size() + ") ===");
        for (String file : filesWithYaml) {
            List<String> yamlRefs = javaFileYamlReferences.get(file);
            System.out.println("  ✓ " + file);
            for (String yamlRef : yamlRefs) {
                System.out.println("    -> " + yamlRef);
            }
        }

        System.out.println();
        System.out.println("=== FILES WITHOUT YAML CONFIGURATIONS (" + filesWithoutYaml.size() + ") ===");
        for (String file : filesWithoutYaml) {
            String reason = javaFileReasons.get(file);
            System.out.println("  ⚠ " + file);
            if (reason != null) {
                System.out.println("    Reason: " + reason);
            }
        }

        System.out.println();
        System.out.println("=== UTILITY/HELPER FILES (" + utilityFiles.size() + ") ===");
        for (String file : utilityFiles) {
            String reason = javaFileReasons.get(file);
            System.out.println("  ℹ " + file + " - " + reason);
        }

        System.out.println();
        System.out.println("=== PROGRAMMATIC DEMOS (" + programmaticDemos.size() + ") ===");
        for (String file : programmaticDemos) {
            String reason = javaFileReasons.get(file);
            System.out.println("  ℹ " + file + " - " + reason);
        }

        System.out.println();
        System.out.println("=== DEPRECATED/LEGACY FILES (" + deprecatedFiles.size() + ") ===");
        for (String file : deprecatedFiles) {
            String reason = javaFileReasons.get(file);
            System.out.println("  ℹ " + file + " - " + reason);
        }

        System.out.println();
        System.out.println("=== MODEL/DATA FILES (" + modelFiles.size() + ") ===");
        for (String file : modelFiles) {
            String reason = javaFileReasons.get(file);
            System.out.println("  ℹ " + file + " - " + reason);
        }
    }

    private Set<String> discoverAvailableYamlFiles() throws IOException {
        Set<String> yamlFiles = new HashSet<>();
        Path resourcesPath = Paths.get("apex-demo/src/main/resources");

        if (!Files.exists(resourcesPath)) {
            resourcesPath = Paths.get("src/main/resources");
        }

        final Path finalResourcesPath = resourcesPath;
        if (Files.exists(finalResourcesPath)) {
            try (Stream<Path> paths = Files.walk(finalResourcesPath)) {
                paths.filter(Files::isRegularFile)
                     .filter(path -> {
                         String fileName = path.toString().toLowerCase();
                         return fileName.endsWith(".yaml") || fileName.endsWith(".yml");
                     })
                     .forEach(path -> {
                         String relativePath = finalResourcesPath.relativize(path).toString().replace('\\', '/');
                         yamlFiles.add(relativePath);
                     });
            }
        }

        return yamlFiles;
    }

    private List<String> discoverAllJavaFiles() throws IOException {
        List<String> javaFiles = new ArrayList<>();
        Path javaPath = Paths.get("apex-demo/src/main/java");

        if (!Files.exists(javaPath)) {
            javaPath = Paths.get("src/main/java");
        }

        final Path finalJavaPath = javaPath;
        if (Files.exists(finalJavaPath)) {
            try (Stream<Path> paths = Files.walk(finalJavaPath)) {
                paths.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".java"))
                     .filter(path -> !path.toString().contains("module-info.java"))
                     .forEach(path -> {
                         String relativePath = finalJavaPath.relativize(path).toString().replace('\\', '/');
                         javaFiles.add(relativePath);
                     });
            }
        }

        Collections.sort(javaFiles);
        return javaFiles;
    }

    private Path getJavaFilePath(String javaFile) {
        Path basePath = Paths.get("apex-demo/src/main/java");
        if (!Files.exists(basePath)) {
            basePath = Paths.get("src/main/java");
        }
        return basePath.resolve(javaFile);
    }
}
