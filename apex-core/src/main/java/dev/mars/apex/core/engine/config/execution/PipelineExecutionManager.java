package dev.mars.apex.core.engine.config.execution;

import dev.mars.apex.core.config.datasink.DataSinkConfiguration;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.pipeline.PipelineConfiguration;
import dev.mars.apex.core.config.yaml.YamlDataSink;
import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.pipeline.DataPipelineException;
import dev.mars.apex.core.engine.pipeline.PipelineExecutor;
import dev.mars.apex.core.engine.pipeline.YamlPipelineExecutionResult;
import dev.mars.apex.core.service.data.external.DataSink;
import dev.mars.apex.core.service.data.external.DataSinkException;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.factory.DataSinkFactory;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.data.external.manager.ExternalDataSourceManager;
import dev.mars.apex.core.service.data.external.registry.DataSourceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.engine.RuleGroupEvaluationService;
import dev.mars.apex.core.service.enrichment.YamlEnrichmentProcessor;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

/**
 * Manages pipeline execution including data source/sink initialization and pipeline execution.
 * 
 * <p>This manager handles:</p>
 * <ul>
 *   <li>Lazy initialization of data sources from YAML configuration</li>
 *   <li>Lazy initialization of data sinks from YAML configuration</li>
 *   <li>Pipeline executor creation and execution</li>
 *   <li>Data source registry management for enrichment processor</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1
 */
public class PipelineExecutionManager {
    private static final Logger logger = LoggerFactory.getLogger(PipelineExecutionManager.class);

    private final DataSourceFactory dataSourceFactory;
    private final DataSinkFactory dataSinkFactory;
    private final DataSourceRegistry dataSourceRegistry;
    private final Map<String, ExternalDataSource> dataSources;
    private final Map<String, DataSink> dataSinks;
    private final List<String> initializationErrors;
    private final ExpressionEvaluatorService evaluatorService;
    private final RuleGroupEvaluationService ruleGroupEvaluationService;
    
    private PipelineExecutor pipelineExecutor; // Lazy-initialized
    private YamlEnrichmentProcessor enrichmentProcessor; // Re-initialized with data sources

    /**
     * Create a new PipelineExecutionManager.
     *
     * @param dataSourceFactory Factory for creating external data sources
     * @param dataSinkFactory Factory for creating data sinks
     * @param dataSources Map to store initialized data sources
     * @param dataSinks Map to store initialized data sinks
     * @param initializationErrors List to collect initialization errors
     * @param evaluatorService Expression evaluator service for enrichments
     * @param ruleGroupEvaluationService Service for canonical rule group evaluation (required)
     */
    public PipelineExecutionManager(
            DataSourceFactory dataSourceFactory,
            DataSinkFactory dataSinkFactory,
            Map<String, ExternalDataSource> dataSources,
            Map<String, DataSink> dataSinks,
            List<String> initializationErrors,
            ExpressionEvaluatorService evaluatorService,
            RuleGroupEvaluationService ruleGroupEvaluationService) {
        this.dataSourceFactory = dataSourceFactory;
        this.dataSinkFactory = dataSinkFactory;
        this.dataSourceRegistry = DataSourceRegistry.getInstance();
        this.dataSources = dataSources;
        this.dataSinks = dataSinks;
        this.initializationErrors = initializationErrors;
        this.evaluatorService = evaluatorService;
        this.ruleGroupEvaluationService = java.util.Objects.requireNonNull(ruleGroupEvaluationService,
                "RuleGroupEvaluationService is required");
    }

    /**
     * Initialize pipeline components (data sources and sinks) from YAML configuration.
     * This method also re-initializes the enrichment processor with the data source registry.
     *
     * @param yamlConfig The YAML configuration containing data source and sink definitions
     * @return Updated enrichment processor if data sources were initialized, null otherwise
     */
    public YamlEnrichmentProcessor initializePipelineComponents(YamlRuleConfiguration yamlConfig) {
        YamlEnrichmentProcessor updatedProcessor = null;
        
        try {
            // Initialize data sources using the unified DataSourceRegistry
            if (yamlConfig.getDataSources() != null && !yamlConfig.getDataSources().isEmpty()) {
                logger.info("Initializing {} data sources via DataSourceRegistry", yamlConfig.getDataSources().size());
                for (YamlDataSource yamlDataSource : yamlConfig.getDataSources()) {
                    try {
                        DataSourceConfiguration config = yamlDataSource.toDataSourceConfiguration();
                        // Use DataSourceRegistry.getOrCreate() - the single source of truth
                        ExternalDataSource dataSource = dataSourceRegistry.getOrCreate(config.getName(), config);
                        dataSources.put(config.getName(), dataSource);
                        logger.debug("Initialized data source via registry: {}", config.getName());
                    } catch (DataSourceException e) {
                        logger.warn("Failed to initialize data source '{}': {}", yamlDataSource.getName(), e.getMessage());
                        initializationErrors.add("Failed to initialize data source '" + yamlDataSource.getName() + "': " + e.getMessage());
                    }
                }
                
                // Re-initialize enrichment processor with data source registry
                // This ensures enrichments can reuse existing data sources instead of creating duplicates
                logger.debug("Re-initializing enrichment processor with {} data sources from registry", dataSources.size());
                updatedProcessor = new YamlEnrichmentProcessor(
                    new LookupServiceRegistry(), 
                    this.evaluatorService,
                    this.dataSources,
                    this.ruleGroupEvaluationService
                );
            }

            // Initialize data sinks
            if (yamlConfig.getDataSinks() != null && !yamlConfig.getDataSinks().isEmpty()) {
                logger.info("Initializing {} data sinks", yamlConfig.getDataSinks().size());
                for (YamlDataSink yamlDataSink : yamlConfig.getDataSinks()) {
                    try {
                        DataSinkConfiguration config = yamlDataSink.toDataSinkConfiguration();
                        DataSink dataSink = dataSinkFactory.createDataSink(config);
                        dataSinks.put(config.getName(), dataSink);
                        logger.debug("Initialized data sink: {}", config.getName());
                    } catch (DataSinkException e) {
                        logger.warn("Failed to initialize data sink '{}': {}", yamlDataSink.getName(), e.getMessage());
                        initializationErrors.add("Failed to initialize data sink '" + yamlDataSink.getName() + "': " + e.getMessage());
                    }
                }
            }

            logger.debug("Pipeline components initialized: {} data sources, {} data sinks",
                    dataSources.size(), dataSinks.size());

        } catch (Exception e) {
            logger.warn("Failed to initialize pipeline components: {}", e.getMessage());
            logger.debug("Pipeline initialization exception details:", e);
            initializationErrors.add("Failed to initialize pipeline components: " + e.getMessage());
        }
        
        return updatedProcessor;
    }

    /**
     * Execute a pipeline configuration.
     *
     * @param pipeline The pipeline configuration to execute
     * @param inputData The input data for the pipeline (not used directly but kept for API consistency)
     * @return RuleResult indicating success or failure
     */
    public RuleResult executePipeline(PipelineConfiguration pipeline, Map<String, Object> inputData) {
        try {
            logger.info("Executing pipeline: {}", pipeline.getName());

            // Lazy-initialize pipeline executor
            if (pipelineExecutor == null) {
                pipelineExecutor = new PipelineExecutor(new DataSourceManagerAdapter());

                // Add all data sinks to executor
                for (Map.Entry<String, DataSink> entry : dataSinks.entrySet()) {
                    pipelineExecutor.addDataSink(entry.getKey(), entry.getValue());
                }
            }

            // Execute pipeline
            YamlPipelineExecutionResult result = pipelineExecutor.execute(pipeline);

            // Convert pipeline steps to ExecutionSteps for tracing
            List<ExecutionStep> pipelineSteps = new ArrayList<>();
            if (result.getStepResults() != null) {
                for (dev.mars.apex.core.engine.pipeline.PipelineStepResult stepResult : result.getStepResults()) {
                    String status = stepResult.isSuccess() ? "SUCCESS" : (stepResult.isSkipped() ? "SKIPPED" : "FAILURE");
                    String message = stepResult.getError() != null ? stepResult.getError() :
                                   (stepResult.isSkipped() ? "Step skipped" : "Step completed successfully");

                    // Use new constructor that captures step data and metrics
                    pipelineSteps.add(new ExecutionStep(
                        stepResult.getStepName(),
                        "PIPELINE_STEP",
                        status,
                        message,
                        stepResult.getDurationMs(),
                        stepResult.getData(),              // Capture step data
                        stepResult.getRecordsProcessed(),  // Capture metrics
                        stepResult.getRecordsFailed()      // Capture metrics
                    ));
                }
            }

            // Convert to RuleResult
            RuleResult ruleResult;
            if (result.isSuccess()) {
                logger.info("Pipeline '{}' executed successfully in {}ms",
                        pipeline.getName(), result.getDurationMs());
                ruleResult = RuleResult.match("pipeline:" + pipeline.getName(),
                        "Pipeline executed successfully", SeverityConstants.INFO);
            } else {
                logger.error("Pipeline '{}' execution failed: {}", pipeline.getName(), result.getError());
                ruleResult = RuleResult.error("pipeline:" + pipeline.getName(),
                        "Pipeline execution failed: " + result.getError());
            }
            
            // Attach the execution path
            ruleResult.setExecutionPath(pipelineSteps);
            return ruleResult;
            
        } catch (DataPipelineException e) {
            logger.error("Pipeline execution failed with exception: {}", e.getMessage());
            logger.debug("Stack trace for pipeline execution failure:", e);
            return RuleResult.error("pipeline:" + pipeline.getName(),
                    "Pipeline execution failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during pipeline execution: {}", e.getMessage());
            logger.debug("Stack trace for unexpected pipeline error:", e);
            return RuleResult.error("pipeline:unknown",
                    "Pipeline execution failed: " + e.getMessage());
        }
    }

    /**
     * Get the data source map for external access.
     * @return Map of data source name to ExternalDataSource
     */
    public Map<String, ExternalDataSource> getDataSources() {
        return dataSources;
    }

    /**
     * Adapter to provide ExternalDataSourceManager interface to PipelineExecutor.
     * This inner class bridges the pipeline executor's data source requirements
     * with the manager's data source registry.
     */
    private class DataSourceManagerAdapter implements ExternalDataSourceManager {
        @Override
        public ExternalDataSource getDataSource(String name) {
            return dataSources.get(name);
        }

        @Override
        public void addDataSource(String name, ExternalDataSource dataSource) {
            dataSources.put(name, dataSource);
        }

        @Override
        public void removeDataSource(String name) {
            dataSources.remove(name);
        }

        @Override
        public boolean hasDataSource(String name) {
            return dataSources.containsKey(name);
        }
    }
}
