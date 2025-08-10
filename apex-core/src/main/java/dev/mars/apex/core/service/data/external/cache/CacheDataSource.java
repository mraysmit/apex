package dev.mars.apex.core.service.data.external.cache;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Cache implementation of ExternalDataSource.
 * 
 * This class provides cache-based data access with support for multiple cache
 * backends, TTL management, eviction policies, and distributed caching.
 * 
 * Supported cache types:
 * - In-memory cache (default)
 * - Redis (if available)
 * - Hazelcast (if available)
 * - Custom cache implementations
 * 
 * Features:
 * - Multiple eviction policies (LRU, LFU, FIFO, TTL-based)
 * - TTL and max idle time support
 * - Cache statistics and monitoring
 * - Key pattern matching
 * - Batch operations
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public class CacheDataSource implements ExternalDataSource {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheDataSource.class);
    
    private DataSourceConfiguration configuration;
    private ConnectionStatus connectionStatus;
    private DataSourceMetrics metrics;
    private CacheManager cacheManager;
    
    /**
     * Constructor with configuration.
     * 
     * @param configuration The data source configuration
     */
    public CacheDataSource(DataSourceConfiguration configuration) {
        this.configuration = configuration;
        this.connectionStatus = ConnectionStatus.notInitialized();
        this.metrics = new DataSourceMetrics();
    }
    
    @Override
    public void initialize(DataSourceConfiguration config) throws DataSourceException {
        this.configuration = config;
        this.connectionStatus = ConnectionStatus.connecting();
        
        try {
            // Initialize cache manager
            this.cacheManager = createCacheManager(config);
            
            // Test the cache
            if (testConnection()) {
                this.connectionStatus = ConnectionStatus.connected("Cache data source initialized");
                LOGGER.info("Cache data source '{}' initialized successfully", config.getName());
            } else {
                throw new DataSourceException(DataSourceException.ErrorType.CONNECTION_ERROR,
                    "Failed to establish cache connection", null, config.getName(), "initialize", true);
            }
        } catch (Exception e) {
            this.connectionStatus = ConnectionStatus.error("Initialization failed", e);
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to initialize cache data source", e, config.getName(), "initialize", false);
        }
    }
    
    @Override
    public DataSourceType getSourceType() {
        return DataSourceType.CACHE;
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
        return cacheManager != null && cacheManager.isHealthy();
    }
    
    @Override
    public boolean testConnection() {
        try {
            if (cacheManager == null) {
                return false;
            }
            
            // Test cache with a simple put/get operation
            String testKey = "_health_check_" + System.currentTimeMillis();
            String testValue = "test";
            
            cacheManager.put(testKey, testValue);
            Object retrieved = cacheManager.get(testKey);
            cacheManager.remove(testKey);
            
            return testValue.equals(retrieved);
            
        } catch (Exception e) {
            LOGGER.warn("Cache connection test failed for '{}'", configuration.getName(), e);
            return false;
        }
    }
    
    @Override
    public String getName() {
        return configuration != null ? configuration.getName() : "cache-source";
    }
    
    @Override
    public String getDataType() {
        return configuration != null ? configuration.getSourceType() : "cache";
    }
    
    @Override
    public boolean supportsDataType(String dataType) {
        return "cache".equals(dataType) || 
               (configuration != null && configuration.getSourceType().equals(dataType));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(String dataType, Object... parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            String key = buildCacheKey(dataType, parameters);
            Object result = cacheManager.get(key);
            
            if (result != null) {
                metrics.recordCacheHit();
                metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
                return (T) result;
            } else {
                metrics.recordCacheMiss();
                metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
                return null;
            }
            
        } catch (Exception e) {
            metrics.recordFailedRequest(System.currentTimeMillis() - startTime);
            LOGGER.error("Failed to get data from cache", e);
            return null;
        }
    }
    
    @Override
    public <T> List<T> query(String query, Map<String, Object> parameters) throws DataSourceException {
        // For caches, the "query" is typically a key pattern
        try {
            List<String> matchingKeys = cacheManager.getKeysByPattern(query);
            List<T> results = new ArrayList<>();
            
            for (String key : matchingKeys) {
                @SuppressWarnings("unchecked")
                T value = (T) cacheManager.get(key);
                if (value != null) {
                    results.add(value);
                }
            }
            
            metrics.recordRecordsProcessed(results.size());
            return results;
            
        } catch (Exception e) {
            throw DataSourceException.executionError("Cache query failed", e, "query");
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
        // For caches, updates are typically key-value pairs
        // This is a simplified implementation
        for (String update : updates) {
            try {
                // Parse update string (format: "key=value")
                String[] parts = update.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    cacheManager.put(key, value);
                } else {
                    LOGGER.warn("Invalid cache update format: {}", update);
                }
            } catch (Exception e) {
                throw DataSourceException.executionError("Cache batch update failed", e, "batchUpdate");
            }
        }
    }
    
    @Override
    public DataSourceConfiguration getConfiguration() {
        return configuration;
    }
    
    @Override
    public void refresh() throws DataSourceException {
        // For caches, refresh might mean clearing expired entries
        if (cacheManager != null) {
            cacheManager.evictExpired();
        }
        
        LOGGER.info("Cache data source '{}' refreshed", getName());
    }
    
    @Override
    public void shutdown() {
        if (cacheManager != null) {
            cacheManager.shutdown();
        }
        connectionStatus = ConnectionStatus.shutdown();
        LOGGER.info("Cache data source '{}' shut down", getName());
    }
    
    /**
     * Put a value into the cache.
     * 
     * @param key The cache key
     * @param value The value to cache
     */
    public void put(String key, Object value) {
        if (cacheManager != null) {
            cacheManager.put(key, value);
        }
    }
    
    /**
     * Put a value into the cache with TTL.
     * 
     * @param key The cache key
     * @param value The value to cache
     * @param ttlSeconds Time-to-live in seconds
     */
    public void put(String key, Object value, long ttlSeconds) {
        if (cacheManager != null) {
            cacheManager.put(key, value, ttlSeconds);
        }
    }
    
    /**
     * Get a value from the cache.
     * 
     * @param key The cache key
     * @return The cached value, or null if not found
     */
    public Object get(String key) {
        return cacheManager != null ? cacheManager.get(key) : null;
    }
    
    /**
     * Remove a value from the cache.
     * 
     * @param key The cache key
     * @return true if the key was removed
     */
    public boolean remove(String key) {
        return cacheManager != null && cacheManager.remove(key);
    }
    
    /**
     * Check if a key exists in the cache.
     * 
     * @param key The cache key
     * @return true if the key exists
     */
    public boolean containsKey(String key) {
        return cacheManager != null && cacheManager.containsKey(key);
    }
    
    /**
     * Get all keys matching a pattern.
     * 
     * @param pattern The key pattern (supports wildcards)
     * @return List of matching keys
     */
    public List<String> getKeysByPattern(String pattern) {
        return cacheManager != null ? cacheManager.getKeysByPattern(pattern) : Collections.emptyList();
    }
    
    /**
     * Get cache statistics.
     * 
     * @return Cache statistics
     */
    public CacheStatistics getStatistics() {
        return cacheManager != null ? cacheManager.getStatistics() : new CacheStatistics();
    }
    
    /**
     * Clear all entries from the cache.
     */
    public void clear() {
        if (cacheManager != null) {
            cacheManager.clear();
        }
    }
    
    /**
     * Get the current cache size.
     * 
     * @return Number of entries in the cache
     */
    public int size() {
        return cacheManager != null ? cacheManager.size() : 0;
    }
    
    /**
     * Create a cache manager based on configuration.
     */
    private CacheManager createCacheManager(DataSourceConfiguration config) throws DataSourceException {
        String sourceType = config.getSourceType();
        
        if (sourceType == null) {
            sourceType = "memory"; // Default to in-memory cache
        }
        
        switch (sourceType.toLowerCase()) {
            case "memory":
            case "in-memory":
                return new InMemoryCacheManager(config);
                
            case "redis":
                return createRedisCacheManager(config);
                
            case "hazelcast":
                return createHazelcastCacheManager(config);
                
            default:
                throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                    "Unsupported cache type: " + sourceType);
        }
    }
    
    /**
     * Create Redis cache manager if Redis is available.
     */
    private CacheManager createRedisCacheManager(DataSourceConfiguration config) throws DataSourceException {
        try {
            // Check if Redis client is available
            Class.forName("redis.clients.jedis.Jedis");
            
            // Use reflection to create Redis cache manager to avoid hard dependency
            Class<?> redisManagerClass = Class.forName(
                "dev.mars.apex.core.service.data.external.cache.RedisCacheManager");
            
            return (CacheManager) redisManagerClass.getConstructor(DataSourceConfiguration.class)
                .newInstance(config);
                
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Redis client not available, falling back to in-memory cache");
            return new InMemoryCacheManager(config);
        } catch (Exception e) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to create Redis cache manager", e);
        }
    }
    
    /**
     * Create Hazelcast cache manager if Hazelcast is available.
     */
    private CacheManager createHazelcastCacheManager(DataSourceConfiguration config) throws DataSourceException {
        try {
            // Check if Hazelcast is available
            Class.forName("com.hazelcast.core.Hazelcast");
            
            // Use reflection to create Hazelcast cache manager to avoid hard dependency
            Class<?> hazelcastManagerClass = Class.forName(
                "dev.mars.apex.core.service.data.external.cache.HazelcastCacheManager");
            
            return (CacheManager) hazelcastManagerClass.getConstructor(DataSourceConfiguration.class)
                .newInstance(config);
                
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Hazelcast not available, falling back to in-memory cache");
            return new InMemoryCacheManager(config);
        } catch (Exception e) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to create Hazelcast cache manager", e);
        }
    }
    
    /**
     * Build cache key from data type and parameters.
     */
    private String buildCacheKey(String dataType, Object... parameters) {
        StringBuilder key = new StringBuilder();
        
        // Add key prefix if configured
        if (configuration.getCache() != null && configuration.getCache().getKeyPrefix() != null) {
            key.append(configuration.getCache().getKeyPrefix()).append(":");
        }
        
        key.append(dataType);
        
        for (Object param : parameters) {
            key.append(":").append(param != null ? param.toString() : "null");
        }
        
        return key.toString();
    }
}
