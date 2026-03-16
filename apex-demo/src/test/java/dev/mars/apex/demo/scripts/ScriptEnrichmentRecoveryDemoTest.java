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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates enrichment-level recovery and partial-failure behaviour
 * when Groovy scripts fail:
 * <ul>
 *   <li>Default-value recovery: script fails, {@code default-value} is applied</li>
 *   <li>Partial failure: multiple enrichments, one fails without default — the
 *       overall result is {@code enrichmentFailure} but successful enrichments
 *       still populate their fields</li>
 * </ul>
 */
@ExtendWith(ColoredTestOutputExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScriptEnrichmentRecoveryDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ScriptEnrichmentRecoveryDemoTest.class);

    // ---------------------------------------------------------------
    //  1. Default-value recovery
    // ---------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("RECOVERY: script fails but default-value provides fallback")
    void testDefaultValueRecovery() throws Exception {
        logger.info("=".repeat(80));
        logger.info("RECOVERY CASE: default-value fallback");
        logger.info("  Script 'ScriptEnrichmentRecoveryDemoTest-always-fails' throws RuntimeException");
        logger.info("  Enrichment has default-value: 'UNKNOWN'");
        logger.info("  Expected: enrichment succeeds with classification = 'UNKNOWN'");
        logger.info("=".repeat(80));

        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/scripts/ScriptEnrichmentRecoveryDemoTest-default-value.yaml");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("notional", 100000);

            RuleResult result = engine.evaluate(data);
            assertNotNull(result, "Result should not be null");

            logger.info("RuleResult type      : {}", result.getResultType());
            logger.info("Success              : {}", result.isSuccess());
            logger.info("classification       : {}", result.getEnrichedData().get("classification"));

            assertTrue(result.isSuccess(),
                    "Enrichment should succeed because default-value provides recovery");

            assertEquals("UNKNOWN", result.getEnrichedData().get("classification"),
                    "Default value 'UNKNOWN' should be used after script failure");

            logger.info("CONFIRMED: default-value recovery applied successfully");
        } finally {
            engine.shutdown();
        }
    }

    // ---------------------------------------------------------------
    //  2. Partial enrichment failure
    // ---------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("PARTIAL FAILURE: one of three enrichments fails, others succeed")
    void testPartialEnrichmentFailure() throws Exception {
        logger.info("=".repeat(80));
        logger.info("PARTIAL FAILURE CASE: multi-enrichment with one failure");
        logger.info("  Enrichment #1: valid classifier script   -> sizeClass");
        logger.info("  Enrichment #2: always-fails script       -> brokenField (NO default-value)");
        logger.info("  Enrichment #3: static SpEL expression    -> staticField");
        logger.info("  Expected: overall enrichment failure, but #1 and #3 populate their fields");
        logger.info("=".repeat(80));

        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/scripts/ScriptEnrichmentRecoveryDemoTest-partial-failure.yaml");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("notional", 1000000);

            RuleResult result = engine.evaluate(data);
            assertNotNull(result, "Result should not be null");

            Map<String, Object> enriched = result.getEnrichedData();
            logger.info("RuleResult type      : {}", result.getResultType());
            logger.info("Success              : {}", result.isSuccess());
            logger.info("Failure messages     : {}", result.getFailureMessages());
            logger.info("sizeClass            : {}", enriched.get("sizeClass"));
            logger.info("brokenField          : {}", enriched.get("brokenField"));
            logger.info("staticField          : {}", enriched.get("staticField"));

            assertFalse(result.isSuccess(),
                    "Overall enrichment should fail because enrichment #2 has no default-value");

            // Successful enrichments should still have populated their fields
            assertEquals("LARGE", enriched.get("sizeClass"),
                    "First enrichment (valid classifier) should have populated sizeClass");
            assertEquals("STATIC_OK", enriched.get("staticField"),
                    "Third enrichment (static expression) should have populated staticField");

            logger.info("CONFIRMED: partial failure — successful enrichments preserved, overall result is failure");
        } finally {
            engine.shutdown();
        }
    }
}
