package dev.mars.apex.core.service.enrichment;

/**
 * Exception thrown when enrichment configuration is invalid or cannot be processed.
 * This represents a critical configuration error that should be logged at ERROR/SEVERE level
 * rather than WARNING level.
 * 
 * @author APEX Team
 */
public class EnrichmentConfigurationException extends RuntimeException {
    
    private final String enrichmentId;
    private final String configurationIssue;
    
    /**
     * Creates a new EnrichmentConfigurationException.
     * 
     * @param enrichmentId The ID of the enrichment that has the configuration issue
     * @param configurationIssue Description of the configuration problem
     */
    public EnrichmentConfigurationException(String enrichmentId, String configurationIssue) {
        super("Enrichment configuration error for '" + enrichmentId + "': " + configurationIssue);
        this.enrichmentId = enrichmentId;
        this.configurationIssue = configurationIssue;
    }
    
    /**
     * Creates a new EnrichmentConfigurationException with a root cause.
     * 
     * @param enrichmentId The ID of the enrichment that has the configuration issue
     * @param configurationIssue Description of the configuration problem
     * @param cause The underlying exception that caused this configuration error
     */
    public EnrichmentConfigurationException(String enrichmentId, String configurationIssue, Throwable cause) {
        super("Enrichment configuration error for '" + enrichmentId + "': " + configurationIssue, cause);
        this.enrichmentId = enrichmentId;
        this.configurationIssue = configurationIssue;
    }
    
    /**
     * Creates a new EnrichmentConfigurationException with just a message and cause.
     * 
     * @param message The error message
     * @param cause The underlying exception
     */
    public EnrichmentConfigurationException(String message, Throwable cause) {
        super(message, cause);
        this.enrichmentId = null;
        this.configurationIssue = message;
    }
    
    /**
     * Gets the ID of the enrichment that has the configuration issue.
     * 
     * @return The enrichment ID, or null if not specified
     */
    public String getEnrichmentId() {
        return enrichmentId;
    }
    
    /**
     * Gets the description of the configuration issue.
     * 
     * @return The configuration issue description
     */
    public String getConfigurationIssue() {
        return configurationIssue;
    }
}
