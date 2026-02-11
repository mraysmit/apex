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


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


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
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class YamlDependencyAnalyzerTest {
    
    private static final Logger logger = LoggerFactory.getLogger(YamlDependencyAnalyzerTest.class);

    @TempDir
    Path tempDir;
    
    private YamlDependencyAnalyzer analyzer;

    @BeforeAll
    static void classSetUp() {
        MDC.put("testContext", "[EXPECTED] ");
        LoggerFactory.getLogger(YamlDependencyAnalyzerTest.class)
            .info("[INTENTIONAL-FAILURE-TEST-CLASS-START] YamlDependencyAnalyzerTest intentionally triggers ERROR/WARN logs");
        LoggerFactory.getLogger(YamlDependencyAnalyzerTest.class)
            .info("[INTENTIONAL-FAILURE-TEST-CLASS-START] Expected: YAML parsing errors, missing file references, circular dependency warnings");
    }

    @AfterAll
    static void classTearDown() {
        LoggerFactory.getLogger(YamlDependencyAnalyzerTest.class)
            .info("[INTENTIONAL-FAILURE-TEST-CLASS-END] YamlDependencyAnalyzerTest intentional error tests completed");
        MDC.remove("testContext");
    }
    
    @BeforeEach
    void setUp() {
        analyzer = new YamlDependencyAnalyzer(tempDir.toString());
    }
    
    @Test
    void testSimpleScenarioAnalysis() throws IOException {
        // Create a simple scenario file
        String scenarioContent = """
            metadata:
              id: "test-scenario"
              name: "Test Scenario"
              version: "1.0.0"
              description: "Test scenario for dependency analysis"
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
              id: "test-rules"
              name: "Test Rules"
              version: "1.0.0"
              description: "Test rule configuration"
              type: "rule-config"

            rules:
              rule-chains:
                - "chains/validation-chain.yaml"
              enrichment-refs:
                - "enrichments/test-enrichment.yaml"
            """;

        String bootstrapContent = """
            metadata:
              id: "test-bootstrap"
              name: "Test Bootstrap"
              version: "1.0.0"
              description: "Test bootstrap configuration"
              type: "rule-config"

            datasets:
              - name: "test-dataset"
                type: "inline"
            """;
        
        // Create the referenced files that are expected
        String chainContent = """
            metadata:
              id: "validation-chain"
              name: "Validation Chain"
              version: "1.0.0"
              description: "Validation chain configuration"
              type: "rule-chain"

            chain:
              name: "validation-chain"
              steps:
                - type: "validation"
                  name: "basic-validation"
            """;

        String enrichmentContent = """
            metadata:
              id: "test-enrichment"
              name: "Test Enrichment"
              version: "1.0.0"
              description: "Test enrichment configuration"
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
              id: "test-scenario-missing"
              name: "Test Scenario with Missing Files"
              version: "1.0.0"
              description: "Test scenario with missing file references"
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
              id: "test-scenario-invalid"
              name: "Test Scenario with Invalid Reference"
              version: "1.0.0"
              description: "Test scenario referencing invalid YAML"
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
              id: "test-nested"
              name: "Test Nested Dependencies Scenario"
              version: "1.0.0"
              description: "Test scenario with nested dependencies"
              type: "scenario"

            scenario:
              scenario-id: "test-scenario"
              rule-configurations:
                - "config/level1.yaml"
            """;

        // Create level 1 file that references level 2
        String level1Content = """
            metadata:
              id: "level1-rules"
              name: "Level 1 Rules"
              version: "1.0.0"
              description: "Level 1 rule configuration"
              type: "rule-config"

            rules:
              rule-chains:
                - "chains/level2.yaml"
            """;
        
        // Create level 2 file that references level 3
        String level2Content = """
            metadata:
              id: "level2-chain"
              name: "Level 2 Chain"
              version: "1.0.0"
              description: "Level 2 chain configuration"
              type: "rule-chain"

            enrichments:
              enrichment-refs:
                - "enrichments/level3.yaml"
            """;

        // Create level 3 file
        String level3Content = """
            metadata:
              id: "level3-enrichment"
              name: "Level 3 Enrichment"
              version: "1.0.0"
              description: "Level 3 enrichment configuration"
              type: "enrichment"

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
              id: "file1"
              name: "File 1"
              version: "1.0.0"
              description: "First file in circular dependency"
              type: "rule-config"

            rules:
              rule-chains:
                - "config/file2.yaml"
            """;

        String file2Content = """
            metadata:
              id: "file2"
              name: "File 2"
              version: "1.0.0"
              description: "Second file in circular dependency"
              type: "rule-config"

            enrichments:
              enrichment-refs:
                - "config/file3.yaml"
            """;

        String file3Content = """
            metadata:
              id: "file3"
              name: "File 3"
              version: "1.0.0"
              description: "Third file in circular dependency"
              type: "rule-config"

            includes:
              include:
                - "config/file1.yaml"
            """;

        String scenarioContent = """
            metadata:
              id: "circular-test"
              name: "Circular Dependency Test Scenario"
              version: "1.0.0"
              description: "Test scenario for circular dependencies"
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
              id: "text-report-test"
              name: "Text Report Test Scenario"
              version: "1.0.0"
              description: "Test scenario for text report generation"
              type: "scenario"

            scenario:
              scenario-id: "test-scenario"
              rule-configurations:
                - "config/test-rules.yaml"
            """;

        String rulesContent = """
            metadata:
              id: "test-rules"
              name: "Test Rules"
              version: "1.0.0"
              description: "Test rules configuration"
              type: "rule-config"

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
              id: "test-scenario"
              name: "Test Scenario"
              version: "1.0.0"
              description: "Test scenario for file type detection"
              type: "scenario"
            scenario: test
            """);
        writeFile("bootstrap/test-bootstrap.yaml", """
            metadata:
              id: "test-bootstrap"
              name: "Test Bootstrap"
              version: "1.0.0"
              description: "Test bootstrap configuration"
              type: "rule-config"
            bootstrap: test
            """);
        writeFile("enrichments/test-enrichment.yaml", """
            metadata:
              id: "test-enrichment"
              name: "Test Enrichment"
              version: "1.0.0"
              description: "Test enrichment configuration"
              type: "enrichment"
            enrichment: test
            """);
        writeFile("rule-chains/test-chain.yaml", """
            metadata:
              id: "test-chain"
              name: "Test Chain"
              version: "1.0.0"
              description: "Test chain configuration"
              type: "rule-chain"
            chain: test
            """);
        writeFile("datasets/test-dataset.yaml", """
            metadata:
              id: "test-dataset"
              name: "Test Dataset"
              version: "1.0.0"
              description: "Test dataset configuration"
              type: "dataset"
            dataset: test
            """);
        writeFile("config/test-config.yaml", """
            metadata:
              id: "test-config"
              name: "Test Config"
              version: "1.0.0"
              description: "Test configuration"
              type: "rule-config"
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

    @Test
    void testComponentFileTypeDetection() throws IOException {
        // Create a component file with metadata.type = "component"
        writeFile("components/test-component.yaml", """
            metadata:
              id: "test-component"
              name: "Test Component"
              type: "component"
              version: "1.0.0"
              description: "Test component for dependency analysis"

            config-files:
              - file: "config/test-rules.yaml"
                execution-order: 1
            """);

        writeFile("config/test-rules.yaml", """
            metadata:
              id: "test-rules"
              name: "Test Rules"
              version: "1.0.0"
              description: "Test rules configuration"
              type: "rule-config"
            rules: []
            """);

        // Analyze dependencies
        YamlDependencyGraph graph = analyzer.analyzeYamlDependencies("components/test-component.yaml");

        // Verify component file type is correctly detected
        YamlNode componentNode = graph.getNode("components/test-component.yaml");
        assertNotNull(componentNode);
        assertEquals(YamlFileType.COMPONENT, componentNode.getFileType());

        // Verify component references are extracted
        assertTrue(componentNode.getReferencedFiles().contains("config/test-rules.yaml"));
    }

    @Test
    void testComponentRefsExtraction() throws IOException {
        // Create a component that references other components
        writeFile("components/parent-component.yaml", """
            metadata:
              id: "parent-component"
              name: "Parent Component"
              type: "component"
              version: "1.0.0"
              description: "Parent component"

            component-refs:
              - file: "components/child-component.yaml"
                execution-order: 1

            config-files:
              - file: "config/parent-rules.yaml"
                execution-order: 2
            """);

        writeFile("components/child-component.yaml", """
            metadata:
              id: "child-component"
              name: "Child Component"
              type: "component"
              version: "1.0.0"
              description: "Child component"

            config-files:
              - file: "config/child-rules.yaml"
                execution-order: 1
            """);

        writeFile("config/parent-rules.yaml", """
            metadata:
              id: "parent-rules"
              name: "Parent Rules"
              version: "1.0.0"
              description: "Parent rules configuration"
              type: "rule-config"
            rules: []
            """);

        writeFile("config/child-rules.yaml", """
            metadata:
              id: "child-rules"
              name: "Child Rules"
              version: "1.0.0"
              description: "Child rules configuration"
              type: "rule-config"
            rules: []
            """);

        // Analyze dependencies
        YamlDependencyGraph graph = analyzer.analyzeYamlDependencies("components/parent-component.yaml");

        // Verify all files are in the graph
        assertEquals(4, graph.getTotalFiles());

        // Verify parent component references
        YamlNode parentNode = graph.getNode("components/parent-component.yaml");
        assertNotNull(parentNode);
        assertEquals(YamlFileType.COMPONENT, parentNode.getFileType());
        assertTrue(parentNode.getReferencedFiles().contains("components/child-component.yaml"));
        assertTrue(parentNode.getReferencedFiles().contains("config/parent-rules.yaml"));

        // Verify child component references
        YamlNode childNode = graph.getNode("components/child-component.yaml");
        assertNotNull(childNode);
        assertEquals(YamlFileType.COMPONENT, childNode.getFileType());
        assertTrue(childNode.getReferencedFiles().contains("config/child-rules.yaml"));
    }

    @Test
    void testComponentNestingDepthWarning() throws IOException {
        // Create a component with depth 3 (should trigger warning)
        writeFile("components/level1.yaml", """
            metadata:
              id: "level1"
              name: "Level 1 Component"
              type: "component"
              version: "1.0.0"
              description: "Level 1"

            component-refs:
              - file: "components/level2.yaml"
                execution-order: 1
            """);

        writeFile("components/level2.yaml", """
            metadata:
              id: "level2"
              name: "Level 2 Component"
              type: "component"
              version: "1.0.0"
              description: "Level 2"

            component-refs:
              - file: "components/level3.yaml"
                execution-order: 1
            """);

        writeFile("components/level3.yaml", """
            metadata:
              id: "level3"
              name: "Level 3 Component"
              type: "component"
              version: "1.0.0"
              description: "Level 3"

            config-files:
              - file: "config/test-rules.yaml"
                execution-order: 1
            """);

        writeFile("config/test-rules.yaml", """
            metadata:
              id: "test-rules"
              name: "Test Rules"
              version: "1.0.0"
              description: "Test rules configuration for component nesting"
              type: "rule-config"
            rules: []
            """);

        // Analyze dependencies - should log warning for depth 3 but not fail
        YamlDependencyGraph graph = analyzer.analyzeYamlDependencies("components/level1.yaml");

        // Verify all components are in the graph
        assertEquals(4, graph.getTotalFiles());
        assertEquals(3, graph.getMaxDepth()); // level1(0) -> level2(1) -> level3(2) -> rules(3)

        // Verify all components are correctly typed
        assertEquals(YamlFileType.COMPONENT, graph.getNode("components/level1.yaml").getFileType());
        assertEquals(YamlFileType.COMPONENT, graph.getNode("components/level2.yaml").getFileType());
        assertEquals(YamlFileType.COMPONENT, graph.getNode("components/level3.yaml").getFileType());
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
