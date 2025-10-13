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
 * Test class for Advanced Conditional Processing Patterns (Guide Section 8).
 * 
 * Tests advanced patterns:
 * - Multi-stage conditional processing
 * - Cascading rule evaluations
 * - Complex routing decisions
 * 
 * Follows existing test patterns from RuleResultReferencesTest.
 * Uses minimal YAML - just enough to demonstrate each pattern.
 * 
 * @author APEX Enhancement Team
 * @since 2025-10-13
 */
@DisplayName("Advanced Conditional Patterns Test - Section 8")
public class AdvancedConditionalPatternsTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedConditionalPatternsTest.class);

    @Test
    @DisplayName("Pattern 1: Multi-stage processing with rule groups and conditional enrichments")
    void testMultiStageProcessing() {
        logger.info("=== Testing Multi-Stage Conditional Processing ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/conditional/AdvancedConditionalPatternsTest.yaml");
            assertNotNull(config, "Configuration should not be null");

            // Test high-risk scenario
            Map<String, Object> highRiskData = new HashMap<>();
            highRiskData.put("creditScore", 550);
            highRiskData.put("amount", 50000.0);

            logger.info("Testing high-risk scenario: " + highRiskData);

            Object result = enrichmentService.enrichObject(config, highRiskData);
            assertNotNull(result, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify multi-stage processing
            assertTrue(enrichedData.containsKey("requiresManualReview"), 
                      "Should have manual review flag from risk enrichment");
            assertEquals(true, enrichedData.get("requiresManualReview"),
                      "High-risk transaction should require manual review");

            assertTrue(enrichedData.containsKey("processingQueue"),
                      "Should have routing decision");
            assertEquals("COMPLIANCE_REVIEW_QUEUE", enrichedData.get("processingQueue"),
                      "High-risk customer should route to compliance queue");

            logger.info("✓ Multi-stage processing working correctly!");
            logger.info("✓ Enriched data: " + enrichedData);

        } catch (Exception e) {
            logger.error("Multi-stage processing test failed: " + e.getMessage(), e);
            fail("Multi-stage processing test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Pattern 2: Cascading conditions with priority-based routing")
    void testCascadingConditions() {
        logger.info("=== Testing Cascading Conditional Logic ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/conditional/AdvancedConditionalPatternsTest.yaml");

            // Test high-value scenario (not high-risk)
            Map<String, Object> highValueData = new HashMap<>();
            highValueData.put("creditScore", 750);  // Good credit
            highValueData.put("amount", 150000.0);  // High value

            logger.info("Testing high-value scenario: " + highValueData);

            Object result = enrichmentService.enrichObject(config, highValueData);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Should route to senior approval (high value but not high risk)
            assertEquals("SENIOR_APPROVAL_QUEUE", enrichedData.get("processingQueue"),
                      "High-value transaction should route to senior approval queue");

            logger.info("✓ Cascading conditions working correctly!");
            logger.info("✓ Routing decision: " + enrichedData.get("processingQueue"));

        } catch (Exception e) {
            logger.error("Cascading conditions test failed: " + e.getMessage(), e);
            fail("Cascading conditions test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Pattern 3: Standard processing when no special conditions match")
    void testStandardProcessing() {
        logger.info("=== Testing Standard Processing Path ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/conditional/AdvancedConditionalPatternsTest.yaml");

            // Test standard scenario
            Map<String, Object> standardData = new HashMap<>();
            standardData.put("creditScore", 700);  // Good credit
            standardData.put("amount", 5000.0);    // Normal amount

            logger.info("Testing standard scenario: " + standardData);

            Object result = enrichmentService.enrichObject(config, standardData);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Should route to auto-processing (no special conditions)
            assertEquals("AUTO_PROCESSING_QUEUE", enrichedData.get("processingQueue"),
                      "Standard transaction should route to auto-processing queue");

            // Should NOT require manual review
            assertFalse(enrichedData.containsKey("requiresManualReview") && 
                       Boolean.TRUE.equals(enrichedData.get("requiresManualReview")),
                      "Standard transaction should not require manual review");

            logger.info("✓ Standard processing path working correctly!");
            logger.info("✓ Routing decision: " + enrichedData.get("processingQueue"));

        } catch (Exception e) {
            logger.error("Standard processing test failed: " + e.getMessage(), e);
            fail("Standard processing test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Pattern 4: Combined high-risk and high-value routing")
    void testCombinedHighRiskHighValue() {
        logger.info("=== Testing Combined High-Risk + High-Value Scenario ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/conditional/AdvancedConditionalPatternsTest.yaml");

            // Test combined scenario - worst case
            Map<String, Object> combinedData = new HashMap<>();
            combinedData.put("creditScore", 550);   // High risk
            combinedData.put("amount", 150000.0);   // High value

            logger.info("Testing combined high-risk + high-value scenario: " + combinedData);

            Object result = enrichmentService.enrichObject(config, combinedData);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Should route to executive review (highest priority)
            assertEquals("EXECUTIVE_REVIEW_QUEUE", enrichedData.get("processingQueue"),
                      "High-risk + high-value should route to executive review queue");

            // Should require manual review
            assertEquals(true, enrichedData.get("requiresManualReview"),
                      "Combined scenario should require manual review");

            logger.info("✓ Combined scenario routing working correctly!");
            logger.info("✓ Routing decision: " + enrichedData.get("processingQueue"));

        } catch (Exception e) {
            logger.error("Combined scenario test failed: " + e.getMessage(), e);
            fail("Combined scenario test failed: " + e.getMessage());
        }
    }
}

