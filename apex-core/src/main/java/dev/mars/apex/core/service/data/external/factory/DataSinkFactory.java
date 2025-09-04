package dev.mars.apex.core.service.data.external.factory;

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
import dev.mars.apex.core.service.data.external.*;
import dev.mars.apex.core.service.data.external.database.DatabaseDataSink;
import dev.mars.apex.core.service.data.external.file.FileSystemDataSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Factory for creating data sink instances.
 * 
 * This factory provides a centralized way to create and configure data sinks
 * based on their type and configuration.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DataSinkFactory {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSinkFactory.class);
    
    // Registry for custom data sink providers
    private final Map<String, Supplier<DataSink>> customProviders = new ConcurrentHashMap<>();
    
    // Singleton instance
    private static volatile DataSinkFactory instance;
    
    /**
     * Private constructor for singleton pattern.
     */
    private DataSinkFactory() {
        // Initialize default providers
    }
    
    /**
     * Get the singleton instance of DataSinkFactory.
     * 
     * @return The factory instance
     */
    public static DataSinkFactory getInstance() {
        if (instance == null) {
            synchronized (DataSinkFactory.class) {
                if (instance == null) {
                    instance = new DataSinkFactory();
                }
            }
        }
        return instance;
    }
    
    /**
     * Create a data sink based on the provided configuration.
     * 
     * @param configuration The data sink configuration
     * @return A configured data sink instance
     * @throws DataSinkException if creation fails
     */
    public DataSink createDataSink(DataSinkConfiguration configuration) throws DataSinkException {
        if (configuration == null) {
            throw DataSinkException.configurationError("Configuration cannot be null");
        }
        
        if (!configuration.isEnabled()) {
            throw DataSinkException.configurationError("Data sink is disabled: " + configuration.getName());
        }
        
        LOGGER.debug("Creating data sink: {} of type: {}", configuration.getName(), configuration.getType());
        
        try {
            DataSink dataSink = createDataSinkByType(configuration);
            dataSink.initialize(configuration);
            
            LOGGER.info("Successfully created data sink: {} of type: {}", 
                       configuration.getName(), configuration.getType());
            
            return dataSink;
            
        } catch (DataSinkException e) {
            throw e;
        } catch (Exception e) {
            throw DataSinkException.configurationError(
                "Failed to create data sink: " + configuration.getName(), e);
        }
    }
    
    /**
     * Create a data sink without initializing it.
     * This is useful for testing or when you want to initialize manually.
     * 
     * @param configuration The data sink configuration
     * @return An uninitialized data sink instance
     * @throws DataSinkException if creation fails
     */
    public DataSink createDataSinkWithoutInitialization(DataSinkConfiguration configuration) throws DataSinkException {
        if (configuration == null) {
            throw DataSinkException.configurationError("Configuration cannot be null");
        }
        
        LOGGER.debug("Creating uninitialized data sink: {} of type: {}", 
                    configuration.getName(), configuration.getType());
        
        try {
            DataSink dataSink = createDataSinkByType(configuration);

            // Set configuration on the data sink for name and other properties
            if (dataSink instanceof DatabaseDataSink) {
                ((DatabaseDataSink) dataSink).setConfiguration(configuration);
            } else if (dataSink instanceof FileSystemDataSink) {
                ((FileSystemDataSink) dataSink).setConfiguration(configuration);
            }

            return dataSink;
        } catch (Exception e) {
            throw DataSinkException.configurationError(
                "Failed to create data sink: " + configuration.getName(), e);
        }
    }
    
    /**
     * Check if a data sink type is supported.
     * 
     * @param type The data sink type
     * @return true if the type is supported
     */
    public boolean isTypeSupported(DataSinkType type) {
        return type != null && isBuiltInType(type);
    }
    
    /**
     * Register a custom data sink provider.
     * 
     * @param type The data sink type identifier
     * @param provider A supplier that creates instances of the custom data sink
     */
    public void registerCustomProvider(String type, Supplier<DataSink> provider) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Data sink type cannot be null or empty");
        }
        
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        
        customProviders.put(type.toLowerCase(), provider);
        LOGGER.info("Registered custom data sink provider for type: {}", type);
    }
    
    // Private helper methods
    
    /**
     * Create a data sink based on its type.
     */
    private DataSink createDataSinkByType(DataSinkConfiguration configuration) throws DataSinkException {
        DataSinkType type = configuration.getSinkType();
        
        if (type == null) {
            throw DataSinkException.configurationError("Unknown data sink type: " + configuration.getType());
        }
        
        switch (type) {
            case DATABASE:
                return createDatabaseDataSink(configuration);
                
            case FILE_SYSTEM:
                return createFileSystemDataSink(configuration);
                
            case MESSAGE_QUEUE:
            case REST_API:
            case CACHE:
            case CUSTOM:
            default:
                throw DataSinkException.configurationError("Unsupported data sink type: " + type);
        }
    }
    
    /**
     * Create a database data sink.
     */
    private DataSink createDatabaseDataSink(DataSinkConfiguration configuration) throws DataSinkException {
        LOGGER.debug("Creating database data sink for: {}", configuration.getName());
        return new DatabaseDataSink();
    }
    
    /**
     * Create a file system data sink.
     */
    private DataSink createFileSystemDataSink(DataSinkConfiguration configuration) throws DataSinkException {
        LOGGER.debug("Creating file system data sink for: {}", configuration.getName());
        return new FileSystemDataSink();
    }
    
    /**
     * Check if a type is a built-in type.
     */
    private boolean isBuiltInType(DataSinkType type) {
        return type == DataSinkType.DATABASE ||
               type == DataSinkType.FILE_SYSTEM;
    }
}
