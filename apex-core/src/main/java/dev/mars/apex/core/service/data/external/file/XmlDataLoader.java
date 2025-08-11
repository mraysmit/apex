package dev.mars.apex.core.service.data.external.file;

import dev.mars.apex.core.config.datasource.FileFormatConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XML data loader implementation.
 * 
 * This class loads data from XML files with support for element selection,
 * namespace handling, and attribute extraction.
 * 
 * Features:
 * - Root and record element selection
 * - Namespace support
 * - Attribute and text content extraction
 * - Custom encoding support
 * - Field mapping
 * 
 * Note: This is a basic XML parser implementation using regex patterns.
 * In production, you would typically use a proper XML parser like DOM or SAX.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class XmlDataLoader implements DataLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(XmlDataLoader.class);
    
    @Override
    public List<Object> loadData(Path filePath, FileFormatConfig formatConfig) throws IOException {
        List<Object> results = new ArrayList<>();
        
        // Determine encoding
        String encoding = formatConfig != null && formatConfig.getEncoding() != null ? 
            formatConfig.getEncoding() : "UTF-8";
        
        try {
            // Read the entire file content
            String xmlContent = Files.readString(filePath, Charset.forName(encoding));
            
            // Parse XML content
            List<Map<String, Object>> parsedData = parseXml(xmlContent, formatConfig);
            
            // Apply transformations
            for (Map<String, Object> item : parsedData) {
                Object transformedItem = applyTransformations(item, formatConfig);
                if (transformedItem != null) {
                    results.add(transformedItem);
                }
            }
            
            LOGGER.debug("Loaded {} objects from XML file: {}", results.size(), filePath);
            
        } catch (IOException e) {
            LOGGER.error("Failed to load XML file: {}", filePath, e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Failed to parse XML file: {}", filePath, e);
            throw new IOException("XML parsing failed", e);
        }
        
        return results;
    }
    
    @Override
    public boolean supportsFormat(FileFormatConfig formatConfig) {
        return formatConfig != null && 
               ("xml".equalsIgnoreCase(formatConfig.getType()) || 
                formatConfig.getFileType() == FileFormatConfig.FileType.XML);
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{"xml"};
    }
    
    /**
     * Parse XML content into a list of data objects.
     */
    private List<Map<String, Object>> parseXml(String xmlContent, FileFormatConfig formatConfig) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Get record element name
        String recordElement = getRecordElement(formatConfig);
        if (recordElement == null) {
            LOGGER.warn("No record element specified for XML parsing");
            return results;
        }
        
        // Find all record elements
        Pattern recordPattern = Pattern.compile(
            "<" + recordElement + "(?:\\s[^>]*)?>([\\s\\S]*?)</" + recordElement + ">",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher recordMatcher = recordPattern.matcher(xmlContent);
        
        while (recordMatcher.find()) {
            String recordXml = recordMatcher.group(0);
            Map<String, Object> recordData = parseXmlElement(recordXml, recordElement);
            if (!recordData.isEmpty()) {
                results.add(recordData);
            }
        }
        
        return results;
    }
    
    /**
     * Parse a single XML element into a data map.
     */
    private Map<String, Object> parseXmlElement(String elementXml, String elementName) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        // Extract attributes from the opening tag
        Pattern attributePattern = Pattern.compile(
            "<" + elementName + "(?:\\s([^>]*?))?>[\\s\\S]*",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher attributeMatcher = attributePattern.matcher(elementXml);
        if (attributeMatcher.find()) {
            String attributesString = attributeMatcher.group(1);
            if (attributesString != null) {
                Map<String, String> attributes = parseAttributes(attributesString);
                for (Map.Entry<String, String> attr : attributes.entrySet()) {
                    result.put("@" + attr.getKey(), attr.getValue());
                }
            }
        }
        
        // Extract child elements and text content
        String innerXml = extractInnerXml(elementXml, elementName);
        if (innerXml != null) {
            parseInnerXml(innerXml, result);
        }
        
        return result;
    }
    
    /**
     * Parse attributes from an attribute string.
     */
    private Map<String, String> parseAttributes(String attributesString) {
        Map<String, String> attributes = new LinkedHashMap<>();
        
        Pattern attrPattern = Pattern.compile("(\\w+)\\s*=\\s*[\"']([^\"']*)[\"']");
        Matcher attrMatcher = attrPattern.matcher(attributesString);
        
        while (attrMatcher.find()) {
            String attrName = attrMatcher.group(1);
            String attrValue = attrMatcher.group(2);
            attributes.put(attrName, attrValue);
        }
        
        return attributes;
    }
    
    /**
     * Extract inner XML content from an element.
     */
    private String extractInnerXml(String elementXml, String elementName) {
        Pattern innerPattern = Pattern.compile(
            "<" + elementName + "(?:\\s[^>]*)?>([\\s\\S]*?)</" + elementName + ">",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher innerMatcher = innerPattern.matcher(elementXml);
        if (innerMatcher.find()) {
            return innerMatcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Parse inner XML content and populate the result map.
     */
    private void parseInnerXml(String innerXml, Map<String, Object> result) {
        // Find all child elements
        Pattern elementPattern = Pattern.compile("<(\\w+)(?:\\s[^>]*)?>([\\s\\S]*?)</\\1>");
        Matcher elementMatcher = elementPattern.matcher(innerXml);
        
        Set<String> processedElements = new HashSet<>();
        
        while (elementMatcher.find()) {
            String childElementName = elementMatcher.group(1);
            String childElementContent = elementMatcher.group(2);
            
            processedElements.add(childElementName);
            
            // Check if this child element contains other elements or just text
            if (childElementContent.contains("<")) {
                // Contains child elements - parse recursively
                Map<String, Object> childData = new LinkedHashMap<>();
                parseInnerXml(childElementContent, childData);
                
                // Handle multiple elements with the same name
                if (result.containsKey(childElementName)) {
                    Object existing = result.get(childElementName);
                    if (existing instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>) existing;
                        list.add(childData);
                    } else {
                        List<Object> list = new ArrayList<>();
                        list.add(existing);
                        list.add(childData);
                        result.put(childElementName, list);
                    }
                } else {
                    result.put(childElementName, childData);
                }
            } else {
                // Just text content
                String textValue = childElementContent.trim();
                Object convertedValue = convertValue(textValue);
                
                // Handle multiple elements with the same name
                if (result.containsKey(childElementName)) {
                    Object existing = result.get(childElementName);
                    if (existing instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>) existing;
                        list.add(convertedValue);
                    } else {
                        List<Object> list = new ArrayList<>();
                        list.add(existing);
                        list.add(convertedValue);
                        result.put(childElementName, list);
                    }
                } else {
                    result.put(childElementName, convertedValue);
                }
            }
        }
        
        // Check for text content that's not in child elements
        String remainingText = innerXml;
        for (String processedElement : processedElements) {
            remainingText = remainingText.replaceAll(
                "<" + processedElement + "(?:\\s[^>]*)?>([\\s\\S]*?)</" + processedElement + ">", "");
        }
        
        remainingText = remainingText.trim();
        if (!remainingText.isEmpty() && !remainingText.matches("\\s*")) {
            result.put("_text", convertValue(remainingText));
        }
    }
    
    /**
     * Convert a string value to the appropriate data type.
     */
    private Object convertValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        value = value.trim();
        
        // Try boolean
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        
        // Try integer
        try {
            if (!value.contains(".") && !value.contains("e") && !value.contains("E")) {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            // Not an integer, continue
        }
        
        // Try decimal
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // Not a number, continue
        }
        
        // Return as string
        return value;
    }
    
    /**
     * Get the record element name from configuration.
     */
    private String getRecordElement(FileFormatConfig formatConfig) {
        if (formatConfig != null && formatConfig.getRecordElement() != null) {
            return formatConfig.getRecordElement();
        }
        return "record"; // Default
    }
    
    /**
     * Apply transformations based on format configuration.
     */
    private Object applyTransformations(Map<String, Object> item, FileFormatConfig formatConfig) {
        if (formatConfig == null || formatConfig.getColumnMappings() == null) {
            return item;
        }
        
        Map<String, Object> transformedMap = new LinkedHashMap<>();
        
        // Apply column mappings
        for (Map.Entry<String, Object> entry : item.entrySet()) {
            String originalKey = entry.getKey();
            String mappedKey = formatConfig.getColumnMappings().getOrDefault(originalKey, originalKey);
            transformedMap.put(mappedKey, entry.getValue());
        }
        
        return transformedMap;
    }
}
