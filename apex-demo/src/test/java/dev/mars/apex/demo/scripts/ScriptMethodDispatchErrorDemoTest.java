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
 * Demonstrates method dispatch errors from the {@code ScriptExecutor}:
 * <ul>
 *   <li>Calling a function name that does not exist in the script</li>
 *   <li>Calling a function with the wrong number of arguments (arity mismatch)</li>
 * </ul>
 *
 * <p>Each error case uses a separate YAML configuration so the intent
 * is unambiguous in the logs.</p>
 */
@ExtendWith(ColoredTestOutputExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScriptMethodDispatchErrorDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ScriptMethodDispatchErrorDemoTest.class);

    @Test
    @Order(1)
    @DisplayName("ERROR CASE: calling a function name that does not exist in the script")
    void testMethodNotFoundProducesFailure() throws Exception {
        logger.info("=".repeat(80));
        logger.info("ERROR CASE: Method not found");
        logger.info("  Expression calls 'noSuchFunction' which is not defined in the Groovy script");
        logger.info("  Expected: enrichment failure with 'No method' message");
        logger.info("=".repeat(80));

        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/scripts/ScriptMethodDispatchErrorDemoTest-method-not-found.yaml");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("notional", 100000);

            RuleResult result = engine.evaluate(data);
            assertNotNull(result, "Result should not be null even on dispatch failure");

            logger.info("RuleResult type      : {}", result.getResultType());
            logger.info("Success              : {}", result.isSuccess());
            logger.info("Failure messages     : {}", result.getFailureMessages());

            assertFalse(result.isSuccess(),
                    "Enrichment should fail when function name does not exist");

            boolean mentionsMethod = result.getFailureMessages().stream()
                    .anyMatch(msg -> msg.toLowerCase().contains("method")
                            || msg.toLowerCase().contains("not found")
                            || msg.toLowerCase().contains("nosuchfunction"));
            assertTrue(mentionsMethod,
                    "Failure message should mention missing method or function name");

            logger.info("CONFIRMED: method-not-found error propagated as enrichment failure");
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @Order(2)
    @DisplayName("ERROR CASE: calling a function with wrong number of arguments")
    void testArityMismatchProducesFailure() throws Exception {
        logger.info("=".repeat(80));
        logger.info("ERROR CASE: Arity mismatch");
        logger.info("  Expression calls 'twoArgHelper' with 1 arg, but it requires 2");
        logger.info("  Expected: enrichment failure with 'does not accept' message");
        logger.info("=".repeat(80));

        RulesEngine engine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/scripts/ScriptMethodDispatchErrorDemoTest-arity-mismatch.yaml");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("notional", 100000);

            RuleResult result = engine.evaluate(data);
            assertNotNull(result, "Result should not be null even on arity mismatch");

            logger.info("RuleResult type      : {}", result.getResultType());
            logger.info("Success              : {}", result.isSuccess());
            logger.info("Failure messages     : {}", result.getFailureMessages());

            assertFalse(result.isSuccess(),
                    "Enrichment should fail when argument count is wrong");

            boolean mentionsArity = result.getFailureMessages().stream()
                    .anyMatch(msg -> msg.toLowerCase().contains("argument")
                            || msg.toLowerCase().contains("does not accept")
                            || msg.toLowerCase().contains("arity"));
            assertTrue(mentionsArity,
                    "Failure message should mention argument count mismatch");

            logger.info("CONFIRMED: arity-mismatch error propagated as enrichment failure");
        } finally {
            engine.shutdown();
        }
    }
}
