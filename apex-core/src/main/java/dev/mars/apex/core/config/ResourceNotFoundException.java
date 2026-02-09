package dev.mars.apex.core.config;

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
 * Exception thrown when a resource cannot be found by the ResourceResolver.
 * 
 * <p>This exception is thrown when a resource reference cannot be resolved
 * from either the classpath or filesystem based on the configured resolution
 * strategy.</p>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-08
 * @see ResourceResolver
 */
public class ResourceNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new ResourceNotFoundException with a message.
     * 
     * @param message The detail message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Create a new ResourceNotFoundException with a message and cause.
     * 
     * @param message The detail message
     * @param cause The underlying cause
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
