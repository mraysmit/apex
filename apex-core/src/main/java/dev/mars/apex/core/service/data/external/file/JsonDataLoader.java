package dev.mars.apex.core.service.data.external.file;

import dev.mars.apex.core.config.datasource.FileFormatConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * JSON data loader implementation.
 * 
 * This class loads data from JSON (JavaScript Object Notation) files with support for
 * nested objects, arrays, and configurable root paths.
 * 
 * Features:
 * - JSONPath-like root path selection
 * - Array flattening options
 * - Nested object handling
 * - Custom encoding support
 * - Field mapping and filtering
 * 
 * Note: This is a basic JSON parser implementation. In production, you would
 * typically use a library like Jackson or Gson for more robust JSON parsing.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public class JsonDataLoader implements DataLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonDataLoader.class);
    
    @Override
    public List<Object> loadData(Path filePath, FileFormatConfig formatConfig) throws IOException {
        List<Object> results = new ArrayList<>();
        
        // Determine encoding
        String encoding = formatConfig != null && formatConfig.getEncoding() != null ? 
            formatConfig.getEncoding() : "UTF-8";
        
        try {
            // Read the entire file content
            String jsonContent = Files.readString(filePath, Charset.forName(encoding));
            
            // Parse JSON content
            Object parsedJson = parseJson(jsonContent);
            
            // Extract data based on root path
            Object extractedData = extractDataFromRootPath(parsedJson, formatConfig);
            
            // Convert to list format
            if (extractedData instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> dataList = (List<Object>) extractedData;
                results.addAll(dataList);
            } else if (extractedData != null) {
                results.add(extractedData);
            }
            
            // Apply field mappings and transformations
            results = applyTransformations(results, formatConfig);
            
            LOGGER.debug("Loaded {} objects from JSON file: {}", results.size(), filePath);
            
        } catch (IOException e) {
            LOGGER.error("Failed to load JSON file: {}", filePath, e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Failed to parse JSON file: {}", filePath, e);
            throw new IOException("JSON parsing failed", e);
        }
        
        return results;
    }
    
    @Override
    public boolean supportsFormat(FileFormatConfig formatConfig) {
        return formatConfig != null && 
               ("json".equalsIgnoreCase(formatConfig.getType()) || 
                formatConfig.getFileType() == FileFormatConfig.FileType.JSON);
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{"json", "jsonl"};
    }
    
    /**
     * Parse JSON content into a Java object structure.
     * This is a basic implementation - use Jackson or Gson in production.
     */
    private Object parseJson(String jsonContent) {
        jsonContent = jsonContent.trim();
        
        if (jsonContent.startsWith("{")) {
            return parseJsonObject(jsonContent);
        } else if (jsonContent.startsWith("[")) {
            return parseJsonArray(jsonContent);
        } else {
            // Try to parse as a primitive value
            return parseJsonValue(jsonContent);
        }
    }
    
    /**
     * Parse a JSON object.
     */
    private Map<String, Object> parseJsonObject(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        // Remove outer braces
        json = json.substring(1, json.length() - 1).trim();
        
        if (json.isEmpty()) {
            return result;
        }
        
        // Simple parsing - split by commas (not inside quotes or nested objects)
        List<String> keyValuePairs = splitJsonElements(json);
        
        for (String pair : keyValuePairs) {
            int colonIndex = findColonIndex(pair);
            if (colonIndex > 0) {
                String key = pair.substring(0, colonIndex).trim();
                String value = pair.substring(colonIndex + 1).trim();
                
                // Remove quotes from key
                if (key.startsWith("\"") && key.endsWith("\"")) {
                    key = key.substring(1, key.length() - 1);
                }
                
                // Parse the value
                Object parsedValue = parseJson(value);
                result.put(key, parsedValue);
            }
        }
        
        return result;
    }
    
    /**
     * Parse a JSON array.
     */
    private List<Object> parseJsonArray(String json) {
        List<Object> result = new ArrayList<>();
        
        // Remove outer brackets
        json = json.substring(1, json.length() - 1).trim();
        
        if (json.isEmpty()) {
            return result;
        }
        
        // Split array elements
        List<String> elements = splitJsonElements(json);
        
        for (String element : elements) {
            Object parsedElement = parseJson(element.trim());
            result.add(parsedElement);
        }
        
        return result;
    }
    
    /**
     * Parse a JSON primitive value.
     */
    private Object parseJsonValue(String value) {
        value = value.trim();
        
        if ("null".equals(value)) {
            return null;
        } else if ("true".equals(value)) {
            return true;
        } else if ("false".equals(value)) {
            return false;
        } else if (value.startsWith("\"") && value.endsWith("\"")) {
            // String value
            return value.substring(1, value.length() - 1);
        } else {
            // Try to parse as number
            try {
                if (value.contains(".") || value.contains("e") || value.contains("E")) {
                    return Double.parseDouble(value);
                } else {
                    return Long.parseLong(value);
                }
            } catch (NumberFormatException e) {
                // Return as string if not a valid number
                return value;
            }
        }
    }
    
    /**
     * Split JSON elements (objects, arrays, or values) by commas.
     */
    private List<String> splitJsonElements(String json) {
        List<String> elements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceLevel = 0;
        int bracketLevel = 0;
        boolean inQuotes = false;
        boolean escapeNext = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escapeNext) {
                current.append(c);
                escapeNext = false;
                continue;
            }
            
            if (c == '\\') {
                escapeNext = true;
                current.append(c);
                continue;
            }
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == '{') {
                    braceLevel++;
                } else if (c == '}') {
                    braceLevel--;
                } else if (c == '[') {
                    bracketLevel++;
                } else if (c == ']') {
                    bracketLevel--;
                } else if (c == ',' && braceLevel == 0 && bracketLevel == 0) {
                    elements.add(current.toString());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
        }
        
        if (current.length() > 0) {
            elements.add(current.toString());
        }
        
        return elements;
    }
    
    /**
     * Find the index of the colon that separates key and value in a JSON object property.
     */
    private int findColonIndex(String pair) {
        boolean inQuotes = false;
        boolean escapeNext = false;
        
        for (int i = 0; i < pair.length(); i++) {
            char c = pair.charAt(i);
            
            if (escapeNext) {
                escapeNext = false;
                continue;
            }
            
            if (c == '\\') {
                escapeNext = true;
                continue;
            }
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ':' && !inQuotes) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Extract data from the specified root path.
     */
    private Object extractDataFromRootPath(Object parsedJson, FileFormatConfig formatConfig) {
        if (formatConfig == null || formatConfig.getRootPath() == null || 
            "$".equals(formatConfig.getRootPath())) {
            return parsedJson;
        }
        
        String rootPath = formatConfig.getRootPath();
        
        // Simple JSONPath-like implementation
        if (rootPath.startsWith("$.")) {
            String[] pathParts = rootPath.substring(2).split("\\.");
            Object current = parsedJson;
            
            for (String part : pathParts) {
                if (current instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) current;
                    current = map.get(part);
                } else {
                    return null; // Path not found
                }
            }
            
            return current;
        }
        
        return parsedJson;
    }
    
    /**
     * Apply transformations based on format configuration.
     */
    private List<Object> applyTransformations(List<Object> data, FileFormatConfig formatConfig) {
        if (formatConfig == null) {
            return data;
        }
        
        List<Object> transformed = new ArrayList<>();
        
        for (Object item : data) {
            Object transformedItem = transformItem(item, formatConfig);
            if (transformedItem != null) {
                transformed.add(transformedItem);
            }
        }
        
        return transformed;
    }
    
    /**
     * Transform a single data item.
     */
    private Object transformItem(Object item, FileFormatConfig formatConfig) {
        if (!(item instanceof Map)) {
            return item;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> originalMap = (Map<String, Object>) item;
        Map<String, Object> transformedMap = new LinkedHashMap<>();
        
        // Apply column mappings
        for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
            String originalKey = entry.getKey();
            String mappedKey = getMappedColumnName(originalKey, formatConfig);
            Object value = entry.getValue();
            
            // Handle nested object flattening if configured
            if (formatConfig.shouldFlattenArrays() && value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> listValue = (List<Object>) value;
                if (!listValue.isEmpty()) {
                    value = listValue.get(0); // Take first element
                }
            }
            
            transformedMap.put(mappedKey, value);
        }
        
        return transformedMap;
    }
    
    /**
     * Get the mapped column name.
     */
    private String getMappedColumnName(String originalName, FileFormatConfig formatConfig) {
        if (formatConfig.getColumnMappings() != null) {
            return formatConfig.getColumnMappings().getOrDefault(originalName, originalName);
        }
        return originalName;
    }
}
