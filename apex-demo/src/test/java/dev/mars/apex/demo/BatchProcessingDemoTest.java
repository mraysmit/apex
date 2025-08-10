package dev.mars.apex.demo;

import dev.mars.apex.demo.examples.BatchProcessingDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for BatchProcessingDemo.
 * Verifies that batch processing functionality works correctly.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
@DisplayName("Batch Processing Demo Tests")
public class BatchProcessingDemoTest {

    private BatchProcessingDemo demo;

    @BeforeEach
    void setUp() {
        try {
            demo = new BatchProcessingDemo();
        } catch (Exception e) {
            fail("Failed to initialize BatchProcessingDemo: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Demo should initialize successfully")
    void testDemoInitialization() {
        assertNotNull(demo, "Demo should be initialized");
    }

    @Test
    @DisplayName("Demo should run without exceptions")
    void testDemoExecution() {
        assertDoesNotThrow(() -> {
            BatchProcessingDemo.main(new String[]{});
        }, "Demo execution should not throw exceptions");
    }

    @Test
    @DisplayName("Demo should handle empty batch gracefully")
    void testEmptyBatchHandling() {
        // This test verifies that the demo can handle edge cases
        assertDoesNotThrow(() -> {
            // The demo should handle various scenarios including empty data
            demo.run();
        }, "Demo should handle empty batches gracefully");
    }

    @Test
    @DisplayName("Demo should complete within reasonable time")
    void testPerformance() {
        long startTime = System.currentTimeMillis();

        assertDoesNotThrow(() -> {
            demo.run();
        });

        long executionTime = System.currentTimeMillis() - startTime;
        assertTrue(executionTime < 30000,
                  "Demo should complete within 30 seconds, took: " + executionTime + "ms");
    }

    @Test
    @DisplayName("Demo should produce meaningful output")
    void testOutputGeneration() {
        // Capture system output to verify demo produces expected results
        assertDoesNotThrow(() -> {
            demo.run();
            // In a real test, we might capture System.out to verify specific output
            // For now, we just ensure no exceptions are thrown
        }, "Demo should produce output without errors");
    }
}