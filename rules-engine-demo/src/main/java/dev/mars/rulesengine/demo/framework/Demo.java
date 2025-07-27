package dev.mars.rulesengine.demo.framework;

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
 * Standard interface for all demo classes in the rules engine demo module.
 *
 * This interface is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Standard interface for all demo classes in the rules engine demo module.
 * This provides a consistent structure and behavior across all demonstrations.
 */
public interface Demo {
    
    /**
     * Get the name of this demo.
     * @return A short, descriptive name for the demo
     */
    String getName();
    
    /**
     * Get a detailed description of what this demo demonstrates.
     * @return A comprehensive description of the demo's purpose and features
     */
    String getDescription();
    
    /**
     * Get the category this demo belongs to.
     * @return The demo category for organization purposes
     */
    DemoCategory getCategory();
    
    /**
     * Run the demo in interactive mode.
     * This may include user prompts, menus, or other interactive elements.
     */
    void run();
    
    /**
     * Run the demo in non-interactive mode.
     * This should run without any user input, suitable for automated testing.
     */
    void runNonInteractive();
    
    /**
     * Get the estimated runtime for this demo in seconds.
     * @return Estimated runtime in seconds, or -1 if unknown
     */
    default int getEstimatedRuntimeSeconds() {
        return -1;
    }
    
    /**
     * Check if this demo requires external dependencies or setup.
     * @return true if the demo has prerequisites, false otherwise
     */
    default boolean hasPrerequisites() {
        return false;
    }
    
    /**
     * Get a list of prerequisites for this demo.
     * @return Array of prerequisite descriptions, empty if none
     */
    default String[] getPrerequisites() {
        return new String[0];
    }
    
    /**
     * Check if this demo is currently available/runnable.
     * @return true if the demo can be run, false if prerequisites are missing
     */
    default boolean isAvailable() {
        return true;
    }
}
