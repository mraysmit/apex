package dev.mars.apex.core.integration;

import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
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
 * Integration tests for multi-file rule reference resolution.
 * 
 * Tests the createRulesEngineFromMultipleFiles() method with rule references
 * across multiple files to ensure proper merging and validation.
 */
@DisplayName("Multi-File Rule Reference Integration Tests")
class MultiFileRuleReferenceIntegrationTest {

    private YamlRulesEngineService rulesEngineService;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        rulesEngineService = new YamlRulesEngineService();
    }

    @Test
    @DisplayName("Should handle rules and rule groups in separate files")
    void testRulesAndRuleGroupsInSeparateFiles() throws Exception {
        // Create rules file
        String rulesYaml = """
            metadata:
              name: "Customer Rules"
              version: "1.0.0"
            
            rules:
              - id: "age-validation"
                name: "Age Validation"
                condition: "#age >= 18"
                message: "Must be 18 or older"
                severity: "ERROR"
                priority: 1
              - id: "email-validation"
                name: "Email Validation"
                condition: "#email != null && #email.contains('@')"
                message: "Valid email required"
                severity: "ERROR"
                priority: 2
            """;
        
        // Create rule groups file
        String ruleGroupsYaml = """
            metadata:
              name: "Customer Rule Groups"
              version: "1.0.0"
            
            rule-groups:
              - id: "customer-validation"
                name: "Customer Validation Group"
                operator: "AND"
                rule-ids:
                  - "age-validation"
                  - "email-validation"
            """;
        
        Path rulesFile = tempDir.resolve("rules.yaml");
        Path ruleGroupsFile = tempDir.resolve("rule-groups.yaml");
        Files.writeString(rulesFile, rulesYaml);
        Files.writeString(ruleGroupsFile, ruleGroupsYaml);
        
        // Create rules engine from multiple files
        RulesEngine engine = rulesEngineService.createRulesEngineFromMultipleFiles(
            rulesFile.toString(),
            ruleGroupsFile.toString()
        );
        
        // Verify engine was created successfully
        assertNotNull(engine, "Rules engine should be created");
        
        // Get the rule group
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("customer-validation");
        assertNotNull(ruleGroup, "Rule group should be found");
        
        // Test with valid data
        Map<String, Object> validData = Map.of(
            "age", 25,
            "email", "test@example.com"
        );
        
        RuleResult validResult = engine.executeRuleGroupsList(List.of(ruleGroup), validData);
        assertNotNull(validResult, "Rule result should not be null");
        assertTrue(validResult.isTriggered(), "Rules should pass with valid data");
        
        // Test with invalid data
        Map<String, Object> invalidData = Map.of(
            "age", 16,
            "email", "invalid-email"
        );
        
        RuleResult invalidResult = engine.executeRuleGroupsList(List.of(ruleGroup), invalidData);
        assertNotNull(invalidResult, "Rule result should not be null");
        assertFalse(invalidResult.isTriggered(), "Rules should fail with invalid data");
    }

    @Test
    @DisplayName("Should handle rule references across multiple files")
    void testRuleReferencesAcrossMultipleFiles() throws Exception {
        // Create external rules file
        String externalRulesYaml = """
            metadata:
              name: "External Rules"
              version: "1.0.0"
            
            rules:
              - id: "external-rule-1"
                name: "External Rule 1"
                condition: "#value1 > 0"
                message: "Value 1 must be positive"
              - id: "external-rule-2"
                name: "External Rule 2"
                condition: "#value2 != null"
                message: "Value 2 is required"
            """;
        
        // Create main configuration with rule references
        String mainConfigYaml = """
            metadata:
              name: "Main Configuration"
              version: "1.0.0"
            
            rule-refs:
              - name: "external-rules"
                source: "%s"
                enabled: true
                description: "External validation rules"
            
            rules:
              - id: "local-rule"
                name: "Local Rule"
                condition: "#local_value == 'valid'"
                message: "Local value must be 'valid'"
            
            rule-groups:
              - id: "combined-validation"
                name: "Combined Validation Group"
                operator: "AND"
                rule-ids:
                  - "external-rule-1"
                  - "external-rule-2"
                  - "local-rule"
            """;
        
        Path externalRulesFile = tempDir.resolve("external-rules.yaml");
        Files.writeString(externalRulesFile, externalRulesYaml);
        
        // Format the main config with the external file path
        String formattedMainConfig = mainConfigYaml.formatted(
            externalRulesFile.toString().replace("\\", "\\\\")
        );
        
        Path mainConfigFile = tempDir.resolve("main-config.yaml");
        Files.writeString(mainConfigFile, formattedMainConfig);
        
        // Create rules engine from main config file (which references external rules)
        RulesEngine engine = rulesEngineService.createRulesEngineFromFile(mainConfigFile.toString());
        
        // Verify engine was created successfully
        assertNotNull(engine, "Rules engine should be created");
        
        // Get the rule group
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("combined-validation");
        assertNotNull(ruleGroup, "Rule group should be found");
        
        // Test with valid data
        Map<String, Object> validData = Map.of(
            "value1", 42,
            "value2", "test",
            "local_value", "valid"
        );
        
        RuleResult validResult = engine.executeRuleGroupsList(List.of(ruleGroup), validData);
        assertNotNull(validResult, "Rule result should not be null");
        assertTrue(validResult.isTriggered(), "Rules should pass with valid data");
        
        // Test with invalid data (missing value2)
        Map<String, Object> invalidData = Map.of(
            "value1", 42,
            "local_value", "valid"
            // value2 is missing
        );
        
        RuleResult invalidResult = engine.executeRuleGroupsList(List.of(ruleGroup), invalidData);
        assertNotNull(invalidResult, "Rule result should not be null");
        assertFalse(invalidResult.isTriggered(), "Rules should fail with missing value2");
    }

    @Test
    @DisplayName("Should handle complex multi-file scenario with mixed references")
    void testComplexMultiFileScenario() throws Exception {
        // Create customer rules file
        String customerRulesYaml = """
            metadata:
              name: "Customer Rules"
              version: "1.0.0"
            
            rules:
              - id: "customer-age"
                name: "Customer Age Check"
                condition: "#age >= 18"
                message: "Customer must be 18 or older"
            """;
        
        // Create product rules file
        String productRulesYaml = """
            metadata:
              name: "Product Rules"
              version: "1.0.0"
            
            rules:
              - id: "product-price"
                name: "Product Price Check"
                condition: "#price > 0"
                message: "Product price must be positive"
            """;
        
        // Create main configuration file with rule references
        String mainConfigYaml = """
            metadata:
              name: "E-commerce Configuration"
              version: "1.0.0"
            
            rule-refs:
              - name: "customer-rules"
                source: "%s"
                enabled: true
              - name: "product-rules"
                source: "%s"
                enabled: true
            
            rules:
              - id: "order-total"
                name: "Order Total Check"
                condition: "#total >= 10.00"
                message: "Minimum order total is $10.00"
            """;
        
        // Create rule groups file
        String ruleGroupsYaml = """
            metadata:
              name: "E-commerce Rule Groups"
              version: "1.0.0"
            
            rule-groups:
              - id: "order-validation"
                name: "Order Validation Group"
                operator: "AND"
                rule-ids:
                  - "customer-age"
                  - "product-price"
                  - "order-total"
            """;
        
        Path customerRulesFile = tempDir.resolve("customer-rules.yaml");
        Path productRulesFile = tempDir.resolve("product-rules.yaml");
        Path ruleGroupsFile = tempDir.resolve("rule-groups.yaml");
        
        Files.writeString(customerRulesFile, customerRulesYaml);
        Files.writeString(productRulesFile, productRulesYaml);
        Files.writeString(ruleGroupsFile, ruleGroupsYaml);
        
        // Format the main config with the external file paths
        String formattedMainConfig = mainConfigYaml.formatted(
            customerRulesFile.toString().replace("\\", "\\\\"),
            productRulesFile.toString().replace("\\", "\\\\")
        );
        
        Path mainConfigFile = tempDir.resolve("main-config.yaml");
        Files.writeString(mainConfigFile, formattedMainConfig);
        
        // Create rules engine from multiple files
        RulesEngine engine = rulesEngineService.createRulesEngineFromMultipleFiles(
            mainConfigFile.toString(),
            ruleGroupsFile.toString()
        );
        
        // Verify engine was created successfully
        assertNotNull(engine, "Rules engine should be created");
        
        // Get the rule group
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("order-validation");
        assertNotNull(ruleGroup, "Rule group should be found");
        
        // Test with valid order data
        Map<String, Object> validOrder = Map.of(
            "age", 25,
            "price", 29.99,
            "total", 29.99
        );
        
        RuleResult validResult = engine.executeRuleGroupsList(List.of(ruleGroup), validOrder);
        assertNotNull(validResult, "Rule result should not be null");
        assertTrue(validResult.isTriggered(), "Rules should pass with valid order");
        
        // Test with invalid order (under minimum total)
        Map<String, Object> invalidOrder = Map.of(
            "age", 25,
            "price", 5.00,
            "total", 5.00
        );
        
        RuleResult invalidResult = engine.executeRuleGroupsList(List.of(ruleGroup), invalidOrder);
        assertNotNull(invalidResult, "Rule result should not be null");
        assertFalse(invalidResult.isTriggered(), "Rules should fail with order under minimum");
    }
}
