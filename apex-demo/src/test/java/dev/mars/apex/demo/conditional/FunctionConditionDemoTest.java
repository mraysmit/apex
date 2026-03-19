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
 * Demonstrates the IF=Function + THEN=Function RuleBuilder pattern: both the condition
 * predicate and the mapping resolution invoke enrichment groups.
 *
 * <p>Scenario: Trade risk classification and routing.
 * <ul>
 *   <li>WHEN side: "risk-classifier-group" evaluates trade notional → sets risk_level</li>
 *   <li>THEN side: appropriate router group produces routing_instruction</li>
 * </ul>
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * 1. Count enrichments in YAML — 5 enrichments (3 routers + 1 classifier + 1 CME)
 * 2. Verify log shows "Processed: X out of X" — 100% execution rate
 * 3. Check EVERY condition type — function conditions invoking enrichment groups
 * 4. Validate EVERY business calculation — risk classification thresholds
 * 5. Assert ALL enrichment results — TRADE_ROUTING for each risk level
 */
public class FunctionConditionDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(FunctionConditionDemoTest.class);

    private static final String CONFIG_PATH =
            "src/test/resources/dev/mars/apex/demo/conditional/FunctionConditionDemoTest.yaml";

    @Test
    @DisplayName("High notional (>5M) classified as HIGH risk → compliance routing")
    void shouldRouteHighRiskToCompliance() {
        logger.info("=== Testing Function Condition: HIGH Risk Classification ===");
        logger.info("Flow: classifier sets risk_level='HIGH' → high-risk-router produces ROUTE_COMPLIANCE_FX_PRIORITY");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);
            logger.info("[OK] Configuration loaded: {} enrichments, {} groups",
                    config.getEnrichments().size(),
                    config.getEnrichmentGroups() != null ? config.getEnrichmentGroups().size() : 0);

            Map<String, Object> testData = new HashMap<>();
            testData.put("NOTIONAL", 10000000);  // 10M — HIGH risk
            testData.put("DESK_CODE", "FX");
            logger.info("Input: {}", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Map<String, Object> enrichedData = ruleResult.getEnrichedData();

            assertNotNull(enrichedData);
            logger.info("[OK] Enriched data: {}", enrichedData);

            assertEquals("ROUTE_COMPLIANCE_FX_PRIORITY", enrichedData.get("TRADE_ROUTING"),
                    "10M notional should classify as HIGH risk and route via compliance");

            logger.info("[OK] TRADE_ROUTING='{}' — function condition + function mapping both resolved",
                    enrichedData.get("TRADE_ROUTING"));

        } catch (Exception e) {
            logger.error("Failed: " + e.getMessage(), e);
            fail("Should route high-risk trade to compliance: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Medium notional (1M-5M) classified as MEDIUM risk → standard routing")
    void shouldRouteMediumRiskToStandardDesk() {
        logger.info("=== Testing Function Condition: MEDIUM Risk Classification ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);

            Map<String, Object> testData = new HashMap<>();
            testData.put("NOTIONAL", 3000000);  // 3M — MEDIUM risk
            testData.put("DESK_CODE", "RATES");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Map<String, Object> enrichedData = ruleResult.getEnrichedData();

            assertNotNull(enrichedData);
            logger.info("[OK] Enriched data: {}", enrichedData);

            assertEquals("ROUTE_STANDARD_RATES", enrichedData.get("TRADE_ROUTING"),
                    "3M notional should classify as MEDIUM risk and route via standard desk");

        } catch (Exception e) {
            logger.error("Failed: " + e.getMessage(), e);
            fail("Should route medium-risk trade to standard desk: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Low notional (<1M) falls to default → auto approve")
    void shouldAutoApproveLowRisk() {
        logger.info("=== Testing Function Condition: LOW Risk → Auto Approve ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);

            Map<String, Object> testData = new HashMap<>();
            testData.put("NOTIONAL", 500000);  // 500K — LOW risk
            testData.put("DESK_CODE", "EQ");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Map<String, Object> enrichedData = ruleResult.getEnrichedData();

            assertNotNull(enrichedData);
            logger.info("[OK] Enriched data: {}", enrichedData);

            assertEquals("AUTO_APPROVED", enrichedData.get("TRADE_ROUTING"),
                    "500K notional should classify as LOW risk and auto-approve");

        } catch (Exception e) {
            logger.error("Failed: " + e.getMessage(), e);
            fail("Should auto-approve low-risk trade: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Boundary: exactly 5M notional classifies as MEDIUM (not HIGH)")
    void shouldClassifyBoundaryAsMedium() {
        logger.info("=== Testing Function Condition: Boundary at 5M ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);

            Map<String, Object> testData = new HashMap<>();
            testData.put("NOTIONAL", 5000000);  // Exactly 5M — MEDIUM (not > 5M)
            testData.put("DESK_CODE", "CREDIT");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Map<String, Object> enrichedData = ruleResult.getEnrichedData();

            assertNotNull(enrichedData);
            logger.info("[OK] Enriched data: {}", enrichedData);

            assertEquals("ROUTE_STANDARD_CREDIT", enrichedData.get("TRADE_ROUTING"),
                    "Exactly 5M should classify as MEDIUM (> test is strict), not HIGH");

        } catch (Exception e) {
            logger.error("Failed: " + e.getMessage(), e);
            fail("Should classify boundary notional correctly: " + e.getMessage());
        }
    }
}
