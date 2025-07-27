package dev.mars.rulesengine.core.engine.model.metadata;

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
 * Enumeration representing the complexity level of a rule.
 *
 * This enum is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public enum RuleComplexity {
    /**
     * Simple rules with basic conditions (e.g., single field comparisons).
     */
    LOW("Low", "Simple rules with basic conditions", 1),
    
    /**
     * Moderate complexity rules with multiple conditions or simple calculations.
     */
    MEDIUM("Medium", "Moderate complexity with multiple conditions", 2),
    
    /**
     * Complex rules with advanced logic, nested conditions, or calculations.
     */
    HIGH("High", "Complex rules with advanced logic", 3),
    
    /**
     * Very complex rules with extensive calculations, external data access, or complex algorithms.
     */
    VERY_HIGH("Very High", "Very complex rules with extensive processing", 4);
    
    private final String displayName;
    private final String description;
    private final int complexityScore;
    
    RuleComplexity(String displayName, String description, int complexityScore) {
        this.displayName = displayName;
        this.description = description;
        this.complexityScore = complexityScore;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getComplexityScore() {
        return complexityScore;
    }
    
    /**
     * Determine complexity based on rule condition characteristics.
     */
    public static RuleComplexity fromCondition(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            return LOW;
        }
        
        String normalizedCondition = condition.toLowerCase();
        int complexityIndicators = 0;
        
        // Count complexity indicators
        if (normalizedCondition.contains("&&") || normalizedCondition.contains("||")) {
            complexityIndicators++;
        }
        if (normalizedCondition.contains("matches(") || normalizedCondition.contains("regex")) {
            complexityIndicators++;
        }
        if (normalizedCondition.contains("foreach") || normalizedCondition.contains("select")) {
            complexityIndicators += 2;
        }
        if (normalizedCondition.contains("new ") || normalizedCondition.contains("@")) {
            complexityIndicators++;
        }
        if (normalizedCondition.length() > 200) {
            complexityIndicators++;
        }
        
        return switch (complexityIndicators) {
            case 0, 1 -> LOW;
            case 2, 3 -> MEDIUM;
            case 4, 5 -> HIGH;
            default -> VERY_HIGH;
        };
    }
}
