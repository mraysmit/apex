package dev.mars.apex.engine.scenario;

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

import dev.mars.apex.core.service.scenario.ScenarioExecutionResult;
import dev.mars.apex.core.service.scenario.StageExecutionResult;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import dev.mars.apex.engine.core.RulesEngine;
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
 * Deterministic regression guard for the nested-input mutation bug in
 * {@link ScenarioEvaluationManager#evaluateScenario(Map)}.
 *
 * <h2>Bug Summary</h2>
 * After deep-copying the caller's input for isolated execution, the manager
 * calls {@code DataCopyUtility.deepMergeInto(inputData, safeInputData)} which
 * recursively descends into caller-owned nested Maps and mutates them in place.
 * Any nested Map shared across callers (e.g. a common reference-data object)
 * gets polluted with enrichment outputs.
 *
 * <h2>Why Single-Threaded Tests Suffice</h2>
 * The mutation happens on every call, not just under race conditions. A single
 * invocation is enough to observe it. Single-threaded tests are deterministic
 * — they cannot flake — and directly prove whether the contract
 * "callers' nested structures are never mutated" holds or is violated.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
@DisplayName("DeepMerge nested-input mutation guard")
class DeepMergeNestedMutationGuardTest {

    private static final Logger logger = LoggerFactory.getLogger(DeepMergeNestedMutationGuardTest.class);

    /**
     * Reuses the same YAML fixtures as {@code ScenarioConcurrentAccessTest}.
     * The scenario copies {@code threadId} and {@code iteration} into
     * {@code #trade.audit.values.*} via field-enrichment mappings.
     */
    private static final String TEST_YAML_DIR = "src/test/java/dev/mars/apex/core/service/scenario/";
    private static final String SCENARIO_FILE = TEST_YAML_DIR + "ScenarioConcurrentAccessTest-scenario.yaml";

    @Nested
    @DisplayName("evaluateScenario(Map) must not mutate caller's nested Maps")
    class SingleCallNestedMutationTests {

        @Test
        @DisplayName("Shared nested Map must remain empty after single evaluateScenario call")
        void sharedNestedMapMustNotBeMutatedBySingleCall() throws Exception {
            logger.info("=".repeat(80));
            logger.info("TEST: Single-call nested mutation guard");
            logger.info("=".repeat(80));

            RulesEngine engine = RulesEngine.fromFile(SCENARIO_FILE);

            // Build a nested structure where the inner Map is what we guard
            Map<String, Object> sharedValues = new HashMap<>();   // <-- must stay empty

            Map<String, Object> sharedAudit = new HashMap<>();
            sharedAudit.put("values", sharedValues);

            Map<String, Object> sharedTrade = new HashMap<>();
            sharedTrade.put("status", "NEW");
            sharedTrade.put("audit", sharedAudit);

            // Caller's top-level input (new per call, but nested refs are shared)
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("threadId", 42);
            inputData.put("iteration", 420);
            inputData.put("trade", sharedTrade);

            try {
                ScenarioExecutionResult result = engine.evaluateScenario(inputData);

                // Stage should succeed and produce enriched outputs
                assertTrue(result.isSuccessful(), "Scenario execution should succeed");
                StageExecutionResult stageResult = result.getStageResult("audit-stage");
                assertNotNull(stageResult, "audit-stage must be present");

                logger.info("Stage outputs: {}", stageResult.getStageOutputs());

                // The stage OUTPUTS should contain the enriched values
                @SuppressWarnings("unchecked")
                Map<String, Object> outputTrade = (Map<String, Object>) stageResult.getStageOutputs().get("trade");
                assertNotNull(outputTrade, "Stage outputs should contain 'trade'");

                @SuppressWarnings("unchecked")
                Map<String, Object> outputAudit = (Map<String, Object>) outputTrade.get("audit");
                assertNotNull(outputAudit, "Stage output trade should contain 'audit'");

                @SuppressWarnings("unchecked")
                Map<String, Object> outputValues = (Map<String, Object>) outputAudit.get("values");
                assertNotNull(outputValues, "Stage output audit should contain 'values'");

                assertEquals(42, outputValues.get("threadId"),
                        "Stage output should contain enriched threadId");
                assertEquals(420, outputValues.get("iteration"),
                        "Stage output should contain enriched iteration");

                // ====================================================================
                // CRITICAL ASSERTION: caller's original nested Map must be untouched
                // ====================================================================
                assertTrue(sharedValues.isEmpty(),
                        "MUTATION BUG: deepMergeInto wrote enrichment data into the caller's " +
                        "shared nested Map. sharedValues should be empty but contains: " + sharedValues);

                assertEquals("NEW", sharedTrade.get("status"),
                        "Top-level field in shared nested Map must not be altered");

                logger.info("PASSED: Caller's nested Map was NOT mutated by evaluateScenario");
            } finally {
                engine.shutdown();
            }
        }

        @Test
        @DisplayName("Two sequential calls with same shared nested Map must not accumulate state")
        void twoSequentialCallsMustNotAccumulateStateInSharedNested() throws Exception {
            logger.info("=".repeat(80));
            logger.info("TEST: Sequential-call accumulation guard");
            logger.info("=".repeat(80));

            RulesEngine engine = RulesEngine.fromFile(SCENARIO_FILE);

            // Shared nested object reused across two calls
            Map<String, Object> sharedValues = new HashMap<>();
            Map<String, Object> sharedAudit = new HashMap<>();
            sharedAudit.put("values", sharedValues);
            Map<String, Object> sharedTrade = new HashMap<>();
            sharedTrade.put("status", "NEW");
            sharedTrade.put("audit", sharedAudit);

            try {
                // --- Call 1 ---
                Map<String, Object> input1 = new HashMap<>();
                input1.put("threadId", 1);
                input1.put("iteration", 10);
                input1.put("trade", sharedTrade);

                ScenarioExecutionResult result1 = engine.evaluateScenario(input1);
                assertTrue(result1.isSuccessful(), "First call should succeed");

                // Guard: shared nested must still be empty after call 1
                assertTrue(sharedValues.isEmpty(),
                        "MUTATION BUG (call 1): sharedValues polluted with: " + sharedValues);

                // --- Call 2 ---
                Map<String, Object> input2 = new HashMap<>();
                input2.put("threadId", 2);
                input2.put("iteration", 20);
                input2.put("trade", sharedTrade);

                ScenarioExecutionResult result2 = engine.evaluateScenario(input2);
                assertTrue(result2.isSuccessful(), "Second call should succeed");

                // Guard: shared nested must still be empty after call 2
                assertTrue(sharedValues.isEmpty(),
                        "MUTATION BUG (call 2): sharedValues polluted with: " + sharedValues);

                // Verify each result got its own isolated outputs
                @SuppressWarnings("unchecked")
                Map<String, Object> out1 = (Map<String, Object>)
                        ((Map<String, Object>) ((Map<String, Object>)
                                result1.getStageResult("audit-stage").getStageOutputs().get("trade"))
                                .get("audit")).get("values");
                assertEquals(1, out1.get("threadId"), "Call 1 output should have threadId=1");
                assertEquals(10, out1.get("iteration"), "Call 1 output should have iteration=10");

                @SuppressWarnings("unchecked")
                Map<String, Object> out2 = (Map<String, Object>)
                        ((Map<String, Object>) ((Map<String, Object>)
                                result2.getStageResult("audit-stage").getStageOutputs().get("trade"))
                                .get("audit")).get("values");
                assertEquals(2, out2.get("threadId"), "Call 2 output should have threadId=2");
                assertEquals(20, out2.get("iteration"), "Call 2 output should have iteration=20");

                logger.info("PASSED: Two sequential calls did NOT accumulate state in shared nested Map");
            } finally {
                engine.shutdown();
            }
        }
    }
}
