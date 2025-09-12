package dev.mars.apex.core.config.yaml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for rule reference processing in YamlConfigurationLoader.
 * 
 * Tests the processRuleReferences() method with various scenarios including
 * enabled/disabled references, missing files, and invalid syntax.
 */
@DisplayName("YamlConfigurationLoader Rule References Unit Tests")
class YamlConfigurationLoaderRuleReferencesTest {

    private YamlConfigurationLoader loader;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("Should handle configuration with no rule references")
    void testNoRuleReferences() throws Exception {
        YamlRuleConfiguration config = new YamlRuleConfiguration();
        
        // Should not throw exception and should not modify configuration
        assertDoesNotThrow(() -> loader.processReferencesAndValidate(config));
        assertNull(config.getRuleRefs(), "Rule refs should remain null");
        assertNull(config.getRules(), "Rules should remain null");
    }

    @Test
    @DisplayName("Should handle configuration with empty rule references list")
    void testEmptyRuleReferencesList() throws Exception {
        YamlRuleConfiguration config = new YamlRuleConfiguration();
        config.setRuleRefs(new ArrayList<>());
        
        // Should not throw exception and should not modify configuration
        assertDoesNotThrow(() -> loader.processReferencesAndValidate(config));
        assertTrue(config.getRuleRefs().isEmpty(), "Rule refs should remain empty");
        assertNull(config.getRules(), "Rules should remain null");
    }

    @Test
    @DisplayName("Should process enabled rule reference from file system")
    void testEnabledRuleReferenceFromFileSystem() throws Exception {
        // Create referenced rule file
        String referencedRulesYaml = """
            metadata:
              name: "Referenced Rules"
              version: "1.0.0"
            
            rules:
              - id: "test-rule-1"
                name: "Test Rule 1"
                condition: "#value > 0"
                message: "Value must be positive"
              - id: "test-rule-2"
                name: "Test Rule 2"
                condition: "#name != null"
                message: "Name is required"
            """;
        
        Path referencedFile = tempDir.resolve("referenced-rules.yaml");
        Files.writeString(referencedFile, referencedRulesYaml);
        
        // Create main configuration with rule reference
        YamlRuleConfiguration config = new YamlRuleConfiguration();
        List<YamlRuleRef> ruleRefs = new ArrayList<>();
        ruleRefs.add(new YamlRuleRef("test-rules", referencedFile.toString(), true, "Test rules"));
        config.setRuleRefs(ruleRefs);
        
        // Process rule references
        loader.processReferencesAndValidate(config);
        
        // Verify rules were merged
        assertNotNull(config.getRules(), "Rules should not be null after processing");
        assertEquals(2, config.getRules().size(), "Should have 2 rules from referenced file");
        
        // Verify rule details
        YamlRule rule1 = config.getRules().get(0);
        assertEquals("test-rule-1", rule1.getId(), "First rule ID should match");
        assertEquals("Test Rule 1", rule1.getName(), "First rule name should match");
        
        YamlRule rule2 = config.getRules().get(1);
        assertEquals("test-rule-2", rule2.getId(), "Second rule ID should match");
        assertEquals("Test Rule 2", rule2.getName(), "Second rule name should match");
    }

    @Test
    @DisplayName("Should skip disabled rule reference")
    void testDisabledRuleReference() throws Exception {
        // Create referenced rule file
        String referencedRulesYaml = """
            metadata:
              name: "Referenced Rules"
              version: "1.0.0"
            
            rules:
              - id: "disabled-rule"
                name: "Disabled Rule"
                condition: "#value > 0"
                message: "This should not be loaded"
            """;
        
        Path referencedFile = tempDir.resolve("disabled-rules.yaml");
        Files.writeString(referencedFile, referencedRulesYaml);
        
        // Create main configuration with disabled rule reference
        YamlRuleConfiguration config = new YamlRuleConfiguration();
        List<YamlRuleRef> ruleRefs = new ArrayList<>();
        ruleRefs.add(new YamlRuleRef("disabled-rules", referencedFile.toString(), false, "Disabled rules"));
        config.setRuleRefs(ruleRefs);
        
        // Process rule references
        loader.processReferencesAndValidate(config);
        
        // Verify no rules were merged
        assertNull(config.getRules(), "Rules should remain null when reference is disabled");
    }

    @Test
    @DisplayName("Should merge rules with existing rules")
    void testMergeWithExistingRules() throws Exception {
        // Create referenced rule file
        String referencedRulesYaml = """
            metadata:
              name: "Referenced Rules"
              version: "1.0.0"
            
            rules:
              - id: "referenced-rule"
                name: "Referenced Rule"
                condition: "#value > 0"
                message: "Referenced rule message"
            """;
        
        Path referencedFile = tempDir.resolve("referenced-rules.yaml");
        Files.writeString(referencedFile, referencedRulesYaml);
        
        // Create main configuration with existing rule
        YamlRuleConfiguration config = new YamlRuleConfiguration();
        List<YamlRule> existingRules = new ArrayList<>();
        YamlRule existingRule = new YamlRule();
        existingRule.setId("existing-rule");
        existingRule.setName("Existing Rule");
        existingRule.setCondition("#existing == true");
        existingRule.setMessage("Existing rule message");
        existingRules.add(existingRule);
        config.setRules(existingRules);
        
        // Add rule reference
        List<YamlRuleRef> ruleRefs = new ArrayList<>();
        ruleRefs.add(new YamlRuleRef("referenced-rules", referencedFile.toString()));
        config.setRuleRefs(ruleRefs);
        
        // Process rule references
        loader.processReferencesAndValidate(config);
        
        // Verify both rules exist
        assertNotNull(config.getRules(), "Rules should not be null");
        assertEquals(2, config.getRules().size(), "Should have both existing and referenced rules");
        
        // Verify existing rule is still there
        assertEquals("existing-rule", config.getRules().get(0).getId(), "Existing rule should be preserved");
        
        // Verify referenced rule was added
        assertEquals("referenced-rule", config.getRules().get(1).getId(), "Referenced rule should be added");
    }

    @Test
    @DisplayName("Should throw exception for missing rule file")
    void testMissingRuleFile() {
        YamlRuleConfiguration config = new YamlRuleConfiguration();
        List<YamlRuleRef> ruleRefs = new ArrayList<>();
        ruleRefs.add(new YamlRuleRef("missing-rules", "non-existent-file.yaml"));
        config.setRuleRefs(ruleRefs);
        
        // Should throw YamlConfigurationException
        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> loader.processReferencesAndValidate(config),
            "Should throw exception for missing file"
        );
        
        assertTrue(exception.getMessage().contains("missing-rules"), 
                  "Exception message should contain reference name");
        assertTrue(exception.getMessage().contains("non-existent-file.yaml"), 
                  "Exception message should contain file path");
    }

    @Test
    @DisplayName("Should handle multiple rule references")
    void testMultipleRuleReferences() throws Exception {
        // Create first referenced rule file
        String rules1Yaml = """
            metadata:
              name: "Rules Set 1"
              version: "1.0.0"
            
            rules:
              - id: "rule-1"
                name: "Rule 1"
                condition: "#value1 > 0"
                message: "Rule 1 message"
            """;
        
        // Create second referenced rule file
        String rules2Yaml = """
            metadata:
              name: "Rules Set 2"
              version: "1.0.0"
            
            rules:
              - id: "rule-2"
                name: "Rule 2"
                condition: "#value2 > 0"
                message: "Rule 2 message"
              - id: "rule-3"
                name: "Rule 3"
                condition: "#value3 > 0"
                message: "Rule 3 message"
            """;
        
        Path rules1File = tempDir.resolve("rules1.yaml");
        Path rules2File = tempDir.resolve("rules2.yaml");
        Files.writeString(rules1File, rules1Yaml);
        Files.writeString(rules2File, rules2Yaml);
        
        // Create main configuration with multiple rule references
        YamlRuleConfiguration config = new YamlRuleConfiguration();
        List<YamlRuleRef> ruleRefs = new ArrayList<>();
        ruleRefs.add(new YamlRuleRef("rules-1", rules1File.toString()));
        ruleRefs.add(new YamlRuleRef("rules-2", rules2File.toString()));
        config.setRuleRefs(ruleRefs);
        
        // Process rule references
        loader.processReferencesAndValidate(config);
        
        // Verify all rules were merged
        assertNotNull(config.getRules(), "Rules should not be null");
        assertEquals(3, config.getRules().size(), "Should have 3 rules from both files");
        
        // Verify rule IDs
        assertEquals("rule-1", config.getRules().get(0).getId(), "First rule should be from first file");
        assertEquals("rule-2", config.getRules().get(1).getId(), "Second rule should be from second file");
        assertEquals("rule-3", config.getRules().get(2).getId(), "Third rule should be from second file");
    }
}
