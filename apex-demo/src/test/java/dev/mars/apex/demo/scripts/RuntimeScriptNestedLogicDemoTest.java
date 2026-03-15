package dev.mars.apex.demo.scripts;

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

import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Demonstrates runtime Groovy invocation with nested business logic (no switch/case).
 *
 * <p>CRITICAL VALIDATION CHECKLIST APPLIED:</p>
 * <ol>
 *   <li>Count enrichments in YAML - 1 enrichment expected</li>
 *   <li>Verify nested Groovy branches are exercised by test data</li>
 *   <li>Validate routeDecision enrichment output for each branch</li>
 *   <li>Assert rule outcomes based on routeDecision</li>
 * </ol>
 */
@ExtendWith(ColoredTestOutputExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RuntimeScriptNestedLogicDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeScriptNestedLogicDemoTest.class);

    @Test
    @Order(1)
    @DisplayName("Derivative + high notional + weak tier routes to MANUAL_REVIEW")
    void testManualReviewForHighRiskDerivative() throws Exception {
        logger.info("=".repeat(80));
        logger.info("TEST: Nested logic branch - high risk derivative -> MANUAL_REVIEW");
        logger.info("=".repeat(80));

        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/scripts/RuntimeScriptNestedLogicDemoTest.yaml");
        try {
            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("productType", "DERIVATIVE");
            tradeData.put("notional", 3000000);
            tradeData.put("counterpartyTier", "TIER3");
            tradeData.put("region", "EMEA");
            tradeData.put("currency", "USD");
            tradeData.put("marginPosted", true);

            RuleResult result = engine.evaluate(tradeData);
            assertNotNull(result);
            assertEquals("MANUAL_REVIEW", result.getEnrichedData().get("routeDecision"));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @Order(2)
    @DisplayName("Derivative + medium notional + margin posted routes to STP_DERIV")
    void testStpDerivativeBranch() throws Exception {
        logger.info("=".repeat(80));
        logger.info("TEST: Nested logic branch - eligible derivative -> STP_DERIV");
        logger.info("=".repeat(80));

        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/scripts/RuntimeScriptNestedLogicDemoTest.yaml");
        try {
            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("productType", "DERIVATIVE");
            tradeData.put("notional", 250000);
            tradeData.put("counterpartyTier", "TIER1");
            tradeData.put("region", "APAC");
            tradeData.put("currency", "EUR");
            tradeData.put("marginPosted", true);

            RuleResult result = engine.evaluate(tradeData);
            assertNotNull(result);
            assertEquals("STP_DERIV", result.getEnrichedData().get("routeDecision"));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @Order(3)
    @DisplayName("Payment + SWIFT + high amount non-priority routes to COMPLIANCE_REVIEW")
    void testComplianceReviewForPaymentBranch() throws Exception {
        logger.info("=".repeat(80));
        logger.info("TEST: Nested logic branch - payment compliance review");
        logger.info("=".repeat(80));

        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/scripts/RuntimeScriptNestedLogicDemoTest.yaml");
        try {
            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("productType", "PAYMENT");
            tradeData.put("amount", 500000);
            tradeData.put("paymentMethod", "SWIFT");
            tradeData.put("isPriorityClient", false);

            RuleResult result = engine.evaluate(tradeData);
            assertNotNull(result);
            assertEquals("COMPLIANCE_REVIEW", result.getEnrichedData().get("routeDecision"));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @Order(4)
    @DisplayName("Payment + SWIFT + priority client routes to STP_PAYMENTS")
    void testStpPaymentsBranch() throws Exception {
        logger.info("=".repeat(80));
        logger.info("TEST: Nested logic branch - payment straight-through processing");
        logger.info("=".repeat(80));

        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/scripts/RuntimeScriptNestedLogicDemoTest.yaml");
        try {
            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("productType", "PAYMENT");
            tradeData.put("amount", 750000);
            tradeData.put("paymentMethod", "SWIFT");
            tradeData.put("isPriorityClient", true);

            RuleResult result = engine.evaluate(tradeData);
            assertNotNull(result);
            assertEquals("STP_PAYMENTS", result.getEnrichedData().get("routeDecision"));
        } finally {
            engine.shutdown();
        }
    }
}
