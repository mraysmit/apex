package dev.mars.apex.core.service.data.external;

import dev.mars.apex.core.service.data.DataSource;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;

import java.util.List;
import java.util.Map;

/**
 * Enhanced interface for external data sources that extends the basic DataSource interface
 * with additional capabilities for enterprise-grade data integration.
 * 
 * This interface supports multiple data source types including databases, REST APIs,
 * message queues, file systems, caches, and custom implementations.
 * 
 * Key features:
 * - Health monitoring and metrics collection
 * - Connection management and lifecycle control
 * - Query capabilities with parameter binding
 * - Batch operations for high-throughput scenarios
 * - Configuration-driven initialization
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public interface ExternalDataSource extends DataSource {
    
    /**
     * Get the type of this external data source.
     * 
     * @return The data source type (DATABASE, REST_API, MESSAGE_QUEUE, etc.)
     */
    DataSourceType getSourceType();
    
    /**
     * Get the current connection status of this data source.
     * 
     * @return The current connection status
     */
    ConnectionStatus getConnectionStatus();
    
    /**
     * Get performance and usage metrics for this data source.
     * 
     * @return Current metrics including response times, error rates, cache hit ratios, etc.
     */
    DataSourceMetrics getMetrics();
    
    /**
     * Initialize the data source with the provided configuration.
     * This method should be called before using the data source.
     * 
     * @param config The configuration for this data source
     * @throws DataSourceException if initialization fails
     */
    void initialize(DataSourceConfiguration config) throws DataSourceException;
    
    /**
     * Shutdown the data source and release all resources.
     * This method should be called when the data source is no longer needed.
     */
    void shutdown();
    
    /**
     * Check if the data source is healthy and available.
     * 
     * @return true if the data source is healthy, false otherwise
     */
    boolean isHealthy();
    
    /**
     * Execute a query against the data source and return a list of results.
     * This method is useful for queries that return multiple records.
     * 
     * @param <T> The type of objects to return
     * @param query The query to execute (format depends on data source type)
     * @param parameters Parameters to bind to the query
     * @return List of results, empty list if no results found
     * @throws DataSourceException if query execution fails
     */
    <T> List<T> query(String query, Map<String, Object> parameters) throws DataSourceException;
    
    /**
     * Execute a query against the data source and return a single result.
     * This method is useful for queries that return a single record.
     * 
     * @param <T> The type of object to return
     * @param query The query to execute (format depends on data source type)
     * @param parameters Parameters to bind to the query
     * @return Single result object, null if no result found
     * @throws DataSourceException if query execution fails
     */
    <T> T queryForObject(String query, Map<String, Object> parameters) throws DataSourceException;
    
    /**
     * Execute multiple queries in a batch operation.
     * This method is useful for high-throughput scenarios.
     * 
     * @param <T> The type of objects to return
     * @param queries List of queries to execute
     * @return List of result lists, one for each query
     * @throws DataSourceException if batch execution fails
     */
    <T> List<List<T>> batchQuery(List<String> queries) throws DataSourceException;
    
    /**
     * Execute multiple update operations in a batch.
     * This method is useful for bulk data modifications.
     * 
     * @param updates List of update operations to execute
     * @throws DataSourceException if batch update fails
     */
    void batchUpdate(List<String> updates) throws DataSourceException;
    
    /**
     * Get the configuration used to initialize this data source.
     * 
     * @return The data source configuration
     */
    DataSourceConfiguration getConfiguration();
    
    /**
     * Refresh the data source connection or cache.
     * This method can be used to force a reconnection or cache refresh.
     * 
     * @throws DataSourceException if refresh fails
     */
    void refresh() throws DataSourceException;
    
    /**
     * Test the connection to the data source.
     * This method performs a lightweight test to verify connectivity.
     * 
     * @return true if connection test passes, false otherwise
     */
    boolean testConnection();
}
