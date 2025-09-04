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
        
        try {
            String sql = resolveOperation(operation);
            Map<String, Object> allParameters = mergeParameters(data, parameters);
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = prepareStatement(connection, sql, allParameters)) {
                
                int rowsAffected = statement.executeUpdate();
                
                metrics.recordSuccessfulWrite(System.currentTimeMillis() - startTime, rowsAffected);
                
                LOGGER.debug("Successfully wrote data using operation '{}', rows affected: {}", operation, rowsAffected);
                
            }
            
        } catch (SQLException e) {
            metrics.recordFailedWrite(System.currentTimeMillis() - startTime);
            throw DataSinkException.writeError("Failed to write data using operation: " + operation, e, 
                "Data: " + (data != null ? data.getClass().getSimpleName() : "null"));
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
        
        try {
            String sql = resolveOperation(operation);
            
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    for (Object item : data) {
                        try {
                            Map<String, Object> allParameters = mergeParameters(item, parameters);
                            setStatementParameters(statement, allParameters);
                            statement.addBatch();
                            successCount++;
                        } catch (SQLException e) {
                            failureCount++;
                            LOGGER.warn("Failed to add item to batch: {}", e.getMessage());
                        }
                    }
                    
                    if (successCount > 0) {
                        int[] results = statement.executeBatch();
                        connection.commit();
                        
                        int totalRowsAffected = Arrays.stream(results).sum();
                        metrics.recordSuccessfulBatch(System.currentTimeMillis() - startTime, totalRowsAffected);
                        
                        LOGGER.debug("Successfully wrote batch using operation '{}', items: {}, rows affected: {}", 
                                   operation, successCount, totalRowsAffected);
                    } else {
                        connection.rollback();
                        throw DataSinkException.batchError("No items could be added to batch", 0, data.size());
                    }
                    
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                }
                
            }
            
            if (failureCount > 0) {
                metrics.recordPartialBatch(System.currentTimeMillis() - startTime, successCount, failureCount);
            }
            
        } catch (SQLException e) {
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
        
        try {
            String sql = resolveOperation(operation);
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = prepareStatement(connection, sql, parameters)) {
                
                boolean hasResultSet = statement.execute();
                
                if (hasResultSet) {
                    try (ResultSet resultSet = statement.getResultSet()) {
                        return extractResultSet(resultSet);
                    }
                } else {
                    return statement.getUpdateCount();
                }
            }
            
        } catch (SQLException e) {
            throw DataSinkException.writeError("Failed to execute operation: " + operation, e, 
                "Parameters: " + parameters);
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
                if (configuration.getSchema().getInitScript() != null &&
                    !configuration.getSchema().getInitScript().trim().isEmpty()) {

                    executeSchemaInitScript(configuration.getSchema().getInitScript());
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
            LOGGER.debug("Executing schema initialization script for database sink: {}", getName());

            // Split script into individual statements (simple approach)
            String[] statements = script.split(";");

            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(true); // Auto-commit for DDL statements

                for (String statement : statements) {
                    String trimmedStatement = statement.trim();
                    if (!trimmedStatement.isEmpty() && !trimmedStatement.startsWith("--")) {
                        LOGGER.debug("Executing SQL statement: {}", trimmedStatement.substring(0, Math.min(50, trimmedStatement.length())) + "...");

                        try (PreparedStatement preparedStatement = connection.prepareStatement(trimmedStatement)) {
                            preparedStatement.execute();
                        }
                    }
                }
            }

            LOGGER.debug("Schema initialization script executed successfully");

        } catch (Exception e) {
            LOGGER.error("Failed to execute schema initialization script", e);
            throw DataSinkException.configurationError("Schema initialization script execution failed", e);
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
        PreparedStatement statement = connection.prepareStatement(sql);
        setStatementParameters(statement, parameters);
        return statement;
    }
    
    private void setStatementParameters(PreparedStatement statement, Map<String, Object> parameters)
            throws SQLException {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        // This is a simplified parameter setting implementation
        // In a full implementation, you would need proper parameter mapping based on SQL parameter names
        int paramIndex = 1;
        for (Object value : parameters.values()) {
            statement.setObject(paramIndex++, value);
        }
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
