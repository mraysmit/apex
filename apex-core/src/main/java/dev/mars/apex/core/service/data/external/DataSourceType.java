package dev.mars.apex.core.service.data.external;

/**
 * Enumeration of supported external data source types.
 * 
 * Each type represents a different category of data source with specific
 * characteristics and implementation requirements.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public enum DataSourceType {
    
    /**
     * Database data sources (PostgreSQL, MySQL, Oracle, SQL Server, etc.).
     * 
     * Characteristics:
     * - JDBC-based connectivity
     * - SQL query support
     * - Transaction support
     * - Connection pooling
     * - High consistency
     */
    DATABASE("database", "Database", "JDBC-based database connections"),
    
    /**
     * REST API data sources (HTTP/HTTPS endpoints).
     * 
     * Characteristics:
     * - HTTP-based connectivity
     * - JSON/XML data formats
     * - Authentication support
     * - Circuit breaker patterns
     * - Variable latency
     */
    REST_API("rest-api", "REST API", "HTTP REST API endpoints"),
    
    /**
     * Message queue data sources (Kafka, RabbitMQ, ActiveMQ, etc.).
     * 
     * Characteristics:
     * - Asynchronous messaging
     * - Topic/queue-based
     * - High throughput
     * - Event-driven processing
     * - Eventual consistency
     */
    MESSAGE_QUEUE("message-queue", "Message Queue", "Message queue systems"),
    
    /**
     * File system data sources (CSV, JSON, XML files).
     * 
     * Characteristics:
     * - File-based storage
     * - Polling mechanisms
     * - Batch processing
     * - Format-specific parsers
     * - Local or network storage
     */
    FILE_SYSTEM("file-system", "File System", "File-based data sources"),
    
    /**
     * Cache data sources (Redis, Hazelcast, Memcached, etc.).
     * 
     * Characteristics:
     * - In-memory storage
     * - Key-value operations
     * - High performance
     * - TTL support
     * - Distributed caching
     */
    CACHE("cache", "Cache", "In-memory cache systems"),
    
    /**
     * Custom data sources (user-defined implementations).
     * 
     * Characteristics:
     * - Custom implementation
     * - Flexible integration
     * - Legacy system support
     * - Specialized protocols
     * - Variable characteristics
     */
    CUSTOM("custom", "Custom", "Custom data source implementations");
    
    private final String configValue;
    private final String displayName;
    private final String description;
    
    /**
     * Constructor for DataSourceType enum values.
     * 
     * @param configValue The value used in YAML configuration
     * @param displayName The human-readable display name
     * @param description A description of this data source type
     */
    DataSourceType(String configValue, String displayName, String description) {
        this.configValue = configValue;
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Get the configuration value used in YAML files.
     * 
     * @return The configuration value
     */
    public String getConfigValue() {
        return configValue;
    }
    
    /**
     * Get the human-readable display name.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the description of this data source type.
     * 
     * @return The description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Find a DataSourceType by its configuration value.
     * 
     * @param configValue The configuration value to search for
     * @return The matching DataSourceType, or null if not found
     */
    public static DataSourceType fromConfigValue(String configValue) {
        if (configValue == null) {
            return null;
        }
        
        for (DataSourceType type : values()) {
            if (type.configValue.equalsIgnoreCase(configValue)) {
                return type;
            }
        }
        
        return null;
    }
    
    /**
     * Check if this data source type supports real-time operations.
     * 
     * @return true if real-time operations are supported
     */
    public boolean supportsRealTime() {
        return this != FILE_SYSTEM; // File system typically requires polling
    }
    
    /**
     * Check if this data source type supports batch operations.
     * 
     * @return true if batch operations are supported
     */
    public boolean supportsBatchOperations() {
        return this == DATABASE || this == MESSAGE_QUEUE || this == FILE_SYSTEM || this == CUSTOM;
    }
    
    /**
     * Check if this data source type supports transactions.
     * 
     * @return true if transactions are supported
     */
    public boolean supportsTransactions() {
        return this == DATABASE || this == MESSAGE_QUEUE;
    }
    
    /**
     * Check if this data source type typically requires caching.
     * 
     * @return true if caching is recommended
     */
    public boolean recommendsCaching() {
        return this == REST_API || this == DATABASE || this == FILE_SYSTEM;
    }
    
    @Override
    public String toString() {
        return displayName + " (" + configValue + ")";
    }
}
