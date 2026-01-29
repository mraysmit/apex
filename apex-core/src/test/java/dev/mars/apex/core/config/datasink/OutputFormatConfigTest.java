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
package dev.mars.apex.core.config.datasink;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test coverage for OutputFormatConfig - data sink output format configuration.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("OutputFormatConfig Tests")
class OutputFormatConfigTest {

    private static final Logger logger = LoggerFactory.getLogger(OutputFormatConfigTest.class);

    private OutputFormatConfig config;

    @BeforeEach
    void setUp() {
        config = new OutputFormatConfig();
    }

    @Nested
    @DisplayName("OutputFormat Enum Tests")
    class OutputFormatEnumTests {

        @Test
        @DisplayName("Should have all expected output formats")
        void shouldHaveAllExpectedOutputFormats() {
            OutputFormatConfig.OutputFormat[] formats = OutputFormatConfig.OutputFormat.values();
            
            assertEquals(7, formats.length);
            assertNotNull(OutputFormatConfig.OutputFormat.JSON);
            assertNotNull(OutputFormatConfig.OutputFormat.XML);
            assertNotNull(OutputFormatConfig.OutputFormat.CSV);
            assertNotNull(OutputFormatConfig.OutputFormat.SQL);
            assertNotNull(OutputFormatConfig.OutputFormat.AVRO);
            assertNotNull(OutputFormatConfig.OutputFormat.PARQUET);
            assertNotNull(OutputFormatConfig.OutputFormat.CUSTOM);
            
            logger.info("[OK] All expected output formats present");
        }

        @Test
        @DisplayName("Should have correct codes for output formats")
        void shouldHaveCorrectCodesForOutputFormats() {
            assertEquals("json", OutputFormatConfig.OutputFormat.JSON.getCode());
            assertEquals("xml", OutputFormatConfig.OutputFormat.XML.getCode());
            assertEquals("csv", OutputFormatConfig.OutputFormat.CSV.getCode());
            assertEquals("sql", OutputFormatConfig.OutputFormat.SQL.getCode());
            assertEquals("avro", OutputFormatConfig.OutputFormat.AVRO.getCode());
            assertEquals("parquet", OutputFormatConfig.OutputFormat.PARQUET.getCode());
            assertEquals("custom", OutputFormatConfig.OutputFormat.CUSTOM.getCode());
            
            logger.info("[OK] All output format codes verified");
        }

        @Test
        @DisplayName("Should have descriptions for output formats")
        void shouldHaveDescriptionsForOutputFormats() {
            for (OutputFormatConfig.OutputFormat format : OutputFormatConfig.OutputFormat.values()) {
                assertNotNull(format.getDescription());
                assertFalse(format.getDescription().isEmpty());
            }
            
            logger.info("[OK] All output formats have descriptions");
        }

        @Test
        @DisplayName("Should convert code to format (case insensitive)")
        void shouldConvertCodeToFormatCaseInsensitive() {
            assertEquals(OutputFormatConfig.OutputFormat.JSON, 
                        OutputFormatConfig.OutputFormat.fromCode("json"));
            assertEquals(OutputFormatConfig.OutputFormat.JSON, 
                        OutputFormatConfig.OutputFormat.fromCode("JSON"));
            assertEquals(OutputFormatConfig.OutputFormat.CSV, 
                        OutputFormatConfig.OutputFormat.fromCode("Csv"));
            
            logger.info("[OK] Code to format conversion case insensitive");
        }

        @Test
        @DisplayName("Should return JSON as default for null code")
        void shouldReturnJsonAsDefaultForNullCode() {
            assertEquals(OutputFormatConfig.OutputFormat.JSON, 
                        OutputFormatConfig.OutputFormat.fromCode(null));
            
            logger.info("[OK] JSON returned as default for null code");
        }

        @Test
        @DisplayName("Should return JSON for unknown code")
        void shouldReturnJsonForUnknownCode() {
            assertEquals(OutputFormatConfig.OutputFormat.JSON, 
                        OutputFormatConfig.OutputFormat.fromCode("unknown"));
            
            logger.info("[OK] JSON returned for unknown code");
        }
    }

    @Nested
    @DisplayName("Constructor and Default Tests")
    class ConstructorAndDefaultTests {

        @Test
        @DisplayName("Should have correct default values")
        void shouldHaveCorrectDefaultValues() {
            OutputFormatConfig config = new OutputFormatConfig();
            
            assertEquals("json", config.getFormat());
            assertEquals("UTF-8", config.getEncoding());
            assertEquals(false, config.getPrettyPrint());
            assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSSZ", config.getDateFormat());
            assertEquals("true/false", config.getBooleanFormat());
            assertEquals(",", config.getDelimiter());
            assertEquals("\"", config.getQuoteCharacter());
            assertEquals("\\", config.getEscapeCharacter());
            assertEquals(true, config.getIncludeHeader());
            assertEquals("\n", config.getLineEnding());
            
            logger.info("[OK] All default values verified");
        }

        @Test
        @DisplayName("Should initialize empty collections")
        void shouldInitializeEmptyCollections() {
            OutputFormatConfig config = new OutputFormatConfig();
            
            assertNotNull(config.getFieldMappings());
            assertTrue(config.getFieldMappings().isEmpty());
            
            assertNotNull(config.getFieldTypes());
            assertTrue(config.getFieldTypes().isEmpty());
            
            assertNotNull(config.getFieldFormats());
            assertTrue(config.getFieldFormats().isEmpty());
            
            assertNotNull(config.getDefaultValues());
            assertTrue(config.getDefaultValues().isEmpty());
            
            logger.info("[OK] Empty collections initialized");
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("Should set and get format")
        void shouldSetAndGetFormat() {
            config.setFormat("xml");
            assertEquals("xml", config.getFormat());
            assertEquals(OutputFormatConfig.OutputFormat.XML, config.getOutputFormat());
            
            logger.info("[OK] Format getter/setter works");
        }

        @Test
        @DisplayName("Should set and get encoding")
        void shouldSetAndGetEncoding() {
            config.setEncoding("ISO-8859-1");
            assertEquals("ISO-8859-1", config.getEncoding());
            
            logger.info("[OK] Encoding getter/setter works");
        }

        @Test
        @DisplayName("Should set and get prettyPrint")
        void shouldSetAndGetPrettyPrint() {
            config.setPrettyPrint(true);
            assertTrue(config.getPrettyPrint());
            
            logger.info("[OK] PrettyPrint getter/setter works");
        }

        @Test
        @DisplayName("Should set and get field mappings")
        void shouldSetAndGetFieldMappings() {
            Map<String, String> mappings = new HashMap<>();
            mappings.put("srcField", "targetField");
            
            config.setFieldMappings(mappings);
            
            assertEquals("targetField", config.getFieldMappings().get("srcField"));
            
            logger.info("[OK] Field mappings getter/setter works");
        }

        @Test
        @DisplayName("Should handle null field mappings")
        void shouldHandleNullFieldMappings() {
            config.setFieldMappings(null);
            
            assertNotNull(config.getFieldMappings());
            assertTrue(config.getFieldMappings().isEmpty());
            
            logger.info("[OK] Null field mappings handled");
        }

        @Test
        @DisplayName("Should set and get field types")
        void shouldSetAndGetFieldTypes() {
            Map<String, String> types = new HashMap<>();
            types.put("amount", "number");
            
            config.setFieldTypes(types);
            
            assertEquals("number", config.getFieldTypes().get("amount"));
            
            logger.info("[OK] Field types getter/setter works");
        }

        @Test
        @DisplayName("Should set and get field formats")
        void shouldSetAndGetFieldFormats() {
            Map<String, String> formats = new HashMap<>();
            formats.put("date", "yyyy-MM-dd");
            
            config.setFieldFormats(formats);
            
            assertEquals("yyyy-MM-dd", config.getFieldFormats().get("date"));
            
            logger.info("[OK] Field formats getter/setter works");
        }

        @Test
        @DisplayName("Should set and get default values")
        void shouldSetAndGetDefaultValues() {
            Map<String, Object> defaults = new HashMap<>();
            defaults.put("status", "PENDING");
            defaults.put("priority", 0);
            
            config.setDefaultValues(defaults);
            
            assertEquals("PENDING", config.getDefaultValues().get("status"));
            assertEquals(0, config.getDefaultValues().get("priority"));
            
            logger.info("[OK] Default values getter/setter works");
        }

        @Test
        @DisplayName("Should set and get date format")
        void shouldSetAndGetDateFormat() {
            config.setDateFormat("dd/MM/yyyy");
            assertEquals("dd/MM/yyyy", config.getDateFormat());
            
            logger.info("[OK] Date format getter/setter works");
        }

        @Test
        @DisplayName("Should set and get number format")
        void shouldSetAndGetNumberFormat() {
            config.setNumberFormat("#,##0.00");
            assertEquals("#,##0.00", config.getNumberFormat());
            
            logger.info("[OK] Number format getter/setter works");
        }

        @Test
        @DisplayName("Should set and get boolean format")
        void shouldSetAndGetBooleanFormat() {
            config.setBooleanFormat("1/0");
            assertEquals("1/0", config.getBooleanFormat());
            
            logger.info("[OK] Boolean format getter/setter works");
        }
    }

    @Nested
    @DisplayName("CSV-Specific Tests")
    class CsvSpecificTests {

        @Test
        @DisplayName("Should set and get delimiter")
        void shouldSetAndGetDelimiter() {
            config.setDelimiter(";");
            assertEquals(";", config.getDelimiter());
            
            logger.info("[OK] Delimiter getter/setter works");
        }

        @Test
        @DisplayName("Should set and get quote character")
        void shouldSetAndGetQuoteCharacter() {
            config.setQuoteCharacter("'");
            assertEquals("'", config.getQuoteCharacter());
            
            logger.info("[OK] Quote character getter/setter works");
        }

        @Test
        @DisplayName("Should set and get escape character")
        void shouldSetAndGetEscapeCharacter() {
            config.setEscapeCharacter("\\\\");
            assertEquals("\\\\", config.getEscapeCharacter());
            
            logger.info("[OK] Escape character getter/setter works");
        }

        @Test
        @DisplayName("Should set and get include header")
        void shouldSetAndGetIncludeHeader() {
            config.setIncludeHeader(false);
            assertFalse(config.getIncludeHeader());
            
            logger.info("[OK] Include header getter/setter works");
        }

        @Test
        @DisplayName("Should set and get line ending")
        void shouldSetAndGetLineEnding() {
            config.setLineEnding("\r\n");
            assertEquals("\r\n", config.getLineEnding());
            
            logger.info("[OK] Line ending getter/setter works");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with default config")
        void shouldPassValidationWithDefaultConfig() {
            assertDoesNotThrow(() -> config.validate());
            
            logger.info("[OK] Default config passes validation");
        }

        @Test
        @DisplayName("Should fail validation with null format")
        void shouldFailValidationWithNullFormat() {
            config.setFormat(null);
            
            assertThrows(IllegalArgumentException.class, () -> config.validate());
            
            logger.info("[OK] Null format fails validation");
        }

        @Test
        @DisplayName("Should fail validation with empty format")
        void shouldFailValidationWithEmptyFormat() {
            config.setFormat("");
            
            assertThrows(IllegalArgumentException.class, () -> config.validate());
            
            logger.info("[OK] Empty format fails validation");
        }

        @Test
        @DisplayName("Should fail validation with null encoding")
        void shouldFailValidationWithNullEncoding() {
            config.setEncoding(null);
            
            assertThrows(IllegalArgumentException.class, () -> config.validate());
            
            logger.info("[OK] Null encoding fails validation");
        }

        @Test
        @DisplayName("Should fail CSV validation with null delimiter")
        void shouldFailCsvValidationWithNullDelimiter() {
            config.setFormat("csv");
            config.setDelimiter(null);
            
            assertThrows(IllegalArgumentException.class, () -> config.validate());
            
            logger.info("[OK] CSV with null delimiter fails validation");
        }
    }

    @Nested
    @DisplayName("Copy Tests")
    class CopyTests {

        @Test
        @DisplayName("Should create deep copy")
        void shouldCreateDeepCopy() {
            // Set up original
            config.setFormat("xml");
            config.setEncoding("ISO-8859-1");
            config.setPrettyPrint(true);
            config.setDateFormat("dd/MM/yyyy");
            
            Map<String, String> mappings = new HashMap<>();
            mappings.put("src", "target");
            config.setFieldMappings(mappings);
            
            // Create copy
            OutputFormatConfig copy = config.copy();
            
            // Verify copy has same values
            assertEquals("xml", copy.getFormat());
            assertEquals("ISO-8859-1", copy.getEncoding());
            assertTrue(copy.getPrettyPrint());
            assertEquals("dd/MM/yyyy", copy.getDateFormat());
            assertEquals("target", copy.getFieldMappings().get("src"));
            
            // Verify modifications to original don't affect copy
            config.setFormat("json");
            assertEquals("xml", copy.getFormat());
            
            config.getFieldMappings().put("src", "modified");
            assertEquals("target", copy.getFieldMappings().get("src"));
            
            logger.info("[OK] Deep copy created correctly");
        }
    }
}
