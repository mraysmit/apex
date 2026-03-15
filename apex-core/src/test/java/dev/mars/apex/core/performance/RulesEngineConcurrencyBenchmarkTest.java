package dev.mars.apex.core.performance;

import dev.mars.apex.core.config.loader.ConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class RulesEngineConcurrencyBenchmarkTest {

    private static final Logger logger = LoggerFactory.getLogger(RulesEngineConcurrencyBenchmarkTest.class);

    private static final int WARMUP_ITERATIONS = 100;
    private static final int MEASUREMENT_OPERATIONS = 600;
    private static final int MEASUREMENT_SAMPLES = 3;
    private static final List<Integer> CONCURRENCY_LEVELS = List.of(1, 8);

    private ConfigurationLoader yamlLoader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        yamlLoader = new ConfigurationLoader();
    }

    @Test
    void shouldGenerateRepeatableConcurrencyBenchmarkReport() throws Exception {
        List<BenchmarkProfile> profiles = buildProfiles();
        List<BenchmarkMetrics> metrics = new ArrayList<>();

        for (BenchmarkProfile profile : profiles) {
            YamlRuleConfiguration config = yamlLoader.fromYamlString(profile.yaml());
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            try {
                warmup(engine, profile.payloads(), profile.expectedFields());

                for (int concurrency : CONCURRENCY_LEVELS) {
                    for (int sampleNumber = 1; sampleNumber <= MEASUREMENT_SAMPLES; sampleNumber++) {
                        BenchmarkMetrics benchmarkMetrics = runBenchmark(
                                engine,
                                profile.payloads(),
                                profile.expectedFields(),
                                profile.profileName(),
                                concurrency,
                                sampleNumber
                        );
                        metrics.add(benchmarkMetrics);
                        logMetrics(benchmarkMetrics);
                    }
                }
            } finally {
                engine.shutdown();
            }
        }

        List<BenchmarkSummary> summaries = summarizeMetrics(metrics);
        Path reportPath = writeReport(metrics, summaries);
        logger.info("Benchmark report written to {}", reportPath.toAbsolutePath());

        assertTrue(Files.exists(reportPath), "Benchmark report should be written");
        assertEquals(profiles.size() * CONCURRENCY_LEVELS.size() * MEASUREMENT_SAMPLES, metrics.size(), "Expected one benchmark result per profile, concurrency level, and sample");
        assertTrue(metrics.stream().allMatch(metric -> metric.failures() == 0), "Benchmark workload should complete without failures");
        assertTrue(metrics.stream().allMatch(metric -> metric.throughputPerSecond() > 0.0), "Throughput must be positive");
        assertTrue(metrics.stream().allMatch(metric -> metric.p99LatencyMs() >= metric.p95LatencyMs()), "Latency percentiles should be monotonic");
        assertEquals(profiles.size() * CONCURRENCY_LEVELS.size(), summaries.size(), "Expected one benchmark summary per profile and concurrency level");
    }

    private List<BenchmarkProfile> buildProfiles() throws SQLException, IOException {
        List<Map<String, Object>> payloads = buildPayloads();
        List<String> expectedFields = List.of("currencyName", "settlementBucket", "processingQueue");

        BenchmarkProfile inlineProfile = new BenchmarkProfile(
                "inline-lookup-baseline",
                "Shared engine with inline lookup enrichment and calculation enrichments.",
                payloads,
                expectedFields,
                buildInlineBenchmarkYaml()
        );

        Path databasePath = tempDir.resolve("benchmark-h2").resolve("currency-benchmark");
        initializeBenchmarkDatabase(databasePath);
        BenchmarkProfile databaseProfile = new BenchmarkProfile(
                "h2-database-lookup",
                "Shared engine with H2-backed lookup enrichment and calculation enrichments.",
                payloads,
                expectedFields,
                buildDatabaseBenchmarkYaml(databasePath)
        );

        return List.of(inlineProfile, databaseProfile);
    }

    private void warmup(RulesEngine engine, List<Map<String, Object>> payloads, List<String> expectedFields) {
        for (int iteration = 0; iteration < WARMUP_ITERATIONS; iteration++) {
            Map<String, Object> payload = new HashMap<>(payloads.get(iteration % payloads.size()));
            RuleResult result = engine.evaluate(payload);
            assertSuccessfulResult(result, expectedFields);
        }
    }

    private BenchmarkMetrics runBenchmark(
            RulesEngine engine,
            List<Map<String, Object>> payloads,
            List<String> expectedFields,
            String profileName,
                int concurrency,
                int sampleNumber
    )
            throws InterruptedException, ExecutionException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                concurrency,
                concurrency,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()
        );
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger maxQueueDepth = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        AtomicInteger payloadIndex = new AtomicInteger();
        List<Future<Long>> futures = new ArrayList<>(MEASUREMENT_OPERATIONS);

        long heapBefore = usedHeapBytes();

        for (int operation = 0; operation < MEASUREMENT_OPERATIONS; operation++) {
            futures.add(executor.submit(() -> {
                startGate.await();

                int currentIndex = payloadIndex.getAndIncrement();
                Map<String, Object> payload = new HashMap<>(payloads.get(currentIndex % payloads.size()));

                long startNs = System.nanoTime();
                try {
                    RuleResult result = engine.evaluate(payload);
                    assertSuccessfulResult(result, expectedFields);
                } catch (AssertionError | RuntimeException exception) {
                    failures.incrementAndGet();
                    throw exception;
                }
                return System.nanoTime() - startNs;
            }));
            maxQueueDepth.accumulateAndGet(executor.getQueue().size(), Math::max);
        }

        long wallClockStartNs = System.nanoTime();
        startGate.countDown();

        List<Long> latenciesNs = new ArrayList<>(MEASUREMENT_OPERATIONS);
        for (Future<Long> future : futures) {
            latenciesNs.add(future.get());
        }

        long wallClockDurationNs = System.nanoTime() - wallClockStartNs;
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "Benchmark executor should terminate promptly");

        long heapAfter = usedHeapBytes();

        Collections.sort(latenciesNs);
        return new BenchmarkMetrics(
                profileName,
                concurrency,
            sampleNumber,
                MEASUREMENT_OPERATIONS,
                nanosToMillis(wallClockDurationNs),
                throughputPerSecond(MEASUREMENT_OPERATIONS, wallClockDurationNs),
                nanosToMillis(averageLatencyNs(latenciesNs)),
                nanosToMillis(percentile(latenciesNs, 0.50)),
                nanosToMillis(percentile(latenciesNs, 0.95)),
                nanosToMillis(percentile(latenciesNs, 0.99)),
                nanosToMillis(latenciesNs.get(latenciesNs.size() - 1)),
                heapAfter - heapBefore,
                maxQueueDepth.get(),
                failures.get()
        );
    }

    private void assertSuccessfulResult(RuleResult result, List<String> expectedFields) {
        assertTrue(result != null, "Benchmark evaluation should succeed");
        Map<String, Object> enrichedData = result.getEnrichedData();
        for (String expectedField : expectedFields) {
            assertTrue(enrichedData.containsKey(expectedField), "Expected benchmark workload to populate " + expectedField);
        }
    }

    private void logMetrics(BenchmarkMetrics metrics) {
        logger.info("Profile={} concurrency={} sample={} ops={} throughput={} ops/s avg={}ms p50={}ms p95={}ms p99={}ms max={}ms heapDelta={} bytes maxQueueDepth={}",
                metrics.profileName(),
                metrics.concurrency(),
            metrics.sampleNumber(),
                metrics.operations(),
                format(metrics.throughputPerSecond()),
                format(metrics.averageLatencyMs()),
                format(metrics.p50LatencyMs()),
                format(metrics.p95LatencyMs()),
                format(metrics.p99LatencyMs()),
                format(metrics.maxLatencyMs()),
                metrics.heapDeltaBytes(),
                metrics.maxQueueDepth());
    }

    private List<BenchmarkSummary> summarizeMetrics(List<BenchmarkMetrics> metrics) {
        Map<String, List<BenchmarkMetrics>> groupedMetrics = new LinkedHashMap<>();

        for (BenchmarkMetrics metric : metrics) {
            String summaryKey = metric.profileName() + "|" + metric.concurrency();
            groupedMetrics.computeIfAbsent(summaryKey, ignored -> new ArrayList<>()).add(metric);
        }

        List<BenchmarkSummary> summaries = new ArrayList<>();
        for (List<BenchmarkMetrics> group : groupedMetrics.values()) {
            BenchmarkMetrics first = group.get(0);
            summaries.add(new BenchmarkSummary(
                    first.profileName(),
                    first.concurrency(),
                    group.size(),
                    median(group.stream().map(BenchmarkMetrics::wallClockMs).toList()),
                    median(group.stream().map(BenchmarkMetrics::throughputPerSecond).toList()),
                    median(group.stream().map(BenchmarkMetrics::averageLatencyMs).toList()),
                    median(group.stream().map(BenchmarkMetrics::p50LatencyMs).toList()),
                    median(group.stream().map(BenchmarkMetrics::p95LatencyMs).toList()),
                    median(group.stream().map(BenchmarkMetrics::p99LatencyMs).toList()),
                    median(group.stream().map(BenchmarkMetrics::maxLatencyMs).toList()),
                    medianLong(group.stream().map(BenchmarkMetrics::heapDeltaBytes).toList()),
                    medianInt(group.stream().map(BenchmarkMetrics::maxQueueDepth).toList()),
                    group.stream().mapToInt(BenchmarkMetrics::failures).max().orElse(0)
            ));
        }

        return summaries;
    }

    private Path writeReport(List<BenchmarkMetrics> metrics, List<BenchmarkSummary> summaries) throws IOException {
        Path reportDirectory = Path.of("target", "benchmark-reports");
        Files.createDirectories(reportDirectory);

        Path reportPath = reportDirectory.resolve("rules-engine-concurrency-benchmark.md");
        StringBuilder report = new StringBuilder();
        report.append("# RulesEngine Concurrency Benchmark\n\n");
        report.append("Generated: ")
                .append(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .append("\n\n");
        report.append("- Warmup iterations: ").append(WARMUP_ITERATIONS).append("\n");
        report.append("- Measured operations per run: ").append(MEASUREMENT_OPERATIONS).append("\n");
        report.append("- Measured samples per profile/concurrency: ").append(MEASUREMENT_SAMPLES).append("\n");
        report.append("- Payload shape: mixed financial trade inputs with rule evaluation, lookup enrichment, and downstream calculation enrichments\n");
        report.append("- Report scope: shared-engine baseline plus H2-backed downstream lookup profile\n\n");
        report.append("## Profiles\n\n");
        report.append("- `inline-lookup-baseline`: in-memory inline lookup dataset, no downstream I/O.\n");
        report.append("- `h2-database-lookup`: H2-backed lookup dataset using `connection-name` and parameterized SQL.\n\n");
        report.append("## Median Summary\n\n");
        report.append("| Profile | Concurrency | Samples | Operations | Median Wall Time (ms) | Median Throughput (ops/s) | Median Avg (ms) | Median p50 (ms) | Median p95 (ms) | Median p99 (ms) | Median Max (ms) | Median Heap Delta (bytes) | Median Max Queue Depth | Max Failures |\n");
        report.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");

        for (BenchmarkSummary summary : summaries) {
            report.append("| ")
                .append(summary.profileName()).append(" | ")
                .append(summary.concurrency()).append(" | ")
                .append(summary.sampleCount()).append(" | ")
                .append(MEASUREMENT_OPERATIONS).append(" | ")
                .append(format(summary.medianWallClockMs())).append(" | ")
                .append(format(summary.medianThroughputPerSecond())).append(" | ")
                .append(format(summary.medianAverageLatencyMs())).append(" | ")
                .append(format(summary.medianP50LatencyMs())).append(" | ")
                .append(format(summary.medianP95LatencyMs())).append(" | ")
                .append(format(summary.medianP99LatencyMs())).append(" | ")
                .append(format(summary.medianMaxLatencyMs())).append(" | ")
                .append(summary.medianHeapDeltaBytes()).append(" | ")
                .append(summary.medianMaxQueueDepth()).append(" | ")
                .append(summary.maxFailures()).append(" |\n");
        }

        report.append("\n## Raw Samples\n\n");
        report.append("| Profile | Concurrency | Sample | Operations | Wall Time (ms) | Throughput (ops/s) | Avg (ms) | p50 (ms) | p95 (ms) | p99 (ms) | Max (ms) | Heap Delta (bytes) | Max Queue Depth | Failures |\n");
        report.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");

        for (BenchmarkMetrics metric : metrics) {
            report.append("| ")
                .append(metric.profileName()).append(" | ")
                .append(metric.concurrency()).append(" | ")
                .append(metric.sampleNumber()).append(" | ")
                    .append(metric.operations()).append(" | ")
                    .append(format(metric.wallClockMs())).append(" | ")
                    .append(format(metric.throughputPerSecond())).append(" | ")
                    .append(format(metric.averageLatencyMs())).append(" | ")
                    .append(format(metric.p50LatencyMs())).append(" | ")
                    .append(format(metric.p95LatencyMs())).append(" | ")
                    .append(format(metric.p99LatencyMs())).append(" | ")
                    .append(format(metric.maxLatencyMs())).append(" | ")
                    .append(metric.heapDeltaBytes()).append(" | ")
                    .append(metric.maxQueueDepth()).append(" | ")
                    .append(metric.failures()).append(" |\n");
        }

        Files.writeString(reportPath, report.toString());
        return reportPath;
    }

    private List<Map<String, Object>> buildPayloads() {
        List<String> currencyCodes = List.of("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY");
        List<Map<String, Object>> payloads = new ArrayList<>();

        for (int index = 0; index < 200; index++) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tradeId", "TRD-" + index);
            payload.put("currencyCode", currencyCodes.get(index % currencyCodes.size()));
            payload.put("amount", 25_000.0 + (index * 1_250.0));
            payload.put("counterpartyRating", (index % 3 == 0) ? "A" : "BBB");
            payload.put("region", (index % 2 == 0) ? "EU" : "US");
            payload.put("processingDate", "2026-03-14");
            payloads.add(payload);
        }

        return payloads;
    }

    private void initializeBenchmarkDatabase(Path databasePath) throws IOException, SQLException {
        Files.createDirectories(databasePath.getParent());
        String jdbcUrl = buildJdbcUrl(databasePath);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS benchmark_currency");
            statement.execute("CREATE TABLE benchmark_currency (code VARCHAR(3) PRIMARY KEY, currency_name VARCHAR(64), symbol VARCHAR(8), settlement_bucket VARCHAR(16))");
            statement.execute("INSERT INTO benchmark_currency (code, currency_name, symbol, settlement_bucket) VALUES ('USD', 'US Dollar', '$', 'T1')");
            statement.execute("INSERT INTO benchmark_currency (code, currency_name, symbol, settlement_bucket) VALUES ('EUR', 'Euro', '€', 'T2')");
            statement.execute("INSERT INTO benchmark_currency (code, currency_name, symbol, settlement_bucket) VALUES ('GBP', 'British Pound Sterling', '£', 'T2')");
            statement.execute("INSERT INTO benchmark_currency (code, currency_name, symbol, settlement_bucket) VALUES ('JPY', 'Japanese Yen', '¥', 'T3')");
            statement.execute("INSERT INTO benchmark_currency (code, currency_name, symbol, settlement_bucket) VALUES ('CHF', 'Swiss Franc', 'CHF', 'T2')");
            statement.execute("INSERT INTO benchmark_currency (code, currency_name, symbol, settlement_bucket) VALUES ('CAD', 'Canadian Dollar', 'C$', 'T1')");
            statement.execute("INSERT INTO benchmark_currency (code, currency_name, symbol, settlement_bucket) VALUES ('AUD', 'Australian Dollar', 'A$', 'T2')");
            statement.execute("INSERT INTO benchmark_currency (code, currency_name, symbol, settlement_bucket) VALUES ('CNY', 'Chinese Yuan', '¥', 'T3')");
        }
    }

    private String buildInlineBenchmarkYaml() {
        return String.join(System.lineSeparator(),
                "metadata:",
                "  id: \"rules-engine-concurrency-benchmark\"",
                "  name: \"Rules Engine Concurrency Benchmark\"",
                "  version: \"1.0.0\"",
                "  description: \"Shared engine benchmark with rules and inline lookup enrichment\"",
                "  type: \"rule-config\"",
                "  author: \"apex.benchmark@company.com\"",
                buildSharedRulesYaml(),
                buildInlineLookupYaml(),
                buildSharedCalculationYaml()
        );
    }

    private String buildDatabaseBenchmarkYaml(Path databasePath) {
        return String.join(System.lineSeparator(),
                "metadata:",
                "  id: \"rules-engine-concurrency-benchmark\"",
                "  name: \"Rules Engine Concurrency Benchmark\"",
                "  version: \"1.0.0\"",
                "  description: \"Shared engine benchmark with rules and H2-backed lookup enrichment\"",
                "  type: \"rule-config\"",
                "  author: \"apex.benchmark@company.com\"",
                buildSharedRulesYaml(),
                "data-sources:",
                "  - name: \"benchmark-database\"",
                "    type: \"database\"",
                "    source-type: \"h2\"",
                "    connection:",
                "      database: \"" + normalizePath(databasePath) + "\"",
                "      username: \"sa\"",
                "      password: \"\"",
                buildDatabaseLookupYaml(),
                buildSharedCalculationYaml()
        );
    }

    private String buildSharedRulesYaml() {
        return String.join(System.lineSeparator(),
                "rules:",
                "  - id: \"currency-present\"",
                "    name: \"Currency Present\"",
                "    condition: \"#currencyCode != null && #currencyCode.length() == 3\"",
                "    message: \"Currency code must be present\"",
                "    severity: \"ERROR\"",
                "    result-field: \"currencyCodeValid\"",
                "  - id: \"positive-amount\"",
                "    name: \"Positive Amount\"",
                "    condition: \"#amount > 0\"",
                "    message: \"Amount must be positive\"",
                "    severity: \"ERROR\"",
                "    result-field: \"amountValid\"",
                "  - id: \"high-value-trade\"",
                "    name: \"High Value Trade\"",
                "    condition: \"#amount >= 100000\"",
                "    message: \"Trade exceeds high value threshold\"",
                "    severity: \"INFO\"",
                "    result-field: \"highValueTrade\""
        );
    }

    private String buildInlineLookupYaml() {
        return String.join(System.lineSeparator(),
                "enrichments:",
                "  - id: \"currency-details-lookup\"",
                "    name: \"Currency Details Lookup\"",
                "    type: \"lookup-enrichment\"",
                "    enabled: true",
                "    condition: \"#currencyCode != null\"",
                "    lookup-config:",
                "      lookup-key: \"#currencyCode\"",
                "      lookup-dataset:",
                "        type: \"inline\"",
                "        key-field: \"code\"",
                "        data:",
                "          - code: \"USD\"",
                "            name: \"US Dollar\"",
                "            symbol: \"$\"",
                "            settlementBucket: \"T1\"",
                "          - code: \"EUR\"",
                "            name: \"Euro\"",
                "            symbol: \"€\"",
                "            settlementBucket: \"T2\"",
                "          - code: \"GBP\"",
                "            name: \"British Pound Sterling\"",
                "            symbol: \"£\"",
                "            settlementBucket: \"T2\"",
                "          - code: \"JPY\"",
                "            name: \"Japanese Yen\"",
                "            symbol: \"¥\"",
                "            settlementBucket: \"T3\"",
                "          - code: \"CHF\"",
                "            name: \"Swiss Franc\"",
                "            symbol: \"CHF\"",
                "            settlementBucket: \"T2\"",
                "          - code: \"CAD\"",
                "            name: \"Canadian Dollar\"",
                "            symbol: \"C$\"",
                "            settlementBucket: \"T1\"",
                "          - code: \"AUD\"",
                "            name: \"Australian Dollar\"",
                "            symbol: \"A$\"",
                "            settlementBucket: \"T2\"",
                "          - code: \"CNY\"",
                "            name: \"Chinese Yuan\"",
                "            symbol: \"¥\"",
                "            settlementBucket: \"T3\"",
                "    field-mappings:",
                "      - source-field: \"name\"",
                "        target-field: \"currencyName\"",
                "        required: true",
                "      - source-field: \"symbol\"",
                "        target-field: \"currencySymbol\"",
                "        required: true",
                "      - source-field: \"settlementBucket\"",
                "        target-field: \"lookupSettlementBucket\"",
                "        required: true"
        );
    }

    private String buildDatabaseLookupYaml() {
        return String.join(System.lineSeparator(),
                "enrichments:",
                "  - id: \"currency-details-lookup\"",
                "    name: \"Currency Details Lookup\"",
                "    type: \"lookup-enrichment\"",
                "    enabled: true",
                "    condition: \"#currencyCode != null\"",
                "    lookup-config:",
                "      lookup-key: \"#currencyCode\"",
                "      lookup-dataset:",
                "        type: \"database\"",
                "        connection-name: \"benchmark-database\"",
                "        query: \"SELECT code, currency_name AS CURRENCY_NAME, symbol AS CURRENCY_SYMBOL, settlement_bucket AS SETTLEMENT_BUCKET FROM benchmark_currency WHERE code = :code\"",
                "        key-field: \"code\"",
                "        parameters:",
                "          - field: \"code\"",
                "            type: \"string\"",
                "    field-mappings:",
                "      - source-field: \"CURRENCY_NAME\"",
                "        target-field: \"currencyName\"",
                "        required: true",
                "      - source-field: \"CURRENCY_SYMBOL\"",
                "        target-field: \"currencySymbol\"",
                "        required: true",
                "      - source-field: \"SETTLEMENT_BUCKET\"",
                "        target-field: \"lookupSettlementBucket\"",
                "        required: true"
        );
    }

    private String buildSharedCalculationYaml() {
        return String.join(System.lineSeparator(),
                "  - id: \"settlement-bucket-calculation\"",
                "    name: \"Settlement Bucket Calculation\"",
                "    type: \"calculation-enrichment\"",
                "    enabled: true",
                "    condition: \"#lookupSettlementBucket != null\"",
                "    calculation-config:",
                "      expression: \"#amount >= 100000 ? 'HIGH_TOUCH' : #lookupSettlementBucket\"",
                "      result-field: \"settlementBucket\"",
                "    field-mappings:",
                "      - source-field: \"settlementBucket\"",
                "        target-field: \"settlementBucket\"",
                "  - id: \"processing-queue-calculation\"",
                "    name: \"Processing Queue Calculation\"",
                "    type: \"calculation-enrichment\"",
                "    enabled: true",
                "    condition: \"#region != null\"",
                "    calculation-config:",
                "      expression: \"#region == 'EU' ? 'EMEA_QUEUE' : 'AMER_QUEUE'\"",
                "      result-field: \"processingQueue\"",
                "    field-mappings:",
                "      - source-field: \"processingQueue\"",
                "        target-field: \"processingQueue\""
        );
    }

    private long averageLatencyNs(List<Long> latenciesNs) {
        long total = 0L;
        for (Long latency : latenciesNs) {
            total += latency;
        }
        return total / latenciesNs.size();
    }

    private long percentile(List<Long> latenciesNs, double percentile) {
        int index = (int) Math.ceil(percentile * latenciesNs.size()) - 1;
        return latenciesNs.get(Math.max(0, Math.min(index, latenciesNs.size() - 1)));
    }

    private double throughputPerSecond(int operations, long wallClockDurationNs) {
        return operations / (wallClockDurationNs / 1_000_000_000.0);
    }

    private long usedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private double median(List<Double> values) {
        List<Double> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);
        int middle = sortedValues.size() / 2;

        if (sortedValues.size() % 2 == 0) {
            return (sortedValues.get(middle - 1) + sortedValues.get(middle)) / 2.0;
        }

        return sortedValues.get(middle);
    }

    private long medianLong(List<Long> values) {
        List<Long> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);
        return sortedValues.get(sortedValues.size() / 2);
    }

    private int medianInt(List<Integer> values) {
        List<Integer> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);
        return sortedValues.get(sortedValues.size() / 2);
    }

    private String normalizePath(Path path) {
        return path.toAbsolutePath().toString().replace('\\', '/');
    }

    private String buildJdbcUrl(Path databasePath) {
        return "jdbc:h2:" + normalizePath(databasePath) + ";MODE=PostgreSQL;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1";
    }

    private record BenchmarkProfile(
            String profileName,
            String description,
            List<Map<String, Object>> payloads,
            List<String> expectedFields,
            String yaml
    ) {
    }

    private record BenchmarkMetrics(
            String profileName,
            int concurrency,
            int sampleNumber,
            int operations,
            double wallClockMs,
            double throughputPerSecond,
            double averageLatencyMs,
            double p50LatencyMs,
            double p95LatencyMs,
            double p99LatencyMs,
            double maxLatencyMs,
            long heapDeltaBytes,
            int maxQueueDepth,
            int failures
    ) {
    }

            private record BenchmarkSummary(
                String profileName,
                int concurrency,
                int sampleCount,
                double medianWallClockMs,
                double medianThroughputPerSecond,
                double medianAverageLatencyMs,
                double medianP50LatencyMs,
                double medianP95LatencyMs,
                double medianP99LatencyMs,
                double medianMaxLatencyMs,
                long medianHeapDeltaBytes,
                int medianMaxQueueDepth,
                int maxFailures
            ) {
            }
}