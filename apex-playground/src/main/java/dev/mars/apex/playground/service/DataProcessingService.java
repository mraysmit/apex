package dev.mars.apex.playground.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;

/**
 * Service for handling multiple data formats in the playground.
 *
 * Provides parsing and processing capabilities for various data formats
 * including JSON, XML, CSV, and other structured data formats.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@Service
public class DataProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DataProcessingService.class);

    private final ObjectMapper jsonMapper;
    private final XmlMapper xmlMapper;

    public DataProcessingService() {
        this.jsonMapper = new ObjectMapper();
        this.xmlMapper = new XmlMapper();
    }

    /**
     * Parse and normalize data from various formats.
     *
     * Auto-detects data format if not specified and parses data into
     * a normalized Map structure ready for rules processing.
     *
     * @param rawData The raw data content
     * @param format The expected data format (optional for auto-detection)
     * @return Normalized data structure as Map<String, Object>
     */
    public Map<String, Object> parseData(String rawData, String format) {
        logger.debug("Parsing data format: {}, length: {}", format, rawData != null ? rawData.length() : 0);

        if (rawData == null || rawData.trim().isEmpty()) {
            throw new IllegalArgumentException("Raw data cannot be null or empty");
        }

        // Auto-detect format if not provided
        if (format == null || format.trim().isEmpty()) {
            format = detectDataFormat(rawData);
            logger.debug("Auto-detected format: {}", format);
        }

        try {
            switch (format.toUpperCase()) {
                case "JSON":
                    return parseJsonData(rawData);
                case "XML":
                    return parseXmlData(rawData);
                case "CSV":
                    return parseCsvData(rawData);
                default:
                    throw new IllegalArgumentException("Unsupported data format: " + format);
            }
        } catch (Exception e) {
            logger.error("Failed to parse {} data: {}", format, e.getMessage());
            throw new RuntimeException("Failed to parse " + format + " data: " + e.getMessage(), e);
        }
    }

    /**
     * Auto-detect the format of input data.
     *
     * @param rawData The raw data content
     * @return Detected format (JSON, XML, CSV, etc.)
     */
    public String detectDataFormat(String rawData) {
        if (rawData == null || rawData.trim().isEmpty()) {
            return "UNKNOWN";
        }

        String trimmed = rawData.trim();

        // JSON detection
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                jsonMapper.readTree(rawData);
                return "JSON";
            } catch (Exception e) {
                // Not valid JSON, continue checking
            }
        }

        // XML detection
        if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
            try {
                xmlMapper.readTree(rawData);
                return "XML";
            } catch (Exception e) {
                // Not valid XML, continue checking
            }
        }

        // CSV detection (simple heuristic)
        if (rawData.contains(",") && rawData.contains("\n")) {
            String[] lines = rawData.split("\n");
            if (lines.length >= 2) {
                // Check if first two lines have same number of commas
                int firstLineCommas = lines[0].split(",").length;
                int secondLineCommas = lines[1].split(",").length;
                if (firstLineCommas == secondLineCommas && firstLineCommas > 1) {
                    return "CSV";
                }
            }
        }

        logger.debug("Could not detect data format, defaulting to JSON");
        return "JSON"; // Default to JSON for single objects
    }

    /**
     * Validate data format and structure.
     *
     * @param rawData The raw data content
     * @param expectedFormat The expected data format
     * @return Validation result
     */
    public boolean validateDataFormat(String rawData, String expectedFormat) {
        if (rawData == null || rawData.trim().isEmpty()) {
            return false;
        }

        try {
            String detectedFormat = detectDataFormat(rawData);
            return expectedFormat.equalsIgnoreCase(detectedFormat);
        } catch (Exception e) {
            logger.debug("Data format validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Parse JSON data into a Map structure.
     */
    private Map<String, Object> parseJsonData(String jsonData) throws Exception {
        JsonNode jsonNode = jsonMapper.readTree(jsonData);

        if (jsonNode.isObject()) {
            // Single object
            return jsonMapper.convertValue(jsonNode, Map.class);
        } else if (jsonNode.isArray()) {
            // Array of objects - return the first object or create a wrapper
            if (jsonNode.size() > 0) {
                JsonNode firstElement = jsonNode.get(0);
                if (firstElement.isObject()) {
                    Map<String, Object> result = jsonMapper.convertValue(firstElement, Map.class);
                    // Add metadata about the array
                    result.put("_arraySize", jsonNode.size());
                    result.put("_isArrayElement", true);
                    return result;
                } else {
                    // Array of primitives
                    Map<String, Object> result = new HashMap<>();
                    result.put("_arrayData", jsonMapper.convertValue(jsonNode, List.class));
                    result.put("_arraySize", jsonNode.size());
                    return result;
                }
            } else {
                // Empty array
                Map<String, Object> result = new HashMap<>();
                result.put("_arrayData", new ArrayList<>());
                result.put("_arraySize", 0);
                return result;
            }
        } else {
            // Primitive value
            Map<String, Object> result = new HashMap<>();
            result.put("value", jsonMapper.convertValue(jsonNode, Object.class));
            return result;
        }
    }

    /**
     * Parse XML data into a Map structure.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseXmlData(String xmlData) throws Exception {
        JsonNode xmlNode = xmlMapper.readTree(xmlData);
        Map<String, Object> result = jsonMapper.convertValue(xmlNode, Map.class);

        // Post-process to convert string numbers to actual numbers
        result = postProcessXmlData(result);

        return result;
    }

    /**
     * Parse CSV data into a Map structure.
     */
    private Map<String, Object> parseCsvData(String csvData) throws Exception {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csvData))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        }

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("CSV data is empty");
        }

        // Parse header
        String[] headers = lines.get(0).split(",");
        for (int i = 0; i < headers.length; i++) {
            headers[i] = headers[i].trim().replaceAll("^\"|\"$", ""); // Remove quotes
        }

        if (lines.size() == 1) {
            // Only header, create empty data
            Map<String, Object> result = new HashMap<>();
            for (String header : headers) {
                result.put(header, null);
            }
            result.put("_csvRowCount", 0);
            return result;
        }

        // Parse first data row
        String[] values = lines.get(1).split(",");
        Map<String, Object> result = new HashMap<>();

        for (int i = 0; i < headers.length && i < values.length; i++) {
            String value = values[i].trim().replaceAll("^\"|\"$", ""); // Remove quotes

            // Try to convert to appropriate type
            Object convertedValue = convertCsvValue(value);
            result.put(headers[i], convertedValue);
        }

        // Add metadata
        result.put("_csvRowCount", lines.size() - 1);
        result.put("_csvColumnCount", headers.length);
        result.put("_isFirstRow", true);

        return result;
    }

    /**
     * Convert CSV string value to appropriate Java type.
     */
    private Object convertCsvValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // Try integer
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Not an integer
        }

        // Try double
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // Not a double
        }

        // Try boolean
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }

        // Return as string
        return value;
    }

    /**
     * Post-process XML data to convert string numbers to actual numbers.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> postProcessXmlData(Map<String, Object> xmlData) {
        Map<String, Object> processedData = new HashMap<>();

        for (Map.Entry<String, Object> entry : xmlData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                // Try to convert string values to appropriate types
                processedData.put(key, convertCsvValue((String) value));
            } else if (value instanceof Map) {
                // Recursively process nested maps
                processedData.put(key, postProcessXmlData((Map<String, Object>) value));
            } else {
                // Keep other types as-is
                processedData.put(key, value);
            }
        }

        return processedData;
    }
}
