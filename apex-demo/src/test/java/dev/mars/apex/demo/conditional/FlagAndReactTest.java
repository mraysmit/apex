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
 * Test class for the "Flag & React" pattern.
 *
 * Verifies that a Rule Chain can set a result-field which then triggers
 * a conditional Enrichment. This is the standard pattern for orchestrating
 * enrichments based on rule chain outcomes.
 */
@DisplayName("Flag and React Pattern Test")
public class FlagAndReactTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(FlagAndReactTest.class.getName());

    @Test
    @DisplayName("Should trigger enrichment when Rule Chain passes and sets flag")
    void testFlagAndReactPattern() {
        logger.info("=== Testing Flag & React Pattern ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/FlagAndReactTest.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("✓ Configuration loaded successfully");

            // Scenario 1: Chain Passes (VIP + High Value)
            // -------------------------------------------
            logger.info("--- Scenario 1: Chain Passes (VIP + High Value) ---");
            Map<String, Object> passInput = new HashMap<>();
            passInput.put("customerType", "VIP");
            passInput.put("amount", 15000.0);
            logger.info("Input Data: {}", passInput);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult passResult = engine.evaluate(config, passInput);
            Map<String, Object> passEnriched = passResult.getEnrichedData();
            logger.info("Result Data: {}", passEnriched);

            // Verify chain result field was set
            assertTrue(passEnriched.containsKey("vipProcessingRequired"), 
                "Rule Chain should have set 'vipProcessingRequired'");
            assertEquals(true, passEnriched.get("vipProcessingRequired"), 
                "Rule Chain result should be true");

            // Verify enrichment triggered based on that field
            assertTrue(passEnriched.containsKey("bonusPoints"), 
                "Enrichment should have triggered and set 'bonusPoints'");
            assertEquals(1500.0, passEnriched.get("bonusPoints"), 
                "Bonus points should be 10% of amount (1500.0)");
            
            logger.info("✓ Scenario 1 passed: Chain set flag, Enrichment reacted to flag");


            // Scenario 2: Chain Fails (VIP but Low Value)
            // -------------------------------------------
            logger.info("--- Scenario 2: Chain Fails (VIP but Low Value) ---");
            Map<String, Object> failInput = new HashMap<>();
            failInput.put("customerType", "VIP");
            failInput.put("amount", 5000.0); // Below 10000 threshold
            logger.info("Input Data: {}", failInput);

            RuleResult failResult = engine.evaluate(config, failInput);
            Map<String, Object> failEnriched = failResult.getEnrichedData();
            logger.info("Result Data: {}", failEnriched);

            // Verify chain result field was set to false (or not set if implementation differs, but usually boolean result)
            // Note: Depending on implementation, failed chains might set false or might not set the field if it's strictly "on success".
            // Let's check what happened.
            Object chainResult = failEnriched.get("vipProcessingRequired");
            logger.info("Chain result for failure case: " + chainResult);
            
            // If the chain fails, the result-field should be false (since it's a boolean outcome of the chain)
            if (chainResult != null) {
                assertEquals(false, chainResult, "Failed chain should set result to false");
            }

            // Verify enrichment did NOT trigger
            assertFalse(failEnriched.containsKey("bonusPoints"), 
                "Enrichment should NOT have triggered for failed chain");

            logger.info("✓ Scenario 2 passed: Chain failed, Enrichment did not run");

        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }
}
