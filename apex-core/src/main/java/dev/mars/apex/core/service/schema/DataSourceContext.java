package dev.mars.apex.core.service.schema;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds context information about the data source used for schema enumeration.
 * This information is included in generated reports for traceability.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class DataSourceContext {

    private String dataSourceName;
    private String dataSourceType; // "database", "csv", "json", etc.
    
    // Database-specific fields
    private String host;
    private Integer port;
    private String databaseName;
    private String schemaName;
    private String username;
    private String jdbcUrl;
    private String databaseType; // "postgresql", "h2", "mysql", etc.
    
    // File-specific fields (CSV, JSON, etc.)
    private String filePath;
    private String fileName;
    private String fileDirectory;
    
    // Filter parameters
    private String tablePattern;
    private String schemaFilter;
    private java.util.List<String> excludeTables;
    
    // Additional metadata
    private Map<String, String> additionalProperties = new HashMap<>();

    public DataSourceContext() {
    }

    // Builder-style setters for fluent API
    public DataSourceContext dataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
        return this;
    }

    public DataSourceContext dataSourceType(String dataSourceType) {
        this.dataSourceType = dataSourceType;
        return this;
    }

    public DataSourceContext host(String host) {
        this.host = host;
        return this;
    }

    public DataSourceContext port(Integer port) {
        this.port = port;
        return this;
    }

    public DataSourceContext databaseName(String databaseName) {
        this.databaseName = databaseName;
        return this;
    }

    public DataSourceContext schemaName(String schemaName) {
        this.schemaName = schemaName;
        return this;
    }

    public DataSourceContext username(String username) {
        this.username = username;
        return this;
    }

    public DataSourceContext jdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        return this;
    }

    public DataSourceContext databaseType(String databaseType) {
        this.databaseType = databaseType;
        return this;
    }

    public DataSourceContext filePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public DataSourceContext fileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public DataSourceContext fileDirectory(String fileDirectory) {
        this.fileDirectory = fileDirectory;
        return this;
    }

    public DataSourceContext tablePattern(String tablePattern) {
        this.tablePattern = tablePattern;
        return this;
    }

    public DataSourceContext schemaFilter(String schemaFilter) {
        this.schemaFilter = schemaFilter;
        return this;
    }

    public DataSourceContext excludeTables(java.util.List<String> excludeTables) {
        this.excludeTables = excludeTables;
        return this;
    }

    public DataSourceContext addProperty(String key, String value) {
        this.additionalProperties.put(key, value);
        return this;
    }

    // Standard getters
    public String getDataSourceName() {
        return dataSourceName;
    }

    public String getDataSourceType() {
        return dataSourceType;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getUsername() {
        return username;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileDirectory() {
        return fileDirectory;
    }

    public String getTablePattern() {
        return tablePattern;
    }

    public String getSchemaFilter() {
        return schemaFilter;
    }

    public java.util.List<String> getExcludeTables() {
        return excludeTables;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    /**
     * Check if this is a database data source.
     */
    public boolean isDatabase() {
        return "database".equalsIgnoreCase(dataSourceType);
    }

    /**
     * Check if this is a file-based data source (CSV, JSON, etc.).
     */
    public boolean isFile() {
        return "csv".equalsIgnoreCase(dataSourceType) || 
               "json".equalsIgnoreCase(dataSourceType) ||
               "file".equalsIgnoreCase(dataSourceType);
    }

    /**
     * Get display-friendly connection string (masks sensitive info).
     */
    public String getConnectionSummary() {
        if (isDatabase()) {
            if (jdbcUrl != null && !jdbcUrl.isEmpty()) {
                // Mask password if present in URL
                return jdbcUrl.replaceAll("password=[^&;]*", "password=****");
            }
            return String.format("%s:%s/%s", 
                    host != null ? host : "localhost",
                    port != null ? port : "default",
                    databaseName != null ? databaseName : "unknown");
        } else if (isFile()) {
            return filePath != null ? filePath : fileName;
        }
        return dataSourceName;
    }

    @Override
    public String toString() {
        return "DataSourceContext{" +
                "dataSourceName='" + dataSourceName + '\'' +
                ", dataSourceType='" + dataSourceType + '\'' +
                ", connectionSummary='" + getConnectionSummary() + '\'' +
                '}';
    }
}
