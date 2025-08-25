package dev.mars.apex.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class for type-safe extraction of data from dynamic structures like YAML/JSON.
 * Prevents ClassCastException at runtime by validating types before casting.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-25
 * @version 1.0
 */
public class TypeSafeDataExtractor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TypeSafeDataExtractor.class);
    
    /**
     * Safely cast an object to Map<String, Object> with validation.
     * 
     * @param obj The object to cast
     * @param context Description of where this cast is happening (for logging)
     * @return Optional containing the map if cast is safe, empty otherwise
     */
    @SuppressWarnings("unchecked")
    public static Optional<Map<String, Object>> safeMapCast(Object obj, String context) {
        if (obj == null) {
            LOGGER.debug("Null object encountered in context: {}", context);
            return Optional.empty();
        }
        
        if (!(obj instanceof Map)) {
            LOGGER.warn("Expected Map but got {} in context: {}", obj.getClass().getSimpleName(), context);
            return Optional.empty();
        }
        
        Map<?, ?> rawMap = (Map<?, ?>) obj;
        
        // Validate that all keys are strings
        for (Object key : rawMap.keySet()) {
            if (!(key instanceof String)) {
                LOGGER.warn("Map contains non-String key {} in context: {}", key, context);
                return Optional.empty();
            }
        }
        
        return Optional.of((Map<String, Object>) rawMap);
    }
    
    /**
     * Safely cast an object to List<Map<String, Object>> with validation.
     * 
     * @param obj The object to cast
     * @param context Description of where this cast is happening (for logging)
     * @return Optional containing the list if cast is safe, empty otherwise
     */
    @SuppressWarnings("unchecked")
    public static Optional<List<Map<String, Object>>> safeListMapCast(Object obj, String context) {
        if (obj == null) {
            LOGGER.debug("Null object encountered in context: {}", context);
            return Optional.empty();
        }
        
        if (!(obj instanceof List)) {
            LOGGER.warn("Expected List but got {} in context: {}", obj.getClass().getSimpleName(), context);
            return Optional.empty();
        }
        
        List<?> rawList = (List<?>) obj;
        
        // Validate that all elements are Maps with String keys
        for (int i = 0; i < rawList.size(); i++) {
            Object element = rawList.get(i);
            if (!safeMapCast(element, context + "[" + i + "]").isPresent()) {
                return Optional.empty();
            }
        }
        
        return Optional.of((List<Map<String, Object>>) rawList);
    }
    
    /**
     * Safely extract a String value from a map.
     * 
     * @param map The map to extract from
     * @param key The key to look for
     * @param defaultValue Default value if key is missing or not a String
     * @return The string value or default
     */
    public static String safeGetString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        if (value != null) {
            LOGGER.debug("Expected String for key '{}' but got {}, using default", key, value.getClass().getSimpleName());
        }
        return defaultValue;
    }
    
    /**
     * Safely extract a Number value from a map.
     * 
     * @param map The map to extract from
     * @param key The key to look for
     * @param defaultValue Default value if key is missing or not a Number
     * @return The number value or default
     */
    public static Number safeGetNumber(Map<String, Object> map, String key, Number defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value != null) {
            LOGGER.debug("Expected Number for key '{}' but got {}, using default", key, value.getClass().getSimpleName());
        }
        return defaultValue;
    }
    
    /**
     * Safely extract a Boolean value from a map.
     * 
     * @param map The map to extract from
     * @param key The key to look for
     * @param defaultValue Default value if key is missing or not a Boolean
     * @return The boolean value or default
     */
    public static Boolean safeGetBoolean(Map<String, Object> map, String key, Boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            LOGGER.debug("Expected Boolean for key '{}' but got {}, using default", key, value.getClass().getSimpleName());
        }
        return defaultValue;
    }
    
    /**
     * Validate that a YAML dataset has the expected structure.
     * 
     * @param dataset The dataset to validate
     * @param expectedKeyField The expected key field name
     * @param context Description for logging
     * @return true if valid, false otherwise
     */
    public static boolean validateDatasetStructure(Object dataset, String expectedKeyField, String context) {
        Optional<List<Map<String, Object>>> dataList = safeListMapCast(dataset, context);
        if (!dataList.isPresent()) {
            return false;
        }
        
        // Check that all entries have the expected key field
        for (int i = 0; i < dataList.get().size(); i++) {
            Map<String, Object> entry = dataList.get().get(i);
            if (!entry.containsKey(expectedKeyField)) {
                LOGGER.warn("Dataset entry {} missing expected key field '{}' in context: {}", 
                           i, expectedKeyField, context);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Create a runtime validation exception with context information.
     * 
     * @param message The error message
     * @param context The context where the error occurred
     * @param cause The underlying cause (optional)
     * @return A runtime exception with detailed information
     */
    public static RuntimeException createValidationException(String message, String context, Throwable cause) {
        String fullMessage = String.format("Data validation failed in %s: %s", context, message);
        return cause != null ? new RuntimeException(fullMessage, cause) : new RuntimeException(fullMessage);
    }
}
