package dev.mars.apex.core.demo;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRule;
import dev.mars.apex.core.config.yaml.YamlEnrichment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstration test for the new default-value functionality.
 * 
 * This test shows how the default-value enhancement works in practice,
 * loading a complete YAML configuration and verifying that default values
 * are properly parsed and available for both rules and calculations.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-27
 * @version 1.0
 */
class DefaultValueDemoTest {

    @Test
    void testDefaultValueEnhancementDemo() throws Exception {
        // Load the demo configuration
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.loadFromClasspath("demo-default-value.yaml");
        
        assertNotNull(config);
        assertEquals("demo-default-value", config.getMetadata().getId());
        assertEquals("Default Value Enhancement Demo", config.getMetadata().getName());
        
        // Verify rules with default values
        assertEquals(3, config.getRules().size());
        
        // Test string default value
        YamlRule customerRule = config.getRules().get(0);
        assertEquals("customer-validation", customerRule.getId());
        assertEquals("Customer validation skipped - using default", customerRule.getDefaultValue());
        
        // Test boolean default value
        YamlRule ageRule = config.getRules().get(1);
        assertEquals("age-check", ageRule.getId());
        assertEquals(true, ageRule.getDefaultValue());
        
        // Test numeric default value
        YamlRule creditRule = config.getRules().get(2);
        assertEquals("credit-score", creditRule.getId());
        assertEquals(600, creditRule.getDefaultValue());
        
        // Verify enrichments with calculation default values
        assertEquals(2, config.getEnrichments().size());
        
        // Test numeric calculation default
        YamlEnrichment riskEnrichment = config.getEnrichments().get(0);
        assertEquals("risk-calculation", riskEnrichment.getId());
        assertNotNull(riskEnrichment.getCalculationConfig());
        assertEquals(500.0, riskEnrichment.getCalculationConfig().getDefaultValue());
        
        // Test string calculation default
        YamlEnrichment tierEnrichment = config.getEnrichments().get(1);
        assertEquals("customer-tier", tierEnrichment.getId());
        assertNotNull(tierEnrichment.getCalculationConfig());
        assertEquals("UNKNOWN", tierEnrichment.getCalculationConfig().getDefaultValue());
        
        // Verify error recovery configuration is also loaded
        assertNotNull(config.getErrorRecovery());
        assertTrue(config.getErrorRecovery().getEnabled());
        assertEquals("CONTINUE_WITH_DEFAULT", config.getErrorRecovery().getDefaultStrategy());
        
        System.out.println("✅ Default Value Enhancement Demo - All tests passed!");
        System.out.println("📋 Configuration loaded successfully:");
        System.out.println("   - Rules with default values: " + config.getRules().size());
        System.out.println("   - Calculations with default values: " + config.getEnrichments().size());
        System.out.println("   - Error recovery enabled: " + config.getErrorRecovery().getEnabled());
    }
    
    @Test
    void testBackwardCompatibilityWithoutDefaultValues() throws Exception {
        // Test that rules without default-value still work (backward compatibility)
        String yamlConfig = """
            metadata:
              id: "backward-compatibility-test"
              name: "Backward Compatibility Test"
              
            rules:
              - id: "simple-rule"
                name: "Simple Rule"
                condition: "#data.field != null"
                message: "Field is not null"
                severity: "INFO"
                # Note: no default-value specified
            """;
        
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        
        assertNotNull(config);
        assertEquals(1, config.getRules().size());
        
        YamlRule rule = config.getRules().get(0);
        assertEquals("simple-rule", rule.getId());
        assertNull(rule.getDefaultValue()); // Should be null when not specified
        
        System.out.println("✅ Backward Compatibility Test - Passed!");
        System.out.println("   - Rules without default-value work correctly");
    }
}
