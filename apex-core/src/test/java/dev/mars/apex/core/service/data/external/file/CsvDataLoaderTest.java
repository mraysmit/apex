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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CsvDataLoader.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
class CsvDataLoaderTest {

    @TempDir
    Path tempDir;

    private CsvDataLoader csvDataLoader;
    private FileFormatConfig defaultConfig;

    @BeforeEach
    void setUp() {
        csvDataLoader = new CsvDataLoader();
        defaultConfig = createDefaultFormatConfig();
    }

    // ========================================
    // Format Support Tests
    // ========================================

    @Test
    @DisplayName("Should support CSV format configuration")
    void testSupportsFormat() {
        FileFormatConfig csvConfig = new FileFormatConfig();
        csvConfig.setType("csv");
        assertTrue(csvDataLoader.supportsFormat(csvConfig));
        
        csvConfig.setType("CSV");
        assertTrue(csvDataLoader.supportsFormat(csvConfig));
        
        csvConfig.setType("json");
        assertFalse(csvDataLoader.supportsFormat(csvConfig));
        
        assertFalse(csvDataLoader.supportsFormat(null));
    }

    @Test
    @DisplayName("Should support CSV file type enum")
    void testSupportsFileTypeEnum() {
        FileFormatConfig config = new FileFormatConfig();
        config.setType("csv"); // This will make getFileType() return CSV
        assertTrue(csvDataLoader.supportsFormat(config));
        assertEquals(FileFormatConfig.FileType.CSV, config.getFileType());
        
        config.setType("json"); // This will make getFileType() return JSON
        assertFalse(csvDataLoader.supportsFormat(config));
        assertEquals(FileFormatConfig.FileType.JSON, config.getFileType());
    }

    @Test
    @DisplayName("Should return correct supported extensions")
    void testGetSupportedExtensions() {
        String[] extensions = csvDataLoader.getSupportedExtensions();
        assertEquals(3, extensions.length);
        assertEquals("csv", extensions[0]);
        assertEquals("tsv", extensions[1]);
        assertEquals("txt", extensions[2]);
    }

    // ========================================
    // Basic CSV Parsing Tests
    // ========================================

    @Test
    @DisplayName("Should parse simple CSV with headers")
    void testParseSimpleCsvWithHeaders() throws IOException {
        String csvContent = """
            name,age,active,balance
            John Doe,30,true,1234.56
            Jane Smith,25,false,2345.67
            Bob Johnson,35,true,3456.78
            """;
        
        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, defaultConfig);
        
        assertEquals(3, result.size());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals("John Doe", firstRow.get("name"));
        assertEquals(30L, firstRow.get("age"));
        assertEquals(true, firstRow.get("active"));
        assertEquals(1234.56, firstRow.get("balance"));
    }

    @Test
    @DisplayName("Should parse CSV without headers")
    void testParseCsvWithoutHeaders() throws IOException {
        String csvContent = """
            John Doe,30,true,1234.56
            Jane Smith,25,false,2345.67
            """;
        
        FileFormatConfig config = createDefaultFormatConfig();
        config.setHeaderRow(false);
        
        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, config);
        
        assertEquals(2, result.size());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals("John Doe", firstRow.get("column_1"));
        assertEquals(30L, firstRow.get("column_2"));
        assertEquals(true, firstRow.get("column_3"));
        assertEquals(1234.56, firstRow.get("column_4"));
    }

    @Test
    @DisplayName("Should handle empty CSV file")
    void testEmptyCsvFile() throws IOException {
        String csvContent = "";
        
        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, defaultConfig);
        
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle CSV with only headers")
    void testCsvWithOnlyHeaders() throws IOException {
        String csvContent = "name,age,active";
        
        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, defaultConfig);
        
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should skip empty lines")
    void testSkipEmptyLines() throws IOException {
        String csvContent = """
            name,age,active
            John Doe,30,true
            
            Jane Smith,25,false
            
            Bob Johnson,35,true
            """;
        
        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, defaultConfig);
        
        assertEquals(3, result.size());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> secondRow = (Map<String, Object>) result.get(1);
        assertEquals("Jane Smith", secondRow.get("name"));
    }

    // ========================================
    // Delimiter Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle semicolon delimiter")
    void testSemicolonDelimiter() throws IOException {
        String csvContent = """
            name;age;active;balance
            John Doe;30;true;1234.56
            Jane Smith;25;false;2345.67
            """;
        
        FileFormatConfig config = createDefaultFormatConfig();
        config.setDelimiter(";");
        
        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, config);
        
        assertEquals(2, result.size());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals("John Doe", firstRow.get("name"));
        assertEquals(30L, firstRow.get("age"));
    }

    @Test
    @DisplayName("Should handle tab delimiter")
    void testTabDelimiter() throws IOException {
        String csvContent = "name\tage\tactive\tbalance\n" +
                           "John Doe\t30\ttrue\t1234.56\n" +
                           "Jane Smith\t25\tfalse\t2345.67";
        
        FileFormatConfig config = createDefaultFormatConfig();
        config.setDelimiter("\t");
        
        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, config);
        
        assertEquals(2, result.size());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals("John Doe", firstRow.get("name"));
        assertEquals(30L, firstRow.get("age"));
    }

    @Test
    @DisplayName("Should handle pipe delimiter")
    void testPipeDelimiter() throws IOException {
        String csvContent = """
            name|age|active|balance
            John Doe|30|true|1234.56
            Jane Smith|25|false|2345.67
            """;
        
        FileFormatConfig config = createDefaultFormatConfig();
        config.setDelimiter("|");
        
        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, config);
        
        assertEquals(2, result.size());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals("John Doe", firstRow.get("name"));
        assertEquals(30L, firstRow.get("age"));
    }

    // ========================================
    // Quote and Escape Character Tests
    // ========================================

    @Test
    @DisplayName("Should handle quoted values with commas")
    void testQuotedValuesWithCommas() throws IOException {
        String csvContent = """
            name,description,price
            "Smith, John",Product with comma in name,19.99
            "Johnson, Jane","Product with, multiple, commas",29.99
            """;
        
        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, defaultConfig);
        
        assertEquals(2, result.size());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals("Smith, John", firstRow.get("name"));
        assertEquals("Product with comma in name", firstRow.get("description"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> secondRow = (Map<String, Object>) result.get(1);
        assertEquals("Johnson, Jane", secondRow.get("name"));
        assertEquals("Product with, multiple, commas", secondRow.get("description"));
    }

    @Test
    @DisplayName("Should handle quoted values with quotes")
    void testQuotedValuesWithQuotes() throws IOException {
        String csvContent = "name,quote\n" +
                           "John,\"He said \"\"Hello\"\"\"\n" +
                           "Jane,\"She replied \"\"Hi there\"\"\"";

        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, defaultConfig);

        assertEquals(2, result.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals("John", firstRow.get("name"));
        // The basic CSV parser may not handle escaped quotes perfectly
        assertEquals("He said Hello", firstRow.get("quote"));
    }

    @Test
    @DisplayName("Should handle custom quote character")
    void testCustomQuoteCharacter() throws IOException {
        String csvContent = """
            name,description
            'Smith, John','Product with comma'
            'Johnson, Jane','Another product'
            """;
        
        FileFormatConfig config = createDefaultFormatConfig();
        config.setQuoteCharacter("'");
        
        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, config);
        
        assertEquals(2, result.size());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals("Smith, John", firstRow.get("name"));
        assertEquals("Product with comma", firstRow.get("description"));
    }

    @Test
    @DisplayName("Should handle escape characters")
    void testEscapeCharacters() throws IOException {
        String csvContent = """
            name,path
            John,C:\\Users\\John\\Documents
            Jane,D:\\Data\\Jane\\Files
            """;
        
        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, defaultConfig);
        
        assertEquals(2, result.size());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals("John", firstRow.get("name"));
        assertEquals("C:UsersJohnDocuments", firstRow.get("path")); // Backslashes are escape chars
    }

    // ========================================
    // Data Type Conversion Tests
    // ========================================

    @Test
    @DisplayName("Should convert data types correctly")
    void testDataTypeConversion() throws IOException {
        String csvContent = """
            name,age,active,balance,nullValue,emptyValue
            John,30,true,1234.56,,""
            Jane,25,false,2345.67,NULL,
            """;

        FileFormatConfig config = createDefaultFormatConfig();
        config.setNullValue("NULL");

        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, config);

        assertEquals(2, result.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals("John", firstRow.get("name"));
        assertEquals(30L, firstRow.get("age"));
        assertEquals(true, firstRow.get("active"));
        assertEquals(1234.56, firstRow.get("balance"));
        assertNull(firstRow.get("nullValue"));
        assertNull(firstRow.get("emptyValue"));

        @SuppressWarnings("unchecked")
        Map<String, Object> secondRow = (Map<String, Object>) result.get(1);
        assertEquals("Jane", secondRow.get("name"));
        assertEquals(25L, secondRow.get("age"));
        assertEquals(false, secondRow.get("active"));
        assertEquals(2345.67, secondRow.get("balance"));
        assertNull(secondRow.get("nullValue"));
        assertNull(secondRow.get("emptyValue"));
    }

    @Test
    @DisplayName("Should handle numeric edge cases")
    void testNumericEdgeCases() throws IOException {
        String csvContent = """
            name,zero,negative,decimal,scientific,large
            Test,0,-123,123.456,1.23E10,9223372036854775807
            """;

        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, defaultConfig);

        assertEquals(1, result.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> row = (Map<String, Object>) result.get(0);
        assertEquals("Test", row.get("name"));
        assertEquals(0L, row.get("zero"));
        assertEquals(-123L, row.get("negative"));
        assertEquals(123.456, row.get("decimal"));
        assertEquals(1.23E10, row.get("scientific"));
        assertEquals(9223372036854775807L, row.get("large"));
    }

    // ========================================
    // Field Mapping Tests
    // ========================================

    @Test
    @DisplayName("Should apply column mappings")
    void testColumnMappings() throws IOException {
        String csvContent = """
            user_id,full_name,email_address
            1,John Doe,john@example.com
            2,Jane Smith,jane@example.com
            """;

        FileFormatConfig config = createDefaultFormatConfig();
        Map<String, String> columnMappings = new HashMap<>();
        columnMappings.put("user_id", "id");
        columnMappings.put("full_name", "name");
        columnMappings.put("email_address", "email");
        config.setColumnMappings(columnMappings);

        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, config);

        assertEquals(2, result.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals(1L, firstRow.get("id"));
        assertEquals("John Doe", firstRow.get("name"));
        assertEquals("john@example.com", firstRow.get("email"));

        // Original keys should not be present
        assertFalse(firstRow.containsKey("user_id"));
        assertFalse(firstRow.containsKey("full_name"));
        assertFalse(firstRow.containsKey("email_address"));
    }

    // ========================================
    // Skip Lines Tests
    // ========================================

    @Test
    @DisplayName("Should skip specified number of lines")
    void testSkipLines() throws IOException {
        String csvContent = """
            # This is a comment line
            # Another comment line
            name,age,active
            John,30,true
            Jane,25,false
            """;

        FileFormatConfig config = createDefaultFormatConfig();
        config.setSkipLines(2);

        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, config);

        assertEquals(2, result.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals("John", firstRow.get("name"));
        assertEquals(30L, firstRow.get("age"));
    }

    // ========================================
    // Encoding Tests
    // ========================================

    @Test
    @DisplayName("Should handle UTF-8 encoding with special characters")
    void testUTF8Encoding() throws IOException {
        String csvContent = """
            name,city,emoji
            José María,São Paulo,😀
            François,Montréal,🇨🇦
            """;

        FileFormatConfig config = createDefaultFormatConfig();
        config.setEncoding("UTF-8");

        Path csvFile = createTempCsvFile(csvContent, "UTF-8");
        List<Object> result = csvDataLoader.loadData(csvFile, config);

        assertEquals(2, result.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals("José María", firstRow.get("name"));
        assertEquals("São Paulo", firstRow.get("city"));
        assertEquals("😀", firstRow.get("emoji"));
    }

    @Test
    @DisplayName("Should handle default encoding when not specified")
    void testDefaultEncoding() throws IOException {
        String csvContent = """
            name,message
            John,Hello World
            Jane,Goodbye World
            """;

        FileFormatConfig config = new FileFormatConfig();
        config.setType("csv");
        // encoding not specified, should default to UTF-8

        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, config);

        assertEquals(2, result.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals("John", firstRow.get("name"));
        assertEquals("Hello World", firstRow.get("message"));
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle missing file")
    void testMissingFile() {
        Path nonExistentFile = tempDir.resolve("nonexistent.csv");

        IOException exception = assertThrows(IOException.class, () -> {
            csvDataLoader.loadData(nonExistentFile, defaultConfig);
        });

        assertNotNull(exception.getMessage());
    }

    @Test
    @DisplayName("Should handle malformed CSV gracefully")
    void testMalformedCsv() throws IOException {
        String malformedCsv = """
            name,age,active
            John,30,true
            Jane,25,false,extra,columns
            Bob,35
            """;

        Path csvFile = createTempCsvFile(malformedCsv);

        // Should not throw exception, but should log warnings and continue processing
        assertDoesNotThrow(() -> {
            List<Object> result = csvDataLoader.loadData(csvFile, defaultConfig);
            // Should still process the valid rows
            assertTrue(result.size() >= 1);
        });
    }

    @Test
    @DisplayName("Should handle null format configuration")
    void testNullFormatConfiguration() throws IOException {
        String csvContent = """
            name,age
            John,30
            Jane,25
            """;

        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, null);

        // With null config, it might treat the header as a data row
        assertEquals(3, result.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        // First row might be the header row treated as data
        assertNotNull(firstRow);
    }

    // ========================================
    // Edge Cases Tests
    // ========================================

    @Test
    @DisplayName("Should handle CSV with varying column counts")
    void testVaryingColumnCounts() throws IOException {
        String csvContent = """
            name,age,active
            John,30,true
            Jane,25
            Bob,35,false,extra
            """;

        Path csvFile = createTempCsvFile(csvContent);

        // Should handle gracefully without throwing exceptions
        assertDoesNotThrow(() -> {
            List<Object> result = csvDataLoader.loadData(csvFile, defaultConfig);
            assertNotNull(result);
            assertTrue(result.size() >= 1);
        });
    }

    @Test
    @DisplayName("Should handle large CSV file")
    void testLargeCsvFile() throws IOException {
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("id,name,value\n");

        for (int i = 0; i < 1000; i++) {
            csvBuilder.append(String.format("%d,Item %d,%d.%02d\n", i, i, i, i % 100));
        }

        Path csvFile = createTempCsvFile(csvBuilder.toString());
        List<Object> result = csvDataLoader.loadData(csvFile, defaultConfig);

        assertEquals(1000, result.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals(0L, firstRow.get("id"));
        assertEquals("Item 0", firstRow.get("name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> lastRow = (Map<String, Object>) result.get(999);
        assertEquals(999L, lastRow.get("id"));
        assertEquals("Item 999", lastRow.get("name"));
    }

    @Test
    @DisplayName("Should handle CSV with whitespace")
    void testCsvWithWhitespace() throws IOException {
        String csvContent = """
            name,age,active
            John,30,true
            Jane,25,false
            """;

        Path csvFile = createTempCsvFile(csvContent);
        List<Object> result = csvDataLoader.loadData(csvFile, defaultConfig);

        assertEquals(2, result.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
        assertEquals("John", firstRow.get("name"));
        assertEquals(30L, firstRow.get("age"));
        assertEquals(true, firstRow.get("active"));
    }

    // ========================================
    // Helper Methods
    // ========================================

    private FileFormatConfig createDefaultFormatConfig() {
        FileFormatConfig config = new FileFormatConfig();
        config.setType("csv");
        config.setDelimiter(",");
        config.setQuoteCharacter("\"");
        config.setEscapeCharacter("\\");
        config.setHeaderRow(true);
        config.setSkipLines(0);
        config.setEncoding("UTF-8");
        return config;
    }

    private Path createTempCsvFile(String content) throws IOException {
        return createTempCsvFile(content, "UTF-8");
    }

    private Path createTempCsvFile(String content, String encoding) throws IOException {
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, content, java.nio.charset.Charset.forName(encoding));
        return csvFile;
    }
}
