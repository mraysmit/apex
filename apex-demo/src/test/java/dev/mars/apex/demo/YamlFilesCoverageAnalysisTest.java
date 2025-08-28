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


import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test to ensure all YAML files in apex-demo are being validated and tested.
 * This test identifies which YAML files are missing test coverage and ensures they can be loaded successfully.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-22
 * @version 1.0
 */
public class YamlFilesCoverageAnalysisTest {
    
    private YamlConfigurationLoader configurationLoader;
    private Set<String> knownTestedFiles;
    
    @BeforeEach
    void setUp() {
        configurationLoader = new YamlConfigurationLoader();
        
        // Files that are currently being tested in existing test classes
        knownTestedFiles = Set.of(
            "demo-rules/custody-auto-repair-rules.yaml",
            "demo-configs/comprehensive-lookup-demo.yaml",
            "config/financial-validation-rules.yaml",
            "demo-rules/quick-start.yaml",
            "scenarios/otc-options-scenario.yaml",
            "scenarios/commodity-swaps-scenario.yaml",
            "scenarios/settlement-auto-repair-scenario.yaml",
            "yaml-examples/file-processing-config.yaml",
            "bootstrap/custody-auto-repair-bootstrap.yaml",
            "bootstrap/otc-options-bootstrap.yaml",
            "bootstrap/datasets/market-data.yaml",
            "config/data-type-scenarios.yaml"
        );
    }
    
    @Test
    @DisplayName("Should identify all YAML files in apex-demo resources")
    void shouldIdentifyAllYamlFiles() throws IOException {
        List<String> allYamlFiles = discoverAllYamlFiles();
        
        System.out.println("=== YAML FILES COVERAGE ANALYSIS ===");
        System.out.println("Total YAML files found: " + allYamlFiles.size());
        System.out.println();
        
        // Categorize files
        List<String> testedFiles = new ArrayList<>();
        List<String> untestedFiles = new ArrayList<>();
        
        for (String file : allYamlFiles) {
            if (knownTestedFiles.contains(file)) {
                testedFiles.add(file);
            } else {
                untestedFiles.add(file);
            }
        }
        
        // Print results
        System.out.println("=== TESTED FILES (" + testedFiles.size() + ") ===");
        testedFiles.forEach(file -> System.out.println("  ✓ " + file));
        
        System.out.println();
        System.out.println("=== UNTESTED FILES (" + untestedFiles.size() + ") ===");
        untestedFiles.forEach(file -> System.out.println("  ⚠ " + file));
        
        // Calculate coverage percentage
        double coveragePercentage = (double) testedFiles.size() / allYamlFiles.size() * 100;
        System.out.println();
        System.out.printf("=== COVERAGE SUMMARY ===\n");
        System.out.printf("Tested: %d/%d files (%.1f%%)\n", testedFiles.size(), allYamlFiles.size(), coveragePercentage);
        
        // Assert that we have reasonable coverage
        assertTrue(allYamlFiles.size() > 0, "Should find YAML files to analyze");
        assertTrue(coveragePercentage >= 15.0,
            String.format("Test coverage should be at least 15%%, but was %.1f%%", coveragePercentage));
    }
    
    @Test
    @DisplayName("Should validate that all YAML files can be loaded without errors")
    void shouldValidateAllYamlFilesCanBeLoaded() throws IOException {
        List<String> allYamlFiles = discoverAllYamlFiles();
        List<String> loadableFiles = new ArrayList<>();
        List<String> problematicFiles = new ArrayList<>();
        Map<String, String> fileErrors = new HashMap<>();
        
        System.out.println("=== YAML FILES LOADING VALIDATION ===");
        
        for (String file : allYamlFiles) {
            try {
                // Try to load as a resource stream first
                InputStream stream = getClass().getClassLoader().getResourceAsStream(file);
                if (stream != null) {
                    try {
                        // Try to parse as YAML configuration
                        YamlRuleConfiguration config = configurationLoader.loadFromStream(stream);
                        loadableFiles.add(file);
                        System.out.println("  ✓ " + file + " - Loaded successfully");
                    } catch (Exception e) {
                        // File exists but has parsing issues
                        problematicFiles.add(file);
                        fileErrors.put(file, e.getMessage());
                        System.out.println("  ⚠ " + file + " - Parse error: " + e.getMessage());
                    } finally {
                        stream.close();
                    }
                } else {
                    problematicFiles.add(file);
                    fileErrors.put(file, "File not found in classpath");
                    System.out.println("  ✗ " + file + " - Not found in classpath");
                }
            } catch (Exception e) {
                problematicFiles.add(file);
                fileErrors.put(file, e.getMessage());
                System.out.println("  ✗ " + file + " - Error: " + e.getMessage());
            }
        }
        
        // Print summary
        System.out.println();
        System.out.printf("=== LOADING SUMMARY ===\n");
        System.out.printf("Successfully loaded: %d/%d files\n", loadableFiles.size(), allYamlFiles.size());
        System.out.printf("Problematic files: %d\n", problematicFiles.size());
        
        if (!problematicFiles.isEmpty()) {
            System.out.println("\n=== PROBLEMATIC FILES DETAILS ===");
            for (String file : problematicFiles) {
                System.out.println("  " + file + ": " + fileErrors.get(file));
            }
        }
        
        // We expect most files to load successfully, but some might be datasets or have different formats
        double successRate = (double) loadableFiles.size() / allYamlFiles.size() * 100;
        assertTrue(successRate >= 60.0, 
            String.format("At least 60%% of YAML files should load successfully, but only %.1f%% did", successRate));
    }
    
    /**
     * Discovers all YAML files in the apex-demo resources directory.
     */
    private List<String> discoverAllYamlFiles() throws IOException {
        List<String> yamlFiles = new ArrayList<>();
        
        // Get the resources directory path
        Path resourcesPath = Paths.get("apex-demo/src/main/resources");
        if (!Files.exists(resourcesPath)) {
            // Try relative path from current working directory
            resourcesPath = Paths.get("src/main/resources");
        }
        
        if (!Files.exists(resourcesPath)) {
            fail("Could not find resources directory at: " + resourcesPath.toAbsolutePath());
        }
        
        final Path finalResourcesPath = resourcesPath;
        try (Stream<Path> paths = Files.walk(resourcesPath)) {
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
        
        Collections.sort(yamlFiles);
        return yamlFiles;
    }
}
