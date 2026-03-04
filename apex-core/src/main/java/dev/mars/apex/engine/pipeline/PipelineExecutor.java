package dev.mars.apex.engine.pipeline;

import dev.mars.apex.core.config.pipeline.PipelineConfiguration;
import dev.mars.apex.core.config.pipeline.PipelineStep;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.engine.model.ExecutionStep;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.DataSink;
import dev.mars.apex.core.service.data.external.DataSinkException;
import dev.mars.apex.core.service.data.external.manager.ExternalDataSourceManager;
import dev.mars.apex.core.service.schema.SchemaMetadata;
import dev.mars.apex.core.service.schema.SchemaReaderService;
import dev.mars.apex.core.service.schema.SchemaHtmlReportGenerator;
import dev.mars.apex.core.service.schema.diff.SchemaDiffService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Executes APEX pipelines based on YAML configuration.
 * Handles step dependencies, error handling, and monitoring.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-05
 */
public class PipelineExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineExecutor.class);

    private final ExternalDataSourceManager dataSourceManager;
    private final Map<String, DataSink> dataSinks;
    private final Map<String, Object> pipelineContext;
    private final Map<String, ExecutionStep> stepResults;
    private PipelineConfiguration currentPipeline; // Store current pipeline for access to execution config
    private final ExpressionParser expressionParser;
    private final SchemaReaderService schemaReaderService;
    private final SchemaHtmlReportGenerator reportGenerator;
    private final SchemaDiffService schemaDiffService;
    private final TransformationStepExecutor transformationExecutor;
    private final SchemaStepExecutor schemaStepExecutor;

    public PipelineExecutor(ExternalDataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
        this.dataSinks = new ConcurrentHashMap<>();
        this.pipelineContext = new ConcurrentHashMap<>();
        this.stepResults = new ConcurrentHashMap<>();
        this.expressionParser = dev.mars.apex.engine.core.SpelParserHolder.INSTANCE;
        this.schemaReaderService = new SchemaReaderService();
        this.reportGenerator = new SchemaHtmlReportGenerator();
        this.schemaDiffService = new SchemaDiffService();
        this.transformationExecutor = new TransformationStepExecutor(expressionParser);
        this.schemaStepExecutor = new SchemaStepExecutor(
            dataSourceManager, schemaReaderService, reportGenerator, 
            schemaDiffService, pipelineContext, stepResults);
    }

    /**
     * Add a data sink to the executor.
     */
    public void addDataSink(String name, DataSink dataSink) {
        dataSinks.put(name, dataSink);
    }
    
    /**
     * Execute a pipeline configuration and return a RuleResult directly.
     *
     * <p>Pipeline steps are captured as {@link ExecutionStep} objects in the result's
     * execution path, eliminating the intermediate PipelineStepResult/YamlPipelineExecutionResult
     * conversion layer.</p>
     *
     * @param pipeline the pipeline configuration to execute
     * @return RuleResult with execution path containing pipeline steps
     */
    public RuleResult execute(PipelineConfiguration pipeline) {
        if (pipeline == null) {
            throw new DataPipelineException("Pipeline configuration is null");
        }

        LOGGER.info("Executing pipeline: {}", pipeline.getName());

        // Store pipeline for access to execution configuration
        this.currentPipeline = pipeline;

        long startTime = System.currentTimeMillis();
        List<ExecutionStep> executionSteps = new ArrayList<>();

        try {
            // Validate pipeline configuration
            validatePipeline(pipeline);

            // Initialize data sinks
            initializeDataSinks(pipeline);

            // Execute pipeline steps
            if ("parallel".equalsIgnoreCase(pipeline.getExecution().getMode())) {
                executeStepsInParallel(pipeline.getSteps(), executionSteps);
            } else {
                executeStepsSequentially(pipeline.getSteps(), executionSteps);
            }

            long durationMs = System.currentTimeMillis() - startTime;

            LOGGER.info("Pipeline '{}' completed successfully in {}ms",
                pipeline.getName(), durationMs);

            RuleResult result = RuleResult.match("pipeline:" + pipeline.getName(),
                    "Pipeline executed successfully", SeverityConstants.INFO)
                    .toBuilder().executionPath(executionSteps).build();
            return result;

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;

            // Log error without stack trace, with debug for full details
            LOGGER.error("Pipeline '{}' failed after {}ms: {}",
                pipeline.getName(), durationMs, e.getMessage());
            LOGGER.debug("Full exception details for pipeline '{}':", pipeline.getName(), e);

            RuleResult result = RuleResult.error("pipeline:" + pipeline.getName(),
                    "Pipeline execution failed: " + e.getMessage())
                    .toBuilder().executionPath(executionSteps).build();
            return result;

        } finally {
            // Note: Data sinks are NOT shut down here - they are managed by the RulesEngine
            // and will be shut down when RulesEngine.shutdown() is called
            this.currentPipeline = null;
        }
    }
    
    /**
     * Validate pipeline configuration.
     */
    private void validatePipeline(PipelineConfiguration pipeline) {
        if (pipeline.getSteps() == null || pipeline.getSteps().isEmpty()) {
            throw new DataPipelineException("Pipeline has no steps defined");
        }
        
        // Check for circular dependencies
        validateStepDependencies(pipeline.getSteps());
        
        // Validate step configurations
        for (PipelineStep step : pipeline.getSteps()) {
            validateStep(step);
        }
    }
    
    /**
     * Validate individual step configuration.
     */
    private void validateStep(PipelineStep step) {
        if (step.getName() == null || step.getName().trim().isEmpty()) {
            throw new DataPipelineException("Step name is required");
        }
        
        if (step.getType() == null || step.getType().trim().isEmpty()) {
            throw new DataPipelineException("Step type is required for step: " + step.getName());
        }
        
        // Validate step-specific requirements
        if (step.isExtractStep() && step.getSource() == null) {
            throw new DataPipelineException("Extract step requires source: " + step.getName());
        }
        
        if (step.isLoadStep() && step.getSink() == null) {
            throw new DataPipelineException("Load step requires sink: " + step.getName());
        }
        
        if (step.isReadSchemaStep() && step.getSource() == null) {
            LOGGER.error("[Pipeline.Validation] Read-schema step '{}' is missing required 'source' property", step.getName());
            throw new DataPipelineException("Read-schema step requires source: " + step.getName());
        }
        
        LOGGER.debug("[Pipeline.Validation] Step '{}' validation passed", step.getName());
    }
    
    /**
     * Validate step dependencies for circular references.
     */
    private void validateStepDependencies(List<PipelineStep> steps) {
        Map<String, Set<String>> dependencies = new HashMap<>();

        // Build dependency graph
        for (PipelineStep step : steps) {
            dependencies.put(step.getName(),
                step.getDependsOn() != null ? new HashSet<>(step.getDependsOn()) : new HashSet<>());
        }

        // Check for circular dependencies using DFS
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String stepName : dependencies.keySet()) {
            if (hasCircularDependency(stepName, dependencies, visited, recursionStack)) {
                throw new DataPipelineException("Circular dependency detected involving step: " + stepName);
            }
        }
    }
    
    /**
     * Check for circular dependencies using DFS.
     */
    private boolean hasCircularDependency(String stepName, Map<String, Set<String>> dependencies,
                                        Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(stepName)) {
            return true;
        }
        
        if (visited.contains(stepName)) {
            return false;
        }
        
        visited.add(stepName);
        recursionStack.add(stepName);
        
        Set<String> stepDeps = dependencies.get(stepName);
        if (stepDeps != null) {
            for (String dep : stepDeps) {
                if (hasCircularDependency(dep, dependencies, visited, recursionStack)) {
                    return true;
                }
            }
        }
        
        recursionStack.remove(stepName);
        return false;
    }
    
    /**
     * Initialize data sinks referenced in pipeline steps.
     */
    private void initializeDataSinks(PipelineConfiguration pipeline) {
        for (PipelineStep step : pipeline.getSteps()) {
            if (step.getSink() != null && !dataSinks.containsKey(step.getSink())) {
                try {
                    // This would need to be implemented to create data sinks from configuration
                    // For now, we'll assume they're created elsewhere
                    LOGGER.debug("Data sink '{}' will be initialized externally", step.getSink());
                } catch (Exception e) {
                    throw new DataPipelineException("Failed to initialize data sink: " + step.getSink(), e);
                }
            }
        }
    }
    
    /**
     * Execute steps sequentially.
     */
    private void executeStepsSequentially(List<PipelineStep> steps, List<ExecutionStep> executionSteps) {

        // Sort steps by dependencies
        List<PipelineStep> sortedSteps = topologicalSort(steps);

        for (PipelineStep step : sortedSteps) {
            executeStep(step, executionSteps);
        }
    }

    /**
     * Execute steps in parallel where possible.
     */
    private void executeStepsInParallel(List<PipelineStep> steps, List<ExecutionStep> executionSteps) {

        // For now, implement as sequential - parallel execution would require more complex dependency management
        executeStepsSequentially(steps, executionSteps);
    }
    
    /**
     * Execute a single pipeline step with retry support.
     */
    private void executeStep(PipelineStep step, List<ExecutionStep> executionSteps) {
        LOGGER.info("Executing step: {} ({})", step.getName(), step.getType());

        long stepStartTime = System.currentTimeMillis();
        ExecutionStep stepResult = new ExecutionStep();
        stepResult.setName(step.getName());
        stepResult.setType("PIPELINE_STEP");
        stepResult.setRecordsProcessed(0);
        stepResult.setRecordsFailed(0);

        // Get retry configuration from step or use defaults
        int maxRetries = getMaxRetries(step);
        long retryDelayMs = getRetryDelayMs(step);

        // Validate retry parameters
        if (maxRetries < 0) {
            LOGGER.error("Invalid max-retries value: {} in pipeline configuration - must be >= 0. Using 0 (no retries)", maxRetries);
            maxRetries = 0;
        }
        if (retryDelayMs < 0) {
            LOGGER.error("Invalid retry-delay-ms value: {} in pipeline configuration - must be >= 0. Using 0 (no delay)", retryDelayMs);
            retryDelayMs = 0;
        }

        int attempt = 0;

        // Retry loop
        while (attempt <= maxRetries) {
            try {
                if (attempt > 0) {
                    LOGGER.info("Retrying step '{}' (attempt {}/{})", step.getName(), attempt + 1, maxRetries + 1);
                    if (retryDelayMs > 0) {
                        Thread.sleep(retryDelayMs);
                    }
                }

                // Check dependencies
                if (step.hasDependencies()) {
                    for (String dependency : step.getDependsOn()) {
                        ExecutionStep depResult = stepResults.get(dependency);
                        if (depResult == null || !"SUCCESS".equals(depResult.getStatus())) {
                            throw new DataPipelineException("Dependency step failed or not found: " + dependency);
                        }
                    }
                }

                // Execute step based on type
                Object stepData = null;
                if (step.isExtractStep()) {
                    stepData = executeExtractStep(step);
                    // Store extracted data for subsequent steps (only if not null)
                    if (stepData != null) {
                        pipelineContext.put("extractedData", stepData);
                    }
                    // Set metrics for extract step
                    if (stepData instanceof List) {
                        stepResult.setRecordsProcessed(((List<?>) stepData).size());
                    } else if (stepData != null) {
                        stepResult.setRecordsProcessed(1);
                    }
                } else if (step.isTransformStep()) {
                    // Get data from previous extract/transform step
                    Object dataToTransform = pipelineContext.get("extractedData");
                    stepData = executeTransformStep(step, dataToTransform);
                    // Store transformed data for subsequent steps (only if not null)
                    if (stepData != null) {
                        pipelineContext.put("extractedData", stepData);
                    }
                    // Set metrics for transform step
                    if (stepData instanceof List) {
                        stepResult.setRecordsProcessed(((List<?>) stepData).size());
                    } else if (stepData != null) {
                        stepResult.setRecordsProcessed(1);
                    }
                } else if (step.isLoadStep()) {
                    // Get data from previous extract/transform step
                    Object dataToLoad = pipelineContext.get("extractedData");
                    int recordsProcessed = executeLoadStep(step, dataToLoad);
                    // Set metrics for load step
                    stepResult.setRecordsProcessed(recordsProcessed);
                    stepResult.setRecordsFailed(0); // Failed records are not currently tracked for load steps
                } else if (step.isAuditStep()) {
                    // Get data from previous steps for auditing
                    Object dataToAudit = pipelineContext.get("extractedData");
                    executeAuditStep(step, dataToAudit);
                    // Metrics for audit step are set inside executeAuditStep
                } else if (step.isReadSchemaStep()) {
                    // Read schema from data source (single table/file or multiple tables)
                    LOGGER.debug("[Pipeline.Execute] Executing read-schema step: {}", step.getName());
                    stepData = schemaStepExecutor.executeReadSchemaStep(step);
                    
                    // Handle both single schema and map of schemas
                    if (stepData != null) {
                        if (stepData instanceof Map) {
                            // Multiple tables enumerated
                            @SuppressWarnings("unchecked")
                            Map<String, SchemaMetadata> tableSchemas = (Map<String, SchemaMetadata>) stepData;
                            LOGGER.debug("[Pipeline.Execute] Storing map of table schemas in pipeline context");
                            pipelineContext.put("tableSchemas", stepData);
                            LOGGER.info("[Pipeline.Execute] Enumerated {} tables", tableSchemas.size());
                            
                            // Log details of each table
                            tableSchemas.forEach((tableName, schema) -> {
                                LOGGER.info("[Pipeline.Execute] Table: {} - {} columns", 
                                           tableName, schema.getColumns().size());
                            });
                            
                            // Calculate total columns across all tables
                            int totalColumns = tableSchemas.values().stream()
                                .mapToInt(schema -> schema.getColumns().size())
                                .sum();
                            LOGGER.info("[Pipeline.Execute] Total: {} tables, {} columns", 
                                       tableSchemas.size(), totalColumns);
                            
                        } else if (stepData instanceof SchemaMetadata) {
                            // Single table/file
                            LOGGER.debug("[Pipeline.Execute] Storing schema metadata in pipeline context");
                            pipelineContext.put("schemaMetadata", stepData);
                            SchemaMetadata schema = (SchemaMetadata) stepData;
                            LOGGER.info("[Pipeline.Execute] Schema metadata stored: {} columns from {}", 
                                       schema.getColumns().size(), schema.getSourceName());
                        } else {
                            LOGGER.error("[Pipeline.Execute] Read-schema step returned unexpected type: {} - expected SchemaMetadata or List<SchemaMetadata>", 
                                       stepData.getClass().getName());
                        }
                    } else {
                        LOGGER.error("[Pipeline.Execute] Read-schema step returned null data - source may be unreachable or misconfigured");
                    }
                    // Set metrics for read-schema step
                    stepResult.setRecordsProcessed(1);
                } else if ("schema-diff".equalsIgnoreCase(step.getType())) {
                    // Compare two schemas from previous read-schema steps
                    LOGGER.debug("[Pipeline.Execute] Executing schema-diff step: {}", step.getName());
                    stepData = schemaStepExecutor.executeSchemaDiffStep(step);
                    
                    if (stepData != null) {
                        pipelineContext.put("schemaDiffResult", stepData);
                        LOGGER.info("[Pipeline.Execute] Schema comparison complete, result stored in context");
                    }
                    stepResult.setRecordsProcessed(1);
                }

                stepResult.setStatus("SUCCESS");
                stepResult.setMessage("Step completed successfully");
                stepResult.setStepData(stepData);
                stepResult.setDurationMs(System.currentTimeMillis() - stepStartTime);

                stepResults.put(step.getName(), stepResult);
                executionSteps.add(stepResult);

                LOGGER.info("Step '{}' completed successfully in {}ms",
                    step.getName(), stepResult.getDurationMs());

                return; // Success - exit retry loop

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new DataPipelineException("Step execution interrupted: " + step.getName(), ie);
            } catch (Exception e) {
                attempt++;

                if (attempt > maxRetries) {
                    // All retries exhausted
                    stepResult.setStatus("FAILURE");
                    stepResult.setMessage(e.getMessage());
                    stepResult.setDurationMs(System.currentTimeMillis() - stepStartTime);

                    stepResults.put(step.getName(), stepResult);
                    executionSteps.add(stepResult);

                    LOGGER.error("Step '{}' failed after {}ms and {} retries: {}",
                        step.getName(), stepResult.getDurationMs(), maxRetries, e.getMessage());

                    if (!step.isOptional()) {
                        throw new DataPipelineException("Required step failed: " + step.getName(), e);
                    }
                } else {
                    LOGGER.error("Step '{}' failed (attempt {}/{}) - will retry: {}",
                        step.getName(), attempt, maxRetries + 1, e.getMessage());
                }
            }
        }
    }

    /**
     * Get max retries for a step (from step-level config or pipeline-level config).
     */
    private int getMaxRetries(PipelineStep step) {
        // Step-level retry configuration takes precedence
        if (step.getRetry() != null && step.getRetry().getMaxAttempts() > 0) {
            return step.getRetry().getMaxAttempts() - 1; // maxAttempts includes the initial attempt
        }
        // Fall back to pipeline-level configuration
        if (currentPipeline != null && currentPipeline.getExecution() != null) {
            return currentPipeline.getExecution().getMaxRetries();
        }
        // Default: no retries
        return 0;
    }

    /**
     * Get retry delay for a step (from step-level config or pipeline-level config).
     */
    private long getRetryDelayMs(PipelineStep step) {
        // Step-level retry configuration takes precedence
        if (step.getRetry() != null && step.getRetry().getDelayMs() > 0) {
            return step.getRetry().getDelayMs();
        }
        // Fall back to pipeline-level configuration
        if (currentPipeline != null && currentPipeline.getExecution() != null) {
            return currentPipeline.getExecution().getRetryDelayMs();
        }
        // Default: no delay
        return 0;
    }
    
    /**
     * Execute an extract step.
     */
    private Object executeExtractStep(PipelineStep step) {
        ExternalDataSource dataSource = dataSourceManager.getDataSource(step.getSource());
        if (dataSource == null) {
            throw new DataPipelineException("Data source not found: " + step.getSource());
        }

        try {
            // Use getData() for named query resolution support
            // ExternalDataSource extends DataSource, so we can use getData() for named query resolution
            return dataSource.getData(step.getOperation());
        } catch (Exception e) {
            throw new DataPipelineException("Extract step failed: " + step.getName(), e);
        }
    }

    /**
     * Execute a transform step.
     * Delegates to TransformationStepExecutor for actual transformation logic.
     */
    private Object executeTransformStep(PipelineStep step, Object data) {
        return transformationExecutor.executeTransformStep(step, data);
    }

    /**
     * Execute a load step.
     *
     * @return the number of records successfully processed
     */
    private int executeLoadStep(PipelineStep step, Object data) {
        LOGGER.info("Looking for data sink: '{}' in available sinks: {}", step.getSink(), dataSinks.keySet());
        DataSink dataSink = dataSinks.get(step.getSink());
        if (dataSink == null) {
            throw new DataPipelineException("Data sink not found: " + step.getSink());
        }
        LOGGER.info("Found data sink: {} (type: {})", dataSink.getName(), dataSink.getClass().getSimpleName());

        if (data == null) {
            throw new DataPipelineException("No data available for load step: " + step.getName());
        }

        int totalRecordsProcessed = 0;

        try {
            LOGGER.info("Loading data to sink '{}' using operation '{}'",
                step.getSink(), step.getOperation());

            // Process the data based on its type
            if (data instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> dataList = (List<Object>) data;
                LOGGER.info("Processing {} records for load step '{}'", dataList.size(), step.getName());

                int successCount = 0;
                int skippedCount = 0;

                // Process each record with graceful error handling
                for (Object record : dataList) {
                    try {
                        LOGGER.info("About to call dataSink.write('{}', {}) on sink: {}",
                            step.getOperation(), record.getClass().getSimpleName(), dataSink.getClass().getSimpleName());
                        dataSink.write(step.getOperation(), record);
                        successCount++;
                    } catch (DataSinkException e) {
                        if (e.getErrorType() == DataSinkException.ErrorType.DATA_INTEGRITY_ERROR) {
                            // Log and skip data integrity violations
                            LOGGER.error("Skipping record due to data integrity violation: {} - Record: {}",
                                       e.getMessage(), record);
                            skippedCount++;
                        } else {
                            // Re-throw other types of errors
                            throw e;
                        }
                    }
                }

                LOGGER.info("Load step '{}' completed: {} records loaded successfully, {} records skipped due to data integrity issues",
                           step.getName(), successCount, skippedCount);

                if (successCount == 0 && skippedCount > 0) {
                    LOGGER.error("All {} records were skipped due to data integrity violations in step '{}' - no data was loaded",
                               skippedCount, step.getName());
                }

                totalRecordsProcessed = successCount;
            } else {
                // Single record
                try {
                    dataSink.write(step.getOperation(), data);
                    LOGGER.info("Successfully loaded single record to sink '{}'", step.getSink());
                    totalRecordsProcessed = 1;
                } catch (DataSinkException e) {
                    if (e.getErrorType() == DataSinkException.ErrorType.DATA_INTEGRITY_ERROR) {
                        // Log and continue for data integrity violations
                        LOGGER.error("Skipped single record due to data integrity violation: {} - Record: {}",
                                   e.getMessage(), data);
                        totalRecordsProcessed = 0;
                    } else {
                        // Re-throw other types of errors
                        throw e;
                    }
                }
            }
        } catch (Exception e) {
            throw new DataPipelineException("Load step failed: " + step.getName(), e);
        }

        return totalRecordsProcessed;
    }
    
    /**
     * Execute an audit step.
     */
    private void executeAuditStep(PipelineStep step, Object data) {
        DataSink dataSink = dataSinks.get(step.getSink());
        if (dataSink == null) {
            throw new DataPipelineException("Data sink not found for audit step: " + step.getSink());
        }

        if (data == null) {
            LOGGER.error("No data available for audit step: {} - upstream extract/transform may have failed", step.getName());
            return;
        }

        try {
            LOGGER.info("Writing audit records to sink '{}' using operation '{}'",
                step.getSink(), step.getOperation());

            // Process the data for auditing
            if (data instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> dataList = (List<Object>) data;
                LOGGER.info("Auditing {} records for step '{}'", dataList.size(), step.getName());

                // Create audit records for each data record
                for (Object record : dataList) {
                    // Create audit record with metadata
                    Map<String, Object> auditRecord = new HashMap<>();
                    auditRecord.put("original_data", record);
                    auditRecord.put("pipeline_name", "customer-etl-pipeline");
                    auditRecord.put("step_name", step.getName());
                    auditRecord.put("timestamp", System.currentTimeMillis());
                    auditRecord.put("status", "processed");

                    dataSink.write(step.getOperation(), auditRecord);
                }

                LOGGER.info("Successfully wrote {} audit records to sink '{}'", dataList.size(), step.getSink());
            } else {
                // Single record audit
                Map<String, Object> auditRecord = new HashMap<>();
                auditRecord.put("original_data", data);
                auditRecord.put("pipeline_name", "customer-etl-pipeline");
                auditRecord.put("step_name", step.getName());
                auditRecord.put("timestamp", System.currentTimeMillis());
                auditRecord.put("status", "processed");

                dataSink.write(step.getOperation(), auditRecord);
                LOGGER.info("Successfully wrote single audit record to sink '{}'", step.getSink());
            }
        } catch (Exception e) {
            throw new DataPipelineException("Audit step failed: " + step.getName(), e);
        }
    }
    
    /**
     * Topological sort of pipeline steps based on dependencies.
     */
    private List<PipelineStep> topologicalSort(List<PipelineStep> steps) {
        // Simple implementation - in practice would need proper topological sorting
        return steps.stream()
            .sorted((a, b) -> {
                if (a.hasDependencies() && !b.hasDependencies()) return 1;
                if (!a.hasDependencies() && b.hasDependencies()) return -1;
                return 0;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Shutdown all data sinks.
     */
    private void shutdownDataSinks() {
        for (DataSink sink : dataSinks.values()) {
            try {
                sink.shutdown();
            } catch (Exception e) {
                LOGGER.warn("Error shutting down data sink: {}", e.getMessage());
                LOGGER.debug("Full exception details:", e);
            }
        }
        dataSinks.clear();
    }
}

