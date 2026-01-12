package dev.mars.apex.core.engine.pipeline;

import dev.mars.apex.core.config.pipeline.PipelineConfiguration;
import dev.mars.apex.core.config.pipeline.PipelineStep;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.DataSink;
import dev.mars.apex.core.service.data.external.DataSinkException;
import dev.mars.apex.core.service.data.external.manager.ExternalDataSourceManager;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.schema.SchemaMetadata;
import dev.mars.apex.core.service.schema.SchemaReaderService;
import dev.mars.apex.core.service.schema.SchemaHtmlReportGenerator;
import dev.mars.apex.core.service.schema.DataSourceContext;
import dev.mars.apex.core.service.data.external.database.DatabaseDataSource;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.util.TestAwareLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Executes APEX pipelines based on YAML configuration.
 * Handles step dependencies, error handling, and monitoring.
 * 
 * @author APEX Team
 * @since 1.0.0
 */
public class PipelineExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineExecutor.class);

    private final ExternalDataSourceManager dataSourceManager;
    private final Map<String, DataSink> dataSinks;
    private final Map<String, Object> pipelineContext;
    private final Map<String, PipelineStepResult> stepResults;
    private PipelineConfiguration currentPipeline; // Store current pipeline for access to execution config
    private final ExpressionParser expressionParser;
    private final SchemaReaderService schemaReaderService;
    private final SchemaHtmlReportGenerator reportGenerator;

    public PipelineExecutor(ExternalDataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
        this.dataSinks = new ConcurrentHashMap<>();
        this.pipelineContext = new ConcurrentHashMap<>();
        this.stepResults = new ConcurrentHashMap<>();
        this.expressionParser = new SpelExpressionParser();
        this.schemaReaderService = new SchemaReaderService();
        this.reportGenerator = new SchemaHtmlReportGenerator();
    }

    /**
     * Add a data sink to the executor.
     */
    public void addDataSink(String name, DataSink dataSink) {
        dataSinks.put(name, dataSink);
    }
    
    /**
     * Execute a pipeline configuration.
     */
    public YamlPipelineExecutionResult execute(PipelineConfiguration pipeline) throws DataPipelineException {
        if (pipeline == null) {
            throw new DataPipelineException("Pipeline configuration is null");
        }

        LOGGER.info("Executing pipeline: {}", pipeline.getName());

        // Store pipeline for access to execution configuration
        this.currentPipeline = pipeline;

        long startTime = System.currentTimeMillis();
        YamlPipelineExecutionResult result = new YamlPipelineExecutionResult(pipeline.getName());

        try {
            // Validate pipeline configuration
            validatePipeline(pipeline);

            // Initialize data sinks
            initializeDataSinks(pipeline);

            // Execute pipeline steps
            if ("parallel".equalsIgnoreCase(pipeline.getExecution().getMode())) {
                executeStepsInParallel(pipeline.getSteps(), result);
            } else {
                executeStepsSequentially(pipeline.getSteps(), result);
            }

            result.setSuccess(true);
            result.setDurationMs(System.currentTimeMillis() - startTime);

            LOGGER.info("Pipeline '{}' completed successfully in {}ms",
                pipeline.getName(), result.getDurationMs());

        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
            result.setDurationMs(System.currentTimeMillis() - startTime);

            // Use TestAwareLogger to avoid stack trace dumps in test environments
            TestAwareLogger.error(LOGGER, "Pipeline '{}' failed after {}ms: {}",
                pipeline.getName(), result.getDurationMs(), e.getMessage());
            LOGGER.debug("Full exception details for pipeline '{}':", pipeline.getName(), e);

            // Don't throw exception - return result with step data even on failure
            // This allows RulesEngine to capture pipeline steps in execution path
            // if (!"continue-on-error".equals(pipeline.getExecution().getErrorHandling())) {
            //     throw new DataPipelineException("Pipeline execution failed: " + e.getMessage(), e);
            // }
        } finally {
            // Note: Data sinks are NOT shut down here - they are managed by the RulesEngine
            // and will be shut down when RulesEngine.shutdown() is called
            this.currentPipeline = null;
        }

        return result;
    }
    
    /**
     * Validate pipeline configuration.
     */
    private void validatePipeline(PipelineConfiguration pipeline) throws DataPipelineException {
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
    private void validateStep(PipelineStep step) throws DataPipelineException {
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
    private void validateStepDependencies(List<PipelineStep> steps) throws DataPipelineException {
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
    private void initializeDataSinks(PipelineConfiguration pipeline) throws DataPipelineException {
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
    private void executeStepsSequentially(List<PipelineStep> steps, YamlPipelineExecutionResult result)
            throws DataPipelineException {

        // Sort steps by dependencies
        List<PipelineStep> sortedSteps = topologicalSort(steps);

        for (PipelineStep step : sortedSteps) {
            executeStep(step, result);
        }
    }

    /**
     * Execute steps in parallel where possible.
     */
    private void executeStepsInParallel(List<PipelineStep> steps, YamlPipelineExecutionResult result)
            throws DataPipelineException {

        // For now, implement as sequential - parallel execution would require more complex dependency management
        executeStepsSequentially(steps, result);
    }
    
    /**
     * Execute a single pipeline step with retry support.
     */
    private void executeStep(PipelineStep step, YamlPipelineExecutionResult result) throws DataPipelineException {
        LOGGER.info("Executing step: {} ({})", step.getName(), step.getType());

        long stepStartTime = System.currentTimeMillis();
        PipelineStepResult stepResult = new PipelineStepResult(step.getName());

        // Get retry configuration from step or use defaults
        int maxRetries = getMaxRetries(step);
        long retryDelayMs = getRetryDelayMs(step);

        // Validate retry parameters
        if (maxRetries < 0) {
            LOGGER.warn("Invalid max-retries value: {}. Using 0 (no retries)", maxRetries);
            maxRetries = 0;
        }
        if (retryDelayMs < 0) {
            LOGGER.warn("Invalid retry-delay-ms value: {}. Using 0 (no delay)", retryDelayMs);
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
                        PipelineStepResult depResult = stepResults.get(dependency);
                        if (depResult == null || !depResult.isSuccess()) {
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
                    stepData = executeReadSchemaStep(step);
                    
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
                            LOGGER.warn("[Pipeline.Execute] Read-schema step returned unexpected type: {}", 
                                       stepData.getClass().getName());
                        }
                    } else {
                        LOGGER.warn("[Pipeline.Execute] Read-schema step returned null data");
                    }
                    // Set metrics for read-schema step
                    stepResult.setRecordsProcessed(1);
                }

                stepResult.setSuccess(true);
                stepResult.setData(stepData);
                stepResult.setDurationMs(System.currentTimeMillis() - stepStartTime);

                stepResults.put(step.getName(), stepResult);
                result.addStepResult(stepResult);

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
                    stepResult.setSuccess(false);
                    stepResult.setError(e.getMessage());
                    stepResult.setDurationMs(System.currentTimeMillis() - stepStartTime);

                    stepResults.put(step.getName(), stepResult);
                    result.addStepResult(stepResult);

                    LOGGER.error("Step '{}' failed after {}ms and {} retries: {}",
                        step.getName(), stepResult.getDurationMs(), maxRetries, e.getMessage());

                    if (!step.isOptional()) {
                        throw new DataPipelineException("Required step failed: " + step.getName(), e);
                    }
                } else {
                    LOGGER.warn("Step '{}' failed (attempt {}/{}): {}",
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
    private Object executeExtractStep(PipelineStep step) throws DataPipelineException {
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
     * Applies transformations to data from previous steps.
     */
    private Object executeTransformStep(PipelineStep step, Object data) throws DataPipelineException {
        if (data == null) {
            LOGGER.warn("No data available for transform step: {}", step.getName());
            return null;
        }

        List<Map<String, Object>> transformations = step.getTransformations();
        if (transformations == null || transformations.isEmpty()) {
            LOGGER.warn("No transformations configured for transform step: {}", step.getName());
            return data;
        }

        try {
            LOGGER.info("Applying {} transformations to data for step '{}'",
                transformations.size(), step.getName());

            // Process data - handle both single records and lists
            if (data instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> dataList = (List<Object>) data;
                List<Object> transformedList = new ArrayList<>();

                for (Object record : dataList) {
                    Object transformedRecord = applyTransformations(record, transformations, step.getName());
                    if (transformedRecord != null) {
                        transformedList.add(transformedRecord);
                    }
                }

                LOGGER.info("Transform step '{}' processed {} records",
                    step.getName(), transformedList.size());
                return transformedList;
            } else {
                // Single record
                Object transformedData = applyTransformations(data, transformations, step.getName());
                LOGGER.info("Transform step '{}' processed single record", step.getName());
                return transformedData;
            }
        } catch (Exception e) {
            throw new DataPipelineException("Transform step failed: " + step.getName(), e);
        }
    }

    /**
     * Apply transformations to a single record.
     */
    @SuppressWarnings("unchecked")
    private Object applyTransformations(Object record, List<Map<String, Object>> transformations, String stepName) {
        if (!(record instanceof Map)) {
            LOGGER.warn("Transform step '{}' can only transform Map records, skipping non-Map record", stepName);
            return record;
        }

        Map<String, Object> recordMap = new LinkedHashMap<>((Map<String, Object>) record);

        for (Map<String, Object> transformation : transformations) {
            try {
                applyTransformation(recordMap, transformation);
            } catch (Exception e) {
                String errorHandling = (String) transformation.get("error-handling");
                if ("skip-record".equals(errorHandling)) {
                    LOGGER.warn("Transformation '{}' failed, skipping record: {}",
                        transformation.get("name"), e.getMessage());
                    return null; // Skip this record
                } else {
                    LOGGER.warn("Transformation '{}' failed, continuing: {}",
                        transformation.get("name"), e.getMessage());
                    // Continue with other transformations
                }
            }
        }

        return recordMap;
    }

    /**
     * Apply a single transformation to a record.
     */
    private void applyTransformation(Map<String, Object> record, Map<String, Object> transformation) {
        String type = (String) transformation.get("type");
        String field = (String) transformation.get("field");

        if (type == null || field == null) {
            LOGGER.warn("Transformation missing required 'type' or 'field' property");
            return;
        }

        switch (type.toLowerCase()) {
            case "field-addition":
                applyFieldAddition(record, field, transformation);
                break;
            case "calculation":
                applyCalculation(record, field, transformation);
                break;
            case "validation":
                applyValidation(record, field, transformation);
                break;
            case "filter":
                // Filter is handled at the record level, not field level
                break;
            case "aggregation":
                // Aggregation requires multiple records, handled separately
                LOGGER.warn("Aggregation transformations not yet supported in pipeline transforms");
                break;
            default:
                LOGGER.warn("Unknown transformation type: {}", type);
        }
    }

    /**
     * Apply field addition transformation.
     */
    private void applyFieldAddition(Map<String, Object> record, String field, Map<String, Object> transformation) {
        Object value = transformation.get("value");
        if ("CURRENT_TIMESTAMP".equals(value)) {
            record.put(field, System.currentTimeMillis());
        } else {
            record.put(field, value);
        }
    }

    /**
     * Apply calculation transformation.
     */
    private void applyCalculation(Map<String, Object> record, String field, Map<String, Object> transformation) {
        String expression = (String) transformation.get("expression");
        if (expression == null) {
            LOGGER.warn("Calculation transformation missing 'expression' property");
            return;
        }

        try {
            // Create evaluation context with record fields as variables
            StandardEvaluationContext context = new StandardEvaluationContext();

            // Add all record fields as variables (both original case and lowercase)
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                // Add with original case
                context.setVariable(entry.getKey(), entry.getValue());
                // Also add with lowercase for case-insensitive access
                context.setVariable(entry.getKey().toLowerCase(), entry.getValue());
            }

            // Parse and evaluate the expression
            Expression exp = expressionParser.parseExpression(expression);
            Object result = exp.getValue(context);

            // Store the result in the record (use lowercase key to match database column names)
            record.put(field.toLowerCase(), result);

            LOGGER.debug("Calculated field '{}' = {} using expression: {}", field, result, expression);
        } catch (Exception e) {
            LOGGER.warn("Failed to evaluate calculation expression '{}' for field '{}': {}",
                expression, field, e.getMessage());
        }
    }

    /**
     * Apply validation transformation.
     */
    private void applyValidation(Map<String, Object> record, String field, Map<String, Object> transformation) {
        String rule = (String) transformation.get("rule");
        if (rule == null) {
            LOGGER.warn("Validation transformation missing 'rule' property");
            return;
        }

        Object value = record.get(field);

        // Simple validation rules
        switch (rule.toLowerCase()) {
            case "required":
                if (value == null || value.toString().trim().isEmpty()) {
                    throw new IllegalArgumentException("Required field '" + field + "' is missing or empty");
                }
                break;
            case "status-format":
                // Example validation - could be extended
                if (value != null && !value.toString().matches("[A-Z]+")) {
                    LOGGER.warn("Field '{}' does not match status format", field);
                }
                break;
            default:
                LOGGER.warn("Unknown validation rule: {}", rule);
        }
    }

    /**
     * Execute a load step.
     *
     * @return the number of records successfully processed
     */
    private int executeLoadStep(PipelineStep step, Object data) throws DataPipelineException {
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
                            LOGGER.warn("Skipping record due to data integrity violation: {} - Record: {}",
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
                    LOGGER.warn("All {} records were skipped due to data integrity violations in step '{}'",
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
                        LOGGER.warn("Skipped single record due to data integrity violation: {} - Record: {}",
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
    private void executeAuditStep(PipelineStep step, Object data) throws DataPipelineException {
        DataSink dataSink = dataSinks.get(step.getSink());
        if (dataSink == null) {
            throw new DataPipelineException("Data sink not found for audit step: " + step.getSink());
        }

        if (data == null) {
            LOGGER.warn("No data available for audit step: {}", step.getName());
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
     * Execute a read-schema step.
     * Reads schema metadata from a data source (database table or CSV file).
     * For databases, if 'table' parameter is missing, enumerates all tables.
     *
     * @param step the read-schema step configuration
     * @return SchemaMetadata for single table/file, or Map<String, SchemaMetadata> for table enumeration
     * @throws DataPipelineException if schema reading fails
     */
    private Object executeReadSchemaStep(PipelineStep step) throws DataPipelineException {
        LOGGER.info("Executing read-schema step: {}", step.getName());
        LOGGER.debug("[Pipeline.ReadSchema] Step details: type={}, source={}, description={}", 
                    step.getType(), step.getSource(), step.getDescription());

        // Get the data source
        String sourceName = step.getSource();
        if (sourceName == null || sourceName.trim().isEmpty()) {
            LOGGER.error("[Pipeline.ReadSchema] No source specified for step: {}", step.getName());
            throw new DataPipelineException("Read-schema step requires a source: " + step.getName());
        }
        LOGGER.debug("[Pipeline.ReadSchema] Retrieving data source: {}", sourceName);

        ExternalDataSource dataSource = dataSourceManager.getDataSource(sourceName);
        if (dataSource == null) {
            LOGGER.error("[Pipeline.ReadSchema] Data source not found: {}", sourceName);
            throw new DataPipelineException("Data source not found: " + sourceName);
        }
        LOGGER.debug("[Pipeline.ReadSchema] Data source retrieved: {} (type: {})", 
                    dataSource.getName(), dataSource.getSourceType());

        // Get parameters (e.g., table name for database, file path for CSV)
        Map<String, Object> parameters = step.getParameters();
        if (parameters == null) {
            LOGGER.debug("[Pipeline.ReadSchema] No parameters provided, using empty map");
            parameters = new HashMap<>();
        } else {
            LOGGER.debug("[Pipeline.ReadSchema] Parameters: {}", parameters);
        }

        try {
            LOGGER.debug("[Pipeline.ReadSchema] Invoking SchemaReaderService...");
            long startTime = System.currentTimeMillis();
            
            // Check if this is a database source and table parameter is missing (= enumerate all tables)
            boolean isDatabaseSource = dataSource.getSourceType() == DataSourceType.DATABASE;
            boolean hasTableParam = parameters.containsKey("table");
            
            if (isDatabaseSource && !hasTableParam) {
                LOGGER.info("[Pipeline.ReadSchema] Table parameter not specified - enumerating all tables");
                
                // Enumerate all tables
                Map<String, SchemaMetadata> tableSchemas = schemaReaderService.enumerateAllTables(dataSource, parameters);
                
                long duration = System.currentTimeMillis() - startTime;
                LOGGER.info("Successfully enumerated {} tables from source '{}' (took {} ms)",
                           tableSchemas.size(), sourceName, duration);
                
                // Log summary of enumerated tables
                LOGGER.debug("[Pipeline.ReadSchema] Enumerated tables:");
                for (Map.Entry<String, SchemaMetadata> entry : tableSchemas.entrySet()) {
                    LOGGER.debug("[Pipeline.ReadSchema]   - {} ({} columns)",
                                entry.getKey(), entry.getValue().getColumns().size());
                }
                
                // Generate HTML report if requested
                generateSchemaReportIfRequested(step, dataSource, tableSchemas);
                
                LOGGER.debug("[Pipeline.ReadSchema] Returning map of table schemas");
                return tableSchemas;
                
            } else {
                // Single table/file read
                LOGGER.debug("[Pipeline.ReadSchema] Reading schema for single table/file");
                
                SchemaMetadata schema = schemaReaderService.readSchema(dataSource, parameters);
                
                long duration = System.currentTimeMillis() - startTime;
                LOGGER.info("Successfully read schema from source '{}': {} columns (took {} ms)",
                           sourceName, schema.getColumns().size(), duration);
                
                // Log column details
                LOGGER.debug("[Pipeline.ReadSchema] Schema details:");
                for (int i = 0; i < schema.getColumns().size(); i++) {
                    SchemaMetadata.ColumnDefinition column = schema.getColumns().get(i);
                    LOGGER.debug("[Pipeline.ReadSchema]   [{}] {} ({}) - nullable: {}, pk: {}, maxLen: {}, precision: {}, scale: {}",
                                i, column.getName(), column.getDataType(), column.isNullable(), 
                                column.isPrimaryKey(), column.getMaxLength(), column.getPrecision(), column.getScale());
                }

                // Generate HTML report if requested
                generateSchemaReportIfRequested(step, dataSource, schema);

                LOGGER.debug("[Pipeline.ReadSchema] Returning schema metadata");
                return schema;
            }

        } catch (Exception e) {
            LOGGER.error("[Pipeline.ReadSchema] Read-schema step failed for '{}': {}", 
                        step.getName(), e.getMessage(), e);
            throw new DataPipelineException("Read-schema step failed: " + step.getName(), e);
        }
    }
    
    /**
     * Generate HTML report for schema if requested via parameters.
     *
     * @param step the pipeline step
     * @param dataSource the data source being read
     * @param stepData the step data (SchemaMetadata or Map of schemas)
     */
    private void generateSchemaReportIfRequested(PipelineStep step, ExternalDataSource dataSource, Object stepData) {
        if (stepData == null) {
            return;
        }
        
        Map<String, Object> parameters = step.getParameters();
        if (parameters == null) {
            return;
        }
        
        String reportPath = (String) parameters.get("report-output");
        if (reportPath == null || reportPath.trim().isEmpty()) {
            return; // No report requested
        }
        
        try {
            LOGGER.info("[Pipeline.ReadSchema] Generating HTML schema report: {}", reportPath);
            
            // Build DataSourceContext for the report
            DataSourceContext dsContext = buildDataSourceContext(dataSource, parameters);
            LOGGER.debug("[Pipeline.ReadSchema] DataSourceContext built: name={}, type={}, dbType={}, host={}, port={}, database={}, schema={}, schemaFilter={}",
                dsContext.getDataSourceName(), dsContext.getDataSourceType(), dsContext.getDatabaseType(),
                dsContext.getHost(), dsContext.getPort(), dsContext.getDatabaseName(),
                dsContext.getSchemaName(), dsContext.getSchemaFilter());
            
            if (stepData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, SchemaMetadata> tableSchemas = (Map<String, SchemaMetadata>) stepData;
                reportGenerator.generateReport(tableSchemas, dsContext, reportPath);
            } else if (stepData instanceof SchemaMetadata) {
                SchemaMetadata schema = (SchemaMetadata) stepData;
                reportGenerator.generateReport(schema, dsContext, reportPath);
            }
            
            LOGGER.info("[Pipeline.ReadSchema] HTML report generated successfully: {}", reportPath);
        } catch (Exception e) {
            LOGGER.warn("[Pipeline.ReadSchema] Failed to generate HTML report: {}", e.getMessage());
            // Don't fail the pipeline if report generation fails
        }
    }
    
    /**
     * Build DataSourceContext from data source and parameters.
     *
     * @param dataSource the data source
     * @param parameters the step parameters
     * @return DataSourceContext with connection details
     */
    private DataSourceContext buildDataSourceContext(ExternalDataSource dataSource, Map<String, Object> parameters) {
        DataSourceContext context = new DataSourceContext();
        
        // Common fields
        context.dataSourceName(dataSource.getName());
        
        // Handle database data sources
        if (dataSource instanceof DatabaseDataSource) {
            DatabaseDataSource dbDataSource = (DatabaseDataSource) dataSource;
            context.dataSourceType("database");
            
            // Get configuration details
            if (dbDataSource.getConfiguration() != null) {
                var config = dbDataSource.getConfiguration();
                
                // Database type from type/sourceType
                if (config.getSourceType() != null) {
                    context.databaseType(config.getSourceType());
                } else if (config.getType() != null) {
                    context.databaseType(config.getType());
                }
                
                // Connection details
                ConnectionConfig connConfig = config.getConnection();
                if (connConfig != null) {
                    context.host(connConfig.getHost());
                    context.port(connConfig.getPort());
                    context.databaseName(connConfig.getDatabase());
                    context.schemaName(connConfig.getSchema());
                    context.username(connConfig.getUsername());
                    
                    // Build JDBC URL if we have the components
                    if (connConfig.getHost() != null) {
                        String jdbcUrl = buildJdbcUrl(config.getSourceType(), connConfig);
                        context.jdbcUrl(jdbcUrl);
                    }
                }
            }
        } else if (dataSource.getSourceType() == DataSourceType.FILE_SYSTEM) {
            context.dataSourceType("file");
            
            // File source details
            if (parameters != null) {
                String filePath = (String) parameters.get("file");
                if (filePath != null) {
                    context.filePath(filePath);
                    
                    // Extract filename and directory
                    java.io.File file = new java.io.File(filePath);
                    context.fileName(file.getName());
                    if (file.getParentFile() != null) {
                        context.fileDirectory(file.getParentFile().getAbsolutePath());
                    }
                }
            }
        }
        
        // Add filter parameters if present
        if (parameters != null) {
            String schemaFilter = (String) parameters.get("schema");
            if (schemaFilter != null) {
                context.schemaFilter(schemaFilter);
            }
            
            String tablePattern = (String) parameters.get("table-pattern");
            if (tablePattern != null) {
                context.tablePattern(tablePattern);
            }
            
            @SuppressWarnings("unchecked")
            List<String> excludeTables = (List<String>) parameters.get("exclude-tables");
            if (excludeTables != null && !excludeTables.isEmpty()) {
                context.excludeTables(excludeTables);
            }
        }
        
        return context;
    }
    
    /**
     * Build a JDBC URL from configuration components.
     */
    private String buildJdbcUrl(String databaseType, ConnectionConfig connConfig) {
        if (databaseType == null || connConfig.getHost() == null) {
            return null;
        }
        
        String host = connConfig.getHost();
        Integer port = connConfig.getPort();
        String database = connConfig.getDatabase();
        
        switch (databaseType.toLowerCase()) {
            case "postgresql":
                return String.format("jdbc:postgresql://%s:%d/%s", 
                    host, port != null ? port : 5432, database != null ? database : "");
            case "mysql":
                return String.format("jdbc:mysql://%s:%d/%s", 
                    host, port != null ? port : 3306, database != null ? database : "");
            case "h2":
                return String.format("jdbc:h2:mem:%s", database != null ? database : "testdb");
            case "oracle":
                return String.format("jdbc:oracle:thin:@%s:%d/%s", 
                    host, port != null ? port : 1521, database != null ? database : "");
            case "sqlserver":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", 
                    host, port != null ? port : 1433, database != null ? database : "");
            default:
                return String.format("jdbc:%s://%s:%s/%s", 
                    databaseType, host, port != null ? port.toString() : "?", database != null ? database : "");
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
            }
        }
        dataSinks.clear();
    }
}
