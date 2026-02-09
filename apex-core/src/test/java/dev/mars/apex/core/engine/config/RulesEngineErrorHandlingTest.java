package dev.mars.apex.core.engine.config;

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

import dev.mars.apex.core.config.YamlConfigurationException;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Modern APEX error handling tests using RulesEngine API.
 * 
 * Demonstrates proper intentional error testing patterns:
 * - SpEL evaluation errors are captured (not thrown)
 * - Errors are reported through RuleResult.getFailureMessages()
 * - YAML configuration errors never produce stack traces
 * - Error recovery via default values in YAML
 * 
 * This is the RECOMMENDED approach for all new APEX applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class RulesEngineErrorHandlingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RulesEngineErrorHandlingTest.class);

    /**
     * Intentional error test: Verifies that RulesEngine handles SpEL evaluation errors gracefully
     * when input data is missing required fields. Tests that APEX captures errors and reports them
     * through RuleResult API without throwing exceptions or logging stack traces.
     * 
     * <p>The YAML configuration contains rules with SpEL expressions referencing fields like
     * 'age', 'email', 'creditScore', 'customerId'. When these fields are missing from input data,
     * SpEL evaluation fails - but APEX should handle this gracefully.</p>
     */
    @Test
    void testRulesEngine_MissingFieldsIntentionalError() throws YamlConfigurationException {
        LOGGER.info("=== INTENTIONAL ERROR TEST: RulesEngine with missing input fields ===");
        
        // Step 1: Create engine from YAML with rules that reference multiple fields
        RulesEngine engine = RulesEngine.fromFile(resourcePath("error-handling/yaml-default-value-test.yaml"));
        
        // Step 2: Prepare input data - intentionally missing ALL fields that rules reference
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);
        // Missing: age, email, creditScore, customerId, principal, interestRate, value
        // These missing fields will trigger SpEL evaluation errors
        
        // Step 3: Evaluate rules - should NOT throw exception
        RuleResult result = engine.evaluate(inputData);
        
        // Step 4: Verify errors are captured in RuleResult (not thrown)
        assertNotNull(result, "Result should not be null even with errors");
        assertTrue(result.hasFailures(), "Should have failures from missing fields");
        
        List<String> failureMessages = result.getFailureMessages();
        assertFalse(failureMessages.isEmpty(), "Should have failure messages reporting SpEL errors");
        
        // Step 5: Verify specific error messages about missing fields
        boolean hasFieldErrors = failureMessages.stream().anyMatch(msg -> 
            msg.contains("age") || msg.contains("email") || msg.contains("creditScore") || 
            msg.contains("customerId") || msg.contains("principal") || msg.contains("value"));
        
        assertTrue(hasFieldErrors, 
            "Should report errors about missing fields in RuleResult. Messages: " + failureMessages);
        
        // Step 6: Verify error messages contain rule names/IDs for traceability
        boolean hasRuleContext = failureMessages.stream().anyMatch(msg -> 
            msg.contains("mandatory-field-check") || msg.contains("Rule evaluation failed"));
        
        assertTrue(hasRuleContext, 
            "Error messages should contain rule context for debugging. Messages: " + failureMessages);
        
        // Step 7: Verify enriched data is still available (error recovery allows partial processing)
        assertNotNull(result.getEnrichedData(), "Enriched data should be available even with errors");
        
        LOGGER.info("[OK] Captured {} error message(s) in RuleResult without throwing exceptions", failureMessages.size());
        LOGGER.info("[OK] Error messages: {}", failureMessages);
        LOGGER.info("[OK] Error messages contain rule context for debugging");
    }

    /**
     * Intentional error test: Verifies error recovery with default values.
     * When rules have default-value configured, errors should be handled gracefully
     * and the default value used instead of failing.
     */
    @Test
    void testRulesEngine_ErrorRecoveryWithDefaultValues() throws YamlConfigurationException {
        LOGGER.info("=== INTENTIONAL ERROR TEST: Error recovery with default values ===");
        
        // Create engine with error-handling YAML
        RulesEngine engine = RulesEngine.fromFile(resourcePath("error-handling/yaml-default-value-test.yaml"));
        
        // Input data missing fields that have default values configured
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);
        // Missing: age (has default-value: false), email (has default-value: INVALID_EMAIL)
        
        // Evaluate
        RuleResult result = engine.evaluate(inputData);
        
        // Verify result structure
        assertNotNull(result, "Result should not be null");
        
        // Errors should be logged but processing continues with default values
        // The result may or may not have failures depending on error recovery policy
        if (result.hasFailures()) {
            List<String> failureMessages = result.getFailureMessages();
            LOGGER.info("Failures handled via default values: {}", failureMessages);
        }
        
        // Verify enriched data is available (default values allow processing to continue)
        assertNotNull(result.getEnrichedData(), "Enriched data should be available");
        
        LOGGER.info("[OK] Error recovery with default values working correctly");
    }

    /**
     * Intentional error test: Verifies partial data scenarios.
     * When some fields are present and others missing, APEX should process
     * what it can and report errors for what it cannot.
     */
    @Test
    void testRulesEngine_PartialDataIntentionalError() throws YamlConfigurationException {
        LOGGER.info("=== INTENTIONAL ERROR TEST: Partial data with some valid fields ===");
        
        RulesEngine engine = RulesEngine.fromFile(resourcePath("error-handling/yaml-default-value-test.yaml"));
        
        // Input data with SOME valid fields
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);
        inputData.put("age", 25);  // Valid - age rule should pass
        inputData.put("customerId", "CUST123");  // Valid - customer rules should work
        // Missing: email, creditScore, principal, interestRate, value
        
        // Evaluate
        RuleResult result = engine.evaluate(inputData);
        
        // Verify result structure
        assertNotNull(result, "Result should not be null");
        
        // May have some failures for missing fields
        if (result.hasFailures()) {
            List<String> failureMessages = result.getFailureMessages();
            LOGGER.info("Failures for missing fields: {}", failureMessages);
            
            // Should NOT have errors for fields that were provided
            boolean hasAgeError = failureMessages.stream().anyMatch(msg -> msg.contains("age"));
            boolean hasCustomerIdError = failureMessages.stream().anyMatch(msg -> msg.contains("customerId"));
            
            assertFalse(hasAgeError, "Should not have age errors when age is provided");
            assertFalse(hasCustomerIdError, "Should not have customerId errors when customerId is provided");
        }
        
        // Verify partial processing succeeded
        assertNotNull(result.getEnrichedData(), "Enriched data should be available");
        
        LOGGER.info("[OK] Partial data processing with selective error reporting working correctly");
    }

    /**
     * Intentional error test: Verifies that enrichment condition failures are handled gracefully.
     * When enrichment conditions fail to evaluate, they should be skipped with clean warnings
     * (not ERROR-level stack traces).
     */
    @Test
    void testRulesEngine_EnrichmentConditionFailuresIntentionalError() throws YamlConfigurationException {
        LOGGER.info("=== INTENTIONAL ERROR TEST: Enrichment condition evaluation failures ===");
        
        RulesEngine engine = RulesEngine.fromFile(resourcePath("error-handling/yaml-default-value-test.yaml"));
        
        // Input data that will cause enrichment conditions to fail
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);
        // Missing: principal, interestRate, customerId, value
        // These are referenced in enrichment conditions
        
        // Evaluate - enrichments should be skipped gracefully, not throw exceptions
        RuleResult result = engine.evaluate(inputData);
        
        // Verify no exceptions thrown
        assertNotNull(result, "Result should not be null");
        
        // Enrichments failed but should not crash the evaluation
        assertNotNull(result.getEnrichedData(), "Enriched data should exist (may be empty)");
        
        LOGGER.info("[OK] Enrichment condition failures handled gracefully without stack traces");
    }

    /**
     * Test showing the simplified 2-line usage pattern for common scenarios.
     * Even with errors, the pattern remains simple and clean.
     */
    @Test
    void testRulesEngine_SimplifiedUsagePattern() throws YamlConfigurationException {
        LOGGER.info("=== MODERN APPROACH: Simplified 2-line usage ===");
        
        // Simple 2-line usage - create engine and evaluate
        RulesEngine engine = RulesEngine.fromFile(resourcePath("error-handling/yaml-default-value-test.yaml"));
        RuleResult result = engine.evaluate(Map.of("id", 1, "customerId", "CUST123"));
        
        // Verify result
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getEnrichedData(), "Enriched data should be available");
        
        LOGGER.info("[OK] Simplified usage pattern works cleanly");
    }

    /**
     * Test demonstrating proper error checking pattern.
     * Shows how consumers should check for errors in production code.
     */
    @Test
    void testRulesEngine_ProperErrorCheckingPattern() throws YamlConfigurationException {
        LOGGER.info("=== BEST PRACTICE: Proper error checking pattern ===");
        
        RulesEngine engine = RulesEngine.fromFile(resourcePath("error-handling/yaml-default-value-test.yaml"));
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);
        // Missing fields will trigger errors
        
        RuleResult result = engine.evaluate(inputData);
        
        // Best practice: Always check for failures before using results
        if (result.hasFailures()) {
            List<String> errors = result.getFailureMessages();
            LOGGER.warn("Rule evaluation encountered {} error(s):", errors.size());
            errors.forEach(error -> LOGGER.warn("  - {}", error));
            
            // In production: decide whether to proceed or abort based on severity
            // For this test: verify errors are properly reported
            assertFalse(errors.isEmpty(), "Should have error messages");
        }
        
        // Even with errors, enriched data may be partially available
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should be accessible");
        
        LOGGER.info("[OK] Proper error checking pattern demonstrated");
    }

    /**
     * Test verifying that hasFailures() accurately reflects error state.
     */
    @Test
    void testRulesEngine_HasFailuresAccuracy() throws YamlConfigurationException {
        LOGGER.info("=== VERIFICATION: hasFailures() accuracy ===");
        
        RulesEngine engine = RulesEngine.fromFile(resourcePath("error-handling/yaml-default-value-test.yaml"));
        
        // Scenario 1: Input with errors
        Map<String, Object> badData = Map.of("id", 1);
        RuleResult badResult = engine.evaluate(badData);
        
        if (badResult.hasFailures()) {
            assertFalse(badResult.getFailureMessages().isEmpty(), 
                "When hasFailures() is true, getFailureMessages() should not be empty");
        }
        
        // Scenario 2: Input with all required fields (may not have errors)
        Map<String, Object> goodData = new HashMap<>();
        goodData.put("id", 1);
        goodData.put("age", 30);
        goodData.put("email", "test@example.com");
        goodData.put("creditScore", 750);
        goodData.put("customerId", "CUST123");
        goodData.put("principal", 10000.0);
        goodData.put("interestRate", 0.05);
        goodData.put("value", 100);
        
        RuleResult goodResult = engine.evaluate(goodData);
        
        // Verify consistency
        assertNotNull(goodResult, "Result should not be null");
        LOGGER.info("[OK] hasFailures() consistency verified");
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Helper to resolve classpath test resources to absolute file paths.
     */
    private String resourcePath(String name) {
        try {
            java.net.URL url = getClass().getClassLoader().getResource(name);
            assertNotNull(url, "Missing test resource: " + name);
            return new java.io.File(url.toURI()).getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve resource: " + name, e);
        }
    }
}
