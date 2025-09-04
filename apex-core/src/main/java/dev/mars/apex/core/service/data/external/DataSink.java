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

import dev.mars.apex.core.config.datasink.DataSinkConfiguration;

import java.util.List;
import java.util.Map;

/**
 * Interface for external data sinks that can write data to various destinations.
 * 
 * This interface provides a unified abstraction for writing data to different
 * output systems including databases, files, message queues, REST APIs, and caches.
 * 
 * Key features:
 * - Health monitoring and metrics collection
 * - Connection management and lifecycle control
 * - Batch operations for high-throughput scenarios
 * - Configuration-driven initialization
 * - Error handling and retry mechanisms
 * 
 * Supported sink types:
 * - Database sinks (PostgreSQL, MySQL, H2, etc.)
 * - File system sinks (CSV, JSON, XML files)
 * - Message queue sinks (Kafka, RabbitMQ, etc.)
 * - REST API sinks (HTTP endpoints)
 * - Cache sinks (Redis, Hazelcast, etc.)
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public interface DataSink {
    
    /**
     * Get the name of this data sink.
     * 
     * @return The name of the data sink
     */
    String getName();
    
    /**
     * Get the type of this data sink.
     * 
     * @return The data sink type (DATABASE, FILE_SYSTEM, MESSAGE_QUEUE, etc.)
     */
    DataSinkType getSinkType();
    
    /**
     * Get the current connection status of this data sink.
     * 
     * @return The current connection status
     */
    ConnectionStatus getConnectionStatus();
    
    /**
     * Get performance and usage metrics for this data sink.
     * 
     * @return Current metrics including write times, error rates, throughput, etc.
     */
    DataSinkMetrics getMetrics();
    
    /**
     * Initialize the data sink with the provided configuration.
     * This method should be called before using the data sink.
     * 
     * @param config The configuration for this data sink
     * @throws DataSinkException if initialization fails
     */
    void initialize(DataSinkConfiguration config) throws DataSinkException;
    
    /**
     * Shutdown the data sink and release all resources.
     * This method should be called when the data sink is no longer needed.
     */
    void shutdown();
    
    /**
     * Check if the data sink is healthy and available.
     * 
     * @return true if the data sink is healthy, false otherwise
     */
    boolean isHealthy();
    
    /**
     * Write a single data object to the sink.
     * 
     * @param data The data object to write
     * @throws DataSinkException if the write operation fails
     */
    void write(Object data) throws DataSinkException;
    
    /**
     * Write a single data object using a specific operation.
     * 
     * @param operation The operation to use (e.g., "insert", "update", "upsert")
     * @param data The data object to write
     * @throws DataSinkException if the write operation fails
     */
    void write(String operation, Object data) throws DataSinkException;
    
    /**
     * Write a single data object with parameters.
     * 
     * @param operation The operation to use
     * @param data The data object to write
     * @param parameters Additional parameters for the operation
     * @throws DataSinkException if the write operation fails
     */
    void write(String operation, Object data, Map<String, Object> parameters) throws DataSinkException;
    
    /**
     * Write a batch of data objects to the sink.
     * This method should be used for high-throughput scenarios.
     * 
     * @param data The list of data objects to write
     * @throws DataSinkException if the batch write operation fails
     */
    void writeBatch(List<Object> data) throws DataSinkException;
    
    /**
     * Write a batch of data objects using a specific operation.
     * 
     * @param operation The operation to use for all objects in the batch
     * @param data The list of data objects to write
     * @throws DataSinkException if the batch write operation fails
     */
    void writeBatch(String operation, List<Object> data) throws DataSinkException;
    
    /**
     * Write a batch of data objects with parameters.
     * 
     * @param operation The operation to use
     * @param data The list of data objects to write
     * @param parameters Additional parameters for the operation
     * @throws DataSinkException if the batch write operation fails
     */
    void writeBatch(String operation, List<Object> data, Map<String, Object> parameters) throws DataSinkException;
    
    /**
     * Execute a custom operation on the sink.
     * This can be used for operations like schema creation, data cleanup, etc.
     * 
     * @param operation The operation to execute
     * @param parameters Parameters for the operation
     * @return The result of the operation, if any
     * @throws DataSinkException if the operation fails
     */
    Object execute(String operation, Map<String, Object> parameters) throws DataSinkException;
    
    /**
     * Flush any pending writes to ensure data is persisted.
     * This method should be called to ensure all buffered data is written.
     * 
     * @throws DataSinkException if the flush operation fails
     */
    void flush() throws DataSinkException;
    
    /**
     * Get the configuration used to initialize this data sink.
     * 
     * @return The data sink configuration
     */
    DataSinkConfiguration getConfiguration();
    
    /**
     * Refresh the data sink configuration.
     * This can be used to update connection settings, credentials, etc.
     * 
     * @param config The new configuration
     * @throws DataSinkException if the refresh operation fails
     */
    void refreshConfiguration(DataSinkConfiguration config) throws DataSinkException;
    
    /**
     * Test the connection to the data sink.
     * This method can be used for health checks and diagnostics.
     * 
     * @return true if the connection test succeeds, false otherwise
     */
    boolean testConnection();
    
    /**
     * Get the list of supported operations for this data sink.
     * 
     * @return List of operation names supported by this sink
     */
    List<String> getSupportedOperations();
    
    /**
     * Check if a specific operation is supported by this data sink.
     * 
     * @param operation The operation to check
     * @return true if the operation is supported, false otherwise
     */
    boolean supportsOperation(String operation);
    
    /**
     * Get detailed information about the data sink.
     * This includes connection details, configuration summary, and status.
     * 
     * @return Map containing detailed information about the sink
     */
    Map<String, Object> getInfo();
}
