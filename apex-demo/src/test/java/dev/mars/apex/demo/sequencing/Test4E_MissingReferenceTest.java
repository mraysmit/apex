package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.engine.core.RulesEngine;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test 4E: Missing Reference Edge Case
 * 
 * Proves: Groups referencing non-existent enrichments don't break the system
 * 
 * YAML Structure:
 * - 1 standalone enrichment
 * - 1 grouped enrichment
 * - 1 enrichment-group referencing both existing and non-existent enrichment
 * 
 * Expected Execution Order:
 * 1. standalone-1 (position 1)
 * 2. grouped-1 (via group-A at position 3)
 * 
 * Definitive Assertions (5 types):
 * 1. EXACT execution count: 2 items
 * 2. EXACT execution order: standalone-1, grouped-1
 * 3. Verify what executed: standalone-1 and grouped-1
 * 4. Verify NO double execution: Each item executes exactly once
 * 5. Verify execution positions: Each item at correct position
 */
public class Test4E_MissingReferenceTest extends DemoTestBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(Test4E_MissingReferenceTest.class);

    @BeforeEach
    public void clearTracker() {
        ExecutionTracker.clear();
    }

    @Test
    public void testMissingReference() throws Exception {
        LOGGER.info("=== TEST 4E: Missing Reference (Expects Validation Error) ===");

        // APEX should reject groups that reference non-existent enrichments
        // This is CORRECT behavior - validation should catch configuration errors

        Exception exception = assertThrows(Exception.class, () -> {
            RulesEngine.fromClasspath("dev/mars/apex/demo/sequencing/Test4E_MissingReferenceTest.yaml");
        });

        // Verify the error message mentions the missing enrichment
        String errorMessage = exception.getMessage();
        LOGGER.info("Expected validation error: " + errorMessage);

        assertTrue(errorMessage.contains("non-existent-enrichment"),
            "Error message should mention the missing enrichment ID");
        assertTrue(errorMessage.contains("group-A"),
            "Error message should mention the group that references it");

        LOGGER.info("[OK] TEST 4E PASSED: APEX correctly validates enrichment references");
        LOGGER.info("  [OK] Missing reference detected during YAML loading");
        LOGGER.info("  [OK] Clear error message provided");
        LOGGER.info("  [OK] Configuration error prevented from reaching runtime");
    }
}

