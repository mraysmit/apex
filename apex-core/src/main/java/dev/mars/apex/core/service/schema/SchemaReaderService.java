package dev.mars.apex.core.service.schema;

import dev.mars.apex.core.engine.pipeline.DataPipelineException;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.database.DatabaseDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Service for reading schema metadata from various data sources.
 * Supports databases and CSV files.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-11
 */
public class SchemaReaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaReaderService.class);

    /**
     * Read schema from a data source.
     *
     * @param dataSource the data source to read schema from
     * @param parameters optional parameters (e.g., table name, CSV file path)
     * @return schema metadata (single table) or null if enumerating all tables
     * @throws DataPipelineException if schema reading fails
     */
    public SchemaMetadata readSchema(ExternalDataSource dataSource, Map<String, Object> parameters) 
            throws DataPipelineException {
        
        LOGGER.debug("[SchemaReader] readSchema() called with dataSource={}, parameters={}", 
                     dataSource != null ? dataSource.getName() : "null", parameters);
        
        if (dataSource == null) {
            LOGGER.error("[SchemaReader] Data source is null");
            throw new DataPipelineException("Data source is null");
        }

        LOGGER.info("Reading schema from data source: {} (type: {})", 
                    dataSource.getName(), dataSource.getSourceType());

        DataSourceType sourceType = dataSource.getSourceType();
        LOGGER.debug("[SchemaReader] Determined source type: {}", sourceType);
        
        if (sourceType == DataSourceType.DATABASE) {
            LOGGER.debug("[SchemaReader] Routing to database schema reader");
            return readDatabaseSchema(dataSource, parameters);
        } else if (sourceType == DataSourceType.FILE_SYSTEM) {
            LOGGER.debug("[SchemaReader] Routing to file system schema reader (CSV)");
            // For now, assume CSV files - could be extended for JSON, XML, etc.
            return readCsvSchema(dataSource, parameters);
        } else {
            LOGGER.error("[SchemaReader] Unsupported source type: {}", sourceType);
            throw new DataPipelineException("Unsupported source type for schema reading: " + sourceType);
        }
    }

    /**
     * Enumerate all tables in a database.
     *
     * @param dataSource the database data source
     * @param parameters optional parameters (schema filter, table pattern, exclusions)
     * @return map of table name to schema metadata
     * @throws DataPipelineException if enumeration fails
     */
    public Map<String, SchemaMetadata> enumerateAllTables(ExternalDataSource dataSource, Map<String, Object> parameters) 
            throws DataPipelineException {
        
        LOGGER.debug("[SchemaReader] enumerateAllTables() called with dataSource={}, parameters={}", 
                     dataSource != null ? dataSource.getName() : "null", parameters);
        
        if (!(dataSource instanceof DatabaseDataSource)) {
            LOGGER.error("[SchemaReader] Invalid data source type: {}", dataSource.getClass().getSimpleName());
            throw new DataPipelineException("Data source is not a DatabaseDataSource: " + 
                                           dataSource.getClass().getSimpleName());
        }

        DatabaseDataSource dbDataSource = (DatabaseDataSource) dataSource;
        Map<String, SchemaMetadata> tableSchemas = new LinkedHashMap<>();
        
        try {
            // Get filtering parameters
            String schemaFilter = parameters != null ? (String) parameters.get("schema") : null;
            String tablePattern = parameters != null ? (String) parameters.get("table-pattern") : null;
            @SuppressWarnings("unchecked")
            List<String> excludeTables = parameters != null ? (List<String>) parameters.get("exclude-tables") : null;
            
            LOGGER.info("[SchemaReader] Enumerating tables with filters: schema={}, pattern={}, excludes={}", 
                       schemaFilter, tablePattern, excludeTables);
            
            // Query to get all tables
            String tablesQuery = buildTablesQuery(schemaFilter, tablePattern);
            LOGGER.debug("[SchemaReader] Tables query: {}", tablesQuery);
            
            List<Map<String, Object>> tables = dbDataSource.query(tablesQuery, new HashMap<>());
            LOGGER.info("[SchemaReader] Found {} tables in database", tables != null ? tables.size() : 0);
            
            if (tables == null || tables.isEmpty()) {
                LOGGER.warn("[SchemaReader] No tables found matching criteria");
                return tableSchemas;
            }
            
            // Read schema for each table
            for (Map<String, Object> tableInfo : tables) {
                // PostgreSQL returns lowercase column names, H2/SQL Server return uppercase
                String tableName = getStringValue(tableInfo, "TABLE_NAME", "table_name");
                
                // Apply exclusion filter
                if (isTableExcluded(tableName, excludeTables)) {
                    LOGGER.debug("[SchemaReader] Skipping excluded table: {}", tableName);
                    continue;
                }
                
                LOGGER.debug("[SchemaReader] Reading schema for table: {}", tableName);
                
                try {
                    // Create parameters for single table read, including schema filter
                    Map<String, Object> tableParams = new HashMap<>();
                    tableParams.put("table", tableName);
                    if (schemaFilter != null) {
                        tableParams.put("schema", schemaFilter);
                    }
                    
                    SchemaMetadata tableSchema = readDatabaseSchema(dataSource, tableParams);
                    tableSchemas.put(tableName, tableSchema);
                    
                    LOGGER.debug("[SchemaReader] Successfully read schema for table '{}': {} columns", 
                                tableName, tableSchema.getColumns().size());
                    
                } catch (Exception e) {
                    LOGGER.warn("[SchemaReader] Failed to read schema for table '{}': {}", 
                               tableName, e.getMessage());
                    // Continue with other tables even if one fails
                }
            }
            
            LOGGER.info("[SchemaReader] Successfully enumerated {} tables", tableSchemas.size());
            
        } catch (Exception e) {
            LOGGER.error("[SchemaReader] Failed to enumerate tables: {}", e.getMessage(), e);
            throw new DataPipelineException("Failed to enumerate database tables: " + e.getMessage(), e);
        }
        
        return tableSchemas;
    }

    /**
     * Build query to get all tables and views from INFORMATION_SCHEMA.
     * Note: Uses case-sensitive comparison to support both PostgreSQL (lowercase) 
     * and H2/SQL Server (uppercase).
     * Includes both BASE TABLE and VIEW types to support querying views.
     */
    private String buildTablesQuery(String schemaFilter, String tablePattern) {
        StringBuilder query = new StringBuilder();
        // Include both tables AND views - many users query views, not just tables
        query.append("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE IN ('BASE TABLE', 'VIEW')");
        
        if (schemaFilter != null && !schemaFilter.trim().isEmpty()) {
            // Use case-insensitive comparison for schema to support both H2 (PUBLIC) and PostgreSQL (public)
            query.append(" AND LOWER(TABLE_SCHEMA) = LOWER('").append(schemaFilter.trim()).append("')");
        }
        
        if (tablePattern != null && !tablePattern.trim().isEmpty()) {
            query.append(" AND TABLE_NAME LIKE '").append(tablePattern).append("'");
        }
        
        query.append(" ORDER BY TABLE_NAME");
        
        return query.toString();
    }

    /**
     * Check if a table should be excluded based on the exclusion list.
     */
    private boolean isTableExcluded(String tableName, List<String> excludeTables) {
        if (excludeTables == null || excludeTables.isEmpty()) {
            return false;
        }
        
        String tableNameUpper = tableName.toUpperCase();
        
        for (String exclude : excludeTables) {
            String excludeUpper = exclude.toUpperCase();
            
            // Support wildcard patterns (simple glob-style)
            if (excludeUpper.contains("*") || excludeUpper.contains("%")) {
                String regex = excludeUpper.replace("*", ".*").replace("%", ".*");
                if (tableNameUpper.matches(regex)) {
                    return true;
                }
            } else {
                // Exact match
                if (tableNameUpper.equals(excludeUpper)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Get a string value from a map, trying multiple key names (for database-specific column name casing).
     * PostgreSQL returns lowercase column names, H2/SQL Server return uppercase.
     */
    private String getStringValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    /**
     * Get an object value from a map, trying multiple key names (for database-specific column name casing).
     * PostgreSQL returns lowercase column names, H2/SQL Server return uppercase.
     */
    private Object getObjectValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Read schema from a database data source.
     * If 'table' parameter is not provided, this method should not be called directly.
     * Use enumerateAllTables() instead for bulk table enumeration.
     */
    private SchemaMetadata readDatabaseSchema(ExternalDataSource dataSource, Map<String, Object> parameters) 
            throws DataPipelineException {
        
        LOGGER.debug("[SchemaReader.DB] Starting database schema read for source: {}", dataSource.getName());
        
        if (!(dataSource instanceof DatabaseDataSource)) {
            LOGGER.error("[SchemaReader.DB] Invalid data source type: {}", dataSource.getClass().getSimpleName());
            throw new DataPipelineException("Data source is not a DatabaseDataSource: " + 
                                           dataSource.getClass().getSimpleName());
        }

        DatabaseDataSource dbDataSource = (DatabaseDataSource) dataSource;
        
        // Check if 'table' parameter is provided
        if (parameters == null || !parameters.containsKey("table")) {
            LOGGER.error("[SchemaReader.DB] Table name is required. Use enumerateAllTables() for bulk enumeration.");
            throw new DataPipelineException("Table name parameter is required for single table schema reading");
        }
        
        String tableName = (String) parameters.get("table");
        if (tableName == null || tableName.trim().isEmpty()) {
            LOGGER.error("[SchemaReader.DB] Table name is null or empty");
            throw new DataPipelineException("Table name cannot be null or empty");
        }
        
        // Get schema filter if provided (defaults to PUBLIC for H2 to avoid INFORMATION_SCHEMA tables)
        String schemaFilter = (String) parameters.get("schema");
        if (schemaFilter == null || schemaFilter.trim().isEmpty()) {
            schemaFilter = "PUBLIC";  // Default to PUBLIC schema
        }
        
        LOGGER.debug("[SchemaReader.DB] Reading schema for table: {} in schema: {}", tableName, schemaFilter);

        SchemaMetadata schema = new SchemaMetadata(dataSource.getName(), dataSource.getSourceType().toString());

        // Use reflection or a query-based approach since getConnection() is not directly exposed
        // We'll use a metadata query approach
        try {
            // Query information_schema to get column metadata
            String metadataQuery = buildSchemaQuery(tableName, schemaFilter);
            LOGGER.debug("[SchemaReader.DB] Executing metadata query: {}", metadataQuery);
            
            List<Map<String, Object>> columns = dbDataSource.query(metadataQuery, new HashMap<>());
            LOGGER.debug("[SchemaReader.DB] Query returned {} column records", 
                        columns != null ? columns.size() : 0);
            
            if (columns == null || columns.isEmpty()) {
                LOGGER.warn("[SchemaReader.DB] No columns found for table: {}", tableName);
                throw new DataPipelineException("Table not found or no columns: " + tableName);
            }

            LOGGER.debug("[SchemaReader.DB] Processing {} columns", columns.size());
            for (Map<String, Object> columnInfo : columns) {
                SchemaMetadata.ColumnDefinition column = new SchemaMetadata.ColumnDefinition();
                
                // Use case-insensitive column name lookup (PostgreSQL=lowercase, H2/SQL Server=uppercase)
                String columnName = getStringValue(columnInfo, "COLUMN_NAME", "column_name");
                column.setName(columnName);
                column.setDataType(getStringValue(columnInfo, "DATA_TYPE", "data_type"));
                
                LOGGER.debug("[SchemaReader.DB]   Processing column: {} ({})", 
                            columnName, column.getDataType());
                
                String isNullable = getStringValue(columnInfo, "IS_NULLABLE", "is_nullable");
                column.setNullable("YES".equalsIgnoreCase(isNullable));
                LOGGER.debug("[SchemaReader.DB]     Nullable: {}", column.isNullable());
                
                Object maxLength = getObjectValue(columnInfo, "CHARACTER_MAXIMUM_LENGTH", "character_maximum_length");
                if (maxLength instanceof Number) {
                    column.setMaxLength(((Number) maxLength).intValue());
                    LOGGER.debug("[SchemaReader.DB]     Max length: {}", column.getMaxLength());
                }
                
                Object precision = getObjectValue(columnInfo, "NUMERIC_PRECISION", "numeric_precision");
                if (precision instanceof Number) {
                    column.setPrecision(((Number) precision).intValue());
                    LOGGER.debug("[SchemaReader.DB]     Precision: {}", column.getPrecision());
                }
                
                Object scale = getObjectValue(columnInfo, "NUMERIC_SCALE", "numeric_scale");
                if (scale instanceof Number) {
                    column.setScale(((Number) scale).intValue());
                    LOGGER.debug("[SchemaReader.DB]     Scale: {}", column.getScale());
                }

                // Note: Primary key detection would require an additional query
                // For simplicity, we'll skip it here or check constraint type
                
                schema.addColumn(column);
            }

            LOGGER.info("Successfully read schema for table '{}': {} columns", 
                       tableName, schema.getColumns().size());
            LOGGER.debug("[SchemaReader.DB] Complete schema: {}", schema);

        } catch (Exception e) {
            LOGGER.error("[SchemaReader.DB] Failed to read database schema for table '{}': {}", 
                        tableName, e.getMessage(), e);
            throw new DataPipelineException("Failed to read database schema: " + e.getMessage(), e);
        }

        LOGGER.debug("[SchemaReader.DB] Returning schema with {} columns", schema.getColumns().size());
        return schema;
    }

    /**
     * Build a database-agnostic query for schema metadata.
     * Uses INFORMATION_SCHEMA which is supported by most databases.
     * Uses case-insensitive schema comparison to work with both H2 (PUBLIC) and PostgreSQL (public).
     */
    private String buildSchemaQuery(String tableName, String schemaFilter) {
        // Standard SQL INFORMATION_SCHEMA query
        // Note: Preserve table name case - PostgreSQL uses lowercase, H2/SQL Server use uppercase
        // Use LOWER() for case-insensitive schema comparison (H2 uses PUBLIC, PostgreSQL uses public)
        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        query.append("COLUMN_NAME, ");
        query.append("DATA_TYPE, ");
        query.append("IS_NULLABLE, ");
        query.append("CHARACTER_MAXIMUM_LENGTH, ");
        query.append("NUMERIC_PRECISION, ");
        query.append("NUMERIC_SCALE ");
        query.append("FROM INFORMATION_SCHEMA.COLUMNS ");
        query.append("WHERE TABLE_NAME = '").append(tableName).append("'");
        if (schemaFilter != null && !schemaFilter.trim().isEmpty()) {
            // Use case-insensitive comparison for schema to support both H2 and PostgreSQL
            query.append(" AND LOWER(TABLE_SCHEMA) = LOWER('").append(schemaFilter.trim()).append("')");
        }
        query.append(" ORDER BY ORDINAL_POSITION");
        return query.toString();
    }

    /**
     * Read schema from a CSV file.
     */
    private SchemaMetadata readCsvSchema(ExternalDataSource dataSource, Map<String, Object> parameters) 
            throws DataPipelineException {
        
        LOGGER.debug("[SchemaReader.CSV] Starting CSV schema read for source: {}", dataSource.getName());
        
        String csvFilePath = getRequiredParameter(parameters, "file", "CSV file path is required for CSV schema reading");
        LOGGER.debug("[SchemaReader.CSV] Reading CSV file: {}", csvFilePath);
        
        SchemaMetadata schema = new SchemaMetadata(dataSource.getName(), "csv");
        
        File csvFile = new File(csvFilePath);
        if (!csvFile.exists()) {
            LOGGER.error("[SchemaReader.CSV] CSV file not found: {}", csvFilePath);
            throw new DataPipelineException("CSV file not found: " + csvFilePath);
        }
        LOGGER.debug("[SchemaReader.CSV] File exists, size: {} bytes", csvFile.length());

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            // Read header line
            String headerLine = reader.readLine();
            LOGGER.debug("[SchemaReader.CSV] Header line: {}", headerLine);
            
            if (headerLine == null || headerLine.trim().isEmpty()) {
                LOGGER.error("[SchemaReader.CSV] CSV file is empty or has no header");
                throw new DataPipelineException("CSV file is empty or has no header: " + csvFilePath);
            }

            // Parse header columns
            String[] headers = parseCSVLine(headerLine);
            LOGGER.debug("[SchemaReader.CSV] Parsed {} header columns", headers.length);
            for (int i = 0; i < headers.length; i++) {
                LOGGER.debug("[SchemaReader.CSV]   Header[{}]: {}", i, headers[i]);
            }
            
            // Read first data line to infer types
            String firstDataLine = reader.readLine();
            LOGGER.debug("[SchemaReader.CSV] First data line: {}", firstDataLine);
            String[] firstValues = firstDataLine != null ? parseCSVLine(firstDataLine) : null;
            if (firstValues != null) {
                LOGGER.debug("[SchemaReader.CSV] Parsed {} data values for type inference", firstValues.length);
            } else {
                LOGGER.debug("[SchemaReader.CSV] No data values available, will default to VARCHAR");
            }

            LOGGER.debug("[SchemaReader.CSV] Creating column definitions");
            for (int i = 0; i < headers.length; i++) {
                SchemaMetadata.ColumnDefinition column = new SchemaMetadata.ColumnDefinition();
                String columnName = headers[i].trim();
                column.setName(columnName);
                
                // Infer data type from first value
                if (firstValues != null && i < firstValues.length) {
                    String value = firstValues[i].trim();
                    String inferredType = inferCsvDataType(value);
                    column.setDataType(inferredType);
                    LOGGER.debug("[SchemaReader.CSV]   Column[{}]: {} - inferred type: {} from value: '{}'", 
                                i, columnName, inferredType, value);
                } else {
                    column.setDataType("VARCHAR");
                    LOGGER.debug("[SchemaReader.CSV]   Column[{}]: {} - defaulted to VARCHAR (no data)", 
                                i, columnName);
                }
                
                // CSV columns are always nullable
                column.setNullable(true);
                
                schema.addColumn(column);
            }

            LOGGER.info("Successfully read CSV schema from '{}': {} columns", 
                       csvFilePath, schema.getColumns().size());
            LOGGER.debug("[SchemaReader.CSV] Complete schema: {}", schema);

        } catch (Exception e) {
            LOGGER.error("[SchemaReader.CSV] Failed to read CSV schema from '{}': {}", 
                        csvFilePath, e.getMessage(), e);
            throw new DataPipelineException("Failed to read CSV schema: " + e.getMessage(), e);
        }

        LOGGER.debug("[SchemaReader.CSV] Returning schema with {} columns", schema.getColumns().size());
        return schema;
    }

    /**
     * Parse a CSV line handling quoted fields.
     */
    private String[] parseCSVLine(String line) {
        LOGGER.debug("[SchemaReader.CSV] Parsing CSV line: {}", line);
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString());

        LOGGER.debug("[SchemaReader.CSV] Parsed {} fields", fields.size());
        return fields.toArray(new String[0]);
    }

    /**
     * Infer CSV data type from a sample value.
     */
    private String inferCsvDataType(String value) {
        LOGGER.debug("[SchemaReader.CSV] Inferring type for value: '{}'", value);
        
        if (value == null || value.isEmpty()) {
            LOGGER.debug("[SchemaReader.CSV] Empty value, defaulting to VARCHAR");
            return "VARCHAR";
        }

        // Try integer
        try {
            Integer.parseInt(value);
            LOGGER.debug("[SchemaReader.CSV] Detected INTEGER type");
            return "INTEGER";
        } catch (NumberFormatException e) {
            // Not an integer
        }

        // Try decimal
        try {
            Double.parseDouble(value);
            LOGGER.debug("[SchemaReader.CSV] Detected DECIMAL type");
            return "DECIMAL";
        } catch (NumberFormatException e) {
            // Not a number
        }

        // Try boolean
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            LOGGER.debug("[SchemaReader.CSV] Detected BOOLEAN type");
            return "BOOLEAN";
        }

        // Try date patterns (basic check)
        if (value.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            LOGGER.debug("[SchemaReader.CSV] Detected TIMESTAMP type");
            return "TIMESTAMP";
        }

        // Default to VARCHAR
        LOGGER.debug("[SchemaReader.CSV] Defaulting to VARCHAR type");
        return "VARCHAR";
    }

    /**
     * Get a required parameter from the map.
     */
    private String getRequiredParameter(Map<String, Object> parameters, String key, String errorMessage) 
            throws DataPipelineException {
        
        LOGGER.debug("[SchemaReader] Getting required parameter: {}", key);
        
        if (parameters == null) {
            LOGGER.error("[SchemaReader] Parameters map is null");
            throw new DataPipelineException(errorMessage);
        }

        Object value = parameters.get(key);
        if (value == null || value.toString().trim().isEmpty()) {
            LOGGER.error("[SchemaReader] Required parameter '{}' is missing or empty", key);
            throw new DataPipelineException(errorMessage);
        }

        String result = value.toString();
        LOGGER.debug("[SchemaReader] Parameter '{}' = '{}'", key, result);
        return result;
    }
}
