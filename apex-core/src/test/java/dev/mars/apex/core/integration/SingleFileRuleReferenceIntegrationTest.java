package dev.mars.apex.core.integration;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRuleFactory;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.model.RuleGroup;
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
 * Integration tests for single-file rule reference resolution.
 * 
 * Tests complete end-to-end functionality of loading YAML files with
 * rule references and executing them through the rules engine.
 */
@DisplayName("Single-File Rule Reference Integration Tests")
class SingleFileRuleReferenceIntegrationTest {

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
    @DisplayName("Should load and execute rules from referenced file")
    void testSingleFileRuleReferenceExecution() throws Exception {
        // Create referenced rule file
        String referencedRulesYaml = """
            metadata:
              name: "Customer Rules"
              version: "1.0.0"
            
            rules:
              - id: "age-validation"
                name: "Age Validation"
                condition: "#age != null && #age >= 18"
                message: "Customer must be 18 or older"
                severity: "ERROR"
                priority: 1
              - id: "email-validation"
                name: "Email Validation"
                condition: "#email != null && #email.contains('@')"
                message: "Valid email address required"
                severity: "ERROR"
                priority: 2
            """;
        
        Path referencedFile = tempDir.resolve("customer-rules.yaml");
        Files.writeString(referencedFile, referencedRulesYaml);
        
        // Create main configuration with rule reference and rule group
        String mainConfigYaml = """
            metadata:
              name: "Main Configuration"
              version: "1.0.0"
            
            rule-refs:
              - name: "customer-rules"
                source: "%s"
                enabled: true
                description: "Customer validation rules"
            
            rule-groups:
              - id: "customer-validation"
                name: "Customer Validation Group"
                operator: "AND"
                rule-ids:
                  - "age-validation"
                  - "email-validation"
            """.formatted(referencedFile.toString().replace("\\", "\\\\"));
        
        Path mainConfigFile = tempDir.resolve("main-config.yaml");
        Files.writeString(mainConfigFile, mainConfigYaml);
        
        // Load configuration and create rules engine
        YamlRuleConfiguration config = configLoader.loadFromFile(mainConfigFile.toString());
        RulesEngineConfiguration engineConfig = ruleFactory.createRulesEngineConfiguration(config);
        RulesEngine engine = new RulesEngine(engineConfig);

        // Verify configuration was loaded correctly
        assertNotNull(config.getRules(), "Rules should be loaded from referenced file");
        assertEquals(2, config.getRules().size(), "Should have 2 rules from referenced file");
        assertEquals(1, config.getRuleGroups().size(), "Should have 1 rule group");

        // Get the rule group for execution
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("customer-validation");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Test with valid data (should pass both rules)
        Map<String, Object> validData = Map.of(
            "age", 25,
            "email", "test@example.com"
        );

        RuleResult validResult = engine.executeRuleGroupsList(List.of(ruleGroup), validData);
        assertNotNull(validResult, "Rule result should not be null");
        assertTrue(validResult.isTriggered(), "Rules should pass with valid data");

        // Test with invalid age (should fail)
        Map<String, Object> invalidAgeData = Map.of(
            "age", 16,
            "email", "test@example.com"
        );

        RuleResult invalidAgeResult = engine.executeRuleGroupsList(List.of(ruleGroup), invalidAgeData);
        assertNotNull(invalidAgeResult, "Rule result should not be null");
        assertFalse(invalidAgeResult.isTriggered(), "Rules should fail with invalid age");

        // Test with invalid email (should fail)
        Map<String, Object> invalidEmailData = Map.of(
            "age", 25,
            "email", "invalid-email"
        );

        RuleResult invalidEmailResult = engine.executeRuleGroupsList(List.of(ruleGroup), invalidEmailData);
        assertNotNull(invalidEmailResult, "Rule result should not be null");
        assertFalse(invalidEmailResult.isTriggered(), "Rules should fail with invalid email");
    }

    @Test
    @DisplayName("Should handle multiple rule references in single file")
    void testMultipleRuleReferencesInSingleFile() throws Exception {
        // Create first referenced rule file
        String customerRulesYaml = """
            metadata:
              name: "Customer Rules"
              version: "1.0.0"
            
            rules:
              - id: "customer-age-check"
                name: "Customer Age Check"
                condition: "#age >= 18"
                message: "Must be 18 or older"
            """;
        
        // Create second referenced rule file
        String productRulesYaml = """
            metadata:
              name: "Product Rules"
              version: "1.0.0"
            
            rules:
              - id: "product-price-check"
                name: "Product Price Check"
                condition: "#price > 0"
                message: "Price must be positive"
            """;
        
        Path customerRulesFile = tempDir.resolve("customer-rules.yaml");
        Path productRulesFile = tempDir.resolve("product-rules.yaml");
        Files.writeString(customerRulesFile, customerRulesYaml);
        Files.writeString(productRulesFile, productRulesYaml);
        
        // Create main configuration with multiple rule references
        String mainConfigYaml = """
            metadata:
              name: "Multi-Reference Configuration"
              version: "1.0.0"
            
            rule-refs:
              - name: "customer-rules"
                source: "%s"
                enabled: true
              - name: "product-rules"
                source: "%s"
                enabled: true
            
            rule-groups:
              - id: "combined-validation"
                name: "Combined Validation Group"
                operator: "AND"
                rule-ids:
                  - "customer-age-check"
                  - "product-price-check"
            """.formatted(
                customerRulesFile.toString().replace("\\", "\\\\"),
                productRulesFile.toString().replace("\\", "\\\\")
            );
        
        Path mainConfigFile = tempDir.resolve("main-config.yaml");
        Files.writeString(mainConfigFile, mainConfigYaml);
        
        // Load configuration and create rules engine
        YamlRuleConfiguration config = configLoader.loadFromFile(mainConfigFile.toString());
        RulesEngineConfiguration engineConfig = ruleFactory.createRulesEngineConfiguration(config);
        RulesEngine engine = new RulesEngine(engineConfig);

        // Verify configuration
        assertNotNull(config.getRules(), "Rules should be loaded");
        assertEquals(2, config.getRules().size(), "Should have 2 rules from both referenced files");

        // Get the rule group for execution
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("combined-validation");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Test execution
        Map<String, Object> testData = Map.of(
            "age", 25,
            "price", 99.99
        );

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Rules should pass with valid data");
    }

    @Test
    @DisplayName("Should handle mixed inline rules and rule references")
    void testMixedInlineRulesAndReferences() throws Exception {
        // Create referenced rule file
        String referencedRulesYaml = """
            metadata:
              name: "External Rules"
              version: "1.0.0"
            
            rules:
              - id: "external-rule"
                name: "External Rule"
                condition: "#external_value > 0"
                message: "External value must be positive"
            """;
        
        Path referencedFile = tempDir.resolve("external-rules.yaml");
        Files.writeString(referencedFile, referencedRulesYaml);
        
        // Create main configuration with both inline rules and rule references
        String mainConfigYaml = """
            metadata:
              name: "Mixed Configuration"
              version: "1.0.0"
            
            # Inline rules
            rules:
              - id: "inline-rule"
                name: "Inline Rule"
                condition: "#inline_value != null"
                message: "Inline value is required"
            
            # Rule references
            rule-refs:
              - name: "external-rules"
                source: "%s"
                enabled: true
            
            rule-groups:
              - id: "mixed-validation"
                name: "Mixed Validation Group"
                operator: "AND"
                rule-ids:
                  - "inline-rule"
                  - "external-rule"
            """.formatted(referencedFile.toString().replace("\\", "\\\\"));
        
        Path mainConfigFile = tempDir.resolve("main-config.yaml");
        Files.writeString(mainConfigFile, mainConfigYaml);
        
        // Load configuration and create rules engine
        YamlRuleConfiguration config = configLoader.loadFromFile(mainConfigFile.toString());
        RulesEngineConfiguration engineConfig = ruleFactory.createRulesEngineConfiguration(config);
        RulesEngine engine = new RulesEngine(engineConfig);

        // Verify configuration
        assertNotNull(config.getRules(), "Rules should be loaded");
        assertEquals(2, config.getRules().size(), "Should have both inline and referenced rules");

        // Verify rule IDs
        assertTrue(config.getRules().stream().anyMatch(r -> "inline-rule".equals(r.getId())),
                  "Should contain inline rule");
        assertTrue(config.getRules().stream().anyMatch(r -> "external-rule".equals(r.getId())),
                  "Should contain external rule");

        // Get the rule group for execution
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("mixed-validation");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Test execution
        Map<String, Object> testData = Map.of(
            "inline_value", "test",
            "external_value", 42
        );

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Rules should pass with valid data");
    }
}
