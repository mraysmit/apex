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
import dev.mars.apex.core.engine.core.RulesEngine;
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
 * Test class for the "Decoupled Decision & Routing" pattern.
 *
 * Verifies that complex logic can be handled by separating the decision phase
 * (using conditional-mapping-enrichment) from the execution phase
 * (using result-based-routing).
 */
@DisplayName("Decoupled Routing Pattern Test")
public class DecoupledRoutingTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(DecoupledRoutingTest.class.getName());

    @Test
    @DisplayName("Should route correctly using decoupled decision logic")
    void testDecoupledRouting() {
        logger.info("=== Testing Decoupled Routing Pattern ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/DecoupledRoutingTest.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("[OK] Configuration loaded successfully");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            // Scenario 1: High Risk -> MANUAL_REVIEW
            // Amount > 1M, Region HighRisk, Not VIP
            logger.info("--- Scenario 1: High Risk (Manual Review) ---");
            Map<String, Object> highRiskInput = new HashMap<>();
            highRiskInput.put("amount", 1500000.0);
            highRiskInput.put("region", "HighRisk");
            highRiskInput.put("customerType", "Standard");
            logger.info("Input Data: {}", highRiskInput);

            RuleResult highRiskResult = engine.evaluate(config, highRiskInput);
            Map<String, Object> highRiskEnriched = highRiskResult.getEnrichedData();
            logger.info("Result Data: {}", highRiskEnriched);

            assertEquals("MANUAL_REVIEW", highRiskEnriched.get("processingPath"), "Should be routed to MANUAL_REVIEW");
            assertTrue(highRiskEnriched.containsKey("reviewRequired"), "Should have reviewRequired flag");
            assertTrue(highRiskEnriched.containsKey("supervisorNotified"), "Should have supervisorNotified flag");
            logger.info("[OK] Scenario 1 passed");


            // Scenario 2: Auto Approve (Condition A) -> AUTO_APPROVE
            // Score > 800, DTI < 0.3
            logger.info("--- Scenario 2: Auto Approve (High Score) ---");
            Map<String, Object> autoApproveInput = new HashMap<>();
            autoApproveInput.put("amount", 200000.0);
            autoApproveInput.put("region", "US");
            autoApproveInput.put("customerType", "Standard");
            autoApproveInput.put("score", 850);
            autoApproveInput.put("dti", 0.25);
            logger.info("Input Data: {}", autoApproveInput);

            RuleResult autoApproveResult = engine.evaluate(config, autoApproveInput);
            Map<String, Object> autoApproveEnriched = autoApproveResult.getEnrichedData();
            logger.info("Result Data: {}", autoApproveEnriched);

            assertEquals("AUTO_APPROVE", autoApproveEnriched.get("processingPath"), "Should be routed to AUTO_APPROVE");
            assertTrue(autoApproveEnriched.containsKey("loanApproved"), "Should have loanApproved flag");
            logger.info("[OK] Scenario 2 passed");


            // Scenario 3: Auto Approve (Condition B) -> AUTO_APPROVE
            // VIP, Amount < 50k
            logger.info("--- Scenario 3: Auto Approve (VIP Small Loan) ---");
            Map<String, Object> vipInput = new HashMap<>();
            vipInput.put("amount", 40000.0);
            vipInput.put("region", "HighRisk"); // Region shouldn't matter for VIP small loan
            vipInput.put("customerType", "VIP");
            vipInput.put("score", 700); // Score shouldn't matter
            vipInput.put("dti", 0.40);  // DTI shouldn't matter
            logger.info("Input Data: {}", vipInput);

            RuleResult vipResult = engine.evaluate(config, vipInput);
            Map<String, Object> vipEnriched = vipResult.getEnrichedData();
            logger.info("Result Data: {}", vipEnriched);

            assertEquals("AUTO_APPROVE", vipEnriched.get("processingPath"), "Should be routed to AUTO_APPROVE");
            assertTrue(vipEnriched.containsKey("loanApproved"), "Should have loanApproved flag");
            logger.info("[OK] Scenario 3 passed");


            // Scenario 4: Standard Process -> STANDARD_PROCESS
            // Fallback
            logger.info("--- Scenario 4: Standard Process ---");
            Map<String, Object> standardInput = new HashMap<>();
            standardInput.put("amount", 200000.0);
            standardInput.put("region", "US");
            standardInput.put("customerType", "Standard");
            standardInput.put("score", 700);
            standardInput.put("dti", 0.35);
            logger.info("Input Data: {}", standardInput);

            RuleResult standardResult = engine.evaluate(config, standardInput);
            Map<String, Object> standardEnriched = standardResult.getEnrichedData();
            logger.info("Result Data: {}", standardEnriched);

            assertEquals("STANDARD_PROCESS", standardEnriched.get("processingPath"), "Should be routed to STANDARD_PROCESS");
            assertTrue(standardEnriched.containsKey("standardValidationComplete"), "Should have standardValidationComplete flag");
            logger.info("[OK] Scenario 4 passed");

        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }
}
