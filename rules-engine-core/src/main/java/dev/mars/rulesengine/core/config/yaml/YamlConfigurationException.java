package dev.mars.rulesengine.core.config.yaml;

/**
 * Exception thrown when there are issues with YAML configuration loading or processing.
 */
public class YamlConfigurationException extends Exception {
    
    /**
     * Constructs a new YamlConfigurationException with the specified detail message.
     * 
     * @param message the detail message
     */
    public YamlConfigurationException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new YamlConfigurationException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public YamlConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new YamlConfigurationException with the specified cause.
     * 
     * @param cause the cause
     */
    public YamlConfigurationException(Throwable cause) {
        super(cause);
    }
}
