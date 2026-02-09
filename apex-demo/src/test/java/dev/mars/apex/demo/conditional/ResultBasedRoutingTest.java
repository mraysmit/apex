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

import dev.mars.apex.core.config.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the "Result-Based Routing" pattern (Switch/Case).
 *
 * Verifies that a Rule Chain can route execution to different paths
 * based on a calculated value (HIGH, MEDIUM, LOW).
 */
@DisplayName("Result Based Routing Test")
public class ResultBasedRoutingTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ResultBasedRoutingTest.class.getName());

    @Test
    @DisplayName("Should route to correct path based on risk score")
    void testResultBasedRoutingPattern() {
        logger.info("=== Testing Result Based Routing Pattern ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ResultBasedRoutingTest.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("[OK] Configuration loaded successfully");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            // Scenario 1: HIGH Risk (> 80)
            // -------------------------------------------
            logger.info("--- Scenario 1: HIGH Risk (Score 90) ---");
            Map<String, Object> highInput = new HashMap<>();
            highInput.put("riskScore", 90);
            logger.info("Input Data: {}", highInput);

            RuleResult highResult = engine.evaluate(config, highInput);
            Map<String, Object> highEnriched = highResult.getEnrichedData();
            logger.info("Result Data: {}", highEnriched);

            // Verify HIGH path executed
            assertTrue(highEnriched.containsKey("transactionRejected"), 
                "HIGH path should have set 'transactionRejected'");
            assertEquals(true, highEnriched.get("transactionRejected"), 
                "Transaction should be rejected");
            
            // Verify other paths did NOT execute
            assertFalse(highEnriched.containsKey("manualReviewRequired"), "MEDIUM path should not execute");
            assertFalse(highEnriched.containsKey("autoApproved"), "LOW path should not execute");
            
            logger.info("[OK] Scenario 1 passed: Routed to HIGH path");


            // Scenario 2: MEDIUM Risk (51-80)
            // -------------------------------------------
            logger.info("--- Scenario 2: MEDIUM Risk (Score 60) ---");
            Map<String, Object> mediumInput = new HashMap<>();
            mediumInput.put("riskScore", 60);
            logger.info("Input Data: {}", mediumInput);

            RuleResult mediumResult = engine.evaluate(config, mediumInput);
            Map<String, Object> mediumEnriched = mediumResult.getEnrichedData();
            logger.info("Result Data: {}", mediumEnriched);

            // Verify MEDIUM path executed
            assertTrue(mediumEnriched.containsKey("manualReviewRequired"), 
                "MEDIUM path should have set 'manualReviewRequired'");
            assertEquals(true, mediumEnriched.get("manualReviewRequired"), 
                "Manual review should be required");

            // Verify other paths did NOT execute
            assertFalse(mediumEnriched.containsKey("transactionRejected"), "HIGH path should not execute");
            assertFalse(mediumEnriched.containsKey("autoApproved"), "LOW path should not execute");

            logger.info("[OK] Scenario 2 passed: Routed to MEDIUM path");


            // Scenario 3: LOW Risk (<= 50)
            // -------------------------------------------
            logger.info("--- Scenario 3: LOW Risk (Score 20) ---");
            Map<String, Object> lowInput = new HashMap<>();
            lowInput.put("riskScore", 20);
            logger.info("Input Data: {}", lowInput);

            RuleResult lowResult = engine.evaluate(config, lowInput);
            Map<String, Object> lowEnriched = lowResult.getEnrichedData();
            logger.info("Result Data: {}", lowEnriched);

            // Verify LOW path executed
            assertTrue(lowEnriched.containsKey("autoApproved"), 
                "LOW path should have set 'autoApproved'");
            assertEquals(true, lowEnriched.get("autoApproved"), 
                "Transaction should be auto-approved");

            // Verify other paths did NOT execute
            assertFalse(lowEnriched.containsKey("transactionRejected"), "HIGH path should not execute");
            assertFalse(lowEnriched.containsKey("manualReviewRequired"), "MEDIUM path should not execute");

            logger.info("[OK] Scenario 3 passed: Routed to LOW path");

        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }
}
