package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EDGE CASE TEST 5: Forward Reference to External Enrichments
 * 
 * Tests that enrichment-groups can reference enrichments from external files
 * even when enrichment-refs comes AFTER the enrichment-groups section.
 * 
 * YAML Structure:
 * - enrichment-groups: references "external-1" (not loaded yet)
 * - enrichment-refs: loads external.yaml containing "external-1"
 * 
 * Expected Behavior:
 * - Should work correctly (reference resolution happens before group validation)
 * - OR should fail with clear error message about missing enrichment
 * 
 * This test verifies the order of reference resolution vs group validation.
 */
@DisplayName("EDGE CASE 5: Forward Reference to External Enrichments")
public class TestEdge5_ForwardReferenceToExternalTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestEdge5_ForwardReferenceToExternalTest.class);

    @Test
    @DisplayName("Should handle forward references to external enrichments")
    public void testForwardReferenceToExternal() {
        LOGGER.info("=== EDGE CASE TEST 5: Forward Reference to External Enrichments ===");

        try {
            // Attempt to load YAML with forward reference
            RulesEngine engine = RulesEngine.fromClasspath("dev/mars/apex/demo/sequencing/TestEdge5_ForwardReferenceToExternalTest.yaml");

            LOGGER.info("✅ EDGE CASE TEST 5 PASSED: Forward references work correctly");
            LOGGER.info("   Reference resolution happens before group validation");
            
            assertNotNull(engine, "RulesEngine should be created successfully");

        } catch (Exception e) {
            // If it fails, verify it's a clear error about missing enrichment
            LOGGER.info("⚠️ EDGE CASE TEST 5: Forward references NOT supported");
            LOGGER.info("   Error message: {}", e.getMessage());
            
            String errorMessage = e.getMessage().toLowerCase();
            assertTrue(
                errorMessage.contains("not found") || errorMessage.contains("missing") || errorMessage.contains("external-1"),
                "Error message should clearly indicate missing enrichment: " + e.getMessage()
            );
        }
    }
}

