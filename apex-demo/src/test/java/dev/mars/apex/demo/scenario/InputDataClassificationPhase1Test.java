/*
 * Copyright (c) 2024 Mark Andrew Ray-Smith Cityline Ltd
 * All rights reserved.
 */
package dev.mars.apex.demo.scenario;

import dev.mars.apex.core.service.classification.*;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InputDataClassificationPhase1Test - Phase 1.2 Implementation Tests
 *
 * PURPOSE:
 * This test class validates the Phase 1.2 implementation of the APEX Input Data
 * Classification System, focusing on content-based classification, enhanced
 * confidence scoring, and basic caching functionality.
 *
 * PHASE 1.2 SCOPE:
 * - Content-based file format detection
 * - Message type and content pattern analysis
 * - Enhanced confidence scoring with multiple factors
 * - Basic classification result caching
 * - Performance improvements and monitoring
 *
 * TESTING APPROACH:
 * - Uses real APEX components for authentic testing
 * - Tests both positive and negative scenarios
 * - Validates caching behavior and performance
 * - Demonstrates enhanced classification accuracy
 * - Follows established APEX demo testing patterns
 *
 * BUSINESS CONTEXT:
 * Phase 1.2 adds intelligent content analysis that can identify financial
 * message types, instrument classifications, and data patterns beyond simple
 * file extensions, enabling more accurate scenario routing.
 *
 * APEX DESIGN PRINCIPLES DEMONSTRATED:
 * 1. Content-based classification with pattern matching
 * 2. Multi-layer confidence scoring
 * 3. Performance optimization through caching
 * 4. Enhanced accuracy through multiple detection strategies
 * 5. Comprehensive monitoring and statistics
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @version 1.0.0
 * @since 2024-12-28
 */
public class InputDataClassificationPhase1Test extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(InputDataClassificationPhase1Test.class);

    private EnhancedDataTypeScenarioService scenarioService;

    @BeforeEach
    public void setUp() {
        // Call parent setup to initialize base APEX services
        super.setUp();

        logger.info("=== Setting up InputDataClassificationPhase1Test ===");

        // Create enhanced scenario service
        scenarioService = new EnhancedDataTypeScenarioService();

        // Load test scenarios
        try {
            String registryPath = "src/test/java/dev/mars/apex/demo/scenario/InputDataClassificationPhase1Test.yaml";
            scenarioService.loadScenarios(registryPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test scenarios", e);
        }

        // Clear cache for clean test state
        scenarioService.clearClassificationCache();

        logger.info("Enhanced scenario service initialized with {} format detectors",
                   scenarioService.getFormatDetectors().size());
    }

    @Test
    @Disabled("Content type classification not yet fully implemented")
    @DisplayName("Should detect JSON content with message type classification")
    void testJsonContentClassification() {
        logger.info("=== Testing JSON content classification ===");

        // Create financial message JSON
        String jsonData = """
            {
                "messageType": "TRADE",
                "tradeId": "T12345",
                "counterparty": "Goldman Sachs",
                "instrument": {
                    "type": "OTC_OPTION",
                    "underlying": "EUR/USD",
                    "expiry": "2024-12-31"
                },
                "notional": 1000000,
                "currency": "USD"
            }
            """;

        // Test classification
        ClassificationResult classification = scenarioService.classifyInputData(
            jsonData,
            "test",
            "trade_message.json",
            (long) jsonData.length(),
            Map.of("region", "US")
        );

        // Validate results
        assertNotNull(classification, "Classification result should not be null");
        assertTrue(classification.isSuccessful(), "Classification should succeed");
        assertEquals("json", classification.getFileFormat(), "Should detect JSON format");

        // Phase 1.2 enhancement: Content type should be classified
        assertNotNull(classification.getContentType(), "Content type should be classified");
        assertNotEquals("unknown", classification.getContentType(), "Content type should not be unknown");

        // Phase 1.2 enhancement: Enhanced confidence scoring
        assertTrue(classification.getConfidence() > 0.6, "Should have good confidence with content analysis");

        logger.info("JSON content classification successful: format={}, contentType={}, confidence={}",
                   classification.getFileFormat(), classification.getContentType(), classification.getConfidence());
    }

    @Test
    @Disabled("Content-based detection not yet fully implemented")
    @DisplayName("Should demonstrate content-based detection vs extension-based")
    void testContentBasedVsExtensionBased() {
        logger.info("=== Testing content-based vs extension-based detection ===");

        // JSON content with misleading extension
        String jsonData = """
            {
                "messageType": "SETTLEMENT",
                "settlementId": "S67890",
                "amount": 500000,
                "currency": "EUR"
            }
            """;

        // Test classification
        ClassificationResult classification = scenarioService.classifyInputData(
            jsonData,
            "test",
            "data.txt", // Misleading extension
            (long) jsonData.length(),
            Map.of()
        );

        // Validate that content-based detection overrides extension
        assertNotNull(classification, "Classification result should not be null");
        assertTrue(classification.isSuccessful(), "Classification should succeed");
        assertEquals("json", classification.getFileFormat(),
                    "Content-based detection should identify JSON despite .txt extension");

        // Should have reasonable confidence from content analysis
        assertTrue(classification.getConfidence() > 0.5,
                  "Content-based detection should provide reasonable confidence");

        logger.info("Content-based detection successful: detected {} despite .txt extension",
                   classification.getFileFormat());
    }

    @Test
    @DisplayName("Should demonstrate classification caching performance")
    void testClassificationCaching() {
        logger.info("=== Testing classification caching performance ===");

        String jsonData = """
            {
                "messageType": "POSITION",
                "positionId": "P11111",
                "portfolio": "HEDGE_FUND_A",
                "holdings": [
                    {"symbol": "AAPL", "quantity": 1000},
                    {"symbol": "GOOGL", "quantity": 500}
                ]
            }
            """;

        // Warm-up a different key to stabilize JIT and thread scheduling without priming this specific cache entry
        try {
            scenarioService.classifyInputData(jsonData, "test", "warmup.json", (long) jsonData.length(), Map.of());
        } catch (Exception ignore) {
            // best-effort warm-up only
        }

        // First call - should be cache miss for this key
        long startTime1 = System.nanoTime();
        ClassificationResult result1 = scenarioService.classifyInputData(
            jsonData, "test", "position.json", (long) jsonData.length(), Map.of()
        );
        long time1 = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime1);

        // Second call - should be cache hit for this key
        long startTime2 = System.nanoTime();
        ClassificationResult result2 = scenarioService.classifyInputData(
            jsonData, "test", "position.json", (long) jsonData.length(), Map.of()
        );
        long time2 = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime2);

        // Validate both results are successful
        assertTrue(result1.isSuccessful(), "First call should succeed");
        assertTrue(result2.isSuccessful(), "Second call should succeed");

        // Results should be equivalent
        assertEquals(result1.getFileFormat(), result2.getFileFormat(),
                    "Cached result should match original");
        assertEquals(result1.getContentType(), result2.getContentType(),
                    "Cached content type should match original");

        // Second call should be faster (cache hit)
        assertTrue(time2 <= time1, "Cached call should be faster or equal");

        // Check cache statistics
        Object cacheStats = scenarioService.getClassificationCacheStatistics();
        assertNotNull(cacheStats, "Cache statistics should be available");

        logger.info("Caching performance: first={}ms, second={}ms, cache stats available: {}",
                   time1, time2, cacheStats != null);
    }

    @Test
    @DisplayName("Should handle XML content classification")
    void testXmlContentClassification() {
        logger.info("=== Testing XML content classification ===");

        String xmlData = """
            <?xml version="1.0" encoding="UTF-8"?>
            <trade>
                <tradeId>T54321</tradeId>
                <counterparty>JP Morgan</counterparty>
                <instrument>
                    <type>COMMODITY_SWAP</type>
                    <commodity>CRUDE_OIL</commodity>
                </instrument>
                <notional>2000000</notional>
            </trade>
            """;

        // Test classification
        ClassificationResult classification = scenarioService.classifyInputData(
            xmlData,
            "test",
            "trade.xml",
            (long) xmlData.length(),
            Map.of()
        );

        // Validate results
        assertNotNull(classification, "Classification result should not be null");
        assertTrue(classification.isSuccessful(), "Classification should succeed");
        assertEquals("xml", classification.getFileFormat(), "Should detect XML format");
        assertNotNull(classification.getContentType(), "Content type should be classified");
        assertTrue(classification.getConfidence() > 0.5, "Should have reasonable confidence");

        logger.info("XML content classification successful: format={}, contentType={}, confidence={}",
                   classification.getFileFormat(), classification.getContentType(), classification.getConfidence());
    }

    @Test
    @DisplayName("Should handle CSV content classification")
    void testCsvContentClassification() {
        logger.info("=== Testing CSV content classification ===");

        String csvData = """
            tradeId,counterparty,amount,currency,timestamp
            T98765,Deutsche Bank,750000,EUR,2024-12-28T10:30:00Z
            T98766,Credit Suisse,1200000,USD,2024-12-28T10:31:00Z
            T98767,UBS,900000,GBP,2024-12-28T10:32:00Z
            """;

        // Test classification
        ClassificationResult classification = scenarioService.classifyInputData(
            csvData,
            "test",
            "trades.csv",
            (long) csvData.length(),
            Map.of()
        );

        // Validate results
        assertNotNull(classification, "Classification result should not be null");
        assertTrue(classification.isSuccessful(), "Classification should succeed");
        assertEquals("csv", classification.getFileFormat(), "Should detect CSV format");
        assertNotNull(classification.getContentType(), "Content type should be classified");
        assertTrue(classification.getConfidence() > 0.5, "Should have reasonable confidence");

        logger.info("CSV content classification successful: format={}, contentType={}, confidence={}",
                   classification.getFileFormat(), classification.getContentType(), classification.getConfidence());
    }

    @Test
    @Disabled("Enhanced confidence scoring not yet fully implemented")
    @DisplayName("Should validate enhanced confidence scoring")
    void testEnhancedConfidenceScoring() {
        logger.info("=== Testing enhanced confidence scoring ===");

        // High-confidence case: JSON with clear message type
        String highConfidenceData = """
            {
                "messageType": "TRADE",
                "tradeId": "T12345",
                "instrument": {"type": "OTC_OPTION"}
            }
            """;

        // Lower-confidence case: Generic JSON without clear patterns
        String lowerConfidenceData = """
            {
                "data": "some value",
                "number": 123,
                "flag": true
            }
            """;

        ClassificationResult result1 = scenarioService.classifyInputData(
            highConfidenceData, "test", "trade.json", (long) highConfidenceData.length(), Map.of()
        );
        ClassificationResult result2 = scenarioService.classifyInputData(
            lowerConfidenceData, "test", "generic.json", (long) lowerConfidenceData.length(), Map.of()
        );

        assertTrue(result1.isSuccessful() && result2.isSuccessful(), "Both classifications should succeed");

        double confidence1 = result1.getConfidence();
        double confidence2 = result2.getConfidence();

        // High-confidence case should have higher confidence
        assertTrue(confidence1 > confidence2,
                  "Trade message should have higher confidence than generic JSON");

        logger.info("Enhanced confidence scoring: trade={}, generic={}", confidence1, confidence2);
    }

    @Test
    @DisplayName("Should validate cache management operations")
    void testCacheManagement() {
        logger.info("=== Testing cache management operations ===");

        String testData = "{\"test\": \"data\"}";

        // Perform classification to populate cache
        ClassificationResult result1 = scenarioService.classifyInputData(
            testData, "test", "test.json", (long) testData.length(), Map.of()
        );
        assertTrue(result1.isSuccessful(), "Initial classification should succeed");

        // Verify cache has content
        assertTrue(scenarioService.getClassificationCache().size() > 0, "Cache should have entries");

        // Clear cache
        scenarioService.clearClassificationCache();
        assertEquals(0, scenarioService.getClassificationCache().size(), "Cache should be empty after clear");

        // Verify classification still works after cache clear
        ClassificationResult result2 = scenarioService.classifyInputData(
            testData, "test", "test.json", (long) testData.length(), Map.of()
        );
        assertTrue(result2.isSuccessful(), "Classification should work after cache clear");

        logger.info("Cache management validation successful");
    }
}
