package dev.mars.apex.engine.core;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.engine.model.Rule;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.engine.model.metadata.RuleMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("RulesEngine lifecycle coordination")
class RulesEngineLifecycleCoordinationTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Shutdown waits for in-flight evaluation and rejects new work")
    void shutdownWaitsForActiveEvaluationAndRejectsNewOperations() throws Exception {
        RulesEngine engine = new RulesEngine(new RulesEngineConfiguration());
        BlockingProbe blocker = new BlockingProbe();
        Rule blockingRule = new Rule(
                "blocking-rule",
                Set.of(),
                "Blocking Rule",
                "#blocker.awaitAndReturnTrue()",
                "blocked",
                "Blocks until released",
                100,
                null,
                RuleMetadata.builder().createdByUser("test").build(),
                null,
                null,
                null,
                null,
                null,
                null,
                true);
        Rule immediateRule = new Rule(
                "immediate-rule",
                Set.of(),
                "Immediate Rule",
                "true",
                "immediate",
                "Immediate rule",
                100,
                null,
                RuleMetadata.builder().createdByUser("test").build(),
                null,
                null,
                null,
                null,
                null,
                null,
                true);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<RuleResult> evaluationFuture = executor.submit(() -> {
                Map<String, Object> facts = new HashMap<>();
                facts.put("blocker", blocker);
                return engine.executeRule(blockingRule, facts);
            });

            assertTrue(blocker.awaitEntered(5, TimeUnit.SECONDS), "Evaluation should enter the blocking rule");

            Future<?> shutdownFuture = executor.submit(engine::shutdown);

            Thread.sleep(200);
            assertFalse(shutdownFuture.isDone(), "Shutdown should wait while an evaluation is still active");

            IllegalStateException duringShutdown = assertThrows(IllegalStateException.class,
                    () -> engine.executeRule(immediateRule, Map.of()),
                    "New work must be rejected once shutdown begins");
            assertTrue(duringShutdown.getMessage().contains("shutting down"));

            blocker.release();

            RuleResult result = evaluationFuture.get(5, TimeUnit.SECONDS);
            assertNotNull(result);
            assertEquals(RuleResult.ResultType.MATCH, result.getResultType());

            shutdownFuture.get(5, TimeUnit.SECONDS);

            IllegalStateException afterShutdown = assertThrows(IllegalStateException.class,
                    () -> engine.executeRule(immediateRule, Map.of()),
                    "New work must be rejected after shutdown completes");
            assertTrue(afterShutdown.getMessage().contains("shut down"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("evaluate(Map) is rejected after shutdown")
    void evaluateMapRejectedAfterShutdown() throws Exception {
        Path yamlFile = tempDir.resolve("lifecycle-evaluate.yaml");
        Files.writeString(yamlFile, """
                metadata:
                  name: \"Lifecycle Evaluate Test\"
                  version: \"1.0\"

                rules:
                  - id: \"always-match\"
                    name: \"Always Match\"
                    condition: \"true\"
                    message: \"ok\"
                    priority: 100
                """);

        RulesEngine engine = RulesEngine.fromFile(yamlFile.toString());
        engine.shutdown();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> engine.evaluate(Map.of()),
                "evaluate(Map) should be rejected after shutdown");
        assertTrue(exception.getMessage().contains("shutting down") || exception.getMessage().contains("shut down"));
    }

    @RepeatedTest(5)
    @DisplayName("Queued starters are rejected once shutdown begins")
    void queuedStartersAreRejectedOnceShutdownBegins() throws Exception {
        RulesEngine engine = new RulesEngine(new RulesEngineConfiguration());
        BlockingProbe blocker = new BlockingProbe();
        Rule blockingRule = new Rule(
                "blocking-rule",
                Set.of(),
                "Blocking Rule",
                "#blocker.awaitAndReturnTrue()",
                "blocked",
                "Blocks until released",
                100,
                null,
                RuleMetadata.builder().createdByUser("test").build(),
                null,
                null,
                null,
                null,
                null,
                null,
                true);
        Rule immediateRule = new Rule(
                "immediate-rule",
                Set.of(),
                "Immediate Rule",
                "true",
                "immediate",
                "Immediate rule",
                100,
                null,
                RuleMetadata.builder().createdByUser("test").build(),
                null,
                null,
                null,
                null,
                null,
                null,
                true);

        int starterCount = 24;
        ExecutorService executor = Executors.newFixedThreadPool(starterCount + 2);
        try {
            Future<RuleResult> blockingEvaluation = executor.submit(() -> {
                Map<String, Object> facts = new HashMap<>();
                facts.put("blocker", blocker);
                return engine.executeRule(blockingRule, facts);
            });

            assertTrue(blocker.awaitEntered(5, TimeUnit.SECONDS), "Blocking evaluation should be active before shutdown");

            CountDownLatch startersReady = new CountDownLatch(starterCount);
            CountDownLatch starterRelease = new CountDownLatch(1);
            List<Future<String>> starterFutures = new ArrayList<>();
            List<Throwable> starterFailures = new CopyOnWriteArrayList<>();

            for (int i = 0; i < starterCount; i++) {
                starterFutures.add(executor.submit(() -> {
                    startersReady.countDown();
                    starterRelease.await();
                    try {
                        RuleResult result = engine.executeRule(immediateRule, Map.of());
                        return result.getResultType().name();
                    } catch (IllegalStateException exception) {
                        return "REJECTED:" + exception.getMessage();
                    } catch (Throwable throwable) {
                        starterFailures.add(throwable);
                        return "FAILED:" + throwable.getClass().getName();
                    }
                }));
            }

            assertTrue(startersReady.await(5, TimeUnit.SECONDS), "Queued starters should be ready before shutdown begins");

            Future<?> shutdownFuture = executor.submit(engine::shutdown);

            IllegalStateException rejection = awaitShutdownRejection(engine, immediateRule);
            assertTrue(rejection.getMessage().contains("shutting down") || rejection.getMessage().contains("shut down"));

            starterRelease.countDown();

            int rejected = 0;
            int matched = 0;
            for (Future<String> starterFuture : starterFutures) {
                String outcome = starterFuture.get(10, TimeUnit.SECONDS);
                if (outcome.startsWith("REJECTED:")) {
                    rejected++;
                } else if (RuleResult.ResultType.MATCH.name().equals(outcome)) {
                    matched++;
                } else {
                    fail("Unexpected starter outcome: " + outcome);
                }
            }

            blocker.release();
            assertEquals(RuleResult.ResultType.MATCH, blockingEvaluation.get(5, TimeUnit.SECONDS).getResultType());
            shutdownFuture.get(5, TimeUnit.SECONDS);

            assertTrue(starterFailures.isEmpty(), () -> "Queued starters failed unexpectedly: " + starterFailures);
            assertEquals(starterCount, rejected,
                    "All queued starters released after shutdown begins should be rejected");
            assertEquals(0, matched,
                    "No queued starter should be admitted once shutdown is already rejecting work");
        } finally {
            executor.shutdownNow();
        }
    }

    private IllegalStateException awaitShutdownRejection(RulesEngine engine, Rule immediateRule) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            try {
                engine.executeRule(immediateRule, Map.of());
            } catch (IllegalStateException exception) {
                return exception;
            }
            Thread.yield();
        }
        fail("Engine never transitioned to rejecting new work during shutdown");
        return null;
    }

    private static final class BlockingProbe {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        public boolean awaitAndReturnTrue() throws InterruptedException {
            entered.countDown();
            release.await();
            return true;
        }

        boolean awaitEntered(long timeout, TimeUnit unit) throws InterruptedException {
            return entered.await(timeout, unit);
        }

        void release() {
            release.countDown();
        }
    }
}