/*
 * Copyright (c) 2025 Devspace Mars Solutions.
 * All rights reserved.
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
package dev.mars.apex.engine.pipeline;

import dev.mars.apex.core.config.pipeline.PipelineStep;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.database.DatabaseDataSource;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.service.data.external.manager.ExternalDataSourceManager;
import dev.mars.apex.core.service.schema.DataSourceContext;
import dev.mars.apex.core.service.schema.SchemaHtmlReportGenerator;
import dev.mars.apex.core.service.schema.SchemaMetadata;
import dev.mars.apex.core.service.schema.SchemaReaderService;
import dev.mars.apex.core.service.schema.diff.ComparisonOptions;
import dev.mars.apex.core.service.schema.diff.SchemaComparisonResult;
import dev.mars.apex.core.service.schema.diff.SchemaDiffService;
import dev.mars.apex.core.service.schema.diff.json.SchemaDiffJsonSerializer;
import dev.mars.apex.core.service.schema.diff.json.SchemaDiffReportBuilder;
import dev.mars.apex.core.service.schema.diff.json.generators.JsonBasedHtmlReportGenerator;
import dev.mars.apex.core.service.schema.diff.json.model.SchemaDiffReport;
import dev.mars.apex.engine.model.ExecutionStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles schema-related step execution for APEX data pipelines.
 * 
 * <p>Extracted from PipelineExecutor as part of god class decomposition.
 * Supports read-schema and schema-diff steps with HTML/JSON report generation.
 * 
 * @author APEX Engine
 * @since 2.4
 */
public class SchemaStepExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaStepExecutor.class);

    private final ExternalDataSourceManager dataSourceManager;
    private final SchemaReaderService schemaReaderService;
    private final SchemaHtmlReportGenerator reportGenerator;
    private final SchemaDiffService schemaDiffService;
    private final Map<String, Object> pipelineContext;
    private final Map<String, ExecutionStep> stepResults;

    /**
     * Constructs a SchemaStepExecutor with required dependencies.
     *
     * @param dataSourceManager the external data source manager
     * @param schemaReaderService the schema reader service
     * @param reportGenerator the HTML report generator
     * @param schemaDiffService the schema diff service
     * @param pipelineContext shared pipeline context for storing/retrieving step data
     * @param stepResults map of step names to their execution results
     */
    public SchemaStepExecutor(
            ExternalDataSourceManager dataSourceManager,
            SchemaReaderService schemaReaderService,
            SchemaHtmlReportGenerator reportGenerator,
            SchemaDiffService schemaDiffService,
            Map<String, Object> pipelineContext,
            Map<String, ExecutionStep> stepResults) {
        this.dataSourceManager = dataSourceManager;
        this.schemaReaderService = schemaReaderService;
        this.reportGenerator = reportGenerator;
        this.schemaDiffService = schemaDiffService;
        this.pipelineContext = pipelineContext;
        this.stepResults = stepResults;
    }

    /**
     * Execute a read-schema step.
     * Reads schema metadata from a data source (database table or CSV file).
     * For databases, if 'table' parameter is missing, enumerates all tables.
     *
     * @param step the read-schema step configuration
     * @return SchemaMetadata for single table/file, or Map&lt;String, SchemaMetadata&gt; for table enumeration
     * @throws DataPipelineException if the operation fails (unchecked)
     */
    public Object executeReadSchemaStep(PipelineStep step) {
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

                // Store DataSourceContext for later use (e.g., in schema-diff reports)
                DataSourceContext dsContext = buildDataSourceContext(dataSource, parameters);
                String contextKey = step.getName() + "_dataSourceContext";
                pipelineContext.put(contextKey, dsContext);
                LOGGER.debug("[Pipeline.ReadSchema] Stored DataSourceContext with key: {}", contextKey);

                // Generate HTML report if requested
                generateSchemaReportIfRequested(step, dataSource, schema);

                LOGGER.debug("[Pipeline.ReadSchema] Returning schema metadata");
                return schema;
            }

        } catch (Exception e) {
            LOGGER.error("[Pipeline.ReadSchema] Read-schema step failed for '{}': {}", 
                        step.getName(), e.getMessage());
            LOGGER.debug("Full exception details:", e);
            throw new DataPipelineException("Read-schema step failed: " + step.getName(), e);
        }
    }

    /**
     * Execute a schema-diff step to compare two schemas.
     *
     * @param step the schema-diff pipeline step
     * @return SchemaComparisonResult containing diff details
     * @throws DataPipelineException if the operation fails (unchecked)
     */
    public SchemaComparisonResult executeSchemaDiffStep(PipelineStep step) {
        LOGGER.info("Executing schema-diff step: {}", step.getName());

        Map<String, Object> parameters = step.getParameters();
        if (parameters == null) {
            throw new DataPipelineException("Schema-diff step requires parameters: " + step.getName());
        }

        // Get source and target step names from parameters
        String sourceStepName = (String) parameters.get("source-step");
        String targetStepName = (String) parameters.get("target-step");

        if (sourceStepName == null || targetStepName == null) {
            throw new DataPipelineException(
                "Schema-diff step requires both 'source-step' and 'target-step' parameters: " + step.getName());
        }

        LOGGER.debug("[Pipeline.SchemaDiff] Retrieving schemas from steps: source={}, target={}",
                    sourceStepName, targetStepName);

        // Retrieve SchemaMetadata from previous steps
        SchemaMetadata sourceSchema = retrieveSchemaFromStep(sourceStepName);
        SchemaMetadata targetSchema = retrieveSchemaFromStep(targetStepName);

        if (sourceSchema == null) {
            throw new DataPipelineException(
                "Source schema not found from step: " + sourceStepName);
        }

        if (targetSchema == null) {
            throw new DataPipelineException(
                "Target schema not found from step: " + targetStepName);
        }

        // Build comparison options from parameters
        ComparisonOptions options = buildComparisonOptions(parameters);

        // Perform comparison
        LOGGER.debug("[Pipeline.SchemaDiff] Comparing schemas: {} ({} columns) vs {} ({} columns)",
                    sourceSchema.getSourceName(), sourceSchema.getColumns().size(),
                    targetSchema.getSourceName(), targetSchema.getColumns().size());

        SchemaComparisonResult result = schemaDiffService.compareSchemas(sourceSchema, targetSchema, options);

        LOGGER.info("Schema comparison complete: {} matching, {} added, {} removed, {} changed, {} breaking changes",
                   result.getMatchingColumns().size(),
                   result.getAddedColumns().size(),
                   result.getRemovedColumns().size(),
                   result.getChangedColumns().size(),
                   result.getBreakingChanges().size());

        // Generate reports
        generateSchemaDiffReports(step, parameters, result, sourceStepName, targetStepName);

        // Check fail-on-incompatibility flag
        Boolean failOnIncompatibility = (Boolean) parameters.get("fail-on-incompatibility");
        if (Boolean.TRUE.equals(failOnIncompatibility) && !result.isCompatible()) {
            throw new DataPipelineException(
                "Schema incompatibility detected with " + result.getBreakingChanges().size() + 
                " breaking changes: " + step.getName());
        }

        return result;
    }

    /**
     * Generate schema diff reports (JSON and HTML) if requested.
     */
    private void generateSchemaDiffReports(PipelineStep step, Map<String, Object> parameters, 
            SchemaComparisonResult result, String sourceStepName, String targetStepName) {
        
        String htmlReportPath = (String) parameters.get("report-output");
        String jsonReportPath = (String) parameters.get("json-report-output");
        
        LOGGER.debug("[Pipeline.SchemaDiff] report-output (HTML): {}", htmlReportPath);
        LOGGER.debug("[Pipeline.SchemaDiff] json-report-output (JSON): {}", jsonReportPath);

        // Only generate reports if at least one output is requested
        if ((htmlReportPath != null && !htmlReportPath.trim().isEmpty()) ||
            (jsonReportPath != null && !jsonReportPath.trim().isEmpty())) {
            
            try {
                // Retrieve DataSourceContext from source and target steps
                DataSourceContext sourceContext = (DataSourceContext) pipelineContext.get(sourceStepName + "_dataSourceContext");
                DataSourceContext targetContext = (DataSourceContext) pipelineContext.get(targetStepName + "_dataSourceContext");
                
                LOGGER.debug("[Pipeline.SchemaDiff] Retrieved DataSourceContext: source={}, target={}", 
                    sourceContext != null ? sourceContext.getDataSourceName() : "null",
                    targetContext != null ? targetContext.getDataSourceName() : "null");
                
                // STEP 1: Build JSON report model (single source of truth)
                LOGGER.info("[Pipeline.SchemaDiff] Building JSON report model (single source of truth)");
                SchemaDiffReportBuilder builder = new SchemaDiffReportBuilder();
                SchemaDiffReport jsonReport = builder.buildReport(result, sourceContext, targetContext);
                
                // Store JSON report in context for potential downstream use
                pipelineContext.put("schema-diff-json-model", jsonReport);
                
                // STEP 2: Generate JSON file if requested
                if (jsonReportPath != null && !jsonReportPath.trim().isEmpty()) {
                    LOGGER.info("[Pipeline.SchemaDiff] Generating JSON report to: {}", jsonReportPath);
                    SchemaDiffJsonSerializer serializer = new SchemaDiffJsonSerializer();
                    String generatedJsonPath = serializer.toJsonFile(jsonReport, jsonReportPath);
                    LOGGER.info("[Pipeline.SchemaDiff] JSON report generated: {}", generatedJsonPath);
                    pipelineContext.put("schema-diff-json-report", generatedJsonPath);
                }
                
                // STEP 3: Generate HTML from JSON model (ensures consistency)
                if (htmlReportPath != null && !htmlReportPath.trim().isEmpty()) {
                    LOGGER.info("[Pipeline.SchemaDiff] Generating HTML report from JSON model to: {}", htmlReportPath);
                    JsonBasedHtmlReportGenerator htmlGenerator = new JsonBasedHtmlReportGenerator();
                    String generatedHtmlPath = htmlGenerator.generateFromReport(jsonReport, htmlReportPath);
                    LOGGER.info("[Pipeline.SchemaDiff] HTML report generated (from JSON model): {}", generatedHtmlPath);
                    pipelineContext.put("schema-diff-report", generatedHtmlPath);
                }
                
            } catch (Exception e) {
                LOGGER.warn("[Pipeline.SchemaDiff] Failed to generate reports: {}", e.getMessage());
                LOGGER.debug("Full exception details:", e);
                // Don't fail the pipeline for report generation errors
            }
        } else {
            LOGGER.debug("[Pipeline.SchemaDiff] No report output parameters specified, skipping report generation");
        }
    }

    /**
     * Generate report for schema if requested via parameters.
     * Supports HTML format (.html extension).
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
        
        // Check file extension to determine report format
        String lowerPath = reportPath.toLowerCase();
        if (lowerPath.endsWith(".json")) {
            LOGGER.error("[Pipeline.ReadSchema] JSON format not yet supported for read-schema reports. " +
                       "Please use .html extension. Skipping report generation for: {}", reportPath);
            return;
        }
        if (lowerPath.endsWith(".md") || lowerPath.endsWith(".markdown")) {
            LOGGER.error("[Pipeline.ReadSchema] Markdown format not yet supported for read-schema reports. " +
                       "Please use .html extension. Skipping report generation for: {}", reportPath);
            return;
        }
        
        // Normalize and ensure report directory exists
        reportPath = normalizeReportPath(reportPath);
        
        try {
            LOGGER.info("[Pipeline.ReadSchema] Generating HTML schema report: {}", reportPath);
            
            // Build DataSourceContext for the report
            DataSourceContext dsContext = buildDataSourceContext(dataSource, parameters);
            
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
            LOGGER.debug("Full exception details:", e);
        }
    }

    /**
     * Normalize report path to ensure it uses the correct directory structure.
     */
    private String normalizeReportPath(String reportPath) {
        if (reportPath == null || reportPath.trim().isEmpty()) {
            return reportPath;
        }
        
        reportPath = reportPath.trim();
        
        // Check if path already contains a directory separator
        if (!reportPath.contains("/") && !reportPath.contains("\\")) {
            reportPath = "reports/" + reportPath;
            LOGGER.debug("[Pipeline.ReadSchema] Report path normalized to default directory: {}", reportPath);
        }
        
        // Ensure parent directory exists
        try {
            Path path = Paths.get(reportPath);
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                LOGGER.debug("[Pipeline.ReadSchema] Created report directory: {}", parentDir);
            }
        } catch (Exception e) {
            LOGGER.warn("[Pipeline.ReadSchema] Could not create report directory: {}", e.getMessage());
        }
        
        return reportPath;
    }

    /**
     * Build DataSourceContext from data source and parameters.
     */
    private DataSourceContext buildDataSourceContext(ExternalDataSource dataSource, Map<String, Object> parameters) {
        DataSourceContext context = new DataSourceContext();
        context.dataSourceName(dataSource.getName());
        
        if (dataSource instanceof DatabaseDataSource) {
            DatabaseDataSource dbDataSource = (DatabaseDataSource) dataSource;
            context.dataSourceType("database");
            
            if (dbDataSource.getConfiguration() != null) {
                var config = dbDataSource.getConfiguration();
                
                if (config.getSourceType() != null) {
                    context.databaseType(config.getSourceType());
                } else if (config.getType() != null) {
                    context.databaseType(config.getType());
                }
                
                ConnectionConfig connConfig = config.getConnection();
                if (connConfig != null) {
                    context.host(connConfig.getHost());
                    context.port(connConfig.getPort());
                    context.databaseName(connConfig.getDatabase());
                    context.schemaName(connConfig.getSchema());
                    context.username(connConfig.getUsername());
                    
                    if (connConfig.getHost() != null) {
                        String jdbcUrl = buildJdbcUrl(config.getSourceType(), connConfig);
                        context.jdbcUrl(jdbcUrl);
                    }
                }
            }
        } else if (dataSource.getSourceType() == DataSourceType.FILE_SYSTEM) {
            context.dataSourceType("file");
            
            if (parameters != null) {
                String filePath = (String) parameters.get("file");
                if (filePath != null) {
                    context.filePath(filePath);
                    
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
     * Retrieve SchemaMetadata from a previous pipeline step's result.
     */
    private SchemaMetadata retrieveSchemaFromStep(String stepName) {
        ExecutionStep stepResult = stepResults.get(stepName);
        if (stepResult == null) {
            throw new DataPipelineException("Step not found: " + stepName);
        }

        Object stepData = stepResult.getStepData();
        if (stepData instanceof SchemaMetadata) {
            return (SchemaMetadata) stepData;
        } else if (stepData != null) {
            throw new DataPipelineException(
                "Step '" + stepName + "' did not produce SchemaMetadata (found: " + 
                stepData.getClass().getSimpleName() + ")");
        } else {
            throw new DataPipelineException("Step '" + stepName + "' produced null data");
        }
    }

    /**
     * Build ComparisonOptions from step parameters.
     */
    private ComparisonOptions buildComparisonOptions(Map<String, Object> parameters) {
        ComparisonOptions options = ComparisonOptions.defaults();

        if (parameters.containsKey("case-insensitive-names")) {
            options.setCaseInsensitiveNames((Boolean) parameters.get("case-insensitive-names"));
        }

        if (parameters.containsKey("inferred-type-tolerance")) {
            options.setInferredTypeTolerance((Boolean) parameters.get("inferred-type-tolerance"));
        }

        if (parameters.containsKey("allow-added-columns")) {
            options.setAllowAddedColumns((Boolean) parameters.get("allow-added-columns"));
        }

        if (parameters.containsKey("allow-removed-columns")) {
            options.setAllowRemovedColumns((Boolean) parameters.get("allow-removed-columns"));
        }

        @SuppressWarnings("unchecked")
        Map<String, String> typeMappings = (Map<String, String>) parameters.get("type-mappings");
        if (typeMappings != null) {
            options.setTypeMappings(typeMappings);
        }

        return options;
    }
}
