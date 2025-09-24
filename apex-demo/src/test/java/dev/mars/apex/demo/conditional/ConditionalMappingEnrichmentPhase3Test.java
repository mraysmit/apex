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
 * Test class for Phase 3: Conditional Mapping Enrichment functionality.
 * Tests the new conditional-mapping-enrichment type with priority-based processing.
 */
public class ConditionalMappingEnrichmentPhase3Test extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ConditionalMappingEnrichmentPhase3Test.class);


    @Test
    @DisplayName("Should process highest priority rule first")
    void shouldProcessHighestPriorityRuleFirst() {
        logger.info("=== Testing Highest Priority Rule Processing ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalMappingEnrichmentPhase3Test.yaml");
            
            // Test data that matches highest priority rule
            Map<String, Object> testData = new HashMap<>();
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("IS_NDF", "Y");
            
            logger.info("Testing highest priority rule with data: " + testData);
            
            // Process enrichments
            Object enrichedData = enrichmentService.enrichObject(config, testData);
            
            assertNotNull(enrichedData);
            assertTrue(enrichedData instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) enrichedData;
            
            // Should match highest priority rule
            assertEquals("HIGH_PRIORITY_NDF", resultMap.get("IS_NDF"));
            
            logger.info("✓ Highest priority rule processing completed successfully");
            logger.info("Result: " + enrichedData);

        } catch (Exception e) {
            logger.error("Failed to process highest priority rule: " + e.getMessage());
            fail("Should be able to process highest priority rule: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should process medium priority rule when high priority doesn't match")
    void shouldProcessMediumPriorityRule() {
        logger.info("=== Testing Medium Priority Rule Processing ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalMappingEnrichmentPhase3Test.yaml");
            
            // Test data that matches medium priority rule (not high priority)
            Map<String, Object> testData = new HashMap<>();
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("IS_NDF", "N");  // Not 'Y', so won't match high priority
            
            logger.info("Testing medium priority rule with data: " + testData);
            
            // Process enrichments
            Object enrichedData = enrichmentService.enrichObject(config, testData);
            
            assertNotNull(enrichedData);
            assertTrue(enrichedData instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) enrichedData;
            
            // Should match medium priority rule
            assertEquals("N_SWIFT", resultMap.get("IS_NDF"));
            
            logger.info("✓ Medium priority rule processing completed successfully");
            logger.info("Result: " + enrichedData);

        } catch (Exception e) {
            logger.error("Failed to process medium priority rule: " + e.getMessage());
            fail("Should be able to process medium priority rule: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should process other systems rule")
    void shouldProcessOtherSystemsRule() {
        logger.info("=== Testing Other Systems Rule Processing ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalMappingEnrichmentPhase3Test.yaml");
            
            // Test data that matches other systems rule
            Map<String, Object> testData = new HashMap<>();
            testData.put("SYSTEM_CODE", "OTHER");
            testData.put("IS_NDF", "Y");
            
            logger.info("Testing other systems rule with data: " + testData);
            
            // Process enrichments
            Object enrichedData = enrichmentService.enrichObject(config, testData);
            
            assertNotNull(enrichedData);
            assertTrue(enrichedData instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) enrichedData;
            
            // Should match other systems rule
            assertEquals("OTHER_SYSTEM_VALUE", resultMap.get("IS_NDF"));
            
            logger.info("✓ Other systems rule processing completed successfully");
            logger.info("Result: " + enrichedData);

        } catch (Exception e) {
            logger.error("Failed to process other systems rule: " + e.getMessage());
            fail("Should be able to process other systems rule: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should fall back to default rule")
    void shouldFallBackToDefaultRule() {
        logger.info("=== Testing Default Rule Fallback ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalMappingEnrichmentPhase3Test.yaml");
            
            // Test data that doesn't match any specific rules
            Map<String, Object> testData = new HashMap<>();
            testData.put("SYSTEM_CODE", "UNKNOWN");
            // No IS_NDF field
            
            logger.info("Testing default rule fallback with data: " + testData);
            
            // Process enrichments
            Object enrichedData = enrichmentService.enrichObject(config, testData);
            
            assertNotNull(enrichedData);
            assertTrue(enrichedData instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) enrichedData;
            
            // Should match default rule
            assertEquals("DEFAULT_NDF", resultMap.get("IS_NDF"));
            
            logger.info("✓ Default rule fallback completed successfully");
            logger.info("Result: " + enrichedData);

        } catch (Exception e) {
            logger.error("Failed to process default rule fallback: " + e.getMessage());
            fail("Should be able to process default rule fallback: " + e.getMessage());
        }
    }
}
