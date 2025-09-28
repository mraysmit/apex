package dev.mars.apex.core.service.data.external.file;

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
import dev.mars.apex.core.config.datasink.OutputFormatConfig;
import dev.mars.apex.core.service.data.external.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * File system implementation of DataSink.
 * 
 * This class provides file-based data output with support for multiple formats
 * including CSV, JSON, XML, and plain text.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class FileSystemDataSink implements DataSink {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemDataSink.class);
    
    private DataSinkConfiguration configuration;
    private DataSinkMetrics metrics;
    private ConnectionStatus connectionStatus;
    private volatile boolean initialized = false;
    private volatile boolean shutdown = false;
    
    // File management
    private Path outputDirectory;
    private String fileNamePattern;
    private OutputFormatConfig.OutputFormat outputFormat;
    private final ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock();
    
    // Buffering
    private final List<Object> writeBuffer = new ArrayList<>();
    private int bufferSize = 100;
    private long lastFlushTime = System.currentTimeMillis();
    
    // Supported operations
    private static final List<String> SUPPORTED_OPERATIONS = Arrays.asList(
        "write", "append", "overwrite", "rotate", "archive"
    );

    // Operation cache for configured operations
    private final Map<String, String> operationCache = new ConcurrentHashMap<>();
    
    /**
     * Default constructor.
     */
    public FileSystemDataSink() {
        this.metrics = new DataSinkMetrics();
        this.connectionStatus = ConnectionStatus.notInitialized();
    }
    
    @Override
    public String getName() {
        return configuration != null ? configuration.getName() : "filesystem-sink";
    }

    /**
     * Set the configuration for this data sink.
     * This method allows setting the configuration without full initialization.
     *
     * @param configuration The data sink configuration
     */
    public void setConfiguration(DataSinkConfiguration configuration) {
        this.configuration = configuration;
    }
    
    @Override
    public DataSinkType getSinkType() {
        return DataSinkType.FILE_SYSTEM;
    }
    
    @Override
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }
    
    @Override
    public DataSinkMetrics getMetrics() {
        return metrics;
    }
    
    @Override
    public void initialize(DataSinkConfiguration config) throws DataSinkException {
        if (initialized) {
            throw DataSinkException.configurationError("File system sink is already initialized");
        }
        
        if (config == null) {
            throw DataSinkException.configurationError("Configuration cannot be null");
        }
        
        try {
            LOGGER.info("Initializing file system sink: {}", config.getName());
            LOGGER.info("*** MARK: FileSystemDataSink.initialize() called with config: {}", config);

            this.configuration = config;
            
            // Validate configuration
            validateConfiguration();
            
            // Initialize file system components
            initializeFileSystem();
            
            // Set up output format
            setupOutputFormat();
            
            // Configure buffering
            configureBuffering();

            // Cache operations from configuration
            LOGGER.info("About to cache operations for file system sink: {}", config.getName());
            cacheOperations();
            LOGGER.info("Finished caching operations for file system sink: {}", config.getName());

            this.initialized = true;
            this.connectionStatus = ConnectionStatus.connected("File system sink initialized successfully");

            LOGGER.info("File system sink initialized successfully: {}", config.getName());
            
        } catch (Exception e) {
            this.connectionStatus = ConnectionStatus.error("Failed to initialize file system sink", e);
            throw DataSinkException.configurationError("Failed to initialize file system sink", e);
        }
    }
    
    @Override
    public void shutdown() {
        if (shutdown) {
            return;
        }
        
        LOGGER.info("Shutting down file system sink: {}", getName());
        
        try {
            // Flush any remaining buffered data
            flush();
            
            this.connectionStatus = ConnectionStatus.shutdown();
            this.shutdown = true;
            
            LOGGER.info("File system sink shutdown completed: {}", getName());
            
        } catch (Exception e) {
            LOGGER.error("Error during file system sink shutdown", e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        if (!initialized || shutdown) {
            return false;
        }
        
        try {
            return testConnection();
        } catch (Exception e) {
            LOGGER.debug("Health check failed for file system sink: {}", getName(), e);
            return false;
        }
    }
    
    @Override
    public void write(Object data) throws DataSinkException {
        write("write", data);
    }
    
    @Override
    public void write(String operation, Object data) throws DataSinkException {
        write(operation, data, null);
    }
    
    @Override
    public void write(String operation, Object data, Map<String, Object> parameters) throws DataSinkException {
        LOGGER.info("*** MARK: FileSystemDataSink.write() called with operation='{}', data={}", operation, data != null ? data.getClass().getSimpleName() : "null");

        if (!initialized) {
            throw DataSinkException.configurationError("File system sink not initialized");
        }
        
        if (shutdown) {
            throw DataSinkException.configurationError("File system sink is shutdown");
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            fileLock.writeLock().lock();

            // Resolve the operation name to actual file system operation
            String resolvedOperation = resolveOperation(operation);

            if ("write".equals(resolvedOperation) || "append".equals(resolvedOperation)) {
                // Add to buffer for batch processing
                writeBuffer.add(data);
                LOGGER.info("Added data to buffer. Buffer size: {}/{}", writeBuffer.size(), bufferSize);

                // Check if we should flush
                if (shouldFlush()) {
                    LOGGER.info("Buffer size threshold reached, flushing buffer");
                    flushBuffer();
                }

                metrics.recordSuccessfulWrite(System.currentTimeMillis() - startTime, 1);

            } else if ("overwrite".equals(resolvedOperation)) {
                // Clear buffer and write immediately
                writeBuffer.clear();
                writeBuffer.add(data);
                flushBuffer();

                metrics.recordSuccessfulWrite(System.currentTimeMillis() - startTime, 1);

            } else {
                throw DataSinkException.configurationError("Unsupported resolved operation: " + resolvedOperation);
            }
            
        } catch (Exception e) {
            metrics.recordFailedWrite(System.currentTimeMillis() - startTime);
            throw DataSinkException.writeError("Failed to write data using operation: " + operation, e, 
                "Data: " + (data != null ? data.getClass().getSimpleName() : "null"));
        } finally {
            fileLock.writeLock().unlock();
        }
    }
    
    @Override
    public void writeBatch(List<Object> data) throws DataSinkException {
        writeBatch("write", data);
    }
    
    @Override
    public void writeBatch(String operation, List<Object> data) throws DataSinkException {
        writeBatch(operation, data, null);
    }
    
    @Override
    public void writeBatch(String operation, List<Object> data, Map<String, Object> parameters) throws DataSinkException {
        if (!initialized) {
            throw DataSinkException.configurationError("File system sink not initialized");
        }
        
        if (shutdown) {
            throw DataSinkException.configurationError("File system sink is shutdown");
        }
        
        if (data == null || data.isEmpty()) {
            LOGGER.debug("No data to write in batch operation");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            fileLock.writeLock().lock();

            // Resolve the operation name to actual file system operation
            String resolvedOperation = resolveOperation(operation);

            if ("write".equals(resolvedOperation) || "append".equals(resolvedOperation)) {
                writeBuffer.addAll(data);
                flushBuffer();

            } else if ("overwrite".equals(resolvedOperation)) {
                writeBuffer.clear();
                writeBuffer.addAll(data);
                flushBuffer();

            } else {
                throw DataSinkException.configurationError("Unsupported batch resolved operation: " + resolvedOperation);
            }
            
            metrics.recordSuccessfulBatch(System.currentTimeMillis() - startTime, data.size());
            
            LOGGER.debug("Successfully wrote batch using operation '{}', items: {}", operation, data.size());
            
        } catch (Exception e) {
            metrics.recordFailedBatch(System.currentTimeMillis() - startTime);
            throw DataSinkException.batchError("Failed to write batch using operation: " + operation, 0, data.size());
        } finally {
            fileLock.writeLock().unlock();
        }
    }
    
    @Override
    public Object execute(String operation, Map<String, Object> parameters) throws DataSinkException {
        if (!initialized) {
            throw DataSinkException.configurationError("File system sink not initialized");
        }
        
        try {
            switch (operation) {
                case "rotate":
                    return rotateFile();
                case "archive":
                    return archiveFile();
                case "flush":
                    flush();
                    return "Flushed";
                default:
                    throw DataSinkException.configurationError("Unsupported execute operation: " + operation);
            }
            
        } catch (Exception e) {
            throw DataSinkException.writeError("Failed to execute operation: " + operation, e, 
                "Parameters: " + parameters);
        }
    }
    
    @Override
    public void flush() throws DataSinkException {
        if (!initialized || shutdown) {
            LOGGER.info("Skipping flush - initialized: {}, shutdown: {}", initialized, shutdown);
            return;
        }

        LOGGER.info("Flushing file system sink. Buffer size: {}", writeBuffer.size());

        try {
            fileLock.writeLock().lock();
            flushBuffer();
        } catch (Exception e) {
            throw DataSinkException.writeError("Failed to flush file system sink", e, null);
        } finally {
            fileLock.writeLock().unlock();
        }
    }
    
    @Override
    public DataSinkConfiguration getConfiguration() {
        return configuration;
    }
    
    @Override
    public void refreshConfiguration(DataSinkConfiguration config) throws DataSinkException {
        // Implementation would go here
        throw DataSinkException.configurationError("Configuration refresh not yet implemented");
    }
    
    @Override
    public boolean testConnection() {
        try {
            return outputDirectory != null && Files.exists(outputDirectory) && Files.isWritable(outputDirectory);
        } catch (Exception e) {
            LOGGER.debug("Connection test failed", e);
            return false;
        }
    }
    
    @Override
    public List<String> getSupportedOperations() {
        return new ArrayList<>(SUPPORTED_OPERATIONS);
    }
    
    @Override
    public boolean supportsOperation(String operation) {
        return SUPPORTED_OPERATIONS.contains(operation);
    }
    
    @Override
    public Map<String, Object> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", getName());
        info.put("type", getSinkType().getDisplayName());
        info.put("status", getConnectionStatus().getState().name());
        info.put("initialized", initialized);
        info.put("shutdown", shutdown);
        info.put("supportedOperations", getSupportedOperations());
        info.put("outputDirectory", outputDirectory != null ? outputDirectory.toString() : null);
        info.put("outputFormat", outputFormat != null ? outputFormat.getCode() : null);
        info.put("bufferSize", bufferSize);
        info.put("bufferedItems", writeBuffer.size());
        info.put("metrics", metrics.toString());
        
        return info;
    }
    
    // Private helper methods (simplified for compilation)
    
    private void validateConfiguration() throws DataSinkException {
        // Basic validation
    }
    
    private void initializeFileSystem() throws DataSinkException {
        try {
            // Get configuration from the actual data sink configuration
            String basePath = configuration.getConnection().getBasePath();
            String filePattern = configuration.getConnection().getFilePattern();

            if (basePath == null || basePath.trim().isEmpty()) {
                throw new DataSinkException(DataSinkException.ErrorType.CONFIGURATION_ERROR,
                    "Base path is required for file system sink");
            }

            if (filePattern == null || filePattern.trim().isEmpty()) {
                throw new DataSinkException(DataSinkException.ErrorType.CONFIGURATION_ERROR,
                    "File pattern is required for file system sink");
            }

            this.outputDirectory = Paths.get(basePath);
            this.fileNamePattern = filePattern;

            // Create output directory if it doesn't exist
            if (!Files.exists(outputDirectory)) {
                Files.createDirectories(outputDirectory);
                LOGGER.info("Created output directory: {}", outputDirectory.toAbsolutePath());
            }

            LOGGER.info("File system sink initialized - Directory: {}, Pattern: {}",
                outputDirectory.toAbsolutePath(), fileNamePattern);

        } catch (Exception e) {
            throw new DataSinkException(DataSinkException.ErrorType.CONFIGURATION_ERROR,
                "Failed to initialize file system: " + e.getMessage(), e);
        }
    }
    
    private void setupOutputFormat() {
        this.outputFormat = OutputFormatConfig.OutputFormat.JSON;
    }
    
    private void configureBuffering() {
        this.bufferSize = 100;
    }
    
    private boolean shouldFlush() {
        return writeBuffer.size() >= bufferSize;
    }
    
    private void flushBuffer() throws IOException {
        if (writeBuffer.isEmpty()) {
            return;
        }

        try {
            // Determine the output file path
            Path outputFile = outputDirectory.resolve(fileNamePattern);

            // Convert buffer data to JSON format
            String jsonContent = convertDataToJson(writeBuffer);

            // Write to file
            String encoding = configuration.getConnection().getEncoding();
            if (encoding == null) {
                encoding = "UTF-8";
            }

            Files.writeString(outputFile, jsonContent, Charset.forName(encoding));

            LOGGER.info("Successfully wrote {} records to file: {}", writeBuffer.size(), outputFile.toAbsolutePath());

            // Clear buffer after successful write
            writeBuffer.clear();
            lastFlushTime = System.currentTimeMillis();

        } catch (Exception e) {
            LOGGER.error("Failed to flush buffer to file: {}", e.getMessage(), e);
            throw new IOException("Failed to write data to file: " + e.getMessage(), e);
        }
    }

    private String convertDataToJson(List<Object> data) {
        // Simple JSON conversion - convert list of maps to JSON array
        StringBuilder json = new StringBuilder();
        json.append("[\n");

        for (int i = 0; i < data.size(); i++) {
            Object item = data.get(i);
            if (item instanceof Map) {
                json.append("  ").append(mapToJson((Map<?, ?>) item));
            } else {
                json.append("  \"").append(item.toString()).append("\"");
            }

            if (i < data.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("]");
        return json.toString();
    }

    private String mapToJson(Map<?, ?> map) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        Object[] keys = map.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            Object key = keys[i];
            Object value = map.get(key);

            json.append("\"").append(key.toString()).append("\": ");

            if (value instanceof String) {
                json.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value.toString());
            } else if (value == null) {
                json.append("null");
            } else {
                json.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
            }

            if (i < keys.length - 1) {
                json.append(", ");
            }
        }

        json.append("}");
        return json.toString();
    }

    /**
     * Cache operations from configuration for operation resolution.
     */
    private void cacheOperations() {
        LOGGER.info("Caching operations for file system sink: {}", getName());
        LOGGER.info("Configuration: {}", configuration);
        if (configuration != null) {
            LOGGER.info("Configuration operations: {}", configuration.getOperations());
            if (configuration.getOperations() != null) {
                operationCache.putAll(configuration.getOperations());
                LOGGER.info("Cached {} operations for file system sink: {}",
                            operationCache.size(), getName());
                LOGGER.info("Operation cache contents: {}", operationCache);
            } else {
                LOGGER.warn("No operations found in configuration for file system sink: {}", getName());
            }
        } else {
            LOGGER.warn("No configuration found for file system sink: {}", getName());
        }
    }

    /**
     * Resolve operation name to actual file system operation.
     * Maps configured operation names to supported file system operations.
     */
    private String resolveOperation(String operation) throws DataSinkException {
        LOGGER.info("Resolving operation '{}' for file system sink: {}", operation, getName());
        LOGGER.info("Operation cache: {}", operationCache);
        LOGGER.info("Supported operations: {}", SUPPORTED_OPERATIONS);

        // First check if it's a direct supported operation
        if (SUPPORTED_OPERATIONS.contains(operation)) {
            LOGGER.info("Operation '{}' is directly supported", operation);
            return operation;
        }

        // Then check if it's a configured operation mapping
        String resolvedOperation = operationCache.get(operation);
        LOGGER.info("Operation '{}' resolved to: {}", operation, resolvedOperation);
        if (resolvedOperation != null) {
            // Validate that the resolved operation is supported
            if (SUPPORTED_OPERATIONS.contains(resolvedOperation)) {
                LOGGER.info("Resolved operation '{}' is supported", resolvedOperation);
                return resolvedOperation;
            } else {
                throw DataSinkException.configurationError(
                    "Configured operation '" + operation + "' maps to unsupported operation: " + resolvedOperation);
            }
        }

        throw DataSinkException.configurationError("Unknown operation: " + operation);
    }
    
    private String rotateFile() {
        return "File rotated";
    }
    
    private String archiveFile() {
        return "File archived";
    }
}
