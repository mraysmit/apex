package dev.mars.rulesengine.core.exception;

/**
 * Exception thrown when there are issues with rule configuration.
 * This includes problems with rule definitions, invalid parameters, or configuration conflicts.
 */
public class RuleConfigurationException extends RuleEngineException {
    private static final long serialVersionUID = 1L;
    
    private final String configurationElement;
    private final String expectedFormat;
    
    public RuleConfigurationException(String configurationElement, String message) {
        super("RULE_CONFIGURATION_ERROR", message, "Configuration element: " + configurationElement);
        this.configurationElement = configurationElement;
        this.expectedFormat = null;
    }
    
    public RuleConfigurationException(String configurationElement, String message, String expectedFormat) {
        super("RULE_CONFIGURATION_ERROR", message, "Configuration element: " + configurationElement);
        this.configurationElement = configurationElement;
        this.expectedFormat = expectedFormat;
    }
    
    public RuleConfigurationException(String configurationElement, String message, Throwable cause) {
        super("RULE_CONFIGURATION_ERROR", message, "Configuration element: " + configurationElement, cause);
        this.configurationElement = configurationElement;
        this.expectedFormat = null;
    }
    
    public String getConfigurationElement() {
        return configurationElement;
    }
    
    public String getExpectedFormat() {
        return expectedFormat;
    }
    
    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder(super.getDetailedMessage());
        if (expectedFormat != null && !expectedFormat.trim().isEmpty()) {
            sb.append("\nExpected format: ").append(expectedFormat);
        }
        return sb.toString();
    }
}
