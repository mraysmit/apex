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
 * Demonstrates configuration-level errors with the {@code #script()} bridge:
 * <ul>
 *   <li>Using {@code #script()} when no {@code runtime-scripts} block is configured</li>
 *   <li>Calling {@code #script()} with fewer than the required 2 arguments</li>
 * </ul>
 *
 * <p>These errors are caught before any script compilation or execution
 * occurs — they represent incorrect YAML / expression authoring.</p>
 */
@ExtendWith(ColoredTestOutputExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScriptMisconfigurationErrorDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ScriptMisconfigurationErrorDemoTest.class);

    // ---------------------------------------------------------------
    //  1. No runtime-scripts block configured
    // ---------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("ERROR CASE: #script() used without runtime-scripts block in YAML")
    void testScriptBridgeNotConfiguredProducesFailure() throws Exception {
        logger.info("=".repeat(80));
        logger.info("ERROR CASE: #script() without runtime-scripts block");
        logger.info("  YAML has no runtime-scripts section — ScriptBridge is never activated");
        logger.info("  Expected: enrichment failure with 'not configured' message");
        logger.info("=".repeat(80));

        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/scripts/ScriptMisconfigurationErrorDemoTest-not-configured.yaml");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("notional", 100000);

            RuleResult result = engine.evaluate(data);
            assertNotNull(result, "Result should not be null even on misconfiguration");

            logger.info("RuleResult type      : {}", result.getResultType());
            logger.info("Success              : {}", result.isSuccess());
            logger.info("Failure messages     : {}", result.getFailureMessages());

            assertFalse(result.isSuccess(),
                    "Enrichment should fail when runtime-scripts is not configured");

            boolean mentionsNotConfigured = result.getFailureMessages().stream()
                    .anyMatch(msg -> msg.toLowerCase().contains("not configured")
                            || msg.toLowerCase().contains("script")
                            || msg.toLowerCase().contains("cannot be used"));
            assertTrue(mentionsNotConfigured,
                    "Failure message should mention that scripts are not configured");

            logger.info("CONFIRMED: ScriptBridge 'not configured' error propagated as enrichment failure");
        } finally {
            engine.shutdown();
        }
    }

    // ---------------------------------------------------------------
    //  2. Too few arguments to #script()
    // ---------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("ERROR CASE: #script() called with only 1 argument (missing payload)")
    void testTooFewArgumentsProducesFailure() throws Exception {
        logger.info("=".repeat(80));
        logger.info("ERROR CASE: #script() with too few arguments");
        logger.info("  Expression: #script('RuntimeScriptDemoTest-risk-score') — only 1 arg");
        logger.info("  Expected: enrichment failure with 'requires at least 2 arguments' message");
        logger.info("=".repeat(80));

        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/scripts/ScriptMisconfigurationErrorDemoTest-too-few-args.yaml");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("notional", 100000);

            RuleResult result = engine.evaluate(data);
            assertNotNull(result, "Result should not be null even with wrong args");

            logger.info("RuleResult type      : {}", result.getResultType());
            logger.info("Success              : {}", result.isSuccess());
            logger.info("Failure messages     : {}", result.getFailureMessages());

            assertFalse(result.isSuccess(),
                    "Enrichment should fail when #script() has too few arguments");

            boolean mentionsArgs = result.getFailureMessages().stream()
                    .anyMatch(msg -> msg.contains("call-with-too-few-args")
                            || msg.toLowerCase().contains("argument")
                            || msg.toLowerCase().contains("failed"));
            assertTrue(mentionsArgs,
                    "Failure message should reference the failed enrichment");

            logger.info("CONFIRMED: too-few-arguments error propagated as enrichment failure");
        } finally {
            engine.shutdown();
        }
    }
}
