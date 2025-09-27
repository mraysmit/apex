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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for YamlErrorRecoveryConfig.
 * 
 * Tests the conversion from YAML configuration to internal ErrorRecoveryConfig,
 * including backward compatibility and default value handling.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-27
 * @version 1.0
 */
class YamlErrorRecoveryConfigTest {

    @Test
    void testDefaultConstructor() {
        YamlErrorRecoveryConfig yamlConfig = new YamlErrorRecoveryConfig();
        
        assertNull(yamlConfig.getEnabled());
        assertNull(yamlConfig.getLogRecoveryAttempts());
        assertNull(yamlConfig.getMetricsEnabled());
        assertNull(yamlConfig.getDefaultStrategy());
        assertNull(yamlConfig.getSeverityPolicies());
    }

    @Test
    void testToErrorRecoveryConfigWithDefaults() {
        YamlErrorRecoveryConfig yamlConfig = new YamlErrorRecoveryConfig();
        
        ErrorRecoveryConfig config = yamlConfig.toErrorRecoveryConfig();
        
        assertNotNull(config);
        // Should use ErrorRecoveryConfig defaults when YAML values are null
        assertTrue(config.isEnabled()); // Default is true
        assertTrue(config.isLogRecoveryAttempts()); // Default is true
        assertTrue(config.isMetricsEnabled()); // Default is true
        assertEquals("CONTINUE_WITH_DEFAULT", config.getDefaultStrategy()); // Default strategy
    }

    @Test
    void testToErrorRecoveryConfigWithCustomValues() {
        YamlErrorRecoveryConfig yamlConfig = new YamlErrorRecoveryConfig();
        yamlConfig.setEnabled(false);
        yamlConfig.setLogRecoveryAttempts(false);
        yamlConfig.setMetricsEnabled(false);
        yamlConfig.setDefaultStrategy("FAIL_FAST");
        
        ErrorRecoveryConfig config = yamlConfig.toErrorRecoveryConfig();
        
        assertNotNull(config);
        assertFalse(config.isEnabled());
        assertFalse(config.isLogRecoveryAttempts());
        assertFalse(config.isMetricsEnabled());
        assertEquals("FAIL_FAST", config.getDefaultStrategy());
    }

    @Test
    void testToErrorRecoveryConfigWithSeverityPolicies() {
        YamlErrorRecoveryConfig yamlConfig = new YamlErrorRecoveryConfig();
        
        // Create severity policies
        Map<String, YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy> severityPolicies = new HashMap<>();
        
        YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy errorPolicy = 
            new YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy();
        errorPolicy.setRecoveryEnabled(false);
        errorPolicy.setStrategy("FAIL_FAST");
        severityPolicies.put("ERROR", errorPolicy);
        
        YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy warningPolicy = 
            new YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy();
        warningPolicy.setRecoveryEnabled(true);
        warningPolicy.setStrategy("CONTINUE_WITH_DEFAULT");
        warningPolicy.setMaxRetries(2);
        warningPolicy.setRetryDelay(500L);
        severityPolicies.put("WARNING", warningPolicy);
        
        yamlConfig.setSeverityPolicies(severityPolicies);
        
        ErrorRecoveryConfig config = yamlConfig.toErrorRecoveryConfig();
        
        assertNotNull(config);
        
        // Check ERROR policy
        assertFalse(config.isRecoveryEnabledForSeverity("ERROR"));
        SeverityRecoveryPolicy errorConfigPolicy = config.getSeverityPolicy("ERROR");
        assertNotNull(errorConfigPolicy);
        assertFalse(errorConfigPolicy.isRecoveryEnabled());
        assertEquals("FAIL_FAST", errorConfigPolicy.getStrategy());
        
        // Check WARNING policy
        assertTrue(config.isRecoveryEnabledForSeverity("WARNING"));
        SeverityRecoveryPolicy warningConfigPolicy = config.getSeverityPolicy("WARNING");
        assertNotNull(warningConfigPolicy);
        assertTrue(warningConfigPolicy.isRecoveryEnabled());
        assertEquals("CONTINUE_WITH_DEFAULT", warningConfigPolicy.getStrategy());
        assertEquals(2, warningConfigPolicy.getMaxRetries());
        assertEquals(500L, warningConfigPolicy.getRetryDelay());
    }

    @Test
    void testYamlSeverityRecoveryPolicyDefaultConstructor() {
        YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy yamlPolicy = 
            new YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy();
        
        assertNull(yamlPolicy.getRecoveryEnabled());
        assertNull(yamlPolicy.getStrategy());
        assertNull(yamlPolicy.getMaxRetries());
        assertNull(yamlPolicy.getRetryDelay());
    }

    @Test
    void testYamlSeverityRecoveryPolicyToSeverityRecoveryPolicyWithDefaults() {
        YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy yamlPolicy = 
            new YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy();
        
        SeverityRecoveryPolicy policy = yamlPolicy.toSeverityRecoveryPolicy();
        
        assertNotNull(policy);
        // Should use SeverityRecoveryPolicy defaults when YAML values are null
        assertTrue(policy.isRecoveryEnabled()); // Default is true
        assertEquals("CONTINUE_WITH_DEFAULT", policy.getStrategy()); // Default strategy
        assertEquals(0, policy.getMaxRetries()); // Default is 0
        assertEquals(100L, policy.getRetryDelay()); // Default is 100ms
    }

    @Test
    void testYamlSeverityRecoveryPolicyToSeverityRecoveryPolicyWithCustomValues() {
        YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy yamlPolicy = 
            new YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy();
        yamlPolicy.setRecoveryEnabled(false);
        yamlPolicy.setStrategy("RETRY_WITH_SAFE_EXPRESSION");
        yamlPolicy.setMaxRetries(3);
        yamlPolicy.setRetryDelay(1000L);
        
        SeverityRecoveryPolicy policy = yamlPolicy.toSeverityRecoveryPolicy();
        
        assertNotNull(policy);
        assertFalse(policy.isRecoveryEnabled());
        assertEquals("RETRY_WITH_SAFE_EXPRESSION", policy.getStrategy());
        assertEquals(3, policy.getMaxRetries());
        assertEquals(1000L, policy.getRetryDelay());
    }

    @Test
    void testGettersAndSetters() {
        YamlErrorRecoveryConfig yamlConfig = new YamlErrorRecoveryConfig();
        
        yamlConfig.setEnabled(true);
        yamlConfig.setLogRecoveryAttempts(false);
        yamlConfig.setMetricsEnabled(true);
        yamlConfig.setDefaultStrategy("SKIP_RULE");
        
        Map<String, YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy> policies = new HashMap<>();
        yamlConfig.setSeverityPolicies(policies);
        
        assertTrue(yamlConfig.getEnabled());
        assertFalse(yamlConfig.getLogRecoveryAttempts());
        assertTrue(yamlConfig.getMetricsEnabled());
        assertEquals("SKIP_RULE", yamlConfig.getDefaultStrategy());
        assertSame(policies, yamlConfig.getSeverityPolicies());
    }

    @Test
    void testYamlSeverityRecoveryPolicyGettersAndSetters() {
        YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy yamlPolicy = 
            new YamlErrorRecoveryConfig.YamlSeverityRecoveryPolicy();
        
        yamlPolicy.setRecoveryEnabled(true);
        yamlPolicy.setStrategy("CONTINUE_WITH_DEFAULT");
        yamlPolicy.setMaxRetries(5);
        yamlPolicy.setRetryDelay(2000L);
        
        assertTrue(yamlPolicy.getRecoveryEnabled());
        assertEquals("CONTINUE_WITH_DEFAULT", yamlPolicy.getStrategy());
        assertEquals(5, yamlPolicy.getMaxRetries());
        assertEquals(2000L, yamlPolicy.getRetryDelay());
    }

    @Test
    void testBackwardCompatibilityWithNullSeverityPolicies() {
        YamlErrorRecoveryConfig yamlConfig = new YamlErrorRecoveryConfig();
        yamlConfig.setEnabled(true);
        // severityPolicies is null - should not cause issues
        
        ErrorRecoveryConfig config = yamlConfig.toErrorRecoveryConfig();
        
        assertNotNull(config);
        assertTrue(config.isEnabled());
        
        // Should use default behavior for severities when no policies are specified
        // ERROR should default to no recovery (backward compatible)
        assertFalse(config.isRecoveryEnabledForSeverity("ERROR"));
        // WARNING should default to recovery enabled
        assertTrue(config.isRecoveryEnabledForSeverity("WARNING"));
    }
}
