package dev.mars.apex.core.config.datasource;

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


import java.util.Objects;

/**
 * Configuration class for caching settings.
 * 
 * This class contains caching-related configuration including TTL,
 * cache size limits, eviction policies, and cache-specific settings.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class CacheConfig {
    
    /**
     * Enumeration of cache eviction policies.
     */
    public enum EvictionPolicy {
        LRU("LRU", "Least Recently Used"),
        LFU("LFU", "Least Frequently Used"),
        FIFO("FIFO", "First In, First Out"),
        RANDOM("RANDOM", "Random eviction"),
        TTL_BASED("TTL_BASED", "Time-to-live based");
        
        private final String code;
        private final String description;
        
        EvictionPolicy(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private Boolean enabled = true;
    private Long ttlSeconds = 3600L; // 1 hour default
    private Long maxIdleSeconds = 1800L; // 30 minutes default
    private Integer maxSize = 10000;
    private EvictionPolicy evictionPolicy = EvictionPolicy.LRU;
    private Boolean preloadEnabled = false;
    private Boolean refreshAhead = false;
    private Long refreshAheadFactor = 75L; // Refresh when 75% of TTL has passed
    private Boolean statisticsEnabled = true;
    private String keyPrefix;
    private Boolean compressionEnabled = false;
    private String serializationFormat = "json";
    
    // Cache warming configuration
    private Boolean warmupEnabled = false;
    private Integer warmupBatchSize = 100;
    private Long warmupDelay = 0L;
    
    // Distributed cache configuration
    private Boolean distributedCache = false;
    private String cacheCluster;
    private Integer replicationFactor = 1;
    
    /**
     * Default constructor with sensible defaults.
     */
    public CacheConfig() {
        // Defaults are set in field declarations
    }
    
    /**
     * Constructor with basic cache configuration.
     * 
     * @param enabled Whether caching is enabled
     * @param ttlSeconds Time-to-live in seconds
     * @param maxSize Maximum cache size
     */
    public CacheConfig(Boolean enabled, Long ttlSeconds, Integer maxSize) {
        this.enabled = enabled;
        this.ttlSeconds = ttlSeconds;
        this.maxSize = maxSize;
    }
    
    // Basic cache configuration
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled != null && enabled;
    }
    
    public Long getTtlSeconds() {
        return ttlSeconds;
    }
    
    public void setTtlSeconds(Long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
    
    public Long getMaxIdleSeconds() {
        return maxIdleSeconds;
    }
    
    public void setMaxIdleSeconds(Long maxIdleSeconds) {
        this.maxIdleSeconds = maxIdleSeconds;
    }
    
    public Integer getMaxSize() {
        return maxSize;
    }
    
    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }
    
    public EvictionPolicy getEvictionPolicy() {
        return evictionPolicy;
    }
    
    public void setEvictionPolicy(EvictionPolicy evictionPolicy) {
        this.evictionPolicy = evictionPolicy;
    }
    
    // Advanced cache configuration
    
    public Boolean getPreloadEnabled() {
        return preloadEnabled;
    }
    
    public void setPreloadEnabled(Boolean preloadEnabled) {
        this.preloadEnabled = preloadEnabled;
    }
    
    public boolean isPreloadEnabled() {
        return preloadEnabled != null && preloadEnabled;
    }
    
    public Boolean getRefreshAhead() {
        return refreshAhead;
    }
    
    public void setRefreshAhead(Boolean refreshAhead) {
        this.refreshAhead = refreshAhead;
    }
    
    public boolean isRefreshAheadEnabled() {
        return refreshAhead != null && refreshAhead;
    }
    
    public Long getRefreshAheadFactor() {
        return refreshAheadFactor;
    }
    
    public void setRefreshAheadFactor(Long refreshAheadFactor) {
        this.refreshAheadFactor = refreshAheadFactor;
    }
    
    public Boolean getStatisticsEnabled() {
        return statisticsEnabled;
    }
    
    public void setStatisticsEnabled(Boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
    }
    
    public boolean isStatisticsEnabled() {
        return statisticsEnabled != null && statisticsEnabled;
    }
    
    public String getKeyPrefix() {
        return keyPrefix;
    }
    
    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
    
    public Boolean getCompressionEnabled() {
        return compressionEnabled;
    }
    
    public void setCompressionEnabled(Boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }
    
    public boolean isCompressionEnabled() {
        return compressionEnabled != null && compressionEnabled;
    }
    
    public String getSerializationFormat() {
        return serializationFormat;
    }
    
    public void setSerializationFormat(String serializationFormat) {
        this.serializationFormat = serializationFormat;
    }
    
    // Cache warming configuration
    
    public Boolean getWarmupEnabled() {
        return warmupEnabled;
    }
    
    public void setWarmupEnabled(Boolean warmupEnabled) {
        this.warmupEnabled = warmupEnabled;
    }
    
    public boolean isWarmupEnabled() {
        return warmupEnabled != null && warmupEnabled;
    }
    
    public Integer getWarmupBatchSize() {
        return warmupBatchSize;
    }
    
    public void setWarmupBatchSize(Integer warmupBatchSize) {
        this.warmupBatchSize = warmupBatchSize;
    }
    
    public Long getWarmupDelay() {
        return warmupDelay;
    }
    
    public void setWarmupDelay(Long warmupDelay) {
        this.warmupDelay = warmupDelay;
    }
    
    // Distributed cache configuration
    
    public Boolean getDistributedCache() {
        return distributedCache;
    }
    
    public void setDistributedCache(Boolean distributedCache) {
        this.distributedCache = distributedCache;
    }
    
    public boolean isDistributedCache() {
        return distributedCache != null && distributedCache;
    }
    
    public String getCacheCluster() {
        return cacheCluster;
    }
    
    public void setCacheCluster(String cacheCluster) {
        this.cacheCluster = cacheCluster;
    }
    
    public Integer getReplicationFactor() {
        return replicationFactor;
    }
    
    public void setReplicationFactor(Integer replicationFactor) {
        this.replicationFactor = replicationFactor;
    }
    
    // Utility methods
    
    /**
     * Get TTL in milliseconds.
     * 
     * @return TTL in milliseconds
     */
    public long getTtlMilliseconds() {
        return ttlSeconds != null ? ttlSeconds * 1000L : 0L;
    }
    
    /**
     * Get max idle time in milliseconds.
     * 
     * @return Max idle time in milliseconds
     */
    public long getMaxIdleMilliseconds() {
        return maxIdleSeconds != null ? maxIdleSeconds * 1000L : 0L;
    }
    
    /**
     * Calculate the refresh ahead threshold in seconds.
     * 
     * @return Refresh ahead threshold in seconds
     */
    public long getRefreshAheadThresholdSeconds() {
        if (ttlSeconds == null || refreshAheadFactor == null) {
            return 0L;
        }
        return (ttlSeconds * refreshAheadFactor) / 100L;
    }
    
    // Validation
    
    /**
     * Validate the cache configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (ttlSeconds != null && ttlSeconds <= 0) {
            throw new IllegalArgumentException("TTL seconds must be positive");
        }
        
        if (maxIdleSeconds != null && maxIdleSeconds <= 0) {
            throw new IllegalArgumentException("Max idle seconds must be positive");
        }
        
        if (maxSize != null && maxSize <= 0) {
            throw new IllegalArgumentException("Max cache size must be positive");
        }
        
        if (refreshAheadFactor != null && (refreshAheadFactor <= 0 || refreshAheadFactor >= 100)) {
            throw new IllegalArgumentException("Refresh ahead factor must be between 1 and 99");
        }
        
        if (warmupBatchSize != null && warmupBatchSize <= 0) {
            throw new IllegalArgumentException("Warmup batch size must be positive");
        }
        
        if (warmupDelay != null && warmupDelay < 0) {
            throw new IllegalArgumentException("Warmup delay cannot be negative");
        }
        
        if (replicationFactor != null && replicationFactor <= 0) {
            throw new IllegalArgumentException("Replication factor must be positive");
        }
    }
    
    /**
     * Create a copy of this cache configuration.
     * 
     * @return A new CacheConfig with the same settings
     */
    public CacheConfig copy() {
        CacheConfig copy = new CacheConfig();
        copy.enabled = this.enabled;
        copy.ttlSeconds = this.ttlSeconds;
        copy.maxIdleSeconds = this.maxIdleSeconds;
        copy.maxSize = this.maxSize;
        copy.evictionPolicy = this.evictionPolicy;
        copy.preloadEnabled = this.preloadEnabled;
        copy.refreshAhead = this.refreshAhead;
        copy.refreshAheadFactor = this.refreshAheadFactor;
        copy.statisticsEnabled = this.statisticsEnabled;
        copy.keyPrefix = this.keyPrefix;
        copy.compressionEnabled = this.compressionEnabled;
        copy.serializationFormat = this.serializationFormat;
        copy.warmupEnabled = this.warmupEnabled;
        copy.warmupBatchSize = this.warmupBatchSize;
        copy.warmupDelay = this.warmupDelay;
        copy.distributedCache = this.distributedCache;
        copy.cacheCluster = this.cacheCluster;
        copy.replicationFactor = this.replicationFactor;
        return copy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheConfig that = (CacheConfig) o;
        return Objects.equals(enabled, that.enabled) &&
               Objects.equals(ttlSeconds, that.ttlSeconds) &&
               Objects.equals(maxSize, that.maxSize) &&
               evictionPolicy == that.evictionPolicy;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(enabled, ttlSeconds, maxSize, evictionPolicy);
    }
    
    @Override
    public String toString() {
        return "CacheConfig{" +
               "enabled=" + enabled +
               ", ttlSeconds=" + ttlSeconds +
               ", maxSize=" + maxSize +
               ", evictionPolicy=" + evictionPolicy +
               ", preloadEnabled=" + preloadEnabled +
               ", statisticsEnabled=" + statisticsEnabled +
               '}';
    }
}
