package dev.mars.apex.core.cache;

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

import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.cache.CacheManager;
import dev.mars.apex.core.service.data.external.cache.CacheStatistics;
import dev.mars.apex.core.service.data.external.cache.InMemoryCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified cache manager facade for APEX.
 *
 * <p>This class provides a single entry point for all caching in APEX, managing
 * multiple cache scopes with different configurations:</p>
 *
 * <p><b>Cache Scopes and TTL Rationale:</b></p>
 * <ul>
 *   <li><b>Dataset Cache</b> (TTL: 2h, Size: 1000): Caches DatasetLookupService instances
 *       by content signature to deduplicate identical datasets.
 *       <i>Why 2 hours?</i> Balances memory efficiency with reload frequency for datasets
 *       that may change during development.</li>
 *
 *   <li><b>Lookup Result Cache</b> (TTL: 5m, Size: 10000): Caches lookup results.
 *       <i>Why 5 minutes?</i> Short TTL ensures data freshness for frequently changing
 *       reference data while providing performance benefits.</li>
 *
 *   <li><b>Expression Cache</b> (TTL: 24h, Size: 1000): Caches compiled SpEL expressions.
 *       <i>Why 24 hours?</i> Expressions are immutable strings. Long TTL maximizes
 *       performance while allowing daily cleanup.</li>
 *
 *   <li><b>Service Registry Cache</b> (TTL: 24h, Size: 500): Caches service instances.
 *       <i>Why 24 hours?</i> Services are stable during runtime. Long TTL ensures
 *       fast lookups.</li>
 * </ul>
 *
 * <p>All caches use the production-ready {@link CacheManager} infrastructure with
 * LRU eviction, statistics tracking, and thread-safe operations.</p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class ApexCacheManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ApexCacheManager.class);
    
    // Cache scope names
    public static final String DATASET_CACHE = "dataset";
    public static final String LOOKUP_RESULT_CACHE = "lookup-result";
    public static final String EXPRESSION_CACHE = "expression";
    public static final String SERVICE_REGISTRY_CACHE = "service-registry";
    
    // Singleton instance
    private static volatile ApexCacheManager instance;
    
    // Cache managers for each scope
    private final Map<String, CacheManager> cacheManagers;
    
    // Configuration
    private final CacheConfig globalConfig;
    
    /**
     * Private constructor - use getInstance() instead.
     */
    private ApexCacheManager(CacheConfig globalConfig) {
        this.globalConfig = globalConfig != null ? globalConfig : createDefaultConfig();
        this.cacheManagers = new ConcurrentHashMap<>();
        
        // Initialize all cache scopes
        initializeCaches();
        
        LOGGER.info("ApexCacheManager initialized with {} cache scopes", cacheManagers.size());
    }
    
    /**
     * Get the singleton instance with default configuration.
     */
    public static ApexCacheManager getInstance() {
        return getInstance(null);
    }
    
    /**
     * Get the singleton instance with custom configuration.
     * 
     * @param config Global cache configuration (null for defaults)
     */
    public static ApexCacheManager getInstance(CacheConfig config) {
        if (instance == null) {
            synchronized (ApexCacheManager.class) {
                if (instance == null) {
                    instance = new ApexCacheManager(config);
                }
            }
        }
        return instance;
    }
    
    /**
     * Reset the singleton instance (for testing).
     */
    public static void resetInstance() {
        synchronized (ApexCacheManager.class) {
            if (instance != null) {
                instance.shutdown();
                instance = null;
            }
        }
    }
    
    /**
     * Initialize all cache scopes.
     */
    private void initializeCaches() {
        cacheManagers.put(DATASET_CACHE, createDatasetCache());
        cacheManagers.put(LOOKUP_RESULT_CACHE, createLookupResultCache());
        cacheManagers.put(EXPRESSION_CACHE, createExpressionCache());
        cacheManagers.put(SERVICE_REGISTRY_CACHE, createServiceRegistryCache());
    }
    
    /**
     * Create dataset cache (TTL: 2h, Size: 1000, LRU).
     */
    private CacheManager createDatasetCache() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("apex-dataset-cache");
        config.setType("cache");
        config.setSourceType("memory");
        
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(7200L);      // 2 hours
        cacheConfig.setMaxSize(1000);          // 1000 datasets
        cacheConfig.setStatisticsEnabled(true);
        cacheConfig.setKeyPrefix("ds:");
        
        config.setCache(cacheConfig);
        return new InMemoryCacheManager(config);
    }
    
    /**
     * Create lookup result cache (TTL: 5m, Size: 10000, LRU).
     */
    private CacheManager createLookupResultCache() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("apex-lookup-result-cache");
        config.setType("cache");
        config.setSourceType("memory");
        
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(300L);       // 5 minutes
        cacheConfig.setMaxSize(10000);         // 10000 results
        cacheConfig.setStatisticsEnabled(true);
        cacheConfig.setKeyPrefix("lr:");
        
        config.setCache(cacheConfig);
        return new InMemoryCacheManager(config);
    }
    
    /**
     * Create expression cache (TTL: 24h, Size: 1000, LRU).
     * Long TTL since expressions rarely change.
     */
    private CacheManager createExpressionCache() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("apex-expression-cache");
        config.setType("cache");
        config.setSourceType("memory");

        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(86400L);     // 24 hours
        cacheConfig.setMaxSize(1000);          // 1000 expressions
        cacheConfig.setStatisticsEnabled(true);
        cacheConfig.setKeyPrefix("ex:");

        config.setCache(cacheConfig);
        return new InMemoryCacheManager(config);
    }

    /**
     * Create service registry cache (TTL: 24h, Size: 500, LRU).
     * Long TTL since services rarely change.
     */
    private CacheManager createServiceRegistryCache() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("apex-service-registry-cache");
        config.setType("cache");
        config.setSourceType("memory");

        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(86400L);     // 24 hours
        cacheConfig.setMaxSize(500);           // 500 services
        cacheConfig.setStatisticsEnabled(true);
        cacheConfig.setKeyPrefix("sr:");

        config.setCache(cacheConfig);
        return new InMemoryCacheManager(config);
    }
    
    /**
     * Create default global configuration.
     */
    private CacheConfig createDefaultConfig() {
        CacheConfig config = new CacheConfig();
        config.setEnabled(true);
        config.setStatisticsEnabled(true);
        return config;
    }
    
    // ========================================
    // Public API - Scoped Cache Operations
    // ========================================
    
    /**
     * Put a value into a specific cache scope.
     */
    public void put(String scope, String key, Object value) {
        CacheManager cache = cacheManagers.get(scope);
        if (cache != null) {
            cache.put(key, value);
        } else {
            LOGGER.warn("Unknown cache scope: {}", scope);
        }
    }
    
    /**
     * Put a value into a specific cache scope with custom TTL.
     */
    public void put(String scope, String key, Object value, long ttlSeconds) {
        CacheManager cache = cacheManagers.get(scope);
        if (cache != null) {
            cache.put(key, value, ttlSeconds);
        } else {
            LOGGER.warn("Unknown cache scope: {}", scope);
        }
    }
    
    /**
     * Get a value from a specific cache scope.
     */
    public Object get(String scope, String key) {
        CacheManager cache = cacheManagers.get(scope);
        if (cache != null) {
            return cache.get(key);
        } else {
            LOGGER.warn("Unknown cache scope: {}", scope);
            return null;
        }
    }
    
    /**
     * Remove a value from a specific cache scope.
     */
    public boolean remove(String scope, String key) {
        CacheManager cache = cacheManagers.get(scope);
        if (cache != null) {
            return cache.remove(key);
        } else {
            LOGGER.warn("Unknown cache scope: {}", scope);
            return false;
        }
    }
    
    /**
     * Check if a key exists in a specific cache scope.
     */
    public boolean containsKey(String scope, String key) {
        CacheManager cache = cacheManagers.get(scope);
        if (cache != null) {
            return cache.containsKey(key);
        } else {
            LOGGER.warn("Unknown cache scope: {}", scope);
            return false;
        }
    }
    
    /**
     * Get the size of a specific cache scope.
     */
    public int size(String scope) {
        CacheManager cache = cacheManagers.get(scope);
        return cache != null ? cache.size() : 0;
    }
    
    /**
     * Clear a specific cache scope.
     */
    public void clearScope(String scope) {
        CacheManager cache = cacheManagers.get(scope);
        if (cache != null) {
            cache.clear();
            LOGGER.info("Cleared cache scope: {}", scope);
        } else {
            LOGGER.warn("Unknown cache scope: {}", scope);
        }
    }
    
    /**
     * Clear all cache scopes.
     */
    public void clearAll() {
        for (Map.Entry<String, CacheManager> entry : cacheManagers.entrySet()) {
            entry.getValue().clear();
        }
        LOGGER.info("Cleared all cache scopes");
    }
    
    // ========================================
    // Statistics
    // ========================================
    
    /**
     * Get statistics for a specific cache scope.
     */
    public CacheStatistics getStatistics(String scope) {
        CacheManager cache = cacheManagers.get(scope);
        return cache != null ? cache.getStatistics() : null;
    }
    
    /**
     * Get statistics for all cache scopes.
     */
    public Map<String, CacheStatistics> getAllStatistics() {
        Map<String, CacheStatistics> stats = new HashMap<>();
        for (Map.Entry<String, CacheManager> entry : cacheManagers.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().getStatistics());
        }
        return stats;
    }
    
    /**
     * Get a summary of all cache statistics.
     */
    public String getStatisticsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("APEX Cache Statistics:\n");
        
        for (Map.Entry<String, CacheManager> entry : cacheManagers.entrySet()) {
            String scope = entry.getKey();
            CacheStatistics stats = entry.getValue().getStatistics();
            
            sb.append(String.format("  %s: size=%d, hits=%d, misses=%d, hitRate=%.2f%%\n",
                scope,
                entry.getValue().size(),
                stats.getHits(),
                stats.getMisses(),
                stats.getHitRate() * 100));
        }
        
        return sb.toString();
    }
    
    // ========================================
    // Lifecycle
    // ========================================
    
    /**
     * Shutdown all cache managers and release resources.
     */
    public void shutdown() {
        for (Map.Entry<String, CacheManager> entry : cacheManagers.entrySet()) {
            try {
                entry.getValue().shutdown();
                LOGGER.info("Shutdown cache scope: {}", entry.getKey());
            } catch (Exception e) {
                LOGGER.error("Error shutting down cache scope: {}", entry.getKey(), e);
            }
        }
        cacheManagers.clear();
        LOGGER.info("ApexCacheManager shutdown complete");
    }
}

