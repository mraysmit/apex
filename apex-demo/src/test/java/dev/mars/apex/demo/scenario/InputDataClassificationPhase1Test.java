/*
 * Copyright (c) 2024 Mark Andrew Ray-Smith Cityline Ltd
 * All rights reserved.
 */
package dev.mars.apex.demo.scenario;

import dev.mars.apex.core.engine.ApexEngine;
import dev.mars.apex.core.service.classification.*;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private ApexEngine apexEngine;
    private EnhancedDataTypeScenarioService scenarioService;

    @BeforeEach
    public void setUp() {
        // Call parent setup to initialize base APEX services
        super.setUp();

        logger.info("=== Setting up InputDataClassificationPhase1Test ===");

        // Create enhanced scenario service
        scenarioService = new EnhancedDataTypeScenarioService();
        
        // Create APEX engine with enhanced service
        apexEngine = new ApexEngine(scenarioService);
        
        // Load test scenarios
        String registryPath = "src/test/java/dev/mars/apex/demo/scenario/InputDataClassificationPhase1Test.yaml";
        apexEngine.loadScenarios(registryPath);
        
        // Clear cache for clean test state
        scenarioService.clearClassificationCache();
        
        logger.info("APEX Engine initialized with {} format detectors", 
                   scenarioService.getFormatDetectors().size());
    }

    @Test
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
        
        ApexProcessingContext context = ApexProcessingContext.builder()
            .source("test")
            .fileName("trade_message.json")
            .fileSize((long) jsonData.length())
            .addMetadata("region", "US")
            .build();
        
        // Test classification
        ApexProcessingResult result = apexEngine.classifyAndProcessData(jsonData, context);
        
        // Validate results
        assertNotNull(result, "Processing result should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");
        
        ClassificationResult classification = result.getClassification();
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
        
        ApexProcessingContext context = ApexProcessingContext.builder()
            .source("test")
            .fileName("data.txt") // Misleading extension
            .fileSize((long) jsonData.length())
            .build();
        
        // Test classification
        ApexProcessingResult result = apexEngine.classifyAndProcessData(jsonData, context);
        
        // Validate that content-based detection overrides extension
        assertNotNull(result, "Processing result should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");
        
        ClassificationResult classification = result.getClassification();
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
        
        ApexProcessingContext context = ApexProcessingContext.builder()
            .source("test")
            .fileName("position.json")
            .fileSize((long) jsonData.length())
            .build();
        
        // First call - should be cache miss
        long startTime1 = System.currentTimeMillis();
        ApexProcessingResult result1 = apexEngine.classifyAndProcessData(jsonData, context);
        long time1 = System.currentTimeMillis() - startTime1;
        
        // Second call - should be cache hit
        long startTime2 = System.currentTimeMillis();
        ApexProcessingResult result2 = apexEngine.classifyAndProcessData(jsonData, context);
        long time2 = System.currentTimeMillis() - startTime2;
        
        // Validate both results are successful
        assertTrue(result1.isSuccess(), "First call should succeed");
        assertTrue(result2.isSuccess(), "Second call should succeed");
        
        // Results should be equivalent
        assertEquals(result1.getClassification().getFileFormat(), 
                    result2.getClassification().getFileFormat(), 
                    "Cached result should match original");
        assertEquals(result1.getClassification().getContentType(), 
                    result2.getClassification().getContentType(), 
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
        
        ApexProcessingContext context = ApexProcessingContext.builder()
            .source("test")
            .fileName("trade.xml")
            .fileSize((long) xmlData.length())
            .build();
        
        // Test classification
        ApexProcessingResult result = apexEngine.classifyAndProcessData(xmlData, context);
        
        // Validate results
        assertNotNull(result, "Processing result should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");
        
        ClassificationResult classification = result.getClassification();
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
        
        ApexProcessingContext context = ApexProcessingContext.builder()
            .source("test")
            .fileName("trades.csv")
            .fileSize((long) csvData.length())
            .build();
        
        // Test classification
        ApexProcessingResult result = apexEngine.classifyAndProcessData(csvData, context);
        
        // Validate results
        assertNotNull(result, "Processing result should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");
        
        ClassificationResult classification = result.getClassification();
        assertEquals("csv", classification.getFileFormat(), "Should detect CSV format");
        assertNotNull(classification.getContentType(), "Content type should be classified");
        assertTrue(classification.getConfidence() > 0.5, "Should have reasonable confidence");
        
        logger.info("CSV content classification successful: format={}, contentType={}, confidence={}", 
                   classification.getFileFormat(), classification.getContentType(), classification.getConfidence());
    }

    @Test
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
        
        ApexProcessingContext context1 = ApexProcessingContext.builder()
            .fileName("trade.json").build();
        ApexProcessingContext context2 = ApexProcessingContext.builder()
            .fileName("generic.json").build();
        
        ApexProcessingResult result1 = apexEngine.classifyAndProcessData(highConfidenceData, context1);
        ApexProcessingResult result2 = apexEngine.classifyAndProcessData(lowerConfidenceData, context2);
        
        assertTrue(result1.isSuccess() && result2.isSuccess(), "Both classifications should succeed");
        
        double confidence1 = result1.getClassification().getConfidence();
        double confidence2 = result2.getClassification().getConfidence();
        
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
        ApexProcessingContext context = ApexProcessingContext.builder()
            .fileName("test.json").build();
        
        // Perform classification to populate cache
        ApexProcessingResult result1 = apexEngine.classifyAndProcessData(testData, context);
        assertTrue(result1.isSuccess(), "Initial classification should succeed");
        
        // Verify cache has content
        assertTrue(scenarioService.getClassificationCache().size() > 0, "Cache should have entries");
        
        // Clear cache
        scenarioService.clearClassificationCache();
        assertEquals(0, scenarioService.getClassificationCache().size(), "Cache should be empty after clear");
        
        // Verify classification still works after cache clear
        ApexProcessingResult result2 = apexEngine.classifyAndProcessData(testData, context);
        assertTrue(result2.isSuccess(), "Classification should work after cache clear");
        
        logger.info("Cache management validation successful");
    }
}
