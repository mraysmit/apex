package dev.mars.apex.core.config.datasource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration class for file format settings.
 * 
 * This class contains file format-specific configuration for parsing
 * different file types including CSV, JSON, XML, and custom formats.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class FileFormatConfig {
    
    /**
     * Enumeration of supported file formats.
     */
    public enum FileType {
        CSV("csv", "Comma-separated values"),
        JSON("json", "JavaScript Object Notation"),
        XML("xml", "Extensible Markup Language"),
        FIXED_WIDTH("fixed-width", "Fixed-width text format"),
        CUSTOM("custom", "Custom file format");
        
        private final String code;
        private final String description;
        
        FileType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static FileType fromCode(String code) {
            if (code == null) {
                return CSV; // Default
            }
            
            for (FileType type : values()) {
                if (type.code.equalsIgnoreCase(code)) {
                    return type;
                }
            }
            
            return CSV;
        }
    }
    
    private String type = "csv";
    private String delimiter = ",";
    private String quoteCharacter = "\"";
    private String escapeCharacter = "\\";
    private Boolean headerRow = true;
    private Integer skipLines = 0;
    private String encoding = "UTF-8";
    private String dateFormat = "yyyy-MM-dd";
    private String timestampFormat = "yyyy-MM-dd HH:mm:ss";
    private String nullValue = "";
    
    // JSON-specific configuration
    private String rootPath = "$";
    private Boolean flattenArrays = false;
    
    // XML-specific configuration
    private String rootElement;
    private String recordElement;
    private Map<String, String> namespaces;
    
    // Fixed-width specific configuration
    private List<FieldDefinition> fieldDefinitions;
    
    // Column mapping configuration
    private Map<String, String> columnMappings;
    private String keyColumn;
    
    // Custom format configuration
    private String customParser;
    private Map<String, Object> customProperties;
    
    /**
     * Default constructor.
     */
    public FileFormatConfig() {
        this.namespaces = new HashMap<>();
        this.columnMappings = new HashMap<>();
        this.customProperties = new HashMap<>();
    }
    
    /**
     * Constructor with file type.
     * 
     * @param type The file type
     */
    public FileFormatConfig(String type) {
        this();
        this.type = type;
    }
    
    // Basic configuration
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public FileType getFileType() {
        return FileType.fromCode(type);
    }
    
    public String getDelimiter() {
        return delimiter;
    }
    
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }
    
    public String getQuoteCharacter() {
        return quoteCharacter;
    }
    
    public void setQuoteCharacter(String quoteCharacter) {
        this.quoteCharacter = quoteCharacter;
    }
    
    public String getEscapeCharacter() {
        return escapeCharacter;
    }
    
    public void setEscapeCharacter(String escapeCharacter) {
        this.escapeCharacter = escapeCharacter;
    }
    
    public Boolean getHeaderRow() {
        return headerRow;
    }
    
    public void setHeaderRow(Boolean headerRow) {
        this.headerRow = headerRow;
    }
    
    public boolean hasHeaderRow() {
        return headerRow != null && headerRow;
    }
    
    public Integer getSkipLines() {
        return skipLines;
    }
    
    public void setSkipLines(Integer skipLines) {
        this.skipLines = skipLines;
    }
    
    public String getEncoding() {
        return encoding;
    }
    
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    
    public String getDateFormat() {
        return dateFormat;
    }
    
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
    
    public String getTimestampFormat() {
        return timestampFormat;
    }
    
    public void setTimestampFormat(String timestampFormat) {
        this.timestampFormat = timestampFormat;
    }
    
    public String getNullValue() {
        return nullValue;
    }
    
    public void setNullValue(String nullValue) {
        this.nullValue = nullValue;
    }
    
    // JSON-specific configuration
    
    public String getRootPath() {
        return rootPath;
    }
    
    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }
    
    public Boolean getFlattenArrays() {
        return flattenArrays;
    }
    
    public void setFlattenArrays(Boolean flattenArrays) {
        this.flattenArrays = flattenArrays;
    }
    
    public boolean shouldFlattenArrays() {
        return flattenArrays != null && flattenArrays;
    }
    
    // XML-specific configuration
    
    public String getRootElement() {
        return rootElement;
    }
    
    public void setRootElement(String rootElement) {
        this.rootElement = rootElement;
    }
    
    public String getRecordElement() {
        return recordElement;
    }
    
    public void setRecordElement(String recordElement) {
        this.recordElement = recordElement;
    }
    
    public Map<String, String> getNamespaces() {
        return namespaces;
    }
    
    public void setNamespaces(Map<String, String> namespaces) {
        this.namespaces = namespaces != null ? namespaces : new HashMap<>();
    }
    
    // Fixed-width configuration
    
    public List<FieldDefinition> getFieldDefinitions() {
        return fieldDefinitions;
    }
    
    public void setFieldDefinitions(List<FieldDefinition> fieldDefinitions) {
        this.fieldDefinitions = fieldDefinitions;
    }
    
    // Column mapping
    
    public Map<String, String> getColumnMappings() {
        return columnMappings;
    }
    
    public void setColumnMappings(Map<String, String> columnMappings) {
        this.columnMappings = columnMappings != null ? columnMappings : new HashMap<>();
    }
    
    public String getKeyColumn() {
        return keyColumn;
    }
    
    public void setKeyColumn(String keyColumn) {
        this.keyColumn = keyColumn;
    }
    
    // Custom format configuration
    
    public String getCustomParser() {
        return customParser;
    }
    
    public void setCustomParser(String customParser) {
        this.customParser = customParser;
    }
    
    public Map<String, Object> getCustomProperties() {
        return customProperties;
    }
    
    public void setCustomProperties(Map<String, Object> customProperties) {
        this.customProperties = customProperties != null ? customProperties : new HashMap<>();
    }
    
    // Validation
    
    /**
     * Validate the file format configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        FileType fileType = getFileType();
        
        if (encoding == null || encoding.trim().isEmpty()) {
            throw new IllegalArgumentException("File encoding is required");
        }
        
        if (skipLines != null && skipLines < 0) {
            throw new IllegalArgumentException("Skip lines cannot be negative");
        }
        
        switch (fileType) {
            case CSV:
                if (delimiter == null || delimiter.isEmpty()) {
                    throw new IllegalArgumentException("Delimiter is required for CSV files");
                }
                break;

            case JSON:
                // JSON files don't require additional validation beyond basic file properties
                break;

            case XML:
                if (recordElement == null || recordElement.trim().isEmpty()) {
                    throw new IllegalArgumentException("Record element is required for XML files");
                }
                break;

            case FIXED_WIDTH:
                if (fieldDefinitions == null || fieldDefinitions.isEmpty()) {
                    throw new IllegalArgumentException("Field definitions are required for fixed-width files");
                }
                break;

            case CUSTOM:
                if (customParser == null || customParser.trim().isEmpty()) {
                    throw new IllegalArgumentException("Custom parser class is required for custom file formats");
                }
                break;
        }
    }
    
    /**
     * Create a copy of this file format configuration.
     * 
     * @return A new FileFormatConfig with the same settings
     */
    public FileFormatConfig copy() {
        FileFormatConfig copy = new FileFormatConfig();
        copy.type = this.type;
        copy.delimiter = this.delimiter;
        copy.quoteCharacter = this.quoteCharacter;
        copy.escapeCharacter = this.escapeCharacter;
        copy.headerRow = this.headerRow;
        copy.skipLines = this.skipLines;
        copy.encoding = this.encoding;
        copy.dateFormat = this.dateFormat;
        copy.timestampFormat = this.timestampFormat;
        copy.nullValue = this.nullValue;
        copy.rootPath = this.rootPath;
        copy.flattenArrays = this.flattenArrays;
        copy.rootElement = this.rootElement;
        copy.recordElement = this.recordElement;
        copy.namespaces = new HashMap<>(this.namespaces);
        copy.fieldDefinitions = this.fieldDefinitions; // Shallow copy for now
        copy.columnMappings = new HashMap<>(this.columnMappings);
        copy.keyColumn = this.keyColumn;
        copy.customParser = this.customParser;
        copy.customProperties = new HashMap<>(this.customProperties);
        return copy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileFormatConfig that = (FileFormatConfig) o;
        return Objects.equals(type, that.type) &&
               Objects.equals(delimiter, that.delimiter) &&
               Objects.equals(encoding, that.encoding) &&
               Objects.equals(headerRow, that.headerRow);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, delimiter, encoding, headerRow);
    }
    
    @Override
    public String toString() {
        return "FileFormatConfig{" +
               "type='" + type + '\'' +
               ", delimiter='" + delimiter + '\'' +
               ", encoding='" + encoding + '\'' +
               ", headerRow=" + headerRow +
               ", skipLines=" + skipLines +
               '}';
    }
    
    /**
     * Inner class for fixed-width field definitions.
     */
    public static class FieldDefinition {
        private String name;
        private Integer startPosition;
        private Integer length;
        private String type = "string";
        private String format;
        
        public FieldDefinition() {}
        
        public FieldDefinition(String name, Integer startPosition, Integer length) {
            this.name = name;
            this.startPosition = startPosition;
            this.length = length;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getStartPosition() { return startPosition; }
        public void setStartPosition(Integer startPosition) { this.startPosition = startPosition; }
        public Integer getLength() { return length; }
        public void setLength(Integer length) { this.length = length; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        
        @Override
        public String toString() {
            return "FieldDefinition{name='" + name + "', start=" + startPosition + ", length=" + length + "}";
        }
    }
}
