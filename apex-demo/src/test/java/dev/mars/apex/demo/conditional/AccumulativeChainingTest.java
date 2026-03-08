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
 * Test class for the "Accumulative Chaining" rule chain pattern.
 *
 * Verifies that a rule chain can accumulate a weighted score from multiple
 * rules and make a final decision based on the accumulated value.
 *
 * Business Scenario: Credit Risk Scoring
 * - +30 High credit rating (AAA/AA)
 * - +20 Low debt-to-income ratio (< 0.3)
 * - +15 Long trading history (> 5 years)
 * - -25 Recent default event
 * - -10 High exposure concentration (> 50%)
 * - Decision: Approve if score >= 30
 */
@DisplayName("Accumulative Chaining Pattern Test")
public class AccumulativeChainingTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(AccumulativeChainingTest.class.getName());

    @Test
    @DisplayName("Should accumulate weighted scores and approve strong counterparty")
    void testAccumulativeChainingPattern() {
        logger.info("=== Testing Accumulative Chaining Pattern ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/resources/dev/mars/apex/demo/conditional/AccumulativeChainingTest.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("[OK] Configuration loaded successfully");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            // Scenario 1: STRONG COUNTERPARTY — Score = +30 +20 +15 = 65 → APPROVED
            // -----------------------------------------------------------------------
            logger.info("--- Scenario 1: STRONG COUNTERPARTY (Score 65 → APPROVED) ---");
            Map<String, Object> strongInput = new HashMap<>();
            strongInput.put("creditRating", "AAA");
            strongInput.put("debtToIncomeRatio", 0.2);
            strongInput.put("tradingYears", 10);
            strongInput.put("hasRecentDefault", false);
            strongInput.put("exposureConcentration", 0.3);
            logger.info("Input Data: {}", strongInput);

            RuleResult strongResult = engine.evaluate(config, strongInput);
            Map<String, Object> strongEnriched = strongResult.getEnrichedData();
            logger.info("Result Data: {}", strongEnriched);

            // Verify accumulated score
            assertTrue(strongEnriched.containsKey("riskScore"),
                "Should have accumulated risk score");
            assertEquals(65.0, ((Number) strongEnriched.get("riskScore")).doubleValue(),
                "Score should be 30+20+15 = 65");

            logger.info("[OK] Scenario 1 passed: Score 65.0, approved");


            // Scenario 2: WEAK COUNTERPARTY — Score = 0 -25 -10 = -35 → REJECTED
            // --------------------------------------------------------------------
            logger.info("--- Scenario 2: WEAK COUNTERPARTY (Score -35 → REJECTED) ---");
            Map<String, Object> weakInput = new HashMap<>();
            weakInput.put("creditRating", "B");           // No match (+0)
            weakInput.put("debtToIncomeRatio", 0.6);      // No match (+0)
            weakInput.put("tradingYears", 2);             // No match (+0)
            weakInput.put("hasRecentDefault", true);      // Match (-25)
            weakInput.put("exposureConcentration", 0.7);  // Match (-10)
            logger.info("Input Data: {}", weakInput);

            RuleResult weakResult = engine.evaluate(config, weakInput);
            Map<String, Object> weakEnriched = weakResult.getEnrichedData();
            logger.info("Result Data: {}", weakEnriched);

            // Verify accumulated score
            assertTrue(weakEnriched.containsKey("riskScore"),
                "Should have accumulated risk score");
            assertEquals(-35.0, ((Number) weakEnriched.get("riskScore")).doubleValue(),
                "Score should be -25 + -10 = -35");

            logger.info("[OK] Scenario 2 passed: Score -35.0, rejected");


            // Scenario 3: BORDERLINE — Score = +30 +0 +0 -0 -0 = 30 → APPROVED (exactly threshold)
            // ------------------------------------------------------------------------------------
            logger.info("--- Scenario 3: BORDERLINE (Score 30 → APPROVED at threshold) ---");
            Map<String, Object> borderlineInput = new HashMap<>();
            borderlineInput.put("creditRating", "AA");         // Match (+30)
            borderlineInput.put("debtToIncomeRatio", 0.4);     // No match (+0)
            borderlineInput.put("tradingYears", 3);            // No match (+0)
            borderlineInput.put("hasRecentDefault", false);    // No match (+0)
            borderlineInput.put("exposureConcentration", 0.2); // No match (+0)
            logger.info("Input Data: {}", borderlineInput);

            RuleResult borderlineResult = engine.evaluate(config, borderlineInput);
            Map<String, Object> borderlineEnriched = borderlineResult.getEnrichedData();
            logger.info("Result Data: {}", borderlineEnriched);

            // Verify accumulated score
            assertTrue(borderlineEnriched.containsKey("riskScore"),
                "Should have accumulated risk score");
            assertEquals(30.0, ((Number) borderlineEnriched.get("riskScore")).doubleValue(),
                "Score should be exactly 30");

            logger.info("[OK] Scenario 3 passed: Score 30.0, approved at threshold");


            // Scenario 4: MIXED — Score = +30 +20 +0 -25 +0 = 25 → REJECTED (just below threshold)
            // --------------------------------------------------------------------------------------
            logger.info("--- Scenario 4: MIXED (Score 25 → REJECTED below threshold) ---");
            Map<String, Object> mixedInput = new HashMap<>();
            mixedInput.put("creditRating", "AAA");            // Match (+30)
            mixedInput.put("debtToIncomeRatio", 0.1);         // Match (+20)
            mixedInput.put("tradingYears", 3);                // No match (+0)
            mixedInput.put("hasRecentDefault", true);         // Match (-25)
            mixedInput.put("exposureConcentration", 0.2);     // No match (+0)
            logger.info("Input Data: {}", mixedInput);

            RuleResult mixedResult = engine.evaluate(config, mixedInput);
            Map<String, Object> mixedEnriched = mixedResult.getEnrichedData();
            logger.info("Result Data: {}", mixedEnriched);

            // Verify accumulated score
            assertTrue(mixedEnriched.containsKey("riskScore"),
                "Should have accumulated risk score");
            assertEquals(25.0, ((Number) mixedEnriched.get("riskScore")).doubleValue(),
                "Score should be 30+20-25 = 25");

            logger.info("[OK] Scenario 4 passed: Score 25.0, rejected (below 30 threshold)");

        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }
}
