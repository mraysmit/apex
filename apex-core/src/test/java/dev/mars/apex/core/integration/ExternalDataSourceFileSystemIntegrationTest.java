package dev.mars.apex.core.integration;

import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
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

    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        this.rulesEngineService = new YamlRulesEngineService();
    }

    @Test
    @DisplayName("Should resolve external data source references when loading from file system")
    void testExternalDataSourceReferencesWithFileSystemLoading() throws Exception {
        // Create external data source configuration
        String externalDataSourceConfig = """
            apiVersion: "apex.dev/v1"
            kind: "DataSource"
            metadata:
              name: "customer-database"
              version: "1.0.0"
              description: "Customer database configuration"
            spec:
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

        // Create main configuration that references the external data source
        String mainConfig = """
            metadata:
              name: "Customer Processing Rules"
              version: "1.0.0"
              description: "Rules for customer data processing with external data sources"
            
            # Reference to external data source configuration
            external-data-sources:
              - name: "customer-db"
                type: "external-data-config"
                source: "customer-database.yaml"
            
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
            """;

        // Write files to temporary directory
        Path externalDataSourceFile = tempDir.resolve("customer-database.yaml");
        Path mainConfigFile = tempDir.resolve("main-config.yaml");
        
        Files.writeString(externalDataSourceFile, externalDataSourceConfig);
        Files.writeString(mainConfigFile, mainConfig);

        // Test: Load using createRulesEngineFromMultipleFiles (this was the failing scenario)
        RulesEngine engine = rulesEngineService.createRulesEngineFromMultipleFiles(
            mainConfigFile.toString()
        );

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
        // External data source
        String externalDataSource = """
            apiVersion: "apex.dev/v1"
            kind: "DataSource"
            metadata:
              name: "product-database"
              version: "1.0.0"
            spec:
              type: "database"
              source-type: "h2"
              enabled: true
            """;

        // Rules file with external data source reference
        String rulesFile = """
            metadata:
              name: "Product Rules"
              version: "1.0.0"
            
            external-data-sources:
              - name: "product-db"
                type: "external-data-config"
                source: "product-database.yaml"
            
            rules:
              - id: "product-validation"
                name: "Product Validation"
                condition: "#productId != null && #productId.length() > 0"
                message: "Product ID is required"
                severity: "ERROR"
                priority: 1
            """;

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

        // Write all files
        Path externalDataSourceFile = tempDir.resolve("product-database.yaml");
        Path rulesConfigFile = tempDir.resolve("rules.yaml");
        Path ruleGroupsConfigFile = tempDir.resolve("rule-groups.yaml");
        
        Files.writeString(externalDataSourceFile, externalDataSource);
        Files.writeString(rulesConfigFile, rulesFile);
        Files.writeString(ruleGroupsConfigFile, ruleGroupsFile);

        // Load multiple files - this should resolve external data source references correctly
        RulesEngine engine = rulesEngineService.createRulesEngineFromMultipleFiles(
            rulesConfigFile.toString(),
            ruleGroupsConfigFile.toString()
        );

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
