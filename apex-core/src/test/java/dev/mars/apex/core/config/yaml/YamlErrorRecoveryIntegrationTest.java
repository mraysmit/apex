package dev.mars.apex.core.config.yaml;

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

import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
import dev.mars.apex.core.config.error.SeverityRecoveryPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for YAML error recovery configuration loading.
 * 
 * Tests the complete flow from YAML file loading through to ErrorRecoveryConfig creation,
 * demonstrating the optional error-recovery section functionality.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-27
 * @version 1.0
 */
class YamlErrorRecoveryIntegrationTest {

    private final YamlConfigurationLoader loader = new YamlConfigurationLoader();

    @Test
    void testLoadYamlConfigurationWithErrorRecoverySection() throws Exception {
        // Load YAML configuration with error-recovery section
        YamlRuleConfiguration config = loader.loadFromClasspath("yaml-error-recovery-test.yaml");
        
        assertNotNull(config);
        assertNotNull(config.getMetadata());
        assertEquals("yaml-error-recovery-test", config.getMetadata().getId());
        
        // Verify error-recovery section is loaded
        YamlErrorRecoveryConfig yamlErrorRecovery = config.getErrorRecovery();
        assertNotNull(yamlErrorRecovery, "Error recovery configuration should be loaded from YAML");
        
        // Verify basic settings
        assertTrue(yamlErrorRecovery.getEnabled());
        assertTrue(yamlErrorRecovery.getLogRecoveryAttempts());
        assertTrue(yamlErrorRecovery.getMetricsEnabled());
        assertEquals("CONTINUE_WITH_DEFAULT", yamlErrorRecovery.getDefaultStrategy());
        
        // Verify severity policies are loaded
        assertNotNull(yamlErrorRecovery.getSeverityPolicies());
        assertEquals(3, yamlErrorRecovery.getSeverityPolicies().size());

        // Check ERROR policy (backward compatible)
        YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy errorPolicy =
            yamlErrorRecovery.getSeverityPolicies().get("ERROR");
        assertNotNull(errorPolicy);
        assertFalse(errorPolicy.getRecoveryEnabled());
        assertEquals("FAIL_FAST", errorPolicy.getStrategy());
        
        // Check WARNING policy
        YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy warningPolicy = 
            yamlErrorRecovery.getSeverityPolicies().get("WARNING");
        assertNotNull(warningPolicy);
        assertTrue(warningPolicy.getRecoveryEnabled());
        assertEquals("CONTINUE_WITH_DEFAULT", warningPolicy.getStrategy());
        assertEquals(1, warningPolicy.getMaxRetries());
        assertEquals(100L, warningPolicy.getRetryDelay());
        
        // Check INFO policy
        YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy infoPolicy = 
            yamlErrorRecovery.getSeverityPolicies().get("INFO");
        assertNotNull(infoPolicy);
        assertTrue(infoPolicy.getRecoveryEnabled());
        assertEquals("CONTINUE_WITH_DEFAULT", infoPolicy.getStrategy());
        assertEquals(0, infoPolicy.getMaxRetries());
        assertEquals(50L, infoPolicy.getRetryDelay());
    }

    @Test
    void testConvertYamlErrorRecoveryToInternalConfig() throws Exception {
        // Load YAML configuration
        YamlRuleConfiguration config = loader.loadFromClasspath("yaml-error-recovery-test.yaml");
        YamlErrorRecoveryConfig yamlErrorRecovery = config.getErrorRecovery();
        
        // Convert to internal ErrorRecoveryConfig
        ErrorRecoveryConfig errorRecoveryConfig = yamlErrorRecovery.toErrorRecoveryConfig();
        
        assertNotNull(errorRecoveryConfig);
        assertTrue(errorRecoveryConfig.isEnabled());
        assertTrue(errorRecoveryConfig.isLogRecoveryAttempts());
        assertTrue(errorRecoveryConfig.isMetricsEnabled());
        assertEquals("CONTINUE_WITH_DEFAULT", errorRecoveryConfig.getDefaultStrategy());
        
        // Test severity-specific recovery behavior
        assertFalse(errorRecoveryConfig.isRecoveryEnabledForSeverity("ERROR"));
        assertTrue(errorRecoveryConfig.isRecoveryEnabledForSeverity("WARNING"));
        assertTrue(errorRecoveryConfig.isRecoveryEnabledForSeverity("INFO"));
        
        SeverityRecoveryPolicy warningPolicy = errorRecoveryConfig.getSeverityPolicy("WARNING");
        assertNotNull(warningPolicy);
        assertTrue(warningPolicy.isRecoveryEnabled());
        assertEquals("CONTINUE_WITH_DEFAULT", warningPolicy.getStrategy());
        assertEquals(1, warningPolicy.getMaxRetries());
        assertEquals(100L, warningPolicy.getRetryDelay());
    }

    @Test
    void testLoadYamlConfigurationWithoutErrorRecoverySection() throws Exception {
        // Load a standard YAML configuration without error-recovery section
        YamlRuleConfiguration config = loader.loadFromClasspath("rulegroups/customer-rules.yaml");

        assertNotNull(config);

        // Verify error-recovery section is null (optional)
        YamlErrorRecoveryConfig yamlErrorRecovery = config.getErrorRecovery();
        assertNull(yamlErrorRecovery, "Error recovery configuration should be null when not present in YAML");
    }

    @Test
    void testBackwardCompatibilityWithoutErrorRecoverySection() throws Exception {
        // Load configuration without error-recovery section
        YamlRuleConfiguration config = loader.loadFromClasspath("rulegroups/customer-rules.yaml");

        // Create default ErrorRecoveryConfig when YAML section is missing
        ErrorRecoveryConfig errorRecoveryConfig;
        if (config.getErrorRecovery() != null) {
            errorRecoveryConfig = config.getErrorRecovery().toErrorRecoveryConfig();
        } else {
            // Use backward-compatible defaults
            errorRecoveryConfig = new ErrorRecoveryConfig();
        }

        assertNotNull(errorRecoveryConfig);

        // Verify backward-compatible behavior
        assertTrue(errorRecoveryConfig.isEnabled());
        assertFalse(errorRecoveryConfig.isRecoveryEnabledForSeverity("ERROR")); // Backward compatible
        assertTrue(errorRecoveryConfig.isRecoveryEnabledForSeverity("WARNING"));
        assertTrue(errorRecoveryConfig.isRecoveryEnabledForSeverity("INFO"));
    }

    @Test
    void testYamlConfigurationWithRulesAndErrorRecovery() throws Exception {
        // Load configuration with both rules and error-recovery
        YamlRuleConfiguration config = loader.loadFromClasspath("yaml-error-recovery-test.yaml");
        
        assertNotNull(config);
        
        // Verify rules are loaded
        assertNotNull(config.getRules());
        assertEquals(3, config.getRules().size());

        // Verify enrichments are loaded
        assertNotNull(config.getEnrichments());
        assertEquals(1, config.getEnrichments().size());

        // Verify error-recovery is loaded
        assertNotNull(config.getErrorRecovery());

        // Verify rules have appropriate severities
        assertEquals("ERROR", config.getRules().get(0).getSeverity());
        assertEquals("WARNING", config.getRules().get(1).getSeverity());
        assertEquals("INFO", config.getRules().get(2).getSeverity());

        // Convert to ErrorRecoveryConfig and verify behavior matches rule severities
        ErrorRecoveryConfig errorRecoveryConfig = config.getErrorRecovery().toErrorRecoveryConfig();

        // ERROR rules should not recover (backward compatible)
        assertFalse(errorRecoveryConfig.isRecoveryEnabledForSeverity("ERROR"));

        // WARNING and INFO rules should recover
        assertTrue(errorRecoveryConfig.isRecoveryEnabledForSeverity("WARNING"));
        assertTrue(errorRecoveryConfig.isRecoveryEnabledForSeverity("INFO"));
    }

    @Test
    void testYamlConfigurationValidation() throws Exception {
        // Load and validate configuration
        YamlRuleConfiguration config = loader.loadFromClasspath("yaml-error-recovery-test.yaml");
        
        assertNotNull(config);
        
        // Verify metadata is required and present
        assertNotNull(config.getMetadata());
        assertNotNull(config.getMetadata().getId());
        assertNotNull(config.getMetadata().getName());
        
        // Verify error-recovery section is optional but valid when present
        YamlErrorRecoveryConfig errorRecovery = config.getErrorRecovery();
        assertNotNull(errorRecovery);
        
        // Convert and validate internal configuration
        ErrorRecoveryConfig internalConfig = errorRecovery.toErrorRecoveryConfig();
        assertNotNull(internalConfig);
        
        // Verify configuration is valid and usable
        assertTrue(internalConfig.isEnabled());
        assertNotNull(internalConfig.getDefaultStrategy());
        
        // Verify all severity policies are valid
        for (String severity : new String[]{"ERROR", "WARNING", "INFO"}) {
            SeverityRecoveryPolicy policy = internalConfig.getSeverityPolicy(severity);
            assertNotNull(policy, "Policy should exist for severity: " + severity);
            assertNotNull(policy.getStrategy(), "Strategy should be defined for severity: " + severity);
            assertTrue(policy.getMaxRetries() >= 0, "Max retries should be non-negative for severity: " + severity);
            assertTrue(policy.getRetryDelay() > 0, "Retry delay should be positive for severity: " + severity);
        }
    }
}
