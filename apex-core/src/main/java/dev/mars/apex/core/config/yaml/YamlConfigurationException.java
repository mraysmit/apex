package dev.mars.apex.core.config.yaml;

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
 * Exception thrown when there are issues with YAML configuration loading or processing.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class YamlConfigurationException extends Exception {
    
    /**
     * Constructs a new YamlConfigurationException with the specified detail message.
     * 
     * @param message the detail message
     */
    public YamlConfigurationException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new YamlConfigurationException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public YamlConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new YamlConfigurationException with the specified cause.
     * 
     * @param cause the cause
     */
    public YamlConfigurationException(Throwable cause) {
        super(cause);
    }
}
