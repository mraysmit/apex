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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency tests for scenario execution via RulesEngine public interfaces.
 *
 * <p>Tests verify that a shared {@link RulesEngine} instance correctly isolates
 * concurrent scenario evaluations — both single-scenario stage-output isolation
 * and multi-scenario registry-based routing isolation.</p>
 *
 * <p>All tests operate on checked-in YAML fixtures and use only public
 * {@code RulesEngine} factory methods and evaluation APIs.</p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 * @see RulesEngine#fromFile(String)
 * @see RulesEngine#fromScenarioRegistry(String)
 * @see RulesEngine#evaluateScenario(Map)
 * @see RulesEngine#evaluateScenario(String, Map)
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
@DisplayName("Scenario concurrent access via RulesEngine public interfaces")
class ScenarioConcurrentAccessTest {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioConcurrentAccessTest.class);

    private static final String TEST_YAML_DIR = "src/test/java/dev/mars/apex/core/service/scenario/";
    private static final String SCENARIO_FILE = TEST_YAML_DIR + "ScenarioConcurrentAccessTest-scenario.yaml";
    private static final String REGISTRY_FILE = TEST_YAML_DIR + "ScenarioConcurrentAccessTest-registry.yaml";

    @Test
    @DisplayName("Shared RulesEngine evaluateScenario isolates concurrent stage outputs")
    void sharedEngineEvaluateScenarioIsolatesStageOutputs() throws Exception {
        RulesEngine engine = RulesEngine.fromFile(SCENARIO_FILE);

        Map<String, Object> sharedTrade = new HashMap<>();
        sharedTrade.put("status", "NEW");
        Map<String, Object> sharedAudit = new HashMap<>();
        sharedAudit.put("values", new HashMap<String, Object>());
        sharedTrade.put("audit", sharedAudit);

        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<ScenarioExecutionResult>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < threadCount; index++) {
                final int threadId = index;
                futures.add(executor.submit(() -> {
                    startLatch.await();

                    Map<String, Object> inputData = new HashMap<>();
                    inputData.put("threadId", threadId);
                    inputData.put("iteration", threadId * 10);
                    inputData.put("trade", sharedTrade);

                    return engine.evaluateScenario(inputData);
                }));
            }

            startLatch.countDown();

            for (int index = 0; index < threadCount; index++) {
                ScenarioExecutionResult result = futures.get(index).get(30, TimeUnit.SECONDS);
                assertTrue(result.isSuccessful(), "Concurrent scenario execution should succeed");

                StageExecutionResult stageResult = result.getStageResult("audit-stage");
                assertNotNull(stageResult, "Stage result should be present for each evaluation");

                @SuppressWarnings("unchecked")
                Map<String, Object> trade = (Map<String, Object>) stageResult.getStageOutputs().get("trade");
                @SuppressWarnings("unchecked")
                Map<String, Object> audit = (Map<String, Object>) trade.get("audit");
                @SuppressWarnings("unchecked")
                Map<String, Object> values = (Map<String, Object>) audit.get("values");

                assertEquals(index, values.get("threadId"), "Thread id should remain isolated per evaluation");
                assertEquals(index * 10, values.get("iteration"), "Iteration should remain isolated per evaluation");
            }

            assertEquals("NEW", sharedTrade.get("status"), "Shared nested input should retain original state");
            @SuppressWarnings("unchecked")
            Map<String, Object> sharedValues = (Map<String, Object>) ((Map<String, Object>) sharedTrade.get("audit")).get("values");
            assertTrue(sharedValues.isEmpty(), "Shared nested input must not be mutated by concurrent evaluation");
        } finally {
            executor.shutdownNow();
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("Shared RulesEngine evaluateScenario by id isolates concurrent scenario selection")
    void sharedEngineEvaluateScenarioByIdIsolatesConcurrentRequests() throws Exception {
        RulesEngine engine = RulesEngine.fromScenarioRegistry(REGISTRY_FILE);

        int threadCount = 10;
        int iterationsPerThread = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        try {
            for (int index = 0; index < threadCount; index++) {
                final boolean useAlpha = index % 2 == 0;
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int iteration = 0; iteration < iterationsPerThread; iteration++) {
                            String scenarioId = useAlpha ? "alpha-scenario" : "beta-scenario";
                            String expectedMarker = useAlpha ? "ALPHA" : "BETA";

                            Map<String, Object> inputData = new HashMap<>();
                            inputData.put("requestId", scenarioId + "-" + iteration);

                            ScenarioExecutionResult result = engine.evaluateScenario(scenarioId, inputData);
                            assertTrue(result.isSuccessful(), "Scenario execution should succeed for " + scenarioId);

                            StageExecutionResult stageResult = result.getStageResult(useAlpha ? "alpha-stage" : "beta-stage");
                            assertNotNull(stageResult, "Expected stage result should exist for " + scenarioId);
                            assertEquals(expectedMarker, stageResult.getStageOutputs().get("scenarioMarker"),
                                    "Concurrent scenario execution must not bleed outputs across scenario ids");
                        }
                    } catch (Throwable throwable) {
                        errors.add(throwable);
                    }
                }));
            }

            startLatch.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "Concurrent scenario-id tasks should finish");

            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }

            assertTrue(errors.isEmpty(), () -> "Concurrent scenario-id evaluation failed: " + errors);
        } finally {
            executor.shutdownNow();
            engine.shutdown();
        }
    }
}