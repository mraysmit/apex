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

package dev.mars.apex.demo.conditional;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Phase 2: Conditional Mappings (Design V1) functionality.
 * Tests the new conditional-mappings syntax in field-enrichment.
 */
public class ConditionalMappingsPhase2Test extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ConditionalMappingsPhase2Test.class);

    @Test
    @DisplayName("Should load and validate conditional-mappings YAML syntax")
    void shouldLoadConditionalMappingsYaml() {
        logger.info("=== Testing Conditional Mappings YAML Loading ===");

        try {
            // Load YAML configuration with conditional-mappings syntax
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalMappingsPhase2Test.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("✓ Configuration loaded successfully: " + config.getMetadata().getName());

            // Verify enrichments are present
            assertNotNull(config.getEnrichments(), "Enrichments should not be null");
            assertFalse(config.getEnrichments().isEmpty(), "Enrichments should not be empty");

            // Find the conditional mapping enrichment
            var conditionalEnrichment = config.getEnrichments().stream()
                    .filter(e -> "test-conditional-mapping".equals(e.getId()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(conditionalEnrichment, "Test conditional mapping enrichment should be present");
            assertEquals("field-enrichment", conditionalEnrichment.getType(), "Should be field-enrichment type");

            // Verify conditional-mappings are present
            assertNotNull(conditionalEnrichment.getConditionalMappings(), "Conditional mappings should not be null");
            assertFalse(conditionalEnrichment.getConditionalMappings().isEmpty(), "Conditional mappings should not be empty");

            logger.info("✓ Conditional mappings syntax validated successfully");

        } catch (Exception e) {
            logger.error("Failed to load conditional mappings YAML: " + e.getMessage());
            fail("Should be able to load conditional mappings YAML: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should process OR conditions using conditional mappings")
    void shouldProcessOrConditions() {
        logger.info("=== Testing OR Conditions with Conditional Mappings ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalMappingsPhase2Test.yaml");
            assertNotNull(config, "Configuration should not be null");

            // Create test data that should match first conditional mapping (OR conditions)
            Map<String, Object> data = new HashMap<>();
            data.put("testField", "VALUE1");  // Should match first OR condition
            data.put("systemCode", "OTHER");

            logger.info("Testing OR condition mapping with data: " + data);

            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, data);
            assertNotNull(result, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify the conditional mapping was applied
            // The first conditional mapping should set result to 'OR_MATCHED'
            assertEquals("OR_MATCHED", enrichedData.get("result"), "Result should be 'OR_MATCHED' for OR condition");

            logger.info("✓ OR condition processing completed successfully");
            logger.info("Result: " + enrichedData);

        } catch (Exception e) {
            logger.error("Failed to process OR conditions: " + e.getMessage());
            fail("Should be able to process OR conditions: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should process second OR condition using conditional mappings")
    void shouldProcessSecondOrCondition() {
        logger.info("=== Testing Second OR Condition with Conditional Mappings ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalMappingsPhase2Test.yaml");
            assertNotNull(config, "Configuration should not be null");

            // Create test data that should match second OR condition
            Map<String, Object> data = new HashMap<>();
            data.put("testField", "VALUE2");  // Should match second OR condition
            data.put("systemCode", "OTHER");

            logger.info("Testing second OR condition mapping with data: " + data);

            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, data);
            assertNotNull(result, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify the conditional mapping was applied
            // The first conditional mapping should set result to 'OR_MATCHED'
            assertEquals("OR_MATCHED", enrichedData.get("result"), "Result should be 'OR_MATCHED' for second OR condition");

            logger.info("✓ Second OR condition processing completed successfully");
            logger.info("Result: " + enrichedData);

        } catch (Exception e) {
            logger.error("Failed to process second OR condition: " + e.getMessage());
            fail("Should be able to process second OR condition: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should process AND conditions using conditional mappings")
    void shouldProcessAndConditions() {
        logger.info("=== Testing AND Conditions with Conditional Mappings ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalMappingsPhase2Test.yaml");
            assertNotNull(config, "Configuration should not be null");

            // Create test data that should match second conditional mapping (AND conditions)
            Map<String, Object> data = new HashMap<>();
            data.put("testField", "VALUE3");  // Should match first AND condition
            data.put("systemCode", "TEST");   // Should match second AND condition

            logger.info("Testing AND condition mapping with data: " + data);

            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, data);
            assertNotNull(result, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify the conditional mapping was applied
            // The second conditional mapping should set result to 'AND_MATCHED'
            assertEquals("AND_MATCHED", enrichedData.get("result"), "Result should be 'AND_MATCHED' for AND conditions");
            assertEquals("TEST", enrichedData.get("system"), "System should be set to 'TEST'");

            logger.info("✓ AND condition processing completed successfully");
            logger.info("Result: " + enrichedData);

        } catch (Exception e) {
            logger.error("Failed to process AND conditions: " + e.getMessage());
            fail("Should be able to process AND conditions: " + e.getMessage());
        }
    }
}
