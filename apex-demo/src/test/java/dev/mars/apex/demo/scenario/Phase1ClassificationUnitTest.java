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
 * - ClassificationContext creation and usage
 * - ClassificationResult construction and validation
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
    @DisplayName("Should create ClassificationContext with builder pattern")
    void testClassificationContextBuilder() {
        logger.info("=== Testing ClassificationContext builder pattern ===");

        // Test builder pattern
        ClassificationContext context = ClassificationContext.builder()
            .source("test-source")
            .fileName("test.json")
            .fileSize(1024L)
            .metadata(Map.of("region", "US", "priority", "HIGH"))
            .correlationId("test-123")
            .inputData("test-data")
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
        try {
            originalMetadata.put("test", "value");
            fail("Metadata should be immutable");
        } catch (UnsupportedOperationException e) {
            // Expected - metadata is immutable
        }

        logger.info("ClassificationContext validation successful: {}", context);
    }

    @Test
    @DisplayName("Should create ClassificationContext with minimal properties")
    void testMinimalClassificationContext() {
        logger.info("=== Testing minimal ClassificationContext ===");

        ClassificationContext context = ClassificationContext.builder()
            .inputData("test-data")
            .build();

        assertNotNull(context, "Context should not be null");
        assertNotNull(context.getMetadata(), "Metadata should not be null");
        // Note: inputData() adds to metadata, so it won't be empty
        assertEquals(1, context.getMetadata().size(), "Metadata should contain inputData");
        assertEquals("test-data", context.getMetadata().get("inputData"), "InputData should be in metadata");

        logger.info("Minimal context validation successful: {}", context);
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
    @DisplayName("Should detect JSON format from extension")
    void testExtensionBasedDetectorJson() {
        logger.info("=== Testing ExtensionBasedFileFormatDetector for JSON ===");

        ExtensionBasedFileFormatDetector detector = new ExtensionBasedFileFormatDetector();

        // Test JSON extensions
        String[] jsonFiles = {"data.json", "trades.jsonl", "messages.ndjson", "DATA.JSON"};

        for (String fileName : jsonFiles) {
            ClassificationContext classContext = ClassificationContext.builder()
                .fileName(fileName)
                .inputData("test-data")
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
            ClassificationContext classContext = ClassificationContext.builder()
                .fileName(fileName)
                .inputData("test-data")
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
            ClassificationContext classContext = ClassificationContext.builder()
                .fileName(fileName)
                .inputData("test-data")
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
            ClassificationContext classContext = ClassificationContext.builder()
                .fileName(fileName)
                .inputData("test-data")
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
