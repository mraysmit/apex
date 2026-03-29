package dev.mars.apex.engine.execution;

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

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deterministic regression guard for the nested-input mutation bug in the
 * plain {@code engine.evaluate()} path via {@link SequentialProcessor}.
 *
 * <h2>Bug Summary</h2>
 * {@code SequentialProcessor.evaluateSequential()} used
 * {@code new HashMap<>(inputData)} — a shallow copy. Because nested Maps were
 * aliased, enrichment writes via {@code FieldAccessor.setFieldValue()} mutated
 * the caller's original nested structures. The same class of bug that was
 * previously fixed in {@code ScenarioEvaluationManager} and
 * {@code EnrichmentGroupExecutor}.
 *
 * <h2>YAML Fixture</h2>
 * Uses {@code EvaluateNestedMutationGuardTest.yaml} — a minimal config with
 * one field-enrichment that writes {@code sourceValue} into
 * {@code #settlement.details.amount} (a nested path requiring the caller to
 * pre-create the intermediate structure).
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
@DisplayName("evaluate() nested-input mutation guard")
class EvaluateNestedMutationGuardTest {

    private static final Logger logger = LoggerFactory.getLogger(EvaluateNestedMutationGuardTest.class);

    private static final String TEST_YAML =
            "src/test/resources/dev/mars/apex/engine/execution/EvaluateNestedMutationGuardTest.yaml";

    @Nested
    @DisplayName("engine.evaluate() must not mutate caller's nested Maps")
    class SingleCallNestedMutationTests {

        @Test
        @DisplayName("Shared nested Map must remain unchanged after single evaluate() call")
        void sharedNestedMapMustNotBeMutatedBySingleEvaluateCall() throws Exception {
            logger.info("=".repeat(80));
            logger.info("TEST: Single-call nested mutation guard for evaluate()");
            logger.info("=".repeat(80));

            RulesEngine engine = RulesEngine.fromFile(TEST_YAML);

            // Build nested structure — the inner 'details' Map is what we guard
            Map<String, Object> sharedDetails = new HashMap<>();   // <-- must stay empty
            Map<String, Object> sharedSettlement = new HashMap<>();
            sharedSettlement.put("details", sharedDetails);

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("sourceValue", 50000.0);
            inputData.put("settlement", sharedSettlement);

            try {
                RuleResult result = engine.evaluate(inputData);

                assertNotNull(result, "evaluate() must return a result");
                assertTrue(result.isSuccess(), "evaluate() should succeed");

                // The enriched output should contain the written value
                Map<String, Object> enriched = result.getEnrichedData();
                assertNotNull(enriched, "Enriched data must be present");

                @SuppressWarnings("unchecked")
                Map<String, Object> outSettlement = (Map<String, Object>) enriched.get("settlement");
                assertNotNull(outSettlement, "Enriched data should contain 'settlement'");

                @SuppressWarnings("unchecked")
                Map<String, Object> outDetails = (Map<String, Object>) outSettlement.get("details");
                assertNotNull(outDetails, "Enriched settlement should contain 'details'");
                assertEquals(50000.0, outDetails.get("amount"),
                        "Enriched output should have amount=50000.0");

                // ====================================================================
                // CRITICAL ASSERTION: caller's original nested Map must be untouched
                // ====================================================================
                assertTrue(sharedDetails.isEmpty(),
                        "MUTATION BUG: evaluate() wrote enrichment data into the caller's " +
                        "shared nested Map. sharedDetails should be empty but contains: " + sharedDetails);

                logger.info("PASSED: Caller's nested Map was NOT mutated by evaluate()");
            } finally {
                engine.shutdown();
            }
        }

        @Test
        @DisplayName("Two sequential evaluate() calls must not accumulate state in caller's nested Map")
        void twoSequentialCallsMustNotAccumulateStateInSharedNested() throws Exception {
            logger.info("=".repeat(80));
            logger.info("TEST: Sequential-call accumulation guard for evaluate()");
            logger.info("=".repeat(80));

            RulesEngine engine = RulesEngine.fromFile(TEST_YAML);

            // Shared nested structure reused across two calls
            Map<String, Object> sharedDetails = new HashMap<>();
            Map<String, Object> sharedSettlement = new HashMap<>();
            sharedSettlement.put("details", sharedDetails);

            try {
                // --- Call 1 ---
                Map<String, Object> input1 = new HashMap<>();
                input1.put("sourceValue", 100.0);
                input1.put("settlement", sharedSettlement);

                RuleResult result1 = engine.evaluate(input1);
                assertTrue(result1.isSuccess(), "First call should succeed");

                // Guard: shared nested must still be empty after call 1
                assertTrue(sharedDetails.isEmpty(),
                        "MUTATION BUG (call 1): sharedDetails polluted with: " + sharedDetails);

                // --- Call 2 ---
                Map<String, Object> input2 = new HashMap<>();
                input2.put("sourceValue", 200.0);
                input2.put("settlement", sharedSettlement);

                RuleResult result2 = engine.evaluate(input2);
                assertTrue(result2.isSuccess(), "Second call should succeed");

                // Guard: shared nested must still be empty after call 2
                assertTrue(sharedDetails.isEmpty(),
                        "MUTATION BUG (call 2): sharedDetails polluted with: " + sharedDetails);

                // Verify each result got its own isolated output
                @SuppressWarnings("unchecked")
                Map<String, Object> out1 = (Map<String, Object>)
                        ((Map<String, Object>) result1.getEnrichedData().get("settlement")).get("details");
                assertEquals(100.0, out1.get("amount"), "Call 1 output should have amount=100.0");

                @SuppressWarnings("unchecked")
                Map<String, Object> out2 = (Map<String, Object>)
                        ((Map<String, Object>) result2.getEnrichedData().get("settlement")).get("details");
                assertEquals(200.0, out2.get("amount"), "Call 2 output should have amount=200.0");

                logger.info("PASSED: Two sequential evaluate() calls did NOT accumulate state");
            } finally {
                engine.shutdown();
            }
        }
    }
}
