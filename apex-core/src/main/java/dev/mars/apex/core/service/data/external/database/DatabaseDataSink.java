package dev.mars.apex.core.service.data.external.database;

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

import dev.mars.apex.core.config.datasink.DataSinkConfiguration;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database implementation of DataSink.
 * 
 * This class provides JDBC-based database connectivity for writing data
 * with support for connection pooling, health monitoring, batch operations,
 * and transaction management.
 * 
 * Supported databases:
 * - PostgreSQL
 * - MySQL
 * - Oracle
 * - SQL Server
 * - H2 (for testing)
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DatabaseDataSink implements DataSink {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseDataSink.class);
    
    private DataSinkConfiguration configuration;
    private DataSource dataSource;
    private DataSinkMetrics metrics;
    private ConnectionStatus connectionStatus;
    private volatile boolean initialized = false;
    private volatile boolean shutdown = false;
    
    // Operation cache for prepared statements
    private final Map<String, String> operationCache = new ConcurrentHashMap<>();
    
    // Supported operations
    private static final List<String> SUPPORTED_OPERATIONS = Arrays.asList(
        "insert", "update", "upsert", "delete", "merge", "execute", "batch"
    );
    
    /**
     * Default constructor.
     */
    public DatabaseDataSink() {
        this.metrics = new DataSinkMetrics();
        this.connectionStatus = ConnectionStatus.notInitialized();
    }
    
    @Override
    public String getName() {
        return configuration != null ? configuration.getName() : "database-sink";
    }

    /**
     * Set the configuration for this data sink.
     * This method allows setting the configuration without full initialization.
     *
     * @param configuration The data sink configuration
     */
    public void setConfiguration(DataSinkConfiguration configuration) {
        this.configuration = configuration;
    }
    
    @Override
    public DataSinkType getSinkType() {
        return DataSinkType.DATABASE;
    }
    
    @Override
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }
    
    @Override
    public DataSinkMetrics getMetrics() {
        return metrics;
    }
    
    @Override
    public void initialize(DataSinkConfiguration config) throws DataSinkException {
        if (initialized) {
            throw DataSinkException.configurationError("Database sink is already initialized");
        }
        
        if (config == null) {
            throw DataSinkException.configurationError("Configuration cannot be null");
        }
        
        try {
            LOGGER.info("Initializing database sink: {}", config.getName());
            
            this.configuration = config;
            
            // Validate configuration
            validateConfiguration();
            
            // Create data source
            createDataSource();
            
            // Test connection
            testConnection();
            
            // Cache operations
            cacheOperations();
            
            // Initialize schema if needed
            initializeSchema();
            
            this.initialized = true;
            this.connectionStatus = ConnectionStatus.connected("Database sink initialized successfully");

            LOGGER.info("Database sink initialized successfully: {}", config.getName());

        } catch (Exception e) {
            this.connectionStatus = ConnectionStatus.error("Failed to initialize database sink", e);
            throw DataSinkException.configurationError("Failed to initialize database sink", e);
        }
    }
    
    @Override
    public void shutdown() {
        if (shutdown) {
            return;
        }
        
        LOGGER.info("Shutting down database sink: {}", getName());
        
        try {
            // Close data source if it's closeable
            if (dataSource instanceof AutoCloseable) {
                ((AutoCloseable) dataSource).close();
            }
            
            this.connectionStatus = ConnectionStatus.shutdown();
            this.shutdown = true;
            
            LOGGER.info("Database sink shutdown completed: {}", getName());
            
        } catch (Exception e) {
            LOGGER.error("Error during database sink shutdown", e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        if (!initialized || shutdown) {
            return false;
        }
        
        try {
            return testConnection();
        } catch (Exception e) {
            LOGGER.debug("Health check failed for database sink: {}", getName(), e);
            return false;
        }
    }
    
    @Override
    public void write(Object data) throws DataSinkException {
        write("insert", data);
    }
    
    @Override
    public void write(String operation, Object data) throws DataSinkException {
        write(operation, data, null);
    }
    
    @Override
    public void write(String operation, Object data, Map<String, Object> parameters) throws DataSinkException {
        if (!initialized) {
            throw DataSinkException.configurationError("Database sink not initialized");
        }
        
        if (shutdown) {
            throw DataSinkException.configurationError("Database sink is shutdown");
        }
        
        long startTime = System.currentTimeMillis();
        LOGGER.debug("Starting write operation '{}' on database sink '{}'", operation, getName());

        try {
            String sql = resolveOperation(operation);
            LOGGER.debug("Resolved operation '{}' to SQL: {}", operation, sql);

            Map<String, Object> allParameters = mergeParameters(data, parameters);
            LOGGER.debug("Merged parameters for write operation: {}", allParameters);

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = prepareStatement(connection, sql, allParameters)) {

                LOGGER.debug("Executing write statement for operation '{}'", operation);
                int rowsAffected = statement.executeUpdate();

                long executionTime = System.currentTimeMillis() - startTime;
                metrics.recordSuccessfulWrite(executionTime, rowsAffected);

                LOGGER.debug("Successfully wrote data using operation '{}', rows affected: {} in {}ms",
                    operation, rowsAffected, executionTime);

            }
            
        } catch (SQLException e) {
            metrics.recordFailedWrite(System.currentTimeMillis() - startTime);

            // Classify the SQL error to determine appropriate handling
            SqlErrorClassifier.SqlErrorType errorType = SqlErrorClassifier.classifyError(e);
            String errorDescription = SqlErrorClassifier.getErrorDescription(errorType);

            switch (errorType) {
                case DATA_INTEGRITY_VIOLATION:
                    // Log the constraint violation but don't crash the pipeline
                    LOGGER.warn("Data integrity violation in operation '{}': {} - Record will be skipped",
                               operation, e.getMessage());

                    // Create a non-fatal exception that can be handled gracefully
                    throw DataSinkException.dataIntegrityError(
                        "Data integrity violation: " + errorDescription, e,
                        "Operation: " + operation + ", Data: " + (data != null ? data.getClass().getSimpleName() : "null"));

                case TRANSIENT_ERROR:
                    // These errors should be retried
                    LOGGER.warn("Transient database error in operation '{}': {} - Operation can be retried",
                               operation, e.getMessage());

                    throw new DataSinkException(DataSinkException.ErrorType.CONNECTION_ERROR,
                                               "Transient database error: " + errorDescription, e,
                                               "Operation: " + operation,
                                               true); // Retryable

                case CONFIGURATION_ERROR:
                    // These are serious configuration issues that should fail fast
                    LOGGER.error("Database configuration error in operation '{}': {}", operation, e.getMessage());

                    throw DataSinkException.configurationError("Database configuration error: " + errorDescription, e);

                case FATAL_ERROR:
                default:
                    // Unknown errors should be escalated
                    LOGGER.error("Fatal database error in operation '{}': {}", operation, e.getMessage());

                    throw DataSinkException.writeError("Fatal database error: " + errorDescription, e,
                        "Operation: " + operation + ", Data: " + (data != null ? data.getClass().getSimpleName() : "null"));
            }
        }
    }
    
    @Override
    public void writeBatch(List<Object> data) throws DataSinkException {
        writeBatch("insert", data);
    }
    
    @Override
    public void writeBatch(String operation, List<Object> data) throws DataSinkException {
        writeBatch(operation, data, null);
    }
    
    @Override
    public void writeBatch(String operation, List<Object> data, Map<String, Object> parameters) throws DataSinkException {
        if (!initialized) {
            throw DataSinkException.configurationError("Database sink not initialized");
        }
        
        if (shutdown) {
            throw DataSinkException.configurationError("Database sink is shutdown");
        }
        
        if (data == null || data.isEmpty()) {
            LOGGER.debug("No data to write in batch operation");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int failureCount = 0;

        LOGGER.debug("Starting batch write operation '{}' on database sink '{}' with {} items",
            operation, getName(), data.size());

        try {
            String sql = resolveOperation(operation);
            LOGGER.debug("Resolved batch operation '{}' to SQL: {}", operation, sql);

            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                LOGGER.debug("Started transaction for batch write operation");

                // For batch operations, we need to prepare each statement individually
                // because named parameter processing needs to happen for each item
                for (Object item : data) {
                    try {
                        Map<String, Object> allParameters = mergeParameters(item, parameters);
                        try (PreparedStatement statement = prepareStatement(connection, sql, allParameters)) {
                            statement.executeUpdate();
                            successCount++;
                        }
                    } catch (SQLException e) {
                        failureCount++;

                        // Classify the SQL error to provide better logging
                        SqlErrorClassifier.SqlErrorType errorType = SqlErrorClassifier.classifyError(e);
                        String errorDescription = SqlErrorClassifier.getErrorDescription(errorType);

                        if (errorType == SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION) {
                            LOGGER.warn("Skipping batch item due to data integrity violation: {} - Item: {}",
                                       e.getMessage(), item);
                        } else {
                            LOGGER.warn("Failed to execute batch item due to {}: {} - Item: {}",
                                       errorDescription, e.getMessage(), item);
                        }
                    }
                }

                if (successCount > 0) {
                    connection.commit();
                    long executionTime = System.currentTimeMillis() - startTime;
                    metrics.recordSuccessfulBatch(executionTime, successCount);

                    LOGGER.debug("Successfully wrote batch using operation '{}', items: {} in {}ms",
                               operation, successCount, executionTime);
                } else {
                    LOGGER.debug("No items could be processed in batch, rolling back transaction");
                    connection.rollback();
                    throw DataSinkException.batchError("No items could be processed in batch", 0, data.size());
                }

            } catch (SQLException e) {
                metrics.recordFailedBatch(System.currentTimeMillis() - startTime);
                throw DataSinkException.batchError("Failed to write batch using operation: " + operation,
                                                 successCount, data.size());
            }

            if (failureCount > 0) {
                metrics.recordPartialBatch(System.currentTimeMillis() - startTime, successCount, failureCount);
            }

        } catch (Exception e) {
            metrics.recordFailedBatch(System.currentTimeMillis() - startTime);
            throw DataSinkException.batchError("Failed to write batch using operation: " + operation,
                                             successCount, data.size());
        }
    }
    
    @Override
    public Object execute(String operation, Map<String, Object> parameters) throws DataSinkException {
        if (!initialized) {
            throw DataSinkException.configurationError("Database sink not initialized");
        }

        LOGGER.debug("Executing operation '{}' on database sink '{}' with parameters: {}",
            operation, getName(), parameters);

        try {
            String sql = resolveOperation(operation);
            LOGGER.debug("Resolved operation '{}' to SQL: {}", operation, sql);

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = prepareStatement(connection, sql, parameters)) {

                LOGGER.debug("Executing statement for operation '{}'", operation);
                boolean hasResultSet = statement.execute();

                if (hasResultSet) {
                    LOGGER.debug("Operation '{}' returned result set", operation);
                    try (ResultSet resultSet = statement.getResultSet()) {
                        Object result = extractResultSet(resultSet);
                        LOGGER.debug("Operation '{}' completed with result: {}", operation,
                            result != null ? result.getClass().getSimpleName() : "null");
                        return result;
                    }
                } else {
                    int updateCount = statement.getUpdateCount();
                    LOGGER.debug("Operation '{}' completed with update count: {}", operation, updateCount);
                    return updateCount;
                }
            }
            
        } catch (SQLException e) {
            // Classify the SQL error to determine appropriate handling
            SqlErrorClassifier.SqlErrorType errorType = SqlErrorClassifier.classifyError(e);
            String errorDescription = SqlErrorClassifier.getErrorDescription(errorType);

            switch (errorType) {
                case DATA_INTEGRITY_VIOLATION:
                    LOGGER.warn("Data integrity violation in execute operation '{}': {}", operation, e.getMessage());
                    throw DataSinkException.dataIntegrityError(
                        "Data integrity violation: " + errorDescription, e,
                        "Operation: " + operation + ", Parameters: " + parameters);

                case TRANSIENT_ERROR:
                    LOGGER.warn("Transient database error in execute operation '{}': {}", operation, e.getMessage());
                    throw new DataSinkException(DataSinkException.ErrorType.CONNECTION_ERROR,
                                               "Transient database error: " + errorDescription, e,
                                               "Operation: " + operation,
                                               true); // Retryable

                case CONFIGURATION_ERROR:
                    LOGGER.error("Database configuration error in execute operation '{}': {}", operation, e.getMessage());
                    throw DataSinkException.configurationError("Database configuration error: " + errorDescription, e);

                case FATAL_ERROR:
                default:
                    LOGGER.error("Fatal database error in execute operation '{}': {}", operation, e.getMessage());
                    throw DataSinkException.writeError("Fatal database error: " + errorDescription, e,
                        "Operation: " + operation + ", Parameters: " + parameters);
            }
        }
    }
    
    @Override
    public void flush() throws DataSinkException {
        // For database sinks, flush is typically handled by connection commit
        // This is a no-op unless we implement buffering
        LOGGER.debug("Flush called on database sink: {}", getName());
    }
    
    @Override
    public DataSinkConfiguration getConfiguration() {
        return configuration;
    }
    
    @Override
    public void refreshConfiguration(DataSinkConfiguration config) throws DataSinkException {
        if (config == null) {
            throw DataSinkException.configurationError("Configuration cannot be null");
        }
        
        LOGGER.info("Refreshing configuration for database sink: {}", getName());
        
        // Store old configuration for rollback
        DataSinkConfiguration oldConfig = this.configuration;
        
        try {
            this.configuration = config;
            validateConfiguration();
            cacheOperations();
            
            LOGGER.info("Configuration refreshed successfully for database sink: {}", getName());
            
        } catch (Exception e) {
            // Rollback to old configuration
            this.configuration = oldConfig;
            throw DataSinkException.configurationError("Failed to refresh configuration", e);
        }
    }
    
    @Override
    public boolean testConnection() {
        if (dataSource == null) {
            return false;
        }
        
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            LOGGER.debug("Connection test failed", e);
            return false;
        }
    }
    
    @Override
    public List<String> getSupportedOperations() {
        List<String> operations = new ArrayList<>(SUPPORTED_OPERATIONS);
        if (configuration != null && configuration.getOperations() != null) {
            operations.addAll(configuration.getOperations().keySet());
        }
        return operations;
    }
    
    @Override
    public boolean supportsOperation(String operation) {
        return getSupportedOperations().contains(operation);
    }
    
    @Override
    public Map<String, Object> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", getName());
        info.put("type", getSinkType().getDisplayName());
        info.put("status", getConnectionStatus().getState().name());
        info.put("initialized", initialized);
        info.put("shutdown", shutdown);
        info.put("supportedOperations", getSupportedOperations());
        
        if (configuration != null) {
            info.put("sourceType", configuration.getSourceType());
            info.put("enabled", configuration.isEnabled());
            if (configuration.getConnection() != null) {
                info.put("host", configuration.getConnection().getHost());
                info.put("port", configuration.getConnection().getPort());
                info.put("database", configuration.getConnection().getDatabase());
            }
        }
        
        info.put("metrics", metrics.toString());
        
        return info;
    }
    
    // Private helper methods
    
    private void validateConfiguration() throws DataSinkException {
        if (configuration.getConnection() == null) {
            throw DataSinkException.configurationError("Connection configuration is required");
        }
        
        if (configuration.getConnection().getDatabase() == null) {
            throw DataSinkException.configurationError("Database name is required");
        }
        
        // Validate operations
        if (configuration.getOperations() != null) {
            for (Map.Entry<String, String> entry : configuration.getOperations().entrySet()) {
                if (entry.getValue() == null || entry.getValue().trim().isEmpty()) {
                    throw DataSinkException.configurationError("Operation '" + entry.getKey() + "' has empty SQL");
                }
            }
        }
    }
    
    private void createDataSource() throws DataSinkException {
        try {
            // Convert DataSinkConfiguration to DataSourceConfiguration for JdbcTemplateFactory
            DataSourceConfiguration dataSourceConfig = convertToDataSourceConfiguration(configuration);
            this.dataSource = JdbcTemplateFactory.createDataSource(dataSourceConfig);

        } catch (Exception e) {
            throw DataSinkException.connectionError("Failed to create data source", e);
        }
    }

    /**
     * Convert DataSinkConfiguration to DataSourceConfiguration for JDBC factory.
     */
    private DataSourceConfiguration convertToDataSourceConfiguration(DataSinkConfiguration sinkConfig) {
        DataSourceConfiguration sourceConfig = new DataSourceConfiguration(sinkConfig.getName(), sinkConfig.getType());
        sourceConfig.setSourceType(sinkConfig.getSourceType());
        sourceConfig.setDescription(sinkConfig.getDescription());
        sourceConfig.setEnabled(sinkConfig.isEnabled());
        sourceConfig.setImplementation(sinkConfig.getImplementation());

        // Copy connection configuration
        if (sinkConfig.getConnection() != null) {
            sourceConfig.setConnection(sinkConfig.getConnection());
        }

        // Copy cache configuration
        if (sinkConfig.getCache() != null) {
            sourceConfig.setCache(sinkConfig.getCache());
        }

        // Copy health check configuration
        if (sinkConfig.getHealthCheck() != null) {
            sourceConfig.setHealthCheck(sinkConfig.getHealthCheck());
        }

        // Copy authentication configuration
        if (sinkConfig.getAuthentication() != null) {
            sourceConfig.setAuthentication(sinkConfig.getAuthentication());
        }

        // Copy circuit breaker configuration
        if (sinkConfig.getCircuitBreaker() != null) {
            sourceConfig.setCircuitBreaker(sinkConfig.getCircuitBreaker());
        }

        return sourceConfig;
    }
    
    private void cacheOperations() {
        operationCache.clear();
        
        if (configuration.getOperations() != null) {
            operationCache.putAll(configuration.getOperations());
        }
        
        LOGGER.debug("Cached {} operations for database sink: {}", operationCache.size(), getName());
    }
    
    private void initializeSchema() throws DataSinkException {
        if (configuration.getSchema() != null && configuration.getSchema().getAutoCreate()) {
            LOGGER.info("Initializing schema auto-creation for database sink: {}", getName());

            try {
                // Execute initialization script if provided
                String initScript = configuration.getSchema().getInitScript();
                LOGGER.info("Checking for init-script: {}", initScript != null ? "present (" + initScript.length() + " chars)" : "null");

                if (initScript != null && !initScript.trim().isEmpty()) {
                    LOGGER.info("Executing schema initialization script for database sink: {}", getName());
                    executeSchemaInitScript(initScript);
                } else {
                    LOGGER.warn("No init-script found for schema auto-creation in database sink: {}", getName());
                }

                // Execute additional initialization scripts if provided
                if (configuration.getSchema().getInitScripts() != null &&
                    !configuration.getSchema().getInitScripts().isEmpty()) {

                    for (String script : configuration.getSchema().getInitScripts()) {
                        if (script != null && !script.trim().isEmpty()) {
                            executeSchemaInitScript(script);
                        }
                    }
                }

                // Verify schema creation with failsafe checks
                verifySchemaCreation();

                LOGGER.info("Schema initialization completed successfully for database sink: {}", getName());

            } catch (Exception e) {
                throw DataSinkException.configurationError("Schema initialization failed for database sink: " + getName(), e);
            }
        }
    }

    /**
     * Execute a schema initialization script.
     */
    private void executeSchemaInitScript(String script) throws DataSinkException {
        if (dataSource == null) {
            throw DataSinkException.configurationError("Data source not available for schema initialization");
        }

        try {
            LOGGER.info("Executing schema initialization script for database sink: {}", getName());
            LOGGER.info("Script content preview: {}", script.substring(0, Math.min(100, script.length())) + "...");

            // Split script into individual statements (simple approach)
            String[] statements = script.split(";");
            LOGGER.info("Split script into {} statements", statements.length);



            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(true); // Auto-commit for DDL statements

                for (int i = 0; i < statements.length; i++) {
                    String trimmedStatement = statements[i].trim();

                    // Remove comment lines and extract actual SQL
                    String[] lines = trimmedStatement.split("\n");
                    StringBuilder sqlBuilder = new StringBuilder();
                    for (String line : lines) {
                        String trimmedLine = line.trim();
                        if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("--")) {
                            sqlBuilder.append(trimmedLine).append(" ");
                        }
                    }
                    String actualSql = sqlBuilder.toString().trim();

                    if (!actualSql.isEmpty()) {
                        LOGGER.info("Executing SQL statement {}/{}: {}", i + 1, statements.length,
                            actualSql.substring(0, Math.min(50, actualSql.length())) + "...");

                        try (PreparedStatement preparedStatement = connection.prepareStatement(actualSql)) {
                            preparedStatement.execute();
                            LOGGER.info("✓ Statement {}/{} executed successfully", i + 1, statements.length);
                        } catch (Exception e) {
                            LOGGER.error("❌ Statement {}/{} failed: {}", i + 1, statements.length, e.getMessage());
                            throw e;
                        }
                    } else {
                        LOGGER.info("Skipping empty or comment-only statement {}/{}", i + 1, statements.length);
                    }
                }
            }

            LOGGER.debug("Schema initialization script executed successfully");

        } catch (Exception e) {
            LOGGER.error("Failed to execute schema initialization script", e);
            throw DataSinkException.configurationError("Schema initialization script execution failed", e);
        }
    }

    /**
     * Verify that the schema was created successfully by performing failsafe checks.
     */
    private void verifySchemaCreation() throws DataSinkException {
        if (dataSource == null) {
            throw DataSinkException.configurationError("Data source not available for schema verification");
        }

        try {
            LOGGER.info("Performing schema verification checks for database sink: {}", getName());

            try (Connection connection = dataSource.getConnection()) {
                // Check if the main table exists (if specified in schema config)
                if (configuration.getSchema().getTableName() != null) {
                    verifyTableExists(connection, configuration.getSchema().getTableName());
                }

                // Perform a basic connectivity test by querying system tables
                verifyDatabaseConnectivity(connection);

                LOGGER.info("✓ Schema verification completed successfully for database sink: {}", getName());
            }

        } catch (Exception e) {
            LOGGER.error("❌ Schema verification failed for database sink: {}", getName(), e);
            throw DataSinkException.configurationError("Schema verification failed", e);
        }
    }

    /**
     * Verify that a specific table exists and is accessible.
     */
    private void verifyTableExists(Connection connection, String tableName) throws Exception {
        String testQuery = "SELECT COUNT(*) FROM " + tableName + " WHERE 1=0";

        try (PreparedStatement statement = connection.prepareStatement(testQuery)) {
            statement.executeQuery();
            LOGGER.info("✓ Table '{}' exists and is accessible", tableName);
        } catch (Exception e) {
            LOGGER.error("❌ Table '{}' verification failed: {}", tableName, e.getMessage());
            throw new Exception("Table '" + tableName + "' does not exist or is not accessible", e);
        }
    }

    /**
     * Verify basic database connectivity by querying system information.
     */
    private void verifyDatabaseConnectivity(Connection connection) throws Exception {
        try {
            // Get database metadata to verify connection
            var metadata = connection.getMetaData();
            String databaseName = metadata.getDatabaseProductName();
            String databaseVersion = metadata.getDatabaseProductVersion();

            LOGGER.info("✓ Database connectivity verified: {} {}", databaseName, databaseVersion);

            // Test a simple query that should work on all databases
            try (PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
                statement.executeQuery();
                LOGGER.info("✓ Basic query execution verified");
            }

        } catch (Exception e) {
            LOGGER.error("❌ Database connectivity verification failed: {}", e.getMessage());
            throw new Exception("Database connectivity verification failed", e);
        }
    }

    private String resolveOperation(String operation) throws DataSinkException {
        String sql = operationCache.get(operation);
        if (sql == null) {
            throw DataSinkException.configurationError("Unknown operation: " + operation);
        }
        return sql;
    }
    
    private Map<String, Object> mergeParameters(Object data, Map<String, Object> parameters) {
        Map<String, Object> merged = new HashMap<>();
        
        if (parameters != null) {
            merged.putAll(parameters);
        }
        
        if (data != null) {
            // Convert data object to parameters using reflection or other means
            // This is a simplified implementation
            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) data;
                merged.putAll(dataMap);
            } else {
                merged.put("data", data);
            }
        }
        
        return merged;
    }
    
    private PreparedStatement prepareStatement(Connection connection, String sql, Map<String, Object> parameters)
            throws SQLException {
        return JdbcParameterUtils.prepareStatement(connection, sql, parameters);
    }
    
    private List<Map<String, Object>> extractResultSet(ResultSet resultSet) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        while (resultSet.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = resultSet.getObject(i);
                row.put(columnName, value);
            }
            results.add(row);
        }
        
        return results;
    }
}
