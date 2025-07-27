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
 * Categories for organizing demos by their primary purpose and complexity level.
 *
 * This enum is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public enum DemoCategory {
    
    /**
     * Basic usage examples showing fundamental concepts and simple operations.
     * These demos are ideal for newcomers to understand core functionality.
     */
    BASIC_USAGE("Basic Usage", "Fundamental concepts and simple operations", 1),
    
    /**
     * Financial domain examples demonstrating real-world use cases.
     * These showcase practical applications in financial services.
     */
    FINANCIAL_EXAMPLES("Financial Examples", "Real-world financial use cases and validation", 2),
    
    /**
     * API layer demonstrations showing different levels of abstraction.
     * These help users understand when to use each API approach.
     */
    API_DEMONSTRATIONS("API Demonstrations", "Layered API design and usage patterns", 3),
    
    /**
     * Performance monitoring and optimization examples.
     * These focus on metrics, monitoring, and performance tuning.
     */
    PERFORMANCE_MONITORING("Performance Monitoring", "Metrics, monitoring, and optimization", 4),
    
    /**
     * Advanced integration patterns and complex scenarios.
     * These demonstrate sophisticated use cases and integration techniques.
     */
    ADVANCED_INTEGRATION("Advanced Integration", "Complex scenarios and integration patterns", 5),
    
    /**
     * Infrastructure and technical demonstrations.
     * These show low-level technical features and system integration.
     */
    INFRASTRUCTURE("Infrastructure", "Technical features and system integration", 6);
    
    private final String displayName;
    private final String description;
    private final int sortOrder;
    
    DemoCategory(String displayName, String description, int sortOrder) {
        this.displayName = displayName;
        this.description = description;
        this.sortOrder = sortOrder;
    }
    
    /**
     * Get the display name for this category.
     * @return Human-readable category name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the description of this category.
     * @return Detailed description of what this category contains
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the sort order for this category.
     * @return Numeric sort order (lower numbers appear first)
     */
    public int getSortOrder() {
        return sortOrder;
    }
    
    /**
     * Get an emoji icon for this category.
     * @return Unicode emoji representing this category
     */
    public String getIcon() {
        return switch (this) {
            case BASIC_USAGE -> "🎯";
            case FINANCIAL_EXAMPLES -> "🏦";
            case API_DEMONSTRATIONS -> "🚀";
            case PERFORMANCE_MONITORING -> "⚡";
            case ADVANCED_INTEGRATION -> "🔧";
            case INFRASTRUCTURE -> "🏗️";
        };
    }
    
    @Override
    public String toString() {
        return getIcon() + " " + displayName;
    }
}
