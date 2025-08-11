package dev.mars.apex.core.service.data.external.registry;

import dev.mars.apex.core.service.data.external.DataSourceType;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Statistics holder for the data source registry.
 * 
 * This class provides statistical information about the registered
 * data sources including counts by type, health status, and timestamps.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class RegistryStatistics {
    
    private final int totalDataSources;
    private final int healthyDataSources;
    private final int unhealthyDataSources;
    private final Map<DataSourceType, Integer> dataSourcesByType;
    private final LocalDateTime snapshotTime;
    
    /**
     * Constructor for registry statistics.
     * 
     * @param totalDataSources Total number of registered data sources
     * @param healthyDataSources Number of healthy data sources
     * @param unhealthyDataSources Number of unhealthy data sources
     * @param dataSourcesByType Count of data sources by type
     * @param snapshotTime Time when the statistics were captured
     */
    public RegistryStatistics(int totalDataSources, int healthyDataSources, int unhealthyDataSources,
                             Map<DataSourceType, Integer> dataSourcesByType, LocalDateTime snapshotTime) {
        this.totalDataSources = totalDataSources;
        this.healthyDataSources = healthyDataSources;
        this.unhealthyDataSources = unhealthyDataSources;
        this.dataSourcesByType = new HashMap<>(dataSourcesByType);
        this.snapshotTime = snapshotTime;
    }
    
    /**
     * Get the total number of registered data sources.
     * 
     * @return Total data sources count
     */
    public int getTotalDataSources() {
        return totalDataSources;
    }
    
    /**
     * Get the number of healthy data sources.
     * 
     * @return Healthy data sources count
     */
    public int getHealthyDataSources() {
        return healthyDataSources;
    }
    
    /**
     * Get the number of unhealthy data sources.
     * 
     * @return Unhealthy data sources count
     */
    public int getUnhealthyDataSources() {
        return unhealthyDataSources;
    }
    
    /**
     * Get the health percentage of data sources.
     * 
     * @return Health percentage (0.0 to 100.0)
     */
    public double getHealthPercentage() {
        if (totalDataSources == 0) {
            return 100.0;
        }
        return (healthyDataSources * 100.0) / totalDataSources;
    }
    
    /**
     * Get the count of data sources by type.
     * 
     * @return Immutable map of data source counts by type
     */
    public Map<DataSourceType, Integer> getDataSourcesByType() {
        return Collections.unmodifiableMap(dataSourcesByType);
    }
    
    /**
     * Get the count of data sources for a specific type.
     * 
     * @param type The data source type
     * @return Count of data sources of the specified type
     */
    public int getCountByType(DataSourceType type) {
        return dataSourcesByType.getOrDefault(type, 0);
    }
    
    /**
     * Get the number of different data source types.
     * 
     * @return Number of different types
     */
    public int getTypeCount() {
        return dataSourcesByType.size();
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
     * Check if the registry is empty.
     * 
     * @return true if no data sources are registered
     */
    public boolean isEmpty() {
        return totalDataSources == 0;
    }
    
    /**
     * Check if all data sources are healthy.
     * 
     * @return true if all data sources are healthy
     */
    public boolean isAllHealthy() {
        return totalDataSources > 0 && unhealthyDataSources == 0;
    }
    
    /**
     * Check if any data sources are unhealthy.
     * 
     * @return true if at least one data source is unhealthy
     */
    public boolean hasUnhealthyDataSources() {
        return unhealthyDataSources > 0;
    }
    
    /**
     * Get a summary string of the statistics.
     * 
     * @return Summary string
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Registry Statistics: ");
        summary.append(totalDataSources).append(" total, ");
        summary.append(healthyDataSources).append(" healthy, ");
        summary.append(unhealthyDataSources).append(" unhealthy");
        
        if (!dataSourcesByType.isEmpty()) {
            summary.append(" (");
            boolean first = true;
            for (Map.Entry<DataSourceType, Integer> entry : dataSourcesByType.entrySet()) {
                if (!first) {
                    summary.append(", ");
                }
                summary.append(entry.getKey().name().toLowerCase())
                       .append(": ")
                       .append(entry.getValue());
                first = false;
            }
            summary.append(")");
        }
        
        return summary.toString();
    }
    
    @Override
    public String toString() {
        return "RegistryStatistics{" +
               "totalDataSources=" + totalDataSources +
               ", healthyDataSources=" + healthyDataSources +
               ", unhealthyDataSources=" + unhealthyDataSources +
               ", healthPercentage=" + String.format("%.1f%%", getHealthPercentage()) +
               ", typeCount=" + getTypeCount() +
               ", snapshotTime=" + snapshotTime +
               '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        RegistryStatistics that = (RegistryStatistics) o;
        
        if (totalDataSources != that.totalDataSources) return false;
        if (healthyDataSources != that.healthyDataSources) return false;
        if (unhealthyDataSources != that.unhealthyDataSources) return false;
        if (!dataSourcesByType.equals(that.dataSourcesByType)) return false;
        return snapshotTime.equals(that.snapshotTime);
    }
    
    @Override
    public int hashCode() {
        int result = totalDataSources;
        result = 31 * result + healthyDataSources;
        result = 31 * result + unhealthyDataSources;
        result = 31 * result + dataSourcesByType.hashCode();
        result = 31 * result + snapshotTime.hashCode();
        return result;
    }
}
