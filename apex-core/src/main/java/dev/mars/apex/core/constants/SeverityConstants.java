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

package dev.mars.apex.core.constants;

import java.util.Map;
import java.util.Set;

/**
 * Centralized constants for severity levels used throughout the APEX rules engine.
 * 
 * This class provides a single source of truth for all severity-related constants,
 * including valid severity values, default severity, and priority mappings for
 * severity aggregation logic.
 * 
 * <p>Supported severity levels (in order of increasing severity):
 * <ul>
 *   <li><strong>INFO</strong> - Informational messages, successful processing</li>
 *   <li><strong>WARNING</strong> - Potential issues that don't prevent processing</li>
 *   <li><strong>ERROR</strong> - Critical issues that require immediate attention</li>
 * </ul>
 * 
 * <p>Usage examples:
 * <pre>{@code
 * // Using severity constants
 * String severity = SeverityConstants.ERROR;
 * 
 * // Checking if severity is valid
 * if (SeverityConstants.VALID_SEVERITIES.contains(userInput)) {
 *     // Process valid severity
 * }
 * 
 * // Getting priority for aggregation
 * int priority = SeverityConstants.SEVERITY_PRIORITY.get(severity);
 * }</pre>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-23
 * @version 1.0
 */
public final class SeverityConstants {
    
    /**
     * Error severity level - indicates critical issues requiring immediate attention.
     * Used for: mandatory field violations, compliance failures, critical business logic errors.
     */
    public static final String ERROR = "ERROR";
    
    /**
     * Warning severity level - indicates potential issues that don't prevent processing.
     * Used for: threshold breaches, unusual patterns, non-critical validation failures.
     */
    public static final String WARNING = "WARNING";
    
    /**
     * Info severity level - indicates informational messages and successful processing.
     * Used for: successful validations, processing notifications, data enrichment info.
     */
    public static final String INFO = "INFO";
    
    /**
     * Set of all valid severity values.
     * Used for validation to ensure only supported severity levels are used.
     */
    public static final Set<String> VALID_SEVERITIES = Set.of(ERROR, WARNING, INFO);
    
    /**
     * Default severity level used when no severity is specified.
     * Defaults to ERROR to ensure validation failures are properly reported.
     * This ensures that missing required fields and other validation issues
     * cause proper workflow failures instead of being silently recovered.
     */
    public static final String DEFAULT_SEVERITY = ERROR;
    
    /**
     * Priority mapping for severity levels used in aggregation logic.
     * Higher numbers indicate higher severity/priority.
     * 
     * <p>Priority values:
     * <ul>
     *   <li>ERROR = 3 (highest priority)</li>
     *   <li>WARNING = 2 (medium priority)</li>
     *   <li>INFO = 1 (lowest priority)</li>
     * </ul>
     * 
     * <p>This mapping is used by {@link dev.mars.apex.core.engine.model.RuleGroupSeverityAggregator}
     * to determine the most severe result when aggregating multiple rule results.
     */
    public static final Map<String, Integer> SEVERITY_PRIORITY = Map.of(
        ERROR, 3,
        WARNING, 2,
        INFO, 1
    );
    
    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static members.
     */
    private SeverityConstants() {
        throw new UnsupportedOperationException("SeverityConstants is a utility class and cannot be instantiated");
    }
    
    /**
     * Check if a severity value is valid.
     * 
     * @param severity The severity value to check
     * @return true if the severity is valid, false otherwise
     */
    public static boolean isValidSeverity(String severity) {
        return severity != null && VALID_SEVERITIES.contains(severity);
    }
    
    /**
     * Get the priority value for a severity level.
     *
     * @param severity The severity level
     * @return Priority value (higher = more severe), or 1 (INFO priority) if severity is invalid
     */
    public static int getSeverityPriority(String severity) {
        if (severity == null) {
            return 1; // Default to INFO priority for null
        }
        return SEVERITY_PRIORITY.getOrDefault(severity, 1);
    }
    
    /**
     * Get the highest severity from two severity values.
     * 
     * @param severity1 First severity value
     * @param severity2 Second severity value
     * @return The severity with higher priority, or DEFAULT_SEVERITY if both are invalid
     */
    public static String getHigherSeverity(String severity1, String severity2) {
        if (!isValidSeverity(severity1) && !isValidSeverity(severity2)) {
            return DEFAULT_SEVERITY;
        }
        if (!isValidSeverity(severity1)) {
            return severity2;
        }
        if (!isValidSeverity(severity2)) {
            return severity1;
        }
        
        int priority1 = getSeverityPriority(severity1);
        int priority2 = getSeverityPriority(severity2);
        
        return priority1 >= priority2 ? severity1 : severity2;
    }
}
