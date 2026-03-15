package dev.mars.apex.engine.execution;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.pipeline.PipelineConfiguration;
import dev.mars.apex.core.config.pipeline.PipelineStep;
import dev.mars.apex.core.service.data.external.ConnectionStatus;
import dev.mars.apex.core.service.data.external.DataSink;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.DataSourceMetrics;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.factory.DataSinkFactory;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.engine.core.ExpressionEvaluatorService;
import dev.mars.apex.engine.core.UnifiedRuleEvaluator;
import dev.mars.apex.engine.model.ExecutionStep;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("PipelineExecutionManager state isolation")
class PipelineExecutionManagerStateIsolationTest {

    @Test
    @DisplayName("Data source getter returns immutable snapshot")
    void getDataSourcesReturnsImmutableSnapshot() {
        Map<String, ExternalDataSource> dataSources = new ConcurrentHashMap<>();
        Map<String, DataSink> dataSinks = new HashMap<>();

        StaticExternalDataSource firstSource = new StaticExternalDataSource("source-a", List.of(record("A")));
        dataSources.put("source-a", firstSource);

        PipelineExecutionManager manager = new PipelineExecutionManager(
                DataSourceFactory.getInstance(),
                DataSinkFactory.getInstance(),
                dataSources,
                dataSinks,
                new ArrayList<>(),
                new ExpressionEvaluatorService(),
                new RuleGroupEvaluationService(new UnifiedRuleEvaluator()));

        Map<String, ExternalDataSource> snapshot = manager.getDataSources();

        assertEquals(1, snapshot.size(), "Snapshot should include current data sources");
        assertSame(firstSource, snapshot.get("source-a"), "Snapshot should contain the same data source instance");
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.put("source-b", new StaticExternalDataSource("source-b", List.of(record("B")))),
                "Exposed data source map must be immutable");

        dataSources.put("source-b", new StaticExternalDataSource("source-b", List.of(record("B"))));

        assertEquals(1, snapshot.size(), "Snapshot should not reflect later backing-map mutations");
        assertFalse(snapshot.containsKey("source-b"), "Snapshot should be isolated from subsequent changes");
    }

    @Test
    @DisplayName("Each pipeline run gets isolated executor state")
    void executePipelineDoesNotReuseStateAcrossRuns() {
        Map<String, ExternalDataSource> dataSources = new ConcurrentHashMap<>();
        Map<String, DataSink> dataSinks = new HashMap<>();

        dataSources.put("source-a", new StaticExternalDataSource("source-a", List.of(record("A"))));

        PipelineExecutionManager manager = new PipelineExecutionManager(
                DataSourceFactory.getInstance(),
                DataSinkFactory.getInstance(),
                dataSources,
                dataSinks,
                new ArrayList<>(),
                new ExpressionEvaluatorService(),
                new RuleGroupEvaluationService(new UnifiedRuleEvaluator()));

        RuleResult firstResult = manager.executePipeline(validPipeline(), Collections.emptyMap());
        assertEquals(RuleResult.ResultType.MATCH, firstResult.getResultType());
        ExecutionStep firstTransform = stepNamed(firstResult, "transform");
        assertNotNull(firstTransform.getStepData(), "First run should produce transform output");

        RuleResult secondResult = manager.executePipeline(invalidPipelineWithMissingExtract(), Collections.emptyMap());

        assertEquals(RuleResult.ResultType.ERROR, secondResult.getResultType(),
                "A second run must not inherit extractedData or dependency results from a previous pipeline");
        assertTrue(secondResult.getMessage().contains("Required step failed: transform"),
            "Missing dependency should surface in the pipeline error message");
    }

    private static PipelineConfiguration validPipeline() {
        PipelineStep extract = new PipelineStep("extract", "extract");
        extract.setSource("source-a");
        extract.setOperation("records");

        PipelineStep transform = new PipelineStep("transform", "transform");
        transform.setDependsOn(List.of("extract"));
        transform.setTransformations(List.of(fieldAddition("sourceLabel", "pipeline-a")));

        PipelineConfiguration pipeline = new PipelineConfiguration("pipeline-a", "Seed pipeline context");
        pipeline.setExecution(sequentialExecution());
        pipeline.setSteps(List.of(extract, transform));
        return pipeline;
    }

    private static PipelineConfiguration invalidPipelineWithMissingExtract() {
        PipelineStep transform = new PipelineStep("transform", "transform");
        transform.setDependsOn(List.of("extract"));
        transform.setTransformations(List.of(fieldAddition("sourceLabel", "pipeline-b")));

        PipelineConfiguration pipeline = new PipelineConfiguration("pipeline-b", "Should fail without extract");
        pipeline.setExecution(sequentialExecution());
        pipeline.setSteps(List.of(transform));
        return pipeline;
    }

    private static PipelineConfiguration.ExecutionConfiguration sequentialExecution() {
        PipelineConfiguration.ExecutionConfiguration execution = new PipelineConfiguration.ExecutionConfiguration();
        execution.setMode("sequential");
        execution.setMaxRetries(0);
        execution.setRetryDelayMs(0);
        return execution;
    }

    private static Map<String, Object> fieldAddition(String field, String value) {
        Map<String, Object> transformation = new HashMap<>();
        transformation.put("type", "field-addition");
        transformation.put("field", field);
        transformation.put("value", value);
        return transformation;
    }

    private static Map<String, Object> record(String value) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", value);
        return record;
    }

    private static ExecutionStep stepNamed(RuleResult result, String name) {
        return result.getExecutionPath().stream()
                .filter(step -> name.equals(step.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing execution step: " + name));
    }

    private static final class StaticExternalDataSource implements ExternalDataSource {
        private final String name;
        private final List<Map<String, Object>> records;

        private StaticExternalDataSource(String name, List<Map<String, Object>> records) {
            this.name = name;
            this.records = records;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDataType() {
            return "records";
        }

        @Override
        public boolean supportsDataType(String dataType) {
            return "records".equals(dataType);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getData(String dataType, Object... parameters) {
            return (T) records;
        }

        @Override
        public DataSourceType getSourceType() {
            return DataSourceType.CACHE;
        }

        @Override
        public ConnectionStatus getConnectionStatus() {
            return ConnectionStatus.connected("static test source ready");
        }

        @Override
        public DataSourceMetrics getMetrics() {
            return null;
        }

        @Override
        public void initialize(DataSourceConfiguration config) {
        }

        @Override
        public void shutdown() {
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        @Override
        public <T> List<T> query(String query, Map<String, Object> parameters) throws DataSourceException {
            return Collections.emptyList();
        }

        @Override
        public <T> T queryForObject(String query, Map<String, Object> parameters) throws DataSourceException {
            return null;
        }

        @Override
        public <T> List<List<T>> batchQuery(List<String> queries) throws DataSourceException {
            return Collections.emptyList();
        }

        @Override
        public void batchUpdate(List<String> updates) throws DataSourceException {
        }

        @Override
        public DataSourceConfiguration getConfiguration() {
            return null;
        }

        @Override
        public void refresh() throws DataSourceException {
        }

        @Override
        public boolean testConnection() {
            return true;
        }
    }
}