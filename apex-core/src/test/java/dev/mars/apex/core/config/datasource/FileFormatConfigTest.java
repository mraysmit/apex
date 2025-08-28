package dev.mars.apex.core.config.datasource;

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


import dev.mars.apex.core.config.datasource.FileFormatConfig.FileType;
import dev.mars.apex.core.config.datasource.FileFormatConfig.FieldDefinition;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for FileFormatConfig.
 * 
 * Tests cover:
 * - Constructor behavior and initialization
 * - FileType enum functionality and conversion
 * - Basic file format properties (type, delimiter, encoding, etc.)
 * - CSV-specific configuration (delimiter, quote character, header row)
 * - JSON-specific configuration (root path, flatten arrays)
 * - XML-specific configuration (root element, record element, namespaces)
 * - Fixed-width configuration (field definitions)
 * - Column mapping configuration
 * - Custom format configuration (parser, custom properties)
 * - Boolean convenience methods (shouldFlattenArrays, hasHeaderRow)
 * - Validation logic for all file format types
 * - Copy method deep cloning behavior
 * - Equals and hashCode contracts
 * - ToString representation
 * - FieldDefinition inner class functionality
 * - Edge cases and error handling
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class FileFormatConfigTest {

    private FileFormatConfig config;

    @BeforeEach
    void setUp() {
        config = new FileFormatConfig();
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    @DisplayName("Should initialize with default constructor")
    void testDefaultConstructor() {
        FileFormatConfig config = new FileFormatConfig();
        
        // Basic properties should have default values
        assertEquals("csv", config.getType());
        assertEquals(FileType.CSV, config.getFileType());
        assertEquals(",", config.getDelimiter());
        assertEquals("\"", config.getQuoteCharacter());
        assertEquals("\\", config.getEscapeCharacter());
        assertTrue(config.getHeaderRow()); // Default is true
        assertEquals(0, config.getSkipLines()); // Default is 0
        assertEquals("UTF-8", config.getEncoding());
        assertEquals("yyyy-MM-dd", config.getDateFormat());
        assertEquals("yyyy-MM-dd HH:mm:ss", config.getTimestampFormat());
        assertEquals("", config.getNullValue());
        
        // JSON-specific properties should have default values
        assertEquals("$", config.getRootPath());
        assertFalse(config.getFlattenArrays()); // Default is false
        
        // XML-specific properties should be null
        assertNull(config.getRootElement());
        assertNull(config.getRecordElement());
        
        // Fixed-width properties should be null
        assertNull(config.getFieldDefinitions());
        
        // Column mapping properties should be initialized
        assertNotNull(config.getColumnMappings());
        assertTrue(config.getColumnMappings().isEmpty());
        assertNull(config.getKeyColumn());
        
        // Custom format properties should be initialized
        assertNull(config.getCustomParser());
        assertNotNull(config.getCustomProperties());
        assertTrue(config.getCustomProperties().isEmpty());
        
        // Maps should be initialized but empty
        assertNotNull(config.getNamespaces());
        assertTrue(config.getNamespaces().isEmpty());
    }

    @Test
    @DisplayName("Should initialize with type constructor")
    void testTypeConstructor() {
        FileFormatConfig config = new FileFormatConfig("json");
        
        assertEquals("json", config.getType());
        assertEquals(FileType.JSON, config.getFileType());
        
        // Maps should still be initialized
        assertNotNull(config.getNamespaces());
        assertNotNull(config.getColumnMappings());
        assertNotNull(config.getCustomProperties());
        assertTrue(config.getNamespaces().isEmpty());
        assertTrue(config.getColumnMappings().isEmpty());
        assertTrue(config.getCustomProperties().isEmpty());
    }

    @Test
    @DisplayName("Should handle null type in constructor")
    void testTypeConstructorWithNull() {
        FileFormatConfig config = new FileFormatConfig(null);
        
        assertNull(config.getType());
        assertEquals(FileType.CSV, config.getFileType()); // fromCode handles null
    }

    // ========================================
    // FileType Enum Tests
    // ========================================

    @Test
    @DisplayName("Should have correct file type enum values")
    void testFileTypeEnumValues() {
        FileType[] types = FileType.values();
        assertEquals(5, types.length);
        
        assertEquals(FileType.CSV, types[0]);
        assertEquals(FileType.JSON, types[1]);
        assertEquals(FileType.XML, types[2]);
        assertEquals(FileType.FIXED_WIDTH, types[3]);
        assertEquals(FileType.CUSTOM, types[4]);
    }

    @Test
    @DisplayName("Should have correct file type codes")
    void testFileTypeCodes() {
        assertEquals("csv", FileType.CSV.getCode());
        assertEquals("json", FileType.JSON.getCode());
        assertEquals("xml", FileType.XML.getCode());
        assertEquals("fixed-width", FileType.FIXED_WIDTH.getCode());
        assertEquals("custom", FileType.CUSTOM.getCode());
    }

    @Test
    @DisplayName("Should have correct file type descriptions")
    void testFileTypeDescriptions() {
        assertEquals("Comma-separated values", FileType.CSV.getDescription());
        assertEquals("JavaScript Object Notation", FileType.JSON.getDescription());
        assertEquals("Extensible Markup Language", FileType.XML.getDescription());
        assertEquals("Fixed-width text format", FileType.FIXED_WIDTH.getDescription());
        assertEquals("Custom file format", FileType.CUSTOM.getDescription());
    }

    @Test
    @DisplayName("Should convert code to file type")
    void testFromCodeConversion() {
        assertEquals(FileType.CSV, FileType.fromCode("csv"));
        assertEquals(FileType.JSON, FileType.fromCode("json"));
        assertEquals(FileType.XML, FileType.fromCode("xml"));
        assertEquals(FileType.FIXED_WIDTH, FileType.fromCode("fixed-width"));
        assertEquals(FileType.CUSTOM, FileType.fromCode("custom"));
    }

    @Test
    @DisplayName("Should handle case insensitive code conversion")
    void testFromCodeCaseInsensitive() {
        assertEquals(FileType.CSV, FileType.fromCode("CSV"));
        assertEquals(FileType.JSON, FileType.fromCode("JSON"));
        assertEquals(FileType.XML, FileType.fromCode("XML"));
        assertEquals(FileType.FIXED_WIDTH, FileType.fromCode("FIXED-WIDTH"));
        assertEquals(FileType.CUSTOM, FileType.fromCode("CUSTOM"));
    }

    @Test
    @DisplayName("Should return CSV for unknown or null codes")
    void testFromCodeUnknown() {
        assertEquals(FileType.CSV, FileType.fromCode("unknown"));
        assertEquals(FileType.CSV, FileType.fromCode("invalid"));
        assertEquals(FileType.CSV, FileType.fromCode(""));
        assertEquals(FileType.CSV, FileType.fromCode(null));
    }

    @Test
    @DisplayName("Should convert type string to FileType enum")
    void testGetFileType() {
        config.setType("csv");
        assertEquals(FileType.CSV, config.getFileType());
        
        config.setType("json");
        assertEquals(FileType.JSON, config.getFileType());
        
        config.setType("xml");
        assertEquals(FileType.XML, config.getFileType());
        
        config.setType("fixed-width");
        assertEquals(FileType.FIXED_WIDTH, config.getFileType());
        
        config.setType("custom");
        assertEquals(FileType.CUSTOM, config.getFileType());
    }

    // ========================================
    // Basic File Format Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get basic file format properties")
    void testBasicFileFormatProperties() {
        config.setType("json");
        config.setDelimiter(";");
        config.setQuoteCharacter("'");
        config.setEscapeCharacter("/");
        config.setHeaderRow(false);
        config.setSkipLines(2);
        config.setEncoding("ISO-8859-1");
        config.setDateFormat("dd/MM/yyyy");
        config.setTimestampFormat("dd/MM/yyyy HH:mm:ss");
        config.setNullValue("NULL");
        
        assertEquals("json", config.getType());
        assertEquals(";", config.getDelimiter());
        assertEquals("'", config.getQuoteCharacter());
        assertEquals("/", config.getEscapeCharacter());
        assertFalse(config.getHeaderRow());
        assertEquals(2, config.getSkipLines());
        assertEquals("ISO-8859-1", config.getEncoding());
        assertEquals("dd/MM/yyyy", config.getDateFormat());
        assertEquals("dd/MM/yyyy HH:mm:ss", config.getTimestampFormat());
        assertEquals("NULL", config.getNullValue());
    }

    @Test
    @DisplayName("Should handle null basic file format properties")
    void testNullBasicFileFormatProperties() {
        config.setType(null);
        config.setDelimiter(null);
        config.setQuoteCharacter(null);
        config.setEscapeCharacter(null);
        config.setHeaderRow(null);
        config.setSkipLines(null);
        config.setEncoding(null);
        config.setDateFormat(null);
        config.setTimestampFormat(null);
        config.setNullValue(null);
        
        assertNull(config.getType());
        assertNull(config.getDelimiter());
        assertNull(config.getQuoteCharacter());
        assertNull(config.getEscapeCharacter());
        assertNull(config.getHeaderRow());
        assertNull(config.getSkipLines());
        assertNull(config.getEncoding());
        assertNull(config.getDateFormat());
        assertNull(config.getTimestampFormat());
        assertNull(config.getNullValue());
    }

    // ========================================
    // JSON-Specific Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get JSON-specific properties")
    void testJsonSpecificProperties() {
        config.setType("json");
        config.setRootPath("$.data.records");
        config.setFlattenArrays(true);
        
        assertEquals("$.data.records", config.getRootPath());
        assertTrue(config.getFlattenArrays());
        assertTrue(config.shouldFlattenArrays());
    }

    @Test
    @DisplayName("Should handle null JSON-specific properties")
    void testNullJsonSpecificProperties() {
        config.setRootPath(null);
        config.setFlattenArrays(null);
        
        assertNull(config.getRootPath());
        assertNull(config.getFlattenArrays());
        assertFalse(config.shouldFlattenArrays()); // null should be false
    }

    @Test
    @DisplayName("Should provide boolean convenience method for flatten arrays")
    void testFlattenArraysConvenienceMethod() {
        config.setFlattenArrays(true);
        assertTrue(config.shouldFlattenArrays());

        config.setFlattenArrays(false);
        assertFalse(config.shouldFlattenArrays());

        config.setFlattenArrays(null);
        assertFalse(config.shouldFlattenArrays()); // null should be false
    }

    // ========================================
    // XML-Specific Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get XML-specific properties")
    void testXmlSpecificProperties() {
        config.setType("xml");
        config.setRootElement("root");
        config.setRecordElement("record");

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("ns1", "http://example.com/ns1");
        namespaces.put("ns2", "http://example.com/ns2");
        config.setNamespaces(namespaces);

        assertEquals("root", config.getRootElement());
        assertEquals("record", config.getRecordElement());
        assertEquals(namespaces, config.getNamespaces());
        assertEquals("http://example.com/ns1", config.getNamespaces().get("ns1"));
        assertEquals("http://example.com/ns2", config.getNamespaces().get("ns2"));
    }

    @Test
    @DisplayName("Should handle null XML-specific properties")
    void testNullXmlSpecificProperties() {
        config.setRootElement(null);
        config.setRecordElement(null);
        config.setNamespaces(null);

        assertNull(config.getRootElement());
        assertNull(config.getRecordElement());
        assertNotNull(config.getNamespaces()); // Should return empty map, not null
        assertTrue(config.getNamespaces().isEmpty());
    }

    @Test
    @DisplayName("Should allow modification of namespaces map")
    void testNamespacesModification() {
        config.getNamespaces().put("test", "http://test.com");
        assertEquals("http://test.com", config.getNamespaces().get("test"));
    }

    // ========================================
    // Fixed-Width Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should set and get fixed-width field definitions")
    void testFixedWidthFieldDefinitions() {
        config.setType("fixed-width");

        List<FieldDefinition> fieldDefs = Arrays.asList(
            new FieldDefinition("id", 1, 10),
            new FieldDefinition("name", 11, 30),
            new FieldDefinition("amount", 41, 15)
        );

        config.setFieldDefinitions(fieldDefs);

        assertEquals(fieldDefs, config.getFieldDefinitions());
        assertEquals(3, config.getFieldDefinitions().size());

        FieldDefinition firstField = config.getFieldDefinitions().get(0);
        assertEquals("id", firstField.getName());
        assertEquals(1, firstField.getStartPosition());
        assertEquals(10, firstField.getLength());
    }

    @Test
    @DisplayName("Should handle null field definitions")
    void testNullFieldDefinitions() {
        config.setFieldDefinitions(null);
        assertNull(config.getFieldDefinitions());
    }

    // ========================================
    // Column Mapping Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should set and get column mapping properties")
    void testColumnMappingProperties() {
        Map<String, String> columnMappings = new HashMap<>();
        columnMappings.put("id", "identifier");
        columnMappings.put("name", "full_name");
        columnMappings.put("amount", "transaction_amount");

        config.setColumnMappings(columnMappings);
        config.setKeyColumn("id");

        assertEquals(columnMappings, config.getColumnMappings());
        assertEquals("id", config.getKeyColumn());
        assertEquals("identifier", config.getColumnMappings().get("id"));
        assertEquals("full_name", config.getColumnMappings().get("name"));
        assertEquals("transaction_amount", config.getColumnMappings().get("amount"));
    }

    @Test
    @DisplayName("Should handle null column mapping properties")
    void testNullColumnMappingProperties() {
        config.setColumnMappings(null);
        config.setKeyColumn(null);

        assertNotNull(config.getColumnMappings()); // Should return empty map, not null
        assertTrue(config.getColumnMappings().isEmpty());
        assertNull(config.getKeyColumn());
    }

    @Test
    @DisplayName("Should allow modification of column mappings map")
    void testColumnMappingsModification() {
        config.getColumnMappings().put("test", "test_column");
        assertEquals("test_column", config.getColumnMappings().get("test"));
    }

    // ========================================
    // Custom Format Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should set and get custom format properties")
    void testCustomFormatProperties() {
        config.setType("custom");
        config.setCustomParser("com.example.CustomFileParser");

        Map<String, Object> customProps = new HashMap<>();
        customProps.put("separator", "|");
        customProps.put("maxLines", 1000);
        customProps.put("strictMode", true);

        config.setCustomProperties(customProps);

        assertEquals("com.example.CustomFileParser", config.getCustomParser());
        assertEquals(customProps, config.getCustomProperties());
        assertEquals("|", config.getCustomProperties().get("separator"));
        assertEquals(1000, config.getCustomProperties().get("maxLines"));
        assertEquals(true, config.getCustomProperties().get("strictMode"));
    }

    @Test
    @DisplayName("Should handle null custom format properties")
    void testNullCustomFormatProperties() {
        config.setCustomParser(null);
        config.setCustomProperties(null);

        assertNull(config.getCustomParser());
        assertNotNull(config.getCustomProperties()); // Should return empty map, not null
        assertTrue(config.getCustomProperties().isEmpty());
    }

    @Test
    @DisplayName("Should allow modification of custom properties map")
    void testCustomPropertiesModification() {
        config.getCustomProperties().put("test", "value");
        assertEquals("value", config.getCustomProperties().get("test"));
    }

    // ========================================
    // Validation Tests
    // ========================================

    @Test
    @DisplayName("Should validate successfully with valid CSV configuration")
    void testValidationValidCsv() {
        config.setType("csv");
        config.setDelimiter(",");
        config.setEncoding("UTF-8");
        config.setSkipLines(0);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should fail validation with null encoding")
    void testValidationNullEncoding() {
        config.setType("csv");
        config.setEncoding(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("File encoding is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with empty encoding")
    void testValidationEmptyEncoding() {
        config.setType("csv");
        config.setEncoding("   ");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("File encoding is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with negative skip lines")
    void testValidationNegativeSkipLines() {
        config.setType("csv");
        config.setEncoding("UTF-8");
        config.setSkipLines(-1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Skip lines cannot be negative", exception.getMessage());
    }

    @Test
    @DisplayName("Should allow zero skip lines")
    void testValidationZeroSkipLines() {
        config.setType("csv");
        config.setEncoding("UTF-8");
        config.setSkipLines(0);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should fail validation for CSV without delimiter")
    void testValidationCsvMissingDelimiter() {
        config.setType("csv");
        config.setEncoding("UTF-8");
        config.setDelimiter(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Delimiter is required for CSV files", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation for CSV with empty delimiter")
    void testValidationCsvEmptyDelimiter() {
        config.setType("csv");
        config.setEncoding("UTF-8");
        config.setDelimiter("");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Delimiter is required for CSV files", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate successfully with valid JSON configuration")
    void testValidationValidJson() {
        config.setType("json");
        config.setEncoding("UTF-8");

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should validate successfully with valid XML configuration")
    void testValidationValidXml() {
        config.setType("xml");
        config.setEncoding("UTF-8");
        config.setRecordElement("record");

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should fail validation for XML without record element")
    void testValidationXmlMissingRecordElement() {
        config.setType("xml");
        config.setEncoding("UTF-8");
        config.setRecordElement(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Record element is required for XML files", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation for XML with empty record element")
    void testValidationXmlEmptyRecordElement() {
        config.setType("xml");
        config.setEncoding("UTF-8");
        config.setRecordElement("   ");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Record element is required for XML files", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate successfully with valid fixed-width configuration")
    void testValidationValidFixedWidth() {
        config.setType("fixed-width");
        config.setEncoding("UTF-8");
        config.setFieldDefinitions(Arrays.asList(
            new FieldDefinition("id", 1, 10),
            new FieldDefinition("name", 11, 30)
        ));

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should fail validation for fixed-width without field definitions")
    void testValidationFixedWidthMissingFieldDefinitions() {
        config.setType("fixed-width");
        config.setEncoding("UTF-8");
        config.setFieldDefinitions(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Field definitions are required for fixed-width files", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation for fixed-width with empty field definitions")
    void testValidationFixedWidthEmptyFieldDefinitions() {
        config.setType("fixed-width");
        config.setEncoding("UTF-8");
        config.setFieldDefinitions(new ArrayList<>());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Field definitions are required for fixed-width files", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate successfully with valid custom configuration")
    void testValidationValidCustom() {
        config.setType("custom");
        config.setEncoding("UTF-8");
        config.setCustomParser("com.example.CustomParser");

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should fail validation for custom without parser")
    void testValidationCustomMissingParser() {
        config.setType("custom");
        config.setEncoding("UTF-8");
        config.setCustomParser(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Custom parser class is required for custom file formats", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation for custom with empty parser")
    void testValidationCustomEmptyParser() {
        config.setType("custom");
        config.setEncoding("UTF-8");
        config.setCustomParser("   ");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Custom parser class is required for custom file formats", exception.getMessage());
    }

    // ========================================
    // Copy Method Tests
    // ========================================

    @Test
    @DisplayName("Should create deep copy with all properties")
    void testCopyMethod() {
        // Set up original configuration with all properties
        config.setType("xml");
        config.setDelimiter(";");
        config.setQuoteCharacter("'");
        config.setEscapeCharacter("/");
        config.setHeaderRow(false);
        config.setSkipLines(2);
        config.setEncoding("ISO-8859-1");
        config.setDateFormat("dd/MM/yyyy");
        config.setTimestampFormat("dd/MM/yyyy HH:mm:ss");
        config.setNullValue("NULL");
        config.setRootPath("$.data");
        config.setFlattenArrays(true);
        config.setRootElement("root");
        config.setRecordElement("record");
        config.getNamespaces().put("ns1", "http://example.com/ns1");
        config.setFieldDefinitions(Arrays.asList(new FieldDefinition("id", 1, 10)));
        config.getColumnMappings().put("id", "identifier");
        config.setKeyColumn("id");
        config.setCustomParser("com.example.Parser");
        config.getCustomProperties().put("custom", "value");

        // Create copy
        FileFormatConfig copy = config.copy();

        // Verify all properties are copied
        assertEquals(config.getType(), copy.getType());
        assertEquals(config.getDelimiter(), copy.getDelimiter());
        assertEquals(config.getQuoteCharacter(), copy.getQuoteCharacter());
        assertEquals(config.getEscapeCharacter(), copy.getEscapeCharacter());
        assertEquals(config.getHeaderRow(), copy.getHeaderRow());
        assertEquals(config.getSkipLines(), copy.getSkipLines());
        assertEquals(config.getEncoding(), copy.getEncoding());
        assertEquals(config.getDateFormat(), copy.getDateFormat());
        assertEquals(config.getTimestampFormat(), copy.getTimestampFormat());
        assertEquals(config.getNullValue(), copy.getNullValue());
        assertEquals(config.getRootPath(), copy.getRootPath());
        assertEquals(config.getFlattenArrays(), copy.getFlattenArrays());
        assertEquals(config.getRootElement(), copy.getRootElement());
        assertEquals(config.getRecordElement(), copy.getRecordElement());
        assertEquals(config.getKeyColumn(), copy.getKeyColumn());
        assertEquals(config.getCustomParser(), copy.getCustomParser());

        // Verify maps are deep copied
        assertNotSame(config.getNamespaces(), copy.getNamespaces());
        assertEquals(config.getNamespaces(), copy.getNamespaces());

        assertNotSame(config.getColumnMappings(), copy.getColumnMappings());
        assertEquals(config.getColumnMappings(), copy.getColumnMappings());

        assertNotSame(config.getCustomProperties(), copy.getCustomProperties());
        assertEquals(config.getCustomProperties(), copy.getCustomProperties());

        // Verify field definitions are shallow copied (same reference for now)
        assertSame(config.getFieldDefinitions(), copy.getFieldDefinitions());
    }

    @Test
    @DisplayName("Should handle null values in copy method")
    void testCopyWithNullValues() {
        // Create a config with explicit null values (overriding defaults)
        config.setType(null);
        config.setDelimiter(null);
        config.setQuoteCharacter(null);
        config.setEscapeCharacter(null);
        config.setHeaderRow(null);
        config.setSkipLines(null);
        config.setEncoding(null);
        config.setDateFormat(null);
        config.setTimestampFormat(null);
        config.setNullValue(null);
        config.setRootPath(null);
        config.setFlattenArrays(null);
        config.setRootElement(null);
        config.setRecordElement(null);
        config.setFieldDefinitions(null);
        config.setKeyColumn(null);
        config.setCustomParser(null);

        FileFormatConfig copy = config.copy();

        // All properties should be null in the copy
        assertNull(copy.getType());
        assertNull(copy.getDelimiter());
        assertNull(copy.getQuoteCharacter());
        assertNull(copy.getEscapeCharacter());
        assertNull(copy.getHeaderRow());
        assertNull(copy.getSkipLines());
        assertNull(copy.getEncoding());
        assertNull(copy.getDateFormat());
        assertNull(copy.getTimestampFormat());
        assertNull(copy.getNullValue());
        assertNull(copy.getRootPath());
        assertNull(copy.getFlattenArrays());
        assertNull(copy.getRootElement());
        assertNull(copy.getRecordElement());
        assertNull(copy.getFieldDefinitions());
        assertNull(copy.getKeyColumn());
        assertNull(copy.getCustomParser());

        // Maps should be empty but not null
        assertNotNull(copy.getNamespaces());
        assertNotNull(copy.getColumnMappings());
        assertNotNull(copy.getCustomProperties());
        assertTrue(copy.getNamespaces().isEmpty());
        assertTrue(copy.getColumnMappings().isEmpty());
        assertTrue(copy.getCustomProperties().isEmpty());
    }

    @Test
    @DisplayName("Should create independent copy that can be modified")
    void testCopyIndependence() {
        config.setType("csv");
        config.setDelimiter(",");
        config.getNamespaces().put("original", "http://original.com");
        config.getColumnMappings().put("original", "orig_col");
        config.getCustomProperties().put("original", "value");

        FileFormatConfig copy = config.copy();

        // Modify original
        config.setType("json");
        config.setDelimiter(";");
        config.getNamespaces().put("new", "http://new.com");
        config.getColumnMappings().put("new", "new_col");
        config.getCustomProperties().put("new", "new_value");

        // Copy should remain unchanged
        assertEquals("csv", copy.getType());
        assertEquals(",", copy.getDelimiter());
        assertEquals(1, copy.getNamespaces().size());
        assertEquals("http://original.com", copy.getNamespaces().get("original"));
        assertNull(copy.getNamespaces().get("new"));
        assertEquals(1, copy.getColumnMappings().size());
        assertEquals("orig_col", copy.getColumnMappings().get("original"));
        assertNull(copy.getColumnMappings().get("new"));
        assertEquals(1, copy.getCustomProperties().size());
        assertEquals("value", copy.getCustomProperties().get("original"));
        assertNull(copy.getCustomProperties().get("new"));

        // Modify copy
        copy.setType("xml");
        copy.setDelimiter("|");
        copy.getNamespaces().put("copy", "http://copy.com");
        copy.getColumnMappings().put("copy", "copy_col");
        copy.getCustomProperties().put("copy", "copy_value");

        // Original should remain unchanged
        assertEquals("json", config.getType());
        assertEquals(";", config.getDelimiter());
        assertEquals(2, config.getNamespaces().size());
        assertNull(config.getNamespaces().get("copy"));
        assertEquals(2, config.getColumnMappings().size());
        assertNull(config.getColumnMappings().get("copy"));
        assertEquals(2, config.getCustomProperties().size());
        assertNull(config.getCustomProperties().get("copy"));
    }

    // ========================================
    // Equals and HashCode Tests
    // ========================================

    @Test
    @DisplayName("Should be equal to itself")
    void testEqualsReflexive() {
        config.setType("csv");
        config.setDelimiter(",");

        assertEquals(config, config);
        assertEquals(config.hashCode(), config.hashCode());
    }

    @Test
    @DisplayName("Should be equal to another instance with same properties")
    void testEqualsSymmetric() {
        config.setType("csv");
        config.setDelimiter(",");
        config.setEncoding("UTF-8");
        config.setHeaderRow(true);

        FileFormatConfig other = new FileFormatConfig();
        other.setType("csv");
        other.setDelimiter(",");
        other.setEncoding("UTF-8");
        other.setHeaderRow(true);

        assertEquals(config, other);
        assertEquals(other, config);
        assertEquals(config.hashCode(), other.hashCode());
    }

    @Test
    @DisplayName("Should not be equal to null")
    void testEqualsNull() {
        config.setType("csv");

        assertNotEquals(config, null);
    }

    @Test
    @DisplayName("Should not be equal to different class")
    void testEqualsDifferentClass() {
        config.setType("csv");

        assertNotEquals(config, "not a FileFormatConfig");
        assertNotEquals(config, new Object());
    }

    @Test
    @DisplayName("Should not be equal when types differ")
    void testEqualsTypeDifference() {
        config.setType("csv");
        config.setDelimiter(",");

        FileFormatConfig other = new FileFormatConfig();
        other.setType("json");
        other.setDelimiter(",");

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when delimiters differ")
    void testEqualsDelimiterDifference() {
        config.setType("csv");
        config.setDelimiter(",");

        FileFormatConfig other = new FileFormatConfig();
        other.setType("csv");
        other.setDelimiter(";");

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when encodings differ")
    void testEqualsEncodingDifference() {
        config.setType("csv");
        config.setEncoding("UTF-8");

        FileFormatConfig other = new FileFormatConfig();
        other.setType("csv");
        other.setEncoding("ISO-8859-1");

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when header row flags differ")
    void testEqualsHeaderRowDifference() {
        config.setType("csv");
        config.setHeaderRow(true);

        FileFormatConfig other = new FileFormatConfig();
        other.setType("csv");
        other.setHeaderRow(false);

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should handle null values in equals comparison")
    void testEqualsWithNullValues() {
        FileFormatConfig config1 = new FileFormatConfig();
        FileFormatConfig config2 = new FileFormatConfig();

        // Both have default values, should be equal
        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());

        // Change one property to make them different
        config1.setType("json");
        assertNotEquals(config1, config2);

        // Make them the same again
        config2.setType("json");
        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    // ========================================
    // ToString Tests
    // ========================================

    @Test
    @DisplayName("Should generate meaningful toString representation")
    void testToString() {
        config.setType("csv");
        config.setDelimiter(";");
        config.setEncoding("ISO-8859-1");
        config.setHeaderRow(false);
        config.setSkipLines(2);

        String result = config.toString();

        assertNotNull(result);
        assertTrue(result.contains("csv"));
        assertTrue(result.contains(";"));
        assertTrue(result.contains("ISO-8859-1"));
        assertTrue(result.contains("false"));
        assertTrue(result.contains("2"));
        assertTrue(result.contains("FileFormatConfig"));
    }

    @Test
    @DisplayName("Should handle null values in toString")
    void testToStringWithNulls() {
        // Set some properties to null explicitly
        config.setType(null);
        config.setDelimiter(null);
        config.setEncoding(null);
        config.setHeaderRow(null);
        config.setSkipLines(null);

        String result = config.toString();

        assertNotNull(result);
        assertTrue(result.contains("FileFormatConfig"));
        assertTrue(result.contains("null"));
    }

    @Test
    @DisplayName("Should be consistent toString output")
    void testToStringConsistency() {
        config.setType("json");
        config.setDelimiter(",");
        config.setEncoding("UTF-8");

        String result1 = config.toString();
        String result2 = config.toString();

        assertEquals(result1, result2);
    }

    // ========================================
    // FieldDefinition Inner Class Tests
    // ========================================

    @Test
    @DisplayName("Should create FieldDefinition with default constructor")
    void testFieldDefinitionDefaultConstructor() {
        FieldDefinition fieldDef = new FieldDefinition();

        assertNull(fieldDef.getName());
        assertNull(fieldDef.getStartPosition());
        assertNull(fieldDef.getLength());
        assertEquals("string", fieldDef.getType()); // Default is string
        assertNull(fieldDef.getFormat());
    }

    @Test
    @DisplayName("Should create FieldDefinition with parameterized constructor")
    void testFieldDefinitionParameterizedConstructor() {
        FieldDefinition fieldDef = new FieldDefinition("id", 1, 10);

        assertEquals("id", fieldDef.getName());
        assertEquals(1, fieldDef.getStartPosition());
        assertEquals(10, fieldDef.getLength());
        assertEquals("string", fieldDef.getType()); // Default is string
        assertNull(fieldDef.getFormat());
    }

    @Test
    @DisplayName("Should set and get FieldDefinition properties")
    void testFieldDefinitionProperties() {
        FieldDefinition fieldDef = new FieldDefinition();

        fieldDef.setName("amount");
        fieldDef.setStartPosition(21);
        fieldDef.setLength(15);
        fieldDef.setType("decimal");
        fieldDef.setFormat("0.00");

        assertEquals("amount", fieldDef.getName());
        assertEquals(21, fieldDef.getStartPosition());
        assertEquals(15, fieldDef.getLength());
        assertEquals("decimal", fieldDef.getType());
        assertEquals("0.00", fieldDef.getFormat());
    }

    @Test
    @DisplayName("Should handle null FieldDefinition properties")
    void testFieldDefinitionNullProperties() {
        FieldDefinition fieldDef = new FieldDefinition();

        fieldDef.setName(null);
        fieldDef.setStartPosition(null);
        fieldDef.setLength(null);
        fieldDef.setType(null);
        fieldDef.setFormat(null);

        assertNull(fieldDef.getName());
        assertNull(fieldDef.getStartPosition());
        assertNull(fieldDef.getLength());
        assertNull(fieldDef.getType());
        assertNull(fieldDef.getFormat());
    }

    @Test
    @DisplayName("Should generate meaningful FieldDefinition toString")
    void testFieldDefinitionToString() {
        FieldDefinition fieldDef = new FieldDefinition("id", 1, 10);

        String result = fieldDef.toString();

        assertNotNull(result);
        assertTrue(result.contains("id"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("10"));
        assertTrue(result.contains("FieldDefinition"));
    }

    @Test
    @DisplayName("Should handle null values in FieldDefinition toString")
    void testFieldDefinitionToStringWithNulls() {
        FieldDefinition fieldDef = new FieldDefinition();
        fieldDef.setName(null);
        fieldDef.setStartPosition(null);
        fieldDef.setLength(null);

        String result = fieldDef.toString();

        assertNotNull(result);
        assertTrue(result.contains("FieldDefinition"));
        assertTrue(result.contains("null"));
    }

    @Test
    @DisplayName("Should create multiple FieldDefinitions for fixed-width parsing")
    void testMultipleFieldDefinitions() {
        List<FieldDefinition> fieldDefs = Arrays.asList(
            new FieldDefinition("id", 1, 10),
            new FieldDefinition("name", 11, 30),
            new FieldDefinition("amount", 41, 15),
            new FieldDefinition("date", 56, 10)
        );

        assertEquals(4, fieldDefs.size());

        // Verify first field
        FieldDefinition idField = fieldDefs.get(0);
        assertEquals("id", idField.getName());
        assertEquals(1, idField.getStartPosition());
        assertEquals(10, idField.getLength());

        // Verify last field
        FieldDefinition dateField = fieldDefs.get(3);
        assertEquals("date", dateField.getName());
        assertEquals(56, dateField.getStartPosition());
        assertEquals(10, dateField.getLength());
    }

    @Test
    @DisplayName("Should support different field types in FieldDefinition")
    void testFieldDefinitionTypes() {
        FieldDefinition stringField = new FieldDefinition("name", 1, 30);
        stringField.setType("string");

        FieldDefinition intField = new FieldDefinition("count", 31, 10);
        intField.setType("integer");

        FieldDefinition decimalField = new FieldDefinition("amount", 41, 15);
        decimalField.setType("decimal");
        decimalField.setFormat("0.00");

        FieldDefinition dateField = new FieldDefinition("date", 56, 10);
        dateField.setType("date");
        dateField.setFormat("yyyy-MM-dd");

        assertEquals("string", stringField.getType());
        assertEquals("integer", intField.getType());
        assertEquals("decimal", decimalField.getType());
        assertEquals("0.00", decimalField.getFormat());
        assertEquals("date", dateField.getType());
        assertEquals("yyyy-MM-dd", dateField.getFormat());
    }
}
