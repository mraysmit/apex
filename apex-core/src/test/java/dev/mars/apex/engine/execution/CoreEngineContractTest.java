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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for core engine processing gaps.
 *
 * <p>Each nested class targets a specific contract that, if broken, should
 * cause an immediate test failure — preventing silent regressions like the
 * deep-copy-without-copy-back bug.</p>
 *
 * <h2>Contracts tested:</h2>
 * <ol>
 *   <li><b>Document ordering</b>: enrichments before rules when YAML declares that order</li>
 *   <li><b>#ruleResults context</b>: enrichment conditions can reference prior rule results</li>
 *   <li><b>inputData copy-back</b>: caller's map is updated with enrichment results after evaluate()</li>
 *   <li><b>RuleResult.getEnrichedData() never null</b>: always returns a map after successful evaluate()</li>
 * </ol>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
@DisplayName("Core engine contract tests")
class CoreEngineContractTest {

    private static final Logger logger = LoggerFactory.getLogger(CoreEngineContractTest.class);

    private static final String RESOURCE_BASE = "src/test/resources/dev/mars/apex/engine/execution/";

    // ========================================================================
    // 1. Document-order processing contract
    // ========================================================================

    @Nested
    @DisplayName("Document-order processing")
    class DocumentOrderTests {

        @Test
        @DisplayName("Enrichment before rule: rule sees enriched data when YAML declares enrichment first")
        void enrichmentBeforeRuleInDocumentOrder() throws Exception {
            logger.info("=== DOCUMENT ORDER CONTRACT: enrichment-first, rule-second ===");

            RulesEngine engine = RulesEngine.fromFile(RESOURCE_BASE + "DocumentOrderContractTest.yaml");

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("tradeId", "T001");

            try {
                RuleResult result = engine.evaluate(inputData);

                assertNotNull(result, "Result must not be null");
                assertTrue(result.isSuccess(), "Evaluation should succeed");

                // The rule checks #enrichedFlag == 'ENRICHED'. If document ordering
                // is broken (rule runs before enrichment), the rule sees null and
                // generates a NO_MATCH. We verify the rule actually matched.
                Map<String, Object> enriched = result.getEnrichedData();
                assertEquals("ENRICHED", enriched.get("enrichedFlag"),
                        "Enrichment must have executed before rule evaluation");

                // Verify the rule triggered (matched) — proves it saw the enriched field
                assertFalse(result.hasFailures(),
                        "Rule should have matched because enrichment ran first and set the flag");

                logger.info("PASSED: Document order preserved — enrichment ran before rule");
            } finally {
                engine.shutdown();
            }
        }
    }

    // ========================================================================
    // 2. #ruleResults context tracking contract
    // ========================================================================

    @Nested
    @DisplayName("#ruleResults context tracking")
    class RuleResultsContextTests {

        @Test
        @DisplayName("Enrichment condition referencing #ruleResults fires when rule passed")
        void enrichmentSeesPassedRuleResult() throws Exception {
            logger.info("=== #ruleResults CONTRACT: enrichment references prior rule result ===");

            RulesEngine engine = RulesEngine.fromFile(
                    RESOURCE_BASE + "RuleResultsContextContractTest.yaml");

            // amount > 1000 → rule passes → enrichment should fire
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("amount", 5000);

            try {
                RuleResult result = engine.evaluate(inputData);

                assertNotNull(result);
                assertTrue(result.isSuccess(), "Evaluation should succeed");

                Map<String, Object> enriched = result.getEnrichedData();
                assertEquals("HIGH_VALUE", enriched.get("classification"),
                        "Enrichment with #ruleResults condition should have fired " +
                        "because the 'amount-check' rule passed (amount=5000 > 1000). " +
                        "If classification is null, #ruleResults context was not populated.");

                logger.info("PASSED: #ruleResults context correctly populated — enrichment fired");
            } finally {
                engine.shutdown();
            }
        }

        @Test
        @DisplayName("Enrichment condition referencing #ruleResults does NOT fire when rule failed")
        void enrichmentDoesNotFireWhenRuleFailed() throws Exception {
            logger.info("=== #ruleResults CONTRACT: enrichment skipped when rule failed ===");

            RulesEngine engine = RulesEngine.fromFile(
                    RESOURCE_BASE + "RuleResultsContextContractTest.yaml");

            // amount <= 1000 → rule fails → enrichment condition is false
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("amount", 500);

            try {
                RuleResult result = engine.evaluate(inputData);

                assertNotNull(result);

                Map<String, Object> enriched = result.getEnrichedData();
                assertNull(enriched.get("classification"),
                        "Enrichment should NOT have fired because the 'amount-check' " +
                        "rule failed (amount=500 <= 1000). If classification is set, " +
                        "the #ruleResults condition was incorrectly evaluated.");

                logger.info("PASSED: Enrichment correctly skipped when #ruleResults showed failure");
            } finally {
                engine.shutdown();
            }
        }
    }

    // ========================================================================
    // 3. inputData copy-back contract
    // ========================================================================

    @Nested
    @DisplayName("inputData copy-back after evaluate()")
    class InputDataCopyBackTests {

        @Test
        @DisplayName("Caller's inputData map contains enriched fields after evaluate()")
        void inputDataUpdatedAfterEvaluate() throws Exception {
            logger.info("=== COPY-BACK CONTRACT: inputData updated with enrichment results ===");

            RulesEngine engine = RulesEngine.fromFile(
                    RESOURCE_BASE + "InputDataCopyBackContractTest.yaml");

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("currencyCode", "USD");

            try {
                RuleResult result = engine.evaluate(inputData);

                assertNotNull(result);
                assertTrue(result.isSuccess(), "Evaluation should succeed");

                // Primary API should always work
                Map<String, Object> enriched = result.getEnrichedData();
                assertEquals("SETTLEMENT_READY", enriched.get("settlementStatus"));
                assertEquals("US Dollar", enriched.get("currencyName"));

                // COPY-BACK CONTRACT: inputData should also have the enriched fields
                assertEquals("SETTLEMENT_READY", inputData.get("settlementStatus"),
                        "COPY-BACK BUG: inputData should contain 'settlementStatus' " +
                        "after evaluate() but it does not. The engine must copy enriched " +
                        "data back to the caller's map.");

                assertEquals("US Dollar", inputData.get("currencyName"),
                        "COPY-BACK BUG: inputData should contain 'currencyName' " +
                        "from lookup enrichment after evaluate().");

                logger.info("PASSED: inputData correctly updated with enrichment results");
            } finally {
                engine.shutdown();
            }
        }

        @Test
        @DisplayName("Copy-back produces independent values — not shared references")
        void copyBackValuesAreIndependent() throws Exception {
            logger.info("=== COPY-BACK ISOLATION: copied-back values are independent ===");

            RulesEngine engine = RulesEngine.fromFile(
                    RESOURCE_BASE + "InputDataCopyBackContractTest.yaml");

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("currencyCode", "USD");

            try {
                RuleResult result = engine.evaluate(inputData);

                // Mutate the value in inputData
                inputData.put("settlementStatus", "MODIFIED");

                // result.getEnrichedData() should still have the original value
                assertEquals("SETTLEMENT_READY", result.getEnrichedData().get("settlementStatus"),
                        "ISOLATION BUG: mutating inputData after evaluate() affected " +
                        "result.getEnrichedData(). Copy-back must use deep copies.");

                logger.info("PASSED: Copy-back values are independent from result.getEnrichedData()");
            } finally {
                engine.shutdown();
            }
        }
    }

    // ========================================================================
    // 4. RuleResult.getEnrichedData() never-null contract
    // ========================================================================

    @Nested
    @DisplayName("RuleResult.getEnrichedData() never-null contract")
    class EnrichedDataNeverNullTests {

        @Test
        @DisplayName("getEnrichedData() returns non-null map after successful evaluation")
        void enrichedDataNeverNullOnSuccess() throws Exception {
            logger.info("=== ENRICHED DATA CONTRACT: never null after success ===");

            RulesEngine engine = RulesEngine.fromFile(
                    RESOURCE_BASE + "DocumentOrderContractTest.yaml");

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("tradeId", "T001");

            try {
                RuleResult result = engine.evaluate(inputData);

                assertNotNull(result.getEnrichedData(),
                        "getEnrichedData() must never return null after evaluate()");

                logger.info("PASSED: getEnrichedData() returned non-null map");
            } finally {
                engine.shutdown();
            }
        }

        @Test
        @DisplayName("getEnrichedData() preserves original input fields")
        void enrichedDataPreservesInputFields() throws Exception {
            logger.info("=== ENRICHED DATA CONTRACT: original fields preserved ===");

            RulesEngine engine = RulesEngine.fromFile(
                    RESOURCE_BASE + "InputDataCopyBackContractTest.yaml");

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("currencyCode", "USD");
            inputData.put("originalField", "must-survive");

            try {
                RuleResult result = engine.evaluate(inputData);

                Map<String, Object> enriched = result.getEnrichedData();
                assertEquals("must-survive", enriched.get("originalField"),
                        "Original input fields must be present in enrichedData");
                assertEquals("USD", enriched.get("currencyCode"),
                        "Original currencyCode must be preserved");

                logger.info("PASSED: Original input fields preserved in enrichedData");
            } finally {
                engine.shutdown();
            }
        }
    }
}
