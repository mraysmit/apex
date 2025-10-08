/*
 * Copyright (c) 2024 Mark Andrew Ray-Smith Cityline Ltd
 * All rights reserved.
 */
package dev.mars.apex.demo.scenario;

import dev.mars.apex.core.service.classification.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase1ClassificationUnitTest - Unit Tests for Phase 1.1 Classification Components
 *
 * PURPOSE:
 * This test class provides focused unit tests for the individual components
 * of the Phase 1.1 classification system without requiring full APEX integration.
 * These tests validate the core logic and can run independently.
 *
 * TESTING SCOPE:
 * - ApexProcessingContext creation and usage
 * - ClassificationResult construction and validation
 * - ApexProcessingResult handling
 * - FileFormatDetector interface and implementations
 * - ExtensionBasedFileFormatDetector logic
 *
 * DESIGN PRINCIPLES:
 * - Fast, focused unit tests
 * - No external dependencies
 * - Clear validation of component behavior
 * - Comprehensive edge case coverage
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @version 1.0.0
 * @since 2024-12-28
 */
public class Phase1ClassificationUnitTest {

    private static final Logger logger = LoggerFactory.getLogger(Phase1ClassificationUnitTest.class);

    @Test
    @DisplayName("Should create ApexProcessingContext with builder pattern")
    void testApexProcessingContextBuilder() {
        logger.info("=== Testing ApexProcessingContext builder pattern ===");
        
        // Test builder pattern
        ApexProcessingContext context = ApexProcessingContext.builder()
            .source("test-source")
            .fileName("test.json")
            .fileSize(1024L)
            .addMetadata("region", "US")
            .addMetadata("priority", "HIGH")
            .correlationId("test-123")
            .build();
        
        // Validate properties
        assertNotNull(context, "Context should not be null");
        assertEquals("test-source", context.getSource(), "Source should match");
        assertEquals("test.json", context.getFileName(), "File name should match");
        assertEquals(1024L, context.getFileSize(), "File size should match");
        assertEquals("test-123", context.getCorrelationId(), "Correlation ID should match");
        
        // Validate metadata
        Map<String, Object> metadata = context.getMetadata();
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals("US", metadata.get("region"), "Region metadata should match");
        assertEquals("HIGH", metadata.get("priority"), "Priority metadata should match");
        
        // Validate immutability
        Map<String, Object> originalMetadata = context.getMetadata();
        originalMetadata.put("test", "value");
        assertFalse(context.getMetadata().containsKey("test"), "Metadata should be immutable");
        
        logger.info("ApexProcessingContext validation successful: {}", context);
    }

    @Test
    @DisplayName("Should create default ApexProcessingContext")
    void testDefaultApexProcessingContext() {
        logger.info("=== Testing default ApexProcessingContext ===");
        
        ApexProcessingContext context = ApexProcessingContext.defaultContext();
        
        assertNotNull(context, "Default context should not be null");
        assertEquals("unknown", context.getSource(), "Default source should be 'unknown'");
        assertNotNull(context.getMetadata(), "Metadata should not be null");
        assertTrue(context.getMetadata().isEmpty(), "Default metadata should be empty");
        assertTrue(context.getStartTime() > 0, "Start time should be set");
        
        logger.info("Default context validation successful: {}", context);
    }

    @Test
    @DisplayName("Should create successful ClassificationResult")
    void testSuccessfulClassificationResult() {
        logger.info("=== Testing successful ClassificationResult ===");
        
        ClassificationResult result = ClassificationResult.successful(
            "json", "trade-message", "otc-option", "test-scenario", null, "test-data"
        );
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccessful(), "Result should be successful");
        assertFalse(result.failed(), "Result should not be failed");
        assertEquals("json", result.getFileFormat(), "File format should match");
        assertEquals("trade-message", result.getContentType(), "Content type should match");
        assertEquals("otc-option", result.getBusinessClassification(), "Business classification should match");
        assertEquals("test-scenario", result.getScenarioId(), "Scenario ID should match");
        assertEquals("test-data", result.getParsedData(), "Parsed data should match");
        assertTrue(result.getConfidence() > 0.8, "Confidence should be high");
        assertTrue(result.isCacheable(), "Result should be cacheable");
        
        logger.info("Successful classification result validation: {}", result);
    }

    @Test
    @DisplayName("Should create failed ClassificationResult")
    void testFailedClassificationResult() {
        logger.info("=== Testing failed ClassificationResult ===");
        
        String errorMessage = "Classification failed due to invalid format";
        ClassificationResult result = ClassificationResult.failed(errorMessage);
        
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccessful(), "Result should not be successful");
        assertTrue(result.failed(), "Result should be failed");
        assertEquals(errorMessage, result.getErrorMessage(), "Error message should match");
        assertEquals(0.0, result.getConfidence(), "Failed result should have zero confidence");
        assertFalse(result.isCacheable(), "Failed result should not be cacheable");
        
        logger.info("Failed classification result validation: {}", result);
    }

    @Test
    @DisplayName("Should create ApexProcessingResult with classification")
    void testApexProcessingResult() {
        logger.info("=== Testing ApexProcessingResult ===");
        
        ClassificationResult classification = ClassificationResult.successful(
            "json", "trade", "option", "scenario-1", null, "data"
        );
        
        ApexProcessingResult result = ApexProcessingResult.successful(
            classification, "processing-output", 150L
        );
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Result should be successful");
        assertFalse(result.isFailed(), "Result should not be failed");
        assertEquals(classification, result.getClassification(), "Classification should match");
        assertEquals("processing-output", result.getProcessingResult(), "Processing result should match");
        assertEquals(150L, result.getExecutionTime(), "Execution time should match");
        assertTrue(result.getTimestamp() > 0, "Timestamp should be set");
        
        logger.info("ApexProcessingResult validation successful: {}", result);
    }

    @Test
    @DisplayName("Should detect JSON format from extension")
    void testExtensionBasedDetectorJson() {
        logger.info("=== Testing ExtensionBasedFileFormatDetector for JSON ===");
        
        ExtensionBasedFileFormatDetector detector = new ExtensionBasedFileFormatDetector();
        
        // Test JSON extensions
        String[] jsonFiles = {"data.json", "trades.jsonl", "messages.ndjson", "DATA.JSON"};
        
        for (String fileName : jsonFiles) {
            ApexProcessingContext context = ApexProcessingContext.builder()
                .fileName(fileName)
                .build();
            
            ClassificationContext classContext = ClassificationContext.builder()
                .processingContext(context)
                .build();
            
            assertTrue(detector.canDetect(classContext), 
                      "Should be able to detect format for: " + fileName);
            
            FileFormatResult result = detector.detect(classContext);
            
            assertNotNull(result, "Result should not be null for: " + fileName);
            assertTrue(result.isSuccessful(), "Detection should succeed for: " + fileName);
            assertEquals("json", result.getFormat(), "Should detect JSON format for: " + fileName);
            assertTrue(result.isConfident(), "Should have high confidence for: " + fileName);
            assertEquals("EXTENSION", result.getDetectionMethod(), "Detection method should match");
            
            logger.info("JSON detection successful for {}: {}", fileName, result);
        }
    }

    @Test
    @DisplayName("Should detect XML format from extension")
    void testExtensionBasedDetectorXml() {
        logger.info("=== Testing ExtensionBasedFileFormatDetector for XML ===");
        
        ExtensionBasedFileFormatDetector detector = new ExtensionBasedFileFormatDetector();
        
        String[] xmlFiles = {"data.xml", "schema.xsd", "service.soap", "config.wsdl"};
        
        for (String fileName : xmlFiles) {
            ApexProcessingContext context = ApexProcessingContext.builder()
                .fileName(fileName)
                .build();
            
            ClassificationContext classContext = ClassificationContext.builder()
                .processingContext(context)
                .build();
            
            FileFormatResult result = detector.detect(classContext);
            
            assertTrue(result.isSuccessful(), "Detection should succeed for: " + fileName);
            assertEquals("xml", result.getFormat(), "Should detect XML format for: " + fileName);
            assertTrue(result.isConfident(), "Should have high confidence for: " + fileName);
        }
        
        logger.info("XML detection validation completed");
    }

    @Test
    @DisplayName("Should detect CSV format from extension")
    void testExtensionBasedDetectorCsv() {
        logger.info("=== Testing ExtensionBasedFileFormatDetector for CSV ===");
        
        ExtensionBasedFileFormatDetector detector = new ExtensionBasedFileFormatDetector();
        
        String[] csvFiles = {"data.csv", "trades.tsv", "prices.psv", "report.tab"};
        
        for (String fileName : csvFiles) {
            ApexProcessingContext context = ApexProcessingContext.builder()
                .fileName(fileName)
                .build();
            
            ClassificationContext classContext = ClassificationContext.builder()
                .processingContext(context)
                .build();
            
            FileFormatResult result = detector.detect(classContext);
            
            assertTrue(result.isSuccessful(), "Detection should succeed for: " + fileName);
            assertEquals("csv", result.getFormat(), "Should detect CSV format for: " + fileName);
            assertTrue(result.isConfident(), "Should have high confidence for: " + fileName);
        }
        
        logger.info("CSV detection validation completed");
    }

    @Test
    @DisplayName("Should handle unknown extensions gracefully")
    void testExtensionBasedDetectorUnknown() {
        logger.info("=== Testing ExtensionBasedFileFormatDetector for unknown extensions ===");
        
        ExtensionBasedFileFormatDetector detector = new ExtensionBasedFileFormatDetector();
        
        String[] unknownFiles = {"data.unknown", "file.xyz", "test.binary", "noextension"};
        
        for (String fileName : unknownFiles) {
            ApexProcessingContext context = ApexProcessingContext.builder()
                .fileName(fileName)
                .build();
            
            ClassificationContext classContext = ClassificationContext.builder()
                .processingContext(context)
                .build();
            
            if (fileName.contains(".")) {
                assertTrue(detector.canDetect(classContext), 
                          "Should be able to attempt detection for: " + fileName);
                
                FileFormatResult result = detector.detect(classContext);
                assertFalse(result.isSuccessful(), 
                           "Detection should fail for unknown extension: " + fileName);
            } else {
                assertFalse(detector.canDetect(classContext), 
                           "Should not be able to detect format for file without extension: " + fileName);
            }
        }
        
        logger.info("Unknown extension handling validation completed");
    }

    @Test
    @DisplayName("Should validate detector properties")
    void testDetectorProperties() {
        logger.info("=== Testing detector properties ===");
        
        ExtensionBasedFileFormatDetector detector = new ExtensionBasedFileFormatDetector();
        
        assertEquals("extension-based", detector.getName(), "Detector name should match");
        assertEquals(1, detector.getPriority(), "Priority should be 1");
        
        // Test supported extensions
        String[] jsonExtensions = detector.getSupportedExtensions("json");
        assertTrue(jsonExtensions.length > 0, "Should have JSON extensions");
        assertTrue(Arrays.asList(jsonExtensions).contains("json"), "Should support .json extension");
        
        String[] xmlExtensions = detector.getSupportedExtensions("xml");
        assertTrue(xmlExtensions.length > 0, "Should have XML extensions");
        assertTrue(Arrays.asList(xmlExtensions).contains("xml"), "Should support .xml extension");
        
        logger.info("Detector properties validation completed");
    }
}
