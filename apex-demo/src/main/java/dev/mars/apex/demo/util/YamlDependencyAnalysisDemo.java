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


import dev.mars.apex.core.service.yaml.YamlDependencyService;
import dev.mars.apex.core.util.YamlDependencyGraph;
import dev.mars.apex.core.util.YamlNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Demonstration of the YAML Dependency Analyzer utility.
 * 
 * This demo analyzes the actual scenario files in the project and shows
 * the complete dependency chains, missing files, and validation results.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class YamlDependencyAnalysisDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(YamlDependencyAnalysisDemo.class);
    
    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("YAML DEPENDENCY ANALYSIS DEMONSTRATION");
        System.out.println("=================================================================");
        System.out.println("Demo Purpose: Analyze YAML file dependencies in scenario configurations");
        System.out.println("Analysis: Trace complete dependency chains from scenarios to rule files");
        System.out.println("Validation: Check for missing files, invalid YAML, circular dependencies");
        System.out.println("Expected Duration: ~2-3 seconds");
        System.out.println("=================================================================");

        YamlDependencyAnalysisDemo demo = new YamlDependencyAnalysisDemo();
        long totalStartTime = System.currentTimeMillis();

        try {
            System.out.println("Initializing YAML Dependency Analysis Demo...");
            demo.runDemo();

            long totalEndTime = System.currentTimeMillis();
            long totalDuration = totalEndTime - totalStartTime;

            System.out.println("=================================================================");
            System.out.println("YAML DEPENDENCY ANALYSIS DEMO COMPLETED SUCCESSFULLY!");
            System.out.println("=================================================================");
            System.out.println("Total Execution Time: " + totalDuration + " ms");
            System.out.println("Demo Status: SUCCESS");
            System.out.println("=================================================================");

        } catch (Exception e) {
            long totalEndTime = System.currentTimeMillis();
            long totalDuration = totalEndTime - totalStartTime;

            System.err.println("=================================================================");
            System.err.println("YAML DEPENDENCY ANALYSIS DEMO FAILED!");
            System.err.println("=================================================================");
            System.err.println("Error Message: " + e.getMessage());
            System.err.println("Execution Time: " + totalDuration + " ms");
            System.err.println("Demo Status: FAILED");
            System.err.println("=================================================================");
            e.printStackTrace();
        }
    }
    
    private void runDemo() {
        logger.info("Starting YAML dependency analysis demonstration...");
        
        YamlDependencyService dependencyService = new YamlDependencyService();

        // List of scenario files to analyze
        String[] scenarioFiles = {
            "validation/otc-options-scenario.yaml",
            "validation/commodity-swaps-scenario.yaml",
            "validation/settlement-auto-repair-scenario.yaml"
        };

        System.out.println("\nAnalyzing scenario files...\n");

        for (String scenarioFile : scenarioFiles) {
            analyzeScenarioFile(dependencyService, scenarioFile);
            System.out.println(); // Add spacing between analyses
        }

        // Demonstrate additional analysis features
        demonstrateAdvancedFeatures(dependencyService);
    }
    
    private void analyzeScenarioFile(YamlDependencyService dependencyService, String scenarioFile) {
        System.out.println("--- Analyzing: " + scenarioFile + " ---");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Perform dependency analysis
            YamlDependencyGraph graph = dependencyService.analyzeDependencies(scenarioFile);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Display summary statistics
            var stats = graph.getStatistics();
            System.out.println("Analysis completed in " + duration + " ms");
            System.out.println("Statistics: " + stats);
            
            // Show dependency tree (abbreviated)
            System.out.println("\nDependency Summary:");
            YamlNode rootNode = graph.getNode(scenarioFile);
            if (rootNode != null && rootNode.getReferencedFiles() != null) {
                for (String referencedFile : rootNode.getReferencedFiles()) {
                    YamlNode refNode = graph.getNode(referencedFile);
                    String status = refNode != null ? refNode.getStatusIndicator() : "?";
                    System.out.println("  └── " + referencedFile + " " + status);
                }
            }
            
            // Show any issues
            if (!stats.isHealthy()) {
                System.out.println("\nIssues Found:");
                
                if (!graph.getMissingFiles().isEmpty()) {
                    System.out.println("  Missing Files:");
                    for (String missingFile : graph.getMissingFiles()) {
                        System.out.println("    [X] " + missingFile);
                    }
                }
                
                if (!graph.getInvalidYamlFiles().isEmpty()) {
                    System.out.println("  Invalid YAML Files:");
                    for (String invalidFile : graph.getInvalidYamlFiles()) {
                        System.out.println("    [!] " + invalidFile);
                    }
                }
                
                if (graph.hasCircularDependencies()) {
                    System.out.println("  Circular Dependencies:");
                    List<List<String>> cycles = graph.findCircularDependencies();
                    for (List<String> cycle : cycles) {
                        System.out.println("    [~] " + String.join(" -> ", cycle));
                    }
                }
            } else {
                System.out.println("[*] All dependencies are healthy");
            }
            
        } catch (Exception e) {
            System.err.println("Failed to analyze " + scenarioFile + ": " + e.getMessage());
            logger.error("Analysis failed for: {}", scenarioFile, e);
        }
    }
    
    private void demonstrateAdvancedFeatures(YamlDependencyService dependencyService) {
        System.out.println("--- Advanced Analysis Features ---");
        
        try {
            // Analyze one scenario in detail
            String detailedScenario = "validation/otc-options-scenario.yaml";
            YamlDependencyGraph graph = dependencyService.analyzeDependencies(detailedScenario);

            System.out.println("\nDetailed Analysis for: " + detailedScenario);

            // Generate dependency report using the service
            var dependencyReport = dependencyService.generateDependencyReport(detailedScenario);
            System.out.println("\nFull Dependency Report:");
            System.out.println("File: " + dependencyReport.getFilePath());
            System.out.println("Statistics: " + dependencyReport.getStatistics());
            System.out.println("Missing Files: " + dependencyReport.getMissingFiles());
            System.out.println("Circular Dependencies: " + dependencyReport.getCircularDependencies());
            System.out.println("Dependency Tree:");
            for (String treeLine : dependencyReport.getDependencyTree()) {
                System.out.println("  " + treeLine);
            }
            
            // Demonstrate reverse dependency lookup
            System.out.println("Reverse Dependency Analysis:");
            for (YamlNode node : graph.getAllNodes()) {
                if (node.isReferenced()) {
                    System.out.println("  " + node.getFilePath() + " is referenced by:");
                    for (String referencedBy : node.getReferencedBy()) {
                        System.out.println("    ← " + referencedBy);
                    }
                }
            }
            
            // Show file type distribution
            System.out.println("\nFile Type Distribution:");
            var fileTypes = new java.util.HashMap<String, Integer>();
            for (YamlNode node : graph.getAllNodes()) {
                String type = node.getFileType().toString();
                fileTypes.put(type, fileTypes.getOrDefault(type, 0) + 1);
            }
            for (var entry : fileTypes.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " files");
            }
            
        } catch (Exception e) {
            System.err.println("Advanced analysis failed: " + e.getMessage());
            logger.error("Advanced analysis failed", e);
        }
    }
}
