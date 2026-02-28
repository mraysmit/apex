package dev.mars.apex.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Centralized utility for resolving environment variables and system properties
 * in configuration values.
 * 
 * This class provides a single source of truth for property placeholder resolution
 * across the APEX framework, ensuring consistent behavior in:
 * - ConfigurationLoader
 * - YamlDataSource  
 * - DataSourceResolver
 * 
 * Supported placeholder formats:
 * - ${VAR} - resolved from system properties, then environment variables
 * - ${VAR:default} - with default value if property not found
 * - $(VAR) - alternative syntax, same resolution order
 * - $(VAR:default) - alternative syntax with default value
 * 
 * Resolution order:
 * 1. System properties (System.getProperty())
 * 2. Environment variables (System.getenv())
 * 3. Default value (if specified)
 * 
 * @author Mark A Ray-Smith Cityline Ltd
 * @since 2025-01-19
 * @version 1.0.0
 */
public final class PropertyResolver {

    private static final Logger logger = LoggerFactory.getLogger(PropertyResolver.class);

    // Regex patterns for placeholder detection
    private static final Pattern CURLY_BRACE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final Pattern PARENTHESIS_PATTERN = Pattern.compile("\\$\\(([^)]+)\\)");

    // Private constructor to prevent instantiation
    private PropertyResolver() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Resolve all property placeholders in the given value.
     * Throws an exception if any required placeholder cannot be resolved.
     *
     * @param value The value containing property placeholders
     * @return The value with all placeholders resolved
     * @throws PropertyResolutionException if a required property is not found
     */
    public static String resolve(String value) throws PropertyResolutionException {
        return resolve(value, true);
    }

    /**
     * Resolve all property placeholders in the given value.
     *
     * @param value The value containing property placeholders
     * @param throwOnUnresolved If true, throws an exception for unresolved placeholders;
     *                          if false, leaves unresolved placeholders as-is
     * @return The value with placeholders resolved
     * @throws PropertyResolutionException if throwOnUnresolved is true and a required property is not found
     */
    public static String resolve(String value, boolean throwOnUnresolved) throws PropertyResolutionException {
        if (value == null || (!value.contains("${") && !value.contains("$("))) {
            return value;
        }

        logger.debug("Resolving properties in value: {}", maskSensitiveValue(value));

        String result = value;

        // First resolve ${VAR} and ${VAR:default} patterns
        if (result.contains("${")) {
            result = resolvePattern(result, CURLY_BRACE_PATTERN);
        }

        // Then resolve $(VAR) and $(VAR:default) patterns
        if (result.contains("$(")) {
            result = resolvePattern(result, PARENTHESIS_PATTERN);
        }

        // Validate no unresolved placeholders remain (if requested)
        if (throwOnUnresolved) {
            validateNoUnresolvedPlaceholders(result);
        }

        logger.debug("Property resolution completed: {}", maskSensitiveValue(result));

        return result;
    }

    /**
     * Resolve placeholders matching the given pattern.
     */
    private static String resolvePattern(String value, Pattern pattern) {
        Matcher matcher = pattern.matcher(value);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String resolved = resolveSingleProperty(placeholder);
            matcher.appendReplacement(result, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * Resolve a single property placeholder.
     *
     * @param placeholder The placeholder content (e.g., "VAR" or "VAR:default")
     * @return The resolved value, or the original placeholder format if not found
     */
    private static String resolveSingleProperty(String placeholder) {
        // Handle default values: VAR:default
        String[] parts = placeholder.split(":", 2);
        String key = parts[0].trim();
        String defaultValue = parts.length > 1 ? parts[1].trim() : null;

        // Resolution order: System Properties -> Environment Variables -> Default
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        if (value == null && defaultValue != null) {
            value = defaultValue;
        }
        if (value == null) {
            // Return the original placeholder if property not found
            return "${" + placeholder + "}";
        }

        // Log resolution (mask sensitive values)
        String logValue = isSensitiveProperty(key) ? "[MASKED]" : value;
        logger.debug("Resolved property: ${} -> {}", key, logValue);

        return value;
    }

    /**
     * Validate that no unresolved property placeholders remain in the value.
     *
     * @param value The value to check for unresolved placeholders
     * @throws PropertyResolutionException if unresolved placeholders are found
     */
    public static void validateNoUnresolvedPlaceholders(String value) throws PropertyResolutionException {
        if (value == null) {
            return;
        }

        // Check for unresolved ${...} placeholders
        Matcher curlyMatcher = CURLY_BRACE_PATTERN.matcher(value);
        if (curlyMatcher.find()) {
            String placeholder = curlyMatcher.group(1);
            String propertyName = placeholder.split(":", 2)[0].trim();
            throw new PropertyResolutionException("Property not found: " + propertyName);
        }

        // Check for unresolved $(...) placeholders
        Matcher parenMatcher = PARENTHESIS_PATTERN.matcher(value);
        if (parenMatcher.find()) {
            String placeholder = parenMatcher.group(1);
            String propertyName = placeholder.split(":", 2)[0].trim();
            throw new PropertyResolutionException("Property not found: " + propertyName);
        }
    }

    /**
     * Check if a value contains any property placeholders.
     *
     * @param value The value to check
     * @return true if the value contains placeholders
     */
    public static boolean containsPlaceholders(String value) {
        if (value == null) {
            return false;
        }
        return value.contains("${") || value.contains("$(");
    }

    /**
     * Check if a property key represents sensitive information.
     *
     * @param key The property key
     * @return true if the property is considered sensitive
     */
    public static boolean isSensitiveProperty(String key) {
        if (key == null) {
            return false;
        }
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("password") ||
               lowerKey.contains("secret") ||
               lowerKey.contains("token") ||
               lowerKey.contains("key") ||
               lowerKey.contains("pwd") ||
               lowerKey.contains("credential");
    }

    /**
     * Mask sensitive values for logging.
     *
     * @param value The value to potentially mask
     * @return The masked value if it appears to contain sensitive data
     */
    public static String maskSensitiveValue(String value) {
        if (value == null) {
            return null;
        }

        // If the value contains ${PASSWORD}, ${SECRET}, etc., mask the whole thing
        String lowerValue = value.toLowerCase();
        if (lowerValue.contains("password") || lowerValue.contains("secret") ||
            lowerValue.contains("token") || lowerValue.contains("key") ||
            lowerValue.contains("pwd") || lowerValue.contains("credential")) {
            return "[MASKED_VALUE_WITH_SENSITIVE_PLACEHOLDERS]";
        }

        return value;
    }

    /**
     * Exception thrown when a required property cannot be resolved.
     */
    public static class PropertyResolutionException extends RuntimeException {
        
        public PropertyResolutionException(String message) {
            super(message);
        }

        public PropertyResolutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
