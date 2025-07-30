package dev.mars.apex.core.service.data.external.manager;

import dev.mars.apex.core.service.data.external.DataSourceMetrics;
import dev.mars.apex.core.service.data.external.registry.RegistryStatistics;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Statistics holder for the data source manager.
 * 
 * This class provides comprehensive statistical information about the
 * data source manager including registry statistics, individual data
 * source metrics, and aggregated performance data.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public class DataSourceManagerStatistics {
    
    private final RegistryStatistics registryStatistics;
    private final Map<String, DataSourceMetrics> dataSourceMetrics;
    private final int typeGroupCount;
    private final LocalDateTime snapshotTime;
    
    /**
     * Constructor for manager statistics.
     * 
     * @param registryStatistics Statistics from the registry
     * @param dataSourceMetrics Metrics from individual data sources
     * @param typeGroupCount Number of type groups for load balancing
     * @param snapshotTime Time when the statistics were captured
     */
    public DataSourceManagerStatistics(RegistryStatistics registryStatistics,
                                      Map<String, DataSourceMetrics> dataSourceMetrics,
                                      int typeGroupCount,
                                      LocalDateTime snapshotTime) {
        this.registryStatistics = registryStatistics;
        this.dataSourceMetrics = new HashMap<>(dataSourceMetrics);
        this.typeGroupCount = typeGroupCount;
        this.snapshotTime = snapshotTime;
    }
    
    /**
     * Get the registry statistics.
     * 
     * @return Registry statistics
     */
    public RegistryStatistics getRegistryStatistics() {
        return registryStatistics;
    }
    
    /**
     * Get metrics for all data sources.
     * 
     * @return Immutable map of data source metrics
     */
    public Map<String, DataSourceMetrics> getDataSourceMetrics() {
        return Collections.unmodifiableMap(dataSourceMetrics);
    }
    
    /**
     * Get metrics for a specific data source.
     * 
     * @param dataSourceName The name of the data source
     * @return Data source metrics, or null if not found
     */
    public DataSourceMetrics getDataSourceMetrics(String dataSourceName) {
        return dataSourceMetrics.get(dataSourceName);
    }
    
    /**
     * Get the number of type groups.
     * 
     * @return Type group count
     */
    public int getTypeGroupCount() {
        return typeGroupCount;
    }
    
    /**
     * Get the time when these statistics were captured.
     * 
     * @return Snapshot time
     */
    public LocalDateTime getSnapshotTime() {
        return snapshotTime;
    }
    
    /**
     * Get the total number of requests across all data sources.
     * 
     * @return Total request count
     */
    public long getTotalRequests() {
        return dataSourceMetrics.values().stream()
            .mapToLong(metrics -> metrics.getSuccessfulRequests() + metrics.getFailedRequests())
            .sum();
    }
    
    /**
     * Get the total number of successful requests across all data sources.
     * 
     * @return Total successful request count
     */
    public long getTotalSuccessfulRequests() {
        return dataSourceMetrics.values().stream()
            .mapToLong(DataSourceMetrics::getSuccessfulRequests)
            .sum();
    }
    
    /**
     * Get the total number of failed requests across all data sources.
     * 
     * @return Total failed request count
     */
    public long getTotalFailedRequests() {
        return dataSourceMetrics.values().stream()
            .mapToLong(DataSourceMetrics::getFailedRequests)
            .sum();
    }
    
    /**
     * Get the overall success rate across all data sources.
     * 
     * @return Success rate as a percentage (0.0 to 100.0)
     */
    public double getOverallSuccessRate() {
        long totalRequests = getTotalRequests();
        if (totalRequests == 0) {
            return 100.0;
        }
        return (getTotalSuccessfulRequests() * 100.0) / totalRequests;
    }
    
    /**
     * Get the total number of cache hits across all data sources.
     * 
     * @return Total cache hit count
     */
    public long getTotalCacheHits() {
        return dataSourceMetrics.values().stream()
            .mapToLong(DataSourceMetrics::getCacheHits)
            .sum();
    }
    
    /**
     * Get the total number of cache misses across all data sources.
     * 
     * @return Total cache miss count
     */
    public long getTotalCacheMisses() {
        return dataSourceMetrics.values().stream()
            .mapToLong(DataSourceMetrics::getCacheMisses)
            .sum();
    }
    
    /**
     * Get the overall cache hit rate across all data sources.
     * 
     * @return Cache hit rate as a percentage (0.0 to 100.0)
     */
    public double getOverallCacheHitRate() {
        long totalCacheRequests = getTotalCacheHits() + getTotalCacheMisses();
        if (totalCacheRequests == 0) {
            return 0.0;
        }
        return (getTotalCacheHits() * 100.0) / totalCacheRequests;
    }
    
    /**
     * Get the average response time across all data sources.
     * 
     * @return Average response time in milliseconds
     */
    public double getAverageResponseTime() {
        if (dataSourceMetrics.isEmpty()) {
            return 0.0;
        }
        
        double totalResponseTime = dataSourceMetrics.values().stream()
            .mapToDouble(DataSourceMetrics::getAverageResponseTime)
            .sum();
        
        return totalResponseTime / dataSourceMetrics.size();
    }
    
    /**
     * Get the total number of records processed across all data sources.
     * 
     * @return Total records processed
     */
    public long getTotalRecordsProcessed() {
        return dataSourceMetrics.values().stream()
            .mapToLong(DataSourceMetrics::getRecordsProcessed)
            .sum();
    }
    
    /**
     * Get a summary string of the statistics.
     * 
     * @return Summary string
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Manager Statistics: ");
        summary.append(registryStatistics.getTotalDataSources()).append(" data sources, ");
        summary.append(registryStatistics.getHealthyDataSources()).append(" healthy, ");
        summary.append(String.format("%.1f%% success rate, ", getOverallSuccessRate()));
        summary.append(String.format("%.1f%% cache hit rate, ", getOverallCacheHitRate()));
        summary.append(String.format("%.2fms avg response time", getAverageResponseTime()));
        
        return summary.toString();
    }
    
    @Override
    public String toString() {
        return "DataSourceManagerStatistics{" +
               "totalDataSources=" + registryStatistics.getTotalDataSources() +
               ", healthyDataSources=" + registryStatistics.getHealthyDataSources() +
               ", typeGroupCount=" + typeGroupCount +
               ", totalRequests=" + getTotalRequests() +
               ", successRate=" + String.format("%.1f%%", getOverallSuccessRate()) +
               ", cacheHitRate=" + String.format("%.1f%%", getOverallCacheHitRate()) +
               ", avgResponseTime=" + String.format("%.2fms", getAverageResponseTime()) +
               ", snapshotTime=" + snapshotTime +
               '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DataSourceManagerStatistics that = (DataSourceManagerStatistics) o;
        
        if (typeGroupCount != that.typeGroupCount) return false;
        if (!registryStatistics.equals(that.registryStatistics)) return false;
        if (!dataSourceMetrics.equals(that.dataSourceMetrics)) return false;
        return snapshotTime.equals(that.snapshotTime);
    }
    
    @Override
    public int hashCode() {
        int result = registryStatistics.hashCode();
        result = 31 * result + dataSourceMetrics.hashCode();
        result = 31 * result + typeGroupCount;
        result = 31 * result + snapshotTime.hashCode();
        return result;
    }
}
