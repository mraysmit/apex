package dev.mars.apex.core.service.data.external.file;

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
 * CSV data loader implementation.
 * 
 * This class loads data from CSV (Comma-Separated Values) files with support for
 * custom delimiters, quote characters, headers, and data type conversion.
 * 
 * Features:
 * - Configurable delimiter, quote, and escape characters
 * - Header row support
 * - Skip lines functionality
 * - Custom encoding support
 * - Data type conversion
 * - Column mapping
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public class CsvDataLoader implements DataLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvDataLoader.class);
    
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
            
            // Read header row if present
            String[] headers = null;
            if (formatConfig != null && formatConfig.hasHeaderRow()) {
                String headerLine = reader.readLine();
                if (headerLine != null) {
                    headers = parseCsvLine(headerLine, formatConfig);
                }
            }
            
            // Read data rows
            String line;
            int rowNumber = skipLines + (headers != null ? 1 : 0);
            
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                
                try {
                    String[] values = parseCsvLine(line, formatConfig);
                    Object rowData = createRowObject(values, headers, formatConfig);
                    results.add(rowData);
                    
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse CSV line {} in file {}: {}", 
                        rowNumber, filePath, e.getMessage());
                    // Continue processing other lines
                }
            }
            
            LOGGER.debug("Loaded {} rows from CSV file: {}", results.size(), filePath);
            
        } catch (IOException e) {
            LOGGER.error("Failed to load CSV file: {}", filePath, e);
            throw e;
        }
        
        return results;
    }
    
    @Override
    public boolean supportsFormat(FileFormatConfig formatConfig) {
        return formatConfig != null && 
               ("csv".equalsIgnoreCase(formatConfig.getType()) || 
                formatConfig.getFileType() == FileFormatConfig.FileType.CSV);
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{"csv", "tsv", "txt"};
    }
    
    /**
     * Parse a CSV line into an array of values.
     */
    private String[] parseCsvLine(String line, FileFormatConfig formatConfig) {
        if (line == null || line.isEmpty()) {
            return new String[0];
        }
        
        String delimiter = getDelimiter(formatConfig);
        String quoteChar = getQuoteCharacter(formatConfig);
        String escapeChar = getEscapeCharacter(formatConfig);
        
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;
        boolean escapeNext = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            String charStr = String.valueOf(c);
            
            if (escapeNext) {
                currentValue.append(c);
                escapeNext = false;
            } else if (charStr.equals(escapeChar)) {
                escapeNext = true;
            } else if (charStr.equals(quoteChar)) {
                inQuotes = !inQuotes;
            } else if (charStr.equals(delimiter) && !inQuotes) {
                values.add(currentValue.toString());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        
        // Add the last value
        values.add(currentValue.toString());
        
        return values.toArray(new String[0]);
    }
    
    /**
     * Create a row object from parsed values.
     */
    private Object createRowObject(String[] values, String[] headers, FileFormatConfig formatConfig) {
        if (headers != null && headers.length > 0) {
            // Create a Map with column names as keys
            Map<String, Object> rowMap = new LinkedHashMap<>();
            
            for (int i = 0; i < values.length; i++) {
                String columnName = i < headers.length ? headers[i] : "column_" + (i + 1);
                Object convertedValue = convertValue(values[i], columnName, formatConfig);
                
                // Apply column mapping if configured
                String mappedColumnName = getMappedColumnName(columnName, formatConfig);
                rowMap.put(mappedColumnName, convertedValue);
            }
            
            return rowMap;
        } else {
            // Create a Map with generic column names
            Map<String, Object> rowMap = new LinkedHashMap<>();
            
            for (int i = 0; i < values.length; i++) {
                String columnName = "column_" + (i + 1);
                Object convertedValue = convertValue(values[i], columnName, formatConfig);
                rowMap.put(columnName, convertedValue);
            }
            
            return rowMap;
        }
    }
    
    /**
     * Convert a string value to the appropriate data type.
     */
    private Object convertValue(String value, String columnName, FileFormatConfig formatConfig) {
        if (value == null) {
            return null;
        }
        
        // Handle null values
        String nullValue = formatConfig != null && formatConfig.getNullValue() != null ? 
            formatConfig.getNullValue() : "";
        
        if (value.equals(nullValue)) {
            return null;
        }
        
        // Trim whitespace if the value is not quoted
        value = value.trim();
        
        if (value.isEmpty()) {
            return null;
        }
        
        // Try to determine the data type and convert
        // This is a simple implementation - could be enhanced with column-specific type configuration
        
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
        
        // Try date/timestamp
        if (formatConfig != null) {
            try {
                if (formatConfig.getDateFormat() != null && isDatePattern(value)) {
                    return parseDate(value, formatConfig.getDateFormat());
                }
                if (formatConfig.getTimestampFormat() != null && isTimestampPattern(value)) {
                    return parseTimestamp(value, formatConfig.getTimestampFormat());
                }
            } catch (Exception e) {
                // Not a date/timestamp, continue
            }
        }
        
        // Return as string
        return value;
    }
    
    /**
     * Get the delimiter character.
     */
    private String getDelimiter(FileFormatConfig formatConfig) {
        if (formatConfig != null && formatConfig.getDelimiter() != null) {
            return formatConfig.getDelimiter();
        }
        return ","; // Default comma
    }
    
    /**
     * Get the quote character.
     */
    private String getQuoteCharacter(FileFormatConfig formatConfig) {
        if (formatConfig != null && formatConfig.getQuoteCharacter() != null) {
            return formatConfig.getQuoteCharacter();
        }
        return "\""; // Default double quote
    }
    
    /**
     * Get the escape character.
     */
    private String getEscapeCharacter(FileFormatConfig formatConfig) {
        if (formatConfig != null && formatConfig.getEscapeCharacter() != null) {
            return formatConfig.getEscapeCharacter();
        }
        return "\\"; // Default backslash
    }
    
    /**
     * Get the mapped column name.
     */
    private String getMappedColumnName(String originalName, FileFormatConfig formatConfig) {
        if (formatConfig != null && formatConfig.getColumnMappings() != null) {
            return formatConfig.getColumnMappings().getOrDefault(originalName, originalName);
        }
        return originalName;
    }
    
    /**
     * Check if a value matches a date pattern.
     */
    private boolean isDatePattern(String value) {
        // Simple heuristic - could be enhanced
        return value.matches("\\d{4}-\\d{2}-\\d{2}") || 
               value.matches("\\d{2}/\\d{2}/\\d{4}") ||
               value.matches("\\d{2}-\\d{2}-\\d{4}");
    }
    
    /**
     * Check if a value matches a timestamp pattern.
     */
    private boolean isTimestampPattern(String value) {
        // Simple heuristic - could be enhanced
        return value.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}") ||
               value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
    }
    
    /**
     * Parse a date string.
     */
    private Object parseDate(String value, String format) {
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
            return java.time.LocalDate.parse(value, formatter);
        } catch (Exception e) {
            return value; // Return as string if parsing fails
        }
    }
    
    /**
     * Parse a timestamp string.
     */
    private Object parseTimestamp(String value, String format) {
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
            return java.time.LocalDateTime.parse(value, formatter);
        } catch (Exception e) {
            return value; // Return as string if parsing fails
        }
    }
}
