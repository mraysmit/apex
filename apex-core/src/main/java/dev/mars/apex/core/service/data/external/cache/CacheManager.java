package dev.mars.apex.core.service.data.external.cache;

import java.util.List;

/**
 * Interface for cache management operations.
 * 
 * This interface defines the contract for cache managers that handle
 * storage, retrieval, and management of cached data.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public interface CacheManager {
    
    /**
     * Put a value into the cache.
     * 
     * @param key The cache key
     * @param value The value to cache
     */
    void put(String key, Object value);
    
    /**
     * Put a value into the cache with TTL.
     * 
     * @param key The cache key
     * @param value The value to cache
     * @param ttlSeconds Time-to-live in seconds
     */
    void put(String key, Object value, long ttlSeconds);
    
    /**
     * Get a value from the cache.
     * 
     * @param key The cache key
     * @return The cached value, or null if not found or expired
     */
    Object get(String key);
    
    /**
     * Remove a value from the cache.
     * 
     * @param key The cache key
     * @return true if the key was removed, false if it didn't exist
     */
    boolean remove(String key);
    
    /**
     * Check if a key exists in the cache.
     * 
     * @param key The cache key
     * @return true if the key exists and is not expired
     */
    boolean containsKey(String key);
    
    /**
     * Get all keys matching a pattern.
     * 
     * @param pattern The key pattern (supports wildcards like * and ?)
     * @return List of matching keys
     */
    List<String> getKeysByPattern(String pattern);
    
    /**
     * Get all keys in the cache.
     * 
     * @return List of all keys
     */
    List<String> getAllKeys();
    
    /**
     * Get the current cache size.
     * 
     * @return Number of entries in the cache
     */
    int size();
    
    /**
     * Clear all entries from the cache.
     */
    void clear();
    
    /**
     * Evict expired entries from the cache.
     */
    void evictExpired();
    
    /**
     * Check if the cache manager is healthy.
     * 
     * @return true if the cache is operational
     */
    boolean isHealthy();
    
    /**
     * Get cache statistics.
     * 
     * @return Cache statistics
     */
    CacheStatistics getStatistics();
    
    /**
     * Shutdown the cache manager and release resources.
     */
    void shutdown();
}
