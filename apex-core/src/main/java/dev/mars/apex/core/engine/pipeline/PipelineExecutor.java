package dev.mars.apex.core.engine.pipeline;

import dev.mars.apex.core.config.pipeline.PipelineConfiguration;
import dev.mars.apex.core.config.pipeline.PipelineStep;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.DataSink;
import dev.mars.apex.core.service.data.external.DataSinkException;
import dev.mars.apex.core.service.data.external.manager.ExternalDataSourceManager;
import dev.mars.apex.core.service.data.external.factory.DataSinkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    
    public PipelineExecutor(ExternalDataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
        this.dataSinks = new ConcurrentHashMap<>();
        this.pipelineContext = new ConcurrentHashMap<>();
        this.stepResults = new ConcurrentHashMap<>();
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
            
            LOGGER.error("Pipeline '{}' failed after {}ms: {}", 
                pipeline.getName(), result.getDurationMs(), e.getMessage(), e);
            
            if (!"continue-on-error".equals(pipeline.getExecution().getErrorHandling())) {
                throw new DataPipelineException("Pipeline execution failed: " + e.getMessage(), e);
            }
        } finally {
            // Cleanup resources
            shutdownDataSinks();
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
     * Execute a single pipeline step.
     */
    private void executeStep(PipelineStep step, YamlPipelineExecutionResult result) throws DataPipelineException {
        LOGGER.info("Executing step: {} ({})", step.getName(), step.getType());
        
        long stepStartTime = System.currentTimeMillis();
        PipelineStepResult stepResult = new PipelineStepResult(step.getName());
        
        try {
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
                // Store extracted data for subsequent steps
                pipelineContext.put("extractedData", stepData);
            } else if (step.isLoadStep()) {
                // Get data from previous extract step
                Object dataToLoad = pipelineContext.get("extractedData");
                executeLoadStep(step, dataToLoad);
            } else if (step.isAuditStep()) {
                // Get data from previous steps for auditing
                Object dataToAudit = pipelineContext.get("extractedData");
                executeAuditStep(step, dataToAudit);
            }
            
            stepResult.setSuccess(true);
            stepResult.setData(stepData);
            stepResult.setDurationMs(System.currentTimeMillis() - stepStartTime);
            
            stepResults.put(step.getName(), stepResult);
            result.addStepResult(stepResult);
            
            LOGGER.info("Step '{}' completed successfully in {}ms", 
                step.getName(), stepResult.getDurationMs());
            
        } catch (Exception e) {
            stepResult.setSuccess(false);
            stepResult.setError(e.getMessage());
            stepResult.setDurationMs(System.currentTimeMillis() - stepStartTime);
            
            stepResults.put(step.getName(), stepResult);
            result.addStepResult(stepResult);
            
            LOGGER.error("Step '{}' failed after {}ms: {}", 
                step.getName(), stepResult.getDurationMs(), e.getMessage());
            
            if (!step.isOptional()) {
                throw new DataPipelineException("Required step failed: " + step.getName(), e);
            }
        }
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
     * Execute a load step.
     */
    private void executeLoadStep(PipelineStep step, Object data) throws DataPipelineException {
        LOGGER.info("Looking for data sink: '{}' in available sinks: {}", step.getSink(), dataSinks.keySet());
        DataSink dataSink = dataSinks.get(step.getSink());
        if (dataSink == null) {
            throw new DataPipelineException("Data sink not found: " + step.getSink());
        }
        LOGGER.info("Found data sink: {} (type: {})", dataSink.getName(), dataSink.getClass().getSimpleName());

        if (data == null) {
            throw new DataPipelineException("No data available for load step: " + step.getName());
        }

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
            } else {
                // Single record
                try {
                    dataSink.write(step.getOperation(), data);
                    LOGGER.info("Successfully loaded single record to sink '{}'", step.getSink());
                } catch (DataSinkException e) {
                    if (e.getErrorType() == DataSinkException.ErrorType.DATA_INTEGRITY_ERROR) {
                        // Log and continue for data integrity violations
                        LOGGER.warn("Skipped single record due to data integrity violation: {} - Record: {}",
                                   e.getMessage(), data);
                    } else {
                        // Re-throw other types of errors
                        throw e;
                    }
                }
            }
        } catch (Exception e) {
            throw new DataPipelineException("Load step failed: " + step.getName(), e);
        }
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
