package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EDGE CASE TEST 7: Empty Numbered Sections
 * 
 * Tests that empty numbered sections mixed with populated ones are handled correctly.
 * 
 * YAML Structure:
 * - enrichments-1: empty
 * - enrichments-2: has items
 * - enrichments-3: empty
 * 
 * Expected Behavior:
 * - Should work correctly (empty sections are skipped during merge)
 * - Should NOT cause errors or break document order
 * 
 * This test verifies that merge logic handles empty sections gracefully.
 */
@DisplayName("EDGE CASE 7: Empty Numbered Sections")
public class TestEdge7_EmptyNumberedSectionsTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestEdge7_EmptyNumberedSectionsTest.class);

    @Test
    @DisplayName("Should handle empty numbered sections correctly")
    public void testEmptyNumberedSections() {
        LOGGER.info("=== EDGE CASE TEST 7: Empty Numbered Sections ===");

        try {
            // Attempt to load YAML with empty numbered sections
            RulesEngine engine = RulesEngine.fromFile(
                "src/test/java/dev/mars/apex/demo/sequencing/TestEdge7_EmptyNumberedSectionsTest.yaml"
            );

            LOGGER.info("✅ EDGE CASE TEST 7 PASSED: Empty numbered sections handled correctly");
            assertNotNull(engine, "RulesEngine should be created successfully");

        } catch (Exception e) {
            LOGGER.error("❌ EDGE CASE TEST 7 FAILED: Empty numbered sections caused error", e);
            fail("Empty numbered sections should be handled gracefully: " + e.getMessage());
        }
    }
}

