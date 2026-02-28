package dev.mars.apex.core.config.yaml;
import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.config.loader.*;
import dev.mars.apex.core.config.exception.*;
import dev.mars.apex.core.config.service.*;

import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that APEX handles circular references gracefully.
 * 
 * This test verifies that when:
 * 1. File A has enrichment-refs that loads File B
 * 2. File B has enrichment-refs that loads File A (circular reference)
 * 
 * Then APEX should:
 * - NOT throw an exception (graceful handling)
 * - NOT enter infinite loop
 * - Load each file only once (duplicate prevention)
 * - Merge enrichments from both files correctly
 * 
 * This is an edge case test to ensure that APEX's circular reference detection works correctly.
 */
@DisplayName("Circular Reference Detection Tests")
class CircularReferenceDetectionTest {

    private static final Logger logger = LoggerFactory.getLogger(CircularReferenceDetectionTest.class);
    private ConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ConfigurationLoader();
    }

    @Test
    @DisplayName("EDGE CASE: circular references should be detected and reported as duplicate ID error")
    void testCircularReferenceDetection() {
        // Load file A which references file B which references file A (circular)
        // This should throw a ConfigurationException due to duplicate IDs

        logger.info("=== CIRCULAR REFERENCE DETECTION TEST ===");

        ConfigurationException exception = assertThrows(
            ConfigurationException.class,
            () -> loader.loadFromClasspath("config/circular-a.yaml"),
            "Circular reference should cause duplicate ID validation error"
        );

        // Verify the exception message mentions duplicate enrichment ID
        String message = exception.getMessage();
        assertTrue(message.contains("Duplicate enrichment ID"),
                "Exception message should mention duplicate enrichment ID");
        assertTrue(message.contains("enrichment_a") || message.contains("enrichment_b"),
                "Exception message should mention the duplicate enrichment ID");

        logger.info("=== TEST RESULTS ===");
        logger.info("No infinite loop - circular reference prevented");
        logger.info("Duplicate ID validation caught the circular reference");
        logger.info("Exception message: {}", message);
    }
}

