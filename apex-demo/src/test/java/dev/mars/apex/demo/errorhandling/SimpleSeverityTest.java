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
 * Simple Severity Test - demonstrates basic severity handling through APEX enrichment.
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
@DisplayName("Simple Severity Test")
public class SimpleSeverityTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleSeverityTest.class);

    @Test
    @DisplayName("Test simple severity handling")
    void testSimpleSeverity() throws Exception {
        logger.info("=== Testing Simple Severity Handling ===");

        // Load simple configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/errorhandling/SimpleSeverityTest.yaml"
        );

        // Test with different amounts to demonstrate severity concepts
        Map<String, Object> lowAmount = new HashMap<>();
        lowAmount.put("amount", 50.0);

        Object result = enrichmentService.enrichObject(config, lowAmount);
        assertNotNull(result, "Result should not be null");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        assertEquals("VALID", enrichedData.get("status"), "Low amount should be valid");
        
        logger.info("✓ Low amount processed as valid");

        // Test high amount
        Map<String, Object> highAmount = new HashMap<>();
        highAmount.put("amount", 1000.0);

        Object highResult = enrichmentService.enrichObject(config, highAmount);
        assertNotNull(highResult, "Result should not be null for high amount");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> highEnrichedData = (Map<String, Object>) highResult;
        assertEquals("VALID", highEnrichedData.get("status"), "High amount should still be valid");
        
        logger.info("✓ High amount processed as valid");
        logger.info("✅ Simple severity test completed");
    }
}
