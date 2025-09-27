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

package dev.mars.apex.core.service.engine;

import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
import dev.mars.apex.core.config.error.SeverityRecoveryPolicy;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for configurable error recovery functionality.
 * 
 * This test demonstrates the complete configurable error recovery system
 * working end-to-end with different severity levels and recovery policies.
 */
@DisplayName("Configurable Error Recovery Integration Tests")
class ConfigurableErrorRecoveryIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurableErrorRecoveryIntegrationTest.class);

    private UnifiedRuleEvaluator evaluator;
    private ErrorRecoveryConfig config;

    @BeforeEach
    void setUp() {
        config = new ErrorRecoveryConfig();
        evaluator = new UnifiedRuleEvaluator(
            new SpelExpressionParser(),
            new ErrorRecoveryService(),
            new RulePerformanceMonitor(),
            config
        );
    }

    @Test
    @DisplayName("Should use default backward-compatible configuration")
    void testDefaultBackwardCompatibleConfiguration() {
        logger.info("=== Testing Default Backward-Compatible Configuration ===");

        // Test ERROR severity - should NOT recover (backward compatible)
        Rule errorRule = new Rule("error-test", "#data.missing != null", "Error test", SeverityConstants.ERROR);
        Map<String, Object> facts = createEmptyFacts();
        
        RuleResult errorResult = evaluator.evaluateRule(errorRule, facts);
        
        assertEquals(RuleResult.ResultType.ERROR, errorResult.getResultType(),
                "ERROR severity should return ERROR result (no recovery)");
        assertEquals(SeverityConstants.ERROR, errorResult.getSeverity(),
                "ERROR severity should be preserved");

        // Test WARNING severity - should recover
        Rule warningRule = new Rule("warning-test", "#data.missing != null", "Warning test", SeverityConstants.WARNING);
        
        RuleResult warningResult = evaluator.evaluateRule(warningRule, facts);
        
        assertEquals(RuleResult.ResultType.NO_MATCH, warningResult.getResultType(),
                "WARNING severity should be recovered to NO_MATCH");

        // Test INFO severity - should recover
        Rule infoRule = new Rule("info-test", "#data.missing != null", "Info test", SeverityConstants.INFO);
        
        RuleResult infoResult = evaluator.evaluateRule(infoRule, facts);
        
        assertEquals(RuleResult.ResultType.NO_MATCH, infoResult.getResultType(),
                "INFO severity should be recovered to NO_MATCH");

        logger.info("✅ Default backward-compatible configuration working correctly");
    }

    @Test
    @DisplayName("Should support custom error recovery configuration")
    void testCustomErrorRecoveryConfiguration() {
        logger.info("=== Testing Custom Error Recovery Configuration ===");

        // Create custom configuration - enable recovery for ERROR severity
        SeverityRecoveryPolicy customErrorPolicy = new SeverityRecoveryPolicy(true, "CONTINUE_WITH_DEFAULT");
        config.setSeverityPolicy(SeverityConstants.ERROR, customErrorPolicy);

        // Test ERROR severity with custom configuration - should now recover
        Rule errorRule = new Rule("custom-error-test", "#data.missing != null", "Custom error test", SeverityConstants.ERROR);
        Map<String, Object> facts = createEmptyFacts();
        
        RuleResult result = evaluator.evaluateRule(errorRule, facts);
        
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(),
                "ERROR severity should be recovered with custom configuration");

        logger.info("✅ Custom error recovery configuration working correctly");
    }

    @Test
    @DisplayName("Should support global enable/disable of error recovery")
    void testGlobalErrorRecoveryEnableDisable() {
        logger.info("=== Testing Global Error Recovery Enable/Disable ===");

        // Disable error recovery globally
        config.setEnabled(false);

        // Test WARNING severity - should NOT recover when globally disabled
        Rule warningRule = new Rule("global-disabled-test", "#data.missing != null", "Global disabled test", SeverityConstants.WARNING);
        Map<String, Object> facts = createEmptyFacts();
        
        RuleResult result = evaluator.evaluateRule(warningRule, facts);
        
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                "WARNING severity should return ERROR when recovery is globally disabled");
        assertEquals(SeverityConstants.WARNING, result.getSeverity(),
                "WARNING severity should be preserved");

        // Re-enable globally
        config.setEnabled(true);

        // Test WARNING severity - should recover when globally enabled
        RuleResult recoveredResult = evaluator.evaluateRule(warningRule, facts);
        
        assertEquals(RuleResult.ResultType.NO_MATCH, recoveredResult.getResultType(),
                "WARNING severity should be recovered when globally enabled");

        logger.info("✅ Global error recovery enable/disable working correctly");
    }

    @Test
    @DisplayName("Should support different recovery strategies per severity")
    void testDifferentRecoveryStrategiesPerSeverity() {
        logger.info("=== Testing Different Recovery Strategies Per Severity ===");

        // Configure different strategies for different severities
        SeverityRecoveryPolicy errorPolicy = new SeverityRecoveryPolicy(true, "RETRY_WITH_SAFE_EXPRESSION");
        SeverityRecoveryPolicy warningPolicy = new SeverityRecoveryPolicy(true, "CONTINUE_WITH_DEFAULT");
        SeverityRecoveryPolicy infoPolicy = new SeverityRecoveryPolicy(true, "SKIP_RULE");

        config.setSeverityPolicy(SeverityConstants.ERROR, errorPolicy);
        config.setSeverityPolicy(SeverityConstants.WARNING, warningPolicy);
        config.setSeverityPolicy(SeverityConstants.INFO, infoPolicy);

        Map<String, Object> facts = createEmptyFacts();

        // Test ERROR with RETRY_WITH_SAFE_EXPRESSION strategy
        Rule errorRule = new Rule("strategy-error-test", "#data.missing != null", "Strategy error test", SeverityConstants.ERROR);
        RuleResult errorResult = evaluator.evaluateRule(errorRule, facts);
        
        // Should be recovered (strategy doesn't affect result type in current implementation)
        assertEquals(RuleResult.ResultType.NO_MATCH, errorResult.getResultType(),
                "ERROR severity should be recovered with RETRY_WITH_SAFE_EXPRESSION strategy");

        // Test WARNING with CONTINUE_WITH_DEFAULT strategy
        Rule warningRule = new Rule("strategy-warning-test", "#data.missing != null", "Strategy warning test", SeverityConstants.WARNING);
        RuleResult warningResult = evaluator.evaluateRule(warningRule, facts);
        
        assertEquals(RuleResult.ResultType.NO_MATCH, warningResult.getResultType(),
                "WARNING severity should be recovered with CONTINUE_WITH_DEFAULT strategy");

        // Test INFO with SKIP_RULE strategy
        Rule infoRule = new Rule("strategy-info-test", "#data.missing != null", "Strategy info test", SeverityConstants.INFO);
        RuleResult infoResult = evaluator.evaluateRule(infoRule, facts);
        
        assertEquals(RuleResult.ResultType.NO_MATCH, infoResult.getResultType(),
                "INFO severity should be recovered with SKIP_RULE strategy");

        logger.info("✅ Different recovery strategies per severity working correctly");
    }

    @Test
    @DisplayName("Should handle unknown severities gracefully")
    void testUnknownSeveritiesHandling() {
        logger.info("=== Testing Unknown Severities Handling ===");

        // Test with unknown severity
        Rule unknownRule = new Rule("unknown-test", "#data.missing != null", "Unknown test", "UNKNOWN");
        Map<String, Object> facts = createEmptyFacts();
        
        RuleResult result = evaluator.evaluateRule(unknownRule, facts);
        
        // Unknown severity should not be recovered (default behavior)
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                "Unknown severity should return ERROR result (no recovery)");
        assertEquals("UNKNOWN", result.getSeverity(),
                "Unknown severity should be preserved");

        logger.info("✅ Unknown severities handled gracefully");
    }

    @Test
    @DisplayName("Should preserve error recovery configuration access")
    void testErrorRecoveryConfigurationAccess() {
        logger.info("=== Testing Error Recovery Configuration Access ===");

        // Verify we can access the configuration
        ErrorRecoveryConfig retrievedConfig = evaluator.getErrorRecoveryConfig();
        
        assertNotNull(retrievedConfig, "Should be able to access error recovery configuration");
        assertSame(config, retrievedConfig, "Should return the same configuration instance");

        // Verify configuration properties
        assertTrue(retrievedConfig.isEnabled(), "Configuration should be enabled by default");
        assertEquals("CONTINUE_WITH_DEFAULT", retrievedConfig.getDefaultStrategy(),
                "Default strategy should be CONTINUE_WITH_DEFAULT");
        assertTrue(retrievedConfig.isLogRecoveryAttempts(),
                "Log recovery attempts should be enabled by default");
        assertTrue(retrievedConfig.isMetricsEnabled(),
                "Metrics should be enabled by default");

        logger.info("✅ Error recovery configuration access working correctly");
    }

    @Test
    @DisplayName("Should demonstrate complete end-to-end configurable recovery")
    void testCompleteEndToEndConfigurableRecovery() {
        logger.info("=== Testing Complete End-to-End Configurable Recovery ===");

        // Create a comprehensive configuration
        ErrorRecoveryConfig comprehensiveConfig = new ErrorRecoveryConfig();
        
        // ERROR: No recovery (strict)
        SeverityRecoveryPolicy errorPolicy = new SeverityRecoveryPolicy(false, "FAIL_FAST");
        comprehensiveConfig.setSeverityPolicy(SeverityConstants.ERROR, errorPolicy);
        
        // WARNING: Recovery with retry
        SeverityRecoveryPolicy warningPolicy = new SeverityRecoveryPolicy(true, "RETRY_WITH_SAFE_EXPRESSION", 1, 100L);
        comprehensiveConfig.setSeverityPolicy(SeverityConstants.WARNING, warningPolicy);
        
        // INFO: Recovery with default
        SeverityRecoveryPolicy infoPolicy = new SeverityRecoveryPolicy(true, "CONTINUE_WITH_DEFAULT", 0, 50L);
        comprehensiveConfig.setSeverityPolicy(SeverityConstants.INFO, infoPolicy);

        // Create evaluator with comprehensive configuration
        UnifiedRuleEvaluator comprehensiveEvaluator = new UnifiedRuleEvaluator(
            new SpelExpressionParser(),
            new ErrorRecoveryService(),
            new RulePerformanceMonitor(),
            comprehensiveConfig
        );

        Map<String, Object> facts = createEmptyFacts();

        // Test all severities
        String[] severities = {SeverityConstants.ERROR, SeverityConstants.WARNING, SeverityConstants.INFO};
        RuleResult.ResultType[] expectedResults = {
            RuleResult.ResultType.ERROR,    // ERROR - no recovery
            RuleResult.ResultType.NO_MATCH, // WARNING - recovery
            RuleResult.ResultType.NO_MATCH  // INFO - recovery
        };

        for (int i = 0; i < severities.length; i++) {
            String severity = severities[i];
            RuleResult.ResultType expectedResult = expectedResults[i];
            
            Rule rule = new Rule("comprehensive-test-" + severity.toLowerCase(), 
                               "#data.missing != null", 
                               "Comprehensive test for " + severity, 
                               severity);
            
            RuleResult result = comprehensiveEvaluator.evaluateRule(rule, facts);
            
            assertEquals(expectedResult, result.getResultType(),
                    severity + " severity should return " + expectedResult + " result");
        }

        logger.info("✅ Complete end-to-end configurable recovery working correctly");
    }

    private Map<String, Object> createEmptyFacts() {
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", new HashMap<String, Object>());
        return facts;
    }
}
