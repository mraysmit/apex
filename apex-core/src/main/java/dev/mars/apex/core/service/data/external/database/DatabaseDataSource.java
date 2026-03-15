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
 * @since 2025-07-30
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
        LOGGER.debug("Creating database data source instance for '{}': connection={} ",
            configuration.getName(), connectionSummary(configuration));
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
        LOGGER.info("Initializing database data source '{}': connection={}", sourceName(), connectionSummary(config));
        
        try {
            // Test the connection
            if (testConnection()) {
                this.connectionStatus = ConnectionStatus.connected("Database connection established");
                LOGGER.info("Database data source '{}' initialized successfully: connection={}", sourceName(), connectionSummary(config));

                // Test database connectivity with a generic query
                try (Connection testConn = dataSource.getConnection()) {
                    LOGGER.debug("Testing database connectivity for '{}': jdbcUrl='{}'", 
                        sourceName(), testConn.getMetaData().getURL());

                    // Perform a simple connectivity check without assuming specific tables
                    try (Statement stmt = testConn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT 1")) {
                        if (rs.next()) {
                            LOGGER.debug("Database connectivity check successful for '{}'", sourceName());
                        }
                    } catch (SQLException e) {
                        LOGGER.warn("Database connectivity check failed for '{}': error={}", sourceName(), e.getMessage());
                        LOGGER.debug("Database connectivity check failure for '{}':", sourceName(), e);
                    }
                } catch (SQLException e) {
                    LOGGER.error("Failed to test database connectivity for '{}': error={}", sourceName(), e.getMessage());
                    LOGGER.debug("Database connectivity test failure for '{}':", sourceName(), e);
                }
            } else {
                throw new DataSourceException(DataSourceException.ErrorType.CONNECTION_ERROR,
                    "Failed to establish database connection", null, config.getName(), "initialize", true);
            }
        } catch (Exception e) {
            this.connectionStatus = ConnectionStatus.error("Initialization failed", e);
            LOGGER.error("Failed to initialize database data source '{}': connection={}, error={}",
                sourceName(), connectionSummary(config), e.getMessage());
            LOGGER.debug("Database initialization failure for '{}':", sourceName(), e);
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
            LOGGER.debug("Testing database connection for '{}': connection={}", sourceName(), configuredConnectionSummary());
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            LOGGER.warn("Database connection test failed for '{}': connection={}, error={}",
                sourceName(), configuredConnectionSummary(), e.getMessage());
            LOGGER.debug("Database connection test failure for '{}':", sourceName(), e);
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
        long startTime = System.currentTimeMillis();
        LOGGER.debug("Database getData start for '{}': dataType='{}', parameters={}",
            sourceName(), dataType, Arrays.toString(parameters));
        
        try {
            // Check cache first if enabled
            if (cacheManager.isEnabled()) {
                String cacheKey = cacheManager.generateCacheKey(dataType, parameters);
                LOGGER.debug("Checking database cache for '{}': cacheKey='{}'", sourceName(), cacheKey);
                Object cached = cacheManager.get(cacheKey);
                if (cached != null) {
                    LOGGER.debug("Database cache hit for '{}': cacheKey='{}'", sourceName(), cacheKey);
                    metrics.recordCacheHit();
                    metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
                    return (T) cached;
                }
                LOGGER.debug("Database cache miss for '{}': cacheKey='{}'", sourceName(), cacheKey);
                metrics.recordCacheMiss();
            }

            // Execute database query
            LOGGER.debug("Executing database getData query for '{}': dataType='{}'", sourceName(), dataType);
            Object result = executeQuery(dataType, parameters);

            // Cache the result if caching is enabled
            if (cacheManager.isEnabled() && result != null) {
                String cacheKey = cacheManager.generateCacheKey(dataType, parameters);
                cacheManager.put(cacheKey, result);
                LOGGER.debug("Cached database result for '{}': cacheKey='{}'", sourceName(), cacheKey);
            }

            long executionTime = System.currentTimeMillis() - startTime;
            metrics.recordSuccessfulRequest(executionTime);
            LOGGER.debug("Database getData completed for '{}': dataType='{}', duration={}ms, resultType='{}'",
                sourceName(), dataType, executionTime, result != null ? result.getClass().getSimpleName() : "null");
            return (T) result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            metrics.recordFailedRequest(executionTime);
            LOGGER.error("Failed to get data from database '{}': dataType='{}', parameters={}, duration={}ms, error={}",
                sourceName(), dataType, Arrays.toString(parameters), executionTime, e.getMessage());
            LOGGER.debug("Database getData failure for '{}':", sourceName(), e);
            return null;
        }
    }
    
    @Override
    public <T> List<T> query(String query, Map<String, Object> parameters) throws DataSourceException {
        // Validate inputs
        if (query == null) {
            throw DataSourceException.configurationError("Query cannot be null");
        }
        if (parameters == null) {
            throw DataSourceException.configurationError("Parameters cannot be null");
        }

        long startTime = System.currentTimeMillis();
        LOGGER.debug("Executing database query for '{}': query='{}', parameterCount={}, parameters={}",
            sourceName(), query, parameters.size(), parameters);

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
                LOGGER.debug("Executing DML/DDL statement for '{}': query='{}'", sourceName(), query);
                int updateCount = statement.executeUpdate();
                long executionTime = System.currentTimeMillis() - startTime;

                metrics.recordSuccessfulRequest(executionTime);
                metrics.recordRecordsProcessed(updateCount);

                LOGGER.debug("DML/DDL statement completed for '{}': rowsAffected={}, duration={}ms", sourceName(), updateCount, executionTime);
                // Return empty list for update operations
                return new ArrayList<>();

            } else {
                // Use executeQuery for SELECT statements
                LOGGER.debug("Executing SELECT statement for '{}': query='{}'", sourceName(), query);
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

                LOGGER.debug("SELECT statement completed for '{}': rowsReturned={}, duration={}ms", sourceName(), results.size(), executionTime);
                return results;
            }

        } catch (SQLException e) {
            metrics.recordFailedRequest(System.currentTimeMillis() - startTime);

            // Classify the SQL error to provide better error handling
            SqlErrorClassifier.SqlErrorType errorType = SqlErrorClassifier.classifyError(e);
            String errorDescription = SqlErrorClassifier.getErrorDescription(errorType);

            switch (errorType) {
                case CONFIGURATION_ERROR:
                    LOGGER.error("Database configuration error for '{}': query='{}', error={}", sourceName(), query, e.getMessage());
                    LOGGER.debug("Database query configuration failure for '{}':", sourceName(), e);
                    throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                                                 "Database configuration error: " + errorDescription, e,
                                                 configuration.getName(), "query", false);

                case TRANSIENT_ERROR:
                    LOGGER.warn("Transient database error for '{}': query='{}', error={}", sourceName(), query, e.getMessage());
                    LOGGER.debug("Transient database query failure for '{}':", sourceName(), e);
                    throw new DataSourceException(DataSourceException.ErrorType.CONNECTION_ERROR,
                                                 "Transient database error: " + errorDescription, e,
                                                 configuration.getName(), "query", true); // Retryable

                case DATA_INTEGRITY_VIOLATION:
                case FATAL_ERROR:
                default:
                    LOGGER.error("Database query failed for '{}': query='{}', error={}", sourceName(), query, e.getMessage());
                    LOGGER.debug("Database query failure for '{}':", sourceName(), e);
                    throw DataSourceException.executionError("Database query failed: " + errorDescription, e, "query");
            }
        }
    }
    
    @Override
    public <T> T queryForObject(String query, Map<String, Object> parameters) throws DataSourceException {
        LOGGER.debug("Executing database queryForObject for '{}': query='{}', parameters={}", sourceName(), query, parameters);
        List<T> results = query(query, parameters);
        T result = results.isEmpty() ? null : results.get(0);
        LOGGER.debug("Database queryForObject completed for '{}': query='{}', resultFound={}", sourceName(), query, result != null);
        return result;
    }
    
    @Override
    public <T> List<List<T>> batchQuery(List<String> queries) throws DataSourceException {
        LOGGER.debug("Executing database batch query for '{}': queryCount={}", sourceName(), queries.size());
        List<List<T>> results = new ArrayList<>();

        for (int i = 0; i < queries.size(); i++) {
            String query = queries.get(i);
            LOGGER.debug("Executing database batch query {}/{} for '{}': query='{}'", i + 1, queries.size(), sourceName(), query);
            List<T> queryResult = query(query, Collections.emptyMap());
            results.add(queryResult);
            LOGGER.debug("Database batch query {}/{} completed for '{}': results={}", i + 1, queries.size(), sourceName(), queryResult.size());
        }

        LOGGER.debug("Database batch query completed for '{}': queryCount={}", sourceName(), queries.size());
        return results;
    }
    
    @Override
    public void batchUpdate(List<String> updates) throws DataSourceException {
        LOGGER.debug("Executing database batch update for '{}': statementCount={}", sourceName(), updates.size());
        long startTime = System.currentTimeMillis();

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            LOGGER.debug("Started transaction for database batch update on '{}'", sourceName());

            try (Statement statement = connection.createStatement()) {
                for (int i = 0; i < updates.size(); i++) {
                    String update = updates.get(i);
                    LOGGER.debug("Adding database batch statement {}/{} for '{}': {}", i + 1, updates.size(), sourceName(), update);
                    statement.addBatch(update);
                }

                LOGGER.debug("Executing database batch for '{}': statementCount={}", sourceName(), updates.size());
                int[] updateCounts = statement.executeBatch();
                connection.commit();

                long executionTime = System.currentTimeMillis() - startTime;
                int totalRows = Arrays.stream(updateCounts).sum();

                metrics.recordSuccessfulRequest(executionTime);
                metrics.recordRecordsProcessed(totalRows);

                LOGGER.debug("Database batch update completed for '{}': statements={}, rowsAffected={}, duration={}ms",
                    sourceName(), updates.size(), totalRows, executionTime);

            } catch (SQLException e) {
                LOGGER.debug("Database batch update failed; rolling back transaction for '{}': {}", sourceName(), e.getMessage());
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
                    LOGGER.error("Database configuration error in batch update for '{}': error={}", sourceName(), e.getMessage());
                    LOGGER.debug("Database batch update configuration failure for '{}':", sourceName(), e);
                    throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                                                 "Database configuration error: " + errorDescription, e,
                                                 configuration.getName(), "batchUpdate", false);

                case TRANSIENT_ERROR:
                    LOGGER.warn("Transient database error in batch update for '{}': error={}", sourceName(), e.getMessage());
                    LOGGER.debug("Transient database batch update failure for '{}':", sourceName(), e);
                    throw new DataSourceException(DataSourceException.ErrorType.CONNECTION_ERROR,
                                                 "Transient database error: " + errorDescription, e,
                                                 configuration.getName(), "batchUpdate", true); // Retryable

                case DATA_INTEGRITY_VIOLATION:
                    LOGGER.warn("Data integrity violation in batch update for '{}': error={}", sourceName(), e.getMessage());
                    LOGGER.debug("Database batch update integrity failure for '{}':", sourceName(), e);
                    throw DataSourceException.executionError("Data integrity violation: " + errorDescription, e, "batchUpdate");

                case FATAL_ERROR:
                default:
                    LOGGER.error("Database batch update failed for '{}': error={}", sourceName(), e.getMessage());
                    LOGGER.debug("Database batch update failure for '{}':", sourceName(), e);
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
        LOGGER.info("Refreshing database data source '{}': connection={}", sourceName(), configuredConnectionSummary());
        // Clear cache
        cacheManager.clear();

        // Test connection
        if (!testConnection()) {
            throw DataSourceException.connectionError("Database connection is not available", null);
        }

        LOGGER.info("Database data source '{}' refreshed", sourceName());
    }
    
    @Override
    public void shutdown() {
        LOGGER.info("Shutting down database data source '{}': preparedQueryCount={}", sourceName(), preparedQueries.size());
        cacheManager.clear();
        preparedQueries.clear();
        connectionStatus = ConnectionStatus.shutdown();
        LOGGER.info("Database data source '{}' shut down", sourceName());
    }
    
    /**
     * Execute a query based on data type and parameters.
     */
    private Object executeQuery(String dataType, Object... parameters) throws SQLException {
        LOGGER.debug("Executing database internal query for '{}': dataType='{}', parameters={}",
            sourceName(), dataType, Arrays.toString(parameters));
        String query = getQueryForDataType(dataType);
        if (query == null) {
            throw new SQLException("No query defined for data type: " + dataType);
        }
        LOGGER.debug("Resolved database internal query for '{}': dataType='{}', query='{}'", sourceName(), dataType, query);

        try (Connection connection = dataSource.getConnection()) {
            if (parameters.length == 0) {
                // Simple query without parameters
                try (Statement statement = connection.createStatement();
                     ResultSet resultSet = statement.executeQuery(query)) {
                    
                    List<Object> results = new ArrayList<>();
                    while (resultSet.next()) {
                        results.add(mapResultSetToObject(resultSet));
                    }
                    LOGGER.debug("Database internal query completed for '{}': dataType='{}', rows={}", sourceName(), dataType, results.size());
                    
                    if (results.isEmpty()) return null;
                    if (results.size() == 1) return results.get(0);
                    return results;
                }
            } else {
                // Parameterized query
                Map<String, Object> paramMap = buildParameterMap(parameters);
                try (PreparedStatement statement = prepareStatement(connection, query, paramMap)) {
                    ResultSet resultSet = statement.executeQuery();
                    
                    List<Object> results = new ArrayList<>();
                    while (resultSet.next()) {
                        results.add(mapResultSetToObject(resultSet));
                    }
                    LOGGER.debug("Database internal parameterized query completed for '{}': dataType='{}', rows={}", sourceName(), dataType, results.size());
                    
                    if (results.isEmpty()) return null;
                    if (results.size() == 1) return results.get(0);
                    return results;
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
        LOGGER.debug("Preparing database statement for '{}': query='{}', parameters={}", sourceName(), query, parameters);
        return JdbcParameterUtils.prepareStatement(connection, query, parameters);
    }

    private String sourceName() {
        return configuration != null && configuration.getName() != null ? configuration.getName() : "database-source";
    }

    private String configuredConnectionSummary() {
        return connectionSummary(configuration);
    }

    private String connectionSummary(DataSourceConfiguration config) {
        if (config == null || config.getConnection() == null) {
            return "unconfigured";
        }

        return String.format("host=%s, port=%s, database=%s, schema=%s, user=%s",
            config.getConnection().getHost(),
            config.getConnection().getPort(),
            config.getConnection().getDatabase(),
            config.getConnection().getSchema(),
            config.getConnection().getUsername());
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
