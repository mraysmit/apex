package dev.mars.apex.core.integration;

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.YamlConfigurationMerger;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.YamlRuleFactory;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Disabled;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to validate external data source references work with file system loading.
 * 
 * This test validates the complete fix for the original issue where APEX could resolve
 * file references from the file system but not when using createRulesEngineFromMultipleFiles
 * with external data source references.
 */
@DisplayName("External Data Source File System Integration Test")
public class ExternalDataSourceFileSystemIntegrationTest {

    @TempDir
    Path tempDir;

    private YamlConfigurationLoader configLoader;
    private YamlRuleFactory ruleFactory;

    @BeforeEach
    void setUp() {
        this.configLoader = new YamlConfigurationLoader();
        this.ruleFactory = new YamlRuleFactory();
    }

    @Test
    @DisplayName("Should resolve external data source references when loading from file system")
    void testExternalDataSourceReferencesWithFileSystemLoading() throws Exception {
        // Create external data source configuration using standard APEX format
        String externalDataSourceConfig = """
            metadata:
              id: "customer-database-config"
              name: "customer-database"
              type: "external-data-config"
              version: "1.0.0"
              description: "Customer database configuration"
            data-sources:
              - name: "customer-database"
                type: "database"
                source-type: "h2"
                enabled: true
                connection:
                  database: "customer_db"
                  username: "sa"
                  password: ""
                queries:
                  getCustomerById: "SELECT * FROM customers WHERE id = :id"
                  getCustomerByEmail: "SELECT * FROM customers WHERE email = :email"
                cache:
                  enabled: true
                  ttlSeconds: 300
            """;

        // Write files to temporary directory
        Path externalDataSourceFile = tempDir.resolve("customer-database.yaml");
        Path mainConfigFile = tempDir.resolve("main-config.yaml");
        
        Files.writeString(externalDataSourceFile, externalDataSourceConfig);

        // Create main configuration that references the external data source
        // Use absolute path to ensure resolution works in test environment
        String mainConfig = """
            metadata:
              name: "Customer Processing Rules"
              version: "1.0.0"
              description: "Rules for customer data processing with external data sources"
            
            # Reference to external data source configuration
            data-source-refs:
              - name: "customer-db"
                source: "%s"
            
            rules:
              - id: "customer-exists"
                name: "Customer Exists Check"
                condition: "#customerId != null"
                message: "Customer ID is required"
                severity: "ERROR"
                priority: 1
            
            rule-groups:
              - id: "customer-validation"
                name: "Customer Validation Group"
                operator: "AND"
                rule-ids:
                  - "customer-exists"
            """.formatted(externalDataSourceFile.toString().replace("\\", "\\\\"));

        Files.writeString(mainConfigFile, mainConfig);

        // Test: Load configuration and create engine (non-deprecated pattern)
        YamlRuleConfiguration yamlConfig = configLoader.loadFromFile(mainConfigFile.toString());
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);
        RulesEngine engine = new RulesEngine(config);

        // Verify the engine was created successfully
        assertNotNull(engine, "Rules engine should be created successfully");
        assertNotNull(engine.getConfiguration(), "Engine configuration should not be null");

        // Verify rule group exists and can be executed
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("customer-validation");
        assertNotNull(ruleGroup, "Rule group should be found");
        assertEquals("customer-validation", ruleGroup.getId());

        // Test rule execution
        Map<String, Object> testData = Map.of("customerId", "CUST001");
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Rule should pass with valid customer ID");

        // Test with invalid data
        Map<String, Object> invalidData = Map.of("otherField", "value");
        RuleResult invalidResult = engine.executeRuleGroupsList(List.of(ruleGroup), invalidData);
        
        assertNotNull(invalidResult, "Rule result should not be null");
        assertFalse(invalidResult.isTriggered(), "Rule should fail without customer ID");
    }

    @Test
    @DisplayName("Should handle multiple files with external data source references")
    void testMultipleFilesWithExternalDataSourceReferences() throws Exception {
        // External data source using standard APEX format
        String externalDataSource = """
            metadata:
              id: "product-database-config"
              name: "product-database"
              type: "external-data-config"
              version: "1.0.0"
            data-sources:
              - name: "product-database"
                type: "database"
                source-type: "h2"
                enabled: true
            """;

        // Write all files
        Path externalDataSourceFile = tempDir.resolve("product-database.yaml");
        Path rulesConfigFile = tempDir.resolve("rules.yaml");
        Path ruleGroupsConfigFile = tempDir.resolve("rule-groups.yaml");
        
        Files.writeString(externalDataSourceFile, externalDataSource);

        // Rules file with external data source reference
        // Use absolute path
        String rulesFile = """
            metadata:
              name: "Product Rules"
              version: "1.0.0"
            
            data-source-refs:
              - name: "product-db"
                source: "%s"
            
            rules:
              - id: "product-validation"
                name: "Product Validation"
                condition: "#productId != null && #productId.length() > 0"
                message: "Product ID is required"
                severity: "ERROR"
                priority: 1
            """.formatted(externalDataSourceFile.toString().replace("\\", "\\\\"));

        // Rule groups file
        String ruleGroupsFile = """
            metadata:
              name: "Product Rule Groups"
              version: "1.0.0"
            
            rule-groups:
              - id: "product-processing"
                name: "Product Processing Group"
                operator: "AND"
                rule-ids:
                  - "product-validation"
            """;

        Files.writeString(rulesConfigFile, rulesFile);
        Files.writeString(ruleGroupsConfigFile, ruleGroupsFile);

        // Load multiple files - merge manually then create engine
        YamlRuleConfiguration rulesConfig = configLoader.loadFromFileWithoutValidation(rulesConfigFile.toString());
        YamlRuleConfiguration groupsConfig = configLoader.loadFromFileWithoutValidation(ruleGroupsConfigFile.toString());
        
        // Merge YAML configurations using YamlConfigurationMerger
        YamlRuleConfiguration merged = new YamlRuleConfiguration();
        YamlConfigurationMerger.merge(merged, rulesConfig);
        YamlConfigurationMerger.merge(merged, groupsConfig);
        
        // Process references and validate the merged configuration
        configLoader.processReferencesAndValidate(merged);
        
        // Create engine from merged configuration
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(merged);
        RulesEngine engine = new RulesEngine(config);

        // Verify successful loading and execution
        assertNotNull(engine, "Rules engine should be created successfully");
        
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("product-processing");
        assertNotNull(ruleGroup, "Rule group should be found");

        // Test execution
        Map<String, Object> testData = Map.of("productId", "PROD123");
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        
        assertTrue(result.isTriggered(), "Rule should pass with valid product ID");
    }
}
