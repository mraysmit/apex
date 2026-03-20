package dev.mars.apex.core.service.scenario;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that scenario stage enrichments are correctly merged back into the
 * caller's input data map after execution, AND that concurrent callers
 * remain isolated from each other.
 *
 * <p>Covers all three evaluation methods on {@link RulesEngine}:</p>
 * <ul>
 *   <li>{@link RulesEngine#evaluateScenario(Map)} — YAML-based single scenario</li>
 *   <li>{@link RulesEngine#evaluateScenario(String, Map)} — registry-based by ID</li>
 *   <li>{@link RulesEngine#evaluateWithClassification(Map)} — classification-based routing</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
@DisplayName("Scenario enrichment merge-back into caller input data")
class ScenarioEnrichmentMergeTest {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioEnrichmentMergeTest.class);

    private static final String TEST_YAML_DIR = "src/test/java/dev/mars/apex/core/service/scenario/";
    private static final String SCENARIO_FILE = TEST_YAML_DIR + "ScenarioEnrichmentMergeTest-scenario.yaml";
    private static final String REGISTRY_FILE = TEST_YAML_DIR + "ScenarioEnrichmentMergeTest-registry.yaml";

    // ========================================================================
    // Positive tests: enrichments merge back into caller's inputData
    // ========================================================================

    @Test
    @DisplayName("evaluateScenario(Map) merges enriched fields back into caller input data")
    void evaluateScenarioMergesEnrichmentsBackIntoInputData() throws Exception {
        RulesEngine engine = RulesEngine.fromFile(SCENARIO_FILE);
        try {
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("tradeId", "TRD-001");
            inputData.put("currency", "USD");
            inputData.put("amount", 75000.00);

            ScenarioExecutionResult result = engine.evaluateScenario(inputData);

            assertTrue(result.isSuccessful(), "Scenario execution should succeed");

            // Enriched fields must be visible in the caller's original map
            assertEquals("HIGH_RISK", inputData.get("riskCategory"),
                    "Enriched riskCategory should be merged back into inputData");
            assertEquals("PENDING_REVIEW", inputData.get("settlementStatus"),
                    "Enriched settlementStatus should be merged back into inputData");

            // Original fields must be preserved
            assertEquals("TRD-001", inputData.get("tradeId"), "Original tradeId must be preserved");
            assertEquals("USD", inputData.get("currency"), "Original currency must be preserved");
            assertEquals(75000.00, inputData.get("amount"), "Original amount must be preserved");

            // No scenario metadata should leak into inputData
            assertFalse(inputData.containsKey("scenarioContext"), "scenarioContext must not leak into inputData");
            assertFalse(inputData.containsKey("scenarioId"), "scenarioId must not leak into inputData");
            assertFalse(inputData.containsKey("previousStageResults"), "previousStageResults must not leak into inputData");
            assertFalse(inputData.containsKey("executionStartTime"), "executionStartTime must not leak into inputData");

            logger.info("evaluateScenario(Map): inputData contains enriched fields: riskCategory={}, settlementStatus={}",
                    inputData.get("riskCategory"), inputData.get("settlementStatus"));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("evaluateScenario(String, Map) merges enriched fields back into caller input data")
    void evaluateScenarioByIdMergesEnrichmentsBackIntoInputData() throws Exception {
        RulesEngine engine = RulesEngine.fromScenarioRegistry(REGISTRY_FILE);
        try {
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("tradeId", "TRD-002");
            inputData.put("productType", "OPTION");
            inputData.put("amount", 120000.00);

            ScenarioExecutionResult result = engine.evaluateScenario("enrichment-merge-scenario", inputData);

            assertTrue(result.isSuccessful(), "Scenario execution should succeed");

            assertEquals("HIGH_RISK", inputData.get("riskCategory"),
                    "Enriched riskCategory should be merged back into inputData");
            assertEquals("PENDING_REVIEW", inputData.get("settlementStatus"),
                    "Enriched settlementStatus should be merged back into inputData");

            assertEquals("TRD-002", inputData.get("tradeId"), "Original tradeId must be preserved");

            logger.info("evaluateScenario(String, Map): inputData contains enriched fields: riskCategory={}, settlementStatus={}",
                    inputData.get("riskCategory"), inputData.get("settlementStatus"));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("evaluateWithClassification(Map) merges enriched fields back into caller input data")
    void evaluateWithClassificationMergesEnrichmentsBackIntoInputData() throws Exception {
        RulesEngine engine = RulesEngine.fromScenarioRegistry(REGISTRY_FILE);
        try {
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("tradeId", "TRD-003");
            inputData.put("productType", "OPTION");
            inputData.put("amount", 200000.00);

            ScenarioExecutionResult result = engine.evaluateWithClassification(inputData);

            assertTrue(result.isSuccessful(), "Classification-based scenario execution should succeed");

            assertEquals("HIGH_RISK", inputData.get("riskCategory"),
                    "Enriched riskCategory should be merged back into inputData");
            assertEquals("PENDING_REVIEW", inputData.get("settlementStatus"),
                    "Enriched settlementStatus should be merged back into inputData");

            assertEquals("TRD-003", inputData.get("tradeId"), "Original tradeId must be preserved");
            assertEquals("OPTION", inputData.get("productType"), "Original productType must be preserved");

            assertFalse(inputData.containsKey("scenarioContext"), "scenarioContext must not leak into inputData");
            assertFalse(inputData.containsKey("scenarioId"), "scenarioId must not leak into inputData");

            logger.info("evaluateWithClassification(Map): inputData contains enriched fields: riskCategory={}, settlementStatus={}",
                    inputData.get("riskCategory"), inputData.get("settlementStatus"));
        } finally {
            engine.shutdown();
        }
    }

    // ========================================================================
    // Negative tests: concurrent callers remain isolated
    // ========================================================================

    @Test
    @DisplayName("Concurrent evaluateScenario calls merge enrichments without cross-thread contamination")
    void concurrentEvaluateScenarioIsolatesEnrichmentMerge() throws Exception {
        RulesEngine engine = RulesEngine.fromFile(SCENARIO_FILE);
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Map<String, Object>>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                final String tradeId = "TRD-CONCURRENT-" + i;
                final double amount = 10000.0 * (i + 1);

                futures.add(executor.submit(() -> {
                    startLatch.await();

                    // Each thread creates its own inputData map
                    Map<String, Object> inputData = new HashMap<>();
                    inputData.put("tradeId", tradeId);
                    inputData.put("amount", amount);

                    ScenarioExecutionResult result = engine.evaluateScenario(inputData);
                    assertTrue(result.isSuccessful(), "Concurrent scenario should succeed for " + tradeId);

                    // Return the inputData map for assertion in the main thread
                    return inputData;
                }));
            }

            startLatch.countDown();

            for (int i = 0; i < threadCount; i++) {
                Map<String, Object> resultData = futures.get(i).get(30, TimeUnit.SECONDS);
                String expectedTradeId = "TRD-CONCURRENT-" + i;
                double expectedAmount = 10000.0 * (i + 1);

                // Each thread's inputData should have its own original values preserved
                assertEquals(expectedTradeId, resultData.get("tradeId"),
                        "Thread " + i + ": original tradeId must be preserved");
                assertEquals(expectedAmount, resultData.get("amount"),
                        "Thread " + i + ": original amount must be preserved");

                // Each thread's inputData should have enrichments merged back
                assertEquals("HIGH_RISK", resultData.get("riskCategory"),
                        "Thread " + i + ": enriched riskCategory should be merged back");
                assertEquals("PENDING_REVIEW", resultData.get("settlementStatus"),
                        "Thread " + i + ": enriched settlementStatus should be merged back");

                // No metadata leakage
                assertFalse(resultData.containsKey("scenarioContext"),
                        "Thread " + i + ": scenarioContext must not leak");
            }

            logger.info("Concurrent enrichment merge: all {} threads received correct enrichments with no cross-contamination", threadCount);
        } finally {
            executor.shutdownNow();
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("Concurrent evaluateWithClassification calls merge enrichments without cross-thread contamination")
    void concurrentEvaluateWithClassificationIsolatesEnrichmentMerge() throws Exception {
        RulesEngine engine = RulesEngine.fromScenarioRegistry(REGISTRY_FILE);
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Map<String, Object>>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                final String tradeId = "TRD-CLASSIFY-" + i;
                final double amount = 50000.0 * (i + 1);

                futures.add(executor.submit(() -> {
                    startLatch.await();

                    Map<String, Object> inputData = new HashMap<>();
                    inputData.put("tradeId", tradeId);
                    inputData.put("productType", "OPTION");
                    inputData.put("amount", amount);

                    ScenarioExecutionResult result = engine.evaluateWithClassification(inputData);
                    assertTrue(result.isSuccessful(), "Classification scenario should succeed for " + tradeId);

                    return inputData;
                }));
            }

            startLatch.countDown();

            for (int i = 0; i < threadCount; i++) {
                Map<String, Object> resultData = futures.get(i).get(30, TimeUnit.SECONDS);
                String expectedTradeId = "TRD-CLASSIFY-" + i;
                double expectedAmount = 50000.0 * (i + 1);

                assertEquals(expectedTradeId, resultData.get("tradeId"),
                        "Thread " + i + ": original tradeId must be preserved");
                assertEquals(expectedAmount, resultData.get("amount"),
                        "Thread " + i + ": original amount must be preserved");
                assertEquals("OPTION", resultData.get("productType"),
                        "Thread " + i + ": original productType must be preserved");

                assertEquals("HIGH_RISK", resultData.get("riskCategory"),
                        "Thread " + i + ": enriched riskCategory should be merged back");
                assertEquals("PENDING_REVIEW", resultData.get("settlementStatus"),
                        "Thread " + i + ": enriched settlementStatus should be merged back");

                assertFalse(resultData.containsKey("scenarioContext"),
                        "Thread " + i + ": scenarioContext must not leak");
            }

            logger.info("Concurrent classification merge: all {} threads received correct enrichments with no cross-contamination", threadCount);
        } finally {
            executor.shutdownNow();
            engine.shutdown();
        }
    }
}
