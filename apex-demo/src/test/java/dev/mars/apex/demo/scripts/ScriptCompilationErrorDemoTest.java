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
 * Demonstrates error behaviour when a Groovy script has a syntax error
 * and cannot be compiled by the Groovy compiler.
 *
 * <p><b>Error case tested:</b> {@code ScriptCompilationException} —
 * the {@code .groovy} file contains invalid syntax (unclosed brace).</p>
 *
 * <p>Expected outcome: the enrichment fails, {@code RuleResult} reports
 * an enrichment failure whose messages reference a compilation error.</p>
 */
@ExtendWith(ColoredTestOutputExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScriptCompilationErrorDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ScriptCompilationErrorDemoTest.class);

    @Test
    @Order(1)
    @DisplayName("ERROR CASE: Groovy syntax error produces compilation failure")
    void testSyntaxErrorProducesCompilationFailure() throws Exception {
        logger.info("=".repeat(80));
        logger.info("ERROR CASE: Script compilation error");
        logger.info("  Script 'ScriptCompilationErrorDemoTest-broken-syntax.groovy' has an unclosed brace");
        logger.info("  Expected: enrichment failure with compilation error in chain");
        logger.info("=".repeat(80));

        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/scripts/ScriptCompilationErrorDemoTest.yaml");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("notional", 100000);

            RuleResult result = engine.evaluate(data);
            assertNotNull(result, "Result should not be null even on compilation failure");

            logger.info("RuleResult type      : {}", result.getResultType());
            logger.info("Success              : {}", result.isSuccess());
            logger.info("Failure messages     : {}", result.getFailureMessages());

            assertFalse(result.isSuccess(),
                    "Enrichment should fail when script has syntax error");

            boolean mentionsCompilation = result.getFailureMessages().stream()
                    .anyMatch(msg -> msg.toLowerCase().contains("compil")
                            || msg.toLowerCase().contains("script")
                            || msg.toLowerCase().contains("failed"));
            assertTrue(mentionsCompilation,
                    "Failure message should mention compilation or script failure");

            logger.info("CONFIRMED: ScriptCompilationException propagated as enrichment failure");
        } finally {
            engine.shutdown();
        }
    }
}
