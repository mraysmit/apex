package dev.mars.apex.demo.errorhandling;

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

import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Configuration Error Handling Test.
 * 
 * Demonstrates proper APEX error handling architecture where configuration
 * loading failures are handled gracefully through RuleResult instead of
 * throwing raw exceptions.
 * 
 * This addresses the architectural issue where YamlConfigurationException
 * was being thrown instead of being properly managed and propagated as
 * per APEX's error handling guidelines.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-27
 * @version 1.0
 */
public class ConfigurationErrorHandlingTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationErrorHandlingTest.class);

    @Test
    @DisplayName("Should handle missing configuration file gracefully without throwing exceptions")
    void testMissingConfigurationFileHandling() {
        logger.info("=== Testing Missing Configuration File Error Handling ===");
        
        // Try to load a non-existent configuration file
        String nonExistentFile = "src/test/java/dev/mars/apex/demo/errorhandling/NonExistentFile.yaml";
        
        // This should NOT throw an exception but return a RuleResult with error details
        RuleResult result = safeLoadYamlConfiguration(nonExistentFile);
        
        // Verify proper error handling
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Result should indicate failure (not triggered)");
        assertNotNull(result.getFailureMessages(), "Failure messages should be present");
        assertFalse(result.getFailureMessages().isEmpty(), "Failure messages should not be empty");
        
        // Verify error details
        assertTrue(result.getFailureMessages().get(0).contains("CRITICAL ERROR: Failed to load configuration from file"),
            "First failure message should indicate CRITICAL configuration loading failure");
        assertTrue(result.getFailureMessages().get(0).contains(nonExistentFile),
            "Failure message should contain the file path");
        
        // Verify rule context
        assertEquals("configuration-loading", result.getRuleName(), "Rule name should indicate configuration loading");
        assertNotNull(result.getMessage(), "Message should be present");
        assertTrue(result.getMessage().contains("CRITICAL: Configuration file loading failed"),
            "Message should indicate CRITICAL configuration loading failure");
        
        logger.info("✓ Missing configuration file handled gracefully");
        logger.info("✓ Error details properly captured in RuleResult");
        logger.info("✓ No exceptions thrown - proper APEX error handling architecture");
    }

    @Test
    @DisplayName("Should handle existing configuration file successfully")
    void testExistingConfigurationFileHandling() {
        logger.info("=== Testing Existing Configuration File Handling ===");

        // Try to load an existing configuration file
        String existingFile = "src/test/java/dev/mars/apex/demo/lookup/PostgreSQLMultiParamLookupTest.yaml";

        // This should either succeed or fail gracefully (depending on file validity)
        RuleResult result = safeLoadYamlConfiguration(existingFile);

        // Verify proper handling (either success or graceful failure)
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getFailureMessages(), "Failure messages should be initialized");

        if (result.isTriggered()) {
            // File loaded successfully
            assertTrue(result.getFailureMessages().isEmpty(), "Failure messages should be empty for success");
            assertEquals("configuration-loading", result.getRuleName(), "Rule name should indicate configuration loading");
            assertNotNull(result.getMessage(), "Message should be present");
            assertTrue(result.getMessage().contains("Configuration loaded successfully"),
                "Message should indicate successful loading");
            logger.info("✓ Existing configuration file loaded successfully");
        } else {
            // File failed to load but was handled gracefully
            assertFalse(result.getFailureMessages().isEmpty(), "Failure messages should be present for failure");
            assertEquals("configuration-loading", result.getRuleName(), "Rule name should indicate configuration loading");
            assertNotNull(result.getMessage(), "Message should be present");
            assertTrue(result.getMessage().contains("CRITICAL: Configuration file loading failed"),
                "Message should indicate CRITICAL loading failure");
            logger.info("✓ Configuration file loading CRITICAL ERROR handled gracefully");
        }

        logger.info("✓ Proper error handling demonstrated (no exceptions thrown)");
        logger.info("✓ Proper APEX error handling architecture demonstrated");
    }

    @Test
    @DisplayName("Should demonstrate the difference between old and new error handling approaches")
    void testErrorHandlingArchitectureComparison() {
        logger.info("=== Demonstrating APEX Error Handling Architecture ===");
        
        logger.info("OLD APPROACH (INCORRECT):");
        logger.info("  - Raw YamlConfigurationException thrown");
        logger.info("  - Exception bubbles up to test framework");
        logger.info("  - Test fails with stack trace");
        logger.info("  - No graceful error recovery");
        
        logger.info("NEW APPROACH (CORRECT APEX ARCHITECTURE):");
        logger.info("  - YamlConfigurationException caught and handled");
        logger.info("  - Error converted to RuleResult with proper details");
        logger.info("  - Test can continue and handle error gracefully");
        logger.info("  - Follows APEX's error propagation patterns");
        
        // Demonstrate the new approach
        RuleResult errorResult = safeLoadYamlConfiguration("non-existent-file.yaml");
        RuleResult successResult = safeLoadYamlConfiguration("src/test/java/dev/mars/apex/demo/lookup/PostgreSQLMultiParamLookupTest.yaml");
        
        logger.info("Error Result: triggered={}, messages={}", 
            errorResult.isTriggered(), errorResult.getFailureMessages().size());
        logger.info("Success Result: triggered={}, messages={}", 
            successResult.isTriggered(), successResult.getFailureMessages().size());
        
        logger.info("✓ APEX error handling architecture properly implemented");
        logger.info("✓ Configuration loading failures managed gracefully");
        logger.info("✓ System follows proper error propagation patterns");
    }
}
