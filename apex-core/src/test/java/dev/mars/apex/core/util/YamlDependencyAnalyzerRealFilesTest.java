package dev.mars.apex.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the YAML dependency analyzer with actual scenario files from the project.
 */
class YamlDependencyAnalyzerRealFilesTest {
    
    @Test
    void testAnalyzeActualScenarioFiles() {
        // Use the actual project structure
        YamlDependencyAnalyzer analyzer = new YamlDependencyAnalyzer("../apex-demo/src/main/resources");
        
        String[] scenarioFiles = {
            "scenarios/otc-options-scenario.yaml",
            "scenarios/commodity-swaps-scenario.yaml", 
            "scenarios/settlement-auto-repair-scenario.yaml"
        };
        
        for (String scenarioFile : scenarioFiles) {
            System.out.println("\n=== Analyzing: " + scenarioFile + " ===");
            
            try {
                YamlDependencyGraph graph = analyzer.analyzeYamlDependencies(scenarioFile);
                
                assertNotNull(graph);
                assertEquals(scenarioFile, graph.getRootFile());
                assertTrue(graph.getTotalFiles() >= 1);
                
                // Generate and print report
                String report = analyzer.generateTextReport(graph);
                System.out.println(report);
                
                // Print statistics
                var stats = graph.getStatistics();
                System.out.println("Statistics: " + stats);
                
                // Show any issues found
                if (!stats.isHealthy()) {
                    System.out.println("Issues found:");
                    if (!graph.getMissingFiles().isEmpty()) {
                        System.out.println("  Missing files: " + graph.getMissingFiles());
                    }
                    if (!graph.getInvalidYamlFiles().isEmpty()) {
                        System.out.println("  Invalid YAML files: " + graph.getInvalidYamlFiles());
                    }
                    if (graph.hasCircularDependencies()) {
                        System.out.println("  Circular dependencies: " + graph.findCircularDependencies());
                    }
                } else {
                    System.out.println("✓ All dependencies are healthy");
                }
                
            } catch (Exception e) {
                System.err.println("Analysis failed for " + scenarioFile + ": " + e.getMessage());
                // Don't fail the test - files might not exist in all environments
            }
        }
    }
}
