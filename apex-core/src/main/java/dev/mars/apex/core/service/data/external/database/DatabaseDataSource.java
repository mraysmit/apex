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


import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.*;
import dev.mars.apex.core.service.data.external.cache.EnhancedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database implementation of ExternalDataSource.
 * 
 * This class provides JDBC-based database connectivity with support for
 * connection pooling, health monitoring, caching, and metrics collection.
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
public class DatabaseDataSource implements ExternalDataSource {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseDataSource.class);
    
    private final DataSource dataSource;
    private DataSourceConfiguration configuration;
    private ConnectionStatus connectionStatus;
    private DataSourceMetrics metrics;
    private DatabaseHealthIndicator healthIndicator;
    
    // Cache for prepared statements
    private final Map<String, String> preparedQueries = new ConcurrentHashMap<>();
    
    // Enhanced cache manager for query results
    private EnhancedCacheManager cacheManager;
    
    /**
     * Constructor with DataSource and configuration.
     * 
     * @param dataSource The JDBC DataSource
     * @param configuration The data source configuration
     */
    public DatabaseDataSource(DataSource dataSource, DataSourceConfiguration configuration) {
        LOGGER.info("APEX-CORE: Creating DatabaseDataSource instance for '{}'", configuration.getName());
        this.dataSource = dataSource;
        this.configuration = configuration;
        this.connectionStatus = ConnectionStatus.notInitialized();
        this.metrics = new DataSourceMetrics();
        this.healthIndicator = new DatabaseHealthIndicator(dataSource, configuration);
        this.cacheManager = new EnhancedCacheManager(configuration);
    }
    
    @Override
    public void initialize(DataSourceConfiguration config) throws DataSourceException {
        this.configuration = config;
        this.connectionStatus = ConnectionStatus.connecting();
        
        try {
            // Test the connection
            if (testConnection()) {
                this.connectionStatus = ConnectionStatus.connected("Database connection established");
                LOGGER.info("Database data source '{}' initialized successfully", config.getName());

                // Test database connectivity and log table existence
                try (Connection testConn = dataSource.getConnection()) {
                    LOGGER.info("Testing database connectivity for '{}'", config.getName());
                    LOGGER.info("Database URL: {}", testConn.getMetaData().getURL());

                    // Check if customers table exists and has data
                    try (Statement stmt = testConn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")) {
                        if (rs.next()) {
                            int count = rs.getInt(1);
                            LOGGER.info("Found {} records in customers table", count);
                        } else {
                            LOGGER.warn("No records found in customers table");
                        }
                    } catch (SQLException e) {
                        LOGGER.warn("Customers table does not exist or query failed: {}", e.getMessage());
                    }
                } catch (SQLException e) {
                    LOGGER.error("Failed to test database connectivity: {}", e.getMessage());
                }
            } else {
                throw new DataSourceException(DataSourceException.ErrorType.CONNECTION_ERROR,
                    "Failed to establish database connection", null, config.getName(), "initialize", true);
            }
        } catch (Exception e) {
            this.connectionStatus = ConnectionStatus.error("Initialization failed", e);
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to initialize database data source", e, config.getName(), "initialize", false);
        }
    }
    
    @Override
    public DataSourceType getSourceType() {
        return DataSourceType.DATABASE;
    }
    
    @Override
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }
    
    @Override
    public DataSourceMetrics getMetrics() {
        return metrics;
    }
    
    @Override
    public boolean isHealthy() {
        return healthIndicator.isHealthy();
    }
    
    @Override
    public boolean testConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            LOGGER.warn("Database connection test failed", e);
            return false;
        }
    }
    
    @Override
    public String getName() {
        return configuration != null ? configuration.getName() : "database-source";
    }
    
    @Override
    public String getDataType() {
        return configuration != null ? configuration.getSourceType() : "database";
    }
    
    @Override
    public boolean supportsDataType(String dataType) {
        return "database".equals(dataType) || 
               (configuration != null && configuration.getSourceType().equals(dataType));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(String dataType, Object... parameters) {
        LOGGER.info("TRACE: DatabaseDataSource.getData called - dataType: {}, parameters: {}", dataType, java.util.Arrays.toString(parameters));
        long startTime = System.currentTimeMillis();
        
        try {
            // Check cache first if enabled
            if (cacheManager.isEnabled()) {
                String cacheKey = cacheManager.generateCacheKey(dataType, parameters);
                LOGGER.debug("Checking cache for key: {}", cacheKey);
                Object cached = cacheManager.get(cacheKey);
                if (cached != null) {
                    LOGGER.debug("Cache hit for key: {}", cacheKey);
                    metrics.recordCacheHit();
                    metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
                    return (T) cached;
                }
                LOGGER.debug("Cache miss for key: {}", cacheKey);
                metrics.recordCacheMiss();
            }

            // Execute database query
            LOGGER.debug("Executing database query for data type: {}", dataType);
            Object result = executeQuery(dataType, parameters);

            // Cache the result if caching is enabled
            if (cacheManager.isEnabled() && result != null) {
                String cacheKey = cacheManager.generateCacheKey(dataType, parameters);
                cacheManager.put(cacheKey, result);
                LOGGER.debug("Cached result for key: {}", cacheKey);
            }

            long executionTime = System.currentTimeMillis() - startTime;
            metrics.recordSuccessfulRequest(executionTime);
            LOGGER.debug("Database operation completed in {}ms", executionTime);
            return (T) result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            metrics.recordFailedRequest(executionTime);
            LOGGER.error("Failed to get data from database after {}ms: {}", executionTime, e.getMessage());
            LOGGER.debug("Database operation failed", e);
            return null;
        }
    }
    
    @Override
    public <T> List<T> query(String query, Map<String, Object> parameters) throws DataSourceException {
        LOGGER.info("TRACE: DatabaseDataSource.query called - query: {}, parameters: {}", query, parameters);
        // Validate inputs
        if (query == null) {
            throw DataSourceException.configurationError("Query cannot be null");
        }
        if (parameters == null) {
            throw new NullPointerException("Parameters cannot be null");
        }

        long startTime = System.currentTimeMillis();
        LOGGER.debug("Executing database query on '{}' with {} parameters", getName(), parameters.size());
        LOGGER.debug("Query: {}", query);
        LOGGER.debug("Parameters: {}", parameters);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = prepareStatement(connection, query, parameters)) {

            // Check if this is an UPDATE, INSERT, DELETE statement
            // But exclude INSERT/UPDATE/DELETE with RETURNING clause as they return results
            String trimmedQuery = query.trim().toUpperCase();
            boolean isModifyingStatement = trimmedQuery.startsWith("UPDATE") || trimmedQuery.startsWith("INSERT") ||
                                         trimmedQuery.startsWith("DELETE") || trimmedQuery.startsWith("CREATE") ||
                                         trimmedQuery.startsWith("DROP") || trimmedQuery.startsWith("ALTER");
            boolean hasReturningClause = trimmedQuery.contains("RETURNING");

            if (isModifyingStatement && !hasReturningClause) {

                // Use executeUpdate for DML/DDL statements
                LOGGER.debug("Executing DML/DDL statement (executeUpdate)");
                int updateCount = statement.executeUpdate();
                long executionTime = System.currentTimeMillis() - startTime;

                metrics.recordSuccessfulRequest(executionTime);
                metrics.recordRecordsProcessed(updateCount);

                LOGGER.debug("DML/DDL statement completed: {} rows affected in {}ms", updateCount, executionTime);
                // Return empty list for update operations
                return new ArrayList<>();

            } else {
                // Use executeQuery for SELECT statements
                LOGGER.debug("Executing SELECT statement (executeQuery)");
                ResultSet resultSet = statement.executeQuery();
                List<T> results = new ArrayList<>();

                while (resultSet.next()) {
                    @SuppressWarnings("unchecked")
                    T result = (T) mapResultSetToObject(resultSet);
                    results.add(result);
                }
                long executionTime = System.currentTimeMillis() - startTime;

                metrics.recordSuccessfulRequest(executionTime);
                metrics.recordRecordsProcessed(results.size());

                LOGGER.debug("SELECT statement completed: {} rows returned in {}ms", results.size(), executionTime);
                return results;
            }

        } catch (SQLException e) {
            metrics.recordFailedRequest(System.currentTimeMillis() - startTime);

            // Classify the SQL error to provide better error handling
            SqlErrorClassifier.SqlErrorType errorType = SqlErrorClassifier.classifyError(e);
            String errorDescription = SqlErrorClassifier.getErrorDescription(errorType);

            switch (errorType) {
                case CONFIGURATION_ERROR:
                    LOGGER.error("Database configuration error in query: {}", e.getMessage());
                    throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                                                 "Database configuration error: " + errorDescription, e,
                                                 configuration.getName(), "query", false);

                case TRANSIENT_ERROR:
                    LOGGER.warn("Transient database error in query: {}", e.getMessage());
                    throw new DataSourceException(DataSourceException.ErrorType.CONNECTION_ERROR,
                                                 "Transient database error: " + errorDescription, e,
                                                 configuration.getName(), "query", true); // Retryable

                case DATA_INTEGRITY_VIOLATION:
                case FATAL_ERROR:
                default:
                    LOGGER.error("Database query failed: {}", e.getMessage());
                    throw DataSourceException.executionError("Database query failed: " + errorDescription, e, "query");
            }
        }
    }
    
    @Override
    public <T> T queryForObject(String query, Map<String, Object> parameters) throws DataSourceException {
        LOGGER.info("TRACE: DatabaseDataSource.queryForObject called - query: {}, parameters: {}", query, parameters);
        LOGGER.debug("Executing queryForObject on '{}' - expecting single result", getName());
        LOGGER.debug("QueryForObject query: {}", query);
        LOGGER.debug("QueryForObject parameters: {}", parameters);
        List<T> results = query(query, parameters);
        T result = results.isEmpty() ? null : results.get(0);
        LOGGER.debug("queryForObject completed: {} result", result != null ? "found" : "no");
        return result;
    }
    
    @Override
    public <T> List<List<T>> batchQuery(List<String> queries) throws DataSourceException {
        LOGGER.debug("Executing batch query on '{}' with {} queries", getName(), queries.size());
        List<List<T>> results = new ArrayList<>();

        for (int i = 0; i < queries.size(); i++) {
            String query = queries.get(i);
            LOGGER.debug("Executing batch query {}/{}: {}", i + 1, queries.size(), query);
            List<T> queryResult = query(query, Collections.emptyMap());
            results.add(queryResult);
            LOGGER.debug("Batch query {}/{} completed: {} results", i + 1, queries.size(), queryResult.size());
        }

        LOGGER.debug("Batch query completed: {} queries executed", queries.size());
        return results;
    }
    
    @Override
    public void batchUpdate(List<String> updates) throws DataSourceException {
        LOGGER.debug("Executing batch update on '{}' with {} statements", getName(), updates.size());
        long startTime = System.currentTimeMillis();

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            LOGGER.debug("Started transaction for batch update");

            try (Statement statement = connection.createStatement()) {
                for (int i = 0; i < updates.size(); i++) {
                    String update = updates.get(i);
                    LOGGER.debug("Adding batch statement {}/{}: {}", i + 1, updates.size(), update);
                    statement.addBatch(update);
                }

                LOGGER.debug("Executing batch of {} statements", updates.size());
                int[] updateCounts = statement.executeBatch();
                connection.commit();

                long executionTime = System.currentTimeMillis() - startTime;
                int totalRows = Arrays.stream(updateCounts).sum();

                metrics.recordSuccessfulRequest(executionTime);
                metrics.recordRecordsProcessed(totalRows);

                LOGGER.debug("Batch update completed: {} statements executed, {} total rows affected in {}ms",
                    updates.size(), totalRows, executionTime);

            } catch (SQLException e) {
                LOGGER.debug("Batch update failed, rolling back transaction: {}", e.getMessage());
                connection.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            metrics.recordFailedRequest(System.currentTimeMillis() - startTime);

            // Classify the SQL error to provide better error handling
            SqlErrorClassifier.SqlErrorType errorType = SqlErrorClassifier.classifyError(e);
            String errorDescription = SqlErrorClassifier.getErrorDescription(errorType);

            switch (errorType) {
                case CONFIGURATION_ERROR:
                    LOGGER.error("Database configuration error in batch update: {}", e.getMessage());
                    throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                                                 "Database configuration error: " + errorDescription, e,
                                                 configuration.getName(), "batchUpdate", false);

                case TRANSIENT_ERROR:
                    LOGGER.warn("Transient database error in batch update: {}", e.getMessage());
                    throw new DataSourceException(DataSourceException.ErrorType.CONNECTION_ERROR,
                                                 "Transient database error: " + errorDescription, e,
                                                 configuration.getName(), "batchUpdate", true); // Retryable

                case DATA_INTEGRITY_VIOLATION:
                    LOGGER.warn("Data integrity violation in batch update: {}", e.getMessage());
                    throw DataSourceException.executionError("Data integrity violation: " + errorDescription, e, "batchUpdate");

                case FATAL_ERROR:
                default:
                    LOGGER.error("Batch update failed: {}", e.getMessage());
                    throw DataSourceException.executionError("Batch update failed: " + errorDescription, e, "batchUpdate");
            }
        }
    }
    
    @Override
    public DataSourceConfiguration getConfiguration() {
        return configuration;
    }
    
    @Override
    public void refresh() throws DataSourceException {
        // Clear cache
        cacheManager.clear();

        // Test connection
        if (!testConnection()) {
            throw DataSourceException.connectionError("Database connection is not available", null);
        }

        LOGGER.info("Database data source '{}' refreshed", getName());
    }
    
    @Override
    public void shutdown() {
        cacheManager.clear();
        preparedQueries.clear();
        connectionStatus = ConnectionStatus.shutdown();
        LOGGER.info("Database data source '{}' shut down", getName());
    }
    
    /**
     * Execute a query based on data type and parameters.
     */
    private Object executeQuery(String dataType, Object... parameters) throws SQLException {
        LOGGER.info("TRACE: DatabaseDataSource.executeQuery called - dataType: {}, parameters: {}", dataType, java.util.Arrays.toString(parameters));
        String query = getQueryForDataType(dataType);
        if (query == null) {
            throw new SQLException("No query defined for data type: " + dataType);
        }
        LOGGER.debug("Resolved query for data type '{}': {}", dataType, query);
        LOGGER.debug("Parameters: {}", java.util.Arrays.toString(parameters));

        try (Connection connection = dataSource.getConnection()) {
            if (parameters.length == 0) {
                // Simple query without parameters
                try (Statement statement = connection.createStatement();
                     ResultSet resultSet = statement.executeQuery(query)) {
                    
                    return resultSet.next() ? mapResultSetToObject(resultSet) : null;
                }
            } else {
                // Parameterized query
                Map<String, Object> paramMap = buildParameterMap(parameters);
                try (PreparedStatement statement = prepareStatement(connection, query, paramMap)) {
                    ResultSet resultSet = statement.executeQuery();
                    return resultSet.next() ? mapResultSetToObject(resultSet) : null;
                }
            }
        }
    }
    
    /**
     * Get the SQL query for a specific data type.
     */
    private String getQueryForDataType(String dataType) {
        if (configuration.getQueries().containsKey(dataType)) {
            return configuration.getQueries().get(dataType);
        }
        return configuration.getQueries().get("default");
    }
    
    /**
     * Build parameter map from array of parameters.
     */
    private Map<String, Object> buildParameterMap(Object... parameters) {
        return JdbcParameterUtils.buildParameterMap(configuration.getParameterNames(), parameters);
    }
    
    /**
     * Prepare a SQL statement with named parameters.
     */
    private PreparedStatement prepareStatement(Connection connection, String query,
                                             Map<String, Object> parameters) throws SQLException {
        LOGGER.info("TRACE: DatabaseDataSource.prepareStatement called - query: {}, parameters: {}", query, parameters);
        LOGGER.debug("DatabaseDataSource.prepareStatement parameters: {}", parameters);
        return JdbcParameterUtils.prepareStatement(connection, query, parameters);
    }
    
    /**
     * Map ResultSet to a generic object (Map).
     */
    private Object mapResultSetToObject(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        Map<String, Object> result = new HashMap<>();
        
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnLabel(i);
            Object value = resultSet.getObject(i);
            result.put(columnName, value);
        }
        
        return result;
    }

}
