package dev.mars.apex.core.service.data.external.cache;

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


import java.util.List;

/**
 * Interface for cache management operations.
 * 
 * This interface defines the contract for cache managers that handle
 * storage, retrieval, and management of cached data.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
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
