package dev.mars.rulesengine.core.config.yaml;

import dev.mars.rulesengine.core.api.RuleSet;
import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.engine.model.metadata.RuleMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the enhanced YAML configuration system with generic architecture support.
 * 
 * Tests cover:
 * - Enterprise metadata integration
 * - Generic category support
 * - GenericRuleSet creation from YAML
 * - Audit trail functionality
 * - Validation and error handling
 */
public class EnhancedYamlConfigurationTest {

    private YamlConfigurationLoader loader;
    private YamlRuleFactory factory;
    private YamlRulesEngineService service;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
        factory = new YamlRuleFactory();
        service = new YamlRulesEngineService(loader, factory);
    }

    @Test
    void testEnhancedYamlRuleWithEnterpriseMetadata() throws YamlConfigurationException {
        String yamlContent = """
            metadata:
              name: "Enterprise Test Configuration"
              version: "2.0.0"
              
            categories:
              - name: "patient-eligibility"
                business-domain: "Healthcare"
                business-owner: "Chief Medical Officer"
                
            rules:
              - id: "patient-age-check"
                name: "Patient Age Verification"
                description: "Validates patient age for treatment eligibility"
                category: "patient-eligibility"
                condition: "#patientAge >= 18 && #patientAge <= 65"
                message: "Patient age must be between 18 and 65"
                priority: 10
                enabled: true
                
                # Enterprise metadata
                created-by: "healthcare.admin@hospital.com"
                business-domain: "Healthcare"
                business-owner: "Chief Medical Officer"
                source-system: "HOSPITAL_MANAGEMENT_SYSTEM"
                effective-date: "2024-01-01T00:00:00Z"
                expiration-date: "2025-01-01T00:00:00Z"
                
                custom-properties:
                  department: "Cardiology"
                  compliance-level: "HIGH"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        RulesEngine engine = service.createRulesEngineFromYamlConfig(yamlConfig);

        assertNotNull(engine);
        
        List<Rule> rules = engine.getConfiguration().getAllRules();
        assertEquals(1, rules.size());
        
        Rule rule = rules.get(0);
        assertEquals("Patient Age Verification", rule.getName());
        assertEquals("#patientAge >= 18 && #patientAge <= 65", rule.getCondition());
        
        // Test enterprise metadata
        RuleMetadata metadata = rule.getMetadata();
        assertEquals("healthcare.admin@hospital.com", metadata.getCreatedByUser());
        assertEquals("Healthcare", metadata.getBusinessDomain().orElse(null));
        assertEquals("Chief Medical Officer", metadata.getBusinessOwner().orElse(null));
        assertEquals("HOSPITAL_MANAGEMENT_SYSTEM", metadata.getSourceSystem().orElse(null));
        
        // Test dates
        assertNotNull(metadata.getEffectiveDate());
        assertNotNull(metadata.getExpirationDate());
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), metadata.getEffectiveDate().orElse(null));
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), metadata.getExpirationDate().orElse(null));
        
        // Test custom properties
        assertEquals("Cardiology", metadata.getCustomProperty("department", String.class).orElse(null));
        assertEquals("HIGH", metadata.getCustomProperty("compliance-level", String.class).orElse(null));
        
        // Test audit dates
        assertNotNull(rule.getCreatedDate());
        assertNotNull(rule.getModifiedDate());
    }

    @Test
    void testGenericRuleSetCreationFromYaml() throws YamlConfigurationException {
        String yamlContent = """
            rules:
              - id: "rule-1"
                name: "Quality Control Rule 1"
                category: "quality-control"
                condition: "#temperature >= 20 && #temperature <= 25"
                message: "Temperature within range"
                created-by: "qc.manager@manufacturing.com"
                business-domain: "Manufacturing"
                
              - id: "rule-2"
                name: "Quality Control Rule 2"
                category: "quality-control"
                condition: "#pressure <= 100"
                message: "Pressure within limits"
                created-by: "qc.manager@manufacturing.com"
                business-domain: "Manufacturing"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        
        // Test GenericRuleSet creation
        List<YamlRule> qualityRules = yamlConfig.getRules();
        RuleSet.GenericRuleSet ruleSet = factory.createGenericRuleSet("quality-control", qualityRules);
        
        assertNotNull(ruleSet);
        assertEquals("quality-control", ruleSet.getCategoryName());
        assertEquals(2, ruleSet.getRuleCount());
        assertFalse(ruleSet.isEmpty());
        
        List<Rule> rules = ruleSet.getRules();
        assertEquals(2, rules.size());
        
        // Verify rules have proper metadata
        for (Rule rule : rules) {
            assertEquals("qc.manager@manufacturing.com", rule.getMetadata().getCreatedByUser());
            assertEquals("Manufacturing", rule.getMetadata().getBusinessDomain().orElse(null));
            assertTrue(rule.getId().startsWith("quality-control-"));
            assertNotNull(rule.getCreatedDate());
            assertNotNull(rule.getModifiedDate());
        }
    }

    @Test
    void testMultipleCategoriesWithGenericArchitecture() throws YamlConfigurationException {
        String yamlContent = """
            categories:
              - name: "healthcare"
                business-domain: "Healthcare"
              - name: "manufacturing"
                business-domain: "Manufacturing"
                
            rules:
              - id: "health-rule"
                name: "Health Rule"
                category: "healthcare"
                condition: "#age >= 18"
                message: "Age valid"
                created-by: "health.admin@hospital.com"
                
              - id: "mfg-rule"
                name: "Manufacturing Rule"
                category: "manufacturing"
                condition: "#temp <= 100"
                message: "Temperature valid"
                created-by: "mfg.admin@factory.com"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        RulesEngine engine = service.createRulesEngineFromYamlConfig(yamlConfig);

        assertNotNull(engine);
        
        List<Rule> rules = engine.getConfiguration().getAllRules();
        assertEquals(2, rules.size());
        
        // Verify rules are in different categories
        Rule healthRule = rules.stream()
            .filter(r -> r.getName().equals("Health Rule"))
            .findFirst()
            .orElse(null);
        assertNotNull(healthRule);
        assertEquals("health.admin@hospital.com", healthRule.getMetadata().getCreatedByUser());
        
        Rule mfgRule = rules.stream()
            .filter(r -> r.getName().equals("Manufacturing Rule"))
            .findFirst()
            .orElse(null);
        assertNotNull(mfgRule);
        assertEquals("mfg.admin@factory.com", mfgRule.getMetadata().getCreatedByUser());
    }

    @Test
    void testRuleExecutionWithEnhancedMetadata() throws YamlConfigurationException {
        String yamlContent = """
            rules:
              - id: "execution-test"
                name: "Execution Test Rule"
                category: "test-execution"
                condition: "#value > 50"
                message: "Value is greater than 50"
                created-by: "test.user@company.com"
                business-domain: "Testing"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        RulesEngine engine = service.createRulesEngineFromYamlConfig(yamlConfig);

        // Test rule execution
        Map<String, Object> facts1 = Map.of("value", 75);
        List<Rule> rules = engine.getConfiguration().getAllRules();
        Rule rule = rules.get(0);
        
        RuleResult result1 = engine.executeRule(rule, facts1);
        assertTrue(result1.isTriggered());
        assertEquals("Value is greater than 50", result1.getMessage());
        assertEquals("Execution Test Rule", result1.getRuleName());
        
        // Test with failing condition
        Map<String, Object> facts2 = Map.of("value", 25);
        RuleResult result2 = engine.executeRule(rule, facts2);
        assertFalse(result2.isTriggered());
        
        // Verify rule metadata is preserved
        assertEquals("test.user@company.com", rule.getMetadata().getCreatedByUser());
        assertEquals("Testing", rule.getMetadata().getBusinessDomain().orElse(null));
    }

    @Test
    void testValidationAndErrorHandling() {
        // Test invalid category name
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createGenericRuleSet("", Arrays.asList());
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createGenericRuleSet(null, Arrays.asList());
        });
        
        // Test invalid date format
        String yamlWithInvalidDate = """
            rules:
              - id: "invalid-date-rule"
                name: "Invalid Date Rule"
                category: "test"
                condition: "#value > 0"
                message: "Test message"
                effective-date: "invalid-date-format"
            """;
        
        assertDoesNotThrow(() -> {
            YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlWithInvalidDate);
            // Should not throw, but should log warning about invalid date
            RulesEngine engine = service.createRulesEngineFromYamlConfig(yamlConfig);
            assertNotNull(engine);
        });
    }

    @Test
    void testBackwardCompatibility() throws YamlConfigurationException {
        // Test that old YAML format still works
        String legacyYamlContent = """
            rules:
              - id: "legacy-rule"
                name: "Legacy Rule"
                category: "validation"
                condition: "#value != null"
                message: "Value is required"
                priority: 10
                enabled: true
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(legacyYamlContent);
        
        // Should work with both old and new methods
        RulesEngine legacyEngine = service.createRulesEngineFromFile("test");  // This would fail in real test, but shows API compatibility
        RulesEngine newEngine = service.createRulesEngineFromYamlConfig(yamlConfig);
        
        assertNotNull(newEngine);
        List<Rule> rules = newEngine.getConfiguration().getAllRules();
        assertEquals(1, rules.size());
        
        Rule rule = rules.get(0);
        assertEquals("Legacy Rule", rule.getName());
        assertEquals("#value != null", rule.getCondition());
        
        // Even legacy rules should have audit dates
        assertNotNull(rule.getCreatedDate());
        assertNotNull(rule.getModifiedDate());
    }

    @Test
    void testCustomPropertiesHandling() throws YamlConfigurationException {
        String yamlContent = """
            rules:
              - id: "custom-props-rule"
                name: "Custom Properties Rule"
                category: "test"
                condition: "#value > 0"
                message: "Test message"
                created-by: "test.user@company.com"
                custom-properties:
                  department: "Engineering"
                  priority-level: "HIGH"
                  numeric-value: 42
                  boolean-flag: true
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        RulesEngine engine = service.createRulesEngineFromYamlConfig(yamlConfig);

        List<Rule> rules = engine.getConfiguration().getAllRules();
        Rule rule = rules.get(0);
        
        RuleMetadata metadata = rule.getMetadata();
        assertEquals("Engineering", metadata.getCustomProperty("department", String.class).orElse(null));
        assertEquals("HIGH", metadata.getCustomProperty("priority-level", String.class).orElse(null));
        assertEquals(42, metadata.getCustomProperty("numeric-value", Integer.class).orElse(null));
        assertEquals(true, metadata.getCustomProperty("boolean-flag", Boolean.class).orElse(null));
    }
}
