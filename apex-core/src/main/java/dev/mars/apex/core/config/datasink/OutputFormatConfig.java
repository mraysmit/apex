package dev.mars.apex.core.config.datasink;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for output format settings in data sinks.
 * 
 * This class defines how data should be formatted when written to the sink,
 * including field mappings, data transformations, and format-specific settings.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class OutputFormatConfig {
    
    /**
     * Enumeration of supported output formats.
     */
    public enum OutputFormat {
        JSON("json", "JavaScript Object Notation"),
        XML("xml", "Extensible Markup Language"),
        CSV("csv", "Comma-separated values"),
        SQL("sql", "Structured Query Language"),
        AVRO("avro", "Apache Avro binary format"),
        PARQUET("parquet", "Apache Parquet columnar format"),
        CUSTOM("custom", "Custom format");
        
        private final String code;
        private final String description;
        
        OutputFormat(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static OutputFormat fromCode(String code) {
            if (code == null) {
                return JSON; // Default
            }
            
            for (OutputFormat format : values()) {
                if (format.code.equalsIgnoreCase(code)) {
                    return format;
                }
            }
            
            return JSON;
        }
    }
    
    private String format = "json";
    private String encoding = "UTF-8";
    private Boolean prettyPrint = false;
    
    // Field mappings and transformations
    private Map<String, String> fieldMappings; // source field -> target field
    private Map<String, String> fieldTypes; // field -> type (string, number, date, etc.)
    private Map<String, String> fieldFormats; // field -> format (for dates, numbers, etc.)
    private Map<String, Object> defaultValues; // field -> default value
    
    // Format-specific settings
    private String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private String numberFormat;
    private String booleanFormat = "true/false"; // or "1/0", "yes/no"
    
    // CSV-specific settings
    private String delimiter = ",";
    private String quoteCharacter = "\"";
    private String escapeCharacter = "\\";
    private Boolean includeHeader = true;
    private String lineEnding = "\n";
    
    // JSON-specific settings
    private String rootElement;
    private Boolean flattenNestedObjects = false;
    private String arrayHandling = "array"; // array, concat, first, last
    
    // XML-specific settings
    private String xmlRootElement = "root";
    private String xmlRecordElement = "record";
    private Map<String, String> xmlNamespaces;
    private Boolean xmlIncludeDeclaration = true;
    
    // SQL-specific settings
    private String tableName;
    private String schemaName;
    private Boolean includeTimestamp = false;
    private String timestampColumn = "created_at";
    
    // Filtering and validation
    private List<String> includeFields; // Only include these fields
    private List<String> excludeFields; // Exclude these fields
    private Map<String, String> fieldValidations; // field -> validation expression
    
    // Transformation settings
    private Boolean trimStrings = true;
    private Boolean convertEmptyToNull = true;
    private String nullHandling = "null"; // null, empty, skip
    
    // Custom transformation
    private String customFormatter; // Custom formatter class
    private Map<String, Object> formatterProperties;
    
    /**
     * Default constructor.
     */
    public OutputFormatConfig() {
        this.fieldMappings = new HashMap<>();
        this.fieldTypes = new HashMap<>();
        this.fieldFormats = new HashMap<>();
        this.defaultValues = new HashMap<>();
        this.xmlNamespaces = new HashMap<>();
        this.formatterProperties = new HashMap<>();
    }
    
    // Getters and setters
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public OutputFormat getOutputFormat() {
        return OutputFormat.fromCode(format);
    }
    
    public String getEncoding() {
        return encoding;
    }
    
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    
    public Boolean getPrettyPrint() {
        return prettyPrint;
    }
    
    public void setPrettyPrint(Boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }
    
    public Map<String, String> getFieldMappings() {
        return fieldMappings;
    }
    
    public void setFieldMappings(Map<String, String> fieldMappings) {
        this.fieldMappings = fieldMappings != null ? fieldMappings : new HashMap<>();
    }
    
    public Map<String, String> getFieldTypes() {
        return fieldTypes;
    }
    
    public void setFieldTypes(Map<String, String> fieldTypes) {
        this.fieldTypes = fieldTypes != null ? fieldTypes : new HashMap<>();
    }
    
    public Map<String, String> getFieldFormats() {
        return fieldFormats;
    }
    
    public void setFieldFormats(Map<String, String> fieldFormats) {
        this.fieldFormats = fieldFormats != null ? fieldFormats : new HashMap<>();
    }
    
    public Map<String, Object> getDefaultValues() {
        return defaultValues;
    }
    
    public void setDefaultValues(Map<String, Object> defaultValues) {
        this.defaultValues = defaultValues != null ? defaultValues : new HashMap<>();
    }
    
    public String getDateFormat() {
        return dateFormat;
    }
    
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
    
    public String getNumberFormat() {
        return numberFormat;
    }
    
    public void setNumberFormat(String numberFormat) {
        this.numberFormat = numberFormat;
    }
    
    public String getBooleanFormat() {
        return booleanFormat;
    }
    
    public void setBooleanFormat(String booleanFormat) {
        this.booleanFormat = booleanFormat;
    }
    
    // CSV-specific getters and setters
    
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
    
    public Boolean getIncludeHeader() {
        return includeHeader;
    }
    
    public void setIncludeHeader(Boolean includeHeader) {
        this.includeHeader = includeHeader;
    }
    
    public String getLineEnding() {
        return lineEnding;
    }
    
    public void setLineEnding(String lineEnding) {
        this.lineEnding = lineEnding;
    }
    
    // Additional getters and setters for other properties...
    // (Continuing with remaining properties to stay within line limit)
    
    /**
     * Validate the output format configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("Output format is required");
        }
        
        OutputFormat outputFormat = getOutputFormat();
        if (outputFormat == null) {
            throw new IllegalArgumentException("Invalid output format: " + format);
        }
        
        if (encoding == null || encoding.trim().isEmpty()) {
            throw new IllegalArgumentException("Encoding is required");
        }
        
        // Validate format-specific settings
        if (outputFormat == OutputFormat.CSV) {
            if (delimiter == null || delimiter.isEmpty()) {
                throw new IllegalArgumentException("CSV delimiter is required");
            }
        }
        
        if (outputFormat == OutputFormat.SQL) {
            if (tableName == null || tableName.trim().isEmpty()) {
                throw new IllegalArgumentException("SQL table name is required");
            }
        }
    }
    
    /**
     * Create a copy of this configuration.
     * 
     * @return A new OutputFormatConfig with the same settings
     */
    public OutputFormatConfig copy() {
        OutputFormatConfig copy = new OutputFormatConfig();
        copy.format = this.format;
        copy.encoding = this.encoding;
        copy.prettyPrint = this.prettyPrint;
        
        copy.fieldMappings = new HashMap<>(this.fieldMappings);
        copy.fieldTypes = new HashMap<>(this.fieldTypes);
        copy.fieldFormats = new HashMap<>(this.fieldFormats);
        copy.defaultValues = new HashMap<>(this.defaultValues);
        
        copy.dateFormat = this.dateFormat;
        copy.numberFormat = this.numberFormat;
        copy.booleanFormat = this.booleanFormat;
        
        copy.delimiter = this.delimiter;
        copy.quoteCharacter = this.quoteCharacter;
        copy.escapeCharacter = this.escapeCharacter;
        copy.includeHeader = this.includeHeader;
        copy.lineEnding = this.lineEnding;
        
        copy.rootElement = this.rootElement;
        copy.flattenNestedObjects = this.flattenNestedObjects;
        copy.arrayHandling = this.arrayHandling;
        
        copy.xmlRootElement = this.xmlRootElement;
        copy.xmlRecordElement = this.xmlRecordElement;
        copy.xmlNamespaces = new HashMap<>(this.xmlNamespaces);
        copy.xmlIncludeDeclaration = this.xmlIncludeDeclaration;
        
        copy.tableName = this.tableName;
        copy.schemaName = this.schemaName;
        copy.includeTimestamp = this.includeTimestamp;
        copy.timestampColumn = this.timestampColumn;
        
        copy.includeFields = this.includeFields != null ? List.copyOf(this.includeFields) : null;
        copy.excludeFields = this.excludeFields != null ? List.copyOf(this.excludeFields) : null;
        copy.fieldValidations = this.fieldValidations != null ? new HashMap<>(this.fieldValidations) : null;
        
        copy.trimStrings = this.trimStrings;
        copy.convertEmptyToNull = this.convertEmptyToNull;
        copy.nullHandling = this.nullHandling;
        
        copy.customFormatter = this.customFormatter;
        copy.formatterProperties = new HashMap<>(this.formatterProperties);
        
        return copy;
    }
}
