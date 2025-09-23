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

package dev.mars.apex.core.engine.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles severity aggregation for rule groups following business logic:
 * - AND Groups: Use highest severity of failed rules, or highest of all if all pass
 * - OR Groups: Use severity of first matching rule, or highest of all evaluated
 * 
 * This class implements the Single Responsibility Principle by focusing solely
 * on severity aggregation logic for rule groups.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-23
 * @version 1.0
 */
public class RuleGroupSeverityAggregator {
    
    private static final Logger logger = LoggerFactory.getLogger(RuleGroupSeverityAggregator.class);
    
    /**
     * Severity priority mapping for comparison.
     * Higher numbers indicate higher severity.
     */
    private static final Map<String, Integer> SEVERITY_PRIORITY = Map.of(
        "ERROR", 3,
        "WARNING", 2,
        "INFO", 1
    );
    
    /**
     * Aggregate severity from multiple rule results based on group operator.
     * 
     * @param results List of individual rule results from the group
     * @param isAndOperator true for AND groups, false for OR groups
     * @return Aggregated severity level (ERROR, WARNING, or INFO)
     */
    public String aggregateSeverity(List<RuleResult> results, boolean isAndOperator) {
        if (results == null || results.isEmpty()) {
            logger.debug("No rule results provided, defaulting to INFO severity");
            return "INFO";
        }
        
        logger.debug("Aggregating severity for {} rule results with {} operator", 
                    results.size(), isAndOperator ? "AND" : "OR");
        
        String aggregatedSeverity = isAndOperator ? 
            aggregateAndGroupSeverity(results) : 
            aggregateOrGroupSeverity(results);
            
        logger.debug("Aggregated severity: {}", aggregatedSeverity);
        return aggregatedSeverity;
    }
    
    /**
     * Aggregate severity for AND groups.
     * Business Logic:
     * - If any rule fails (not triggered), use highest severity of failed rules
     * - If all rules pass (triggered), use highest severity of all rules
     * 
     * @param results List of rule results
     * @return Aggregated severity
     */
    private String aggregateAndGroupSeverity(List<RuleResult> results) {
        List<RuleResult> failedRules = results.stream()
            .filter(r -> !r.isTriggered())
            .collect(Collectors.toList());
            
        if (!failedRules.isEmpty()) {
            logger.debug("AND group has {} failed rules, using highest severity of failed rules", 
                        failedRules.size());
            return getHighestSeverity(failedRules);
        } else {
            logger.debug("AND group has all rules passing, using highest severity of all rules");
            return getHighestSeverity(results);
        }
    }
    
    /**
     * Aggregate severity for OR groups.
     * Business Logic:
     * - Use severity of first matching (triggered) rule
     * - If no rules match, use highest severity of all evaluated rules
     * 
     * @param results List of rule results
     * @return Aggregated severity
     */
    private String aggregateOrGroupSeverity(List<RuleResult> results) {
        // Find first matching rule
        RuleResult firstMatch = results.stream()
            .filter(RuleResult::isTriggered)
            .findFirst()
            .orElse(null);
            
        if (firstMatch != null) {
            logger.debug("OR group using severity '{}' from first matching rule: {}", 
                        firstMatch.getSeverity(), firstMatch.getRuleName());
            return firstMatch.getSeverity();
        } else {
            logger.debug("OR group has no matching rules, using highest severity of all evaluated rules");
            return getHighestSeverity(results);
        }
    }
    
    /**
     * Get the highest severity from a list of rule results.
     * 
     * @param results List of rule results
     * @return Highest severity level
     */
    private String getHighestSeverity(List<RuleResult> results) {
        if (results == null || results.isEmpty()) {
            return "INFO";
        }
        
        String highestSeverity = results.stream()
            .map(RuleResult::getSeverity)
            .filter(severity -> severity != null && SEVERITY_PRIORITY.containsKey(severity))
            .max((s1, s2) -> Integer.compare(
                SEVERITY_PRIORITY.get(s1), 
                SEVERITY_PRIORITY.get(s2)))
            .orElse("INFO");
            
        logger.debug("Highest severity from {} results: {}", results.size(), highestSeverity);
        return highestSeverity;
    }
    
    /**
     * Validate that a severity level is supported.
     * 
     * @param severity The severity to validate
     * @return true if the severity is valid
     */
    public boolean isValidSeverity(String severity) {
        return severity != null && SEVERITY_PRIORITY.containsKey(severity);
    }
    
    /**
     * Get the priority value for a severity level.
     * 
     * @param severity The severity level
     * @return Priority value (higher = more severe)
     */
    public int getSeverityPriority(String severity) {
        return SEVERITY_PRIORITY.getOrDefault(severity, 1); // Default to INFO priority
    }
}
