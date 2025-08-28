package dev.mars.apex.core.service.data.external.file;

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


import dev.mars.apex.core.config.datasource.FileFormatConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Text data loader implementation for fixed-width and plain text files.
 * 
 * This class loads data from text files with support for fixed-width formats,
 * line-based processing, and custom field definitions.
 * 
 * Features:
 * - Fixed-width field parsing
 * - Line-based text processing
 * - Custom encoding support
 * - Field type conversion
 * - Skip lines functionality
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class TextDataLoader implements DataLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TextDataLoader.class);
    
    @Override
    public List<Object> loadData(Path filePath, FileFormatConfig formatConfig) throws IOException {
        List<Object> results = new ArrayList<>();
        
        // Determine encoding
        String encoding = formatConfig != null && formatConfig.getEncoding() != null ? 
            formatConfig.getEncoding() : "UTF-8";
        
        try (BufferedReader reader = Files.newBufferedReader(filePath, Charset.forName(encoding))) {
            
            // Skip lines if configured
            int skipLines = formatConfig != null && formatConfig.getSkipLines() != null ? 
                formatConfig.getSkipLines() : 0;
            
            for (int i = 0; i < skipLines; i++) {
                reader.readLine();
            }
            
            // Determine processing mode
            if (isFixedWidthFormat(formatConfig)) {
                results = loadFixedWidthData(reader, formatConfig);
            } else {
                results = loadPlainTextData(reader, formatConfig);
            }
            
            LOGGER.debug("Loaded {} objects from text file: {}", results.size(), filePath);
            
        } catch (IOException e) {
            LOGGER.error("Failed to load text file: {}", filePath, e);
            throw e;
        }
        
        return results;
    }
    
    @Override
    public boolean supportsFormat(FileFormatConfig formatConfig) {
        return formatConfig != null && 
               ("txt".equalsIgnoreCase(formatConfig.getType()) || 
                "fixed-width".equalsIgnoreCase(formatConfig.getType()) ||
                formatConfig.getFileType() == FileFormatConfig.FileType.FIXED_WIDTH);
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{"txt", "dat", "log"};
    }
    
    /**
     * Check if this is a fixed-width format.
     */
    private boolean isFixedWidthFormat(FileFormatConfig formatConfig) {
        return formatConfig != null && 
               (formatConfig.getFileType() == FileFormatConfig.FileType.FIXED_WIDTH ||
                "fixed-width".equalsIgnoreCase(formatConfig.getType())) &&
               formatConfig.getFieldDefinitions() != null &&
               !formatConfig.getFieldDefinitions().isEmpty();
    }
    
    /**
     * Load data from fixed-width format file.
     */
    private List<Object> loadFixedWidthData(BufferedReader reader, FileFormatConfig formatConfig) 
            throws IOException {
        List<Object> results = new ArrayList<>();
        List<FileFormatConfig.FieldDefinition> fieldDefs = formatConfig.getFieldDefinitions();
        
        String line;
        int lineNumber = 0;
        
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            
            if (line.trim().isEmpty()) {
                continue; // Skip empty lines
            }
            
            try {
                Map<String, Object> rowData = parseFixedWidthLine(line, fieldDefs);
                if (!rowData.isEmpty()) {
                    results.add(rowData);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to parse fixed-width line {} in file: {}", lineNumber, e.getMessage());
                // Continue processing other lines
            }
        }
        
        return results;
    }
    
    /**
     * Load data from plain text file.
     */
    private List<Object> loadPlainTextData(BufferedReader reader, FileFormatConfig formatConfig) 
            throws IOException {
        List<Object> results = new ArrayList<>();
        
        String line;
        int lineNumber = 0;
        
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            
            if (line.trim().isEmpty()) {
                continue; // Skip empty lines
            }
            
            // Create a simple object with line number and content
            Map<String, Object> lineData = new LinkedHashMap<>();
            lineData.put("lineNumber", lineNumber);
            lineData.put("content", line);
            lineData.put("length", line.length());
            
            // Apply any transformations
            Object transformedData = applyTransformations(lineData, formatConfig);
            if (transformedData != null) {
                results.add(transformedData);
            }
        }
        
        return results;
    }
    
    /**
     * Parse a fixed-width line based on field definitions.
     */
    private Map<String, Object> parseFixedWidthLine(String line, 
                                                   List<FileFormatConfig.FieldDefinition> fieldDefs) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        for (FileFormatConfig.FieldDefinition fieldDef : fieldDefs) {
            String fieldName = fieldDef.getName();
            Integer startPos = fieldDef.getStartPosition();
            Integer length = fieldDef.getLength();
            String fieldType = fieldDef.getType();
            
            if (fieldName == null || startPos == null || length == null) {
                LOGGER.warn("Invalid field definition: name={}, start={}, length={}", 
                    fieldName, startPos, length);
                continue;
            }
            
            // Adjust for 0-based indexing (assuming configuration uses 1-based)
            int adjustedStart = startPos - 1;
            int adjustedEnd = adjustedStart + length;
            
            // Extract field value
            String fieldValue = null;
            if (adjustedStart >= 0 && adjustedStart < line.length()) {
                if (adjustedEnd <= line.length()) {
                    fieldValue = line.substring(adjustedStart, adjustedEnd);
                } else {
                    fieldValue = line.substring(adjustedStart);
                }
            }
            
            // Convert and store the value
            Object convertedValue = convertFieldValue(fieldValue, fieldType, fieldDef.getFormat());
            result.put(fieldName, convertedValue);
        }
        
        return result;
    }
    
    /**
     * Convert a field value to the specified type.
     */
    private Object convertFieldValue(String value, String fieldType, String format) {
        if (value == null) {
            return null;
        }
        
        // Trim whitespace
        value = value.trim();
        
        if (value.isEmpty()) {
            return null;
        }
        
        // Convert based on field type
        if (fieldType == null) {
            fieldType = "string";
        }
        
        try {
            switch (fieldType.toLowerCase()) {
                case "string":
                case "text":
                    return value;
                    
                case "integer":
                case "int":
                case "long":
                    return Long.parseLong(value);
                    
                case "decimal":
                case "double":
                case "float":
                case "number":
                    return Double.parseDouble(value);
                    
                case "boolean":
                case "bool":
                    return Boolean.parseBoolean(value) || 
                           "1".equals(value) || 
                           "Y".equalsIgnoreCase(value) || 
                           "YES".equalsIgnoreCase(value);
                    
                case "date":
                    return parseDate(value, format);
                    
                case "datetime":
                case "timestamp":
                    return parseDateTime(value, format);
                    
                default:
                    return value;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to convert value '{}' to type '{}': {}", value, fieldType, e.getMessage());
            return value; // Return as string if conversion fails
        }
    }
    
    /**
     * Parse a date string.
     */
    private Object parseDate(String value, String format) {
        if (format == null) {
            format = "yyyy-MM-dd"; // Default format
        }
        
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
            return java.time.LocalDate.parse(value, formatter);
        } catch (Exception e) {
            // Try common date formats
            String[] commonFormats = {
                "yyyy-MM-dd",
                "MM/dd/yyyy",
                "dd/MM/yyyy",
                "yyyy/MM/dd",
                "dd-MM-yyyy",
                "MM-dd-yyyy"
            };
            
            for (String commonFormat : commonFormats) {
                try {
                    java.time.format.DateTimeFormatter formatter = 
                        java.time.format.DateTimeFormatter.ofPattern(commonFormat);
                    return java.time.LocalDate.parse(value, formatter);
                } catch (Exception ignored) {
                    // Continue trying other formats
                }
            }
            
            return value; // Return as string if all parsing attempts fail
        }
    }
    
    /**
     * Parse a date-time string.
     */
    private Object parseDateTime(String value, String format) {
        if (format == null) {
            format = "yyyy-MM-dd HH:mm:ss"; // Default format
        }
        
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
            return java.time.LocalDateTime.parse(value, formatter);
        } catch (Exception e) {
            // Try common datetime formats
            String[] commonFormats = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "MM/dd/yyyy HH:mm:ss",
                "dd/MM/yyyy HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss"
            };
            
            for (String commonFormat : commonFormats) {
                try {
                    java.time.format.DateTimeFormatter formatter = 
                        java.time.format.DateTimeFormatter.ofPattern(commonFormat);
                    return java.time.LocalDateTime.parse(value, formatter);
                } catch (Exception ignored) {
                    // Continue trying other formats
                }
            }
            
            return value; // Return as string if all parsing attempts fail
        }
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
