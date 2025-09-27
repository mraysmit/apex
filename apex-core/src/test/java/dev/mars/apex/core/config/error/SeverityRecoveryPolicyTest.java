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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SeverityRecoveryPolicy.
 * 
 * Tests the configuration of individual severity recovery policies,
 * validation logic, and parameter handling.
 */
@DisplayName("SeverityRecoveryPolicy Tests")
class SeverityRecoveryPolicyTest {

    private SeverityRecoveryPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new SeverityRecoveryPolicy();
    }

    @Test
    @DisplayName("Should initialize with default values")
    void testDefaultValues() {
        assertTrue(policy.isRecoveryEnabled(), "Recovery should be enabled by default");
        assertEquals("CONTINUE_WITH_DEFAULT", policy.getStrategy(), "Default strategy should be CONTINUE_WITH_DEFAULT");
        assertEquals(0, policy.getMaxRetries(), "Default max retries should be 0");
        assertEquals(100L, policy.getRetryDelay(), "Default retry delay should be 100ms");
        assertEquals(5000L, policy.getMaxRetryDelay(), "Default max retry delay should be 5000ms");
        assertEquals(2.0, policy.getRetryBackoffMultiplier(), "Default backoff multiplier should be 2.0");
        assertTrue(policy.isLogRecoveryAttempts(), "Log recovery attempts should be enabled by default");
    }

    @Test
    @DisplayName("Should initialize with constructor parameters")
    void testConstructorWithParameters() {
        // Test basic constructor
        SeverityRecoveryPolicy basicPolicy = new SeverityRecoveryPolicy(false, "FAIL_FAST");
        assertFalse(basicPolicy.isRecoveryEnabled(), "Recovery should be disabled");
        assertEquals("FAIL_FAST", basicPolicy.getStrategy(), "Strategy should be FAIL_FAST");

        // Test full constructor
        SeverityRecoveryPolicy fullPolicy = new SeverityRecoveryPolicy(true, "RETRY_WITH_SAFE_EXPRESSION", 3, 200L);
        assertTrue(fullPolicy.isRecoveryEnabled(), "Recovery should be enabled");
        assertEquals("RETRY_WITH_SAFE_EXPRESSION", fullPolicy.getStrategy(), "Strategy should be RETRY_WITH_SAFE_EXPRESSION");
        assertEquals(3, fullPolicy.getMaxRetries(), "Max retries should be 3");
        assertEquals(200L, fullPolicy.getRetryDelay(), "Retry delay should be 200ms");
    }

    @Test
    @DisplayName("Should validate valid configuration")
    void testValidConfiguration() {
        // Default configuration should be valid
        assertDoesNotThrow(() -> policy.validate(), "Default configuration should be valid");

        // Custom valid configuration
        policy.setStrategy("SKIP_RULE");
        policy.setMaxRetries(5);
        policy.setRetryDelay(1000L);
        policy.setMaxRetryDelay(10000L);
        policy.setRetryBackoffMultiplier(1.5);

        assertDoesNotThrow(() -> policy.validate(), "Custom valid configuration should be valid");
    }

    @Test
    @DisplayName("Should reject null strategy")
    void testNullStrategy() {
        policy.setStrategy(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> policy.validate(),
                "Should throw exception for null strategy");

        assertTrue(exception.getMessage().contains("cannot be null or empty"),
                "Exception message should mention null/empty strategy");
    }

    @Test
    @DisplayName("Should reject empty strategy")
    void testEmptyStrategy() {
        policy.setStrategy("");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> policy.validate(),
                "Should throw exception for empty strategy");

        assertTrue(exception.getMessage().contains("cannot be null or empty"),
                "Exception message should mention null/empty strategy");
    }

    @Test
    @DisplayName("Should reject invalid strategy")
    void testInvalidStrategy() {
        policy.setStrategy("INVALID_STRATEGY");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> policy.validate(),
                "Should throw exception for invalid strategy");

        assertTrue(exception.getMessage().contains("Invalid recovery strategy"),
                "Exception message should mention invalid strategy");
        assertTrue(exception.getMessage().contains("INVALID_STRATEGY"),
                "Exception message should include the invalid strategy name");
    }

    @Test
    @DisplayName("Should reject negative max retries")
    void testNegativeMaxRetries() {
        policy.setMaxRetries(-1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> policy.validate(),
                "Should throw exception for negative max retries");

        assertTrue(exception.getMessage().contains("Max retries cannot be negative"),
                "Exception message should mention negative max retries");
    }

    @Test
    @DisplayName("Should reject negative retry delay")
    void testNegativeRetryDelay() {
        policy.setRetryDelay(-100L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> policy.validate(),
                "Should throw exception for negative retry delay");

        assertTrue(exception.getMessage().contains("Retry delay cannot be negative"),
                "Exception message should mention negative retry delay");
    }

    @Test
    @DisplayName("Should reject max retry delay less than retry delay")
    void testInvalidMaxRetryDelay() {
        policy.setRetryDelay(1000L);
        policy.setMaxRetryDelay(500L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> policy.validate(),
                "Should throw exception when max retry delay is less than retry delay");

        assertTrue(exception.getMessage().contains("Max retry delay") && 
                   exception.getMessage().contains("cannot be less than retry delay"),
                "Exception message should mention max retry delay constraint");
    }

    @Test
    @DisplayName("Should reject non-positive backoff multiplier")
    void testInvalidBackoffMultiplier() {
        policy.setRetryBackoffMultiplier(0.0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> policy.validate(),
                "Should throw exception for zero backoff multiplier");

        assertTrue(exception.getMessage().contains("Retry backoff multiplier must be positive"),
                "Exception message should mention positive backoff multiplier requirement");

        // Test negative multiplier
        policy.setRetryBackoffMultiplier(-1.5);

        exception = assertThrows(IllegalArgumentException.class,
                () -> policy.validate(),
                "Should throw exception for negative backoff multiplier");

        assertTrue(exception.getMessage().contains("Retry backoff multiplier must be positive"),
                "Exception message should mention positive backoff multiplier requirement");
    }

    @Test
    @DisplayName("Should create accurate copy")
    void testCopy() {
        // Configure original policy
        policy.setRecoveryEnabled(false);
        policy.setStrategy("SKIP_RULE");
        policy.setMaxRetries(3);
        policy.setRetryDelay(500L);
        policy.setMaxRetryDelay(8000L);
        policy.setRetryBackoffMultiplier(1.8);
        policy.setLogRecoveryAttempts(false);
        policy.setRecoveryMessageTemplate("Custom message: {0}");

        // Create copy
        SeverityRecoveryPolicy copy = policy.copy();

        // Verify copy has same values
        assertEquals(policy.isRecoveryEnabled(), copy.isRecoveryEnabled(), "Recovery enabled should match");
        assertEquals(policy.getStrategy(), copy.getStrategy(), "Strategy should match");
        assertEquals(policy.getMaxRetries(), copy.getMaxRetries(), "Max retries should match");
        assertEquals(policy.getRetryDelay(), copy.getRetryDelay(), "Retry delay should match");
        assertEquals(policy.getMaxRetryDelay(), copy.getMaxRetryDelay(), "Max retry delay should match");
        assertEquals(policy.getRetryBackoffMultiplier(), copy.getRetryBackoffMultiplier(), "Backoff multiplier should match");
        assertEquals(policy.isLogRecoveryAttempts(), copy.isLogRecoveryAttempts(), "Log recovery attempts should match");
        assertEquals(policy.getRecoveryMessageTemplate(), copy.getRecoveryMessageTemplate(), "Recovery message template should match");

        // Verify they are different objects
        assertNotSame(policy, copy, "Copy should be a different object");

        // Verify modifying copy doesn't affect original
        copy.setStrategy("FAIL_FAST");
        assertNotEquals(policy.getStrategy(), copy.getStrategy(), "Modifying copy should not affect original");
    }

    @Test
    @DisplayName("Should provide meaningful toString representation")
    void testToString() {
        String toString = policy.toString();

        assertNotNull(toString, "toString should not be null");
        assertTrue(toString.contains("SeverityRecoveryPolicy"), "toString should contain class name");
        assertTrue(toString.contains("recoveryEnabled=true"), "toString should show recovery enabled status");
        assertTrue(toString.contains("strategy='CONTINUE_WITH_DEFAULT'"), "toString should show strategy");
        assertTrue(toString.contains("maxRetries=0"), "toString should show max retries");
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void testEqualsAndHashCode() {
        // Create two identical policies
        SeverityRecoveryPolicy policy1 = new SeverityRecoveryPolicy(true, "SKIP_RULE", 2, 300L);
        SeverityRecoveryPolicy policy2 = new SeverityRecoveryPolicy(true, "SKIP_RULE", 2, 300L);

        // Test equality
        assertEquals(policy1, policy2, "Identical policies should be equal");
        assertEquals(policy1.hashCode(), policy2.hashCode(), "Identical policies should have same hash code");

        // Test inequality
        policy2.setMaxRetries(3);
        assertNotEquals(policy1, policy2, "Different policies should not be equal");

        // Test null and different class
        assertNotEquals(policy1, null, "Policy should not equal null");
        assertNotEquals(policy1, "string", "Policy should not equal different class");

        // Test self-equality
        assertEquals(policy1, policy1, "Policy should equal itself");
    }

    @Test
    @DisplayName("Should handle all valid recovery strategies")
    void testAllValidStrategies() {
        String[] validStrategies = {
            "CONTINUE_WITH_DEFAULT",
            "RETRY_WITH_SAFE_EXPRESSION", 
            "SKIP_RULE",
            "FAIL_FAST"
        };

        for (String strategy : validStrategies) {
            policy.setStrategy(strategy);
            assertDoesNotThrow(() -> policy.validate(),
                    "Strategy '" + strategy + "' should be valid");
        }
    }
}
