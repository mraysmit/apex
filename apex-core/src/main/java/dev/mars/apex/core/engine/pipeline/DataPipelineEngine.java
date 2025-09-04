package dev.mars.apex.core.engine.pipeline;

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

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasink.DataSinkConfiguration;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.DataSink;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.data.external.factory.DataSinkFactory;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.DataSinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Pipeline Engine for APEX.
 * 
 * This engine orchestrates data flow from sources through processing to sinks
 * based on YAML configuration. It follows the established APEX pattern of
 * YAML-driven processing with factory-based component creation.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DataPipelineEngine {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DataPipelineEngine.class);
    
    private final DataSourceFactory dataSourceFactory;
    private final DataSinkFactory dataSinkFactory;
    
    private final Map<String, ExternalDataSource> dataSources = new HashMap<>();
    private final Map<String, DataSink> dataSinks = new HashMap<>();
    
    /**
     * Constructor.
     */
    public DataPipelineEngine() {
        this.dataSourceFactory = DataSourceFactory.getInstance();
        this.dataSinkFactory = DataSinkFactory.getInstance();
    }
    
    /**
     * Initialize the pipeline engine from YAML configuration.
     * 
     * @param yamlConfig The YAML configuration containing data sources and sinks
     * @throws DataPipelineException if initialization fails
     */
    public void initialize(YamlRuleConfiguration yamlConfig) throws DataPipelineException {
        LOGGER.info("Initializing Data Pipeline Engine from YAML configuration");
        
        try {
            // Initialize data sources
            initializeDataSources(yamlConfig);
            
            // Initialize data sinks
            initializeDataSinks(yamlConfig);
            
            LOGGER.info("Data Pipeline Engine initialized successfully with {} sources and {} sinks", 
                       dataSources.size(), dataSinks.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Data Pipeline Engine", e);
            shutdown(); // Cleanup on failure
            throw new DataPipelineException("Pipeline initialization failed", e);
        }
    }
    
    /**
     * Execute a simple pipeline: read from source, write to sink.
     * 
     * @param sourceQuery The query to execute on the data source
     * @param sourceName The name of the data source
     * @param sinkName The name of the data sink
     * @param sinkOperation The operation to perform on the data sink
     * @return Pipeline execution result
     * @throws DataPipelineException if execution fails
     */
    public PipelineExecutionResult execute(String sourceQuery, String sourceName, 
                                         String sinkName, String sinkOperation) throws DataPipelineException {
        
        LOGGER.info("Executing pipeline: {} -> {} (operation: {})", sourceName, sinkName, sinkOperation);
        
        long startTime = System.currentTimeMillis();
        PipelineExecutionResult.Builder resultBuilder = PipelineExecutionResult.builder()
            .pipelineId(generatePipelineId(sourceName, sinkName))
            .startTime(startTime);
        
        try {
            // Get data source and sink
            ExternalDataSource dataSource = getDataSource(sourceName);
            DataSink dataSink = getDataSink(sinkName);
            
            // Read data from source
            LOGGER.debug("Reading data from source: {}", sourceName);
            LOGGER.debug("Executing query: {}", sourceQuery);
            List<Object> sourceData = dataSource.query(sourceQuery, new HashMap<>());
            LOGGER.info("Read {} records from source: {}", sourceData.size(), sourceName);

            // Debug: Log first few records if any
            if (!sourceData.isEmpty()) {
                LOGGER.debug("Sample record from source: {}", sourceData.get(0));
            } else {
                LOGGER.warn("No data returned from source query - this indicates a problem with data loading or query execution");
            }
            
            // Write data to sink
            LOGGER.debug("Writing data to sink: {}", sinkName);
            int processedRecords = 0;
            int failedRecords = 0;
            
            for (Object record : sourceData) {
                try {
                    dataSink.write(sinkOperation, record);
                    processedRecords++;
                } catch (DataSinkException e) {
                    LOGGER.warn("Failed to write record to sink: {}", e.getMessage());
                    failedRecords++;
                }
            }
            
            // Flush any pending operations
            dataSink.flush();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            LOGGER.info("Pipeline execution completed: {} processed, {} failed in {}ms", 
                       processedRecords, failedRecords, executionTime);
            
            return resultBuilder
                .successful(true)
                .recordsProcessed(processedRecords)
                .recordsFailed(failedRecords)
                .executionTimeMs(executionTime)
                .build();
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            LOGGER.error("Pipeline execution failed", e);
            
            return resultBuilder
                .successful(false)
                .errorMessage(e.getMessage())
                .executionTimeMs(executionTime)
                .build();
        }
    }
    
    /**
     * Execute a batch pipeline with configurable batch size.
     * 
     * @param sourceQuery The query to execute on the data source
     * @param sourceName The name of the data source
     * @param sinkName The name of the data sink
     * @param sinkOperation The operation to perform on the data sink
     * @param batchSize The batch size for processing
     * @return Pipeline execution result
     * @throws DataPipelineException if execution fails
     */
    public PipelineExecutionResult executeBatch(String sourceQuery, String sourceName, 
                                              String sinkName, String sinkOperation, 
                                              int batchSize) throws DataPipelineException {
        
        LOGGER.info("Executing batch pipeline: {} -> {} (batch size: {})", sourceName, sinkName, batchSize);
        
        long startTime = System.currentTimeMillis();
        PipelineExecutionResult.Builder resultBuilder = PipelineExecutionResult.builder()
            .pipelineId(generatePipelineId(sourceName, sinkName))
            .startTime(startTime);
        
        try {
            // Get data source and sink
            ExternalDataSource dataSource = getDataSource(sourceName);
            DataSink dataSink = getDataSink(sinkName);
            
            // Read data from source
            LOGGER.debug("Reading data from source: {}", sourceName);
            List<Object> sourceData = dataSource.query(sourceQuery, new HashMap<>());
            LOGGER.info("Read {} records from source: {}", sourceData.size(), sourceName);
            
            // Process in batches
            int totalProcessed = 0;
            int totalFailed = 0;
            int batchCount = 0;
            
            for (int i = 0; i < sourceData.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, sourceData.size());
                List<Object> batch = sourceData.subList(i, endIndex);
                batchCount++;
                
                try {
                    LOGGER.debug("Processing batch {} with {} records", batchCount, batch.size());
                    dataSink.writeBatch(sinkOperation, batch);
                    totalProcessed += batch.size();
                    LOGGER.debug("Batch {} completed successfully", batchCount);
                } catch (DataSinkException e) {
                    LOGGER.warn("Batch {} failed: {}", batchCount, e.getMessage());
                    totalFailed += batch.size();
                }
            }
            
            // Flush any pending operations
            dataSink.flush();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            LOGGER.info("Batch pipeline execution completed: {} batches, {} processed, {} failed in {}ms", 
                       batchCount, totalProcessed, totalFailed, executionTime);
            
            return resultBuilder
                .successful(true)
                .recordsProcessed(totalProcessed)
                .recordsFailed(totalFailed)
                .batchesProcessed(batchCount)
                .executionTimeMs(executionTime)
                .build();
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            LOGGER.error("Batch pipeline execution failed", e);
            
            return resultBuilder
                .successful(false)
                .errorMessage(e.getMessage())
                .executionTimeMs(executionTime)
                .build();
        }
    }
    
    /**
     * Get a data source by name.
     */
    public ExternalDataSource getDataSource(String name) throws DataPipelineException {
        ExternalDataSource dataSource = dataSources.get(name);
        if (dataSource == null) {
            throw new DataPipelineException("Data source not found: " + name);
        }
        return dataSource;
    }
    
    /**
     * Get a data sink by name.
     */
    public DataSink getDataSink(String name) throws DataPipelineException {
        DataSink dataSink = dataSinks.get(name);
        if (dataSink == null) {
            throw new DataPipelineException("Data sink not found: " + name);
        }
        return dataSink;
    }
    
    /**
     * Shutdown the pipeline engine and cleanup resources.
     */
    public void shutdown() {
        LOGGER.info("Shutting down Data Pipeline Engine");
        
        // Shutdown data sources
        for (Map.Entry<String, ExternalDataSource> entry : dataSources.entrySet()) {
            try {
                entry.getValue().shutdown();
                LOGGER.debug("Shutdown data source: {}", entry.getKey());
            } catch (Exception e) {
                LOGGER.warn("Error shutting down data source {}: {}", entry.getKey(), e.getMessage());
            }
        }
        
        // Shutdown data sinks
        for (Map.Entry<String, DataSink> entry : dataSinks.entrySet()) {
            try {
                entry.getValue().shutdown();
                LOGGER.debug("Shutdown data sink: {}", entry.getKey());
            } catch (Exception e) {
                LOGGER.warn("Error shutting down data sink {}: {}", entry.getKey(), e.getMessage());
            }
        }
        
        dataSources.clear();
        dataSinks.clear();
        
        LOGGER.info("Data Pipeline Engine shutdown complete");
    }
    
    // Private helper methods
    
    private void initializeDataSources(YamlRuleConfiguration yamlConfig) throws DataSourceException {
        if (yamlConfig.getDataSources() != null) {
            LOGGER.info("Initializing {} data sources", yamlConfig.getDataSources().size());
            
            for (var yamlDataSource : yamlConfig.getDataSources()) {
                DataSourceConfiguration config = yamlDataSource.toDataSourceConfiguration();
                ExternalDataSource dataSource = dataSourceFactory.createDataSource(config);
                dataSources.put(config.getName(), dataSource);
                LOGGER.debug("Initialized data source: {}", config.getName());
            }
        }
    }
    
    private void initializeDataSinks(YamlRuleConfiguration yamlConfig) throws DataSinkException {
        if (yamlConfig.getDataSinks() != null) {
            LOGGER.info("Initializing {} data sinks", yamlConfig.getDataSinks().size());
            
            for (var yamlDataSink : yamlConfig.getDataSinks()) {
                DataSinkConfiguration config = yamlDataSink.toDataSinkConfiguration();
                DataSink dataSink = dataSinkFactory.createDataSink(config);
                dataSinks.put(config.getName(), dataSink);
                LOGGER.debug("Initialized data sink: {}", config.getName());
            }
        }
    }
    
    private String generatePipelineId(String sourceName, String sinkName) {
        return String.format("pipeline_%s_to_%s_%d", sourceName, sinkName, System.currentTimeMillis());
    }
}
