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
 * Demonstrates runtime execution errors from Groovy scripts:
 * <ul>
 *   <li>Script throws an exception during execution</li>
 *   <li>Script exceeds the configured execution timeout</li>
 *   <li>Script returns {@code null} (edge case, not necessarily an error)</li>
 * </ul>
 *
 * <p>Each error case has its own YAML and Groovy script so the logs
 * clearly show which scenario is being exercised.</p>
 */
@ExtendWith(ColoredTestOutputExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScriptRuntimeExecutionErrorDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ScriptRuntimeExecutionErrorDemoTest.class);

    // ---------------------------------------------------------------
    //  1. Script throws RuntimeException
    // ---------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("ERROR CASE: script throws RuntimeException during execution")
    void testScriptThrowsExceptionProducesFailure() throws Exception {
        logger.info("=".repeat(80));
        logger.info("ERROR CASE: Script throws RuntimeException");
        logger.info("  Script body: throw new RuntimeException('Intentional business error: invalid trade state')");
        logger.info("  Expected: enrichment failure with thrown message in chain");
        logger.info("=".repeat(80));

        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/scripts/ScriptRuntimeExecutionErrorDemoTest-throws.yaml");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("notional", 100000);

            RuleResult result = engine.evaluate(data);
            assertNotNull(result, "Result should not be null even when script throws");

            logger.info("RuleResult type      : {}", result.getResultType());
            logger.info("Success              : {}", result.isSuccess());
            logger.info("Failure messages     : {}", result.getFailureMessages());

            assertFalse(result.isSuccess(),
                    "Enrichment should fail when script throws an exception");

            boolean mentionsError = result.getFailureMessages().stream()
                    .anyMatch(msg -> msg.contains("business error")
                            || msg.contains("invalid trade state")
                            || msg.toLowerCase().contains("script"));
            assertTrue(mentionsError,
                    "Failure message should contain the thrown exception text");

            logger.info("CONFIRMED: RuntimeException from script propagated as enrichment failure");
        } finally {
            engine.shutdown();
        }
    }

    // ---------------------------------------------------------------
    //  2. Script exceeds timeout
    // ---------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("ERROR CASE: script exceeds execution timeout")
    void testScriptTimeoutProducesFailure() throws Exception {
        logger.info("=".repeat(80));
        logger.info("ERROR CASE: Script timeout");
        logger.info("  Script sleeps for 10 seconds; execution-timeout-ms = 500");
        logger.info("  Expected: enrichment failure with timeout message");
        logger.info("=".repeat(80));

        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/scripts/ScriptRuntimeExecutionErrorDemoTest-timeout.yaml");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("notional", 100000);

            RuleResult result = engine.evaluate(data);
            assertNotNull(result, "Result should not be null even on timeout");

            logger.info("RuleResult type      : {}", result.getResultType());
            logger.info("Success              : {}", result.isSuccess());
            logger.info("Failure messages     : {}", result.getFailureMessages());

            assertFalse(result.isSuccess(),
                    "Enrichment should fail when script exceeds timeout");

            boolean mentionsTimeout = result.getFailureMessages().stream()
                    .anyMatch(msg -> msg.contains("call-slow-script")
                            || msg.toLowerCase().contains("timeout")
                            || msg.toLowerCase().contains("failed"));
            assertTrue(mentionsTimeout,
                    "Failure message should reference the failed enrichment");

            logger.info("CONFIRMED: ScriptExecutionTimeoutException propagated as enrichment failure");
        } finally {
            engine.shutdown();
        }
    }

    // ---------------------------------------------------------------
    //  3. Script returns null
    // ---------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("EDGE CASE: script returns null — enrichment succeeds but field is null")
    void testScriptReturnsNullSetsFieldToNull() throws Exception {
        logger.info("=".repeat(80));
        logger.info("EDGE CASE: Script returns null");
        logger.info("  Script body: return null");
        logger.info("  Expected: enrichment succeeds, 'result' field is null");
        logger.info("=".repeat(80));

        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/scripts/ScriptRuntimeExecutionErrorDemoTest-returns-null.yaml");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("notional", 100000);

            RuleResult result = engine.evaluate(data);
            assertNotNull(result, "Result should not be null");

            logger.info("RuleResult type      : {}", result.getResultType());
            logger.info("Success              : {}", result.isSuccess());
            logger.info("Enriched 'result'    : {}", result.getEnrichedData().get("result"));

            // The enrichment itself should succeed — null is a valid script return
            assertTrue(result.isSuccess(),
                    "Enrichment should succeed (null is a valid return value)");

            assertNull(result.getEnrichedData().get("result"),
                    "Target field should be null when script returns null");

            logger.info("CONFIRMED: null return from script preserved as null field value");
        } finally {
            engine.shutdown();
        }
    }
}
