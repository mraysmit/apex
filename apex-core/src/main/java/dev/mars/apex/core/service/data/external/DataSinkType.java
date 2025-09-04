package dev.mars.apex.core.service.data.external;

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

/**
 * Enumeration of supported external data sink types.
 * 
 * Each type represents a different category of data sink with specific
 * characteristics and implementation requirements for writing data.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public enum DataSinkType {
    
    /**
     * Database data sinks (PostgreSQL, MySQL, Oracle, SQL Server, H2, etc.).
     * 
     * Characteristics:
     * - JDBC-based connectivity
     * - SQL insert/update/upsert operations
     * - Transaction support
     * - Connection pooling
     * - ACID compliance
     * - Schema management
     */
    DATABASE("database", "Database", "JDBC-based database connections for data output"),
    
    /**
     * File system data sinks (CSV, JSON, XML files).
     * 
     * Characteristics:
     * - File-based storage
     * - Format-specific writers
     * - Append/overwrite modes
     * - Directory management
     * - Local or network storage
     * - Batch file operations
     */
    FILE_SYSTEM("file-system", "File System", "File-based data output destinations"),
    
    /**
     * Message queue data sinks (Kafka, RabbitMQ, ActiveMQ, etc.).
     * 
     * Characteristics:
     * - Asynchronous messaging
     * - Topic/queue publishing
     * - High throughput
     * - Event-driven architecture
     * - Message ordering
     * - Delivery guarantees
     */
    MESSAGE_QUEUE("message-queue", "Message Queue", "Message queue publishing destinations"),
    
    /**
     * REST API data sinks (HTTP/HTTPS endpoints).
     * 
     * Characteristics:
     * - HTTP-based connectivity
     * - JSON/XML data formats
     * - Authentication support
     * - Circuit breaker patterns
     * - Retry mechanisms
     * - Rate limiting
     */
    REST_API("rest-api", "REST API", "HTTP REST API endpoints for data output"),
    
    /**
     * Cache data sinks (Redis, Hazelcast, Memcached, etc.).
     * 
     * Characteristics:
     * - In-memory storage
     * - Key-value operations
     * - High performance
     * - TTL support
     * - Distributed caching
     * - Atomic operations
     */
    CACHE("cache", "Cache", "In-memory cache systems for data storage"),
    
    /**
     * Custom data sinks (user-defined implementations).
     * 
     * Characteristics:
     * - Custom implementation
     * - Flexible integration
     * - Legacy system support
     * - Specialized protocols
     * - Variable characteristics
     * - Domain-specific logic
     */
    CUSTOM("custom", "Custom", "Custom data sink implementations");
    
    private final String code;
    private final String displayName;
    private final String description;
    
    /**
     * Constructor for DataSinkType enum.
     * 
     * @param code The string code for this type
     * @param displayName The human-readable display name
     * @param description A description of this sink type
     */
    DataSinkType(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Get the string code for this data sink type.
     * This is used in configuration files and API calls.
     * 
     * @return The string code
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Get the human-readable display name for this data sink type.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the description of this data sink type.
     * 
     * @return The description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get a DataSinkType from its string code.
     * This method is case-insensitive.
     * 
     * @param code The string code to look up
     * @return The corresponding DataSinkType, or null if not found
     */
    public static DataSinkType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        
        String normalizedCode = code.trim().toLowerCase();
        for (DataSinkType type : values()) {
            if (type.code.equals(normalizedCode)) {
                return type;
            }
        }
        
        return null;
    }
    
    /**
     * Get a DataSinkType from its display name.
     * This method is case-insensitive.
     * 
     * @param displayName The display name to look up
     * @return The corresponding DataSinkType, or null if not found
     */
    public static DataSinkType fromDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return null;
        }
        
        String normalizedName = displayName.trim().toLowerCase();
        for (DataSinkType type : values()) {
            if (type.displayName.toLowerCase().equals(normalizedName)) {
                return type;
            }
        }
        
        return null;
    }
    
    /**
     * Check if this data sink type supports batch operations.
     * 
     * @return true if batch operations are supported
     */
    public boolean supportsBatchOperations() {
        switch (this) {
            case DATABASE:
            case FILE_SYSTEM:
            case MESSAGE_QUEUE:
            case CACHE:
                return true;
            case REST_API:
            case CUSTOM:
                return false; // Depends on implementation
            default:
                return false;
        }
    }
    
    /**
     * Check if this data sink type supports transactions.
     * 
     * @return true if transactions are supported
     */
    public boolean supportsTransactions() {
        switch (this) {
            case DATABASE:
                return true;
            case FILE_SYSTEM:
            case MESSAGE_QUEUE:
            case REST_API:
            case CACHE:
            case CUSTOM:
                return false; // Depends on implementation
            default:
                return false;
        }
    }
    
    /**
     * Check if this data sink type is typically high-throughput.
     * 
     * @return true if this type is designed for high throughput
     */
    public boolean isHighThroughput() {
        switch (this) {
            case MESSAGE_QUEUE:
            case CACHE:
                return true;
            case DATABASE:
            case FILE_SYSTEM:
            case REST_API:
            case CUSTOM:
                return false; // Depends on implementation
            default:
                return false;
        }
    }
    
    @Override
    public String toString() {
        return displayName + " (" + code + ")";
    }
}
