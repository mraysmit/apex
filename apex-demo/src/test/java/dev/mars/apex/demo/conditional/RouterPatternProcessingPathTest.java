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
 * Test class for the "Router" pattern with processingPath.
 */
@DisplayName("Router Pattern Processing Path Test")
public class RouterPatternProcessingPathTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RouterPatternProcessingPathTest.class.getName());

    @Test
    @DisplayName("Should route correctly based on processingPath")
    void testRouterPattern() {
        logger.info("=== Testing Router Pattern (Processing Path) ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/RouterPatternProcessingPathTest.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("[OK] Configuration loaded successfully");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            // Scenario 1: MANUAL_REVIEW
            logger.info("--- Scenario 1: MANUAL_REVIEW ---");
            Map<String, Object> manualInput = new HashMap<>();
            manualInput.put("processingPath", "MANUAL_REVIEW");
            logger.info("Input Data: {}", manualInput);

            RuleResult manualResult = engine.evaluate(config, manualInput);
            Map<String, Object> manualEnriched = manualResult.getEnrichedData();
            logger.info("Result Data: {}", manualEnriched);

            assertTrue(manualEnriched.containsKey("reviewStatus"), "Should have reviewStatus field");
            assertEquals(true, manualEnriched.get("reviewStatus"), "Review status flag should be true");
            logger.info("[OK] Scenario 1 passed: Correctly routed to MANUAL_REVIEW");


            // Scenario 2: AUTO_APPROVE
            logger.info("--- Scenario 2: AUTO_APPROVE ---");
            Map<String, Object> approveInput = new HashMap<>();
            approveInput.put("processingPath", "AUTO_APPROVE");
            logger.info("Input Data: {}", approveInput);

            RuleResult approveResult = engine.evaluate(config, approveInput);
            Map<String, Object> approveEnriched = approveResult.getEnrichedData();
            logger.info("Result Data: {}", approveEnriched);

            assertTrue(approveEnriched.containsKey("approvalStatus"), "Should have approvalStatus field");
            assertEquals(true, approveEnriched.get("approvalStatus"), "Approval status flag should be true");
            logger.info("[OK] Scenario 2 passed: Correctly routed to AUTO_APPROVE");


            // Scenario 3: STANDARD_PROCESS
            logger.info("--- Scenario 3: STANDARD_PROCESS ---");
            Map<String, Object> standardInput = new HashMap<>();
            standardInput.put("processingPath", "STANDARD_PROCESS");
            logger.info("Input Data: {}", standardInput);

            RuleResult standardResult = engine.evaluate(config, standardInput);
            Map<String, Object> standardEnriched = standardResult.getEnrichedData();
            logger.info("Result Data: {}", standardEnriched);

            assertTrue(standardEnriched.containsKey("validationStatus"), "Should have validationStatus field");
            assertEquals(true, standardEnriched.get("validationStatus"), "Validation status flag should be true");
            logger.info("[OK] Scenario 3 passed: Correctly routed to STANDARD_PROCESS");

        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }
}
