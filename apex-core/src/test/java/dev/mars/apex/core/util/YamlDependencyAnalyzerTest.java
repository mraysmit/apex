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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for YamlDependencyAnalyzer.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
/**
 * Test class for YamlDependencyAnalyzer.
 * 
 * This test class creates temporary YAML files to test the dependency analysis
 * functionality without relying on actual project files.
 */
class YamlDependencyAnalyzerTest {
    
    @TempDir
    Path tempDir;
    
    private YamlDependencyAnalyzer analyzer;
    
    @BeforeEach
    void setUp() {
        analyzer = new YamlDependencyAnalyzer(tempDir.toString());
    }
    
    @Test
    void testSimpleScenarioAnalysis() throws IOException {
        // Create a simple scenario file
        String scenarioContent = """
            metadata:
              name: "Test Scenario"
              type: "scenario"
            
            scenario:
              scenario-id: "test-scenario"
              data-types:
                - "TestDataType"
              rule-configurations:
                - "config/test-rules.yaml"
                - "bootstrap/test-bootstrap.yaml"
            """;
        
        // Create rule configuration files
        String ruleConfigContent = """
            metadata:
              name: "Test Rules"
              type: "rule-config"

            rules:
              rule-chains:
                - "chains/validation-chain.yaml"
              enrichment-refs:
                - "enrichments/test-enrichment.yaml"
            """;

        String bootstrapContent = """
            metadata:
              name: "Test Bootstrap"
              type: "bootstrap"

            datasets:
              - name: "test-dataset"
                type: "inline"
            """;
        
        // Create the referenced files that are expected
        String chainContent = """
            metadata:
              name: "Validation Chain"
              type: "chain"

            chain:
              name: "validation-chain"
              steps:
                - type: "validation"
                  name: "basic-validation"
            """;

        String enrichmentContent = """
            metadata:
              name: "Test Enrichment"
              type: "enrichment"

            enrichment:
              name: "test-enrichment"
              type: "lookup"
            """;

        // Write files
        writeFile("scenarios/test-scenario.yaml", scenarioContent);
        writeFile("config/test-rules.yaml", ruleConfigContent);
        writeFile("bootstrap/test-bootstrap.yaml", bootstrapContent);
        writeFile("chains/validation-chain.yaml", chainContent);
        writeFile("enrichments/test-enrichment.yaml", enrichmentContent);
        
        // Analyze dependencies
        YamlDependencyGraph graph = analyzer.analyzeYamlDependencies("scenarios/test-scenario.yaml");
        
        // Verify results
        assertNotNull(graph);
        assertEquals("scenarios/test-scenario.yaml", graph.getRootFile());
        assertEquals(5, graph.getTotalFiles()); // scenario + 2 rule files + 2 referenced files
        assertEquals(0, graph.getMissingFiles().size()); // All files exist now
        assertTrue(graph.getStatistics().isHealthy()); // No missing files
        
        // Verify specific nodes exist
        assertNotNull(graph.getNode("scenarios/test-scenario.yaml"));
        assertNotNull(graph.getNode("config/test-rules.yaml"));
        assertNotNull(graph.getNode("bootstrap/test-bootstrap.yaml"));
        assertNotNull(graph.getNode("chains/validation-chain.yaml"));
        assertNotNull(graph.getNode("enrichments/test-enrichment.yaml"));
        
        // Verify dependencies
        List<YamlDependency> dependencies = graph.getAllDependencies();
        assertEquals(4, dependencies.size()); // 2 from scenario + 2 from rule files
    }
    
    @Test
    void testMissingFileDetection() throws IOException {
        // Create scenario that references missing files
        String scenarioContent = """
            metadata:
              name: "Test Scenario with Missing Files"
              type: "scenario"

            scenario:
              scenario-id: "test-scenario"
              rule-configurations:
                - "missing/file1.yaml"
                - "missing/file2.yaml"
            """;
        
        writeFile("scenarios/test-scenario.yaml", scenarioContent);
        
        // Analyze dependencies
        YamlDependencyGraph graph = analyzer.analyzeYamlDependencies("scenarios/test-scenario.yaml");
        
        // Verify missing files are detected
        assertEquals(3, graph.getTotalFiles()); // scenario + 2 missing files
        assertEquals(2, graph.getMissingFiles().size());
        assertTrue(graph.getMissingFiles().contains("missing/file1.yaml"));
        assertTrue(graph.getMissingFiles().contains("missing/file2.yaml"));
        assertFalse(graph.getStatistics().isHealthy());
    }
    
    @Test
    void testInvalidYamlDetection() throws IOException {
        // Create scenario with valid YAML
        String scenarioContent = """
            metadata:
              name: "Test Scenario with Invalid Reference"
              type: "scenario"

            scenario:
              scenario-id: "test-scenario"
              rule-configurations:
                - "config/invalid.yaml"
            """;
        
        // Create invalid YAML file
        String invalidYamlContent = """
            invalid: yaml: content:
              - missing
                - bracket
            unclosed: [
            """;
        
        writeFile("scenarios/test-scenario.yaml", scenarioContent);
        writeFile("config/invalid.yaml", invalidYamlContent);
        
        // Analyze dependencies
        YamlDependencyGraph graph = analyzer.analyzeYamlDependencies("scenarios/test-scenario.yaml");
        
        // Verify invalid YAML is detected
        assertEquals(1, graph.getInvalidYamlFiles().size());
        assertTrue(graph.getInvalidYamlFiles().contains("config/invalid.yaml"));
        assertFalse(graph.getStatistics().isHealthy());
    }
    
    @Test
    void testNestedDependencies() throws IOException {
        // Create scenario
        String scenarioContent = """
            metadata:
              name: "Test Nested Dependencies Scenario"
              type: "scenario"

            scenario:
              scenario-id: "test-scenario"
              rule-configurations:
                - "config/level1.yaml"
            """;

        // Create level 1 file that references level 2
        String level1Content = """
            metadata:
              name: "Level 1 Rules"
              type: "rule-config"
              author: "test"

            rules:
              rule-chains:
                - "chains/level2.yaml"
            """;
        
        // Create level 2 file that references level 3
        String level2Content = """
            metadata:
              name: "Level 2 Chain"
              type: "rule-chain"
              author: "test"

            enrichments:
              enrichment-refs:
                - "enrichments/level3.yaml"
            """;

        // Create level 3 file
        String level3Content = """
            metadata:
              name: "Level 3 Enrichment"
              type: "enrichment"
              author: "test"

            enrichment:
              name: "Final Level"
            """;
        
        writeFile("scenarios/test-scenario.yaml", scenarioContent);
        writeFile("config/level1.yaml", level1Content);
        writeFile("chains/level2.yaml", level2Content);
        writeFile("enrichments/level3.yaml", level3Content);
        
        // Analyze dependencies
        YamlDependencyGraph graph = analyzer.analyzeYamlDependencies("scenarios/test-scenario.yaml");
        
        // Verify nested dependencies
        assertEquals(4, graph.getTotalFiles());
        assertEquals(3, graph.getMaxDepth()); // 0-based: scenario(0) -> level1(1) -> level2(2) -> level3(3)
        assertTrue(graph.getStatistics().isHealthy());
        
        // Verify dependency chain
        YamlNode scenarioNode = graph.getNode("scenarios/test-scenario.yaml");
        assertTrue(scenarioNode.getReferencedFiles().contains("config/level1.yaml"));
        
        YamlNode level1Node = graph.getNode("config/level1.yaml");
        assertTrue(level1Node.getReferencedFiles().contains("chains/level2.yaml"));
        
        YamlNode level2Node = graph.getNode("chains/level2.yaml");
        assertTrue(level2Node.getReferencedFiles().contains("enrichments/level3.yaml"));
    }
    
    @Test
    void testCircularDependencyDetection() throws IOException {
        // Create files with circular dependencies
        String file1Content = """
            metadata:
              name: "File 1"
              type: "rule-config"
              author: "test"

            rules:
              rule-chains:
                - "config/file2.yaml"
            """;

        String file2Content = """
            metadata:
              name: "File 2"
              type: "rule-config"
              author: "test"

            enrichments:
              enrichment-refs:
                - "config/file3.yaml"
            """;

        String file3Content = """
            metadata:
              name: "File 3"
              type: "rule-config"
              author: "test"

            includes:
              include:
                - "config/file1.yaml"
            """;

        String scenarioContent = """
            metadata:
              name: "Circular Dependency Test Scenario"
              type: "scenario"

            scenario:
              rule-configurations:
                - "config/file1.yaml"
            """;
        
        writeFile("scenarios/test-scenario.yaml", scenarioContent);
        writeFile("config/file1.yaml", file1Content);
        writeFile("config/file2.yaml", file2Content);
        writeFile("config/file3.yaml", file3Content);
        
        // Analyze dependencies
        YamlDependencyGraph graph = analyzer.analyzeYamlDependencies("scenarios/test-scenario.yaml");
        
        // Verify circular dependency detection
        assertTrue(graph.hasCircularDependencies());
        List<List<String>> cycles = graph.findCircularDependencies();
        assertFalse(cycles.isEmpty());
        assertFalse(graph.getStatistics().isHealthy());
    }
    
    @Test
    void testTextReportGeneration() throws IOException {
        // Create simple scenario
        String scenarioContent = """
            metadata:
              name: "Text Report Test Scenario"
              type: "scenario"

            scenario:
              scenario-id: "test-scenario"
              rule-configurations:
                - "config/test-rules.yaml"
            """;

        String rulesContent = """
            metadata:
              name: "Test Rules"
              type: "rule-config"
              author: "test"

            rules:
              name: "Test Rules"
            """;
        
        writeFile("scenarios/test-scenario.yaml", scenarioContent);
        writeFile("config/test-rules.yaml", rulesContent);
        
        // Analyze and generate report
        YamlDependencyGraph graph = analyzer.analyzeYamlDependencies("scenarios/test-scenario.yaml");
        String report = analyzer.generateTextReport(graph);
        
        // Verify report content
        assertNotNull(report);
        assertTrue(report.contains("YAML Dependency Analysis"));
        assertTrue(report.contains("scenarios/test-scenario.yaml"));
        assertTrue(report.contains("Total YAML Files: 2"));
        assertTrue(report.contains("Missing Files: 0"));
        assertTrue(report.contains("Dependency Tree:"));
        assertTrue(report.contains("config/test-rules.yaml"));
    }
    
    @Test
    void testFileTypeDetection() throws IOException {
        // Create files in different directories with proper metadata
        writeFile("scenarios/test-scenario.yaml", """
            metadata:
              name: "Test Scenario"
              type: "scenario"
            scenario: test
            """);
        writeFile("bootstrap/test-bootstrap.yaml", """
            metadata:
              name: "Test Bootstrap"
              type: "bootstrap"
            bootstrap: test
            """);
        writeFile("enrichments/test-enrichment.yaml", """
            metadata:
              name: "Test Enrichment"
              type: "enrichment"
              author: "test"
            enrichment: test
            """);
        writeFile("rule-chains/test-chain.yaml", """
            metadata:
              name: "Test Chain"
              type: "rule-chain"
              author: "test"
            chain: test
            """);
        writeFile("datasets/test-dataset.yaml", """
            metadata:
              name: "Test Dataset"
              type: "dataset"
            dataset: test
            """);
        writeFile("config/test-config.yaml", """
            metadata:
              name: "Test Config"
              type: "rule-config"
              author: "test"
            config: test
            """);
        
        // Test each file type
        YamlDependencyGraph graph1 = analyzer.analyzeYamlDependencies("scenarios/test-scenario.yaml");
        assertEquals(YamlFileType.SCENARIO, graph1.getNode("scenarios/test-scenario.yaml").getFileType());
        
        YamlDependencyGraph graph2 = analyzer.analyzeYamlDependencies("bootstrap/test-bootstrap.yaml");
        assertEquals(YamlFileType.RULE_CONFIG, graph2.getNode("bootstrap/test-bootstrap.yaml").getFileType());
        
        YamlDependencyGraph graph3 = analyzer.analyzeYamlDependencies("enrichments/test-enrichment.yaml");
        assertEquals(YamlFileType.ENRICHMENT, graph3.getNode("enrichments/test-enrichment.yaml").getFileType());
    }
    
    /**
     * Helper method to write content to a file in the temp directory.
     */
    private void writeFile(String relativePath, String content) throws IOException {
        Path filePath = tempDir.resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content);
    }
}
