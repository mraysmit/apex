package dev.mars.apex.core.integration;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRuleFactory;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for rule reference resolution.
 * 
 * Tests performance impact of rule reference processing and ensures
 * no significant regression compared to inline rules.
 */
@DisplayName("Rule Reference Performance Tests")
class RuleReferencePerformanceTest {

    private YamlConfigurationLoader configLoader;
    private YamlRuleFactory ruleFactory;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        configLoader = new YamlConfigurationLoader();
        ruleFactory = new YamlRuleFactory();
    }

    @Test
    @DisplayName("Should load rule references with acceptable performance")
    void testRuleReferenceLoadingPerformance() throws Exception {
        // Create a rule file with multiple rules
        StringBuilder rulesYamlBuilder = new StringBuilder();
        rulesYamlBuilder.append("""
            metadata:
              name: "Performance Test Rules"
              version: "1.0.0"
            
            rules:
            """);
        
        // Generate 50 rules for performance testing
        for (int i = 1; i <= 50; i++) {
            rulesYamlBuilder.append(String.format("""
              - id: "rule-%d"
                name: "Performance Rule %d"
                condition: "#value%d > %d"
                message: "Rule %d validation failed"
                severity: "ERROR"
                priority: %d
            """, i, i, i, i, i, i));
        }
        
        Path rulesFile = tempDir.resolve("performance-rules.yaml");
        Files.writeString(rulesFile, rulesYamlBuilder.toString());
        
        // Create main configuration with rule reference
        String mainConfigYaml = """
            metadata:
              name: "Performance Test Configuration"
              version: "1.0.0"
            
            rule-refs:
              - name: "performance-rules"
                source: "%s"
                enabled: true
                description: "Performance test rules"
            
            rule-groups:
              - id: "performance-group"
                name: "Performance Test Group"
                operator: "AND"
                rule-ids:
                  - "rule-1"
                  - "rule-2"
                  - "rule-3"
                  - "rule-4"
                  - "rule-5"
            """.formatted(rulesFile.toString().replace("\\", "\\\\"));
        
        Path mainConfigFile = tempDir.resolve("main-config.yaml");
        Files.writeString(mainConfigFile, mainConfigYaml);
        
        // Measure loading time with rule references
        long startTime = System.currentTimeMillis();
        YamlRuleConfiguration config = configLoader.loadFromFile(mainConfigFile.toString());
        RulesEngineConfiguration engineConfig = ruleFactory.createRulesEngineConfiguration(config);
        RulesEngine engine = new RulesEngine(engineConfig);
        long endTime = System.currentTimeMillis();
        
        long loadingTime = endTime - startTime;
        
        // Verify configuration was loaded correctly
        assertNotNull(config.getRules(), "Rules should be loaded");
        assertEquals(50, config.getRules().size(), "Should have 50 rules from referenced file");
        assertEquals(1, config.getRuleGroups().size(), "Should have 1 rule group");
        
        // Performance assertion - loading should complete within reasonable time
        assertTrue(loadingTime < 5000, 
                  String.format("Loading should complete within 5 seconds, took %d ms", loadingTime));
        
        System.out.printf("Rule reference loading time: %d ms for 50 rules%n", loadingTime);
        
        // Test execution performance
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("performance-group");
        assertNotNull(ruleGroup, "Rule group should be found");
        
        Map<String, Object> testData = Map.of(
            "value1", 10,
            "value2", 20,
            "value3", 30,
            "value4", 40,
            "value5", 50
        );
        
        // Measure execution time
        startTime = System.currentTimeMillis();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        endTime = System.currentTimeMillis();
        
        long executionTime = endTime - startTime;
        
        // Verify execution result
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Rules should pass with valid data");
        
        // Performance assertion - execution should be fast
        assertTrue(executionTime < 1000, 
                  String.format("Execution should complete within 1 second, took %d ms", executionTime));
        
        System.out.printf("Rule execution time: %d ms for 5 rules%n", executionTime);
    }

    @Test
    @DisplayName("Should handle multiple rule references efficiently")
    void testMultipleRuleReferencesPerformance() throws Exception {
        // Create multiple rule files
        for (int fileIndex = 1; fileIndex <= 5; fileIndex++) {
            StringBuilder rulesYamlBuilder = new StringBuilder();
            rulesYamlBuilder.append(String.format("""
                metadata:
                  name: "Rules File %d"
                  version: "1.0.0"
                
                rules:
                """, fileIndex));
            
            // Generate 10 rules per file
            for (int ruleIndex = 1; ruleIndex <= 10; ruleIndex++) {
                int globalRuleId = (fileIndex - 1) * 10 + ruleIndex;
                rulesYamlBuilder.append(String.format("""
                  - id: "rule-file%d-rule%d"
                    name: "Rule %d from File %d"
                    condition: "#value%d > 0"
                    message: "Rule validation failed"
                """, fileIndex, ruleIndex, ruleIndex, fileIndex, globalRuleId));
            }
            
            Path rulesFile = tempDir.resolve(String.format("rules-file-%d.yaml", fileIndex));
            Files.writeString(rulesFile, rulesYamlBuilder.toString());
        }
        
        // Create main configuration with multiple rule references
        StringBuilder mainConfigBuilder = new StringBuilder();
        mainConfigBuilder.append("""
            metadata:
              name: "Multiple References Performance Test"
              version: "1.0.0"
            
            rule-refs:
            """);
        
        for (int fileIndex = 1; fileIndex <= 5; fileIndex++) {
            Path rulesFile = tempDir.resolve(String.format("rules-file-%d.yaml", fileIndex));
            mainConfigBuilder.append(String.format("""
              - name: "rules-file-%d"
                source: "%s"
                enabled: true
                description: "Rules file %d"
            """, fileIndex, rulesFile.toString().replace("\\", "\\\\"), fileIndex));
        }
        
        mainConfigBuilder.append("""
            
            rule-groups:
              - id: "multi-file-group"
                name: "Multi-File Performance Group"
                operator: "AND"
                rule-ids:
                  - "rule-file1-rule1"
                  - "rule-file2-rule1"
                  - "rule-file3-rule1"
                  - "rule-file4-rule1"
                  - "rule-file5-rule1"
            """);
        
        Path mainConfigFile = tempDir.resolve("multi-file-config.yaml");
        Files.writeString(mainConfigFile, mainConfigBuilder.toString());
        
        // Measure loading time with multiple rule references
        long startTime = System.currentTimeMillis();
        YamlRuleConfiguration config = configLoader.loadFromFile(mainConfigFile.toString());
        RulesEngineConfiguration engineConfig = ruleFactory.createRulesEngineConfiguration(config);
        RulesEngine engine = new RulesEngine(engineConfig);
        long endTime = System.currentTimeMillis();
        
        long loadingTime = endTime - startTime;
        
        // Verify configuration was loaded correctly
        assertNotNull(config.getRules(), "Rules should be loaded");
        assertEquals(50, config.getRules().size(), "Should have 50 rules from 5 referenced files");
        assertEquals(1, config.getRuleGroups().size(), "Should have 1 rule group");
        
        // Performance assertion - loading should complete within reasonable time
        assertTrue(loadingTime < 10000, 
                  String.format("Loading should complete within 10 seconds, took %d ms", loadingTime));
        
        System.out.printf("Multiple rule references loading time: %d ms for 5 files with 50 total rules%n", loadingTime);
        
        // Test execution
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("multi-file-group");
        assertNotNull(ruleGroup, "Rule group should be found");
        
        Map<String, Object> testData = Map.of(
            "value1", 10,
            "value11", 20,
            "value21", 30,
            "value31", 40,
            "value41", 50
        );
        
        // Measure execution time
        startTime = System.currentTimeMillis();
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        endTime = System.currentTimeMillis();
        
        long executionTime = endTime - startTime;
        
        // Verify execution result
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Rules should pass with valid data");
        
        // Performance assertion - execution should be fast
        assertTrue(executionTime < 1000, 
                  String.format("Execution should complete within 1 second, took %d ms", executionTime));
        
        System.out.printf("Multi-file rule execution time: %d ms for 5 rules from different files%n", executionTime);
    }

    @Test
    @DisplayName("Should compare performance between inline rules and rule references")
    void testPerformanceComparisonInlineVsReferences() throws Exception {
        // Create inline configuration
        StringBuilder inlineConfigBuilder = new StringBuilder();
        inlineConfigBuilder.append("""
            metadata:
              name: "Inline Rules Configuration"
              version: "1.0.0"
            
            rules:
            """);
        
        // Generate 20 inline rules
        for (int i = 1; i <= 20; i++) {
            inlineConfigBuilder.append(String.format("""
              - id: "inline-rule-%d"
                name: "Inline Rule %d"
                condition: "#value%d > %d"
                message: "Inline rule %d validation failed"
            """, i, i, i, i, i));
        }
        
        inlineConfigBuilder.append("""
            
            rule-groups:
              - id: "inline-group"
                name: "Inline Rules Group"
                operator: "AND"
                rule-ids:
                  - "inline-rule-1"
                  - "inline-rule-2"
                  - "inline-rule-3"
            """);
        
        Path inlineConfigFile = tempDir.resolve("inline-config.yaml");
        Files.writeString(inlineConfigFile, inlineConfigBuilder.toString());
        
        // Create referenced rules configuration
        String referencedRulesYaml = """
            metadata:
              name: "Referenced Rules"
              version: "1.0.0"
            
            rules:
              - id: "ref-rule-1"
                name: "Referenced Rule 1"
                condition: "#value1 > 1"
                message: "Referenced rule 1 validation failed"
              - id: "ref-rule-2"
                name: "Referenced Rule 2"
                condition: "#value2 > 2"
                message: "Referenced rule 2 validation failed"
              - id: "ref-rule-3"
                name: "Referenced Rule 3"
                condition: "#value3 > 3"
                message: "Referenced rule 3 validation failed"
            """;
        
        Path referencedRulesFile = tempDir.resolve("referenced-rules.yaml");
        Files.writeString(referencedRulesFile, referencedRulesYaml);
        
        String referencedConfigYaml = """
            metadata:
              name: "Referenced Rules Configuration"
              version: "1.0.0"
            
            rule-refs:
              - name: "referenced-rules"
                source: "%s"
                enabled: true
            
            rule-groups:
              - id: "referenced-group"
                name: "Referenced Rules Group"
                operator: "AND"
                rule-ids:
                  - "ref-rule-1"
                  - "ref-rule-2"
                  - "ref-rule-3"
            """.formatted(referencedRulesFile.toString().replace("\\", "\\\\"));
        
        Path referencedConfigFile = tempDir.resolve("referenced-config.yaml");
        Files.writeString(referencedConfigFile, referencedConfigYaml);
        
        // Measure inline loading time
        long startTime = System.currentTimeMillis();
        YamlRuleConfiguration inlineConfig = configLoader.loadFromFile(inlineConfigFile.toString());
        RulesEngineConfiguration inlineEngineConfig = ruleFactory.createRulesEngineConfiguration(inlineConfig);
        RulesEngine inlineEngine = new RulesEngine(inlineEngineConfig);
        long inlineLoadTime = System.currentTimeMillis() - startTime;
        
        // Measure referenced loading time
        startTime = System.currentTimeMillis();
        YamlRuleConfiguration referencedConfig = configLoader.loadFromFile(referencedConfigFile.toString());
        RulesEngineConfiguration referencedEngineConfig = ruleFactory.createRulesEngineConfiguration(referencedConfig);
        RulesEngine referencedEngine = new RulesEngine(referencedEngineConfig);
        long referencedLoadTime = System.currentTimeMillis() - startTime;
        
        System.out.printf("Inline loading time: %d ms%n", inlineLoadTime);
        System.out.printf("Referenced loading time: %d ms%n", referencedLoadTime);
        
        // Referenced loading should not be significantly slower (allow 3x overhead)
        assertTrue(referencedLoadTime < inlineLoadTime * 3, 
                  String.format("Referenced loading (%d ms) should not be more than 3x slower than inline (%d ms)", 
                               referencedLoadTime, inlineLoadTime));
        
        // Both configurations should work correctly
        assertEquals(20, inlineConfig.getRules().size(), "Inline config should have 20 rules");
        assertEquals(3, referencedConfig.getRules().size(), "Referenced config should have 3 rules");
    }
}
