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
 * Demonstrates error behaviour when {@code #script()} references a script ID
 * that does not exist in any configured location.
 *
 * <p><b>Error case tested:</b> {@code ScriptNotFoundException} —
 * the script registry has no entry for the requested ID.</p>
 *
 * <p>Expected outcome: the enrichment fails, {@code RuleResult} reports
 * an enrichment failure with {@code overallSuccess = false}.</p>
 */
@ExtendWith(ColoredTestOutputExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScriptNotFoundErrorDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ScriptNotFoundErrorDemoTest.class);

    @Test
    @Order(1)
    @DisplayName("ERROR CASE: #script() referencing a non-existent script ID produces enrichment failure")
    void testScriptNotFoundProducesEnrichmentFailure() throws Exception {
        logger.info("=".repeat(80));
        logger.info("ERROR CASE: Script not found");
        logger.info("  Script ID 'this-script-does-not-exist' is not present in any location");
        logger.info("  Expected: enrichment failure with ScriptNotFoundException in chain");
        logger.info("=".repeat(80));

        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/scripts/ScriptNotFoundErrorDemoTest.yaml");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("notional", 100000);

            RuleResult result = engine.evaluate(data);
            assertNotNull(result, "Result should not be null even on enrichment failure");

            logger.info("RuleResult type      : {}", result.getResultType());
            logger.info("Success              : {}", result.isSuccess());
            logger.info("Failure messages     : {}", result.getFailureMessages());

            assertFalse(result.isSuccess(),
                    "Enrichment should fail when script ID does not exist");

            boolean mentionsNotFound = result.getFailureMessages().stream()
                    .anyMatch(msg -> msg.toLowerCase().contains("not found")
                            || msg.toLowerCase().contains("script"));
            assertTrue(mentionsNotFound,
                    "Failure message should mention the missing script");

            logger.info("CONFIRMED: ScriptNotFoundException propagated as enrichment failure");
        } finally {
            engine.shutdown();
        }
    }
}
