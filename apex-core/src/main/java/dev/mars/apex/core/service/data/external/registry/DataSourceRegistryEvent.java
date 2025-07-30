package dev.mars.apex.core.service.data.external.registry;

import dev.mars.apex.core.service.data.external.ExternalDataSource;

import java.time.LocalDateTime;

/**
 * Event representing changes in the data source registry.
 * 
 * This class encapsulates information about data source lifecycle events
 * such as registration, unregistration, and health status changes.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public class DataSourceRegistryEvent {
    
    /**
     * Event types for data source registry changes.
     */
    public enum EventType {
        REGISTERED,      // Data source was registered
        UNREGISTERED,    // Data source was unregistered
        HEALTH_RESTORED, // Data source health was restored
        HEALTH_LOST      // Data source health was lost
    }
    
    private final EventType eventType;
    private final String dataSourceName;
    private final ExternalDataSource dataSource;
    private final LocalDateTime timestamp;
    private final String message;
    
    /**
     * Private constructor for creating events.
     * 
     * @param eventType The type of event
     * @param dataSourceName The name of the data source
     * @param dataSource The data source instance
     * @param message Optional message describing the event
     */
    private DataSourceRegistryEvent(EventType eventType, String dataSourceName, 
                                   ExternalDataSource dataSource, String message) {
        this.eventType = eventType;
        this.dataSourceName = dataSourceName;
        this.dataSource = dataSource;
        this.timestamp = LocalDateTime.now();
        this.message = message;
    }
    
    /**
     * Create a registration event.
     * 
     * @param dataSourceName The name of the registered data source
     * @param dataSource The data source instance
     * @return Registration event
     */
    public static DataSourceRegistryEvent registered(String dataSourceName, ExternalDataSource dataSource) {
        return new DataSourceRegistryEvent(EventType.REGISTERED, dataSourceName, dataSource,
            "Data source '" + dataSourceName + "' was registered");
    }
    
    /**
     * Create an unregistration event.
     * 
     * @param dataSourceName The name of the unregistered data source
     * @param dataSource The data source instance
     * @return Unregistration event
     */
    public static DataSourceRegistryEvent unregistered(String dataSourceName, ExternalDataSource dataSource) {
        return new DataSourceRegistryEvent(EventType.UNREGISTERED, dataSourceName, dataSource,
            "Data source '" + dataSourceName + "' was unregistered");
    }
    
    /**
     * Create a health restored event.
     * 
     * @param dataSourceName The name of the data source
     * @param dataSource The data source instance
     * @return Health restored event
     */
    public static DataSourceRegistryEvent healthRestored(String dataSourceName, ExternalDataSource dataSource) {
        return new DataSourceRegistryEvent(EventType.HEALTH_RESTORED, dataSourceName, dataSource,
            "Data source '" + dataSourceName + "' health was restored");
    }
    
    /**
     * Create a health lost event.
     * 
     * @param dataSourceName The name of the data source
     * @param dataSource The data source instance
     * @return Health lost event
     */
    public static DataSourceRegistryEvent healthLost(String dataSourceName, ExternalDataSource dataSource) {
        return new DataSourceRegistryEvent(EventType.HEALTH_LOST, dataSourceName, dataSource,
            "Data source '" + dataSourceName + "' health was lost");
    }
    
    /**
     * Get the event type.
     * 
     * @return The event type
     */
    public EventType getEventType() {
        return eventType;
    }
    
    /**
     * Get the data source name.
     * 
     * @return The data source name
     */
    public String getDataSourceName() {
        return dataSourceName;
    }
    
    /**
     * Get the data source instance.
     * 
     * @return The data source instance
     */
    public ExternalDataSource getDataSource() {
        return dataSource;
    }
    
    /**
     * Get the event timestamp.
     * 
     * @return The timestamp when the event occurred
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get the event message.
     * 
     * @return The event message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Check if this is a registration event.
     * 
     * @return true if this is a registration event
     */
    public boolean isRegistration() {
        return eventType == EventType.REGISTERED;
    }
    
    /**
     * Check if this is an unregistration event.
     * 
     * @return true if this is an unregistration event
     */
    public boolean isUnregistration() {
        return eventType == EventType.UNREGISTERED;
    }
    
    /**
     * Check if this is a health-related event.
     * 
     * @return true if this is a health-related event
     */
    public boolean isHealthEvent() {
        return eventType == EventType.HEALTH_RESTORED || eventType == EventType.HEALTH_LOST;
    }
    
    /**
     * Check if this represents a health improvement.
     * 
     * @return true if health was restored
     */
    public boolean isHealthImprovement() {
        return eventType == EventType.HEALTH_RESTORED;
    }
    
    /**
     * Check if this represents a health degradation.
     * 
     * @return true if health was lost
     */
    public boolean isHealthDegradation() {
        return eventType == EventType.HEALTH_LOST;
    }
    
    @Override
    public String toString() {
        return "DataSourceRegistryEvent{" +
               "eventType=" + eventType +
               ", dataSourceName='" + dataSourceName + '\'' +
               ", timestamp=" + timestamp +
               ", message='" + message + '\'' +
               '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DataSourceRegistryEvent that = (DataSourceRegistryEvent) o;
        
        if (eventType != that.eventType) return false;
        if (!dataSourceName.equals(that.dataSourceName)) return false;
        return timestamp.equals(that.timestamp);
    }
    
    @Override
    public int hashCode() {
        int result = eventType.hashCode();
        result = 31 * result + dataSourceName.hashCode();
        result = 31 * result + timestamp.hashCode();
        return result;
    }
}
