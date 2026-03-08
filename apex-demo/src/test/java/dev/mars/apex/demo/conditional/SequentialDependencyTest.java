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

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the "Sequential Dependency" rule chain pattern.
 *
 * Verifies that a rule chain can process stages in strict order where each
 * stage evaluates a rule and stores the result in an output-variable.
 * Subsequent stages reference output variables from earlier stages.
 *
 * Business Scenario: Trade Settlement Eligibility Pipeline
 * - Stage 1: Validate trade data (tradeId, notional, currency)
 * - Stage 2: Check counterparty status (depends on Stage 1 passing)
 * - Stage 3: Check settlement readiness (depends on Stage 2 passing)
 */
@DisplayName("Sequential Dependency Pattern Test")
public class SequentialDependencyTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SequentialDependencyTest.class.getName());

    @Test
    @DisplayName("Should process stages sequentially with dependency on prior results")
    void testSequentialDependencyPattern() {
        logger.info("=== Testing Sequential Dependency Pattern ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/resources/dev/mars/apex/demo/conditional/SequentialDependencyTest.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("[OK] Configuration loaded successfully");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            // Scenario 1: ALL STAGES PASS (Valid trade, active counterparty, fully funded)
            // ---------------------------------------------------------------------------
            logger.info("--- Scenario 1: ALL STAGES PASS ---");
            Map<String, Object> passInput = new HashMap<>();
            passInput.put("tradeId", "TRD-001");
            passInput.put("notional", 1000000.0);
            passInput.put("currency", "USD");
            passInput.put("counterpartyStatus", "ACTIVE");
            passInput.put("marginPosted", true);
            passInput.put("fundingConfirmed", true);
            logger.info("Input Data: {}", passInput);

            RuleResult passResult = engine.evaluate(config, passInput);
            Map<String, Object> passEnriched = passResult.getEnrichedData();
            logger.info("Result Data: {}", passEnriched);

            // Stage 1 should pass and set tradeValidated = true
            assertTrue(passEnriched.containsKey("tradeValidated"),
                "Stage 1 should have set 'tradeValidated'");
            assertEquals(true, passEnriched.get("tradeValidated"),
                "tradeValidated should be true");

            // Stage 2 should pass and set counterpartyApproved = true
            assertTrue(passEnriched.containsKey("counterpartyApproved"),
                "Stage 2 should have set 'counterpartyApproved'");
            assertEquals(true, passEnriched.get("counterpartyApproved"),
                "counterpartyApproved should be true");

            // Stage 3 should pass and set settlementReady = true
            assertTrue(passEnriched.containsKey("settlementReady"),
                "Stage 3 should have set 'settlementReady'");
            assertEquals(true, passEnriched.get("settlementReady"),
                "settlementReady should be true");

            logger.info("[OK] Scenario 1 passed: All 3 stages completed successfully");


            // Scenario 2: FAIL AT STAGE 1 (Missing trade data)
            // -------------------------------------------------
            logger.info("--- Scenario 2: FAIL AT STAGE 1 (Missing trade data) ---");
            Map<String, Object> failStage1Input = new HashMap<>();
            failStage1Input.put("tradeId", null);       // Invalid: null tradeId
            failStage1Input.put("notional", 1000000.0);
            failStage1Input.put("currency", "USD");
            failStage1Input.put("counterpartyStatus", "ACTIVE");
            failStage1Input.put("marginPosted", true);
            failStage1Input.put("fundingConfirmed", true);
            logger.info("Input Data: {}", failStage1Input);

            RuleResult failStage1Result = engine.evaluate(config, failStage1Input);
            Map<String, Object> failStage1Enriched = failStage1Result.getEnrichedData();
            logger.info("Result Data: {}", failStage1Enriched);

            // Stage 1 should fail (tradeId is null)
            assertEquals(false, failStage1Enriched.getOrDefault("tradeValidated", false),
                "tradeValidated should be false when tradeId is null");

            // Stage 2 should NOT execute (depends on Stage 1)
            assertFalse(failStage1Enriched.containsKey("counterpartyApproved"),
                "Stage 2 should not have executed");

            // Stage 3 should NOT execute (depends on Stage 2)
            assertFalse(failStage1Enriched.containsKey("settlementReady"),
                "Stage 3 should not have executed");

            logger.info("[OK] Scenario 2 passed: Chain stopped at Stage 1");


            // Scenario 3: FAIL AT STAGE 2 (Inactive counterparty)
            // ----------------------------------------------------
            logger.info("--- Scenario 3: FAIL AT STAGE 2 (Inactive counterparty) ---");
            Map<String, Object> failStage2Input = new HashMap<>();
            failStage2Input.put("tradeId", "TRD-002");
            failStage2Input.put("notional", 500000.0);
            failStage2Input.put("currency", "EUR");
            failStage2Input.put("counterpartyStatus", "SUSPENDED");  // Fails Stage 2
            failStage2Input.put("marginPosted", true);
            failStage2Input.put("fundingConfirmed", true);
            logger.info("Input Data: {}", failStage2Input);

            RuleResult failStage2Result = engine.evaluate(config, failStage2Input);
            Map<String, Object> failStage2Enriched = failStage2Result.getEnrichedData();
            logger.info("Result Data: {}", failStage2Enriched);

            // Stage 1 should pass
            assertTrue(failStage2Enriched.containsKey("tradeValidated"),
                "Stage 1 should have set 'tradeValidated'");
            assertEquals(true, failStage2Enriched.get("tradeValidated"),
                "tradeValidated should be true (valid trade data)");

            // Stage 2 should fail (counterparty is SUSPENDED)
            assertEquals(false, failStage2Enriched.getOrDefault("counterpartyApproved", false),
                "counterpartyApproved should be false for SUSPENDED counterparty");

            // Stage 3 should NOT execute (depends on Stage 2)
            assertFalse(failStage2Enriched.containsKey("settlementReady"),
                "Stage 3 should not have executed");

            logger.info("[OK] Scenario 3 passed: Passed Stage 1, stopped at Stage 2");


            // Scenario 4: FAIL AT STAGE 3 (Margin not posted)
            // ------------------------------------------------
            logger.info("--- Scenario 4: FAIL AT STAGE 3 (Margin not posted) ---");
            Map<String, Object> failStage3Input = new HashMap<>();
            failStage3Input.put("tradeId", "TRD-003");
            failStage3Input.put("notional", 750000.0);
            failStage3Input.put("currency", "GBP");
            failStage3Input.put("counterpartyStatus", "ACTIVE");
            failStage3Input.put("marginPosted", false);       // Fails Stage 3
            failStage3Input.put("fundingConfirmed", true);
            logger.info("Input Data: {}", failStage3Input);

            RuleResult failStage3Result = engine.evaluate(config, failStage3Input);
            Map<String, Object> failStage3Enriched = failStage3Result.getEnrichedData();
            logger.info("Result Data: {}", failStage3Enriched);

            // Stage 1 should pass
            assertTrue(failStage3Enriched.containsKey("tradeValidated"),
                "Stage 1 should have set 'tradeValidated'");
            assertEquals(true, failStage3Enriched.get("tradeValidated"),
                "tradeValidated should be true");

            // Stage 2 should pass
            assertTrue(failStage3Enriched.containsKey("counterpartyApproved"),
                "Stage 2 should have set 'counterpartyApproved'");
            assertEquals(true, failStage3Enriched.get("counterpartyApproved"),
                "counterpartyApproved should be true");

            // Stage 3 should fail (margin not posted)
            assertEquals(false, failStage3Enriched.getOrDefault("settlementReady", false),
                "settlementReady should be false when margin is not posted");

            logger.info("[OK] Scenario 4 passed: Passed Stages 1 & 2, stopped at Stage 3");

        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }
}
