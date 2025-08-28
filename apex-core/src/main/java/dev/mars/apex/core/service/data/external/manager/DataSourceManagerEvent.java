package dev.mars.apex.core.service.data.external.manager;

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


import java.time.LocalDateTime;

/**
 * Event representing changes in the data source manager.
 * 
 * This class encapsulates information about manager lifecycle events
 * and data source management operations.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DataSourceManagerEvent {
    
    /**
     * Event types for data source manager changes.
     */
    public enum EventType {
        INITIALIZED,         // Manager was initialized
        SHUTDOWN,           // Manager was shut down
        DATA_SOURCE_ADDED,  // Data source was added
        DATA_SOURCE_REMOVED, // Data source was removed
        HEALTH_RESTORED,    // Data source health was restored
        HEALTH_LOST,        // Data source health was lost
        REFRESH_COMPLETED   // Refresh operation completed
    }
    
    private final EventType eventType;
    private final String dataSourceName;
    private final Object data;
    private final LocalDateTime timestamp;
    private final String message;
    
    /**
     * Private constructor for creating events.
     * 
     * @param eventType The type of event
     * @param dataSourceName The name of the data source (optional)
     * @param data Additional event data (optional)
     * @param message Event message
     */
    private DataSourceManagerEvent(EventType eventType, String dataSourceName, 
                                  Object data, String message) {
        this.eventType = eventType;
        this.dataSourceName = dataSourceName;
        this.data = data;
        this.timestamp = LocalDateTime.now();
        this.message = message;
    }
    
    /**
     * Create an initialization event.
     * 
     * @param dataSourceCount Number of data sources initialized
     * @return Initialization event
     */
    public static DataSourceManagerEvent initialized(int dataSourceCount) {
        return new DataSourceManagerEvent(EventType.INITIALIZED, null, dataSourceCount,
            "DataSourceManager initialized with " + dataSourceCount + " data sources");
    }
    
    /**
     * Create a shutdown event.
     * 
     * @return Shutdown event
     */
    public static DataSourceManagerEvent shutdown() {
        return new DataSourceManagerEvent(EventType.SHUTDOWN, null, null,
            "DataSourceManager was shut down");
    }
    
    /**
     * Create a data source added event.
     * 
     * @param dataSourceName The name of the added data source
     * @return Data source added event
     */
    public static DataSourceManagerEvent dataSourceAdded(String dataSourceName) {
        return new DataSourceManagerEvent(EventType.DATA_SOURCE_ADDED, dataSourceName, null,
            "Data source '" + dataSourceName + "' was added");
    }
    
    /**
     * Create a data source removed event.
     * 
     * @param dataSourceName The name of the removed data source
     * @return Data source removed event
     */
    public static DataSourceManagerEvent dataSourceRemoved(String dataSourceName) {
        return new DataSourceManagerEvent(EventType.DATA_SOURCE_REMOVED, dataSourceName, null,
            "Data source '" + dataSourceName + "' was removed");
    }
    
    /**
     * Create a health restored event.
     * 
     * @param dataSourceName The name of the data source
     * @return Health restored event
     */
    public static DataSourceManagerEvent healthRestored(String dataSourceName) {
        return new DataSourceManagerEvent(EventType.HEALTH_RESTORED, dataSourceName, null,
            "Data source '" + dataSourceName + "' health was restored");
    }
    
    /**
     * Create a health lost event.
     * 
     * @param dataSourceName The name of the data source
     * @return Health lost event
     */
    public static DataSourceManagerEvent healthLost(String dataSourceName) {
        return new DataSourceManagerEvent(EventType.HEALTH_LOST, dataSourceName, null,
            "Data source '" + dataSourceName + "' health was lost");
    }
    
    /**
     * Create a refresh completed event.
     * 
     * @return Refresh completed event
     */
    public static DataSourceManagerEvent refreshCompleted() {
        return new DataSourceManagerEvent(EventType.REFRESH_COMPLETED, null, null,
            "Data source refresh operation completed");
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
     * @return The data source name (may be null)
     */
    public String getDataSourceName() {
        return dataSourceName;
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
        return eventType == EventType.INITIALIZED || eventType == EventType.SHUTDOWN;
    }
    
    /**
     * Check if this is a data source event.
     * 
     * @return true if this is a data source event
     */
    public boolean isDataSourceEvent() {
        return eventType == EventType.DATA_SOURCE_ADDED || 
               eventType == EventType.DATA_SOURCE_REMOVED ||
               eventType == EventType.HEALTH_RESTORED ||
               eventType == EventType.HEALTH_LOST;
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
        return "DataSourceManagerEvent{" +
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
        
        DataSourceManagerEvent that = (DataSourceManagerEvent) o;
        
        if (eventType != that.eventType) return false;
        if (dataSourceName != null ? !dataSourceName.equals(that.dataSourceName) : that.dataSourceName != null)
            return false;
        return timestamp.equals(that.timestamp);
    }
    
    @Override
    public int hashCode() {
        int result = eventType.hashCode();
        result = 31 * result + (dataSourceName != null ? dataSourceName.hashCode() : 0);
        result = 31 * result + timestamp.hashCode();
        return result;
    }
}
