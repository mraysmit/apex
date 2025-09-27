/*
 * Copyright (c) 2024 APEX Rules Engine Contributors
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

package dev.mars.apex.core.config.error;

import dev.mars.apex.core.constants.SeverityConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ErrorRecoveryConfig.
 * 
 * Tests the configuration of severity-based error recovery policies,
 * validation logic, and backward compatibility.
 */
@DisplayName("ErrorRecoveryConfig Tests")
class ErrorRecoveryConfigTest {

    private ErrorRecoveryConfig config;

    @BeforeEach
    void setUp() {
        config = new ErrorRecoveryConfig();
    }

    @Test
    @DisplayName("Should initialize with backward-compatible defaults")
    void testBackwardCompatibleDefaults() {
        // ERROR severity should have recovery disabled (backward compatible)
        assertFalse(config.isRecoveryEnabledForSeverity(SeverityConstants.ERROR),
                "ERROR severity should have recovery disabled by default");
        
        // WARNING and INFO should have recovery enabled
        assertTrue(config.isRecoveryEnabledForSeverity(SeverityConstants.WARNING),
                "WARNING severity should have recovery enabled by default");
        assertTrue(config.isRecoveryEnabledForSeverity(SeverityConstants.INFO),
                "INFO severity should have recovery enabled by default");
        
        // Check default strategies
        assertEquals("FAIL_FAST", config.getRecoveryStrategy(SeverityConstants.ERROR),
                "ERROR severity should use FAIL_FAST strategy");
        assertEquals("CONTINUE_WITH_DEFAULT", config.getRecoveryStrategy(SeverityConstants.WARNING),
                "WARNING severity should use CONTINUE_WITH_DEFAULT strategy");
        assertEquals("CONTINUE_WITH_DEFAULT", config.getRecoveryStrategy(SeverityConstants.INFO),
                "INFO severity should use CONTINUE_WITH_DEFAULT strategy");
    }

    @Test
    @DisplayName("Should handle custom severity policies")
    void testCustomSeverityPolicies() {
        // Create custom policy for ERROR severity (enable recovery)
        SeverityRecoveryPolicy errorPolicy = new SeverityRecoveryPolicy(true, "RETRY_WITH_SAFE_EXPRESSION");
        config.setSeverityPolicy(SeverityConstants.ERROR, errorPolicy);
        
        // Verify custom policy is applied
        assertTrue(config.isRecoveryEnabledForSeverity(SeverityConstants.ERROR),
                "ERROR severity should have recovery enabled with custom policy");
        assertEquals("RETRY_WITH_SAFE_EXPRESSION", config.getRecoveryStrategy(SeverityConstants.ERROR),
                "ERROR severity should use custom strategy");
    }

    @Test
    @DisplayName("Should handle global enable/disable")
    void testGlobalEnableDisable() {
        // Disable globally
        config.setEnabled(false);
        
        // All severities should have recovery disabled
        assertFalse(config.isRecoveryEnabledForSeverity(SeverityConstants.ERROR),
                "ERROR severity should be disabled when global recovery is disabled");
        assertFalse(config.isRecoveryEnabledForSeverity(SeverityConstants.WARNING),
                "WARNING severity should be disabled when global recovery is disabled");
        assertFalse(config.isRecoveryEnabledForSeverity(SeverityConstants.INFO),
                "INFO severity should be disabled when global recovery is disabled");
        
        // Re-enable globally
        config.setEnabled(true);
        
        // Should return to default behavior
        assertFalse(config.isRecoveryEnabledForSeverity(SeverityConstants.ERROR),
                "ERROR severity should remain disabled (default policy)");
        assertTrue(config.isRecoveryEnabledForSeverity(SeverityConstants.WARNING),
                "WARNING severity should be enabled when global recovery is enabled");
    }

    @Test
    @DisplayName("Should handle null and unknown severities")
    void testNullAndUnknownSeverities() {
        // Null severity
        assertFalse(config.isRecoveryEnabledForSeverity(null),
                "Null severity should return false");
        assertEquals("CONTINUE_WITH_DEFAULT", config.getRecoveryStrategy(null),
                "Null severity should return default strategy");
        
        // Unknown severity
        assertFalse(config.isRecoveryEnabledForSeverity("UNKNOWN"),
                "Unknown severity should return false");
        assertEquals("CONTINUE_WITH_DEFAULT", config.getRecoveryStrategy("UNKNOWN"),
                "Unknown severity should return default strategy");
    }

    @Test
    @DisplayName("Should validate configuration successfully")
    void testValidConfiguration() {
        // Default configuration should be valid
        assertDoesNotThrow(() -> config.validate(),
                "Default configuration should be valid");
        
        // Custom valid configuration
        SeverityRecoveryPolicy validPolicy = new SeverityRecoveryPolicy(true, "SKIP_RULE", 2, 500L);
        config.setSeverityPolicy("CUSTOM", validPolicy);
        
        assertDoesNotThrow(() -> config.validate(),
                "Custom valid configuration should be valid");
    }

    @Test
    @DisplayName("Should reject invalid default strategy")
    void testInvalidDefaultStrategy() {
        config.setDefaultStrategy("INVALID_STRATEGY");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> config.validate(),
                "Should throw exception for invalid default strategy");
        
        assertTrue(exception.getMessage().contains("Invalid default recovery strategy"),
                "Exception message should mention invalid default strategy");
    }

    @Test
    @DisplayName("Should reject null default strategy")
    void testNullDefaultStrategy() {
        config.setDefaultStrategy(null);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> config.validate(),
                "Should throw exception for null default strategy");
        
        assertTrue(exception.getMessage().contains("cannot be null or empty"),
                "Exception message should mention null/empty strategy");
    }

    @Test
    @DisplayName("Should reject invalid severity policy")
    void testInvalidSeverityPolicy() {
        // Create policy with invalid strategy
        SeverityRecoveryPolicy invalidPolicy = new SeverityRecoveryPolicy(true, "INVALID_STRATEGY");
        config.setSeverityPolicy(SeverityConstants.ERROR, invalidPolicy);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> config.validate(),
                "Should throw exception for invalid severity policy");
        
        assertTrue(exception.getMessage().contains("Invalid recovery policy for severity ERROR"),
                "Exception message should mention invalid policy for ERROR severity");
    }

    @Test
    @DisplayName("Should handle case-insensitive severity lookup")
    void testCaseInsensitiveSeverityLookup() {
        // Test different cases
        assertTrue(config.isRecoveryEnabledForSeverity("warning"),
                "Lowercase 'warning' should work");
        assertTrue(config.isRecoveryEnabledForSeverity("Warning"),
                "Mixed case 'Warning' should work");
        assertTrue(config.isRecoveryEnabledForSeverity("WARNING"),
                "Uppercase 'WARNING' should work");
        
        assertFalse(config.isRecoveryEnabledForSeverity("error"),
                "Lowercase 'error' should work");
        assertFalse(config.isRecoveryEnabledForSeverity("Error"),
                "Mixed case 'Error' should work");
        assertFalse(config.isRecoveryEnabledForSeverity("ERROR"),
                "Uppercase 'ERROR' should work");
    }

    @Test
    @DisplayName("Should provide meaningful toString representation")
    void testToString() {
        String toString = config.toString();
        
        assertNotNull(toString, "toString should not be null");
        assertTrue(toString.contains("ErrorRecoveryConfig"), "toString should contain class name");
        assertTrue(toString.contains("enabled=true"), "toString should show enabled status");
        assertTrue(toString.contains("policies"), "toString should mention policies");
    }

    @Test
    @DisplayName("Should handle severity policies map operations safely")
    void testSeverityPoliciesMapSafety() {
        // Get policies map and verify it's a copy
        var policiesMap = config.getSeverityPolicies();
        int originalSize = policiesMap.size();
        
        // Modify the returned map
        policiesMap.put("TEST", new SeverityRecoveryPolicy());
        
        // Verify original config is not affected
        assertEquals(originalSize, config.getSeverityPolicies().size(),
                "Original config should not be affected by modifications to returned map");
        
        assertNull(config.getSeverityPolicy("TEST"),
                "Config should not contain the policy added to returned map");
    }
}
