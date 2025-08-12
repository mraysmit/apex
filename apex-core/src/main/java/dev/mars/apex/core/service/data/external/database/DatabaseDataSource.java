package dev.mars.apex.core.service.data.external.database;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.*;
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
    
    // Simple in-memory cache for query results
    private final Map<String, CachedResult> resultCache = new ConcurrentHashMap<>();
    
    /**
     * Constructor with DataSource and configuration.
     * 
     * @param dataSource The JDBC DataSource
     * @param configuration The data source configuration
     */
    public DatabaseDataSource(DataSource dataSource, DataSourceConfiguration configuration) {
        this.dataSource = dataSource;
        this.configuration = configuration;
        this.connectionStatus = ConnectionStatus.notInitialized();
        this.metrics = new DataSourceMetrics();
        this.healthIndicator = new DatabaseHealthIndicator(dataSource, configuration);
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
        long startTime = System.currentTimeMillis();
        
        try {
            // Check cache first if enabled
            if (isCacheEnabled()) {
                String cacheKey = generateCacheKey(dataType, parameters);
                CachedResult cached = resultCache.get(cacheKey);
                if (cached != null && !cached.isExpired()) {
                    metrics.recordCacheHit();
                    metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
                    return (T) cached.getData();
                }
                metrics.recordCacheMiss();
            }
            
            // Execute database query
            Object result = executeQuery(dataType, parameters);
            
            // Cache the result if caching is enabled
            if (isCacheEnabled() && result != null) {
                String cacheKey = generateCacheKey(dataType, parameters);
                long ttl = configuration.getCache().getTtlSeconds() * 1000L;
                resultCache.put(cacheKey, new CachedResult(result, System.currentTimeMillis() + ttl));
            }
            
            metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
            return (T) result;
            
        } catch (Exception e) {
            metrics.recordFailedRequest(System.currentTimeMillis() - startTime);
            LOGGER.error("Failed to get data from database", e);
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
            throw new NullPointerException("Parameters cannot be null");
        }

        long startTime = System.currentTimeMillis();

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
                int updateCount = statement.executeUpdate();
                metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
                metrics.recordRecordsProcessed(updateCount);

                // Return empty list for update operations
                return new ArrayList<>();

            } else {
                // Use executeQuery for SELECT statements
                ResultSet resultSet = statement.executeQuery();
                List<T> results = new ArrayList<>();

                while (resultSet.next()) {
                    @SuppressWarnings("unchecked")
                    T result = (T) mapResultSetToObject(resultSet);
                    results.add(result);
                }

                metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
                metrics.recordRecordsProcessed(results.size());

                return results;
            }

        } catch (SQLException e) {
            metrics.recordFailedRequest(System.currentTimeMillis() - startTime);
            throw DataSourceException.executionError("Database query failed", e, "query");
        }
    }
    
    @Override
    public <T> T queryForObject(String query, Map<String, Object> parameters) throws DataSourceException {
        List<T> results = query(query, parameters);
        return results.isEmpty() ? null : results.get(0);
    }
    
    @Override
    public <T> List<List<T>> batchQuery(List<String> queries) throws DataSourceException {
        List<List<T>> results = new ArrayList<>();
        
        for (String query : queries) {
            List<T> queryResult = query(query, Collections.emptyMap());
            results.add(queryResult);
        }
        
        return results;
    }
    
    @Override
    public void batchUpdate(List<String> updates) throws DataSourceException {
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            
            try (Statement statement = connection.createStatement()) {
                for (String update : updates) {
                    statement.addBatch(update);
                }
                
                int[] updateCounts = statement.executeBatch();
                connection.commit();
                
                metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
                metrics.recordRecordsProcessed(Arrays.stream(updateCounts).sum());
                
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            metrics.recordFailedRequest(System.currentTimeMillis() - startTime);
            throw DataSourceException.executionError("Batch update failed", e, "batchUpdate");
        }
    }
    
    @Override
    public DataSourceConfiguration getConfiguration() {
        return configuration;
    }
    
    @Override
    public void refresh() throws DataSourceException {
        // Clear cache
        resultCache.clear();
        
        // Test connection
        if (!testConnection()) {
            throw DataSourceException.connectionError("Database connection is not available", null);
        }
        
        LOGGER.info("Database data source '{}' refreshed", getName());
    }
    
    @Override
    public void shutdown() {
        resultCache.clear();
        preparedQueries.clear();
        connectionStatus = ConnectionStatus.shutdown();
        LOGGER.info("Database data source '{}' shut down", getName());
    }
    
    /**
     * Execute a query based on data type and parameters.
     */
    private Object executeQuery(String dataType, Object... parameters) throws SQLException {
        String query = getQueryForDataType(dataType);
        if (query == null) {
            throw new SQLException("No query defined for data type: " + dataType);
        }
        
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
        Map<String, Object> paramMap = new HashMap<>();
        String[] paramNames = configuration.getParameterNames();
        
        if (paramNames != null) {
            for (int i = 0; i < parameters.length && i < paramNames.length; i++) {
                paramMap.put(paramNames[i], parameters[i]);
            }
        } else {
            // Use generic parameter names
            for (int i = 0; i < parameters.length; i++) {
                paramMap.put("param" + (i + 1), parameters[i]);
            }
        }
        
        return paramMap;
    }
    
    /**
     * Prepare a SQL statement with named parameters.
     */
    private PreparedStatement prepareStatement(Connection connection, String query,
                                             Map<String, Object> parameters) throws SQLException {
        // Validate inputs
        if (query == null) {
            throw new SQLException("Query cannot be null");
        }
        if (parameters == null) {
            throw new SQLException("Parameters cannot be null");
        }

        // Better implementation - process parameters in order they appear in SQL
        String processedQuery = query;
        List<Object> paramValues = new ArrayList<>();

        // Find all parameter placeholders in order
        int searchIndex = 0;
        while (searchIndex < processedQuery.length()) {
            int colonIndex = processedQuery.indexOf(':', searchIndex);
            if (colonIndex == -1) break;

            // Find the end of the parameter name
            int endIndex = colonIndex + 1;
            while (endIndex < processedQuery.length() &&
                   (Character.isLetterOrDigit(processedQuery.charAt(endIndex)) ||
                    processedQuery.charAt(endIndex) == '_')) {
                endIndex++;
            }

            String paramName = processedQuery.substring(colonIndex + 1, endIndex);
            if (parameters.containsKey(paramName)) {
                // Replace this occurrence with ?
                processedQuery = processedQuery.substring(0, colonIndex) + "?" +
                               processedQuery.substring(endIndex);
                paramValues.add(parameters.get(paramName));
                searchIndex = colonIndex + 1;
            } else {
                searchIndex = endIndex;
            }
        }

        PreparedStatement statement = connection.prepareStatement(processedQuery);

        // Set parameter values
        for (int i = 0; i < paramValues.size(); i++) {
            statement.setObject(i + 1, paramValues.get(i));
        }

        return statement;
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
    
    /**
     * Check if caching is enabled.
     */
    private boolean isCacheEnabled() {
        return configuration.getCache() != null && configuration.getCache().isEnabled();
    }
    
    /**
     * Generate cache key for the given data type and parameters.
     */
    private String generateCacheKey(String dataType, Object... parameters) {
        StringBuilder key = new StringBuilder(dataType);
        for (Object param : parameters) {
            key.append(":").append(param != null ? param.toString() : "null");
        }
        return key.toString();
    }
    
    /**
     * Simple cached result holder.
     */
    private static class CachedResult {
        private final Object data;
        private final long expiryTime;
        
        public CachedResult(Object data, long expiryTime) {
            this.data = data;
            this.expiryTime = expiryTime;
        }
        
        public Object getData() {
            return data;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
