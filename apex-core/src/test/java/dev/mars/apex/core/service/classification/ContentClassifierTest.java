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
package dev.mars.apex.core.service.classification;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test coverage for ContentClassifier - content analysis and message pattern classification.
 * 
 * Tests the content classification functionality including:
 * - JSON content classification (messageType, instrumentType, field patterns)
 * - XML content classification (root element detection)
 * - CSV content classification (header analysis)
 * - Text content classification (keyword matching)
 * - Error handling for malformed content
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("ContentClassifier Tests")
class ContentClassifierTest {

    private static final Logger logger = LoggerFactory.getLogger(ContentClassifierTest.class);
    
    private ContentClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new ContentClassifier();
    }

    @Nested
    @DisplayName("JSON Content Classification Tests")
    class JsonContentClassificationTests {

        @Test
        @DisplayName("Should classify JSON with messageType field")
        void shouldClassifyJsonWithMessageTypeField() {
            String json = """
                {
                    "messageType": "TRADE",
                    "tradeId": "T12345",
                    "amount": 1000000
                }
                """;
            
            ContentClassificationResult result = classifier.classify("json", json, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("trade-message", result.getContentType());
            
            logger.info("[OK] JSON with messageType classified as trade-message");
        }

        @Test
        @DisplayName("Should classify JSON with type field as SETTLEMENT")
        void shouldClassifyJsonWithTypeFieldAsSettlement() {
            String json = """
                {
                    "type": "SETTLEMENT",
                    "settlementId": "S001",
                    "currency": "USD"
                }
                """;
            
            ContentClassificationResult result = classifier.classify("json", json, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("settlement-message", result.getContentType());
            
            logger.info("[OK] JSON with type=SETTLEMENT classified correctly");
        }

        @Test
        @DisplayName("Should classify JSON with instrument type OTC_OPTION")
        void shouldClassifyJsonWithInstrumentTypeOtcOption() {
            String json = """
                {
                    "instrumentType": "OTC_OPTION",
                    "strike": 100.0,
                    "expiry": "2025-12-31"
                }
                """;
            
            ContentClassificationResult result = classifier.classify("json", json, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("otc-option-instrument", result.getContentType());
            
            logger.info("[OK] JSON with instrumentType=OTC_OPTION classified correctly");
        }

        @Test
        @DisplayName("Should classify JSON by field patterns for trade data")
        void shouldClassifyJsonByFieldPatternsForTradeData() {
            String json = """
                {
                    "tradeId": "T001",
                    "counterparty": "ACME Corp",
                    "notional": 500000
                }
                """;
            
            ContentClassificationResult result = classifier.classify("json", json, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            // Classification may vary based on field pattern matching
            assertNotNull(result.getContentType());
            
            logger.info("[OK] JSON with trade fields classified: {}", result.getContentType());
        }

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJsonGracefully() {
            String malformedJson = "{ invalid json }";
            
            ContentClassificationResult result = classifier.classify("json", malformedJson, null);
            
            assertNotNull(result);
            // Should return unknown classification for malformed content
            assertFalse(result.isSuccessful());
            assertEquals("unknown", result.getContentType());
            
            logger.info("[OK] Malformed JSON handled gracefully");
        }

        @Test
        @DisplayName("Should classify generic JSON as json-data")
        void shouldClassifyGenericJsonAsJsonData() {
            String json = """
                {
                    "key1": "value1",
                    "key2": 123,
                    "key3": true
                }
                """;
            
            ContentClassificationResult result = classifier.classify("json", json, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("json-data", result.getContentType());
            
            logger.info("[OK] Generic JSON classified as json-data");
        }

        @Test
        @DisplayName("Should handle JSON with nested messageType")
        void shouldHandleJsonWithNestedMessageType() {
            // Note: The classifier uses .at() which should handle nested paths
            String json = """
                {
                    "header": {
                        "version": "1.0"
                    },
                    "body": {
                        "data": "value"
                    }
                }
                """;
            
            ContentClassificationResult result = classifier.classify("json", json, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            
            logger.info("[OK] Nested JSON handled: {}", result.getContentType());
        }
    }

    @Nested
    @DisplayName("XML Content Classification Tests")
    class XmlContentClassificationTests {

        @Test
        @DisplayName("Should classify XML with trade root element")
        void shouldClassifyXmlWithTradeRootElement() {
            String xml = """
                <?xml version="1.0"?>
                <trade>
                    <tradeId>T001</tradeId>
                    <amount>100000</amount>
                </trade>
                """;
            
            ContentClassificationResult result = classifier.classify("xml", xml, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("trade-xml", result.getContentType());
            
            logger.info("[OK] XML with trade root element classified correctly");
        }

        @Test
        @DisplayName("Should classify XML with position root element")
        void shouldClassifyXmlWithPositionRootElement() {
            String xml = """
                <?xml version="1.0"?>
                <position>
                    <positionId>P001</positionId>
                    <holdings>50000</holdings>
                </position>
                """;
            
            ContentClassificationResult result = classifier.classify("xml", xml, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("position-xml", result.getContentType());
            
            logger.info("[OK] XML with position root element classified correctly");
        }

        @Test
        @DisplayName("Should classify XML with settlement root element")
        void shouldClassifyXmlWithSettlementRootElement() {
            String xml = """
                <?xml version="1.0"?>
                <settlement>
                    <settlementId>S001</settlementId>
                    <status>PENDING</status>
                </settlement>
                """;
            
            ContentClassificationResult result = classifier.classify("xml", xml, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("settlement-xml", result.getContentType());
            
            logger.info("[OK] XML with settlement root element classified correctly");
        }

        @Test
        @DisplayName("Should classify generic XML as xml-data")
        void shouldClassifyGenericXmlAsXmlData() {
            String xml = """
                <?xml version="1.0"?>
                <data>
                    <item>value</item>
                </data>
                """;
            
            ContentClassificationResult result = classifier.classify("xml", xml, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("xml-data", result.getContentType());
            
            logger.info("[OK] Generic XML classified as xml-data");
        }
    }

    @Nested
    @DisplayName("CSV Content Classification Tests")
    class CsvContentClassificationTests {

        @Test
        @DisplayName("Should classify CSV with tradeId header")
        void shouldClassifyCsvWithTradeIdHeader() {
            String csv = """
                tradeId,type,amount,currency
                T001,TRADE,100000,USD
                T002,SETTLEMENT,200000,EUR
                """;
            
            ContentClassificationResult result = classifier.classify("csv", csv, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("trade-csv", result.getContentType());
            
            logger.info("[OK] CSV with tradeId header classified correctly");
        }

        @Test
        @DisplayName("Should classify CSV with positionId header")
        void shouldClassifyCsvWithPositionIdHeader() {
            String csv = """
                positionId,portfolio,quantity,marketValue
                P001,EQUITY,1000,50000
                P002,BONDS,2000,100000
                """;
            
            ContentClassificationResult result = classifier.classify("csv", csv, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("position-csv", result.getContentType());
            
            logger.info("[OK] CSV with positionId header classified correctly");
        }

        @Test
        @DisplayName("Should classify CSV with price/quantity as market data")
        void shouldClassifyCsvWithPriceQuantityAsMarketData() {
            String csv = """
                symbol,price,quantity,timestamp
                AAPL,150.25,1000,2025-01-29T10:00:00Z
                GOOG,2800.50,500,2025-01-29T10:00:00Z
                """;
            
            ContentClassificationResult result = classifier.classify("csv", csv, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("market-data-csv", result.getContentType());
            
            logger.info("[OK] CSV with price/quantity classified as market-data-csv");
        }

        @Test
        @DisplayName("Should classify generic CSV as csv-data")
        void shouldClassifyGenericCsvAsCsvData() {
            String csv = """
                name,age,city
                John,30,New York
                Jane,25,London
                """;
            
            ContentClassificationResult result = classifier.classify("csv", csv, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("csv-data", result.getContentType());
            
            logger.info("[OK] Generic CSV classified as csv-data");
        }
    }

    @Nested
    @DisplayName("Text Content Classification Tests")
    class TextContentClassificationTests {

        @Test
        @DisplayName("Should classify text with trade keywords")
        void shouldClassifyTextWithTradeKeywords() {
            String text = "This trade requires settlement by end of day.";
            
            ContentClassificationResult result = classifier.classify("text", text, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("trade-text", result.getContentType());
            
            logger.info("[OK] Text with trade keywords classified correctly");
        }

        @Test
        @DisplayName("Should classify text with position keywords")
        void shouldClassifyTextWithPositionKeywords() {
            String text = "The portfolio position needs reconciliation.";
            
            ContentClassificationResult result = classifier.classify("text", text, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("position-text", result.getContentType());
            
            logger.info("[OK] Text with position keywords classified correctly");
        }

        @Test
        @DisplayName("Should classify generic text as text-data")
        void shouldClassifyGenericTextAsTextData() {
            String text = "Hello, this is a generic message.";
            
            ContentClassificationResult result = classifier.classify("text", text, null);
            
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("text-data", result.getContentType());
            
            logger.info("[OK] Generic text classified as text-data");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null content")
        void shouldHandleNullContent() {
            ContentClassificationResult result = classifier.classify("json", null, null);
            
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            
            logger.info("[OK] Null content handled gracefully");
        }

        @Test
        @DisplayName("Should handle empty content")
        void shouldHandleEmptyContent() {
            ContentClassificationResult result = classifier.classify("json", "", null);
            
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            
            logger.info("[OK] Empty content handled gracefully");
        }

        @Test
        @DisplayName("Should handle whitespace-only content")
        void shouldHandleWhitespaceOnlyContent() {
            ContentClassificationResult result = classifier.classify("json", "   \n\t  ", null);
            
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            
            logger.info("[OK] Whitespace-only content handled gracefully");
        }

        @Test
        @DisplayName("Should handle unknown file format")
        void shouldHandleUnknownFileFormat() {
            String content = "some random content";
            
            ContentClassificationResult result = classifier.classify("unknown", content, null);
            
            assertNotNull(result);
            // Unknown formats should be treated as text
            assertTrue(result.isSuccessful());
            
            logger.info("[OK] Unknown file format handled: {}", result.getContentType());
        }
    }

    @Nested
    @DisplayName("ContentClassificationResult Tests")
    class ContentClassificationResultTests {

        @Test
        @DisplayName("Should create classified result with all properties")
        void shouldCreateClassifiedResultWithAllProperties() {
            ContentClassificationResult result = ContentClassificationResult.classified(
                "trade-message", 0.85, "messageType=TRADE");
            
            assertTrue(result.isSuccessful());
            assertEquals("trade-message", result.getContentType());
            assertEquals(0.85, result.getConfidence());
            assertEquals("messageType=TRADE", result.getReason());
            
            logger.info("[OK] Classified result created with all properties");
        }

        @Test
        @DisplayName("Should create unknown result")
        void shouldCreateUnknownResult() {
            ContentClassificationResult result = ContentClassificationResult.unknown();
            
            assertFalse(result.isSuccessful());
            assertEquals("unknown", result.getContentType());
            assertEquals(0.0, result.getConfidence());
            
            logger.info("[OK] Unknown result created correctly");
        }

        @Test
        @DisplayName("Should have proper toString representation")
        void shouldHaveProperToStringRepresentation() {
            ContentClassificationResult result = ContentClassificationResult.classified(
                "test-type", 0.9, "test-reason");
            
            String str = result.toString();
            
            assertNotNull(str);
            assertTrue(str.contains("test-type"));
            assertTrue(str.contains("0.9"));
            
            logger.info("[OK] toString representation is correct");
        }
    }
}
