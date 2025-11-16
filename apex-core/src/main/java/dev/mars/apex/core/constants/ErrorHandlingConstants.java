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

import java.util.Set;

/**
 * Centralized constants for error handling strategies used in rule groups and enrichment groups.
 * 
 * This class provides a single source of truth for all error handling strategy constants,
 * preventing hardcoded strings throughout the codebase.
 * 
 * <p>Supported error handling strategies:
 * <ul>
 *   <li><strong>FAIL_FAST</strong> - Stop immediately and return error (default)</li>
 *   <li><strong>CONTINUE_ON_ERROR</strong> - Log error and continue with remaining items</li>
 *   <li><strong>SKIP_ON_ERROR</strong> - Skip the failed item and continue</li>
 * </ul>
 * 
 * <p><strong>Note:</strong> This is different from {@code stop-on-first-failure} which controls
 * business logic short-circuiting (AND/OR evaluation). Error handling strategies control
 * exception handling behavior during rule/enrichment group evaluation.
 * 
 * <p>Usage examples:
 * <pre>{@code
 * // Using error handling constants
 * String strategy = ErrorHandlingConstants.FAIL_FAST;
 * 
 * // Checking if strategy is valid
 * if (ErrorHandlingConstants.isValidStrategy(userInput)) {
 *     // Process valid strategy
 * }
 * 
 * // Getting default strategy
 * String defaultStrategy = ErrorHandlingConstants.DEFAULT_STRATEGY;
 * }</pre>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-16
 * @version 1.0
 */
public final class ErrorHandlingConstants {
    
    /**
     * Fail-fast error handling strategy - stop immediately and return error (default).
     * Used when: Critical errors where continuing would be dangerous or meaningless.
     */
    public static final String FAIL_FAST = "fail-fast";

    /**
     * Continue-on-error error handling strategy - log error and continue with remaining items.
     * Used when: Non-critical errors where processing should continue despite failures.
     */
    public static final String CONTINUE_ON_ERROR = "continue-on-error";

    /**
     * Skip-on-error error handling strategy - skip the failed item and continue.
     * Used when: Optional items where failures should be silently skipped.
     */
    public static final String SKIP_ON_ERROR = "skip-on-error";
    
    /**
     * Set of all valid error handling strategy values.
     * Used for validation to ensure only supported strategies are used.
     */
    public static final Set<String> VALID_STRATEGIES = Set.of(FAIL_FAST, CONTINUE_ON_ERROR, SKIP_ON_ERROR);
    
    /**
     * Default error handling strategy used when no strategy is specified.
     * Defaults to FAIL_FAST for safety and backward compatibility.
     */
    public static final String DEFAULT_STRATEGY = FAIL_FAST;
    
    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static members.
     */
    private ErrorHandlingConstants() {
        throw new UnsupportedOperationException("ErrorHandlingConstants is a utility class and cannot be instantiated");
    }
    
    /**
     * Check if an error handling strategy value is valid.
     * 
     * @param strategy The strategy value to check
     * @return true if the strategy is valid, false otherwise
     */
    public static boolean isValidStrategy(String strategy) {
        return strategy != null && VALID_STRATEGIES.contains(strategy);
    }
    
    /**
     * Get a valid error handling strategy, returning the default if the input is invalid.
     * 
     * @param strategy The strategy value to validate
     * @return The input strategy if valid, otherwise DEFAULT_STRATEGY
     */
    public static String getValidStrategyOrDefault(String strategy) {
        return isValidStrategy(strategy) ? strategy : DEFAULT_STRATEGY;
    }
}

