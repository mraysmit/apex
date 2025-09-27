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

package dev.mars.apex.demo.errorhandling;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple Error Handling Test - demonstrates basic error handling through APEX enrichment.
 * 
 * This test follows prompts.txt guidelines:
 * - Simple and focused on the requirement
 * - Tests actual APEX functionality using enrichmentService.enrichObject()
 * - Validates functional results with specific assertions
 * - Extends DemoTestBase for consistent test setup
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-27
 * @version 1.0
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Simple Error Handling Test")
public class SimpleErrorHandlingTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleErrorHandlingTest.class);

    @Test
    @DisplayName("Test simple validation with valid data")
    void testValidData() throws Exception {
        logger.info("=== Testing Valid Data ===");

        // Load simple configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/errorhandling/SimpleErrorHandlingTest.yaml"
        );

        // Test valid data
        Map<String, Object> validData = new HashMap<>();
        validData.put("amount", 100.0);

        Object result = enrichmentService.enrichObject(config, validData);
        assertNotNull(result, "Result should not be null");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        logger.info("Enriched data keys: " + enrichedData.keySet());
        logger.info("Enriched data: " + enrichedData);
        // Verify the validation result - now using the mapped field name
        assertEquals("VALID", enrichedData.get("status"), "Valid amount should pass");
        
        logger.info("✓ Valid data processed successfully");
        logger.info("✅ Valid data test completed");
    }

    @Test
    @DisplayName("Test simple validation with invalid data")
    void testInvalidData() throws Exception {
        logger.info("=== Testing Invalid Data ===");

        // Load simple configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/errorhandling/SimpleErrorHandlingTest.yaml"
        );

        // Test invalid data
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("amount", -100.0);

        Object result = enrichmentService.enrichObject(config, invalidData);
        assertNotNull(result, "Result should not be null for invalid data");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        assertEquals("INVALID", enrichedData.get("status"), "Invalid amount should fail");
        
        logger.info("✓ Invalid data handled gracefully");
        logger.info("✅ Invalid data test completed");
    }

    @Test
    @DisplayName("Test simple validation with null data")
    void testNullData() throws Exception {
        logger.info("=== Testing Null Data ===");

        // Load simple configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/errorhandling/SimpleErrorHandlingTest.yaml"
        );

        // Test null data
        Map<String, Object> nullData = new HashMap<>();
        nullData.put("amount", null);

        Object result = enrichmentService.enrichObject(config, nullData);
        assertNotNull(result, "Result should not be null for null data");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        assertEquals("INVALID", enrichedData.get("status"), "Null amount should fail");
        
        logger.info("✓ Null data handled gracefully");
        logger.info("✅ Null data test completed");
    }
}
