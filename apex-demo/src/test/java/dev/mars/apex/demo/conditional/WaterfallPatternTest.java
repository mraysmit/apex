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
 * Test class for the "Waterfall" pattern (Option 2).
 *
 * Verifies that multiple Rule Chains can be chained sequentially using
 * result-fields (flags) to create a multi-stage processing workflow.
 */
@DisplayName("Waterfall Pattern Test")
public class WaterfallPatternTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(WaterfallPatternTest.class.getName());

    @Test
    @DisplayName("Should process sequentially through 3 levels of logic")
    void testWaterfallPattern() {
        logger.info("=== Testing Waterfall Pattern (3-Level Workflow) ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/WaterfallPatternTest.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("[OK] Configuration loaded successfully");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            // Scenario 1: REJECT (Level 1 Failure: Credit Score <= 700)
            // ---------------------------------------------------------
            logger.info("--- Scenario 1: REJECT (Credit Score <= 700) ---");
            Map<String, Object> rejectInput = new HashMap<>();
            rejectInput.put("creditScore", 650);
            rejectInput.put("income", 100000.0);
            rejectInput.put("dti", 0.20);
            logger.info("Input Data: {}", rejectInput);

            RuleResult rejectResult = engine.evaluate(config, rejectInput);
            Map<String, Object> rejectEnriched = rejectResult.getEnrichedData();
            logger.info("Result Data: {}", rejectEnriched);

            // Should fail Level 1
            assertEquals(false, rejectEnriched.getOrDefault("level1Passed", false), "Should NOT have passed Level 1");
            assertTrue(rejectEnriched.containsKey("rejectionReason"), "Should have rejectionReason");
            assertEquals(true, rejectEnriched.get("rejectionReason"), "Rejection reason flag should be true");
            
            // Should NOT have proceeded to Level 2 or 3 flags
            assertEquals(false, rejectEnriched.getOrDefault("level2Passed", false), "Should NOT have passed Level 2");
            assertEquals(false, rejectEnriched.getOrDefault("isApproved", false), "Should NOT be approved");
            
            logger.info("[OK] Scenario 1 passed: Stopped at Level 1");


            // Scenario 2: REFER_INCOME (Level 2 Failure: Income <= 50000)
            // -----------------------------------------------------------
            logger.info("--- Scenario 2: REFER_INCOME (Income <= 50000) ---");
            Map<String, Object> referIncomeInput = new HashMap<>();
            referIncomeInput.put("creditScore", 750); // Pass Level 1
            referIncomeInput.put("income", 40000.0);  // Fail Level 2
            referIncomeInput.put("dti", 0.20);
            logger.info("Input Data: {}", referIncomeInput);

            RuleResult referIncomeResult = engine.evaluate(config, referIncomeInput);
            Map<String, Object> referIncomeEnriched = referIncomeResult.getEnrichedData();
            logger.info("Result Data: {}", referIncomeEnriched);

            // Should pass Level 1
            assertTrue(referIncomeEnriched.containsKey("level1Passed"), "Should have level1Passed flag");
            assertEquals(true, referIncomeEnriched.get("level1Passed"));

            // Should fail Level 2
            assertEquals(false, referIncomeEnriched.getOrDefault("level2Passed", false), "Should NOT have passed Level 2");
            assertTrue(referIncomeEnriched.containsKey("referralReason"), "Should have referralReason");
            assertEquals(true, referIncomeEnriched.get("referralReason"), "Referral reason flag should be true");

            logger.info("[OK] Scenario 2 passed: Passed Level 1, Stopped at Level 2");


            // Scenario 3: REFER_DEBT (Level 3 Failure: DTI >= 0.40)
            // -----------------------------------------------------
            logger.info("--- Scenario 3: REFER_DEBT (DTI >= 0.40) ---");
            Map<String, Object> referDebtInput = new HashMap<>();
            referDebtInput.put("creditScore", 750); // Pass Level 1
            referDebtInput.put("income", 80000.0);  // Pass Level 2
            referDebtInput.put("dti", 0.45);        // Fail Level 3
            logger.info("Input Data: {}", referDebtInput);

            RuleResult referDebtResult = engine.evaluate(config, referDebtInput);
            Map<String, Object> referDebtEnriched = referDebtResult.getEnrichedData();
            logger.info("Result Data: {}", referDebtEnriched);

            // Should pass Level 1 & 2
            assertTrue(referDebtEnriched.containsKey("level1Passed"), "Should have level1Passed flag");
            assertTrue(referDebtEnriched.containsKey("level2Passed"), "Should have level2Passed flag");

            // Should fail Level 3
            assertEquals(false, referDebtEnriched.getOrDefault("isApproved", false), "Should NOT be approved");
            assertTrue(referDebtEnriched.containsKey("referralReason"), "Should have referralReason");
            assertEquals(true, referDebtEnriched.get("referralReason"), "Referral reason flag should be true");

            logger.info("[OK] Scenario 3 passed: Passed Level 1 & 2, Stopped at Level 3");


            // Scenario 4: APPROVE (All Levels Pass)
            // -------------------------------------
            logger.info("--- Scenario 4: APPROVE (All Levels Pass) ---");
            Map<String, Object> approveInput = new HashMap<>();
            approveInput.put("creditScore", 750); // Pass Level 1
            approveInput.put("income", 80000.0);  // Pass Level 2
            approveInput.put("dti", 0.30);        // Pass Level 3
            logger.info("Input Data: {}", approveInput);

            RuleResult approveResult = engine.evaluate(config, approveInput);
            Map<String, Object> approveEnriched = approveResult.getEnrichedData();
            logger.info("Result Data: {}", approveEnriched);

            // Should pass all levels
            assertTrue(approveEnriched.containsKey("level1Passed"), "Should have level1Passed flag");
            assertTrue(approveEnriched.containsKey("level2Passed"), "Should have level2Passed flag");
            assertTrue(approveEnriched.containsKey("isApproved"), "Should have isApproved flag");
            
            assertTrue(approveEnriched.containsKey("loanStatus"), "Should have loanStatus");
            assertEquals(true, approveEnriched.get("loanStatus"), "Loan status flag should be true");

            logger.info("[OK] Scenario 4 passed: Passed all levels");

        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }
}
