package dev.mars.apex.core.service.data.external.config;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;

import java.time.LocalDateTime;

/**
 * Event representing changes in data source configurations.
 * 
 * This class encapsulates information about configuration lifecycle events
 * such as addition, removal, and health status changes.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DataSourceConfigurationEvent {
    
    /**
     * Event types for data source configuration changes.
     */
    public enum EventType {
        INITIALIZED,            // Service was initialized
        CONFIGURATION_ADDED,    // Configuration was added
        CONFIGURATION_REMOVED,  // Configuration was removed
        CONFIGURATION_UPDATED,  // Configuration was updated
        HEALTH_RESTORED,       // Data source health was restored
        HEALTH_LOST,           // Data source health was lost
        RELOADED               // Configurations were reloaded
    }
    
    private final EventType eventType;
    private final String configurationName;
    private final DataSourceConfiguration configuration;
    private final Object data;
    private final LocalDateTime timestamp;
    private final String message;
    
    /**
     * Private constructor for creating events.
     * 
     * @param eventType The type of event
     * @param configurationName The name of the configuration (optional)
     * @param configuration The configuration instance (optional)
     * @param data Additional event data (optional)
     * @param message Event message
     */
    private DataSourceConfigurationEvent(EventType eventType, String configurationName,
                                        DataSourceConfiguration configuration, Object data, String message) {
        this.eventType = eventType;
        this.configurationName = configurationName;
        this.configuration = configuration;
        this.data = data;
        this.timestamp = LocalDateTime.now();
        this.message = message;
    }
    
    /**
     * Create an initialization event.
     * 
     * @param configurationCount Number of configurations initialized
     * @return Initialization event
     */
    public static DataSourceConfigurationEvent initialized(int configurationCount) {
        return new DataSourceConfigurationEvent(EventType.INITIALIZED, null, null, configurationCount,
            "DataSourceConfigurationService initialized with " + configurationCount + " configurations");
    }
    
    /**
     * Create a configuration added event.
     * 
     * @param name The name of the added configuration
     * @param configuration The configuration instance
     * @return Configuration added event
     */
    public static DataSourceConfigurationEvent configurationAdded(String name, DataSourceConfiguration configuration) {
        return new DataSourceConfigurationEvent(EventType.CONFIGURATION_ADDED, name, configuration, null,
            "Configuration '" + name + "' was added");
    }
    
    /**
     * Create a configuration removed event.
     * 
     * @param name The name of the removed configuration
     * @param configuration The configuration instance
     * @return Configuration removed event
     */
    public static DataSourceConfigurationEvent configurationRemoved(String name, DataSourceConfiguration configuration) {
        return new DataSourceConfigurationEvent(EventType.CONFIGURATION_REMOVED, name, configuration, null,
            "Configuration '" + name + "' was removed");
    }
    
    /**
     * Create a configuration updated event.
     * 
     * @param name The name of the updated configuration
     * @param configuration The configuration instance
     * @return Configuration updated event
     */
    public static DataSourceConfigurationEvent configurationUpdated(String name, DataSourceConfiguration configuration) {
        return new DataSourceConfigurationEvent(EventType.CONFIGURATION_UPDATED, name, configuration, null,
            "Configuration '" + name + "' was updated");
    }
    
    /**
     * Create a health restored event.
     * 
     * @param name The name of the configuration
     * @return Health restored event
     */
    public static DataSourceConfigurationEvent healthRestored(String name) {
        return new DataSourceConfigurationEvent(EventType.HEALTH_RESTORED, name, null, null,
            "Configuration '" + name + "' health was restored");
    }
    
    /**
     * Create a health lost event.
     * 
     * @param name The name of the configuration
     * @return Health lost event
     */
    public static DataSourceConfigurationEvent healthLost(String name) {
        return new DataSourceConfigurationEvent(EventType.HEALTH_LOST, name, null, null,
            "Configuration '" + name + "' health was lost");
    }
    
    /**
     * Create a reloaded event.
     * 
     * @param configurationCount Number of configurations reloaded
     * @return Reloaded event
     */
    public static DataSourceConfigurationEvent reloaded(int configurationCount) {
        return new DataSourceConfigurationEvent(EventType.RELOADED, null, null, configurationCount,
            "Configurations reloaded with " + configurationCount + " configurations");
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
     * Get the configuration name.
     * 
     * @return The configuration name (may be null)
     */
    public String getConfigurationName() {
        return configurationName;
    }
    
    /**
     * Get the configuration instance.
     * 
     * @return The configuration instance (may be null)
     */
    public DataSourceConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * Get additional event data.
     * 
     * @return The event data (may be null)
     */
    public Object getData() {
        return data;
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
     * Check if this is a lifecycle event.
     * 
     * @return true if this is a lifecycle event
     */
    public boolean isLifecycleEvent() {
        return eventType == EventType.INITIALIZED || eventType == EventType.RELOADED;
    }
    
    /**
     * Check if this is a configuration change event.
     * 
     * @return true if this is a configuration change event
     */
    public boolean isConfigurationChangeEvent() {
        return eventType == EventType.CONFIGURATION_ADDED ||
               eventType == EventType.CONFIGURATION_REMOVED ||
               eventType == EventType.CONFIGURATION_UPDATED;
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
        return "DataSourceConfigurationEvent{" +
               "eventType=" + eventType +
               ", configurationName='" + configurationName + '\'' +
               ", timestamp=" + timestamp +
               ", message='" + message + '\'' +
               '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DataSourceConfigurationEvent that = (DataSourceConfigurationEvent) o;
        
        if (eventType != that.eventType) return false;
        if (configurationName != null ? !configurationName.equals(that.configurationName) : that.configurationName != null)
            return false;
        return timestamp.equals(that.timestamp);
    }
    
    @Override
    public int hashCode() {
        int result = eventType.hashCode();
        result = 31 * result + (configurationName != null ? configurationName.hashCode() : 0);
        result = 31 * result + timestamp.hashCode();
        return result;
    }
}
