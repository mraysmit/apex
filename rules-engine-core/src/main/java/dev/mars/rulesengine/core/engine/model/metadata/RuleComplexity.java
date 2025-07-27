package dev.mars.rulesengine.core.engine.model.metadata;

/**
 * Enumeration representing the complexity level of a rule.
 * This can be used for performance optimization, monitoring, and resource allocation.
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
